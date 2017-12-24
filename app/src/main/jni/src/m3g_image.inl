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
* Description: Image implementation for the OpenGL ES API
*
*/


/*!
 * \internal
 * \file
 * \brief Image implementation for the OpenGL ES API
 *
 * $Id: m3g_image.inl,v 1.11 2006/03/15 13:26:36 roimela Exp $
 */

#if defined(M3G_NGL_TEXTURE_API)
#   error This file is for the OES API only
#endif

/*----------------------------------------------------------------------
 * Data types
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Additional data for a "large" image
 *
 * A large image is an image that is larger than the maximum texture
 * size.  They basically get split into a bunch of smaller textures so
 * that we can use them for drawing backgrounds via OpenGL ES.  Some
 * optimization is done to make sure we don't waste excessive amounts
 * of memory in doing so.
 */
struct LargeImageImpl
{
    M3Gsizei tilesX, tilesY;
    M3Gint tileWidth, tileHeight;
    M3Gbool dirty;

    /* The size of the tile texture name array is set dynamically upon
     * allocation, and it *must* be the last field in the
     * structure! */
    GLuint tileNames[1];
};

/*----------------------------------------------------------------------
 * Private functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Queries whether an image can be paletted internally or not
 */
static M3Gbool m3gSupportedPaletteFormat(M3GImageFormat format)
{
    return (format == M3G_RGB || format == M3G_RGBA);
}

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Matches an M3G pixel format with a GL texture format
 */
static GLenum m3gGetGLFormat(M3GPixelFormat format)
{
    switch (format) {
    case M3G_A8:
        return GL_ALPHA;
    case M3G_L8:
        return GL_LUMINANCE;
    case M3G_LA8:
        return GL_LUMINANCE_ALPHA;
    case M3G_RGB8:
    case M3G_RGB8_32:
    case M3G_BGR8_32:
        return GL_RGB;
    case M3G_RGBA8:
    case M3G_BGRA8:
    case M3G_ARGB8:
        return GL_RGBA;
    case M3G_PALETTE8_RGB8:
        return GL_PALETTE8_RGB8_OES;
    case M3G_PALETTE8_RGBA8:
        return GL_PALETTE8_RGBA8_OES;
    default:
        return 0;
    }
}


/*!
 * \internal
 * \brief Destroys the additional data of a "large" image
 *
 * This can be called to save (OpenGL) memory at any time -- the data
 * will be recreated when necessary.  Performance will obviously
 * suffer, though.
 */
static void m3gDestroyLargeImage(Image *img)
{
    LargeImage *lrg = img->large;
    M3G_VALIDATE_MEMBLOCK(lrg);

    m3gDeleteGLTextures(M3G_INTERFACE(img),
                        lrg->tilesX * lrg->tilesY, lrg->tileNames);
    m3gFree(M3G_INTERFACE(img), img->large);
    
    img->large = NULL;
}

/*!
 * \internal
 * \brief Binds an image as an OpenGL texture object
 *
 * The image is bound to the active texture unit, which must be
 * selected outside of this function.
 */
