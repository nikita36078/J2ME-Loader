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
* Description: Material interface
*
*/

#ifndef __M3G_MATERIAL_H__
#define __M3G_MATERIAL_H__

/*!
 * \internal
 * \file
 * \brief Material interface
 */

#include "m3g_object.h"
#include "m3g_gl.h"

/*!
 * \internal
 * \brief Material object
 */
struct M3GMaterialImpl
{
    Object object;

    GLboolean vertexColorTracking;
    M3Guint ambientColor;
    M3Guint diffuseColor;
    M3Guint emissiveColor;
    M3Guint specularColor;
    GLfloat shininess;
};

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static void m3gApplyMaterial(Material *material, M3Gint alphaFactor);
static void m3gApplyDefaultMaterial(void);

#endif /*__M3G_MATERIAL_H__*/
