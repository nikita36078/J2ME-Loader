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
* Description: SkinnedMesh implementation
*
*/


/*!
 * \internal
 * \file
 * \brief SkinnedMesh implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_skinnedmesh.h"
#include "m3g_memory.h"
#include "m3g_animationtrack.h"

/*----------------------------------------------------------------------
 * Internal structures
 *--------------------------------------------------------------------*/

struct BoneRecord
{
    Node *node;

    /*! \internal \brief "At-rest" transformation from skinned mesh to bone */
    Matrix toBone;

    /*! \internal \brief Cached animated transformation for positions */
    M3Gshort baseMatrix[9];
    M3Gshort posVec[3];
    M3Gshort baseExp, posExp, maxExp;

    /*! \internal \brief Cached animated transformation for normals */
    M3Gshort normalMatrix[9];
};

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Destroys this SkinnedMesh object.
 *
 * \param obj SkinnedMesh object
 */
static void m3gDestroySkinnedMesh(Object *obj)
{
    SkinnedMesh *mesh = (SkinnedMesh *) obj;
    M3G_VALIDATE_OBJECT(mesh);
    {
        int i;
        Interface *m3g = M3G_INTERFACE(mesh);
        
        m3gDeleteVertexBuffer(mesh->morphedVB);

        for (i = 0; i < mesh->bonesPerVertex; ++i) {
            m3gFree(m3g, mesh->boneIndices[i]);
            m3gFree(m3g, mesh->boneWeights[i]);
            m3gFree(m3g, mesh->normalizedWeights[i]);
        }
        m3gFree(m3g, mesh->weightShifts);
        
        for (i = 0; i < m3gArraySize(&mesh->bones); ++i) {
            m3gFree(m3g, m3gGetArrayElement(&mesh->bones, i));
        }
        m3gDestroyArray(&mesh->bones, m3g);
        
        if (mesh->skeleton != NULL) {
            m3gSetParent((Node*) mesh->skeleton, NULL);
            M3G_ASSIGN_REF(mesh->skeleton, NULL);
        }
    }
    m3gDestroyMesh(obj);
}


/*!
 * \internal
 * \brief Get a bone index for a given node
 *
 * This finds an existing record if the bone has been added
 * previously, or creates a new one if no record exists yet.
 *
 * \note Inline because only called from AddTransform.
 */
static M3G_INLINE M3Gint m3gBoneIndex(SkinnedMesh *mesh, Node *node)
{
    PointerArray *boneArray = &mesh->bones;
    const int numBones = m3gArraySize(boneArray);
    
    /* First look for an existing record in the array */
    {
        int i;
        
        for (i = 0; i < numBones; ++i) {
            Bone *b = m3gGetArrayElement(boneArray, i);
            if (b->node == node) {
                return i;
            }
        }
    }

    /* Not found; create a new one, append to the array, and set up
     * the "at-rest" transformation for the bone. Note, however, that
     * we can only store a maximum of 256 bones with byte indices! */
    {
        Interface *m3g = M3G_INTERFACE(mesh);
        
        if (numBones >= 256) {
            /* Out of available bone indices */
            m3gRaiseError(m3g, M3G_OUT_OF_MEMORY);
            return -1;
        }
        else {
            M3Gint idx;
            Bone *bone = (Bone*) m3gAllocZ(m3g, sizeof(Bone));
            if (!bone || !m3gGetTransformTo((Node*) mesh, node,
                                            &bone->toBone)) {
                m3gFree(m3g, bone);
                return -1; /* out of memory or singular transform */
            }
            bone->node = node;

            idx = m3gArrayAppend(boneArray, bone, m3g);
            if (idx < 0) {
                m3gFree(m3g, bone);
                return -1; /* out of memory */
            }
            return idx;
        }
    }
}

/*!
 * \internal
 * \brief Reallocate the per-vertex data if necessary.
 *
 * \note Inline because only called from AddTransform.
 */
static M3G_INLINE M3Gbool m3gEnsureVertexCount(SkinnedMesh *mesh, M3Gint count)
{
    /* Reallocate only if vertex count increased */
    
    if (count > mesh->weightedVertexCount) {
        
        Interface *m3g = M3G_INTERFACE(mesh);

        int i;

        /* Reallocate the weight shift array */
        {
            M3Gubyte *pNew = (M3Gubyte*) m3gAllocZ(m3g, count);
            if (!pNew) {
                return M3G_FALSE;
            }
            m3gCopy(pNew, mesh->weightShifts, mesh->weightedVertexCount);
            m3gFree(m3g, mesh->weightShifts);
            mesh->weightShifts = pNew;
        }

        /* Reallocate each of the bone index and weight arrays */
        
        for (i = 0; i < mesh->bonesPerVertex; ++i) {
            
            M3Gubyte *pNew;
            
            /* Weights */
            pNew = (M3Gubyte*) m3gAllocZ(m3g, count);
            if (!pNew) {
                return M3G_FALSE; /* out of memory */
            }
            m3gCopy(pNew, mesh->boneWeights[i], mesh->weightedVertexCount);
            m3gFree(m3g, mesh->boneWeights[i]);
            mesh->boneWeights[i] = pNew;
            
            pNew = (M3Gubyte*) m3gAllocZ(m3g, count);
            if (!pNew) {
                return M3G_FALSE; /* out of memory */
            }
            m3gCopy(pNew, mesh->normalizedWeights[i],
                    mesh->weightedVertexCount);
            m3gFree(m3g, mesh->normalizedWeights[i]);
            mesh->normalizedWeights[i] = pNew;

            /* Indices */
            pNew = (M3Gubyte*) m3gAllocZ(m3g, count);
            if (!pNew) {
                return M3G_FALSE; /* out of memory */
            }
            m3gCopy(pNew, mesh->boneIndices[i], mesh->weightedVertexCount);
            m3gFree(m3g, mesh->boneIndices[i]);
            mesh->boneIndices[i] = pNew;
        }

        mesh->weightedVertexCount = count;
    }
    return M3G_TRUE;
}
    
/*!
 * \internal
 * \brief Reallocate the per-vertex data if necessary.
 *
 * \note Inline because only called from AddTransform.
 */
static M3G_INLINE M3Gbool m3gEnsureBonesPerVertex(SkinnedMesh *mesh,
                                                  M3Gint count)
{
    M3G_ASSERT(count <= M3G_MAX_VERTEX_TRANSFORMS);

    /* Allocate only if per-vertex bone count increased */
    
    if (count > mesh->bonesPerVertex) {
        
        Interface *m3g = M3G_INTERFACE(mesh);
        
        const M3Gint vertexCount = mesh->weightedVertexCount;
        M3Gubyte *pNew;
        
        int i;

        /* Allocate new arrays for bone indices and weights until
         * we're satisfied */
        
        for (i = mesh->bonesPerVertex; i < count; ++i) {
            pNew = (M3Gubyte*) m3gAllocZ(m3g, vertexCount);
            if (!pNew) {
                goto AllocFailed; /* out of memory */
            }
            mesh->boneIndices[i] = pNew;
            
            pNew = (M3Gubyte*) m3gAllocZ(m3g, vertexCount);
            if (!pNew) {
                goto AllocFailed; /* out of memory */
            }
            mesh->boneWeights[i] = pNew;
            
            pNew = (M3Gubyte*) m3gAllocZ(m3g, vertexCount);
            if (!pNew) {
                goto AllocFailed; /* out of memory */
            }
            mesh->normalizedWeights[i] = pNew;
        }

        mesh->bonesPerVertex = count;
        return M3G_TRUE;

        /* In case of failure, clean up to keep the bonesPerVertex
         * counter in sync with the actual number of arrays
         * allocated */
    
    AllocFailed:
        for (i = mesh->bonesPerVertex; i < count; ++i) {
            m3gFree(m3g, mesh->boneIndices[i]);
            m3gFree(m3g, mesh->boneWeights[i]);
            m3gFree(m3g, mesh->normalizedWeights[i]);

            mesh->boneIndices[i] = NULL;
            mesh->boneWeights[i] = NULL;
            mesh->normalizedWeights[i] = NULL;
        }
        return M3G_FALSE;
    }
    return M3G_TRUE;
}
    
/*!
 * \internal
 * \brief Add a new bone influence to a vertex
 *
 * If the target vertex is already affected by
 * M3G_MAX_VERTEX_TRANSFORMS bones, the one with the lowest weight is
 * discarded.
 */
