/*----------------------------------------------------------------------------
 *
 * File:
 * eas_voicemgt.c
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
 *   $Revision: 794 $
 *   $Date: 2007-08-01 00:08:48 -0700 (Wed, 01 Aug 2007) $
 *----------------------------------------------------------------------------
*/

/* includes */
#include "eas.h"
#include "eas_data.h"
#include "eas_config.h"
#include "eas_report.h"
#include "eas_midictrl.h"
#include "eas_host.h"
#include "eas_synth_protos.h"
#include "eas_vm_protos.h"

#ifdef DLS_SYNTHESIZER
#include "eas_mdls.h"
#endif

// #define _DEBUG_VM

/* some defines for workload */
#define WORKLOAD_AMOUNT_SMALL_INCREMENT     5
#define WORKLOAD_AMOUNT_START_NOTE          10
#define WORKLOAD_AMOUNT_STOP_NOTE           10
#define WORKLOAD_AMOUNT_KEY_GROUP           10
#define WORKLOAD_AMOUNT_POLY_LIMIT          10

/* pointer to base sound library */
extern S_EAS easSoundLib;

#ifdef TEST_HARNESS
extern S_EAS easTestLib;
EAS_SNDLIB_HANDLE VMGetLibHandle(EAS_INT libNum)
{
    switch (libNum)
    {
        case 0:
            return &easSoundLib;
#ifdef _WT_SYNTH
        case 1:
            return &easTestLib;
#endif
        default:
            return NULL;
    }
}
#endif

/* pointer to synthesizer interface(s) */
#ifdef _WT_SYNTH
extern const S_SYNTH_INTERFACE wtSynth;
#endif

#ifdef _FM_SYNTH
extern const S_SYNTH_INTERFACE fmSynth;
#endif

typedef S_SYNTH_INTERFACE *S_SYNTH_INTERFACE_HANDLE;

/* wavetable on MCU */
#if defined(EAS_WT_SYNTH)
const S_SYNTH_INTERFACE *const pPrimarySynth = &wtSynth;

/* FM on MCU */
#elif defined(EAS_FM_SYNTH)
const S_SYNTH_INTERFACE *const pPrimarySynth = &fmSynth;

/* wavetable drums on MCU, FM melodic on DSP */
#elif defined(EAS_HYBRID_SYNTH)
const S_SYNTH_INTERFACE *const pPrimarySynth = &wtSynth;
const S_SYNTH_INTERFACE *const pSecondarySynth = &fmSynth;

/* wavetable drums on MCU, wavetable melodic on DSP */
#elif defined(EAS_SPLIT_WT_SYNTH)
const S_SYNTH_INTERFACE *const pPrimarySynth = &wtSynth;
extern const S_FRAME_INTERFACE wtFrameInterface;
const S_FRAME_INTERFACE *const pFrameInterface = &wtFrameInterface;

/* wavetable drums on MCU, FM melodic on DSP */
#elif defined(EAS_SPLIT_HYBRID_SYNTH)
const S_SYNTH_INTERFACE *const pPrimarySynth = &wtSynth;
const S_SYNTH_INTERFACE *const pSecondarySynth = &fmSynth;
extern const S_FRAME_INTERFACE fmFrameInterface;
const S_FRAME_INTERFACE *const pFrameInterface = &fmFrameInterface;

/* FM on DSP */
#elif defined(EAS_SPLIT_FM_SYNTH)
const S_SYNTH_INTERFACE *const pPrimarySynth = &fmSynth;
extern const S_FRAME_INTERFACE fmFrameInterface;
const S_FRAME_INTERFACE *const pFrameInterface = &fmFrameInterface;

#else
#error "Undefined architecture option"
#endif

/*----------------------------------------------------------------------------
 * inline functions
 *----------------------------------------------------------------------------
*/
EAS_INLINE const S_REGION* GetRegionPtr (S_SYNTH *pSynth, EAS_U16 regionIndex)
{
#if defined(DLS_SYNTHESIZER)
    if (regionIndex & FLAG_RGN_IDX_DLS_SYNTH)
        return &pSynth->pDLS->pDLSRegions[regionIndex & REGION_INDEX_MASK].wtRegion.region;
#endif
#if defined(_HYBRID_SYNTH)
    if (regionIndex & FLAG_RGN_IDX_FM_SYNTH)
        return &pSynth->pEAS->pFMRegions[regionIndex & REGION_INDEX_MASK].region;
    else
        return &pSynth->pEAS->pWTRegions[regionIndex].region;
#elif defined(_WT_SYNTH)
    return &pSynth->pEAS->pWTRegions[regionIndex].region;
#elif defined(_FM_SYNTH)
    return &pSynth->pEAS->pFMRegions[regionIndex].region;
#endif
}

/*lint -esym(715, voiceNum) used in some implementation */
EAS_INLINE const S_SYNTH_INTERFACE* GetSynthPtr (EAS_INT voiceNum)
{
#if defined(_HYBRID_SYNTH)
    if (voiceNum < NUM_PRIMARY_VOICES)
        return pPrimarySynth;
    else
        return pSecondarySynth;
#else
    return pPrimarySynth;
#endif
}

EAS_INLINE EAS_INT GetAdjustedVoiceNum (EAS_INT voiceNum)
{
#if defined(_HYBRID_SYNTH)
    if (voiceNum >= NUM_PRIMARY_VOICES)
        return voiceNum - NUM_PRIMARY_VOICES;
#endif
    return voiceNum;
}

EAS_INLINE EAS_U8 VSynthToChannel (S_SYNTH *pSynth, EAS_U8 channel)
{
    /*lint -e{734} synthNum is always 0-15 */
    return channel | (pSynth->vSynthNum << 4);
}

/*----------------------------------------------------------------------------
 * InitVoice()
 *----------------------------------------------------------------------------
 * Initialize a synthesizer voice
 *----------------------------------------------------------------------------
*/
void InitVoice (S_SYNTH_VOICE *pVoice)
{
    pVoice->channel = UNASSIGNED_SYNTH_CHANNEL;
    pVoice->nextChannel = UNASSIGNED_SYNTH_CHANNEL;
    pVoice->note = pVoice->nextNote = DEFAULT_KEY_NUMBER;
    pVoice->velocity = pVoice->nextVelocity = DEFAULT_VELOCITY;
    pVoice->regionIndex = DEFAULT_REGION_INDEX;
    pVoice->age = DEFAULT_AGE;
    pVoice->voiceFlags = DEFAULT_VOICE_FLAGS;
    pVoice->voiceState = DEFAULT_VOICE_STATE;
}

/*----------------------------------------------------------------------------
 * IncVoicePoolCount()
 *----------------------------------------------------------------------------
 * Updates the voice pool count when a voice changes state
 *----------------------------------------------------------------------------
*/
static void IncVoicePoolCount (S_VOICE_MGR *pVoiceMgr, S_SYNTH_VOICE *pVoice)
{
    S_SYNTH *pSynth;
    EAS_INT pool;

    /* ignore muting voices */
    if (pVoice->voiceState == eVoiceStateMuting)
        return;

    if (pVoice->voiceState == eVoiceStateStolen)
    {
        pSynth = pVoiceMgr->pSynth[GET_VSYNTH(pVoice->nextChannel)];
        pool = pSynth->channels[GET_CHANNEL(pVoice->nextChannel)].pool;
    }
    else
    {
        pSynth = pVoiceMgr->pSynth[GET_VSYNTH(pVoice->channel)];
        pool = pSynth->channels[GET_CHANNEL(pVoice->channel)].pool;
    }

    pSynth->poolCount[pool]++;

#ifdef _DEBUG_VM
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IncVoicePoolCount: Synth=%d pool=%d\n", pSynth->vSynthNum, pool); */ }
#endif
}

/*----------------------------------------------------------------------------
 * DecVoicePoolCount()
 *----------------------------------------------------------------------------
 * Updates the voice pool count when a voice changes state
 *----------------------------------------------------------------------------
*/
static void DecVoicePoolCount (S_VOICE_MGR *pVoiceMgr, S_SYNTH_VOICE *pVoice)
{
    S_SYNTH *pSynth;
    EAS_INT pool;

    /* ignore muting voices */
    if (pVoice->voiceState == eVoiceStateMuting)
        return;

    if (pVoice->voiceState == eVoiceStateStolen)
    {
        pSynth = pVoiceMgr->pSynth[GET_VSYNTH(pVoice->nextChannel)];
        pool = pSynth->channels[GET_CHANNEL(pVoice->nextChannel)].pool;
    }
    else
    {
        pSynth = pVoiceMgr->pSynth[GET_VSYNTH(pVoice->channel)];
        pool = pSynth->channels[GET_CHANNEL(pVoice->channel)].pool;
    }

    pSynth->poolCount[pool]--;

#ifdef _DEBUG_VM
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "DecVoicePoolCount: Synth=%d pool=%d\n", pSynth->vSynthNum, pool); */ }
#endif
}

/*----------------------------------------------------------------------------
 * VMInitialize()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMInitialize (S_EAS_DATA *pEASData)
{
    S_VOICE_MGR *pVoiceMgr;
    EAS_INT i;

    /* check Configuration Module for data allocation */
    if (pEASData->staticMemoryModel)
        pVoiceMgr = EAS_CMEnumData(EAS_CM_SYNTH_DATA);
    else
        pVoiceMgr = EAS_HWMalloc(pEASData->hwInstData, sizeof(S_VOICE_MGR));
    if (!pVoiceMgr)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "VMInitialize: Failed to allocate synthesizer memory\n"); */ }
        return EAS_ERROR_MALLOC_FAILED;
    }
    EAS_HWMemSet(pVoiceMgr, 0, sizeof(S_VOICE_MGR));

    /* initialize non-zero variables */
    pVoiceMgr->pGlobalEAS = (S_EAS*) &easSoundLib;
    pVoiceMgr->maxPolyphony = (EAS_U16) MAX_SYNTH_VOICES;

#if defined(_SECONDARY_SYNTH) || defined(EAS_SPLIT_WT_SYNTH)
    pVoiceMgr->maxPolyphonyPrimary = NUM_PRIMARY_VOICES;
    pVoiceMgr->maxPolyphonySecondary = NUM_SECONDARY_VOICES;
#endif

    /* set max workload to zero */
    pVoiceMgr->maxWorkLoad = 0;

    /* initialize the voice manager parameters */
    for (i = 0; i < MAX_SYNTH_VOICES; i++)
        InitVoice(&pVoiceMgr->voices[i]);

    /* initialize the synth */
    /*lint -e{522} return unused at this time */
    pPrimarySynth->pfInitialize(pVoiceMgr);

    /* initialize the off-chip synth */
#ifdef _HYBRID_SYNTH
    /*lint -e{522} return unused at this time */
    pSecondarySynth->pfInitialize(pVoiceMgr);
#endif

    pEASData->pVoiceMgr = pVoiceMgr;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * VMInitMIDI()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMInitMIDI (S_EAS_DATA *pEASData, S_SYNTH **ppSynth)
{
    EAS_RESULT result;
    S_SYNTH *pSynth;
    EAS_INT virtualSynthNum;

    *ppSynth = NULL;

    /* static memory model only allows one synth */
    if (pEASData->staticMemoryModel)
    {
        if (pEASData->pVoiceMgr->pSynth[0] != NULL)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "VMInitMIDI: No virtual synthesizer support for static memory model\n"); */ }
            return EAS_ERROR_NO_VIRTUAL_SYNTHESIZER;
        }

        /* check Configuration Module for data allocation */
        pSynth = EAS_CMEnumData(EAS_CM_MIDI_DATA);
        virtualSynthNum = 0;
    }

    /* dynamic memory model */
    else
    {
        for (virtualSynthNum = 0; virtualSynthNum < MAX_VIRTUAL_SYNTHESIZERS; virtualSynthNum++)
            if (pEASData->pVoiceMgr->pSynth[virtualSynthNum] == NULL)
                break;
        if (virtualSynthNum == MAX_VIRTUAL_SYNTHESIZERS)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "VMInitMIDI: Exceeded number of active virtual synthesizers"); */ }
            return EAS_ERROR_NO_VIRTUAL_SYNTHESIZER;
        }
        pSynth = EAS_HWMalloc(pEASData->hwInstData, sizeof(S_SYNTH));
    }

    /* make sure we have a valid memory pointer */
    if (pSynth == NULL)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "VMInitMIDI: Failed to allocate synthesizer memory\n"); */ }
        return EAS_ERROR_MALLOC_FAILED;
    }
    EAS_HWMemSet(pSynth, 0, sizeof(S_SYNTH));

    /* set the sound library pointer */
    if ((result = VMSetEASLib(pSynth, pEASData->pVoiceMgr->pGlobalEAS)) != EAS_SUCCESS)
    {
        VMMIDIShutdown(pEASData, pSynth);
        return result;
    }

    /* link in DLS bank if downloaded */
#ifdef DLS_SYNTHESIZER
    if (pEASData->pVoiceMgr->pGlobalDLS)
    {
        pSynth->pDLS = pEASData->pVoiceMgr->pGlobalDLS;
        DLSAddRef(pSynth->pDLS);
    }
#endif

    /* initialize MIDI state variables */
    pSynth->synthFlags = DEFAULT_SYNTH_FLAGS;
    pSynth->masterVolume = DEFAULT_SYNTH_MASTER_VOLUME;
    pSynth->refCount = 1;
    pSynth->priority = DEFAULT_SYNTH_PRIORITY;
    pSynth->poolAlloc[0] = (EAS_U8) pEASData->pVoiceMgr->maxPolyphony;

    VMInitializeAllChannels(pEASData->pVoiceMgr, pSynth);

    pSynth->vSynthNum = (EAS_U8) virtualSynthNum;
    pEASData->pVoiceMgr->pSynth[virtualSynthNum] = pSynth;

    *ppSynth = pSynth;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * VMIncRefCount()
 *----------------------------------------------------------------------------
 * Increment reference count for virtual synth
 *----------------------------------------------------------------------------
*/
void VMIncRefCount (S_SYNTH *pSynth)
{
    pSynth->refCount++;
}

/*----------------------------------------------------------------------------
 * VMReset()
 *----------------------------------------------------------------------------
 * Purpose:
 * We call this routine to start the process of reseting the synth.
 * This routine sets a flag for the entire synth indicating that we want
 * to reset.
 * We also force all voices to mute quickly.
 * However, we do not actually perform any synthesis in this routine. That
 * is, we do not ramp the voices down from this routine, but instead, we
 * let the "regular" synth processing steps take care of adding the ramp
 * down samples to the output buffer. After we are sure that all voices
 * have completed ramping down, we continue the process of resetting the
 * synth (from another routine).
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 * force - force reset even if voices are active
 *
 * Outputs:
 *
 * Side Effects:
 * - set a flag (in psSynthObject->m_nFlags) indicating synth reset requested.
 * - force all voices to update their envelope states to mute
 *
 *----------------------------------------------------------------------------
*/
void VMReset (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_BOOL force)
{

#ifdef _DEBUG_VM
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMReset: request to reset synth. Force = %d\n", force); */ }
#endif

    /* force voices to off state - may cause audio artifacts */
    if (force)
    {
        pVoiceMgr->activeVoices -= pSynth->numActiveVoices;
        pSynth->numActiveVoices = 0;
        VMInitializeAllVoices(pVoiceMgr, pSynth->vSynthNum);
    }
    else
        VMMuteAllVoices(pVoiceMgr, pSynth);

    /* don't reset if voices are still playing */
    if (pSynth->numActiveVoices == 0)
    {
        EAS_INT i;

#ifdef _DEBUG_VM
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMReset: complete the reset process\n"); */ }
#endif

        VMInitializeAllChannels(pVoiceMgr, pSynth);
        for (i = 0; i < NUM_SYNTH_CHANNELS; i++)
            pSynth->poolCount[i] = 0;

        /* set polyphony */
        if (pSynth->maxPolyphony < pVoiceMgr->maxPolyphony)
            pSynth->poolAlloc[0] = (EAS_U8) pVoiceMgr->maxPolyphony;
        else
            pSynth->poolAlloc[0] = (EAS_U8) pSynth->maxPolyphony;

        /* clear reset flag */
        pSynth->synthFlags &= ~SYNTH_FLAG_RESET_IS_REQUESTED;
    }

    /* handle reset after voices are muted */
    else
        pSynth->synthFlags |= SYNTH_FLAG_RESET_IS_REQUESTED;
}

