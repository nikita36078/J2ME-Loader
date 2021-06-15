/*----------------------------------------------------------------------------
 *
 * File:
 * eas.h
 *
 * Contents and purpose:
 * The public interface header for the EAS synthesizer.
 *
 * This header only contains declarations that are specific
 * to this implementation.
 *
 * DO NOT MODIFY THIS FILE!
 *
 * Copyright Sonic Network Inc. 2005, 2006

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
 *   $Revision: 852 $
 *   $Date: 2007-09-04 11:43:49 -0700 (Tue, 04 Sep 2007) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_H
#define _EAS_H

#include "eas_types.h"

/* for C++ linkage */
#ifdef __cplusplus
extern "C" {
#endif

/* library version macro */
#define MAKE_LIB_VERSION(a,b,c,d) (((((((EAS_U32) a <<8) | (EAS_U32) b) << 8) | (EAS_U32) c) << 8) | (EAS_U32) d)
#define LIB_VERSION MAKE_LIB_VERSION(3, 6, 10, 14)

typedef struct
{
    EAS_U32     libVersion;
    EAS_BOOL    checkedVersion;
    EAS_I32     maxVoices;
    EAS_I32     numChannels;
    EAS_I32     sampleRate;
    EAS_I32     mixBufferSize;
    EAS_BOOL    filterEnabled;
    EAS_U32     buildTimeStamp;
    EAS_CHAR    *buildGUID;
} S_EAS_LIB_CONFIG;

/* enumerated effects module numbers for configuration */
typedef enum
{
    EAS_MODULE_ENHANCER = 0,
    EAS_MODULE_COMPRESSOR,
    EAS_MODULE_REVERB,
    EAS_MODULE_CHORUS,
    EAS_MODULE_WIDENER,
    EAS_MODULE_GRAPHIC_EQ,
    EAS_MODULE_WOW,
    EAS_MODULE_MAXIMIZER,
    EAS_MODULE_TONECONTROLEQ,
    NUM_EFFECTS_MODULES
} E_FX_MODULES;

/* enumerated optional module numbers for configuration */
typedef enum
{
    EAS_MODULE_MMAPI_TONE_CONTROL = 0,
    EAS_MODULE_METRICS
} E_OPT_MODULES;
#define NUM_OPTIONAL_MODULES    2

/* enumerated audio decoders for configuration */
typedef enum
{
    EAS_DECODER_PCM = 0,
    EAS_DECODER_SMAF_ADPCM,
    EAS_DECODER_IMA_ADPCM,
    EAS_DECODER_7BIT_SMAF_ADPCM,
    EAS_DECODER_NOT_SUPPORTED
} E_DECODER_MODULES;
#define NUM_DECODER_MODULES     4

/* defines for EAS_PEOpenStream flags parameter */
#define PCM_FLAGS_STEREO        0x00000100  /* stream is stereo */
#define PCM_FLAGS_8_BIT         0x00000001  /* 8-bit format */
#define PCM_FLAGS_UNSIGNED      0x00000010  /* unsigned format */
#define PCM_FLAGS_STREAMING     0x80000000  /* streaming mode */

/* maximum volume setting */
#define EAS_MAX_VOLUME          100

/*----------------------------------------------------------------------------
 * EAS_Init()
 *----------------------------------------------------------------------------
 * Purpose:
 * Initialize the synthesizer library
 *
 * Inputs:
 *  polyphony       - number of voices to play (dynamic memory model only)
 *  ppLibData       - pointer to data handle variable for this instance
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_Init (EAS_DATA_HANDLE *ppEASData);

/*----------------------------------------------------------------------------
 * EAS_Config()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns a pointer to a structure containing the configuration options
 * in this library build.
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC const S_EAS_LIB_CONFIG *EAS_Config (void);

/*----------------------------------------------------------------------------
 * EAS_Shutdown()
 *----------------------------------------------------------------------------
 * Purpose:
 * Shuts down the library. Deallocates any memory associated with the
 * synthesizer (dynamic memory model only)
 *
 * Inputs:
 *  pEASData        - handle to data for this instance
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_Shutdown (EAS_DATA_HANDLE pEASData);

/*----------------------------------------------------------------------------
 * EAS_Render()
 *----------------------------------------------------------------------------
 * Purpose:
 * Parse the Midi data and render PCM audio data.
 *
 * Inputs:
 *  pEASData        - buffer for internal EAS data
 *  pOut            - output buffer pointer
 *  nNumRequested   - requested num samples to generate
 *  pnNumGenerated  - actual number of samples generated
 *
 * Outputs:
 *  EAS_SUCCESS if PCM data was successfully rendered
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_Render (EAS_DATA_HANDLE pEASData, EAS_PCM *pOut, EAS_I32 numRequested, EAS_I32 *pNumGenerated);

/*----------------------------------------------------------------------------
 * EAS_SetRepeat()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the selected stream to repeat.
 *
 * Inputs:
 *  pEASData        - handle to data for this instance
 *  streamHandle    - handle to stream
 *  repeatCount     - repeat count (0 = no repeat, -1 = repeat forever)
 *
 * Outputs:
 *
 * Side Effects:
 *
 * Notes:
 *  0 = no repeat
 *  1 = repeat once, i.e. play through twice
 *  -1 = repeat forever
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetRepeat (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_I32 repeatCount);

/*----------------------------------------------------------------------------
 * EAS_GetRepeat()
 *----------------------------------------------------------------------------
 * Purpose:
 * Gets the current repeat count for the selected stream.
 *
 * Inputs:
 *  pEASData        - handle to data for this instance
 *  streamHandle    - handle to stream
 *  pRrepeatCount   - pointer to variable to hold repeat count
 *
 * Outputs:
 *
 * Side Effects:
 *
 * Notes:
 *  0 = no repeat
 *  1 = repeat once, i.e. play through twice
 *  -1 = repeat forever
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetRepeat (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_I32 *pRepeatCount);

/*----------------------------------------------------------------------------
 * EAS_SetPlaybackRate()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the playback rate.
 *
 * Inputs:
 *  pEASData        - handle to data for this instance
 *  streamHandle    - handle to stream
 *  rate            - rate (28-bit fractional amount)
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetPlaybackRate (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_U32 rate);
#define MAX_PLAYBACK_RATE   (EAS_U32)(1L << 29)
#define MIN_PLAYBACK_RATE   (EAS_U32)(1L << 27)

/*----------------------------------------------------------------------------
 * EAS_SetTransposition)
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the key tranposition for the synthesizer. Transposes all
 * melodic instruments by the specified amount. Range is limited
 * to +/-12 semitones.
 *
 * Inputs:
 *  pEASData        - handle to data for this instance
 *  streamHandle    - handle to stream
 *  transposition   - +/-12 semitones
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetTransposition (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_I32 transposition);
#define MAX_TRANSPOSE       12

/*----------------------------------------------------------------------------
 * EAS_SetSynthPolyphony()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the polyphony of the synthesizer. Value must be >= 1 and <= the
 * maximum number of voices. This function will pin the polyphony
 * at those limits
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * synthNum         - synthesizer number (0 = onboard, 1 = DSP)
 * polyphonyCount   - the desired polyphony count
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetSynthPolyphony (EAS_DATA_HANDLE pEASData, EAS_I32 synthNum, EAS_I32 polyphonyCount);

/*----------------------------------------------------------------------------
 * EAS_GetSynthPolyphony()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the current polyphony setting of the synthesizer
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * synthNum         - synthesizer number (0 = onboard, 1 = DSP)
 * pPolyphonyCount  - pointer to variable to receive polyphony count
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetSynthPolyphony (EAS_DATA_HANDLE pEASData, EAS_I32 synthNum, EAS_I32 *pPolyphonyCount);

/*----------------------------------------------------------------------------
 * EAS_SetPolyphony()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the polyphony of the stream. Value must be >= 1 and <= the
 * maximum number of voices. This function will pin the polyphony
 * at those limits
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - handle returned by EAS_OpenFile
 * polyphonyCount   - the desired polyphony count
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetPolyphony (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_I32 polyphonyCount);

/*----------------------------------------------------------------------------
 * EAS_GetPolyphony()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the current polyphony setting of the stream
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - handle returned by EAS_OpenFile
 * pPolyphonyCount  - pointer to variable to receive polyphony count
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetPolyphony (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_I32 *pPolyphonyCount);

/*----------------------------------------------------------------------------
 * EAS_SetPriority()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the priority of the stream. Determines which stream's voices
 * are stolen when there are insufficient voices for all notes.
 * Value must be in the range of 1-255, lower values are higher
 * priority. The default priority is 50.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - handle returned by EAS_OpenFile
 * polyphonyCount   - the desired polyphony count
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetPriority (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_I32 priority);

/*----------------------------------------------------------------------------
 * EAS_GetPriority()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the current priority setting of the stream
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - handle returned by EAS_OpenFile
 * pPriority        - pointer to variable to receive priority
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetPriority (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_I32 *pPriority);

/*----------------------------------------------------------------------------
 * EAS_SetVolume()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the master volume for the mixer. The default volume setting is
 * 90 (-10 dB). The volume range is 0 to 100 in 1dB increments.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * volume           - the desired master volume
 *
 * Outputs:
 *
 *
 * Side Effects:
 * overrides any previously set master volume from sysex
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetVolume (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_I32 volume);

/*----------------------------------------------------------------------------
 * EAS_GetVolume()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the master volume for the mixer in 1dB increments.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * volume           - the desired master volume
 *
 * Outputs:
 *
 *
 * Side Effects:
 * overrides any previously set master volume from sysex
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_I32 EAS_GetVolume (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle);

/*----------------------------------------------------------------------------
 * EAS_SetMaxLoad()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the maximum workload the parsers will do in a single call to
 * EAS_Render. The units are currently arbitrary, but should correlate
 * well to the actual CPU cycles consumed. The primary effect is to
 * reduce the occasional peaks in CPU cycles consumed when parsing
 * dense parts of a MIDI score. Setting maxWorkLoad to zero disables
 * the workload limiting function.
 *
 * Inputs:
 *  pEASData        - handle to data for this instance
 *  maxLoad         - the desired maximum workload
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetMaxLoad (EAS_DATA_HANDLE pEASData, EAS_I32 maxLoad);

/*----------------------------------------------------------------------------
 * EAS_SetMaxPCMStreams()
 *----------------------------------------------------------------------------
 * Sets the maximum number of PCM streams allowed in parsers that
 * use PCM streaming.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - handle returned by EAS_OpenFile
 * maxNumStreams    - maximum number of PCM streams
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetMaxPCMStreams (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_I32 maxNumStreams);

/*----------------------------------------------------------------------------
 * EAS_OpenFile()
 *----------------------------------------------------------------------------
 * Purpose:
 * Opens a file for audio playback.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * locator          - pointer to filename or other locating information
 * pStreamHandle    - pointer to stream handle variable
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_OpenFile (EAS_DATA_HANDLE pEASData, EAS_FILE_LOCATOR locator, EAS_HANDLE *pStreamHandle);

#ifdef MMAPI_SUPPORT
/*----------------------------------------------------------------------------
 * EAS_MMAPIToneControl()
 *----------------------------------------------------------------------------
 * Purpose:
 * Opens a ToneControl file for audio playback.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * locator          - pointer to filename or other locating information
 * pStreamHandle    - pointer to stream handle variable
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_MMAPIToneControl (EAS_DATA_HANDLE pEASData, EAS_FILE_LOCATOR locator, EAS_HANDLE *pStreamHandle);

/*----------------------------------------------------------------------------
 * EAS_GetWaveFmtChunk
 *----------------------------------------------------------------------------
 * Helper function to retrieve WAVE file fmt chunk for MMAPI
 *----------------------------------------------------------------------------
 * pEASData         - pointer to EAS persistent data object
 * streamHandle     - stream handle
 * pFmtChunk        - pointer to pointer to FMT chunk data
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetWaveFmtChunk (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_VOID_PTR *ppFmtChunk);
#endif

/*----------------------------------------------------------------------------
 * EAS_GetFileType
 *----------------------------------------------------------------------------
 * Returns the file type (see eas_types.h for enumerations)
 *----------------------------------------------------------------------------
 * pEASData         - pointer to EAS persistent data object
 * streamHandle     - stream handle
 * pFileType        - pointer to variable to receive file type
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetFileType (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_I32 *pFileType);

/*----------------------------------------------------------------------------
 * EAS_ParseMetaData()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - file or stream handle
 * playLength       - pointer to variable to store the play length (in msecs)
 *
 * Outputs:
 *
 *
 * Side Effects:
 *                  - resets the parser to the start of the file
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_ParseMetaData (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_I32 *pPlayLength);

/*----------------------------------------------------------------------------
 * EAS_Prepare()
 *----------------------------------------------------------------------------
 * Purpose:
 * Prepares the synthesizer to play the file or stream. Parses the first
 * frame of data from the file and arms the synthesizer.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - file or stream handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_Prepare (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle);

/*----------------------------------------------------------------------------
 * EAS_State()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the state of an audio file or stream.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - file or stream handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_State (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_STATE *pState);

/*----------------------------------------------------------------------------
 * EAS_RegisterMetaDataCallback()
 *----------------------------------------------------------------------------
 * Purpose:
 * Registers a metadata callback function for parsed metadata. To
 * de-register the callback, call this function again with parameter
 * cbFunc set to NULL.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - file or stream handle
 * cbFunc           - pointer to host callback function
 * metaDataBuffer   - pointer to metadata buffer
 * metaDataBufSize  - maximum size of the metadata buffer
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_RegisterMetaDataCallback (
    EAS_DATA_HANDLE pEASData,
    EAS_HANDLE streamHandle,
    EAS_METADATA_CBFUNC cbFunc,
    char *metaDataBuffer,
    EAS_I32 metaDataBufSize,
    EAS_VOID_PTR pUserData);

/*----------------------------------------------------------------------------
 * EAS_GetNoteCount ()
 *----------------------------------------------------------------------------
 * Returns the total number of notes played in this stream
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - file or stream handle
 * pNoteCount       - pointer to variable to receive note count
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetNoteCount (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_I32 *pNoteCount);

/*----------------------------------------------------------------------------
 * EAS_CloseFile()
 *----------------------------------------------------------------------------
 * Purpose:
 * Closes an audio file or stream. Playback should have either paused or
 * completed (EAS_State returns EAS_PAUSED or EAS_STOPPED).
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - file or stream handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_CloseFile (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle);

/*----------------------------------------------------------------------------
 * EAS_OpenMIDIStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Opens a raw MIDI stream allowing the host to route MIDI cable data directly to the synthesizer
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * pStreamHandle    - pointer to variable to hold file or stream handle
 * streamHandle     - open stream or NULL for new synthesizer instance
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_OpenMIDIStream (EAS_DATA_HANDLE pEASData, EAS_HANDLE *pStreamHandle, EAS_HANDLE streamHandle);

/*----------------------------------------------------------------------------
 * EAS_WriteMIDIStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Send data to the MIDI stream device
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - stream handle
 * pBuffer          - pointer to buffer
 * count            - number of bytes to write
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_WriteMIDIStream(EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_U8 *pBuffer, EAS_I32 count);

/*----------------------------------------------------------------------------
 * EAS_CloseMIDIStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Closes a raw MIDI stream
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - stream handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_CloseMIDIStream (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle);

/*----------------------------------------------------------------------------
 * EAS_Locate()
 *----------------------------------------------------------------------------
 * Purpose:
 * Locate into the file associated with the handle.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - file handle
 * milliseconds     - playback offset from start of file in milliseconds
 *
 * Outputs:
 *
 *
 * Side Effects:
 * the actual offset will be quantized to the closest update period, typically
 * a resolution of 5.9ms. Notes that are started prior to this time will not
 * sound. Any notes currently playing will be shut off.
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_Locate (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_I32 milliseconds, EAS_BOOL offset);

/*----------------------------------------------------------------------------
 * EAS_GetRenderTime()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the current playback offset
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 *
 * Outputs:
 * Gets the render time clock in msecs.
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetRenderTime (EAS_DATA_HANDLE pEASData, EAS_I32 *pTime);

/*----------------------------------------------------------------------------
 * EAS_GetLocation()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the current playback offset
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - file handle
 *
 * Outputs:
 * The offset in milliseconds from the start of the current sequence, quantized
 * to the nearest update period. Actual resolution is typically 5.9 ms.
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetLocation (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_I32 *pTime);

/*----------------------------------------------------------------------------
 * EAS_Pause()
 *----------------------------------------------------------------------------
 * Purpose:
 * Pauses the playback of the data associated with this handle. The audio
 * is gracefully ramped down to prevent clicks and pops. It may take several
 * buffers of audio before the audio is muted.
 *
 * Inputs:
 * psEASData        - pointer to overall EAS data structure
 * streamHandle     - file or stream handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_Pause (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle);

/*----------------------------------------------------------------------------
 * EAS_Resume()
 *----------------------------------------------------------------------------
 * Purpose:
 * Resumes the playback of the data associated with this handle. The audio
 * is gracefully ramped up to prevent clicks and pops.
 *
 * Inputs:
 * psEASData        - pointer to overall EAS data structure
 * streamHandle     - file or stream handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_Resume (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle);

/*----------------------------------------------------------------------------
 * EAS_GetParameter()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the parameter of a module. See E_MODULES for a list of modules
 * and the header files of the modules for a list of parameters.
 *
 * Inputs:
 * psEASData        - pointer to overall EAS data structure
 * module           - enumerated module number
 * param            - enumerated parameter number
 * pValue           - pointer to variable to receive parameter value
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetParameter (EAS_DATA_HANDLE pEASData, EAS_I32 module, EAS_I32 param, EAS_I32 *pValue);

/*----------------------------------------------------------------------------
 * EAS_SetParameter()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the parameter of a module. See E_MODULES for a list of modules
 * and the header files of the modules for a list of parameters.
 *
 * Inputs:
 * psEASData        - pointer to overall EAS data structure
 * handle           - file or stream handle
 * module           - enumerated module number
 * param            - enumerated parameter number
 * value            - new parameter value
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetParameter (EAS_DATA_HANDLE pEASData, EAS_I32 module, EAS_I32 param, EAS_I32 value);

#ifdef _METRICS_ENABLED
/*----------------------------------------------------------------------------
 * EAS_MetricsReport()
 *----------------------------------------------------------------------------
 * Purpose:
 * Displays the current metrics through the EAS_Report interface.
 *
 * Inputs:
 * pEASData             - instance data handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_MetricsReport (EAS_DATA_HANDLE pEASData);

/*----------------------------------------------------------------------------
 * EAS_MetricsReset()
 *----------------------------------------------------------------------------
 * Purpose:
 * Displays the current metrics through the EAS_Report interface.
 *
 * Inputs:
 * pEASData             - instance data handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_MetricsReset (EAS_DATA_HANDLE pEASData);
#endif

/*----------------------------------------------------------------------------
 * EAS_SetSoundLibrary()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the location of the sound library.
 *
 * Inputs:
 * pEASData             - instance data handle
 * streamHandle         - file or stream handle
 * pSoundLib            - pointer to sound library
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetSoundLibrary (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_SNDLIB_HANDLE pSndLib);

/*----------------------------------------------------------------------------
 * EAS_SetHeaderSearchFlag()
 *----------------------------------------------------------------------------
 * By default, when EAS_OpenFile is called, the parsers check the
 * first few bytes of the file looking for a specific header. Some
 * mobile devices may add a header to the start of a file, which
 * will prevent the parser from recognizing the file. If the
 * searchFlag is set to EAS_TRUE, the parser will search the entire
 * file looking for the header. This may enable EAS to recognize
 * some files that it would ordinarily reject. The negative is that
 * it make take slightly longer to process the EAS_OpenFile request.
 *
 * Inputs:
 * pEASData             - instance data handle
 * searchFlag           - search flag (EAS_TRUE or EAS_FALSE)
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetHeaderSearchFlag (EAS_DATA_HANDLE pEASData, EAS_BOOL searchFlag);

/*----------------------------------------------------------------------------
 * EAS_SetPlayMode()
 *----------------------------------------------------------------------------
 * Some file formats support special play modes, such as iMode partial
 * play mode. This call can be used to change the play mode. The
 * default play mode (usually straight playback) is always zero.
 *
 * Inputs:
 * pEASData             - instance data handle
 * handle               - file or stream handle
 * playMode             - play mode (see eas_types.h for enumerations)
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetPlayMode (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_I32 playMode);

#ifdef DLS_SYNTHESIZER
/*----------------------------------------------------------------------------
 * EAS_LoadDLSCollection()
 *----------------------------------------------------------------------------
 * Purpose:
 * Downloads a DLS collection
 *
 * Inputs:
 * pEASData             - instance data handle
 * streamHandle         - file or stream handle
 * locator              - file locator
 *
 * Outputs:
 *
 *
 * Side Effects:
 * May overlay instruments in the GM sound set
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_LoadDLSCollection (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_FILE_LOCATOR locator);
#endif

/*----------------------------------------------------------------------------
 * EAS_SetFrameBuffer()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the frame buffer pointer passed to the IPC communications functions
 *
 * Inputs:
 * pEASData             - instance data handle
 * locator              - file locator
 *
 * Outputs:
 *
 *
 * Side Effects:
 * May overlay instruments in the GM sound set
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetFrameBuffer (EAS_DATA_HANDLE pEASData, EAS_FRAME_BUFFER_HANDLE pFrameBuffer);

#ifdef EXTERNAL_AUDIO
/*----------------------------------------------------------------------------
 * EAS_RegExtAudioCallback()
 *----------------------------------------------------------------------------
 * Purpose:
 * Registers callback functions for audio events.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - file or stream handle
 * cbProgChgFunc    - pointer to host callback function for program change
 * cbEventFunc      - pointer to host callback functio for note events
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_RegExtAudioCallback (EAS_DATA_HANDLE pEASData,
    EAS_HANDLE streamHandle,
    EAS_VOID_PTR pInstData,
    EAS_EXT_PRG_CHG_FUNC cbProgChgFunc,
    EAS_EXT_EVENT_FUNC cbEventFunc);

/*----------------------------------------------------------------------------
 * EAS_GetMIDIControllers()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the current state of MIDI controllers on the requested channel.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - file or stream handle
 * pControl         - pointer to structure to receive data
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetMIDIControllers (EAS_DATA_HANDLE pEASData, EAS_HANDLE streamHandle, EAS_U8 channel, S_MIDI_CONTROLLERS *pControl);
#endif

/*----------------------------------------------------------------------------
 * EAS_SearchFile
 *----------------------------------------------------------------------------
 * Search file for specific sequence starting at current file
 * position. Returns offset to start of sequence.
 *
 * Inputs:
 * pEASData         - pointer to EAS persistent data object
 * fileHandle       - file handle
 * searchString     - pointer to search sequence
 * len              - length of search sequence
 * pOffset          - pointer to variable to store offset to sequence
 *
 * Returns EAS_EOF if end-of-file is reached
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_SearchFile (EAS_DATA_HANDLE pEASData, EAS_FILE_HANDLE fileHandle, const EAS_U8 *searchString, EAS_I32 len, EAS_I32 *pOffset);

#ifdef __cplusplus
} /* end extern "C" */
#endif

#endif /* #ifndef _EAS_H */
