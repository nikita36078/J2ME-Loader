package com.mascotcapsule.micro3d.v3.render;

import android.graphics.Canvas;
import android.opengl.GLES20;

import com.mascotcapsule.micro3d.v3.Figure;
import com.mascotcapsule.micro3d.v3.FigureLayout;
import com.mascotcapsule.micro3d.v3.Texture;
import com.mascotcapsule.micro3d.v3.figure.CanvasFigure;
import com.mascotcapsule.micro3d.v3.figure.DirectFigure;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.lcdui.Graphics;
import javax.microedition.util.ArrayStack;

public class Renderer {

	private EGL10 egl;
	private EGLDisplay eglDisplay;
	private EGLSurface eglWindowSurface;
	private EGLConfig eglConfig;
	private EGLContext eglContext;
	private int width, height;
	private int drawCount;

	private ObjectRenderer objectRenderer;
	private ArrayStack<DirectFigure> directFigurePool;
	private CanvasFigure canvasFigure;
	private ByteBuffer pixelBuf;
	private RenderQueue renderQueue;

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
				EGL10.EGL_DEPTH_SIZE, 16,
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

		this.renderQueue = new RenderQueue();
		this.directFigurePool = new ArrayStack<>();
		this.canvasFigure = new CanvasFigure();
	}

	public void bind(Graphics graphics, boolean targetChanged) {
		if (egl == null) init();
		if (targetChanged) {
			Canvas canvas = graphics.getCanvas();
			int width = canvas.getWidth();
			int height = canvas.getHeight();

			if (this.width != width || this.height != height) {
				this.width = width;
				this.height = height;
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

				GLES20.glEnable(GLES20.GL_DEPTH_TEST);
				GLES20.glDepthFunc(GLES20.GL_LEQUAL);
				// this projection matrix is applied to object coordinates
				GLES20.glViewport(0, 0, width, height);
				objectRenderer = new ObjectRenderer();
				pixelBuf = ByteBuffer.allocateDirect(width * height * 4);
				pixelBuf.order(ByteOrder.LITTLE_ENDIAN);
			}
		}
		// Draw background color
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		// Draw canvas
		canvasFigure.loadTexture(graphics.getBitmap());
		objectRenderer.draw(canvasFigure);
		flush();
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

	public void render(Texture texture, FigureLayout layout, int command,
					   int numPrimitives, int[] vertexCoords,
					   int[] textureCoords, int[] colors) {
		DirectFigure directFigure = directFigurePool.pop();
		if (directFigure == null) {
			directFigure = new DirectFigure();
		}
		directFigure.parse(texture, command, numPrimitives, vertexCoords, textureCoords, colors);
		renderQueue.add(directFigure, layout);
	}

	public void render(Figure figure, FigureLayout layout) {
		renderQueue.add(figure, layout);
	}

	public void release(Graphics graphics) {
		if (drawCount > 1) {
			pixelBuf.position(0);
			GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuf);
			pixelBuf.position(0);
			graphics.getBitmap().copyPixelsFromBuffer(pixelBuf);
		}
		drawCount = 0;
	}

	public void flush() {
		renderQueue.sort();
		for (RenderElement element : renderQueue.getQueue()) {
			objectRenderer.draw(element);
			if (element.getRenderable() instanceof DirectFigure) {
				directFigurePool.push((DirectFigure) element.getRenderable());
			}
		}
		renderQueue.clear();
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
		drawCount++;
	}

	public void dispose() {
		canvasFigure.dispose();
	}
}
