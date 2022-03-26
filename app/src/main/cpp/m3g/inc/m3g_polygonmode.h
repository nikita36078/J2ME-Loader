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
* Description: PolygonMode interface
*
*/

#ifndef __M3G_POLYGONMODE_H__
#define __M3G_POLYGONMODE_H__

/*!
 * \internal
 * \file
 * \brief PolygonMode interface
 */

#include "m3g_object.h"
#include "m3g_gl.h"

/*!
 * \internal
 * \brief PolygonMode buffer object
 */
struct M3GPolygonModeImpl
{
    Object object;

    int cullingMode;
    int windingMode;
    int shadingMode;
    
    GLboolean enableLocalCameraLighting;
    GLboolean enablePerspectiveCorrection;
    GLboolean enableTwoSidedLighting;
};

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static void m3gApplyPolygonMode(PolygonMode *polygonMode);
static void m3gApplyDefaultPolygonMode(void);

#endif /*__M3G_POLYGONMODE_H__*/
