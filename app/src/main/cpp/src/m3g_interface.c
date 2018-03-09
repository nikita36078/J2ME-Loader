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
* Description: Interface function implementation
*
*/


/*!
 * \internal
 * \file
 * \brief Interface function implementation
 */

#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_array.h"
#include "m3g_gl.h"

#if defined(M3G_DEBUG_OUT_OF_MEMORY)
#   include <stdlib.h> /* for getenv() and atoi() */
#endif

/*----------------------------------------------------------------------
 * Private data structure(s)
 *--------------------------------------------------------------------*/

#define MAX_LOCKHEAP_SIZE 100

typedef struct StartGuardRec HeapBlock;

/*!
 * \internal
 * \brief "Interface" structure
 *
 * Holds global state for an M3G instance
 */
struct M3GInterfaceImpl
{
    /*!
     * \internal
     * \brief Interface function pointers
     *
     * Each interface can have a separate set of functions for memory
     * allocation.
     */
    struct {
        /*@shared@*/ m3gMallocFunc        *malloc;
        /*@shared@*/ m3gFreeFunc          *free;
        /*@shared@*/ m3gObjectAllocator   *objAlloc;
        /*@shared@*/ m3gObjectResolver    *objResolve;
        /*@shared@*/ m3gObjectDeallocator *objFree;
        /*@shared@*/ m3gErrorHandler      *error;
        /*@shared@*/ m3gBeginRenderFunc   *getFrameBuffer;
        /*@shared@*/ m3gEndRenderFunc     *releaseFrameBuffer;
        /*@shared@*/ m3gReleaseTargetFunc *releaseTarget;
    } func;
    
    /*! \internal \brief Latest error code for this interface */
    M3Genum error;

    /*! \internal \brief Associated user context data */
    void *userContext;
    
#   if defined(M3G_DEBUG)
    /*! \internal \brief Number of memory lock requests in effect */
    M3Gint lockCount;
    struct {
        const char *file;
        int line;
    } lockHeap[MAX_LOCKHEAP_SIZE];
#   endif

#   if !defined(M3G_NGL_CONTEXT_API)
    /*! \internal \brief Number of GL activation requests */
    M3Gint glRefCount;
#   endif

    /* \internal \brief List of live objects */
    PointerArray objects;     
    /*! \internal \brief Number of objects registered for this interface */
    M3Gint objCount;

    /*! \internal \brief "Shutdown" flag for when we need to wait for
     *  objects to be deleted */
    M3Gbool shutdown;
    
    /* Temporary buffer */
    void *tempBuffer;
    M3Gsizei tempSize;
    M3Gbool tempLocked;

    /* Transformation cache */
    TCache *tcache;
    
#   if !defined(M3G_NGL_TEXTURE_API)
    PointerArray deadGLObjects;
#   endif
    
#   if defined(M3G_ENABLE_PROFILING)
    /*!
     * \internal
     * \brief Statistics counters
     */
    M3Gint statistics[M3G_STAT_MAX];
    M3Gint lastPeak;
    M3Gint profileInterval;
#   endif /*M3G_ENABLE_PROFILING*/
    
    /* Memory allocation debug counters */
#   if defined(M3G_DEBUG)
    M3Gint mallocCount;
    M3Gint objAllocCount;
#   endif

#   if defined(M3G_DEBUG_OUT_OF_MEMORY)
    M3Gsize mallocBytes, mallocLimit;
    M3Gsize objAllocBytes, objAllocLimit;
    M3Gint mallocFailureCounter, mallocFailRate;
    M3Gint objAllocFailureCounter, objAllocFailRate;
#   endif
    
#   if defined(M3G_DEBUG_HEAP_TRACKING)
    HeapBlock *blockList;
#   endif

    M3Gint maxTextureDimension;
    M3Gint maxViewportWidth;
    M3Gint maxViewportHeight;
    M3Gint maxViewportDim;
    M3Gbool supportAntialiasing;
    M3Gbool colorMaskWorkaround;
    M3Gbool twoSidedLightingWorkaround;
};

#if defined(M3G_DEBUG)

typedef struct StartGuardRec
{
#   if defined(M3G_DEBUG_HEAP_TRACKING)
    const char *allocFile;
    int allocLine : 31;
    int isObject  : 1;
    HeapBlock *next, *prev;
#   endif
    
    M3Guint endOffset;
    M3Guint magic;
} StartGuard;

typedef struct
{
    M3Guint magic;
} EndGuard;

/* Magic number used to tag memory blocks */
#define MEMORY_MAGIC 0xAE352001u

/* Macros for computing the instrumentated and "user" sizes and
 * pointers of memory blocks */

/*@notfunction@*/
#   define INSTRUMENTATED_SIZE(bytes)                           \
        ((M3Guint) (sizeof(StartGuard) + sizeof(EndGuard)       \
            + M3G_ALIGN_TO(bytes, sizeof(M3Guint))))
/*@notfunction@*/
#   define PAYLOAD_BLOCK(physicalPtr) \
        (((M3Gubyte*)physicalPtr) + sizeof(StartGuard))
/*@notfunction@*/
#   define PHYSICAL_BLOCK(payloadPtr) \
        (((M3Gubyte*)payloadPtr) - sizeof(StartGuard))
/*@notfunction@*/
#   define PAYLOAD_SIZE(blockPtr)      \
        (((const StartGuard *)PHYSICAL_BLOCK(blockPtr))->endOffset      \
         - sizeof(StartGuard))
            
#else /* !M3G_DEBUG */

/*@notfunction@*/
#   define INSTRUMENTATED_SIZE(bytes)   ((M3Guint)(bytes))
/*@notfunction@*/
#   define PAYLOAD_BLOCK(ptr)           (ptr)
/*@notfunction@*/
#   define PHYSICAL_BLOCK(ptr)          (ptr)
/*@notfunction@*/
#   define PAYLOAD_SIZE(blockPtr)      0

#endif /* M3G_DEBUG */

/*----------------------------------------------------------------------
 * Static data for managing NGL contexts
 *--------------------------------------------------------------------*/

#if defined(M3G_NGL_CONTEXT_API)
static M3Gint m3gs_glRefCount = 0;

#if defined(M3G_BUILD_ISA)
#   define M3G_SYSTEM_ALLOC     os_block_alloc
#   define M3G_SYSTEM_DEALLOC   os_block_dealloc
#else
#   define M3G_SYSTEM_ALLOC     malloc
#   define M3G_SYSTEM_DEALLOC   free
#endif

static void *m3gs_nglMem;
static void *m3gs_nglTexMgr;
#endif

#if defined(M3G_NGL_TEXTURE_API)
M3Gubyte *m3gs_nglTexUnit[M3G_NUM_TEXTURE_UNITS];
#endif

/*----------------------------------------------------------------------
 * Private functions
 *--------------------------------------------------------------------*/

/*@access M3GMemObject@*/
static /*@dependent@*/ void *defaultResolver(M3GMemObject handle)
{
    return (void *)handle;
}

/*--------------------------------------------------------------------*/

#if defined(M3G_DEBUG)

static void validateBlock(/*@reldef@*//*@temp@*//*@null@*/ const void *ptr) 
{
    if (ptr != NULL && M3G_IS_ALIGNED(ptr)) {
        const StartGuard *start = (const StartGuard *) PHYSICAL_BLOCK(ptr);
        if (start->magic == MEMORY_MAGIC) {
            const EndGuard *end = (const EndGuard *)
                (((M3Gubyte *)start) + start->endOffset);
            if (end->magic == MEMORY_MAGIC) {
                return; /* all clear */
            }
        }
        M3G_LOG1(M3G_LOG_FATAL_ERRORS,
                 "Corrupted memory block 0x%08X!\n", (unsigned) ptr);
        M3G_ASSERT(M3G_FALSE);
    }
    else {
        M3G_LOG1(M3G_LOG_FATAL_ERRORS,
                 "Invalid pointer to 0x%08X!\n", (unsigned) ptr);
        M3G_ASSERT(M3G_FALSE);
    }
}

