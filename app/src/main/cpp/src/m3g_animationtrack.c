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
* Description: AnimationTrack implementation
*
*/


/*!
 * \internal
 * \file
 * \brief AnimationTrack implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_animationtrack.h"
#include "m3g_keyframesequence.h"
#include "m3g_memory.h"


/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Destroys this AnimationTrack object.
 *
 * \param obj AnimationTrack object
 */
static void m3gDestroyAnimationTrack(Object *obj)
{
    AnimationTrack *animTrack = (AnimationTrack *) obj;
    M3G_VALIDATE_OBJECT(animTrack);
    
    M3G_ASSIGN_REF(animTrack->sequence,   NULL);
    M3G_ASSIGN_REF(animTrack->controller, NULL);
    
    m3gDestroyObject(&animTrack->object);
}

/*!
 * \internal
 * \brief Calculates animation track contribution.
 *
 * \param track AnimationTrack object
 * \param time current world time
 * \param accumSamples accumulated samples
 * \param sampleInfo sample information
 */
static void m3gGetContribution(const AnimationTrack *track, M3Gint time,
                               M3Gfloat *accumSamples, SampleInfo *sampleInfo)
{
    if (track->controller == NULL || !m3gIsActive(track->controller, time)) {
        sampleInfo->weight = 0;
        sampleInfo->validity = (track->controller ?
                                m3gTimeToActivation(track->controller, time) :
                                0x7FFFFFFF);
        if (sampleInfo->validity < 1)
            sampleInfo->validity = 1;
        return;
    }
    {
        M3Gfloat stackSampleVector[4];
        Interface *m3g = M3G_INTERFACE(track);
        M3Gint i, sampleTime, sampleValidity;
        M3Gfloat weight;
        M3Gint sampleLength = m3gGetNumComponents(track->sequence);
        M3Gfloat *sample;

        /* Before sampling, make sure that this track has some effect
         * on the end result */
        
        weight = m3gGetWeight(track->controller);
        sampleInfo->weight = weight;
        
        if (weight <= 0.0f) {
            sampleInfo->validity = 0x7FFFFFFF;
            return;
        }
            
        /* We use the stack-allocated sample vector by default, but
         * the MORPH_WEIGHTS target may have more than 4 components,
         * in which case we resort to the global temp vector. This
         * means that the temp vector can not be used in the keyframe
         * sampling code, but it doesn't seem likely that it would be
         * needed there. */
        
        if (sampleLength > 4) {
            sample = (M3Gfloat*) m3gAllocTemp(m3g, (M3Gsize) sampleLength * sizeof(M3Gfloat));
            if (!sample) {
                sampleInfo->validity = 0;
                return; /* automatic out-of-memory error */
            }
        }
        else {
            sample = stackSampleVector;
        }
        
        sampleTime = m3gRoundToInt(m3gGetPosition(track->controller, time));
        sampleValidity = m3gGetSample(track->sequence, sampleTime, sample);
        sampleInfo->validity = sampleValidity;

        /* Only bother if there was no error in GetSample... */
            
        if (sampleValidity > 0) {

            /* Resolve the validity time of the sample */
                
            sampleValidity = m3gTimeToDeactivation(track->controller, time);
            if (sampleValidity < sampleInfo->validity) {
                sampleInfo->validity = sampleValidity;
            }
                
            /* Add the weighted sample to the accumulated value
             * and return */
                
            for (i = 0; i < sampleLength; ++i) {
                accumSamples[i] = m3gAdd(accumSamples[i],
                                         m3gMul(sample[i], weight));
            }
        }
        if (sample != stackSampleVector) {
            m3gFreeTemp(m3g);
        }
    }
}

/*!
 * \internal
 * \brief Checks animation property size.
 *
 * \param m3g M3G interface
 * \param property animation property
 * \param numComponents number of components
 * \retval M3G_TRUE valid size for property
 * \retval M3G_FALSE invalid size for property
 */
