/*----------------------------------------------------------------------------
 *
 * File:
 * eas_wtsynth.c
 *
 * Contents and purpose:
 * Implements the synthesizer functions.
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
 *   $Revision: 795 $
 *   $Date: 2007-08-01 00:14:45 -0700 (Wed, 01 Aug 2007) $
 *----------------------------------------------------------------------------
*/

// includes
#define LOG_TAG "SYNTH"
#include "log/log.h"

#include "eas_data.h"
#include "eas_report.h"
#include "eas_host.h"
#include "eas_math.h"
#include "eas_synth_protos.h"
#include "eas_wtsynth.h"
#include "eas_pan.h"

#ifdef DLS_SYNTHESIZER
#include "eas_dlssynth.h"
#endif

#ifdef _METRICS_ENABLED
#include "eas_perf.h"
#endif

/* local prototypes */
static EAS_RESULT WT_Initialize(S_VOICE_MGR *pVoiceMgr);
static void WT_ReleaseVoice (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, EAS_I32 voiceNum);
static void WT_MuteVoice (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, EAS_I32 voiceNum);
static void WT_SustainPedal (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, S_SYNTH_CHANNEL *pChannel, EAS_I32 voiceNum);
static EAS_RESULT WT_StartVoice (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, EAS_I32 voiceNum, EAS_U16 regionIndex);
static EAS_BOOL WT_UpdateVoice (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, EAS_I32 voiceNum, EAS_I32 *pMixBuffer, EAS_I32 numSamples);
static void WT_UpdateChannel (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel);
static EAS_I32 WT_UpdatePhaseInc (S_WT_VOICE *pWTVoice, const S_ARTICULATION *pArt, S_SYNTH_CHANNEL *pChannel, EAS_I32 pitchCents);
static EAS_I32 WT_UpdateGain (S_SYNTH_VOICE *pVoice, S_WT_VOICE *pWTVoice, const S_ARTICULATION *pArt, S_SYNTH_CHANNEL *pChannel, EAS_I32 gain);
static void WT_UpdateEG1 (S_WT_VOICE *pWTVoice, const S_ENVELOPE *pEnv);
static void WT_UpdateEG2 (S_WT_VOICE *pWTVoice, const S_ENVELOPE *pEnv);

#ifdef EAS_SPLIT_WT_SYNTH
extern EAS_BOOL WTE_StartFrame (EAS_FRAME_BUFFER_HANDLE pFrameBuffer);
extern EAS_BOOL WTE_EndFrame (EAS_FRAME_BUFFER_HANDLE pFrameBuffer, EAS_I32 *pMixBuffer, EAS_I16 masterGain);
#endif

#ifdef _FILTER_ENABLED
static void WT_UpdateFilter (S_WT_VOICE *pWTVoice, S_WT_INT_FRAME *pIntFrame, const S_ARTICULATION *pArt);
#endif

#ifdef _STATS
extern double statsPhaseIncrement;
extern double statsMaxPhaseIncrement;
extern long statsPhaseSampleCount;
extern double statsSampleSize;
extern long statsSampleCount;
#endif

/*----------------------------------------------------------------------------
 * Synthesizer interface
 *----------------------------------------------------------------------------
*/

const S_SYNTH_INTERFACE wtSynth =
{
    WT_Initialize,
    WT_StartVoice,
    WT_UpdateVoice,
    WT_ReleaseVoice,
    WT_MuteVoice,
    WT_SustainPedal,
    WT_UpdateChannel
};

#ifdef EAS_SPLIT_WT_SYNTH
const S_FRAME_INTERFACE wtFrameInterface =
{
    WTE_StartFrame,
    WTE_EndFrame
};
#endif

/*----------------------------------------------------------------------------
 * WT_Initialize()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 * pVoice - pointer to voice to initialize
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT WT_Initialize (S_VOICE_MGR *pVoiceMgr)
{
    EAS_INT i;

    for (i = 0; i < NUM_WT_VOICES; i++)
    {

        pVoiceMgr->wtVoices[i].artIndex = DEFAULT_ARTICULATION_INDEX;

        pVoiceMgr->wtVoices[i].eg1State = DEFAULT_EG1_STATE;
        pVoiceMgr->wtVoices[i].eg1Value = DEFAULT_EG1_VALUE;
        pVoiceMgr->wtVoices[i].eg1Increment = DEFAULT_EG1_INCREMENT;

        pVoiceMgr->wtVoices[i].eg2State = DEFAULT_EG2_STATE;
        pVoiceMgr->wtVoices[i].eg2Value = DEFAULT_EG2_VALUE;
        pVoiceMgr->wtVoices[i].eg2Increment = DEFAULT_EG2_INCREMENT;

        /* left and right gain values are needed only if stereo output */
#if (NUM_OUTPUT_CHANNELS == 2)
        pVoiceMgr->wtVoices[i].gainLeft = DEFAULT_VOICE_GAIN;
        pVoiceMgr->wtVoices[i].gainRight = DEFAULT_VOICE_GAIN;
#endif

        pVoiceMgr->wtVoices[i].phaseFrac = DEFAULT_PHASE_FRAC;
        pVoiceMgr->wtVoices[i].phaseAccum = DEFAULT_PHASE_INT;

#ifdef _FILTER_ENABLED
        pVoiceMgr->wtVoices[i].filter.z1 = DEFAULT_FILTER_ZERO;
        pVoiceMgr->wtVoices[i].filter.z2 = DEFAULT_FILTER_ZERO;
#endif
    }

    return EAS_TRUE;
}

/*----------------------------------------------------------------------------
 * WT_ReleaseVoice()
 *----------------------------------------------------------------------------
 * Purpose:
 * The selected voice is being released.
 *
 * Inputs:
 * pEASData - pointer to S_EAS_DATA
 * pVoice - pointer to voice to release
 *
 * Outputs:
 * None
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pVoice) used in some implementations */
static void WT_ReleaseVoice (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, EAS_I32 voiceNum)
{
    S_WT_VOICE *pWTVoice;
    const S_ARTICULATION *pArticulation;

#ifdef DLS_SYNTHESIZER
    if (pVoice->regionIndex & FLAG_RGN_IDX_DLS_SYNTH)
    {
        DLS_ReleaseVoice(pVoiceMgr, pSynth, pVoice, voiceNum);
        return;
    }
#endif

    pWTVoice = &pVoiceMgr->wtVoices[voiceNum];
    pArticulation = &pSynth->pEAS->pArticulations[pWTVoice->artIndex];

    /* release EG1 */
    pWTVoice->eg1State = eEnvelopeStateRelease;
    pWTVoice->eg1Increment = pArticulation->eg1.releaseTime;

    /*
    The spec says we should release EG2, but doing so with the current
    voicing is causing clicks. This fix will need to be coordinated with
    a new sound library release
    */

    /* release EG2 */
    pWTVoice->eg2State = eEnvelopeStateRelease;
    pWTVoice->eg2Increment = pArticulation->eg2.releaseTime;
}