#if defined(M3G_DEBUG_HEAP_TRACKING)
static void instrumentateBlock(/*@reldef@*//*@temp@*/ void *ptr,
                               M3Gsize bytes,
                               const char *file,
                               int line)
#else
static void instrumentateBlock(/*@reldef@*//*@temp@*/ void *ptr, M3Gsize bytes)
#endif
{
    M3Guint offset = M3G_ALIGN_TO(bytes, M3G_ALIGNMENT) + sizeof(StartGuard);
    StartGuard *start;
    EndGuard *end;
    
    M3G_ASSERT_PTR(ptr);
    M3G_ASSERT_ALIGNMENT(offset);

    /* Insert start and end guard blocks for holding debugging data as
     * well as guarding against (short) over- and underruns */
    
    start = (StartGuard *) PHYSICAL_BLOCK(ptr);
    end   = (EndGuard *) (((M3Gubyte *)start) + offset);
    
    start->endOffset = offset;
    start->magic = MEMORY_MAGIC;
    end->magic = MEMORY_MAGIC;

#   if defined(M3G_DEBUG_HEAP_TRACKING)
    start->isObject = 0;
#   endif
    
    /* Fill with garbage that will show up on the debugger if used
     * before initialized */
    {
        M3Guint *p = (M3Guint *) ptr;
        M3Guint count = bytes >> 2;
        
        while (count--) {
            *p++ = 0xBAADF00Du;
        }
    }

#   if defined(M3G_DEBUG_HEAP_TRACKING)
    /* Register allocation location */
    start->allocFile = file;
    start->allocLine = line;
#   endif
    
    validateBlock(ptr);
}

static void destroyBlock(/*@reldef@*//*@temp@*//*@null@*/ void *ptr)
{
    if (ptr != NULL) {
        validateBlock(ptr);
        {
            /* Fill with garbage that will show up on the debugger if
             * used after deallocation */
            
            StartGuard *start = (StartGuard *) PHYSICAL_BLOCK(ptr);
            M3Guint *p = (M3Guint *) start;
            M3Guint count = (start->endOffset + sizeof(EndGuard)) >> 2;

            while (count--) {
                *p++ = 0xDEADBEEFu;
            }
        }
    }
}

#if defined(M3G_DEBUG_HEAP_TRACKING)
static void insertBlock(void *ptr, HeapBlock **blockListHead)
{
    HeapBlock *head  = *blockListHead;
    HeapBlock *block = (HeapBlock *) PHYSICAL_BLOCK(ptr);
    
    M3G_ASSERT_PTR(block);
    
    *blockListHead = block;
    block->prev = NULL;
    block->next = head;

    if (head != NULL) {
        head->prev = block;
    }
}
#endif

#if defined(M3G_DEBUG_HEAP_TRACKING)
static void removeBlock(void *ptr, HeapBlock **blockListHead)
{
    if (ptr != NULL) {
        HeapBlock *head  = *blockListHead;
        HeapBlock *block = (HeapBlock *) PHYSICAL_BLOCK(ptr);

        validateBlock(ptr);
        
        M3G_ASSERT_PTR(head);
        M3G_ASSERT_PTR(block);
        
        if (block->prev) {
            block->prev->next = block->next;
        }
        if (block->next) {
            block->next->prev = block->prev;
        }
        if (block == head) {
            *blockListHead = block->next;
        }
        
        block->next = NULL;
        block->prev = NULL;
    }
}
#endif

#if defined(M3G_DEBUG_HEAP_TRACKING) && defined(M3G_LOGLEVEL)
static void dumpBlocks(const HeapBlock *head) 
{
    while (head) {
        M3G_LOG4(M3G_LOG_FATAL_ERRORS,
                 "0x%08X: %s:%d, %d bytes\n",
                 (unsigned) PAYLOAD_BLOCK(head),
                 head->allocFile,
                 head->allocLine,
                 (M3Gsizei) PAYLOAD_SIZE(PAYLOAD_BLOCK(head)));
        head = head->next;
    }
}
#endif

#undef MEMORY_MAGIC

#else /* !M3G_DEBUG */
#   define instrumentateBlock(ptr, bytes)
#   define validateBlock(ptr)
#   define destroyBlock(ptr)
#endif /* M3G_DEBUG */

/*--------------------------------------------------------------------*/

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

#if defined(M3G_DEBUG)

/*!
 * \internal
 * \brief Locks all memory allocation for an interface
 *
 * This is used to ensure that memory compacting does not affect the
 * addresses of our "memory objects" while they're in use. A count of
 * locks is maintained; the same interface may be locked several
 * times, and each lock must be separately released with a call to
 * m3gUnlockMemory.
 *
 * Memory is also automatically locked while a handle is mapped to a
 * pointer; see m3gMapObject.
 *
 */
static void m3gDebugLockMemory(Interface *m3g, const char *file, int line)
{
    M3G_ASSERT(m3gInRange(m3g->lockCount, 0, 0x7FFFFFFE));
    M3G_ASSERT(m3g->lockCount < MAX_LOCKHEAP_SIZE);
    
    m3g->lockHeap[m3g->lockCount].file = file;
    m3g->lockHeap[m3g->lockCount].line = line;
    m3g->lockCount++;
    
    m3gIncStat(m3g, M3G_STAT_MEMORY_LOCKS, 1);
}

/*!
 * \internal
 * \brief Releases a memory allocation lock on an interface
 */
static void m3gUnlockMemory(Interface *m3g)
{
    M3G_ASSERT(m3g->lockCount > 0);
    m3g->lockHeap[m3g->lockCount].file = "";
    m3g->lockHeap[m3g->lockCount].line = 0;
    m3g->lockCount--;
}

/*!
 * \internal
 * \brief Checks whether memory is currently locked (debug only)
 */
static M3Gbool m3gMemoryLocked(Interface *m3g)
{
    M3G_ASSERT(m3g->lockCount >= 0);
    
    if (m3g->lockCount > 0) {
#       if defined(M3G_LOGLEVEL)
        int i;
        M3G_LOG1(M3G_LOG_FATAL_ERRORS,
                 "%d memory lock(s) in effect:\n", m3g->lockCount);
        for (i = m3g->lockCount - 1; i >= 0; --i) {
            M3G_LOG2(M3G_LOG_FATAL_ERRORS, "%s, line %d\n",
                     m3g->lockHeap[i].file, m3g->lockHeap[i].line);
        }
#       endif
        return M3G_TRUE;
    }
    
    return M3G_FALSE;
}
#endif

/*!
 * \internal
 * \brief Allocates a block of memory from a regular C-style heap
 *
 */
#if defined(M3G_DEBUG)
static void *m3gDebugAlloc(
    Interface *m3g, M3Gsize bytes, const char *file, int line)
