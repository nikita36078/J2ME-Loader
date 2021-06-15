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
#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
Java_com_sonivox_mmapi_EAS_nInit(JNIEnv *env, jclass clazz) {
    MMAPI_DATA_HANDLE easHandle = MMAPI_Init();
    return (jint) easHandle;
}

JNIEXPORT void JNICALL
Java_com_sonivox_mmapi_EAS_nShutdown(JNIEnv *env, jclass clazz, jint eas_handle) {
    MMAPI_Shutdown((MMAPI_DATA_HANDLE) eas_handle);
}

JNIEXPORT jint JNICALL
Java_com_sonivox_mmapi_EAS_nOpenFile(JNIEnv *env, jclass clazz, jint eas_handle, jstring locator,
                                     jint mode) {
    const char *sLocator = NULL;
    if (locator) {
        sLocator = env->GetStringUTFChars(locator, NULL);
    }

    jint res = reinterpret_cast<jint>(MMAPI_OpenFile((MMAPI_DATA_HANDLE) eas_handle,
                                                     (EAS_CHAR *) sLocator,
                                                     (MMAPI_OPEN_MODE) mode));

    if (sLocator) {
        env->ReleaseStringUTFChars(locator, sLocator);
    }
    return res;
}

JNIEXPORT void JNICALL
Java_com_sonivox_mmapi_EAS_nCloseFile(JNIEnv *env, jclass clazz, jint eas_handle,
                                      jint file_handle) {
    MMAPI_CloseFile((MMAPI_DATA_HANDLE) eas_handle, (MMAPI_FILE_HANDLE) file_handle);
}

JNIEXPORT jint JNICALL
Java_com_sonivox_mmapi_EAS_nWrite(JNIEnv *env, jclass clazz, jint eas_handle, jint file_handle,
                                  jbyteArray buffer, jint offset, jint count, jint total_length,
                                  jint flags) {
    EAS_U8 *buf = (EAS_U8 *)(env->GetByteArrayElements(buffer, NULL));
    jint res = MMAPI_WriteBuffer((MMAPI_DATA_HANDLE) eas_handle, 
                                 (MMAPI_FILE_HANDLE) file_handle, 
                                 (EAS_U8*) (buf),
                             offset, count, total_length, flags);
    env->ReleaseByteArrayElements(buffer, (jbyte*)buf, JNI_ABORT);
    return res;
}

JNIEXPORT jint JNICALL
Java_com_sonivox_mmapi_EAS_nGeneral(JNIEnv *env, jclass clazz, jint eas_handle, jint file_handle,
                                    jint command_code, jint param) {
    return MMAPI_GeneralCommand((MMAPI_DATA_HANDLE) eas_handle, (MMAPI_FILE_HANDLE) file_handle,
                                (MMAPI_COMMAND_CODE) command_code, param);
}

JNIEXPORT jboolean JNICALL
Java_com_sonivox_mmapi_EAS_nPrepare(JNIEnv *env, jclass clazz, jint eas_handle, jint file_handle) {
    return MMAPI_Prepare((MMAPI_DATA_HANDLE) eas_handle, (MMAPI_FILE_HANDLE) file_handle);
}

JNIEXPORT jboolean JNICALL
Java_com_sonivox_mmapi_EAS_nResume(JNIEnv *env, jclass clazz, jint eas_handle, jint file_handle) {
    return MMAPI_Resume((MMAPI_DATA_HANDLE) eas_handle, (MMAPI_FILE_HANDLE) file_handle);
}

JNIEXPORT jboolean JNICALL
Java_com_sonivox_mmapi_EAS_nPause(JNIEnv *env, jclass clazz, jint eas_handle, jint file_handle) {
    return MMAPI_Pause((MMAPI_DATA_HANDLE) eas_handle, (MMAPI_FILE_HANDLE) file_handle);
}

JNIEXPORT jboolean JNICALL
Java_com_sonivox_mmapi_EAS_nRender(JNIEnv *env, jclass clazz, jint eas_handle) {
    return MMAPI_Render((MMAPI_DATA_HANDLE) eas_handle);
}

JNIEXPORT jint JNICALL
Java_com_sonivox_mmapi_EAS_nGetState(JNIEnv *env, jclass clazz, jint eas_handle, jint file_handle) {
    return MMAPI_GetState((MMAPI_DATA_HANDLE) eas_handle, (MMAPI_FILE_HANDLE) file_handle);
}