/*----------------------------------------------------------------------------
 * WT_MuteVoice()
 *----------------------------------------------------------------------------
 * Purpose:
 * The selected voice is being muted.
 *
 * Inputs:
 * pVoice - pointer to voice to release
 *
 * Outputs:
 * None
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pSynth) used in some implementations */
static void WT_MuteVoice (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, EAS_I32 voiceNum)
{

#ifdef DLS_SYNTHESIZER
    if (pVoice->regionIndex & FLAG_RGN_IDX_DLS_SYNTH)
    {
        DLS_MuteVoice(pVoiceMgr, pSynth, pVoice, voiceNum);
        return;
    }
#endif

    /* clear deferred action flags */
    pVoice->voiceFlags &=
        ~(VOICE_FLAG_DEFER_MIDI_NOTE_OFF |
        VOICE_FLAG_SUSTAIN_PEDAL_DEFER_NOTE_OFF |
        VOICE_FLAG_DEFER_MUTE);

    /* set the envelope state */
    pVoiceMgr->wtVoices[voiceNum].eg1State = eEnvelopeStateMuted;
    pVoiceMgr->wtVoices[voiceNum].eg2State = eEnvelopeStateMuted;
}

/*----------------------------------------------------------------------------
 * WT_SustainPedal()
 *----------------------------------------------------------------------------
 * Purpose:
 * The selected voice is held due to sustain pedal
 *
 * Inputs:
 * pVoice - pointer to voice to sustain
 *
 * Outputs:
 * None
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pChannel) used in some implementations */
static void WT_SustainPedal (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, S_SYNTH_CHANNEL *pChannel, EAS_I32 voiceNum)
{
    S_WT_VOICE *pWTVoice;

#ifdef DLS_SYNTHESIZER
    if (pVoice->regionIndex & FLAG_RGN_IDX_DLS_SYNTH)
    {
        DLS_SustainPedal(pVoiceMgr, pSynth, pVoice, pChannel, voiceNum);
        return;
    }
#endif

    /* don't catch the voice if below the sustain level */
    pWTVoice = &pVoiceMgr->wtVoices[voiceNum];
    if (pWTVoice->eg1Value < pSynth->pEAS->pArticulations[pWTVoice->artIndex].eg1.sustainLevel)
        return;

    /* sustain flag is set, damper pedal is on */
    /* defer releasing this note until the damper pedal is off */
    pWTVoice->eg1State = eEnvelopeStateDecay;
    pVoice->voiceState = eVoiceStatePlay;

    /*
    because sustain pedal is on, this voice
    should defer releasing its note
    */
    pVoice->voiceFlags |= VOICE_FLAG_SUSTAIN_PEDAL_DEFER_NOTE_OFF;

#ifdef _DEBUG_SYNTH
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "WT_SustainPedal: defer note off because sustain pedal is on\n"); */ }
#endif
}

/*----------------------------------------------------------------------------
 * WT_StartVoice()
 *----------------------------------------------------------------------------
 * Purpose:
 * Assign the region for the given instrument using the midi key number
 * and the RPN2 (coarse tuning) value. By using RPN2 as part of the
 * region selection process, we reduce the amount a given sample has
 * to be transposed by selecting the closest recorded root instead.
 *
 * This routine is the second half of SynthAssignRegion().
 * If the region was successfully found by SynthFindRegionIndex(),
 * then assign the region's parameters to the voice.
 *
 * Setup and initialize the following voice parameters:
 * m_nRegionIndex
 *
 * Inputs:
 * pVoice - ptr to the voice we have assigned for this channel
 * nRegionIndex - index of the region
 * pEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * success - could find and assign the region for this voice's note otherwise
 * failure - could not find nor assign the region for this voice's note
 *
 * Side Effects:
 * psSynthObject->m_sVoice[].m_nRegionIndex is assigned
 * psSynthObject->m_sVoice[] parameters are assigned
 *----------------------------------------------------------------------------
*/
static EAS_RESULT WT_StartVoice (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, EAS_I32 voiceNum, EAS_U16 regionIndex)
{
    S_WT_VOICE *pWTVoice;
    const S_WT_REGION *pRegion;
    const S_ARTICULATION *pArt;
    S_SYNTH_CHANNEL *pChannel;

#if (NUM_OUTPUT_CHANNELS == 2)
    EAS_INT pan;
#endif

#ifdef EAS_SPLIT_WT_SYNTH
    S_WT_CONFIG wtConfig;
#endif

    /* no samples have been synthesized for this note yet */
    pVoice->regionIndex = regionIndex;
    pVoice->voiceFlags = VOICE_FLAG_NO_SAMPLES_SYNTHESIZED_YET;

    /* get the articulation index for this region */
    pWTVoice = &pVoiceMgr->wtVoices[voiceNum];
    pChannel = &pSynth->channels[pVoice->channel & 15];

    /* update static channel parameters */
    if (pChannel->channelFlags & CHANNEL_FLAG_UPDATE_CHANNEL_PARAMETERS)
        WT_UpdateChannel(pVoiceMgr, pSynth, pVoice->channel & 15);

#ifdef DLS_SYNTHESIZER
    if (pVoice->regionIndex & FLAG_RGN_IDX_DLS_SYNTH)
        return DLS_StartVoice(pVoiceMgr, pSynth, pVoice, voiceNum, regionIndex);
#endif

    pRegion = &(pSynth->pEAS->pWTRegions[regionIndex]);
    pWTVoice->artIndex = pRegion->artIndex;

#ifdef _DEBUG_SYNTH
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "WT_StartVoice: Voice %ld; Region %d\n", (EAS_I32) (pVoice - pVoiceMgr->voices), regionIndex); */ }
#endif

    pArt = &pSynth->pEAS->pArticulations[pWTVoice->artIndex];

    /* MIDI note on puts this voice into attack state */
    pWTVoice->eg1State = eEnvelopeStateAttack;
    pWTVoice->eg1Value = 0;
    pWTVoice->eg1Increment = pArt->eg1.attackTime;
    pWTVoice->eg2State = eEnvelopeStateAttack;
    pWTVoice->eg2Value = 0;
    pWTVoice->eg2Increment = pArt->eg2.attackTime;

    /* init the LFO */
    pWTVoice->modLFO.lfoValue = 0;
    pWTVoice->modLFO.lfoPhase = -pArt->lfoDelay;

    pVoice->gain = 0;

