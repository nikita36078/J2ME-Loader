/*----------------------------------------------------------------------------
 *
 * File:
 * eas_mmapi.c
 *
 * Contents and purpose:
 * Main implementation file for the MMAPI implementation. It wraps the
 * MMAPI calls to EAS calls. It also introduces some abstractions for
 * the different file open modes, and it hides some of the configuration
 * options of EAS.
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

/*
 * TODO: use EAS_Pause on buffer underrun
 */

#include "eas_mmapi_config.h"
#include "eas_mmapi_types.h"
/* include MMAPI wrapper header */
#include "eas_mmapi.h"
/* include EAS headers */
#include "../sonivox/host_src/eas.h"
#include "../sonivox/host_src/eas_host.h"

#ifdef MMAPI_HAS_CAPTURE
/* include capture support */
#include "eas_wavein.h"
#endif

#ifdef SONIVOX_DEBUG
#include "../sonivox/host_src/eas_report.h"
#include <stdio.h>

/* uncomment to see debugging output in the MMAPI_Render function */
/* #define SONIVOX_DEBUG_RENDER */
/* uncomment to see debugging output for the GetState function */
/* #define SONIVOX_DEBUG_STATE */
/* uncomment to see extended debugging output for the GetDuration function */
/* #define SONIVOX_DEBUG_DURATION */
#endif

/* this module requires dynamic memory support */
#ifdef _STATIC_MEMORY
#error "the MMAPI implementation requires the dynamic memory model!\n"
#endif

/* value for duration before tried to get duration */
#define MMAPI_DURATION_UNINITIALIZED (-2)

/* value for duration if impossible to get duration for file */
#define MMAPI_DURATION_NOT_AVAILABLE (-1)

/* wrap EAS_DATA_HANDLE and the rendering buffer  */
typedef struct mmapi_data_struct_tag {
	/* handle to the EAS synth */
	EAS_DATA_HANDLE easHandle;
	/* handle to the output device */
	MMAPI_OUTPUT_HANDLE outputHandle;
	/* mix buffer for EAS_Render */
	EAS_PCM* mixBuffer;
	/* size of mixBuffer in samples */
	EAS_I32 mixBufferSize;
	/* how many samples filled in mixBuffer */
	EAS_I32 bufferFilled;
	/* how many samples written to output device */
	EAS_I32 written;
#ifdef MMAPI_HAS_CAPTURE
	/* if non-NULL, the active capture stream */
	MMAPI_FILE_STRUCT* captureStream;
#endif
} MMAPI_DATA_STRUCT;

/*
 * META DATA SUPPORT
 */

/* forward declaration for the meta data callback */
void MMAPI_AddMetaData(E_EAS_METADATA_TYPE type, EAS_CHAR* value, MMAPI_FILE_STRUCT* mfh);

/*
 * Recording support: forward declarations
 */
EAS_BOOL MMAPI_CommitRecording(MMAPI_DATA_STRUCT* mdh, MMAPI_FILE_STRUCT* mfh);
void MMAPI_CloseRecording(MMAPI_DATA_STRUCT* mdh, MMAPI_FILE_STRUCT* mfh);

#ifdef MMAPI_HAS_CAPTURE
/*
 * Capture support: forward declaration
 */
void MMAPI_HandleCapturedData(MMAPI_DATA_STRUCT* mdh, MMAPI_FILE_STRUCT* mfh);
#endif


/*
 * Support for interactive MIDI on MIDI file players: forward declarations
 */
EAS_HANDLE MMAPI_OpenInteractiveMIDI(MMAPI_DATA_STRUCT* mdh, MMAPI_FILE_STRUCT* mfh);
void MMAPI_CloseInteractiveMIDI(MMAPI_DATA_STRUCT* mdh, EAS_HANDLE midiHandle);


/*
 * Implementation of functions in eas_mmapi.h
 */


MMAPI_DATA_HANDLE MMAPI_Init() {
	const S_EAS_LIB_CONFIG* pConfig = EAS_Config();
	MMAPI_DATA_STRUCT* mdh;
	EAS_RESULT res = EAS_SUCCESS;
	EAS_I32 size;

#ifdef SONIVOX_DEBUG
	EAS_SetDebugLevel(MMAPI_DEBUG_EAS_DEBUG_LEVEL);
	EAS_SetDebugFile(stdout, EAS_TRUE /* flush after write */);
	EAS_Report(4, "> MMAPI_Init()\n");
#endif
	mdh = (MMAPI_DATA_STRUCT*) EAS_HWMalloc(NULL, sizeof(MMAPI_DATA_STRUCT));
	if (mdh != NULL) {
		EAS_HWMemSet(mdh, 0, sizeof(MMAPI_DATA_STRUCT));
#ifdef MMAPI_NATIVE_RENDERING_THREAD
#error "Native rendering thread is not implemented\n"
		/* TODO: implement native rendering thread */
#endif
		if (res == EAS_SUCCESS) {
			/* set up the mix buffer */
			/* convert mixBufferSize to bytes */
			mdh->mixBufferSize = pConfig->mixBufferSize * MMAPI_AUDIODEVICE_BUFFERS;
			size = sizeof(EAS_PCM) * mdh->mixBufferSize * pConfig->numChannels;
			mdh->mixBuffer = (EAS_PCM*) EAS_HWMalloc(NULL, size);
			res = (mdh->mixBuffer != NULL)?EAS_SUCCESS:EAS_FAILURE;
		}
		if (res == EAS_SUCCESS) {
			mdh->outputHandle = MMAPI_HWOutputCreate();
			res = (mdh->outputHandle != NULL)?EAS_SUCCESS:EAS_FAILURE;
		}
		if (res == EAS_SUCCESS) {
			res = EAS_Init(&(mdh->easHandle));
			#ifdef SONIVOX_DEBUG
			if (res == EAS_SUCCESS) {
				EAS_Report(5, "   easHandle=%p\n", (void*) mdh->easHandle);
			}
			#endif
		}
		if (res != EAS_SUCCESS) {
			MMAPI_Shutdown(mdh);
			mdh = NULL;
		}
	}
#ifdef SONIVOX_DEBUG
	EAS_Report(4, "< MMAPI_Init\n");
#endif
	return mdh;
}



void MMAPI_Shutdown(MMAPI_DATA_HANDLE mHandle) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
#ifdef SONIVOX_DEBUG
	EAS_Report(4, "MMAPI_Shutdown: mHandle=%p\n", (void*) mdh);
#endif
	if (mdh != NULL) {
		if (mdh->easHandle != NULL) {
			EAS_Shutdown(mdh->easHandle);
			/* if Shutdown is not successful, there is nothing we can do... */
			mdh->easHandle = NULL;
		}
		if (mdh->outputHandle) {
			MMAPI_HWOutputDestroy(mdh->outputHandle);
			mdh->outputHandle = NULL;
		}
		if (mdh->mixBuffer) {
			EAS_HWFree(NULL, mdh->mixBuffer);
			mdh->mixBuffer = NULL;
		}
		EAS_HWFree(NULL, mdh);
	}
}


/*
 * Internal function to actually open the EAS file.
 * This function is called from MMAPI_OpenFile() for NATIVE or INTERACTIVE
 * locators, and from MMAPI_WriteBuffer() for STREAM, MEMORY, and TONE
 * locators.
 */
