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

package com.motorola.iden.micro3d;

import ru.woesss.j2me.micro3d.MathUtil;

public class AffineTransform {
	public static final int M00 = 0;
	public static final int M01 = 1;
	public static final int M02 = 2;
	public static final int M03 = 3;
	public static final int M10 = 4;
	public static final int M11 = 5;
	public static final int M12 = 6;
	public static final int M13 = 7;
	public static final int M20 = 8;
	public static final int M21 = 9;
	public static final int M22 = 10;
	public static final int M23 = 11;
	public static final int MAX_VALUE = 32767;
	public static final int MIN_VALUE = -32768;

	int m00, m01, m02, m03;
	int m10, m11, m12, m13;
	int m20, m21, m22, m23;

	public AffineTransform() {}

	public AffineTransform(int[][] elements) {
		set(elements);
	}

	public int get(int fieldID) {
		switch (fieldID) {
			case M00: return m00;
			case M01: return m01;
			case M02: return m02;
			case M03: return m03;
			case M10: return m10;
			case M11: return m11;
			case M12: return m12;
			case M13: return m13;
			case M20: return m20;
			case M21: return m21;
			case M22: return m22;
			case M23: return m23;
			default: throw new IllegalArgumentException();
		}
	}

	public static AffineTransform getViewPointTransform(Vector3D position,
														Vector3D look,
														Vector3D up) {
		if (position == null || look == null || up == null) {
			throw new NullPointerException();
		}

		AffineTransform a = new AffineTransform();

		int mpx = -position.x;
		int mpy = -position.y;
		int mpz = -position.z;

		Vector3D tmp = Vector3D.outerProduct(look, up);
		if (tmp.isZero()) {
			throw new IllegalArgumentException();
		}
		tmp.normalize();
		a.m00 = tmp.x;
		a.m01 = tmp.y;
		a.m02 = tmp.z;
		a.m03 = mpx * tmp.x + mpy * tmp.y + mpz * tmp.z + 2048 >> 12;

		tmp = Vector3D.outerProduct(look, tmp);
		tmp.normalize();
		a.m10 = tmp.x;
		a.m11 = tmp.y;
		a.m12 = tmp.z;
		a.m13 = mpx * tmp.x + mpy * tmp.y + mpz * tmp.z + 2048 >> 12;

		tmp.set(look);
		tmp.normalize();
		a.m20 = tmp.x;
		a.m21 = tmp.y;
		a.m22 = tmp.z;
		a.m23 = mpx * tmp.x + mpy * tmp.y + mpz * tmp.z + 2048 >> 12;
		return a;
	}

	public void multiply(AffineTransform multiplier) {
		mulA2(this, multiplier);
	}

	public static AffineTransform multiply(AffineTransform multiplicand,
										   AffineTransform multiplier) {
		AffineTransform result = new AffineTransform();
		result.mulA2(multiplicand, multiplier);
		return result;
	}

	public static void multiply(AffineTransform destination,
								AffineTransform multiplicand,
								AffineTransform multiplier) {
		destination.mulA2(multiplicand, multiplier);
	}

	public void normalize() {
		if ((m00 & m01 & m02 & m10 & m11 & m12 & m20 & m21 & m22) == 0) {
			return;
		}

		Vector3D fv = new Vector3D(this.m00, this.m10, this.m20);
		fv.normalize();
		this.m00 = fv.x;
		this.m10 = fv.y;
		this.m20 = fv.z;

		Vector3D sv = new Vector3D(this.m01, this.m11, this.m21);
		sv = Vector3D.outerProduct(fv, sv);
		sv.normalize();
		this.m02 = sv.x;
		this.m12 = sv.y;
		this.m22 = sv.z;

		sv.outerProduct(fv);
		this.m01 = sv.x >> 12;
		this.m11 = sv.y >> 12;
		this.m21 = sv.z >> 12;
	}

