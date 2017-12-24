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
* Description: Material implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Material implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_material.h"
#include "m3g_animationtrack.h"

#define ALL_TARGET_MASK (M3G_AMBIENT_BIT | M3G_DIFFUSE_BIT | M3G_EMISSIVE_BIT | M3G_SPECULAR_BIT)

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Applies default material to OpenGL.
 */
static void m3gApplyDefaultMaterial(void)
{
    glDisable(GL_COLOR_MATERIAL);
    glDisable(GL_LIGHTING);
}

/*!
 * \internal
 * \brief Applies material to OpenGL.
 *
 * \param material Material object
 * \param alphaFactor alpha factor as 1.16 fixed point
 */
static void m3gApplyMaterial(Material *material, M3Gint alphaFactor)
{
    if (material != NULL) {
    	M3Gfloat colors[4];

        /* NOTE We must set the ColorMaterial state *before* setting
         * any of the material colors, as they will not change if
         * tracking is enabled! */
        
        if (material->vertexColorTracking) {
            glEnable(GL_COLOR_MATERIAL);
        }
        else {
            glDisable(GL_COLOR_MATERIAL);

            /* Ambient and diffuse only need to be set when tracking
             * is disabled */
            
            m3gFloatColor(material->ambientColor, 1.0f, colors);
            glMaterialfv(GL_FRONT_AND_BACK, GL_AMBIENT, colors);
            m3gFloatColor(material->diffuseColor, 1.0f, colors);
            if (alphaFactor < 0x10000) {
                colors[3] = m3gMul(colors[3],
                                   m3gMul((M3Gfloat) alphaFactor,
                                          1.f/65536.f));
            }
            glMaterialfv(GL_FRONT_AND_BACK, GL_DIFFUSE, colors);
        }
        m3gFloatColor(material->emissiveColor, 1.0f, colors);
        glMaterialfv(GL_FRONT_AND_BACK, GL_EMISSION, colors);
        m3gFloatColor(material->specularColor, 1.0f, colors);
        glMaterialfv(GL_FRONT_AND_BACK, GL_SPECULAR, colors);
        glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, material->shininess);

        glEnable(GL_LIGHTING);
    }
    else {
        m3gApplyDefaultMaterial();
    }
    M3G_ASSERT_GL;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param property      animation property
 * \retval M3G_TRUE     property supported
 * \retval M3G_FALSE    property not supported
 */
