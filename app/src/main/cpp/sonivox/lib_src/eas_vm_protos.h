/*----------------------------------------------------------------------------
 *
 * File:
 * eas_vm_protos.h
 *
 * Contents and purpose:
 * Declarations, interfaces, and prototypes for voice manager.
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
 *   $Revision: 736 $
 *   $Date: 2007-06-22 13:51:24 -0700 (Fri, 22 Jun 2007) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_VM_PROTOS_H
#define _EAS_VM_PROTOS_H

// includes
#include "eas_data.h"
#include "eas_sndlib.h"

/*----------------------------------------------------------------------------
 * VMInitialize()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMInitialize (S_EAS_DATA *pEASData);

/*----------------------------------------------------------------------------
 * VMInitMIDI()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMInitMIDI (S_EAS_DATA *pEASData, S_SYNTH **ppSynth);

/*----------------------------------------------------------------------------
 * VMInitializeAllChannels()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
void VMInitializeAllChannels (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth);

/*----------------------------------------------------------------------------
 * VMResetControllers()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
void VMResetControllers (S_SYNTH *pSynth);

/*----------------------------------------------------------------------------
 * VMInitMIPTable()
 *----------------------------------------------------------------------------
 * Purpose:
 * Initialize the SP-MIDI MIP table
 *
 * Inputs:
 * pEASData         - pointer to synthesizer instance data
 * mute             - EAS_FALSE to unmute channels, EAS_TRUE to mute
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
void VMInitMIPTable (S_SYNTH *pSynth);

/*----------------------------------------------------------------------------
 * VMSetMIPEntry()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the priority and MIP level for a MIDI channel
 *
 * Inputs:
 * pEASData         - pointer to synthesizer instance data
 * channel          - MIDI channel number
 * priority         - priority (0-15 with 0 = highest priority)
 * mip              - maximum instantaneous polyphony
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
void VMSetMIPEntry (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 priority, EAS_U8 mip);

/*----------------------------------------------------------------------------
 * VMUpdateMIPTable()
 *----------------------------------------------------------------------------
 * Purpose:
 * This routine is called when the polyphony count in the synthesizer changes
 *
 * Inputs:
 * pEASData         - pointer to synthesizer instance data
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
void VMUpdateMIPTable (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth);

/*----------------------------------------------------------------------------
 * VMInitializeAllVoices()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
void VMInitializeAllVoices (S_VOICE_MGR *pVoiceMgr, EAS_INT vSynthNum);

/*----------------------------------------------------------------------------
 * VMStartNote()
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the synth's state to play the requested note on the requested
 * channel if possible.
 *
 * Inputs:
 * nChannel - the MIDI channel
 * nKeyNumber - the MIDI key number for this note
 * nNoteVelocity - the key velocity for this note
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
void VMStartNote (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 note, EAS_U8 velocity);

/*----------------------------------------------------------------------------
 * VMCheckKeyGroup()
 *----------------------------------------------------------------------------
 * Purpose:
 * If the note that we've been asked to start is in the same key group as
 * any currently playing notes, then we must shut down the currently playing
 * note in the same key group and then start the newly requested note.
 *
 * Inputs:
 * nChannel - synth channel that wants to start a new note
 * nKeyNumber - new note's midi note number
 * nRegionIndex - calling routine finds this index and gives to us
 * nNoteVelocity - new note's velocity
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * pbVoiceStealingRequired - flag: this routine sets true if we needed to
 *                                 steal a voice
 *
 * Side Effects:
 * gsSynthObject.m_sVoice[free voice num].m_nKeyNumber may be assigned
 * gsSynthObject.m_sVoice[free voice num].m_nVelocity may be assigned
 *----------------------------------------------------------------------------
*/
void VMCheckKeyGroup (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U16 keyGroup, EAS_U8 channel);

