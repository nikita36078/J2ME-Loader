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
* Description: Base object class implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Base object class implementation
 *
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

/*----------------------------------------------------------------------
 * Constructor & destructor
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Constructor for all Objects
 *
 * The reference count for new objects is initialized to zero; the
 * object pointer must be stored using \c m3gSetRef, or m3gAddRef
 * called explicitly, to increase this to one.
 */
static void m3gInitObject(Object *obj,
                          Interface *interface,
                          M3GClass classID)
{
    M3G_ASSERT_PTR(obj);
    M3G_VALIDATE_INTERFACE(interface);

    M3G_ASSERT(m3gInRange(classID,
                          M3G_CLASS_ANIMATION_CONTROLLER, M3G_CLASS_WORLD));
    
    obj->classID = (M3Guint) classID;
    obj->interface = interface;
    obj->animTracks = NULL;
    obj->refCount = 0u;
    
    M3G_VALIDATE_OBJECT(obj);

    m3gAddChildObject(interface, obj);
    m3gMarkObject(obj);
    
    m3gIncStat(M3G_INTERFACE(obj), M3G_STAT_OBJECTS, 1);
    M3G_LOG2(M3G_LOG_OBJECTS, "New %s 0x%08X\n",
             m3gClassName((M3GClass) obj->classID),
             (unsigned) obj);
}

/*!
 * \internal
 * \brief Destructor for all Objects
 */
static void m3gDestroyObject(Object *obj)
{
    M3G_VALIDATE_OBJECT(obj);
    M3G_ASSERT(m3gIsObject(obj));

    if (obj->animTracks != NULL) {
        int n = m3gArraySize(obj->animTracks);
        int i;

        for (i = 0; i < n; ++i) {
            M3GObject hTrk = (M3GObject) m3gGetArrayElement(obj->animTracks, i);
            m3gDeleteRef(hTrk);
        }
        m3gDestroyArray(obj->animTracks, M3G_INTERFACE(obj));
        m3gFree(obj->interface, obj->animTracks);
    }

    m3gDelChildObject(obj->interface, obj);
    m3gUnmarkObject(obj);
    
    m3gIncStat(M3G_INTERFACE(obj), M3G_STAT_OBJECTS, -1);
    M3G_LOG2(M3G_LOG_OBJECTS, "Destroyed %s 0x%08X\n",
             m3gClassName((M3GClass) obj->classID),
             (unsigned) obj);
}

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Sets an object reference to a new value and updates the
 * reference count accordingly
 *
 * Note that this may lead to the originally referenced object being
 * destroyed.
 *
 * \param ref Pointer to the reference (pointer) to set to a new value
 * \param obj New value of the reference
 */
static void m3gSetRef(Object **ref, Object *obj)
{
    M3G_ASSERT_PTR(ref);

    if (*ref != obj) {
        if (obj != NULL) {
            m3gAddRef((M3GObject) obj);
        }
        if (*ref != NULL) {
            m3gDeleteRef((M3GObject) *ref);
        }
        *ref = obj;
    }
}

#if defined(M3G_DEBUG)
/*!
 * \internal
 * \brief Checks the integrity of an Object-derived object
 */
static void m3gValidateObject(const void *pObj)
{
    const Object *obj = (const Object *) pObj;
    M3G_VALIDATE_MEMBLOCK(obj);
    M3G_VALIDATE_MEMBLOCK(obj->interface);
    M3G_ASSERT(m3gInRange(obj->classID,
                          M3G_CLASS_ANIMATION_CONTROLLER, M3G_CLASS_WORLD));
    M3G_ASSERT_PTR(m3gGetVFTable(obj));
}
#endif /* M3G_DEBUG */


/* ---------------- Internal Object3D functions ---------------- */

/*!
 * \internal
 * \brief Default \c applyAnimation function implementation
 */
