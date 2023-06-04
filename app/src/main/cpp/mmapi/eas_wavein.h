/*----------------------------------------------------------------------------
 *
 * File:
 * eas_wavein.h
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

#ifndef EAS_WAVEIN_H
#define EAS_WAVEIN_H

#include "eas_mmapi_config.h"
#include "eas_mmapi_types.h"
#include "../sonivox/host_src/eas_types.h"
#include "eas_mmapi.h"

#ifdef __cplusplus
extern "C"
{
#endif

/*----------------------------------------------------------------------------
 * NumWaveInDevices()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the number of of WaveIn devices
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_INT NumWaveInDevices (void);

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
EAS_INT GetWaveInReadSize (void);

/*----------------------------------------------------------------------------
 * OpenWaveInDevice()
 *----------------------------------------------------------------------------
 * Purpose:
 * Opens the WaveIn device for capture
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_VOID_PTR OpenWaveInDevice (EAS_U32 devNum, EAS_I32 channels, EAS_I32 samplesPerSec, EAS_I32 bitsPerSample);

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
EAS_BOOL StartWaveInCapture (EAS_VOID_PTR waveInDev);

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
EAS_BOOL StopWaveInCapture (EAS_VOID_PTR waveInDev);

/*----------------------------------------------------------------------------
 * ReadWaveInData()
 *----------------------------------------------------------------------------
 * Purpose:
 * Adds an empty audio buffer to the WaveIn device to be filled
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_INT ReadWaveInData (EAS_VOID_PTR waveInDev, void *buffer, EAS_I32 n);

/*----------------------------------------------------------------------------
 * WriteWaveInDataToStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Reads data from the WaveIn device capture buffers and writes it to the
 * specified stream.
 * Uses MMAPI_HWWriteFile() internally to write the data to the stream.
 *
 * Inputs:
 *    mHandle: the synth instance
 *    streamHandle: MMAPI handle to the stream.
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_INT WriteWaveInDataToStream (EAS_VOID_PTR waveInDev, EAS_HW_DATA_HANDLE hwInstData, MMAPI_FILE_STRUCT* streamHandle);

/*----------------------------------------------------------------------------
 * CloseWaveInDevice()
 *----------------------------------------------------------------------------
 * Purpose:
 * Closes the WaveIn device
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_BOOL CloseWaveInDevice (EAS_VOID_PTR waveInDev);

#ifdef __cplusplus
}
#endif

#endif /* end #ifndef EAS_WAVEIN_H */
