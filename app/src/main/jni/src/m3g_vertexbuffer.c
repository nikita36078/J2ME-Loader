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
* Description: Vertex buffer implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Vertex buffer implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_vertexbuffer.h"
#include "m3g_vertexarray.h"

#include "m3g_appearance.h"
#include "m3g_node.h" /* for NODE_ALPHA_FACTOR_BITS */

/*----------------------------------------------------------------------
 * Private functions
 *--------------------------------------------------------------------*/

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Deletes a vertex buffer
 *
 * \param obj VertexBuffer object
 */
static void m3gDestroyVertexBuffer(Object *obj)
{
    M3Gint i;
    VertexBuffer *buffer = (VertexBuffer *) obj;
    M3G_VALIDATE_OBJECT(buffer);

    if (buffer->locked) {
        M3G_ASSERT(M3G_FALSE);
        m3gReleaseVertexBuffer(buffer);
    }

    M3G_ASSIGN_REF(buffer->vertices, NULL);
    M3G_ASSIGN_REF(buffer->normals, NULL);
    M3G_ASSIGN_REF(buffer->colors, NULL);
    for(i = 0; i < M3G_NUM_TEXTURE_UNITS; i++) {
        M3G_ASSIGN_REF(buffer->texCoords[i], NULL);
    }
    
    m3gDestroyObject(&buffer->object);
}

/*!
 * \internal
 * \brief Applies the scale and bias values of a vertex buffer to the
 * current GL state
 *
 * The scale and bias transformations are applied to the existing
 * values in the GL_MODELVIEW and GL_TEXTURE matrix stacks.
 *
 * \param buffer VertexBuffer object
 */
static void m3gApplyScaleAndBias(const VertexBuffer *buffer)
{
    M3G_VALIDATE_OBJECT(buffer);
    
    glMatrixMode(GL_TEXTURE);
    {
        M3Gint i;
        for (i = 0; i < M3G_NUM_TEXTURE_UNITS; ++i) {
            if (buffer->texCoords[i] != NULL) {
                glActiveTexture((GLenum)(GL_TEXTURE0 + i));
                glTranslatef(buffer->texCoordBias[i][0],
                             buffer->texCoordBias[i][1],
                             buffer->texCoordBias[i][2]);
                glScalef(buffer->texCoordScale[i],
                         buffer->texCoordScale[i],
                         buffer->texCoordScale[i]);
            }
        }
    }

    glMatrixMode(GL_MODELVIEW);
    if (buffer->vertices != NULL) {
        glTranslatef(buffer->vertexBias[0],
                     buffer->vertexBias[1],
                     buffer->vertexBias[2]);
        glScalef(buffer->vertexScale,
                 buffer->vertexScale,
                 buffer->vertexScale);
    }
}

/*!
 * \internal
 * \brief Locks a vertex buffer for subsequent rendering
 *
 * \param buffer        VertexBuffer object
 * \param alphaFactor   alpha factor as 1.16 fixed point
 */
static void m3gLockVertexBuffer(const VertexBuffer *buffer,
                                M3Gint alphaFactor)
{
    M3G_VALIDATE_OBJECT(buffer);
    M3G_ASSERT(!buffer->locked);

    if (buffer->colors != NULL) {
        glEnableClientState(GL_COLOR_ARRAY);
        m3gLockColorArray(buffer->colors, alphaFactor);
    }
    else {
        GLfixed r = buffer->defaultColor.r;
        GLfixed g = buffer->defaultColor.g;
        GLfixed b = buffer->defaultColor.b;
        GLfixed a = buffer->defaultColor.a * alphaFactor;

        r = (r << 8) + r + (r >> 7);
        g = (g << 8) + g + (g >> 7);
        b = (b << 8) + b + (b >> 7);
        a = (a >> (NODE_ALPHA_FACTOR_BITS - 8))
            + (a >> NODE_ALPHA_FACTOR_BITS)
            + (a >> (NODE_ALPHA_FACTOR_BITS + 7));
        
        glDisableClientState(GL_COLOR_ARRAY);
        glColor4x(r, g, b, a);
    }

    if (buffer->normals != NULL) {
        glEnableClientState(GL_NORMAL_ARRAY);
        m3gLockNormalArray(buffer->normals);
    }
    else {
        glDisableClientState(GL_NORMAL_ARRAY);
    }

    if (buffer->vertices != NULL) {
        glEnableClientState(GL_VERTEX_ARRAY);
        m3gLockVertexArray(buffer->vertices);
    }
    else {
        glDisableClientState(GL_VERTEX_ARRAY);
    }
    
    {
        M3Gint i;
        for (i = 0; i < M3G_NUM_TEXTURE_UNITS; ++i) {
            const VertexArray *array = buffer->texCoords[i];
            glClientActiveTexture(GL_TEXTURE0 + i);
            glActiveTexture(GL_TEXTURE0 + i);
            if (array != NULL) {
                glEnableClientState(GL_TEXTURE_COORD_ARRAY);
                m3gLockTexCoordArray(array);
            }
            else {
                glDisableClientState(GL_TEXTURE_COORD_ARRAY);
            }    
        }
    }
    
    ((VertexBuffer*)buffer)->locked = M3G_TRUE;
}