/*----------------------------------------------------------------------------
 * VMInitializeAllChannels()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
void VMInitializeAllChannels (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth)
{
    S_SYNTH_CHANNEL *pChannel;
    EAS_INT i;

    VMResetControllers(pSynth);

    /* init each channel */
    pChannel = pSynth->channels;

    for (i = 0; i < NUM_SYNTH_CHANNELS; i++, pChannel++)
    {
        pChannel->channelFlags = DEFAULT_CHANNEL_FLAGS;
        pChannel->staticGain = DEFAULT_CHANNEL_STATIC_GAIN;
        pChannel->staticPitch = DEFAULT_CHANNEL_STATIC_PITCH;
        pChannel->pool = 0;

        /* the drum channel needs a different init */
        if (i == DEFAULT_DRUM_CHANNEL)
        {
            pChannel->bankNum = DEFAULT_RHYTHM_BANK_NUMBER;
            pChannel->channelFlags |= CHANNEL_FLAG_RHYTHM_CHANNEL;
        }
        else
            pChannel->bankNum = DEFAULT_MELODY_BANK_NUMBER;

        VMProgramChange(pVoiceMgr, pSynth, (EAS_U8) i, DEFAULT_SYNTH_PROGRAM_NUMBER);
    }

}

/*----------------------------------------------------------------------------
 * VMResetControllers()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
void VMResetControllers (S_SYNTH *pSynth)
{
    S_SYNTH_CHANNEL *pChannel;
    EAS_INT i;

    pChannel = pSynth->channels;

    for (i = 0; i < NUM_SYNTH_CHANNELS; i++, pChannel++)
    {
        pChannel->pitchBend = DEFAULT_PITCH_BEND;
        pChannel->modWheel = DEFAULT_MOD_WHEEL;
        pChannel->volume = DEFAULT_CHANNEL_VOLUME;
        pChannel->pan = DEFAULT_PAN;
        pChannel->expression = DEFAULT_EXPRESSION;

#ifdef  _REVERB
        pSynth->channels[i].reverbSend = DEFAULT_REVERB_SEND;
#endif

#ifdef  _CHORUS
        pSynth->channels[i].chorusSend = DEFAULT_CHORUS_SEND;
#endif

        pChannel->channelPressure = DEFAULT_CHANNEL_PRESSURE;
        pChannel->registeredParam = DEFAULT_REGISTERED_PARAM;
        pChannel->pitchBendSensitivity = DEFAULT_PITCH_BEND_SENSITIVITY;
        pChannel->finePitch = DEFAULT_FINE_PITCH;
        pChannel->coarsePitch = DEFAULT_COARSE_PITCH;

        /* update all voices on this channel */
        pChannel->channelFlags |= CHANNEL_FLAG_UPDATE_CHANNEL_PARAMETERS;
    }
}

/*----------------------------------------------------------------------------
 * VMInitializeAllVoices()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
void VMInitializeAllVoices (S_VOICE_MGR *pVoiceMgr, EAS_INT vSynthNum)
{
    EAS_INT i;

    /* initialize the voice manager parameters */
    for (i = 0; i < MAX_SYNTH_VOICES; i++)
    {
        if (pVoiceMgr->voices[i].voiceState != eVoiceStateStolen)
        {
            if (GET_VSYNTH(pVoiceMgr->voices[i].channel) == vSynthNum)
                InitVoice(&pVoiceMgr->voices[i]);
        }
        else
        {
            if (GET_VSYNTH(pVoiceMgr->voices[i].nextChannel) == vSynthNum)
                InitVoice(&pVoiceMgr->voices[i]);
        }
    }
}

/*----------------------------------------------------------------------------
 * VMMuteVoice()
 *----------------------------------------------------------------------------
 * Mute the selected voice
 *----------------------------------------------------------------------------
*/
void VMMuteVoice (S_VOICE_MGR *pVoiceMgr, EAS_I32 voiceNum)
{
    S_SYNTH *pSynth;
    S_SYNTH_VOICE *pVoice;

    /* take no action if voice is already muted */
    pVoice = &pVoiceMgr->voices[voiceNum];
    if ((pVoice->voiceState == eVoiceStateMuting) || (pVoice->voiceState == eVoiceStateFree))
        return;

    /* one less voice in pool */
    DecVoicePoolCount(pVoiceMgr, pVoice);

    pSynth = pVoiceMgr->pSynth[GET_VSYNTH(pVoice->channel)];
    GetSynthPtr(voiceNum)->pfMuteVoice(pVoiceMgr, pSynth, pVoice, GetAdjustedVoiceNum(voiceNum));
    pVoice->voiceState = eVoiceStateMuting;

}

/*----------------------------------------------------------------------------
 * VMReleaseVoice()
 *----------------------------------------------------------------------------
 * Release the selected voice
 *----------------------------------------------------------------------------
*/
void VMReleaseVoice (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_I32 voiceNum)
{
    S_SYNTH_VOICE *pVoice = &pVoiceMgr->voices[voiceNum];

    /* take no action if voice is already free, muting, or releasing */
    if (( pVoice->voiceState == eVoiceStateMuting) ||
        (pVoice->voiceState == eVoiceStateFree) ||
        (pVoice->voiceState == eVoiceStateRelease))
            return;

    /* stolen voices should just be muted */
    if (pVoice->voiceState == eVoiceStateStolen)
        VMMuteVoice(pVoiceMgr, voiceNum);

    /* release this voice */
    GetSynthPtr(voiceNum)->pfReleaseVoice(pVoiceMgr, pSynth, &pVoiceMgr->voices[voiceNum], GetAdjustedVoiceNum(voiceNum));
    pVoice->voiceState = eVoiceStateRelease;
}

/*----------------------------------------------------------------------------
 * VMInitMIPTable()
 *----------------------------------------------------------------------------
 * Initialize the SP-MIDI MIP table in preparation for receiving MIP message
 *----------------------------------------------------------------------------
*/
void VMInitMIPTable (S_SYNTH *pSynth)
{
    EAS_INT i;

#ifdef _DEBUG_VM
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMInitMIPTable\n"); */ }
#endif

    /* clear SP-MIDI flag */
    pSynth->synthFlags &= ~SYNTH_FLAG_SP_MIDI_ON;
    for (i = 0; i < NUM_SYNTH_CHANNELS; i++)
    {
        pSynth->channels[i].pool = 0;
        pSynth->channels[i].mip = 0;
    }
}

/*----------------------------------------------------------------------------
 * VMSetMIPEntry()
 *----------------------------------------------------------------------------
 * Sets the priority and MIP level for a MIDI channel
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pVoiceMgr) reserved for future use */
void VMSetMIPEntry (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 priority, EAS_U8 mip)
{

#ifdef _DEBUG_VM
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMSetMIPEntry: channel=%d, priority=%d, MIP=%d\n", channel, priority, mip); */ }
#endif

    /* save data for use by MIP message processing */
    if (priority < NUM_SYNTH_CHANNELS)
    {
        pSynth->channels[channel].pool = priority;
        pSynth->channels[channel].mip = mip;
    }
}

/*----------------------------------------------------------------------------
 * VMMIPUpdateChannelMuting()
 *----------------------------------------------------------------------------
 * This routine is called after an SP-MIDI message is received and
 * any time the allocated polyphony changes. It mutes or unmutes
 * channels based on polyphony.
 *----------------------------------------------------------------------------
*/
void VMMIPUpdateChannelMuting (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth)
{
    EAS_INT i;
    EAS_INT maxPolyphony;
    EAS_INT channel;
    EAS_INT vSynthNum;
    EAS_INT pool;

#ifdef _DEBUG_VM
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMUpdateMIPTable\n"); */ }
#endif

    /* determine max polyphony */
    if (pSynth->maxPolyphony)
        maxPolyphony = pSynth->maxPolyphony;
    else
        maxPolyphony = pVoiceMgr->maxPolyphony;

    /* process channels */
    for (i = 0; i < NUM_SYNTH_CHANNELS; i++)
    {

        /* channel must be in MIP message and must meet allocation target */
        if ((pSynth->channels[i].mip != 0) && (pSynth->channels[i].mip <= maxPolyphony))
            pSynth->channels[i].channelFlags &= ~CHANNEL_FLAG_MUTE;
        else
            pSynth->channels[i].channelFlags |= CHANNEL_FLAG_MUTE;

        /* reset voice pool count */
        pSynth->poolCount[i] = 0;
    }

    /* mute any voices on muted channels, and count unmuted voices */
    for (i = 0; i < MAX_SYNTH_VOICES; i++)
    {

        /* ignore free voices */
        if (pVoiceMgr->voices[i].voiceState == eVoiceStateFree)
            continue;

        /* get channel and virtual synth */
        if (pVoiceMgr->voices[i].voiceState != eVoiceStateStolen)
        {
            vSynthNum = GET_VSYNTH(pVoiceMgr->voices[i].channel);
            channel = GET_CHANNEL(pVoiceMgr->voices[i].channel);
        }
        else
        {
            vSynthNum = GET_VSYNTH(pVoiceMgr->voices[i].nextChannel);
            channel = GET_CHANNEL(pVoiceMgr->voices[i].nextChannel);
        }

        /* ignore voices on other synths */
        if (vSynthNum != pSynth->vSynthNum)
            continue;

        /* count voices */
        pool = pSynth->channels[channel].pool;

        /* deal with muted channels */
        if (pSynth->channels[channel].channelFlags & CHANNEL_FLAG_MUTE)
        {
            /* mute stolen voices scheduled to play on this channel */
            if (pVoiceMgr->voices[i].voiceState == eVoiceStateStolen)
                pVoiceMgr->voices[i].voiceState = eVoiceStateMuting;

            /* release voices that aren't already muting */
            else if (pVoiceMgr->voices[i].voiceState != eVoiceStateMuting)
            {
                VMReleaseVoice(pVoiceMgr, pSynth, i);
                pSynth->poolCount[pool]++;
            }
        }

        /* not muted, count this voice */
        else
            pSynth->poolCount[pool]++;
    }
}

/*----------------------------------------------------------------------------
 * VMUpdateMIPTable()
 *----------------------------------------------------------------------------
 * This routine is called at the end of the SysEx message to allow
 * the Voice Manager to complete the initialization of the MIP
 * table. It assigns channels to the appropriate voice pool based
 * on the MIP setting and calculates the voices allocated for each
 * pool.
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pVoiceMgr) reserved for future use */
void VMUpdateMIPTable (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth)
{
    S_SYNTH_CHANNEL *pChannel;
    EAS_INT i;
    EAS_INT currentMIP;
    EAS_INT currentPool;
    EAS_INT priority[NUM_SYNTH_CHANNELS];

#ifdef _DEBUG_VM
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMUpdateMIPTable\n"); */ }
#endif

    /* set SP-MIDI flag */
    pSynth->synthFlags |= SYNTH_FLAG_SP_MIDI_ON;

    /* sort channels into priority order */
    for (i = 0; i < NUM_SYNTH_CHANNELS; i++)
        priority[i] = -1;
    for (i = 0; i < NUM_SYNTH_CHANNELS; i++)
    {
        if (pSynth->channels[i].pool != DEFAULT_SP_MIDI_PRIORITY)
            priority[pSynth->channels[i].pool] = i;
    }

    /* process channels in priority order */
    currentMIP = 0;
    currentPool = -1;
    for (i = 0; i < NUM_SYNTH_CHANNELS; i++)
    {
        /* stop when we run out of channels */
        if (priority[i] == -1)
            break;

        pChannel = &pSynth->channels[priority[i]];

        /* when 2 or more channels have the same MIP setting, they
         * share a common voice pool
         */
        if (pChannel->mip == currentMIP && currentPool != -1)
            pChannel->pool = (EAS_U8) currentPool;

        /* new voice pool */
        else
        {
            currentPool++;
            pSynth->poolAlloc[currentPool] = (EAS_U8) (pChannel->mip - currentMIP);
            currentMIP = pChannel->mip;
        }
    }

    /* set SP-MIDI flag */
    pSynth->synthFlags |= SYNTH_FLAG_SP_MIDI_ON;

    /* update channel muting */
    VMMIPUpdateChannelMuting (pVoiceMgr, pSynth);
}

/*----------------------------------------------------------------------------
 * VMMuteAllVoices()
 *----------------------------------------------------------------------------
 * Purpose:
 * We call this in an emergency reset situation.
 * This forces all voices to mute quickly.
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 * - forces all voices to update their envelope states to mute
 *
 *----------------------------------------------------------------------------
*/
void VMMuteAllVoices (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth)
{
    EAS_INT i;

#ifdef _DEBUG_VM
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMMuteAllVoices: about to mute all voices!!\n"); */ }
#endif

    for (i = 0; i < MAX_SYNTH_VOICES; i++)
    {
        /* for stolen voices, check new channel */
        if (pVoiceMgr->voices[i].voiceState == eVoiceStateStolen)
        {
            if (GET_VSYNTH(pVoiceMgr->voices[i].nextChannel) == pSynth->vSynthNum)
                VMMuteVoice(pVoiceMgr, i);
        }

        else if (pSynth->vSynthNum == GET_VSYNTH(pVoiceMgr->voices[i].channel))
            VMMuteVoice(pVoiceMgr, i);
    }
}

/*----------------------------------------------------------------------------
 * VMReleaseAllVoices()
 *----------------------------------------------------------------------------
 * Purpose:
 * We call this after we've encountered the end of the Midi file.
 * This ensures all voices are either in release (because we received their
 * note off already) or forces them to mute quickly.
 * We use this as a safety to prevent bad midi files from playing forever.
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 * - forces all voices to update their envelope states to release or mute
 *
 *----------------------------------------------------------------------------
*/
void VMReleaseAllVoices (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth)
{
    EAS_INT i;

    /* release sustain pedal on all channels */
    for (i = 0; i < NUM_SYNTH_CHANNELS; i++)
    {
        if (pSynth->channels[ i ].channelFlags & CHANNEL_FLAG_SUSTAIN_PEDAL)
        {
            VMReleaseAllDeferredNoteOffs(pVoiceMgr, pSynth, (EAS_U8) i);
            pSynth->channels[i].channelFlags &= ~CHANNEL_FLAG_SUSTAIN_PEDAL;
        }
    }

    /* release all voices */
    for (i = 0; i < MAX_SYNTH_VOICES; i++)
    {

        switch (pVoiceMgr->voices[i].voiceState)
        {
            case eVoiceStateStart:
            case eVoiceStatePlay:
                /* only release voices on this synth */
                if (GET_VSYNTH(pVoiceMgr->voices[i].channel) == pSynth->vSynthNum)
                    VMReleaseVoice(pVoiceMgr, pSynth, i);
                break;

            case eVoiceStateStolen:
                if (GET_VSYNTH(pVoiceMgr->voices[i].nextChannel) == pSynth->vSynthNum)
                    VMMuteVoice(pVoiceMgr, i);
                break;

            case eVoiceStateFree:
            case eVoiceStateRelease:
            case eVoiceStateMuting:
                break;

            case eVoiceStateInvalid:
            default:
#ifdef _DEBUG_VM
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMReleaseAllVoices: error, %d is an unrecognized state\n",
                    pVoiceMgr->voices[i].voiceState); */ }
#endif
                break;
        }
    }
}

/*----------------------------------------------------------------------------
 * VMAllNotesOff()
 *----------------------------------------------------------------------------
 * Purpose:
 * Quickly mute all notes on the given channel.
 *
 * Inputs:
 * nChannel - quickly turn off all notes on this channel
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 * - forces all voices on this channel to update their envelope states to mute
 *
 *----------------------------------------------------------------------------
*/
void VMAllNotesOff (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel)
{
    EAS_INT voiceNum;
    S_SYNTH_VOICE *pVoice;

#ifdef _DEBUG_VM
    if (channel >= NUM_SYNTH_CHANNELS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMAllNotesOff: error, %d invalid channel number\n",
            channel); */ }
        return;
    }
#endif

    /* increment workload */
    pVoiceMgr->workload += WORKLOAD_AMOUNT_SMALL_INCREMENT;

    /* check each voice */
    channel = VSynthToChannel(pSynth, channel);
    for (voiceNum = 0; voiceNum < MAX_SYNTH_VOICES; voiceNum++)
    {
        pVoice = &pVoiceMgr->voices[voiceNum];
        if (pVoice->voiceState != eVoiceStateFree)
        {
            if (((pVoice->voiceState != eVoiceStateStolen) && (channel == pVoice->channel)) ||
                ((pVoice->voiceState == eVoiceStateStolen) && (channel == pVoice->nextChannel)))
            {
                /* this voice is assigned to the requested channel */
                GetSynthPtr(voiceNum)->pfMuteVoice(pVoiceMgr, pSynth, pVoice, GetAdjustedVoiceNum(voiceNum));
                pVoice->voiceState = eVoiceStateMuting;
            }
        }
    }
}

