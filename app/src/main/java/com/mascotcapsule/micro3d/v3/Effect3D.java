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

	protected Light light;
	protected Texture texture;
	int shading;
	int toonHigh;
	int toonLow;
	int toonThreshold;
	boolean isTransparency;
	boolean isLighting;
	boolean isReflection;
	boolean isToonShading;

	public Effect3D() {
		shading = NORMAL_SHADING;
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
		this.shading = shading;
		isTransparency = isEnableTrans;
		texture = tex;
	}

	Effect3D(Effect3D src) {
		Light sl = src.light;
		light = sl == null ? null : new Light(sl);
		shading = src.shading;
		texture = src.texture;
		toonHigh = src.toonHigh;
		toonLow = src.toonLow;
		toonThreshold = src.toonThreshold;
		isTransparency = src.isTransparency;
		isLighting = src.isLighting;
		isReflection = src.isReflection;
		isToonShading = src.isToonShading;
	}

	public Light getLight() {
		return light;
	}

	public final void setLight(Light light) {
		this.light = light;
	}

	@Deprecated
	public final int getShading() {
		return shading;
	}

	public final int getShadingType() {
		return shading;
	}

	@Deprecated
	public final void setShading(int shading) {
		setShadingType(shading);
	}

	public final void setShadingType(int shading) {
		switch (shading) {
			case NORMAL_SHADING:
			case TOON_SHADING:
				this.shading = shading;
				return;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Deprecated
	public final int getThreshold() {
		return toonThreshold;
	}

	public final int getToonThreshold() {
		return toonThreshold;
	}

	@Deprecated
	public final int getThresholdHigh() {
		return toonHigh;
	}

	public final int getToonHigh() {
		return toonHigh;
	}

	@Deprecated
	public final int getThresholdLow() {
		return toonLow;
	}

	public final int getToonLow() {
		return toonLow;
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
			toonThreshold = threshold;
			toonHigh = high;
			toonLow = low;
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
	public Texture getSphereMap() {
		return texture;
	}

	public final Texture getSphereTexture() {
		return texture;
	}

	@Deprecated
	public final void setSphereMap(Texture tex) {
		setSphereTexture(tex);
	}

	public final void setSphereTexture(Texture tex) {
		if (tex != null && !tex.isSphere) {
			throw new IllegalArgumentException();
		}
		texture = tex;
	}

	void set(Effect3D src) {
		shading = src.shading;
		texture = src.texture;
		toonHigh = src.toonHigh;
		toonLow = src.toonLow;
		toonThreshold = src.toonThreshold;
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
