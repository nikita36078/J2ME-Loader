/*
 *  Copyright 2022 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.woesss.j2me.micro3d;

import static ru.woesss.j2me.micro3d.Utils.TAG;

import android.opengl.GLES20;
import android.util.Log;

import com.mascotcapsule.micro3d.v3.Texture;

import java.io.IOException;

import javax.microedition.shell.AppClassLoader;

public class TextureImpl {
	static int sLastId;
	final TextureData image;
	int mTexId = -1;

	public TextureImpl(byte[] b) {
		if (b == null) {
			throw new NullPointerException();
		}
		try {
			image = Loader.loadBmpData(b);
		} catch (IOException e) {
			Log.e(TAG, "Error loading data", e);
			throw new RuntimeException(e);
		}
	}

	public TextureImpl(String name) throws IOException {
		if (name == null) {
			throw new NullPointerException();
		}
		byte[] b = AppClassLoader.getResourceAsBytes(name);
		if (b == null) {
			throw new IOException();
		}
		try {
			image = Loader.loadBmpData(b);
		} catch (IOException e) {
			Log.e(TAG, "Error loading data from [" + name + "]", e);
			throw new RuntimeException(e);
		}
	}

	public final void dispose() {}

	int getId() {
		if (GLES20.glIsTexture(mTexId)) {
			return mTexId;
		}
		mTexId = loadTexture(image);
		return mTexId;
	}

	static int loadTexture(TextureData bitmap) {
		final int[] textureIds = new int[1];
		synchronized (Texture.class) {
			while (textureIds[0] <= sLastId) {
				GLES20.glGenTextures(1, textureIds, 0);
			}
			sLastId = textureIds[0];
		}
		Render.checkGlError("glGenTextures");

		if (bitmap == null) {
			GLES20.glDeleteTextures(1, textureIds, 0);
			return -1;
		}

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);

		boolean filter = Boolean.getBoolean("micro3d.v3.texture.filter");
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter ? GLES20.GL_LINEAR : GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter ? GLES20.GL_LINEAR : GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap.width, bitmap.height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bitmap.getRaster());

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

		return textureIds[0];
	}

	int getWidth() {
		return image.width;
	}

	int getHeight() {
		return image.height;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			dispose();
		} finally {
			super.finalize();
		}
	}
}