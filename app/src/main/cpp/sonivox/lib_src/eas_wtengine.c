/*----------------------------------------------------------------------------
 *
 * File:
 * eas_wtengine.c
 *
 * Contents and purpose:
 * This file contains the critical synthesizer components that need to
 * be optimized for best performance.
 *
 * Copyright Sonic Network Inc. 2004-2005

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
 *   $Revision: 844 $
 *   $Date: 2007-08-23 14:33:32 -0700 (Thu, 23 Aug 2007) $
 *----------------------------------------------------------------------------
*/

/*------------------------------------
 * includes
 *------------------------------------
*/
#define LOG_TAG "Sonivox"
#include "util/log.h"

#include "eas_types.h"
#include "eas_math.h"
#include "eas_audioconst.h"
#include "eas_sndlib.h"
#include "eas_wtengine.h"
#include "eas_mixer.h"

/*----------------------------------------------------------------------------
 * prototypes
 *----------------------------------------------------------------------------
*/
extern void WT_NoiseGenerator (S_WT_VOICE *pWTVoice, S_WT_INT_FRAME *pWTIntFrame);
extern void WT_VoiceGain (S_WT_VOICE *pWTVoice, S_WT_INT_FRAME *pWTIntFrame);

#if defined(_OPTIMIZED_MONO)
extern void WT_InterpolateMono (S_WT_VOICE *pWTVoice, S_WT_INT_FRAME *pWTIntFrame);
#else
extern void WT_InterpolateNoLoop (S_WT_VOICE *pWTVoice, S_WT_INT_FRAME *pWTIntFrame);
extern void WT_Interpolate (S_WT_VOICE *pWTVoice, S_WT_INT_FRAME *pWTIntFrame);
#endif

#if defined(_FILTER_ENABLED)
extern void WT_VoiceFilter (S_FILTER_CONTROL*pFilter, S_WT_INT_FRAME *pWTIntFrame);
#endif

// The PRNG in WT_NoiseGenerator relies on modulo math
#undef  NO_INT_OVERFLOW_CHECKS
#define NO_INT_OVERFLOW_CHECKS __attribute__((no_sanitize("integer")))

#if defined(_OPTIMIZED_MONO) || !defined(NATIVE_EAS_KERNEL) || defined(_16_BIT_SAMPLES)
/*----------------------------------------------------------------------------
 * WT_VoiceGain
 *----------------------------------------------------------------------------
 * Purpose:
 * Output gain for individual voice
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pWTVoice) reserved for future use */
void WT_VoiceGain (S_WT_VOICE *pWTVoice, S_WT_INT_FRAME *pWTIntFrame)
{
    EAS_I32 *pMixBuffer;
    EAS_PCM *pInputBuffer;
    EAS_I32 gain;
    EAS_I32 gainIncrement;
    EAS_I32 tmp0;
    EAS_I32 tmp1;
    EAS_I32 tmp2;
    EAS_I32 numSamples;

#if (NUM_OUTPUT_CHANNELS == 2)
    EAS_I32 gainLeft, gainRight;
#endif

    /* initialize some local variables */
    numSamples = pWTIntFrame->numSamples;
    if (numSamples <= 0) {
        ALOGE("b/26366256");
        android_errorWriteLog(0x534e4554, "26366256");
        return;
    }
    pMixBuffer = pWTIntFrame->pMixBuffer;
    pInputBuffer = pWTIntFrame->pAudioBuffer;

    gainIncrement = (pWTIntFrame->frame.gainTarget - pWTIntFrame->prevGain) * (1 << (16 - SYNTH_UPDATE_PERIOD_IN_BITS));
    if (gainIncrement < 0)
        gainIncrement++;
    gain = pWTIntFrame->prevGain * (1 << 16);

#if (NUM_OUTPUT_CHANNELS == 2)
    gainLeft = pWTVoice->gainLeft;
    gainRight = pWTVoice->gainRight;
#endif

    while (numSamples--) {

        /* incremental gain step to prevent zipper noise */
        tmp0 = *pInputBuffer++;
        gain += gainIncrement;
        /*lint -e{704} <avoid divide>*/
        tmp2 = gain >> 16;

        /* scale sample by gain */
        tmp2 *= tmp0;


        /* stereo output */
#if (NUM_OUTPUT_CHANNELS == 2)
        /*lint -e{704} <avoid divide>*/
        tmp2 = tmp2 >> 14;

        /* get the current sample in the final mix buffer */
        tmp1 = *pMixBuffer;

        /* left channel */
        tmp0 = tmp2 * gainLeft;
        /*lint -e{704} <avoid divide>*/
        tmp0 = tmp0 >> NUM_MIXER_GUARD_BITS;
        tmp1 += tmp0;
        *pMixBuffer++ = tmp1;

        /* get the current sample in the final mix buffer */
        tmp1 = *pMixBuffer;

        /* right channel */
        tmp0 = tmp2 * gainRight;
        /*lint -e{704} <avoid divide>*/
        tmp0 = tmp0 >> NUM_MIXER_GUARD_BITS;
        tmp1 += tmp0;
        *pMixBuffer++ = tmp1;

        /* mono output */
#else

        /* get the current sample in the final mix buffer */
        tmp1 = *pMixBuffer;
        /*lint -e{704} <avoid divide>*/
        tmp2 = tmp2 >> (NUM_MIXER_GUARD_BITS - 1);
        tmp1 += tmp2;
        *pMixBuffer++ = tmp1;
#endif

    }
}
#endif

