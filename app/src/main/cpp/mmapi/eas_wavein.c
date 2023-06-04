/*----------------------------------------------------------------------------
 *
 * File:
 * eas_wavein.c
 *
 * Contents and purpose:
 * This module captures a PCM stream from a Windows WAVE input device
 *
 * Copyright Sonic Network Inc. 2006
 *----------------------------------------------------------------------------
 * Revision Control:
 *   $Revision: 560 $
 *   $Date: 2007-02-02 14:34:18 -0800 (Fri, 02 Feb 2007) $
 *----------------------------------------------------------------------------
*/

#include <windows.h>
#include <malloc.h>
#include "eas_wavein.h"
#include "../sonivox/host_src/eas_report.h"

#ifndef MMAPI_CAPTURE_DEVICE_BUFFERS
#define MMAPI_CAPTURE_DEVICE_BUFFERS		8
#endif

#ifndef CAPTURE_BUFFER_SIZE
#define CAPTURE_BUFFER_SIZE	256
#endif

#define QUEUE_TIMEOUT	5000

/* wave header blocks for buffers */
typedef struct
{
	EAS_INT		readCount;
	EAS_INT 	readBuffer;
	EAS_INT 	readOffset;
	EAS_INT 	recBuffer;
	EAS_BOOL	closed;
	EAS_BOOL 	queued[MMAPI_CAPTURE_DEVICE_BUFFERS];
	WAVEHDR 	wh[MMAPI_CAPTURE_DEVICE_BUFFERS];
} S_WAVE_IN_STATE;

S_WAVE_IN_STATE waveInState;

/* local prototypes */
static void CALLBACK WaveInCallback (HWAVEIN hwi, UINT uMsg, DWORD dwInstance, DWORD dwParam1, DWORD dwParam2);

/*----------------------------------------------------------------------------
 * NumWaveInDevices()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the number of WaveIn devices
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_INT NumWaveInDevices (void)
{
	return (EAS_INT) waveInGetNumDevs();
}

/*----------------------------------------------------------------------------
 * GetWaveInReadSize()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the number bytes in the capture buffers
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_INT GetWaveInReadSize (void)
{
	return waveInState.readCount;
}

/*----------------------------------------------------------------------------
 * OpenWaveInDevice()
 *----------------------------------------------------------------------------
 * Purpose:
 * Opens the Wave input device for capture
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_VOID_PTR OpenWaveInDevice (EAS_U32 devNum, EAS_I32 channels, EAS_I32 samplesPerSec, EAS_I32 bitsPerSample)
{
	WAVEFORMATEX wf;
	HWAVEIN waveInDev;
	PWAVEHDR p;
	EAS_INT i;

	/* initialize state */
	memset(&waveInState, 0, sizeof(waveInState));

	/* setup header */
	/* TODO: use WAVE_FillHeader in eas_mmapi_wave.c */
	wf.cbSize = sizeof(wf);
	wf.wFormatTag = WAVE_FORMAT_PCM;
	wf.nChannels = (WORD) channels;
	wf.nSamplesPerSec = samplesPerSec;
	wf.wBitsPerSample = (WORD) bitsPerSample;
	wf.nBlockAlign = (WORD) (channels * (EAS_U16) (bitsPerSample / 8));
	wf.nAvgBytesPerSec = wf.nBlockAlign * samplesPerSec;

	/* open the wave output device */
	if (waveInOpen(&waveInDev, devNum, &wf, (DWORD_PTR)WaveInCallback, (DWORD_PTR) &waveInState, CALLBACK_FUNCTION) != MMSYSERR_NOERROR)
		return NULL;

	#ifdef SONIVOX_DEBUG
	EAS_Report(4, "OpenWaveInDevice: opened capture: channels=%d, rate=%d, %d-bits\n", channels, samplesPerSec, bitsPerSample);
	#endif

	/* add the capture buffers to the queue */
	for (i = 0; i < MMAPI_CAPTURE_DEVICE_BUFFERS; i++)
	{

		/* allocate buffer */
		p = &waveInState.wh[i];
		p->lpData = malloc(CAPTURE_BUFFER_SIZE);
		p->dwBufferLength = CAPTURE_BUFFER_SIZE;

		/* prepare for capture */
		if (waveInPrepareHeader((HWAVEIN) waveInDev, p, sizeof(WAVEHDR)) != MMSYSERR_NOERROR) {
			waveInClose((HWAVEIN) waveInDev);
			return NULL;
		}

		/* add to WaveIn device queue */
		if (waveInAddBuffer((HWAVEIN) waveInDev, p, sizeof(WAVEHDR)) != MMSYSERR_NOERROR) {
			waveInClose((HWAVEIN) waveInDev);
	return NULL;
}

		/* buffer is queued for capture */
		waveInState.queued[waveInState.readBuffer] = EAS_TRUE;
	}

	return waveInDev;
}

