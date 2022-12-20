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

import com.mascotcapsule.micro3d.v3.Util3D;
import com.mascotcapsule.micro3d.v3.Vector3D;

public class Vec3i {
	private final Vector3D target;

	public Vec3i(Vector3D target) {
		this.target = target;
	}

	public final void unit() {
		int x = target.getX();
		int y = target.getY();
		int z = target.getZ();
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
		int i = Util3D.sqrt(x * x + y * y + z * z);
		if (i != 0) {
			target.setX((x << 12) / i);
			target.setY((y << 12) / i);
			target.setZ(((z << 12) / i));
		} else {
			target.setX(0);
			target.setY(0);
			target.setZ(4096);
		}
	}

	public final int getX() {
		return target.getX();
	}

	public final void setX(int x) {
		target.setX(x);
	}

	public final int getY() {
		return target.getY();
	}

	public final void setY(int y) {
		target.setY(y);
	}

	public final int getZ() {
		return target.getZ();
	}

	public final void setZ(int z) {
		target.setZ(z);
	}

	public final void set(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
		target.x = v.x;
		target.y = v.y;
		target.z = v.z;
	}

	public final void set(int x, int y, int z) {
		target.x = x;
		target.y = y;
		target.z = z;
	}

}