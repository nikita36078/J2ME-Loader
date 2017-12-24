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
* Description: Transformable implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Transformable implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_transformable.h"
#include "m3g_math.h"

/*----------------------------------------------------------------------
 * Constructor & destructor
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Initializes a Transformable object. See specification
 * for default values.
 *
 * \param tf            Transformable object
 * \param m3g           M3G interface
 * \param vfTable       virtual function table
 */
static void m3gInitTransformable(
    Transformable *tf, Interface *m3g, M3GClass classID)
{
    m3gIdentityQuat(&tf->orientation);
    tf->sx = tf->sy = tf->sz = 1.f;
    
    m3gInitObject(&tf->object, m3g, classID);
}

/*!
 * \internal
 * \brief Destroys this Transformable object.
 *
 * \param obj Transformable object
 */
static void m3gDestroyTransformable(Object *obj)
{
    Transformable *tf = (Transformable *) obj;
    M3G_VALIDATE_OBJECT(tf);

    m3gInvalidateCachedTransforms(m3gGetTransformCache(M3G_INTERFACE(tf)), tf);
    if (tf->matrix != NULL) {
        m3gFree(M3G_INTERFACE(tf), tf->matrix);
    }

    m3gDestroyObject(obj);
}

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Invalidates cached data after changing transformations
 */
