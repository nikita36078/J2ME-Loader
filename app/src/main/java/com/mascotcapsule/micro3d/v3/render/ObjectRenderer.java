package com.mascotcapsule.micro3d.v3.render;

import android.opengl.GLES20;

import com.mascotcapsule.micro3d.v3.Texture;
import com.mascotcapsule.micro3d.v3.figure.CanvasFigure;
import com.mascotcapsule.micro3d.v3.figure.Material;
import com.mascotcapsule.micro3d.v3.figure.Mesh;
import com.mascotcapsule.micro3d.v3.figure.Polygon;
import com.mascotcapsule.micro3d.v3.figure.Renderable;

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
	private int utTextureSizeLocation;

	private int acPositionLocation;
	private int ucMatrixLocation;
	private int ucOffsetLocation;
	private int acColorLocation;

	public ObjectRenderer() {
		colorProgram = GLUtils.createProgram(Shader.colorVertex, Shader.colorFragment);
		textureProgram = GLUtils.createProgram(Shader.textureVertex, Shader.textureFragment);
		getColorLocations();
		getTextureLocations();
	}

	public void draw(RenderElement element) {
		Renderable renderable = element.getRenderable();
		float[] mvpMatrix = element.getMatrix();
		float[] glCenter = element.getGlCenter();
		FloatBuffer vertexData = renderable.getVboPolyT();
		if (renderable.getNumPolyT() > 0) {
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

			GLES20.glUniform2fv(utOffsetLocation, 1, glCenter, 0);
			// Put the texture to the unit 0 target
			GLES20.glActiveTexture(GL_TEXTURE0);
			// Texture units
			GLES20.glUniform1i(utTextureUnitLocation, 0);

			for (int i = 0; i < renderable.getMeshesT().size(); i++) {
				Mesh mesh = renderable.getMeshesT().get(i);
				Material material = mesh.getMaterial();
				if (material.getBlendMode() == Polygon.BLENDING_MODE_ADD) {
					GLES20.glEnable(GL_BLEND);
					GLES20.glBlendEquation(GL_FUNC_ADD);
					GLES20.glBlendFunc(GL_SRC_ALPHA, GL_ONE);
				} else if (material.getBlendMode() == Polygon.BLENDING_MODE_SUB) {
					GLES20.glEnable(GL_BLEND);
					GLES20.glBlendEquation(GL_FUNC_REVERSE_SUBTRACT);
					GLES20.glBlendFuncSeparate(GL_ONE, GL_ONE, GL_ZERO, GL_ONE);
				} else if (material.isTransparent()) {
					GLES20.glEnable(GL_BLEND);
					GLES20.glBlendEquation(GL_FUNC_ADD);
					GLES20.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
				} else {
					GLES20.glDisable(GL_BLEND);
				}

				Texture texture = renderable.getTextureById(material.getTextureId());
				if (material.isTransparent()) {
					GLES20.glBindTexture(GL_TEXTURE_2D, texture.getTransparentId());
				} else {
					GLES20.glBindTexture(GL_TEXTURE_2D, texture.getId());
				}
				GLES20.glUniform2fv(utTextureSizeLocation, 1, texture.getSize(), 0);
				// Draw the triangle
				GLES20.glDrawArrays(GLES20.GL_TRIANGLES, mesh.getStart(), mesh.getCount());
				GLUtils.checkGlError("glDrawArrays");
			}
		}
		vertexData = renderable.getVboPolyF();
		if (renderable.getNumPolyF() > 0) {
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

			GLES20.glUniform2fv(ucOffsetLocation, 1, glCenter, 0);

			for (int i = 0; i < renderable.getMeshesF().size(); i++) {
				Mesh mesh = renderable.getMeshesF().get(i);
				Material material = mesh.getMaterial();
				if (material.getBlendMode() == Polygon.BLENDING_MODE_ADD) {
					GLES20.glEnable(GL_BLEND);
					GLES20.glBlendEquation(GL_FUNC_ADD);
					GLES20.glBlendFunc(GL_SRC_ALPHA, GL_ONE);
				} else if (material.getBlendMode() == Polygon.BLENDING_MODE_SUB) {
					GLES20.glEnable(GL_BLEND);
					GLES20.glBlendEquation(GL_FUNC_REVERSE_SUBTRACT);
					GLES20.glBlendFuncSeparate(GL_ONE, GL_ONE, GL_ZERO, GL_ONE);
				} else {
					GLES20.glDisable(GL_BLEND);
				}
				// Draw the triangle
				GLES20.glDrawArrays(GLES20.GL_TRIANGLES, mesh.getStart(), mesh.getCount());
				GLUtils.checkGlError("glDrawArrays");
			}
		}
	}

	public void draw(CanvasFigure figure) {
		float[] mvpMatrix = figure.getMatrix();
		FloatBuffer vertexData = figure.getVboPoly();

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
		GLES20.glUniform2fv(ucOffsetLocation, 1, figure.getGlCenter(), 0);

		// Put the texture to the unit 0 target
		GLES20.glActiveTexture(GL_TEXTURE0);
		// Texture units
		GLES20.glUniform1i(utTextureUnitLocation, 0);

		GLES20.glBindTexture(GL_TEXTURE_2D, figure.getId());
		GLES20.glUniform2fv(utTextureSizeLocation, 1, figure.getSize(), 0);
		// Draw the triangle
		GLES20.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		GLUtils.checkGlError("glDrawArrays");
	}

	private void getTextureLocations() {
		atPositionLocation = GLES20.glGetAttribLocation(textureProgram, "vPosition");
		atTextureLocation = GLES20.glGetAttribLocation(textureProgram, "aTexture");
		utTextureUnitLocation = GLES20.glGetUniformLocation(textureProgram, "uTextureUnit");
		utMatrixLocation = GLES20.glGetUniformLocation(textureProgram, "uMVPMatrix");
		utOffsetLocation = GLES20.glGetUniformLocation(textureProgram, "uTranslationOffset");
		utTextureSizeLocation = GLES20.glGetUniformLocation(textureProgram, "uTextureSize");
	}

	private void getColorLocations() {
		acPositionLocation = GLES20.glGetAttribLocation(colorProgram, "vPosition");
		acColorLocation = GLES20.glGetAttribLocation(colorProgram, "aColor");
		ucMatrixLocation = GLES20.glGetUniformLocation(colorProgram, "uMVPMatrix");
		ucOffsetLocation = GLES20.glGetUniformLocation(colorProgram, "uTranslationOffset");
	}
}
