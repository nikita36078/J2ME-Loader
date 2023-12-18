/*
 * Copyright 2022 Yury Kharchenko
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

package ru.woesss.j2me.micro3d;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

abstract class RenderNode {
	final float[] viewMatrix = new float[12];
	final float[] projMatrix = new float[16];
	int attrs;
	Light light;
	TextureImpl specular;
	int toonHigh;
	int toonLow;
	int toonThreshold;

	RenderNode() {}

	void setData(Render render) {
		Render.Environment env = render.env;
		System.arraycopy(env.viewMatrix, 0, viewMatrix, 0, 12);
		System.arraycopy(env.projMatrix, 0, projMatrix, 0, 16);
		attrs = env.attrs;
		Light light = env.light;
		if (this.light == null) {
			this.light = new Light(light);
		} else {
			this.light.set(light.ambIntensity, light.dirIntensity, light.x, light.y, light.z);
		}

		specular = env.specular;
		toonHigh = env.toonHigh;
		toonLow = env.toonLow;
		toonThreshold = env.toonThreshold;
	}

	abstract void render(Render render);

	void recycle() {}

	static final class FigureNode extends RenderNode {
		TextureImpl[] textures;
		final FloatBuffer vertices;
		final FigureImpl figure;
		final FloatBuffer normals;

		FigureNode(Render render, FigureImpl figure) {
			this.figure = figure;
			Model model = figure.model;
			vertices = BufferUtils.createFloatBuffer(model.vertexArrayCapacity);
			if (model.originalNormals != null) {
				normals = BufferUtils.createFloatBuffer(model.vertexArrayCapacity);
			} else {
				normals = null;
			}
			setData(render);
		}

		@Override
		void setData(Render render) {
			super.setData(render);
			Render.Environment env = render.env;
			textures = new TextureImpl[env.texturesLen];
			System.arraycopy(env.textures, 0, textures, 0, env.texturesLen);
			figure.fillBuffers(vertices, normals);
		}

		@Override
		void render(Render render) {
			render.renderFigure(figure.model,
					textures,
					attrs,
					projMatrix,
					viewMatrix,
					vertices,
					normals,
					light,
					specular,
					toonThreshold,
					toonHigh,
					toonLow);
		}

		@Override
		void recycle() {
			figure.stack.push(this);
		}
	}

	static final class PrimitiveNode extends RenderNode {
		final int command;
		final FloatBuffer vertices;
		final FloatBuffer normals;
		final ByteBuffer texCoords;
		final ByteBuffer colors;
		final TextureImpl texture;

		PrimitiveNode(Render render, int command,
					  FloatBuffer vertices, FloatBuffer normals,
					  ByteBuffer texCoords, ByteBuffer colors) {
			setData(render);
			Render.Environment env = render.env;
			this.texture = env.getTexture();
			this.command = command;
			this.vertices = vertices;
			this.normals = normals;
			this.texCoords = texCoords;
			this.colors = colors;
		}

		@Override
		void render(Render render) {
			render.renderPrimitive(this);
		}
	}
}