/*----------------------------------------------------------------------------
 * VMCheckPolyphonyLimiting()
 *----------------------------------------------------------------------------
 * Purpose:
 * We only play at most 2 of the same note on a MIDI channel.
 * E.g., if we are asked to start note 36, and there are already two voices
 * that are playing note 36, then we must steal the voice playing
 * the oldest note 36 and use that stolen voice to play the new note 36.
 *
 * Inputs:
 * nChannel - synth channel that wants to start a new note
 * nKeyNumber - new note's midi note number
 * nNoteVelocity - new note's velocity
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * pbVoiceStealingRequired - flag: this routine sets true if we needed to
 *                                 steal a voice
 * *
 * Side Effects:
 * psSynthObject->m_sVoice[free voice num].m_nKeyNumber may be assigned
 * psSynthObject->m_sVoice[free voice num].m_nVelocity may be assigned
 *----------------------------------------------------------------------------
*/
EAS_BOOL VMCheckPolyphonyLimiting (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 note, EAS_U8 velocity, EAS_U16 regionIndex, EAS_I32 lowVoice, EAS_I32 highVoice);

/*----------------------------------------------------------------------------
 * VMStopNote()
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the synth's state to end the requested note on the requested
 * channel.
 *
 * Inputs:
 * nChannel - the MIDI channel
 * nKeyNumber - the key number of the note to stop
 * nNoteVelocity - the note-off velocity
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * Side Effects:
 * gsSynthObject.m_sVoice[free voice num].m_nSynthChannel may be assigned
 * gsSynthObject.m_sVoice[free voice num].m_nKeyNumber is assigned
 * gsSynthObject.m_sVoice[free voice num].m_nVelocity is assigned
 *----------------------------------------------------------------------------
*/
void VMStopNote (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 key, EAS_U8 velocity);

/*----------------------------------------------------------------------------
 * VMFindAvailableVoice()
 *----------------------------------------------------------------------------
 * Purpose:
 * Find an available voice and return the voice number if available.
 *
 * Inputs:
 * pnVoiceNumber - really an output, see below
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * pnVoiceNumber - returns the voice number of available voice if found
 * success - if there is an available voice
 * failure - otherwise
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMFindAvailableVoice (S_VOICE_MGR *pVoiceMgr, EAS_INT *pVoiceNumber, EAS_I32 lowVoice, EAS_I32 highVoice);

/*----------------------------------------------------------------------------
 * VMStealVoice()
 *----------------------------------------------------------------------------
 * Purpose:
 * Steal a voice and return the voice number
 *
 * Stealing algorithm: steal the best choice with minimal work, taking into
 * account SP-Midi channel priorities and polyphony allocation.
 *
 * In one pass through all the voices, figure out which voice to steal
 * taking into account a number of different factors:
 * Priority of the voice's MIDI channel
 * Number of voices over the polyphony allocation for voice's MIDI channel
 * Amplitude of the voice
 * Note age
 * Key velocity (for voices that haven't been started yet)
 * If any matching notes are found
 *
 * Inputs:
 * nChannel - the channel that this voice wants to be started on
 * nKeyNumber - the key number for this new voice
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * pnVoiceNumber - voice stolen
 * EAS_RESULT EAS_SUCCESS - always successful
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMStealVoice (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_INT *pVoiceNumber, EAS_U8 channel, EAS_U8 note, EAS_I32 lowVoice, EAS_I32 highVoice);

/*----------------------------------------------------------------------------
 * VMAddSamples()
 *----------------------------------------------------------------------------
 * Purpose:
 * Synthesize the requested number of samples.
 *
 * Inputs:
 * nNumSamplesToAdd - number of samples to write to buffer
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * number of samples actually written to buffer
 *
 * Side Effects:
 * - samples are added to the presently free buffer
 *
 *----------------------------------------------------------------------------
*/
EAS_I32 VMAddSamples (S_VOICE_MGR *pVoiceMgr, EAS_I32 *pMixBuffer, EAS_I32 numSamplesToAdd);

