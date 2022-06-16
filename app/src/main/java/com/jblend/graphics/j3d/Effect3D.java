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

package com.jblend.graphics.j3d;

public class Effect3D extends com.mascotcapsule.micro3d.v3.Effect3D {
	public Effect3D() {
		super();
	}

	public Effect3D(Light light, int shading, boolean isEnableTrans, Texture tex) {
		super(light, shading, isEnableTrans, tex);
	}

	public void setLight(Light light) {
		super.setLight(light);
	}

	public void setSphereMap(Texture texture) {
		setSphereTexture(texture);
	}
}