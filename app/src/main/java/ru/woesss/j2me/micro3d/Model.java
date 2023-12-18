/*
 * Copyright 2020 Yury Kharchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.woesss.j2me.micro3d;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

class Model {
	final int numPatterns;
	final boolean hasPolyC;
	final boolean hasPolyT;

	FloatBuffer vertexArray;
	FloatBuffer normalsArray;
	final ByteBuffer texCoordArray;
	final FloatBuffer originalVertices;
	FloatBuffer normals;
	FloatBuffer originalNormals;
	final Polygon[] polygonsC;
	final Polygon[] polygonsT;
	final FloatBuffer vertices;
	final int vertexArrayCapacity;
	final int[][][] subMeshesLengthsT;
	final int[][] subMeshesLengthsC;
	final int numVerticesPolyT;
	final int[] indices;
	final ByteBuffer bones;

	Model(int vertices, int numBones, int patterns, int numTextures,
		  int polyT3, int polyT4, int polyC3, int polyC4) {
		numPatterns = patterns;
		subMeshesLengthsT = new int[4][numTextures][2];
		subMeshesLengthsC = new int[4][2];
		numVerticesPolyT = polyT3 * 3 + polyT4 * 6;
		int numVertices = (polyT3 + polyC3) * 3 + (polyT4 + polyC4) * 6;
		indices = new int[numVertices];
		vertexArrayCapacity = numVertices * 3;
		polygonsC = new Polygon[polyC3 + polyC4];
		polygonsT = new Polygon[polyT3 + polyT4];
		hasPolyT = polyT3 + polyT4 > 0;
		hasPolyC = polyC3 + polyC4 > 0;
		texCoordArray = BufferUtils.createByteBuffer(numVertices * 5);
		originalVertices = BufferUtils.createFloatBuffer(vertices * 3);
		int i = vertices * 3 + 3;
		this.vertices = BufferUtils.createFloatBuffer(i);
		this.vertices.put(--i, Float.POSITIVE_INFINITY);
		bones = BufferUtils.createByteBuffer(numBones * (12 + 2) * 4);
	}

	static final class Polygon {
		// polygon material flags
		static final int TRANSPARENT = 1;
		static final int BLEND_HALF = 2;
		static final int BLEND_ADD = 4;
		static final int BLEND_SUB = 6;
		private static final int DOUBLE_FACE = 16;
		static final int LIGHTING = 32;
		static final int SPECULAR = 64;
		final int[] indices;
		final int blendMode;
		final int doubleFace;
		byte[] texCoords;
		int face = -1;
		int pattern;

		Polygon(int material, byte[] texCoords, int... indices) {
			this.indices = indices;
			this.texCoords = texCoords;
			doubleFace = (material & DOUBLE_FACE) >> 4;
			blendMode = (material & BLEND_SUB);
		}
	}
}
