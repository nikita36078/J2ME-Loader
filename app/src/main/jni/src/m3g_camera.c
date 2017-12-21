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
* Description: Camera implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Camera implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_camera.h"

/* Internal frustum plane enumeration (and testing order!) */

#define NEAR_PLANE      0
#define FAR_PLANE       1
#define LEFT_PLANE      2
#define RIGHT_PLANE     3
#define BOTTOM_PLANE    4
#define TOP_PLANE       5

/*----------------------------------------------------------------------
 * Private functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Makes sure that the internal projection matrix is up-to-date
 *        and checks if the camera has a zero view volume.
 */
static void m3gValidateProjectionMatrix(Camera *camera)
{
    M3Gint projType = camera->projType;

    /* The generic matrix is always valid, but for perspective and
     * parallel we must regenerate the matrix */
    
    if (projType != M3G_GENERIC) {
        M3Gfloat m[16];

        M3Gfloat clipNear = camera->clipNear;
        M3Gfloat clipFar = camera->clipFar;

        if (projType == M3G_PERSPECTIVE) {
            
            M3Gfloat height = m3gTan(m3gMul(M3G_DEG2RAD * 0.5f,
                                            camera->heightFov));
            
            m[0] = m3gRcp(m3gMul(camera->aspect, height));
            m[1] = m[2] = m[3] = 0.f;
            
            m[4] = 0.f;
            m[5] = m3gRcp(height);
            m[6] = m[7] = 0.f;
            
            m[8] = m[9] = 0.f;
            m[10] = m3gDiv(-m3gAdd(clipFar, clipNear),
                           m3gSub(clipFar, clipNear));
            m[11] = -1.f;
            
            m[12] = m[13] = 0.f;
            m[14] = m3gDiv(m3gMul(m3gMul(-2.f, clipFar), clipNear),
                           m3gSub(clipFar, clipNear));
            m[15] = 0.f;
        }
        else if (projType == M3G_PARALLEL) {

            M3Gfloat height = camera->heightFov;
            
            m[0] = m3gDiv(2.f, m3gMul(camera->aspect, height));
            m[1] = m[2] = m[3] = 0.f;

            m[4] = 0.f;
            m[5] = m3gDiv(2.f, height);
            m[6] = m[7] = 0;
            
            m[8] = m[9] = 0;
            m[10] = m3gDiv(-2.f, m3gSub(clipFar, clipNear));
            m[11] = 0.f;
            
            m[12] = m[13] = 0.f;
            m[14] = m3gDiv(-m3gAdd(clipFar, clipNear),
                           m3gSub(clipFar, clipNear));
            m[15] = 1.f;
        }
        else {
            M3G_ASSERT(M3G_FALSE); /* unknown projection type! */
        }
        m3gSetMatrixColumns(&camera->projMatrix, m); 
    }

    {
        M3GMatrix im;
        if (!m3gMatrixInverse(&im, &camera->projMatrix)) {
            camera->zeroViewVolume = M3G_TRUE;
        }
        else {
            camera->zeroViewVolume = M3G_FALSE;
        }
    }

    camera->frustumPlanesValid = M3G_FALSE;
}

/*!
 * \internal
 * \brief Validates the cached view frustum planes
 */
static void m3gValidateFrustumPlanes(Camera *camera) 
{
    if (!camera->frustumPlanesValid) {
        Vec4 *plane;
        Vec4 rows[4];
        
        m3gGetMatrixRows(&camera->projMatrix, (M3Gfloat*) rows);

        plane = &camera->frustumPlanes[LEFT_PLANE];
        *plane = rows[3];
        m3gAddVec4(plane, &rows[0]);

        plane = &camera->frustumPlanes[RIGHT_PLANE];
        *plane = rows[3];
        m3gSubVec4(plane, &rows[0]);

        plane = &camera->frustumPlanes[BOTTOM_PLANE];
        *plane = rows[3];
        m3gAddVec4(plane, &rows[1]);

        plane = &camera->frustumPlanes[TOP_PLANE];
        *plane = rows[3];
        m3gSubVec4(plane, &rows[1]);

        plane = &camera->frustumPlanes[NEAR_PLANE];
        *plane = rows[3];
        m3gAddVec4(plane, &rows[2]);

        plane = &camera->frustumPlanes[FAR_PLANE];
        *plane = rows[3];
        m3gSubVec4(plane, &rows[2]);

        camera->frustumPlanesValid = M3G_TRUE;
    }
}