EAS_RESULT MMAPI_OpenFileImpl(MMAPI_DATA_STRUCT* mdh, MMAPI_FILE_STRUCT* mfh, EAS_CHAR* locator) {
	EAS_RESULT res = -1;

	#ifdef SONIVOX_DEBUG
	EAS_Report(5, "  OpenFileImpl(mdh=%p, mfh=%p, locator=%p)\n", (void*) mdh, (void*) mfh, (void*) locator);
	#endif

	if (mfh->mode == MMAPI_OPEN_MODE_NATIVE) {
		mfh->locator = locator;
		#ifdef MMAPI_USE_ORIGINAL_HOST
			/* skip the file:// protocol */
			locator += 7;
			res = EAS_OpenFile(mdh->easHandle, (EAS_FILE_LOCATOR) locator, &(mfh->handle));
		#else
			res = EAS_OpenFile(mdh->easHandle, (EAS_FILE_LOCATOR) mfh, &(mfh->handle));
		#endif
		/* must not use the locator after this function terminates */
		mfh->locator = NULL;

	} else if (mfh->mode == MMAPI_OPEN_MODE_STREAM) {
		res = EAS_OpenFile(mdh->easHandle, (EAS_FILE_LOCATOR) mfh, &(mfh->handle));
	} else if (mfh->mode == MMAPI_OPEN_MODE_MEMORY) {
		res = EAS_OpenFile(mdh->easHandle, (EAS_FILE_LOCATOR) mfh, &(mfh->handle));
	} else if (mfh->mode == MMAPI_OPEN_MODE_INTERACTIVE_MIDI) {
		/* open MIDI stream in interactive mode only */
		res = EAS_OpenMIDIStream(mdh->easHandle, &(mfh->handle), NULL);
#ifdef MMAPI_HAS_TONE_CONTROL
	} else if (mfh->mode == MMAPI_OPEN_MODE_TONE_SEQUENCE) {
		/* open tone sequence. This requires MEMORY mode */
		mfh->mode = MMAPI_OPEN_MODE_MEMORY;
		res = EAS_MMAPIToneControl(mdh->easHandle, (EAS_FILE_LOCATOR) mfh, &(mfh->handle));
		#ifdef SONIVOX_DEBUG
		EAS_Report(4, "opening tone control returned %d\n", res);
		#ifdef MMAPI_DEBUG_WRITE_TONE_SEQUENCE
		if (mfh->mb == NULL) {
			EAS_Report(2, "mb=NULL!\n");
		} else if (mfh->mb->buffer == NULL) {
			EAS_Report(2, "mb->buffer=NULL!\n");
		} else if (res == EAS_SUCCESS) {
			FILE* f;
			EAS_Report(4, "opening C:\\tonesequence.jts...\n");
			f = fopen("C:\\tonesequence.jts", "wb");
			if (f) {
				EAS_Report(4, "writing C:\\tonesequence.jts. buffer=%p, bufferFilled=%d\n",
					mfh->mb->buffer, mfh->mb->bufferFilled);
				fwrite(mfh->mb->buffer, mfh->mb->bufferFilled, 1, f);
				fclose(f);
			}
		}
		#endif
		#endif
#endif
#ifdef MMAPI_HAS_CAPTURE
	} else if (mfh->mode == MMAPI_OPEN_MODE_CAPTURE) {
		/* open the host implementation in stream mode */
		mfh->mode = MMAPI_OPEN_MODE_STREAM;
		res = EAS_OpenFile(mdh->easHandle, (EAS_FILE_LOCATOR) mfh, &(mfh->handle));
		mfh->mode = MMAPI_OPEN_MODE_CAPTURE;
#endif
	}
	#ifdef SONIVOX_DEBUG
	if (res == EAS_SUCCESS) {
		EAS_Report(5, "   mfh->handle=%p\n", (void*) mfh->handle);
	}
	#endif

	return res;
}



MMAPI_FILE_HANDLE MMAPI_OpenFile(MMAPI_DATA_HANDLE mHandle,
							EAS_CHAR* locator,
							MMAPI_OPEN_MODE mode) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh;
	EAS_RESULT res = EAS_SUCCESS;

	#ifdef SONIVOX_DEBUG
		char* openMode = "MODE_UNKNOWN";
		switch (mode) {
		case MMAPI_OPEN_MODE_NATIVE: openMode = "NATIVE"; break;
		case MMAPI_OPEN_MODE_MEMORY: openMode = "MEMORY"; break;
		case MMAPI_OPEN_MODE_STREAM: openMode = "STREAM"; break;
		case MMAPI_OPEN_MODE_INTERACTIVE_MIDI: openMode = "INTERACTIVE"; break;
		case MMAPI_OPEN_MODE_TONE_SEQUENCE: openMode = "TONE_SEQUENCE"; break;
		case MMAPI_OPEN_MODE_CAPTURE: openMode = "CAPTURE"; break;
		}
		EAS_Report(4, "> MMAPI_OpenFile: mHandle=%p locator=%s  open mode=%d = %s\n",
			(void*) mdh, (char*) locator, (int) mode, openMode);
	#endif


	#ifdef MMAPI_USE_ORIGINAL_HOST
		if (mode != MMAPI_OPEN_MODE_NATIVE && mode != MMAPI_OPEN_MODE_INTERACTIVE_MIDI) {
			return NULL;
		}
	#endif

	mfh = (MMAPI_FILE_STRUCT*) EAS_HWMalloc(NULL, sizeof(MMAPI_FILE_STRUCT));
	if (mfh != NULL) {
		EAS_HWMemSet(mfh, 0, sizeof(MMAPI_FILE_STRUCT));
		mfh->mode = mode;
		/* initialize the duration to not known */
		mfh->duration = MMAPI_DURATION_UNINITIALIZED;
		/*
		 * for NATIVE and INTERACTIVE modes, open directly.
		 * MEMORY, STREAM, TONE will open the EAS file in the WriteBuffer() function.
		 * CAPTURE will open the device in the MMAPI_OpenCapture function.
		 * This is necessary, because EAS_OpenFile() already reads the file.
		 */
		if (mode == MMAPI_OPEN_MODE_NATIVE || mode == MMAPI_OPEN_MODE_INTERACTIVE_MIDI) {
			res = MMAPI_OpenFileImpl(mdh, mfh, locator);
		} else {
			/* for MEMORY, STREAM, TONE, and CAPTURE, set up the media buffer struct */
			#ifdef SONIVOX_DEBUG
				EAS_Report(4, "  MMAPI_OpenFile: creating media buffer\n");
			#endif

			mfh->mb = (MMAPI_MediaBuffer*) EAS_HWMalloc(NULL, sizeof(MMAPI_MediaBuffer));
			if (mfh->mb == NULL) {
				res = EAS_ERROR_MALLOC_FAILED;
			} else {
				EAS_HWMemSet(mfh->mb, 0, sizeof(MMAPI_MediaBuffer));
			}
		}
		#ifdef MMAPI_HAS_CAPTURE
		if (res == EAS_SUCCESS && mode == MMAPI_OPEN_MODE_CAPTURE) {
			/* we will not be able to provide duration */
			mfh->duration = MMAPI_DURATION_NOT_AVAILABLE;
		}
		#endif

		if (res != EAS_SUCCESS) {
			EAS_HWFree(NULL, mfh);
			if (res >= -100 && res < 0) {
				mfh = (void*) res;
			} else {
				mfh = NULL;
			}
			#ifdef SONIVOX_DEBUG
			EAS_Report(_EAS_SEVERITY_ERROR, "MMAPI_OpenFile: ERROR: error code=%d\n", (int) res);
			#endif
		}

	}
	#ifdef SONIVOX_DEBUG
	if (res == EAS_SUCCESS) {
		EAS_Report(4, "< MMAPI_OpenFile -> fileHandle=%p, EAS_HANDLE=%p\n", (void*) mfh, (void*) mfh->handle);
	} else {
		EAS_Report(4, "< MMAPI_OpenFile (error)\n");
	}
	#endif
	return mfh;
}

