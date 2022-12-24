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

import static com.mascotcapsule.micro3d.v3.Graphics3D.*;
import static ru.woesss.j2me.micro3d.MathUtil.TO_FLOAT;

import com.mascotcapsule.micro3d.v3.Graphics3D;

import ru.woesss.j2me.micro3d.ActTableImpl;
import ru.woesss.j2me.micro3d.FigureImpl;
import ru.woesss.j2me.micro3d.MathUtil;
import ru.woesss.j2me.micro3d.Render;
import ru.woesss.j2me.micro3d.TextureImpl;

public class RenderProxy {

	static void getViewTrans(AffineTransform a, float[] out) {
		out[ 0] = a.m00 * TO_FLOAT;
		out[ 1] = a.m10 * TO_FLOAT;
		out[ 2] = a.m20 * TO_FLOAT;
		out[ 3] = a.m01 * TO_FLOAT;
		out[ 4] = a.m11 * TO_FLOAT;
		out[ 5] = a.m21 * TO_FLOAT;
		out[ 6] = a.m02 * TO_FLOAT;
		out[ 7] = a.m12 * TO_FLOAT;
		out[ 8] = a.m22 * TO_FLOAT;
		out[ 9] = a.m03;
		out[10] = a.m13;
		out[11] = a.m23;
	}

	static void setProjection(Render render, FigureNode node) {
		int[] params = node.projectionParameters;
		switch (node.projectionType) {
			case Layout3D.PARALLEL_SCALE: {
				int sx = params[0];
				int sy = params[1];
				render.setOrthographicScale(sx, sy);
				break;
			}
			case Layout3D.PARALLEL_WIDTH: {
				int w = params[0];
				render.setOrthographicW(w);
				break;
			}
			case Layout3D.PARALLEL_WIDTH_HEIGHT: {
				int w = params[0];
				int h = params[1];
				render.setOrthographicWH(w, h);
				break;
			}
			case Layout3D.PERSPECTIVE_FOV: {
				int near = params[0];
				int far = params[1];
				int angle = params[2];
				render.setPerspectiveFov(near, far, angle);
				break;
			}
			case Layout3D.PERSPECTIVE_WIDTH: {
				int near = params[0];
				int far = params[1];
				int w = params[2];
				render.setPerspectiveW(near, far, w);
				break;
			}
			case Layout3D.PERSPECTIVE_WIDTH_HEIGHT: {
				int near = params[0];
				int far = params[1];
				int w = params[2];
				int h = params[3];
				render.setPerspectiveWH(near, far, w, h);
				break;
			}
		}
	}

	static Texture getTexture(Object3D obj) {
		Texture texture = obj.texture;
		if (texture instanceof MultiTexture) {
			MultiTexture mt = (MultiTexture) texture;
			return mt.getCurrentTexture();
		}
		return texture;
	}

	public static boolean checkNormals(Vector3D[] normals) {
		if (normals == null) {
			return false;
		}
		for (Vector3D normal : normals) {
			if (normal == null) {
				return false;
			}
		}
		return true;
	}

	static abstract class RenderNode {
		protected final int x;
		protected final int y;
		protected final int projectionType;
		protected final int[] projectionParameters;
		protected final float[] viewMatrix = new float[12];
		protected TextureImpl texture;
		protected int toonThreshold;
		protected int toonHighColor;
		protected int toonLowColor;
		protected TextureImpl sphereTexture;
		protected int attrs = 0;
		protected Light light;


		RenderNode(Object3D object3d, int x, int y) {
			this.x = x;
			this.y = y;
			Layout3D layout = object3d.layout;
			Light light = layout.light;
			if (light != null) {
				this.light = new Light(light);
				attrs |= Graphics3D.ENV_ATTR_LIGHTING;
			} else {
				attrs &= ~Graphics3D.ENV_ATTR_LIGHTING;
			}

			if (layout.isToonShaded) {
				attrs |= ENV_ATTR_TOON_SHADING;
				toonThreshold = layout.toonThreshold;
				toonHighColor = layout.toonHighColor;
				toonLowColor = layout.toonLowColor;
			} else {
				attrs &= ~Graphics3D.ENV_ATTR_TOON_SHADING;
			}

			if (layout.isSemiTransparent) {
				attrs |= Graphics3D.ENV_ATTR_SEMI_TRANSPARENT;
			} else {
				attrs &= ~Graphics3D.ENV_ATTR_SEMI_TRANSPARENT;
			}

			Texture specular = object3d.sphereTexture;
			if (specular != null) {
				attrs |= Graphics3D.ENV_ATTR_SPHERE_MAP;
				sphereTexture = specular.impl;
			} else {
				attrs &= ~Graphics3D.ENV_ATTR_SPHERE_MAP;
			}

			projectionParameters = layout.getProjectionParameters();
			projectionType = layout.projectionType;

			AffineTransform viewPointTransform = layout.getViewPointTransform();
			AffineTransform viewTransform = layout.getViewTransform();
			if (viewTransform != null) {
				getViewTrans(AffineTransform.multiply(viewPointTransform, viewTransform), viewMatrix);
			} else {
				getViewTrans(viewPointTransform, viewMatrix);
				Vector3D axis = layout.rotateAxis;
				int rotateAngle = layout.rotateAngle;
				if (axis != null && rotateAngle != 0) {
					MathUtil.rotateM12(viewMatrix, axis.x, axis.y, axis.z, rotateAngle);
				} else {
					viewMatrix[0] = 1.0f;
					viewMatrix[4] = 1.0f;
					viewMatrix[8] = 1.0f;
				}
			}
		}

