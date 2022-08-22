/*
 *  Copyright 2020 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.mascotcapsule.micro3d.v3;

import static android.opengl.GLES20.*;
import static com.mascotcapsule.micro3d.v3.Graphics3D.*;
import static com.mascotcapsule.micro3d.v3.Util3D.TAG;
import static com.mascotcapsule.micro3d.v3.Utils.TO_FLOAT;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.mascotcapsule.micro3d.v3.RenderNode.FigureNode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.lcdui.Graphics;

class Render {
	private final static FloatBuffer BG_VBO = ByteBuffer.allocateDirect(8 * 2 * 4)
			.order(ByteOrder.nativeOrder()).asFloatBuffer()
			.put(new float[]{
					-1.0f, -1.0f, 0.0f, 0.0f,
					1.0f, -1.0f, 1.0f, 0.0f,
					-1.0f, 1.0f, 0.0f, 1.0f,
					1.0f, 1.0f, 1.0f, 1.0f
			});
	private static final int[] EMPTY_ARRAY = {};
	private static Render instance;
	private EGLDisplay eglDisplay;
	private EGLSurface eglWindowSurface;
	private EGLConfig eglConfig;
	private EGLContext eglContext;
	private final int[] bgTextureId = new int[]{-1};
	private final float[] MVP_TMP = new float[16];

	private Graphics graphics;
	private Bitmap mBitmapBuffer;
	private int width, height;
	private final Rect gClip = new Rect();
	private final Rect clip = new Rect();
	private final boolean skipSprites = Boolean.getBoolean("micro3d.v3.skipSprites");
	private boolean backCopied;
	private final LinkedList<RenderNode> stack = new LinkedList<>();
	private int flushStep;
	private Texture[] textures;
	private final boolean postCopy2D = !Boolean.getBoolean("micro3d.v3.render.no-mix2D3D");
	private final boolean preCopy2D = !Boolean.getBoolean("micro3d.v3.render.background.ignore");
	private int textureIdx;

	/**
	 * Utility method for debugging OpenGL calls.
	 * <p>
	 * If the operation is not successful, the check throws an error.
	 *
	 * @param glOperation - Name of the OpenGL call to check.
	 */
	static void checkGlError(String glOperation) {
		int error = glGetError();
		if (error != GL_NO_ERROR) {
			String s = GLU.gluErrorString(error);
			Log.e(TAG, glOperation + ": glError " + s);
			throw new RuntimeException(glOperation + ": glError " + s);
		}
	}

	synchronized static Render getRender() {
		if (instance == null) {
			instance = new Render();
		}
		return instance;
	}

	private void init() {
		EGL10 egl = (EGL10) EGLContext.getEGL();
		this.eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

		int[] version = new int[2];
		egl.eglInitialize(eglDisplay, version);

		int EGL_OPENGL_ES2_BIT = 0x0004;
		int[] num_config = new int[1];
		int[] attribs = {
				EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
				EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
				EGL10.EGL_RED_SIZE, 8,
				EGL10.EGL_GREEN_SIZE, 8,
				EGL10.EGL_BLUE_SIZE, 8,
				EGL10.EGL_ALPHA_SIZE, 8,
				EGL10.EGL_DEPTH_SIZE, 16,
				EGL10.EGL_STENCIL_SIZE, EGL10.EGL_DONT_CARE,
				EGL10.EGL_NONE
		};
		EGLConfig[] eglConfigs = new EGLConfig[1];
		egl.eglChooseConfig(eglDisplay, attribs, eglConfigs, 1, num_config);
		this.eglConfig = eglConfigs[0];

		int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
		int[] attrib_list = {
				EGL_CONTEXT_CLIENT_VERSION, 2,
				EGL10.EGL_NONE
		};
		this.eglContext = egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
	}

	synchronized void bind(Graphics graphics) {
		this.graphics = graphics;
		Canvas canvas = graphics.getCanvas();
		int width = canvas.getWidth();
		int height = canvas.getHeight();
		if (eglContext == null) init();
		mBitmapBuffer = graphics.getBitmap();
		EGL10 egl = (EGL10) EGLContext.getEGL();
		if (this.width != width || this.height != height) {

			if (this.eglWindowSurface != null) {
				releaseEglContext();
				egl.eglDestroySurface(this.eglDisplay, this.eglWindowSurface);
			}

			int[] surface_attribs = {
					EGL10.EGL_WIDTH, width,
					EGL10.EGL_HEIGHT, height,
					EGL10.EGL_NONE};
			this.eglWindowSurface = egl.eglCreatePbufferSurface(eglDisplay, eglConfig, surface_attribs);
			egl.eglMakeCurrent(eglDisplay, eglWindowSurface, eglWindowSurface, eglContext);

			glViewport(0, 0, width, height);
			Program.create();
			this.width = width;
			this.height = height;
			glClearColor(0, 0, 0, 1);
			glClear(GL_COLOR_BUFFER_BIT);
		}
		egl.eglMakeCurrent(eglDisplay, eglWindowSurface, eglWindowSurface, eglContext);
		Rect clip = this.clip;
		canvas.getClipBounds(clip);
		int l = clip.left;
		int t = clip.top;
		int r = clip.right;
		int b = clip.bottom;
		gClip.set(l, t, r, b);
		if (l == 0 && t == 0 && r == this.width && b == this.height) {
			glDisable(GL_SCISSOR_TEST);
		} else {
			glEnable(GL_SCISSOR_TEST);
			glScissor(l, t, r - l, b - t);
		}
		glClear(GL_DEPTH_BUFFER_BIT);
		backCopied = false;
		egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
	}

	private static void applyBlending(int blendMode) {
		switch (blendMode) {
			case Model.Polygon.BLEND_HALF:
				glEnable(GL_BLEND);
				glBlendColor(0.5f, 0.5f, 0.5f, 1.0f);
				glBlendEquation(GL_FUNC_ADD);
				glBlendFunc(GL_CONSTANT_COLOR, GL_CONSTANT_COLOR);
				break;
			case Model.Polygon.BLEND_ADD:
				glEnable(GL_BLEND);
				glBlendEquation(GL_FUNC_ADD);
				glBlendFunc(GL_ONE, GL_ONE);
				break;
			case Model.Polygon.BLEND_SUB:
				glEnable(GL_BLEND);
				glBlendEquation(GL_FUNC_REVERSE_SUBTRACT);
				glBlendFuncSeparate(GL_ONE, GL_ONE, GL_ZERO, GL_ONE);
				break;
			default:
				glDisable(GL_BLEND);
		}
	}

	private void copy2d(boolean preProcess) {
		if (!glIsTexture(bgTextureId[0])) {
			glGenTextures(1, bgTextureId, 0);
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D, bgTextureId[0]);
			boolean filter = Boolean.getBoolean("micro3d.v3.background.filter");
			glTexParameteri(GLES20.GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter ? GL_LINEAR : GL_NEAREST);
			glTexParameteri(GLES20.GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter ? GL_LINEAR : GL_NEAREST);
			glTexParameteri(GLES20.GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GLES20.GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		} else {
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D, bgTextureId[0]);
		}
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmapBuffer, 0);
		checkGlError("texImage2D");

		final Program.Simple program = Program.simple;
		program.use();

		BG_VBO.rewind();
		glVertexAttribPointer(program.aPosition, 2, GL_FLOAT, false, 4 * 4, BG_VBO);
		glEnableVertexAttribArray(program.aPosition);

		// координаты текстур
		BG_VBO.position(2);
		glVertexAttribPointer(program.aTexture, 2, GL_FLOAT, false, 4 * 4, BG_VBO);
		glEnableVertexAttribArray(program.aTexture);

		// юнит текстуры
		glUniform1i(program.uTextureUnit, 1);

		if (preProcess) {
			glDisable(GL_BLEND);
		} else {
			glEnable(GL_BLEND);
			glBlendEquation(GL_FUNC_ADD);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		}
		glDisable(GL_DEPTH_TEST);
		glDepthMask(false);
		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		glDisableVertexAttribArray(program.aPosition);
		glDisableVertexAttribArray(program.aTexture);
		checkGlError("copy2d");
		if (preProcess) {
			if (postCopy2D) {
				mBitmapBuffer.setHasAlpha(true);
				graphics.getCanvas().drawColor(0, PorterDuff.Mode.SRC);
			}
			backCopied = true;
		} else {
			mBitmapBuffer.setHasAlpha(false);
		}
	}

	@Override
	protected synchronized void finalize() throws Throwable {
		try {
			// Destroy EGL
			EGL10 egl = (EGL10) EGLContext.getEGL();
			egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
			if (eglWindowSurface != null)
				egl.eglDestroySurface(eglDisplay, eglWindowSurface);
			egl.eglDestroyContext(eglDisplay, eglContext);
			egl.eglTerminate(eglDisplay);
		} finally {
			super.finalize();
		}
	}

	void renderFigure(Model model, int x, int y, FigureLayout layout, Texture[] textures,
					  Effect3D effect, FloatBuffer vertices, FloatBuffer normals) {
		if (!effect.isTransparency && flushStep == 2) return;

		if (!model.hasPolyT && !model.hasPolyC)
			return;

		glEnable(GL_DEPTH_TEST);
		glDepthMask(flushStep == 1);
		float[] mvm = getMvMatrix(layout);
		float[] pm = getProjectionMatrix(layout, x, y);
		float[] mvp = MVP_TMP;
		Matrix.multiplyMM(mvp, 0, pm, 0, mvm, 0);
		int[] bufHandles = new int[3];
		glGenBuffers(3, bufHandles, 0);
		vertices.rewind();
		try {
			glBindBuffer(GL_ARRAY_BUFFER, bufHandles[0]);
			glBufferData(GL_ARRAY_BUFFER, vertices.capacity() * 4, vertices, GL_STREAM_DRAW);
			ByteBuffer tcBuf = model.texCoordArray;
			tcBuf.rewind();
			glBindBuffer(GL_ARRAY_BUFFER, bufHandles[1]);
			glBufferData(GL_ARRAY_BUFFER, tcBuf.capacity(), tcBuf, GL_STREAM_DRAW);

			if (normals != null) {
				normals.rewind();
				glBindBuffer(GL_ARRAY_BUFFER, bufHandles[2]);
				glBufferData(GL_ARRAY_BUFFER, normals.capacity() * 4, normals, GL_STREAM_DRAW);
			}
			if (model.hasPolyT) {
				final Program.Tex program = Program.tex;
				program.use();
				program.setToonShading(effect);

				glBindBuffer(GL_ARRAY_BUFFER, bufHandles[0]);
				glEnableVertexAttribArray(program.aPosition);
				glVertexAttribPointer(program.aPosition, 3, GL_FLOAT, false, 3 * 4, 0);

				glBindBuffer(GL_ARRAY_BUFFER, bufHandles[1]);
				glEnableVertexAttribArray(program.aColorData);
				glVertexAttribPointer(program.aColorData, 2, GL_UNSIGNED_BYTE, false, 5, 0);
				glEnableVertexAttribArray(program.aMaterial);
				glVertexAttribPointer(program.aMaterial, 3, GL_UNSIGNED_BYTE, false, 5, 2);

				if (normals != null) {
					glBindBuffer(GL_ARRAY_BUFFER, bufHandles[2]);
					glEnableVertexAttribArray(program.aNormal);
					glVertexAttribPointer(program.aNormal, 3, GL_FLOAT, false, 3 * 4, 0);
				} else {
					glDisableVertexAttribArray(program.aNormal);
				}

				program.bindMatrices(mvp, mvm);
				program.setLight(effect.isLighting ? effect.light : null);
				if (effect.isLighting && effect.mTexture != null) {
					glActiveTexture(GL_TEXTURE2);
					glBindTexture(GL_TEXTURE_2D, effect.mTexture.getId());
					glUniform1i(program.uSphereUnit, 2);
					glUniform2f(program.uSphereSize, 64.0f / effect.mTexture.width, 64.0f / effect.mTexture.height);
				} else {
					glUniform2f(program.uSphereSize, -1, -1);
				}
				// Draw triangles
				renderModel(model, textures, effect);
				glDisableVertexAttribArray(program.aPosition);
				glDisableVertexAttribArray(program.aColorData);
				glDisableVertexAttribArray(program.aMaterial);
				glDisableVertexAttribArray(program.aNormal);
				glBindBuffer(GL_ARRAY_BUFFER, 0);
			}

			if (model.hasPolyC) {
				final Program.Color program = Program.color;
				program.use();

				glBindBuffer(GL_ARRAY_BUFFER, bufHandles[0]);
				glEnableVertexAttribArray(program.aPosition);
				glVertexAttribPointer(program.aPosition, 3, GL_FLOAT, false, 3 * 4, 0);

				glBindBuffer(GL_ARRAY_BUFFER, bufHandles[1]);
				glVertexAttribPointer(program.aColorData, 3, GL_UNSIGNED_BYTE, true, 5, 0);
				glEnableVertexAttribArray(program.aColorData);
				glEnableVertexAttribArray(program.aMaterial);
				glVertexAttribPointer(program.aMaterial, 2, GL_UNSIGNED_BYTE, false, 5, 3);

				if (normals != null) {
					glBindBuffer(GL_ARRAY_BUFFER, bufHandles[2]);
					glVertexAttribPointer(program.aNormal, 3, GL_FLOAT, false, 3 * 4, 0);
					glEnableVertexAttribArray(program.aNormal);
				} else {
					glDisableVertexAttribArray(program.aNormal);
				}
				program.bindMatrices(mvp, mvm);
				program.setLight(effect.isLighting ? effect.light : null);
				if (effect.isLighting && effect.mTexture != null) {
					glActiveTexture(GL_TEXTURE2);
					glBindTexture(GL_TEXTURE_2D, effect.mTexture.getId());
					glUniform1i(program.uSphereUnit, 2);
					glUniform2f(program.uSphereSize, 64.0f / effect.mTexture.width, 64.0f / effect.mTexture.height);
				} else {
					glUniform2f(program.uSphereSize, -1, -1);
				}
				program.setToonShading(effect);
				renderModel(model, effect.isTransparency);
				glDisableVertexAttribArray(program.aPosition);
				glDisableVertexAttribArray(program.aColorData);
				glDisableVertexAttribArray(program.aMaterial);
				glDisableVertexAttribArray(program.aNormal);
			}
		} finally {
			glBindBuffer(GL_ARRAY_BUFFER, 0);
			glDeleteBuffers(3, bufHandles, 0);
		}
	}

	private float[] getProjectionMatrix(FigureLayout layout, int x, int y) {
		float[] pm = new float[16];
		switch (layout.settingIndex) {
			case COMMAND_PARALLEL_SCALE:
				Utils.parallelScale(pm, x, y, layout, width, height);
				break;
			case COMMAND_PARALLEL_SIZE:
				Utils.parallelWH(pm, x, y, layout, width, height);
				break;
			case COMMAND_PERSPECTIVE_FOV:
				Utils.perspectiveFov(pm, x, y, layout, width, height);
				break;
			case COMMAND_PERSPECTIVE_WH:
				Utils.perspectiveWH(pm, x, y, layout, width, height);
				break;
		}
		return pm;
	}

	private float[] getMvMatrix(FigureLayout layout) {
		AffineTrans a = layout.affine;
		float[] m = new float[16];
		m[0] = a.m00 * TO_FLOAT; m[4] = a.m01 * TO_FLOAT; m[ 8] = a.m02 * TO_FLOAT; m[12] = a.m03;
		m[1] = a.m10 * TO_FLOAT; m[5] = a.m11 * TO_FLOAT; m[ 9] = a.m12 * TO_FLOAT; m[13] = a.m13;
		m[2] = a.m20 * TO_FLOAT; m[6] = a.m21 * TO_FLOAT; m[10] = a.m22 * TO_FLOAT; m[14] = a.m23;
		m[3] =             0.0F; m[7] =             0.0F; m[11] =             0.0F; m[15] =  1.0F;
		return m;
	}

	private void renderModel(Model model, Texture[] textures, Effect3D effect) {
		if (textures == null || textures.length == 0) return;
		Program.Tex program = Program.tex;
		program.enableTexUnit();
		int[][][] meshes = model.subMeshesLengthsT;
		int length = meshes.length;
		int blendMode = 0;
		int pos = 0;
		if (flushStep == 1) {
			if (effect.isTransparency) length = 1;
			glDisable(GL_BLEND);
		} else {
			int[][] mesh = meshes[blendMode++];
			int cnt = 0;
			for (int[] lens : mesh) {
				for (int len : lens) {
					cnt += len;
				}
			}
			pos += cnt;
		}
		while (blendMode < length) {
			int[][] texMesh = meshes[blendMode];
			if (flushStep == 2) {
				applyBlending(blendMode << 1);
			}
			for (int face = 0; face < texMesh.length; face++) {
				int[] lens = texMesh[face];
				Texture tex = face >= textures.length ? null : textures[face];
				program.setTex(tex);
				int cnt = lens[0];
				if (cnt > 0) {
					glEnable(GL_CULL_FACE);
					glDrawArrays(GL_TRIANGLES, pos, cnt);
					pos += cnt;
				}
				cnt = lens[1];
				if (cnt > 0) {
					glDisable(GL_CULL_FACE);
					glDrawArrays(GL_TRIANGLES, pos, cnt);
					pos += cnt;
				}
			}
			blendMode++;
		}
		checkGlError("glDrawArrays");
	}

	private void renderModel(Model model, boolean enableBlending) {
		int[][] meshes = model.subMeshesLengthsC;
		int length = meshes.length;
		int pos = model.numVerticesPolyT;
		int blendMode = 0;
		if (flushStep == 1) {
			if (enableBlending) length = 1;
			glDisable(GL_BLEND);
		} else {
			int[] mesh = meshes[blendMode++];
			int cnt = 0;
			for (int len : mesh) {
				cnt += len;
			}
			pos += cnt;
		}
		while (blendMode < length) {
			int[] mesh = meshes[blendMode];
			if (flushStep == 2) {
				applyBlending(blendMode << 1);
			}
			int cnt = mesh[0];
			if (cnt > 0) {
				glEnable(GL_CULL_FACE);
				glDrawArrays(GL_TRIANGLES, pos, cnt);
				pos += cnt;
			}
			cnt = mesh[1];
			if (cnt > 0) {
				glDisable(GL_CULL_FACE);
				glDrawArrays(GL_TRIANGLES, pos, cnt);
				pos += cnt;
			}
			blendMode++;
		}
		checkGlError("glDrawArrays");
	}

	synchronized void release() {
		bindEglContext();
		stack.clear();
		if (postCopy2D) {
			copy2d(false);
		}
		Rect clip = this.gClip;
		Utils.glReadPixels(clip.left, clip.top, clip.width(), clip.height(), mBitmapBuffer);
		releaseEglContext();
	}

	synchronized void flush() {
		if (stack.isEmpty()) {
			return;
		}
		bindEglContext();
		try {
			if (!backCopied && preCopy2D) copy2d(true);
			flushStep = 1;
			for (RenderNode r : stack) {
				r.run();
			}
			flushStep = 2;
			for (RenderNode r : stack) {
				r.run();
				r.recycle();
			}
			glDisable(GL_BLEND);
			glDepthMask(true);
			glClear(GL_DEPTH_BUFFER_BIT);
			glFlush();
		} finally {
			stack.clear();
			releaseEglContext();
		}
	}

	private void renderPrimitives(Texture texture, int command, int numPrimitives,
								  int[] vertices, int[] normals, int[] texCoords,
								  int[] colors, Effect3D effect,
								  FigureLayout layout) {
		float[] pm = getProjectionMatrix(layout, 0, 0);
		float[] mvm = getMvMatrix(layout);
		int blend = command & Graphics3D.PATTR_BLEND_SUB;
		boolean blendEnabled = (effect.isTransparency || (command & Graphics3D.ENV_ATTR_SEMI_TRANSPARENT) != 0) && blend != 0;
		if (blendEnabled) {
			if (flushStep == 1) {
				return;
			}
		} else if (flushStep == 2) {
			return;
		}
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LESS);
		glDepthMask(flushStep == 1);
		switch ((command & 0x7000000)) {
			case PRIMITVE_POINTS: {
				int vcLen = numPrimitives * 3;
				FloatBuffer vcBuf = ByteBuffer.allocateDirect(numPrimitives * 3 * 4)
						.order(ByteOrder.nativeOrder()).asFloatBuffer();
				for (int i = 0; i < vcLen; i++) {
					vcBuf.put(vertices[i]);
				}
				Program.Color program = Program.color;
				program.use();
				program.setLight(null);
				float[] mvp = MVP_TMP;
				Matrix.multiplyMM(mvp, 0, pm, 0, mvm, 0);
				program.bindMatrices(mvp, mvm);

				if ((command & PDATA_COLOR_PER_COMMAND) != 0) {
					program.setColor(colors[0]);
				} else {
					ByteBuffer colorBuf = ByteBuffer.allocateDirect(numPrimitives * 3 * 4)
							.order(ByteOrder.nativeOrder());
					for (int i = 0; i < numPrimitives; i++) {
						int color = colors[i];
						colorBuf.put((byte) (color >> 16 & 0xFF));
						colorBuf.put((byte) (color >> 8 & 0xFF));
						colorBuf.put((byte) (color & 0xFF));
					}
					colorBuf.rewind();
					glVertexAttribPointer(program.aColorData, 3, GL_UNSIGNED_BYTE, true, 3, colorBuf);
					glEnableVertexAttribArray(program.aColorData);
				}
				vcBuf.rewind();
				glVertexAttribPointer(program.aPosition, 3, GL_FLOAT, false, 3 * 4, vcBuf);
				glEnableVertexAttribArray(program.aPosition);

				applyBlending(blendEnabled ? blend >> 4 : 0);
				glDrawArrays(GL_POINTS, 0, numPrimitives);
				glDisableVertexAttribArray(program.aPosition);
				glDisableVertexAttribArray(program.aColorData);
				checkGlError("glDrawArrays");
				break;
			}
			case PRIMITVE_LINES: {
				int vcLen = numPrimitives * 3 * 2;
				FloatBuffer vcBuf = ByteBuffer.allocateDirect(numPrimitives * 2 * 3 * 4)
						.order(ByteOrder.nativeOrder()).asFloatBuffer();
				for (int i = 0; i < vcLen; i++) {
					vcBuf.put(vertices[i]);
				}
				Program.Color program = Program.color;
				program.use();
				program.setLight(null);
				float[] mvp = MVP_TMP;
				Matrix.multiplyMM(mvp, 0, pm, 0, mvm, 0);
				program.bindMatrices(mvp, mvm);

				if ((command & PDATA_COLOR_PER_COMMAND) != 0) {
					program.setColor(colors[0]);
				} else {
					ByteBuffer colorBuf = ByteBuffer.allocateDirect(numPrimitives * 2 * 3 * 4)
							.order(ByteOrder.nativeOrder());
					for (int i = 0; i < numPrimitives; i++) {
						int color = colors[i];
						byte r = (byte) (color >> 16 & 0xFF);
						byte g = (byte) (color >> 8 & 0xFF);
						byte b = (byte) (color & 0xFF);
						colorBuf.put(r).put(g).put(b);
						colorBuf.put(r).put(g).put(b);
					}
					colorBuf.rewind();
					glVertexAttribPointer(program.aColorData, 3, GL_UNSIGNED_BYTE, true, 3, colorBuf);
					glEnableVertexAttribArray(program.aColorData);
				}
				vcBuf.rewind();
				glVertexAttribPointer(program.aPosition, 3, GL_FLOAT, false, 3 * 4, vcBuf);
				glEnableVertexAttribArray(program.aPosition);

				applyBlending(blendEnabled ? blend >> 4 : 0);
				glDrawArrays(GL_LINES, 0, numPrimitives * 2);
				glDisableVertexAttribArray(program.aPosition);
				glDisableVertexAttribArray(program.aColorData);
				checkGlError("glDrawArrays");
				break;
			}
			case PRIMITVE_TRIANGLES: {
				glDisable(GL_CULL_FACE);
				int vcLen = numPrimitives * 3 * 3;
				FloatBuffer vcBuf = ByteBuffer.allocateDirect(vcLen * 4)
						.order(ByteOrder.nativeOrder()).asFloatBuffer();
				for (int i = 0; i < vcLen; i++) {
					vcBuf.put(vertices[i]);
				}
				vcBuf.rewind();
				FloatBuffer ncBuf;
				switch (command & PDATA_NORMAL_PER_VERTEX) {
					case PDATA_NORMAL_PER_FACE:
						ncBuf = ByteBuffer.allocateDirect(vcLen * 4)
								.order(ByteOrder.nativeOrder()).asFloatBuffer();
						for (int i = 0, normLen = numPrimitives * 3; i < normLen; ) {
							float x = normals[i++];
							float y = normals[i++];
							float z = normals[i++];
							ncBuf.put(x).put(y).put(z);
							ncBuf.put(x).put(y).put(z);
							ncBuf.put(x).put(y).put(z);
						}
						break;
					case PDATA_NORMAL_PER_VERTEX:
						ncBuf = ByteBuffer.allocateDirect(vcLen * 4)
								.order(ByteOrder.nativeOrder()).asFloatBuffer();
						for (int i = 0; i < vcLen; i++) {
							ncBuf.put(normals[i]);
						}
						break;
					default:
						ncBuf = null;
				}
				if ((command & PDATA_COLOR_PER_COMMAND) != 0) {
					float[] mvp = MVP_TMP;
					Matrix.multiplyMM(mvp, 0, pm, 0, mvm, 0);
					renderMesh(mvp, mvm, command, blendEnabled, effect, vcBuf, ncBuf, colors[0]);
				} else if ((command & PDATA_TEXURE_COORD) != 0) {
					int tcLen = numPrimitives * 3 * 2;
					ByteBuffer tcBuf = ByteBuffer.allocateDirect(tcLen)
							.order(ByteOrder.nativeOrder());
					for (int i = 0; i < tcLen; i++) {
						tcBuf.put((byte) texCoords[i]);
					}
					tcBuf.rewind();
					float[] mvp = MVP_TMP;
					Matrix.multiplyMM(mvp, 0, pm, 0, mvm, 0);
					renderMesh(texture, mvp, mvm, command, blendEnabled, effect, vcBuf, ncBuf, tcBuf);
				} else if ((command & PDATA_COLOR_PER_FACE) != 0) {
					ByteBuffer colorBuf = ByteBuffer.allocateDirect(vcLen).order(ByteOrder.nativeOrder());
					for (int i = 0; i < numPrimitives; i++) {
						int color = colors[i];
						byte r = (byte) (color >> 16 & 0xFF);
						byte g = (byte) (color >> 8 & 0xFF);
						byte b = (byte) (color & 0xFF);
						colorBuf.put(r).put(g).put(b);
						colorBuf.put(r).put(g).put(b);
						colorBuf.put(r).put(g).put(b);
					}
					colorBuf.rewind();
					float[] mvp = MVP_TMP;
					Matrix.multiplyMM(mvp, 0, pm, 0, mvm, 0);
					renderMesh(mvp, mvm, command, blendEnabled, effect, vcBuf, ncBuf, colorBuf);
				}
				break;
			}
			case PRIMITVE_QUADS: {
				FloatBuffer vcBuf = ByteBuffer.allocateDirect(numPrimitives * 6 * 3 * 4)
						.order(ByteOrder.nativeOrder()).asFloatBuffer();
				for (int i = 0; i < numPrimitives; i++) {
					int offset = i * 4 * 3;
					int pos = offset;
					vcBuf.put(vertices[pos++]).put(vertices[pos++]).put(vertices[pos++]); // A
					vcBuf.put(vertices[pos++]).put(vertices[pos++]).put(vertices[pos++]); // B
					vcBuf.put(vertices[pos++]).put(vertices[pos++]).put(vertices[pos++]); // C
					vcBuf.put(vertices[pos++]).put(vertices[pos++]).put(vertices[pos]);   // D
					pos = offset;
					vcBuf.put(vertices[pos++]).put(vertices[pos++]).put(vertices[pos]);   // A
					pos = offset + 2 * 3;
					vcBuf.put(vertices[pos++]).put(vertices[pos++]).put(vertices[pos]);   // C
				}
				vcBuf.rewind();
				FloatBuffer ncBuf;
				switch (command & PDATA_NORMAL_PER_VERTEX) {
					case PDATA_NORMAL_PER_FACE:
						ncBuf = ByteBuffer.allocateDirect(numPrimitives * 6 * 3 * 4)
								.order(ByteOrder.nativeOrder()).asFloatBuffer();
						for (int i = 0, ncLen = numPrimitives * 3; i < ncLen; ) {
							float x = normals[i++];
							float y = normals[i++];
							float z = normals[i++];
							ncBuf.put(x).put(y).put(z);
							ncBuf.put(x).put(y).put(z);
							ncBuf.put(x).put(y).put(z);
							ncBuf.put(x).put(y).put(z);
							ncBuf.put(x).put(y).put(z);
							ncBuf.put(x).put(y).put(z);
						}
						break;
					case PDATA_NORMAL_PER_VERTEX:
						ncBuf = ByteBuffer.allocateDirect(numPrimitives * 6 * 3 * 4)
								.order(ByteOrder.nativeOrder()).asFloatBuffer();
						for (int i = 0; i < numPrimitives; i++) {
							int offset = i * 4 * 3;
							int pos = offset;
							ncBuf.put(normals[pos++]).put(normals[pos++]).put(normals[pos++]); // A
							ncBuf.put(normals[pos++]).put(normals[pos++]).put(normals[pos++]); // B
							ncBuf.put(normals[pos++]).put(normals[pos++]).put(normals[pos++]); // C
							ncBuf.put(normals[pos++]).put(normals[pos++]).put(normals[pos]);   // D
							pos = offset;
							ncBuf.put(normals[pos++]).put(normals[pos++]).put(normals[pos]);   // A
							pos = offset + 2 * 3;
							ncBuf.put(normals[pos++]).put(normals[pos++]).put(normals[pos]);   // C
						}
						break;
					default:
						ncBuf = null;
				}
				if ((command & PDATA_COLOR_PER_COMMAND) != 0) {
					Matrix.multiplyMM(MVP_TMP, 0, pm, 0, mvm, 0);
					renderMesh(MVP_TMP, mvm, command, blendEnabled, effect, vcBuf, ncBuf, colors[0]);
				} else if ((command & PDATA_TEXURE_COORD) != 0) {
					ByteBuffer tcBuf = ByteBuffer.allocateDirect(numPrimitives * 6 * 2)
							.order(ByteOrder.nativeOrder());
					for (int i = 0; i < numPrimitives; i++) {
						int offset = i * 4 * 2;
						int pos = offset;
						tcBuf.put((byte) texCoords[pos++]).put((byte) texCoords[pos++]); // A
						tcBuf.put((byte) texCoords[pos++]).put((byte) texCoords[pos++]); // B
						tcBuf.put((byte) texCoords[pos++]).put((byte) texCoords[pos++]); // C
						tcBuf.put((byte) texCoords[pos++]).put((byte) texCoords[pos]);   // D
						pos = offset;
						tcBuf.put((byte) texCoords[pos++]).put((byte) texCoords[pos]);   // A
						pos = offset + 2 * 2;
						tcBuf.put((byte) texCoords[pos++]).put((byte) texCoords[pos]);   // C
					}
					tcBuf.rewind();
					float[] mvp = MVP_TMP;
					Matrix.multiplyMM(mvp, 0, pm, 0, mvm, 0);
					renderMesh(texture, mvp, mvm, command, blendEnabled, effect, vcBuf, ncBuf, tcBuf);
				} else if ((command & PDATA_COLOR_PER_FACE) != 0) {
					ByteBuffer colorBuf = ByteBuffer.allocateDirect(numPrimitives * 6 * 3)
							.order(ByteOrder.nativeOrder());
					for (int i = 0; i < numPrimitives; i++) {
						int color = colors[i];
						byte r = (byte) (color >> 16 & 0xFF);
						byte g = (byte) (color >> 8 & 0xFF);
						byte b = (byte) (color & 0xFF);
						colorBuf.put(r).put(g).put(b);
						colorBuf.put(r).put(g).put(b);
						colorBuf.put(r).put(g).put(b);
						colorBuf.put(r).put(g).put(b);
						colorBuf.put(r).put(g).put(b);
						colorBuf.put(r).put(g).put(b);
					}
					colorBuf.rewind();
					float[] mvp = MVP_TMP;
					Matrix.multiplyMM(mvp, 0, pm, 0, mvm, 0);
					renderMesh(mvp, mvm, command, blendEnabled, effect, vcBuf, ncBuf, colorBuf);
				}
				break;
			}
			case Graphics3D.PRIMITVE_POINT_SPRITES: {
				renderSprites(texture, command, numPrimitives, vertices, texCoords, layout, pm, mvm, blend, blendEnabled);
			}
		}
	}

	private void renderSprites(Texture texture, int command, int numPrimitives, int[] vertices, int[] texCoords, FigureLayout layout, float[] pm, float[] mvm, int blend, boolean blendEnabled) {
		if (skipSprites) return;

		int numParams;
		switch (command & PDATA_POINT_SPRITE_PARAMS_PER_VERTEX) {
			case PDATA_POINT_SPRITE_PARAMS_PER_CMD:
				numParams = 1;
				break;
			case PDATA_POINT_SPRITE_PARAMS_PER_FACE:
			case PDATA_POINT_SPRITE_PARAMS_PER_VERTEX:
				numParams = numPrimitives;
				break;
			default:
				throw new IllegalArgumentException("Point sprite params is 0");
		}
		Program.Sprite program = Program.sprite;
		program.use();

		float[] m = new float[16];
		Matrix.multiplyMM(m, 0, pm, 0, mvm, 0);
		float[] vert = new float[8];
		float[] quad = new float[4 * 6];

		FloatBuffer vcBuf = ByteBuffer.allocateDirect(numPrimitives * 6 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		ByteBuffer tcBuf = ByteBuffer.allocateDirect(numPrimitives * 6 * 2).order(ByteOrder.nativeOrder());
		int pos = 0;
		int texOffset = 0;
		for (int i = 0; i < numPrimitives; i++) {
			vert[4] = vertices[pos++];
			vert[5] = vertices[pos++];
			vert[6] = vertices[pos++];
			vert[7] = 1.0f;
			Matrix.multiplyMV(vert, 0, m, 0, vert, 4);

			if (numParams != 1) {
				texOffset = i * 8;
			}

			float width = texCoords[texOffset];
			float height = texCoords[texOffset + 1];
			int angle = texCoords[texOffset + 2];
			float halfWidth;
			float halfHeight;
			switch (texCoords[texOffset + 7]) {
				case Graphics3D.POINT_SPRITE_LOCAL_SIZE | Graphics3D.POINT_SPRITE_PERSPECTIVE:
					halfWidth = width * pm[0] * 0.5f;
					halfHeight = height * pm[5] * 0.5f;
					break;
				case Graphics3D.POINT_SPRITE_PIXEL_SIZE | Graphics3D.POINT_SPRITE_PERSPECTIVE:
					if (layout.settingIndex <= Graphics3D.COMMAND_PARALLEL_SIZE) {
						halfWidth = width / this.width;
						halfHeight = height / this.height;
					} else {
						halfWidth = width / this.width * layout.near;
						halfHeight = height / this.height * layout.near;
					}
					break;
				case Graphics3D.POINT_SPRITE_LOCAL_SIZE | Graphics3D.POINT_SPRITE_NO_PERS:
					if (layout.settingIndex <= Graphics3D.COMMAND_PARALLEL_SIZE) {
						halfWidth = width * pm[0] * 0.5f;
						halfHeight = height * pm[5] * 0.5f;
					} else {
						float near = layout.near;
						halfWidth = width * pm[0] / near * 0.5f * vert[3];
						halfHeight = height * pm[5] / near * 0.5f * vert[3];
					}
					break;
				case Graphics3D.POINT_SPRITE_PIXEL_SIZE | Graphics3D.POINT_SPRITE_NO_PERS:
					halfWidth = width / this.width * vert[3];
					halfHeight = height / this.height * vert[3];
					break;
				default:
					throw new IllegalArgumentException();
			}
			Utils.getSpriteVertex(quad, vert, angle, halfWidth, halfHeight);
			vcBuf.put(quad);

			byte x0 = (byte) texCoords[texOffset + 3];
			byte y0 = (byte) texCoords[texOffset + 4];
			byte x1 = (byte) (texCoords[texOffset + 5] - 1);
			byte y1 = (byte) (texCoords[texOffset + 6] - 1);

			tcBuf.put(x0).put(y1);
			tcBuf.put(x0).put(y0);
			tcBuf.put(x1).put(y1);
			tcBuf.put(x1).put(y1);
			tcBuf.put(x0).put(y0);
			tcBuf.put(x1).put(y0);
		}
		vcBuf.rewind();
		glVertexAttribPointer(program.aPosition, 4, GL_FLOAT, false, 4 * 4, vcBuf);
		glEnableVertexAttribArray(program.aPosition);

		tcBuf.rewind();
		glVertexAttribPointer(program.aColorData, 2, GL_UNSIGNED_BYTE, false, 2, tcBuf);
		glEnableVertexAttribArray(program.aColorData);

		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, texture.getId());
		glUniform1i(program.uTexUnit, 0);
		glUniform2f(program.uTexSize, texture.width, texture.height);
		glUniform3fv(program.uColorKey, 1, texture.getColorKey());

		applyBlending(blendEnabled ? blend >> 4 : 0);
		glUniform1i(program.uIsTransparency, (command & PATTR_COLORKEY));
		glDrawArrays(GL_TRIANGLES, 0, numPrimitives * 6);
		glDisableVertexAttribArray(program.aPosition);
		glDisableVertexAttribArray(program.aColorData);
		checkGlError("drawPointSprites");
	}

	private void renderMesh(float[] mvp, float[] mv, int command, boolean blendEnabled,
							Effect3D effect, FloatBuffer vertices, FloatBuffer normals, int color) {
		Program.Color program = Program.color;
		program.use();
		glVertexAttrib2f(program.aMaterial, effect.isLighting ? 1 : 0, effect.isReflection ? 1 : 0);
		if (effect.isLighting && effect.mTexture != null && (command & Graphics3D.PATTR_SPHERE_MAP) != 0) {
			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GL_TEXTURE_2D, effect.mTexture.getId());
			glUniform1i(program.uSphereUnit, 2);
			glUniform2f(program.uSphereSize, 64.0f / effect.mTexture.width, 64.0f / effect.mTexture.height);
		} else {
			glUniform2f(program.uSphereSize, -1, -1);
		}
		if (normals != null && (command & ENV_ATTR_LIGHTING) != 0) {
			program.setLight(effect.isLighting ? effect.light : null);
		} else {
			program.setLight(null);
		}
		program.setToonShading(effect);
		program.bindMatrices(mvp, mv);

		vertices.rewind();
		glVertexAttribPointer(program.aPosition, 3, GL_FLOAT, false, 3 * 4, vertices);
		glEnableVertexAttribArray(program.aPosition);

		if (normals != null) {
			normals.rewind();
			glVertexAttribPointer(program.aNormal, 3, GL_FLOAT, false, 3 * 4, normals);
			glEnableVertexAttribArray(program.aNormal);
		} else {
			glDisableVertexAttribArray(program.aNormal);
		}

		program.setColor(color);

		glDisable(GL_CULL_FACE);
		int blendMode = command & PATTR_BLEND_SUB;
		applyBlending(blendEnabled ? blendMode >> 4 : 0);
		glDrawArrays(GL_TRIANGLES, 0, vertices.capacity() / 3);
		glDisableVertexAttribArray(program.aPosition);
		glDisableVertexAttribArray(program.aNormal);
		checkGlError("glDrawArrays");
	}

	private void renderMesh(float[] mvp, float[] mv, int command, boolean blendEnabled,
							Effect3D effect, FloatBuffer vertices, FloatBuffer normals, ByteBuffer colors) {
		Program.Color program = Program.color;
		program.use();
		glVertexAttrib2f(program.aMaterial, effect.isLighting ? 1 : 0, effect.isReflection ? 1 : 0);
		if (effect.isLighting && effect.mTexture != null && (command & Graphics3D.PATTR_SPHERE_MAP) != 0) {
			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GL_TEXTURE_2D, effect.mTexture.getId());
			glUniform1i(program.uSphereUnit, 2);
			glUniform2f(program.uSphereSize, 64.0f / effect.mTexture.width, 64.0f / effect.mTexture.height);
		} else {
			glUniform2f(program.uSphereSize, -1, -1);
		}
		if (normals != null && (command & ENV_ATTR_LIGHTING) != 0) {
			program.setLight(effect.isLighting ? effect.light : null);
		} else {
			program.setLight(null);
		}
		program.bindMatrices(mvp, mv);
		program.setToonShading(effect);

		vertices.rewind();
		glVertexAttribPointer(program.aPosition, 3, GL_FLOAT, false, 3 * 4, vertices);
		glEnableVertexAttribArray(program.aPosition);

		if (normals != null) {
			normals.rewind();
			glVertexAttribPointer(program.aNormal, 3, GL_FLOAT, false, 3 * 4, normals);
			glEnableVertexAttribArray(program.aNormal);
		} else {
			glDisableVertexAttribArray(program.aNormal);
		}

		colors.rewind();
		glVertexAttribPointer(program.aColorData, 3, GL_UNSIGNED_BYTE, true, 3, colors);
		glEnableVertexAttribArray(program.aColorData);

		glDisable(GL_CULL_FACE);
		int blendMode = command & PATTR_BLEND_SUB;
		applyBlending(blendEnabled ? blendMode >> 4 : 0);
		glDrawArrays(GL_TRIANGLES, 0, vertices.capacity() / 3);
		glDisableVertexAttribArray(program.aColorData);
		glDisableVertexAttribArray(program.aPosition);
		glDisableVertexAttribArray(program.aNormal);
		checkGlError("glDrawArrays");
	}

	private void renderMesh(Texture texture, float[] mvp, float[] mv, int command, boolean blendEnabled,
							Effect3D effect, FloatBuffer vertices, FloatBuffer normals, ByteBuffer texCoords) {
		Program.Tex program = Program.tex;
		program.use();
		glVertexAttrib3f(program.aMaterial,
				effect.isLighting ? 1 : 0,
				effect.isReflection ? 1 : 0,
				command & PATTR_COLORKEY);
		if (effect.isLighting && effect.mTexture != null && (command & Graphics3D.PATTR_SPHERE_MAP) != 0) {
			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GL_TEXTURE_2D, effect.mTexture.getId());
			glUniform1i(program.uSphereUnit, 2);
			glUniform2f(program.uSphereSize, 64.0f / effect.mTexture.width, 64.0f / effect.mTexture.height);
		} else {
			glUniform2f(program.uSphereSize, -1, -1);
		}
		if (normals != null && (command & ENV_ATTR_LIGHTING) != 0) {
			program.setLight(effect.isLighting ? effect.light : null);
		} else {
			program.setLight(null);
		}
		program.setToonShading(effect);
		program.bindMatrices(mvp, mv);

		vertices.rewind();
		glVertexAttribPointer(program.aPosition, 3, GL_FLOAT, false, 3 * 4, vertices);
		glEnableVertexAttribArray(program.aPosition);

		if (normals != null) {
			normals.rewind();
			glVertexAttribPointer(program.aNormal, 3, GL_FLOAT, false, 3 * 4, normals);
			glEnableVertexAttribArray(program.aNormal);
		} else {
			glDisableVertexAttribArray(program.aNormal);
		}

		texCoords.rewind();
		glVertexAttribPointer(program.aColorData, 2, GL_UNSIGNED_BYTE, false, 2, texCoords);
		glEnableVertexAttribArray(program.aColorData);

		program.enableTexUnit();
		program.setTex(texture);

		glDisable(GL_CULL_FACE);
		int blendMode = command & PATTR_BLEND_SUB;
		applyBlending(blendEnabled ? blendMode >> 4 : 0);
		glDrawArrays(GL_TRIANGLES, 0, vertices.capacity() / 3);
		glDisableVertexAttribArray(program.aPosition);
		glDisableVertexAttribArray(program.aColorData);
		glDisableVertexAttribArray(program.aNormal);
		checkGlError("glDrawArrays");
	}

	void drawCmd(Texture[] textures, int x, int y, FigureLayout layout, Effect3D effect, int[] cmds) {
		if (COMMAND_LIST_VERSION_1_0 != cmds[0]) {
			throw new IllegalArgumentException("Unsupported command list version: " + cmds[0]);
		}
		if (textures != null) {
			this.textures = textures.clone();
		}
		layout = new FigureLayout(layout);
		effect = new Effect3D(effect);
		for (int i = 1; i < cmds.length; ) {
			int cmd = cmds[i++];
			switch (cmd & 0xFF000000) {
				case COMMAND_AFFINE_INDEX:
					layout.selectAffineTrans(cmd & 0xFFFFFF);
					break;
				case COMMAND_AMBIENT_LIGHT: {
					Light light = effect.getLight();
					if (light == null) {
						light = new Light();
						effect.setLight(light);
					}
					light.setAmbientIntensity(i++);
					break;
				}
				case COMMAND_ATTRIBUTE:
					int params = cmd & 0xFFFFFF;
					effect.setTransparency((params & ENV_ATTR_SEMI_TRANSPARENT) != 0);
					effect.isLighting = (params & ENV_ATTR_LIGHTING) != 0;
					effect.isReflection = (params & ENV_ATTR_SPHERE_MAP) != 0;
					effect.isToonShading = (params & ENV_ATTR_TOON_SHADING) != 0;
					break;
				case COMMAND_CENTER:
					layout.setCenter(cmds[i++], cmds[i++]);
					break;
				case COMMAND_CLIP:
					clip.intersect(cmds[i++], cmds[i++], cmds[i++], cmds[i++]);
					updateClip();
					break;
				case COMMAND_DIRECTION_LIGHT: {
					Light light = effect.getLight();
					if (light == null) {
						light = new Light();
						effect.setLight(light);
					}
					light.getParallelLightDirection().set(i++, i++, i++);
					light.setParallelLightIntensity(i++);
					break;
				}
				case COMMAND_FLUSH:
					flush();
					break;
				case COMMAND_NOP:
					i += cmd & 0xFFFFFF;
					break;
				case COMMAND_PARALLEL_SCALE:
					layout.setScale(cmds[i++], cmds[i++]);
					break;
				case COMMAND_PARALLEL_SIZE:
					layout.setParallelSize(cmds[i++], cmds[i++]);
					break;
				case COMMAND_PERSPECTIVE_FOV:
					layout.setPerspective(cmds[i++], cmds[i++], cmds[i++]);
					break;
				case COMMAND_PERSPECTIVE_WH:
					layout.setPerspective(cmds[i++], cmds[i++], cmds[i++], cmds[i++]);
					break;
				case COMMAND_TEXTURE_INDEX:
					int tid = cmd & 0xFFFFFF;
					if (tid > 0 && tid < 16) {
						this.textureIdx = tid;
					}
					break;
				case COMMAND_THRESHOLD:
					effect.setToonParams(cmds[i++], cmds[i++], cmds[i++]);
					break;
				case COMMAND_END:
					return;
				default:
					int type = cmd & 0x7000000;
					if (type == 0) {
						break;
					}
					int num = cmd >> 16 & 0xFF;
					int sizeOf = sizeOf(type);
					int len = num * 3 * sizeOf;
					int[] vert = new int[len];
					System.arraycopy(cmds, i, vert, 0, len);
					i += len;
					int[] norm;
					if (type == PRIMITVE_TRIANGLES || type == PRIMITVE_QUADS) {
						switch (cmd & PDATA_NORMAL_PER_VERTEX) {
							case PDATA_NORMAL_PER_FACE:
								len = num * 3;
								norm = new int[len];
								System.arraycopy(cmds, i, norm, 0, len);
								i += len;
								break;
							case PDATA_NORMAL_PER_VERTEX:
								norm = new int[len];
								System.arraycopy(cmds, i, norm, 0, len);
								i += len;
								break;
							default:
								norm = EMPTY_ARRAY;
								break;
						}
					} else norm = EMPTY_ARRAY;
					int[] texCoord;
					int[] col;
					if ((cmd & PDATA_COLOR_PER_COMMAND) != 0) {
						col = new int[]{cmds[i++]};
					} else if ((cmd & PDATA_COLOR_PER_FACE) != 0) {
						col = new int[num];
						System.arraycopy(cmds, i, col, 0, num);
						i += num;
					} else {
						col = EMPTY_ARRAY;
					}
					if ((cmd & PDATA_TEXURE_COORD) != 0) {
						int tcLen;
						if (type == PRIMITVE_POINT_SPRITES) {
							switch (cmd & PDATA_POINT_SPRITE_PARAMS_PER_VERTEX) {
								case PDATA_POINT_SPRITE_PARAMS_PER_CMD:
									tcLen = 8;
									break;
								case PDATA_POINT_SPRITE_PARAMS_PER_FACE:
								case PDATA_POINT_SPRITE_PARAMS_PER_VERTEX:
									tcLen = num * 8;
									break;
								default:
									throw new IllegalArgumentException("Point sprite params is 0");
							}
						} else {
							tcLen = num * 2 * sizeOf;
						}
						texCoord = new int[tcLen];
						System.arraycopy(cmds, i, texCoord, 0, tcLen);
						i += tcLen;
					} else {
						texCoord = EMPTY_ARRAY;
					}
					synchronized (this) {
						Effect3D effectCopy = new Effect3D(effect);
						FigureLayout layoutCopy = new FigureLayout(layout);
						layoutCopy.centerX += x;
						layoutCopy.centerY += y;
						Texture finalTex = getTexture();
						stack.add(new RenderNode() {
							@Override
							public void run() {
								renderPrimitives(finalTex, cmd, num, vert,
										norm, texCoord, col, effectCopy, layoutCopy);
							}
						});
					}
					break;
			}
		}
	}

	private void updateClip() {
		bindEglContext();
		Rect clip = this.clip;
		int l = clip.left;
		int t = clip.top;
		int r = clip.right;
		int b = clip.bottom;
		if (l == 0 && t == 0 && r == width && b == height) {
			glDisable(GL_SCISSOR_TEST);
		} else {
			glEnable(GL_SCISSOR_TEST);
			glScissor(l, t, r - l, b - t);
		}
		releaseEglContext();
	}

	private int sizeOf(int type) {
		switch (type) {
			case PRIMITVE_POINTS:
			case PRIMITVE_POINT_SPRITES: return 1;
			case PRIMITVE_LINES:         return 2;
			case PRIMITVE_TRIANGLES:     return 3;
			case PRIMITVE_QUADS:         return 4;
			default:                     return 0;
		}
	}

	synchronized void postFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect) {
		Texture[] ta = figure.textures;
		if (ta != null) {
			textures = ta.clone();
		}
		FigureNode rn;
		if (figure.stack.empty()) {
			rn = new FigureNode(this, figure, x, y, layout, effect);
		} else {
			rn = figure.stack.pop();
			rn.setData(this, x, y, layout, effect);
		}
		rn.textures = textures == null ? null : textures.clone();
		stack.add(rn);
	}

	synchronized void postPrimitives(Texture texture, int x, int y, FigureLayout layout, Effect3D effect,
									 int command, int numPrimitives,
									 int[] vertexCoords, int[] normals, int[] textureCoords, int[] colors) {
		Effect3D effectCopy = new Effect3D(effect);
		FigureLayout layoutCopy = new FigureLayout(layout);
		layoutCopy.centerX += x;
		layoutCopy.centerY += y;
		setTexture(texture);
		Texture finalTex = getTexture();
		stack.add(new RenderNode() {
			@Override
			public void run() {
				renderPrimitives(finalTex, command, numPrimitives, vertexCoords,
						normals, textureCoords, colors, effectCopy, layoutCopy);
			}
		});
	}

	synchronized void drawFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect) {
		bindEglContext();
		if (!backCopied && preCopy2D) copy2d(true);
		try {
			flushStep = 1;
			for (int i = 0, stackSize = stack.size(); i < stackSize; i++) {
				RenderNode r = stack.get(i);
				r.run();
			}
			Texture tex = figure.getTexture();
			setTexture(tex);
			Model data = figure.data;
			FloatBuffer vertices = figure.getVertexData();
			FloatBuffer normals = figure.getNormalsData();
			renderFigure(data, x, y, layout, textures, effect, vertices, normals);
			flushStep = 2;
			for (int i = 0, stackSize = stack.size(); i < stackSize; i++) {
				RenderNode r = stack.get(i);
				r.run();
				r.recycle();
			}
			renderFigure(data, x, y, layout, textures, effect, vertices, normals);
			glDisable(GL_BLEND);
			glDepthMask(true);
			glClear(GL_DEPTH_BUFFER_BIT);
		} finally {
			stack.clear();
			releaseEglContext();
		}
	}

	void bindEglContext() {
		((EGL10) EGLContext.getEGL()).eglMakeCurrent(eglDisplay, eglWindowSurface, eglWindowSurface, eglContext);
	}

	void releaseEglContext() {
		((EGL10) EGLContext.getEGL()).eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
	}

	void reset() {
		stack.clear();
	}

	void setTexture(Texture texture) {
		textures = new Texture[]{texture};
		textureIdx = 0;
	}

	Texture getTexture() {
		return textures == null ? null : textureIdx < textures.length ? textures[textureIdx] : null;
	}
}
