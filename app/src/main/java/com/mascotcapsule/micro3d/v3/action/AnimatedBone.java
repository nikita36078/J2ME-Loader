package com.mascotcapsule.micro3d.v3.action;

import java.util.ArrayList;

public class AnimatedBone {
	private ArrayList<BaseAnimation> animations;

	public AnimatedBone() {
		animations = new ArrayList<>();
	}

	public void add(BaseAnimation animation) {
		animations.add(animation);
	}
}
