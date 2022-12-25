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

public class Vector3D {
	public int x;
	public int y;
	public int z;

	public Vector3D() {}

	public Vector3D(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector3D(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
		x = v.x;
		y = v.y;
		z = v.z;
	}

	public final int getX() {
		return x;
	}

	public final int getY() {
		return y;
	}

	public final int getZ() {
		return z;
	}

	public final int innerProduct(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
		return x * v.x + y * v.y + z * v.z;
	}

	public static int innerProduct(Vector3D v1, Vector3D v2) {
		if (v1 == null) {
			throw new NullPointerException();
		}
		return v1.innerProduct(v2);
	}

	public final void outerProduct(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
		int x = this.x;
		int y = this.y;
		int z = this.z;
		this.x = y * v.z - z * v.y;
		this.y = z * v.x - x * v.z;
		this.z = x * v.y - y * v.x;
	}

	public static Vector3D outerProduct(Vector3D v1, Vector3D v2) {
		if (v1 == null || v2 == null) {
			throw new NullPointerException();
		}
		int x = v1.y * v2.z - v1.z * v2.y;
		int y = v1.z * v2.x - v1.x * v2.z;
		int z = v1.x * v2.y - v1.y * v2.x;
		return new Vector3D(x, y, z);
	}

	public final void set(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public final void set(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
		x = v.x;
		y = v.y;
		z = v.z;
	}

	public final void setX(int x) {
		this.x = x;
	}

	public final void setY(int y) {
		this.y = y;
	}

	public final void setZ(int z) {
		this.z = z;
	}

	public final void unit() {
		int x = this.x;
		int y = this.y;
		int z = this.z;
		int shift = Integer.numberOfLeadingZeros(Math.abs(x) | Math.abs(y) | Math.abs(z)) - 17;
		if (shift > 0) {
			x <<= shift;
			y <<= shift;
			z <<= shift;
		} else if (shift < 0) {
			shift = -shift;
			x >>= shift;
			y >>= shift;
			z >>= shift;
		}
		int i = MathUtil.uSqrt(x * x + y * y + z * z);
		if (i != 0) {
			this.x = (x << 12) / i;
			this.y = (y << 12) / i;
			this.z = ((z << 12) / i);
		} else {
			this.x = 0;
			this.y = 0;
			this.z = 4096;
		}
	}
}
