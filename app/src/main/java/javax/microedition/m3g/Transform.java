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

public class Transform {
	//------------------------------------------------------------------
	// Static data
	//------------------------------------------------------------------

	//------------------------------------------------------------------
	// Instance data
	//------------------------------------------------------------------

	// Check size from m3g_math.h Matrix
	byte[] matrix = new byte[72];

	//------------------------------------------------------------------
	// Constructor(s)
	//------------------------------------------------------------------

	public Transform() {
		if (!Platform.uiThreadAvailable()) {
			throw new Error("UI thread not initialized");
		}
		setIdentity();
	}

	/**
	 */
	public Transform(Transform other) {
		if (!Platform.uiThreadAvailable()) {
			throw new Error("UI thread not initialized");
		}
		set(other);
	}

	//------------------------------------------------------------------
	// Public methods
	//------------------------------------------------------------------

	public void setIdentity() {
		_setIdentity(matrix);
	}

	public void set(Transform transform) {
		System.arraycopy(transform.matrix, 0,
				this.matrix, 0,
				this.matrix.length);
	}

	public void set(float[] matrix) {
		_setMatrix(this.matrix, matrix);
	}

	public void get(float[] matrix) {
		_getMatrix(this.matrix, matrix);
	}

	public void invert() {
		_invert(matrix);
	}

	public void transpose() {
		_transpose(matrix);
	}

	public void postMultiply(Transform transform) {
		_mul(this.matrix, this.matrix, transform.matrix);
	}

	public void postScale(float sx, float sy, float sz) {
		_scale(matrix, sx, sy, sz);
	}

	/**
	 */
	public void postRotate(float angle, float ax, float ay, float az) {
		_rotate(matrix, angle, ax, ay, az);
	}

	/**
	 */
	public void postRotateQuat(float qx, float qy, float qz, float qw) {
		_rotateQuat(matrix, qx, qy, qz, qw);
	}

	/**
	 */
	public void postTranslate(float tx, float ty, float tz) {
		_translate(matrix, tx, ty, tz);
	}

	/**
	 */
	public void transform(float[] v) {
		if ((v.length % 4) != 0) {
			throw new IllegalArgumentException();
		}

		if (v.length != 0) {
			_transformTable(matrix, v);
		}
	}

	/**
	 */
	public void transform(VertexArray in, float[] out, boolean W) {
		if (in == null || out == null) {
			throw new NullPointerException();
		}

		_transformArray(matrix, in.handle, out, W);
	}

	//------------------------------------------------------------------
	// Private methods
	//------------------------------------------------------------------

	// Native methods
	private static native void _mul(byte[] prod, byte[] left, byte[] right);

	private static native void _setIdentity(byte[] matrix);

	private static native void _setMatrix(byte[] matrix, float[] srcMatrix);

	private static native void _getMatrix(byte[] matrix, float[] dstMatrix);

	private static native void _invert(byte[] matrix);

	private static native void _transpose(byte[] matrix);

	private static native void _rotate(byte[] matrix, float angle, float ax, float ay, float az);

	private static native void _rotateQuat(byte[] matrix, float qx, float qy, float qz, float qw);

	private static native void _scale(byte[] matrix, float sx, float sy, float sz);

	private static native void _translate(byte[] matrix, float tx, float ty, float tz);

	private static native void _transformTable(byte[] matrix, float[] v);

	private static native void _transformArray(byte[] matrix, long handle, float[] out, boolean W);
}
