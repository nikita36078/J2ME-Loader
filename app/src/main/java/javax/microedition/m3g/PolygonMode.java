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

public class PolygonMode extends Object3D
{

    //------------------------------------------------------------------
    // Static data
    //------------------------------------------------------------------

    public static final int CULL_BACK = 0xA0;
    public static final int CULL_FRONT = CULL_BACK + 1;
    public static final int CULL_NONE = CULL_FRONT + 1;
    public static final int SHADE_FLAT = 0xA4;
    public static final int SHADE_SMOOTH = SHADE_FLAT + 1;
    public static final int WINDING_CCW = 0xA8;
    public static final int WINDING_CW = WINDING_CCW + 1;

    //------------------------------------------------------------------
    // Constructor(s)
    //------------------------------------------------------------------

    public PolygonMode()
    {
        super(_ctor(Interface.getHandle()));
    }

    /**
     */
    PolygonMode(int handle)
    {
        super(handle);
    }

    //------------------------------------------------------------------
    // Public methods
    //------------------------------------------------------------------

    public void setCulling(int mode)
    {
        _setCulling(handle, mode);
    }

    public int getCulling()
    {
        return _getCulling(handle);
    }

    public void setWinding(int mode)
    {
        _setWinding(handle, mode);
    }

    public int getWinding()
    {
        return _getWinding(handle);
    }

    public void setShading(int mode)
    {
        _setShading(handle, mode);
    }

    public int getShading()
    {
        return _getShading(handle);
    }

    public void setTwoSidedLightingEnable(boolean enable)
    {
        _setTwoSidedLightingEnable(handle, enable);
    }

    public boolean isTwoSidedLightingEnabled()
    {
        return _isTwoSidedLightingEnabled(handle);
    }

    public void setLocalCameraLightingEnable(boolean enable)
    {
        _setLocalCameraLightingEnable(handle, enable);
    }

    public void setPerspectiveCorrectionEnable(boolean enable)
    {
        _setPerspectiveCorrectionEnable(handle, enable);
    }

    // M3G 1.1 Maintenance release getters
    public boolean isLocalCameraLightingEnabled()
    {
        return _isLocalCameraLightingEnabled(handle);
    }

    public boolean isPerspectiveCorrectionEnabled()
    {
        return _isPerspectiveCorrectionEnabled(handle);
    }


    //------------------------------------------------------------------
    // Private methods
    //------------------------------------------------------------------

    private native static int _ctor(int hInterface);
    private native static void _setLocalCameraLightingEnable(int handle, boolean enable);
    private native static void _setPerspectiveCorrectionEnable(int handle, boolean enable);
    private native static void _setCulling(int handle, int mode);
    private native static int _getCulling(int handle);
    private native static void _setWinding(int handle, int mode);
    private native static int _getWinding(int handle);
    private native static void _setShading(int handle, int mode);
    private native static int _getShading(int handle);
    private native static void _setTwoSidedLightingEnable(int handle, boolean enable);
    private native static boolean _isTwoSidedLightingEnabled(int handle);

    // M3G 1.1 Maintenance release getters
    private native static boolean _isLocalCameraLightingEnabled(int handle);
    private native static boolean _isPerspectiveCorrectionEnabled(int handle);

}

