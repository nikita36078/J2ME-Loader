/*
 *  Copyright 2020 Yury Kharchenko
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

package com.motorola.graphics.j3d;

public class AffineTrans extends com.mascotcapsule.micro3d.v3.AffineTrans {
	public AffineTrans() {}

	public AffineTrans(int[][] a) {
		super(a);
	}

	AffineTrans(int m00, int m01, int m02, int m03,
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

	public Vector3D transPoint(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
		int x = (v.x * m00 + v.y * m01 + v.z * m02 >> 12) + m03;
		int y = (v.x * m10 + v.y * m11 + v.z * m12 >> 12) + m13;
		int z = (v.x * m20 + v.y * m21 + v.z * m22 >> 12) + m23;
		return new Vector3D(x, y, z);
	}

	public void multiply(AffineTrans a) {
		mul(a);
	}

	public void multiply(AffineTrans a1, AffineTrans a2) {
		mul(a1, a2);
	}

	public void rotationV(Vector3D v, int r) {
		setRotation(v, r);
	}

	public void setViewTrans(Vector3D pos, Vector3D look, Vector3D up) {
		lookAt(pos, look, up);
	}
}