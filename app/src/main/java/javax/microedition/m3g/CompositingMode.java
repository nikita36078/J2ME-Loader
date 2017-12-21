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

public class CompositingMode extends Object3D
{
    //------------------------------------------------------------------
    // Static data
    //------------------------------------------------------------------

    public static final int ALPHA       = 64;
    public static final int ALPHA_ADD   = 65;
    public static final int MODULATE    = 66;
    public static final int MODULATE_X2 = 67;
    public static final int REPLACE     = 68;

    //------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------

    public CompositingMode()
    {
        super(_ctor(Interface.getHandle()));
    }

    /**
     */
    CompositingMode(int handle)
    {
        super(handle);
    }

    //------------------------------------------------------------------
    // Public methods
    //------------------------------------------------------------------

    public void setBlending(int mode)
    {
        _setBlending(handle, mode);
    }

    public int getBlending()
    {
        return _getBlending(handle);
    }

    public void setAlphaThreshold(float threshold)
    {
        _setAlphaThreshold(handle, threshold);
    }

    public float getAlphaThreshold()
    {
        return _getAlphaThreshold(handle);
    }

    public void setAlphaWriteEnable(boolean enable)
    {
        _setAlphaWriteEnable(handle, enable);
    }

    public boolean isAlphaWriteEnabled()
    {
        return _isAlphaWriteEnabled(handle);
    }

    public void setColorWriteEnable(boolean enable)
    {
        _enableColorWrite(handle, enable);
    }

    public boolean isColorWriteEnabled()
    {
        return _isColorWriteEnabled(handle);
    }

    public void setDepthWriteEnable(boolean enable)
    {
        _enableDepthWrite(handle, enable);
    }

    public boolean isDepthWriteEnabled()
    {
        return _isDepthWriteEnabled(handle);
    }

    public void setDepthTestEnable(boolean enable)
    {
        _enableDepthTest(handle, enable);
    }

    public boolean isDepthTestEnabled()
    {
        return _isDepthTestEnabled(handle);
    }

    public void setDepthOffset(float factor, float units)
    {
        _setDepthOffset(handle, factor, units);
    }

    public float getDepthOffsetFactor()
    {
        return _getDepthOffsetFactor(handle);
    }

    public float getDepthOffsetUnits()
    {
        return _getDepthOffsetUnits(handle);
    }

    //------------------------------------------------------------------
    // Private methods
    //------------------------------------------------------------------

    private native static int _ctor(int hInterface);
    private native static void _setBlending(int handle, int mode);
    private native static int _getBlending(int handle);
    private native static void _setAlphaThreshold(int handle, float threshold);
    private native static float _getAlphaThreshold(int handle);
    private native static void _setAlphaWriteEnable(int handle, boolean enable);
    private native static boolean _isAlphaWriteEnabled(int handle);
    private native static void _enableDepthTest(int handle, boolean enable);
    private native static boolean _isDepthTestEnabled(int handle);
    private native static void _enableDepthWrite(int handle, boolean enable);
    private native static boolean _isDepthWriteEnabled(int handle);
    private native static void _enableColorWrite(int handle, boolean enable);
    private native static boolean _isColorWriteEnabled(int handle);
    private native static void _setDepthOffset(int handle, float factor, float units);
    private native static float _getDepthOffsetFactor(int handle);
    private native static float _getDepthOffsetUnits(int handle);
}
