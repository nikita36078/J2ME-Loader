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

import com.mascotcapsule.micro3d.v3.Graphics3D;

import javax.microedition.lcdui.Graphics;

import ru.woesss.j2me.micro3d.MathUtil;
import ru.woesss.j2me.micro3d.Render;

public class RenderProxy {

	public static void drawFigure(Graphics g, Figure figure, int x, int y,
								  FigureLayout layout, Effect3D effect) {
		if (figure == null || layout == null || effect == null) {
			throw new NullPointerException();
		}
		Render render = Render.getRender();
		render.bind(g);

		getViewTrans(layout.affine, render.getViewMatrix());
		render.setCenter(layout.centerX + x, layout.centerY + y);
		render.setOrthographicScale(layout.scaleX, layout.scaleY);

		int attrs = render.getAttributes();
		Light light = effect.light;
		if (light != null) {
			attrs |= Graphics3D.ENV_ATTR_LIGHTING;
			Vector3D dir = light.direction;
			render.setLight(light.ambIntensity, light.dirIntensity, dir.x, dir.y, dir.z);
		} else {
			attrs &= ~Graphics3D.ENV_ATTR_LIGHTING;
		}

		int shading = effect.shading;
		if (shading == Effect3D.TOON_SHADING) {
			attrs |= Graphics3D.ENV_ATTR_TOON_SHADING;
			int tress = effect.toonThreshold;
			int high = effect.toonHigh;
			int low = effect.toonLow;
			render.setToonParam(tress, high, low);
		} else {
			attrs &= ~Graphics3D.ENV_ATTR_TOON_SHADING;
		}

		if (effect.isTransparency) {
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

		Texture texture = figure.texture;
		if (texture != null) {
			render.setTexture(texture.impl);
		}

		render.drawFigure(figure.impl);
		render.release();
	}

	private static void getViewTrans(AffineTrans a, float[] out) {
		out[0] = a.m00 * MathUtil.TO_FLOAT; out[3] = a.m01 * MathUtil.TO_FLOAT; out[6] = a.m02 * MathUtil.TO_FLOAT; out[ 9] = a.m03;
		out[1] = a.m10 * MathUtil.TO_FLOAT; out[4] = a.m11 * MathUtil.TO_FLOAT; out[7] = a.m12 * MathUtil.TO_FLOAT; out[10] = a.m13;
		out[2] = a.m20 * MathUtil.TO_FLOAT; out[5] = a.m21 * MathUtil.TO_FLOAT; out[8] = a.m22 * MathUtil.TO_FLOAT; out[11] = a.m23;
	}
}