/*----------------------------------------------------------------------------
 * VMProgramChange()
 *----------------------------------------------------------------------------
 * Purpose:
 * Change the instrument (program) for the given channel.
 *
 * Depending on the program number, and the bank selected for this channel, the
 * program may be in ROM, RAM (from SMAF or CMX related RAM wavetable), or
 * Alternate wavetable (from mobile DLS or other DLS file)
 *
 * This function figures out what wavetable should be used, and sets it up as the
 * wavetable to use for this channel. Also the channel may switch from a melodic
 * channel to a rhythm channel, or vice versa.
 *
 * Inputs:
 *
 * Outputs:
 * Side Effects:
 * gsSynthObject.m_sChannel[nChannel].m_nProgramNumber is likely changed
 * gsSynthObject.m_sChannel[nChannel].m_psEAS may be changed
 * gsSynthObject.m_sChannel[nChannel].m_bRhythmChannel may be changed
 *
 *----------------------------------------------------------------------------
*/
void VMProgramChange (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 program);

/*----------------------------------------------------------------------------
 * VMChannelPressure()
 *----------------------------------------------------------------------------
 * Purpose:
 * Change the channel pressure for the given channel
 *
 * Inputs:
 * nChannel - the MIDI channel
 * nVelocity - the channel pressure value
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * Side Effects:
 * gsSynthObject.m_sChannel[nChannel].m_nChannelPressure is updated
 *----------------------------------------------------------------------------
*/
void VMChannelPressure (S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 value);

/*----------------------------------------------------------------------------
 * VMPitchBend()
 *----------------------------------------------------------------------------
 * Purpose:
 * Change the pitch wheel value for the given channel.
 * This routine constructs the proper 14-bit argument when the calling routine
 * passes the pitch LSB and MSB.
 *
 * Note: some midi disassemblers display a bipolar pitch bend value.
 * We can display the bipolar value using
 * if m_nPitchBend >= 0x2000
 *      bipolar pitch bend = postive (m_nPitchBend - 0x2000)
 * else
 *      bipolar pitch bend = negative (0x2000 - m_nPitchBend)
 *
 * Inputs:
 * nChannel - the MIDI channel
 * nPitchLSB - the LSB byte from the pitch bend message
 * nPitchMSB - the MSB byte from the message
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 * gsSynthObject.m_sChannel[nChannel].m_nPitchBend is changed
 *
 *----------------------------------------------------------------------------
*/
void VMPitchBend (S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 pitchLSB, EAS_U8 pitchMSB);

/*----------------------------------------------------------------------------
 * VMControlChange()
 *----------------------------------------------------------------------------
 * Purpose:
 * Change the controller (or mode) for the given channel.
 *
 * Inputs:
 * nChannel - the MIDI channel
 * nControllerNumber - the controller number
 * nControlValue - the controller number for this control change
 * nControlValue - the value for this control change
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * Side Effects:
 * gsSynthObject.m_sChannel[nChannel] controller is changed
 *
 *----------------------------------------------------------------------------
*/
void VMControlChange (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 controller, EAS_U8 value);

/*----------------------------------------------------------------------------
 * VMUpdateRPNStateMachine()
 *----------------------------------------------------------------------------
 * Purpose:
 * Call this function when we want to parse a stream of RPN messages.
 * NOTE: The synth has only one set of global RPN data instead of RPN data
 * per channel.
 * So actually, we don't really need to look at the nChannel parameter,
 * but we pass it to facilitate future upgrades. Furthermore, we only
 * support RPN0 (pitch bend sensitivity), RPN1 (fine tuning) and
 * RPN2 (coarse tuning). Any other RPNs are rejected.
 *
 * Inputs:
 * nChannel - the MIDI channel
 * nControllerNumber - the RPN controller number
 * nControlValue - the value for this control change
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 * gsSynthObject.m_RPN0 (or m_RPN1 or m_RPN2) may be updated if the
 * proper RPN message sequence is parsed.
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMUpdateRPNStateMachine (S_SYNTH *pSynth, EAS_U8 channel, EAS_U8 controller, EAS_U8 value);

/*----------------------------------------------------------------------------
 * VMUpdateStaticChannelParameters()
 *----------------------------------------------------------------------------
 * Purpose:
 * Update all of the static channel parameters for channels that have had
 * a controller change values
 * Or if the synth has signalled that all channels must forcibly
 * be updated
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * none
 *
 * Side Effects:
 * - psSynthObject->m_sChannel[].m_nStaticGain and m_nStaticPitch
 * are updated for channels whose controller values have changed
 * or if the synth has signalled that all channels must forcibly
 * be updated
 *----------------------------------------------------------------------------
*/
void VMUpdateStaticChannelParameters (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth);

