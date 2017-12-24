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
* Description: VertexArray implementation
*
*/


/*!
 * \internal
 * \file
 * \brief VertexArray implementation
 *
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_vertexarray.h"

#define DIRTY_ALPHA_FACTOR (-1)

/*----------------------------------------------------------------------
 * Private functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Signals that the contents of an array have changed
 */
static M3G_INLINE void m3gInvalidateArray(VertexArray *array)
{
    array->cachedAlphaFactor = DIRTY_ALPHA_FACTOR;
    array->rangeMin = 1;
    array->rangeMax = 0;
    
    ++array->timestamp;
}
    
/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Destroys this VertexArray object.
 *
 * \param obj VertexArray object
 */
static void m3gDestroyVertexArray(Object *obj)
{
    VertexArray *array = (VertexArray *) obj;
    M3G_VALIDATE_OBJECT(array);
    M3G_ASSERT(array->numLocks == 0);
    {
        Interface *m3g = M3G_INTERFACE(array);
        m3gFreeObject(m3g, array->data);
        m3gFreeObject(m3g, array->cachedColors);
    }
    m3gDestroyObject(&array->object);
}

/*!
 * \internal
 * \brief Sends color array to OpenGL.
 *
 * \note Alpha scaling currently prevents an array from being used for
 * anything else while it is being bound as a color array.
 *
 * \param array         VertexArray object
 * \param alphaFactor   1.16 alpha factor in [0, 0x10000]
 */
static void m3gLockColorArray(const VertexArray *array, M3Gint alphaFactor)
{
    Interface *m3g = M3G_INTERFACE(array);
    M3G_VALIDATE_OBJECT(array);
    M3G_ASSERT(!array->mapCount);
    M3G_ASSERT(array->numLocks == 0);
    M3G_ASSERT(m3gInRange(alphaFactor, 0, 0x10000));

    /* With an alpha factor of 1.0, we can just load up the original data */
    
    if (alphaFactor >= 0x10000) {
        GLenum type = array->elementType;
        if (type >= GL_BYTE && type <= GL_UNSIGNED_SHORT) {
            type |= 0x01; /* force type to unsigned for GL */
        }
        glColorPointer(type == GL_UNSIGNED_BYTE ? 4 : array->elementSize,
                       type,
                       array->stride,
                       m3gMapObject(m3g, array->data));
    }
    else {

        /* With a non-unit alpha factor, we may need to update the
         * cached pre-scaled colors. */

        M3Gubyte* const cache = (M3Gubyte *)
            m3gMapObject(m3g, array->cachedColors);
            
        if (array->cachedAlphaFactor != alphaFactor) {
            M3Gubyte *dst = cache;
            int i, n;

            M3G_VALIDATE_MEMBLOCK(cache);
            
            /* Scale the colors, converting from the source format */

            n = array->vertexCount;
            
            /* Byte colors are always padded to 4 bytes per entry,
             * with the implicit alpha set to 0xFF for RGB colors, so
             * we can do a near-straight copy. */
            
            switch (array->elementType) {
            case GL_BYTE:
            case GL_UNSIGNED_BYTE:
            {   
                const M3Gubyte *src = (M3Gubyte *)m3gMapObject(m3g,
                                                               array->data);
                for (i = 0; i < n; ++i) {
                    *dst++ = *src++;
                    *dst++ = *src++;
                    *dst++ = *src++;
                    {
                        M3Guint tmp = *src++ * (M3Guint) alphaFactor;
                        *dst++ = (M3Gubyte)(tmp >> 16);
                    }
                }
                m3gUnmapObject(m3g, array->data);
                break;
            }
            default:
                M3G_ASSERT(M3G_FALSE);
            }
            
            ((VertexArray*)array)->cachedAlphaFactor = alphaFactor;
        }
        
        /* We now have the scaled colors in the cache, so just set the
         * pointer there */
        
        glColorPointer(4, GL_UNSIGNED_BYTE, 0, cache);
    }
    M3G_ASSERT_GL;
    
    ++((VertexArray*)array)->numLocks;
}

