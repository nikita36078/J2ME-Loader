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

import com.mascotcapsule.micro3d.v3.impl.Renderer;

import javax.microedition.lcdui.Graphics;

@SuppressWarnings("unused, WeakerAccess")
public class Graphics3D {

	public static final int COMMAND_LIST_VERSION_1_0 = 0xFE000001;

	public static final int COMMAND_END = 0x80000000;
	public static final int COMMAND_NOP = 0x81000000;
	public static final int COMMAND_FLUSH = 0x82000000;
	public static final int COMMAND_ATTRIBUTE = 0x83000000;
	public static final int COMMAND_CLIP = 0x84000000;
	public static final int COMMAND_CENTER = 0x85000000;
	public static final int COMMAND_TEXTURE_INDEX = 0x86000000;
	public static final int COMMAND_AFFINE_INDEX = 0x87000000;
	public static final int COMMAND_PARALLEL_SCALE = 0x90000000;
	public static final int COMMAND_PARALLEL_SIZE = 0x91000000;
	public static final int COMMAND_PERSPECTIVE_FOV = 0x92000000;
	public static final int COMMAND_PERSPECTIVE_WH = 0x93000000;
	public static final int COMMAND_AMBIENT_LIGHT = 0xA0000000;
	public static final int COMMAND_DIRECTION_LIGHT = 0xA1000000;
	public static final int COMMAND_THRESHOLD = 0xaF000000;
	public static final int PRIMITVE_POINTS = 0x1000000;
	public static final int PRIMITVE_LINES = 0x2000000;
	public static final int PRIMITVE_TRIANGLES = 0x3000000;
	public static final int PRIMITVE_QUADS = 0x4000000;
	public static final int PRIMITVE_POINT_SPRITES = 0x5000000;
	public static final int POINT_SPRITE_LOCAL_SIZE = 0;
	public static final int POINT_SPRITE_PIXEL_SIZE = 1;
	public static final int POINT_SPRITE_PERSPECTIVE = 0;
	public static final int POINT_SPRITE_NO_PERS = 2;
	public static final int ENV_ATTR_LIGHTING = 1;
	public static final int ENV_ATTR_SPHERE_MAP = 2;
	public static final int ENV_ATTR_TOON_SHADING = 4;
	public static final int ENV_ATTR_SEMI_TRANSPARENT = 8;
	public static final int PATTR_LIGHTING = 1;
	public static final int PATTR_SPHERE_MAP = 2;
	public static final int PATTR_COLORKEY = 16;
	public static final int PATTR_BLEND_NORMAL = 0;
	public static final int PATTR_BLEND_HALF = 32;
	public static final int PATTR_BLEND_ADD = 64;
	public static final int PATTR_BLEND_SUB = 96;
	public static final int PDATA_NORMAL_NONE = 0;
	public static final int PDATA_NORMAL_PER_FACE = 512;
	public static final int PDATA_NORMAL_PER_VERTEX = 768;
	public static final int PDATA_COLOR_NONE = 0;
	public static final int PDATA_COLOR_PER_COMMAND = 1024;
	public static final int PDATA_COLOR_PER_FACE = 2048;
	public static final int PDATA_TEXURE_COORD_NONE = 0;
	public static final int PDATA_POINT_SPRITE_PARAMS_PER_CMD = 4096;
	public static final int PDATA_POINT_SPRITE_PARAMS_PER_FACE = 8192;
	public static final int PDATA_POINT_SPRITE_PARAMS_PER_VERTEX = 12288;
	public static final int PDATA_TEXURE_COORD = 12288;

	private boolean bound = false;
	private Graphics graphics;
	private Renderer renderer;

	public Graphics3D() {
		renderer = new Renderer();
	}

	public final synchronized void bind(Graphics graphics) throws IllegalStateException, NullPointerException {
		if (bound) {
			throw new IllegalStateException("Target already bound");
		}
		boolean changed = this.graphics != graphics;
		renderer.bind(graphics, changed);
		this.graphics = graphics;
		this.bound = true;
	}

	public final synchronized void release(Graphics graphics) throws IllegalArgumentException, NullPointerException {
		if (graphics != this.graphics) {
			throw new IllegalArgumentException("Unknown target");
		} else if (bound) {
			renderer.release(graphics);
			bound = false;
		}
	}

	public final void renderPrimitives(Texture texture, int x, int y,
									   FigureLayout layout, Effect3D effect,
									   int command, int numPrimitives, int[] vertexCoords,
									   int[] normals, int[] textureCoords, int[] colors) {
		if (layout == null || effect == null) {
			throw new NullPointerException();
		} else if (vertexCoords == null || normals == null || textureCoords == null || colors == null) {
			throw new NullPointerException();
		} else if (command < 0) {
			throw new IllegalArgumentException();
		} else if (numPrimitives <= 0 || numPrimitives >= 256) {
			throw new IllegalArgumentException();
		}
		layout.setOffset(x, y);
		renderer.render(texture, layout, command, numPrimitives, vertexCoords, textureCoords, colors);
	}

	public final void drawCommandList(Texture[] textures,
									  int x, int y,
									  FigureLayout layout,
									  Effect3D effect,
									  int[] commandList) {
		if (layout == null || effect == null || commandList == null) {
			throw new NullPointerException();
		}
		if (textures != null) {
			for (Texture texture : textures) {
				if (texture == null) {
					throw new NullPointerException();
				}
			}
		}
//		renderer.parse(layout.getMatrix());
	}

	public final void drawCommandList(Texture texture,
									  int x,
									  int y,
									  FigureLayout layout,
									  Effect3D effect,
									  int[] commandList) {
		Texture[] ta = null;
		if (texture != null) {
			ta = new Texture[]{texture};
		}
		drawCommandList(ta, x, y, layout, effect, commandList);
	}

	public final void renderFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect)
			throws IllegalStateException {
		checkTargetIsValid();

		if (figure == null || layout == null || effect == null) {
			throw new NullPointerException();
		}
		layout.setOffset(x, y);
		renderer.render(figure, layout);
	}

	public final void drawFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect) throws IllegalStateException {
		renderFigure(figure, x, y, layout, effect);
		flush();
	}

	public final void flush() throws IllegalStateException {
		checkTargetIsValid();
		renderer.flush();
	}

	public final void dispose() {
	}

	private void checkTargetIsValid() throws IllegalStateException {
		if (!bound) {
			throw new IllegalStateException("No target is bound");
		}
	}
}
