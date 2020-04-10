package com.mascotcapsule.micro3d.v3.figure;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.*;

public class CanvasFigure {

	private static final float[] triangleFaces = {
			// X, Y, Z, U, V
			-1.0f, -1.0f, 0, 0.f, 0.f,
			1.0f, -1.0f, 0, 1.f, 0.f,
			-1.0f, 1.0f, 0, 0.f, 1.f,
			1.0f, 1.0f, 0, 1.f, 1.f,
	};

	private FloatBuffer vboPoly;
	private int glTexId;
	private float[] glMVPMatrix = new float[16];
	private float[] size = new float[]{1, 1};
	private float[] glCenter = new float[]{0, 0};

	public CanvasFigure() {
		ByteBuffer buff = ByteBuffer.allocateDirect(triangleFaces.length * 4);
		buff.order(ByteOrder.nativeOrder());
		vboPoly = buff.asFloatBuffer();
		vboPoly.put(triangleFaces);
		vboPoly.position(0);
		Matrix.setIdentityM(glMVPMatrix, 0);
	}

	public void loadTexture(Bitmap bitmap) {
		if (glTexId == 0) {
			final int[] textureIds = new int[1];
			glGenTextures(1, textureIds, 0);
			if (textureIds[0] == 0) {
				com.mascotcapsule.micro3d.v3.render.GLUtils.checkGlError("glGenTextures");
				glTexId = 0;
				return;
			}

			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, textureIds[0]);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

			GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);

			glBindTexture(GL_TEXTURE_2D, 0);

			glTexId = textureIds[0];
		} else {
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, glTexId);

			GLUtils.texSubImage2D(GL_TEXTURE_2D, 0, 0, 0, bitmap);

			glBindTexture(GL_TEXTURE_2D, 0);
		}
	}

	public final void dispose() {
		if (glTexId > 0) {
			GLES20.glDeleteTextures(1, new int[]{glTexId}, 0);
			glTexId = -1;
		}
	}

	public int getId() {
		if (glTexId == -1) throw new IllegalStateException("Already disposed!!!");
		return glTexId;
	}

	public float[] getMatrix() {
		return glMVPMatrix;
	}

	public FloatBuffer getVboPoly() {
		return vboPoly;
	}

	public float[] getSize() {
		return size;
	}

	public float[] getGlCenter() {
		return size;
	}
}
