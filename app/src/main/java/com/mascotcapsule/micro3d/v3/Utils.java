/*
 *  Copyright 2020 Yury Kharchenko
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

package com.mascotcapsule.micro3d.v3;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

class Utils {

	static final float TO_FLOAT = 2.4414062E-04f;
	static final float TO_RADIANS = (float) (Math.PI / 2048.0);
	static final float[] IDENTITY_AFFINE = {
			// 0     1     2     3
			// 0     4     8    12
			1.0f, 0.0f, 0.0f, 0.0f,
			// 4     5     6     7
			// 1     5     9    13
			0.0f, 1.0f, 0.0f, 0.0f,
			// 8     9    10    11
			// 2     6    10    14
			0.0f, 0.0f, 1.0f, 0.0f
	};

	static void parallelScale(float[] pm, int x, int y, FigureLayout layout, float vw, float vh) {
		float w = vw * (4096.0f / layout.scaleX);
		float h = vh * (4096.0f / layout.scaleY);

		float sx = 2.0f / w;
		float sy = 2.0f / h;
		float sz = 1.0f / 65536.0f;
		float tx = 2.0f * (layout.centerX + x) / vw - 1.0f;
		float ty = 2.0f * (layout.centerY + y) / vh - 1.0f;
		float tz = 0.0f;

		pm[ 0] =   sx; pm[ 4] = 0.0f; pm[ 8] = 0.0f; pm[12] =   tx;
		pm[ 1] = 0.0f; pm[ 5] =   sy; pm[ 9] = 0.0f; pm[13] =   ty;
		pm[ 2] = 0.0f; pm[ 6] = 0.0f; pm[10] =   sz; pm[14] =   tz;
		pm[ 3] = 0.0f; pm[ 7] = 0.0f; pm[11] = 0.0f; pm[15] = 1.0f;
	}

	static void parallelWH(float[] pm, int x, int y, FigureLayout layout, float vw, float vh) {
		float w = layout.parallelWidth == 0 ? 400.0f * 4.0f : layout.parallelWidth;
		float h = layout.parallelHeight == 0 ? w * (vh / vw) : layout.parallelHeight;

		float sx = 2.0f / w;
		float sy = 2.0f / h;
		float sz = 1.0f / 65536.0f;
		float tx = 2.0f * (layout.centerX + x) / vw - 1.0f;
		float ty = 2.0f * (layout.centerY + y) / vh - 1.0f;
		float tz = 0.0f;

		pm[0] =   sx; pm[4] = 0.0f; pm[ 8] = 0.0f; pm[12] =   tx;
		pm[1] = 0.0f; pm[5] =   sy; pm[ 9] = 0.0f; pm[13] =   ty;
		pm[2] = 0.0f; pm[6] = 0.0f; pm[10] =   sz; pm[14] =   tz;
		pm[3] = 0.0f; pm[7] = 0.0f; pm[11] = 0.0f; pm[15] = 1.0f;
	}

	static void perspectiveFov(float[] pm, int x, int y, FigureLayout layout, float vw, float vh) {
		float near = layout.near;
		float far = layout.far;
		float rd = 1.0f / (near - far);
		float sx = 1.0f / (float) Math.tan(layout.angle * TO_FLOAT * Math.PI);
		float sy = sx * (vw / vh);
		float sz = -(far + near) * rd;
		float tx = 2.0f * (layout.centerX + x) / vw - 1.0f;
		float ty = 2.0f * (layout.centerY + y) / vh - 1.0f;
		float tz = 2.0f * far * near * rd;

		pm[0] =   sx; pm[4] = 0.0f; pm[ 8] =   tx; pm[12] = 0.0f;
		pm[1] = 0.0f; pm[5] =   sy; pm[ 9] =   ty; pm[13] = 0.0f;
		pm[2] = 0.0f; pm[6] = 0.0f; pm[10] =   sz; pm[14] =   tz;
		pm[3] = 0.0f; pm[7] = 0.0f; pm[11] = 1.0f; pm[15] = 0.0f;
	}

	static void perspectiveWH(float[] pm, int x, int y, FigureLayout layout, float vw, float vh) {
		float zFar = layout.far;
		float zNear = layout.near;
		float width = layout.perspectiveWidth == 0 ? vw : layout.perspectiveWidth * TO_FLOAT;
		float height = layout.perspectiveHeight == 0 ? vh : layout.perspectiveHeight * TO_FLOAT;

		float rd = 1.0f / (zNear - zFar);
		float sx = 2.0f * zNear / width;
		float sy = 2.0f * zNear / height;
		float sz = -(zNear + zFar) * rd;
		float tx = 2.0f * (layout.centerX + x) / vw - 1.0f;
		float ty = 2.0f * (layout.centerY + y) / vh - 1.0f;
		float tz = 2.0f * zFar * zNear * rd;

		pm[0] =   sx; pm[4] = 0.0f; pm[ 8] =   tx; pm[12] = 0.0f;
		pm[1] = 0.0f; pm[5] =   sy; pm[ 9] =   ty; pm[13] = 0.0f;
		pm[2] = 0.0f; pm[6] = 0.0f; pm[10] =   sz; pm[14] =   tz;
		pm[3] = 0.0f; pm[7] = 0.0f; pm[11] = 1.0f; pm[15] = 0.0f;
	}

	static void getSpriteVertex(float[] quad, float[] center, int angle, float halfW, float halfH) {
		float r = angle * TO_RADIANS;
		float sin = (float) Math.sin(r);
		float cos = (float) Math.cos(r);
		float x = center[0];
		float y = center[1];
		float z = center[2];
		float w = center[3];
		quad[0] = -halfW * cos + halfH * -sin + x;
		quad[1] = -halfW * sin + halfH * cos + y;
		quad[2] = z;
		quad[3] = w;
		float bx = -halfW * cos + -halfH * -sin + x;
		float by = -halfW * sin + -halfH * cos + y;
		quad[4] = bx;
		quad[5] = by;
		quad[6] = z;
		quad[7] = w;
		float cx = halfW * cos + halfH * -sin + x;
		float cy = halfW * sin + halfH * cos + y;
		quad[8] = cx;
		quad[9] = cy;
		quad[10] = z;
		quad[11] = w;
		quad[12] = cx;
		quad[13] = cy;
		quad[14] = z;
		quad[15] = w;
		quad[16] = bx;
		quad[17] = by;
		quad[18] = z;
		quad[19] = w;
		quad[20] = halfW * cos + -halfH * -sin + x;
		quad[21] = halfW * sin + -halfH * cos + y;
		quad[22] = z;
		quad[23] = w;
	}

	static native void fillBuffer(FloatBuffer buffer, FloatBuffer vertices, int[] indices);

	static native void glReadPixels(int x, int y, int width, int height, Bitmap bitmapBuffer);

	static native void transform(FloatBuffer srcVertices, FloatBuffer dstVertices,
								 FloatBuffer srcNormals, FloatBuffer dstNormals,
								 ByteBuffer boneMatrices, float[] actionMatrices);

	static {
		System.loadLibrary("micro3d");
	}
}