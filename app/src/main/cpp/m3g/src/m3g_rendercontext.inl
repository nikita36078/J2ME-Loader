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
* Description: EGL rendering context management functions
*
*/


/*!
 * \internal
 * \file
 * \brief EGL rendering context management functions
 */

#if defined(M3G_NGL_CONTEXT_API)
#   error This file is for the OES API only
#endif

#include <EGL/egl.h>
#include "m3g_image.h"

/*----------------------------------------------------------------------
 * Private functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Queries for an EGL configuration matching given M3G format
 * parameters
 */
static EGLConfig m3gQueryEGLConfig(M3Genum format,
                                   M3Gbitmask bufferBits,
                                   EGLint surfaceBits,
                                   M3GNativeBitmap bitmapHandle)
{
    struct { int attrib, value; } attribs[10];
    int samples;

    /* Determine color depth */
    
    attribs[0].attrib = EGL_RED_SIZE;
    attribs[1].attrib = EGL_GREEN_SIZE;
    attribs[2].attrib = EGL_BLUE_SIZE;
    attribs[3].attrib = EGL_ALPHA_SIZE;
        
    switch (format) {
    case M3G_RGB4:
        attribs[0].value = 4;
        attribs[1].value = 4;
        attribs[2].value = 4;
        attribs[3].value = 0;
        break;
    case M3G_RGB565:
        attribs[0].value = 5;
        attribs[1].value = 6;
        attribs[2].value = 5;
        attribs[3].value = 0;
        break;
    case M3G_RGB8:
    case M3G_BGR8_32:
        attribs[0].value = 8;
        attribs[1].value = 8;
        attribs[2].value = 8;
        attribs[3].value = 0;
        break;
    case M3G_RGBA8:
    case M3G_BGRA8:
        attribs[0].value = 8;
        attribs[1].value = 8;
        attribs[2].value = 8;
        attribs[3].value = 8;
        break;
    default:
        return NULL;
    }

    /* Set up the depth buffer */
    
    attribs[4].attrib = EGL_DEPTH_SIZE;
    attribs[4].value = (bufferBits & M3G_DEPTH_BUFFER_BIT) ? 8 : 0;
    
    /* Set target surface type mask */
    
    attribs[5].attrib = EGL_SURFACE_TYPE;
    attribs[5].value = surfaceBits;


    if (bitmapHandle) {
        /* This attribute is matched only for pixmap targets */
        attribs[6].attrib = EGL_MATCH_NATIVE_PIXMAP;
        attribs[6].value = bitmapHandle;

        /* Try to get multisampling if requested */

        attribs[7].attrib = EGL_SAMPLE_BUFFERS;
        attribs[8].attrib = EGL_SAMPLES;

        attribs[9].attrib = EGL_NONE;
    } else {
        /* Try to get multisampling if requested */

        attribs[6].attrib = EGL_SAMPLE_BUFFERS;
        attribs[7].attrib = EGL_SAMPLES;

        attribs[8].attrib = EGL_NONE;
    }
    
    
    /* Try 4 samples if multisampling enabled, then 2, then 1 */
    
    samples = (bufferBits & M3G_MULTISAMPLE_BUFFER_BIT) ? 4 : 1;
    for ( ; samples > 0; samples >>= 1) {
        
        if (bitmapHandle) {
            if (samples > 1) {
                attribs[7].value = 1;
                attribs[8].value = samples;
            }
            else {
                attribs[7].value = EGL_FALSE;
                attribs[8].value = 0;
            }
        } else {
            if (samples > 1) {
                attribs[6].value = 1;
                attribs[7].value = samples;
            }
            else {
                attribs[6].value = EGL_FALSE;
                attribs[7].value = 0;
            }
        }

        /* Get the first matching config; according to EGL sorting
         * rules, this should be an accelerated one if possible */
        {
            EGLConfig config;
            int numConfigs;  
            EGLint error;            
        
            eglChooseConfig(eglGetDisplay(EGL_DEFAULT_DISPLAY),
                            (const int *) attribs,
                            &config, 1,
                            &numConfigs);
            
            error = eglGetError();
            if (error != EGL_SUCCESS) {
                M3G_LOG1(M3G_LOG_FATAL_ERRORS, "eglChooseConfig  failed: %d\n", error);
            }
            
            
            M3G_ASSERT(error == EGL_SUCCESS);

            /* If we got a config, return that; otherwise, drop the
             * number of multisampling samples and try again, or
             * return NULL for no config if we already have zero
             * samples */
            
            if (numConfigs > 0) {
                M3G_LOG1(M3G_LOG_RENDERING, "Selected EGL config #%d\n", config);
                return config;
            }

            if (samples == 2) {
                M3G_LOG(M3G_LOG_WARNINGS, "Warning: multisampling not available\n");
            }
        }
    }

    /* No matching configuration found */
    
    return NULL;
}

/*!
 * \internal
 * \brief Initializes EGL
 */
static void m3gInitializeEGL(void)
{
    M3G_LOG(M3G_LOG_INTERFACE, "Initializing EGL\n");
    eglInitialize(eglGetDisplay(EGL_DEFAULT_DISPLAY), NULL, NULL);
}

/*!
 * \internal
 * \brief Terminates EGL
 */
static void m3gTerminateEGL(void)
{
    M3G_LOG(M3G_LOG_INTERFACE, "Shutting down EGL\n");
    eglTerminate(eglGetDisplay(EGL_DEFAULT_DISPLAY));
}

/*!
 * \internal
 * \brief Creates a new EGL context
 */
/*static EGLContext m3gCreateGLContext(M3Genum format,
                                     M3Gbitmask bufferBits,
                                     M3Gbitmask reqSurfaceBits,
                                     EGLContext share,
                                     M3Gbitmask *outSurfaceBits)
{
    EGLContext ctx;
    EGLConfig config;

    M3G_ASSERT((reqSurfaceBits & ~(EGL_PIXMAP_BIT|EGL_PBUFFER_BIT|EGL_WINDOW_BIT)) == 0);
    
    config = m3gQueryEGLConfig(format, bufferBits, reqSurfaceBits, NULL);
    
    if (!config || !eglGetConfigAttrib(eglGetDisplay(EGL_DEFAULT_DISPLAY),
                                       config,
                                       EGL_SURFACE_TYPE,
                                       (EGLint *) outSurfaceBits)) {
        return NULL;
    }
    
    ctx = eglCreateContext(eglGetDisplay(EGL_DEFAULT_DISPLAY),
                           config,
                           share,
                           NULL);
    
#   if defined(M3G_DEBUG)
    {
        EGLint err = eglGetError();
        M3G_ASSERT(err == EGL_SUCCESS || err == EGL_BAD_ALLOC);
    }
#   endif

    M3G_LOG1(M3G_LOG_OBJECTS, "New GL context 0x%08X\n", (unsigned) ctx);
    return ctx;
}
*/
/*!
 * \internal
 * \brief Deletes an EGL context
 */
