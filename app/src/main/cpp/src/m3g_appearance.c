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
* Description: Appearance implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Appearance implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_appearance.h"
#include "m3g_vertexbuffer.h"

/*----------------------------------------------------------------------
 * Private functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Applies default appearance values to OpenGL.
 */
static void m3gApplyAppearanceDefaults(RenderContext *ctx)
{
    int i;
    m3gApplyCompositingMode(NULL, ctx);
    m3gApplyPolygonMode(NULL);
    m3gApplyMaterial(NULL, 0x10000);
	m3gApplyFog(NULL);

    for (i = 0; i < M3G_NUM_TEXTURE_UNITS; ++i) {
        glActiveTexture(GL_TEXTURE0 + i);
        glDisable(GL_TEXTURE_2D);
    }
}

/*!
 * \internal
 * \brief Generate a hash number for a pointer
 */
static M3Guint m3gGenPointerHash(const void *ptr)
{
    M3Guint p = ((M3Guint) ptr) >> 2;
    M3Guint key = p ^ (p >> 5) ^ (p >> 10) ^ (p >> 15) ^ (p >> 20) ^ (p >> 25);
    return key;
}

/*!
 * \internal
 * \brief Generate a quick hash bit pattern for the textures of this
 * appearance object
 */
static M3Guint m3gGen12BitTextureHash(const Appearance *app)
{
    M3Guint key = 0; 
	
    int i;
    
    for (i = 0; i < M3G_NUM_TEXTURE_UNITS; ++i) {
        const Texture *tex = app->texture[i];
        if (tex) {
            key ^= (m3gGenPointerHash(m3gGetTextureImage((M3GTexture)tex)) >> i) << 6;
            key ^= (m3gGenPointerHash(tex) >> i) & 0x3Fu;
        }
    }
    return key & ((1u<<12)-1);
}

/*!
 * \internal
 * \brief Generate the sorting key for render queue
 *
 * Sort key is a combination of user settable layer and
 * blending mode. Blended objects are always drawn last.
 *
 * \param appearance    Appearance object
 * \return              sort key
 */
static void m3gRegenerateSortKey(Appearance *appearance)
{
	M3Guint key = 0;
    M3G_VALIDATE_OBJECT(appearance);

    /*------------------------------------------------------------
     * First do the mandatory sorting by layer index and blending
     * state; this currently uses the top eight bits, 31..24
     *-----------------------------------------------------------*/
        
    key = (appearance->layer - M3G_APPEARANCE_MIN_LAYER)
        << (33 - M3G_APPEARANCE_HARD_SORT_BITS);
    
    /* NOTE the blending state bit is not set here, but dynamically in
     * m3gGetAppearanceSortKey; this way we do not need to implement
     * signaling from CompositingMode to Appearance when the blending
     * state changes */

    /*-----------------------------------------------------------------
     * The rest of the bits, 23..0, affect performance only; ideally,
     * these should be sorted so that the more expensive state is in the
     * higher bits, but this is largely dependent on the hardware
     *----------------------------------------------------------------*/

    /* Texturing changes are often expensive in graphics hardware, so
     * we put a hash of the texture objects into the top twelve
     * bits
     *
     * NOTE we do not currently update this if a texture image
     * changes, but that shouldn't happen too often and only has
     * relatively minor performance implications
     */

    key |= m3gGen12BitTextureHash(appearance) << 12;

    /* Use the rest of the bits for the various components; depth
     * function changes are another potentially costly operation, so
     * put that next */

    key |= (m3gGenPointerHash(appearance->compositingMode) & 0x0Fu) << 8;
    key |= (m3gGenPointerHash(appearance->material) & 0x07u) << 5;
    key |= (m3gGenPointerHash(appearance->polygonMode) & 0x07u) << 2;
    key |= (m3gGenPointerHash(appearance->fog) & 0x03u);
    
    /* Store the final value */
    appearance->sortKey = key;
}

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Destroys this Appearance object.
 *
 * \param obj Appearance object
 */
static void m3gDestroyAppearance(Object *obj)
{
    int i;
    Appearance *appearance = (Appearance *) obj;
    M3G_VALIDATE_OBJECT(appearance);

    M3G_ASSIGN_REF(appearance->compositingMode, NULL);
    M3G_ASSIGN_REF(appearance->fog,             NULL);
    M3G_ASSIGN_REF(appearance->material,        NULL);
    M3G_ASSIGN_REF(appearance->polygonMode,     NULL);
    for (i = 0; i < M3G_NUM_TEXTURE_UNITS; ++i) {
        M3G_ASSIGN_REF(appearance->texture[i], NULL);
    }
    
    m3gDestroyObject(obj);
}

