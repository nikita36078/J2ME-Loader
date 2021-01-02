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
* Description: Rendering context function implementations
*
*/


/*!
 * \internal
 * \file
 * \brief Rendering context function implementations
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_rendercontext.h"
#include "m3g_object.h"

#include "m3g_gl.h"
#include "m3g_memory.h"
#include "m3g_appearance.h"
#include "m3g_indexbuffer.h"
#include "m3g_lightmanager.h"
#include "m3g_vertexbuffer.h"
#include "m3g_world.h"

/*----------------------------------------------------------------------
 * Private data types
 *--------------------------------------------------------------------*/

#if defined(M3G_NGL_CONTEXT_API)
/*!
 * \internal
 * \brief Depth buffer data
 */
typedef struct
{
    M3GMemObject handle;
    M3Gsizei size;
} DepthBuffer;
#endif /*M3G_NGL_CONTEXT_API*/

#if !defined(M3G_NGL_CONTEXT_API)
/*! \internal \brief OpenGL rendering context record */
typedef struct {
    EGLContext handle;
    M3GPixelFormat format;
    M3Gbitmask bufferBits;
    M3Gbitmask surfaceTypeBits;
    M3Gbitmask modeBits;
    M3Guint lastUseTime;
} GLContextRecord;

/*! \internal \brief OpenGL surface record */
typedef struct {
    EGLSurface handle;
    M3Gbitmask bufferBits;
    M3Gbitmask type;
    M3Guint width;
    M3Guint height;
    M3Guint format;
    M3Gpointer targetHandle;
    void* pixels;
    M3Guint lastUseTime;
} GLSurfaceRecord;
#endif /*!M3G_NGL_CONTEXT_API*/

/*!
 * \internal \brief Rendering target data
 */
typedef struct 
{
    M3Gbitmask type;
    M3GPixelFormat format;
    M3Gint width, height;
    M3Guint stride;
    /*@shared@*/ void *pixels, *lockedPixels;
    EGLSurface surface;
    M3Gpointer handle;
    M3Guint userData;
    
    /*!
     * \internal
     * \brief Flag set to indicate back buffer rendering
     *
     * The final target is only written to, via a format
     * conversion, when releasing the target.
     */
    M3Gbool buffered;
} RenderTarget;

/*!
 * \internal
 * \brief Back color buffer data
 */
typedef struct {
#   if defined(M3G_NGL_CONTEXT_API)
    M3GMemObject handle;
    M3Gsizei size;
#   else
    M3Gint width, height;
    EGLSurface glSurface;
#   endif /* M3G_NGL_CONTEXT_API */
    M3Gbool contentsValid;
} BackBuffer;

/*!
 * \internal
 * \brief Rendering context data structure
 *
 * This includes data related to a specific rendering context,
 * including e.g.  viewport settings, and active lights and
 * camera. This is equivalent to the Graphics3D class in the Java API.
 */
struct M3GRenderContextImpl
{
    Object object;
    
    RenderTarget target;
    BackBuffer backBuffer;
#   if defined(M3G_NGL_CONTEXT_API)
    DepthBuffer depthBuffer;
#   endif

#   if !defined(M3G_NGL_CONTEXT_API)
    
    /* OpenGL context and surface caches */
    
    GLContextRecord glContext[M3G_MAX_GL_CONTEXTS];
    GLSurfaceRecord glSurface[M3G_MAX_GL_SURFACES];
    M3Guint cacheTimeStamp;
    
#   endif /* M3G_NGL_CONTEXT_API */

    /*! \internal \brief Current/last rendering mode */
    M3Genum renderMode;
    
    /*! \internal \brief OpenGL viewing transformation */
    GLfloat viewTransform[16];

    /*! \internal \brief Current camera */
    const Camera *camera;

    /*! \internal \brief Light manager component */
    LightManager lightManager;

    /*! \internal \brief Last used scope, to speed up light selection */
    M3Gint lastScope;

	M3Gfloat depthNear;
	M3Gfloat depthFar;
    
    /*! \internal \brief Clipping rectangle parameters */
    struct { M3Gint x0, y0, x1, y1; } clip;

    /*! \internal \brief Scissor and viewport rectangles */
    struct { GLint x, y, width, height; } scissor, viewport;

    /*! \internal \brief Physical display size */
    struct { M3Gint width, height; } display;
    
    M3Gbitmask bufferBits;      /*!< \brief Rendering buffer bits */
    M3Gbitmask modeBits;        /*!< \brief Rendering mode bits */

    /*! \internal \brief OpenGL subsystem initialization flag */
    M3Gbool glInitialized;

    /*! \internal \brief HW acceleration status flag */
    M3Gbool accelerated;
    
    /*! \internal \brief Render queue for this context */
	RenderQueue *renderQueue;

    M3Gbool currentColorWrite;
    M3Gbool currentAlphaWrite;
    M3Gbool inSplitDraw;
    M3Gbool alphaWrite;
};

/*
 * Rendering target types; note that the values here MUST match the
 * respective EGL bit values
 */
enum SurfaceType {
    SURFACE_NONE = 0,
    SURFACE_IMAGE = 0x01,       /* EGL_PBUFFER_BIT */
    SURFACE_BITMAP = 0x02,      /* EGL_PIXMAP_BIT */
    SURFACE_WINDOW = 0x04,      /* EGL_WINDOW_BIT */
    SURFACE_MEMORY = SURFACE_IMAGE | SURFACE_BITMAP | SURFACE_WINDOW,
    SURFACE_EGL = 0x80
};

enum RenderMode {
    RENDER_IMMEDIATE,
    RENDER_NODES,
    RENDER_WORLD
};

/*----------------------------------------------------------------------
 * Platform specific code
 *--------------------------------------------------------------------*/

static M3Gbool m3gBindRenderTarget(RenderContext *ctx,
                                   M3Genum targetType,
                                   M3Gint width, M3Gint height,
                                   M3GPixelFormat format,
                                   M3Gpointer handle);
static void m3gResetRectangles(RenderContext *ctx);
static void m3gSetGLDefaults(void);
static void m3gUpdateScissor(RenderContext *ctx);
static void m3gValidateBuffers(RenderContext *ctx);
static M3Gbool m3gValidTargetFormat(M3GPixelFormat format);

#include "m3g_rendercontext.inl"

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Rendering context destructor
 *
 */
static void m3gDestroyContext(/*@only@*/ Object *obj)
{
    RenderContext *ctx = (RenderContext *) obj;
    M3G_VALIDATE_OBJECT(ctx);

    M3G_ASSIGN_REF(ctx->camera, NULL);
    
#   if defined(M3G_NGL_CONTEXT_API)
    if (ctx->target.type == SURFACE_MEMORY && ctx->target.pixels == NULL) {
        m3gSignalTargetRelease(M3G_INTERFACE(ctx), ctx->target.handle);
    }

    m3gFreeObject(M3G_INTERFACE(ctx), ctx->depthBuffer.handle);
    m3gFreeObject(M3G_INTERFACE(ctx), ctx->backBuffer.handle);
    
#   else /* !M3G_NGL_CONTEXT_API */
    
    {
        int i;
        for (i = 0; i < M3G_MAX_GL_CONTEXTS; ++i) {
            if (ctx->glContext[i].handle != 0) {
                m3gDeleteGLContext(ctx->glContext[i].handle);
            }
        }
        for (i = 0; i < M3G_MAX_GL_SURFACES; ++i) {
            if (ctx->glSurface[i].handle != 0) {
                m3gDeleteGLSurface(ctx->glSurface[i].handle);
            }
        }
    }

#   endif /* M3G_NGL_CONTEXT_API */
    
    if (ctx->glInitialized) {
        m3gShutdownGL(M3G_INTERFACE(ctx));
    }

    m3gDestroyLightManager(&ctx->lightManager, M3G_INTERFACE(ctx));
    m3gDestroyRenderQueue(M3G_INTERFACE(ctx), ctx->renderQueue);
    m3gDestroyObject(obj);
}

