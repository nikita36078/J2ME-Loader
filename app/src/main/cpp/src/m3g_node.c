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
* Description: Node implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Node implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_node.h"
#include "m3g_memory.h"
#include "m3g_animationtrack.h"
#include "m3g_skinnedmesh.h"
#include "m3g_tcache.h"
#include "m3g_transformable.h"

#define TARGET_NONE   0
#define TARGET_X_AXIS 1
#define TARGET_Y_AXIS 2
#define TARGET_Z_AXIS 3
#define TARGET_ORIGIN 4

/*----------------------------------------------------------------------
 * Private functions
 *--------------------------------------------------------------------*/

static M3Guint internalTarget(M3Genum target)
{
    switch (target) {
    case M3G_NONE:
        return TARGET_NONE;
    case M3G_ORIGIN:
        return TARGET_ORIGIN;
    case M3G_X_AXIS:
        return TARGET_X_AXIS;
    case M3G_Y_AXIS:
        return TARGET_Y_AXIS;
    case M3G_Z_AXIS:
        return TARGET_Z_AXIS;
    default:
        M3G_ASSERT(M3G_FALSE);
        return TARGET_NONE;
    }
}   

static M3Guint externalTarget(M3Genum target)
{
    switch (target) {
    case TARGET_NONE:
        return M3G_NONE;
    case TARGET_ORIGIN:
        return M3G_ORIGIN;
    case TARGET_X_AXIS:
        return M3G_X_AXIS;
    case TARGET_Y_AXIS:
        return M3G_Y_AXIS;
    case TARGET_Z_AXIS:
        return M3G_Z_AXIS;
    default:
        M3G_ASSERT(M3G_FALSE);
        return M3G_NONE;
    }
}   

/*----------------------------------------------------------------------
 * Constructor & destructor
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Initializes a Node object. See specification
 * for default values.
 *
 * \param m3g           M3G interface
 * \param node          Node object
 * \param vfTable       virtual function table
 */
static void m3gInitNode(Interface *m3g, Node *node, M3GClass classID)
{
	/* Node is derived from Transformable */
	m3gInitTransformable(&node->transformable, m3g, classID);
    
    /* Set default values */
    
    node->enableBits = (NODE_RENDER_BIT|NODE_PICK_BIT);
	node->alphaFactor = (1u << NODE_ALPHA_FACTOR_BITS) - 1;
	node->scope = -1;
    node->zTarget = TARGET_NONE;
    node->yTarget = TARGET_NONE;
}

/*!
 * \internal
 * \brief Destroys this Node object.
 *
 * \param obj Node object
 */
static void m3gDestroyNode(Object *obj)
{
    Node *node = (Node *) obj;
    M3G_VALIDATE_OBJECT(node);
    M3G_ASSERT(node->parent == NULL);
    m3gDestroyTransformable((Object *) node);
}

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Checks if node is a child of the parent.
 *
 * \param parent    assumed parent Node object
 * \param child     Node object to check
 * \retval          M3G_TRUE is a child
 * \retval          M3G_FALSE is not a child
 */
static M3Gbool m3gIsChildOf(const Node *parent, const Node *child)
{
	const Node *n;
	
	for (n = child; n != NULL; n = n->parent) {
		if (n->parent == parent) return M3G_TRUE;
	}
	
	return M3G_FALSE;
}

/*!
 * \internal
 * \brief Executes the given function for each node in a subtree
 *
 * The function \c func is executed recursively in each branch,
 * starting from the leaves. That is, the function is called for the
 * children of each group before the group itself.
 *
 * \param node   the node containing the subtree to process
 * \param func   pointer to the function to all for each node
 * \param params pointer to function-dependent arguments to pass
 * to each \c func invokation; this may be e.g. a structure
 * modified by \c func
 *
 * \return The return value of the top-level call to \c func
 */
static void m3gForSubtree(Node *node, NodeFuncPtr func, void *params)
{
    M3GClass nodeClass;
    M3G_VALIDATE_OBJECT(node);
    
    /* Recurse into the children first */
    
    nodeClass = M3G_CLASS(node);
    
    if (nodeClass == M3G_CLASS_SKINNED_MESH) {
        m3gForSubtree((Node*)((SkinnedMesh*)node)->skeleton, func, params);
    }
    else if (nodeClass == M3G_CLASS_GROUP ||
             nodeClass == M3G_CLASS_WORLD) {
        Group *group = (Group*) node;
        Node *child = group->firstChild;
        if (child) {
            do {
                Node *next = child->right;
                m3gForSubtree(child, func, params);
                child = next;
            } while (child != group->firstChild);
        }
    }

    /* Execute function on self */
    
    (*func)(node, params);
}

/*!
 * \internal
 * \brief Overloaded Object3D method
 *
 * \param property      animation property
 * \retval M3G_TRUE     property supported
 * \retval M3G_FALSE    property not supported
 */
