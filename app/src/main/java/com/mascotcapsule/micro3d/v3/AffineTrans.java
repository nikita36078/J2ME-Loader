/*
 * Copyright 2018 Yury Kharchenko
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressWarnings({"WeakerAccess", "unused"})
public class AffineTrans {
	public int m00;
	public int m01;
	public int m02;
	public int m03;
	public int m10;
	public int m11;
	public int m12;
	public int m13;
	public int m20;
	public int m21;
	public int m22;
	public int m23;

	public AffineTrans(int m00, int m01, int m02, int m03,
					   int m10, int m11, int m12, int m13,
					   int m20, int m21, int m22, int m23) {
		set(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23);
	}

	public AffineTrans(AffineTrans a) {
		set(a);
	}

	public AffineTrans(int[][] a) {
		set(a);
	}

	public AffineTrans(int[] a) {
		set(a);
	}

	public AffineTrans(int[] a, int offset) {
		set(a, offset);
	}

	public AffineTrans() {
	}

	public final void setRotationX(int r) {
		rotationX(r);
	}

	public final void setRotationY(int r) {
		rotationY(r);
	}

	public final void setRotationZ(int r) {
		rotationZ(r);
	}

	public final void setIdentity() {
		set(4096, 0, 0, 0, 0, 4096, 0, 0, 0, 0, 4096, 0);
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
		a[offset++] = this.m00;
		a[offset++] = this.m01;
		a[offset++] = this.m02;
		a[offset++] = this.m03;
		a[offset++] = this.m10;
		a[offset++] = this.m11;
		a[offset++] = this.m12;
		a[offset++] = this.m13;
		a[offset++] = this.m20;
		a[offset++] = this.m21;
		a[offset++] = this.m22;
		a[offset] = this.m23;
	}

	public final void set(int[] a, int offset) {
		if (a == null) {
			throw new NullPointerException();
		}
		if (offset < 0 || a.length - offset < 12) {
			throw new IllegalArgumentException();
		}
		this.m00 = a[offset++];
		this.m01 = a[offset++];
		this.m02 = a[offset++];
		this.m03 = a[offset++];
		this.m10 = a[offset++];
		this.m11 = a[offset++];
		this.m12 = a[offset++];
		this.m13 = a[offset++];
		this.m20 = a[offset++];
		this.m21 = a[offset++];
		this.m22 = a[offset++];
		this.m23 = a[offset];
	}

	public final void set(int m00, int m01, int m02, int m03,
						  int m10, int m11, int m12, int m13,
						  int m20, int m21, int m22, int m23) {
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;
		this.m03 = m03;
		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;
		this.m13 = m13;
		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;
		this.m23 = m23;
	}

	public final void set(AffineTrans a) {
		if (a == null) {
			throw new NullPointerException();
		}
		this.m00 = a.m00;
		this.m01 = a.m01;
		this.m02 = a.m02;
		this.m03 = a.m03;
		this.m10 = a.m10;
		this.m11 = a.m11;
		this.m12 = a.m12;
		this.m13 = a.m13;
		this.m20 = a.m20;
		this.m21 = a.m21;
		this.m22 = a.m22;
		this.m23 = a.m23;
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
		this.m00 = a[0][0];
		this.m01 = a[0][1];
		this.m02 = a[0][2];
		this.m03 = a[0][3];
		this.m10 = a[1][0];
		this.m11 = a[1][1];
		this.m12 = a[1][2];
		this.m13 = a[1][3];
		this.m20 = a[2][0];
		this.m21 = a[2][1];
		this.m22 = a[2][2];
		this.m23 = a[2][3];
	}

	public final void set(int[] a) {
		set(a, 0);
	}

	public final Vector3D transPoint(Vector3D v) {
		return transform(v);
	}

	public final Vector3D transform(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
		int x = (v.x * m00 + v.y * m01 + v.z * m02 >> 12) + m03;
		int y = (v.x * m10 + v.y * m11 + v.z * m12 >> 12) + m13;
		int z = (v.x * m20 + v.y * m21 + v.z * m22 >> 12) + m23;
		return new Vector3D(x, y, z);
	}

	public final void rotationX(int r) {
		int cos = Util3D.cos(r);
		int sin = Util3D.sin(r);
		m00 = 4096;
		m01 = 0;
		m02 = 0;
		m10 = 0;
		m11 = cos;
		m12 = -sin;
		m20 = 0;
		m21 = sin;
		m22 = cos;
	}

	public final void rotationY(int r) {
		int cos = Util3D.cos(r);
		int sin = Util3D.sin(r);
		m00 = cos;
		m01 = 0;
		m02 = sin;
		m10 = 0;
		m11 = 4096;
		m12 = 0;
		m20 = -sin;
		m21 = 0;
		m22 = cos;
	}

	public final void rotationZ(int r) {
		int cos = Util3D.cos(r);
		int sin = Util3D.sin(r);
		m00 = cos;
		m01 = -sin;
		m02 = 0;
		m10 = sin;
		m11 = cos;
		m12 = 0;
		m20 = 0;
		m21 = 0;
		m22 = 4096;
	}

	public final void multiply(AffineTrans a) {
		mulA2(this, a);
	}

	public final void mul(AffineTrans a) {
		if (a == null) {
			throw new NullPointerException();
		}
		mulA2(this, a);
	}

	public final void multiply(AffineTrans a1, AffineTrans a2) {
		mulA2(a1, a2);
	}

	public final void mul(AffineTrans a1, AffineTrans a2) {
		if (a1 == null || a2 == null) {
			throw new NullPointerException();
		}
		mulA2(a1, a2);
	}

	public final void rotationV(Vector3D v, int r) {
		setRotation(v, r);
	}

	public final void setRotation(Vector3D v, int r) {
		if (v == null) {
			throw new NullPointerException();
		}
		int cos = Util3D.cos(r);
		int sin = Util3D.sin(r);
		int x = v.x;
		int y = v.y;
		int z = v.z;
		int i = sin * z + 2048 >> 12;
		int i1 = (x * y + 2048 >> 12) * (4096 - cos) + 2048 >> 12;
		int i2 = sin * y + 2048 >> 12;
		int i3 = (x * z + 2048 >> 12) * (4096 - cos) + 2048 >> 12;
		int i4 = sin * x + 2048 >> 12;
		int i5 = (y * z + 2048 >> 12) * (4096 - cos) + 2048 >> 12;
		m00 = cos + ((4096 - cos) * (x * x + 2048 >> 12) + 2048 >> 12);
		m01 = i1 - i;
		m02 = i3 + i2;
		m10 = i + i1;
		m11 = cos + ((4096 - cos) * (y * y + 2048 >> 12) + 2048 >> 12);
		m20 = i3 - i2;
		m12 = i5 - i4;
		m21 = i4 + i5;
		m22 = cos + ((4096 - cos) * (z * z + 2048 >> 12) + 2048 >> 12);
	}

	public final void setViewTrans(Vector3D pos, Vector3D look, Vector3D up) {
		lookAt(pos, look, up);
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
		m03 = (mpy * tmp.y + mpz * tmp.z + mpx * tmp.x + 2048) >> 12;

		tmp = Vector3D.outerProduct(look, tmp);
		tmp.unit();
		m10 = tmp.x;
		m11 = tmp.y;
		m12 = tmp.z;
		m13 = (mpy * tmp.y + mpz * tmp.z + mpx * tmp.x + 2048) >> 12;

		tmp = new Vector3D(look);
		tmp.unit();
		m20 = tmp.x;
		m21 = tmp.y;
		m22 = tmp.z;
		m23 = (mpy * tmp.y + mpz * tmp.z + mpx * tmp.x + 2048) >> 12;
	}

	private void mulA2(AffineTrans a1, AffineTrans a2) {
		int m00_1 = a1.m00;
		int m01_1 = a1.m01;
		int m02_1 = a1.m02;
		int m10_1 = a1.m10;
		int m11_1 = a1.m11;
		int m12_1 = a1.m12;
		int m20_1 = a1.m20;
		int m21_1 = a1.m21;
		int m22_1 = a1.m22;
		int m00_2 = a2.m00;
		int m01_2 = a2.m01;
		int m02_2 = a2.m02;
		int m03_2 = a2.m03;
		int m10_2 = a2.m10;
		int m11_2 = a2.m11;
		int m12_2 = a2.m12;
		int m13_2 = a2.m13;
		int m20_2 = a2.m20;
		int m21_2 = a2.m21;
		int m22_2 = a2.m22;
		int m23_2 = a2.m23;
		this.m00 =  m00_1 * m00_2 + m01_1 * m10_2 + m02_1 * m20_2 + 2048 >> 12;
		this.m01 =  m00_1 * m01_2 + m01_1 * m11_2 + m02_1 * m21_2 + 2048 >> 12;
		this.m02 =  m00_1 * m02_2 + m01_1 * m12_2 + m02_1 * m22_2 + 2048 >> 12;
		this.m03 = (m00_1 * m03_2 + m01_1 * m13_2 + m02_1 * m23_2 + 2048 >> 12) + a1.m03;
		this.m10 =  m10_1 * m00_2 + m11_1 * m10_2 + m12_1 * m20_2 + 2048 >> 12;
		this.m11 =  m10_1 * m01_2 + m11_1 * m11_2 + m12_1 * m21_2 + 2048 >> 12;
		this.m12 =  m10_1 * m02_2 + m11_1 * m12_2 + m12_1 * m22_2 + 2048 >> 12;
		this.m13 = (m10_1 * m03_2 + m11_1 * m13_2 + m12_1 * m23_2 + 2048 >> 12) + a1.m13;
		this.m20 =  m20_1 * m00_2 + m21_1 * m10_2 + m22_1 * m20_2 + 2048 >> 12;
		this.m21 =  m20_1 * m01_2 + m21_1 * m11_2 + m22_1 * m21_2 + 2048 >> 12;
		this.m22 =  m20_1 * m02_2 + m21_1 * m12_2 + m22_1 * m22_2 + 2048 >> 12;
		this.m23 = (m20_1 * m03_2 + m21_1 * m13_2 + m22_1 * m23_2 + 2048 >> 12) + a1.m23;
	}

	@NonNull
	@Override
	public String toString() {
		return super.toString() + "{" + m00 + ", " + m01 + ", " + m02 + ", " + m03 + ", "
				+ m10 + ", " + m11 + ", " + m12 + ", " + m13 + ", "
				+ m20 + ", " + m21 + ", " + m22 + ", " + m23 + "}";
	}

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof AffineTrans)) {
            return false;
        }
        AffineTrans o = (AffineTrans) obj;
        return m00 == o.m00 && m01 == o.m01 && m02 == o.m02 && m03 == o.m03
                && m10 == o.m10 && m11 == o.m11 && m12 == o.m12 && m13 == o.m13
                && m20 == o.m20 && m21 == o.m21 && m22 == o.m22 && m23 == o.m23;
    }
}