/*
 * For STREAM mode, discard all STREAM data and reposition to 0.
 * Ignore for other modes.
 */
void MMAPI_ReOpenFile(MMAPI_DATA_STRUCT* mdh, MMAPI_FILE_STRUCT* mfh) {
	if (mfh->mode != MMAPI_OPEN_MODE_STREAM) {
		return;
	}
	if (mfh->mb != NULL) {
		EAS_CloseFile(mdh->easHandle, mfh->handle);
#ifdef SONIVOX_DEBUG
		EAS_Report(4, "MMAPI_ReOpenFile.\n");
#endif
		mfh->handle = NULL;
		mfh->mb->bufferFilled = 0;
		mfh->mb->readPos = 0;
		mfh->mb->readPosOffset = 0;
		mfh->mb->writePos = 0;
	}
}



void MMAPI_CloseFile(MMAPI_DATA_HANDLE mHandle,
					 MMAPI_FILE_HANDLE fHandle) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;
#ifdef SONIVOX_DEBUG
	EAS_Report(4, "> MMAPI_CloseFile: mHandle=%p fHandle=%p\n",
		(void*) mdh, (void*) mfh);
#endif
	if (mdh != NULL && mfh != NULL) {
		if (mfh->handle != NULL) {
#ifdef SONIVOX_DEBUG
			EAS_Report(4, "EAS_CloseFile...\n");
#endif
			if (mfh->mode == MMAPI_OPEN_MODE_INTERACTIVE_MIDI) {
				EAS_CloseMIDIStream(mdh->easHandle, mfh->handle);
			} else {
				EAS_CloseFile(mdh->easHandle, mfh->handle);
			}
			#ifdef MMAPI_HAS_CAPTURE
			if (mdh->captureStream == mfh) {
				mdh->captureStream = NULL;
			}
			if (mfh->captureDevice != NULL) {
				CloseWaveInDevice(mfh->captureDevice);
				mfh->captureDevice = NULL;
			}
			#endif
		}
		if (mfh->mb != NULL) {
			if (mfh->mb->buffer != NULL) {
				#ifdef SONIVOX_DEBUG
				EAS_Report(4, "free buffer...\n");
				#endif
				EAS_HWFree(NULL, mfh->mb->buffer);
			}
			#ifdef SONIVOX_DEBUG
			EAS_Report(4, "free media buffer struct...\n");
			#endif
			EAS_HWFree(NULL, mfh->mb);
		}
		if (mfh->record.mb != NULL) {
			if (mfh->record.mb->buffer != NULL) {
				#ifdef SONIVOX_DEBUG
				EAS_Report(4, "free recording buffer...\n");
				#endif
				EAS_HWFree(NULL, mfh->record.mb->buffer);
			}
			#ifdef SONIVOX_DEBUG
			EAS_Report(4, "free recording media buffer struct...\n");
			#endif
			EAS_HWFree(NULL, mfh->record.mb);
		}


		/* if CloseFile is not successful, there is nothing we can do... */
#ifdef SONIVOX_DEBUG
		EAS_Report(4, "free file struct...\n");
#endif
		EAS_HWFree(NULL, mfh);
	}
#ifdef SONIVOX_DEBUG
	EAS_Report(4, "< MMAPI_CloseFile\n");
#endif
}


EAS_I32 MMAPI_WriteBuffer(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle,
						  EAS_U8* buffer, EAS_I32 offset, EAS_I32 count,
						  EAS_I32 totalLength, EAS_I32 flags) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;
	EAS_RESULT res = EAS_SUCCESS;
	EAS_I32 returnCount = 0;
	MMAPI_OPEN_MODE writeMode = mfh->mode;

	#ifdef SONIVOX_DEBUG
	EAS_Report(5, "MMAPI_WriteBuffer: mHandle=%p fHandle=%p count=%d, totalLength=%d, flags=%d\n",
		(void*) mdh, (void*) mfh, (int) count, (int) totalLength, (int) flags);
	#endif

	/* for writing, tone sequence is equivalent to MEMORY */
	if (writeMode == MMAPI_OPEN_MODE_TONE_SEQUENCE) {
		writeMode = MMAPI_OPEN_MODE_MEMORY;
	}
	/* capture is equivalent to STREAM */
	if (writeMode == MMAPI_OPEN_MODE_CAPTURE) {
		writeMode = MMAPI_OPEN_MODE_STREAM;
	}

	if (writeMode == MMAPI_OPEN_MODE_INTERACTIVE_MIDI || (flags & MMAPI_WRITE_FLAG_INTERACTIVE_MIDI)) {
		if (flags & MMAPI_WRITE_FLAG_INTERACTIVE_MIDI) {
			/* fHandle is a direct handle to the EAS MIDI stream opened with MMAPI_OpenInteractiveMIDI() */
			res = EAS_WriteMIDIStream(mdh->easHandle, (EAS_HANDLE) fHandle, buffer, count);
		} else {
			res = EAS_WriteMIDIStream(mdh->easHandle, mfh->handle, buffer, count);
		}
		if (res >= EAS_SUCCESS) {
			return count;
		}
		return res;
	} else if (writeMode != MMAPI_OPEN_MODE_MEMORY
			&& writeMode != MMAPI_OPEN_MODE_STREAM) {
		/* sanity check */
		return EAS_ERROR_INVALID_PARAMETER;
	}

	/* apply offset */
	buffer += offset;

	if ((writeMode == MMAPI_OPEN_MODE_STREAM)
			&& (mfh->mb->buffer == NULL)) {
		/* initialize the circular buffer size */
		mfh->mb->bufferSize = MMAPI_STREAM_CIRCULAR_BUFFER_SIZE;
	}

	/* set the totalSize field */
	mfh->mb->totalSize = totalLength;

	/* do the actual write operation */
	res = MMAPI_HWWriteFileImpl(writeMode, mfh->mb, buffer, count, &returnCount);

	/*
	 * a special exploit:
	 * if the entire streaming file fits into the
	 * STREAM buffer, switch to MEMORY mode.
	 *
	 * For that, the file is directly closed in the host
	 * interface ("behind the back of EAS") and re-opened
	 * in MEMORY mode. EAS will not notice any difference,
	 * since the handle provided to EAS remains the same.
	 */
	if (res == EAS_SUCCESS
			&& writeMode == MMAPI_OPEN_MODE_STREAM
			&& mfh->mb->totalSize >= 0
			&& mfh->mb->totalSize <= mfh->mb->bufferSize
			/* bufferFilled is reduced after reading the first bytes */
			/* && mfh->mb->bufferFilled >= mfh->mb->totalSize*/ ) {
		/* switch to MEMORY mode */
		mfh->mode = MMAPI_OPEN_MODE_MEMORY;
		if (mfh->hwFileHandle != NULL) {
			/* if it was already open, re-open the native file directly */
			res = MMAPI_HWSwitchToMemoryMode(mfh->hwInstData, mfh->hwFileHandle);
			#ifdef SONIVOX_DEBUG
			if (res == EAS_SUCCESS) {
				EAS_Report(4, "MMAPI_WriteBuffer: entire file fits into buffer, switch mode to MEMORY.\n");
			} else {
				EAS_Report(1, "MMAPI_WriteBuffer: ERROR: switching mode to MEMORY failed!\n");
			}
			#endif
		}
	}

	/* after the first STREAM nWrite call, or after the
	 * last MEMORY/TONE nWrite call, actually open the EAS file */
	if (res == EAS_SUCCESS && mfh->handle == NULL) {
		if (!(flags & MMAPI_WRITE_FLAG_MORE_COMING) || writeMode == MMAPI_OPEN_MODE_STREAM) {
			res = MMAPI_OpenFileImpl(mdh, mfh, NULL);
		}
	}

	if (res != EAS_SUCCESS) {
		#ifdef SONIVOX_DEBUG
		EAS_Report(_EAS_SEVERITY_ERROR, "MMAPI_WriteBuffer: ERROR: error code=%d\n", (int) res);
		#endif
		/* safeguard against positive error codes */
		if (res > 0) {
			res = -1;
		}
		return res;
	}
	return returnCount;
}



