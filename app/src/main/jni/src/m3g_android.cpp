/*
* Copyright (c) 2009 Nokia Corporation and/or its subsidiary(-ies).
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
* Description: Symbian debugging and profiling functions
*
*/

#include "m3g_defs.h"

#include <android/log.h>
#include <zlib.h>

struct M3GLogger
{
    M3Gint refCount;
    
#   if defined(M3G_SYMBIAN_REMOTE_LOGGING)
    REcmt iEcmt;
#   endif

#   if defined(M3G_ENABLE_PROFILING)
    M3Guint tickCount[M3G_STAT_MAX];
    M3Guint lastOutputTickCount;
#   endif
};

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Symbian implementation of the block inflation function for
 * the Loader class
 */
extern "C" M3Gsizei m3gSymbianInflateBlock(M3Gsizei srcLength,
                                           const M3Gubyte *src,
                                           M3Gsizei dstLength,
                                           M3Gubyte *dst)
{
    M3G_ASSERT(src);
    M3G_ASSERT(dst);
    M3G_ASSERT(srcLength > 0);

    {
        uLongf len = (uLongf) dstLength;
        if (uncompress((Bytef *) dst, &len,
                       (const Bytef *) src, (uLong) srcLength) != Z_OK) {
            return 0;
        }
        return (M3Gsizei) len;
    }
}

/*!
 * \brief Logging function
 *
 * \note This currently creates a session to the file server for each
 * message, so performance will be sub-optimal if logging is frequent
 */
extern "C" void m3gLogMessage(const char *format, ...)
{    
	va_list args;

	va_start(args, format);
	__android_log_vprint(ANDROID_LOG_WARN, "M3G", format, args);
	va_end(args);
}

/*!
 * \brief Create new log file
 */
extern "C" void m3gBeginLog(void)
{
    /* Output initial message(s) */
    
    m3gLogMessage("--- M3G event log ---\n");
}

/*!
 * \brief End logging
 */
extern "C" void m3gEndLog(void)
{
    m3gLogMessage("--- end of log ---\n");
}

/*!
 * \brief Assertion handler
 */
extern "C" void m3gAssertFailed(const char *filename, int line)
{
    M3G_LOG2(M3G_LOG_ALL, "Assertion failed: %s, line %d\n", filename, line);
    exit(0);
}

#if defined(M3G_BUILD_DLL)
/*!
 * \brief DLL load check
 */
#ifndef EKA2
GLDEF_C TInt E32Dll( TDllReason /* aReason */ )
{
    return KErrNone;
}
#endif
#endif /* M3G_BUILD_DLL */

/*----------------------------------------------------------------------
 * Profiling
 *--------------------------------------------------------------------*/

#if defined(M3G_ENABLE_PROFILING)

extern "C" void m3gCleanupProfile(void)
{
    ;
}

extern "C" void m3gBeginProfile(int stat)
{
}

extern "C" int m3gEndProfile(int stat)
{
    return 0;
}

extern "C" M3Gint m3gProfileTriggered(void)
{
    return 0;
}

#endif

