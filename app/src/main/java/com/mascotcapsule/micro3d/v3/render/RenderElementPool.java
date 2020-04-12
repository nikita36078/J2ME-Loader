package com.mascotcapsule.micro3d.v3.render;

import com.mascotcapsule.micro3d.v3.util.ObjectPool;

public class RenderElementPool extends ObjectPool<RenderElement> {

	public RenderElementPool() {
		super(20);
	}

	@Override
	public RenderElement create() {
		return new RenderElement();
	}
}