#if (NUM_OUTPUT_CHANNELS == 2)
    /*
    Get the Midi CC10 pan value for this voice's channel
    convert the pan value to an "angle" representation suitable for
    our sin, cos calculator. This representation is NOT necessarily the same
    as the transform in the GM manuals because of our sin, cos calculator.
    "angle" = (CC10 - 64)/128
    */
    pan = (EAS_INT) pSynth->channels[pVoice->channel & 15].pan - 64;
    pan += pArt->pan;
    EAS_CalcPanControl(pan, &pWTVoice->gainLeft, &pWTVoice->gainRight);
#endif

#ifdef _FILTER_ENABLED
    /* clear out the filter states */
    pWTVoice->filter.z1 = 0;
    pWTVoice->filter.z2 = 0;
#endif

    /* if this wave is to be generated using noise generator */
    if (pRegion->region.keyGroupAndFlags & REGION_FLAG_USE_WAVE_GENERATOR)
    {
        pWTVoice->phaseAccum = 4574296;
        pWTVoice->loopStart = WT_NOISE_GENERATOR;
        pWTVoice->loopEnd = 4574295;
    }

    /* normal sample */
    else
    {

#ifdef EAS_SPLIT_WT_SYNTH
        if (voiceNum < NUM_PRIMARY_VOICES)
            pWTVoice->phaseAccum = (EAS_U32) pSynth->pEAS->pSamples + pSynth->pEAS->pSampleOffsets[pRegion->waveIndex];
        else
            pWTVoice->phaseAccum = pSynth->pEAS->pSampleOffsets[pRegion->waveIndex];
#else
        pWTVoice->phaseAccum = (EAS_U32) pSynth->pEAS->pSamples + pSynth->pEAS->pSampleOffsets[pRegion->waveIndex];
#endif

        if (pRegion->region.keyGroupAndFlags & REGION_FLAG_IS_LOOPED)
        {
#if defined (_8_BIT_SAMPLES)
            pWTVoice->loopStart = pWTVoice->phaseAccum + pRegion->loopStart;
            pWTVoice->loopEnd = pWTVoice->phaseAccum + pRegion->loopEnd - 1;
#else //_16_BIT_SAMPLES
            pWTVoice->loopStart = pWTVoice->phaseAccum + (pRegion->loopStart<<1);
            pWTVoice->loopEnd = pWTVoice->phaseAccum + (pRegion->loopEnd<<1) - 2;
#endif
        }
        else {
#if defined (_8_BIT_SAMPLES)
            pWTVoice->loopStart = pWTVoice->loopEnd = pWTVoice->phaseAccum + pSynth->pEAS->pSampleLen[pRegion->waveIndex] - 1;
#else //_16_BIT_SAMPLES
            pWTVoice->loopStart = pWTVoice->loopEnd = pWTVoice->phaseAccum + pSynth->pEAS->pSampleLen[pRegion->waveIndex] - 2;
#endif
        }
    }

#ifdef EAS_SPLIT_WT_SYNTH
    /* configure off-chip voices */
    if (voiceNum >= NUM_PRIMARY_VOICES)
    {
        wtConfig.phaseAccum = pWTVoice->phaseAccum;
        wtConfig.loopStart = pWTVoice->loopStart;
        wtConfig.loopEnd = pWTVoice->loopEnd;
        wtConfig.gain = pVoice->gain;

#if (NUM_OUTPUT_CHANNELS == 2)
        wtConfig.gainLeft = pWTVoice->gainLeft;
        wtConfig.gainRight = pWTVoice->gainRight;
#endif

        WTE_ConfigVoice(voiceNum - NUM_PRIMARY_VOICES, &wtConfig, pVoiceMgr->pFrameBuffer);
    }
#endif

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * WT_CheckSampleEnd
 *----------------------------------------------------------------------------
 * Purpose:
 * Check for end of sample and calculate number of samples to synthesize
 *
 * Inputs:
 *
 * Outputs:
 *
 * Notes:
 *
 *----------------------------------------------------------------------------
*/
EAS_BOOL WT_CheckSampleEnd (S_WT_VOICE *pWTVoice, S_WT_INT_FRAME *pWTIntFrame, EAS_BOOL update)
{
    EAS_U32 endPhaseAccum;
    EAS_U32 endPhaseFrac;
    EAS_I32 numSamples;
    EAS_BOOL done = EAS_FALSE;

    /* check to see if we hit the end of the waveform this time */
    /*lint -e{703} use shift for performance */
    endPhaseFrac = pWTVoice->phaseFrac + (pWTIntFrame->frame.phaseIncrement << SYNTH_UPDATE_PERIOD_IN_BITS);
#if defined (_8_BIT_SAMPLES)
    endPhaseAccum = pWTVoice->phaseAccum + GET_PHASE_INT_PART(endPhaseFrac);
#else //_16_BIT_SAMPLES
    // Multiply by 2 for 16 bit processing module implementation
    endPhaseAccum = pWTVoice->phaseAccum + (EAS_U32)(endPhaseFrac >> 14);
#endif
    if (endPhaseAccum >= pWTVoice->loopEnd)
    {
        /* calculate how far current ptr is from end */
        numSamples = (EAS_I32) (pWTVoice->loopEnd - pWTVoice->phaseAccum);
#if defined (_16_BIT_SAMPLES)
        numSamples >>= 1;        // Divide by 2 for 16 bit processing module implementation
#endif
        /* now account for the fractional portion */
        /*lint -e{703} use shift for performance */
        numSamples = (numSamples << NUM_PHASE_FRAC_BITS) - (EAS_I32) pWTVoice->phaseFrac;
        if (pWTIntFrame->frame.phaseIncrement) {
            pWTIntFrame->numSamples = 1 + (numSamples / pWTIntFrame->frame.phaseIncrement);
        } else {
            pWTIntFrame->numSamples = numSamples;
        }
        if (pWTIntFrame->numSamples < 0) {
            ALOGE("b/26366256");
            android_errorWriteLog(0x534e4554, "26366256");
            pWTIntFrame->numSamples = 0;
        }

        /* sound will be done this frame */
        done = EAS_TRUE;
    }

    /* update data for off-chip synth */
    if (update)
    {
        pWTVoice->phaseFrac = endPhaseFrac;
        pWTVoice->phaseAccum = endPhaseAccum;
    }

    return done;
}

