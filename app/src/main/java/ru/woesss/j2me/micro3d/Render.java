/*
 *  Copyright 2022 Yury Kharchenko
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

package ru.woesss.j2me.micro3d;

import static android.opengl.GLES20.*;
import static com.mascotcapsule.micro3d.v3.Graphics3D.*;
import static ru.woesss.j2me.micro3d.Utils.TAG;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.mascotcapsule.micro3d.v3.Light;
import com.mascotcapsule.micro3d.v3.Texture;
import com.motorola.graphics.j3d.Effect3D;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.lcdui.Graphics;

import ru.woesss.j2me.micro3d.RenderNode.FigureNode;

public class Render {
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
	private boolean backCopied;
	private final LinkedList<RenderNode> stack = new LinkedList<>();
	private int flushStep;
	private final TextureImpl[] textures = new TextureImpl[16];
	private final boolean postCopy2D = !Boolean.getBoolean("micro3d.v3.render.no-mix2D3D");
	private final boolean preCopy2D = !Boolean.getBoolean("micro3d.v3.render.background.ignore");
	private int textureIdx;
	private int texturesLen;
	private IntBuffer bufHandles;
	private Texture specular;
	private final Light light = new Light();

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

	public synchronized static Render getRender() {
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

	public synchronized void bind(Graphics graphics) {
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
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter ? GL_LINEAR : GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter ? GL_LINEAR : GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		} else {
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D, bgTextureId[0]);
		}
		GLUtils.texImage2D(GL_TEXTURE_2D, 0, mBitmapBuffer, 0);
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

	void renderFigure(Model model, int x, int y, TextureImpl[] textures,
					  FloatBuffer vertices, FloatBuffer normals, FigureLayoutImpl layout, Effect3dImpl effect) {
		if (!effect.isTransparency && flushStep == 2) return;

		if (!model.hasPolyT && !model.hasPolyC)
			return;

		glEnable(GL_DEPTH_TEST);
		glDepthMask(flushStep == 1);
		float[] mvm = layout.getViewMatrix();
		float[] pm = layout.getProjectionMatrix(x, y, this.width, this.height);
		float[] mvp = MVP_TMP;
		Matrix.multiplyMM(mvp, 0, pm, 0, mvm, 0);
		if (bufHandles == null) {
			bufHandles = ByteBuffer.allocateDirect(4 * 3).order(ByteOrder.nativeOrder()).asIntBuffer();
			glGenBuffers(3, bufHandles);
		}
		vertices.rewind();
		try {
			glBindBuffer(GL_ARRAY_BUFFER, bufHandles.get(0));
			glBufferData(GL_ARRAY_BUFFER, vertices.capacity() * 4, vertices, GL_STREAM_DRAW);
			ByteBuffer tcBuf = model.texCoordArray;
			tcBuf.rewind();
			glBindBuffer(GL_ARRAY_BUFFER, bufHandles.get(1));
			glBufferData(GL_ARRAY_BUFFER, tcBuf.capacity(), tcBuf, GL_STREAM_DRAW);

			boolean isLight = effect.light != null && normals != null;
			if (isLight) {
				normals.rewind();
				glBindBuffer(GL_ARRAY_BUFFER, bufHandles.get(2));
				glBufferData(GL_ARRAY_BUFFER, normals.capacity() * 4, normals, GL_STREAM_DRAW);
			}
			Texture sphere = effect.texture;
			if (model.hasPolyT) {
				final Program.Tex program = Program.tex;
				program.use();

				glBindBuffer(GL_ARRAY_BUFFER, bufHandles.get(0));
				glEnableVertexAttribArray(program.aPosition);
				glVertexAttribPointer(program.aPosition, 3, GL_FLOAT, false, 3 * 4, 0);

				glBindBuffer(GL_ARRAY_BUFFER, bufHandles.get(1));
				glEnableVertexAttribArray(program.aColorData);
				glVertexAttribPointer(program.aColorData, 2, GL_UNSIGNED_BYTE, false, 5, 0);
				glEnableVertexAttribArray(program.aMaterial);
				glVertexAttribPointer(program.aMaterial, 3, GL_UNSIGNED_BYTE, false, 5, 2);

				if (isLight) {
					glBindBuffer(GL_ARRAY_BUFFER, bufHandles.get(2));
					glEnableVertexAttribArray(program.aNormal);
					glVertexAttribPointer(program.aNormal, 3, GL_FLOAT, false, 3 * 4, 0);
					program.setToonShading(effect);
					program.setLight(effect.light);
					program.setSphere(sphere == null ? null : sphere.impl);
				} else {
					glDisableVertexAttribArray(program.aNormal);
					program.setLight(null);
				}

				program.bindMatrices(mvp, mvm);
				// Draw triangles
				renderModel(textures, model, effect.isTransparency);
				glDisableVertexAttribArray(program.aPosition);
				glDisableVertexAttribArray(program.aColorData);
				glDisableVertexAttribArray(program.aMaterial);
				glDisableVertexAttribArray(program.aNormal);
				glBindBuffer(GL_ARRAY_BUFFER, 0);
			}

			if (model.hasPolyC) {
				final Program.Color program = Program.color;
				program.use();

				int offset = model.numVerticesPolyT;
				glBindBuffer(GL_ARRAY_BUFFER, bufHandles.get(0));
				glEnableVertexAttribArray(program.aPosition);
				glVertexAttribPointer(program.aPosition, 3, GL_FLOAT, false, 3 * 4, 3 * 4 * offset);

				glBindBuffer(GL_ARRAY_BUFFER, bufHandles.get(1));
				glVertexAttribPointer(program.aColorData, 3, GL_UNSIGNED_BYTE, true, 5, 5 * offset);
				glEnableVertexAttribArray(program.aColorData);
				glEnableVertexAttribArray(program.aMaterial);
				glVertexAttribPointer(program.aMaterial, 2, GL_UNSIGNED_BYTE, false, 5, 5 * offset + 3);

				if (isLight) {
					glBindBuffer(GL_ARRAY_BUFFER, bufHandles.get(2));
					glVertexAttribPointer(program.aNormal, 3, GL_FLOAT, false, 3 * 4, 3 * 4 * offset);
					glEnableVertexAttribArray(program.aNormal);
					program.setLight(effect.light);
					program.setSphere(sphere == null ? null : sphere.impl);
					program.setToonShading(effect);
				} else {
					glDisableVertexAttribArray(program.aNormal);
					program.setLight(null);
				}
				program.bindMatrices(mvp, mvm);
				renderModel(model, effect.isTransparency);
				glDisableVertexAttribArray(program.aPosition);
				glDisableVertexAttribArray(program.aColorData);
				glDisableVertexAttribArray(program.aMaterial);
				glDisableVertexAttribArray(program.aNormal);
			}
		} finally {
			glBindBuffer(GL_ARRAY_BUFFER, 0);
		}
	}

	private void renderModel(TextureImpl[] textures, Model model, boolean enableBlending) {
		if (textures == null || textures.length == 0) return;
		Program.Tex program = Program.tex;
		int[][][] meshes = model.subMeshesLengthsT;
		int length = meshes.length;
		int blendMode = 0;
		int pos = 0;
		if (flushStep == 1) {
			if (enableBlending) length = 1;
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
				if (face >= textures.length) {
					program.setTex(null);
				} else {
					TextureImpl tex = textures[face];
					program.setTex(tex);
				}
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
		int pos = 0;
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

	public synchronized void release() {
		bindEglContext();
		stack.clear();
		if (postCopy2D) {
			copy2d(false);
		}
		Rect clip = this.gClip;
		Utils.glReadPixels(clip.left, clip.top, clip.width(), clip.height(), mBitmapBuffer);
		releaseEglContext();
	}

	public synchronized void flush() {
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

	private void renderPrimitives(TextureImpl texture, int command,
								  int[] vertices, int[] normals, int[] texCoords,
								  int[] colors, FigureLayoutImpl layout, Effect3dImpl effect) {
		float[] pm = layout.getProjectionMatrix(0, 0, this.width, this.height);
		float[] mvm = layout.getViewMatrix();
		boolean blendEnabled = effect.isTransparency && (command & PATTR_BLEND_SUB) != 0;
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
		int numPrimitives = command >> 16 & 0xff;
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

				applyBlending(blendEnabled ? (command & PATTR_BLEND_SUB) >> 4 : 0);
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

				applyBlending(blendEnabled ? (command & PATTR_BLEND_SUB) >> 4 : 0);
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
					renderMesh(mvp, mvm, command, blendEnabled, vcBuf, ncBuf, colors[0], effect);
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
					renderMesh(texture, mvp, mvm, command, blendEnabled, vcBuf, ncBuf, tcBuf, effect);
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
					renderMesh(texture, mvp, mvm, command, blendEnabled, vcBuf, ncBuf, colorBuf, effect);
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
					renderMesh(MVP_TMP, mvm, command, blendEnabled, vcBuf, ncBuf, colors[0], effect);
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
					renderMesh(texture, mvp, mvm, command, blendEnabled, vcBuf, ncBuf, tcBuf, effect);
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
					renderMesh(texture, mvp, mvm, command, blendEnabled, vcBuf, ncBuf, colorBuf, effect);
				}
				break;
			}
			case PRIMITVE_POINT_SPRITES: {
				renderSprites(texture, command, numPrimitives, vertices, texCoords, layout, pm, mvm, blendEnabled);
			}
		}
	}

	private void renderSprites(TextureImpl texture, int command, int numPrimitives,
							   int[] vertices, int[] texCoords, FigureLayoutImpl layout,
							   float[] pm, float[] mvm, boolean blendEnabled) {
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
				case POINT_SPRITE_LOCAL_SIZE | POINT_SPRITE_PERSPECTIVE:
					halfWidth = width * pm[0] * 0.5f;
					halfHeight = height * pm[5] * 0.5f;
					break;
				case POINT_SPRITE_PIXEL_SIZE | POINT_SPRITE_PERSPECTIVE:
					if (layout.projection <= COMMAND_PARALLEL_SIZE) {
						halfWidth = width / this.width;
						halfHeight = height / this.height;
					} else {
						halfWidth = width / this.width * layout.near;
						halfHeight = height / this.height * layout.near;
					}
					break;
				case POINT_SPRITE_LOCAL_SIZE | POINT_SPRITE_NO_PERS:
					if (layout.projection <= COMMAND_PARALLEL_SIZE) {
						halfWidth = width * pm[0] * 0.5f;
						halfHeight = height * pm[5] * 0.5f;
					} else {
						float near = layout.near;
						halfWidth = width * pm[0] / near * 0.5f * vert[3];
						halfHeight = height * pm[5] / near * 0.5f * vert[3];
					}
					break;
				case POINT_SPRITE_PIXEL_SIZE | POINT_SPRITE_NO_PERS:
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

		program.setTexture(texture);

		applyBlending(blendEnabled ? (command & PATTR_BLEND_SUB) >> 4 : 0);
		glUniform1i(program.uIsTransparency, (command & PATTR_COLORKEY));
		glDrawArrays(GL_TRIANGLES, 0, numPrimitives * 6);
		glDisableVertexAttribArray(program.aPosition);
		glDisableVertexAttribArray(program.aColorData);
		checkGlError("drawPointSprites");
	}

	private void renderMesh(float[] mvp, float[] mv, int command, boolean blendEnabled,
							FloatBuffer vertices, FloatBuffer normals, int color, Effect3dImpl effect) {
		Program.Color program = Program.color;
		program.use();
		if (effect.isLighting && normals != null && (command & PATTR_LIGHTING) != 0) {
			Texture sphere = effect.texture;
			if (effect.isReflection && (command & PATTR_SPHERE_MAP) != 0 && sphere != null) {
				glVertexAttrib2f(program.aMaterial, 1, 1);
				program.setSphere(sphere.impl);
			} else {
				glVertexAttrib2f(program.aMaterial, 1, 0);
				program.setSphere(null);
			}
			program.setLight(effect.getLight());
			program.setToonShading(effect);

			normals.rewind();
			glVertexAttribPointer(program.aNormal, 3, GL_FLOAT, false, 3 * 4, normals);
			glEnableVertexAttribArray(program.aNormal);
		} else {
			glVertexAttrib2f(program.aMaterial, 0, 0);
			program.setLight(null);
			glDisableVertexAttribArray(program.aNormal);
		}

		program.bindMatrices(mvp, mv);

		vertices.rewind();
		glVertexAttribPointer(program.aPosition, 3, GL_FLOAT, false, 3 * 4, vertices);
		glEnableVertexAttribArray(program.aPosition);

		program.setColor(color);

		glDisable(GL_CULL_FACE);
		applyBlending(blendEnabled ? (command & PATTR_BLEND_SUB) >> 4 : 0);
		glDrawArrays(GL_TRIANGLES, 0, vertices.capacity() / 3);
		glDisableVertexAttribArray(program.aPosition);
		glDisableVertexAttribArray(program.aNormal);
		checkGlError("glDrawArrays");
	}

	private void renderMesh(TextureImpl texture, float[] mvp, float[] mv, int command, boolean blendEnabled,
							FloatBuffer vertices, FloatBuffer normals, ByteBuffer colors, Effect3dImpl effect) {
		Program.Color program = Program.color;
		program.use();
		if (effect.isLighting && normals != null && (command & PATTR_LIGHTING) != 0) {
			Texture sphere = effect.texture;
			if (effect.isReflection && (command & PATTR_SPHERE_MAP) != 0 && sphere != null) {
				glVertexAttrib2f(program.aMaterial, 1, 1);
				program.setSphere(sphere.impl);
			} else {
				glVertexAttrib2f(program.aMaterial, 1, 0);
				program.setSphere(null);
			}
			program.setLight(effect.getLight());
			program.setToonShading(effect);

			normals.rewind();
			glVertexAttribPointer(program.aNormal, 3, GL_FLOAT, false, 3 * 4, normals);
			glEnableVertexAttribArray(program.aNormal);
		} else {
			glVertexAttrib2f(program.aMaterial, 0, 0);
			program.setLight(null);
			glDisableVertexAttribArray(program.aNormal);
		}
		program.bindMatrices(mvp, mv);

		vertices.rewind();
		glVertexAttribPointer(program.aPosition, 3, GL_FLOAT, false, 3 * 4, vertices);
		glEnableVertexAttribArray(program.aPosition);

		colors.rewind();
		glVertexAttribPointer(program.aColorData, 3, GL_UNSIGNED_BYTE, true, 3, colors);
		glEnableVertexAttribArray(program.aColorData);

		glDisable(GL_CULL_FACE);
		applyBlending(blendEnabled ? (command & PATTR_BLEND_SUB) >> 4 : 0);
		glDrawArrays(GL_TRIANGLES, 0, vertices.capacity() / 3);
		glDisableVertexAttribArray(program.aColorData);
		glDisableVertexAttribArray(program.aPosition);
		glDisableVertexAttribArray(program.aNormal);
		checkGlError("glDrawArrays");
	}

	private void renderMesh(Texture texture, float[] mvp, float[] mv, int command, boolean blendEnabled,
							FloatBuffer vertices, FloatBuffer normals, ByteBuffer texCoords, Effect3dImpl effect) {
		Program.Tex program = Program.tex;
		program.use();
		if (effect.isLighting && normals != null && (command & PATTR_LIGHTING) != 0) {
			Texture sphere = effect.texture;
			if (effect.isReflection && (command & PATTR_SPHERE_MAP) != 0 && sphere != null) {
				glVertexAttrib3f(program.aMaterial, 1, 1, command & PATTR_COLORKEY);
				program.setSphere(sphere.impl);
			} else {
				glVertexAttrib3f(program.aMaterial, 1, 0, command & PATTR_COLORKEY);
				program.setSphere(null);
			}
			program.setLight(effect.getLight());
			program.setToonShading(effect);

			normals.rewind();
			glVertexAttribPointer(program.aNormal, 3, GL_FLOAT, false, 3 * 4, normals);
			glEnableVertexAttribArray(program.aNormal);
		} else {
			glVertexAttrib3f(program.aMaterial, 0, 0, command & PATTR_COLORKEY);
			program.setLight(null);
			glDisableVertexAttribArray(program.aNormal);
		}

		program.bindMatrices(mvp, mv);

		vertices.rewind();
		glVertexAttribPointer(program.aPosition, 3, GL_FLOAT, false, 3 * 4, vertices);
		glEnableVertexAttribArray(program.aPosition);

		texCoords.rewind();
		glVertexAttribPointer(program.aColorData, 2, GL_UNSIGNED_BYTE, false, 2, texCoords);
		glEnableVertexAttribArray(program.aColorData);

		program.setTex(texture.impl);

		glDisable(GL_CULL_FACE);
		applyBlending(blendEnabled ? (command & PATTR_BLEND_SUB) >> 4 : 0);
		glDrawArrays(GL_TRIANGLES, 0, vertices.capacity() / 3);
		glDisableVertexAttribArray(program.aPosition);
		glDisableVertexAttribArray(program.aColorData);
		glDisableVertexAttribArray(program.aNormal);
		checkGlError("glDrawArrays");
	}

	public void drawCmd(TextureImpl[] textures, int x, int y, FigureLayoutImpl layout, Effect3dImpl effect, int[] cmds) {
		if (COMMAND_LIST_VERSION_1_0 != cmds[0]) {
			throw new IllegalArgumentException("Unsupported command list version: " + cmds[0]);
		}
		setTextureArray(textures);
		setSpecular(effect.texture);
		setLight(effect.light);
		layout = new FigureLayoutImpl(layout);
		layout.centerX += x;
		layout.centerY += y;
		effect = new Effect3dImpl(effect);
		effect.isLighting = effect.light != null;
		effect.isToonShading = effect.shading == com.motorola.graphics.j3d.Effect3D.TOON_SHADING;
		effect.isReflection = effect.texture != null;
		effect.texture = specular;
		effect.light = light;
		for (int i = 1; i < cmds.length; ) {
			int cmd = cmds[i++];
			switch (cmd & 0xFF000000) {
				case COMMAND_AFFINE_INDEX:
					layout.selectAffineTrans(cmd & 0xFFFFFF);
					break;
				case COMMAND_AMBIENT_LIGHT: {
					light.setAmbientIntensity(i++);
					break;
				}
				case COMMAND_ATTRIBUTE:
					int params = cmd & 0xFFFFFF;
					effect.isTransparency = (params & ENV_ATTR_SEMI_TRANSPARENT) != 0;
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
						Effect3dImpl effectCopy = new Effect3dImpl(effect);
						FigureLayoutImpl layoutCopy = new FigureLayoutImpl(layout);
						TextureImpl finalTex = getTexture();
						stack.add(new RenderNode() {
							@Override
							public void run() {
								renderPrimitives(finalTex, cmd, vert,
										norm, texCoord, col, layoutCopy, effectCopy);
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

	public synchronized void postFigure(FigureImpl figure, int x, int y, FigureLayoutImpl layout, Effect3dImpl effect) {
		setTextureArray(figure.textures);
		setSpecular(effect.texture);
		setLight(effect.getLight());
		FigureNode rn;
		if (figure.stack.empty()) {
			rn = new FigureNode(this, figure, x, y, layout, effect);
		} else {
			rn = figure.stack.pop();
			rn.setData(this, x, y, layout, effect);
		}
		TextureImpl[] copy = new TextureImpl[texturesLen];
		System.arraycopy(textures, 0, copy, 0, texturesLen);
		rn.textures = (TextureImpl[]) copy;
		stack.add(rn);
	}

	public synchronized void postPrimitives(TextureImpl texture, int x, int y,
											FigureLayoutImpl layout, Effect3dImpl effect,
											int command,
											int[] vertexCoords, int[] normals,
											int[] textureCoords, int[] colors) {
		Effect3dImpl effectCopy = new Effect3dImpl(effect);
		effectCopy.isLighting = effect.light != null;
		effectCopy.isToonShading = effect.shading == Effect3D.TOON_SHADING;
		effectCopy.isReflection = effect.texture != null;
		FigureLayoutImpl layoutCopy = new FigureLayoutImpl(layout);
		layoutCopy.centerX += x;
		layoutCopy.centerY += y;
		setTexture(texture);
		setSpecular(effect.texture);
		setLight(effect.light);
		TextureImpl finalTex = getTexture();
		stack.add(new RenderNode() {
			@Override
			public void run() {
				renderPrimitives(finalTex, command, vertexCoords,
						normals, textureCoords, colors, layoutCopy, effectCopy);
			}
		});
	}

	public synchronized void drawFigure(FigureImpl figure, int x, int y, FigureLayoutImpl layout, Effect3dImpl effect) {
		bindEglContext();
		if (!backCopied && preCopy2D) copy2d(true);
		setTexture(figure.getTextureImpl());
		setSpecular(effect.texture);
		setLight(effect.light);
		try {
			flushStep = 1;
			for (int i = 0, stackSize = stack.size(); i < stackSize; i++) {
				RenderNode r = stack.get(i);
				r.run();
			}
			Model data = figure.data;
			FloatBuffer vertices = figure.getVertexData();
			FloatBuffer normals = figure.getNormalsData();
			renderFigure(data, x, y, textures, vertices, normals, layout, effect);
			flushStep = 2;
			for (int i = 0, stackSize = stack.size(); i < stackSize; i++) {
				RenderNode r = stack.get(i);
				r.run();
				r.recycle();
			}
			renderFigure(data, x, y, textures, vertices, normals, layout, effect);
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

	public void reset() {
		stack.clear();
	}

	void setTexture(TextureImpl tex) {
		if (tex == null) {
			return;
		}
		textures[0] = tex;
		textureIdx = 0;
		texturesLen = 1;
	}

	private void setTextureArray(TextureImpl[] tex) {
		if (tex == null) {
			return;
		}
		int len = tex.length;
		System.arraycopy(tex, 0, textures, 0, len);
		texturesLen = len;
	}

	void setTextureArray(Texture[] tex) {
		if (tex == null) {
			return;
		}
		int len = tex.length;
		for (int i = 0; i < len; i++) {
			textures[i] = tex[i].impl;
		}
		texturesLen = len;
	}

	TextureImpl getTexture() {
		if (textureIdx < 0 || textureIdx >= texturesLen) {
			return null;
		}
		return textures[textureIdx];
	}

	private void setSpecular(Texture tex) {
		if (tex != null) {
			specular = tex;
		}
	}

	private void setLight(Light light) {
		if (light != null) {
			this.light.impl.set(light.impl);
		}
	}
}
