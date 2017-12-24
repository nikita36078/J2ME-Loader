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
* Description: SkinnedMesh interface
*
*/

#ifndef __M3G_SKINNEDMESH_H__
#define __M3G_SKINNEDMESH_H__

/*!
 * \internal
 * \file
 * \brief SkinnedMesh interface
 */

#include "m3g_mesh.h"
#include "m3g_array.h"

typedef struct BoneRecord Bone;

struct M3GSkinnedMeshImpl
{
	Mesh mesh;
    
    Group *skeleton;
    PointerArray bones;

    M3Gint weightedVertexCount, bonesPerVertex;
    
    M3Gubyte *boneIndices[M3G_MAX_VERTEX_TRANSFORMS];
    M3Gubyte *boneWeights[M3G_MAX_VERTEX_TRANSFORMS];
    M3Gubyte *weightShifts;
    M3Gubyte *normalizedWeights[M3G_MAX_VERTEX_TRANSFORMS];

    /* Scale and bias transformation for morphing */    
    M3Gshort scaleMatrix[9], biasVector[3];
    M3Gshort scaleExp, biasExp, scaleBiasExp;
    
    M3Gshort maxExp; /* total maximum exponent */

    /*! \internal \brief internal morphed vertex buffer */
    VertexBuffer *morphedVB;
    
    M3Gint vbTimestamp;
    M3Gshort posShift, normalShift;

    AABB bbox;

    M3Gbool weightsDirty;
};

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

#endif /*__M3G_SKINNEDMESH_H__*/
