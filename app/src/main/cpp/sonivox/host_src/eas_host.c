/*----------------------------------------------------------------------------
 *
 * File:
 * eas_host.c
 *
 * Contents and purpose:
 * This file contains the host wrapper functions for stdio, stdlib, etc.
 * This is a sample version that wraps the standard library functions.
 * Modify this file to suit the needs of your particular system.
 *
 * EAS_MAX_FILE_HANDLES sets the maximum number of MIDI streams within
 * a MIDI type 1 file that can be played. To maintain efficiency, data
 * is buffered locally when byte access is used (EAS_HWGetByte). The
 * size of the buffer is set by EAS_FILE_BUFFER_SIZE.
 *
 * EAS_HW_FILE is a structure to support local file buffering. It
 * comprises the OS File handle, some data related to the local file
 * buffer, the position of the next byte of data to be read, the dup
 * flag which when set, indicates that the handle has been duplicated,
 * and the data buffer. Since the data buffer is only used for byte
 * access, it does not need to be large.
 *
 * If the file system supports duplicate file handles and buffering,
 * this entire subsystem can be replaced with direct calls to the
 * native file I/O routines.
 *
 * If the system has enough memory to support reading the entire file
 * into memory, it will be much more efficient to do so on the call to
 * EAS_HWOpenFile and then close the file. Simply substitute a memory
 * pointer for the FILE* pointer. Calls to EAS_HW_DupHandle will work
 * as they do in this version. In the call to EAS_HWCloseFile, instead
 * of calling fclose, free the memory containing the file data.
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
 *   $Revision: 853 $
 *   $Date: 2007-09-05 09:54:17 -0700 (Wed, 05 Sep 2007) $
 *----------------------------------------------------------------------------
*/

#ifdef _lint
#include "lint_stdlib.h"
#else
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#endif

#include "eas_host.h"

// #define DEBUG_FILE_IO

/* Only for debugging LED, vibrate, and backlight functions */
#include "eas_report.h"

#ifndef EAS_MAX_FILE_HANDLES
#define EAS_MAX_FILE_HANDLES    32
#endif

#ifndef EAS_FILE_BUFFER_SIZE
#define EAS_FILE_BUFFER_SIZE    32
#endif

/*
 * this structure and the related function are here
 * to support the ability to create duplicate handles
 * and buffering into a single file. If the OS supports
 * duplicate file handles natively, this code can be
 * stripped to eliminate double-buffering.
 */
typedef struct eas_hw_file_tag
{
    FILE *pFile;
    EAS_I32 bytesInBuffer;
    EAS_I32 readIndex;
    EAS_I32 filePos;
    EAS_BOOL dup;
    EAS_U8 buffer[EAS_FILE_BUFFER_SIZE];
} EAS_HW_FILE;

typedef struct eas_hw_inst_data_tag
{
    EAS_HW_FILE files[EAS_MAX_FILE_HANDLES];
} EAS_HW_INST_DATA;

/* local memory for files and streams */
#ifdef _STATIC_MEMORY
EAS_HW_INST_DATA fileData;
#endif

/*----------------------------------------------------------------------------
 * EAS_HWInit
 *
 * Initialize host wrapper interface
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWInit (EAS_HW_DATA_HANDLE *pHWInstData)
{

    /* need to track file opens for duplicate handles */
#ifndef _STATIC_MEMORY
    *pHWInstData = malloc(sizeof(EAS_HW_INST_DATA));
    if (!(*pHWInstData))
        return EAS_ERROR_MALLOC_FAILED;
#else
    *pHWInstData = &fileData;
