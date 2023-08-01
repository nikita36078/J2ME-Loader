/*----------------------------------------------------------------------------
 *
 * File:
 * eas_mdls.c
 *
 * Contents and purpose:
 * This file contains DLS to EAS converter.
 *
 * Copyright (c) 2005 Sonic Network Inc.

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
 *   $Revision: 818 $
 *   $Date: 2007-08-02 15:19:41 -0700 (Thu, 02 Aug 2007) $
 *----------------------------------------------------------------------------
*/

/*
 * NOTES:
 *
 * Processor Endian-ness:
 *
 * We use the EAS_HWGetDWord() and EAS_HWGetWord () functions
 * extensively in this module. It would probably be faster to read
 * an entire data structure, but this introduces the problem of
 * sensitivity to processor endian-ness to the parser. By utlilizing
 * the host wrapper functions, we avoid having to flip bytes around
 * for big-endian processors. The default host wrapper versions of
 * these functions are insensitive to processor endian-ness due to
 * the fact that they read the file as a byte stream.
 *
 * Dynamic Memory:
 *
 * Dynamic memory allocation is a risky proposition in a mobile
 * device. The memory can become fragmented, resulting in an
 * inability to allocate a memory block, or garbage collection
 * routines can use many CPU cycles. Either can contribute to
 * failures of critical systems. Therefore, we try to minimize the
 * number of memory allocations we make.
 *
 * We allocate a single large block of memory for the entire
 * converted DLS collection, including the articulation data and
 * samples. This block is then sub-allocated for the various
 * data structures.
 *
 * Parser Overview:
 *
 * We make two passes through the file, the first pass to count the
 * number of instruments, regions, etc. and allocate memory for
 * them. The second pass parses the data into the allocated data
 * structures.
 *
 * Conditional chunks are challenging in that they can occur
 * anywhere in the list chunk that contains them. To simplify, we
 * parse the blocks in a list in specific order, no matter which
 * order they appear in the file. This way we don't allocate memory
 * and parse a block that we end up throwing away later due to
 * a conditional chunk.
 *
 * Assumptions that may bite us in the future:
 *
 * We make some assumptions to simplify things. The most fundamental
 * assumption is that there will be no more than one of any type of
 * chunk in a list. While this is consistent with the block diagram
 * of the file layout in the mDLS spec, there is nothing in the
 * spec that precludes having mulitple lar2 or rgn2 chunks, with
 * conditional blocks that dictate their usage.
 *
 * DLS -> EAS Conversion Process:
 *
 * Another challenge is that the DLS structure does not map well to
 * the current EAS sound library structure. Not all DLS constructs
 * are supported, and data from DLS structures must sometimes be
 * mapped to multiple EAS data structures. To simplify the process,
 * the EAS region, articulation, and envelopes are treated as a
 * single combined unit. Thus for each region, there must be one
 * articulation element and two envelope elements.
 *
 * The sample processing is also a multi-step process. First the
 * ptbl chunk is pre-parsed to determine the number of samples
 * in the collection. The next step is to parse the instrument data
 * to determine which samples are actually used by instruments.
 * Some samples may not be used because they are used only in
 * conditional blocks that the synthesizer cannot parse, or the
 * author neglected to remove unused samples from the collection.
 * In the next step, the active samples are read into memory and
 * converted to the appropriate playback format. Finally, as the
 * instruments are processed, the links are made to the samples and
 * wsmp data is extracted for the region and articulation data
 * structures.
*/

#ifndef _FILTER_ENABLED
#error "Filter must be enabled if DLS_SYNTHESIZER is enabled"
#endif

/*------------------------------------
 * includes
 *------------------------------------
*/

/* this define allows us to use the sndlib.h structures as RW memory */
#define SCNST

#define LOG_TAG "Sonivox"
#include "util/log.h"

#include "eas_data.h"
#include "eas_host.h"
#include "eas_mdls.h"
#include "eas_math.h"
#include "dls.h"
#include "dls2.h"
#include "eas_report.h"
#include <string.h>

//2 we should replace log10() function with fixed point routine in ConvertSampleRate()
/* lint is choking on the ARM math.h file, so we declare the log10 function here */
extern double log10(double x);

/*------------------------------------
 * defines
 *------------------------------------
*/

// #define _DEBUG_DLS

#define DLS_MAX_WAVE_COUNT      1024
#define DLS_MAX_ART_COUNT       2048
#define DLS_MAX_REGION_COUNT    2048
#define DLS_MAX_INST_COUNT      256
#define MAX_DLS_WAVE_SIZE       (1024*1024)

#ifndef EAS_U32_MAX
#define EAS_U32_MAX             (4294967295U)
#endif

#ifndef EAS_I32_MAX
#define EAS_I32_MAX             (2147483647)
#endif

/*------------------------------------
 * typedefs
 *------------------------------------
*/

/* offsets to articulation data */
typedef enum
{
    PARAM_MODIFIED = 0,
    PARAM_MOD_LFO_FREQ,
    PARAM_MOD_LFO_DELAY,

    PARAM_VIB_LFO_FREQ,
    PARAM_VIB_LFO_DELAY,

    PARAM_VOL_EG_DELAY,
    PARAM_VOL_EG_ATTACK,
    PARAM_VOL_EG_HOLD,
    PARAM_VOL_EG_DECAY,
    PARAM_VOL_EG_SUSTAIN,
    PARAM_VOL_EG_RELEASE,
    PARAM_VOL_EG_SHUTDOWN,
    PARAM_VOL_EG_VEL_TO_ATTACK,
    PARAM_VOL_EG_KEY_TO_DECAY,
    PARAM_VOL_EG_KEY_TO_HOLD,

    PARAM_MOD_EG_DELAY,
    PARAM_MOD_EG_ATTACK,
    PARAM_MOD_EG_HOLD,
    PARAM_MOD_EG_DECAY,
    PARAM_MOD_EG_SUSTAIN,
    PARAM_MOD_EG_RELEASE,
    PARAM_MOD_EG_VEL_TO_ATTACK,
    PARAM_MOD_EG_KEY_TO_DECAY,
    PARAM_MOD_EG_KEY_TO_HOLD,

    PARAM_INITIAL_FC,
    PARAM_INITIAL_Q,
    PARAM_MOD_LFO_TO_FC,
    PARAM_MOD_LFO_CC1_TO_FC,
    PARAM_MOD_LFO_CHAN_PRESS_TO_FC,
    PARAM_MOD_EG_TO_FC,
    PARAM_VEL_TO_FC,
    PARAM_KEYNUM_TO_FC,

    PARAM_MOD_LFO_TO_GAIN,
    PARAM_MOD_LFO_CC1_TO_GAIN,
    PARAM_MOD_LFO_CHAN_PRESS_TO_GAIN,
    PARAM_VEL_TO_GAIN,

    PARAM_TUNING,
    PARAM_KEYNUM_TO_PITCH,
    PARAM_VIB_LFO_TO_PITCH,
    PARAM_VIB_LFO_CC1_TO_PITCH,
    PARAM_VIB_LFO_CHAN_PRESS_TO_PITCH,
    PARAM_MOD_LFO_TO_PITCH,
    PARAM_MOD_LFO_CC1_TO_PITCH,
    PARAM_MOD_LFO_CHAN_PRESS_TO_PITCH,
    PARAM_MOD_EG_TO_PITCH,

    PARAM_DEFAULT_PAN,
    PARAM_MIDI_CC91_TO_REVERB_SEND,
    PARAM_DEFAULT_REVERB_SEND,
    PARAM_MIDI_CC93_TO_CHORUS_SEND,
    PARAM_DEFAULT_CHORUS_SEND,
    PARAM_TABLE_SIZE
} E_ART_INDEX;

/* temporary data structure combining region, articulation, and envelope data */
typedef struct s_art_dls_tag
{
    EAS_I16     values[PARAM_TABLE_SIZE];
} S_DLS_ART_VALUES;

/* temporary data structure for wlnk chunk data */
typedef struct
{
    EAS_I32 gain;
    EAS_U32 loopStart;
    EAS_U32 loopLength;
    EAS_U32 sampleRate;
    EAS_U16 bitsPerSample;
    EAS_I16 fineTune;
    EAS_U8  unityNote;
} S_WSMP_DATA;

/* temporary data structure used while parsing a DLS file */
typedef struct
{
    S_DLS               *pDLS;
    EAS_HW_DATA_HANDLE  hwInstData;
    EAS_FILE_HANDLE     fileHandle;
    S_WSMP_DATA         *wsmpData;
    EAS_U32             instCount;
    EAS_U32             regionCount;
    EAS_U32             artCount;
    EAS_U32             waveCount;
    EAS_U32             wavePoolSize;
    EAS_U32             wavePoolOffset;
    EAS_BOOL            bigEndian;
    EAS_BOOL            filterUsed;
} SDLS_SYNTHESIZER_DATA;

/* connection lookup table */
typedef struct s_connection_tag
{
    EAS_U16 source;
    EAS_U16 control;
    EAS_U16 destination;
    EAS_U16 connection;
} S_CONNECTION;

