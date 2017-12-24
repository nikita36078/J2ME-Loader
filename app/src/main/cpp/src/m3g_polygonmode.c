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
* Description: PolygonMode implementation
*
*/


/*!
 * \internal
 * \file
 * \brief PolygonMode implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

/*
 * Uncomment this line to switch tracing on for this file's functions
 */
/* #define M3G_LOCAL_TRACEF_ON */

#include "m3g_polygonmode.h"

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Applies default polygon mode to OpenGL.
 */
static void m3gApplyDefaultPolygonMode()
{
    /* cull = BACK */
    glCullFace(GL_BACK);
    glEnable(GL_CULL_FACE);

    /* shading = M3G_SHADE_SMOOTH */
    glShadeModel(GL_SMOOTH);

    /*  winding = M3G_WINDING_CCW */
    glFrontFace(GL_CCW);

    /* perspective correction = false */
    glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_FASTEST);

    /* lighting = ONE_SIDED */
    glLightModelf(GL_LIGHT_MODEL_TWO_SIDE, GL_FALSE);
}

/*!
 * \internal
 * \brief Applies polygon mode to OpenGL.
 *
 * \param polygonMode   PolygonMode object
 */
static void m3gApplyPolygonMode(PolygonMode *polygonMode)
{
    if (polygonMode != NULL) {
        if (polygonMode->cullingMode == M3G_CULL_NONE) {
            glDisable(GL_CULL_FACE);
        }
        else {
            if (polygonMode->cullingMode == M3G_CULL_BACK) {
                glCullFace(GL_BACK);
            }
            else {
                glCullFace(GL_FRONT);
            }
            glEnable(GL_CULL_FACE);
        }

        if (m3gGetTwoSidedLightingWorkaround(M3G_INTERFACE(polygonMode))) {
            glLightModelf(GL_LIGHT_MODEL_TWO_SIDE, GL_FALSE);
        }
        else {
            glLightModelf(GL_LIGHT_MODEL_TWO_SIDE, polygonMode->enableTwoSidedLighting);
        }

        if (polygonMode->shadingMode == M3G_SHADE_FLAT) {
            glShadeModel(GL_FLAT);
        }
        else {
            glShadeModel(GL_SMOOTH);
        }

        if (polygonMode->windingMode == M3G_WINDING_CW) {
            glFrontFace(GL_CW);
        }
        else {
            glFrontFace(GL_CCW);
        }

        if (polygonMode->enablePerspectiveCorrection == GL_TRUE) {
            glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
        }
        else {
            glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_FASTEST);
        }
    }
    else {
        m3gApplyDefaultPolygonMode();
    }
    M3G_ASSERT_GL;

}