/*!
 * \internal
 * \brief Resets the clipping and viewport rectangles to defaults
 *
 * This is called after binding a new target.
 */
static void m3gResetRectangles(RenderContext *ctx)
{
    int w = ctx->display.width;
    int h = ctx->display.height;
    
    ctx->clip.x0 = 0;
    ctx->clip.y0 = ctx->target.height - ctx->display.height;
    ctx->clip.x1 = w;
    ctx->clip.y1 = ctx->clip.y0 + h;

    ctx->viewport.x = 0;
    ctx->viewport.y = 0;
    ctx->viewport.width = M3G_MIN(w, M3G_MAX_VIEWPORT_DIMENSION);
    ctx->viewport.height = M3G_MIN(h, M3G_MAX_VIEWPORT_DIMENSION);
}

/*!
 * \internal
 * \brief Constrains the clip rectangle to the rendering target.
 */
static void m3gValidateClipRect(RenderContext *ctx)
{
    int xMin = 0;
    int xMax = ctx->display.width;
    int yMin = ctx->target.height - ctx->display.height;
    int yMax = yMin + ctx->display.height;
    
    ctx->clip.x0 = m3gClampInt(ctx->clip.x0, xMin, xMax);
    ctx->clip.y0 = m3gClampInt(ctx->clip.y0, yMin, yMax);
    ctx->clip.x1 = m3gClampInt(ctx->clip.x1, xMin, xMax);
    ctx->clip.y1 = m3gClampInt(ctx->clip.y1, yMin, yMax);
}

/*!
 * \internal
 * \brief Computes the GL scissor rectangle
 *
 * The scissor rectangle is the intersection of the viewport and the
 * clipping rectangle.
 */
static void m3gUpdateScissor(RenderContext *ctx)
{
    int sx0 = ctx->viewport.x;
    int sy0 = ctx->viewport.y;
    int sx1 = sx0 + ctx->viewport.width;
    int sy1 = sy0 + ctx->viewport.height;

    sx0 = M3G_MAX(sx0, ctx->clip.x0);
    sy0 = M3G_MAX(sy0, ctx->clip.y0);
    sx1 = M3G_MIN(sx1, ctx->clip.x1);
    sy1 = M3G_MIN(sy1, ctx->clip.y1);

    ctx->scissor.x = sx0;
    ctx->scissor.y = sy0;
    
    if (sx0 < sx1 && sy0 < sy1) {
        ctx->scissor.width = sx1 - sx0;
        ctx->scissor.height = sy1 - sy0;
    }
    else {
        ctx->scissor.width = ctx->scissor.height = 0;
    }
}

/*!
 * \internal
 * \brief Checks whether we can render in a given format
 */
static M3Gbool m3gValidTargetFormat(M3GPixelFormat format)
{
    return m3gInRange(format, M3G_RGB8, M3G_RGBA4);
}

/*!
 * \internal
 * \brief Checks whether a given format has alpha
 */
static M3Gbool m3gFormatHasAlpha(M3GPixelFormat format)
{
    switch (format) {
    case M3G_A8:
    case M3G_LA8:
    case M3G_LA4:
    case M3G_ARGB8:
    case M3G_RGBA8:
    case M3G_BGRA8:
    case M3G_RGBA4:
    case M3G_RGB5A1:
    case M3G_PALETTE8_RGBA8:
        return M3G_TRUE;
    default:
        return M3G_FALSE;
    }
}

/*!
 * \internal
 * \brief Sets the global alpha write enable flag. 
 *
 *	Used for disabling the alpha channel writes when the rendering 
 *  target is a Java MIDP Image that has an alpha channel.
 *
 * \param ctx        the rendering context
 * \param enable     alpha write enable flag
 */
M3G_API void m3gSetAlphaWrite(M3GRenderContext ctx, M3Gbool enable)
{
	ctx->alphaWrite = enable;
}

/*!
 * \internal
 * \brief Reads the global alpha write enable flag. 
 *
 * \param ctx        the rendering context
 */
M3G_API M3Gbool m3gGetAlphaWrite(M3GRenderContext ctx)
{
	return ctx->alphaWrite;
}

/*!
 * \brief Frees all GLES resources allocated by the M3G API 
 *        (EGL surfaces, contexts and texture objects). 
 *
 * \note M3G must not be bound to any target when calling this.
 *
 */
M3G_API void m3gFreeGLESResources(M3GRenderContext ctx)
{
#ifdef M3G_ENABLE_GLES_RESOURCE_HANDLING

    PointerArray image2DObjects;
    M3Gint i;

    /* M3G must not be bound to a rendering target at this point. */
    if (ctx->target.type != SURFACE_NONE) {
        m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_OPERATION);
    }

    /* EGL might not be initialized yet, so do it here just in case. */ 
    eglInitialize(eglGetDisplay(EGL_DEFAULT_DISPLAY), NULL, NULL);
    eglMakeCurrent(eglGetDisplay(EGL_DEFAULT_DISPLAY), NULL, NULL, NULL);

    /* Delete EGL surfaces */
    for (i = 0; i < M3G_MAX_GL_SURFACES; ++i) {
        GLSurfaceRecord *surf = &ctx->glSurface[i];
        if (surf->handle) {
            m3gDeleteGLSurface(surf->handle);
        }
        m3gZero(surf, sizeof(GLSurfaceRecord));
    }
    if (ctx->backBuffer.glSurface != NULL) {
        m3gDeleteGLSurface(ctx->backBuffer.glSurface);
        m3gZero(&ctx->backBuffer, sizeof(BackBuffer));
    }

    /* Delete EGL contexts */
    for (i = 0; i < M3G_MAX_GL_CONTEXTS; ++i) {
        GLContextRecord *context = &ctx->glContext[i];
        if (context->handle) {
            m3gDeleteGLContext(context->handle);
        }
        m3gZero(context, sizeof(GLContextRecord));
    }

    /* Delete references to GLES texture objects from all live Image2D objects. 
       Texture objects themselves have already been destroyed with the last GL context. */

    m3gInitArray(&image2DObjects);
    m3gGetObjectsWithClassID(M3G_INTERFACE(ctx), M3G_CLASS_IMAGE, &image2DObjects);

    i = m3gArraySize(&image2DObjects);

    while (i > 0) {
        Image *image = (Image*)m3gGetArrayElement(&image2DObjects, --i);

        m3gInvalidateImage(image);
        image->texObject = 0;
    }
    m3gDestroyArray(&image2DObjects, M3G_INTERFACE(ctx));
#endif
}


/*!
 * \internal
 * \brief Sets up a new rendering target
 *
 * \param ctx        the rendering context
 * \param targetType rendering target type
 * \param width      width of the target
 * \param height     height of the target
 * \param format     target pixel format
 * \param handle     user object handle
 */
static M3Gbool m3gBindRenderTarget(RenderContext *ctx,
                                   M3Genum targetType,
                                   M3Gint width, M3Gint height,
                                   M3GPixelFormat format,
                                   M3Gpointer handle)
{
    /* Check for generic errors */
    
    if (ctx->target.type != SURFACE_NONE) {
        m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_OPERATION);
        return M3G_FALSE;
    }
    if (!m3gValidTargetFormat(format)) {
        m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_ENUM);
        return M3G_FALSE;
    }

    /* If target width or height exceeds maximum viewport width or height
       an exception is thrown. */
    
    if (width > M3G_MAX_VIEWPORT_WIDTH ||
        height > M3G_MAX_VIEWPORT_HEIGHT) {
        m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_ENUM);
        return M3G_FALSE;
    }

    /* Everything checks out; set up the target parameters */
    
    ctx->target.type = targetType;
    ctx->target.width = width;
    ctx->target.height = height;
    ctx->display.width = width;
    ctx->display.height = height;
    ctx->target.format = format;
    ctx->target.handle = handle;
    m3gResetRectangles(ctx);
    m3gUpdateScissor(ctx);
    m3gValidateBuffers(ctx);
    
    /* Invalidate lights in case we're using a different OpenGL
     * rendering context this time around */
    
    ctx->lastScope = 0;
    
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Initializes the current GL context to default settings.
 */
