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
* Description: Mesh interface
*
*/

#ifndef __M3G_MESH_H__
#define __M3G_MESH_H__

/*!
 * \internal
 * \file
 * \brief Mesh interface
 */

#include "m3g_gl.h"
#include "m3g_node.h"
#include "m3g_appearance.h"
#include "m3g_indexbuffer.h"
#include "m3g_vertexbuffer.h"

struct M3GMeshImpl
{
	Node node;
    
	VertexBuffer *vertexBuffer;
	Appearance **appearances;
	IndexBuffer **indexBuffers;

    M3Gint vbTimestamp;
    
	M3Gushort trianglePatchCount;
    M3Gushort totalAlphaFactor;
};

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static void m3gDestroyMesh(Object *obj);

static M3Gint m3gMeshApplyAnimation(Object *self, M3Gint time);
static M3Gbool m3gInitMesh(Interface *m3g,
                           Mesh *mesh,
                           M3GVertexBuffer hVertices,
                           M3GIndexBuffer *hTriangles,
                           M3GAppearance *hAppearances,
                           M3Gint trianglePatchCount,
                           M3GClass classID);
static M3Gint m3gMeshDoGetReferences(Object *self, Object **references);
static Object *m3gMeshFindID(Object *self, M3Gint userID);
static M3Gbool m3gMeshDuplicate(const Object *original, Object **clone, Object **pairs, M3Gint *numPairs);
static M3Gbool m3gMeshValidate(Node *self, M3Gbitmask stateBits, M3Gint scope);

static M3Gbool m3gMeshRayIntersectInternal(	Mesh *mesh,
                                            VertexBuffer *vertices,
            								M3Gint mask,
            								M3Gfloat *ray,
            								RayIntersection *ri,
            								Matrix *toGroup);

#if 0
static M3Gint m3gMeshRenderingCost(const Mesh *mesh);
#endif

static M3Gbool m3gQueueMesh(Mesh *mesh, const Matrix *toCamera,
                            RenderQueue *renderQueue);


#endif /*__M3G_MESH_H__*/