static M3Gbool m3gIsValidSize(Interface *m3g, M3Gint property, M3Gint numComponents)
{
    switch (property) {
    case M3G_ANIM_ALPHA:
    case M3G_ANIM_DENSITY:
    case M3G_ANIM_FAR_DISTANCE:
    case M3G_ANIM_FIELD_OF_VIEW:
    case M3G_ANIM_INTENSITY:
    case M3G_ANIM_NEAR_DISTANCE:
    case M3G_ANIM_PICKABILITY:
    case M3G_ANIM_SHININESS:
    case M3G_ANIM_SPOT_ANGLE:
    case M3G_ANIM_SPOT_EXPONENT:
    case M3G_ANIM_VISIBILITY:
        return (numComponents == 1);
    case M3G_ANIM_CROP:
        return (numComponents == 2 || numComponents == 4);
    case M3G_ANIM_COLOR:
    case M3G_ANIM_AMBIENT_COLOR:
    case M3G_ANIM_DIFFUSE_COLOR:
    case M3G_ANIM_EMISSIVE_COLOR:
    case M3G_ANIM_SPECULAR_COLOR:
        return (numComponents == 3);
    case M3G_ANIM_TRANSLATION:
        return (numComponents == 3);
    case M3G_ANIM_ORIENTATION:
        return (numComponents == 4);
    case M3G_ANIM_SCALE:
        return (numComponents == 1 || numComponents == 3);
    case M3G_ANIM_MORPH_WEIGHTS:
        return (numComponents > 0);
    default:
        m3gRaiseError(m3g, M3G_INVALID_ENUM);
    }
    return M3G_FALSE;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param self AnimationTrack object
 * \param references array of reference objects
 * \return number of references
 */
static M3Gint m3gAnimationTrackDoGetReferences(Object *self, Object **references)
{
    AnimationTrack *track = (AnimationTrack *) self;
    M3Gint num = m3gObjectDoGetReferences(self, references);
    if (track->sequence != NULL) {
        if (references != NULL)
            references[num] = (Object *)track->sequence;
        num++;
    }
    if (track->controller != NULL) {
        if (references != NULL)
            references[num] = (Object *)track->controller;
        num++;
    }
    return num;
}

/*!
 * \internal
 * \brief Overloaded Object3D method
 */
static Object *m3gAnimationTrackFindID(Object *self, M3Gint userID)
{
    AnimationTrack *track = (AnimationTrack *) self;
    Object *found = m3gObjectFindID(self, userID);
    
    if (!found && track->sequence != NULL) {
        found = m3gFindID((Object*) track->sequence, userID);
    }
    if (!found && track->controller != NULL) {
        found = m3gFindID((Object*) track->controller, userID);
    }
    return found;
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original AnimationTrack object
 * \param cloneObj pointer to cloned AnimationTrack object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gAnimationTrackDuplicate(const Object *originalObj,
                                          Object **cloneObj,
                                          Object **pairs,
                                          M3Gint *numPairs)
{
    AnimationTrack *original = (AnimationTrack *)originalObj;
    AnimationTrack *clone =
        (AnimationTrack *)m3gCreateAnimationTrack(originalObj->interface,
                                                  original->sequence,
                                                  original->property);
    *cloneObj = (Object *)clone;
    if (*cloneObj == NULL) {
        return M3G_FALSE;
    }

    if(m3gObjectDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        M3G_ASSIGN_REF(clone->controller, original->controller);
        return M3G_TRUE;
    }
    else {
        return M3G_FALSE;
    }
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const ObjectVFTable m3gvf_AnimationTrack = {
    m3gObjectApplyAnimation,
    m3gObjectIsCompatible,
    m3gObjectUpdateProperty,
    m3gAnimationTrackDoGetReferences,
    m3gAnimationTrackFindID,
    m3gAnimationTrackDuplicate,
    m3gDestroyAnimationTrack
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a new AnimationTrack with default values
 *
 * \param hInterface            M3G interface
 * \param hSequence             KeyframeSequence object
 * \param property              target animation property
 * \retval AnimationTrack new AnimationTrack object
 * \retval NULL AnimationTrack creating failed
 */
M3G_API M3GAnimationTrack m3gCreateAnimationTrack(M3GInterface hInterface,
                                                  M3GKeyframeSequence hSequence,
                                                  M3Gint property)
{
    KeyframeSequence *sequence = (KeyframeSequence *)hSequence;
    Interface *m3g = (Interface *) hInterface;
    M3G_VALIDATE_INTERFACE(m3g);

    /* Check for invalid arguments */
    
    if (sequence == NULL) {
        m3gRaiseError(m3g, M3G_NULL_POINTER);
        return NULL;
    }
    if (property < M3G_ANIM_ALPHA || property > M3G_ANIM_VISIBILITY) {
        m3gRaiseError(m3g, M3G_INVALID_ENUM);
        return NULL;
    }
    if (!m3gIsValidSize(m3g, property, m3gGetNumComponents(sequence))) {
        m3gRaiseError(m3g, M3G_INVALID_VALUE);
        return NULL;
    }

    /* Allocate and initialize the object */
    
    {
        AnimationTrack *track = m3gAllocZ(m3g, sizeof(AnimationTrack));

        if (track != NULL) {
            M3G_ASSIGN_REF(track->sequence, sequence);
            track->property = property;
            m3gInitObject(&track->object, m3g, M3G_CLASS_ANIMATION_TRACK);
        }
        
        return track;
    }
}

/*!
 * \brief Get animation controller.
 *
 * \param hTrack                AnimationTrack object
 * \retval                      AnimationController object
 */
M3G_API M3GAnimationController m3gGetController(M3GAnimationTrack hTrack)
{
    AnimationTrack *track = (AnimationTrack *) hTrack;
    M3G_VALIDATE_OBJECT(track);

    return track->controller;
}

/*!
 * \brief Get key frame sequence.
 *
 * \param hTrack                AnimationTrack object
 * \retval                      KeyframeSequence object
 */
M3G_API M3GKeyframeSequence m3gGetSequence(M3GAnimationTrack hTrack)
{
    AnimationTrack *track = (AnimationTrack *) hTrack;
    M3G_VALIDATE_OBJECT(track);

    return track->sequence;
}

/*!
 * \brief Get animation target property.
 *
 * \param hTrack                AnimationTrack object
 * \retval                      target property
 */
M3G_API M3Gint m3gGetTargetProperty(M3GAnimationTrack hTrack)
{
    AnimationTrack *track = (AnimationTrack *) hTrack;
    M3G_VALIDATE_OBJECT(track);

    return track->property;
}

/*!
 * \brief Set animation controller.
 *
 * \param hTrack                AnimationTrack object
 * \param hController           AnimationController object
 */
M3G_API void m3gSetController(M3GAnimationTrack hTrack,
                              M3GAnimationController hController)
{
    AnimationTrack *track = (AnimationTrack *) hTrack;
    M3G_VALIDATE_OBJECT(track);

    M3G_ASSIGN_REF(track->controller, hController);
}

