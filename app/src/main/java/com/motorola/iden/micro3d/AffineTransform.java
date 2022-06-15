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

public class AffineTransform {
	public static final int M00 = 0;
	public static final int M01 = 1;
	public static final int M02 = 2;
	public static final int M03 = 3;
	public static final int M10 = 4;
	public static final int M11 = 5;
	public static final int M12 = 6;
	public static final int M13 = 7;
	public static final int M20 = 8;
	public static final int M21 = 9;
	public static final int M22 = 10;
	public static final int M23 = 11;
	public static final int MAX_VALUE = 32767;
	public static final int MIN_VALUE = -32768;

	public AffineTransform() {
	}

	public AffineTransform(int[][] elements) {
	}

	public int get(int fieldID) {
		return 0;
	}

	public static AffineTransform getViewPointTransform(Vector3D position,
														Vector3D look,
														Vector3D up) {
		return null;
	}

	public void multiply(AffineTransform multiplier) {
	}

	public static AffineTransform multiply(AffineTransform multiplicand,
										   AffineTransform multiplier) {
		return null;
	}

	public static void multiply(AffineTransform destination,
								AffineTransform multiplicand,
								AffineTransform multiplier) {
	}

	public void normalize() {
	}

	public void rotateV(Vector3D axis, int angle) {
	}

	public void rotateX(int angle) {
	}

	public void rotateY(int angle) {
	}

	public void rotateZ(int angle) {
	}

	public void set(int[][] elements) {
	}

	public void set(int fieldID, int value) {
	}

	public void setIdentity() {
	}

	public Vector3D transformPoint(Vector3D source) {
		return null;
	}
}
