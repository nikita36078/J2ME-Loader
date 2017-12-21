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
* Description: Background implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Background implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_background.h"
#include "m3g_image.h"
#include "m3g_memory.h"
#include "m3g_animationtrack.h"
#include "m3g_rendercontext.h"

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Destroys this Background object.
 *
 * \param obj Background object
 */
static void m3gDestroyBackground(Object *obj)
{
    Background *background = (Background *) obj;
    M3G_VALIDATE_OBJECT(background);

    M3G_ASSIGN_REF(background->image, NULL);
    m3gDestroyObject(obj);
}

/*!
 * \internal
 * \brief Applies background color and image
 * using a textured quad.
 *
 * \param ctx           render context
 * \param background    Background object
 */
static void m3gApplyBackground(RenderContext *ctx, Background *background)
{
    GLbitfield glBits = 0;
    GLfixed temp[4];
    
    if (background->depthClearEnable) {
        glBits |= GL_DEPTH_BUFFER_BIT;
    }

    /* Skip color buffer clearing if the background image
     * fills the whole viewport. This is guaranteed to happen
     * if the crop rectangle is non-zero and both X and Y
     * wrapping modes are REPEAT. */
    
    if (background->colorClearEnable) {
        if (background->image == NULL ||
            background->crop.width == 0 ||
            background->crop.height == 0 ||
            background->modeX == M3G_BORDER ||
            background->modeY == M3G_BORDER)
        {
            glBits |= GL_COLOR_BUFFER_BIT;
            m3gGLColor(background->color, temp);
            glClearColorx(temp[0], temp[1], temp[2], temp[3]);
        }
    }
    
    /* Clear color and/or depth buffer (or neither) */
    glClear(glBits);

    /* Apply background image using a quad that
       fills the viewport */

    if (background->colorClearEnable &&
        background->image != NULL &&
        background->crop.width != 0 && 
        background->crop.height != 0)
    {
        {
            /* Texture coordinates */
            M3Gshort texvert[4 * 2];
            /* Quad that fills the viewport */
            M3Gint vert[4 * 3]   = { -65536,  65536, 0,
                                     -65536, -65536, 0,
                                      65536,  65536, 0,
                                      65536, -65536, 0 };
            Rect rImage, rIntersection;
            M3Gbool intersects;
            Image *imagePow2;

            /* Get power of two image */
            imagePow2 = m3gGetPowerOfTwoImage(background->image);
            /* If NULL -> out of memory */
            if (!imagePow2) {
                return;
            }

            rImage.x = 0;
            rImage.y = 0;
            rImage.width = m3gGetWidth(background->image);
            rImage.height = m3gGetHeight(background->image);

            /* Intersection of source image and crop rectangle */
            intersects = m3gIntersectRectangle(&rIntersection, &rImage, &background->crop);

            /* Setup X vertices and texture S coordinates */
            if (background->modeX == M3G_BORDER) {
                /* If both modes are border and no intersection ->
                   nothing to draw */
                if (background->modeY == M3G_BORDER && !intersects) {
                    return;
                }

                texvert[0 * 2 + 0] = (M3Gshort) rIntersection.x;
                texvert[1 * 2 + 0] = (M3Gshort) rIntersection.x;
                texvert[2 * 2 + 0] = (M3Gshort) (rIntersection.x + rIntersection.width);
                texvert[3 * 2 + 0] = (M3Gshort) (rIntersection.x + rIntersection.width);

                vert[0 * 3 + 0] = -65536 + 2 * 65536 * (rIntersection.x - background->crop.x) / background->crop.width;
                vert[1 * 3 + 0] = vert[0 * 3 + 0];
                vert[2 * 3 + 0] = vert[0 * 3 + 0] + 2 * 65536 * rIntersection.width / background->crop.width;
                vert[3 * 3 + 0] = vert[2 * 3 + 0];
            }
            else {
                /* In repeat mode texture coordinates are directly crop rectangle coordinates */
                texvert[0 * 2 + 0] = (M3Gshort) background->crop.x;
                texvert[1 * 2 + 0] = (M3Gshort) background->crop.x;
                texvert[2 * 2 + 0] = (M3Gshort) (background->crop.x + background->crop.width);
                texvert[3 * 2 + 0] = (M3Gshort) (background->crop.x + background->crop.width);
            }

            /* Setup Y vertices and texture T coordinates */
            if (background->modeY == M3G_BORDER) {
                texvert[0 * 2 + 1] = (M3Gshort) rIntersection.y;
                texvert[1 * 2 + 1] = (M3Gshort) (rIntersection.y + rIntersection.height);
                texvert[2 * 2 + 1] = (M3Gshort) rIntersection.y;
                texvert[3 * 2 + 1] = (M3Gshort) (rIntersection.y + rIntersection.height);


                vert[0 * 3 + 1] =  65536 - 2 * 65536 * (rIntersection.y - background->crop.y) / background->crop.height;
                vert[1 * 3 + 1] = vert[0 * 3 + 1] - 2 * 65536 * rIntersection.height / background->crop.height;
                vert[2 * 3 + 1] = vert[0 * 3 + 1];
                vert[3 * 3 + 1] = vert[1 * 3 + 1];
            }
            else {
                /* In repeat mode texture coordinates are directly crop rectangle coordinates */
                texvert[0 * 2 + 1] = (M3Gshort) background->crop.y;
                texvert[1 * 2 + 1] = (M3Gshort) (background->crop.y + background->crop.height);
                texvert[2 * 2 + 1] = (M3Gshort) background->crop.y;
                texvert[3 * 2 + 1] = (M3Gshort) (background->crop.y + background->crop.height);
            }

            /* Disable unwanted state and depth writes */
            m3gApplyAppearance(NULL, ctx, 0);
            glDepthMask(GL_FALSE);

            /* Disable color array, normals and textures*/
            glDisableClientState(GL_COLOR_ARRAY);
            glDisableClientState(GL_NORMAL_ARRAY);
        
            /* Background image to texture unit 0 */
            glClientActiveTexture(GL_TEXTURE0);
            glActiveTexture(GL_TEXTURE0);
            glEnableClientState(GL_TEXTURE_COORD_ARRAY);
            glTexCoordPointer(2, GL_SHORT, 0, texvert);
            glEnable(GL_TEXTURE_2D);
            glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, (GLfixed) GL_REPLACE);
            m3gBindTextureImage(imagePow2,
                                M3G_FILTER_BASE_LEVEL,
                                m3gIsAccelerated(ctx) ? M3G_FILTER_LINEAR : M3G_FILTER_NEAREST);

            /* Set wrapping */
            if (background->modeX == M3G_REPEAT) {
                glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            }
            else {
                glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            }
    
            if (background->modeY == M3G_REPEAT) {
                glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            }
            else {
                glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            }

            /* Texture matrix scale */
            glMatrixMode(GL_TEXTURE);
            glLoadIdentity();
            glScalef(   m3gRcp((M3Gfloat)m3gGetWidth(background->image)),
                        m3gRcp((M3Gfloat)m3gGetHeight(background->image)),
                        1.f);
            glMatrixMode(GL_MODELVIEW);

            /* Load vertices */
            glEnableClientState(GL_VERTEX_ARRAY);
            glVertexPointer(3, GL_FIXED, 0, vert);
        
            /* Set up an identity modelview and projection */
            m3gPushScreenSpace(ctx, M3G_FALSE);

            /* Load indices -> draws the background */
            M3G_BEGIN_PROFILE(M3G_INTERFACE(ctx), M3G_PROFILE_NGL_DRAW);
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
            M3G_END_PROFILE(M3G_INTERFACE(ctx), M3G_PROFILE_NGL_DRAW);
        
            m3gPopSpace(ctx);
            m3gReleaseTextureImage(imagePow2);
        }
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
static M3Gbool m3gBackgroundIsCompatible(M3Gint property)
{
    switch (property) {
    case M3G_ANIM_ALPHA:
    case M3G_ANIM_COLOR:
    case M3G_ANIM_CROP:
        return M3G_TRUE;
    default:
        return m3gObjectIsCompatible(property);
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self          Background object
 * \param property      animation property
 * \param valueSize     size of value array
 * \param value         value array
 */
static void m3gBackgroundUpdateProperty(Object *self,
                                        M3Gint property,
                                        M3Gint valueSize,
                                        const M3Gfloat *value)
{
    Background *background = (Background *)self;
    M3G_VALIDATE_OBJECT(background);
    M3G_ASSERT_PTR(value);
    
    switch (property) {
    case M3G_ANIM_ALPHA:
        M3G_ASSERT(valueSize >= 1);
        background->color = m3gAlpha1f(value[0])
            & (background->color | M3G_ALPHA_MASK);
        break;
    case M3G_ANIM_COLOR:
        M3G_ASSERT(valueSize >= 3);
        background->color = m3gColor3f(value[0], value[1], value[2])
            & (background->color | M3G_RGB_MASK);
        break;
    case M3G_ANIM_CROP:
        M3G_ASSERT(valueSize >= 2);
        background->crop.x = m3gRoundToInt(value[0]);
        background->crop.y = m3gRoundToInt(value[1]);
        if (valueSize > 2) {
            M3G_ASSERT(valueSize >= 4);
            background->crop.width =
                (value[2] < 0) ? 0 : m3gRoundToInt(value[2]);
            background->crop.height =
                (value[3] < 0) ? 0 : m3gRoundToInt(value[3]);
        }
        break;
    default:
        m3gObjectUpdateProperty(self, property, valueSize, value);
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self Background object
 * \param references array of reference objects
 * \return number of references
 */
static M3Gint m3gBackgroundDoGetReferences(Object *self, Object **references)
{
    Background *bg = (Background *)self;
    M3Gint num = m3gObjectDoGetReferences(self, references);
    if (bg->image != NULL) {
        if (references != NULL)
            references[num] = (Object *)bg->image;
        num++;
    }
    return num;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 */
static Object *m3gBackgroundFindID(Object *self, M3Gint userID)
{
    Background *bg = (Background *)self;
    Object *found = m3gObjectFindID(self, userID);

    if (!found && bg->image) {
        found = m3gFindID((Object*) bg->image, userID);
    }
    return found;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original Background object
 * \param cloneObj pointer to cloned Background object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gBackgroundDuplicate(const Object *originalObj,
                                      Object **cloneObj,
                                      Object **pairs,
                                      M3Gint *numPairs)
{
    Background *original = (Background *)originalObj;
    Background *clone = (Background *)m3gCreateBackground(originalObj->interface);
    *cloneObj = (Object *)clone;
    if (*cloneObj == NULL) {
        return M3G_FALSE;
    }

    if (m3gObjectDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        clone->color = original->color;
        clone->modeX = original->modeX;
        clone->modeY = original->modeY;
        clone->crop = original->crop;
        clone->colorClearEnable = original->colorClearEnable;
        clone->depthClearEnable = original->depthClearEnable;
        M3G_ASSIGN_REF(clone->image, original->image);
        return M3G_TRUE;
    }
    else {
        return M3G_FALSE;
    }
}

/*!
 * \internal
 * \brief Initializes a Background object. See specification
 * for default values.
 *
 * \param m3g           M3G interface
 * \param background    Background object
 */
static void m3gInitBackground(Interface *m3g, Background *background)
{
    /* Background is derived from Object */
    m3gInitObject(&background->object, m3g, M3G_CLASS_BACKGROUND);

    background->modeX = M3G_BORDER; 
    background->modeY = M3G_BORDER; 
    background->colorClearEnable = M3G_TRUE;
    background->depthClearEnable = M3G_TRUE;
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const ObjectVFTable m3gvf_Background = {
    m3gObjectApplyAnimation,
    m3gBackgroundIsCompatible,
    m3gBackgroundUpdateProperty,
    m3gBackgroundDoGetReferences,
    m3gBackgroundFindID,
    m3gBackgroundDuplicate,
    m3gDestroyBackground
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a Background object.
 *
 * \param interface     M3G interface
 * \retval Background new Background object
 * \retval NULL Background creating failed
 */

/*@access M3GInterface@*/
/*@access M3Gobject@*/
M3G_API M3GBackground m3gCreateBackground(M3GInterface interface)
{
    Interface *m3g = (Interface *) interface;
    M3G_VALIDATE_INTERFACE(m3g);

    {
        Background *background =  m3gAllocZ(m3g, sizeof(Background));
    
        if (background != NULL) {
            m3gInitBackground(m3g, background);
        }

        return (M3GBackground) background;
    }
}

/*!
 * \brief Sets background color.
 *
 * \param handle        Background object
 * \param ARGB          background color as ARGB
 */

/*@access M3Gobject@*/
M3G_API void m3gSetBgColor(M3GBackground handle, M3Guint ARGB)
{
    Background *background = (Background *) handle;
    M3G_VALIDATE_OBJECT(background);
    
    background->color = ARGB;
}

/*!
 * \brief Sets background image x and y mode.
 *
 * \param handle        Background object
 * \param modeX         Image X mode
 * \param modeY         Image Y mode
 */

/*@access M3Gobject@*/
M3G_API void m3gSetBgMode(M3GBackground handle, M3Gint modeX, M3Gint modeY)
{
    Background *background = (Background *) handle;
    M3G_VALIDATE_OBJECT(background);
    
    /* Check for errors */
    if (modeX < M3G_BORDER || modeX > M3G_REPEAT ||
        modeY < M3G_BORDER || modeY > M3G_REPEAT) {
        m3gRaiseError(M3G_INTERFACE(background), M3G_INVALID_VALUE);
        return;
    }

    background->modeX = modeX;
    background->modeY = modeY;
}

/*!
 * \brief Sets background image crop rectangle.
 *
 * \param handle        Background object
 * \param cropX         crop upper left x
 * \param cropY         crop upper left y
 * \param width         crop width
 * \param height        crop height
 */

/*@access M3Gobject@*/
M3G_API void m3gSetBgCrop(M3GBackground handle,
                          M3Gint cropX, M3Gint cropY,
                          M3Gint width, M3Gint height)
{
    Background *background = (Background *) handle;
    M3G_VALIDATE_OBJECT(background);

    /* Check for errors */
    if (width < 0 || height < 0) {
        m3gRaiseError(M3G_INTERFACE(background), M3G_INVALID_VALUE);
        return;
    }

    background->crop.x = cropX;
    background->crop.y = cropY;
    background->crop.width = width;
    background->crop.height = height;
}

/*!
 * \brief Sets background image.
 *
 * \param handle        Background object
 * \param hImage        Image2D object or NULL
 */

/*@access M3Gobject@*/
M3G_API void m3gSetBgImage(M3GBackground handle, M3GImage hImage)
{
    Background *background = (Background *) handle;
    Image *image = (Image *)hImage;
    M3G_VALIDATE_OBJECT(background);

    if (image != NULL) {
        /* Check allowed formats */
        if (m3gGetFormat(image) != M3G_RGB &&
            m3gGetFormat(image) != M3G_RGBA) {
            m3gRaiseError(M3G_INTERFACE(background), M3G_INVALID_VALUE);
            return;
        }

        background->crop.x = 0;
        background->crop.y = 0;
        background->crop.width = m3gGetWidth(image);
        background->crop.height = m3gGetHeight(image);
    }

    M3G_ASSIGN_REF(background->image, image);
}

/*!
 * \brief Gets background image.
 *
 * \param handle        Background object
 * \return              Image2D object or NULL
 */

/*@access M3GObject@*/
M3G_API M3GImage m3gGetBgImage(M3GBackground handle)
{
    Background *bg = (Background *) handle;
    M3G_VALIDATE_OBJECT(bg);

    return (M3GImage) bg->image;
}

/*!
 * \brief Gets background color as ARGB.
 *
 * \param handle        Background object
 * \return              ARGB color
 */

/*@access M3Gobject@*/
M3G_API M3Guint m3gGetBgColor(M3GBackground handle)
{
    Background *background = (Background *) handle;
    M3G_VALIDATE_OBJECT(background);

    return background->color;
}

/*!
 * \brief Gets background image x or y mode.
 *
 * \param handle        Background object
 * \param which         which mode to return
 *                      \arg M3G_GET_MODEX
 *                      \arg M3G_GET_MODEY
 * \return              image x or y mode
 */

/*@access M3Gobject@*/
M3G_API M3Gint m3gGetBgMode(M3GBackground handle, M3Gint which)
{
    Background *background = (Background *) handle;
    M3G_VALIDATE_OBJECT(background);

    switch(which) {
        case M3G_GET_MODEX:
            return background->modeX;
        case M3G_GET_MODEY:
        default:
            return background->modeY;
    }
}

/*!
 * \brief Gets background image crop parameter.
 *
 * \param handle        Background object
 * \param which         which crop parameter to return
 *                      \arg M3G_GET_CROPX
 *                      \arg M3G_GET_CROPY
 *                      \arg M3G_GET_CROPWIDTH
 *                      \arg M3G_GET_CROPHEIGHT
 * \return              image crop parameter
 */

/*@access M3Gobject@*/
M3G_API M3Gint m3gGetBgCrop(M3GBackground handle, M3Gint which)
{
    Background *background = (Background *) handle;
    M3G_VALIDATE_OBJECT(background);

    switch(which) {
        case M3G_GET_CROPX:
            return background->crop.x;
        case M3G_GET_CROPY:
            return background->crop.y;
        case M3G_GET_CROPWIDTH:
            return background->crop.width;
        case M3G_GET_CROPHEIGHT:
        default:
            return background->crop.height;
    }
}

/*!
 * \brief Sets background color or depth clear enable.
 *
 * \param handle        Background object
 * \param which         which clear to enable
 *                      \arg M3G_SETGET_COLORCLEAR
 *                      \arg M3G_SETGET_DEPTHCLEAR
 * \param enable        clear enable/disable
 */

/*@access M3Gobject@*/
M3G_API void m3gSetBgEnable(M3GBackground handle, M3Gint which, M3Gbool enable)
{
    Background *background = (Background *) handle;
    M3G_VALIDATE_OBJECT(background);

    switch(which) {
        case M3G_SETGET_COLORCLEAR:
            background->colorClearEnable = enable;
            break;
        case M3G_SETGET_DEPTHCLEAR:
        default:
            background->depthClearEnable = enable;
            break;
    }
}

/*!
 * \brief Gets background color or depth clear enable.
 *
 * \param handle        Background object
 * \param which         which clear to return
 *                      \arg M3G_SETGET_COLORCLEAR
 *                      \arg M3G_SETGET_DEPTHCLEAR
 * \return              clear enabled
 */

/*@access M3Gobject@*/
M3G_API M3Gbool m3gIsBgEnabled(M3GBackground handle, M3Gint which)
{
    Background *background = (Background *) handle;
    M3G_VALIDATE_OBJECT(background);

    switch(which) {
        case M3G_SETGET_COLORCLEAR:
            return background->colorClearEnable;
        case M3G_SETGET_DEPTHCLEAR:
        default:
            return background->depthClearEnable;
    }
}

