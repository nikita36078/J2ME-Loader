/*
 *  Copyright 2020 Yury Kharchenko
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

package com.mascotcapsule.micro3d.v3;

import static com.mascotcapsule.micro3d.v3.Util3D.TAG;
import static com.mascotcapsule.micro3d.v3.Utils.IDENTITY_AFFINE;
import static com.mascotcapsule.micro3d.v3.Utils.TO_FLOAT;
import static com.mascotcapsule.micro3d.v3.Utils.TO_RADIANS;

import android.util.Log;
import android.util.SparseIntArray;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

class Loader {
	private static final int[] POOL_NORMALS = new int[]{0, 0, 64, 0, 0, -64, 0, 0};
	private static final int[] SIZES = {8, 10, 13, 16};
	private final byte[] mBytes;
	private int pos;
	private int cached;
	private int cache;

	private Loader(byte[] bytes) {
		this.mBytes = bytes;
	}

	static Model loadMbacData(byte[] bytes) throws IOException {
		Loader loader = new Loader(bytes);
		if (loader.readUByte() != 'M' || loader.readUByte() != 'B') {
			throw new RuntimeException("Not a MBAC file");
		}
		int version = loader.readUByte();
		if (loader.readUByte() != 0 || version < 2 || version > 5) {
			throw new RuntimeException("Unsupported MBAC version: " + version);
		}
		int vertexFormat;
		int normalFormat;
		int polygonFormat;
		int boneFormat;
		if (version > 3) {
			vertexFormat = loader.readUByte();
			normalFormat = loader.readUByte();
			polygonFormat = loader.readUByte();
			boneFormat = loader.readUByte();
		} else {
			vertexFormat = 1;
			normalFormat = 0;
			polygonFormat = 1;
			boneFormat = 1;
		}
		if (boneFormat != 1) {
			throw new RuntimeException("Unexpected bone format: " + boneFormat);
		}

		int numVertices = loader.readUShort();
		int numPolyT3 = loader.readUShort();
		int numPolyT4 = loader.readUShort();
		int numBones = loader.readUShort();

		int numTextures;
		int numColors;
		int numPolyC3;
		int numPolyC4;
		int numPatterns;
		if (polygonFormat < 3) {
			numTextures = 1;
			numPolyC3 = 0;
			numPolyC4 = 0;
			numPatterns = 1;
			numColors = 0;
		} else {
			numPolyC3 = loader.readUShort();
			numPolyC4 = loader.readUShort();
			numTextures = loader.readUShort();
			numPatterns = loader.readUShort();
			numColors = loader.readUShort();
		}
		if (numVertices > 21845 || numTextures > 16 || numPatterns > 33 || numColors > 256) {
			throw new RuntimeException(String.format("MBAC format error:\n" +
							"numVertices=%d numTextures=%d " +
							"numPatterns=%d numColors=%d\n",
					numVertices, numTextures, numPatterns, numColors));
		}

		Model data = new Model(numVertices, numBones, numPatterns,
				numTextures, numPolyT3, numPolyT4, numPolyC3, numPolyC4);
		int[][][] patterns = new int[numPatterns][(numTextures + 1)][2];
		if (version == 5) {
			for (int i = 0; i < numPatterns; i++) {
				int[][] pattern = patterns[i];
				pattern[0][0] = loader.readUShort();
				pattern[0][1] = loader.readUShort();
				for (int j = 1; j <= numTextures; j++) {
					pattern[j][0] = loader.readUShort();
					pattern[j][1] = loader.readUShort();
				}
			}
		} else {
			patterns[0] = new int[][]{new int[]{numPolyC3, numPolyC4}, new int[]{numPolyT3, numPolyT4}};
		}

		if (vertexFormat == 1) {
			loader.readVerticesV1(data.originalVertices);
		} else if (vertexFormat == 2) {
			loader.readVerticesV2(data.originalVertices);
		} else {
			throw new RuntimeException("Unexpected vertexFormat: " + vertexFormat);
		}
		loader.clearCache();
		data.originalVertices.rewind();

		if (normalFormat != 0) {
			FloatBuffer normals = ByteBuffer.allocateDirect(numVertices * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
			if (normalFormat == 1) {
				try {
					loader.readNormalsV1(normals);
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Normals loading error", e);
				}
			} else if (normalFormat == 2) {
				try {
					loader.readNormalsV2(normals);
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Normals loading error", e);
				}
			} else {
				throw new RuntimeException("Unsupported normalFormat: " + normalFormat);
			}
			normals.rewind();
			data.originalNormals = normals;
			int len = numVertices * 3 + 3;
			data.normals = ByteBuffer.allocateDirect(len * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
			data.normals.put(--len, 1.0f);
		}
		loader.clearCache();

		if (data.hasPolyC) {
			loader.readPolyC(data, numVertices, numColors, numPolyC3);
		}

		if (data.hasPolyT) {

			switch (polygonFormat) {
				case 1:
					loader.readPolyV1(data, numVertices, numPolyT3);
					break;
				case 2:
					loader.readPolyV2(data, numVertices, numPolyT3);
					break;
				case 3:
					loader.readPolyV3(data, numVertices, numPolyT3);
					break;
				default:
					throw new RuntimeException("Unexpected polygonFormat: " + polygonFormat);
			}
		}
		loader.clearCache();

		int c3 = 0;
		int c4 = numPolyC3;
		int t3 = 0;
		int t4 = numPolyT3;

		Model.Polygon[] polygonsC = data.polygonsC;
		Model.Polygon[] polygonsT = data.polygonsT;
		for (int i = 0; i < numPatterns; i++) {
			final int[][] pattern = patterns[i];
			int[] polygons = pattern[0];
			int cnt = polygons[0];
			final int p = i == 0 ? 0 : 1 << i;
			for (int j = 0; j < cnt; j++) {
				polygonsC[c3++].pattern = p;
			}
			cnt = polygons[1];
			for (int j = 0; j < cnt; j++) {
				polygonsC[c4++].pattern = p;
			}
			for (int j = 0; j < numTextures; j++) {
				polygons = pattern[j + 1];
				cnt = polygons[0];
				for (int k = 0; k < cnt; k++) {
					final Model.Polygon polygon = polygonsT[t3++];
					polygon.pattern = p;
					polygon.face = j;
				}
				cnt = polygons[1];
				for (int k = 0; k < cnt; k++) {
					final Model.Polygon polygon = polygonsT[t4++];
					polygon.pattern = p;
					polygon.face = j;
				}

			}
		}

		int count = loader.readBones(numBones, data);
		if (count != numVertices) {
			throw new RuntimeException("Bones vertices = " + count + ", but all vertices = " + numVertices);
		}

		int available = loader.available();
		if (version >= 4) {
			available -= 20;
		}
		if (available > 0) {
			Log.e(TAG, "Uninterpreted bytes in MBAC (" + available + ", v=" + version);
		}

		return data;
	}

	static Action[] loadMtraData(byte[] bytes) throws IOException {
		Loader reader = new Loader(bytes);
		if (reader.readUByte() != 'M' || reader.readUByte() != 'T') {
			throw new RuntimeException("Not a MTRA file");
		}
		int version = reader.readUByte();
		if (reader.readUByte() != 0 || version < 2 || version > 5) {
			throw new RuntimeException("Unsupported version: " + version);
		}

		int numActions = reader.readUShort();
		int numBones = reader.readUShort();
		Action[] actions = new Action[numActions];
		// number of bones by transform types
		int[] transTypeCounts = new int[8];
		for (int i = 0; i < 8; i++) {
			transTypeCounts[i] = reader.readUShort();
		}
		if (transTypeCounts[7] != 0) {
			// index 7 is unknown, I did not find mtra with non-zero value
			Log.w(TAG, "ActTableData: transTypeCounts[7] = " + transTypeCounts[7]);
		}
		//noinspection unused
		int dataSize = reader.readInt();
		// 'dataSize' and 'transTypeCounts' may be used for allocate memory and verify data)

		for (int action = 0; action < numActions; action++) {
			int keyframes = reader.readUShort();
			Action act = new Action(keyframes, numBones);
			actions[action] = act;

			for (int bone = 0; bone < numBones; bone++) {
				act.boneActions[bone] = reader.readBoneAction(act, bone * 12);
			}
			if (version < 5) continue;
			// dynamic polygons chunk
			int count = reader.readUShort();
			final SparseIntArray sparseIntArray = new SparseIntArray(count);
			actions[action].dynamic = sparseIntArray;
			for (int j = 0; j < count; j++) {
				int frame = reader.readUShort();
				int pattern = reader.readInt();
				sparseIntArray.put(frame, pattern);
			}
		}

		int available = reader.available();
		if (version >= 4) {
			available -= 20;
		}
		if (available > 0) {
			Log.e(TAG, "ActTableData: uninterpreted bytes in MTRA");
		}

		return actions;
	}

	private void readVerticesV1(FloatBuffer vertices) throws IOException {
		while (vertices.hasRemaining()) {
			vertices.put(readShort());
		}
	}

	private void readVerticesV2(FloatBuffer vertices) throws IOException {
		while (vertices.hasRemaining()) {
			int chunk = readUBits(8);
			int type = chunk >> 6;
			int size = SIZES[type];
			int count = (chunk & 0x3F) + 1;
			if (count > vertices.remaining()) {
				throw new IOException("Vertex data largest numVertices param");
			}
			for (int i = 0; i < count; i++) {
				vertices.put(readBits(size));
				vertices.put(readBits(size));
				vertices.put(readBits(size));
			}
		}
	}

	private void readNormalsV1(FloatBuffer normals) throws IOException {
		while (normals.hasRemaining()) {
			normals.put(readShort());
		}
	}

	private void readNormalsV2(FloatBuffer normals) throws IOException {
		for (int i = 0, len = normals.capacity() / 3; i < len; i++) {
			int x = readUBits(7);
			int y;
			int z;
			if (x == 64) {
				int type = readUBits(3);
				if (type > 5) throw new RuntimeException("Normal read error");
				z = POOL_NORMALS[type++];
				y = POOL_NORMALS[type++];
				x = POOL_NORMALS[type];
			} else {
				x = (x << 25) >> 25;
				y = (readUBits(7) << 25) >> 25;
				int sign = readUBits(1);
				int dq = 4096 - x * x - y * y;
				z = dq > 0 ? (int) Math.round(Math.sqrt(dq)) : 0;
				if (sign == 1) z = -z;
			}
			normals.put(x);
			normals.put(y);
			normals.put(z);
		}
	}

	private void readPolyC(Model data, int numVertex, int numColor, int numTriangles) throws IOException {
		int materialBits = readUByte();
		int vertexIndexBits = readUByte();
		int colorBits = readUByte();
		int colorIdBits = readUByte();
		int unknownByte = readUByte();
		if (unknownByte != 0) Log.w(TAG, "PolyC unknownByte = " + unknownByte);

		byte[] colors = new byte[numColor * 3];
		for (int i = 0; i < colors.length; i++) {
			colors[i] = (byte) readUBits(colorBits);
		}

		Model.Polygon[] polygonsC = data.polygonsC;
		for (int i = 0; i < numTriangles; i++) {
			int material = readUBits(materialBits) << 1;
			if ((material & 0xFC09) != 0) {
				throw new RuntimeException("Unexpected material: " + material);
			}
			int a = readUBits(vertexIndexBits);
			int b = readUBits(vertexIndexBits);
			int c = readUBits(vertexIndexBits);
			if (a >= numVertex || b >= numVertex || c >= numVertex) {
				throw new IOException("Format error: indices greatest or equal num vertices");
			}
			int colorId = readUBits(colorIdBits) * 3;
			byte R = colors[colorId++];
			byte G = colors[colorId++];
			byte B = colors[colorId];
			byte light = (byte) ((material & Model.Polygon.LIGHTING) >> 5);
			byte specular = (byte) ((material & Model.Polygon.SPECULAR) >> 6);
			byte[] materialData = {
					R, G, B, light, specular,
					R, G, B, light, specular,
					R, G, B, light, specular,
			};
			final Model.Polygon polygon = new Model.Polygon(material, materialData, a, b, c);
			polygonsC[i] = polygon;
		}

		for (int i = numTriangles; i < polygonsC.length; i++) {
			int material = readUBits(materialBits) << 1;
			if ((material & 0xFC09) != 0) {
				throw new RuntimeException("Unexpected material: " + material);
			}
			int a = readUBits(vertexIndexBits);
			int b = readUBits(vertexIndexBits);
			int c = readUBits(vertexIndexBits);
			int d = readUBits(vertexIndexBits);
			if (a >= numVertex || b >= numVertex || c >= numVertex || d >= numVertex) {
				throw new IOException("Format error: indices greatest or equal num vertices");
			}
			int colorId = readUBits(colorIdBits) * 3;
			byte R = colors[colorId++];
			byte G = colors[colorId++];
			byte B = colors[colorId];
			byte light = (byte) ((material & Model.Polygon.LIGHTING) >> 5);
			byte specular = (byte) ((material & Model.Polygon.SPECULAR) >> 6);
			byte[] materialData = {
					R, G, B, light, specular,
					R, G, B, light, specular,
					R, G, B, light, specular,
					R, G, B, light, specular,
					R, G, B, light, specular,
					R, G, B, light, specular,
			};
			final Model.Polygon polygon = new Model.Polygon(material, materialData, a, b, c, c, b, d);
			polygonsC[i] = polygon;
		}
	}

	private void readPolyV1(Model data, int numVertices, int numPolyT3) throws IOException {

		Model.Polygon[] polygons = data.polygonsT;
		for (int i = 0; i < numPolyT3; i++) {
			int material = readUShort();
			if ((material & 0xFFF9) != 0) {
				throw new IOException("Unexpected material: " + material);
			}
			int a = readUShort();
			int b = readUShort();
			int c = readUShort();
			if (a >= numVertices || b >= numVertices || c >= numVertices) {
				throw new IOException("Format error: indices greatest or equal num vertices");
			}
			int mat = (material & 4) << 2 | (material & 2) >> 1;
			byte transparent = (byte) (mat & Model.Polygon.TRANSPARENT);
			byte[] texCoords = new byte[]{
					readByte(), readByte(), 1, 0, transparent,
					readByte(), readByte(), 1, 0, transparent,
					readByte(), readByte(), 1, 0, transparent,
			};
			polygons[i] = new Model.Polygon(mat, texCoords, a, b, c);
		}

		for (int i = numPolyT3, len = polygons.length; i < len; i++) {
			int material = readUShort();
			if ((material & 0xFFF8) != 0 || (material & 1) == 0) {
				throw new IOException("Unexpected material: " + material);
			}
			int a = readUShort();
			int b = readUShort();
			int c = readUShort();
			int d = readUShort();
			if (a >= numVertices || b >= numVertices || c >= numVertices || d >= numVertices) {
				throw new IOException("Format error: indices greatest or equal num vertices");
			}
			byte uA = readByte();
			byte vA = readByte();
			byte uB = readByte();
			byte vB = readByte();
			byte uC = readByte();
			byte vC = readByte();
			byte uD = readByte();
			byte vD = readByte();
			int mat = (material & 4) << 2 | (material & 2) >> 1;
			byte transparent = (byte) (mat & Model.Polygon.TRANSPARENT);
			byte[] texCoords = new byte[]{
					uA, vA, 1, 0, transparent,
					uB, vB, 1, 0, transparent,
					uC, vC, 1, 0, transparent,
					uC, vC, 1, 0, transparent,
					uB, vB, 1, 0, transparent,
					uD, vD, 1, 0, transparent,
			};
			polygons[i] = new Model.Polygon(mat, texCoords, a, b, c, c, b, d);
		}
	}

	private void readPolyV2(Model data, int numVertex, int numTriangles) throws IOException {
		int matBitSize = readUByte();
		int vertexIdxSize = readUByte();

		Model.Polygon[] polygons = data.polygonsT;
		for (int i = 0; i < numTriangles; i++) {
			int material = readUBits(matBitSize);
			if ((material & 0xFF88) != 0) {
				throw new IOException("Unexpected material: " + material);
			}
			int a = readUBits(vertexIdxSize);
			int b = readUBits(vertexIdxSize);
			int c = readUBits(vertexIdxSize);
			if (a >= numVertex || b >= numVertex || c >= numVertex) {
				throw new IOException("Format error: indices greatest or equal num vertices");
			}
			byte transparent = (byte) (material & Model.Polygon.TRANSPARENT);
			byte light = (byte) ((material & Model.Polygon.LIGHTING) >> 5);
			byte specular = (byte) ((material & Model.Polygon.SPECULAR) >> 6);
			byte[] texCoords = new byte[]{
					(byte) readUBits(7), (byte) readUBits(7), light, specular, transparent,
					(byte) readUBits(7), (byte) readUBits(7), light, specular, transparent,
					(byte) readUBits(7), (byte) readUBits(7), light, specular, transparent,
			};
			polygons[i] = new Model.Polygon(material, texCoords, a, b, c);
		}

		for (int i = numTriangles, len = polygons.length; i < len; i++) {
			int material = readUBits(matBitSize);
			if ((material & 0xFF88) != 0) {
				throw new RuntimeException("Unexpected material: " + material);
			}
			int a = readUBits(vertexIdxSize);
			int b = readUBits(vertexIdxSize);
			int c = readUBits(vertexIdxSize);
			int d = readUBits(vertexIdxSize);
			if (a >= numVertex || b >= numVertex || c >= numVertex || d >= numVertex) {
				throw new RuntimeException("Format error: indices greatest or equal num vertices");
			}
			byte uA = (byte) readUBits(7);
			byte vA = (byte) readUBits(7);
			byte uB = (byte) readUBits(7);
			byte vB = (byte) readUBits(7);
			byte uC = (byte) readUBits(7);
			byte vC = (byte) readUBits(7);
			byte uD = (byte) readUBits(7);
			byte vD = (byte) readUBits(7);

			byte transparent = (byte) (material & Model.Polygon.TRANSPARENT);
			byte light = (byte) ((material & Model.Polygon.LIGHTING) >> 5);
			byte specular = (byte) ((material & Model.Polygon.SPECULAR) >> 6);
			byte[] texCoords = new byte[]{
					uA, vA, light, specular, transparent,
					uB, vB, light, specular, transparent,
					uC, vC, light, specular, transparent,
					uC, vC, light, specular, transparent,
					uB, vB, light, specular, transparent,
					uD, vD, light, specular, transparent,
			};
			polygons[i] = new Model.Polygon(material, texCoords, a, b, c, c, b, d);
		}
	}

	private void readPolyV3(Model data, int numVertex, int numTriangles) throws IOException {
		int materialBits = readUBits(8);
		int vertexIndexBits = readUBits(8);
		int uvBits = readUBits(8);
		int unknownByte = readUBits(8);
		if (unknownByte != 0) Log.w(TAG, "PolyT v3: unknownByte = " + unknownByte);


		Model.Polygon[] polygons = data.polygonsT;
		for (int i = 0; i < numTriangles; i++) {
			int material = readUBits(materialBits);
			if ((material & 0xFC08) != 0)
				throw new IOException("Unexpected material: " + material);
			int a = readUBits(vertexIndexBits);
			int b = readUBits(vertexIndexBits);
			int c = readUBits(vertexIndexBits);
			if (a >= numVertex || b >= numVertex || c >= numVertex) {
				throw new IOException("Format error: indices greatest or equal num vertices");
			}

			byte transparent = (byte) (material & Model.Polygon.TRANSPARENT);
			byte light = (byte) ((material & Model.Polygon.LIGHTING) >> 5);
			byte specular = (byte) ((material & Model.Polygon.SPECULAR) >> 6);
			byte[] texCoords = new byte[]{
					(byte) readUBits(uvBits), (byte) readUBits(uvBits), light, specular, transparent,
					(byte) readUBits(uvBits), (byte) readUBits(uvBits), light, specular, transparent,
					(byte) readUBits(uvBits), (byte) readUBits(uvBits), light, specular, transparent,
			};
			polygons[i] = new Model.Polygon(material, texCoords, a, b, c);
		}

		for (int i = numTriangles, len = polygons.length; i < len; i++) {
			int material = readUBits(materialBits);
			if ((material & 0xFC08) != 0)
				throw new IOException("Unexpected material: " + material);
			int a = readUBits(vertexIndexBits);
			int b = readUBits(vertexIndexBits);
			int c = readUBits(vertexIndexBits);
			int d = readUBits(vertexIndexBits);
			if (a >= numVertex || b >= numVertex || c >= numVertex || d >= numVertex) {
				throw new IOException("Format error: indices greatest or equal num vertices");
			}

			byte uA = (byte) readUBits(uvBits);
			byte vA = (byte) readUBits(uvBits);
			byte uB = (byte) readUBits(uvBits);
			byte vB = (byte) readUBits(uvBits);
			byte uC = (byte) readUBits(uvBits);
			byte vC = (byte) readUBits(uvBits);
			byte uD = (byte) readUBits(uvBits);
			byte vD = (byte) readUBits(uvBits);

			byte transparent = (byte) (material & Model.Polygon.TRANSPARENT);
			byte light = (byte) ((material & Model.Polygon.LIGHTING) >> 5);
			byte specular = (byte) ((material & Model.Polygon.SPECULAR) >> 6);
			byte[] texCoords = new byte[]{
					uA, vA, light, specular, transparent,
					uB, vB, light, specular, transparent,
					uC, vC, light, specular, transparent,
					uC, vC, light, specular, transparent,
					uB, vB, light, specular, transparent,
					uD, vD, light, specular, transparent,
			};
			polygons[i] = new Model.Polygon(material, texCoords, a, b, c, c, b, d);
		}
	}

	private int readBones(int numBones, Model data) throws IOException {
		ByteBuffer bones = data.bones;

		int boneVertexSum = 0;
		for (int i = 0; i < numBones; i++) {
			int boneVertices = readUShort();
			int parent = readShort();

			if (parent < -1) {
				throw new RuntimeException("Format error (negative parent). Please report this bug");
			}
			bones.putInt(boneVertices);
			bones.putInt(parent);

			bones.putFloat(readShort() * TO_FLOAT);
			bones.putFloat(readShort() * TO_FLOAT);
			bones.putFloat(readShort() * TO_FLOAT);
			bones.putFloat(readShort());
			bones.putFloat(readShort() * TO_FLOAT);
			bones.putFloat(readShort() * TO_FLOAT);
			bones.putFloat(readShort() * TO_FLOAT);
			bones.putFloat(readShort());
			bones.putFloat(readShort() * TO_FLOAT);
			bones.putFloat(readShort() * TO_FLOAT);
			bones.putFloat(readShort() * TO_FLOAT);
			bones.putFloat(readShort());

			boneVertexSum += boneVertices;
		}
		bones.rewind();
		return boneVertexSum;
	}

	private Action.Bone readBoneAction(Action act, int mtxOffset) throws IOException {
		int type = readUByte();
		Action.Bone boneAction = new Action.Bone(type, mtxOffset, act.matrices);
		switch (type) {
			case 0:
				float[] m = act.matrices;
				m[mtxOffset     ] = readShort() * TO_FLOAT;
				m[mtxOffset +  1] = readShort() * TO_FLOAT;
				m[mtxOffset +  2] = readShort() * TO_FLOAT;
				m[mtxOffset +  3] = readShort();
				m[mtxOffset +  4] = readShort() * TO_FLOAT;
				m[mtxOffset +  5] = readShort() * TO_FLOAT;
				m[mtxOffset +  6] = readShort() * TO_FLOAT;
				m[mtxOffset +  7] = readShort();
				m[mtxOffset +  8] = readShort() * TO_FLOAT;
				m[mtxOffset +  9] = readShort() * TO_FLOAT;
				m[mtxOffset + 10] = readShort() * TO_FLOAT;
				m[mtxOffset + 11] = readShort();
				break;
			case 1:
				System.arraycopy(IDENTITY_AFFINE, 0, act.matrices, mtxOffset, 12);
				break;
			case 2: {
				// translate
				int count = readUShort();
				Action.Animation translate = new Action.Animation(count);
				for (int j = 0; j < count; j++) {
					int kf = readUShort(); // key frame
					int x = readShort();  // translate.x
					int y = readShort();  // translate.y
					int z = readShort();  // translate.z
					translate.set(j, kf, x, y, z);
				}
				boneAction.translate = translate;

				// scale
				count = readUShort();
				Action.Animation scale = new Action.Animation(count);
				for (int j = 0; j < count; j++) {
					int kf = readUShort(); // key frame
					float x = readShort() * TO_FLOAT;  // scale.x
					float y = readShort() * TO_FLOAT;  // scale.y
					float z = readShort() * TO_FLOAT;  // scale.z
					scale.set(j, kf, x, y, z);
				}
				boneAction.scale = scale;

				// rotate
				count = readUShort();
				Action.Animation rotate = new Action.Animation(count);
				for (int j = 0; j < count; j++) {
					int kf = readUShort(); // key frame
					float x = readShort();  // rotate.x
					float y = readShort();  // rotate.y
					float z = readShort();  // rotate.z
					rotate.set(j, kf, x, y, z);
				}
				boneAction.rotate = rotate;

				// roll
				count = readUShort();
				Action.RollAnim roll = new Action.RollAnim(count);
				for (int j = 0; j < count; j++) {
					int kf = readUShort();   // key frame
					float r = readShort() * TO_RADIANS; // roll
					roll.set(j, kf, r);
				}
				boneAction.roll = roll;
				break;
			}
			case 3: {
				// translate (for all frames)
				Action.Animation translate = new Action.Animation(1);
				int transX = readShort();
				int transY = readShort();
				int transZ = readShort();
				translate.set(0, 0, transX, transY, transZ);
				boneAction.translate = translate;

				// rotate
				int count = readUShort();
				Action.Animation rotate = new Action.Animation(count);
				for (int j = 0; j < count; j++) {
					int kf = readUShort(); // key frame
					float x = readShort();  // rotate.x
					float y = readShort();  // rotate.y
					float z = readShort();  // rotate.z
					rotate.set(j, kf, x, y, z);
				}
				boneAction.rotate = rotate;

				// roll (for all frames)
				float r = readShort() * TO_RADIANS;
				Action.RollAnim roll = new Action.RollAnim(1);
				roll.set(0, 0, r);
				boneAction.roll = roll;
				break;
			}
			case 4: {
				// rotate
				int count = readUShort();
				Action.Animation rotate = new Action.Animation(count);
				for (int j = 0; j < count; j++) {
					int kf = readUShort(); // key frame
					float x = readShort();  // rotate.x
					float y = readShort();  // rotate.y
					float z = readShort();  // rotate.z
					rotate.set(j, kf, x, y, z);
				}
				boneAction.rotate = rotate;

				// roll
				count = readUShort();
				Action.RollAnim roll = new Action.RollAnim(count);
				for (int j = 0; j < count; j++) {
					int kf = readUShort();   // key frame
					float r = readShort() * TO_RADIANS; // roll
					roll.set(j, kf, r);
				}
				boneAction.roll = roll;
				break;
			}
			case 5: {
				// rotate
				int count = readUShort();
				Action.Animation rotate = new Action.Animation(count);
				for (int j = 0; j < count; j++) {
					int kf = readUShort(); // key frame
					float x = readShort();  // rotate.x
					float y = readShort();  // rotate.y
					float z = readShort();  // rotate.z
					rotate.set(j, kf, x, y, z);
				}
				boneAction.rotate = rotate;
				break;
			}
			case 6: {
				// translate
				int count = readUShort();
				Action.Animation translate = new Action.Animation(count);
				for (int j = 0; j < count; j++) {
					int kf = readUShort(); // key frame
					int x = readShort();  // translate.x
					int y = readShort();  // translate.y
					int z = readShort();  // translate.z
					translate.set(j, kf, x, y, z);
				}
				boneAction.translate = translate;

				// rotate
				count = readUShort();
				Action.Animation rotate = new Action.Animation(count);
				for (int j = 0; j < count; j++) {
					int kf = readUShort(); // key frame
					float x = readShort();  // rotate.x
					float y = readShort();  // rotate.y
					float z = readShort();  // rotate.z
					rotate.set(j, kf, x, y, z);
				}
				boneAction.rotate = rotate;

				// roll
				count = readUShort();
				Action.RollAnim roll = new Action.RollAnim(count);
				for (int j = 0; j < count; j++) {
					int kf = readUShort();   // key frame
					float r = readShort() * TO_RADIANS; // roll
					roll.set(j, kf, r);
				}
				boneAction.roll = roll;
				break;
			}
			default:
				throw new RuntimeException("Animation type " + type + " is not supported");
		}
		return boneAction;
	}

	private byte readByte() throws IOException {
		if (pos >= mBytes.length) throw new EOFException();
		return mBytes[pos++];
	}

	private int readUByte() throws IOException {
		if (pos >= mBytes.length) throw new EOFException();
		return mBytes[pos++] & 0xff;
	}

	private short readShort() throws IOException {
		if (pos + 1 >= mBytes.length) throw new EOFException();
		return (short) (mBytes[pos++] & 0xff | mBytes[pos++] << 8);
	}

	private int readUShort() throws IOException {
		if (pos + 1 >= mBytes.length) throw new EOFException();
		return mBytes[pos++] & 0xff | (mBytes[pos++] & 0xff) << 8;
	}

	private int readInt() throws IOException {
		if (pos + 3 >= mBytes.length) throw new EOFException();
		return mBytes[pos++] & 0xff | (mBytes[pos++] & 0xff) << 8
				| (mBytes[pos++] & 0xff) << 16 | mBytes[pos++] << 24;
	}

	private int available() {
		return mBytes.length - pos;
	}

	private int readUBits(int size) throws IOException {
		if (size > 25) {
			Log.e(TAG, "readUBits(size=" + size + ')');
			throw new IllegalArgumentException("Invalid bit size=" + size);
		}
		while (size > cached) {
			cache |= readUByte() << cached;
			cached += 8;
		}
		int mask = ~(0xffffffff << size);
		int result = cache & mask;
		cached -= size;
		cache >>>= size;
		return result;
	}

	private int readBits(int size) throws IOException {
		int lzb = 32 - size;
		return (readUBits(size) << lzb) >> lzb;
	}

	private void clearCache() {
		cache = 0;
		cached = 0;
	}
}
