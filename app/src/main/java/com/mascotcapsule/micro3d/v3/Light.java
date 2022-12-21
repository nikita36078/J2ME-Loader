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
	Vector3D direction;
	int dirIntensity;
	int ambIntensity;

	public Light() {
		direction = new Vector3D(0, 0, 4096);
		this.dirIntensity = 4096;
		this.ambIntensity = 0;
	}

	public Light(Vector3D dir, int dirIntensity, int ambIntensity) {
		if (dir == null) {
			throw new NullPointerException();
		}
		direction = dir;
		this.dirIntensity = dirIntensity;
		this.ambIntensity = ambIntensity;
	}

	public final int getAmbientIntensity() {
		return ambIntensity;
	}

	@Deprecated
	public final int getAmbIntensity() {
		return ambIntensity;
	}

	@Deprecated
	public Vector3D getDirection() {
		return direction;
	}

	@Deprecated
	public final int getDirIntensity() {
		return dirIntensity;
	}

	public final Vector3D getParallelLightDirection() {
		return direction;
	}

	public final int getParallelLightIntensity() {
		return dirIntensity;
	}

	public final void setAmbientIntensity(int p) {
		ambIntensity = p;
	}

	@Deprecated
	public final void setAmbIntensity(int p) {
		ambIntensity = p;
	}

	@Deprecated
	public final void setDirection(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
		direction = v;
	}

	@Deprecated
	public final void setDirIntensity(int p) {
		dirIntensity = p;
	}

	public final void setParallelLightDirection(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
		direction = v;
	}

	public final void setParallelLightIntensity(int p) {
		dirIntensity = p;
	}
}
