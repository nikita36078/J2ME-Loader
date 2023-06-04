/*----------------------------------------------------------------------------
 *
 * File:
 * eas_pcm.h
 *
 * Contents and purpose:
 * External function prototypes for eas_pcm.c module
 *
 *
 * Copyright Sonic Network Inc. 2005

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *----------------------------------------------------------------------------
 * Revision Control:
 *   $Revision: 847 $
 *   $Date: 2007-08-27 21:30:08 -0700 (Mon, 27 Aug 2007) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_PCM_H
#define _EAS_PCM_H

/* default gain setting - roughly unity gain */
#define PCM_DEFAULT_GAIN_SETTING    0x6000

typedef struct s_pcm_state_tag *EAS_PCM_HANDLE;
typedef void (*EAS_PCM_CALLBACK) (EAS_DATA_HANDLE pEASData, EAS_VOID_PTR cbInstData, EAS_PCM_HANDLE pcmHandle, EAS_STATE state);

/* parameters for EAS_PEOpenStream */
typedef struct s_pcm_open_params_tag
{
    EAS_FILE_HANDLE     fileHandle;
    EAS_I32             decoder;
    EAS_U32             sampleRate;
    EAS_I32             size;
    EAS_U32             loopStart;
    EAS_U32             loopSamples;
    EAS_I32             blockSize;
    EAS_U32             flags;
    EAS_U32             envData;
    EAS_I16             volume;
    EAS_PCM_CALLBACK    pCallbackFunc;
    EAS_VOID_PTR        cbInstData;
 } S_PCM_OPEN_PARAMS;

/*----------------------------------------------------------------------------
 * EAS_PEInit()
 *----------------------------------------------------------------------------
 * Purpose:
 * Initializes the PCM engine
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
EAS_RESULT EAS_PEInit (EAS_DATA_HANDLE pEASData);

/*----------------------------------------------------------------------------
 * EAS_PEShutdown()
 *----------------------------------------------------------------------------
 * Purpose:
 * Shuts down the PCM engine
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
EAS_RESULT EAS_PEShutdown (EAS_DATA_HANDLE pEASData);

/*----------------------------------------------------------------------------
 * EAS_PEOpenStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Starts up a PCM playback
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
EAS_RESULT EAS_PEOpenStream (EAS_DATA_HANDLE pEASData, S_PCM_OPEN_PARAMS *pParams, EAS_PCM_HANDLE *pHandle);

/*----------------------------------------------------------------------------
 * EAS_PEContinueStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Continues a PCM stream
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
EAS_RESULT EAS_PEContinueStream (EAS_DATA_HANDLE pEASData, EAS_PCM_HANDLE handle, EAS_I32 size);

/*----------------------------------------------------------------------------
 * EAS_PEGetFileHandle()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the file handle of a stream
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
EAS_RESULT EAS_PEGetFileHandle (EAS_DATA_HANDLE pEASData, EAS_PCM_HANDLE handle, EAS_FILE_HANDLE *pFileHandle);

/*----------------------------------------------------------------------------
 * EAS_PERender()
 *----------------------------------------------------------------------------
 * Purpose:
 * Render a buffer of PCM audio
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
EAS_RESULT EAS_PERender (EAS_DATA_HANDLE pEASData, EAS_I32 numSamples);

/*----------------------------------------------------------------------------
 * EAS_PEUpdateParams()
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the pitch and volume parameters using MIDI controls
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
EAS_RESULT EAS_PEUpdateParams (EAS_DATA_HANDLE pEASData, EAS_PCM_HANDLE pState, EAS_I16 pitch, EAS_I16 gainLeft, EAS_I16 gainRight);

/*----------------------------------------------------------------------------
 * EAS_PELocate()
 *----------------------------------------------------------------------------
 * Purpose:
 * This function seeks to the requested place in the file. Accuracy
 * is dependent on the sample rate and block size.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * pState           - stream handle
 * time             - media time in milliseconds
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_PELocate (EAS_DATA_HANDLE pEASData, EAS_PCM_HANDLE pState, EAS_I32 time);

/*----------------------------------------------------------------------------
 * EAS_PEUpdateVolume()
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the volume parameters for a PCM stream
 *
 * Inputs:
 * pEASData         - pointer to EAS library instance data
 * handle           - pointer to S_PCM_STATE for this stream
 * gainLeft         - linear gain multipler in 1.15 fraction format
 * gainRight        - linear gain multipler in 1.15 fraction format
 * initial          - initial settings, set current gain
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 * Notes
 * In mono mode, leftGain controls the output gain and rightGain is ignored
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
EAS_RESULT EAS_PEUpdateVolume (EAS_DATA_HANDLE pEASData, EAS_PCM_HANDLE pState, EAS_I16 volume);

/*----------------------------------------------------------------------------
 * EAS_PEUpdatePitch()
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the pitch parameter for a PCM stream
 *
 * Inputs:
 * pEASData         - pointer to EAS library instance data
 * pState           - pointer to S_PCM_STATE for this stream
 * pitch            - new pitch value in pitch cents
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
EAS_RESULT EAS_PEUpdatePitch (EAS_DATA_HANDLE pEASData, EAS_PCM_HANDLE pState, EAS_I16 pitch);

/*----------------------------------------------------------------------------
 * EAS_PEState()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the current state of the stream
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 * pState           - pointer to variable to store state
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 * Notes:
 * This interface is also exposed in the internal library for use by the other modules.
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_PEState (EAS_DATA_HANDLE pEASData, EAS_PCM_HANDLE handle, EAS_STATE *pState);

/*----------------------------------------------------------------------------
 * EAS_PEClose()
 *----------------------------------------------------------------------------
 * Purpose:
 * Close the file and clean up
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_PEClose (EAS_DATA_HANDLE pEASData, EAS_PCM_HANDLE handle);

/*----------------------------------------------------------------------------
 * EAS_PEReset()
 *----------------------------------------------------------------------------
 * Purpose:
 * Reset the sequencer. Used for locating backwards in the file.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_PEReset (EAS_DATA_HANDLE pEASData, EAS_PCM_HANDLE handle);

/*----------------------------------------------------------------------------
 * EAS_PEPause()
 *----------------------------------------------------------------------------
 * Purpose:
 * Mute and pause rendering a PCM stream. Sets the gain target to zero and stops the playback
 * at the end of the next audio frame.
 *
 * Inputs:
 * pEASData         - pointer to EAS library instance data
 * handle           - pointer to S_PCM_STATE for this stream
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_PEPause (EAS_DATA_HANDLE pEASData, EAS_PCM_HANDLE handle);

/*----------------------------------------------------------------------------
 * EAS_PEResume()
 *----------------------------------------------------------------------------
 * Purpose:
 * Resume rendering a PCM stream. Sets the gain target back to its
 * previous setting and restarts playback at the end of the next audio
 * frame.
 *
 * Inputs:
 * pEASData         - pointer to EAS library instance data
 * handle           - pointer to S_PCM_STATE for this stream
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_PEResume (EAS_DATA_HANDLE pEASData, EAS_PCM_HANDLE handle);

/*----------------------------------------------------------------------------
 * EAS_PERelease()
 *----------------------------------------------------------------------------
 * Purpose:
 * Put the PCM stream envelope into release.
 *
 * Inputs:
 * pEASData         - pointer to EAS library instance data
 * handle           - pointer to S_PCM_STATE for this stream
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_PERelease (EAS_DATA_HANDLE pEASData, EAS_PCM_HANDLE handle);

#endif /* end _EAS_PCM_H */