/*!
 * \internal
 * \brief Creates the color cache required for alpha factors
 */
static M3Gbool m3gCreateAlphaColorCache(VertexArray *array)
{
    M3G_VALIDATE_OBJECT(array);
    M3G_ASSERT(array->cachedColors == 0);
    
    /* There are always four bytes per color entry */
    
    array->cachedColors = m3gAllocObject(M3G_INTERFACE(array),
                                         4 * array->vertexCount);
    
    return (array->cachedColors != 0);
}

/*!
 * \internal
 * \brief Sends normal array to OpenGL.
 *
 * \param array VertexArray object
 */
static void m3gLockNormalArray(const VertexArray *array)
{
    M3G_VALIDATE_OBJECT(array);
    M3G_ASSERT(!array->mapCount);

    glNormalPointer(array->elementType, array->stride,
                    m3gMapObject(M3G_INTERFACE(array), array->data));
    M3G_ASSERT_GL;
    
    ++((VertexArray*)array)->numLocks;
}

/*!
 * \internal
 * \brief Sends texture coordinate array to OpenGL.
 *
 * \param array VertexArray object
 */
static void m3gLockTexCoordArray(const VertexArray *array)
{
    M3G_VALIDATE_OBJECT(array);
    M3G_ASSERT(!array->mapCount);

    glTexCoordPointer(array->elementSize,
                      array->elementType,
                      array->stride,
                      m3gMapObject(M3G_INTERFACE(array), array->data));
    M3G_ASSERT_GL;
    
    ++((VertexArray*)array)->numLocks;
}

/*!
 * \internal
 * \brief Sends vertex array to OpenGL.
 *
 * \param array VertexArray object
 */
static void m3gLockVertexArray(const VertexArray *array)
{
    M3G_VALIDATE_OBJECT(array);
    M3G_ASSERT(!array->mapCount);

    glVertexPointer(array->elementSize,
                    array->elementType,
                    array->stride,
                    m3gMapObject(M3G_INTERFACE(array), array->data));
    M3G_ASSERT_GL;
    
    ++((VertexArray*)array)->numLocks;
}

/*!
 * \internal
 * \brief Decreases array lock count.
 *
 * \param array VertexArray object
 */
static void m3gUnlockArray(const VertexArray *array)
{
    M3G_VALIDATE_OBJECT(array);
    M3G_ASSERT(array->numLocks > 0);
    
    m3gUnmapObject(M3G_INTERFACE(array), array->data);
    
    --((VertexArray*)array)->numLocks;
}

/*!
 * \internal
 * \brief Clones a VertexArray.
 *
 * Used by MorphingMesh.
 *
 * \param array VertexArray object
 * \return cloned VertexArray object
 *
 */
static VertexArray *m3gCloneVertexArray(const VertexArray *array)
{
	VertexArray *clone;
	Interface *m3g = M3G_INTERFACE(array);
	
    M3G_VALIDATE_OBJECT(array);
    M3G_ASSERT(!array->mapCount);

	clone = (VertexArray *) m3gAlloc(m3g, sizeof(VertexArray));
	if (clone == NULL) {
        return NULL;
    }

	m3gCopy(clone, array, sizeof(VertexArray));
    m3gInitObject((Object*) clone, m3g, M3G_CLASS_VERTEX_ARRAY);

	clone->data = m3gAllocObject(m3g, array->vertexCount * array->stride);

	if (!clone->data) {
        m3gDestroyObject((Object*) clone);
		m3gFree(m3g, clone);
		return NULL;
	}

	m3gCopy(m3gMapObject(m3g, clone->data),
            m3gMapObject(m3g, array->data),
            array->vertexCount * array->stride);
    m3gUnmapObject(m3g, clone->data);
    m3gUnmapObject(m3g, array->data);

	return clone;
}

