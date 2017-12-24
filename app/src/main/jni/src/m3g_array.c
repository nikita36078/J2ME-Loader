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
* Description: Dynamic pointer array implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Dynamic pointer array implementation
 *
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_array.h"


/* Define a minimum (default) capacity and a maximum amount of growth
 * for the array, to try and keep memory usage reasonable */

#define MIN_CAPACITY 8  /* 32 bytes */
#define MAX_GROWTH 1024 /* 4 KB */

M3G_CT_ASSERT(MIN_CAPACITY < MAX_GROWTH);

/*----------------------------------------------------------------------
 * Private functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Resizes an array to the given size
 */
static M3Gbool m3gReallocateArray(PointerArray *array,
                                  M3Gsizei newCapacity,
                                  Interface *m3g)
{
    void **newItems;
    M3G_VALIDATE_INTERFACE(m3g);
    
    /* Allocate the new data block */
    
    newItems = m3gAlloc(m3g, (M3Gsize) newCapacity * sizeof(void*));
    if (newItems == NULL) {
        return M3G_FALSE; /* automatic out of memory raised by m3gAlloc */
    }

    /* Copy array contents */
    
    if (array->items != NULL) {
        int i;
        M3G_ASSERT(array->size <= newCapacity);
        
        for (i = 0; i < array->size; ++i) {
            newItems[i] = array->items[i];
        }
        m3gFree(m3g, array->items);
    }

    array->capacity = newCapacity;
    array->items = newItems;
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Increases the capacity of the array
 *
 * Array growth is limited by the \c MAX_GROWTH constant, to avoid
 * blowing memory management for very large arrays. Small arrays are
 * always grown to the next power of two, to allow for easier
 * recycling of memory blocks.
 */
static M3Gbool m3gGrowArray(PointerArray *array, Interface *m3g)
{
    M3Gsizei capacity = array->capacity;
    M3Gsizei newCapacity;

    /* Calculate the new capacity for the array */

    if (capacity >= MIN_CAPACITY) {
        if (capacity < MAX_GROWTH) {
            newCapacity = MIN_CAPACITY << 1;
            while (newCapacity <= capacity) {
                newCapacity <<= 1;
            }
        }
        else {
            newCapacity = capacity + MAX_GROWTH;
        }
    }
    else {
        newCapacity = MIN_CAPACITY;
    }

    /* Reallocate the array to the new capacity */

    return m3gReallocateArray(array, newCapacity, m3g);
}

/*----------------------------------------------------------------------
 * M3G internal functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Appends a new item to the end of the array
 *
 * Grows the array if necessary, by allocating new data from the
 * interface \c m3g.
 */
static M3Gint m3gArrayAppend(PointerArray *array, void *item, Interface *m3g)
{
    M3G_VALIDATE_PTR(array);
    M3G_ASSERT(array->size <= array->capacity);
    if (array->size == array->capacity) {
        if (!m3gGrowArray(array, m3g)) {
            return -1;
        }
    }
    array->items[array->size] = item;
    return (array->size++);
}

/*!
 * \internal
 * \brief Deletes an element in the array
 *
 * All subsequent elements will move back to fill the gap, and the
 * size of the array decremented by one.
 */
static void m3gArrayDelete(PointerArray *array, M3Gint idx)
{
    int i, n;
    M3G_VALIDATE_PTR(array);
    M3G_ASSERT(idx >= 0 && idx < array->size);

    n = --array->size;
    for (i = idx; i < n; ++i) {
        array->items[i] = array->items[i+1];
    }

    M3G_ASSERT(array->size >= 0);
}

/*!
 * \internal
 * \brief Finds the first occurrence of an item in the array
 *
 * \return index of \c item, or -1 if not found
 */
static M3Gint m3gArrayFind(const PointerArray *array, void *item)
{
    int i;
    M3G_VALIDATE_PTR(array);

    for (i = 0; i < array->size; ++i) {
        if (array->items[i] == item) {
            return i;
        }
    }
    return -1;
}

/*!
 * \internal
 * \brief Inserts an element into the array
 *
 * All subsequent elements move forward by one index.
 */
static M3Gint m3gArrayInsert(PointerArray *array,
                             M3Gint idx,
                             void *item,
                             Interface *m3g)
{
    int i;
    M3G_VALIDATE_PTR(array);
    M3G_ASSERT(idx >= 0 && idx <= array->size);
    
    if (array->size == array->capacity) {
        if (!m3gGrowArray(array, m3g)) {
            return -1;
        }
    }

    for (i = array->size++; i > idx; --i) {
        array->items[i] = array->items[i-1];
    }
    array->items[idx] = item;
    return idx;
}

#if 0 /* currently unused, but available here */
/*!
 * \internal
 * \brief Removes the first instance of an item from the array
 *
 * \return true if \c item found, false otherwise
 */
static M3Gbool m3gArrayRemove(PointerArray *array, void *item)
{
    M3Gint idx = m3gArrayFind(array, item);
    if (idx >= 0) {
        m3gArrayDelete(array, idx);
        return M3G_TRUE;
    }
    return M3G_FALSE;
}
#endif

/*!
 * \internal
 * \brief Clears all array elements
 *
 * Does not affect the capacity of the array
 */
static void m3gClearArray(PointerArray *array)
{
    M3G_VALIDATE_PTR(array);
    array->size = 0;
}

/*!
 * \internal
 * \brief Destroys the array, freeing any resources allocated
 */
static void m3gDestroyArray(PointerArray *array, Interface *m3g)
{
    M3G_VALIDATE_PTR(array);
    m3gFree(m3g, array->items);
    array->items = NULL;
}

/*!
 * \internal
 * \brief Initializes an array prior to its first use
 *
 * \note This is also accomplished by clearing the array structure to
 * zero
 */
static void m3gInitArray(PointerArray *array)
{
    M3G_VALIDATE_PTR(array);
    m3gZero(array, sizeof(PointerArray));
}

/*!
 * \internal
 * \brief Ensures that the array has a specified capacity
 */
static M3Gbool m3gEnsureArrayCapacity(PointerArray *array,
                                      M3Gsizei capacity,
                                      Interface *m3g)
{
    M3G_VALIDATE_PTR(array);    
    if (array->capacity < capacity) {
        return m3gReallocateArray(array, capacity, m3g);
    }
    return M3G_TRUE;
}

#if 0 /* currently unused, but available here */
/*!
 * \internal
 * \brief Minimizes the memory usage of the array
 */
static M3Gbool m3gTrimArray(PointerArray *array, Interface *m3g)
{
    M3G_VALIDATE_PTR(array);
    M3G_ASSERT(array->size <= array->capacity);
    return m3gReallocateArray(array, array->size, m3g);
}
#endif


#undef MIN_CAPACITY
#undef MAX_GROWTH

