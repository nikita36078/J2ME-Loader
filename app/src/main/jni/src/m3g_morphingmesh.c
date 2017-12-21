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
* Description: MorphingMesh implementation
*
*/


/*!
 * \internal
 * \file
 * \brief MorphingMesh implementation
 *
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_morphingmesh.h"
#include "m3g_memory.h"
#include "m3g_animationtrack.h"

#define WEIGHT_SHIFT        ( 8 )
#define WEIGHT_SCALE        ( 1 << WEIGHT_SHIFT )
#define WEIGHT_ROUND_PLUS   ( WEIGHT_SCALE / 4 )
#define WEIGHT_ROUND_MINUS  ( 0 )

/*!
 * \internal
 * \brief offsetof macro
 */
#include <stddef.h>

static M3Gbool m3gMorph(MorphingMesh *momesh);
static M3Gbool m3gCreateClones(VertexBuffer *vertices, VertexBuffer *morphed);
static void m3gDeleteClones(VertexBuffer *morphed);

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Destroys this MorphingMesh object.
 *
 * \param obj MorphingMesh object
 */
static void m3gDestroyMorphingMesh(Object *obj)
{
    M3Gint i;
    MorphingMesh *momesh = (MorphingMesh *) obj;
    M3G_VALIDATE_OBJECT(momesh);

    for (i = 0; i < momesh->numTargets; i++) {
        M3G_ASSIGN_REF(momesh->targets[i], NULL);
    }

    M3G_ASSIGN_REF(momesh->morphed, NULL);

    {
        Interface *m3g = M3G_INTERFACE(momesh); 
        m3gFree(m3g, momesh->targets);
        m3gFree(m3g, momesh->weights);
        m3gFree(m3g, momesh->floatWeights);
    }

    m3gDestroyMesh(obj);
}

/*!
 * \internal
 * \brief Overloaded Node method.
 *
 * Setup morphing mesh render. Morph and call Mesh
 * render setup.
 *
 * \param self MorphingMesh object
 * \param toCamera transform to camera
 * \param alphaFactor total alpha factor
 * \param caller caller node
 * \param renderQueue RenderQueue
 *
 * \retval M3G_TRUE continue render setup
 * \retval M3G_FALSE abort render setup
 */
