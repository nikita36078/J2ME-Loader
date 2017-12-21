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
* Description: CompositingMode implementation
*
*/


/*!
 * \internal
 * \file
 * \brief CompositingMode implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_compositingmode.h"
#include "m3g_interface.h"

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Applies default CompositingMode to OpenGL.
 */
static void m3gApplyDefaultCompositingMode(M3Gbool alphaWrite)
{
    /* depth write = true */
    glDepthFunc(GL_LEQUAL);
    glDepthMask(GL_TRUE);

    /* colorWrite = true */
    glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, alphaWrite);

    /* threshold = 0.0 */
    glAlphaFunc(GL_GEQUAL, 0.0f);
    glDisable(GL_ALPHA_TEST);

    /* blend = REPLACE */
    glDisable(GL_BLEND);

    /* factor = 0 && units = 0 */
    glDisable(GL_POLYGON_OFFSET_FILL);
}

/*!
 * \internal
 * \brief Applies CompositingMode to OpenGL.
 *
 * \param compositingMode CompositingMode object
 */
static void m3gApplyCompositingMode(CompositingMode *compositingMode, RenderContext *ctx)
{
	M3Gbool alphaWrite = m3gGetAlphaWrite(ctx);

	if (compositingMode != NULL) {

        glDepthFunc(compositingMode->depthTest ? GL_LEQUAL : GL_ALWAYS);
        glDepthMask(compositingMode->depthWrite);

        if (m3gGetColorMaskWorkaround(M3G_INTERFACE(compositingMode))) {
            glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
        }
        else {
            glColorMask(compositingMode->colorWrite,
                        compositingMode->colorWrite,
                        compositingMode->colorWrite,
						alphaWrite & compositingMode->alphaWrite);
        }

        if (compositingMode->alphaThreshold == 0.0f) {
            glDisable(GL_ALPHA_TEST);
        }
        else {
            glAlphaFunc(GL_GEQUAL, compositingMode->alphaThreshold);
            glEnable(GL_ALPHA_TEST);
        }

        if (compositingMode->blendingMode == M3G_REPLACE) {
            glDisable(GL_BLEND);
        }
        else {
            M3Gint src = GL_ONE;
            M3Gint dst = GL_ZERO;
            switch (compositingMode->blendingMode) {
                case M3G_ALPHA_ADD:
                    src = GL_SRC_ALPHA;
                    dst = GL_ONE;
                    break;
                case M3G_ALPHA_BLEND:
                    src = GL_SRC_ALPHA;
                    dst = GL_ONE_MINUS_SRC_ALPHA;
                    break;
                case M3G_MODULATE:
                    src = GL_ZERO;
                    dst = GL_SRC_COLOR;
                    break;
                case M3G_MODULATE_X2:
                    src = GL_DST_COLOR;
                    dst = GL_SRC_COLOR;
                    break;
            }
            glBlendFunc(src, dst);
            glEnable(GL_BLEND);
        }

        glPolygonOffset(compositingMode->factor, compositingMode->units);
        if (compositingMode->factor != 0 || compositingMode->units != 0) {
            glEnable(GL_POLYGON_OFFSET_FILL);
        }
        else {
            glDisable(GL_POLYGON_OFFSET_FILL);
        }
    }
    else {
		m3gApplyDefaultCompositingMode(alphaWrite);
    }
    M3G_ASSERT_GL;
}

/*!
 * \internal
 * \brief Duplicates a CompositingMode object.
 *
 * \param originalObj original CompositingMode object
 * \param cloneObj pointer to cloned CompositingMode object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gCompositingModeDuplicate(const Object *originalObj,
                                           Object **cloneObj,
                                           Object **pairs,
                                           M3Gint *numPairs)
{
    CompositingMode *original = (CompositingMode *)originalObj;
    CompositingMode *clone =
        (CompositingMode *)m3gCreateCompositingMode(originalObj->interface);
    *cloneObj = (Object *)clone;
    if (*cloneObj == NULL) {
        return M3G_FALSE;
    }

    if(m3gObjectDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        clone->blendingMode = original->blendingMode;
        clone->alphaThreshold = original->alphaThreshold;
        clone->depthTest = original->depthTest;
        clone->depthWrite = original->depthWrite;
        clone->colorWrite = original->colorWrite;
        clone->alphaWrite = original->alphaWrite;
        clone->factor = original->factor;
        clone->units = original->units;
        return M3G_TRUE;
    }
    else {
        return M3G_FALSE;
    }
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const ObjectVFTable m3gvf_CompositingMode = {
    m3gObjectApplyAnimation,
    m3gObjectIsCompatible,
    m3gObjectUpdateProperty,
    m3gObjectDoGetReferences,
    m3gObjectFindID,
    m3gCompositingModeDuplicate,
    m3gDestroyObject
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a new CompositingMode object.
 *
 * \param interface     M3G interface
 * \retval CompositingMode new CompositingMode object
 * \retval NULL CompositingMode creating failed
 */