#undef NEAR_PLANE
#undef FAR_PLANE
#undef LEFT_PLANE
#undef RIGHT_PLANE
#undef BOTTOM_PLANE
#undef TOP_PLANE

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param property      animation property
 * \retval M3G_TRUE     property supported
 * \retval M3G_FALSE    property not supported
 */
static M3Gbool m3gCameraIsCompatible(M3Gint property)
{
    switch (property) {
    case M3G_ANIM_FAR_DISTANCE:
    case M3G_ANIM_FIELD_OF_VIEW:
    case M3G_ANIM_NEAR_DISTANCE:
        return M3G_TRUE;
    default:
        return m3gNodeIsCompatible(property);
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param obj          Camera object
 * \param property      animation property
 * \param valueSize     size of value array
 * \param value         value array
 */
static void m3gCameraUpdateProperty(Object *obj,
                                    M3Gint property,
                                    M3Gint valueSize,
                                    const M3Gfloat *value)
{
    Camera *camera = (Camera *) obj;
    M3G_VALIDATE_OBJECT(camera);
    M3G_ASSERT_PTR(value);

    switch (property) {
    case M3G_ANIM_FAR_DISTANCE:
        M3G_ASSERT(valueSize >= 1);
        camera->clipFar =   (camera->projType == M3G_PERSPECTIVE)
                            ? m3gClampFloatPositive(value[0])
                            : value[0];
        break;
    case M3G_ANIM_FIELD_OF_VIEW:
        M3G_ASSERT(valueSize >= 1);
        camera->heightFov = (camera->projType == M3G_PERSPECTIVE)
                            ? m3gClampFloat(value[0], 0.f, 180.f)
                            : m3gClampFloatPositive(value[0]);
        break;
    case M3G_ANIM_NEAR_DISTANCE:
        M3G_ASSERT(valueSize >= 1);
        camera->clipNear =  (camera->projType == M3G_PERSPECTIVE)
                            ? m3gClampFloatPositive(value[0])
                            : value[0];
        break;
    default:
        m3gNodeUpdateProperty(obj, property, valueSize, value);
        return; /* don't invalidate the matrix */
    }

    /* Validate the projection matrix if we changed any of the
     * camera parameters */

    m3gValidateProjectionMatrix(camera);
}

/*!
 * \internal
 * \brief Overloaded Node method.
 *
 * Start render setup scene traversal.
 *
 * \param node Camera object
 * \param toCamera transform to camera
 * \param alphaFactor total alpha factor
 * \param caller caller node
 * \param renderQueue RenderQueue
 *
 * \retval M3G_TRUE continue render setup
 * \retval M3G_FALSE abort render setup
 */
static M3Gbool m3gCameraSetupRender(Node *self,
                                    const Node *caller,
                                    SetupRenderState *s,
                                    RenderQueue *renderQueue)
{
    Node *parent;
    M3Gbool success = M3G_TRUE;

    /* Just do the parent node.  Note that we won't be needing the old
     * state back after going up the tree, so we can overwrite it. */
    
    parent = self->parent;

    if (caller != parent && parent != NULL) {
        Matrix t;
        
        M3G_BEGIN_PROFILE(M3G_INTERFACE(self), M3G_PROFILE_SETUP_TRANSFORMS);
        if (!m3gGetInverseNodeTransform(self, &t)) {
            return M3G_FALSE;
        }
        m3gMulMatrix(&s->toCamera, &t);
        M3G_END_PROFILE(M3G_INTERFACE(self), M3G_PROFILE_SETUP_TRANSFORMS);

        /* The parent node will update the alpha factor and culling
         * mask if necessary, so we need not touch those */
        
        success = M3G_VFUNC(Node, parent, setupRender)(parent,
                                                       self, s, renderQueue);
    }

    return success;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original Camera object
 * \param cloneObj pointer to cloned Camera object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gCameraDuplicate(const Object *originalObj,
                                  Object **cloneObj,
                                  Object **pairs,
                                  M3Gint *numPairs)
{
    Camera *original = (Camera *)originalObj;
    Camera *clone = (Camera *)m3gCreateCamera(originalObj->interface);
    *cloneObj = (Object *)clone;
    if (*cloneObj == NULL) {
        return M3G_FALSE;
    }

    if (m3gNodeDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        clone->projType = original->projType;
        clone->projMatrix = original->projMatrix;
        clone->heightFov = original->heightFov;
        clone->aspect = original->aspect;
        clone->clipNear = original->clipNear;
        clone->clipFar = original->clipFar;
        clone->zeroViewVolume = original->zeroViewVolume;
        return M3G_TRUE;
    }
    else {
        return M3G_FALSE;
    }
}

/*!
 * \internal
 * \brief Initializes a Camera object. See specification
 * for default values.
 *
 * \param m3g           M3G interface
 * \param camera        Camera object
 */
static void m3gInitCamera(Interface *m3g, Camera *camera)
{
    M3GMatrix m;

    /* Camera is derived from node */
    m3gInitNode(m3g, &camera->node, M3G_CLASS_CAMERA);

    /* GENERIC, Identity */
    m3gIdentityMatrix(&m);
    m3gSetProjectionMatrix(camera, (const M3GMatrix *)&m);
}

/*!
 * \internal
 * \brief Sets camera matrix to OpenGL
 * projection matrix.
 *
 * \param camera Camera object
 */
static void m3gApplyProjection(const Camera *camera)
{
    M3Gfloat t[16];

    m3gGetMatrixColumns(&camera->projMatrix, t);
    
    glMatrixMode(GL_PROJECTION);
    glLoadMatrixf(t);
    glMatrixMode(GL_MODELVIEW);
}

/*!
 * \internal
 * \brief Returns a pointer to the camera projection matrix
 *
 * The matrix <em>must not</em> be accessed directly, as only this
 * function will ensure that the returned matrix has valid values in
 * it.
 * 
 * \param camera Camera object
 * \return a pointer to the projection matrix
 */
static const Matrix *m3gProjectionMatrix(const Camera *camera)
{
    M3G_VALIDATE_OBJECT(camera);

    return &camera->projMatrix;
}

/*!
 * \internal
 * \brief Retrieves a pointer to the six camera space view frustum planes
 */
static const Vec4 *m3gFrustumPlanes(const Camera *camera)
{
    M3G_VALIDATE_OBJECT(camera);
    m3gValidateFrustumPlanes((Camera*) camera);
    return camera->frustumPlanes;
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const NodeVFTable m3gvf_Camera = {
    {
        {
            m3gObjectApplyAnimation,
            m3gCameraIsCompatible,
            m3gCameraUpdateProperty,
            m3gObjectDoGetReferences,
            m3gObjectFindID,
            m3gCameraDuplicate,
            m3gDestroyNode /* no extra clean-up for Camera */
        }
    },
    m3gNodeAlign,
    NULL, /* pure virtual DoRender */
    m3gNodeGetBBox,
    m3gNodeRayIntersect,
    m3gCameraSetupRender,
    m3gNodeUpdateDuplicateReferences,
    m3gNodeValidate
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a Camera object.
 *
 * \param interface     M3G interface
 * \retval Camera new Camera object
 * \retval NULL Camera creating failed
 */
M3G_API M3GCamera m3gCreateCamera(M3GInterface interface)
{
    Interface *m3g = (Interface *) interface;
    M3G_VALIDATE_INTERFACE(m3g);

    {
        Camera *camera =  m3gAllocZ(m3g, sizeof(Camera));

        if (camera != NULL) {
            m3gInitCamera(m3g, camera);
        }

        return (M3GCamera) camera;
    }
}

/*!
 * \brief Sets a parallel projection.
 *
 * \param handle        Camera object
 * \param height        height (=fovy)
 * \param aspectRatio   viewport aspect ratio
 * \param clipNear      near clipping plane
 * \param clipFar       far clipping plane
 */
M3G_API void m3gSetParallel(M3GCamera handle,
                            M3Gfloat height,
                            M3Gfloat aspectRatio,
                            M3Gfloat clipNear, M3Gfloat clipFar)
{
    Camera *camera = (Camera *) handle;
    M3G_VALIDATE_OBJECT(camera);

    if (height <= 0 || aspectRatio <= 0.0f) {
        m3gRaiseError(M3G_INTERFACE(camera), M3G_INVALID_VALUE);
        return;
    }

    camera->projType   = M3G_PARALLEL;
    camera->heightFov  = height;
    camera->aspect     = aspectRatio;
    camera->clipNear   = clipNear;
    camera->clipFar    = clipFar;

    m3gValidateProjectionMatrix(camera);
}

/*!
 * \brief Sets a perspective projection.
 *
 * \param handle        Camera object
 * \param fovy          fovy
 * \param aspectRatio   viewport aspect ratio
 * \param clipNear      near clipping plane
 * \param clipFar       far clipping plane
 */
M3G_API void m3gSetPerspective(M3GCamera handle,
                               M3Gfloat fovy,
                               M3Gfloat aspectRatio,
                               M3Gfloat clipNear, M3Gfloat clipFar)
{
    Camera *camera = (Camera *) handle;
    M3G_VALIDATE_OBJECT(camera);

    if (fovy <= 0.0f || fovy >= 180.f
        || aspectRatio <= 0.0f
        || clipNear <= 0.0f || clipFar <= 0.0f) {
        m3gRaiseError(M3G_INTERFACE(camera), M3G_INVALID_VALUE);
        return;
    }

    camera->projType   = M3G_PERSPECTIVE;
    camera->heightFov  = fovy;
    camera->aspect     = aspectRatio;
    camera->clipNear   = clipNear;
    camera->clipFar    = clipFar;

    m3gValidateProjectionMatrix(camera);
}

/*!
 * \brief Sets a generic projection.
 *
 * \param handle        Camera object
 * \param transform     projection matrix
 */
M3G_API void m3gSetProjectionMatrix(M3GCamera handle,
                                    const M3GMatrix *transform)
{
    Camera *camera = (Camera *) handle;
    M3G_VALIDATE_OBJECT(camera);

    if (transform == NULL) {
        m3gRaiseError(M3G_INTERFACE(camera), M3G_NULL_POINTER);
        return;
    }
    
    camera->projType = M3G_GENERIC;    
    m3gCopyMatrix(&camera->projMatrix, transform);
    
    m3gValidateProjectionMatrix(camera);
}

/*!
 * \brief Gets camera matrix.
 *
 * \param handle        Camera object
 * \param transform     projection matrix to fill in
 * \return              projection type
 */
M3G_API M3Gint m3gGetProjectionAsMatrix(M3GCamera handle,
                                        M3GMatrix *transform)
{
    Camera *camera = (Camera *) handle;
    M3G_VALIDATE_OBJECT(camera);

    if (transform != NULL) {
        /* Check for impossible projection matrix */
        if (camera->projType != M3G_GENERIC && 
            camera->clipFar == camera->clipNear) {
            m3gRaiseError(M3G_INTERFACE(camera), M3G_ARITHMETIC_ERROR);
            return 0;
        }

        m3gCopyMatrix(transform, m3gProjectionMatrix(camera));
    }

    return camera->projType;
}

/*!
 * \brief Gets camera parameters.
 *
 * \param handle        Camera object
 * \param params        camera parameters to fill in
 * \return              projection type
 */
M3G_API M3Gint m3gGetProjectionAsParams(M3GCamera handle, M3Gfloat *params)
{
    Camera *camera = (Camera *) handle;
    M3G_VALIDATE_OBJECT(camera);

    if (params != NULL && camera->projType != M3G_GENERIC) {
        params[0] = camera->heightFov;
        params[1] = camera->aspect;
        params[2] = camera->clipNear;
        params[3] = camera->clipFar;
    }

    return camera->projType;
}

