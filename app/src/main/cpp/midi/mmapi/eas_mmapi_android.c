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
#include <eas.h>
#include <eas_host.h>


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
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <log.h>
#include <pthread.h>

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

static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

// engine interfaces
static SLObjectItf engineObject = NULL;
static SLEngineItf engineEngine;

// output mix interfaces
static SLObjectItf outputMixObject = NULL;

// buffer queue player interfaces
static SLObjectItf bqPlayerObject = NULL;
static SLPlayItf bqPlayerPlay;
static SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue;

// this callback handler is called every time a buffer finishes
// playing
void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
    EAS_RESULT result;
    EAS_I32 numGenerated;
    EAS_I32 count;
}


SLresult createEngine()
{
    SLresult result;

    // create engine
    result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    if (SL_RESULT_SUCCESS != result)
        return result;

    // LOG_D(LOG_TAG, "Engine created");

    // realize the engine
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != result)
        return result;

    // LOG_D(LOG_TAG, "Engine realised");

    // get the engine interface, which is needed in order to create
    // other objects
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE,
                                           &engineEngine);
    if (SL_RESULT_SUCCESS != result)
        return result;

    // LOG_D(LOG_TAG, "Engine Interface retrieved");

    // create output mix
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject,
                                              0, NULL, NULL);
    if (SL_RESULT_SUCCESS != result)
        return result;

    // LOG_D(LOG_TAG, "Output mix created");

    // realize the output mix
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != result)
        return result;

    // LOG_D(LOG_TAG, "Output mix realised");

    return SL_RESULT_SUCCESS;
}

// create buffer queue audio player
SLresult createBufferQueueAudioPlayer(SLuint32 numChannels, SLuint32 sampleRate)
{
    SLresult result;

    // configure audio source
    SLDataLocator_AndroidSimpleBufferQueue loc_bufq =
            {
                    SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, MMAPI_AUDIODEVICE_BUFFERS
            };
    SLDataFormat_PCM format_pcm =
            {
                    SL_DATAFORMAT_PCM, numChannels, sampleRate * 1000,
                    SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
                    SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,
                    SL_BYTEORDER_LITTLEENDIAN
            };
    SLDataSource audioSrc = {&loc_bufq, &format_pcm};

    // configure audio sink
    SLDataLocator_OutputMix loc_outmix =
            {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSnk = {&loc_outmix, NULL};

    // create audio player
    const SLInterfaceID ids[1] = {SL_IID_BUFFERQUEUE};
    const SLboolean req[1] = {SL_BOOLEAN_TRUE};

    result = (*engineEngine)->CreateAudioPlayer(engineEngine,
                                                &bqPlayerObject,
                                                &audioSrc, &audioSnk,
                                                1, ids, req);
    if (SL_RESULT_SUCCESS != result)
        return result;

    // LOG_D(LOG_TAG, "Audio player created");

    // realize the player
    result = (*bqPlayerObject)->Realize(bqPlayerObject, SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != result)
        return result;

    // LOG_D(LOG_TAG, "Audio player realised");

    // get the play interface
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_PLAY,
                                             &bqPlayerPlay);
    if (SL_RESULT_SUCCESS != result)
        return result;

    // LOG_D(LOG_TAG, "Play interface retrieved");

    // get the buffer queue interface
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_BUFFERQUEUE,
                                             &bqPlayerBufferQueue);
    if (SL_RESULT_SUCCESS != result)
        return result;

    // LOG_D(LOG_TAG, "Buffer queue interface retrieved");

    // register callback on the buffer queue
    result = (*bqPlayerBufferQueue)->RegisterCallback(bqPlayerBufferQueue,
                                                      bqPlayerCallback, NULL);
    if (SL_RESULT_SUCCESS != result)
        return result;

    // LOG_D(LOG_TAG, "Callback registered");

    // set the player's state to playing
    result = (*bqPlayerPlay)->SetPlayState(bqPlayerPlay, SL_PLAYSTATE_PLAYING);
    if (SL_RESULT_SUCCESS != result)
        return result;

    // LOG_D(LOG_TAG, "Audio player set playing");

    return SL_RESULT_SUCCESS;
}

// shut down the native audio system
void shutdownAudio()
{
    // destroy buffer queue audio player object, and invalidate all
    // associated interfaces
    if (bqPlayerObject != NULL)
    {
        (*bqPlayerObject)->Destroy(bqPlayerObject);
        bqPlayerObject = NULL;
        bqPlayerPlay = NULL;
        bqPlayerBufferQueue = NULL;
    }

    // destroy output mix object, and invalidate all associated interfaces
    if (outputMixObject != NULL)
    {
        (*outputMixObject)->Destroy(outputMixObject);
        outputMixObject = NULL;
    }

    // destroy engine object, and invalidate all associated interfaces
    if (engineObject != NULL)
    {
        (*engineObject)->Destroy(engineObject);
        engineObject = NULL;
        engineEngine = NULL;
    }
}

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
        EAS_RESULT result;
        if ((result = createEngine()) != SL_RESULT_SUCCESS)
        {
            MMAPI_HWOutputDestroy(oHandle);
            oHandle = NULL;
            return oHandle;
        }

        // create buffer queue audio player
        if ((result = createBufferQueueAudioPlayer(pConfig->numChannels, pConfig->sampleRate)) != SL_RESULT_SUCCESS)
        {
            MMAPI_HWOutputDestroy(oHandle);
            oHandle = NULL;
            return oHandle;
        }

        oHandle->ad = (EAS_HANDLE) bqPlayerBufferQueue;
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
        shutdownAudio();
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
//			if (!OutputWaveDevice(mwh->ad, samples, count * pConfig->numChannels * sizeof(EAS_PCM))) {
//				/* signal to repeat this call to MMAPI_HWOutput */
//				count = 0;
//			}
            EAS_RESULT result;
            if ((result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, samples,
                count * pConfig->numChannels * sizeof(EAS_PCM))) != SL_RESULT_SUCCESS) {
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