/*----------------------------------------------------------------------------
 * VMDeferredStopNote()
 *----------------------------------------------------------------------------
 * Purpose:
 * Stop the notes that had deferred note-off requests.
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * None.
 *
 * Side Effects:
 * voices that have had deferred note-off requests are now put into release
 * psSynthObject->m_sVoice[i].m_nFlags has the VOICE_FLAG_DEFER_MIDI_NOTE_OFF
 *  cleared
 *----------------------------------------------------------------------------
*/
void VMDeferredStopNote (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth)
{
    EAS_INT voiceNum;
    EAS_INT channel;
    EAS_BOOL deferredNoteOff;

    deferredNoteOff = EAS_FALSE;

    /* check each voice to see if it requires a deferred note off */
    for (voiceNum=0; voiceNum < MAX_SYNTH_VOICES; voiceNum++)
    {
        if (pVoiceMgr->voices[voiceNum].voiceFlags & VOICE_FLAG_DEFER_MIDI_NOTE_OFF)
        {
            /* check if this voice was stolen */
            if (pVoiceMgr->voices[voiceNum].voiceState == eVoiceStateStolen)
            {
                /*
                This voice was stolen, AND it also has a deferred note-off.
                The stolen note must be completely ramped down at this point.
                The note that caused the stealing to occur, however, must
                have received a note-off request before the note that caused
                stealing ever had a chance to even start. We want to give
                the note that caused the stealing a chance to play, so we
                start it on the next update interval, and we defer sending
                the note-off request until the subsequent update interval.
                So do not send the note-off request for this voice because
                this voice was stolen and should have completed ramping down,
                Also, do not clear the global flag nor this voice's flag
                because we must indicate that the subsequent update interval,
                after the note that caused stealing has started, should
                then send the deferred note-off request.
                */
                deferredNoteOff = EAS_TRUE;

#ifdef _DEBUG_VM
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMDeferredStopNote: defer request to stop voice %d (channel=%d note=%d) - voice not started\n",
                    voiceNum,
                    pVoiceMgr->voices[voiceNum].nextChannel,
                    pVoiceMgr->voices[voiceNum].note); */ }

                /* sanity check: this stolen voice better be ramped to zero */
                if (0 != pVoiceMgr->voices[voiceNum].gain)
                {
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMDeferredStopNote: warning, this voice did not complete its ramp to zero\n"); */ }
                }
#endif  // #ifdef _DEBUG_VM

            }
            else
            {
                /* clear the flag using exor */
                pVoiceMgr->voices[voiceNum].voiceFlags ^=
                    VOICE_FLAG_DEFER_MIDI_NOTE_OFF;

#ifdef _DEBUG_VM
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMDeferredStopNote: Stop voice %d (channel=%d note=%d)\n",
                    voiceNum,
                    pVoiceMgr->voices[voiceNum].nextChannel,
                    pVoiceMgr->voices[voiceNum].note); */ }
#endif

                channel = pVoiceMgr->voices[voiceNum].channel & 15;

                /* check if sustain pedal is on */
                if (pSynth->channels[channel].channelFlags & CHANNEL_FLAG_SUSTAIN_PEDAL)
                {
                    GetSynthPtr(voiceNum)->pfSustainPedal(pVoiceMgr, pSynth, &pVoiceMgr->voices[voiceNum], &pSynth->channels[channel], GetAdjustedVoiceNum(voiceNum));
                }

                /* release this voice */
                else
                    VMReleaseVoice(pVoiceMgr, pSynth, voiceNum);

            }

        }

    }

    /* clear the deferred note-off flag, unless there's another one pending */
    if (deferredNoteOff == EAS_FALSE)
        pSynth->synthFlags ^= SYNTH_FLAG_DEFERRED_MIDI_NOTE_OFF_PENDING;
}

/*----------------------------------------------------------------------------
 * VMReleaseAllDeferredNoteOffs()
 *----------------------------------------------------------------------------
 * Purpose:
 * Call this functin when the sustain flag is presently set but
 * we are now transitioning from damper pedal on to
 * damper pedal off. This means all notes in this channel
 * that received a note off while the damper pedal was on, and
 * had their note-off requests deferred, should now proceed to
 * the release state.
 *
 * Inputs:
 * nChannel - this channel has its sustain pedal transitioning from on to off
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * Side Effects:
 * any voice with deferred note offs on this channel are updated such that
 * pVoice->m_sEG1.m_eState = eEnvelopeStateRelease
 * pVoice->m_sEG1.m_nIncrement = release increment
 * pVoice->m_nFlags = clear the deferred note off flag
 *----------------------------------------------------------------------------
*/
void VMReleaseAllDeferredNoteOffs (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel)
{
    S_SYNTH_VOICE *pVoice;
    EAS_INT voiceNum;

#ifdef _DEBUG_VM
    if (channel >= NUM_SYNTH_CHANNELS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMReleaseAllDeferredNoteOffs: error, %d invalid channel number\n",
            channel); */ }
        return;
    }
#endif  /* #ifdef _DEBUG_VM */

    /* increment workload */
    pVoiceMgr->workload += WORKLOAD_AMOUNT_SMALL_INCREMENT;

    /* find all the voices assigned to this channel */
    channel = VSynthToChannel(pSynth, channel);
    for (voiceNum = 0; voiceNum < MAX_SYNTH_VOICES; voiceNum++)
    {

        pVoice = &pVoiceMgr->voices[voiceNum];
        if (channel == pVoice->channel)
        {

            /* does this voice have a deferred note off? */
            if (pVoice->voiceFlags & VOICE_FLAG_SUSTAIN_PEDAL_DEFER_NOTE_OFF)
            {
                /* release voice */
                VMReleaseVoice(pVoiceMgr, pSynth, voiceNum);

                /* use exor to flip bit, clear the flag */
                pVoice->voiceFlags &= ~VOICE_FLAG_SUSTAIN_PEDAL_DEFER_NOTE_OFF;

            }

        }
    }

    return;
}

/*----------------------------------------------------------------------------
 * VMCatchNotesForSustainPedal()
 *----------------------------------------------------------------------------
 * Purpose:
 * Call this function when the sustain flag is presently clear and
 * the damper pedal is off and we are transitioning from damper pedal OFF to
 * damper pedal ON. Currently sounding notes should be left
 * unchanged. However, we should try to "catch" notes if possible.
 * If any notes are in release and have levels >= sustain level, catch them,
 * otherwise, let them continue to release.
 *
 * Inputs:
 * nChannel - this channel has its sustain pedal transitioning from on to off
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *----------------------------------------------------------------------------
*/
void VMCatchNotesForSustainPedal (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel)
{
    EAS_INT voiceNum;

#ifdef _DEBUG_VM
    if (channel >= NUM_SYNTH_CHANNELS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMCatchNotesForSustainPedal: error, %d invalid channel number\n",
            channel); */ }
        return;
    }
#endif

    pVoiceMgr->workload += WORKLOAD_AMOUNT_SMALL_INCREMENT;
    channel = VSynthToChannel(pSynth, channel);

    /* find all the voices assigned to this channel */
    for (voiceNum = 0; voiceNum < MAX_SYNTH_VOICES; voiceNum++)
    {
        if (channel == pVoiceMgr->voices[voiceNum].channel)
        {
            if (eVoiceStateRelease == pVoiceMgr->voices[voiceNum].voiceState)
                GetSynthPtr(voiceNum)->pfSustainPedal(pVoiceMgr, pSynth, &pVoiceMgr->voices[voiceNum], &pSynth->channels[channel], GetAdjustedVoiceNum(voiceNum));
        }
    }
}

/*----------------------------------------------------------------------------
 * VMUpdateAllNotesAge()
 *----------------------------------------------------------------------------
 * Purpose:
 * Increment the note age for all of the active voices.
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 * m_nAge for all voices is incremented
 *----------------------------------------------------------------------------
*/
void VMUpdateAllNotesAge (S_VOICE_MGR *pVoiceMgr, EAS_U16 age)
{
    EAS_INT i;

    for (i = 0; i < MAX_SYNTH_VOICES; i++)
    {
        if (age - pVoiceMgr->voices[i].age > 0)
            pVoiceMgr->voices[i].age++;
     }
}

/*----------------------------------------------------------------------------
 * VMStolenVoice()
 *----------------------------------------------------------------------------
 * Purpose:
 * The selected voice is being stolen. Sets the parameters so that the
 * voice will begin playing the new sound on the next buffer.
 *
 * Inputs:
 * pVoice - pointer to voice to steal
 * nChannel - the channel to start a note on
 * nKeyNumber - the key number to start a note for
 * nNoteVelocity - the key velocity from this note
 *
 * Outputs:
 * None
 *----------------------------------------------------------------------------
*/
static void VMStolenVoice (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_I32 voiceNum, EAS_U8 channel, EAS_U8 note, EAS_U8 velocity, EAS_U16 regionIndex)
{
    S_SYNTH_VOICE *pVoice = &pVoiceMgr->voices[voiceNum];

    /* one less voice in old pool */
    DecVoicePoolCount(pVoiceMgr, pVoice);

    /* mute the sound that is currently playing */
    GetSynthPtr(voiceNum)->pfMuteVoice(pVoiceMgr, pVoiceMgr->pSynth[GET_VSYNTH(pVoice->channel)], &pVoiceMgr->voices[voiceNum], GetAdjustedVoiceNum(voiceNum));
    pVoice->voiceState = eVoiceStateStolen;

    /* set new note data */
    pVoice->nextChannel = VSynthToChannel(pSynth, channel);
    pVoice->nextNote = note;
    pVoice->nextVelocity = velocity;
    pVoice->nextRegionIndex = regionIndex;

    /* one more voice in new pool */
    IncVoicePoolCount(pVoiceMgr, pVoice);

    /* clear the deferred flags */
    pVoice->voiceFlags &=
        ~(VOICE_FLAG_DEFER_MIDI_NOTE_OFF |
        VOICE_FLAG_DEFER_MUTE |
        VOICE_FLAG_SUSTAIN_PEDAL_DEFER_NOTE_OFF);

    /* all notes older than this one get "younger" */
    VMUpdateAllNotesAge(pVoiceMgr, pVoice->age);

    /* assign current age to this note and increment for the next note */
    pVoice->age = pVoiceMgr->age++;
}

/*----------------------------------------------------------------------------
 * VMFreeVoice()
 *----------------------------------------------------------------------------
 * Purpose:
 * The selected voice is done playing and being returned to the
 * pool of free voices
 *
 * Inputs:
 * pVoice - pointer to voice to free
 *
 * Outputs:
 * None
 *----------------------------------------------------------------------------
*/
static void VMFreeVoice (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice)
{

    /* do nothing if voice is already free */
    if (pVoice->voiceState == eVoiceStateFree)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "VMFreeVoice: Attempt to free voice that is already free\n"); */ }
        return;
    }

    /* if we jump directly to free without passing muting stage,
     * we need to adjust the voice count */
    DecVoicePoolCount(pVoiceMgr, pVoice);


#ifdef _DEBUG_VM
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "VMFreeVoice: Synth=%d\n", pSynth->vSynthNum); */ }
#endif

    /* return to free voice pool */
    pVoiceMgr->activeVoices--;
    pSynth->numActiveVoices--;
    InitVoice(pVoice);

#ifdef _DEBUG_VM
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMFreeVoice: free voice %d\n", pVoice - pVoiceMgr->voices); */ }
#endif

    /* all notes older than this one get "younger" */
    VMUpdateAllNotesAge(pVoiceMgr, pVoice->age);
 }

/*----------------------------------------------------------------------------
 * VMRetargetStolenVoice()
 *----------------------------------------------------------------------------
 * Purpose:
 * The selected voice has been stolen and needs to be initalized with
 * the paramters of its new note.
 *
 * Inputs:
 * pVoice - pointer to voice to retarget
 *
 * Outputs:
 * None
 *----------------------------------------------------------------------------
*/
static EAS_BOOL VMRetargetStolenVoice (S_VOICE_MGR *pVoiceMgr, EAS_I32 voiceNum)
{
    EAS_U8 flags;
    S_SYNTH_CHANNEL *pMIDIChannel;
    S_SYNTH_VOICE *pVoice;
    S_SYNTH *pSynth;
    S_SYNTH *pNextSynth;

    /* establish some pointers */
    pVoice = &pVoiceMgr->voices[voiceNum];
    pSynth = pVoiceMgr->pSynth[GET_VSYNTH(pVoice->channel)];
    pMIDIChannel = &pSynth->channels[pVoice->channel & 15];
    pNextSynth = pVoiceMgr->pSynth[GET_VSYNTH(pVoice->nextChannel)];

#ifdef _DEBUG_VM
{ /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMRetargetStolenVoice: retargeting stolen voice %d on channel %d\n",
        voiceNum, pVoice->channel); */ }

    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "\to channel %d note: %d velocity: %d\n",
        pVoice->nextChannel, pVoice->nextNote, pVoice->nextVelocity); */ }
#endif

    /* make sure new channel hasn't been muted by SP-MIDI since the voice was stolen */
    if ((pSynth->synthFlags & SYNTH_FLAG_SP_MIDI_ON) &&
        (pMIDIChannel->channelFlags & CHANNEL_FLAG_MUTE))
    {
        VMFreeVoice(pVoiceMgr, pSynth, &pVoiceMgr->voices[voiceNum]);
        return EAS_FALSE;
    }

    /* if assigned to a new synth, correct the active voice count */
    if (pVoice->channel != pVoice->nextChannel)
    {
#ifdef _DEBUG_VM
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMRetargetStolenVoice: Note assigned to different virtual synth, adjusting numActiveVoices\n"); */ }
#endif
        pSynth->numActiveVoices--;
        pNextSynth->numActiveVoices++;
    }

    /* assign new channel number, and increase channel voice count */
    pVoice->channel = pVoice->nextChannel;
    pMIDIChannel = &pNextSynth->channels[pVoice->channel & 15];

    /* assign other data */
    pVoice->note = pVoice->nextNote;
    pVoice->velocity = pVoice->nextVelocity;
    pVoice->nextChannel = UNASSIGNED_SYNTH_CHANNEL;
    pVoice->regionIndex = pVoice->nextRegionIndex;

    /* save the flags, pfStartVoice() will clear them */
    flags = pVoice->voiceFlags;

    /* keep track of the note-start related workload */
    pVoiceMgr->workload += WORKLOAD_AMOUNT_START_NOTE;

    /* setup the voice parameters */
    pVoice->voiceState = eVoiceStateStart;

    /*lint -e{522} return not used at this time */
    GetSynthPtr(voiceNum)->pfStartVoice(pVoiceMgr, pNextSynth, &pVoiceMgr->voices[voiceNum], GetAdjustedVoiceNum(voiceNum), pVoice->regionIndex);

    /* did the new note already receive a MIDI note-off request? */
    if (flags & VOICE_FLAG_DEFER_MIDI_NOTE_OFF)
    {
#ifdef _DEBUG_VM
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMRetargetVoice: stolen note note-off request deferred\n"); */ }
#endif
        pVoice->voiceFlags |= VOICE_FLAG_DEFER_MIDI_NOTE_OFF;
        pNextSynth->synthFlags |= SYNTH_FLAG_DEFERRED_MIDI_NOTE_OFF_PENDING;
    }

    return EAS_TRUE;
}

