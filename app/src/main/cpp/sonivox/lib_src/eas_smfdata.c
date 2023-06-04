/*----------------------------------------------------------------------------
 *
 * File:
 * eas_smfdata.c
 *
 * Contents and purpose:
 * SMF File Parser
 *
 * This file contains data definitions for the SMF parser.
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
 *   $Revision: 778 $
 *   $Date: 2007-07-23 16:45:17 -0700 (Mon, 23 Jul 2007) $
 *----------------------------------------------------------------------------
*/

#include "eas_miditypes.h"
#include "eas_smfdata.h"

/*----------------------------------------------------------------------------
 *
 * S_SMF_STREAM
 *
 * Static memory allocation for SMF parser
 *----------------------------------------------------------------------------
*/
static S_SMF_STREAM eas_SMFStreams[MAX_SMF_STREAMS];

/*----------------------------------------------------------------------------
 *
 * eas_SMFData
 *
 * Static memory allocation for SMF parser
 *----------------------------------------------------------------------------
*/
S_SMF_DATA eas_SMFData =
{
    eas_SMFStreams,     /* pointer to individual streams in file */
    0,                  /* pointer to next stream with event */
    0,                  /* pointer to synth */
    0,                  /* file handle */
    { 0, 0, 0, 0},      /* metadata callback */
    0,                  /* file offset */
    0,                  /* current time in milliseconds/256 */
    0,                  /* actual number of streams */
    0,                  /* current MIDI tick to msec conversion */
    0,                  /* ticks per quarter note */
    0,                  /* current state EAS_STATE_XXXX */
    0                   /* flags */
};

