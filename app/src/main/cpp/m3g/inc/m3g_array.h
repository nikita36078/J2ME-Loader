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
* Description: Dynamic pointer array
*
*/

#ifndef __M3G_ARRAY_H__
#define __M3G_ARRAY_H__

/*!
 * \internal
 * \file
 * \brief Dynamic pointer array
 */

/*!
 * \internal
 * \brief Dynamic array structure
 *
 * Prior to using an array, the structure must be initialized by
 * either calling \c m3gInitArray or explicitly clearing the structure
 * to all zeros.
 */
typedef struct
{
    M3Gsizei size, capacity;
    void **items;
} PointerArray;

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static M3Gint   m3gArrayAppend  (PointerArray *array,
                                 void *item,
                                 Interface *m3g);
static void     m3gArrayDelete  (PointerArray *array, M3Gint idx);
static M3Gint   m3gArrayInsert  (PointerArray *array,
                                 M3Gint idx,
                                 void *item,
                                 Interface *m3g);
static M3Gint   m3gArrayFind    (const PointerArray *array, void *item);
#if 0 /* currently disabled, but available if needed */
static M3Gbool  m3gArrayRemove  (PointerArray *array, void *item);
#endif
static M3Gsizei m3gArraySize    (const PointerArray *array);
static void     m3gClearArray   (PointerArray *array);
static void     m3gDestroyArray (PointerArray *array, Interface *m3g);
static M3Gbool  m3gEnsureArrayCapacity(PointerArray *array,
                                       M3Gsizei capacity,
                                       Interface *m3g);
static void*    m3gGetArrayElement(const PointerArray *array, M3Gint idx);
static void     m3gInitArray    (PointerArray *array);
#if defined(M3G_NATIVE_LOADER) /* currently only used in loader */
static void     m3gSetArrayElement(PointerArray *array,
                                   M3Gint idx,
                                   void *item);
#endif
#if 0 /* currently unused, but available here */
static M3Gbool  m3gTrimArray    (PointerArray *array, Interface *m3g);
#endif

/*----------------------------------------------------------------------
 * Inline functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Returns the number of elements in the array
 */
static M3G_INLINE M3Gsizei m3gArraySize(const PointerArray *array)
{
    M3G_ASSERT_PTR(array);
    M3G_ASSERT(array->size <= array->capacity);
    return array->size;
}

/*!
 * \internal
 * \brief Returns the array element at \c index
 */
static M3G_INLINE void *m3gGetArrayElement(const PointerArray *array,
                                           M3Gint idx)
{
    M3G_ASSERT_PTR(array);
    M3G_ASSERT(idx >= 0 && idx < array->size);
    return array->items[idx];
}

#if defined(M3G_NATIVE_LOADER) /* currently only used in loader */
/*!
 * \internal
 * \brief Sets the value of the array element at \c index
 */
static M3G_INLINE void m3gSetArrayElement(PointerArray *array,
                                          M3Gint idx,
                                          void *item)
{
    M3G_ASSERT_PTR(array);
    M3G_ASSERT(idx >= 0 && idx < array->size);
    array->items[idx] = item;
}
#endif

#endif /*__M3G_ARRAY_H__*/
