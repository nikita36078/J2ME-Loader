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
* Description: KeyframeSequence implementation
*
*/


/*!
 * \internal
 * \file
 * \brief KeyframeSequence implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_keyframesequence.h"
#include "m3g_memory.h"

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Destroys this KeyframeSequence object.
 *
 * \param obj KeyframeSequence object
 */
static void m3gDestroyKeyframeSequence(Object *obj)
{
    KeyframeSequence *sequence = (KeyframeSequence *) obj;
    M3G_VALIDATE_OBJECT(sequence);
    {
        Interface *m3g = M3G_INTERFACE(sequence);
        m3gFree(m3g, sequence->keyframes);
        m3gFree(m3g, sequence->keyframeTimes);
        m3gFree(m3g, sequence->inTangents);
        m3gFree(m3g, sequence->outTangents);
        m3gFree(m3g, sequence->a);
        m3gFree(m3g, sequence->b);
    }
    m3gDestroyObject(&sequence->object);
}

/*!
 * \internal
 * \brief Overloaded Object3D method.
 *
 * \param originalObj original KeyframeSequence object
 * \param cloneObj pointer to cloned KeyframeSequence object
 * \param pairs array for all object-duplicate pairs
 * \param numPairs number of pairs
 */
static M3Gbool m3gKeyframeSequenceDuplicate(const Object *originalObj,
                                            Object **cloneObj,
                                            Object **pairs,
                                            M3Gint *numPairs)
{
    KeyframeSequence *original = (KeyframeSequence *)originalObj;
    KeyframeSequence *clone =
        (KeyframeSequence *)m3gCreateKeyframeSequence(originalObj->interface,
                                                      original->numKeyframes,
                                                      original->numComponents,
                                                      original->interpolation);
    *cloneObj = (Object *)clone;
    if (*cloneObj == NULL) {
        return M3G_FALSE;
    }

    if(m3gObjectDuplicate(originalObj, cloneObj, pairs, numPairs)) {
        M3Gsizei n = original->numKeyframes * original->numComponents;
        
        m3gCopy(clone->keyframes, original->keyframes, n * sizeof(M3Gfloat));
        m3gCopy(clone->keyframeTimes, original->keyframeTimes, original->numKeyframes * sizeof(M3Gint));
        if (original->dirty == M3G_FALSE) {
            if (original->inTangents) {
                m3gCopy(clone->inTangents, original->inTangents, n * sizeof(M3Gfloat));
                m3gCopy(clone->outTangents, original->outTangents, n * sizeof(M3Gfloat));
            }
            if (original->a) {
                m3gCopy(clone->a, original->a, original->numKeyframes * sizeof(Quat));
                m3gCopy(clone->b, original->b, original->numKeyframes * sizeof(Quat));
            }
        }
        else {
            clone->dirty = M3G_TRUE;
        }

        clone->duration = original->duration;
        clone->closed = original->closed;
        clone->firstValid = original->firstValid;
        clone->lastValid = original->lastValid;
        return M3G_TRUE;
    }
    else {
        return M3G_FALSE;
    }
}

/*!
 * \internal
 * \brief Initializes a KeyframeSequence object. See specification
 * for default values.
 *
 * \param m3g                   M3G interface
 * \param sequence              KeyframeSequence object
 * \param numKeyframes          number of keyframes
 * \param numComponents         number of components
 * \param interpolation         interpolation type
 * \retval                      KeyframeSequence initialized KeyframeSequence object
 * \retval                      NULL initialization failed
 */
