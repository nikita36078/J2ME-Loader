/*----------------------------------------------------------------------------
 *
 * File:
 * eas_reverbdata.h
 *
 * Contents and purpose:
 * Contains the prototypes for the Reverb effect.
 *
 *
 * Copyright Sonic Network Inc. 2006

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
 *   $Revision: 499 $
 *   $Date: 2006-12-11 16:07:20 -0800 (Mon, 11 Dec 2006) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_REVERBDATA_H
#define _EAS_REVERBDATA_H

#include "eas_types.h"
#include "eas_audioconst.h"

/*------------------------------------
 * defines
 *------------------------------------
*/

/*
CIRCULAR() calculates the array index using modulo arithmetic.
The "trick" is that modulo arithmetic is simplified by masking
the effective address where the mask is (2^n)-1. This only works
if the buffer size is a power of two.
*/
#define CIRCULAR(base,offset,size) (EAS_U32)(               \
            (                                               \
                ((EAS_I32)(base)) + ((EAS_I32)(offset))     \
            )                                               \
            & size                                          \
                                            )

/* reverb parameters are updated every 2^(REVERB_UPDATE_PERIOD_IN_BITS) samples */
#if defined (_SAMPLE_RATE_8000)

#define REVERB_UPDATE_PERIOD_IN_BITS        5
#define REVERB_BUFFER_SIZE_IN_SAMPLES       2048

#elif defined (_SAMPLE_RATE_16000)

#define REVERB_UPDATE_PERIOD_IN_BITS        6
#define REVERB_BUFFER_SIZE_IN_SAMPLES       4096

#elif defined (_SAMPLE_RATE_22050)

#define REVERB_UPDATE_PERIOD_IN_BITS        7
#define REVERB_BUFFER_SIZE_IN_SAMPLES       4096

#elif defined (_SAMPLE_RATE_32000)

#define REVERB_UPDATE_PERIOD_IN_BITS        7
#define REVERB_BUFFER_SIZE_IN_SAMPLES       8192

#elif defined (_SAMPLE_RATE_44100)

#define REVERB_UPDATE_PERIOD_IN_BITS        8
#define REVERB_BUFFER_SIZE_IN_SAMPLES       8192

#elif defined (_SAMPLE_RATE_48000)

#define REVERB_UPDATE_PERIOD_IN_BITS        8
#define REVERB_BUFFER_SIZE_IN_SAMPLES       8192

#endif

// Define a mask for circular addressing, so that array index
// can wraparound and stay in array boundary of 0, 1, ..., (buffer size -1)
// The buffer size MUST be a power of two
#define REVERB_BUFFER_MASK                  (REVERB_BUFFER_SIZE_IN_SAMPLES -1)

#define REVERB_MAX_ROOM_TYPE            4   // any room numbers larger than this are invalid
#define REVERB_MAX_NUM_REFLECTIONS      5   // max num reflections per channel

/* synth parameters are updated every SYNTH_UPDATE_PERIOD_IN_SAMPLES */
#define REVERB_UPDATE_PERIOD_IN_SAMPLES (EAS_I32)(0x1L << REVERB_UPDATE_PERIOD_IN_BITS)

/*
calculate the update counter by bitwise ANDING with this value to
generate a 2^n modulo value
*/
#define REVERB_MODULO_UPDATE_PERIOD_IN_SAMPLES  (EAS_I32)(REVERB_UPDATE_PERIOD_IN_SAMPLES -1)

/* synth parameters are updated every SYNTH_UPDATE_PERIOD_IN_SECONDS seconds */
#define REVERB_UPDATE_PERIOD_IN_SECONDS     (REVERB_UPDATE_PERIOD_IN_SAMPLES / _OUTPUT_SAMPLE_RATE)

// xfade parameters
#define REVERB_XFADE_PERIOD_IN_SECONDS      (100.0 / 1000.0)        // xfade once every this many seconds

#define REVERB_XFADE_PERIOD_IN_SAMPLES      (REVERB_XFADE_PERIOD_IN_SECONDS * _OUTPUT_SAMPLE_RATE)

#define REVERB_XFADE_PHASE_INCREMENT    (EAS_I16)(65536 / ((EAS_I16)REVERB_XFADE_PERIOD_IN_SAMPLES/(EAS_I16)REVERB_UPDATE_PERIOD_IN_SAMPLES))

/**********/
/* the entire synth uses various flags in a bit field */

