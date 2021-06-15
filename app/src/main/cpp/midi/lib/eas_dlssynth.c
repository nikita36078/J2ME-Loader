/*----------------------------------------------------------------------------
 *
 * File:
 * eas_dlssynth.c
 *
 * Contents and purpose:
 * Implements the Mobile DLS synthesizer.
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
 *   $Revision: 795 $
 *   $Date: 2007-08-01 00:14:45 -0700 (Wed, 01 Aug 2007) $
 *----------------------------------------------------------------------------
*/

// includes
#include "eas_data.h"
#include "eas_report.h"
#include "eas_host.h"
#include "eas_math.h"
#include "eas_synth_protos.h"
#include "eas_wtsynth.h"
#include "eas_pan.h"
#include "eas_mdls.h"
#include "eas_dlssynth.h"

#ifdef _METRICS_ENABLED
#include "eas_perf.h"
#endif

static void DLS_UpdateEnvelope (S_SYNTH_VOICE *pVoice, S_SYNTH_CHANNEL *pChannel,  const S_DLS_ENVELOPE *pEnvParams, EAS_I16 *pValue, EAS_I16 *pIncrement, EAS_U8 *pState);

/*----------------------------------------------------------------------------
 * DLS_MuteVoice()
 *----------------------------------------------------------------------------
 * Mute the voice using shutdown time from the DLS articulation data
 *----------------------------------------------------------------------------
*/
void DLS_MuteVoice (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, EAS_I32 voiceNum)
{
    S_WT_VOICE *pWTVoice;
    const S_DLS_ARTICULATION *pDLSArt;

    pWTVoice = &pVoiceMgr->wtVoices[voiceNum];
    pDLSArt = &pSynth->pDLS->pDLSArticulations[pWTVoice->artIndex];

    /* clear deferred action flags */
    pVoice->voiceFlags &=
        ~(VOICE_FLAG_DEFER_MIDI_NOTE_OFF |
        VOICE_FLAG_SUSTAIN_PEDAL_DEFER_NOTE_OFF |
        VOICE_FLAG_DEFER_MUTE);

    /* set the envelope state */
    pVoiceMgr->wtVoices[voiceNum].eg1State = eEnvelopeStateRelease;
    pWTVoice->eg1Increment = pDLSArt->eg1ShutdownTime;
    pVoiceMgr->wtVoices[voiceNum].eg2State = eEnvelopeStateRelease;
    pWTVoice->eg2Increment = pDLSArt->eg2.releaseTime;
}

/*----------------------------------------------------------------------------
 * DLS_ReleaseVoice()
 *----------------------------------------------------------------------------
 * Release the selected voice.
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pVoice) standard API, pVoice may be used by other synthesizers */
void DLS_ReleaseVoice (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, EAS_I32 voiceNum)
{
    S_WT_VOICE *pWTVoice;
    const S_DLS_ARTICULATION *pDLSArt;

    pWTVoice = &pVoiceMgr->wtVoices[voiceNum];
    pDLSArt = &pSynth->pDLS->pDLSArticulations[pWTVoice->artIndex];

    /* if still in attack phase, convert units to log */
    /*lint -e{732} eg1Value is never negative */
    /*lint -e{703} use shift for performance */
    if (pWTVoice->eg1State == eEnvelopeStateAttack)
        pWTVoice->eg1Value = (EAS_I16) ((EAS_flog2(pWTVoice->eg1Value) << 1) + 2048);

    /* release EG1 */
    pWTVoice->eg1State = eEnvelopeStateRelease;
    pWTVoice->eg1Increment = pDLSArt->eg1.releaseTime;

    /* release EG2 */
    pWTVoice->eg2State = eEnvelopeStateRelease;
    pWTVoice->eg2Increment = pDLSArt->eg2.releaseTime;
}

