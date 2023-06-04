/*----------------------------------------------------------------------------
 *
 * File:
 * eas_mmapi_windows.c
 *
 * Contents and purpose:
 * Windows specific: implementation of functions that are hardware/OS 
 * dependent, in particular all audio device I/O.
 *
 * This implementation is for testing purposes only.
 *
 * For function documentation, see eas_mmapi.h.
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
#include "../sonivox/host_src/eas.h"
#include "../sonivox/host_src/eas_host.h"

#ifdef MMAPI_DEBUG_WAVEFILE
#include <stdio.h>
#include <eas_wave.h>
 #ifndef MMAPI_DEBUG_HAS_AUDIO_DEV
 /* for timeGetTime() */
 #include <windows.h>
 #include <mmsystem.h>
 #endif
#endif
#ifdef MMAPI_DEBUG_HAS_AUDIO_DEV
#include "eas_waveout.h"
#endif

typedef struct {
#ifdef MMAPI_DEBUG_WAVEFILE
	WAVE_FILE* wf;
#ifndef MMAPI_DEBUG_HAS_AUDIO_DEV
	EAS_U32 startTime;
	double writtenMillis;
#endif
#endif
#ifdef MMAPI_DEBUG_HAS_AUDIO_DEV
	/* the wave audio device */
	EAS_HANDLE ad;
#endif
} MMAPI_WIN_STRUCT, *MMAPI_WIN_HANDLE;


/* MMAPI_HWOutputCreate */
MMAPI_OUTPUT_HANDLE MMAPI_HWOutputCreate() {
	const S_EAS_LIB_CONFIG* pConfig = EAS_Config();
	MMAPI_WIN_HANDLE oHandle = (MMAPI_WIN_HANDLE) EAS_HWMalloc(NULL, sizeof(MMAPI_WIN_STRUCT));

	if (oHandle != NULL) {
		EAS_HWMemSet(oHandle, 0, sizeof(MMAPI_WIN_STRUCT));
#ifdef MMAPI_DEBUG_WAVEFILE
		oHandle->wf = WaveFileCreate(MMAPI_DEBUG_WAVEFILE_NAME, 
			pConfig->numChannels, pConfig->sampleRate, sizeof(EAS_PCM) * 8);
		if (oHandle->wf == NULL) {
			/* ERROR: fail? */
		}
#ifndef MMAPI_DEBUG_HAS_AUDIO_DEV
		oHandle->startTime = timeGetTime();
#endif
#endif
#ifdef MMAPI_DEBUG_HAS_AUDIO_DEV
	oHandle->ad = OpenWaveDevice((EAS_U32) -1, pConfig->numChannels, 
		pConfig->sampleRate, sizeof(EAS_PCM) * 8);
#endif
	}
	return (MMAPI_OUTPUT_HANDLE) oHandle;
}


/* MMAPI_HWOutputDestroy */
void MMAPI_HWOutputDestroy(MMAPI_OUTPUT_HANDLE handle) {
	if (handle != NULL) {
#ifdef MMAPI_DEBUG_WAVEFILE
		if (((MMAPI_WIN_HANDLE) handle)->wf) {
			WaveFileClose(((MMAPI_WIN_HANDLE) handle)->wf);
			((MMAPI_WIN_HANDLE) handle)->wf = NULL;
		}
#endif
#ifdef MMAPI_DEBUG_HAS_AUDIO_DEV
		CloseWaveDevice(((MMAPI_WIN_HANDLE) handle)->ad);
#endif
		EAS_HWFree(NULL, handle);
	}
}


/* MMAPI_HWOutput. count is in samples (not bytes) */
EAS_I32 MMAPI_HWOutput(MMAPI_OUTPUT_HANDLE handle, EAS_PCM* samples, EAS_I32 count) {
	const S_EAS_LIB_CONFIG* pConfig = EAS_Config();
    MMAPI_WIN_STRUCT* mwh = (MMAPI_WIN_STRUCT*) handle;

	if (mwh != NULL) {
#ifdef MMAPI_DEBUG_HAS_AUDIO_DEV
		if (mwh->ad) {
			if (!OutputWaveDevice(mwh->ad, samples, count * pConfig->numChannels * sizeof(EAS_PCM))) {
				/* signal to repeat this call to MMAPI_HWOutput */
				count = 0;
			}
		}
#endif

#ifdef MMAPI_DEBUG_WAVEFILE
		if (mwh->wf) {
#ifndef MMAPI_DEBUG_HAS_AUDIO_DEV
			/* simulate real-time behavior */
			double toBeWrittenMillis = mwh->writtenMillis 
				+ (((double) count) * 1000.0) / ((double) pConfig->sampleRate);
			EAS_U32 elapsedMillis = timeGetTime() - mwh->startTime;
			if (elapsedMillis <= toBeWrittenMillis) {
				/* if the time has not elapsed yet, don't write the buffer 
				 * yet to the file */
				count = 0;
			} else {
				mwh->writtenMillis = toBeWrittenMillis;
			}
#endif /* !MMAPI_DEBUG_HAS_AUDIO_DEV */
			if (count > 0) {
				WaveFileWrite(((MMAPI_WIN_HANDLE) handle)->wf, samples, 
					count * pConfig->numChannels * sizeof(EAS_PCM));
			}
		}
#endif /* MMAPI_DEBUG_WAVEFILE */
	}
	return count;
}