static const S_CONNECTION connTable[] =
{
    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_LFO_FREQUENCY, PARAM_MOD_LFO_FREQ },
    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_LFO_STARTDELAY, PARAM_MOD_LFO_DELAY},

    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_VIB_FREQUENCY, PARAM_VIB_LFO_FREQ },
    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_VIB_STARTDELAY, PARAM_VIB_LFO_DELAY },

    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_EG1_DELAYTIME, PARAM_VOL_EG_DELAY },
    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_EG1_ATTACKTIME, PARAM_VOL_EG_ATTACK },
    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_EG1_HOLDTIME, PARAM_VOL_EG_HOLD },
    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_EG1_DECAYTIME, PARAM_VOL_EG_DECAY },
    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_EG1_SUSTAINLEVEL, PARAM_VOL_EG_SUSTAIN },
    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_EG1_RELEASETIME, PARAM_VOL_EG_RELEASE },
    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_EG1_SHUTDOWNTIME, PARAM_VOL_EG_SHUTDOWN },
    { CONN_SRC_KEYONVELOCITY, CONN_SRC_NONE, CONN_DST_EG1_ATTACKTIME, PARAM_VOL_EG_VEL_TO_ATTACK },
    { CONN_SRC_KEYNUMBER, CONN_SRC_NONE, CONN_DST_EG1_DECAYTIME, PARAM_VOL_EG_KEY_TO_DECAY },
    { CONN_SRC_KEYNUMBER, CONN_SRC_NONE, CONN_DST_EG1_HOLDTIME, PARAM_VOL_EG_KEY_TO_HOLD },

    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_EG2_DELAYTIME, PARAM_MOD_EG_DELAY },
    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_EG2_ATTACKTIME, PARAM_MOD_EG_ATTACK },
    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_EG2_HOLDTIME, PARAM_MOD_EG_HOLD },
    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_EG2_DECAYTIME, PARAM_MOD_EG_DECAY },
    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_EG2_SUSTAINLEVEL, PARAM_MOD_EG_SUSTAIN },
    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_EG2_RELEASETIME, PARAM_MOD_EG_RELEASE },
    { CONN_SRC_KEYONVELOCITY, CONN_SRC_NONE, CONN_DST_EG2_ATTACKTIME, PARAM_MOD_EG_VEL_TO_ATTACK },
    { CONN_SRC_KEYNUMBER, CONN_SRC_NONE, CONN_DST_EG2_DECAYTIME, PARAM_MOD_EG_KEY_TO_DECAY },
    { CONN_SRC_KEYNUMBER, CONN_SRC_NONE, CONN_DST_EG2_HOLDTIME, PARAM_MOD_EG_KEY_TO_HOLD },

    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_FILTER_CUTOFF, PARAM_INITIAL_FC },
    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_FILTER_Q, PARAM_INITIAL_Q },
    { CONN_SRC_LFO, CONN_SRC_NONE, CONN_DST_FILTER_CUTOFF, PARAM_MOD_LFO_TO_FC },
    { CONN_SRC_LFO, CONN_SRC_CC1, CONN_DST_FILTER_CUTOFF, PARAM_MOD_LFO_CC1_TO_FC },
    { CONN_SRC_LFO, CONN_SRC_CHANNELPRESSURE, CONN_DST_FILTER_CUTOFF, PARAM_MOD_LFO_CHAN_PRESS_TO_FC },
    { CONN_SRC_EG2, CONN_SRC_NONE, CONN_DST_FILTER_CUTOFF, PARAM_MOD_EG_TO_FC },
    { CONN_SRC_KEYONVELOCITY, CONN_SRC_NONE, CONN_DST_FILTER_CUTOFF, PARAM_VEL_TO_FC },
    { CONN_SRC_KEYNUMBER, CONN_SRC_NONE, CONN_DST_FILTER_CUTOFF, PARAM_KEYNUM_TO_FC },

    { CONN_SRC_LFO, CONN_SRC_NONE, CONN_DST_GAIN, PARAM_MOD_LFO_TO_GAIN },
    { CONN_SRC_LFO, CONN_SRC_CC1, CONN_DST_GAIN, PARAM_MOD_LFO_CC1_TO_GAIN },
    { CONN_SRC_LFO, CONN_SRC_CHANNELPRESSURE, CONN_DST_GAIN, PARAM_MOD_LFO_CHAN_PRESS_TO_GAIN },
    { CONN_SRC_KEYONVELOCITY, CONN_SRC_NONE, CONN_DST_GAIN, PARAM_VEL_TO_GAIN },

    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_PITCH, PARAM_TUNING },
    { CONN_SRC_KEYNUMBER, CONN_SRC_NONE, CONN_DST_PITCH, PARAM_KEYNUM_TO_PITCH },
    { CONN_SRC_VIBRATO, CONN_SRC_NONE, CONN_DST_PITCH, PARAM_VIB_LFO_TO_PITCH },
    { CONN_SRC_VIBRATO, CONN_SRC_CC1, CONN_DST_PITCH, PARAM_VIB_LFO_CC1_TO_PITCH },
    { CONN_SRC_VIBRATO, CONN_SRC_CHANNELPRESSURE, CONN_DST_PITCH, PARAM_VIB_LFO_CHAN_PRESS_TO_PITCH },
    { CONN_SRC_LFO, CONN_SRC_NONE, CONN_DST_PITCH, PARAM_MOD_LFO_TO_PITCH },
    { CONN_SRC_LFO, CONN_SRC_CC1, CONN_DST_PITCH, PARAM_MOD_LFO_CC1_TO_PITCH },
    { CONN_SRC_LFO, CONN_SRC_CHANNELPRESSURE, CONN_DST_PITCH, PARAM_MOD_LFO_CHAN_PRESS_TO_PITCH },
    { CONN_SRC_EG2, CONN_SRC_NONE, CONN_DST_PITCH, PARAM_MOD_EG_TO_PITCH },

    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_PAN, PARAM_DEFAULT_PAN },
    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_REVERB, PARAM_DEFAULT_REVERB_SEND },
    { CONN_SRC_CC91, CONN_SRC_NONE, CONN_DST_REVERB, PARAM_MIDI_CC91_TO_REVERB_SEND },
    { CONN_SRC_NONE, CONN_SRC_NONE, CONN_DST_CHORUS, PARAM_DEFAULT_CHORUS_SEND },
    { CONN_SRC_CC93, CONN_SRC_NONE, CONN_DST_REVERB, PARAM_MIDI_CC93_TO_CHORUS_SEND }
};
#define ENTRIES_IN_CONN_TABLE (sizeof(connTable)/sizeof(S_CONNECTION))

static const S_DLS_ART_VALUES defaultArt =
{
    {
    0,              /* not modified */
    -851,           /* Mod LFO frequency: 5 Hz */
    -7973,          /* Mod LFO delay: 10 milliseconds */

    -851,           /* Vib LFO frequency: 5 Hz */
    -7973,          /* Vib LFO delay: 10 milliseconds */

    -32768,         /* EG1 delay time: 0 secs */
    -32768,         /* EG1 attack time: 0 secs */
    -32768,         /* EG1 hold time: 0 secs */
    -32768,         /* EG1 decay time: 0 secs */
    1000,           /* EG1 sustain level: 100.0% */
    -32768,         /* EG1 release time: 0 secs */
    -7271,          /* EG1 shutdown time: 15 msecs */
    0,              /* EG1 velocity to attack: 0 time cents */
    0,              /* EG1 key number to decay: 0 time cents */
    0,              /* EG1 key number to hold: 0 time cents */

    -32768,         /* EG2 delay time: 0 secs */
    -32768,         /* EG2 attack time: 0 secs */
    -32768,         /* EG2 hold time: 0 secs */
    -32768,         /* EG2 decay time: 0 secs */
    1000,           /* EG2 sustain level: 100.0% */
    -32768,         /* EG2 release time: 0 secs */
    0,              /* EG2 velocity to attack: 0 time cents */
    0,              /* EG2 key number to decay: 0 time cents */
    0,              /* EG2 key number to hold: 0 time cents */

    0x7fff,         /* Initial Fc: Disabled */
    0,              /* Initial Q: 0 dB */
    0,              /* Mod LFO to Fc: 0 cents */
    0,              /* Mod LFO CC1 to Fc: 0 cents */
    0,              /* Mod LFO channel pressure to Fc: 0 cents */
    0,              /* EG2 to Fc: 0 cents */
    0,              /* Velocity to Fc: 0 cents */
    0,              /* Key number to Fc: 0 cents */

    0,              /* Mod LFO to gain: 0 dB */
    0,              /* Mod LFO CC1 to gain: 0 dB */
    0,              /* Mod LFO channel pressure to gain: 0 dB */
    960,            /* Velocity to gain: 96 dB */

    0,              /* Tuning: 0 cents */
    12800,          /* Key number to pitch: 12,800 cents */
    0,              /* Vibrato to pitch: 0 cents */
    0,              /* Vibrato CC1 to pitch: 0 cents */
    0,              /* Vibrato channel pressure to pitch: 0 cents */
    0,              /* Mod LFO to pitch: 0 cents */
    0,              /* Mod LFO CC1 to pitch: 0 cents */
    0,              /* Mod LFO channel pressure to pitch: 0 cents */
    0,              /* Mod EG to pitch: 0 cents */

    0,              /* Default pan: 0.0% */
    0,              /* Default reverb send: 0.0% */
    1000,           /* Default CC91 to reverb send: 100.0% */
    0,              /* Default chorus send: 0.0% */
    1000            /* Default CC93 to chorus send: 100.0% */
    }
};

/*------------------------------------
 * local variables
 *------------------------------------
*/

#if defined(_8_BIT_SAMPLES)
static const EAS_INT bitDepth = 8;
#elif defined(_16_BIT_SAMPLES)
static const EAS_INT bitDepth = 16;
#else
#error "Must define _8_BIT_SAMPLES or _16_BIT_SAMPLES"
#endif

static const EAS_U32 outputSampleRate = _OUTPUT_SAMPLE_RATE;
static const EAS_I32 dlsRateConvert = DLS_RATE_CONVERT;
static const EAS_I32 dlsLFOFrequencyConvert = DLS_LFO_FREQUENCY_CONVERT;

/*------------------------------------
 * inline functions
 *------------------------------------
*/
EAS_INLINE void *PtrOfs (void *p, EAS_I32 offset)
{
    return (void*) (((EAS_U8*) p) + offset);
}

/*------------------------------------
 * prototypes
 *------------------------------------
*/
static EAS_RESULT NextChunk (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 *pPos, EAS_U32 *pChunkType, EAS_I32 *pSize);
static EAS_RESULT Parse_ptbl (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_I32 wsmpPos, EAS_I32 wsmpSize);
static EAS_RESULT Parse_wave (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_U16 waveIndex);
static EAS_RESULT Parse_wsmp (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, S_WSMP_DATA *p);
static EAS_RESULT Parse_fmt (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, S_WSMP_DATA *p);
static EAS_RESULT Parse_data (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_I32 size, S_WSMP_DATA *p, EAS_SAMPLE *pSample, EAS_U32 sampleLen);
static EAS_RESULT Parse_lins(SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_I32 size);
static EAS_RESULT Parse_ins (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_I32 size);
static EAS_RESULT Parse_insh (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_U32 *pRgnCount, EAS_U32 *pLocale);
static EAS_RESULT Parse_lrgn (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_I32 size, EAS_U16 artIndex, EAS_U32 numRegions);
static EAS_RESULT Parse_rgn (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_I32 size, EAS_U16 artIndex);
static EAS_RESULT Parse_rgnh (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, S_DLS_REGION *pRgn);
static EAS_RESULT Parse_lart (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_I32 size, S_DLS_ART_VALUES *pArt);
static EAS_RESULT Parse_art (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, S_DLS_ART_VALUES *pArt);
static EAS_RESULT Parse_wlnk (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_U32 *pWaveIndex);
static EAS_RESULT Parse_cdl (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 size, EAS_U32 *pValue);
static void Convert_rgn (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_U16 regionIndex, EAS_U16 artIndex, EAS_U16 waveIndex, S_WSMP_DATA *pWsmp);
static void Convert_art (SDLS_SYNTHESIZER_DATA *pDLSData, const S_DLS_ART_VALUES *pDLSArt,  EAS_U16 artIndex);
static EAS_I16 ConvertSampleRate (EAS_U32 sampleRate);
static EAS_I16 ConvertSustain (EAS_I32 sustain);
static EAS_I16 ConvertLFOPhaseIncrement (EAS_I32 pitchCents);
static EAS_I8 ConvertPan (EAS_I32 pan);
static EAS_U8 ConvertQ (EAS_I32 q);

#ifdef _DEBUG_DLS
static void DumpDLS (S_EAS *pEAS);
#endif


