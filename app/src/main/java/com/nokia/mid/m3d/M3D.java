/*
 *  Copyright 2023 Yury Kharchenko
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

package com.nokia.mid.m3d;

import android.graphics.Bitmap;
import android.graphics.Rect;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.lcdui.Graphics;

// TODO: 23.01.2023 not implemented check exceptions
public class M3D {
	public static final int DEPTH_TEST = GL10.GL_DEPTH_TEST;
	public static final int CULL_FACE = GL10.GL_CULL_FACE;
	public static final int BACK = GL10.GL_BACK;
	public static final int PROJECTION = GL10.GL_PROJECTION;
	public static final int TEXTURE_COORD_ARRAY = GL10.GL_TEXTURE_COORD_ARRAY;
	public static final int MODELVIEW = GL10.GL_MODELVIEW;
	public static final int VERTEX_ARRAY = GL10.GL_VERTEX_ARRAY;
	public static final int TEXTURE_2D = GL10.GL_TEXTURE_2D;
	public static final int LUMINANCE8 = 0x8040;
	public static final int COLOR_BUFFER_BIT = GL10.GL_COLOR_BUFFER_BIT;
	public static final int DEPTH_BUFFER_BIT = GL10.GL_DEPTH_BUFFER_BIT;
	public static final int TRIANGLES = GL10.GL_TRIANGLES;

	private final EGL10 egl;
	private final GL11 gl;
	private final EGLContext eglContext;
	private final EGLDisplay eglDisplay;
	private final EGLConfig eglConfig;
	private final Rect rect = new Rect();

	private EGLSurface eglWindowSurface;
	private Bitmap imageBuffer;
	private ByteBuffer pixelBuffer;
	private int width;
	private int height;
	private ByteBuffer vertexBuffer;
	private ByteBuffer indexBuffer;
	private ByteBuffer texCoordBuffer;

	public M3D() {
		egl = (EGL10) EGLContext.getEGL();
		eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
		egl.eglInitialize(eglDisplay, null);

		int[] configAttrs = {
				EGL10.EGL_RED_SIZE, 8,
				EGL10.EGL_GREEN_SIZE, 8,
				EGL10.EGL_BLUE_SIZE, 8,
				EGL10.EGL_ALPHA_SIZE, 8,
				EGL10.EGL_DEPTH_SIZE, 16,
				EGL10.EGL_STENCIL_SIZE, EGL10.EGL_DONT_CARE,
				EGL10.EGL_NONE
		};
		EGLConfig[] eglConfigs = new EGLConfig[1];
		egl.eglChooseConfig(eglDisplay, configAttrs, eglConfigs, 1, null);
		eglConfig = eglConfigs[0];

		eglContext = egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, null);
		this.gl = (GL11) eglContext.getGL();
	}

	public static M3D createInstance() {
		return new M3D();
	}

	// TODO: 22.01.2023 parameter 'flags' not interpreted
	public synchronized void setupBuffers(int flags, int width, int height) {
		if (width != this.width || height != this.height || eglWindowSurface == null) {
			this.width = width;
			this.height = height;
			imageBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			pixelBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());

			if (eglWindowSurface != null) {
				releaseEglContext();
				egl.eglDestroySurface(eglDisplay, eglWindowSurface);
			}

			int[] surface_attribs = {
					EGL10.EGL_WIDTH, width,
					EGL10.EGL_HEIGHT, height,
					EGL10.EGL_LARGEST_PBUFFER, 1,
					EGL10.EGL_NONE};
			eglWindowSurface = egl.eglCreatePbufferSurface(eglDisplay, eglConfig, surface_attribs);
		}
	}

	private void bindEglContext() {
		egl.eglMakeCurrent(eglDisplay, eglWindowSurface, eglWindowSurface, eglContext);
	}

	private void releaseEglContext() {
		egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
	}

	public synchronized void removeBuffers() {
		if (eglWindowSurface != null) {
			releaseEglContext();
			egl.eglDestroySurface(eglDisplay, eglWindowSurface);
			eglWindowSurface = null;
			imageBuffer = null;
			pixelBuffer = null;
		}
	}

	public synchronized void cullFace(int mode) {
		bindEglContext();
		// revert facing as part of workaround mirroring frame
		gl.glCullFace(mode == GL11.GL_BACK ? GL11.GL_FRONT : mode == GL11.GL_FRONT ? GL11.GL_BACK : mode);
		releaseEglContext();
	}

	public synchronized void viewport(int x, int y, int w, int h) {
		bindEglContext();
		gl.glViewport(x, y, w, h);
		releaseEglContext();
	}

	public synchronized void clear(int mask) {
		bindEglContext();
		gl.glClear(mask);
		releaseEglContext();
	}

	public synchronized void matrixMode(int mode) {
		bindEglContext();
		gl.glMatrixMode(mode);
		releaseEglContext();
	}

	public synchronized void loadIdentity() {
		bindEglContext();
		gl.glLoadIdentity();
		releaseEglContext();
	}

	public synchronized void frustumxi(int left, int right, int bottom, int top, int near, int far) {
		bindEglContext();
		// flip 'top' and 'bottom' as part of workaround mirroring frame
		gl.glFrustumx(left, right, top, bottom, near, far);
		releaseEglContext();
	}

	public synchronized void scalexi(int x, int y, int z) {
		bindEglContext();
		gl.glScalex(x, y, z);
		releaseEglContext();
	}

	public synchronized void translatexi(int x, int y, int z) {
		bindEglContext();
		gl.glTranslatex(x, y, z);
		releaseEglContext();
	}

	public synchronized void rotatexi(int angle, int x, int y, int z) {
		bindEglContext();
		gl.glRotatex(angle, x, y, z);
		releaseEglContext();
	}

	public synchronized void pushMatrix() {
		bindEglContext();
		gl.glPushMatrix();
		releaseEglContext();
	}

	public synchronized void popMatrix() {
		bindEglContext();
		gl.glPopMatrix();
		releaseEglContext();
	}

	public synchronized void color4ub(byte r, byte g, byte b, byte a) {
		bindEglContext();
		gl.glColor4ub(r, g, b, a);
		releaseEglContext();
	}

	public synchronized void clearColor4ub(byte r, byte g, byte b, byte a) {
		bindEglContext();
		gl.glClearColor((r & 0xff) / 255.0f, (g & 0xff) / 255.0f, (b & 0xff) / 255.0f, (a & 0xff) / 255.0f);
		releaseEglContext();
	}

	public synchronized void vertexPointerub(int size, int stride, byte[] vertices) {
		bindEglContext();
		vertexBuffer = getBuffer(vertexBuffer, vertices);
		gl.glVertexPointer(size, GL10.GL_BYTE, stride, vertexBuffer);
		releaseEglContext();
	}

	private static ByteBuffer getBuffer(ByteBuffer buffer, byte[] data) {
		if (buffer == null || buffer.capacity() < data.length) {
			buffer = ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());
		}

		buffer.rewind();
		buffer.put(data);
		buffer.rewind();
		return buffer;
	}

	public synchronized void drawElementsub(int mode, int count, byte[] indices) {
		bindEglContext();
		indexBuffer = getBuffer(indexBuffer, indices);
		gl.glDrawElements(mode, count, GL10.GL_UNSIGNED_BYTE, indexBuffer);
		releaseEglContext();
	}

	public synchronized void drawArrays(int mode, int first, int count) {
		bindEglContext();
		gl.glDrawArrays(mode, first, count);
		releaseEglContext();
	}

	public synchronized void bindTexture(int target, Texture texture) {
		bindEglContext();
		gl.glActiveTexture(GL10.GL_TEXTURE0);
		gl.glBindTexture(target, texture.glId(gl));
		releaseEglContext();
	}

	public synchronized void texCoordPointerub(int size, int stride, byte[] uvs) {
		bindEglContext();
		texCoordBuffer = getBuffer(texCoordBuffer, uvs);
		gl.glTexCoordPointer(size, GL10.GL_BYTE, stride, texCoordBuffer);
		releaseEglContext();
	}

	public synchronized void enableClientState(int array) {
		bindEglContext();
		gl.glEnableClientState(array);
		releaseEglContext();
	}

	public synchronized void disableClientState(int array) {
		bindEglContext();
		gl.glDisableClientState(array);
		releaseEglContext();
	}

	public synchronized void enable(int cap) {
		bindEglContext();
		gl.glEnable(cap);
		releaseEglContext();
	}

	public synchronized void disable(int cap) {
		bindEglContext();
		gl.glDisable(cap);
		releaseEglContext();
	}

	public synchronized void blit(Graphics g, int x, int y, int w, int h) {
		if (pixelBuffer == null || w <= 0 || h <= 0) {
			return;
		}
		bindEglContext();
		gl.glFinish();
		gl.glReadPixels(0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, pixelBuffer.rewind());
		imageBuffer.copyPixelsFromBuffer(pixelBuffer.rewind());
		rect.set(x, y, x + w, y + h);
		g.getCanvas().drawBitmap(imageBuffer, rect, rect, null);
		releaseEglContext();
	}
}