static M3Gbool m3gMaterialIsCompatible(M3Gint property)
{
    switch (property) {
    case M3G_ANIM_ALPHA:
    case M3G_ANIM_AMBIENT_COLOR:
    case M3G_ANIM_DIFFUSE_COLOR:
    case M3G_ANIM_EMISSIVE_COLOR:
    case M3G_ANIM_SHININESS:
    case M3G_ANIM_SPECULAR_COLOR:
        return M3G_TRUE;
    default:
        return m3gObjectIsCompatible(property);
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self          Material object
 * \param property      animation property
 * \param valueSize     size of value array
 * \param value         value array
 */
static void m3gMaterialUpdateProperty(Object *self,
                                      M3Gint property,
                                      M3Gint valueSize,
                                      const M3Gfloat *value)
{
    Material *material = (Material *)self;
    M3G_VALIDATE_OBJECT(material);
    M3G_ASSERT_PTR(value);

    switch (property) {
    case M3G_ANIM_ALPHA:
        M3G_ASSERT(valueSize >= 1);
        material->diffuseColor = (material->diffuseColor | M3G_ALPHA_MASK) &
            m3gAlpha1f(value[0]);
        break;
    case M3G_ANIM_AMBIENT_COLOR:
        M3G_ASSERT(valueSize >= 3);
        material->ambientColor = m3gColor3f(value[0], value[1], value[2]);
        break;
    case M3G_ANIM_DIFFUSE_COLOR:
        M3G_ASSERT(valueSize >= 3);
        material->diffuseColor = (material->diffuseColor | M3G_RGB_MASK)
            & m3gColor3f(value[0], value[1], value[2]);
        break;
    case M3G_ANIM_EMISSIVE_COLOR:
        M3G_ASSERT(valueSize >= 3);
        material->emissiveColor = m3gColor3f(value[0], value[1], value[2]) & M3G_RGB_MASK;
        break;
    case M3G_ANIM_SHININESS:
        M3G_ASSERT(valueSize >= 1);
        material->shininess = m3gClampFloat(value[0], 0.f, 128.f);
        break;
    case M3G_ANIM_SPECULAR_COLOR:
        M3G_ASSERT(valueSize >= 3);
        material->specularColor = m3gColor3f(value[0], value[1], value[2]);
        break;
    default:
        m3gObjectUpdateProperty(self, property, valueSize, value);
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original Material object
 * \param cloneObj pointer to cloned Material object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gMaterialDuplicate(const Object *originalObj,
                                    Object **cloneObj,
                                    Object **pairs,
                                    M3Gint *numPairs)
{
    const Material *original = (const Material *)originalObj;

    /* Create the clone object */
    
    Material *clone = (Material *)m3gCreateMaterial(originalObj->interface);
    if (!clone) {
        return M3G_FALSE;
    }
    *cloneObj = (Object *)clone;

    /* Duplicate base class and own data */
    
    if (m3gObjectDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        clone->vertexColorTracking = original->vertexColorTracking;
        clone->ambientColor = original->ambientColor;
        clone->diffuseColor = original->diffuseColor;
        clone->emissiveColor = original->emissiveColor;
        clone->specularColor = original->specularColor;
        clone->shininess = original->shininess;
        return M3G_TRUE;
    }
    else {
        return M3G_FALSE;
    }
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const ObjectVFTable m3gvf_Material = {
    m3gObjectApplyAnimation,
    m3gMaterialIsCompatible,
    m3gMaterialUpdateProperty,
    m3gObjectDoGetReferences,
    m3gObjectFindID,
    m3gMaterialDuplicate,
    m3gDestroyObject
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a Material object.
 *
 * \param interface     M3G interface
 * \retval Material new Material object
 * \retval NULL Material creating failed
 */
M3G_API M3GMaterial m3gCreateMaterial(M3GInterface interface)
{
    Interface *m3g = (Interface *) interface;
    M3G_VALIDATE_INTERFACE(m3g);

    {
        Material *material = m3gAllocZ(m3g, sizeof(Material));

        if (material != NULL) {
            m3gInitObject(&material->object, m3g, M3G_CLASS_MATERIAL);
            /* Default values are from the jsr-184 specification */
            material->vertexColorTracking = GL_FALSE;
            material->ambientColor = 0x00333333U;
            material->diffuseColor = 0xFFCCCCCCU;
            material->emissiveColor = 0x00000000U;
            material->specularColor = 0x00000000U;
            material->shininess = 0.0f;
        }

        return (M3GMaterial)material;
    }
}

/*!
 * \brief Set material color.
 *
 * \param hMaterial Material object
 * \param target    color target
 * \param ARGB      ARGB color
 */
M3G_API void m3gSetColor(M3GMaterial hMaterial, M3Genum target, M3Guint ARGB)
{
    Material *material = (Material *) hMaterial;
    M3G_VALIDATE_OBJECT(material);
    /* is invalid target in the mask OR no target */
    if (((target | ALL_TARGET_MASK) != ALL_TARGET_MASK) || ((target & ALL_TARGET_MASK) == 0)) {
        m3gRaiseError(M3G_INTERFACE(material), M3G_INVALID_VALUE);
        return;
    }
    if ((target & M3G_AMBIENT_BIT) != 0) {
        material->ambientColor = ARGB & M3G_RGB_MASK;
    }
    if ((target & M3G_DIFFUSE_BIT) != 0) {
        material->diffuseColor = ARGB;
    }
    if ((target & M3G_EMISSIVE_BIT) != 0) {
        material->emissiveColor = ARGB & M3G_RGB_MASK;
    }
    if ((target & M3G_SPECULAR_BIT) != 0) {
        material->specularColor = ARGB & M3G_RGB_MASK;
    }
}

/*!
 * \brief Get material color.
 *
 * \param hMaterial Material object
 * \param target    color target
 * \return          ARGB color
 */
M3G_API M3Guint m3gGetColor(M3GMaterial hMaterial, M3Genum target)
{
    Material *material = (Material *) hMaterial;
    M3G_VALIDATE_OBJECT(material);
    
    switch (target) {
    case M3G_AMBIENT_BIT:
        return material->ambientColor;
    case M3G_DIFFUSE_BIT:
        return material->diffuseColor;
    case M3G_EMISSIVE_BIT:
        return material->emissiveColor;
    case M3G_SPECULAR_BIT:
        return material->specularColor;
    default:
        m3gRaiseError(M3G_INTERFACE(material), M3G_INVALID_VALUE);
        break;
    }

    return 0; /* Error */
}

/*!
 * \brief Set material shininess.
 *
 * \param hMaterial Material object
 * \param shininess shininess
 */
M3G_API void m3gSetShininess(M3GMaterial hMaterial, M3Gfloat shininess)
{
    Material *material = (Material *) hMaterial;
    M3G_VALIDATE_OBJECT(material);
    if (!m3gInRangef(shininess, 0.0f, 128.f)) {
        m3gRaiseError(M3G_INTERFACE(material), M3G_INVALID_VALUE);
        return;
    }
    material->shininess = shininess;
}

/*!
 * \brief Get material shininess.
 *
 * \param hMaterial Material object
 * \return          shininess
 */
M3G_API GLfloat m3gGetShininess(M3GMaterial hMaterial)
{
    Material *material = (Material *) hMaterial;
    M3G_VALIDATE_OBJECT(material);
    return material->shininess;
}

/*!
 * \brief Set vertex color tracking enable.
 *
 * \param hMaterial Material object
 * \param enable color tracking enable
 */
M3G_API void m3gSetVertexColorTrackingEnable(M3GMaterial hMaterial,
                                             M3Gbool enable)
{
    Material *material = (Material *) hMaterial;
    M3G_VALIDATE_OBJECT(material);
    material->vertexColorTracking = (GLboolean) enable;
}

/*!
 * \brief Get vertex color tracking enable.
 *
 * \param hMaterial Material object
 * \retval M3G_TRUE color tracking enabled
 * \retval M3G_FALSE color tracking disabled
 */
M3G_API M3Gbool m3gIsVertexColorTrackingEnabled(M3GMaterial hMaterial)
{
    Material *material = (Material *) hMaterial;
    M3G_VALIDATE_OBJECT(material);
    return (M3Gbool) material->vertexColorTracking;
}