/*----------------------------------------------------------------------------
 * DLSParser ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 * pEASData - pointer to over EAS data instance
 * fileHandle - file handle for input file
 * offset - offset into file where DLS data starts
 *
 * Outputs:
 * EAS_RESULT
 * ppEAS - address of pointer to alternate EAS wavetable
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT DLSParser (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE fileHandle, EAS_I32 offset, EAS_DLSLIB_HANDLE *ppDLS)
{
    EAS_RESULT result;
    SDLS_SYNTHESIZER_DATA dls;
    EAS_U32 temp;
    uint32_t chunk_type;
    EAS_I32 pos;
    EAS_I32 chunkPos;
    EAS_I32 size;
    EAS_I32 instSize;
    EAS_I32 rgnPoolSize;
    EAS_I32 artPoolSize;
    EAS_I32 waveLenSize;
    EAS_I32 endDLS;
    EAS_I32 wvplPos;
    EAS_I32 wvplSize;
    EAS_I32 linsPos;
    EAS_I32 linsSize;
    EAS_I32 ptblPos;
    EAS_I32 ptblSize;
    void *p;

    /* zero counts and pointers */
    EAS_HWMemSet(&dls, 0, sizeof(dls));

    /* save file handle and hwInstData to save copying pointers around */
    dls.hwInstData = hwInstData;
    dls.fileHandle = fileHandle;

    /* NULL return value in case of error */
    *ppDLS = NULL;

    /* seek to start of DLS and read in RIFF tag and set processor endian flag */
    if ((result = EAS_HWFileSeek(dls.hwInstData, dls.fileHandle, offset)) != EAS_SUCCESS)
        return result;
    if ((result = EAS_HWReadFile(dls.hwInstData, dls.fileHandle, &chunk_type, sizeof(chunk_type), &size)) != EAS_SUCCESS)
        return result;

    /* check for processor endian-ness */
    dls.bigEndian = (chunk_type == CHUNK_RIFF);

    /* first chunk should be DLS */
    pos = offset;
    if ((result = NextChunk(&dls, &pos, &temp, &size)) != EAS_SUCCESS)
        return result;
    if (temp != CHUNK_DLS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Expected DLS chunk, got %08lx\n", temp); */ }
        return EAS_ERROR_FILE_FORMAT;
    }

    /* no instrument or wavepool chunks */
    linsSize = wvplSize = ptblSize = linsPos = wvplPos = ptblPos = 0;

    /* scan the chunks in the DLS list */
    endDLS = offset + size;
    pos = offset + 12;
    while (pos < endDLS)
    {
        chunkPos = pos;

        /* get the next chunk type */
        if ((result = NextChunk(&dls, &pos, &temp, &size)) != EAS_SUCCESS)
            return result;

        /* parse useful chunks */
        switch (temp)
        {
            case CHUNK_CDL:
                if ((result = Parse_cdl(&dls, size, &temp)) != EAS_SUCCESS)
                    return result;
                if (!temp)
                    return EAS_ERROR_UNRECOGNIZED_FORMAT;
                break;

            case CHUNK_LINS:
                linsPos = chunkPos + 12;
                linsSize = size - 4;
                break;

            case CHUNK_WVPL:
                wvplPos = chunkPos + 12;
                wvplSize = size - 4;
                break;

            case CHUNK_PTBL:
                ptblPos = chunkPos + 8;
                ptblSize = size - 4;
                break;

            default:
                break;
        }
    }

    /* must have a lins chunk */
    if (linsSize == 0)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "No lins chunk found"); */ }
        return EAS_ERROR_UNRECOGNIZED_FORMAT;
    }

    /* must have a wvpl chunk */
    if (wvplSize == 0)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "No wvpl chunk found"); */ }
        return EAS_ERROR_UNRECOGNIZED_FORMAT;
    }

    /* must have a ptbl chunk */
    if ((ptblSize == 0) || (ptblSize > (EAS_I32) (DLS_MAX_WAVE_COUNT * sizeof(POOLCUE) + sizeof(POOLTABLE))))
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "No ptbl chunk found"); */ }
        return EAS_ERROR_UNRECOGNIZED_FORMAT;
    }

    /* pre-parse the wave pool chunk */
    if ((result = Parse_ptbl(&dls, ptblPos, wvplPos, wvplSize)) != EAS_SUCCESS)
        return result;

    /* limit check  */
    if ((dls.waveCount == 0) || (dls.waveCount > DLS_MAX_WAVE_COUNT))
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "DLS file contains invalid #waves [%u]\n", dls.waveCount); */ }
        return EAS_ERROR_FILE_FORMAT;
    }

    /* allocate memory for wsmp data */
    dls.wsmpData = EAS_HWMalloc(dls.hwInstData, (EAS_I32) (sizeof(S_WSMP_DATA) * dls.waveCount));
    if (dls.wsmpData == NULL)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "EAS_HWMalloc for wsmp data failed\n"); */ }
        return EAS_ERROR_MALLOC_FAILED;
    }
    EAS_HWMemSet(dls.wsmpData, 0, (EAS_I32) (sizeof(S_WSMP_DATA) * dls.waveCount));

    /* pre-parse the lins chunk */
    result = Parse_lins(&dls, linsPos, linsSize);
    if (result == EAS_SUCCESS)
    {

        /* limit check  */
        if ((dls.regionCount == 0) || (dls.regionCount > DLS_MAX_REGION_COUNT))
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "DLS file contains invalid #regions [%u]\n", dls.regionCount); */ }
            EAS_HWFree(dls.hwInstData, dls.wsmpData);
            return EAS_ERROR_FILE_FORMAT;
        }

        /* limit check  */
        if ((dls.artCount == 0) || (dls.artCount > DLS_MAX_ART_COUNT))
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "DLS file contains invalid #articulations [%u]\n", dls.regionCount); */ }
            EAS_HWFree(dls.hwInstData, dls.wsmpData);
            return EAS_ERROR_FILE_FORMAT;
        }

        /* limit check  */
        if ((dls.instCount == 0) || (dls.instCount > DLS_MAX_INST_COUNT))
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "DLS file contains invalid #instruments [%u]\n", dls.instCount); */ }
            EAS_HWFree(dls.hwInstData, dls.wsmpData);
            return EAS_ERROR_FILE_FORMAT;
        }

        /* Allocate memory for the converted DLS data */
        /* calculate size of instrument data */
        instSize = (EAS_I32) (sizeof(S_PROGRAM) * dls.instCount);

        /* calculate size of region pool */
        rgnPoolSize = (EAS_I32) (sizeof(S_DLS_REGION) * dls.regionCount);

        /* calculate size of articulation pool, add one for default articulation */
        dls.artCount++;
        artPoolSize = (EAS_I32) (sizeof(S_DLS_ARTICULATION) * dls.artCount);

        /* calculate size of wave length and offset arrays */
        waveLenSize = (EAS_I32) (dls.waveCount * sizeof(EAS_U32));

        /* calculate final memory size */
        size = (EAS_I32) sizeof(S_EAS) + instSize + rgnPoolSize + artPoolSize + (2 * waveLenSize) + (EAS_I32) dls.wavePoolSize;
        if (size <= 0) {
            EAS_HWFree(dls.hwInstData, dls.wsmpData);
            return EAS_ERROR_FILE_FORMAT;
        }

        /* allocate the main EAS chunk */
        dls.pDLS = EAS_HWMalloc(dls.hwInstData, size);
        if (dls.pDLS == NULL)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "EAS_HWMalloc failed for DLS memory allocation size %ld\n", size); */ }
            EAS_HWFree(dls.hwInstData, dls.wsmpData);
            return EAS_ERROR_MALLOC_FAILED;
        }
        EAS_HWMemSet(dls.pDLS, 0, size);
        dls.pDLS->refCount = 1;
        p = PtrOfs(dls.pDLS, sizeof(S_EAS));

        /* setup pointer to programs */
        dls.pDLS->numDLSPrograms = (EAS_U16) dls.instCount;
        dls.pDLS->pDLSPrograms = p;
        p = PtrOfs(p, instSize);

        /* setup pointer to regions */
        dls.pDLS->pDLSRegions = p;
        dls.pDLS->numDLSRegions = (EAS_U16) dls.regionCount;
        p = PtrOfs(p, rgnPoolSize);

        /* setup pointer to articulations */
        dls.pDLS->numDLSArticulations = (EAS_U16) dls.artCount;
        dls.pDLS->pDLSArticulations = p;
        p = PtrOfs(p, artPoolSize);

        /* setup pointer to wave length table */
        dls.pDLS->numDLSSamples = (EAS_U16) dls.waveCount;
        dls.pDLS->pDLSSampleLen = p;
        p = PtrOfs(p, waveLenSize);

        /* setup pointer to wave offsets table */
        dls.pDLS->pDLSSampleOffsets = p;
        p = PtrOfs(p, waveLenSize);

        /* setup pointer to wave pool */
        dls.pDLS->pDLSSamples = p;

        /* clear filter flag */
        dls.filterUsed = EAS_FALSE;

        /* parse the wave pool and load samples */
        result = Parse_ptbl(&dls, ptblPos, wvplPos, wvplSize);
    }

    /* create the default articulation */
    if (dls.pDLS) {
        Convert_art(&dls, &defaultArt, 0);
        dls.artCount = 1;
    }

    /* parse the lins chunk and load instruments */
    dls.regionCount = dls.instCount = 0;
    if (result == EAS_SUCCESS)
        result = Parse_lins(&dls, linsPos, linsSize);

    /* clean up any temporary objects that were allocated */
    if (dls.wsmpData)
        EAS_HWFree(dls.hwInstData, dls.wsmpData);

    /* if successful, return a pointer to the EAS collection */
    if (result == EAS_SUCCESS)
    {
        *ppDLS = dls.pDLS;
#ifdef _DEBUG_DLS
        DumpDLS(dls.pDLS);
#endif
    }

    /* something went wrong, deallocate the EAS collection */
    else
        DLSCleanup(dls.hwInstData, dls.pDLS);

    return result;
}

/*----------------------------------------------------------------------------
 * DLSCleanup ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 * pEASData - pointer to over EAS data instance
 * pEAS - pointer to alternate EAS wavetable
 *
 * Outputs:
 * EAS_RESULT
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT DLSCleanup (EAS_HW_DATA_HANDLE hwInstData, S_DLS *pDLS)
{

    /* free the allocated memory */
    if (pDLS)
    {
        if (pDLS->refCount)
        {
            if (--pDLS->refCount == 0)
                EAS_HWFree(hwInstData, pDLS);
        }
    }
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * DLSAddRef ()
 *----------------------------------------------------------------------------
 * Increment reference count
 *----------------------------------------------------------------------------
*/
void DLSAddRef (S_DLS *pDLS)
{
    if (pDLS)
        pDLS->refCount++;
}

/*----------------------------------------------------------------------------
 * NextChunk ()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the type and size of the next chunk in the file
 *
 * Inputs:
 *
 * Outputs:
 *
 * Side Effects:
 *----------------------------------------------------------------------------
*/
static EAS_RESULT NextChunk (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 *pPos, EAS_U32 *pChunkType, EAS_I32 *pSize)
{
    EAS_RESULT result;

    /* seek to start of chunk */
    if ((result = EAS_HWFileSeek(pDLSData->hwInstData, pDLSData->fileHandle, *pPos)) != EAS_SUCCESS)
        return result;

    /* read the chunk type */
    if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, pChunkType, EAS_TRUE)) != EAS_SUCCESS)
        return result;

    /* read the chunk size */
    if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, pSize, EAS_FALSE)) != EAS_SUCCESS)
        return result;

    if (*pSize < 0) {
        ALOGE("b/37093318");
        return EAS_ERROR_FILE_FORMAT;
    }

    /* get form type for RIFF and LIST types */
    if ((*pChunkType == CHUNK_RIFF) || (*pChunkType == CHUNK_LIST))
    {

        /* read the form type */
        if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, pChunkType, EAS_TRUE)) != EAS_SUCCESS)
            return result;

    }

    /* calculate start of next chunk */
    *pPos += *pSize + 8;

    /* adjust to word boundary */
    if (*pPos & 1)
        (*pPos)++;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * Parse_ptbl ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT Parse_ptbl (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_I32 wtblPos, EAS_I32 wtblSize)
{
    EAS_RESULT result;
    EAS_U32 temp;
    EAS_FILE_HANDLE tempFile;
    EAS_U16 waveIndex;

    /* seek to start of chunk */
    if ((result = EAS_HWFileSeek(pDLSData->hwInstData, pDLSData->fileHandle, pos)) != EAS_SUCCESS)
        return result;

    /* get the structure size */
    if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &temp, EAS_FALSE)) != EAS_SUCCESS)
        return result;

    /* get the number of waves */
    if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &pDLSData->waveCount, EAS_FALSE)) != EAS_SUCCESS)
        return result;

    /* if second pass, ensure waveCount matches with the value parsed in first pass */
    if (pDLSData->pDLS)
    {
        if (pDLSData->waveCount != pDLSData->pDLS->numDLSSamples)
        {
            return EAS_ERROR_DATA_INCONSISTENCY;
        }
    }

#if 0
    /* just need the wave count on the first pass */
    if (!pDLSData->pDLS)
        return EAS_SUCCESS;
