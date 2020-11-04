/*
 * Copyright 2018 Nikita Shakarun
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

public class FigureLayout {
	private AffineTrans[] myAffineArray;
	private AffineTrans myAffineNow;
	private int myCenterX;
	private int myCenterY;
	private int myParaHeight;
	private int myParaWidth;
	private int myPersAngle;
	private int myPersFar;
	private int myPersHeight;
	private int myPersNear;
	private int myPersWidth;
	private int myScaleX;
	private int myScaleY;
	private int mySettingIndex;

	public FigureLayout() {
		setAffineTrans((AffineTrans) null);
		this.myScaleX = 512;
		this.myScaleY = 512;
	}

	public FigureLayout(AffineTrans trans, int sx, int sy, int cx, int cy) {
		setAffineTrans(trans);
		this.myScaleX = sx;
		this.myScaleY = sy;
		this.myCenterX = cx;
		this.myCenterY = cy;
	}

	public AffineTrans getAffineTrans() {
		return this.myAffineNow;
	}

	public final void setAffineTrans(AffineTrans trans) {
		if (trans == null) {
			trans = new AffineTrans();
			trans.setIdentity();
		}
		if (this.myAffineArray == null) {
			this.myAffineArray = new AffineTrans[1];
			this.myAffineArray[0] = trans;
		}
		this.myAffineNow = trans;
	}

	public final void setAffineTransArray(AffineTrans[] trans) {
		setAffineTrans(trans);
	}

	public final void setAffineTrans(AffineTrans[] trans) {
		if (trans == null || trans.length == 0) {
			throw new NullPointerException();
		}
		for (AffineTrans affineTrans : trans) {
			if (affineTrans == null) {
				throw new NullPointerException();
			}
		}
		this.myAffineArray = trans;
	}

	public final void selectAffineTrans(int idx) {
		if (this.myAffineArray == null || idx < 0 || idx >= this.myAffineArray.length) {
			throw new IllegalArgumentException();
		}
		this.myAffineNow = this.myAffineArray[idx];
	}

	public final int getScaleX() {
		return this.myScaleX;
	}

	public final int getScaleY() {
		return this.myScaleY;
	}

	public final void setScale(int sx, int sy) {
		this.myScaleX = sx;
		this.myScaleY = sy;
		this.mySettingIndex = 0;
	}

	public final int getParallelWidth() {
		return this.myParaWidth;
	}

	public final int getParallelHeight() {
		return this.myParaHeight;
	}

	public final void setParallelSize(int w, int h) {
		if (w < 0 || h < 0) {
			throw new IllegalArgumentException();
		}
		this.myParaWidth = w;
		this.myParaHeight = h;
		this.mySettingIndex = 1;
	}

	public final int getCenterX() {
		return this.myCenterX;
	}

	public final int getCenterY() {
		return this.myCenterY;
	}

	public final void setCenter(int cx, int cy) {
		this.myCenterX = cx;
		this.myCenterY = cy;
	}

	public final void setPerspective(int zNear, int zFar, int angle) {
		if (zNear >= zFar || zNear < 1 || zNear > 32766 || zFar < 2 || zFar > 32767 || angle < 1 || angle > 2047) {
			throw new IllegalArgumentException();
		}
		this.myPersNear = zNear;
		this.myPersFar = zFar;
		this.myPersAngle = angle;
		this.mySettingIndex = 2;
	}

	public final void setPerspective(int zNear, int zFar, int width, int height) {
		if (zNear >= zFar || zNear < 1 || zNear > 32766 || zFar < 2 || zFar > 32767 || width < 0 || height < 0) {
			throw new IllegalArgumentException();
		}
		this.myPersNear = zNear;
		this.myPersFar = zFar;
		this.myPersWidth = width;
		this.myPersHeight = height;
		this.mySettingIndex = 3;
	}
}