/*----------------------------------------------------------------------------
 * WT_UpdateVoice()
 *----------------------------------------------------------------------------
 * Purpose:
 * Synthesize a block of samples for the given voice.
 * Use linear interpolation.
 *
 * Inputs:
 * pEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * number of samples actually written to buffer
 *
 * Side Effects:
 * - samples are added to the presently free buffer
 *
 *----------------------------------------------------------------------------
*/
static EAS_BOOL WT_UpdateVoice (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, EAS_I32 voiceNum, EAS_I32 *pMixBuffer, EAS_I32  numSamples)
{
    S_WT_VOICE *pWTVoice;
    S_WT_INT_FRAME intFrame;
    S_SYNTH_CHANNEL *pChannel;
    const S_WT_REGION *pWTRegion;
    const S_ARTICULATION *pArt;
    EAS_I32 temp;
    EAS_BOOL done;

#ifdef DLS_SYNTHESIZER
    if (pVoice->regionIndex & FLAG_RGN_IDX_DLS_SYNTH)
        return DLS_UpdateVoice(pVoiceMgr, pSynth, pVoice, voiceNum, pMixBuffer, numSamples);
#endif
    /* establish pointers to critical data */
    pWTVoice = &pVoiceMgr->wtVoices[voiceNum];
    pWTRegion = &pSynth->pEAS->pWTRegions[pVoice->regionIndex & REGION_INDEX_MASK];
    pArt = &pSynth->pEAS->pArticulations[pWTVoice->artIndex];
    pChannel = &pSynth->channels[pVoice->channel & 15];
    intFrame.prevGain = pVoice->gain;

    /* update the envelopes */
    WT_UpdateEG1(pWTVoice, &pArt->eg1);
    WT_UpdateEG2(pWTVoice, &pArt->eg2);

    /* update the LFO */
    WT_UpdateLFO(&pWTVoice->modLFO, pArt->lfoFreq);

#ifdef _FILTER_ENABLED
    /* calculate filter if library uses filter */
    if (pSynth->pEAS->libAttr & LIB_FORMAT_FILTER_ENABLED)
        WT_UpdateFilter(pWTVoice, &intFrame, pArt);
    else
        intFrame.frame.k = 0;
#endif

    /* update the gain */
    intFrame.frame.gainTarget = WT_UpdateGain(pVoice, pWTVoice, pArt, pChannel, pWTRegion->gain);

    /* calculate base pitch*/
    temp = pChannel->staticPitch + pWTRegion->tuning;

    /* include global transpose */
    if (pChannel->channelFlags & CHANNEL_FLAG_RHYTHM_CHANNEL)
        temp += pVoice->note * 100;
    else
        temp += (pVoice->note + pSynth->globalTranspose) * 100;
    intFrame.frame.phaseIncrement = WT_UpdatePhaseInc(pWTVoice, pArt, pChannel, temp);
    if (pWTVoice->loopStart == WT_NOISE_GENERATOR) {
        temp = 0;
    } else {
        temp = pWTVoice->loopEnd - pWTVoice->loopStart;
    }
#ifdef _16_BIT_SAMPLES
    temp >>= 1;
#endif
    if (temp != 0) {
        temp = temp << NUM_PHASE_FRAC_BITS;
        if (intFrame.frame.phaseIncrement > temp) {
            ALOGW("%p phaseIncrement=%d", pWTVoice, (int)intFrame.frame.phaseIncrement);
            intFrame.frame.phaseIncrement %= temp;
        }
    }

    /* call into engine to generate samples */
    intFrame.pAudioBuffer = pVoiceMgr->voiceBuffer;
    intFrame.pMixBuffer = pMixBuffer;
    intFrame.numSamples = numSamples;

    /* check for end of sample */
    if ((pWTVoice->loopStart != WT_NOISE_GENERATOR) && (pWTVoice->loopStart == pWTVoice->loopEnd))
        done = WT_CheckSampleEnd(pWTVoice, &intFrame, (EAS_BOOL) (voiceNum >= NUM_PRIMARY_VOICES));
    else
        done = EAS_FALSE;

    if (intFrame.numSamples < 0) intFrame.numSamples = 0;

    if (intFrame.numSamples > BUFFER_SIZE_IN_MONO_SAMPLES)
        intFrame.numSamples = BUFFER_SIZE_IN_MONO_SAMPLES;

#ifdef EAS_SPLIT_WT_SYNTH
    if (voiceNum < NUM_PRIMARY_VOICES)
    {
#ifndef _SPLIT_WT_TEST_HARNESS
        WT_ProcessVoice(pWTVoice, &intFrame);
#endif
    }
    else
        WTE_ProcessVoice(voiceNum - NUM_PRIMARY_VOICES, &intFrame.frame, pVoiceMgr->pFrameBuffer);
#else
    WT_ProcessVoice(pWTVoice, &intFrame);
#endif

    /* clear flag */
    pVoice->voiceFlags &= ~VOICE_FLAG_NO_SAMPLES_SYNTHESIZED_YET;

    /* if voice has finished, set flag for voice manager */
    if ((pVoice->voiceState != eVoiceStateStolen) && (pWTVoice->eg1State == eEnvelopeStateMuted))
        done = EAS_TRUE;

    /* if the update interval has elapsed, then force the current gain to the next
     * gain since we never actually reach the next gain when ramping -- we just get
     * very close to the target gain.
     */
    pVoice->gain = (EAS_I16) intFrame.frame.gainTarget;

    return done;
}