static M3Gint m3gObjectApplyAnimation(Object *self, M3Gint time)
{
    Interface *m3g = M3G_INTERFACE(self);
    M3Gint validity = 0x7FFFFFFF;
    M3Gint trackIndex, numTracks;
    M3Gfloat stackSampleVector[4];
    const PointerArray *tracks = self->animTracks;

    /* Quick exit if no animation tracks */

    if (tracks == NULL) {
        return validity;
    }

    /* Loop through the tracks. Note that the tracks are ordered so
     * that tracks targeting the same property are adjacent in the
     * array; this makes animation blending easier. */

    numTracks = m3gArraySize(tracks);

    for (trackIndex = 0; trackIndex < numTracks; ) {
        const AnimationTrack *track = (const AnimationTrack *)
            m3gGetArrayElement(tracks, trackIndex);
        const KeyframeSequence *sequence = track->sequence;

        M3Gint components = sequence->numComponents;
        M3Gint property = track->property;
        M3Gint nextProperty;

        M3Gfloat sumWeights = 0;
        M3Gfloat *sumValues;

        /* Collect the contributions from all the tracks targeting the
         * same property */

        if (components <= 4) {
            sumValues = stackSampleVector;
        }
        else {
            sumValues = (M3Gfloat *)
                m3gAlloc(m3g, components * sizeof(M3Gfloat));
            if (sumValues == NULL) {
                return 0;
            }
        }

        m3gZero(sumValues, components * sizeof(M3Gfloat));

        do {
            SampleInfo sampleInfo;
            
            m3gGetContribution(track, time, sumValues, &sampleInfo);
            if (sampleInfo.validity <= 0) {
                return 0;
            }
            sumWeights += sampleInfo.weight;
            validity = M3G_MIN(validity, sampleInfo.validity);

            if (++trackIndex == numTracks) {
                break;
            }
            track = (const AnimationTrack *) m3gGetArrayElement(tracks,
                                                                trackIndex);
            nextProperty = track->property;
        } while (nextProperty == property);

        if (sumWeights > 0) {
            M3G_VFUNC(Object, self, updateProperty)(
                self, property, components, sumValues);
        }
        if (sumValues != stackSampleVector) {
            m3gFree(m3g, sumValues);
        }
    }

    return validity;
}

/*!
 * \internal
 * \brief Default \c isCompatible function implementation
 */
static M3Gbool m3gObjectIsCompatible(M3Gint property)
{
    M3G_UNREF(property);
    
    return M3G_FALSE;
}

/*!
 * \internal
 * \brief Default \c updateProperty function implementation
 *
 * Silently ignoring an update request for a non-existent object
 * property does no harm, so this just asserts in debug builds and is
 * NOP otherwise.
 */
static void m3gObjectUpdateProperty(Object *self,
                                    M3Gint property,
                                    M3Gint valueSize,
                                    const M3Gfloat *value)
{
    M3G_UNREF(self);
    M3G_UNREF(property);
    M3G_UNREF(valueSize);
    M3G_UNREF(value);
    
    M3G_ASSERT(M3G_FALSE);
}

/*!
 * \internal
 * \brief Default \c getReferences function implementation
 */
static M3Gint m3gObjectDoGetReferences(Object *self, Object **references)
{
    M3Gint i;
    if (self->animTracks != NULL) {
        if (references != NULL) {
            for (i = 0; i < m3gArraySize(self->animTracks); ++i) {
                references[i] = (Object *)m3gGetArrayElement(self->animTracks, i);
            }
        }
        return m3gArraySize(self->animTracks);
    }
    return 0;
}

/*!
 * \internal
 * \brief Default \c find implementation
 */
static Object *m3gObjectFindID(Object *self, M3Gint userID)
{
    M3Gint i;

    if (self->userID == userID) {
        return self;
    }
    
    if (self->animTracks) {
        for (i = 0; i < m3gArraySize(self->animTracks); ++i) {
            Object *found =
                m3gFindID((Object *) m3gGetArrayElement(self->animTracks, i),
                          userID);
            if (found) {
                return found;
            }
        }
    }
    
    return NULL;
}

