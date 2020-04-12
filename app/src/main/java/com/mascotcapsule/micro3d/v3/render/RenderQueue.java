package com.mascotcapsule.micro3d.v3.render;

import com.mascotcapsule.micro3d.v3.FigureLayout;
import com.mascotcapsule.micro3d.v3.figure.Renderable;

import java.util.ArrayList;

public class RenderQueue {

	private ArrayList<RenderElement> queue;
	RenderElementPool renderElementPool;

	public RenderQueue() {
		this.queue = new ArrayList<>();
		this.renderElementPool = new RenderElementPool();
	}

	public void add(Renderable renderable, FigureLayout layout) {
		RenderElement element = renderElementPool.get();
		element.setLayout(layout);
		element.setRenderable(renderable);
		queue.add(element);
	}

	public void sort() {

	}

	public void clear() {
		for (RenderElement element : queue) {
			renderElementPool.release(element);
		}
		queue.clear();
	}

	public ArrayList<RenderElement> getQueue() {
		return queue;
	}
}