#else
static void *m3gAlloc(Interface *m3g, M3Gsize bytes)
#endif    
{
    void *ptr;
    M3G_VALIDATE_INTERFACE(m3g);
    M3G_ASSERT_NO_LOCK(m3g);

    /* Simulate memory allocation failures if enabled */
    
#   if defined(M3G_DEBUG_OUT_OF_MEMORY)
    if (m3g->mallocFailRate > 0 &&
            m3g->mallocFailureCounter++ >= m3g->mallocFailRate) {
        m3g->mallocFailureCounter = 0;
        goto AllocFailed;
    }
    if (m3g->mallocLimit > 0 &&
            m3g->mallocBytes + bytes > m3g->mallocLimit) {
        goto AllocFailed;
    }
#   endif
        
    /* First just try to allocate more memory; if that fails, garbage
     * collect and try again before returning with an error */

    ptr = (*m3g->func.malloc)(INSTRUMENTATED_SIZE(bytes));
    if (ptr == NULL) {
        M3G_LOG(M3G_LOG_WARNINGS|M3G_LOG_MEMORY_ALL,
                "Warning: heap alloc failed\n");
        m3gGarbageCollectAll(m3g);
        ptr = (*m3g->func.malloc)(INSTRUMENTATED_SIZE(bytes));
    }
    if (ptr == NULL) {
        goto AllocFailed;
    }

    /* Succesfully allocated some, so update statistics */
    
#   if defined(M3G_DEBUG)
    m3g->mallocCount++;
#   endif
#   if defined(M3G_DEBUG_OUT_OF_MEMORY)
    m3g->mallocBytes += bytes;
#   endif

    /* Add instrumentation to the block */
    
    M3G_ASSERT_ALIGNMENT(ptr);
    ptr = PAYLOAD_BLOCK(ptr);
#   if defined(M3G_DEBUG_HEAP_TRACKING)
    insertBlock(ptr, &m3g->blockList);
    instrumentateBlock(ptr, bytes, file, line);
#   else
    instrumentateBlock(ptr, bytes);
#   endif
    
    m3gIncStat(m3g, M3G_STAT_MEMORY_ALLOCS, 1);
#   if defined(M3G_DEBUG) && defined(M3G_ENABLE_PROFILING)
    {
        M3Gint size = PAYLOAD_SIZE(ptr);
        m3gIncStat(m3g, M3G_STAT_MEMORY_ALLOCATED, size);
        m3gIncStat(m3g, M3G_STAT_MEMORY_MALLOC_BYTES, size);
    }
#   else
    m3gIncStat(m3g, M3G_STAT_MEMORY_ALLOCATED, bytes);
    m3gIncStat(m3g, M3G_STAT_MEMORY_MALLOC_BYTES, bytes);
#   endif
    
#   if defined(M3G_DEBUG)
    M3G_LOG4(M3G_LOG_MEMORY_BLOCKS,
             "Alloc 0x%08X, %d bytes (%s, line %d)\n",
             (unsigned) ptr, bytes, file, line);
#   else
    M3G_LOG2(M3G_LOG_MEMORY_BLOCKS, "Alloc 0x%08X, %d bytes\n",
             (unsigned) ptr, bytes);
#   endif
    
    m3gUpdateMemoryPeakCounter(m3g);
    
    return ptr;

AllocFailed:
    m3gRaiseError(m3g, M3G_OUT_OF_MEMORY);
    return NULL;
}
    
/*!
 * \internal
 * \brief Same as m3gAlloc, but also zero-initializes the allocated
 * block
 */
#if !defined(M3G_DEBUG)
static void *m3gAllocZ(Interface *m3g, M3Gsize bytes)
{
    void *ptr = m3gAlloc(m3g, bytes);
    if (ptr != NULL) {
        m3gZero(ptr, bytes);
    }
    return ptr;
}
#else
static void *m3gDebugAllocZ(
    Interface *m3g, M3Gsize bytes, const char *file, int line)
{
    void *ptr = m3gDebugAlloc(m3g, bytes, file, line);
    if (ptr != NULL) {
        m3gZero(ptr, bytes);
    }
    return ptr;
}
#endif /* M3G_DEBUG_HEAP_TRACKING */

/*!
 * \internal
 * \brief Frees a block of memory allocated using m3gAlloc
 */
#if defined(M3G_DEBUG)
static void m3gDebugFree(Interface *m3g, void *ptr, const char *file, int line)
#else
static void m3gFree(Interface *m3g, void *ptr)
#endif
{
    M3G_VALIDATE_INTERFACE(m3g);
    M3G_ASSERT_ALIGNMENT(ptr);
    M3G_ASSERT_NO_LOCK(m3g);
    
    if (ptr != NULL) {
        M3G_VALIDATE_MEMBLOCK(ptr);
        M3G_ASSERT(!m3gIsObject(ptr));
        
#       if defined(M3G_DEBUG)
        m3g->mallocCount--;
        M3G_ASSERT(m3g->mallocCount >= 0);
#       endif

#if defined(M3G_ENABLE_PROFILING) || defined(M3G_DEBUG_OUT_OF_MEMORY)
        {
            M3Gint size = PAYLOAD_SIZE(ptr);        
#           if defined(M3G_ENABLE_PROFILING)
            m3gIncStat(m3g, M3G_STAT_MEMORY_ALLOCATED, -size);
            m3gIncStat(m3g, M3G_STAT_MEMORY_MALLOC_BYTES, -size);
#           endif
#           if defined(M3G_DEBUG_OUT_OF_MEMORY)
            m3g->mallocBytes -= size;
#           endif
        }
#endif

#       if defined(M3G_DEBUG_HEAP_TRACKING)
        removeBlock(ptr, &m3g->blockList);
#       endif
    
    
#       if defined(M3G_DEBUG)
#           if defined(M3G_DEBUG_HEAP_TRACKING)
            M3G_LOG4(M3G_LOG_MEMORY_BLOCKS,
                     "Free 0x%08X, %d bytes (%s, line %d)\n",
                     (unsigned) ptr, PAYLOAD_SIZE(ptr), file, line);
#           else
            M3G_LOG3(M3G_LOG_MEMORY_BLOCKS,
                     "Free 0x%08X (%s, line %d)\n",
                     (unsigned) ptr, file, line);
#           endif
#       else
        M3G_LOG1(M3G_LOG_MEMORY_BLOCKS, "Free 0x%08X\n", (unsigned) ptr);
#       endif
    
        destroyBlock(ptr);
        (*m3g->func.free)(PHYSICAL_BLOCK(ptr));
    }
}

#if 0

/*!
 * \brief Recycles a block of memory for later use
 *
 * Same as free, but instead of always returning the memory to the OS,
 * may place it on a list of free blocks for reuse.  Blocks in the
 * free list can be quickly reused by m3gAlloc.
 * 
 * \param m3g    Interface instance
 * \param ptr    pointer to block to recycle
 * \param bytes  size of the block, in bytes
 */
static void m3gRecycle(Interface *m3g, void *ptr, M3Gsize bytes)
{
    M3G_UNREF(bytes);
    m3gFree(m3g, ptr);
}

#endif

#if defined(M3G_DEBUG)
/*!
 * \internal
 * \brief Checks the integrity of a memory block
 */
static void m3gValidateMemory(const void *ptr)
{
    validateBlock(ptr);
}
#endif /* M3G_DEBUG */

#if defined(M3G_DEBUG)
/*!
 * \internal
 * \brief Checks the integrity of an Interface object
 */
static void m3gValidateInterface(const Interface *m3g)
{
    M3G_VALIDATE_MEMBLOCK(m3g);
}
#endif /* M3G_DEBUG */

#if defined (M3G_DEBUG_HEAP_TRACKING)
/*!
 * \internal
 * \brief Marks a block as a live object block
 *
 * Live objects can never have their memory freed.
 */
static void m3gMarkObject(void *ptr)
{
    StartGuard *start;
    validateBlock(ptr);
    
    start = (StartGuard *) PHYSICAL_BLOCK(ptr);
    start->isObject = 1;
}
#endif

#if defined (M3G_DEBUG_HEAP_TRACKING)
/*!
 * \internal
 * \brief Checks if a block is an object block
 */
static M3Gbool m3gIsObject(const void *ptr)
{
    const StartGuard *start;
    validateBlock(ptr);
    
    start = (StartGuard *) PHYSICAL_BLOCK(ptr);
    return (start->isObject != 0);
}
#endif

#if defined (M3G_DEBUG_HEAP_TRACKING)
/*!
 * \internal
 * \brief Unmarks an object block
 */
static void m3gUnmarkObject(void *ptr)
{
    StartGuard *start;
    validateBlock(ptr);
    
    start = (StartGuard *) PHYSICAL_BLOCK(ptr);
    start->isObject = 0;
}
#endif


