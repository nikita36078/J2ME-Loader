package com.mascotcapsule.micro3d.v3.figure;

import androidx.annotation.Nullable;

public class Material {

	private int blendMode;
	private int textureId;
	private boolean transparent;
	private boolean doubleFace;

	public Material() {
	}

	public Material(Material copy) {
		set(copy.blendMode, copy.textureId, copy.transparent, copy.doubleFace);
	}

	public void set(int blendMode, int textureId, boolean transparent, boolean doubleFace) {
		this.blendMode = blendMode;
		this.textureId = textureId;
		this.transparent = transparent;
		this.doubleFace = doubleFace;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		Material material = (Material) obj;
		return blendMode == material.blendMode && textureId == material.textureId &&
				transparent == material.transparent;
	}

	public void copy(PolygonT3 polygon) {
		set(polygon.blendMode, polygon.textureId, polygon.transparent, polygon.doubleFace);
	}

	public void copy(PolygonF3 polygon) {
		set(polygon.blendMode, -1, polygon.transparent, polygon.doubleFace);
	}

	public int getBlendMode() {
		return blendMode;
	}

	public int getTextureId() {
		return textureId;
	}

	public boolean isTransparent() {
		return transparent;
	}

	public boolean isDoubleFace() {
		return doubleFace;
	}
}
