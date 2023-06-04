/*----------------------------------------------------------------------------
 *
 * File:
 * eas_mmapi.h
 *
 * Contents and purpose:
 * Main header file for the MMAPI bridge to EAS. It defines the functions
 * and types for the MMAPI primitives.
 *
 * Note that this file is kept independent from the Java implementation
 * and independent from the EAS implementation. This makes it easy
 * to change the Java-specific implementation without touching any of
 * the EAS specific code. Only eas_types.h is included in order to have
 * a standardized set of basic types.
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
#include "eas_mmapi_wave.h"

#ifndef _EAS_MMAPI_INCLUDED
#define _EAS_MMAPI_INCLUDED

/* for C++ linkage */
#ifdef __cplusplus
extern "C" {
#endif

/* recording data */
typedef struct mmapi_recordingdata_tag {
	/* the host handle returned by EAS_HWOpen().
	 * This is where the data is written to. */
	EAS_FILE_HANDLE handle;
	/* recording state, one of the MMAPI_RS_* constants */
	EAS_I32 state;
	/* a possibly set limit to the recorded file size.
	 * Checked/enforced in MMAPI_ReadRecordedBuffer(). */
	EAS_I32 sizeLimit;
	/* the WAVE format of the original file being read. It is used for the file being written */
	WAVE_FMT_CHUNK waveFmt;
	/* size of complete WAVE header of the original file being read, not the one being written */
	EAS_I32 waveHeaderSize;
	/* size of WAVE data following the header in original file, or -1 if not known */
	EAS_I32 waveDataSize;
	/* size of complete WAVE header of the file being written */
	EAS_I32 writtenHeaderSize;
	/* the circular buffer for recording to OutputStream */
	MMAPI_MediaBuffer* mb;
} MMAPI_RecordingData;

/*
 * the actual struct behind MMAPI_FILE_HANDLE
 * (used by eas_mmapi.c and eas_mmapi_host.c).
 * This struct is also used to wrap the locator
 * in the EAS_OpenFile call.
 */
typedef struct mmapi_file_struct_tag {
	/* the EAS_HANDLE for the opened file */
	EAS_HANDLE handle;
	/* the mode for opening this file */
	MMAPI_OPEN_MODE mode;
	/* the duration of the file in milliseconds, or one of the MMAPI_DURATION_ constants */
	EAS_I32 duration;
	/* linked list of dynamically allocated meta data entries */
	MMAPI_MetaData* metaData;
	/* the host instance handle. Filled in by EAS_HWOpenFile so that
	 * eas_mmapi.c can access host functions */
	EAS_HW_DATA_HANDLE hwInstData;
	/* the host file handle. Filled in by EAS_HWOpenFile so that
	 * eas_mmapi.c can access host functions */
	EAS_FILE_HANDLE hwFileHandle;
	/* structure used for recording */
	MMAPI_RecordingData record;
	union {
		/* the locator for OPEN_MODE_NATIVE. Only valid during OpenFile call! */
		EAS_CHAR* locator;
		/* locator for OPEN_MODE_MEMORY and OPEN_MODE_STREAM */
		MMAPI_MediaBuffer* mb;
	};
	/* the meta data buffer */
	EAS_CHAR metaDataBuffer[MMAPI_METADATA_BUFFER_SIZE+1];
	/* if capturing, handle to the capture device */
	EAS_VOID_PTR captureDevice;
} MMAPI_FILE_STRUCT;


/*----------------------------------------------------------------------------
 * Bridge from Java nInit() to EAS_Init()
 *
 * @return the native MMAPI_DATA_HANDLE (eashandle), or NULL on error
 *----------------------------------------------------------------------------
*/
MMAPI_DATA_HANDLE MMAPI_Init();


/*----------------------------------------------------------------------------
 * Bridge from Java nShutdown() to EAS_Shutdown()
 *
 * @param mHandle the native MMAPI_DATA_HANDLE
 *----------------------------------------------------------------------------
*/
void MMAPI_Shutdown(MMAPI_DATA_HANDLE mHandle);


/*----------------------------------------------------------------------------
 * Bridge from Java nOpenFile() to EAS_OpenFile().
 *
 * @param mHandle the EAS synth handle (MMAPI_DATA_HANDLE)
 * @param locator the locator to open
 * @param mode one of the MODE_ constants
 * @return the instance fHandle of the opened file (MMAPI_FILE_HANDLE), or NULL on
 *         error
 *----------------------------------------------------------------------------
*/
MMAPI_FILE_HANDLE MMAPI_OpenFile(MMAPI_DATA_HANDLE mHandle, EAS_CHAR* locator, MMAPI_OPEN_MODE mode);


/*----------------------------------------------------------------------------
 * Bridge from Java nCloseFile() to EAS_CloseFile().
 *
 * @param mHandle the EAS synth handle (MMAPI_DATA_HANDLE)
 * @param fHandle the synth to the open file (MMAPI_FILE_HANDLE)
 *----------------------------------------------------------------------------
*/
void MMAPI_CloseFile(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle);


/*----------------------------------------------------------------------------
 * Support for memory and stream modes: wrap Java nWrite().
 *
 * @param mHandle the EAS synth handle (EAS_DATA_HANDLE)
 * @param fHandle the synth to the open file (EAS_FILE_HANDLE), or a handle
 *        to the interactive MIDI device if flags includes
 *        MMAPI_WRITE_FLAG_INTERACTIVE_MIDI.
 * @param buffer the media data buffer
 * @param count number of valid bytes in buffer
 * @param totalLength total file size, or -1 if not known
 * @param flags one or more of the MMAPI_WRITE_FLAG_ constants
 * @return number of bytes successfully written
 *----------------------------------------------------------------------------
*/
EAS_I32 MMAPI_WriteBuffer(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle,
						  EAS_U8* buffer, EAS_I32 offset, EAS_I32 count,
						  EAS_I32 totalLength, EAS_I32 flags);


/*----------------------------------------------------------------------------
 * A way of executing simple getters/setters with just one native Java
 * method. The commandCode (one of the COMMAND_* constants) defines
 * which native function to execute. One optional integer parameter
 * can be passed to the native function. The function can return an
 * integer return code.
 *
 * @param mHandle the EAS synth handle (EAS_DATA_HANDLE)
 * @param fHandle the handle to the open file (EAS_HANDLE)
 * @param commandCode which native function to execute, one of the COMMAND_* constants.
 * @param param an optional parameter for the native function, depends on commandCode what it means
 * @return an integer return value, the meaning depends on commandCode.
 *----------------------------------------------------------------------------
*/
EAS_I32 MMAPI_GeneralCommand(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle,
							 MMAPI_COMMAND_CODE commandCode, EAS_I32 param);

/*----------------------------------------------------------------------------
 * Bridge from Java nPrepare() to EAS_Prepare().
 * The file does not automatically start playing.
 *
 * @param mHandle the EAS synth handle (MMAPI_DATA_HANDLE)
 * @param fHandle the synth to the open file (MMAPI_FILE_HANDLE)
 * @return true if successful
 *----------------------------------------------------------------------------
*/
EAS_BOOL MMAPI_Prepare(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle);


/*----------------------------------------------------------------------------
 * Bridge from Java nResume() to EAS_Resume().
 *
 * @param mHandle the EAS synth handle (MMAPI_DATA_HANDLE)
 * @param fHandle the synth to the open file (MMAPI_FILE_HANDLE)
 * @return true if successful
 *----------------------------------------------------------------------------
*/
EAS_BOOL MMAPI_Resume(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle);


/*----------------------------------------------------------------------------
 * Bridge from Java nPause() to EAS_Pause().
 *
 * @param mHandle the EAS synth handle (MMAPI_DATA_HANDLE)
 * @param fHandle the synth to the open file (MMAPI_FILE_HANDLE)
 * @return true if successful
 *----------------------------------------------------------------------------
*/
EAS_BOOL MMAPI_Pause(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle);


/*----------------------------------------------------------------------------
 * Bridge from Java nRender() to EAS_Render(). This is only used when
 * MMAPI_NATIVE_RENDERING_THREAD is not set.
 *
 * The implementation should not block! This could make the Java system
 * sluggish in a green threads implementation.
 *
 * @param mHandle the EAS synth handle (MMAPI_DATA_HANDLE)
 * @return true if successful
 *----------------------------------------------------------------------------
*/
EAS_BOOL MMAPI_Render(MMAPI_DATA_HANDLE mHandle);


/*----------------------------------------------------------------------------
 * Bridge from Java nGetState() to EAS_State().
 *
 * @param mHandle the EAS synth handle (EAS_DATA_HANDLE)
 * @param fHandle the handle to the open file (EAS_HANDLE)
 * @return the state of the synth, one of the STATE_* constants or -1 on
 *         error
 *----------------------------------------------------------------------------
*/
EAS_I32 MMAPI_GetState(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle);


/*----------------------------------------------------------------------------
 * Bridge from Java nGetLocation() to EAS_GetLocation().
 *
 * @param mHandle the EAS synth handle (MMAPI_DATA_HANDLE)
 * @param fHandle the synth to the open file (MMAPI_FILE_HANDLE)
 * @return the media time in millis
 *----------------------------------------------------------------------------
*/
EAS_I32 MMAPI_GetLocation(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle);

/*----------------------------------------------------------------------------
 * Bridge from Java nLocate() to EAS_Locate().
 *
 * @param mHandle the EAS synth handle (EAS_DATA_HANDLE)
 * @param fHandle the synth to the open file (EAS_HANDLE)
 * @param millis the desired media time in milliseconds
 * @return the media time in millis, or -1 for error
 *----------------------------------------------------------------------------
*/
EAS_I32 MMAPI_Locate(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle, EAS_I32 millis);


/*----------------------------------------------------------------------------
 * Bridge from Java nSetVolume() to EAS_SetVolume().
 *
 * @param mHandle the EAS synth handle (EAS_DATA_HANDLE)
 * @param fHandle the handle to the open file (EAS_HANDLE)
 * @param level the linear volume level, 0...100
 * @return true if successful
 *----------------------------------------------------------------------------
*/
EAS_BOOL MMAPI_SetVolume(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle, EAS_I32 level);


/*----------------------------------------------------------------------------
 * Bridge from Java nGetVolume() to EAS_GetVolume().
 *
 * @param mHandle the EAS synth handle (EAS_DATA_HANDLE)
 * @param fHandle the handle to the open file (EAS_HANDLE)
 * @return level the linear volume level, 0...100, or -1 on error
 *----------------------------------------------------------------------------
*/
EAS_I32 MMAPI_GetVolume(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle);


/*----------------------------------------------------------------------------
 * Bridge from Java nSetRepeat() to EAS_SetRepeat().
 *
 * @param mHandle the EAS synth handle (EAS_DATA_HANDLE)
 * @param fHandle the handle to the open file (EAS_HANDLE)
 * @param repeatCount the number of repeats, 0 for no repetitions, -1 for
 *            infinite repeats
 * @return true if successful
 *----------------------------------------------------------------------------
*/
EAS_BOOL MMAPI_SetRepeat(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle, EAS_I32 repeatCount);


/*----------------------------------------------------------------------------
 * Bridge from Java nGetCurrentRepeat() to EAS_GetCurrentRepeat().
 *
 * @param mHandle the EAS synth handle (EAS_DATA_HANDLE)
 * @param fHandle the handle to the open file (EAS_HANDLE)
 * @return the current repeat count: 0 if played for the first time, 1 if
 *         repeating for the first time, etc. Or -1 for error.
 *----------------------------------------------------------------------------
*/
EAS_I32 MMAPI_GetCurrentRepeat(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle);


/*----------------------------------------------------------------------------
 * Bridge from Java nSetPlaybackRate() to EAS_SetPlaybackRate().
 *
 * @param mHandle the EAS synth handle (EAS_DATA_HANDLE)
 * @param fHandle the handle to the open file (EAS_HANDLE)
 * @param rate the playback rate 4.28-bit fixed point
 * @return the actually set playback rate
 *----------------------------------------------------------------------------
*/
EAS_I32 MMAPI_SetPlaybackRate(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle, EAS_I32 rate);


/*----------------------------------------------------------------------------
 * Bridge from Java nSetTransposition() to EAS_SetTransposition().
 *
 * @param mHandle the EAS synth handle (EAS_DATA_HANDLE)
 * @param fHandle the handle to the open file (EAS_HANDLE)
 * @param transposition the number of semitones to transpose
 * @return the actually set transposition
 *----------------------------------------------------------------------------
*/
EAS_I32 MMAPI_SetTransposition(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle, EAS_I32 transpose);


/*----------------------------------------------------------------------------
 * Retrieve the first meta data in the list of this stream's meta data.
 * Use MMAPI_NextMetaData() in order to remove the first entry. After that,
 * GetMetaData() will return the next meta data entry.
 *
 * @param mHandle the EAS synth handle (EAS_DATA_HANDLE)
 * @param fHandle the handle to the open file (EAS_HANDLE)
 * @return a pointer to a MMAPI_MetaData structure, or NULL upon error or if
 *         no meta data is available anymore.
 *----------------------------------------------------------------------------
*/
MMAPI_MetaData* MMAPI_GetMetaData(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle);


/*----------------------------------------------------------------------------
 * Remove the first entry from the list of meta data.
 *
 * @param mHandle the EAS synth handle (EAS_DATA_HANDLE)
 * @param fHandle the handle to the open file (EAS_HANDLE)
 *----------------------------------------------------------------------------
*/
void MMAPI_NextMetaData(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle);


/*----------------------------------------------------------------------------
 * Retrieve the media duration. Uses EAS_ParseMetaData() to
 * retrieve it, if it's not already called by way of the
 * MMAPI_GetMetaData() function.
 * <p>
 * This is a potentially time-consuming call. It only succeeds
 * if the player is currently stopped/paused, or if the duration was
 * previously retrieved.
 *
 * @param mHandle the EAS synth handle (EAS_DATA_HANDLE)
 * @param fHandle the handle to the open file (EAS_HANDLE)
 * @return the media duration in millis, or -1 for error/not known, or -2 for not yet available
 *----------------------------------------------------------------------------
*/
EAS_I32 MMAPI_GetDuration(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle);


/*----------------------------------------------------------------------------
 * Prepare the specified file stream for recording. If no locator is given,
 * the recorded media data needs to be read with nReadRecordedBytes().
 * Otherwise, the native implementation will write directly to the specified
 * locator.
 *
 * @param mHandle the EAS synth handle (EAS_DATA_HANDLE)
 * @param fHandle the handle to the open file (EAS_HANDLE)
 * @param locator the file URL to write the data to, or NULL if the data
 *            should be made available through readRecordedBytes().
 * @return true if successful, false on error
 *----------------------------------------------------------------------------
*/
EAS_BOOL MMAPI_OpenRecording(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle, EAS_CHAR* locator);


/*----------------------------------------------------------------------------
 * Read recorded bytes. If no bytes are currently available, return 0.
 * If an unrecoverable error occured, -1 is returned. If the end of recording is reached,
 * because of a file size limit, -2 is returned, requiring subsequent stop and commit.
 * If buffer is NULL, just return the current state.
 *
 * @param mHandle the EAS synth handle (EAS_DATA_HANDLE)
 * @param fHandle the file to commit the recording.
 * @param buffer the buffer to receive the recorded bytes
 * @param offset the offset in buffer, where to start filling
 * @param count the number of bytes to fill at maximum into buffer
 * @return the number of bytes read into buffer, or the negative recording state
 *----------------------------------------------------------------------------
*/
EAS_I32 MMAPI_ReadRecordedBuffer(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle,
								 EAS_U8* buffer, EAS_I32 offset, EAS_I32 count);


/*----------------------------------------------------------------------------
 * Open the capture device, after opening the stream in mode CAPTURE.
 *
 * @param mHandle the EAS synth handle (EAS_DATA_HANDLE)
 * @param fHandle the opened file
 * @param encoding one of the CAPTURE_ENCODING_* constants
 * @param rate the sampling rate in Hz
 * @param bits the number of bits per sample, e.g. 8 nor 16
 * @param channels the number of channels, e.g. 1 for mono
 * @param isBigEndian if true, the capture device is opened in bigEndian
 *            (ignored for bits<=8)
 * @param isSigned if true, the capture device is opened with signed samples
 *            (ignored for bits<=8)
 * @return a file handle needed for subsequent calls
 *----------------------------------------------------------------------------
*/
EAS_BOOL MMAPI_OpenCapture(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle,
						   MMAPI_CAPTURE_ENCODING encoding, EAS_I32 rate, EAS_I32 bits,
						   EAS_I32 channels, EAS_BOOL isBigEndian, EAS_BOOL isSigned);


/* ---------------------------------------------------------------------------- */


/*
 * Additions to the host interface
 * -- a direct link between eas_mmapi.c and eas_mmapi_host.c
 */


/*----------------------------------------------------------------------------
 * A special function, in order to change the mode seamlessly,
 * so that EAS does not notice it.
 * This is used for the case that a media file completely fits into the
 * circular buffer used for the SREAMS mode.
 *
 * @param mHandle the EAS synth handle (EAS_DATA_HANDLE)
 * @param fHandle the handle to the open file (EAS_HANDLE)
 * @return an EAS error code
 *----------------------------------------------------------------------------
*/
EAS_RESULT MMAPI_HWSwitchToMemoryMode(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle);


/*----------------------------------------------------------------------------
 *
 * MMAPI_HWWriteFileImpl
 *
 * Write data to a memory file or stream. In lack of EAS_HWWriteFile,
 * this function is called directly from eas_mmapi.c.
 *
 * For MEMORY mode, data is appended to the media buffer. If the media
 * buffer is too small, it is enlarged before.
 *
 * For STREAM mode, data is written to the circular buffer, without
 * overwriting the read position. If the circular buffer is NULL,
 * it is allocated with a size of mb->bufferSize.
 *
 * The MMAPI implementation requires write operations to take place before
 * EAS_OpenFile(), so it cannot take the usual host handles (because they're
 * not yet set up).
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT MMAPI_HWWriteFileImpl(MMAPI_OPEN_MODE mode,
								 MMAPI_MediaBuffer* mb,
								 EAS_U8* buffer, EAS_I32 count,
								 EAS_I32 *pBytesWritten);


/*----------------------------------------------------------------------------
 *
 * MMAPI_HWWriteFile (master)
 *
 * Write data to a file
 *
 * Dispatch to the respective {Native|Memory|Stream} implementations.
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT MMAPI_HWWriteFile(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file,
						     void *pBuffer, EAS_I32 n, EAS_I32 *pBytesWritten);

/*----------------------------------------------------------------------------
 *
 * MMAPI_GetWaveInfo
 *
 * Get the format and header size from the file.
 * @param fmt [OUT] format header filled
 * @param headerSize [OUT] the total size of the headers at the beginning
 *        of the file, up to the beginning of the actual audio data
 * @param dataSize [OUT] number of audio data bytes, or -1 if not known.
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT MMAPI_GetWaveInfo(EAS_DATA_HANDLE pEASData, EAS_HANDLE handle,
						   WAVE_FMT_CHUNK* fmt, EAS_I32* headerSize, EAS_I32* dataSize);



/*
 * Mini-API for writing the rendered data to the audio device
 */

/* an opaque handle for the output layer */
typedef void* MMAPI_OUTPUT_HANDLE;

/*----------------------------------------------------------------------------
 * Open the output audio device
 * @return an opaque handle to the output device
 *----------------------------------------------------------------------------
*/
MMAPI_OUTPUT_HANDLE MMAPI_HWOutputCreate();

/*----------------------------------------------------------------------------
 * Close the output device
 * @param handle the opaque handle to the output device, retrieved with
 *               MMAPI_HWOutputCreate
 *----------------------------------------------------------------------------
*/
void MMAPI_HWOutputDestroy(MMAPI_OUTPUT_HANDLE handle);

/*----------------------------------------------------------------------------
 * Write rendered data to the output device.
 * This method does not block. If the device's buffers are all filled,
 * this method returns immediately with return value 0.
 *
 * @param handle the opaque handle to the output device, retrieved with
 *               MMAPI_HWOutputCreate
 * @param samples pointer to the samples rendered with EAS_Render()
 * @param count the number of samples to write
 * @return the number of samples written
 *----------------------------------------------------------------------------
*/
EAS_I32 MMAPI_HWOutput(MMAPI_OUTPUT_HANDLE handle, EAS_PCM* samples, EAS_I32 count);

/* ---------------------------------------------------------------------------- */


EAS_BOOL MMAPI_LoadDLSCollection(MMAPI_DATA_HANDLE mHandle, EAS_CHAR *locator);

#ifdef __cplusplus
}  /* end extern "C" */
#endif
#endif /* _EAS_MMAPI_INCLUDED */
