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

import ru.woesss.j2me.micro3d.MathUtil;

@SuppressWarnings({"WeakerAccess", "unused"})
public class AffineTrans {
	public int m00, m01, m02, m03;
	public int m10, m11, m12, m13;
	public int m20, m21, m22, m23;

	public AffineTrans() {}

	public AffineTrans(AffineTrans a) {
		set(a);
	}

	public AffineTrans(int[] a) {
		set(a);
	}

	public AffineTrans(int[][] a) {
		set(a);
	}

	public AffineTrans(int[] a, int offset) {
		set(a, offset);
	}

	public AffineTrans(int m00, int m01, int m02, int m03,
					   int m10, int m11, int m12, int m13,
					   int m20, int m21, int m22, int m23) {
		this.m00 = m00; this.m01 = m01; this.m02 = m02; this.m03 = m03;
		this.m10 = m10; this.m11 = m11; this.m12 = m12; this.m13 = m13;
		this.m20 = m20; this.m21 = m21; this.m22 = m22; this.m23 = m23;
	}

	public final void get(int[] a) {
		get(a, 0);
	}

	public final void get(int[] a, int offset) {
		if (a == null) {
			throw new NullPointerException();
		}
		if (offset < 0 || a.length - offset < 12) {
			throw new IllegalArgumentException();
		}
		a[offset++] = m00;
		a[offset++] = m01;
		a[offset++] = m02;
		a[offset++] = m03;
		a[offset++] = m10;
		a[offset++] = m11;
		a[offset++] = m12;
		a[offset++] = m13;
		a[offset++] = m20;
		a[offset++] = m21;
		a[offset++] = m22;
		a[offset  ] = m23;
	}

	public final void lookAt(Vector3D pos, Vector3D look, Vector3D up) {
		if (pos == null || look == null || up == null) {
			throw new NullPointerException();
		}

		int mpx = -pos.x;
		int mpy = -pos.y;
		int mpz = -pos.z;

		Vector3D tmp = Vector3D.outerProduct(look, up);
		tmp.unit();
		m00 = tmp.x;
		m01 = tmp.y;
		m02 = tmp.z;
		m03 = mpx * tmp.x + mpy * tmp.y + mpz * tmp.z + 2048 >> 12;

		tmp = Vector3D.outerProduct(look, tmp);
		tmp.unit();
		m10 = tmp.x;
		m11 = tmp.y;
		m12 = tmp.z;
		m13 = mpx * tmp.x + mpy * tmp.y + mpz * tmp.z + 2048 >> 12;

		tmp.set(look);
		tmp.unit();
		m20 = tmp.x;
		m21 = tmp.y;
		m22 = tmp.z;
		m23 = mpx * tmp.x + mpy * tmp.y + mpz * tmp.z + 2048 >> 12;
	}

	public final void mul(AffineTrans a) {
		if (a == null) {
			throw new NullPointerException();
		}
		mulA2(this, a);
	}

	public final void mul(AffineTrans a1, AffineTrans a2) {
		if (a1 == null || a2 == null) {
			throw new NullPointerException();
		}
		mulA2(a1, a2);
	}

	@Deprecated
	public final void multiply(AffineTrans a) {
		if (a == null) {
			throw new NullPointerException();
		}
		mulA2(this, a);
	}

	@Deprecated
	public final void multiply(AffineTrans a1, AffineTrans a2) {
		if (a1 == null || a2 == null) {
			throw new NullPointerException();
		}
		mulA2(a1, a2);
	}

	@Deprecated
	public final void rotationV(Vector3D v, int r) {
		setRotation(v, r);
	}

	public final void rotationX(int r) {
		int cos = MathUtil.iCos(r);
		int sin = MathUtil.iSin(r);
		m00 = 4096; m01 =   0; m02 =    0;
		m10 =    0; m11 = cos; m12 = -sin;
		m20 =    0; m21 = sin; m22 =  cos;
	}

	public final void rotationY(int r) {
		int cos = MathUtil.iCos(r);
		int sin = MathUtil.iSin(r);
		m00 =  cos; m01 =    0; m02 = sin;
		m10 =    0; m11 = 4096; m12 =   0;
		m20 = -sin; m21 =    0; m22 = cos;
	}

