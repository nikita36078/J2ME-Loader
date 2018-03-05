/*
 * Copyright 2018 Nikita Shakarun
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

public class Vector3D {
	public int x;
	public int y;
	public int z;

	public Vector3D() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
	}

	public Vector3D(Vector3D v) {
		set(v);
	}

	public Vector3D(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public final void unit() {
	}

	public final int getX() {
		return this.x;
	}

	public final int getY() {
		return this.y;
	}

	public final int getZ() {
		return this.z;
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

	public final void set(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
	}

	public final void set(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public final int innerProduct(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
		return 0;
	}

	public final void outerProduct(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
	}

	public static final int innerProduct(Vector3D v1, Vector3D v2) {
		if (v1 != null && v2 != null) {
			return 0;
		}
		throw new NullPointerException();
	}

	public static final Vector3D outerProduct(Vector3D v1, Vector3D v2) {
		if (v1 == null || v2 == null) {
			throw new NullPointerException();
		}
		Vector3D r = new Vector3D();
		return r;
	}
}