/*----------------------------------------------------------------------------
 * WT_UpdatePhaseInc()
 *----------------------------------------------------------------------------
 * Purpose:
 * Calculate the phase increment
 *
 * Inputs:
 * pVoice - pointer to the voice being updated
 * psRegion - pointer to the region
 * psArticulation - pointer to the articulation
 * nChannelPitchForThisVoice - the portion of the pitch that is fixed for this
 *                  voice during the duration of this synthesis
 * pEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 * set the phase increment for this voice
 *----------------------------------------------------------------------------
*/
static EAS_I32 WT_UpdatePhaseInc (S_WT_VOICE *pWTVoice, const S_ARTICULATION *pArt, S_SYNTH_CHANNEL *pChannel, EAS_I32 pitchCents)
{
    EAS_I32 temp;

    /*pitchCents due to CC1 = LFO * (CC1 / 128) * DEFAULT_LFO_MOD_WHEEL_TO_PITCH_CENTS */
    temp = MULT_EG1_EG1(DEFAULT_LFO_MOD_WHEEL_TO_PITCH_CENTS,
        ((pChannel->modWheel) << (NUM_EG1_FRAC_BITS -7)));

    /* pitchCents due to channel pressure = LFO * (channel pressure / 128) * DEFAULT_LFO_CHANNEL_PRESSURE_TO_PITCH_CENTS */
    temp += MULT_EG1_EG1(DEFAULT_LFO_CHANNEL_PRESSURE_TO_PITCH_CENTS,
         ((pChannel->channelPressure) << (NUM_EG1_FRAC_BITS -7)));

    /* now multiply the (channel pressure + CC1) pitch values by the LFO value */
    temp = MULT_EG1_EG1(pWTVoice->modLFO.lfoValue, temp);

    /*
    add in the LFO pitch due to
    channel pressure and CC1 along with
    the LFO pitch, the EG2 pitch, and the
    "static" pitch for this voice on this channel
    */
    temp += pitchCents +
        (MULT_EG1_EG1(pWTVoice->eg2Value, pArt->eg2ToPitch)) +
        (MULT_EG1_EG1(pWTVoice->modLFO.lfoValue, pArt->lfoToPitch));

    /* convert from cents to linear phase increment */
    return EAS_Calculate2toX(temp);
}

/*----------------------------------------------------------------------------
 * WT_UpdateChannel()
 *----------------------------------------------------------------------------
 * Purpose:
 * Calculate and assign static channel parameters
 * These values only need to be updated if one of the controller values
 * for this channel changes
 *
 * Inputs:
 * nChannel - channel to update
 * pEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 * - the given channel's static gain and static pitch are updated
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pVoiceMgr) reserved for future use */
static void WT_UpdateChannel (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel)
{
    EAS_I32 staticGain;
    EAS_I32 pitchBend;
    S_SYNTH_CHANNEL *pChannel;

    pChannel = &pSynth->channels[channel];

    /*
    nChannelGain = (CC7 * CC11)^2  * master volume
    where CC7 == 100 by default, CC11 == 127, master volume == 32767
    */
    staticGain = MULT_EG1_EG1((pChannel->volume) << (NUM_EG1_FRAC_BITS - 7),
        (pChannel->expression) << (NUM_EG1_FRAC_BITS - 7));

    /* staticGain has to be squared */
    staticGain = MULT_EG1_EG1(staticGain, staticGain);

    pChannel->staticGain = (EAS_I16) MULT_EG1_EG1(staticGain, pSynth->masterVolume);

    /*
    calculate pitch bend: RPN0 * ((2*pitch wheel)/16384  -1)
    However, if we use the EG1 macros, remember that EG1 has a full
    scale value of 32768 (instead of 16384). So instead of multiplying
    by 2, multiply by 4 (left shift by 2), and subtract by 32768 instead
    of 16384. This utilizes the fact that the EG1 macro places a binary
    point 15 places to the left instead of 14 places.
    */
    /*lint -e{703} <avoid multiply for performance>*/
    pitchBend =
        (((EAS_I32)(pChannel->pitchBend) << 2)
        - 32768);

    pChannel->staticPitch =
        MULT_EG1_EG1(pitchBend, pChannel->pitchBendSensitivity);

    /* if this is not a drum channel, then add in the per-channel tuning */
    if (!(pChannel->channelFlags & CHANNEL_FLAG_RHYTHM_CHANNEL))
        pChannel->staticPitch += pChannel->finePitch + (pChannel->coarsePitch * 100);

    /* clear update flag */
    pChannel->channelFlags &= ~CHANNEL_FLAG_UPDATE_CHANNEL_PARAMETERS;
    return;
}

/*----------------------------------------------------------------------------
 * WT_UpdateGain()
 *----------------------------------------------------------------------------
 * Purpose:
 * Calculate and assign static voice parameters as part of WT_UpdateVoice()
 *
 * Inputs:
 * pVoice - ptr to the synth voice that we want to synthesize
 * pEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 * - various voice parameters are calculated and assigned
 *
 *----------------------------------------------------------------------------
*/
static EAS_I32 WT_UpdateGain (S_SYNTH_VOICE *pVoice, S_WT_VOICE *pWTVoice, const S_ARTICULATION *pArt, S_SYNTH_CHANNEL *pChannel, EAS_I32 gain)
{
    EAS_I32 lfoGain;
    EAS_I32 temp;

    /*
    If this voice was stolen, then the velocity is actually
    for the new note, not the note that we are currently ramping down.
    So we really shouldn't use this velocity. However, that would require
    more memory to store the velocity value, and the improvement may
    not be sufficient to warrant the added memory.
    */
    /* velocity is fixed at note start for a given voice and must be squared */
    temp = (pVoice->velocity) << (NUM_EG1_FRAC_BITS - 7);
    temp = MULT_EG1_EG1(temp, temp);

    /* region gain is fixed as part of the articulation */
    temp = MULT_EG1_EG1(temp, gain);

    /* include the channel gain */
    temp = MULT_EG1_EG1(temp, pChannel->staticGain);

    /* calculate LFO gain using an approximation for 10^x */
    lfoGain = MULT_EG1_EG1(pWTVoice->modLFO.lfoValue, pArt->lfoToGain);
    lfoGain = MULT_EG1_EG1(lfoGain, LFO_GAIN_TO_CENTS);

    /* convert from a dB-like value to linear gain */
    lfoGain = EAS_Calculate2toX(lfoGain);
    temp = MULT_EG1_EG1(temp, lfoGain);

    /* calculate the voice's gain */
    temp = (EAS_I16)MULT_EG1_EG1(temp, pWTVoice->eg1Value);

    return temp;
}