/*----------------------------------------------------------------------------
 * VMCheckKeyGroup()
 *----------------------------------------------------------------------------
 * If the note that we've been asked to start is in the same key group as
 * any currently playing notes, then we must shut down the currently playing
 * note in the same key group
 *----------------------------------------------------------------------------
*/
void VMCheckKeyGroup (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U16 keyGroup, EAS_U8 channel)
{
    const S_REGION *pRegion;
    EAS_INT voiceNum;

    /* increment frame workload */
    pVoiceMgr->workload += WORKLOAD_AMOUNT_KEY_GROUP;

    /* need to check all voices in case this is a layered sound */
    channel = VSynthToChannel(pSynth, channel);
    for (voiceNum = 0; voiceNum < MAX_SYNTH_VOICES; voiceNum++)
    {
        if (pVoiceMgr->voices[voiceNum].voiceState != eVoiceStateStolen)
        {
            /* voice must be on the same channel */
            if (channel == pVoiceMgr->voices[voiceNum].channel)
            {
                /* check key group */
                pRegion = GetRegionPtr(pSynth, pVoiceMgr->voices[voiceNum].regionIndex);
                if (keyGroup == (pRegion->keyGroupAndFlags & 0x0f00))
                {
#ifdef _DEBUG_VM
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMCheckKeyGroup: voice %d matches key group %d\n", voiceNum, keyGroup >> 8); */ }
#endif

                    /* if this voice was just started, set it to mute on the next buffer */
                    if (pVoiceMgr->voices[voiceNum].voiceFlags & VOICE_FLAG_NO_SAMPLES_SYNTHESIZED_YET)
                        pVoiceMgr->voices[voiceNum].voiceFlags |= VOICE_FLAG_DEFER_MUTE;

                    /* mute immediately */
                    else
                        VMMuteVoice(pVoiceMgr, voiceNum);
                }
            }
        }

        /* for stolen voice, check new values */
        else
        {
            /* voice must be on the same channel */
            if (channel == pVoiceMgr->voices[voiceNum].nextChannel)
            {
                /* check key group */
                pRegion = GetRegionPtr(pSynth, pVoiceMgr->voices[voiceNum].nextRegionIndex);
                if (keyGroup == (pRegion->keyGroupAndFlags & 0x0f00))
                {
#ifdef _DEBUG_VM
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMCheckKeyGroup: voice %d matches key group %d\n", voiceNum, keyGroup >> 8); */ }
#endif

                    /* if this voice was just started, set it to mute on the next buffer */
                    if (pVoiceMgr->voices[voiceNum].voiceFlags & VOICE_FLAG_NO_SAMPLES_SYNTHESIZED_YET)
                        pVoiceMgr->voices[voiceNum].voiceFlags |= VOICE_FLAG_DEFER_MUTE;

                    /* mute immediately */
                    else
                        VMMuteVoice(pVoiceMgr, voiceNum);
                }
            }

        }
    }
}

/*----------------------------------------------------------------------------
 * VMCheckPolyphonyLimiting()
 *----------------------------------------------------------------------------
 * Purpose:
 * We only play at most 2 of the same note on a MIDI channel.
 * E.g., if we are asked to start note 36, and there are already two voices
 * that are playing note 36, then we must steal the voice playing
 * the oldest note 36 and use that stolen voice to play the new note 36.
 *
 * Inputs:
 * nChannel - synth channel that wants to start a new note
 * nKeyNumber - new note's midi note number
 * nNoteVelocity - new note's velocity
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * pbVoiceStealingRequired - flag: this routine sets true if we needed to
 *                                 steal a voice
 * *
 * Side Effects:
 * psSynthObject->m_sVoice[free voice num].m_nKeyNumber may be assigned
 * psSynthObject->m_sVoice[free voice num].m_nVelocity may be assigned
 *----------------------------------------------------------------------------
*/
EAS_BOOL VMCheckPolyphonyLimiting (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 note, EAS_U8 velocity, EAS_U16 regionIndex, EAS_I32 lowVoice, EAS_I32 highVoice)
{
    EAS_INT voiceNum;
    EAS_INT oldestVoiceNum;
    EAS_INT numVoicesPlayingNote;
    EAS_U16 age;
    EAS_U16 oldestNoteAge;

    pVoiceMgr->workload += WORKLOAD_AMOUNT_POLY_LIMIT;

    numVoicesPlayingNote = 0;
    oldestVoiceNum = MAX_SYNTH_VOICES;
    oldestNoteAge = 0;
    channel = VSynthToChannel(pSynth, channel);

    /* examine each voice on this channel playing this note */
    for (voiceNum = lowVoice; voiceNum <= highVoice; voiceNum++)
    {
        /* check stolen notes separately */
        if (pVoiceMgr->voices[voiceNum].voiceState != eVoiceStateStolen)
        {

            /* same channel and note ? */
            if ((channel == pVoiceMgr->voices[voiceNum].channel) && (note == pVoiceMgr->voices[voiceNum].note))
            {
                numVoicesPlayingNote++;
                age = pVoiceMgr->age - pVoiceMgr->voices[voiceNum].age;

                /* is this the oldest voice for this note? */
                if (age >= oldestNoteAge)
                {
                    oldestNoteAge = age;
                    oldestVoiceNum = voiceNum;
                }
            }
        }

        /* handle stolen voices */
        else
        {
            /* same channel and note ? */
            if ((channel == pVoiceMgr->voices[voiceNum].nextChannel) && (note == pVoiceMgr->voices[voiceNum].nextNote))
            {
                numVoicesPlayingNote++;
            }
        }
    }

    /* check to see if we exceeded poly limit */
    if (numVoicesPlayingNote < DEFAULT_CHANNEL_POLYPHONY_LIMIT)
        return EAS_FALSE;

    /* make sure we have a voice to steal */
    if (oldestVoiceNum != MAX_SYNTH_VOICES)
    {
#ifdef _DEBUG_VM
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMCheckPolyphonyLimiting: voice %d has the oldest note\n", oldestVoiceNum); */ }
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "VMCheckPolyphonyLimiting: polyphony limiting requires shutting down note %d \n", pVoiceMgr->voices[oldestVoiceNum].note); */ }
#endif
        VMStolenVoice(pVoiceMgr, pSynth, oldestVoiceNum, channel, note, velocity, regionIndex);
        return EAS_TRUE;
    }

#ifdef _DEBUG_VM
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "VMCheckPolyphonyLimiting: No oldest voice to steal\n"); */ }
#endif
    return EAS_FALSE;
}

/*----------------------------------------------------------------------------
 * VMStartVoice()
 *----------------------------------------------------------------------------
 * Starts a voice given a region index
 *----------------------------------------------------------------------------
*/
void VMStartVoice (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 note, EAS_U8 velocity, EAS_U16 regionIndex)
{
    const S_REGION *pRegion;
    S_SYNTH_CHANNEL *pChannel;
    EAS_INT voiceNum;
    EAS_INT maxSynthPoly;
    EAS_I32 lowVoice, highVoice;
    EAS_U16 keyGroup;

    pChannel = &pSynth->channels[channel];
    pRegion = GetRegionPtr(pSynth, regionIndex);

    /* select correct synth */
#if defined(_SECONDARY_SYNTH) || defined(EAS_SPLIT_WT_SYNTH)
    {
#ifdef EAS_SPLIT_WT_SYNTH
        if ((pRegion->keyGroupAndFlags & REGION_FLAG_OFF_CHIP) == 0)
#else
        if ((regionIndex & FLAG_RGN_IDX_FM_SYNTH) == 0)
#endif
        {
            lowVoice = 0;
            highVoice = NUM_PRIMARY_VOICES - 1;
        }
        else
        {
            lowVoice = NUM_PRIMARY_VOICES;
            highVoice = MAX_SYNTH_VOICES - 1;
        }
    }
#else
    lowVoice = 0;
    highVoice = MAX_SYNTH_VOICES - 1;
#endif

    /* keep track of the note-start related workload */
    pVoiceMgr->workload+= WORKLOAD_AMOUNT_START_NOTE;

    /* other voices in pool, check for key group and poly limiting */
    if (pSynth->poolCount[pChannel->pool] != 0)
    {

        /* check for key group exclusivity */
        keyGroup = pRegion->keyGroupAndFlags & 0x0f00;
        if (keyGroup!= 0)
            VMCheckKeyGroup(pVoiceMgr, pSynth, keyGroup, channel);

        /* check polyphony limit and steal a voice if necessary */
        if ((pRegion->keyGroupAndFlags & REGION_FLAG_NON_SELF_EXCLUSIVE) == 0)
        {
            if (VMCheckPolyphonyLimiting(pVoiceMgr, pSynth, channel, note, velocity, regionIndex, lowVoice, highVoice) == EAS_TRUE)
                return;
        }
    }

    /* check max poly allocation */
    if ((pSynth->maxPolyphony == 0) || (pVoiceMgr->maxPolyphony < pSynth->maxPolyphony))
        maxSynthPoly = pVoiceMgr->maxPolyphony;
    else
        maxSynthPoly = pSynth->maxPolyphony;

    /* any free voices? */
    if ((pVoiceMgr->activeVoices < pVoiceMgr->maxPolyphony) &&
        (pSynth->numActiveVoices < maxSynthPoly) &&
        (EAS_SUCCESS == VMFindAvailableVoice(pVoiceMgr, &voiceNum, lowVoice, highVoice)))
    {
        S_SYNTH_VOICE *pVoice = &pVoiceMgr->voices[voiceNum];

#ifdef _DEBUG_VM
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "VMStartVoice: Synth=%d\n", pSynth->vSynthNum); */ }
#endif

        /* bump voice counts */
        pVoiceMgr->activeVoices++;
        pSynth->numActiveVoices++;

#ifdef _DEBUG_VM
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMStartVoice: voice %d assigned to channel %d note %d velocity %d\n",
            voiceNum, channel, note, velocity); */ }
#endif

        /* save parameters */
        pVoiceMgr->voices[voiceNum].channel = VSynthToChannel(pSynth, channel);
        pVoiceMgr->voices[voiceNum].note = note;
        pVoiceMgr->voices[voiceNum].velocity = velocity;

        /* establish note age for voice stealing */
        pVoiceMgr->voices[voiceNum].age = pVoiceMgr->age++;

        /* setup the synthesis parameters */
        pVoiceMgr->voices[voiceNum].voiceState = eVoiceStateStart;

        /* increment voice pool count */
        IncVoicePoolCount(pVoiceMgr, pVoice);

        /* start voice on correct synth */
        /*lint -e{522} return not used at this time */
        GetSynthPtr(voiceNum)->pfStartVoice(pVoiceMgr, pSynth, &pVoiceMgr->voices[voiceNum], GetAdjustedVoiceNum(voiceNum), regionIndex);
        return;
    }

    /* no free voices, we have to steal one using appropriate algorithm */
    if (VMStealVoice(pVoiceMgr, pSynth, &voiceNum, channel, note, lowVoice, highVoice) == EAS_SUCCESS)
        VMStolenVoice(pVoiceMgr, pSynth, voiceNum, channel, note, velocity, regionIndex);

#ifdef _DEBUG_VM
    else
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMStartVoice: Could not steal a voice for channel %d note %d velocity %d\n",
            channel, note, velocity); */ }
    }
#endif

    return;
}

/*----------------------------------------------------------------------------
 * VMStartNote()
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the synth's state to play the requested note on the requested
 * channel if possible.
 *
 * Inputs:
 * nChannel - the channel to start a note on
 * nKeyNumber - the key number to start a note for
 * nNoteVelocity - the key velocity from this note
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * Side Effects:
 * psSynthObject->m_nNumActiveVoices may be incremented
 * psSynthObject->m_sVoice[free voice num].m_nSynthChannel may be assigned
 * psSynthObject->m_sVoice[free voice num].m_nKeyNumber is assigned
 * psSynthObject->m_sVoice[free voice num].m_nVelocity is assigned
 *----------------------------------------------------------------------------
*/
void VMStartNote (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 note, EAS_U8 velocity)
{
    S_SYNTH_CHANNEL *pChannel;
    EAS_U16 regionIndex;
    EAS_I16 adjustedNote;

    /* bump note count */
    pSynth->totalNoteCount++;

    pChannel = &pSynth->channels[channel];

    /* check channel mute */
    if (pChannel->channelFlags & CHANNEL_FLAG_MUTE)
        return;

#ifdef EXTERNAL_AUDIO
    /* pass event to external audio when requested */
    if ((pChannel->channelFlags & CHANNEL_FLAG_EXTERNAL_AUDIO) && (pSynth->cbEventFunc != NULL))
    {
        S_EXT_AUDIO_EVENT event;
        event.channel = channel;
        event.note = note;
        event.velocity = velocity;
        event.noteOn = EAS_TRUE;
        if (pSynth->cbEventFunc(pSynth->pExtAudioInstData, &event))
            return;
    }
#endif

    /* start search at first region */
    regionIndex = pChannel->regionIndex;

    /* handle transposition */
    adjustedNote = note;
    if (pChannel->channelFlags & CHANNEL_FLAG_RHYTHM_CHANNEL)
        adjustedNote += pChannel->coarsePitch;
    else
        adjustedNote += pChannel->coarsePitch + pSynth->globalTranspose;

    /* limit adjusted key number so it does not wraparound, over/underflow */
    if (adjustedNote < 0)
    {
        adjustedNote = 0;
    }
    else if (adjustedNote > 127)
    {
        adjustedNote = 127;
    }

#if defined(DLS_SYNTHESIZER)
    if (regionIndex & FLAG_RGN_IDX_DLS_SYNTH)
    {
        /* DLS voice */
        for (;;)
        {
            /*lint -e{740,826} cast OK, we know this is actually a DLS region */
            const S_DLS_REGION *pDLSRegion = (S_DLS_REGION*) GetRegionPtr(pSynth, regionIndex);

            /* check key against this region's key and velocity range */
            if (((adjustedNote >= pDLSRegion->wtRegion.region.rangeLow) && (adjustedNote <= pDLSRegion->wtRegion.region.rangeHigh)) &&
                ((velocity >= pDLSRegion->velLow) && (velocity <= pDLSRegion->velHigh)))
            {
                VMStartVoice(pVoiceMgr, pSynth, channel, note, velocity, regionIndex);
            }

            /* last region in program? */
            if (pDLSRegion->wtRegion.region.keyGroupAndFlags & REGION_FLAG_LAST_REGION)
                break;

            /* advance to next region */
            regionIndex++;
        }
    }
    else
#endif

    /* braces here for #if clause */
    {
        /* EAS voice */
        for (;;)
        {
            const S_REGION *pRegion = GetRegionPtr(pSynth, regionIndex);

            /* check key against this region's keyrange */
            if ((adjustedNote >= pRegion->rangeLow) && (adjustedNote <= pRegion->rangeHigh))
            {
                VMStartVoice(pVoiceMgr, pSynth, channel, note, velocity, regionIndex);
                break;
            }

            /* last region in program? */
            if (pRegion->keyGroupAndFlags & REGION_FLAG_LAST_REGION)
                break;

            /* advance to next region */
            regionIndex++;
        }
    }
}

/*----------------------------------------------------------------------------
 * VMStopNote()
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the synth's state to end the requested note on the requested
 * channel.
 *
 * Inputs:
 * nChannel - the channel to stop a note on
 * nKeyNumber - the key number for this note off
 * nNoteVelocity - the note-off velocity
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * Side Effects:
 * psSynthObject->m_sVoice[free voice num].m_nSynthChannel may be assigned
 * psSynthObject->m_sVoice[free voice num].m_nKeyNumber is assigned
 * psSynthObject->m_sVoice[free voice num].m_nVelocity is assigned
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, velocity) reserved for future use */
void VMStopNote (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 note, EAS_U8 velocity)
{
    S_SYNTH_CHANNEL *pChannel;
    EAS_INT voiceNum;

    pChannel = &(pSynth->channels[channel]);

#ifdef EXTERNAL_AUDIO
    if ((pChannel->channelFlags & CHANNEL_FLAG_EXTERNAL_AUDIO) && (pSynth->cbEventFunc != NULL))
    {
        S_EXT_AUDIO_EVENT event;
        event.channel = channel;
        event.note = note;
        event.velocity = velocity;
        event.noteOn = EAS_FALSE;
        if (pSynth->cbEventFunc(pSynth->pExtAudioInstData, &event))
            return;
    }
#endif

    /* keep track of the note-start workload */
    pVoiceMgr->workload += WORKLOAD_AMOUNT_STOP_NOTE;

    channel = VSynthToChannel(pSynth, channel);

    for (voiceNum=0; voiceNum < MAX_SYNTH_VOICES; voiceNum++)
    {

        /* stolen notes are handled separately */
        if (eVoiceStateStolen != pVoiceMgr->voices[voiceNum].voiceState)
        {

            /* channel and key number must match */
            if ((channel == pVoiceMgr->voices[voiceNum].channel) && (note == pVoiceMgr->voices[voiceNum].note))
            {
#ifdef _DEBUG_VM
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMStopNote: voice %d channel %d note %d\n",
                    voiceNum, channel, note); */ }
#endif

                /* if sustain pedal is down, set deferred note-off flag */
                if (pChannel->channelFlags & CHANNEL_FLAG_SUSTAIN_PEDAL)
                {
                    pVoiceMgr->voices[voiceNum].voiceFlags |= VOICE_FLAG_SUSTAIN_PEDAL_DEFER_NOTE_OFF;
                    continue;
                }

                /* if this note just started, wait before we stop it */
                if (pVoiceMgr->voices[voiceNum].voiceFlags & VOICE_FLAG_NO_SAMPLES_SYNTHESIZED_YET)
                {
#ifdef _DEBUG_VM
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "\tDeferred: Not started yet\n"); */ }
#endif
                    pVoiceMgr->voices[voiceNum].voiceFlags |= VOICE_FLAG_DEFER_MIDI_NOTE_OFF;
                    pSynth->synthFlags |= SYNTH_FLAG_DEFERRED_MIDI_NOTE_OFF_PENDING;
                }

                /* release voice */
                else
                    VMReleaseVoice(pVoiceMgr, pSynth, voiceNum);

            }
        }

        /* process stolen notes, new channel and key number must match */
        else if ((channel == pVoiceMgr->voices[voiceNum].nextChannel) && (note == pVoiceMgr->voices[voiceNum].nextNote))
        {

#ifdef _DEBUG_VM
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMStopNote: voice %d channel %d note %d\n\tDeferred: Stolen voice\n",
                voiceNum, channel, note); */ }
