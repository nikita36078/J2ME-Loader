/*----------------------------------------------------------------------------
 *
 * File:
 * eas_host.h
 *
 * Contents and purpose:
 * This header defines the host wrapper functions for stdio, stdlib, etc.
 * The host application must provide an abstraction layer for these functions
 * to support certain features, such as SMAF and SMF-1 conversion.
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

// sentinel
#ifndef _EAS_HOST_H
#define _EAS_HOST_H

#include "eas_types.h"

/* for C++ linkage */
#ifdef __cplusplus
extern "C" {
#endif

/* initialization and shutdown routines */
extern EAS_RESULT EAS_HWInit(EAS_HW_DATA_HANDLE *hwInstData);
extern EAS_RESULT EAS_HWShutdown(EAS_HW_DATA_HANDLE hwInstData);

/* threading */
extern void* EAS_HWRegisterSignalHandler();
extern EAS_RESULT EAS_HWUnRegisterSignalHandler(void *cookie);

/* memory functions */
extern void *EAS_HWMemSet(void *s, int c, EAS_I32 n);
extern void *EAS_HWMemCpy(void *s1, const void *s2, EAS_I32 n);
extern EAS_I32 EAS_HWMemCmp(const void *s1, const void *s2, EAS_I32 n);

/* memory allocation */
extern void *EAS_HWMalloc(EAS_HW_DATA_HANDLE hwInstData, EAS_I32 size);
extern void EAS_HWFree(EAS_HW_DATA_HANDLE hwInstData, void *p);

/* file I/O */
extern EAS_RESULT EAS_HWOpenFile(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_LOCATOR locator, EAS_FILE_HANDLE *pFile, EAS_FILE_MODE mode);
extern EAS_RESULT EAS_HWReadFile(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, void *pBuffer, EAS_I32 n, EAS_I32 *pBytesRead);
extern EAS_RESULT EAS_HWGetByte(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, void *p);
extern EAS_RESULT EAS_HWGetWord (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, void *p, EAS_BOOL msbFirst);
extern EAS_RESULT EAS_HWGetDWord (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, void *p, EAS_BOOL msbFirst);
extern EAS_RESULT EAS_HWFilePos (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, EAS_I32 *pPosition);
extern EAS_RESULT EAS_HWFileSeek (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, EAS_I32 position);
extern EAS_RESULT EAS_HWFileSeekOfs (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, EAS_I32 position);
extern EAS_RESULT EAS_HWFileLength (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, EAS_I32 *pLength);
extern EAS_RESULT EAS_HWDupHandle (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, EAS_FILE_HANDLE* pFile);
extern EAS_RESULT EAS_HWCloseFile (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file);

/* vibrate, LED, and backlight functions */
extern EAS_RESULT EAS_HWVibrate(EAS_HW_DATA_HANDLE hwInstData, EAS_BOOL state);
extern EAS_RESULT EAS_HWLED(EAS_HW_DATA_HANDLE hwInstData, EAS_BOOL state);
extern EAS_RESULT EAS_HWBackLight(EAS_HW_DATA_HANDLE hwInstData, EAS_BOOL state);

#ifdef __cplusplus
} /* end extern "C" */
#endif


/* host yield function */
extern EAS_BOOL EAS_HWYield(EAS_HW_DATA_HANDLE hwInstData);
#endif /* end _EAS_HOST_H */