/*!
 * \internal
 * \brief Applies Apperance settings to current OpenGL state
 *
 * \note m3gReleaseTextures must be called when no longer using this,
 * to properly reset texture usage counters and unmap the texture
 * images.
 * 
 * \param appearance    Appearance object
 * \param alphaFactor   alpha factor as 1.16 fixed point
 */
static void m3gApplyAppearance(const Appearance *appearance,
							   RenderContext *ctx,
                               M3Gint alphaFactor)
{
    M3G_ASSERT_GL;

    if (appearance != NULL) {
        int i;

#       if defined(M3G_NGL_TEXTURE_API)
        m3gLockMemory(M3G_INTERFACE(appearance)); /* for textures */
#       endif
    
        m3gApplyCompositingMode(appearance->compositingMode, ctx);
        m3gApplyPolygonMode(appearance->polygonMode);
        m3gApplyMaterial(appearance->material, alphaFactor);
        m3gApplyFog(appearance->fog);

        for (i = 0; i < M3G_NUM_TEXTURE_UNITS; ++i) {
            Texture *tex = appearance->texture[i];
            glActiveTexture(GL_TEXTURE0 + i);
            if (tex != NULL) {
                glEnable(GL_TEXTURE_2D);
                m3gBindTexture(appearance->texture[i]);
            }
            else {
                glDisable(GL_TEXTURE_2D);
            }
        }
    }
    else {
        m3gApplyAppearanceDefaults(ctx);
    }

    M3G_ASSERT_GL;
}

/*!
 * \internal
 * \brief Release the textures bound for this appearance
 */
