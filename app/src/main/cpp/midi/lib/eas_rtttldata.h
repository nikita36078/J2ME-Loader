/*----------------------------------------------------------------------------
 *
 * File:
 * eas_rtttldata.h
 *
 * Contents and purpose:
 * SMF File Parser
 *
 * This file contains data declarations for the RTTTL parser.
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
 *   $Revision: 82 $
 *   $Date: 2006-07-10 11:45:19 -0700 (Mon, 10 Jul 2006) $
 *----------------------------------------------------------------------------
*/

#ifndef EAS_RTTTLDATA_H
#define EAS_RTTTLDATA_H

#include "eas_data.h"


/* maximum line size as specified in iMelody V1.2 spec */
#define MAX_LINE_SIZE           75

/*----------------------------------------------------------------------------
 *
 * S_RTTTL_DATA
 *
 * This structure contains the state data for the iMelody parser
 *----------------------------------------------------------------------------
*/

typedef struct
{
    EAS_FILE_HANDLE fileHandle;             /* file handle */
    S_SYNTH *pSynth;                    /* synthesizer handle */
    S_METADATA_CB   metadata;               /* metadata callback */
    EAS_I32     fileOffset;                 /* offset to start of data */
    EAS_I32     time;                       /* current time in 256ths of a msec */
    EAS_I32     tick;                       /* length of 32nd note in 256th of a msec */
    EAS_I32     restTicks;                  /* ticks to rest after current note */
    EAS_I32     repeatOffset;               /* file offset to start of repeat section */
    EAS_U8      repeatCount;                /* repeat counter */
    EAS_U8      notePlayedSinceRepeat;      /* indicates if any work done since last repeat */
    EAS_I8      dataByte;                   /* storage for characters that are "put back" */
    EAS_U8      state;                      /* current state EAS_STATE_XXXX */
    EAS_I8      style;                      /* from STYLE */
    EAS_U8      note;                       /* MIDI note number */
    EAS_U8      octave;                     /* decault octave prefix */
    EAS_I8      duration;                   /* default note duration */
} S_RTTTL_DATA;

#endif