#endif
            pVoiceMgr->voices[voiceNum].voiceFlags |= VOICE_FLAG_DEFER_MIDI_NOTE_OFF;
        }
    }
}

/*----------------------------------------------------------------------------
 * VMFindAvailableVoice()
 *----------------------------------------------------------------------------
 * Purpose:
 * Find an available voice and return the voice number if available.
 *
 * Inputs:
 * pnVoiceNumber - really an output, returns the voice number found
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * success - if there is an available voice
 * failure - otherwise
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMFindAvailableVoice (S_VOICE_MGR *pVoiceMgr, EAS_INT *pVoiceNumber, EAS_I32 lowVoice, EAS_I32 highVoice)
{
    EAS_INT voiceNum;

    /* Check each voice to see if it has been assigned to a synth channel */
    for (voiceNum = lowVoice; voiceNum <= highVoice; voiceNum++)
    {
        /* check if this voice has been assigned to a synth channel */
        if ( pVoiceMgr->voices[voiceNum].voiceState == eVoiceStateFree)
        {
            *pVoiceNumber = voiceNum;       /* this voice is available */
            return EAS_SUCCESS;
        }
    }

    /* if we reach here, we have not found a free voice */
    *pVoiceNumber = UNASSIGNED_SYNTH_VOICE;

#ifdef _DEBUG_VM
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMFindAvailableVoice: error, could not find an available voice\n"); */ }
#endif
    return EAS_FAILURE;
}

/*----------------------------------------------------------------------------
 * VMStealVoice()
 *----------------------------------------------------------------------------
 * Purpose:
 * Steal a voice and return the voice number
 *
 * Stealing algorithm: steal the best choice with minimal work, taking into
 * account SP-Midi channel priorities and polyphony allocation.
 *
 * In one pass through all the voices, figure out which voice to steal
 * taking into account a number of different factors:
 * Priority of the voice's MIDI channel
 * Number of voices over the polyphony allocation for voice's MIDI channel
 * Amplitude of the voice
 * Note age
 * Key velocity (for voices that haven't been started yet)
 * If any matching notes are found
 *
 * Inputs:
 * pnVoiceNumber - really an output, see below
 * nChannel - the channel that this voice wants to be started on
 * nKeyNumber - the key number for this new voice
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * pnVoiceNumber - voice number of the voice that was stolen
 * EAS_RESULT EAS_SUCCESS - always successful
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMStealVoice (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_INT *pVoiceNumber, EAS_U8 channel, EAS_U8 note, EAS_I32 lowVoice, EAS_I32 highVoice)
{
    S_SYNTH_VOICE *pCurrVoice;
    S_SYNTH *pCurrSynth;
    EAS_INT voiceNum;
    EAS_INT bestCandidate;
    EAS_U8 currChannel;
    EAS_U8 currNote;
    EAS_I32 bestPriority;
    EAS_I32 currentPriority;

    /* determine which voice to steal */
    bestPriority = 0;
    bestCandidate = MAX_SYNTH_VOICES;

    for (voiceNum = lowVoice; voiceNum <= highVoice; voiceNum++)
    {
        pCurrVoice = &pVoiceMgr->voices[voiceNum];

        /* ignore free voices */
        if (pCurrVoice->voiceState == eVoiceStateFree)
            continue;

        /* for stolen voices, use the new parameters, not the old */
        if (pCurrVoice->voiceState == eVoiceStateStolen)
        {
            pCurrSynth = pVoiceMgr->pSynth[GET_VSYNTH(pCurrVoice->nextChannel)];
            currChannel = pCurrVoice->nextChannel;
            currNote = pCurrVoice->nextNote;
        }
        else
        {
            pCurrSynth = pVoiceMgr->pSynth[GET_VSYNTH(pCurrVoice->channel)];
            currChannel = pCurrVoice->channel;
            currNote = pCurrVoice->note;
        }

        /* ignore voices that are higher priority */
        if (pSynth->priority > pCurrSynth->priority)
            continue;
#ifdef _DEBUG_VM
//      { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMStealVoice: New priority = %d exceeds old priority = %d\n", pSynth->priority, pCurrSynth->priority); */ }
#endif

        /* if voice is stolen or just started, reduce the likelihood it will be stolen */
        if (( pCurrVoice->voiceState == eVoiceStateStolen) || (pCurrVoice->voiceFlags & VOICE_FLAG_NO_SAMPLES_SYNTHESIZED_YET))
        {
            currentPriority = 128 - pCurrVoice->nextVelocity;
        }
        else
        {
            /* compute the priority of this voice, higher means better for stealing */
            /* use not age */
            currentPriority = (EAS_I32) pCurrVoice->age << NOTE_AGE_STEAL_WEIGHT;

            /* include note gain -higher gain is lower steal value */
            /*lint -e{704} use shift for performance */
            currentPriority += ((32768 >> (12 - NOTE_GAIN_STEAL_WEIGHT)) + 256) -
                ((EAS_I32) pCurrVoice->gain >> (12 - NOTE_GAIN_STEAL_WEIGHT));
        }

        /* in SP-MIDI mode, include over poly allocation and channel priority */
        if (pSynth->synthFlags & SYNTH_FLAG_SP_MIDI_ON)
        {
            S_SYNTH_CHANNEL *pChannel = &pCurrSynth->channels[GET_CHANNEL(currChannel)];
            /*lint -e{701} use shift for performance */
            if (pSynth->poolCount[pChannel->pool] >= pSynth->poolAlloc[pChannel->pool])
                currentPriority += (pSynth->poolCount[pChannel->pool] -pSynth->poolAlloc[pChannel->pool] + 1) << CHANNEL_POLY_STEAL_WEIGHT;

            /* include channel priority */
            currentPriority += (EAS_I32)(pChannel->pool << CHANNEL_PRIORITY_STEAL_WEIGHT);
        }

        /* if a note is already playing that matches this note, consider stealing it more readily */
        if ((note == currNote) && (channel == currChannel))
            currentPriority += NOTE_MATCH_PENALTY;

        /* is this the best choice so far? */
        if (currentPriority >= bestPriority)
        {
            bestPriority = currentPriority;
            bestCandidate = voiceNum;
        }
    }

    /* may happen if all voices are allocated to a higher priority virtual synth */
    if (bestCandidate == MAX_SYNTH_VOICES)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMStealVoice: Unable to allocate a voice\n"); */ }
        return EAS_ERROR_NO_VOICE_ALLOCATED;
    }

#ifdef _DEBUG_VM
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMStealVoice: Voice %d stolen\n", bestCandidate); */ }

    /* are we stealing a stolen voice? */
    if (pVoiceMgr->voices[bestCandidate].voiceState == eVoiceStateStolen)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "VMStealVoice: Voice %d is already marked as stolen and was scheduled to play ch: %d note: %d vel: %d\n",
            bestCandidate,
            pVoiceMgr->voices[bestCandidate].nextChannel,
            pVoiceMgr->voices[bestCandidate].nextNote,
            pVoiceMgr->voices[bestCandidate].nextVelocity); */ }
    }
#endif

    *pVoiceNumber = (EAS_U16) bestCandidate;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * VMChannelPressure()
 *----------------------------------------------------------------------------
 * Purpose:
 * Change the channel pressure for the given channel
 *
 * Inputs:
 * nChannel - the MIDI channel
 * nVelocity - the channel pressure value
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * Side Effects:
 * psSynthObject->m_sChannel[nChannel].m_nChannelPressure is updated
 *----------------------------------------------------------------------------
*/
void VMChannelPressure (S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 value)
{
    S_SYNTH_CHANNEL *pChannel;

    pChannel = &(pSynth->channels[channel]);
    pChannel->channelPressure = value;

    /*
    set a channel flag to request parameter updates
    for all the voices associated with this channel
    */
    pChannel->channelFlags |= CHANNEL_FLAG_UPDATE_CHANNEL_PARAMETERS;
}

/*----------------------------------------------------------------------------
 * VMPitchBend()
 *----------------------------------------------------------------------------
 * Purpose:
 * Change the pitch wheel value for the given channel.
 * This routine constructs the proper 14-bit argument when the calling routine
 * passes the pitch LSB and MSB.
 *
 * Note: some midi disassemblers display a bipolar pitch bend value.
 * We can display the bipolar value using
 * if m_nPitchBend >= 0x2000
 *      bipolar pitch bend = postive (m_nPitchBend - 0x2000)
 * else
 *      bipolar pitch bend = negative (0x2000 - m_nPitchBend)
 *
 * Inputs:
 * nChannel - the MIDI channel
 * nPitchLSB - the LSB byte of the pitch bend message
 * nPitchMSB - the MSB byte of the pitch bend message
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 * psSynthObject->m_sChannel[nChannel].m_nPitchBend is changed
 *
 *----------------------------------------------------------------------------
*/
void VMPitchBend (S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 nPitchLSB, EAS_U8 nPitchMSB)
{
    S_SYNTH_CHANNEL *pChannel;

    pChannel = &(pSynth->channels[channel]);
    pChannel->pitchBend = (EAS_I16) ((nPitchMSB << 7) | nPitchLSB);

    /*
    set a channel flag to request parameter updates
    for all the voices associated with this channel
    */
    pChannel->channelFlags |= CHANNEL_FLAG_UPDATE_CHANNEL_PARAMETERS;
}

/*----------------------------------------------------------------------------
 * VMControlChange()
 *----------------------------------------------------------------------------
 * Purpose:
 * Change the controller (or mode) for the given channel.
 *
 * Inputs:
 * nChannel - the MIDI channel
 * nControllerNumber - the MIDI controller number
 * nControlValue - the value for this controller message
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * Side Effects:
 * psSynthObject->m_sChannel[nChannel] controller is changed
 *
 *----------------------------------------------------------------------------
*/
void VMControlChange (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 controller, EAS_U8 value)
{
    S_SYNTH_CHANNEL *pChannel;

    pChannel = &(pSynth->channels[channel]);

    /*
    set a channel flag to request parameter updates
    for all the voices associated with this channel
    */
    pChannel->channelFlags |= CHANNEL_FLAG_UPDATE_CHANNEL_PARAMETERS;

    switch ( controller )
    {
    case MIDI_CONTROLLER_BANK_SELECT_MSB:
#ifdef _DEBUG_VM
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMControlChange: Bank Select MSB: msb 0x%X\n", value); */ }
#endif
        /* use this MSB with a zero LSB, until we get an LSB message */
        pChannel->bankNum = value << 8;
        break;

    case MIDI_CONTROLLER_MOD_WHEEL:
        /* we treat mod wheel as a 7-bit controller and only use the MSB */
        pChannel->modWheel = value;
        break;

    case MIDI_CONTROLLER_VOLUME:
        /* we treat volume as a 7-bit controller and only use the MSB */
        pChannel->volume = value;
        break;

    case MIDI_CONTROLLER_PAN:
        /* we treat pan as a 7-bit controller and only use the MSB */
        pChannel->pan = value;
        break;

    case MIDI_CONTROLLER_EXPRESSION:
        /* we treat expression as a 7-bit controller and only use the MSB */
        pChannel->expression = value;
        break;

    case MIDI_CONTROLLER_BANK_SELECT_LSB:
#ifdef _DEBUG_VM
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMControlChange: Bank Select LSB: lsb 0x%X\n", value); */ }
#endif
        /*
        construct bank number as 7-bits (stored as 8) of existing MSB
        and 7-bits of new LSB (also stored as 8(
        */
        pChannel->bankNum =
            (pChannel->bankNum & 0xFF00) | value;

        break;

    case MIDI_CONTROLLER_SUSTAIN_PEDAL:
        /* we treat sustain pedal as a boolean on/off bit flag */
        if (value < 64)
        {
            /*
            we are requested to turn the pedal off, but first check
            if the pedal is already on
            */
            if (0 !=
                (pChannel->channelFlags & CHANNEL_FLAG_SUSTAIN_PEDAL)
               )
            {
                /*
                The sustain flag is presently set and the damper pedal is on.
                We are therefore transitioning from damper pedal ON to
                damper pedal OFF. This means all notes in this channel
                that received a note off while the damper pedal was on, and
                had their note-off requests deferred, should now proceed to
                the release state.
                */
                VMReleaseAllDeferredNoteOffs(pVoiceMgr, pSynth, channel);
            }   /* end if sustain pedal is already on */

            /* turn the sustain pedal off */
            pChannel->channelFlags &= ~CHANNEL_FLAG_SUSTAIN_PEDAL;
        }
        else
        {
            /*
            we are requested to turn the pedal on, but first check
            if the pedal is already off
            */
            if (0 ==
                (pChannel->channelFlags & CHANNEL_FLAG_SUSTAIN_PEDAL)
               )
            {
                /*
                The sustain flag is presently clear and the damper pedal is off.
                We are therefore transitioning from damper pedal OFF to
                damper pedal ON. Currently sounding notes should be left
                unchanged. However, we should try to "catch" notes if possible.
                If any notes have levels >= sustain level, catch them,
                otherwise, let them continue to release.
                */
                VMCatchNotesForSustainPedal(pVoiceMgr, pSynth, channel);
            }

            /* turn the sustain pedal on */
            pChannel->channelFlags |= CHANNEL_FLAG_SUSTAIN_PEDAL;
        }

        break;
#ifdef _REVERB
    case MIDI_CONTROLLER_REVERB_SEND:
        /* we treat send as a 7-bit controller and only use the MSB */
        pSynth->channels[channel].reverbSend = value;
        break;
#endif
#ifdef _CHORUS
    case MIDI_CONTROLLER_CHORUS_SEND:
        /* we treat send as a 7-bit controller and only use the MSB */
        pSynth->channels[channel].chorusSend = value;
        break;
#endif
    case MIDI_CONTROLLER_RESET_CONTROLLERS:
        /* despite the Midi message name, not ALL controllers are reset */
        pChannel->modWheel = DEFAULT_MOD_WHEEL;
        pChannel->expression = DEFAULT_EXPRESSION;

        /* turn the sustain pedal off as default/reset */
        pChannel->channelFlags &= ~CHANNEL_FLAG_SUSTAIN_PEDAL;
        pChannel->pitchBend = DEFAULT_PITCH_BEND;

        /* reset channel pressure */
        pChannel->channelPressure = DEFAULT_CHANNEL_PRESSURE;

        /* reset RPN values */
        pChannel->registeredParam = DEFAULT_REGISTERED_PARAM;
        pChannel->pitchBendSensitivity = DEFAULT_PITCH_BEND_SENSITIVITY;
        pChannel->finePitch = DEFAULT_FINE_PITCH;
        pChannel->coarsePitch = DEFAULT_COARSE_PITCH;

        /*
        program change, bank select, channel volume CC7, pan CC10
        are NOT reset
        */
        break;

    /*
    For logical reasons, the RPN data entry are grouped together.
    However, keep in mind that these cases are not necessarily in
    ascending order.
    e.g., MIDI_CONTROLLER_DATA_ENTRY_MSB == 6,
    whereas MIDI_CONTROLLER_SUSTAIN_PEDAL == 64.
    So arrange these case statements in whatever manner is more efficient for
    the processor / compiler.
    */
    case MIDI_CONTROLLER_ENTER_DATA_MSB:
    case MIDI_CONTROLLER_ENTER_DATA_LSB:
    case MIDI_CONTROLLER_SELECT_RPN_LSB:
    case MIDI_CONTROLLER_SELECT_RPN_MSB:
    case MIDI_CONTROLLER_SELECT_NRPN_MSB:
    case MIDI_CONTROLLER_SELECT_NRPN_LSB:
        VMUpdateRPNStateMachine(pSynth, channel, controller, value);
        break;

    case MIDI_CONTROLLER_ALL_SOUND_OFF:
    case MIDI_CONTROLLER_ALL_NOTES_OFF:
    case MIDI_CONTROLLER_OMNI_OFF:
    case MIDI_CONTROLLER_OMNI_ON:
    case MIDI_CONTROLLER_MONO_ON_POLY_OFF:
    case MIDI_CONTROLLER_POLY_ON_MONO_OFF:
        /* NOTE: we treat all sounds off the same as all notes off */
        VMAllNotesOff(pVoiceMgr, pSynth, channel);
        break;

    default:
#ifdef _DEBUG_VM
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMControlChange: controller %d not yet implemented\n", controller); */ }
#endif
        break;

    }

    return;
}

