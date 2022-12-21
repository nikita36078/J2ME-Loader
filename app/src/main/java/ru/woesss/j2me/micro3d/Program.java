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
import static ru.woesss.j2me.micro3d.Utils.TAG;
import static ru.woesss.j2me.micro3d.Utils.TO_FLOAT;

import android.util.Log;

import com.mascotcapsule.micro3d.v3.Graphics3D;

import java.nio.ByteBuffer;

import javax.microedition.util.ContextHolder;

abstract class Program {
	static Tex tex;
	static Color color;
	static Simple simple;
	static Sprite sprite;
	private static boolean isCreated;

	protected final int id;
	protected int uAmbIntensity;
	protected int uDirIntensity;
	protected int uLightDir;
	protected int uMatrix;
	protected int uNormalMatrix;
	int aPosition;
	int aNormal;
	int aColorData;
	int aMaterial;

	Program(String vertexShader, String fragmentShader) {
		id = createProgram(vertexShader, fragmentShader);
		getLocations();
		Render.checkGlError("getLocations");
	}

	static void create() {
		if (isCreated) return;
		tex = new Tex();
		color = new Color();
		simple = new Simple();
		sprite = new Sprite();
		glReleaseShaderCompiler();
	}

	private int createProgram(String vertexShader, String fragmentShader) {
		String vertexShaderCode = ContextHolder.getAssetAsString(vertexShader);
		String fragmentShaderCode = ContextHolder.getAssetAsString(fragmentShader);

		int vertexId = loadShader(GL_VERTEX_SHADER, vertexShaderCode);
		int fragmentId = loadShader(GL_FRAGMENT_SHADER, fragmentShaderCode);

		int program = glCreateProgram();             // create empty OpenGL Program
		glAttachShader(program, vertexId);   // add the vertex shader to program
		glAttachShader(program, fragmentId); // add the fragment shader to program

		glLinkProgram(program);                  // create OpenGL program executables
		int[] status = new int[1];
		glGetProgramiv(program, GL_LINK_STATUS, status, 0);
		if (status[0] == 0) {
			String s = glGetProgramInfoLog(program);
			Log.e(TAG, "createProgram: " + s);
		}
		glDeleteShader(vertexId);
		glDeleteShader(fragmentId);
		Render.checkGlError("glLinkProgram");
		return program;
	}

	/**
	 * Utility method for compiling a OpenGL shader.
	 *
	 * <p><strong>Note:</strong> When developing shaders, use the checkGlError()
	 * method to debug shader coding errors.</p>
	 *
	 * @param type       - Vertex or fragment shader type.
	 * @param shaderCode - String containing the shader code.
	 * @return - Returns an id for the shader.
	 */
	protected int loadShader(int type, String shaderCode) {

		// create a vertex shader type (GLES20.GL_VERTEX_SHADER)
		// or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
		int shader = glCreateShader(type);

		// add the source code to the shader and compile it
		glShaderSource(shader, shaderCode);
		glCompileShader(shader);
		int[] status = new int[1];
		glGetShaderiv(shader, GL_COMPILE_STATUS, status, 0);
		if (status[0] == 0) {
			String s = glGetShaderInfoLog(shader);
			Log.e(TAG, "loadShader: " + s);
		}
		Render.checkGlError("glCompileShader");
		return shader;
	}

	void use() {
		glUseProgram(id);
	}

	protected abstract void getLocations();

	static void release() {
		if (!isCreated) return;
		tex.delete();
		color.delete();
		simple.delete();
		sprite.delete();
		isCreated = false;
	}

	void delete() {
		glDeleteProgram(id);
		Render.checkGlError("program delete");
	}

	void setLight(Light light) {
		if (light == null) {
			glUniform1f(uAmbIntensity, -1.0f);
			return;
		}
		glUniform1f(uAmbIntensity, Math.max(0, Math.min(light.ambIntensity, 4096)) * TO_FLOAT);
		glUniform1f(uDirIntensity, Math.max(0, Math.min(light.dirIntensity, 16384)) * TO_FLOAT);
		float x = light.x;
		float y = light.y;
		float z = light.z;
		float rlf = -1.0f / (float) Math.sqrt(x * x + y * y + z * z);
		glUniform3f(uLightDir, x * rlf, y * rlf, z * rlf);
	}

