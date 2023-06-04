/*----------------------------------------------------------------------------
 *
 * File:
 * eas_mmapi_kvm.c
 *
 * Contents and purpose:
 * This file contains the KNI (KVM Native Interface) functions
 * for MMAPI's native methods. For each native method, the respective
 * function in eas_mmapi.c is called.
 *
 * This file is meant to separate Java-specific source. This
 * separation allows easy porting to other types of native interface,
 * like JNI. Replace with a different implementation if a different
 * native interface is used.
 *
 * For function documentation, see the respective Java files in
 * package com.sonivox.mmapi (especially EAS.java).
 *
 * Copyright 2006 Sonic Network Inc.
 *
 *----------------------------------------------------------------------------
 * Revision Control:
 *   $Revision: 560 $
 *   $Date: 2007-02-02 14:34:18 -0800 (Fri, 02 Feb 2007) $
 *----------------------------------------------------------------------------
*/

#include "eas_mmapi_config.h"
#include "eas_mmapi_types.h"
#include "eas_mmapi.h"

/* KNI declarations */
#include <kni.h>

/* static int nInit(); */
void Java_com_sonivox_mmapi_EAS_nInit() {
	MMAPI_DATA_HANDLE easHandle = MMAPI_Init();
	pushStack((EAS_I32) easHandle);
}

/* static void nShutdown(int easHandle); */
void Java_com_sonivox_mmapi_EAS_nShutdown() {
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	MMAPI_Shutdown(easHandle);
}

#define MAX_LOCATOR_SIZE 400

/* static int nOpenFile(int easHandle, String locator, int mode); */
void Java_com_sonivox_mmapi_EAS_nOpenFile() {
	MMAPI_OPEN_MODE mode = popStack();
    STRING_INSTANCE locator = popStackAsType(STRING_INSTANCE);
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
    EAS_CHAR mLocator[MAX_LOCATOR_SIZE+1];
    EAS_CHAR* sLocator = NULL;
	MMAPI_FILE_HANDLE fileHandle = NULL;
	
	if (locator != NULL) {
		sLocator = getStringContentsSafely(locator, mLocator, MAX_LOCATOR_SIZE);
	}
	fileHandle = MMAPI_OpenFile(easHandle, sLocator, mode);

	pushStack((EAS_I32) fileHandle);
}

/* static void nCloseFile(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nCloseFile() {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	MMAPI_CloseFile(easHandle, fileHandle);
}

/* static int nWrite(int easHandle, int fileHandle, byte[] buffer, 
			int offset, int count, int totalLength, int flags); */
void Java_com_sonivox_mmapi_EAS_nWrite() {
	EAS_I32 flags = (EAS_BOOL) popStack();
	EAS_I32 totalLength = (EAS_I32) popStack();
	EAS_I32 count = (EAS_I32) popStack();
	EAS_I32 offset = (EAS_I32) popStack();
    BYTEARRAY buffer = popStackAsType(BYTEARRAY);
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_I32 result;

	result = MMAPI_WriteBuffer(easHandle, fileHandle, (EAS_U8*) (buffer->bdata), 
		offset, count, totalLength, flags);
	pushStack(result);
}


/* static int nGeneral(int easHandle, int fileHandle, int commandCode, int param); */
void Java_com_sonivox_mmapi_EAS_nGeneral() {
	EAS_I32 param = (EAS_I32) popStack();
	MMAPI_COMMAND_CODE commandCode = (MMAPI_COMMAND_CODE) popStack();
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_I32 result = MMAPI_GeneralCommand(easHandle, fileHandle, commandCode, param);
	pushStack(result);
}

/* static boolean nPrepare(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nPrepare() {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_BOOL result = MMAPI_Prepare(easHandle, fileHandle);
	pushStack(result);
}


/* static boolean nResume(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nResume() {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_BOOL result = MMAPI_Resume(easHandle, fileHandle);
	pushStack(result);
}

/* static boolean nPause(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nPause() {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_BOOL result = MMAPI_Pause(easHandle, fileHandle);
	pushStack(result);
}

/* static boolean nRender(int easHandle); */
void Java_com_sonivox_mmapi_EAS_nRender() {
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_BOOL result = MMAPI_Render(easHandle);
	pushStack(result);
}

/* static int nGetState(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nGetState() {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_I32 result = MMAPI_GetState(easHandle, fileHandle);
	pushStack(result);
}


/* static int nGetLocation(int easHandle, int fileHandle) */
void Java_com_sonivox_mmapi_EAS_nGetLocation() {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_I32 result = MMAPI_GetLocation(easHandle, fileHandle);
	pushStack(result);
}


/* static int nLocate(int easHandle, int fileHandle, int time); */
void Java_com_sonivox_mmapi_EAS_nLocate() {
	EAS_I32 millis = (EAS_I32) popStack();
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_I32 result = MMAPI_Locate(easHandle, fileHandle, millis);
	pushStack(result);
}


