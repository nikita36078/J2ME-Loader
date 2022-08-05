/*
 * Copyright 2020 Yury Kharchenko
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

import androidx.preference.PreferenceManager;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.ViewHandler;
import javax.microedition.util.ContextHolder;

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
	public static final int COMMAND_THRESHOLD = 0xAF000000;
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

	private boolean isBound = false;
	private Graphics graphics;
	private Render render;

	public Graphics3D() {
	}

	public final synchronized void bind(Graphics graphics) {
		if (graphics == null) {
			throw new NullPointerException("Argument 'Graphics' is NULL");
		}
		if (isBound || render != null) {
			throw new IllegalStateException("Target already bound");
		}
		render = Render.getRender();
		render.bind(graphics);
		this.graphics = graphics;
		isBound = true;
	}

	public final synchronized void release(Graphics graphics) {
		if (graphics == null) {
			throw new NullPointerException("Argument 'Graphics' is NULL");
		}
		if (graphics != this.graphics) {
			if (render != null) {
				render.reset();
			}
			render = null;
			isBound = false;
			return;
		}
		if (isBound) {
			render.release();
			isBound = false;
		}
		render = null;
	}

	public final void renderPrimitives(Texture texture, int x, int y,
									   FigureLayout layout, Effect3D effect,
									   int command, int numPrimitives, int[] vertexCoords,
									   int[] normals, int[] textureCoords, int[] colors) {
		if (!isBound) return;
		if (layout == null || effect == null || vertexCoords == null || normals == null
				|| textureCoords == null || colors == null) {
			throw new NullPointerException();
		}
		if (command < 0 || numPrimitives <= 0 || numPrimitives >= 256) {
			throw new IllegalArgumentException();
		}
		render.postPrimitives(texture, x, y, layout, effect, command, numPrimitives,
				vertexCoords.clone(), normals.clone(), textureCoords.clone(), colors.clone());
	}

	public final void drawCommandList(Texture[] textures,
									  int x, int y,
									  FigureLayout layout,
									  Effect3D effect,
									  int[] commandList) {
		if (!isBound) return;
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
		render.drawCmd(textures, x, y, layout, effect, commandList);
	}

	public final void drawCommandList(Texture texture,
									  int x,
									  int y,
									  FigureLayout layout,
									  Effect3D effect,
									  int[] commandList) {
		if (!isBound) return;
		Texture[] ta = texture == null ? null : new Texture[]{texture};
		if (layout == null || effect == null || commandList == null) {
			throw new NullPointerException();
		}
		render.drawCmd(ta, x, y, layout, effect, commandList);
	}

	public final void renderFigure(Figure figure, int x, int y,
								   FigureLayout layout, Effect3D effect) {
		checkTargetIsValid();

		if (figure == null || layout == null || effect == null) {
			throw new NullPointerException();
		}
		render.postFigure(figure, x, y, layout, effect);
	}

	public final void drawFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect) {
		checkTargetIsValid();
		if (figure == null || layout == null || effect == null) {
			throw new NullPointerException();
		}
		render.drawFigure(figure, x, y, layout, effect);
	}

	public final void flush() {
		checkTargetIsValid();
		render.flush();
	}

	public final void dispose() {
		// TODO: 27.01.2020 not implemented method
	}

	private void checkTargetIsValid() throws IllegalStateException {
		if (!isBound) {
			throw new IllegalStateException("No target is bound");
		}
	}

	static {
		if (PreferenceManager.getDefaultSharedPreferences(ContextHolder.getAppContext()).getBoolean("micro3d_using_message", false)) {
			ViewHandler.postEvent(
					() -> Toast.makeText(ContextHolder.getAppContext(),
							"Mascot Capsule 3D!",
							Toast.LENGTH_LONG).show());
		}
	}
}
