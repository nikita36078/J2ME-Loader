/*
* Copyright (c) 2009 Nokia Corporation and/or its subsidiary(-ies).
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
#include "javax_microedition_m3g_Image2D.h"

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Image2D__1set
(JNIEnv* aEnv, jclass, jlong aHImage2D, jint aX, jint aY, jint aWidth, jint aHeight, jbyteArray aImageArray)
{
    jbyte* imageArray = NULL;
    if (aImageArray)
    {
        imageArray = aEnv->GetByteArrayElements(aImageArray, NULL);
        if (imageArray == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }
    }
    M3G_DO_LOCK
    m3gSetSubImage((M3GImage)aHImage2D, aX, aY, aWidth, aHeight, aImageArray ? aEnv->GetArrayLength(aImageArray) : 0, imageArray);
    M3G_DO_UNLOCK(aEnv)

    if (imageArray)
    {
        aEnv->ReleaseByteArrayElements(aImageArray, imageArray, JNI_ABORT);
    }
}

#if 0
static void getImageScanline(const QImage* qtImage,
                             M3Gint line,
                             M3Gint bpl,
                             M3Guint *pixels,
                             M3Gbool *trueAlpha)
{

    // Get pointer to start of requested line
    const unsigned char* srcAddr = qtImage->bits() + line * bpl;

    // As input and output are in the same, i.e. #AARRGGBB format,
    // just run mem copy from source to destination to copy one line
    memcpy(pixels, srcAddr, bpl);
    *trueAlpha = false;
}

/*
 * Must be excuted in UI thread
 */
JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Image2D__1ctorImage
(JNIEnv* aEnv, jclass, jlong aHM3g, jint aFormat, jint aImageHandle)
{

    if (aImageHandle != 0)
    {
        Java::GFX::Image* cgfxImage = reinterpret_cast<Java::GFX::Image*>(aImageHandle);
        QImage qtImage = cgfxImage->toImage();
        if (qtImage.isNull())
        {
            return 0;
        }

        // m3g needs format in 32bpp, i.e. in RGB32 or ARGB32 so
        // if format is not one of those convert it here
        if ((qtImage.format() != QImage::Format_ARGB32) || (qtImage.format() != QImage::Format_RGB32))
        {
            qtImage = qtImage.convertToFormat(QImage::Format_ARGB32);
            if (qtImage.isNull())
            {
                return 0;
            }
        }

        // Create Image2D
        M3GImage image;
        M3Gint width = qtImage.width();
        M3Gint height = qtImage.height();
        M3Gint bpl = qtImage.bytesPerLine();

        M3G_DO_LOCK

        image = m3gCreateImage((M3GInterface)aHM3g, (M3GImageFormat)aFormat, width, height, 0);
        if (image == NULL)
        {
            M3G_DO_UNLOCK(aEnv)
            return 0;    // exception automatically raised
        }

        M3Guint *tempPixels = (M3Guint *) malloc(width * 4);
        if (tempPixels == NULL)
        {
            m3gDeleteObject((M3GObject) image);
            M3G_DO_UNLOCK(aEnv)
            return 0;
        }

        // read and write scanline by scanline
        for (M3Gint y = 0; y < height; ++y)
        {
            M3Gbool trueAlpha;
            getImageScanline(&qtImage, y, bpl, tempPixels, &trueAlpha);
            m3gSetImageScanline(image, y, trueAlpha, tempPixels);
        }

        // finally commit image
        m3gCommitImage(image);

        M3G_DO_UNLOCK(aEnv)

        // free memory
        free(tempPixels);

        cgfxImage = NULL;
        return reinterpret_cast<jint>(image);
    }
    return 0;
}
#endif

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Image2D__1getFormat
(JNIEnv* aEnv, jclass, jlong aHImage2D)
{
    M3G_DO_LOCK
    jint format = (jint)m3gGetFormat((M3GImage)aHImage2D);
    M3G_DO_UNLOCK(aEnv)
    return format;
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_Image2D__1ctorSizePixelsPalette
(JNIEnv* aEnv, jclass, jlong aM3g, jint aFormat, jint aWidth, jint aHeight, jbyteArray aImage, jbyteArray aPalette)
{
    M3GImageFormat format = (M3GImageFormat)aFormat;

    int bpp = jsr184BytesPerPixel(format);

    if (validateArray(aEnv, aImage, aWidth * aHeight))
    {
        if (aPalette == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/NullPointerException");
            return 0;
        }
        int paletteLen = aEnv->GetArrayLength(aPalette);
        if ((paletteLen < 256 *(unsigned)bpp) &&
                (paletteLen % (unsigned)bpp != 0))
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/IllegalArgumentException");
            return 0;
        }
        else
        {
            M3G_DO_LOCK

            M3GImage hImg = m3gCreateImage((M3GInterface)aM3g,
                                           format,
                                           aWidth, aHeight,
                                           M3G_PALETTED);
            if (hImg != NULL)
            {
                jbyte* palette = NULL;

                int numEntries = paletteLen / bpp;
                if (numEntries > 256)
                {
                    numEntries = 256;
                }

                jbyte* image = aEnv->GetByteArrayElements(aImage, NULL);
                if (image == NULL)
                {
                    M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
                    M3G_DO_UNLOCK(aEnv)
                    return 0;
                }

                m3gSetImage(hImg, image);

                palette = aEnv->GetByteArrayElements(aPalette, NULL);
                if (palette == NULL)
                {
                    if (image)
                    {
                        aEnv->ReleaseByteArrayElements(aImage, image, JNI_ABORT);
                    }
                    M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
                    M3G_DO_UNLOCK(aEnv)
                    return 0;
                }

                m3gSetImagePalette(hImg, numEntries, palette);
                m3gCommitImage(hImg);

                if (image)
                {
                    aEnv->ReleaseByteArrayElements(aImage, image, JNI_ABORT);
                }
                if (palette)
                {
                    aEnv->ReleaseByteArrayElements(aPalette, palette, JNI_ABORT);
                }
            }
            M3G_DO_UNLOCK(aEnv)
            return ((jlong) hImg);
        }
    }
    return 0;
}

