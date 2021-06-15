/*----------------------------------------------------------------------------
 *
 * File:
 * eas_sndlib.h
 *
 * Contents and purpose:
 * Declarations for the sound library
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
 *   $Revision: 550 $
 *   $Date: 2007-02-02 09:37:03 -0800 (Fri, 02 Feb 2007) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_SNDLIB_H
#define _EAS_SNDLIB_H

#include "eas_types.h"
#include "eas_synthcfg.h"

#ifdef _WT_SYNTH
#include "eas_wtengine.h"
#endif

/*----------------------------------------------------------------------------
 * This is bit of a hack to allow us to keep the same structure
 * declarations for the DLS parser. Normally, the data is located
 * in read-only memory, but for DLS, we store the data in RW
 * memory.
 *----------------------------------------------------------------------------
*/
#ifndef SCNST
#define SCNST const
#endif

/*----------------------------------------------------------------------------
 * sample size
 *----------------------------------------------------------------------------
*/
#ifdef _16_BIT_SAMPLES
typedef EAS_I16 EAS_SAMPLE;
#else
typedef EAS_I8 EAS_SAMPLE;
#endif

/*----------------------------------------------------------------------------
 * EAS Library ID - quick check for valid library and version
 *----------------------------------------------------------------------------
*/
#define _EAS_LIBRARY_VERSION        0x01534145

#define NUM_PROGRAMS_IN_BANK        128
#define INVALID_REGION_INDEX        0xffff

/* this bit in region index indicates that region is for secondary synth */
#define FLAG_RGN_IDX_FM_SYNTH       0x8000
#define FLAG_RGN_IDX_DLS_SYNTH      0x4000
#define REGION_INDEX_MASK           0x3fff

/*----------------------------------------------------------------------------
 * Generic region data structure
 *
 * This must be the first element in each region structure
 *----------------------------------------------------------------------------
*/
typedef struct s_region_tag
{
    EAS_U16     keyGroupAndFlags;
    EAS_U8      rangeLow;
    EAS_U8      rangeHigh;
} S_REGION;

/*
 * Bit fields for m_nKeyGroupAndFlags
 * Bits 0-2 are mode bits in FM synth
 * Bits 8-11 are the key group
 */
#define REGION_FLAG_IS_LOOPED                   0x01
#define REGION_FLAG_USE_WAVE_GENERATOR          0x02
#define REGION_FLAG_USE_ADPCM                   0x04
#define REGION_FLAG_ONE_SHOT                    0x08
#define REGION_FLAG_SQUARE_WAVE                 0x10
#define REGION_FLAG_OFF_CHIP                    0x20
#define REGION_FLAG_NON_SELF_EXCLUSIVE          0x40
#define REGION_FLAG_LAST_REGION                 0x8000

/*----------------------------------------------------------------------------
 * Envelope data structure
 *----------------------------------------------------------------------------
*/
typedef struct s_envelope_tag
{
    EAS_I16     attackTime;
    EAS_I16     decayTime;
    EAS_I16     sustainLevel;
    EAS_I16     releaseTime;
} S_ENVELOPE;

/*----------------------------------------------------------------------------
 * DLS envelope data structure
 *----------------------------------------------------------------------------
*/
typedef struct s_dls_envelope_tag
{
    EAS_I16         delayTime;
    EAS_I16         attackTime;
    EAS_I16         holdTime;
    EAS_I16         decayTime;
    EAS_I16         sustainLevel;
    EAS_I16         releaseTime;
    EAS_I16         velToAttack;
    EAS_I16         keyNumToDecay;
    EAS_I16         keyNumToHold;
} S_DLS_ENVELOPE;

/*----------------------------------------------------------------------------
 * LFO data structure
 *----------------------------------------------------------------------------
*/
typedef struct s_lfo_params_tag
{
    EAS_I16     lfoFreq;
    EAS_I16     lfoDelay;
} S_LFO_PARAMS;

/*----------------------------------------------------------------------------
 * Articulation data structure
 *----------------------------------------------------------------------------
*/
typedef struct s_articulation_tag
{
    S_ENVELOPE  eg1;
    S_ENVELOPE  eg2;
    EAS_I16     lfoToPitch;
    EAS_I16     lfoDelay;
    EAS_I16     lfoFreq;
    EAS_I16     eg2ToPitch;
    EAS_I16     eg2ToFc;
    EAS_I16     filterCutoff;
    EAS_I8      lfoToGain;
    EAS_U8      filterQ;
    EAS_I8      pan;
} S_ARTICULATION;