static KeyframeSequence *m3gInitKeyframeSequence(Interface *m3g,
                                                 KeyframeSequence *sequence,
                                                 M3Gint numKeyframes,
                                                 M3Gint numComponents,
                                                 M3Gint interpolation)
{
    m3gInitObject(&sequence->object, m3g, M3G_CLASS_KEYFRAME_SEQUENCE);

    /* Set keyframe parameters */
    
    sequence->numKeyframes = numKeyframes;
    sequence->numComponents = numComponents;
    sequence->interpolation = interpolation;
    sequence->lastValid = numKeyframes - 1; /* firstValid defaults to 0 */

    /* Allocate keyframe and tangent data */
    {    
        M3Gsizei n = numKeyframes * numComponents;
        
        sequence->keyframes = (M3Gfloat *)m3gAllocZ(m3g, n * sizeof(M3Gfloat));
        if (sequence->keyframes == NULL) {
            goto AllocFailed;
        }
        sequence->keyframeTimes = (M3Gint *)m3gAllocZ(m3g, numKeyframes * sizeof(M3Gint));
        if (sequence->keyframeTimes == NULL) {
            goto AllocFailed;
        }
        
        if (interpolation == M3G_SPLINE) {
            sequence->inTangents  = (M3Gfloat *)m3gAllocZ(m3g, n * sizeof(M3Gfloat));
            sequence->outTangents = (M3Gfloat *)m3gAllocZ(m3g, n * sizeof(M3Gfloat));
            if (sequence->inTangents == NULL || sequence->outTangents == NULL) {
                goto AllocFailed;
            }
        }
        else if (interpolation == M3G_SQUAD) {
            sequence->a = (Quat *)m3gAlloc(m3g, numKeyframes * sizeof(Quat));
            sequence->b = (Quat *)m3gAlloc(m3g, numKeyframes * sizeof(Quat));
            if (sequence->a == NULL || sequence->b == NULL) {
                goto AllocFailed;
            }
        }

        /* Success; just make a note that the data is not valid yet */
        
        sequence->dirty = M3G_TRUE;
        return sequence;

AllocFailed:
        /* The destructor contains exactly the code we need for this,
         * so just call that */
        
        m3gDestroyKeyframeSequence((Object*) sequence);
        return NULL;
    }
}

/*!
 * \internal
 * \brief Checks the validity of keyframe timings
 *
 * \param sequence   KeyframeSequence object
 * \retval M3G_TRUE  sequence valid
 * \retval M3G_FALSE sequence invalid
 */
static M3Gbool m3gValidSequence(const KeyframeSequence *sequence)
{    
    const M3Gint last = sequence->lastValid;
    M3Gint current = sequence->firstValid;
    
    while (current != last) {
        M3Gint next = (current < sequence->numKeyframes-1) ? current + 1 : 0;
        if (sequence->keyframeTimes[next] < sequence->keyframeTimes[current]) {
            return M3G_FALSE;
        }
        current = next;
    }
    return (sequence->keyframeTimes[last] <= sequence->duration);
}


/*!
 * \internal
 * \brief Get number of components
 *
 * \param sequence   KeyframeSequence object
 * \return number of components
 */
static M3Gint m3gGetNumComponents(const KeyframeSequence *sequence)
{
    return sequence->numComponents;
}

/*!
 * \internal
 * \brief Get next keyframe index
 *
 * \param sequence  KeyframeSequence object
 * \param ind       current index
 * \return next index
 */
static M3Gint m3gNextKeyframeIndex(const KeyframeSequence *sequence, M3Gint ind)
{
    if (ind == sequence->lastValid) {
        return sequence->firstValid;
    }
    else if (ind == sequence->numKeyframes - 1) {
        return 0;
    }
    else {
        return (ind + 1);
    }
}
    
/*!
 * \internal
 * \brief Get previous keyframe index
 *
 * \param sequence  KeyframeSequence object
 * \param ind       current index
 * \return previous index
 */
static M3Gint m3gPreviousKeyframeIndex(const KeyframeSequence *sequence, M3Gint ind)
{
    if (ind == sequence->firstValid) {
        return sequence->lastValid;
    }
    else if (ind == 0) {
        return (sequence->numKeyframes - 1);
    }
    else {
        return (ind - 1);
    }
}

/*!
 * \internal
 * \brief Get keyframe at index
 *
 * \param seq       KeyframeSequence object
 * \param idx       keyframe index
 * \return keyframe value
 */
static M3G_INLINE const M3Gfloat *m3gKeyframeAt(const KeyframeSequence *seq, M3Gint idx)
{
    return seq->keyframes + idx * seq->numComponents;
}

/*!
 * \internal
 * \brief Get keyframe at index -1
 *
 * \param seq       KeyframeSequence object
 * \param idx       keyframe index
 * \return keyframe value
 */
