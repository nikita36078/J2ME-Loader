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
* Description: Sprite interface
*
*/

#ifndef __M3G_SPRITE_H__
#define __M3G_SPRITE_H__

/*!
 * \internal
 * \file
 * \brief Sprite interface
 */

#include "m3g_gl.h"
#include "m3g_node.h"

struct M3GSpriteImpl
{
    Node node;

    Appearance *appearance;
    Image *image;

    M3Gint  flip; /*!< \internal \brief Sprite x and y flip flags */
    M3Gint  width;
    M3Gint  height;
    M3Gushort totalAlphaFactor;
    Rect crop;
    M3Gbool scaled;
};

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static M3Gbool m3gInitSprite(Interface *m3g,
                             Sprite *sprite,
                             M3Gbool scaled,
                             Appearance *appearance,
                             Image *image);

#endif /*__M3G_SPRITE_H__*/