static void m3gDeleteGLContext(EGLContext ctx)
{
    eglDestroyContext(eglGetDisplay(EGL_DEFAULT_DISPLAY), ctx);
#   if defined(M3G_DEBUG)
    {
        EGLint err = eglGetError();
        if (err != EGL_SUCCESS) {
            M3G_LOG1(M3G_LOG_FATAL_ERRORS, "EGL error 0x%08X\n", (unsigned) err);
        }
        M3G_ASSERT(err == EGL_SUCCESS);
    }
#   endif
    M3G_LOG1(M3G_LOG_OBJECTS, "Destroyed GL context 0x%08X\n",
             (unsigned) ctx);
}

    
/*!
 * \internal
 * \brief Creates a new EGL window surface
 */
static EGLSurface m3gCreateWindowSurface(M3Genum format,
                                         M3Gbitmask bufferBits,
                                         M3GNativeWindow wnd)
{
    EGLSurface surf;
    EGLConfig config = m3gQueryEGLConfig(format, bufferBits, EGL_WINDOW_BIT, NULL);
    
    if (!config) {
        return NULL;
    }

    surf = eglCreateWindowSurface(eglGetDisplay(EGL_DEFAULT_DISPLAY),
                                  config,
                                  (NativeWindowType) wnd,
                                  NULL);

#   if defined(M3G_DEBUG)
    {
        EGLint err = eglGetError();
        M3G_ASSERT(err == EGL_SUCCESS || err == EGL_BAD_ALLOC);
    }
#   endif

    if (surf != EGL_NO_SURFACE) {
        M3G_LOG1(M3G_LOG_OBJECTS, "New GL window surface 0x%08X\n",
                 (unsigned) surf);
        return surf;
    }
    return NULL;
}


/*!
 * \internal
 * \brief Creates a new EGL pixmap surface
 */
static EGLSurface m3gCreateBitmapSurface(M3Genum format,
                                         M3Gbitmask bufferBits,
                                         M3GNativeBitmap bmp)
{
    EGLSurface surf;
    EGLConfig config = m3gQueryEGLConfig(format, bufferBits, EGL_PIXMAP_BIT, bmp);
    
    if (!config) {
        return NULL;
    }
    
    surf = eglCreatePixmapSurface(eglGetDisplay(EGL_DEFAULT_DISPLAY),
                                  config,
                                  (NativePixmapType) bmp,
                                  NULL);

#   if defined(M3G_DEBUG)
    {
        EGLint err = eglGetError();
        M3G_ASSERT(err == EGL_SUCCESS || err == EGL_BAD_ALLOC);
    }
#   endif
    
    if (surf != EGL_NO_SURFACE) {
        M3G_LOG1(M3G_LOG_OBJECTS, "New GL pixmap surface 0x%08X\n",
                 (unsigned) surf);
        return surf;
    }
    return NULL;
}


/*!
 * \internal
 * \brief Creates a new PBuffer
 */
static EGLSurface m3gCreatePBufferSurface(M3Genum format,
                                          M3Gbitmask bufferBits,
                                          M3Gint width, M3Gint height)
{
    EGLSurface surf;
    EGLConfig config;
    EGLint attrib[5];
    
    attrib[0] = EGL_WIDTH;
    attrib[1] = width;
    attrib[2] = EGL_HEIGHT;
    attrib[3] = height;
    attrib[4] = EGL_NONE;
    
    config = m3gQueryEGLConfig(format, bufferBits, EGL_PBUFFER_BIT, NULL);
    if (!config) {
        return NULL;
    }

    surf = eglCreatePbufferSurface(eglGetDisplay(EGL_DEFAULT_DISPLAY),
                                   config,
                                   attrib);
#   if defined(M3G_DEBUG)
    {
        EGLint err = eglGetError();
        M3G_ASSERT(err == EGL_SUCCESS || err == EGL_BAD_ALLOC);
    }
#   endif
                                              
    if (surf != EGL_NO_SURFACE) {
        M3G_LOG1(M3G_LOG_OBJECTS, "New GL pbuffer surface 0x%08X\n",
                 (unsigned) surf);
        return surf;
    }
    return NULL;
}


/*!
 * \internal
 * \brief Deletes an EGL surface
 */
static void m3gDeleteGLSurface(EGLSurface surface)
{
    eglDestroySurface(eglGetDisplay(EGL_DEFAULT_DISPLAY), surface);

#   if defined(M3G_DEBUG)
    {
        EGLint err = eglGetError();
        M3G_ASSERT(err == EGL_SUCCESS);
    }
#   endif

    M3G_LOG1(M3G_LOG_OBJECTS, "Destroyed GL surface 0x%08X\n",
             (unsigned) surface);
}

/*!
 * \brief Swap buffers on a rendering surface
 */
static M3Gbool m3gSwapBuffers(EGLSurface surface)
{
    EGLBoolean success = eglSwapBuffers(eglGetDisplay(EGL_DEFAULT_DISPLAY),
                                        surface);
    
#   if defined(M3G_DEBUG)
    EGLint err = eglGetError();
    M3G_ASSERT(err == EGL_SUCCESS);
#   endif

    return (M3Gbool) success;
}

/*!
 * \brief Does a sub-blit of a frame buffer blit operation
 */
