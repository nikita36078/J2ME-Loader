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

/* static int nInit(); */
void Java_com_sonivox_mmapi_EAS_nInit() {
	MMAPI_DATA_HANDLE easHandle = MMAPI_Init();
}

/* static void nShutdown(int easHandle); */
void Java_com_sonivox_mmapi_EAS_nShutdown() {

}

#define MAX_LOCATOR_SIZE 400

/* static int nOpenFile(int easHandle, String locator, int mode); */
void Java_com_sonivox_mmapi_EAS_nOpenFile() {


}

/* static void nCloseFile(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nCloseFile() {

}

/* static int nWrite(int easHandle, int fileHandle, byte[] buffer, 
			int offset, int count, int totalLength, int flags); */
void Java_com_sonivox_mmapi_EAS_nWrite() {

}


/* static int nGeneral(int easHandle, int fileHandle, int commandCode, int param); */
void Java_com_sonivox_mmapi_EAS_nGeneral() {

}

/* static boolean nPrepare(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nPrepare() {

}


/* static boolean nResume(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nResume() {

}

/* static boolean nPause(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nPause() {

}

/* static boolean nRender(int easHandle); */
void Java_com_sonivox_mmapi_EAS_nRender() {

}

/* static int nGetState(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nGetState() {
}


/* static int nGetLocation(int easHandle, int fileHandle) */
void Java_com_sonivox_mmapi_EAS_nGetLocation() {

}


/* static int nLocate(int easHandle, int fileHandle, int time); */
void Java_com_sonivox_mmapi_EAS_nLocate() {

}


/* static boolean nSetVolume(int easHandle, int fileHandle,	int level); */
void Java_com_sonivox_mmapi_EAS_nSetVolume() {

}


/* static int nGetVolume(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nGetVolume() {

}


/* static boolean nSetRepeat(int easHandle, int fileHandle, int repeatCount); */
void Java_com_sonivox_mmapi_EAS_nSetRepeat() {

}


/* static int nGetCurrentRepeat(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nGetCurrentRepeat() {

}


/* static int nSetPlaybackRate(int easHandle, int fileHandle, int rate); */
void Java_com_sonivox_mmapi_EAS_nSetPlaybackRate() {

}


/* static int nSetTransposition(int easHandle, int fileHandle, int transposition); */
void Java_com_sonivox_mmapi_EAS_nSetTransposition() {

}

/* static int nGetNextMetaDataType(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nGetNextMetaDataType() {

}

/* static String nGetNextMetaDataValue(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nGetNextMetaDataValue() {

}


/* static int nGetDuration(int easHandle, int fileHandle); */
void Java_com_sonivox_mmapi_EAS_nGetDuration() {

}

/* static boolean nOpenRecording(int easHandle, int fileHandle, String locator); */
void Java_com_sonivox_mmapi_EAS_nOpenRecording() {

}

/* static int nReadRecordedBytes(int easHandle, int fileHandle, byte[] buffer, int offset, int count); */
void Java_com_sonivox_mmapi_EAS_nReadRecordedBytes() {

}


/* static boolean nOpenCapture(int easHandle, int fileHandle,
		int encoding, int rate, int bits, int channels, boolean bigEndian,
		boolean isSigned); */
void Java_com_sonivox_mmapi_EAS_nOpenCapture() {

}