/*----------------------------------------------------------------------------
 * VMReleaseAllDeferredNoteOffs()
 *----------------------------------------------------------------------------
 * Purpose:
 * Call this functin when the sustain flag is presently set but
 * we are now transitioning from damper pedal on to
 * damper pedal off. This means all notes in this channel
 * that received a note off while the damper pedal was on, and
 * had their note-off requests deferred, should now proceed to
 * the release state.
 *
 * Inputs:
 * nChannel - this channel has its sustain pedal transitioning from on to off
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * Side Effects:
 * any voice with deferred note offs on this channel are updated such that
 *
 *
 *----------------------------------------------------------------------------
*/
void VMReleaseAllDeferredNoteOffs (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel);

/*----------------------------------------------------------------------------
 * VMCatchNotesForSustainPedal()
 *----------------------------------------------------------------------------
 * Purpose:
 * Call this function when the sustain flag is presently clear and
 * the damper pedal is off and we are transitioning from damper pedal OFF to
 * damper pedal ON. Currently sounding notes should be left
 * unchanged. However, we should try to "catch" notes if possible.
 * If any notes have levels >= sustain level, catch them,
 * otherwise, let them continue to release.
 *
 * Inputs:
 * nChannel - this channel has its sustain pedal transitioning from on to off
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * Side Effects:
 * any voice with deferred note offs on this channel are updated such that
 * psVoice->m_sEG1.m_eState = eEnvelopeStateSustainPedal
 *----------------------------------------------------------------------------
*/
void VMCatchNotesForSustainPedal (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel);

/*----------------------------------------------------------------------------
 * VMUpdateAllNotesAge()
 *----------------------------------------------------------------------------
 * Purpose:
 * Increment the note age for all voices older than the age of the voice
 * that is stopping, effectively making the voices "younger".
 *
 * Inputs:
 * nAge - age of voice that is going away
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 * m_nAge for some voices is incremented
 *----------------------------------------------------------------------------
*/
void VMUpdateAllNotesAge (S_VOICE_MGR *pVoiceMgr, EAS_U16 nAge);

/*----------------------------------------------------------------------------
 * VMFindRegionIndex()
 *----------------------------------------------------------------------------
 * Purpose:
 * Find the region index for the given instrument using the midi key number
 * and the RPN2 (coarse tuning) value. By using RPN2 as part of the
 * region selection process, we reduce the amount a given sample has
 * to be transposed by selecting the closest recorded root instead.
 *
 * Inputs:
 * nChannel - current channel for this note
 * nKeyNumber - current midi note number
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * pnRegionIndex - valid only if we returned success
 * success if we found the region index number, otherwise
 * failure
 *
 * Side Effects:
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMFindRegionIndex (S_VOICE_MGR *pVoiceMgr, EAS_U8 channel, EAS_U8 note, EAS_U16 *pRegionIndex);

/*----------------------------------------------------------------------------
 * VMIncRefCount()
 *----------------------------------------------------------------------------
 * Increment reference count for virtual synth
 *----------------------------------------------------------------------------
*/
void VMIncRefCount (S_SYNTH *pSynth);