#if !defined(NATIVE_EAS_KERNEL) || defined(_16_BIT_SAMPLES)
/*----------------------------------------------------------------------------
 * WT_Interpolate
 *----------------------------------------------------------------------------
 * Purpose:
 * Interpolation engine for wavetable synth
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
void WT_Interpolate (S_WT_VOICE *pWTVoice, S_WT_INT_FRAME *pWTIntFrame)
{
    EAS_PCM *pOutputBuffer;
    EAS_I32 phaseInc;
    EAS_I32 phaseFrac;
    EAS_I32 acc0;
    const EAS_SAMPLE *pSamples;
    const EAS_SAMPLE *loopEnd;
    EAS_I32 samp1;
    EAS_I32 samp2;
    EAS_I32 numSamples;

    /* initialize some local variables */
    numSamples = pWTIntFrame->numSamples;
    if (numSamples <= 0) {
        ALOGE("b/26366256");
        android_errorWriteLog(0x534e4554, "26366256");
        return;
    }
    pOutputBuffer = pWTIntFrame->pAudioBuffer;

    loopEnd = (const EAS_SAMPLE*) pWTVoice->loopEnd + 1;
    pSamples = (const EAS_SAMPLE*) pWTVoice->phaseAccum;
    /*lint -e{713} truncation is OK */
    phaseFrac = pWTVoice->phaseFrac & PHASE_FRAC_MASK;
    phaseInc = pWTIntFrame->frame.phaseIncrement;

    /* fetch adjacent samples */
#if defined(_8_BIT_SAMPLES)
    /*lint -e{701} <avoid multiply for performance>*/
    samp1 = pSamples[0] << 8;
    /*lint -e{701} <avoid multiply for performance>*/
    samp2 = pSamples[1] << 8;
#else
    samp1 = pSamples[0];
    samp2 = pSamples[1];
#endif

    while (numSamples--) {

        EAS_I32 nextSamplePhaseInc;

        /* linear interpolation */
        acc0 = samp2 - samp1;
        acc0 = acc0 * phaseFrac;
        /*lint -e{704} <avoid divide>*/
        acc0 = samp1 + (acc0 >> NUM_PHASE_FRAC_BITS);

        /* save new output sample in buffer */
        /*lint -e{704} <avoid divide>*/
        *pOutputBuffer++ = (EAS_I16)(acc0 >> 2);

        /* increment phase */
        phaseFrac += phaseInc;
        /*lint -e{704} <avoid divide>*/
        nextSamplePhaseInc = phaseFrac >> NUM_PHASE_FRAC_BITS;

        /* next sample */
        if (nextSamplePhaseInc > 0) {
            /* advance sample pointer */
            pSamples +=  nextSamplePhaseInc;
            phaseFrac = phaseFrac & PHASE_FRAC_MASK;

            /* decrementing pSamples by entire buffer length until second pSample is within */
            /* loopEnd                                                                      */
            while (&pSamples[1] >= loopEnd) {
                pSamples -= (loopEnd - (const EAS_SAMPLE*)pWTVoice->loopStart);
            }

            /* fetch new samples */
#if defined(_8_BIT_SAMPLES)
            /*lint -e{701} <avoid multiply for performance>*/
            samp1 = pSamples[0] << 8;
            /*lint -e{701} <avoid multiply for performance>*/
            samp2 = pSamples[1] << 8;
#else
            samp1 = pSamples[0];
            samp2 = pSamples[1];
#endif
        }
    }

    /* save pointer and phase */
    pWTVoice->phaseAccum = (EAS_U32) pSamples;
    pWTVoice->phaseFrac = (EAS_U32) phaseFrac;
}
#endif

