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
* Description: IndexBuffer implementation
*
*/


/*!
 * \internal
 * \file
 * \brief IndexBuffer implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_indexbuffer.h"

/*----------------------------------------------------------------------
 * Private functions
 *--------------------------------------------------------------------*/


/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief IndexBuffer destructor
 *
 * \param obj IndexBuffer object
 */
static void m3gDestroyIndexBuffer(Object *obj)
{
    IndexBuffer *ib = (IndexBuffer *) obj;
    M3G_VALIDATE_OBJECT(ib);

	{
		Interface *m3g = M3G_INTERFACE(ib);
		m3gFree(m3g, ib->indices);
		m3gFree(m3g, ib->lengths);
	}

    m3gDestroyObject(obj);
}

/*!
 * \internal
 * \brief Sends the contents of an IndexBuffer to the current GL
 * instance for processing
 *
 * \param buf IndexBuffer object
 */
static void m3gSendIndexBuffer(const IndexBuffer *buf)
{
    M3G_VALIDATE_OBJECT(buf);
    
    M3G_ASSERT(buf->indices);
    M3G_ASSERT(buf->glPrimitive == GL_TRIANGLE_STRIP);
    
    M3G_BEGIN_PROFILE(M3G_INTERFACE(buf), M3G_PROFILE_NGL_DRAW);
    glDrawElements(buf->glPrimitive, buf->indexCount, buf->glType, buf->indices);
    M3G_END_PROFILE(M3G_INTERFACE(buf), M3G_PROFILE_NGL_DRAW);

    M3G_ASSERT_GL;
}

/*!
 * \internal
 * \brief Gets triangle indices, used by mesh pick routine.
 *
 * \param buf IndexBuffer object
 * \param triangle triangle index
 * \param indices triangle indices
 * \retval M3G_TRUE  indices fetched
 * \retval M3G_FALSE no such triangle
 */
