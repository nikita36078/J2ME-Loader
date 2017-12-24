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
* Description: World implementation
*
*/


/*!
 * \internal
 * \file
 * \brief World implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_world.h"
#include "m3g_memory.h"

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Destroys this World object.
 *
 * \param obj World object
 */
static void m3gDestroyWorld(Object *obj)
{
    World *world = (World *) obj;
    M3G_VALIDATE_OBJECT(world);

    M3G_ASSIGN_REF(world->activeCamera, NULL);
    M3G_ASSIGN_REF(world->background, NULL);

    m3gDestroyGroup(obj);
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self World object
 * \param time current world time
 * \return minimum validity
 */
static M3Gint m3gWorldApplyAnimation(Object *self, M3Gint time)
{
    M3Gint minValidity;
    World *world = (World *)self;
    M3G_VALIDATE_OBJECT(world);

    minValidity = m3gGroupApplyAnimation(self, time);
    
    if (world->background != NULL && minValidity > 0) {
        M3Gint validity = M3G_VFUNC(Object, world->background, applyAnimation)((Object *)world->background, time);
        minValidity = (minValidity < validity ? minValidity : validity);
    }
    return minValidity;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self World object
 * \param references array of reference objects
 * \return number of references
 */
static M3Gint m3gWorldDoGetReferences(Object *self, Object **references)
{
    World *world = (World *)self;
    M3Gint num = m3gGroupDoGetReferences(self, references);
    if (world->activeCamera != NULL) {
        if (references != NULL)
            references[num] = (Object *)world->activeCamera;
        num++;
    }
    if (world->background != NULL) {
        if (references != NULL)
            references[num] = (Object *)world->background;
        num++;
    }
    return num;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 */
static Object *m3gWorldFindID(Object *self, M3Gint userID)
{
    World *world = (World *)self;
    Object *found = m3gGroupFindID(self, userID);
    
    if (!found && world->activeCamera != NULL) {
        found = m3gFindID((Object*) world->activeCamera, userID);
    }
    if (!found && world->background != NULL) {
        found = m3gFindID((Object*) world->background, userID);
    }
    return found;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original World object
 * \param cloneObj pointer to cloned World object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gWorldDuplicate(const Object *originalObj,
                                 Object **cloneObj,
                                 Object **pairs,
                                 M3Gint *numPairs)
{
    World *original = (World *)originalObj;
    World *clone;
    M3G_ASSERT(*cloneObj == NULL); /* no derived classes */

    /* Create the clone object */
    
    clone = (World *)m3gCreateWorld(originalObj->interface);
    if (!clone) {
        return M3G_FALSE;
    }
    *cloneObj = (Object *)clone;

    /* Duplicate base class data */
    
    if (!m3gGroupDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        return M3G_FALSE;
    }

    /* Duplicate our own data */
    
    M3G_ASSIGN_REF(clone->background, original->background);
    M3G_ASSIGN_REF(clone->activeCamera, original->activeCamera);
    
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self World object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static void m3gWorldUpdateDuplicateReferences(Node *self, Object **pairs, M3Gint numPairs)
{
    World *world = (World *)self;
    m3gGroupUpdateDuplicateReferences(self, pairs, numPairs);
    if (world->activeCamera != NULL) {
        Node *duplicatedInstance = m3gGetDuplicatedInstance(self, pairs, numPairs);
        Node *activeCamDuplicate = m3gGetDuplicatedInstance((Node *)world->activeCamera, pairs, numPairs);
        if (activeCamDuplicate != NULL &&
            m3gIsChildOf(duplicatedInstance, activeCamDuplicate)) {
            M3G_ASSIGN_REF(((World *)duplicatedInstance)->activeCamera, activeCamDuplicate);
        }
    }
}

/*!
 * \internal
 * \brief Initializes a World object. See specification
 * for default values.
 *
 * \param m3g           M3G interface
 * \param world         World object
 */
static void m3gInitWorld(Interface *m3g, World *world)
{
	/* World is derived from group */
	m3gInitGroup(m3g, &world->group, M3G_CLASS_WORLD);
}


/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const NodeVFTable m3gvf_World = {
    {
        {
            m3gWorldApplyAnimation,
            m3gNodeIsCompatible,
            m3gNodeUpdateProperty,
            m3gWorldDoGetReferences,
            m3gWorldFindID,
            m3gWorldDuplicate,
            m3gDestroyWorld
        }
    },
    m3gGroupAlign,
    NULL, /* pure virtual DoRender */
    m3gGroupGetBBox,
    m3gGroupRayIntersect,
    m3gGroupSetupRender,
    m3gWorldUpdateDuplicateReferences,
    m3gGroupValidate
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a World object.
 *
 * \param interface M3G interface
 * \retval World new World object
 * \retval NULL World creating failed
 */

/*@access M3Ginterface@*/
/*@access M3Gobject@*/
M3G_API M3GWorld m3gCreateWorld(M3GInterface interface)
{
    Interface *m3g = (Interface *) interface;
    M3G_VALIDATE_INTERFACE(m3g);

	{
		World *world = m3gAllocZ(m3g, sizeof(World));
	
        if (world != NULL) {
    		m3gInitWorld(m3g, world);
        }

		return (M3GWorld) world;
	}
}

/*!
 * \brief Set active camera.
 *
 * \param handle        World object
 * \param hCamera       Camera object
 */

/*@access M3Gobject@*/
M3G_API void m3gSetActiveCamera(M3GWorld handle, M3GCamera hCamera)
{
    World *world = (World *) handle;
    M3G_VALIDATE_OBJECT(world);

    if(hCamera == NULL) {
        m3gRaiseError(M3G_INTERFACE(world), M3G_NULL_POINTER);
        return;
    }

    M3G_ASSIGN_REF(world->activeCamera, hCamera);
}

/*!
 * \brief Set background.
 *
 * \param handle        World object
 * \param hBackground   Background object
 */

/*@access M3Gobject@*/
M3G_API void m3gSetBackground(M3GWorld handle, M3GBackground hBackground)
{
    World *world = (World *) handle;
    M3G_VALIDATE_OBJECT(world);

    M3G_ASSIGN_REF(world->background, hBackground);
}

/*!
 * \brief Get background.
 *
 * \param handle        World object
 * \return              Background object
 */

/*@access M3Gobject@*/
M3G_API M3GBackground m3gGetBackground(M3GWorld handle)
{
    World *world = (World *) handle;
    M3G_VALIDATE_OBJECT(world);

    return world->background;
}

/*!
 * \brief Get active camera.
 *
 * \param handle        World object
 * \return              Camera object
 */

/*@access M3Gobject@*/
M3G_API M3GCamera m3gGetActiveCamera(M3GWorld handle)
{
    World *world = (World *) handle;
    M3G_VALIDATE_OBJECT(world);

    return world->activeCamera;
}

