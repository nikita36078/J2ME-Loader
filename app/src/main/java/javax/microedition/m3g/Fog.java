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

public class Fog extends Object3D
{
    public static final int EXPONENTIAL = 80;
    public static final int LINEAR = 81;

    public Fog()
    {
        super(_ctor(Interface.getHandle()));
    }

    /**
     */
    Fog(int handle)
    {
        super(handle);
    }

    public void setMode(int mode)
    {
        _setMode(handle, mode);
    }

    public int getMode()
    {
        return _getMode(handle);
    }

    public void setLinear(float near, float far)
    {
        _setLinear(handle, near, far);
    }

    public float getNearDistance()
    {
        return _getDistance(handle, Defs.GET_NEAR);
    }

    public float getFarDistance()
    {
        return _getDistance(handle, Defs.GET_FAR);
    }

    public void setDensity(float density)
    {
        _setDensity(handle, density);
    }

    public float getDensity()
    {
        return _getDensity(handle);
    }

    public void setColor(int RGB)
    {
        _setColor(handle, RGB);
    }

    public int getColor()
    {
        return _getColor(handle);
    }

    // Native methods
    private static native int _ctor(int hInterface);
    private static native void _setMode(int handle, int mode);
    private static native int _getMode(int handle);
    private static native void _setLinear(int handle, float near, float far);
    private static native float _getDistance(int handle, int which);
    private static native void _setDensity(int handle, float density);
    private static native float _getDensity(int handle);
    private static native void _setColor(int handle, int RGB);
    private static native int _getColor(int handle);
}