	public void rotateV(Vector3D axis, int angle) {
		if (axis == null) {
			throw new NullPointerException();
		}
		int x = axis.x;
		int y = axis.y;
		int z = axis.z;
		int cos = MathUtil.iCos(angle);
		int sin = MathUtil.iSin(angle);
		int xs = x * sin + 2048 >> 12;
		int ys = y * sin + 2048 >> 12;
		int zs = z * sin + 2048 >> 12;
		int nc = 4096 - cos;
		int xync = (x * y + 2048 >> 12) * nc + 2048 >> 12;
		int yznc = (y * z + 2048 >> 12) * nc + 2048 >> 12;
		int zxnc = (x * z + 2048 >> 12) * nc + 2048 >> 12;
		m00 = cos + ((x * x + 2048 >> 12) * nc + 2048 >> 12);
		m01 = xync - zs;
		m02 = zxnc + ys;
		m10 = xync + zs;
		m11 = cos + ((y * y + 2048 >> 12) * nc + 2048 >> 12);
		m20 = zxnc - ys;
		m12 = yznc - xs;
		m21 = yznc + xs;
		m22 = cos + ((z * z + 2048 >> 12) * nc + 2048 >> 12);
	}

	public void rotateX(int angle) {
		int cos = MathUtil.iCos(angle);
		int sin = MathUtil.iSin(angle);
		m00 = 4096; m01 =   0; m02 =    0;
		m10 =    0; m11 = cos; m12 = -sin;
		m20 =    0; m21 = sin; m22 =  cos;
	}

	public void rotateY(int angle) {
		int cos = MathUtil.iCos(angle);
		int sin = MathUtil.iSin(angle);
		m00 =  cos; m01 =    0; m02 = sin;
		m10 =    0; m11 = 4096; m12 =   0;
		m20 = -sin; m21 =    0; m22 = cos;
	}

	public void rotateZ(int angle) {
		int cos = MathUtil.iCos(angle);
		int sin = MathUtil.iSin(angle);
		m00 = cos; m01 = -sin; m02 =    0;
		m10 = sin; m11 =  cos; m12 =    0;
		m20 =   0; m21 =    0; m22 = 4096;
	}

	public void set(int[][] elements) {
		if (elements == null) {
			throw new NullPointerException();
		}
		if (elements.length < 3) {
			throw new IllegalArgumentException();
		}
		if (elements[0].length < 4 || (elements[1].length < 4) || (elements[2].length < 4)) {
			throw new IllegalArgumentException();
		}
		m00 = MathUtil.clamp(elements[0][0], MIN_VALUE, MAX_VALUE);
		m01 = MathUtil.clamp(elements[0][1], MIN_VALUE, MAX_VALUE);
		m02 = MathUtil.clamp(elements[0][2], MIN_VALUE, MAX_VALUE);
		m03 = MathUtil.clamp(elements[0][3], MIN_VALUE, MAX_VALUE);
		m10 = MathUtil.clamp(elements[1][0], MIN_VALUE, MAX_VALUE);
		m11 = MathUtil.clamp(elements[1][1], MIN_VALUE, MAX_VALUE);
		m12 = MathUtil.clamp(elements[1][2], MIN_VALUE, MAX_VALUE);
		m13 = MathUtil.clamp(elements[1][3], MIN_VALUE, MAX_VALUE);
		m20 = MathUtil.clamp(elements[2][0], MIN_VALUE, MAX_VALUE);
		m21 = MathUtil.clamp(elements[2][1], MIN_VALUE, MAX_VALUE);
		m22 = MathUtil.clamp(elements[2][2], MIN_VALUE, MAX_VALUE);
		m23 = MathUtil.clamp(elements[2][3], MIN_VALUE, MAX_VALUE);
	}

