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
* Description: CompositingMode interface
*
*/

#ifndef __M3G_COMPOSITINGMODE_H__
#define __M3G_COMPOSITINGMODE_H__

/*!
 * \internal
 * \file
 * \brief CompositingMode interface
 */

#include "m3g_object.h"
#include "m3g_gl.h"
#include "m3g_rendercontext.h"

/*!
 * \internal
 * \brief CompositingMode object
 */
struct M3GCompositingModeImpl
{
    Object object;

    M3Gint blendingMode;
    GLfloat alphaThreshold;
    GLboolean depthTest;
    GLboolean depthWrite;
    GLboolean colorWrite;
    GLboolean alphaWrite;
    GLfloat factor;
    GLfloat units;
};

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static void m3gApplyCompositingMode(CompositingMode *compositingMode, RenderContext *ctx);
static void m3gApplyDefaultCompositingMode(M3Gbool alphaWrite);

#endif /*__M3G_COMPOSITINGMODE_H__*/
