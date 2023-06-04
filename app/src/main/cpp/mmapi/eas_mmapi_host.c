/*
 * EAS Host implementation for the MMAPI implementation.
 * Based on eas_host.c and eas_hostmm.c. For simplicity,
 * only the dynamic memory model is supported.
 *
 * It allows 3 different locator modes:
 * 1) NATIVE
 *   In this mode, the locator is a URI that can be passed
 *   to the fopen() function, usually a "file://..." URI
 *   for files of the native file system.
 *   This mode provides full seeking and duplication capability.
 *   It would be possible to extend this mode with http:// or other
 *   functionality. This would possibly enable seeking for http,
 *   and reduce the caching complexity required for MEMORY and STREAM modes.
 *
 * 2) MEMORY
 *   The media data is loaded by Java (usually for the http protocol and
 *   when creating a Player from an InputStream). Then this native
 *   implementation allocates one (or more) buffers to hold the entire
 *   media file. The Java layer pushes the entire media data to native
 *   before calling EAS_OpenFile().
 *   This mode provides full seeking and duplication capability.
 *
 * 3) STREAM
 *   For media data potentially too large to be loaded at once before
 *   playback, only a small portion is held in the native buffer.
 *   The Java layer pushes the data to native as needed. This mode
 *   is also used for capturing live audio data from an audio input
 *   device. It is assumed that (different than on Windows), the capture
 *   device does not buffer intermediate data.
 *
 * This implementation provides 3 versions of each file access
 * function, one for each mode. The master file access functions
 * just dispatch to the 3 functions, depending on the mode.
 * In this mode, neither seeking nor file duplication is available.
 * This architecture with pseudo polymorphism allows almost 1:1
 * usage of the implementation in eas_host.c and eas_hostmm.c,
 * meaning that improvements in those files (or existing ports)
 * can easily be re-used for this implementation.
 * For this reason, the individual implementations of the file access
 * functions are left in its original state.
 *
 * An exception is made for EAS_HWOpenFile(), because the individual
 * implementations of EAS_HWOpenFile() like EAS_HWOpenFileNative() cannot
 * access the array of EAS_HW_FILE_MIXED structures. Therefore the master
 * function EAS_HWOpenFile() is responsible for finding an empty slot
 * in the array of EAS_HW_FILE_MIXED structures, and passes that structure
 * to the individual implementation of EAS_HWOpenFile().
 *
 * Another exception concerns closure of duplicated handles: in order
 * to find duplicated handles, EAS_HWCloseFile{Native|Memory|Stream}
 * need to iterate through the array of EAS_HW_FILE_MIXED structs.
 *
 * One may consider combining the separated functions for the 3 modes
 * in order to reduce code size and/or to optimize performance.
 * Specifically, filePos and fileSize
 * could be easily shared by using one data structure for all 3 modes.
 * Also, the "file" variables could be eliminated by using typed
 * parameters for the individual implementations. Another optimization
 * (at the cost of readable code) is to re-use the "mode" field
 * in EAS_HW_FILE_MIXED for the "inUse" field. Also, handling duplicated
 * handles could be done in one centralized function.
 *
 */

/* define this symbol to have fine grained trace of data IO calls */
/* #define SONIVOX_DEBUG_IO */

/* define this symbol to have fine grained trace of seek calls */
/* #define SONIVOX_DEBUG_SEEK */

/* always first include file */
#include "eas_mmapi_config.h"
#include "eas_mmapi_types.h"

/* for debugging purposes, use Sonic's host implementation */
#ifdef MMAPI_USE_ORIGINAL_HOST
#include "../../host_src/eas_host.c"
#else

/*lint -esym(761, size_t) <allow redundant typedef of size_t here> */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* set flag to prevent eas_host.h from defining types */
#define _NO_HOST_WRAPPER_TYPES
#include "../sonivox/host_src/eas_host.h"

#ifdef SONIVOX_DEBUG
#include "../sonyvox/host_src/eas_report.h"
#endif

/* include MMAPI specific structs */
#include "eas_mmapi.h"

#ifndef EAS_MAX_FILE_HANDLES
#define EAS_MAX_FILE_HANDLES	32
#endif

#ifndef EAS_FILE_BUFFER_SIZE
#define EAS_FILE_BUFFER_SIZE	32
#endif

/*
 * This struct is equivalent to the EAS_HW_FILE
 * struct in eas_host.c file.
 *
 * this structure and the related function are here
 * to support the ability to create duplicate handles
 * and buffering into a single file. If the OS supports
 * duplicate file handles natively, this code can be
 * stripped to eliminate double-buffering.
 */
typedef struct eas_hw_file_native_tag {
	FILE *pFile;
	EAS_I32 bytesInBuffer;
	EAS_I32 bytesRead;
	EAS_I32 filePos;
	EAS_BOOL dup;
	EAS_U8 buffer[EAS_FILE_BUFFER_SIZE];
} EAS_HW_FILE_NATIVE, *EAS_FILE_NATIVE;

/*
 * This struct is equivalent to the EAS_HW_FILE
 * struct in eas_hostmm.c file.
 *
 * this structure and the related function are here
 * to support the ability to create duplicate handles
 * and buffering it in memory. If your system uses
 * in-memory resources, you can eliminate the calls
 * to malloc and free, the dup flag, and simply track
 * the file size and read position.
 */
typedef struct {
	EAS_I32 fileSize;
	EAS_I32 filePos;
	EAS_BOOL dup;
	EAS_U8 *buffer;
	/* the media buffer used to set up this memory file.
	 * Only required for MMAPI_HWWrite() */
	MMAPI_MediaBuffer* mb;
} EAS_HW_FILE_MEMORY, *EAS_FILE_MEMORY;

/*
 * STREAM mode just uses the MMAPI_MediaBuffer struct
 */
typedef MMAPI_MediaBuffer EAS_HW_FILE_STREAM, *EAS_FILE_STREAM;

typedef struct eas_hw_file_tag {
	int mode;
	EAS_BOOL inUse;
	union {
		EAS_HW_FILE_NATIVE native;
		EAS_HW_FILE_MEMORY memory;
		/* STREAM mode uses the MMAPI_MediaBuffer directly */
		EAS_FILE_STREAM stream;
	};
	/* the recording hook */
	MMAPI_RecordingData* record;
} EAS_HW_FILE_MIXED;

typedef struct eas_hw_inst_data_tag
{
	EAS_HW_FILE_MIXED files[EAS_MAX_FILE_HANDLES];
} EAS_HW_INST_DATA;


/* local memory for files and streams */
#ifdef _STATIC_MEMORY
EAS_HW_INST_DATA fileData;
#endif

/*----------------------------------------------------------------------------
 * MMAPI_HWRecordBuffer (forward declaration)
 *
 * Internal function: for every call to EAS_HWRead, call this function
 * with the just read data.
 *----------------------------------------------------------------------------
*/
EAS_RESULT MMAPI_HWRecordBuffer(EAS_HW_DATA_HANDLE hwInstData, EAS_HW_FILE_MIXED* file,
								EAS_U8* buffer, EAS_I32 count);


/*----------------------------------------------------------------------------
 * EAS_HWInit
 *
 * Initialize host wrapper interface.
 * This initialization function works for all 3 modes.
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
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
/*lint -e{715} hwInstData available for customer use */
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
/*lint -e{715} hwInstData available for customer use */
void *EAS_HWMalloc(EAS_HW_DATA_HANDLE hwInstData, EAS_I32 size)
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
/*lint -e{715} hwInstData available for customer use */
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
void *EAS_HWMemCpy(void *dest, const void *src, EAS_I32 amount)
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
void *EAS_HWMemSet(void *dest, int val, EAS_I32 amount)
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
EAS_I32 EAS_HWMemCmp(const void *s1, const void *s2, EAS_I32 amount)
{
	return (EAS_I32) memcmp(s1, s2, (size_t) amount);
}



/*----------------------------------------------------------------------------
 *
 * NATIVE host functions
 * ---------------------
 * Host functions based on native FILE's. From eas_host.c
 *
 *----------------------------------------------------------------------------
*/