/*!
 * \internal
 * \brief Default \c duplicate function implementation
 */
static M3Gbool m3gObjectDuplicate(const Object *original,
                                  Object **clone,
                                  Object **pairs,
                                  M3Gint *numPairs)
{
    Interface *m3g = original->interface;
    M3G_ASSERT_PTR(*clone); /* abstract class, must be derived */

    pairs[2 * (*numPairs)] = (Object *)original;
    pairs[2 * (*numPairs) + 1] = *clone;
    (*numPairs)++;

    /* Copy basic object properties */
    
    (*clone)->interface = m3g;
    (*clone)->classID   = original->classID;
    (*clone)->userID    = original->userID;

    /* Copy animation tracks.  This may fail due to out-of-memory, so
     * we check for that; clean-up will be handled by the derived
     * class method. */
    
    if (original->animTracks != NULL) {
        M3Gsizei numTracks = m3gArraySize(original->animTracks);
        M3Gint i;

        /* Allocate the track array and make sure it has enough room
         * for holding the tracks we're about to copy */
        
        PointerArray *animTracks =
            (PointerArray*) m3gAlloc(m3g, sizeof(PointerArray));
        if (animTracks == NULL) {
            return M3G_FALSE; /* out of memory */
        }        
        (*clone)->animTracks = animTracks;

        m3gInitArray(animTracks);
        if (!m3gEnsureArrayCapacity(animTracks, numTracks, m3g)) {
            return M3G_FALSE; /* out of memory */
        }                           

        /* Copy tracks one-by-one and update references.  This can no
         * longer fail, as the capacity request above has been
         * satisfied */
        
        for (i = 0; i < numTracks; ++i) {
            AnimationTrack *track =
                (AnimationTrack *) m3gGetArrayElement(original->animTracks, i);

            if (m3gArrayAppend(animTracks, track, m3g) != i) {
                M3G_ASSERT(M3G_FALSE);
            }
            m3gAddRef((Object *) track);
        }
    }
    return M3G_TRUE;
}

#if defined(M3G_LOGLEVEL)
/*!
 * \internal
 * \brief Returns the name of an object class
 */
static const char *m3gClassName(M3GClass classID)
{
    switch (classID) {
    case M3G_CLASS_ANIMATION_CONTROLLER:
        return "AnimationController";
    case M3G_CLASS_ANIMATION_TRACK:
        return "AnimationTrack";
    case M3G_CLASS_APPEARANCE:
        return "Appearance";
    case M3G_CLASS_BACKGROUND:
        return "Background";
    case M3G_CLASS_CAMERA:
        return "Camera";
    case M3G_CLASS_COMPOSITING_MODE:
        return "CompositingMode";
    case M3G_CLASS_FOG:
        return "Fog";
    case M3G_CLASS_GROUP:
        return "Group";
    case M3G_CLASS_IMAGE:
        return "Image";
    case M3G_CLASS_INDEX_BUFFER:
        return "IndexBuffer";
    case M3G_CLASS_KEYFRAME_SEQUENCE:
        return "KeyframeSequence";
    case M3G_CLASS_LIGHT:
        return "Light";
    case M3G_CLASS_LOADER:
        return "Loader";
    case M3G_CLASS_MATERIAL:
        return "Material";
    case M3G_CLASS_MESH:
        return "Mesh";
    case M3G_CLASS_MORPHING_MESH:
        return "MorphingMesh";
    case M3G_CLASS_POLYGON_MODE:
        return "PolygonMode";
    case M3G_CLASS_RENDER_CONTEXT:
        return "RenderContext";
    case M3G_CLASS_SKINNED_MESH:
        return "SkinnedMesh";
    case M3G_CLASS_SPRITE:
        return "Sprite";
    case M3G_CLASS_TEXTURE:
        return "Texture";
    case M3G_CLASS_VERTEX_ARRAY:
        return "VertexArray";
    case M3G_CLASS_VERTEX_BUFFER:
        return "VertexBuffer";
    case M3G_CLASS_WORLD:
        return "World";
    default:
        return "<abstract class?>";
    }
}
#endif /* defined(M3G_LOGLEVEL) */

