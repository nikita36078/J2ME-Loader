package com.mascotcapsule.micro3d.v3.impl;

import android.opengl.GLES20;

import com.mascotcapsule.micro3d.v3.Figure;
import com.mascotcapsule.micro3d.v3.FigureLayout;

import java.nio.FloatBuffer;

import static android.opengl.GLES20.*;

public class ObjectRenderer {

	// number of coordinates per vertex in this array
	private static final int COORDS_PER_VERTEX = 3;
	private static final int COLORS_PER_VERTEX = 4;
	private static final int TEX_COORDS_PER_VERTEX = 2;
	private static final int TEXTURE_STRIDE = (COORDS_PER_VERTEX + TEX_COORDS_PER_VERTEX) * 4;
	private static final int COLOR_STRIDE = (COORDS_PER_VERTEX + COLORS_PER_VERTEX) * 4;
	private final int colorProgram;
	private final int textureProgram;

	private int atPositionLocation;
	private int atTextureLocation;
	private int utTextureUnitLocation;
	private int utMatrixLocation;
	private int utOffsetLocation;

	private int acPositionLocation;
	private int ucMatrixLocation;
	private int ucOffsetLocation;
	private int acColorLocation;

	public ObjectRenderer() {
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LEQUAL);
		colorProgram = GLUtils.createProgram(Shader.colorVertex, Shader.colorFragment);
		textureProgram = GLUtils.createProgram(Shader.textureVertex, Shader.textureFragment);
		getColorLocations();
		getTextureLocations();
	}

	public void draw(Figure figure, FigureLayout layout) {
		float[] mvpMatrix = layout.getMatrix();
		FloatBuffer vertexData = figure.figure.vboPolyT;
		if (vertexData.capacity() > 0) {
			int[] texturedPolygons = figure.figure.texturedPolygons;
			int prevCount = 0;

			GLES20.glUseProgram(textureProgram);
			glUniformMatrix4fv(utMatrixLocation, 1, false, mvpMatrix, 0);
			GLUtils.checkGlError("glUniformMatrix4fv");
			// Vertex coords
			vertexData.position(0);
			GLES20.glVertexAttribPointer(atPositionLocation, COORDS_PER_VERTEX, GL_FLOAT,
					false, TEXTURE_STRIDE, vertexData);
			GLES20.glEnableVertexAttribArray(atPositionLocation);

			// Texture coords
			vertexData.position(COORDS_PER_VERTEX);
			GLES20.glVertexAttribPointer(atTextureLocation, TEX_COORDS_PER_VERTEX, GL_FLOAT,
					false, TEXTURE_STRIDE, vertexData);
			GLES20.glEnableVertexAttribArray(atTextureLocation);

			GLES20.glUniform2fv(utOffsetLocation, 1, layout.getCenterGL(), 0);
			// Put the texture to the unit 0 target
			GLES20.glActiveTexture(GL_TEXTURE0);
			// Texture units
			GLES20.glUniform1i(utTextureUnitLocation, 0);

			for (int i = 0; i < figure.getNumTextures() && i < texturedPolygons.length; i++) {
				int count = texturedPolygons[i] * 3;

				GLES20.glBindTexture(GL_TEXTURE_2D, figure.getTextureById(i).getId());
				// Draw the triangle
				GLES20.glDrawArrays(GLES20.GL_TRIANGLES, prevCount, count);
				GLUtils.checkGlError("glDrawArrays");
				prevCount += count;
			}
		}
		vertexData = figure.figure.vboPolyF;
		if (vertexData.capacity() > 0) {
			GLES20.glUseProgram(colorProgram);
			glUniformMatrix4fv(ucMatrixLocation, 1, false, mvpMatrix, 0);
			GLUtils.checkGlError("glUniformMatrix4fv");
			// Vertex coords
			vertexData.position(0);
			GLES20.glVertexAttribPointer(acPositionLocation, COORDS_PER_VERTEX, GL_FLOAT,
					false, COLOR_STRIDE, vertexData);
			GLES20.glEnableVertexAttribArray(acPositionLocation);

			// Color data
			vertexData.position(COORDS_PER_VERTEX);
			GLES20.glVertexAttribPointer(acColorLocation, COLORS_PER_VERTEX, GL_FLOAT,
					false, COLOR_STRIDE, vertexData);
			GLES20.glEnableVertexAttribArray(acColorLocation);

			GLES20.glUniform2fv(ucOffsetLocation, 1, layout.getCenterGL(), 0);

			int count = vertexData.capacity() / (COORDS_PER_VERTEX + COLORS_PER_VERTEX);
			// Draw the triangle
			GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count);
			GLUtils.checkGlError("glDrawArrays");
		}
	}

	private void getTextureLocations() {
		atPositionLocation = GLES20.glGetAttribLocation(textureProgram, "vPosition");
		atTextureLocation = GLES20.glGetAttribLocation(textureProgram, "aTexture");
		utTextureUnitLocation = GLES20.glGetUniformLocation(textureProgram, "uTextureUnit");
		utMatrixLocation = GLES20.glGetUniformLocation(textureProgram, "uMVPMatrix");
		utOffsetLocation = GLES20.glGetUniformLocation(textureProgram, "uTranslationOffset");
	}

	private void getColorLocations() {
		acPositionLocation = GLES20.glGetAttribLocation(colorProgram, "vPosition");
		acColorLocation = GLES20.glGetAttribLocation(colorProgram, "aColor");
		ucMatrixLocation = GLES20.glGetUniformLocation(colorProgram, "uMVPMatrix");
		ucOffsetLocation = GLES20.glGetUniformLocation(colorProgram, "uTranslationOffset");
	}
}