#endif

    /* open duplicate file handle */
    if ((result = EAS_HWDupHandle(pDLSData->hwInstData, pDLSData->fileHandle, &tempFile)) != EAS_SUCCESS)
        return result;

    /* read to end of chunk */
    for (waveIndex = 0; waveIndex < pDLSData->waveCount; waveIndex++)
    {

        /* get the offset to the wave and make sure it is within the wtbl chunk */
        if ((result = EAS_HWGetDWord(pDLSData->hwInstData, tempFile, &temp, EAS_FALSE)) != EAS_SUCCESS)
            return result;
        if (temp > (EAS_U32) wtblSize)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Ptbl offset exceeds size of wtbl\n"); */ }
            EAS_HWCloseFile(pDLSData->hwInstData, tempFile);
            return EAS_ERROR_FILE_FORMAT;
        }

        /* parse the wave */
        if ((result = Parse_wave(pDLSData, wtblPos +(EAS_I32)  temp, waveIndex)) != EAS_SUCCESS)
            return result;
    }

    /* close the temporary handle and return */
    EAS_HWCloseFile(pDLSData->hwInstData, tempFile);
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * Parse_wave ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT Parse_wave (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_U16 waveIndex)
{
    EAS_RESULT result;
    EAS_U32 temp;
    EAS_I32 size;
    EAS_I32 endChunk;
    EAS_I32 chunkPos;
    EAS_I32 wsmpPos = 0;
    EAS_I32 fmtPos = 0;
    EAS_I32 dataPos = 0;
    EAS_I32 dataSize = 0;
    S_WSMP_DATA *p;
    void *pSample;
    S_WSMP_DATA wsmp;

    /* seek to start of chunk */
    chunkPos = pos + 12;
    if ((result = EAS_HWFileSeek(pDLSData->hwInstData, pDLSData->fileHandle, pos)) != EAS_SUCCESS)
        return result;

    /* get the chunk type */
    if ((result = NextChunk(pDLSData, &pos, &temp, &size)) != EAS_SUCCESS)
        return result;

    /* make sure it is a wave chunk */
    if (temp != CHUNK_WAVE)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Offset in ptbl does not point to wave chunk\n"); */ }
        return EAS_ERROR_FILE_FORMAT;
    }

    /* read to end of chunk */
    pos = chunkPos;
    endChunk = pos + size;
    while (pos < endChunk)
    {
        chunkPos = pos;

        /* get the chunk type */
        if ((result = NextChunk(pDLSData, &pos, &temp, &size)) != EAS_SUCCESS)
            return result;

        /* parse useful chunks */
        switch (temp)
        {
            case CHUNK_WSMP:
                wsmpPos = chunkPos + 8;
                break;

            case CHUNK_FMT:
                fmtPos = chunkPos + 8;
                break;

            case CHUNK_DATA:
                dataPos = chunkPos + 8;
                dataSize = size;
                break;

            default:
                break;
        }
    }

    // limit to reasonable size
    if (dataSize < 0 || dataSize > MAX_DLS_WAVE_SIZE)
    {
        return EAS_ERROR_SOUND_LIBRARY;
    }

    /* for first pass, use temporary variable */
    if (pDLSData->pDLS == NULL)
        p = &wsmp;
    else
        p = &pDLSData->wsmpData[waveIndex];

    /* set the defaults */
    p->fineTune = 0;
    p->unityNote = 60;
    p->gain = 0;
    p->loopStart = 0;
    p->loopLength = 0;

    /* must have a fmt chunk */
    if (!fmtPos)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "DLS wave chunk has no fmt chunk\n"); */ }
        return EAS_ERROR_UNRECOGNIZED_FORMAT;
    }

    /* must have a data chunk */
    if (!dataPos)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "DLS wave chunk has no data chunk\n"); */ }
        return EAS_ERROR_UNRECOGNIZED_FORMAT;
    }

    /* parse the wsmp chunk */
    if (wsmpPos)
    {
        if ((result = Parse_wsmp(pDLSData, wsmpPos, p)) != EAS_SUCCESS)
            return result;
    }

    /* parse the fmt chunk */
    if ((result = Parse_fmt(pDLSData, fmtPos, p)) != EAS_SUCCESS)
        return result;

    /* calculate the size of the wavetable needed. We need only half
     * the memory for 16-bit samples when in 8-bit mode, and we need
     * double the memory for 8-bit samples in 16-bit mode. For
     * unlooped samples, we may use ADPCM. If so, we need only 1/4
     * the memory.
     *
     * We also need to add one for looped samples to allow for
     * the first sample to be copied to the end of the loop.
     */

    /* use ADPCM encode for unlooped 16-bit samples if ADPCM is enabled */
    /*lint -e{506} -e{774} groundwork for future version to support 8 & 16 bit */
    if (bitDepth == 8)
    {
        if (p->bitsPerSample == 8)
            size = dataSize;
        else
            /*lint -e{704} use shift for performance */
            size = dataSize >> 1;
        if (p->loopLength)
            size++;
    }

    else
    {
        if (p->bitsPerSample == 16)
            size = dataSize;
        else
            /*lint -e{703} use shift for performance */
            size = dataSize << 1;
        if (p->loopLength)
            size += 2;
    }

    /* for first pass, add size to wave pool size and return */
    if (pDLSData->pDLS == NULL)
    {
        pDLSData->wavePoolSize += (EAS_U32) size;
        return EAS_SUCCESS;
    }

    /* allocate memory and read in the sample data */
    pSample = (EAS_U8*)pDLSData->pDLS->pDLSSamples + pDLSData->wavePoolOffset;
    pDLSData->pDLS->pDLSSampleOffsets[waveIndex] = pDLSData->wavePoolOffset;
    pDLSData->pDLS->pDLSSampleLen[waveIndex] = (EAS_U32) size;
    pDLSData->wavePoolOffset += (EAS_U32) size;
    if (pDLSData->wavePoolOffset > pDLSData->wavePoolSize)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Wave pool exceeded allocation\n"); */ }
        return EAS_ERROR_SOUND_LIBRARY;
    }

    if ((result = Parse_data(pDLSData, dataPos, dataSize, p, pSample, (EAS_U32)size)) != EAS_SUCCESS)
        return result;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * Parse_wsmp ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT Parse_wsmp (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, S_WSMP_DATA *p)
{
    EAS_RESULT result;
    EAS_U16 wtemp;
    EAS_U32 ltemp;
    EAS_U32 cbSize;

    /* seek to start of chunk */
    if ((result = EAS_HWFileSeek(pDLSData->hwInstData, pDLSData->fileHandle, pos)) != EAS_SUCCESS)
        return result;

    /* get structure size */
    if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &cbSize, EAS_FALSE)) != EAS_SUCCESS)
        return result;

    /* get unity note */
    if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &wtemp, EAS_FALSE)) != EAS_SUCCESS)
        return result;
    if (wtemp <= 127)
        p->unityNote = (EAS_U8) wtemp;
    else
    {
        p->unityNote = 60;
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Invalid unity note [%u] in DLS wsmp ignored, set to 60\n", wtemp); */ }
    }

    /* get fine tune */
    if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &p->fineTune, EAS_FALSE)) != EAS_SUCCESS)
        return result;

    /* get gain */
    if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &p->gain, EAS_FALSE)) != EAS_SUCCESS)
        return result;
    if (p->gain > 0)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Positive gain [%ld] in DLS wsmp ignored, set to 0dB\n", p->gain); */ }
        p->gain = 0;
    }

    /* option flags */
    if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &ltemp, EAS_FALSE)) != EAS_SUCCESS)
        return result;

    /* sample loops */
    if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &ltemp, EAS_FALSE)) != EAS_SUCCESS)
        return result;

    /* if looped sample, get loop data */
    if (ltemp)
    {

        if (ltemp > 1)
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "DLS sample with %lu loops, ignoring extra loops\n", ltemp); */ }

        /* skip ahead to loop data */
        if ((result = EAS_HWFileSeek(pDLSData->hwInstData, pDLSData->fileHandle, pos + (EAS_I32) cbSize)) != EAS_SUCCESS)
            return result;

        /* get structure size */
        if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &ltemp, EAS_FALSE)) != EAS_SUCCESS)
            return result;

        /* get loop type */
        if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &ltemp, EAS_FALSE)) != EAS_SUCCESS)
            return result;

        /* get loop start */
        if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &p->loopStart, EAS_FALSE)) != EAS_SUCCESS)
            return result;

        /* get loop length */
        if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &p->loopLength, EAS_FALSE)) != EAS_SUCCESS)
            return result;

        /* ensure no overflow */
        if (p->loopLength
            && ((p->loopStart > EAS_U32_MAX - p->loopLength)
                || (p->loopStart + p->loopLength > EAS_U32_MAX / sizeof(EAS_SAMPLE))))
        {
            return EAS_FAILURE;
        }
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * Parse_fmt ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT Parse_fmt (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, S_WSMP_DATA *p)
{
    EAS_RESULT result;
    EAS_U16 wtemp;
    EAS_U32 ltemp;

    /* seek to start of chunk */
    if ((result = EAS_HWFileSeek(pDLSData->hwInstData, pDLSData->fileHandle, pos)) != EAS_SUCCESS)
        return result;

    /* get format tag */
    if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &wtemp, EAS_FALSE)) != EAS_SUCCESS)
        return result;
    if (wtemp != WAVE_FORMAT_PCM)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Unsupported DLS sample format %04x\n", wtemp); */ }
        return EAS_ERROR_UNRECOGNIZED_FORMAT;
    }

    /* get number of channels */
    if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &wtemp, EAS_FALSE)) != EAS_SUCCESS)
        return result;
    if (wtemp != 1)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "No support for DLS multi-channel samples\n"); */ }
        return EAS_ERROR_UNRECOGNIZED_FORMAT;
    }

    /* get sample rate */
    if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &p->sampleRate, EAS_FALSE)) != EAS_SUCCESS)
        return result;

    /* bytes/sec */
    if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &ltemp, EAS_FALSE)) != EAS_SUCCESS)
        return result;

    /* block align */
    if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &wtemp, EAS_FALSE)) != EAS_SUCCESS)
        return result;

    /* bits/sample */
    if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &p->bitsPerSample, EAS_FALSE)) != EAS_SUCCESS)
        return result;

    if ((p->bitsPerSample != 8) && (p->bitsPerSample != 16))
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Unsupported DLS bits-per-sample %d\n", p->bitsPerSample); */ }
        return EAS_ERROR_UNRECOGNIZED_FORMAT;
    }

    return EAS_SUCCESS;
}

