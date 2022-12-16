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

package com.mascotcapsule.micro3d.v3;

import static com.mascotcapsule.micro3d.v3.Util3D.TAG;

import android.util.Log;
import android.util.SparseIntArray;

import com.mascotcapsule.micro3d.v3.RenderNode.FigureNode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Stack;

import javax.microedition.shell.AppClassLoader;

public class Figure {
	Stack<FigureNode> stack = new Stack<>();
	Model data;
	Texture[] textures;
	int textureIndex = -1;
	int pattern;

	@SuppressWarnings("unused")
	public Figure(byte[] b) {
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
	public Figure(String name) throws IOException {
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

	private synchronized void init(byte[] bytes) throws IOException {
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

	private void sortPolygons() {
		Model.Polygon[] polygonsT = data.polygonsT;
		Arrays.sort(polygonsT, (a, b) -> {
			int cmp = Integer.compare(a.blendMode, b.blendMode);
			if (cmp != 0) return cmp;
			cmp = Integer.compare(a.face, b.face);
			if (cmp != 0) return cmp;
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
			int cmp = Integer.compare(a.blendMode, b.blendMode);
			if (cmp != 0) return cmp;
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
	public synchronized final void setPosture(ActionTable actionTable, int action, int frame) {
		if (actionTable == null) {
			throw new NullPointerException();
		} else if (action < 0 || action >= actionTable.getNumActions()) {
			throw new IllegalArgumentException();
		}
		Action act = actionTable.actions[action];
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

	private void applyPattern() {
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

	public final void setTexture(Texture tex) {
		if (tex == null)
			throw new NullPointerException();
		if (tex.isSphere)
			throw new IllegalArgumentException();

		textures = new Texture[]{tex};
		textureIndex = 0;
	}

	public final void setTexture(Texture[] t) {
		if (t == null) throw new NullPointerException();
		if (t.length == 0) throw new IllegalArgumentException();
		for (Texture texture : t) {
			if (texture == null) throw new NullPointerException();
			if (texture.isSphere) throw new IllegalArgumentException();
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

	private void applyBoneAction(Action act, int frame) {
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

	private void fillTexCoordBuffer() {
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

	synchronized FloatBuffer getVertexData() {
		if (data.vertexArray == null) {
			data.vertexArray = ByteBuffer.allocateDirect(data.vertexArrayCapacity)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();
		}
		Utils.fillBuffer(data.vertexArray, data.vertices, data.indices);
		return data.vertexArray;
	}

	synchronized FloatBuffer getNormalsData() {
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