static M3Gbool m3gNodeIsCompatible(M3Gint property)
{
    switch (property) {
    case M3G_ANIM_ALPHA:
    case M3G_ANIM_PICKABILITY:
    case M3G_ANIM_VISIBILITY:
        return M3G_TRUE;
    default:
        return m3gTransformableIsCompatible(property);
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method
 *
 * \param self          Node object
 * \param property      animation property
 * \param valueSize     size of value array
 * \param value         value array
 */
static void m3gNodeUpdateProperty(Object *self,
                                  M3Gint property,
                                  M3Gint valueSize,
                                  const M3Gfloat *value)
{
    Node *node = (Node *)self;
    M3G_VALIDATE_OBJECT(node);
    M3G_ASSERT_PTR(value);

    switch (property) {
    case M3G_ANIM_ALPHA:
        M3G_ASSERT(valueSize >= 1);
        node->alphaFactor =
            m3gRoundToInt(
                m3gMul(m3gClampFloat(value[0], 0.f, 1.f),
                       (float)((1 << NODE_ALPHA_FACTOR_BITS) - 1)));
        break;
    case M3G_ANIM_PICKABILITY:
        M3G_ASSERT(valueSize >= 1);
        node->enableBits &= ~NODE_PICK_BIT;
        if (value[0] >= 0.5f) {
            node->enableBits |= NODE_PICK_BIT;
        }
        break;
    case M3G_ANIM_VISIBILITY:
        M3G_ASSERT(valueSize >= 1);
        node->enableBits &= ~NODE_RENDER_BIT;
        if (value[0] >= 0.5f) {
            node->enableBits |= NODE_RENDER_BIT;
        }
        break;
    default:
        m3gTransformableUpdateProperty(self, property, valueSize, value);
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method
 *
 * \param originalObj original Node object
 * \param cloneObj pointer to cloned Node object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gNodeDuplicate(const Object *originalObj,
                                Object **cloneObj,
                                Object **pairs,
                                M3Gint *numPairs)
{
    Node *original = (Node *)originalObj;
    Node *clone = (Node *)*cloneObj;
    M3G_ASSERT_PTR(*cloneObj); /* abstract class, must be derived */

    /* Duplicate base class data */
    
    if (!m3gTransformableDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        return M3G_FALSE;
    }

    /* Duplicate our own data */
    
    clone->zReference  = original->zReference;
    clone->yReference  = original->yReference;
    clone->zTarget     = original->zTarget;
    clone->yTarget     = original->yTarget;
    clone->enableBits  = original->enableBits;
    clone->alphaFactor = original->alphaFactor;
    clone->scope       = original->scope;
    clone->hasBones    = original->hasBones;
    clone->hasRenderables = original->hasRenderables;
    
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Find corresponding duplicate for a Node
 *
 * \param node Node object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static Node *m3gGetDuplicatedInstance(Node *node, Object **pairs, M3Gint numPairs)
{
    M3Gint i;
    for (i = 0; i < numPairs; i++)
        if (pairs[i * 2] == (Object *)node)
            return (Node *)pairs[i * 2 + 1];
    return NULL;
}

/*!
 * \internal
 * \brief Updates references of the duplicate object.
 *
 * When objects are duplicated scenegraph references
 * must be updated to equivalent duplicated references.
 * This function is overloaded by objects that have
 * references that has to be updated.
 *
 * \param self Node object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static void m3gNodeUpdateDuplicateReferences(Node *self, Object **pairs, M3Gint numPairs)
{
    if (self->zTarget != TARGET_NONE && self->zReference != NULL) {
        Node *duplicatedInstance = m3gGetDuplicatedInstance(self, pairs, numPairs);
        Node *duplicatedRef = m3gGetDuplicatedInstance(self->zReference, pairs, numPairs);
        if (duplicatedRef != NULL
            && m3gIsChildOf(m3gGetRoot(duplicatedInstance), duplicatedRef)) {
            duplicatedInstance->zReference = duplicatedRef;
        }
    }
    if (self->yTarget != TARGET_NONE && self->yReference != NULL) {
        Node *duplicatedInstance = m3gGetDuplicatedInstance(self, pairs, numPairs);
        Node *duplicatedRef = m3gGetDuplicatedInstance(self->yReference, pairs, numPairs);
        if (duplicatedRef != NULL
            && m3gIsChildOf(m3gGetRoot(duplicatedInstance), duplicatedRef)) {
            duplicatedInstance->yReference = duplicatedRef;
        }
    }
}

/*!
 * \internal
 * \brief Gets size of the subtree
 *
 * \param node Node object
 * \param numRef number of references
 */
static void m3gDoGetSubtreeSize(Node *node, void *numRef)
{
    M3Gint *num = (M3Gint *)numRef;
    M3G_UNREF(node);
    (*num)++;
}

/*!
 * \internal
 * \brief Default function for non-pickable objects
 *
 * \param self      Camera object
 * \param mask      pick scope mask
 * \param ray       pick ray
 * \param ri        RayIntersection object
 * \param toGroup   transform to originating group
 * \retval M3G_TRUE always return success
 */
static M3Gbool m3gNodeRayIntersect(Node *self,
                                   M3Gint mask,
                                   M3Gfloat *ray,
                                   RayIntersection *ri,
                                   Matrix *toGroup)
{
    M3G_UNREF(self);
    M3G_UNREF(mask);
    M3G_UNREF(ray);
    M3G_UNREF(ri);
    M3G_UNREF(toGroup);

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Computes the bounding box for this node
 *
 * \param self  node pointer
 * \param bbox  bounding box structure filled in for non-zero return values
 * 
 * \return The "yield" factor for the node, i.e. the approximate
 * rendering cost of the node \em including any internal bounding box
 * checks; the yield factor is used to estimate the benefit of adding
 * enclosing bounding boxes at higher levels in the scene tree
 */
static M3Gint m3gNodeGetBBox(Node *self, AABB *bbox)
{
    M3G_UNREF(self);
    M3G_UNREF(bbox);
    return 0;
}

/*!
 * \internal
 * \brief Updates the bounding box for this node
 */
static M3Gbool m3gNodeValidate(Node *self, M3Gbitmask stateBits, M3Gint scope)
{
    M3G_UNREF(stateBits);
    M3G_UNREF(scope);

    /* Invalidate parent state in case we've encountered a previously
     * disabled node, then reset the dirty bits */
    
    if (self->dirtyBits && self->parent) {
        m3gInvalidateNode(self->parent, self->dirtyBits);
    }
    self->dirtyBits = 0;
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Gets a vector according to alignment target
 * and transforms it with a given transform.
 *
 * \param target        alignment target
 * \param transform     transform to be applied
 * \param out           vector to fill in
 */
static void m3gTransformAlignmentTarget(M3Genum target,
                                        const Matrix *transform,
                                        Vec4 *out)
{
	switch (target) {
    case TARGET_ORIGIN:
		*out = Vec4_ORIGIN;
	    break;
	case TARGET_X_AXIS:
		*out = Vec4_X_AXIS;
	    break;
	case TARGET_Y_AXIS:
		*out = Vec4_Y_AXIS;
	    break;
	case TARGET_Z_AXIS:
		*out = Vec4_Z_AXIS;
	    break;
	default:
		M3G_ASSERT(M3G_FALSE);
	}

	m3gTransformVec4(transform, out);
}

/*!
 * \internal
 * \brief Computes a single alignment rotation for a node.
 *
 * \param node              Node object
 * \param srcAxis           source axis
 * \param targetNode        Node object
 * \param targetAxisName    target axis name
 * \param constraint        constraint
 */
static M3Gbool m3gComputeAlignmentRotation(Node *node,
                                           const Vec3 *srcAxis,
                                           const Node *targetNode,
                                           M3Genum targetAxisName,
                                           M3Genum constraint)
{
    const Node *parent = node->parent;
    Matrix transform;
    Vec4 targetAxis;
    
    M3G_VALIDATE_OBJECT(parent);
    M3G_ASSERT(constraint == TARGET_NONE || constraint == TARGET_Z_AXIS);

    /* Get the transformation from the reference target node to the
     * current node, omitting all components except translation.
     * Rotation is also applied if this is a constrained alignment. */
    {
        const Transformable *tf = &node->transformable;
        
        if (!m3gGetTransformTo((M3GNode) targetNode, (M3GNode) parent,
                               &transform)) {
            return M3G_FALSE;
        }
        m3gPreTranslateMatrix(&transform, -tf->tx, -tf->ty, -tf->tz);
        
        if (constraint != TARGET_NONE) {
            Quat rot = tf->orientation;
            rot.w = -rot.w;
            m3gPreRotateMatrixQuat(&transform, &rot);
        }
    }

    m3gTransformAlignmentTarget(targetAxisName, &transform, &targetAxis);

    /* Apply the Z constraint if enabled; this is done by simply
     * zeroing the Z component of the target vector.  If the X and Y
     * alone don't span a non-zero vector, just exit as there's
     * nothing defined to rotate about. */

    if (constraint == TARGET_Z_AXIS) {
        M3Gfloat norm = m3gAdd(m3gMul(targetAxis.x, targetAxis.x),
                               m3gMul(targetAxis.y, targetAxis.y));
        if (norm < 1.0e-5f) {
            return M3G_TRUE;
        }
        norm = m3gRcpSqrt(norm);
        
        targetAxis.x = m3gMul(targetAxis.x, norm);
        targetAxis.y = m3gMul(targetAxis.y, norm);
        targetAxis.z = 0.0f;
                                
        M3G_ASSERT(srcAxis->z == 0.0f);
    }
    else {
        m3gNormalizeVec3((Vec3*)&targetAxis); /* srcAxis will be unit length */
    }

    if (constraint != TARGET_NONE) {
        Quat rot;
        m3gSetQuatRotation(&rot, srcAxis, (const Vec3*) &targetAxis);
        m3gMulQuat(&node->transformable.orientation, &rot);
    }
    else {
        m3gSetQuatRotation(&node->transformable.orientation,
                           srcAxis, (const Vec3*) &targetAxis);
    }

    /* Invalidate transformations and bounding boxes after setting
     * node orientation */
    
    m3gInvalidateTransformable((Transformable*)node);

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Computes alignment for a single node.
 *
 * \param node              Node object
 * \param refNode           Node object
 * \retval                  M3G_TRUE alignment ok
 * \retval                  M3G_FALSE alignment failed
 */
static M3Gbool m3gComputeAlignment(Node *node, const Node *refNode)
{
    const Node *root = m3gGetRoot(node);
    const Node *zRef = node->zReference;
    const Node *yRef = node->yReference;
    const M3Genum zTarget = node->zTarget;
    const M3Genum yTarget = node->yTarget;
    M3G_VALIDATE_OBJECT(node);

    /* Quick exit if nothing to do */
    
    if (zTarget == TARGET_NONE && yTarget == TARGET_NONE) {
        return M3G_TRUE;
    }
        
    /* Check scene graph state */
    
    if (zRef != NULL && (m3gIsChildOf(node, zRef) || m3gGetRoot(zRef) != root)) {
        m3gRaiseError(M3G_INTERFACE(node), M3G_INVALID_OPERATION);
        return M3G_FALSE;
    }
    if (yRef != NULL && (m3gIsChildOf(node, yRef) || m3gGetRoot(yRef) != root)) {
        m3gRaiseError(M3G_INTERFACE(node), M3G_INVALID_OPERATION);
        return M3G_FALSE;
    }

    /* Compute the alignment rotations for Z and Y */
    {
        if (node->zTarget != TARGET_NONE) {
            if (zRef == NULL && refNode == node) {
                m3gRaiseError(M3G_INTERFACE(node), M3G_INVALID_OPERATION);
                return M3G_FALSE;
            }
            if (!m3gComputeAlignmentRotation(
                    node,
                    (const Vec3*) &Vec4_Z_AXIS,
                    zRef != NULL ? zRef : refNode,
                    zTarget,
                    TARGET_NONE)) {
                return M3G_FALSE;
            }
        }
        if (node->yTarget != TARGET_NONE) {
            if (yRef == NULL && refNode == node) {
                m3gRaiseError(M3G_INTERFACE(node), M3G_INVALID_OPERATION);
                return M3G_FALSE;
            }
            if (!m3gComputeAlignmentRotation(
                    node,
                    (const Vec3*) &Vec4_Y_AXIS,
                    yRef != NULL ? yRef : refNode,
                    yTarget,
                    zTarget != TARGET_NONE ? TARGET_Z_AXIS : TARGET_NONE)) {
                return M3G_FALSE;
            }
        }
    }   
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Gets the transformation to an ancestor node
 */
static void m3gGetTransformUpPath(const Node *node, const Node *ancestor, Matrix *transform)
{
    M3G_ASSERT(node);
    
    if (node == ancestor) {
        m3gIdentityMatrix(transform);
    }
    else {
        TCache *tc;
        M3G_ASSERT(!ancestor || m3gIsChildOf(ancestor, node));
    
        /* Look for a cached path */

        tc = m3gGetTransformCache(M3G_INTERFACE(node));
        if (m3gGetCachedPath(tc, node, ancestor, transform)) {
            return;
        }
    
        /* No dice -- do a recursive search and cache the result */

        if (node->parent == ancestor) {
            m3gGetCompositeNodeTransform(node, transform);
        }
        else {
            m3gGetTransformUpPath(node->parent, ancestor, transform);
            {
                Matrix mtx;
                m3gGetCompositeNodeTransform(node, &mtx);
                m3gMulMatrix(transform, &mtx);
            }
        }
        m3gCachePath(tc, node, ancestor, transform);
    }
}

/*!
 * \internal
 * \brief Gets depth of a node in the scenegraph.
 *
 * \param node              Node object
 * \return                  Depth of the node
 */
static M3Gint m3gGetDepth(const Node *node)
{
	const Node *n = node;
	M3Gint depth = 0;

	while (n->parent != NULL) {
	    n = n->parent;
	    depth++;
	}

	return depth;
}

/*!
 * \internal
 * \brief Gets root of a node in the scenegraph.
 *
 * \param node              Node object
 * \return                  root Node object
 */
static Node *m3gGetRoot(const Node *node)
{
    const Node *n = node;

    while (n->parent != NULL) {
        n = n->parent;
    }

    return (Node *)n;
}

/*!
 * \internal
 * \brief Gets total alpha factor.
 *
 * \param node              Node object
 * \param root              root Node object
 */
static M3Guint m3gGetTotalAlphaFactor(Node *node, const Node *root)
{
    const Node *n = node;
    M3Guint f = node->alphaFactor;
    
    while (n->parent != NULL && n != root) {
        n = n->parent;
        f = ((f + 1) * n->alphaFactor) >> NODE_ALPHA_FACTOR_BITS;
    }
    return f;
}

/*!
 * \internal
 * \brief Checks if node is enabled for rendering from the root.
 *
 * \param node              Node object
 * \param root              root Node object
 * \retval                  M3G_TRUE node is visible
 * \retval                  M3G_FALSE node is not visible
 */
static M3Gbool m3gHasEnabledPath(const Node *node, const Node *root)
{
	const Node *n;
	
	for (n = node; n != NULL; n = n->parent) {
	    if (!(n->enableBits & NODE_RENDER_BIT)) {
            return M3G_FALSE;
        }
	    if (n == root) {
            break;
        }
	}

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Checks if node is pickable from the root.
 *
 * \param node              Node object
 * \param root              root Node object
 * \retval                  M3G_TRUE node is pickable
 * \retval                  M3G_FALSE node is not pickable
 */
static M3Gbool m3gHasPickablePath(const Node *node, const Node *root)
{
	const Node *n;
	
	for (n = node; n != NULL; n = n->parent) {
	    if (!(n->enableBits & NODE_PICK_BIT)) {
            return M3G_FALSE;
        }
	    if (n == root) {
            break;
        }
	}

    return M3G_TRUE;
}

#if defined(M3G_ENABLE_VF_CULLING)
/*!
 * \brief Invalidates the bounding box hierarchy from a node upwards
 */
static void m3gInvalidateNode(Node *node, M3Gbitmask flags)
{
    Interface *m3g = M3G_INTERFACE(node);
    M3G_BEGIN_PROFILE(m3g, M3G_PROFILE_VFC_UPDATE);

    while (node && (node->dirtyBits & flags) != flags) {
        node->dirtyBits |= flags;
        node = node->parent;
    }
    M3G_END_PROFILE(m3g, M3G_PROFILE_VFC_UPDATE);
}
#endif /*M3G_ENABLE_VF_CULLING*/

/*!
 * \internal
 * \brief Aligns a node.
 *
 * \param node              Node object
 * \param ref               reference Node object
 *
 * \retval M3G_TRUE continue align
 * \retval M3G_FALSE abort align
 */
static M3Gbool m3gNodeAlign(Node *node, const Node *ref) 
{
    if (ref == NULL) {
        return m3gComputeAlignment(node, node);
    }
    else {
        M3G_VALIDATE_OBJECT(ref);
        return m3gComputeAlignment(node, ref);
    }
}

/*!
 * \internal
 * \brief Updates node counters when moving nodes around
 */
static void m3gUpdateNodeCounters(Node *node,
                                  M3Gint nonCullableChange,
                                  M3Gint renderableChange)
{
    Interface *m3g = M3G_INTERFACE(node);
    M3Gbool hasRenderables = (renderableChange > 0);
    M3G_BEGIN_PROFILE(m3g, M3G_PROFILE_VFC_UPDATE);
    while (node) {
        M3GClass nodeClass = M3G_CLASS(node);
        if (nodeClass == M3G_CLASS_GROUP || nodeClass == M3G_CLASS_WORLD) {
            Group *g = (Group *) node;
            g->numNonCullables = (M3Gushort)(g->numNonCullables + nonCullableChange);
            g->numRenderables  = (M3Gushort)(g->numRenderables + renderableChange);
            hasRenderables = (g->numRenderables > 0);
        }
        node->hasRenderables = hasRenderables;
        node = node->parent;
    }
    M3G_END_PROFILE(m3g, M3G_PROFILE_VFC_UPDATE);
}

/*!
 * \internal
 * \brief Sets the parent link of this node to a new value
 *
 * Relevant reference counts are updated accordingly, so note that
 * setting the parent to NULL may lead to either the node or the
 * parent itself being destroyed.
 *
 * \param node Node object
 * \param parent parent Node object
 */
static void m3gSetParent(Node *node, Node *parent)
{
    M3GClass nodeClass;
    M3Gint nonCullableChange = 0, renderableChange = 0;
    M3G_VALIDATE_OBJECT(node);

    /* Determine the number of various kinds of nodes being moved around */

    M3G_BEGIN_PROFILE(M3G_INTERFACE(node), M3G_PROFILE_VFC_UPDATE);
    nodeClass = M3G_CLASS(node);
    switch (nodeClass) {
    case M3G_CLASS_GROUP:
    {
        const Group *g = (const Group *) node;
        nonCullableChange = g->numNonCullables;
        renderableChange  = g->numRenderables;
        break;
    }
    case M3G_CLASS_SPRITE:
        renderableChange = 1;
        if (m3gIsScaledSprite((M3GSprite) node)) {
            break;
        }
        /* conditional fall-through! */
    case M3G_CLASS_LIGHT:
        nonCullableChange = 1;
        break;
    case M3G_CLASS_SKINNED_MESH:
    {
        const SkinnedMesh *mesh = (const SkinnedMesh *) node;
        nonCullableChange += mesh->skeleton->numNonCullables;
        renderableChange  += mesh->skeleton->numRenderables + 1;
        break;
    }
    case M3G_CLASS_MESH:
    case M3G_CLASS_MORPHING_MESH:
        renderableChange = 1;
        break;
    default:
        ;
    }
    M3G_END_PROFILE(M3G_INTERFACE(node), M3G_PROFILE_VFC_UPDATE);

    /* Invalidate any cached transformation paths through this node
     * *before* we move the node */
    
    m3gInvalidateCachedPaths(m3gGetTransformCache(M3G_INTERFACE(node)), node);
    
    /* Update bookkeeping for the old parent tree */

    if (node->parent) {
        m3gUpdateNodeCounters(node->parent,
                              -nonCullableChange, -renderableChange);
        if (renderableChange) {
            m3gInvalidateNode(node->parent, NODE_BBOX_BIT|NODE_TRANSFORMS_BIT);
        }
    }

    /* Change the parent link */
    
    if (node->parent == NULL && parent != NULL) {
        node->parent = parent;
        m3gAddRef((Object *) node);
    }
    else if (node->parent != NULL && parent == NULL) {
        node->parent = parent;
        m3gDeleteRef((Object *) node);
    }

    /* Update bookkeeping for the new parent tree */

    if (parent) {
        M3Gbitmask dirtyBits = node->dirtyBits;
        if (renderableChange) {
            dirtyBits |= NODE_BBOX_BIT;
        }
        if (node->hasBones) {
            dirtyBits |= NODE_TRANSFORMS_BIT;
        }
        m3gUpdateNodeCounters(parent, nonCullableChange, renderableChange);
        m3gInvalidateNode(parent, dirtyBits);
    }
}

/*!
 * \brief Computes the "near" and "far" box vertices
 * for plane testing
 */
static M3G_INLINE void m3gGetTestPoints(const Vec3 *planeNormal,
                                        const AABB *box,
                                        Vec3 *vNear, Vec3 *vFar)
{
    const M3Gfloat *fNormal = (const M3Gfloat*) planeNormal;
    M3Gfloat *fNear = (M3Gfloat*) vNear;
    M3Gfloat *fFar  = (M3Gfloat*) vFar;
    int i;
    
    for (i = 0; i < 3; ++i) {
        M3Gfloat n = *fNormal++;
        if (IS_NEGATIVE(n)) {
            *fNear++ = box->max[i];
            *fFar++ = box->min[i];
        }
        else {
            *fNear++ = box->min[i];
            *fFar++ = box->max[i];
        }
    }
}

#if defined(M3G_ENABLE_VF_CULLING)
/*!
 * \internal
 * \brief Update the frustum culling mask for one level of an AABB hierarchy
 *
 * \param s    the current traversal state
 * \param bbox the bounding box to check against
 */
static void m3gUpdateCullingMask(SetupRenderState *s,
                                 const Camera *cam, const AABB *bbox)
{
    M3Gbitmask cullMask = s->cullMask;
    M3G_BEGIN_PROFILE(M3G_INTERFACE(cam), M3G_PROFILE_VFC_TEST);

    /* First, check whether any planes are previously marked as
     * intersecting */
    
    if (cullMask & CULLMASK_ALL) {
                
        /* We need to do some culling, so let's get the planes and the
         * transformation matrix; note that the "toCamera" matrix is
         * the inverse of the camera-to-node matrix, so we only need
         * to transpose */

        M3Gbitmask planeMask;
        const Vec4 *camPlanes = m3gFrustumPlanes(cam);
        
        Matrix t;
        m3gMatrixTranspose(&t, &s->toCamera);
        
        /* Loop over the active frustum planes, testing the ones we've
         * previously intersected with */

        planeMask = CULLMASK_INTERSECTS;
        while (planeMask <= cullMask) {
            if (cullMask & planeMask) {
                
                /* Transform the respective frustum plane into the node
                 * local space our AABB is in */
                
                Vec4 plane;
                plane = *camPlanes++;
                m3gTransformVec4(&t, &plane);
                
                /* Test the AABB against the plane and update the mask
                 * based on the result */
                
                m3gIncStat(M3G_INTERFACE(cam), M3G_STAT_CULLING_TESTS, 1);
                {
                    /* Get the "near" and "far" corner points of the box */
                    
                    const Vec3* normal = (Vec3*) &plane;
                    Vec3 vNear, vFar;
                    m3gGetTestPoints(normal, bbox, &vNear, &vFar);

                    /* Our normals point inside, so flip this */
                    
                    plane.w = m3gNegate(plane.w);
                    
                    /* "Far" point behind plane? */
                    
                    if (m3gDot3(normal, &vFar) < plane.w) {
                        /* All outside, no need to test further! */
                        cullMask = 0;
                        break;
                    }
                    
                    /* "Near" point in front of plane? */

                    if (m3gDot3(normal, &vNear) > plane.w) {
                        cullMask &= ~planeMask;
                        cullMask |= planeMask >> 1; /* intersects->inside */
                    }
                }
            }
            planeMask <<= 2; /* next plane */
        }
        s->cullMask = cullMask; /* write the output mask */
    }
    M3G_END_PROFILE(M3G_INTERFACE(cam), M3G_PROFILE_VFC_TEST);
}
#endif /*M3G_ENABLE_VF_CULLING*/

/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Gets transform from node to another.
 *
 * \param handle            Node object
 * \param hTarget           target Node object
 * \param transform         transform to fill in
 * \retval                  M3G_TRUE success
 * \retval                  M3G_FALSE failed
 */
M3G_API M3Gbool m3gGetTransformTo(M3GNode handle,
                                  M3GNode hTarget,
                                  M3GMatrix *transform)
{
    const Node *node = (Node *) handle;
	const Node *target = (Node *) hTarget;
    TCache *tc;
	
    M3G_VALIDATE_OBJECT(node);
    M3G_BEGIN_PROFILE(M3G_INTERFACE(node), M3G_PROFILE_TRANSFORM_TO);

    /* Look for quick exits first */
    
    tc = m3gGetTransformCache(M3G_INTERFACE(node));

    if (node == target) {
        m3gIdentityMatrix(transform);
        M3G_END_PROFILE(M3G_INTERFACE(node), M3G_PROFILE_TRANSFORM_TO);
        return M3G_TRUE;
    }
    else if (m3gGetCachedPath(tc, node, target, transform)) {
        M3G_END_PROFILE(M3G_INTERFACE(node), M3G_PROFILE_TRANSFORM_TO);
        return M3G_TRUE;
    }
    else {

        /* No luck, must recompute the whole thing -- begin by finding
         * a common ancestor node for the pivot point of the path */
        
        const Node *pivot = NULL;
        {
            const Node *s = node;
            const Node *t = target;
	
            /* First traverse to the same depth */
            {
                int sd = m3gGetDepth(s);
                int td = m3gGetDepth(t);
            
                while (sd > td) {
                    s = s->parent;
                    --sd;
                }
                while (td > sd) {
                    t = t->parent;
                    --td;
                }
            }

            /* Then traverse until we reach a common node or run out of
             * ancestors in both branches, meaning there is no path
             * between the nodes */
        
            while (s != t) {
                s = s->parent;	
                t = t->parent;
            }
            pivot = s;
        }
        if (!pivot) {
            M3G_END_PROFILE(M3G_INTERFACE(node), M3G_PROFILE_TRANSFORM_TO);
            return M3G_FALSE;
        }
        
        /* Now, fetch the transformations for both branches and
         * combine into the complete transformation; optimize by
         * skipping most of that altogether for paths where the target
         * node is the topmost node of the path */
        
        if (pivot != target) {
            Matrix targetPath;
            Matrix sourcePath;

            /* Look for a cached version of the to-target path to
             * avoid the inversion if possible */
            
            if (!m3gGetCachedPath(tc, pivot, target, &targetPath)) {
                m3gGetTransformUpPath(target, pivot, &targetPath);
            
                /* Invert the target-side path since we want the
                 * downstream transformation for that one */
            
                M3G_BEGIN_PROFILE(M3G_INTERFACE(node), M3G_PROFILE_TRANSFORM_INVERT);
                if (!m3gInvertMatrix(&targetPath)) {    
                    M3G_END_PROFILE(M3G_INTERFACE(node), M3G_PROFILE_TRANSFORM_TO);
                    m3gRaiseError(M3G_INTERFACE(node), M3G_ARITHMETIC_ERROR);
                    return M3G_FALSE;
                }

                /* Cache the inverse for future use */
                m3gCachePath(tc, pivot, target, &targetPath);
            }
            
            M3G_ASSERT(m3gIsWUnity(&targetPath));

            /* Paste in the from-source path to get the complete
             * transformation for the path */

            if (pivot != node) {
                m3gGetTransformUpPath(node, pivot, &sourcePath);

                M3G_END_PROFILE(M3G_INTERFACE(node), M3G_PROFILE_TRANSFORM_INVERT);
                m3gRightMulMatrix(&targetPath, &sourcePath);
                m3gCopyMatrix(transform, &targetPath);
                M3G_ASSERT(m3gIsWUnity(transform));
            
                /* Cache the combined result for future use */
                m3gCachePath(tc, node, target, transform);
            }
            else {
                *transform = targetPath;
            }
        }
        else {
            /* For many cases, we only need this upstream path */
            m3gGetTransformUpPath(node, pivot, transform);
        }
        
        M3G_END_PROFILE(M3G_INTERFACE(node), M3G_PROFILE_TRANSFORM_TO);
        return M3G_TRUE;
    }
}

/*!
 * \brief Sets alignment targets.
 *
 * \param handle            Node object
 * \param hZReference       Z target Node object
 * \param zTarget           Z target type
 * \param hYReference       Y target Node object
 * \param yTarget           Y target type
 */
M3G_API void m3gSetAlignment(M3GNode handle,
                             M3GNode hZReference, M3Gint zTarget,
                             M3GNode hYReference, M3Gint yTarget)
{
    Node *node = (Node *) handle;
	Node *zReference = (Node *) hZReference;
	Node *yReference = (Node *) hYReference;
    M3G_VALIDATE_OBJECT(node);

    /* Check for errors */
	if (!m3gInRange(zTarget, M3G_NONE, M3G_Z_AXIS) ||
        !m3gInRange(yTarget, M3G_NONE, M3G_Z_AXIS)) {
		m3gRaiseError(M3G_INTERFACE(node), M3G_INVALID_VALUE);
        return;
	}

	if (zReference == node || yReference == node) {
		m3gRaiseError(M3G_INTERFACE(node), M3G_INVALID_VALUE);
        return;
	}
    
	if (zReference == yReference && zTarget == yTarget && zTarget != M3G_NONE) {
		m3gRaiseError(M3G_INTERFACE(node), M3G_INVALID_VALUE);
        return;
	}

    node->zReference = (zTarget != M3G_NONE) ? zReference : NULL;
    node->yReference = (yTarget != M3G_NONE) ? yReference : NULL;
	node->zTarget = internalTarget(zTarget);
	node->yTarget = internalTarget(yTarget);
}

/*!
 * \brief Aligns a node.
 *
 * \param hNode             Node object
 * \param hRef              reference Node object
 */
M3G_API void m3gAlignNode(M3GNode hNode, M3GNode hRef)
{
    Node *node = (Node *)hNode;
    const Node *ref = (const Node *)hRef;
    M3G_VALIDATE_OBJECT(node);
    
    if (ref != NULL && (m3gGetRoot(node) != m3gGetRoot(ref))) {
        m3gRaiseError(M3G_INTERFACE(node), M3G_INVALID_VALUE);
    }
    else {
        M3G_VFUNC(Node, node, align)(node, !ref ? node : ref);
    }
}

/*!
 * \brief Sets node alpha factor.
 *
 * \param handle            Node object
 * \param alphaFactor       node alpha factor
 */
M3G_API void m3gSetAlphaFactor(M3GNode handle, M3Gfloat alphaFactor)
{
    Node *node = (Node *) handle;
    M3G_VALIDATE_OBJECT(node);

    if (alphaFactor >= 0.f && alphaFactor <= 1.0f) {
		node->alphaFactor = (M3Guint)
            m3gRoundToInt(m3gMul(alphaFactor,
                                 (1 << NODE_ALPHA_FACTOR_BITS) - 1));
	}
	else {
	    m3gRaiseError(M3G_INTERFACE(node), M3G_INVALID_VALUE);
	}
}


/*!
 * \brief Gets node alpha factor.
 *
 * \param handle            Node object
 * \return                  node alpha factor
 */
M3G_API M3Gfloat m3gGetAlphaFactor(M3GNode handle)
{
    Node *node = (Node *) handle;
    M3G_VALIDATE_OBJECT(node);

	return m3gMul((M3Gfloat) node->alphaFactor,
                  1.f / ((1 << NODE_ALPHA_FACTOR_BITS) - 1));
}

/*!
 * \brief Sets node redering or picking enable flag.
 *
 * \param handle            Node object
 * \param which             which flag to enable
 *                          \arg M3G_SETGET_RENDERING
 *                          \arg M3G_SETGET_PICKING
 * \param enable            enable flag
 */
M3G_API void m3gEnable(M3GNode handle, M3Gint which, M3Gbool enable)
{
    Node *node = (Node *) handle;
    M3G_VALIDATE_OBJECT(node);

	switch (which) {
		case M3G_SETGET_RENDERING:
            node->enableBits &= ~NODE_RENDER_BIT;
            if (enable) {
                node->enableBits |= NODE_RENDER_BIT;
            }
            break;
		case M3G_SETGET_PICKING:
		default:
            node->enableBits &= ~NODE_PICK_BIT;
            if (enable) {
                node->enableBits |= NODE_PICK_BIT;
            }
            break;
	}
}

/*!
 * \brief Gets node redering or picking enable flag.
 *
 * \param handle            Node object
 * \param which             which flag to return
 *                          \arg M3G_SETGET_RENDERING
 *                          \arg M3G_SETGET_PICKING
 * \return                  enable flag
 */
M3G_API M3Gint m3gIsEnabled(M3GNode handle, M3Gint which)
{
    Node *node = (Node *) handle;
    M3G_VALIDATE_OBJECT(node);

	switch(which) {
		case M3G_SETGET_RENDERING:
            return (node->enableBits & NODE_RENDER_BIT) != 0;
		case M3G_SETGET_PICKING:
		default:
            return (node->enableBits & NODE_PICK_BIT) != 0;
	}
}

/*!
 * \brief Sets node scope.
 *
 * \param handle            Node object
 * \param id                node scope id
 */
M3G_API void m3gSetScope(M3GNode handle, M3Gint id)
{
    Node *node = (Node *) handle;
    M3G_VALIDATE_OBJECT(node);

	node->scope = id;
}

/*!
 * \brief Gets node scope.
 *
 * \param handle            Node object
 * \return                  node scope
 */
M3G_API M3Gint m3gGetScope(M3GNode handle)
{
    Node *node = (Node *) handle;
    M3G_VALIDATE_OBJECT(node);

	return node->scope;
}

/*!
 * \brief Gets node parent.
 *
 * \param handle            Node object
 * \return                  parent Node object
 */
M3G_API M3GNode m3gGetParent(M3GNode handle)
{
    Node *node = (Node *) handle;
    M3G_VALIDATE_OBJECT(node);

	return node->parent;
}

/*!
 * \brief Gets node alignment Z reference.
 *
 * \param handle            Node object
 * \return                  Z reference Node object
 */
M3G_API M3GNode m3gGetZRef(M3GNode handle)
{
    Node *node = (Node *) handle;
    M3G_VALIDATE_OBJECT(node);

	return node->zReference;
}

/*!
 * \brief Gets node alignment Y reference.
 *
 * \param handle            Node object
 * \return                  Y reference Node object
 */
M3G_API M3GNode m3gGetYRef(M3GNode handle)
{
    Node *node = (Node *) handle;
    M3G_VALIDATE_OBJECT(node);

	return node->yReference;
}

/*!
 * \brief Gets node alignment target
 *
 * \param handle            Node object
 * \param axis              axis
 * \return                  alignment target
 */
M3G_API M3Gint m3gGetAlignmentTarget(M3GNode handle, M3Gint axis)
{
    Node *node = (Node *) handle;
    M3G_VALIDATE_OBJECT(node);

    switch (axis) {
    case M3G_Y_AXIS: return externalTarget(node->yTarget);
    case M3G_Z_AXIS: return externalTarget(node->zTarget);
    default:
        m3gRaiseError(M3G_INTERFACE(node), M3G_INVALID_VALUE);
        return 0;
    }
}

/*!
 * \brief Gets node subtree size.
 *
 * \param handle            Node object
 * \return                  subtree size
 */
M3G_API M3Gint m3gGetSubtreeSize(M3GNode handle)
{
    M3Gint numRef = 0;
    Node *node = (Node *) handle;
    M3G_VALIDATE_OBJECT(node);

    m3gForSubtree(node, m3gDoGetSubtreeSize, (void *)&numRef);
    return numRef;
}

#undef TARGET_ORIGIN
#undef TARGET_Z_AXIS
#undef TARGET_Y_AXIS
#undef TARGET_X_AXIS
#undef TARGET_NONE

