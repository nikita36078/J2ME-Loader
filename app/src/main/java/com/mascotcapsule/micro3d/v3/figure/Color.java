package com.mascotcapsule.micro3d.v3.figure;

public class Color {
	public final float r;
	public final float g;
	public final float b;
	public final float a;

	public Color(int r, int g, int b, int a) {
		this.r = r / 255F;
		this.g = g / 255F;
		this.b = b / 255F;
		this.a = a / 255F;
	}
}