/* static boolean nSetVolume(int easHandle, int fileHandle,	int level); */
void Java_com_sonivox_mmapi_EAS_nSetVolume() {
	EAS_I32 level = (EAS_I32) popStack();
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_BOOL result = MMAPI_SetVolume(easHandle, fileHandle, level);
	pushStack(result);
}


/* static int nGetVolume(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nGetVolume() {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_I32 result = MMAPI_GetVolume(easHandle, fileHandle);
	pushStack(result);
}


/* static boolean nSetRepeat(int easHandle, int fileHandle, int repeatCount); */
void Java_com_sonivox_mmapi_EAS_nSetRepeat() {
	EAS_I32 repeatCount = (EAS_I32) popStack();
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_BOOL result = MMAPI_SetRepeat(easHandle, fileHandle, repeatCount);
	pushStack(result);
}


/* static int nGetCurrentRepeat(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nGetCurrentRepeat() {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_I32 result = MMAPI_GetCurrentRepeat(easHandle, fileHandle);
	pushStack(result);
}


/* static int nSetPlaybackRate(int easHandle, int fileHandle, int rate); */
void Java_com_sonivox_mmapi_EAS_nSetPlaybackRate() {
	EAS_I32 rate = (EAS_I32) popStack();
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_I32 result = MMAPI_SetPlaybackRate(easHandle, fileHandle, rate);
	pushStack(result);
}


/* static int nSetTransposition(int easHandle, int fileHandle, int transposition); */
void Java_com_sonivox_mmapi_EAS_nSetTransposition() {
	EAS_I32 transpose = (EAS_I32) popStack();
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_I32 result = MMAPI_SetTransposition(easHandle, fileHandle, transpose);
	pushStack(result);
}

/* static int nGetNextMetaDataType(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nGetNextMetaDataType() {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_I32 result = -1;
	MMAPI_MetaData* md = MMAPI_GetMetaData(easHandle, fileHandle);
	if (md != NULL) {
		result = (EAS_I32) md->type;
	}
	pushStack(result);
}

/* static String nGetNextMetaDataValue(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nGetNextMetaDataValue() {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
    KNI_StartHandles(1);
    KNI_DeclareHandle(string);

	EAS_I32 result = -1;
	MMAPI_MetaData* md = MMAPI_GetMetaData(easHandle, fileHandle);
	if (md != NULL) {
        KNI_NewStringUTF(md->value, string);
		MMAPI_NextMetaData(easHandle, fileHandle);
    } else {
        KNI_ReleaseHandle(string);
    }
    KNI_EndHandlesAndReturnObject(string);
}


/* static int nGetDuration(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nGetDuration() {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_I32 result = MMAPI_GetDuration(easHandle, fileHandle);
	pushStack(result);
}

/* static boolean nOpenRecording(int easHandle, int fileHandle, String locator); */
void Java_com_sonivox_mmapi_EAS_nOpenRecording() {
    STRING_INSTANCE locator = popStackAsType(STRING_INSTANCE);
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
    EAS_CHAR mLocator[MAX_LOCATOR_SIZE+1];
    EAS_CHAR* sLocator = NULL;
	EAS_BOOL result;
	
	if (locator != NULL) {
		sLocator = getStringContentsSafely(locator, mLocator, MAX_LOCATOR_SIZE);
	}
	result = MMAPI_OpenRecording(easHandle, fileHandle, sLocator);
	pushStack((EAS_I32) result);
}

/* static int nReadRecordedBytes(int easHandle, int fileHandle, byte[] buffer, int offset, int count); */
void Java_com_sonivox_mmapi_EAS_nReadRecordedBytes() {
	EAS_I32 count = (EAS_I32) popStack();
	EAS_I32 offset = (EAS_I32) popStack();
    BYTEARRAY buffer = popStackAsType(BYTEARRAY);
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_I32 result;

	result = MMAPI_ReadRecordedBuffer(easHandle, fileHandle, (EAS_U8*) (buffer->bdata), 
			offset, count);
	pushStack(result);
}


/* static boolean nOpenCapture(int easHandle, int fileHandle,
		int encoding, int rate, int bits, int channels, boolean bigEndian,
		boolean isSigned); */
void Java_com_sonivox_mmapi_EAS_nOpenCapture() {
	EAS_BOOL isSigned = (EAS_BOOL) popStack();
	EAS_BOOL isBigEndian = (EAS_BOOL) popStack();
	EAS_I32 channels = (EAS_I32) popStack();
	EAS_I32 bits = (EAS_I32) popStack();
	EAS_I32 rate = (EAS_I32) popStack();
	MMAPI_CAPTURE_ENCODING encoding = (MMAPI_CAPTURE_ENCODING) popStack();
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) popStack();
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) popStack();
	EAS_BOOL result;

#ifdef MMAPI_HAS_CAPTURE
	result = MMAPI_OpenCapture(easHandle, fileHandle, encoding, rate, bits,
				channels, isBigEndian, isSigned);
#else
	result = EAS_FALSE;
#endif

	pushStack((EAS_I32) result);
}