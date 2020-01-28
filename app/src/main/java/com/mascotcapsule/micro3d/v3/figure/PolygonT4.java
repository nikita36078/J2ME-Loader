package com.mascotcapsule.micro3d.v3.figure;

public class PolygonT4 extends Polygon4 {

	public final int u1;
	public final int v1;
	public final int u2;
	public final int v2;
	public final int u3;
	public final int v3;
	public final int u4;
	public final int v4;

	public PolygonT4(int a, int b, int c, int d, int u1, int v1, int u2, int v2,
					 int u3, int v3, int u4, int v4, int attribute) {
		super(a, b, c, d, attribute);
		this.u1 = u1;
		this.v1 = v1;
		this.u2 = u2;
		this.v2 = v2;
		this.u3 = u3;
		this.v3 = v3;
		this.u4 = u4;
		this.v4 = v4;
	}
}
