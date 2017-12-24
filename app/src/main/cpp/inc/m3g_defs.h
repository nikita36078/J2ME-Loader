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
* Description: Global definitions
*
*/

#ifndef __M3G_DEFS_H__
#define __M3G_DEFS_H__

/*!
 * \internal
 * \file
 * \brief Global definitions for the Nokia M3G API implementation
 */

/* Include public API */
#include "M3G/m3g_core.h"

#if defined(__cplusplus)
extern "C" {
#endif

/* Try to recognize debug builds */
#if defined(_DEBUG) || defined(DEBUG)
#undef M3G_DEBUG
#define M3G_DEBUG
#endif

/*----------------------------------------------------------------------
 * Platform dependent definitions
 *--------------------------------------------------------------------*/

/* Define constants for use in m3g_config.h */
    
#define M3G_LOG_FATAL_ERRORS    0x0001
#define M3G_LOG_USER_ERRORS     0x0002
#define M3G_LOG_WARNINGS        0x0003
#define M3G_LOG_PROFILE         0x0004
#define M3G_LOG_INTERFACE       0x0010
#define M3G_LOG_OBJECTS         0x0020
#define M3G_LOG_STAGES          0x0040
#define M3G_LOG_REFCOUNT        0x0080
#define M3G_LOG_MEMORY_USAGE    0x0100
#define M3G_LOG_MEMORY_BLOCKS   0x0200
#define M3G_LOG_MEMORY_MAPPING  0x0400
#define M3G_LOG_MEMORY_ALL      0x0F00
#define M3G_LOG_RENDERING       0x1000
#define M3G_LOG_IMAGES          0x2000
#define M3G_LOG_ALL             0xFFFF
    
/* Include the platform configuration file; the possible configuration
 * options are documented and their default values set below */
    
#include "m3g_config.h"

/*!
 * \internal
 * \def M3G_TARGET_GENERIC
 * \brief Enables a generic C build
 */   
/*!
 * \internal
 * \def M3G_TARGET_ISA
 * \brief Enables building for ISA
 */   
/*!
 * \internal
 * \def M3G_TARGET_SYMBIAN
 * \brief Enables building for Symbian
 */
/*!
 * \internal
 * \def M3G_TARGET_WIN32
 * \brief Enables building for Win32
 */      
#if defined(M3G_TARGET_GENERIC)
#elif defined(M3G_TARGET_ISA)
#elif defined(M3G_TARGET_SYMBIAN)
#elif defined(M3G_TARGET_WIN32)
#elif defined(S_SPLINT_S) /* auto-recognize Splint as a special case */
#   define M3G_TARGET_GENERIC
#else
#   error Build target undefined! Supply one of the M3G_TARGET_* definitions.
#endif
    
/*!
 * \internal
 * \def M3G_USE_NGL_API
 * \brief Enable the legacy "NGL" OpenGL context management interface
 */
#if defined(M3G_USE_NGL_API)
#   define M3G_NGL_CONTEXT_API
#   define M3G_NGL_TEXTURE_API
#endif
    
#if defined(M3G_TARGET_ISA)
#   if !defined(M3G_NGL_CONTEXT_API)
#       error ISA builds must define M3G_USE_NGL_API
#   endif
#endif   
#if defined(M3G_TARGET_SYMBIAN)
#   if defined(M3G_NGL_TEXTURE_API) || defined(M3G_NGL_CONTEXT_API)
#       error Legacy NGL API not supported in Symbian builds
#   endif
#endif
    
/*!
 * \internal
 * \def M3G_ALIGNMENT
 * \brief Alignment, in bytes, mandated for internal data structures
 *
 * This must meet or exceed the alignment requirement of the
 * underlying hardware. The default is 4 bytes.
 */
#if !defined(M3G_ALIGNMENT)
#   define M3G_ALIGNMENT        4
#endif

/*!
 * \internal
 * \def M3G_MAX_GL_CONTEXTS
 * \brief Maximum number of GL contexts to cache at once
 *
 * \note This only applies to builds using the EGL API.
 */
#if !defined(M3G_MAX_GL_CONTEXTS)
#   define M3G_MAX_GL_CONTEXTS  3
#endif

/*!
 * \internal
 * \def M3G_MAX_GL_SURFACES
 * \brief Maximum number of GL surfaces to cache at once
 *
 * \note This only applies to builds using the EGL API.
 */
#if !defined(M3G_MAX_GL_SURFACES)
#   define M3G_MAX_GL_SURFACES  4
#endif

/*!
 * \internal
 * \def M3G_MAX_LIGHTS
 * \brief Maximum number of lights in use simultaneously
 *
 * This is a rendering quality / performance trade-off; less lights
 * will be (slightly) faster, but reduce the quality of lighting.
 * 
 * \note The value set here must not exceed the limits of the
 * underlying OpenGL implementation; this is not automatically
 * verified, but the default should be safe for all compliant OpenGL
 * ES implementation
 */
#if !defined(M3G_MAX_LIGHTS)
#   define M3G_MAX_LIGHTS       8
#endif

/*!
 * \internal
 * \def M3G_MAX_TEXTURE_DIMENSION
 * \brief Maximum supported texture dimension
 *
 * \note The value set here must not exceed the limits of the
 * underlying OpenGL implementation; this is not automatically
 * verified, but the default should be safe for all compliant OpenGL
 * ES implementation
 */
#if !defined(M3G_MAX_TEXTURE_DIMENSION)
#   define M3G_MAX_TEXTURE_DIMENSION    256
#endif
    
/*!
 * \internal
 * \def M3G_MAX_VERTEX_TRANSFORMS
 * \brief Maximum number of transforms per vertex
 *
 * \note The value set here must not exceed the limits of the
 * underlying OpenGL implementation; this is not automatically
 * verified
 */
#if !defined(M3G_MAX_VERTEX_TRANSFORMS)
#   define M3G_MAX_VERTEX_TRANSFORMS    4
#endif

/*!
 * \internal
 * \def M3G_MAX_VIEWPORT_DIMENSION
 * \brief Maximum supported viewport dimension
 *
 * \note The value set here must not exceed the limits of the
 * underlying OpenGL implementation; this is not automatically
 * verified, but the default should be safe for all compliant OpenGL
 * ES implementation
 */
#if !defined(M3G_MAX_VIEWPORT_DIMENSION)
#   define M3G_MAX_VIEWPORT_DIMENSION   1024
#endif

/*!
 * \internal
 * \def M3G_MAX_VIEWPORT_WIDTH
 * \brief Maximum supported viewport width
 *
 * \note The value set here must not exceed the limits of the
 * underlying OpenGL implementation; this is not automatically
 * verified, but the default should be safe for all compliant OpenGL
 * ES implementation
 */
#if !defined(M3G_MAX_VIEWPORT_WIDTH)
#   define M3G_MAX_VIEWPORT_WIDTH       1024
#endif

/*!
 * \internal
 * \def M3G_MAX_VIEWPORT_HEIGHT
 * \brief Maximum supported viewport height
 *
 * \note The value set here must not exceed the limits of the
 * underlying OpenGL implementation; this is not automatically
 * verified, but the default should be safe for all compliant OpenGL
 * ES implementation
 */
#if !defined(M3G_MAX_VIEWPORT_HEIGHT)
#   define M3G_MAX_VIEWPORT_HEIGHT      1024
#endif

/*!
 * \internal
 * \def M3G_NATIVE_LOADER
 * \brief Include a native loader implementation
 *
 * This is always enabled in current builds.
 */
#undef M3G_NATIVE_LOADER
#define M3G_NATIVE_LOADER
    
/*!
 * \internal
 * \def M3G_NUM_TEXTURE_UNITS
 * \brief Number of texture units to support
 *
 * \note The value set here must not exceed the limits of the
 * underlying OpenGL implementation; this is not automatically
 * verified, but the default should be safe for all compliant OpenGL
 * ES implementation
 */
#if !defined(M3G_NUM_TEXTURE_UNITS)
#   define M3G_NUM_TEXTURE_UNITS        2
#endif

/*!
 * \internal
 * \def M3G_SUPPORT_ANTIALIASING
 * \brief Enable or disable support for antialiasing
 *
 * \note This is currently only supported for Symbian.
 */
#if !defined(M3G_SUPPORT_ANTIALIASING)
#   if defined(M3G_NGL_CONTEXT_API)
#       define M3G_SUPPORT_ANTIALIASING         M3G_FALSE
#   else
#       define M3G_SUPPORT_ANTIALIASING         M3G_TRUE
#   endif
#endif

/*!
 * \internal
 * \def M3G_SUPPORT_DITHERING
 * \brief Enable or disable support for dithering
 */
#if !defined(M3G_SUPPORT_DITHERING)
#   define M3G_SUPPORT_DITHERING                M3G_FALSE
#endif

/*!
 * \internal
 * \def M3G_SUPPORT_LOCAL_CAMERA_LIGHTING
 * \brief Enable or disable support for local camera lighting
 */
#if !defined(M3G_SUPPORT_LOCAL_CAMERA_LIGHTING)
#   define M3G_SUPPORT_LOCAL_CAMERA_LIGHTING    M3G_FALSE
#endif

/*!
 * \internal
 * \def M3G_SUPPORT_MIPMAPPING
 * \brief Enable or disable support for mipmapping
 */
#if !defined(M3G_SUPPORT_MIPMAPPING)
#   define M3G_SUPPORT_MIPMAPPING               M3G_TRUE
#endif

/*!
 * \internal
 * \def M3G_SUPPORT_PERSPECTIVE_CORRECTION
 * \brief Enable or disable support for perspective correct texturing
 */
#if !defined(M3G_SUPPORT_PERSPECTIVE_CORRECTION)
#   define M3G_SUPPORT_PERSPECTIVE_CORRECTION   M3G_TRUE
#endif

/*!
 * \internal
 * \def M3G_SUPPORT_TRUE_COLOR
 * \brief Enable or disable support for "true color" rendering
 */
#if !defined(M3G_SUPPORT_TRUE_COLOR)
#   define M3G_SUPPORT_TRUE_COLOR               M3G_FALSE
#endif

/*!
 * \internal
 * \def M3G_USE_16BIT_TEXTURES
 * \brief Use 16-bit RGB textures internally to save memory
 *
 * This may reduce in some loss of performance, so it is defined
 * M3G_FALSE by default.
 */
#if !defined(M3G_USE_16BIT_TEXTURES)
#   define M3G_USE_16BIT_TEXTURES               M3G_FALSE
#endif
    
/*!
 * \internal
 * \def M3G_LOGLEVEL
 * 
 * \brief Controls the amount of log output
 *
 * Log output can be enabled by defining M3G_LOGLEVEL in m3g_config.h.
 * It must be set to a bitmask indicating which categories of log
 * messages to output.  Fatal internal errors are always output when
 * M3G_LOGLEVEL is defined; other kinds of messages can be included by
 * setting it to any combination of the following in m3g_config.h:
 *
 *  M3G_LOG_FATAL_ERRORS    fatal internal errors (equals 0)
 *  M3G_LOG_USER_ERRORS     user errors (the same as reported
 *                          via m3gGetError)
 *  M3G_LOG_WARNINGS        performance warnings
 *  M3G_LOG_PROFILE         profiling counters
 *  M3G_LOG_INTERFACE       interface construction/destruction events
 *  M3G_LOG_OBJECTS         object construction/destruction events
 *  M3G_LOG_STAGES          log processing stages (animate, render, etc.)
 *  M3G_LOG_REFCOUNT        reference count operations
 *  M3G_LOG_MEMORY_USAGE    memory usage counters
 *  M3G_LOG_MEMORY_BLOCKS   memory block allocs/deallocs
 *  M3G_LOG_MEMORY_ALL      output everything memory-related
 *  M3G_LOG_RENDERING       log rendering details
 *  M3G_LOG_IMAGES          log image memory usage details
 *
 * \note The amount of log output may be limited in non-debug builds.
 */

/*!
 * \internal
 * \def M3G_PROFILE_LOG_INTERVAL
 * \brief Profile logging interval
 * 
 * Number of frames to wait between outputting (and resetting!) the
 * profiling counters.  Zero (default) disables profile logging.
 */
#if !defined(M3G_PROFILE_LOG_INTERVAL)
#   define M3G_PROFILE_LOG_INTERVAL     0
#endif
    
/*----------------------------------------------------------------------
 * Debug build setup
 *--------------------------------------------------------------------*/

/* General settings enabled in debug builds */
#if defined(M3G_DEBUG)
#   define M3G_DEBUG_ASSERTS
#   define M3G_DEBUG_RANGE_CHECKING
#   define M3G_DEBUG_HEAP_TRACKING
#endif

/*----------------------------------------------------------------------
 * Compiler dependent definitions
 *--------------------------------------------------------------------*/

/* Try to recognize the compiler */
#if !defined(M3G_NO_COMPILER_DETECTION)
#   if (defined (_MSC_VER) || defined(__VC32__))&& !defined(__MWERKS__) /* Microsoft Visual C++ */
#       undef M3G_BUILD_MSVC
#       define M3G_BUILD_MSVC
#   elif defined(__ARMCC_VERSION)   /* ARM Developer Suite */
#       undef M3G_BUILD_ADS
#       define M3G_BUILD_ADS
#   elif defined(__BORLANDC__)      /* Borland C++ */
#       undef M3G_BUILD_BORLAND
#       define M3G_BUILD_BORLAND
#   elif defined(__MWERKS__) || defined(__CW32__) /* CodeWarrior */
#       undef M3G_BUILD_CW
#       define M3G_BUILD_CW
#   elif defined(__GNUC__) || defined(__GCC32__) /* GNU C */
#       undef M3G_BUILD_GCC
#       define M3G_BUILD_GCC
#   elif defined(__ARMCC__)         /* RVCT */
#       undef M3G_BUILD_RVCT
#       define M3G_BUILD_RVCT
#   elif defined(S_SPLINT_S)        /* Splint */
#       undef M3G_BUILD_SPLINT
#       define M3G_BUILD_SPLINT
#   else
#       error Could not identify the compiler. Please refer to m3g_defs.h for more information.
#   endif
#endif

/* Try to recognize thumb/arm */
#if defined(M3G_BUILD_ADS)
#   if defined(__thumb)
#       define M3G_BUILD_THUMB
#   else
#       define M3G_BUILD_ARM
#   endif
#endif

/* Set up compiler-dependent definitions */

/*@-macroparams@*/
/*@notfunction@*/
/*!
 * \internal \brief Macro used to denote Splint constraints */
#define M3G_SPLINT(def)
/*@+macroparams@*/

/*!
 * \internal \def M3G_INLINE
 * \brief Platform-independent inline function specifier
 */

#if defined(M3G_BUILD_MSVC)
#   define M3G_INLINE  __inline
#   pragma warning (disable:4127) /* 4127: conditional expression is constant */
#   pragma warning (disable:4514) /* 4514: unreferenced inline function has been removed */
#   pragma warning (disable:4710) /* 4710: function not inlined */

#elif defined (M3G_BUILD_ADS)
#   define M3G_INLINE __inline

#elif defined(M3G_BUILD_BORLAND)
#   define M3G_INLINE  __inline

#elif defined(M3G_BUILD_CW)
#   define M3G_INLINE
    
#elif defined(M3G_BUILD_GCC)
#   define M3G_INLINE inline

#elif defined(M3G_BUILD_RVCT)
#   define M3G_INLINE __inline
    
#elif defined(M3G_BUILD_SPLINT)
#   define M3G_INLINE
#   undef M3G_SPLINT
/*@notfunction@*/
#   define M3G_SPLINT(def) def

#   undef M3G_DEBUG
/*#   define M3G_DEBUG*/

#else           /* generic fallback */
#   define M3G_INLINE

#endif

/* Optionally cancel non-portable definitions. This is mainly to
 * facilitate portability checking with GCC, which defines __STDC__
 * even in non-ANSI mode) */

#if defined(M3G_STRICT_STDC)
#   undef M3G_INLINE
#   define M3G_INLINE
#endif

/*----------------------------------------------------------------------
 * Identify GL version
 *--------------------------------------------------------------------*/
#if defined(GL_VERSION_ES_CM_1_1) || defined(GL_OES_VERSION_1_1)
#   define M3G_GL_ES_1_1
#endif

/*----------------------------------------------------------------------
 * Internal configuration and performance tuning parameters
 *--------------------------------------------------------------------*/

#define M3G_RENDERQUEUE_BUCKET_BITS 8
#define M3G_ENABLE_VF_CULLING
#define M3G_GL_FORCE_PBUFFER_SIZE
    
/*----------------------------------------------------------------------
 * Standard C library
 *--------------------------------------------------------------------*/

#if defined(M3G_NO_STDLIB)
#   define NULL 0
typedef M3Guint M3Gsize;
#else
#   include <stdlib.h>
typedef size_t M3Gsize;
#endif

/*----------------------------------------------------------------------
 * Run time and compile time assertions
 *--------------------------------------------------------------------*/

/*!
 * \internal \def M3G_ASSERT(a)
 * \brief Run-time assertion
 */
#if defined(M3G_DEBUG_ASSERTS)

extern void m3gAssertFailed(const char *filename, int line);
#define M3G_ASSERT(cond)                                        \
    do {                                                        \
        if (!(cond)) m3gAssertFailed(__FILE__, __LINE__);       \
    } while (M3G_FALSE)

M3G_SPLINT(extern /*@noreturnwhenfalse@*/ void M3G_ASSERT(/*@sef@*//*@null@*/ M3Gbool /*@alt const void*@*/ cond)/*@*/;)

#else

/*@-macroparams@*/
#   define M3G_ASSERT(cond)
/*@+macroparams@*/
M3G_SPLINT(extern /*@noreturnwhenfalse@*/ void M3G_ASSERT(/*@sef@*//*@null@*//*@unused@*/ M3Gbool /*@alt const void*@*/ cond)/*@*/;)
    
#endif /* M3G_DEBUG */

/*!
 * \internal
 * \brief Compile-time assertion
 */
/*@notfunction@*/
#define M3G_CT_ASSERT(a)    struct __M3G_UNIQUE_NAME { unsigned bf : (a) ? 1 : -1; }
/*@notfunction@*/
#define __M3G_UNIQUE_NAME           __M3G_MAKE_UNIQUE_NAME(__LINE__, 0)
/*@notfunction@*/
#define __M3G_MAKE_UNIQUE_NAME(line, num)   __M3G_MAKE_UNIQUE_NAME2(line, num)
/*@notfunction@*/
#define __M3G_MAKE_UNIQUE_NAME2(line, num)  m3gCTassert ## num ## line

/*@notfunction@*/
#define M3G_CT_ASSERT1(a)   struct __M3G_UNIQUE_NAME1 { unsigned bf : (a) ? 1 : -1; }
/*@notfunction@*/
#define __M3G_UNIQUE_NAME1  M3G_MAKE_UNIQUE_NAME(__LINE__, 1)

/*@notfunction@*/
#define M3G_CT_ASSERT2(a)   struct __M3G_UNIQUE_NAME2 { unsigned bf : (a) ? 1 : -1; }
/*@notfunction@*/
#define __M3G_UNIQUE_NAME2  __M3G_MAKE_UNIQUE_NAME(__LINE__, 2)

#if defined(M3G_DEBUG_RANGE_CHECKING)
#define M3G_ASSERT_RANGE(val, min, max) M3G_ASSERT(m3gInRange((val), (min), (max)))
#else
/*@-macroparams@*/
#define M3G_ASSERT_RANGE(val, min, max)
/*@+macroparams@*/
#endif

/*----------------------------------------------------------------------
 * Verify any global definitions, expected type sizes etc.
 *--------------------------------------------------------------------*/

#if !defined(M3G_INLINE)
#   error M3G_INLINE not defined in platform definitions!
#endif

#if !defined(M3G_ALIGNMENT)
#   error M3G_ALIGNMENT not defined in platform definitions!
#endif

#if ((M3G_ALIGNMENT & (M3G_ALIGNMENT-1)) != 0)
#   error M3G_ALIGNMENT is not a power of two!
#endif

#if defined(M3G_TARGET_ISA) && !defined(M3G_USE_NGL_API)
#   error ISA targets currently supported with M3G_USE_NGL_API only!
#endif

#if (defined(M3G_NGL_CONTEXT_API) && !defined(M3G_NGL_TEXTURE_API)) \
        || (defined(M3G_NGL_TEXTURE_API) && !defined(M3G_NGL_CONTEXT_API))
#   if !defined(M3G_DEBUG)
#       error Nonstandard NGL configurations only allowed in debug builds!
#   endif
#endif

/* Verify our expected type sizes */
M3G_CT_ASSERT(sizeof(M3Gbyte)   == 1);
M3G_CT_ASSERT(sizeof(M3Gubyte)  == 1);
M3G_CT_ASSERT(sizeof(M3Gshort)  == 2);
M3G_CT_ASSERT(sizeof(M3Gushort) == 2);
M3G_CT_ASSERT(sizeof(M3Gint)    == 4);
M3G_CT_ASSERT(sizeof(M3Guint)   == 4);
M3G_CT_ASSERT(sizeof(M3Gfloat)   == 4);

/* Unsigned is used extensively as a wrapper for object pointers, so
 * check that we can fit a pointer in there */
M3G_CT_ASSERT(sizeof(M3Guint) >= sizeof(void*));

/*
 * Globally disable some redundant Lint messages
 */

/*lint -e701 Signed shifts left should never be a problem */
/*lint -e702 Signed shifts right are run-time verified (m3g_interface.c) */

/*----------------------------------------------------------------------
 * Event logging
 *--------------------------------------------------------------------*/

#if !defined(M3G_LOGLEVEL)    
#   define M3G_LOG(level, msg)              ((void)(level), (void)(msg))
#   define M3G_LOG1(level, msg, a)          ((void)(level), (void)(msg), (void)(a))
#   define M3G_LOG2(level, msg, a, b)       ((void)(level), (void)(msg), (void)(a), (void)(b))
#   define M3G_LOG3(level, msg, a, b, c)    ((void)(level), (void)(msg), (void)(a), (void)(b), (void)(c))
#   define M3G_LOG4(level, msg, a, b, c, d) ((void)(level), (void)(msg), (void)(a), (void)(b), (void)(c), (void)(d))
#   define M3G_LOG5(level, msg, a, b, c, d, e) ((void)(level), (void)(msg), (void)(a), (void)(b), (void)(c), (void)(d), (void)(e))
#else
    void m3gBeginLog(void);
    void m3gLogMessage(const char *format, ...);
    void m3gEndLog(void);
#   define M3G_LOG(level, msg) \
    do { if ((level) & (M3G_LOGLEVEL)) m3gLogMessage((msg)); } while (M3G_FALSE)
#   define M3G_LOG1(level, msg, a) \
    do { if ((level) & (M3G_LOGLEVEL)) m3gLogMessage((msg), (a)); } while (M3G_FALSE)
#   define M3G_LOG2(level, msg, a, b) \
    do { if ((level) & (M3G_LOGLEVEL)) m3gLogMessage((msg), (a), (b)); } while (M3G_FALSE)
#   define M3G_LOG3(level, msg, a, b, c) \
    do { if ((level) & (M3G_LOGLEVEL)) m3gLogMessage((msg), (a), (b), (c)); } while (M3G_FALSE)
#   define M3G_LOG4(level, msg, a, b, c, d) \
    do { if ((level) & (M3G_LOGLEVEL)) m3gLogMessage((msg), (a), (b), (c), (d)); } while (M3G_FALSE)
#   define M3G_LOG5(level, msg, a, b, c, d, e) \
    do { if ((level) & (M3G_LOGLEVEL)) m3gLogMessage((msg), (a), (b), (c), (d), (e)); } while (M3G_FALSE)
#endif
    
/*----------------------------------------------------------------------
 * Useful stuff
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Range checking inline function
 *
 * \param a     Value to check
 * \param min   Lower bound, inclusive
 * \param max   Upper bound, inclusive
 */
static M3G_INLINE M3Gbool m3gInRange(M3Gint a, M3Gint min, M3Gint max)
{
    return ((M3Guint)(a - min) <= (M3Guint)(max - min));
}

/*!
 * \internal
 * \brief Range checking for floats
 */
static M3G_INLINE M3Gbool m3gInRangef(M3Gfloat a, M3Gfloat min, M3Gfloat max)
{
    return (a >= min && a <= max);
}
    
/*!
 * \internal
 * \brief Clamping function for floats
 *
 * \param a     Value to clamp
 * \param min   Minimum value
 * \param max   Maximum value
 */
static M3G_INLINE M3Gfloat m3gClampFloat(M3Gfloat a, M3Gfloat min, M3Gfloat max)
{
    return (a <= min) ? min : (a >= max) ? max : a;
}

/*!
 * \internal
 * \brief Clamping function for floats
 *
 * \param a     Value to clamp
 */
static M3G_INLINE M3Gfloat m3gClampFloatPositive(M3Gfloat a)
{
    return (a <= 0) ? 0 : a;
}

/*!
 * \internal
 */
static M3G_INLINE M3Gint m3gClampInt(M3Gint a, M3Gint min, M3Gint max)
{
    return (a <= min) ? min : (a >= max) ? max : a;
}

/*!
 * \internal
 *
 */
static M3G_INLINE M3Gbool m3gIsPowerOfTwo(M3Guint a)
{
    return ((a & (a-1)) == 0u);
}

/*!
 * \internal
 * \brief Returns the smallest power of two greater than or equal to a
 * given integer
 *
 * \note Only works with positive numbers of 2^30 or less
 */
static M3G_INLINE M3Gint m3gNextPowerOfTwo(M3Gint x)
{
    M3Gint y = 1;
    M3G_ASSERT(m3gInRange(x, 0, 1 << 30));
    while (y < x) {
        y <<= 1;
    }
    return y;
}

/*!
 * \internal
 * \brief Indicate (intentionally) unreferenced parameters
 *
 * This is used on virtual functions, where not every implementation
 * uses all the arguments.
 */
/*@notfunction@*/
#define M3G_UNREF(a)    ((void)(a)) /*@*/

/*!
 * \internal
 * \brief Clamp a value to signed 8-bit range
 */
#define M3G_CLAMP_BYTE(a) ((M3Gbyte)  ((a) <   -128 ?   -128 : ((a) >   127 ?   127 : (a))))
M3G_SPLINT(extern M3Gbyte M3G_CLAMP_BYTE(/*@sef@*/ int a);)

/*!
 * \internal
 * \brief Clamp a value to signed 16-bit range
 */
#define M3G_CLAMP_SHORT(a) ((M3Gshort) ((a) < -32768 ? -32768 : ((a) > 32767 ? 32767 : (a))))
M3G_SPLINT(extern M3Gshort M3G_CLAMP_SHORT(/*@sef@*/ int a);)

/*!
 * \internal
 * \brief Clamp a value to unsigned 8-bit range
 */
#define M3G_CLAMP_UBYTE(a) ((M3Gubyte) ((a) < 0 ? 0 : ((a) >   255 ?   255 : (a))))
M3G_SPLINT(extern M3Gubyte M3G_CLAMP_UBYTE(/*@sef@*/ int a);)

/*!
 * \internal
 * \brief Clamp a value to unsigned 16-bit range
 */
#define M3G_CLAMP_USHORT(a) ((M3Gushort)((a) < 0 ? 0 : ((a) > 65535 ? 65535 : (a))))
M3G_SPLINT(extern M3Gushort M3G_CLAMP_USHORT(/*@sef@*/ int a);)

/*!
 * \internal
 * \brief Offset any pointer by a given number of bytes
 */
#define M3G_OFFSET_PTR(ptr, bytes) (void*)(((M3Gbyte*)(ptr)) + bytes)

/* Min & max macros and inline functions
 *
 * The inline function may be better optimized when one or both of the
 * arguments are expressions */
    
#define M3G_MIN(a, b) ((a) <= (b) ? (a) : (b))
#define M3G_MAX(a, b) ((a) >= (b) ? (a) : (b))

static M3G_INLINE M3Gint m3gMaxInt(M3Gint a, M3Gint b)
{
    return M3G_MAX(a, b);
}

static M3G_INLINE M3Gint m3gMinInt(M3Gint a, M3Gint b)
{
    return M3G_MIN(a, b);
}

/*!
 * \internal
 * \brief RGB mask
 */
#define M3G_RGB_MASK    0x00FFFFFFu

/*!
 * \internal
 * \brief Alpha mask
 */
#define M3G_ALPHA_MASK  0xFF000000u

/*----------------------------------------------------------------------
 * Data alignment macros
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Align a pointer or size value to an N-byte boundary
 *
 * @param value Value to align.
 * @param size  Alignment size; must be a power of two
 */
#define M3G_ALIGN_TO(value, size) ((((unsigned) (value)) + ((size) - 1)) & ~((size) - 1))
M3G_SPLINT(extern int M3G_ALIGN_TO(int /*@alt size_t@*/ value, /*@sef@*/ int size);)

/*!
 * \internal
 * \brief Returns a truth value for whether a pointer is aligned or not
 */
#define M3G_IS_ALIGNED(ptr) ((((unsigned)(ptr)) & (M3G_ALIGNMENT-1)) == 0)

/*!
 * \internal
 * \brief Assert proper alignment of a pointer
 *
 * Currently, we assume alignment to 4-byte boundaries.
 */
#define M3G_ASSERT_ALIGNMENT(ptr) M3G_ASSERT(M3G_IS_ALIGNED(ptr))

/*!
 * \internal
 * \brief Asserts that a pointer is both non-null and aligned
 */
#define M3G_ASSERT_PTR(ptr) M3G_ASSERT((ptr) != NULL && M3G_IS_ALIGNED(ptr))
#define M3G_VALIDATE_PTR(ptr) M3G_ASSERT_PTR(ptr)
    
/*----------------------------------------------------------------------
 * Easier names for math classes...
 *--------------------------------------------------------------------*/

typedef M3GMatrix       Matrix;
typedef M3GQuat         Quat;
typedef M3GVec3         Vec3;
typedef M3GVec4         Vec4;
typedef M3GRectangle    Rect;

/* We rely on the layout of a Vec4 or a Quat matching an array of 4
 * floats in a number of places, so check that */

M3G_CT_ASSERT(sizeof(M3GVec4) == 4 * sizeof(M3Gfloat));
M3G_CT_ASSERT(sizeof(M3GQuat) == 4 * sizeof(M3Gfloat));

#if defined(__cplusplus)
} /* extern "C" */
#endif

#endif /*__M3G_DEFS_H__*/