/*!
 * \internal
 * \brief Releases a vertex buffer
 *
 * \param buffer        VertexBuffer object
 */
static void m3gReleaseVertexBuffer(const VertexBuffer *buffer)
{
    M3G_VALIDATE_OBJECT(buffer);
    M3G_ASSERT(buffer->locked);

    if (buffer->colors != NULL) {
        m3gUnlockArray(buffer->colors);
    }
    if (buffer->normals != NULL) {
        m3gUnlockArray(buffer->normals);
    }
    if (buffer->vertices != NULL) {
        m3gUnlockArray(buffer->vertices);
    }
    {
        M3Gint i;
        for (i = 0; i < M3G_NUM_TEXTURE_UNITS; ++i) {
            const VertexArray *array = buffer->texCoords[i];
            if (array != NULL) {
                m3gUnlockArray(array);
            }
        }
    }
    
    ((VertexBuffer*)buffer)->locked = M3G_FALSE;
}

/*!
 * \internal
 * \brief Gets a vertex position. Scale and bias
 * are not applied.
 *
 * \param buffer    VertexBuffer object
 * \param idx       index of coordinate
 * \param v         vector to fill in
 * \retval          M3G_TRUE get ok
 * \retval          M3G_FALSE no such vertex
 */
static M3Gbool m3gGetVertex(const VertexBuffer *buffer, M3Gint idx, M3GVec3 *v)
{
    return m3gGetCoordinates(buffer->vertices, 3, idx, &v->x);
}

/*!
 * \internal
 * \brief Gets a normal coordinate.
 *
 * \param buffer    VertexBuffer object
 * \param idx       index of coordinate
 * \param v         vector to fill in
 * \retval          M3G_TRUE get ok
 * \retval          M3G_FALSE no such vertex
 */
static M3Gbool m3gGetNormal(const VertexBuffer *buffer, M3Gint idx, M3GVec3 *v)
{
    return m3gGetCoordinates(buffer->normals, 3, idx, &v->x);
}

/*!
 * \internal
 * \brief Gets a texture coordinate, used in pick routines.
 * Scale and bias are applied to the coordinates.
 *
 * \param buffer    VertexBuffer object
 * \param idx       index of coordinate
 * \param unit      texturing unit
 * \param v         vector to fill in
 * \retval          M3G_TRUE get ok
 * \retval          M3G_FALSE no such vertex
 */
static M3Gbool m3gGetTexCoord(const VertexBuffer *buffer, M3Gint idx, M3Gint unit, M3GVec3 *v)
{
    M3Gbool res;
    res = m3gGetCoordinates(buffer->texCoords[unit], 2, idx, &v->x);

    v->x = m3gMul(v->x, buffer->texCoordScale[unit]);
    v->y = m3gMul(v->y, buffer->texCoordScale[unit]);

    v->x = m3gAdd(v->x, buffer->texCoordBias[unit][0]);
    v->y = m3gAdd(v->y, buffer->texCoordBias[unit][1]);

    v->z = 0.f;

    return res;
}

/*!
 * \internal
 * \brief Gets vertex buffer positions bounding box.
 *
 * The bounding box is returned as floats, with scale and bias
 * applied.
 *
 * \param buffer        VertexBuffer object
 * \param boundingBox   bounding box float array
 */
