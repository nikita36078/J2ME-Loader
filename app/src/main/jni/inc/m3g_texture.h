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
* Description: Texture2D interface
*
*/


/*!
 * \internal
 * \file
 * \brief Texture2D interface
 */

#ifndef __M3G_TEXTURE_H__
#define __M3G_TEXTURE_H__

#include "m3g_defs.h"

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static void m3gDisableTextures(void);
static void m3gBindTexture(Texture *texture);
static void m3gReleaseTexture(Texture *texture);
#if defined(M3G_NGL_TEXTURE_API)
static M3Gbool m3gValidateTextureMipmapping(Texture *texture);
#endif

#endif /*__M3G_TEXTURE_H__*/
