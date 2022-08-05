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
* Description: Light manager interface
*
*/

#ifndef __M3G_LIGHTMANAGER_H__
#define __M3G_LIGHTMANAGER_H__

/*!
 * \internal
 * \file
 * \brief Light manager class for managing and selecting from a
 * dynamic pool of lights in RenderContext
 *
 * A LightManager is a component of a RenderContext, and can not exist
 * as a standalone object.
 * 
 */

#include "m3g_array.h"

/*!
 * \internal
 * \brief Light manager structure
 */
typedef struct
{
    PointerArray lights;
    M3Gsizei numActive;
} LightManager;

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static void     m3gClearLights2         (LightManager *mgr);
static void     m3gDestroyLightManager  (LightManager *mgr, Interface *m3g);
static M3Gint   m3gInsertLight          (LightManager *mgr, Light *light, const Matrix *tf, Interface *m3g);
static M3Gsizei m3gLightArraySize       (const LightManager *mgr);
static void     m3gReplaceLight         (LightManager *mgr, M3Gint idx, Light *light, const Matrix *tf);
static void     m3gSelectGLLights       (const LightManager *mgr,
                                         M3Gsizei maxNum,
                                         M3Guint scope,
                                         M3Gfloat x, M3Gfloat y, M3Gfloat z);
static void m3gTransformLights          (LightManager *mgr, const Matrix *mtx);
static Light *m3gGetLightTransformInternal(const LightManager *mgr,
                                           M3Gint idx, M3GMatrix *transform);
#endif /*__M3G_LIGHTMANAGER_H__*/