static M3G_INLINE const M3Gfloat *m3gKeyframeBefore(const KeyframeSequence *seq, M3Gint idx)
{
    return m3gKeyframeAt(seq, m3gPreviousKeyframeIndex(seq, idx));
}

/*!
 * \internal
 * \brief Get keyframe at index + 1
 *
 * \param seq       KeyframeSequence object
 * \param idx       keyframe index
 * \return keyframe value
 */
static M3G_INLINE const M3Gfloat *m3gKeyframeAfter(const KeyframeSequence *seq, M3Gint idx)
{
    return m3gKeyframeAt(seq, m3gNextKeyframeIndex(seq, idx));
}

/*!
 * \internal
 * \brief Get tangent to index
 *
 * \param seq       KeyframeSequence object
 * \param idx       keyframe index
 * \return tangent value
 */
static M3G_INLINE const M3Gfloat *m3gTangentTo(const KeyframeSequence *seq, M3Gint idx)
{
    M3G_ASSERT(seq->inTangents != NULL);
    return seq->inTangents + idx * seq->numComponents;
}

/*!
 * \internal
 * \brief Get tangent from index
 *
 * \param seq       KeyframeSequence object
 * \param idx       keyframe index
 * \return tangent value
 */
static M3G_INLINE const M3Gfloat *m3gTangentFrom(const KeyframeSequence *seq, M3Gint idx)
{
    M3G_ASSERT(seq->outTangents != NULL);
    return seq->outTangents + idx * seq->numComponents;
}

/*!
 * \internal
 * \brief Get time delta
 *
 * \param sequence  KeyframeSequence object
 * \param ind       keyframe index
 * \return time delta
 */
static M3Gint m3gTimeDelta(const KeyframeSequence *sequence, M3Gint ind)
{
    if (ind == sequence->lastValid) {
        return
            (sequence->duration
             - sequence->keyframeTimes[sequence->lastValid])
            + sequence->keyframeTimes[sequence->firstValid];
    }
    return sequence->keyframeTimes[m3gNextKeyframeIndex(sequence, ind)]
        - sequence->keyframeTimes[ind];
}

/*!
 * \internal
 * \brief Get incoming tangent scale
 *
 * \param sequence  KeyframeSequence object
 * \param ind       keyframe index
 * \return tangent scale
 */
static M3Gfloat m3gIncomingTangentScale(const KeyframeSequence *sequence,
                                        M3Gint ind)
{
    if (!sequence->closed
        && (ind == sequence->firstValid || ind == sequence->lastValid)) {
        return 0;
    }
    else {
        M3Gint prevind = m3gPreviousKeyframeIndex(sequence, ind);
        return m3gDiv(m3gMul(2.0f, (M3Gfloat) m3gTimeDelta(sequence, prevind)),
                      (M3Gfloat)(m3gTimeDelta(sequence, ind)
                                 + m3gTimeDelta(sequence, prevind)));
    }
}

/*!
 * \internal
 * \brief Get outgoing tangent scale
 *
 * \param sequence  KeyframeSequence object
 * \param ind       keyframe index
 * \return tangent scale
 */
static M3Gfloat m3gOutgoingTangentScale(const KeyframeSequence *sequence,
                                        M3Gint ind)
{
    if (!sequence->closed
        && (ind == sequence->firstValid || ind == sequence->lastValid)) {
        return 0;
    }
    else {
        M3Gint prevind = m3gPreviousKeyframeIndex(sequence, ind);
        return m3gDiv(m3gMul(2.0f, (M3Gfloat) m3gTimeDelta(sequence, ind)),
                      (M3Gfloat)(m3gTimeDelta(sequence, ind)
                                 + m3gTimeDelta(sequence, prevind)));
    }
}

/*!
 * \internal
 * \brief Precalculate all tangents
 *
 * \param sequence  KeyframeSequence object
 */