/*----------------------------------------------------------------------------
 *
 * EAS_HWOpenFileNative
 *
 * Open a file for read or write
 *
 * This function is modified from the original signature: the handle
 * must be set up before calling this implementation of EAS_HWOpenFile().
 *
 * locator points to a string with the filename.
 *
 * The implementation is slightly enhanced to parse the locator and remove
 * a protocol prefix like "file://"
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWOpenFileNative(EAS_HW_DATA_HANDLE hwInstData, EAS_CHAR *locator,
								EAS_FILE_NATIVE file, EAS_FILE_MODE mode) {
	EAS_CHAR* c;
	EAS_CHAR* openMode;

#ifdef SONIVOX_DEBUG
	EAS_Report(4, "> EAS_HWOpenFileNative(instData=%p, locator=%s, file=%p, openMode=%d)\n",
		hwInstData, (char*) locator, file, mode);
#endif

	/* support read mode or write mode */
	if (mode == EAS_FILE_READ) {
		openMode = "rb";
	} else if (mode == EAS_FILE_WRITE) {
		openMode = "wb";
	} else {
#ifdef SONIVOX_DEBUG
		EAS_Report(_EAS_SEVERITY_ERROR, "< EAS_HWOpenFileNative(): openMode ERROR\n");
#endif
		return EAS_ERROR_INVALID_FILE_MODE;
	}

	/* parse for protocol prefix */
	if (locator != NULL) {
		c = locator;
		/* simple implementation for now) */
		if (c[0]=='f' && c[1]=='i' && c[2]=='l' && c[3]=='e'
			&& c[4]==':' && c[5]=='/' && c[6]=='/') {
			locator += 7;
		}
	}


	/* open the file */
	file->pFile = fopen((const char*) locator, openMode);
	if (file->pFile == NULL) {
#ifdef SONIVOX_DEBUG
		EAS_Report(_EAS_SEVERITY_ERROR, "< ERROR: EAS_HWOpenFileNative(locator=%s)\n", (char*) locator);
#endif
		return EAS_ERROR_FILE_OPEN_FAILED;
	}

	/* initialize some values */
	file->bytesInBuffer = 0;
	file->bytesRead = 0;
	file->filePos = 0;
	file->dup = EAS_FALSE;

	return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWReadFileNative
 *
 * Read data from a file
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWReadFileNative(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_NATIVE file, void *pBuffer, EAS_I32 n, EAS_I32 *pBytesRead)
{
	/* check handle integrity */
	if (file->pFile == NULL)
		return EAS_ERROR_INVALID_HANDLE;

	/* if duplicated handle,  need to reposition in case the file pointer moved */
	if (file->dup)
	{

		/* reposition the file pointer */
		if (fseek(file->pFile,file->filePos,SEEK_SET) != 0)
			return EAS_ERROR_FILE_SEEK;

		/* reset the buffer info so EAS_HWGetByte doesn't fail */
		file->bytesInBuffer = 0;
		file->bytesRead = 0;
	}

	/* read data in the buffer */
	*pBytesRead = (EAS_I32) fread(pBuffer, 1, (size_t) n, file->pFile);
	file->filePos += *pBytesRead;

	/* were n bytes read? */
	if (*pBytesRead != n)
		return EAS_EOF;
	return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWGetByteNative
 *
 * Read a byte from a file
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWGetByteNative(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_NATIVE file, void *p)
{
	int c;

	/* check handle integrity */
	if (file->pFile == NULL)
		return EAS_ERROR_INVALID_HANDLE;

	/* if no duplicate handle, use fgetc */
	if (!file->dup)
	{
		if ((c = fgetc(file->pFile)) == EOF)
			return EAS_EOF;
		*((EAS_U8*) p) = (EAS_U8) c;
		file->filePos++;
		return EAS_SUCCESS;
	}

	/* use local buffer - do we have any data? */
	if (file->bytesInBuffer <= file->bytesRead)
	{

		/* reposition the file pointer */
		if (fseek(file->pFile,file->filePos,SEEK_SET) != 0)
			return EAS_ERROR_FILE_SEEK;

		/* read some data from the file */
		file->bytesInBuffer = (EAS_I32) fread(file->buffer,1,EAS_FILE_BUFFER_SIZE,file->pFile);

		/* if nothing to read, return EOF */
		if (file->bytesInBuffer == 0)
			return EAS_EOF;

		/* reset buffer info */
		file->bytesRead = 0;
	}

	/* get a character from the buffer */
	*((EAS_U8*) p) = file->buffer[file->bytesRead++];
	file->filePos++;

	return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFilePosNative
 *
 * Returns the current location in the file
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWFilePosNative(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_NATIVE file, EAS_I32 *pPosition)
{
	/* check handle integrity */
	if (file->pFile == NULL)
		return EAS_ERROR_INVALID_HANDLE;

	*pPosition = file->filePos;
	return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFileSeekNative
 *
 * Seek to a specific location in the file
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWFileSeekNative(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_NATIVE file, EAS_I32 position)
{
	/* check handle integrity */
	if (file->pFile == NULL)
		return EAS_ERROR_INVALID_HANDLE;

	if (fseek(file->pFile,position,SEEK_SET) != 0) {
#ifdef SONIVOX_DEBUG
		EAS_Report(4, "  EAS_HWFileSeekNative(file=%p): ERROR: fseek failed. file->pFile=%p, position=%d\n",
			(void*) file, (void*) file->pFile, (int)position);
#endif

		return EAS_ERROR_FILE_SEEK;
	}

	/* save new position and reset buffer info so EAS_HWGetByte doesn't fail */
	file->filePos = position;
	file->bytesInBuffer = 0;
	file->bytesRead = 0;

#ifdef SONIVOX_DEBUG
	EAS_Report(4, "  EAS_HWFileSeekNative(file=%p): OK. position=%d\n",
		(void*) file, (int) file->filePos);
#endif
	return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFileSeekOfsNative
 *
 * Seek forward or back relative to the current position
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWFileSeekOfsNative(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_NATIVE file, EAS_I32 position)
{
	/* check handle integrity */
	if (file->pFile == NULL)
		return EAS_ERROR_INVALID_HANDLE;

	if (fseek(file->pFile,position,SEEK_CUR) != 0)
		return EAS_ERROR_FILE_SEEK;

	/* save new position and reset buffer info so EAS_HWGetByte doesn't fail */
	file->filePos += position;
	file->bytesInBuffer = 0;
	file->bytesRead = 0;

	return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFileLengthNative
 *
 * Return the file length
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWFileLengthNative(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_NATIVE file, EAS_I32 *pLength)
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
 * EAS_HWDupHandleNative
 *
 * Duplicate a file handle
 *
 * This function signature is modified from the original: the handle
 * must be set up before calling this implementation of EAS_HWOpenFile().
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWDupHandleNative(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_NATIVE file,
								 EAS_FILE_NATIVE newFile) {
	/* check handle integrity */
	if (file->pFile == NULL)
		return EAS_ERROR_INVALID_HANDLE;

	/* copy info from the handle to be duplicated */
	newFile->filePos = file->filePos;
	newFile->pFile = file->pFile;

	/* set the duplicate handle flag */
	newFile->dup = file->dup = EAS_TRUE;

	/* initialize some values */
	newFile->bytesInBuffer = 0;
	newFile->bytesRead = 0;

	return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWCloseNative
 *
 * Wrapper for fclose function
 *
 * This method accesses the array of EAS_HW_FILE_MIXED structs!
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWCloseFileNative(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_NATIVE file1)
{
	EAS_HW_FILE_NATIVE* dupFile;
	EAS_HW_FILE_MIXED* file2;
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
			if ((file1 != &(file2->native)) && (file2->native.pFile == file1->pFile))
			{
				/* is there more than one duplicate? */
				if (dupFile != NULL)
				{
					/* clear this entry and return */
					file1->pFile = NULL;
					#ifdef SONIVOX_DEBUG
					EAS_Report(4, "  Closed this duplicate. More file instances remaining.\n");
					#endif
					return EAS_SUCCESS;
				}

				/* this is the first duplicate found */
				dupFile = &(file2->native);
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
		#ifdef SONIVOX_DEBUG
		EAS_Report(4, "  Closed this duplicate. One more file instance remaining.\n");
		#endif
		return EAS_SUCCESS;
	}

	/* no duplicates - close the file */
	if (fclose(file1->pFile) != 0)
		return EAS_ERROR_CLOSE_FAILED;

	/* clear this entry and return */
	file1->pFile = NULL;
	#ifdef SONIVOX_DEBUG
	EAS_Report(4, "  Closed last instance of this file.\n");
	#endif
	return EAS_SUCCESS;
}



/*----------------------------------------------------------------------------
*
 * MEMORY host functions
 * ---------------------
 * Host functions based on memory images of the files. Based on eas_hostmm.c,
 * but extended to allow Java to fill the memory image.
 *
 *----------------------------------------------------------------------------
*/


/*----------------------------------------------------------------------------
 *
 * EAS_HWOpenFileMemory
 *
 * Open a file for read or write
 *
 * This function is modified from the original signature: the handle
 * must be set up before calling this implementation of EAS_HWOpenFile().
 *
 * Also, this function does not load the native file into memory (as the
 * implementation in eas_hostmm.c does). The memory buffer is pre-set and
 * pre-loaded in eas_mmapi.c.
 *
 * locator points to a MMAPI_MediaBuffer structure.
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWOpenFileMemory(EAS_HW_DATA_HANDLE hwInstData, MMAPI_MediaBuffer *locator,
								EAS_FILE_MEMORY file, EAS_FILE_MODE mode)
{
	MMAPI_MediaBuffer* mb = (MMAPI_MediaBuffer*) locator;

#ifdef SONIVOX_DEBUG
	EAS_Report(4, "> EAS_HWOpenFileMemory(instData=%p, mediaBuffer=%p, file=%p, openMode=%s%s)\n",
		hwInstData, locator, file, (mode & EAS_FILE_READ)?"R":"", (mode & EAS_FILE_WRITE)?"W":"");
	EAS_Report(4, " mediaBuffer: totalSize=%d, buffer=%p\n", mb->totalSize, mb->buffer);
#endif

	/* initialize the structure */
	file->fileSize = mb->totalSize;
	file->buffer = mb->buffer;
	file->filePos = 0;
	file->dup = EAS_FALSE;

	/* required for write mode */
	file->mb = mb;

#ifdef SONIVOX_DEBUG
	EAS_Report(4, "< EAS_HWOpenFileMemory\n");
#endif
	return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWReadFileMemory
 *
 * Read data from a file
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} some hosts may not use hwInstData */
EAS_RESULT EAS_HWReadFileMemory(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_MEMORY file, void *pBuffer, EAS_I32 n, EAS_I32 *pBytesRead)
{
	EAS_I32 count;
	#ifdef SONIVOX_DEBUG_IO
	int a,b;
	#endif

	/* make sure we have a valid handle */
	if (file->buffer == NULL)
		return EAS_ERROR_INVALID_HANDLE;

	/* calculate the bytes to read */
	count = file->fileSize - file->filePos;
	if (n < count)
		count = n;

	/* copy the data to the requested location, and advance the pointer */
	if (count) {
		EAS_HWMemCpy(pBuffer, &file->buffer[file->filePos], count);
		#ifdef SONIVOX_DEBUG_IO
		a = ((EAS_U8*)pBuffer)[0];
		b = ((EAS_U8*)pBuffer)[1];
		#endif
	}
	#ifdef SONIVOX_DEBUG_IO
	EAS_Report(5, "EAS_HWReadFileMemory: read %d bytes at offset %d: %2x, %2x, ...\n",
		(int) count, (int) file->filePos, a, b);
	#endif
	file->filePos += count;
	*pBytesRead = count;

	/* were n bytes read? */
	if (count!= n)
		return EAS_EOF;
	return EAS_SUCCESS;
}


/*----------------------------------------------------------------------------
 *
 * EAS_HWGetByteMemory
 *
 * Read a byte from a file
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} some hosts may not use hwInstData */
EAS_RESULT EAS_HWGetByteMemory (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_MEMORY file, void *p)
{
	/* make sure we have a valid handle */
	if (file->buffer == NULL)
		return EAS_ERROR_INVALID_HANDLE;

	/* check for end of file */
	if (file->filePos >= file->fileSize)
	{
		*((EAS_U8*) p) = 0;
		return EAS_EOF;
	}

	/* get a character from the buffer */
	*((EAS_U8*) p) = file->buffer[file->filePos++];

	return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFilePosMemory
 *
 * Returns the current location in the file
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} some hosts may not use hwInstData */
EAS_RESULT EAS_HWFilePosMemory(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_MEMORY file, EAS_I32 *pPosition)
{
	/* make sure we have a valid handle */
	if (file->buffer == NULL)
		return EAS_ERROR_INVALID_HANDLE;

	*pPosition = file->filePos;
	return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFileSeekMemory
 *
 * Seek to a specific location in the file
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} some hosts may not use hwInstData */
EAS_RESULT EAS_HWFileSeekMemory(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_MEMORY file, EAS_I32 position)
{
	/* make sure we have a valid handle */
	if (file->buffer == NULL)
		return EAS_ERROR_INVALID_HANDLE;

	/* validate new position */
	if ((position < 0) || (position > file->fileSize))
		return EAS_ERROR_FILE_SEEK;

	/* save new position */
	file->filePos = position;
	return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFileSeekOfsMemory
 *
 * Seek forward or back relative to the current position
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWFileSeekOfsMemory(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_MEMORY file, EAS_I32 position)
{
	/* make sure we have a valid handle */
	if (file->buffer == NULL)
		return EAS_ERROR_INVALID_HANDLE;

	/* determine the file position */
	position += file->filePos;
	if ((position < 0) || (position > file->fileSize))
		return EAS_ERROR_FILE_SEEK;

	/* save new position */
	file->filePos = position;
	return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFileLengthMemory
 *
 * Return the file length
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} some hosts may not use hwInstData */
EAS_RESULT EAS_HWFileLengthMemory(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_MEMORY file, EAS_I32 *pLength)
{
	/* make sure we have a valid handle */
	if (file->buffer == NULL)
		return EAS_ERROR_INVALID_HANDLE;

	*pLength = file->fileSize;
	return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWDupHandleMemory
 *
 * Duplicate a file handle. newFile points to an already retrieved entry.
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWDupHandleMemory(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_MEMORY file, EAS_FILE_MEMORY dupFile)
{
	/* make sure we have a valid handle */
	if (file->buffer == NULL)
		return EAS_ERROR_INVALID_HANDLE;

	/* copy info from the handle to be duplicated */
	dupFile->filePos = file->filePos;
	dupFile->fileSize = file->fileSize;
	dupFile->buffer = file->buffer;
	dupFile->mb = file->mb;

	/* set the duplicate handle flag */
	dupFile->dup = file->dup = EAS_TRUE;

	return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWCloseMemory
 *
 * Wrapper for fclose function
 *
 * This method accesses the array of hwInstData!
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWCloseFileMemory (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_MEMORY file1)
{
	EAS_HW_FILE_MEMORY *dupFile;
	EAS_HW_FILE_MIXED* file2;
	int i;

	/* make sure we have a valid handle */
	if (file1->buffer == NULL)
		return EAS_ERROR_INVALID_HANDLE;

	/* check for duplicate handle */
	if (file1->dup)
	{
		dupFile = NULL;
		file2 = hwInstData->files;
		for (i = 0; i < EAS_MAX_FILE_HANDLES; i++)
		{
			/* check for duplicate */
			if ((file1 != &(file2->memory)) && (file2->memory.buffer == file1->buffer))
			{
				/* is there more than one duplicate? */
				if (dupFile != NULL)
				{
					/* clear this entry and return */
					file1->buffer = NULL;
					return EAS_SUCCESS;
				}

				/* this is the first duplicate found */
				else
					dupFile = &(file2->memory);
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
		file1->buffer = NULL;
		return EAS_SUCCESS;
	}

	/* DO NOT FREE the buffer here. The buffer is owned by eas_mmapi.c */

	/* clear this entry and return */
	file1->buffer = NULL;
	return EAS_SUCCESS;
}


/*----------------------------------------------------------------------------
 *
 * STREAM host functions
 * ---------------------
 * Host functions based on a circular buffer that is read by EAS and filled
 * by the Java layer.
 *
 *----------------------------------------------------------------------------
*/

/*----------------------------------------------------------------------------
 *
 * EAS_HWOpenFileStream
 *
 * Open a file for read or write
 *
 * This function is modified from the original signature: the handle
 * must be set up before calling this implementation of EAS_HWOpenFile().
 *
 * locator is ignored.
 * mb points to the MMAPI_MediaBuffer instance to use. The buffer
 * is already set up and pre-filled with data in eas_mmapi.c.
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWOpenFileStream(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_LOCATOR locator,
								EAS_FILE_STREAM mb, EAS_FILE_MODE mode) {
	/* a quick sanity check */
	if (mb == NULL) {
		#ifdef SONIVOX_DEBUG
		EAS_Report(2, "EAS_HWOpenFileStream(): error, mb is NULL!\n");
		#endif
		return EAS_ERROR_FILE_OPEN_FAILED;
	}
	return EAS_SUCCESS;
}

/*
 * Internal function to check for EOF and wrap-around in STREAM mode.
 * @return true if EOF is reached.
 */
EAS_BOOL EAS_HWIsEOF_Stream(MMAPI_MediaBuffer* mb) {
	/* check for end of file */
	#ifdef SONIVOX_DEBUG_ISEOF
	EAS_Report(5, "EAS_HWIsEOF_Stream: mb->readPos(%d) + mb->readPosOffset(%d) = %d, mb->totalSize=%d \n",
		mb->readPos, mb->readPosOffset, mb->readPos + mb->readPosOffset, mb->totalSize);
	#endif
	if (mb->totalSize >= 0 && mb->readPos + mb->readPosOffset >= mb->totalSize) {
		#ifdef SONIVOX_DEBUG
		EAS_Report(4, "EAS_HWIsEOF_Stream: found EOF. mb->totalSize=%d\n", mb->totalSize);
		#endif
		return EAS_TRUE;
	}
	return EAS_FALSE;
}

/*
 * Internal function to handle wrap-around of the circular
 * buffer in STREAM mode.
 */
void EAS_HWHandleWrapStream(MMAPI_MediaBuffer* mb) {
	/* check for wrap around */
	if (mb->readPos >= mb->bufferSize) {
		mb->readPos -= mb->bufferSize;
		mb->readPosOffset += mb->bufferSize;
	}
}

#ifdef MMAPI_PROVIDE_SILENCE_ON_UNDERRUN

/*
 * minimum number of silence bytes written to streaming buffer
 * on buffer underrun
 */
#define MMAPI_MIN_SILENCE_WRITE (240)

/*
 * For stream operation: fills some silence bytes so that
 * the read operation will not fail.
 */
void EAS_CheckUnderrunFillSilence(MMAPI_MediaBuffer* mb, EAS_I32 minSize) {
	if (minSize > 0 && minSize > mb->bufferFilled) {
		if (minSize > mb->bufferSize) {
			minSize = mb->bufferSize;
		}
		minSize = minSize - mb->bufferFilled;
		if (minSize < MMAPI_MIN_SILENCE_WRITE) {
			minSize = MMAPI_MIN_SILENCE_WRITE;
		}
		#ifdef SONIVOX_DEBUG
		EAS_Report(2, "EAS_HWReadFileStream: buffer underrun, inserting %d bytes silence\n", minSize);
		#endif
		MMAPI_HWWriteFileImpl(MMAPI_OPEN_MODE_STREAM, mb, NULL, minSize, &minSize);
	}
}
#endif


/*----------------------------------------------------------------------------
 *
 * EAS_HWReadFileStream
 *
 * Read data from a file stream
 * Copy data from the circular buffer to pBuffer.
 * mb points to the MMAPI_MediaBuffer
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWReadFileStream(EAS_HW_DATA_HANDLE hwInstData,
								EAS_FILE_STREAM mb,
								void *pBuffer,
								EAS_I32 n,
								EAS_I32 *pBytesRead) {
	EAS_I32 i, thisCount;
	EAS_U8* from;
	EAS_U8* to = pBuffer;
	#ifdef SONIVOX_DEBUG_IO
	int a,b;
	#endif

	(*pBytesRead) = 0;

	/* check for end of file */
	if (EAS_HWIsEOF_Stream(mb)) {
		return EAS_EOF;
	}
	/* return silence if necessary */
#ifdef MMAPI_PROVIDE_SILENCE_ON_UNDERRUN
	if (!mb->noSilenceOnUnderrun) {
		EAS_CheckUnderrunFillSilence(mb, n);
	}
#endif

	/* copy at most twice */
	for (i = 0; i < 2; i++) {
		from = mb->buffer;
		from += mb->readPos;
		thisCount = n;
		/* need to account for wrap-around at buffer boundary */
		if (thisCount + mb->readPos > mb->bufferSize) {
			thisCount = mb->bufferSize - mb->readPos;
		}
		/* cannot read more than the number of bytes written to the buffer */
		if (thisCount > mb->bufferFilled) {
			thisCount = mb->bufferFilled;
		}

		if (thisCount > 0) {
			EAS_HWMemCpy(to, from, thisCount);
			#ifdef SONIVOX_DEBUG_IO
			if (i == 0) {
				a = to[0];
				b = to[1];
			}
			#endif
			mb->bufferFilled -= thisCount;
			mb->readPos += thisCount;
			EAS_HWHandleWrapStream(mb);
			to += thisCount;
			n -= thisCount;
			(*pBytesRead) += thisCount;
		}
	}
	#ifdef SONIVOX_DEBUG_IO
	EAS_Report(5, "EAS_HWReadFileStream: read %d bytes: %2x, %2x, ...\n",
		(int) *(pBytesRead), a,b);
	#endif

	return EAS_SUCCESS;
}


/*----------------------------------------------------------------------------
 *
 * EAS_HWGetByteStream
 *
 * Read a byte from a file stream
 * mb points to the MMAPI_MediaBuffer
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWGetByteStream(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_STREAM mb, void *p) {

	/* check for end of file */
	if (EAS_HWIsEOF_Stream(mb)) {
		*((EAS_U8*) p) = 0;
		return EAS_EOF;
	}
#ifdef MMAPI_PROVIDE_SILENCE_ON_UNDERRUN
	if (!mb->noSilenceOnUnderrun) {
		EAS_CheckUnderrunFillSilence(mb, 1);
	}
#endif
	if (mb->bufferFilled >= 1) {
		*((EAS_U8*) p) = mb->buffer[mb->readPos++];
		mb->bufferFilled -= 1;
		EAS_HWHandleWrapStream(mb);
		return EAS_SUCCESS;
	}
	#ifdef SONIVOX_DEBUG
	EAS_Report(2, "EAS_HWGetByteStream: cannot read a byte, need more buffer!\n");
	#endif
	/* should really return "buffering" */
	return EAS_ERROR_FILE_READ_FAILED;
	/* return EAS_STREAM_BUFFERING; */
}


/*----------------------------------------------------------------------------
 *
 * EAS_HWFilePosStream
 *
 * Returns the current read location in the file.
 * mb points to the MMAPI_MediaBuffer
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWFilePosStream(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_STREAM mb, EAS_I32 *pPosition) {
	(*pPosition) = mb->readPos + mb->readPosOffset;
	return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFileSeekOfsStream
 *
 * Seek forward or back relative to the current position.
 * This implementation just skips data that was already read.
 * A problem is that if it is seeked forward, immediate reads() will
 * fail if not enough data is already cached.
 *
 * mb points to the MMAPI_MediaBuffer
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWFileSeekOfsStream(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_STREAM mb,
								   EAS_I32 position) {

	if ((position < mb->bufferFilled - mb->bufferSize)
		|| (position > mb->bufferFilled)) {
		#ifdef SONIVOX_DEBUG
		EAS_Report(2, "EAS_HWFileSeekStream: cannot seek to %d, need more buffer! currPos=%d, bufferFilled=%d\n",
			mb->readPosOffset + mb->readPos + position, mb->readPosOffset + mb->readPos, mb->bufferFilled);
		#endif
		return EAS_ERROR_FILE_SEEK;
		/* return EAS_STREAM_BUFFERING; */
	}
	/*
	#ifdef SONIVOX_DEBUG
	EAS_Report(2, "EAS_HWFileSeekStream: seek to %d. before: currPos=%d, readPos=%d  bufferFilled=%d  \n",
		mb->readPosOffset + mb->readPos + position, mb->readPosOffset + mb->readPos, mb->readPos, mb->bufferFilled);
	#endif
	*/

	mb->readPos += position;
	mb->bufferFilled -= position;
	/* check wraps */
	if (mb->readPos < 0) {
		mb->readPos += mb->bufferSize;
		mb->readPosOffset -= mb->bufferSize;
	} else {
		EAS_HWHandleWrapStream(mb);
	}

	/*
	#ifdef SONIVOX_DEBUG
	EAS_Report(2, "EAS_HWFileSeekStream:       after: currPos=%d, readPos=%d  bufferFilled=%d  \n",
		mb->readPosOffset + mb->readPos, mb->readPos, mb->bufferFilled);
	#endif
	*/

	return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFileSeekStream
 *
 * Seek to a specific location in the file.
 * This implementation converts the position to a relative offset
 * and calls EAS_HWFileSeekOfsStream,
 *
 * mb points to the MMAPI_MediaBuffer
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWFileSeekStream (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_STREAM mb,
								 EAS_I32 position) {
	EAS_I32 currPos = mb->readPos + mb->readPosOffset;
	return EAS_HWFileSeekOfsStream(hwInstData, mb, position - currPos);
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFileLengthStream
 *
 * Return the file length
 * mb points to the MMAPI_MediaBuffer
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWFileLengthStream(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_STREAM mb,
								  EAS_I32 *pLength) {

	if (mb->totalSize >= 0) {
		(*pLength) = mb->totalSize;
	} else {
		(*pLength) = MMAPI_FILE_SIZE_UNKNOWN;
	}
	#ifdef SONIVOX_DEBUG
	EAS_Report(5, "EAS_HWFileLengthStream: returning %d = 0x%x\n", (*pLength), (*pLength));
	#endif
	return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWDupHandleStream
 *
 * Duplicate a file handle.
 * This implementation cannot duplicate STREAM handles.
 *
 * mb points to the MMAPI_MediaBuffer
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWDupHandleStream(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_STREAM mb,
								 EAS_FILE_STREAM newMB) {
	#ifdef SONIVOX_DEBUG
	EAS_Report(2, "EAS_HWDupHandleStream: cannot duplicate stream\n");
	#endif

	return EAS_ERROR_FEATURE_NOT_AVAILABLE;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWCloseStream
 *
 * Close the stream -- nothing to do, since the buffer is managed by eas_mmapi.c.
 *
 * mb points to the MMAPI_MediaBuffer
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWCloseFileStream(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_STREAM mb) {
	return EAS_SUCCESS;
}

#ifdef MMAPI_DEBUG_WRITE_STREAM_DUMP
FILE* STREAM_DUMP_HANDLE = NULL;
#endif

/*----------------------------------------------------------------------------
 *
 * Master host functions
 * ---------------------
 * Dispatch to the specialized host functions depending on the mode
 *
 *----------------------------------------------------------------------------
*/

/*----------------------------------------------------------------------------
 *
 * EAS_HWOpenFile (master)
 *
 * Open a file for read or write
 *
 * This implementation searches an available entry in the list of
 * EAS_HW_FILE_MIXED entries. When a suitable entry is found, it calls
 * the respective implementation of EAS_HWOpenFile{Native|Memory|Stream}.
 *
 * The locator parameter points to a MMAPI_FILE_STRUCT structure, encapsulating
 * the "real" locator, plus the MMAPI_OPEN_MODE.
 *
 * If the state of the "record" struct in MMAPI_FILE_STRUCT is not
 * MMAPI_RS_NATIVE_ERROR, a pointer to the record struct is remembered
 * and checked in EAS_HWRead() for the recording hook.
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWOpenFile(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_LOCATOR locator, EAS_FILE_HANDLE *pHandle, EAS_FILE_MODE mode)
{
	EAS_HW_FILE_MIXED *file;
	int i;
	EAS_RESULT ret;
	MMAPI_FILE_STRUCT* fHandle = (MMAPI_FILE_STRUCT*) locator;

#ifdef SONIVOX_DEBUG
	EAS_Report(4, "> EAS_HWOpenFile(instData=%p, locator=%p, EAS_FileOpenMode=%d)\n",
		hwInstData, locator, mode);
#endif
	/* set return value to NULL */
	*pHandle = NULL;

	/* find an empty entry in the file table */
	file = hwInstData->files;
	for (i = 0; i < EAS_MAX_FILE_HANDLES; i++)
	{
		/* is this slot being used? */
		if (!file->inUse)
		{
			ret = EAS_ERROR_HANDLE_INTEGRITY;
			/* actually open the file */
			switch (fHandle->mode) {
				case MMAPI_OPEN_MODE_NATIVE:
					ret = EAS_HWOpenFileNative(NULL, fHandle->locator, &(file->native), mode);
					break;
				case MMAPI_OPEN_MODE_MEMORY:
					ret = EAS_HWOpenFileMemory(NULL, fHandle->mb, &(file->memory), mode);
					break;
				case MMAPI_OPEN_MODE_STREAM:
					file->stream = fHandle->mb;
					ret = EAS_HWOpenFileStream(NULL, NULL, file->stream, mode);
					break;
			}
			if (ret == EAS_SUCCESS) {
				/* set the mode and inUse flags */
				file->mode = fHandle->mode;
				file->inUse = EAS_TRUE;
				if (fHandle->record.state != MMAPI_RS_NATIVE_ERROR) {
					file->record = &(fHandle->record);
				}
				/* set the returned handle */
				(*pHandle) = (EAS_FILE_HANDLE) file;
				/* set the host handles in the MMAPI_FILE_STRUCT so that eas_mmapi.c
				 * can call host functions directly */
				fHandle->hwInstData = hwInstData;
				fHandle->hwFileHandle = (*pHandle);
#ifdef MMAPI_DEBUG_WRITE_STREAM_DUMP
				STREAM_DUMP_HANDLE = fopen("C:\\streamdump.pcm", "wb");
#endif
#ifdef SONIVOX_DEBUG
				EAS_Report(4, "< EAS_HWOpenFile: pHandle=%p MMAPI_mode=%d\n", (void*) (*pHandle), file->mode);
#endif
			}
			return ret;
		}
		file++;
	}
#ifdef SONIVOX_DEBUG
	EAS_Report(4, "< EAS_HWOpenFile: ERROR too many files open.\n");
#endif
	return EAS_ERROR_MAX_FILES_OPEN;
}


/*
 * see eas_mmapi.h for details
 */
EAS_RESULT MMAPI_HWSwitchToMemoryMode(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle) {
	EAS_HW_FILE_MIXED* file = (EAS_HW_FILE_MIXED*) fHandle;
	EAS_I32 pos;
	EAS_RESULT res;
	void* buffer;

	/* some sanity checks */
	if (file->mode == MMAPI_OPEN_MODE_MEMORY) {
		/* nothing to do */
		return EAS_SUCCESS;
	}
	if (!file->inUse || file->mode != MMAPI_OPEN_MODE_STREAM) {
		return EAS_ERROR_INVALID_PARAMETER;
	}
	/* remember the file position and buffer */
	res = EAS_HWFilePosStream(mHandle, fHandle, &pos);
	buffer = file->stream;
	if (res == EAS_SUCCESS) {
		res = EAS_HWCloseFileStream(mHandle, file->stream);
	}
	if (res == EAS_SUCCESS) {
		EAS_HWMemSet(&(file->memory), 0, sizeof(EAS_HW_FILE_MEMORY));
		file->mode = MMAPI_OPEN_MODE_MEMORY;
		res = EAS_HWOpenFileMemory(NULL, buffer, &(file->memory), EAS_FILE_READ);
	}
	if (res == EAS_SUCCESS) {
		res = EAS_HWFileSeekMemory(NULL, &(file->memory), pos);
	}
	if (res != EAS_SUCCESS) {
		EAS_HWCloseFile(mHandle, fHandle);
		/* make sure that this handle is freed */
		file->inUse = EAS_FALSE;
	}
	return res;
}


/*----------------------------------------------------------------------------
 *
 * EAS_HWReadFile (master)
 *
 * Read data from a file
 *
 * Dispatch to the respective {Native|Memory|Stream} implementations.
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWReadFile(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file,
						  void *pBuffer, EAS_I32 n, EAS_I32 *pBytesRead) {
	EAS_RESULT res = EAS_ERROR_HANDLE_INTEGRITY;
#ifdef SONIVOX_DEBUG_IO
	EAS_Report(5, "EAS_HWReadFile(instData=%p, file=%p, count=%d)\n",
		hwInstData, file, n);
#endif
	switch (file->mode) {
	case MMAPI_OPEN_MODE_NATIVE:
		res = EAS_HWReadFileNative(NULL, &(file->native), pBuffer, n, pBytesRead);
		break;
	case MMAPI_OPEN_MODE_MEMORY:
		res = EAS_HWReadFileMemory(NULL, &(file->memory), pBuffer, n, pBytesRead);
		break;
	case MMAPI_OPEN_MODE_STREAM:
		res = EAS_HWReadFileStream(NULL, file->stream, pBuffer, n, pBytesRead);
		break;
	}
	/* the recording hook: if recording enabled, write the buffer to the record file */
	if (res == EAS_SUCCESS
			&& file->record != NULL
			&& file->record->handle != NULL
			&& file->record->state == MMAPI_RS_RECORDING) {
		#ifdef SONIVOX_DEBUG_IO
				EAS_Report(5, "EAS_HWReadFile: recording hook, writing %d bytes\n", (*pBytesRead));
		#endif
		MMAPI_HWRecordBuffer(hwInstData, file, pBuffer, (*pBytesRead));
	}
#ifdef MMAPI_DEBUG_WRITE_STREAM_DUMP
	fwrite(pBuffer, (*pBytesRead), 1, STREAM_DUMP_HANDLE);
#endif
	return res;

}

/*----------------------------------------------------------------------------
 *
 * EAS_HWGetByte (master)
 *
 * Read a byte from a file
 *
 * Dispatch to the respective {Native|Memory|Stream} implementations.
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWGetByte(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, void *p) {
	EAS_RESULT res = EAS_ERROR_HANDLE_INTEGRITY;

	switch (file->mode) {
	case MMAPI_OPEN_MODE_NATIVE:
		res = EAS_HWGetByteNative(NULL, &(file->native), p);
		break;
	case MMAPI_OPEN_MODE_MEMORY:
		res = EAS_HWGetByteMemory(NULL, &(file->memory), p);
		break;
	case MMAPI_OPEN_MODE_STREAM:
		res = EAS_HWGetByteStream(NULL, file->stream, p);
		break;
	}
	/* the recording hook: if recording enabled, write the buffer to the record file */
	if (res == EAS_SUCCESS
			&& file->record != NULL
			&& file->record->handle != NULL
			&& file->record->state == MMAPI_RS_RECORDING) {
				#ifdef SONIVOX_DEBUG_IO
				EAS_Report(5, "EAS_HWGetByte: recording hook, writing 1 byte\n", (*pBytesRead));
				#endif
				MMAPI_HWRecordBuffer(hwInstData, file, (EAS_U8*) p, 1);
	}
#ifdef MMAPI_DEBUG_WRITE_STREAM_DUMP
	fwrite(p, 1, 1, STREAM_DUMP_HANDLE);
#endif
return res;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWGetWord (master)
 *
 * Returns the current location in the file
 *
 * This method uses 2 calls to EAS_HWGetByte() to retrieve
 * a full 16-bit word. This function can be optimized by
 * using a switch (mode) clause and calling the respective
 * EAS_HWGetDWord{Native|Memory|Stream} directly.
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWGetWord (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, void *p, EAS_BOOL msbFirst)
{
	EAS_RESULT result;
	EAS_U8 c1, c2;

	/* read 2 bytes from the file */
	if ((result = EAS_HWGetByte(hwInstData, file,&c1)) != EAS_SUCCESS)
		return result;
	if ((result = EAS_HWGetByte(hwInstData, file,&c2)) != EAS_SUCCESS)
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
 * EAS_HWGetDWord (master)
 *
 * Returns the current location in the file
 *
 * This method uses 4 calls to EAS_HWGetByte() to retrieve
 * a full 32-bit word. This function can be optimized by
 * using a switch (mode) clause and calling the respective
 * EAS_HWGetDWord{Native|Memory|Stream} directly.
 *
 *----------------------------------------------------------------------------
*/
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
		*((EAS_U32*) p) = ((EAS_U32) c4 << 24) | ((EAS_U32) c3 << 16) | ((EAS_U32) c2 << 8) | c1;

	return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFilePos (master)
 *
 * Returns the current location in the file
 *
 * Dispatch to the respective {Native|Memory|Stream} implementations.
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWFilePos (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file,
						  EAS_I32 *pPosition) {
#ifdef SONIVOX_DEBUG_IO
	EAS_Report(5, "EAS_HWFilePos(instData=%p, file=%p)\n",
		hwInstData, file);
#endif
	switch (file->mode) {
	case MMAPI_OPEN_MODE_NATIVE:
		return EAS_HWFilePosNative(NULL, &(file->native), pPosition);
	case MMAPI_OPEN_MODE_MEMORY:
		return EAS_HWFilePosMemory(NULL, &(file->memory), pPosition);
	case MMAPI_OPEN_MODE_STREAM:
		return EAS_HWFilePosStream(NULL, file->stream, pPosition);
	}
	return EAS_ERROR_HANDLE_INTEGRITY;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFileSeek (master)
 *
 * Seek to a specific location in the file
 *
 * Dispatch to the respective {Native|Memory|Stream} implementations.
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWFileSeek (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file,
						   EAS_I32 position) {
#ifdef SONIVOX_DEBUG_SEEK
	EAS_Report(4, "EAS_HWFileSeek(instData=%p, file=%p, pos=%d)\n",
		hwInstData, file, position);
#endif
	switch (file->mode) {
	case MMAPI_OPEN_MODE_NATIVE:
		return EAS_HWFileSeekNative(NULL, &(file->native), position);
	case MMAPI_OPEN_MODE_MEMORY:
		return EAS_HWFileSeekMemory(NULL, &(file->memory), position);
	case MMAPI_OPEN_MODE_STREAM:
		return EAS_HWFileSeekStream(NULL, file->stream, position);
	}
#ifdef SONIVOX_DEBUG
	EAS_Report(2, "  EAS_HWFileSeek: error: file->mode=%d\n", file->mode);
#endif
	return EAS_ERROR_HANDLE_INTEGRITY;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFileSeekOfs (master)
 *
 * Seek forward or back relative to the current position
 *
 * Dispatch to the respective {Native|Memory|Stream} implementations.
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWFileSeekOfs (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file,
							  EAS_I32 position) {
#ifdef SONIVOX_DEBUG_SEEK
	EAS_Report(4, "EAS_HWFileSeekOfs(instData=%p, file=%p, pos=%d)\n",
		hwInstData, file, position);
#endif
	switch (file->mode) {
	case MMAPI_OPEN_MODE_NATIVE:
		return EAS_HWFileSeekOfsNative(NULL, &(file->native), position);
	case MMAPI_OPEN_MODE_MEMORY:
		return EAS_HWFileSeekOfsMemory(NULL, &(file->memory), position);
	case MMAPI_OPEN_MODE_STREAM:
		return EAS_HWFileSeekOfsStream(NULL, file->stream, position);
	}
	return EAS_ERROR_HANDLE_INTEGRITY;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFileLength (master)
 *
 * Return the file length
 *
 * Dispatch to the respective {Native|Memory|Stream} implementations.
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWFileLength (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file,
							 EAS_I32 *pLength) {
#ifdef SONIVOX_DEBUG
	EAS_Report(4, "EAS_HWFileLength(instData=%p, file=%p)\n",
		hwInstData, file);
#endif
	switch (file->mode) {
	case MMAPI_OPEN_MODE_NATIVE:
		return EAS_HWFileLengthNative(NULL, &(file->native), pLength);
	case MMAPI_OPEN_MODE_MEMORY:
		return EAS_HWFileLengthMemory(NULL, &(file->memory), pLength);
	case MMAPI_OPEN_MODE_STREAM:
		return EAS_HWFileLengthStream(NULL, file->stream, pLength);
	}
	return EAS_ERROR_HANDLE_INTEGRITY;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWDupHandle (master)
 *
 * Duplicate a file handle
 *
 * Find a suitable duplicate entry in the array of EAS_HW_FILE_MIXED entries,
 * then dispatch to the respective {Native|Memory|Stream} implementation.
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWDupHandle (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file,
							EAS_FILE_HANDLE *pDupFile) {
	EAS_HW_FILE_MIXED* newFile;
	EAS_RESULT ret;
	int i;
#ifdef SONIVOX_DEBUG
	EAS_Report(4, "> EAS_HWDupHandle(instData=%p, file=%p)\n",
		hwInstData, file);
#endif

	/* initialize the return handle with NULL */
	(*pDupFile) = NULL;

	/* find an empty entry in the file table */
	newFile = (EAS_HW_FILE_MIXED*) hwInstData;
	for (i = 0; i < EAS_MAX_FILE_HANDLES; i++)
	{
		/* is this slot being used? */
		if (!newFile->inUse)
		{
#ifdef SONIVOX_DEBUG
			EAS_Report(4, "  EAS_HWDupHandle: newFile=%p mode=%d\n",
				(void*) newFile, (int) file->mode);
#endif
			ret = EAS_ERROR_HANDLE_INTEGRITY;
			switch (file->mode) {
			case MMAPI_OPEN_MODE_NATIVE:
				ret = EAS_HWDupHandleNative(NULL, &(file->native), &(newFile->native));
				break;
			case MMAPI_OPEN_MODE_MEMORY:
				ret = EAS_HWDupHandleMemory(NULL, &(file->memory), &(newFile->memory));
				break;
			case MMAPI_OPEN_MODE_STREAM:
				ret = EAS_HWDupHandleStream(NULL, file->stream, newFile->stream);
				break;
			}
			if (ret == EAS_SUCCESS) {
				newFile->mode = file->mode;
				newFile->inUse = EAS_TRUE;
				(*pDupFile) = newFile;
			}
#ifdef SONIVOX_DEBUG
			EAS_Report(4, "< EAS_HWDupHandle, pDupFile=%p, ret=%d\n",
				(void*) (*pDupFile), (int) ret);
#endif
			return ret;
		}
		newFile++;
	}
#ifdef SONIVOX_DEBUG
	EAS_Report(4, "< EAS_HWDupHandle, ERROR: too many files open\n");
#endif
	return EAS_ERROR_MAX_FILES_OPEN;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWClose (master)
 *
 * Wrapper for fclose function
 *
 * Dispatch to the respective {Native|Memory|Stream} implementations.
 * For finding duplicates, the individual implementation functions need
 * to access the EAS_HW_FILE_MIXED array, therefore the hwInstData pointer
 * is provided to them.
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWCloseFile (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file) {
	EAS_RESULT ret = EAS_ERROR_HANDLE_INTEGRITY;

#ifdef SONIVOX_DEBUG
	EAS_Report(4, "> EAS_HWCloseFile(instData=%p, file=%p)\n",
		hwInstData, file);
	/* work around a problem in EAS 3.3 */
	if (file == NULL) {
		return EAS_SUCCESS;
	}
#endif
	switch (file->mode) {
	case MMAPI_OPEN_MODE_NATIVE:
		ret = EAS_HWCloseFileNative(hwInstData, &(file->native));
		break;
	case MMAPI_OPEN_MODE_MEMORY:
		ret = EAS_HWCloseFileMemory(hwInstData, &(file->memory));
		break;
	case MMAPI_OPEN_MODE_STREAM:
		ret = EAS_HWCloseFileStream(hwInstData, file->stream);
		break;
	}
	if (ret == EAS_SUCCESS) {
		/* unset the inUse flag, make it available for the OpenFile function */
		file->inUse = EAS_FALSE;
	}
#ifdef MMAPI_DEBUG_WRITE_STREAM_DUMP
	 fclose(STREAM_DUMP_HANDLE);
#endif
#ifdef SONIVOX_DEBUG
	EAS_Report(4, "< EAS_HWCloseFile, ret=%d\n", ret);
#endif
	return ret;
}


/*----------------------------------------------------------------------------
 *
 * Other host functions
 * ---------------------
 *
 *----------------------------------------------------------------------------
*/

/*----------------------------------------------------------------------------
 *
 * EAS_HWVibrate
 *
 * Turn on/off vibrate function
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWVibrate(EAS_HW_DATA_HANDLE hwInstData, EAS_BOOL state)
{
#ifdef SONIVOX_DEBUG
	EAS_ReportX(_EAS_SEVERITY_NOFILTER, "Vibrate state: %d\n", state);
#endif
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
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWLED(EAS_HW_DATA_HANDLE hwInstData, EAS_BOOL state)
{
#ifdef SONIVOX_DEBUG
	EAS_ReportX(_EAS_SEVERITY_NOFILTER, "LED state: %d\n", state);
#endif
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
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWBackLight(EAS_HW_DATA_HANDLE hwInstData, EAS_BOOL state)
{
#ifdef SONIVOX_DEBUG
	EAS_ReportX(_EAS_SEVERITY_NOFILTER, "Backlight state: %d\n", state);
#endif
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
/*lint -e{715} hwInstData available for customer use */
EAS_BOOL EAS_HWYield (EAS_HW_DATA_HANDLE hwInstData)
{
	/* we cannot jump back to Java to issue a Java yield(),
	 * so we must return FALSE here to let EAS return from
	 * the EAS_Render() method and let Java do a yield(). */
	return EAS_FALSE;
}


/*
 * Additional MMAPI host functions
 */

/*----------------------------------------------------------------------------
 *
 * EAS_HWReadFileNative
 *
 * Write data to a file
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT EAS_HWWriteFileNative(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_NATIVE file,
								 void *pBuffer, EAS_I32 n, EAS_I32 *pBytesWritten)
{
	/* write data from the buffer */
	*pBytesWritten = (EAS_I32) fwrite(pBuffer, 1, (size_t) n, file->pFile);
	file->filePos += *pBytesWritten;

	return EAS_SUCCESS;
}


/*----------------------------------------------------------------------------
 *
 * MMAPI_HWWriteFileImpl
 *
 * Write data to a memory file or stream. In lack of EAS_HWWriteFile,
 * this function is called directly from eas_mmapi.c.
 *
 * For MEMORY mode, data is appended to the media buffer. If the media
 * buffer is too small, it is enlarged before.
 *
 * For STREAM mode, data is written to the circular buffer, without
 * overwriting the read position. If the circular buffer is NULL,
 * it is allocated with a size of mb->bufferSize.
 *
 * If buffer is NULL, silence is written to the buffer.
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT MMAPI_HWWriteFileImpl(MMAPI_OPEN_MODE mode,
								 MMAPI_MediaBuffer* mb,
								 EAS_U8* buffer, EAS_I32 count,
								 EAS_I32 *pBytesWritten) {
	EAS_I32 effSize;
	EAS_U8* newBuffer = NULL;
	EAS_RESULT res = EAS_SUCCESS;
	EAS_I32 i;
	EAS_I32 thisCount;

	#ifdef SONIVOX_DEBUG_IO
	EAS_Report(5, "MMAPI_HWWriteFileImpl: mode=%d count=%d\n", (int) mode, (int) count);
	#endif

	*pBytesWritten = 0;

	if (mode != MMAPI_OPEN_MODE_STREAM && mode != MMAPI_OPEN_MODE_MEMORY) {
		return EAS_ERROR_INVALID_PARAMETER;
	}

	/*
	 * for MEMORY mode, need to (re-)allocate buffer.
	 * for STREAM mode, only allocate the buffer if not already done.
	 */
	if (mode == MMAPI_OPEN_MODE_MEMORY
		|| ((mode == MMAPI_OPEN_MODE_STREAM) && (!mb->buffer))) {

		/* allocate buffer, if not already done */
		if (mb->totalSize < 0) {
			if (mode == MMAPI_OPEN_MODE_STREAM) {
				/* STREAM mode: if total size is not known, set circular buffer size */
				effSize = mb->bufferSize;
			} else {
				/* MEMORY mode: may need to re-allocate to new size */
				effSize = mb->writePos + count;
			}
		} else {
			/* if the total size of the file is known in advance, then set the
			 * buffer size to this size. */
			effSize = mb->totalSize;
			if ((mode == MMAPI_OPEN_MODE_STREAM)
					&& (effSize > mb->bufferSize)) {
				/* for STREAM mode, limit to circular buffer size */
				effSize = mb->bufferSize;
			}
		}
		if (mb->bufferSize < effSize || mb->buffer == NULL) {
			/* need to allocate new buffer */
			#ifdef SONIVOX_DEBUG_IO
			EAS_Report(5, "MMAPI_HWWriteFileImpl: totalSize=%d, bufferSize=%d. Allocating buffer with %d bytes\n",
				mb->totalSize, mb->bufferSize, effSize);
			#endif
			newBuffer = (EAS_U8*) EAS_HWMalloc(NULL, effSize);
			if (newBuffer == NULL) {
				#ifdef SONIVOX_DEBUG
				EAS_Report(1, "MMAPI_HWWriteFileImpl: ERROR out of memory.\n");
				#endif
				return EAS_ERROR_MALLOC_FAILED;
			}
			/* if re-allocating, need to copy over previous data */
			if (mb->buffer != NULL) {
				/*
				 * TODO: for systems where malloc() is expensive, better to use
				 * some kind of linked list, rather than re-allocating
				 * the memory block with every additional call to WriteBuffer
				 */
				EAS_HWMemCpy(newBuffer, mb->buffer, mb->bufferFilled);
				/* free old buffer */
				EAS_HWFree(NULL, mb->buffer);
				mb->buffer = NULL;
			}
			/* from now on, use the new buffer */
			mb->buffer = newBuffer;
			mb->bufferSize = effSize;
		}
	}

	/* now copy over new data */
	/* at maximum, do it in 2 iterations to account for wrap-around */
	for (i = 0; i < 2; i++) {
		newBuffer = mb->buffer;
		newBuffer += mb->writePos;
		thisCount = count;

		/* need to account for wrap-around at buffer boundary */
		if (thisCount + mb->writePos > mb->bufferSize) {
			thisCount = mb->bufferSize - mb->writePos;
		}
		/* cannot fill more than the size of the buffer */
		if (thisCount + mb->bufferFilled > mb->bufferSize) {
			thisCount = mb->bufferSize - mb->bufferFilled;
		}

		#ifdef SONIVOX_DEBUG_IO
		EAS_Report(5, "MMAPI_HWWriteFileImpl: Copy %d bytes at offset %d. Buffer filled=%d, size=%d\n",
			(int) thisCount, (int) mb->writePos, (int) mb->bufferFilled, (int) mb->bufferSize);
		#endif
		if (thisCount > 0) {
			if (buffer != NULL) {
				EAS_HWMemCpy(newBuffer, buffer, thisCount);
				buffer += thisCount;
			} else {
				/* FIXME: for 8-bit unsigned data, 0x80 is silence! */
				EAS_HWMemSet(newBuffer, 0, thisCount);
			}
			count -= thisCount;
			(*pBytesWritten) += thisCount;
			mb->bufferFilled += thisCount;
			mb->writePos += thisCount;
			/* wrap around ? */
			if ((mode == MMAPI_OPEN_MODE_STREAM) && (mb->writePos >= mb->bufferSize)) {
				mb->writePos -= mb->bufferSize;
			} else {
				/* no need for second iteration if no wrap around */
				break;
			}
		}
	}
	return res;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWWriteFileMemory
 *
 * Write data to a memory file. The read pointer is advanced along
 * with the write pointer. This operation may increase the file size.
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} some hosts may not use hwInstData */
EAS_RESULT EAS_HWWriteFileMemory(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_MEMORY file, void *pBuffer, EAS_I32 n, EAS_I32 *pBytesWritten)
{
	EAS_RESULT res;

	file->mb->writePos = file->filePos;
	res = MMAPI_HWWriteFileImpl(MMAPI_OPEN_MODE_MEMORY, file->mb, (EAS_U8*) pBuffer, n, pBytesWritten);

	file->filePos = file->mb->writePos;
	file->fileSize = file->mb->bufferFilled;

	return res;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWWriteFileStream
 *
 * Write data to the circular buffer. The read pointer is not moved.
 * It is only written as much data as fits into the circular buffer
 * without overwriting the read position.
 * If the buffer is not allocated prior to calling this function,
 * it is allocated with a size of mediaBuffer->bufferSize.
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} some hosts may not use hwInstData */
EAS_RESULT EAS_HWWriteFileStream(EAS_HW_DATA_HANDLE hwInstData,
								 EAS_FILE_STREAM mb,
								 void *pBuffer, EAS_I32 n,
								 EAS_I32 *pBytesWritten) {
	return MMAPI_HWWriteFileImpl(MMAPI_OPEN_MODE_STREAM,
		mb, (EAS_U8*) pBuffer, n, pBytesWritten);

}


/*----------------------------------------------------------------------------
 *
 * MMAPI_HWWriteFile (master)
 *
 * Write data to a file
 *
 * Dispatch to the respective {Native|Memory|Stream} implementations.
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} hwInstData available for customer use */
EAS_RESULT MMAPI_HWWriteFile(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file,
						     void *pBuffer, EAS_I32 n, EAS_I32 *pBytesWritten) {
#ifdef SONIVOX_DEBUG_IO
	EAS_Report(5, "EAS_HWWriteFile(instData=%p, file=%p, count=%d), MMAPI_open_mode=%d\n",
		hwInstData, file, n, file->mode);
#endif
	switch (file->mode) {
	case MMAPI_OPEN_MODE_NATIVE:
		return EAS_HWWriteFileNative(NULL, &(file->native), pBuffer, n, pBytesWritten);
	case MMAPI_OPEN_MODE_MEMORY:
		return EAS_HWWriteFileMemory(NULL, &(file->memory), pBuffer, n, pBytesWritten);
	case MMAPI_OPEN_MODE_STREAM:
		return EAS_HWWriteFileStream(NULL, file->stream, pBuffer, n, pBytesWritten);
	}
	return EAS_ERROR_HANDLE_INTEGRITY;

}


/*----------------------------------------------------------------------------
 * MMAPI_HWRecordBuffer
 *
 * Internal function: for every call to EAS_HWReadFile, call this function
 * with the just read data.
 *----------------------------------------------------------------------------
*/
EAS_RESULT MMAPI_HWRecordBuffer(EAS_HW_DATA_HANDLE hwInstData, EAS_HW_FILE_MIXED* file,
								EAS_U8* buffer, EAS_I32 count) {
	MMAPI_RecordingData* rec = file->record;
	EAS_I32 pos = 0;
	EAS_I32 bytesInHeader;
	EAS_RESULT res;

	/* discard header data:
	 * (pos - count) is the position where the recorded data starts.
	 * if (pos - count) is in the header, decrease count */
	res = EAS_HWFilePos(hwInstData, (EAS_FILE_HANDLE) file, &pos);
	if (res == EAS_SUCCESS) {
		bytesInHeader = rec->waveHeaderSize - (pos - count);
		if (bytesInHeader > 0) {
			buffer += bytesInHeader;
			count -= bytesInHeader;
		}
		if (count > 0) {
			pos = 0;
			res = MMAPI_HWWriteFile(hwInstData, rec->handle, buffer, count, &pos);
			#ifdef SONIVOX_DEBUG
			if (res == EAS_SUCCESS && pos < count) {
				EAS_Report(_EAS_SEVERITY_ERROR,
					"MMAPI_HWRecordBuffer: ERROR: buffer overflow, could only write %d bytes instead of %d!\n",
					(int) pos, (int) count);
			}
			#endif
		}
	}
	#ifdef SONIVOX_DEBUG
	if (res != EAS_SUCCESS) {
		EAS_Report(_EAS_SEVERITY_ERROR, "MMAPI_HWRecordBuffer: ERROR: error code=%d\n", (int) res);
	}
	#endif
	return res;
}


#endif /* !MMAPI_USE_ORIGINAL_HOST */
