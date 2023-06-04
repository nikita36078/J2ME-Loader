/*----------------------------------------------------------------------------
 *
 * File:
 * eas_report.c
 *
 * Contents and purpose:
 * This file contains the debug message handling routines for the EAS library.
 * These routines should be modified as needed for your system.
 *
 * Copyright 2005 Sonic Network Inc.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *----------------------------------------------------------------------------
 * Revision Control:
 *   $Revision: 659 $
 *   $Date: 2007-04-24 13:36:35 -0700 (Tue, 24 Apr 2007) $
 *----------------------------------------------------------------------------
*/

#ifdef _lint
#include "lint_stdlib.h"
#else
#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#endif

#include "eas_report.h"

static int severityLevel = 9999;

/* debug file */
static FILE *debugFile = NULL;
int flush = 0;

#ifndef _NO_DEBUG_PREPROCESSOR

/* structure should have an #include for each error message header file */
S_DEBUG_MESSAGES debugMessages[] =
{
#ifndef UNIFIED_DEBUG_MESSAGES
#include "eas_config_msgs.h"


#include "eas_host_msgs.h"
#include "eas_hostmm_msgs.h"
#include "eas_math_msgs.h"
#include "eas_midi_msgs.h"
#include "eas_mixer_msgs.h"
#include "eas_pcm_msgs.h"
#include "eas_public_msgs.h"
#include "eas_smf_msgs.h"
#include "eas_wave_msgs.h"
#include "eas_voicemgt_msgs.h"

#ifdef _FM_SYNTH
#include "eas_fmsynth_msgs.h"
#include "eas_fmengine_msgs.h"
#endif

#ifdef _WT_SYNTH
#include "eas_wtsynth_msgs.h"
#include "eas_wtengine_msgs.h"
#endif

#ifdef _ARM_TEST_MAIN
#include "arm_main_msgs.h"
#endif

#ifdef _EAS_MAIN
#include "eas_main_msgs.h"
#endif

#ifdef _EAS_MAIN_IPC
#include "eas_main_ipc_msgs.h"
#endif

#ifdef _METRICS_ENABLED
#include "eas_perf_msgs.h"
#endif

#ifdef _COMPRESSOR_ENABLED
#include "eas_compressor_msgs.h"
#endif

#ifdef _ENHANCER_ENABLED
#include "eas_enhancer_msgs.h"
#endif

#ifdef _WOW_ENABLED
#include "eas_wow_msgs.h"
#endif

#ifdef _SMAF_PARSER
#include "eas_smaf_msgs.h"
#endif

#ifdef _OTA_PARSER
#include "eas_ota_msgs.h"
#endif

#ifdef _IMELODY_PARSER
#include "eas_imelody_msgs.h"
#endif

#ifdef _WAVE_PARSER
#include "eas_wavefile_msgs.h"
#endif

#if defined(_CMX_PARSER) || defined(_MFI_PARSER)
#include "eas_cmf_msgs.h"
#endif

#if defined(_CMX_PARSER) || defined(_MFI_PARSER) || defined(_WAVE_PARSER)
#include "eas_imaadpcm_msgs.h"
#endif

#else
#include "eas_debugmsgs.h"
#endif

/* denotes end of error messages */
{ 0,0,0 }
};

/*----------------------------------------------------------------------------
 * EAS_ReportEx()
 *
 * This is the error message handler. The default handler outputs error
 * messages to stdout. Modify this as needed for your system.
 *----------------------------------------------------------------------------
*/
void EAS_ReportEx (int severity, unsigned long hashCode, int serialNum, ...)
{
    va_list vargs;
    int i;

    /* check severity level */
    if (severity > severityLevel)
        return;

    /* find the error message and output to stdout */
    /*lint -e{661} we check for NULL pointer - no fence post error here */
    for (i = 0; debugMessages[i].m_pDebugMsg; i++)
    {
        if ((debugMessages[i].m_nHashCode == hashCode) &&
        (debugMessages[i].m_nSerialNum == serialNum))
        {
            /*lint -e{826} <allow variable args> */
            va_start(vargs, serialNum);
            if (debugFile)
            {
                vfprintf(debugFile, debugMessages[i].m_pDebugMsg, vargs);
                if (flush)
                    fflush(debugFile);
            }
            else
            {
                vprintf(debugMessages[i].m_pDebugMsg, vargs);
            }
            va_end(vargs);
            return;
        }
    }
    printf("Unrecognized error: Severity=%d; HashCode=%lu; SerialNum=%d\n", severity, hashCode, serialNum);
} /* end EAS_ReportEx */

#else
/*----------------------------------------------------------------------------
 * EAS_Report()
 *
 * This is the error message handler. The default handler outputs error
 * messages to stdout. Modify this as needed for your system.
 *----------------------------------------------------------------------------
*/
void EAS_Report (int severity, const char *fmt, ...)
{
    va_list vargs;

    /* check severity level */
    if (severity > severityLevel)
        return;

    /*lint -e{826} <allow variable args> */
    va_start(vargs, fmt);
    if (debugFile)
    {
        vfprintf(debugFile, fmt, vargs);
        if (flush)
            fflush(debugFile);
    }
    else
    {
        vprintf(fmt, vargs);
    }
    va_end(vargs);
} /* end EAS_Report */

/*----------------------------------------------------------------------------
 * EAS_ReportX()
 *
 * This is the error message handler. The default handler outputs error
 * messages to stdout. Modify this as needed for your system.
 *----------------------------------------------------------------------------
*/
void EAS_ReportX (int severity, const char *fmt, ...)
{
    va_list vargs;

    /* check severity level */
    if (severity > severityLevel)
        return;

    /*lint -e{826} <allow variable args> */
    va_start(vargs, fmt);
    if (debugFile)
    {
        vfprintf(debugFile, fmt, vargs);
        if (flush)
            fflush(debugFile);
    }
    else
    {
        vprintf(fmt, vargs);
    }
    va_end(vargs);
} /* end EAS_ReportX */
#endif

/*----------------------------------------------------------------------------
 * EAS_SetDebugLevel()
 *
 * Sets the level for debug message output
 *----------------------------------------------------------------------------
*/

void EAS_SetDebugLevel (int severity)
{
    severityLevel = severity;
} /* end EAS_SetDebugLevel */

/*----------------------------------------------------------------------------
 * EAS_SetDebugFile()
 *
 * Redirect debugger output to the specified file.
 *----------------------------------------------------------------------------
*/
void EAS_SetDebugFile (void *file, int flushAfterWrite)
{
    debugFile = (FILE*) file;
    flush = flushAfterWrite;
} /* end EAS_SetDebugFile */