EAS_I32 MMAPI_GeneralCommand(MMAPI_DATA_HANDLE mHandle,
							 MMAPI_FILE_HANDLE fHandle,
							 MMAPI_COMMAND_CODE commandCode,
							 EAS_I32 param) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;
#ifdef SONIVOX_DEBUG
	EAS_Report(5, "MMAPI_GeneralCommand %d: param=%d mHandle=%p, fHandle=%p\n",
		commandCode, param, mHandle, fHandle);
#endif
	if (mdh == NULL || mfh == NULL) {
		#ifdef SONIVOX_DEBUG
		EAS_Report(1, "MMAPI_GeneralCommand: illegal handles: easHandle=%p, fileHandle=%p\n", mdh, mfh);
		#endif
		return -1;
	}
	switch (commandCode) {
	case MMAPI_COMMAND_GET_MODE: return mfh->mode;
	/* static void startRecording(int fileHandle) */
	case MMAPI_COMMAND_START_RECORDING:
		if (mfh->record.state >= 0) {
			mfh->record.state = MMAPI_RS_RECORDING;
		}
		return 0;
	/* static void stopRecording(int fileHandle) */
	case MMAPI_COMMAND_STOP_RECORDING:
		if (mfh->record.state >= 0) {
			mfh->record.state = MMAPI_RS_NONE;
		}
		return 0;
	/* static boolean commitRecording(int fileHandle) */
	case MMAPI_COMMAND_COMMIT_RECORDING:
		return MMAPI_CommitRecording(mdh, mfh);
	/* static void closeRecording(int fileHandle) */
	case MMAPI_COMMAND_CLOSE_RECORDING:
		MMAPI_CloseRecording(mdh, mfh);
		return 0;
	/* static int getRecordingState(int fileHandle) */
	case MMAPI_COMMAND_GET_RECORDING_STATE:
		return MMAPI_ReadRecordedBuffer(mHandle, fHandle, NULL, 0, 0);
	/* static void setRecordSizeLimit(int fileHandle, int limit) */
	case MMAPI_COMMAND_LIMIT_RECORDING:
		mfh->record.sizeLimit = param;
		return 0;
#ifdef MMAPI_HAS_TEMPO_CONTROL
	case MMAPI_COMMAND_SET_TEMPO:
		res = EAS_SetTempo(mdh->easHandle, mfh->handle, param);
		/* fall through */
	case MMAPI_COMMAND_GET_TEMPO:
		param = -1;
		res = EAS_GetTempo(mdh->easHandle, mfh->handle, &param);
		return (res == EAS_SUCCESS)?param:-1;
#endif
	case MMAPI_COMMAND_REOPEN:
		MMAPI_ReOpenFile(mdh, mfh);
		return 0;
	case MMAPI_OPEN_INTERACTIVE_MIDI:
		return (EAS_I32) MMAPI_OpenInteractiveMIDI(mdh, mfh);
	case MMAPI_CLOSE_INTERACTIVE_MIDI:
		MMAPI_CloseInteractiveMIDI(mdh, (EAS_HANDLE) param);
		return 0;
	}
	#ifdef SONIVOX_DEBUG
	EAS_Report(1, "MMAPI_GeneralCommand: unknown command code=%d.\n", (int) commandCode);
	#endif
	return -1;
}



EAS_BOOL MMAPI_Prepare(MMAPI_DATA_HANDLE mHandle,
					   MMAPI_FILE_HANDLE fHandle) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;
	EAS_RESULT res;
	#ifdef SONIVOX_DEBUG
	EAS_RESULT metaRes;
	#endif
	const S_EAS_LIB_CONFIG* pConfig = EAS_Config();

	#ifdef SONIVOX_DEBUG
	EAS_Report(4, "MMAPI_Prepare: mHandle=%p fHandle=%p\n",
		(void*) mdh, (void*) mfh);
	#endif
	if (mfh->mode == MMAPI_OPEN_MODE_INTERACTIVE_MIDI) {
		return EAS_TRUE;
	}

	/* for MEMORY and STREAM modes, need to verify that WriteBuffer() was called */
	if (mfh->handle == NULL) {
		#ifdef SONIVOX_DEBUG
		EAS_Report(1, "MMAPI_Prepare: internal error: file is not open. "
			"Possibly MMAPI_WriteBuffer wasn't called?\n");
		#endif
		return EAS_FALSE;
	}

	res = EAS_Prepare(mdh->easHandle, mfh->handle);
	if (res == EAS_SUCCESS
		&& mfh->mode != MMAPI_OPEN_MODE_INTERACTIVE_MIDI
		&& mfh->mode != MMAPI_OPEN_MODE_CAPTURE) {
		/* NOTE: do not evaluate the return value -- shouldn't
		 * cause a failure only because meta data cannot be initialized. */
		/* Use the pointer to MMAPI_FILE_STRUCT as user pointer */
		#ifdef SONIVOX_DEBUG
		EAS_Report(5, "MMAPI_Prepare: registering meta data callback, state=%d\n", MMAPI_GetState(mHandle, fHandle));
		metaRes =
		#endif
		EAS_RegisterMetaDataCallback(mdh->easHandle, mfh->handle,
			MMAPI_AddMetaData,
			&(mfh->metaDataBuffer[0]),
			MMAPI_METADATA_BUFFER_SIZE,
			mfh);
		#ifdef SONIVOX_DEBUG
		if (metaRes != EAS_SUCCESS) {
			EAS_Report(4, "registering meta data callback (state=%d) returned %d\n",
				MMAPI_GetState(mHandle, fHandle), metaRes);
		}
		#endif
	}
	if (res == EAS_SUCCESS
		&& mfh->mode != MMAPI_OPEN_MODE_INTERACTIVE_MIDI) {
		/* it seems that EAS starts playing directly after prefetching the file! */
		#ifdef SONIVOX_DEBUG
		EAS_Report(5, "MMAPI_Prepare: state=%d, calling EAS_Pause\n", MMAPI_GetState(mHandle, fHandle));
		metaRes =
		#endif
		EAS_Pause(mdh->easHandle, mfh->handle);
		#ifdef SONIVOX_DEBUG
		if (metaRes != EAS_SUCCESS) {
			EAS_Report(4, "EAS_Pause returned %d (state=%d)\n", metaRes, MMAPI_GetState(mHandle, fHandle));
		}
		#endif
	}

	/* cause recalculation of duration */
	mfh->duration = MMAPI_DURATION_UNINITIALIZED;

	#ifdef SONIVOX_DEBUG
	if (res != EAS_SUCCESS) {
		EAS_Report(1, "MMAPI_Prepare: error=%d. state=%d\n", (int) res, MMAPI_GetState(mHandle, fHandle));
	}
	#endif
	return (res == EAS_SUCCESS);
}

EAS_BOOL MMAPI_Resume(MMAPI_DATA_HANDLE mHandle,
					  MMAPI_FILE_HANDLE fHandle) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;
	EAS_RESULT res = EAS_SUCCESS;
	EAS_STATE state = EAS_STATE_READY;

