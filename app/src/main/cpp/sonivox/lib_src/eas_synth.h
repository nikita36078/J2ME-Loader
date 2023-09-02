/*----------------------------------------------------------------------------
 *
 * File:
 * eas_synth.h
 *
 * Contents and purpose:
 * Declarations, interfaces, and prototypes for synth.
 *
 * Copyright Sonic Network Inc. 2004, 2005

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
 *   $Revision: 718 $
 *   $Date: 2007-06-08 16:43:16 -0700 (Fri, 08 Jun 2007) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_SYNTH_H
#define _EAS_SYNTH_H

#include "eas_types.h"
#include "eas_sndlib.h"

#ifdef _WT_SYNTH
#include "eas_wtsynth.h"
#endif

#ifdef _FM_SYNTH
#include "eas_fmsynth.h"
#endif

#ifndef NUM_OUTPUT_CHANNELS
#define NUM_OUTPUT_CHANNELS         2
#endif

#ifndef MAX_SYNTH_VOICES
#define MAX_SYNTH_VOICES            64
#endif

#ifndef MAX_VIRTUAL_SYNTHESIZERS
#define MAX_VIRTUAL_SYNTHESIZERS    4
#endif

/* defines */
#ifndef NUM_PRIMARY_VOICES
#define NUM_PRIMARY_VOICES      MAX_SYNTH_VOICES
#elif !defined(NUM_SECONDARY_VOICES)
#define NUM_SECONDARY_VOICES    (MAX_SYNTH_VOICES - NUM_PRIMARY_VOICES)
#endif

#if defined(EAS_WT_SYNTH)
#define NUM_WT_VOICES           MAX_SYNTH_VOICES

/* FM on MCU */
#elif defined(EAS_FM_SYNTH)
#define NUM_FM_VOICES           MAX_SYNTH_VOICES

/* wavetable drums on MCU, wavetable melodic on DSP */
#elif defined(EAS_SPLIT_WT_SYNTH)
#define NUM_WT_VOICES           MAX_SYNTH_VOICES

/* wavetable drums and FM melodic on MCU */
#elif defined(EAS_HYBRID_SYNTH)
#define NUM_WT_VOICES           NUM_PRIMARY_VOICES
#define NUM_FM_VOICES           NUM_SECONDARY_VOICES

/* wavetable drums on MCU, FM melodic on DSP */
#elif defined(EAS_SPLIT_HYBRID_SYNTH)
#define NUM_WT_VOICES           NUM_PRIMARY_VOICES
#define NUM_FM_VOICES           NUM_SECONDARY_VOICES

/* FM synth on DSP */
#elif defined(EAS_SPLIT_FM_SYNTH)
#define NUM_FM_VOICES           MAX_SYNTH_VOICES

#else
#error "Unrecognized architecture option"
#endif

#define NUM_SYNTH_CHANNELS      16

#define DEFAULT_SYNTH_VOICES    MAX_SYNTH_VOICES

/* use the following values to specify unassigned channels or voices */
#define UNASSIGNED_SYNTH_CHANNEL    NUM_SYNTH_CHANNELS
#define UNASSIGNED_SYNTH_VOICE      MAX_SYNTH_VOICES


/* synth parameters are updated every SYNTH_UPDATE_PERIOD_IN_SAMPLES */
#define SYNTH_UPDATE_PERIOD_IN_SAMPLES  (EAS_I32)(0x1L << SYNTH_UPDATE_PERIOD_IN_BITS)

/* stealing weighting factors */
#define NOTE_AGE_STEAL_WEIGHT           1
#define NOTE_GAIN_STEAL_WEIGHT          4
#define CHANNEL_POLY_STEAL_WEIGHT       12
#define CHANNEL_PRIORITY_STEAL_WEIGHT   2
#define NOTE_MATCH_PENALTY              128
#define SYNTH_PRIORITY_WEIGHT           8

/* default synth master volume */
#define DEFAULT_SYNTH_MASTER_VOLUME     0x7fff

