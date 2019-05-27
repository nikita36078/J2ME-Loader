package com.mascotcapsule.micro3d.v3.impl;

public class Bone {
	public int parent;
	public int[] mtx;
	public int start;
	public int end;

	public Bone(int parent, int[] mtx, int start, int end) {
		this.parent = parent;
		this.mtx = mtx;
		this.start = start;
		this.end = end;
	}
}
