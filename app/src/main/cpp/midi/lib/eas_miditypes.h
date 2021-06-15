/*----------------------------------------------------------------------------
 *
 * File:
 * eas_miditypes.h
 *
 * Contents and purpose:
 * Contains declarations for the MIDI stream parser.
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
 *   $Revision: 778 $
 *   $Date: 2007-07-23 16:45:17 -0700 (Mon, 23 Jul 2007) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_MIDITYPES_H
#define _EAS_MIDITYPES_H

#include "eas_data.h"
#include "eas_parser.h"

/*----------------------------------------------------------------------------
 * S_MIDI_STREAM
 *
 * Maintains parser state for the MIDI stream parser
 *
 *----------------------------------------------------------------------------
*/

typedef struct s_midi_stream_tag
{
    EAS_BOOL8           byte3;              /* flag indicates 3rd byte expected */
    EAS_BOOL8           pending;            /* flag indicates more data expected */
    EAS_U8              sysExState;         /* maintains the SysEx state */
    EAS_U8              runningStatus;      /* last running status received */
    EAS_U8              status;             /* status byte */
    EAS_U8              d1;                 /* first data byte */
    EAS_U8              d2;                 /* second data byte */
    EAS_U8              flags;              /* flags - see below for definition */
#ifdef JET_INTERFACE
    EAS_U32             jetData;            /* JET data */
#endif
} S_MIDI_STREAM;

/* flags for S_MIDI_STREAM.flags */
#define MIDI_FLAG_GM_ON         0x01        /* GM System On message received */
#define MIDI_FLAG_FIRST_NOTE    0x02        /* first note received */

/* flags for S_MIDI_STREAM.jetFlags */
#define MIDI_FLAGS_JET_MUTE     0x00000001  /* track is muted */
#define MIDI_FLAGS_JET_CB       0x00000002  /* JET callback enabled */

/*----------------------------------------------------------------------------
 *
 * S_SMF_STREAM
 *
 * This structure contains data required to parse an SMF stream. For SMF0 files, there
 * will be a single instance of this per file. For SMF1 files, there will be multiple instance,
 * one for each separate stream in the file.
 *
 *----------------------------------------------------------------------------
*/

typedef struct s_smf_stream_tag
{
    EAS_FILE_HANDLE     fileHandle;         /* host wrapper file handle */
    EAS_U32             ticks;              /* time of next event in stream */
    EAS_I32             startFilePos;       /* start location of track within file */
    S_MIDI_STREAM       midiStream;         /* MIDI stream state */
} S_SMF_STREAM;

/*----------------------------------------------------------------------------
 *
 * S_SMF_DATA
 *
 * This structure contains the instance data required to parse an SMF stream.
 *
 *----------------------------------------------------------------------------
*/

typedef struct s_smf_data_tag
{
#ifdef _CHECKED_BUILD
    EAS_U32             handleCheck;        /* signature check for checked build */
#endif
    S_SMF_STREAM        *streams;           /* pointer to individual streams in file */
    S_SMF_STREAM        *nextStream;        /* pointer to next stream with event */
    S_SYNTH             *pSynth;            /* pointer to synth */
    EAS_FILE_HANDLE     fileHandle;         /* file handle */
    S_METADATA_CB       metadata;           /* metadata callback */
    EAS_I32             fileOffset;         /* for embedded files */
    EAS_I32             time;               /* current time in milliseconds/256 */
    EAS_U16             numStreams;         /* actual number of streams */
    EAS_U16             tickConv;           /* current MIDI tick to msec conversion */
    EAS_U16             ppqn;               /* ticks per quarter note */
    EAS_U8              state;              /* current state EAS_STATE_XXXX */
    EAS_U8              flags;              /* flags - see definitions below */
} S_SMF_DATA;

#define SMF_FLAGS_CHASE_MODE        0x01    /* chase mode - skip to first note */
#define SMF_FLAGS_HAS_TIME_SIG      0x02    /* time signature encountered at time 0 */
#define SMF_FLAGS_HAS_TEMPO         0x04    /* tempo encountered at time 0  */
#define SMF_FLAGS_HAS_GM_ON         0x08    /* GM System On encountered at time 0 */
#define SMF_FLAGS_JET_STREAM        0x80    /* JET in use - keep strict timing */

/* combo flags indicate setup bar */
#define SMF_FLAGS_SETUP_BAR (SMF_FLAGS_HAS_TIME_SIG | SMF_FLAGS_HAS_TEMPO | SMF_FLAGS_HAS_GM_ON)

/*----------------------------------------------------------------------------
 * Interactive MIDI structure
 *----------------------------------------------------------------------------
*/
typedef struct s_interactive_midi_tag
{
#ifdef _CHECKED_BUILD
    EAS_U32             handleCheck;        /* signature check for checked build */
#endif
    S_SYNTH     *pSynth;            /* pointer to synth */
    S_MIDI_STREAM       stream;             /* stream data */
} S_INTERACTIVE_MIDI;

#endif /* #ifndef _EAS_MIDITYPES_H */