		abstract void push(Render render);
	}

	static class FigureNode extends RenderNode {
		private final FigureImpl figure;
		private final int actionIndex;
		private final int frameIndex;
		private final int pattern;

		private ActTableImpl actionTable;
		private TextureImpl[] texArray;

		public FigureNode(Figure figure, int x, int y) {
			super(figure, x, y);
			this.figure = figure.impl;
			ActionTable actionTable = figure.actionTable;
			if (actionTable != null) {
				this.actionTable = actionTable.impl;
			}
			actionIndex = figure.actionIndex;
			frameIndex = figure.frameIndex;
			pattern = figure.pattern;

			Texture texture = figure.texture;
			if (texture instanceof MultiTexture) {
				MultiTexture mt = (MultiTexture) texture;
				int size = mt.size;
				if (size > 0) {
					texArray = new TextureImpl[size];
					for (int i = 0; i < size; i++) {
						texArray[i] = mt.textures[i].impl;
					}
				} else {
					Texture t = mt.getCurrentTexture();
					if (t != null) {
						this.texture = t.impl;
					}
				}
			} else if (texture != null) {
				this.texture = texture.impl;
			}
		}

		@Override
		void push(Render render) {
			if (light != null) {
				Vector3D dir = light.direction;
				if (dir != null) {
					render.setLight(light.ambientIntensity, light.directionalIntensity, dir.x, dir.y, dir.z);
				} else {
					render.setLight(light.ambientIntensity, 0, 0, 0, 0);
				}
			}

			render.setToonParam(toonThreshold, toonHighColor, toonLowColor);

			if (sphereTexture != null) {
				render.setSphereTexture(sphereTexture);
			}

			render.setAttribute(attrs);
			render.setCenter(this.x, this.y);
			RenderProxy.setProjection(render, this);
			System.arraycopy(viewMatrix, 0, render.getViewMatrix(), 0, 12);
			if (texArray != null) {
				render.setTextureArray(texArray);
			} else {
				render.setTexture(texture);
			}

			figure.setPosture(actionTable, actionIndex, frameIndex, pattern);
			render.postFigure(figure);
		}
	}

	static class PrimitiveNode extends RenderNode {
		int command = 0;
		int[] data;
		int normOffset = 0;
		int texOffset = 0;
		int colorOffset = 0;