static void m3gPrecalculateTangents(KeyframeSequence *sequence)
{
    M3Gint i, kf = sequence->firstValid;
    do {
        const M3Gfloat *prev = m3gKeyframeBefore(sequence, kf);
        const M3Gfloat *next = m3gKeyframeAfter(sequence, kf);
        const M3Gfloat sIn  = m3gIncomingTangentScale(sequence, kf);
        const M3Gfloat sOut = m3gOutgoingTangentScale(sequence, kf);
        M3Gfloat *in  = (M3Gfloat *) m3gTangentTo(sequence, kf);
        M3Gfloat *out = (M3Gfloat *) m3gTangentFrom(sequence, kf);        
        
        for (i = 0; i < sequence->numComponents; ++i) {
            in[i]  = m3gMul(m3gMul(0.5f, (m3gSub(next[i], prev[i]))), sIn);
            out[i] = m3gMul(m3gMul(0.5f, (m3gSub(next[i], prev[i]))), sOut);
        }
        
        kf = m3gNextKeyframeIndex(sequence, kf);
    } while (kf != sequence->firstValid);
}

/*!
 * \internal
 * \brief Precalculate A and B
 *
 * \param sequence  KeyframeSequence object
 */
static void m3gPrecalculateAB(KeyframeSequence *sequence)
{
    Quat start, end, prev, next;
    Vec3 tangent, cfd;
    M3Gfloat temp[4]; /* used for both quats and vectors */
    
    M3Gint kf = sequence->firstValid;
    do {

        m3gSetQuat(&prev, m3gKeyframeBefore(sequence, kf));
        m3gSetQuat(&start, m3gKeyframeAt(sequence, kf));
        m3gSetQuat(&end, m3gKeyframeAfter(sequence, kf));
        m3gSetQuat(&next, m3gKeyframeAfter(sequence, m3gNextKeyframeIndex(sequence, kf)));

        /* Compute the centered finite difference at this
           keyframe; note that this would be the tangent for basic
           Catmull-Rom interpolation. */

        m3gLogDiffQuat(&cfd, &start, &end);
        m3gLogDiffQuat((Vec3*)temp, &prev, &start);
        m3gAddVec3(&cfd, (Vec3*)temp);
        m3gScaleVec3(&cfd, 0.5f);

        /* Compute the outgoing tangent, scaled to compensate for
           keyframe timing, then compute the "A" intermediate
           quaternion. */

        tangent = cfd;
        m3gScaleVec3(&tangent, m3gOutgoingTangentScale(sequence, kf));

        m3gLogDiffQuat((Vec3*)temp, &start, &end);
        m3gSubVec3(&tangent, (Vec3*)temp);
        m3gScaleVec3(&tangent, 0.5f);
        m3gExpQuat((Quat*)temp, &tangent);
        sequence->a[kf] = start;
        m3gMulQuat(&(sequence->a[kf]), (Quat*)temp);

        /* Then repeat the same steps for the incoming tangent and
           the "B" intermediate quaternion. */

        tangent = cfd;
        m3gScaleVec3(&tangent, m3gIncomingTangentScale(sequence, kf));

        m3gLogDiffQuat((Vec3*)temp, &prev, &start);
        m3gSubVec3((Vec3*)temp, &tangent);
        m3gScaleVec3((Vec3*)temp, 0.5f);
        m3gExpQuat((Quat*)temp, (Vec3*)temp);
        sequence->b[kf] = start;
        m3gMulQuat(&(sequence->b[kf]), (Quat*)temp);
        
        kf = m3gNextKeyframeIndex(sequence, kf);
    } while (kf != sequence->firstValid);
}

/*!
 * \internal
 * \brief Update all tangents
 *
 * \param sequence  KeyframeSequence object
 */
static void m3gUpdateTangents(KeyframeSequence *sequence)
{
    if (sequence->interpolation == M3G_SPLINE) {
        m3gPrecalculateTangents(sequence);
    }
    else if (sequence->interpolation == M3G_SQUAD) {
        m3gPrecalculateAB(sequence);
    }
}

/*!
 * \internal
 * \brief Linear interpolate
 *
 * \param sequence      KeyframeSequence object
 * \param sample        input samples
 * \param s             speed
 * \param startIndex    start index
 * \param endIndex      end index
 */