/*!
 * \internal
 * \brief Allocates a "memory object" from a potentially compacting heap
 *
 * A block of memory of \c bytes is allocated, but its address is
 * available only via the m3gMapObject function.
 * 
 * \return a handle used to refer to the allocated block during the
 * rest of its lifetime
 */
/*@access M3GMemObject@*/
#if defined(M3G_DEBUG)
static M3GMemObject m3gDebugAllocObject(
    Interface *m3g, M3Gsize bytes, const char *file, int line)
#else
static M3GMemObject m3gAllocObject(Interface *m3g, M3Gsize bytes)
#endif
{
    M3GMemObject handle;
    M3G_VALIDATE_INTERFACE(m3g);
    M3G_ASSERT_NO_LOCK(m3g);

    /* Simulate memory allocation failures if enabled */
    
#   if defined(M3G_DEBUG_OUT_OF_MEMORY)
    if (m3g->objAllocFailRate > 0 &&
            m3g->objAllocFailureCounter++ >= m3g->objAllocFailRate) {
        m3g->objAllocFailureCounter = 0;
        goto AllocFailed;
    }
    if (m3g->objAllocLimit > 0 &&
            m3g->objAllocBytes + bytes > m3g->objAllocLimit) {
        goto AllocFailed;
    }
#   endif
        
    /* Similarly to Alloc, garbage collect and try again if the
     * first allocation fails */
        
    handle = (*m3g->func.objAlloc)(INSTRUMENTATED_SIZE(bytes));
    if (!handle) {
        M3G_LOG(M3G_LOG_WARNINGS|M3G_LOG_MEMORY_ALL,
                "Warning: object alloc failed\n");
        m3gGarbageCollectAll(m3g);
        handle = (*m3g->func.objAlloc)(INSTRUMENTATED_SIZE(bytes));
    }
    if (!handle) {
        goto AllocFailed;
    }

    /* Succesfully allocated, update statistics */
        
    m3gIncStat(m3g, M3G_STAT_MEMORY_ALLOCS, 1);
#   if defined(M3G_DEBUG)
    m3g->objAllocCount++;
    if (handle != 0) {
        void *ptr = PAYLOAD_BLOCK((*m3g->func.objResolve)(handle));
#       if defined(M3G_DEBUG_HEAP_TRACKING)
        instrumentateBlock(ptr, bytes, file, line);
#       else
        instrumentateBlock(ptr, bytes);
#       endif
#       if defined(M3G_ENABLE_PROFILING)
        {
            M3Gint size = PAYLOAD_SIZE(ptr);
            m3gIncStat(m3g, M3G_STAT_MEMORY_ALLOCATED, size);
            m3gIncStat(m3g, M3G_STAT_MEMORY_OBJECT_BYTES, size);
        }
#       endif
    }
#   else
    m3gIncStat(m3g, M3G_STAT_MEMORY_ALLOCATED, bytes);
    m3gIncStat(m3g, M3G_STAT_MEMORY_OBJECT_BYTES, bytes);
#   endif

#   if defined(M3G_DEBUG)
    M3G_LOG4(M3G_LOG_MEMORY_BLOCKS,
             "ObjAlloc 0x%08X, %d bytes (%s, line %d)\n",
             (unsigned) handle, bytes, file, line);
#   else
    M3G_LOG2(M3G_LOG_MEMORY_BLOCKS, "ObjAlloc 0x%08X, %d bytes\n",
             (unsigned) handle, bytes);
#   endif
    
    m3gUpdateMemoryPeakCounter(m3g);
    
#   if defined(M3G_DEBUG_OUT_OF_MEMORY)
    m3g->objAllocBytes += bytes;
#   endif
        
    return handle;

AllocFailed:
    m3gRaiseError(m3g, M3G_OUT_OF_MEMORY);
    return 0;
}

/*!
 * \internal
 * \brief Frees a memory object allocated with m3gAllocObject
 */
/*@access M3GMemObject@*/
#if defined(M3G_DEBUG)
static void m3gDebugFreeObject(Interface *m3g, M3GMemObject handle, const char *file, int line)
#else
static void m3gFreeObject(Interface *m3g, M3GMemObject handle)
#endif
{
    M3G_VALIDATE_INTERFACE(m3g);
    M3G_ASSERT_NO_LOCK(m3g);

    /* Debugging code */
    
#   if defined(M3G_DEBUG)
    if (handle != 0) {
        void *ptr = m3gMapObject(m3g, handle);
        M3G_VALIDATE_MEMBLOCK(ptr);
        M3G_ASSERT(!m3gIsObject(ptr));
        
        if (ptr != NULL) {
#           if defined(M3G_ENABLE_PROFILING) || defined(M3G_DEBUG_OUT_OF_MEMORY)
            M3Gint size = PAYLOAD_SIZE(ptr);
#           endif
#           if defined(M3G_ENABLE_PROFILING)
            m3gIncStat(m3g, M3G_STAT_MEMORY_ALLOCATED, -size);
            m3gIncStat(m3g, M3G_STAT_MEMORY_OBJECT_BYTES, -size);
#           endif
#           if defined(M3G_DEBUG_OUT_OF_MEMORY)
            m3g->objAllocBytes -= size;
#           endif
#           if defined(M3G_DEBUG_HEAP_TRACKING)
            M3G_LOG4(M3G_LOG_MEMORY_BLOCKS,
                     "ObjFree 0x%08X, %d bytes (%s, line %d)\n",
                     (unsigned) handle, PAYLOAD_SIZE(ptr), file, line);
#           else
            M3G_LOG3(M3G_LOG_MEMORY_BLOCKS,
                     "ObjFree 0x%08X (%s, line %d)\n",
                     (unsigned) handle, file, line);
#           endif
        }
        m3gUnmapObject(m3g, handle);
        
        destroyBlock(ptr);
        
        m3g->objAllocCount--;
        M3G_ASSERT(m3g->objAllocCount >= 0);
    }
#   else /* !M3G_DEBUG*/
        M3G_LOG1(M3G_LOG_MEMORY_BLOCKS, "ObjFree 0x%08X\n", (unsigned) handle);
#   endif

    /* Actual operation */
    
    (*m3g->func.objFree)(handle);
}

/*!
 * \internal
 * \brief Allocates a temporary data buffer
 *
 * The temporary buffer is intended for situations where more
 * temporary data is required than can be allocated on the stack.
 * Only one temporary buffer exists for each M3G interface, and it
 * must be freed via \c m3gFreeTemp before reallocating.
 *
 * \param m3g   interface object
 * \param bytes size of the temp buffer requested, in bytes
 */
static void *m3gAllocTemp(Interface *m3g, M3Gsizei bytes)
{
    M3G_VALIDATE_INTERFACE(m3g);
    M3G_ASSERT_NO_LOCK(m3g);
    M3G_ASSERT(!m3g->tempLocked);

    if (m3g->tempSize < bytes) {
        m3gFree(m3g, m3g->tempBuffer);
        m3g->tempBuffer = NULL;
    }

    if (m3g->tempBuffer == NULL) {
        m3g->tempBuffer = m3gAlloc(m3g, bytes);
        if (m3g->tempBuffer == NULL) {
            return NULL; /* automatic out of memory */
        }
        m3g->tempSize = bytes;
    }

    m3g->tempLocked = M3G_TRUE;
    return m3g->tempBuffer;
}

/*!
 * \internal
 * \brief Release the currently allocated temporary data buffer
 */
static void m3gFreeTemp(Interface *m3g)
{
    M3G_VALIDATE_INTERFACE(m3g);
    M3G_ASSERT_NO_LOCK(m3g);
    M3G_ASSERT(m3g->tempLocked);
    
    m3g->tempLocked = M3G_FALSE;
}

/*!
 * \internal
 * \brief
 */
static void m3gAddChildObject(Interface *m3g, Object *obj)
{
    M3G_ASSERT(!m3g->shutdown);
    M3G_ASSERT(m3gInRange(m3g->objCount, 0, 0x7FFFFFFF));
    ++m3g->objCount;

    /* Add the object to the list of live objects */
    m3gArrayAppend(&m3g->objects, obj, m3g);
}

