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
* Description: Light manager implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Light manager implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_lightmanager.h"
#include "m3g_light.h"
#include "m3g_rendercontext.h"
#include "m3g_math.h"

/*!
 * \internal
 * \brief Light array element
 */
typedef struct
{
    /*! \internal \brief eye space spot direction */
    Vec4 spotDir;

    /*! \internal \brief eye space position */
    Vec4 position;
    
    /*! \internal \brief reference to the Light node */
    Light *light;
} LightRecord;

/*----------------------------------------------------------------------
 * Private functions
 *--------------------------------------------------------------------*/
 
/*!
 * \internal
 * \brief Update a single light record
 *
 * Light position and spot direction are transformed into world space,
 * i.e. omitting the viewing transformation which is applied by OpenGL
 * when rendering. Both transformations are special cases and
 * accomplished by just reading a part of the matrix.
 *
 * \note We need to transform the spotlight direction even if the
 * light is not a spotlight, since in immediate mode, it may change
 * into a spotlight later on!
 */
static void m3gSetLightRecord(LightRecord *lrec,
                              Light *light,
                              const Matrix *tf)
{
    Vec4 v;
    M3G_ASSIGN_REF(lrec->light, light);

    if (tf != NULL) {
        m3gGetMatrixColumn(tf, 3, &lrec->position);
        m3gGetMatrixColumn(tf, 2, &v);
        lrec->spotDir.x = -v.x;
        lrec->spotDir.y = -v.y;
        lrec->spotDir.z = -v.z;
        lrec->spotDir.w = 0.0f;
    }
    else {
        lrec->spotDir.x = lrec->spotDir.y = lrec->spotDir.w = 0.0f;
        lrec->spotDir.z = -1.0f;
        lrec->position.x = lrec->position.y = lrec->position.z = 0.0f;
        lrec->position.w = 1.0f;
    }
}

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Clears all lights in the current pool of lights
 */
static void m3gClearLights2(LightManager *mgr)
{
    LightRecord *lrec;
    PointerArray *lights;
    int i, n;
    M3G_ASSERT_PTR(mgr);

    lights = &mgr->lights;
    n = m3gArraySize(lights);
    for (i = 0; i < n; ++i) {
        lrec = m3gGetArrayElement(lights, i);
        M3G_ASSIGN_REF(lrec->light, NULL);
    }

    mgr->numActive = 0;
}

/*!
 * \internal
 * \brief Destroys the light manager, freeing allocated resources
 */
static void m3gDestroyLightManager(LightManager *mgr, Interface *m3g)
{
    int i, n;
    M3G_ASSERT_PTR(mgr);
    M3G_VALIDATE_INTERFACE(m3g);

    /* First remove all light references */
    m3gClearLights2(mgr);

    /* Free the records currently in the light array */
    n = m3gArraySize(&mgr->lights);
    for (i = 0; i < n; ++i) {
        m3gFree(m3g, m3gGetArrayElement(&mgr->lights, i));
    }
    m3gDestroyArray(&mgr->lights, m3g);
}

/*!
 * \internal
 * \brief Appends a light at the end of the light manager array
 */
static M3Gint m3gInsertLight(LightManager *mgr,
                             Light *light,
                             const Matrix *tf,
                             Interface *m3g)
{
    LightRecord *lrec;
    PointerArray *lights;
    M3Gint idx;
    M3G_ASSERT_PTR(mgr);
    M3G_VALIDATE_INTERFACE(m3g);

    lights = &mgr->lights;
    
    /* Get the first unused light record, or add a new one */
    
    if (mgr->numActive < m3gArraySize(lights)) {
        lrec = m3gGetArrayElement(lights, mgr->numActive);
    }
    else {
        M3G_ASSERT(mgr->numActive == m3gArraySize(lights));
        lrec = m3gAllocZ(m3g, sizeof(LightRecord));
        if (lrec == NULL) {
            return -1;
        }
        if (m3gArrayAppend(lights, lrec, m3g) == -1) {
            return -1;
        }
    }
    idx = mgr->numActive++;

    m3gSetLightRecord(lrec, light, tf);
    return idx;
}

