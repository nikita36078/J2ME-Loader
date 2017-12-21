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

public class Sprite3D extends Node
{
    //------------------------------------------------------------------
    // Instance data
    //------------------------------------------------------------------

    private Image2D    image;
    private Appearance appearance;

    //------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------

    public Sprite3D(boolean scaled, Image2D image, Appearance appearance)
    {
        super(_ctor(Interface.getHandle(),
                    scaled,
                    image != null ? image.handle : 0,
                    appearance != null ? appearance.handle : 0));
        this.image = image;
        this.appearance = appearance;
    }

    /**
     */
    Sprite3D(int handle)
    {
        super(handle);
        image = (Image2D)getInstance(_getImage(handle));
        appearance = (Appearance)getInstance(_getAppearance(handle));
    }

    //------------------------------------------------------------------
    // Public methods
    //------------------------------------------------------------------

    public boolean isScaled()
    {
        return _isScaled(handle);
    }

    public void setAppearance(Appearance appearance)
    {
        _setAppearance(handle, appearance != null ? appearance.handle : 0);
        this.appearance = appearance;
    }

    public Appearance getAppearance()
    {
        return appearance;
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

    // Native methods

    private static native int _ctor(int hInterface,
                                    boolean scaled,
                                    int hImage,
                                    int hAppearance);
    private static native boolean _isScaled(int handle);
    private static native void _setAppearance(int handle, int hAppearance);
    private static native void _setImage(int handle, int hImage);
    private static native void _setCrop(int handle,
                                        int cropX, int cropY,
                                        int width, int height);
    private static native int _getCrop(int handle, int which);
    private static native int _getAppearance(int handle);
    private static native int _getImage(int handle);
}
