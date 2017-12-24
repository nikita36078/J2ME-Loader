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
* Description: Node interface
*
*/

#ifndef __M3G_NODE_H__
#define __M3G_NODE_H__

/*!
 * \internal
 * \file
 * \brief Node interface
 */

#include "m3g_transformable.h"

#include "m3g_gl.h"
#include "m3g_math.h"
#include "m3g_lightmanager.h"
#include "m3g_renderqueue.h"

#define NODE_ALPHA_FACTOR_BITS  16

/* NOTE: Implementation depends on these values -- do not change! */
#define CULLMASK_OUTSIDE        0x0
#define CULLMASK_INSIDE         0x1
#define CULLMASK_INTERSECTS     0x2
#define CULLMASK_ALL            0xAAA
M3G_CT_ASSERT(CULLMASK_INSIDE == (CULLMASK_INTERSECTS >> 1));

/*
 * Estimated load metrics for VF culling
 *
 */
#define VFC_BBOX_COST           30
#define VFC_NODE_OVERHEAD       10
#define VFC_RENDERCALL_OVERHEAD 15
#define VFC_TRIANGLE_COST       1
#define VFC_VERTEX_COST         1

/*
 * Enable bit masks
 */
#define NODE_RENDER_BIT 0x1
#define NODE_PICK_BIT   0x2

/*!
 * \internal
 * \brief RayIntersection object
*/
struct M3GRayIntersectionImpl
{
    Node *root;
    Camera *camera;
    M3Gfloat x;
    M3Gfloat y;
    M3Gfloat tMin;
	M3Gfloat distance;
	M3Gint submeshIndex;
	M3Gfloat textureS[M3G_NUM_TEXTURE_UNITS];
	M3Gfloat textureT[M3G_NUM_TEXTURE_UNITS];
	M3Gfloat normal[3];
    Node *intersected;
};

/*
RayIntersection Java side result must be in this format.

Offsets Contents
-------------------------------------
0       distance
1       subMeshIndex
2-3     textureS coordinates
4-5     textureT coordinates
6-8     normal coordinates
9-14    ray coordinates and direction

*/

/*!
 * \internal
 * \brief Recursively inherited state passed to each SetupRender call
 */
typedef struct 
{
    Matrix toCamera;
    M3Gbitmask cullMask;
} SetupRenderState;

/* Function pointer prototypes */

typedef M3Gbool (*m3gAlignFuncPtr)			    (Node *self, const Node *refNode);
typedef void    (*m3gDoRenderFuncPtr)		    (Node *self, RenderContext *ctx, const Matrix *toCamera, M3Gint patchIndex);
typedef M3Gint  (*m3gGetBBoxFuncPtr)            (Node *self, AABB *bbox);
typedef M3Gbool (*m3gRayIntersectFuncPtr)       (Node *self, M3Gint mask, M3Gfloat *ray, RayIntersection *ri, Matrix *toGroup);
typedef M3Gbool (*m3gSetupRenderFuncPtr)	    (Node *self, const Node *caller, SetupRenderState *rs, RenderQueue *renderQueue);
typedef void    (*m3gUpdateDuplicateRefFuncPtr)	(Node *self, Object **pairs, M3Gint numPairs);
typedef M3Gbool (*m3gValidate)                  (Node *self, M3Gbitmask state, M3Gint scope);

/*!
 * \internal
 * \brief Node class virtual functions
 */
typedef struct
{
	TransformableVFTable transformable;
    
	m3gAlignFuncPtr                 align;
	m3gDoRenderFuncPtr              doRender;
    m3gGetBBoxFuncPtr               getBBox;
	m3gRayIntersectFuncPtr          rayIntersect;
	m3gSetupRenderFuncPtr           setupRender;
    m3gUpdateDuplicateRefFuncPtr    updateDuplicateReferences;
    m3gValidate                     validate;
} NodeVFTable;

/*!
 * \internal
 * \brief Node class structure
 *
 */
struct M3GNodeImpl
{
	Transformable transformable;

	/* See default values form RI implementation Node.java */

    /* These scene graph pointers are managed by the Group class */
    
	Node *parent;
	Node *left;
	Node *right;
    
	M3Gint scope;

    /* Alignment references */
    
    Node *zReference;
	Node *yReference;

