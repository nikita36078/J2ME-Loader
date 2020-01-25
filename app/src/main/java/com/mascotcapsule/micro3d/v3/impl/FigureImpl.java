package com.mascotcapsule.micro3d.v3.impl;

import com.mascotcapsule.micro3d.v3.AffineTrans;
import com.mascotcapsule.micro3d.v3.Vector3D;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class FigureImpl {

	private final static int MAGNITUDE_8BIT = 0;
	private final static int MAGNITUDE_10BIT = 1;
	private final static int MAGNITUDE_13BIT = 2;
	private final static int MAGNITUDE_16BIT = 3;
	private final static int[] DIRECTIONS = new int[]{
			0x40, 0, 0,
			0, 0x40, 0,
			0, 0, 0x40,
			0xC0, 0, 0,
			0, 0xC0, 0,
			0, 0, 0xC0
	};
	private ArrayList<Vector3D> vertices = new ArrayList<>();
	private ArrayList<Vector3D> normals = new ArrayList<>();
	private ArrayList<PolygonT3> triangleFacesT = new ArrayList<>();
	private ArrayList<PolygonT4> quadFacesT = new ArrayList<>();
	private ArrayList<Bone> bones = new ArrayList<>();
	public FloatBuffer vboPolyT;
	public FloatBuffer vboPolyF;
	public int[] texturedPolygons;
	private ArrayList<PolygonF4> quadFacesF = new ArrayList<>();
	private ArrayList<PolygonF3> triangleFacesF = new ArrayList<>();
	private int numPattern;

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
		int num_texture = bis.readUnsignedShort();
		numPattern = bis.readUnsignedShort();
		int num_color = bis.readUnsignedShort();
		System.out.printf("num_polyf3=%d num_polyf4=%d num_texture=%d num_pattern=%d num_color=%d\n",
				num_polyf3, num_polyf4, num_texture, numPattern, num_color);

		texturedPolygons = new int[num_texture];
		for (int i = 0; i < numPattern; i++) {
			int num_unk_polyf3 = bis.readUnsignedShort();
			int num_unk_polyf4 = bis.readUnsignedShort();
			for (int j = 0; j < num_texture; j++) {
				int num_textured_polyt3 = bis.readUnsignedShort();
				int num_textured_polyt4 = bis.readUnsignedShort();
				// Don't support an external patterns for now
				texturedPolygons[j] += num_textured_polyt3 + num_textured_polyt4 * 2;
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
					vertices.add(new Vector3D(x, y, z));
				}
			} else if (magnitude == MAGNITUDE_10BIT) {
				for (int i = 0; i < count; i++) {
					int x = bis.readBitsSigned(10);
					int y = bis.readBitsSigned(10);
					int z = bis.readBitsSigned(10);
					vertices.add(new Vector3D(x, y, z));
				}
			} else if (magnitude == MAGNITUDE_13BIT) {
				for (int i = 0; i < count; i++) {
					int x = bis.readBitsSigned(13);
					int y = bis.readBitsSigned(13);
					int z = bis.readBitsSigned(13);
					vertices.add(new Vector3D(x, y, z));
				}
			} else if (magnitude == MAGNITUDE_16BIT) {
				for (int i = 0; i < count; i++) {
					int x = bis.readBitsSigned(16);
					int y = bis.readBitsSigned(16);
					int z = bis.readBitsSigned(16);
					vertices.add(new Vector3D(x, y, z));
				}
			} else {
				throw new RuntimeException();
			}
		}
	}

	private void unpackNormals(BitInputStream bis, int num_vertices) throws IOException {
		while (normals.size() < num_vertices) {
			int type = bis.readBitsSigned(7);
			int x, y, z;
			if (type == -64) {
				int direction = bis.readBits(3);
				if (direction > 5) {
					throw new RuntimeException("Invalid direction");
				}
				x = DIRECTIONS[direction * 3];
				y = DIRECTIONS[direction * 3 + 1];
				z = DIRECTIONS[direction * 3 + 2];
			} else {
				x = type;
				y = bis.readBitsSigned(7);
				int z_negative = bis.readBits(1);

				int temp = 4096 - x * x - y * y;
				if (temp >= 0) {
					z = (int) Math.sqrt(temp) * ((z_negative > 0) ? -1 : 1);
				} else {
					z = 0;
				}
			}
			normals.add(new Vector3D(x, y, z));
		}
	}

	private void unpackPolyF(BitInputStream bis, int num_color, int num_polyf3, int num_polyf4) throws IOException {
		Color[] colors = new Color[num_color];
		int attribute_bits = bis.readBits(8);
		int vertex_index_bits = bis.readBits(8);
		int color_bits = bis.readBits(8);
		int color_id_bits = bis.readBits(8);
		bis.readBits(8);

		for (int i = 0; i < num_color; i++) {
			int r = bis.readBits(color_bits);
			int g = bis.readBits(color_bits);
			int b = bis.readBits(color_bits);
			colors[i] = new Color(r, g, b, 0xFF);
		}

		for (int i = 0; i < num_polyf3; i++) {
			int attribute = bis.readBits(attribute_bits);
			int a = bis.readBits(vertex_index_bits);
			int b = bis.readBits(vertex_index_bits);
			int c = bis.readBits(vertex_index_bits);

			int color_id = bis.readBits(color_id_bits);
			triangleFacesF.add(new PolygonF3(a, b, c, colors[color_id], attribute));
		}

		for (int i = 0; i < num_polyf4; i++) {
			int attribute = bis.readBits(attribute_bits);
			int a = bis.readBits(vertex_index_bits);
			int b = bis.readBits(vertex_index_bits);
			int c = bis.readBits(vertex_index_bits);
			int d = bis.readBits(vertex_index_bits);

			int color_id = bis.readBits(color_id_bits);
			quadFacesF.add(new PolygonF4(a, b, c, d, colors[color_id], attribute));
		}
	}

	private void unpackPolyT(BitInputStream bis, int num_polyt3, int num_polyt4) throws IOException {
		int attribute_bits = bis.readBits(8);
		int vertex_index_bits = bis.readBits(8);
		int uv_bits = bis.readBits(8);
		bis.readBits(8);

		for (int i = 0; i < num_polyt3; i++) {
			int attribute = bis.readBits(attribute_bits);
			int a = bis.readBits(vertex_index_bits);
			int b = bis.readBits(vertex_index_bits);
			int c = bis.readBits(vertex_index_bits);

			int u1 = bis.readBits(uv_bits);
			int v1 = bis.readBits(uv_bits);
			int u2 = bis.readBits(uv_bits);
			int v2 = bis.readBits(uv_bits);
			int u3 = bis.readBits(uv_bits);
			int v3 = bis.readBits(uv_bits);
			triangleFacesT.add(new PolygonT3(a, b, c, u1, v1, u2, v2, u3, v3, attribute));
		}

		for (int i = 0; i < num_polyt4; i++) {
			int attribute = bis.readBits(attribute_bits);
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
			quadFacesT.add(new PolygonT4(a, b, c, d, u1, v1, u2, v2, u3, v3, u4, v4, attribute));
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

			AffineTrans parentMatrix = getParentMatrix(parent);
			AffineTrans mtx = new AffineTrans(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23);
			mtx.mul(parentMatrix, mtx);
			bones.add(new Bone(parent, mtx, bone_vertices_sum, bone_vertices + bone_vertices_sum));

			bone_vertices_sum += bone_vertices;
		}
		return bone_vertices_sum;
	}

	private AffineTrans getParentMatrix(int parent) {
		if (parent >= bones.size()) {
			throw new RuntimeException("Format error (invalid parent index). Please report this bug");
		}

		AffineTrans parent_mtx;
		if (parent < 0) {
			parent_mtx = new AffineTrans();
			parent_mtx.setIdentity();
		} else {
			parent_mtx = bones.get(parent).mtx;
		}

		return parent_mtx;
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

			for (int i = bone.start; i < bone.end; i++) {
				Vector3D vertex = vertices.get(i);
				Vector3D transformed = bone.mtx.transform(vertex);
				vertex.set(transformed);
			}
		}
	}

	private void createVerticesArray(int num_polyt3, int num_polyt4, int num_polyf3, int num_polyf4) {

		vboPolyT = ByteBuffer.allocateDirect(num_polyt3 * 15 * 4 + num_polyt4 * 30 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
		vboPolyF = ByteBuffer.allocateDirect(num_polyf3 * 21 * 4 + num_polyf4 * 42 * 4)
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		for (int i = 0; i < quadFacesT.size(); i++) {
			PolygonT4 polygon4 = quadFacesT.get(i);
			Vector3D a = vertices.get(polygon4.a);
			Vector3D b = vertices.get(polygon4.b);
			Vector3D c = vertices.get(polygon4.c);
			Vector3D d = vertices.get(polygon4.d);

			vboPolyT.put(a.x);
			vboPolyT.put(a.y);
			vboPolyT.put(a.z);
			vboPolyT.put(polygon4.u1);
			vboPolyT.put(polygon4.v1);

			vboPolyT.put(b.x);
			vboPolyT.put(b.y);
			vboPolyT.put(b.z);
			vboPolyT.put(polygon4.u2);
			vboPolyT.put(polygon4.v2);

			vboPolyT.put(c.x);
			vboPolyT.put(c.y);
			vboPolyT.put(c.z);
			vboPolyT.put(polygon4.u3);
			vboPolyT.put(polygon4.v3);

			//Create second triangle

			vboPolyT.put(c.x);
			vboPolyT.put(c.y);
			vboPolyT.put(c.z);
			vboPolyT.put(polygon4.u3);
			vboPolyT.put(polygon4.v3);

			vboPolyT.put(b.x);
			vboPolyT.put(b.y);
			vboPolyT.put(b.z);
			vboPolyT.put(polygon4.u2);
			vboPolyT.put(polygon4.v2);

			vboPolyT.put(d.x);
			vboPolyT.put(d.y);
			vboPolyT.put(d.z);
			vboPolyT.put(polygon4.u4);
			vboPolyT.put(polygon4.v4);
		}
		for (int i = 0; i < triangleFacesT.size(); i++) {
			PolygonT3 polygon3 = triangleFacesT.get(i);
			Vector3D a = vertices.get(polygon3.a);
			Vector3D b = vertices.get(polygon3.b);
			Vector3D c = vertices.get(polygon3.c);

			vboPolyT.put(a.x);
			vboPolyT.put(a.y);
			vboPolyT.put(a.z);
			vboPolyT.put(polygon3.u1);
			vboPolyT.put(polygon3.v1);

			vboPolyT.put(b.x);
			vboPolyT.put(b.y);
			vboPolyT.put(b.z);
			vboPolyT.put(polygon3.u2);
			vboPolyT.put(polygon3.v2);

			vboPolyT.put(c.x);
			vboPolyT.put(c.y);
			vboPolyT.put(c.z);
			vboPolyT.put(polygon3.u3);
			vboPolyT.put(polygon3.v3);
		}
		for (int i = 0; i < quadFacesF.size(); i++) {
			PolygonF4 polygon4 = quadFacesF.get(i);
			Vector3D a = vertices.get(polygon4.a);
			Vector3D b = vertices.get(polygon4.b);
			Vector3D c = vertices.get(polygon4.c);
			Vector3D d = vertices.get(polygon4.d);

			vboPolyF.put(a.x);
			vboPolyF.put(a.y);
			vboPolyF.put(a.z);
			vboPolyF.put(polygon4.color.r);
			vboPolyF.put(polygon4.color.g);
			vboPolyF.put(polygon4.color.b);
			vboPolyF.put(polygon4.color.a);

			vboPolyF.put(b.x);
			vboPolyF.put(b.y);
			vboPolyF.put(b.z);
			vboPolyF.put(polygon4.color.r);
			vboPolyF.put(polygon4.color.g);
			vboPolyF.put(polygon4.color.b);
			vboPolyF.put(polygon4.color.a);

			vboPolyF.put(c.x);
			vboPolyF.put(c.y);
			vboPolyF.put(c.z);
			vboPolyF.put(polygon4.color.r);
			vboPolyF.put(polygon4.color.g);
			vboPolyF.put(polygon4.color.b);
			vboPolyF.put(polygon4.color.a);

			//Create second triangle

			vboPolyF.put(c.x);
			vboPolyF.put(c.y);
			vboPolyF.put(c.z);
			vboPolyF.put(polygon4.color.r);
			vboPolyF.put(polygon4.color.g);
			vboPolyF.put(polygon4.color.b);
			vboPolyF.put(polygon4.color.a);

			vboPolyF.put(b.x);
			vboPolyF.put(b.y);
			vboPolyF.put(b.z);
			vboPolyF.put(polygon4.color.r);
			vboPolyF.put(polygon4.color.g);
			vboPolyF.put(polygon4.color.b);
			vboPolyF.put(polygon4.color.a);

			vboPolyF.put(d.x);
			vboPolyF.put(d.y);
			vboPolyF.put(d.z);
			vboPolyF.put(polygon4.color.r);
			vboPolyF.put(polygon4.color.g);
			vboPolyF.put(polygon4.color.b);
			vboPolyF.put(polygon4.color.a);
		}
		for (int i = 0; i < triangleFacesF.size(); i++) {
			PolygonF3 polygon3 = triangleFacesF.get(i);
			Vector3D a = vertices.get(polygon3.a);
			Vector3D b = vertices.get(polygon3.b);
			Vector3D c = vertices.get(polygon3.c);

			vboPolyF.put(a.x);
			vboPolyF.put(a.y);
			vboPolyF.put(a.z);
			vboPolyF.put(polygon3.color.r);
			vboPolyF.put(polygon3.color.g);
			vboPolyF.put(polygon3.color.b);
			vboPolyF.put(polygon3.color.a);

			vboPolyF.put(b.x);
			vboPolyF.put(b.y);
			vboPolyF.put(b.z);
			vboPolyF.put(polygon3.color.r);
			vboPolyF.put(polygon3.color.g);
			vboPolyF.put(polygon3.color.b);
			vboPolyF.put(polygon3.color.a);

			vboPolyF.put(c.x);
			vboPolyF.put(c.y);
			vboPolyF.put(c.z);
			vboPolyF.put(polygon3.color.r);
			vboPolyF.put(polygon3.color.g);
			vboPolyF.put(polygon3.color.b);
			vboPolyF.put(polygon3.color.a);
		}
		vboPolyT.position(0);
		vboPolyF.position(0);
	}

	public int getNumPattern() {
		return numPattern;
	}
}
