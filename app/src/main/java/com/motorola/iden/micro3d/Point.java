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

public class Point extends Primitive {

	public Point(Vector3D vertexA, Layout3D layout, int color) {
		super(1);
		if (vertexA == null) {
			throw new NullPointerException();
		}
		vertices[0] = vertexA;
		setLayout(layout);
		setColor(color);
	}

	public Vector3D getVector(int vectorID) {
		if (vectorID != VERTEX_A) {
			throw new IllegalArgumentException();
		}
		return vertices[0];
	}

	public void setVector(int vectorID, Vector3D vector) {
		if (vector == null) {
			throw new NullPointerException();
		}
		if (vectorID != VERTEX_A) {
			throw new IllegalArgumentException();
		}
		vertices[0] = vector;
	}
}