/*----------------------------------------------------------------------------
 * DLS_SustainPedal()
 *----------------------------------------------------------------------------
 * The sustain pedal was just depressed. If the voice is still
 * above the sustain level, catch the voice and continue holding.
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pChannel) pChannel reserved for future use */
void DLS_SustainPedal (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, S_SYNTH_CHANNEL *pChannel, EAS_I32 voiceNum)
{
    S_WT_VOICE *pWTVoice;
    const S_DLS_ARTICULATION *pDLSArt;

    pWTVoice = &pVoiceMgr->wtVoices[voiceNum];
    pDLSArt = &pSynth->pDLS->pDLSArticulations[pWTVoice->artIndex];

    /* don't catch the voice if below the sustain level */
    if (pWTVoice->eg1Value < pDLSArt->eg1.sustainLevel)
        return;

    /* defer releasing this note until the damper pedal is off */
    pWTVoice->eg1State = eEnvelopeStateDecay;
    pVoice->voiceState = eVoiceStatePlay;
    pVoice->voiceFlags |= VOICE_FLAG_SUSTAIN_PEDAL_DEFER_NOTE_OFF;

#ifdef _DEBUG_SYNTH
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "DLS_SustainPedal: defer note off because sustain pedal is on\n"); */ }
#endif
}

/*----------------------------------------------------------------------------
 * DLS_UpdatePhaseInc()
 *----------------------------------------------------------------------------
 * Calculate the oscillator phase increment for the next frame
 *----------------------------------------------------------------------------
*/
static EAS_I32 DLS_UpdatePhaseInc (S_WT_VOICE *pWTVoice, const S_DLS_ARTICULATION *pDLSArt, S_SYNTH_CHANNEL *pChannel, EAS_I32 pitchCents)
{
    EAS_I32 temp;

    /* start with base mod LFO modulation */
    temp = pDLSArt->modLFOToPitch;

    /* add mod wheel effect */
    /*lint -e{702} use shift for performance */
    temp += ((pDLSArt->modLFOCC1ToPitch * pChannel->modWheel) >> 7);

    /* add channel pressure effect */
    /*lint -e{702} use shift for performance */
    temp += ((pDLSArt->modLFOChanPressToPitch * pChannel->channelPressure) >> 7);

    /* add total mod LFO effect */
    pitchCents += FMUL_15x15(temp, pWTVoice->modLFO.lfoValue);

    /* start with base vib LFO modulation */
    temp = pDLSArt->vibLFOToPitch;

    /* add mod wheel effect */
    /*lint -e{702} use shift for performance */
    temp += ((pDLSArt->vibLFOCC1ToPitch * pChannel->modWheel) >> 7);

    /* add channel pressure effect */
    /*lint -e{702} use shift for performance */
    temp += ((pDLSArt->vibLFOChanPressToPitch * pChannel->channelPressure) >> 7);

    /* add total vibrato LFO effect */
    pitchCents += FMUL_15x15(temp, pWTVoice->vibLFO.lfoValue);

    /* add EG2 effect */
    pitchCents += FMUL_15x15(pDLSArt->eg2ToPitch, pWTVoice->eg2Value);

    /* convert from cents to linear phase increment */
    return EAS_Calculate2toX(pitchCents);
}

