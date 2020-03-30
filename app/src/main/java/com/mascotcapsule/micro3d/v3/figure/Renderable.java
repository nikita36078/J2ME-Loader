package com.mascotcapsule.micro3d.v3.figure;

import com.mascotcapsule.micro3d.v3.Texture;

import java.nio.FloatBuffer;
import java.util.ArrayList;

public interface Renderable {

	public ArrayList<Material> getMaterialsT();

	public ArrayList<Material> getMaterialsF();

	public FloatBuffer getVboPolyT();

	public FloatBuffer getVboPolyF();

	public int getNumPolyT();

	public int getNumPolyF();

	public Texture getTextureById(int idx);
}
