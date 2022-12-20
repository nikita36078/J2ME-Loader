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

import static ru.woesss.j2me.micro3d.Utils.TO_FLOAT;

import com.mascotcapsule.micro3d.v3.AffineTrans;
import com.mascotcapsule.micro3d.v3.Util3D;
import com.mascotcapsule.micro3d.v3.Vector3D;

public class AffineTransImpl {
	private final AffineTrans target;

	public AffineTransImpl(AffineTrans affineTrans) {
		this.target = affineTrans;
	}

	public final void setIdentity() {
		target.m00 = 4096;
		target.m01 = 0;
		target.m02 = 0;
		target.m03 = 0;
		target.m10 = 0;
		target.m11 = 4096;
		target.m12 = 0;
		target.m13 = 0;
		target.m20 = 0;
		target.m21 = 0;
		target.m22 = 4096;
		target.m23 = 0;
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
		a[offset++] = target.m00;
		a[offset++] = target.m01;
		a[offset++] = target.m02;
		a[offset++] = target.m03;
		a[offset++] = target.m10;
		a[offset++] = target.m11;
		a[offset++] = target.m12;
		a[offset++] = target.m13;
		a[offset++] = target.m20;
		a[offset++] = target.m21;
		a[offset++] = target.m22;
		a[offset  ] = target.m23;
	}

	public final void set(int[] a, int offset) {
		if (a == null) {
			throw new NullPointerException();
		}
		if (offset < 0 || a.length - offset < 12) {
			throw new IllegalArgumentException();
		}
		target.m00 = a[offset++];
		target.m01 = a[offset++];
		target.m02 = a[offset++];
		target.m03 = a[offset++];
		target.m10 = a[offset++];
		target.m11 = a[offset++];
		target.m12 = a[offset++];
		target.m13 = a[offset++];
		target.m20 = a[offset++];
		target.m21 = a[offset++];
		target.m22 = a[offset++];
		target.m23 = a[offset];
	}

	public final void set(int m00, int m01, int m02, int m03,
						  int m10, int m11, int m12, int m13,
						  int m20, int m21, int m22, int m23) {

		target.m00 = m00;
		target.m01 = m01;
		target.m02 = m02;
		target.m03 = m03;
		target.m10 = m10;
		target.m11 = m11;
		target.m12 = m12;
		target.m13 = m13;
		target.m20 = m20;
		target.m21 = m21;
		target.m22 = m22;
		target.m23 = m23;
	}

	public final void set(AffineTrans a) {
		if (a == null) {
			throw new NullPointerException();
		}

		target.m00 = a.m00;
		target.m01 = a.m01;
		target.m02 = a.m02;
		target.m03 = a.m03;
		target.m10 = a.m10;
		target.m11 = a.m11;
		target.m12 = a.m12;
		target.m13 = a.m13;
		target.m20 = a.m20;
		target.m21 = a.m21;
		target.m22 = a.m22;
		target.m23 = a.m23;
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

		target.m00 = a[0][0];
		target.m01 = a[0][1];
		target.m02 = a[0][2];
		target.m03 = a[0][3];
		target.m10 = a[1][0];
		target.m11 = a[1][1];
		target.m12 = a[1][2];
		target.m13 = a[1][3];
		target.m20 = a[2][0];
		target.m21 = a[2][1];
		target.m22 = a[2][2];
		target.m23 = a[2][3];
	}

	public final void set(int[] a) {
		set(a, 0);
	}

	public final Vector3D transform(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
		int x = (v.x * target.m00 + v.y * target.m01 + v.z * target.m02 + 2048 >> 12) + target.m03;
		int y = (v.x * target.m10 + v.y * target.m11 + v.z * target.m12 + 2048 >> 12) + target.m13;
		int z = (v.x * target.m20 + v.y * target.m21 + v.z * target.m22 + 2048 >> 12) + target.m23;
		return new Vector3D(x, y, z);
	}

	public final void rotationX(int r) {
		int cos = Util3D.cos(r);
		int sin = Util3D.sin(r);

		target.m00 = 4096;
		target.m01 = 0;
		target.m02 = 0;
		target.m10 = 0;
		target.m11 = cos;
		target.m12 = -sin;
		target.m20 = 0;
		target.m21 = sin;
		target.m22 = cos;
	}

	public final void rotationY(int r) {
		int cos = Util3D.cos(r);
		int sin = Util3D.sin(r);

		target.m00 = cos;
		target.m01 = 0;
		target.m02 = sin;
		target.m10 = 0;
		target.m11 = 4096;
		target.m12 = 0;
		target.m20 = -sin;
		target.m21 = 0;
		target.m22 = cos;
	}