static void m3gSetGLDefaults(void)
{
	static const GLfloat black[] = {0.f, 0.f, 0.f, 0.f};
    glEnable(GL_NORMALIZE);
    glEnable(GL_SCISSOR_TEST);
	glLightModelfv(GL_LIGHT_MODEL_AMBIENT, black);
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
}

/*!
 * \internal
 * \brief Validates the buffers required for a rendering context
 *
 * Allocates or reallocates buffers as necessary, according to the
 * currently set flags of the context.
 */
static void m3gValidateBuffers(RenderContext *ctx)
{
    M3G_VALIDATE_OBJECT(ctx);

    /* Initialize OpenGL if not already done */
    
    if (!ctx->glInitialized) {
        m3gInitializeGL(M3G_INTERFACE(ctx));
        ctx->glInitialized = M3G_TRUE;
    }

    /* Check whether we can render directly to the target or need to
     * use a back buffer */
    
    ctx->target.buffered = !m3gCanDirectRender(ctx);
#   if defined(M3G_FORCE_BUFFERED_RENDERING)
    ctx->target.buffered = M3G_TRUE;
#   endif
    
    /* If direct rendering wasn't possible, check that the back buffer
     * for buffered rendering exists. */
    
    if (ctx->target.buffered) {
        if (!m3gValidateBackBuffer(ctx)) {
            return; /* out of memory */
        }
    }

    /* With the legacy NGL API, we also manage the depth buffer */
    
#   if defined(M3G_NGL_CONTEXT_API)
    if (!m3gValidateDepthBuffer(ctx)) {
        return; /* out of memory */
    }
#   endif

    /* Delay blitting from the front buffer until we know it's
     * necessary; let's raise a flag to check that later on */
    
    if (ctx->target.buffered) {
        if (ctx->modeBits & M3G_OVERWRITE_BIT) {
            ctx->backBuffer.contentsValid = M3G_TRUE;
        }
        else {
            ctx->backBuffer.contentsValid = M3G_FALSE;
        }
    }
}

/*!
 * \internal
 * \brief Makes a GL context current to this thread and the currently
 * set rendering target buffer
 */
static void m3gMakeCurrent(RenderContext *ctx)
{
    m3gMakeGLCurrent(ctx);

    /* Note that the depth buffer may in some cases exist even if not
     * explicitly requested, so we need to disable the depth test just
     * in case */
    
    if ((ctx->bufferBits & M3G_DEPTH_BUFFER_BIT) == 0) {
        glDisable(GL_DEPTH_TEST);
    }
    else {
        glEnable(GL_DEPTH_TEST);
    }

    /* Enable multisampling if required */

    if (ctx->modeBits & M3G_ANTIALIAS_BIT) {
        glEnable(GL_MULTISAMPLE);
    }
    else {
        glDisable(GL_MULTISAMPLE);
    }
    
    M3G_ASSERT_GL;
}

/*!
 * \internal
 * \brief Returns the HW acceleration status of the current context
 */
static M3Gbool m3gIsAccelerated(const RenderContext *ctx)
{
    return ctx->accelerated;
}

/*!
 * \internal
 * \brief Sets the currently enabled lights to the GL state
 *
 * \note the correct viewing matrix *must* be set prior to calling
 * this for the lights to be transformed into eye space correctly
 */
static M3G_INLINE void m3gApplyLights(RenderContext *ctx, M3Gint scope)
{
    if (ctx->lastScope != scope) {

        /* If coming from RenderNode, we have the geometry in camera
         * space but the lights in world space, so we need to apply
         * the viewing matrix to the lights only */
        
        if (ctx->renderMode == RENDER_NODES) {
            glPushMatrix();
            glLoadMatrixf(ctx->viewTransform);
        }
        
        m3gSelectGLLights(&ctx->lightManager, 8, scope, 0, 0, 0);
        ctx->lastScope = scope;
        
        if (ctx->renderMode == RENDER_NODES) {
            glPopMatrix();
        }
    }
	M3G_ASSERT_GL;
}

/*!
 * \internal
 * \brief Gets the current camera
 */
static const Camera *m3gGetCurrentCamera(const RenderContext *ctx) {
	return ctx->camera;	
}

/*!
 * \internal
 * \brief Sets up some rendering parameters that
 * do not change during scene renders.
 */
static void m3gInitRender(M3GRenderContext context, M3Genum renderMode)
{
    RenderContext *ctx = (RenderContext *) context;
    M3G_VALIDATE_OBJECT(ctx);
    
    m3gIncrementRenderTimeStamp(ctx);
    m3gMakeCurrent(ctx);
    m3gCollectGLObjects(M3G_INTERFACE(ctx));
    
    /* If buffered rendering, blit the image to the back buffer at
     * this point */
    
    if (ctx->target.buffered && !ctx->backBuffer.contentsValid) {
        m3gUpdateBackBuffer(ctx);
    }

    /* Set up viewport and scissoring */
    
    glViewport(ctx->viewport.x, ctx->viewport.y,
               ctx->viewport.width, ctx->viewport.height);
	glDepthRangef(ctx->depthNear, ctx->depthFar);
    glScissor(ctx->scissor.x, ctx->scissor.y,
              ctx->scissor.width, ctx->scissor.height);
    M3G_ASSERT_GL;
    
    /* Set up the projection and viewing transformations (static
     * during rendering) */

	m3gApplyProjection(ctx->camera);
    if (renderMode == RENDER_NODES) {
        glLoadIdentity();
    }
    else {
        glLoadMatrixf(ctx->viewTransform);
    }
    M3G_ASSERT_GL;

    /* Invalidate any already set GL lights if rendering mode changed */
    
    if (renderMode != ctx->renderMode) {
        ctx->lastScope = 0;
    }
    ctx->renderMode = renderMode;
}

/*!
 * \internal
 * \brief A workaround for a broken implementation of glColorMask
 *
 * Saves the framebuffer in the OpenGL default texture each time the
 * color mask changes, for restoring later.  Not very pretty, but
 * works as long as the default texture is not touched in between --
 * currently, we only touch that when copying to and from the back
 * buffer.
 *
 * \param newColorWrite the color mask state we're about to change to
 * \param newAlphaWrite the alpha write state we're about to change to
 */
