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
public class Effect3D {
	public static final int NORMAL_SHADING = 0;
	public static final int TOON_SHADING = 1;

	Light light;
	Texture texture;
	int shading;
	int toonHigh;
	int toonLow;
	int toonThreshold;
	boolean isTransparency;

	public Effect3D() {
		shading = NORMAL_SHADING;
		isTransparency = true;
	}

	public Effect3D(Light light, int shading, boolean isEnableTrans, Texture tex) {
		setShadingType(shading);
		setSphereTexture(tex);
		setLight(light);
		isTransparency = isEnableTrans;
	}

	public final Light getLight() {
		return light;
	}

	@Deprecated
	public final int getShading() {
		return shading;
	}

	public final int getShadingType() {
		return shading;
	}

	@Deprecated
	public final Texture getSphereMap() {
		return texture;
	}

	public final Texture getSphereTexture() {
		return texture;
	}

	@Deprecated
	public final int getThreshold() {
		return toonThreshold;
	}

	@Deprecated
	public final int getThresholdHigh() {
		return toonHigh;
	}

	@Deprecated
	public final int getThresholdLow() {
		return toonLow;
	}

	public final int getToonHigh() {
		return toonHigh;
	}

	public final int getToonLow() {
		return toonLow;
	}

	public final int getToonThreshold() {
		return toonThreshold;
	}

	@Deprecated
	public final boolean isSemiTransparentEnabled() {
		return isTransparency;
	}

	public final boolean isTransparency() {
		return isTransparency;
	}

	public final void setLight(Light light) {
		this.light = light;
	}

	@Deprecated
	public final void setSemiTransparentEnabled(boolean isEnable) {
		isTransparency = isEnable;
	}

	@Deprecated
	public final void setShading(int shading) {
		setShadingType(shading);
	}

	public final void setShadingType(int shading) {
		if ((shading & ~TOON_SHADING) != 0) {
			throw new IllegalArgumentException();
		}
		this.shading = shading;
	}

	@Deprecated
	public final void setSphereMap(Texture tex) {
		setSphereTexture(tex);
	}

	public final void setSphereTexture(Texture tex) {
		if (tex != null && tex.isForModel) {
			throw new IllegalArgumentException();
		}
		texture = tex;
	}

	@Deprecated
	public final void setThreshold(int threshold, int high, int low) {
		setToonParams(threshold, high, low);
	}

	public final void setToonParams(int threshold, int high, int low) {
		if (((threshold & ~0xff) | (high & ~0xff) | (low & ~0xff)) != 0) {
			throw new IllegalArgumentException();
		}
		toonThreshold = threshold;
		toonHigh = high;
		toonLow = low;
	}

	public final void setTransparency(boolean isEnable) {
		isTransparency = isEnable;
	}
}