/*----------------------------------------------------------------------------
 * DLS_UpdateGain()
 *----------------------------------------------------------------------------
 * Calculate the gain for the next frame
 *----------------------------------------------------------------------------
*/
static EAS_I32 DLS_UpdateGain (S_WT_VOICE *pWTVoice, const S_DLS_ARTICULATION *pDLSArt, S_SYNTH_CHANNEL *pChannel, EAS_I32 gain, EAS_U8 velocity)
{
    EAS_I32 temp;

    /* start with base mod LFO modulation */
    temp = pDLSArt->modLFOToGain;

    /* add mod wheel effect */
    /*lint -e{702} use shift for performance */
    temp += ((pDLSArt->modLFOCC1ToGain * pChannel->modWheel) >> 7);

    /* add channel pressure effect */
    /*lint -e{702} use shift for performance */
    temp += ((pDLSArt->modLFOChanPressToGain * pChannel->channelPressure) >> 7);

    /* add total mod LFO effect */
    gain += FMUL_15x15(temp, pWTVoice->modLFO.lfoValue);
    if (gain > 0)
        gain = 0;

    /* convert to linear gain including EG1 */
    if (pWTVoice->eg1State != eEnvelopeStateAttack)
    {
        gain = (DLS_GAIN_FACTOR * gain) >> DLS_GAIN_SHIFT;
        /*lint -e{702} use shift for performance */
#if 1
        gain += (pWTVoice->eg1Value - 32767) >> 1;
        gain = EAS_LogToLinear16(gain);
#else
        gain = EAS_LogToLinear16(gain);
        temp = EAS_LogToLinear16((pWTVoice->eg1Value - 32767) >> 1);
        gain = FMUL_15x15(gain, temp);
#endif
    }
    else
    {
        gain = (DLS_GAIN_FACTOR * gain) >> DLS_GAIN_SHIFT;
        gain = EAS_LogToLinear16(gain);
        gain = FMUL_15x15(gain, pWTVoice->eg1Value);
    }

    /* include MIDI channel gain */
    gain = FMUL_15x15(gain, pChannel->staticGain);

    /* include velocity */
    if (pDLSArt->filterQandFlags & FLAG_DLS_VELOCITY_SENSITIVE)
    {
        temp = velocity << 8;
        temp = FMUL_15x15(temp, temp);
        gain = FMUL_15x15(gain, temp);
    }

    /* return gain */
    return gain;
}

/*----------------------------------------------------------------------------
 * DLS_UpdateFilter()
 *----------------------------------------------------------------------------
 * Update the Filter parameters
 *----------------------------------------------------------------------------
*/
static void DLS_UpdateFilter (S_SYNTH_VOICE *pVoice, S_WT_VOICE *pWTVoice, S_WT_INT_FRAME *pIntFrame, S_SYNTH_CHANNEL *pChannel, const S_DLS_ARTICULATION *pDLSArt)
{
    EAS_I32 cutoff;
    EAS_I32 temp;

    /* no need to calculate filter coefficients if it is bypassed */
    if (pDLSArt->filterCutoff == DEFAULT_DLS_FILTER_CUTOFF_FREQUENCY)
    {
        pIntFrame->frame.k = 0;
        return;
    }

    /* start with base cutoff frequency */
    cutoff = pDLSArt->filterCutoff;

    /* get base mod LFO modulation */
    temp = pDLSArt->modLFOToFc;

    /* add mod wheel effect */
    /*lint -e{702} use shift for performance */
    temp += ((pDLSArt->modLFOCC1ToFc * pChannel->modWheel) >> 7);

    /* add channel pressure effect */
    /*lint -e{702} use shift for performance */
    temp += ((pDLSArt->modLFOChanPressToFc* pChannel->channelPressure) >> 7);

    /* add total mod LFO effect */
    cutoff += FMUL_15x15(temp, pWTVoice->modLFO.lfoValue);

    /* add EG2 effect */
    cutoff += FMUL_15x15(pWTVoice->eg2Value, pDLSArt->eg2ToFc);

    /* add velocity effect */
    /*lint -e{702} use shift for performance */
    cutoff += (pVoice->velocity * pDLSArt->velToFc) >> 7;

    /* add velocity effect */
    /*lint -e{702} use shift for performance */
    cutoff += (pVoice->note * pDLSArt->keyNumToFc) >> 7;

    /* subtract the A5 offset and the sampling frequency */
    cutoff -= FILTER_CUTOFF_FREQ_ADJUST + A5_PITCH_OFFSET_IN_CENTS;

    /* limit the cutoff frequency */
    if (cutoff > FILTER_CUTOFF_MAX_PITCH_CENTS)
        cutoff = FILTER_CUTOFF_MAX_PITCH_CENTS;
    else if (cutoff < FILTER_CUTOFF_MIN_PITCH_CENTS)
        cutoff = FILTER_CUTOFF_MIN_PITCH_CENTS;

    WT_SetFilterCoeffs(pIntFrame, cutoff, pDLSArt->filterQandFlags & FILTER_Q_MASK);
}