#ifdef SONIVOX_DEBUG
	EAS_Report(5, "MMAPI_Resume()\n");
#endif
	#ifdef MMAPI_HAS_CAPTURE
	if (mfh->captureDevice != NULL) {
		if (mdh->captureStream != NULL) {
			res = EAS_FAILURE;
			#ifdef SONIVOX_DEBUG
			EAS_Report(2, "MMAPI_Resume(): ERROR another capture stream is already active!\n");
			#endif
		} else
		if (StartWaveInCapture(mfh->captureDevice)) {
			mdh->captureStream = mfh;
		} else {
			res = EAS_FAILURE;
			#ifdef SONIVOX_DEBUG
			EAS_Report(2, "MMAPI_Resume: error in StartWaveInCapture()!\n");
			#endif
		}
	}
	#endif

	if (res == EAS_SUCCESS) {
		EAS_State(mdh->easHandle, mfh->handle, &state);
		if (state == EAS_STATE_PAUSED || state == EAS_STATE_PAUSING) {
			/* if in PAUSING state, EAS_Resume() will probably cause
			 * an error, but at least we consistently report an error
			 * if unable to resume */
			res = EAS_Resume(mdh->easHandle, mfh->handle);
		}
	}
#ifdef SONIVOX_DEBUG
	if (res != EAS_SUCCESS) {
		EAS_Report(2, "MMAPI_Resume: state=%d, res=%d...\n", MMAPI_GetState(mHandle, fHandle), (int) res);
	}
#endif
	return (res == EAS_SUCCESS);
}

EAS_BOOL MMAPI_Pause(MMAPI_DATA_HANDLE mHandle,
					 MMAPI_FILE_HANDLE fHandle) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;
	EAS_RESULT res = EAS_SUCCESS;
	EAS_STATE state = EAS_STATE_READY;

#ifdef SONIVOX_DEBUG
	EAS_Report(5, "MMAPI_Pause()\n");
#endif
	#ifdef MMAPI_HAS_CAPTURE
	if (mfh->captureDevice != NULL) {
		StopWaveInCapture(mfh->captureDevice);
		mdh->captureStream = NULL;
	}
	#endif

	EAS_State(mdh->easHandle, mfh->handle, &state);
	if (state != EAS_STATE_PAUSED && state != EAS_STATE_PAUSING) {
		res = EAS_Pause(mdh->easHandle, mfh->handle);
	}
#ifdef SONIVOX_DEBUG
	if (res != EAS_SUCCESS) {
		EAS_Report(2, "MMAPI_Pause: res=%d...\n", (int) res);
	}
#endif
	return (res == EAS_SUCCESS);
}


EAS_BOOL MMAPI_Render(MMAPI_DATA_HANDLE mHandle) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	/* FIXME: is it better to cache the pConfig values in mdh? */
	const S_EAS_LIB_CONFIG* pConfig = EAS_Config();
	EAS_I32 count = 0;
	EAS_I32 requested;
	EAS_PCM* p;
	EAS_RESULT res = EAS_SUCCESS;

#ifdef SONIVOX_DEBUG_RENDER
	EAS_Report(5, "MMAPI_Render: mHandle=%p...", (void*) mdh);
#endif

#ifdef MMAPI_HAS_CAPTURE
	/* does any capture data need to be transferred to the stream? */
	if (mdh->captureStream != NULL) {
		MMAPI_HandleCapturedData(mdh, mdh->captureStream);
	}
#endif

	/* if the buffer was only filled partially the last time we called
	 * Render, continue */
	requested = mdh->mixBufferSize - mdh->bufferFilled;
	if (requested > 0) {
		/* adjust buffer pointer for partial buffers */
		p = mdh->mixBuffer;
		p += (mdh->bufferFilled * pConfig->numChannels);
		if (requested > pConfig->mixBufferSize) {
			requested = pConfig->mixBufferSize;
		}
		res = EAS_Render(mdh->easHandle, p, requested, &count);
		mdh->bufferFilled += count;
		#ifdef SONIVOX_DEBUG_RENDER
		EAS_Report(5, "rendered %d of %d samples...", (int) count, (int) requested);
		#endif
	}
	if (mdh->bufferFilled >= mdh->mixBufferSize) {
		/* write to audio device */
		p = mdh->mixBuffer;
		p += (mdh->written * pConfig->numChannels);
		count = mdh->bufferFilled - mdh->written;
		if (count > 0) {
			count = MMAPI_HWOutput(mdh->outputHandle, p, count);
			mdh->written += count;
			#ifdef SONIVOX_DEBUG_RENDER
			EAS_Report(5, " written %d samples to audio device...", (int) count);
			#endif
		}
		if (mdh->written >= mdh->bufferFilled) {
			/* writing of the mix buffer completed. Now render the next buffer */
			mdh->bufferFilled = 0;
			mdh->written = 0;
		}
	}
#ifdef SONIVOX_DEBUG_RENDER
	if (res == EAS_SUCCESS) {
		EAS_Report(5, "OK.\n");
	} else {
		EAS_Report(5, "ERROR code=%d.\n", res);
	}
#endif
	return (res == EAS_SUCCESS);
}


EAS_I32 MMAPI_GetState(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;
	EAS_STATE state = 0;
	EAS_RESULT res;

#ifdef SONIVOX_DEBUG_STATE
	EAS_Report(5, "MMAPI_GetState: mHandle=%p fHandle=%p...", (void*) mdh, (void*) mfh);
#endif
	res = EAS_State(mdh->easHandle, mfh->handle, &state);
#ifdef SONIVOX_DEBUG_STATE
	if (res == EAS_SUCCESS) {
		EAS_Report(5, "%d\n", state);
	} else {
		EAS_Report(5, "ERROR code=%d.\n", res);
	}
#endif
	if (res == EAS_SUCCESS) {
		return (EAS_I32) state;
	}
	return -1;
}


EAS_I32 MMAPI_GetLocation(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;
	EAS_I32 millis = -1;
	EAS_RESULT res = EAS_GetLocation(mdh->easHandle, mfh->handle, &millis);
	if (res == EAS_SUCCESS) {
		return millis;
	}
	return -1;
}


EAS_I32 MMAPI_Locate(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle, EAS_I32 millis) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;
	EAS_I32 state;

	/* work-around for stream mode: disallow any seeking, except for rewinding to beginning */
	/* $$fb is done in Java layer */
	/* if (millis != 0 && mfh->mode == MMAPI_OPEN_MODE_STREAM) {
		return -1;
	} */

	if (mfh->mode != MMAPI_OPEN_MODE_CAPTURE) {
		state = MMAPI_GetState(mHandle, fHandle);
		if (EAS_Locate(mdh->easHandle, mfh->handle, millis, EAS_FALSE) == EAS_SUCCESS) {
			if (state != EAS_STATE_PLAY) {
				/* if EAS_Locate is called from STOPPED state, it will start playback! */
				EAS_Pause(mdh->easHandle, mfh->handle);
			}
			return millis;
		}
	}
	return -1;
}


EAS_BOOL MMAPI_SetVolume(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle, EAS_I32 level) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;

	if (EAS_SetVolume(mdh->easHandle, mfh->handle, level) == EAS_SUCCESS) {
		return EAS_TRUE;
	}
	return EAS_FALSE;
}


EAS_I32 MMAPI_GetVolume(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;

	return EAS_GetVolume(mdh->easHandle, mfh->handle);
}