/*----------------------------------------------------------------------
 * Public interface functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Deletes an M3G object
 *
 * Similarly to m3gDeleteRef, the object will still remain until all
 * references to it are deleted.  The difference from m3gDeleteRef is
 * mostly stylistic: m3gDeleteObject is meant to be called by the
 * owner of an object, while m3gDeleteRef should be used by users of
 * the object.  Functionally, they are equivalent in all normal use
 * cases.
 *
 * \note The only functional differences between m3gDeleteObject and
 * m3gDeleteRef are that m3gDeleteObject can be used on an object with
 * a reference count of zero, while m3gDeleteRef asserts against this
 * in debug builds; and m3gDeleteObject accepts a NULL object.
 */
/*@access M3GObject@*/
M3G_API void m3gDeleteObject(M3GObject hObject)
{
    Interface *m3g;
    Object *obj = (Object *) hObject;

    if (obj != NULL) {
        M3G_VALIDATE_OBJECT(obj);

        if (obj->refCount > 0) {
            m3gDeleteRef(obj);
        }
        else {
            M3G_LOG2(M3G_LOG_REFCOUNT,
                     "Deleting %s 0x%08X\n",
                     m3gClassName((M3GClass) obj->classID),
                     (unsigned) obj);
            
            m3g = obj->interface;
            M3G_VALIDATE_INTERFACE(m3g);
            
            M3G_ASSERT(m3gGetVFTable(obj)->destroy != NULL);
            
            M3G_VFUNC(Object, obj, destroy)(obj);
            m3gFree(m3g, obj);
        }
    }
}

/*!
 * \brief Notifies that a new reference to an object has been created
 *
 * An object will not be deleted while references to it exist.
 */
M3G_API void m3gAddRef(M3GObject hObject)
{
    Object *obj = (Object *) hObject;
    M3G_VALIDATE_OBJECT(obj);

    M3G_LOG3(M3G_LOG_REFCOUNT,
             "Adding ref to 0x%08X (%s), new count %u\n",
             (unsigned) obj,
             m3gClassName((M3GClass) obj->classID),
             (unsigned) (obj->refCount + 1));

    M3G_ASSERT(obj->refCount < 0xFFFFFF);
    ++obj->refCount;
}

/*!
 * \brief Notifies that a reference to an object has been deleted
 *
 * If the reference count for an object reaches zero, the object is
 * automatically destroyed.
 */
M3G_API void m3gDeleteRef(M3GObject hObject)
{
    Object *obj = (Object *) hObject;
    M3G_VALIDATE_OBJECT(obj);

    M3G_ASSERT(obj->refCount > 0);

    M3G_LOG3(M3G_LOG_REFCOUNT,
             "Deleting ref to 0x%08X (%s), new count %u\n",
             (unsigned) obj,
             m3gClassName((M3GClass) obj->classID),
             (unsigned) (obj->refCount - 1));

    if (--obj->refCount == 0) {
        m3gDeleteObject(hObject);
    }
}

/*!
 * \brief Returns the run-time class of an object
 */
M3G_API M3GClass m3gGetClass(M3GObject hObject)
{
    Object *obj = (Object *) hObject;
    M3G_VALIDATE_OBJECT(obj);
    return M3G_CLASS(obj);
}

/*!
 * \brief Returns the interface owning this object
 */
M3G_API M3GInterface m3gGetObjectInterface(M3GObject hObject)
{
    Object *obj = (Object *) hObject;
    M3G_VALIDATE_OBJECT(obj);
    return obj->interface;
}

/* ---------------- Object3D functions ---------------- */

/*!
 *
 */
