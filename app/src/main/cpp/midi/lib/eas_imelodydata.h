/*----------------------------------------------------------------------------
 *
 * File:
 * eas_imelodydata.h
 *
 * Contents and purpose:
 * SMF File Parser
 *
 * This file contains data declarations for the iMelody parser.
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

#ifndef EAS_IMELODYDATA_H
#define EAS_IMELODYDATA_H

#include "eas_data.h"

/* maximum line size as specified in iMelody V1.2 spec */
#define MAX_LINE_SIZE           75

/*----------------------------------------------------------------------------
 *
 * S_IMELODY_DATA
 *
 * This structure contains the state data for the iMelody parser
 *----------------------------------------------------------------------------
*/

typedef struct
{
    EAS_FILE_HANDLE fileHandle;                 /* file handle */
    S_SYNTH         *pSynth;                    /* pointer to synth */
    EAS_I32         fileOffset;                 /* offset to start of data */
    EAS_I32         time;                       /* current time in 256ths of a msec */
    EAS_I32         tickBase;                   /* basline length of 32nd note in 256th of a msec */
    EAS_I32         tick;                       /* actual length of 32nd note in 256th of a msec */
    EAS_I32         restTicks;                  /* ticks to rest after current note */
    EAS_I32         startLine;                  /* file offset at start of line (for repeats) */
    EAS_I32         repeatOffset;               /* file offset to start of repeat section */
    EAS_I32         repeatTime;                 /* time at start of repeat section */
    S_METADATA_CB   metadata;                   /* metadata callback */
    EAS_I16         repeatCount;                /* repeat counter */
    EAS_U8          state;                      /* current state EAS_STATE_XXXX */
    EAS_U8          style;                      /* from STYLE */
    EAS_U8          index;                      /* index into buffer */
    EAS_U8          octave;                     /* octave prefix */
    EAS_U8          volume;                     /* current volume */
    EAS_U8          note;                       /* MIDI note number */
    EAS_I8          noteModifier;               /* sharp or flat */
    EAS_I8          buffer[MAX_LINE_SIZE+1];    /* buffer for ASCII data */
} S_IMELODY_DATA;

#endif


