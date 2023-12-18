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

package ru.woesss.j2me.micro3d;

class Light {
	int ambIntensity;
	int dirIntensity;
	int x;
	int y;
	int z;

	Light() {
		this.ambIntensity = 4096;
		this.dirIntensity = 0;
		this.x = 0;
		this.y = 0;
		this.z = 4096;
	}

	Light(Light light) {
		this.ambIntensity = light.ambIntensity;
		this.dirIntensity = light.dirIntensity;
		this.x = light.x;
		this.y = light.y;
		this.z = light.z;
	}

	void set(int ambIntensity, int dirIntensity, int x, int y, int z) {
		this.ambIntensity = ambIntensity;
		this.dirIntensity = dirIntensity;
		this.x = x;
		this.y = y;
		this.z = z;
	}
}