#if !defined(NATIVE_EAS_KERNEL) || defined(_16_BIT_SAMPLES)
/*----------------------------------------------------------------------------
 * WT_InterpolateNoLoop
 *----------------------------------------------------------------------------
 * Purpose:
 * Interpolation engine for wavetable synth
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
void WT_InterpolateNoLoop (S_WT_VOICE *pWTVoice, S_WT_INT_FRAME *pWTIntFrame)
{
    EAS_PCM *pOutputBuffer;
    EAS_I32 phaseInc;
    EAS_I32 phaseFrac;
    EAS_I32 acc0;
    const EAS_SAMPLE *pSamples;
    const EAS_SAMPLE *bufferEndP1;
    EAS_I32 samp1;
    EAS_I32 samp2;
    EAS_I32 numSamples;

    /* initialize some local variables */
    numSamples = pWTIntFrame->numSamples;
    if (numSamples <= 0) {
        ALOGE("b/26366256");
        android_errorWriteLog(0x534e4554, "26366256");
        return;
    }
    pOutputBuffer = pWTIntFrame->pAudioBuffer;

    phaseInc = pWTIntFrame->frame.phaseIncrement;
    bufferEndP1 = (const EAS_SAMPLE*) pWTVoice->loopEnd + 1;
    pSamples = (const EAS_SAMPLE*) pWTVoice->phaseAccum;
    phaseFrac = (EAS_I32)(pWTVoice->phaseFrac & PHASE_FRAC_MASK);

    /* fetch adjacent samples */
#if defined(_8_BIT_SAMPLES)
    /*lint -e{701} <avoid multiply for performance>*/
    samp1 = pSamples[0] << 8;
    /*lint -e{701} <avoid multiply for performance>*/
    samp2 = pSamples[1] << 8;
#else
    samp1 = pSamples[0];
    samp2 = pSamples[1];
#endif

    while (numSamples--) {

        EAS_I32 nextSamplePhaseInc;

        /* linear interpolation */
        acc0 = samp2 - samp1;
        acc0 = acc0 * phaseFrac;
        /*lint -e{704} <avoid divide>*/
        acc0 = samp1 + (acc0 >> NUM_PHASE_FRAC_BITS);

        /* save new output sample in buffer */
        /*lint -e{704} <avoid divide>*/
        *pOutputBuffer++ = (EAS_I16)(acc0 >> 2);

        /* increment phase */
        phaseFrac += phaseInc;
        /*lint -e{704} <avoid divide>*/
        nextSamplePhaseInc = phaseFrac >> NUM_PHASE_FRAC_BITS;

        /* next sample */
        if (nextSamplePhaseInc > 0) {

            /* check for loop end */
            if ( &pSamples[nextSamplePhaseInc+1] >= bufferEndP1) {
                break;
            }

            /* advance sample pointer */
            pSamples += nextSamplePhaseInc;
            phaseFrac = (EAS_I32)((EAS_U32)phaseFrac & PHASE_FRAC_MASK);

            /* fetch new samples */
#if defined(_8_BIT_SAMPLES)
            /*lint -e{701} <avoid multiply for performance>*/
            samp1 = pSamples[0] << 8;
            /*lint -e{701} <avoid multiply for performance>*/
            samp2 = pSamples[1] << 8;
#else
            samp1 = pSamples[0];
            samp2 = pSamples[1];
#endif
        }
    }

    /* save pointer and phase */
    pWTVoice->phaseAccum = (EAS_U32) pSamples;
    pWTVoice->phaseFrac = (EAS_U32) phaseFrac;
}
#endif