M3G_API M3GCompositingMode m3gCreateCompositingMode(M3GInterface interface)
{
    Interface *m3g = (Interface *) interface;
    M3G_VALIDATE_INTERFACE(m3g);

    {
        CompositingMode *compositingMode = m3gAllocZ(m3g, sizeof(CompositingMode));

        if (compositingMode != NULL) {
            m3gInitObject(&compositingMode->object, m3g,
                          M3G_CLASS_COMPOSITING_MODE);
    
            compositingMode->blendingMode = M3G_REPLACE;
            compositingMode->alphaThreshold = 0.0f;
            compositingMode->depthTest = GL_TRUE;
            compositingMode->depthTest = GL_TRUE;
            compositingMode->depthWrite = GL_TRUE;
            compositingMode->colorWrite = GL_TRUE;
            compositingMode->alphaWrite = GL_TRUE;
            compositingMode->factor = 0.0f;
            compositingMode->units = 0.0f;
        }

        return (M3GCompositingMode) compositingMode;
    }
}

/*!
 * \brief Set blending mode.
 *
 * \param handle CompositingMode object
 * \param mode blending mode
 */
M3G_API void m3gSetBlending(M3GCompositingMode handle, M3Gint mode)
{
    CompositingMode *compositingMode = (CompositingMode*)handle;
    if (mode != M3G_REPLACE && mode != M3G_ALPHA_BLEND && mode != M3G_MODULATE && mode != M3G_MODULATE_X2 && mode != M3G_ALPHA_ADD) {
        m3gRaiseError(M3G_INTERFACE(compositingMode), M3G_INVALID_VALUE);
        return;
    }
    M3G_VALIDATE_OBJECT(compositingMode);
    compositingMode->blendingMode = mode;
}

/*!
 * \brief Get blending mode.
 *
 * \param handle CompositingMode object
 * \return blending mode
 */
M3G_API M3Gint m3gGetBlending(M3GCompositingMode handle)
{
    CompositingMode *compositingMode = (CompositingMode*)handle;
    M3G_VALIDATE_OBJECT(compositingMode);
    return compositingMode->blendingMode;
}

/*!
 * \brief Set alpha threshold [0, 1].
 *
 * \param handle CompositingMode object
 * \param threshold alpha threshold [0, 1]
 */
M3G_API void m3gSetAlphaThreshold(M3GCompositingMode handle,
                                  M3Gfloat threshold)
{
    CompositingMode *compositingMode = (CompositingMode*)handle;
    M3G_VALIDATE_OBJECT(compositingMode);
    if (!m3gInRangef(threshold, 0.0f, 1.0f)) {
        m3gRaiseError(M3G_INTERFACE(compositingMode), M3G_INVALID_VALUE);
        return;
    }
    compositingMode->alphaThreshold = threshold;
}

/*!
 * \brief Get alpha threshold.
 *
 * \param handle CompositingMode object
 * \return alpha threshold
 */
M3G_API M3Gfloat m3gGetAlphaThreshold(M3GCompositingMode handle)
{
    CompositingMode *compositingMode = (CompositingMode*)handle;
    M3G_VALIDATE_OBJECT(compositingMode);
    return compositingMode->alphaThreshold;
}

/*!
 * \brief Set depth test enable.
 *
 * \param handle CompositingMode object
 * \param enable depth test enable
 */
M3G_API void m3gEnableDepthTest(M3GCompositingMode handle, M3Gbool enable)
{
    CompositingMode *compositingMode = (CompositingMode*)handle;
    M3G_VALIDATE_OBJECT(compositingMode);
    compositingMode->depthTest = (GLboolean) enable;
}

/*!
 * \brief Set depth write enable.
 *
 * \param handle CompositingMode object
 * \param enable depth write enable
 */