EAS_BOOL MMAPI_SetRepeat(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle, EAS_I32 repeatCount) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;

	if (mfh->mode != MMAPI_OPEN_MODE_CAPTURE) {
		if (EAS_SetRepeat(mdh->easHandle, mfh->handle, repeatCount) == EAS_SUCCESS) {
			return EAS_TRUE;
		}
	}
	return EAS_FALSE;
}


EAS_I32 MMAPI_GetCurrentRepeat(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;
	EAS_I32 repeat = 0;
	if (EAS_GetRepeat(mdh->easHandle, mfh->handle, &repeat) != EAS_SUCCESS) {
		return -1;
	}
	return repeat;
}


EAS_I32 MMAPI_SetPlaybackRate(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle, EAS_I32 rate) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;

	if (mfh->mode != MMAPI_OPEN_MODE_CAPTURE) {
		if (EAS_SetPlaybackRate(mdh->easHandle, mfh->handle, rate) == EAS_SUCCESS) {
			return rate;
		}
	}
	return 0x10000000; /* no rate change */
}


EAS_I32 MMAPI_SetTransposition(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle, EAS_I32 transpose) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;

	if (EAS_SetTransposition(mdh->easHandle, mfh->handle, transpose) == EAS_SUCCESS) {
		return transpose;
	}
	return 0; /* no pitch change */
}


/* internal function to add a meta data entry */
void MMAPI_AddMetaData(E_EAS_METADATA_TYPE type, EAS_CHAR* value, MMAPI_FILE_STRUCT* mfh) {
	MMAPI_MetaData* md, *ins;
	int len = 0;
	EAS_CHAR* temp = value;

#ifdef SONIVOX_DEBUG
	EAS_Report(4, "MMAPI_AddMetaData: type=%d, value=%s\n", (int) type, (char*) value);
#endif
	if (mfh == NULL) return;

	/* TODO: detect duplicates and overwrite the older version */

	/* get length of value */
	while ((*temp) != NULL) { temp++; len++; }
	if (len == 0) return;

	/* create the entry structure: MMAPI_MetaData followed by string data */
	md = (MMAPI_MetaData*) EAS_HWMalloc(NULL, len + sizeof(MMAPI_MetaData));
	if (md != NULL) {
		md->next = NULL;
		md->type = type;
		EAS_HWMemCpy(md->value, value, len+1);
		/* append it to the linked list */
		if (mfh->metaData == NULL) {
			/* add the first entry */
			mfh->metaData = md;
		} else {
			/* add as last entry in the list */
			ins = mfh->metaData;
			while (ins->next != NULL) ins = ins->next;
			ins->next = md;
		}
	} else {
#ifdef SONIVOX_DEBUG
		EAS_Report(1, "MMAPI_AddMetaData: out of memory for meta data string of length: %d \n", len);
#endif
	}
}

MMAPI_MetaData* MMAPI_GetMetaData(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;

	/* if not yet done, try to get some meta data from the file */
	MMAPI_GetDuration(mHandle, fHandle);

	return mfh->metaData;
}


void MMAPI_NextMetaData(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle) {
	/* no use for mHandle */
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;
	MMAPI_MetaData* md = mfh->metaData;

	if (md) {
		mfh->metaData = md->next;
		EAS_HWFree(NULL, md);
	}
}


EAS_I32 MMAPI_GetDuration(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;
	EAS_I32 state;
	EAS_RESULT res;

	#ifdef SONIVOX_DEBUG_DURATION
	EAS_Report(4, "> MMAPI_GetDuration: old mfh->duration=%d milliseconds\n", mfh->duration);
	#endif
	if (mfh->mode == MMAPI_OPEN_MODE_MEMORY
		|| mfh->mode == MMAPI_OPEN_MODE_TONE_SEQUENCE
		|| mfh->mode == MMAPI_OPEN_MODE_NATIVE) {
		if (mfh->duration == MMAPI_DURATION_UNINITIALIZED) {
			state = MMAPI_GetState(mHandle, fHandle);
			if (state != -1
				&& state != EAS_STATE_PLAY
				&& state != EAS_STATE_ERROR
				&& state != EAS_STATE_PAUSING
				&& state != EAS_STATE_OPEN
				&& state != EAS_STATE_STOPPING) {

				res = EAS_ParseMetaData(mdh->easHandle, mfh->handle, &(mfh->duration));
				#ifdef SONIVOX_DEBUG
				if (res == EAS_SUCCESS) {
					EAS_Report(4, "MMAPI_GetDuration: state=%d, duration=%d milliseconds\n", state, mfh->duration);
				} else {
					EAS_Report(1, "MMAPI_GetDuration: state=%d, cannot get duration. Error: %d.\n", state, res);
				}
				#endif
#ifdef MMAPI_DURATION_WORKAROUND
				if (res == EAS_SUCCESS && mfh->duration == 0) {
					mfh->duration = MMAPI_DURATION_NOT_AVAILABLE;
					#ifdef SONIVOX_DEBUG
					EAS_Report(2, "MMAPI_GetDuration: workaround, set duration to NOT_AVAILABLE.\n");
					#endif
				}
#endif
				if (state == EAS_STATE_PAUSED) {
					/* if EAS_ParseMetaData is called from PAUSED state, it will start playback! */
					#ifdef SONIVOX_DEBUG
					EAS_Report(4, "MMAPI_GetDuration: Need to call pause again...\n");
					#endif
					EAS_Pause(mdh->easHandle, mfh->handle);
				}
				if (res != EAS_SUCCESS || mfh->duration < 0) {
					mfh->duration = MMAPI_DURATION_NOT_AVAILABLE;
				}
			}
			#ifdef SONIVOX_DEBUG_DURATION
			else {
				EAS_Report(4, "  Cannot calculate duration because of invalid state=%d\n", state);
			}
			#endif
		}
		#ifdef SONIVOX_DEBUG_DURATION
		else {
			EAS_Report(4, "  Cannot calculate duration because already tried to calculate it.\n");
		}
		#endif
	}
	#ifdef SONIVOX_DEBUG_DURATION
	else {
		EAS_Report(4, "  Cannot calculate duration. mode=%d\n", mfh->mode);
	}
	#endif

	#ifdef SONIVOX_DEBUG_DURATION
	EAS_Report(4, "< MMAPI_GetDuration: new mfh->duration=%d milliseconds\n", mfh->duration);
	#endif
	return mfh->duration;
}

/*
 * Recording
 */

/*
 * MMAPI_OpenRecording
 *
 * Set up the Recording Data struct, and possibly allocate the media buffer struct.
 * Then open the host file (either as a stream or native file).
 */
EAS_BOOL MMAPI_OpenRecording(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle, EAS_CHAR* locator) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;
	/* need a temporary file struct for opening the file/stream */
	MMAPI_FILE_STRUCT recFile;
	EAS_RESULT res = EAS_SUCCESS;
	MMAPI_MediaBuffer* mb = NULL;
	EAS_I32 dataSize;

#ifdef SONIVOX_DEBUG
	EAS_Report(5, "MMAPI_OpenRecording\n");
