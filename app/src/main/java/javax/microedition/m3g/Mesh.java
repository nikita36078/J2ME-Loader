/*
 * Copyright (c) 2003 Nokia Corporation and/or its subsidiary(-ies).
 * All rights reserved.
 * This component and the accompanying materials are made available
 * under the terms of "Eclipse Public License v1.0"
 * which accompanies this distribution, and is available
 * at the URL "http://www.eclipse.org/legal/epl-v10.html".
 *
 * Initial Contributors:
 * Nokia Corporation - initial contribution.
 *
 * Contributors:
 *
 * Description:
 *
 */

package javax.microedition.m3g;

public class Mesh extends Node {
	//------------------------------------------------------------------
	// Instance data
	//------------------------------------------------------------------

	private VertexBuffer vertices;
	private Appearance[] appearances;
	private IndexBuffer[] triangles;

	static private IndexBuffer[] tempTrianglesArray;
	static private Appearance[] tempAppearanceArray;

	static private IndexBuffer tempTriangles;
	static private Appearance tempAppearance;

	//------------------------------------------------------------------
	// Constructor(s)
	//------------------------------------------------------------------

	Mesh(long handle) {
		super(handle);
		updateReferences();
	}

	public Mesh(VertexBuffer vertices,
				IndexBuffer[] triangles,
				Appearance[] appearances) {
		super(createHandle(vertices, triangles, appearances));
		updateReferences();
	}

	public Mesh(VertexBuffer vertices,
				IndexBuffer triangles,
				Appearance appearance) {
		super(createHandle(vertices, triangles, appearance));
		updateReferences();
	}

	//------------------------------------------------------------------
	// Public methods
	//------------------------------------------------------------------

	public void setAppearance(int index, Appearance appearance) {
		_setAppearance(handle, index, appearance != null ? appearance.handle : 0);
		appearances[index] = appearance;
	}

	public Appearance getAppearance(int index) {
		return appearances[index];
	}

	public IndexBuffer getIndexBuffer(int index) {
		return triangles[index];
	}

	public VertexBuffer getVertexBuffer() {
		return vertices;
	}

	public int getSubmeshCount() {
		return _getSubmeshCount(handle);
	}

	//------------------------------------------------------------------
	// Private methods
	//------------------------------------------------------------------

	static void verifyParams(VertexBuffer vertices,
							 IndexBuffer[] triangles,
							 Appearance[] appearances) {
		if (vertices == null || triangles == null) {
			throw new NullPointerException();
		}
		if (triangles.length == 0
				|| appearances != null && appearances.length < triangles.length) {
			throw new IllegalArgumentException();
		}
		for (IndexBuffer triangle : triangles) {
			if (triangle == null) {
				throw new NullPointerException();
			}
		}
	}

	static void verifyParams(VertexBuffer vertices,
							 IndexBuffer triangles) {
		if (vertices == null || triangles == null) {
			throw new NullPointerException();
		}
	}

	void updateReferences() {
		triangles = new IndexBuffer[_getSubmeshCount(handle)];
		appearances = new Appearance[triangles.length];

		vertices = (VertexBuffer) getInstance(_getVertexBuffer(handle));

		for (int i = 0; i < triangles.length; i++) {
			triangles[i] = (IndexBuffer) getInstance(_getIndexBuffer(handle, i));
			appearances[i] = (Appearance) getInstance(_getAppearance(handle, i));
		}
	}

	static long createHandle(VertexBuffer vertices,
							IndexBuffer[] triangles,
							Appearance[] appearances) {

		tempTrianglesArray = triangles;
		tempAppearanceArray = appearances;

		// Verify parameters
		verifyParams(vertices, triangles, appearances);

		// Init the native side
		long[] hTriangles = new long[triangles.length];
		long[] hAppearances = null;

		if (appearances != null) {
			hAppearances = new long[appearances.length];
		}

		for (int i = 0; i < triangles.length; i++) {
			hTriangles[i] = triangles[i].handle;

			if (appearances != null) {
				hAppearances[i] = appearances[i] != null ? appearances[i].handle : 0;
			}
		}

		long ret = _ctor(Interface.getHandle(),
				vertices.handle,
				hTriangles,
				hAppearances);


		tempTrianglesArray = null;
		tempAppearanceArray = null;

		return ret;

	}

	static long createHandle(VertexBuffer vertices,
							IndexBuffer triangles,
							Appearance appearance) {

		tempTriangles = triangles;
		tempAppearance = appearance;

		verifyParams(vertices, triangles);

		// Init the native side
		long[] hTriangles = new long[1];
		long[] hAppearances = null;

		hTriangles[0] = triangles.handle;

		if (appearance != null) {
			hAppearances = new long[1];
			hAppearances[0] = appearance.handle;
		}

		long ret = _ctor(Interface.getHandle(),
				vertices.handle,
				hTriangles,
				hAppearances);


		tempTriangles = null;
		tempAppearance = null;


		return ret;

	}

	// Native methods
	private static native long _ctor(long hInstance,
									long hVertices,
									long[] hTriangles,
									long[] hAppearances);

	private static native void _setAppearance(long handle, int index, long hAppearance);

	private static native long _getAppearance(long handle, int index);

	private static native long _getIndexBuffer(long handle, int index);

	private static native long _getVertexBuffer(long handle);

	private static native int _getSubmeshCount(long handle);
}
