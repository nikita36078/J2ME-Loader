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

package ru.woesss.j2me.micro3d;

import static com.mascotcapsule.micro3d.v3.Graphics3D.COMMAND_PARALLEL_SCALE;
import static com.mascotcapsule.micro3d.v3.Graphics3D.COMMAND_PARALLEL_SIZE;
import static com.mascotcapsule.micro3d.v3.Graphics3D.COMMAND_PERSPECTIVE_FOV;
import static com.mascotcapsule.micro3d.v3.Graphics3D.COMMAND_PERSPECTIVE_WH;
import static ru.woesss.j2me.micro3d.Utils.TO_FLOAT;

import com.mascotcapsule.micro3d.v3.AffineTrans;
import com.mascotcapsule.micro3d.v3.Graphics3D;

public class FigureLayoutImpl {
	public AffineTrans[] affineArray;
	public AffineTrans affine;
	public int scaleX;
	public int scaleY;
	public int centerX;
	public int centerY;
	public int parallelWidth;
	public int parallelHeight;
	public int near;
	public int far;
	public int angle;
	public int perspectiveWidth;
	public int perspectiveHeight;
	public int projection;

	public FigureLayoutImpl() {
		this(null, 512, 512, 0, 0);
	}

	public FigureLayoutImpl(AffineTrans trans, int sx, int sy, int cx, int cy) {
		setAffineTrans(trans);
		centerX = cx;
		centerY = cy;
		setScale(sx, sy);
	}

	public FigureLayoutImpl(FigureLayoutImpl src) {
		affine = new AffineTrans(src.affine);
		affineArray = src.affineArray;
		angle = src.angle;
		centerX = src.centerX;
		centerY = src.centerY;
		far = src.far;
		near = src.near;
		parallelHeight = src.parallelHeight;
		parallelWidth = src.parallelWidth;
		perspectiveHeight = src.perspectiveHeight;
		perspectiveWidth = src.perspectiveWidth;
		scaleX = src.scaleX;
		scaleY = src.scaleY;
		projection = src.projection;
	}

	public AffineTrans getAffineTrans() {
		return affine;
	}

	public final void setAffineTrans(AffineTrans[] trans) {
		if (trans == null || trans.length == 0) {
			throw new NullPointerException();
		}
		for (AffineTrans tran : trans) {
			if (tran == null) throw new NullPointerException();
		}
		affineArray = trans;
	}

	/**
	 * Sets the affine transformation object.
	 *
	 * @param trans Affine transformation (no transformation if null)
	 */
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

	public final void selectAffineTrans(int idx) {
		if (affineArray == null || idx < 0 || idx >= affineArray.length) {
			throw new IllegalArgumentException();
		}
		affine = affineArray[idx];
	}

	public final int getScaleX() {
		return scaleX;
	}

	public final int getScaleY() {
		return scaleY;
	}

	public final void setScale(int sx, int sy) {
		scaleX = sx;
		scaleY = sy;
		projection = Graphics3D.COMMAND_PARALLEL_SCALE;
	}

	public final int getParallelWidth() {
		return parallelWidth;
	}

	public final int getParallelHeight() {
		return parallelHeight;
	}

	public final void setParallelSize(int w, int h) {
		if (w < 0 || h < 0) {
			throw new IllegalArgumentException();
		}
		parallelWidth = w;
		parallelHeight = h;
		projection = Graphics3D.COMMAND_PARALLEL_SIZE;
	}

	public final int getCenterX() {
		return centerX;
	}

	public final int getCenterY() {
		return centerY;
	}

