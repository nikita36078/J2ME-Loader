package javax.microedition.m3g.render;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.opengl.GLES20;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.lcdui.Graphics;
import javax.microedition.m3g.Appearance;
import javax.microedition.m3g.Background;
import javax.microedition.m3g.Color;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.m3g.Image2D;
import javax.microedition.m3g.IndexBuffer;
import javax.microedition.m3g.Sprite3D;
import javax.microedition.m3g.Transform;
import javax.microedition.m3g.VertexArray;
import javax.microedition.m3g.VertexBuffer;

public class Renderer {

	private int maxTextureUnits = 1;
	private int maxTextureSize;

	private int maxViewportWidth = 0;
	private int maxViewportHeight = 0;

	private EGL10 egl;
	private EGLConfig eglConfig;
	private EGLDisplay eglDisplay;
	private EGLSurface eglWindowSurface;
	private EGLContext eglContext;
	private GL10 gl = null;

	private Background defaultBackground = new Background();
	private Object renderTarget;

	public Renderer() {
		initGLES();
	}

	private void initGLES() {
		// Create EGL context
		this.egl = (EGL10) EGLContext.getEGL();

		this.eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
		EGL_ASSERT(eglDisplay != EGL10.EGL_NO_DISPLAY);

		int[] major_minor = new int[2];
		EGL_ASSERT(egl.eglInitialize(eglDisplay, major_minor));

		int[] num_config = new int[1];
		int[] s_configAttribs = {
				EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
				EGL10.EGL_RED_SIZE, 8,
				EGL10.EGL_GREEN_SIZE, 8,
				EGL10.EGL_BLUE_SIZE, 8,
				EGL10.EGL_ALPHA_SIZE, 8,
				EGL10.EGL_DEPTH_SIZE, 8,
				EGL10.EGL_NONE};
		EGLConfig[] eglConfigs = new EGLConfig[1];
		EGL_ASSERT(egl.eglChooseConfig(eglDisplay, s_configAttribs, eglConfigs, 1, num_config));
		this.eglConfig = eglConfigs[0];

		this.eglContext = egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, null);
		EGL_ASSERT(eglContext != EGL10.EGL_NO_CONTEXT);

		int[] eglPbufferAttribs = {
				EGL10.EGL_WIDTH, 1,
				EGL10.EGL_HEIGHT, 1,
				EGL10.EGL_NONE};
		EGLSurface tmpSurface = egl.eglCreatePbufferSurface(eglDisplay, eglConfig, eglPbufferAttribs);
		EGL_ASSERT(tmpSurface != EGL10.EGL_NO_SURFACE);
		EGL_ASSERT(egl.eglMakeCurrent(eglDisplay, tmpSurface, tmpSurface, eglContext));

		this.gl = (GL10) eglContext.getGL();

