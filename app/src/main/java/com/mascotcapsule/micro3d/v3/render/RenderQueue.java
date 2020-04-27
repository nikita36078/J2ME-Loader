package com.mascotcapsule.micro3d.v3.render;

import com.mascotcapsule.micro3d.v3.FigureLayout;
import com.mascotcapsule.micro3d.v3.figure.Renderable;

import java.util.ArrayList;

import javax.microedition.util.ArrayStack;

public class RenderQueue {

	private ArrayList<RenderElement> queue;
	ArrayStack<RenderElement> renderElementPool;

	public RenderQueue() {
		this.queue = new ArrayList<>();
		this.renderElementPool = new ArrayStack<>();
	}

	public void add(Renderable renderable, FigureLayout layout) {
		RenderElement element = renderElementPool.pop();
		if (element == null) {
			element = new RenderElement();
		}
		element.setLayout(layout);
		element.setRenderable(renderable);
		queue.add(element);
	}

	public void sort() {

	}

	public void clear() {
		for (RenderElement element : queue) {
			renderElementPool.push(element);
		}
		queue.clear();
	}

	public ArrayList<RenderElement> getQueue() {
		return queue;
	}
}