	public final void rotationZ(int r) {
		int cos = Util3D.cos(r);
		int sin = Util3D.sin(r);

		target.m00 = cos;
		target.m01 = -sin;
		target.m02 = 0;
		target.m10 = sin;
		target.m11 = cos;
		target.m12 = 0;
		target.m20 = 0;
		target.m21 = 0;
		target.m22 = 4096;
	}

	public final void mul(AffineTrans a) {
		if (a == null) {
			throw new NullPointerException();
		}
		mulA2(target, a);
	}

	public final void mul(AffineTrans a1, AffineTrans a2) {
		if (a1 == null || a2 == null) {
			throw new NullPointerException();
		}
		mulA2(a1, a2);
	}

	public final void setRotation(Vector3D v, int r) {
		if (v == null) {
			throw new NullPointerException();
		}
		int x = v.x;
		int y = v.y;
		int z = v.z;
		int cos = Util3D.cos(r);
		int sin = Util3D.sin(r);
		int xs = x * sin + 2048 >> 12;
		int ys = y * sin + 2048 >> 12;
		int zs = z * sin + 2048 >> 12;
		int nc = 4096 - cos;
		int xync = (x * y + 2048 >> 12) * nc + 2048 >> 12;
		int yznc = (y * z + 2048 >> 12) * nc + 2048 >> 12;
		int zxnc = (x * z + 2048 >> 12) * nc + 2048 >> 12;

		target.m00 = cos + ((x * x + 2048 >> 12) * nc + 2048 >> 12);
		target.m01 = xync - zs;
		target.m02 = zxnc + ys;
		target.m10 = zs + xync;
		target.m11 = cos + ((y * y + 2048 >> 12) * nc + 2048 >> 12);
		target.m20 = zxnc - ys;
		target.m12 = yznc - xs;
		target.m21 = xs + yznc;
		target.m22 = cos + ((z * z + 2048 >> 12) * nc + 2048 >> 12);
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

		target.m00 = tmp.x;
		target.m01 = tmp.y;
		target.m02 = tmp.z;
		target.m03 = mpx * tmp.x + mpy * tmp.y + mpz * tmp.z + 2048 >> 12;

		tmp = Vector3D.outerProduct(look, tmp);
		tmp.unit();
		target.m10 = tmp.x;
		target.m11 = tmp.y;
		target.m12 = tmp.z;
		target.m13 = mpx * tmp.x + mpy * tmp.y + mpz * tmp.z + 2048 >> 12;

		tmp.set(look);
		tmp.unit();
		target.m20 = tmp.x;
		target.m21 = tmp.y;
		target.m22 = tmp.z;
		target.m23 = mpx * tmp.x + mpy * tmp.y + mpz * tmp.z + 2048 >> 12;
	}

	public void mulA2(AffineTrans a1, AffineTrans a2) {
		int l00 = a1.m00;
		int l01 = a1.m01;
		int l02 = a1.m02;
		int l10 = a1.m10;
		int l11 = a1.m11;
		int l12 = a1.m12;
		int l20 = a1.m20;
		int l21 = a1.m21;
		int l22 = a1.m22;
		int r00 = a2.m00;
		int r01 = a2.m01;
		int r02 = a2.m02;
		int r03 = a2.m03;
		int r10 = a2.m10;
		int r11 = a2.m11;
		int r12 = a2.m12;
		int r13 = a2.m13;
		int r20 = a2.m20;
		int r21 = a2.m21;
		int r22 = a2.m22;
		int r23 = a2.m23;

		target.m00 = l00 * r00 + l01 * r10 + l02 * r20 + 2048 >> 12;
		target.m01 = l00 * r01 + l01 * r11 + l02 * r21 + 2048 >> 12;
		target.m02 = l00 * r02 + l01 * r12 + l02 * r22 + 2048 >> 12;
		target.m03 = (l00 * r03 + l01 * r13 + l02 * r23 + 2048 >> 12) + a1.m03;
		target.m10 = l10 * r00 + l11 * r10 + l12 * r20 + 2048 >> 12;
		target.m11 = l10 * r01 + l11 * r11 + l12 * r21 + 2048 >> 12;
		target.m12 = l10 * r02 + l11 * r12 + l12 * r22 + 2048 >> 12;
		target.m13 = (l10 * r03 + l11 * r13 + l12 * r23 + 2048 >> 12) + a1.m13;
		target.m20 = l20 * r00 + l21 * r10 + l22 * r20 + 2048 >> 12;
		target.m21 = l20 * r01 + l21 * r11 + l22 * r21 + 2048 >> 12;
		target.m22 = l20 * r02 + l21 * r12 + l22 * r22 + 2048 >> 12;
		target.m23 = (l20 * r03 + l21 * r13 + l22 * r23 + 2048 >> 12) + a1.m23;
	}
}