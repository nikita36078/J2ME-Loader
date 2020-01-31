package com.mascotcapsule.micro3d.v3.action;

import java.util.ArrayList;

public class Action {
	private ArrayList<AnimatedBone> bones;

	public Action() {
		bones = new ArrayList<>();
	}

	public void add(AnimatedBone bone) {
		bones.add(bone);
	}
}
