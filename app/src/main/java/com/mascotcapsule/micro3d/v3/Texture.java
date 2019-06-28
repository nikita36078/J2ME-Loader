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

import java.io.IOException;

import javax.microedition.lcdui.Image;

import static android.opengl.GLES20.*;

@SuppressWarnings("unused, WeakerAccess")
public class Texture {
	protected boolean isModel;
	protected Bitmap image;
	private int glTexId;

	public Texture(byte[] b, boolean isForModel) {
		if (b == null) {
			throw new RuntimeException();
		}
		this.isModel = isForModel;
		this.image = Image.createImage(b, 0, b.length).getBitmap();
	}

	public Texture(String name, boolean isForModel) throws IOException {
		if (name == null) {
			throw new NullPointerException();
		}
		this.isModel = isForModel;
		this.image = Image.createImage(name).getBitmap();
	}

	public static int loadTexture(Bitmap bitmap) {
		final int[] textureIds = new int[1];
		glGenTextures(1, textureIds, 0);
		if (textureIds[0] == 0) {
			com.mascotcapsule.micro3d.v3.impl.GLUtils.checkGlError("glGenTextures");
			return 0;
		}

		if (bitmap == null) {
			glDeleteTextures(1, textureIds, 0);
			return 0;
		}

		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, textureIds[0]);

		glTexParameteri(GLES20.GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GLES20.GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

		bitmap.recycle();

		glBindTexture(GL_TEXTURE_2D, 0);

		return textureIds[0];
	}

	public final void dispose() {
		if (glTexId > 0) {
			GLES20.glDeleteTextures(1, new int[]{glTexId}, 0);
			glTexId = -1;
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
}