/*!
 * \internal
 * \brief
 */
static M3Gsizei m3gLightArraySize(const LightManager *mgr)
{
    M3G_ASSERT_PTR(mgr);
    return mgr->numActive;
}

/*!
 * \internal
 * \brief
 */
static Light *m3gGetLightTransformInternal(const LightManager *mgr, M3Gint idx, M3GMatrix *transform)
{
    M3Gfloat matrix[16];
    LightRecord *lrec;
    M3G_ASSERT_PTR(mgr);
    M3G_ASSERT(m3gInRange(idx, 0, mgr->numActive - 1));

    lrec = m3gGetArrayElement(&mgr->lights, idx);

    if (transform != NULL) {
        m3gZero(matrix, sizeof(matrix));
    
        matrix[0 * 4 + 0] = 1.f;
        matrix[1 * 4 + 1] = 1.f;
    
        matrix[2 * 4 + 0] = -lrec->spotDir.x;
        matrix[2 * 4 + 1] = -lrec->spotDir.y;
        matrix[2 * 4 + 2] = -lrec->spotDir.z;
    
        matrix[3 * 4 + 0] = lrec->position.x;
        matrix[3 * 4 + 1] = lrec->position.y;
        matrix[3 * 4 + 2] = lrec->position.z;
        matrix[3 * 4 + 3] = lrec->position.w;
    
        m3gSetMatrixColumns(transform, matrix);
    }

    return lrec->light;
}

/*!
 * \internal
 * \brief Replaces an existing light in the light array
 */
static void m3gReplaceLight(LightManager *mgr,
                            M3Gint idx,
                            Light *light,
                            const Matrix *tf)
{
    LightRecord *lrec;
    M3G_ASSERT_PTR(mgr);
    M3G_ASSERT(m3gInRange(idx, 0, mgr->numActive - 1));

    lrec = m3gGetArrayElement(&mgr->lights, idx);
    m3gSetLightRecord(lrec, light, tf);
}

/*!
 * \internal
 * \brief Selects a set of lights from the current light array for OpenGL
 *
 * Selects a maximum on \c maxNum lights from the array matching the
 * given scope, sets those into the current OpenGL context, and
 * disables the rest of the OpenGL lights GL_LIGHT0..GL_LIGHT7. A
 * maximum of 8 lights is ever used.
 *
 */
static void m3gSelectGLLights(const LightManager *mgr,
                              M3Gsizei maxNum,
                              M3Guint scope,
                              M3Gfloat x, M3Gfloat y, M3Gfloat z)
{
    const PointerArray *lights;
    int i, required, total;
    GLenum glIndex = GL_LIGHT0;
    M3G_ASSERT_PTR(mgr);

    M3G_UNREF(x);
    M3G_UNREF(y);
    M3G_UNREF(z);

    lights = &mgr->lights;
    required = m3gClampInt(maxNum, 0, 8);
    total = mgr->numActive;

    /* Select the first n lights that match the scope */
    
    for (i = 0; required > 0 && i < total; ++i) {
        const LightRecord *lrec;
        const Light *light;
        
        lrec = (const LightRecord *) m3gGetArrayElement(lights, i);
        M3G_ASSERT(lrec != NULL);
        light = lrec->light;
        
        if (light != NULL && (light->node.scope & scope) != 0) {
            m3gApplyLight(light, glIndex++, &lrec->position, &lrec->spotDir);
            --required;
        }
    }

    /* Disable the leftover lights */
    
    while (glIndex <= GL_LIGHT7) {
        glDisable(glIndex++);
    }
}

/*!
 * \internal
 * \brief Transforms all lights with the given matrix
 */
static void m3gTransformLights(LightManager *mgr, const Matrix *mtx)
{
    const PointerArray *lights;
    M3Gint i, n;

    lights = &mgr->lights;
    n = mgr->numActive;
    
    for (i = 0; i < n; ++i) {
        LightRecord *lrec = (LightRecord*) m3gGetArrayElement(lights, i);
        m3gTransformVec4(mtx, &lrec->position);
        m3gTransformVec4(mtx, &lrec->spotDir);
    }
}

