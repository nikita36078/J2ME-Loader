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

package ru.woesss.j2me.micro3d;

import com.motorola.graphics.j3d.Effect3D;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Stack;

abstract class RenderNode implements Runnable {
	protected void recycle() {}

	static final class FigureNode extends RenderNode {
		private final Stack<FigureNode> stack;
		private Render render;
		private Effect3dImpl effect;
		TextureImpl[] textures;
		private FigureLayoutImpl layout;
		private final FloatBuffer vertices;
		private final Model data;
		private final FigureImpl figure;
		private final FloatBuffer normals;
		private int x;
		private int y;

		FigureNode(Render render, FigureImpl impl, int x, int y, FigureLayoutImpl layout, Effect3dImpl effect) {
			stack = impl.stack;
			data = impl.data;
			this.figure = impl;
			vertices = ByteBuffer.allocateDirect(data.vertexArrayCapacity)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();
			if (data.originalNormals != null) {
				normals = ByteBuffer.allocateDirect(data.vertexArrayCapacity)
						.order(ByteOrder.nativeOrder()).asFloatBuffer();
			} else {
				normals = null;
			}
			setData(render, x, y, layout, effect);
		}

		void setData(Render render, int x, int y, FigureLayoutImpl layout, Effect3dImpl effect) {
			this.render = render;
			if (this.layout != null) {
				this.layout.set(layout);
			} else {
				this.layout = new FigureLayoutImpl(layout);
			}
			if (this.effect != null) {
				this.effect.set(effect);
			} else {
				this.effect = new Effect3dImpl(effect);
			}
			this.effect.isToonShading = effect.toonHigh == Effect3D.TOON_SHADING;
			this.x = x;
			this.y = y;
			synchronized (figure) {
				Utils.fillBuffer(vertices, data.vertices, data.indices);
				if (normals != null) {
					Utils.fillBuffer(normals, data.normals, data.indices);
				}
			}
		}

		@Override
		public void run() {
			render.renderFigure(data, x, y, textures, vertices, normals, layout, effect);
		}

		@Override
		protected void recycle() {
			stack.push(this);
		}
	}
}
