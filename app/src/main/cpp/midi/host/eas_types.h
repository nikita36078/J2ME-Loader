/*----------------------------------------------------------------------------
 *
 * File:
 * eas_types.h
 *
 * Contents and purpose:
 * The public interface header for the EAS synthesizer.
 *
 * This header only contains declarations that are specific
 * to this implementation.
 *
 * DO NOT MODIFY THIS FILE!
 *
 * Copyright Sonic Network Inc. 2004

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
 *   $Revision: 726 $
 *   $Date: 2007-06-14 23:10:46 -0700 (Thu, 14 Jun 2007) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_TYPES_H
#define _EAS_TYPES_H

/* EAS_RESULT return codes */
typedef long EAS_RESULT;
#define EAS_SUCCESS                         0
#define EAS_FAILURE                         -1
#define EAS_ERROR_INVALID_MODULE            -2
#define EAS_ERROR_MALLOC_FAILED             -3
#define EAS_ERROR_FILE_POS                  -4
#define EAS_ERROR_INVALID_FILE_MODE         -5
#define EAS_ERROR_FILE_SEEK                 -6
#define EAS_ERROR_FILE_LENGTH               -7
#define EAS_ERROR_NOT_IMPLEMENTED           -8
#define EAS_ERROR_CLOSE_FAILED              -9
#define EAS_ERROR_FILE_OPEN_FAILED          -10
#define EAS_ERROR_INVALID_HANDLE            -11
#define EAS_ERROR_NO_MIX_BUFFER             -12
#define EAS_ERROR_PARAMETER_RANGE           -13
#define EAS_ERROR_MAX_FILES_OPEN            -14
#define EAS_ERROR_UNRECOGNIZED_FORMAT       -15
#define EAS_BUFFER_SIZE_MISMATCH            -16
#define EAS_ERROR_FILE_FORMAT               -17
#define EAS_ERROR_SMF_NOT_INITIALIZED       -18
#define EAS_ERROR_LOCATE_BEYOND_END         -19
#define EAS_ERROR_INVALID_PCM_TYPE          -20
#define EAS_ERROR_MAX_PCM_STREAMS           -21
#define EAS_ERROR_NO_VOICE_ALLOCATED        -22
#define EAS_ERROR_INVALID_CHANNEL           -23
#define EAS_ERROR_ALREADY_STOPPED           -24
#define EAS_ERROR_FILE_READ_FAILED          -25
#define EAS_ERROR_HANDLE_INTEGRITY          -26
#define EAS_ERROR_MAX_STREAMS_OPEN          -27
#define EAS_ERROR_INVALID_PARAMETER         -28
#define EAS_ERROR_FEATURE_NOT_AVAILABLE     -29
#define EAS_ERROR_SOUND_LIBRARY             -30
#define EAS_ERROR_NOT_VALID_IN_THIS_STATE   -31
#define EAS_ERROR_NO_VIRTUAL_SYNTHESIZER    -32
#define EAS_ERROR_FILE_ALREADY_OPEN         -33
#define EAS_ERROR_FILE_ALREADY_CLOSED       -34
#define EAS_ERROR_INCOMPATIBLE_VERSION      -35
#define EAS_ERROR_QUEUE_IS_FULL             -36
#define EAS_ERROR_QUEUE_IS_EMPTY            -37
#define EAS_ERROR_FEATURE_ALREADY_ACTIVE    -38

/* special return codes */
#define EAS_EOF                             3
#define EAS_STREAM_BUFFERING                4
#define EAS_BUFFER_FULL                     5

/* EAS_STATE return codes */
typedef long EAS_STATE;
typedef enum
{
    EAS_STATE_READY = 0,
    EAS_STATE_PLAY,
    EAS_STATE_STOPPING,
    EAS_STATE_PAUSING,
    EAS_STATE_STOPPED,
    EAS_STATE_PAUSED,
    EAS_STATE_OPEN,
    EAS_STATE_ERROR,
    EAS_STATE_EMPTY
} E_EAS_STATE;

/* constants */
#ifndef EAS_CONST
#define EAS_CONST const
#endif

/* definition for public interface functions */
#ifndef EAS_PUBLIC
#define EAS_PUBLIC
#endif

/* boolean values */
typedef unsigned EAS_BOOL;
typedef unsigned char EAS_BOOL8;

#define EAS_FALSE   0
#define EAS_TRUE    1

/* scalar variable definitions */
typedef unsigned char EAS_U8;
typedef signed char EAS_I8;
typedef char EAS_CHAR;

typedef unsigned short EAS_U16;
typedef short EAS_I16;

typedef unsigned long EAS_U32;
typedef long EAS_I32;

typedef unsigned EAS_UINT;
typedef int EAS_INT;
typedef long EAS_LONG;

/* audio output type */
typedef short EAS_PCM;

/* file open modes */
typedef EAS_I32 EAS_FILE_MODE;
#define EAS_FILE_READ   1
#define EAS_FILE_WRITE  2