/*!
 * \internal
 * \brief
 */
static void m3gDelChildObject(Interface *m3g, Object *obj)
{
    M3G_ASSERT(m3g->objCount > 0);

    /* Remove the object from the list of live objects */
    m3gArrayDelete(&m3g->objects, m3gArrayFind(&m3g->objects, obj));
    if (--m3g->objCount == 0 && m3g->shutdown) {
        m3gDeleteInterface(m3g);
    }
}

#if !defined(M3G_NGL_TEXTURE_API)
/*!
 * \internal
 * \brief Get a list of live objects with matching class ID
 */
static void m3gGetObjectsWithClassID(Interface *m3g, M3GClass classID, PointerArray* objects)
{
    M3Gsizei i = m3gArraySize(&m3g->objects);
    while (i > 0) {
        M3GObject obj = (M3GObject)m3gGetArrayElement(&m3g->objects, --i);
        if (m3gGetClass(obj) == classID)
            m3gArrayAppend(objects, obj, m3g);
    }
}
/*!
 * \internal
 * \brief Queue OpenGL texture objects for deletion
 *
 * The objects will be deleted when a GL context is next made current.
 *
 */
static void m3gDeleteGLTextures(Interface *m3g, M3Gsizei n, M3Guint *t)
{
    PointerArray *objs = &m3g->deadGLObjects;
    while (n--) {
        if (m3gArrayAppend(objs, (void*) t[n], m3g) < 0) {
            return; 
        }
    }
}

/*!
 * \internal
 * \brief Delete queued OpenGL objects
 *
 * This function should be called at suitable points during execution
 * to delete dead GL texture objects.  A GL context must be current.
 */
static void m3gCollectGLObjects(Interface *m3g)
{
    PointerArray *objs = &m3g->deadGLObjects;
    M3Gsizei n = m3gArraySize(objs);
    M3Gint i;
    for (i = 0; i < n; ++i) {
        GLuint t = (GLuint) m3gGetArrayElement(objs, i);
        glDeleteTextures(1, &t);
        M3G_LOG1(M3G_LOG_OBJECTS, "Destroyed GL texture object 0x%08X\n",
                 (unsigned) t);
    }
    m3gClearArray(objs);
}
#endif /* !defined(M3G_NGL_TEXTURE_API)*/


/*!
 * \internal
 * \brief Locks a memory object in place and returns a pointer to it
 *
 * The block is mapped to the returned address until a matching call
 * to m3gUnmapObject. While any object is mapped, memory allocation is
 * prohibited on the whole interface; see m3gLockMemory. Every
 * m3gMapObject call must be followed by a matching m3gUnmapObject
 * call to release the memory lock.
 */
/*@access M3GMemObject@*/
static void *m3gMapObject(Interface *m3g, M3GMemObject handle)
{
    M3G_VALIDATE_INTERFACE(m3g);

    if (handle == 0) {
        return NULL;
    }
    else {
        void *ptr;

        m3gLockMemory(m3g);

        ptr = (*m3g->func.objResolve)(handle);
        ptr = PAYLOAD_BLOCK(ptr);

        M3G_LOG2(M3G_LOG_MEMORY_MAPPING, "MapObj 0x%08X -> 0x%08X\n",
                 (unsigned) handle, (unsigned) ptr);
        
        validateBlock(ptr);
        return ptr;
    }
}

/*!
 * \internal
 * \brief Releases a memory object locked with m3gMapObject
 *
 * The memory address of the object, as obtained from m3gMapObject,
 * must not be used again until a new m3gMapObject call.
 */
/*@access M3GMemObject@*/
static void m3gUnmapObject(Interface *m3g, M3GMemObject handle)
{
    M3G_VALIDATE_INTERFACE(m3g);
    
#   if defined(M3G_DEBUG)
    if (handle != 0) {
        void *ptr = (*m3g->func.objResolve)(handle);
        validateBlock(PAYLOAD_BLOCK(ptr));
        M3G_LOG1(M3G_LOG_MEMORY_MAPPING,
                 "UnmapObj 0x%08X\n", (unsigned) handle);
        m3gUnlockMemory(m3g);
    }
#   else
    M3G_UNREF(m3g);
    M3G_UNREF(handle);
#   endif
}

/*!
 * \internal
 * \brief Garbage collect until no more objects can be freed
 *
 * \note This is currently a no-op if reference counting has not been
 * enabled at build time.
 */
static void m3gGarbageCollectAll(Interface *m3g)
{
    M3G_VALIDATE_INTERFACE(m3g);
    M3G_ASSERT_NO_LOCK(m3g);
    M3G_ASSERT(!m3g->tempLocked);

    /* Free the temporary buffer */
    
    m3gFree(m3g, m3g->tempBuffer);
    m3g->tempBuffer = NULL;
    m3g->tempSize = 0;
}

#if defined(M3G_NGL_CONTEXT_API)
/*!
 * \internal
 * \brief Gets the address of an external frame buffer and locks the
 * buffer in place
 *
 * Used for memory rendering targets only; see m3gBindMemoryTarget.
 */
static void *m3gGetExternalFB(Interface *m3g, M3Guint userTarget)
{
    void *ptr = NULL;
    M3G_VALIDATE_INTERFACE(m3g);
    M3G_ASSERT_NO_LOCK(m3g);

    if (m3g->func.getFrameBuffer != NULL) {
        ptr = (*m3g->func.getFrameBuffer)(userTarget);
    }
    
    m3gLockMemory(m3g); /* note that this must be done *after* calling
                         * the callback, or we'll get asserts if GC is
                         * triggered */
    return ptr;
}
#endif /* defined(M3G_NGL_CONTEXT_API) */

#if defined(M3G_NGL_CONTEXT_API)
/*!
 * \internal
 * \brief Releases the external frame buffer locked with
 * m3gGetExternalFB
 */
static void m3gReleaseExternalFB(Interface *m3g, M3Guint userTarget)
{
    M3G_VALIDATE_INTERFACE(m3g);
    m3gUnlockMemory(m3g);
    if (m3g->func.releaseFrameBuffer != NULL) {
        (*m3g->func.releaseFrameBuffer)(userTarget);
    }
}
#endif /* defined(M3G_NGL_CONTEXT_API) */

#if defined(M3G_NGL_CONTEXT_API)
/*!
 * \internal
 * \brief Signals to a user callback that the bound rendering target
 * has been released
 *
 * \note This is a callback because it is possible in Java for a
 * rendering context to be destroyed without first releasing the
 * target; in that case, this callback is called to ensure that all
 * external resources are freed.
 */
static void m3gSignalTargetRelease(Interface *m3g, M3Guint userTarget)
{
    M3G_VALIDATE_INTERFACE(m3g);
    if (m3g->func.releaseTarget != NULL) {
        (*m3g->func.releaseTarget)(userTarget);
    }
}
#endif /* defined(M3G_NGL_CONTEXT_API) */

/*!
 * \internal
 * \brief Raise an error status on this interface
 *
 * Any previous error code will be overwritten.
 */
#if defined(M3G_DEBUG)
static void m3gDebugRaiseError(Interface *m3g,
                               M3Genum errorCode,
                               const char *filename,
                               int line)
#else
static void m3gRaiseError(Interface *m3g, M3Genum errorCode)
#endif
{
    M3G_VALIDATE_INTERFACE(m3g);

    if (errorCode == M3G_OUT_OF_MEMORY) {
        M3G_LOG(M3G_LOG_MEMORY_ALL|M3G_LOG_WARNINGS,
                "Error: Out of memory!\n");
    }
    
#   if defined(M3G_DEBUG)
    M3G_LOG3(M3G_LOG_USER_ERRORS,
             "Error %d at %s, line %d\n", (int) errorCode, filename, line);
#   else
    M3G_LOG1(M3G_LOG_USER_ERRORS, "Error %d\n", (int) errorCode);
#   endif /* M3G_DEBUG */
    
    m3g->error = errorCode;
    if (m3g->func.error != NULL) {
        (*m3g->func.error)(errorCode, (M3GInterface) m3g);
        m3g->error = M3G_NO_ERROR;
    }
}