/*----------------------------------------------------------------------------
 * DLS articulation data structure
 *----------------------------------------------------------------------------
*/

typedef struct s_dls_articulation_tag
{
    S_LFO_PARAMS    modLFO;
    S_LFO_PARAMS    vibLFO;

    S_DLS_ENVELOPE  eg1;
    S_DLS_ENVELOPE  eg2;

    EAS_I16         eg1ShutdownTime;

    EAS_I16         filterCutoff;
    EAS_I16         modLFOToFc;
    EAS_I16         modLFOCC1ToFc;
    EAS_I16         modLFOChanPressToFc;
    EAS_I16         eg2ToFc;
    EAS_I16         velToFc;
    EAS_I16         keyNumToFc;

    EAS_I16         modLFOToGain;
    EAS_I16         modLFOCC1ToGain;
    EAS_I16         modLFOChanPressToGain;

    EAS_I16         tuning;
    EAS_I16         keyNumToPitch;
    EAS_I16         vibLFOToPitch;
    EAS_I16         vibLFOCC1ToPitch;
    EAS_I16         vibLFOChanPressToPitch;
    EAS_I16         modLFOToPitch;
    EAS_I16         modLFOCC1ToPitch;
    EAS_I16         modLFOChanPressToPitch;
    EAS_I16         eg2ToPitch;

    /* pad to 4-byte boundary */
    EAS_U16         pad;

    EAS_I8          pan;
    EAS_U8          filterQandFlags;

#ifdef _REVERB
    EAS_I16         reverbSend;
    EAS_I16         cc91ToReverbSend;
#endif

#ifdef _CHORUS
    EAS_I16         chorusSend;
    EAS_I16         cc93ToChorusSend;
#endif
} S_DLS_ARTICULATION;

/* flags in filterQandFlags
 * NOTE: Q is stored in bottom 5 bits
 */
#define FLAG_DLS_VELOCITY_SENSITIVE     0x80
#define FILTER_Q_MASK                   0x1f

/*----------------------------------------------------------------------------
 * Wavetable region data structure
 *----------------------------------------------------------------------------
*/
typedef struct s_wt_region_tag
{
    S_REGION    region;
    EAS_I16     tuning;
    EAS_I16     gain;
    EAS_U32     loopStart;
    EAS_U32     loopEnd;
    EAS_U16     waveIndex;
    EAS_U16     artIndex;
} S_WT_REGION;

/*----------------------------------------------------------------------------
 * DLS region data structure
 *----------------------------------------------------------------------------
*/
typedef struct s_dls_region_tag
{
    S_WT_REGION     wtRegion;
    EAS_U8          velLow;
    EAS_U8          velHigh;
} S_DLS_REGION;

/*----------------------------------------------------------------------------
 * FM synthesizer data structures
 *----------------------------------------------------------------------------
*/
typedef struct s_fm_oper_tag
{
    EAS_I16     tuning;
    EAS_U8      attackDecay;
    EAS_U8      velocityRelease;
    EAS_U8      egKeyScale;
    EAS_U8      sustain;
    EAS_U8      gain;
    EAS_U8      flags;
} S_FM_OPER;

/* defines for S_FM_OPER.m_nFlags */
#define FM_OPER_FLAG_MONOTONE           0x01
#define FM_OPER_FLAG_NO_VIBRATO         0x02
#define FM_OPER_FLAG_NOISE              0x04
#define FM_OPER_FLAG_LINEAR_VELOCITY    0x08

/* NOTE: The first two structure elements are common with S_WT_REGION
 * and we will rely on that in the voice management code and must
 * remain there unless the voice management code is revisited.
 */
typedef struct s_fm_region_tag
{
    S_REGION        region;
    EAS_U8          vibTrem;
    EAS_U8          lfoFreqDelay;
    EAS_U8          feedback;
    EAS_I8          pan;
    S_FM_OPER       oper[4];
} S_FM_REGION;

/*----------------------------------------------------------------------------
 * Common data structures
 *----------------------------------------------------------------------------
*/

