package com.mascotcapsule.micro3d.v3.figure;

import androidx.annotation.Nullable;

public class Material {

	private int blendMode;
	private int textureId;
	private boolean transparent;

	public Material() {
	}

	public Material(Material copy) {
		this.blendMode = copy.blendMode;
		this.textureId = copy.textureId;
		this.transparent = copy.transparent;
	}

	public void set(int blendMode, int textureId, boolean transparent) {
		this.blendMode = blendMode;
		this.textureId = textureId;
		this.transparent = transparent;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		Material material = (Material) obj;
		return blendMode == material.blendMode && textureId == material.textureId &&
				transparent == material.transparent;
	}

	public void copy(PolygonT3 polygon) {
		this.blendMode = polygon.blendMode;
		this.textureId = polygon.textureId;
		this.transparent = polygon.transparent;
	}

	public void copy(PolygonF3 polygon) {
		this.blendMode = polygon.blendMode;
		this.textureId = -1;
		this.transparent = polygon.transparent;
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
}
