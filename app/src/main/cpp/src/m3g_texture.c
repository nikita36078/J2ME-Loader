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
* Description: Texture2D implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Texture2D implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

/*
 * Uncomment this line to switch tracing on for this file's functions
 */
/* #define M3G_LOCAL_TRACEF_ON */

#include "m3g_object.h"

#include "m3g_image.h"
#include "m3g_math.h"
#include "m3g_texture.h"
#include "m3g_animationtrack.h"
#include "m3g_transformable.h"

/*!
 * \internal
 * \brief Texture object
 */
struct M3GTextureImpl
{
    Transformable transformable;

    Image *image;

    M3Guint blendColor;
    M3Genum blendFunc;

    M3Genum levelFilter;
    M3Genum imageFilter;

    M3Genum wrapS;
    M3Genum wrapT;
};

/*
 * Uncomment this line to switch tracing on for this file's functions
 */
/* #define M3G_LOCAL_TRACEF_ON */

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Destroys this Texture object.
 *
 * \param obj Texture object
 */
static void m3gDestroyTexture(Object *obj)
{
    Texture *tex = (Texture *) obj;
    M3G_VALIDATE_OBJECT(tex);

    M3G_ASSIGN_REF(tex->image, NULL);
    m3gDestroyTransformable(obj);
}

/*!
 * \internal
 * \brief Disables all texturing units and texture coordinate arrays
 */
static void m3gDisableTextures(void)
{
    M3Gint i;
    for (i = 0; i < M3G_NUM_TEXTURE_UNITS; ++i) {
        glClientActiveTexture(GL_TEXTURE0 + i);
        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        glActiveTexture(GL_TEXTURE0 + i);
        glDisable(GL_TEXTURE_2D);
    }
}

/*!
 * \internal
 * \brief Applies texture to OpenGL.
 *
 * \param texture Texture object
 */
static void m3gBindTexture(Texture *texture)
{
    M3Gfloat colors[4];
    M3Gint mode;

    M3G_VALIDATE_OBJECT(texture);
    
    m3gBindTextureImage(texture->image,
                        texture->levelFilter,
                        texture->imageFilter); 

    /* setting up texturing mode */
    {
        M3GMatrix mtx;
        M3Gfloat matrixValues[16];
        m3gGetCompositeTransform((Transformable *) texture, &mtx);
        m3gGetMatrixColumns(&mtx, matrixValues);
        glMatrixMode(GL_TEXTURE);
        glLoadMatrixf(matrixValues);
    }
    glMatrixMode(GL_MODELVIEW);

    mode = GL_REPLACE;
    switch (texture->blendFunc) {
    case M3G_FUNC_REPLACE:
        mode = GL_REPLACE;
        break;
    case M3G_FUNC_ADD:
        mode = GL_ADD;
        break;
    case M3G_FUNC_BLEND:
        mode = GL_BLEND;
        break;
    case M3G_FUNC_DECAL:
        mode = GL_DECAL;
        break;
    case M3G_FUNC_MODULATE:
        mode = GL_MODULATE;
        break;
    default:
        /* This should never happen */
        M3G_ASSERT(0);
        break;
    }
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, (GLfixed)mode);

    m3gFloatColor(texture->blendColor, 1.f, colors);
    glTexEnvfv(GL_TEXTURE_ENV, GL_TEXTURE_ENV_COLOR, colors);

    /* setting up wrapping */
    if (texture->wrapS  == M3G_WRAP_CLAMP) {
        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    }
    else {
        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    }
    if (texture->wrapT == M3G_WRAP_CLAMP) {
        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }
    else {
        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    }

    M3G_ASSERT_GL;
}

/*!
 * \internal
 * \brief Releases a bound texture from the current texture unit
 *
 */
static void m3gReleaseTexture(Texture *texture)
{
    m3gReleaseTextureImage(texture->image);
}

#if defined(M3G_NGL_TEXTURE_API)
/*!
 * \internal
 * \brief Make sure that mipmaps are allocated if needed
 */
static M3Gbool m3gValidateTextureMipmapping(Texture *texture)
{
    return (texture->levelFilter == M3G_FILTER_BASE_LEVEL
            || texture->image->mipData);
}
#endif

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param property      animation property
 * \retval M3G_TRUE     property supported
 * \retval M3G_FALSE    property not supported
 */
