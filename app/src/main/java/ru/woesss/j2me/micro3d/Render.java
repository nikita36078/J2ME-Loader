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
import static ru.woesss.j2me.micro3d.MathUtil.TO_FLOAT;
import static ru.woesss.j2me.micro3d.Utils.TAG;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

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
	final Params params = new Params();
	private EGLDisplay eglDisplay;
	private EGLSurface eglWindowSurface;
	private EGLConfig eglConfig;
	private EGLContext eglContext;
	private final int[] bgTextureId = new int[]{-1};
	private final float[] MVP_TMP = new float[16];

	private Bitmap targetBitmap;
	private final Rect gClip = new Rect();
	private final Rect clip = new Rect();
	private boolean backCopied;
	private final LinkedList<RenderNode> stack = new LinkedList<>();
	private int flushStep;
	private final boolean postCopy2D = !Boolean.getBoolean("micro3d.v3.render.no-mix2D3D");
	private final boolean preCopy2D = !Boolean.getBoolean("micro3d.v3.render.background.ignore");
	private IntBuffer bufHandles;
	private int clearColor;
	private TextureImpl targetTexture;

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

	public static Render getRender() {
		return InstanceHolder.instance;
	}

	private void init() {
		EGL10 egl = (EGL10) EGLContext.getEGL();
		eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

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
		eglConfig = eglConfigs[0];

		int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
		int[] attrib_list = {
				EGL_CONTEXT_CLIENT_VERSION, 2,
				EGL10.EGL_NONE
		};
		eglContext = egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
	}

	public synchronized void bind(Graphics graphics) {
		Canvas canvas = graphics.getCanvas();
		int width = canvas.getWidth();
		int height = canvas.getHeight();
		if (eglContext == null) init();
		targetBitmap = graphics.getBitmap();
		EGL10 egl = (EGL10) EGLContext.getEGL();
		if (params.width != width || params.height != height) {

			if (eglWindowSurface != null) {
				releaseEglContext();
				egl.eglDestroySurface(eglDisplay, eglWindowSurface);
			}

			int[] surface_attribs = {
					EGL10.EGL_WIDTH, width,
					EGL10.EGL_HEIGHT, height,
					EGL10.EGL_NONE};
			eglWindowSurface = egl.eglCreatePbufferSurface(eglDisplay, eglConfig, surface_attribs);
			egl.eglMakeCurrent(eglDisplay, eglWindowSurface, eglWindowSurface, eglContext);

			glViewport(0, 0, width, height);
			Program.create();
			params.width = width;
			params.height = height;
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
		if (l == 0 && t == 0 && r == params.width && b == params.height) {
			glDisable(GL_SCISSOR_TEST);
		} else {
			glEnable(GL_SCISSOR_TEST);
			glScissor(l, t, r - l, b - t);
		}
		glClear(GL_DEPTH_BUFFER_BIT);
		backCopied = false;
		egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
	}

	public synchronized void bind(TextureImpl tex) {
		targetTexture = tex;
		int width = tex.getWidth();
		int height = tex.getHeight();
		if (eglContext == null) init();
		EGL10 egl = (EGL10) EGLContext.getEGL();
		if (params.width != width || params.height != height) {

			if (eglWindowSurface != null) {
				releaseEglContext();
				egl.eglDestroySurface(eglDisplay, eglWindowSurface);
			}

			int[] surface_attribs = {
					EGL10.EGL_WIDTH, width,
					EGL10.EGL_HEIGHT, height,
					EGL10.EGL_NONE};
			eglWindowSurface = egl.eglCreatePbufferSurface(eglDisplay, eglConfig, surface_attribs);
			egl.eglMakeCurrent(eglDisplay, eglWindowSurface, eglWindowSurface, eglContext);

			glViewport(0, 0, width, height);
			Program.create();
			params.width = width;
			params.height = height;
		}
		egl.eglMakeCurrent(eglDisplay, eglWindowSurface, eglWindowSurface, eglContext);
		Rect clip = this.clip;
		clip.set(0, 0, width, height);
		int l = clip.left;
		int t = clip.top;
		int r = clip.right;
		int b = clip.bottom;
		gClip.set(l, t, r, b);
		if (l == 0 && t == 0 && r == params.width && b == params.height) {
			glDisable(GL_SCISSOR_TEST);
		} else {
			glEnable(GL_SCISSOR_TEST);
			glScissor(l, t, r - l, b - t);
		}
		glClearColor(
				((clearColor >> 16) & 0xff) / 255.0f,
				((clearColor >> 8) & 0xff) / 255.0f,
				(clearColor & 0xff) / 255.0f,
				1.0f);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
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
		if (targetBitmap == null) {// render to texture
			return;
		}
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
		GLUtils.texImage2D(GL_TEXTURE_2D, 0, targetBitmap, 0);
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
				targetBitmap.setHasAlpha(true);
				Canvas canvas = new Canvas(targetBitmap);
				canvas.clipRect(gClip);
				canvas.drawColor(0, PorterDuff.Mode.SRC);
			}
			backCopied = true;
		} else {
			targetBitmap.setHasAlpha(false);
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

	void renderFigure(FigureNode node) {
		boolean isTransparency = (node.attrs & ENV_ATTR_SEMI_TRANSPARENT) != 0;
		if (!isTransparency && flushStep == 2) return;

		Model model = node.figure.model;
		if (!model.hasPolyT && !model.hasPolyC)
			return;

		glEnable(GL_DEPTH_TEST);
		glDepthMask(flushStep == 1);
		MathUtil.multiplyMM(MVP_TMP, node.projMatrix, node.viewMatrix);
		if (bufHandles == null) {
			bufHandles = ByteBuffer.allocateDirect(4 * 3).order(ByteOrder.nativeOrder()).asIntBuffer();
			glGenBuffers(3, bufHandles);
		}
		node.vertices.rewind();
		try {
			glBindBuffer(GL_ARRAY_BUFFER, bufHandles.get(0));
			glBufferData(GL_ARRAY_BUFFER, node.vertices.capacity() * 4, node.vertices, GL_STREAM_DRAW);
			ByteBuffer tcBuf = model.texCoordArray;
			tcBuf.rewind();
			glBindBuffer(GL_ARRAY_BUFFER, bufHandles.get(1));
			glBufferData(GL_ARRAY_BUFFER, tcBuf.capacity(), tcBuf, GL_STREAM_DRAW);

			boolean isLight = node.light != null && node.normals != null;
			if (isLight) {
				node.normals.rewind();
				glBindBuffer(GL_ARRAY_BUFFER, bufHandles.get(2));
				glBufferData(GL_ARRAY_BUFFER, node.normals.capacity() * 4, node.normals, GL_STREAM_DRAW);
			}
			TextureImpl sphere = node.specular;
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
					program.setToonShading(node.attrs, node.toonThreshold, node.toonHigh, node.toonLow);
					program.setLight(node.light);
					program.setSphere(sphere);
				} else {
					glDisableVertexAttribArray(program.aNormal);
					program.setLight(null);
				}

				program.bindMatrices(MVP_TMP, node.viewMatrix);
				// Draw triangles
				renderModel(node.textures, model, isTransparency);
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
					program.setLight(node.light);
					program.setSphere(sphere);
					program.setToonShading(node.attrs, node.toonThreshold, node.toonHigh, node.toonLow);
				} else {
					glDisableVertexAttribArray(program.aNormal);
					program.setLight(null);
				}
				program.bindMatrices(MVP_TMP, node.viewMatrix);
				renderModel(model, isTransparency);
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
		if (targetTexture != null) {
			glReadPixels(0, 0, 256, 256, GL_RGBA, GL_UNSIGNED_BYTE, targetTexture.image.getRaster());
			targetTexture = null;
		} else if (targetBitmap != null) {
			if (postCopy2D) {
				copy2d(false);
			}
			Rect clip = this.gClip;
			Utils.glReadPixels(clip.left, clip.top, clip.width(), clip.height(), targetBitmap);
			targetBitmap = null;
		}
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

	private void renderMeshC(RenderNode.PrimitiveNode node) {
		int command = node.command;
		Program.Color program = Program.color;
		program.use();
		if ((node.attrs & ENV_ATTR_LIGHTING) != 0 && node.normals != null && (command & PATTR_LIGHTING) != 0) {
			TextureImpl sphere = node.specular;
			if ((node.attrs & ENV_ATTR_SPHERE_MAP) != 0 && (command & PATTR_SPHERE_MAP) != 0 && sphere != null) {
				glVertexAttrib2f(program.aMaterial, 1, 1);
				program.setSphere(sphere);
			} else {
				glVertexAttrib2f(program.aMaterial, 1, 0);
				program.setSphere(null);
			}
			program.setLight(node.light);
			program.setToonShading(node.attrs, node.toonThreshold, node.toonHigh, node.toonLow);

			glVertexAttribPointer(program.aNormal, 3, GL_FLOAT, false, 3 * 4, node.normals.rewind());
			glEnableVertexAttribArray(program.aNormal);
		} else {
			glVertexAttrib2f(program.aMaterial, 0, 0);
			program.setLight(null);
			glDisableVertexAttribArray(program.aNormal);
		}

		program.bindMatrices(MVP_TMP, node.viewMatrix);

		glVertexAttribPointer(program.aPosition, 3, GL_FLOAT, false, 3 * 4, node.vertices.rewind());
		glEnableVertexAttribArray(program.aPosition);

		if ((command & PDATA_COLOR_PER_COMMAND) != 0) {
			program.setColor(node.colors);
		} else {
			glVertexAttribPointer(program.aColorData, 3, GL_UNSIGNED_BYTE, true, 3, node.colors.rewind());
			glEnableVertexAttribArray(program.aColorData);
		}

		glDrawArrays(GL_TRIANGLES, 0, node.vertices.capacity() / 3);
		glDisableVertexAttribArray(program.aPosition);
		glDisableVertexAttribArray(program.aColorData);
		glDisableVertexAttribArray(program.aNormal);
		checkGlError("renderMeshC");
	}

	private void renderMeshT(RenderNode.PrimitiveNode node) {
		int command = node.command;
		Program.Tex program = Program.tex;
		program.use();
		if ((node.attrs & ENV_ATTR_LIGHTING) != 0 && node.normals != null && (command & PATTR_LIGHTING) != 0) {
			TextureImpl sphere = node.specular;
			if ((node.attrs & ENV_ATTR_SPHERE_MAP) != 0 && (command & PATTR_SPHERE_MAP) != 0 && sphere != null) {
				glVertexAttrib3f(program.aMaterial, 1, 1, command & PATTR_COLORKEY);
				program.setSphere(sphere);
			} else {
				glVertexAttrib3f(program.aMaterial, 1, 0, command & PATTR_COLORKEY);
				program.setSphere(null);
			}
			program.setLight(node.light);
			program.setToonShading(node.attrs, node.toonThreshold, node.toonHigh, node.toonLow);

			glVertexAttribPointer(program.aNormal, 3, GL_FLOAT, false, 3 * 4, node.normals.rewind());
			glEnableVertexAttribArray(program.aNormal);
		} else {
			glVertexAttrib3f(program.aMaterial, 0, 0, command & PATTR_COLORKEY);
			program.setLight(null);
			glDisableVertexAttribArray(program.aNormal);
		}

		program.bindMatrices(MVP_TMP, node.viewMatrix);

		glVertexAttribPointer(program.aPosition, 3, GL_FLOAT, false, 3 * 4, node.vertices.rewind());
		glEnableVertexAttribArray(program.aPosition);

		glVertexAttribPointer(program.aColorData, 2, GL_UNSIGNED_BYTE, false, 2, node.texCoords.rewind());
		glEnableVertexAttribArray(program.aColorData);

		program.setTex(node.texture);

		glDrawArrays(GL_TRIANGLES, 0, node.vertices.capacity() / 3);
		glDisableVertexAttribArray(program.aPosition);
		glDisableVertexAttribArray(program.aColorData);
		glDisableVertexAttribArray(program.aNormal);
		checkGlError("renderMeshT");
	}

	public void drawCommandList(int[] cmds) {
		if (COMMAND_LIST_VERSION_1_0 != cmds[0]) {
			throw new IllegalArgumentException("Unsupported command list version: " + cmds[0]);
		}
		for (int i = 1; i < cmds.length; ) {
			int cmd = cmds[i++];
			switch (cmd & 0xFF000000) {
				case COMMAND_AFFINE_INDEX:
					selectAffineTrans(cmd & 0xFFFFFF);
					break;
				case COMMAND_AMBIENT_LIGHT: {
					params.light.ambIntensity = i++;
					break;
				}
				case COMMAND_ATTRIBUTE:
					params.attrs = cmd & 0xFFFFFF;
					break;
				case COMMAND_CENTER:
					setCenter(cmds[i++], cmds[i++]);
					break;
				case COMMAND_CLIP:
					clip.intersect(cmds[i++], cmds[i++], cmds[i++], cmds[i++]);
					updateClip();
					break;
				case COMMAND_DIRECTION_LIGHT: {
					params.light.x = i++;
					params.light.y = i++;
					params.light.z = i++;
					params.light.dirIntensity = i++;
					break;
				}
				case COMMAND_FLUSH:
					flush();
					break;
				case COMMAND_NOP:
					i += cmd & 0xFFFFFF;
					break;
				case COMMAND_PARALLEL_SCALE:
					setOrthographicScale(cmds[i++], cmds[i++]);
					break;
				case COMMAND_PARALLEL_SIZE:
					setOrthographicWH(cmds[i++], cmds[i++]);
					break;
				case COMMAND_PERSPECTIVE_FOV:
					setPerspectiveFov(cmds[i++], cmds[i++], cmds[i++]);
					break;
				case COMMAND_PERSPECTIVE_WH:
					setPerspectiveWH(cmds[i++], cmds[i++], cmds[i++], cmds[i++]);
					break;
				case COMMAND_TEXTURE_INDEX:
					int tid = cmd & 0xFFFFFF;
					if (tid > 0 && tid < 16) {
						params.textureIdx = tid;
					}
					break;
				case COMMAND_THRESHOLD:
					setToonParam(cmds[i++], cmds[i++], cmds[i++]);
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
					int vo = i;
					i += len;
					int no = i;
					if (type == PRIMITVE_TRIANGLES || type == PRIMITVE_QUADS) {
						switch (cmd & PDATA_NORMAL_PER_VERTEX) {
							case PDATA_NORMAL_PER_FACE:
								i += num * 3;
								break;
							case PDATA_NORMAL_PER_VERTEX:
								i += len;
								break;
						}
					} else if ((cmd & PDATA_NORMAL_PER_VERTEX) != 0) {
						throw new IllegalArgumentException();
					}
					int to = i;
					if (type == PRIMITVE_POINT_SPRITES) {
						switch (cmd & PDATA_POINT_SPRITE_PARAMS_PER_VERTEX) {
							case PDATA_POINT_SPRITE_PARAMS_PER_CMD:
								i += 8;
								break;
							case PDATA_POINT_SPRITE_PARAMS_PER_FACE:
							case PDATA_POINT_SPRITE_PARAMS_PER_VERTEX:
								i += num * 8;
								break;
						}
					} else if ((cmd & PDATA_TEXURE_COORD) == PDATA_TEXURE_COORD) {
						i += num * 2 * sizeOf;
					}

					int co = i;
					switch (cmd & (PDATA_COLOR_PER_COMMAND | PDATA_COLOR_PER_FACE)) {
						case PDATA_COLOR_PER_COMMAND:
							i++;
							break;
						case PDATA_COLOR_PER_FACE:
							i += num;
							break;
						case (PDATA_COLOR_PER_COMMAND | PDATA_COLOR_PER_FACE):
							if (type != PRIMITVE_POINT_SPRITES) {
								i += num * sizeOf;
							}
							break;
					}
					postPrimitives(cmd, cmds, vo, cmds, no, cmds, to, cmds, co);
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
		if (l == 0 && t == 0 && r == params.width && b == params.height) {
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

	public synchronized void postFigure(FigureImpl figure) {
		FigureNode rn;
		if (figure.stack.empty()) {
			rn = new FigureNode(this, figure);
		} else {
			rn = figure.stack.pop();
			rn.setData(this);
		}
		stack.add(rn);
	}

	public synchronized void postPrimitives(int command,
											int[] vertices, int vo,
											int[] normals, int no,
											int[] textureCoords, int to,
											int[] colors, int co) {
		int numPrimitives = command >> 16 & 0xff;
		FloatBuffer vcBuf = null;
		FloatBuffer ncBuf = null;
		ByteBuffer tcBuf = null;
		ByteBuffer colorBuf = null;
		switch ((command & 0x7000000)) {
			case PRIMITVE_POINTS: {
				int vcLen = numPrimitives * 3;
				vcBuf = ByteBuffer.allocateDirect(vcLen * 4)
						.order(ByteOrder.nativeOrder()).asFloatBuffer();
				for (int i = 0; i < vcLen; i++) {
					vcBuf.put(vertices[vo++]);
				}

				if ((command & PDATA_COLOR_PER_COMMAND) != 0) {
					colorBuf = ByteBuffer.allocateDirect(3).order(ByteOrder.nativeOrder());
					int color = colors[co];
					colorBuf.put((byte) (color >> 16 & 0xFF));
					colorBuf.put((byte) (color >> 8 & 0xFF));
					colorBuf.put((byte) (color & 0xFF));
				} else if ((command & PDATA_COLOR_PER_FACE) != 0) {
					colorBuf = ByteBuffer.allocateDirect(vcLen).order(ByteOrder.nativeOrder());
					for (int i = 0; i < numPrimitives; i++) {
						int color = colors[co++];
						colorBuf.put((byte) (color >> 16 & 0xFF));
						colorBuf.put((byte) (color >> 8 & 0xFF));
						colorBuf.put((byte) (color & 0xFF));
					}
				} else {
					return;
				}
				break;
			}
			case PRIMITVE_LINES: {
				int vcLen = numPrimitives * 2 * 3;
				vcBuf = ByteBuffer.allocateDirect(vcLen * 4).order(ByteOrder.nativeOrder())
						.asFloatBuffer();
				for (int i = 0; i < vcLen; i++) {
					vcBuf.put(vertices[vo++]);
				}

				if ((command & PDATA_COLOR_PER_COMMAND) != 0) {
					colorBuf = ByteBuffer.allocateDirect(3).order(ByteOrder.nativeOrder());
					int color = colors[co];
					colorBuf.put((byte) (color >> 16 & 0xFF));
					colorBuf.put((byte) (color >> 8 & 0xFF));
					colorBuf.put((byte) (color & 0xFF));
				} else if ((command & PDATA_COLOR_PER_FACE) != 0) {
					colorBuf = ByteBuffer.allocateDirect(vcLen).order(ByteOrder.nativeOrder());
					for (int i = 0; i < numPrimitives; i++) {
						int color = colors[co++];
						byte r = (byte) (color >> 16 & 0xFF);
						byte g = (byte) (color >> 8 & 0xFF);
						byte b = (byte) (color & 0xFF);
						colorBuf.put(r).put(g).put(b);
						colorBuf.put(r).put(g).put(b);
					}
				} else {
					return;
				}
				break;
			}
			case PRIMITVE_TRIANGLES: {
				int vcLen = numPrimitives * 3 * 3;
				vcBuf = ByteBuffer.allocateDirect(vcLen * 4)
						.order(ByteOrder.nativeOrder()).asFloatBuffer();
				for (int i = 0; i < vcLen; i++) {
					vcBuf.put(vertices[vo++]);
				}
				switch (command & PDATA_NORMAL_PER_VERTEX) {
					case PDATA_NORMAL_PER_FACE:
						ncBuf = ByteBuffer.allocateDirect(vcLen * 4)
								.order(ByteOrder.nativeOrder()).asFloatBuffer();
						for (int end = no + numPrimitives * 3; no < end; ) {
							float x = normals[no++];
							float y = normals[no++];
							float z = normals[no++];
							ncBuf.put(x).put(y).put(z);
							ncBuf.put(x).put(y).put(z);
							ncBuf.put(x).put(y).put(z);
						}
						break;
					case PDATA_NORMAL_PER_VERTEX:
						ncBuf = ByteBuffer.allocateDirect(vcLen * 4)
								.order(ByteOrder.nativeOrder()).asFloatBuffer();
						for (int end = no + vcLen; no < end; ) {
							ncBuf.put(normals[no++]);
						}
						break;
					default:
				}
				if ((command & PDATA_TEXURE_COORD) == PDATA_TEXURE_COORD) {
					if (params.getTexture() == null) {
						return;
					}
					int tcLen = numPrimitives * 3 * 2;
					tcBuf = ByteBuffer.allocateDirect(tcLen).order(ByteOrder.nativeOrder());
					for (int i = 0; i < tcLen; i++) {
						tcBuf.put((byte) textureCoords[to++]);
					}
				} else if ((command & PDATA_COLOR_PER_COMMAND) != 0) {
					colorBuf = ByteBuffer.allocateDirect(3).order(ByteOrder.nativeOrder());
					int color = colors[co];
					colorBuf.put((byte) (color >> 16 & 0xFF));
					colorBuf.put((byte) (color >> 8 & 0xFF));
					colorBuf.put((byte) (color & 0xFF));
				} else if ((command & PDATA_COLOR_PER_FACE) != 0) {
					colorBuf = ByteBuffer.allocateDirect(vcLen).order(ByteOrder.nativeOrder());
					for (int i = 0; i < numPrimitives; i++) {
						int color = colors[co++];
						byte r = (byte) (color >> 16 & 0xFF);
						byte g = (byte) (color >> 8 & 0xFF);
						byte b = (byte) (color & 0xFF);
						colorBuf.put(r).put(g).put(b);
						colorBuf.put(r).put(g).put(b);
						colorBuf.put(r).put(g).put(b);
					}
				}
				break;
			}
			case PRIMITVE_QUADS: {
				vcBuf = ByteBuffer.allocateDirect(numPrimitives * 6 * 3 * 4)
						.order(ByteOrder.nativeOrder()).asFloatBuffer();
				for (int i = 0; i < numPrimitives; i++) {
					int offset = vo + i * 4 * 3;
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
				switch (command & PDATA_NORMAL_PER_VERTEX) {
					case PDATA_NORMAL_PER_FACE:
						ncBuf = ByteBuffer.allocateDirect(numPrimitives * 6 * 3 * 4)
								.order(ByteOrder.nativeOrder()).asFloatBuffer();
						for (int end = no + numPrimitives * 3; no < end; ) {
							float x = normals[no++];
							float y = normals[no++];
							float z = normals[no++];
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
							int offset = no + i * 4 * 3;
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
				}
				if ((command & PDATA_TEXURE_COORD) == PDATA_TEXURE_COORD) {
					if (params.getTexture() == null) {
						return;
					}
					tcBuf = ByteBuffer.allocateDirect(numPrimitives * 6 * 2)
							.order(ByteOrder.nativeOrder());
					for (int i = 0; i < numPrimitives; i++) {
						int offset = to + i * 4 * 2;
						int pos = offset;
						tcBuf.put((byte) textureCoords[pos++]).put((byte) textureCoords[pos++]); // A
						tcBuf.put((byte) textureCoords[pos++]).put((byte) textureCoords[pos++]); // B
						tcBuf.put((byte) textureCoords[pos++]).put((byte) textureCoords[pos++]); // C
						tcBuf.put((byte) textureCoords[pos++]).put((byte) textureCoords[pos]);   // D
						pos = offset;
						tcBuf.put((byte) textureCoords[pos++]).put((byte) textureCoords[pos]);   // A
						pos = offset + 2 * 2;
						tcBuf.put((byte) textureCoords[pos++]).put((byte) textureCoords[pos]);   // C
					}
				} else if ((command & PDATA_COLOR_PER_COMMAND) != 0) {
					colorBuf = ByteBuffer.allocateDirect(3).order(ByteOrder.nativeOrder());
					int color = colors[co];
					colorBuf.put((byte) (color >> 16 & 0xFF));
					colorBuf.put((byte) (color >> 8 & 0xFF));
					colorBuf.put((byte) (color & 0xFF));
				} else if ((command & PDATA_COLOR_PER_FACE) != 0) {
					colorBuf = ByteBuffer.allocateDirect(numPrimitives * 6 * 3)
							.order(ByteOrder.nativeOrder());
					for (int i = 0; i < numPrimitives; i++) {
						int color = colors[co++];
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
				}
				break;
			}
			case PRIMITVE_POINT_SPRITES: {
				if (params.getTexture() == null) {
					return;
				}
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
						return;
				}

				float[] vert = new float[8];
				float[] quad = new float[4 * 6];

				vcBuf = ByteBuffer.allocateDirect(numPrimitives * 6 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
				tcBuf = ByteBuffer.allocateDirect(numPrimitives * 6 * 2).order(ByteOrder.nativeOrder());
				int texOffset = 0;
				MathUtil.multiplyMM(MVP_TMP, params.projMatrix, params.viewMatrix);
				for (int i = 0; i < numPrimitives; i++) {
					vert[4] = vertices[vo++];
					vert[5] = vertices[vo++];
					vert[6] = vertices[vo++];
					vert[7] = 1.0f;
					Matrix.multiplyMV(vert, 0, MVP_TMP, 0, vert, 4);

					if (numParams != 1) {
						texOffset = to + i * 8;
					}

					float width = textureCoords[texOffset];
					float height = textureCoords[texOffset + 1];
					int angle = textureCoords[texOffset + 2];
					float halfWidth;
					float halfHeight;
					switch (textureCoords[texOffset + 7]) {
						case POINT_SPRITE_LOCAL_SIZE | POINT_SPRITE_PERSPECTIVE:
							halfWidth = width * params.projMatrix[0] * 0.5f;
							halfHeight = height * params.projMatrix[5] * 0.5f;
							break;
						case POINT_SPRITE_PIXEL_SIZE | POINT_SPRITE_PERSPECTIVE:
							if (params.projection <= COMMAND_PARALLEL_SIZE) {
								halfWidth = width / params.width;
								halfHeight = height / params.height;
							} else {
								halfWidth = width / params.width * params.near;
								halfHeight = height / params.height * params.near;
							}
							break;
						case POINT_SPRITE_LOCAL_SIZE | POINT_SPRITE_NO_PERS:
							if (params.projection <= COMMAND_PARALLEL_SIZE) {
								halfWidth = width * params.projMatrix[0] * 0.5f;
								halfHeight = height * params.projMatrix[5] * 0.5f;
							} else {
								float near = params.near;
								halfWidth = width * params.projMatrix[0] / near * 0.5f * vert[3];
								halfHeight = height * params.projMatrix[5] / near * 0.5f * vert[3];
							}
							break;
						case POINT_SPRITE_PIXEL_SIZE | POINT_SPRITE_NO_PERS:
							halfWidth = width / params.width * vert[3];
							halfHeight = height / params.height * vert[3];
							break;
						default:
							throw new IllegalArgumentException();
					}
					Utils.getSpriteVertex(quad, vert, angle, halfWidth, halfHeight);
					vcBuf.put(quad);

					byte x0 = (byte) textureCoords[texOffset + 3];
					byte y0 = (byte) textureCoords[texOffset + 4];
					byte x1 = (byte) (textureCoords[texOffset + 5] - 1);
					byte y1 = (byte) (textureCoords[texOffset + 6] - 1);

					tcBuf.put(x0).put(y1);
					tcBuf.put(x0).put(y0);
					tcBuf.put(x1).put(y1);
					tcBuf.put(x1).put(y1);
					tcBuf.put(x0).put(y0);
					tcBuf.put(x1).put(y0);
				}
			}
		}
		stack.add(new RenderNode.PrimitiveNode(this, command, vcBuf, ncBuf, tcBuf, colorBuf));
	}

	public synchronized void drawFigure(FigureImpl figure) {
		bindEglContext();
		if (!backCopied && preCopy2D) copy2d(true);
		try {
			flushStep = 1;
			for (int i = 0, stackSize = stack.size(); i < stackSize; i++) {
				RenderNode r = stack.get(i);
				r.run();
			}
			Model data = figure.model;
			figure.prepareBuffers();
			renderFigureV2(data);
			flushStep = 2;
			for (int i = 0, stackSize = stack.size(); i < stackSize; i++) {
				RenderNode r = stack.get(i);
				r.run();
				r.recycle();
			}
			renderFigureV2(data);
			glDisable(GL_BLEND);
			glDepthMask(true);
			glClear(GL_DEPTH_BUFFER_BIT);
		} finally {
			releaseEglContext();
		}
	}

	public synchronized void drawFigureV2(FigureImpl figure) {
		bindEglContext();
		if (!backCopied && preCopy2D) copy2d(true);
		try {
			Model model = figure.model;
			figure.prepareBuffers();

			flushStep = 1;
			renderFigureV2(model);
			flushStep = 2;
			renderFigureV2(model);

			glDisable(GL_BLEND);
			glDepthMask(true);
			glClear(GL_DEPTH_BUFFER_BIT);
		} finally {
			releaseEglContext();
		}
	}

	private void bindEglContext() {
		((EGL10) EGLContext.getEGL()).eglMakeCurrent(eglDisplay, eglWindowSurface, eglWindowSurface, eglContext);
	}

	private void releaseEglContext() {
		((EGL10) EGLContext.getEGL()).eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
	}

	public void reset() {
		stack.clear();
	}

	public void setTexture(TextureImpl tex) {
		if (tex == null) {
			return;
		}
		params.textures[0] = tex;
		params.textureIdx = 0;
		params.texturesLen = 1;
	}

	public void setTextureArray(TextureImpl[] tex) {
		if (tex == null) {
			return;
		}
		int len = tex.length;
		System.arraycopy(tex, 0, params.textures, 0, len);
		params.texturesLen = len;
	}

	public float[] getViewMatrix() {
		return params.viewMatrix;
	}

	public void setLight(int ambIntensity, int dirIntensity, int x, int y, int z) {
		params.light.set(ambIntensity, dirIntensity, x, y, z);
	}

	public int getAttributes() {
		return params.attrs;
	}

	public void setToonParam(int tress, int high, int low) {
		params.toonThreshold = tress;
		params.toonHigh = high;
		params.toonLow = low;
	}

	public void setSphereTexture(TextureImpl tex) {
		if (tex != null) {
			params.specular = tex;
		}
	}

	public void setAttribute(int attrs) {
		params.attrs = attrs;
	}

	private void renderFigureV2(Model model) {
		boolean isTransparency = (params.attrs & ENV_ATTR_SEMI_TRANSPARENT) != 0;
		if (!isTransparency && flushStep == 2) return;

		if (!model.hasPolyT && !model.hasPolyC)
			return;

		glEnable(GL_DEPTH_TEST);
		glDepthMask(flushStep == 1);
		MathUtil.multiplyMM(MVP_TMP, params.projMatrix, params.viewMatrix);
		if (bufHandles == null) {
			bufHandles = ByteBuffer.allocateDirect(4 * 3).order(ByteOrder.nativeOrder()).asIntBuffer();
			glGenBuffers(3, bufHandles);
		}
		try {
			FloatBuffer vertices = model.vertexArray;
			vertices.rewind();
			glBindBuffer(GL_ARRAY_BUFFER, bufHandles.get(0));
			glBufferData(GL_ARRAY_BUFFER, vertices.capacity() * 4, vertices, GL_STREAM_DRAW);
			ByteBuffer tcBuf = model.texCoordArray;
			tcBuf.rewind();
			glBindBuffer(GL_ARRAY_BUFFER, bufHandles.get(1));
			glBufferData(GL_ARRAY_BUFFER, tcBuf.capacity(), tcBuf, GL_STREAM_DRAW);

			FloatBuffer normals = model.normalsArray;
			boolean isLight = (params.attrs & ENV_ATTR_LIGHTING) != 0 && normals != null;
			if (isLight) {
				normals.rewind();
				glBindBuffer(GL_ARRAY_BUFFER, bufHandles.get(2));
				glBufferData(GL_ARRAY_BUFFER, normals.capacity() * 4, normals, GL_STREAM_DRAW);
			}
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
					program.setToonShading(params.attrs, params.toonThreshold, params.toonHigh, params.toonLow);
					program.setLight(params.light);
					program.setSphere(params.specular);
				} else {
					glDisableVertexAttribArray(program.aNormal);
					program.setLight(null);
				}

				program.bindMatrices(MVP_TMP, params.viewMatrix);
				// Draw triangles
				renderModel(params.textures, model, isTransparency);
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
					program.setLight(params.light);
					program.setSphere(params.specular);
					program.setToonShading(params.attrs, params.toonThreshold, params.toonHigh, params.toonLow);
				} else {
					glDisableVertexAttribArray(program.aNormal);
					program.setLight(null);
				}
				program.bindMatrices(MVP_TMP, params.viewMatrix);
				renderModel(model, isTransparency);
				glDisableVertexAttribArray(program.aPosition);
				glDisableVertexAttribArray(program.aColorData);
				glDisableVertexAttribArray(program.aMaterial);
				glDisableVertexAttribArray(program.aNormal);
			}
		} finally {
			glBindBuffer(GL_ARRAY_BUFFER, 0);
		}
	}

	void renderPrimitive(RenderNode.PrimitiveNode node) {
		int command = node.command;
		int blend = (node.attrs & ENV_ATTR_SEMI_TRANSPARENT) != 0 ? (command & PATTR_BLEND_SUB) >> 4 : 0;
		if (blend != 0) {
			if (flushStep == 1) {
				return;
			}
		} else if (flushStep == 2) {
			return;
		}
		MathUtil.multiplyMM(MVP_TMP, node.projMatrix, node.viewMatrix);
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LESS);
		glDepthMask(flushStep == 1);
		glDisable(GL_CULL_FACE);
		applyBlending(blend);
		int numPrimitives = command >> 16 & 0xff;
		switch ((command & 0x7000000)) {
			case PRIMITVE_POINTS: {
				Program.Color program = Program.color;
				program.use();
				program.setLight(null);
				program.bindMatrices(MVP_TMP, node.viewMatrix);

				if ((command & PDATA_COLOR_PER_COMMAND) != 0) {
					program.setColor(node.colors);
				} else {
					glVertexAttribPointer(program.aColorData, 3, GL_UNSIGNED_BYTE, true, 3, node.colors.rewind());
					glEnableVertexAttribArray(program.aColorData);
				}
				glVertexAttribPointer(program.aPosition, 3, GL_FLOAT, false, 3 * 4, node.vertices.rewind());
				glEnableVertexAttribArray(program.aPosition);

				glDrawArrays(GL_POINTS, 0, numPrimitives);
				glDisableVertexAttribArray(program.aPosition);
				glDisableVertexAttribArray(program.aColorData);
				checkGlError("renderPrimitive[PRIMITVE_POINTS]");
				break;
			}
			case PRIMITVE_LINES: {
				Program.Color program = Program.color;
				program.use();
				program.setLight(null);
				program.bindMatrices(MVP_TMP, node.viewMatrix);

				if ((command & PDATA_COLOR_PER_COMMAND) != 0) {
					program.setColor(node.colors);
				} else {
					glVertexAttribPointer(program.aColorData, 3, GL_UNSIGNED_BYTE, true, 3, node.colors.rewind());
					glEnableVertexAttribArray(program.aColorData);
				}
				glVertexAttribPointer(program.aPosition, 3, GL_FLOAT, false, 3 * 4, node.vertices.rewind());
				glEnableVertexAttribArray(program.aPosition);

				glDrawArrays(GL_LINES, 0, numPrimitives * 2);
				glDisableVertexAttribArray(program.aPosition);
				glDisableVertexAttribArray(program.aColorData);
				checkGlError("renderPrimitive[PRIMITVE_LINES]");
				break;
			}
			case PRIMITVE_TRIANGLES:
			case PRIMITVE_QUADS: {
				if ((command & PDATA_TEXURE_COORD) == PDATA_TEXURE_COORD) {
					renderMeshT(node);
				} else if ((command & PDATA_COLOR_PER_COMMAND) != 0) {
					renderMeshC(node);
				} else if ((command & PDATA_COLOR_PER_FACE) != 0) {
					renderMeshC(node);
				}
				break;
			}
			case PRIMITVE_POINT_SPRITES: {
				Program.Sprite program = Program.sprite;
				program.use();

				glVertexAttribPointer(program.aPosition, 4, GL_FLOAT, false, 4 * 4, node.vertices.rewind());
				glEnableVertexAttribArray(program.aPosition);

				glVertexAttribPointer(program.aColorData, 2, GL_UNSIGNED_BYTE, false, 2, node.texCoords.rewind());
				glEnableVertexAttribArray(program.aColorData);

				program.setTexture(node.texture);

				glUniform1i(program.uIsTransparency, (command & PATTR_COLORKEY));
				glDrawArrays(GL_TRIANGLES, 0, numPrimitives * 6);
				glDisableVertexAttribArray(program.aPosition);
				glDisableVertexAttribArray(program.aColorData);
				checkGlError("renderPrimitive[PRIMITVE_POINT_SPRITES]");
			}
		}

	}

	public void setOrthographicScale(int scaleX, int scaleY) {
		params.projection = COMMAND_PARALLEL_SCALE;
		float vw = params.width;
		float vh = params.height;
		float w = vw * (4096.0f / scaleX);
		float h = vh * (4096.0f / scaleY);

		float sx = 2.0f / w;
		float sy = 2.0f / h;
		float sz = 1.0f / 65536.0f;
		float tx = 2.0f * params.centerX / vw - 1.0f;
		float ty = 2.0f * params.centerY / vh - 1.0f;
		float tz = 0.0f;

		float[] pm = params.projMatrix;
		pm[ 0] =   sx; pm[ 4] = 0.0f; pm[ 8] = 0.0f; pm[12] =   tx;
		pm[ 1] = 0.0f; pm[ 5] =   sy; pm[ 9] = 0.0f; pm[13] =   ty;
		pm[ 2] = 0.0f; pm[ 6] = 0.0f; pm[10] =   sz; pm[14] =   tz;
		pm[ 3] = 0.0f; pm[ 7] = 0.0f; pm[11] = 0.0f; pm[15] = 1.0f;
	}

	public void setOrthographicW(int w) {
		params.projection = COMMAND_PARALLEL_SIZE;
		float vw = params.width;
		float vh = params.height;
		float sx = 2.0f / w;
		float sy = sx * (vw / vh);
		float sz = 1.0f / 65536.0f;
		float tx = 2.0f * params.centerX / vw - 1.0f;
		float ty = 2.0f * params.centerY / vh - 1.0f;
		float tz = 0.0f;

		float[] pm = params.projMatrix;
		pm[0] =   sx; pm[4] = 0.0f; pm[ 8] = 0.0f; pm[12] =   tx;
		pm[1] = 0.0f; pm[5] =   sy; pm[ 9] = 0.0f; pm[13] =   ty;
		pm[2] = 0.0f; pm[6] = 0.0f; pm[10] =   sz; pm[14] =   tz;
		pm[3] = 0.0f; pm[7] = 0.0f; pm[11] = 0.0f; pm[15] = 1.0f;
	}

	public void setOrthographicWH(int w, int h) {
		params.projection = COMMAND_PARALLEL_SIZE;
		float sx = 2.0f / w;
		float sy = 2.0f / h;
		float sz = 1.0f / 65536.0f;
		float tx = 2.0f * params.centerX / params.width - 1.0f;
		float ty = 2.0f * params.centerY / params.height - 1.0f;
		float tz = 0.0f;

		float[] pm = params.projMatrix;
		pm[0] =   sx; pm[4] = 0.0f; pm[ 8] = 0.0f; pm[12] =   tx;
		pm[1] = 0.0f; pm[5] =   sy; pm[ 9] = 0.0f; pm[13] =   ty;
		pm[2] = 0.0f; pm[6] = 0.0f; pm[10] =   sz; pm[14] =   tz;
		pm[3] = 0.0f; pm[7] = 0.0f; pm[11] = 0.0f; pm[15] = 1.0f;
	}

	public void setPerspectiveFov(int near, int far, int angle) {
		params.projection = COMMAND_PERSPECTIVE_FOV;
		params.near = near;
		float rd = 1.0f / (near - far);
		float sx = 1.0f / (float) Math.tan(angle * TO_FLOAT * Math.PI);
		float vw = params.width;
		float vh = params.height;
		float sy = sx * (vw / vh);
		float sz = -(far + near) * rd;
		float tx = 2.0f * params.centerX / vw - 1.0f;
		float ty = 2.0f * params.centerY / vh - 1.0f;
		float tz = 2.0f * far * near * rd;

		float[] pm = params.projMatrix;
		pm[0] =   sx; pm[4] = 0.0f; pm[ 8] =   tx; pm[12] = 0.0f;
		pm[1] = 0.0f; pm[5] =   sy; pm[ 9] =   ty; pm[13] = 0.0f;
		pm[2] = 0.0f; pm[6] = 0.0f; pm[10] =   sz; pm[14] =   tz;
		pm[3] = 0.0f; pm[7] = 0.0f; pm[11] = 1.0f; pm[15] = 0.0f;
	}

	public void setPerspectiveW(int near, int far, int w) {
		params.projection = COMMAND_PERSPECTIVE_WH;
		params.near = near;
		float vw = params.width;
		float vh = params.height;

		float rd = 1.0f / (near - far);
		float sx = 2.0f * near / (w * TO_FLOAT);
		float sy = sx * (vw / vh);
		float sz = -(near + far) * rd;
		float tx = 2.0f * params.centerX / vw - 1.0f;
		float ty = 2.0f * params.centerY / vh - 1.0f;
		float tz = 2.0f * far * near * rd;

		float[] pm = params.projMatrix;
		pm[0] =   sx; pm[4] = 0.0f; pm[ 8] =   tx; pm[12] = 0.0f;
		pm[1] = 0.0f; pm[5] =   sy; pm[ 9] =   ty; pm[13] = 0.0f;
		pm[2] = 0.0f; pm[6] = 0.0f; pm[10] =   sz; pm[14] =   tz;
		pm[3] = 0.0f; pm[7] = 0.0f; pm[11] = 1.0f; pm[15] = 0.0f;
	}

	public void setPerspectiveWH(float near, float far, int w, int h) {
		params.projection = COMMAND_PERSPECTIVE_WH;
		params.near = near;
		float width = w * TO_FLOAT;
		float height = h * TO_FLOAT;

		float rd = 1.0f / (near - far);
		float sx = 2.0f * near / width;
		float sy = 2.0f * near / height;
		float sz = -(near + far) * rd;
		float tx = 2.0f * params.centerX / params.width - 1.0f;
		float ty = 2.0f * params.centerY / params.height - 1.0f;
		float tz = 2.0f * far * near * rd;

		float[] pm = params.projMatrix;
		pm[0] =   sx; pm[4] = 0.0f; pm[ 8] =   tx; pm[12] = 0.0f;
		pm[1] = 0.0f; pm[5] =   sy; pm[ 9] =   ty; pm[13] = 0.0f;
		pm[2] = 0.0f; pm[6] = 0.0f; pm[10] =   sz; pm[14] =   tz;
		pm[3] = 0.0f; pm[7] = 0.0f; pm[11] = 1.0f; pm[15] = 0.0f;
	}

	public void setViewTransArray(float[] matrices) {
		params.matrices = matrices;
	}

	private void selectAffineTrans(int n) {
		float[] matrices = params.matrices;
		if (matrices != null && matrices.length >= (n + 1) * 12) {
			System.arraycopy(matrices, n * 12, params.viewMatrix, 0, 12);
		}
	}

	public void setCenter(int cx, int cy) {
		params.centerX = cx;
		params.centerY = cy;
	}

	public void setClearColor(int color) {
		clearColor = color;
	}

	static class Params {
		final TextureImpl[] textures = new TextureImpl[16];
		final Light light = new Light();
		final float[] viewMatrix = new float[12];
		final float[] projMatrix = new float[16];

		int projection;
		float near;
		float[] matrices;
		int centerX;
		int centerY;
		int width;
		int height;
		int toonThreshold;
		int toonHigh;
		int toonLow;
		int attrs;
		int textureIdx;
		int texturesLen;
		TextureImpl specular;

		Params() {}

		TextureImpl getTexture() {
			if (textureIdx < 0 || textureIdx >= texturesLen) {
				return null;
			}
			return textures[textureIdx];
		}
	}

	private static final class InstanceHolder {
		static final Render instance = new Render();
	}
}
