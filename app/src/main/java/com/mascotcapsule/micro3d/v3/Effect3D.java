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

public class Effect3D {
	public static final int NORMAL_SHADING = 0;
	public static final int TOON_SHADING = 1;
	private boolean myEnable;
	private Light myLight;
	private int myShading;
	private Texture myTex;
	private int myToonH;
	private int myToonL;
	private int myToonT;

	public Effect3D() {
		this.myShading = NORMAL_SHADING;
		this.myEnable = true;
	}

	public Effect3D(Light light, int shading, boolean isEnableTrans, Texture tex) {
		switch (shading) {
			case 0:
			case 1:
				if (tex != null) {
					if (tex.isModel) {
						throw new IllegalArgumentException();
					}
				}
				this.myLight = light;
				this.myShading = shading;
				this.myEnable = isEnableTrans;
				this.myTex = tex;
				return;
			default:
				throw new IllegalArgumentException();
		}
	}

	public final Light getLight() {
		return this.myLight;
	}

	public final void setLight(Light light) {
		this.myLight = light;
	}

	public final int getShading() {
		return getShadingType();
	}

	public final int getShadingType() {
		return this.myShading;
	}

	public final void setShading(int shading) {
		setShadingType(shading);
	}

	public final void setShadingType(int shading) {
		switch (shading) {
			case 0:
			case 1:
				this.myShading = shading;
				return;
			default:
				throw new IllegalArgumentException();
		}
	}

	public final int getThreshold() {
		return getToonThreshold();
	}

	public final int getToonThreshold() {
		return this.myToonT;
	}

	public final int getThresholdHigh() {
		return getToonHigh();
	}

	public final int getToonHigh() {
		return this.myToonH;
	}

	public final int getThresholdLow() {
		return getToonLow();
	}

	public final int getToonLow() {
		return this.myToonL;
	}

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
			this.myToonT = threshold;
			this.myToonH = high;
			this.myToonL = low;
		}
	}

	public final boolean isSemiTransparentEnabled() {
		return isTransparency();
	}

	public final boolean isTransparency() {
		return this.myEnable;
	}

	public final void setSemiTransparentEnabled(boolean isEnable) {
		setTransparency(isEnable);
	}

	public final void setTransparency(boolean isEnable) {
		this.myEnable = isEnable;
	}

	public final Texture getSphereMap() {
		return getSphereTexture();
	}

	public final Texture getSphereTexture() {
		return this.myTex;
	}

	public final void setSphereMap(Texture tex) {
		setSphereTexture(tex);
	}

	public final void setSphereTexture(Texture tex) {
		if (tex != null) {
			if (tex.isModel) {
				throw new IllegalArgumentException();
			}
		}
		this.myTex = tex;
	}
}
