package com.mascotcapsule.micro3d.v3.figure;

public class PolygonF4 extends Polygon4 {
	public final Color color;

	public PolygonF4(int a, int b, int c, int d, Color color, int attribute) {
		super(a, b, c, d, attribute * 2);
		this.color = color;
	}
}
