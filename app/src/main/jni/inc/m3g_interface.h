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
* Description: M3G interface class internal interface
*
*/

#ifndef __M3G_INTERFACE_H__
#define __M3G_INTERFACE_H__

/*!
 * \internal
 * \file
 * \brief M3G interface class internal interface
 */

typedef struct M3GInterfaceImpl Interface;

/*--------------------------------------------------------------------
 * Memory allocation functions
 *------------------------------------------------------------------*/

#if !defined(M3G_DEBUG_HEAP_TRACKING)

static /*@only@*//*@null@*/ void* m3gAlloc(/*@in@*/ Interface *m3g,
                                           M3Gsize bytes);

static /*@only@*//*@null@*/ void* m3gAllocZ(/*@in@*/ Interface *m3g,
                                            M3Gsize bytes);

static void m3gFree(/*@in@*/ Interface *m3g,
                    /*@only@*//*@out@*//*@null@*/ void *ptr);

static /*@only@*//*@null@*/ M3GMemObject m3gAllocObject(Interface *m3g,
                                                        M3Gsize bytes);
static void m3gFreeObject(Interface *m3g,
                          /*@only@*//*@out@*//*@null@*/ M3GMemObject handle);

#define m3gMarkObject(p)
#define m3gIsObject(p) (M3G_FALSE)
#define m3gUnmarkObject(p)

#endif /* !M3G_DEBUG_HEAP_TRACKING */

#if 0
static void m3gRecycle(Interface *m3g, void *ptr, M3Gsize bytes);
#endif

static M3Gbool m3gGetColorMaskWorkaround(Interface *m3g);
static M3Gbool m3gGetTwoSidedLightingWorkaround(Interface *m3g);

/*--------------------------------------------------------------------
 * Debug memory allocation functions
 *------------------------------------------------------------------*/

#if defined(M3G_DEBUG_HEAP_TRACKING)

static /*@only@*//*@null@*/ void* m3gDebugAlloc(
    /*@in@*/ Interface *m3g, M3Gsize bytes, const char *file, int line);

static /*@only@*//*@null@*/ void* m3gDebugAllocZ(
    /*@in@*/ Interface *m3g, M3Gsize bytes, const char *file, int line);

static void m3gDebugFree(/*@in@*/ Interface *m3g,
                         /*@only@*//*@out@*//*@null@*/ void *ptr,
                         const char *file, int line);

static /*@only@*//*@null@*/ M3GMemObject m3gDebugAllocObject(
    Interface *m3g, M3Gsize bytes, const char *file, int line);

static void m3gDebugFreeObject(
    Interface *m3g, /*@only@*//*@out@*//*@null@*/ M3GMemObject handle,
    const char *file, int line);

#define m3gAlloc(m3g, bytes) \
    m3gDebugAlloc(m3g, bytes, __FILE__, __LINE__)
#define m3gAllocZ(m3g, bytes) \
    m3gDebugAllocZ(m3g, bytes, __FILE__, __LINE__)

#define m3gAllocObject(m3g, bytes) \
    m3gDebugAllocObject(m3g, bytes, __FILE__, __LINE__)

#define m3gFree(m3g, ptr)       m3gDebugFree(m3g, ptr, __FILE__, __LINE__)
#define m3gFreeObject(m3g, obj) m3gDebugFreeObject(m3g, obj, __FILE__, __LINE__)

static void m3gMarkObject(void *ptr);
static M3Gbool m3gIsObject(const void *ptr);
static void m3gUnmarkObject(void *ptr);
    
#endif /* M3G_DEBUG_HEAP_TRACKING */

/*----------------------------------------------------------------------
 * Profiling
 *--------------------------------------------------------------------*/

#if defined(M3G_ENABLE_PROFILING)

static void m3gIncStat(Interface *m3g, M3Gstatistic stat, M3Gint increment);
static void m3gResetStat(Interface *m3g, M3Gstatistic stat);
static void m3gUpdateMemoryPeakCounter(Interface *m3g);
static void m3gLogMemoryPeakCounter(const Interface *m3g);
static void m3gLogProfileCounters(Interface *m3g);
extern void m3gBeginProfile(int stat);
extern int m3gEndProfile(int stat);
extern void m3gCleanupProfile(void);
#define M3G_BEGIN_PROFILE(m3g, stat) m3gBeginProfile(stat);
#define M3G_END_PROFILE(m3g, stat)   m3gIncStat(m3g, stat, m3gEndProfile(stat));

#else

