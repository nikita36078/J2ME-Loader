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
import android.graphics.Matrix;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.microedition.util.ContextHolder;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraController {
	private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
	private ImageCapture imageCapture;
	private byte[] result = null;
	private final Object monitor = new Object();

	public void setUp(PreviewView view) {
		Context context = ContextHolder.getActivity();
		cameraProviderFuture = ProcessCameraProvider.getInstance(context);
		cameraProviderFuture.addListener(() -> {
			try {
				ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
				Preview preview = new Preview.Builder()
						.build();
				CameraSelector cameraSelector = new CameraSelector.Builder()
						.requireLensFacing(CameraSelector.LENS_FACING_BACK)
						.build();
				imageCapture = new ImageCapture.Builder()
						.build();

				preview.setSurfaceProvider(view.getSurfaceProvider());

				cameraProvider.unbindAll();
				cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, imageCapture, preview);
			} catch (ExecutionException | InterruptedException e) {
				// No errors need to be handled for this Future.
				// This should never be reached.
			}
		}, ContextCompat.getMainExecutor(context));
	}

	public byte[] getSnapshot() {
		Executor executor = Executors.newSingleThreadExecutor();
		imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
			@Override
			public void onCaptureSuccess(@NonNull ImageProxy image) {
				ImageInfo info = image.getImageInfo();
				ByteBuffer buffer = image.getPlanes()[0].getBuffer();
				byte[] temp = new byte[buffer.remaining()];
				buffer.get(temp);
				Bitmap bmp = BitmapFactory.decodeByteArray(temp, 0, temp.length);
				try {
					bmp = rotateImageIfRequired(bmp, info.getRotationDegrees());
				} catch (IOException e) {
					e.printStackTrace();
				}
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
				result = stream.toByteArray();

				synchronized (monitor) {
					monitor.notifyAll();
				}
			}

			@Override
			public void onError(@NonNull ImageCaptureException exception) {
				result = null;
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
		return result;
	}

	private Bitmap rotateImageIfRequired(Bitmap img, int orientation) throws IOException {
		switch (orientation) {
			case 90:
				return rotateImage(img, 90);
			case 180:
				return rotateImage(img, 180);
			case 270:
				return rotateImage(img, 270);
			default:
				return img;
		}
	}

	private Bitmap rotateImage(Bitmap img, int degree) {
		Matrix matrix = new Matrix();
		matrix.postRotate(degree);
		Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, false);
		img.recycle();
		return rotatedImg;
	}
}