    /* Various node flags and other bitfield data */

    M3Guint alphaFactor : NODE_ALPHA_FACTOR_BITS;
    
	M3Guint zTarget     : 3; /* Z alignment target */
	M3Guint yTarget     : 3; /* Y alignment target */

	M3Guint enableBits  : 2; /* Rendering/picking enable bits */

    M3Guint hasBones    : 1; /* Node is part of a SkinnedMesh skeleton */
    M3Guint hasRenderables : 1; /* Node has renderables in its subtree */

    M3Guint dirtyBits   : 2; /* BBox and transform dirty bits */
};

/*!
 * \internal \brief Node bounding box in valid/dirty bitmasks
 */
#define NODE_BBOX_BIT           0x01
/*!
 * \internal \brief Node-child transformations in valid/dirty bitmasks
 */
#define NODE_TRANSFORMS_BIT     0x02

/* Sanity check; check compiler padding settings if this assert fails */

M3G_CT_ASSERT(sizeof(Node) == sizeof(Transformable) + 28);

              
/*----------------------------------------------------------------------
 * Virtual functions
 *--------------------------------------------------------------------*/

static void m3gDestroyNode(Object *obj);
static M3Gbool m3gNodeAlign(Node *self, const Node *refNode);
static M3Gbool m3gNodeDuplicate(const Object *original, Object **clone, Object **pairs, M3Gint *numPairs);
static M3Gint m3gNodeGetBBox(Node *self, AABB *bbox);
static M3Gbool m3gNodeIsCompatible(M3Gint property);
static M3Gbool m3gNodeRayIntersect(Node *self, M3Gint mask, M3Gfloat *ray, RayIntersection *ri, Matrix *toGroup);
static void m3gNodeUpdateProperty(Object *self, M3Gint property, M3Gint valueSize, const M3Gfloat *value);
static void m3gNodeUpdateDuplicateReferences(Node *self, Object **pairs, M3Gint numPairs);
static M3Gbool m3gNodeValidate(Node *self, M3Gbitmask stateBits, M3Gint scope);


static M3G_INLINE M3Gint m3gGetNodeBBox(Node *node, AABB *bbox)
{
    return M3G_VFUNC(Node, node, getBBox)(node, bbox);
}

static M3G_INLINE M3Gbool m3gValidateNode(Node *node, M3Gbitmask stateBits, M3Gint scope) 
{
    return M3G_VFUNC(Node, node, validate)(node, stateBits, scope);
}

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static void m3gInitNode(Interface *m3g, Node *node, M3GClass classID);
static M3Guint m3gGetTotalAlphaFactor(Node *node, const Node *root);
static M3Gbool m3gHasEnabledPath(const Node *node, const Node *root);
static M3Gbool m3gHasPickablePath(const Node *node, const Node *root);
static M3Gbool m3gIsChildOf(const Node *parent, const Node *child);
static Node *m3gGetRoot(const Node *node);

static void m3gInvalidateNode(Node *node, M3Gbitmask flags);

typedef void    (*NodeFuncPtr)  (Node *node, void *params);
static void     m3gForSubtree   (Node *node, NodeFuncPtr func, void *params);

static void     m3gSetParent    (Node *node, Node *parent);
static Node *m3gGetDuplicatedInstance(Node *self, Object **references, M3Gint numRef);

#if defined(M3G_ENABLE_VF_CULLING)           
static void m3gUpdateCullingMask(SetupRenderState *s, const Camera *cam, const AABB *bbox);
#endif

/*!
 * \internal
 * \brief Type-safe helper function
 */
static M3G_INLINE void m3gGetCompositeNodeTransform(const Node *node, Matrix *mtx)
{
    m3gGetCompositeTransform((Transformable*) node, mtx);
    M3G_ASSERT(m3gIsWUnity(mtx));
}

/*!
 * \internal
 * \brief Type-safe helper function
 */
static M3G_INLINE M3Gbool m3gGetInverseNodeTransform(const Node *node, Matrix *mtx)
{
    M3Gbool ok = m3gGetInverseCompositeTransform((Transformable*) node, mtx);
    M3G_ASSERT(m3gIsWUnity(mtx));
    return ok;
}

#endif /*__M3G_NODE_H__*/