static M3G_INLINE void m3gLerpSample(const KeyframeSequence *sequence,
                                     M3Gfloat *sample,
                                     M3Gfloat s,
                                     M3Gint startIndex, M3Gint endIndex)
{
    const M3Gfloat *start = m3gKeyframeAt(sequence, startIndex);
    const M3Gfloat *end   = m3gKeyframeAt(sequence, endIndex);

    m3gLerp(sequence->numComponents, sample, s, start, end);
}

/*!
 * \internal
 * \brief Spline interpolate
 *
 * \param sequence      KeyframeSequence object
 * \param sample        input samples
 * \param s             speed
 * \param startIndex    start index
 * \param endIndex      end index
 */
static M3G_INLINE void m3gSplineSample(const KeyframeSequence *sequence,
                                       M3Gfloat *sample,
                                       M3Gfloat s,
                                       M3Gint startIndex, M3Gint endIndex)
{
    const M3Gfloat *start, *end;
    const M3Gfloat *tStart, *tEnd;
    
    /* Get the required keyframe values and the (one-sided) tangents
     * at the ends of the segment. */
    
    start = m3gKeyframeAt(sequence, startIndex);
    end   = m3gKeyframeAt(sequence, endIndex);

    tStart = m3gTangentFrom(sequence, startIndex);
    tEnd = m3gTangentTo(sequence, endIndex);

    /* Interpolate the final value using a Hermite spline. */

    m3gHermite(sequence->numComponents, sample, s, start, end, tStart, tEnd);
}

/*!
 * \internal
 * \brief Spherical linear interpolate
 *
 * \param sequence      KeyframeSequence object
 * \param sample        input samples
 * \param s             speed
 * \param startIndex    start index
 * \param endIndex      end index
 */
static M3G_INLINE void m3gSlerpSample(const KeyframeSequence *sequence,
                                      M3Gfloat *sample,
                                      M3Gfloat s,
                                      M3Gint startIndex, M3Gint endIndex)
{
    if (sequence->numComponents != 4) {
        m3gRaiseError(M3G_INTERFACE(sequence), M3G_INVALID_VALUE);
        return;
    }
    
    m3gSlerpQuat((Quat *) sample,
                 s,
                 (const Quat *) m3gKeyframeAt(sequence, startIndex),
                 (const Quat *) m3gKeyframeAt(sequence, endIndex));
}

/*!
 * \internal
 * \brief Spline interpolate quats
 *
 * \param sequence      KeyframeSequence object
 * \param sample        input samples
 * \param s             speed
 * \param startIndex    start index
 * \param endIndex      end index
 */
static M3G_INLINE void m3gSquadSample(const KeyframeSequence *sequence,
                                      M3Gfloat *sample,
                                      M3Gfloat s,
                                      M3Gint startIndex, M3Gint endIndex)
{
    if (sequence->numComponents != 4) {
        m3gRaiseError(M3G_INTERFACE(sequence), M3G_INVALID_VALUE);
        return;
    }

    m3gSquadQuat((Quat *) sample,
                 s,
                 (const Quat *) m3gKeyframeAt(sequence, startIndex),
                 &(sequence->a[startIndex]),
                 &(sequence->b[endIndex]),
                 (const Quat *) m3gKeyframeAt(sequence, endIndex));
}

/*!
 * \internal
 * \brief Get sample
 *
 * \param sequence      KeyframeSequence object
 * \param time          time
 * \param sample        pointer to sample
 * \return              sample validity
 */