/*----------------------------------------------------------------------------
 * VMUpdateRPNStateMachine()
 *----------------------------------------------------------------------------
 * Purpose:
 * Call this function when we want to parse RPN related controller messages.
 * We only support RPN0 (pitch bend sensitivity), RPN1 (fine tuning) and
 * RPN2 (coarse tuning). Any other RPNs or NRPNs are ignored for now.
 *.
 * Supports any order, so not a state machine anymore. This function was
 * rewritten to work correctly regardless of order.
 *
 * Inputs:
 * nChannel - the channel this controller message is coming from
 * nControllerNumber - which RPN related controller
 * nControlValue - the value of the RPN related controller
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * returns EAS_RESULT, which is typically EAS_SUCCESS, since there are
 * few possible errors
 *
 * Side Effects:
 * gsSynthObject.m_sChannel[nChannel].m_nPitchBendSensitivity
 * (or m_nFinePitch or m_nCoarsePitch)
 * will be updated if the proper RPN message is received.
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMUpdateRPNStateMachine (S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 controller, EAS_U8 value)
{
    S_SYNTH_CHANNEL *pChannel;

#ifdef _DEBUG_VM
    if (channel >= NUM_SYNTH_CHANNELS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMUpdateRPNStateMachines: error, %d invalid channel number\n",
            channel); */ }
        return EAS_FAILURE;
    }
#endif

    pChannel = &(pSynth->channels[channel]);

    switch (controller)
    {
    case MIDI_CONTROLLER_SELECT_NRPN_MSB:
    case MIDI_CONTROLLER_SELECT_NRPN_LSB:
        pChannel->registeredParam = DEFAULT_REGISTERED_PARAM;
        break;
    case MIDI_CONTROLLER_SELECT_RPN_MSB:
        pChannel->registeredParam =
            (pChannel->registeredParam & 0x7F) | (value<<7);
        break;
    case MIDI_CONTROLLER_SELECT_RPN_LSB:
        pChannel->registeredParam =
            (pChannel->registeredParam & 0x7F00) | value;
        break;
    case MIDI_CONTROLLER_ENTER_DATA_MSB:
        switch (pChannel->registeredParam)
        {
        case 0:
            pChannel->pitchBendSensitivity = value * 100;
            break;
        case 1:
            /*lint -e{702} <avoid division for performance reasons>*/
            pChannel->finePitch = (EAS_I8)((((value << 7) - 8192) * 100) >> 13);
            break;
        case 2:
            pChannel->coarsePitch = (EAS_I8)(value - 64);
            break;
        default:
            break;
        }
        break;
    case MIDI_CONTROLLER_ENTER_DATA_LSB:
        switch (pChannel->registeredParam)
        {
        case 0:
            //ignore lsb
            break;
        case 1:
            //ignore lsb
            break;
        case 2:
            //ignore lsb
            break;
        default:
            break;
        }
        break;
    default:
        return EAS_FAILURE; //not a RPN related controller
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * VMUpdateStaticChannelParameters()
 *----------------------------------------------------------------------------
 * Purpose:
 * Update all of the static channel parameters for channels that have had
 * a controller change values
 * Or if the synth has signalled that all channels must forcibly
 * be updated
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * none
 *
 * Side Effects:
 * - psSynthObject->m_sChannel[].m_nStaticGain and m_nStaticPitch
 * are updated for channels whose controller values have changed
 * or if the synth has signalled that all channels must forcibly
 * be updated
 *----------------------------------------------------------------------------
*/
void VMUpdateStaticChannelParameters (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth)
{
    EAS_INT channel;

    if (pSynth->synthFlags & SYNTH_FLAG_UPDATE_ALL_CHANNEL_PARAMETERS)
    {
        /*
        the synth wants us to forcibly update all channel
        parameters. This event occurs when we are about to
        finish resetting the synth
        */
        for (channel = 0; channel < NUM_SYNTH_CHANNELS; channel++)
        {
#ifdef _HYBRID_SYNTH
            if (pSynth->channels[channel].regionIndex & FLAG_RGN_IDX_FM_SYNTH)
                pSecondarySynth->pfUpdateChannel(pVoiceMgr, pSynth, (EAS_U8) channel);
            else
                pPrimarySynth->pfUpdateChannel(pVoiceMgr, pSynth, (EAS_U8) channel);
#else
            pPrimarySynth->pfUpdateChannel(pVoiceMgr, pSynth, (EAS_U8) channel);
#endif
        }

        /*
        clear the flag to indicates we have now forcibly
        updated all channel parameters
        */
        pSynth->synthFlags &= ~SYNTH_FLAG_UPDATE_ALL_CHANNEL_PARAMETERS;
    }
    else
    {

        /* only update channel params if signalled by a channel flag */
        for (channel = 0; channel < NUM_SYNTH_CHANNELS; channel++)
        {
            if ( 0 != (pSynth->channels[channel].channelFlags & CHANNEL_FLAG_UPDATE_CHANNEL_PARAMETERS))
            {
#ifdef _HYBRID_SYNTH
                if (pSynth->channels[channel].regionIndex & FLAG_RGN_IDX_FM_SYNTH)
                    pSecondarySynth->pfUpdateChannel(pVoiceMgr, pSynth, (EAS_U8) channel);
                else
                    pPrimarySynth->pfUpdateChannel(pVoiceMgr, pSynth, (EAS_U8) channel);
#else
                pPrimarySynth->pfUpdateChannel(pVoiceMgr, pSynth, (EAS_U8) channel);
#endif
            }
        }

    }

    return;
}

/*----------------------------------------------------------------------------
 * VMFindProgram()
 *----------------------------------------------------------------------------
 * Purpose:
 * Look up an individual program in sound library. This function
 * searches the bank list for a program, then the individual program
 * list.
 *
 * Inputs:
 *
 * Outputs:
 *----------------------------------------------------------------------------
*/
static EAS_RESULT VMFindProgram (const S_EAS *pEAS, EAS_U32 bank, EAS_U8 programNum, EAS_U16 *pRegionIndex)
{
    EAS_U32 locale;
    const S_PROGRAM *p;
    EAS_U16 i;
    EAS_U16 regionIndex;

    /* make sure we have a valid sound library */
    if (pEAS == NULL)
        return EAS_FAILURE;

    /* search the banks */
    for (i = 0; i <  pEAS->numBanks; i++)
    {
        if (bank == (EAS_U32) pEAS->pBanks[i].locale)
        {
            regionIndex = pEAS->pBanks[i].regionIndex[programNum];
            if (regionIndex != INVALID_REGION_INDEX)
            {
                *pRegionIndex = regionIndex;
                return EAS_SUCCESS;
            }
            break;
        }
    }

    /* establish locale */
    locale = ( bank << 8) | programNum;

    /* search for program */
    for (i = 0, p = pEAS->pPrograms; i < pEAS->numPrograms; i++, p++)
    {
        if (p->locale == locale)
        {
            *pRegionIndex = p->regionIndex;
            return EAS_SUCCESS;
        }
    }

    return EAS_FAILURE;
}

#ifdef DLS_SYNTHESIZER
/*----------------------------------------------------------------------------
 * VMFindDLSProgram()
 *----------------------------------------------------------------------------
 * Purpose:
 * Look up an individual program in sound library. This function
 * searches the bank list for a program, then the individual program
 * list.
 *
 * Inputs:
 *
 * Outputs:
 *----------------------------------------------------------------------------
*/
static EAS_RESULT VMFindDLSProgram (const S_DLS *pDLS, EAS_U32 bank, EAS_U8 programNum, EAS_U16 *pRegionIndex)
{
    EAS_U32 locale;
    const S_PROGRAM *p;
    EAS_U16 i;

    /* make sure we have a valid sound library */
    if (pDLS == NULL)
        return EAS_FAILURE;

    /* establish locale */
    locale = (bank << 8) | programNum;

    /* search for program */
    for (i = 0, p = pDLS->pDLSPrograms; i < pDLS->numDLSPrograms; i++, p++)
    {
        if (p->locale == locale)
        {
            *pRegionIndex = p->regionIndex;
            return EAS_SUCCESS;
        }
    }

    return EAS_FAILURE;
}
#endif

/*----------------------------------------------------------------------------
 * VMProgramChange()
 *----------------------------------------------------------------------------
 * Purpose:
 * Change the instrument (program) for the given channel.
 *
 * Depending on the program number, and the bank selected for this channel, the
 * program may be in ROM, RAM (from SMAF or CMX related RAM wavetable), or
 * Alternate wavetable (from mobile DLS or other DLS file)
 *
 * This function figures out what wavetable should be used, and sets it up as the
 * wavetable to use for this channel. Also the channel may switch from a melodic
 * channel to a rhythm channel, or vice versa.
 *
 * Inputs:
 *
 * Outputs:
 * Side Effects:
 * gsSynthObject.m_sChannel[nChannel].m_nProgramNumber is likely changed
 * gsSynthObject.m_sChannel[nChannel].m_psEAS may be changed
 * gsSynthObject.m_sChannel[nChannel].m_bRhythmChannel may be changed
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pVoiceMgr) reserved for future use */
void VMProgramChange (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 program)
{
    S_SYNTH_CHANNEL *pChannel;
    EAS_U32 bank;
    EAS_U16 regionIndex;

#ifdef _DEBUG_VM
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "VMProgramChange: vSynthNum=%d, channel=%d, program=%d\n", pSynth->vSynthNum, channel, program); */ }
#endif

    /* setup pointer to MIDI channel data */
    pChannel = &pSynth->channels[channel];
    bank = pChannel->bankNum;

    /* allow channels to switch between being melodic or rhythm channels, using GM2 CC values */
    if ((bank & 0xFF00) == DEFAULT_RHYTHM_BANK_NUMBER)
    {
        /* make it a rhythm channel */
        pChannel->channelFlags |= CHANNEL_FLAG_RHYTHM_CHANNEL;
    }
    else if ((bank & 0xFF00) == DEFAULT_MELODY_BANK_NUMBER)
    {
        /* make it a melody channel */
        pChannel->channelFlags &= ~CHANNEL_FLAG_RHYTHM_CHANNEL;
    }

    regionIndex = DEFAULT_REGION_INDEX;

#ifdef EXTERNAL_AUDIO
    /* give the external audio interface a chance to handle it */
    if (pSynth->cbProgChgFunc != NULL)
    {
        S_EXT_AUDIO_PRG_CHG prgChg;
        prgChg.channel = channel;
        prgChg.bank = (EAS_U16) bank;
        prgChg.program = program;
        if (pSynth->cbProgChgFunc(pSynth->pExtAudioInstData, &prgChg))
            pChannel->channelFlags |= CHANNEL_FLAG_EXTERNAL_AUDIO;
    }

#endif


#ifdef DLS_SYNTHESIZER
    /* first check for DLS program that may overlay the internal instrument */
    if (VMFindDLSProgram(pSynth->pDLS, bank, program, &regionIndex) != EAS_SUCCESS)
#endif

    /* braces to support 'if' clause above */
    {

        /* look in the internal banks */
        if (VMFindProgram(pSynth->pEAS, bank, program, &regionIndex) != EAS_SUCCESS)

        /* fall back to default bank */
        {
            if (pSynth->channels[channel].channelFlags & CHANNEL_FLAG_RHYTHM_CHANNEL)
                bank = DEFAULT_RHYTHM_BANK_NUMBER;
            else
                bank = DEFAULT_MELODY_BANK_NUMBER;

            if (VMFindProgram(pSynth->pEAS, bank, program, &regionIndex) != EAS_SUCCESS)

            /* switch to program 0 in the default bank */
            {
                if (VMFindProgram(pSynth->pEAS, bank, 0, &regionIndex) != EAS_SUCCESS)
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "VMProgramChange: No program @ %03d:%03d:%03d\n",
                        (bank >> 8) & 0x7f, bank & 0x7f, program); */ }
            }
        }
    }

    /* we have our new program change for this channel */
    pChannel->programNum = program;
    pChannel->regionIndex = regionIndex;

    /*
    set a channel flag to request parameter updates
    for all the voices associated with this channel
    */
    pChannel->channelFlags |= CHANNEL_FLAG_UPDATE_CHANNEL_PARAMETERS;

    return;
}

/*----------------------------------------------------------------------------
 * VMAddSamples()
 *----------------------------------------------------------------------------
 * Purpose:
 * Synthesize the requested number of samples (block based processing)
 *
 * Inputs:
 * nNumSamplesToAdd - number of samples to write to buffer
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * number of voices rendered
 *
 * Side Effects:
 * - samples are added to the presently free buffer
 *
 *----------------------------------------------------------------------------
*/
EAS_I32 VMAddSamples (S_VOICE_MGR *pVoiceMgr, EAS_I32 *pMixBuffer, EAS_I32 numSamples)
{
    S_SYNTH *pSynth;
    EAS_INT voicesRendered;
    EAS_INT voiceNum;
    EAS_BOOL done;

#ifdef  _REVERB
    EAS_PCM *pReverbSendBuffer;
#endif  // ifdef    _REVERB

#ifdef  _CHORUS
    EAS_PCM *pChorusSendBuffer;
#endif  // ifdef    _CHORUS

    voicesRendered = 0;
    for (voiceNum = 0; voiceNum < MAX_SYNTH_VOICES; voiceNum++)
    {

        /* retarget stolen voices */
        if ((pVoiceMgr->voices[voiceNum].voiceState == eVoiceStateStolen) && (pVoiceMgr->voices[voiceNum].gain <= 0))
            VMRetargetStolenVoice(pVoiceMgr, voiceNum);

        /* get pointer to virtual synth */
        pSynth = pVoiceMgr->pSynth[pVoiceMgr->voices[voiceNum].channel >> 4];

        /* synthesize active voices */
        if (pVoiceMgr->voices[voiceNum].voiceState != eVoiceStateFree)
        {
            done = GetSynthPtr(voiceNum)->pfUpdateVoice(pVoiceMgr, pSynth, &pVoiceMgr->voices[voiceNum], GetAdjustedVoiceNum(voiceNum), pMixBuffer, numSamples);
            voicesRendered++;

            /* voice is finished */
            if (done == EAS_TRUE)
            {
                /* set gain of stolen voice to zero so it will be restarted */
                if (pVoiceMgr->voices[voiceNum].voiceState == eVoiceStateStolen)
                    pVoiceMgr->voices[voiceNum].gain = 0;

                /* or return it to the free voice pool */
                else
                    VMFreeVoice(pVoiceMgr, pSynth, &pVoiceMgr->voices[voiceNum]);
            }

            /* if this voice is scheduled to be muted, set the mute flag */
            if (pVoiceMgr->voices[voiceNum].voiceFlags & VOICE_FLAG_DEFER_MUTE)
            {
                pVoiceMgr->voices[voiceNum].voiceFlags &= ~(VOICE_FLAG_DEFER_MUTE | VOICE_FLAG_DEFER_MIDI_NOTE_OFF);
                VMMuteVoice(pVoiceMgr, voiceNum);
            }

            /* if voice just started, advance state to play */
            if (pVoiceMgr->voices[voiceNum].voiceState == eVoiceStateStart)
                pVoiceMgr->voices[voiceNum].voiceState = eVoiceStatePlay;
        }
    }

    return voicesRendered;
}