/*----------------------------------------------------------------------------
 * WT_UpdateEG1()
 *----------------------------------------------------------------------------
 * Purpose:
 * Calculate the EG1 envelope for the given voice (but do not update any
 * state)
 *
 * Inputs:
 * pVoice - ptr to the voice whose envelope we want to update
 * nVoice - this voice's number - used only for debug
 * pEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * nValue - the envelope value
 *
 * Side Effects:
 * - updates EG1 state value for the given voice
 *----------------------------------------------------------------------------
*/
static void WT_UpdateEG1 (S_WT_VOICE *pWTVoice, const S_ENVELOPE *pEnv)
{
    EAS_I32 temp;

    switch (pWTVoice->eg1State)
    {
        case eEnvelopeStateAttack:
            temp = pWTVoice->eg1Value + pWTVoice->eg1Increment;

            /* check if we have reached peak amplitude */
            if (temp >= SYNTH_FULL_SCALE_EG1_GAIN)
            {
                /* limit the volume */
                temp = SYNTH_FULL_SCALE_EG1_GAIN;

                /* prepare to move to decay state */
                pWTVoice->eg1State = eEnvelopeStateDecay;
                pWTVoice->eg1Increment = pEnv->decayTime;
            }

            break;

        /* exponential decay */
        case eEnvelopeStateDecay:
            temp = MULT_EG1_EG1(pWTVoice->eg1Value, pWTVoice->eg1Increment);

            /* check if we have reached sustain level */
            if (temp <= pEnv->sustainLevel)
            {
                /* enforce the sustain level */
                temp = pEnv->sustainLevel;

                /* if sustain level is zero, skip sustain & release the voice */
                if (temp > 0)
                    pWTVoice->eg1State = eEnvelopeStateSustain;

                /* move to sustain state */
                else
                    pWTVoice->eg1State = eEnvelopeStateMuted;
            }

            break;

        case eEnvelopeStateSustain:
            return;

        case eEnvelopeStateRelease:
            temp = MULT_EG1_EG1(pWTVoice->eg1Value, pWTVoice->eg1Increment);

            /* if we hit zero, this voice isn't contributing any audio */
            if (temp <= 0)
            {
                temp = 0;
                pWTVoice->eg1State = eEnvelopeStateMuted;
            }
            break;

        /* voice is muted, set target to zero */
        case eEnvelopeStateMuted:
            temp = 0;
            break;

        case eEnvelopeStateInvalid:
        default:
            temp = 0;
#ifdef  _DEBUG_SYNTH
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "WT_UpdateEG1: error, %d is an unrecognized state\n",
                pWTVoice->eg1State); */ }
#endif
            break;

    }

    pWTVoice->eg1Value = (EAS_I16) temp;
}

/*----------------------------------------------------------------------------
 * WT_UpdateEG2()
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the EG2 envelope for the given voice
 *
 * Inputs:
 * pVoice - ptr to the voice whose envelope we want to update
 * pEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 * - updates EG2 values for the given voice
 *----------------------------------------------------------------------------
*/

static void WT_UpdateEG2 (S_WT_VOICE *pWTVoice, const S_ENVELOPE *pEnv)
{
    EAS_I32 temp;

    switch (pWTVoice->eg2State)
    {
        case eEnvelopeStateAttack:
            temp = pWTVoice->eg2Value + pWTVoice->eg2Increment;

            /* check if we have reached peak amplitude */
            if (temp >= SYNTH_FULL_SCALE_EG1_GAIN)
            {
                /* limit the volume */
                temp = SYNTH_FULL_SCALE_EG1_GAIN;

                /* prepare to move to decay state */
                pWTVoice->eg2State = eEnvelopeStateDecay;

                pWTVoice->eg2Increment = pEnv->decayTime;
            }

            break;

            /* implement linear pitch decay in cents */
        case eEnvelopeStateDecay:
            temp = pWTVoice->eg2Value -pWTVoice->eg2Increment;

            /* check if we have reached sustain level */
            if (temp <= pEnv->sustainLevel)
            {
                /* enforce the sustain level */
                temp = pEnv->sustainLevel;

                /* prepare to move to sustain state */
                pWTVoice->eg2State = eEnvelopeStateSustain;
            }
            break;

        case eEnvelopeStateSustain:
            return;

        case eEnvelopeStateRelease:
            temp = pWTVoice->eg2Value - pWTVoice->eg2Increment;

            if (temp <= 0)
            {
                temp = 0;
                pWTVoice->eg2State = eEnvelopeStateMuted;
            }

            break;

        /* voice is muted, set target to zero */
        case eEnvelopeStateMuted:
            temp = 0;
            break;

        case eEnvelopeStateInvalid:
        default:
            temp = 0;
#ifdef  _DEBUG_SYNTH
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "WT_UpdateEG2: error, %d is an unrecognized state\n",
                pWTVoice->eg2State); */ }
#endif
            break;
    }

    pWTVoice->eg2Value = (EAS_I16) temp;
}

/*----------------------------------------------------------------------------
 * WT_UpdateLFO ()
 *----------------------------------------------------------------------------
 * Purpose:
 * Calculate the LFO for the given voice
 *
 * Inputs:
 * pLFO         - ptr to the LFO data
 * phaseInc     - phase increment
 *
 * Outputs:
 *
 * Side Effects:
 * - updates LFO values for the given voice
 *----------------------------------------------------------------------------
*/
void WT_UpdateLFO (S_LFO_CONTROL *pLFO, EAS_I16 phaseInc)
{

    /* To save memory, if m_nPhaseValue is negative, we are in the
     * delay phase, and m_nPhaseValue represents the time left
     * in the delay.
     */
     if (pLFO->lfoPhase < 0)
     {
        pLFO->lfoPhase++;
        return;
     }

    /* calculate LFO output from phase value */
    /*lint -e{701} Use shift for performance */
    pLFO->lfoValue = (EAS_I16) (pLFO->lfoPhase << 2);
    /*lint -e{502} <shortcut to turn sawtooth into triangle wave> */
    if ((pLFO->lfoPhase > 0x1fff) && (pLFO->lfoPhase < 0x6000))
        pLFO->lfoValue = ~pLFO->lfoValue;

    /* update LFO phase */
    pLFO->lfoPhase = (pLFO->lfoPhase + phaseInc) & 0x7fff;
}

#ifdef _FILTER_ENABLED
/*----------------------------------------------------------------------------
 * WT_UpdateFilter()
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the Filter parameters
 *
 * Inputs:
 * pVoice - ptr to the voice whose filter we want to update
 * pEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 * - updates Filter values for the given voice
 *----------------------------------------------------------------------------
*/
static void WT_UpdateFilter (S_WT_VOICE *pWTVoice, S_WT_INT_FRAME *pIntFrame, const S_ARTICULATION *pArt)
{
    EAS_I32 cutoff;

    /* no need to calculate filter coefficients if it is bypassed */
    if (pArt->filterCutoff == DEFAULT_EAS_FILTER_CUTOFF_FREQUENCY)
    {
        pIntFrame->frame.k = 0;
        return;
    }

    /* determine the dynamic cutoff frequency */
    cutoff = MULT_EG1_EG1(pWTVoice->eg2Value, pArt->eg2ToFc);
    cutoff += pArt->filterCutoff;

    /* subtract the A5 offset and the sampling frequency */
    cutoff -= FILTER_CUTOFF_FREQ_ADJUST + A5_PITCH_OFFSET_IN_CENTS;

    /* limit the cutoff frequency */
    if (cutoff > FILTER_CUTOFF_MAX_PITCH_CENTS)
        cutoff = FILTER_CUTOFF_MAX_PITCH_CENTS;
    else if (cutoff < FILTER_CUTOFF_MIN_PITCH_CENTS)
        cutoff = FILTER_CUTOFF_MIN_PITCH_CENTS;

    WT_SetFilterCoeffs(pIntFrame, cutoff, pArt->filterQ);
}
#endif