/*!
 * \internal
 * \brief Gets array vertex count.
 *
 * \param array VertexArray object
 * \return number of vertices
 */
static M3Gint m3gGetArrayVertexCount(const VertexArray *array)
{
    return array->vertexCount;
}

/*!
 * \internal
 * \brief Returns the minimum and maximum value stored in the array
 */
static void m3gGetArrayValueRange(const VertexArray *array,
                                  M3Gint *minValue, M3Gint *maxValue)
{
    Interface *m3g = M3G_INTERFACE(array);
    
    if (array->rangeMin > array->rangeMax) {
        M3Gint count = array->elementSize * array->vertexCount;
        M3Gint minVal = 0, maxVal = 0;
        
        if (count > 0) {
            switch (array->elementType) {
            case GL_BYTE:
            {
                const GLbyte *src = (const GLbyte*) m3gMapObject(m3g,
                                                                 array->data);
                const M3Gint c = array->elementSize;
                const M3Gint skip = array->stride - c;
                minVal = maxVal = (M3Gint) *src++;
                while (count) {
                    M3Gint i;
                    for (i = 0; i < c; ++i) {
                        M3Gint v = (M3Gint) *src++;
                        minVal = M3G_MIN(minVal, v);
                        maxVal = M3G_MAX(maxVal, v);
                    }
                    count -= c;
                    src += skip;
                }
                break;
            }
            case GL_UNSIGNED_BYTE:
            {
                const GLubyte *src = (const GLubyte*) m3gMapObject(m3g,
                                                                   array->data);
                const M3Gint c = array->elementSize;
                const M3Gint skip = array->stride - c;
                minVal = maxVal = (M3Gint) *src++;
                while (count) {
                    M3Gint i;
                    for (i = 0; i < c; ++i) {
                        M3Gint v = (M3Gint) *src++;
                        minVal = M3G_MIN(minVal, v);
                        maxVal = M3G_MAX(maxVal, v);
                    }
                    count -= c;
                    src += skip;
                }
                break;
            }
            case GL_SHORT:
            {
                const GLshort *src = (const GLshort*)
                    m3gMapObject(m3g, array->data);
                minVal = maxVal = (M3Gint) *src++;
                while (--count) {
                    M3Gint v = (M3Gint) *src++;
                    minVal = M3G_MIN(minVal, v);
                    maxVal = M3G_MAX(maxVal, v);
                }
                break;
            }
            case GL_UNSIGNED_SHORT:
            {
                const GLushort *src = (const GLushort*)
                    m3gMapObject(m3g, array->data);
                minVal = maxVal = (M3Gint) *src++;
                while (--count) {
                    M3Gint v = (M3Gint) *src++;
                    minVal = M3G_MIN(minVal, v);
                    maxVal = M3G_MAX(maxVal, v);
                }
                break;
            }
            default:
                M3G_ASSERT(M3G_FALSE);
            }
        }
        m3gUnmapObject(m3g, array->data);

        M3G_ASSERT(m3gInRange(minVal, -32768, 32767));
        M3G_ASSERT(m3gInRange(maxVal, -32768, 32767));
        
        ((VertexArray*)array)->rangeMin = (M3Gshort) minVal;
        ((VertexArray*)array)->rangeMax = (M3Gshort) maxVal;
    }
    
    *minValue = array->rangeMin;
    *maxValue = array->rangeMax;
}

/*!
 * \internal
 * \brief Compares attributes of two vertex arrays.
 *
 * \param array         VertexArray object
 * \param other         VertexArray object
 * \retval M3G_TRUE     arrays are compatible
 * \retval M3G_FALSE    arrays are not compatible
 */