	public void set(int fieldID, int value) {
		switch (fieldID) {
			case M00: m00 = MathUtil.clamp(value, MIN_VALUE, MAX_VALUE);
			case M01: m01 = MathUtil.clamp(value, MIN_VALUE, MAX_VALUE);
			case M02: m02 = MathUtil.clamp(value, MIN_VALUE, MAX_VALUE);
			case M03: m03 = MathUtil.clamp(value, MIN_VALUE, MAX_VALUE);
			case M10: m10 = MathUtil.clamp(value, MIN_VALUE, MAX_VALUE);
			case M11: m11 = MathUtil.clamp(value, MIN_VALUE, MAX_VALUE);
			case M12: m12 = MathUtil.clamp(value, MIN_VALUE, MAX_VALUE);
			case M13: m13 = MathUtil.clamp(value, MIN_VALUE, MAX_VALUE);
			case M20: m20 = MathUtil.clamp(value, MIN_VALUE, MAX_VALUE);
			case M21: m21 = MathUtil.clamp(value, MIN_VALUE, MAX_VALUE);
			case M22: m22 = MathUtil.clamp(value, MIN_VALUE, MAX_VALUE);
			case M23: m23 = MathUtil.clamp(value, MIN_VALUE, MAX_VALUE);
			default: throw new IllegalArgumentException();
		}
	}

	public void setIdentity() {
		m00 = 4096; m01 =    0; m02 =    0; m03 = 0;
		m10 =    0; m11 = 4096; m12 =    0; m13 = 0;
		m20 =    0; m21 =    0; m22 = 4096; m23 = 0;
	}

	public Vector3D transformPoint(Vector3D source) {
		if (source == null) {
			throw new NullPointerException();
		}

		Vector3D r = new Vector3D();
		r.x = (source.x * m00 + source.y * m01 + source.z * m02 + 2048 >> 12) + m03;
		r.y = (source.x * m10 + source.y * m11 + source.z * m12 + 2048 >> 12) + m13;
		r.z = (source.x * m20 + source.y * m21 + source.z * m22 + 2048 >> 12) + m23;
		return r;
	}

	private void mulA2(AffineTransform a1, AffineTransform a2) {
		int l00 = a1.m00, l01 = a1.m01, l02 = a1.m02;
		int l10 = a1.m10, l11 = a1.m11, l12 = a1.m12;
		int l20 = a1.m20, l21 = a1.m21, l22 = a1.m22;

		int r00 = a2.m00, r01 = a2.m01, r02 = a2.m02, r03 = a2.m03;
		int r10 = a2.m10, r11 = a2.m11, r12 = a2.m12, r13 = a2.m13;
		int r20 = a2.m20, r21 = a2.m21, r22 = a2.m22, r23 = a2.m23;

		m00 =  l00 * r00 + l01 * r10 + l02 * r20 + 2048 >> 12;
		m01 =  l00 * r01 + l01 * r11 + l02 * r21 + 2048 >> 12;
		m02 =  l00 * r02 + l01 * r12 + l02 * r22 + 2048 >> 12;
		m03 = (l00 * r03 + l01 * r13 + l02 * r23 + 2048 >> 12) + a1.m03;
		m10 =  l10 * r00 + l11 * r10 + l12 * r20 + 2048 >> 12;
		m11 =  l10 * r01 + l11 * r11 + l12 * r21 + 2048 >> 12;
		m12 =  l10 * r02 + l11 * r12 + l12 * r22 + 2048 >> 12;
		m13 = (l10 * r03 + l11 * r13 + l12 * r23 + 2048 >> 12) + a1.m13;
		m20 =  l20 * r00 + l21 * r10 + l22 * r20 + 2048 >> 12;
		m21 =  l20 * r01 + l21 * r11 + l22 * r21 + 2048 >> 12;
		m22 =  l20 * r02 + l21 * r12 + l22 * r22 + 2048 >> 12;
		m23 = (l20 * r03 + l21 * r13 + l22 * r23 + 2048 >> 12) + a1.m23;
	}
}
