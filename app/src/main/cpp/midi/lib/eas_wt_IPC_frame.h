/*----------------------------------------------------------------------------
 *
 * File:
 * eas_wt_IPC_frame.h
 *
 * Contents and purpose:
 * This module contains data definitions for the interprocessor
 * communications framework for a split-architecture synthesizer.
 *
 * This sample version writes IPC data to a file that can be used
 * as a test vector for the DSP simulator. For a real-time system
 * the file I/O is replaced with an IPC protocol in the hardware.
 *
 * Synchronization with the DSP is accomplished at the API level,
 * i.e. the host code should call EAS_Render when it is ready to
 * buffer another block of data for transmission to the DSP.
 *
 * Copyright Sonic Network Inc. 2005

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
 *   $Revision: 818 $
 *   $Date: 2007-08-02 15:19:41 -0700 (Thu, 02 Aug 2007) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_WT_IPC_FRAME_H
#define _EAS_WT_IPC_FRAME_H

/*----------------------------------------------------------------------------
 * S_WT_FRAME
 *
 * This structure contains the common parameters that are updated
  *for each frame of audio.
 *----------------------------------------------------------------------------
*/
typedef struct s_wt_frame_tag
{
    EAS_I32         gainTarget;
    EAS_I32         phaseIncrement;

#if defined(_FILTER_ENABLED)
    EAS_I32         k;
    EAS_I32         b1;
    EAS_I32         b2;
#endif
} S_WT_FRAME;

/*----------------------------------------------------------------------------
 * S_WT_CONFIG
 *
 * This structure contains state data for the wavetable engine
 *----------------------------------------------------------------------------
*/
typedef struct s_wt_config_tag
{
    EAS_U32             loopEnd;                /* points to last PCM sample (not 1 beyond last) */
    EAS_U32             loopStart;              /* points to first sample at start of loop */
    EAS_U32             phaseAccum;             /* current sample, integer portion of phase */

#if (NUM_OUTPUT_CHANNELS == 2)
    EAS_I16             gainLeft;               /* left channel gain */
    EAS_I16             gainRight;              /* right channel gain */
#endif

    EAS_I16             gain;                   /* current voice gain */
} S_WT_CONFIG;

#endif