	public final void rotationZ(int r) {
		int cos = MathUtil.iCos(r);
		int sin = MathUtil.iSin(r);
		m00 = cos; m01 = -sin; m02 =    0;
		m10 = sin; m11 =  cos; m12 =    0;
		m20 =   0; m21 =    0; m22 = 4096;
	}

	public final void set(AffineTrans a) {
		if (a == null) {
			throw new NullPointerException();
		}
		m00 = a.m00; m01 = a.m01; m02 = a.m02; m03 = a.m03;
		m10 = a.m10; m11 = a.m11; m12 = a.m12; m13 = a.m13;
		m20 = a.m20; m21 = a.m21; m22 = a.m22; m23 = a.m23;
	}

	public final void set(int[] a) {
		set(a, 0);
	}

	public final void set(int[][] a) {
		if (a == null) {
			throw new NullPointerException();
		}
		if (a.length < 3) {
			throw new IllegalArgumentException();
		}
		if (a[0].length < 4 || (a[1].length < 4) || (a[2].length < 4)) {
			throw new IllegalArgumentException();
		}
		m00 = a[0][0]; m01 = a[0][1]; m02 = a[0][2]; m03 = a[0][3];
		m10 = a[1][0]; m11 = a[1][1]; m12 = a[1][2]; m13 = a[1][3];
		m20 = a[2][0]; m21 = a[2][1]; m22 = a[2][2]; m23 = a[2][3];
	}

	public final void set(int[] a, int offset) {
		if (a == null) {
			throw new NullPointerException();
		}
		if (offset < 0 || a.length - offset < 12) {
			throw new IllegalArgumentException();
		}
		m00 = a[offset++];
		m01 = a[offset++];
		m02 = a[offset++];
		m03 = a[offset++];
		m10 = a[offset++];
		m11 = a[offset++];
		m12 = a[offset++];
		m13 = a[offset++];
		m20 = a[offset++];
		m21 = a[offset++];
		m22 = a[offset++];
		m23 = a[offset];
	}

	public final void set(int m00, int m01, int m02, int m03,
						  int m10, int m11, int m12, int m13,
						  int m20, int m21, int m22, int m23) {
		this.m00 = m00; this.m01 = m01; this.m02 = m02; this.m03 = m03;
		this.m10 = m10; this.m11 = m11; this.m12 = m12; this.m13 = m13;
		this.m20 = m20; this.m21 = m21; this.m22 = m22; this.m23 = m23;
	}

	public final void setIdentity() {
		m00 = 4096; m01 =    0; m02 =    0; m03 = 0;
		m10 =    0; m11 = 4096; m12 =    0; m13 = 0;
		m20 =    0; m21 =    0; m22 = 4096; m23 = 0;
	}

	public final void setRotation(Vector3D v, int r) {
		if (v == null) {
			throw new NullPointerException();
		}
		int x = v.x;
		int y = v.y;
		int z = v.z;
		int cos = MathUtil.iCos(r);
		int sin = MathUtil.iSin(r);
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
		m10 = zs + xync;
		m11 = cos + ((y * y + 2048 >> 12) * nc + 2048 >> 12);
		m20 = zxnc - ys;
		m12 = yznc - xs;
		m21 = xs + yznc;
		m22 = cos + ((z * z + 2048 >> 12) * nc + 2048 >> 12);
	}

	@Deprecated
	public final void setRotationX(int r) {
		rotationX(r);
	}

	@Deprecated
	public final void setRotationY(int r) {
		rotationY(r);
	}

	@Deprecated
	public final void setRotationZ(int r) {
		rotationZ(r);
	}

	@Deprecated
	public final void setViewTrans(Vector3D pos, Vector3D look, Vector3D up) {
		lookAt(pos, look, up);
	}

	public final Vector3D transform(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
		int x = (v.x * m00 + v.y * m01 + v.z * m02 + 2048 >> 12) + m03;
		int y = (v.x * m10 + v.y * m11 + v.z * m12 + 2048 >> 12) + m13;
		int z = (v.x * m20 + v.y * m21 + v.z * m22 + 2048 >> 12) + m23;
		return new Vector3D(x, y, z);
	}

	@Deprecated
	public final Vector3D transPoint(Vector3D v) {
		return transform(v);
	}

	private void mulA2(AffineTrans a1, AffineTrans a2) {
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
