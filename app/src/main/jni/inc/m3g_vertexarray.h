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
* Description: VertexArray interface
*
*/

#ifndef __M3G_VERTEXARRAY_H__
#define __M3G_VERTEXARRAY_H__

/*!
 * \internal
 * \file
 * \brief VertexArray interface
 */

#include "m3g_object.h"
#include "m3g_gl.h"

/*!
 * \internal
 * \brief \c VertexArray object instance
 */
struct M3GVertexArrayImpl
{
    Object object;
    
    M3Gsizei vertexCount;
    M3Gint mapCount;
    M3Guint numLocks;
    
    GLint elementSize;
    GLenum elementType;
    GLsizei stride;

    M3GMemObject data;
    M3Gshort rangeMin, rangeMax;

    /*!
     * \internal
     * \brief Cached color array(s?) with premultiplied alpha
     */
    M3GMemObject cachedColors;

    /*!
     * \internal
     * \brief Alpha factor for currently stored scaled colors
     *
     * anything below zero is considered a dirty value
     */
    M3Gint cachedAlphaFactor;
    M3Gint timestamp;
};

/* If this assert fails, check the compiler padding settings */
M3G_CT_ASSERT(sizeof(VertexArray) == sizeof(Object) + 44);

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static void m3gLockColorArray   (const VertexArray *array, M3Gint alphaFactor);
static void m3gLockNormalArray  (const VertexArray *array);
static void m3gLockTexCoordArray(const VertexArray *array);
static void m3gLockVertexArray  (const VertexArray *array);
static void m3gUnlockArray      (const VertexArray *array);

static M3Gbool m3gCreateAlphaColorCache(VertexArray *array);
    
static VertexArray *m3gCloneVertexArray(const VertexArray *array);
static M3Gint m3gGetArrayVertexCount (const VertexArray *array);
static M3Gbool m3gIsCompatible  (const VertexArray *array, const VertexArray *other);
static M3Gint m3gGetArrayTimestamp(const VertexArray *array);
static void m3gGetArrayBoundingBox(const VertexArray *array, M3Gshort *boundingBox);
static M3Gbool m3gGetCoordinates(VertexArray *va,
                                 M3Gint elementCount,
                                 M3Gint idx,
                                 M3Gfloat *v);

static void m3gGetArrayValueRange(const VertexArray *array,
                                  M3Gint *minValue, M3Gint *maxValue);

#endif /*__M3G_VERTEXARRAY_H__*/