/* file locator e.g. filename or memory pointer */
typedef struct s_eas_file_tag {
    void *handle;
    int(*readAt)(void *handle, void *buf, int offset, int size);
    int(*size)(void *handle);
} EAS_FILE, *EAS_FILE_LOCATOR;

/* handle to stream */
typedef struct s_eas_stream_tag *EAS_HANDLE;

/* handle to file */
typedef struct eas_hw_file_tag *EAS_FILE_HANDLE;

/* handle for synthesizer data */
typedef struct s_eas_data_tag *EAS_DATA_HANDLE;

/* handle to persistent data for host wrapper interface */
typedef struct eas_hw_inst_data_tag *EAS_HW_DATA_HANDLE;

/* handle to sound library */
typedef struct s_eas_sndlib_tag *EAS_SNDLIB_HANDLE;
typedef struct s_eas_dls_tag *EAS_DLSLIB_HANDLE;

/* pointer to frame buffer - used in split architecture only */
typedef struct s_eas_frame_buffer_tag *EAS_FRAME_BUFFER_HANDLE;

/* untyped pointer for instance data */
typedef void *EAS_VOID_PTR;

/* inline functions */
#ifndef EAS_INLINE
#if defined (__XCC__)
#define EAS_INLINE __inline__
#elif defined (__GNUC__)
#define EAS_INLINE inline static
#else
#define EAS_INLINE __inline
#endif
#endif

/* define NULL value */
#ifndef NULL
#define NULL 0
#endif

/* metadata types for metadata return codes */
typedef enum
{
    EAS_METADATA_UNKNOWN = 0,
    EAS_METADATA_TITLE,
    EAS_METADATA_AUTHOR,
    EAS_METADATA_COPYRIGHT,
    EAS_METADATA_LYRIC,
    EAS_METADATA_TEXT
} E_EAS_METADATA_TYPE;

/* metadata callback function */
typedef void (*EAS_METADATA_CBFUNC) (E_EAS_METADATA_TYPE metaDataType, char *metaDataBuf, EAS_VOID_PTR pUserData);

/* file types for metadata return codes */
typedef enum
{
    EAS_FILE_UNKNOWN = 0,
    EAS_FILE_SMF0,
    EAS_FILE_SMF1,
    EAS_FILE_SMAF_UNKNOWN,
    EAS_FILE_SMAF_MA2,
    EAS_FILE_SMAF_MA3,
    EAS_FILE_SMAF_MA5,
    EAS_FILE_CMX,
    EAS_FILE_MFI,
    EAS_FILE_OTA,
    EAS_FILE_IMELODY,
    EAS_FILE_RTTTL,
    EAS_FILE_XMF0,
    EAS_FILE_XMF1,
    EAS_FILE_WAVE_PCM,
    EAS_FILE_WAVE_IMA_ADPCM,
    EAS_FILE_MMAPI_TONE_CONTROL
} E_EAS_FILE_TYPE;

/* enumeration for synthesizers */
typedef enum
{
    EAS_MCU_SYNTH = 0,
    EAS_DSP_SYNTH
} E_SYNTHESIZER;

/* external audio callback program change */
typedef struct s_ext_audio_prg_chg_tag
{
    EAS_U16     bank;
    EAS_U8      program;
    EAS_U8      channel;
} S_EXT_AUDIO_PRG_CHG;

/* external audio callback event */
typedef struct s_ext_audio_event_tag
{
    EAS_U8      channel;
    EAS_U8      note;
    EAS_U8      velocity;
    EAS_BOOL8   noteOn;
} S_EXT_AUDIO_EVENT;

typedef struct s_midi_controllers_tag
{
    EAS_U8      modWheel;           /* CC1 */
    EAS_U8      volume;             /* CC7 */
    EAS_U8      pan;                /* CC10 */
    EAS_U8      expression;         /* CC11 */
    EAS_U8      channelPressure;    /* MIDI channel pressure */

#ifdef  _REVERB
    EAS_U8      reverbSend;         /* CC91 */
#endif

#ifdef  _CHORUS
    EAS_U8      chorusSend;         /* CC93 */
#endif
} S_MIDI_CONTROLLERS;

/* iMode play modes enumeration for EAS_SetPlayMode */
typedef enum
{
    IMODE_PLAY_ALL = 0,
    IMODE_PLAY_PARTIAL
} E_I_MODE_PLAY_MODE;

typedef EAS_BOOL (*EAS_EXT_PRG_CHG_FUNC) (EAS_VOID_PTR pInstData, S_EXT_AUDIO_PRG_CHG *pPrgChg);
typedef EAS_BOOL (*EAS_EXT_EVENT_FUNC) (EAS_VOID_PTR pInstData, S_EXT_AUDIO_EVENT *pEvent);

#endif /* #ifndef _EAS_TYPES_H */