/* if flag is set, synth reset has been requested */
#define REVERB_FLAG_RESET_IS_REQUESTED          0x01    /* bit 0 */
#define MASK_REVERB_RESET_IS_REQUESTED          0x01
#define MASK_REVERB_RESET_IS_NOT_REQUESTED      (EAS_U32)(~MASK_REVERB_RESET_IS_REQUESTED)

/*
by default, we always want to update ALL channel parameters
when we reset the synth (e.g., during GM ON)
*/
#define DEFAULT_REVERB_FLAGS                    0x0

/* coefficients for generating sin, cos */
#define REVERB_PAN_G2   4294940151          /* -0.82842712474619 = 2 - 4/sqrt(2) */
/*
EAS_I32 nPanG1 = +1.0 for sin
EAS_I32 nPanG1 = -1.0 for cos
*/
#define REVERB_PAN_G0   23170               /* 0.707106781186547 = 1/sqrt(2) */

/*************************************************************/
// define the input injection points
#define GUARD               5                       // safety guard of this many samples

#define MAX_AP_TIME         (double) (20.0/1000.0)  // delay time in milliseconds
#define MAX_DELAY_TIME      (double) (65.0/1000.0)  // delay time in milliseconds

#define MAX_AP_SAMPLES      (int)(((double) MAX_AP_TIME)    * ((double) _OUTPUT_SAMPLE_RATE))
#define MAX_DELAY_SAMPLES   (int)(((double) MAX_DELAY_TIME) * ((double) _OUTPUT_SAMPLE_RATE))

#define AP0_IN              0
#define AP1_IN              (AP0_IN     + MAX_AP_SAMPLES    + GUARD)
#define DELAY0_IN           (AP1_IN     + MAX_AP_SAMPLES    + GUARD)
#define DELAY1_IN           (DELAY0_IN  + MAX_DELAY_SAMPLES + GUARD)

// Define the max offsets for the end points of each section
// i.e., we don't expect a given section's taps to go beyond
// the following limits
#define AP0_OUT             (AP0_IN     + MAX_AP_SAMPLES    -1)
#define AP1_OUT             (AP1_IN     + MAX_AP_SAMPLES    -1)
#define DELAY0_OUT          (DELAY0_IN  + MAX_DELAY_SAMPLES -1)
#define DELAY1_OUT          (DELAY1_IN  + MAX_DELAY_SAMPLES -1)

#define REVERB_DEFAULT_ROOM_NUMBER      1       // default preset number
#define DEFAULT_AP0_LENGTH              (int)(((double) (17.0/1000.0))  * ((double) _OUTPUT_SAMPLE_RATE))
#define DEFAULT_AP0_GAIN                19400
#define DEFAULT_AP1_LENGTH              (int)(((double) (16.5/1000.0))  * ((double) _OUTPUT_SAMPLE_RATE))
#define DEFAULT_AP1_GAIN                -19400

#define REVERB_DEFAULT_WET              32767
#define REVERB_DEFAULT_DRY              0

#define EAS_REVERB_WET_MAX              32767
#define EAS_REVERB_WET_MIN              0
#define EAS_REVERB_DRY_MAX              32767
#define EAS_REVERB_DRY_MIN              0

/* parameters for each allpass */
typedef struct
{
    EAS_U16             m_zApOut;       // delay offset for ap out

    EAS_I16             m_nApGain;      // gain for ap

    EAS_U16             m_zApIn;        // delay offset for ap in

} S_ALLPASS_OBJECT;


/* parameters for each allpass */
typedef struct
{
    EAS_PCM             m_zLpf;                     // actual state variable, not a length

    EAS_I16             m_nLpfFwd;                  // lpf forward gain

    EAS_I16             m_nLpfFbk;                  // lpf feedback gain

    EAS_U16             m_zDelay[REVERB_MAX_NUM_REFLECTIONS];   // delay offset for ap out

    EAS_I16             m_nGain[REVERB_MAX_NUM_REFLECTIONS];    // gain for ap

} S_EARLY_REFLECTION_OBJECT;

