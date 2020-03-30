package com.mascotcapsule.micro3d.v3.figure;

import com.mascotcapsule.micro3d.v3.Graphics3D;
import com.mascotcapsule.micro3d.v3.Texture;
import com.mascotcapsule.micro3d.v3.Util3D;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class DirectFigure implements Renderable {

	public FloatBuffer vboPolyT;
	public FloatBuffer vboPolyF;
	public int numPolyT;
	public int numPolyF;
	public Texture texture;
	public int blendMode;
	public boolean transparent;
	public ArrayList<Material> materials;

	public DirectFigure() {
		vboPolyT = ByteBuffer.allocateDirect(255 * 30 * 4)
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		vboPolyF = ByteBuffer.allocateDirect(255 * 42 * 4)
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		materials = new ArrayList();
		materials.add(new Material());
	}

	public void parse(Texture texture, int command,
					  int numPrimitives, int[] vertexCoords,
					  int[] textureCoords, int[] colors) {
		numPolyT = 0;
		numPolyF = 0;
		vboPolyT.position(0);
		vboPolyF.position(0);
		this.texture = texture;

		if ((command & Graphics3D.PRIMITVE_POINT_SPRITES) == Graphics3D.PRIMITVE_POINT_SPRITES) {
			if ((command & Graphics3D.PDATA_POINT_SPRITE_PARAMS_PER_VERTEX) == Graphics3D.PDATA_POINT_SPRITE_PARAMS_PER_VERTEX) {
				numPolyT = 2 * numPrimitives;
				addVertexSprites(numPrimitives, vertexCoords, textureCoords);
			} else if ((command & Graphics3D.PDATA_POINT_SPRITE_PARAMS_PER_FACE) == Graphics3D.PDATA_POINT_SPRITE_PARAMS_PER_FACE) {
				numPolyT = 2 * numPrimitives;
				addVertexSprites(numPrimitives, vertexCoords, textureCoords);
			} else if ((command & Graphics3D.PDATA_POINT_SPRITE_PARAMS_PER_CMD) == Graphics3D.PDATA_POINT_SPRITE_PARAMS_PER_CMD) {
				numPolyT = 2 * numPrimitives;
				addSprites(numPrimitives, vertexCoords, textureCoords);
			}
		} else if ((command & Graphics3D.PRIMITVE_TRIANGLES) == Graphics3D.PRIMITVE_TRIANGLES) {
			if ((command & Graphics3D.PDATA_COLOR_PER_COMMAND) == Graphics3D.PDATA_COLOR_PER_COMMAND) {
				numPolyF = numPrimitives;
				addFTriangles(numPrimitives, vertexCoords, colors);
			} else if ((command & Graphics3D.PDATA_TEXURE_COORD) == Graphics3D.PDATA_TEXURE_COORD) {
				numPolyT = numPrimitives;
				addTTriangles(numPrimitives, vertexCoords, textureCoords);
			}
		} else if ((command & Graphics3D.PRIMITVE_QUADS) == Graphics3D.PRIMITVE_QUADS) {
			if ((command & Graphics3D.PDATA_COLOR_PER_COMMAND) == Graphics3D.PDATA_COLOR_PER_COMMAND) {
				numPolyF = numPrimitives * 2;
				addFQuads(numPrimitives, vertexCoords, colors);
			} else if ((command & Graphics3D.PDATA_TEXURE_COORD) == Graphics3D.PDATA_TEXURE_COORD) {
				numPolyT = numPrimitives * 2;
				addTQuads(numPrimitives, vertexCoords, textureCoords);
			}
		}

		int blendMode = (command & Graphics3D.PATTR_BLEND_SUB) >> 5;
		boolean transparent = (command & Graphics3D.PATTR_COLORKEY) == Graphics3D.PATTR_COLORKEY;
		materials.get(0).set(0, (numPolyT + numPolyF) * 3, blendMode, 0, transparent);
	}

	private void addSprites(int numPrimitives, int[] vertexCoords, int[] textureCoords) {
		int x, y, z;
		float[] size = texture.getSize();
		float m = Util3D.sqrt((int) (size[0] * size[0] + size[1] * size[1] + 512 * 512));
		float w0 = textureCoords[0];
		float h0 = textureCoords[1];
		int a0 = textureCoords[2];
		int x00 = textureCoords[3];
		int y00 = textureCoords[4];
		int x01 = textureCoords[5];
		int y01 = textureCoords[6];
		int f0 = textureCoords[7];
		if (f0 == Graphics3D.POINT_SPRITE_PIXEL_SIZE && w0 >= 240 && h0 >= 320) {
			w0 = (w0 / 240f);
			h0 = (h0 / -320f);
		} else {
			w0 = (w0 * m) / 1024;
			h0 = (h0 * m) / 1024;
		}

		for (int i = 0; i < numPrimitives; i++) {
			x = vertexCoords[i * 3];
			y = vertexCoords[i * 3 + 1];
			z = vertexCoords[i * 3 + 2];

			vboPolyT.put(x - w0);
			vboPolyT.put(y + h0);
			vboPolyT.put(z);
			vboPolyT.put(x00);
			vboPolyT.put(y00);

			vboPolyT.put(x - w0);
			vboPolyT.put(y - h0);
			vboPolyT.put(z);
			vboPolyT.put(x00);
			vboPolyT.put(y01);

			vboPolyT.put(x + w0);
			vboPolyT.put(y - h0);
			vboPolyT.put(z);
			vboPolyT.put(x01);
			vboPolyT.put(y01);

			vboPolyT.put(x + w0);
			vboPolyT.put(y - h0);
			vboPolyT.put(z);
			vboPolyT.put(x01);
			vboPolyT.put(y01);

			vboPolyT.put(x + w0);
			vboPolyT.put(y + h0);
			vboPolyT.put(z);
			vboPolyT.put(x01);
			vboPolyT.put(y00);

			vboPolyT.put(x - w0);
			vboPolyT.put(y + h0);
			vboPolyT.put(z);
			vboPolyT.put(x00);
			vboPolyT.put(y00);
		}
	}

	private void addVertexSprites(int numPrimitives, int[] vertexCoords, int[] textureCoords) {
		float w0, h0;
		int a0, x00, y00, x01, y01, f0;
		int x, y, z;
		float[] size = texture.getSize();
		float m = Util3D.sqrt((int) (size[0] * size[0] + size[1] * size[1] + 512 * 512));
		for (int i = 0; i < numPrimitives; i++) {
			w0 = textureCoords[i * 8];
			h0 = textureCoords[i * 8 + 1];
			a0 = textureCoords[i * 8 + 2];
			x00 = textureCoords[i * 8 + 3];
			y00 = textureCoords[i * 8 + 4];
			x01 = textureCoords[i * 8 + 5];
			y01 = textureCoords[i * 8 + 6];
			f0 = textureCoords[i * 8 + 7];
			if (f0 == Graphics3D.POINT_SPRITE_PIXEL_SIZE && w0 >= 240 && h0 >= 320) {
				w0 = (w0 / 240f);
				h0 = (h0 / -320f);
			} else {
				w0 = (w0 * m) / 1024;
				h0 = (h0 * m) / 1024;
			}

			x = vertexCoords[i * 3];
			y = vertexCoords[i * 3 + 1];
			z = vertexCoords[i * 3 + 2];

			vboPolyT.put(x - w0);
			vboPolyT.put(y + h0);
			vboPolyT.put(z);
			vboPolyT.put(x00);
			vboPolyT.put(y00);

			vboPolyT.put(x - w0);
			vboPolyT.put(y - h0);
			vboPolyT.put(z);
			vboPolyT.put(x00);
			vboPolyT.put(y01);

			vboPolyT.put(x + w0);
			vboPolyT.put(y - h0);
			vboPolyT.put(z);
			vboPolyT.put(x01);
			vboPolyT.put(y01);

			vboPolyT.put(x + w0);
			vboPolyT.put(y - h0);
			vboPolyT.put(z);
			vboPolyT.put(x01);
			vboPolyT.put(y01);

			vboPolyT.put(x + w0);
			vboPolyT.put(y + h0);
			vboPolyT.put(z);
			vboPolyT.put(x01);
			vboPolyT.put(y00);

			vboPolyT.put(x - w0);
			vboPolyT.put(y + h0);
			vboPolyT.put(z);
			vboPolyT.put(x00);
			vboPolyT.put(y00);
		}
	}

	private void addTTriangles(int numPrimitives, int[] vertexCoords, int[] textureCoords) {
		int x, y, z, u1, v1;
		for (int i = 0; i < numPrimitives * 3; i++) {
			x = vertexCoords[i * 3];
			y = vertexCoords[i * 3 + 1];
			z = vertexCoords[i * 3 + 2];
			u1 = textureCoords[i * 2];
			v1 = textureCoords[i * 2 + 1];

			vboPolyT.put(x);
			vboPolyT.put(y);
			vboPolyT.put(z);
			vboPolyT.put(u1);
			vboPolyT.put(v1);
		}
	}

	private void addTQuads(int numPrimitives, int[] vertexCoords, int[] textureCoords) {
		int x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, u1, v1, u2, v2, u3, v3, u4, v4;
		for (int i = 0; i < numPrimitives; i++) {
			x1 = vertexCoords[i * 12];
			y1 = vertexCoords[i * 12 + 1];
			z1 = vertexCoords[i * 12 + 2];
			x2 = vertexCoords[i * 12 + 3];
			y2 = vertexCoords[i * 12 + 4];
			z2 = vertexCoords[i * 12 + 5];
			x3 = vertexCoords[i * 12 + 6];
			y3 = vertexCoords[i * 12 + 7];
			z3 = vertexCoords[i * 12 + 8];
			x4 = vertexCoords[i * 12 + 9];
			y4 = vertexCoords[i * 12 + 10];
			z4 = vertexCoords[i * 12 + 11];

			u1 = textureCoords[i * 8];
			v1 = textureCoords[i * 8 + 1];
			u2 = textureCoords[i * 8 + 2];
			v2 = textureCoords[i * 8 + 3];
			u3 = textureCoords[i * 8 + 4];
			v3 = textureCoords[i * 8 + 5];
			u4 = textureCoords[i * 8 + 6];
			v4 = textureCoords[i * 8 + 7];

			vboPolyT.put(x1);
			vboPolyT.put(y1);
			vboPolyT.put(z1);
			vboPolyT.put(u1);
			vboPolyT.put(v1);

			vboPolyT.put(x2);
			vboPolyT.put(y2);
			vboPolyT.put(z2);
			vboPolyT.put(u2);
			vboPolyT.put(v2);

			vboPolyT.put(x3);
			vboPolyT.put(y3);
			vboPolyT.put(z3);
			vboPolyT.put(u3);
			vboPolyT.put(v3);

			vboPolyT.put(x3);
			vboPolyT.put(y3);
			vboPolyT.put(z3);
			vboPolyT.put(u3);
			vboPolyT.put(v3);

			vboPolyT.put(x4);
			vboPolyT.put(y4);
			vboPolyT.put(z4);
			vboPolyT.put(u4);
			vboPolyT.put(v4);

			vboPolyT.put(x1);
			vboPolyT.put(y1);
			vboPolyT.put(z1);
			vboPolyT.put(u1);
			vboPolyT.put(v1);
		}
	}

	private void addFTriangles(int numPrimitives, int[] vertexCoords, int[] colors) {
		int x, y, z;
		float r, g, b, a;
		int color = colors[0];
		r = ((color >> 16) & 0xFF) / 255F;
		g = ((color >> 8) & 0xFF) / 255F;
		b = (color & 0xFF) / 255F;
		a = 1.0F;
		for (int i = 0; i < numPrimitives * 3; i++) {
			x = vertexCoords[i * 3];
			y = vertexCoords[i * 3 + 1];
			z = vertexCoords[i * 3 + 2];

			vboPolyF.put(x);
			vboPolyF.put(y);
			vboPolyF.put(z);
			vboPolyF.put(r);
			vboPolyF.put(g);
			vboPolyF.put(b);
			vboPolyF.put(a);
		}
	}

	private void addFQuads(int numPrimitives, int[] vertexCoords, int[] colors) {
		int x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4;
		float r, g, b, a;
		int color = colors[0];
		r = ((color >> 16) & 0xFF) / 255F;
		g = ((color >> 8) & 0xFF) / 255F;
		b = (color & 0xFF) / 255F;
		a = 1.0F;
		for (int i = 0; i < numPrimitives; i++) {
			x1 = vertexCoords[i * 12];
			y1 = vertexCoords[i * 12 + 1];
			z1 = vertexCoords[i * 12 + 2];
			x2 = vertexCoords[i * 12 + 3];
			y2 = vertexCoords[i * 12 + 4];
			z2 = vertexCoords[i * 12 + 5];
			x3 = vertexCoords[i * 12 + 6];
			y3 = vertexCoords[i * 12 + 7];
			z3 = vertexCoords[i * 12 + 8];
			x4 = vertexCoords[i * 12 + 9];
			y4 = vertexCoords[i * 12 + 10];
			z4 = vertexCoords[i * 12 + 11];

			vboPolyF.put(x1);
			vboPolyF.put(y1);
			vboPolyF.put(z1);
			vboPolyF.put(r);
			vboPolyF.put(g);
			vboPolyF.put(b);
			vboPolyF.put(a);

			vboPolyF.put(x2);
			vboPolyF.put(y2);
			vboPolyF.put(z2);
			vboPolyF.put(r);
			vboPolyF.put(g);
			vboPolyF.put(b);
			vboPolyF.put(a);

			vboPolyF.put(x3);
			vboPolyF.put(y3);
			vboPolyF.put(z3);
			vboPolyF.put(r);
			vboPolyF.put(g);
			vboPolyF.put(b);
			vboPolyF.put(a);

			vboPolyF.put(x3);
			vboPolyF.put(y3);
			vboPolyF.put(z3);
			vboPolyF.put(r);
			vboPolyF.put(g);
			vboPolyF.put(b);
			vboPolyF.put(a);

			vboPolyF.put(x4);
			vboPolyF.put(y4);
			vboPolyF.put(z4);
			vboPolyF.put(r);
			vboPolyF.put(g);
			vboPolyF.put(b);
			vboPolyF.put(a);

			vboPolyF.put(x1);
			vboPolyF.put(y1);
			vboPolyF.put(z1);
			vboPolyF.put(r);
			vboPolyF.put(g);
			vboPolyF.put(b);
			vboPolyF.put(a);
		}
	}

	@Override
	public ArrayList<Material> getMaterials() {
		return materials;
	}

	@Override
	public FloatBuffer getVboPolyT() {
		return vboPolyT;
	}

	@Override
	public FloatBuffer getVboPolyF() {
		return vboPolyF;
	}

	@Override
	public int getNumPolyT() {
		return numPolyT;
	}

	@Override
	public int getNumPolyF() {
		return numPolyF;
	}

	@Override
	public Texture getTextureById(int idx) {
		return texture;
	}
}
