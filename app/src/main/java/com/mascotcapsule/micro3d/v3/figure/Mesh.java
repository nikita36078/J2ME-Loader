package com.mascotcapsule.micro3d.v3.figure;

public class Mesh {

	private int start;
	private int count;
	private Material material;

	public Mesh(int start, int count, Material material) {
		set(start, count, material);
	}

	public void set(int start, int count, Material material) {
		this.start = start;
		this.count = count;
		this.material = material;
	}

	public int getEnd() {
		return start + count;
	}

	public int getStart() {
		return start;
	}

	public int getCount() {
		return count;
	}

	public Material getMaterial() {
		return material;
	}

	public void increment() {
		count += 3;
	}
}
