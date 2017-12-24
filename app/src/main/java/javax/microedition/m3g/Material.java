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

public class Material extends Object3D
{
    //------------------------------------------------------------------
    // Static data
    //------------------------------------------------------------------

    public static final int AMBIENT  = 1024;
    public static final int DIFFUSE  = 2048;
    public static final int EMISSIVE = 4096;
    public static final int SPECULAR = 8192;

    //------------------------------------------------------------------
    // Constructor(s)
    //------------------------------------------------------------------

    public Material()
    {
        super(_ctor(Interface.getHandle()));
    }

    /**
     */
    Material(int handle)
    {
        super(handle);
    }

    //------------------------------------------------------------------
    // Public methods
    //------------------------------------------------------------------

    public void setColor(int target, int ARGB)
    {
        _setColor(handle, target, ARGB);
    }

    public int getColor(int target)
    {
        return _getColor(handle, target);
    }

    public void setShininess(float shininess)
    {
        _setShininess(handle, shininess);
    }

    public float getShininess()
    {
        return _getShininess(handle);
    }

    public void setVertexColorTrackingEnable(boolean enable)
    {
        _setVertexColorTrackingEnable(handle, enable);
    }

    public boolean isVertexColorTrackingEnabled()
    {
        return _isVertexColorTrackingEnabled(handle);
    }

    //------------------------------------------------------------------
    // Private methods
    //------------------------------------------------------------------

    private native static int _ctor(int hInstance);
    private native static void _setColor(int handle, int target, int ARGB);
    private native static int _getColor(int handle, int target);
    private native static void _setShininess(int handle, float shininess);
    private native static float _getShininess(int handle);
    private native static void _setVertexColorTrackingEnable(int handle, boolean enable);
    private native static boolean _isVertexColorTrackingEnabled(int handle);
}