static void m3gInvalidateTransformable(Transformable *self) 
{
    /* Invalidate bounding boxes up the tree */
    
    if (M3G_CLASS(self) != M3G_CLASS_TEXTURE) {
        Node *node = (Node *) self;
        if (node->parent && (node->hasRenderables || node->hasBones)) {
            m3gInvalidateNode(node->parent, NODE_BBOX_BIT|NODE_TRANSFORMS_BIT);
        }
    }

    /* Invalidate any transformation cache entries */
    {
        TCache *tc = m3gGetTransformCache(M3G_INTERFACE(self));
        m3gInvalidateCachedTransforms(tc, self);
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param property      animation property
 * \retval M3G_TRUE     property supported
 * \retval M3G_FALSE    property not supported
 */
static M3Gbool m3gTransformableIsCompatible(M3Genum property)
{
    switch (property) {
    case M3G_ANIM_ORIENTATION:
    case M3G_ANIM_TRANSLATION:
    case M3G_ANIM_SCALE:
        return M3G_TRUE;
    default:
        return m3gObjectIsCompatible(property);
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self          Transformable object
 * \param property      animation property
 * \param valueSize     size of value array
 * \param value         value array
 */
static void m3gTransformableUpdateProperty(Object *self,
                                           M3Genum property,
                                           M3Gint valueSize,
                                           const M3Gfloat *value)
{
    Transformable *t = (Transformable *)self;
    M3G_VALIDATE_OBJECT(t);
    M3G_ASSERT_PTR(value);
    
    {
        M3Gbool invalidate = M3G_TRUE;
    
        switch (property) {
        case M3G_ANIM_ORIENTATION:
            M3G_ASSERT(valueSize >= 4);
            m3gSetQuat(&t->orientation, value);
            m3gNormalizeQuat(&t->orientation);
            break;
        case M3G_ANIM_TRANSLATION:
            M3G_ASSERT(valueSize >= 3);
            t->tx = value[0];
            t->ty = value[1];
            t->tz = value[2];
            break;
        case M3G_ANIM_SCALE:
            M3G_ASSERT(valueSize >= 1);
            if (valueSize == 1) {
                t->sx = t->sy = t->sz = value[0];
            }
            else {
                M3G_ASSERT(valueSize >= 3);
                t->sx = value[0];
                t->sy = value[1];
                t->sz = value[2];
            }
            break;
        default:
            m3gObjectUpdateProperty(self, property, valueSize, value);
            invalidate = M3G_FALSE;
        }

        if (invalidate) {
            m3gInvalidateTransformable(t);
        }
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original Transformable object
 * \param cloneObj pointer to cloned Transformable object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gTransformableDuplicate(const Object *originalObj,
                                         Object **cloneObj,
                                         Object **pairs,
                                         M3Gint *numPairs)
{
    Transformable *original = (Transformable *)originalObj;
    Transformable *clone = (Transformable *)*cloneObj;
    M3G_ASSERT_PTR(clone); /* abstract class, must be derived */

    /* Duplicate base class data */
    
    if (!m3gObjectDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        return M3G_FALSE;
    }

    /* Duplicate our own data */
    
    clone->orientation = original->orientation;
    clone->sx = original->sx;
    clone->sy = original->sy;
    clone->sz = original->sz;
    clone->tx = original->tx;
    clone->ty = original->ty;
    clone->tz = original->tz;
    if (original->matrix != NULL) {
        clone->matrix = (Matrix *)m3gAlloc(originalObj->interface,
                                           sizeof(Matrix));
        if (clone->matrix == NULL) {
            return M3G_FALSE;
        }
        *clone->matrix = *original->matrix;
    }
    return M3G_TRUE;
}

/*!
 * \brief Get the inverse of the composite transformation
 *
 * \param handle        Transformable object
 * \param transform     transformation matrix
 * \return M3G_TRUE on success, M3G_FALSE if the transformation
 *         could not be inverted
 *
 * \note M3G_ARITHMETIC_ERROR is automatically raised when attempting
 * to invert a singular transformation
 */
static M3Gbool m3gGetInverseCompositeTransform(M3GTransformable handle,
                                               M3GMatrix *transform)
{
    const Transformable *obj = (Transformable *) handle;
    M3G_VALIDATE_OBJECT(obj);

	/* Apply scale and the generic matrix, then invert */
    
    m3gScalingMatrix(transform, obj->sx, obj->sy, obj->sz);
    if (obj->matrix != NULL) {
        m3gMulMatrix(transform, obj->matrix);
    }
    {
        M3Gbool ok;
        M3G_BEGIN_PROFILE(M3G_INTERFACE(obj), M3G_PROFILE_TRANSFORM_INVERT);
        ok = m3gInvertMatrix(transform);
        M3G_END_PROFILE(M3G_INTERFACE(obj), M3G_PROFILE_TRANSFORM_INVERT);
        if (!ok) {
            m3gRaiseError(M3G_INTERFACE(obj), M3G_ARITHMETIC_ERROR);
            return M3G_FALSE;
        }
    }
    
    /* Apply the rest of the component based transformation */
    {
        Quat temp = obj->orientation;
        temp.w = m3gNegate(temp.w);
        m3gRotateMatrixQuat(transform, &temp);
    }
    m3gTranslateMatrix(transform, m3gNegate(obj->tx), m3gNegate(obj->ty), m3gNegate(obj->tz));

#   if defined(M3G_DEBUG)
    if (m3gGetClass((M3GObject) handle) != M3G_CLASS_TEXTURE) {
        M3G_ASSERT(m3gIsWUnity(transform));
    }
#   endif /* M3G_DEBUG */

    return M3G_TRUE;
}

/*----------------------------------------------------------------------
 * Public interface functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Set orientation as angle axis.
 *
 * \param handle    Transformable object
 * \param angle     angle of rotation
 * \param ax        x component of the rotation axis
 * \param ay        y component of the rotation axis
 * \param az        z component of the rotation axis
 */
M3G_API void m3gSetOrientation(M3GTransformable handle,
                               M3Gfloat angle,
                               M3Gfloat ax, M3Gfloat ay, M3Gfloat az)
{
    Transformable *obj = (Transformable *) handle;
    M3G_VALIDATE_OBJECT(obj);

	if (angle != 0 && ax == 0 && ay == 0 && az == 0) {
		m3gRaiseError(M3G_INTERFACE(obj), M3G_INVALID_VALUE);
        return;
	}

	m3gSetAngleAxis(&obj->orientation, angle, ax, ay, az);
    
    m3gInvalidateTransformable(obj);
}

/*!
 * \brief Post rotate as angle axis.
 *
 * \param handle    Transformable object
 * \param angle     angle of rotation
 * \param ax        x component of the rotation axis
 * \param ay        y component of the rotation axis
 * \param az        z component of the rotation axis
 */
M3G_API void m3gPostRotate(M3GTransformable handle,
                           M3Gfloat angle,
                           M3Gfloat ax, M3Gfloat ay, M3Gfloat az)
{
    Transformable *obj = (Transformable *) handle;
    Quat rotate;
    M3G_VALIDATE_OBJECT(obj);

	if(angle != 0 && ax == 0 && ay == 0 && az == 0) {
		m3gRaiseError(M3G_INTERFACE(obj), M3G_INVALID_VALUE);
        return;
	}

	m3gSetAngleAxis(&rotate, angle, ax, ay, az);
	m3gMulQuat(&obj->orientation, &rotate);
    
    m3gInvalidateTransformable(obj);
}

/*!
 * \brief Pre rotate as angle axis.
 *
 * \param handle    Transformable object
 * \param angle     angle of rotation
 * \param ax        x component of the rotation axis
 * \param ay        y component of the rotation axis
 * \param az        z component of the rotation axis
 */
M3G_API void m3gPreRotate(M3GTransformable handle,
                          M3Gfloat angle,
                          M3Gfloat ax, M3Gfloat ay, M3Gfloat az)
{
    Transformable *obj = (Transformable *) handle;
    Quat rotate;
    M3G_VALIDATE_OBJECT(obj);

	if(angle != 0 && ax == 0 && ay == 0 && az == 0) {
		m3gRaiseError(M3G_INTERFACE(obj), M3G_INVALID_VALUE);
        return;
	}

	m3gSetAngleAxis(&rotate, angle, ax, ay, az);
	m3gMulQuat(&rotate, &obj->orientation);	
	obj->orientation = rotate;
    
    m3gInvalidateTransformable(obj);
}

/*!
 * \brief Get orientation as angle axis.
 *
 * \param handle    Transformable object
 * \param angleAxis array to fill in angle, ax, ay, az
 */
M3G_API void m3gGetOrientation(M3GTransformable handle, M3Gfloat *angleAxis)
{
    const Transformable *obj = (Transformable *) handle;
    M3G_VALIDATE_OBJECT(obj);
    M3G_ASSERT_PTR(angleAxis);

	m3gGetAngleAxis(&obj->orientation,
                    angleAxis, (Vec3*)(angleAxis + 1));
}

/*!
 * \brief Set scale.
 *
 * \param handle    Transformable object
 * \param sx        x scale
 * \param sy        y scale
 * \param sz        z scale
 */
M3G_API void m3gSetScale(M3GTransformable handle,
                         M3Gfloat sx, M3Gfloat sy, M3Gfloat sz)
{
    Transformable *obj = (Transformable *) handle;
    M3G_VALIDATE_OBJECT(obj);

    obj->sx = sx;
    obj->sy = sy;
    obj->sz = sz;
    
    m3gInvalidateTransformable(obj);
}

/*!
 * \brief Scale.
 *
 * \param handle    Transformable object
 * \param sx        x scale
 * \param sy        y scale
 * \param sz        z scale
 */
M3G_API void m3gScale(M3GTransformable handle,
                      M3Gfloat sx, M3Gfloat sy, M3Gfloat sz)
{
    Transformable *obj = (Transformable *) handle;
    M3G_VALIDATE_OBJECT(obj);

    obj->sx = m3gMul(obj->sx, sx);
    obj->sy = m3gMul(obj->sy, sy);
    obj->sz = m3gMul(obj->sz, sz);
    
    m3gInvalidateTransformable(obj);
}

/*!
 * \brief Get scale.
 *
 * \param handle    Transformable object
 * \param scale     array to fill in sx, sy, sz
 */
M3G_API void m3gGetScale(M3GTransformable handle, M3Gfloat *scale)
{
    const Transformable *obj = (Transformable *) handle;
    M3G_VALIDATE_OBJECT(obj);
    M3G_ASSERT_PTR(scale);

	scale[0] = obj->sx;
	scale[1] = obj->sy;
	scale[2] = obj->sz;
}

/*!
 * \brief Set translation.
 *
 * \param handle    Transformable object
 * \param tx        x translation
 * \param ty        y translation
 * \param tz        z translation
 */
M3G_API void m3gSetTranslation(M3GTransformable handle,
                               M3Gfloat tx, M3Gfloat ty, M3Gfloat tz)
{
    Transformable *obj = (Transformable *) handle;
    M3G_VALIDATE_OBJECT(obj);
    
    obj->tx = tx;
    obj->ty = ty;
    obj->tz = tz;
    
    m3gInvalidateTransformable(obj);
}

/*!
 * \brief Translate.
 *
 * \param handle    Transformable object
 * \param tx        x translation
 * \param ty        y translation
 * \param tz        z translation
 */
M3G_API void m3gTranslate(M3GTransformable handle,
                          M3Gfloat tx, M3Gfloat ty, M3Gfloat tz)
{
    Transformable *obj = (Transformable *) handle;
    M3G_VALIDATE_OBJECT(obj);

    obj->tx = m3gAdd(obj->tx, tx);
    obj->ty = m3gAdd(obj->ty, ty);
    obj->tz = m3gAdd(obj->tz, tz);
    
    m3gInvalidateTransformable(obj);
}

/*!
 * \brief Get translation.
 *
 * \param handle      Transformable object
 * \param translation array to fill in tx, ty, tz
 */
M3G_API void m3gGetTranslation(M3GTransformable handle, M3Gfloat *translation)
{
    const Transformable *obj = (Transformable *) handle;
    M3G_VALIDATE_OBJECT(obj);
    M3G_ASSERT_PTR(translation);

	translation[0] = obj->tx;
	translation[1] = obj->ty;
	translation[2] = obj->tz;
}

/*!
 * \brief Set a static transformation matrix.
 *
 * \param handle        Transformable object
 * \param transform     transformation matrix
 */
M3G_API void m3gSetTransform(M3GTransformable handle,
                             const M3GMatrix *transform)
{
    Transformable *obj = (Transformable *) handle;
    M3G_VALIDATE_OBJECT(obj);

    /* Although this function behaves differently for nodes and
     * textures, the difference is small enough to warrant a run-time
     * class check below instead of an entry in the VF table(s) */

	if (transform != NULL) {
	    if (m3gGetClass((Object*)obj) != M3G_CLASS_TEXTURE
            && !m3gIsWUnity(transform)) {
            m3gRaiseError(M3G_INTERFACE(obj), M3G_INVALID_VALUE);
            return;
        }
        if (obj->matrix == NULL) {
            obj->matrix = m3gAlloc(M3G_INTERFACE(obj), sizeof(Matrix));
            if (obj->matrix == NULL) {
                return; /* automatic out-of-memory error */
            }
        }
		m3gCopyMatrix(obj->matrix, transform);
	}
	else if (obj->matrix != NULL) {
        m3gIdentityMatrix(obj->matrix);
	}
    
    m3gInvalidateTransformable(obj);
}

/*!
 * \brief Get transform.
 *
 * \param handle        Transformable object
 * \param transform     transformation matrix
 */
M3G_API void m3gGetTransform(M3GTransformable handle, M3GMatrix *transform)
{
    const Transformable *obj = (Transformable *) handle;
    M3G_VALIDATE_OBJECT(obj);

    if (obj->matrix != NULL) {
        m3gCopyMatrix(transform, obj->matrix);
    }
    else {
        m3gIdentityMatrix(transform);
    }
}

/*!
 * \brief Get composite transformation
 *
 * \param handle        Transformable object
 * \param transform     transformation matrix
 */
M3G_API void m3gGetCompositeTransform(M3GTransformable handle,
                                      M3GMatrix *transform)
{
    const Transformable *obj = (Transformable *) handle;
    TCache *tc;
    M3G_VALIDATE_OBJECT(obj);
    
    tc = m3gGetTransformCache(M3G_INTERFACE(obj));
        
    /* Check for a cached copy, and recompute if not available */
    
    if (!m3gGetCachedComposite(tc, obj, transform)) {
        
        /* Apply component based transformation */
        m3gIdentityMatrix(transform);
        m3gTranslateMatrix(transform, obj->tx, obj->ty, obj->tz);
        m3gRotateMatrixQuat(transform, &obj->orientation);
        m3gScaleMatrix(transform, obj->sx, obj->sy, obj->sz);

        /* Apply generic matrix */
        if (obj->matrix != NULL) {
            m3gMulMatrix(transform, obj->matrix);
        }

        /* Cache for reuse */
        m3gCacheComposite(tc, obj, transform);
    }
    
#   if defined(M3G_DEBUG)
    if (m3gGetClass((M3GObject) handle) != M3G_CLASS_TEXTURE) {
        M3G_ASSERT(m3gIsWUnity(transform));
    }
#   endif /* M3G_DEBUG */
}

