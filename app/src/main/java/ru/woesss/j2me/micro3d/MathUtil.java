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

public class MathUtil {
	public static final float TO_FLOAT = 2.4414062E-04f;
	static final float TO_RADIANS = (float) (Math.PI / 2048.0);
	static final float[] IDENTITY_AFFINE = {
			1.0f, 0.0f, 0.0f, 0.0f,
			0.0f, 1.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 1.0f, 0.0f
	};

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
		return iSin(p + 1024);
	}

	public static int iSqrt(int x) {
		if (x < 0) {
			throw new IllegalArgumentException("Negative arg=" + x);
		}
		if (x == 0) {
			return 0;
		}
		return (int) Math.round(Math.sqrt(x));
	}

	static void multiplyMM(float[] m, float[] pm, float[] mvm) {
		float l00 = pm[ 0], l01 = pm[ 4], l02 = pm[ 8], l03 = pm[12];
		float l10 = pm[ 1], l11 = pm[ 5], l12 = pm[ 9], l13 = pm[13];
		float l20 = pm[ 2], l21 = pm[ 6], l22 = pm[10], l23 = pm[14];
		float l30 = pm[ 3], l31 = pm[ 7], l32 = pm[11], l33 = pm[15];

		float r00 = mvm[ 0], r01 = mvm[ 3], r02 = mvm[ 6], r03 = mvm[ 9];
		float r10 = mvm[ 1], r11 = mvm[ 4], r12 = mvm[ 7], r13 = mvm[10];
		float r20 = mvm[ 2], r21 = mvm[ 5], r22 = mvm[ 8], r23 = mvm[11];

		m[ 0] = l00 * r00 + l01 * r10 + l02 * r20;
		m[ 1] = l10 * r00 + l11 * r10 + l12 * r20;
		m[ 2] = l20 * r00 + l21 * r10 + l22 * r20;
		m[ 3] = l30 * r00 + l31 * r10 + l32 * r20;
		m[ 4] = l00 * r01 + l01 * r11 + l02 * r21;
		m[ 5] = l10 * r01 + l11 * r11 + l12 * r21;
		m[ 6] = l20 * r01 + l21 * r11 + l22 * r21;
		m[ 7] = l30 * r01 + l31 * r11 + l32 * r21;
		m[ 8] = l00 * r02 + l01 * r12 + l02 * r22;
		m[ 9] = l10 * r02 + l11 * r12 + l12 * r22;
		m[10] = l20 * r02 + l21 * r12 + l22 * r22;
		m[11] = l30 * r02 + l31 * r12 + l32 * r22;
		m[12] = l00 * r03 + l01 * r13 + l02 * r23 + l03;
		m[13] = l10 * r03 + l11 * r13 + l12 * r23 + l13;
		m[14] = l20 * r03 + l21 * r13 + l22 * r23 + l23;
		m[15] = l30 * r03 + l31 * r13 + l32 * r23 + l33;
	}
}
