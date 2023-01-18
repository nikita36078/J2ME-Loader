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

package com.mascotcapsule.micro3d.v3;

import static ru.woesss.j2me.micro3d.MathUtil.TO_FLOAT;

import ru.woesss.j2me.micro3d.Render;
import ru.woesss.j2me.micro3d.TextureImpl;

public class RenderProxy {

	static void getViewTrans(AffineTrans a, float[] out, int n) {
		int offset = n * 12;
		out[offset++] = a.m00 * TO_FLOAT;
		out[offset++] = a.m10 * TO_FLOAT;
		out[offset++] = a.m20 * TO_FLOAT;
		out[offset++] = a.m01 * TO_FLOAT;
		out[offset++] = a.m11 * TO_FLOAT;
		out[offset++] = a.m21 * TO_FLOAT;
		out[offset++] = a.m02 * TO_FLOAT;
		out[offset++] = a.m12 * TO_FLOAT;
		out[offset++] = a.m22 * TO_FLOAT;
		out[offset++] = a.m03;
		out[offset++] = a.m13;
		out[offset  ] = a.m23;
	}

	static void setTextureArray(Render render, Texture[] textures) {
		if (textures != null) {
			int len = textures.length;
			if (len > 0) {
				if (len > 16) {
					len = 16;
				}
				TextureImpl[] texArray = new TextureImpl[len];
				for (int i = 0; i < len; i++) {
					Texture texture = textures[i];
					if (texture == null) {
						throw new NullPointerException();
					}
					texArray[i] = texture.impl;
				}
				render.setTextureArray(texArray);
			}
		}
	}

	static void setEffects(Render render, Effect3D effect) {
		int attrs = render.getAttributes();
		Light light = effect.light;
		if (light != null) {
			int ambIntencity = light.ambIntensity;
			int dirIntensity = light.dirIntensity;
			Vector3D dir = light.direction;
			render.setLight(ambIntencity, dirIntensity, dir.x, dir.y, dir.z);
			attrs |= Graphics3D.ENV_ATTR_LIGHTING;
		} else {
			attrs &= ~Graphics3D.ENV_ATTR_LIGHTING;
		}

		int shading = effect.shading;
		if (shading == Effect3D.TOON_SHADING) {
			attrs |= Graphics3D.ENV_ATTR_TOON_SHADING;
			render.setToonParam(effect.toonThreshold, effect.toonHigh, effect.toonLow);
		} else {
			attrs &= ~Graphics3D.ENV_ATTR_TOON_SHADING;
		}


		boolean isBlend = effect.isTransparency;
		if (isBlend) {
			attrs |= Graphics3D.ENV_ATTR_SEMI_TRANSPARENT;
		} else {
			attrs &= ~Graphics3D.ENV_ATTR_SEMI_TRANSPARENT;
		}

		Texture specular = effect.texture;
		if (specular != null) {
			attrs |= Graphics3D.ENV_ATTR_SPHERE_MAP;
			render.setSphereTexture(specular.impl);
		} else {
			attrs &= ~Graphics3D.ENV_ATTR_SPHERE_MAP;
		}

		render.setAttribute(attrs);
	}

	static void setProjection(Render render, FigureLayout layout) {
		switch (layout.projection) {
			case Graphics3D.COMMAND_PARALLEL_SCALE: {
				int sx = layout.scaleX;
				int sy = layout.scaleY;
				render.setOrthographicScale(sx, sy);
				break;
			}
			case Graphics3D.COMMAND_PARALLEL_SIZE: {
				int w = layout.parallelWidth;
				int h = layout.parallelHeight;
				render.setOrthographicWH(w, h);
				break;
			}
			case Graphics3D.COMMAND_PERSPECTIVE_FOV: {
				int near = layout.near;
				int far = layout.far;
				int angle = layout.angle;
				render.setPerspectiveFov(near, far, angle);
				break;
			}
			case Graphics3D.COMMAND_PERSPECTIVE_WH: {
				int w = layout.perspectiveWidth;
				int h = layout.perspectiveHeight;
				int near = layout.near;
				int far = layout.far;
				render.setPerspectiveWH(near, far, w, h);
				break;
			}
		}
	}

	static void setAffineArray(Render render, AffineTrans[] affineArray) {
		if (affineArray != null) {
			int len = affineArray.length;
			float[] transArray = new float[len * 12];
			for (int i = 0; i < len; i++) {
				getViewTrans(affineArray[i], transArray, i);
			}
			render.setViewTransArray(transArray);
		}
	}
}
