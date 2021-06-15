/*----------------------------------------------------------------------------
 *
 * File:
 * eas_wtsynth.h
 *
 * Contents and purpose:
 * This file defines the interface for synthesizer engine
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

#ifndef _EAS_WTSYNTH_H
#define _EAS_WTSYNTH_H

#include "eas_sndlib.h"
#include "eas_wtengine.h"

/* adjust the filter cutoff frequency to the sample rate */
#if defined (_SAMPLE_RATE_8000)
#define FILTER_CUTOFF_FREQ_ADJUST       0
#elif defined (_SAMPLE_RATE_16000)
#define FILTER_CUTOFF_FREQ_ADJUST       1200
#elif defined (_SAMPLE_RATE_20000)
#define FILTER_CUTOFF_FREQ_ADJUST       1586
#elif defined (_SAMPLE_RATE_22050)
#define FILTER_CUTOFF_FREQ_ADJUST       1756
#elif defined (_SAMPLE_RATE_24000)
#define FILTER_CUTOFF_FREQ_ADJUST       1902
#elif defined (_SAMPLE_RATE_32000)
#define FILTER_CUTOFF_FREQ_ADJUST       2400
#elif defined (_SAMPLE_RATE_44100)
#define FILTER_CUTOFF_FREQ_ADJUST       2956
#elif defined (_SAMPLE_RATE_48000)
#define FILTER_CUTOFF_FREQ_ADJUST       3102
#else
#error "_SAMPLE_RATE_XXXXX must be defined to valid rate"
#endif

/* function prototypes */
void WT_UpdateLFO (S_LFO_CONTROL *pLFO, EAS_I16 phaseInc);

#if defined(_FILTER_ENABLED) || defined(DLS_SYNTHESIZER)
void WT_SetFilterCoeffs (S_WT_INT_FRAME *pIntFrame, EAS_I32 cutoff, EAS_I32 resonance);
#endif

#endif


