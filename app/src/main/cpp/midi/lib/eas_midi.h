/*----------------------------------------------------------------------------
 *
 * File:
 * eas_midi.h
 *
 * Contents and purpose:
 * Prototypes for MIDI stream parsing functions
 *
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
 *   $Revision: 82 $
 *   $Date: 2006-07-10 11:45:19 -0700 (Mon, 10 Jul 2006) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_MIDI_H
#define _EAS_MIDI_H

/*----------------------------------------------------------------------------
 * EAS_InitMIDIStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Initializes the MIDI stream state for parsing.
 *
 * Inputs:
 *
 * Outputs:
 * returns EAS_RESULT (EAS_SUCCESS is OK)
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
void EAS_InitMIDIStream (S_MIDI_STREAM *pMIDIStream);

/*----------------------------------------------------------------------------
 * EAS_ParseMIDIStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Parses a MIDI input stream character by character. Characters are pushed (rather than pulled)
 * so the interface works equally well for both file and stream I/O.
 *
 * Inputs:
 * c            - character from MIDI stream
 *
 * Outputs:
 * returns EAS_RESULT (EAS_SUCCESS is OK)
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_ParseMIDIStream (S_EAS_DATA *pEASData, S_SYNTH *pSynth, S_MIDI_STREAM *pMIDIStream, EAS_U8 c, EAS_INT parserMode);

#endif /* #define _EAS_MIDI_H */