/*----------------------------------------------------------------------------
 * VMReset()
 *----------------------------------------------------------------------------
 * Purpose:
 * We call this routine to start the process of reseting the synth.
 * This routine sets a flag for the entire synth indicating that we want
 * to reset.
 * We also force all voices to mute quickly.
 * However, we do not actually perform any synthesis in this routine. That
 * is, we do not ramp the voices down from this routine, but instead, we
 * let the "regular" synth processing steps take care of adding the ramp
 * down samples to the output buffer. After we are sure that all voices
 * have completed ramping down, we continue the process of resetting the
 * synth (from another routine).
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 * - set a flag (in gsSynthObject.m_nFlags) indicating synth reset requested.
 * - force all voices to update their envelope states to mute
 *
 *----------------------------------------------------------------------------
*/
void VMReset (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_BOOL force);

/*----------------------------------------------------------------------------
 * VMMuteAllVoices()
 *----------------------------------------------------------------------------
 * Purpose:
 * We call this in an emergency reset situation.
 * This forces all voices to mute quickly.
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 * - forces all voices to update their envelope states to mute
 *
 *----------------------------------------------------------------------------
*/
void VMMuteVoice (S_VOICE_MGR *pVoiceMgr, EAS_I32 voiceNum);
void VMMuteAllVoices (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth);

/*----------------------------------------------------------------------------
 * VMReleaseAllVoices()
 *----------------------------------------------------------------------------
 * Purpose:
 * We call this after we've encountered the end of the Midi file.
 * This ensures all voice are either in release (because we received their
 * note off already) or forces them to mute quickly.
 * We use this as a safety to prevent bad midi files from playing forever.
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 * - forces all voices to update their envelope states to release or mute
 *
 *----------------------------------------------------------------------------
*/
void VMReleaseAllVoices (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth);

/*----------------------------------------------------------------------------
 * VMAllNotesOff()
 *----------------------------------------------------------------------------
 * Purpose:
 * Quickly mute all notes on the given channel.
 *
 * Inputs:
 * nChannel - quickly turn off all notes on this channel
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 * - forces all voices on this channel to update their envelope states to mute
 *
 *----------------------------------------------------------------------------
*/
void VMAllNotesOff (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_U8 channel);

/*----------------------------------------------------------------------------
 * VMDeferredStopNote()
 *----------------------------------------------------------------------------
 * Purpose:
 * Stop the notes that had deferred note-off requests.
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * None.
 *
 * Side Effects:
 * voices that have had deferred note-off requests are now put into release
 * gsSynthObject.m_sVoice[i].m_nFlags has the VOICE_FLAG_DEFER_MIDI_NOTE_OFF
 *  cleared
 *----------------------------------------------------------------------------
*/
void VMDeferredStopNote (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth);

