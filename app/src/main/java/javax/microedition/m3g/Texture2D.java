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

import javax.microedition.lcdui.Image;

import java.io.*;

public class Texture2D extends Transformable
{
    //------------------------------------------------------------------
    // Static data
    //------------------------------------------------------------------

    public static final int FILTER_BASE_LEVEL = 208;
    public static final int FILTER_LINEAR     = 209;
    public static final int FILTER_NEAREST    = 210;
    public static final int FUNC_ADD      = 224;
    public static final int FUNC_BLEND    = 225;
    public static final int FUNC_DECAL    = 226;
    public static final int FUNC_MODULATE = 227;
    public static final int FUNC_REPLACE  = 228;
    public static final int WRAP_CLAMP  = 240;
    public static final int WRAP_REPEAT = 241;

    //------------------------------------------------------------------
    // Instance data
    //------------------------------------------------------------------

    private Image2D image;

    //------------------------------------------------------------------
    // Constructor(s)
    //------------------------------------------------------------------

    public Texture2D(final Image2D image)
    {
        super(_ctor(Interface.getHandle(), image != null ? image.handle : 0));
        this.image = image;
    }

    /**
     */
    Texture2D(int handle)
    {
        super(handle);
        image = (Image2D) getInstance(_getImage(handle));
    }

    //------------------------------------------------------------------
    // Public methods
    //------------------------------------------------------------------

    public void setImage(final Image2D image)
    {
        _setImage(handle, image != null ? image.handle : 0);
        this.image = image;
    }

    public Image2D getImage()
    {
        return image;
    }

    public void setFiltering(int levelFilter, int imageFilter)
    {
        _setFiltering(handle, levelFilter, imageFilter);
    }

    public void setWrapping(int wrapS, int wrapT)
    {
        _setWrapping(handle, wrapS, wrapT);
    }

    public int getWrappingS()
    {
        return _getWrappingS(handle);
    }

    public int getWrappingT()
    {
        return _getWrappingT(handle);
    }

    public void setBlending(int func)
    {
        _setBlending(handle, func);
    }

    public int getBlending()
    {
        return _getBlending(handle);
    }

    public void setBlendColor(int RGB)
    {
        _setBlendColor(handle, RGB);
    }

    public int getBlendColor()
    {
        return _getBlendColor(handle);
    }

    // M3G 1.1 Maintenance release getters

    public int getImageFilter()
    {
        return _getImageFilter(handle);
    }

    public int getLevelFilter()
    {
        return _getLevelFilter(handle);
    }

    // Native methods
    private native static int _ctor(int hInterface, int imageHandle);

    private native static void _setImage(int handle, int imageHandle);
    private native static int  _getImage(int handle);

    private native static void _setFiltering(int handle, int levelFilter, int imageFilter);
    private native static void _setWrapping(int handle, int wrapS, int wrapT);
    private native static int _getWrappingS(int handle);
    private native static int _getWrappingT(int handle);
    private native static void _setBlending(int handle, int func);
    private native static int _getBlending(int handle);
    private native static void _setBlendColor(int handle, int RGB);
    private native static int _getBlendColor(int handle);

    // M3G 1.1 Maintenance release getters
    private native static int _getImageFilter(int handle);
    private native static int _getLevelFilter(int handle);
}
