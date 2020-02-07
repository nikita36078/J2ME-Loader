package com.mascotcapsule.micro3d.v3.figure;

public class Polygon {
	public boolean specular;
	public boolean lighting;
	public boolean doubleFace;
	public int blendMode;
	public boolean transparent;

	public static final int BLENDING_MODE_NORMAL = 0;
	public static final int BLENDING_MODE_HALF = 1;
	public static final int BLENDING_MODE_ADD = 2;
	public static final int BLENDING_MODE_SUB = 3;

	public Polygon(int attribute) {
		specular = (attribute & 0x40) != 0;
		lighting = (attribute & 0x20) != 0;
		doubleFace = (attribute & 0x10) != 0;
		transparent = (attribute & 0x1) != 0;
		blendMode = (attribute >> 1) & 0x3;
	}

}
