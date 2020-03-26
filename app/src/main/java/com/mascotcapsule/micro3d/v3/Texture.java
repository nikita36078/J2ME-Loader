/*
 * Copyright 2018 Nikita Shakarun
 * Copyright 2019 Yury Kharchenko
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

package com.mascotcapsule.micro3d.v3;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.mascotcapsule.micro3d.v3.util.BitmapUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.Image;
import javax.microedition.util.ContextHolder;

import static android.opengl.GLES20.*;

@SuppressWarnings("unused, WeakerAccess")
public class Texture {
	protected boolean isModel;
	protected Bitmap image, transparentImage;
	private int glTexId, glTranspTexId;
	private int transparentColor;
	private float[] size;

	public Texture(byte[] b, boolean isForModel) {
		initTexture(b, isForModel);
	}

	public Texture(String name, boolean isForModel) throws IOException {
		InputStream inputStream = ContextHolder.getResourceAsStream(null, name);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[2048];
		int length;
		while ((length = inputStream.read(buffer)) > 0) {
			outputStream.write(buffer, 0, length);
		}
		initTexture(outputStream.toByteArray(), isForModel);
	}

	private void initTexture(byte[] b, boolean isForModel) {
		if (b == null) {
			throw new RuntimeException();
		}
		this.isModel = isForModel;
		this.image = Image.createImage(b, 0, b.length).getBitmap();
		this.size = new float[]{image.getWidth() - 1, image.getHeight() - 1};
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(b);
			this.transparentColor = BitmapUtils.getPaletteColor(bais);
			bais.close();
			createTransparentBitmap();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createTransparentBitmap() {
		int width = image.getWidth();
		int height = image.getHeight();
		transparentImage = image.copy(Bitmap.Config.ARGB_8888, true);
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				int color = image.getPixel(j, i);
				if (color == transparentColor) {
					transparentImage.setPixel(j, i, 0);
				} else {
					transparentImage.setPixel(j, i, color);
				}
			}
		}
	}

	private int loadTexture(Bitmap bitmap) {
		final int[] textureIds = new int[1];
		glGenTextures(1, textureIds, 0);
		if (textureIds[0] == 0) {
			com.mascotcapsule.micro3d.v3.render.GLUtils.checkGlError("glGenTextures");
			return 0;
		}

		if (bitmap == null) {
			glDeleteTextures(1, textureIds, 0);
			return 0;
		}

		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, textureIds[0]);

		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

		GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);

		bitmap.recycle();

		glBindTexture(GL_TEXTURE_2D, 0);

		return textureIds[0];
	}

	public final void dispose() {
		if (glTexId > 0) {
			GLES20.glDeleteTextures(1, new int[]{glTexId}, 0);
			glTexId = -1;
		}
		if (glTranspTexId > 0) {
			GLES20.glDeleteTextures(1, new int[]{glTranspTexId}, 0);
			glTranspTexId = -1;
		}
	}

	public int getId() {
		if (glTexId == -1) throw new IllegalStateException("Already disposed!!!");
		if (glTexId == 0) {
			glTexId = loadTexture(image);
			image = null;
		}
		return glTexId;
	}

	public int getTransparentId() {
		if (glTranspTexId == -1) throw new IllegalStateException("Already disposed!!!");
		if (glTranspTexId == 0) {
			glTranspTexId = loadTexture(transparentImage);
			transparentImage = null;
		}
		return glTranspTexId;
	}

	public float[] getSize() {
		return size;
	}
}