static M3Gint m3gGetSample(KeyframeSequence *sequence,
                           M3Gint time,
                           M3Gfloat *sample)
{
    M3Gint start, end, i;
    const M3Gfloat *value;
    M3Gfloat s;
    
    M3G_VALIDATE_OBJECT(sequence);
    
    if (sequence->dirty == M3G_TRUE) {
        if (!m3gValidSequence(sequence)) {
            m3gRaiseError(M3G_INTERFACE(sequence), M3G_INVALID_OPERATION);
            return 0;
        }
        m3gUpdateTangents(sequence);
        sequence->dirty = M3G_FALSE;
        sequence->probablyNext = sequence->firstValid;
    }

    /* First, map the time to the valid range of a repeating
       sequence, or handle the special end cases of an open-ended
       sequence. */
    
    if (sequence->closed) {
        if (time < 0)
            time = (time % sequence->duration) + sequence->duration;
        else
            time = time % sequence->duration;
        if (time < sequence->keyframeTimes[sequence->firstValid]) {
            time += sequence->duration;
        }
    }
    else {
        if (time < sequence->keyframeTimes[sequence->firstValid]) {
            value = m3gKeyframeAt(sequence, sequence->firstValid);
            for (i = 0; i < sequence->numComponents; i++)
                sample[i] = value[i];
            return (sequence->keyframeTimes[sequence->firstValid] - time);
        }
        else if (time >= sequence->keyframeTimes[sequence->lastValid]) {
            value = m3gKeyframeAt(sequence, sequence->lastValid);
            for (i = 0; i < sequence->numComponents; i++)
                sample[i] = value[i];
            /* \ define a meaningful constant */
            return 0x7FFFFFFF;
        }
    }

    /* Search for the starting keyframe of the segment to
       interpolate. Starting the search from the previously
       used keyframe, we are very likely to find the match
       sooner than if we'd start from the first keyframe. */

    start = sequence->probablyNext;
    if (sequence->keyframeTimes[start] > time)
        start = sequence->firstValid;
    while (start != sequence->lastValid &&
           sequence->keyframeTimes[m3gNextKeyframeIndex(sequence, start)] <= time) {
        start = m3gNextKeyframeIndex(sequence, start);
    }
    sequence->probablyNext = start;
    
    /* Calculate the interpolation factor if necessary; the quick
       exit also avoids a division by zero in the case that we
       have a quirky sequence with only multiple coincident
       keyframes. */

    if (time == sequence->keyframeTimes[start] || sequence->interpolation == M3G_STEP) {
        value = m3gKeyframeAt(sequence, start);
        for (i = 0; i < sequence->numComponents; i++)
            sample[i] = value[i];
        return (sequence->interpolation == M3G_STEP)
               ? (m3gTimeDelta(sequence, start) - (time - sequence->keyframeTimes[start]))
               : 1;
    }
    s = m3gDivif(time - sequence->keyframeTimes[start], m3gTimeDelta(sequence, start));
    
    /* Pick the correct interpolation function and pass the
       segment start and end keyframe indices. */

    end = m3gNextKeyframeIndex(sequence, start);

    switch (sequence->interpolation) {
    case M3G_LINEAR:
        m3gLerpSample(sequence, sample, s, start, end);
        break;
    case M3G_SLERP:
        m3gSlerpSample(sequence, sample, s, start, end);
        break;
    case M3G_SPLINE:
        m3gSplineSample(sequence, sample, s, start, end);
        break;
    case M3G_SQUAD:
        m3gSquadSample(sequence, sample, s, start, end);
        break;
    default:
        m3gRaiseError(M3G_INTERFACE(sequence), M3G_INVALID_ENUM);
        return 0;
    }

    return 1;
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const ObjectVFTable m3gvf_KeyframeSequence = {
    m3gObjectApplyAnimation,
    m3gObjectIsCompatible,
    m3gObjectUpdateProperty,
    m3gObjectDoGetReferences,
    m3gObjectFindID,
    m3gKeyframeSequenceDuplicate,
    m3gDestroyKeyframeSequence
};


/*----------------------------------------------------------------------
 * Public API functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a new KeyframeSequence with default values
 * 
 * \param hInterface            M3G interface
 * \param numKeyframes          number of keyframes
 * \param numComponents         number of components
 * \param interpolation         interpolation type
 * \retval KeyframeSequence new KeyframeSequence object
 * \retval NULL KeyframeSequence creating failed
 */
/*@access M3GInterface@*/
/*@access M3Gappearance@*/
M3G_API M3GKeyframeSequence m3gCreateKeyframeSequence(M3GInterface hInterface,
                                                      M3Gint numKeyframes,
                                                      M3Gint numComponents,
                                                      M3Gint interpolation)
{
    Interface *m3g = (Interface *) hInterface;
    M3G_VALIDATE_INTERFACE(m3g);

    if (numKeyframes < 1 || numComponents < 1
        || interpolation < M3G_LINEAR || interpolation > M3G_STEP
        || ((interpolation == M3G_SLERP || interpolation == M3G_SQUAD)
            && numComponents != 4)) {
        m3gRaiseError(m3g, M3G_INVALID_VALUE);
        return NULL;
    }

    {
        KeyframeSequence *sequence = m3gAllocZ(m3g, sizeof(KeyframeSequence));

        if (sequence != NULL) {
            if (m3gInitKeyframeSequence(m3g,
                                        sequence,
                                        numKeyframes, numComponents,
                                        interpolation) == NULL) {
                m3gFree(m3g, sequence);
                return NULL;
            }
        }

        return (M3GKeyframeSequence) sequence;
    }
}

/*!
 * \brief Assigns a time and value to a keyframe sequence entry
 *
 * \param handle    handle of the keyframe sequence object
 * \param ind       index of the entry to set
 * \param time      time to set in the entry
 * \param valueSize number of elements in the value; this must match
 *                  the number of elements given when constructing
 *                  the sequence
 * \param value     pointer to an array of \c valueSize floats
 */
M3G_API void m3gSetKeyframe(M3GKeyframeSequence handle,
                            M3Gint ind,
                            M3Gint time,
                            M3Gint valueSize, const M3Gfloat *value)
{
    KeyframeSequence *sequence = (KeyframeSequence *)handle;
    M3G_VALIDATE_OBJECT(sequence);

    /* Check for invalid inputs */
    
    if (value == NULL) {
        m3gRaiseError(M3G_INTERFACE(sequence), M3G_NULL_POINTER);
        return;
    }
    if (valueSize < sequence->numComponents || time < 0) {
        m3gRaiseError(M3G_INTERFACE(sequence), M3G_INVALID_VALUE);
        return;
    }
    if (ind < 0 || ind >= sequence->numKeyframes) {
        m3gRaiseError(M3G_INTERFACE(sequence), M3G_INVALID_INDEX);
        return;
    }

    /* Assign  the  time  and  value. Quaternion  keyframes  are  also
     * normalized, as indicated in the specification. */    
    {
        M3Gfloat *kf = (M3Gfloat *) m3gKeyframeAt(sequence, ind);
        int c;
        
        sequence->keyframeTimes[ind] = time;
        
        for (c = 0; c < sequence->numComponents; ++c) {
            kf[c] = value[c];
        }

        if (sequence->interpolation == M3G_SLERP
            || sequence->interpolation == M3G_SQUAD) {
            m3gNormalizeQuat((Quat*) kf);
        }
    }
    
    sequence->dirty = M3G_TRUE;        
}

/*!
 * \brief Set valid range
 *
 * \param handle    handle of the keyframe sequence object
 * \param first     first valid keyframe
 * \param last      last valid keyframe
 */
M3G_API void m3gSetValidRange(M3GKeyframeSequence handle,
                              M3Gint first, M3Gint last)
{
    KeyframeSequence *sequence = (KeyframeSequence *)handle;
    M3G_VALIDATE_OBJECT(sequence);

    if (first < 0 || first >= sequence->numKeyframes ||
        last < 0 || last >= sequence->numKeyframes) {
        m3gRaiseError(M3G_INTERFACE(sequence), M3G_INVALID_INDEX);
        return;
    }

    sequence->firstValid = first;
    sequence->lastValid = last;
    sequence->dirty = M3G_TRUE;
}

/*!
 * \brief Set duration
 *
 * \param handle    handle of the keyframe sequence object
 * \param duration  duration
 */
M3G_API void m3gSetDuration(M3GKeyframeSequence handle, M3Gint duration)
{
    KeyframeSequence *sequence = (KeyframeSequence *)handle;
    M3G_VALIDATE_OBJECT(sequence);

    /* Check for errors */
    if (duration <= 0) {
        m3gRaiseError(M3G_INTERFACE(sequence), M3G_INVALID_VALUE);
        return;
    }

    sequence->duration = duration;
    sequence->dirty = M3G_TRUE;
}

/*!
 * \brief Get duration
 *
 * \param handle    handle of the keyframe sequence object
 * \return          duration
 */
M3G_API M3Gint m3gGetDuration(M3GKeyframeSequence handle)
{
    KeyframeSequence *sequence = (KeyframeSequence *)handle;
    M3G_VALIDATE_OBJECT(sequence);
    return sequence->duration;
}

/*!
 * \brief Get component count
 *
 * \param handle    handle of the keyframe sequence object
 * \return          component count
 */
M3G_API M3Gint m3gGetComponentCount(M3GKeyframeSequence handle)
{
    KeyframeSequence *sequence = (KeyframeSequence *)handle;
    M3G_VALIDATE_OBJECT(sequence);
    return sequence->numComponents;
}

/*!
 * \brief Get interpolation type
 *
 * \param handle    handle of the keyframe sequence object
 * \return          interpolation type
 */
M3G_API M3Gint m3gGetInterpolationType(M3GKeyframeSequence handle)
{
    KeyframeSequence *sequence = (KeyframeSequence *)handle;
    M3G_VALIDATE_OBJECT(sequence);
    return sequence->interpolation;
}

/*!
 * \brief Get keyframe value
 *
 * \param handle     handle of the keyframe sequence object
 * \param frameIndex keyframe index
 * \param value      value array
 
 * \return           time value of the keyframe
 */
M3G_API M3Gint m3gGetKeyframe  (M3GKeyframeSequence handle, M3Gint frameIndex, M3Gfloat *value)
{
    KeyframeSequence *sequence = (KeyframeSequence *)handle;
    M3G_VALIDATE_OBJECT(sequence);

    if (frameIndex < 0 || frameIndex >= sequence->numKeyframes) {
        m3gRaiseError(M3G_INTERFACE(sequence), M3G_INVALID_INDEX);
        return 0;
    }

    if (value != NULL) {
        m3gCopy(    value,
                    sequence->keyframes + frameIndex * sequence->numComponents,
                    sequence->numComponents * sizeof(M3Gfloat));
    }

    return sequence->keyframeTimes[frameIndex];
}

/*!
 * \brief Get keyframe count
 *
 * \param handle    handle of the keyframe sequence object
 * \return          keyframe count
 */
M3G_API M3Gint m3gGetKeyframeCount(M3GKeyframeSequence handle)
{
    KeyframeSequence *sequence = (KeyframeSequence *)handle;
    M3G_VALIDATE_OBJECT(sequence);
    return sequence->numKeyframes;
}

/*!
 * \brief Get valid range
 *
 * \param handle    handle of the keyframe sequence object
 * \param first     pointer to valid range start
 * \param last      pointer to valid range end
 */
M3G_API void m3gGetValidRange(M3GKeyframeSequence handle, M3Gint *first, M3Gint *last)
{
    KeyframeSequence *sequence = (KeyframeSequence *)handle;
    M3G_VALIDATE_OBJECT(sequence);
    *first = sequence->firstValid;
    *last = sequence->lastValid;
}

/*!
 * \brief Set repeat mode
 *
 * \param handle    handle of the keyframe sequence object
 * \param mode      repeat mode
 */
M3G_API void m3gSetRepeatMode(M3GKeyframeSequence handle, M3Genum mode)
{
    KeyframeSequence *sequence = (KeyframeSequence *)handle;
    M3G_VALIDATE_OBJECT(sequence);
    if (mode != M3G_CONSTANT && mode != M3G_LOOP) {
        m3gRaiseError(M3G_INTERFACE(sequence), M3G_INVALID_ENUM);
        return;
    }
    sequence->closed = (mode == M3G_LOOP) ? M3G_TRUE : M3G_FALSE;
}

/*!
 * \brief Get repeat mode
 *
 * \param handle    handle of the keyframe sequence object
 * \return          repeat mode
 */
M3G_API M3Genum m3gGetRepeatMode(M3GKeyframeSequence handle)
{
    KeyframeSequence *sequence = (KeyframeSequence *)handle;
    M3G_VALIDATE_OBJECT(sequence);
    return (sequence->closed ? M3G_LOOP : M3G_CONSTANT);
}

