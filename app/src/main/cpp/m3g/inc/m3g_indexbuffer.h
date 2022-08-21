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
* Description: IndexBuffer interface
*
*/

#ifndef __M3G_INDEXBUFFER_H__
#define __M3G_INDEXBUFFER_H__

/*!
 * \internal
 * \file
 * \brief IndexBuffer interface
 */

#include "m3g_object.h"
#include "m3g_gl.h"

/*!
 * \internal
 * \brief Index buffer structure
 *
 * Contrary to JSR-184, a single Index Buffer type is used to hold all
 * the different primitive types.
 */
struct M3GIndexBufferImpl
{
    Object object;
    
    M3Gint maxIndex;
    GLsizei indexCount;
    GLenum glPrimitive;
    GLenum glType;
    M3Gsizei stripCount;
    M3Gushort *lengths;
    void *indices;
};

/*----------------------------------------------------------------------
 * M3G internal API
 *--------------------------------------------------------------------*/

static void m3gSendIndexBuffer(const IndexBuffer *buf);
static M3Gbool m3gGetIndices(const IndexBuffer *buf, M3Gint triangle, M3Gint *indices);
static M3Gint m3gGetMaxIndex(const IndexBuffer *buf);

#endif /*__M3G_INDEXBUFFER_H__*/