#if defined(_FILTER_ENABLED) || defined(DLS_SYNTHESIZER)
/*----------------------------------------------------------------------------
 * coef
 *----------------------------------------------------------------------------
 * Table of filter coefficients for low-pass filter
 *----------------------------------------------------------------------------
 *
 * polynomial coefficients are based on 8kHz sampling frequency
 * filter coef b2 = k2 = k2g0*k^0 + k2g1*k^1*(2^x) + k2g2*k^2*(2^x)
 *
 *where k2g0, k2g1, k2g2 are from the truncated power series expansion on theta
 *(k*2^x = theta, but we incorporate the k along with the k2g0, k2g1, k2g2)
 *note: this is a power series in 2^x, not k*2^x
 *where k = (2*pi*440)/8kHz == convert octaves to radians
 *
 *  so actually, the following coefs listed as k2g0, k2g1, k2g2 are really
 *  k2g0*k^0 = k2g0
 *  k2g1*k^1
 *  k2g2*k^2
 *
 *
 * filter coef n1 = numerator = n1g0*k^0 + n1g1*k^1*(2^x) + n1g2*k^2*(2^x) + n1g3*k^3*(2^x)
 *
 *where n1g0, n1g1, n1g2, n1g3 are from the truncated power series expansion on theta
 *(k*2^x = theta, but we incorporate the k along with the n1g0, n1g1, n1g2, n2g3)
 *note: this is a power series in 2^x, not k*2^x
 *where k = (2*pi*440)/8kHz == convert octaves to radians
 *we also include the optimization factor of 0.81
 *
 *  so actually, the following coefs listed as n1g0, n1g1, n1g2, n2g3 are really
 *  n1g0*k^0 = n1g0
 *  n1g1*k^1
 *  n1g2*k^2
 *  n1g3*k^3
 *
 *  NOTE that n1g0 == n1g1 == 0, always, so we only need to store n1g2 and n1g3
 *----------------------------------------------------------------------------
*/

static const EAS_I16 nk1g0 = -32768;
static const EAS_I16 nk1g2 = 1580;
static const EAS_I16 k2g0 = 32767;

static const EAS_I16 k2g1[] =
{
        -11324, /* k2g1[0] = -0.3455751918948761 */
        -10387, /* k2g1[1] = -0.3169878073928751 */
        -9528,  /* k2g1[2] = -0.29076528753345476 */
        -8740,  /* k2g1[3] = -0.2667120011011279 */
        -8017,  /* k2g1[4] = -0.24464850028971705 */
        -7353,  /* k2g1[5] = -0.22441018194495696 */
        -6745,  /* k2g1[6] = -0.20584605955455101 */
        -6187,  /* k2g1[7] = -0.18881763682420102 */
        -5675,  /* k2g1[8] = -0.1731978744360067 */
        -5206,  /* k2g1[9] = -0.15887024228080968 */
        -4775,  /* k2g1[10] = -0.14572785009373057 */
        -4380,  /* k2g1[11] = -0.13367265000706827 */
        -4018,  /* k2g1[12] = -0.1226147050712642 */
        -3685,  /* k2g1[13] = -0.11247151828678581 */
        -3381,  /* k2g1[14] = -0.10316741714122014 */
        -3101,  /* k2g1[15] = -0.0946329890599603 */
        -2844,  /* k2g1[16] = -0.08680456355870586 */
        -2609,  /* k2g1[17] = -0.07962373723441349 */
        -2393,  /* k2g1[18] = -0.07303693805092666 */
        -2195,  /* k2g1[19] = -0.06699502566866912 */
        -2014,  /* k2g1[20] = -0.06145292483669077 */
        -1847,  /* k2g1[21] = -0.056369289112013346 */
        -1694,  /* k2g1[22] = -0.05170619239747895 */
        -1554,  /* k2g1[23] = -0.04742884599684141 */
        -1426,  /* k2g1[24] = -0.043505339076210514 */
        -1308,  /* k2g1[25] = -0.03990640059558053 */
        -1199,  /* k2g1[26] = -0.03660518093435039 */
        -1100,  /* k2g1[27] = -0.03357705158166837 */
        -1009,  /* k2g1[28] = -0.030799421397205727 */
        -926,   /* k2g1[29] = -0.028251568071585884 */
        -849    /* k2g1[30] = -0.025914483529091967 */
};

static const EAS_I16 k2g2[] =
{
        1957,   /* k2g2[0] = 0.059711106626580836 */
        1646,   /* k2g2[1] = 0.05024063501786333 */
        1385,   /* k2g2[2] = 0.042272226217199664 */
        1165,   /* k2g2[3] = 0.03556764576567844 */
        981,    /* k2g2[4] = 0.029926444346999134 */
        825,    /* k2g2[5] = 0.025179964880280382 */
        694,    /* k2g2[6] = 0.02118630011706455 */
        584,    /* k2g2[7] = 0.01782604998793514 */
        491,    /* k2g2[8] = 0.014998751854573014 */
        414,    /* k2g2[9] = 0.012619876941179595 */
        348,    /* k2g2[10] = 0.010618303146468736 */
        293,    /* k2g2[11] = 0.008934188679954682 */
        246,    /* k2g2[12] = 0.007517182949855368 */
        207,    /* k2g2[13] = 0.006324921212866403 */
        174,    /* k2g2[14] = 0.005321757979794424 */
        147,    /* k2g2[15] = 0.004477701309210577 */
        123,    /* k2g2[16] = 0.00376751612730811 */
        104,    /* k2g2[17] = 0.0031699697655869644 */
        87,     /* k2g2[18] = 0.00266719715992703 */
        74,     /* k2g2[19] = 0.0022441667321724647 */
        62,     /* k2g2[20] = 0.0018882309854916855 */
        52,     /* k2g2[21] = 0.0015887483774966232 */
        44,     /* k2g2[22] = 0.0013367651661223448 */
        37,     /* k2g2[23] = 0.0011247477162958733 */
        31,     /* k2g2[24] = 0.0009463572640678758 */
        26,     /* k2g2[25] = 0.0007962604042473498 */
        22,     /* k2g2[26] = 0.0006699696356181593 */
        18,     /* k2g2[27] = 0.0005637091964589207 */
        16,     /* k2g2[28] = 0.00047430217920125243 */
        13,     /* k2g2[29] = 0.00039907554925166274 */
        11      /* k2g2[30] = 0.00033578022828973666 */
};

