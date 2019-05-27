package com.mascotcapsule.micro3d.v3.impl;

import android.opengl.GLES20;

public class ObjectRenderer {

	private final int program;

	// number of coordinates per vertex in this array
	private static final int COORDS_PER_VERTEX = 3;
	private static final int COLORS_PER_VERTEX = 4;

	public ObjectRenderer() {
		program = GLUtils.createProgram();
	}

	public void draw(float[] mvpMatrix, FigureImpl figure) {
		// Add program to OpenGL environment
		GLES20.glUseProgram(program);

		// get handle to vertex shader's vPosition member
		int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");

		// Enable a handle to the triangle vertices
		GLES20.glEnableVertexAttribArray(positionHandle);

		// Prepare the triangle coordinate data
		GLES20.glVertexAttribPointer(
				positionHandle, COORDS_PER_VERTEX,
				GLES20.GL_FLOAT, false,
				0, figure.triangleBuffer);

		//int mColorHandle = GLES20.glGetUniformLocation(program, "vColor");

		// Set color for drawing the triangle
		//GLES20.glUniform4fv(mColorHandle, 1, color, 0);

		int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
		GLUtils.checkGlError("glGetUniformLocation");

		// Apply the projection and view transformation
		GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
		GLUtils.checkGlError("glUniformMatrix4fv");

		int count = figure.triangleBuffer.capacity() / COORDS_PER_VERTEX;
		// Draw the triangle
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count);

		// Disable vertex array
		GLES20.glDisableVertexAttribArray(positionHandle);
	}

}
