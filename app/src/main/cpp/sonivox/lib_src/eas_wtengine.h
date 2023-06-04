/*----------------------------------------------------------------------------
 *
 * File:
 * eas_wtengine.h
 *
 * Contents and purpose:
 * This file defines the interface for wavetable synthesis engine
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
 *   $Revision: 818 $
 *   $Date: 2007-08-02 15:19:41 -0700 (Thu, 02 Aug 2007) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_WTENGINE_H
#define _EAS_WTENGINE_H

/* option sanity check */
#if defined(_OPTIMIZED_MONO) && defined(_FILTER_ENABLED)
#error "Incompatible build settings: _OPTIMIZED_MONO cannot be used with _FILTER_ENABLED"
#endif

#if defined(_OPTIMIZED_MONO) && (NUM_OUTPUT_CHANNELS != 1)
#error "Incompatible build settings: _OPTIMIZED_MONO can only be used with NUM_OUTPUT_CHANNELS = 1"
#endif

#include "eas_wt_IPC_frame.h"

/*----------------------------------------------------------------------------
 * defines
 *----------------------------------------------------------------------------
*/
#define WT_NOISE_GENERATOR                  0xffffffff

/*----------------------------------------------------------------------------
 * typedefs
 *----------------------------------------------------------------------------
*/

/*----------------------------------------------------------------------------
 * S_WT_INT_FRAME
 *
 * This structure includes S_WT_FRAME plus the bus mixing
 * parameters for the internal voices.
 *----------------------------------------------------------------------------
*/
typedef struct s_wt_int_frame_tag
{
    S_WT_FRAME      frame;
    EAS_PCM         *pAudioBuffer;
    EAS_I32         *pMixBuffer;
    EAS_I32         numSamples;
    EAS_I32         prevGain;
} S_WT_INT_FRAME;

#if defined(_FILTER_ENABLED)
/*----------------------------------------------------------------------------
 * S_FILTER_CONTROL data structure
 *----------------------------------------------------------------------------
*/
typedef struct s_filter_control_tag
{
    EAS_I16     z1;                             /* 1 sample delay state variable */
    EAS_I16     z2;                             /* 2 sample delay state variable */
} S_FILTER_CONTROL;
#endif

/*------------------------------------
 * S_LFO_CONTROL data structure
 *------------------------------------
*/
typedef struct s_lfo_control_tag
{
    EAS_I16     lfoValue;           /* LFO current output value */
    EAS_I16     lfoPhase;           /* LFO current phase */
} S_LFO_CONTROL;

/* bit definitions for S_WT_VOICE:flags */
#define WT_FLAGS_ADPCM_NIBBLE           1       /* high/low nibble flag */
#define WT_FLAGS_ADPCM_READY            2       /* first 2 samples are decoded */
#define WT_FLAGS_USE_ADPCM              4       /* sample is ADPCM encoded */

/* eg1State and eg2State */
typedef enum {
    eEnvelopeStateInit = 0,
    eEnvelopeStateDelay,
    eEnvelopeStateAttack,
    eEnvelopeStateHold,
    eEnvelopeStateDecay,
    eEnvelopeStateSustain,
    eEnvelopeStateRelease,
    eEnvelopeStateMuting,
    eEnvelopeStateMuted,
    eEnvelopeStateInvalid           /* should never be in this state! */
} E_ENVELOPE_STATE;

#define DEFAULT_EG1_STATE       eEnvelopeStateAttack
#define DEFAULT_EG1_VALUE       0
#define DEFAULT_EG1_INCREMENT   0
#define DEFAULT_EG2_STATE       eEnvelopeStateAttack
#define DEFAULT_EG2_VALUE       0
#define DEFAULT_EG2_INCREMENT   0

/*----------------------------------------------------------------------------
 * S_WT_VOICE
 *
 * This structure contains state data for the wavetable engine
 *----------------------------------------------------------------------------
*/
typedef struct s_wt_voice_tag
{
    EAS_U32             loopEnd;                /* points to last PCM sample (not 1 beyond last) */
    EAS_U32             loopStart;              /* points to first sample at start of loop */
    EAS_U32             phaseAccum;             /* current sample, integer portion of phase */
    EAS_U32             phaseFrac;              /* fractional portion of phase */

#if (NUM_OUTPUT_CHANNELS == 2)
    EAS_I16             gainLeft;               /* current gain, left ch  */
    EAS_I16             gainRight;              /* current gain, right ch */
#endif

#if defined(_FILTER_ENABLED)
    S_FILTER_CONTROL    filter;                 /* low pass filter */
#endif

    S_LFO_CONTROL       modLFO;                 /* modulator LFO */

#ifdef DLS_SYNTHESIZER
    S_LFO_CONTROL       vibLFO;                 /* vibrato LFO */
#endif

    /* envelope control */
    EAS_I16             eg1Value;
    EAS_I16             eg2Value;
    EAS_I16             eg1Increment;
    EAS_I16             eg2Increment;
    EAS_U8              eg1State;
    EAS_U8              eg2State;

    EAS_U16             artIndex;               /* index to articulation params */

} S_WT_VOICE;

/*----------------------------------------------------------------------------
 * prototypes
 *----------------------------------------------------------------------------
*/
EAS_BOOL WT_CheckSampleEnd (S_WT_VOICE *pWTVoice, S_WT_INT_FRAME *pWTIntFrame, EAS_BOOL update);
void WT_ProcessVoice (S_WT_VOICE *pWTVoice, S_WT_INT_FRAME *pWTIntFrame);

#ifdef EAS_SPLIT_WT_SYNTH
void WTE_ConfigVoice (EAS_I32 voiceNum, S_WT_CONFIG *pWTConfig, EAS_FRAME_BUFFER_HANDLE pFrameBuffer);
void WTE_ProcessVoice (EAS_I32 voiceNum, S_WT_FRAME *pWTParams, EAS_FRAME_BUFFER_HANDLE pFrameBuffer);
#endif

#endif