static void m3gBindTextureObject(Image *img, M3Gbool mipmap)
{
    Interface *m3g;
    M3G_VALIDATE_OBJECT(img);
    m3g = M3G_INTERFACE(img);
    M3G_ASSERT(img->special == 0);
    M3G_ASSERT_NO_LOCK(m3g);
    M3G_ASSERT_GL;

    /* Bind the next available texture object; create a new one if it
     * doesn't exist yet. */
    {
        if (!img->texObject) {
            GLint err;
            glGenTextures(1, &img->texObject);
            err = glGetError();
            if (err == GL_OUT_OF_MEMORY) {
                m3gRaiseError(M3G_INTERFACE(img), M3G_OUT_OF_MEMORY);
                return;
            }
            M3G_ASSERT(err == GL_NO_ERROR);
            M3G_LOG1(M3G_LOG_OBJECTS, "New GL texture object 0x%08X\n",
                     (unsigned) img->texObject);
            img->dirty = M3G_TRUE;
        }
        glBindTexture(GL_TEXTURE_2D, img->texObject);
    }

    /* Upload the texture image to OpenGL if the one in the texture
     * object isn't up to date */

    if (img->dirty || (mipmap && img->mipmapsDirty)) {

        M3Gubyte *pixels = ((M3Gubyte *)m3gMapObject(m3g, img->data));

        /* Reload the level 0 image if dirty. Note that paletted
         * textures are loaded as compressed, and the mipmap dirty
         * flag is only raised for non-paletted textures. */

        if (img->dirty) {
            M3G_ASSERT_PTR(pixels);
            if (img->paletteBytes > 0) {
                M3G_ASSERT(img->glFormat == GL_PALETTE8_RGBA8_OES
                    || img->glFormat == GL_PALETTE8_RGB8_OES);
                M3G_ASSERT(mipmap == M3G_FALSE);
                glCompressedTexImage2D(GL_TEXTURE_2D,
                                       0,
                                       img->glFormat,
                                       img->width, img->height,
                                       0,
                                       img->width * img->height + img->paletteBytes,
                                       pixels);
            }
            else {
#               if defined(M3G_GL_ES_1_1)
                glTexParameteri(GL_TEXTURE_2D, GL_GENERATE_MIPMAP,
                                mipmap ? GL_TRUE : GL_FALSE);
#               endif
                glTexImage2D(GL_TEXTURE_2D,
                             0,
                             img->glFormat,
                             img->width, img->height,
                             0,
                             img->glFormat,
                             GL_UNSIGNED_BYTE,
                             pixels);
#               if defined(M3G_GL_ES_1_1)
                img->mipmapsDirty = M3G_FALSE;
#               else
                img->mipmapsDirty = M3G_TRUE;
#               endif
            }
            m3gUnmapObject(m3g, img->data);
            img->dirty = M3G_FALSE;
        }

        /* Regenerate mipmap levels if necessary; also regenerate if
         * the image will never change again, as this allows us to
         * free the user memory copy of the image and keep only the
         * mipmap pyramid in OpenGL memory, saving some in total */
#       if !defined(M3G_GL_ES_1_1)
        if (img->mipmapsDirty && (mipmap || (img->flags & M3G_DYNAMIC) == 0)) {
            int i, n;
            M3Gint w, h;
            const M3Gubyte *src;
            M3Gubyte *temp;

            M3G_ASSERT(!img->dirty);

            w = img->width;
            h = img->height;
            n = m3gGetNumMipmapLevels(w, h);

            temp = m3gAllocTemp(m3g,
                                w * h * m3gBytesPerPixel(img->internalFormat));
            if (!temp) {
                return; /* automatic out of memory */
            }
            src = ((M3Gubyte *)m3gMapObject(m3g, img->data));

            for (i = 1; i < n; ++i) {
                m3gDownsample(img->internalFormat,
                              src,
                              &w, &h,
                              temp);
                glTexImage2D(GL_TEXTURE_2D,
                             i,
                             img->glFormat,
                             w, h,
                             0,
                             img->glFormat,
                             GL_UNSIGNED_BYTE,
                             temp);
                src = temp;
            }

            m3gUnmapObject(m3g, img->data);
            m3gFreeTemp(m3g);
            img->mipmapsDirty = M3G_FALSE;
        }
#       endif /* !M3G_GL_ES_1_1 */

        /* Free the pixel data if we can; we've uploaded mipmap
         * levels, so OpenGL will keep them for us for the rest of the
         * lifetime of this object */

        if (!img->pinned && !img->mipmapsDirty) {
            m3gFreeImageData(img);
        }

        /* Raise out-of-memory if the OpenGL implementation ran out of
         * resources */
        {
            GLint err = glGetError();
            
            if (err == GL_OUT_OF_MEMORY) {
                m3gRaiseError(M3G_INTERFACE(img), M3G_OUT_OF_MEMORY);
            }
            else if (err != GL_NO_ERROR) {
                M3G_ASSERT(M3G_FALSE);
            }
        }
    }
}

/*!
 * \internal
 * \brief Releases one of the texture objects bound for this image
 *
 * This assumes that the texture unit the image was bound to is
 * current.
 */
static void m3gReleaseTextureImage(Image *img)
{
    M3G_VALIDATE_OBJECT(img);
    M3G_UNREF(img);
    
    glBindTexture(GL_TEXTURE_2D, 0);
    
    M3G_ASSERT_GL;
}

/*!
 * \internal
 * \brief Copies an image from the bottom left corner of the frame
 * buffer
 *
 */
static void m3gCopyFrameBufferImage(Image *img)
{
    Interface *m3g;
    M3Gubyte *pixels;
    M3G_VALIDATE_OBJECT(img);
    M3G_ASSERT_GL;

    m3g = M3G_INTERFACE(img);
    
    {
        int row;
        M3Gsizei stride = img->width * m3gBytesPerPixel(img->internalFormat);
        
        /* An RGBA image we can copy straight into the user memory buffer */

        if (img->internalFormat == M3G_RGBA8) {
            pixels = m3gMapObject(m3g, img->data);
            for (row = 0; row < img->height; ++row) {
                glReadPixels(0, img->height - row - 1,
                             img->width, 1,
                             GL_RGBA, GL_UNSIGNED_BYTE,
                             pixels + row * stride);
            }
            m3gUnmapObject(m3g, img->data);
        }
        else {
            
            /* For non-RGBA images, we must do a format conversion from
             * the RGBA returned by ReadPixels to the destination
             * format. We do this one scanline at a time to spare memory.
             */
            
            M3Gubyte *temp = m3gAllocTemp(m3g, img->width * 4);
            if (!temp) {
                return; /* out of memory */
            }
            pixels = m3gMapObject(m3g, img->data);
            
            for (row = 0; row < img->height; ++row) {
                glReadPixels(0, img->height - row - 1,
                             img->width, 1,
                             GL_RGBA, GL_UNSIGNED_BYTE,
                             temp);
                m3gConvertPixels(M3G_RGBA8, temp,
                                 img->internalFormat, pixels + row * stride,
                                 img->width);
            }
            m3gUnmapObject(m3g, img->data);
            m3gFreeTemp(m3g);
        }
    }
    M3G_ASSERT_GL;
    
    m3gInvalidateImage(img);
}

/*!
 * \internal
 * \brief Draws any RGB or RGBA image into the bottom left corner of
 * the frame buffer
 */
static void m3gDrawFrameBufferImage(RenderContext *ctx, const Image *img)
{
    M3G_VALIDATE_OBJECT(img);
    {
        const M3Gubyte *pixels = m3gMapObject(M3G_INTERFACE(img), img->data);
        m3gBlitFrameBufferPixels(ctx,
                                 0, 0,
                                 img->width, img->height,
                                 img->internalFormat,
                                 m3gGetImageStride(img),
                                 pixels);
        m3gUnmapObject(M3G_INTERFACE(img), img->data);
    }
}
