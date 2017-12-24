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
* Description: MorphingMesh interface
*
*/

#ifndef __M3G_MORPHINGMESH_H__
#define __M3G_MORPHINGMESH_H__

/*!
 * \internal
 * \file
 * \brief MorphingMesh interface
 */

#include "m3g_gl.h"
#include "m3g_mesh.h"
/*
#include "m3g_appearance.h"
#include "m3g_indexbuffer.h"
#include "m3g_vertexbuffer.h"
*/

struct M3GMorphingMeshImpl
{
	Mesh mesh;
    M3Gfloat *floatWeights;
	M3Gint *weights;
	M3Gint sumWeights;
	VertexBuffer **targets;
	VertexBuffer *base;
    VertexBuffer *morphed;
	M3Gint numTargets;
	M3Gbool dirtyState; /*!< \internal \brief state of morphing*/

    /*! \internal \brief base vertex buffer configuration*/
    M3Gbitmask cloneArrayMask;

    AABB bbox;
};

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

#endif /*__M3G_MORPHINGMESH_H__*/
