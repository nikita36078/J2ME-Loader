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
* Description: OpenGL utility functions and definitions
*
*/

#ifndef __M3G_GL_H__
#define __M3G_GL_H__

/*!
 * \internal
 * \file
 * \brief OpenGL utility functions and definitions
 */

#include "M3G/m3g_core.h"
#include "m3g_defs.h"

#if !(defined(M3G_NGL_CONTEXT_API) || defined(M3G_NGL_TEXTURE_API))
#   include <GLES/gl.h>
#   include <EGL/egl.h>
#else

/*@notfunction@*/
#   define NGL_PREFIX(func)     gl ## func
/*@notfunction@*/
#   define NGLI_PREFIX(func)    ngl ## func
/*@notfunction@*/
#   define NGLU_PREFIX(func)    nglu ## func
#   include "ngl.h"

#endif

#if defined(__cplusplus)
extern "C" {
#endif
    
/*----------------------------------------------------------------------
 * Handy macros
 *--------------------------------------------------------------------*/

#if defined(M3G_DEBUG_ASSERTS)
    static M3G_INLINE void m3gAssertGL(const char *filename, int line)
    {
        GLint err = glGetError();
        if (err != GL_NO_ERROR) {
            M3G_LOG3(M3G_LOG_FATAL_ERRORS, "GL error 0x%X (%s: %d)\n",
                     err, filename, line);
            M3G_ASSERT(M3G_FALSE);
        }
    }
#   define M3G_ASSERT_GL m3gAssertGL(__FILE__, __LINE__)
#else
#   define M3G_ASSERT_GL
#endif

#define M3G_GLTYPE(m3gType) ((m3gType) + 0x1400)
#define M3G_M3GTYPE(glType) ((glType) - 0x1400)

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/
    
static GLenum m3gGetGLFormat(M3GPixelFormat format);    /* m3g_image.inl */
static void m3gGLColor(M3Guint argb, GLfixed *dst);     /* m3g_image.c */

#if !defined(M3G_NGL_CONTEXT_API)
static void m3gInitializeEGL(void);     /* m3g_rendercontext.inl */
static void m3gTerminateEGL(void);      /* m3g_rendercontext.inl */
#endif

/*----------------------------------------------------------------------
 * Native platform abstraction layer
 *--------------------------------------------------------------------*/
    
#if !defined(M3G_NGL_CONTEXT_API)
    
M3Gbool m3gglLockNativeBitmap(M3GNativeBitmap bitmap,
                              M3Gubyte **ptr,
                              M3Gsizei *stride);
void    m3gglReleaseNativeBitmap(M3GNativeBitmap bitmap);
    
M3Gbool m3gglGetNativeBitmapParams(M3GNativeBitmap bitmap,
                                   M3GPixelFormat *format,
                                   M3Gint *width, M3Gint *height, M3Gint *pixels);
M3Gbool m3gglGetNativeWindowParams(M3GNativeWindow wnd,
                                   M3GPixelFormat *format,
                                   M3Gint *width, M3Gint *height);
    
#endif /* M3G_NGL_CONTEXT_API */
    
#if defined(__cplusplus)
} /* extern "C" */
#endif

#endif /*__M3G_GL_H__*/
