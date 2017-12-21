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
* Description: Background interface
*
*/

#ifndef __M3G_BACKGROUND_H__
#define __M3G_BACKGROUND_H__

/*!
 * \internal
 * \file
 * \brief Background interface
 */

#include "m3g_object.h"
#include "m3g_gl.h"

struct M3GBackgroundImpl
{
	Object object;
	
	Image *image;
	M3Gint modeX;
	M3Gint modeY;
    Rect crop;
    
	M3Guint color;

	M3Gbool colorClearEnable;
	M3Gbool depthClearEnable;
};

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static void m3gApplyBackground(	RenderContext *ctx,
								Background *background);

#endif /*__M3G_BACKGROUND_H__*/