#endif
    EAS_HWMemSet(*pHWInstData, 0, sizeof(EAS_HW_INST_DATA));
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_HWShutdown
 *
 * Shut down host wrapper interface
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWShutdown (EAS_HW_DATA_HANDLE hwInstData)
{

#ifndef _STATIC_MEMORY
    free(hwInstData);
#endif
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
#ifdef _STATIC_MEMORY
/*lint -esym(715, size) not used in static memory model */
#endif
void *EAS_HWMalloc (EAS_HW_DATA_HANDLE hwInstData, EAS_I32 size)
{
#ifdef _STATIC_MEMORY
    return NULL;
#else
    return malloc((EAS_U32)size);
#endif
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
#ifdef _STATIC_MEMORY
/*lint -esym(715, p) not used in static memory model */
#endif
void EAS_HWFree(EAS_HW_DATA_HANDLE hwInstData, void *p)
{
#ifndef _STATIC_MEMORY
    free(p);
#endif
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
    return memcpy(dest,src,(size_t) amount);
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
    return memset(dest,val,(size_t) amount);
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
    int i;

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
        if (file->pFile == NULL)
        {
            /* open the file */
            if (locator->path)
                file->pFile = fopen((const char*) locator->path, "rb");
            if (file->pFile == NULL)
                return EAS_ERROR_FILE_OPEN_FAILED;

#ifdef DEBUG_FILE_IO
            EAS_ReportX(_EAS_SEVERITY_NOFILTER, "EAS_HWOpenFile: Open file %d\n", i);
#endif

            /* initialize some values */
            file->bytesInBuffer = 0;
            file->readIndex = 0;
            file->filePos = 0;
            file->dup = EAS_FALSE;

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
 * EAS_HWFillBuffer
 *
 * Fill buffer from file
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWFillBuffer (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file)
{
    /* reposition the file pointer */
    if (fseek(file->pFile, file->filePos, SEEK_SET) != 0)
        return EAS_ERROR_FILE_SEEK;

    /* read some data from the file */
    file->bytesInBuffer = (EAS_I32) fread(file->buffer, 1, EAS_FILE_BUFFER_SIZE, file->pFile);
    file->readIndex = 0;
    if (file->bytesInBuffer == 0)
        return EAS_EOF;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWReadFile
 *
 * Read data from a file
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWReadFile (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, void *pBuffer, EAS_I32 n, EAS_I32 *pBytesRead)
{
    EAS_RESULT result;
    EAS_I32 temp;
    EAS_U8 *p = pBuffer;
    EAS_I32 bytesLeft = n;

    *pBytesRead = 0;

    /* check handle integrity */
    if (file->pFile == NULL)
        return EAS_ERROR_INVALID_HANDLE;

#ifdef DEBUG_FILE_IO
    EAS_ReportX(_EAS_SEVERITY_NOFILTER, "EAS_HWReadFile: Reading %d bytes from position %d\n", n, file->filePos);
#endif

    /* try to fulfill request from buffer */
    for (;bytesLeft > 0;)
    {
        /* how many bytes can we get from buffer? */
        temp = file->bytesInBuffer - file->readIndex;
        if (temp > bytesLeft)
            temp = bytesLeft;

        /* copy data from buffer */
        EAS_HWMemCpy(p, &file->buffer[file->readIndex], temp);
        *pBytesRead += temp;
        file->readIndex += temp;
        file->filePos += temp;
        bytesLeft -= temp;
        p += temp;

        /* don't refill buffer if request is bigger than buffer */
        if ((bytesLeft == 0) || (bytesLeft >= EAS_FILE_BUFFER_SIZE))
            break;

        /* refill buffer */
        if ((result = EAS_HWFillBuffer(hwInstData, file)) != EAS_SUCCESS)
            return result;
    }

    /* more to read? do unbuffered read directly to target memory */
    if (bytesLeft)
    {

        /* position the file pointer */
        if (fseek(file->pFile, file->filePos, SEEK_SET) != 0)
            return EAS_ERROR_FILE_SEEK;

        /* read data in the buffer */
        /*lint -e{826} lint doesn't like this with STATIC_MEMORY defined for some reason */
        temp = (EAS_I32) fread(p, 1, (size_t) bytesLeft, file->pFile);
        *pBytesRead += temp;
        file->filePos += temp;

        /* reset buffer info */
        file->bytesInBuffer = 0;
        file->readIndex = 0;
    }

#ifdef DEBUG_FILE_IO
    {
#define BYTES_PER_LINE 16
        char str[BYTES_PER_LINE * 3 + 1];
        EAS_INT i;
        for (i = 0; i < (n > BYTES_PER_LINE ? BYTES_PER_LINE : n) ; i ++)
            sprintf(&str[i*3], "%02x ", ((EAS_U8*)pBuffer)[i]);
        if (i)
            EAS_ReportX(_EAS_SEVERITY_NOFILTER, "%s\n", str);
    }
#endif

    /* were n bytes read? */
    if (*pBytesRead != n)
        return EAS_EOF;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWGetByte
 *
 * Read a byte from a file
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWGetByte (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, void *p)
{
    EAS_RESULT result;

    /* check handle integrity */
    if (file->pFile == NULL)
        return EAS_ERROR_INVALID_HANDLE;

    /* use local buffer - do we have any data? */
    if (file->readIndex >= file->bytesInBuffer)
    {
        if ((result = EAS_HWFillBuffer(hwInstData, file)) != EAS_SUCCESS)
            return result;

        /* if nothing to read, return EOF */
        if (file->bytesInBuffer == 0)
            return EAS_EOF;
    }

    /* get a character from the buffer */
    *((EAS_U8*) p) = file->buffer[file->readIndex++];

#ifdef DEBUG_FILE_IO
    EAS_ReportX(_EAS_SEVERITY_NOFILTER, "EAS_HWGetByte: Reading from position %d, byte = 0x%02x\n", file->filePos, *(EAS_U8*)p);
#endif

    file->filePos++;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWGetWord
 *
 * Read a 16-bit value from the file
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWGetWord (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, void *p, EAS_BOOL msbFirst)
{
    EAS_RESULT result;
    EAS_I32 count;
    EAS_U8 c[2];

#ifdef DEBUG_FILE_IO
    EAS_ReportX(_EAS_SEVERITY_NOFILTER, "EAS_HWGetWord: Reading 2 bytes from position %d\n", file->filePos);
#endif

    /* read 2 bytes from the file */
    if ((result = EAS_HWReadFile(hwInstData, file, c, 2, &count)) != EAS_SUCCESS)
        return result;

    /* order them as requested */
    if (msbFirst)
        *((EAS_U16*) p) = ((EAS_U16) c[0] << 8) | c[1];
    else
        *((EAS_U16*) p) = ((EAS_U16) c[1] << 8) | c[0];

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWGetDWord
 *
 * Read a 16-bit value from the file
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWGetDWord (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, void *p, EAS_BOOL msbFirst)
{
    EAS_RESULT result;
    EAS_I32 count;
    EAS_U8 c[4];

#ifdef DEBUG_FILE_IO
    EAS_ReportX(_EAS_SEVERITY_NOFILTER, "EAS_HWGetDWord: Reading 4 bytes from position %d\n", file->filePos);
#endif

    /* read 4 bytes from the file */
    if ((result = EAS_HWReadFile(hwInstData, file, c, 4, &count)) != EAS_SUCCESS)
        return result;

    /* order them as requested */
    if (msbFirst)
        *((EAS_U32*) p) = ((EAS_U32) c[0] << 24) | ((EAS_U32) c[1] << 16) | ((EAS_U32) c[2] << 8) | c[3];
    else
        *((EAS_U32*) p) = ((EAS_U32) c[3] << 24) | ((EAS_U32) c[2] << 16) | ((EAS_U32) c[1] << 8) | c[0];

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

    /* check handle integrity */
    if (file->pFile == NULL)
        return EAS_ERROR_INVALID_HANDLE;

    *pPosition = file->filePos;
    return EAS_SUCCESS;
}

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
    EAS_I32 newIndex;

    /* check handle integrity */
    if (file->pFile == NULL)
        return EAS_ERROR_INVALID_HANDLE;

#ifdef DEBUG_FILE_IO
    EAS_ReportX(_EAS_SEVERITY_NOFILTER, "EAS_HWFileSeek: Seeking to new position %d\n", file->filePos);
#endif

    /* is new position in current buffer? */
    newIndex = position - file->filePos + file->readIndex;
    if ((newIndex >= 0) && (newIndex < file->bytesInBuffer))
    {
        file->readIndex = newIndex;
        file->filePos = position;
        return EAS_SUCCESS;
    }

    /* save new position and reset buffer info so EAS_HWGetByte doesn't fail */
    file->filePos = position;
    file->bytesInBuffer = 0;
    file->readIndex = 0;
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
    EAS_I32 temp;

#ifdef DEBUG_FILE_IO
    EAS_ReportX(_EAS_SEVERITY_NOFILTER, "EAS_HWFileSeekOfs: Seeking to new position %d\n", file->filePos + position);
#endif

    /* check handle integrity */
    if (file->pFile == NULL)
        return EAS_ERROR_INVALID_HANDLE;

    /* is new position in current buffer? */
    temp = position + file->readIndex;
    if ((temp >= 0) && (temp < file->bytesInBuffer))
    {
        file->readIndex = temp;
        file->filePos += position;
        return EAS_SUCCESS;
    }

    /* save new position and reset buffer info so EAS_HWGetByte doesn't fail */
    file->filePos += position;
    file->bytesInBuffer = 0;
    file->readIndex = 0;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFileLength
 *
 * Return the file length
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWFileLength (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, EAS_I32 *pLength)
{
    long pos;

    /* check handle integrity */
    if (file->pFile == NULL)
        return EAS_ERROR_INVALID_HANDLE;

    if ((pos = ftell(file->pFile)) == -1L)
        return EAS_ERROR_FILE_LENGTH;
    if (fseek(file->pFile, 0L, SEEK_END) != 0)
        return EAS_ERROR_FILE_LENGTH;
    if ((*pLength = ftell(file->pFile)) == -1L)
        return EAS_ERROR_FILE_LENGTH;
    if (fseek(file->pFile, pos, SEEK_SET) != 0)
        return EAS_ERROR_FILE_LENGTH;
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
EAS_RESULT EAS_HWDupHandle (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, EAS_FILE_HANDLE* pDupFile)
{
    EAS_HW_FILE *dupfile;
    int i;

    /* check handle integrity */
    *pDupFile = NULL;
    if (file->pFile == NULL)
        return EAS_ERROR_INVALID_HANDLE;

    /* find an empty entry in the file table */
    dupfile = hwInstData->files;
    for (i = 0; i < EAS_MAX_FILE_HANDLES; i++)
    {
        /* is this slot being used? */
        if (dupfile->pFile == NULL)
        {

            /* copy info from the handle to be duplicated */
            dupfile->filePos = file->filePos;
            dupfile->pFile = file->pFile;

            /* set the duplicate handle flag */
            dupfile->dup = file->dup = EAS_TRUE;

            /* initialize some values */
            dupfile->bytesInBuffer = 0;
            dupfile->readIndex = 0;

            *pDupFile = dupfile;
            return EAS_SUCCESS;
        }
        dupfile++;
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

    /* check handle integrity */
    if (file1->pFile == NULL)
        return EAS_ERROR_INVALID_HANDLE;

    /* check for duplicate handle */
    if (file1->dup)
    {
        dupFile = NULL;
        file2 = hwInstData->files;
        for (i = 0; i < EAS_MAX_FILE_HANDLES; i++)
        {
            /* check for duplicate */
            if ((file1 != file2) && (file2->pFile == file1->pFile))
            {
                /* is there more than one duplicate? */
                if (dupFile != NULL)
                {
                    /* clear this entry and return */
                    file1->pFile = NULL;
                    return EAS_SUCCESS;
                }

                /* this is the first duplicate found */
                dupFile = file2;
            }
            file2++;
        }

        /* there is only one duplicate, clear the dup flag */
        if (dupFile)
            dupFile->dup = EAS_FALSE;
        else
            /* if we get here, there's a serious problem */
            return EAS_ERROR_HANDLE_INTEGRITY;

        /* clear this entry and return */
        file1->pFile = NULL;
        return EAS_SUCCESS;
    }

    /* no duplicates - close the file */
    if (fclose(file1->pFile) != 0)
        return EAS_ERROR_CLOSE_FAILED;

    /* clear this entry and return */
    file1->pFile = NULL;
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
    EAS_ReportX(_EAS_SEVERITY_NOFILTER, "Vibrate state: %d\n", state);
    return EAS_SUCCESS;
}

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
    EAS_ReportX(_EAS_SEVERITY_NOFILTER, "LED state: %d\n", state);
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
    EAS_ReportX(_EAS_SEVERITY_NOFILTER, "Backlight state: %d\n", state);
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