static M3G_INLINE void m3gAddInfluence(SkinnedMesh *mesh,
                                       M3Gint vertexIndex,
                                       M3Gint boneIndex,
                                       M3Gint weight) 
{
    M3Gint bonesPerVertex = mesh->bonesPerVertex;
    M3Guint minWeight = weight;
    M3Gint minWeightIndex = -1;
    int i;

    /* Shift the weight into the same scale with the other weights for
     * this vertex. */

    weight >>= mesh->weightShifts[vertexIndex];

    /* Look for an existing weight for our bone, or find the index
     * with the lowest weight if not found, and store it in
     * minWeightIndex.  Note that we're not separately tagging indices
     * as used/unused; unused ones will merely have a weight of
     * zero. */
        
    for (i = 0; i < bonesPerVertex; ++i) {

        /* If we find an existing weight for our bone, just add to
         * that and break out. Otherwise, keep track of the minimum
         * weight encountered so far. */
        
        if (mesh->boneIndices[i][vertexIndex] == boneIndex) {
            weight += mesh->boneWeights[i][vertexIndex];
            minWeightIndex = i;
            break;
        }
        else {
            M3Guint tempWeight = mesh->boneWeights[i][vertexIndex];
            if (tempWeight < minWeight) {
                minWeight = tempWeight;
                minWeightIndex = i;
            }
        }
    }

    /* Check whether our total weight exceeds the allocated range,
     * shifting all existing weights down if necessary */

    while (weight >= (1 << 8)) { /* byte range */
        weight >>= 1;
        mesh->weightShifts[vertexIndex] += 1;
        for (i = 0; i < bonesPerVertex; ++i) {
            mesh->boneWeights[i][vertexIndex] >>= 1;
        }
        M3G_ASSERT(mesh->weightShifts[vertexIndex] <= 31);
    }

    /* Add the index and weight contribution of the new
     * transformation, provided that the minimum weight found was
     * indeed smaller than the one we're adding */
        
    if (minWeightIndex >= 0) {
        mesh->boneIndices[minWeightIndex][vertexIndex] = (M3Gubyte) boneIndex;
        mesh->boneWeights[minWeightIndex][vertexIndex] = (M3Gubyte) weight;

        /* Need an update of the normalizing scales, too, as well as
         * the actual transformed vertices */
        
        mesh->weightsDirty = M3G_TRUE;
        m3gInvalidateNode((Node*) mesh, NODE_TRANSFORMS_BIT|NODE_BBOX_BIT);
    }
}

/*!
 * \internal
 * \brief Computes the normalization scales for vertex weights
 */
static void m3gNormalizeWeights(SkinnedMesh *mesh)
{
    const M3Gint bonesPerVertex = mesh->bonesPerVertex;
    const M3Gint vertexCount = mesh->weightedVertexCount;
    M3Gint vi;

    for (vi = 0; vi < vertexCount; ++vi) {
        M3Gint k;
        
        /* Sum up the 8-bit (possibly downshifted) weights */
        
        M3Guint sum = 0;
        for (k = 0; k < bonesPerVertex; ++k) {
            sum += mesh->boneWeights[k][vi];
        }

        /* Compute an 8.24 reciprocal of the weights, then scale with
         * that to normalize, and shift to 1.7 fixed point */
        {
            M3Guint s = (sum > 0 ? (1U << 24) / sum : 0);

            sum = 0;
            for (k = 0; k < bonesPerVertex; ++k) {
                M3Guint normalized = (s * mesh->boneWeights[k][vi]) >> 17;
                M3G_ASSERT(m3gInRange((M3Gint)normalized, 0, 128));
                sum += normalized;
                mesh->normalizedWeights[k][vi] = (M3Gubyte) normalized;
            }
            
            /* NOTE there is a maximum of ½ rounding error per
             * component, plus the rounding error from the reciprocal
             * calculation, so the sum of weights will often not sum
             * to 128 exactly! We therefore only assert against
             * clearly out-of-range values here */
            
            M3G_ASSERT(sum == 0 || m3gInRange((M3Gint) sum, 96, 128));
        }
    }

    mesh->weightsDirty = M3G_FALSE;
}

/*!
 * \internal
 * \brief Computes an optimal exponent value for a fixed point
 * transformation
 *
 * This scales the translation exponent up to optimally utilize the
 * 32-bit intermediate precision if the matrix exponent is smaller.
 */
static M3Gint m3gOptimalExponent(M3Gint matrixExp, M3Gint transExp)
{
    M3Gint maxExp = matrixExp;
    M3Gint shift = transExp - matrixExp;
    if (shift > 0) {

        /* The matrix part will always occupy less than half of the
         * available range if shifted down by at least one bit, so we
         * can shift the translation up by a maximum of 15 bits.  If
         * the matrix is shifted by more than 31 bits, it will always
         * flush to zero, freeing the full 32-bit range for the
         * translation alone. */

        if (shift >= 32) {      /* matrix will flush to zero */
            shift = 16;
        }
        else if (shift >= 16) { /* matrix always < half of the range */
            shift = 15;
        }
        else {
            shift -= 1;     /* shift matrix by at least one bit */
        }
        
        maxExp = transExp - shift;
    }
    
    M3G_ASSERT(maxExp >= matrixExp && maxExp >= transExp - 16);
    return maxExp;
}

/*
 * \brief Fixed point vertex transformation
 *
 * \param mtx        pointer to a 3x3 16-bit matrix
 * \param mtxExp     exponent for the matrix elements (upshift from int)
 * \param trans      pointer to 3-element 16-bit translation vector
 * \param transExp   exponent for the translation vector
 * \param maxExp     precalculated "optimal" exponent
 * \param vx         vertex X coordinate (16-bit range)
 * \param vy         vertex Y coordinate (16-bit range)
 * \param vz         vertex Z coordinate (16-bit range)
 * \param out        output vertex, 25 bits of precision
 * \return exponent value for \c out
 */
static M3Gint m3gFixedPointTransform(const M3Gshort *mtx, M3Gint mtxExp,
                                     const M3Gshort *trans, M3Gint transExp,
                                     M3Gint maxExp,
                                     M3Gint vx, M3Gint vy, M3Gint vz,
                                     M3Gint *out)
{
    M3Gint shift;
    M3Gint ox = 0, oy = 0, oz = 0;
    
    /* First put in the translation part, upscaled to the optimal
     * range for this bone */

    if (trans) {
        shift = maxExp - (transExp - 16);
        M3G_ASSERT(shift >= 0);
        if (shift < 32) {
            ox += ((M3Gint) trans[0] << 16) >> shift;
            oy += ((M3Gint) trans[1] << 16) >> shift;
            oz += ((M3Gint) trans[2] << 16) >> shift;
        }
    }
        
    /* Add the input multiplied with the base 3x3 matrix and shifted
     * to the "maxExp" scale, provided that it has any effect on the
     * outcome */
    
    shift = maxExp - mtxExp;
    M3G_ASSERT(shift >= 0);
    if (shift < 32) {
        
#       if defined(M3G_DEBUG)
        M3Gint iMin = (-1 << 31) + (65535 * 32768 >> shift);
        M3Gint iMax = (M3Gint)((1u << 31)-1) - (65535 * 32768 >> shift);
        M3G_ASSERT(m3gInRange(ox, iMin, iMax));
        M3G_ASSERT(m3gInRange(oy, iMin, iMax));
        M3G_ASSERT(m3gInRange(oz, iMin, iMax));
#       endif /* M3G_DEBUG */
        
        ox += (mtx[0] * vx + mtx[3] * vy + mtx[6] * vz) >> shift;
        oy += (mtx[1] * vx + mtx[4] * vy + mtx[7] * vz) >> shift;
        oz += (mtx[2] * vx + mtx[5] * vy + mtx[8] * vz) >> shift;
    }

    /* Shift the output down to fit into 25 bits; we're dropping 7
     * bits of precision here, so adjust the exponent accordingly */

    out[0] = ox >> 7;
    out[1] = oy >> 7;
    out[2] = oz >> 7;
    return maxExp + 7;
}

/*!
 * \internal
 * \brief Applies scale and bias to a vertex
 *
 * This is required for vertices that have no bones attached.
 * 
 * \param mesh    the SkinnedMesh object
 * \param vx      vertex X coordinate (16-bit range)
 * \param vy      vertex Y coordinate (16-bit range)
 * \param vz      vertex Z coordinate (16-bit range)
 * \param upshift scaling value for the input coordinates and the
 *                translation component of the transformation
 * \param vertex  output vertex position
 * \return exponent value for \c vertex
 */
