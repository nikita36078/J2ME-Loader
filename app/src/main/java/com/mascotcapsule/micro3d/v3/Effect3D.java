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
	int mShading;
	Texture mTexture;
	int mToonHigh;
	int mToonLow;
	int mToonThreshold;
	boolean isTransparency;
	boolean isLighting;
	boolean isReflection;
	boolean isToonShading;

	public Effect3D() {
		mShading = NORMAL_SHADING;
		isTransparency = true;
	}

	public Effect3D(Light light, int shading, boolean isEnableTrans, Texture tex) {
		if (shading != NORMAL_SHADING && shading != TOON_SHADING) {
			throw new IllegalArgumentException();
		}
		if (tex != null && !tex.isSphere) {
			throw new IllegalArgumentException();
		}
		setLight(light);
		mShading = shading;
		isTransparency = isEnableTrans;
		mTexture = tex;
	}

	Effect3D(Effect3D src) {
		Light sl = src.light;
		light = sl == null ? null : new Light(sl);
		mShading = src.mShading;
		mTexture = src.mTexture;
		mToonHigh = src.mToonHigh;
		mToonLow = src.mToonLow;
		mToonThreshold = src.mToonThreshold;
		isTransparency = src.isTransparency;
		isLighting = src.isLighting;
		isReflection = src.isReflection;
		isToonShading = src.isToonShading;
	}

	public final Light getLight() {
		return light;
	}

	public final void setLight(Light light) {
		this.light = light;
		isLighting = light != null;
	}

	@Deprecated
	public final int getShading() {
		return mShading;
	}

	public final int getShadingType() {
		return mShading;
	}

	@Deprecated
	public final void setShading(int shading) {
		setShadingType(shading);
	}

	public final void setShadingType(int shading) {
		switch (shading) {
			case NORMAL_SHADING:
			case TOON_SHADING:
				mShading = shading;
				return;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Deprecated
	public final int getThreshold() {
		return mToonThreshold;
	}

	public final int getToonThreshold() {
		return mToonThreshold;
	}

	@Deprecated
	public final int getThresholdHigh() {
		return mToonHigh;
	}

	public final int getToonHigh() {
		return mToonHigh;
	}

	@Deprecated
	public final int getThresholdLow() {
		return mToonLow;
	}

	public final int getToonLow() {
		return mToonLow;
	}

	@Deprecated
	public final void setThreshold(int threshold, int high, int low) {
		setToonParams(threshold, high, low);
	}

	public final void setToonParams(int threshold, int high, int low) {
		if (threshold < 0 || threshold > 255) {
			throw new IllegalArgumentException();
		} else if (high < 0 || high > 255) {
			throw new IllegalArgumentException();
		} else if (low < 0 || low > 255) {
			throw new IllegalArgumentException();
		} else {
			mToonThreshold = threshold;
			mToonHigh = high;
			mToonLow = low;
		}
	}

	@Deprecated
	public final boolean isSemiTransparentEnabled() {
		return isTransparency;
	}

	public final boolean isTransparency() {
		return isTransparency;
	}

	@Deprecated
	public final void setSemiTransparentEnabled(boolean isEnable) {
		isTransparency = isEnable;
	}

	public final void setTransparency(boolean isEnable) {
		isTransparency = isEnable;
	}

	@Deprecated
	public final Texture getSphereMap() {
		return mTexture;
	}

	public final Texture getSphereTexture() {
		return mTexture;
	}

	@Deprecated
	public final void setSphereMap(Texture tex) {
		setSphereTexture(tex);
	}

	public final void setSphereTexture(Texture tex) {
		if (tex != null && !tex.isSphere) {
			throw new IllegalArgumentException();
		}
		mTexture = tex;
	}

	void set(Effect3D src) {
		mShading = src.mShading;
		mTexture = src.mTexture;
		mToonHigh = src.mToonHigh;
		mToonLow = src.mToonLow;
		mToonThreshold = src.mToonThreshold;
		isTransparency = src.isTransparency;
		isLighting = src.isLighting;
		isReflection = src.isReflection;
		isToonShading = src.isToonShading;
		Light sl = src.light;
		if (sl == null) {
			light = null;
			return;
		}
		if (light == null) {
			light = new Light(sl);
			return;
		}
		light.set(sl);
	}
}
