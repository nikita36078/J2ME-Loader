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

public class Vector3D {
	public static final int MAX_VALUE = 32767;
	public static final int MIN_VALUE = -32768;
	public static final int SIZE_OF_VECTOR3D = 3;

	int x;
	int y;
	int z;

	public Vector3D() {}

	public Vector3D(int x, int y, int z) {
		this.x = clamp(x);
		this.y = clamp(y);
		this.z = clamp(z);
	}

	Vector3D(Vector3D src) {
		x = src.x;
		y = src.y;
		z = src.z;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public int innerProduct(Vector3D multiplier) {
		if (multiplier == null) {
			throw new NullPointerException();
		}
		return x * multiplier.x + y * multiplier.y + z * multiplier.z;
	}

	public void normalize() {
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

	public static Vector3D normalize(Vector3D vector) {
		if (vector == null) {
			throw new NullPointerException();
		}
		if (vector.isZero()) {
			throw new IllegalArgumentException();
		}
		Vector3D result = new Vector3D(vector);
		result.normalize();
		return result;
	}

	public void outerProduct(Vector3D multiplier) {
		if (multiplier == null) {
			throw new NullPointerException();
		}
		int x = this.x;
		int y = this.y;
		int z = this.z;
		this.x = y * multiplier.z - z * multiplier.y;
		this.y = z * multiplier.x - x * multiplier.z;
		this.z = x * multiplier.y - y * multiplier.x;
	}

	public static Vector3D outerProduct(Vector3D multiplicand, Vector3D multiplier) {
		if (multiplicand == null) {
			throw new NullPointerException();
		}
		Vector3D result = new Vector3D(multiplicand);
		result.outerProduct(multiplier);
		return result;
	}

	public void set(int x, int y, int z) {
		this.x = clamp(x);
		this.y = clamp(y);
		this.z = clamp(z);
	}

	public void setX(int x) {
		this.x = clamp(x);
	}

	public void setY(int y) {
		this.y = clamp(y);
	}

	public void setZ(int z) {
		this.z = clamp(z);
	}

	boolean isZero() {
		return (this.x & this.y & this.z) == 0;
	}

	void set(Vector3D src) {
		x = src.x;
		y = src.y;
		z = src.z;
	}

	@SuppressWarnings("ManualMinMaxCalculation")
	private static int clamp(int v) {
		return v >= MAX_VALUE ? MAX_VALUE : v <= MIN_VALUE ? MIN_VALUE : v;
	}
}