static const EAS_I16 n1g2[] =
{
        3170,   /* n1g2[0] = 0.0967319927350769 */
        3036,   /* n1g2[1] = 0.0926446051254155 */
        2908,   /* n1g2[2] = 0.08872992911818503 */
        2785,   /* n1g2[3] = 0.08498066682523227 */
        2667,   /* n1g2[4] = 0.08138982872895201 */
        2554,   /* n1g2[5] = 0.07795072065216213 */
        2446,   /* n1g2[6] = 0.0746569312785634 */
        2343,   /* n1g2[7] = 0.07150232020051943 */
        2244,   /* n1g2[8] = 0.06848100647187474 */
        2149,   /* n1g2[9] = 0.06558735764447099 */
        2058,   /* n1g2[10] = 0.06281597926792246 */
        1971,   /* n1g2[11] = 0.06016170483307614 */
        1888,   /* n1g2[12] = 0.05761958614040857 */
        1808,   /* n1g2[13] = 0.05518488407540374 */
        1732,   /* n1g2[14] = 0.052853059773715245 */
        1659,   /* n1g2[15] = 0.05061976615964251 */
        1589,   /* n1g2[16] = 0.04848083984214659 */
        1521,   /* n1g2[17] = 0.046432293353298 */
        1457,   /* n1g2[18] = 0.04447030771468711 */
        1396,   /* n1g2[19] = 0.04259122531793907 */
        1337,   /* n1g2[20] = 0.040791543106060944 */
        1280,   /* n1g2[21] = 0.03906790604290942 */
        1226,   /* n1g2[22] = 0.037417100858604564 */
        1174,   /* n1g2[23] = 0.035836050059229754 */
        1125,   /* n1g2[24] = 0.03432180618965023 */
        1077,   /* n1g2[25] = 0.03287154633875494 */
        1032,   /* n1g2[26] = 0.03148256687687814 */
        988,    /* n1g2[27] = 0.030152278415589925 */
        946,    /* n1g2[28] = 0.028878200980459685 */
        906,    /* n1g2[29] = 0.02765795938779331 */
        868     /* n1g2[30] = 0.02648927881672521 */
};

static const EAS_I16 n1g3[] =
{
        -548,   /* n1g3[0] = -0.016714088475899017 */
        -481,   /* n1g3[1] = -0.014683605122742116 */
        -423,   /* n1g3[2] = -0.012899791676436092 */
        -371,   /* n1g3[3] = -0.01133268185193299 */
        -326,   /* n1g3[4] = -0.00995594976868754 */
        -287,   /* n1g3[5] = -0.008746467702146129 */
        -252,   /* n1g3[6] = -0.00768391756106361 */
        -221,   /* n1g3[7] = -0.006750449563854721 */
        -194,   /* n1g3[8] = -0.005930382380083576 */
        -171,   /* n1g3[9] = -0.005209939699767622 */
        -150,   /* n1g3[10] = -0.004577018805123356 */
        -132,   /* n1g3[11] = -0.004020987256990177 */
        -116,   /* n1g3[12] = -0.003532504280467257 */
        -102,   /* n1g3[13] = -0.00310336384922047 */
        -89,    /* n1g3[14] = -0.002726356832432369 */
        -78,    /* n1g3[15] = -0.002395149888601605 */
        -69,    /* n1g3[16] = -0.0021041790717285314 */
        -61,    /* n1g3[17] = -0.0018485563625771063 */
        -53,    /* n1g3[18] = -0.001623987554831628 */
        -47,    /* n1g3[19] = -0.0014267001167177025 */
        -41,    /* n1g3[20] = -0.0012533798162347005 */
        -36,    /* n1g3[21] = -0.0011011150453668693 */
        -32,    /* n1g3[22] = -0.0009673479079754438 */
        -28,    /* n1g3[23] = -0.0008498312496971563 */
        -24,    /* n1g3[24] = -0.0007465909079943587 */
        -21,    /* n1g3[25] = -0.0006558925481952733 */
        -19,    /* n1g3[26] = -0.0005762125284029567 */
        -17,    /* n1g3[27] = -0.0005062123038325457 */
        -15,    /* n1g3[28] = -0.0004447159405951901 */
        -13,    /* n1g3[29] = -0.00039069036118270117 */
        -11     /* n1g3[30] = -0.00034322798979677605 */
};

/*----------------------------------------------------------------------------
 * WT_SetFilterCoeffs()
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the Filter parameters
 *
 * Inputs:
 * pVoice - ptr to the voice whose filter we want to update
 * pEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 * - updates Filter values for the given voice
 *----------------------------------------------------------------------------
*/
void WT_SetFilterCoeffs (S_WT_INT_FRAME *pIntFrame, EAS_I32 cutoff, EAS_I32 resonance)
{
    EAS_I32 temp;

    /*
    Convert the cutoff, which has had A5 subtracted, using the 2^x approx
    Note, this cutoff is related to theta cutoff by
    theta = k * 2^x
    We use 2^x and incorporate k in the power series coefs instead
    */
    cutoff = EAS_Calculate2toX(cutoff);

    /* calculate b2 coef */
    temp = k2g1[resonance] + MULT_AUDIO_COEF(cutoff, k2g2[resonance]);
    temp = k2g0 + MULT_AUDIO_COEF(cutoff, temp);
    pIntFrame->frame.b2 = temp;

    /* calculate b1 coef */
    temp = MULT_AUDIO_COEF(cutoff, nk1g2);
    temp = nk1g0 + MULT_AUDIO_COEF(cutoff, temp);
    temp += MULT_AUDIO_COEF(temp, pIntFrame->frame.b2);
    pIntFrame->frame.b1 = temp >> 1;

    /* calculate K coef */
    temp = n1g2[resonance] + MULT_AUDIO_COEF(cutoff, n1g3[resonance]);
    temp = MULT_AUDIO_COEF(cutoff, temp);
    temp = MULT_AUDIO_COEF(cutoff, temp);
    pIntFrame->frame.k = temp;
}
#endif

