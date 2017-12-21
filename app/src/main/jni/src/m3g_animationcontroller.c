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
* Description: AnimationController implementation
*
*/


/*!
 * \internal
 * \file
 * \brief AnimationController implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_animationcontroller.h"
#include "m3g_memory.h"


/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Destroys this AnimationController object.
 *
 * \param obj AnimationController object
 */
static void m3gDestroyAnimationController(Object *obj)
{
    AnimationController *animController = (AnimationController *) obj;
    M3G_VALIDATE_OBJECT(animController);

    m3gDestroyObject(&animController->object);
}

/*!
 * \internal
 * \brief Check if controller is active.
 *
 * \param controller AnimationController object
 * \param worldTime current world time
 * \retval M3G_TRUE controller active
 * \retval M3G_FALSE controller not active
 */
static M3Gbool m3gIsActive(const AnimationController *controller,
                        M3Gint worldTime)
{
    if (controller->activationTime == controller->deactivationTime) {
        return M3G_TRUE;
    }
    return (worldTime >= controller->activationTime &&
            worldTime < controller->deactivationTime);
}

/*!
 * \internal
 * \brief Gets time to controller activation.
 *
 * \param controller AnimationController object
 * \param worldTime current world time
 * \return time to controller activation
 */
static M3Gint m3gTimeToActivation(const AnimationController *controller,
                               M3Gint worldTime)
{
    if (worldTime < controller->activationTime) {
        return (controller->activationTime - worldTime);
    }
    else if (worldTime < controller->deactivationTime) {
        return 0;
    }
    return 0x7FFFFFFF;
}

/*!
 * \internal
 * \brief Gets time to controller deactivation.
 *
 * \param controller AnimationController object
 * \param worldTime current world time
 * \return time to controller deactivation
 */
