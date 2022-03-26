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
* Description: Appearance interface
*
*/

#ifndef __M3G_APPEARANCE_H__
#define __M3G_APPEARANCE_H__

/*!
 * \internal
 * \file
 * \brief Appearance interface
 */

#include "m3g_object.h"
#include "m3g_compositingmode.h"
#include "m3g_polygonmode.h"
#include "m3g_material.h"
#include "m3g_texture.h"
#include "m3g_fog.h"
#include "m3g_rendercontext.h"

/* Limits for user settable layer */
#define M3G_APPEARANCE_MIN_LAYER      -63
#define M3G_APPEARANCE_MAX_LAYER       63
#define M3G_APPEARANCE_HARD_SORT_BITS   8

typedef /*@dependent@*//*@null@*/ Texture *TexturePtr;

/*!
 * \internal
 * \brief Appearance object
 */
struct M3GAppearanceImpl
{
    Object object;

    /*@dependent@*//*@null@*/
    Material *material;

    /*@dependent@*//*@null@*/
    CompositingMode *compositingMode;

    /*@dependent@*//*@null@*/
    PolygonMode *polygonMode;

    /*@dependent@*//*@null@*/
    Fog *fog;

    TexturePtr texture[M3G_NUM_TEXTURE_UNITS];

    M3Guint sortKey;
    
    M3Gshort layer;
    M3Gushort vertexMask;
};

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static void m3gApplyAppearance(const Appearance *appearance,
							   RenderContext *ctx,
                               M3Gint alphaFactor);

/*!
 * \internal
 * \brief Get vertex mask
 *
 * \param app Appearance object
 * \return vertex mask
 */
static M3G_INLINE M3Gbitmask m3gGetVertexMask(const Appearance *app)
{
    return app->vertexMask;
}

/*!
 * \internal
 * \brief Shortcut getter for color mask
 */
static M3G_INLINE M3Gbool m3gColorMask(const Appearance *app)
{
    if (app) {
        const CompositingMode *cm = app->compositingMode;
        return !cm || cm->colorWrite;
    }
    return M3G_FALSE;
}

/*!
 * \internal
 * \brief Shortcut getter for alpha mask
 */
static M3G_INLINE M3Gbool m3gAlphaMask(const Appearance *app)
{
    if (app) {
        const CompositingMode *cm = app->compositingMode;
        return !cm || cm->alphaWrite;
    }
    return M3G_FALSE;
}

/*!
 * \internal
 * \brief Get sortkey for render queue
 *
 * Sort key is a combination of user settable layer and
 * blending mode. Blended objects are always drawn last.
 *
 * \param appearance    Appearance object
 * \return              sort key
 */
static M3G_INLINE M3Guint m3gGetAppearanceSortKey(const Appearance *appearance)
{
    if (appearance) {
        M3Guint key = appearance->sortKey;

        /* The blending state bit is set dynamically, as it may change
         * without changing the appearance (we have no signaling from
         * CompositingMode for that) */
        
        if (appearance->compositingMode != NULL
            && appearance->compositingMode->blendingMode != M3G_REPLACE) {
            key |= (1u << (32 - M3G_APPEARANCE_HARD_SORT_BITS));
        }

        if (m3gGetColorMaskWorkaround(M3G_INTERFACE(appearance))) {
            /* Override the top 2 bits of the sorting key so that ColorMask
             * changes are minimized */
            if (appearance) {
                key &= ~(0x03 << 22);
                key |= (((M3Guint) m3gColorMask(appearance)) & 1) << 23;
                key |= (((M3Guint) m3gAlphaMask(appearance)) & 1) << 22;
            }
        }

        return key;
    } 

    return M3G_FALSE;
}

/*!
 * \internal
 * \brief Release the textures bound for this appearance
 */
static void m3gReleaseTextures(const Appearance *appearance);

#endif /*__M3G_APPEARANCE_H__*/
