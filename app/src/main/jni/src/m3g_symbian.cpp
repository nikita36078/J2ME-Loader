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
#include <e32base.h>
#include <f32file.h>
#include <ezlib.h>

#if defined(M3G_SYMBIAN_REMOTE_LOGGING)
#include <EcmtClient.h>
#endif

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
#   if !defined(M3G_SYMBIAN_REMOTE_LOGGING)
    _LIT(KFileName, "c:\\m3g_event.log");

    RFs fileSrv;
    RFile file;

    if (fileSrv.Connect() != KErrNone) {
        return;
    }

    TInt accessMode = EFileWrite|EFileShareExclusive;
    TInt err = file.Open(fileSrv, KFileName, accessMode);

    // If the file exists, seek to the end; if it doesn't, create it
    
    if (err == KErrNone) {
        TInt pos = 0;
        file.Seek(ESeekEnd, pos);
    }
    else if (err == KErrNotFound) {
        if (file.Create(fileSrv, KFileName, accessMode) != KErrNone) {
            fileSrv.Close();
            return;
        }
    }
#   endif // !M3G_SYMBIAN_REMOTE_LOGGING

    // Format and write the message

    TBuf8<1024> msg;
    TPtrC8 aFormat((const TUint8 *) format);
    
    VA_LIST aList;
    VA_START(aList, format);

    msg.FormatList(aFormat, aList);

    VA_END(aList);

#   if defined(M3G_SYMBIAN_REMOTE_LOGGING)

    M3GLogger *logger = (M3GLogger*) Dll::Tls();

    if (logger) {
        TBuf16<1024> msg16;
        msg16.Copy(msg);
        logger->iEcmt.Write(msg16);
    }
    
#   else
    file.Write(msg);

    // Close the file and the server connection
    
    file.Flush();
    file.Close();
    fileSrv.Close();
    
#   endif // !M3G_SYMBIAN_REMOTE_LOGGING
}

/*!
 * \brief Create new log file
 */
extern "C" void m3gBeginLog(void)
{
#   if defined(M3G_SYMBIAN_REMOTE_LOGGING)

    M3GLogger *logger = (M3GLogger*) Dll::Tls();
    
    if (!logger) {
        logger = new M3GLogger();
        logger->refCount = 0;
        logger->lastOutputTickCount = User::TickCount();
        Dll::SetTls(logger);
    }

    if (++logger->refCount == 1) {
        logger->iEcmt.Connect();
    }

#   else
    
    /* Just delete any existing log file */
    
    _LIT(KFileName, "c:\\m3g_event.log");

    RFs fileSrv;
    RFile file;

    if (fileSrv.Connect() != KErrNone) {
        return;
    }

    TInt accessMode = EFileWrite|EFileShareExclusive;
    if (file.Replace(fileSrv, KFileName, accessMode) == KErrNone) {
        file.Flush();
        file.Close();
    }
    fileSrv.Close();
    
#   endif // !M3G_SYMBIAN_REMOTE_LOGGING
    
    /* Output initial message(s) */
    
    m3gLogMessage("--- M3G event log ---\n");
}

/*!
 * \brief End logging
 */
extern "C" void m3gEndLog(void)
{
#   if defined(M3G_SYMBIAN_REMOTE_LOGGING)

    M3GLogger *logger = (M3GLogger*) Dll::Tls();
    if (logger) {
        if (--logger->refCount == 0) {
            logger->iEcmt.Close();
            Dll::FreeTls();
            delete logger;
        }
    }

#   endif
    
    m3gLogMessage("--- end of log ---\n");
}

/*!
 * \brief Assertion handler
 */
extern "C" void m3gAssertFailed(const char *filename, int line)
{
    M3G_LOG2(M3G_LOG_ALL, "Assertion failed: %s, line %d\n", filename, line);
    User::Panic(_L("M3G-ASSERT"), line);
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
    M3GLogger *logger = (M3GLogger*) Dll::Tls();
    if (logger) {
        logger->tickCount[stat] = User::TickCount();
    }
}

extern "C" int m3gEndProfile(int stat)
{
    M3GLogger *logger = (M3GLogger*) Dll::Tls();
    if (logger) {
        return User::TickCount() - logger->tickCount[stat];
    }
    return 0;
}

extern "C" M3Gint m3gProfileTriggered(void)
{
#   if (M3G_PROFILE_LOG_INTERVAL > 0)
    M3GLogger *logger = (M3GLogger*) Dll::Tls();
    if (logger) {
        M3Guint tickCount = User::TickCount();
        M3Guint delta = tickCount - logger->lastOutputTickCount;
        if (delta >= M3G_PROFILE_LOG_INTERVAL) {
            logger->lastOutputTickCount = tickCount;
            return (M3Gint) delta;
        }
    }
#   endif
    return 0;
}

#endif