static M3Gbool m3gTextureIsCompatible(M3Gint property)
{
    switch (property) {
    case M3G_ANIM_COLOR:
        return M3G_TRUE;
    default:
        return m3gTransformableIsCompatible(property);
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self          Texture object
 * \param property      animation property
 * \param valueSize     size of value array
 * \param value         value array
 */
static void m3gTextureUpdateProperty(Object *self,
                                     M3Gint property,
                                     M3Gint valueSize,
                                     const M3Gfloat *value)
{
    Texture *texture = (Texture *)self;
    M3G_VALIDATE_OBJECT(texture);
    M3G_ASSERT_PTR(value);

    switch (property) {
    case M3G_ANIM_COLOR:
        M3G_ASSERT(valueSize >= 3);
        texture->blendColor =
            (valueSize == 3
             ? m3gColor3f(value[0], value[1], value[2])
             : m3gColor4f(value[0], value[1], value[2], value[3]));
        break;
    default:
        m3gTransformableUpdateProperty(self, property, valueSize, value);
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self Texture object
 * \param references array of reference objects
 * \return number of references
 */
static M3Gint m3gTextureDoGetReferences(Object *self, Object **references)
{
    Texture *texture = (Texture *)self;
    M3Gint num = m3gObjectDoGetReferences(self, references);
    if (texture->image != NULL) {
        if (references != NULL)
            references[num] = (Object *)texture->image;
        num++;
    }
    return num;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 */
static Object *m3gTextureFindID(Object *self, M3Gint userID)
{
    Texture *texture = (Texture *)self;
    Object *found = m3gObjectFindID(self, userID);
    
    if (!found && texture->image != NULL) {
        found = m3gFindID((Object*) texture->image, userID);
    }
    return found;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original Texture object
 * \param cloneObj pointer to cloned Texture object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gTextureDuplicate(const Object *originalObj,
                                   Object **cloneObj,
                                   Object **pairs,
                                   M3Gint *numPairs)
{
    Texture *original = (Texture *)originalObj;
    Texture *clone;
    M3G_ASSERT(*cloneObj == NULL); /* no derived classes */

    /* Create the clone object */
    
    clone = (Texture *)m3gCreateTexture(originalObj->interface,
                                        original->image);
    if (!clone) {
        return M3G_FALSE;
    }
    *cloneObj = (Object *)clone;

    /* Duplicate base class data */
    
    if (!m3gTransformableDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        return M3G_FALSE;
    }

    /* Duplicate our own data */
    
    clone->blendColor = original->blendColor;
    clone->blendFunc = original->blendFunc;
    clone->levelFilter = original->levelFilter;
    clone->imageFilter = original->imageFilter;
    clone->wrapS = original->wrapS;
    clone->wrapT = original->wrapT;

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Check texture dimensions.
 *
 * \retval M3G_TRUE dimensions valid
 * \retval M3G_FALSE dimensions invalid
 */
static M3Gbool m3gIsValidDimensions(M3Gint width, M3Gint height)
{
    return (       m3gInRange(width,  1, M3G_MAX_TEXTURE_DIMENSION)
                && m3gInRange(height, 1, M3G_MAX_TEXTURE_DIMENSION)
                && m3gIsPowerOfTwo(width)
                && m3gIsPowerOfTwo(height) );
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const ObjectVFTable m3gvf_Texture = {
    m3gObjectApplyAnimation,
    m3gTextureIsCompatible,
    m3gTextureUpdateProperty,
    m3gTextureDoGetReferences,
    m3gTextureFindID,
    m3gTextureDuplicate,
    m3gDestroyTexture
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Texture constructor, creates a texture with
 * default values.
 *
 *
 * \param interface             M3G interface
 * \param hImage                texture Image object
 * \retval Texture new Texture object
 * \retval NULL Texture creating failed
 */
M3G_API M3GTexture m3gCreateTexture(M3GInterface interface,
                                    M3GImage hImage)
{
    Interface *m3g = (Interface *) interface;
    Image* image = (Image *)hImage;
    M3G_VALIDATE_INTERFACE(m3g);

    /* Check inputs */
    if (image == NULL) {
        m3gRaiseError(m3g, M3G_NULL_POINTER);
        return NULL;
    }

    if (!m3gIsValidDimensions(image->width, image->height)) {
        m3gRaiseError(m3g, M3G_INVALID_VALUE);
        return NULL;
    }

    /* Allocate and initialize the object */
    {
        Texture *texture;
        texture = m3gAllocZ(m3g, sizeof(Texture));
        if (texture != NULL) {
            m3gInitTransformable(&texture->transformable, m3g,
                                 M3G_CLASS_TEXTURE);

            M3G_ASSIGN_REF(texture->image, image);
    
            texture->blendColor = 0x00000000;   /* Black */
            texture->blendFunc = M3G_FUNC_MODULATE;
            texture->levelFilter = M3G_FILTER_BASE_LEVEL;
            texture->imageFilter = M3G_FILTER_NEAREST;
            texture->wrapS = M3G_WRAP_REPEAT;
            texture->wrapT = M3G_WRAP_REPEAT;
        }

        return (M3GTexture)texture;
    }
}

/*!
 * \brief Set texture image.
 *
 * \param hTexture  Texture object
 * \param hImage    Image object
 */
M3G_API void m3gSetTextureImage(M3GTexture hTexture, M3GImage hImage)
{
    Texture *texture = (Texture*)hTexture;
    Image *image = (Image *)hImage;
    M3G_VALIDATE_OBJECT(texture);

    if (image == NULL) {
        m3gRaiseError(M3G_INTERFACE(texture), M3G_NULL_POINTER);
        return;
    }

    if (!m3gIsValidDimensions(image->width, image->height)) {
        m3gRaiseError(M3G_INTERFACE(texture), M3G_INVALID_VALUE);
        return;
    }

    M3G_ASSIGN_REF(texture->image, image);
}

/*!
 * \brief Get texture image.
 *
 * \param hTexture  Texture object
 * \return          Image object
 */
M3G_API M3GImage m3gGetTextureImage(M3GTexture hTexture)
{
    const Texture *texture = (const Texture *) hTexture;
    M3G_VALIDATE_OBJECT(texture);

    return (M3GImage)(texture->image);
}

/*!
 * \brief Set texture filtering.
 *
 * \param hTexture      Texture object
 * \param levelFilter   level filter type
 * \param imageFilter   image filter type
 */
M3G_API void m3gSetFiltering(M3GTexture hTexture,
                             M3Gint levelFilter,
                             M3Gint imageFilter)
{
    Texture *texture = (Texture*)hTexture;
    if ((levelFilter != M3G_FILTER_LINEAR &&
         levelFilter != M3G_FILTER_NEAREST &&
         levelFilter != M3G_FILTER_BASE_LEVEL)
        || (imageFilter != M3G_FILTER_LINEAR &&
            imageFilter != M3G_FILTER_NEAREST)) {
        m3gRaiseError(M3G_INTERFACE(texture), M3G_INVALID_VALUE);
        return;
    }
    texture->levelFilter = levelFilter;
    texture->imageFilter = imageFilter;
}

/*!
 * \brief Set texture S & T wrapping mode.
 *
 * \param hTexture  Texture object
 * \param wrapS     S wrap mode
 * \param wrapT     T wrap mode
 */
M3G_API void m3gSetWrapping(M3GTexture hTexture, M3Gint wrapS, M3Gint wrapT)
{
    Texture *texture = (Texture*)hTexture;
    if (wrapS != M3G_WRAP_CLAMP && wrapS != M3G_WRAP_REPEAT) {
        m3gRaiseError(M3G_INTERFACE(texture), M3G_INVALID_VALUE);
        return;
    }
    if (wrapT != M3G_WRAP_CLAMP && wrapT != M3G_WRAP_REPEAT) {
        m3gRaiseError(M3G_INTERFACE(texture), M3G_INVALID_VALUE);
        return;
    }
    texture->wrapS = wrapS;
    texture->wrapT = wrapT;
}

/*!
 * \brief Get texture S wrapping mode.
 *
 * \param hTexture  Texture object
 * \return S wrapping mode
 */
M3G_API M3Gint m3gGetWrappingS(M3GTexture hTexture)
{
    Texture *texture = (Texture*)hTexture;
    return texture->wrapS;
}

/*!
 * \brief Get texture T wrapping mode.
 *
 * \param hTexture  Texture object
 * \return T wrapping mode
 */
M3G_API M3Gint m3gGetWrappingT(M3GTexture hTexture)
{
    Texture *texture = (Texture*)hTexture;
    return texture->wrapT;
}

/*!
 * \brief Set texture blending function.
 *
 * \param hTexture  Texture object
 * \param func      blending function
 */
M3G_API void m3gTextureSetBlending(M3GTexture hTexture, M3Gint func)
{
    Texture *texture = (Texture*)hTexture;

    switch (func) {
    case M3G_FUNC_ADD:
    case M3G_FUNC_BLEND:
    case M3G_FUNC_DECAL:
    case M3G_FUNC_MODULATE:
    case M3G_FUNC_REPLACE:
        texture->blendFunc = func;
        break;
    default:
        m3gRaiseError(M3G_INTERFACE(texture), M3G_INVALID_VALUE);
        break;
    }
}

/*!
 * \brief Get texture blending function.
 *
 * \param hTexture  Texture object
 * \return          blending function
 */
M3G_API M3Gint m3gTextureGetBlending(M3GTexture hTexture)
{
    Texture *texture = (Texture*)hTexture;
    return texture->blendFunc;
}

/*!
 * \brief Set texture blend color as RGB.
 *
 * \param hTexture  Texture object
 * \param RGB       blend color as RGB
 */
M3G_API void m3gSetBlendColor(M3GTexture hTexture, M3Guint RGB)
{
    Texture *texture = (Texture*)hTexture;
    texture->blendColor = RGB & M3G_RGB_MASK;
}

/*!
 * \brief Get texture blend color as RGB.
 *
 * \param hTexture  Texture object
 * \return          blend color as RGB
 */
M3G_API M3Guint m3gGetBlendColor(M3GTexture hTexture)
{
    Texture *texture = (Texture*)hTexture;
    return texture->blendColor;
}

/*!
 * \brief Get texture filtering
 *
 * \param hTexture      Texture object
 * \param levelFilter   pointer to store level filter
 * \param imageFilter   pointer to store image filter
 */
M3G_API void m3gGetFiltering(M3GTexture hTexture, M3Gint *levelFilter, M3Gint *imageFilter)
{
    Texture *texture = (Texture*)hTexture;
    *levelFilter = texture->levelFilter;
    *imageFilter = texture->imageFilter;
}

/*
 * Uncomment these lines' opening pair at the begining of the file
 * if you want to switch tracing on for this file.
 */
#ifdef M3G_LOCAL_TRACEF_ON
#undef M3G_LOCAL_TRACEF_ON
#endif

