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

public class MorphingMesh extends Mesh {
	//------------------------------------------------------------------
	// Instance data
	//------------------------------------------------------------------

	private VertexBuffer[] targets;

	static private IndexBuffer[] tempTrianglesArray;
	static private Appearance[] tempAppearanceArray;

	static private IndexBuffer tempTriangles;
	static private Appearance tempAppearance;

	//------------------------------------------------------------------
	// Constructor(s)
	//------------------------------------------------------------------

	public MorphingMesh(
			VertexBuffer base,
			VertexBuffer[] targets,
			IndexBuffer triangles,
			Appearance appearance) {
		super(createHandle(base, targets, triangles, appearance));
		this.targets = new VertexBuffer[targets.length];
		System.arraycopy(targets, 0, this.targets, 0, targets.length);
	}

	public MorphingMesh(
			VertexBuffer base,
			VertexBuffer[] targets,
			IndexBuffer[] triangles,
			Appearance[] appearances) {
		super(createHandle(base, targets, triangles, appearances));
		this.targets = new VertexBuffer[targets.length];
		System.arraycopy(targets, 0, this.targets, 0, targets.length);
	}

	/**
	 */
	MorphingMesh(long handle) {
		super(handle);
		targets = new VertexBuffer[_getMorphTargetCount(handle)];
		for (int i = 0; i < targets.length; i++) {
			targets[i] = (VertexBuffer) getInstance(_getMorphTarget(handle, i));
		}
	}

	public VertexBuffer getMorphTarget(int index) {
		return targets[index];
	}

	public int getMorphTargetCount() {
		return _getMorphTargetCount(handle);
	}

	public void setWeights(float[] weights) {
		_setWeights(handle, weights);
	}

	public void getWeights(float[] weights) {
		_getWeights(handle, weights);
	}

	//------------------------------------------------------------------
	// Private methods
	//------------------------------------------------------------------

	static long createHandle(VertexBuffer base,
							VertexBuffer[] targets,
							IndexBuffer triangles,
							Appearance appearance) {

		tempTriangles = triangles;
		tempAppearance = appearance;

		verifyParams(base, triangles);

		long[] hTargets = new long[targets.length];
		long[] hTriangles;
		long[] hAppearances = null;

		for (int i = 0; i < targets.length; i++) {
			hTargets[i] = targets[i].handle;
		}

		hTriangles = new long[1];
		hTriangles[0] = triangles.handle;

		if (appearance != null) {
			hAppearances = new long[1];
			hAppearances[0] = appearance.handle;
		}

		long ret = _ctor(Interface.getHandle(),
				base.handle,
				hTargets,
				hTriangles,
				hAppearances);

		tempTriangles = null;
		tempAppearance = null;

		return ret;
	}

	static long createHandle(VertexBuffer base,
							VertexBuffer[] targets,
							IndexBuffer[] triangles,
							Appearance[] appearances) {

		tempTrianglesArray = triangles;
		tempAppearanceArray = appearances;


		verifyParams(base, triangles, appearances);

		long[] hTargets = new long[targets.length];
		long[] hTriangles;
		long[] hAppearances = null;

		for (int i = 0; i < targets.length; i++) {
			hTargets[i] = targets[i].handle;
		}

		hTriangles = new long[triangles.length];

		if (appearances != null) {
			hAppearances = new long[appearances.length];
		}

		for (int i = 0; i < triangles.length; i++) {
			hTriangles[i] = triangles[i].handle;

			if (hAppearances != null) {
				hAppearances[i] = appearances[i] != null ? appearances[i].handle : 0;
			}
		}

		long ret = _ctor(Interface.getHandle(),
				base.handle,
				hTargets,
				hTriangles,
				hAppearances);

		tempTrianglesArray = null;
		tempAppearanceArray = null;

		return ret;

	}

	// Native methods
	private static native long _ctor(long hInstance,
									long handle,
									long[] hTargets,
									long[] hTriangles,
									long[] hAppearances);

	private static native void _setWeights(long handle, float[] weights);

	private static native void _getWeights(long handle, float[] weights);

	private static native long _getMorphTarget(long handle, int index);

	private static native int _getMorphTargetCount(long handle);
}
