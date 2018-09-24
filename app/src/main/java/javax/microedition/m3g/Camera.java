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

public class Camera extends Node {
	//------------------------------------------------------------------
	// Static data
	//------------------------------------------------------------------

	public static final int GENERIC = 48;
	public static final int PARALLEL = 49;
	public static final int PERSPECTIVE = 50;

	//------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------

	public Camera() {
		super(_ctor(Interface.getHandle()));
	}

	/**
	 */
	Camera(long handle) {
		super(handle);
	}

	//------------------------------------------------------------------
	// Public methods
	//------------------------------------------------------------------

	public void setParallel(float height, float aspectRatio, float near, float far) {
		_setParallel(handle, height, aspectRatio, near, far);
	}

	public void setPerspective(float fovy, float aspectRatio, float near, float far) {
		_setPerspective(handle, fovy, aspectRatio, near, far);
	}

	public void setGeneric(Transform transform) {
		_setGeneric(handle, transform.matrix);
	}

	public int getProjection(Transform transform) {
		return _getProjectionAsTransform(handle, transform != null ? transform.matrix : null);
	}

	public int getProjection(float[] params) {
		return _getProjectionAsParams(handle, params);
	}

	//------------------------------------------------------------------
	// Private methods
	//------------------------------------------------------------------

	// Native methods
	private static native long _ctor(long hInterface);

	private static native void _setParallel(long handle, float height, float aspectRatio, float near, float far);

	private static native void _setPerspective(long handle, float fovy, float aspectRatio, float near, float far);

	private static native void _setGeneric(long handle, byte[] transform);

	private static native int _getProjectionAsTransform(long handle, byte[] transform);

	private static native int _getProjectionAsParams(long handle, float[] params);
}