#ifdef M3G_NATIVE_LOADER
/*!
 * \internal
 * \brief Checks whether an error flag has been raised
 */
static M3GError m3gErrorRaised(const Interface *m3g)
{
    M3G_VALIDATE_INTERFACE(m3g);
    return (M3GError)m3g->error;
}

/*!
 * \internal
 * \brief Sets a new error handler and returns the old one
 */
static m3gErrorHandler *m3gSetErrorHandler(Interface *m3g, m3gErrorHandler *errorHandler)
{
    m3gErrorHandler *current = m3g->func.error;
    m3g->func.error = errorHandler;
    return current;
}
#endif

/*!
 * \internal
 * \brief Gets various constants from the GL driver
 */

#include <stdio.h>

static void m3gConfigureGL(Interface *m3g)
{
#   if defined(M3G_NGL_CONTEXT_API)
    m3g->maxTextureDimension = M3G_MAX_TEXTURE_DIMENSION;
    m3g->maxViewportWidth = M3G_MAX_VIEWPORT_WIDTH;
    m3g->maxViewportHeight = M3G_MAX_VIEWPORT_HEIGHT;
    m3g->maxViewportDim = M3G_MAX_VIEWPORT_DIMENSION;
#   else /* !M3G_NGL_CONTEXT_API */
	 const GLubyte *info;
    int params[2];
    int numConfigs;
    EGLContext ctx;
    EGLConfig config;
    EGLSurface surf;
    EGLint attrib[5];

    m3gInitializeGL(m3g);

    attrib[0] = EGL_SURFACE_TYPE;
    attrib[1] = EGL_PBUFFER_BIT;
    attrib[2] = EGL_NONE;
    
    eglChooseConfig(eglGetDisplay(0),
                    attrib,
                    &config, 1,
                    &numConfigs);

    M3G_ASSERT(numConfigs > 0);
    
    ctx = eglCreateContext(eglGetDisplay(0),
                           config,
                           NULL,
                           NULL);

    attrib[0] = EGL_WIDTH;
    attrib[1] = 2;
    attrib[2] = EGL_HEIGHT;
    attrib[3] = 2;
    attrib[4] = EGL_NONE;

    surf = eglCreatePbufferSurface(eglGetDisplay(0),
                                   config,
                                   attrib);

    eglMakeCurrent(eglGetDisplay(0), 
                   surf, surf, ctx);


    /* Check antialiasing support and workarounds
       from the renderer string.
       HW platforms like MBX has AA, Gerbera and NGL does not.
       MBX needs workarounds for color mask and two sided lighting.
    */

	info = glGetString(GL_RENDERER);

    if (strstr((const char *)info, "HW")) {
        m3g->supportAntialiasing = M3G_TRUE;
    }
    else {
        m3g->supportAntialiasing = M3G_FALSE;
    }

    if (strstr((const char *)info, "MBX")) {
        m3g->colorMaskWorkaround = M3G_TRUE;
        m3g->twoSidedLightingWorkaround = M3G_TRUE;
    }
    else {
        m3g->colorMaskWorkaround = M3G_FALSE;
        m3g->twoSidedLightingWorkaround = M3G_FALSE;
    }

    /* For testing purposes only */
#   if defined(M3G_FORCE_MBX_WORKAROUNDS)
    m3g->colorMaskWorkaround = M3G_TRUE;
    m3g->twoSidedLightingWorkaround = M3G_TRUE;
#   endif

    glGetIntegerv(GL_MAX_TEXTURE_SIZE, params);

    m3g->maxTextureDimension = params[0];

    glGetIntegerv(GL_MAX_VIEWPORT_DIMS, params);

    m3g->maxViewportWidth = params[0];
    m3g->maxViewportHeight = params[1];
    m3g->maxViewportDim = M3G_MIN(params[0], params[1]);

    eglMakeCurrent(eglGetDisplay(0), NULL, NULL, NULL);
    eglDestroySurface(eglGetDisplay(0), surf);
    eglDestroyContext(eglGetDisplay(0), ctx);

    m3gShutdownGL(m3g);
    
#endif /* M3G_NGL_CONTEXT_API */
}


/*!
 * \internal
 * \brief Initializes the GL subsystem
 */
static void m3gInitializeGL(Interface *m3g)
{
#   if defined(M3G_NGL_CONTEXT_API)
#       define glRefCount m3gs_glRefCount
#   else
#       define glRefCount m3g->glRefCount
#   endif
    
    M3G_VALIDATE_INTERFACE(m3g);
    M3G_UNREF(m3g);

    if (++glRefCount == 1) {
        M3G_LOG(M3G_LOG_INTERFACE, "Initializing GL\n");
        
#       if defined(M3G_NGL_CONTEXT_API)
        
        m3gs_nglMem = M3G_SYSTEM_ALLOC(NGL_MIN_WORKING_MEMORY_BYTES);
        m3gs_nglTexMgr = nglCreateTextureManager();
        if (!nglInit(m3gs_nglMem, NGL_MIN_WORKING_MEMORY_BYTES,
                     m3gs_nglTexMgr,
                     0)) {
            M3G_ASSERT(M3G_FALSE);
        }
        M3G_ASSERT_GL;
    
#       else /* !M3G_NGL_CONTEXT_API */
    
        m3gInitializeEGL();

#       endif
        
#       if defined(M3G_NGL_TEXTURE_API)
        {
            int i;
            GLuint texture;
            for (i = 0; i < M3G_NUM_TEXTURE_UNITS; ++i) {
                m3gs_nglTexUnit[i] = M3G_SYSTEM_ALLOC(NGL_TEXTURE_STRUCT_SIZE);
                M3G_ASSERT(m3gs_nglTexUnit[i]);
                texture = (GLuint) m3gs_nglTexUnit[i];
                nglInitTextures(1, &texture);
                glActiveTexture(GL_TEXTURE0 + i);
                nglBindTextureInternal(GL_TEXTURE_2D, texture);
            }
            glActiveTexture(GL_TEXTURE0);
        }
#       endif
    }
    
    M3G_ASSERT(glRefCount > 0);

#   undef glRefCount
}

/*!
 * \internal
 * \brief Shuts down the GL subsystem
 */
static void m3gShutdownGL(Interface *m3g)
{
#   if defined(M3G_NGL_CONTEXT_API)
#       define glRefCount m3gs_glRefCount
#   else
#       define glRefCount m3g->glRefCount
#   endif

    M3G_VALIDATE_INTERFACE(m3g);
    M3G_UNREF(m3g);
    M3G_ASSERT(glRefCount > 0);    
    M3G_LOG(M3G_LOG_INTERFACE, "Shutting down GL...\n");
        
    if (--glRefCount == 0) {
#       if defined(M3G_NGL_CONTEXT_API)
        
        nglExit();
        nglDeleteTextureManager(m3gs_nglTexMgr);
        M3G_SYSTEM_DEALLOC(m3gs_nglMem);
        m3gs_nglTexMgr = NULL;
        m3gs_nglMem = NULL;
    
#       else /* !M3G_NGL_CONTEXT_API */
    
        m3gTerminateEGL();
        
#       endif

#       if defined(M3G_NGL_TEXTURE_API)
        {
            int i;
            for (i = 0; i < M3G_NUM_TEXTURE_UNITS; ++i) {
                M3G_SYSTEM_DEALLOC(m3gs_nglTexUnit[i]);
                m3gs_nglTexUnit[i] = NULL;
            }
        }
#       endif
    }
    else {
        M3G_LOG1(M3G_LOG_INTERFACE, "Waiting for %d GL objects\n",
                 glRefCount);
    }
#   undef glRefCount
}