#define DEFAULT_SYNTH_PRIORITY          5

/* default tuning values */
#define DEFAULT_PITCH_BEND_SENSITIVITY      200     /* 2 semitones */
#define DEFAULT_FINE_PITCH                  0       /* 0 cents */
#define DEFAULT_COARSE_PITCH                0       /* 0 semitones */

/* default drum channel is 10, but is internally 9 due to unit offset */
#define DEFAULT_DRUM_CHANNEL            9

/* drum channel can simultaneously play this many voices at most */
#define DEFAULT_CHANNEL_POLYPHONY_LIMIT 2

/* default instrument is acoustic piano */
#define DEFAULT_MELODY_BANK_MSB         0x79
#define DEFAULT_RHYTHM_BANK_MSB         0x78
#define DEFAULT_MELODY_BANK_NUMBER      (DEFAULT_MELODY_BANK_MSB << 8)
#define DEFAULT_RHYTHM_BANK_NUMBER      (DEFAULT_RHYTHM_BANK_MSB << 8)
#define DEFAULT_SYNTH_PROGRAM_NUMBER    0

#define DEFAULT_PITCH_BEND      0x2000  /* 0x2000 == (0x40 << 7) | 0x00 */
#define DEFAULT_MOD_WHEEL       0
#define DEFAULT_CHANNEL_VOLUME  0x64
#define DEFAULT_PAN             0x40    /* decimal 64, center */

#ifdef _REVERB
#define DEFAULT_REVERB_SEND     40      /* some reverb */
#endif

#ifdef _CHORUS
#define DEFAULT_CHORUS_SEND     0       /* no chorus */
#endif

#define DEFAULT_EAS_FILTER_CUTOFF_FREQUENCY 0           /* EAS synth uses a different default */
#define DEFAULT_FILTER_RESONANCE        0
#define DEFAULT_EXPRESSION              0x7F

#define DEFAULT_CHANNEL_PRESSURE        0

#define DEFAULT_REGISTERED_PARAM        0x3FFF

#define DEFAULT_CHANNEL_STATIC_GAIN     0
#define DEFAULT_CHANNEL_STATIC_PITCH    0

#define DEFAULT_LFO_MOD_WHEEL_TO_PITCH_CENTS            50
#define DEFAULT_LFO_CHANNEL_PRESSURE_TO_PITCH_CENTS     50

#define DEFAULT_KEY_NUMBER          0x69
#define DEFAULT_VELOCITY            0x64
#define DEFAULT_REGION_INDEX        0
#define DEFAULT_ARTICULATION_INDEX  0
#define DEFAULT_VOICE_GAIN          0
#define DEFAULT_AGE                 0
#define DEFAULT_SP_MIDI_PRIORITY    16


/* filter defines */
#define DEFAULT_FILTER_ZERO     0
#define FILTER_CUTOFF_MAX_PITCH_CENTS       1919
#define FILTER_CUTOFF_MIN_PITCH_CENTS       -4467
#define A5_PITCH_OFFSET_IN_CENTS            6900

/*------------------------------------
 * S_SYNTH_CHANNEL data structure
 *------------------------------------
*/

/* S_SYNTH_CHANNEL.m_nFlags */
#define CHANNEL_FLAG_SUSTAIN_PEDAL                      0x01
#define CHANNEL_FLAG_MUTE                               0x02
#define CHANNEL_FLAG_UPDATE_CHANNEL_PARAMETERS          0x04
#define CHANNEL_FLAG_RHYTHM_CHANNEL                     0x08
#define CHANNEL_FLAG_EXTERNAL_AUDIO                     0x10
#define DEFAULT_CHANNEL_FLAGS                           0

/* macros for extracting virtual synth and channel numbers */
#define GET_VSYNTH(a) ((a) >> 4)
#define GET_CHANNEL(a) ((a) & 15)