#if defined( _8_BIT_SAMPLES)
/*----------------------------------------------------------------------------
 * Parse_data ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * NOTE: The optimized assembly versions of the interpolator require
 * an extra sample at the end of the loop - a copy of the first
 * sample. This routine must allocate an extra sample of data and
 * copy the first sample of the loop to the end.
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT Parse_data (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_I32 size, S_WSMP_DATA *pWsmp, EAS_SAMPLE *pSample, EAS_U32 sampleLen)
{
    EAS_RESULT result;
    EAS_U8 convBuf[SAMPLE_CONVERT_CHUNK_SIZE];
    EAS_I32 count;
    EAS_I32 i;
    EAS_I8 *p;

    /* seek to start of chunk */
    if ((result = EAS_HWFileSeek(pDLSData->hwInstData, pDLSData->fileHandle, pos)) != EAS_SUCCESS)
        return result;

    /* 8-bit samples in an 8-bit synth, just copy the data, and flip bit 7 */
    p = pSample;
    if (pWsmp->bitsPerSample == 8)
    {
        if ((result = EAS_HWReadFile(pDLSData->hwInstData, pDLSData->fileHandle, pSample, size, &count)) != EAS_SUCCESS)
            return result;
        for (i = 0; i < size; i++)
            /*lint -e{734} convert from unsigned to signed audio */
            *p++ ^= 0x80;
    }

    /* 16-bit samples, need to convert to 8-bit or ADPCM */
    else
    {

        while (size)
        {
            EAS_I8 *pInput;

            /* for undithered conversion, we're just copying the 8-bit data */
            if (pDLSData->bigEndian)
                pInput = (EAS_I8*) convBuf;
            else
                pInput = (EAS_I8*) convBuf + 1;

            /* read a small chunk of data and convert it */
            count = (size < SAMPLE_CONVERT_CHUNK_SIZE ? size : SAMPLE_CONVERT_CHUNK_SIZE);
            if ((result = EAS_HWReadFile(pDLSData->hwInstData, pDLSData->fileHandle, convBuf, count, &count)) != EAS_SUCCESS)
                return result;
            size -= count;
            /*lint -e{704} use shift for performance */
            count = count >> 1;

            while (count--)
            {
                *p++ = *pInput;
                pInput += 2;
            }
        }
    }

    /* for looped samples, copy the last sample to the end */
    if (pWsmp->loopLength)
    {
        if (sampleLen < sizeof(EAS_SAMPLE)
            || (pWsmp->loopStart + pWsmp->loopLength) * sizeof(EAS_SAMPLE) > sampleLen - sizeof(EAS_SAMPLE))
        {
            return EAS_FAILURE;
        }

        pSample[pWsmp->loopStart + pWsmp->loopLength] = pSample[pWsmp->loopStart];
    }

    return EAS_SUCCESS;
}
#elif defined(_16_BIT_SAMPLES)
static EAS_RESULT Parse_data (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_I32 size, S_WSMP_DATA *pWsmp, EAS_SAMPLE *pSample, EAS_U32 sampleLen)
{
    EAS_RESULT result;
    EAS_U8 convBuf[SAMPLE_CONVERT_CHUNK_SIZE];
    EAS_I32 count = 0;
    EAS_I32 i;
    EAS_I16 *p;

    /* seek to start of chunk */
    if ((result = EAS_HWFileSeek(pDLSData->hwInstData, pDLSData->fileHandle, pos)) != EAS_SUCCESS)
        return result;

        p = pSample;

        while (size)
        {
            /* read a small chunk of data and convert it */
            count = (size < SAMPLE_CONVERT_CHUNK_SIZE ? size : SAMPLE_CONVERT_CHUNK_SIZE);
            if ((result = EAS_HWReadFile(pDLSData->hwInstData, pDLSData->fileHandle, convBuf, count, &count)) != EAS_SUCCESS)
            {
                return result;
            }
            size -= count;
            if (pWsmp->bitsPerSample == 16)
            {
                memcpy(p, convBuf, count);
                p += count >> 1;
            }
            else
            {
                for(i=0; i<count; i++)
                {
                    *p++ = (short)((convBuf[i] ^ 0x80) << 8);
                }
            }

        }
    /* for looped samples, copy the last sample to the end */
    if (pWsmp->loopLength)
    {
        if( (pDLSData->wavePoolOffset + pWsmp->loopLength) >= pDLSData->wavePoolSize )
        {
            return EAS_SUCCESS;
        }
        if (sampleLen < sizeof(EAS_SAMPLE)
            || (pWsmp->loopStart + pWsmp->loopLength) * sizeof(EAS_SAMPLE) > sampleLen - sizeof(EAS_SAMPLE)) {
            return EAS_FAILURE;
        }

        pSample[(pWsmp->loopStart + pWsmp->loopLength)>>1] = pSample[(pWsmp->loopStart)>>1];
    }

    return EAS_SUCCESS;
}
#else
#error "Must specifiy _8_BIT_SAMPLES or _16_BIT_SAMPLES"
#endif