#if defined(_FILTER_ENABLED) && !defined(NATIVE_EAS_KERNEL)
/*----------------------------------------------------------------------------
 * WT_VoiceFilter
 *----------------------------------------------------------------------------
 * Purpose:
 * Implements a 2-pole filter
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
void WT_VoiceFilter (S_FILTER_CONTROL *pFilter, S_WT_INT_FRAME *pWTIntFrame)
{
    EAS_PCM *pAudioBuffer;
    EAS_I32 k;
    EAS_I32 b1;
    EAS_I32 b2;
    EAS_I32 z1;
    EAS_I32 z2;
    EAS_I32 acc0;
    EAS_I32 acc1;
    EAS_I32 numSamples;

    /* initialize some local variables */
    numSamples = pWTIntFrame->numSamples;
    if (numSamples <= 0) {
        ALOGE("b/26366256");
        android_errorWriteLog(0x534e4554, "26366256");
        return;
    }
    pAudioBuffer = pWTIntFrame->pAudioBuffer;

    z1 = pFilter->z1;
    z2 = pFilter->z2;
    b1 = -pWTIntFrame->frame.b1;

    /*lint -e{702} <avoid divide> */
    b2 = -pWTIntFrame->frame.b2 >> 1;

    /*lint -e{702} <avoid divide> */
    k = pWTIntFrame->frame.k >> 1;

    while (numSamples--)
    {

        /* do filter calculations */
        acc0 = *pAudioBuffer;
        acc1 = z1 * b1;
        acc1 += z2 * b2;
        acc0 = acc1 + k * acc0;
        z2 = z1;

        /*lint -e{702} <avoid divide> */
        z1 = acc0 >> 14;
        *pAudioBuffer++ = (EAS_I16) z1;
    }

    /* save delay values     */
    pFilter->z1 = (EAS_I16) z1;
    pFilter->z2 = (EAS_I16) z2;
}
#endif

/*----------------------------------------------------------------------------
 * WT_NoiseGenerator
 *----------------------------------------------------------------------------
 * Purpose:
 * Generate pseudo-white noise using PRNG and interpolation engine
 *
 * Inputs:
 *
 * Outputs:
 *
 * Notes:
 * This output is scaled -12dB to prevent saturation in the filter. For a
 * high quality synthesizer, the output can be set to full scale, however
 * if the filter is used, it can overflow with certain coefficients. In this
 * case, either a saturation operation should take in the filter before
 * scaling back to 16 bits or the signal path should be increased to 18 bits
 * or more.
 *----------------------------------------------------------------------------
*/
 void NO_INT_OVERFLOW_CHECKS WT_NoiseGenerator (S_WT_VOICE *pWTVoice, S_WT_INT_FRAME *pWTIntFrame)
 {
    EAS_PCM *pOutputBuffer;
    EAS_I32 phaseInc;
    EAS_I32 tmp0;
    EAS_I32 tmp1;
    EAS_I32 nInterpolatedSample;
    EAS_I32 numSamples;

    /* initialize some local variables */
    numSamples = pWTIntFrame->numSamples;
    if (numSamples <= 0) {
        ALOGE("b/26366256");
        android_errorWriteLog(0x534e4554, "26366256");
        return;
    }
    pOutputBuffer = pWTIntFrame->pAudioBuffer;
    phaseInc = pWTIntFrame->frame.phaseIncrement;

    /* get last two samples generated */
    /*lint -e{704} <avoid divide for performance>*/
    tmp0 = (EAS_I32) (pWTVoice->phaseAccum) >> 18;
    /*lint -e{704} <avoid divide for performance>*/
    tmp1 = (EAS_I32) (pWTVoice->loopEnd) >> 18;

    /* generate a buffer of noise */
    while (numSamples--) {
        nInterpolatedSample = MULT_AUDIO_COEF( tmp0, (PHASE_ONE - pWTVoice->phaseFrac));
        nInterpolatedSample += MULT_AUDIO_COEF( tmp1, pWTVoice->phaseFrac);
        *pOutputBuffer++ = (EAS_PCM) nInterpolatedSample;

        /* update PRNG */
        pWTVoice->phaseFrac += (EAS_U32) phaseInc;
        if (GET_PHASE_INT_PART(pWTVoice->phaseFrac))    {
            tmp0 = tmp1;
            pWTVoice->phaseAccum = pWTVoice->loopEnd;
            pWTVoice->loopEnd = (5 * pWTVoice->loopEnd + 1);
            tmp1 = (EAS_I32) (pWTVoice->loopEnd) >> 18;
            pWTVoice->phaseFrac = GET_PHASE_FRAC_PART(pWTVoice->phaseFrac);
        }

    }
}