static M3Gint m3gScaleAndBiasVertex(const SkinnedMesh *mesh,
                                    M3Gint vx, M3Gint vy, M3Gint vz,
                                    M3Gint upshift,
                                    M3Gshort *vertex)
{
    M3Gint temp[3];
    M3Gint expo;

    M3G_ASSERT(m3gInRange(vx, -1 << 15, (1 << 15) - 1));
    M3G_ASSERT(m3gInRange(vy, -1 << 15, (1 << 15) - 1));
    M3G_ASSERT(m3gInRange(vz, -1 << 15, (1 << 15) - 1));
    
    expo = m3gFixedPointTransform(mesh->scaleMatrix, mesh->scaleExp,
                                  mesh->biasVector, mesh->biasExp + upshift,
                                  mesh->scaleBiasExp,
                                  vx << upshift, vy << upshift, vz << upshift,
                                  temp) - upshift;

    /* Scale down from 25 to 16 bits, adjusting the exponent
     * accordingly */
    
    vertex[0] = (M3Gshort)(temp[0] >> 9);
    vertex[1] = (M3Gshort)(temp[1] >> 9);
    vertex[2] = (M3Gshort)(temp[2] >> 9);
    expo += 9;
    
    M3G_ASSERT(m3gInRange(expo, -127, 127));
    return expo;
}

/*!
 * \internal
 * \brief Computes the blended position for a single vertex
 *
 * \param mesh    the SkinnedMesh object
 * \param vidx    vertex index (for accessing bone data)
 * \param vx      vertex X coordinate (16-bit range)
 * \param vy      vertex Y coordinate (16-bit range)
 * \param vz      vertex Z coordinate (16-bit range)
 * \param upshift scaling value for the input coordinates and the
 *                translation component of the transformation
 * \param vertex  output vertex position
 * \return exponent value for \c vertex
 */
static M3Gint m3gBlendVertex(const SkinnedMesh *mesh,
                             M3Gint vidx,
                             M3Gint vx, M3Gint vy, M3Gint vz,
                             M3Gint upshift,
                             M3Gshort *vertex)
{
    const M3Gint boneCount = mesh->bonesPerVertex;
    const PointerArray *boneArray = &mesh->bones;
    M3Gint i;
    
    M3Gint outExp = -128;
    M3Gint sumWeights = 0;
    
    M3Gint ox = 0, oy = 0, oz = 0;
    
    vx <<= upshift;
    vy <<= upshift;
    vz <<= upshift;

    M3G_ASSERT(m3gInRange(vx, -1 << 15, (1 << 15) - 1));
    M3G_ASSERT(m3gInRange(vy, -1 << 15, (1 << 15) - 1));
    M3G_ASSERT(m3gInRange(vz, -1 << 15, (1 << 15) - 1));

    /* Loop over the bones and sum the contribution from each */
    
    for (i = 0; i < boneCount; ++i) {
        
        M3Gint weight = (M3Gint) mesh->normalizedWeights[i][vidx];
        sumWeights += weight;

        /* Skip bones with zero weights */
        
        if (weight > 0) {
            
            const Bone *bone = (const Bone *)
                m3gGetArrayElement(boneArray, mesh->boneIndices[i][vidx]);
            M3Gint temp[3];
            M3Gint shift;

            shift = m3gFixedPointTransform(bone->baseMatrix, bone->baseExp,
                                           bone->posVec, bone->posExp + upshift,
                                           bone->maxExp,
                                           vx, vy, vz,
                                           temp);

            shift = outExp - shift;
            if (shift < 0) {
                shift = -shift;
                if (shift < 31) {
                    ox >>= shift;
                    oy >>= shift;
                    oz >>= shift;
                }
                else {
                    ox = oy = oz = 0;
                }
                outExp += shift;
                shift = 0;
            }

            /* Apply the vertex weights: 1.7 * 25.0 -> 26.7, but since
             * the weights are positive and sum to 1, we should stay
             * within the 32-bit range */
            
            if (shift < 31) {
                
                M3G_ASSERT(m3gInRange(temp[0], -1 << 24, (1 << 24) - 1));
                M3G_ASSERT(m3gInRange(temp[1], -1 << 24, (1 << 24) - 1));
                M3G_ASSERT(m3gInRange(temp[2], -1 << 24, (1 << 24) - 1));
                
                ox += (weight * temp[0]) >> shift;
                oy += (weight * temp[1]) >> shift;
                oz += (weight * temp[2]) >> shift;
            }
        }
    }

    /* Before returning, we still need to check for the special case
     * of all-zero weights, and shift the values from the post-scaling
     * 32-bit precision back into the 16-bit range; we're essentially
     * dropping the (25 - 16) bits of the blended result, so the
     * exponent must change accordingly */

    if (sumWeights > 0) {
        vertex[0] = (M3Gshort)(ox >> 16);
        vertex[1] = (M3Gshort)(oy >> 16);
        vertex[2] = (M3Gshort)(oz >> 16);
        outExp = outExp - upshift + 9;

        M3G_ASSERT(m3gInRange(outExp, -127, 127));
        return outExp;
    }
    else {
        vx >>= upshift;
        vy >>= upshift;
        vz >>= upshift;
        return m3gScaleAndBiasVertex(mesh, vx, vy, vz, upshift, vertex);
    }
}

/*!
 * \internal
 * \brief Computes the blended normal vector for a single vertex
 *
 * \param mesh    the SkinnedMesh object
 * \param vidx    vertex index (for accessing bone data)
 * \param nx      normal X coordinate (16-bit range)
 * \param ny      normal Y coordinate (16-bit range)
 * \param nz      normal Z coordinate (16-bit range)
 * \param upshift scaling for input coordinates to increase precision
 * \param normal  output normal vector (8-bit range!)
 * \return a shift value for the output vertex (scale from integer)
 */
static void m3gBlendNormal(const SkinnedMesh *mesh,
                           M3Gint vidx,
                           M3Gint nx, M3Gint ny, M3Gint nz,
                           M3Gint upshift,
                           M3Gbyte *normal)
{
    const M3Gint boneCount = mesh->bonesPerVertex;
    const PointerArray *boneArray = &mesh->bones;
    M3Gint i;
    
    M3Gint outExp = -128;
    M3Gint sumWeights = 0;
    
    M3Gint ox = 0, oy = 0, oz = 0;

    nx <<= upshift;
    ny <<= upshift;
    nz <<= upshift;
    
    M3G_ASSERT(m3gInRange(nx, -1 << 15, (1 << 15) - 1));
    M3G_ASSERT(m3gInRange(ny, -1 << 15, (1 << 15) - 1));
    M3G_ASSERT(m3gInRange(nz, -1 << 15, (1 << 15) - 1));

    /* Loop over the bones and sum the contribution from each */
    
    for (i = 0; i < boneCount; ++i) {
        
        M3Gint weight = (M3Gint) mesh->normalizedWeights[i][vidx];
        sumWeights += weight;

        /* Skip bones with zero weights */
        
        if (weight > 0) {
            
            const Bone *bone = (const Bone *)
                m3gGetArrayElement(boneArray, mesh->boneIndices[i][vidx]);
            M3Gint temp[3];
            M3Gint shift;

            shift = m3gFixedPointTransform(bone->normalMatrix, 0,
                                           NULL, 0,
                                           0,
                                           nx, ny, nz,
                                           temp);

            shift = outExp - shift;
            if (shift < 0) {
                shift = -shift;
                if (shift < 31) {
                    ox >>= shift;
                    oy >>= shift;
                    oz >>= shift;
                }
                else {
                    ox = oy = oz = 0;
                }
                outExp += shift;
                shift = 0;
            }

            /* Apply the vertex weights: 1.7 * 25.0 -> 26.7, but since
             * the weights are positive and sum to 1, we should stay
             * within the 32-bit range */
            
            if (shift < 31) {
                
                M3G_ASSERT(m3gInRange(temp[0], -1 << 24, (1 << 24) - 1));
                M3G_ASSERT(m3gInRange(temp[1], -1 << 24, (1 << 24) - 1));
                M3G_ASSERT(m3gInRange(temp[2], -1 << 24, (1 << 24) - 1));
                
                ox += (weight * temp[0]) >> shift;
                oy += (weight * temp[1]) >> shift;
                oz += (weight * temp[2]) >> shift;
            }
        }
    }

    /* Before returning, we still need to check for the special case
     * of all-zero weights, and shift the values from the post-scaling
     * 32-bit precision down into the 8-bit range */

    if (sumWeights > 0) {
        normal[0] = (M3Gbyte)(ox >> 24);
        normal[1] = (M3Gbyte)(oy >> 24);
        normal[2] = (M3Gbyte)(oz >> 24);
    }
    else {
        normal[0] = (M3Gbyte)(ox >> 8);
        normal[1] = (M3Gbyte)(oy >> 8);
        normal[2] = (M3Gbyte)(oz >> 8);
    }
}

/*!
 * \internal
 * \brief Updates internal vertex buffer
 *
 * \param mesh SkinnedMesh object
 *
 * \retval M3G_TRUE VertexBuffer is up to date
 * \retval M3G_FALSE Failed to update VertexBuffer, out of memory exception raised
 */