/*----------------------------------------------------------------------------
 * DLS_StartVoice()
 *----------------------------------------------------------------------------
 * Start up a DLS voice
 *----------------------------------------------------------------------------
*/
EAS_RESULT DLS_StartVoice (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, EAS_I32 voiceNum, EAS_U16 regionIndex)
{
    S_WT_VOICE *pWTVoice;
    const S_DLS_REGION *pDLSRegion;
    const S_DLS_ARTICULATION *pDLSArt;
    S_SYNTH_CHANNEL *pChannel;

#ifdef _DEBUG_SYNTH
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "DLS_StartVoice: Voice %ld; Region %d\n", (EAS_I32) (pVoice - pVoiceMgr->voices), regionIndex); */ }
#endif

    pWTVoice = &pVoiceMgr->wtVoices[voiceNum];
    pChannel = &pSynth->channels[pVoice->channel & 15];
    pDLSRegion = &pSynth->pDLS->pDLSRegions[regionIndex & REGION_INDEX_MASK];
    pWTVoice->artIndex = pDLSRegion->wtRegion.artIndex;
    pDLSArt = &pSynth->pDLS->pDLSArticulations[pWTVoice->artIndex];

    /* initialize the envelopes */
    pWTVoice->eg1State = eEnvelopeStateInit;
    DLS_UpdateEnvelope(pVoice, pChannel, &pDLSArt->eg1, &pWTVoice->eg1Value, &pWTVoice->eg1Increment, &pWTVoice->eg1State);
    pWTVoice->eg2State = eEnvelopeStateInit;
    DLS_UpdateEnvelope(pVoice, pChannel, &pDLSArt->eg2, &pWTVoice->eg2Value, &pWTVoice->eg2Increment, &pWTVoice->eg2State);

    /* initialize the LFOs */
    pWTVoice->modLFO.lfoValue = 0;
    pWTVoice->modLFO.lfoPhase = pDLSArt->modLFO.lfoDelay;
    pWTVoice->vibLFO.lfoValue = 0;
    pWTVoice->vibLFO.lfoPhase = pDLSArt->vibLFO.lfoDelay;

    /* initalize the envelopes and calculate initial gain */
    DLS_UpdateEnvelope(pVoice, pChannel, &pDLSArt->eg1, &pWTVoice->eg1Value, &pWTVoice->eg1Increment, &pWTVoice->eg1State);
    DLS_UpdateEnvelope(pVoice, pChannel, &pDLSArt->eg2, &pWTVoice->eg2Value, &pWTVoice->eg2Increment, &pWTVoice->eg2State);
    pVoice->gain = (EAS_I16) DLS_UpdateGain(pWTVoice, pDLSArt, pChannel, pDLSRegion->wtRegion.gain, pVoice->velocity);

#if (NUM_OUTPUT_CHANNELS == 2)
    EAS_CalcPanControl((EAS_INT) pChannel->pan - 64 + (EAS_INT) pDLSArt->pan, &pWTVoice->gainLeft, &pWTVoice->gainRight);
