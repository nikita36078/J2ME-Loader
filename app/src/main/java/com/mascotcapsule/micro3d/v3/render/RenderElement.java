package com.mascotcapsule.micro3d.v3.render;

import com.mascotcapsule.micro3d.v3.FigureLayout;
import com.mascotcapsule.micro3d.v3.figure.Renderable;

public class RenderElement {

	private Renderable renderable;
	private float[] glMVPMatrix;
	private float[] glCenter;

	public RenderElement() {
		this.glMVPMatrix = new float[16];
		this.glCenter = new float[2];
	}

	public void setRenderable(Renderable renderable) {
		this.renderable = renderable;
	}

	public void setLayout(FigureLayout layout) {
		System.arraycopy(layout.getMatrix(), 0, glMVPMatrix, 0, 16);
		System.arraycopy(layout.getGlCenter(), 0, glCenter, 0, 2);
	}

	public float[] getGlCenter() {
		return glCenter;
	}

	public float[] getMatrix() {
		return glMVPMatrix;
	}

	public Renderable getRenderable() {
		return renderable;
	}
}