	static final class Color extends Program {
		private static final String VERTEX = "shaders/color.vsh";
		private static final String FRAGMENT = "shaders/color.fsh";
		int uSphereSize;
		int uToonThreshold;
		int uToonHigh;
		int uToonLow;

		Color() {
			super(VERTEX, FRAGMENT);
		}

		@Override
		protected void getLocations() {
			aPosition = glGetAttribLocation(id, "aPosition");
			aNormal = glGetAttribLocation(id, "aNormal");
			aColorData = glGetAttribLocation(id, "aColorData");
			aMaterial = glGetAttribLocation(id, "aMaterial");
			uMatrix = glGetUniformLocation(id, "uMatrix");
			uNormalMatrix = glGetUniformLocation(id, "uNormalMatrix");
			uAmbIntensity = glGetUniformLocation(id, "uAmbIntensity");
			uDirIntensity = glGetUniformLocation(id, "uDirIntensity");
			uLightDir = glGetUniformLocation(id, "uLightDir");
			uSphereSize = glGetUniformLocation(id, "uSphereSize");
			uToonThreshold = glGetUniformLocation(id, "uToonThreshold");
			uToonHigh = glGetUniformLocation(id, "uToonHigh");
			uToonLow = glGetUniformLocation(id, "uToonLow");
			use();
			glUniform1i(glGetUniformLocation(id, "uSphereUnit"), 2);
		}

		void setColor(ByteBuffer rgb) {
			rgb.rewind();
			float r = (rgb.get() & 0xff) / 255.0f;
			float g = (rgb.get() & 0xff) / 255.0f;
			float b = (rgb.get() & 0xff) / 255.0f;
			glVertexAttrib3f(aColorData, r, g, b);
		}

		void setToonShading(int attrs, int threshold, int high, int low) {
			if ((attrs & Graphics3D.ENV_ATTR_TOON_SHADING) != 0) {
				glUniform1f(uToonThreshold, threshold / 255.0f);
				glUniform1f(uToonHigh, high / 255.0f);
				glUniform1f(uToonLow, low / 255.0f);
			} else {
				glUniform1f(uToonThreshold, -1.0f);
			}
		}

		void bindMatrices(float[] mvp, float[] mv) {
			glUniformMatrix4fv(uMatrix, 1, false, mvp, 0);
			glUniformMatrix3fv(uNormalMatrix, 1, false, mv, 0);
		}

		void setSphere(TextureImpl sphere) {
			if (sphere != null) {
				glActiveTexture(GL_TEXTURE2);
				glBindTexture(GL_TEXTURE_2D, sphere.getId());
				glUniform2f(uSphereSize, sphere.getWidth(), sphere.getHeight());
			} else {
				glUniform2f(uSphereSize, -1, -1);
			}
		}
	}

	static final class Simple extends Program {
		private static final String VERTEX = "shaders/simple.vsh";
		private static final String FRAGMENT = "shaders/simple.fsh";
		int aTexture;

		Simple() {
			super(VERTEX, FRAGMENT);
		}

		protected void getLocations() {
			aPosition = glGetAttribLocation(id, "a_position");
			aTexture = glGetAttribLocation(id, "a_texcoord0");
			use();
			glUniform1i(glGetUniformLocation(id, "sampler0"), 1);
		}
	}

	static final class Tex extends Program {
		private static final String VERTEX = "shaders/tex.vsh";
		private static final String FRAGMENT = "shaders/tex.fsh";
		int uTexSize;
		int uSphereSize;
		int uToonThreshold;
		int uToonHigh;
		int uToonLow;

		Tex() {
			super(VERTEX, FRAGMENT);
		}

