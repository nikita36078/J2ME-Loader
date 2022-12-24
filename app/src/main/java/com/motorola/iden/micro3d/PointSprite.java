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

public class PointSprite extends Primitive {
	public static final int LOCAL_SIZE = 0;
	public static final int PARALLEL_PROJECTION = 2;
	public static final int PERSPECTIVE_PROJECTION = 0;
	public static final int PIXEL_SIZE = 1;

	final int[] params = new int[8];

	public PointSprite(Vector3D vertexA,
					   int width,
					   int height,
					   int rotation,
					   int textureX,
					   int textureY,
					   int textureWidth,
					   int textureHeight,
					   int displayType,
					   Layout3D layout,
					   Texture texture) {
		super(1);
		if (vertexA == null) {
			throw new NullPointerException();
		}
		vertices[0] = vertexA;
		setLayout(layout);
		setTexture(texture);
		setWidth(width);
		setHeight(height);
		params[2] = rotation;
		params[3] = textureX;
		params[4] = textureY;
		params[5] = textureWidth;
		params[6] = textureHeight;
		params[7] = displayType;
	}

	public int getColor() {
		return 0;
	}

	public int getDisplayType() {
		return params[7];
	}

	public int getHeight() {
		return params[6];
	}

	public int getRotation() {
		return params[2];
	}

	public int getTextureCoordinateX(int vertexID) {
		// TODO: 26.12.2022 don't understand documentation
		return params[3];
	}

	public int getTextureCoordinateY(int vertexID) {
		// TODO: 26.12.2022 don't understand documentation
		return params[4];
	}

	public Vector3D getVector(int vectorID) {
		if (vectorID != VERTEX_A) {
			throw new IllegalArgumentException();
		}
		return vertices[0];
	}

	public int getWidth() {
		return params[0];
	}

	public void setColor(int color) {}

	public void setDisplayType(int displayType) {
		params[7] = displayType;
	}

	public void setHeight(int height) {
		if (height < 0) {
			throw new IllegalArgumentException();
		}
		params[6] = height;
	}

	public void setRotation(int rotation) {
		params[2] = rotation;
	}

	public void setTextureCoordinates(int vertexID, int x, int y) {
		// TODO: 26.12.2022 don't understand documentation

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

	public void setWidth(int width) {
		if (width < 0) {
			throw new IllegalArgumentException();
		}
		params[5] = width;
	}
}