static M3Gbool m3gSkinnedMeshUpdateVB(SkinnedMesh *mesh)
{
    M3Gint vbTimestamp;
    M3G_ASSERT(mesh->mesh.vertexBuffer != NULL);
    M3G_ASSERT(mesh->morphedVB != NULL);
    
    /* Source vertex buffer array configuration changed since last
     * update? */

    vbTimestamp = m3gGetTimestamp(mesh->mesh.vertexBuffer);
    
    if (mesh->vbTimestamp != vbTimestamp) {
        Interface *m3g = M3G_INTERFACE(mesh);
        VertexArray *array;
        M3Gint vcount = m3gGetVertexCount(mesh->mesh.vertexBuffer);

        /* Must ensure that our internal morphing buffer matches the
         * configuration of the source buffer, with dedicated arrays
         * for the morphed positions and normals */
        
        if (!m3gMakeModifiedVertexBuffer(mesh->morphedVB,
                                         mesh->mesh.vertexBuffer,
                                         M3G_POSITION_BIT|M3G_NORMAL_BIT,
                                         M3G_FALSE)) {
            return M3G_FALSE; /* out of memory */
        }

        /* We always have the vertex positions as shorts, but the
         * array may not be actually initialized yet, so we must check
         * whether to create a copy or not */

        if (mesh->mesh.vertexBuffer->vertices) {
            array = m3gCreateVertexArray(m3g, vcount, 3, M3G_SHORT);
            if (!array) {
                return M3G_FALSE;
            }
            m3gSetVertexArray(mesh->morphedVB, array, 1.f, NULL, 0);
        }

        /* Normals (always bytes) only exist if in the original VB */
        
        if (mesh->mesh.vertexBuffer->normals) {
            array = m3gCreateVertexArray(m3g, vcount, 3, M3G_BYTE);
            if (!array) {
                return M3G_FALSE;
            }
            m3gSetNormalArray(mesh->morphedVB, array);
        }
    
        mesh->vbTimestamp = vbTimestamp;
    }
    
    /* The default color must always be updated, because it can be
     * animated (doesn't affect timestamp) */
    
    mesh->morphedVB->defaultColor = mesh->mesh.vertexBuffer->defaultColor;
    return M3G_TRUE;
}


/*!
 * \internal
 * \brief Gets the transformation(s) for a single bone record
 *
 * Also stores the normal transformation matrix if needed.
 *
 * \param mesh       pointer to the mesh object
 * \param bone       pointer to the bone record
 * \param hasNormals flag indicating whether the normals transformation
 *                   should be computed and cached in the bone record
 * \param mtx        matrix to store the vertex transformation in
 */