#endif

	EAS_HWMemSet(&recFile, 0, sizeof(MMAPI_RecordingData));

	/* first get the wave format */
	res = //MMAPI_GetWaveInfo(mHandle, fHandle, &(mfh->record.waveFmt),
		MMAPI_GetWaveInfo(mdh->easHandle, mfh->handle, &(mfh->record.waveFmt),
			&(mfh->record.waveHeaderSize), &(mfh->record.waveDataSize));

	if (res == EAS_SUCCESS && locator == NULL) {
		/* if recording to OutputStream, we need to set the media buffer as circular buffer */
		mb = (MMAPI_MediaBuffer*) EAS_HWMalloc(NULL, sizeof(MMAPI_MediaBuffer));
		if (mb == NULL) {
			res = EAS_ERROR_MALLOC_FAILED;
		}
		if (res == EAS_SUCCESS) {
			EAS_HWMemSet(mb, 0, sizeof(MMAPI_MediaBuffer));
			mb->totalSize = -1; /* prevent EOF */
#ifdef MMAPI_PROVIDE_SILENCE_ON_UNDERRUN
			/* prevent insertion of silence on underrun */
			mb->noSilenceOnUnderrun = EAS_TRUE;
#endif
			mb->bufferSize = MMAPI_RECORD_CIRCULAR_BUFFER_SIZE;
			mb->buffer = (EAS_U8*) EAS_HWMalloc(NULL, mb->bufferSize);
			if (mb->buffer == NULL) {
				res = EAS_ERROR_MALLOC_FAILED;
			}
		}
		if (res == EAS_SUCCESS) {
			mfh->record.mb = mb;
			recFile.mb = mb;
			recFile.mode = MMAPI_OPEN_MODE_STREAM;
		}
	} else {
		/* recording to a native file */
		recFile.locator = locator;
		recFile.mode = MMAPI_OPEN_MODE_NATIVE;
	}
	if (res == EAS_SUCCESS) {
		/* signal EAS_HWOpenFile to not remember the record struct */
		recFile.record.state = MMAPI_RS_NATIVE_ERROR;
		/* eventually open the host file for writing */
		res = EAS_HWOpenFile(mfh->hwInstData, (EAS_FILE_LOCATOR) &recFile, &(mfh->record.handle), EAS_FILE_WRITE);
	}
	if (res == EAS_SUCCESS) {
		/* write the initial header, set file size header field to unknown */
		dataSize = -1;
#ifdef MMAPI_CAPTURE_STREAMING_WORKAROUND
		/* for now, so that these files can be opened by EAS, use
		 * workaround for files recorded to OutputStream */
		if (recFile.mode == MMAPI_OPEN_MODE_STREAM) {
			dataSize = -2;
		}
#endif
		WAVE_WriteHeader(mfh->hwInstData, mfh->record.handle,
			&(mfh->record.waveFmt), dataSize, &(mfh->record.writtenHeaderSize));
	}
	/* clean up if not successful */
	if (res != EAS_SUCCESS) {
		#ifdef SONIVOX_DEBUG
		EAS_Report(_EAS_SEVERITY_ERROR, "MMAPI_OpenRecording: ERROR: error code=%d\n", (int) res);
		#endif
		MMAPI_CloseRecording(mdh, mfh);
	}

	return (res == EAS_SUCCESS);
}

/* Recording utility function: get the number of bytes written to file in pos.
 * This is the number of bytes *written* to the recorded file.
 * For OutputStream mode, this include pending bytes to be read if includeBufferedBytes is EAS_TRUE.
 */
EAS_RESULT MMAPI_GetRecordedByteCount(MMAPI_FILE_STRUCT* mfh, EAS_I32* pos, EAS_BOOL includeBufferedBytes) {
	EAS_RESULT res;

	res = EAS_HWFilePos(mfh->hwInstData, mfh->record.handle, pos);
	if (includeBufferedBytes && res == EAS_SUCCESS && mfh->record.mb != NULL) {
		/* for STREAM mode, need to add the pending bytes in the buffer */
		(*pos) += mfh->record.mb->bufferFilled;
	}
	return res;
}

/*
 * MMAPI_CommitRecording()
 *
 * internal function called from MMAPI_GeneralCommand().
 * Returns EAS_TRUE if more bytes are available for read in the circular buffer,
 * or if an error occured.
 */
EAS_BOOL MMAPI_CommitRecording(MMAPI_DATA_STRUCT* mdh, MMAPI_FILE_STRUCT* mfh) {
	EAS_RESULT res;
	EAS_I32 pos = 0;

#ifdef SONIVOX_DEBUG
	EAS_Report(5, "MMAPI_CommitRecording\n");
#endif
	res = MMAPI_GetRecordedByteCount(mfh, &pos, EAS_TRUE);
	if (res == EAS_SUCCESS && mfh->record.mb == NULL) {
		/* for non-STREAM mode, patch the header */
		res = EAS_HWFileSeek(mfh->hwInstData, mfh->record.handle, 0);
		if (res == EAS_SUCCESS) {
			res = WAVE_WriteHeader(mfh->hwInstData, mfh->record.handle, &(mfh->record.waveFmt),
					pos - mfh->record.writtenHeaderSize, &pos);
		}
	}
	if (res != EAS_SUCCESS) {
		#ifdef SONIVOX_DEBUG
		EAS_Report(_EAS_SEVERITY_ERROR, "MMAPI_CommitRecording: ERROR: error code=%d\n", (int) res);
		#endif
		mfh->record.state = MMAPI_RS_NATIVE_ERROR;
	}
	return ((mfh->record.state == MMAPI_RS_NATIVE_ERROR)
		|| ((mfh->record.mb != NULL) && (mfh->record.mb->bufferFilled > 0)));
}


/* internal function called from MMAPI_GeneralCommand() */
void MMAPI_CloseRecording(MMAPI_DATA_STRUCT* mdh, MMAPI_FILE_STRUCT* mfh) {
#ifdef SONIVOX_DEBUG
	EAS_Report(5, "MMAPI_CloseRecording\n");
#endif
	if (mfh->record.handle) {
		EAS_HWCloseFile(mfh->hwInstData, mfh->record.handle);
	}
	/* clean up */
	if (mfh->record.mb != NULL) {
		if (mfh->record.mb->buffer != NULL) {
			EAS_HWFree(NULL, mfh->record.mb->buffer);
		}
		EAS_HWFree(NULL, mfh->record.mb);
	}
	/* radical initialization */
	EAS_HWMemSet(&(mfh->record), 0, sizeof(MMAPI_RecordingData));
}


EAS_I32 MMAPI_ReadRecordedBuffer(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle,
								 EAS_U8* buffer, EAS_I32 offset, EAS_I32 count) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;
	EAS_RESULT res;
	EAS_I32 pos = 0;
	EAS_I32 readBytes = 0;

#ifdef SONIVOX_DEBUG
	EAS_Report(5, "MMAPI_ReadRecordedBuffer\n");
#endif
	/* if an error occured, never do anything, just return the error code */
	if (mfh->record.state == MMAPI_RS_NATIVE_ERROR) {
		return MMAPI_RS_NATIVE_ERROR;
	}

	/* check file size limit. Only look at actually processed data */
	res = MMAPI_GetRecordedByteCount(mfh, &pos, EAS_FALSE);
	if (res == EAS_SUCCESS && pos >= mfh->record.sizeLimit) {
		return MMAPI_RS_NATIVE_STOPPED;
	}

	/* if no buffer is given, or in native file mode, or error, just return the state */
	if (res == EAS_SUCCESS && (buffer == NULL || mfh->record.mb == NULL)) {
		/* everything seems fine */
		return 0;
	}

	if (res == EAS_SUCCESS) {
		/* actually try to read some. */
		/* commit the buffer offset */
		buffer += offset;
		/* how many bytes to read */
		/* Never read more than file size limit */
		if (count + pos > mfh->record.sizeLimit) {
			count = mfh->record.sizeLimit - pos;
		}
		/* finally read from the circular buffer */
		res = EAS_HWReadFile(mfh->hwInstData, mfh->record.handle, buffer, count, &readBytes);
	}
	if (res != EAS_SUCCESS) {
		#ifdef SONIVOX_DEBUG
		EAS_Report(_EAS_SEVERITY_ERROR, "MMAPI_ReadRecordedBuffer: ERROR: error code=%d\n", (int) res);
		#endif
		mfh->record.state = MMAPI_RS_NATIVE_ERROR;
		return MMAPI_RS_NATIVE_ERROR;
	}
	return readBytes;
}

