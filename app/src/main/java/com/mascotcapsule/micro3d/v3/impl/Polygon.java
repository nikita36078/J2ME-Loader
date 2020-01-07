package com.mascotcapsule.micro3d.v3.impl;

public class Polygon {
	public boolean lighting;
	public boolean doubleFace;
	public int blendMode;

	private static final int BLENDING_MODE_NORMAL = 0;
	private static final int BLENDING_MODE_HALF = 1;
	private static final int BLENDING_MODE_ADD = 2;
	private static final int BLENDING_MODE_SUB = 3;

	public Polygon(int attribute) {
		lighting = (attribute & 0x10) != 0;
		doubleFace = (attribute & 0x8) != 0;
		blendMode = attribute & 0x3;
	}

}
