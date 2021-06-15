/*----------------------------------------------------------------------------
 *
 * File:
 * eas_data.h
 *
 * Contents and purpose:
 * This header defines all types, to support dynamic allocation of the
 * memory resources needed for persistent EAS data.
 *
 * Copyright 2004 Sonic Network Inc.

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
 *   $Revision: 842 $
 *   $Date: 2007-08-23 14:32:31 -0700 (Thu, 23 Aug 2007) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_DATA_H
#define _EAS_DATA_H

#define JET_INTERFACE

#include "eas_types.h"
#include "eas_synthcfg.h"
#include "eas.h"
#include "eas_audioconst.h"
#include "eas_sndlib.h"
#include "eas_pcm.h"
#include "eas_pcmdata.h"
#include "eas_synth.h"
#include "eas_miditypes.h"
#include "eas_effects.h"

#ifdef AUX_MIXER
#include "eas_auxmixdata.h"
#endif

#ifdef JET_INTERFACE
#include "jet.h"
#endif

#ifdef _METRICS_ENABLED
#include "eas_perf.h"
#endif

#ifndef MAX_NUMBER_STREAMS
#define MAX_NUMBER_STREAMS          4
#endif

/* flags for S_EAS_STREAM */
#define STREAM_FLAGS_PARSED         1
#define STREAM_FLAGS_PAUSE          2
#define STREAM_FLAGS_LOCATE         4
#define STREAM_FLAGS_RESUME         8

/* structure for parsing a stream */
typedef struct s_eas_stream_tag
{
    void                            *pParserModule;
    EAS_U32                         time;
    EAS_U32                         frameLength;
    EAS_I32                         repeatCount;
    EAS_VOID_PTR                    handle;
    EAS_U8                          volume;
    EAS_BOOL8                       streamFlags;
} S_EAS_STREAM;

/* default master volume is -10dB */
#define DEFAULT_VOLUME              90
#define DEFAULT_STREAM_VOLUME       100
#define DEFAULT_STREAM_GAIN         14622

/* 10 dB of boost available for individual parsers */
#define STREAM_VOLUME_HEADROOM      10

/* amalgamated persistent data type */
typedef struct s_eas_data_tag
{
#ifdef _CHECKED_BUILD
    EAS_U32                         handleCheck;
#endif
    EAS_HW_DATA_HANDLE              hwInstData;

    S_EFFECTS_MODULE                effectsModules[NUM_EFFECTS_MODULES];

#ifdef _METRICS_ENABLED
    S_METRICS_INTERFACE             *pMetricsModule;
    EAS_VOID_PTR                    pMetricsData;
#endif

    EAS_I32                         *pMixBuffer;
    EAS_PCM                         *pOutputAudioBuffer;

#ifdef AUX_MIXER
    S_EAS_AUX_MIXER                 auxMixer;
#endif

#ifdef _MAXIMIZER_ENABLED
    EAS_VOID_PTR                    pMaximizerData;
#endif

    S_EAS_STREAM                    streams[MAX_NUMBER_STREAMS];

    S_PCM_STATE                     *pPCMStreams;

    S_VOICE_MGR                     *pVoiceMgr;

#ifdef JET_INTERFACE
    JET_DATA_HANDLE                 jetHandle;
#endif

    EAS_U32                         renderTime;
    EAS_I16                         masterGain;
    EAS_U8                          masterVolume;
    EAS_BOOL8                       staticMemoryModel;
    EAS_BOOL8                       searchHeaderFlag;
} S_EAS_DATA;

#endif