typedef struct s_synth_channel_tag
{
    /* use static channel parameters to reduce MIPs */
    /* parameters shared by multiple voices assigned to same channel */
    EAS_I32     staticPitch;        /* (pitch bend * pitch sens) + fine pitch */
    EAS_I16     staticGain;         /* (CC7 * CC11 * master vol)^2  */

    EAS_U16     regionIndex;        /* index of first region in program */

    EAS_U16     bankNum;            /* play programs from this bank */
    EAS_I16     pitchBend;          /* pitch wheel value */
    EAS_I16     pitchBendSensitivity;
    EAS_I16     registeredParam;    /* currently selected registered param */


#if defined(_FM_SYNTH)
    EAS_I16     lfoAmt;             /* amount of LFO to apply to voice */
#endif

    EAS_U8      programNum;         /* play this instrument number */
    EAS_U8      modWheel;           /* CC1 */
    EAS_U8      volume;             /* CC7 */
    EAS_U8      pan;                /* CC10 */

    EAS_U8      expression;         /* CC11 */

    /* the following parameters are controlled by RPNs */
    EAS_I8      finePitch;
    EAS_I8      coarsePitch;

    EAS_U8      channelPressure;    /* applied to all voices on a given channel */

    EAS_U8      channelFlags;       /* bit field channelFlags for */
                                    /* CC64, SP-MIDI channel masking */

    EAS_U8      pool;               /* SPMIDI channel voice pool */
    EAS_U8      mip;                /* SPMIDI MIP setting */

#ifdef  _REVERB
    EAS_U8      reverbSend;         /* CC91 */
#endif

#ifdef  _CHORUS
    EAS_U8      chorusSend;         /* CC93 */
#endif
} S_SYNTH_CHANNEL;

/*------------------------------------
 * S_SYNTH_VOICE data structure
 *------------------------------------
*/

/* S_SYNTH_VOICE.m_nFlags */
#define VOICE_FLAG_UPDATE_VOICE_PARAMETERS              0x01
#define VOICE_FLAG_SUSTAIN_PEDAL_DEFER_NOTE_OFF         0x02
#define VOICE_FLAG_DEFER_MIDI_NOTE_OFF                  0x04
#define VOICE_FLAG_NO_SAMPLES_SYNTHESIZED_YET           0x08
#define VOICE_FLAG_DEFER_MUTE                           0x40
#define DEFAULT_VOICE_FLAGS                             0

/* S_SYNTH_VOICE.m_eState */
typedef enum {

    eVoiceStateFree = 0,
    eVoiceStateStart,
    eVoiceStatePlay,
    eVoiceStateRelease,
    eVoiceStateMuting,
    eVoiceStateStolen,
    eVoiceStateInvalid          /* should never be in this state! */

} E_VOICE_STATE;
#define DEFAULT_VOICE_STATE     eVoiceStateFree

typedef struct s_synth_voice_tag
{

/* These parameters are common to both wavetable and FM
 * synthesizers. The voice manager should only access this data.
 * Any other data should be manipulated by the code that is
 * specific to that synthesizer and reflected back through the
 * common state data available here.
 */
    EAS_U16             regionIndex;        /* index to wave and playback params */
    EAS_I16             gain;               /* current gain */
    EAS_U16             age;                /* large value means old note */
    EAS_U16             nextRegionIndex;    /* index to wave and playback params */
    EAS_U8              voiceState;         /* current voice state */
    EAS_U8              voiceFlags;         /* misc flags/bit fields */
    EAS_U8              channel;            /* this voice plays on this synth channel */
    EAS_U8              note;               /* 12 <= key number <= 108 */
    EAS_U8              velocity;           /* 0 <= velocity <= 127 */
    EAS_U8              nextChannel;        /* play stolen voice on this channel */
    EAS_U8              nextNote;           /* 12 <= key number <= 108 */
    EAS_U8              nextVelocity;       /* 0 <= velocity <= 127 */
} S_SYNTH_VOICE;

/*------------------------------------
 * S_SYNTH data structure
 *
 * One instance for each MIDI stream
 *------------------------------------
*/

