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
* Description: Mesh implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Mesh implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_mesh.h"
#include "m3g_memory.h"

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Destroys this Mesh object.
 *
 * \param obj Mesh object
 */
static void m3gDestroyMesh(Object *obj)
{
    M3Gint i;
    Mesh *mesh = (Mesh *) obj;
    M3G_VALIDATE_OBJECT(mesh);

    for (i = 0; i < mesh->trianglePatchCount; ++i) {
        M3G_ASSIGN_REF(mesh->indexBuffers[i], NULL);
        M3G_ASSIGN_REF(mesh->appearances[i], NULL);
    }
    M3G_ASSIGN_REF(mesh->vertexBuffer, NULL);

	{
		Interface *m3g = M3G_INTERFACE(mesh);
		m3gFree(m3g, mesh->indexBuffers);
		m3gFree(m3g, mesh->appearances);
	}

    m3gIncStat(M3G_INTERFACE(obj), M3G_STAT_RENDERABLES, -1);
    
    m3gDestroyNode(obj);
}

/*!
 * \internal
 * \brief Insert a mesh into a rendering queue
 */
static M3Gbool m3gQueueMesh(Mesh *mesh, const Matrix *toCamera,
                            RenderQueue *renderQueue)
{
    M3Gint i;

    /* Fetch the cumulative alpha factor for this node */
    
    mesh->totalAlphaFactor =
        (M3Gushort) m3gGetTotalAlphaFactor((Node*) mesh, renderQueue->root);
        
    /* Insert each submesh into the rendering queue */
            
    for (i = 0; i < mesh->trianglePatchCount; i++) {
        if (mesh->appearances[i] != NULL) {
            if (!m3gInsertDrawable(M3G_INTERFACE(mesh),
                                   renderQueue,
                                   (Node*) mesh,
                                   toCamera,
                                   i,
                                   m3gGetAppearanceSortKey(mesh->appearances[i])))
                return M3G_FALSE;
        }
    }
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Overloaded Node method.
 *
 * Setup mesh rendering by adding all submeshes to
 * the render queue.
 *
 * \param self Mesh object
 * \param toCamera transform to camera
 * \param alphaFactor total alpha factor
 * \param caller caller node
 * \param renderQueue RenderQueue
 *
 * \retval M3G_TRUE continue render setup
 * \retval M3G_FALSE abort render setup
 */
static M3Gbool m3gMeshSetupRender(Node *self,
                                  const Node *caller,
                                  SetupRenderState *s,
                                  RenderQueue *renderQueue)
{
	Mesh *mesh = (Mesh *)self;
    M3G_UNREF(caller);
    m3gIncStat(M3G_INTERFACE(self), M3G_STAT_RENDER_NODES, 1);
    
	if ((self->enableBits & NODE_RENDER_BIT) != 0 &&
        (self->scope & renderQueue->scope) != 0) {

        /* Check view frustum culling */
        
#       if defined(M3G_ENABLE_VF_CULLING)
        AABB bbox;
        m3gGetBoundingBox(mesh->vertexBuffer, &bbox);
        m3gUpdateCullingMask(s, renderQueue->camera, &bbox);
        if (s->cullMask == 0) {
            m3gIncStat(M3G_INTERFACE(self),
                       M3G_STAT_RENDER_NODES_CULLED, 1);
            return M3G_TRUE;
        }
#       endif

        /* No dice, let's render... */

        return m3gQueueMesh(mesh, &s->toCamera, renderQueue);
    }
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Overloaded Node method.
 *
 * Renders one submesh.
 *
 * \param self Mesh object
 * \param ctx current render context
 * \param patchIndex submesh index
 */
static void m3gMeshDoRender(Node *self,
                            RenderContext *ctx,
                            const Matrix *toCamera,
                            M3Gint patchIndex)
{
    Mesh *mesh = (Mesh *)self;

	m3gDrawMesh(ctx,
                mesh->vertexBuffer,
                mesh->indexBuffers[patchIndex],
                mesh->appearances[patchIndex],
                toCamera,
                mesh->totalAlphaFactor + 1,
                self->scope);
}

/*!
 * \internal
 * \brief Internal equivalent routine called
 * by m3gMeshRayIntersect.
 *
 * \param mesh      Mesh object
 * \param vertices  VertexBuffer object used in calculations
 * \param mask      pick scope mask
 * \param ray       pick ray
 * \param ri        RayIntersection object
 * \param toGroup   transform to originating group
 * \retval          M3G_TRUE    continue pick
 * \retval          M3G_FALSE   abort pick
 */
static M3Gbool m3gMeshRayIntersectInternal(	Mesh *mesh,
                                            VertexBuffer *vertices,
            								M3Gint mask,
            								M3Gfloat *ray,
            								RayIntersection *ri,
            								Matrix *toGroup)
{
    Vec3 v0, v1, v2, tuv;
    Vec4 transformed, p0, p1;
    M3Gint indices[4] = { 0, 0, 0, 0 }; 
    M3Gint i, j, k, cullMode;
    Matrix t;   /* Reused as texture transform */

    if (vertices == NULL ||
         mesh->appearances == NULL ||
         mesh->indexBuffers == NULL ||
         (((Node *)mesh)->scope & mask) == 0) {
        return M3G_TRUE;
    }

    if (vertices->vertices == NULL) {
        m3gRaiseError(M3G_INTERFACE(mesh), M3G_INVALID_OPERATION);
        return M3G_FALSE;
    }

    p0.x = ray[0];
    p0.y = ray[1];
    p0.z = ray[2];
    p0.w = 1.f;

    p1.x = ray[3];
    p1.y = ray[4];
    p1.z = ray[5];
    p1.w = 1.f;

    m3gCopyMatrix(&t, toGroup);
    M3G_BEGIN_PROFILE(M3G_INTERFACE(mesh), M3G_PROFILE_TRANSFORM_INVERT);
    if (!m3gInvertMatrix(&t)) {
        m3gRaiseError(M3G_INTERFACE(mesh), M3G_ARITHMETIC_ERROR);
        return M3G_FALSE;
    }
    M3G_END_PROFILE(M3G_INTERFACE(mesh), M3G_PROFILE_TRANSFORM_INVERT);
    m3gTransformVec4(&t, &p0);
    m3gTransformVec4(&t, &p1);
    
    m3gScaleVec3((Vec3*) &p0, m3gRcp(p0.w));
    m3gScaleVec3((Vec3*) &p1, m3gRcp(p1.w));

    m3gSubVec4(&p1, &p0);

    /* Quick bounding box test for Meshes */
    if (m3gGetClass((Object *)mesh) == M3G_CLASS_MESH) {
        AABB boundingBox;
        m3gGetBoundingBox(vertices, &boundingBox);

        if (!m3gIntersectBox((Vec3*) &p0, (Vec3*) &p1, &boundingBox)) {
            return M3G_TRUE;
        }
    }

    /* Apply the inverse of the vertex scale and bias to the ray */
    
    if (!IS_ZERO(vertices->vertexScale)) {
        const Vec3 *bias = (const Vec3*) vertices->vertexBias;
        M3Gfloat ooScale = m3gRcp(vertices->vertexScale);
        m3gSubVec3((Vec3*) &p0, bias);
        m3gScaleVec3((Vec3*) &p0, ooScale);
        m3gScaleVec3((Vec3*) &p1, ooScale); /* direction vector -> no bias */
    }
    else {
        m3gRaiseError(M3G_INTERFACE(mesh), M3G_ARITHMETIC_ERROR);
        return M3G_FALSE;
    }
    
    /* Go through all submeshes */
    for (i = 0; i < mesh->trianglePatchCount; i++) {
        /* Do not pick submeshes with null appearance */
        if (mesh->appearances[i] == NULL ||
            mesh->indexBuffers[i] == NULL) continue;

        /* Validate indices versus vertex buffer */
        if (m3gGetMaxIndex(mesh->indexBuffers[i]) >= m3gGetNumVertices(vertices)) {
            m3gRaiseError(M3G_INTERFACE(mesh), M3G_INVALID_OPERATION);
            return M3G_FALSE;
        }

        if (mesh->appearances[i]->polygonMode != NULL) {
            cullMode = m3gGetWinding(mesh->appearances[i]->polygonMode) == M3G_WINDING_CCW ? 0 : 1;
            switch(m3gGetCulling(mesh->appearances[i]->polygonMode)) {
            case M3G_CULL_FRONT: cullMode ^= 1; break;
            case M3G_CULL_NONE:  cullMode  = 2; break;
            }
        }
        else {
            cullMode = 0;
        }

        /* Go through all triangels */
        for (j = 0; m3gGetIndices(mesh->indexBuffers[i], j, indices); j++) {
            /* Ignore zero area triangles */
            if ( indices[0] == indices[1] ||
                 indices[0] == indices[2] ||
                 indices[1] == indices[2]) continue;

            m3gGetVertex(vertices, indices[0], &v0);
            m3gGetVertex(vertices, indices[1], &v1);
            m3gGetVertex(vertices, indices[2], &v2);

            if (m3gIntersectTriangle((Vec3*)&p0, (Vec3*)&p1, &v0, &v1, &v2, &tuv, indices[3] ^ cullMode)) {
                /* Check that we are going to fill this intersection */
                if (tuv.x <= 0.f || tuv.x >= ri->tMin) continue;

                /* Fill in to RayIntersection */
                ri->tMin = tuv.x;
                ri->distance = tuv.x;
                ri->submeshIndex = i;
                ri->intersected = (Node *)mesh;

                /* Fetch normal */
                if (m3gGetNormal(vertices, indices[0], &v0)) {
                    m3gGetNormal(vertices, indices[1], &v1);
                    m3gGetNormal(vertices, indices[2], &v2);

                    ri->normal[0] = m3gAdd(
                        m3gMul(v0.x, m3gSub(1.f, m3gAdd(tuv.y, tuv.z))),
                        m3gAdd(
                            m3gMul(v1.x, tuv.y),
                            m3gMul(v2.x, tuv.z)));
                    
                    ri->normal[1] = m3gAdd(
                        m3gMul(v0.y, m3gSub(1.f, m3gAdd(tuv.y, tuv.z))),
                        m3gAdd(
                            m3gMul(v1.y, tuv.y),
                            m3gMul(v2.y, tuv.z)));
                    
                    ri->normal[2] = m3gAdd(
                        m3gMul(v0.z, m3gSub(1.f, m3gAdd(tuv.y, tuv.z))),
                        m3gAdd(
                            m3gMul(v1.z, tuv.y),
                            m3gMul(v2.z, tuv.z)));
                }
                else {
                    ri->normal[0] = 0.f;
                    ri->normal[1] = 0.f;
                    ri->normal[2] = 1.f;
                }
            
                /* Fetch texture coordinates for each unit */
                for (k = 0; k < M3G_NUM_TEXTURE_UNITS; k++) {
                    if (m3gGetTexCoord(vertices, indices[0], k, &v0)) {
                        m3gGetTexCoord(vertices, indices[1], k, &v1);
                        m3gGetTexCoord(vertices, indices[2], k, &v2);

                        /* Calculate transformed S and T */
                        transformed.x = m3gAdd(
                            m3gMul(v0.x, m3gSub(1.f, m3gAdd(tuv.y, tuv.z))),
                            m3gAdd(
                                m3gMul(v1.x, tuv.y),
                                m3gMul(v2.x, tuv.z)));
                        
                        transformed.y = m3gAdd(
                            m3gMul(v0.y, m3gSub(1.f, m3gAdd(tuv.y, tuv.z))),
                            m3gAdd(
                                m3gMul(v1.y, tuv.y),
                                m3gMul(v2.y, tuv.z)));
                        
                        transformed.z = 0;
                        transformed.w = 1;

                        /* Transform and * 1/w */
                        if (mesh->appearances[i]->texture[k] != NULL) {
                            m3gGetCompositeTransform((Transformable *)mesh->appearances[i]->texture[k], &t);
                            m3gTransformVec4(&t, &transformed);
                            m3gScaleVec4(&transformed, m3gRcp(transformed.w));
                        }

                        ri->textureS[k] = transformed.x;
                        ri->textureT[k] = transformed.y;
                    }
                    else {
                        ri->textureS[k] = 0.f;
                        ri->textureT[k] = 0.f;
                    }
                }
            }
        }
    }

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Overloaded Node method.
 *
 * Just forward call internal ray intersect.
 *
 * \param self      Mesh object
 * \param mask      pick scope mask
 * \param ray       pick ray
 * \param ri        RayIntersection object
 * \param toGroup   transform to originating group
 * \retval          M3G_TRUE    continue pick
 * \retval          M3G_FALSE   abort pick
 */
static M3Gbool m3gMeshRayIntersect( Node *self,
    								M3Gint mask,
    								M3Gfloat *ray,
    								RayIntersection *ri,
    								Matrix *toGroup)
{
    Mesh *mesh = (Mesh *)self;
    return m3gMeshRayIntersectInternal(mesh, mesh->vertexBuffer, mask, ray, ri, toGroup);
}

/*!
 * \internal
 * \brief Initializes a Mesh object. See specification
 * for default values.
 *
 * \param m3g                   M3G interface
 * \param mesh                  Mesh object
 * \param hVertices             VertexBuffer object
 * \param hTriangles            array of IndexBuffer objects
 * \param hAppearances          array of Appearance objects
 * \param trianglePatchCount    number of submeshes
 * \param vfTable               virtual function table
 * \retval                      M3G_TRUE success
 * \retval                      M3G_FALSE failed
 */
static M3Gbool m3gInitMesh(Interface *m3g,
                           Mesh *mesh,
                           M3GVertexBuffer hVertices,
                           M3GIndexBuffer *hTriangles,
                           M3GAppearance *hAppearances,
                           M3Gint trianglePatchCount,
                           M3GClass classID)
{
    M3Gint i;
	
    /* Out of memory if more than 65535 triangle patches */
    if (trianglePatchCount > 65535) {
        m3gRaiseError(m3g, M3G_OUT_OF_MEMORY);
        return M3G_FALSE;
    }

	for (i = 0; i < trianglePatchCount; i++) {
		if (hTriangles[i] == NULL) {
			m3gRaiseError(m3g, M3G_NULL_POINTER);
            return M3G_FALSE;
		}
	}

	mesh->indexBuffers =
        m3gAllocZ(m3g, sizeof(IndexBuffer *) * trianglePatchCount);
	if (!mesh->indexBuffers) {
		return M3G_FALSE;
	}

	mesh->appearances =
        m3gAllocZ(m3g, sizeof(Appearance *) * trianglePatchCount);
	if (!mesh->appearances) {
		m3gFree(m3g, mesh->indexBuffers);
		return M3G_FALSE;
	}

	/* Mesh is derived from node */
	m3gInitNode(m3g, &mesh->node, classID);
    mesh->node.hasRenderables = M3G_TRUE;
    mesh->node.dirtyBits |= NODE_BBOX_BIT;

    for (i = 0; i < trianglePatchCount; i++) {
        M3G_ASSIGN_REF(mesh->indexBuffers[i], hTriangles[i]);
    }
	
	if (hAppearances != NULL) {
        for (i = 0; i < trianglePatchCount; i++) {
            M3G_ASSIGN_REF(mesh->appearances[i], hAppearances[i]);
        }
	}
	else {
		m3gZero(mesh->appearances, sizeof(Appearance *) * trianglePatchCount);
    }
	
    M3G_ASSIGN_REF(mesh->vertexBuffer, hVertices);
	mesh->trianglePatchCount = (M3Gshort) trianglePatchCount;

    m3gIncStat(M3G_INTERFACE(mesh), M3G_STAT_RENDERABLES, 1);
    
	return M3G_TRUE;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self Mesh object
 * \param references array of reference objects
 * \return number of references
 */
static M3Gint m3gMeshDoGetReferences(Object *self, Object **references)
{
    Mesh *mesh = (Mesh *)self;
    M3Gint i, num = m3gObjectDoGetReferences(self, references);
    if (references != NULL)
        references[num] = (Object *)mesh->vertexBuffer;
    num++;
    for (i = 0; i < mesh->trianglePatchCount; i++) {
        if (mesh->indexBuffers[i] != NULL) {
            if (references != NULL)
                references[num] = (Object *)mesh->indexBuffers[i];
            num++;
        }
        if (mesh->appearances[i] != NULL) {
            if (references != NULL)
                references[num] = (Object *)mesh->appearances[i];
            num++;
        }
    }
    return num;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 */
static Object *m3gMeshFindID(Object *self, M3Gint userID)
{
    int i;
    Mesh *mesh = (Mesh *)self;
    Object *found = m3gObjectFindID(self, userID);

    if (!found) {
        found = m3gFindID((Object*) mesh->vertexBuffer, userID);
    }    
    for (i = 0; !found && i < mesh->trianglePatchCount; ++i) {
        if (mesh->indexBuffers[i] != NULL) {
            found = m3gFindID((Object*) mesh->indexBuffers[i], userID);
        }
        if (!found && mesh->appearances[i] != NULL) {
            found = m3gFindID((Object*) mesh->appearances[i], userID);
        }
    }
    return found;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original Mesh object
 * \param cloneObj pointer to cloned Mesh object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gMeshDuplicate(const Object *originalObj,
                                Object **cloneObj,
                                Object **pairs,
                                M3Gint *numPairs)
{
    /* Create the clone if it doesn't exist; otherwise we'll be all
     * set by the derived class(es) and can just call through to the
     * base class */
    
    if (*cloneObj == NULL) {
        Mesh *original = (Mesh *)originalObj;
        Mesh *clone = (Mesh *)m3gCreateMesh(originalObj->interface,
                                            original->vertexBuffer,
                                            original->indexBuffers,
                                            original->appearances,
                                            original->trianglePatchCount);
        *cloneObj = (Object *)clone;
        if (*cloneObj == NULL) {
            return M3G_FALSE;
        }
    }
    
    return m3gNodeDuplicate(originalObj, cloneObj, pairs, numPairs);
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self Mesh object
 * \param time current world time
 * \return minimum validity
 */
static M3Gint m3gMeshApplyAnimation(Object *self, M3Gint time)
{
    M3Gint validity, minValidity;
    Mesh *mesh = (Mesh *)self;
    Object *vb;
    M3G_VALIDATE_OBJECT(mesh);

    minValidity = m3gObjectApplyAnimation(self, time);

    vb = (Object *) mesh->vertexBuffer;

    if (vb != NULL && minValidity > 0) {
        validity = M3G_VFUNC(Object, vb, applyAnimation)(vb, time);
        minValidity = M3G_MIN(validity, minValidity);
    }
    
    if (mesh->appearances != NULL) {
        Object *app;
        M3Gint i, n;
        n = mesh->trianglePatchCount;
        
        for (i = 0; i < n && minValidity > 0; ++i) {
            app = (Object *) mesh->appearances[i];
            if (app != NULL) {
                validity = M3G_VFUNC(Object, app, applyAnimation)(app, time);
                minValidity = M3G_MIN(validity, minValidity);
            }
        }
    }

    return minValidity;
}

/*!
 * \internal
 * \brief Overloaded Node method
 */
static M3Gint m3gMeshGetBBox(Node *self, AABB *bbox)
{
    Mesh *mesh = (Mesh *) self;
    VertexBuffer *vb = mesh->vertexBuffer;

    if (vb->vertices) {
        m3gGetBoundingBox(vb, bbox);
        return VFC_BBOX_COST + VFC_NODE_OVERHEAD;
    }
    else {
        return 0;
    }
}

/*!
 * \internal
 * \brief Overloaded Node method
 */
static M3Gbool m3gMeshValidate(Node *self, M3Gbitmask stateBits, M3Gint scope)
{
    Mesh *mesh = (Mesh *) self;
    VertexBuffer *vb = mesh->vertexBuffer;
    int i;

    if ((scope & self->scope) != 0) {
        if (stateBits & self->enableBits) {
            
            /* Validate vertex buffer components */
            
            for (i = 0; i < mesh->trianglePatchCount; ++i) {
                Appearance *app = mesh->appearances[i];
                if (app) {
                    if (!m3gValidateVertexBuffer(
                            vb, app, m3gGetMaxIndex(mesh->indexBuffers[i]))) {
                        m3gRaiseError(M3G_INTERFACE(mesh), M3G_INVALID_OPERATION);
                        return M3G_FALSE;
                    }
                }
            }
    
            /* Invalidate cached vertex stuff if source buffer changed */
            {
                M3Gint vbTimestamp = m3gGetTimestamp(vb);
                if (mesh->vbTimestamp != vbTimestamp) {
                    m3gInvalidateNode(self, NODE_BBOX_BIT);
                    mesh->vbTimestamp = vbTimestamp;
                }
            }
            
            return m3gNodeValidate(self, stateBits, scope);
        }
    }
    return M3G_TRUE;
}

#if 0
/*!
 * \internal
 * \brief Computes the estimated rendering cost for this Mesh node
 */
static M3Gint m3gMeshRenderingCost(const Mesh *mesh)
{
    /* Since we're using strips, just assume that each vertex
     * generates a new triangle... */
    
    return
        mesh->vertexBuffer->vertexCount * (VFC_VERTEX_COST +
                                           VFC_TRIANGLE_COST) +
        mesh->trianglePatchCount * VFC_RENDERCALL_OVERHEAD +
        VFC_NODE_OVERHEAD;
}
#endif

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const NodeVFTable m3gvf_Mesh = {
    {
        {
            m3gMeshApplyAnimation,
            m3gNodeIsCompatible,
            m3gNodeUpdateProperty,
            m3gMeshDoGetReferences,
            m3gMeshFindID,
            m3gMeshDuplicate,
            m3gDestroyMesh
        }
    },
    m3gNodeAlign,
    m3gMeshDoRender,
    m3gMeshGetBBox,
    m3gMeshRayIntersect,
    m3gMeshSetupRender,
    m3gNodeUpdateDuplicateReferences,
    m3gMeshValidate
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a Mesh object.
 *
 * \param interface             M3G interface
 * \param hVertices             VertexBuffer object
 * \param hTriangles            array of IndexBuffer objects
 * \param hAppearances          array of Appearance objects
 * \param trianglePatchCount    number of submeshes
 * \retval Mesh new Mesh object
 * \retval NULL Mesh creating failed
 */
M3G_API M3GMesh m3gCreateMesh(M3GInterface interface,
                              M3GVertexBuffer hVertices,
                              M3GIndexBuffer *hTriangles,
                              M3GAppearance *hAppearances,
                              M3Gint trianglePatchCount)
{
    Interface *m3g = (Interface *) interface;
    M3G_VALIDATE_INTERFACE(m3g);

	{
		Mesh *mesh = m3gAllocZ(m3g, sizeof(Mesh));

		if (mesh != NULL) {
    		if (!m3gInitMesh(m3g, mesh,
                             hVertices, hTriangles, hAppearances,
                             trianglePatchCount,
                             M3G_CLASS_MESH)) {
    			m3gFree(m3g, mesh);
    			return NULL;
    		}
		}

		return (M3GMesh)mesh;
	}
}

/*!
 * \brief Sets submesh appearance.
 *
 * \param handle                Mesh object
 * \param appearanceIndex       submesh index
 * \param hAppearance           Appearance object
 */
M3G_API void m3gSetAppearance(M3GMesh handle,
                              M3Gint appearanceIndex,
                              M3GAppearance hAppearance)
{
	Mesh *mesh = (Mesh *)handle;
    M3G_VALIDATE_OBJECT(mesh);
	
	if (appearanceIndex < 0 || appearanceIndex >= mesh->trianglePatchCount) {
		m3gRaiseError(M3G_INTERFACE(mesh), M3G_INVALID_INDEX);
        return;
	}

    M3G_ASSIGN_REF(mesh->appearances[appearanceIndex], (Appearance *) hAppearance);
}

/*!
 * \brief Gets submesh appearance.
 *
 * \param handle                Mesh object
 * \param idx                   submesh index
 * \return                      Appearance object
 */
M3G_API M3GAppearance m3gGetAppearance(M3GMesh handle,
                                       M3Gint idx)
{
	Mesh *mesh = (Mesh *)handle;
    M3G_VALIDATE_OBJECT(mesh);
	
	if (idx < 0 || idx >= mesh->trianglePatchCount) {
		m3gRaiseError(M3G_INTERFACE(mesh), M3G_INVALID_INDEX);
        return NULL;
	}

    return mesh->appearances[idx];
}

/*!
 * \brief Gets submesh index buffer.
 *
 * \param handle                Mesh object
 * \param idx                   submesh index
 * \return                      IndexBuffer object
 */
M3G_API M3GIndexBuffer m3gGetIndexBuffer(M3GMesh handle,
                                         M3Gint idx)
{
	Mesh *mesh = (Mesh *)handle;
    M3G_VALIDATE_OBJECT(mesh);
	
	if (idx < 0 || idx >= mesh->trianglePatchCount) {
		m3gRaiseError(M3G_INTERFACE(mesh), M3G_INVALID_INDEX);
        return NULL;
	}

    return mesh->indexBuffers[idx];
}

/*!
 * \brief Gets VertexBuffer.
 *
 * \param handle                Mesh object
 * \return                      VertexBuffer object
 */
M3G_API M3GVertexBuffer m3gGetVertexBuffer(M3GMesh handle)
{
	Mesh *mesh = (Mesh *)handle;
    M3G_VALIDATE_OBJECT(mesh);

    return mesh->vertexBuffer;
}

/*!
 * \brief Gets submesh count.
 *
 * \param handle                Mesh object
 * \return                      submesh count
 */
M3G_API M3Gint m3gGetSubmeshCount(M3GMesh handle)
{
	Mesh *mesh = (Mesh *)handle;
    M3G_VALIDATE_OBJECT(mesh);

    return mesh->trianglePatchCount;
}

