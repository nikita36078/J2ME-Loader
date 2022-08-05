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
* Description: Core library main module
*
*/


/*!
 * \internal
 * \file
 * \brief Core library main module
 *
 * Just includes most of the other core modules.
 */

/*----------------------------------------------------------------------
 * Forward declarations for internal class data types
 *--------------------------------------------------------------------*/

/* When adding to this list, please maintain the alphabetical order!
 * It helps spotting any missing classes. */

typedef struct M3GAnimationControllerImpl       AnimationController;
typedef struct M3GAnimationTrackImpl            AnimationTrack;
typedef struct M3GAppearanceImpl                Appearance;
typedef struct M3GBackgroundImpl                Background;
typedef struct M3GCameraImpl                    Camera;
typedef struct M3GCompositingModeImpl           CompositingMode;
typedef struct M3GFogImpl                       Fog;
typedef struct M3GGroupImpl                     Group;
typedef struct M3GImageImpl                     Image;
typedef struct M3GIndexBufferImpl               IndexBuffer;
typedef struct M3GKeyframeSequenceImpl          KeyframeSequence;
typedef struct M3GLightImpl                     Light;
typedef struct M3GMaterialImpl                  Material;
typedef struct M3GMeshImpl                      Mesh;
typedef struct M3GMorphingMeshImpl              MorphingMesh;
typedef struct M3GNodeImpl                      Node;
typedef struct M3GObjectImpl                    Object;
typedef struct M3GPolygonModeImpl               PolygonMode;
typedef struct M3GRayIntersectionImpl           RayIntersection;
typedef struct M3GRenderContextImpl             RenderContext;
typedef struct M3GSkinnedMeshImpl               SkinnedMesh;
typedef struct M3GSpriteImpl                    Sprite;
typedef struct M3GTextureImpl                   Texture;
typedef struct M3GTransformableImpl             Transformable;
typedef struct M3GVertexArrayImpl               VertexArray;
typedef struct M3GVertexBufferImpl              VertexBuffer;
typedef struct M3GWorldImpl                     World;

typedef struct RenderQueueImpl  RenderQueue;

/*----------------------------------------------------------------------
 * Global default includes
 *--------------------------------------------------------------------*/

#include "m3g_defs.h"
#include "m3g_interface.h"
#include "m3g_math.h"
#include "m3g_memory.h"

/*----------------------------------------------------------------------
 * Include inline code modules
 *--------------------------------------------------------------------*/

#define M3G_CORE_INCLUDE

/*
 * Include internal components; this should include all the inline C
 * modules pertaining to the core implementation and containing
 * implementation details not visible outside of the core module.
 *
 * Also note that the Java, GL, and math modules are not included
 * here; they are linked normally instead.
 */
#if !defined(M3G_DEBUG)
#include "m3g_rendercontext.c"
#include "m3g_animationcontroller.c"
#include "m3g_animationtrack.c"
#include "m3g_appearance.c"
#include "m3g_array.c"
#include "m3g_background.c"
#include "m3g_camera.c"
#include "m3g_compositingmode.c"
#include "m3g_fog.c"
#include "m3g_group.c"
#include "m3g_image.c"
#include "m3g_indexbuffer.c"
#include "m3g_interface.c"
#include "m3g_keyframesequence.c"
#include "m3g_light.c"
#include "m3g_lightmanager.c"
#ifdef M3G_NATIVE_LOADER
#include "m3g_loader.c"
#endif
#include "m3g_material.c"
#include "m3g_math.c"
#include "m3g_memory.c"
#include "m3g_mesh.c"
#include "m3g_morphingmesh.c"
#include "m3g_node.c"
#include "m3g_object.c"
#include "m3g_polygonmode.c"
#include "m3g_renderqueue.c"
#include "m3g_skinnedmesh.c"
#include "m3g_sprite.c"
#include "m3g_tcache.c"
#include "m3g_texture.c"
#include "m3g_transformable.c"
#include "m3g_vertexarray.c"
#include "m3g_vertexbuffer.c"
#include "m3g_world.c"

#else /* M3G_DEBUG */