static void m3gBlitFrameBufferPixels2(RenderContext *ctx,
                                      M3Gint xOffset, M3Gint yOffset,
                                      M3Gint width, M3Gint height,
                                      M3GPixelFormat internalFormat,
                                      M3Gsizei stride,
                                      const M3Gubyte *pixels)
{
#   define MAX_TEMP_TEXTURES    8
    GLuint glFormat;
    static const int MAX_TILE_SIZE = 256; /* -> 256 KB temp buffer(s) */
    static const M3Gbyte tc[8] = { 0, 0, 0, 1, 1, 0, 1, 1 };
    GLshort pos[8];
    int tileWidth = MAX_TILE_SIZE, tileHeight = MAX_TILE_SIZE;
    M3Gbool mustConvert = M3G_FALSE;
    M3Gubyte *tempPixels = 0; /* initialize to avoid compiler warnings */
    GLuint tempTexObj[MAX_TEMP_TEXTURES];
    GLint tempTexCount;
        
    M3G_VALIDATE_OBJECT(ctx);
    M3G_ASSERT_GL;

    /* Analyze source and destination formats for possible conversion */
    
    glFormat = m3gGetGLFormat(internalFormat);
    if (!glFormat) {
        M3G_ASSERT(M3G_FALSE);  /* internal format not supported in GL */
        return;
    }
    if (internalFormat == M3G_RGB8_32) {
        glFormat = GL_RGBA;
    }
    if (internalFormat == M3G_BGR8_32 || internalFormat == M3G_ARGB8) {
        glFormat = GL_RGBA;
        mustConvert = M3G_TRUE;
    }

    /* Tweak tile size to avoid using excessive amounts of memory for
     * portions outside the blit area */

    M3G_ASSERT((width > 0) && (height > 0));
    
    while (tileWidth >= width * 2) {
        tileWidth >>= 1;
        tileHeight <<= 1;
    }
    while (tileHeight >= height * 2) {
        tileHeight >>= 1;
    }
        
    /* Allocate temp memory for conversion or adjust tile size for
     * optimal direct download to GL */
    
    if (mustConvert) {
        tempPixels = m3gAllocTemp(M3G_INTERFACE(ctx),
                                  tileWidth * tileHeight * 4);
        if (!tempPixels) {
            return; /* out of memory */
        }
    }
    else {

        /* Attempt to adjust the tile size so that we can copy
         * complete scanlines at a time -- this is because OpenGL ES
         * is missing PixelStore settings that could be used for
         * stride control during image uploading */

        M3Gint maxWidth;
        glGetIntegerv(GL_MAX_TEXTURE_SIZE, &maxWidth);
        
        while (tileWidth < width &&
               tileWidth < maxWidth &&
               tileHeight > 1) {
            tileWidth <<= 1;
            tileHeight >>= 1;
        }
    }
    
    /* Load default images into the temp texture objects */
    
    glActiveTexture(GL_TEXTURE0);
    glEnable(GL_TEXTURE_2D);
    {
        int ti;
        tempTexCount = ((width + tileWidth - 1) / tileWidth)
            * ((height + tileHeight - 1) / tileHeight);
        tempTexCount = m3gMinInt(tempTexCount, MAX_TEMP_TEXTURES);
        
        glGenTextures(tempTexCount, tempTexObj);
        
        for (ti = 0; ti < tempTexCount; ++ti) {
            glBindTexture(GL_TEXTURE_2D, tempTexObj[ti]);
            glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
            glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            M3G_ASSERT_GL;
            
            glTexImage2D(GL_TEXTURE_2D, 0,
                         glFormat,
                         tileWidth, tileHeight,
                         0,
                         glFormat,
                         GL_UNSIGNED_BYTE, NULL);

            /* Raise out-of-memory if OpenGL ran out of resources */
            {
                GLint err = glGetError();

                if (err == GL_OUT_OF_MEMORY) {
                    m3gRaiseError(M3G_INTERFACE(ctx), M3G_OUT_OF_MEMORY);
                    goto CleanUpAndExit;
                }
                else if (err != GL_NO_ERROR) {
                    M3G_ASSERT(M3G_FALSE);
                }
            }
        }
    }

    /* Set up texture and vertex coordinate arrays for the image tiles */

    glClientActiveTexture(GL_TEXTURE0);
    glTexCoordPointer(2, GL_BYTE, 0, tc);
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    glVertexPointer(2, GL_SHORT, 0, pos);
    glEnableClientState(GL_VERTEX_ARRAY);
    glMatrixMode(GL_TEXTURE);
    glLoadIdentity();
    glMatrixMode(GL_MODELVIEW);
    M3G_ASSERT_GL;

    /* Load each image tile into a texture and draw */

    {
        M3Gint nextTexTile = 0;
        M3Gint x, y, bpp;
        bpp = m3gBytesPerPixel(internalFormat);
        if (stride == 0) {
            stride = bpp * width;
        }

        for (y = 0; y < height; y += tileHeight) {
            for (x = 0; x < width; x += tileWidth) {
                M3Gint w, h;

                w = M3G_MIN(tileWidth, width - x);
                h = M3G_MIN(tileHeight, height - y);

                glBindTexture(GL_TEXTURE_2D, tempTexObj[nextTexTile]);
                nextTexTile = (nextTexTile + 1) % MAX_TEMP_TEXTURES;
                              
                if (mustConvert) {
                    m3gConvertPixelRect(internalFormat,
                                        pixels + y * stride + x * bpp,
                                        stride,
                                        w, h,
                                        M3G_RGBA8, tempPixels, w * 4);
                    glTexSubImage2D(GL_TEXTURE_2D, 0,
                                    0, 0,
                                    w, h,
                                    GL_RGBA, GL_UNSIGNED_BYTE, tempPixels);
                }
                else {
                    if (w*bpp == stride) {
                        glTexSubImage2D(GL_TEXTURE_2D, 0,
                                        0, 0,
                                        w, h,
                                        glFormat,
                                        GL_UNSIGNED_BYTE,
                                        pixels + y * stride + x * bpp);
                    }
                    else {
                        int k;
                        for (k = 0; k < h; ++k) {
                            glTexSubImage2D(GL_TEXTURE_2D, 0,
                                            0, k,
                                            w, 1,
                                            glFormat,
                                            GL_UNSIGNED_BYTE,
                                            pixels + (y+k) * stride + x * bpp);
                        }
                    }
                }
                
                pos[0] = (GLshort)(x + xOffset);
                pos[1] = (GLshort)((height - y) + yOffset);
                pos[2] = pos[0];
                pos[3] = (GLshort)((height - (y + tileHeight)) + yOffset);
                pos[4] = (GLshort)((x + tileWidth) + xOffset);
                pos[5] = pos[1];
                pos[6] = pos[4];
                pos[7] = pos[3];
                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
            }
        }
        M3G_ASSERT_GL;
    }

    /* Restore required OpenGL state and release resources */

CleanUpAndExit:
    if (mustConvert) {
        m3gFreeTemp(M3G_INTERFACE(ctx));
    }

    glDeleteTextures(tempTexCount, tempTexObj);
        
    M3G_ASSERT_GL;
    
#   undef MAX_TEMP_TEXTURES
}

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/* The frame buffer should be the first thing locked and the last one
 * released, so let's mandate that even though it has no real effect
 * with EGL */
#define m3gLockFrameBuffer(ctx)    M3G_ASSERT_NO_LOCK(M3G_INTERFACE(ctx))
#define m3gReleaseFrameBuffer(ctx) M3G_ASSERT_NO_LOCK(M3G_INTERFACE(ctx))

/*!
 * \internal
 * \brief Alternate rendering function for two-sided lighting on buggy
 * hardware
 */
