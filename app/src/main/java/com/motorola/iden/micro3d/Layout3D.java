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

public class Layout3D {
	public static final int PARALLEL_SCALE = 24;
	public static final int PARALLEL_WIDTH = 25;
	public static final int PARALLEL_WIDTH_HEIGHT = 26;
	public static final int PERSPECTIVE_FOV = 21;
	public static final int PERSPECTIVE_WIDTH = 22;
	public static final int PERSPECTIVE_WIDTH_HEIGHT = 23;


	public Layout3D() {
	}

	public Light getLight() {
		return null;
	}

	public int[] getProjectionParameters() {
		return null;
	}

	public int getProjectionType() {
		return 0;
	}

	public int getToonHighColor() {
		return 0;
	}

	public int getToonLowColor() {
		return 0;
	}

	public int getToonThreshold() {
		return 0;
	}

	public AffineTransform getViewPointTransform() {
		return null;
	}

	public AffineTransform getViewTransform() {
		return null;
	}

	public boolean isSemiTransparent() {
		return false;
	}

	public boolean isToonShaded() {
		return false;
	}

	public void rotateV(Vector3D axis, int angle) {
	}

	public void rotateX(int angle) {
	}

	public void rotateY(int angle) {
	}

	public void rotateZ(int angle) {
	}

	public void setLight(Light light) {
	}

	public void setProjection(int type, int[] parameters) {
	}

	public void setSemiTransparent(boolean transparent) {
	}

	public void setToonShading(boolean toon) {
	}

	public void setToonShading(int threshold, int highColor, int lowColor) {
	}

	public void setViewPoint(Vector3D position, Vector3D look, Vector3D up) {
	}

	public void setViewPointTransform(AffineTransform viewPointTransform) {
	}

	public void setViewTransform(AffineTransform viewTransform) {
	}
}