//demo
typedef struct
{
    EAS_I16             m_nLpfFbk;
    EAS_I16             m_nLpfFwd;

    EAS_I16             m_nEarly;
    EAS_I16             m_nWet;
    EAS_I16             m_nDry;

    EAS_I16             m_nEarlyL_LpfFbk;
    EAS_I16             m_nEarlyL_LpfFwd;

    EAS_I16             m_nEarlyL_Delay0; //8
    EAS_I16             m_nEarlyL_Gain0;
    EAS_I16             m_nEarlyL_Delay1;
    EAS_I16             m_nEarlyL_Gain1;
    EAS_I16             m_nEarlyL_Delay2;
    EAS_I16             m_nEarlyL_Gain2;
    EAS_I16             m_nEarlyL_Delay3;
    EAS_I16             m_nEarlyL_Gain3;
    EAS_I16             m_nEarlyL_Delay4;
    EAS_I16             m_nEarlyL_Gain4;

    EAS_I16             m_nEarlyR_Delay0; //18
    EAS_I16             m_nEarlyR_Gain0;
    EAS_I16             m_nEarlyR_Delay1;
    EAS_I16             m_nEarlyR_Gain1;
    EAS_I16             m_nEarlyR_Delay2;
    EAS_I16             m_nEarlyR_Gain2;
    EAS_I16             m_nEarlyR_Delay3;
    EAS_I16             m_nEarlyR_Gain3;
    EAS_I16             m_nEarlyR_Delay4;
    EAS_I16             m_nEarlyR_Gain4;

    EAS_U16             m_nMaxExcursion; //28
    EAS_I16             m_nXfadeInterval;

    EAS_I16             m_nAp0_ApGain; //30
    EAS_I16             m_nAp0_ApOut;
    EAS_I16             m_nAp1_ApGain;
    EAS_I16             m_nAp1_ApOut;

    EAS_I16             m_rfu4;
    EAS_I16             m_rfu5;
    EAS_I16             m_rfu6;
    EAS_I16             m_rfu7;
    EAS_I16             m_rfu8;
    EAS_I16             m_rfu9;
    EAS_I16             m_rfu10; //43

} S_REVERB_PRESET;

typedef struct
{
    S_REVERB_PRESET     m_sPreset[REVERB_MAX_ROOM_TYPE];    //array of presets

} S_REVERB_PRESET_BANK;

/* parameters for each reverb */
typedef struct
{
    /* controls entire reverb playback volume */
    /* to conserve memory, use the MSB and ignore the LSB */
    EAS_U8              m_nMasterVolume;

    /* update counter keeps track of when synth params need updating */
    /* only needs to be as large as REVERB_UPDATE_PERIOD_IN_SAMPLES */
    EAS_I16             m_nUpdateCounter;

    EAS_U16             m_nMinSamplesToAdd;         /* ComputeReverb() generates this many samples */

    EAS_U8              m_nFlags;                   /* misc flags/bit fields */

    EAS_PCM             *m_pOutputBuffer;
    EAS_PCM             *m_pInputBuffer;

    EAS_U16             m_nNumSamplesInOutputBuffer;
    EAS_U16             m_nNumSamplesInInputBuffer;

    EAS_U16             m_nNumInputSamplesRead;     // if m_nNumInputSamplesRead >= NumSamplesInInputBuffer
                                                    // then get a new input buffer
    EAS_PCM             *m_pNextInputSample;

    EAS_U16             m_nBaseIndex;                                   // base index for circular buffer

    // reverb delay line offsets, allpass parameters, etc:

    EAS_PCM             m_nRevOutFbkR;              // combine feedback reverb right out with dry left in

    S_ALLPASS_OBJECT    m_sAp0;                     // allpass 0 (left channel)

    EAS_U16             m_zD0In;                    // delay offset for delay line D0 in

    EAS_PCM             m_nRevOutFbkL;              // combine feedback reverb left out with dry right in

    S_ALLPASS_OBJECT    m_sAp1;                     // allpass 1 (right channel)

    EAS_U16             m_zD1In;                    // delay offset for delay line D1 in

    // delay output taps, notice criss cross order
    EAS_U16             m_zD0Self;                  // self feeds forward d0 --> d0

    EAS_U16             m_zD1Cross;                 // cross feeds across d1 --> d0

    EAS_PCM             m_zLpf0;                    // actual state variable, not a length

    EAS_U16             m_zD1Self;                  // self feeds forward d1 --> d1

    EAS_U16             m_zD0Cross;                 // cross feeds across d0 --> d1

    EAS_PCM             m_zLpf1;                    // actual state variable, not a length

    EAS_I16             m_nSin;                     // gain for self taps

    EAS_I16             m_nCos;                     // gain for cross taps

    EAS_I16             m_nSinIncrement;            // increment for gain

    EAS_I16             m_nCosIncrement;            // increment for gain

    EAS_I16             m_nLpfFwd;                  // lpf forward gain (includes scaling for mixer)

    EAS_I16             m_nLpfFbk;                  // lpf feedback gain

    EAS_U16             m_nXfadeInterval;           // update/xfade after this many samples

    EAS_U16             m_nXfadeCounter;            // keep track of when to xfade

    EAS_I16             m_nPhase;                   // -1 <= m_nPhase < 1
                                                    // but during sin,cos calculations
                                                    // use m_nPhase/2

    EAS_I16             m_nPhaseIncrement;          // add this to m_nPhase each frame

    EAS_I16             m_nNoise;                   // random noise sample

    EAS_U16             m_nMaxExcursion;            // the taps can excurse +/- this amount

    EAS_BOOL            m_bUseNoise;                // if EAS_TRUE, use noise as input signal

    EAS_BOOL            m_bBypass;                  // if EAS_TRUE, then bypass reverb and copy input to output

    EAS_I16             m_nCurrentRoom;             // preset number for current room

    EAS_I16             m_nNextRoom;                // preset number for next room

    EAS_I16             m_nWet;                     // gain for wet (processed) signal

    EAS_I16             m_nDry;                     // gain for dry (unprocessed) signal

    EAS_I16             m_nEarly;                   // gain for early (widen) signal

    S_EARLY_REFLECTION_OBJECT   m_sEarlyL;          // left channel early reflections
    S_EARLY_REFLECTION_OBJECT   m_sEarlyR;          // right channel early reflections

    EAS_PCM             m_nDelayLine[REVERB_BUFFER_SIZE_IN_SAMPLES];    // one large delay line for all reverb elements

    S_REVERB_PRESET     pPreset;

    S_REVERB_PRESET_BANK    m_sPreset;

    //EAS_I8            preset;

} S_REVERB_OBJECT;


