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

public class Image2D extends Object3D
{
    //------------------------------------------------------------------
    // Static data
    //------------------------------------------------------------------

    public static final int ALPHA               = 96;
    public static final int LUMINANCE           = 97;
    public static final int LUMINANCE_ALPHA     = 98;
    public static final int RGB                 = 99;
    public static final int RGBA                = 100;

    static int tempHandle;

    //------------------------------------------------------------------
    // Constructor(s)
    //------------------------------------------------------------------

    public Image2D(int format, Object image)
    {
        // If image is instance of lcdui.Image then checkAndCreate
        // builds the image and returns the handle to native image,
        // otherwise throws exception Done this way because class of
        // image cannot be checked befor calling super()
        super(Image2D.checkAndCreate(format, image));
    }

    public Image2D(int format, int width, int height, byte[] image)
    {
        super(createHandle(format, width, height, image));
    }

    public Image2D(int format,
                   int width, int height,
                   byte[] image,
                   byte[] palette)
    {
        super(createHandle(format, width, height, image, palette));
    }

    public Image2D(int format, int width, int height)
    {
        super(createHandle(format, width, height));
    }

    Image2D(int handle)
    {
        super(handle);
    }

    //------------------------------------------------------------------
    // Public methods
    //------------------------------------------------------------------

    public void set(int x, int y, int width, int height, byte[] image)
    {
        if (image == null)
        {
            throw new NullPointerException();
        }
        _set(handle, x, y, width, height, image);
    }

    public boolean isMutable()
    {
        return _isMutable(handle);
    }

    public int getFormat()
    {
        return _getFormat(handle);
    }

    public int getWidth()
    {
        return _getWidth(handle);
    }

    public int getHeight()
    {
        return _getHeight(handle);
    }

    //------------------------------------------------------------------
    // Private methods
    //------------------------------------------------------------------

	private static int getBytesPerPixel(int format) {
		switch (format) {
			case ALPHA:		return 1;
			case LUMINANCE:		return 1;
			case LUMINANCE_ALPHA:	return 2;
			case RGB:		return 3;
			case RGBA:		return 4;
			default: throw new RuntimeException("Invalid format on image");
		}
	}

    private static int checkAndCreate(int format, Object image)
    {
        if (image == null)
        {
            throw new NullPointerException();
        }
        if (!(image instanceof javax.microedition.lcdui.Image))
        {
            throw new IllegalArgumentException();
        }

        final int finalFormat = format;
        tempHandle = 0;

	// TODO
        if (image instanceof javax.microedition.lcdui.Image)
        {
            final javax.microedition.lcdui.Image cgfxImage = (javax.microedition.lcdui.Image)image;

	    int bpp = getBytesPerPixel(finalFormat);
	    int[] argbArr = new int[cgfxImage.getWidth() * cgfxImage.getHeight()];
	    final byte[] byteArr = new byte[cgfxImage.getWidth() * cgfxImage.getHeight() * bpp];
	    int index = 0;

	    cgfxImage.getRGB(argbArr, 0, cgfxImage.getWidth(), 0, 0, cgfxImage.getWidth(), cgfxImage.getHeight());

		for (int row = 0; row < cgfxImage.getHeight(); ++row) {
			for (int col = 0; col < cgfxImage.getWidth(); ++col) {
				int packedPixel = argbArr[row * cgfxImage.getWidth() + col];
				if (bpp == 1)
					byteArr[index++] = ((byte) ((packedPixel >> 24) & 0xFF));
				else if (bpp == 2) {
					// TODO
				} else if (bpp >= 3) {
					byteArr[index++] = ((byte) (packedPixel & 0xFF));
					byteArr[index++] = ((byte) ((packedPixel >> 8) & 0xFF));
					byteArr[index++] = ((byte) ((packedPixel >> 16) & 0xFF));
					if (bpp >= 4)
						byteArr[index++] = ((byte) ((packedPixel >> 24) & 0xFF));
				}
			}
		}

            // excute in UI thread
            Platform.executeInUIThread(
                new M3gRunnable()
            {
                void doRun()
                {
                    tempHandle = createHandle(finalFormat, cgfxImage.getWidth(), cgfxImage.getHeight(), byteArr);
                }
            });
        }
        return tempHandle;
    }

    //Platform.heuristicGC();
    //ToolkitInvoker invoker = ToolkitInvoker.getToolkitInvoker();

    // Decide if trueAlpha
    //Image i = (Image)image;
    //boolean trueAlpha = !(i.isMutable() && format == ALPHA);

    //Platform.sync((Image) image);

//        Platform.getUIThread().syncExec(
//                    new Runnable() {
//                        public void run() {
//                                               tempHandle = _ctorImage(/*Interface.getEventSourceHandle(),*/ Interface.getHandle(), finalFormat, /*invoker.imageGetHandle(image)*/ 5);
//                                          }
//                                  });
//          return tempHandle;


    private static int createHandle(int format, int width, int height, byte[] image)
    {
        Platform.heuristicGC();
        return _ctorSizePixels(Interface.getHandle(),
                               format,
                               width, height,
                               image);
    }

    private static int createHandle(int format,
                                    int width, int height,
                                    byte[] image,
                                    byte[] palette)
    {
        Platform.heuristicGC();
        return _ctorSizePixelsPalette(Interface.getHandle(),
                                      format,
                                      width, height,
                                      image, palette);
    }

    private static int createHandle(int format, int width, int height)
    {
        Platform.heuristicGC();
        return _ctorSize(Interface.getHandle(), format, width, height);
    }

    // Native methods
    private native static int _ctorImage(/*int eventSourceHandle,*/
        int hInterface,
        int format,
        int imageHandle);
    private native static int _ctorSizePixels(int hInterface,
            int format,
            int width, int height,
            byte[] image);
    private native static int _ctorSizePixelsPalette(int hInterface,
            int format,
            int width, int height,
            byte[] image,
            byte[] palette);
    private native static int _ctorSize(int hInterface,
                                        int format,
                                        int width, int height);

    private native static void _set(int handle, int x, int y, int width,
                                    int height, byte[] image);
    private native static boolean _isMutable(int handle);
    private native static int _getFormat(int handle);
    private native static int _getWidth(int handle);
    private native static int _getHeight(int handle);
}