#endif

    /* initialize the filter states */
    pWTVoice->filter.z1 = 0;
    pWTVoice->filter.z2 = 0;

    /* initialize the oscillator */
    pWTVoice->phaseAccum = (EAS_U32) pSynth->pDLS->pDLSSamples + pSynth->pDLS->pDLSSampleOffsets[pDLSRegion->wtRegion.waveIndex];
    if (pDLSRegion->wtRegion.region.keyGroupAndFlags & REGION_FLAG_IS_LOOPED)
    {
        pWTVoice->loopStart = pWTVoice->phaseAccum + pDLSRegion->wtRegion.loopStart;
        pWTVoice->loopEnd = pWTVoice->phaseAccum + pDLSRegion->wtRegion.loopEnd - 1;
    }
    else
        pWTVoice->loopStart = pWTVoice->loopEnd = pWTVoice->phaseAccum + pSynth->pDLS->pDLSSampleLen[pDLSRegion->wtRegion.waveIndex] - 1;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * DLS_UpdateVoice()
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
EAS_BOOL DLS_UpdateVoice (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, EAS_I32 voiceNum, EAS_I32 *pMixBuffer, EAS_I32 numSamples)
{
    S_WT_VOICE *pWTVoice;
    S_SYNTH_CHANNEL *pChannel;
    const S_DLS_REGION *pDLSRegion;
    const S_DLS_ARTICULATION *pDLSArt;
    S_WT_INT_FRAME intFrame;
    EAS_I32 temp;
    EAS_BOOL done = EAS_FALSE;

    /* establish pointers to critical data */
    pWTVoice = &pVoiceMgr->wtVoices[voiceNum];
    pDLSRegion = &pSynth->pDLS->pDLSRegions[pVoice->regionIndex & REGION_INDEX_MASK];
    pChannel = &pSynth->channels[pVoice->channel & 15];
    pDLSArt = &pSynth->pDLS->pDLSArticulations[pWTVoice->artIndex];

    /* update the envelopes */
    DLS_UpdateEnvelope(pVoice, pChannel, &pDLSArt->eg1, &pWTVoice->eg1Value, &pWTVoice->eg1Increment, &pWTVoice->eg1State);
    DLS_UpdateEnvelope(pVoice, pChannel, &pDLSArt->eg2, &pWTVoice->eg2Value, &pWTVoice->eg2Increment, &pWTVoice->eg2State);

    /* update the LFOs using the EAS synth function */
    WT_UpdateLFO(&pWTVoice->modLFO, pDLSArt->modLFO.lfoFreq);
    WT_UpdateLFO(&pWTVoice->vibLFO, pDLSArt->vibLFO.lfoFreq);

    /* calculate base frequency */
    temp = pDLSArt->tuning + pChannel->staticPitch + pDLSRegion->wtRegion.tuning +
        (((EAS_I32) pVoice->note * (EAS_I32) pDLSArt->keyNumToPitch) >> 7);

    /* don't transpose rhythm channel */
    if ((pChannel ->channelFlags & CHANNEL_FLAG_RHYTHM_CHANNEL) == 0)
        temp += pSynth->globalTranspose * 100;

    /* calculate phase increment including modulation effects */
    intFrame.frame.phaseIncrement = DLS_UpdatePhaseInc(pWTVoice, pDLSArt, pChannel, temp);

    /* calculate gain including modulation effects */
    intFrame.frame.gainTarget = DLS_UpdateGain(pWTVoice, pDLSArt, pChannel, pDLSRegion->wtRegion.gain, pVoice->velocity);
    intFrame.prevGain = pVoice->gain;

    DLS_UpdateFilter(pVoice, pWTVoice, &intFrame, pChannel, pDLSArt);

    /* call into engine to generate samples */
    intFrame.pAudioBuffer = pVoiceMgr->voiceBuffer;
    intFrame.pMixBuffer = pMixBuffer;
    intFrame.numSamples = numSamples;
    if (numSamples < 0)
        return EAS_FALSE;

    /* check for end of sample */
    if ((pWTVoice->loopStart != WT_NOISE_GENERATOR) && (pWTVoice->loopStart == pWTVoice->loopEnd))
        done = WT_CheckSampleEnd(pWTVoice, &intFrame, EAS_FALSE);

    WT_ProcessVoice(pWTVoice, &intFrame);

    /* clear flag */
    pVoice->voiceFlags &= ~VOICE_FLAG_NO_SAMPLES_SYNTHESIZED_YET;

    /* if the update interval has elapsed, then force the current gain to the next
     * gain since we never actually reach the next gain when ramping -- we just get
     * very close to the target gain.
     */
    pVoice->gain = (EAS_I16) intFrame.frame.gainTarget;

    /* if voice has finished, set flag for voice manager */
    if ((pVoice->voiceState != eVoiceStateStolen) && (pWTVoice->eg1State == eEnvelopeStateMuted))
        done = EAS_TRUE;

    return done;
}