static M3Gbool m3gGetIndices(const IndexBuffer *buf,
                             M3Gint triangle,
                             M3Gint *indices) {
    M3Gubyte *bptr;
    M3Gushort *sptr;

    if(triangle + 2 >= buf->indexCount) return M3G_FALSE;

    switch(buf->glType) {
        case GL_UNSIGNED_BYTE:
            bptr = (M3Gubyte *)buf->indices;
            bptr += triangle;
            indices[0] = *bptr++;
            indices[1] = *bptr++;
            indices[2] = *bptr;
            break;

        case GL_UNSIGNED_SHORT:
            sptr = (M3Gushort *)buf->indices;
            sptr += triangle;
            indices[0] = (M3Gint) *sptr++;
            indices[1] = (M3Gint) *sptr++;
            indices[2] = (M3Gint) *sptr;
            break;
    }

    /* Winding */
    indices[3] = triangle & 1;

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original IndexBuffer object
 * \param cloneObj pointer to cloned IndexBuffer object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gIndexBufferDuplicate(const Object *originalObj,
                                       Object **cloneObj,
                                       Object **pairs,
                                       M3Gint *numPairs)
{
    M3Gint size;
    IndexBuffer *original = (IndexBuffer *)originalObj;
    IndexBuffer *clone = (IndexBuffer *)m3gAllocZ(originalObj->interface, sizeof(IndexBuffer));

    if (clone == NULL) {
        return M3G_FALSE;
    }

    *cloneObj = (Object *)clone;

    /* Call init since this object is 'manually' created */
    m3gInitObject((Object*) clone, originalObj->interface, M3G_CLASS_INDEX_BUFFER);

    if(!m3gObjectDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        return M3G_FALSE;
    }

    clone->indexCount = original->indexCount;
    clone->glType = original->glType;
    clone->glPrimitive = original->glPrimitive;
    clone->stripCount = original->stripCount;

    if (clone->glType == GL_UNSIGNED_BYTE) {
        size = clone->indexCount;
        clone->indices = m3gAlloc(originalObj->interface, size);
    }
    else {
        size = clone->indexCount * sizeof(M3Gshort);
        clone->indices = m3gAlloc(originalObj->interface, size);
    }

    clone->lengths = (M3Gushort *) m3gAlloc(originalObj->interface, (M3Gsize) clone->stripCount*2);

    if(clone->indices == NULL || clone->lengths == NULL) {
        /* Duplicate will call m3gDeleteObject */
        return M3G_FALSE;
    }

    m3gCopy(clone->lengths, original->lengths, clone->stripCount*2);
    m3gCopy(clone->indices, original->indices, size);

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Gets maximum index of this index buffer.
 *
 * \param buf IndexBuffer object
 * \return maximum index used
 */
static M3Gint m3gGetMaxIndex(const IndexBuffer *buf)
{
    return buf->maxIndex;
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const ObjectVFTable m3gvf_IndexBuffer = {
    m3gObjectApplyAnimation,
    m3gObjectIsCompatible,
    m3gObjectUpdateProperty,
    m3gObjectDoGetReferences,
    m3gObjectFindID,
    m3gIndexBufferDuplicate,
    m3gDestroyIndexBuffer
};

/*----------------------------------------------------------------------
 * Public functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates an implicit strip buffer
 *
 * \param interface     M3G interface
 * \param stripCount    number of triangle strips
 * \param stripLengths  array of strip lengths
 * \param firstIndex    first index
 * \retval IndexBuffer new IndexBuffer object
 * \retval NULL IndexBuffer creating failed
 */
M3G_API M3GIndexBuffer m3gCreateImplicitStripBuffer(
    M3GInterface interface,
    M3Gsizei stripCount,
    const M3Gsizei *stripLengths,
    M3Gint firstIndex)
{
	M3GIndexBuffer ib;
	M3Gint *stripIndices, i, indexCount = 0;
    Interface *m3g = (Interface *) interface;
    M3G_VALIDATE_INTERFACE(m3g);
	
	if (stripLengths == NULL) {
		m3gRaiseError(m3g, M3G_NULL_POINTER);
        return 0;
	}

    if (stripCount == 0) {
		m3gRaiseError(m3g, M3G_INVALID_VALUE);
        return 0;
    }

	for (i = 0; i < stripCount; i++) {
		if(stripLengths[i] < 3) {
			m3gRaiseError(m3g, M3G_INVALID_VALUE);
            return 0;
		}
		indexCount += stripLengths[i];
	}

	if (firstIndex < 0 ||
		(firstIndex + indexCount) > 65535) {
		m3gRaiseError(m3g, M3G_INVALID_INDEX);
        return 0;
	}

	stripIndices = m3gAlloc(m3g, indexCount * sizeof(M3Gint));

	if (stripIndices == NULL) {
		return 0;	/* automatic out of memory from m3gAlloc */
	}

	/* Generate explict arrays */
    
	for (i = 0; i < indexCount; i++) {
		stripIndices[i] = firstIndex + i;
	}

	ib = m3gCreateStripBuffer(interface,
                              M3G_TRIANGLE_STRIPS,
                              stripCount, stripLengths,
                              M3G_INT, indexCount, stripIndices);
	m3gFree(m3g, stripIndices);
	return ib;
}

/*!
 * \brief Creates an indexed triangle strip buffer
 *
 * \note Optimizes rendering by joining the indices. Also scans the
 * array for the maximum value, and allocates the storage using the
 * smallest possible data type.
 *
 * \param interface     M3G interface
 * \param primitive     primitive type, always M3G_TRIANGLE_STRIPS
 * \param stripCount    number of triangle strips
 * \param stripLengths  array of strip lengths
 * \param type          data type of indices
 * \param numIndices    number of indices
 * \param stripIndices  array of indices
 * \retval IndexBuffer new IndexBuffer object
 * \retval NULL IndexBuffer creating failed
 */
M3G_API M3GIndexBuffer m3gCreateStripBuffer(M3GInterface interface,
                                            M3Gprimitive primitive,
                                            M3Gsizei stripCount,
                                            const M3Gsizei *stripLengths,
                                            M3Gdatatype type,
                                            M3Gsizei numIndices,
                                            const void *stripIndices)
{
    M3Gsizei joinedIndexCount = 0;
    M3Gsizei originalIndexCount = 0;
    M3Gint maxIndex = 0;

    Interface *m3g = (Interface *) interface;
    M3G_VALIDATE_INTERFACE(m3g);

    if (primitive != M3G_TRIANGLE_STRIPS
        || (type != M3G_INT && type != M3G_UINT)) {
        m3gRaiseError(m3g, M3G_INVALID_ENUM);
        return 0;
    }
    if (stripIndices == NULL ||
    	stripLengths == NULL) {
        m3gRaiseError(m3g, M3G_NULL_POINTER);
        return 0;
    }

	if (stripCount == 0 || numIndices == 0) {
        m3gRaiseError(m3g, M3G_INVALID_VALUE);
        return 0;
	}

	{
        /* Find the maximum index and count the actual number of indices
         * required for joining the strips */
        
        int strip;
        M3Gint *idx = (M3Gint *) stripIndices;
        for (strip = 0; strip < stripCount; ++strip) {
            if(stripLengths[strip] < 3) {
                m3gRaiseError(m3g, M3G_INVALID_VALUE);
                return 0;
            }
            if (strip != 0) {
                joinedIndexCount += (M3Guint)((joinedIndexCount & 1) ? 3 : 2);
            }

            joinedIndexCount += (M3Guint) stripLengths[strip];
            originalIndexCount += (M3Guint) stripLengths[strip];

        	if (numIndices < originalIndexCount) {
        		m3gRaiseError(m3g, M3G_INVALID_VALUE);
                return 0;
        	}

            M3G_ASSERT(stripLengths[strip] > 0);
            {
                int i;
                for (i = 0; i < stripLengths[strip]; ++i, ++idx) {
                    if ((*idx & 0xFFFF0000u) != 0) { /* > 65535? */
                    	m3gRaiseError(m3g, M3G_INVALID_INDEX);
                        return 0;
                    }
                    if (*idx > maxIndex)
                        maxIndex = *idx;
                }
            }
        }
    }

    {
        /* Allocate the buffer object */
    
        IndexBuffer *buf = m3gAllocZ(m3g, sizeof(IndexBuffer));
        if (buf == NULL) {
            return 0;
        }
		/* IndexBuffer is derived from Object */
		m3gInitObject(&buf->object, m3g, M3G_CLASS_INDEX_BUFFER);

        buf->glPrimitive = GL_TRIANGLE_STRIP;
        buf->indexCount = joinedIndexCount;

        /* Allocate the index elements as either bytes or shorts,
         * depending on the maximum value we need to store. Note that
         * OpenGL ES does not support 32-bit indices */
        
        if (maxIndex <= 0xFF) {
            buf->indices = m3gAlloc(m3g, (M3Gsize) joinedIndexCount);
            buf->glType = GL_UNSIGNED_BYTE;
        }
        else {
            M3G_ASSERT(maxIndex <= 0xFFFF);
            buf->indices = m3gAlloc(m3g, (M3Gsize) joinedIndexCount*2);
            buf->glType = GL_UNSIGNED_SHORT;
        }
        
        /* Allocate space for original strip lengths */
        buf->lengths = (M3Gushort *) m3gAlloc(m3g, (M3Gsize) stripCount*2);

        if (buf->indices == NULL || buf->lengths == NULL) {
            m3gDeleteObject((M3GObject) buf);
            return 0;
        }

        buf->stripCount = stripCount;

        {
            /* Copy the indices, converting to the chosen type and
             * joining the strips as we go */
            
            M3Guint *src = (M3Guint*) stripIndices;     /* type asserted above */
            void *dstStrip = buf->indices;
            int strip;
            
            for (strip = 0; strip < stripCount; ++strip) {
                int i;

                buf->lengths[strip] = (M3Gushort) stripLengths[strip];

                /*@notfunction@*/
                #define COPY_STRIP(indexType) do {              \
                    indexType *dst = (indexType *) dstStrip;    \
                    if (strip != 0) {                           \
                        *dst++ = (indexType) *(src-1);          \
                        *dst++ = (indexType) *src;              \
                        if (stripLengths[strip-1] & 1) {        \
                            *dst++ = (indexType) *src;          \
                        }                                       \
                    }                                           \
                    for (i = 0; i < stripLengths[strip]; ++i) { \
                        *dst++ = (indexType) *src++;            \
                    }                                           \
                    dstStrip = (void *) dst;                    \
                    M3G_ASSERT(dst <= (indexType *)(buf->indices) + buf->indexCount); \
                } while (0)
                    
                switch (buf->glType) {
                    
                case GL_UNSIGNED_BYTE:
                    COPY_STRIP(GLubyte);
                    break;
                    
                case GL_UNSIGNED_SHORT:
                    COPY_STRIP(GLushort);
                    break;
                    
                default:
                    M3G_ASSERT(0);
                }
                
                #undef COPY_STRIP
            }
        }

        /* Store maximum index */
        buf->maxIndex = maxIndex;

        /* All done! */
        return (M3GIndexBuffer) buf;
    }
}

/*!
 * \brief Gets the number of index batches in an index buffer
 *
 * An index batch usually corresponds to a single OpenGL rendering
 * call.
 *
 * \param buffer  index buffer handle
 * \return number of rendering batches
 */
M3G_API M3Gint m3gGetBatchCount(M3GIndexBuffer buffer)
{
    M3G_VALIDATE_OBJECT(buffer);
    M3G_UNREF(buffer);
    return 1;
}

/*!
 * \brief Returns the indices in an index batch
 *
 * \param buffer      index buffer handle
 * \param batchIndex  batch index
 * \param indices     pointer to a buffer to hold the indices
 * \retval M3G_TRUE   buffer has explicit indices
 * \retval M3G_FALSE  buffer has implicit indices; only the first index
 *                    is stored in \c indices
 */
M3G_API M3Gbool m3gGetBatchIndices(M3GIndexBuffer buffer,
                                   M3Gint batchIndex,
                                   M3Gint *indices)
{
    M3Gint i, j, tri = 0;
    M3Gint triIndices[4] = {0, 0, 0, 0};
    M3G_VALIDATE_OBJECT(buffer);
    M3G_UNREF(batchIndex);

    for (i = 0; i < buffer->stripCount; i++) {
        for (j = 0; j < buffer->lengths[i] - 2; j++) {
            m3gGetIndices(buffer, tri, triIndices); 

            *indices++ = triIndices[0];
            if (triIndices[3] == 0) {
                *indices++ = triIndices[1];
                *indices++ = triIndices[2];
            }
            else {
                *indices++ = triIndices[2];
                *indices++ = triIndices[1];
            }

            ++tri;
        }

        /* Eliminate degenerate triangles */
        if (buffer->lengths[i] & 1) {
            tri += 5;
        }
        else {
            tri += 4;
        }
    }

    return M3G_TRUE;
}

/*!
 * \brief Returns the size of an index batch
 *
 * \param buffer      index buffer handle
 * \param batchIndex  batch index
 * \return number of indices in the batch
 */
M3G_API M3Gint m3gGetBatchSize(M3GIndexBuffer buffer, M3Gint batchIndex)
{
    M3Gint i, count = 0;
    M3G_VALIDATE_OBJECT(buffer);
    
    if (batchIndex != 0) {
        return 0;
    }

    for (i = 0; i < buffer->stripCount; i++) {
        count += buffer->lengths[i] - 2;
    }

    return count * 3;
}

/*!
 * \brief Returns the primitive type of an index buffer
 *
 * \param buffer  index buffer handle
 * \return type of primitives stored in the buffer
 */
M3G_API M3Gprimitive m3gGetPrimitive(M3GIndexBuffer buffer)
{
    M3G_UNREF(buffer);
    M3G_VALIDATE_OBJECT(buffer);
    M3G_ASSERT(buffer->glPrimitive == GL_TRIANGLE_STRIP);
    return M3G_TRIANGLE_STRIPS;
}

