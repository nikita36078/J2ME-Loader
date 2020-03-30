package com.mascotcapsule.micro3d.v3.figure;

public class Material {
	public int start;
	public int count;
	public int blendMode;
	public int textureId;
	public boolean transparent;

	public Material() {
	}

	public Material(int start, int count, int blendMode, int textureId, boolean transparent) {
		this.start = start;
		this.count = count;
		this.blendMode = blendMode;
		this.textureId = textureId;
		this.transparent = transparent;
	}

	public void set(int start, int count, int blendMode, int textureId, boolean transparent) {
		this.start = start;
		this.count = count;
		this.blendMode = blendMode;
		this.textureId = textureId;
		this.transparent = transparent;
	}
}
