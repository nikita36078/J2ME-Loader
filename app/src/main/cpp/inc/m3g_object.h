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
* Description: M3G base object class internal interface
*
*/

#ifndef __M3G_OBJECT_H__
#define __M3G_OBJECT_H__

/*!
 * \internal
 * \file
 * \brief M3G base object class internal interface
 *
 * The fundamental feature of the object model is that each object
 * instance structure includes the base class structure as its first
 * member.  Consequently, pointers to derived classes can be resolved
 * to pointers to base classes by simple casts, and things such as
 * virtual function pointers can be found at a fixed offset regardless
 * of the actual class of the object being dealt with.
 *
 * The per-class virtual function tables are laid out similarly to the
 * class structures, with the base class table preceding the derived
 * class table in memory. Currently, virtual function tables are
 * constructed by hand, but they are only needed for non-abstract
 * classes.
 */

#include "m3g_interface.h"
#include "m3g_array.h"

/*----------------------------------------------------------------------
 * Object class definition
 *--------------------------------------------------------------------*/

typedef M3Gint  (*m3gApplyAnimationFuncPtr)     (Object *self, M3Gint time);
typedef M3Gbool (*m3gIsCompatibleFuncPtr)       (M3Gint property);
typedef void    (*m3gUpdatePropertyFuncPtr)     (Object *self, M3Gint property, M3Gint valueSize, const M3Gfloat *value);
typedef M3Gint  (*m3gGetReferencesFuncPtr)      (Object *self, Object **references);
typedef Object* (*m3gFindFuncPtr)               (Object *self, M3Gint userID);
typedef M3Gbool (*m3gDuplicateFuncPtr)          (const Object *original, Object **clone, Object **pairs, M3Gint *numPairs);

typedef void    (*m3gDestroyFuncPtr)            (Object *obj);

/*!
 * \internal
 * \brief Object class virtual functions
 */
typedef struct
{
    m3gApplyAnimationFuncPtr    applyAnimation;
    m3gIsCompatibleFuncPtr      isCompatible;
    m3gUpdatePropertyFuncPtr    updateProperty;
    m3gGetReferencesFuncPtr     getReferences;
    m3gFindFuncPtr              find;
    m3gDuplicateFuncPtr         duplicate;
    m3gDestroyFuncPtr           destroy;
} ObjectVFTable;

/*!
 * \internal
 * \brief Internal object structure
 *
 * \note Part of this is JSR-184 Object3D related and doesn't apply to
 * all native objects; namely, RenderContext does not use animation
 * tracks for anything
 */
struct M3GObjectImpl
{
    /*! \internal \brief Pointer to the interface that created this object */
    Interface *interface;
    
    /*!
     * \internal
     * \brief Class ID (as in M3GClass)
     *
     * This is used to resolve the virtual function table pointer,
     * among other things
     */
    M3Guint classID  :  8;

    /*! \internal \brief Reference count */
    M3Guint refCount : 24;
    
    /*! \internal \brief Table for animation tracks */
    PointerArray *animTracks;
    
    M3Gint userID;
};


/* Some compile-time sanity checks... */

M3G_CT_ASSERT(M3G_CLASS_WORLD <= 255);
//M3G_CT_ASSERT(sizeof(Object) == 16);


/* Self-validation */
#if defined(M3G_DEBUG)
/*@notfunction@*/
#   define M3G_VALIDATE_OBJECT(obj)   m3gValidateObject(obj)
static void m3gValidateObject(const void *pObj);

#else
#   define M3G_VALIDATE_OBJECT(obj)
#endif

/*!
 * \internal
 * \brief Returns the interface of any M3GObject-derived object
 */
/*@notfunction@*/
#define M3G_INTERFACE(obj) (((const Object *)(obj))->interface)

/*!
 * \internal
 * \brief Returns the class ID of any M3GObject-derived object
 */
/*@notfunction@*/
#define M3G_CLASS(obj) ((M3GClass)(((const Object *)(obj))->classID))

/*!
 * \internal
 * \brief Virtual function call macro
 *
 * \param className     name of class
 * \param pObj          pointer to object instance
 * \param funcName      name of function to call
 *
 */
/*@notfunction@*/
#define M3G_VFUNC(className, pObj, funcName) \
    (((className##VFTable*)m3gGetVFTable((Object*)(pObj)))->funcName)

static M3G_INLINE const ObjectVFTable *m3gGetVFTable(const Object *obj);

/*--------------------------------------------------------------------
 * Constructor
 *------------------------------------------------------------------*/

static void m3gInitObject(Object *object,
                          Interface *interface,
                          M3GClass classID);

/*-------------------------------------------------------------------
 * Internal functions
 *-----------------------------------------------------------------*/

/*! \internal \brief Nicer form for the \c find virtual function */
static M3G_INLINE Object *m3gFindID(Object *obj, M3Gint userID)
{
    return M3G_VFUNC(Object, obj, find)(obj, userID);
}

/* Reference handling */
static void m3gSetRef(Object **ref, Object *obj);
#define M3G_ASSIGN_REF(ref, value) m3gSetRef((Object**)&(ref), (Object*) value)

/* Virtual functions */
static M3Gint   m3gObjectApplyAnimation (Object *self, M3Gint time);
static M3Gbool  m3gObjectIsCompatible   (M3Gint property);
static void     m3gObjectUpdateProperty (Object *self, M3Gint property, M3Gint valueSize, const M3Gfloat *value);
static M3Gint   m3gObjectDoGetReferences(Object *self, Object **references);
static Object*  m3gObjectFindID         (Object *self, M3Gint userID);
static M3Gbool  m3gObjectDuplicate      (const Object *original, Object **clone, Object **pairs, M3Gint *numPairs);
static void     m3gDestroyObject        (Object *object);

#if defined(M3G_LOGLEVEL)
static const char *m3gClassName(M3GClass classID);
#else
/*lint -save -e607 this is intentional */
#   define m3gClassName(id) " ## id ## "
/*lint -restore */
#endif

#endif /*__M3G_OBJECT_H__*/