static void m3gUpdateColorMaskStatus(RenderContext *ctx,
                                     M3Gbool newColorWrite,
                                     M3Gbool newAlphaWrite)
{
    GLint pow2Width, pow2Height;

	/* Get the global alpha write value */
	newAlphaWrite &= m3gGetAlphaWrite(ctx);

    /* Check that the ColorMask state is actually about to change */
    
    if (ctx->currentColorWrite == newColorWrite
        && (ctx->currentAlphaWrite == newAlphaWrite || !m3gFormatHasAlpha(ctx->target.format))) {
        return; /* no change, quick exit */
    }
    
    pow2Width = m3gNextPowerOfTwo(ctx->clip.x1 - ctx->clip.x0);
    pow2Height = m3gNextPowerOfTwo(ctx->clip.y1 - ctx->clip.y0);
    
    /* If we previously had stored something, restore it now */

    if (ctx->currentColorWrite != ctx->currentAlphaWrite) {
        
        /* Disable any stray state we don't want */

        glDisable(GL_CULL_FACE);
        glDisable(GL_ALPHA_TEST);
        glDisableClientState(GL_NORMAL_ARRAY);
        glDisableClientState(GL_COLOR_ARRAY);
        glDisable(GL_LIGHTING);
        glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
        glDepthMask(GL_FALSE);
        glDepthFunc(GL_ALWAYS);
        m3gDisableTextures();
        M3G_ASSERT_GL;
    
        /* Bind the default texture and set up screen space rendering */
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        M3G_ASSERT_GL;

        glScissor(ctx->clip.x0, ctx->clip.y0,
                  ctx->clip.x1 - ctx->clip.x0, ctx->clip.y1 - ctx->clip.y0);
        m3gPushScreenSpace(ctx, M3G_FALSE);
        glViewport(0, 0, ctx->target.width, ctx->target.height);
        glMatrixMode(GL_PROJECTION);
        glOrthox(0, ctx->target.width << 16,
                 0, ctx->target.height << 16,
                 -1 << 16, 1 << 16);
        glMatrixMode(GL_MODELVIEW);
            
        /* Set up texture and vertex coordinate arrays */

        glClientActiveTexture(GL_TEXTURE0);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glEnableClientState(GL_VERTEX_ARRAY);
        glMatrixMode(GL_TEXTURE);
        glLoadIdentity();
        glMatrixMode(GL_MODELVIEW);
        M3G_ASSERT_GL;

        /* Blend the texture with the frame buffer */
        {
            static const M3Gbyte tc[8] = { 0, 0, 0, 1, 1, 0, 1, 1 };
            GLshort pos[8];
            
            GLfixed cm = (GLfixed)(ctx->currentColorWrite ? 0 : 1 << 16);
            GLfixed am = (GLfixed)(ctx->currentAlphaWrite ? 0 : 1 << 16);

            glVertexPointer(2, GL_SHORT, 0, pos);
            glTexCoordPointer(2, GL_BYTE, 0, tc);
                
            pos[0] = (GLshort) ctx->clip.x0;
            pos[1] = (GLshort) ctx->clip.y0;
            pos[2] = pos[0];
            pos[3] = (GLshort) (pos[1] + pow2Height);
            pos[4] = (GLshort) (pos[0] + pow2Width);
            pos[5] = pos[1];
            pos[6] = pos[4];
            pos[7] = pos[3];
            
            glEnable(GL_BLEND);
            glColor4x(cm, cm, cm, am);

            /* Zero the masked channels */
            
            glBlendFunc(GL_ZERO, GL_ONE_MINUS_SRC_COLOR);
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

            /* Add the masked channels from the stored texture */
            
            glEnable(GL_TEXTURE_2D);
            glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
            glBlendFunc(GL_ONE, GL_ONE);
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        }
            
        /* Restore the mandatory state */
            
        glScissor(ctx->scissor.x, ctx->scissor.y,
                  ctx->scissor.width, ctx->scissor.height);
        glViewport(ctx->viewport.x, ctx->viewport.y,
                   ctx->viewport.width, ctx->viewport.height);
        m3gPopSpace(ctx);
    }
    
    /* Copy the current clip rectangle into the default texture if
     * we're going to be rendering with unsupported masks in effect */
    
    if (newColorWrite != newAlphaWrite) {
        GLenum err;
            
        glBindTexture(GL_TEXTURE_2D, 0);
        M3G_ASSERT_GL;
            
        glCopyTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA,
                         ctx->clip.x0, ctx->clip.y0,
                         pow2Width, pow2Height,
                         0);
        err = glGetError();
        if (err == GL_INVALID_OPERATION) {
            /* Incompatible FB format -- must be GL_RGB then */
            glCopyTexImage2D(GL_TEXTURE_2D, 0, GL_RGB,
                             ctx->clip.x0, ctx->clip.y0,
                             pow2Width, pow2Height,
                             0);
            err = glGetError();
        }
        if (err == GL_OUT_OF_MEMORY) {
            m3gRaiseError(M3G_INTERFACE(ctx), M3G_OUT_OF_MEMORY);
        }
        M3G_ASSERT(!err);
    }
    else {
        
        /* Texture not needed for now, so allow GL to free some
         * resources */
        
        glTexImage2D(GL_TEXTURE_2D, 0,
                     GL_RGBA,
                     1, 1,
                     0,
                     GL_RGBA, GL_UNSIGNED_BYTE, NULL);
    }
    
    ctx->currentColorWrite = newColorWrite;
    ctx->currentAlphaWrite = newAlphaWrite;
}

/*!
 * \internal
 * \brief Sets the GL to input screen space coordinates
 *
 * This pushes the current modelview and projection matrices into the
 * matrix stack, then sets up an orthogonal projection and an identity
 * modelview matrix.
 * 
 * \param ctx the rendering context
 * \param realPixels M3G_TRUE to use actual pixel coordinates,
 * M3G_FALSE to use normalized device coordinates
 */
static void m3gPushScreenSpace(RenderContext *ctx, M3Gbool realPixels)
{
    M3G_VALIDATE_OBJECT(ctx);
    
    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();
    if (realPixels) {
        int w = ctx->viewport.width;
        int h = ctx->viewport.height;
        glOrthox(0, w << 16, 0, h << 16, -1 << 16, 1 << 16);
    }
    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();
}

/*!
 * \internal
 * \brief Restores the projection and modelview matrix modified by
 * m3gPushScreenSpace
 */
static void m3gPopSpace(RenderContext *ctx)
{
    M3G_VALIDATE_OBJECT(ctx);
    
    M3G_UNREF(ctx);
    glMatrixMode(GL_PROJECTION);
    glPopMatrix();
    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();
}

/*!
 * \internal
 * \brief Clears the current buffer(s)
 */
static void m3gClearInternal(RenderContext *ctx, Background *bg)
{
    m3gMakeCurrent(ctx);
    
    /* If buffered rendering, copy data to the back buffer at this
     * point if we're not clearing the whole clip rectangle */
    
    if (ctx->target.buffered && !ctx->backBuffer.contentsValid) {
        if (ctx->scissor.x > ctx->clip.x0 || ctx->scissor.y > ctx->clip.y0 ||
            ctx->scissor.x + ctx->scissor.width < ctx->clip.x1 ||
            ctx->scissor.y + ctx->scissor.height < ctx->clip.y1) {
            m3gUpdateBackBuffer(ctx);
        }
    }

    if (m3gGetColorMaskWorkaround(M3G_INTERFACE(ctx))) {
        m3gUpdateColorMaskStatus(ctx, M3G_TRUE, M3G_TRUE);
    }

    glDepthMask(GL_TRUE);
    glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, m3gGetAlphaWrite(ctx));
    glDepthRangef(ctx->depthNear, ctx->depthFar);
    glViewport(ctx->viewport.x, ctx->viewport.y,
               ctx->viewport.width, ctx->viewport.height);
    glScissor(ctx->scissor.x, ctx->scissor.y,
              ctx->scissor.width, ctx->scissor.height);

    /* Touch the background image to make sure it's created prior to
     * locking memory for rendering */
    
    if (bg != NULL && bg->image != NULL) {
        if (!m3gGetPowerOfTwoImage(bg->image)) {
            return; /* out of memory */
        }
    }

    /* All clear for clearing... */
    
    m3gLockFrameBuffer(ctx);
    
    if (bg != NULL) {
        m3gApplyBackground(ctx, bg);
        if (ctx->target.buffered && bg->colorClearEnable) {
            ctx->backBuffer.contentsValid = M3G_TRUE;
        }
    }
    else {
        glClearColorx(0, 0, 0, 0);
        glClearDepthx(1 << 16);
        glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
        if (ctx->target.buffered) {
            ctx->backBuffer.contentsValid = M3G_TRUE;
        }
    }
        
    m3gReleaseFrameBuffer(ctx);
}

/*!
 * \internal
 * \brief Draws a batch of primitives
 *
 * This is the place most rendering commands are eventually routed to;
 * sprites and backgrounds are the only exception to this. We assume
 * that all eror checking has been performed at this point.
 */
