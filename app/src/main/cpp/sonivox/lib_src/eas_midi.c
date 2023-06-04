/*----------------------------------------------------------------------------
 *
 * File:
 * eas_midi.c
 *
 * Contents and purpose:
 * This file implements the MIDI stream parser. It is called by eas_smf.c to parse MIDI messages
 * that are streamed out of the file. It can also parse live MIDI streams.
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
 *   $Revision: 794 $
 *   $Date: 2007-08-01 00:08:48 -0700 (Wed, 01 Aug 2007) $
 *----------------------------------------------------------------------------
*/

#include "eas_data.h"
#include "eas_report.h"
#include "eas_miditypes.h"
#include "eas_midi.h"
#include "eas_vm_protos.h"
#include "eas_parser.h"

#ifdef JET_INTERFACE
#include "jet_data.h"
#endif


/* state enumerations for ProcessSysExMessage */
typedef enum
{
    eSysEx,
    eSysExUnivNonRealTime,
    eSysExUnivNrtTargetID,
    eSysExGMControl,
    eSysExUnivRealTime,
    eSysExUnivRtTargetID,
    eSysExDeviceControl,
    eSysExMasterVolume,
    eSysExMasterVolLSB,
    eSysExSPMIDI,
    eSysExSPMIDIchan,
    eSysExSPMIDIMIP,
    eSysExMfgID1,
    eSysExMfgID2,
    eSysExMfgID3,
    eSysExEnhancer,
    eSysExEnhancerSubID,
    eSysExEnhancerFeedback1,
    eSysExEnhancerFeedback2,
    eSysExEnhancerDrive,
    eSysExEnhancerWet,
    eSysExEOX,
    eSysExIgnore
} E_SYSEX_STATES;

