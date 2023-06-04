/*----------------------------------------------------------------------------
 *
 * File:
 * eas_mmapi_types.c
 *
 * Contents and purpose:
 * MMAPI type declarations.
 *
 * Copyright 2006 Sonic Network Inc.
 *
 *----------------------------------------------------------------------------
 * Revision Control:
 *   $Revision: 560 $
 *   $Date: 2007-02-02 14:34:18 -0800 (Fri, 02 Feb 2007) $
 *----------------------------------------------------------------------------
 */
/* include EAS headers */
#include "eas_mmapi_config.h"
#include "../sonivox/host_src/eas_types.h"

#ifndef _EAS_MMAPI_TYPES_INCLUDED
#define _EAS_MMAPI_TYPES_INCLUDED

/*
 * MMAPI implementation specific type declarations
 */

/* The MODE_ constants from class com.sonivox.mmapi.EAS */
typedef enum {
	MMAPI_OPEN_MODE_NATIVE = 1,
	MMAPI_OPEN_MODE_MEMORY,
	MMAPI_OPEN_MODE_STREAM,
	MMAPI_OPEN_MODE_INTERACTIVE_MIDI,
	MMAPI_OPEN_MODE_TONE_SEQUENCE,
	MMAPI_OPEN_MODE_CAPTURE,
} MMAPI_OPEN_MODE;


/* Recording States */
#define MMAPI_RS_NONE			 0
#define MMAPI_RS_RECORDING		 3
#define MMAPI_RS_NATIVE_ERROR	-1
#define MMAPI_RS_NATIVE_STOPPED	-2


/* Capture encodings */
typedef enum {
	MMAPI_CAPTURE_ENCODING_PCM = 1,
} MMAPI_CAPTURE_ENCODING;


/* The COMMAND_ constants from class com.sonivox.mmapi.EAS */
typedef enum {
	MMAPI_COMMAND_GET_MODE = 1,
	/* recording commands */
	MMAPI_COMMAND_START_RECORDING,
	MMAPI_COMMAND_STOP_RECORDING,
	MMAPI_COMMAND_COMMIT_RECORDING,
	MMAPI_COMMAND_CLOSE_RECORDING,
	MMAPI_COMMAND_GET_RECORDING_STATE,
	MMAPI_COMMAND_LIMIT_RECORDING,
	MMAPI_COMMAND_SET_TEMPO,
	MMAPI_COMMAND_GET_TEMPO,
	MMAPI_COMMAND_REOPEN,
	MMAPI_OPEN_INTERACTIVE_MIDI,
	MMAPI_CLOSE_INTERACTIVE_MIDI,
} MMAPI_COMMAND_CODE;

/* write flag for the MMAPI_WriteBuffer function (sync with EAS.java) */
#define MMAPI_WRITE_FLAG_MORE_COMING 1

/* write flag for the MMAPI_WriteBuffer function (sync with EAS.java) */
#define MMAPI_WRITE_FLAG_INTERACTIVE_MIDI 2

/* an opaque handle for the engine (wraps EAS_DATA_HANDLE) */
typedef void* MMAPI_DATA_HANDLE;

/* an opaque handle for an open file (wraps EAS_HANDLE) */
typedef void* MMAPI_FILE_HANDLE;

/* a media buffer for MEMORY operation and as circular buffer for STREAM */
typedef struct mmapi_mediabuffer_tag {
	/* the buffer. Owned by eas_mmapi.c */
	EAS_U8* buffer;
	/* the total size of the buffer */
	EAS_I32 bufferSize;
	/* the number of bytes readable in buffer */
	EAS_I32 bufferFilled;
	/* STREAM mode: the read position in buffer. Remains 0 for MEMORY mode */
	EAS_I32 readPos;
	/* STREAM mode: the read offset to account for wrap arounds. This number
	 * increases by the buffer size with every read wrap around  */
	EAS_I32 readPosOffset;
	/* STREAM mode: the write position in buffer */
	EAS_I32 writePos;
	/* the total number of bytes in the file, or -1 if not known */
	EAS_I32 totalSize;
#ifdef MMAPI_PROVIDE_SILENCE_ON_UNDERRUN
	/* flag to prevent silence on underrun. Will set for recording */
	EAS_BOOL noSilenceOnUnderrun;
#endif
} MMAPI_MediaBuffer;


/* meta data entry */
typedef struct mmapi_metadata_tag {
	/* the next meta data entry, or NULL if end of list */
	struct mmapi_metadata_tag* next;
	E_EAS_METADATA_TYPE type;
	EAS_CHAR value[1];
} MMAPI_MetaData;

/* wave data size constant if size is not known */
#define MMAPI_WAVE_SIZE_UNKNOWN (0x80000000)

/* file size constant if size is not (yet) known, e.g. for STREAM mode */
#define MMAPI_FILE_SIZE_UNKNOWN MMAPI_WAVE_SIZE_UNKNOWN

#endif /* _EAS_MMAPI_TYPES_INCLUDED */
