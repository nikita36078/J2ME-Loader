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
* Description: Light interface
*
*/

#ifndef __M3G_LIGHT_H__
#define __M3G_LIGHT_H__

/*!
 * \internal
 * \file
 * \brief Light interface
 */

#include "m3g_gl.h"
#include "m3g_node.h"

/*!
 * \internal
 * \brief Light source node instance data
 */
struct M3GLightImpl
{
	Node node;

	M3Gfloat constantAttenuation;
	M3Gfloat linearAttenuation;
	M3Gfloat quadraticAttenuation;
	
	M3Gfloat intensity;
	M3Guint color;
	
	M3Gint mode;
	M3Gfloat spotAngle;
	M3Gfloat spotExponent;
};
    
/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static void m3gInitLight(Interface *m3g, Light *light);
static void m3gApplyLight(const Light *self, GLenum glLight, const Vec4 *pos, const Vec4 *spotDir);

#endif /*__M3G_LIGHT_H__*/