static M3Gbool m3gSplitDrawMesh(RenderContext *ctx,
                                const VertexBuffer *vb,
                                const IndexBuffer *ib,
                                const Appearance *app,
                                const M3GMatrix *modelTransform,
                                M3Gint alphaFactor,
                                M3Gint scope)
{
    if (!ctx->inSplitDraw && m3gGetMaterial((M3GAppearance) app) && vb->normals) {
        PolygonMode *pm = m3gGetPolygonMode((M3GAppearance) app);
        if (pm && pm->enableTwoSidedLighting) {
            M3Gint originalCulling = m3gGetCulling(pm);
            if (originalCulling != M3G_CULL_BACK) {
                
                /* OK, we must render the back sides separately with
                 * flipped normals */
                    
                Interface *m3g = M3G_INTERFACE(ctx);
                VertexArray *tempNormals;
                
                M3Gint normalCount = vb->vertexCount;
                M3Gint normalStride = vb->normals->stride;

                /* Duplicate the normal array */
                
                m3gReleaseFrameBuffer(ctx);
                tempNormals = m3gCloneVertexArray(vb->normals);
                if (!tempNormals) {
                    m3gLockFrameBuffer(ctx);
                    return M3G_TRUE; /* automatic out-of-memory */
                }

                /* Flip the signs of the temp normals */

                if (tempNormals->elementType == GL_BYTE) {
                    M3Gbyte *p = (M3Gbyte*) m3gMapObject(m3g, tempNormals->data);
                    int i;
                    for (i = 0; i < normalCount; ++i) {
                        p[0] = (M3Gbyte) -m3gClampInt(p[0], -127, 127);
                        p[1] = (M3Gbyte) -m3gClampInt(p[1], -127, 127);
                        p[2] = (M3Gbyte) -m3gClampInt(p[2], -127, 127);
                        p += normalStride;
                    }
                }
                else {
                    M3Gshort *p = (M3Gshort*) m3gMapObject(m3g, tempNormals->data);
                    int i;
                    for (i = 0; i < normalCount; ++i) {
                        p[0] = (M3Gshort) -m3gClampInt(p[0], -32767, 32767);
                        p[1] = (M3Gshort) -m3gClampInt(p[1], -32767, 32767);
                        p[2] = (M3Gshort) -m3gClampInt(p[2], -32767, 32767);
                        p += normalStride / 2;
                    }
                }
                m3gUnmapObject(m3g, tempNormals->data);
                m3gLockFrameBuffer(ctx);
                
                ctx->inSplitDraw = M3G_TRUE;

                /* Set culling to front faces only and render with the
                 * flipped normals */
                {
                    VertexArray *orgNormals = vb->normals;
                    ((VertexBuffer*)vb)->normals = tempNormals;
                    m3gSetCulling(pm, M3G_CULL_FRONT);
                    m3gDrawMesh(ctx,
                                vb, ib, app,
                                modelTransform,
                                alphaFactor, scope);
                    ((VertexBuffer*)vb)->normals = orgNormals;
                }

                /* If no culling was enabled, render the front faces
                 * with the original normals */

                if (originalCulling == M3G_CULL_NONE) {
                    m3gSetCulling(pm, M3G_CULL_BACK);
                    m3gDrawMesh(ctx,
                                vb, ib, app,
                                modelTransform,
                                alphaFactor, scope);
                }

                /* Restore original culling and free the temp normals */
                    
                m3gSetCulling(pm, originalCulling);
                
                m3gReleaseFrameBuffer(ctx);
                m3gDeleteObject((M3GObject) tempNormals);
                m3gLockFrameBuffer(ctx);

                ctx->inSplitDraw = M3G_FALSE;
                return M3G_TRUE;
            }
        }
    }
    return M3G_FALSE;
}

/*!
 * \internal
 * \brief Determines whether a format/mode combination can be directly
 * rendered
 */
static M3Gbool m3gCanDirectRender(const RenderContext *ctx)
{
    M3GPixelFormat format = ctx->target.format;
    M3Gbitmask bufferBits = ctx->bufferBits;
    M3Gbitmask surfaceType = ctx->target.type;
    M3GNativeBitmap bitmapHandle = ctx->target.handle;
    int i;

    /* Images always go via pbuffers; EGL surfaces can always be
     * rendered to */
    
    if (surfaceType == SURFACE_IMAGE) {
        return M3G_FALSE;
    }
    if (surfaceType == SURFACE_EGL || surfaceType == SURFACE_WINDOW) {
        return M3G_TRUE;
    }
    
    /* First scan the context cache for a matching previously used
     * context; this should be faster than querying EGL */
    
    for (i = 0; i < M3G_MAX_GL_CONTEXTS; ++i) {
        const GLContextRecord *rc = &ctx->glContext[i];
        
        if ((rc->surfaceTypeBits & surfaceType) == surfaceType
            && rc->format == format
            && (rc->bufferBits & bufferBits) == bufferBits) {
            
            return M3G_TRUE;
        }
    }

    /* No dice; must resort to querying from EGL */

    return (m3gQueryEGLConfig(format, bufferBits, (EGLint) surfaceType, bitmapHandle) != NULL);
}

/*!
 * \internal
 * \brief Ensures that a sufficient back buffer exists
 *
 * Creates a new PBuffer for the back buffer if required.
 */