#ifndef _OPTIMIZED_MONO
/*----------------------------------------------------------------------------
 * WT_ProcessVoice
 *----------------------------------------------------------------------------
 * Purpose:
 * This routine does the block processing for one voice. It is isolated
 * from the main synth code to allow for various implementation-specific
 * optimizations. It calls the interpolator, filter, and gain routines
 * appropriate for a particular configuration.
 *
 * Inputs:
 *
 * Outputs:
 *
 * Notes:
 *----------------------------------------------------------------------------
*/
void WT_ProcessVoice (S_WT_VOICE *pWTVoice, S_WT_INT_FRAME *pWTIntFrame)
{

    /* use noise generator */
    if (pWTVoice->loopStart == WT_NOISE_GENERATOR)
        WT_NoiseGenerator(pWTVoice, pWTIntFrame);

    /* generate interpolated samples for looped waves */
    else if (pWTVoice->loopStart != pWTVoice->loopEnd)
        WT_Interpolate(pWTVoice, pWTIntFrame);

    /* generate interpolated samples for unlooped waves */
    else
    {
        WT_InterpolateNoLoop(pWTVoice, pWTIntFrame);
    }

#ifdef _FILTER_ENABLED
    if (pWTIntFrame->frame.k != 0)
        WT_VoiceFilter(&pWTVoice->filter, pWTIntFrame);
#endif

//2 TEST NEW MIXER FUNCTION
#ifdef UNIFIED_MIXER
    {
        EAS_I32 gainLeft, gainIncLeft;

#if (NUM_OUTPUT_CHANNELS == 2)
        EAS_I32 gainRight, gainIncRight;
#endif

        gainLeft = (pWTIntFrame->prevGain * pWTVoice->gainLeft) << 1;
        gainIncLeft = (((pWTIntFrame->frame.gainTarget * pWTVoice->gainLeft) << 1) - gainLeft) >> SYNTH_UPDATE_PERIOD_IN_BITS;

#if (NUM_OUTPUT_CHANNELS == 2)
        gainRight = (pWTIntFrame->prevGain * pWTVoice->gainRight) << 1;
        gainIncRight = (((pWTIntFrame->frame.gainTarget * pWTVoice->gainRight) << 1) - gainRight) >> SYNTH_UPDATE_PERIOD_IN_BITS;
        EAS_MixStream(
            pWTIntFrame->pAudioBuffer,
            pWTIntFrame->pMixBuffer,
            pWTIntFrame->numSamples,
            gainLeft,
            gainRight,
            gainIncLeft,
            gainIncRight,
            MIX_FLAGS_STEREO_OUTPUT);

#else
        EAS_MixStream(
            pWTIntFrame->pAudioBuffer,
            pWTIntFrame->pMixBuffer,
            pWTIntFrame->numSamples,
            gainLeft,
            0,
            gainIncLeft,
            0,
            0);
#endif
    }

#else
    /* apply gain, and left and right gain */
    WT_VoiceGain(pWTVoice, pWTIntFrame);
#endif
}
#endif