		@Override
		protected int loadShader(int type, String shaderCode) {
			if (Boolean.getBoolean("micro3d.v3.texture.filter")) {
				shaderCode = "#define FILTER\n" + shaderCode;
			}
			return super.loadShader(type, shaderCode);
		}

		protected void getLocations() {
			aPosition = glGetAttribLocation(id, "aPosition");
			aNormal = glGetAttribLocation(id, "aNormal");
			aColorData = glGetAttribLocation(id, "aColorData");
			aMaterial = glGetAttribLocation(id, "aMaterial");
			uTexSize = glGetUniformLocation(id, "uTexSize");
			uSphereSize = glGetUniformLocation(id, "uSphereSize");
			uMatrix = glGetUniformLocation(id, "uMatrix");
			uNormalMatrix = glGetUniformLocation(id, "uNormalMatrix");
			uAmbIntensity = glGetUniformLocation(id, "uAmbIntensity");
			uDirIntensity = glGetUniformLocation(id, "uDirIntensity");
			uLightDir = glGetUniformLocation(id, "uLightDir");
			uToonThreshold = glGetUniformLocation(id, "uToonThreshold");
			uToonHigh = glGetUniformLocation(id, "uToonHigh");
			uToonLow = glGetUniformLocation(id, "uToonLow");
			use();
			glUniform1i(glGetUniformLocation(id, "uTextureUnit"), 0);
			glUniform1i(glGetUniformLocation(id, "uSphereUnit"), 2);
		}

		void setTex(TextureImpl tex) {
			if (tex != null) {
				glActiveTexture(GL_TEXTURE0);
				glBindTexture(GL_TEXTURE_2D, tex.getId());
				glUniform2f(uTexSize, tex.getWidth(), tex.getHeight());
			} else {
				glUniform2f(uTexSize, 256, 256);
				glBindTexture(GL_TEXTURE_2D, 0);
			}
		}

		void setToonShading(int attrs, int threshold, int high, int low) {
			if ((attrs & Graphics3D.ENV_ATTR_TOON_SHADING) != 0) {
				glUniform1f(uToonThreshold, threshold / 255.0f);
				glUniform1f(uToonHigh, high / 255.0f);
				glUniform1f(uToonLow, low / 255.0f);
			} else {
				glUniform1f(uToonThreshold, -1.0f);
			}
		}

		void bindMatrices(float[] mvp, float[] mv) {
			glUniformMatrix4fv(uMatrix, 1, false, mvp, 0);
			glUniformMatrix3fv(uNormalMatrix, 1, false, mv, 0);
		}

		void setSphere(TextureImpl sphere) {
			if (sphere != null) {
				glActiveTexture(GL_TEXTURE2);
				glBindTexture(GL_TEXTURE_2D, sphere.getId());
				glUniform2f(uSphereSize, sphere.getWidth(), sphere.getHeight());
			} else {
				glUniform2f(uSphereSize, -1, -1);
			}
		}
	}

	static class Sprite extends Program {
		private static final String VERTEX = "shaders/sprite.vsh";
		private static final String FRAGMENT = "shaders/sprite.fsh";
		int uTexSize;
		int uIsTransparency;

		Sprite() {
			super(VERTEX, FRAGMENT);
		}

		@Override
		protected int loadShader(int type, String shaderCode) {
			if (Boolean.getBoolean("micro3d.v3.texture.filter")) {
				shaderCode = "#define FILTER\n" + shaderCode;
			}
			return super.loadShader(type, shaderCode);
		}

		protected void getLocations() {
			aPosition = glGetAttribLocation(id, "aPosition");
			aColorData = glGetAttribLocation(id, "aColorData");
			uTexSize = glGetUniformLocation(id, "uTexSize");
			uIsTransparency = glGetUniformLocation(id, "uIsTransparency");
			use();
			glUniform1i(glGetUniformLocation(id, "uTextureUnit"), 0);
		}

		void setTexture(TextureImpl texture) {
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, texture.getId());
			glUniform2f(uTexSize, texture.getWidth(), texture.getHeight());
		}
	}
}
