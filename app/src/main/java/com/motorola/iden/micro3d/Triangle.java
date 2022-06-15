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

public class Triangle extends Primitive {

	public Triangle(Vector3D vertexA,
					Vector3D vertexB,
					Vector3D vertexC,
					int textureXA,
					int textureYA,
					int textureXB,
					int textureYB,
					int textureXC,
					int textureYC,
					Layout3D layout,
					Texture texture) {
	}

	public Triangle(Vector3D vertexA,
					Vector3D vertexB,
					Vector3D vertexC,
					Layout3D layout,
					int color) {
	}

	public Triangle(Vector3D vertexA,
					Vector3D vertexB,
					Vector3D vertexC,
					Vector3D faceNormal,
					int textureXA,
					int textureYA,
					int textureXB,
					int textureYB,
					int textureXC,
					int textureYC,
					Layout3D layout,
					Texture texture) {
	}

	public Triangle(Vector3D vertexA,
					Vector3D vertexB,
					Vector3D vertexC,
					Vector3D faceNormal,
					Layout3D layout,
					int color) {
	}

	public Triangle(Vector3D vertexA,
					Vector3D vertexB,
					Vector3D vertexC,
					Vector3D normalA,
					Vector3D normalB,
					Vector3D normalC,
					int textureXA,
					int textureYA,
					int textureXB,
					int textureYB,
					int textureXC,
					int textureYC,
					Layout3D layout,
					Texture texture) {
	}

	public Triangle(Vector3D vertexA,
					Vector3D vertexB,
					Vector3D vertexC,
					Vector3D normalA,
					Vector3D normalB,
					Vector3D normalC,
					Layout3D layout,
					int color) {
	}

	public int getTextureCoordinateX(int vertexID) {
		return 0;
	}

	public int getTextureCoordinateY(int vertexID) {
		return 0;
	}

	public Vector3D getVector(int vectorID) {
		return null;
	}

	public void setTextureCoordinates(int vertexID, int x, int y) {
	}

	public void setVector(int vectorID, Vector3D vector) {
	}
}