JNIEXPORT jboolean JNICALL Java_javax_microedition_m3g_Image2D__1isMutable
(JNIEnv* aEnv, jclass, jlong aHImage2D)
{
    M3G_DO_LOCK
    jboolean isMutable = (jboolean)m3gIsMutable((M3GImage)aHImage2D);
    M3G_DO_UNLOCK(aEnv)
    return isMutable;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Image2D__1getHeight
(JNIEnv* aEnv, jclass, jlong aHImage2D)
{
    M3G_DO_LOCK
    jint height = (jint)m3gGetHeight((M3GImage)aHImage2D);
    M3G_DO_UNLOCK(aEnv)
    return height;
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_Image2D__1ctorSize
(JNIEnv* aEnv, jclass, jlong aM3g, jint aFormat, jint aWidth, jint aHeight)
{
    M3G_DO_LOCK
    jlong handle = (jlong) m3gCreateImage((M3GInterface)aM3g,
                                           (M3GImageFormat)aFormat,
                                           aWidth, aHeight,
                                           M3G_DYNAMIC|M3G_RENDERING_TARGET);
    M3G_DO_UNLOCK(aEnv)
    return handle;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Image2D__1getWidth
(JNIEnv* aEnv, jclass, jlong aHImage2D)
{
    M3G_DO_LOCK
    jint width = (jint)m3gGetWidth((M3GImage)aHImage2D);
    M3G_DO_UNLOCK(aEnv)
    return width;
}


JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_Image2D__1ctorSizePixels
(JNIEnv* aEnv, jclass, jlong aM3g, jint aFormat, jint aWidth, jint aHeight, jbyteArray aImage)
{
    M3GImageFormat format = (M3GImageFormat)aFormat;

    if (validateArray(aEnv, aImage, jsr184BytesPerPixel(format) * aWidth * aHeight))
    {
        M3G_DO_LOCK

        M3GImage hImg = m3gCreateImage((M3GInterface)aM3g, format, aWidth, aHeight, 0);
        if (hImg != NULL)
        {
            M3GImageFormat format = (M3GImageFormat)aFormat;

            int bpp = jsr184BytesPerPixel(format);
            jbyte* imageScanline = (jbyte*)malloc(aWidth * bpp);
            for (int i=0; i < aHeight; i++)
            {
                aEnv->GetByteArrayRegion(aImage, aWidth * i * bpp, aWidth * bpp, imageScanline);
                m3gSetSubImage(hImg, 0, i, aWidth, 1, aWidth * bpp, imageScanline);
            }
            m3gCommitImage(hImg);

            free(imageScanline);
        }
        M3G_DO_UNLOCK(aEnv)
        return (jlong) hImg;
    }
    return 0;
}
