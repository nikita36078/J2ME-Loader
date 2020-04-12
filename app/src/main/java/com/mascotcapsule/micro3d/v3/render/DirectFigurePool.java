package com.mascotcapsule.micro3d.v3.render;

import com.mascotcapsule.micro3d.v3.figure.DirectFigure;
import com.mascotcapsule.micro3d.v3.util.ObjectPool;

public class DirectFigurePool extends ObjectPool<DirectFigure> {

	public DirectFigurePool() {
		super(20);
	}

	@Override
	public DirectFigure create() {
		return new DirectFigure();
	}
}
