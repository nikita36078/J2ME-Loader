/*----------------------------------------------------------------------------
 *
 * File: 
 * eas_waveout.h
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

#ifndef EAS_WAVEOUT_H
#define EAS_WAVEOUT_H

#include "eas_mmapi_config.h"
#include "eas_mmapi_types.h"
#include "../sonivox/host_src/eas_types.h"

/* for C++ linkage */
#ifdef __cplusplus
extern "C" {
#endif

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
EAS_HANDLE OpenWaveDevice (EAS_U32 devNum, EAS_I32 channels, EAS_I32 samplesPerSec, EAS_I32 bitsPerSample);

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
EAS_INT OutputWaveDevice (EAS_HANDLE waveOutDev, void *buffer, EAS_I32 n);

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
EAS_BOOL CloseWaveDevice (EAS_HANDLE waveOutDev);

#ifdef __cplusplus
} /* end extern "C" */
#endif

#endif /* end #ifndef EAS_WAVEOUT_H */

