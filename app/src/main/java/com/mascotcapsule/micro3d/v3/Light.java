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

public class Light {
	private int myAmbIntensity;
	private int myDirIntensity;
	private Vector3D myDirVector;

	public Light() {
		this.myDirVector = new Vector3D(0, 0, 4096);
		this.myDirIntensity = 4096;
		this.myAmbIntensity = 0;
	}

	public Light(Vector3D dir, int dirIntensity, int ambIntensity) {
		if (dir == null) {
			throw new NullPointerException();
		}
		this.myDirVector = dir;
		this.myDirIntensity = dirIntensity;
		this.myAmbIntensity = ambIntensity;
	}

	public final int getDirIntensity() {
		return getParallelLightIntensity();
	}

	public final int getParallelLightIntensity() {
		return this.myDirIntensity;
	}

	public final void setDirIntensity(int p) {
		setParallelLightIntensity(p);
	}

	public final void setParallelLightIntensity(int p) {
		this.myDirIntensity = p;
	}

	public final int getAmbIntensity() {
		return getAmbientIntensity();
	}

	public final int getAmbientIntensity() {
		return this.myAmbIntensity;
	}

	public final void setAmbIntensity(int p) {
		setAmbientIntensity(p);
	}

	public final void setAmbientIntensity(int p) {
		this.myAmbIntensity = p;
	}

	public Vector3D getDirection() {
		return getParallelLightDirection();
	}

	public final Vector3D getParallelLightDirection() {
		return this.myDirVector;
	}

	public final void setDirection(Vector3D v) {
		setParallelLightDirection(v);
	}

	public final void setParallelLightDirection(Vector3D v) {
		if (v == null) {
			throw new NullPointerException();
		}
		this.myDirVector = v;
	}
}
