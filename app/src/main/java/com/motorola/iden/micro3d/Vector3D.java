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

public class Vector3D {
	public static final int MAX_VALUE = 32767;
	public static final int MIN_VALUE = -32768;
	public static final int SIZE_OF_VECTOR3D = 3;

	public Vector3D() {
	}

	public Vector3D(int x, int y, int z) {
	}

	public int getX() {
		return 0;
	}

	public int getY() {
		return 0;
	}

	public int getZ() {
		return 0;
	}

	public int innerProduct(Vector3D multiplier) {
		return 0;
	}

	public void normalize() {
	}

	public static Vector3D normalize(Vector3D vector) {
		return null;
	}

	public void outerProduct(Vector3D multiplier) {
	}

	public static Vector3D outerProduct(Vector3D multiplicand, Vector3D multiplier) {
		return null;
	}

	public void set(int x, int y, int z) {
	}

	public void setX(int x) {
	}

	public void setY(int y) {
	}

	public void setZ(int z) {
	}
}