/*------------------------------------
 * prototypes
 *------------------------------------
*/

/*----------------------------------------------------------------------------
 * ReverbUpdateXfade
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the xfade parameters as required
 *
 * Inputs:
 * nNumSamplesToAdd - number of samples to write to buffer
 *
 * Outputs:
 *
 *
 * Side Effects:
 * - xfade parameters will be changed
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ReverbUpdateXfade(S_REVERB_OBJECT* pReverbData, EAS_INT nNumSamplesToAdd);

/*----------------------------------------------------------------------------
 * ReverbCalculateNoise
 *----------------------------------------------------------------------------
 * Purpose:
 * Calculate a noise sample and limit its value
 *
 * Inputs:
 * nMaxExcursion - noise value is limited to this value
 * pnNoise - return new noise sample in this (not limited)
 *
 * Outputs:
 * new limited noise value
 *
 * Side Effects:
 * - *pnNoise noise value is updated
 *
 *----------------------------------------------------------------------------
*/
static EAS_U16 ReverbCalculateNoise(EAS_U16 nMaxExcursion, EAS_I16 *pnNoise);

/*----------------------------------------------------------------------------
 * ReverbCalculateSinCos
 *----------------------------------------------------------------------------
 * Purpose:
 * Calculate a new sin and cosine value based on the given phase
 *
 * Inputs:
 * nPhase   - phase angle
 * pnSin    - input old value, output new value
 * pnCos    - input old value, output new value
 *
 * Outputs:
 *
 * Side Effects:
 * - *pnSin, *pnCos are updated
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ReverbCalculateSinCos(EAS_I16 nPhase, EAS_I16 *pnSin, EAS_I16 *pnCos);

/*----------------------------------------------------------------------------
 * Reverb
 *----------------------------------------------------------------------------
 * Purpose:
 * apply reverb to the given signal
 *
 * Inputs:
 * nNu
 * pnSin    - input old value, output new value
 * pnCos    - input old value, output new value
 *
 * Outputs:
 * number of samples actually reverberated
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT Reverb(S_REVERB_OBJECT* pReverbData, EAS_INT nNumSamplesToAdd, EAS_PCM *pOutputBuffer, EAS_PCM *pInputBuffer);

/*----------------------------------------------------------------------------
 * ReverbReadInPresets()
 *----------------------------------------------------------------------------
 * Purpose: sets global reverb preset bank to defaults
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ReverbReadInPresets(S_REVERB_OBJECT* pReverbData);


/*----------------------------------------------------------------------------
 * ReverbUpdateRoom
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the room's preset parameters as required
 *
 * Inputs:
 *
 * Outputs:
 *
 *
 * Side Effects:
 * - reverb paramters (fbk, fwd, etc) will be changed
 * - m_nCurrentRoom := m_nNextRoom
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ReverbUpdateRoom(S_REVERB_OBJECT* pReverbData);

#endif /* #ifndef _EAS_REVERBDATA_H */


