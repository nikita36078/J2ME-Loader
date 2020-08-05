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

package com.vodafone.v10.graphics.j3d;

public class AffineTrans extends com.mascotcapsule.micro3d.v3.AffineTrans {
	public AffineTrans() {}

	public AffineTrans(int[][] a) {
		super(a);
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
		super.multiply(a);
	}

	public void multiply(AffineTrans a1, AffineTrans a2) {
		super.multiply(a1, a2);
	}

	public void rotationV(Vector3D v, int r) {
		super.setRotation(v, r);
	}

	public void setViewTrans(Vector3D pos, Vector3D look, Vector3D up) {
		super.setViewTrans(pos, look, up);
	}
}