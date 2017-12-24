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

public class Background extends Object3D
{
    //------------------------------------------------------------------
    // Static data
    //------------------------------------------------------------------

    public static final int BORDER = 32;
    public static final int REPEAT = 33;

    //------------------------------------------------------------------
    // Instance data
    //------------------------------------------------------------------

    private Image2D image;

    //------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------

    public Background()
    {
        super(_ctor(Interface.getHandle()));
    }

    /**
     */
    Background(int handle)
    {
        super(handle);
        image = (Image2D) getInstance(_getImage(handle));
    }

    //------------------------------------------------------------------
    // Public methods
    //------------------------------------------------------------------

    public void setColor(int ARGB)
    {
        _setColor(handle, ARGB);
    }

    public int getColor()
    {
        return _getColor(handle);
    }

    public void setImage(Image2D image)
    {
        _setImage(handle, image != null ? image.handle : 0);
        this.image = image;
    }

    public Image2D getImage()
    {
        return image;
    }

    public void setImageMode(int modeX, int modeY)
    {
        _setImageMode(handle, modeX, modeY);
    }

    public int getImageModeX()
    {
        return _getImageMode(handle, Defs.GET_MODEX);
    }

    public int getImageModeY()
    {
        return _getImageMode(handle, Defs.GET_MODEY);
    }

    public void setColorClearEnable(boolean enable)
    {
        _enable(handle, Defs.SETGET_COLORCLEAR, enable);
    }

    public void setDepthClearEnable(boolean enable)
    {
        _enable(handle, Defs.SETGET_DEPTHCLEAR, enable);
    }

    public boolean isColorClearEnabled()
    {
        return _isEnabled(handle, Defs.SETGET_COLORCLEAR);
    }

    public boolean isDepthClearEnabled()
    {
        return _isEnabled(handle, Defs.SETGET_DEPTHCLEAR);
    }

    public void setCrop(int cropX, int cropY, int width, int height)
    {
        _setCrop(handle, cropX, cropY, width, height);
    }

    public int getCropX()
    {
        return _getCrop(handle, Defs.GET_CROPX);
    }

    public int getCropY()
    {
        return _getCrop(handle, Defs.GET_CROPY);
    }

    public int getCropWidth()
    {
        return _getCrop(handle, Defs.GET_CROPWIDTH);
    }

    public int getCropHeight()
    {
        return _getCrop(handle, Defs.GET_CROPHEIGHT);
    }

    //------------------------------------------------------------------
    // Private methods
    //------------------------------------------------------------------

    // Native functions
    private static native int _ctor(int hInterface);
    private static native void _setColor(int handle, int ARGB);
    private static native int _getColor(int handle);
    private static native void _setImage(int handle, int hImage);
    private static native int  _getImage(int handle);
    private static native void _setImageMode(int handle, int modeX, int modeY);
    private static native int _getImageMode(int handle, int which);
    private static native void _enable(int handle, int which, boolean enable);
    private static native boolean _isEnabled(int handle, int which);
    private static native void _setCrop(int handle, int cropX, int cropY, int width, int height);
    private static native int _getCrop(int handle, int which);
}
