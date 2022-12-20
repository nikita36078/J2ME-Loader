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

import android.graphics.Bitmap;

import com.mascotcapsule.micro3d.v3.Util3D;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Utils {
	public static final String TAG = "micro3d";

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

	public static int uSqrt(int p) {
		if (p == 0) return 0;
		double a;
		if (p < 0) {
			if (p > 0xfffd0002) return 0xffff;
			a = p & 0xffffffffL;
		} else {
			a = p;
		}
		return (int) Math.round(Math.sqrt(a));
	}

	public static int iSin(int p) {
		double radian = p * Math.PI / 2048;
		return (int) Math.round(Math.sin(radian) * 4096);
	}

	public static int iCos(int p) {
		return Util3D.sin(p + 1024);
	}

	public static int iSqrt(int x) {
		if (x == 0) return 0;
		if (x < 0) {
			throw new IllegalArgumentException("Negative arg=" + x);
		}
		return (int) Math.round(Math.sqrt(x));
	}

	static {
		System.loadLibrary("micro3d");
	}
}