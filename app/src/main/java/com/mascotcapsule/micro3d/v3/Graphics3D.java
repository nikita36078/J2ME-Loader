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

import android.widget.Toast;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.ViewHandler;
import javax.microedition.util.ContextHolder;

public class Graphics3D {
	public static final int COMMAND_AFFINE_INDEX = -2030043136;
	public static final int COMMAND_AMBIENT_LIGHT = -1610612736;
	public static final int COMMAND_ATTRIBUTE = -2097152000;
	public static final int COMMAND_CENTER = -2063597568;
	public static final int COMMAND_CLIP = -2080374784;
	public static final int COMMAND_DIRECTION_LIGHT = -1593835520;
	public static final int COMMAND_END = Integer.MIN_VALUE;
	public static final int COMMAND_FLUSH = -2113929216;
	public static final int COMMAND_LIST_VERSION_1_0 = -33554431;
	public static final int COMMAND_NOP = -2130706432;
	public static final int COMMAND_PARALLEL_SCALE = -1879048192;
	public static final int COMMAND_PARALLEL_SIZE = -1862270976;
	public static final int COMMAND_PERSPECTIVE_FOV = -1845493760;
	public static final int COMMAND_PERSPECTIVE_WH = -1828716544;
	public static final int COMMAND_TEXTURE_INDEX = -2046820352;
	public static final int COMMAND_THRESHOLD = -1358954496;
	public static final int ENV_ATTR_LIGHTING = 1;
	public static final int ENV_ATTR_SEMI_TRANSPARENT = 8;
	public static final int ENV_ATTR_SPHERE_MAP = 2;
	public static final int ENV_ATTR_TOON_SHADING = 4;
	public static final int PATTR_BLEND_ADD = 64;
	public static final int PATTR_BLEND_HALF = 32;
	public static final int PATTR_BLEND_NORMAL = 0;
	public static final int PATTR_BLEND_SUB = 96;
	public static final int PATTR_COLORKEY = 16;
	public static final int PATTR_LIGHTING = 1;
	public static final int PATTR_SPHERE_MAP = 2;
	public static final int PDATA_COLOR_NONE = 0;
	public static final int PDATA_COLOR_PER_COMMAND = 1024;
	public static final int PDATA_COLOR_PER_FACE = 2048;
	public static final int PDATA_NORMAL_NONE = 0;
	public static final int PDATA_NORMAL_PER_FACE = 512;
	public static final int PDATA_NORMAL_PER_VERTEX = 768;
	public static final int PDATA_POINT_SPRITE_PARAMS_PER_CMD = 4096;
	public static final int PDATA_POINT_SPRITE_PARAMS_PER_FACE = 8192;
	public static final int PDATA_POINT_SPRITE_PARAMS_PER_VERTEX = 12288;
	public static final int PDATA_TEXURE_COORD = 12288;
	public static final int PDATA_TEXURE_COORD_NONE = 0;
	public static final int POINT_SPRITE_LOCAL_SIZE = 0;
	public static final int POINT_SPRITE_NO_PERS = 2;
	public static final int POINT_SPRITE_PERSPECTIVE = 0;
	public static final int POINT_SPRITE_PIXEL_SIZE = 1;
	public static final int PRIMITVE_LINES = 33554432;
	public static final int PRIMITVE_POINTS = 16777216;
	public static final int PRIMITVE_POINT_SPRITES = 83886080;
	public static final int PRIMITVE_QUADS = 67108864;
	public static final int PRIMITVE_TRIANGLES = 50331648;
	private static int ID = 0;
	private static boolean mIsBound = false;
	private Graphics mGraphics;

	private final void checkTargetIsValid() throws IllegalStateException {
		if (this.mGraphics == null) {
			throw new IllegalStateException("No target is bound");
		}
	}

	public Graphics3D() {
	}

	public final synchronized void bind(Graphics graphics) throws IllegalStateException, NullPointerException {
		if (mIsBound) {
			throw new IllegalStateException("Target already bound");
		}
		this.mGraphics = graphics;
		mIsBound = true;
	}

	public final synchronized void release(Graphics graphics) throws IllegalArgumentException, NullPointerException {
		if (graphics != this.mGraphics) {
			throw new IllegalArgumentException("Unknown target");
		} else if (graphics == this.mGraphics && mIsBound) {
			this.mGraphics = null;
			mIsBound = false;
		}
	}

	public final void renderPrimitives(Texture texture, int x, int y, FigureLayout layout, Effect3D effect, int command, int numPrimitives, int[] vertexCoords, int[] normals, int[] textureCoords, int[] colors) {
		if (layout == null || effect == null) {
			throw new NullPointerException();
		} else if (vertexCoords == null || normals == null || textureCoords == null || colors == null) {
			throw new NullPointerException();
		} else if (command < 0) {
			throw new IllegalArgumentException();
		} else if (numPrimitives <= 0 || numPrimitives >= 256) {
			throw new IllegalArgumentException();
		}
	}

	public final void drawCommandList(Texture[] textures, int x, int y, FigureLayout layout, Effect3D effect, int[] commandList) {
		if (layout == null || effect == null) {
			throw new NullPointerException();
		}
		if (textures != null) {
			for (Texture texture : textures) {
				if (texture == null) {
					throw new NullPointerException();
				}
			}
		}
		if (commandList == null) {
			throw new NullPointerException();
		}
	}

	public final void drawCommandList(Texture texture, int x, int y, FigureLayout layout, Effect3D effect, int[] commandList) {
		Texture[] ta = null;
		if (texture != null) {
			ta = new Texture[]{texture};
		}
		drawCommandList(ta, x, y, layout, effect, commandList);
	}

	public final void renderFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect) throws IllegalStateException {
		checkTargetIsValid();
		if (figure == null || layout == null || effect == null) {
			throw new NullPointerException();
		}
	}

	public final void drawFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect) throws IllegalStateException {
		checkTargetIsValid();
		if (figure == null || layout == null || effect == null) {
			throw new NullPointerException();
		}
	}

	public final void flush() throws IllegalStateException {
		checkTargetIsValid();
	}

	public final void dispose() {
	}

	static {
		ViewHandler.postEvent(
				() -> Toast.makeText(ContextHolder.getActivity(),
						"Mascot Capsule 3D!",
						Toast.LENGTH_LONG).show());
	}
}
