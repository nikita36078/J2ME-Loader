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

package com.jblend.graphics.j3d;

public class Vector3D extends com.mascotcapsule.micro3d.v3.Vector3D {
	public Vector3D() {
	}

	public Vector3D(int x, int y, int z) {
		super(x, y, z);
	}

	public int innerProduct(Vector3D v) {
		return super.innerProduct(v);
	}

	public void outerProduct(Vector3D v) {
		super.outerProduct(v);
	}

	public static int innerProduct(Vector3D v1, Vector3D v2) {
		if (v1 == null || v2 == null) {
			throw new NullPointerException();
		}
		return v1.innerProduct(v2);
	}

	public static Vector3D outerProduct(Vector3D v1, Vector3D v2) {
		if (v1 == null || v2 == null) {
			throw new NullPointerException();
		}
		Vector3D dst = new Vector3D();
		dst.x = v1.y * v2.z - v1.z * v2.y;
		dst.y = v1.z * v2.x - v1.x * v2.z;
		dst.z = v1.x * v2.y - v1.y * v2.x;
		return dst;
	}
}