/*----------------------------------------------------------------------------
 * Program data structure
 * Used for individual programs not stored as a complete bank.
 *----------------------------------------------------------------------------
*/
typedef struct s_program_tag
{
    EAS_U32 locale;
    EAS_U16 regionIndex;
} S_PROGRAM;

/*----------------------------------------------------------------------------
 * Bank data structure
 *
 * A bank always consists of 128 programs. If a bank is less than 128
 * programs, it should be stored as a spare matrix in the pPrograms
 * array.
 *
 * bankNum:     MSB/LSB of MIDI bank select controller
 * regionIndex: Index of first region in program
 *----------------------------------------------------------------------------
*/
typedef struct s_bank_tag
{
    EAS_U16 locale;
    EAS_U16 regionIndex[NUM_PROGRAMS_IN_BANK];
} S_BANK;


/* defines for libFormat field
 * bits 0-17 are the sample rate
 * bit 18 is true if wavetable is present
 * bit 19 is true if FM is present
 * bit 20 is true if filter is enabled
 * bit 21 is sample depth (0 = 8-bits, 1 = 16-bits)
 * bits 22-31 are reserved
 */
#define LIBFORMAT_SAMPLE_RATE_MASK      0x0003ffff
#define LIB_FORMAT_TYPE_MASK            0x000c0000
#define LIB_FORMAT_WAVETABLE            0x00000000
#define LIB_FORMAT_FM                   0x00040000
#define LIB_FORMAT_HYBRID               0x00080000
#define LIB_FORMAT_FILTER_ENABLED       0x00100000
#define LIB_FORMAT_16_BIT_SAMPLES       0x00200000

#ifdef DLS_SYNTHESIZER
/*----------------------------------------------------------------------------
 * DLS data structure
 *
 * pDLSPrograms         pointer to array of DLS programs
 * pDLSRegions          pointer to array of DLS regions
 * pDLSArticulations    pointer to array of DLS articulations
 * pSampleLen           pointer to array of sample lengths
 * ppSamples            pointer to array of sample pointers
 * numDLSPrograms       number of DLS programs
 * numDLSRegions        number of DLS regions
 * numDLSArticulations  number of DLS articulations
 * numDLSSamples        number of DLS samples
 *----------------------------------------------------------------------------
*/
typedef struct s_eas_dls_tag
{
    S_PROGRAM           *pDLSPrograms;
    S_DLS_REGION        *pDLSRegions;
    S_DLS_ARTICULATION  *pDLSArticulations;
    EAS_U32             *pDLSSampleLen;
    EAS_U32             *pDLSSampleOffsets;
    EAS_SAMPLE          *pDLSSamples;
    EAS_U16             numDLSPrograms;
    EAS_U16             numDLSRegions;
    EAS_U16             numDLSArticulations;
    EAS_U16             numDLSSamples;
    EAS_U8              refCount;
} S_DLS;
#endif

/*----------------------------------------------------------------------------
 * Sound library data structure
 *
 * pBanks           pointer to array of banks
 * pPrograms        pointer to array of programs
 * pWTRegions       pointer to array of wavetable regions
 * pFMRegions       pointer to array of FM regions
 * pArticulations   pointer to array of articulations
 * pSampleLen       pointer to array of sample lengths
 * ppSamples        pointer to array of sample pointers
 * numBanks         number of banks
 * numPrograms      number of individual program
 * numRegions       number of regions
 * numArticulations number of articulations
 * numSamples       number of samples
 *----------------------------------------------------------------------------
*/
typedef struct s_eas_sndlib_tag
{
    SCNST EAS_U32               identifier;
    SCNST EAS_U32               libAttr;

    SCNST S_BANK                *pBanks;
    SCNST S_PROGRAM             *pPrograms;

    SCNST S_WT_REGION           *pWTRegions;
    SCNST S_ARTICULATION        *pArticulations;
    SCNST EAS_U32               *pSampleLen;
    SCNST EAS_U32               *pSampleOffsets;
    SCNST EAS_SAMPLE            *pSamples;

    SCNST S_FM_REGION           *pFMRegions;

    SCNST EAS_U16               numBanks;
    SCNST EAS_U16               numPrograms;

    SCNST EAS_U16               numWTRegions;
    SCNST EAS_U16               numArticulations;
    SCNST EAS_U16               numSamples;

    SCNST EAS_U16               numFMRegions;
} S_EAS;

#endif