static M3G_INLINE M3Gbool m3gGetBoneTransformInternal(SkinnedMesh *mesh,
                                              Bone *bone,
                                              M3Gbool hasNormals,
                                              Matrix *mtx)
{
    const VertexBuffer *vb = mesh->mesh.vertexBuffer;

    /* Get the vertex transformation and concatenate it with the
     * at-rest matrix and the vertex scale and bias transformations.
     * The resulting 3x4 transformation matrix is then split into a
     * fixed point 3x3 matrix and translation vector */
    
    if (!m3gGetTransformTo(bone->node, (Node*) mesh, mtx)) {
        return M3G_FALSE; /* no path or singular transform */
    }
    m3gMulMatrix(mtx, &bone->toBone);

    /* If normals are enabled, compute and store the inverse transpose
     * matrix for transforming normals at this stage */
    
    if (hasNormals) {
        Matrix t;
        if (!m3gInverseTranspose(&t, mtx)) {
            m3gRaiseError(M3G_INTERFACE(mesh), M3G_ARITHMETIC_ERROR);
            return M3G_FALSE; /* singular transform */
        }
        m3gGetFixedPoint3x3Basis(&t, bone->normalMatrix);
    }

    /* Apply the vertex bias and scale to the transformation */
    
    m3gTranslateMatrix(
        mtx, vb->vertexBias[0], vb->vertexBias[1], vb->vertexBias[2]);
    m3gScaleMatrix(mtx, vb->vertexScale, vb->vertexScale, vb->vertexScale);
    
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Compute and cache the bone transformations for morphing
 *
 * \param mesh     the SkinnedMesh object
 * \param posShift vertex position value "gain"
 */
static M3Gbool m3gPreComputeTransformations(SkinnedMesh *mesh,
                                            M3Gint posShift,
                                            M3Gbool hasNormals)
{
    M3Gint boneCount = m3gArraySize(&mesh->bones);
    M3Gint i;
    Matrix *tBone = NULL;

    /* First, just compute the floating point transformation matrices
     * for the bones, caching them in a temp array */

    if (boneCount > 0) {
        tBone = m3gAllocTemp(M3G_INTERFACE(mesh), boneCount * sizeof(Matrix));
        if (!tBone) {
            return M3G_FALSE; /* out of memory */
        }    
        for (i = 0; i < boneCount; ++i) {
            Bone *bone = m3gGetArrayElement(&mesh->bones, i);
            if (!m3gGetBoneTransformInternal(mesh, bone, hasNormals, &tBone[i])) {
                return M3G_FALSE;
            }
        }
    }

    /* Find the value range of the bone translations, and offset the
     * bones to center output vertex values (roughly) around the
     * origin */
    {
        const VertexBuffer *vb = mesh->mesh.vertexBuffer;
        M3Gfloat min[3], max[3], bias[3];
        M3Gint maxExp;
        Vec4 t;

        /* Find the minimum and maximum values; start with the plain
         * vertex bias for non-weighted bones */
        
        min[0] = max[0] = vb->vertexBias[0];
        min[1] = max[1] = vb->vertexBias[1];
        min[2] = max[2] = vb->vertexBias[2];
        
        for (i = 0; i < boneCount; ++i) {
            m3gGetMatrixColumn(&tBone[i], 3, &t); 
            min[0] = M3G_MIN(min[0], t.x);
            max[0] = M3G_MAX(max[0], t.x);
            min[1] = M3G_MIN(min[1], t.y);
            max[1] = M3G_MAX(max[1], t.y);
            min[2] = M3G_MIN(min[2], t.z);
            max[2] = M3G_MAX(max[2], t.z);
        }
        
        /* Divide to get the mean translation, store in the
         * destination VB, and invert for de-biasing the bones */
        
        for (i = 0; i < 3; ++i) {
            bias[i] = m3gMul(0.5f, m3gAdd(min[i], max[i]));
            mesh->morphedVB->vertexBias[i] = bias[i];
            bias[i] = m3gNegate(bias[i]);
        }
        
        /* Offset bones by the (now inverted) bias vector, and store
         * the fixed point matrix & vector parts in the bone record;
         * also set the maximum bone exponent into the mesh */

        maxExp = -128;
        for (i = 0; i < boneCount; ++i) {
            Bone *bone = m3gGetArrayElement(&mesh->bones, i);
            m3gPreTranslateMatrix(&tBone[i], bias[0], bias[1], bias[2]); /*lint !e613 tBone not null if boneCount > 0 */
            
            bone->baseExp = (M3Gshort)
                m3gGetFixedPoint3x3Basis(&tBone[i], bone->baseMatrix); /*lint !e613 tBone not null if boneCount > 0 */
            bone->posExp = (M3Gshort)
                m3gGetFixedPointTranslation(&tBone[i], bone->posVec); /*lint !e613 tBone not null if boneCount > 0 */
            bone->maxExp = (M3Gshort)
                m3gOptimalExponent(bone->baseExp, bone->posExp + posShift);

            maxExp = M3G_MAX(maxExp, bone->maxExp);
        }

        /* Make a fixed-point matrix for applying the scale and bias as
         * well, for vertices not attached to any bone (this is not the
         * optimal way to store the information, but we can just reuse
         * existing code this way) */
        {
            Matrix sb;
            m3gTranslationMatrix(&sb,
                                 m3gAdd(bias[0], vb->vertexBias[0]),
                                 m3gAdd(bias[1], vb->vertexBias[1]),
                                 m3gAdd(bias[2], vb->vertexBias[2]));
            m3gScaleMatrix(&sb,
                           vb->vertexScale, vb->vertexScale, vb->vertexScale);
            
            mesh->scaleExp = (M3Gshort)
                m3gGetFixedPoint3x3Basis(&sb, mesh->scaleMatrix);
            mesh->biasExp = (M3Gshort)
                m3gGetFixedPointTranslation(&sb, mesh->biasVector);
            mesh->scaleBiasExp = (M3Gshort)
                m3gOptimalExponent(mesh->scaleExp, mesh->biasExp + posShift);
        
            maxExp = M3G_MAX(mesh->scaleBiasExp, maxExp);
        }

        /* Compute the maximum post-blending exponent and store it as the
         * morphed vertex buffer scale -- this is dependent on the
         * implementations of m3gBlendVertex, m3gScaleAndBiasVertex, and
         * m3gFixedPointTransform! */

        maxExp = maxExp + 16 - posShift;
        M3G_ASSERT(m3gInRange(maxExp, -127, 127));
        *(M3Gint*)&mesh->morphedVB->vertexScale = (maxExp + 127) << 23;
        mesh->maxExp = (M3Gshort) maxExp;
    }
    
    if (boneCount > 0) {
        m3gFreeTemp(M3G_INTERFACE(mesh));
    }
    
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Computes derived data required for bounding volumes and skinning
 */
static M3Gbool m3gSkinnedMeshPreMorph(SkinnedMesh *mesh)
{
    const VertexBuffer *srcVB = mesh->mesh.vertexBuffer;
    M3Gint posShift = 0, normalShift = 0;
    
    /* Compute upscaling shift values for positions and normals so
     * that we can maximize precision even for absurdly small
     * vertex values */
    {
        M3Gint minVal, maxVal;
        
        if (srcVB->normals) {
            m3gGetArrayValueRange(srcVB->normals, &minVal, &maxVal);
            maxVal = M3G_MAX(-minVal, maxVal);
            M3G_ASSERT(maxVal >= 0);
            if (maxVal) {
                while ((maxVal << normalShift) < (1 << 14)) {
                    ++normalShift;
                }
            }
        }
            
        m3gGetArrayValueRange(srcVB->vertices, &minVal, &maxVal);
        maxVal = M3G_MAX(-minVal, maxVal);
        M3G_ASSERT(maxVal >= 0);
        if (maxVal) {
            while ((maxVal << posShift) < (1 << 14)) {
                ++posShift;
            }
        }
        
        mesh->posShift    = (M3Gshort) posShift;
        mesh->normalShift = (M3Gshort) normalShift;
    }

    /* Now that we can compute the optimized exponents for the
     * transformations based on the position upshift value, let's
     * resolve the bone transformations; this will also cache the
     * maximum bone exponent in mesh->maxExp */

    if (!m3gPreComputeTransformations(mesh,
                                      posShift,
                                      srcVB->normals != NULL)) { 
        return M3G_FALSE; /* invalid transform */
    }
    
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Does the actual vertex morphing into the internal vertex buffer
 *
 * \param mesh   SkinnedMesh object
 * \retval M3G_TRUE     skinning ok
 * \retval M3G_FALSE    skinning failed, exception raised
 */
static void m3gSkinnedMeshMorph(SkinnedMesh *mesh)
{
    const VertexBuffer *srcVB = mesh->mesh.vertexBuffer;
    const void *srcPositions;
    const void *srcNormals = NULL;
    VertexBuffer *dstVB = mesh->morphedVB;
    M3Gshort *dstPositions;
    M3Gbyte *dstNormals = NULL;
    M3Gint vertexCount = mesh->weightedVertexCount;
    M3Gint maxExp = mesh->maxExp;
    M3Gint posShift = mesh->posShift, normShift = mesh->normalShift;
    M3Gint i;

    M3G_ASSERT(!((Node*) mesh)->dirtyBits);
    
    /* Let's update the vertex weights if we need to */
        
    if (mesh->weightsDirty) {
        m3gNormalizeWeights(mesh);
    }

    /* Get pointers to source and destination position and normal
     * data; the latter will always be shorts and bytes,
     * respectively, while the former can be either */
        
    srcPositions = m3gMapVertexArrayReadOnly(srcVB->vertices);
    dstPositions = (M3Gshort*) m3gMapVertexArray(dstVB->vertices);
    if (srcVB->normals) {
        srcNormals = m3gMapVertexArrayReadOnly(srcVB->normals);
        dstNormals = (M3Gbyte*) m3gMapVertexArray(dstVB->normals);
    }
        
    /* Transform the vertices that are affected by bones */
    {
        M3Gshort *dst = dstPositions;
            
        if (srcVB->vertices->elementType == GL_BYTE) {
            const M3Gbyte *src = (const M3Gbyte*) srcPositions;
            for (i = 0; i < vertexCount; ++i) {
                M3Gint shift =
                    maxExp - m3gBlendVertex(mesh, i,
                                            src[0], src[1], src[2],
                                            posShift,
                                            dst);
                if (shift > 31) {
                    *dst++ = 0;
                    *dst++ = 0;
                    *dst++ = 0;
                }
                else {
                    *dst++ >>= shift;
                    *dst++ >>= shift;
                    *dst++ >>= shift;
                }
            
                src += 4; /* byte data always padded to 32 bits */
            }
        }
        else {
            const M3Gshort *src = (const M3Gshort*) srcPositions;
            for (i = 0; i < vertexCount; ++i) {
                M3Gint shift =
                    maxExp - m3gBlendVertex(mesh, i,
                                            src[0], src[1], src[2],
                                            posShift,
                                            dst);
                if (shift > 31) {
                    *dst++ = 0;
                    *dst++ = 0;
                    *dst++ = 0;
                }
                else {
                    *dst++ >>= shift;
                    *dst++ >>= shift;
                    *dst++ >>= shift;
                }
                
                src += 3;
            }
        }
    }

    /* Transform the normals (if enabled).  Normals will be
     * normalized when rendering, so no need to keep track of
     * scales here */
        
    if (srcNormals) {
        M3Gbyte *dst = dstNormals;
            
        if (srcVB->normals->elementType == GL_BYTE) {
            const M3Gbyte *src = (const M3Gbyte*) srcNormals;
            for (i = 0; i < vertexCount; ++i) {
                m3gBlendNormal(mesh, i,
                               src[0], src[1], src[2],
                               normShift,
                               dst);
                src += 4; /* byte data padded to 32 bits */
                dst += 4; 
            }
        }
        else {
            const M3Gshort *src = (const M3Gshort*) srcNormals;
            for (i = 0; i < vertexCount; ++i) {
                m3gBlendNormal(mesh, i,
                               src[0], src[1], src[2],
                               normShift,
                               dst);
                src += 3;
                dst += 4; 
            }
        }
    }

    /* Finally, handle the remaining vertices, which have no bones
     * attached; these just need to have the scale and bias
     * applied */

    vertexCount = m3gGetNumVertices(srcVB);
    if (i < vertexCount) {
            
        M3Gint startIndex = i;
        M3Gshort *dstPos = dstPositions + startIndex * 3;
        M3Gshort temp[3];
            
        if (srcVB->vertices->elementType == GL_BYTE) {
            const M3Gbyte *src = ((const M3Gbyte*) srcPositions) + startIndex * 4;
            for (i = startIndex ; i < vertexCount; ++i) {
                M3Gint shift =
                    maxExp - m3gScaleAndBiasVertex(mesh,
                                                   src[0], src[1], src[2],
                                                   posShift,
                                                   temp);
                *dstPos++ = (M3Gshort)(temp[0] >> shift);
                *dstPos++ = (M3Gshort)(temp[1] >> shift);
                *dstPos++ = (M3Gshort)(temp[2] >> shift);                    
                src += 4; /* byte data, padded to 32 bits */
            }
        }
        else {
            const M3Gshort *src = ((const M3Gshort*) srcPositions) + startIndex * 3;
            for (i = startIndex ; i < vertexCount; ++i) {
                M3Gint shift =
                    maxExp - m3gScaleAndBiasVertex(mesh,
                                                   src[0], src[1], src[2],
                                                   posShift,
                                                   temp);
                *dstPos++ = (M3Gshort)(temp[0] >> shift);
                *dstPos++ = (M3Gshort)(temp[1] >> shift);
                *dstPos++ = (M3Gshort)(temp[2] >> shift);                    
                src += 3;
            }
        }
            
        /* Byte normals can just use a memcopy, as we don't have
         * to scale them at all; shorts will require a conversion,
         * after prescaling with the normal upshift to avoid
         * underflowing to zero */
                
        if (srcNormals) {
            M3Gbyte *dstNorm = dstNormals + startIndex * 4; 
            if (srcVB->normals->elementType == GL_BYTE) {
                const M3Gbyte *src =
                    ((const M3Gbyte*) srcNormals) + startIndex * 4;
                m3gCopy(dstNorm, src, (vertexCount - startIndex) * 4);
            }
            else {
                const M3Gshort *src =
                    ((const M3Gshort*) srcNormals) + startIndex * 3;
                for (i = startIndex ; i < vertexCount; ++i) {
                    *dstNorm++ = (M3Gbyte)((*src++ << normShift) >> 8);
                    *dstNorm++ = (M3Gbyte)((*src++ << normShift) >> 8);
                    *dstNorm++ = (M3Gbyte)((*src++ << normShift) >> 8);
                    ++dstNorm; /* again, padding for byte values */
                }
            }
        }
    }
        
    /* All done! Clean up and exit */

    m3gUnmapVertexArray(srcVB->vertices);
    m3gUnmapVertexArray(dstVB->vertices);
    if (srcNormals) {
        m3gUnmapVertexArray(srcVB->normals);
        m3gUnmapVertexArray(dstVB->normals);
    }
}

/*!
 * \internal
 * \brief Overloaded Node method.
 *
 * Setup skinned mesh render. Call mesh render setup,
 * do skinning calculations and traverse into the skeleton or the parent
 *
 * \param self SkinnedMesh object
 * \param toCamera transform to camera
 * \param alphaFactor total alpha factor
 * \param caller caller node
 * \param renderQueue RenderQueue
 *
 * \retval M3G_TRUE continue render setup
 * \retval M3G_FALSE abort render setup
 */
static M3Gbool m3gSkinnedMeshSetupRender(Node *self,
                                         const Node *caller,
                                         SetupRenderState *s,
                                         RenderQueue *renderQueue)
{
    SkinnedMesh *mesh = (SkinnedMesh *)self;
    Node *skeleton = (Node*) mesh->skeleton;
    M3Gbool enabled, success = M3G_TRUE;
    m3gIncStat(M3G_INTERFACE(self), M3G_STAT_RENDER_NODES, 1);
    
    /* Optimize the rendering-enable checking for top-down traversal */

    enabled = (self->enableBits & NODE_RENDER_BIT) != 0;
    if (caller != self->parent) {
        enabled = m3gHasEnabledPath(self, renderQueue->root);
        s->cullMask = CULLMASK_ALL;
    }

    /* Handle self and the skeleton if enabled */

    if (enabled) {
        
        /* Traverse into the skeleton unless coming from there */
    
        if (skeleton != caller) {
            SetupRenderState cs;
            cs.cullMask = s->cullMask;
        
            M3G_BEGIN_PROFILE(M3G_INTERFACE(self), M3G_PROFILE_SETUP_TRANSFORMS);
            m3gGetCompositeNodeTransform(skeleton, &cs.toCamera);
            m3gPreMultiplyMatrix(&cs.toCamera, &s->toCamera);
            M3G_END_PROFILE(M3G_INTERFACE(self), M3G_PROFILE_SETUP_TRANSFORMS);
        
            success = M3G_VFUNC(Node, skeleton, setupRender)(skeleton,
                                                             self,
                                                             &cs,
                                                             renderQueue);
        }

        /* Handle self if in scope */
        
        if ((self->scope & renderQueue->scope) != 0) {

            /* Try view frustum culling */

#           if defined(M3G_ENABLE_VF_CULLING)
            m3gUpdateCullingMask(s, renderQueue->camera, &mesh->bbox);
#           endif

            if (s->cullMask == 0) {
                m3gIncStat(M3G_INTERFACE(self),
                           M3G_STAT_RENDER_NODES_CULLED, 1);
            }
            else {
                success &= m3gQueueMesh((Mesh*) self, &s->toCamera, renderQueue);
                
                if (success) {
                    M3G_BEGIN_PROFILE(M3G_INTERFACE(self), M3G_PROFILE_SKIN);
                    m3gSkinnedMeshMorph(mesh);
                    M3G_END_PROFILE(M3G_INTERFACE(self), M3G_PROFILE_SKIN);
                }
            }
        }
    }

    /* Traverse into the parent node unless coming from there.  Again,
     * discard the old traversal state at this point, as we're not
     * coming back. */
    
    if (success && self != renderQueue->root) {
        Node *parent = self->parent;
        if (parent != NULL && parent != caller) {
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
 * \brief Overloaded Node method.
 *
 * Renders one skinned submesh.
 *
 * \param self SkinnedMesh object
 * \param ctx current render context
 * \param patchIndex submesh index
 */
static void m3gSkinnedMeshDoRender(Node *self,
                                   RenderContext *ctx,
                                   const Matrix *toCamera,
                                   int patchIndex)
{
    SkinnedMesh *mesh = (SkinnedMesh *)self;
    IndexBuffer *indexBuffer = mesh->mesh.indexBuffers[patchIndex];
    Appearance *appearance = mesh->mesh.appearances[patchIndex];

    if (indexBuffer == NULL || appearance == NULL)
        return;

    m3gDrawMesh(ctx,
                mesh->morphedVB,
                indexBuffer,
                appearance,
                toCamera,
                mesh->mesh.totalAlphaFactor + 1,
                mesh->mesh.node.scope);
}

/*!
 * \internal
 * \brief Overloaded Node method.
 *
 * Do skinning calculations and forward to Mesh internal ray intersect.
 *
 * \param self      SkinnedMesh object
 * \param mask      pick scope mask
 * \param ray       pick ray
 * \param ri        RayIntersection object
 * \param toGroup   transform to originating group
 * \retval          M3G_TRUE    continue pick
 * \retval          M3G_FALSE   abort pick
 */
static M3Gbool m3gSkinnedMeshRayIntersect(  Node *self,
                                            M3Gint mask,
                                            M3Gfloat *ray,
                                            RayIntersection *ri,
                                            Matrix *toGroup)
{
    SkinnedMesh *mesh = (SkinnedMesh *)self;
    M3G_BEGIN_PROFILE(M3G_INTERFACE(self), M3G_PROFILE_SKIN);

    if ((((Node *)mesh)->scope & mask) == 0) {
        return M3G_TRUE;
    }

    if (!m3gSkinnedMeshPreMorph(mesh)) {
        return M3G_FALSE;
    }
    m3gSkinnedMeshMorph(mesh);
    M3G_END_PROFILE(M3G_INTERFACE(self), M3G_PROFILE_SKIN);
    return m3gMeshRayIntersectInternal( &mesh->mesh,
                                        mesh->morphedVB,
                                        mask,
                                        ray,
                                        ri,
                                        toGroup);
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self SkinnedMesh object
 * \param time current world time
 * \return minimum validity
 */
static M3Gint m3gSkinnedMeshApplyAnimation(Object *self, M3Gint time)
{
    SkinnedMesh *mesh = (SkinnedMesh *)self;
    
    M3Gint validity = m3gMeshApplyAnimation((Object*) &mesh->mesh, time);
    
    if (validity > 0) {
        M3Gint validity2 =
            M3G_VFUNC(Object, mesh->skeleton, applyAnimation)(
                (Object *)mesh->skeleton, time);
        return (validity < validity2 ? validity : validity2);
    }
    return 0;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self SkinnedMesh object
 * \param references array of reference objects
 * \return number of references
 */
static M3Gint m3gSkinnedMeshDoGetReferences(Object *self, Object **references)
{
    SkinnedMesh *smesh = (SkinnedMesh *)self;
    M3Gint num = m3gMeshDoGetReferences(self, references);
    if (smesh->skeleton != NULL)
    {
        if (references != NULL)
            references[num] = (Object *)smesh->skeleton;
        num++;
    }
    return num;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 */
static Object *m3gSkinnedMeshFindID(Object *self, M3Gint userID)
{
    SkinnedMesh *smesh = (SkinnedMesh *)self;
    Object *found = m3gMeshFindID(self, userID);
    
    if (!found && smesh->skeleton != NULL) {
        found = m3gFindID((Object*) smesh->skeleton, userID);
    }
    return found;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original SkinnedMesh object
 * \param cloneObj pointer to cloned SkinnedMesh object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gSkinnedMeshDuplicate(const Object *originalObj,
                                       Object **cloneObj,
                                       Object **pairs,
                                       M3Gint *numPairs)
{
    M3Gint i;
    SkinnedMesh *original = (SkinnedMesh *)originalObj;
    Group *skeleton = NULL;
    SkinnedMesh *clone;
    M3G_ASSERT(*cloneObj == NULL); /* no derived classes */
    
    /* Duplicate the skeleton group first, as this is a prerequisite
     * for creating the clone SkinnedMesh.  If this fails, we must
     * manually delete the skeleton, as no record of it will be stored
     * anywhere else; we also need to hold a reference until ownership
     * of the skeleton transfers to the clone SkinnedMesh. */
    
    if (!M3G_VFUNC(Object, original->skeleton, duplicate)(
            (Object*) original->skeleton,
            (Object**) &skeleton, pairs, numPairs)) {
        m3gDeleteObject((Object*) skeleton);
        return M3G_FALSE;
    }
    m3gAddRef((Object*) skeleton); /* don't leave this floating */

    /* Create the actual clone object */
    
    clone = (SkinnedMesh*)
        m3gCreateSkinnedMesh(originalObj->interface,
                             original->mesh.vertexBuffer,
                             original->mesh.indexBuffers,
                             original->mesh.appearances,
                             original->mesh.trianglePatchCount,
                             skeleton);    
    m3gDeleteRef((Object*) skeleton); /* ownership transferred to clone */
    if (!clone) {
        return M3G_FALSE;
    }
    *cloneObj = (Object *)clone;

    /* Duplicate base class data; we're OK for normal deletion at this
     * point, so can just leave it up to the caller on failure */
    
    if (!m3gMeshDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        return M3G_FALSE;
    }

    /* Duplicate the rest of our own data */

    if (!m3gEnsureVertexCount(clone, original->weightedVertexCount) ||
        !m3gEnsureBonesPerVertex(clone, original->bonesPerVertex)) {
        return M3G_FALSE; /* out of memory */
    }
    
    for (i = 0; i < clone->bonesPerVertex; i++) {
        m3gCopy(clone->boneIndices[i], original->boneIndices[i],
                clone->weightedVertexCount);
        m3gCopy(clone->boneWeights[i], original->boneWeights[i],
                clone->weightedVertexCount);
        m3gCopy(clone->normalizedWeights[i], original->normalizedWeights[i],
                clone->weightedVertexCount);
    }
    clone->weightsDirty = original->weightsDirty;
    m3gCopy(clone->weightShifts, original->weightShifts,
            clone->weightedVertexCount);

    for (i = 0; i < m3gArraySize(&original->bones); i++) {
        Bone *cloneBone = (Bone*) m3gAllocZ(originalObj->interface,
                                            sizeof(Bone));
        if (!cloneBone) {
            return M3G_FALSE; /* out of memory */
        }
        /* this line looks odd, but really just copies the *contents*
         * of the bone structure... */
        *cloneBone = *(Bone*)m3gGetArrayElement(&original->bones, i);

        if (m3gArrayAppend(&clone->bones, cloneBone, originalObj->interface) < 0) {
            m3gFree(originalObj->interface, cloneBone);
            return M3G_FALSE; /* out of memory */
        }
    }
    
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Overloaded Node method
 */
static M3Gint m3gSkinnedMeshGetBBox(Node *self, AABB *bbox)
{
    SkinnedMesh *mesh = (SkinnedMesh*) self;
    Node *skeleton = (Node*) mesh->skeleton;

    /* First update our local bounding box if necessary */
    
    if (self->dirtyBits & NODE_BBOX_BIT) {
        
        /* Compute an estimated bounding box from the morphed vertex
         * buffer scale and bias (from PreComputeTransformations).
         * The morphed vertex array is always scaled to utilize most
         * of the 16-bit short range, so we just use that as the
         * extents. */
        {
            const GLfloat scale = mesh->morphedVB->vertexScale;
            const GLfloat *bias = mesh->morphedVB->vertexBias;
            int i;
            
            for (i = 0; i < 3; ++i) {
                mesh->bbox.min[i] = m3gMadd(scale, -1 << 15, bias[i]);
                mesh->bbox.max[i] = m3gMadd(scale, (1 << 15) - 1, bias[i]);
            }
        }
    }
    *bbox = mesh->bbox;
    
    /* Mix in the skeleton bounding box if we need to -- but only into
     * the output bbox, as we're handling the local mesh bbox
     * specially in SetupRender! */
        
    if (skeleton->hasRenderables && skeleton->enableBits) {
        AABB skeletonBBox;
        if (m3gGetNodeBBox(skeleton, &skeletonBBox)) {
            Matrix t;
            m3gGetCompositeNodeTransform(self, &t);
            m3gTransformAABB(&skeletonBBox, &t);
            m3gFitAABB(bbox, &skeletonBBox, bbox);
        }
    }    
    return m3gArraySize(&mesh->bones) * VFC_NODE_OVERHEAD;
}

/*!
 * \internal
 * \brief Overloaded Node method
 */
static M3Gbool m3gSkinnedMeshValidate(Node *self, M3Gbitmask stateBits, M3Gint scope)
{
    SkinnedMesh *mesh = (SkinnedMesh*) self;
    Interface *m3g = M3G_INTERFACE(mesh);
    Node *skeleton = (Node*) mesh->skeleton;
    const VertexBuffer *srcVB = mesh->mesh.vertexBuffer;
    M3Gint vertexCount = mesh->weightedVertexCount;

    if ((scope & self->scope) != 0) {
        if (stateBits & self->enableBits) {
            
            /* Check for invalid SkinnedMesh state */
        
            if (srcVB->vertices == NULL || vertexCount > srcVB->vertexCount) {
                m3gRaiseError(m3g, M3G_INVALID_OPERATION);
                return M3G_FALSE;
            }
            if (!m3gSkinnedMeshUpdateVB(mesh)) { /* Memory allocation failed */
                return M3G_FALSE;
            }
        
            /* Validate the skeleton */
        
            if (!m3gValidateNode(skeleton, stateBits, scope)) {
                return M3G_FALSE;
            }
    
            /* Validate our local state */
    
            if ((self->dirtyBits & NODE_TRANSFORMS_BIT) != 0 || 
                m3gGetTimestamp(srcVB) != mesh->mesh.vbTimestamp) {
                if (!m3gSkinnedMeshPreMorph((SkinnedMesh*) self)) {
                    return M3G_FALSE;
                }
            }
            if (self->dirtyBits & NODE_BBOX_BIT) {
                M3G_BEGIN_PROFILE(M3G_INTERFACE(self), M3G_PROFILE_VFC_UPDATE);
                m3gGetNodeBBox(self, &mesh->bbox);
                M3G_END_PROFILE(M3G_INTERFACE(self), M3G_PROFILE_VFC_UPDATE);
            }
    
            return m3gMeshValidate(self, stateBits, scope);
        }
    }
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self SkinnedMesh object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static void m3gSkinnedMeshUpdateDuplicateReferences(Node *self, Object **pairs, M3Gint numPairs)
{
    SkinnedMesh *skinned = (SkinnedMesh *)self;
    SkinnedMesh *duplicate = (SkinnedMesh *)m3gGetDuplicatedInstance(self, pairs, numPairs);
    M3Gint i, n;
    
    m3gNodeUpdateDuplicateReferences(self, pairs, numPairs);
    
    n = m3gArraySize(&duplicate->bones);
    for (i = 0; i < n; i++) {
        Bone *bone = (Bone*) m3gGetArrayElement(&duplicate->bones, i);
        Node *boneDuplicate = m3gGetDuplicatedInstance(bone->node, pairs, numPairs);
        if (boneDuplicate != NULL) {
            bone->node = boneDuplicate;
        }
    }
    
    M3G_VFUNC(Node, skinned->skeleton, updateDuplicateReferences)(
        (Node *)skinned->skeleton, pairs, numPairs);
}

/*!
 * \internal
 * \brief Initializes a SkinnedMesh object. See specification
 * for default values.
 *
 * \param m3g                   M3G interface
 * \param mesh           SkinnedMesh object
 * \param hVertices             VertexBuffer object
 * \param hTriangles            array of IndexBuffer objects
 * \param hAppearances          array of Appearance objects
 * \param trianglePatchCount    number of submeshes
 * \param hSkeleton             Group containing the skeleton
 * \retval                      M3G_TRUE success
 * \retval                      M3G_FALSE failure
 */
static M3Gbool m3gInitSkinnedMesh(Interface *m3g,
                                  SkinnedMesh *mesh,
                                  M3GVertexBuffer hVertices,
                                  M3GIndexBuffer *hTriangles,
                                  M3GAppearance *hAppearances,
                                  M3Gint trianglePatchCount,
                                  M3GGroup hSkeleton)
{
    /* SkinnedMesh is derived from Mesh */
    if (!m3gInitMesh(m3g, &mesh->mesh,
                     hVertices, hTriangles, hAppearances,
                     trianglePatchCount,
                     M3G_CLASS_SKINNED_MESH))
    {
        return M3G_FALSE;
    }

    /* Make sure our mesh gets blended even if no bones are added */    
    ((Node*)mesh)->dirtyBits |= NODE_TRANSFORMS_BIT;
        
    /* Set default values, see RI SkinnedMesh.java for reference */
    m3gSetParent(&((Group *)hSkeleton)->node, &mesh->mesh.node);
    M3G_ASSIGN_REF(mesh->skeleton, (Group *)hSkeleton);

    m3gInitArray(&mesh->bones);
    
    mesh->morphedVB = (VertexBuffer *)m3gCreateVertexBuffer(m3g);
    if (mesh->morphedVB == NULL
        || m3gSkinnedMeshUpdateVB(mesh) == M3G_FALSE) {
        
        /* We're sufficiently initialized at this point that the
         * destructor can be called for cleaning up */
        
        m3gDestroySkinnedMesh((Object *)mesh);
        return M3G_FALSE;
    }
    return M3G_TRUE;
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const NodeVFTable m3gvf_SkinnedMesh = {
    {
        {
            m3gSkinnedMeshApplyAnimation,
            m3gNodeIsCompatible,
            m3gNodeUpdateProperty,
            m3gSkinnedMeshDoGetReferences,
            m3gSkinnedMeshFindID,
            m3gSkinnedMeshDuplicate,
            m3gDestroySkinnedMesh
        }
    },
    m3gNodeAlign,
    m3gSkinnedMeshDoRender,
    m3gSkinnedMeshGetBBox,
    m3gSkinnedMeshRayIntersect,
    m3gSkinnedMeshSetupRender,
    m3gSkinnedMeshUpdateDuplicateReferences,
    m3gSkinnedMeshValidate
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a SkinnedMesh object.
 *
 * \param interface             M3G interface
 * \param hVertices             VertexBuffer object
 * \param hTriangles            array of IndexBuffer objects
 * \param hAppearances          array of Appearance objects
 * \param trianglePatchCount    number of submeshes
 * \param hSkeleton             Group containing the skeleton
 * \retval                      SkinnedMesh new SkinnedMesh object
 * \retval                      NULL SkinnedMesh creating failed
 */
M3G_API M3GSkinnedMesh m3gCreateSkinnedMesh(M3GInterface interface,
                                            M3GVertexBuffer hVertices,
                                            M3GIndexBuffer *hTriangles,
                                            M3GAppearance *hAppearances,
                                            M3Gint trianglePatchCount,
                                            M3GGroup hSkeleton)
{
    Interface *m3g = (Interface *) interface;
    M3G_VALIDATE_INTERFACE(m3g);
    {
        SkinnedMesh *mesh = NULL;
        Group *skeleton = (Group *) hSkeleton;
        if (skeleton == NULL) {
            m3gRaiseError(m3g, M3G_NULL_POINTER);
            return NULL;
        }
        if (skeleton->node.parent != NULL ||
            M3G_CLASS(skeleton) == M3G_CLASS_WORLD) {
            m3gRaiseError(m3g, M3G_INVALID_VALUE);
            return NULL;
        }
        
        mesh = m3gAllocZ(m3g, sizeof(SkinnedMesh));
        if (mesh) {
            if (!m3gInitSkinnedMesh(m3g, mesh,
                                    hVertices, hTriangles, hAppearances,
                                    trianglePatchCount,
                                    hSkeleton)) {
                m3gFree(m3g, mesh);
                return NULL;
            }
        }
        return (M3GSkinnedMesh)mesh;
    }
}

/*!
 * \brief Add new weighted transformation (bone) to range of vertices
 *
 * 
 * \param handle        SkinnedMesh object
 * \param hNode         bone to transform the vertices with
 * \param weight        weight of the bone
 * \param firstVertex   index to the first affected vertex
 * \param numVertices   number of affected vertices
 */
M3G_API void m3gAddTransform(M3GSkinnedMesh handle,
                             M3GNode hNode,
                             M3Gint weight,
                             M3Gint firstVertex, M3Gint numVertices)
{
    SkinnedMesh *mesh = (SkinnedMesh *)handle;
    Node *boneNode = (Node *)hNode;
    Interface *m3g = M3G_INTERFACE(mesh);
    
    M3Gint lastVertex = firstVertex + numVertices;
    M3G_VALIDATE_OBJECT(mesh);

    /* Check for errors */
    
    if (!boneNode) {
        m3gRaiseError(m3g, M3G_NULL_POINTER);
        return;
    }
    M3G_VALIDATE_OBJECT(boneNode);
    if (!m3gIsChildOf((const Node*) mesh, boneNode)
        || numVertices <= 0 || weight <= 0) {
        m3gRaiseError(m3g, M3G_INVALID_VALUE);
        return;
    }
    if (firstVertex < 0 || lastVertex > 65535) {
        m3gRaiseError(m3g, M3G_INVALID_INDEX);
        return;
    }

    /* Make sure we have enough per-vertex data */
    
    if (!m3gEnsureVertexCount(mesh, lastVertex)) {
        return; /* out of memory */
    }
    
    /* Check whether we may need to increase the number of bone
     * entries per vertex, or whether we're already maxed out */
    
    if (mesh->bonesPerVertex < M3G_MAX_VERTEX_TRANSFORMS) {

        /* Scan the input vertex range to find the maximum number of
         * transforms per vertex (with non-zero weights) already in
         * use, then make sure we can fit one more */
        
        int numBones = mesh->bonesPerVertex;
        int maxBones = 0;
        
        int vertex;
        for (vertex = firstVertex; vertex < lastVertex; ++vertex) {
            int k;
            for (k = numBones; k > 0; --k) {
                if (mesh->boneWeights[k-1][vertex] > 0) {
                    maxBones = M3G_MAX(maxBones,  k);
                    break;
                }
            }
        }
        if (!m3gEnsureBonesPerVertex(mesh, maxBones + 1)) {
            return; /* out of memory */
        }
    }
    
    /* Get a bone record for the bone node, and add the bone influence
     * to all affected vertices */
    {
        int i;
        
        M3Gint boneIndex = m3gBoneIndex(mesh, boneNode);
        if (boneIndex < 0) {
            return; /* out of memory */
        }
        
        for (i = firstVertex; i < lastVertex; i++) {
            m3gAddInfluence(mesh, i, boneIndex, weight);
        }
    }
    
    /* Update the bone flag for the bone node and its parents up to
     * the SkinnedMesh node */
    
    while (boneNode != (Node*) mesh) { /* boneNode must be a child of ours */
        M3G_ASSERT(boneNode);
        boneNode->hasBones = M3G_TRUE;
        boneNode = boneNode->parent;
    }
}

/*!
 * \brief Getter for skeleton.
 *
 * \param handle                SkinnedMesh object
 * \return                      Group object
 */
M3G_API M3GGroup m3gGetSkeleton(M3GSkinnedMesh handle)
{
    SkinnedMesh *mesh = (SkinnedMesh *)handle;
    M3G_VALIDATE_OBJECT(mesh);

    return mesh->skeleton;
}

/*!
 * \brief Getter for bone transform.
 *
 * \param handle                SkinnedMesh object
 * \param hBone                 Bone
 * \param transform             Transform
 */
M3G_API void m3gGetBoneTransform(M3GSkinnedMesh handle,
                                 M3GNode hBone,
                                 M3GMatrix *transform)
{
    SkinnedMesh *mesh = (SkinnedMesh *)handle;
    Node *node = (Node *)hBone;
    M3Gint i;
    M3Gint boneCount;

    M3G_VALIDATE_OBJECT(mesh);
    M3G_VALIDATE_OBJECT(node);

    if (!m3gIsChildOf((Node*) mesh->skeleton, node)) {
        m3gRaiseError(M3G_INTERFACE(mesh), M3G_INVALID_VALUE);
        return;
    }   
    
    boneCount = m3gArraySize(&mesh->bones);

    for (i = 0; i < boneCount; ++i) {
        Bone *bone = m3gGetArrayElement(&mesh->bones, i);

        if (bone->node == node) {
            m3gCopyMatrix(transform, &bone->toBone);
            break;
        }
    }
}

/*!
 * \brief Getter for bone vertices.
 *
 * \param handle                SkinnedMesh object
 * \param hBone                 Bone
 * \param indices               Influenced indices
 * \param weights               Weights
 * \return                      Number of influenced vertices
 */
M3G_API M3Gint m3gGetBoneVertices(M3GSkinnedMesh handle,
                                  M3GNode hBone,
                                  M3Gint *indices, M3Gfloat *weights)
{
    SkinnedMesh *mesh = (SkinnedMesh *)handle;
    Node *node = (Node *)hBone;
    M3Gint boneIndex, boneCount, count = 0;

    M3G_VALIDATE_OBJECT(mesh);
    M3G_VALIDATE_OBJECT(node);

    /* Check for errors */

    if (!m3gIsChildOf((Node*) mesh->skeleton, node)) {
        m3gRaiseError(M3G_INTERFACE(mesh), M3G_INVALID_VALUE);
        return 0;
    }   
        
    /* Find the bone index corresponding to our bone node */
    
    boneCount = m3gArraySize(&mesh->bones);

    for (boneIndex = 0; boneIndex < boneCount; ++boneIndex) {
        Bone *bone = m3gGetArrayElement(&mesh->bones, boneIndex);
        if (bone->node == node) {
            break;
        }
    }

    /* Loop over the vertices, outputting index-weight pairs for each
     * vertex influenced by the bone */

    if (boneIndex < boneCount) {
        M3Gint i, j;

        for (i = 0; i < mesh->weightedVertexCount; ++i) {
            for (j = 0; j < mesh->bonesPerVertex; ++j) {
                if (mesh->boneIndices[j][i] == boneIndex && mesh->boneWeights[j][i] > 0) {
                    if (indices != NULL && weights != NULL) {
                        M3Gint k, sum = 0;
                        for (k = 0; k < mesh->bonesPerVertex; ++k) {
                            sum += mesh->boneWeights[k][i];
                        }
                        indices[count] = i;
                        if (sum != 0) {
                            weights[count] = ((M3Gfloat) mesh->boneWeights[j][i]) / sum;
                        }
                        else {
                            weights[count] = 0;
                        }
                    }
                    ++count;
                }
            }                    
        }
    }
    return count;
}

