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

/**
 *
 */
public abstract class Transformable extends Object3D {
	//------------------------------------------------------------------
	// Constructor(s)
	//------------------------------------------------------------------

	Transformable(long handle) {
		super(handle);
	}

	//------------------------------------------------------------------
	// Public methods
	//------------------------------------------------------------------


	public void setOrientation(float angle, float ax, float ay, float az) {
		_setOrientation(handle, angle, ax, ay, az, true);
	}

	public void postRotate(float angle, float ax, float ay, float az) {
		_setOrientation(handle, angle, ax, ay, az, false);
	}

	public void preRotate(float angle, float ax, float ay, float az) {
		_preRotate(handle, angle, ax, ay, az);
	}

	public void getOrientation(float[] angleAxis) {
		_getOrientation(handle, angleAxis);
	}

	public void setScale(float sx, float sy, float sz) {
		_setScale(handle, sx, sy, sz, true);
	}

	public void scale(float sx, float sy, float sz) {
		_setScale(handle, sx, sy, sz, false);
	}

	public void getScale(float[] xyz) {
		_getScale(handle, xyz);
	}

	public void setTranslation(float tx, float ty, float tz) {
		_setTranslation(handle, tx, ty, tz, true);
	}

	public void translate(float tx, float ty, float tz) {
		_setTranslation(handle, tx, ty, tz, false);
	}

	public void getTranslation(float[] xyz) {
		_getTranslation(handle, xyz);
	}

	public void setTransform(Transform transform) {
		_setTransform(handle, (transform != null) ? transform.matrix : null);
	}

	public void getTransform(Transform transform) {
		_getTransform(handle, transform.matrix);
	}

	public void getCompositeTransform(Transform transform) {
		_getComposite(handle, transform.matrix);
	}

	//------------------------------------------------------------------
	// Private methods
	//------------------------------------------------------------------

	private static native void _setOrientation(long handle,
											   float angle,
											   float ax, float ay, float az,
											   boolean absolute);

	private static native void _preRotate(long handle,
										  float angle,
										  float ax, float ay, float az);

	private static native void _getOrientation(long handle, float[] angleAxis);

	private static native void _setScale(long handle,
										 float sx, float sy, float sz,
										 boolean absolute);

	private static native void _getScale(long handle, float[] scale);

	private static native void _setTranslation(long handle,
											   float tx, float ty, float tz,
											   boolean absolute);

	private static native void _getTranslation(long handle, float[] translation);

	private static native void _setTransform(long handle, byte[] transform);

	private static native void _getTransform(long handle, byte[] transform);

	private static native void _getComposite(long handle, byte[] transform);
}
