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

import ru.woesss.j2me.micro3d.Effect3dImpl;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Effect3D {
	public static final int NORMAL_SHADING = 0;
	public static final int TOON_SHADING = 1;

	final Effect3dImpl impl;

	public Effect3D() {
		impl = new Effect3dImpl();
	}

	public Effect3D(Light light, int shading, boolean isEnableTrans, Texture tex) {
		if (shading != NORMAL_SHADING && shading != TOON_SHADING) {
			throw new IllegalArgumentException();
		}
		if (tex != null && !tex.impl.isSphere) {
			throw new IllegalArgumentException();
		}
		impl = new Effect3dImpl(light, shading, isEnableTrans, tex);
	}

	public Light getLight() {
		return impl.getLight();
	}

	public final void setLight(Light light) {
		impl.setLight(light);
	}

	@Deprecated
	public final int getShading() {
		return impl.getShadingType();
	}

	public final int getShadingType() {
		return impl.getShadingType();
	}

	@Deprecated
	public final void setShading(int shading) {
		impl.setShadingType(shading);
	}

	public final void setShadingType(int shading) {
		impl.setShadingType(shading);
	}

	@Deprecated
	public final int getThreshold() {
		return impl.getToonThreshold();
	}

	public final int getToonThreshold() {
		return impl.getToonThreshold();
	}

	@Deprecated
	public final int getThresholdHigh() {
		return impl.getToonHigh();
	}

	public final int getToonHigh() {
		return impl.getToonHigh();
	}

	@Deprecated
	public final int getThresholdLow() {
		return impl.getToonLow();
	}

	public final int getToonLow() {
		return impl.getToonLow();
	}

	@Deprecated
	public final void setThreshold(int threshold, int high, int low) {
		impl.setToonParams(threshold, high, low);
	}

	public final void setToonParams(int threshold, int high, int low) {
		impl.setToonParams(threshold, high, low);
	}

	@Deprecated
	public final boolean isSemiTransparentEnabled() {
		return impl.isTransparency();
	}

	public final boolean isTransparency() {
		return impl.isTransparency();
	}

	@Deprecated
	public final void setSemiTransparentEnabled(boolean isEnable) {
		impl.setTransparency(isEnable);
	}

	public final void setTransparency(boolean isEnable) {
		impl.setTransparency(isEnable);
	}

	@Deprecated
	public Texture getSphereMap() {
		return impl.getSphereTexture();
	}

	public final Texture getSphereTexture() {
		return impl.getSphereTexture();
	}

	@Deprecated
	public final void setSphereMap(Texture tex) {
		impl.setSphereTexture(tex);
	}

	public final void setSphereTexture(Texture tex) {
		impl.setSphereTexture(tex);
	}

	void set(Effect3D src) {
		impl.set(src.impl);
	}
}
