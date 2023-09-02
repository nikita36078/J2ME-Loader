/*----------------------------------------------------------------------------
 *
 * File:
 * eas_mmapi_jni.c
 *
 * Contents and purpose:
 * This file contains the JNI functions
 * for MMAPI's native methods. For each native method, the respective
 * function in eas_mmapi.c is called.
 *
 * For function documentation, see the respective Java files in
 * package com.sonivox.mmapi (especially EAS.java).
 *
 * Copyright 2006 Sonic Network Inc.
 *
*/

#include "eas_mmapi_config.h"
#include "eas_mmapi_types.h"
#include "eas_mmapi.h"

/* JNI declarations */
#include <jni.h>


#pragma clang diagnostic push
#pragma ide diagnostic ignored "UnusedParameter"

/* static int nInit(); */
JNIEXPORT jlong JNICALL Java_com_sonivox_mmapi_EAS_nInit(JNIEnv *env, jclass clazz) {
	MMAPI_DATA_HANDLE easHandle = MMAPI_Init();
	return (intptr_t) easHandle;
}

/* static void nShutdown(int easHandle); */
JNIEXPORT void JNICALL Java_com_sonivox_mmapi_EAS_nShutdown
(JNIEnv *env, jclass clazz, jlong eas_handle) {
    MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	MMAPI_Shutdown(easHandle);
}

/* static int nOpenFile(int easHandle, String locator, int mode); */
JNIEXPORT jlong JNICALL Java_com_sonivox_mmapi_EAS_nOpenFile
(JNIEnv *env, jclass clazz, jlong eas_handle, jstring locator, jint mode) {
    MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
    const char* sLocator;
	MMAPI_FILE_HANDLE fileHandle;
	
	if (locator != NULL) {
		sLocator = (*env)->GetStringUTFChars(env, locator, NULL);
	} else {
        sLocator = NULL;
    }
    fileHandle = MMAPI_OpenFile(easHandle, sLocator, mode);
    if (locator != NULL) {
        (*env)->ReleaseStringUTFChars(env, locator, sLocator);
    }

    return (intptr_t) fileHandle;
}

/* static void nCloseFile(int easHandle, int fileHandle); */
JNIEXPORT void JNICALL Java_com_sonivox_mmapi_EAS_nCloseFile
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle) {
    MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
    MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	MMAPI_CloseFile(easHandle, fileHandle);
}

/* static int nWrite(int easHandle, int fileHandle, byte[] buffer, 
			int offset, int count, int totalLength, int flags); */
JNIEXPORT jint JNICALL Java_com_sonivox_mmapi_EAS_nWrite
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle,
 jbyteArray buffer, jint offset, jint count, jint total_length, jint flags) {
    jbyte *data = (*env)->GetByteArrayElements(env, buffer, NULL);
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
    MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	EAS_I32 result;

	result = MMAPI_WriteBuffer(easHandle, fileHandle, (EAS_U8*) (data),
		offset, count, total_length, flags);
	(*env)->ReleaseByteArrayElements(env, buffer, data, 0);
	return result;
}


/* static int nGeneral(int easHandle, int fileHandle, int commandCode, int param); */
JNIEXPORT jint JNICALL Java_com_sonivox_mmapi_EAS_nGeneral
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle, jint command_code, jint param) {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	return MMAPI_GeneralCommand(easHandle, fileHandle, command_code, param);
}

/* static boolean nPrepare(int easHandle, int fileHandle); */
JNIEXPORT jboolean JNICALL Java_com_sonivox_mmapi_EAS_nPrepare
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle) {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	EAS_BOOL result = MMAPI_Prepare(easHandle, fileHandle);
	return result;
}


/* static boolean nResume(int easHandle, int fileHandle); */
JNIEXPORT jboolean JNICALL Java_com_sonivox_mmapi_EAS_nResume
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle) {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	EAS_BOOL result = MMAPI_Resume(easHandle, fileHandle);
	return result;
}

/* static boolean nPause(int easHandle, int fileHandle); */
JNIEXPORT jboolean JNICALL Java_com_sonivox_mmapi_EAS_nPause
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle) {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	EAS_BOOL result = MMAPI_Pause(easHandle, fileHandle);
	return result;
}

/* static boolean nRender(int easHandle); */
JNIEXPORT jboolean JNICALL Java_com_sonivox_mmapi_EAS_nRender
(JNIEnv *env, jclass clazz, jlong eas_handle) {
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	EAS_BOOL result = MMAPI_Render(easHandle);
	return result;
}

/* static int nGetState(int easHandle, int fileHandle); */
JNIEXPORT jint JNICALL Java_com_sonivox_mmapi_EAS_nGetState
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle) {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	EAS_I32 result = MMAPI_GetState(easHandle, fileHandle);
	return result;
}


/* static int nGetLocation(int easHandle, int fileHandle) */
JNIEXPORT jint JNICALL Java_com_sonivox_mmapi_EAS_nGetLocation
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle) {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	EAS_I32 result = MMAPI_GetLocation(easHandle, fileHandle);
	return result;
}


/* static int nLocate(int easHandle, int fileHandle, int time); */
JNIEXPORT jint JNICALL Java_com_sonivox_mmapi_EAS_nLocate
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle, jint time) {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	EAS_I32 result = MMAPI_Locate(easHandle, fileHandle, time);
	return result;
}


/* static boolean nSetVolume(int easHandle, int fileHandle,	int level); */
JNIEXPORT jboolean JNICALL Java_com_sonivox_mmapi_EAS_nSetVolume
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle, jint level) {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	EAS_BOOL result = MMAPI_SetVolume(easHandle, fileHandle, level);
	return result;
}


