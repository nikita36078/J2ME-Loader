package com.mascotcapsule.micro3d.v3.impl;

import com.mascotcapsule.micro3d.v3.AffineTrans;

public class Bone {
	public int parent;
	public AffineTrans mtx;
	public int start;
	public int end;

	public Bone(int parent, AffineTrans mtx, int start, int end) {
		this.parent = parent;
		this.mtx = mtx;
		this.start = start;
		this.end = end;
	}
}
