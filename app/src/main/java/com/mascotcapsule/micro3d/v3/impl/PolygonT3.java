package com.mascotcapsule.micro3d.v3.impl;

public class PolygonT3 extends Polygon3 {

	public final int u1;
	public final int v1;
	public final int u2;
	public final int v2;
	public final int u3;
	public final int v3;

	public PolygonT3(int a, int b, int c, int u1, int v1, int u2, int v2, int u3, int v3, int attribute) {
		super(a, b, c, attribute);
		this.u1 = u1;
		this.v1 = v1;
		this.u2 = u2;
		this.v2 = v2;
		this.u3 = u3;
		this.v3 = v3;
	}
}