/*----------------------------------------------------------------------------
 * VMSetSynthPolyphony()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the synth to a new polyphony value. Value must be >= 1 and
 * <= MAX_SYNTH_VOICES. This function will pin the polyphony at those limits
 *
 * Inputs:
 * pVoiceMgr        pointer to synthesizer data
 * synth            synthesizer number (0 = onboard, 1 = DSP)
 * polyphonyCount   desired polyphony count
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMSetSynthPolyphony (S_VOICE_MGR *pVoiceMgr, EAS_I32 synth, EAS_I32 polyphonyCount);

/*----------------------------------------------------------------------------
 * VMGetSynthPolyphony()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the synth to a new polyphony value. Value must be >= 1 and
 * <= MAX_SYNTH_VOICES. This function will pin the polyphony at those limits
 *
 * Inputs:
 * pVoiceMgr        pointer to synthesizer data
 * synth            synthesizer number (0 = onboard, 1 = DSP)
 * polyphonyCount   desired polyphony count
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMGetSynthPolyphony (S_VOICE_MGR *pVoiceMgr, EAS_I32 synth, EAS_I32 *pPolyphonyCount);

/*----------------------------------------------------------------------------
 * VMSetPolyphony()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the virtual synth polyphony. 0 = no limit (i.e. can use
 * all available voices).
 *
 * Inputs:
 * pVoiceMgr        pointer to synthesizer data
 * polyphonyCount   desired polyphony count
 * pSynth           pointer to virtual synth
 *
 * Outputs:
 * Returns error code
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMSetPolyphony (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_I32 polyphonyCount);

/*----------------------------------------------------------------------------
 * VMGetPolyphony()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the current polyphony setting
 *
 * Inputs:
 * pVoiceMgr        pointer to synthesizer data
 * pSynth           pointer to virtual synth
 * pPolyphonyCount  pointer to variable to receive data
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMGetPolyphony (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_I32 *pPolyphonyCount);

/*----------------------------------------------------------------------------
 * VMSetPriority()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the virtual synth priority
 *
 * Inputs:
 * pVoiceMgr        pointer to synthesizer data
 * priority         new priority
 * pSynth           pointer to virtual synth
 *
 * Outputs:
 * Returns error code
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMSetPriority (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_I32 priority);

/*----------------------------------------------------------------------------
 * VMGetPriority()
 *----------------------------------------------------------------------------
 * Purpose:
 * Get the virtual synth priority
 *
 * Inputs:
 * pVoiceMgr        pointer to synthesizer data
 * pPriority        pointer to variable to hold priority
 * pSynth           pointer to virtual synth
 *
 * Outputs:
 * Returns error code
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMGetPriority (S_VOICE_MGR *pVoiceMgr, S_SYNTH *pSynth, EAS_I32 *pPriority);

/*----------------------------------------------------------------------------
 * VMSetVolume()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the master volume for this sequence
 *
 * Inputs:
 * nSynthVolume - the desired master volume
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *
 * Side Effects:
 * overrides any previously set master volume from sysex
 *
 *----------------------------------------------------------------------------
*/
void VMSetVolume (S_SYNTH *pSynth, EAS_U16 masterVolume);

/*----------------------------------------------------------------------------
 * VMSetPitchBendRange()
 *----------------------------------------------------------------------------
 * Set the pitch bend range for the given channel.
 *----------------------------------------------------------------------------
*/
void VMSetPitchBendRange (S_SYNTH *pSynth, EAS_INT channel, EAS_I16 pitchBendRange);

/*----------------------------------------------------------------------------
 * VMSetEASLib()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the pointer to the sound library
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMSetGlobalEASLib (S_VOICE_MGR *pVoiceMgr, EAS_SNDLIB_HANDLE pEAS);
EAS_RESULT VMSetEASLib (S_SYNTH *pSynth, EAS_SNDLIB_HANDLE pEAS);

#ifdef DLS_SYNTHESIZER
/*----------------------------------------------------------------------------
 * VMSetDLSLib()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the pointer to the sound library
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMSetGlobalDLSLib (EAS_DATA_HANDLE pEASData, EAS_DLSLIB_HANDLE pDLS);
EAS_RESULT VMSetDLSLib (S_SYNTH *pSynth, EAS_DLSLIB_HANDLE pDLS);
#endif

/*----------------------------------------------------------------------------
 * VMSetTranposition()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the global key transposition used by the synthesizer.
 * Transposes all melodic instruments up or down by the specified
 * amount. Range is limited to +/-12 semitones.
 *
 * Inputs:
 * psEASData        - pointer to overall EAS data structure
 * transposition    - transpose amount (+/-12)
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
void VMSetTranposition (S_SYNTH *pSynth, EAS_I32 transposition);

/*----------------------------------------------------------------------------
 * VMGetTranposition()
 *----------------------------------------------------------------------------
 * Purpose:
 * Gets the global key transposition used by the synthesizer.
 * Transposes all melodic instruments up or down by the specified
 * amount. Range is limited to +/-12 semitones.
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
void VMGetTranposition (S_SYNTH *pSynth, EAS_I32 *pTransposition);

/*----------------------------------------------------------------------------
 * VMGetNoteCount()
 *----------------------------------------------------------------------------
* Returns the total note count
*----------------------------------------------------------------------------
*/
EAS_I32 VMGetNoteCount (S_SYNTH *pSynth);