/*!
 * \internal
 * \brief Make any run-time portability checks
 */
static M3Gbool m3gSystemCheck(void)
{
    /* Check that right shifts on signed values work as expected
     * (extending the top bit to keep the sign) */
    {
        int magic = -0x7F7E80B1;
        if ((magic >> 4) != -0x07F7E80C) { 
            return M3G_FALSE;
        }
    }

    /* Check endianess if dependent code introduced */
    
    return M3G_TRUE;
}

static M3Gbool m3gGetColorMaskWorkaround(Interface *m3g)
{
    return m3g->colorMaskWorkaround;
}

static M3Gbool m3gGetTwoSidedLightingWorkaround(Interface *m3g)
{
    return m3g->twoSidedLightingWorkaround;
}

/*!
 * \internal
 * \brief Increment a statistics counter
 */
#if defined(M3G_ENABLE_PROFILING)
static void m3gIncStat(Interface *m3g, M3Gstatistic stat, M3Gint increment)
{
    m3g->statistics[stat] += increment;
}
#endif

/*!
 * \internal
 * \brief Increment a statistics counter
 */
#if defined(M3G_ENABLE_PROFILING)
static void m3gResetStat(Interface *m3g, M3Gstatistic stat)
{
    m3g->statistics[stat] = 0;
}
#endif

/*!
 * \internal
 * \brief Output memory peak counters to the log
 */
#if defined(M3G_ENABLE_PROFILING)
static void m3gLogMemoryPeakCounter(const Interface *m3g) 
{
    M3G_LOG3(M3G_LOG_MEMORY_USAGE,
             "Memory peaks: %d KB heap, %d KB obj, %d KB total\n",
             m3g->statistics[M3G_STAT_MEMORY_MALLOC_PEAK] >> 10,
             m3g->statistics[M3G_STAT_MEMORY_OBJECT_PEAK] >> 10,
             m3g->statistics[M3G_STAT_MEMORY_PEAK] >> 10);
}
#endif

/*!
 * \internal
 * \brief Update the peak memory counter
 *
 * This function should be called after each memory allocation
 */
#if defined(M3G_ENABLE_PROFILING)
static void m3gUpdateMemoryPeakCounter(Interface *m3g)
{
    if (m3g->statistics[M3G_STAT_MEMORY_ALLOCATED] > m3g->statistics[M3G_STAT_MEMORY_PEAK]) {
        m3g->statistics[M3G_STAT_MEMORY_PEAK] = m3g->statistics[M3G_STAT_MEMORY_ALLOCATED];
    }
    if (m3g->statistics[M3G_STAT_MEMORY_MALLOC_BYTES] > m3g->statistics[M3G_STAT_MEMORY_MALLOC_PEAK]) {
        m3g->statistics[M3G_STAT_MEMORY_MALLOC_PEAK] = m3g->statistics[M3G_STAT_MEMORY_MALLOC_BYTES];
    }
    if (m3g->statistics[M3G_STAT_MEMORY_OBJECT_BYTES] > m3g->statistics[M3G_STAT_MEMORY_OBJECT_PEAK]) {
        m3g->statistics[M3G_STAT_MEMORY_OBJECT_PEAK] = m3g->statistics[M3G_STAT_MEMORY_OBJECT_BYTES];
    }

    /* Output peaks in 100 KB increments to reduce amont of log clutter */
    
    if (m3g->statistics[M3G_STAT_MEMORY_PEAK] - m3g->lastPeak > (100 << 10)) {
        m3gLogMemoryPeakCounter(m3g);
        m3g->lastPeak = m3g->statistics[M3G_STAT_MEMORY_PEAK];
    }
}
#endif /* M3G_ENABLE_PROFILING */

#if defined(M3G_ENABLE_PROFILING) && defined(M3G_TARGET_SYMBIAN)
extern M3Gbool m3gProfileTriggered(void);
#endif

/*!
 * \internal
 * \brief Output profiling counters to the log
 */
