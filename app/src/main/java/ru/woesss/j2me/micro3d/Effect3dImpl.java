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

import static com.motorola.graphics.j3d.Effect3D.NORMAL_SHADING;
import static com.motorola.graphics.j3d.Effect3D.TOON_SHADING;

import com.mascotcapsule.micro3d.v3.Light;
import com.mascotcapsule.micro3d.v3.Texture;

public class Effect3dImpl {
	public Light light;
	public Texture texture;
	public int shading;
	public int toonHigh;
	public int toonLow;
	public int toonThreshold;
	public boolean isTransparency;
	public boolean isLighting;
	public boolean isReflection;
	public boolean isToonShading;

	public Effect3dImpl() {
		shading = NORMAL_SHADING;
		isTransparency = true;
	}

	public Effect3dImpl(Light light, int shading, boolean isEnableTrans, Texture tex) {
		if (shading != NORMAL_SHADING && shading != TOON_SHADING) {
			throw new IllegalArgumentException();
		}
		if (tex != null && !tex.impl.isSphere) {
			throw new IllegalArgumentException();
		}
		setLight(light);
		setShadingType(shading);
		isTransparency = isEnableTrans;
		texture = tex;
	}

	Effect3dImpl(Effect3dImpl src) {
		Light sl = src.getLight();
		this.light = sl == null ? null : new Light(sl.impl);
		this.shading = src.shading;
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

	public final int getShadingType() {
		return shading;
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

	public final int getToonThreshold() {
		return toonThreshold;
	}

	public final int getToonHigh() {
		return toonHigh;
	}

	public final int getToonLow() {
		return toonLow;
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

	public final boolean isTransparency() {
		return isTransparency;
	}

	public final void setTransparency(boolean isEnable) {
		isTransparency = isEnable;
	}

	public final Texture getSphereTexture() {
		return texture;
	}

	public final void setSphereTexture(Texture tex) {
		if (tex != null && !tex.impl.isSphere) {
			throw new IllegalArgumentException();
		}
		texture = tex;
	}

	public void set(Effect3dImpl src) {
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
			light = new Light(sl.impl);
			return;
		}
		light.set(sl);
	}
}