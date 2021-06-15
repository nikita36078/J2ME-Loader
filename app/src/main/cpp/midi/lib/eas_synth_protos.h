/*----------------------------------------------------------------------------
 *
 * File:
 * eas_synth_protos.h
 *
 * Contents and purpose:
 * Declarations, interfaces, and prototypes for synth.
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
 *   $Revision: 82 $
 *   $Date: 2006-07-10 11:45:19 -0700 (Mon, 10 Jul 2006) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_SYNTH_PROTOS_H
#define _EAS_SYNTH_PROTOS_H

/* includes */
#include "eas_data.h"
#include "eas_sndlib.h"

#ifdef _SPLIT_ARCHITECTURE
typedef struct s_frame_interface_tag
{
    EAS_BOOL (* EAS_CONST pfStartFrame)(EAS_FRAME_BUFFER_HANDLE pFrameBuffer);
    EAS_BOOL (* EAS_CONST pfEndFrame)(EAS_FRAME_BUFFER_HANDLE pFrameBuffer, EAS_I32 *pMixBuffer, EAS_I16 masterGain);
} S_FRAME_INTERFACE;
#endif

/* generic synthesizer interface */
typedef struct
{
    EAS_RESULT (* EAS_CONST pfInitialize)(S_VOICE_MGR *pVoiceMgr);
    EAS_RESULT (* EAS_CONST pfStartVoice)(S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, EAS_I32 voiceNum, EAS_U16 regionIndex);
    EAS_BOOL (* EAS_CONST pfUpdateVoice)(S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, EAS_I32 voiceNum, EAS_I32 *pMixBuffer, EAS_I32 numSamples);
    void (* EAS_CONST pfReleaseVoice)(S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, EAS_I32 voiceNum);
    void (* EAS_CONST pfMuteVoice)(S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, EAS_I32 voiceNum);
    void (* EAS_CONST pfSustainPedal)(S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, S_SYNTH_VOICE *pVoice, S_SYNTH_CHANNEL *pChannel, EAS_I32 voiceNum);
    void (* EAS_CONST pfUpdateChannel)(S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel);
} S_SYNTH_INTERFACE;

#endif



