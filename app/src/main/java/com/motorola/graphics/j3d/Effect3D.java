/*
 *  Copyright 2020 Yury Kharchenko
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

package com.motorola.graphics.j3d;

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

	public Effect3D(Light light, int shading, boolean isEnabled, Texture sphereMap) {
		setShading(shading);
		setSphereMap(sphereMap);
		setLight(light);
		isTransparency = isEnabled;
	}

	public Light getLight() {
		return light;
	}

	public int getShading() {
		return shading;
	}

	public Texture getSphereMap() {
		return texture;
	}

	public final int getThreshold() {
		return toonThreshold;
	}

	public final int getThresholdHigh() {
		return toonHigh;
	}

	public final int getThresholdLow() {
		return toonLow;
	}

	public final boolean isSemiTransparentEnabled() {
		return isTransparency;
	}

	public void setLight(Light light) {
		this.light = light;
	}

	public final void setSemiTransparentEnabled(boolean isEnable) {
		isTransparency = isEnable;
	}

	public final void setShading(int shading) {
		if ((shading & ~TOON_SHADING) != 0) {
			throw new IllegalArgumentException();
		}
		this.shading = shading;
	}

	public void setSphereMap(Texture texture) {
		if (texture != null && texture.isForModel) {
			throw new IllegalArgumentException();
		}
		this.texture = texture;
	}

	public final void setThreshold(int threshold, int high, int low) {
		if (((threshold & ~0xff) | (high & ~0xff) | (low & ~0xff)) != 0) {
			throw new IllegalArgumentException();
		}
		toonThreshold = threshold;
		toonHigh = high;
		toonLow = low;
	}
}