/*!
 * \internal
 * \brief Overloaded Object3D method
 *
 * \param originalObj original PolygonMode object
 * \param cloneObj pointer to cloned PolygonMode object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gPolygonModeDuplicate(const Object *originalObj,
                                       Object **cloneObj,
                                       Object **pairs,
                                       M3Gint *numPairs)
{
    PolygonMode *original = (PolygonMode *)originalObj;
    PolygonMode *clone;
    M3G_ASSERT(*cloneObj == NULL); /* no derived classes */

    /* Create the clone object */
    
    clone = (PolygonMode *)m3gCreatePolygonMode(originalObj->interface);
    if (!clone) {
        return M3G_FALSE;
    }
    *cloneObj = (Object *)clone;

    /* Duplicate base class data */
    
    if (!m3gObjectDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        return M3G_FALSE;
    }

    /* Duplicate our own data */
    
    clone->enableLocalCameraLighting = original->enableLocalCameraLighting;
    clone->enablePerspectiveCorrection = original->enablePerspectiveCorrection;
    clone->cullingMode = original->cullingMode;
    clone->windingMode = original->windingMode;
    clone->shadingMode = original->shadingMode;
    clone->enableTwoSidedLighting = original->enableTwoSidedLighting;

    return M3G_TRUE;
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const ObjectVFTable m3gvf_PolygonMode = {
    m3gObjectApplyAnimation,
    m3gObjectIsCompatible,
    m3gObjectUpdateProperty,
    m3gObjectDoGetReferences,
    m3gObjectFindID,
    m3gPolygonModeDuplicate,
    m3gDestroyObject
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a new PolygonMode object.
 *
 * \param interface     M3G interface object
 * \retval PolygonMode new PolygonMode object
 * \retval NULL PolygonMode creating failed
 */
M3G_API M3GPolygonMode m3gCreatePolygonMode(M3GInterface interface)
{
    Interface *m3g = (Interface *) interface;
    M3G_VALIDATE_INTERFACE(m3g);

    {
        PolygonMode *polygonMode = m3gAllocZ(m3g, sizeof(PolygonMode));

        if (polygonMode != NULL) {
            m3gInitObject(&polygonMode->object, m3g, M3G_CLASS_POLYGON_MODE);
            polygonMode->enableLocalCameraLighting = GL_FALSE;
            polygonMode->enablePerspectiveCorrection = GL_FALSE;
            polygonMode->cullingMode = M3G_CULL_BACK;
            polygonMode->windingMode = M3G_WINDING_CCW;
            polygonMode->shadingMode = M3G_SHADE_SMOOTH;
            polygonMode->enableTwoSidedLighting = GL_FALSE;
        }

        return (M3GPolygonMode)polygonMode;
    }
}

/*!
 * \brief Set local camera lightning.
 *
 * \param handle        PolygonMode object
 * \param enable        enable flag
 */
M3G_API void m3gSetLocalCameraLightingEnable(M3GPolygonMode handle,
                                             M3Gbool enable)
{
    PolygonMode *polygonMode = (PolygonMode*)handle;
    M3G_VALIDATE_OBJECT(polygonMode);
    polygonMode->enableLocalCameraLighting = (GLboolean) enable;
}

/*!
 * \brief Set perspective correction.
 *
 * \param handle        PolygonMode object
 * \param enable        enable flag
 */
void m3gSetPerspectiveCorrectionEnable(M3GPolygonMode handle, M3Gbool enable)
{
    PolygonMode *polygonMode = (PolygonMode*)handle;
    M3G_VALIDATE_OBJECT(polygonMode);
    polygonMode->enablePerspectiveCorrection = (GLboolean) enable;
}

/*!
 * \brief Set culling mode.
 *
 * \param handle        PolygonMode object
 * \param mode          culling mode
 */
void m3gSetCulling(M3GPolygonMode handle, M3Gint mode)
{
    PolygonMode *polygonMode = (PolygonMode*)handle;
    M3G_VALIDATE_OBJECT(polygonMode);

    switch (mode) {
    case M3G_CULL_BACK:
    case M3G_CULL_FRONT:
    case M3G_CULL_NONE:
        polygonMode->cullingMode = mode;
        break;
    default:
        m3gRaiseError(M3G_INTERFACE(polygonMode), M3G_INVALID_VALUE);
        break;
    }
}

/*!
 * \brief Get culling mode.
 *
 * \param handle        PolygonMode object
 * \return              culling mode
 */
M3G_API M3Gint m3gGetCulling(M3GPolygonMode handle)
{
    PolygonMode *polygonMode = (PolygonMode*)handle;
    M3G_VALIDATE_OBJECT(polygonMode);
    return polygonMode->cullingMode;
}

/*!
 * \brief Set winding mode.
 *
 * \param handle        PolygonMode object
 * \param mode          winding mode
 */
M3G_API void m3gSetWinding(M3GPolygonMode handle, M3Gint mode)
{
    PolygonMode *polygonMode = (PolygonMode*)handle;
    M3G_VALIDATE_OBJECT(polygonMode);

    if (mode != M3G_WINDING_CCW && mode != M3G_WINDING_CW) {
        m3gRaiseError(M3G_INTERFACE(polygonMode), M3G_INVALID_VALUE);
    }
    else {
        polygonMode->windingMode = mode;
    }
}

/*!
 * \brief Get winding mode.
 *
 * \param handle        PolygonMode object
 * \return              winding mode
 */
M3G_API M3Gint m3gGetWinding(M3GPolygonMode handle)
{
    PolygonMode *polygonMode = (PolygonMode*)handle;
    M3G_VALIDATE_OBJECT(polygonMode);
    return polygonMode->windingMode;
}

/*!
 * \brief Set shading mode.
 *
 * \param handle        PolygonMode object
 * \param mode          shading mode
 */
M3G_API void m3gSetShading(M3GPolygonMode handle, M3Gint mode)
{
    PolygonMode *polygonMode = (PolygonMode*)handle;
    M3G_VALIDATE_OBJECT(polygonMode);

    if (mode != M3G_SHADE_FLAT && mode != M3G_SHADE_SMOOTH) {
        m3gRaiseError(M3G_INTERFACE(polygonMode), M3G_INVALID_VALUE);
    }
    else {
        polygonMode->shadingMode = mode;
    }
}

/*!
 * \brief Get shading mode.
 *
 * \param handle        PolygonMode object
 * \return              shading mode
 */
M3G_API M3Gint m3gGetShading(M3GPolygonMode handle)
{
    PolygonMode *polygonMode = (PolygonMode*)handle;
    M3G_VALIDATE_OBJECT(polygonMode);
    return polygonMode->shadingMode;
}

/*!
 * \brief Set two sided lighting.
 *
 * \param handle        PolygonMode object
 * \param enable        enable flag
 */
M3G_API void m3gSetTwoSidedLightingEnable(M3GPolygonMode handle,
                                          M3Gbool enable)
{
    PolygonMode *polygonMode = (PolygonMode*)handle;
    M3G_VALIDATE_OBJECT(polygonMode);
    polygonMode->enableTwoSidedLighting = (GLboolean) enable;
}

/*!
 * \brief Get two sided lighting.
 *
 * \param handle        PolygonMode object
 * \retval M3G_TRUE     enabled
 * \retval M3G_FALSE    disabled
 */
M3G_API M3Gbool m3gIsTwoSidedLightingEnabled(M3GPolygonMode handle)
{
    PolygonMode *polygonMode = (PolygonMode*)handle;
    M3G_VALIDATE_OBJECT(polygonMode);
    return (M3Gbool) polygonMode->enableTwoSidedLighting;
}

/*!
 * \brief Get local camera lighting.
 *
 * \param handle        PolygonMode object
 * \retval M3G_TRUE     enabled
 * \retval M3G_FALSE    disabled
 */

M3G_API M3Gbool m3gIsLocalCameraLightingEnabled(M3GPolygonMode handle)
{
    PolygonMode *polygonMode = (PolygonMode*)handle;
    M3G_VALIDATE_OBJECT(polygonMode);
    return (M3Gbool) polygonMode->enableLocalCameraLighting;
}

/*!
 * \brief Get perspective correction.
 *
 * \param handle        PolygonMode object
 * \retval M3G_TRUE     enabled
 * \retval M3G_FALSE    disabled
 */
M3G_API M3Gbool m3gIsPerspectiveCorrectionEnabled(M3GPolygonMode handle)
{
    PolygonMode *polygonMode = (PolygonMode*)handle;
    M3G_VALIDATE_OBJECT(polygonMode);
    return (M3Gbool) polygonMode->enablePerspectiveCorrection;
}

/*
 * Uncomment these lines' opening pair at the begining of the file
 * if you want to switch tracing on for this file.
 */
#ifdef M3G_LOCAL_TRACEF_ON
#undef M3G_LOCAL_TRACEF_ON
#endif

