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

public interface Graphics3D {
	int COMMAND_AFFINE_INDEX = -2030043136;
	int COMMAND_AMBIENT_LIGHT = -1610612736;
	int COMMAND_ATTRIBUTE = -2097152000;
	int COMMAND_CENTER = -2063597568;
	int COMMAND_CLIP = -2080374784;
	int COMMAND_DIRECTION_LIGHT = -1593835520;
	int COMMAND_END = -2147483648;
	int COMMAND_FLUSH = -2113929216;
	int COMMAND_LIST_VERSION_1_0 = -33554431;
	int COMMAND_NOP = -2130706432;
	int COMMAND_PARALLEL_SCALE = -1879048192;
	int COMMAND_PARALLEL_SIZE = -1862270976;
	int COMMAND_PERSPECTIVE_FOV = -1845493760;
	int COMMAND_PERSPECTIVE_WH = -1828716544;
	int COMMAND_TEXTURE_INDEX = -2046820352;
	int COMMAND_THRESHOLD = -1358954496;
	int ENV_ATTR_LIGHTING = 1;
	int ENV_ATTR_SEMI_TRANSPARENT = 8;
	int ENV_ATTR_SPHERE_MAP = 2;
	int ENV_ATTR_TOON_SHADING = 4;
	int PATTR_BLEND_ADD = 64;
	int PATTR_BLEND_HALF = 32;
	int PATTR_BLEND_NORMAL = 0;
	int PATTR_BLEND_SUB = 96;
	int PATTR_COLORKEY = 16;
	int PATTR_LIGHTING = 1;
	int PATTR_SPHERE_MAP = 2;
	int PDATA_COLOR_NONE = 0;
	int PDATA_COLOR_PER_COMMAND = 1024;
	int PDATA_COLOR_PER_FACE = 2048;
	int PDATA_NORMAL_NONE = 0;
	int PDATA_NORMAL_PER_FACE = 512;
	int PDATA_NORMAL_PER_VERTEX = 768;
	int PDATA_POINT_SPRITE_PARAMS_PER_CMD = 4096;
	int PDATA_POINT_SPRITE_PARAMS_PER_FACE = 8192;
	int PDATA_POINT_SPRITE_PARAMS_PER_VERTEX = 12288;
	int PDATA_TEXURE_COORD = 12288;
	int PDATA_TEXURE_COORD_NONE = 0;
	int POINT_SPRITE_LOCAL_SIZE = 0;
	int POINT_SPRITE_NO_PERS = 2;
	int POINT_SPRITE_PERSPECTIVE = 0;
	int POINT_SPRITE_PIXEL_SIZE = 1;
	int PRIMITIVE_LINES = 33554432;
	int PRIMITIVE_POINT_SPRITES = 83886080;
	int PRIMITIVE_POINTS = 16777216;
	int PRIMITIVE_QUADS = 67108864;
	int PRIMITIVE_TRIANGLES = 50331648;

	void drawCommandList(Texture[] textures,
						 int x,
						 int y,
						 FigureLayout layout,
						 Effect3D effect,
						 int[] commandlist);

	void drawCommandList(Texture texture,
						 int x,
						 int y,
						 FigureLayout layout,
						 Effect3D effect,
						 int[] commandlist);

	void drawFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect);

	void flush();

	void renderFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect);

	void renderPrimitives(Texture texture,
						  int x,
						  int y,
						  FigureLayout layout,
						  Effect3D effect,
						  int command,
						  int numPrimitives,
						  int[] vertexCoords,
						  int[] normals,
						  int[] textureCoords,
						  int[] colors);
}
