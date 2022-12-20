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

import com.mascotcapsule.micro3d.v3.Vector3D;

public class LightImpl {
	public Vector3D direction;
	public int dirIntensity;
	public int ambIntensity;

	public LightImpl() {
		direction = new Vector3D(0, 0, 4096);
		dirIntensity = 4096;
		ambIntensity = 0;
	}

	public LightImpl(Vector3D dir, int dirIntensity, int ambIntensity) {
		if (dir == null) {
			throw new NullPointerException();
		}
		direction = dir;
		this.dirIntensity = dirIntensity;
		this.ambIntensity = ambIntensity;
	}

	public LightImpl(LightImpl src) {
		direction = new Vector3D(src.direction);
		dirIntensity = src.dirIntensity;
		ambIntensity = src.ambIntensity;
	}

	public final int getParallelLightIntensity() {
		return dirIntensity;
	}

	public final void setParallelLightIntensity(int p) {
		dirIntensity = p;
	}

	public final int getAmbientIntensity() {
		return ambIntensity;
	}

	public final void setAmbientIntensity(int p) {
		ambIntensity = p;
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

	public void set(LightImpl src) {
		direction.set(src.direction);
		dirIntensity = src.dirIntensity;
		ambIntensity = src.ambIntensity;
	}
}