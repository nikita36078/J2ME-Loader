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

public class Light extends Node {
	public static final int AMBIENT = 128;
	public static final int DIRECTIONAL = 129;
	public static final int OMNI = 130;
	public static final int SPOT = 131;

	public Light() {
		super(_ctor(Interface.getHandle()));
	}

	/**
	 */
	Light(long handle) {
		super(handle);
	}

	public void setIntensity(float intensity) {
		_setIntensity(handle, intensity);
	}

	public float getIntensity() {
		return _getIntensity(handle);
	}

	public void setColor(int RGB) {
		_setColor(handle, RGB);
	}

	public int getColor() {
		return _getColor(handle);
	}

	public void setMode(int mode) {
		_setMode(handle, mode);
	}

	public int getMode() {
		return _getMode(handle);
	}

	public void setSpotAngle(float angle) {
		_setSpotAngle(handle, angle);
	}

	public float getSpotAngle() {
		return _getSpotAngle(handle);
	}

	public void setSpotExponent(float exponent) {
		_setSpotExponent(handle, exponent);
	}

	public float getSpotExponent() {
		return _getSpotExponent(handle);
	}

	public void setAttenuation(float constant, float linear, float quadratic) {
		_setAttenuation(handle, constant, linear, quadratic);
	}

	public float getConstantAttenuation() {
		return _getAttenuation(handle, Defs.GET_CONSTANT);
	}

	public float getLinearAttenuation() {
		return _getAttenuation(handle, Defs.GET_LINEAR);
	}

	public float getQuadraticAttenuation() {
		return _getAttenuation(handle, Defs.GET_QUADRATIC);
	}

	// Native methods
	private static native long _ctor(long hInterface);

	private static native void _setIntensity(long handle, float intensity);

	private static native float _getIntensity(long handle);

	private static native void _setColor(long handle, int RGB);

	private static native int _getColor(long handle);

	private static native void _setMode(long handle, int mode);

	private static native int _getMode(long handle);

	private static native void _setSpotAngle(long handle, float angle);

	private static native float _getSpotAngle(long handle);

	private static native void _setSpotExponent(long handle, float exponent);

	private static native float _getSpotExponent(long handle);

	private static native void _setAttenuation(long handle, float constant, float linear, float quadratic);

	private static native float _getAttenuation(long handle, int type);
}
