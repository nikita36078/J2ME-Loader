package com.mascotcapsule.micro3d.v3.impl;

import android.graphics.Canvas;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.mascotcapsule.micro3d.v3.Figure;
import com.mascotcapsule.micro3d.v3.FigureLayout;

import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class Renderer {

	private EGL10 egl;
	private EGLDisplay eglDisplay;
	private EGLSurface eglWindowSurface;
	private EGLConfig eglConfig;
	private EGLContext eglContext;
	private int width, height;

	private final float[] mvpMatrix = new float[16];
	private final float[] projectionMatrix = new float[16];
	private final float[] viewMatrix = new float[16];

	private final float[] modelMatrix = new float[16];

	private ObjectRenderer objectRenderer;

	private void init() {
		this.egl = (EGL10) EGLContext.getEGL();
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
				EGL10.EGL_DEPTH_SIZE, 8,
				EGL10.EGL_STENCIL_SIZE, EGL10.EGL_DONT_CARE,
				EGL10.EGL_NONE};
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

	public void bind(Graphics graphics, boolean targetChanged) {
		if (egl == null) init();
		if (targetChanged) {
			Canvas canvas = graphics.getCanvas();
			width = canvas.getWidth();
			height = canvas.getHeight();

			if (this.eglWindowSurface != null) {
				egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
				egl.eglDestroySurface(this.eglDisplay, this.eglWindowSurface);
			}

			int[] surface_attribs = {
					EGL10.EGL_WIDTH, width,
					EGL10.EGL_HEIGHT, height,
					EGL10.EGL_NONE};
			this.eglWindowSurface = egl.eglCreatePbufferSurface(eglDisplay, eglConfig, surface_attribs);
			egl.eglMakeCurrent(eglDisplay, eglWindowSurface, eglWindowSurface, eglContext);

			float ratio = (float) width / height;
			// this projection matrix is applied to object coordinates
			Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 2, 7);
			GLES20.glViewport(0, 0, width, height);
			objectRenderer = new ObjectRenderer();
		}
		// Draw background color
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
	}

	@Override
	protected void finalize() {
		// Destroy EGL
		egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
		if (eglWindowSurface != null)
			egl.eglDestroySurface(eglDisplay, eglWindowSurface);
		egl.eglDestroyContext(eglDisplay, eglContext);
		egl.eglTerminate(eglDisplay);
	}

	public void render() {
		// Set the camera position (View matrix)
		Matrix.setLookAtM(viewMatrix, 0, 2, 2, -6, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
		// Calculate the projection and view transformation
		Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
		// Create scale matrix
		Matrix.setIdentityM(modelMatrix, 0);
		float scaleFactor = 1f / 30;
		Matrix.scaleM(modelMatrix, 0, scaleFactor, scaleFactor, scaleFactor);
		// Calculate the model transformation
		Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
	}

	public void render(Figure figure, FigureLayout layout) {
		render();
		// Draw figure
		objectRenderer.draw(mvpMatrix, figure.figure);
	}

	public void release(Graphics graphics) {
		IntBuffer intBuffer = IntBuffer.allocate(width * height);
		GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, intBuffer);

		int[] intArrayO = intBuffer.array();
		int[] intArrayR = new int[width * height];
		for (int i = 0; i < height; i++) {
			if (width >= 0)
				System.arraycopy(intArrayO, i * width, intArrayR, (height - i - 1) * width, width);
		}

		Image image = Image.createImage(width, height);
		image.getBitmap().copyPixelsFromBuffer(IntBuffer.wrap(intArrayR));
		graphics.drawImage(image, 0, 0, 0);
	}
}
