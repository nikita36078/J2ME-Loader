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

import java.util.Arrays;

public class Layout3D {
	public static final int PARALLEL_SCALE = 24;
	public static final int PARALLEL_WIDTH = 25;
	public static final int PARALLEL_WIDTH_HEIGHT = 26;
	public static final int PERSPECTIVE_FOV = 21;
	public static final int PERSPECTIVE_WIDTH = 22;
	public static final int PERSPECTIVE_WIDTH_HEIGHT = 23;

	Light light;
	boolean isSemiTransparent;
	boolean isToonShaded;
	int toonThreshold;
	int toonHighColor;
	int toonLowColor;
	private AffineTransform viewPointTransform;
	private AffineTransform viewTransform;
	int projectionType = PARALLEL_SCALE;
	final int[] projectionParameters = new int[]{4096, 4096, 0, 0};
	Vector3D rotateAxis;
	int rotateAngle;

	public Layout3D() {}

	public Light getLight() {
		return light;
	}

	public int[] getProjectionParameters() {
		switch (projectionType) {
			case PARALLEL_SCALE:
			case PARALLEL_WIDTH_HEIGHT:
				return Arrays.copyOf(projectionParameters, 2);
			case PARALLEL_WIDTH:
				return Arrays.copyOf(projectionParameters, 1);
			case PERSPECTIVE_FOV:
			case PERSPECTIVE_WIDTH:
				return Arrays.copyOf(projectionParameters, 3);
			case PERSPECTIVE_WIDTH_HEIGHT:
				return Arrays.copyOf(projectionParameters, 4);
			default:
				throw new IllegalStateException("Unexpected value: " + projectionType);
		}
	}

	public int getProjectionType() {
		return projectionType;
	}

	public int getToonHighColor() {
		return toonHighColor;
	}

	public int getToonLowColor() {
		return toonLowColor;
	}

	public int getToonThreshold() {
		return toonThreshold;
	}

	public AffineTransform getViewPointTransform() {
		if (viewPointTransform == null) {
			Vector3D position = new Vector3D();
			Vector3D look = new Vector3D(0, 0, 4096);
			Vector3D up = new Vector3D(0, 4096, 0);
			viewPointTransform = AffineTransform.getViewPointTransform(position, look, up);
		}
		return viewPointTransform;
	}

	public AffineTransform getViewTransform() {
		return viewTransform;
	}

	public boolean isSemiTransparent() {
		return isSemiTransparent;
	}

	public boolean isToonShaded() {
		return isToonShaded;
	}

	public void rotateV(Vector3D axis, int angle) {
		rotateAngle = angle;
		if (rotateAxis == null) {
			rotateAxis = new Vector3D(axis);
		} else {
			rotateAxis.set(axis);
		}
		viewTransform = null;
	}

	public void rotateX(int angle) {
		if (rotateAxis == null) {
			rotateAxis = new Vector3D(4096, 0, 0);
		} else {
			rotateAxis.set(4096, 0, 0);
		}
		rotateAngle = angle;
		viewTransform = null;
	}

	public void rotateY(int angle) {
		if (rotateAxis == null) {
			rotateAxis = new Vector3D(0, 4096, 0);
		} else {
			rotateAxis.set(0, 4096, 0);
		}
		rotateAngle = angle;
		viewTransform = null;
	}

	public void rotateZ(int angle) {
		if (rotateAxis == null) {
			rotateAxis = new Vector3D(0, 0, 4096);
		} else {
			rotateAxis.set(0, 0, 4096);
		}
		rotateAngle = angle;
		viewTransform = null;
	}

	public void setLight(Light light) {
		this.light = light;
	}

	public void setProjection(int type, int[] parameters) {
		if (parameters == null) {
			throw new NullPointerException();
		}
		switch (type) {
			case PARALLEL_SCALE:
			case PARALLEL_WIDTH_HEIGHT:
				if (parameters.length < 2) {
					throw new IllegalArgumentException();
				}
				projectionParameters[0] = parameters[0];
				projectionParameters[1] = parameters[1];
				break;
			case PARALLEL_WIDTH:
				if (parameters.length < 1) {
					throw new IllegalArgumentException();
				}
				projectionParameters[0] = parameters[0];
				break;
			case PERSPECTIVE_FOV: {
				if (parameters.length < 3) {
					throw new IllegalArgumentException();
				}
				int near = parameters[0];
				int far = parameters[1];
				int angle = parameters[2];
				if (near >= far || near < 1 || far > 32767 || angle < 1 || angle > 2047) {
					throw new IllegalArgumentException();
				}
				projectionParameters[0] = near;
				projectionParameters[1] = far;
				projectionParameters[2] = angle;
				break;
			}
			case PERSPECTIVE_WIDTH: {
				if (parameters.length < 3) {
					throw new IllegalArgumentException();
				}
				int near = parameters[0];
				int far = parameters[1];
				int width = parameters[2];
				if (near >= far || near < 1 || far > 32767 || width < 0) {
					throw new IllegalArgumentException();
				}
				projectionParameters[0] = near;
				projectionParameters[1] = far;
				projectionParameters[2] = width;
				break;
			}
			case PERSPECTIVE_WIDTH_HEIGHT: {
				if (parameters.length < 4) {
					throw new IllegalArgumentException();
				}
				int near = parameters[0];
				int far = parameters[1];
				int width = parameters[2];
				int height = parameters[3];
				if (near >= far || near < 1 || far > 32767 || width < 0 || height < 0) {
					throw new IllegalArgumentException();
				}
				projectionParameters[0] = near;
				projectionParameters[1] = far;
				projectionParameters[2] = width;
				projectionParameters[3] = height;
				break;
			}
			default:
				throw new IllegalArgumentException();
		}
		projectionType = type;
	}

	public void setSemiTransparent(boolean transparent) {
		isSemiTransparent = transparent;
	}

	public void setToonShading(boolean toon) {
		isToonShaded = toon;
	}

	public void setToonShading(int threshold, int highColor, int lowColor) {
		if ((threshold & ~0xff) != 0) {
			throw new IllegalArgumentException();
		}
		this.toonThreshold = threshold;
		this.toonHighColor = highColor;
		this.toonLowColor = lowColor;
	}

	public void setViewPoint(Vector3D position, Vector3D look, Vector3D up) {
		setViewPointTransform(AffineTransform.getViewPointTransform(position, look, up));
	}

	public void setViewPointTransform(AffineTransform viewPointTransform) {
		if (viewPointTransform == null) {
			throw new NullPointerException();
		}
		this.viewPointTransform = viewPointTransform;
	}

	public void setViewTransform(AffineTransform viewTransform) {
		if (viewTransform == null) {
			throw new NullPointerException();
		}
		this.viewTransform = viewTransform;
	}
}
