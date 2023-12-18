/*
 * Copyright 2022-2023 Yury Kharchenko
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

import static ru.woesss.j2me.micro3d.MathUtil.TO_FLOAT;

import androidx.annotation.NonNull;

import java.util.WeakHashMap;

import javax.microedition.lcdui.Graphics;

import ru.woesss.j2me.micro3d.Render;
import ru.woesss.j2me.micro3d.TextureImpl;

public class RenderProxy {
	private static final WeakHashMap<Graphics, Render> renders = new WeakHashMap<>();

	public static void drawCommandList(Graphics g,
									   Texture[] textures,
									   int x, int y,
									   FigureLayout layout,
									   Effect3D effect,
									   int[] commandList) {

		if (layout == null || effect == null || commandList == null) {
			throw new NullPointerException();
		}
		Render render = getRender(g);

		RenderProxy.getViewTrans(layout.affine, render.getViewMatrix(), 0);
		RenderProxy.setTextureArray(render, textures);
		RenderProxy.setAffineArray(render, layout.affineArray);
		render.setCenter(layout.centerX + x, layout.centerY + y);
		RenderProxy.setProjection(render, layout);
		RenderProxy.setEffects(render, effect);

		render.drawCommandList(commandList);
	}

	public static void drawCommandList(Graphics g,
									   Texture texture,
									   int x, int y,
									   FigureLayout layout,
									   Effect3D effect,
									   int[] commandList) {

		if (layout == null || effect == null || commandList == null) {
			throw new NullPointerException();
		}
		Render render = getRender(g);

		RenderProxy.getViewTrans(layout.affine, render.getViewMatrix(), 0);
		RenderProxy.setAffineArray(render, layout.affineArray);
		render.setCenter(layout.centerX + x, layout.centerY + y);
		RenderProxy.setProjection(render, layout);
		RenderProxy.setEffects(render, effect);
		if (texture != null) {
			render.setTexture(texture.impl);
		}

		render.drawCommandList(commandList);
	}

	public static void drawFigure(Graphics g, Figure figure, int x, int y,
								  FigureLayout layout, Effect3D effect) {
		if (figure == null || layout == null || effect == null) {
			throw new NullPointerException();
		}
		Render render = getRender(g);

		RenderProxy.getViewTrans(layout.affine, render.getViewMatrix(), 0);
		render.setCenter(layout.centerX + x, layout.centerY + y);
		RenderProxy.setProjection(render, layout);
		RenderProxy.setEffects(render, effect);
		Texture texture = figure.textures[0];
		if (texture != null) {
			render.setTexture(texture.impl);
		}

		render.drawFigure(figure.impl);
	}

	public static void flush(Graphics g) {
		getRender(g).flushToBuffer();
	}

	public static void renderFigure(Graphics g, Figure figure, int x, int y,
									FigureLayout layout, Effect3D effect) {
		if (figure == null || layout == null || effect == null) {
			throw new NullPointerException();
		}

		Render render = getRender(g);

		getViewTrans(layout.affine, render.getViewMatrix(), 0);
		setTextureArray(render, figure.textures);
		render.setCenter(layout.centerX + x, layout.centerY + y);
		RenderProxy.setProjection(render, layout);
		RenderProxy.setEffects(render, effect);

		render.postFigure(figure.impl);
	}

	public static void renderPrimitives(Graphics g, Texture texture, int x, int y,
										FigureLayout layout, Effect3D effect,
										int command, int numPrimitives, int[] vertexCoords,
										int[] normals, int[] textureCoords, int[] colors) {
		if (layout == null || effect == null || vertexCoords == null || normals == null
				|| textureCoords == null || colors == null) {
			throw new NullPointerException();
		}
		if (command < 0 || numPrimitives <= 0 || numPrimitives >= 256) {
			throw new IllegalArgumentException();
		}
		Render render = getRender(g);

		RenderProxy.getViewTrans(layout.affine, render.getViewMatrix(), 0);
		render.setCenter(layout.centerX + x, layout.centerY + y);
		RenderProxy.setProjection(render, layout);
		RenderProxy.setEffects(render, effect);
		if (texture != null) {
			render.setTexture(texture.impl);
		}

		render.postPrimitives(command | numPrimitives << 16, vertexCoords, 0, normals, 0, textureCoords, 0, colors, 0);
	}

	@NonNull
	private static Render getRender(Graphics g) {
		Render render = renders.get(g);
		if (render == null) {
			render = new Render();
			render.bind(g);
			renders.put(g, render);
		}
		return render;
	}

	private static void getViewTrans(AffineTrans a, float[] out, int n) {
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

	private static void setTextureArray(Render render, Texture[] textures) {
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

	private static void setEffects(Render render, Effect3D effect) {
		int attrs = render.getAttributes();
		Light light = effect.light;
		if (light != null) {
			int ambIntensity = light.ambIntensity;
			int dirIntensity = light.dirIntensity;
			Vector3D dir = light.direction;
			render.setLight(ambIntensity, dirIntensity, dir.x, dir.y, dir.z);
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

	private static void setProjection(Render render, FigureLayout layout) {
		switch (layout.projection) {
			case Graphics3D.COMMAND_PARALLEL_SCALE -> {
				int sx = layout.scaleX;
				int sy = layout.scaleY;
				render.setOrthographicScale(sx, sy);
			}
			case Graphics3D.COMMAND_PARALLEL_SIZE -> {
				int w = layout.parallelWidth;
				int h = layout.parallelHeight;
				render.setOrthographicWH(w, h);
			}
			case Graphics3D.COMMAND_PERSPECTIVE_FOV -> {
				int near = layout.near;
				int far = layout.far;
				int angle = layout.angle;
				render.setPerspectiveFov(near, far, angle);
			}
			case Graphics3D.COMMAND_PERSPECTIVE_WH -> {
				int w = layout.perspectiveWidth;
				int h = layout.perspectiveHeight;
				int near = layout.near;
				int far = layout.far;
				render.setPerspectiveWH(near, far, w, h);
			}
		}
	}

	private static void setAffineArray(Render render, AffineTrans[] affineArray) {
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
