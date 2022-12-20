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

import static ru.woesss.j2me.micro3d.Utils.TAG;

import android.util.Log;
import android.util.SparseIntArray;

import com.mascotcapsule.micro3d.v3.Texture;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Stack;

import javax.microedition.shell.AppClassLoader;

public class FigureImpl {
	public Stack<RenderNode.FigureNode> stack = new Stack<RenderNode.FigureNode>();
	public Model data;
	public Texture[] textures;
	public int textureIndex = -1;
	public int pattern;

	@SuppressWarnings("unused")
	public FigureImpl(byte[] b) {
		if (b == null) {
			throw new NullPointerException();
		}
		try {
			init(b);
		} catch (Exception e) {
			Log.e(TAG, "Error loading data", e);
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unused")
	public FigureImpl(String name) throws IOException {
		byte[] bytes = AppClassLoader.getResourceAsBytes(name);
		if (bytes == null) {
			throw new IOException("Error reading resource: " + name);
		}
		try {
			init(bytes);
		} catch (Exception e) {
			Log.e(TAG, "Error loading data from [" + name + "]", e);
			throw new RuntimeException(e);
		}
	}

	public synchronized void init(byte[] bytes) throws IOException {
		data = Loader.loadMbacData(bytes);
		Utils.transform(data.originalVertices, data.vertices,
				data.originalNormals, data.normals, data.bones, null);
		sortPolygons();
		fillTexCoordBuffer();
	}

	@SuppressWarnings("unused")
	public final void dispose() {
		data = null;
	}

	public void sortPolygons() {
		Model.Polygon[] polygonsT = data.polygonsT;
		Arrays.sort(polygonsT, (a, b) -> {
			if (a.blendMode != b.blendMode) {
				return a.blendMode - b.blendMode;
			}
			if (a.face != b.face) {
				return a.face - b.face;
			}
			return a.doubleFace - b.doubleFace;
		});
		int[][][] subMeshesLengthsT = data.subMeshesLengthsT;
		int[] indexArray = data.indices;
		int pos = 0;
		for (Model.Polygon p : polygonsT) {
			int[] indices = p.indices;
			int length = indices.length;
			subMeshesLengthsT[p.blendMode >> 1][p.face][p.doubleFace] += length;
			System.arraycopy(indices, 0, indexArray, pos, length);
			pos += length;
		}

		Model.Polygon[] polygonsC = data.polygonsC;
		Arrays.sort(polygonsC, (a, b) -> {
			if (a.blendMode != b.blendMode) {
				return a.blendMode - b.blendMode;
			}
			return a.doubleFace - b.doubleFace;
		});
		int[][] subMeshesLengthsC = data.subMeshesLengthsC;
		for (Model.Polygon p : polygonsC) {
			int[] indices = p.indices;
			int length = indices.length;
			subMeshesLengthsC[p.blendMode >> 1][p.doubleFace] += length;
			System.arraycopy(indices, 0, indexArray, pos, length);
			pos += length;
		}
	}

	@SuppressWarnings("unused")
	public synchronized final void setPosture(ActTableImpl actTable, int action, int frame) {
		Action act = actTable.actions[action];
		final SparseIntArray dynamic = act.dynamic;
		if (dynamic != null) {
			int iFrame = frame < 0 ? 0 : frame >> 16;
			for (int i = dynamic.size() - 1; i >= 0; i--) {
				if (dynamic.keyAt(i) <= iFrame) {
					pattern = dynamic.valueAt(i);
					applyPattern();
					break;
				}
			}
		}
		//noinspection ManualMinMaxCalculation
		applyBoneAction(act, frame < 0 ? 0 : frame);
	}

	public void applyPattern() {
		int[] indexArray = data.indices;
		int pos = 0;
		int invalid = data.vertices.capacity() / 3 - 1;
		for (Model.Polygon p : data.polygonsT) {
			int[] indices = p.indices;
			int length = indices.length;
			int pp = p.pattern;
			if ((pp & pattern) == pp) {
				for (int i = 0; i < length; i++) {
					indexArray[pos++] = indices[i];
				}
			} else {
				while (length > 0) {
					indexArray[pos++] = invalid;
					length--;
				}
			}
		}

		for (Model.Polygon p : data.polygonsC) {
			int[] indices = p.indices;
			int length = indices.length;
			int pp = p.pattern;
			if ((pp & pattern) == pp) {
				for (int i = 0; i < length; i++) {
					indexArray[pos++] = indices[i];
				}
			} else {
				while (length > 0) {
					indexArray[pos++] = invalid;
					length--;
				}
			}
		}
	}

	public final Texture getTexture() {
		if (textureIndex < 0) {
			return null;
		}
		return textures[textureIndex];
	}

	public final TextureImpl getTextureImpl() {
		if (textureIndex < 0) {
			return null;
		}
		return textures[textureIndex].impl;
	}

	public final void setTexture(Texture tex) {
		if (tex == null)
			throw new NullPointerException();
		if (tex.impl.isSphere)
			throw new IllegalArgumentException();

		textures = new Texture[]{tex};
		textureIndex = 0;
	}

	public final void setTexture(Texture[] t) {
		if (t == null) throw new NullPointerException();
		if (t.length == 0) throw new IllegalArgumentException();
		for (Texture texture : t) {
			if (texture == null) throw new NullPointerException();
			if (texture.impl.isSphere) throw new IllegalArgumentException();
		}
		textures = t;
		textureIndex = -1;
	}

	@SuppressWarnings("WeakerAccess")
	public final int getNumTextures() {
		if (textures == null) {
			return 0;
		}
		return textures.length;
	}

	@SuppressWarnings("unused")
	public final void selectTexture(int idx) {
		if (idx < 0 || idx >= getNumTextures()) {
			throw new IllegalArgumentException();
		}
		textureIndex = idx;
	}

	@SuppressWarnings("unused")
	public final int getNumPattern() {
		return data.numPatterns;
	}

	@SuppressWarnings("unused")
	public synchronized final void setPattern(int idx) {
		pattern = idx;
		applyPattern();
	}

	public void applyBoneAction(Action act, int frame) {
		Action.Bone[] actionBones = act.boneActions;
		if (actionBones.length == 0) return;
		synchronized (act.matrices) {
			for (final Action.Bone actionBone : actionBones) {
				actionBone.setFrame(frame);
			}
			Utils.transform(data.originalVertices, data.vertices,
					data.originalNormals, data.normals, data.bones, act.matrices);
		}
	}

	public void fillTexCoordBuffer() {
		ByteBuffer buffer = data.texCoordArray;
		buffer.rewind();
		for (Model.Polygon poly : data.polygonsT) {
			buffer.put(poly.texCoords);
			poly.texCoords = null;
		}
		for (Model.Polygon poly : data.polygonsC) {
			buffer.put(poly.texCoords);
			poly.texCoords = null;
		}
		buffer.rewind();
	}

	public synchronized FloatBuffer getVertexData() {
		if (data.vertexArray == null) {
			data.vertexArray = ByteBuffer.allocateDirect(data.vertexArrayCapacity)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();
		}
		Utils.fillBuffer(data.vertexArray, data.vertices, data.indices);
		return data.vertexArray;
	}

	public synchronized FloatBuffer getNormalsData() {
		if (data.originalNormals == null) {
			return null;
		}
		if (data.normalsArray == null) {
			data.normalsArray = ByteBuffer.allocateDirect(data.vertexArrayCapacity)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();
		}
		Utils.fillBuffer(data.normalsArray, data.normals, data.indices);
		return data.normalsArray;
	}
}