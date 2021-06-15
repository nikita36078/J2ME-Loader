/*----------------------------------------------------------------------------
 *
 * File:
 * eas_hostmm.c
 *
 * Contents and purpose:
 * This file contains the host wrapper functions for stdio, stdlib, etc.
 * This is a sample version that reads from a filedescriptor.
 * The file locator (EAS_FILE_LOCATOR) handle passed to
 * HWOpenFile is the same one that is passed to EAS_OpenFile.
 *
 * Modify this file to suit the needs of your particular system.
 *
 * EAS_MAX_FILE_HANDLES sets the maximum number of MIDI streams within
 * a MIDI type 1 file that can be played.
 *
 * EAS_HW_FILE is a structure to support the file I/O functions. It
 * comprises the file descriptor, the file read pointer, and
 * the dup flag, which when set, indicates that the file handle has
 * been duplicated, and offset and length within the file.
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
 *   $Revision: 795 $
 *   $Date: 2007-08-01 00:14:45 -0700 (Wed, 01 Aug 2007) $
 *----------------------------------------------------------------------------
*/

#ifdef _lint
#include "lint_stdlib.h"
#else
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <limits.h>
#include <sys/mman.h>
#include <errno.h>
#include <signal.h>
#include <pthread.h>
#include <media/MediaPlayerInterface.h>
#endif

#include "eas_host.h"

/* Only for debugging LED, vibrate, and backlight functions */
#include "eas_report.h"

/* this module requires dynamic memory support */
#ifdef _STATIC_MEMORY
#error "eas_hostmm.c requires the dynamic memory model!\n"
#endif

#ifndef EAS_MAX_FILE_HANDLES
// 100 max file handles == 3 * (nb tracks(32) + 1 for the segment) + 1 for jet file
//                         3 == 1(playing segment) + 1(prepared segment)
//                              + 1(after end of playing segment, before files closed)
#define EAS_MAX_FILE_HANDLES    100
#endif

/*
 * this structure and the related function are here
 * to support the ability to create duplicate handles
 * and buffering it in memory. If your system uses
 * in-memory resources, you can eliminate the calls
 * to malloc and free, the dup flag, and simply track
 * the file size and read position.
 */
typedef struct eas_hw_file_tag
{
    int (*readAt)(void *handle, void *buf, int offset, int size);
    int (*size)(void *handle);
    int filePos;
    void *handle;
} EAS_HW_FILE;

typedef struct eas_hw_inst_data_tag
{
    EAS_HW_FILE files[EAS_MAX_FILE_HANDLES];
} EAS_HW_INST_DATA;

pthread_key_t EAS_sigbuskey;