static M3Gbool m3gMorphingMeshSetupRender(Node *self,
                                          const Node *caller,
                                          SetupRenderState *s,
                                          RenderQueue *renderQueue)
{
    MorphingMesh *momesh = (MorphingMesh *)self;
    M3G_UNREF(caller);
    m3gIncStat(M3G_INTERFACE(self), M3G_STAT_RENDER_NODES, 1);
    
    momesh->dirtyState = M3G_TRUE;

    if ((self->enableBits & NODE_RENDER_BIT) != 0 &&
        (self->scope & renderQueue->scope) != 0) {

        /* Try view frustum culling */

#       if defined(M3G_ENABLE_VF_CULLING)
        m3gUpdateCullingMask(s, renderQueue->camera, &momesh->bbox);
        if (s->cullMask == 0) {
            m3gIncStat(M3G_INTERFACE(self),
                       M3G_STAT_RENDER_NODES_CULLED, 1);
            return M3G_TRUE;
        }
#       endif
        
        /* No dice, must morph & render */
        
        M3G_BEGIN_PROFILE(M3G_INTERFACE(momesh), M3G_PROFILE_MORPH);
        if (!m3gMorph(momesh)) {
            return M3G_FALSE;
        }
        M3G_END_PROFILE(M3G_INTERFACE(momesh), M3G_PROFILE_MORPH);

        return m3gQueueMesh((Mesh*) self, &s->toCamera, renderQueue);
    }
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Overloaded Node method.
 *
 * Renders one submesh.
 *
 * \param self MorphingMesh object
 * \param ctx current render context
 * \param patchIndex submesh index
 */
static void m3gMorphingMeshDoRender(Node *self,
                                    RenderContext *ctx,
                                    const Matrix *toCamera,
                                    M3Gint patchIndex)
{
    MorphingMesh *momesh = (MorphingMesh *)self;
    Mesh *mesh = (Mesh *)self;

    m3gDrawMesh(ctx,
                momesh->morphed,
                mesh->indexBuffers[patchIndex],
                mesh->appearances[patchIndex],
                toCamera,
                mesh->totalAlphaFactor + 1,
                self->scope);
}

/*!
 * \internal
 * \brief Overloaded Node method.
 *
 * Morph and forward call Mesh internal ray intersect.
 *
 * \param self      MorphingMesh object
 * \param mask      pick scope mask
 * \param ray       pick ray
 * \param ri        RayIntersection object
 * \param toGroup   transform to originating group
 * \retval          M3G_TRUE    continue pick
 * \retval          M3G_FALSE   abort pick
 */
static M3Gbool m3gMorphingMeshRayIntersect( Node *self,
                                            M3Gint mask,
                                            M3Gfloat *ray,
                                            RayIntersection *ri,
                                            Matrix *toGroup)
{
    MorphingMesh *momesh = (MorphingMesh *)self;
    M3G_BEGIN_PROFILE(M3G_INTERFACE(momesh), M3G_PROFILE_MORPH);
    if (!m3gMorph(momesh)) {
        return M3G_FALSE;
    }
    M3G_END_PROFILE(M3G_INTERFACE(momesh), M3G_PROFILE_MORPH);
    return m3gMeshRayIntersectInternal(&momesh->mesh, momesh->morphed, mask, ray, ri, toGroup);
}

/*!
 * \internal
 * \brief Morphes short type vertex arrays.
 *
 * \param momesh        MorphingMesh object
 * \param dsta          destination VertexArray object
 * \param base          base VertexArray object
 * \param arrayOffset   offset to VertexArray object inside Mesh object
 * \retval M3G_TRUE     morph ok
 * \retval M3G_FALSE    morph failed, exception raised
 */
static M3Gbool m3gMorphShorts(MorphingMesh *momesh, VertexArray *dsta, VertexArray *base, M3Gint arrayOffset)
{
    M3Gint i;
    M3Gint sum, sw;
    M3Gshort *dst, *src;
    M3Gshort j, numMsTargets = 0;
    M3Gshort **srct = m3gAllocTemp(M3G_INTERFACE(momesh), sizeof(M3Gshort *) * momesh->numTargets * 2);
    M3Gshort *msTargets = (M3Gshort *) (srct + momesh->numTargets);

    if (msTargets == NULL) {
        return M3G_FALSE;
    }

    /* Check that target arrays are in same format */
    for (j = 0; j < momesh->numTargets; j++) {
        VertexArray **va;
        va = (VertexArray **)(((M3Gbyte *)momesh->targets[j]) + arrayOffset);
        if (!m3gIsCompatible(*va, base)) {
            /* Unmap all arrays */
            for (j = 0; j < numMsTargets; j++) {
                m3gUnmapVertexArray(*(VertexArray **)(((M3Gbyte *)momesh->targets[msTargets[j]]) + arrayOffset));
            }
            m3gFreeTemp(M3G_INTERFACE(momesh));
            m3gRaiseError(M3G_INTERFACE(momesh), M3G_INVALID_OPERATION);
            return M3G_FALSE;
        }
        /* Use only most significant targets*/
        if (momesh->weights[j] != 0) {
            msTargets[numMsTargets] = j;
            srct[numMsTargets] = (M3Gshort *) m3gMapVertexArrayReadOnly(*va);
            numMsTargets++;
        }
    }

    dst = (M3Gshort *) m3gMapVertexArray(dsta);
    src = (M3Gshort *) m3gMapVertexArrayReadOnly(base);
    sw = momesh->sumWeights;

    for (i = 0; i < base->vertexCount * base->elementSize; i++) {
        sum = src[i] * sw;
        for (j = 0; j < numMsTargets; j++) {
            sum += srct[j][i] * momesh->weights[msTargets[j]];
        }
        /* Round */
        if (sum >= 0) {
            sum += WEIGHT_ROUND_PLUS;
        }
        else {
            sum -= WEIGHT_ROUND_MINUS;
        }
        dst[i] = (M3Gshort)(sum >> WEIGHT_SHIFT);
    }

    /* Unmap all arrays */
    for (j = 0; j < numMsTargets; j++) {
        m3gUnmapVertexArray(*(VertexArray **)(((M3Gbyte *)momesh->targets[msTargets[j]]) + arrayOffset));
    }

    m3gUnmapVertexArray(base);
    m3gUnmapVertexArray(dsta);
    
    m3gFreeTemp(M3G_INTERFACE(momesh));
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Morphes byte type vertex arrays.
 *
 * \param momesh        MorphingMesh object
 * \param dsta          destination VertexArray object
 * \param base          base VertexArray object
 * \param arrayOffset   offset to VertexArray object inside Mesh object
 * \retval M3G_TRUE     morph ok
 * \retval M3G_FALSE    morph failed, exception raised
 */
static M3Gbool m3gMorphBytes(MorphingMesh *momesh, VertexArray *dsta, VertexArray *base, M3Gint arrayOffset)
{
    M3Gint i;
    M3Gint sum, sw;
    M3Gbyte *dst, *src;
    M3Gint skip;
    M3Gshort j, numMsTargets = 0;
    M3Gbyte **srct = m3gAllocTemp(M3G_INTERFACE(momesh), sizeof(M3Gbyte *) * momesh->numTargets * 2);
    M3Gshort *msTargets = (M3Gshort *) (srct + momesh->numTargets);

    if (msTargets == NULL) {
        return M3G_FALSE;
    }

    /* Check that target arrays are in same format */
    for (j = 0; j < momesh->numTargets; j++) {
        VertexArray **va;
        va = (VertexArray **)(((M3Gbyte *)momesh->targets[j]) + arrayOffset);
        if (!m3gIsCompatible(*va, base)) {
            /* Unmap all arrays */
            for (j = 0; j < numMsTargets; j++) {
                m3gUnmapVertexArray(*(VertexArray **)(((M3Gbyte *)momesh->targets[msTargets[j]]) + arrayOffset));
            }
            m3gFreeTemp(M3G_INTERFACE(momesh));
            m3gRaiseError(M3G_INTERFACE(momesh), M3G_INVALID_OPERATION);
            return M3G_FALSE;
        }
        /* Use only most significant targets*/
        if (momesh->weights[j] != 0) {
            msTargets[numMsTargets] = j;
            srct[numMsTargets] = (M3Gbyte *) m3gMapVertexArrayReadOnly(*va);
            numMsTargets++;
        }
    }

    dst = (M3Gbyte *) m3gMapVertexArray(dsta);
    src = (M3Gbyte *) m3gMapVertexArrayReadOnly(base);
    sw = momesh->sumWeights;
    skip = base->elementSize;

    for (i = 0; i < base->vertexCount * base->stride; i++) {
        if ((i & 3) >= skip) continue;
        sum = src[i] * sw;
        for (j = 0; j < numMsTargets; j++) {
            sum += srct[j][i] * momesh->weights[msTargets[j]];
        }
        /* Round */
        if (sum >= 0) {
            sum += WEIGHT_ROUND_PLUS;
        }
        else {
            sum -= WEIGHT_ROUND_MINUS;
        }
        dst[i] = (M3Gbyte)(sum >> WEIGHT_SHIFT);
    }

    /* Unmap all arrays */
    for (j = 0; j < numMsTargets; j++) {
        m3gUnmapVertexArray(*(VertexArray **)(((M3Gbyte *)momesh->targets[msTargets[j]]) + arrayOffset));
    }

    m3gUnmapVertexArray(base);
    m3gUnmapVertexArray(dsta);

    m3gFreeTemp(M3G_INTERFACE(momesh));
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Morphes byte type color vertex arrays.
 *
 * \param momesh        MorphingMesh object
 * \param dsta          destination VertexArray object
 * \param base          base VertexArray object
 * \param arrayOffset   offset to VertexArray object inside Mesh object
 * \retval M3G_TRUE     morph ok
 * \retval M3G_FALSE    morph failed, exception raised
 */
static M3Gbool m3gMorphColorBytes(MorphingMesh *momesh, VertexArray *dsta, VertexArray *base, M3Gint arrayOffset)
{
    M3Gint i;
    M3Gint sum, sw;
    M3Gubyte *dst, *src;
    M3Gint skip;
    M3Gshort j, numMsTargets = 0;
    M3Gubyte **srct = m3gAllocTemp(M3G_INTERFACE(momesh), sizeof(M3Gubyte *) * momesh->numTargets * 2);
    M3Gshort *msTargets = (M3Gshort *) (srct + momesh->numTargets);


    if (msTargets == NULL) {
        return M3G_FALSE;
    }

    /* Check that target arrays are in same format */
    for (j = 0; j < momesh->numTargets; j++) {
        VertexArray **va;
        va = (VertexArray **)(((M3Gbyte *)momesh->targets[j]) + arrayOffset);
        if (!m3gIsCompatible(*va, base)) {
            /* Unmap all arrays */
            for (j = 0; j < numMsTargets; j++) {
                m3gUnmapVertexArray(*(VertexArray **)(((M3Gbyte *)momesh->targets[msTargets[j]]) + arrayOffset));
            }
            m3gFreeTemp(M3G_INTERFACE(momesh));
            m3gRaiseError(M3G_INTERFACE(momesh), M3G_INVALID_OPERATION);
            return M3G_FALSE;
        }
        /* Use only most significant targets*/
        if (momesh->weights[j] != 0) {
            msTargets[numMsTargets] = j;
            srct[numMsTargets] = (M3Gubyte *) m3gMapVertexArrayReadOnly(*va);
            numMsTargets++;
        }
    }

    dst = (M3Gubyte *) m3gMapVertexArray(dsta);
    src = (M3Gubyte *) m3gMapVertexArray(base);
    sw = momesh->sumWeights;
    skip = base->elementSize;

    for (i = 0; i < base->vertexCount * base->stride; i++) {
        if ((i & 3) >= skip) continue;
        sum = src[i] * sw;
        for (j = 0; j < numMsTargets; j++) {
            sum += srct[j][i] * momesh->weights[msTargets[j]];
        }

        /* Round */
        sum += WEIGHT_ROUND_PLUS;
        /* Clamp */
        if (sum > 255 * WEIGHT_SCALE) sum = 255 * WEIGHT_SCALE;

        dst[i] = (M3Gubyte) (sum >> WEIGHT_SHIFT);
    }

    /* Unmap all arrays */
    for (j = 0; j < numMsTargets; j++) {
        m3gUnmapVertexArray(*(VertexArray **)(((M3Gbyte *)momesh->targets[msTargets[j]]) + arrayOffset));
    }

    m3gUnmapVertexArray(base);
    m3gUnmapVertexArray(dsta);
    
    m3gFreeTemp(M3G_INTERFACE(momesh));
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Morphes all MorphingMesh vertex arrays. That
 * is positions, normals, texture coordinates and colors.
 *
 * \param momesh        MorphingMesh object
 * \retval M3G_TRUE     morph ok
 * \retval M3G_FALSE    morph failed, exception raised
 */
static M3Gbool m3gMorph(MorphingMesh *momesh)
{
    M3Gint i, j;
    M3Gint nullValues;

    if (momesh->cloneArrayMask != m3gGetArrayMask(momesh->base)) {
        if (!m3gCreateClones(momesh->base, momesh->morphed)) {
            return M3G_FALSE;
        }
        momesh->cloneArrayMask = m3gGetArrayMask(momesh->base);
        momesh->dirtyState = M3G_TRUE;
    }

    if (momesh->dirtyState == M3G_FALSE) return M3G_TRUE;

    if (momesh->base->vertices != NULL) {
        nullValues = 0;
        for (i = 0; i < momesh->numTargets; i++) {
            if (momesh->targets[i]->vertices == NULL) {
                nullValues++;
            }           
        }

        if (nullValues != momesh->numTargets && nullValues != 0) {
            m3gRaiseError(M3G_INTERFACE(momesh), M3G_INVALID_OPERATION);
            return M3G_FALSE;
        }

        if (nullValues == 0) {
            if (momesh->base->vertices->elementType == M3G_GLTYPE(M3G_SHORT)) {
                if (!m3gMorphShorts( momesh,
                                     momesh->morphed->vertices,
                                     momesh->base->vertices,
                                     offsetof(VertexBuffer, vertices) )) return M3G_FALSE;
            }
            else {
                if (!m3gMorphBytes(  momesh,
                                     momesh->morphed->vertices,
                                     momesh->base->vertices,
                                     offsetof(VertexBuffer, vertices) )) return M3G_FALSE;
            }
        }
        else {
            m3gCopy(m3gMapVertexArray(momesh->morphed->vertices),
                    m3gMapVertexArrayReadOnly(momesh->base->vertices),
                    momesh->base->vertices->stride * momesh->base->vertices->vertexCount );
            m3gUnmapVertexArray(momesh->base->vertices);
            m3gUnmapVertexArray(momesh->morphed->vertices);
        }
    }
    else {
        for (i = 0; i < momesh->numTargets; i++) {
            if (momesh->targets[i]->vertices != NULL) {
                m3gRaiseError(M3G_INTERFACE(momesh), M3G_INVALID_OPERATION);
                return M3G_FALSE;
            }           
        }
    }

    if (momesh->base->normals != NULL) {
        nullValues = 0;
        for (i = 0; i < momesh->numTargets; i++) {
            if (momesh->targets[i]->normals == NULL) {
                nullValues++;
            }           
        }

        if (nullValues != momesh->numTargets && nullValues != 0) {
            m3gRaiseError(M3G_INTERFACE(momesh), M3G_INVALID_OPERATION);
            return M3G_FALSE;
        }

        if (nullValues == 0) {
            if (momesh->base->normals->elementType == M3G_GLTYPE(M3G_SHORT)) {
                if (!m3gMorphShorts( momesh,
                                     momesh->morphed->normals,
                                     momesh->base->normals,
                                     offsetof(VertexBuffer, normals) )) return M3G_FALSE;
            }
            else {
                if (!m3gMorphBytes(  momesh,
                                     momesh->morphed->normals,
                                     momesh->base->normals,
                                     offsetof(VertexBuffer, normals) )) return M3G_FALSE;
            }
        }
        else {
            m3gCopy(m3gMapVertexArray(momesh->morphed->normals),
                    m3gMapVertexArrayReadOnly(momesh->base->normals),
                    momesh->base->normals->stride * momesh->base->normals->vertexCount );
            m3gUnmapVertexArray(momesh->base->normals);
            m3gUnmapVertexArray(momesh->morphed->normals);
        }
    }
    else {
        for (i = 0; i < momesh->numTargets; i++) {
            if (momesh->targets[i]->normals != NULL) {
                m3gRaiseError(M3G_INTERFACE(momesh), M3G_INVALID_OPERATION);
                return M3G_FALSE;
            }           
        }
    }
    
    if (momesh->base->colors != NULL) {
        nullValues = 0;
        for (i = 0; i < momesh->numTargets; i++) {
            if (momesh->targets[i]->colors == NULL) {
                nullValues++;
            }           
        }
        
        if (nullValues != momesh->numTargets && nullValues != 0) {
            m3gRaiseError(M3G_INTERFACE(momesh), M3G_INVALID_OPERATION);
            return M3G_FALSE;
        }

        if (nullValues == 0) {
            /* Only byte colors are supported */
            if (!m3gMorphColorBytes( momesh,
                                     momesh->morphed->colors,
                                     momesh->base->colors,
                                     offsetof(VertexBuffer, colors) )) return M3G_FALSE;
        }
        else {
            m3gCopy(m3gMapVertexArray(momesh->morphed->colors),
                    m3gMapVertexArrayReadOnly(momesh->base->colors),
                    momesh->base->colors->stride * momesh->base->colors->vertexCount );
            m3gUnmapVertexArray(momesh->base->colors);
            m3gUnmapVertexArray(momesh->morphed->colors);
        }
    }
    else {
        /* Morph default color */   
        M3Gint r, g, b, a;
        
        for (i = 0; i < momesh->numTargets; i++) {
            if (momesh->targets[i]->colors != NULL) {
                m3gRaiseError(M3G_INTERFACE(momesh), M3G_INVALID_OPERATION);
                return M3G_FALSE;
            }           
        }

        r = momesh->base->defaultColor.r * momesh->sumWeights;
        g = momesh->base->defaultColor.g * momesh->sumWeights;
        b = momesh->base->defaultColor.b * momesh->sumWeights;
        a = momesh->base->defaultColor.a * momesh->sumWeights;

        for (i = 0; i < momesh->numTargets; i++) {
            r += momesh->targets[i]->defaultColor.r * momesh->weights[i];
            g += momesh->targets[i]->defaultColor.g * momesh->weights[i];
            b += momesh->targets[i]->defaultColor.b * momesh->weights[i];
            a += momesh->targets[i]->defaultColor.a * momesh->weights[i];
        }       

        /* Clamp values*/
        if (r < 0) r = 0;
        if (g < 0) g = 0;
        if (b < 0) b = 0;
        if (a < 0) a = 0;

        if (r > 255 * WEIGHT_SCALE) r = 255 * WEIGHT_SCALE;
        if (g > 255 * WEIGHT_SCALE) g = 255 * WEIGHT_SCALE;
        if (b > 255 * WEIGHT_SCALE) b = 255 * WEIGHT_SCALE;
        if (a > 255 * WEIGHT_SCALE) a = 255 * WEIGHT_SCALE;

        momesh->morphed->defaultColor.r = (GLubyte) (r >> WEIGHT_SHIFT);
        momesh->morphed->defaultColor.g = (GLubyte) (g >> WEIGHT_SHIFT);
        momesh->morphed->defaultColor.b = (GLubyte) (b >> WEIGHT_SHIFT);
        momesh->morphed->defaultColor.a = (GLubyte) (a >> WEIGHT_SHIFT);
    }

    for (i = 0; i < M3G_NUM_TEXTURE_UNITS; i++) {
        if (momesh->base->texCoords[i] != NULL) {
            nullValues = 0;
            for (j = 0; j < momesh->numTargets; j++) {
                if (momesh->targets[j]->texCoords[i] == NULL) {
                    nullValues++;
                }           
            }

            if (nullValues != momesh->numTargets && nullValues != 0) {
                m3gRaiseError(M3G_INTERFACE(momesh), M3G_INVALID_OPERATION);
                return M3G_FALSE;
            }
    
            if (nullValues == 0) {
                if (momesh->base->texCoords[i]->elementType == M3G_GLTYPE(M3G_SHORT)) {
                    if (!m3gMorphShorts( momesh,
                                         momesh->morphed->texCoords[i],
                                         momesh->base->texCoords[i],
                                         offsetof(VertexBuffer, texCoords) + i * sizeof(VertexBuffer *) )) return M3G_FALSE;
                }
                else {
                    if (!m3gMorphBytes(  momesh,
                                         momesh->morphed->texCoords[i],
                                         momesh->base->texCoords[i],
                                         offsetof(VertexBuffer, texCoords) + i * sizeof(VertexBuffer *) )) return M3G_FALSE;
                }
            }
            else {
                m3gCopy(m3gMapVertexArray(momesh->morphed->texCoords[i]),
                        m3gMapVertexArrayReadOnly(momesh->base->texCoords[i]),
                        momesh->base->texCoords[i]->stride * momesh->base->texCoords[i]->vertexCount );
                m3gUnmapVertexArray(momesh->base->texCoords[i]);
                m3gUnmapVertexArray(momesh->morphed->texCoords[i]);
            }
        }
        else {
            for (j = 0; j < momesh->numTargets; j++) {
                if (momesh->targets[j]->texCoords[i] != NULL) {
                    m3gRaiseError(M3G_INTERFACE(momesh), M3G_INVALID_OPERATION);
                    return M3G_FALSE;
                }           
            }
        }
    }

    momesh->dirtyState = M3G_FALSE;
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param property      animation property
 * \retval M3G_TRUE     property supported
 * \retval M3G_FALSE    property not supported
 */
static M3Gbool m3gMorphingMeshIsCompatible(M3Gint property)
{
    switch (property) {
    case M3G_ANIM_MORPH_WEIGHTS:
        return M3G_TRUE;
    default:
        return m3gNodeIsCompatible(property);
    }
}

/*!
 * \internal
 * \brief Overloaded Node method
 */
static M3Gint m3gMorphingMeshGetBBox(Node *self, AABB *bbox)
{
    MorphingMesh *m = (MorphingMesh*) self;
    
    if (m->base->vertices) {
        if (self->dirtyBits & NODE_BBOX_BIT) {

            /* Blend target and base mesh bounding boxes to estimate the
             * real bounding box of the morphed mesh */
        
            M3Gint i;
            M3Gint vMin, vMax;
            M3Gfloat sumWeights = (M3Gfloat)m->sumWeights / WEIGHT_SCALE;

            /* Get base mesh bounding box */

            m3gGetArrayValueRange(m->base->vertices, &vMin, &vMax);
            if (sumWeights < 0) {
                M3Gint t = vMin;
                vMin = vMax;
                vMax = t;
            }
            
            for (i = 0; i < 3; ++i) {
                bbox->min[i] = (M3Gfloat) vMin;
                bbox->max[i] = (M3Gfloat) vMax;
            }
            
            /* Blend with target bounding boxes */
            
            for (i = 0; i < m->numTargets; i++) {
                if (m->targets[i]->vertices) {
                    M3Gfloat w = m->floatWeights[i];

                    m3gGetArrayValueRange(m->targets[i]->vertices,
                                          &vMin, &vMax);
                    if (w < 0) {
                        M3Gint t = vMin;
                        vMin = vMax;
                        vMax = t;
                    }
                    
                    bbox->min[0] = m3gMadd((M3Gfloat) vMin, w, bbox->min[0]);
                    bbox->min[1] = m3gMadd((M3Gfloat) vMin, w, bbox->min[1]);
                    bbox->min[2] = m3gMadd((M3Gfloat) vMin, w, bbox->min[2]);
                    bbox->max[0] = m3gMadd((M3Gfloat) vMax, w, bbox->max[0]);
                    bbox->max[1] = m3gMadd((M3Gfloat) vMax, w, bbox->max[1]);
                    bbox->max[2] = m3gMadd((M3Gfloat) vMax, w, bbox->max[2]);
                }
            }

            /* Apply scale and bias, and flip the min-max values if
             * the scale is negative */
            {
                const VertexBuffer *vb = m->base;
                for (i = 0; i < 3; ++i) {
                    
                    bbox->min[i] = m3gMadd(bbox->min[i], vb->vertexScale,
                                           vb->vertexBias[i]);
                    bbox->max[i] = m3gMadd(bbox->max[i], vb->vertexScale,
                                           vb->vertexBias[i]);
                    
                    if (bbox->min[i] > bbox->max[i]) {
                        M3Gfloat t = bbox->min[i];
                        bbox->min[i] = bbox->max[i];
                        bbox->max[i] = t;
                    }
                }
            }
            
            m->bbox = *bbox;
        }
        else {
            *bbox = m->bbox;
        }

        /* Estimate a cost of 6 times normal bbox check, as we're
         * dynamically computing the box every time... */
        
        return 6 * VFC_BBOX_COST + VFC_NODE_OVERHEAD;
    }
    return 0; /* no vertices, nothing to render */
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self          MorphingMesh object
 * \param property      animation property
 * \param valueSize     size of value array
 * \param value         value array
 */
static void m3gMorphingMeshUpdateProperty(Object *self,
                                          M3Gint property,
                                          M3Gint valueSize,
                                          const M3Gfloat *value)
{
    M3Gint i;
    MorphingMesh *mMesh = (MorphingMesh *)self;
    M3G_VALIDATE_OBJECT(mMesh);
    M3G_ASSERT_PTR(value);
    
    switch (property) {
    case M3G_ANIM_MORPH_WEIGHTS:
        mMesh->sumWeights = WEIGHT_SCALE;
        mMesh->dirtyState = M3G_TRUE;

        for (i = 0; i < mMesh->numTargets; i++) {
            /* Value array can have less or more elements than
               numTargets is. If less, weights after the last
               element are set to 0. If more, weights after
               numTargets are ignored. */
            if (i < valueSize) {
                mMesh->floatWeights[i] = value[i];
                mMesh->weights[i] = m3gRoundToInt(m3gMul(value[i], WEIGHT_SCALE));
                mMesh->sumWeights -= mMesh->weights[i];
            }
            else
                mMesh->weights[i] = 0;
        }
        m3gInvalidateNode((Node*)self, NODE_BBOX_BIT);
        break;
    default:
        m3gNodeUpdateProperty(self, property, valueSize, value);
    }
}

/*!
 * \internal
 * \brief Deletes all internal vertex arrays.
 *
 * \param morphed       VertexBuffer object that contains
 *                      the internal arrays.
 */
static void m3gDeleteClones(VertexBuffer *morphed)
{
    M3Gint i;

    M3G_ASSIGN_REF(morphed->vertices, NULL);
    M3G_ASSIGN_REF(morphed->normals, NULL);
    M3G_ASSIGN_REF(morphed->colors, NULL);

    for (i = 0; i < M3G_NUM_TEXTURE_UNITS; i++) {
        M3G_ASSIGN_REF(morphed->texCoords[i], NULL);
    }
}

/*!
 * \internal
 * \brief Creates all internal vertex arrays.
 *
 * \param vertices      VertexBuffer object that contains
 *                      the original arrays.
 * \param morphed       VertexBuffer object that contains
 *                      the internal arrays.
 * \retval M3G_TRUE     clones created
 * \retval M3G_FALSE    clone creation failed
 */
static M3Gbool m3gCreateClones(VertexBuffer *vertices, VertexBuffer *morphed)
{
    M3Gint i, refCount;

    /* Delete old clone arrays */
    m3gDeleteClones(morphed);

    /* Clone all attributes but arrays, reference count and animtracks. Because
       morphed is an internal buffer it cannot have those attributes copied.
       If they would be copied it would cause a memory leak when morphed array
       is destroyed. */
    refCount = morphed->object.refCount;
    m3gCopy(morphed, vertices, sizeof(VertexBuffer));
    morphed->object.refCount = refCount;
    morphed->object.animTracks = NULL;
    morphed->vertices = NULL;
    morphed->normals = NULL;
    morphed->colors = NULL;
    for (i = 0; i < M3G_NUM_TEXTURE_UNITS; i++) {
        morphed->texCoords[i] = NULL;
    }

    if (vertices->vertices != NULL) {
        M3G_ASSIGN_REF(morphed->vertices, m3gCloneVertexArray(vertices->vertices));
        if (morphed->vertices == NULL) {
            return M3G_FALSE;
        }
    }

    if (vertices->normals != NULL) {
        M3G_ASSIGN_REF(morphed->normals, m3gCloneVertexArray(vertices->normals));
        if (morphed->normals == NULL) {
            return M3G_FALSE;
        }
    }

    if (vertices->colors != NULL) {
        M3G_ASSIGN_REF(morphed->colors, m3gCloneVertexArray(vertices->colors));
        if (morphed->colors == NULL) {
            return M3G_FALSE;
        }
    }

    for (i = 0; i < M3G_NUM_TEXTURE_UNITS; i++) {
        if (vertices->texCoords[i] != NULL) {
            M3G_ASSIGN_REF(morphed->texCoords[i], m3gCloneVertexArray(vertices->texCoords[i]));
            if (morphed->texCoords[i] == NULL) {
                return M3G_FALSE;
            }
        }   
    }

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self MorphingMesh object
 * \param references array of reference objects
 * \return number of references
 */
static M3Gint m3gMorphingMeshDoGetReferences(Object *self, Object **references)
{
    MorphingMesh *mmesh = (MorphingMesh *)self;
    M3Gint i, num = m3gMeshDoGetReferences(self, references);
    for (i = 0; i < mmesh->numTargets; i++) {
        if (mmesh->targets[i] != NULL) {
            if (references != NULL)
                references[num] = (Object *)mmesh->targets[i];
            num++;
        }
    }
    return num;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 */
static Object *m3gMorphingMeshFindID(Object *self, M3Gint userID)
{
    int i;
    MorphingMesh *mmesh = (MorphingMesh *)self;
    Object *found = m3gMeshFindID(self, userID);
    
    for (i = 0; !found && i < mmesh->numTargets; ++i) {
        if (mmesh->targets[i] != NULL) {
            found = m3gFindID((Object*) mmesh->targets[i], userID);
        }
    }
    return found;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original MorphingMesh object
 * \param cloneObj pointer to cloned Mesh object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gMorphingMeshDuplicate(const Object *originalObj,
                                        Object **cloneObj,
                                        Object **pairs,
                                        M3Gint *numPairs)
{
    MorphingMesh *original = (MorphingMesh *)originalObj;
    MorphingMesh *clone;
    M3G_ASSERT(*cloneObj == NULL); /* no derived classes */

    /* Create the clone object */
    
    clone = (MorphingMesh*) m3gCreateMorphingMesh(originalObj->interface,
                                                  original->mesh.vertexBuffer,
                                                  original->targets,
                                                  original->mesh.indexBuffers,
                                                  original->mesh.appearances,
                                                  original->mesh.trianglePatchCount,
                                                  original->numTargets);
    if (!clone) {
        return M3G_FALSE;
    }
    *cloneObj = (Object *)clone;

    /* Duplicate base class data and if that succeeds, our own */
    
    if (m3gMeshDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        m3gSetWeights(clone, original->floatWeights, original->numTargets);
        return M3G_TRUE;
    }
    else {
        return M3G_FALSE;
    }
}

/*!
 * \internal
 * \brief Overloaded Node method
 */
static M3Gbool m3gMorphingMeshValidate(Node *self, M3Gbitmask stateBits, M3Gint scope)
{
    MorphingMesh *mesh = (MorphingMesh*) self;
    AABB bbox;
    int i;
    M3G_BEGIN_PROFILE(M3G_INTERFACE(self), M3G_PROFILE_VFC_UPDATE);

    /* Always compute a new bounding box for morphing meshes --
     * otherwise we would have to maintain timestamps for all source
     * vertex buffers */
    
    m3gMorphingMeshGetBBox(self, &bbox);

    /* Invalidate enclosing bounding boxes if new box is larger */
    
    for (i = 0; i < 3; ++i) {
        if (bbox.min[i] < mesh->bbox.min[i] ||
            bbox.max[i] > mesh->bbox.max[i]) {
            m3gInvalidateNode(self, NODE_BBOX_BIT);
            break;
        }
    }
    mesh->bbox = bbox;
    
    M3G_END_PROFILE(M3G_INTERFACE(self), M3G_PROFILE_VFC_UPDATE);
    return m3gMeshValidate(self, stateBits, scope);
}

/*!
 * \internal
 * \brief Initializes a MorphingMesh object. See specification
 * for default values.
 *
 * \param m3g                   M3G interface
 * \param momesh                MorphingMesh object
 * \param hVertices             VertexBuffer object
 * \param hTargets              array of morph target VertexBuffer objects
 * \param hTriangles            array of IndexBuffer objects
 * \param hAppearances          array of Appearance objects
 * \param trianglePatchCount    number of submeshes
 * \param targetCount           number of morph targets
 * \retval                      M3G_TRUE success
 * \retval                      M3G_FALSE failed
 */
static M3Gbool m3gInitMorphingMesh( Interface *m3g,
                                    MorphingMesh *momesh,
                                    M3GVertexBuffer hVertices,
                                    M3GVertexBuffer *hTargets,
                                    M3GIndexBuffer *hTriangles,
                                    M3GAppearance *hAppearances,
                                    M3Gint trianglePatchCount,
                                    M3Gint targetCount )
{
    M3Gint i;

    VertexBuffer *morphed;

    /* Check target validities */
    for (i = 0; i < targetCount; i++) {
        if (hTargets[i] == NULL) {
            m3gRaiseError(m3g, M3G_NULL_POINTER);
            return M3G_FALSE;
        }
    }

    /* Create morphed vertex buffer */
    morphed = (VertexBuffer *)m3gCreateVertexBuffer(m3g);
    if (morphed == NULL) {
        return M3G_FALSE;
    }

    M3G_ASSIGN_REF(momesh->morphed, morphed);

    /* Create initial vertex array clones */
    if (!m3gCreateClones(hVertices, morphed)) {
        M3G_ASSIGN_REF(momesh->morphed, NULL);
        return M3G_FALSE;
    }

    /* MorphingMesh is derived from Mesh */
    if (!m3gInitMesh(m3g, &momesh->mesh,
                     hVertices, hTriangles, hAppearances,
                     trianglePatchCount,
                     M3G_CLASS_MORPHING_MESH)) {
        M3G_ASSIGN_REF(momesh->morphed, NULL);
        return M3G_FALSE;        
    }

    momesh->targets = m3gAllocZ(m3g, sizeof(VertexBuffer *) * targetCount);
    momesh->weights = m3gAllocZ(m3g, sizeof(M3Gint) * targetCount);
    momesh->floatWeights = m3gAllocZ(m3g, sizeof(M3Gfloat) * targetCount);

    /* Check for errors */
    if (!momesh->targets || !momesh->weights || !momesh->floatWeights) {
        m3gDestroyMesh((Object *) &momesh->mesh); /* already init'd above */
        m3gFree(m3g, momesh->targets);
        m3gFree(m3g, momesh->weights);
        m3gFree(m3g, momesh->floatWeights);
        M3G_ASSIGN_REF(momesh->morphed, NULL);
        return M3G_FALSE;    
    }

    /* Assign references */
    for (i = 0; i < targetCount; i++) {
        M3G_ASSIGN_REF(momesh->targets[i], hTargets[i]);
    }
    
    momesh->base = hVertices;
    momesh->numTargets = targetCount;
    momesh->sumWeights = WEIGHT_SCALE;
    momesh->dirtyState = M3G_TRUE;
    momesh->cloneArrayMask = m3gGetArrayMask(hVertices);

    return M3G_TRUE;
}

/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

static const NodeVFTable m3gvf_MorphingMesh = {
    {
        {
            m3gMeshApplyAnimation,
            m3gMorphingMeshIsCompatible,
            m3gMorphingMeshUpdateProperty,
            m3gMorphingMeshDoGetReferences,
            m3gMorphingMeshFindID,
            m3gMorphingMeshDuplicate,
            m3gDestroyMorphingMesh
        }
    },
    m3gNodeAlign,
    m3gMorphingMeshDoRender,
    m3gMorphingMeshGetBBox,
    m3gMorphingMeshRayIntersect,
    m3gMorphingMeshSetupRender,
    m3gNodeUpdateDuplicateReferences,
    m3gMorphingMeshValidate
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a MorphingMesh object.
 *
 * \param interface             M3G interface
 * \param hVertices             VertexBuffer object
 * \param hTargets              array of morph target VertexBuffer objects
 * \param hTriangles            array of IndexBuffer objects
 * \param hAppearances          array of Appearance objects
 * \param trianglePatchCount    number of submeshes
 * \param targetCount           number of morph targets
 * \retval Mesh new MorphingMesh object
 * \retval NULL MorphingMesh creating failed
 */
M3G_API M3GMorphingMesh m3gCreateMorphingMesh(M3GInterface interface,
                                              M3GVertexBuffer hVertices,
                                              M3GVertexBuffer *hTargets,
                                              M3GIndexBuffer *hTriangles,
                                              M3GAppearance *hAppearances,
                                              M3Gint trianglePatchCount,
                                              M3Gint targetCount)
{
    Interface *m3g = (Interface *) interface;
    M3G_VALIDATE_INTERFACE(m3g);

    {
        MorphingMesh *momesh = m3gAllocZ(m3g, sizeof(MorphingMesh));
    
        if (momesh != NULL) {
            if (!m3gInitMorphingMesh(   m3g,
                                        momesh,
                                        hVertices, hTargets,
                                        hTriangles, hAppearances,
                                        trianglePatchCount, targetCount)) {
                m3gFree(m3g, momesh);
                return NULL;
            }
        }

        return (M3GMorphingMesh)momesh;
    }
}

/*!
 * \brief Set morph weights.
 *
 * \param handle                MorphingMesh object
 * \param weights               array of weights
 * \param numWeights            number of weights in array
 */
M3G_API void m3gSetWeights(M3GMorphingMesh handle,
                           M3Gfloat *weights,
                           M3Gint numWeights)
{
    MorphingMesh *momesh = (MorphingMesh *)handle;
    M3G_VALIDATE_OBJECT(momesh);
    
    if (numWeights >= momesh->numTargets) {
        M3Gint i;
        momesh->dirtyState = M3G_TRUE;
        momesh->sumWeights = WEIGHT_SCALE;
        for (i = 0; i < momesh->numTargets; i++) {
            momesh->floatWeights[i] = weights[i];
            momesh->weights[i] = m3gRoundToInt(m3gMul(weights[i], WEIGHT_SCALE));
            momesh->sumWeights -= momesh->weights[i];
        }
        m3gInvalidateNode((Node*)momesh, NODE_BBOX_BIT);
    }
    else {
        m3gRaiseError(M3G_INTERFACE(momesh), M3G_INVALID_VALUE);
    }
}

/*!
 * \brief Get morph weights.
 *
 * \param handle                MorphingMesh object
 * \param weights               array of weights to fill in
 * \param numWeights            max number of weights in array
 */
M3G_API void m3gGetWeights(M3GMorphingMesh handle,
                           M3Gfloat *weights,
                           M3Gint numWeights)
{
    MorphingMesh *momesh = (MorphingMesh *)handle;
    M3G_VALIDATE_OBJECT(momesh);
    
    if (numWeights >= momesh->numTargets) {
        M3Gint i;
        for (i = 0; i < momesh->numTargets; i++) {
            weights[i] = momesh->floatWeights[i];
        }
    }
    else {
        m3gRaiseError(M3G_INTERFACE(momesh), M3G_INVALID_VALUE);
    }
}

/*!
 * \brief Get morph target.
 *
 * \param handle                MorphingMesh object
 * \param idx                   target index
 * \return                      VertexBuffer object
 */
M3G_API M3GVertexBuffer m3gGetMorphTarget(M3GMorphingMesh handle, M3Gint idx)
{
    MorphingMesh *momesh = (MorphingMesh *)handle;
    M3G_VALIDATE_OBJECT(momesh);

    if (idx < 0 || idx >= momesh->numTargets) {
        m3gRaiseError(M3G_INTERFACE(momesh), M3G_INVALID_INDEX);
        return NULL;
    }

    return momesh->targets[idx];
}

/*!
 * \brief Get morph target count.
 *
 * \param handle                MorphingMesh object
 * \return                      number of morph targets
 */
M3G_API M3Gint m3gGetMorphTargetCount(M3GMorphingMesh handle)
{
    MorphingMesh *momesh = (MorphingMesh *)handle;
    M3G_VALIDATE_OBJECT(momesh);

    return momesh->numTargets;
}