static M3Gbool m3gValidateBackBuffer(RenderContext *ctx)
{
    BackBuffer *bbuf = &ctx->backBuffer;
    int w = ctx->target.width;
    int h = ctx->target.height;

    /* NOTE the EGL specification is fuzzy on eglCopyBuffers when the
     * pbuffer is larger than the target, so we require an exact match
     * (can be relaxed by #undefining the flag, see m3g_defs.h) */

#   if defined(M3G_GL_FORCE_PBUFFER_SIZE)
    if (bbuf->width != w || bbuf->height != h) {
#   else
    if (bbuf->width < w || bbuf->height < h) {
#   endif
        
        M3G_LOG(M3G_LOG_WARNINGS,
                "Warning (performance): Buffered rendering.\n");
        
        if (bbuf->glSurface != NULL) {
            m3gDeleteGLSurface(bbuf->glSurface);
        }
        
        bbuf->glSurface = m3gCreatePBufferSurface(
            M3G_RGBA8,
            (M3Gbitmask)(M3G_COLOR_BUFFER_BIT|M3G_DEPTH_BUFFER_BIT),
            w, h);
        
        bbuf->width = w;
        bbuf->height = h;
        
        if (!bbuf->glSurface) {
            if (eglGetError() == EGL_BAD_ALLOC) {
                return M3G_FALSE; /* ouf of memory */
            }
            else {
                M3G_ASSERT(M3G_FALSE);
            }
        }
    }
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Increment the rendering time stamp
 *
 * The time stamp is used to manage the various caches for the
 * context, so it needs to be updated often enough for the caches to
 * function optimally.
 * 
 * In the rare case that the time stamp should wrap around(!), we
 * reset the time stamps dependent on it to avoid sub-optimal cache
 * performance.
 */
static void m3gIncrementRenderTimeStamp(RenderContext *ctx)
{
    if (++ctx->cacheTimeStamp == 0) {
        int i;
        for (i = 0; i < M3G_MAX_GL_CONTEXTS; ++i) {
            ctx->glContext[i].lastUseTime = 0;
        }
        for (i = 0; i < M3G_MAX_GL_SURFACES; ++i) {
            ctx->glSurface[i].lastUseTime = 0;
        }
    }
}

/*!
 * \internal
 * \brief Draws a raw RGB or RGBA pixel rectangle of arbitrary size
 * into the frame buffer
 *
 * The offset only affects the position of the blitted rectangle in
 * the frame buffer. The source data is read starting at the given
 * pointer.
 * 
 * \param ctx            the rendering context
 * \param xOffset        offset from the left edge of the frame buffer
 * \param yOffset        offset from the bottom of the frame buffer
 * \param width          width of the rectangle
 * \param height         height of the rectangle
 * \param internalFormat format of the source pixels
 * \param stride         stride of the source data
 * \param pixels         pointer to the source pixels in top-to-bottom order
 */
static void m3gBlitFrameBufferPixels(RenderContext *ctx,
                                     M3Gint xOffset, M3Gint yOffset,
                                     M3Gint width, M3Gint height,
                                     M3GPixelFormat internalFormat,
                                     M3Gsizei stride,
                                     const M3Gubyte *pixels)
{
    /* Skip this if nothing to copy */

    if (width <= 0 || height <= 0) {
        return;
    }
    
    /* Set viewport, projection and modelview to map coordinates to
     * pixel boundaries */

    glScissor(xOffset, yOffset, width, height);
    glViewport(0, 0, ctx->target.width, ctx->target.height);
    glMatrixMode(GL_PROJECTION);
    glLoadIdentity();
    glOrthox(0, ctx->target.width << 16,
             0, ctx->target.height << 16,
             -1 << 16, 1 << 16);
    glMatrixMode(GL_MODELVIEW);
    glLoadIdentity();
    
    /* Disable any stray state we don't want */

    glDisable(GL_CULL_FACE);
    glDisable(GL_BLEND);
    glDisable(GL_ALPHA_TEST);
    glDisableClientState(GL_NORMAL_ARRAY);
    glDisableClientState(GL_COLOR_ARRAY);
    glDisable(GL_LIGHTING);
    glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
    glDepthMask(GL_FALSE);
    glDepthFunc(GL_ALWAYS);
    m3gDisableTextures();
    M3G_ASSERT_GL;
    
    /* Split the large blit operation into smaller chunks that are
     * efficiently taken care of using power-of-two textures */
    {
        const int MAX_BLIT_SIZE = 4096; /* should be power of two */
    
        int xBlits = (width / MAX_BLIT_SIZE) + 1;
        int yBlits = (height / MAX_BLIT_SIZE) + 1;

        int xBlit, yBlit;
    
        for (yBlit = yBlits-1; yBlit >= 0; yBlit--) {
            for (xBlit = 0; xBlit < xBlits; ++xBlit) {
            
                M3Gint xStart = xOffset + xBlit * MAX_BLIT_SIZE;
                M3Gint yStart = yOffset + yBlit * MAX_BLIT_SIZE;
                M3Gint xSize = m3gMinInt(MAX_BLIT_SIZE, width - (xStart - xOffset));
                M3Gint ySize = m3gMinInt(MAX_BLIT_SIZE, height - (yStart - yOffset));

                M3Gint srcOffset = (height - (yStart - yOffset + ySize)) * stride + (xStart - xOffset) * m3gBytesPerPixel(ctx->target.format);
                
                m3gBlitFrameBufferPixels2(ctx,
                                          xStart, yStart,
                                          xSize, ySize,
                                          internalFormat,
                                          stride,
                                          pixels + srcOffset);
            }
        }
    }
}

/*!
 * \internal
 * \brief Synchronizes the contents of the back buffer with the target
 * buffer
 */
static void m3gUpdateBackBuffer(RenderContext *ctx)
{
    if (ctx->target.type == SURFACE_IMAGE) {
        m3gDrawFrameBufferImage(ctx, (Image *) ctx->target.handle);
    }
    else if (ctx->target.type == SURFACE_BITMAP || ctx->target.type == SURFACE_MEMORY) {

        M3Gubyte *src;
        M3Gsizei stride;

        M3Gint clipWidth = ctx->clip.x1 - ctx->clip.x0;
        M3Gint clipHeight = ctx->clip.y1 - ctx->clip.y0;
        M3Gint srcOffset;

        if (ctx->target.type == SURFACE_BITMAP) {
            /* Obtain a pointer to the native bitmap and copy the data to
             * the backbuffer from there */
            if (!m3gglLockNativeBitmap((M3GNativeBitmap) ctx->target.handle,
                                      &src, &stride)) {
                /* No dice! There's no way that we know of to copy the
                 * data between the buffers */
                M3G_ASSERT(M3G_FALSE);
            }
        } else {
            /* Memory target */
            src = ctx->target.pixels;
            stride = ctx->target.stride;
        }

        srcOffset =
            (ctx->target.height - ctx->clip.y1) * stride
            + ctx->clip.x0 * m3gBytesPerPixel(ctx->target.format);
        
        m3gBlitFrameBufferPixels(
            ctx,
            ctx->clip.x0, ctx->clip.y0,
            clipWidth, clipHeight,
            ctx->target.format,
            stride,
            src + srcOffset);

        if (ctx->target.type == SURFACE_BITMAP) {
            m3gglReleaseNativeBitmap((M3GNativeBitmap) ctx->target.handle);
        }
    }
    else {
        /* Buffered rendering is not supported for window and pbuffer
         * targets */
        M3G_ASSERT(M3G_FALSE);
    }
    ctx->backBuffer.contentsValid = M3G_TRUE;
}

/*!
 * \internal
 * \brief Synchronizes the contents of the target buffer with the back
 * buffer
 */
static void m3gUpdateTargetBuffer(RenderContext *ctx)
{
    if (ctx->target.type == SURFACE_IMAGE) {
        m3gCopyFrameBufferImage((Image *) ctx->target.handle);
    }
    else if (ctx->target.type == SURFACE_BITMAP || ctx->target.type == SURFACE_MEMORY) {
        
        M3GPixelFormat format = ctx->target.format;
        M3Gint width = ctx->clip.x1 - ctx->clip.x0;
        M3Gint height = ctx->clip.y1 - ctx->clip.y0;
        M3Gint xOffset = ctx->clip.x0;
        M3Gint yOffset = ctx->clip.y0;
        M3Gint row;

        M3Gubyte *dst;
        M3Gsizei stride;
        M3Gubyte *temp;

        if (ctx->target.type == SURFACE_BITMAP) {
            /* We must copy the back buffer to a native bitmap: first
             * attempt a fast buffer-to-buffer copy using EGL, but if that
             * fails, obtain a pointer and do the copy ourselves */

            /* We can only do the fast copy for the full buffer */

            M3Gbool fullClip = (ctx->clip.x0 == 0)
                && (ctx->clip.y0 <= ctx->target.height - ctx->display.height)
                && (ctx->clip.x1 >= ctx->display.width)
                && (ctx->clip.y1 >= ctx->clip.y0 + ctx->display.height);
            
            if (fullClip && eglCopyBuffers(eglGetDisplay(EGL_DEFAULT_DISPLAY),
                                             ctx->backBuffer.glSurface,
                                             (NativePixmapType) ctx->target.handle)) 
            {
                return;
            }
            
            /* Fast copy failed, try the generic approach */
            if (!m3gglLockNativeBitmap((M3GNativeBitmap) ctx->target.handle,
                                      &dst, &stride)) {
                /* No dice! There's no way that we know of to copy the
                 * data between the buffers */
                M3G_ASSERT(M3G_FALSE);
            }
        } else {
            /* Memory target */
            dst = ctx->target.pixels;
            stride = ctx->target.stride;
        }
                
        /* OK, got the pointer; now, copy a few scanlines at a
         * time, and we can pretty much assume conversion since 
         * the fast method didn't work */

#define READPIXELS_BUFFER_SIZE      16384

        if (width > 0 && height > 0) {

            M3Gint bufSize = (width * 4 > READPIXELS_BUFFER_SIZE ? width * 4 : READPIXELS_BUFFER_SIZE);
            M3Gint numLinesInBuffer = bufSize/(width * 4);
            M3Gint numLinesRead, line;
            temp = m3gAllocTemp(M3G_INTERFACE(ctx), bufSize);

            if (!temp) {
                return; /* out of memory */
            }

            dst += (ctx->target.height - (yOffset + height)) * stride
                + xOffset * m3gBytesPerPixel(format);

            for (row = 0; row < height; row += numLinesRead) {
                line = numLinesRead = (row + numLinesInBuffer > height) ? (height - row) : numLinesInBuffer;

                glReadPixels(xOffset, 
                    yOffset + height - row - numLinesRead,
                    width, numLinesRead, 
                    GL_RGBA, GL_UNSIGNED_BYTE, 
                    temp);

                while (line-- > 0) {
                    m3gConvertPixels(M3G_RGBA8, &temp[4*line*width], format, dst, width);
                    dst += stride;
                }
            }
            m3gFreeTemp(M3G_INTERFACE(ctx));
        }

        if (ctx->target.type == SURFACE_BITMAP) {
            m3gglReleaseNativeBitmap((M3GNativeBitmap) ctx->target.handle);
        }

#undef READPIXELS_BUFFER_SIZE

    } 
    else {
        /* Buffered rendering is not supported for window and pbuffer
         * targets */
        M3G_ASSERT(M3G_FALSE);
    }
}

static EGLConfig m3gEGLConfigForConfigID(EGLDisplay display, GLint configID)
{
	EGLConfig configs[1024];
	EGLint numConfigs = 0;

	if (EGL_FALSE == eglGetConfigs(display, configs, 1024, &numConfigs) )
	{
		return NULL;
	}

	for (int i = 0; i < numConfigs; i++)
	{
		EGLint value = 0;

		eglGetConfigAttrib(display, configs[i], EGL_CONFIG_ID, &value);
		if (value == configID)
		{
			return configs[i];
		}
	}

	return NULL;
}

/*!
 * \internal
 * \brief Selects a GL context matching a given GL surface and a set
 * of rendering parameters
 *
 * If no existing context matches, a new one is created. Contexts are
 * stored in a fixed-size cache and managed using a LRU policy.
 */
static EGLContext m3gSelectGLContext(RenderContext *ctx,
                                     M3GPixelFormat format,
                                     M3Gbitmask bufferBits,
                                     M3Gbitmask surfaceTypeBits,
                                     EGLSurface surface)
{
    int i;
    
    /* Look for a matching cached context and attempt to make it
     * current; on success, update the time in the context record and
     * return the GL context handle */

    for (i = 0; i < M3G_MAX_GL_CONTEXTS; ++i) {
        GLContextRecord *rc = &ctx->glContext[i];

        if ((rc->surfaceTypeBits & surfaceTypeBits) == surfaceTypeBits
            && rc->format == format
            && (rc->bufferBits & bufferBits) == bufferBits) {

            if (eglMakeCurrent(eglGetDisplay(EGL_DEFAULT_DISPLAY), 
                               surface, surface, rc->handle)) {
                rc->lastUseTime = ctx->cacheTimeStamp;
                return rc->handle;
            }
            else {
                /* NOTE we intentionally clear the error flag, since
                 * the MakeCurrent call above can fail in case of a
                 * context mismatch */
                eglGetError();
            }
        }
    }

    /* No match found, we must create a new context */
    {
        GLContextRecord *lru = &ctx->glContext[0];
        EGLContext shareRc = lru->handle;
        EGLContext glrc;

        /* Find the least recently used context entry */
 
        for (i = 1; i < M3G_MAX_GL_CONTEXTS; ++i) {
            GLContextRecord *rc = &ctx->glContext[i];
            if (rc->handle) {
                shareRc = rc->handle; /* keep this for sharing */
            }
            if (!rc->handle || rc->lastUseTime < lru->lastUseTime) {
                lru = rc;
            }
        }

        /* Create a new GL context, then delete the LRU one. This is
         * done in this order so that we don't lose any shared texture
         * objects when deleting a context. */

        //if (surfaceTypeBits == SURFACE_EGL) 
        {
            EGLDisplay dpy = eglGetDisplay(EGL_DEFAULT_DISPLAY);
            EGLint configID;
            eglQuerySurface(dpy,
                            surface,//(EGLSurface) ctx->target.handle,
                            EGL_CONFIG_ID,
                            &configID);
            glrc = eglCreateContext(dpy, m3gEGLConfigForConfigID(dpy, configID), shareRc, NULL);
            //M3G_ASSERT(glrc);
        }
        /*else {
            glrc = m3gCreateGLContext(format,
                                      bufferBits,
                                      surfaceTypeBits,
                                      shareRc,
                                      &lru->surfaceTypeBits);
        }
        */
        if (!glrc) {
            m3gRaiseError(M3G_INTERFACE(ctx), M3G_OUT_OF_MEMORY);
            return NULL;
        }
        if (lru->handle) {
            m3gDeleteGLContext(lru->handle);
        }
        
        /* Store the parameters for the new context and make it
         * current */
        
        lru->handle = glrc;
		lru->surfaceTypeBits = surfaceTypeBits;
        lru->format = format;
        lru->bufferBits = bufferBits;
        lru->modeBits = ctx->modeBits;
        {
            M3Gbool ok = eglMakeCurrent(eglGetDisplay(EGL_DEFAULT_DISPLAY),
                                        surface, surface, glrc);
            M3G_ASSERT(ok);
            if (!ok) {
                return NULL;
            }
        }
        lru->lastUseTime = ctx->cacheTimeStamp;
        m3gSetGLDefaults();
        return glrc;
    }
}

/*!
 * \internal
 * \brief Selects a GL surface suitable for rendering into the current
 * target using the currently set rendering parameters
 *
 * If no existing surface matches, a new one is created. Surfaces are
 * stored in a fixed-size LRU cache.
 */
static EGLSurface m3gSelectGLSurface(RenderContext *ctx)
{
    int attempts = 0;
    int i;

    /* Quick exit for EGL surfaces */

    if (ctx->target.type == SURFACE_EGL) {
        return (EGLSurface) ctx->target.handle;
    }
    
    /* Buffered rendering is handled elsewhere! */
    
    if (ctx->target.buffered) {
        M3G_ASSERT(M3G_FALSE);
        return NULL;
    }

    /* Find the first matching surface and return it */
    
    for (i = 0; i < M3G_MAX_GL_SURFACES; ++i) {
        GLSurfaceRecord *surf = &ctx->glSurface[i];
        
        if ((surf->type == ctx->target.type)
            && (surf->targetHandle == ctx->target.handle)
            && ((ctx->bufferBits & surf->bufferBits) == ctx->bufferBits)
            && (surf->width == ctx->target.width)
            && (surf->height == ctx->target.height)
            && (surf->format == ctx->target.format)
            && (surf->pixels == ctx->target.pixels)) {
            
            surf->lastUseTime = ctx->cacheTimeStamp;
            return surf->handle;
        }
    }

    /* No matching surface found; must create a new one. If the cache
     * is fully occupied, or if we run out of memory, one of the
     * existing surfaces is swapped out */
    
    while (attempts <= 1) {
        
        GLSurfaceRecord *lru = &ctx->glSurface[0];

        /* Find the first entry without a GL surface handle, or the
         * least recently used one if all are occupied. */
        
        for (i = 1; lru->handle != NULL && i < M3G_MAX_GL_SURFACES; ++i) {
            GLSurfaceRecord *surf = &ctx->glSurface[i];
            if (!surf->handle || surf->lastUseTime < lru->lastUseTime) {
                lru = surf;
            }
        }

        /* Delete the existing surface if we hit an occupied slot */
        
        if (lru->handle) {
            m3gDeleteGLSurface(lru->handle);
        }

        /* Create a new surface depending on the type of the current
         * rendering target */
        
        switch (ctx->target.type) {
        case SURFACE_BITMAP:
            lru->handle =
                m3gCreateBitmapSurface(ctx->target.format,
                                       ctx->bufferBits,
                                       (M3GNativeBitmap) ctx->target.handle);
            break;
        case SURFACE_WINDOW:
            lru->handle =
                m3gCreateWindowSurface(ctx->target.format,
                                       ctx->bufferBits,
                                       (M3GNativeWindow) ctx->target.handle);
            break;
        default:
            M3G_ASSERT(M3G_FALSE);
            return NULL;
        }

        /* Success, return the new surface */

        if (lru->handle) {
            lru->type         = ctx->target.type;
            lru->targetHandle = ctx->target.handle;
            lru->bufferBits   = ctx->bufferBits;
            lru->width        = ctx->target.width;
            lru->height       = ctx->target.height;
            lru->format       = ctx->target.format;
            lru->pixels       = ctx->target.pixels;
            lru->lastUseTime  = ctx->cacheTimeStamp;
            return lru->handle;
        }

        /* No surface created, likely due to running out of memory;
         * delete all existing surfaces and try again */
        
        if (!lru->handle) {
            for (i = 0; i < M3G_MAX_GL_SURFACES; ++i) {
                GLSurfaceRecord *surf = &ctx->glSurface[i];
                if (surf->handle) {
                    m3gDeleteGLSurface(surf->handle);
                    surf->handle = NULL;
                    surf->type = SURFACE_NONE;
                }
            }
            ++attempts;
            continue;
        }
    }

    /* Couldn't create a new surface; must return with an error */
    
    m3gRaiseError(M3G_INTERFACE(ctx), M3G_OUT_OF_MEMORY);
    return NULL;
}


/*!
 * \internal
 * \brief Deletes all native surfaces for a specific target
 *
 * \param ctx    rendering context
 * \param type   bitmask of the types of surfaces to remove
 * \param handle native target handle of the surfaces to remove
 */
static void m3gDeleteGLSurfaces(RenderContext *ctx,
                                M3Gbitmask type,
                                M3Gpointer handle)
{
    int i;
    M3G_VALIDATE_OBJECT(ctx);
    
    for (i = 0; i < M3G_MAX_GL_SURFACES; ++i) {
        GLSurfaceRecord *surf = &ctx->glSurface[i];

        if ((surf->type & type) != 0 && surf->targetHandle == handle) {
            m3gDeleteGLSurface(surf->handle);
            
            surf->type = SURFACE_NONE;
            surf->handle = NULL;
        }
    }
}
    
/*!
 * \internal
 * \brief Makes an OpenGL context current to the current rendering target
 */
static void m3gMakeGLCurrent(RenderContext *ctx)
{
    if (ctx != NULL) {
        EGLContext eglCtx = NULL;
        if (ctx->target.buffered) {
            eglCtx = m3gSelectGLContext(
                ctx,
                M3G_RGBA8,
                (M3Gbitmask) M3G_COLOR_BUFFER_BIT |
                             M3G_DEPTH_BUFFER_BIT,
                (M3Gbitmask) EGL_PBUFFER_BIT,
                ctx->backBuffer.glSurface);
            ctx->target.surface = ctx->backBuffer.glSurface;
        }
        else {
            EGLSurface surface = m3gSelectGLSurface(ctx);
            if (surface) {
                eglCtx = m3gSelectGLContext(ctx,
                                   ctx->target.format,
                                   ctx->bufferBits,
                                   ctx->target.type,
                                   surface);
                ctx->target.surface = surface;
            }
        }
        /* Update the current acceleration status */
        
        if (eglCtx) {
            EGLint param;
            eglQueryContext(eglGetCurrentDisplay(),
                            eglCtx, EGL_CONFIG_ID,
                            &param);
            eglGetConfigAttrib(eglGetCurrentDisplay(),
                               m3gEGLConfigForConfigID(eglGetCurrentDisplay(), param), EGL_CONFIG_CAVEAT,
                               &param);
            ctx->accelerated = (param == EGL_NONE);
        }
    }
    else {
        eglMakeCurrent(eglGetDisplay(EGL_DEFAULT_DISPLAY), NULL, NULL, NULL);
    }
}


/*----------------------------------------------------------------------
 * Public API
 *--------------------------------------------------------------------*/

/*!
 * \brief
 */
void m3gBindBitmapTarget(M3GRenderContext hCtx,
                         M3GNativeBitmap hBitmap)
{
    M3GPixelFormat format;
    M3Gint width, height, pixels;
    RenderContext *ctx = (RenderContext *) hCtx;
    M3G_VALIDATE_OBJECT(ctx);
    
    M3G_LOG1(M3G_LOG_RENDERING, "Binding bitmap 0x%08X\n", (unsigned) hBitmap);
    
    if (!m3gglGetNativeBitmapParams(hBitmap, &format, &width, &height, &pixels)) {
        m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_OBJECT);
        return;
    }

    if (!m3gBindRenderTarget(ctx,
                             SURFACE_BITMAP,
                             width, height,
                             format,
                             hBitmap)) {
        return; /* appropriate error raised automatically */
    }

    /* Set the bitmap target specific parameters */
    
    ctx->target.pixels = (void*)pixels;

}

/*!
 * \brief Binds an external EGL surface as a rendering target
 *
 * \param context the M3G rendering context
 * \param surface an EGLSurface cast to M3GEGLSurface
 */
M3G_API void m3gBindEGLSurfaceTarget(M3GRenderContext context,
                                     M3GEGLSurface surface)
{
    RenderContext *ctx = (RenderContext*) context;
    Interface *m3g = M3G_INTERFACE(ctx);
    M3G_VALIDATE_OBJECT(ctx);

    M3G_LOG1(M3G_LOG_RENDERING, "Binding EGL surface 0x%08X\n", (unsigned) surface);
    {
        EGLDisplay dpy = eglGetDisplay(EGL_DEFAULT_DISPLAY);
        EGLSurface surf = (EGLSurface) surface;
        M3Gint width, height;
        
        if (!(eglQuerySurface(dpy, surf, EGL_WIDTH, &width) &&
              eglQuerySurface(dpy, surf, EGL_HEIGHT, &height))) {
            m3gRaiseError(m3g, M3G_INVALID_OBJECT);
            return;
        }

        if (!m3gBindRenderTarget(ctx,
                                 SURFACE_EGL,
                                 width, height,
                                 M3G_RGBA8,
                                 surface)) {
            return; /* error raised automatically */
        }

        /* placeholder for target type specific setup */
    }
}

/*!
 * \brief Binds a new memory rendering target to this rendering
 * context
 *
 * Upon first binding of a specific target, binding the buffer may
 * require auxiliary data to be allocated, depending on the rendering
 * modes set for this context. In that case, the binding will be
 * canceled, and the function will return a non-zero value giving the
 * number of bytes of additional memory that needs to be supplied for
 * binding of that target to succeed. The function must then be called
 * again and a pointer to a sufficient memory block supplied as the \c
 * mem parameter.
 *
 * \param pixels NULL to signal that the frame buffer is accessed
 * using a callback upon rendering time
 */
/*@access M3GGLContext@*/
void m3gBindMemoryTarget(M3GRenderContext context,
                         /*@shared@*/ void *pixels,
                         M3Guint width, M3Guint height,
                         M3GPixelFormat format,
                         M3Guint stride,
                         M3Guint userHandle)
{
    RenderContext *ctx = (RenderContext*) context;
    Interface *m3g = M3G_INTERFACE(ctx);
    M3G_VALIDATE_OBJECT(ctx);

    M3G_LOG1(M3G_LOG_RENDERING, "Binding memory buffer 0x%08X\n",
             (unsigned) pixels);
    
    /* Check for bitmap specific errors */
    
    if (width == 0 || height == 0 || stride < width) {
        m3gRaiseError(m3g, M3G_INVALID_VALUE);
        return; 
    }

    /* Effect the generic target binding */
    
    if (!m3gBindRenderTarget(ctx,
                             SURFACE_MEMORY,
                             width, height,
                             format,
                             userHandle)) {
        return; /* appropriate error raised automatically */
    }

    /* Set the memory target specific parameters */
    
    ctx->target.pixels = pixels;
    ctx->target.stride = stride;
}

/*!
 * \brief
 */
M3G_API void m3gBindWindowTarget(M3GRenderContext hCtx,
                                 M3GNativeWindow hWindow)
{
    M3GPixelFormat format;
    M3Gint width, height;
    RenderContext *ctx = (RenderContext *) hCtx;
    M3G_VALIDATE_OBJECT(ctx);
    
    M3G_LOG1(M3G_LOG_RENDERING, "Binding window 0x%08X\n", (unsigned) hWindow);
    
    if (!m3gglGetNativeWindowParams(hWindow, &format, &width, &height)) {
        m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_OBJECT);
        return;
    }

    if (!m3gBindRenderTarget(ctx,
                             SURFACE_WINDOW,
                             width, height,
                             format,
                             hWindow)) {
        return; /* appropriate error raised automatically */
    }

    /* placeholder for window target specific setup */
}

