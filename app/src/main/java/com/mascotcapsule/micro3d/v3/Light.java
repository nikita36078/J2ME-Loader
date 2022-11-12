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

@SuppressWarnings("unused")
public class Light {
	protected Vector3D direction;
	protected int dirIntensity;
	protected int ambIntensity;

	public Light() {
		this(new Vector3D(0, 0, 4096), 4096, 0);
	}

	public Light(Vector3D dir, int dirIntensity, int ambIntensity) {
		if (dir == null) {
			throw new NullPointerException();
		}
		direction = dir;
		this.dirIntensity = dirIntensity;
		this.ambIntensity = ambIntensity;
	}

	Light(Light src) {
		direction = new Vector3D(src.direction);
		dirIntensity = src.dirIntensity;
		ambIntensity = src.ambIntensity;
	}

	@Deprecated
	public final int getDirIntensity() {
		return dirIntensity;
	}

	@Deprecated
	public final void setDirIntensity(int p) {
		dirIntensity = p;
	}

	public final int getParallelLightIntensity() {
		return dirIntensity;
	}

	public final void setParallelLightIntensity(int p) {
		dirIntensity = p;
	}

	@Deprecated
	public final int getAmbIntensity() {
		return ambIntensity;
	}

	@Deprecated
	public final void setAmbIntensity(int p) {
		ambIntensity = p;
	}

	public final int getAmbientIntensity() {
		return ambIntensity;
	}

	public final void setAmbientIntensity(int p) {
		ambIntensity = p;
	}

	@Deprecated
	public Vector3D getDirection() {
		return direction;
	}

	@Deprecated
	public final void setDirection(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
		direction = v;
	}

	public final Vector3D getParallelLightDirection() {
		return direction;
	}

	public final void setParallelLightDirection(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
		direction = v;
	}

	void set(Light src) {
		direction.set(src.direction);
		dirIntensity = src.dirIntensity;
		ambIntensity = src.ambIntensity;
	}
}