/* static int nGetVolume(int easHandle, int fileHandle); */
JNIEXPORT jint JNICALL Java_com_sonivox_mmapi_EAS_nGetVolume
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle) {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	EAS_I32 result = MMAPI_GetVolume(easHandle, fileHandle);
	return result;
}


/* static boolean nSetRepeat(int easHandle, int fileHandle, int repeatCount); */
JNIEXPORT jboolean JNICALL Java_com_sonivox_mmapi_EAS_nSetRepeat
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle, jint repeat_count) {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	EAS_BOOL result = MMAPI_SetRepeat(easHandle, fileHandle, repeat_count);
	return result;
}


/* static int nGetCurrentRepeat(int easHandle, int fileHandle); */
JNIEXPORT jint JNICALL Java_com_sonivox_mmapi_EAS_nGetCurrentRepeat
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle) {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	EAS_I32 result = MMAPI_GetCurrentRepeat(easHandle, fileHandle);
	return result;
}


/* static int nSetPlaybackRate(int easHandle, int fileHandle, int rate); */
JNIEXPORT jint JNICALL Java_com_sonivox_mmapi_EAS_nSetPlaybackRate
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle, jint rate) {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	EAS_I32 result = MMAPI_SetPlaybackRate(easHandle, fileHandle, rate);
	return result;
}


/* static int nSetTransposition(int easHandle, int fileHandle, int transposition); */
JNIEXPORT jint JNICALL Java_com_sonivox_mmapi_EAS_nSetTransposition
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle, jint transposition) {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	EAS_I32 result = MMAPI_SetTransposition(easHandle, fileHandle, transposition);
	return result;
}

/* static int nGetNextMetaDataType(int easHandle, int fileHandle); */
JNIEXPORT jint JNICALL Java_com_sonivox_mmapi_EAS_nGetNextMetaDataType
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle) {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	EAS_I32 result = -1;
	MMAPI_MetaData* md = MMAPI_GetMetaData(easHandle, fileHandle);
	if (md != NULL) {
		result = (EAS_I32) md->type;
	}
	return result;
}

/* static String nGetNextMetaDataValue(int easHandle, int fileHandle); */
JNIEXPORT jstring JNICALL Java_com_sonivox_mmapi_EAS_nGetNextMetaDataValue
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle) {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
    jstring string;

	MMAPI_MetaData* md = MMAPI_GetMetaData(easHandle, fileHandle);
	if (md != NULL) {
        string = (*env)->NewStringUTF(env, md->value);
		MMAPI_NextMetaData(easHandle, fileHandle);
    } else {
        string = NULL;
    }
    return string;
}


/* static int nGetDuration(int easHandle, int fileHandle); */
JNIEXPORT jint JNICALL Java_com_sonivox_mmapi_EAS_nGetDuration
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle) {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	EAS_I32 result = MMAPI_GetDuration(easHandle, fileHandle);
	return result;
}

/* static boolean nOpenRecording(int easHandle, int fileHandle, String locator); */
JNIEXPORT jboolean JNICALL Java_com_sonivox_mmapi_EAS_nOpenRecording
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle, jstring locator) {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
    const char* sLocator;
	EAS_BOOL result;

	if (locator != NULL) {
		sLocator = (*env)->GetStringUTFChars(env, locator, NULL);
	} else {
		sLocator = NULL;
	}

	result = MMAPI_OpenRecording(easHandle, fileHandle, sLocator);

	if (locator != NULL) {
		(*env)->ReleaseStringUTFChars(env, locator, sLocator);
	}
	return (EAS_I32) result;
}

/* static int nReadRecordedBytes(int easHandle, int fileHandle, byte[] buffer, int offset, int count); */
JNIEXPORT jint JNICALL Java_com_sonivox_mmapi_EAS_nReadRecordedBytes
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle,
 jbyteArray buffer, jint offset, jint count) {
    jbyte *data = (*env)->GetByteArrayElements(env, buffer, NULL);
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;

	return MMAPI_ReadRecordedBuffer(easHandle, fileHandle, (EAS_U8 *) (data), offset, count);
}


/* static boolean nOpenCapture(int easHandle, int fileHandle,
		int encoding, int rate, int bits, int channels, boolean bigEndian,
		boolean isSigned); */
JNIEXPORT jboolean JNICALL Java_com_sonivox_mmapi_EAS_nOpenCapture
(JNIEnv *env, jclass clazz, jlong eas_handle, jlong file_handle,
 jint encoding, jint rate, jint bits, jint channels, jboolean big_endian, jboolean is_signed) {
	MMAPI_FILE_HANDLE fileHandle = (MMAPI_FILE_HANDLE) file_handle;
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	EAS_BOOL result;

#ifdef MMAPI_HAS_CAPTURE
	result = MMAPI_OpenCapture(easHandle, fileHandle, encoding,
                               rate, bits, channels, big_endian, is_signed);
#else
	result = EAS_FALSE;
#endif

	return result;
}

/* static boolean nLoadDLSCollection(long easHandle, String locator); */
JNIEXPORT jboolean JNICALL Java_com_sonivox_mmapi_EAS_nLoadDLSCollection
(JNIEnv *env, jclass clazz, jlong eas_handle, jstring locator) {
	MMAPI_DATA_HANDLE easHandle = (MMAPI_DATA_HANDLE) eas_handle;
	EAS_BOOL result;

	const char *sLocator = locator == NULL ? NULL : (*env)->GetStringUTFChars(env, locator, NULL);
	result = MMAPI_LoadDLSCollection(easHandle, sLocator);
	if (locator != NULL) {
		(*env)->ReleaseStringUTFChars(env, locator, sLocator);
	}

	return result;
}

#pragma clang diagnostic pop