#include "m3g_rendercontext.c"
#include "m3g_world.c"
#include "m3g_vertexbuffer.c"
#include "m3g_vertexarray.c"
#include "m3g_transformable.c"
#include "m3g_texture.c"
#include "m3g_tcache.c"
#include "m3g_sprite.c"
#include "m3g_skinnedmesh.c"
#include "m3g_renderqueue.c"
#include "m3g_polygonmode.c"
#include "m3g_object.c"
#include "m3g_node.c"
#include "m3g_morphingmesh.c"
#include "m3g_mesh.c"
#include "m3g_memory.c"
#include "m3g_math.c"
#include "m3g_material.c"
#ifdef M3G_NATIVE_LOADER
#include "m3g_loader.c"
#endif
#include "m3g_lightmanager.c"
#include "m3g_light.c"
#include "m3g_keyframesequence.c"
#include "m3g_interface.c"
#include "m3g_indexbuffer.c"
#include "m3g_image.c"
#include "m3g_group.c"
#include "m3g_fog.c"
#include "m3g_compositingmode.c"
#include "m3g_camera.c"
#include "m3g_background.c"
#include "m3g_array.c"
#include "m3g_appearance.c"
#include "m3g_animationtrack.c"
#include "m3g_animationcontroller.c"

#endif /* M3G_DEBUG */

#undef M3G_CORE_INCLUDE

/*----------------------------------------------------------------------
 * Virtual function tables
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief This table maps class ID's to virtual function table pointers
 *
 * The table pointers \em must correspond to the class ID enumeration
 * defined in m3g_core.h
 */
static const void* const m3gc_vfTable[M3G_CLASS_WORLD + 1] = {
    NULL,                       /* M3G_ABSTRACT_CLASS */
    &m3gvf_AnimationController, /* M3G_CLASS_ANIMATION_CONTROLLER */
    &m3gvf_AnimationTrack,      /* M3G_CLASS_ANIMATION_TRACK      */
    &m3gvf_Appearance,          /* M3G_CLASS_APPEARANCE           */
    &m3gvf_Background,          /* M3G_CLASS_BACKGROUND           */
    &m3gvf_Camera,              /* M3G_CLASS_CAMERA               */
    &m3gvf_CompositingMode,     /* M3G_CLASS_COMPOSITING_MODE     */
    &m3gvf_Fog,                 /* M3G_CLASS_FOG                  */
    &m3gvf_Group,               /* M3G_CLASS_GROUP                */
    &m3gvf_Image,               /* M3G_CLASS_IMAGE                */
    &m3gvf_IndexBuffer,         /* M3G_CLASS_INDEX_BUFFER         */
    &m3gvf_KeyframeSequence,    /* M3G_CLASS_KEYFRAME_SEQUENCE    */
    &m3gvf_Light,               /* M3G_CLASS_LIGHT                */
#ifdef M3G_NATIVE_LOADER
    &m3gvf_Loader,              /* M3G_CLASS_LOADER               */
#else
    NULL,                       /* Native loader not used         */
#endif
    &m3gvf_Material,            /* M3G_CLASS_MATERIAL             */
    &m3gvf_Mesh,                /* M3G_CLASS_MESH                 */
    &m3gvf_MorphingMesh,        /* M3G_CLASS_MORPHING_MESH        */
    &m3gvf_PolygonMode,         /* M3G_CLASS_POLYGON_MODE         */
    &m3gvf_RenderContext,       /* M3G_CLASS_RENDER_CONTEXT       */
    &m3gvf_SkinnedMesh,         /* M3G_CLASS_SKINNED_MESH         */
    &m3gvf_Sprite,              /* M3G_CLASS_SPRITE               */
    &m3gvf_Texture,             /* M3G_CLASS_TEXTURE              */
    &m3gvf_VertexArray,         /* M3G_CLASS_VERTEX_ARRAY         */
    &m3gvf_VertexBuffer,        /* M3G_CLASS_VERTEX_BUFFER        */
    &m3gvf_World                /* M3G_CLASS_WORLD                */
};

static M3G_INLINE const ObjectVFTable *m3gGetVFTable(const Object *obj)
{
    return (const ObjectVFTable*) m3gc_vfTable[obj->classID];
}