static void m3gDrawMesh(RenderContext *ctx,
                        const VertexBuffer *vb,
                        const IndexBuffer *ib,
                        const Appearance *app,
                        const M3GMatrix *modelTransform,
                        M3Gint alphaFactor,
                        M3Gint scope)
{
    M3G_VALIDATE_OBJECT(ctx);
    M3G_VALIDATE_OBJECT(vb);
    M3G_VALIDATE_OBJECT(ib);
    M3G_VALIDATE_OBJECT(app);

    /* Check whether we need to use alternate rendering to get
     * two-sided lighting */
    if (m3gGetTwoSidedLightingWorkaround(M3G_INTERFACE(ctx))) {
        if (m3gSplitDrawMesh(ctx, vb, ib, app, modelTransform, alphaFactor, scope)) {
            return;
        }
    }

    M3G_ASSERT(m3gInRange(alphaFactor, 0, 0x10000));
    
    if (m3gGetColorMaskWorkaround(M3G_INTERFACE(ctx))) {
		m3gUpdateColorMaskStatus(ctx, m3gColorMask(app), m3gAlphaMask(app));
    }
    
    /* Load lights */
    
    m3gApplyLights(ctx, scope);
    
    /* Apply the extra modeling transformation if present */
    
    if (modelTransform != NULL) {
		float transform[16];
		m3gGetMatrixColumns(modelTransform, transform);
        
        glPushMatrix();
        glMultMatrixf(transform);
    }

    /* Check whether we need to create an alpha-factored color cache
     * for the vertex buffer; this requires unlocking the frame buffer
     * for a while, and we may even run out of memory in the process,
     * but we still need to exit with the frame buffer lock and the
     * matrix stack in the expected state */

    if (alphaFactor < 0x10000 && !m3gValidateAlphaCache(vb)) {
        M3Gbool ok;
        m3gReleaseFrameBuffer(ctx);
        ok = m3gCreateAlphaColorCache(vb->colors);
        m3gLockFrameBuffer(ctx);
        if (!ok) {
            goto RestoreModelview; /* let's just skip the drawing part */
        }
    }

#   if defined(M3G_NGL_TEXTURE_API)
    /* Similarly to the alpha cache above, also check whether any
     * textures may need to allocate mipmaps at this point */
    {
        M3Gint i;
        for (i = 0; i < M3G_NUM_TEXTURE_UNITS; ++i) {
            Texture *tex = app->texture[i];
            if (tex && !m3gValidateTextureMipmapping(tex)) {
                M3Gbool ok;
                m3gReleaseFrameBuffer(ctx);
                ok = m3gValidateMipmapMemory(m3gGetTextureImage(tex));
                m3gLockFrameBuffer(ctx);
                if (!ok) {
                    goto RestoreModelview;
                }
            }
        }
    }
#   endif
    
    /* Load up the rest of the stuff we need for rendering; note that
     * the vertex buffer scale and bias apply to the texture matrix
     * from the appearance object, so they need to be applied last */
    
    m3gApplyAppearance(app, ctx, alphaFactor);
    m3gLockVertexBuffer(vb, alphaFactor);
    m3gApplyScaleAndBias(vb);
    
    /* All ready, render and then release the stuff we bound above */
    
    m3gSendIndexBuffer(ib);
    m3gReleaseVertexBuffer(vb);
    m3gReleaseTextures(app);

    /* Restore viewing-only modelview if changed */

RestoreModelview:
    if (modelTransform != NULL) {
        glPopMatrix();
    }
}

/*!
 * \internal
 * \brief Validates background format against current target
 *
 * \retval M3G_TRUE valid format
 * \retval M3G_FALSE invalid format
 */
static M3Gbool m3gValidateBackground(RenderContext *ctx, Background *bg)
{
    /* Check that source image and target formats match */
    if (bg != NULL && bg->image != NULL) {
        M3GPixelFormat boundFormat =
            (ctx->target.type == SURFACE_IMAGE)
            ? m3gPixelFormat(((const Image *)ctx->target.handle)->format)
            : ctx->target.format;
        if (ctx->target.type == SURFACE_IMAGE && boundFormat == M3G_RGBA8) {
            return (m3gGetFormat(bg->image) == M3G_RGBA);
        }
        else {
            return (m3gGetFormat(bg->image) == M3G_RGB);
        }
    }

    return M3G_TRUE;
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const ObjectVFTable m3gvf_RenderContext = {
    NULL, /* ApplyAnimation */
    NULL, /* IsCompatible */
    NULL, /* UpdateProperty */
    NULL, /* GetReference */
    NULL, /* find */
    NULL, /* CreateClone */
    m3gDestroyContext
};


/*----------------------------------------------------------------------
 * Public API
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates and initializes a new rendering context
 *
 * \param bufferBits buffer bitmask
 * \param width maximum width of context
 * \param height maximum height of context
 * \param modeBits hint bitmask
 * \param mem pointer to memory block to allocate from
 */
/*@access M3GInterface@*/
/*@access M3GRenderContext@*/
/*@only@*/
M3G_API M3GRenderContext m3gCreateContext(M3GInterface interface)/*@*/
{
    Interface *m3g = (Interface*) interface;
    M3G_VALIDATE_INTERFACE(m3g);
        
    {
        RenderContext *ctx =
            (RenderContext*) m3gAllocZ(m3g, (int) sizeof(RenderContext));
        if (ctx == NULL) {
            return NULL; /* m3gAlloc automatically raises out-of-mem */
        }

		ctx->renderQueue = m3gCreateRenderQueue(m3g);
        if (ctx->renderQueue == NULL) {
            m3gFree(m3g, ctx);
            return NULL;
        }
        ctx->bufferBits = M3G_COLOR_BUFFER_BIT|M3G_DEPTH_BUFFER_BIT;
        ctx->depthNear = 0.0f;
        ctx->depthFar = 1.0f;

        m3gInitObject(&ctx->object, m3g, M3G_CLASS_RENDER_CONTEXT);

		m3gSetAlphaWrite(ctx, M3G_TRUE);

        if (m3gGetColorMaskWorkaround(M3G_INTERFACE(ctx))) {
            ctx->currentColorWrite = M3G_TRUE;
            ctx->currentAlphaWrite = m3gGetAlphaWrite(ctx);
        }
        
		return (M3GRenderContext)ctx;
    }
}

/*!
 * \brief Sets the buffers to use for subsequent rendering
 */
M3G_API M3Gbool m3gSetRenderBuffers(M3GRenderContext hCtx,
                                    M3Gbitmask bufferBits)
{
    RenderContext *ctx = (RenderContext *) hCtx;
    M3G_VALIDATE_OBJECT(ctx);

    if ((bufferBits & ~(M3G_COLOR_BUFFER_BIT|M3G_DEPTH_BUFFER_BIT|M3G_STENCIL_BUFFER_BIT|M3G_MULTISAMPLE_BUFFER_BIT)) != 0) {
        m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_VALUE);
        return M3G_FALSE;
    }
    ctx->bufferBits = bufferBits;
    return M3G_TRUE;
}

/*!
 * \brief Sets the rendering quality hints to use for subsequent
 * rendering
 *
 * \note This may not take effect before the target is released and
 * rebound
 */
M3G_API M3Gbool m3gSetRenderHints(M3GRenderContext hCtx, M3Gbitmask modeBits)
{
    RenderContext *ctx = (RenderContext *) hCtx;
    M3G_VALIDATE_OBJECT(ctx);

    if ((modeBits & ~(M3G_OVERWRITE_BIT|M3G_ANTIALIAS_BIT|M3G_DITHER_BIT|M3G_TRUECOLOR_BIT)) != 0) {
        m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_VALUE);
        return M3G_FALSE;
    }
    
    /* Disable features not supported in the current configuration */

    if (M3G_SUPPORT_ANTIALIASING == M3G_FALSE ||
        !m3gIsAntialiasingSupported(M3G_INTERFACE(ctx))) {
        modeBits &= ~M3G_ANTIALIAS_BIT;
    }
    if (M3G_SUPPORT_DITHERING == M3G_FALSE) {
        modeBits &= ~M3G_DITHER_BIT;
    }
    if (M3G_SUPPORT_TRUE_COLOR == M3G_FALSE) {
        modeBits &= ~M3G_TRUECOLOR_BIT;
    }

    ctx->modeBits = modeBits;
    return M3G_TRUE;
}

