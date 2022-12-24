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

public class Quadrangle extends Primitive {
	Vector3D[] normals;
	final int[] textureCoords = new int[8];
	boolean hasFaceNormal;

	public Quadrangle(Vector3D vertexA,
					  Vector3D vertexB,
					  Vector3D vertexC,
					  Vector3D vertexD,
					  int textureXA,
					  int textureYA,
					  int textureXB,
					  int textureYB,
					  int textureXC,
					  int textureYC,
					  int textureXD,
					  int textureYD,
					  Layout3D layout,
					  Texture texture) {
		this(vertexA, vertexB, vertexC, vertexD, layout);
		textureCoords[0] = textureXA;
		textureCoords[1] = textureYA;
		textureCoords[2] = textureXB;
		textureCoords[3] = textureYB;
		textureCoords[4] = textureXC;
		textureCoords[5] = textureYC;
		textureCoords[6] = textureXD;
		textureCoords[7] = textureYD;
		setTexture(texture);
	}

	public Quadrangle(Vector3D vertexA,
					  Vector3D vertexB,
					  Vector3D vertexC,
					  Vector3D vertexD,
					  Layout3D layout,
					  int color) {
		this(vertexA, vertexB, vertexC, vertexD, layout);
		setColor(color);
	}

	public Quadrangle(Vector3D vertexA,
					  Vector3D vertexB,
					  Vector3D vertexC,
					  Vector3D vertexD,
					  Vector3D faceNormal,
					  int textureXA,
					  int textureYA,
					  int textureXB,
					  int textureYB,
					  int textureXC,
					  int textureYC,
					  int textureXD,
					  int textureYD,
					  Layout3D layout,
					  Texture texture) {
		this(vertexA, vertexB, vertexC, vertexD, layout);
		if (faceNormal == null) {
			throw new NullPointerException();
		}
		hasFaceNormal = true;
		normals = new Vector3D[]{faceNormal};
		textureCoords[0] = textureXA;
		textureCoords[1] = textureYA;
		textureCoords[2] = textureXB;
		textureCoords[3] = textureYB;
		textureCoords[4] = textureXC;
		textureCoords[5] = textureYC;
		textureCoords[6] = textureXD;
		textureCoords[7] = textureYD;
		setTexture(texture);
	}

	public Quadrangle(Vector3D vertexA,
					  Vector3D vertexB,
					  Vector3D vertexC,
					  Vector3D vertexD,
					  Vector3D faceNormal,
					  Layout3D layout,
					  int color) {
		this(vertexA, vertexB, vertexC, vertexD, layout);
		if (faceNormal == null) {
			throw new NullPointerException();
		}
		hasFaceNormal = true;
		normals = new Vector3D[]{faceNormal};
		setColor(color);
	}

	public Quadrangle(Vector3D vertexA,
					  Vector3D vertexB,
					  Vector3D vertexC,
					  Vector3D vertexD,
					  Vector3D normalA,
					  Vector3D normalB,
					  Vector3D normalC,
					  Vector3D normalD,
					  int textureXA,
					  int textureYA,
					  int textureXB,
					  int textureYB,
					  int textureXC,
					  int textureYC,
					  int textureXD,
					  int textureYD,
					  Layout3D layout,
					  Texture texture) {
		this(vertexA, vertexB, vertexC, vertexD, layout);
		if (normalA == null && normalB == null && normalC == null && normalD == null) {
			throw new NullPointerException();
		}
		normals = new Vector3D[]{normalA, normalB, normalC, normalD};
		textureCoords[0] = textureXA;
		textureCoords[1] = textureYA;
		textureCoords[2] = textureXB;
		textureCoords[3] = textureYB;
		textureCoords[4] = textureXC;
		textureCoords[5] = textureYC;
		textureCoords[6] = textureXD;
		textureCoords[7] = textureYD;
		setTexture(texture);
	}

	public Quadrangle(Vector3D vertexA,
					  Vector3D vertexB,
					  Vector3D vertexC,
					  Vector3D vertexD,
					  Vector3D normalA,
					  Vector3D normalB,
					  Vector3D normalC,
					  Vector3D normalD,
					  Layout3D layout,
					  int color) {
		this(vertexA, vertexB, vertexC, vertexD, layout);
		if (normalA == null && normalB == null && normalC == null && normalD == null) {
			throw new NullPointerException();
		}
		normals = new Vector3D[]{normalA, normalB, normalC, normalD};
		setColor(color);
	}

	private Quadrangle(Vector3D vertexA, Vector3D vertexB, Vector3D vertexC, Vector3D vertexD, Layout3D layout) {
		super(4);
		if (vertexA == null && vertexB == null && vertexC == null && vertexD == null) {
			throw new NullPointerException();
		}
		vertices[0] = vertexA;
		vertices[1] = vertexB;
		vertices[2] = vertexC;
		vertices[3] = vertexD;
		setLayout(layout);
	}

	public int getTextureCoordinateX(int vertexID) {
		if (vertexID < VERTEX_A || vertexID > VERTEX_D) {
			throw new IllegalArgumentException();
		}
		return textureCoords[(vertexID - 1) * 2];
	}

	public int getTextureCoordinateY(int vertexID) {
		if (vertexID < VERTEX_A || vertexID > VERTEX_D) {
			throw new IllegalArgumentException();
		}
		return textureCoords[(vertexID - 1) * 2 + 1];
	}

	public Vector3D getVector(int vectorID) {
		if (vectorID == FACE_NORMAL) {
			return hasFaceNormal ? normals[0] : null;
		}
		int maskedId = vectorID & ~NORMAL;
		if (maskedId < VERTEX_A || maskedId > VERTEX_D) {
			throw new IllegalArgumentException();
		}
		if ((vectorID & NORMAL) != 0) {
			if (hasFaceNormal || normals == null) {
				return null;
			}
			return normals[maskedId - 1];
		} else {
			return vertices[maskedId - 1];
		}
	}

	public void setTextureCoordinates(int vertexID, int x, int y) {
		if (vertexID < VERTEX_A || vertexID > VERTEX_D) {
			throw new IllegalArgumentException();
		}
		int offset = (vertexID - 1) * 2;
		textureCoords[offset++] = x;
		textureCoords[offset  ] = y;
	}

	public void setVector(int vectorID, Vector3D vector) {
		if (vector == null) {
			throw new NullPointerException();
		}
		if (vectorID == FACE_NORMAL) {
			hasFaceNormal = true;
			if (normals == null) {
				normals = new Vector3D[]{vector};
			} else {
				normals[0] = vector;
			}
			return;
		}
		int idMasked = vectorID & ~NORMAL;
		if (idMasked < VERTEX_A || idMasked > VERTEX_D) {
			throw new IllegalArgumentException();
		}
		if ((vectorID & NORMAL) != 0) {
			if (normals == null || hasFaceNormal) {
				normals = new Vector3D[3];
			}
			normals[idMasked - 1] = vector;
		} else {
			vertices[idMasked - 1] = vector;
		}
	}
}