M3G_API void m3gEnableDepthWrite(M3GCompositingMode handle, M3Gbool enable)
{
    CompositingMode *compositingMode = (CompositingMode*)handle;
    M3G_VALIDATE_OBJECT(compositingMode);
    compositingMode->depthWrite = (GLboolean) enable;
}

/*!
 * \brief Set color write enable.
 *
 * \param handle CompositingMode object
 * \param enable color write enable
 */
M3G_API void m3gEnableColorWrite(M3GCompositingMode handle, M3Gbool enable)
{
    CompositingMode *compositingMode = (CompositingMode*)handle;
    M3G_VALIDATE_OBJECT(compositingMode);
    compositingMode->colorWrite = (GLboolean) enable;
}

/*!
 * \brief Set depth offset with factor and units.
 *
 * \param handle CompositingMode object
 * \param factor slope dependent depth offset
 * \param units constant depth offset
 */
M3G_API void m3gSetDepthOffset(M3GCompositingMode handle,
                               M3Gfloat factor, M3Gfloat units)
{
    CompositingMode *compositingMode = (CompositingMode*)handle;
    M3G_VALIDATE_OBJECT(compositingMode);
    compositingMode->factor = factor;
    compositingMode->units = units;
}

/*!
 * \brief Get depth offset factor.
 *
 * \param handle CompositingMode object
 * \return slope dependent depth offset
 */
M3G_API M3Gfloat m3gGetDepthOffsetFactor(M3GCompositingMode handle)
{
    CompositingMode *compositingMode = (CompositingMode*)handle;
    M3G_VALIDATE_OBJECT(compositingMode);
    return compositingMode->factor;
}

/*!
 * \brief Get depth offset units.
 *
 * \param handle CompositingMode object
 * \return constant depth offset
 */
M3G_API M3Gfloat m3gGetDepthOffsetUnits(M3GCompositingMode handle)
{
    CompositingMode *compositingMode = (CompositingMode*)handle;
    M3G_VALIDATE_OBJECT(compositingMode);
    return compositingMode->units;
}

/*!
 * \brief Get alpha write enable.
 *
 * \param handle CompositingMode object
 * \retval M3G_TRUE alpha write enabled
 * \retval M3G_FALSE alpha write disabled
 */
M3G_API M3Gbool m3gIsAlphaWriteEnabled(M3GCompositingMode handle)
{
    CompositingMode *compositingMode = (CompositingMode*)handle;
    M3G_VALIDATE_OBJECT(compositingMode);
    return compositingMode->alphaWrite;
}

/*!
 * \brief Get color write enable.
 *
 * \param handle CompositingMode object
 * \retval M3G_TRUE color write enabled
 * \retval M3G_FALSE color  write disabled
 */
M3G_API M3Gbool m3gIsColorWriteEnabled(M3GCompositingMode handle)
{
    CompositingMode *compositingMode = (CompositingMode*)handle;
    M3G_VALIDATE_OBJECT(compositingMode);
    return compositingMode->colorWrite;
}

/*!
 * \brief Get depth test enable.
 *
 * \param handle CompositingMode object
 * \retval M3G_TRUE depth test enabled
 * \retval M3G_FALSE depth test disabled
 */
M3G_API M3Gbool m3gIsDepthTestEnabled(M3GCompositingMode handle)
{
    CompositingMode *compositingMode = (CompositingMode*)handle;
    M3G_VALIDATE_OBJECT(compositingMode);
    return compositingMode->depthTest;
}

/*!
 * \brief Get depth write enable.
 *
 * \param handle CompositingMode object
 * \retval M3G_TRUE depth write enabled
 * \retval M3G_FALSE depth write disabled
 */
M3G_API M3Gbool m3gIsDepthWriteEnabled(M3GCompositingMode handle)
{
    CompositingMode *compositingMode = (CompositingMode*)handle;
    M3G_VALIDATE_OBJECT(compositingMode);
    return compositingMode->depthWrite;
}

/*!
 * \brief Set alpha write enable.
 *
 * \param handle CompositingMode object
 * \param enable alpha write enable
 */
M3G_API void m3gSetAlphaWriteEnable(M3GCompositingMode handle, M3Gbool enable)
{
    CompositingMode *compositingMode = (CompositingMode*)handle;
    M3G_VALIDATE_OBJECT(compositingMode);
    compositingMode->alphaWrite = (GLboolean) enable;
}

