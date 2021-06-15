/*----------------------------------------------------------------------------
 *
 * File:
 * eas_chorusdata.h
 *
 * Contents and purpose:
 * Contains the prototypes for the Chorus effect.
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
 *   $Revision: 309 $
 *   $Date: 2006-09-12 18:52:45 -0700 (Tue, 12 Sep 2006) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_CHORUS_H
#define _EAS_CHORUS_H

#include "eas_types.h"
#include "eas_audioconst.h"

//defines for chorus

#define EAS_CHORUS_BYPASS_DEFAULT   1
#define EAS_CHORUS_PRESET_DEFAULT   0
#define EAS_CHORUS_RATE_DEFAULT     30
#define EAS_CHORUS_DEPTH_DEFAULT    39
#define EAS_CHORUS_LEVEL_DEFAULT    32767

#define EAS_CHORUS_LEVEL_MIN        0
#define EAS_CHORUS_LEVEL_MAX        32767

#define EAS_CHORUS_RATE_MIN         10
#define EAS_CHORUS_RATE_MAX         50

#define EAS_CHORUS_DEPTH_MIN        15
#define EAS_CHORUS_DEPTH_MAX        60

#define CHORUS_SIZE_MS 20
#define CHORUS_L_SIZE ((CHORUS_SIZE_MS*_OUTPUT_SAMPLE_RATE)/1000)
#define CHORUS_R_SIZE CHORUS_L_SIZE
#define CHORUS_SHAPE_SIZE 128
#define CHORUS_DELAY_MS 10

#define CHORUS_MAX_TYPE         4   // any Chorus numbers larger than this are invalid

typedef struct
{
    EAS_I16             m_nRate;
    EAS_I16             m_nDepth;
    EAS_I16             m_nLevel;

} S_CHORUS_PRESET;

typedef struct
{
    S_CHORUS_PRESET     m_sPreset[CHORUS_MAX_TYPE]; //array of presets

} S_CHORUS_PRESET_BANK;

/* parameters for each Chorus */
typedef struct
{
    EAS_I32 lfoLPhase;
    EAS_I32 lfoRPhase;
    EAS_I16 chorusIndexL;
    EAS_I16 chorusIndexR;
    EAS_U16 chorusTapPosition;

    EAS_I16 m_nRate;
    EAS_I16 m_nDepth;
    EAS_I16 m_nLevel;

    //delay lines used by the chorus, longer would sound better
    EAS_PCM chorusDelayL[CHORUS_L_SIZE];
    EAS_PCM chorusDelayR[CHORUS_R_SIZE];

    EAS_BOOL    bypass;
    EAS_I8      preset;

    EAS_I16     m_nCurrentChorus;           // preset number for current Chorus
    EAS_I16     m_nNextChorus;              // preset number for next Chorus

    S_CHORUS_PRESET         pPreset;

    S_CHORUS_PRESET_BANK    m_sPreset;

} S_CHORUS_OBJECT;


/*----------------------------------------------------------------------------
 * WeightedTap()
 *----------------------------------------------------------------------------
 * Purpose: Does fractional array look-up using linear interpolation
 *
 * first convert indexDesired to actual desired index by taking into account indexReference
 * then do linear interpolation between two actual samples using fractional part
 *
 * Inputs:
 * array: pointer to array of signed 16 bit values, typically either PCM data or control data
 * indexReference: the circular buffer relative offset
 * indexDesired: the fractional index we are looking up (16 bits index + 16 bits fraction)
 * indexLimit: the total size of the array, used to compute buffer wrap
 *
 * Outputs:
 * Value from the input array, linearly interpolated between two actual data values
 *
 *----------------------------------------------------------------------------
*/
static EAS_I16 WeightedTap(const EAS_I16 *array, EAS_I16 indexReference, EAS_I32 indexDesired, EAS_I16 indexLimit);

/*----------------------------------------------------------------------------
 * ChorusReadInPresets()
 *----------------------------------------------------------------------------
 * Purpose: sets global Chorus preset bank to defaults
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ChorusReadInPresets(S_CHORUS_OBJECT *pChorusData);

/*----------------------------------------------------------------------------
 * ChorusUpdate
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the Chorus preset parameters as required
 *
 * Inputs:
 *
 * Outputs:
 *
 *
 * Side Effects:
 * - chorus paramters will be changed
 * - m_nCurrentChorus := m_nNextChorus
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ChorusUpdate(S_CHORUS_OBJECT* pChorusData);

#endif /* #ifndef _EAS_CHORUSDATA_H */