M3G_API void m3gBindImageTarget(M3GRenderContext hCtx, M3GImage hImage)
{
    RenderContext *ctx = (RenderContext *) hCtx;
    Image *img = (Image *) hImage;
    M3G_VALIDATE_OBJECT(ctx);
    M3G_VALIDATE_OBJECT(img);

    M3G_LOG1(M3G_LOG_RENDERING, "Binding image target 0x%08X\n",
             (unsigned) img);

    /* Check for image-specific errors */
    
    if ((img->flags & M3G_DYNAMIC) == 0
        || !m3gValidTargetFormat(img->internalFormat)) {
        
        m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_ENUM);
        return;
    }

    /* Do the generic checking and set-up */
    
    if (!m3gBindRenderTarget(ctx,
                             SURFACE_IMAGE,
                             img->width, img->height,
                             img->internalFormat,
                             (M3Gpointer) hImage)) {
        return; /* appropriate error raised automatically */
    }

    /* Set up image-specific parameters */
    
#   if defined(M3G_NGL_CONTEXT_API)
    ctx->target.stride = m3gGetImageStride(img);
    ctx->target.pixels = NULL;
#   endif
    
    m3gAddRef((Object*) img);
}

/*!
 */
M3G_API M3Guint m3gGetUserHandle(M3GRenderContext hCtx)
{
    RenderContext *ctx = (RenderContext *) hCtx;
    M3G_VALIDATE_OBJECT(ctx);

    if (ctx->target.type == SURFACE_MEMORY) {
        return ctx->target.handle;
    }
    return 0;
}

/*!
 */
M3G_API void m3gSetUserData(M3GRenderContext hCtx, M3Guint hData)
{
    RenderContext *ctx = (RenderContext *) hCtx;
    M3G_VALIDATE_OBJECT(ctx);
    ctx->target.userData = hData;
}

/*!
 */
M3G_API M3Guint m3gGetUserData(M3GRenderContext hCtx)
{
    RenderContext *ctx = (RenderContext *) hCtx;
    M3G_VALIDATE_OBJECT(ctx);
    return ctx->target.userData;
}

/*!
 * \brief Clears the current buffer(s)
 */
M3G_API void m3gClear(M3GRenderContext context, M3GBackground hBackground)
{
    RenderContext *ctx = (RenderContext*) context;
    Background *bg = (Background *) hBackground;
    M3G_VALIDATE_OBJECT(ctx);

    M3G_LOG(M3G_LOG_STAGES, "Clearing frame buffer\n");
    
    /* Check errors */
    
    if (ctx->target.type == SURFACE_NONE) {
        m3gRaiseError(M3G_INTERFACE(context), M3G_INVALID_OPERATION);
        return;
    }

    if(m3gValidateBackground(ctx, bg)) {
        m3gClearInternal(ctx, bg);
    }
    else {
        m3gRaiseError(M3G_INTERFACE(bg), M3G_INVALID_VALUE);
    }
}

/*!
 * \brief Release the currently bound color buffer
 *
 * Flushes all rendering and commits the final result to the currently
 * bound target color buffer. Any changes to the target buffer since
 * it was bound may be overwritten.
 */
M3G_API void m3gReleaseTarget(M3GRenderContext context)
{
    RenderContext *ctx = (RenderContext*) context;
    M3G_VALIDATE_OBJECT(ctx);

    M3G_LOG(M3G_LOG_RENDERING, "Releasing target\n");
    
    if (ctx->target.type == SURFACE_NONE) {
        return;
    }
    
    m3gMakeCurrent(ctx);

    if (m3gGetColorMaskWorkaround(M3G_INTERFACE(ctx))) {
        m3gUpdateColorMaskStatus(ctx, M3G_TRUE, M3G_TRUE);
    }

    glFinish();

    /* Update the real target if we rendered into the back buffer */
    
    if (ctx->target.buffered && ctx->backBuffer.contentsValid) {
        m3gUpdateTargetBuffer(ctx);
    }

    /* Invalidate Image targets so that mipmap levels and/or OpenGL
     * texture objects are updated accordingly */
    
    if (ctx->target.type == SURFACE_IMAGE) {
        Image *img = (Image *) ctx->target.handle;
        M3G_VALIDATE_OBJECT(img);
        m3gInvalidateImage(img);
        m3gDeleteRef((Object*) img);
    }

    /* Swap in case we rendered onto a double-buffered surface,
     * release any GL resources that might have been release since the
     * last time we rendered, then release the GL context so we don't
     * hog resources */
#   if !defined(M3G_NGL_CONTEXT_API)
    if (ctx->target.type == SURFACE_WINDOW) {
        m3gSwapBuffers(ctx->target.surface);
    }
#   endif
    m3gCollectGLObjects(M3G_INTERFACE(ctx));
#   if !defined(M3G_NGL_CONTEXT_API)
    m3gMakeGLCurrent(NULL);
    ctx->target.surface = NULL;
#   else
    if (ctx->target.type == SURFACE_MEMORY && ctx->target.pixels == NULL) {
        m3gSignalTargetRelease(M3G_INTERFACE(ctx), ctx->target.handle);
    }
#   endif

    ctx->target.type = SURFACE_NONE;
    ctx->renderQueue->root = NULL;

#   if (M3G_PROFILE_LOG_INTERVAL > 0)
    m3gLogProfileCounters(M3G_INTERFACE(ctx));
#   endif
}

/*!
 * \brief Sets a camera for this context
 */
M3G_API void m3gSetCamera(M3GRenderContext context,
                          M3GCamera hCamera,
                          M3GMatrix *transform)
{
	Matrix m;
    RenderContext *ctx = (RenderContext*) context;
	const Camera *camera = (Camera *)hCamera;

    M3G_VALIDATE_OBJECT(ctx);

    M3G_ASSIGN_REF(ctx->camera, camera);

	if (transform != NULL) {
		if (!m3gMatrixInverse(&m, transform)) {
            m3gRaiseError(M3G_INTERFACE(ctx), M3G_ARITHMETIC_ERROR);
            return;
        }
	}
	else {
		m3gIdentityMatrix(&m);
	}

	m3gGetMatrixColumns(&m, ctx->viewTransform);

    ctx->lastScope = 0;
}

/*!
 * \brief Adds a light to the light array for this context
 */
M3G_API M3Gint m3gAddLight(M3GRenderContext hCtx,
                           M3GLight hLight,
                           const M3GMatrix *transform)
{
    RenderContext *ctx = (RenderContext *)hCtx;
    Light *light = (Light *)hLight;
    M3G_VALIDATE_OBJECT(ctx);

    if (light == NULL) {
        m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_VALUE);
        return -1;
    }
    else {
        LightManager *mgr = &ctx->lightManager;
        M3G_VALIDATE_OBJECT(light);
        ctx->lastScope = 0;
        return m3gInsertLight(mgr, light, transform, M3G_INTERFACE(ctx));
    }
}

/**
 * \brief Sets a light for this context
 */
M3G_API void m3gSetLight(M3GRenderContext context,
                         M3Gint lightIndex,
                         M3GLight hLight,
                         const M3GMatrix *transform)
{
    RenderContext *ctx = (RenderContext*) context;
	Light *light = (Light *)hLight;
    M3G_VALIDATE_OBJECT(ctx);

	/* Check for invalid arguments */
	if (lightIndex < 0 || lightIndex >= m3gLightArraySize(&ctx->lightManager)) {
		m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_INDEX);
        return;
	}

    ctx->lastScope = 0;
    m3gReplaceLight(&ctx->lightManager, lightIndex, light, transform);
}

