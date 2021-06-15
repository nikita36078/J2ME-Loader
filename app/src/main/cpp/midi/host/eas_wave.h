/*----------------------------------------------------------------------------
 *
 * File:
 * eas_wave.h
 *
 * Contents and purpose:
 * Writes output to a .WAV file
 *
 * DO NOT MODIFY THIS FILE!
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
 *----------------------------------------------------------------------------
 * Revision Control:
 *   $Revision: 82 $
 *   $Date: 2006-07-10 11:45:19 -0700 (Mon, 10 Jul 2006) $
 *----------------------------------------------------------------------------
*/

#include "eas_types.h"

/* sentinel */
#ifndef _EAS_WAVE_H
#define _EAS_WAVE_H

/* .WAV file format chunk */
typedef struct {
    EAS_U16 wFormatTag;
    EAS_U16 nChannels;
    EAS_U32 nSamplesPerSec;
    EAS_U32 nAvgBytesPerSec;
    EAS_U16 nBlockAlign;
    EAS_U16 wBitsPerSample;
} FMT_CHUNK;

/* .WAV file header */
typedef struct {
    EAS_U32 nRiffTag;
    EAS_U32 nRiffSize;
    EAS_U32 nWaveTag;
    EAS_U32 nFmtTag;
    EAS_U32 nFmtSize;
    FMT_CHUNK fc;
    EAS_U32 nDataTag;
    EAS_U32 nDataSize;
} WAVE_HEADER;

typedef struct {
    WAVE_HEADER wh;
    FILE *file;
    EAS_BOOL write;
    EAS_U32 dataSize;
} WAVE_FILE;

WAVE_FILE *WaveFileCreate (const char *filename, EAS_I32 nChannels, EAS_I32 nSamplesPerSec, EAS_I32 wBitsPerSample);
EAS_I32 WaveFileWrite (WAVE_FILE *wFile, void *buffer, EAS_I32 n);
EAS_BOOL WaveFileClose (WAVE_FILE *wFile);
WAVE_FILE *WaveFileOpen (const char *filename);

#endif /* end #ifndef _EAS_WAVE_H */



