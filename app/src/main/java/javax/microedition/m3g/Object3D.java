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

import java.util.Vector;

/**
 */
public abstract class Object3D {
	//------------------------------------------------------------------
	// Instance data
	//------------------------------------------------------------------

	long handle;

	private Object userObject;
	private Vector animTracks;
	private Interface iInterface;

	@Override
	protected void finalize() {
		doFinalize();
	}

	//------------------------------------------------------------------
	// Constructor(s)
	//------------------------------------------------------------------

	/**
	 * <p>Only a package private constructor exists for this class.</p>
	 */
	Object3D(long handle) {
		if (handle != 0) {
			this.handle = handle;
			_addRef(handle);

			// Get associated Interafece object and
			// register this instance with that
			iInterface = Interface.getInstance();
			Interface.register(this);

			int n = _getAnimationTrackCount(handle);
			while (n-- > 0) {
				linkAnimTrack((AnimationTrack) getInstance(_getAnimationTrack(handle, n)));
			}
		} else {
			System.out.println("Warning: Object3D constructor called with zero handle");
		}
	}

	//------------------------------------------------------------------
	// Public API
	//------------------------------------------------------------------

	public final Object3D duplicate() {
		int numRef = 1;
		if (this instanceof Node) {
			numRef = ((Node) this)._getSubtreeSize(handle);
		}
		long[] handles = new long[numRef * 2];
		Object3D obj = getInstance(_duplicate(handle, handles));
		for (int i = 0; i < numRef; i++) {
			Object userObj = getInstance(handles[i * 2]).getUserObject();
			Object3D duplicateObj = getInstance(handles[i * 2 + 1]);
			if (userObj != null) {
				duplicateObj.setUserObject(userObj);
			}
		}
		return obj;
	}

	public int getReferences(Object3D[] references) {
		long[] handles = null;
		if (references != null) {
			handles = new long[references.length];
		}
		int num = _getReferences(handle, handles);
		if (references != null) {
			for (int i = 0; i < num; i++) {
				references[i] = getInstance(handles[i]);
			}
		}
		return num;
	}

	public void setUserID(int userID) {
		_setUserID(handle, userID);
	}

	public int getUserID() {
		return _getUserID(handle);
	}

	public Object3D find(int userID) {
		return getInstance(_find(handle, userID));
	}

	public void addAnimationTrack(AnimationTrack animationTrack) {
		_addAnimationTrack(handle, animationTrack.handle);
		linkAnimTrack(animationTrack);
	}

	public AnimationTrack getAnimationTrack(int index) {
		/* Don't try to match the native indexing here -- just call
		 * the native getter */
		return (AnimationTrack) getInstance(_getAnimationTrack(handle, index));
	}

	public void removeAnimationTrack(AnimationTrack animationTrack) {
		if (animationTrack != null) {
			_removeAnimationTrack(handle, animationTrack.handle);

			if (animTracks != null) {
				animTracks.removeElement(animationTrack);
				if (animTracks.isEmpty()) {
					animTracks = null;
				}
			}
		}
	}

	public int getAnimationTrackCount() {
		return _getAnimationTrackCount(handle);
	}

	public final int animate(int time) {
		return _animate(handle, time);
	}

	public void setUserObject(Object obj) {
		userObject = obj;
	}

	public Object getUserObject() {
		return userObject;
	}

	//------------------------------------------------------------------
	// Private methods
	//------------------------------------------------------------------

	static final Object3D getInstance(long handle) {
		return Interface.getObjectInstance(handle);
	}

	/**
	 * Adds a reference to an animation track.
	 */
	private void linkAnimTrack(AnimationTrack track) {
		if (animTracks == null) {
			animTracks = new Vector();
		}
		animTracks.addElement(track);
	}

	/**
	 * Native peer finalization
	 */
	private void doFinalize() {
		if (handle != 0) {
			// finalize native peer
			Platform.finalizeObject(handle, iInterface);
			Interface.deregister(this, iInterface);

			// reset handles
			iInterface = null;
			handle = 0;
		}
	}

	// Native methods
	private static native int _addAnimationTrack(long hObject, long hAnimationTrack);

	private static native void _removeAnimationTrack(long hObject, long hAnimationTrack);

	private static native int _getAnimationTrackCount(long hObject);

	private static native int _animate(long hObject, int time);

	private static native void _setUserID(long hObject, int userID);

	private static native int _getUserID(long hObject);

	private static native void _addRef(long hObject);

	private static native long _getAnimationTrack(long hObject, int index);

	private static native long _duplicate(long hObject, long[] handles);

	private static native int _getReferences(long hObject, long[] handles);

	private static native long _find(long hObject, int userID);
}