/*----------------------------------------------------------------------------
 * DLS_UpdateEnvelope()
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
/*lint -esym(715, pChannel) pChannel not used in this instance */
static void DLS_UpdateEnvelope (S_SYNTH_VOICE *pVoice, S_SYNTH_CHANNEL *pChannel,  const S_DLS_ENVELOPE *pEnvParams, EAS_I16 *pValue, EAS_I16 *pIncrement, EAS_U8 *pState)
{
    EAS_I32 temp;

    switch (*pState)
    {
        /* initial state */
        case eEnvelopeStateInit:
            *pState = eEnvelopeStateDelay;
            *pValue = 0;
            *pIncrement = pEnvParams->delayTime;
            if (*pIncrement != 0)
                return;
            /*lint -e{825} falls through to next case */

        case eEnvelopeStateDelay:
            if (*pIncrement)
            {
                *pIncrement = *pIncrement - 1;
                return;
            }

            /* calculate attack rate */
            *pState = eEnvelopeStateAttack;
            if (pEnvParams->attackTime != ZERO_TIME_IN_CENTS)
            {
                /*lint -e{702} use shift for performance */
                temp = pEnvParams->attackTime + ((pEnvParams->velToAttack * pVoice->velocity) >> 7);
                *pIncrement = ConvertRate(temp);
                return;
            }

            *pValue = SYNTH_FULL_SCALE_EG1_GAIN;
            /*lint -e{825} falls through to next case */

        case eEnvelopeStateAttack:
            if (*pValue < SYNTH_FULL_SCALE_EG1_GAIN)
            {
                temp = *pValue + *pIncrement;
                *pValue = (EAS_I16) (temp < SYNTH_FULL_SCALE_EG1_GAIN ? temp : SYNTH_FULL_SCALE_EG1_GAIN);
                return;
            }

            /* calculate hold time */
            *pState = eEnvelopeStateHold;
            if (pEnvParams->holdTime != ZERO_TIME_IN_CENTS)
            {
                /*lint -e{702} use shift for performance */
                temp = pEnvParams->holdTime + ((pEnvParams->keyNumToHold * pVoice->note) >> 7);
                *pIncrement = ConvertDelay(temp);
                return;
            }
            else
                *pIncrement = 0;
            /*lint -e{825} falls through to next case */

        case eEnvelopeStateHold:
            if (*pIncrement)
            {
                *pIncrement = *pIncrement - 1;
                return;
            }

            /* calculate decay rate */
            *pState = eEnvelopeStateDecay;
            if (pEnvParams->decayTime != ZERO_TIME_IN_CENTS)
            {
                /*lint -e{702} use shift for performance */
                temp = pEnvParams->decayTime + ((pEnvParams->keyNumToDecay * pVoice->note) >> 7);
                *pIncrement = ConvertRate(temp);
                return;
            }

//          *pValue = pEnvParams->sustainLevel;
            /*lint -e{825} falls through to next case */

        case eEnvelopeStateDecay:
            if (*pValue > pEnvParams->sustainLevel)
            {
                temp = *pValue - *pIncrement;
                *pValue = (EAS_I16) (temp > pEnvParams->sustainLevel ? temp : pEnvParams->sustainLevel);
                return;
            }

            *pState = eEnvelopeStateSustain;
            *pValue = pEnvParams->sustainLevel;
            /*lint -e{825} falls through to next case */

        case eEnvelopeStateSustain:
            return;

        case eEnvelopeStateRelease:
            temp = *pValue - *pIncrement;
            if (temp <= 0)
            {
                *pState = eEnvelopeStateMuted;
                *pValue = 0;
            }
            else
                *pValue = (EAS_I16) temp;
            break;

        case eEnvelopeStateMuted:
            *pValue = 0;
            return;

        default:
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Envelope in invalid state %d\n", *pState); */ }
            break;
    }
}

