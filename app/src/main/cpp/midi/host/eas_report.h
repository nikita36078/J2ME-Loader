/*----------------------------------------------------------------------------
 *
 * File:
 * eas_report.h
 *
 * Contents and purpose:
 * This file contains the debug message handling routines for the EAS library.
 * These routines should be modified as needed for your system.
 *
 * DO NOT MODIFY THIS FILE!
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
 *   $Revision: 82 $
 *   $Date: 2006-07-10 11:45:19 -0700 (Mon, 10 Jul 2006) $
 *----------------------------------------------------------------------------
*/

/* sentinel */
#ifndef _EAS_REPORT_H
#define _EAS_REPORT_H

#define _EAS_SEVERITY_NOFILTER 0
#define _EAS_SEVERITY_FATAL 1
#define _EAS_SEVERITY_ERROR 2
#define _EAS_SEVERITY_WARNING 3
#define _EAS_SEVERITY_INFO 4
#define _EAS_SEVERITY_DETAIL 5

/* for C++ linkage */
#ifdef __cplusplus
extern "C" {
#endif

#ifndef _NO_DEBUG_PREPROCESSOR

/* structure for included debug message header files */
typedef struct
{
    unsigned long m_nHashCode;
    int m_nSerialNum;
    char *m_pDebugMsg;
} S_DEBUG_MESSAGES;

/* debug message handling prototypes */
extern void EAS_ReportEx (int severity, unsigned long hashCode, int serialNum, ...);

#else

/* these prototypes are used if the debug preprocessor is not used */
extern void EAS_Report (int severity, const char* fmt, ...);
extern void EAS_ReportX (int severity, const char* fmt, ...);

#endif

extern void EAS_SetDebugLevel (int severity);
extern void EAS_SetDebugFile (void *file, int flushAfterWrite);

#ifdef __cplusplus
} /* end extern "C" */
#endif

#endif