/*----------------------------------------------------------------------------
 * Parse_lins ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT Parse_lins (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_I32 size)
{
    EAS_RESULT result;
    EAS_U32 temp;
    EAS_I32 endChunk;
    EAS_I32 chunkPos;

    /* seek to start of chunk */
    if ((result = EAS_HWFileSeek(pDLSData->hwInstData, pDLSData->fileHandle, pos)) != EAS_SUCCESS)
        return result;

    /* read to end of chunk */
    endChunk = pos + size;
    while (pos < endChunk)
    {
        chunkPos = pos;

        /* get the next chunk type */
        if ((result = NextChunk(pDLSData, &pos, &temp, &size)) != EAS_SUCCESS)
            return result;

        /* only instrument chunks are useful */
        if (temp != CHUNK_INS)
            continue;

        /* if second pass, ensure instCount is less than numDLSPrograms */
        if (pDLSData->pDLS)
        {
            if (pDLSData->instCount >= pDLSData->pDLS->numDLSPrograms)
            {
                return EAS_ERROR_DATA_INCONSISTENCY;
            }
        }

        if ((result = Parse_ins(pDLSData, chunkPos + 12, size)) != EAS_SUCCESS)
            return result;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * Parse_ins ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT Parse_ins (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_I32 size)
{
    EAS_RESULT result;
    EAS_U32 temp;
    EAS_I32 chunkPos;
    EAS_I32 endChunk;
    EAS_I32 lrgnPos;
    EAS_I32 lrgnSize;
    EAS_I32 lartPos;
    EAS_I32 lartSize;
    EAS_I32 lar2Pos;
    EAS_I32 lar2Size;
    EAS_I32 inshPos;
    EAS_U32 regionCount;
    EAS_U32 locale;
    S_DLS_ART_VALUES art;
    S_PROGRAM *pProgram;
    EAS_U16 artIndex;

    /* seek to start of chunk */
    if ((result = EAS_HWFileSeek(pDLSData->hwInstData, pDLSData->fileHandle, pos)) != EAS_SUCCESS)
        return result;

    /* no chunks yet */
    lrgnPos = lrgnSize = lartPos = lartSize = lar2Pos = lar2Size = inshPos = artIndex = 0;

    /* read to end of chunk */
    endChunk = pos + size;
    while (pos < endChunk)
    {
        chunkPos = pos;

        /* get the next chunk type */
        if ((result = NextChunk(pDLSData, &pos, &temp, &size)) != EAS_SUCCESS)
            return result;

        /* parse useful chunks */
        switch (temp)
        {
            case CHUNK_INSH:
                inshPos = chunkPos + 8;
                break;

            case CHUNK_LART:
                lartPos = chunkPos + 12;
                lartSize = size;
                break;

            case CHUNK_LAR2:
                lar2Pos = chunkPos + 12;
                lar2Size = size;
                break;

            case CHUNK_LRGN:
                lrgnPos = chunkPos + 12;
                lrgnSize = size;
                break;

            default:
                break;
        }
    }

    /* must have an lrgn to be useful */
    if (!lrgnPos)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "DLS ins chunk has no lrgn chunk\n"); */ }
        return EAS_ERROR_UNRECOGNIZED_FORMAT;
    }

    /* must have an insh to be useful */
    if (!inshPos)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "DLS ins chunk has no insh chunk\n"); */ }
        return EAS_ERROR_UNRECOGNIZED_FORMAT;
    }

    /* parse the instrument header */
    if ((result = Parse_insh(pDLSData, inshPos, &regionCount, &locale)) != EAS_SUCCESS)
        return result;

    /* initialize and parse the global data first */
    EAS_HWMemCpy(&art, &defaultArt, sizeof(S_DLS_ART_VALUES));
    if (lartPos)
        if ((result = Parse_lart(pDLSData, lartPos, lartSize, &art)) != EAS_SUCCESS)
            return result;
    if (lar2Pos)
        if ((result = Parse_lart(pDLSData, lar2Pos, lar2Size, &art)) != EAS_SUCCESS)
            return result;

    if (art.values[PARAM_MODIFIED])
    {
        artIndex = (EAS_U16) pDLSData->artCount;
        pDLSData->artCount++;
    }

    /* convert data on second pass */
    if (pDLSData->pDLS)
    {

        if (art.values[PARAM_MODIFIED])
            Convert_art(pDLSData, &art, artIndex);

        /* setup pointers */
        pProgram = &pDLSData->pDLS->pDLSPrograms[pDLSData->instCount];

        /* initialize instrument */
        pProgram->locale = locale;
        pProgram->regionIndex = (EAS_U16) pDLSData->regionCount | FLAG_RGN_IDX_DLS_SYNTH;

    }

    /* parse the region data */
    if ((result = Parse_lrgn(pDLSData, lrgnPos, lrgnSize, artIndex, regionCount)) != EAS_SUCCESS)
        return result;

    /* bump instrument count */
    pDLSData->instCount++;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * Parse_insh ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT Parse_insh (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_U32 *pRgnCount, EAS_U32 *pLocale)
{
    EAS_RESULT result;
    EAS_U32 bank;
    EAS_U32 program;

    /* seek to start of chunk */
    if ((result = EAS_HWFileSeek(pDLSData->hwInstData, pDLSData->fileHandle, pos)) != EAS_SUCCESS)
        return result;

    /* get the region count and locale */
    if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, pRgnCount, EAS_FALSE)) != EAS_SUCCESS)
        return result;
    if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &bank, EAS_FALSE)) != EAS_SUCCESS)
        return result;
    if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &program, EAS_FALSE)) != EAS_SUCCESS)
        return result;

    /* verify the parameters are valid */
    if (bank & 0x7fff8080)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "DLS bank number is out of range: %08lx\n", bank); */ }
        bank &= 0xff7f;
    }
    if (program > 127)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "DLS program number is out of range: %08lx\n", program); */ }
        program &= 0x7f;
    }

    /* save the program number */
    *pLocale = (bank << 8) | program;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * Parse_lrgn ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT Parse_lrgn (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_I32 size, EAS_U16 artIndex, EAS_U32 numRegions)
{
    EAS_RESULT result;
    EAS_U32 temp;
    EAS_I32 chunkPos;
    EAS_I32 endChunk;
    EAS_U16 regionCount;

    /* seek to start of chunk */
    if ((result = EAS_HWFileSeek(pDLSData->hwInstData, pDLSData->fileHandle, pos)) != EAS_SUCCESS)
        return result;

    /* read to end of chunk */
    regionCount = 0;
    endChunk = pos + size;
    while (pos < endChunk)
    {
        chunkPos = pos;

        /* get the next chunk type */
        if ((result = NextChunk(pDLSData, &pos, &temp, &size)) != EAS_SUCCESS)
            return result;

        if ((temp == CHUNK_RGN) || (temp == CHUNK_RGN2))
        {
            if (regionCount == numRegions)
            {
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "DLS region count exceeded cRegions value in insh, extra region ignored\n"); */ }
                return EAS_SUCCESS;
            }
            /* if second pass, ensure regionCount is less than numDLSRegions */
            if (pDLSData->pDLS)
            {
                if (pDLSData->regionCount >= pDLSData->pDLS->numDLSRegions)
                {
                    return EAS_ERROR_DATA_INCONSISTENCY;
                }
            }
            if ((result = Parse_rgn(pDLSData, chunkPos + 12, size, artIndex)) != EAS_SUCCESS)
                return result;
            regionCount++;
        }
    }

    /* set a flag in the last region */
    if ((pDLSData->pDLS != NULL) && (regionCount > 0))
        pDLSData->pDLS->pDLSRegions[pDLSData->regionCount - 1].wtRegion.region.keyGroupAndFlags |= REGION_FLAG_LAST_REGION;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * Parse_rgn ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT Parse_rgn (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_I32 size, EAS_U16 artIndex)
{
    EAS_RESULT result;
    EAS_U32 temp;
    EAS_I32 chunkPos;
    EAS_I32 endChunk;
    EAS_I32 rgnhPos;
    EAS_I32 lartPos;
    EAS_I32 lartSize;
    EAS_I32 lar2Pos;
    EAS_I32 lar2Size;
    EAS_I32 wlnkPos;
    EAS_I32 wsmpPos;
    EAS_U32 waveIndex;
    S_DLS_ART_VALUES art;
    S_WSMP_DATA wsmp;
    S_WSMP_DATA *pWsmp;
    EAS_U16 regionIndex;

    /* seek to start of chunk */
    if ((result = EAS_HWFileSeek(pDLSData->hwInstData, pDLSData->fileHandle, pos)) != EAS_SUCCESS)
        return result;

    /* no chunks found yet */
    rgnhPos = lartPos = lartSize = lar2Pos = lar2Size = wsmpPos = wlnkPos = 0;
    regionIndex = (EAS_U16) pDLSData->regionCount;

    /* read to end of chunk */
    endChunk = pos + size;
    while (pos < endChunk)
    {
        chunkPos = pos;

        /* get the next chunk type */
        if ((result = NextChunk(pDLSData, &pos, &temp, &size)) != EAS_SUCCESS)
            return result;

        /* parse useful chunks */
        switch (temp)
        {
            case CHUNK_CDL:
                if ((result = Parse_cdl(pDLSData, size, &temp)) != EAS_SUCCESS)
                    return result;

                /* if conditional chunk evaluates false, skip this list */
                if (!temp)
                    return EAS_SUCCESS;
                break;

            case CHUNK_RGNH:
                rgnhPos = chunkPos + 8;
                break;

            case CHUNK_WLNK:
                wlnkPos = chunkPos + 8;
                break;

            case CHUNK_WSMP:
                wsmpPos = chunkPos + 8;
                break;

            case CHUNK_LART:
                lartPos = chunkPos + 12;
                lartSize = size;
                break;

            case CHUNK_LAR2:
                lar2Pos = chunkPos + 12;
                lar2Size = size;
                break;

            default:
                break;
        }
    }

    /* must have a rgnh chunk to be useful */
    if (!rgnhPos)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "DLS rgn chunk has no rgnh chunk\n"); */ }
        return EAS_ERROR_UNRECOGNIZED_FORMAT;
    }

    /* must have a wlnk chunk to be useful */
    if (!wlnkPos)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "DLS rgn chunk has no wlnk chunk\n"); */ }
        return EAS_ERROR_UNRECOGNIZED_FORMAT;
    }

    /* parse wlnk chunk */
    if ((result = Parse_wlnk(pDLSData, wlnkPos, &waveIndex)) != EAS_SUCCESS)
        return result;
    if (waveIndex >= pDLSData->waveCount)
    {
        return EAS_FAILURE;
    }
    pWsmp = &pDLSData->wsmpData[waveIndex];

    /* if there is any articulation data, parse it */
    EAS_HWMemCpy(&art, &defaultArt, sizeof(S_DLS_ART_VALUES));
    if (lartPos)
    {
        if ((result = Parse_lart(pDLSData, lartPos, lartSize, &art)) != EAS_SUCCESS)
            return result;
    }

    if (lar2Pos)
    {
        if ((result = Parse_lart(pDLSData, lar2Pos, lar2Size, &art)) != EAS_SUCCESS)
            return result;
    }

    /* if second pass, process region header */
    if (pDLSData->pDLS)
    {

        /* if local data was found convert it */
        if (art.values[PARAM_MODIFIED] == EAS_TRUE)
        {
            /* ensure artCount is less than numDLSArticulations */
            if (pDLSData->artCount >= pDLSData->pDLS->numDLSArticulations)
            {
                return EAS_ERROR_DATA_INCONSISTENCY;
            }

            Convert_art(pDLSData, &art, (EAS_U16) pDLSData->artCount);
            artIndex = (EAS_U16) pDLSData->artCount;
        }

        /* parse region header */
        if ((result = Parse_rgnh(pDLSData, rgnhPos, &pDLSData->pDLS->pDLSRegions[regionIndex & REGION_INDEX_MASK])) != EAS_SUCCESS)
            return result;

        /* parse wsmp chunk, copying parameters from original first */
        if (wsmpPos)
        {
            EAS_HWMemCpy(&wsmp, pWsmp, sizeof(wsmp));
            if ((result = Parse_wsmp(pDLSData, wsmpPos, &wsmp)) != EAS_SUCCESS)
                return result;

            pWsmp = &wsmp;
        }

        Convert_rgn(pDLSData, regionIndex, artIndex, (EAS_U16) waveIndex, pWsmp);

        /* ensure loopStart and loopEnd fall in the range */
        if (pWsmp->loopLength != 0)
        {
            EAS_U32 sampleLen = pDLSData->pDLS->pDLSSampleLen[waveIndex];
            if (sampleLen < sizeof(EAS_SAMPLE)
                || (pWsmp->loopStart + pWsmp->loopLength) * sizeof(EAS_SAMPLE) > sampleLen - sizeof(EAS_SAMPLE))
            {
                return EAS_FAILURE;
            }
        }
    }

    /* if local articulation, bump count */
    if (art.values[PARAM_MODIFIED])
        pDLSData->artCount++;

    /* increment region count */
    pDLSData->regionCount++;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * Parse_rgnh ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT Parse_rgnh (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, S_DLS_REGION *pRgn)
{
    EAS_RESULT result;
    EAS_U16 lowKey;
    EAS_U16 highKey;
    EAS_U16 lowVel;
    EAS_U16 highVel;
    EAS_U16 optionFlags;
    EAS_U16 keyGroup;

    /* seek to start of chunk */
    if ((result = EAS_HWFileSeek(pDLSData->hwInstData, pDLSData->fileHandle, pos)) != EAS_SUCCESS)
        return result;

    /* get the key range */
    if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &lowKey, EAS_FALSE)) != EAS_SUCCESS)
        return result;
    if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &highKey, EAS_FALSE)) != EAS_SUCCESS)
        return result;

    /* check the range */
    if (lowKey > 127)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "DLS rgnh: Low key out of range [%u]\n", lowKey); */ }
        lowKey = 127;
    }
    if (highKey > 127)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "DLS rgnh: High key out of range [%u]\n", lowKey); */ }
        highKey = 127;
    }

    /* get the velocity range */
    if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &lowVel, EAS_FALSE)) != EAS_SUCCESS)
        return result;
    if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &highVel, EAS_FALSE)) != EAS_SUCCESS)
        return result;

    /* check the range */
    if (lowVel > 127)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "DLS rgnh: Low velocity out of range [%u]\n", lowVel); */ }
        lowVel = 127;
    }
    if (highVel > 127)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "DLS rgnh: High velocity out of range [%u]\n", highVel); */ }
        highVel = 127;
    }

    /* get the option flags */
    if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &optionFlags, EAS_FALSE)) != EAS_SUCCESS)
        return result;

    /* get the key group */
    if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &keyGroup, EAS_FALSE)) != EAS_SUCCESS)
        return result;

    /* save the key range and key group */
    pRgn->wtRegion.region.rangeLow = (EAS_U8) lowKey;
    pRgn->wtRegion.region.rangeHigh = (EAS_U8) highKey;

    /*lint -e{734} keyGroup will always be from 0-15 */
    pRgn->wtRegion.region.keyGroupAndFlags = keyGroup << 8;
    pRgn->velLow = (EAS_U8) lowVel;
    pRgn->velHigh = (EAS_U8) highVel;
    if (optionFlags & F_RGN_OPTION_SELFNONEXCLUSIVE)
        pRgn->wtRegion.region.keyGroupAndFlags |= REGION_FLAG_NON_SELF_EXCLUSIVE;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * Parse_lart ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT Parse_lart (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_I32 size, S_DLS_ART_VALUES *pArt)
{
    EAS_RESULT result;
    EAS_U32 temp;
    EAS_I32 endChunk;
    EAS_I32 chunkPos;
    EAS_I32 art1Pos;
    EAS_I32 art2Pos;

    /* seek to start of chunk */
    if ((result = EAS_HWFileSeek(pDLSData->hwInstData, pDLSData->fileHandle, pos)) != EAS_SUCCESS)
        return result;

    /* no articulation chunks yet */
    art1Pos = art2Pos = 0;

    /* read to end of chunk */
    endChunk = pos + size;
    while (pos < endChunk)
    {
        chunkPos = pos;

        /* get the next chunk type */
        if ((result = NextChunk(pDLSData, &pos, &temp, &size)) != EAS_SUCCESS)
            return result;

        /* parse useful chunks */
        switch (temp)
        {
            case CHUNK_CDL:
                if ((result = Parse_cdl(pDLSData, size, &temp)) != EAS_SUCCESS)
                    return result;

                /* if conditional chunk evaluates false, skip this list */
                if (!temp)
                    return EAS_SUCCESS;
                break;

            case CHUNK_ART1:
                art1Pos = chunkPos + 8;
                break;

            case CHUNK_ART2:
                art2Pos = chunkPos + 8;
                break;

            default:
                break;

        }
    }

    if (art1Pos)
    {
        if ((result = Parse_art(pDLSData, art1Pos, pArt)) != EAS_SUCCESS)
            return result;
    }

    if (art2Pos)
    {
        if ((result = Parse_art(pDLSData, art2Pos, pArt)) != EAS_SUCCESS)
            return result;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * Parse_art()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT Parse_art (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, S_DLS_ART_VALUES *pArt)
{
    EAS_RESULT result;
    EAS_U32 structSize;
    EAS_U32 numConnections;
    EAS_U16 source;
    EAS_U16 control;
    EAS_U16 destination;
    EAS_U16 transform;
    EAS_I32 scale;
    EAS_INT i;

    /* seek to start of data */
    if ((result = EAS_HWFileSeek(pDLSData->hwInstData, pDLSData->fileHandle, pos)) != EAS_SUCCESS)
        return result;

    /* get the structure size */
    if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &structSize, EAS_FALSE)) != EAS_SUCCESS)
        return result;
    pos += (EAS_I32) structSize;

    /* get the number of connections */
    if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &numConnections, EAS_FALSE)) != EAS_SUCCESS)
        return result;

    /* skip to start of connections */
    if ((result = EAS_HWFileSeek(pDLSData->hwInstData, pDLSData->fileHandle, pos)) != EAS_SUCCESS)
        return result;

    while (numConnections)
    {
        numConnections--;
        /* read the connection data */
        if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &source, EAS_FALSE)) != EAS_SUCCESS)
            return result;
        if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &control, EAS_FALSE)) != EAS_SUCCESS)
            return result;
        if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &destination, EAS_FALSE)) != EAS_SUCCESS)
            return result;
        if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &transform, EAS_FALSE)) != EAS_SUCCESS)
            return result;
        if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &scale, EAS_FALSE)) != EAS_SUCCESS)
            return result;

        /* look up the connection */
        for (i = 0; i < (EAS_INT) ENTRIES_IN_CONN_TABLE; i++)
        {
            if ((connTable[i].source == source) &&
                (connTable[i].destination == destination) &&
                (connTable[i].control == control))
            {
                /*lint -e{704} use shift for performance */
                pArt->values[connTable[i].connection] = (EAS_I16) (scale >> 16);
                pArt->values[PARAM_MODIFIED] = EAS_TRUE;
                break;
            }
        }
        if (i == PARAM_TABLE_SIZE)
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "WARN: Unsupported parameter in DLS file\n"); */ }
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * Parse_wlnk ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT Parse_wlnk (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 pos, EAS_U32 *pWaveIndex)
{
    EAS_RESULT result;

    /* we only care about the the index */
    if ((result = EAS_HWFileSeek(pDLSData->hwInstData, pDLSData->fileHandle, pos + 8)) != EAS_SUCCESS)
        return result;

    /* read the index */
    return EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle,pWaveIndex, EAS_FALSE);
}