	public final void setCenter(int cx, int cy) {
		centerX = cx;
		centerY = cy;
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

	public void set(FigureLayoutImpl src) {
		projection = src.projection;
		scaleY = src.scaleY;
		scaleX = src.scaleX;
		perspectiveWidth = src.perspectiveWidth;
		perspectiveHeight = src.perspectiveHeight;
		parallelWidth = src.parallelWidth;
		parallelHeight = src.parallelHeight;
		near = src.near;
		far = src.far;
		centerY = src.centerY;
		centerX = src.centerX;
		angle = src.angle;
		affineArray = src.affineArray;
		affine.set(src.affine);
	}

	public float[] getViewMatrix() {
		AffineTrans a = this.affine;
		float[] m = new float[16];
		m[0] = a.m00 * TO_FLOAT; m[4] = a.m01 * TO_FLOAT; m[ 8] = a.m02 * TO_FLOAT; m[12] = a.m03;
		m[1] = a.m10 * TO_FLOAT; m[5] = a.m11 * TO_FLOAT; m[ 9] = a.m12 * TO_FLOAT; m[13] = a.m13;
		m[2] = a.m20 * TO_FLOAT; m[6] = a.m21 * TO_FLOAT; m[10] = a.m22 * TO_FLOAT; m[14] = a.m23;
		m[3] =             0.0F; m[7] =             0.0F; m[11] =             0.0F; m[15] =  1.0F;
		return m;
	}

	float[] getProjectionMatrix(int x, int y, int width, int height) {
		float[] pm = new float[16];
		switch (projection) {
			case COMMAND_PARALLEL_SCALE:
				parallelScale(pm, x, y, width, height);
				break;
			case COMMAND_PARALLEL_SIZE:
				parallelWH(pm, x, y, width, height);
				break;
			case COMMAND_PERSPECTIVE_FOV:
				perspectiveFov(pm, x, y, width, height);
				break;
			case COMMAND_PERSPECTIVE_WH:
				perspectiveWH(pm, x, y, width, height);
				break;
		}
		return pm;
	}

	private void perspectiveWH(float[] pm, int x, int y, float vw, float vh) {
		float zFar = far;
		float zNear = near;
		float width = perspectiveWidth == 0 ? vw : perspectiveWidth * TO_FLOAT;
		float height = perspectiveHeight == 0 ? vh : perspectiveHeight * TO_FLOAT;

		float rd = 1.0f / (zNear - zFar);
		float sx = 2.0f * zNear / width;
		float sy = 2.0f * zNear / height;
		float sz = -(zNear + zFar) * rd;
		float tx = 2.0f * (centerX + x) / vw - 1.0f;
		float ty = 2.0f * (centerY + y) / vh - 1.0f;
		float tz = 2.0f * zFar * zNear * rd;

		pm[0] =   sx; pm[4] = 0.0f; pm[ 8] =   tx; pm[12] = 0.0f;
		pm[1] = 0.0f; pm[5] =   sy; pm[ 9] =   ty; pm[13] = 0.0f;
		pm[2] = 0.0f; pm[6] = 0.0f; pm[10] =   sz; pm[14] =   tz;
		pm[3] = 0.0f; pm[7] = 0.0f; pm[11] = 1.0f; pm[15] = 0.0f;
	}

	private void perspectiveFov(float[] pm, int x, int y, float vw, float vh) {
		float near = this.near;
		float far = this.far;
		float rd = 1.0f / (near - far);
		float sx = 1.0f / (float) Math.tan(angle * TO_FLOAT * Math.PI);
		float sy = sx * (vw / vh);
		float sz = -(far + near) * rd;
		float tx = 2.0f * (centerX + x) / vw - 1.0f;
		float ty = 2.0f * (centerY + y) / vh - 1.0f;
		float tz = 2.0f * far * near * rd;

		pm[0] =   sx; pm[4] = 0.0f; pm[ 8] =   tx; pm[12] = 0.0f;
		pm[1] = 0.0f; pm[5] =   sy; pm[ 9] =   ty; pm[13] = 0.0f;
		pm[2] = 0.0f; pm[6] = 0.0f; pm[10] =   sz; pm[14] =   tz;
		pm[3] = 0.0f; pm[7] = 0.0f; pm[11] = 1.0f; pm[15] = 0.0f;
	}

	private void parallelWH(float[] pm, int x, int y, float vw, float vh) {
		float w = parallelWidth == 0 ? 400.0f * 4.0f : parallelWidth;
		float h = parallelHeight == 0 ? w * (vh / vw) : parallelHeight;

		float sx = 2.0f / w;
		float sy = 2.0f / h;
		float sz = 1.0f / 65536.0f;
		float tx = 2.0f * (centerX + x) / vw - 1.0f;
		float ty = 2.0f * (centerY + y) / vh - 1.0f;
		float tz = 0.0f;

		pm[0] =   sx; pm[4] = 0.0f; pm[ 8] = 0.0f; pm[12] =   tx;
		pm[1] = 0.0f; pm[5] =   sy; pm[ 9] = 0.0f; pm[13] =   ty;
		pm[2] = 0.0f; pm[6] = 0.0f; pm[10] =   sz; pm[14] =   tz;
		pm[3] = 0.0f; pm[7] = 0.0f; pm[11] = 0.0f; pm[15] = 1.0f;
	}

	private void parallelScale(float[] pm, int x, int y, float vw, float vh) {
		float w = vw * (4096.0f / scaleX);
		float h = vh * (4096.0f / scaleY);

		float sx = 2.0f / w;
		float sy = 2.0f / h;
		float sz = 1.0f / 65536.0f;
		float tx = 2.0f * (centerX + x) / vw - 1.0f;
		float ty = 2.0f * (centerY + y) / vh - 1.0f;
		float tz = 0.0f;

		pm[ 0] =   sx; pm[ 4] = 0.0f; pm[ 8] = 0.0f; pm[12] =   tx;
		pm[ 1] = 0.0f; pm[ 5] =   sy; pm[ 9] = 0.0f; pm[13] =   ty;
		pm[ 2] = 0.0f; pm[ 6] = 0.0f; pm[10] =   sz; pm[14] =   tz;
		pm[ 3] = 0.0f; pm[ 7] = 0.0f; pm[11] = 0.0f; pm[15] = 1.0f;
	}
}