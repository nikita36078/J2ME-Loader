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

import java.util.LinkedList;

import javax.microedition.lcdui.Graphics;

import ru.woesss.j2me.micro3d.Render;

public class Renderer {
	private final LinkedList<RenderProxy.RenderNode> stack = new LinkedList<>();

	public Renderer() {}

	public void draw(Object3D object3d, int x, int y) {
		if (object3d == null) {
			throw new NullPointerException();
		}
		Layout3D layout = object3d.layout;
		if (layout == null) {
			return;
		}
		if (x < -2048) x = -2048;
		if (y < -2048) y = -2048;
		if (x > 2047) x = 2047;
		if (y > 2047) y = 2047;
		if (object3d instanceof Figure) {
			Figure figure = (Figure) object3d;
			stack.addLast(new RenderProxy.FigureNode(figure, x, y));
		} else if (object3d instanceof Primitive) {
			Primitive primitive = (Primitive) object3d;
			stack.addLast(new RenderProxy.PrimitiveNode(primitive, x, y));
		}
	}

	public void paint(Graphics g) {
		Render render = Render.getRender();
		render.bind(g);
		for (RenderProxy.RenderNode node : stack) {
			node.push(render);
		}
		stack.clear();
		render.flush();
		render.release();
	}

	public Texture paint(Texture texture, int color) {
		if (texture == null) {
			texture = new Texture(true);
		} else if (texture instanceof MultiTexture) {
			throw new IllegalArgumentException();
		} else if (!texture.isMutable()) {
			texture = new Texture(true);
		}
		Render render = Render.getRender();
		render.setClearColor(color);
		render.bind(texture.impl);
		for (RenderProxy.RenderNode node : stack) {
			node.push(render);
		}
		stack.clear();
		render.flush();
		render.release();
		return texture;
	}
}