/*----------------------------------------------------------------------------
 * PopcdlStack ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT PopcdlStack (EAS_U32 *pStack, EAS_INT *pStackPtr, EAS_U32 *pValue)
{

    /* stack underflow, cdl block has an errorr */
    if (*pStackPtr < 0)
        return EAS_ERROR_FILE_FORMAT;

    /* pop the value off the stack */
    *pValue = pStack[*pStackPtr];
    *pStackPtr = *pStackPtr - 1;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * PushcdlStack ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT PushcdlStack (EAS_U32 *pStack, EAS_INT *pStackPtr, EAS_U32 value)
{

    /* stack overflow, return an error */
    if (*pStackPtr >= (CDL_STACK_SIZE - 1)) {
        ALOGE("b/34031018, stackPtr(%d)", *pStackPtr);
        android_errorWriteLog(0x534e4554, "34031018");
        return EAS_ERROR_FILE_FORMAT;
    }

    /* push the value onto the stack */
    *pStackPtr = *pStackPtr + 1;
    pStack[*pStackPtr] = value;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * QueryGUID ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_BOOL QueryGUID (const DLSID *pGUID, EAS_U32 *pValue)
{

    /* assume false */
    *pValue = 0;
    if (EAS_HWMemCmp(&DLSID_GMInHardware, pGUID, sizeof(DLSID)) == 0)
    {
        *pValue = 0xffffffff;
        return EAS_TRUE;
    }

    if (EAS_HWMemCmp(&DLSID_GSInHardware, pGUID, sizeof(DLSID)) == 0)
        return EAS_TRUE;

    if (EAS_HWMemCmp(&DLSID_XGInHardware, pGUID, sizeof(DLSID)) == 0)
        return EAS_TRUE;

    if (EAS_HWMemCmp(&DLSID_SupportsDLS1, pGUID, sizeof(DLSID)) == 0)
    {
        *pValue = 0xffffffff;
        return EAS_TRUE;
    }

    if (EAS_HWMemCmp(&DLSID_SupportsDLS2, pGUID, sizeof(DLSID)) == 0)
        return EAS_TRUE;

    if (EAS_HWMemCmp(&DLSID_SampleMemorySize, pGUID, sizeof(DLSID)) == 0)
    {
        *pValue = MAX_DLS_MEMORY;
        return EAS_TRUE;
    }

    if (EAS_HWMemCmp(&DLSID_ManufacturersID, pGUID, sizeof(DLSID)) == 0)
    {
        *pValue = 0x0000013A;
        return EAS_TRUE;
    }

    if (EAS_HWMemCmp(&DLSID_ProductID, pGUID, sizeof(DLSID)) == 0)
    {
        *pValue = LIB_VERSION;
        return EAS_TRUE;
    }

    if (EAS_HWMemCmp(&DLSID_SamplePlaybackRate, pGUID, sizeof(DLSID)) == 0)
    {
        *pValue = (EAS_U32) outputSampleRate;
        return EAS_TRUE;
    }

    /* unrecognized DLSID */
    return EAS_FALSE;
}

/*----------------------------------------------------------------------------
 * ReadDLSID ()
 *----------------------------------------------------------------------------
 * Purpose:
 * Reads a DLSID in a manner that is not sensitive to processor endian-ness
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ReadDLSID (SDLS_SYNTHESIZER_DATA *pDLSData, DLSID *pDLSID)
{
    EAS_RESULT result;
    EAS_I32 n;

    if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &pDLSID->Data1, EAS_FALSE)) != EAS_SUCCESS)
        return result;
    if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &pDLSID->Data2, EAS_FALSE)) != EAS_SUCCESS)
        return result;
    if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &pDLSID->Data3, EAS_FALSE)) != EAS_SUCCESS)
        return result;
    return EAS_HWReadFile(pDLSData->hwInstData, pDLSData->fileHandle, pDLSID->Data4, sizeof(pDLSID->Data4), &n);
}

/*----------------------------------------------------------------------------
 * Parse_cdl ()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT Parse_cdl (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_I32 size, EAS_U32 *pValue)
{
    EAS_RESULT result;
    EAS_U32 stack[CDL_STACK_SIZE];
    EAS_U16 opcode;
    EAS_INT stackPtr;
    EAS_U32 x, y;
    DLSID dlsid;

    stackPtr = -1;
    *pValue = 0;
    x = 0;
    while (size)
    {
        /* read the opcode */
        if ((result = EAS_HWGetWord(pDLSData->hwInstData, pDLSData->fileHandle, &opcode, EAS_FALSE)) != EAS_SUCCESS)
            return result;

        /* handle binary opcodes */
        if (opcode <= DLS_CDL_EQ)
        {
            /* pop X and Y */
            if ((result = PopcdlStack(stack, &stackPtr, &x)) != EAS_SUCCESS)
                return result;
            if ((result = PopcdlStack(stack, &stackPtr, &y)) != EAS_SUCCESS)
                return result;
            switch (opcode)
            {
                case DLS_CDL_AND:
                    x = x & y;
                    break;
                case DLS_CDL_OR:
                    x = x | y;
                    break;
                case DLS_CDL_XOR:
                    x = x ^ y;
                    break;
                case DLS_CDL_ADD:
                    x = x + y;
                    break;
                case DLS_CDL_SUBTRACT:
                    x = x - y;
                    break;
                case DLS_CDL_MULTIPLY:
                    x = x * y;
                    break;
                case DLS_CDL_DIVIDE:
                    if (!y)
                        return EAS_ERROR_FILE_FORMAT;
                    x = x / y;
                    break;
                case DLS_CDL_LOGICAL_AND:
                    x = (x && y);
                    break;
                case DLS_CDL_LOGICAL_OR:
                    x = (x || y);
                    break;
                case DLS_CDL_LT:
                    x = (x < y);
                    break;
                case DLS_CDL_LE:
                    x = (x <= y);
                    break;
                case DLS_CDL_GT:
                    x = (x > y);
                    break;
                case DLS_CDL_GE:
                    x = (x >= y);
                    break;
                case DLS_CDL_EQ:
                    x = (x == y);
                    break;
                default:
                    break;
            }
        }

        else if (opcode == DLS_CDL_NOT)
        {
            if ((result = PopcdlStack(stack, &stackPtr, &x)) != EAS_SUCCESS)
                return result;
            x = !x;
        }

        else if (opcode == DLS_CDL_CONST)
        {
            if ((result = EAS_HWGetDWord(pDLSData->hwInstData, pDLSData->fileHandle, &x, EAS_FALSE)) != EAS_SUCCESS)
                return result;
        }

        else if (opcode == DLS_CDL_QUERY)
        {
            if ((result = ReadDLSID(pDLSData, &dlsid)) != EAS_SUCCESS)
                return result;
            QueryGUID(&dlsid, &x);
        }

        else if (opcode == DLS_CDL_QUERYSUPPORTED)
        {
            if ((result = ReadDLSID(pDLSData, &dlsid)) != EAS_SUCCESS)
                return result;
            x = QueryGUID(&dlsid, &y);
        }
        else
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Unsupported opcode %d in DLS file\n", opcode); */ }

        /* push the result on the stack */
        if ((result = PushcdlStack(stack, &stackPtr, x)) != EAS_SUCCESS)
            return result;
    }

    /* pop the last result off the stack */
    return PopcdlStack(stack, &stackPtr, pValue);
}

/*----------------------------------------------------------------------------
 * Convert_rgn()
 *----------------------------------------------------------------------------
 * Purpose:
 * Convert region data from DLS to EAS
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static void Convert_rgn (SDLS_SYNTHESIZER_DATA *pDLSData, EAS_U16 regionIndex, EAS_U16 artIndex, EAS_U16 waveIndex, S_WSMP_DATA *pWsmp)
{
    S_DLS_REGION *pRgn;

    /* setup pointers to data structures */
    pRgn = &pDLSData->pDLS->pDLSRegions[regionIndex];

    /* intiailize indices */
    pRgn->wtRegion.artIndex = artIndex;
    pRgn->wtRegion.waveIndex = waveIndex;

    /* convert region data */
    /*lint -e{704} use shift for performance */
    pRgn->wtRegion.gain = (EAS_I16) (pWsmp->gain >> 16);
    pRgn->wtRegion.loopStart = pWsmp->loopStart;
    pRgn->wtRegion.loopEnd = (pWsmp->loopStart + pWsmp->loopLength);
    pRgn->wtRegion.tuning = pWsmp->fineTune -(pWsmp->unityNote * 100) + ConvertSampleRate(pWsmp->sampleRate);
    if (pWsmp->loopLength != 0)
        pRgn->wtRegion.region.keyGroupAndFlags |= REGION_FLAG_IS_LOOPED;
}