EAS_RESULT MMAPI_GetWaveInfo(EAS_DATA_HANDLE pEASData, EAS_HANDLE handle,
						   WAVE_FMT_CHUNK* fmt, EAS_I32* headerSize, EAS_I32* dataSize) {
	WAVE_FMT_CHUNK* ptr;
	EAS_RESULT res;

	/* TODO: find out actual header size */
	*headerSize = 44;
	*dataSize = MMAPI_WAVE_SIZE_UNKNOWN;

	res = EAS_GetWaveFmtChunk (pEASData, handle, &ptr);
	if (res == EAS_SUCCESS) {
		/* copy wave format */
		(*fmt) = (*ptr);
	}
	#ifdef SONIVOX_DEBUG
	if (res == EAS_SUCCESS) {
		EAS_Report(4, "EAS_GetWaveFmtChunk(): samplerate=%d, bits=%d, %d channels\n",
			(int) ptr->nSamplesPerSec, (int) ptr->wBitsPerSample, (int) ptr->nChannels);
	} else {
		EAS_Report(2, "EAS_GetWaveFmtChunk() returned error: %d\n", (int) res);
	}
	#endif

	return res;
}


#ifdef MMAPI_HAS_CAPTURE
/*
 * CAPTURE SUPPORT
 */

/*
 * This function reads data from the capture device and writes it
 * to the stream.
 */
void MMAPI_HandleCapturedData(MMAPI_DATA_STRUCT* mdh, MMAPI_FILE_STRUCT* mfh) {
	if (mfh->captureDevice) {
		WriteWaveInDataToStream(mfh->captureDevice, NULL, mfh);
	}
}

/*
 * write wave header to stream
 * open capture device
 * open EAS for wave playback
 */
EAS_BOOL MMAPI_OpenCapture(MMAPI_DATA_HANDLE mHandle, MMAPI_FILE_HANDLE fHandle,
						   MMAPI_CAPTURE_ENCODING encoding, EAS_I32 rate, EAS_I32 bits,
						   EAS_I32 channels, EAS_BOOL isBigEndian, EAS_BOOL isSigned) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) fHandle;
	EAS_I32 headerSize = 0;
	EAS_I32 dataSize;
	WAVE_FMT_CHUNK fmt;
	EAS_RESULT res;

	#ifdef SONIVOX_DEBUG
	EAS_Report(5, "> MMAPI_OpenCapture(encoding=%d, %d Hz, %d-bits, %d channels, %s endian, %ssigned\n",
		encoding, rate, bits, channels, isBigEndian?"big":"little", isSigned?"":"un");
	#endif

	/* initialize the circular buffer size */
	mfh->mb->bufferSize = MMAPI_CAPTURE_CIRCULAR_BUFFER_SIZE;
	/* total size is "not known" */
	mfh->mb->totalSize = -1;

	res = WAVE_FillFormat(&fmt, encoding, rate, bits, channels, isBigEndian, isSigned);

	if (res == EAS_SUCCESS) {
		#ifdef SONIVOX_DEBUG
		EAS_Report(5, "  ..writing wave header\n");
		#endif
#ifdef MMAPI_CAPTURE_STREAMING_WORKAROUND
		/* tell WAVE_WriteHeaderToBuffer() to use EAS value for "streamed wave" */
		dataSize = -2;
#else
		dataSize = -1;
#endif
		res = WAVE_WriteHeaderToBuffer(mfh->mb, &fmt, dataSize, &headerSize);
	}

	if (res == EAS_SUCCESS) {
		#ifdef SONIVOX_DEBUG
		EAS_Report(5, "  ..opening capture device\n");
		#endif
		mfh->captureDevice = OpenWaveInDevice(-1, channels, rate, bits);
		if (!mfh->captureDevice) {
			res = EAS_FAILURE;
		}
	}

	if (res == EAS_SUCCESS) {
		#ifdef SONIVOX_DEBUG
		EAS_Report(5, "  ..opening host file and wave out\n");
		#endif
		res = MMAPI_OpenFileImpl(mdh, mfh, NULL);
	}
	#ifdef SONIVOX_DEBUG
	EAS_Report(5, "< MMAPI_OpenCapture, res=%d\n", (int) res);
	#endif
	return (res == EAS_SUCCESS)?EAS_TRUE:EAS_FALSE;
}

#endif


/*
 * Open an interactive MIDI stream on the player identified by mfh.
 */
EAS_HANDLE MMAPI_OpenInteractiveMIDI(MMAPI_DATA_STRUCT* mdh, MMAPI_FILE_STRUCT* mfh) {
	EAS_HANDLE midiHandle = NULL;
	EAS_RESULT res = EAS_ERROR_HANDLE_INTEGRITY;

#ifdef SONIVOX_DEBUG
	EAS_Report(5, "MMAPI_OpenInteractiveMIDI...");
#endif
	if (mdh->easHandle && mfh->handle) {
		res = EAS_OpenMIDIStream(mdh->easHandle, &midiHandle, mfh->handle);
		if (res == EAS_SUCCESS) {
			#ifdef SONIVOX_DEBUG
			EAS_Report(5, "returning handle=%p\n", midiHandle);
			#endif
			return midiHandle;
		}
	}
	#ifdef SONIVOX_DEBUG
	EAS_Report(_EAS_SEVERITY_ERROR, "ERROR: error code=%d\n", (int) res);
	#endif
	return NULL;
}

/*
 * Open an interactive MIDI stream previously opened with MMAPI_OpenInteractiveMIDI().
 */
void MMAPI_CloseInteractiveMIDI(MMAPI_DATA_STRUCT* mdh, EAS_HANDLE midiHandle) {
#ifdef SONIVOX_DEBUG
	EAS_Report(5, "MMAPI_CloseInteractiveMIDI(mdh=%p, midiHandle=%p)", (void*) mdh, (void*) midiHandle);
#endif
	if (mdh->easHandle && midiHandle) {
		EAS_CloseMIDIStream(mdh->easHandle, midiHandle);
	}
}


EAS_BOOL MMAPI_LoadDLSCollection(MMAPI_DATA_HANDLE mHandle, EAS_CHAR *locator) {
	MMAPI_DATA_STRUCT* mdh = (MMAPI_DATA_STRUCT*) mHandle;
	EAS_RESULT res = EAS_SUCCESS;
	
	MMAPI_FILE_STRUCT* mfh = (MMAPI_FILE_STRUCT*) EAS_HWMalloc(NULL, sizeof(MMAPI_FILE_STRUCT));

	if (mfh != NULL) {
		EAS_HWMemSet(mfh, 0, sizeof(MMAPI_FILE_STRUCT));
		mfh->mode = MMAPI_OPEN_MODE_NATIVE;
		mfh->duration = MMAPI_DURATION_UNINITIALIZED;
		mfh->locator = locator;
		res = EAS_LoadDLSCollection(mdh->easHandle, NULL, (EAS_FILE_LOCATOR) mfh);
		EAS_HWFree(NULL, mfh);
	}
	return res == EAS_SUCCESS;
}

/* workaround for a linker problem, when __ftol2_sse does not exist*/
#ifdef USING_MS_VC_2003_NET_STANDARD
long _ftol2(double);
long _ftol2_sse(double x) { return _ftol2(x); }
#endif
