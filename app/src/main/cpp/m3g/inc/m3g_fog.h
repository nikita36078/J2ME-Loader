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
* Description: Fog interface
*
*/

#ifndef __M3G_FOG_H__
#define __M3G_FOG_H__

/*!
 * \internal
 * \file
 * \brief Fog interface
 */

#include "m3g_object.h"
#include "m3g_gl.h"

struct M3GFogImpl
{
	Object object;

	M3Gfloat density;
	M3Gfloat start;
	M3Gfloat end;
	M3Gint mode;

    M3Guint color;
};

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static void m3gApplyFog(const Fog *self);
static void m3gApplySpriteFog(const Fog *self, M3Gfloat eyeZ, M3Gfloat finalZ);

#endif /*__M3G_FOG_H__*/
