/*
 * Copyright 2022 Yury Kharchenko
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

package com.jblend.graphics.j3d;

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

	public int getAmbIntensity() {
		return ambIntensity;
	}

	public Vector3D getDirection() {
		return direction;
	}

	public int getDirIntensity() {
		return dirIntensity;
	}

	public void setAmbIntensity(int ambIntensity) {
		this.ambIntensity = ambIntensity;
	}

	public void setDirection(Vector3D dir) {
		if (dir == null) {
			throw new NullPointerException();
		}
		direction = dir;
	}

	public void setDirIntensity(int dirIntensity) {
		this.dirIntensity = dirIntensity;
	}
}
