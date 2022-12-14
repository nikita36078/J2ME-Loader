/*
 * Copyright 2020 Yury Kharchenko
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

import static android.opengl.GLES20.*;
import static com.mascotcapsule.micro3d.v3.Util3D.TAG;

import android.util.Log;

import java.io.IOException;

import javax.microedition.shell.AppClassLoader;

@SuppressWarnings("unused, WeakerAccess")
public class Texture {
	private static int sLastId;

	final boolean isSphere;
	private final TextureData image;
	private int mTexId = -2;

	public Texture(byte[] b, boolean isForModel) {
		if (b == null) {
			throw new NullPointerException();
		}
		isSphere = !isForModel;
		try {
			image = Loader.loadBmpData(b);
		} catch (IOException e) {
			Log.e(TAG, "Error loading data", e);
			throw new RuntimeException(e);
		}
	}

	public Texture(String name, boolean isForModel) throws IOException {
		this(getData(name), isForModel);
	}

	public final void dispose() {
//		synchronized (Render.getRender()) {
//			Render.getRender().bindEglContext();
//			if (glIsTexture(mTexId)) {
//				glDeleteTextures(1, new int[]{mTexId}, 0);
//				mTexId = -1;
//			}
//			Render.getRender().releaseEglContext();
//		}
	}

	int getId() {
		if (mTexId == -1) throw new IllegalStateException("Already disposed!!!");
		if (glIsTexture(mTexId)) {
			return mTexId;
		}
		mTexId = loadTexture(image);
		return mTexId;
	}

	private synchronized static int loadTexture(TextureData bitmap) {
		final int[] textureIds = new int[1];
		synchronized (Texture.class) {
			while (textureIds[0] <= sLastId) {
				glGenTextures(1, textureIds, 0);
			}
		}
		Render.checkGlError("glGenTextures");

		if (bitmap == null) {
			glDeleteTextures(1, textureIds, 0);
			return 0;
		}

		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, textureIds[0]);

		boolean filter = Boolean.getBoolean("micro3d.v3.texture.filter");
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter ? GL_LINEAR : GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter ? GL_LINEAR : GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, bitmap.width, bitmap.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, bitmap.getRaster());

		glBindTexture(GL_TEXTURE_2D, 0);

		int textureId = textureIds[0];
		sLastId = textureId;
		return textureId;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			dispose();
		} finally {
			super.finalize();
		}
	}

	private static byte[] getData(String name) throws IOException {
		if (name == null) {
			throw new NullPointerException();
		}
		byte[] b = AppClassLoader.getResourceAsBytes(name);
		if (b == null) throw new IOException();
		return b;
	}

	int getWidth() {
		return image.width;
	}

	int getHeight() {
		return image.height;
	}
}