static void m3gGetBoundingBox(VertexBuffer *vb, AABB *boundingBox)
{
    /* If timestamp is changed, refresh bounding box */
    
    if (vb->vertices && (m3gGetArrayTimestamp(vb->vertices)
                         != vb->verticesTimestamp)) {
        M3Gshort ab[6];
        vb->verticesTimestamp = m3gGetArrayTimestamp(vb->vertices);
        m3gGetArrayBoundingBox(vb->vertices, ab);
        
        vb->bbox.min[0] = m3gMadd(ab[0], vb->vertexScale, vb->vertexBias[0]);
        vb->bbox.min[1] = m3gMadd(ab[1], vb->vertexScale, vb->vertexBias[1]);
        vb->bbox.min[2] = m3gMadd(ab[2], vb->vertexScale, vb->vertexBias[2]);
        vb->bbox.max[0] = m3gMadd(ab[3], vb->vertexScale, vb->vertexBias[0]);
        vb->bbox.max[1] = m3gMadd(ab[4], vb->vertexScale, vb->vertexBias[1]);
        vb->bbox.max[2] = m3gMadd(ab[5], vb->vertexScale, vb->vertexBias[2]);

        /* Flip the bounding box if the scale was negative */
        
        if (vb->vertexScale < 0) {
            M3Gint i;
            for (i = 0; i < 3; ++i) {
                M3Gfloat t = vb->bbox.min[i];
                vb->bbox.min[i] = vb->bbox.max[i];
                vb->bbox.max[i] = t;
            }
        }
    }
    *boundingBox = vb->bbox;
}

/*!
 * \internal
 * \brief Gets vertex buffer timestamp.
 *
 * \param buffer        VertexBuffer object
 * \return timestamp
 */
static M3Gint m3gGetTimestamp(const VertexBuffer *buffer)
{
    if (buffer->vertices &&
        m3gGetArrayTimestamp(buffer->vertices) != buffer->verticesTimestamp) {
        return buffer->timestamp + 1;
    }
    return buffer->timestamp;
}

/**
 * Updates the vertex count bookkeeping when setting vertex
 * arrays.
 *
 * \param buffer        VertexBuffer object
 * \param oldArray      VertexArray object
 * \param newArray      VertexArray object
 * \param maskBit       array mask bit
 */