/* local prototypes */
static EAS_RESULT ProcessMIDIMessage (S_EAS_DATA *pEASData, S_SYNTH *pSynth, S_MIDI_STREAM *pMIDIStream, EAS_INT parserMode);
static EAS_RESULT ProcessSysExMessage (S_EAS_DATA *pEASData, S_SYNTH *pSynth, S_MIDI_STREAM *pMIDIStream, EAS_U8 c, EAS_INT parserMode);

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
void EAS_InitMIDIStream (S_MIDI_STREAM *pMIDIStream)
{
    pMIDIStream->byte3 = EAS_FALSE;
    pMIDIStream->pending = EAS_FALSE;
    pMIDIStream->runningStatus = 0;
    pMIDIStream->status = 0;
}

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
EAS_RESULT EAS_ParseMIDIStream (S_EAS_DATA *pEASData, S_SYNTH *pSynth, S_MIDI_STREAM *pMIDIStream, EAS_U8 c, EAS_INT parserMode)
{

    /* check for new status byte */
    if (c & 0x80)
    {
        /* save new running status */
        if (c < 0xf8)
        {
            pMIDIStream->runningStatus = c;
            pMIDIStream->byte3 = EAS_FALSE;

            /* deal with SysEx */
            if ((c == 0xf7) || (c == 0xf0))
            {
                if (parserMode == eParserModeMetaData)
                    return EAS_SUCCESS;
                return ProcessSysExMessage(pEASData, pSynth, pMIDIStream, c, parserMode);
            }

            /* inform the file parser that we're in the middle of a message */
            if ((c < 0xf4) || (c > 0xf6))
                pMIDIStream->pending = EAS_TRUE;
        }

        /* real-time message - ignore it */
        return EAS_SUCCESS;
    }

    /* 3rd byte of a 3-byte message? */
    if (pMIDIStream->byte3)
    {
        pMIDIStream->d2 = c;
        pMIDIStream->byte3 = EAS_FALSE;
        pMIDIStream->pending = EAS_FALSE;
        if (parserMode == eParserModeMetaData)
            return EAS_SUCCESS;
        return ProcessMIDIMessage(pEASData, pSynth, pMIDIStream, parserMode);
    }

    /* check for status received */
    if (pMIDIStream->runningStatus)
    {

        /* save new status and data byte */
        pMIDIStream->status = pMIDIStream->runningStatus;

        /* check for 3-byte messages */
        if (pMIDIStream->status < 0xc0)
        {
            pMIDIStream->d1 = c;
            pMIDIStream->pending = EAS_TRUE;
            pMIDIStream->byte3 = EAS_TRUE;
            return EAS_SUCCESS;
        }

        /* check for 2-byte messages */
        if (pMIDIStream->status < 0xe0)
        {
            pMIDIStream->d1 = c;
            pMIDIStream->pending = EAS_FALSE;
            if (parserMode == eParserModeMetaData)
                return EAS_SUCCESS;
            return ProcessMIDIMessage(pEASData, pSynth, pMIDIStream, parserMode);
        }

        /* check for more 3-bytes message */
        if (pMIDIStream->status < 0xf0)
        {
            pMIDIStream->d1 = c;
            pMIDIStream->pending = EAS_TRUE;
            pMIDIStream->byte3 = EAS_TRUE;
            return EAS_SUCCESS;
        }

        /* SysEx message? */
        if (pMIDIStream->status == 0xF0)
        {
            if (parserMode == eParserModeMetaData)
                return EAS_SUCCESS;
            return ProcessSysExMessage(pEASData, pSynth, pMIDIStream, c, parserMode);
        }

        /* remaining messages all clear running status */
        pMIDIStream->runningStatus = 0;

        /* F2 is 3-byte message */
        if (pMIDIStream->status == 0xf2)
        {
            pMIDIStream->byte3 = EAS_TRUE;
            return EAS_SUCCESS;
        }
    }

    /* no status byte received, provide a warning, but we should be able to recover */
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Received MIDI data without a valid status byte: %d\n",c); */ }
    pMIDIStream->pending = EAS_FALSE;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * ProcessMIDIMessage()
 *----------------------------------------------------------------------------
 * Purpose:
 * This function processes a typical MIDI message. All of the data has been received, just need
 * to take appropriate action.
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ProcessMIDIMessage (S_EAS_DATA *pEASData, S_SYNTH *pSynth, S_MIDI_STREAM *pMIDIStream, EAS_INT parserMode)
{
    EAS_U8 channel;

    channel = pMIDIStream->status & 0x0f;
    switch (pMIDIStream->status & 0xf0)
    {
    case 0x80:
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL,"NoteOff: %02x %02x %02x\n",
            pMIDIStream->status, pMIDIStream->d1, pMIDIStream->d2); */ }
        if (parserMode <= eParserModeMute)
            VMStopNote(pEASData->pVoiceMgr, pSynth, channel, pMIDIStream->d1, pMIDIStream->d2);
        break;

    case 0x90:
        if (pMIDIStream->d2)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL,"NoteOn: %02x %02x %02x\n",
                pMIDIStream->status, pMIDIStream->d1, pMIDIStream->d2); */ }
            pMIDIStream->flags |= MIDI_FLAG_FIRST_NOTE;
            if (parserMode == eParserModePlay)
                VMStartNote(pEASData->pVoiceMgr, pSynth, channel, pMIDIStream->d1, pMIDIStream->d2);
        }
        else
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL,"NoteOff: %02x %02x %02x\n",
                pMIDIStream->status, pMIDIStream->d1, pMIDIStream->d2); */ }
            if (parserMode <= eParserModeMute)
                VMStopNote(pEASData->pVoiceMgr, pSynth, channel, pMIDIStream->d1, pMIDIStream->d2);
        }
        break;

    case 0xa0:
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL,"PolyPres: %02x %02x %02x\n",
            pMIDIStream->status, pMIDIStream->d1, pMIDIStream->d2); */ }
        break;

    case 0xb0:
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL,"Control: %02x %02x %02x\n",
            pMIDIStream->status, pMIDIStream->d1, pMIDIStream->d2); */ }
        if (parserMode <= eParserModeMute)
            VMControlChange(pEASData->pVoiceMgr, pSynth, channel, pMIDIStream->d1, pMIDIStream->d2);
#ifdef JET_INTERFACE
        if (pMIDIStream->jetData & MIDI_FLAGS_JET_CB)
        {
            JET_Event(pEASData, pMIDIStream->jetData & (JET_EVENT_SEG_MASK | JET_EVENT_TRACK_MASK),
                channel, pMIDIStream->d1, pMIDIStream->d2);
        }
