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
* Description: Group implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Group implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_group.h"
#include "m3g_memory.h"

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Links a new child into the child list of this node.
 *
 * This assumes that all error checking has been done prior to calling
 * the function, and the operation is a valid one.
 *
 * \param child Node object
 * \param group Group object
 */
static void m3gLinkChild(Node *child, Group *group)
{
    M3G_VALIDATE_OBJECT(child);
    M3G_VALIDATE_OBJECT(group);
    
	if (group->firstChild == NULL) {
		group->firstChild = child;
		child->left = child;
		child->right = child;
	}
	else {
        Node *linkChild = group->firstChild;
        
		child->left = linkChild->left;
		linkChild->left->right = child;
        
		child->right = linkChild;
		linkChild->left = child;
	}
    m3gSetParent(child, (Node *) group);    
}

/*!
 * \internal
 * \brief Removes a child from the child list of this node.
 *
 * This assumes that all error checking has been done prior to calling
 * the function, and the operation is a valid one.
 *
 * \param child Node object
 * \param group Group object
 */
static void m3gDetachChild(Node *child, Group *group)
{
	Node *n;
    M3G_VALIDATE_OBJECT(child);
    M3G_VALIDATE_OBJECT(group);

    n = group->firstChild;
    
	do {
		if (n == child) {
            M3G_VALIDATE_OBJECT(child->right);
            M3G_VALIDATE_OBJECT(child->left);
            
			n->right->left = n->left;
			n->left->right = n->right;

			if (group->firstChild == n) {
				group->firstChild = (n->right != n) ? n->right : NULL;
			}
            
            n->left = NULL;
            n->right = NULL;
            m3gSetParent(n, NULL);
			return;
		}
        n = n->right;
	} while (n != group->firstChild);
}

/*!
 * \internal
 * \brief Destroys this Group object.
 *
 * \param obj Group object
 */
static void m3gDestroyGroup(Object *obj)
{
    /* Release child references so they can be deleted */
    
	Group *group = (Group *) obj;
	while (group->firstChild != NULL) {
        m3gDetachChild(group->firstChild, group);
	}
#   if defined(M3G_ENABLE_VF_CULLING)
    if (group->bbox) {
        m3gFree(M3G_INTERFACE(group), group->bbox);
        m3gIncStat(M3G_INTERFACE(group), M3G_STAT_BOUNDING_BOXES, -1);
    }
#   endif
    m3gDestroyNode(obj);
}

/*!
 * \internal
 * \brief Overloaded Node method.
 *
 * \param self Group object
 * \param refNode alignment reference Node object
 *
 * \retval M3G_TRUE continue align
 * \retval M3G_FALSE abort align
 */