M3G_API M3Gint m3gAddAnimationTrack(M3GObject hObject,
                                    M3GAnimationTrack hAnimationTrack)
{
    AnimationTrack *track = (AnimationTrack *)hAnimationTrack;
    Object *obj = (Object *) hObject;
    Interface *m3g = M3G_INTERFACE(obj);
    M3G_VALIDATE_OBJECT(obj);

    /* Check for errors */

    if (!M3G_VFUNC(Object, obj, isCompatible)(track->property)) {
        m3gRaiseError(m3g, M3G_INVALID_OBJECT);
        return -1;
    }

     /* Allocate animation track array only when adding animations for
      * the first time */

    if (obj->animTracks == NULL) {
        obj->animTracks = (PointerArray*) m3gAlloc(m3g, sizeof(PointerArray));
        if (obj->animTracks == NULL) return 0;
        m3gInitArray(obj->animTracks);
    }

    /*  The animation tracks are maintained in a sorted order based on
     *  their target property enumerations.  This keeps all tracks
     *  targeting the same property adjacent so that we can easily
     *  handle animation blending. */
    {
        PointerArray *trackArray = obj->animTracks;
        M3Gsizei numTracks = m3gArraySize(trackArray);
        M3Gint i;

        for (i = 0; i < numTracks; ++i) {

            const AnimationTrack *arrayTrack =
                (const AnimationTrack *) m3gGetArrayElement(trackArray, i);

            if (arrayTrack->property > track->property) {
                break;
            }

            if ((track == arrayTrack) ||
                (   (track->property == arrayTrack->property) &&
                    (track->sequence->numComponents != arrayTrack->sequence->numComponents))) {

                    m3gRaiseError(m3g, M3G_INVALID_OBJECT);
                    return -1;
                }
        }

        if (m3gArrayInsert(trackArray, i, track, m3g) < 0) {
            return -1;
        }
        m3gAddRef((M3GObject) track);

        return i;
    }
}

/*!
 *
 */
M3G_API void m3gRemoveAnimationTrack(M3GObject hObject,
                                     M3GAnimationTrack hAnimationTrack)
{
    AnimationTrack *track = (AnimationTrack *)hAnimationTrack;
    Object *obj = (Object *) hObject;
    M3G_VALIDATE_OBJECT(obj);

    /* Remove the track from the array, and if no tracks remain,
     * delete the array, too */

    if (track != NULL && obj->animTracks != NULL) {
        M3Gint i = m3gArrayFind(obj->animTracks, track);

        if (i != -1) {
            m3gArrayDelete(obj->animTracks, i);
            m3gDeleteRef((Object *) track);

            if (m3gArraySize(obj->animTracks) == 0) {
                m3gDestroyArray(obj->animTracks, M3G_INTERFACE(obj));
                m3gFree(M3G_INTERFACE(obj), obj->animTracks);
                obj->animTracks = NULL;
            }
        }
    }
}

/*!
 *
 */
M3G_API M3Gint m3gGetAnimationTrackCount(M3GObject hObject)
{
    Object *obj = (Object *) hObject;
    M3G_VALIDATE_OBJECT(obj);

    return (obj->animTracks == NULL ? 0 : m3gArraySize(obj->animTracks));
}

M3G_API M3GAnimationTrack m3gGetAnimationTrack(M3GObject hObject, M3Gint idx)
{
    Object *obj = (Object *) hObject;
    M3G_VALIDATE_OBJECT(obj);

    /* idx must be in range [0, to size of array - 1] */
    if (obj->animTracks == NULL
            || !m3gInRange(idx, 0, m3gArraySize(obj->animTracks) - 1)) {
        m3gRaiseError(M3G_INTERFACE(obj), M3G_INVALID_INDEX);
        return NULL;
    }

    return (M3GAnimationTrack) m3gGetArrayElement(obj->animTracks, idx);
}

