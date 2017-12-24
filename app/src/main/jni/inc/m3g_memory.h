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
* Description: Internal memory management function declarations
*
*/

#ifndef __M3G_MEMORY_H__
#define __M3G_MEMORY_H__

#if defined(__cplusplus)
extern "C" {
#endif
    
/*!
 * \internal
 * \file
 * \brief Internal memory management function declarations
 */

#include "M3G/m3g_core.h"
#include "m3g_defs.h"

#if !defined(M3G_NO_STDLIB)
#   include <string.h>
#endif
    
/*----------------------------------------------------------------------
 * Internal API
 *--------------------------------------------------------------------*/
    
/*!
 * \internal
 * \brief Fills a block of memory with a value
 * 
 * \param ptr pointer to the block to fill
 * \param size length of the block, in bytes
 */
static M3G_INLINE void m3gFill(void *ptr, size_t size, M3Gbyte value)/*@modifies *ptr@*/
{
#if defined(M3G_NO_STDLIB)
    M3Gbyte *pb = (M3Gbyte*) ptr;
    while(size--) {
        *pb++ = value;
    }
#else
    memset(ptr, (int) value, size);
#endif
}

/*!
 * \internal
 * \brief Sets a block of memory to zero
 * 
 * \param ptr pointer to the block to zero
 * \param size length of the block, in bytes
 */
static M3G_INLINE void m3gZero(void *ptr, size_t size)/*@modifies *ptr@*/
{
    m3gFill(ptr, size, (M3Gbyte) 0);
}

/*!
 * \internal
 *  \brief Copies a block of memory
 *
 * \param ptr pointer to the block to zero
 * \param size length of the block, in bytes
 */
static M3G_INLINE void m3gCopy(
    /*@unique@*/ void *dst,
    /*@unique@*/ const void *src,
    size_t size)/*@modifies *dst@*/
{
#if defined(M3G_NO_STDLIB)
    M3Gbyte *dst = (M3Gbyte *) dst;
    const M3Gbyte *src = (const M3Gbyte *) src;
    while(size--) {
        *dst++ = *src++;
    }
#else
    memcpy(dst, src, size);
#endif
}

/*!
 * \internal
 *  \brief Moves a block of memory
 *
 * \param ptr pointer to the block to zero
 * \param size length of the block, in bytes
 */
static M3G_INLINE void m3gMove(
    /*@unique@*/ void *dst,
    /*@unique@*/ const void *src,
    size_t size)/*@modifies *dst@*/
{
#if defined(M3G_NO_STDLIB)
    M3Gbyte *dst = (M3Gbyte *) dst;
    const M3Gbyte *src = (const M3Gbyte *) src;
    if(dst <= src || (src + size) >= dst) {
	    while(size--) {
	        *dst++ = *src++;
	    }
    }
    else {
	    src += size - 1;
	    dst += size - 1;
	    while(size--) {
	        *dst-- = *src--;
	    }
	}
#else
    memmove(dst, src, size);
#endif
}

#if defined(__cplusplus)
} /* extern "C" */
#endif

#endif /*__M3G_MEMORY_H__*/
