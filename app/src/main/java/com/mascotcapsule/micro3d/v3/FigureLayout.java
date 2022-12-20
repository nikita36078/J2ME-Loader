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

import ru.woesss.j2me.micro3d.FigureLayoutImpl;

@SuppressWarnings({"unused", "WeakerAccess"})
public class FigureLayout {
	protected final FigureLayoutImpl impl;

	public FigureLayout() {
		impl = new FigureLayoutImpl();
	}

	public FigureLayout(AffineTrans trans, int sx, int sy, int cx, int cy) {
		impl = new FigureLayoutImpl(trans, sx, sy, cx, cy);
	}

	FigureLayout(FigureLayout src) {
		impl = new FigureLayoutImpl(src.impl);
	}

	public AffineTrans getAffineTrans() {
		return impl.getAffineTrans();
	}

	public final void setAffineTrans(AffineTrans[] trans) {
		impl.setAffineTrans(trans);
	}

	/**
	 * Sets the affine transformation object.
	 *
	 * @param trans Affine transformation (no transformation if null)
	 */
	public final void setAffineTrans(AffineTrans trans) {
		impl.setAffineTrans(trans);
	}

	@Deprecated
	public final void setAffineTransArray(AffineTrans[] trans) {
		impl.setAffineTrans(trans);
	}

	public final void selectAffineTrans(int idx) {
		impl.selectAffineTrans(idx);
	}

	public final int getScaleX() {
		return impl.getScaleX();
	}

	public final int getScaleY() {
		return impl.getScaleY();
	}

	public final void setScale(int sx, int sy) {
		impl.setScale(sx, sy);
	}

	public final int getParallelWidth() {
		return impl.getParallelWidth();
	}

	public final int getParallelHeight() {
		return impl.getParallelHeight();
	}

	public final void setParallelSize(int w, int h) {
		impl.setParallelSize(w, h);
	}

	public final int getCenterX() {
		return impl.getCenterX();
	}

	public final int getCenterY() {
		return impl.getCenterY();
	}

	public final void setCenter(int cx, int cy) {
		impl.setCenter(cx, cy);
	}

	public final void setPerspective(int zNear, int zFar, int angle) {
		impl.setPerspective(zNear, zFar, angle);
	}

	public final void setPerspective(int zNear, int zFar, int width, int height) {
		impl.setPerspective(zNear, zFar, width, height);
	}

	void set(FigureLayout src) {
		impl.set(src.impl);
	}
}