#if defined(_OPTIMIZED_MONO) && !defined(NATIVE_EAS_KERNEL)
/*----------------------------------------------------------------------------
 * WT_InterpolateMono
 *----------------------------------------------------------------------------
 * Purpose:
 * A C version of the sample interpolation + gain routine, optimized for mono.
 * It's not pretty, but it matches the assembly code exactly.
 *
 * Inputs:
 *
 * Outputs:
 *
 * Notes:
 *----------------------------------------------------------------------------
*/
void WT_InterpolateMono (S_WT_VOICE *pWTVoice, S_WT_INT_FRAME *pWTIntFrame)
{
    EAS_I32 *pMixBuffer;
    const EAS_I8 *pLoopEnd;
    const EAS_I8 *pCurrentPhaseInt;
    EAS_I32 numSamples;
    EAS_I32 gain;
    EAS_I32 gainIncrement;
    EAS_I32 currentPhaseFrac;
    EAS_I32 phaseInc;
    EAS_I32 tmp0;
    EAS_I32 tmp1;
    EAS_I32 tmp2;
    EAS_I8 *pLoopStart;

    numSamples = pWTIntFrame->numSamples;
    if (numSamples <= 0) {
        ALOGE("b/26366256");
        android_errorWriteLog(0x534e4554, "26366256");
        return;
    }
    pMixBuffer = pWTIntFrame->pMixBuffer;

    /* calculate gain increment */
    gainIncrement = (pWTIntFrame->gainTarget - pWTIntFrame->prevGain) * (1 << (16 - SYNTH_UPDATE_PERIOD_IN_BITS));
    if (gainIncrement < 0)
        gainIncrement++;
    gain = pWTIntFrame->prevGain * (1 << 16);

    pCurrentPhaseInt = pWTVoice->pPhaseAccum;
    currentPhaseFrac = pWTVoice->phaseFrac;
    phaseInc = pWTIntFrame->phaseIncrement;

    pLoopStart = pWTVoice->pLoopStart;
    pLoopEnd = pWTVoice->pLoopEnd + 1;

InterpolationLoop:
    tmp0 = (EAS_I32)(pCurrentPhaseInt - pLoopEnd);
    if (tmp0 >= 0)
        pCurrentPhaseInt = pLoopStart + tmp0;

    tmp0 = *pCurrentPhaseInt;
    tmp1 = *(pCurrentPhaseInt + 1);

    tmp2 = phaseInc + currentPhaseFrac;

    tmp1 = tmp1 - tmp0;
    tmp1 = tmp1 * currentPhaseFrac;

    tmp1 = tmp0 + (tmp1 >> NUM_EG1_FRAC_BITS);

    pCurrentPhaseInt += (tmp2 >> NUM_PHASE_FRAC_BITS);
    currentPhaseFrac = tmp2 & PHASE_FRAC_MASK;

    gain += gainIncrement;
    tmp2 = (gain >> SYNTH_UPDATE_PERIOD_IN_BITS);

    tmp0 = *pMixBuffer;
    tmp2 = tmp1 * tmp2;
    tmp2 = (tmp2 >> 9);
    tmp0 = tmp2 + tmp0;
    *pMixBuffer++ = tmp0;

    numSamples--;
    if (numSamples > 0)
        goto InterpolationLoop;

    pWTVoice->pPhaseAccum = pCurrentPhaseInt;
    pWTVoice->phaseFrac = currentPhaseFrac;
    /*lint -e{702} <avoid divide>*/
    pWTVoice->gain = (EAS_I16)(gain >> SYNTH_UPDATE_PERIOD_IN_BITS);
}
#endif

#ifdef _OPTIMIZED_MONO
/*----------------------------------------------------------------------------
 * WT_ProcessVoice
 *----------------------------------------------------------------------------
 * Purpose:
 * This routine does the block processing for one voice. It is isolated
 * from the main synth code to allow for various implementation-specific
 * optimizations. It calls the interpolator, filter, and gain routines
 * appropriate for a particular configuration.
 *
 * Inputs:
 *
 * Outputs:
 *
 * Notes:
 * This special version works handles an optimized mono-only signal
 * without filters
 *----------------------------------------------------------------------------
*/
void WT_ProcessVoice (S_WT_VOICE *pWTVoice, S_WT_INT_FRAME *pWTIntFrame)
{

    /* use noise generator */
    if (pWTVoice->loopStart== WT_NOISE_GENERATOR)
    {
        WT_NoiseGenerator(pWTVoice, pWTIntFrame);
        WT_VoiceGain(pWTVoice, pWTIntFrame);
    }

    /* or generate interpolated samples */
    else
    {
        WT_InterpolateMono(pWTVoice, pWTIntFrame);
    }
}
#endif