#if defined(M3G_ENABLE_PROFILING) && (M3G_PROFILE_LOG_INTERVAL > 0)
static void m3gLogProfileCounters(Interface *m3g)
{
    M3Gint profTime;
#   if defined(M3G_TARGET_SYMBIAN)
    profTime = m3gProfileTriggered();
    if (profTime > 0) {
#   else
    profTime = M3G_PROFILE_LOG_INTERVAL;
    if (++m3g->profileInterval >= M3G_PROFILE_LOG_INTERVAL) {
#   endif
        M3Gint v, i;
            
        M3G_LOG1(M3G_LOG_PROFILE, "Profile %d:", profTime);

        for (i = 0; i < M3G_STAT_MAX; ++i) {
            v = m3gGetStatistic(m3g, i);
            M3G_LOG1(M3G_LOG_PROFILE, " %d", v);
        }
        M3G_LOG(M3G_LOG_PROFILE, "\n");
        m3g->profileInterval = 0;
    }
}
#endif /* M3G_ENABLE_PROFILING && M3G_PROFILE_LOG_INTERVAL > 0 */

/*----------------------------------------------------------------------
 * Public API implementation
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a new M3G interface
 *
 */
M3G_API M3GInterface m3gCreateInterface(
    /*@in@*//*@temp@*/ const M3Gparams *params)
{
    /* This is likely to get executed exactly once during the
     * execution of an application, so before doing anything else,
     * execute the run-time sanity checks */

    if (!m3gSystemCheck()) {
        M3G_ASSERT(M3G_FALSE);
        return NULL;
    }

    /* Allow for deletion of existing log files */
    
#   if defined(M3G_LOGLEVEL)
    m3gBeginLog();
#   endif
    
    /* To actually create the interface, first check the supplied
     * function pointers */
    
    if (params == NULL
        || params->mallocFunc == NULL
        || params->freeFunc == NULL) {
        return NULL;
    }
    if (params->objAllocFunc != NULL
        && (params->objResolveFunc == NULL
            || params->objFreeFunc == NULL)) {
        return NULL;
    }
    
    /* Allocate the interface using the provided malloc function and
     * initialize. Note that this is slightly different from all other
     * constructors due to the memory instrumentation being done
     * directly rather than via m3gAlloc which isn't usable yet. */
    {
        Interface *m3g = (*params->mallocFunc)(INSTRUMENTATED_SIZE(sizeof(Interface)));
        if (m3g == NULL) {
            M3G_LOG(M3G_LOG_FATAL_ERRORS, "Interface creation failed\n");
            return NULL;
        }
        M3G_LOG1(M3G_LOG_INTERFACE, "New interface 0x%08X\n", (unsigned) m3g);
        
        m3g = (Interface *) PAYLOAD_BLOCK(m3g);
#       if defined(M3G_DEBUG_HEAP_TRACKING)
        instrumentateBlock(m3g, sizeof(*m3g), __FILE__, __LINE__);
#       else
        instrumentateBlock(m3g, sizeof(*m3g));
#       endif
        m3gZero(m3g, sizeof(*m3g));
            
        m3g->func.malloc = params->mallocFunc;
        m3g->func.free   = params->freeFunc;

        if (params->objAllocFunc) {
            m3g->func.objAlloc   = params->objAllocFunc;
            m3g->func.objFree    = params->objFreeFunc;
            m3g->func.objResolve = params->objResolveFunc;
        }
        else {
            m3g->func.objAlloc   = (m3gObjectAllocator*)(m3g->func.malloc);
            m3g->func.objFree    = (m3gObjectDeallocator*)(m3g->func.free);
            m3g->func.objResolve = defaultResolver;
        }

        m3g->func.error = params->errorFunc;
        m3g->func.getFrameBuffer = params->beginRenderFunc;
        m3g->func.releaseFrameBuffer = params->endRenderFunc;

        m3g->userContext = params->userContext;
        
        /* Initialize memory allocation failure debugging */
#       if defined(M3G_DEBUG_OUT_OF_MEMORY)
        {
            const char *str;
            
#           define M3G_GETENV(name, field)      \
                str = getenv(name);             \
                if (str) {                      \
                    m3g-> ## field = atoi(str); \
                }

            M3G_GETENV("M3G_DEBUG_MALLOC_LIMIT", mallocLimit);
            M3G_GETENV("M3G_DEBUG_OBJALLOC_LIMIT", objAllocLimit);
            M3G_GETENV("M3G_DEBUG_MALLOC_FAILRATE", mallocFailRate);
            M3G_GETENV("M3G_DEBUG_OBJALLOC_FAILRATE", objAllocFailRate);

#           undef M3G_GETENV
        }
#       endif

#       if !defined(M3G_NGL_CONTEXT_API)
        /* Before messing with EGL state, check if EGL is already
         * initialized by the calling application. */
    
        if (eglQueryString(eglGetDisplay(EGL_DEFAULT_DISPLAY), EGL_VERSION)) {
            ++m3g->glRefCount;
        }
#       endif /*!M3G_NGL_CONTEXT_API*/
        
        /* Dig some constants from the GL implementation */
        
        m3gConfigureGL(m3g);

        /* All done! Now we can allocate the more trival stuff */

        m3g->tcache = m3gCreateTransformCache(m3g);
        m3gInitArray(&m3g->objects);
        
        M3G_LOG1(M3G_LOG_INTERFACE,
                 "Interface 0x%08X initialized\n", (unsigned) m3g);
        return (M3GInterface) m3g;
    }
}

/*!
 * \brief Deletes an M3G interface and all associated objects
 */
M3G_API void m3gDeleteInterface(M3GInterface interface)
{
    Interface *m3g = (Interface *)interface;
    M3G_VALIDATE_INTERFACE(m3g);
    M3G_LOG1(M3G_LOG_INTERFACE,
             "Shutting down interface 0x%08X...\n", (unsigned) m3g);

    /* Check if we still have objects lingering (this may happen when
     * Java GC deletes the interface first, for instance), and just
     * mark the interface for deletion in that case */
    
    if (m3g->objCount > 0) {
        M3G_ASSERT(!interface->shutdown);
        M3G_LOG1(M3G_LOG_INTERFACE, "Waiting for %d objects\n",
                 interface->objCount);
        interface->shutdown = M3G_TRUE;
        return;
    }

    m3gDestroyArray(&m3g->objects, m3g);
#   if !defined(M3G_NGL_TEXTURE_API)
    /* Free the list of dead GL objects (those will have been deleted
     * along with the owning contexts by now) */

    m3gDestroyArray(&m3g->deadGLObjects, m3g);
#   endif

    /* Delete temp buffers and caches */
    
    M3G_ASSERT(!m3g->tempLocked);
    m3gFree(m3g, m3g->tempBuffer);

    m3gDeleteTransformCache(m3g->tcache);
    
    /* Check for any leaked memory */
#   if defined(M3G_DEBUG)
    if (m3g->mallocCount != 0 || m3g->objAllocCount != 0) {
        M3G_LOG(M3G_LOG_FATAL_ERRORS, "Memory leak detected!\n");
        if (m3g->mallocCount != 0) {
            M3G_LOG1(M3G_LOG_FATAL_ERRORS,
                     "\t%d memory blocks\n", m3g->mallocCount);
#           if defined(M3G_DEBUG_HEAP_TRACKING) && defined(M3G_LOGLEVEL)
            M3G_LOG(M3G_LOG_FATAL_ERRORS, "Dumping blocks...\n");
            dumpBlocks(m3g->blockList);
#           endif
        }
        if (m3g->objAllocCount != 0) {
            M3G_LOG1(M3G_LOG_FATAL_ERRORS,
                     "\t%d memory objects\n", m3g->objAllocCount);
        }
    }
#   endif /* M3G_DEBUG */

    /* Cleanup profiling resources */
    m3gCleanupProfile();
    m3gLogMemoryPeakCounter(m3g);
    
    /* Delete self */
    {
        m3gFreeFunc *freeFunc = m3g->func.free;
        destroyBlock(m3g);
        (*freeFunc)(PHYSICAL_BLOCK(m3g));
    }

    M3G_LOG1(M3G_LOG_INTERFACE,
             "Interface 0x%08X destroyed\n", (unsigned) m3g);

    /* Allow for log cleanup */
    
#   if defined(M3G_LOGLEVEL)
    m3gEndLog();
#   endif    
}

/*!
 * \brief Returns the latest error that occurred on an M3G interface
 *
 * Returns the latest error for \c interface, and resets the error
 * status to M3G_NO_ERROR.
 *
 * @param interface handle of the interface to query for errors
 */
M3G_API M3Genum m3gGetError(M3GInterface interface)
{
    Interface *m3g = (Interface *)interface;
    M3G_VALIDATE_INTERFACE(m3g);
    {
        M3Genum error = m3g->error;
        m3g->error = M3G_NO_ERROR;
        return error;
    }
}

/*!
 * \brief Returns the user context data pointer associated with an M3G interface
 *
 * User context data can be associated with an interface via the
 * M3GParams struct in m3gCreateInterface.
 * 
 * @param interface handle of the interface
 * @return pointer to the user context
 */
M3G_API void *m3gGetUserContext(M3GInterface interface)
{
    Interface *m3g = (Interface *)interface;
    M3G_VALIDATE_INTERFACE(m3g);
    return m3g->userContext;
}

/*!
 * \brief Returns if antialiasing is supported
 *
 * User context data can be associated with an interface via the
 * M3GParams struct in m3gCreateInterface.
 * 
 * @param interface handle of the interface
 * @return pointer to the user context
 */
M3G_API M3Gbool m3gIsAntialiasingSupported(M3GInterface interface)
{
    Interface *m3g = (Interface *)interface;
    M3G_VALIDATE_INTERFACE(m3g);
    return m3g->supportAntialiasing;
}

/*!
 * \brief Free memory by removing all detached objects
 *
 * Garbage collection will run automatically during normal operation,
 * so there is no need to call this function. However, it allows the
 * application to signal a suitable time to invest more time in
 * garbage collection, potentially improving performance later on.
 */
M3G_API void m3gGarbageCollect(M3GInterface interface)
{
    Interface *m3g = (Interface *) interface;
    M3G_VALIDATE_INTERFACE(m3g);
    m3gGarbageCollectAll(m3g);
}

/*!
 * \brief Returns the transformation cache for this interface
 */
static TCache *m3gGetTransformCache(Interface *m3g)
{
    return m3g->tcache;
}

/*!
 * \brief Returns the value of a given statistic
 *
 * If the statistic is a counter (such as number of rendering calls),
 * its value is also cleared.
 *
 * \note Dependent on the M3G_ENABLE_PROFILING compile-time flag; if
 * undefined, all statistic queries return zero
 */
#if defined(M3G_ENABLE_PROFILING)
/*@access M3GInterface@*/
M3G_API M3Gint m3gGetStatistic(M3GInterface hInterface, M3Gstatistic stat)
{
    Interface *m3g = (Interface*) hInterface;
    M3G_VALIDATE_INTERFACE(m3g);
    
    if (m3gInRange(stat, 0, M3G_STAT_MAX-1)) {
        M3Gint value = m3g->statistics[stat];

        if (stat < M3G_STAT_CUMULATIVE) {
            m3g->statistics[stat] = 0;
        }
    
        return value;
    }
    else {
        m3gRaiseError(m3g, M3G_INVALID_ENUM);
        return -1;
    }
}
#else
M3G_API M3Gint m3gGetStatistic(M3GInterface hInterface, M3Gstatistic stat)
{
    M3G_UNREF(hInterface);
    M3G_UNREF(stat);
    return 0;
}
#endif /*M3G_ENABLE_PROFILING*/


#undef INSTRUMENTATED_SIZE
#undef PAYLOAD_BLOCK
#undef PHYSICAL_BLOCK

