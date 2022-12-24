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
	Layout3D layout;
	Texture sphereTexture;
	Texture texture;

	public Layout3D getLayout() {
		return layout;
	}

	public Texture getSphereTexture() {
		return sphereTexture;
	}

	public Texture getTexture() {
		return texture;
	}

	public void setLayout(Layout3D layout) {
		this.layout = layout;
	}

	public void setSphereTexture(Texture sphereTexture) {
		if (sphereTexture != null && !sphereTexture.isSphereTexture()) {
			throw new IllegalArgumentException();
		}
		this.sphereTexture = sphereTexture;
	}

	public void setTexture(Texture texture) {
		if (texture != null && texture.isSphereTexture()) {
			throw new IllegalArgumentException();
		}
		this.texture = texture;
	}
}