/*----------------------------------------------------------------------------
 * Convert_art()
 *----------------------------------------------------------------------------
 * Purpose:
 * Convert articulation data from DLS to EAS
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 *----------------------------------------------------------------------------
*/
static void Convert_art (SDLS_SYNTHESIZER_DATA *pDLSData, const S_DLS_ART_VALUES *pDLSArt,  EAS_U16 artIndex)
{
    S_DLS_ARTICULATION *pArt;

    /* setup pointers to data structures */
    pArt = &pDLSData->pDLS->pDLSArticulations[artIndex];

    /* LFO parameters */
    pArt->modLFO.lfoFreq = ConvertLFOPhaseIncrement(pDLSArt->values[PARAM_MOD_LFO_FREQ]);
    pArt->modLFO.lfoDelay = -ConvertDelay(pDLSArt->values[PARAM_MOD_LFO_DELAY]);
    pArt->vibLFO.lfoFreq = ConvertLFOPhaseIncrement(pDLSArt->values[PARAM_VIB_LFO_FREQ]);
    pArt->vibLFO.lfoDelay = -ConvertDelay(pDLSArt->values[PARAM_VIB_LFO_DELAY]);

    /* EG1 parameters */
    pArt->eg1.delayTime = ConvertDelay(pDLSArt->values[PARAM_VOL_EG_DELAY]);
    pArt->eg1.attackTime = pDLSArt->values[PARAM_VOL_EG_ATTACK];
    pArt->eg1.holdTime = pDLSArt->values[PARAM_VOL_EG_HOLD];
    pArt->eg1.decayTime = pDLSArt->values[PARAM_VOL_EG_DECAY];
    pArt->eg1.sustainLevel = ConvertSustain(pDLSArt->values[PARAM_VOL_EG_SUSTAIN]);
    pArt->eg1.releaseTime = ConvertRate(pDLSArt->values[PARAM_VOL_EG_RELEASE]);
    pArt->eg1.velToAttack = pDLSArt->values[PARAM_VOL_EG_VEL_TO_ATTACK];
    pArt->eg1.keyNumToDecay = pDLSArt->values[PARAM_VOL_EG_KEY_TO_DECAY];
    pArt->eg1.keyNumToHold = pDLSArt->values[PARAM_VOL_EG_KEY_TO_HOLD];
    pArt->eg1ShutdownTime = ConvertRate(pDLSArt->values[PARAM_VOL_EG_SHUTDOWN]);

    /* EG2 parameters */
    pArt->eg2.delayTime = ConvertDelay(pDLSArt->values[PARAM_MOD_EG_DELAY]);
    pArt->eg2.attackTime = pDLSArt->values[PARAM_MOD_EG_ATTACK];
    pArt->eg2.holdTime = pDLSArt->values[PARAM_MOD_EG_HOLD];
    pArt->eg2.decayTime = pDLSArt->values[PARAM_MOD_EG_DECAY];
    pArt->eg2.sustainLevel = ConvertSustain(pDLSArt->values[PARAM_MOD_EG_SUSTAIN]);
    pArt->eg2.releaseTime = ConvertRate(pDLSArt->values[PARAM_MOD_EG_RELEASE]);
    pArt->eg2.velToAttack = pDLSArt->values[PARAM_MOD_EG_VEL_TO_ATTACK];
    pArt->eg2.keyNumToDecay = pDLSArt->values[PARAM_MOD_EG_KEY_TO_DECAY];
    pArt->eg2.keyNumToHold = pDLSArt->values[PARAM_MOD_EG_KEY_TO_HOLD];

    /* filter parameters */
    pArt->filterCutoff = pDLSArt->values[PARAM_INITIAL_FC];
    pArt->filterQandFlags = ConvertQ(pDLSArt->values[PARAM_INITIAL_Q]);
    pArt->modLFOToFc = pDLSArt->values[PARAM_MOD_LFO_TO_FC];
    pArt->modLFOCC1ToFc = pDLSArt->values[PARAM_MOD_LFO_CC1_TO_FC];
    pArt->modLFOChanPressToFc = pDLSArt->values[PARAM_MOD_LFO_CHAN_PRESS_TO_FC];
    pArt->eg2ToFc = pDLSArt->values[PARAM_MOD_EG_TO_FC];
    pArt->velToFc = pDLSArt->values[PARAM_VEL_TO_FC];
    pArt->keyNumToFc = pDLSArt->values[PARAM_KEYNUM_TO_FC];

    /* gain parameters */
    pArt->modLFOToGain = pDLSArt->values[PARAM_MOD_LFO_TO_GAIN];
    pArt->modLFOCC1ToGain = pDLSArt->values[PARAM_MOD_LFO_CC1_TO_GAIN];
    pArt->modLFOChanPressToGain = pDLSArt->values[PARAM_MOD_LFO_CHAN_PRESS_TO_GAIN];

    /* pitch parameters */
    pArt->tuning = pDLSArt->values[PARAM_TUNING];
    pArt->keyNumToPitch = pDLSArt->values[PARAM_KEYNUM_TO_PITCH];
    pArt->vibLFOToPitch = pDLSArt->values[PARAM_VIB_LFO_TO_PITCH];
    pArt->vibLFOCC1ToPitch = pDLSArt->values[PARAM_VIB_LFO_CC1_TO_PITCH];
    pArt->vibLFOChanPressToPitch = pDLSArt->values[PARAM_VIB_LFO_CHAN_PRESS_TO_PITCH];
    pArt->modLFOToPitch = pDLSArt->values[PARAM_MOD_LFO_TO_PITCH];
    pArt->modLFOCC1ToPitch = pDLSArt->values[PARAM_MOD_LFO_CC1_TO_PITCH];
    pArt->modLFOChanPressToPitch = pDLSArt->values[PARAM_MOD_LFO_CHAN_PRESS_TO_PITCH];
    pArt->eg2ToPitch = pDLSArt->values[PARAM_MOD_EG_TO_PITCH];

    /* output parameters */
    pArt->pan = ConvertPan(pDLSArt->values[PARAM_DEFAULT_PAN]);

    if (pDLSArt->values[PARAM_VEL_TO_GAIN] != 0)
        pArt->filterQandFlags |= FLAG_DLS_VELOCITY_SENSITIVE;

#ifdef _REVERB
    pArt->reverbSend = pDLSArt->values[PARAM_DEFAULT_REVERB_SEND];
    pArt->cc91ToReverbSend = pDLSArt->values[PARAM_MIDI_CC91_TO_REVERB_SEND];
#endif

#ifdef _CHORUS
    pArt->chorusSend = pDLSArt->values[PARAM_DEFAULT_CHORUS_SEND];
    pArt->cc93ToChorusSend = pDLSArt->values[PARAM_MIDI_CC93_TO_CHORUS_SEND];
#endif
}

/*----------------------------------------------------------------------------
 * ConvertSampleRate()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 *
 * Outputs:
 *
 * Side Effects:
 *----------------------------------------------------------------------------
*/
static EAS_I16 ConvertSampleRate (EAS_U32 sampleRate)
{
    return (EAS_I16) (1200.0 * log10((double) sampleRate / (double) outputSampleRate) / log10(2.0));
}

/*----------------------------------------------------------------------------
 * ConvertSustainEG2()
 *----------------------------------------------------------------------------
 * Convert sustain level to pitch/Fc multipler for EG2
 *----------------------------------------------------------------------------
*/
static EAS_I16 ConvertSustain (EAS_I32 sustain)
{
    /* check for sustain level of zero */
    if (sustain == 0)
        return 0;

    /* convert to log2 factor */
    /*lint -e{704} use shift for performance */
    sustain = (sustain * SUSTAIN_LINEAR_CONVERSION_FACTOR) >> 15;

    if (sustain > SYNTH_FULL_SCALE_EG1_GAIN)
        return SYNTH_FULL_SCALE_EG1_GAIN;
    return (EAS_I16) sustain;
}

/*----------------------------------------------------------------------------
 * ConvertDelay ()
 *----------------------------------------------------------------------------
 * Converts timecents to frame count. Used for LFO and envelope
 * delay times.
 *----------------------------------------------------------------------------
*/
EAS_I16 ConvertDelay (EAS_I32 timeCents)
{
    EAS_I32 temp;

    if (timeCents == ZERO_TIME_IN_CENTS)
        return 0;

    /* divide time by secs per frame to get number of frames */
    temp = timeCents - dlsRateConvert;

    /* convert from time cents to 10-bit fraction */
    temp = FMUL_15x15(temp, TIME_CENTS_TO_LOG2);

    /* convert to frame count */
    temp = EAS_LogToLinear16(temp - (15 << 10));

    if (temp < SYNTH_FULL_SCALE_EG1_GAIN)
        return (EAS_I16) temp;
    return SYNTH_FULL_SCALE_EG1_GAIN;
}

/*----------------------------------------------------------------------------
 * ConvertRate ()
 *----------------------------------------------------------------------------
 * Convert timecents to rate
 *----------------------------------------------------------------------------
*/
EAS_I16 ConvertRate (EAS_I32 timeCents)
{
    EAS_I32 temp;

    if (timeCents == ZERO_TIME_IN_CENTS)
        return SYNTH_FULL_SCALE_EG1_GAIN;

    /* divide frame rate by time in log domain to get rate */
    temp = dlsRateConvert - timeCents;

#if 1
    temp = EAS_Calculate2toX(temp);
#else
    /* convert from time cents to 10-bit fraction */
    temp = FMUL_15x15(temp, TIME_CENTS_TO_LOG2);

    /* convert to rate */
    temp = EAS_LogToLinear16(temp);
#endif

    if (temp < SYNTH_FULL_SCALE_EG1_GAIN)
        return (EAS_I16) temp;
    return SYNTH_FULL_SCALE_EG1_GAIN;
}


/*----------------------------------------------------------------------------
 * ConvertLFOPhaseIncrement()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 *
 * Outputs:
 *
 * Side Effects:
 *----------------------------------------------------------------------------
*/
static EAS_I16 ConvertLFOPhaseIncrement (EAS_I32 pitchCents)
{

    /* check range */
    if (pitchCents > MAX_LFO_FREQUENCY_IN_PITCHCENTS)
        pitchCents = MAX_LFO_FREQUENCY_IN_PITCHCENTS;
    if (pitchCents < MIN_LFO_FREQUENCY_IN_PITCHCENTS)
        pitchCents = MIN_LFO_FREQUENCY_IN_PITCHCENTS;

    /* double the rate and divide by frame rate by subtracting in log domain */
    pitchCents = pitchCents - dlsLFOFrequencyConvert;

    /* convert to phase increment */
    return (EAS_I16) EAS_Calculate2toX(pitchCents);
}

/*----------------------------------------------------------------------------
 * ConvertPan()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 *
 * Outputs:
 *
 * Side Effects:
 *----------------------------------------------------------------------------
*/
static EAS_I8 ConvertPan (EAS_I32 pan)
{

    /* multiply by conversion factor */
    pan = FMUL_15x15 (PAN_CONVERSION_FACTOR, pan);
    if (pan < MIN_PAN_VALUE)
        return MIN_PAN_VALUE;
    if (pan > MAX_PAN_VALUE)
        return MAX_PAN_VALUE;
    return (EAS_I8) pan;
}

/*----------------------------------------------------------------------------
 * ConvertQ()
 *----------------------------------------------------------------------------
 * Convert the DLS filter resonance to an index value used by the synth
 * that accesses tables of coefficients based on the Q.
 *----------------------------------------------------------------------------
*/
static EAS_U8 ConvertQ (EAS_I32 q)
{

    /* apply limits */
    if (q <= 0)
        return 0;

    /* convert to table index */
    /*lint -e{704} use shift for performance */
    q = (FILTER_Q_CONVERSION_FACTOR * q + 0x4000) >> 15;

    /* apply upper limit */
    if (q >= FILTER_RESONANCE_NUM_ENTRIES)
        q = FILTER_RESONANCE_NUM_ENTRIES - 1;
    return (EAS_U8) q;
}

#ifdef _DEBUG_DLS
/*----------------------------------------------------------------------------
 * DumpDLS()
 *----------------------------------------------------------------------------
*/
static void DumpDLS (S_EAS *pEAS)
{
    S_DLS_ARTICULATION *pArt;
    S_DLS_REGION *pRegion;
    EAS_INT i;
    EAS_INT j;

    EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000022 , pEAS->numPrograms);
    EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000023 , pEAS->numWTRegions);
    EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000024 , pEAS->numDLSArticulations);
    EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000025 , pEAS->numSamples);

    /* dump the instruments */
    for (i = 0; i < pEAS->numPrograms; i++)
    {
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000026 ,
                pEAS->pPrograms[i].locale >> 16,
                (pEAS->pPrograms[i].locale >> 8) & 0x7f,
                pEAS->pPrograms[i].locale & 0x7f);

        for (j = pEAS->pPrograms[i].regionIndex; ; j++)
        {
            EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000027 , j);
            pRegion = &pEAS->pWTRegions[j];
            EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000028 , pRegion->gain);
            EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000029 , pRegion->region.rangeLow, pRegion->region.rangeHigh);
            EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x0000002a , pRegion->region.keyGroupAndFlags);
            EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x0000002b , pRegion->loopStart);
            EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x0000002c , pRegion->loopEnd);
            EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x0000002d , pRegion->tuning);
            EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x0000002e , pRegion->artIndex);
            EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x0000002f , pRegion->waveIndex);

            if (pRegion->region.keyGroupAndFlags & REGION_FLAG_LAST_REGION)
                break;
        }

    }

    /* dump the articulation data */
    for (i = 0; i < pEAS->numDLSArticulations; i++)
    {
        /* articulation data */
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000030 , i);
        pArt = &pEAS->pDLSArticulations[i];
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000031 , pArt->m_nEG2toFilterDepth);
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000032 , pArt->m_nEG2toPitchDepth);
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000033 , pArt->m_nFilterCutoffFrequency);
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000034 , pArt->m_nFilterResonance);
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000035 , pArt->m_nLFOAmplitudeDepth);
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000036 , pArt->m_nLFODelayTime);
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000037 , pArt->m_nLFOFrequency);
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000038 , pArt->m_nLFOPitchDepth);
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000039 , pArt->m_nPan);

        /* EG1 data */
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x0000003a , pArt->m_sEG1.m_nAttack);
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x0000003b , pArt->m_sEG1.m_nDecay);
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x0000003c , pArt->m_sEG1.m_nSustain);
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x0000003d , pArt->m_sEG1.m_nRelease);

        /* EG2 data */
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x0000003e , pArt->m_sEG2.m_nAttack);
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x0000003f , pArt->m_sEG2.m_nDecay);
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000040 , pArt->m_sEG2.m_nSustain);
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000041 , pArt->m_sEG2.m_nRelease);

    }

    /* dump the waves */
    for (i = 0; i < pEAS->numSamples; i++)
    {
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000042 , i);
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000043 , pEAS->pSampleLen[i]);
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x19299ed4, 0x00000044 , pEAS->ppSamples[i]);
    }

}
#endif