/*!
 * \brief Removes all lights from this context
 */
M3G_API void m3gClearLights(M3GRenderContext context)
{
    RenderContext *ctx = (RenderContext *)context;
    M3G_VALIDATE_OBJECT(ctx);
    ctx->lastScope = 0;
    m3gClearLights2(&ctx->lightManager);
}

/*!
 * \brief Sets the viewport
 *
 */
M3G_API void m3gSetViewport(M3GRenderContext hCtx,
                            M3Gint x, M3Gint y,
                            M3Gint width, M3Gint height)
{
    RenderContext *ctx = (RenderContext *)hCtx;
    M3G_VALIDATE_OBJECT(ctx);

    /* Note that the error checking here differs from that specified
     * for the Java API; this is to avoid complications when setting
     * from BindTarget where the clip rectangle may be zero.
     * Additional checks are performed in the Java glue code. */
    
    if (width < 0 || height < 0) {
        m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_VALUE);
        return;
    }
    
    width = M3G_MIN(width, M3G_MAX_VIEWPORT_DIMENSION);
    height = M3G_MIN(height, M3G_MAX_VIEWPORT_DIMENSION);
    
    ctx->viewport.x = x;
    ctx->viewport.y = ctx->target.height - (y + height);
    ctx->viewport.width = width;
    ctx->viewport.height = height;
    m3gUpdateScissor(ctx);
}


/*!
 * \brief Gets the viewport
 */
M3G_API void m3gGetViewport(M3GRenderContext hCtx,
                            M3Gint *x, M3Gint *y,
                            M3Gint *width, M3Gint *height)
{
    RenderContext *ctx = (RenderContext *)hCtx;
    M3G_VALIDATE_OBJECT(ctx);

    *x = ctx->viewport.x;
    *y = ctx->target.height - (ctx->viewport.y + ctx->viewport.height);
    *width = ctx->viewport.width;
    *height = ctx->viewport.height;
}

/*!
 * \brief Sets the scissor rectangle
 */
M3G_API void m3gSetClipRect(M3GRenderContext hCtx,
                            M3Gint x, M3Gint y,
                            M3Gint width, M3Gint height)
{
    RenderContext *ctx = (RenderContext *)hCtx;
    M3G_VALIDATE_OBJECT(ctx);

    if (width < 0 || height < 0) {
        m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_VALUE);
        return;
    }
    ctx->clip.x0 = x;
    ctx->clip.y0 = ctx->target.height - (y + height);
    ctx->clip.x1 = x + width;
    ctx->clip.y1 = ctx->clip.y0 + height;
    m3gValidateClipRect(ctx);
    m3gUpdateScissor(ctx);
}

/*!
 * \brief Sets the physical display area
 *
 * The display are is normally set to the full rendering targte size
 * in m3gBindTarget, but this function allows overriding the default
 * setting.
 *
 * Any pixels outside of the display area can be discarded for
 * performance.  The origin is assumed to be in the top-left corner of
 * the rendering target.
 */
M3G_API void m3gSetDisplayArea(M3GRenderContext hCtx,
                               M3Gint width, M3Gint height)
{
    RenderContext *ctx = (RenderContext*) hCtx;
    M3G_VALIDATE_OBJECT(ctx);
    
    ctx->display.width = M3G_MIN(width, ctx->target.width);
    ctx->display.height = M3G_MIN(height, ctx->target.height);
}

/*!
 * \brief Sets depth range
 * 
 */
M3G_API void m3gSetDepthRange(M3GRenderContext hCtx,
                              M3Gfloat depthNear, M3Gfloat depthFar)
{
    RenderContext *ctx = (RenderContext *)hCtx;
    M3G_VALIDATE_OBJECT(ctx);
	
	if (depthNear < 0 || depthNear > 1.0f || depthFar < 0 || depthFar > 1.0f) {
		m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_VALUE);
        return;
    }

	ctx->depthNear = depthNear;
	ctx->depthFar = depthFar;
}

/*!
 * \brief Gets depth range
 * 
 */
M3G_API void m3gGetDepthRange(M3GRenderContext hCtx,
                              M3Gfloat *depthNear, M3Gfloat *depthFar)
{
    RenderContext *ctx = (RenderContext *)hCtx;
    M3G_VALIDATE_OBJECT(ctx);
	
	*depthNear = ctx->depthNear;
	*depthFar= ctx->depthFar;
}

/*!
 * \brief Gets current view transform
 * 
 */

M3G_API void m3gGetViewTransform(M3GRenderContext hCtx,
                                 M3GMatrix *transform)
{
    RenderContext *ctx = (RenderContext *)hCtx;
    M3G_VALIDATE_OBJECT(ctx);
    m3gSetMatrixColumns(transform, ctx->viewTransform);
    m3gInvertMatrix(transform); /*lint !e534 always invertible */
}

/*!
 * \brief Gets current Camera
 * 
 */

M3G_API M3GCamera m3gGetCamera(M3GRenderContext hCtx)
{
    RenderContext *ctx = (RenderContext *)hCtx;
    M3G_VALIDATE_OBJECT(ctx);
    return (M3GCamera) ctx->camera;
}

/*!
 * \brief Gets light transform of given light
 * 
 */

M3G_API M3GLight m3gGetLightTransform (M3GRenderContext hCtx,
                                       M3Gint lightIndex, M3GMatrix *transform)
{
    RenderContext *ctx = (RenderContext *)hCtx;
    M3G_VALIDATE_OBJECT(ctx);
    return m3gGetLightTransformInternal(&ctx->lightManager, lightIndex, transform);
}

/*!
 * \brief Gets light count
 * 
 */

M3G_API M3Gsizei m3gGetLightCount (M3GRenderContext hCtx)
{
    RenderContext *ctx = (RenderContext *)hCtx;
    M3G_VALIDATE_OBJECT(ctx);
    return m3gLightArraySize(&ctx->lightManager);
}

/*!
 * \brief Renders a world
 *
 */