		// Get parameters from the GL instance
		int[] params = new int[2];
		gl.glGetIntegerv(GL10.GL_MAX_TEXTURE_UNITS, params, 0);
		maxTextureUnits = params[0];
		gl.glGetIntegerv(GL10.GL_MAX_LIGHTS, params, 0);
		maxLights = params[0];
		lightFlags = new boolean[maxLights];
		gl.glGetIntegerv(GL10.GL_MAX_VIEWPORT_DIMS, params, 0);
		maxViewportWidth = params[0];
		maxViewportHeight = params[1];
		gl.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, params, 0);
		maxTextureSize = params[0];
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

		EGL_ASSERT(egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT));
		EGL_ASSERT(egl.eglDestroySurface(eglDisplay, tmpSurface));
	}

	private void populateProperties() {
		implementationProperties.put(PROPERTY_SUPPORT_ANTIALIASING, new Boolean(true));
		implementationProperties.put(PROPERTY_SUPPORT_TRUECOLOR, new Boolean(true));
		implementationProperties.put(PROPERTY_SUPPORT_DITHERING, new Boolean(false));
		implementationProperties.put(PROPERTY_SUPPORT_MIPMAPPING, new Boolean(false));
		implementationProperties.put(PROPERTY_SUPPORT_PERSPECTIVE_CORRECTION, new Boolean(true));
		implementationProperties.put(PROPERTY_SUPPORT_LOCAL_CAMERA_LIGHTING, new Boolean(false));
		implementationProperties.put(PROPERTY_MAX_LIGHTS, new Integer(maxLights));
		implementationProperties.put(PROPERTY_MAX_VIEWPORT_WIDTH, new Integer(maxViewportWidth));
		implementationProperties.put(PROPERTY_MAX_VIEWPORT_HEIGHT, new Integer(maxViewportHeight));
		implementationProperties.put(PROPERTY_MAX_VIEWPORT_DIMENSION, new Integer(Math.min(maxViewportWidth,
				maxViewportHeight)));
		implementationProperties.put(PROPERTY_MAX_TEXTURE_DIMENSION, new Integer(maxTextureSize));
		implementationProperties.put(PROPERTY_MAX_SPRITE_CROP_DIMENSION, new Integer(maxTextureSize));
		implementationProperties.put(PROPERTY_MAX_TRANSFORM_PER_VERTEX, new Integer(4));
		implementationProperties.put(PROPERTY_MAX_TEXTURE_UNITS, new Integer(maxTextureUnits));
	}

	public void bindTarget(Object target, boolean depthBuffer, int hints) {
		if (target == null)
			throw new NullPointerException("Rendering target must not be null");
		renderTarget = target;

		if (target instanceof Graphics) {
			Graphics graphics = (Graphics) target;
			Canvas canvas = graphics.getCanvas();
			Rect bounds = canvas.getClipBounds();
			width = canvas.getWidth();
			height = canvas.getHeight();
			setClipRect(bounds.left, bounds.top, bounds.width(), bounds.height());
			setViewport(bounds.left, bounds.top, bounds.width(), bounds.height());
		} else if (target instanceof Image2D) {
			Image2D image = (Image2D) target;
			width = image.getWidth();
			height = image.getHeight();
			setClipRect(0, 0, width, height);
			setViewport(0, 0, width, height);
		}

		// Create an offscreen surface
		int[] s_surfaceAttribs = {
				EGL10.EGL_WIDTH, width,
				EGL10.EGL_HEIGHT, height,
				EGL10.EGL_NONE};

		this.eglWindowSurface = egl.eglCreatePbufferSurface(eglDisplay, eglConfig, s_surfaceAttribs);
		EGL_ASSERT(this.eglWindowSurface != EGL10.EGL_NO_SURFACE);
		EGL_ASSERT(egl.eglMakeCurrent(eglDisplay, eglWindowSurface, eglWindowSurface, eglContext));
		this.gl = (GL10) eglContext.getGL();

		gl.glEnable(GL10.GL_SCISSOR_TEST);
		gl.glPixelStorei(GL10.GL_UNPACK_ALIGNMENT, 1);
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		gl.glDepthMask(true);
		gl.glColorMask(true, true, true, !overwrite);
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glClearDepthf(1.0f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		// Depth buffer
		depthBufferEnabled = depthBuffer;
		if (depthBuffer)
			gl.glEnable(GL10.GL_DEPTH_TEST);
		else
			gl.glDisable(GL10.GL_DEPTH_TEST);
		this.hints = hints;

		// Multisapling
		if ((hints & ANTIALIAS) != 0)
			gl.glEnable(GL10.GL_MULTISAMPLE);
		else
			gl.glDisable(GL10.GL_MULTISAMPLE);

		// Overwriting
		overwrite = ((hints & OVERWRITE) != 0);
	}

	public Object getTarget() {
		return renderTarget;
	}

	public void releaseTarget() {
		int b[] = new int[width * height];
		int bt[] = new int[width * height];
		IntBuffer ib = IntBuffer.wrap(b);
		ib.position(0);

		gl.glFinish();
		gl.glReadPixels(0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, ib);

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				int pix = b[i * width + j];
				int pb = (pix >>> 16) & 0xff;
				int pr = (pix << 16) & 0x00ff0000;
				int pix1 = (pix & 0xff00ff00) | pr | pb | (((pix & 0xff000000) == 0 && (pix & 0x00ffffff) == 0) ? 0 : 0xff000000);
				bt[(height - i - 1) * width + j] = pix1;
			}
		}

		if (renderTarget instanceof Graphics)
			((Graphics) renderTarget).drawRGB(bt, 0, width, 0, 0, width, height, true);
		else if (renderTarget instanceof Image2D) {
			ByteBuffer bb = ((Image2D) renderTarget).getPixels();
			if (bb == null)
				bb = ByteBuffer.allocateDirect(width * height * 4);
			bb.position(0);
			for (int i = 0; i < height; i++)
				for (int j = 0; j < width; j++) {
					int pix = bt[i * width + j];
					bb.put((byte) (pix >>> 24));
					bb.put((byte) ((pix >>> 16) & 0xFF));
					bb.put((byte) ((pix >>> 8) & 0xFF));
					bb.put((byte) (pix & 0xFF));
				}
		}

		for (int i = 0; i < Image2D.recycledTextures.size(); i++) {
			((Image2D) Image2D.recycledTextures.elementAt(i)).releaseTexture(gl);
			Image2D.recycledTextures.remove(i);
		}

		if (this.eglWindowSurface != null) {
			EGL_ASSERT(egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT));
			egl.eglDestroySurface(this.eglDisplay, this.eglWindowSurface);
		}
		renderTarget = null;
	}

	public void clear(Background background) {
		if (background != null)
			background.setupGL(gl);
		else {
			defaultBackground.setupGL(gl);
		}
	}

	public void initRender(boolean cameraHasChanged, boolean lightHasChanged,
						   boolean depthRangeHasChanged) {
		if (cameraHasChanged) {
			Transform t = new Transform();

			gl.glMatrixMode(GL10.GL_PROJECTION);
			camera.getProjection(t);
			t.setGL(gl);

			gl.glMatrixMode(GL10.GL_MODELVIEW);
			t.set(cameraTransform);
			t.setGL(gl);

			gl.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
			gl.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
		}

		if (lightHasChanged) {
			for (int i = 0; i < maxLights; i++) {
				if (lightFlags[i]) {
					Light light = (Light) lights.elementAt(i);
					Transform transform = (Transform) lightTransforms.elementAt(i);
					gl.glEnable(GL10.GL_LIGHT0 + i);
					gl.glPushMatrix();
					transform.multGL(gl);
					light.setupGL(gl, GL10.GL_LIGHT0 + i);
					gl.glPopMatrix();
				} else {
					gl.glDisable(GL10.GL_LIGHT0 + i);
				}
			}
		}

		if (depthRangeHasChanged) {
			gl.glDepthRangef(depthRangeNear, depthRangeFar);
		}
	}

	public void render(VertexBuffer vertices, IndexBuffer triangles, Appearance appearance, Transform transform) {
		// Appearance
		appearance.setupGL(gl);

		// Vertices
		float[] scaleBias = new float[4];
		VertexArray positions = vertices.getPositions(scaleBias);
		if (positions.getComponentType() == 1) {
			ByteBuffer pos = (ByteBuffer) positions.getBuffer();
			pos.position(0);
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glVertexPointer(positions.getComponentCount(), GL10.GL_BYTE, 4, pos);
		} else {
			ShortBuffer pos = (ShortBuffer) positions.getBuffer();
			pos.position(0);
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glVertexPointer(positions.getComponentCount(), GL10.GL_SHORT, positions.stride, pos);
		}

		// Normals
		VertexArray normals = vertices.getNormals();
		if (normals != null) {
			gl.glEnable(GL10.GL_NORMALIZE);
			if (normals.getComponentType() == 1) {
				ByteBuffer norm = (ByteBuffer) normals.getBuffer();
				norm.position(0);
				gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
				gl.glNormalPointer(GL10.GL_BYTE, 4, norm);
			} else {
				ShortBuffer norm = (ShortBuffer) normals.getBuffer();
				norm.position(0);
				gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
				gl.glNormalPointer(GL10.GL_SHORT, normals.stride, norm);
			}
		} else {
			gl.glDisable(GL10.GL_NORMALIZE);
			gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
		}

		// Colors
		VertexArray colors = vertices.getColors();
		if (colors != null) {
			Buffer buffer = colors.getBuffer();
			buffer.position(0);
			gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
			gl.glColorPointer(4, GL10.GL_UNSIGNED_BYTE, 4, buffer);
		} else {
			// Use default color as we don't have color per vertex
			Color color = new Color(vertices.getDefaultColor());
			float[] colorArray = color.toRGBAArray();
			gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
			gl.glColor4f(colorArray[0], colorArray[1], colorArray[2], colorArray[3]);
		}

		// Textures
		for (int i = 0; i < maxTextureUnits; ++i) {
			float[] texScaleBias = new float[4];
			VertexArray texcoords = vertices.getTexCoords(i, texScaleBias);
			gl.glClientActiveTexture(GL10.GL_TEXTURE0 + i);
			gl.glActiveTexture(GL10.GL_TEXTURE0 + i);
			if ((texcoords != null) && (appearance.getTexture(i) != null)) {
				// Enable the texture coordinate array
				gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

				// Activate the texture unit
				gl.glEnable(GL10.GL_TEXTURE_2D);
				appearance.getTexture(i).setupGL(gl, texScaleBias);

				// Set the texture coordinates
				if (texcoords.getComponentType() == 1) {
					ByteBuffer buffer = (ByteBuffer) texcoords.getBuffer();
					buffer.position(0);
					gl.glTexCoordPointer(texcoords.getComponentCount(), GL10.GL_BYTE, 4, buffer);
				} else {
					ShortBuffer buffer = (ShortBuffer) texcoords.getBuffer();
					buffer.position(0);
					gl.glTexCoordPointer(texcoords.getComponentCount(), GL10.GL_SHORT, texcoords.stride, buffer);
				}

			} else {
				gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
				gl.glDisable(GL10.GL_TEXTURE_2D);
			}
		}

		// Scene
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glPushMatrix();
		transform.multGL(gl);

		gl.glTranslatef(scaleBias[1], scaleBias[2], scaleBias[3]);
		gl.glScalef(scaleBias[0], scaleBias[0], scaleBias[0]);

		// Draw
		IntBuffer indices = triangles.getBuffer();
		indices.position(0);
		gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, triangles.getIndexCount(), GLES20.GL_UNSIGNED_INT, indices);

		gl.glPopMatrix();

		// Release textures
		for (int i = 0; i < maxTextureUnits; i++) {
			if (appearance.getTexture(i) != null) {
				gl.glActiveTexture(GL10.GL_TEXTURE0 + i);
				gl.glClientActiveTexture(GL10.GL_TEXTURE0 + i);
				gl.glBindTexture(GL10.GL_TEXTURE_2D, 0);
			}
		}
	}

	public void renderSprite(Sprite3D sprite, Transform transform, Graphics3D graphics3D) {
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glPushMatrix();
		transform.multGL(gl);

		sprite.render(gl, graphics3D);

		gl.glPopMatrix();
	}

	private static void EGL_ASSERT(boolean val) {
		if (!val) {
			System.out.println("EGL_ASSERT failed!");
			throw new IllegalStateException();
		}
	}

	public int getMaxViewportWidth() {
		return maxViewportWidth;
	}

	public int getMaxViewportHeight() {
		return maxViewportHeight;
	}

	public int getMaxTextureSize() {
		return maxTextureSize;
	}

	public int getMaxTextureUnits() {
		return maxTextureUnits;
	}

	public void disableTextureUnits() {
		for (int i = 0; i < maxTextureUnits; i++) {
			gl.glActiveTexture(GL10.GL_TEXTURE0 + i);
			gl.glDisable(GL10.GL_TEXTURE_2D);
		}
	}

	@Override
	protected void finalize() {
		// Release textures
		for (int i = 0; i < Image2D.recycledTextures.size(); i++) {
			((Image2D) Image2D.recycledTextures.elementAt(i)).releaseTexture(gl);
			Image2D.recycledTextures.remove(i);
		}

		// Destroy EGL
		egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
		if (eglWindowSurface != null)
			egl.eglDestroySurface(eglDisplay, eglWindowSurface);
		egl.eglDestroyContext(eglDisplay, eglContext);
		egl.eglTerminate(eglDisplay);
	}
}