JNIEXPORT jint JNICALL
Java_com_sonivox_mmapi_EAS_nGetLocation(JNIEnv *env, jclass clazz, jint eas_handle,
                                        jint file_handle) {
    return MMAPI_GetLocation((MMAPI_DATA_HANDLE) eas_handle, (MMAPI_FILE_HANDLE) file_handle);
}

JNIEXPORT jint JNICALL
Java_com_sonivox_mmapi_EAS_nLocate(JNIEnv *env, jclass clazz, jint eas_handle, jint file_handle,
                                   jint time) {
    return MMAPI_Locate((MMAPI_DATA_HANDLE) eas_handle, (MMAPI_FILE_HANDLE) file_handle, time);
}

JNIEXPORT jboolean JNICALL
Java_com_sonivox_mmapi_EAS_nSetVolume(JNIEnv *env, jclass clazz, jint eas_handle, jint file_handle,
                                      jint level) {
    return MMAPI_GetVolume((MMAPI_DATA_HANDLE) eas_handle, (MMAPI_FILE_HANDLE) file_handle);
}

JNIEXPORT jint JNICALL
Java_com_sonivox_mmapi_EAS_nGetVolume(JNIEnv *env, jclass clazz, jint eas_handle,
                                      jint file_handle) {
    return MMAPI_GetVolume((MMAPI_DATA_HANDLE) eas_handle, (MMAPI_FILE_HANDLE) file_handle);
}

JNIEXPORT jboolean JNICALL
Java_com_sonivox_mmapi_EAS_nSetRepeat(JNIEnv *env, jclass clazz, jint eas_handle, jint file_handle,
                                      jint repeat_count) {
    return MMAPI_SetRepeat((MMAPI_DATA_HANDLE) eas_handle, (MMAPI_FILE_HANDLE) file_handle, repeat_count);
}

JNIEXPORT jint JNICALL
Java_com_sonivox_mmapi_EAS_nGetCurrentRepeat(JNIEnv *env, jclass clazz, jint eas_handle,
                                             jint file_handle) {
    return MMAPI_GetCurrentRepeat((MMAPI_DATA_HANDLE) eas_handle, (MMAPI_FILE_HANDLE) file_handle);
}

JNIEXPORT jint JNICALL
Java_com_sonivox_mmapi_EAS_nSetPlaybackRate(JNIEnv *env, jclass clazz, jint eas_handle,
                                            jint file_handle, jint rate) {
    return MMAPI_SetPlaybackRate((MMAPI_DATA_HANDLE) eas_handle, (MMAPI_FILE_HANDLE) file_handle, rate);
}

JNIEXPORT jint JNICALL
Java_com_sonivox_mmapi_EAS_nSetTransposition(JNIEnv *env, jclass clazz, jint eas_handle,
                                             jint file_handle, jint transposition) {
    return MMAPI_SetTransposition((MMAPI_DATA_HANDLE) eas_handle, (MMAPI_FILE_HANDLE) file_handle, transposition);
}

JNIEXPORT jint JNICALL
Java_com_sonivox_mmapi_EAS_nGetNextMetaDataType(JNIEnv *env, jclass clazz, jint eas_handle,
                                                jint file_handle) {
    // TODO: implement nGetNextMetaDataType()
}

JNIEXPORT jstring JNICALL
Java_com_sonivox_mmapi_EAS_nGetNextMetaDataValue(JNIEnv *env, jclass clazz, jint eas_handle,
                                                 jint file_handle) {
    // TODO: implement nGetNextMetaDataValue()
}

JNIEXPORT jint JNICALL
Java_com_sonivox_mmapi_EAS_nGetDuration(JNIEnv *env, jclass clazz, jint eas_handle,
                                        jint file_handle) {
    return MMAPI_GetDuration((MMAPI_DATA_HANDLE) eas_handle, (MMAPI_FILE_HANDLE) file_handle);
}

JNIEXPORT jboolean JNICALL
Java_com_sonivox_mmapi_EAS_nOpenRecording(JNIEnv *env, jclass clazz, jint eas_handle,
                                          jint file_handle, jstring locator) {
    return EAS_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_sonivox_mmapi_EAS_nReadRecordedBytes(JNIEnv *env, jclass clazz, jint eas_handle,
                                              jint file_handle, jbyteArray buffer, jint offset,
                                              jint count) {
    return 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sonivox_mmapi_EAS_nOpenCapture(JNIEnv *env, jclass clazz, jint eas_handle,
                                        jint file_handle, jint encoding, jint rate, jint bits,
                                        jint channels, jboolean big_endian, jboolean is_signed) {
    return EAS_FALSE;
}

#ifdef __cplusplus
}
#endif