static void m3gUpdateArray(VertexBuffer *buffer,
                           VertexArray *oldArray,
                           VertexArray *newArray,
                           M3Gbitmask maskBit)
{
    M3Gint change = (oldArray == NULL && newArray != NULL) ?  1 :
        (oldArray != NULL && newArray == NULL) ? -1 :
        0;

    /* If adding or replacing an array, set the initial vertex count,
     * or compare the new array against the current vertex count */
    
    if (newArray != NULL) {
    	if (buffer->arrayCount == 0 || (buffer->arrayCount == 1
                                        && change == 0)) {
    		buffer->vertexCount = m3gGetArrayVertexCount(newArray);
    	}
    	else if (m3gGetArrayVertexCount(newArray) != buffer->vertexCount) {
            m3gRaiseError(M3G_INTERFACE(buffer), M3G_INVALID_VALUE);
            return;
    	}
    }

    /* Update the array bitmask */
    
    if (newArray != NULL) {
        buffer->arrayMask |= maskBit;
    }
    else {
        buffer->arrayMask &= ~maskBit;
    }

    /* Update the array count, and reset the vertex count to zero if
     * no arrays remain */
    
    buffer->arrayCount += change;
    if (buffer->arrayCount == 0) {
        M3G_ASSERT(buffer->arrayMask == 0);
        buffer->vertexCount = 0;
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method
 *
 * \param self VertexBuffer object
 * \param references array of reference objects
 * \return number of references
 */
static M3Gint m3gVertexBufferDoGetReferences(Object *self, Object **references)
{
    VertexBuffer *vb = (VertexBuffer *)self;
    M3Gint i, num = m3gObjectDoGetReferences(self, references);
    if (vb->vertices != NULL) {
        if (references != NULL)
            references[num] = (Object *)vb->vertices;
        num++;
    }
    if (vb->normals != NULL) {
        if (references != NULL)
            references[num] = (Object *)vb->normals;
        num++;
    }
    if (vb->colors != NULL) {
        if (references != NULL)
            references[num] = (Object *)vb->colors;
        num++;
    }
    for (i = 0; i < M3G_NUM_TEXTURE_UNITS; i++) {
        if (vb->texCoords[i] != NULL) {
            if (references != NULL)
                references[num] = (Object *)vb->texCoords[i];
            num++;
        }
    }
    return num;
}

/*!
 * \internal
 * \brief Overloaded Object3D method
 */
static Object *m3gVertexBufferFindID(Object *self, M3Gint userID)
{
    int i;
    VertexBuffer *vb = (VertexBuffer *)self;
    Object *found = m3gObjectFindID(self, userID);
    
    if (!found && vb->vertices != NULL) {
        found = m3gFindID((Object*) vb->vertices, userID);
    }
    if (!found && vb->normals != NULL) {
        found = m3gFindID((Object*) vb->normals, userID);
    }
    if (!found && vb->colors != NULL) {
        found = m3gFindID((Object*) vb->colors, userID);
    }
    for (i = 0; !found && i < M3G_NUM_TEXTURE_UNITS; ++i) {
        if (vb->texCoords[i] != NULL) {
            found = m3gFindID((Object*) vb->texCoords[i], userID);
        }
    }
    return found;
}

/*!
 * \internal
 * \brief Duplicates vertex buffer data and array configuration
 */
static void m3gDuplicateVertexBufferData(VertexBuffer *clone,
                                         const VertexBuffer *original)
{
    M3Gint i;
    
    clone->vertexScale = original->vertexScale;
    m3gCopy(clone->vertexBias, original->vertexBias, 3 * sizeof(GLfloat));
    clone->defaultColor = original->defaultColor;
    clone->locked = original->locked;
    clone->vertexCount = original->vertexCount;
    clone->arrayCount = original->arrayCount;
    clone->arrayMask = original->arrayMask;
    clone->timestamp = original->timestamp;
    
    for (i = 0; i < M3G_NUM_TEXTURE_UNITS; i++) {
        clone->texCoordScale[i] = original->texCoordScale[i];
        clone->texCoordBias[i][0] = original->texCoordBias[i][0];
        clone->texCoordBias[i][1] = original->texCoordBias[i][1];
        clone->texCoordBias[i][2] = original->texCoordBias[i][2];
        M3G_ASSIGN_REF(clone->texCoords[i], original->texCoords[i]);
    }
    M3G_ASSIGN_REF(clone->colors, original->colors);
    M3G_ASSIGN_REF(clone->normals, original->normals);
    M3G_ASSIGN_REF(clone->vertices, original->vertices);
}

/*!
 * \internal
 * \brief Overloaded Object3D method
 *
 * \param originalObj original VertexBuffer object
 * \param cloneObj pointer to cloned VertexBuffer object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gVertexBufferDuplicate(const Object *originalObj,
                                        Object **cloneObj,
                                        Object **pairs,
                                        M3Gint *numPairs)
{
    VertexBuffer *original = (VertexBuffer*) originalObj;
    VertexBuffer *clone;
    M3G_ASSERT(*cloneObj == NULL); /* no derived classes */

    /* Create the clone object */
    
    clone = (VertexBuffer*) m3gCreateVertexBuffer(originalObj->interface);
    if (!clone) {
        return M3G_FALSE;
    }
    *cloneObj = (Object *)clone;

    /* Duplicate base class data */
    
    if (!m3gObjectDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        return M3G_FALSE;
    }

    /* Duplicate our own data */

    m3gDuplicateVertexBufferData(clone, original);
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
static M3Gbool m3gVertexBufferIsCompatible(M3Gint property)
{
    switch (property) {
    case M3G_ANIM_ALPHA:
    case M3G_ANIM_COLOR:
        return M3G_TRUE;
    default:
        return m3gObjectIsCompatible(property);
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method
 *
 * \param self          VertexBuffer object
 * \param property      animation property
 * \param valueSize     size of value array
 * \param value         value array
 */
static void m3gVertexBufferUpdateProperty(Object *self,
                                          M3Gint property,
                                          M3Gint valueSize,
                                          const M3Gfloat *value)
{
    VertexBuffer *buffer = (VertexBuffer *) self;
    M3G_VALIDATE_OBJECT(buffer);
    M3G_ASSERT_PTR(value);

    switch (property) {
    case M3G_ANIM_ALPHA:
        M3G_ASSERT(valueSize >= 1);
        buffer->defaultColor.a =
            (GLubyte)m3gAdd(m3gMul(255.f, m3gClampFloat(value[0], 0.f, 1.f)), 0.5f);
        break;
    case M3G_ANIM_COLOR:
        M3G_ASSERT(valueSize >= 3);
        buffer->defaultColor.r =
            (GLubyte)m3gAdd(m3gMul(255.f, m3gClampFloat(value[0], 0.f, 1.f)), 0.5f);
        buffer->defaultColor.g =
            (GLubyte)m3gAdd(m3gMul(255.f, m3gClampFloat(value[1], 0.f, 1.f)), 0.5f);
        buffer->defaultColor.b =
            (GLubyte)m3gAdd(m3gMul(255.f, m3gClampFloat(value[2], 0.f, 1.f)), 0.5f);
        break;
    default:
        m3gObjectUpdateProperty(self, property, valueSize, value);
    }
}

/*!
 * \internal
 * \brief Checks that a vertex buffer can be properly rendered
 *
 * The vertex format required by \c app is compared against the vertex
 * arrays included in \c vb, and \c vb is checked to have at least \c
 * maxIndex vertex entries.
 *
 * \param vb        VertexBuffer object
 * \param app       Appearance object
 * \param maxIndex  maximum index in index buffer
 * \retval M3G_TRUE  valid state
 * \retval M3G_FALSE invalid state
 */
static M3Gbool m3gValidateVertexBuffer(const VertexBuffer *vb,
                                       const Appearance *app,
                                       M3Gsizei maxIndex)
{
    M3Gbitmask reqMask;
    M3G_UNREF(app);
    reqMask = M3G_POSITION_BIT;
    if ((m3gGetArrayMask(vb) & reqMask) != reqMask) {
        return M3G_FALSE;
    }
    return (m3gGetNumVertices(vb) > maxIndex);
}

/*!
 * \internal
 * \brief Sets a vertex buffer up for subsequent vertex modification
 *
 * A specified subset of the arrays of \c srcBuffer will be replicated
 * in \c buffer, without copying the contents.  The others will be
 * copied as references only.
 *
 * \param buffer       the modified buffer
 * \param srcBuffer    the source buffer
 * \param arrayMask    bitmask of arrays to modify
 * \param createArrays M3G_TRUE to create the arrays specified by
 *                     \c arrayMask, M3G_FALSE to leave them NULL
 * \retval M3G_TRUE success
 * \retval M3G_FALSE out of memory
 */
static M3Gbool m3gMakeModifiedVertexBuffer(VertexBuffer *buffer,
                                           const VertexBuffer *srcBuffer,
                                           M3Gbitmask arrayMask,
                                           M3Gbool createArrays)
{
    M3G_VALIDATE_OBJECT(buffer);
    M3G_VALIDATE_OBJECT(srcBuffer);
    {
        Interface *m3g = M3G_INTERFACE(buffer);
        VertexArray *array;
        int i;

        /* First, just copy the data from the other buffer */

        m3gDuplicateVertexBufferData(buffer, srcBuffer);
        
        /* Now, override the specified arrays: release the existing
         * array, and either allocate a new one or leave as NULL,
         * depending on the value of the createArrays flag */

#       define MODIFY_ARRAY(bit, name)                          \
            if (arrayMask & (bit)) {                            \
                array = NULL;                                   \
                if (srcBuffer->name && createArrays) {          \
                    array = m3gCreateVertexArray(               \
                        m3g,                                    \
                        srcBuffer->name->vertexCount,           \
                        srcBuffer->name->elementSize,           \
                        srcBuffer->name->elementType == GL_SHORT ? M3G_SHORT : M3G_BYTE); \
                    if (!array) {                               \
                        return M3G_FALSE;                       \
                    }                                           \
                }                                               \
                m3gUpdateArray(buffer, buffer->name, array, bit); \
                M3G_ASSIGN_REF(buffer->name, array);            \
            }

        MODIFY_ARRAY(M3G_POSITION_BIT, vertices);
        MODIFY_ARRAY(M3G_COLOR_BIT, colors);
        MODIFY_ARRAY(M3G_NORMAL_BIT, normals);
        
        for (i = 0; i < M3G_NUM_TEXTURE_UNITS; ++i) {
            MODIFY_ARRAY(M3G_TEXCOORD0_BIT << i, texCoords[i]);
        }

#       undef MODIFY_ARRAY

        return M3G_TRUE;
    }
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const ObjectVFTable m3gvf_VertexBuffer = {
    m3gObjectApplyAnimation,
    m3gVertexBufferIsCompatible,
    m3gVertexBufferUpdateProperty,
    m3gVertexBufferDoGetReferences,
    m3gVertexBufferFindID,
    m3gVertexBufferDuplicate,
    m3gDestroyVertexBuffer
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates an empty vertex buffer
 *
 * \param interface    M3G interface
 * \retval VertexBuffer new VertexBuffer object
 * \retval NULL VertexBuffer creating failed
 */
/*@access M3Ginterface@*/
/*@access M3Gobject@*/
M3G_API M3GVertexBuffer m3gCreateVertexBuffer(M3GInterface interface)
{
    Interface *m3g = (Interface *) interface;
    M3G_VALIDATE_INTERFACE(m3g);
    
    {
        VertexBuffer *buffer = m3gAllocZ(m3g, sizeof(VertexBuffer));

        if (buffer != NULL) {
            m3gInitObject(&buffer->object, m3g, M3G_CLASS_VERTEX_BUFFER);
        
            /* Set default color to white */
            buffer->defaultColor.r = (GLubyte) 0xFF;
            buffer->defaultColor.g = (GLubyte) 0xFF;
            buffer->defaultColor.b = (GLubyte) 0xFF;
            buffer->defaultColor.a = (GLubyte) 0xFF;
        }

        return (M3GVertexBuffer) buffer;
    }
}

/*!
 * \brief Sets the color array of a vertex buffer
 *
 * \param hBuffer   VertexBuffer object
 * \param hArray    VertexArray object
 */
/*@access M3Gobject@*/
M3G_API void m3gSetColorArray(M3GVertexBuffer hBuffer, M3GVertexArray hArray)
{
    VertexBuffer *buffer = (VertexBuffer *) hBuffer;
    VertexArray *array = (VertexArray *) hArray;
    M3G_VALIDATE_OBJECT(buffer);
    
    if (array != NULL) {
        M3G_VALIDATE_OBJECT(array);

        /* Check for errors */
        if (!m3gInRange(array->elementSize, 3, 4) ||
            array->elementType != M3G_GLTYPE(M3G_BYTE)) {
            m3gRaiseError(M3G_INTERFACE(array), M3G_INVALID_VALUE);
            return;
        }
    }
    
    m3gUpdateArray(buffer, buffer->colors, array, M3G_COLOR_BIT);
    M3G_ASSIGN_REF(buffer->colors, array);
    ++buffer->timestamp;
}

/*!
 * \brief Sets the normal array of a vertex buffer
 *
 * \param hBuffer   VertexBuffer object
 * \param hArray    VertexArray object
 */
/*@access M3Gobject@*/
M3G_API void m3gSetNormalArray(M3GVertexBuffer hBuffer, M3GVertexArray hArray)
{
    VertexBuffer *buffer = (VertexBuffer *) hBuffer;
    VertexArray *array = (VertexArray *) hArray;
    M3G_VALIDATE_OBJECT(buffer);
    if (array != NULL) {
        M3G_VALIDATE_OBJECT(array);

        /* Check for errors */
        if (array->elementSize != 3) {
            m3gRaiseError(M3G_INTERFACE(array), M3G_INVALID_VALUE);
            return;
        }
    }
    
    m3gUpdateArray(buffer, buffer->normals, array, M3G_NORMAL_BIT);
    M3G_ASSIGN_REF(buffer->normals, array);
    ++buffer->timestamp;
}

/*!
 * \brief Sets the texture coordinate array of a vertex buffer
 *
 * \param hBuffer   VertexBuffer object
 * \param unit      texturing unit
 * \param hArray    VertexArray object
 * \param scale     scale
 * \param bias      bias array
 * \param biasLength bias array length
 */
/*@access M3Gobject@*/
M3G_API void m3gSetTexCoordArray(M3GVertexBuffer hBuffer,
                                 M3Gint unit,
                                 M3GVertexArray hArray,
                                 M3Gfloat scale, M3Gfloat *bias,
                                 M3Gint biasLength)
{
    VertexBuffer *buffer = (VertexBuffer *) hBuffer;
    VertexArray *array = (VertexArray *) hArray;

    M3G_VALIDATE_OBJECT(buffer);
    if (array != NULL) {
        M3G_VALIDATE_OBJECT(array);
    }

    /* Check errors */
    
    if (unit < 0 || unit >= M3G_NUM_TEXTURE_UNITS) {
        m3gRaiseError(M3G_INTERFACE(buffer), M3G_INVALID_INDEX);
        return;
    }

    if (array != NULL) {
        if (array->elementSize != 2 && array->elementSize != 3) {
            m3gRaiseError(M3G_INTERFACE(buffer), M3G_INVALID_VALUE);
            return;
        }
        if (bias != NULL && biasLength < array->elementSize) {
            m3gRaiseError(M3G_INTERFACE(buffer), M3G_INVALID_VALUE);
            return;
        }
    }

    m3gUpdateArray(buffer, buffer->texCoords[unit], array, M3G_TEXCOORD0_BIT << unit);
    M3G_ASSIGN_REF(buffer->texCoords[unit], array);

    if (array != NULL && bias != NULL) {
        buffer->texCoordBias[unit][0] = bias[0];
        buffer->texCoordBias[unit][1] = bias[1];
        if (biasLength > 2) {
            buffer->texCoordBias[unit][2] = bias[2];
        }
    }
    else {
        buffer->texCoordBias[unit][0] = 0.f;
        buffer->texCoordBias[unit][1] = 0.f;
        buffer->texCoordBias[unit][2] = 0.f;
    }

    buffer->texCoordScale[unit] = scale;

    ++buffer->timestamp;
}

/*!
 * \brief Sets the vertex array of a vertex buffer
 *
 * \param hBuffer   VertexBuffer object
 * \param hArray    VertexArray object
 * \param scale     scale
 * \param bias      bias array
 * \param biasLength bias array length
 */
/*@access M3Gobject@*/
M3G_API void m3gSetVertexArray(M3GVertexBuffer hBuffer,
                               M3GVertexArray hArray,
                               M3Gfloat scale,
                               M3Gfloat *bias, M3Gint biasLength)
{
    VertexBuffer *buffer = (VertexBuffer *) hBuffer;
    VertexArray *array = (VertexArray *) hArray;
    M3G_VALIDATE_OBJECT(buffer);
    if (array != NULL) {
        M3G_VALIDATE_OBJECT(array);
    }

    if (array != NULL && array->elementSize != 3) {
        m3gRaiseError(M3G_INTERFACE(buffer), M3G_INVALID_VALUE);
        return;
    }

    if (array != NULL && bias != NULL && biasLength < 3) {
        m3gRaiseError(M3G_INTERFACE(buffer), M3G_INVALID_VALUE);
        return;
    }

    m3gUpdateArray(buffer, buffer->vertices, array, M3G_POSITION_BIT);
    M3G_ASSIGN_REF(buffer->vertices, array);

    if (array != NULL && bias != NULL) {
        buffer->vertexBias[0] = bias[0];
        buffer->vertexBias[1] = bias[1];
        buffer->vertexBias[2] = bias[2];
    }
    else {
        buffer->vertexBias[0] = 0.f;
        buffer->vertexBias[1] = 0.f;
        buffer->vertexBias[2] = 0.f;
    }
    buffer->vertexScale = scale;

    /* Make sure we invalidate the cached bounding box */
    
    ++buffer->timestamp;
    if (array != NULL) {
        buffer->verticesTimestamp = ~m3gGetArrayTimestamp(array); /*lint !e502 ok for signed */
    }
}

/*!
 * \brief Sets the default color of a vertex buffer
 *
 * \param handle    VertexBuffer object
 * \param ARGB      default color as ARGB
 */
/*@access M3Gobject@*/

M3G_API void m3gSetVertexDefaultColor(M3GVertexBuffer handle, M3Guint ARGB)
{
    VertexBuffer *buffer = (VertexBuffer *) handle;
    M3G_VALIDATE_OBJECT(buffer);    

    buffer->defaultColor.b = (GLubyte)(ARGB);
    buffer->defaultColor.g = (GLubyte)(ARGB >> 8);
    buffer->defaultColor.r = (GLubyte)(ARGB >> 16);
    buffer->defaultColor.a = (GLubyte)(ARGB >> 24);
    ++buffer->timestamp;
}

/*!
 * \brief Gets the default color of a vertex buffer
 * \param handle    VertexBuffer object
 * \return          default color as ARGB
 */
/*@access M3Gobject@*/

M3G_API M3Guint m3gGetVertexDefaultColor(M3GVertexBuffer handle)
{
	unsigned ARGB;
    VertexBuffer *buffer = (VertexBuffer *) handle;
    M3G_VALIDATE_OBJECT(buffer);    

	ARGB = buffer->defaultColor.a;
	ARGB <<= 8;
	ARGB |= buffer->defaultColor.r;
	ARGB <<= 8;
	ARGB |= buffer->defaultColor.g;
	ARGB <<= 8;
	ARGB |= buffer->defaultColor.b;

	return ARGB;
}

/*!
 * \brief Gets vertex array of a vertex buffer
 *
 * \param handle    VertexBuffer object
 * \param which     which array to get
 *                  \arg M3G_GET_POSITIONS
 *                  \arg M3G_GET_NORMALS
 *                  \arg M3G_GET_COLORS
 *                  \arg M3G_GET_TEXCOORDS0
 *                  \arg M3G_GET_TEXCOORDS0 + 1
 * \param scaleBias array for scale and bias (s, bx, by, bz)
 * \param sbLength  length of scale bias array
 */

/*@access M3Gobject@*/
M3G_API M3GVertexArray m3gGetVertexArray(M3GVertexBuffer handle,
                                         M3Gint which,
                                         M3Gfloat *scaleBias, M3Gint sbLength)
{
    M3Gint tci = 1;
    VertexBuffer *buffer = (VertexBuffer *) handle;
    M3G_VALIDATE_OBJECT(buffer);

    switch(which) {
    case M3G_GET_POSITIONS:
        if (scaleBias != NULL && sbLength < 4) {
            m3gRaiseError(M3G_INTERFACE(buffer), M3G_INVALID_VALUE);
            return 0;
        }

        if (scaleBias != NULL) {
            scaleBias[0] = buffer->vertexScale;
            scaleBias[1] = buffer->vertexBias[0];
            scaleBias[2] = buffer->vertexBias[1];
            scaleBias[3] = buffer->vertexBias[2];
        }
        return buffer->vertices;
    case M3G_GET_NORMALS: return buffer->normals;
    case M3G_GET_COLORS:  return buffer->colors;
    case M3G_GET_TEXCOORDS0:
        --tci;
        /* Flow through */
    case M3G_GET_TEXCOORDS0 + 1:
        if (buffer->texCoords[tci] != NULL) {
            if (scaleBias != NULL && sbLength < (buffer->texCoords[tci]->elementSize + 1)) {
                m3gRaiseError(M3G_INTERFACE(buffer), M3G_INVALID_VALUE);
                return 0;
            }
    
            if (scaleBias != NULL) {
                scaleBias[0] = buffer->texCoordScale[tci];
                scaleBias[1] = buffer->texCoordBias[tci][0];
                scaleBias[2] = buffer->texCoordBias[tci][1];
                if (buffer->texCoords[tci]->elementSize > 2) {
                    scaleBias[3] = buffer->texCoordBias[tci][2];
                }
            }
        }
        return buffer->texCoords[tci];
    default:
        m3gRaiseError(M3G_INTERFACE(buffer), M3G_INVALID_VALUE);
        break;
    }

    return 0; /* Error */
}

/*!
 * \brief Gets vertex count of a vertex buffer
 *
 * \param handle    VertexBuffer object
 * \return vertex count
 */

/*@access M3Gobject@*/
M3G_API M3Gint  m3gGetVertexCount(M3GVertexBuffer handle)
{
    VertexBuffer *buffer = (VertexBuffer *) handle;
    M3G_VALIDATE_OBJECT(buffer);    

    return buffer->vertexCount;    
}

