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
* Description: Fog implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Fog implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_fog.h"
#include "m3g_animationtrack.h"

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Destroys this Fog object.
 *
 * \param obj Fog object
 */
static void m3gDestroyFog(Object *obj)
{
    Fog *fog = (Fog *) obj;
    M3G_VALIDATE_OBJECT(fog);

    m3gDestroyObject(&fog->object);
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param property      animation property
 * \retval M3G_TRUE     property supported
 * \retval M3G_FALSE    property not supported
 */
static M3Gbool m3gFogIsCompatible(M3Gint property)
{
    switch (property) {
    case M3G_ANIM_COLOR:
    case M3G_ANIM_DENSITY:
    case M3G_ANIM_FAR_DISTANCE:
    case M3G_ANIM_NEAR_DISTANCE:
        return M3G_TRUE;
    default:
        return m3gObjectIsCompatible(property);
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self          Fog object
 * \param property      animation property
 * \param valueSize     size of value array
 * \param value         value array
 */
static void m3gFogUpdateProperty(Object *self,
                                 M3Gint property,
                                 M3Gint valueSize,
                                 const M3Gfloat *value)
{
    Fog *fog = (Fog *)self;
    M3G_VALIDATE_OBJECT(fog);
    M3G_ASSERT_PTR(value);

    switch (property) {
    case M3G_ANIM_COLOR:
        M3G_ASSERT(valueSize >= 3);
        fog->color = m3gColor3f(value[0], value[1], value[2]) & M3G_RGB_MASK;
        break;
    case M3G_ANIM_DENSITY:
        M3G_ASSERT(valueSize >= 1);
        fog->density = (value[0] < 0.f) ? 0.f : value[0];
        break;
    case M3G_ANIM_FAR_DISTANCE:
        M3G_ASSERT(valueSize >= 1);
        fog->end = value[0];
        break;
    case M3G_ANIM_NEAR_DISTANCE:
        M3G_ASSERT(valueSize >= 1);
        fog->start = value[0];
        break;
    default:
        m3gObjectUpdateProperty(self, property, valueSize, value);
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original Fog object
 * \param cloneObj pointer to cloned Fog object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gFogDuplicate(const Object *originalObj,
                               Object **cloneObj,
                               Object **pairs,
                               M3Gint *numPairs)
{
    Fog *original = (Fog *)originalObj;
    Fog *clone = (Fog *)m3gCreateFog(originalObj->interface);
    *cloneObj = (Object *)clone;
    if (*cloneObj == NULL) {
        return M3G_FALSE;
    }

    if(m3gObjectDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        clone->color = original->color;
        clone->density = original->density;
        clone->start = original->start;
        clone->end = original->end;
        clone->mode = original->mode;
        return M3G_TRUE;
    }
    else {
        return M3G_FALSE;
    }
}

/*!
 * \internal
 * \brief Initializes a Fog object. See specification
 * for default values.
 *
 * \param m3g           M3G interface
 * \param fog           Fog object
 */
static void m3gInitFog(Interface *m3g, Fog *fog)
{
	/* Fog is derived from object */
	m3gInitObject(&fog->object, m3g, M3G_CLASS_FOG);

	fog->density = 1.0f;
	fog->start = 0.0f;
	fog->end = 1.0f;
	fog->mode = M3G_LINEAR_FOG;
}

/*!
 * \internal
 * \brief Applies fog to OpenGL. This is used for
 * all Mesh objects.
 *
 * \param self          Fog object
 */
static void m3gApplyFog(const Fog *self)
{
	if (self != NULL) {
		GLfixed temp[4];

        m3gGLColor(self->color, temp);

		switch (self->mode) {
		case M3G_LINEAR_FOG:
			glEnable(GL_FOG);
			glFogf(GL_FOG_MODE, GL_LINEAR);
			glFogf(GL_FOG_START, self->start);
			glFogf(GL_FOG_END, self->end);
			glFogxv(GL_FOG_COLOR, temp);
			break;
		case M3G_EXPONENTIAL_FOG:
			glEnable(GL_FOG);
			glFogf(GL_FOG_MODE, GL_EXP);
			glFogf(GL_FOG_DENSITY, self->density);
			glFogxv(GL_FOG_COLOR, temp);
			break;
		}
	}
	else {
		glDisable(GL_FOG);
	}
    M3G_ASSERT_GL;
}

#ifdef M3G_USE_NGL_API
/*!
 * \internal
 * \brief Applies fog to NGL. This is used for
 * Sprite3D objects only.
 *
 * \param self          Fog object
 * \param eyeZ          Eye space Z (e.g. after modelview)
 * \param finalZ        Final Z (e.g. after modelview and projection)
 */
static void m3gApplySpriteFog(const Fog *self, M3Gfloat eyeZ, M3Gfloat finalZ)
{
    if(self != NULL) {
		M3Gint temp[4];
    	M3Gfloat fogValue = 1;

        /* Calculate fog value and use OpenGL linear fog
         * to result in same value. Sprites are drawn with
         * identity MV and P and therefore the fog has to
         * be adjusted like this */
        switch (self->mode) {
    	case M3G_LINEAR_FOG:
            fogValue = m3gDiv(m3gAdd(self->end, eyeZ), m3gSub(self->end, self->start));
    		break;
    	case M3G_EXPONENTIAL_FOG:
            fogValue = m3gExp(m3gMul(self->density, eyeZ));
    		break;
        default:
            M3G_ASSERT(M3G_FALSE);
            break;
    	}

        m3gGLColor(self->color, temp);

		glEnable(GL_FOG);
		glFogf(GL_FOG_MODE, GL_LINEAR);

        /* NGL works differently in fog calculation */
		glFogf(GL_FOG_START, -m3gDiv(finalZ, fogValue));
		glFogf(GL_FOG_END, 0.f);
		glFogxv(GL_FOG_COLOR, temp);
    }
    else {
		glDisable(GL_FOG);
    }
}
#endif
/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const ObjectVFTable m3gvf_Fog = {
    m3gObjectApplyAnimation,
    m3gFogIsCompatible,
    m3gFogUpdateProperty,
    m3gObjectDoGetReferences,
    m3gObjectFindID,
    m3gFogDuplicate,
    m3gDestroyFog
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a Fog object.
 *
 * \param interface     M3G interface
 * \retval Fog new Fog object
 * \retval NULL Fog creating failed
 */
M3G_API M3GFog m3gCreateFog(M3GInterface interface)
{
    Interface *m3g = (Interface *) interface;
    M3G_VALIDATE_INTERFACE(m3g);

	{
		Fog *fog =  m3gAllocZ(m3g, sizeof(Fog));

        if (fog != NULL) {
    		m3gInitFog(m3g, fog);
        }

		return (M3GFog) fog;
	}
}

/*!
 * \brief Sets fog mode.
 *
 * \param handle        Fog object
 * \param mode          fog mode
 */
M3G_API void m3gSetFogMode(M3GFog handle, M3Gint mode)
{
	Fog *fog = (Fog *) handle;
	M3G_VALIDATE_OBJECT(fog);

	/* Check for errors */
	if(mode < M3G_EXPONENTIAL_FOG || mode > M3G_LINEAR_FOG) {
		m3gRaiseError(M3G_INTERFACE(fog), M3G_INVALID_VALUE);
        return;
	}

	fog->mode = mode;
}

/*!
 * \brief Gets fog mode.
 *
 * \param handle        Fog object
 * \return              fog mode
 */
M3G_API M3Gint m3gGetFogMode(M3GFog handle)
{
	Fog *fog = (Fog *) handle;
	M3G_VALIDATE_OBJECT(fog);

	return fog->mode;
}

/*!
 * \brief Sets linear fog parameters.
 *
 * \param handle        Fog object
 * \param fogNear       near distance
 * \param fogFar        far distance
 */
M3G_API void m3gSetFogLinear(M3GFog handle, M3Gfloat fogNear, M3Gfloat fogFar)
{
	Fog *fog = (Fog *) handle;
	M3G_VALIDATE_OBJECT(fog);

	fog->start = fogNear;
	fog->end = fogFar;
}

/*!
 * \brief Gets linear fog parameters.
 *
 * \param handle        Fog object
 * \param which         which parameter to return
 *                      \arg M3G_GET_NEAR
 *                      \arg M3G_GET_FAR
 * \return              near or far distance
 */
M3G_API M3Gfloat m3gGetFogDistance(M3GFog handle, M3Gint which)
{
	Fog *fog = (Fog *) handle;
	M3G_VALIDATE_OBJECT(fog);

	switch(which) {
	case M3G_GET_NEAR:
		return fog->start;
	case M3G_GET_FAR:
	default:
		return fog->end;
	}
}

/*!
 * \brief Sets exponential fog density.
 *
 * \param handle        Fog object
 * \param density       fog density
 */
M3G_API void m3gSetFogDensity(M3GFog handle, M3Gfloat density)
{
	Fog *fog = (Fog *) handle;
	M3G_VALIDATE_OBJECT(fog);

	if(density < 0.f) {
		m3gRaiseError(M3G_INTERFACE(fog), M3G_INVALID_VALUE);
        return;
	}

	fog->density = density;
}

/*!
 * \brief Gets exponential fog density.
 *
 * \param handle        Fog object
 * \return              fog density
 */
M3G_API M3Gfloat m3gGetFogDensity(M3GFog handle)
{
	Fog *fog = (Fog *) handle;
	M3G_VALIDATE_OBJECT(fog);

	return fog->density;
}

/*!
 * \brief Sets fog color as RGB.
 *
 * \param handle        Fog object
 * \param rgb           fog color as RGB
 */
M3G_API void m3gSetFogColor(M3GFog handle, M3Guint rgb)
{
	Fog *fog = (Fog *) handle;
	M3G_VALIDATE_OBJECT(fog);

    fog->color = rgb & M3G_RGB_MASK;
}

/*!
 * \brief Gets fog color as RGB.
 *
 * \param handle        Fog object
 * \return              fog color as RGB
 */
M3G_API M3Guint m3gGetFogColor(M3GFog handle)
{
	Fog *fog = (Fog *) handle;
	M3G_VALIDATE_OBJECT(fog);

    return fog->color;
}

