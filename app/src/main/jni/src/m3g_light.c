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
* Description: Light implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Light implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_light.h"
#include "m3g_animationtrack.h"

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Overloaded Node method.
 *
 * Insert light to render queue.
 *
 * \param self Light object
 * \param toCamera transform to camera
 * \param alphaFactor total alpha factor
 * \param caller caller node
 * \param renderQueue RenderQueue
 *
 * \retval M3G_TRUE continue render setup
 * \retval M3G_FALSE abort render setup
 */
static M3Gbool m3gLightSetupRender(Node *self,
                                   const Node *caller,
                                   SetupRenderState *s,
                                   RenderQueue *renderQueue)
{
    M3G_UNREF(caller);

    if (renderQueue->lightManager != NULL) {
        Light *light = (Light *)self;
        
        if (self->enableBits & NODE_RENDER_BIT) {
            if (m3gInsertLight(renderQueue->lightManager,
                               light, &s->toCamera, M3G_INTERFACE(light)) == -1)
                return M3G_FALSE;
        }
    }

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
static M3Gbool m3gLightIsCompatible(M3Gint property)
{
    switch (property) {
    case M3G_ANIM_COLOR:
    case M3G_ANIM_INTENSITY:
    case M3G_ANIM_SPOT_ANGLE:
    case M3G_ANIM_SPOT_EXPONENT:
        return M3G_TRUE;
    default:
        return m3gNodeIsCompatible(property);
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self          Light object
 * \param property      animation property
 * \param valueSize     size of value array
 * \param value         value array
 */
static void m3gLightUpdateProperty(Object *self,
                                   M3Gint property,
                                   M3Gint valueSize,
                                   const M3Gfloat *value)
{
    Light *light = (Light *)self;
    M3G_VALIDATE_OBJECT(light);
    M3G_ASSERT_PTR(value);
    
    switch (property) {
    case M3G_ANIM_COLOR:
        M3G_ASSERT(valueSize >= 3);
        light->color = m3gColor3f(value[0], value[1], value[2]);
        break;
    case M3G_ANIM_INTENSITY:
        M3G_ASSERT(valueSize >= 1);
        light->intensity = value[0];
        break;
    case M3G_ANIM_SPOT_ANGLE:
        M3G_ASSERT(valueSize >= 1);
        light->spotAngle = m3gClampFloat(value[0], 0.f, 90.f);
        break;
    case M3G_ANIM_SPOT_EXPONENT:
        M3G_ASSERT(valueSize >= 1);
        light->spotExponent = m3gClampFloat(value[0], 0.f, 128.f);
        break;
    default:
        m3gNodeUpdateProperty(self, property, valueSize, value);
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original Light object
 * \param cloneObj pointer to cloned Light object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gLightDuplicate(const Object *originalObj,
                                 Object **cloneObj,
                                 Object **pairs,
                                 M3Gint *numPairs)
{
    const Light *original = (const Light *) originalObj;
    Light *clone;
    M3G_ASSERT(*cloneObj == NULL); /* no derived classes for light */
    
    /* Create the clone object and exit on failure */
    
    clone = (Light *)m3gCreateLight(originalObj->interface);
    if (!clone) {
        return M3G_FALSE;
    }
    *cloneObj = (Object *)clone;

    /* Duplicate base class data */
    
    if (!m3gNodeDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        return M3G_FALSE;
    }

    /* Duplicate own data */
    
	clone->constantAttenuation = original->constantAttenuation;
	clone->linearAttenuation = original->linearAttenuation;
	clone->quadraticAttenuation = original->quadraticAttenuation;
	clone->intensity = original->intensity;
	clone->color = original->color;
	clone->mode = original->mode;
    clone->spotAngle = original->spotAngle;
	clone->spotExponent = original->spotExponent;
    
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Initializes a Light object. See specification
 * for default values.
 *
 * \param m3g           M3G interface
 * \param light         Light object
 */
static void m3gInitLight(Interface *m3g, Light *light)
{
	/* Light is derived from node */
	m3gInitNode(m3g, &light->node, M3G_CLASS_LIGHT);

	light->constantAttenuation = 1.0f;
	light->intensity = 1.0f;
	light->color = 0x00ffffff;  /* Full white */
	light->mode = M3G_DIRECTIONAL;
	light->spotAngle = 45.0f;
}

/*!
 * \internal
 * \brief Applies this light to the current OpenGL context.
 *
 * \param self          Light object
 * \param glLight       OpenGL light index
 * \param pos           light position
 * \param spotDir       light direction
 */
static void m3gApplyLight(const Light *self,
                          GLenum glLight,
                          const Vec4 *pos,
                          const Vec4 *spotDir)
{
	static const M3Gfloat BLACK[] = { 0.0f, 0.0f, 0.0f, 0.0f };
	M3Gfloat light[4];
    
    M3G_ASSERT(m3gInRange(glLight, GL_LIGHT0, GL_LIGHT7));
    glEnable(glLight);
    
	m3gFloatColor(self->color, self->intensity, light);

    /* Set light position */
    
    if (self->mode == M3G_DIRECTIONAL) {
        GLfloat temp[4];
        temp[0] = -spotDir->x;
        temp[1] = -spotDir->y;
        temp[2] = -spotDir->z;
        temp[3] = 0.0f;
        glLightfv(glLight, GL_POSITION, temp);
    }
    else {
        glLightfv(glLight, GL_POSITION, &pos->x);
        if (self->mode == M3G_SPOT) {
            glLightfv(glLight, GL_SPOT_DIRECTION, &spotDir->x);
        }
    }

    /* Set ambient, diffuse, and specular contributions */
    
	if (self->mode == M3G_AMBIENT) {
		glLightfv(glLight, GL_AMBIENT, light);
		glLightfv(glLight, GL_DIFFUSE, BLACK);
		glLightfv(glLight, GL_SPECULAR, BLACK);
	}
	else {
		glLightfv(glLight, GL_AMBIENT, BLACK);
		glLightfv(glLight, GL_DIFFUSE, light);
		glLightfv(glLight, GL_SPECULAR, light);
	}

    /* Set spot parameters */
    
	if (self->mode == M3G_SPOT) {
		glLightf(glLight, GL_SPOT_EXPONENT, self->spotExponent);
		glLightf(glLight, GL_SPOT_CUTOFF, self->spotAngle);
	}
	else {
		glLightf(glLight, GL_SPOT_CUTOFF, 180.0f);
	}

    /* Set attenuation */
    
	if (self->mode == M3G_OMNI || self->mode == M3G_SPOT) {
		glLightf(glLight, GL_CONSTANT_ATTENUATION,  self->constantAttenuation);
		glLightf(glLight, GL_LINEAR_ATTENUATION,    self->linearAttenuation);
		glLightf(glLight, GL_QUADRATIC_ATTENUATION, self->quadraticAttenuation);
	}
	else if (self->mode == M3G_AMBIENT) {
		glLightf(glLight, GL_CONSTANT_ATTENUATION,  1.0f);
		glLightf(glLight, GL_LINEAR_ATTENUATION,    0.0f);
		glLightf(glLight, GL_QUADRATIC_ATTENUATION, 0.0f);
	}
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const NodeVFTable m3gvf_Light = {
    {
        {
            m3gObjectApplyAnimation,
            m3gLightIsCompatible,
            m3gLightUpdateProperty,
            m3gObjectDoGetReferences,
            m3gObjectFindID,
            m3gLightDuplicate,
            m3gDestroyNode
        }
    },
    m3gNodeAlign,
    NULL, /* pure virtual DoRender */
    m3gNodeGetBBox,
    m3gNodeRayIntersect,
    m3gLightSetupRender,
    m3gNodeUpdateDuplicateReferences,
    m3gNodeValidate
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a Light object.
 *
 * \param interface     M3G interface
 * \retval Light new Light object
 * \retval NULL Light creating failed
 */
M3G_API M3GLight m3gCreateLight(M3GInterface interface)
{
    Interface *m3g = (Interface *) interface;
    M3G_VALIDATE_INTERFACE(m3g);

	{
		Light *light =  m3gAllocZ(m3g, sizeof(Light));

        if (light != NULL) {
    		m3gInitLight(m3g, light);
        }

		return (M3GLight) light;
	}
}

/*!
 * \brief Set light intensity.
 *
 * \param handle        Light object
 * \param intensity     light intensity
 */
M3G_API void m3gSetIntensity(M3GLight handle, M3Gfloat intensity)
{
	Light *light = (Light *) handle;
	M3G_VALIDATE_OBJECT(light);
	
	light->intensity = intensity;
}

/*!
 * \brief Set light color as RGB.
 *
 * \param handle        Light object
 * \param rgb           light color as RGB
 */
M3G_API void m3gSetLightColor(M3GLight handle, M3Guint rgb)
{
	Light *light = (Light *) handle;
	M3G_VALIDATE_OBJECT(light);

	light->color = rgb & M3G_RGB_MASK;
}

/*!
 * \brief Set light mode.
 *
 * \param handle        Light object
 * \param mode          light mode
 */
M3G_API void m3gSetLightMode(M3GLight handle, M3Gint mode)
{
	Light *light = (Light *) handle;
	M3G_VALIDATE_OBJECT(light);

	if (mode < M3G_AMBIENT || mode > M3G_SPOT) {
    	m3gRaiseError(M3G_INTERFACE(light), M3G_INVALID_VALUE);
        return;
    }

	light->mode = mode;
}

/*!
 * \brief Set light spot angle.
 *
 * \param handle        Light object
 * \param angle         spot angle
 */
M3G_API void m3gSetSpotAngle(M3GLight handle, M3Gfloat angle)
{
	Light *light = (Light *) handle;
	M3G_VALIDATE_OBJECT(light);

	if (angle < 0.0f || angle > 90.f) {
    	m3gRaiseError(M3G_INTERFACE(light), M3G_INVALID_VALUE);
        return;
	}

	light->spotAngle = angle;
}

/*!
 * \brief Set light spot exponent.
 *
 * \param handle        Light object
 * \param exponent      spot exponent
 */
M3G_API void m3gSetSpotExponent(M3GLight handle, M3Gfloat exponent)
{
	Light *light = (Light *) handle;
	M3G_VALIDATE_OBJECT(light);

    if (exponent < 0.0f || exponent > 128.0f) {
    	m3gRaiseError(M3G_INTERFACE(light), M3G_INVALID_VALUE);
        return;
    }

    light->spotExponent = exponent;
}

/*!
 * \brief Set light attenuation factors.
 *
 * \param handle        Light object
 * \param constant      constant attenuation
 * \param linear        linear attenuation
 * \param quadratic     quadratic attenuation
 */
M3G_API void m3gSetAttenuation(M3GLight handle,
                               M3Gfloat constant,
                               M3Gfloat linear,
                               M3Gfloat quadratic)
{
	Light *light = (Light *) handle;
	M3G_VALIDATE_OBJECT(light);

    if (constant < 0.0f || linear < 0.0f || quadratic < 0.0f
        || (constant == 0.0f && linear == 0.0f && quadratic == 0.0f)) {
    	m3gRaiseError(M3G_INTERFACE(light), M3G_INVALID_VALUE);
        return;
    }

    light->constantAttenuation  = constant;
    light->linearAttenuation    = linear;
    light->quadraticAttenuation = quadratic;
}

/*!
 * \brief Get light intensity.
 *
 * \param handle        Light object
 * \return              light intensity
 */
M3G_API M3Gfloat m3gGetIntensity(M3GLight handle)
{
	Light *light = (Light *) handle;
	M3G_VALIDATE_OBJECT(light);

    return light->intensity;
}

/*!
 * \brief Get light color as RGB.
 *
 * \param handle        Light object
 * \return              light color as RGB
 */
M3G_API M3Guint m3gGetLightColor(M3GLight handle)
{
	Light *light = (Light *) handle;
	M3G_VALIDATE_OBJECT(light);

    return light->color;
}

/*!
 * \brief Get light mode.
 *
 * \param handle        Light object
 * \return              light mode
 */
M3G_API M3Gint m3gGetLightMode(M3GLight handle)
{
	Light *light = (Light *) handle;
	M3G_VALIDATE_OBJECT(light);

    return light->mode;
}

/*!
 * \brief Get light spot angle.
 *
 * \param handle        Light object
 * \return              light spot angle
 */
M3G_API M3Gfloat m3gGetSpotAngle(M3GLight handle)
{
	Light *light = (Light *) handle;
	M3G_VALIDATE_OBJECT(light);

    return light->spotAngle;
}

/*!
 * \brief Get light spot exponent.
 *
 * \param handle        Light object
 * \return              light spot exponent
 */
M3G_API M3Gfloat m3gGetSpotExponent(M3GLight handle)
{
	Light *light = (Light *) handle;
	M3G_VALIDATE_OBJECT(light);

    return light->spotExponent;
}

/*!
 * \brief Get light attenuation factor.
 *
 * \param handle        Light object
 * \param type          which factor to return
 *                      \arg M3G_GET_CONSTANT
 *                      \arg M3G_GET_LINEAR
 *                      \arg M3G_GET_QUADRATIC
 * \return              light attenuation factor
 */
M3G_API M3Gfloat m3gGetAttenuation(M3GLight handle, M3Gint type)
{
	Light *light = (Light *) handle;
	M3G_VALIDATE_OBJECT(light);

	switch(type) {
	case M3G_GET_CONSTANT:
	    return light->constantAttenuation;
	case M3G_GET_LINEAR:
    	return light->linearAttenuation;
    case M3G_GET_QUADRATIC:
	default:
	    return light->quadraticAttenuation;
	}
}

