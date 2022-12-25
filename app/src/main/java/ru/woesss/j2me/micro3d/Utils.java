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

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Utils {
	static final String TAG = "micro3d";

	static void getSpriteVertex(float[] quad, float[] center, int angle, float halfW, float halfH) {
		float r = angle * MathUtil.TO_RADIANS;
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