		PrimitiveNode(Primitive primitive, int x, int y) {
			super(primitive, x, y);
			if (primitive instanceof Point) {
				Point point = (Point) primitive;

				command = PRIMITVE_POINTS | PDATA_NORMAL_NONE | 1 << 16;
				command |= PDATA_COLOR_PER_COMMAND | PDATA_TEXURE_COORD_NONE;
				command |= point.blendingType - Primitive.BLENDING_NONE << 5;
				command |= PATTR_LIGHTING | PATTR_SPHERE_MAP;

				Vector3D v = point.vertices[0];
				data = new int[]{v.x, v.y, v.z, point.color};
				colorOffset = 3;
			} else if (primitive instanceof Line) {
				Line line = (Line) primitive;

				command = PRIMITVE_LINES | PDATA_NORMAL_NONE | 1 << 16;
				command |= PDATA_COLOR_PER_COMMAND | PDATA_TEXURE_COORD_NONE;
				command |= line.blendingType - Primitive.BLENDING_NONE << 5;
				command |= PATTR_LIGHTING | PATTR_SPHERE_MAP;

				Vector3D v1 = line.vertices[0];
				Vector3D v2 = line.vertices[1];
				data = new int[]{v1.x, v1.y, v1.z, v2.x, v2.y, v2.z, line.color};
				colorOffset = 6;
			} else if (primitive instanceof Triangle) {
				Triangle triangle = (Triangle) primitive;

				int dataSize = 3 * 3;
				command = PRIMITVE_TRIANGLES | 1 << 16;
				command |= PATTR_SPHERE_MAP;
				Vector3D[] normals = triangle.normals;
				if (triangle.hasFaceNormal) {
					command |= PDATA_NORMAL_PER_FACE | PATTR_LIGHTING;
					dataSize += 3;
				} else if (RenderProxy.checkNormals(normals)) {
					command |= PDATA_NORMAL_PER_VERTEX | PATTR_LIGHTING;
					dataSize += 3 * 3;
				}
				Texture texture = RenderProxy.getTexture(triangle);
				if (texture != null) {
					this.texture = texture.impl;
					command |= PDATA_COLOR_NONE | PDATA_TEXURE_COORD;
					dataSize += 6;
				} else {
					command |= PDATA_COLOR_PER_COMMAND | PDATA_TEXURE_COORD_NONE;
					dataSize += 1;

				}
				command |= triangle.blendingType - Primitive.BLENDING_NONE << 5;
				if (triangle.isColorKey) {
					command |= PATTR_COLORKEY;
				}

				data = new int[dataSize];
				Vector3D[] vertices = triangle.vertices;
				normOffset = addVertices(data, 0, vertices, 3);
				texOffset = normOffset;
				if ((command & PDATA_NORMAL_PER_FACE) != 0) {
					texOffset = addVertices(data, normOffset, normals, 1);
				} else if ((command & PDATA_NORMAL_PER_VERTEX) != 0) {
					texOffset = addVertices(data, normOffset, normals, 3);
				}
				colorOffset = texOffset;
				if ((command & PDATA_TEXURE_COORD) != 0) {
					System.arraycopy(triangle.textureCoords, 0, data, texOffset, 6);
					colorOffset += 6;
				}
			} else if (primitive instanceof Quadrangle) {
				Quadrangle quadrangle = (Quadrangle) primitive;

				int dataSize = 4 * 3;
				command = PRIMITVE_QUADS | 1 << 16;
				command |= PATTR_SPHERE_MAP;
				Vector3D[] normals = quadrangle.normals;
				if (quadrangle.hasFaceNormal) {
					command |= PDATA_NORMAL_PER_FACE | PATTR_LIGHTING;
					dataSize += 3;
				} else if (RenderProxy.checkNormals(normals)) {
					command |= PDATA_NORMAL_PER_VERTEX | PATTR_LIGHTING;
					dataSize += 4 * 3;
				}
				Texture texture = RenderProxy.getTexture(quadrangle);
				if (texture != null) {
					this.texture = texture.impl;
					command |= PDATA_COLOR_NONE | PDATA_TEXURE_COORD;
					dataSize += 8;
				} else {
					command |= PDATA_COLOR_PER_COMMAND | PDATA_TEXURE_COORD_NONE;
					dataSize += 1;

				}
				command |= quadrangle.blendingType - Primitive.BLENDING_NONE << 5;
				if (quadrangle.isColorKey) {
					command |= PATTR_COLORKEY;
				}

				data = new int[dataSize];
				Vector3D[] vertices = quadrangle.vertices;
				normOffset = addVertices(data, 0, vertices, 4);
				texOffset = normOffset;
				if ((command & PDATA_NORMAL_PER_FACE) != 0) {
					texOffset = addVertices(data, normOffset, normals, 1);
				} else if ((command & PDATA_NORMAL_PER_VERTEX) != 0) {
					texOffset = addVertices(data, normOffset, normals, 4);
				}
				colorOffset = texOffset;
				if ((command & PDATA_TEXURE_COORD) != 0) {
					System.arraycopy(quadrangle.textureCoords, 0, data, texOffset, 8);
					colorOffset += 8;
				}
			} else if (primitive instanceof PointSprite) {
				PointSprite sprite = (PointSprite) primitive;
				Texture texture = RenderProxy.getTexture(sprite);
				if (texture == null) {
					return;
				}

				command = PRIMITVE_POINT_SPRITES | 1 << 16 | PDATA_TEXURE_COORD;
				if (sprite.isColorKey) {
					command |= PATTR_COLORKEY;
				}

				data = new int[11];
				Vector3D v = sprite.vertices[0];
				data[0] = v.x;
				data[1] = v.y;
				data[2] = v.z;
				System.arraycopy(sprite.params, 0, data, 3, 8);
				texOffset = 3;
			}
		}

		@Override
		void push(Render render) {
			render.postPrimitives(command, data, 0, data, normOffset, data, texOffset, data, colorOffset);
		}

		private static int addVertices(int[] data, int offset, Vector3D[] vertices, int len) {
			for (int i = 0; i < len; i++) {
				Vector3D v = vertices[i];
				data[offset++] = v.x;
				data[offset++] = v.y;
				data[offset++] = v.z;
			}
			return offset;
		}
	}
}
