/*
 * Copyright 2018 David Richardson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nokia.mid.m3d;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class M3D {
	private double[] matrix = new double[16];

	private double[] stack = new double[16];

	private double[] tempm = new double[16];

	private double[] tempr = new double[16];

	private double[] tempt = new double[16];

	private double[] temps = new double[16];

	private double[] rotm = new double[16];

	private double[] projm = new double[16];

	private int width;

	private int height;

	private Texture texture;

	private boolean boundTexture = false;

	private double[] verts = new double[256];
	private double[] UVs = new double[256];
	private double[] faces = new double[384];
	private double[] faceUVs = new double[384];
	private int[] debugColors = new int[384];
	private int debugColor = 0xFF0000;
	private int vertCount;
	private int faceCount;

	private Image platformImage;
	private Graphics gc;

	private double[] zbuffer;
	private double near;
	private double far;

	private int color = 0xFF000000;
	private int clearcolor = 0xFFFFFFFF;

	public static M3D createInstance() {
		return new M3D();
	}

	public void setupBuffers(int flags, int displayWidth, int displayHeight) {
		// flags c.c (init?) passes 0x20 | 0x1

		width = displayWidth;
		height = displayHeight;
		platformImage = Image.createImage(width, height);
		gc = platformImage.getGraphics();
		zbuffer = new double[width * height];
		clear(0);
	}

	public void removeBuffers() {
	} // runs only on app shutdown

	public void cullFace(int mode) {
	} // guessing front or back facing? set to 1029

	public void viewport(int x, int y, int w, int h) {
	} // called once, always 0, 0, 96, 65

	public void clear(int mask) // GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT (16640)
	{
		gc.setColor(clearcolor);
		gc.fillRect(0, 0, width, height);
		gc.setColor(color);
		identity(matrix);
		identity(stack);
		for (int i = 0; i < zbuffer.length; i++) {
			zbuffer[i] = -128;
		}
		boundTexture = false;
	}

	public void matrixMode(int mode) {
	} // mode is 5889 for frustrum, 5888 other times,

	public void loadIdentity() {
		identity(matrix);
	}

	public void frustumxi(int left, int right, int top, int bottom, int nearclip, int farclip) //-3,3, -2,2, 3,1000
	{
		//System.out.println("frustrumxi("+a+", "+b+", "+c+", "+d+", "+near+", "+far+");");
		// c.c: bu.frustumxi(-bp << 11, bp << 11, -bo << 11, bo << 11, 196608, 65536000);
		double r = right / 2048;
		double l = left / 2048;
		double t = top / 2048;
		double b = bottom / 2048;

		double n = nearclip / 655360;
		double f = farclip / 655360;

		near = -0.01;//25;//-n;
		far = -f;
		projection(projm, r - l, b - t, 90, near, far);
	}

	public void scalexi(int X, int Y, int Z) {
		double x = (X / 65536.0);
		double y = (Y / 65536.0);
		double z = (Z / 65536.0);

		temps[0]  = x; temps[1]  = 0; temps[2]  = 0; temps[3]  = 0;
		temps[4]  = 0; temps[5]  = y; temps[6]  = 0; temps[7]  = 0;
		temps[8]  = 0; temps[9]  = 0; temps[10] = z; temps[11] = 0;
		temps[12] = 0; temps[13] = 0; temps[14] = 0; temps[15] = 1;

		matmul(temps, matrix);
		clone(matrix, temps);
	}

	public void translatexi(int X, int Y, int Z) {
		double x = (X / 65536.0);
		double y = (Y / 65536.0);
		double z = (Z / 65536.0);
		tempt[0]  = 1; tempt[1]  = 0; tempt[2]  = 0; tempt[3]  = 0;
		tempt[4]  = 0; tempt[5]  = 1; tempt[6]  = 0; tempt[7]  = 0;
		tempt[8]  = 0; tempt[9]  = 0; tempt[10] = 1; tempt[11] = 0;
		tempt[12] = x; tempt[13] = y; tempt[14] = z; tempt[15] = 1;

		matmul(tempt, matrix);
		clone(matrix, tempt);
	}

	public void rotatexi(int Y, int Z, int X, int W) // probably not a quaternion
	{
		//System.out.println("rotatexi("+Y+", "+Z+", "+X+", "+W+");");
		// 1 degree = 0.0174533 rad
		// from d:1343 rotatexi(1310720, 65536, 0, 0);
		// from d:1347 rotatexi(c0, 65536, 0, 0);
		// from d:1354 rotatexi(cx * 90, 0, 65536, 0);

		double x = (X / 65536.0) * 0.0174533;
		double y = ((Y / 65536.0) - 10) * 0.0174533;
		double z = (Z / 65536.0) * 0.0174533;

		// rotate on y
		tempr[0]  =  Math.cos(y); tempr[1]  =  0; tempr[2]  = -Math.sin(y); tempr[3]  =  0;
		tempr[4]  =  0;           tempr[5]  =  1; tempr[6]  =  0;           tempr[7]  =  0;
		tempr[8]  =  Math.sin(y); tempr[9]  =  0; tempr[10] =  Math.cos(y); tempr[11] =  0;
		tempr[12] =  0;           tempr[13] =  0; tempr[14] =  0;           tempr[15] =  1;
		clone(rotm, tempr);

		// rotate on x
		tempr[0]  =  1; tempr[1]  =  0;            tempr[2]  =  0;           tempr[3]  =  0;
		tempr[4]  =  0; tempr[5]  =  Math.cos(x);  tempr[6]  =  Math.sin(x); tempr[7]  =  0;
		tempr[8]  =  0; tempr[9]  = -Math.sin(x);  tempr[10] =  Math.cos(x); tempr[11] =  0;
		tempr[12] =  0; tempr[13] =  0;            tempr[14] =  0;           tempr[15] =  1;
		matmul(rotm, tempr);

		// rotate on z
		tempr[0]  =  Math.cos(z); tempr[1]  =  Math.sin(z); tempr[2]  =  0; tempr[3]  =  0;
		tempr[4]  = -Math.sin(z); tempr[5]  =  Math.cos(z); tempr[6]  =  0; tempr[7]  =  0;
		tempr[8]  =  0;           tempr[9]  =  0;           tempr[10] =  1; tempr[11] =  0;
		tempr[12] =  0;           tempr[13] =  0;           tempr[14] =  0; tempr[15] =  1;
		matmul(rotm, tempr);

		matmul(rotm, matrix);
		clone(matrix, rotm);
	}

	public void pushMatrix() // game doesn't seem to push more than one thing at a time
	{
		clone(stack, matrix);
	}

	public void popMatrix() {
		clone(matrix, stack);
	}

	public void color4ub(byte r, byte g, byte b, byte a) {
		color = ((0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}

	public void clearColor4ub(byte r, byte g, byte b, byte a) {
		clearcolor = ((0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}

	public void vertexPointerub(int a, int b, byte[] vertices) {
		for (int i = 0; i < vertices.length; i += 3) {
			verts[i] = vertices[i];
			verts[i + 1] = vertices[i + 1];
			verts[i + 2] = vertices[i + 2];
		}
		vertCount = vertices.length;
	}

	private void addFaces(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double u1, double v1, double u2, double v2, double u3, double v3) {
		debugColor = color;
		// add faces -- split triangles that intersect near plane
		if (z1 <= near && z2 <= near && z3 <= near) {
			addFace(x1, y1, z1, x2, y2, z2, x3, y3, z3, u1, v1, u2, v2, u3, v3);
			return;
		}
		// one point behind near plane, split in to two triangles
		if (z1 > near && z2 <= near && z3 <= near) {
			// point 1 behind near plane
			addFace2(x2, y2, z2, x3, y3, z3, x1, y1, z1, u2, v2, u3, v3, u1, v1);
			return;
		}
		if (z1 <= near && z2 > near && z3 <= near) {
			addFace2(x1, y1, z1, x3, y3, z3, x2, y2, z2, u1, v1, u3, v3, u2, v2);
			return;
		}
		if (z1 <= near && z2 <= near && z3 > near) {
			addFace2(x1, y1, z1, x2, y2, z2, x3, y3, z3, u1, v1, u2, v2, u3, v3);
			return;
		}
		// two points behind near plane, clip triangle
		if (z1 > near && z2 > near && z3 <= near) {
			addFace1(x3, y3, z3, x1, y1, z1, x2, y2, z2, u3, v3, u1, v1, u2, v2);
			return;
		}
		if (z1 > near && z2 <= near && z3 > near) {
			addFace1(x2, y2, z2, x1, y1, z1, x3, y3, z3, u2, v2, u1, v1, u3, v3);
			return;
		}
		if (z1 <= near && z2 > near && z3 > near) {
			addFace1(x1, y1, z1, x2, y2, z2, x3, y3, z3, u1, v1, u2, v2, u3, v3);
			return;
		}
	}

	private void addFace2(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double u1, double v1, double u2, double v2, double u3, double v3) {
		double dx, dy, dz, x4, y4, z4, x5, y5, z5;
		// point3 is behind the near plane, create and add two new faces
		dx = x3 - x1;
		dy = y3 - y1;
		dz = z1 - z3;
		x4 = x1 + (dx / dz) * (z1 - near);
		y4 = y1 + (dy / dz) * (z1 - near);
		z4 = near;

		dx = x3 - x2;
		dy = y3 - y2;
		dz = z2 - z3;
		x5 = x2 + (dx / dz) * (z2 - near);
		y5 = y2 + (dy / dz) * (z2 - near);
		z5 = near;

		//debugColor = 0x00FF00;
		addFace(x1, y1, z1, x2, y2, z2, x5, y5, z5, u1, v1, u2, v2, u3, v3);
		//debugColor = 0x0000FF;
		addFace(x1, y1, z1, x5, y5, z5, x4, y4, z4, u1, v1, u2, v2, u3, v3);
	}

	private void addFace1(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double u1, double v1, double u2, double v2, double u3, double v3) {
		double dx, dy, dz;
		// points 2 and 3 are behind the near plane, clip and add the new face
		dx = x2 - x1;
		dy = y2 - y1;
		dz = z1 - z2;
		x2 = x1 + (dx / dz) * (z1 - near);
		y2 = y1 + (dy / dz) * (z1 - near);
		z2 = near;

		dx = x3 - x1;
		dy = y3 - y1;
		dz = z1 - z3;
		x3 = x1 + (dx / dz) * (z1 - near);
		y3 = y1 + (dy / dz) * (z1 - near);
		z3 = near;

		addFace(x1, y1, z1, x2, y2, z2, x3, y3, z3, u1, v1, u2, v2, u3, v3);
	}

	private void addFace(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double u1, double v1, double u2, double v2, double u3, double v3) {
		//debugColors[faceCount] = debugColor;
		//debugColors[faceCount+1] = debugColor;
		//debugColors[faceCount+2] = debugColor;
		faces[faceCount * 3] = x1;
		faces[faceCount * 3 + 1] = y1;
		faces[faceCount * 3 + 2] = z1;
		faceUVs[faceCount * 2] = u1;
		faceUVs[faceCount * 2 + 1] = v1;
		faceCount++;

		faces[faceCount * 3] = x2;
		faces[faceCount * 3 + 1] = y2;
		faces[faceCount * 3 + 2] = z2;
		faceUVs[faceCount * 2] = u2;
		faceUVs[faceCount * 2 + 1] = v2;
		faceCount++;

		faces[faceCount * 3] = x3;
		faces[faceCount * 3 + 1] = y3;
		faces[faceCount * 3 + 2] = z3;
		faceUVs[faceCount * 2] = u3;
		faceUVs[faceCount * 2 + 1] = v3;
		faceCount++;
	}

	public void drawElementsub(int A, int B, byte[] facelist) {
		byte a, b;
		double x1, y1, z1, x2, y2, z2, x3, y3, z3, w1;
		double u1, v1, u2, v2, u3, v3;

		double x, y, z, w;
		double ox = width / 2;
		double oy = height / 2;

		u1 = v1 = u2 = v2 = u3 = v3 = 0;

		gc.setColor(color);

		applyMatrix(matrix);

		// clip //
		faceCount = 0;
		for (int i = 0; i < facelist.length; i += 3) {
			x1 = verts[(facelist[i] * 3)];
			y1 = verts[(facelist[i] * 3) + 1];
			z1 = verts[(facelist[i] * 3) + 2];

			x2 = verts[(facelist[i + 1] * 3)];
			y2 = verts[(facelist[i + 1] * 3) + 1];
			z2 = verts[(facelist[i + 1] * 3) + 2];

			x3 = verts[(facelist[i + 2] * 3)];
			y3 = verts[(facelist[i + 2] * 3) + 1];
			z3 = verts[(facelist[i + 2] * 3) + 2];

			if (z1 > near && z2 > near && z3 > near) {
				continue;
			} // drop face

			u1 = UVs[facelist[i] * 2];
			v1 = UVs[facelist[i] * 2 + 1];

			u2 = UVs[facelist[i + 1] * 2];
			v2 = UVs[facelist[i + 1] * 2 + 1];

			u3 = UVs[facelist[i + 2] * 2];
			v3 = UVs[facelist[i + 2] * 2 + 1];

			addFaces(x1, y1, z1, x2, y2, z2, x3, y3, z3, u1, v1, u2, v2, u3, v3);
		}

		// projection
		for (int i = 0; i < faceCount; i++) {
			x = faces[i * 3];
			y = faces[i * 3 + 1];
			z = faces[i * 3 + 2];
			w = 1;

			x1 = x * projm[0] + y * projm[4] + z * projm[8] + projm[12];
			y1 = x * projm[1] + y * projm[5] + z * projm[9] + projm[13];
			z1 = x * projm[2] + y * projm[6] + z * projm[10] + projm[14];
			w1 = x * projm[3] + y * projm[7] + z * projm[11] + projm[15];

			if (w1 != 1) {
				faces[i * 3] = (x1 / w1);
				faces[i * 3 + 1] = (y1 / w1);
				//faces[i*3+2] = (z1/w1);
			}
		}

		// draw elements
		for (int i = 0; i < faceCount; i += 3) {
			x1 = faces[i * 3];
			y1 = faces[i * 3 + 1];
			z1 = faces[i * 3 + 2];

			x2 = faces[i * 3 + 3];
			y2 = faces[i * 3 + 4];
			z2 = faces[i * 3 + 5];

			x3 = faces[i * 3 + 6];
			y3 = faces[i * 3 + 7];
			z3 = faces[i * 3 + 8];

			u1 = faceUVs[i * 2];
			v1 = faceUVs[i * 2 + 1];

			u2 = faceUVs[i * 2 + 2];
			v2 = faceUVs[i * 2 + 3];

			u3 = faceUVs[i * 2 + 4];
			v3 = faceUVs[i * 2 + 5];

			// scale and center on screen
			x1 = (x1 * ox) + ox;
			x2 = (x2 * ox) + ox;
			x3 = (x3 * ox) + ox;
			y1 = (y1 * oy) + oy;
			y2 = (y2 * oy) + oy;
			y3 = (y3 * oy) + oy;

			//gc.setColor(debugColors[i]);

			// draw
			if (boundTexture) {
				texture.setUVs((int) u1, (int) v1, (int) u2, (int) v2, (int) u3, (int) v3);
				texture.mapto((int) x1, (int) y1, (int) x2, (int) y2, (int) x3, (int) y3);
			}
			fillTriangle(x1, y1, z1, x2, y2, z2, x3, y3, z3);
		}
	}

	public void drawArrays(int a, int b, int c)  // called after clear -- background?
	{
		// The expected result.  No idea how to get there from here:
		//vertexPointerub(3, 0, [ -100, 20, -100, 0, -20, 0, 100, 20, -100, ]); // 3, 0, len 9
		//drawArrays(4, 0, 3);
		gc.setColor(color);
		gc.fillRect(0, 20, width, height);

	}

	public void bindTexture(int a, Texture b) {
		texture = b;
		boundTexture = true;
	}

	public void texCoordPointerub(int a, int b, byte[] uvs) {
		for (int i = 0; i < uvs.length; i++) {
			UVs[i] = uvs[i];
		}
	}

	public void enableClientState(int flags) {
	}

	public void disableClientState(int flags) {
	}

	public void enable(int feature) {
	}

	public void disable(int feature) {
	}

	public void blit(Graphics g, int x, int y, int w, int h) // 0, 0, 95, 65
	{
		g.drawImage(platformImage, x, y, Graphics.LEFT | Graphics.TOP);
	}

	private void identity(double[] m) {
		m[0]  = 1; m[1]  = 0; m[2]  = 0; m[3]  = 0;
		m[4]  = 0; m[5]  = 1; m[6]  = 0; m[7]  = 0;
		m[8]  = 0; m[9]  = 0; m[10] = 1; m[11] = 0;
		m[12] = 0; m[13] = 0; m[14] = 0; m[15] = 1;
	}

	private void projection(double[] m, double w, double h, double fov, double near, double far) {
		fov = fov * 0.0174533;
		double sx = 1 / Math.tan(fov / 2);
		double sy = -sx * (w / h);
		double sz = (far / (far - near)); // 1 > z > -1
		double d = -1.0;
		double e = 1;

		m[0] = sx;
		m[1] = 0;
		m[2] = 0;
		m[3] = 0;
		m[4] = 0;
		m[5] = sy;
		m[6] = 0;
		m[7] = 0;
		m[8] = 0;
		m[9] = 0;
		m[10] = sz;
		m[11] = d;
		m[12] = 0;
		m[13] = 0;
		m[14] = e;
		m[15] = 0;
	}

	private void clone(double[] m1, double[] m2) {
		System.arraycopy(m2, 0, m1, 0, 16);
	}

	private void matmul(double[] m1, double[] m2) {
		tempm[0] = m1[0] * m2[0] + m1[1] * m2[4] + m1[2] * m2[8] + m1[3] * m2[12];
		tempm[1] = m1[0] * m2[1] + m1[1] * m2[5] + m1[2] * m2[9] + m1[3] * m2[13];
		tempm[2] = m1[0] * m2[2] + m1[1] * m2[6] + m1[2] * m2[10] + m1[3] * m2[14];
		tempm[3] = m1[0] * m2[3] + m1[1] * m2[7] + m1[2] * m2[11] + m1[3] * m2[15];

		tempm[4] = m1[4] * m2[0] + m1[5] * m2[4] + m1[6] * m2[8] + m1[7] * m2[12];
		tempm[5] = m1[4] * m2[1] + m1[5] * m2[5] + m1[6] * m2[9] + m1[7] * m2[13];
		tempm[6] = m1[4] * m2[2] + m1[5] * m2[6] + m1[6] * m2[10] + m1[7] * m2[14];
		tempm[7] = m1[4] * m2[3] + m1[5] * m2[7] + m1[6] * m2[11] + m1[7] * m2[15];

		tempm[8] = m1[8] * m2[0] + m1[9] * m2[4] + m1[10] * m2[8] + m1[11] * m2[12];
		tempm[9] = m1[8] * m2[1] + m1[9] * m2[5] + m1[10] * m2[9] + m1[11] * m2[13];
		tempm[10] = m1[8] * m2[2] + m1[9] * m2[6] + m1[10] * m2[10] + m1[11] * m2[14];
		tempm[11] = m1[8] * m2[3] + m1[9] * m2[7] + m1[10] * m2[11] + m1[11] * m2[15];

		tempm[12] = m1[12] * m2[0] + m1[13] * m2[4] + m1[14] * m2[8] + m1[15] * m2[12];
		tempm[13] = m1[12] * m2[1] + m1[13] * m2[5] + m1[14] * m2[9] + m1[15] * m2[13];
		tempm[14] = m1[12] * m2[2] + m1[13] * m2[6] + m1[14] * m2[10] + m1[15] * m2[14];
		tempm[15] = m1[12] * m2[3] + m1[13] * m2[7] + m1[14] * m2[11] + m1[15] * m2[15];

		clone(m1, tempm);
	}

	private void invert(double[] m) {
		double a0 = m[0], a1 = m[1], a2 = m[2], a3 = m[3];
		double a4 = m[4], a5 = m[5], a6 = m[6], a7 = m[7];
		double a8 = m[8], a9 = m[9], a10 = m[10], a11 = m[11];
		double a12 = m[12], a13 = m[13], a14 = m[14], a15 = m[15];

		double b0 = a0 * a5 - a1 * a4, b1 = a0 * a6 - a2 * a4;
		double b2 = a0 * a7 - a3 * a4, b3 = a1 * a6 - a2 * a5;
		double b4 = a1 * a7 - a3 * a5, b5 = a2 * a7 - a3 * a6;
		double b6 = a8 * a13 - a9 * a12, b7 = a8 * a14 - a10 * a12;
		double b8 = a8 * a15 - a11 * a12, b9 = a9 * a14 - a10 * a13;
		double b10 = a9 * a15 - a11 * a13, b11 = a10 * a15 - a11 * a14;

		double det = b0 * b11 - b1 * b10 + b2 * b9 + b3 * b8 - b4 * b7 + b5 * b6;

		if (det == 0) {
			return;
		} // should be an error

		det = 1 / det;

		m[0] = (a5 * b11 - a6 * b10 + a7 * b9) * det;
		m[1] = (a2 * b10 - a1 * b11 - a3 * b9) * det;
		m[2] = (a13 * b5 - a14 * b4 + a15 * b3) * det;
		m[3] = (a10 * b4 - a9 * b5 - a11 * b3) * det;
		m[4] = (a6 * b8 - a4 * b11 - a7 * b7) * det;
		m[5] = (a0 * b11 - a2 * b8 + a3 * b7) * det;
		m[6] = (a14 * b2 - a12 * b5 - a15 * b1) * det;
		m[7] = (a8 * b5 - a10 * b2 + a11 * b1) * det;
		m[8] = (a4 * b10 - a5 * b8 + a7 * b6) * det;
		m[9] = (a1 * b8 - a0 * b10 - a3 * b6) * det;
		m[10] = (a12 * b4 - a13 * b2 + a15 * b0) * det;
		m[11] = (a9 * b2 - a8 * b4 - a11 * b0) * det;
		m[12] = (a5 * b7 - a4 * b9 - a6 * b6) * det;
		m[13] = (a0 * b9 - a1 * b7 + a2 * b6) * det;
		m[14] = (a13 * b1 - a12 * b3 - a14 * b0) * det;
		m[15] = (a8 * b3 - a9 * b1 + a10 * b0) * det;
	}

	private void applyMatrix(double[] m) {
		double x, y, z, w;
		for (int i = 0; i < vertCount; i += 3) {
			x = verts[i];
			y = verts[i + 1];
			z = verts[i + 2];
			w = 1;
			verts[i] = x * m[0] + y * m[4] + z * m[8] + w * m[12];
			verts[i + 1] = x * m[1] + y * m[5] + z * m[9] + w * m[13];
			verts[i + 2] = x * m[2] + y * m[6] + z * m[10] + w * m[14];
		}
	}

	private void fillTriangle(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3) {
		double a, b, c;
		double depth;
		double px, py;

		// find rect, clip to screen
		int maxX = (int) Math.min(Math.max(x1, Math.max(x2, x3)), width - 1);
		int maxY = (int) Math.min(Math.max(y1, Math.max(y2, y3)), height - 1);
		int minX = (int) Math.max(Math.min(x1, Math.min(x2, x3)), 0);
		int minY = (int) Math.max(Math.min(y1, Math.min(y2, y3)), 0);

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				px = (double) x;
				py = (double) y;

				// point in triangle
				//cross product to find area*2 of triangles around point
				a = (x3 - x2) * -(py - y2) - -(y3 - y2) * (px - x2); // p1
				b = (x1 - x3) * -(py - y3) - -(y1 - y3) * (px - x3); // p2
				c = (x2 - x1) * -(py - y1) - -(y2 - y1) * (px - x1); // p3

				//if(!(a<0 || b<0 || c<0))
				if (a >= 0 && b >= 0 && c >= 0) {
					//fragment depth
					depth = (a * z1 + b * z2 + c * z3) / (a + b + c);

					// plot
					if (depth >= zbuffer[x + (y * width)]) {
						zbuffer[x + (y * width)] = depth;
						if (boundTexture) {
							gc.setColorAlpha(texture.map(x, y));
						}
						gc.drawLine(x, y, x, y);
					}
				}
			}
		}
	}

}