static void m3gReleaseTextures(const Appearance *appearance)
{
    if (appearance != NULL) {
        int i;

        for (i = 0; i < M3G_NUM_TEXTURE_UNITS; ++i) {
            Texture *tex = appearance->texture[i];
            if (tex != NULL) {
                m3gReleaseTexture(tex);
            }
        }

#       if defined(M3G_NGL_TEXTURE_API)
        m3gUnlockMemory(M3G_INTERFACE(appearance));
#       endif
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self Appearance object
 * \param time current world time
 * \return minimum validity
 */
static M3Gint m3gAppearanceApplyAnimation(Object *self, M3Gint time) {
    M3Gint i, validity, minValidity = 0x7fffffff;
    Appearance *appearance = (Appearance *)self;
    M3G_VALIDATE_OBJECT(appearance);

    if (appearance->compositingMode != NULL) {
        validity = M3G_VFUNC(Object, appearance->compositingMode, applyAnimation)((Object *)appearance->compositingMode, time);
        minValidity = (validity < minValidity ? validity : minValidity);
    }
    if (appearance->fog != NULL) {
        validity = M3G_VFUNC(Object, appearance->fog, applyAnimation)((Object *)appearance->fog, time);
        minValidity = (validity < minValidity ? validity : minValidity);
    }
    if (appearance->material != NULL) {
        validity = M3G_VFUNC(Object, appearance->material, applyAnimation)((Object *)appearance->material, time);
        minValidity = (validity < minValidity ? validity : minValidity);
    }
    for (i = 0; i < M3G_NUM_TEXTURE_UNITS; ++i) {
        if (appearance->texture[i] != NULL) {
            validity = M3G_VFUNC(Object, appearance->texture[i], applyAnimation)((Object *)appearance->texture[i], time);
            minValidity = (validity < minValidity ? validity : minValidity);
        }
    }

    /* no animations can target an Appearance directly, so we need
       not call super.applyAnimation() here. */
    return minValidity;
}    

/*!
 * \internal
 * \brief Overloaded Object3D method.
 */
static Object *m3gAppearanceFindID(Object *self, M3Gint userID)
{
    int i;
    Appearance *app = (Appearance *)self;
    Object *found = m3gObjectFindID(self, userID);
    
    if (!found && app->compositingMode) {
        found = m3gFindID((Object*) app->compositingMode, userID);
    }
    if (!found && app->polygonMode) {
        found = m3gFindID((Object*) app->polygonMode, userID);
    }
    if (!found && app->fog) {
        found = m3gFindID((Object*) app->fog, userID);
    }
    if (!found && app->material) {
        found = m3gFindID((Object*) app->material, userID);
    }
    for (i = 0; !found && i < M3G_NUM_TEXTURE_UNITS; ++i) {
        if (app->texture[i]) {
            found = m3gFindID((Object*) app->texture[i], userID);
        }
    }
    return found;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self Appearance object
 * \param references array of reference objects
 * \return number of references
 */
static M3Gint m3gAppearanceDoGetReferences(Object *self, Object **references)
{
    Appearance *app = (Appearance *)self;
    M3Gint i, num = m3gObjectDoGetReferences(self, references);
    if (app->compositingMode != NULL) {
        if (references != NULL)
            references[num] = (Object *)app->compositingMode;
        num++;
    }
    if (app->polygonMode != NULL) {
        if (references != NULL)
            references[num] = (Object *)app->polygonMode;
        num++;
    }
    if (app->fog != NULL) {
        if (references != NULL)
            references[num] = (Object *)app->fog;
        num++;
    }
    if (app->material != NULL) {
        if (references != NULL)
            references[num] = (Object *)app->material;
        num++;
    }
    for (i = 0; i < M3G_NUM_TEXTURE_UNITS; i++) {
        if (app->texture[i] != NULL) {
            if (references != NULL)
                references[num] = (Object *)app->texture[i];
            num++;
        }
    }
    return num;
}


/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original Appearance object
 * \param cloneObj pointer to cloned Appearance object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gAppearanceDuplicate(const Object *originalObj,
                                      Object **cloneObj,
                                      Object **pairs,
                                      M3Gint *numPairs)
{
    M3Gint i;
    Appearance *original = (Appearance *)originalObj;
    Appearance *clone = (Appearance *)m3gCreateAppearance(originalObj->interface);
    *cloneObj = (Object *)clone;
    if (*cloneObj == NULL) {
        return M3G_FALSE;
    }

    if (m3gObjectDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        clone->layer = original->layer;
    
        M3G_ASSIGN_REF(clone->compositingMode, original->compositingMode);
        M3G_ASSIGN_REF(clone->fog, original->fog);
        M3G_ASSIGN_REF(clone->polygonMode, original->polygonMode);
        M3G_ASSIGN_REF(clone->material, original->material);
        for (i = 0; i < M3G_NUM_TEXTURE_UNITS; i++) {
            M3G_ASSIGN_REF(clone->texture[i], original->texture[i]);
        }

        m3gRegenerateSortKey(clone);
        
        return M3G_TRUE;
    }
    else {
        return M3G_FALSE;
    }
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const ObjectVFTable m3gvf_Appearance = {
    m3gAppearanceApplyAnimation,
    m3gObjectIsCompatible,
    m3gObjectUpdateProperty,
    m3gAppearanceDoGetReferences,
    m3gAppearanceFindID,
    m3gAppearanceDuplicate,
    m3gDestroyAppearance
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a new Appearance with default values
 *
 * \param hInterface            M3G interface
 * \retval Appearance new Appearance object
 * \retval NULL Appearance creating failed
 */
/*@access M3GInterface@*/
/*@access M3GAppearance@*/
M3G_API M3GAppearance m3gCreateAppearance(M3GInterface hInterface)
{
    Interface *m3g = (Interface *) hInterface;
    M3G_VALIDATE_INTERFACE(m3g);
    {
        Appearance *appearance = m3gAllocZ(m3g, sizeof(Appearance));

        if (appearance != NULL) {
    		m3gInitObject(&appearance->object, m3g, M3G_CLASS_APPEARANCE);
            m3gRegenerateSortKey(appearance);
        }
        
        return (M3GAppearance) appearance;
    }
}

/*!
 * \brief Get compositing mode
 *
 * \param hAppearance Appearance object
 * \return CompositingMode object
 */
M3G_API M3GCompositingMode m3gGetCompositingMode(M3GAppearance hAppearance)
{
    const Appearance *appearance = (const Appearance *) hAppearance;
    M3G_VALIDATE_OBJECT(appearance);
    return (M3GCompositingMode)(appearance->compositingMode);
}

/*!
 * \brief Get fog
 *
 * \param hAppearance Appearance object
 * \return Fog object
 */
M3G_API M3GFog m3gGetFog(M3GAppearance hAppearance)
{
    const Appearance *appearance = (const Appearance *) hAppearance;
    M3G_VALIDATE_OBJECT(appearance);
    return (M3GFog)(appearance->fog);
}

/*!
 * \brief Get material
 *
 * \param hAppearance Appearance object
 * \return Material object
 */
M3G_API M3GMaterial m3gGetMaterial(M3GAppearance hAppearance)
{
    const Appearance *appearance = (const Appearance *) hAppearance;
    M3G_VALIDATE_OBJECT(appearance);
    return (M3GMaterial)(appearance->material);
}

/*!
 * \brief Get polygon mode
 *
 * \param hAppearance Appearance object
 * \return PolygonMode object
 */
M3G_API M3GPolygonMode m3gGetPolygonMode(M3GAppearance hAppearance)
{
    const Appearance *appearance = (const Appearance *) hAppearance;
    M3G_VALIDATE_OBJECT(appearance);
    return (M3GPolygonMode)(appearance->polygonMode);
}

/*!
 * \brief Get texture
 *
 * \param hAppearance Appearance object
 * \param unit texturing unit
 * \return Texture2D object
 */
M3G_API M3GTexture m3gGetTexture(M3GAppearance hAppearance, M3Gint unit)
{
    const Appearance *appearance = (const Appearance *) hAppearance;
    M3G_VALIDATE_OBJECT(appearance);
    if (!m3gInRange(unit, 0, M3G_NUM_TEXTURE_UNITS - 1)) {
        m3gRaiseError(M3G_INTERFACE(appearance), M3G_INVALID_INDEX);
        return (M3GTexture) NULL;
    }
    return (M3GTexture)(appearance->texture[unit]);
}

/*!
 * \brief Get layer
 *
 * \param hAppearance Appearance object
 * \return layer number
 */
M3G_API M3Gint m3gGetLayer(M3GAppearance hAppearance)
{
    const Appearance *appearance = (const Appearance *) hAppearance;
    M3G_VALIDATE_OBJECT(appearance);
    return appearance->layer;
}

/*!
 * \brief Set compositing mode
 *
 * \param hAppearance Appearance object
 * \param hMode CompositingMode object
 */
M3G_API void m3gSetCompositingMode(M3GAppearance hAppearance,
                                   M3GCompositingMode hMode)
{
    Appearance *appearance = (Appearance *) hAppearance;
    CompositingMode *mode = (CompositingMode *) hMode;
    M3G_VALIDATE_OBJECT(appearance);
    
    M3G_ASSIGN_REF(appearance->compositingMode, mode);

    m3gRegenerateSortKey(appearance);
}

/*!
 * \brief Set polygon mode
 *
 * \param hAppearance Appearance object
 * \param hMode PolygonMode object
 */
M3G_API void m3gSetPolygonMode(M3GAppearance hAppearance,
                               M3GPolygonMode hMode)
{
    Appearance *appearance = (Appearance *) hAppearance;
    PolygonMode *mode = (PolygonMode *) hMode;
    M3G_VALIDATE_OBJECT(appearance);

    M3G_ASSIGN_REF(appearance->polygonMode, mode);

    m3gRegenerateSortKey(appearance);
}

/*!
 * \brief Set layer
 *
 * \param hAppearance Appearance object
 * \param layer layer number
 */
M3G_API void m3gSetLayer(M3GAppearance hAppearance, M3Gint layer)
{
    Appearance *appearance = (Appearance *) hAppearance;
    M3G_VALIDATE_OBJECT(appearance);

    /* Check for errors */
    if (!m3gInRange(layer, M3G_APPEARANCE_MIN_LAYER, M3G_APPEARANCE_MAX_LAYER)) {
        m3gRaiseError(M3G_INTERFACE(appearance), M3G_INVALID_INDEX);
        return;
    }

    appearance->layer = (M3Gshort) layer;
    
    m3gRegenerateSortKey(appearance);
}

/*!
 * \brief Set material
 *
 * \param hAppearance Appearance object
 * \param hMaterial Material object
 */
M3G_API void m3gSetMaterial(M3GAppearance hAppearance,
                            M3GMaterial hMaterial)
{
    Appearance *appearance = (Appearance *) hAppearance;
    Material *material = (Material *) hMaterial;
    M3G_VALIDATE_OBJECT(appearance);

    M3G_ASSIGN_REF(appearance->material, material);
    
    if (material != NULL) {
        appearance->vertexMask |= (M3Gushort)M3G_NORMAL_BIT;
    }
    else {
        appearance->vertexMask &= ~(M3Gushort)M3G_NORMAL_BIT;
    }

    m3gRegenerateSortKey(appearance);
}

/*!
 * \brief Set texture
 *
 * \param hAppearance Appearance object
 * \param unit texturing unit
 * \param hTexture Texture2D object
 */
M3G_API void m3gSetTexture(M3GAppearance hAppearance,
                           M3Gint unit, M3GTexture hTexture)
{
    Appearance *appearance = (Appearance *) hAppearance;
    Texture *texture = (Texture *) hTexture;
    M3G_VALIDATE_OBJECT(appearance);
    
    if (!m3gInRange(unit, 0, M3G_NUM_TEXTURE_UNITS - 1)) {
        m3gRaiseError(M3G_INTERFACE(appearance), M3G_INVALID_INDEX);
        return;
    }
    
    M3G_ASSIGN_REF(appearance->texture[unit], texture);
    
    if (texture != NULL) {
        appearance->vertexMask |= (M3Gushort) (M3G_TEXCOORD0_BIT << unit);
    }
    else {
        appearance->vertexMask &= (M3Gushort) ~(M3G_TEXCOORD0_BIT << unit);
    }

    m3gRegenerateSortKey(appearance);
}

/*!
 * \brief Set fog
 *
 * \param hAppearance Appearance object
 * \param hFog Fog object
 */
M3G_API void m3gSetFog(M3GAppearance hAppearance, M3GFog hFog)
{
    Appearance *appearance = (Appearance *) hAppearance;
    M3G_VALIDATE_OBJECT(appearance);
	
	M3G_ASSIGN_REF(appearance->fog, hFog);

    m3gRegenerateSortKey(appearance);
}

