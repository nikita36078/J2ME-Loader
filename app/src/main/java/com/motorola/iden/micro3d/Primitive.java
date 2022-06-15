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

public abstract class Primitive extends Object3D {
	public static final int BLENDING_ADD = 202;
	public static final int BLENDING_HALF = 201;
	public static final int BLENDING_NONE = 200;
	public static final int BLENDING_SUB = 203;
	public static final int FACE_NORMAL = 32;
	public static final int NORMAL = 16;
	public static final int VERTEX_A = 1;
	public static final int VERTEX_B = 2;
	public static final int VERTEX_C = 3;
	public static final int VERTEX_D = 4;

	public void enableColorKeyTransparency(boolean enable) {
	}

	public int getBlendingType() {
		return 0;
	}

	public int getColor() {
		return 0;
	}

	public abstract Vector3D getVector(int vectorID);

	public boolean hasColorKeyTransparency() {
		return false;
	}

	public void setBlendingType(int blendingType) {
	}

	public void setColor(int color) {
	}

	public abstract void setVector(int vectorID, Vector3D vector);
}