#define m3gIncStat(m3g, stat, increment) (M3G_UNREF(m3g), M3G_UNREF(stat), M3G_UNREF(increment))
#define m3gResetStat(m3g, stat)         (M3G_UNREF(m3g), M3G_UNREF(stat))
#define m3gUpdateMemoryPeakCounter(m3g) (M3G_UNREF(m3g))
#define m3gLogMemoryPeakCounter(m3g)    (M3G_UNREF(m3g))
#define m3gLogProfileCounters(m3g)      (M3G_UNREF(m3g))
#define m3gCleanupProfile()
#define M3G_BEGIN_PROFILE(m3g, stat)    (M3G_UNREF(m3g), M3G_UNREF(stat))
#define M3G_END_PROFILE(m3g, stat)      (M3G_UNREF(m3g), M3G_UNREF(stat))
#endif

/*--------------------------------------------------------------------
 * Other internal functions
 *------------------------------------------------------------------*/

static void     m3gAddChildObject(Interface *m3g, Object *obj);
static void     m3gDelChildObject(Interface *m3g, Object *obj);

#include "m3g_array.h"
static void     m3gGetObjectsWithClassID(Interface *m3g, M3GClass classID, PointerArray* objects);

#if !defined(M3G_NGL_TEXTURE_API)
static void     m3gDeleteGLTextures(Interface *m3g, M3Gsizei n, M3Guint *t);
static void     m3gCollectGLObjects(Interface *m3g);
#else
#   define m3gCollectGLObjects(a)
#endif

static void*    m3gAllocTemp(Interface *m3g, M3Gsizei bytes);
static void     m3gFreeTemp(Interface *m3g);

static void*    m3gMapObject    (Interface *m3g, M3GMemObject handle);
static void     m3gUnmapObject  (Interface *m3g, M3GMemObject handle);

static void     m3gGarbageCollectAll            (Interface *m3g);

#include "m3g_tcache.h"
static TCache *m3gGetTransformCache(Interface *m3g);

#if defined(M3G_NGL_CONTEXT_API)
static void *m3gGetExternalFB(Interface *m3g, M3Guint userTarget);
static void m3gReleaseExternalFB(Interface *m3g, M3Guint userTarget);
static void m3gSignalTargetRelease(Interface *m3g, M3Guint userTarget);
#endif /* M3G_NGL_CONTEXT_API */

#if defined(M3G_DEBUG)
static void m3gDebugRaiseError(/*@in@*/ Interface *m3g,
                               M3Genum error,
                               const char *filename,
                               int line);
#define m3gRaiseError(m3g, error) \
    m3gDebugRaiseError(m3g, error, __FILE__, __LINE__)
#else
static void m3gRaiseError(/*@in@*/ Interface *m3g, M3Genum error);
#endif /* M3G_DEBUG */

#ifdef M3G_NATIVE_LOADER
static M3GError m3gErrorRaised(const Interface *m3g);
static m3gErrorHandler *m3gSetErrorHandler(Interface *m3g, m3gErrorHandler *errorHandler);
#endif

/* Memory locking only affects assertions in debug builds, so let's
 * disable it in non-debug ones for a bit of extra performance */

#if defined(M3G_DEBUG)
#define m3gLockMemory(a) m3gDebugLockMemory((a), __FILE__, __LINE__)
static void    m3gDebugLockMemory(Interface *interface, const char *file, int line);
static void    m3gUnlockMemory  (Interface *interface);
static M3Gbool m3gMemoryLocked  (Interface *interface);
#else
#   define m3gLockMemory(a)   ((void)(a))
#   define m3gUnlockMemory(a) ((void)(a))
#   define m3gMemoryLocked(a) (M3G_FALSE)
#endif

#if defined(M3G_NGL_CONTEXT_API) && !defined(M3G_NGL_TEXTURE_API)
#   define M3G_ASSERT_NO_LOCK(interface)
#else
#   define M3G_ASSERT_NO_LOCK(interface) M3G_ASSERT(!m3gMemoryLocked(interface))
#endif

static void m3gInitializeGL(Interface *interface);
static void m3gShutdownGL(Interface *interface);

/* Self-validation */

#if defined(M3G_DEBUG)
/*@notfunction@*/
#   define M3G_VALIDATE_MEMBLOCK(ptr) m3gValidateMemory(ptr)
static void m3gValidateMemory(const void *ptr);
/*@notfunction@*/
#   define M3G_VALIDATE_INTERFACE(m3g) m3gValidateInterface(m3g)
static void m3gValidateInterface(const Interface *m3g);

#else
#   define M3G_VALIDATE_MEMBLOCK(ptr)
#   define M3G_VALIDATE_INTERFACE(m3g)
#endif

#endif /*__M3G_INTERFACE_H__*/