static M3Gbool m3gGroupAlign(Node *self, const Node *refNode)
{
	Group *group = (Group *)self;
	Node *child = group->firstChild;

    if (!m3gNodeAlign(self, refNode)) {
        return M3G_FALSE;
    }

    if (child) {
        do {
            if (!M3G_VFUNC(Node, child, align)(child, refNode)) {
                return M3G_FALSE;
            }
            child = child->right;
        } while (child != group->firstChild);
	}

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Overloaded Node method.
 *
 * Setup group rendering by calling child
 * nodes' render setup.
 *
 * \param self Group object
 * \param toCamera transform to camera
 * \param alphaFactor total alpha factor
 * \param caller caller node
 * \param renderQueue RenderQueue
 *
 * \retval M3G_TRUE continue render setup
 * \retval M3G_FALSE abort render setup
 */
static M3Gbool m3gGroupSetupRender(Node *self,
                                   const Node *caller,
                                   SetupRenderState *s,
                                   RenderQueue *renderQueue)
{
	Group *group = (Group *)self;
    M3Gbool enabled, success = M3G_TRUE;

    /* Check whether we're going up or down, and optimize the
     * rendering-enabled and visibility checking based on that */

    enabled = (self->enableBits & NODE_RENDER_BIT) != 0;
    if (caller != self->parent) {
        enabled = m3gHasEnabledPath(self, renderQueue->root);
        s->cullMask = CULLMASK_ALL;
    }
    M3G_ASSERT(!self->dirtyBits || !enabled);
    
	/* First do the child nodes, unless disabled (inheritable, so
     * children would be, too) */
    
    if (enabled && (group->numNonCullables > 0 || group->numRenderables > 0)) {
        
        Node *child = group->firstChild;
        if (child) {

            /* Check the bounding box if we have one */
            
#           if defined(M3G_ENABLE_VF_CULLING)
            if (group->bbox) {
                m3gValidateAABB(group->bbox);
                m3gUpdateCullingMask(s, renderQueue->camera, group->bbox);
            }
#           endif
            
            /* If we're not culled, or if we carry lights, we really
             * need to recurse into each child node */
            
            if (s->cullMask || group->numNonCullables > 0) {
                do {
                    if (child != caller) {
                        SetupRenderState cs;
                        cs.cullMask = s->cullMask;
                        
                        M3G_BEGIN_PROFILE(M3G_INTERFACE(group),
                                          M3G_PROFILE_SETUP_TRANSFORMS);
                        m3gGetCompositeNodeTransform(child, &cs.toCamera);
                        m3gPreMultiplyMatrix(&cs.toCamera, &s->toCamera);
                        M3G_END_PROFILE(M3G_INTERFACE(group),
                                        M3G_PROFILE_SETUP_TRANSFORMS);
                        
                        if (!M3G_VFUNC(Node, child, setupRender)(
                                child, self, &cs, renderQueue)) {
                            return M3G_FALSE;
                        }
                    }
                    child = child->right;
                } while (child != group->firstChild);
            }
            else {
                M3GInterface m3g = M3G_INTERFACE(group);
                M3Gint n = group->numRenderables;
                m3gIncStat(m3g, M3G_STAT_RENDER_NODES, n);
                m3gIncStat(m3g, M3G_STAT_RENDER_NODES_CULLED, n);
            }
        }
    }

	/* Then do the parent node if we're going up the tree.  Again, we
     * can discard the old traversal state at this point. */
    
	if (self != renderQueue->root) {
	    Node *parent = self->parent;
	    
	    if (parent != caller && parent != NULL) {
            Matrix t;

            M3G_BEGIN_PROFILE(M3G_INTERFACE(self), M3G_PROFILE_SETUP_TRANSFORMS);
            if (!m3gGetInverseNodeTransform(self, &t)) {
                return M3G_FALSE;
            }
			m3gMulMatrix(&s->toCamera, &t);
            M3G_END_PROFILE(M3G_INTERFACE(self), M3G_PROFILE_SETUP_TRANSFORMS);

            success = M3G_VFUNC(Node, parent, setupRender)(parent,
                                                           self,
                                                           s,
                                                           renderQueue);
	    }
	}

    return success;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self Group object
 * \param time current world time
 * \return minimum validity
 */
static M3Gint m3gGroupApplyAnimation(Object *self, M3Gint time)
{
    M3Gint validity, minValidity;
	Node *child;
    Group *group = (Group *)self;
    M3G_VALIDATE_OBJECT(group);

    minValidity = m3gObjectApplyAnimation(self, time);
    
    child = group->firstChild;
    if (child && minValidity > 0) {
        do {
            validity = M3G_VFUNC(Object, child, applyAnimation)(
                (Object *)child, time);
            minValidity = validity < minValidity ? validity : minValidity;
            child = child->right;
        } while (minValidity > 0 && child != group->firstChild);
    }
    return minValidity;
}

/*!
 * \internal
 * \brief Overloaded Node method.
 *
 * Call child nodes' ray intersect.
 *
 * \param self      Group object
 * \param mask      pick scope mask
 * \param ray       pick ray
 * \param ri        RayIntersection object
 * \param toGroup   transform to originating group
 * \retval          M3G_TRUE    continue pick
 * \retval          M3G_FALSE   abort pick
 */
static M3Gbool m3gGroupRayIntersect(Node *self,
                                    M3Gint mask,
                                    M3Gfloat *ray,
                                    RayIntersection *ri,
                                    Matrix *toGroup)
{
    Group *group = (Group *)self;
    Node *child;
    Matrix t, nt;

    m3gIdentityMatrix(&t);
    m3gIdentityMatrix(&nt);

    child = group->firstChild;
    if (child) {
        do {
            if (m3gHasPickablePath(child, ri->root)) {
                m3gCopyMatrix(&t, toGroup);
                m3gGetCompositeNodeTransform(child, &nt);
                m3gRightMulMatrix(&t, &nt);
                
                if (!M3G_VFUNC(Node, child, rayIntersect)(
                        child, mask, ray, ri, &t)) {
                    return M3G_FALSE;
                }
            }    
            child = child->right;
        } while (child != group->firstChild);
    }

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Initializes pick traversing.
 *
 * \param ri        RayIntersection object
 * \param root      Root node for the traversing
 * \param camera    Camera object used in pick (2D pick only)
 * \param x         viewport x (2D pick only)
 * \param y         viewport y (2D pick only)
 */
static void m3gInitPick(RayIntersection *ri, Node *root, Camera *camera, M3Gfloat x, M3Gfloat y)
{
    m3gZero(ri, sizeof(*ri));

    ri->root = root;
    ri->camera = camera;
    ri->x = x;
    ri->y = y;
    ri->tMin = M3G_MAX_POSITIVE_FLOAT;
}

/*!
 * \internal
 * \brief Fills Java side RayIntersection result.
 *
 * \param ri        RayIntersection object
 * \param ray       Ray used in pick
 * \param result    Java side float array
 */
static void m3gFillPickResult(RayIntersection *ri, M3Gfloat *ray, M3Gfloat *result)
{
    if (ri->intersected != NULL) {
        Vec3 n;

        /* Fill in the values */
        result[0] = ri->distance;
        result[1] = (M3Gfloat)ri->submeshIndex;
        result[2] = ri->textureS[0];
        result[3] = ri->textureS[1];
        result[4] = ri->textureT[0];
        result[5] = ri->textureT[1];

        /* Normalize normal */
        n.x = ri->normal[0];
        n.y = ri->normal[1];
        n.z = ri->normal[2];
        m3gNormalizeVec3(&n);

        result[6] = n.x;
        result[7] = n.y;
        result[8] = n.z;

        result[9] = ray[0];
        result[10] = ray[1];
        result[11] = ray[2];
        result[12] = m3gSub(ray[3], ray[0]);
        result[13] = m3gSub(ray[4], ray[1]);
        result[14] = m3gSub(ray[5], ray[2]);
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self Group object
 * \param references array of reference objects
 * \return number of references
 */
static M3Gint m3gGroupDoGetReferences(Object *self, Object **references)
{
    Group *group = (Group *)self;
    M3Gint num = m3gObjectDoGetReferences(self, references);
    Node *child = group->firstChild;
    if (child) {
        do {
            if (references != NULL)
                references[num] = (Object *)child;
            child = child->right;
            num++;
        } while (child != group->firstChild);
    }
    return num;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self Group object
 * \param references array of reference objects
 * \return number of references
 */
static Object *m3gGroupFindID(Object *self, M3Gint userID)
{
    Group *group = (Group *)self;
    Object *found = m3gObjectFindID(self, userID);
    
    Node *child = group->firstChild;
    if (child && !found) {
        do {
            found = m3gFindID((Object*) child, userID);
            child = child->right;
        } while (!found && child != group->firstChild);
    }
    return found;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original Group object
 * \param cloneObj pointer to cloned Group object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gGroupDuplicate(const Object *originalObj,
                                 Object **cloneObj,
                                 Object **pairs,
                                 M3Gint *numPairs)
{
    Node *child;
    Group *original = (Group *)originalObj;
    Group *clone;

    /* Create the clone object, unless already created in a derived
     * class function */
    
    if (*cloneObj == NULL) {
        clone = (Group *)m3gCreateGroup(originalObj->interface);
        if (!clone) {
            return M3G_FALSE; /* out of memory */
        }
        *cloneObj = (Object *)clone;
    }
    else {
        clone = (Group *)*cloneObj;
    }

    /* Call base class function to duplicate base class data */
    
    if (!m3gNodeDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        return M3G_FALSE; /* out of memory; caller will delete us */
    }

    /* Duplicate child nodes. */
    
    child = original->firstChild;
    if (child) {
        do {
            Node *temp = NULL;
            if (!M3G_VFUNC(Object, child, duplicate)(
                    (Object *)child, (Object**)&temp, pairs, numPairs)) {
                m3gDeleteObject((Object*) temp); /* we have the only reference */
                return M3G_FALSE;
            }
            m3gAddChild(clone, temp);
            child = child->right;
        } while (child != original->firstChild);
    }
    
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Overloaded Node method
 */
static M3Gint m3gGroupGetBBox(Node *self, AABB *bbox)
{
    Group *group = (Group*) self;
    
    /* Quick exit for empty volumes */
    
    if (!group->firstChild || !self->hasRenderables) {
        return 0;
    }

    /* Assume our existing bbox is ok, but compute a new one if it
     * isn't */
    
    if (group->bbox && !(self->dirtyBits & NODE_BBOX_BIT)) {
        *bbox = *group->bbox;
    }
    else {

        /* Compute local bounding box by recursively merging the
         * bounding boxes of all renderable child nodes */
    
        Node *child = group->firstChild;
        M3Gint groupYield = 0;
        
        do {
            if (child->hasRenderables && child->enableBits) {
                
                /* Get the transformation for the child node, then
                 * update our existing state with its bounding box */
                
                AABB childBBox;
                M3Gint childYield;
                Matrix t;
                
                childYield = m3gGetNodeBBox(child, &childBBox);
                if (childYield > 0) {
                    m3gGetCompositeNodeTransform(child, &t);
                    m3gTransformAABB(&childBBox, &t);
                    
                    if (groupYield) {
                        m3gFitAABB(bbox, &childBBox, bbox);
                    }
                    else {
                        *bbox = childBBox;
                    }
                    groupYield += childYield;
                }
            }
            child = child->right;
        } while (child != group->firstChild);
        
        /* Store the updated bbox locally if we have one, or return
         * the combined child yield factor if we don't */
        
        if (group->bbox) {
            *group->bbox = *bbox;
        }
        else {
            return (groupYield > 0) ? groupYield + VFC_NODE_OVERHEAD : 0;
        }
    }
    return VFC_BBOX_COST + VFC_NODE_OVERHEAD;
}

/*!
 * \internal
 * \brief Overloaded Node method
 */
static M3Gbool m3gGroupValidate(Node *self, M3Gbitmask stateBits, M3Gint scope)
{
    Group *group = (Group*) self;

    if (stateBits & self->enableBits) {
        
        /* First validate child nodes to ensure we don't skip anything,
         * and allow children to invalidate our state */
    
        Node *child = group->firstChild;
        if (child) {
            do {
                if (!m3gValidateNode(child, stateBits, scope)) {
                    return M3G_FALSE;
                }
                child = child->right;
            } while (child != group->firstChild);
        }

        /* Re-evaluate our local bounding box if necessary */

        if (self->hasRenderables && self->dirtyBits & NODE_BBOX_BIT) {
            AABB bbox;
            M3Gint yield;
            M3G_BEGIN_PROFILE(M3G_INTERFACE(self), M3G_PROFILE_VFC_UPDATE);
            
            yield = m3gGetNodeBBox(self, &bbox);
        
            /* Think about adding a bounding box if we don't yet have one,
             * or removing the current one if it doesn't seem worth it */

            if (!group->bbox) {
                if (yield > (3*VFC_BBOX_COST) >> 1) {
                    group->bbox = m3gAlloc(M3G_INTERFACE(group),
                                           sizeof(*group->bbox));
                    if (group->bbox) {
                        m3gIncStat(M3G_INTERFACE(group),
                                   M3G_STAT_BOUNDING_BOXES, 1);
                        *group->bbox = bbox;
                    }
                    else {
                        return M3G_FALSE;
                    }
                }
            }
            else if (yield <= VFC_BBOX_COST) {
                m3gFree(M3G_INTERFACE(group), group->bbox);
                group->bbox = NULL;
                m3gIncStat(M3G_INTERFACE(group), M3G_STAT_BOUNDING_BOXES, -1);
            }
            M3G_END_PROFILE(M3G_INTERFACE(self), M3G_PROFILE_VFC_UPDATE);
        }
        return m3gNodeValidate(self, stateBits, scope);
    }
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Overloaded Node method.
 *
 * \param self Group object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static void m3gGroupUpdateDuplicateReferences(Node *self, Object **pairs, M3Gint numPairs)
{
    Group *group = (Group *)self;
    Node *child = group->firstChild;
    
    m3gNodeUpdateDuplicateReferences(self, pairs, numPairs);
    
    if (child) {
        do {
            M3G_VFUNC(Node, child, updateDuplicateReferences)(
                child, pairs, numPairs);
            child = child->right;
        } while (child != group->firstChild);
    }
}

/*!
 * \internal
 * \brief Initializes a Group object. See specification
 * for default values.
 *
 * \param m3g           M3G interface
 * \param group         Group object
 * \param vfTable       virtual function table
 */
static void m3gInitGroup(Interface *m3g, Group *group, M3GClass classID)
{
	/* Group is derived from Node */
	m3gInitNode(m3g, &group->node, classID);
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const NodeVFTable m3gvf_Group = {
    {
        {
            m3gGroupApplyAnimation,
            m3gNodeIsCompatible,
            m3gNodeUpdateProperty,
            m3gGroupDoGetReferences,
            m3gGroupFindID,
            m3gGroupDuplicate,
            m3gDestroyGroup
        }
    },
    m3gGroupAlign,
    NULL, /* pure virtual m3gNodeDoRender */
    m3gGroupGetBBox,
    m3gGroupRayIntersect,
    m3gGroupSetupRender,
    m3gGroupUpdateDuplicateReferences,
    m3gGroupValidate
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a Group object.
 *
 * \param interface     M3G interface
 * \retval Group new Group object
 * \retval NULL Group creating failed
 */
M3G_API M3GGroup m3gCreateGroup(M3GInterface interface)
{
    Interface *m3g = (Interface *) interface;
    M3G_VALIDATE_INTERFACE(m3g);

	{
		Group *group =  m3gAllocZ(m3g, sizeof(Group));
	
        if (group != NULL) {
    		m3gInitGroup(m3g, group, M3G_CLASS_GROUP);
        }

		return (M3GGroup) group;
	}
}

/*!
 * \brief Adds a node to this group.
 *
 * \param handle        Group object
 * \param hNode         Node object
 */
M3G_API void m3gAddChild(M3GGroup handle, M3GNode hNode)
{
    Group *group = (Group *) handle;
	Node *child = (Node *) hNode;
    
    M3G_VALIDATE_OBJECT(group);

    if (child == NULL) {
        m3gRaiseError(M3G_INTERFACE(group), M3G_NULL_POINTER);
        return;
    }

    if (child == (Node *)group   ||
        (child->parent != NULL && child->parent != (Node *)group) ||
        m3gIsChildOf(child, (Node *)group) ||
        m3gGetClass((Object *) child) == M3G_CLASS_WORLD) {
        m3gRaiseError(M3G_INTERFACE(group), M3G_INVALID_VALUE);
        return;
    }

    if (child->parent == NULL) {
        m3gLinkChild(child, group);
    }
}

/*!
 * \brief Removes a node from this group.
 *
 * \param handle        Group object
 * \param hNode         Node object
 */
M3G_API void m3gRemoveChild(M3GGroup handle, M3GNode hNode)
{
    Group *group = (Group *) handle;
	Node *child = (Node *)hNode;
    M3G_VALIDATE_OBJECT(group);

    if (child == NULL) {
        return;
    }

    if (child->hasBones == M3G_TRUE) {
        m3gRaiseError(M3G_INTERFACE(group), M3G_INVALID_VALUE);
        return;
    }
    
    if (group->firstChild == NULL) {
        return;
    }

    m3gDetachChild(child, group);
}

/*!
 * \brief Performs 3D pick.
 *
 * \param handle        Group object
 * \param mask          pick scope mask
 * \param ray           pick ray
 * \arg ray[0]          origin X
 * \arg ray[1]          origin Y
 * \arg ray[2]          origin Z
 * \arg ray[3]          direction X
 * \arg ray[4]          direction Y
 * \arg ray[5]          direction Z
 * \param result        java side RayIntersection result
 * \arg result[0]       distance
 * \arg result[1]       submesh index
 * \arg result[2]       textureS[0]
 * \arg result[3]       textureS[1]
 * \arg result[4]       textureT[0]
 * \arg result[5]       textureT[1]
 * \arg result[6]       normal X
 * \arg result[7]       normal Y
 * \arg result[8]       normal Z
 * \arg result[9]       ray ox
 * \arg result[10]      ray oy
 * \arg result[11]      ray oz
 * \arg result[12]      ray dx
 * \arg result[13]      ray dy
 * \arg result[14]      ray dz
 * \return              intersected Node object
 */

#ifdef M3G_ENABLE_PROFILING
static M3GNode m3gPick3DInternal(M3GGroup handle,
                          M3Gint mask,
                          M3Gfloat *ray,
                          M3Gfloat *result);

M3G_API M3GNode m3gPick3D(M3GGroup handle,
                          M3Gint mask,
                          M3Gfloat *ray,
                          M3Gfloat *result)
{
    M3GNode pickResult;
    M3G_BEGIN_PROFILE(M3G_INTERFACE(handle), M3G_PROFILE_PICK);
    pickResult = m3gPick3DInternal(handle, mask, ray, result);
    M3G_END_PROFILE(M3G_INTERFACE(handle), M3G_PROFILE_PICK);
    return pickResult;
}

static M3GNode m3gPick3DInternal(M3GGroup handle,
                          M3Gint mask,
                          M3Gfloat *ray,
                          M3Gfloat *result)
#else
M3G_API M3GNode m3gPick3D(M3GGroup handle,
                          M3Gint mask,
                          M3Gfloat *ray,
                          M3Gfloat *result)

#endif
{
    RayIntersection ri;
    Matrix toGroup;
    Group *group = (Group *) handle;
    M3G_VALIDATE_OBJECT(group);

    M3G_LOG1(M3G_LOG_STAGES, "Picking group 0x%08X\n", (unsigned) group);
    
    /* Check for errors */
    if (ray[3] == 0 && ray[4] == 0 && ray[5] == 0) {
        m3gRaiseError(M3G_INTERFACE(group), M3G_INVALID_VALUE);
        return NULL;
    }
    if (!m3gValidateNode((Node*) group, NODE_PICK_BIT, mask)) {
        return NULL;
    }
        
    m3gInitPick(&ri, (Node *)group, NULL, 0, 0);
    m3gIdentityMatrix(&toGroup);

    ray[3] = m3gAdd(ray[3], ray[0]);
    ray[4] = m3gAdd(ray[4], ray[1]);
    ray[5] = m3gAdd(ray[5], ray[2]);

    M3G_VFUNC(Node, group, rayIntersect)(   (Node *)group,
                                            mask,
                                            ray,
                                            &ri,
                                            &toGroup);
    m3gFillPickResult(&ri, ray, result);
    return ri.intersected;
}

/*!
 * \brief Performs 2D pick.
 *
 * \param handle        Group object
 * \param mask          pick scope mask
 * \param x             viewport x
 * \param y             viewport y
 * \param hCamera       Camera object
 * \param result        java side RayIntersection result, see m3gPick3D
 * \return              intersected Node object
 */

#ifdef M3G_ENABLE_PROFILING
static M3GNode m3gPick2DInternal(M3GGroup handle,
                          M3Gint mask,
                          M3Gfloat x, M3Gfloat y,
                          M3GCamera hCamera,
                          M3Gfloat *result);

M3G_API M3GNode m3gPick2D(M3GGroup handle,
                          M3Gint mask,
                          M3Gfloat x, M3Gfloat y,
                          M3GCamera hCamera,
                          M3Gfloat *result)
{
    M3GNode pickResult;
    M3G_BEGIN_PROFILE(M3G_INTERFACE(handle), M3G_PROFILE_PICK);
    pickResult = m3gPick2DInternal(handle, mask, x, y, hCamera, result);
    M3G_END_PROFILE(M3G_INTERFACE(handle), M3G_PROFILE_PICK);
    return pickResult;
}

static M3GNode m3gPick2DInternal(M3GGroup handle,
                          M3Gint mask,
                          M3Gfloat x, M3Gfloat y,
                          M3GCamera hCamera,
                          M3Gfloat *result)
#else
M3G_API M3GNode m3gPick2D(M3GGroup handle,
                          M3Gint mask,
                          M3Gfloat x, M3Gfloat y,
                          M3GCamera hCamera,
                          M3Gfloat *result)
#endif
{
    Vec4 farp, nearp;
    RayIntersection ri;
    Matrix toGroup;
    M3Gfloat ray[6 + 2];    /* Extra floats to store near and far plane z */
    Node *root;
    Group *group = (Group *) handle;    

    M3G_LOG2(M3G_LOG_STAGES, "Picking group 0x%08X via camera 0x%08X\n",
             (unsigned) group, (unsigned) hCamera);
    
    M3G_VALIDATE_OBJECT(group);

    if (hCamera == 0) {
        m3gRaiseError(M3G_INTERFACE(group), M3G_NULL_POINTER);
        return NULL;
    }

    root = m3gGetRoot((Node *)hCamera);

    if (root != m3gGetRoot(&group->node)) {
        m3gRaiseError(M3G_INTERFACE(group), M3G_INVALID_OPERATION);
        return NULL;
    }
    if (!m3gValidateNode(root, NODE_PICK_BIT, mask)) {
        return NULL;
    }

    farp.x = m3gSub(m3gMul(2, x), 1.f);
    farp.y = m3gSub(1.f, m3gMul(2, y));
    farp.z = 1.f;
    farp.w = 1.f;

    nearp.x = farp.x;
    nearp.y = farp.y;
    nearp.z = -1.f;
    nearp.w =  1.f;

    m3gCopyMatrix(&toGroup, m3gProjectionMatrix((Camera *)hCamera));

    M3G_BEGIN_PROFILE(M3G_INTERFACE(group), M3G_PROFILE_TRANSFORM_INVERT);
    if (!m3gInvertMatrix(&toGroup)) {
        m3gRaiseError(M3G_INTERFACE(group), M3G_ARITHMETIC_ERROR);
        return NULL;
    }
    M3G_END_PROFILE(M3G_INTERFACE(group), M3G_PROFILE_TRANSFORM_INVERT);

    m3gTransformVec4(&toGroup, &nearp);
    m3gTransformVec4(&toGroup, &farp);

    m3gScaleVec4(&nearp, m3gRcp(nearp.w));
    m3gScaleVec4(&farp, m3gRcp(farp.w));

    /* Store near and far plane z for sprite picking */
    ray[6] = nearp.z;
    ray[7] = farp.z;

    if (!m3gGetTransformTo((M3GNode) hCamera, (Node *) group, &toGroup)) {
        return NULL;
    }

    m3gTransformVec4(&toGroup, &nearp);
    m3gTransformVec4(&toGroup, &farp);

    m3gScaleVec4(&nearp, m3gRcp(nearp.w));
    m3gScaleVec4(&farp, m3gRcp(farp.w));

    ray[0] = nearp.x;
    ray[1] = nearp.y;
    ray[2] = nearp.z;
    ray[3] = farp.x;
    ray[4] = farp.y;
    ray[5] = farp.z;


    m3gInitPick(&ri, (Node *)group, (Camera *)hCamera, x, y);
    m3gIdentityMatrix(&toGroup);

    M3G_VFUNC(Node, group, rayIntersect)((Node *)group, mask, ray, &ri, &toGroup);

    m3gFillPickResult(&ri, ray, result);
    return ri.intersected;
}

/*!
 * \brief Gets a child.
 *
 * \param handle        Group object
 * \param idx           child index
 * \return              Node object
 */
M3G_API M3GNode m3gGetChild(M3GGroup handle, M3Gint idx)
{
    Node *n;
    Group *group = (Group *) handle;
    M3G_VALIDATE_OBJECT(group);

    if (idx < 0) {
        goto InvalidIndex;
    }

	n = group->firstChild;

    while (idx-- > 0) {
        n = n->right;
        if (n == group->firstChild) {
            goto InvalidIndex;
        }
    }
    return n;

    InvalidIndex:
    m3gRaiseError(M3G_INTERFACE(group), M3G_INVALID_INDEX);
    return NULL;
}

/*!
 * \brief Gets children count.
 *
 * \param handle        Group object
 * \return              children count
 */
M3G_API M3Gint m3gGetChildCount(M3GGroup handle)
{
    Group *group = (Group *) handle;
    M3G_VALIDATE_OBJECT(group);
    {
        M3Gint count = 0;
        const Node *child = group->firstChild;
        if (child) {
            do {
                ++count;
                child = child->right;
            } while (child != group->firstChild);
        }
        return count;
    }
}

