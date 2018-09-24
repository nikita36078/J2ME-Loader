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

public abstract class Node extends Transformable {
	//------------------------------------------------------------------
	// Static data
	//------------------------------------------------------------------

	public static final int NONE = 144;
	public static final int ORIGIN = 145;
	public static final int X_AXIS = 146;
	public static final int Y_AXIS = 147;
	public static final int Z_AXIS = 148;

	//------------------------------------------------------------------
	// Instance data
	//------------------------------------------------------------------

	// The node references are kept on the Java side to avoid having
	// to replicate garbage collection in the native implementation.
	// Instead, we let Java manage Node reachability detection and
	// deletion.

	private Node parent;
	private Node zRef, yRef;

	//------------------------------------------------------------------
	// Constructor(s)
	//------------------------------------------------------------------

	/**
	 * There is only a package private constructor. Applications can not extend
	 * this class directly.</p>
	 */
	Node(long handle) {
		super(handle);
		parent = (Node) getInstance(_getParent(handle));
		zRef = (Node) getInstance(_getZRef(handle));
		yRef = (Node) getInstance(_getYRef(handle));
	}

	//------------------------------------------------------------------
	// Public methods
	//------------------------------------------------------------------

	public Node getParent() {
		return parent;
	}

	public boolean getTransformTo(Node target, Transform transform) {
		return _getTransformTo(handle,
				target.handle,
				transform != null ? transform.matrix : null);
	}

	public void setAlignment(Node zReference, int zTarget,
							 Node yReference, int yTarget) {
		_setAlignment(handle,
				zReference != null ? zReference.handle : 0, zTarget,
				yReference != null ? yReference.handle : 0, yTarget);
		zRef = zReference;
		yRef = yReference;
	}

	public void setAlphaFactor(float alphaFactor) {
		_setAlphaFactor(handle, alphaFactor);
	}

	public float getAlphaFactor() {
		return _getAlphaFactor(handle);
	}

	public void setRenderingEnable(boolean enable) {
		_enable(handle, Defs.SETGET_RENDERING, enable);
	}

	public boolean isRenderingEnabled() {
		return _isEnabled(handle, Defs.SETGET_RENDERING);
	}

	public void setPickingEnable(boolean enable) {
		_enable(handle, Defs.SETGET_PICKING, enable);
	}

	public boolean isPickingEnabled() {
		return _isEnabled(handle, Defs.SETGET_PICKING);
	}

	public void setScope(int id) {
		_setScope(handle, id);
	}

	public int getScope() {
		return _getScope(handle);
	}

	public final void align(Node reference) {
		_align(handle, reference != null ? reference.handle : 0);
	}

	// M3G 1.1 Maintenance release getters

	public Node getAlignmentReference(int axis) {
		switch (axis) {
			case Y_AXIS:
				return yRef;
			case Z_AXIS:
				return zRef;
			default:
				throw new IllegalArgumentException();
		}
	}

	public int getAlignmentTarget(int axis) {
		return _getAlignmentTarget(handle, axis);
	}


	//------------------------------------------------------------------
	// Private methods
	//------------------------------------------------------------------

	void setParent(Node parent) {
		this.parent = parent;
	}

	// Native methods
	private static native boolean _getTransformTo(long handle,
												  long hTarget,
												  byte[] transform);

	private static native void _align(long handle, long refHandle);

	private static native void _setAlignment(long handle,
											 long hZReference, int zTarget,
											 long hYReference, int yTarget);

	private static native void _setAlphaFactor(long handle, float alphaFactor);

	private static native float _getAlphaFactor(long handle);

	private static native void _enable(long handle, int which, boolean enable);

	private static native boolean _isEnabled(long handle, int which);

	private static native void _setScope(long handle, int id);

	private static native int _getScope(long handle);

	private static native long _getParent(long handle);

	private static native long _getZRef(long handle);

	private static native long _getYRef(long handle);

	static native int _getSubtreeSize(long handle);

	// M3G 1.1 Maintenance release getters
	private static native int _getAlignmentTarget(long handle, int axis);
}