static M3Gint m3gTimeToDeactivation(const AnimationController *controller,
                                 M3Gint worldTime)
{
    if (worldTime < controller->deactivationTime) {
        return (controller->deactivationTime - worldTime);
    }
    return 0x7FFFFFFF;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original AnimationController object
 * \param cloneObj pointer to cloned AnimationController object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gAnimationControllerDuplicate(const Object *originalObj,
                                               Object **cloneObj,
                                               Object **pairs,
                                               M3Gint *numPairs)
{
    const AnimationController *original = (AnimationController *)originalObj;
    AnimationController *clone =
        m3gCreateAnimationController(originalObj->interface);
    *cloneObj = (Object *)clone;
    if (*cloneObj == NULL) {
        return M3G_FALSE;
    }

    if (m3gObjectDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        clone->activationTime = original->activationTime;
        clone->deactivationTime = original->deactivationTime;
        clone->weight = original->weight;
        clone->speed = original->speed;
        clone->refWorldTime = original->refWorldTime;
        clone->refSequenceTime = original->refSequenceTime;
        return M3G_TRUE;
    }
    else {
        return M3G_FALSE;
    }
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const ObjectVFTable m3gvf_AnimationController = {
    m3gObjectApplyAnimation,
    m3gObjectIsCompatible,
    m3gObjectUpdateProperty,
    m3gObjectDoGetReferences,
    m3gObjectFindID,
    m3gAnimationControllerDuplicate,
    m3gDestroyAnimationController
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a new AnimationController with default values
 *
 * \param hInterface    M3G interface
 * \retval AnimationController new AnimationController object
 * \retval NULL AnimationController creating failed
 */
M3G_API M3GAnimationController m3gCreateAnimationController(
    M3GInterface hInterface)
{
    Interface *m3g = (Interface *) hInterface;
    M3G_VALIDATE_INTERFACE(m3g);
    {
        AnimationController *controller = m3gAllocZ(m3g, sizeof(AnimationController));

        if (controller != NULL) {
            m3gInitObject(&controller->object, m3g,
                          M3G_CLASS_ANIMATION_CONTROLLER);
            controller->weight = 1;
            controller->speed = 1.0f;
        }

        return (M3GAnimationController) controller;
    }
}

/*!
 * \brief Set active interval.
 *
 * \param hController AnimationController object
 * \param worldTimeMin active interval start
 * \param worldTimeMax active interval end
 */
M3G_API void m3gSetActiveInterval(M3GAnimationController hController,
                                  M3Gint worldTimeMin,
                                  M3Gint worldTimeMax)
{
    AnimationController *controller = (AnimationController *) hController;
    M3G_VALIDATE_OBJECT(controller);

    if (worldTimeMin > worldTimeMax) {
        m3gRaiseError(M3G_INTERFACE(controller), M3G_INVALID_VALUE);
        return;
    }
    controller->activationTime   = worldTimeMin;
    controller->deactivationTime = worldTimeMax;
}

/*!
 * \brief Get active interval start.
 *
 * \param hController AnimationController object
 * \return active interval start
 */
M3G_API M3Gint m3gGetActiveIntervalStart(M3GAnimationController hController)
{
    AnimationController *controller = (AnimationController *) hController;
    M3G_VALIDATE_OBJECT(controller);

    return controller->activationTime;
}

/*!
 * \brief Get active interval end.
 *
 * \param hController AnimationController object
 * \return active interval end
 */
M3G_API M3Gint m3gGetActiveIntervalEnd(M3GAnimationController hController)
{
    AnimationController *controller = (AnimationController *) hController;
    M3G_VALIDATE_OBJECT(controller);

    return controller->deactivationTime;
}

/*!
 * \brief Set speed.
 *
 * \param hController   AnimationController object
 * \param factor        speed factor
 * \param worldTime     reference world time
 */
M3G_API void m3gSetSpeed(M3GAnimationController hController,
                         M3Gfloat factor, M3Gint worldTime)
{
    AnimationController *controller = (AnimationController *) hController;
    M3G_VALIDATE_OBJECT(controller);

    controller->refSequenceTime = m3gGetPosition(controller, worldTime);
    controller->refWorldTime = worldTime;
    controller->speed = factor;
}

/*!
 * \brief Get speed.
 *
 * \param hController   AnimationController object
 * \return              speed factor
 */
M3G_API M3Gfloat m3gGetSpeed(M3GAnimationController hController)
{
    AnimationController *controller = (AnimationController *) hController;
    M3G_VALIDATE_OBJECT(controller);

    return controller->speed;
}

/*!
 * \brief Set position.
 *
 * \param hController   AnimationController object
 * \param sequenceTime  sequence time
 * \param worldTime     world time
 */
M3G_API void m3gSetPosition(M3GAnimationController hController,
                            M3Gfloat sequenceTime, M3Gint worldTime)
{
    AnimationController *controller = (AnimationController *) hController;
    M3G_VALIDATE_OBJECT(controller);

    controller->refWorldTime = worldTime;
    controller->refSequenceTime = sequenceTime;
}

/*!
 * \brief Get position.
 *
 * \param hController   AnimationController object
 * \param worldTime     current world time
 * \retrun              position
 */
M3G_API M3Gfloat m3gGetPosition(M3GAnimationController hController, M3Gint worldTime)
{
    AnimationController *controller = (AnimationController *) hController;
    M3G_VALIDATE_OBJECT(controller);

    return m3gAdd(controller->refSequenceTime,
                  m3gMul(controller->speed, (M3Gfloat) worldTime - controller->refWorldTime));
}


/*!
 * \brief Set controller weight.
 *
 * \param hController   AnimationController object
 * \param weight        controller weight
 */
M3G_API void m3gSetWeight(M3GAnimationController hController, M3Gfloat weight)
{
    AnimationController *controller = (AnimationController *) hController;
    M3G_VALIDATE_OBJECT(controller);

    if (weight < 0) {
        m3gRaiseError(M3G_INTERFACE(controller), M3G_INVALID_VALUE);
        return;
    }
    controller->weight = weight;
}

/*!
 * \brief Get controller weight.
 *
 * \param hController   AnimationController object
 * \return              controller weight
 */
M3G_API M3Gfloat m3gGetWeight(M3GAnimationController hController)
{
    AnimationController *controller = (AnimationController *) hController;
    M3G_VALIDATE_OBJECT(controller);

    return controller->weight;
}

/*!
 * \brief Get reference world time.
 *
 * \param hController   AnimationController object
 * \return              reference world time
 */
M3G_API M3Gint m3gGetRefWorldTime(M3GAnimationController hController)
{
    AnimationController *controller = (AnimationController *) hController;
    M3G_VALIDATE_OBJECT(controller);

    return controller->refWorldTime;
}