/*!
 * \brief Invalidate a previously bound bitmap target
 *
 * This should be called prior to deleting a native bitmap that has
 * been used as an M3G rendering target in the past. This erases the
 * object from any internal caches and ensures it will not be accessed
 * in the future.
 *
 * \param hCtx    M3G rendering context
 * \param hBitmap native handle of the bitmap object
 */
M3G_API void m3gInvalidateBitmapTarget(M3GRenderContext hCtx,
                                       M3GNativeBitmap hBitmap)
{
    RenderContext *ctx = (RenderContext *) hCtx;
    M3G_VALIDATE_OBJECT(ctx);

    M3G_LOG1(M3G_LOG_RENDERING, "Invalidating bitmap 0x%08X\n",
             (unsigned) hBitmap);
    
    m3gDeleteGLSurfaces(ctx, (M3Gbitmask) SURFACE_BITMAP, (M3Gpointer) hBitmap);
}

/*!
 * \brief Invalidate a previously bound window target
 *
 * This should be called prior to deleting a native window that has
 * been used as an M3G rendering target in the past. This erases the
 * object from any internal caches and ensures it will not be accessed
 * in the future.
 *
 * \param hCtx    M3G rendering context
 * \param hWindow native handle of the bitmap object
 */
M3G_API void m3gInvalidateWindowTarget(M3GRenderContext hCtx,
                                       M3GNativeWindow hWindow)
{
    RenderContext *ctx = (RenderContext *) hCtx;
    M3G_VALIDATE_OBJECT(ctx);

    M3G_LOG1(M3G_LOG_RENDERING, "Invalidating window 0x%08X\n",
             (unsigned) hWindow);
    
    m3gDeleteGLSurfaces(ctx, (M3Gbitmask) SURFACE_WINDOW, (M3Gpointer) hWindow);
}

/*!
 * \brief Invalidate a previously bound memorytarget
 *
 * This should be called prior to deleting a memory buffer that has
 * been used as an M3G rendering target in the past. 
 *
 * \param hCtx    M3G rendering context
 * \param pixels  pointer to the memory buffer
 */
M3G_API void m3gInvalidateMemoryTarget(M3GRenderContext hCtx,
                                       void *pixels)
{
    RenderContext *ctx = (RenderContext *) hCtx;
    M3G_VALIDATE_OBJECT(ctx);

    M3G_LOG1(M3G_LOG_RENDERING, "Invalidating memory target 0x%08X\n",
             (unsigned) pixels);
    
    m3gDeleteGLSurfaces(ctx, (M3Gbitmask) SURFACE_MEMORY, (M3Gpointer) pixels);
}
