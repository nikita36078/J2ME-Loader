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
	}

	public int getColor() {
		return 0;
	}

	public int getDisplayType() {
		return 0;
	}

	public int getHeight() {
		return 0;
	}

	public int getRotation() {
		return 0;
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

	public int getWidth() {
		return 0;
	}

	public void setColor(int color) {
	}

	public void setDisplayType(int displayType) {
	}

	public void setHeight(int height) {
	}

	public void setRotation(int rotation) {
	}

	public void setTextureCoordinates(int vertexID, int x, int y) {
	}

	public void setVector(int vectorID, Vector3D vector) {
	}

	public void setWidth(int width) {
	}
}