#endif
        break;

    case 0xc0:
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL,"Program: %02x %02x\n",
            pMIDIStream->status, pMIDIStream->d1); */ }
        if (parserMode <= eParserModeMute)
            VMProgramChange(pEASData->pVoiceMgr, pSynth, channel, pMIDIStream->d1);
        break;

    case 0xd0:
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL,"ChanPres: %02x %02x\n",
            pMIDIStream->status, pMIDIStream->d1); */ }
        if (parserMode <= eParserModeMute)
            VMChannelPressure(pSynth, channel, pMIDIStream->d1);
        break;

    case 0xe0:
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL,"PBend: %02x %02x %02x\n",
            pMIDIStream->status, pMIDIStream->d1, pMIDIStream->d2); */ }
        if (parserMode <= eParserModeMute)
            VMPitchBend(pSynth, channel, pMIDIStream->d1, pMIDIStream->d2);
        break;

    default:
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL,"Unknown: %02x %02x %02x\n",
            pMIDIStream->status, pMIDIStream->d1, pMIDIStream->d2); */ }
    }
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * ProcessSysExMessage()
 *----------------------------------------------------------------------------
 * Purpose:
 * Process a SysEx character byte from the MIDI stream. Since we cannot
 * simply wait for the next character to arrive, we are forced to save
 * state after each character. It would be easier to parse at the file
 * level, but then we lose the nice feature of being able to support
 * these messages in a real-time MIDI stream.
 *
 * Inputs:
 * pEASData         - pointer to synthesizer instance data
 * c                - character to be processed
 * locating         - if true, the sequencer is relocating to a new position
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 * Notes:
 * These are the SysEx messages we can receive:
 *
 * SysEx messages
 * { f0 7e 7f 09 01 f7 } GM 1 On
 * { f0 7e 7f 09 02 f7 } GM 1/2 Off
 * { f0 7e 7f 09 03 f7 } GM 2 On
 * { f0 7f 7f 04 01 lsb msb } Master Volume
 * { f0 7f 7f 0b 01 ch mip [ch mip ...] f7 } SP-MIDI
 * { f0 00 01 3a 04 01 fdbk1 fdbk2 drive wet dry f7 } Enhancer
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ProcessSysExMessage (S_EAS_DATA *pEASData, S_SYNTH *pSynth, S_MIDI_STREAM *pMIDIStream, EAS_U8 c, EAS_INT parserMode)
{

    /* check for start byte */
    if (c == 0xf0)
    {
        pMIDIStream->sysExState = eSysEx;
    }
    /* check for end byte */
    else if (c == 0xf7)
    {
        /* if this was a MIP message, update the MIP table */
        if ((pMIDIStream->sysExState == eSysExSPMIDIchan) && (parserMode != eParserModeMetaData))
            VMUpdateMIPTable(pEASData->pVoiceMgr, pSynth);
        pMIDIStream->sysExState = eSysExIgnore;
    }

    /* process SysEx message */
    else
    {
        switch (pMIDIStream->sysExState)
        {
        case eSysEx:

            /* first byte, determine message class */
            switch (c)
            {
            case 0x7e:
                pMIDIStream->sysExState = eSysExUnivNonRealTime;
                break;
            case 0x7f:
                pMIDIStream->sysExState = eSysExUnivRealTime;
                break;
            case 0x00:
                pMIDIStream->sysExState = eSysExMfgID1;
                break;
            default:
                pMIDIStream->sysExState = eSysExIgnore;
                break;
            }
            break;

        /* process GM message */
        case eSysExUnivNonRealTime:
            if (c == 0x7f)
                pMIDIStream->sysExState = eSysExUnivNrtTargetID;
            else
                pMIDIStream->sysExState = eSysExIgnore;
            break;

        case eSysExUnivNrtTargetID:
            if (c == 0x09)
                pMIDIStream->sysExState = eSysExGMControl;
            else
                pMIDIStream->sysExState = eSysExIgnore;
            break;

        case eSysExGMControl:
            if ((c == 1) || (c == 3))
            {
                /* GM 1 or GM2 On, reset synth */
                if (parserMode != eParserModeMetaData)
                {
                    pMIDIStream->flags |= MIDI_FLAG_GM_ON;
                    VMReset(pEASData->pVoiceMgr, pSynth, EAS_FALSE);
                    VMInitMIPTable(pSynth);
                }
                pMIDIStream->sysExState = eSysExEOX;
            }
            else
                pMIDIStream->sysExState = eSysExIgnore;
            break;

        /* Process Master Volume and SP-MIDI */
        case eSysExUnivRealTime:
            if (c == 0x7f)
                pMIDIStream->sysExState = eSysExUnivRtTargetID;
            else
                pMIDIStream->sysExState = eSysExIgnore;
            break;

        case eSysExUnivRtTargetID:
            if (c == 0x04)
                pMIDIStream->sysExState = eSysExDeviceControl;
            else if (c == 0x0b)
                pMIDIStream->sysExState = eSysExSPMIDI;
            else
                pMIDIStream->sysExState = eSysExIgnore;
            break;

        /* process master volume */
        case eSysExDeviceControl:
            if (c == 0x01)
                pMIDIStream->sysExState = eSysExMasterVolume;
            else
                pMIDIStream->sysExState = eSysExIgnore;
            break;

        case eSysExMasterVolume:
            /* save LSB */
            pMIDIStream->d1 = c;
            pMIDIStream->sysExState = eSysExMasterVolLSB;
            break;

        case eSysExMasterVolLSB:
            if (parserMode != eParserModeMetaData)
            {
                EAS_I32 gain = ((EAS_I32) c << 8) | ((EAS_I32) pMIDIStream->d1 << 1);
                gain = (gain * gain) >> 15;
                VMSetVolume(pSynth, (EAS_U16) gain);
            }
            pMIDIStream->sysExState = eSysExEOX;
            break;

        /* process SP-MIDI MIP message */
        case eSysExSPMIDI:
            if (c == 0x01)
            {
                /* assume all channels are muted */
                if (parserMode != eParserModeMetaData)
                    VMInitMIPTable(pSynth);
                pMIDIStream->d1 = 0;
                pMIDIStream->sysExState = eSysExSPMIDIchan;
            }
            else
                pMIDIStream->sysExState = eSysExIgnore;
            break;

        case eSysExSPMIDIchan:
            if (c < NUM_SYNTH_CHANNELS)
            {
                pMIDIStream->d2 = c;
                pMIDIStream->sysExState = eSysExSPMIDIMIP;
            }
            else
            {
                /* bad MIP message - unmute channels */
                if (parserMode != eParserModeMetaData)
                    VMInitMIPTable(pSynth);
                pMIDIStream->sysExState = eSysExIgnore;
            }
            break;

        case eSysExSPMIDIMIP:
            /* process MIP entry here */
            if (parserMode != eParserModeMetaData)
                VMSetMIPEntry(pEASData->pVoiceMgr, pSynth, pMIDIStream->d2, pMIDIStream->d1, c);
            pMIDIStream->sysExState = eSysExSPMIDIchan;

            /* if 16 channels received, update MIP table */
            if (++pMIDIStream->d1 == NUM_SYNTH_CHANNELS)
            {
                if (parserMode != eParserModeMetaData)
                    VMUpdateMIPTable(pEASData->pVoiceMgr, pSynth);
                pMIDIStream->sysExState = eSysExEOX;
            }
            break;

        /* process Enhancer */
        case eSysExMfgID1:
            if (c == 0x01)
                pMIDIStream->sysExState = eSysExMfgID1;
            else
                pMIDIStream->sysExState = eSysExIgnore;
            break;

        case eSysExMfgID2:
            if (c == 0x3a)
                pMIDIStream->sysExState = eSysExMfgID1;
            else
                pMIDIStream->sysExState = eSysExIgnore;
            break;

        case eSysExMfgID3:
            if (c == 0x04)
                pMIDIStream->sysExState = eSysExEnhancer;
            else
                pMIDIStream->sysExState = eSysExIgnore;
            break;

        case eSysExEnhancer:
            if (c == 0x01)
                pMIDIStream->sysExState = eSysExEnhancerSubID;
            else
                pMIDIStream->sysExState = eSysExIgnore;
            break;

        case eSysExEnhancerSubID:
            pMIDIStream->sysExState = eSysExEnhancerFeedback1;
            break;

        case eSysExEnhancerFeedback1:
            pMIDIStream->sysExState = eSysExEnhancerFeedback2;
            break;

        case eSysExEnhancerFeedback2:
            pMIDIStream->sysExState = eSysExEnhancerDrive;
            break;

        case eSysExEnhancerDrive:
            pMIDIStream->sysExState = eSysExEnhancerWet;
            break;

        case eSysExEnhancerWet:
            pMIDIStream->sysExState = eSysExEOX;
            break;

        case eSysExEOX:
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Expected F7, received %02x\n", c); */ }
            pMIDIStream->sysExState = eSysExIgnore;
            break;

        case eSysExIgnore:
            break;

        default:
            pMIDIStream->sysExState = eSysExIgnore;
            break;
        }
    }

    if (pMIDIStream->sysExState == eSysExIgnore)
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Ignoring SysEx byte %02x\n", c); */ }
    return EAS_SUCCESS;
} /* end ProcessSysExMessage */

