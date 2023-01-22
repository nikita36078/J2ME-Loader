/*
 *  Copyright 2023 Yury Kharchenko
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

package com.nokia.mid.m3d;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL11;
import javax.microedition.lcdui.Image;

public class Texture {
	private static final String TAG = "nokia.m3d.Texture";
	private static int sLastId;
	private int mTexId = -1;
	private final int target;
	private final int format;

	private final int width;
	private final int height;
	private final ByteBuffer buffer;

	public Texture(int target, int format, Image image) {
		if (format != 32832) {
			Log.e(TAG, "Not supported texture format: " + format);
			throw new RuntimeException("Not supported texture format: " + format);
		}
		this.target = target;
		this.format = GL11.GL_LUMINANCE;
		width = image.getWidth();
		height = image.getHeight();
		int[] imagedata = new int[width * height];
		image.getRGB(imagedata, 0, width, 0, 0, width, height);
		buffer = ByteBuffer.allocateDirect(width * height);
		// fill texData for luminance format
		for (int p : imagedata) {
			int r = p >> 16 & 0xFF;
			int g = p >> 8 & 0xFF;
			int b = p & 0xFF;
			buffer.put((byte) (0xff - (0x4CB2 * r + 0x9691 * g + 0x1D3E * b >> 16)));
		}
	}

	public int glId(GL11 gl) {
		if (!gl.glIsTexture(mTexId)) {
			generateId(gl);
		} else {
			return mTexId;
		}
		loadToGL(gl);
		return mTexId;
	}

	private void generateId(GL11 gl) {
		final IntBuffer textureIds = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
		synchronized (Texture.class) {
			while (textureIds.get(0) <= sLastId) {
				textureIds.rewind();
				gl.glGenTextures(1, textureIds);
			}
			sLastId = textureIds.get(0);
			mTexId = textureIds.get(0);
		}
	}

	private void loadToGL(GL11 gl) {
		gl.glBindTexture(target, mTexId);

		gl.glTexParameteri(target, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		gl.glTexParameteri(target, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		gl.glTexParameteri(target, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(target, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP_TO_EDGE);
		gl.glTexImage2D(target, 0, format, width, height, 0, format, GL11.GL_UNSIGNED_BYTE, buffer.rewind());
	}
}