/*----------------------------------------------------------------------------
 * VMRender()
 *----------------------------------------------------------------------------
 * Purpose:
 * This routine renders a frame of audio
 *
 * Inputs:
 * psEASData        - pointer to overall EAS data structure
 *
 * Outputs:
 * pVoicesRendered  - number of voices rendered this frame
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMRender (S_VOICE_MGR *pVoiceMgr, EAS_I32 numSamples, EAS_I32 *pMixBuffer, EAS_I32 *pVoicesRendered)
{
    S_SYNTH *pSynth;
    EAS_INT i;
    EAS_INT channel;

#ifdef _CHECKED_BUILD
    SanityCheck(pVoiceMgr);
#endif

    /* update MIDI channel parameters */
    *pVoicesRendered = 0;
    for (i = 0; i < MAX_VIRTUAL_SYNTHESIZERS; i++)
    {
        if (pVoiceMgr->pSynth[i] != NULL)
            VMUpdateStaticChannelParameters(pVoiceMgr, pVoiceMgr->pSynth[i]);
    }

    /* synthesize a buffer of audio */
    *pVoicesRendered = VMAddSamples(pVoiceMgr, pMixBuffer, numSamples);

    /*
     * check for deferred note-off messages
     * If flag is set, that means one or more voices are expecting deferred
     * midi note-off messages because the midi note-on and corresponding midi
     * note-off requests occurred during the same update interval. The goal
     * is the defer the note-off request so that the note can at least start.
    */
    for (i = 0; i < MAX_VIRTUAL_SYNTHESIZERS; i++)
    {
        pSynth = pVoiceMgr->pSynth[i];

        if (pSynth== NULL)
            continue;

        if (pSynth->synthFlags & SYNTH_FLAG_DEFERRED_MIDI_NOTE_OFF_PENDING)
            VMDeferredStopNote(pVoiceMgr, pSynth);

        /* check if we need to reset the synth */
        if ((pSynth->synthFlags & SYNTH_FLAG_RESET_IS_REQUESTED) &&
            (pSynth->numActiveVoices == 0))
        {
            /*
            complete the process of resetting the synth now that
            all voices have muted
            */
#ifdef _DEBUG_VM
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "VMAddSamples: complete the reset process\n"); */ }
#endif

            VMInitializeAllChannels(pVoiceMgr, pSynth);
            VMInitializeAllVoices(pVoiceMgr, pSynth->vSynthNum);

            /* clear the reset flag */
            pSynth->synthFlags &= ~SYNTH_FLAG_RESET_IS_REQUESTED;
        }

        /* clear channel update flags */
        for (channel = 0; channel < NUM_SYNTH_CHANNELS; channel++)
            pSynth->channels[channel].channelFlags &= ~CHANNEL_FLAG_UPDATE_CHANNEL_PARAMETERS;

        }

#ifdef _CHECKED_BUILD
    SanityCheck(pVoiceMgr);
#endif

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * VMInitWorkload()
 *----------------------------------------------------------------------------
 * Purpose:
 * Clears the workload counter
 *
 * Inputs:
 * pVoiceMgr            - pointer to instance data
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
void VMInitWorkload (S_VOICE_MGR *pVoiceMgr)
{
    pVoiceMgr->workload = 0;
}

/*----------------------------------------------------------------------------
 * VMSetWorkload()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the max workload for a single frame.
 *
 * Inputs:
 * pVoiceMgr            - pointer to instance data
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
void VMSetWorkload (S_VOICE_MGR *pVoiceMgr, EAS_I32 maxWorkLoad)
{
    pVoiceMgr->maxWorkLoad = maxWorkLoad;
}

/*----------------------------------------------------------------------------
 * VMCheckWorkload()
 *----------------------------------------------------------------------------
 * Purpose:
 * Checks to see if work load has been exceeded on this frame.
 *
 * Inputs:
 * pVoiceMgr            - pointer to instance data
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_BOOL VMCheckWorkload (S_VOICE_MGR *pVoiceMgr)
{
    if (pVoiceMgr->maxWorkLoad > 0)
        return (EAS_BOOL) (pVoiceMgr->workload >= pVoiceMgr->maxWorkLoad);
    return EAS_FALSE;
}

/*----------------------------------------------------------------------------
 * VMActiveVoices()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the number of active voices in the synthesizer.
 *
 * Inputs:
 * pEASData         - pointer to instance data
 *
 * Outputs:
 * Returns the number of active voices
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_I32 VMActiveVoices (S_SYNTH *pSynth)
{
    return pSynth->numActiveVoices;
}

/*----------------------------------------------------------------------------
 * VMSetSynthPolyphony()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the synth to a new polyphony value. Value must be >= 1 and
 * <= MAX_SYNTH_VOICES. This function will pin the polyphony at those limits
 *
 * Inputs:
 * pVoiceMgr        pointer to synthesizer data
 * polyphonyCount   desired polyphony count
 * synth            synthesizer number (0 = onboard, 1 = DSP)
 *
 * Outputs:
 * Returns error code
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMSetSynthPolyphony (S_VOICE_MGR *pVoiceMgr, EAS_I32 synth, EAS_I32 polyphonyCount)
{
    EAS_INT i;
    EAS_INT activeVoices;

    /* lower limit */
    if (polyphonyCount < 1)
        polyphonyCount = 1;

    /* split architecture */
#if defined(_SECONDARY_SYNTH) || defined(EAS_SPLIT_WT_SYNTH)
    if (synth == EAS_MCU_SYNTH)
    {
        if (polyphonyCount > NUM_PRIMARY_VOICES)
            polyphonyCount = NUM_PRIMARY_VOICES;
        if (pVoiceMgr->maxPolyphonyPrimary == polyphonyCount)
            return EAS_SUCCESS;
        pVoiceMgr->maxPolyphonyPrimary = (EAS_U16) polyphonyCount;
    }
    else if (synth == EAS_DSP_SYNTH)
    {
        if (polyphonyCount > NUM_SECONDARY_VOICES)
            polyphonyCount = NUM_SECONDARY_VOICES;
        if (pVoiceMgr->maxPolyphonySecondary == polyphonyCount)
            return EAS_SUCCESS;
        pVoiceMgr->maxPolyphonySecondary = (EAS_U16) polyphonyCount;
    }
    else
        return EAS_ERROR_PARAMETER_RANGE;

    /* setting for SP-MIDI */
    pVoiceMgr->maxPolyphony = pVoiceMgr->maxPolyphonyPrimary + pVoiceMgr->maxPolyphonySecondary;

    /* standard architecture */
#else
    if (synth != EAS_MCU_SYNTH)
        return EAS_ERROR_PARAMETER_RANGE;

    /* pin desired value to possible limits */
    if (polyphonyCount > MAX_SYNTH_VOICES)
        polyphonyCount = MAX_SYNTH_VOICES;

    /* set polyphony, if value is different than current value */
    if (pVoiceMgr->maxPolyphony == polyphonyCount)
        return EAS_SUCCESS;

    pVoiceMgr->maxPolyphony = (EAS_U16) polyphonyCount;
#endif

    /* if SPMIDI enabled, update channel masking based on new polyphony */
    for (i = 0; i < MAX_VIRTUAL_SYNTHESIZERS; i++)
    {
        if (pVoiceMgr->pSynth[i])
        {
            if (pVoiceMgr->pSynth[i]->synthFlags & SYNTH_FLAG_SP_MIDI_ON)
                VMMIPUpdateChannelMuting(pVoiceMgr, pVoiceMgr->pSynth[i]);
            else
                pVoiceMgr->pSynth[i]->poolAlloc[0] = (EAS_U8) polyphonyCount;
        }
    }

    /* are we under polyphony limit? */
    if (pVoiceMgr->activeVoices <= polyphonyCount)
        return EAS_SUCCESS;

    /* count the number of active voices */
    activeVoices = 0;
    for (i = 0; i < MAX_SYNTH_VOICES; i++)
    {

        /* is voice active? */
        if ((pVoiceMgr->voices[i].voiceState != eVoiceStateFree) && (pVoiceMgr->voices[i].voiceState != eVoiceStateMuting))
            activeVoices++;
    }

    /* we may have to mute voices to reach new target */
    while (activeVoices > polyphonyCount)
    {
        S_SYNTH *pSynth;
        S_SYNTH_VOICE *pVoice;
        EAS_I32 currentPriority, bestPriority;
        EAS_INT bestCandidate;

        /* find the lowest priority voice */
        bestPriority = bestCandidate = -1;
        for (i = 0; i < MAX_SYNTH_VOICES; i++)
        {

            pVoice = &pVoiceMgr->voices[i];

            /* ignore free and muting voices */
            if ((pVoice->voiceState == eVoiceStateFree) || (pVoice->voiceState == eVoiceStateMuting))
                continue;

            pSynth = pVoiceMgr->pSynth[GET_VSYNTH(pVoice->channel)];

            /* if voice is stolen or just started, reduce the likelihood it will be stolen */
            if (( pVoice->voiceState == eVoiceStateStolen) || (pVoice->voiceFlags & VOICE_FLAG_NO_SAMPLES_SYNTHESIZED_YET))
            {
                /* include velocity */
                currentPriority = 128 - pVoice->nextVelocity;

                /* include channel priority */
                currentPriority += pSynth->channels[GET_CHANNEL(pVoice->nextChannel)].pool << CHANNEL_PRIORITY_STEAL_WEIGHT;
            }
            else
            {
                /* include age */
                currentPriority = (EAS_I32) pVoice->age << NOTE_AGE_STEAL_WEIGHT;

                /* include note gain -higher gain is lower steal value */
                /*lint -e{704} use shift for performance */
                currentPriority += ((32768 >> (12 - NOTE_GAIN_STEAL_WEIGHT)) + 256) -
                    ((EAS_I32) pVoice->gain >> (12 - NOTE_GAIN_STEAL_WEIGHT));

                /* include channel priority */
                currentPriority += pSynth->channels[GET_CHANNEL(pVoice->channel)].pool << CHANNEL_PRIORITY_STEAL_WEIGHT;
            }

            /* include synth priority */
            currentPriority += pSynth->priority << SYNTH_PRIORITY_WEIGHT;

            /* is this the best choice so far? */
            if (currentPriority > bestPriority)
            {
                bestPriority = currentPriority;
                bestCandidate = i;
            }
        }

        /* shutdown best candidate */
        if (bestCandidate < 0)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "VMSetPolyphony: Unable to reduce polyphony\n"); */ }
            break;
        }

        /* shut down this voice */
        /*lint -e{771} pSynth is initialized if bestCandidate >= 0 */
        VMMuteVoice(pVoiceMgr, bestCandidate);
        activeVoices--;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * VMGetSynthPolyphony()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the current polyphony setting
 *
 * Inputs:
 * pVoiceMgr        pointer to synthesizer data
 * synth            synthesizer number (0 = onboard, 1 = DSP)
 *
 * Outputs:
 * Returns actual polyphony value set, as pinned by limits
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMGetSynthPolyphony (S_VOICE_MGR *pVoiceMgr, EAS_I32 synth, EAS_I32 *pPolyphonyCount)
{

#if defined(_SECONDARY_SYNTH) || defined(EAS_SPLIT_WT_SYNTH)
    if (synth == EAS_MCU_SYNTH)
        *pPolyphonyCount = pVoiceMgr->maxPolyphonyPrimary;
    else if (synth == EAS_DSP_SYNTH)
        *pPolyphonyCount = pVoiceMgr->maxPolyphonySecondary;
    else
        return EAS_ERROR_PARAMETER_RANGE;
#else
    if (synth != EAS_MCU_SYNTH)
        return EAS_ERROR_PARAMETER_RANGE;
    *pPolyphonyCount = pVoiceMgr->maxPolyphony;
#endif
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * VMSetPolyphony()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the virtual synth polyphony. 0 = no limit (i.e. can use
 * all available voices).
 *
 * Inputs:
 * pVoiceMgr        pointer to synthesizer data
 * polyphonyCount   desired polyphony count
 * pSynth           pointer to virtual synth
 *
 * Outputs:
 * Returns error code
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMSetPolyphony (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_I32 polyphonyCount)
{
    EAS_INT i;
    EAS_INT activeVoices;

    /* check limits */
    if (polyphonyCount < 0)
        return EAS_ERROR_PARAMETER_RANGE;

    /* zero is max polyphony */
    if ((polyphonyCount == 0) || (polyphonyCount > MAX_SYNTH_VOICES))
    {
        pSynth->maxPolyphony = 0;
        return EAS_SUCCESS;
    }

    /* set new polyphony */
    pSynth->maxPolyphony = (EAS_U16) polyphonyCount;

    /* max polyphony is minimum of virtual synth and actual synth */
    if (polyphonyCount > pVoiceMgr->maxPolyphony)
        polyphonyCount = pVoiceMgr->maxPolyphony;

    /* if SP-MIDI mode, update the channel muting */
    if (pSynth->synthFlags & SYNTH_FLAG_SP_MIDI_ON)
        VMMIPUpdateChannelMuting(pVoiceMgr, pSynth);
    else
        pSynth->poolAlloc[0] = (EAS_U8) polyphonyCount;

    /* are we under polyphony limit? */
    if (pSynth->numActiveVoices <= polyphonyCount)
        return EAS_SUCCESS;

    /* count the number of active voices */
    activeVoices = 0;
    for (i = 0; i < MAX_SYNTH_VOICES; i++)
    {
        /* this synth? */
        if (GET_VSYNTH(pVoiceMgr->voices[i].nextChannel) != pSynth->vSynthNum)
            continue;

        /* is voice active? */
        if ((pVoiceMgr->voices[i].voiceState != eVoiceStateFree) && (pVoiceMgr->voices[i].voiceState != eVoiceStateMuting))
            activeVoices++;
    }

    /* we may have to mute voices to reach new target */
    while (activeVoices > polyphonyCount)
    {
        S_SYNTH_VOICE *pVoice;
        EAS_I32 currentPriority, bestPriority;
        EAS_INT bestCandidate;

        /* find the lowest priority voice */
        bestPriority = bestCandidate = -1;
        for (i = 0; i < MAX_SYNTH_VOICES; i++)
        {
            pVoice = &pVoiceMgr->voices[i];

            /* this synth? */
            if (GET_VSYNTH(pVoice->nextChannel) != pSynth->vSynthNum)
                continue;

            /* if voice is stolen or just started, reduce the likelihood it will be stolen */
            if (( pVoice->voiceState == eVoiceStateStolen) || (pVoice->voiceFlags & VOICE_FLAG_NO_SAMPLES_SYNTHESIZED_YET))
            {
                /* include velocity */
                currentPriority = 128 - pVoice->nextVelocity;

                /* include channel priority */
                currentPriority += pSynth->channels[GET_CHANNEL(pVoice->nextChannel)].pool << CHANNEL_PRIORITY_STEAL_WEIGHT;
            }
            else
            {
                /* include age */
                currentPriority = (EAS_I32) pVoice->age << NOTE_AGE_STEAL_WEIGHT;

                /* include note gain -higher gain is lower steal value */
                /*lint -e{704} use shift for performance */
                currentPriority += ((32768 >> (12 - NOTE_GAIN_STEAL_WEIGHT)) + 256) -
                    ((EAS_I32) pVoice->gain >> (12 - NOTE_GAIN_STEAL_WEIGHT));

                /* include channel priority */
                currentPriority += pSynth->channels[GET_CHANNEL(pVoice->nextChannel)].pool << CHANNEL_PRIORITY_STEAL_WEIGHT;
            }

            /* is this the best choice so far? */
            if (currentPriority > bestPriority)
            {
                bestPriority = currentPriority;
                bestCandidate = i;
            }
        }

        /* shutdown best candidate */
        if (bestCandidate < 0)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "VMSetPolyphony: Unable to reduce polyphony\n"); */ }
            break;
        }

        /* shut down this voice */
        VMMuteVoice(pVoiceMgr, bestCandidate);
        activeVoices--;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * VMGetPolyphony()
 *----------------------------------------------------------------------------
 * Purpose:
 * Get the virtual synth polyphony
 *
 * Inputs:
 * pVoiceMgr        pointer to synthesizer data
 * pPolyphonyCount  pointer to variable to hold polyphony count
 * pSynth           pointer to virtual synth
 *
 * Outputs:
 * Returns error code
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pVoiceMgr) reserved for future use */
EAS_RESULT VMGetPolyphony (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_I32 *pPolyphonyCount)
{
    *pPolyphonyCount = (EAS_U16) pSynth->maxPolyphony;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * VMSetPriority()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the virtual synth priority
 *
 * Inputs:
 * pVoiceMgr        pointer to synthesizer data
 * priority         new priority
 * pSynth           pointer to virtual synth
 *
 * Outputs:
 * Returns error code
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pVoiceMgr) reserved for future use */
EAS_RESULT VMSetPriority (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_I32 priority)
{
    pSynth->priority = (EAS_U8) priority ;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * VMGetPriority()
 *----------------------------------------------------------------------------
 * Purpose:
 * Get the virtual synth priority
 *
 * Inputs:
 * pVoiceMgr        pointer to synthesizer data
 * pPriority        pointer to variable to hold priority
 * pSynth           pointer to virtual synth
 *
 * Outputs:
 * Returns error code
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pVoiceMgr) reserved for future use */
EAS_RESULT VMGetPriority (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_I32 *pPriority)
{
    *pPriority = pSynth->priority;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * VMSetVolume()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the master volume for this synthesizer for this sequence.
 *
 * Inputs:
 * nSynthVolume - the desired master volume
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *
 * Side Effects:
 * overrides any previously set master volume from sysex
 *
 *----------------------------------------------------------------------------
*/
void VMSetVolume (S_SYNTH *pSynth, EAS_U16 masterVolume)
{
    pSynth->masterVolume = masterVolume;
    pSynth->synthFlags |= SYNTH_FLAG_UPDATE_ALL_CHANNEL_PARAMETERS;
}

/*----------------------------------------------------------------------------
 * VMSetPitchBendRange()
 *----------------------------------------------------------------------------
 * Set the pitch bend range for the given channel.
 *----------------------------------------------------------------------------
*/
void VMSetPitchBendRange (S_SYNTH *pSynth, EAS_INT channel, EAS_I16 pitchBendRange)
{
    pSynth->channels[channel].pitchBendSensitivity = pitchBendRange;
}

/*----------------------------------------------------------------------------
 * VMValidateEASLib()
 *----------------------------------------------------------------------------
 * Validates an EAS library
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMValidateEASLib (EAS_SNDLIB_HANDLE pEAS)
{
    /* validate the sound library */
    if (pEAS)
    {
        if (pEAS->identifier != _EAS_LIBRARY_VERSION)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "VMValidateEASLib: Sound library mismatch in sound library: Read 0x%08x, expected 0x%08x\n",
                pEAS->identifier, _EAS_LIBRARY_VERSION); */ }
            return EAS_ERROR_SOUND_LIBRARY;
        }

        /* check sample rate */
        if ((pEAS->libAttr & LIBFORMAT_SAMPLE_RATE_MASK) != _OUTPUT_SAMPLE_RATE)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "VMValidateEASLib: Sample rate mismatch in sound library: Read %lu, expected %lu\n",
                pEAS->libAttr & LIBFORMAT_SAMPLE_RATE_MASK, _OUTPUT_SAMPLE_RATE); */ }
            return EAS_ERROR_SOUND_LIBRARY;
        }

