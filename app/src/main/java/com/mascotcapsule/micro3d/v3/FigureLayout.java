/*
 * Copyright 2020 Yury Kharchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mascotcapsule.micro3d.v3;

@SuppressWarnings({"unused", "WeakerAccess"})
public class FigureLayout {
	AffineTrans[] affineArray;
	AffineTrans affine;
	int scaleX;
	int scaleY;
	int centerX;
	int centerY;
	int parallelWidth;
	int parallelHeight;
	int near;
	int far;
	int angle;
	int perspectiveWidth;
	int perspectiveHeight;
	int projection;

	public FigureLayout() {
		this(null, 512, 512, 0, 0);
	}

	public FigureLayout(AffineTrans trans, int sx, int sy, int cx, int cy) {
		setAffineTrans(trans);
		centerX = cx;
		centerY = cy;
		setScale(sx, sy);
	}

	public final AffineTrans getAffineTrans() {
		return affine;
	}

	public final int getCenterX() {
		return centerX;
	}

	public final int getCenterY() {
		return centerY;
	}

	public final int getParallelHeight() {
		return parallelHeight;
	}

	public final int getParallelWidth() {
		return parallelWidth;
	}

	public final int getScaleX() {
		return scaleX;
	}

	public final int getScaleY() {
		return scaleY;
	}

	public final void selectAffineTrans(int idx) {
		if (affineArray == null || idx < 0 || idx >= affineArray.length) {
			throw new IllegalArgumentException();
		}
		affine = affineArray[idx];
	}

	public final void setAffineTrans(AffineTrans trans) {
		if (trans == null) {
			trans = new AffineTrans(4096, 0, 0, 0, 0, 4096, 0, 0, 0, 0, 4096, 0);
		}
		if (affineArray == null) {
			affineArray = new AffineTrans[1];
			affineArray[0] = trans;
		}
		affine = trans;
	}

	public final void setAffineTrans(AffineTrans[] trans) {
		if (trans == null) {
			throw new NullPointerException();
		}
		for (AffineTrans tran : trans) {
			if (tran == null) {
				throw new NullPointerException();
			}
		}
		affineArray = trans;
	}

	@Deprecated
	public final void setAffineTransArray(AffineTrans[] trans) {
		setAffineTrans(trans);
	}

	public final void setCenter(int cx, int cy) {
		centerX = cx;
		centerY = cy;
	}

	public final void setParallelSize(int w, int h) {
		if (w < 0 || h < 0) {
			throw new IllegalArgumentException();
		}
		parallelWidth = w;
		parallelHeight = h;
		projection = Graphics3D.COMMAND_PARALLEL_SIZE;
	}

	public final void setPerspective(int zNear, int zFar, int angle) {
		if (zNear >= zFar || zNear < 1 || zFar > 32767 || angle < 1 || angle > 2047) {
			throw new IllegalArgumentException();
		}
		near = zNear;
		far = zFar;
		this.angle = angle;
		projection = Graphics3D.COMMAND_PERSPECTIVE_FOV;
	}

	public final void setPerspective(int zNear, int zFar, int width, int height) {
		if (zNear >= zFar || zNear < 1 || zFar > 32767 || width < 0 || height < 0) {
			throw new IllegalArgumentException();
		}
		near = zNear;
		far = zFar;
		perspectiveWidth = width;
		perspectiveHeight = height;
		projection = Graphics3D.COMMAND_PERSPECTIVE_WH;
	}

	public final void setScale(int sx, int sy) {
		scaleX = sx;
		scaleY = sy;
		projection = Graphics3D.COMMAND_PARALLEL_SCALE;
	}
}