M3G_API void m3gRenderWorld(M3GRenderContext context, M3GWorld hWorld)
{
	Camera *camera;
    RenderContext *ctx = (RenderContext*) context;
	World *world = (World *) hWorld;

    M3G_LOG1(M3G_LOG_STAGES, "Rendering World 0x%08X\n", (unsigned) world);
    
    M3G_VALIDATE_OBJECT(ctx);
    M3G_VALIDATE_OBJECT(world);

	camera = m3gGetActiveCamera(world);

    /* Check for errors */
    
    if (ctx->target.type == SURFACE_NONE) {
        m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_OPERATION);
        return;
    }
    
	if (camera == NULL ||
        !m3gIsChildOf((Node *)world, (Node *)camera)) {
		m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_OPERATION);
        return;
	}

    /* Exit if the camera will show nothing (zero view volume) */
    
    if (!m3gValidProjection(camera)) {
        return;
    }

    /* Override the currently set viewing transformation with identity
     * (will fix this before we return) */
    
	m3gSetCamera(ctx, camera, NULL);

    if (m3gValidateBackground(ctx, world->background)) {
    	m3gClearInternal(ctx, world->background);
    }
    else {
        m3gRaiseError(M3G_INTERFACE(world), M3G_INVALID_OPERATION);
        return;
    }

    /* All clear for rendering */
    
    M3G_LOG(M3G_LOG_RENDERING, "Rendering: start\n");    
    M3G_ASSERT(ctx->renderQueue->root == NULL);
    M3G_BEGIN_PROFILE(M3G_INTERFACE(ctx), M3G_PROFILE_VALIDATE);
    
    if (m3gValidateNode((Node*) world, NODE_RENDER_BIT, camera->node.scope)) {
        M3Gbool setup;
        SetupRenderState s;
        M3G_END_PROFILE(M3G_INTERFACE(ctx), M3G_PROFILE_VALIDATE);

        /* We start the traversal from the camera, so set the initial
         * camera-space transformation to identity */
        
        m3gIdentityMatrix(&s.toCamera);
        s.cullMask = CULLMASK_ALL;
        
        m3gClearLights2(&ctx->lightManager);
        
        ctx->renderQueue->root = (Node *)world;
        ctx->renderQueue->scope = camera->node.scope;
        ctx->renderQueue->lightManager = &ctx->lightManager;
        ctx->renderQueue->camera = camera;

        M3G_BEGIN_PROFILE(M3G_INTERFACE(ctx), M3G_PROFILE_SETUP);
        
        setup = M3G_VFUNC(Node, camera, setupRender)((Node *) camera,
                                                     NULL,
                                                     &s,
                                                     ctx->renderQueue);
        M3G_END_PROFILE(M3G_INTERFACE(ctx), M3G_PROFILE_SETUP);
        M3G_LOG(M3G_LOG_RENDERING, "Rendering: commit\n");
        M3G_BEGIN_PROFILE(M3G_INTERFACE(ctx), M3G_PROFILE_COMMIT);

        if (setup) {
            m3gInitRender(ctx, RENDER_WORLD);
            m3gLockFrameBuffer(ctx);
            m3gCommit(ctx->renderQueue, ctx);
            m3gReleaseFrameBuffer(ctx);
        }
        
        M3G_END_PROFILE(M3G_INTERFACE(ctx), M3G_PROFILE_COMMIT);

        /* Fix light and camera transformations to be relative to world
         * space on exit */
    
        if (setup) {
            Matrix m;
            if (m3gGetTransformTo((Node*) world, (Node*) camera, &m)) {
                m3gGetMatrixColumns(&m, ctx->viewTransform);
                if (m3gInvertMatrix(&m)) {
                    m3gTransformLights(&ctx->lightManager, &m);
                }
                else {
                    M3G_ASSERT(M3G_FALSE);
                }
            }
            else {
                M3G_ASSERT(M3G_FALSE);
            }
        }
    }
    
	m3gClearRenderQueue(ctx->renderQueue);
    M3G_LOG(M3G_LOG_RENDERING, "Rendering: end\n");
}

/*!
 * \brief Renders a node or subtree
 */
M3G_API void m3gRenderNode(M3GRenderContext context,
                           M3GNode hNode,
                           const M3GMatrix *transform)
{
    RenderContext *ctx = (RenderContext*) context;
    Node *node = (Node *) hNode;

    M3G_LOG1(M3G_LOG_STAGES, "Rendering Node 0x%08X\n", (unsigned) node);
    
    M3G_VALIDATE_OBJECT(ctx);
    M3G_VALIDATE_OBJECT(node);

    /* Check for errors */
    
    if (node == NULL) {
        m3gRaiseError(M3G_INTERFACE(ctx), M3G_NULL_POINTER);
        return;
    }

    if (ctx->target.type == SURFACE_NONE || ctx->camera == NULL) {
        m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_OPERATION);
        return;
    }

    /* Exit if the camera will show nothing (zero view volume) */
    
    if (!m3gValidProjection(ctx->camera)) {
        return;
    }

    /* All clear, draw away */

    M3G_LOG(M3G_LOG_RENDERING, "Rendering: start\n");
	M3G_ASSERT(ctx->renderQueue->root == NULL);
    M3G_BEGIN_PROFILE(M3G_INTERFACE(ctx), M3G_PROFILE_VALIDATE);
    
    if (m3gValidateNode(node, NODE_RENDER_BIT, ctx->camera->node.scope)) {
        M3Gbool setup;
        SetupRenderState s;
        M3G_END_PROFILE(M3G_INTERFACE(ctx), M3G_PROFILE_VALIDATE);
        
        s.cullMask = CULLMASK_ALL;

        /* We start the traversal from world space, so preload the
         * current camera-space transformation to get camera-space
         * meshes and correct view frustum culling */
        
        m3gSetMatrixColumns(&s.toCamera, ctx->viewTransform);
        if (transform) {
            m3gMulMatrix(&s.toCamera, transform);
        }
        ctx->renderQueue->root = (Node *) node;
        ctx->renderQueue->scope = ctx->camera->node.scope;
        ctx->renderQueue->lightManager = NULL;
        ctx->renderQueue->camera = ctx->camera;
        
        M3G_BEGIN_PROFILE(M3G_INTERFACE(ctx), M3G_PROFILE_SETUP);
        
        setup = M3G_VFUNC(Node, node, setupRender)(node,
                                                   NULL,
                                                   &s,
                                                   ctx->renderQueue);
        M3G_END_PROFILE(M3G_INTERFACE(ctx), M3G_PROFILE_SETUP);
        M3G_LOG(M3G_LOG_RENDERING, "Rendering: commit\n");
        M3G_BEGIN_PROFILE(M3G_INTERFACE(ctx), M3G_PROFILE_COMMIT);
        
        if (setup) {
            m3gInitRender(ctx, RENDER_NODES);
            m3gLockFrameBuffer(ctx);
    		m3gCommit(ctx->renderQueue, ctx);
            m3gReleaseFrameBuffer(ctx);
        }
        
        M3G_END_PROFILE(M3G_INTERFACE(ctx), M3G_PROFILE_COMMIT);
	}

	m3gClearRenderQueue(ctx->renderQueue);
    
    M3G_LOG(M3G_LOG_RENDERING, "Rendering: end\n");
}

/*!
 * \brief Render a set of primitives
 * 
 */
M3G_API void m3gRender(M3GRenderContext context,
                       M3GVertexBuffer hVertices,
                       M3GIndexBuffer hIndices,
                       M3GAppearance hAppearance,
                       const M3GMatrix *transformMatrix,
                       M3Gfloat alphaFactor,
                       M3Gint scope)
{
    RenderContext *ctx = (RenderContext *) context;
    const VertexBuffer *vb = (const VertexBuffer *) hVertices;
    const IndexBuffer *ib = (const IndexBuffer *) hIndices;
    const Appearance *app = (const Appearance *) hAppearance;
    M3G_VALIDATE_OBJECT(ctx);

    M3G_LOG1(M3G_LOG_STAGES, "Rendering vertex buffer 0x%08X\n",
             (unsigned) vb);
    
    /* Check validity of input */
    
    if (ctx->target.type == SURFACE_NONE || ctx->camera == NULL) {
        m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_OPERATION);
        return;
    }
    
    /* Quick exit if out of scope or zero view volume */

    if ((scope & ctx->camera->node.scope) == 0
        || !m3gValidProjection(ctx->camera)) {
        return;
    }

    if (vb == NULL || ib == NULL || app == NULL) {
        m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_OBJECT);
        return;
    }

    if (!m3gValidateVertexBuffer(vb, app, m3gGetMaxIndex(ib))) {
        m3gRaiseError(M3G_INTERFACE(ctx), M3G_INVALID_OPERATION);
        return;
    }

    /* Everything checks out, so draw */

    M3G_LOG(M3G_LOG_RENDERING, "Rendering: start immediate\n");
    
    m3gInitRender(ctx, RENDER_IMMEDIATE);
    m3gLockFrameBuffer(ctx);
    m3gDrawMesh(ctx,
                vb, ib, app,
                transformMatrix,
                m3gRoundToInt(
                    m3gMul(alphaFactor,
                           (M3Gfloat)(1 << NODE_ALPHA_FACTOR_BITS))),
                scope);
    m3gReleaseFrameBuffer(ctx);
    
    M3G_LOG(M3G_LOG_RENDERING, "Rendering: end immediate\n");
}