M3G_API M3Gint m3gAnimate(M3GObject hObject, M3Gint time)
{
    M3Gint validity;
    Object *obj = (Object *) hObject;

    M3G_LOG2(M3G_LOG_STAGES,
             "Animating %s 0x%08X\n",
             m3gClassName((M3GClass) obj->classID), (unsigned) obj);
    
    M3G_VALIDATE_OBJECT(obj);
    
    M3G_BEGIN_PROFILE(M3G_INTERFACE(obj), M3G_PROFILE_ANIM);
    validity = M3G_VFUNC(Object, obj, applyAnimation)(obj, time);
    M3G_END_PROFILE(M3G_INTERFACE(obj), M3G_PROFILE_ANIM);
    
    return validity;
}

/*!
 * \brief Sets userID for this object
*/
M3G_API void m3gSetUserID(M3GObject hObject, M3Gint userID)
{
    Object *obj = (Object *) hObject;
    M3G_VALIDATE_OBJECT(obj);
	obj->userID = userID;
}

/*!
 * \brief Gets userID of this object
*/
M3G_API M3Gint m3gGetUserID(M3GObject hObject)
{
    Object *obj = (Object *) hObject;
    M3G_VALIDATE_OBJECT(obj);

	return obj->userID;
}

/*!
 * \brief Creates a duplicate of this Object3D
*/
M3G_API M3GObject m3gDuplicate(M3GObject hObject, M3GObject *hReferences)
{
    Object **references = (Object **)hReferences;
    const Object *obj = (const Object *) hObject;
    Object *clone = NULL;
    M3Gint numRef = 0;

    M3G_LOG2(M3G_LOG_STAGES|M3G_LOG_OBJECTS,
             "Duplicating %s 0x%08X\n",
             m3gClassName((M3GClass) obj->classID), (unsigned) obj);

    M3G_VALIDATE_OBJECT(obj);
    
    /* Clone the object (or subtree) */
    if (!M3G_VFUNC(Object, obj, duplicate)(obj, &clone, references, &numRef)) {
        m3gDeleteObject(clone);
        return NULL; /* failed; out of memory will have been thrown */
    }

    /* NOTE This will have to change (the virtual function moved to
     * the Object class) if we add classes where child objects may get
     * duplicated */
    
    if (clone->classID == M3G_CLASS_CAMERA ||
        clone->classID == M3G_CLASS_GROUP ||
        clone->classID == M3G_CLASS_WORLD ||
        clone->classID == M3G_CLASS_LIGHT ||
        clone->classID == M3G_CLASS_MESH ||
        clone->classID == M3G_CLASS_MORPHING_MESH ||
        clone->classID == M3G_CLASS_SKINNED_MESH ||
        clone->classID == M3G_CLASS_SPRITE)
        M3G_VFUNC(Node, clone, updateDuplicateReferences)((Node *)obj, references, numRef);

    return clone;
}

/*!
 * \brief Checks the length of the references array and calls virtual
 * getReferences
 */
M3G_API M3Gint m3gGetReferences(M3GObject hObject,
                                M3GObject *references,
                                M3Gint length)
{
    Object *obj = (Object *) hObject;
    M3G_VALIDATE_OBJECT(obj);
    if (references != NULL) {
        int num = M3G_VFUNC(Object, obj, getReferences)(obj, NULL);
        if (length < num) {
            m3gRaiseError(obj->interface, M3G_INVALID_OBJECT);
            return 0;
        }
    }
    return M3G_VFUNC(Object, obj, getReferences)(obj, (Object **)references);
}

/*!
 * \brief Uses m3gGetReferences to find given userID
 */
M3G_API M3GObject m3gFind(M3GObject hObject, M3Gint userID)
{
    Object *obj = (Object *) hObject;

    M3G_LOG3(M3G_LOG_STAGES, "Finding ID 0x%08X (%d) in 0x%08X\n",
             (unsigned) userID, userID, (unsigned) obj);
    
    M3G_VALIDATE_OBJECT(obj);

    if (obj->userID == userID) {
        return obj;
    }
    
    return M3G_VFUNC(Object, obj, find)(obj, userID);
}