/* S_SYNTH.m_nFlags */
#define SYNTH_FLAG_RESET_IS_REQUESTED                   0x01
#define SYNTH_FLAG_SP_MIDI_ON                           0x02
#define SYNTH_FLAG_UPDATE_ALL_CHANNEL_PARAMETERS        0x04
#define SYNTH_FLAG_DEFERRED_MIDI_NOTE_OFF_PENDING       0x08
#define DEFAULT_SYNTH_FLAGS     SYNTH_FLAG_UPDATE_ALL_CHANNEL_PARAMETERS

typedef struct s_synth_tag
{
    struct s_eas_data_tag   *pEASData;
    const S_EAS             *pEAS;

#ifdef DLS_SYNTHESIZER
    S_DLS                   *pDLS;
#endif

#ifdef EXTERNAL_AUDIO
    EAS_EXT_PRG_CHG_FUNC    cbProgChgFunc;
    EAS_EXT_EVENT_FUNC      cbEventFunc;
    EAS_VOID_PTR            *pExtAudioInstData;
#endif

    S_SYNTH_CHANNEL         channels[NUM_SYNTH_CHANNELS];
    EAS_I32                 totalNoteCount;
    EAS_U16                 maxPolyphony;
    EAS_U16                 numActiveVoices;
    EAS_U16                 masterVolume;
    EAS_U8                  channelsByPriority[NUM_SYNTH_CHANNELS];
    EAS_U8                  poolCount[NUM_SYNTH_CHANNELS];
    EAS_U8                  poolAlloc[NUM_SYNTH_CHANNELS];
    EAS_U8                  synthFlags;
    EAS_I8                  globalTranspose;
    EAS_U8                  vSynthNum;
    EAS_U8                  refCount;
    EAS_U8                  priority;
} S_SYNTH;

/*------------------------------------
 * S_VOICE_MGR data structure
 *
 * One instance for each EAS library instance
 *------------------------------------
*/
typedef struct s_voice_mgr_tag
{
    S_SYNTH                 *pSynth[MAX_VIRTUAL_SYNTHESIZERS];
    EAS_PCM                 voiceBuffer[SYNTH_UPDATE_PERIOD_IN_SAMPLES];

#ifdef _FM_SYNTH
    EAS_PCM                 operMixBuffer[SYNTH_UPDATE_PERIOD_IN_SAMPLES];
    S_FM_VOICE              fmVoices[NUM_FM_VOICES];
#endif

#ifdef _WT_SYNTH
    S_WT_VOICE              wtVoices[NUM_WT_VOICES];
#endif

#ifdef _REVERB
    EAS_PCM                 reverbSendBuffer[NUM_OUTPUT_CHANNELS * SYNTH_UPDATE_PERIOD_IN_SAMPLES];
#endif

#ifdef _CHORUS
    EAS_PCM                 chorusSendBuffer[NUM_OUTPUT_CHANNELS * SYNTH_UPDATE_PERIOD_IN_SAMPLES];
#endif
    S_SYNTH_VOICE           voices[MAX_SYNTH_VOICES];

    EAS_SNDLIB_HANDLE       pGlobalEAS;

#ifdef DLS_SYNTHESIZER
    S_DLS                   *pGlobalDLS;
#endif

#ifdef _SPLIT_ARCHITECTURE
    EAS_FRAME_BUFFER_HANDLE pFrameBuffer;
#endif

#if defined(_SECONDARY_SYNTH) || defined(EAS_SPLIT_WT_SYNTH)
    EAS_U16                 maxPolyphonyPrimary;
    EAS_U16                 maxPolyphonySecondary;
#endif

    EAS_I32                 workload;
    EAS_I32                 maxWorkLoad;

    EAS_U16                 activeVoices;
    EAS_U16                 maxPolyphony;

    EAS_U16                 age;

/* limits the number of voice starts in a frame for split architecture */
#ifdef MAX_VOICE_STARTS
    EAS_U16                 numVoiceStarts;
#endif
} S_VOICE_MGR;

#endif /* #ifdef _EAS_SYNTH_H */


