/*----------------------------------------------------------------------------
 *
 * File:
 * eas_otadata.h
 *
 * Contents and purpose:
 * OTA File Parser
 *
 * This file contains data declarations for the OTA parser.
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

#ifndef EAS_OTADATA_H
#define EAS_OTADATA_H

#include "eas_data.h"

/* definition for state flags */
#define OTA_FLAGS_UNICODE           0x01    /* unicode text */

/*----------------------------------------------------------------------------
 *
 * S_OTA_DATA
 *
 * This structure contains the state data for the OTA parser
 *----------------------------------------------------------------------------
*/

typedef struct
{
    EAS_I32         fileOffset;             /* offset to location in file */
    EAS_U8          patternLen;             /* length of current pattern */
    EAS_U8          dataByte;               /* previous char from file */
    EAS_U8          bitCount;               /* bit count in char */
} S_OTA_LOC;

typedef struct
{
    EAS_FILE_HANDLE fileHandle;             /* file handle */
    S_SYNTH *pSynth;                /* synth handle */
    EAS_I32         fileOffset;             /* offset to start of data */
    EAS_I32         time;                   /* current time in 256ths of a msec */
    EAS_U32         tick;                   /* length of 32nd note in 256th of a msec */
    EAS_U32         restTicks;              /* ticks to rest after current note */
    S_OTA_LOC       patterns[4];            /* pattern locations */
    S_OTA_LOC       current;                /* current location */
    S_OTA_LOC       restore;                /* previous location */
    S_METADATA_CB   metadata;               /* metadata callback */
    EAS_U8          flags;                  /* bit flags */
    EAS_U8          numPatterns;            /* number of patterns left in song */
    EAS_U8          currentPattern;         /* current pattern for loop */
    EAS_U8          note;                   /* MIDI note number */
    EAS_U8          octave;                 /* octave modifier */
    EAS_U8          style;                  /* from STYLE */
    EAS_U8          velocity;               /* current volume */
    EAS_U8          state;                  /* current state EAS_STATE_XXXX */
    EAS_U8          loopCount;              /* loop count for pattern */
} S_OTA_DATA;

#endif


