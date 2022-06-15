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

package com.motorola.iden.micro3d;

public abstract class Object3D {

	public Layout3D getLayout() {
		return null;
	}

	public Texture getSphereTexture() {
		return null;
	}

	public Texture getTexture() {
		return null;
	}

	public void setLayout(Layout3D layout) {
	}

	public void setSphereTexture(Texture sphereTexture) {
	}

	public void setTexture(Texture texture) {
	}
}
