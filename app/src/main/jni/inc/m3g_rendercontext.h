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
* Description: Rendering context interface
*
*/

#ifndef __M3G_RENDERCONTEXT_H__
#define __M3G_RENDERCONTEXT_H__

/*!
 * \internal
 * \file
 * \brief Rendering context data structure definition and
 * function declarations
 */

static void m3gDrawMesh(RenderContext *ctx,
                        const VertexBuffer *vb,
                        const IndexBuffer *ib,
                        const Appearance *app,
                        const M3GMatrix *modelTransform,
                        M3Gint alphaFactor,
                        M3Gint scope);

static void m3gPushScreenSpace(RenderContext *ctx, M3Gbool realPixels);
static void m3gPopSpace(RenderContext *ctx);

static const Camera *m3gGetCurrentCamera(const RenderContext *ctx);

static M3Gbool m3gIsAccelerated(const RenderContext *ctx);

#if !defined(M3G_NGL_CONTEXT_API)
static void m3gBlitFrameBufferPixels(RenderContext *ctx,
                                     M3Gint xOffset, M3Gint yOffset,
                                     M3Gint width, M3Gint height,
                                     M3GPixelFormat internalFormat,
                                     M3Gsizei stride,
                                     const M3Gubyte *pixels);
#endif

static void m3gUpdateColorMaskStatus(RenderContext *ctx,
                                     M3Gbool newColorWrite,
                                     M3Gbool newAlphaWrite);

#endif /*__M3G_RENDERCONTEXT_H__*/
