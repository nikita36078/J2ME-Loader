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
* Description: Sprite implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Sprite implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

/*#include <stdio.h>*/

#include "m3g_sprite.h"
#include "m3g_appearance.h"
#include "m3g_camera.h"
#include "m3g_rendercontext.h"
#include "m3g_renderqueue.h"

#define FLIPX   1
#define FLIPY   2


/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Destroys this Sprite object.
 *
 * \param obj Sprite object
 */
static void m3gDestroySprite(Object *obj)
{
    Sprite *sprite = (Sprite *) obj;
    M3G_VALIDATE_OBJECT(sprite);

    M3G_ASSIGN_REF(sprite->image, NULL);
    M3G_ASSIGN_REF(sprite->appearance, NULL);

    m3gIncStat(M3G_INTERFACE(obj), M3G_STAT_RENDERABLES, -1);
    
    m3gDestroyNode(obj);
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param property      animation property
 * \retval M3G_TRUE     property supported
 * \retval M3G_FALSE    property not supported
 */
static M3Gbool m3gSpriteIsCompatible(M3Gint property)
{
    switch (property) {
    case M3G_ANIM_CROP:
        return M3G_TRUE;
    default:
        return m3gNodeIsCompatible(property);
    }
}

/*!
 * \internal
 * \brief Overloaded Node method
 */
static M3Gint m3gSpriteGetBBox(Node *self, AABB *bbox)
{
    Sprite *sprite = (Sprite*) self;

    /* Only scaled sprites can have a bounding box; non-scaled ones
     * are marked as non-cullable in the "SetParent" function in
     * m3g_node.c */
    
    if (sprite->scaled) {
        const AABB spriteBBox = { { -.5f, -.5f,  0.f },
                                  {  .5f,  .5f,  0.f } };
        *bbox = spriteBBox;
        return (4 * VFC_VERTEX_COST +
                2 * VFC_TRIANGLE_COST +
                VFC_NODE_OVERHEAD);
    }
    else {
        return 0; /* no bounding box for non-scaled sprites */
    }
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self          Sprite object
 * \param property      animation property
 * \param valueSize     size of value array
 * \param value         value array
 */
static void m3gSpriteUpdateProperty(Object *self,
                                    M3Gint property,
                                    M3Gint valueSize,
                                    const M3Gfloat *value)
{
    Sprite *sprite = (Sprite *) self;
    M3G_VALIDATE_OBJECT(sprite);
    M3G_ASSERT_PTR(value);

    switch (property) {
    case M3G_ANIM_CROP:
        /* Assert that the value vector is large enough */
        if (valueSize > 2) {
            M3G_ASSERT(valueSize >= 4);
            m3gSetCrop(sprite,  m3gRoundToInt(value[0]),
                       m3gRoundToInt(value[1]),
                       m3gClampInt(m3gRoundToInt(value[2]),
                                   -M3G_MAX_TEXTURE_DIMENSION,
                                   M3G_MAX_TEXTURE_DIMENSION),
                       m3gClampInt(m3gRoundToInt(value[3]),
                                   -M3G_MAX_TEXTURE_DIMENSION,
                                   M3G_MAX_TEXTURE_DIMENSION) );
        }
        else {
            M3G_ASSERT(valueSize >= 2);
            m3gSetCrop(sprite,  m3gRoundToInt(value[0]),
                       m3gRoundToInt(value[1]),
                       sprite->crop.width,
                       sprite->crop.height );
        }
        break;
    default:
        m3gNodeUpdateProperty(self, property, valueSize, value);
    }
}

/*!
 * \internal
 * \brief Overloaded Node method.
 *
 * \param self Sprite object
 * \param toCamera transform to camera
 * \param alphaFactor total alpha factor
 * \param caller caller node
 * \param renderQueue RenderQueue
 *
 * \retval M3G_TRUE continue render setup
 * \retval M3G_FALSE abort render setup
 */
static M3Gbool m3gSpriteSetupRender(Node *self,
                                    const Node *caller,
                                    SetupRenderState *s,
                                    RenderQueue *renderQueue)
{
    Sprite *sprite = (Sprite *)self;
    Interface *m3g = M3G_INTERFACE(sprite);
    M3G_UNREF(caller);
    m3gIncStat(M3G_INTERFACE(self), M3G_STAT_RENDER_NODES, 1);

    if ((self->enableBits & NODE_RENDER_BIT) != 0 &&
        (self->scope & renderQueue->scope) != 0) {
        
        if (sprite->appearance != NULL && sprite->image != NULL &&
            sprite->crop.width != 0 && sprite->crop.height != 0) {

            /* Fetch the cumulative alpha factor for this node */
            sprite->totalAlphaFactor =
                (M3Gushort) m3gGetTotalAlphaFactor((Node*) sprite, renderQueue->root);

            /* Touch the POT image to make sure it's allocated prior
             * to rendering */
            
            if (!m3gGetPowerOfTwoImage(sprite->image) ||
                !m3gInsertDrawable(m3g,
                                   renderQueue,
                                   self,
                                   &s->toCamera,
                                   0,
                                   m3gGetAppearanceSortKey(sprite->appearance)))
                return M3G_FALSE;
        }
    }

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Calculates sprite vertex positions and texture coordinates.
 *
 * \param sprite        Sprite object
 * \param ctx           RenderContext object (Graphics3D)
 * \param cam           Camera object
 * \param vert          vertex position to fill in
 * \param texvert       texture coordinates to fill in
 * \param eyeSpace      coordinates after modelview
 * \param adjust        adjust for texture coorinates, render and
 *                      pick need different adjustment
 * \retval M3G_TRUE     crop and image intersect
 * \retval M3G_FALSE    crop and image do not intersect
 */
static M3Gbool m3gGetSpriteCoordinates(Sprite *sprite,
                                       RenderContext *ctx,
                                       const Camera *cam,
                                       const Matrix *toCamera,
                                       M3Gint *vert,
                                       M3Gshort *texvert,
                                       Vec4 *eyeSpace,
                                       M3Gshort adjust)
{
    Vec4 o = {0, 0, 0, 1};      /* Origin */
    Vec4 x = {0.5f, 0, 0, 1};   /* Half of x unit */
    Vec4 y = {0, 0.5f, 0, 1};   /* Half of y unit */
    Vec4 ot;
    Rect rIsect, rImage;

    rImage.x = 0;
    rImage.y = 0;
    rImage.width = sprite->width;
    rImage.height = sprite->height;

    /* Intersection of image and crop*/
    if (!m3gIntersectRectangle(&rIsect, &rImage, &sprite->crop)) {
        /* No intersection -> nothing to render / pick */
        return M3G_FALSE;
    }

    /* Calculate origin and vectors after modelview */
    m3gTransformVec4(toCamera, &o);
    m3gTransformVec4(toCamera, &x);
    m3gTransformVec4(toCamera, &y);

    ot = o;

    m3gScaleVec4(&o, m3gRcp(o.w));
    m3gScaleVec4(&x, m3gRcp(x.w));
    m3gScaleVec4(&y, m3gRcp(y.w));

    /* Store eyespace coordinates */
    if (eyeSpace != NULL) {
        eyeSpace->x = o.x;
        eyeSpace->y = o.y;
        eyeSpace->z = o.z;
    }

    m3gSubVec4(&x, &o);
    m3gSubVec4(&y, &o);

    x.x = m3gAdd(ot.x, m3gLengthVec3((const Vec3*) &x));
    x.y = ot.y;
    x.z = ot.z;
    x.w = ot.w;

    y.y = m3gAdd(ot.y, m3gLengthVec3((const Vec3*) &y));
    y.x = ot.x;
    y.z = ot.z;
    y.w = ot.w;

    /* Calculate origin and vectors after projection */
    {
        const Matrix *projMatrix = m3gProjectionMatrix(cam);
        m3gTransformVec4(projMatrix, &ot);
        m3gTransformVec4(projMatrix, &x);
        m3gTransformVec4(projMatrix, &y);
    }
#ifndef M3G_USE_NGL_API
    /* Store w after projection */
    if (eyeSpace != NULL) {
        eyeSpace->w = ot.w;
    }
#endif
    m3gScaleVec4(&ot, m3gRcp(ot.w));
    m3gScaleVec4(&x, m3gRcp(x.w));
    m3gScaleVec4(&y, m3gRcp(y.w));

    m3gSubVec4(&x, &ot);
    m3gSubVec4(&y, &ot);

    x.x = m3gLengthVec3((const Vec3*) &x);
    y.y = m3gLengthVec3((const Vec3*) &y);

    /* Non-scaled sprites take width from crop rectangle*/
    if (!sprite->scaled) {
        M3Gint viewport[4];
        if (ctx != NULL) {
            m3gGetViewport(ctx, viewport, viewport + 1, viewport + 2, viewport + 3);
        }
        else {
            /* Use a dummy viewport, this is only when picking and
               not rendering to anything. Values must represent a valid viewport */
            viewport[0] = 0;
            viewport[1] = 0;
            viewport[2] = 256;
            viewport[3] = 256;
        }

        x.x = m3gDivif (rIsect.width, viewport[2]);
        y.y = m3gDivif (rIsect.height, viewport[3]);

        ot.x = m3gSub(ot.x,
                      m3gDivif (2 * sprite->crop.x + sprite->crop.width - 2 * rIsect.x - rIsect.width,
                                viewport[2]));

        ot.y = m3gAdd(ot.y,
                      m3gDivif (2 * sprite->crop.y + sprite->crop.height - 2 * rIsect.y - rIsect.height,
                                viewport[3]));
    }
    else {
        /* Adjust width and height according to cropping rectangle */
        x.x = m3gDiv(x.x, (M3Gfloat) sprite->crop.width);
        y.y = m3gDiv(y.y, (M3Gfloat) sprite->crop.height);

        ot.x = m3gSub(ot.x,
                      m3gMul((M3Gfloat)(2 * sprite->crop.x + sprite->crop.width - 2 * rIsect.x - rIsect.width),
                             x.x));

        ot.y = m3gAdd(ot.y,
                      m3gMul((M3Gfloat)(2 * sprite->crop.y + sprite->crop.height - 2 * rIsect.y - rIsect.height),
                             y.y));

        x.x = m3gMul(x.x, (M3Gfloat) rIsect.width);
        y.y = m3gMul(y.y, (M3Gfloat) rIsect.height);
    }
#ifdef M3G_USE_NGL_API
    /* Store final Z */
    if (eyeSpace != NULL) {
        eyeSpace->w = ot.z;
    }
#endif
    /* Set up positions */
    vert[0 * 3 + 0] = (M3Gint) m3gMul(65536, m3gSub(ot.x, x.x));
    vert[0 * 3 + 1] = m3gRoundToInt(m3gAdd(m3gMul(65536, m3gAdd(ot.y, y.y)), 0.5f));
    vert[0 * 3 + 2] = m3gRoundToInt(m3gMul(65536, ot.z));

    vert[1 * 3 + 0] = vert[0 * 3 + 0];
    vert[1 * 3 + 1] = (M3Gint) m3gMul(65536, m3gSub(ot.y, y.y));
    vert[1 * 3 + 2] = vert[0 * 3 + 2];

    vert[2 * 3 + 0] = m3gRoundToInt(m3gAdd(m3gMul(65536, m3gAdd(ot.x, x.x)), 0.5f));
    vert[2 * 3 + 1] = vert[0 * 3 + 1];
    vert[2 * 3 + 2] = vert[0 * 3 + 2];

    vert[3 * 3 + 0] = vert[2 * 3 + 0];
    vert[3 * 3 + 1] = vert[1 * 3 + 1];
    vert[3 * 3 + 2] = vert[0 * 3 + 2];

    /* Set up texture coordinates */
    if (!(sprite->flip & FLIPX)) {
        texvert[0 * 2 + 0] = (M3Gshort) rIsect.x;
        texvert[1 * 2 + 0] = (M3Gshort) rIsect.x;
        texvert[2 * 2 + 0] = (M3Gshort) (rIsect.x + rIsect.width - adjust);
        texvert[3 * 2 + 0] = (M3Gshort) (rIsect.x + rIsect.width - adjust);
    }
    else {
        texvert[0 * 2 + 0] = (M3Gshort) (rIsect.x + rIsect.width - adjust);
        texvert[1 * 2 + 0] = (M3Gshort) (rIsect.x + rIsect.width - adjust);
        texvert[2 * 2 + 0] = (M3Gshort) rIsect.x;
        texvert[3 * 2 + 0] = (M3Gshort) rIsect.x;
    }

    if (!(sprite->flip & FLIPY)) {
        texvert[0 * 2 + 1] = (M3Gshort) rIsect.y;
        texvert[1 * 2 + 1] = (M3Gshort) (rIsect.y + rIsect.height - adjust);
        texvert[2 * 2 + 1] = (M3Gshort) rIsect.y;
        texvert[3 * 2 + 1] = (M3Gshort) (rIsect.y + rIsect.height - adjust);
    }
    else {
        texvert[0 * 2 + 1] = (M3Gshort) (rIsect.y + rIsect.height - adjust);
        texvert[1 * 2 + 1] = (M3Gshort) rIsect.y;
        texvert[2 * 2 + 1] = (M3Gshort) (rIsect.y + rIsect.height - adjust);
        texvert[3 * 2 + 1] = (M3Gshort) rIsect.y;
    }

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Overloaded Node method.
 *
 * Renders the sprite as a textured quad.
 *
 * \param self Mesh object
 * \param ctx current render context
 * \param patchIndex submesh index
 */
static void m3gSpriteDoRender(Node *self,
                              RenderContext *ctx,
                              const Matrix *toCamera,
                              M3Gint patchIndex)
{
    Sprite *sprite = (Sprite *)self;
    M3Gshort texvert[4 * 2];
    M3Gint vert[4 * 3];
    Vec4 eyeSpace;
    Image *imagePow2;
    M3G_UNREF(patchIndex);

    M3G_BEGIN_PROFILE(M3G_INTERFACE(self), M3G_PROFILE_SETUP_TRANSFORMS);
    if (!m3gGetSpriteCoordinates(sprite,
                                 ctx,
                                 m3gGetCurrentCamera(ctx),
                                 toCamera,
                                 vert,
                                 texvert,
                                 &eyeSpace,
                                 0)) {
        return;
    }
    M3G_END_PROFILE(M3G_INTERFACE(self), M3G_PROFILE_SETUP_TRANSFORMS);

    /* Get power of two image */
    imagePow2 = m3gGetPowerOfTwoImage(sprite->image);
    /* If NULL -> out of memory */
    if (imagePow2 == NULL) {
        return;
    }

    if (m3gGetColorMaskWorkaround(M3G_INTERFACE(ctx))) {
        m3gUpdateColorMaskStatus(ctx,
                                 m3gColorMask(sprite->appearance),
                                 m3gAlphaMask(sprite->appearance));
    }

    /* Disable unwanted state. Note that we do this BEFORE setting the
     * sprite color to avoid any problems with glColorMaterial  */
    m3gApplyDefaultMaterial();
    m3gApplyDefaultPolygonMode();

    /* Disable color array, normals and textures*/
    glDisableClientState(GL_COLOR_ARRAY);
    glDisableClientState(GL_NORMAL_ARRAY);
    m3gDisableTextures();

    /* Sprite image to texture unit 0 */
    glClientActiveTexture(GL_TEXTURE0);
    glActiveTexture(GL_TEXTURE0);
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    glTexCoordPointer(2, GL_SHORT, 0, texvert);
    glEnable(GL_TEXTURE_2D);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, (GLfixed) GL_MODULATE);
    m3gBindTextureImage(imagePow2,
                        M3G_FILTER_BASE_LEVEL,
                        m3gIsAccelerated(ctx) ? M3G_FILTER_LINEAR : M3G_FILTER_NEAREST);

    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    
    glMatrixMode(GL_TEXTURE);
    glLoadIdentity();
    glScalef(m3gRcp((M3Gfloat) m3gGetWidth(sprite->image)),
             m3gRcp((M3Gfloat) m3gGetHeight(sprite->image)),
             1.f);
    glMatrixMode(GL_MODELVIEW);

    /* Apply fog and compositing mode */
#ifdef M3G_USE_NGL_API
    m3gApplySpriteFog(sprite->appearance->fog, eyeSpace.z, eyeSpace.w);
#else
    m3gApplyFog(sprite->appearance->fog);
#endif
    m3gApplyCompositingMode(sprite->appearance->compositingMode, ctx);

    {
        GLfixed a = (GLfixed) (0xff * sprite->totalAlphaFactor);
        a = (a >> (NODE_ALPHA_FACTOR_BITS - 8))
            + (a >> NODE_ALPHA_FACTOR_BITS)
            + (a >> (NODE_ALPHA_FACTOR_BITS + 7));
        glColor4x((GLfixed) 1 << 16, (GLfixed) 1 << 16, (GLfixed) 1 << 16, a);
    }

    /* Load vertices */
    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(3, GL_FIXED, 0, vert);

    /* Store current matrices, then set up an identity modelview and
     * projection */

    m3gPushScreenSpace(ctx, M3G_FALSE);

#ifndef M3G_USE_NGL_API
    /* Transform the sprite vertices (in NDC) back to eye coordinates, so that 
       the fog distance will be calculated correctly in the OpenGL pipeline. */
    {
        GLfloat transform[16];
        GLfloat scaleW[16] = { 0.f, 0.f, 0.f, 0.f,
                               0.f, 0.f, 0.f, 0.f,
                               0.f, 0.f, 0.f, 0.f,
                               0.f, 0.f, 0.f, 0.f };
        Matrix invProjMatrix;
        const Matrix *projMatrix = m3gProjectionMatrix(m3gGetCurrentCamera(ctx));

        m3gMatrixInverse(&invProjMatrix, projMatrix);
		m3gGetMatrixColumns(&invProjMatrix, transform);
        
        glMatrixMode(GL_MODELVIEW);
        glMultMatrixf(transform);
        scaleW[0] = scaleW[5] = scaleW[10] = scaleW[15] = eyeSpace.w;
        glMultMatrixf(scaleW);

        glMatrixMode(GL_PROJECTION);
        m3gGetMatrixColumns(projMatrix, transform);
        glLoadMatrixf(transform);
    }
#endif

    /* Load indices -> draws the sprite */
    M3G_BEGIN_PROFILE(M3G_INTERFACE(ctx), M3G_PROFILE_NGL_DRAW);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    M3G_END_PROFILE(M3G_INTERFACE(ctx), M3G_PROFILE_NGL_DRAW);

    m3gReleaseTextureImage(imagePow2);
    
    /* Restore the previous modelview and projection */

    m3gPopSpace(ctx);
}

/*!
 * \internal
 * \brief Overloaded Node method.
 *
 * Picks a scaled sprite as 2D from viewport.
 *
 * \param self      Mesh object
 * \param mask      pick scope mask
 * \param ray       pick ray
 * \param ri        RayIntersection object
 * \param toGroup   transform to originating group
 * \retval          M3G_TRUE    continue pick
 * \retval          M3G_FALSE   abort pick
 */
static M3Gbool m3gSpriteRayIntersect(Node *self,
                                     M3Gint mask,
                                     M3Gfloat *ray,
                                     RayIntersection *ri,
                                     Matrix *toGroup)
{
    Sprite *sprite = (Sprite *)self;
    M3Gshort texvert[4 * 2];
    M3Gint vert[4 * 3];
    M3Gint x, y;
    Vec4 eyeSpace;
    M3Gfloat distance;
    M3G_UNREF(toGroup);

    /* Check that picking is possible */
    
    if (sprite->image == NULL ||
        sprite->appearance == NULL ||
        ri->camera == NULL ||
        !sprite->scaled ||
        sprite->crop.width == 0 ||
        sprite->crop.height == 0 ||
        (self->scope & mask) == 0) {
        return M3G_TRUE;
    }

    /* Calculate modelview transform, picking is possible without rendering */
    
    {
        Matrix toCamera;
        
        if (!m3gGetTransformTo(self, (Node *)ri->camera,
                               &toCamera)) {
            return M3G_FALSE;
        }
        if (!m3gGetSpriteCoordinates(sprite, NULL,
                                     (const Camera *)ri->camera, &toCamera,
                                     vert, texvert, &eyeSpace, 1)) {
            return M3G_TRUE;
        }
    }

    /* Do the pick in 2D, formula is from the spec and values are
       set to 16.16 fixed point format */
    
    x = m3gRoundToInt(m3gMul(2 * 65536, ri->x)) - 65536;
    y = 65536 - m3gRoundToInt(m3gMul(2 * 65536, ri->y));

    if (x >= vert[0 * 3 + 0] && x <= vert[2 * 3 + 0] &&
        y <= vert[0 * 3 + 1] && y >= vert[1 * 3 + 1] ) {

        distance = m3gDiv(m3gSub(eyeSpace.z, ray[6]), m3gSub(ray[7], ray[6]));

        if (distance <= 0 ||
            distance >= ri->tMin) return M3G_TRUE;

        ri->tMin = distance;
        ri->distance = ri->tMin;
        ri->submeshIndex = 0;

        x -= vert[0 * 3 + 0];
        y  = vert[0 * 3 + 1] - y;

        if (!(sprite->flip & FLIPX)) {
            ri->textureS[0] = m3gAdd(texvert[0 * 2 + 0],
                                     m3gDivif ((texvert[2 * 2 + 0] - texvert[0 * 2 + 0] + 1) * x,
                                               vert[2 * 3 + 0] - vert[0 * 3 + 0]));
        }
        else {
            ri->textureS[0] = m3gSub((M3Gfloat)(texvert[0 * 2 + 0] + 1),
                                     m3gDivif ((texvert[0 * 2 + 0] - texvert[2 * 2 + 0] + 1) * x,
                                               vert[2 * 3 + 0] - vert[0 * 3 + 0]));
        }

        if (!(sprite->flip & FLIPY)) {
            ri->textureT[0] = m3gAdd(texvert[0 * 2 + 1],
                                     m3gDivif ((texvert[1 * 2 + 1] - texvert[0 * 2 + 1] + 1) * y,
                                               vert[0 * 3 + 1] - vert[1 * 3 + 1]));
        }
        else {
            ri->textureT[0] = m3gSub((M3Gfloat)(texvert[0 * 2 + 1] + 1),
                                     m3gDivif ((texvert[0 * 2 + 1] - texvert[1 * 2 + 1] + 1) * y,
                                               vert[0 * 3 + 1] - vert[1 * 3 + 1]));
        }

        {
            /* Finally check against alpha */
            M3Gint threshold = 0, alpha;

            if (sprite->appearance->compositingMode) {
                threshold = (M3Gint)m3gMul(m3gGetAlphaThreshold(sprite->appearance->compositingMode), 256);
            }

            alpha = m3gGetAlpha(sprite->image, (M3Gint)ri->textureS[0], (M3Gint)ri->textureT[0]);

            if (alpha >= threshold) {
                /* Normalize texture coordinates */
                ri->textureS[0] = m3gDiv(ri->textureS[0], (M3Gfloat) sprite->width);
                ri->textureT[0] = m3gDiv(ri->textureT[0], (M3Gfloat) sprite->height);

                ri->textureS[1] = 0.f;
                ri->textureT[1] = 0.f;

                ri->normal[0] = 0.f;
                ri->normal[1] = 0.f;
                ri->normal[2] = 1.f;

                ri->intersected = self;
            }
        }
    }

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self Sprite object
 * \param references array of reference objects
 * \return number of references
 */
static M3Gint m3gSpriteDoGetReferences(Object *self, Object **references)
{
    Sprite *sprite = (Sprite *)self;
    int num = m3gObjectDoGetReferences(self, references);
    if (sprite->image != NULL) {
        if (references != NULL)
            references[num] = (Object *)sprite->image;
        num++;
    }
    if (sprite->appearance != NULL) {
        if (references != NULL)
            references[num] = (Object *)sprite->appearance;
        num++;
    }
    return num;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 */
static Object *m3gSpriteFindID(Object *self, M3Gint userID)
{
    Sprite *sprite = (Sprite *)self;
    Object *found = m3gObjectFindID(self, userID);
        
    if (!found && sprite->image != NULL) {
        found = m3gFindID((Object*) sprite->image, userID);
    }
    if (!found && sprite->appearance != NULL) {
        found = m3gFindID((Object*) sprite->appearance, userID);
    }
    return found;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original Sprite object
 * \param cloneObj pointer to cloned Sprite object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gSpriteDuplicate(const Object *originalObj,
                                  Object **cloneObj,
                                  Object **pairs,
                                  M3Gint *numPairs)
{
    Sprite *original = (Sprite *)originalObj;
    Sprite *clone;
    M3G_ASSERT(*cloneObj == NULL); /* no derived classes */

    /* Create the clone object */
    
    clone = (Sprite *)m3gCreateSprite(originalObj->interface,
                                      original->scaled,
                                      original->image,
                                      original->appearance);
    if (!clone) {
        return M3G_FALSE;
    }
    *cloneObj = (Object *)clone;

    /* Duplicate our own fields */
    
    clone->crop = original->crop;
    clone->flip = original->flip;
    
    /* Duplicate base class data */
    
    return m3gNodeDuplicate(originalObj, cloneObj, pairs, numPairs);
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self Sprite object
 * \param time current world time
 * \return minimum validity
 */
static M3Gint m3gSpriteApplyAnimation(Object *self, M3Gint time)
{
    M3Gint validity, minValidity;
    Sprite *sprite = (Sprite *)self;
    Object *app;
    M3G_VALIDATE_OBJECT(sprite);

    minValidity = m3gObjectApplyAnimation(self, time);

    if (minValidity > 0) {
        app = (Object *) sprite->appearance;
        
        if (app != NULL) {
            validity = M3G_VFUNC(Object, app, applyAnimation)(app, time);
            minValidity = M3G_MIN(validity, minValidity);
        }
    }
    return minValidity;
}

/*!
 * \internal
 * \brief Initializes a Sprite object. See specification
 * for default values.
 *
 * \param m3g           M3G interface
 * \param sprite        Sprite object
 * \param scaled        scaled flag
 * \param appearance    Appearance object
 * \param image         Image2D object
 * \retval M3G_TRUE     Sprite initialized
 * \retval M3G_FALSE    initialization failed
 */
static M3Gbool m3gInitSprite(Interface *m3g,
                             Sprite *sprite,
                             M3Gbool scaled,
                             Appearance *appearance,
                             Image *image)
{
    /* Sprite is derived from node */
    m3gInitNode(m3g, &sprite->node, M3G_CLASS_SPRITE);
    sprite->node.hasRenderables = M3G_TRUE;

    m3gIncStat(m3g, M3G_STAT_RENDERABLES, 1);
    
    sprite->scaled = scaled;
    M3G_ASSIGN_REF(sprite->appearance, appearance);
    return m3gSetSpriteImage(sprite, image);
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const NodeVFTable m3gvf_Sprite = {
    {
        {
            m3gSpriteApplyAnimation,
            m3gSpriteIsCompatible,
            m3gSpriteUpdateProperty,
            m3gSpriteDoGetReferences,
            m3gSpriteFindID,
            m3gSpriteDuplicate,
            m3gDestroySprite
        }
    },
    m3gNodeAlign,
    m3gSpriteDoRender,
    m3gSpriteGetBBox,
    m3gSpriteRayIntersect,
    m3gSpriteSetupRender,
    m3gNodeUpdateDuplicateReferences,
    m3gNodeValidate
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a Sprite object.
 *
 * \param hInterface    M3G interface
 * \param scaled        scaled flag
 * \param hImage        Image2D object
 * \param hAppearance   Appearance object
 * \retval Sprite new Sprite object
 * \retval NULL Sprite creating failed
 */
M3G_API M3GSprite m3gCreateSprite(M3GInterface hInterface,
                                  M3Gbool scaled,
                                  M3GImage hImage,
                                  M3GAppearance hAppearance)
{
    Interface *m3g = (Interface *) hInterface;
    M3G_VALIDATE_INTERFACE(m3g);

    if (hImage == 0) {
        m3gRaiseError(m3g, M3G_NULL_POINTER);
        return NULL;
    }

    {
        Sprite *sprite =  m3gAllocZ(m3g, sizeof(Sprite));

        if (sprite != NULL) {
            if (!m3gInitSprite(m3g,
                               sprite,
                               scaled,
                               (Appearance *)hAppearance,
                               (Image *)hImage)) {
                M3G_ASSIGN_REF(sprite->image, NULL);
                M3G_ASSIGN_REF(sprite->appearance, NULL);
                m3gFree(m3g, sprite);
                return NULL;
            }
        }

        return (M3GSprite) sprite;
    }
}

/*!
 * \brief Get sprite scaled flag.
 *
 * \param handle        Sprite object
 * \retval M3G_TRUE     sprite is scaled
 * \retval M3G_FALSE    sprite is not scaled
 */
M3G_API M3Gbool m3gIsScaledSprite(M3GSprite handle)
{
    Sprite *sprite = (Sprite *) handle;
    M3G_VALIDATE_OBJECT(sprite);

    return sprite->scaled;
}

/*!
 * \brief Set sprite appearance.
 *
 * \param handle        Sprite object
 * \param hAppearance   Appearance object
 */
M3G_API void m3gSetSpriteAppearance(M3GSprite handle,
                                    M3GAppearance hAppearance)
{
    Sprite *sprite = (Sprite *) handle;
    M3G_VALIDATE_OBJECT(sprite);

    M3G_ASSIGN_REF(sprite->appearance, hAppearance);
}

/*!
 * \brief Set sprite image
 *
 * \param handle        Sprite object
 * \param hImage        Image2D object
 * \retval              M3G_TRUE image was set
 * \retval              M3G_FALSE failed to set image
 */
M3G_API M3Gbool m3gSetSpriteImage(M3GSprite handle, M3GImage hImage)
{
    Sprite *sprite = (Sprite *) handle;
    Image *image = (Image *)hImage;
    M3G_VALIDATE_OBJECT(sprite);

    if (image == NULL) {
        m3gRaiseError(M3G_INTERFACE(sprite), M3G_NULL_POINTER);
        return M3G_FALSE;
    }

    M3G_ASSIGN_REF(sprite->image, image);

    sprite->width = m3gGetWidth(image);
    sprite->height = m3gGetHeight(image);

    sprite->crop.x = 0;
    sprite->crop.y = 0;
    sprite->crop.width  = m3gClampInt(sprite->width,  0, M3G_MAX_TEXTURE_DIMENSION);
    sprite->crop.height = m3gClampInt(sprite->height, 0, M3G_MAX_TEXTURE_DIMENSION);

    sprite->flip = 0;

    return M3G_TRUE;
}

/*!
 * \brief Set sprite image crop rectangle.
 *
 * \param handle        Sprite object
 * \param cropX         crop upper left x
 * \param cropY         crop upper left y
 * \param width         crop width
 * \param height        crop height
 */
M3G_API void m3gSetCrop(M3GSprite handle,
                        M3Gint cropX, M3Gint cropY,
                        M3Gint width, M3Gint height)
{
    Sprite *sprite = (Sprite *) handle;
    M3G_VALIDATE_OBJECT(sprite);

    /* Check for illegal crop size */
    if (!m3gInRange(width,  -M3G_MAX_TEXTURE_DIMENSION, M3G_MAX_TEXTURE_DIMENSION) ||
        !m3gInRange(height, -M3G_MAX_TEXTURE_DIMENSION, M3G_MAX_TEXTURE_DIMENSION) ) {
        m3gRaiseError(M3G_INTERFACE(sprite), M3G_INVALID_VALUE);
        return;
    }

    sprite->crop.x = cropX;
    sprite->crop.y = cropY;

    if (width < 0) {
        sprite->crop.width = -width;
        sprite->flip |= FLIPX;
    }
    else {
        sprite->crop.width = width;
        sprite->flip &= ~FLIPX;
    }

    if (height < 0) {
        sprite->crop.height = -height;
        sprite->flip |= FLIPY;
    }
    else {
        sprite->crop.height = height;
        sprite->flip &= ~FLIPY;
    }
}

/*!
 * \brief Get sprite image crop parameter.
 *
 * \param handle        Sprite object
 * \param which         which crop parameter to return
 *                      \arg M3G_GET_CROPX
 *                      \arg M3G_GET_CROPY
 *                      \arg M3G_GET_CROPWIDTH
 *                      \arg M3G_GET_CROPHEIGHT
 * \return              image crop parameter
 */
M3Gint m3gGetCrop(M3GSprite handle, M3Gint which)
{
    Sprite *sprite = (Sprite *) handle;
    M3G_VALIDATE_OBJECT(sprite);

    switch(which) {
    case M3G_GET_CROPX:
        return sprite->crop.x;
    case M3G_GET_CROPY:
        return sprite->crop.y;
    case M3G_GET_CROPWIDTH:
        return (sprite->flip & FLIPX) ? -sprite->crop.width : sprite->crop.width;
    case M3G_GET_CROPHEIGHT:
    default:
        return (sprite->flip & FLIPY) ? -sprite->crop.height : sprite->crop.height;
    }
}

/*!
 * \brief Gets sprite appearance.
 *
 * \param handle        Sprite object
 * \return              Appearance object
 */
M3G_API M3GAppearance m3gGetSpriteAppearance(M3GSprite handle)
{
    Sprite *sprite = (Sprite *) handle;
    M3G_VALIDATE_OBJECT(sprite);

    return sprite->appearance;
}

/*!
 * \brief Gets sprite image.
 *
 * \param handle        Sprite object
 * \return              Image2D object
 */
M3G_API M3GImage m3gGetSpriteImage(M3GSprite handle)
{
    Sprite *sprite = (Sprite *) handle;
    M3G_VALIDATE_OBJECT(sprite);

    return sprite->image;
}

#undef FLIPX
#undef FLIPY