static M3Gbool m3gIsCompatible(const VertexArray *array, const VertexArray *other)
{
    return( other != NULL &&
            other->elementType == array->elementType &&
            other->elementSize == array->elementSize &&
            other->vertexCount == array->vertexCount);
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original VertexArray object
 * \param cloneObj pointer to cloned VertexArray object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gVertexArrayDuplicate(const Object *originalObj,
                                       Object **cloneObj,
                                       Object **pairs,
                                       M3Gint *numPairs)
{
    VertexArray *clone = m3gCloneVertexArray((VertexArray *)originalObj);
    if (!clone) {
        return M3G_FALSE;
    }
    *cloneObj = (Object*) clone;
    return m3gObjectDuplicate(originalObj, cloneObj, pairs, numPairs);
}

/*!
 * \internal
 * \brief Gets array timestamp.
 *
 * \param array VertexArray object
 * \return timestamp
 */
static M3Gint m3gGetArrayTimestamp(const VertexArray *array)
{
    return array->timestamp;
}

/*!
 * \internal
 * \brief Gets array bounding box as shorts.
 *
 * \param array VertexArray object
 * \param boundingBox   array to fill in
 *                      \arg [0] = minX
 *                      \arg [1] = minY
 *                      \arg [2] = minZ
 *                      \arg [3] = maxX
 *                      \arg [4] = maxY
 *                      \arg [5] = maxZ
 */
static void m3gGetArrayBoundingBox(const VertexArray *array, M3Gshort *boundingBox)
{
    Interface *m3g = M3G_INTERFACE(array);
    M3Gint i;
    M3Gshort minX, minY, minZ;
    M3Gshort maxX, maxY, maxZ;
    M3Gbyte *bptr;
    M3Gshort *sptr;
    
    /* Only support 3 component arrays */
    if (array->elementSize != 3 || array->vertexCount == 0) {
        return;
    }

    switch(array->elementType) {
    case M3G_GLTYPE(M3G_BYTE):
    case M3G_GLTYPE(M3G_UBYTE):
        bptr = (M3Gbyte *) m3gMapObject(m3g, array->data);

        minX = maxX = bptr[0];
        minY = maxY = bptr[1];
        minZ = maxZ = bptr[2];
        bptr += 4;

        for (i = 0; i < array->vertexCount - 1; i++) {
            if (bptr[0] < minX) minX = bptr[0];
            if (bptr[0] > maxX) maxX = bptr[0];
            if (bptr[1] < minY) minY = bptr[1];
            if (bptr[1] > maxY) maxY = bptr[1];
            if (bptr[2] < minZ) minZ = bptr[2];
            if (bptr[2] > maxZ) maxZ = bptr[2];
            bptr += 4;
        }
        break;

    case M3G_GLTYPE(M3G_SHORT):
    case M3G_GLTYPE(M3G_USHORT):
        sptr = (M3Gshort *) m3gMapObject(m3g, array->data);

        minX = maxX = sptr[0];
        minY = maxY = sptr[1];
        minZ = maxZ = sptr[2];
        sptr += 3;

        for (i = 0; i < array->vertexCount - 1; i++) {
            if (sptr[0] < minX) minX = sptr[0];
            if (sptr[0] > maxX) maxX = sptr[0];
            if (sptr[1] < minY) minY = sptr[1];
            if (sptr[1] > maxY) maxY = sptr[1];
            if (sptr[2] < minZ) minZ = sptr[2];
            if (sptr[2] > maxZ) maxZ = sptr[2];
            sptr += 3;
        }
        break;

    default: /* Error */
        M3G_ASSERT(0);
        return;
    }

    m3gUnmapObject(m3g, array->data);

    boundingBox[0] = minX;
    boundingBox[1] = minY;
    boundingBox[2] = minZ;
    boundingBox[3] = maxX;
    boundingBox[4] = maxY;
    boundingBox[5] = maxZ;
}

/*!
 * \internal
 * \brief Gets a coordinate from vertex array.
 *
 * \param va            VertexArray object
 * \param elementCount  elemens in coordinate
 * \param idx           index of coordinate
 * \param v             vector to fill in
 * \retval              M3G_TRUE get ok
 * \retval              M3G_FALSE no such vertex
 */
static M3Gbool m3gGetCoordinates(VertexArray *va,
                                 M3Gint elementCount,
                                 M3Gint idx,
                                 M3Gfloat *v)
{
    Interface *m3g;
    M3Gbyte *bptr;
    M3Gshort *sptr;
    int i;
    
    if (!va) {
        return M3G_FALSE;
    }

    m3g = M3G_INTERFACE(va);

    switch (va->elementType) {
    case M3G_GLTYPE(M3G_BYTE):
    case M3G_GLTYPE(M3G_UBYTE):
        idx *= 4;
        bptr = (M3Gbyte *) m3gMapObject(m3g, va->data);
        bptr += idx;
        for (i = 0; i < elementCount; ++i) {
            *v++ = *bptr++;
        }
        break;

    case M3G_GLTYPE(M3G_SHORT):
    case M3G_GLTYPE(M3G_USHORT):
        idx *= elementCount;
        sptr = (M3Gshort *) m3gMapObject(m3g, va->data);
        sptr += idx;
        for (i = 0; i < elementCount; ++i) {
            *v++ = *sptr++;
        }
        break;
    }

    m3gUnmapObject(m3g, va->data);
    return M3G_TRUE;
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const ObjectVFTable m3gvf_VertexArray = {
    m3gObjectApplyAnimation,
    m3gObjectIsCompatible,
    m3gObjectUpdateProperty,
    m3gObjectDoGetReferences,
    m3gObjectFindID,
    m3gVertexArrayDuplicate,
    m3gDestroyVertexArray
};
        

/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a VertexArray object.
 *
 * \param interface     M3G interface
 * \param count         Count of vertices
 * \param size          Size of each element [2, 4]
 * \param type          Type of elements
 * \retval VertexArray new VertexArray object
 * \retval NULL VertexArray creating failed
 */

/*@access M3Ginterface@*/
/*@access M3GVertexArray@*/
M3G_API M3GVertexArray m3gCreateVertexArray(M3GInterface interface,
                                            M3Gsizei count,
                                            M3Gint size,
                                            M3Gdatatype type)
{
    Interface *m3g = (Interface *) interface;
    M3G_VALIDATE_INTERFACE(m3g);
    
    /* Check errors */
    if (count < 1 || count > 65535 ||
        size < 2 || size > 4 ||
        (type != M3G_BYTE && type != M3G_SHORT)) {
        m3gRaiseError(m3g, M3G_INVALID_VALUE);
        return NULL;
    }

    {
        /* Allocate the array object and its data buffer */
        
        VertexArray *array = m3gAllocZ(m3g, (M3Gsizei) sizeof(VertexArray));
        if (!array) {
            return NULL;
        }

        switch (type) {
        case M3G_BYTE:
            /* always padded to 4 bytes */
            array->stride = 4;
            break;
        case M3G_SHORT:
            array->stride = size * sizeof(M3Gshort);
            break;
        }

        /* Alloc and initialize all values to zero */
        array->data = m3gAllocObject(m3g, count * array->stride);
        if (!array->data) {
            m3gFree(m3g, array);
            return NULL;
        }
        else {
            void *ptr = m3gMapObject(m3g, array->data);
            m3gZero(ptr, count * array->stride);
            m3gUnmapObject(m3g, array->data);
        }

        m3gInitObject(&array->object, m3g, M3G_CLASS_VERTEX_ARRAY);

        array->elementType = M3G_GLTYPE(type);
        array->elementSize = size;
        array->vertexCount = count;
        m3gInvalidateArray(array);
        
        return (M3GVertexArray) array;
    }
}

/*!
 * \brief Returns the data layout parameters for a vertex array
 *
 * This gives the format of the data mapped to user memory with \c
 * m3gMapVertexArray.
 *
 * \param handle  array handle
 * \param count   pointer for number of vertices (output)
 * \param size    pointer for components per vertex (output)
 * \param type    pointer to data element type (output)
 * \param stride  pointer to stride, i.e. number of bytes from
 *                the beginning of one vertex to the next (output)
 */
M3G_API void m3gGetVertexArrayParams(M3GVertexArray handle,
                                     M3Gsizei *count,
                                     M3Gint *size,
                                     M3Gdatatype *type,
                                     M3Gsizei *stride)
{
    VertexArray *array = (VertexArray *) handle;
    M3G_VALIDATE_OBJECT(array);

    if (count) {
        *count = array->vertexCount;
    }
    if (size) {
        *size = array->elementSize;
    }
    if (type) {
        *type = (M3Gdatatype) M3G_M3GTYPE(array->elementType);
    }
    if (stride) {
        *stride = array->stride;
    }
}

/*!
 * \brief Maps the data of a vertex array to application memory
 *
 * The contents of the array will remain mapped to application memory
 * until a matching \c m3gUnMapVertexArray call. While mapped to user
 * memory, the array can not be used for rendering.
 *
 * Deleting a mapped array will also implicitly unmap it.
 *
 * \param handle handle of the array to map
 * \return pointer to the array data
 */
M3G_API void *m3gMapVertexArray(M3GVertexArray handle)
{
    void *ptr = (void*) m3gMapVertexArrayReadOnly(handle);
    if (ptr) {
        m3gInvalidateArray((VertexArray*) handle);
    }
    return ptr;
}

/*!
 * \brief Maps a vertex array for reading only
 *
 * This is the same as m3gMapVertexArray, but maps the array for
 * reading only, allowing internal optimizations.
 *
 */
M3G_API const void *m3gMapVertexArrayReadOnly(M3GVertexArray handle)
{
    VertexArray *array = (VertexArray *) handle;
    M3G_VALIDATE_OBJECT(array);
    
    if (array->numLocks > 0) {
        m3gRaiseError(M3G_INTERFACE(array), M3G_INVALID_OPERATION);
        return NULL;
    }
    
    ++array->mapCount;
    return m3gMapObject(M3G_INTERFACE(array), array->data);
}

/*!
 * \brief Releases an array mapped to user memory
 *
 * The pointer obtained with a preceding \c m3gMapVertexArray call
 * will not be valid after unmapping the array.
 *
 * \param handle handle of the array to release
 */
M3G_API void m3gUnmapVertexArray(M3GVertexArray handle)
{
    VertexArray *array = (VertexArray *) handle;
    M3G_VALIDATE_OBJECT(array);
    M3G_ASSERT(array->mapCount);

    m3gUnmapObject(M3G_INTERFACE(array), array->data);
    --array->mapCount;
}

/*!
 * \brief Set a range of vertex array elements
 *
 * \param handle array handle
 * \param first  index of first vertex to set
 * \param count  number of total vertices to set
 * \param srcLength length of source data
 * \param type  data type of source data
 * \param src   source data
 */
M3G_API void m3gSetVertexArrayElements(M3GVertexArray handle,
                                       M3Gint first, M3Gsizei count,
                                       M3Gsizei srcLength,
                                       M3Gdatatype type,
                                       const void *src)
{
    VertexArray *array = (VertexArray *) handle;
    M3G_VALIDATE_OBJECT(array);

    M3G_ASSERT(array->numLocks == 0);

    /* Check errors */
    if (array->mapCount) {
        m3gRaiseError(M3G_INTERFACE(array), M3G_INVALID_OPERATION);
        return;
    }
    if (src == NULL) {
        m3gRaiseError(M3G_INTERFACE(array), M3G_NULL_POINTER);
        return;
    }
    if (first < 0 || first + count > array->vertexCount) {
        m3gRaiseError(M3G_INTERFACE(array), M3G_INVALID_INDEX);
        return;
    }
    if (count < 0 || srcLength < count * array->elementSize) {
        m3gRaiseError(M3G_INTERFACE(array), M3G_INVALID_VALUE);
        return;
    }

    /* Copy source data according to destination array type */
    {
        int values = count * array->elementSize;
        
        switch (array->elementType) {
        case GL_BYTE:
        case GL_UNSIGNED_BYTE:
            if (type != M3G_BYTE) {
                m3gRaiseError(M3G_INTERFACE(array), M3G_INVALID_OPERATION);
                return;
            }
            else {
                GLubyte *dst =
                    ((GLubyte *)m3gMapObject(M3G_INTERFACE(array),
                                             array->data))
                    + first * array->stride;
                GLubyte *srcByte = (GLubyte *) src;

                M3G_ASSERT(array->elementSize >= 2 && array->elementSize <= 4);
                M3G_ASSERT(array->stride == 4);
                
                while (values > 0) {
                    *dst++ = *srcByte++;
                    *dst++ = *srcByte++;
                    *dst++ = (M3Gubyte)((array->elementSize >= 3) ? *srcByte++ : 0x00);
                    *dst++ = (M3Gubyte)((array->elementSize == 4) ? *srcByte++ : 0xFF);
                    values -= array->elementSize;
                }
            }
            break;
        
        case GL_SHORT:
        case GL_UNSIGNED_SHORT:
            if (type != M3G_SHORT) {
                m3gRaiseError(M3G_INTERFACE(array), M3G_INVALID_OPERATION);
                return;
            }
            else {
                GLushort *dst =
                    ((GLushort *)m3gMapObject(M3G_INTERFACE(array),
                                              array->data))
                    + first * array->stride / 2;
                GLushort *srcShort = (GLushort *) src;
                M3G_ASSERT(array->stride == (GLsizei)(array->elementSize * sizeof(*dst)));
                
                while (values--) {
                    *dst++ = *srcShort++;
                }
            }
            break;

        default:
            M3G_ASSERT(0);      /* fatal internal error */
        }
    }

    m3gUnmapObject(M3G_INTERFACE(array), array->data);
    m3gInvalidateArray(array);
}

/*!
 * \brief Get a range of vertex array elements
 *
 * \param handle array handle
 * \param first  index of first vertex to set
 * \param count  number of total vertices to set
 * \param dstLength length of destination data
 * \param type  data type of destination data
 * \param dst   destination data
 */
M3G_API void m3gGetVertexArrayElements(M3GVertexArray handle,
                                       M3Gint first, M3Gsizei count,
                                       M3Gsizei dstLength, M3Gdatatype type, void *dst)
{
    VertexArray *array = (VertexArray *) handle;
    M3G_VALIDATE_OBJECT(array);

    M3G_ASSERT(array->numLocks == 0);

    /* Check errors */
    if (array->mapCount) {
        m3gRaiseError(M3G_INTERFACE(array), M3G_INVALID_OPERATION);
        return;
    }
    if (dst == NULL) {
        m3gRaiseError(M3G_INTERFACE(array), M3G_NULL_POINTER);
        return;
    }
    if (first < 0 || first + count > array->vertexCount) {
        m3gRaiseError(M3G_INTERFACE(array), M3G_INVALID_INDEX);
        return;
    }
    if (count < 0 || dstLength < count * array->elementSize) {
        m3gRaiseError(M3G_INTERFACE(array), M3G_INVALID_VALUE);
        return;
    }

    /* Data according to destination array type */
    {
        int values = count * array->elementSize;
        
        switch (array->elementType) {
        case GL_BYTE:
        case GL_UNSIGNED_BYTE:
            if (type != M3G_BYTE) {
                m3gRaiseError(M3G_INTERFACE(array), M3G_INVALID_OPERATION);
                return;
            }
            else {
                GLubyte *src =
                    ((GLubyte *)m3gMapObject(M3G_INTERFACE(array),
                                             array->data))
                    + first * array->stride;
                GLubyte *dstByte = (GLubyte *) dst;

                M3G_ASSERT(array->elementSize >= 2 && array->elementSize <= 4);
                M3G_ASSERT(array->stride == 4);
                
                while (values > 0) {
                    *dstByte++ = src[0];
                    *dstByte++ = src[1];
                    if (array->elementSize >= 3) {
                        *dstByte++ = src[2];
                    }
                    if (array->elementSize == 4) {
                        *dstByte++ = src[3];
                    }
                    src += 4;
                    values -= array->elementSize;
                }
            }
            break;
        
        case GL_SHORT:
        case GL_UNSIGNED_SHORT:
            if (type != M3G_SHORT) {
                m3gRaiseError(M3G_INTERFACE(array), M3G_INVALID_OPERATION);
                return;
            }
            else {
                GLushort *src =
                    ((GLushort *)m3gMapObject(M3G_INTERFACE(array),
                                              array->data))
                    + first * array->stride / 2;
                GLushort *dstShort = (GLushort *) dst;
                M3G_ASSERT(array->stride == (GLsizei)(array->elementSize * sizeof(*src)));
                
                while (values--) {
                    *dstShort++ = *src++;
                }
            }
            break;

        default:
            M3G_ASSERT(0);      /* fatal internal error */
        }
    }

    m3gUnmapObject(M3G_INTERFACE(array), array->data);
}

/*!
 * \brief Transform vertex array with
 * given transform and w.
 *
 * \param handle        array handle
 * \param transform     transform
 * \param out           output array to fill in
 * \param outLength     length of the output array
 * \param w             use w
 */
M3G_API void m3gTransformArray(M3GVertexArray handle,
                               M3GMatrix *transform,
                               M3Gfloat *out, M3Gint outLength,
                               M3Gbool w)
{
    M3Gbyte *bptr;
    M3Gshort *sptr;
    M3Gfloat *outPtr = out;
    M3Gint i;
    M3GVec4 vec;
    VertexArray *array = (VertexArray *) handle;
    M3G_VALIDATE_OBJECT(array);

    /* Check for errors */
    if (outLength < (4 * array->vertexCount) ||
        array->elementSize == 4) {
        m3gRaiseError(M3G_INTERFACE(array), M3G_INVALID_VALUE);
        return;
    }

    switch(array->elementType) {
        case GL_BYTE:
        case GL_UNSIGNED_BYTE:
            bptr = (M3Gbyte *)m3gMapObject(M3G_INTERFACE(array), array->data);

            for (i = 0; i < array->vertexCount * 4; i += 4) {
                vec.x = bptr[i + 0];
                vec.y = bptr[i + 1];
                vec.z = 0;
                if (array->elementSize == 3) {
                    vec.z = bptr[i + 2];
                }
                vec.w = (M3Gfloat)w;

                m3gTransformVec4(transform, &vec);

                *outPtr++ = vec.x;
                *outPtr++ = vec.y;
                *outPtr++ = vec.z;
                *outPtr++ = vec.w;
            }
            break;

        case GL_SHORT:
        case GL_UNSIGNED_SHORT:
            sptr = (M3Gshort *)m3gMapObject(M3G_INTERFACE(array), array->data);

            for (i = 0; i < array->vertexCount * array->elementSize; i += array->elementSize) {
                vec.x = sptr[i + 0];
                vec.y = sptr[i + 1];
                vec.z = 0;
                if (array->elementSize == 3) {
                    vec.z = sptr[i + 2];
                }
                vec.w = (M3Gfloat)w;

                m3gTransformVec4(transform, &vec);

                *outPtr++ = vec.x;
                *outPtr++ = vec.y;
                *outPtr++ = vec.z;
                *outPtr++ = vec.w;
            }
            break;
    }
    m3gUnmapObject(M3G_INTERFACE(array), array->data);
}

#undef DIRTY_ALPHA_FACTOR

