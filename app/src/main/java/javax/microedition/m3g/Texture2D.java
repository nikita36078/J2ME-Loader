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

public class Texture2D extends Transformable {
	//------------------------------------------------------------------
	// Static data
	//------------------------------------------------------------------

	public static final int FILTER_BASE_LEVEL = 208;
	public static final int FILTER_LINEAR = 209;
	public static final int FILTER_NEAREST = 210;
	public static final int FUNC_ADD = 224;
	public static final int FUNC_BLEND = 225;
	public static final int FUNC_DECAL = 226;
	public static final int FUNC_MODULATE = 227;
	public static final int FUNC_REPLACE = 228;
	public static final int WRAP_CLAMP = 240;
	public static final int WRAP_REPEAT = 241;

	//------------------------------------------------------------------
	// Instance data
	//------------------------------------------------------------------

	private Image2D image;

	//------------------------------------------------------------------
	// Constructor(s)
	//------------------------------------------------------------------

	public Texture2D(final Image2D image) {
		super(_ctor(Interface.getHandle(), image != null ? image.handle : 0));
		this.image = image;
	}

	/**
	 */
	Texture2D(long handle) {
		super(handle);
		image = (Image2D) getInstance(_getImage(handle));
	}

	//------------------------------------------------------------------
	// Public methods
	//------------------------------------------------------------------

	public void setImage(final Image2D image) {
		_setImage(handle, image != null ? image.handle : 0);
		this.image = image;
	}

	public Image2D getImage() {
		return image;
	}

	public void setFiltering(int levelFilter, int imageFilter) {
		_setFiltering(handle, levelFilter, imageFilter);
	}

	public void setWrapping(int wrapS, int wrapT) {
		_setWrapping(handle, wrapS, wrapT);
	}

	public int getWrappingS() {
		return _getWrappingS(handle);
	}

	public int getWrappingT() {
		return _getWrappingT(handle);
	}

	public void setBlending(int func) {
		_setBlending(handle, func);
	}

	public int getBlending() {
		return _getBlending(handle);
	}

	public void setBlendColor(int RGB) {
		_setBlendColor(handle, RGB);
	}

	public int getBlendColor() {
		return _getBlendColor(handle);
	}

	// M3G 1.1 Maintenance release getters

	public int getImageFilter() {
		return _getImageFilter(handle);
	}

	public int getLevelFilter() {
		return _getLevelFilter(handle);
	}

	// Native methods
	private native static long _ctor(long hInterface, long imageHandle);

	private native static void _setImage(long handle, long imageHandle);

	private native static long _getImage(long handle);

	private native static void _setFiltering(long handle, int levelFilter, int imageFilter);

	private native static void _setWrapping(long handle, int wrapS, int wrapT);

	private native static int _getWrappingS(long handle);

	private native static int _getWrappingT(long handle);

	private native static void _setBlending(long handle, int func);

	private native static int _getBlending(long handle);

	private native static void _setBlendColor(long handle, int RGB);

	private native static int _getBlendColor(long handle);

	// M3G 1.1 Maintenance release getters
	private native static int _getImageFilter(long handle);

	private native static int _getLevelFilter(long handle);
}
