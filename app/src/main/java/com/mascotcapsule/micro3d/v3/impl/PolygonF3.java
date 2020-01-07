package com.mascotcapsule.micro3d.v3.impl;

public class PolygonF3 extends Polygon3 {
	public final Color color;

	public PolygonF3(int a, int b, int c, Color color, int attribute) {
		super(a, b, c, attribute);
		this.color = color;
	}
}
