package com.mascotcapsule.micro3d.v3.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class FigureImpl {

	public FloatBuffer triangleBuffer;

	private final static int MAGNITUDE_8BIT = 0;
	private final static int MAGNITUDE_10BIT = 1;
	private final static int MAGNITUDE_13BIT = 2;
	private final static int MAGNITUDE_16BIT = 3;
	private ArrayList<Vertex> vertices = new ArrayList<>();
	private ArrayList<Normal> normals = new ArrayList<>();
	private ArrayList<Polygon3> triangleFaces = new ArrayList<>();
	private ArrayList<Polygon4> quadFaces = new ArrayList<>();
	private ArrayList<Bone> bones = new ArrayList<>();

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public FigureImpl(InputStream inputStream) throws IOException {
		BitInputStream bis = new BitInputStream(inputStream, ByteOrder.LITTLE_ENDIAN);
		byte[] mbacMagic = new byte[2];
		byte[] mbacVersion = new byte[2];
		bis.read(mbacMagic);
		bis.read(mbacVersion);
		byte[] magic = {'M', 'B'};
		if (!Arrays.equals(mbacMagic, magic)) {
			throw new RuntimeException("Not a MBAC file");
		}
		byte[] version = {5, 0};
		if (!Arrays.equals(mbacVersion, version)) {
			throw new RuntimeException("Unsupported version");
		}
		int vertexformat = bis.readUnsignedByte();
		int normalformat = bis.readUnsignedByte();
		int polygonformat = bis.readUnsignedByte();
		int boneformat = bis.readUnsignedByte();
		System.out.printf("vertexformat=%d normalformat=%d polygonformat=%d boneformat=%d\n",
				vertexformat, normalformat, polygonformat, boneformat);

		int num_vertices = bis.readUnsignedShort();
		int num_polyt3 = bis.readUnsignedShort();
		int num_polyt4 = bis.readUnsignedShort();
		int num_bones = bis.readUnsignedShort();
		System.out.printf("num_vertices=%d num_polyt3=%d num_polyt4=%d num_bones=%d\n",
				num_vertices, num_polyt3, num_polyt4, num_bones);

		if (polygonformat != 3) {
			throw new RuntimeException("Unsupported polygonformat. Please report this bug.");
		}

		int num_polyf3 = bis.readUnsignedShort();
		int num_polyf4 = bis.readUnsignedShort();
		int matcnt = bis.readUnsignedShort();
		int unk21 = bis.readUnsignedShort();
		int num_color = bis.readUnsignedShort();
		System.out.printf("num_polyf3=%d num_polyf4=%d matcnt=%d unk21=%d num_color=%d\n",
				num_polyf3, num_polyf4, matcnt, unk21, num_color);

		for (int i = 0; i < unk21; i++) {
			int unk1 = bis.readUnsignedShort();
			int unk2 = bis.readUnsignedShort();
			for (int j = 0; j < matcnt; j++) {
				int unk3 = bis.readUnsignedShort();
				int unk4 = bis.readUnsignedShort();
			}
		}

		if (vertexformat != 2) {
			throw new RuntimeException("Unsupported vertexformat. Please report this bug.");
		}

		unpackVertices(bis, num_vertices);
		if (vertices.size() != num_vertices) {
			System.out.println("Vertices loading error");
		}

		if (normalformat > 0) {
			if (normalformat != 2) {
				throw new RuntimeException("Unsupported normalformat. Please report this bug.");
			}
			bis.clearBitCache();
			unpackNormals(bis, num_vertices);
			if (normals.size() != num_vertices) {
				System.out.println("Normals loading error");
			}
		}
		bis.clearBitCache();

		if (num_polyf3 + num_polyf4 > 0) {
			unpackPolyF(bis, num_color, num_polyf3, num_polyf4);
		}

		if (num_polyt3 + num_polyt4 > 0) {
			unpackPolyT(bis, num_polyt3, num_polyt4);
		}

		bis.clearBitCache();

		if (unpackBones(bis, num_bones) != num_vertices) {
			throw new RuntimeException("Format error (bone_vertices_sum). Please report this bug.");
		}

		applyBoneTransform();

		unpackTrailer(bis);
		if (bis.read() != -1) {
			System.out.println("uninterpreted bytes in file");
		}

		createVerticesArray(num_polyt3, num_polyt4, num_polyf3, num_polyf4);

		bis.close();
	}

	private void unpackVertices(BitInputStream bis, int num_vertices) throws IOException {
		while (vertices.size() < num_vertices) {
			int header = bis.readBits(8);
			int magnitude = header >> 6;
			int count = (header & 0x3F) + 1;

			if (magnitude == MAGNITUDE_8BIT) {
				for (int i = 0; i < count; i++) {
					int x = bis.readBitsSigned(8);
					int y = bis.readBitsSigned(8);
					int z = bis.readBitsSigned(8);
					vertices.add(new Vertex(x, y, z));
				}
			} else if (magnitude == MAGNITUDE_10BIT) {
				for (int i = 0; i < count; i++) {
					int x = bis.readBitsSigned(10);
					int y = bis.readBitsSigned(10);
					int z = bis.readBitsSigned(10);
					vertices.add(new Vertex(x, y, z));
				}
			} else if (magnitude == MAGNITUDE_13BIT) {
				for (int i = 0; i < count; i++) {
					int x = bis.readBitsSigned(13);
					int y = bis.readBitsSigned(13);
					int z = bis.readBitsSigned(13);
					vertices.add(new Vertex(x, y, z));
				}
			} else if (magnitude == MAGNITUDE_16BIT) {
				for (int i = 0; i < count; i++) {
					int x = bis.readBitsSigned(16);
					int y = bis.readBitsSigned(16);
					int z = bis.readBitsSigned(16);
					vertices.add(new Vertex(x, y, z));
				}
			} else {
				throw new RuntimeException();
			}
		}
	}

	private void unpackNormals(BitInputStream bis, int num_vertices) throws IOException {
		while (normals.size() < num_vertices) {
			int x = bis.readBitsSigned(7);
			int y = 0;
			int z = 0;
			if (x == -64) {
				int direction = bis.readBits(3);
			} else {
				x = x / 64;
				y = bis.readBitsSigned(7) / 64;
				int z_negative = bis.readBits(1);

				if (1 - x * x - y * y >= 0) {
					z = (int) Math.sqrt(1 - x * x - y * y) * ((z_negative > 0) ? -1 : 1);
				} else {
					z = 0;
				}
			}
			normals.add(new Normal(x, y, z));
		}
	}

	private void unpackPolyF(BitInputStream bis, int num_color, int num_polyf3, int num_polyf4) throws IOException {
		int unknown_bits = bis.readBits(8);
		int vertex_index_bits = bis.readBits(8);
		int color_bits = bis.readBits(8);
		int color_id_bits = bis.readBits(8);
		bis.readBits(8);

		for (int i = 0; i < num_color; i++) {
			int r = bis.readBits(color_bits);
			int g = bis.readBits(color_bits);
			int b = bis.readBits(color_bits);
		}

		for (int i = 0; i < num_polyf3; i++) {
			int unknown = bis.readBits(unknown_bits);
			int a = bis.readBits(vertex_index_bits);
			int b = bis.readBits(vertex_index_bits);
			int c = bis.readBits(vertex_index_bits);

			int color_id = bis.readBits(color_id_bits);
			triangleFaces.add(new Polygon3(a, b, c));
		}

		for (int i = 0; i < num_polyf4; i++) {
			int unknown = bis.readBits(unknown_bits);
			int a = bis.readBits(vertex_index_bits);
			int b = bis.readBits(vertex_index_bits);
			int c = bis.readBits(vertex_index_bits);
			int d = bis.readBits(vertex_index_bits);

			int color_id = bis.readBits(color_id_bits);
			quadFaces.add(new Polygon4(a, b, c, d));
		}
	}

	private void unpackPolyT(BitInputStream bis, int num_polyt3, int num_polyt4) throws IOException {
		int unknown_bits = bis.readBits(8);
		int vertex_index_bits = bis.readBits(8);
		int uv_bits = bis.readBits(8);
		bis.readBits(8);

		if (unknown_bits > 8) {
			throw new RuntimeException("Format error. Please report this bug.");
		}

		for (int i = 0; i < num_polyt3; i++) {
			int unknown = bis.readBits(unknown_bits);
			int a = bis.readBits(vertex_index_bits);
			int b = bis.readBits(vertex_index_bits);
			int c = bis.readBits(vertex_index_bits);

			int u1 = bis.readBits(uv_bits);
			int v1 = bis.readBits(uv_bits);
			int u2 = bis.readBits(uv_bits);
			int v2 = bis.readBits(uv_bits);
			int u3 = bis.readBits(uv_bits);
			int v3 = bis.readBits(uv_bits);
			triangleFaces.add(new Polygon3(a, b, c));
		}

		for (int i = 0; i < num_polyt4; i++) {
			int unknown = bis.readBits(unknown_bits);
			int a = bis.readBits(vertex_index_bits);
			int b = bis.readBits(vertex_index_bits);
			int c = bis.readBits(vertex_index_bits);
			int d = bis.readBits(vertex_index_bits);

			int u1 = bis.readBits(uv_bits);
			int v1 = bis.readBits(uv_bits);
			int u2 = bis.readBits(uv_bits);
			int v2 = bis.readBits(uv_bits);
			int u3 = bis.readBits(uv_bits);
			int v3 = bis.readBits(uv_bits);
			int u4 = bis.readBits(uv_bits);
			int v4 = bis.readBits(uv_bits);
			quadFaces.add(new Polygon4(a, b, c, d));
		}
	}

	private int unpackBones(BitInputStream bis, int num_bones) throws IOException {
		int bone_vertices_sum = 0;
		for (int i = 0; i < num_bones; i++) {
			int bone_vertices = bis.readUnsignedShort();
			int parent = bis.readShort();

			int m00 = bis.readShort();
			int m01 = bis.readShort();
			int m02 = bis.readShort();
			int m03 = bis.readShort();
			int m10 = bis.readShort();
			int m11 = bis.readShort();
			int m12 = bis.readShort();
			int m13 = bis.readShort();
			int m20 = bis.readShort();
			int m21 = bis.readShort();
			int m22 = bis.readShort();
			int m23 = bis.readShort();

			if (parent < -1) {
				throw new RuntimeException("Format error (negative parent). Please report this bug");
			}

			int[] mtx = new int[]{m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23};
			bones.add(new Bone(parent, mtx, bone_vertices, bone_vertices + bone_vertices_sum));

			bone_vertices_sum += bone_vertices;
		}
		return bone_vertices_sum;
	}

	private int[] getBoneMatrix(Bone bone) {
		int parent = bone.parent;
		if (parent >= bones.size()) {
			throw new RuntimeException("Format error (invalid parent index). Please report this bug");
		}

		int[] parent_mtx;
		if (parent < 0) {
			parent_mtx = new int[]{
					4096, 0, 0, 0,
					0, 4096, 0, 0,
					0, 0, 4096, 0};
		} else {
			parent_mtx = bone.mtx;
		}

		int[] a = parent_mtx;
		int[] b = bone.mtx;

		int m00 = (a[0] * b[0] + a[1] * b[4] + a[2] * b[8]) / 4096;
		int m01 = (a[0] * b[1] + a[1] * b[5] + a[2] * b[9]) / 4096;
		int m02 = (a[0] * b[2] + a[1] * b[6] + a[2] * b[10]) / 4096;
		int m03 = (a[0] * b[3] + a[1] * b[7] + a[2] * b[11]) / 4096 + a[3];

		int m10 = (a[4] * b[0] + a[5] * b[4] + a[6] * b[8]) / 4096;
		int m11 = (a[4] * b[1] + a[5] * b[5] + a[6] * b[9]) / 4096;
		int m12 = (a[4] * b[2] + a[5] * b[6] + a[6] * b[10]) / 4096;
		int m13 = (a[4] * b[3] + a[5] * b[7] + a[6] * b[11]) / 4096 + a[7];

		int m20 = (a[8] * b[0] + a[9] * b[4] + a[10] * b[8]) / 4096;
		int m21 = (a[8] * b[1] + a[9] * b[5] + a[10] * b[9]) / 4096;
		int m22 = (a[8] * b[2] + a[9] * b[6] + a[10] * b[10]) / 4096;
		int m23 = (a[8] * b[3] + a[9] * b[7] + a[10] * b[11]) / 4096 + a[11];

		return new int[]{m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23};
	}

	private void unpackTrailer(BitInputStream bis) throws IOException {
		StringBuilder wordStr = new StringBuilder();
		for (int i = 0; i < 2; i++) {
			byte[] key = new byte[2];
			bis.read(key);
			for (int j = 0; j < 4; j++) {
				byte[] word = new byte[2];

				for (int k = 0; k < 2; k++) {
					int encrypted_byte = bis.readUnsignedByte();
					word[k] = (byte) (((encrypted_byte ^ key[k]) + 127) & 0xff);
				}
				wordStr.append(new String(word));
			}
		}
		System.out.println("Trailer: " + wordStr);
	}

	private void applyBoneTransform() {
		for (Bone bone : bones) {
			int[] mtx = getBoneMatrix(bone);

			for (int i = bone.start; i < bone.end; i++) {
				Vertex vertex = vertices.get(i);
				int x = vertex.x;
				int y = vertex.y;
				int z = vertex.z;
				int[] m = Arrays.copyOf(mtx, mtx.length);
				vertex.set((m[0] * x + m[1] * y + m[2] * z) / 4096 + m[3],
						(m[4] * x + m[5] * y + m[6] * z) / 4096 + m[7],
						(m[8] * x + m[9] * y + m[10] * z) / 4096 + m[11]);
			}
		}
	}

	private void createVerticesArray(int num_polyt3, int num_polyt4, int num_polyf3, int num_polyf4) {
		float[] verts3 = new float[(num_polyt3 + num_polyf3) * 3 * 3 + (num_polyt4 + num_polyf4) * 3 * 6];

		int num = 0;
		for (int i = 0; i < quadFaces.size(); i++) {
			Polygon4 polygon4 = quadFaces.get(i);
			verts3[num++] = vertices.get(polygon4.a).x;
			verts3[num++] = vertices.get(polygon4.a).y;
			verts3[num++] = vertices.get(polygon4.a).z;

			verts3[num++] = vertices.get(polygon4.b).x;
			verts3[num++] = vertices.get(polygon4.b).y;
			verts3[num++] = vertices.get(polygon4.b).z;

			verts3[num++] = vertices.get(polygon4.c).x;
			verts3[num++] = vertices.get(polygon4.c).y;
			verts3[num++] = vertices.get(polygon4.c).z;

			//Create second triangle

			verts3[num++] = vertices.get(polygon4.c).x;
			verts3[num++] = vertices.get(polygon4.c).y;
			verts3[num++] = vertices.get(polygon4.c).z;

			verts3[num++] = vertices.get(polygon4.b).x;
			verts3[num++] = vertices.get(polygon4.b).y;
			verts3[num++] = vertices.get(polygon4.b).z;

			verts3[num++] = vertices.get(polygon4.d).x;
			verts3[num++] = vertices.get(polygon4.d).y;
			verts3[num++] = vertices.get(polygon4.d).z;
		}
		for (int i = 0; i < triangleFaces.size(); i++) {
			Polygon3 polygon3 = triangleFaces.get(i);
			verts3[num++] = vertices.get(polygon3.a).x;
			verts3[num++] = vertices.get(polygon3.a).y;
			verts3[num++] = vertices.get(polygon3.a).z;

			verts3[num++] = vertices.get(polygon3.b).x;
			verts3[num++] = vertices.get(polygon3.b).y;
			verts3[num++] = vertices.get(polygon3.b).z;

			verts3[num++] = vertices.get(polygon3.c).x;
			verts3[num++] = vertices.get(polygon3.c).y;
			verts3[num++] = vertices.get(polygon3.c).z;
		}
		ByteBuffer bb = ByteBuffer.allocateDirect(verts3.length * 4);
		bb.order(ByteOrder.nativeOrder());
		triangleBuffer = bb.asFloatBuffer();
		triangleBuffer.put(verts3);
		triangleBuffer.position(0);
	}
}
