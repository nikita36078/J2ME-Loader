/*----------------------------------------------------------------------------
 *
 * File:
 * eas_mixbuf.c
 *
 * Contents and purpose:
 * Contains a data allocation for synthesizer
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

// includes
#include "eas_data.h"
#include "eas_mixer.h"

// globals
EAS_I32 eas_MixBuffer[BUFFER_SIZE_IN_MONO_SAMPLES * NUM_OUTPUT_CHANNELS];

