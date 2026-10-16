/*
 * Copyright 2022 Nikita Shakarun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.microedition.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.microedition.util.ContextHolder;

public class CameraController {
	private static final Size FRAME_SIZE = new Size(640, 480);

	public interface FrameListener {
		void onFrame(Bitmap frame);
	}

	private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
	private ProcessCameraProvider cameraProvider;
	private ImageCapture imageCapture;
	private FrameListener frameListener;
	private int sourceWidth = FRAME_SIZE.getWidth();
	private int sourceHeight = FRAME_SIZE.getHeight();
	private byte[] pixels;
	private byte[] snapshot;
	private final Object monitor = new Object();
	private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "MidletCameraExecutor");
		thread.setDaemon(true);
		return thread;
	});

	public void setUp(PreviewView view) {
		Preview preview = new Preview.Builder()
				.build();
		preview.setSurfaceProvider(view.getSurfaceProvider());
		bind(preview);
	}

	public void setUp(FrameListener listener) {
		frameListener = listener;

		ImageAnalysis analysis = new ImageAnalysis.Builder()
				.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
				.setResolutionSelector(new ResolutionSelector.Builder()
						.setResolutionStrategy(new ResolutionStrategy(FRAME_SIZE,
								ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER))
						.build())
				.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
				.build();
		analysis.setAnalyzer(executor, this::analyze);
		bind(analysis);
	}

	private void bind(UseCase useCase) {
		Context context = ContextHolder.getActivity();

		if (cameraProviderFuture == null) {
			cameraProviderFuture = ProcessCameraProvider.getInstance(context);
			cameraProviderFuture.addListener(() -> {
				try {
					cameraProvider = cameraProviderFuture.get();
				} catch (ExecutionException | InterruptedException e) {
					e.printStackTrace();
				} finally {
					cameraProviderFuture = null;
				}

				CameraSelector cameraSelector = new CameraSelector.Builder()
						.requireLensFacing(CameraSelector.LENS_FACING_BACK)
						.build();
				imageCapture = new ImageCapture.Builder()
						.setResolutionSelector(new ResolutionSelector.Builder()
								.setResolutionStrategy(new ResolutionStrategy(FRAME_SIZE,
										ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER))
								.build())
						.build();

				cameraProvider.unbindAll();
				cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, imageCapture, useCase);
			}, ContextCompat.getMainExecutor(context));
		}
	}

	private void analyze(@NonNull ImageProxy image) {
		FrameListener listener;
		synchronized (this) {
			listener = frameListener;
		}
		if (listener == null) {
			image.close();
			return;
		}
		try {
			Bitmap frame = toFrameBitmap(image);
			if (frame != null) {
				sourceWidth = frame.getWidth();
				sourceHeight = frame.getHeight();
				listener.onFrame(frame);
			}
		} finally {
			image.close();
		}
	}

	public int getSourceWidth() {
		return sourceWidth;
	}

	public int getSourceHeight() {
		return sourceHeight;
	}

	public byte[] getSnapshot(String imageType) {
		imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
			@Override
			public void onCaptureSuccess(@NonNull ImageProxy image) {
				snapshot = toSnapshot(image, imageType);

				synchronized (monitor) {
					monitor.notifyAll();
				}
			}

			@Override
			public void onError(@NonNull ImageCaptureException exception) {
				snapshot = null;
				synchronized (monitor) {
					monitor.notifyAll();
				}
			}
		});
		synchronized (monitor) {
			try {
				monitor.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return snapshot;
	}

	private Bitmap toFrameBitmap(ImageProxy image) {
		ImageProxy.PlaneProxy plane = image.getPlanes()[0];
		int width = image.getWidth();
		int height = image.getHeight();
		int rowStride = plane.getRowStride();
		int rowSize = width * 4;
		int size = rowSize * height;
		ByteBuffer buffer = plane.getBuffer();
		if (pixels == null || pixels.length < size) {
			pixels = new byte[size];
		}
		if (rowStride == rowSize) {
			buffer.get(pixels, 0, size);
		} else {
			for (int row = 0; row < height; row++) {
				buffer.position(row * rowStride);
				buffer.get(pixels, row * rowSize, rowSize);
			}
		}
		Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bmp.copyPixelsFromBuffer(ByteBuffer.wrap(pixels, 0, size));
		try {
			bmp = rotateImageIfRequired(bmp, image.getImageInfo().getRotationDegrees());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bmp;
	}

	@Nullable
	private byte[] toSnapshot(ImageProxy image, String imageType) {
		if (image.getFormat() != ImageFormat.JPEG) {
			return null;
		}
		ByteBuffer buffer = image.getPlanes()[0].getBuffer();
		byte[] data = new byte[buffer.remaining()];
		buffer.get(data);
		boolean jpeg = isJpeg(imageType);
		Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
		if (bmp == null) {
			return null;
		}
		try {
			bmp = rotateImageIfRequired(bmp, image.getImageInfo().getRotationDegrees());
		} catch (IOException e) {
			e.printStackTrace();
		}
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		Bitmap.CompressFormat format = jpeg ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.PNG;
		if (!bmp.compress(format, 90, stream)) {
			bmp.recycle();
			return null;
		}
		bmp.recycle();
		return stream.toByteArray();
	}

	private static boolean isJpeg(String imageType) {
		return imageType != null && imageType.contains("jpeg");
	}

	private Bitmap rotateImageIfRequired(Bitmap img, int orientation) throws IOException {
		return switch (orientation) {
			case 90 -> rotateImage(img, 90);
			case 180 -> rotateImage(img, 180);
			case 270 -> rotateImage(img, 270);
			default -> img;
		};
	}

	private Bitmap rotateImage(Bitmap img, int degree) {
		Matrix matrix = new Matrix();
		matrix.postRotate(degree);
		Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, false);
		img.recycle();
		return rotatedImg;
	}

	public synchronized void tearDown() {
		frameListener = null;
		ProcessCameraProvider provider = cameraProvider;
		Context context = ContextHolder.getActivity();
		if (provider != null && context != null) {
			ContextCompat.getMainExecutor(context).execute(provider::unbindAll);
		}
	}
}
