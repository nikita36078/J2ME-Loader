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

import ru.woesss.j2me.micro3d.LightImpl;

@SuppressWarnings("unused")
public class Light {
	public final LightImpl impl;

	public Light() {
		impl = new LightImpl();
	}

	public Light(Vector3D dir, int dirIntensity, int ambIntensity) {
		impl = new LightImpl(dir, dirIntensity, ambIntensity);
	}

	public Light(LightImpl src) {
		impl = new LightImpl(src);
	}

	@Deprecated
	public final int getDirIntensity() {
		return impl.getParallelLightIntensity();
	}

	@Deprecated
	public final void setDirIntensity(int p) {
		impl.setParallelLightIntensity(p);
	}

	public final int getParallelLightIntensity() {
		return impl.getParallelLightIntensity();
	}

	public final void setParallelLightIntensity(int p) {
		impl.setParallelLightIntensity(p);
	}

	@Deprecated
	public final int getAmbIntensity() {
		return impl.getAmbientIntensity();
	}

	@Deprecated
	public final void setAmbIntensity(int p) {
		impl.setAmbientIntensity(p);
	}

	public final int getAmbientIntensity() {
		return impl.getAmbientIntensity();
	}

	public final void setAmbientIntensity(int p) {
		impl.setAmbientIntensity(p);
	}

	@Deprecated
	public Vector3D getDirection() {
		return impl.getParallelLightDirection();
	}

	@Deprecated
	public final void setDirection(Vector3D v) {
		impl.setParallelLightDirection(v);
	}

	public final Vector3D getParallelLightDirection() {
		return impl.getParallelLightDirection();
	}

	public final void setParallelLightDirection(Vector3D v) {
		impl.setParallelLightDirection(v);
	}

	public void set(Light src) {
		impl.set(src.impl);
	}
}
