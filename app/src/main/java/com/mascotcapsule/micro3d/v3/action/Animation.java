package com.mascotcapsule.micro3d.v3.action;

public class Animation extends BaseAnimation {
	private int type;
	private int start;
	private int x;
	private int y;
	private int z;

	public Animation(int type, int start, int x, int y, int z) {
		this.type = type;
		this.start = start;
		this.x = x;
		this.y = y;
		this.z = z;
	}
}