/*----------------------------------------------------------------------------
 * StartWaveInCapture()
 *----------------------------------------------------------------------------
 * Purpose:
 * Start recording
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_BOOL StartWaveInCapture (EAS_VOID_PTR waveInDev)
{
	return (waveInStart((HWAVEIN) waveInDev) == MMSYSERR_NOERROR);
}

/*----------------------------------------------------------------------------
 * StopWaveInCapture()
 *----------------------------------------------------------------------------
 * Purpose:
 * Stop recording
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_BOOL StopWaveInCapture (EAS_VOID_PTR waveInDev)
{
	return (waveInStop((HWAVEIN) waveInDev) == MMSYSERR_NOERROR);
}

/*----------------------------------------------------------------------------
 * ReadWaveInData()
 *----------------------------------------------------------------------------
 * Purpose:
 * Reads data from the WaveIn device capture buffers
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_INT ReadWaveInData (EAS_VOID_PTR waveInDev, void *buffer, EAS_I32 n)
{
	EAS_INT total;
	EAS_INT count;
	EAS_INT temp;
	WAVEHDR *p;
	char *q = buffer;

	/* copy the specified number of bytes from the capture buffers */
	total = 0;
	while (n)
	{

		/* make sure this buffer is de-queued */
		if (waveInState.queued[waveInState.readBuffer])
			break;

		/* determine how much to copy from this buffer */
		p = &waveInState.wh[waveInState.readBuffer];
		temp = p->dwBytesRecorded - waveInState.readOffset;
		count = n > temp ? temp : n;
		memcpy(q, p->lpData + waveInState.readOffset, count);

		/* adjust read pointer */
		total += count;
		n -= count;
		waveInState.readOffset += count;
		q += count;

		/* end of buffer? */
		if (waveInState.readOffset == p->dwBytesRecorded)
		{
			/* re-queue the buffer */
			if (!waveInState.closed)
			{

				/* add to WaveIn device */
				if (waveInAddBuffer((HWAVEIN) waveInDev, p, sizeof(WAVEHDR)) != MMSYSERR_NOERROR)
					break;
				waveInState.queued[waveInState.readBuffer] = EAS_TRUE;
			}

			waveInState.readOffset = 0;
			if (++waveInState.readBuffer == MMAPI_CAPTURE_DEVICE_BUFFERS)
				waveInState.readBuffer = 0;
		}
	}

	return total;
}


/*----------------------------------------------------------------------------
 * WriteWaveInDataToStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Reads data from the WaveIn device capture buffers and writes it to the
 * specified stream.
 *
 * Inputs:
 *    mHandle: the synth instance
 *    streamHandle: MMAPI handle to the stream.
 * TODO: signature should be modified to be
 *       EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file
 *       once the host write function is available.
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_INT WriteWaveInDataToStream (EAS_VOID_PTR waveInDev, EAS_HW_DATA_HANDLE hwInstData, MMAPI_FILE_STRUCT* streamHandle)
{
	EAS_INT total;
	EAS_I32 count;
	WAVEHDR *p;
	EAS_RESULT res;

	/* copy the specified number of bytes from the capture buffers */
	total = 0;
	while (!waveInState.queued[waveInState.readBuffer])
	{

		/* determine how much to copy from this buffer */
		p = &waveInState.wh[waveInState.readBuffer];
		count = p->dwBytesRecorded - waveInState.readOffset;

		// TODO: use hwInstData and file parameters directly
		res = MMAPI_HWWriteFile(streamHandle->hwInstData,
		                        streamHandle->hwFileHandle,
						        p->lpData + waveInState.readOffset,
								count, &count);
		if (res != EAS_SUCCESS) {
			break;
		}

		/* adjust read pointer */
		total += count;
		waveInState.readOffset += count;

		/* end of buffer? */
		if (waveInState.readOffset == p->dwBytesRecorded)
		{
			/* re-queue the buffer */
			if (!waveInState.closed)
			{

				/* add to WaveIn device */
				if (waveInAddBuffer((HWAVEIN) waveInDev, p, sizeof(WAVEHDR)) != MMSYSERR_NOERROR)
					break;
				waveInState.queued[waveInState.readBuffer] = EAS_TRUE;
			}

			waveInState.readOffset = 0;
			if (++waveInState.readBuffer == MMAPI_CAPTURE_DEVICE_BUFFERS)
				waveInState.readBuffer = 0;
		}
	}

	return total;
}

/*----------------------------------------------------------------------------
 * CloseWaveInDevice()
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
EAS_BOOL CloseWaveInDevice (EAS_VOID_PTR waveInDev)
{
	EAS_INT i;
	EAS_INT count;

	/* reset the device to release capture buffers */
	waveInReset(waveInDev);
	Sleep(1);

	/* wait for buffers to be released */
	for (i = 0; i < MMAPI_CAPTURE_DEVICE_BUFFERS; i++)
	{
		count = QUEUE_TIMEOUT;
		while (waveInState.queued[i])
		{
			if (--count == 0)
				return EAS_FALSE;
			Sleep(1);
		}
	}

	/* free the buffers */
	for (i = 0; i < MMAPI_CAPTURE_DEVICE_BUFFERS; i++)
	{

		/* unprepare the buffer */
		waveInUnprepareHeader((HWAVEIN) waveInDev, &waveInState.wh[i], sizeof(WAVEHDR));

		/* free the buffer */
		free(waveInState.wh[i].lpData);
	}

	/* close the device */
	waveInClose((HWAVEIN) waveInDev);

	/* initialize state */
	memset(&waveInState, 0, sizeof(waveInState));

	return EAS_TRUE;
}

/*----------------------------------------------------------------------------
 * WaveInCallback()
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
static void CALLBACK WaveInCallback (HWAVEIN hiw, UINT uMsg, DWORD dwInstance, DWORD dwParam1, DWORD dwParam2)
{
	S_WAVE_IN_STATE *pState;

	/* only care about done messages */
	if (uMsg != WIM_DATA)
		return;

	/* setup pointer to data - ignore warning about 64-bit pointer conversion */
	#pragma warning( disable : 4312)
	pState = (S_WAVE_IN_STATE*) dwInstance;
	#pragma warning( default : 4312)

	/* increment read count */
	pState->readCount += pState->wh[pState->recBuffer].dwBytesRecorded;

	/* clear queued flag for this buffer */
	pState->queued[pState->recBuffer] = EAS_FALSE;
	if (++(pState->recBuffer) == MMAPI_CAPTURE_DEVICE_BUFFERS)
		pState->recBuffer = 0;
}

