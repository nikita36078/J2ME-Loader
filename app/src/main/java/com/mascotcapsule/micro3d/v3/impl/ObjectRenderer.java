package com.mascotcapsule.micro3d.v3.impl;

import android.opengl.GLES20;

import com.mascotcapsule.micro3d.v3.Figure;

import java.nio.FloatBuffer;

import static android.opengl.GLES20.*;

public class ObjectRenderer {

	// number of coordinates per vertex in this array
	private static final int COORDS_PER_VERTEX = 3;
	private static final int COLORS_PER_VERTEX = 4;
	private static final int TEX_COORDS_PER_VERTEX = 2;
	private static final int STRIDE = (COORDS_PER_VERTEX + TEX_COORDS_PER_VERTEX) * 4;
	private final int program;

	private int aPositionLocation;
	private int aTextureLocation;
	private int uTextureUnitLocation;
	private int uMatrixLocation;

	public ObjectRenderer() {
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LEQUAL);
		program = GLUtils.createProgram();
	}

	public void draw(Figure figure, float[] mvpMatrix) {
		// Add program to OpenGL environment
		GLES20.glUseProgram(program);
		getLocations();
		bindMatrix(mvpMatrix);


		// координаты вершин
		FloatBuffer vertexData = figure.figure.vboPolyT;
		vertexData.position(0);
		GLES20.glVertexAttribPointer(aPositionLocation, COORDS_PER_VERTEX, GL_FLOAT,
				false, STRIDE, vertexData);
		GLES20.glEnableVertexAttribArray(aPositionLocation);

		// координаты текстур
		vertexData.position(COORDS_PER_VERTEX);
		GLES20.glVertexAttribPointer(aTextureLocation, TEX_COORDS_PER_VERTEX, GL_FLOAT,
				false, STRIDE, vertexData);
		GLES20.glEnableVertexAttribArray(aTextureLocation);

		// помещаем текстуру в target 2D юнита 0
		GLES20.glActiveTexture(GL_TEXTURE0);
		GLES20.glBindTexture(GL_TEXTURE_2D, figure.getTexture().getId());

		// юнит текстуры
		GLES20.glUniform1i(uTextureUnitLocation, 0);

		vertexData.position(0);
		int count = vertexData.capacity() / (COORDS_PER_VERTEX + TEX_COORDS_PER_VERTEX);
		// Draw the triangle
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count);
		GLUtils.checkGlError("glDrawArrays");
	}

	private void getLocations() {
		aPositionLocation = GLES20.glGetAttribLocation(program, "vPosition");
		aTextureLocation = GLES20.glGetAttribLocation(program, "aTexture");
		uTextureUnitLocation = GLES20.glGetUniformLocation(program, "uTextureUnit");
		uMatrixLocation = GLES20.glGetUniformLocation(program, "uMVPMatrix");
	}


	private void bindMatrix(float[] mvpMatrix) {
		glUniformMatrix4fv(uMatrixLocation, 1, false, mvpMatrix, 0);
		GLUtils.checkGlError("glUniformMatrix4fv");
	}
}
