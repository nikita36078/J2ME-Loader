/*
* Copyright (c) 2003 Nokia Corporation and/or its subsidiary(-ies).
* All rights reserved.
* This component and the accompanying materials are made available
* under the terms of the License "Eclipse Public License v1.0"
* which accompanies this distribution, and is available
* at the URL "http://www.eclipse.org/legal/epl-v10.html".
*
* Initial Contributors:
* Nokia Corporation - initial contribution.
*
* Contributors:
*
* Description: Image interface
*
*/

#ifndef __M3G_IMAGE_H__
#define __M3G_IMAGE_H__

/*!
 * \internal
 * \file
 * \brief Image interface
 */

#include "m3g_object.h"
#include "m3g_gl.h"

#if !defined(M3G_NGL_TEXTURE_API)
typedef struct LargeImageImpl LargeImage;
#endif

/*!
 * \internal
 * \brief Image object
 */
struct M3GImageImpl
{
    Object object;

    M3Gint width, height;
    M3GImageFormat format;
    M3GPixelFormat internalFormat;

    Image *powerOfTwo;

    GLenum glFormat;
#   if !defined(M3G_NGL_TEXTURE_API)
    GLuint texObject;
    LargeImage *large;  /*! \internal \ */
#   endif

    M3GMemObject data;
    M3GMemObject mipData;
    M3Gsizei paletteBytes;

    M3Gbitmask flags            : 8; /* flags defined in m3g_core.h */
    M3Gbitmask special          : 8; /* flags defined below */
    
    M3Gbool pinned              : 1; /* image can not be deleted */
    M3Gbool dirty               : 1;
    M3Gbool powerOfTwoDirty     : 1;
    M3Gbool mipDataMapCount     : 4; /* max. 16 concurrent uses */
#   if !defined(M3G_NGL_TEXTURE_API)
    M3Gbool mipmapsDirty        : 1; /*!< \internal 'dirty' overrides this */
#   endif

    /* For easy cloning of immutable images without replicating the
     * data, we must keep a reference to the original so that it isn't
     * deleted until we are */
    
    Image *copyOf;
};

/*!
 * \internal \brief "special" flag bits */
#define IMG_NPOT        0x01
#define IMG_LARGE       0x02

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static void m3gBindTextureImage(Image *img, M3Genum levelFilter, M3Genum imageFilter);
static void m3gReleaseTextureImage(Image *img);

static Image *m3gGetPowerOfTwoImage(Image *img);

static M3GPixelFormat m3gPixelFormat  (M3GImageFormat format);
static M3Gint         m3gBytesPerPixel(M3GPixelFormat format);

static void m3gConvertPixels(M3GPixelFormat srcFormat, const M3Gubyte *src,
                             M3GPixelFormat dstFormat, M3Gubyte *dst,
                             M3Gsizei count);

static void m3gConvertPixelRect(
    M3GPixelFormat srcFormat, const M3Gubyte *src, M3Gsizei srcStride,
    M3Gsizei width, M3Gsizei height,
    M3GPixelFormat dstFormat, M3Gubyte *dst, M3Gsizei dstStride);

static void m3gCopyImagePixels(Image *dst, const Image *src);
#if !defined(M3G_NGL_TEXTURE_API)
static void m3gCopyFrameBufferImage(Image *dst);
static void m3gDrawFrameBufferImage(RenderContext *ctx, const Image *src);
#endif /* !defined(M3G_NGL_TEXTURE_API)*/

static void m3gInvalidateImage(Image *img);

#if defined(M3G_NGL_TEXTURE_API)
static M3Gbool m3gValidateMipmapMemory(Image *img);
#endif

/*! \internal */
static M3Gsizei m3gGetImageStride(const Image *img);

static M3Gint m3gGetAlpha(Image *image, M3Gint x, M3Gint y);

static M3G_INLINE M3Gbool m3gIsInternallyPaletted(const Image *img)
{
    return (img->paletteBytes > 0);
}

#if 0
/*!
 * \internal
 * \brief Type-safe helper function
 */
static M3G_INLINE void m3gDeleteImage(Image *img)
{
    m3gDeleteObject((Object*) img);
}
#endif

#endif /*__M3G_IMAGE_H__*/