#ifdef _WT_SYNTH
        /* check sample bit depth */
#ifdef _8_BIT_SAMPLES
        if (pEAS->libAttr & LIB_FORMAT_16_BIT_SAMPLES)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "VMValidateEASLib: Expected 8-bit samples and found 16-bit\n",
                pEAS->libAttr & LIBFORMAT_SAMPLE_RATE_MASK, _OUTPUT_SAMPLE_RATE); */ }
            return EAS_ERROR_SOUND_LIBRARY;
        }
#endif
#ifdef _16_BIT_SAMPLES
        if ((pEAS->libAttr & LIB_FORMAT_16_BIT_SAMPLES) == 0)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "VMValidateEASLib: Expected 16-bit samples and found 8-bit\n",
                pEAS->libAttr & LIBFORMAT_SAMPLE_RATE_MASK, _OUTPUT_SAMPLE_RATE); */ }
            return EAS_ERROR_SOUND_LIBRARY;
        }
#endif
#endif
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * VMSetGlobalEASLib()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the EAS library to be used by the synthesizer
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMSetGlobalEASLib (S_VOICE_MGR *pVoiceMgr, EAS_SNDLIB_HANDLE pEAS)
{
    EAS_RESULT result;

    result = VMValidateEASLib(pEAS);
    if (result != EAS_SUCCESS)
        return result;

    pVoiceMgr->pGlobalEAS = pEAS;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * VMSetEASLib()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the EAS library to be used by the synthesizer
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMSetEASLib (S_SYNTH *pSynth, EAS_SNDLIB_HANDLE pEAS)
{
    EAS_RESULT result;

    result = VMValidateEASLib(pEAS);
    if (result != EAS_SUCCESS)
        return result;

    pSynth->pEAS = pEAS;
    return EAS_SUCCESS;
}

#ifdef DLS_SYNTHESIZER
/*----------------------------------------------------------------------------
 * VMSetGlobalDLSLib()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the DLS library to be used by the synthesizer
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMSetGlobalDLSLib (EAS_DATA_HANDLE pEASData, EAS_DLSLIB_HANDLE pDLS)
{

    if (pEASData->pVoiceMgr->pGlobalDLS)
        DLSCleanup(pEASData->hwInstData, pEASData->pVoiceMgr->pGlobalDLS);

    pEASData->pVoiceMgr->pGlobalDLS = pDLS;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * VMSetDLSLib()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the DLS library to be used by the synthesizer
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMSetDLSLib (S_SYNTH *pSynth, EAS_DLSLIB_HANDLE pDLS)
{
    pSynth->pDLS = pDLS;
    return EAS_SUCCESS;
}
#endif

/*----------------------------------------------------------------------------
 * VMSetTranposition()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the global key transposition used by the synthesizer.
 * Transposes all melodic instruments up or down by the specified
 * amount. Range is limited to +/-12 semitones.
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
void VMSetTranposition (S_SYNTH *pSynth, EAS_I32 transposition)
{
    pSynth->globalTranspose = (EAS_I8) transposition;
}

/*----------------------------------------------------------------------------
 * VMGetTranposition()
 *----------------------------------------------------------------------------
 * Purpose:
 * Gets the global key transposition used by the synthesizer.
 * Transposes all melodic instruments up or down by the specified
 * amount. Range is limited to +/-12 semitones.
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
void VMGetTranposition (S_SYNTH *pSynth, EAS_I32 *pTransposition)
{
    *pTransposition = pSynth->globalTranspose;
}

/*----------------------------------------------------------------------------
 * VMGetNoteCount()
 *----------------------------------------------------------------------------
* Returns the total note count
*----------------------------------------------------------------------------
*/
EAS_I32 VMGetNoteCount (S_SYNTH *pSynth)
{
    return pSynth->totalNoteCount;
}

/*----------------------------------------------------------------------------
 * VMMIDIShutdown()
 *----------------------------------------------------------------------------
 * Purpose:
 * Clean up any Synth related system issues.
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * None
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
void VMMIDIShutdown (S_EAS_DATA *pEASData, S_SYNTH *pSynth)
{
    EAS_INT vSynthNum;

    /* decrement reference count, free if all references are gone */
    if (--pSynth->refCount > 0)
        return;

    vSynthNum = pSynth->vSynthNum;

    /* cleanup DLS load */
#ifdef DLS_SYNTHESIZER
    /*lint -e{550} result used only in debugging code */
    if (pSynth->pDLS != NULL)
    {
        EAS_RESULT result;
        if ((result = DLSCleanup(pEASData->hwInstData, pSynth->pDLS)) != EAS_SUCCESS)
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "VMMIDIShutdown: Error %ld cleaning up DLS collection\n", result); */ }
        pSynth->pDLS = NULL;
    }
#endif

    VMReset(pEASData->pVoiceMgr, pSynth, EAS_TRUE);

    /* check Configuration Module for static memory allocation */
    if (!pEASData->staticMemoryModel)
        EAS_HWFree(pEASData->hwInstData, pSynth);

    /* clear pointer to MIDI state */
    pEASData->pVoiceMgr->pSynth[vSynthNum] = NULL;
}

/*----------------------------------------------------------------------------
 * VMShutdown()
 *----------------------------------------------------------------------------
 * Purpose:
 * Clean up any Synth related system issues.
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * None
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
void VMShutdown (S_EAS_DATA *pEASData)
{

    /* don't free a NULL pointer */
    if (pEASData->pVoiceMgr == NULL)
        return;

#ifdef DLS_SYNTHESIZER
    /* if we have a global DLS collection, clean it up */
    if (pEASData->pVoiceMgr->pGlobalDLS)
    {
        DLSCleanup(pEASData->hwInstData, pEASData->pVoiceMgr->pGlobalDLS);
        pEASData->pVoiceMgr->pGlobalDLS = NULL;
    }
#endif

    /* check Configuration Module for static memory allocation */
    if (!pEASData->staticMemoryModel)
        EAS_HWFree(pEASData->hwInstData, pEASData->pVoiceMgr);
    pEASData->pVoiceMgr = NULL;
}

#ifdef EXTERNAL_AUDIO
/*----------------------------------------------------------------------------
 * EAS_RegExtAudioCallback()
 *----------------------------------------------------------------------------
 * Register a callback for external audio processing
 *----------------------------------------------------------------------------
*/
void VMRegExtAudioCallback (S_SYNTH *pSynth, EAS_VOID_PTR pInstData, EAS_EXT_PRG_CHG_FUNC cbProgChgFunc, EAS_EXT_EVENT_FUNC cbEventFunc)
{
    pSynth->pExtAudioInstData = pInstData;
    pSynth->cbProgChgFunc = cbProgChgFunc;
    pSynth->cbEventFunc = cbEventFunc;
}

/*----------------------------------------------------------------------------
 * VMGetMIDIControllers()
 *----------------------------------------------------------------------------
 * Returns the MIDI controller values on the specified channel
 *----------------------------------------------------------------------------
*/
void VMGetMIDIControllers (S_SYNTH *pSynth, EAS_U8 channel, S_MIDI_CONTROLLERS *pControl)
{
    pControl->modWheel = pSynth->channels[channel].modWheel;
    pControl->volume = pSynth->channels[channel].volume;
    pControl->pan = pSynth->channels[channel].pan;
    pControl->expression = pSynth->channels[channel].expression;
    pControl->channelPressure = pSynth->channels[channel].channelPressure;

#ifdef _REVERB
    pControl->reverbSend = pSynth->channels[channel].reverbSend;
#endif

#ifdef _CHORUSE
    pControl->chorusSend = pSynth->channels[channel].chorusSend;
#endif
}
#endif

#ifdef _SPLIT_ARCHITECTURE
/*----------------------------------------------------------------------------
 * VMStartFrame()
 *----------------------------------------------------------------------------
 * Purpose:
 * Starts an audio frame
 *
 * Inputs:
 *
 * Outputs:
 * Returns true if EAS_MixEnginePrep should be called (onboard mixing)
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_BOOL VMStartFrame (S_EAS_DATA *pEASData)
{

    /* init counter for voices starts in split architecture */
#ifdef MAX_VOICE_STARTS
    pVoiceMgr->numVoiceStarts = 0;
#endif

    return pFrameInterface->pfStartFrame(pEASData->pVoiceMgr->pFrameBuffer);
}

/*----------------------------------------------------------------------------
 * VMEndFrame()
 *----------------------------------------------------------------------------
 * Purpose:
 * Stops an audio frame
 *
 * Inputs:
 *
 * Outputs:
 * Returns true if EAS_MixEnginePost should be called (onboard mixing)
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_BOOL VMEndFrame (S_EAS_DATA *pEASData)
{

    return pFrameInterface->pfEndFrame(pEASData->pVoiceMgr->pFrameBuffer, pEASData->pMixBuffer, pEASData->masterGain);
}
#endif

#ifdef TEST_HARNESS
/*----------------------------------------------------------------------------
 * SanityCheck()
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMSanityCheck (EAS_DATA_HANDLE pEASData)
{
    S_SYNTH_VOICE *pVoice;
    S_SYNTH *pSynth;
    EAS_INT i;
    EAS_INT j;
    EAS_INT freeVoices;
    EAS_INT activeVoices;
    EAS_INT playingVoices;
    EAS_INT stolenVoices;
    EAS_INT releasingVoices;
    EAS_INT mutingVoices;
    EAS_INT poolCount[MAX_VIRTUAL_SYNTHESIZERS][NUM_SYNTH_CHANNELS];
    EAS_INT vSynthNum;
    EAS_RESULT result = EAS_SUCCESS;

    /* initialize counts */
    EAS_HWMemSet(poolCount, 0, sizeof(poolCount));
    freeVoices = activeVoices = playingVoices = stolenVoices = releasingVoices = mutingVoices = 0;

    /* iterate through all voices */
    for (i = 0; i < MAX_SYNTH_VOICES; i++)
    {
        pVoice = &pEASData->pVoiceMgr->voices[i];
        if (pVoice->voiceState != eVoiceStateFree)
        {
            vSynthNum = GET_VSYNTH(pVoice->channel);
            if (vSynthNum >= MAX_VIRTUAL_SYNTHESIZERS)
            {
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "VMSanityCheck: Voice %d has invalid virtual synth number %d\n", i, vSynthNum); */ }
                result = EAS_FAILURE;
                continue;
            }
            pSynth = pEASData->pVoiceMgr->pSynth[vSynthNum];

            switch (pVoice->voiceState)
            {
                case eVoiceStateMuting:
                    activeVoices++;
                    mutingVoices++;
                    break;

                case eVoiceStateStolen:
                    vSynthNum = GET_VSYNTH(pVoice->nextChannel);
                    if (vSynthNum >= MAX_VIRTUAL_SYNTHESIZERS)
                    {
                        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "VMSanityCheck: Voice %d has invalid virtual synth number %d\n", i, vSynthNum); */ }
                        result = EAS_FAILURE;
                        continue;
                    }
                    pSynth = pEASData->pVoiceMgr->pSynth[vSynthNum];
                    activeVoices++;
                    stolenVoices++;
                    poolCount[vSynthNum][pSynth->channels[GET_CHANNEL(pVoice->nextChannel)].pool]++;
                    break;

                case eVoiceStateStart:
                case eVoiceStatePlay:
                    activeVoices++;
                    playingVoices++;
                    poolCount[vSynthNum][pSynth->channels[GET_CHANNEL(pVoice->channel)].pool]++;
                    break;

                case eVoiceStateRelease:
                    activeVoices++;
                    releasingVoices++;
                    poolCount[vSynthNum][pSynth->channels[GET_CHANNEL(pVoice->channel)].pool]++;
                    break;

                default:
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "VMSanityCheck : voice %d in invalid state\n", i); */ }
                    result = EAS_FAILURE;
                    break;
            }
        }

        /* count free voices */
        else
            freeVoices++;
    }

    /* dump state info */
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "%d free\n", freeVoices); */ }
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "%d active\n", activeVoices); */ }
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "%d playing\n", playingVoices); */ }
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "%d releasing\n", releasingVoices); */ }
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "%d muting\n", mutingVoices); */ }
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "%d stolen\n", stolenVoices); */ }

    if (pEASData->pVoiceMgr->activeVoices != activeVoices)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Active voice mismatch was %d should be %d\n",
            pEASData->pVoiceMgr->activeVoices, activeVoices); */ }
        result = EAS_FAILURE;
    }

    /* check virtual synth status */
    for (i = 0; i < MAX_VIRTUAL_SYNTHESIZERS; i++)
    {
        if (pEASData->pVoiceMgr->pSynth[i] == NULL)
            continue;

        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Synth %d numActiveVoices: %d\n", i, pEASData->pVoiceMgr->pSynth[i]->numActiveVoices); */ }
        if (pEASData->pVoiceMgr->pSynth[i]->numActiveVoices > MAX_SYNTH_VOICES)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "VMSanityCheck: Synth %d illegal count for numActiveVoices: %d\n", i, pEASData->pVoiceMgr->pSynth[i]->numActiveVoices); */ }
            result = EAS_FAILURE;
        }
        for (j = 0; j < NUM_SYNTH_CHANNELS; j++)
        {
            if (poolCount[i][j] != pEASData->pVoiceMgr->pSynth[i]->poolCount[j])
            {
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Pool count mismatch synth %d pool %d, was %d, should be %d\n",
                    i, j, pEASData->pVoiceMgr->pSynth[i]->poolCount[j], poolCount[i][j]); */ }
                result = EAS_FAILURE;
            }
        }
    }

    return result;
}
#endif


