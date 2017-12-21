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
* Description: VertexBuffer interface
*
*/

#ifndef __M3G_VERTEXBUFFER_H__
#define __M3G_VERTEXBUFFER_H__

/*!
 * \internal
 * \file
 * \brief VertexBuffer interface
 */

#include "m3g_object.h"
#include "m3g_vertexarray.h"

/*!
 * \internal
 * \brief \c VertexBuffer object instance
 */
struct M3GVertexBufferImpl
{
    Object object;

    /*@dependent@*/
    VertexArray *colors;
    /*@dependent@*/
    VertexArray *normals;
    VertexArray *texCoords[M3G_NUM_TEXTURE_UNITS];
    /*@dependent@*/
    VertexArray *vertices;

    GLfloat texCoordScale[M3G_NUM_TEXTURE_UNITS];
    GLfloat texCoordBias[M3G_NUM_TEXTURE_UNITS][3];

    GLfloat vertexScale;
    GLfloat vertexBias[3];

    /*! \internal \brief Default vertex color */
    struct { GLubyte r, g, b, a; } defaultColor;
    
    M3Gbool locked;
    M3Gint vertexCount;
    M3Gint arrayCount;
    M3Gbitmask arrayMask;
    M3Gint timestamp;
    M3Gint verticesTimestamp;
    
    AABB bbox;
};

/*! \brief vertex component mask bits */
#define M3G_POSITION_BIT        0x01u
#define M3G_COLOR_BIT           0x02u
#define M3G_NORMAL_BIT          0x04u

#define M3G_TEXCOORD0_BIT       0x10u
#define M3G_TEXCOORD1_BIT       0x20u
#define M3G_TEXCOORD2_BIT       0x40u
#define M3G_TEXCOORD3_BIT       0x80u

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static void m3gApplyScaleAndBias(const VertexBuffer *buffer);
static void m3gLockVertexBuffer(const VertexBuffer *buffer, M3Gint alphaFactor);
static void m3gReleaseVertexBuffer(const VertexBuffer *buffer);

static M3Gbool m3gGetVertex(const VertexBuffer *buffer, M3Gint idx, Vec3 *v);
static M3Gbool m3gGetNormal(const VertexBuffer *buffer, M3Gint idx, Vec3 *v);
static M3Gbool m3gGetTexCoord(const VertexBuffer *buffer, M3Gint idx, int unit, Vec3 *v);

static M3Gint m3gGetTimestamp(const VertexBuffer *buffer);
static M3Gint m3gGetNumVertices(const VertexBuffer *buffer);
static M3Gbitmask m3gGetArrayMask(const VertexBuffer *buffer);

static M3Gbool m3gValidateVertexBuffer(const VertexBuffer *vb,
                                       const Appearance *app,
                                       M3Gsizei maxIndex);
static void m3gGetBoundingBox(VertexBuffer *buffer, AABB *boundingBox);

static M3Gbool m3gMakeModifiedVertexBuffer(VertexBuffer *buffer,
                                           const VertexBuffer *srcBuffer,
                                           M3Gbitmask arrayMask,
                                           M3Gbool createArrays);


/* -------- Inline functions -------- */

/*!
 * \internal
 * \brief Queries whether a vertex buffer can be (safely) bound with
 * memory locking in effect
 */
static M3G_INLINE M3Gbool m3gValidateAlphaCache(const VertexBuffer *buffer) 
{
    const VertexArray *colArray = buffer->colors;
    return colArray ? (colArray->cachedColors != 0) : M3G_TRUE;
}

/*!
 * \internal
 * \brief Type-safe helper function
 */
static M3G_INLINE void m3gDeleteVertexBuffer(VertexBuffer *buffer)
{
    m3gDeleteObject((Object *) buffer);
}

/*!
 * \internal
 * \brief Gets number of vertices in this buffer.
 */
static M3G_INLINE M3Gint m3gGetNumVertices(const VertexBuffer *buffer)
{
    return buffer->vertexCount;
}

/*!
 * \internal
 * \brief Returns the current array mask for this buffer
 */
static M3G_INLINE M3Gbitmask m3gGetArrayMask(const VertexBuffer *buffer)
{
    return buffer->arrayMask;
}

#endif /*__M3G_VERTEXBUFFER_H__*/
