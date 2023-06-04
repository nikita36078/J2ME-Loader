/*----------------------------------------------------------------------------
 *
 * File:
 * eas_waveout.c
 *
 * Contents and purpose:
 * This module outputs a PCM stream to a Windows WAVE output device
 *
 * Copyright Sonic Network Inc. 2005
 *----------------------------------------------------------------------------
 * Revision Control:
 *   $Revision: 560 $
 *   $Date: 2007-02-02 14:34:18 -0800 (Fri, 02 Feb 2007) $
 *----------------------------------------------------------------------------
*/

#include <windows.h>
#include <malloc.h>
#include "eas_waveout.h"


#define MAX_BUFFERS		8
#ifdef MIDP
/* $$fb for MIDP, we must not block in native */
#define QUEUE_TIMEOUT	0
#else
#define QUEUE_TIMEOUT	5000
#endif

/* timeout, in approx. milliseconds, for the close function to wait */
#define CLOSE_TIMEOUT	5000


/* wave header blocks for buffers */
typedef struct
{
    EAS_INT queueBuffer;
    EAS_INT playBuffer;
    EAS_BOOL queued[MAX_BUFFERS];
    WAVEHDR wh[MAX_BUFFERS];
} S_WAVE_OUT_STATE;

S_WAVE_OUT_STATE waveOutState;

/* local prototypes */
static void CALLBACK WaveOutCallback (HWAVEOUT hwo, UINT uMsg, DWORD dwInstance, DWORD dwParam1, DWORD dwParam2);
/* $$fb added timeout parameter */
static EAS_BOOL WaitOnBuffer (EAS_HANDLE waveOutDev, EAS_INT n, EAS_INT timeout);

/*----------------------------------------------------------------------------
 * OpenWaveDevice()
 *----------------------------------------------------------------------------
 * Purpose:
 * Opens the Wave output device for playback
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_HANDLE OpenWaveDevice (EAS_U32 devNum, EAS_I32 channels, EAS_I32 samplesPerSec, EAS_I32 bitsPerSample)
{
    WAVEFORMATEX wf;
    HWAVEOUT waveOutDev;

    /* initialize state */
    memset(&waveOutState, 0, sizeof(waveOutState));

    /* setup header */
    wf.cbSize = sizeof(wf);
    wf.wFormatTag = WAVE_FORMAT_PCM;
    wf.nChannels = (WORD) channels;
    wf.nSamplesPerSec = samplesPerSec;
    wf.wBitsPerSample = (WORD) bitsPerSample;
    wf.nBlockAlign = (WORD) (channels * (EAS_U16) (bitsPerSample / 8));
    wf.nAvgBytesPerSec = wf.nBlockAlign * samplesPerSec;

    /* open the wave output device */
    if (waveOutOpen(&waveOutDev, devNum, &wf, (DWORD_PTR)WaveOutCallback, (DWORD_PTR) &waveOutState, CALLBACK_FUNCTION) != MMSYSERR_NOERROR)
        return NULL;
    return (EAS_HANDLE) waveOutDev;
} /* end OpenWaveDevice */

/*----------------------------------------------------------------------------
 * OutputWaveDevice()
 *----------------------------------------------------------------------------
 * Purpose:
 * Outputs a buffer of audio to the Wave device
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_BOOL OutputWaveDevice (EAS_HANDLE waveOutDev, void *buffer, EAS_I32 n)
{
    PWAVEHDR p;

    /* need to make sure we don't overwrite a buffer in use */
    if (!WaitOnBuffer(waveOutDev, waveOutState.queueBuffer, QUEUE_TIMEOUT))
        return EAS_FALSE;

    /* allocate double-buffer and copy data */
    p = &waveOutState.wh[waveOutState.queueBuffer];
    p->lpData = malloc(n);
    memcpy(p->lpData, buffer, n);
    p->dwBufferLength = n;
    waveOutState.queued[waveOutState.queueBuffer] = EAS_TRUE;

    /* prepare the header */
    if (waveOutPrepareHeader((HWAVEOUT)waveOutDev, p, sizeof(WAVEHDR)) != MMSYSERR_NOERROR)
    return EAS_FALSE;

    /* output to wave device */
    if (waveOutWrite((HWAVEOUT)waveOutDev, p, sizeof(WAVEHDR)) != MMSYSERR_NOERROR)
    return EAS_FALSE;

    if (++waveOutState.queueBuffer == MAX_BUFFERS)
        waveOutState.queueBuffer = 0;
    return EAS_TRUE;
} /* end WriteWaveHeader */

/*----------------------------------------------------------------------------
 * CloseWaveDevice()
 *----------------------------------------------------------------------------
 * Purpose:
 * Closes the Wave device
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_BOOL CloseWaveDevice (EAS_HANDLE waveOutDev)
{
    EAS_INT i;

    /* $$fb accelerate closure */
    waveOutReset((HWAVEOUT)waveOutDev);

    /* wait for buffers to finish playing */
    for (i = 0; i < MAX_BUFFERS; i++)
    {
        if (!WaitOnBuffer(waveOutDev, i, CLOSE_TIMEOUT))
            return EAS_FALSE;
    }

    waveOutClose((HWAVEOUT)waveOutDev);
    return EAS_TRUE;
} /* end WaveFileClose */

/*----------------------------------------------------------------------------
 * WaveOutCallback()
 *----------------------------------------------------------------------------
 * Purpose:
 * Callback function for wave output device calls
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static void CALLBACK WaveOutCallback (HWAVEOUT hwo, UINT uMsg, DWORD dwInstance, DWORD dwParam1, DWORD dwParam2)
{
S_WAVE_OUT_STATE *pState;

/* only care about done messages */
if (uMsg != WOM_DONE)
return;

/* setup pointer to data - ignore warning about 64-bit pointer conversion */
#pragma warning( disable : 4312)
pState = (S_WAVE_OUT_STATE*) dwInstance;
#pragma warning( default : 4312)

/* clear queued flag for this buffer */
pState->queued[pState->playBuffer] = EAS_FALSE;
if (++(pState->playBuffer) == MAX_BUFFERS)
pState->playBuffer = 0;
} /* end WaveOutCallback */

/*----------------------------------------------------------------------------
 * WaitOnBuffer()
 *----------------------------------------------------------------------------
 * Purpose:
 * Callback function for wave output device calls
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/* $$fb added timeout parameter */
static EAS_BOOL WaitOnBuffer (EAS_HANDLE waveOutDev, EAS_INT n, EAS_INT timeout)
{
    /* need to make sure we don't overwrite a buffer in use */
    while (waveOutState.queued[n])
    {
        if (--timeout <= 0)
            return EAS_FALSE;
        Sleep(1);
    }

    /* free previously allocated data */
    if (waveOutState.wh[n].lpData)
    {

        /* unprepare the buffer */
        if (waveOutUnprepareHeader((HWAVEOUT)waveOutDev, &waveOutState.wh[n], sizeof(WAVEHDR)) != MMSYSERR_NOERROR)
        return EAS_FALSE;

        /* free it */
        free(waveOutState.wh[n].lpData);
        waveOutState.wh[n].lpData = NULL;
    }
    return EAS_TRUE;
} /* end WaitOnBuffer */