/*----------------------------------------------------------------------------
 * VMRender()
 *----------------------------------------------------------------------------
 * Purpose:
 * This routine renders a frame of audio
 *
 * Inputs:
 * psEASData        - pointer to overall EAS data structure
 *
 * Outputs:
 * pVoicesRendered  - number of voices rendered this frame
 *
 * Side Effects:
 * sets psMidiObject->m_nMaxWorkloadPerFrame
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT VMRender (S_VOICE_MGR *pVoiceMgr, EAS_I32 numSamples, EAS_I32 *pMixBuffer, EAS_I32 *pVoicesRendered);

/*----------------------------------------------------------------------------
 * VMInitWorkload()
 *----------------------------------------------------------------------------
 * Purpose:
 * Clears the workload counter
 *
 * Inputs:
 * pVoiceMgr            - pointer to instance data
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
void VMInitWorkload (S_VOICE_MGR *pVoiceMgr);

/*----------------------------------------------------------------------------
 * VMSetWorkload()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the max workload for a single frame.
 *
 * Inputs:
 * pVoiceMgr            - pointer to instance data
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
void VMSetWorkload (S_VOICE_MGR *pVoiceMgr, EAS_I32 maxWorkLoad);

/*----------------------------------------------------------------------------
 * VMCheckWorkload()
 *----------------------------------------------------------------------------
 * Purpose:
 * Checks to see if work load has been exceeded on this frame.
 *
 * Inputs:
 * pVoiceMgr            - pointer to instance data
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_BOOL VMCheckWorkload (S_VOICE_MGR *pVoiceMgr);

/*----------------------------------------------------------------------------
 * VMActiveVoices()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the number of active voices in the synthesizer.
 *
 * Inputs:
 * pEASData         - pointer to instance data
 *
 * Outputs:
 * Returns the number of active voices
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_I32 VMActiveVoices (S_SYNTH *pSynth);

/*----------------------------------------------------------------------------
 * VMMIDIShutdown()
 *----------------------------------------------------------------------------
 * Purpose:
 * Clean up any Synth related system issues.
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * None
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
void VMMIDIShutdown (S_EAS_DATA *pEASData, S_SYNTH *pSynth);

/*----------------------------------------------------------------------------
 * VMShutdown()
 *----------------------------------------------------------------------------
 * Purpose:
 * Clean up any Synth related system issues.
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * None
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
void VMShutdown (S_EAS_DATA *pEASData);

#ifdef EXTERNAL_AUDIO
/*----------------------------------------------------------------------------
 * EAS_RegExtAudioCallback()
 *----------------------------------------------------------------------------
 * Register a callback for external audio processing
 *----------------------------------------------------------------------------
*/
void VMRegExtAudioCallback (S_SYNTH *pSynth, EAS_VOID_PTR pInstData, EAS_EXT_PRG_CHG_FUNC cbProgChgFunc, EAS_EXT_EVENT_FUNC cbEventFunc);

/*----------------------------------------------------------------------------
 * VMGetMIDIControllers()
 *----------------------------------------------------------------------------
 * Returns the MIDI controller values on the specified channel
 *----------------------------------------------------------------------------
*/
void VMGetMIDIControllers (S_SYNTH *pSynth, EAS_U8 channel, S_MIDI_CONTROLLERS *pControl);
#endif

#ifdef _SPLIT_ARCHITECTURE
/*----------------------------------------------------------------------------
 * VMStartFrame()
 *----------------------------------------------------------------------------
 * Purpose:
 * Starts an audio frame
 *
 * Inputs:
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_BOOL VMStartFrame (S_EAS_DATA *pEASData);

/*----------------------------------------------------------------------------
 * VMEndFrame()
 *----------------------------------------------------------------------------
 * Purpose:
 * Stops an audio frame
 *
 * Inputs:
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_BOOL VMEndFrame (S_EAS_DATA *pEASData);
#endif

#endif /* #ifdef _EAS_VM_PROTOS_H */

