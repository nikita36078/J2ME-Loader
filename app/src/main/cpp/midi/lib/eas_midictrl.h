/*----------------------------------------------------------------------------
 *
 * File:
 * eas_midictrl.h
 *
 * Contents and purpose:
 * MIDI controller definitions
 *
 * This header only contains declarations that are specific
 * to this implementation.
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

#ifndef _EAS_MIDICTRL_H
#define _EAS_MIDICTRL_H

/* define controller types */
/*
    Note that these controller types are specified in base 10 (decimal)
    and not in hexadecimal. The above midi messages are specified
    in hexadecimal.
*/
#define MIDI_CONTROLLER_BANK_SELECT         0
#define MIDI_CONTROLLER_BANK_SELECT_MSB     0
#define MIDI_CONTROLLER_MOD_WHEEL           1
#define MIDI_CONTROLLER_ENTER_DATA_MSB      6
#define MIDI_CONTROLLER_VOLUME              7
#define MIDI_CONTROLLER_PAN                 10
#define MIDI_CONTROLLER_EXPRESSION          11
#define MIDI_CONTROLLER_BANK_SELECT_LSB     32
#define MIDI_CONTROLLER_ENTER_DATA_LSB      38      /* 0x26 */
#define MIDI_CONTROLLER_SUSTAIN_PEDAL       64
#define MIDI_CONTROLLER_SELECT_NRPN_LSB     98
#define MIDI_CONTROLLER_SELECT_NRPN_MSB     99
#define MIDI_CONTROLLER_SELECT_RPN_LSB      100     /* 0x64 */
#define MIDI_CONTROLLER_SELECT_RPN_MSB      101     /* 0x65 */
#define MIDI_CONTROLLER_ALL_SOUND_OFF       120
#define MIDI_CONTROLLER_RESET_CONTROLLERS   121
#define MIDI_CONTROLLER_ALL_NOTES_OFF       123
#define MIDI_CONTROLLER_OMNI_OFF            124
#define MIDI_CONTROLLER_OMNI_ON             125
#define MIDI_CONTROLLER_MONO_ON_POLY_OFF    126
#define MIDI_CONTROLLER_POLY_ON_MONO_OFF    127

#endif /* #ifndef _EAS_MIDICTRL_H */