/*----------------------------------------------------------------------------
 * EAS_HWInit
 *
 * Initialize host wrapper interface
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWInit (EAS_HW_DATA_HANDLE *pHWInstData)
{
    EAS_HW_FILE *file;
    int i;

    /* need to track file opens for duplicate handles */
    *pHWInstData = malloc(sizeof(EAS_HW_INST_DATA));
    if (!(*pHWInstData))
        return EAS_ERROR_MALLOC_FAILED;

    EAS_HWMemSet(*pHWInstData, 0, sizeof(EAS_HW_INST_DATA));

    file = (*pHWInstData)->files;
    for (i = 0; i < EAS_MAX_FILE_HANDLES; i++)
    {
        file->handle = NULL;
        file++;
    }


    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_HWShutdown
 *
 * Shut down host wrapper interface
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWShutdown (EAS_HW_DATA_HANDLE hwInstData)
{

    free(hwInstData);
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWMalloc
 *
 * Allocates dynamic memory
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
void *EAS_HWMalloc (EAS_HW_DATA_HANDLE hwInstData, EAS_I32 size)
{
    /* Since this whole library loves signed sizes, let's not let
     * negative or 0 values through */
    if (size <= 0)
      return NULL;
    return malloc((size_t) size);
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFree
 *
 * Frees dynamic memory
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
void EAS_HWFree (EAS_HW_DATA_HANDLE hwInstData, void *p)
{
    free(p);
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWMemCpy
 *
 * Copy memory wrapper
 *
 *----------------------------------------------------------------------------
*/
void *EAS_HWMemCpy (void *dest, const void *src, EAS_I32 amount)
{
    if (amount < 0)  {
      EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x1a54b6e8, 0x00000004 , amount);
      exit(255);
    }
    return memcpy(dest, src, (size_t) amount);
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWMemSet
 *
 * Set memory wrapper
 *
 *----------------------------------------------------------------------------
*/
void *EAS_HWMemSet (void *dest, int val, EAS_I32 amount)
{
    if (amount < 0)  {
      EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x1a54b6e8, 0x00000005 , amount);
      exit(255);
    }
    return memset(dest, val, (size_t) amount);
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWMemCmp
 *
 * Compare memory wrapper
 *
 *----------------------------------------------------------------------------
*/
EAS_I32 EAS_HWMemCmp (const void *s1, const void *s2, EAS_I32 amount)
{
    if (amount < 0) {
      EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x1a54b6e8, 0x00000006 , amount);
      exit(255);
    }
    return (EAS_I32) memcmp(s1, s2, (size_t) amount);
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWOpenFile
 *
 * Open a file for read or write
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWOpenFile (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_LOCATOR locator, EAS_FILE_HANDLE *pFile, EAS_FILE_MODE mode)
{
    EAS_HW_FILE *file;
    int fd;
    int i, temp;

    /* set return value to NULL */
    *pFile = NULL;

    /* only support read mode at this time */
    if (mode != EAS_FILE_READ)
        return EAS_ERROR_INVALID_FILE_MODE;

    /* find an empty entry in the file table */
    file = hwInstData->files;
    for (i = 0; i < EAS_MAX_FILE_HANDLES; i++)
    {
        /* is this slot being used? */
        if (file->handle == NULL)
        {
            file->handle = locator->handle;
            file->readAt = locator->readAt;
            file->size = locator->size;
            file->filePos = 0;
            *pFile = file;
            return EAS_SUCCESS;
        }
        file++;
    }

    /* too many open files */
    return EAS_ERROR_MAX_FILES_OPEN;
}


/*----------------------------------------------------------------------------
 *
 * EAS_HWReadFile
 *
 * Read data from a file
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWReadFile (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, void *pBuffer, EAS_I32 n, EAS_I32 *pBytesRead)
{
    EAS_I32 count;

    /* make sure we have a valid handle */
    if (file->handle == NULL)
        return EAS_ERROR_INVALID_HANDLE;

    if (n < 0)
      return EAS_EOF;

    /* calculate the bytes to read */
    count = file->size(file->handle) - file->filePos;
    if (n < count)
        count = n;
    if (count < 0)
      return EAS_EOF;

    /* copy the data to the requested location, and advance the pointer */
    if (count) {
        count = file->readAt(file->handle, pBuffer, file->filePos, count);
    }
    file->filePos += count;
    *pBytesRead = count;

    /* were n bytes read? */
    if (count!= n)
        return EAS_EOF;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWGetByte
 *
 * Read a byte from a file
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWGetByte (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, void *p)
{
    EAS_I32 numread;
    return EAS_HWReadFile(hwInstData, file, p, 1, &numread);
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWGetWord
 *
 * Read a 16 bit word from a file
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWGetWord (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, void *p, EAS_BOOL msbFirst)
{
    EAS_RESULT result;
    EAS_U8 c1, c2;

    /* read 2 bytes from the file */
    if ((result = EAS_HWGetByte(hwInstData, file, &c1)) != EAS_SUCCESS)
        return result;
    if ((result = EAS_HWGetByte(hwInstData, file, &c2)) != EAS_SUCCESS)
        return result;

    /* order them as requested */
    if (msbFirst)
        *((EAS_U16*) p) = ((EAS_U16) c1 << 8) | c2;
    else
        *((EAS_U16*) p) = ((EAS_U16) c2 << 8) | c1;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWGetDWord
 *
 * Returns the current location in the file
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWGetDWord (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, void *p, EAS_BOOL msbFirst)
{
    EAS_RESULT result;
    EAS_U8 c1, c2,c3,c4;

    /* read 4 bytes from the file */
    if ((result = EAS_HWGetByte(hwInstData, file, &c1)) != EAS_SUCCESS)
        return result;
    if ((result = EAS_HWGetByte(hwInstData, file, &c2)) != EAS_SUCCESS)
        return result;
    if ((result = EAS_HWGetByte(hwInstData, file, &c3)) != EAS_SUCCESS)
        return result;
    if ((result = EAS_HWGetByte(hwInstData, file, &c4)) != EAS_SUCCESS)
        return result;

    /* order them as requested */
    if (msbFirst)
        *((EAS_U32*) p) = ((EAS_U32) c1 << 24) | ((EAS_U32) c2 << 16) | ((EAS_U32) c3 << 8) | c4;
    else
        *((EAS_U32*) p)= ((EAS_U32) c4 << 24) | ((EAS_U32) c3 << 16) | ((EAS_U32) c2 << 8) | c1;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFilePos
 *
 * Returns the current location in the file
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWFilePos (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, EAS_I32 *pPosition)
{

    /* make sure we have a valid handle */
    if (file->handle == NULL)
        return EAS_ERROR_INVALID_HANDLE;

    *pPosition = file->filePos;
    return EAS_SUCCESS;
} /* end EAS_HWFilePos */

/*----------------------------------------------------------------------------
 *
 * EAS_HWFileSeek
 *
 * Seek to a specific location in the file
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWFileSeek (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, EAS_I32 position)
{

    /* make sure we have a valid handle */
    if (file->handle == NULL)
        return EAS_ERROR_INVALID_HANDLE;

    /* validate new position */
    if ((position < 0) || (position > file->size(file->handle)))
        return EAS_ERROR_FILE_SEEK;

    /* save new position */
    file->filePos = position;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFileSeekOfs
 *
 * Seek forward or back relative to the current position
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWFileSeekOfs (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, EAS_I32 position)
{

    /* make sure we have a valid handle */
    if (file->handle == NULL)
        return EAS_ERROR_INVALID_HANDLE;

    /* determine the file position */
    position += file->filePos;
    if ((position < 0) || (position > file->size(file->handle)))
        return EAS_ERROR_FILE_SEEK;

    /* save new position */
    file->filePos = position;
    return EAS_SUCCESS;
}


/*----------------------------------------------------------------------------
 *
 * EAS_HWDupHandle
 *
 * Duplicate a file handle
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWDupHandle (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, EAS_FILE_HANDLE *pDupFile)
{
    EAS_HW_FILE *dupFile;
    int i;

    /* make sure we have a valid handle */
    if (file->handle == NULL)
        return EAS_ERROR_INVALID_HANDLE;

    /* find an empty entry in the file table */
    dupFile = hwInstData->files;
    for (i = 0; i < EAS_MAX_FILE_HANDLES; i++)
    {
        /* is this slot being used? */
        if (dupFile->handle == NULL)
        {
            /* copy info from the handle to be duplicated */
            dupFile->handle = file->handle;
            dupFile->filePos = file->filePos;
            dupFile->readAt = file->readAt;
            dupFile->size = file->size;

            *pDupFile = dupFile;
            return EAS_SUCCESS;
        }
        dupFile++;
    }

    /* too many open files */
    return EAS_ERROR_MAX_FILES_OPEN;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWClose
 *
 * Wrapper for fclose function
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWCloseFile (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file1)
{
    EAS_HW_FILE *file2,*dupFile;
    int i;


    /* make sure we have a valid handle */
    if (file1->handle == NULL)
        return EAS_ERROR_INVALID_HANDLE;

    file1->handle = NULL;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWVibrate
 *
 * Turn on/off vibrate function
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWVibrate (EAS_HW_DATA_HANDLE hwInstData, EAS_BOOL state)
{
    EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x1a54b6e8, 0x00000001 , state);
    return EAS_SUCCESS;
} /* end EAS_HWVibrate */

/*----------------------------------------------------------------------------
 *
 * EAS_HWLED
 *
 * Turn on/off LED
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWLED (EAS_HW_DATA_HANDLE hwInstData, EAS_BOOL state)
{
    EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x1a54b6e8, 0x00000002 , state);
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWBackLight
 *
 * Turn on/off backlight
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWBackLight (EAS_HW_DATA_HANDLE hwInstData, EAS_BOOL state)
{
    EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x1a54b6e8, 0x00000003 , state);
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWYield
 *
 * This function is called periodically by the EAS library to give the
 * host an opportunity to allow other tasks to run. There are two ways to
 * use this call:
 *
 * If you have a multi-tasking OS, you can call the yield function in the
 * OS to allow other tasks to run. In this case, return EAS_FALSE to tell
 * the EAS library to continue processing when control returns from this
 * function.
 *
 * If tasks run in a single thread by sequential function calls (sometimes
 * call a "commutator loop"), return EAS_TRUE to cause the EAS Library to
 * return to the caller. Be sure to check the number of bytes rendered
 * before passing the audio buffer to the codec - it may not be filled.
 * The next call to EAS_Render will continue processing until the buffer
 * has been filled.
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_BOOL EAS_HWYield (EAS_HW_DATA_HANDLE hwInstData)
{
    /* put your code here */
    return EAS_FALSE;
}

