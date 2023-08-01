/*----------------------------------------------------------------------------
 *
 * File:
 * eas_smf.c
 *
 * Contents and purpose:
 * SMF Type 0 and 1 File Parser
 *
 * For SMF timebase analysis, see "MIDI Sequencer Analysis.xls".
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
 *   $Revision: 803 $
 *   $Date: 2007-08-01 09:57:00 -0700 (Wed, 01 Aug 2007) $
 *----------------------------------------------------------------------------
*/

#define LOG_TAG "Sonivox"
#include "util/log.h"

#include "eas_data.h"
#include "eas_miditypes.h"
#include "eas_parser.h"
#include "eas_report.h"
#include "eas_host.h"
#include "eas_midi.h"
#include "eas_config.h"
#include "eas_vm_protos.h"
#include "eas_smfdata.h"
#include "eas_smf.h"

#ifdef JET_INTERFACE
#include "jet_data.h"
#endif

//3 dls: The timebase for this module is adequate to keep MIDI and
//3 digital audio synchronized for only a few minutes. It should be
//3 sufficient for most mobile applications. If better accuracy is
//3 required, more fractional bits should be added to the timebase.

static const EAS_U8 smfHeader[] = { 'M', 'T', 'h', 'd' };

/* local prototypes */
static EAS_RESULT SMF_GetVarLenData (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE fileHandle, EAS_U32 *pData);
static EAS_RESULT SMF_ParseMetaEvent (S_EAS_DATA *pEASData, S_SMF_DATA *pSMFData, S_SMF_STREAM *pSMFStream);
static EAS_RESULT SMF_ParseSysEx (S_EAS_DATA *pEASData, S_SMF_DATA *pSMFData, S_SMF_STREAM *pSMFStream, EAS_U8 f0, EAS_INT parserMode);
static EAS_RESULT SMF_ParseEvent (S_EAS_DATA *pEASData, S_SMF_DATA *pSMFData, S_SMF_STREAM *pSMFStream, EAS_INT parserMode);
static EAS_RESULT SMF_GetDeltaTime (EAS_HW_DATA_HANDLE hwInstData, S_SMF_STREAM *pSMFStream);
static void SMF_UpdateTime (S_SMF_DATA *pSMFData, EAS_U32 ticks);


/*----------------------------------------------------------------------------
 *
 * SMF_Parser
 *
 * This structure contains the functional interface for the SMF parser
 *----------------------------------------------------------------------------
*/
const S_FILE_PARSER_INTERFACE EAS_SMF_Parser =
{
    SMF_CheckFileType,
    SMF_Prepare,
    SMF_Time,
    SMF_Event,
    SMF_State,
    SMF_Close,
    SMF_Reset,
#ifdef JET_INTERFACE
    SMF_Pause,
    SMF_Resume,
#else
    NULL,
    NULL,
#endif
    NULL,
    SMF_SetData,
    SMF_GetData,
    NULL
};

/*----------------------------------------------------------------------------
 * SMF_CheckFileType()
 *----------------------------------------------------------------------------
 * Purpose:
 * Check the file type to see if we can parse it
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT SMF_CheckFileType (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, EAS_VOID_PTR *ppHandle, EAS_I32 offset)
{
    S_SMF_DATA* pSMFData;
    EAS_RESULT result;

    /* seek to starting offset - usually 0 */
    *ppHandle = NULL;
    if ((result = EAS_HWFileSeek(pEASData->hwInstData, fileHandle, offset)) != EAS_SUCCESS)
        return result;

#ifdef FILE_HEADER_SEARCH
    /* search through file for header - slow method */
    if (pEASData->searchHeaderFlag)
    {
        result = EAS_SearchFile(pEASData, fileHandle, smfHeader, sizeof(smfHeader), &offset);
        if (result != EAS_SUCCESS)
            return (result == EAS_EOF) ? EAS_SUCCESS : result;
    }

    /* read the first 4 bytes of the file - quick method */
    else
#endif
    {
        EAS_U8 header[4];
        EAS_I32 count;
        if ((result = EAS_HWReadFile(pEASData->hwInstData, fileHandle, header, sizeof(header), &count)) != EAS_SUCCESS)
            return result;

        /* check for 'MThd' - If no match then return SUCCESS with NULL handle
         * to indicate not an SMF file. */
        if ((header[0] != 'M') || (header[1] != 'T') || (header[2] != 'h') || (header[3] != 'd'))
            return EAS_SUCCESS;
    }

    /* check for static memory allocation */
    if (pEASData->staticMemoryModel)
        pSMFData = EAS_CMEnumData(EAS_CM_SMF_DATA);
    else
    {
        pSMFData = EAS_HWMalloc(pEASData->hwInstData, sizeof(S_SMF_DATA));
        EAS_HWMemSet((void *)pSMFData,0, sizeof(S_SMF_DATA));
    }
    if (!pSMFData)
        return EAS_ERROR_MALLOC_FAILED;

    /* initialize some critical data */
    pSMFData->fileHandle = fileHandle;
    pSMFData->fileOffset = offset;
    pSMFData->pSynth = NULL;
    pSMFData->time = 0;
    pSMFData->state = EAS_STATE_OPEN;
    *ppHandle = pSMFData;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * SMF_Prepare()
 *----------------------------------------------------------------------------
 * Purpose:
 * Prepare to parse the file. Allocates instance data (or uses static allocation for
 * static memory model).
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT SMF_Prepare (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_SMF_DATA* pSMFData;
    EAS_RESULT result;

    /* check for valid state */
    pSMFData = (S_SMF_DATA *) pInstData;
    if (pSMFData->state != EAS_STATE_OPEN)
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;

    /* instantiate a synthesizer */
    if ((result = VMInitMIDI(pEASData, &pSMFData->pSynth)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "VMInitMIDI returned %d\n", result); */ }
        return result;
    }

    /* parse the file header and setup the individual stream parsers */
    if ((result = SMF_ParseHeader(pEASData->hwInstData, pSMFData)) != EAS_SUCCESS)
        return result;

    /* ready to play */
    pSMFData->state = EAS_STATE_READY;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * SMF_Time()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the time of the next event in msecs
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 * pTime            - pointer to variable to hold time of next event (in msecs)
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
EAS_RESULT SMF_Time (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_U32 *pTime)
{
    S_SMF_DATA *pSMFData;

    pSMFData = (S_SMF_DATA*) pInstData;

    /* sanity check */
#ifdef _CHECKED_BUILD
    if (pSMFData->state == EAS_STATE_STOPPED)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Can't ask for time on a stopped stream\n"); */ }
    }

    if (pSMFData->nextStream == NULL)
    {
        { /* dpp: EAS_ReportEx( _EAS_SEVERITY_ERROR, "no is NULL\n"); */ }
    }
#endif

#if 0
    /* return time in milliseconds */
    /* if chase mode, lie about time */
    if (pSMFData->flags & SMF_FLAGS_CHASE_MODE)
        *pTime = 0;

    else
#endif

        /*lint -e{704} use shift instead of division */
        *pTime = pSMFData->time >> 8;

    *pTime = pSMFData->time >> 8;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * SMF_Event()
 *----------------------------------------------------------------------------
 * Purpose:
 * Parse the next event in the file
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT SMF_Event (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_INT parserMode)
{
    S_SMF_DATA* pSMFData;
    EAS_RESULT result;
    EAS_I32 i;
    EAS_U32 ticks;
    EAS_U32 temp;

    /* establish pointer to instance data */
    pSMFData = (S_SMF_DATA*) pInstData;
    if (pSMFData->state >= EAS_STATE_OPEN)
        return EAS_SUCCESS;

    if (!pSMFData->nextStream) {
        return EAS_ERROR_FILE_FORMAT;
    }


    /* get current ticks */
    ticks = pSMFData->nextStream->ticks;

    /* assume that an error occurred */
    pSMFData->state = EAS_STATE_ERROR;

#ifdef JET_INTERFACE
    /* if JET has track muted, set parser mode to mute */
    if (pSMFData->nextStream->midiStream.jetData & MIDI_FLAGS_JET_MUTE)
        parserMode = eParserModeMute;
#endif

    /* parse the next event from all the streams */
    if ((result = SMF_ParseEvent(pEASData, pSMFData, pSMFData->nextStream, parserMode)) != EAS_SUCCESS)
    {
        /* check for unexpected end-of-file */
        if (result != EAS_EOF)
            return result;

        /* indicate end of track for this stream */
        pSMFData->nextStream->ticks = SMF_END_OF_TRACK;
    }

    /* get next delta time, unless already at end of track */
    else if (pSMFData->nextStream->ticks != SMF_END_OF_TRACK)
    {
        if ((result = SMF_GetDeltaTime(pEASData->hwInstData, pSMFData->nextStream)) != EAS_SUCCESS)
        {
            /* check for unexpected end-of-file */
            if (result != EAS_EOF)
                return result;

            /* indicate end of track for this stream */
            pSMFData->nextStream->ticks = SMF_END_OF_TRACK;
        }

        /* if zero delta to next event, stay with this stream */
        else if (pSMFData->nextStream->ticks == ticks)
        {
            pSMFData->state = EAS_STATE_PLAY;
            return EAS_SUCCESS;
        }
    }

    /* find next event in all streams */
    temp = 0x7ffffff;
    pSMFData->nextStream = NULL;
    for (i = 0; i < pSMFData->numStreams; i++)
    {
        if (pSMFData->streams[i].ticks < temp)
        {
            temp = pSMFData->streams[i].ticks;
            pSMFData->nextStream = &pSMFData->streams[i];
        }
    }

    /* are there any more events to parse? */
    if (pSMFData->nextStream)
    {
        pSMFData->state = EAS_STATE_PLAY;

        /* update the time of the next event */
        SMF_UpdateTime(pSMFData, pSMFData->nextStream->ticks - ticks);
    }
    else
    {
        pSMFData->state = EAS_STATE_STOPPING;
        VMReleaseAllVoices(pEASData->pVoiceMgr, pSMFData->pSynth);
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * SMF_State()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the current state of the stream
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 * pState           - pointer to variable to store state
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
EAS_RESULT SMF_State (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 *pState)
{
    S_SMF_DATA* pSMFData;

    /* establish pointer to instance data */
    pSMFData = (S_SMF_DATA*) pInstData;

    /* if stopping, check to see if synth voices are active */
    if (pSMFData->state == EAS_STATE_STOPPING)
    {
        if (VMActiveVoices(pSMFData->pSynth) == 0)
            pSMFData->state = EAS_STATE_STOPPED;
    }

    if (pSMFData->state == EAS_STATE_PAUSING)
    {
        if (VMActiveVoices(pSMFData->pSynth) == 0)
            pSMFData->state = EAS_STATE_PAUSED;
    }

    /* return current state */
    *pState = pSMFData->state;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * SMF_Close()
 *----------------------------------------------------------------------------
 * Purpose:
 * Close the file and clean up
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT SMF_Close (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_SMF_DATA* pSMFData;
    EAS_I32 i;
    EAS_RESULT result;

    pSMFData = (S_SMF_DATA*) pInstData;

    /* close all the streams */
    for (i = 0; i < pSMFData->numStreams; i++)
    {
        if (pSMFData->streams[i].fileHandle != NULL)
        {
            if ((result = EAS_HWCloseFile(pEASData->hwInstData, pSMFData->streams[i].fileHandle)) != EAS_SUCCESS)
                return result;
        }
    }
    if (pSMFData->fileHandle != NULL)
        if ((result = EAS_HWCloseFile(pEASData->hwInstData, pSMFData->fileHandle)) != EAS_SUCCESS)
            return result;

    /* free the synth */
    if (pSMFData->pSynth != NULL)
        VMMIDIShutdown(pEASData, pSMFData->pSynth);

    /* if using dynamic memory, free it */
    if (!pEASData->staticMemoryModel)
    {
        if (pSMFData->streams)
            EAS_HWFree(pEASData->hwInstData, pSMFData->streams);

        /* free the instance data */
        EAS_HWFree(pEASData->hwInstData, pSMFData);
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * SMF_Reset()
 *----------------------------------------------------------------------------
 * Purpose:
 * Reset the sequencer. Used for locating backwards in the file.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT SMF_Reset (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_SMF_DATA* pSMFData;
    EAS_I32 i;
    EAS_RESULT result;
    EAS_U32 ticks;

    pSMFData = (S_SMF_DATA*) pInstData;

    /* reset time to zero */
    pSMFData->time = 0;

    /* reset the synth */
    VMReset(pEASData->pVoiceMgr, pSMFData->pSynth, EAS_TRUE);

    /* find the start of each track */
    ticks = 0x7fffffffL;
    pSMFData->nextStream = NULL;
    for (i = 0; i < pSMFData->numStreams; i++)
    {

        /* reset file position to first byte of data in track */
        if ((result = EAS_HWFileSeek(pEASData->hwInstData, pSMFData->streams[i].fileHandle, pSMFData->streams[i].startFilePos)) != EAS_SUCCESS)
            return result;

        /* initalize some data */
        pSMFData->streams[i].ticks = 0;

        /* initalize the MIDI parser data */
        EAS_InitMIDIStream(&pSMFData->streams[i].midiStream);

        /* parse the first delta time in each stream */
        if ((result = SMF_GetDeltaTime(pEASData->hwInstData,&pSMFData->streams[i])) != EAS_SUCCESS)
            return result;
        if (pSMFData->streams[i].ticks < ticks)
        {
            ticks = pSMFData->streams[i].ticks;
            pSMFData->nextStream = &pSMFData->streams[i];
        }
    }


    pSMFData->state = EAS_STATE_READY;
    return EAS_SUCCESS;
}

#ifdef JET_INTERFACE
/*----------------------------------------------------------------------------
 * SMF_Pause()
 *----------------------------------------------------------------------------
 * Purpose:
 * Pauses the sequencer. Mutes all voices and sets state to pause.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT SMF_Pause (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_SMF_DATA *pSMFData;

    /* can't pause a stopped stream */
    pSMFData = (S_SMF_DATA*) pInstData;
    if (pSMFData->state == EAS_STATE_STOPPED)
        return EAS_ERROR_ALREADY_STOPPED;

    /* mute the synthesizer */
    VMMuteAllVoices(pEASData->pVoiceMgr, pSMFData->pSynth);
    pSMFData->state = EAS_STATE_PAUSING;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * SMF_Resume()
 *----------------------------------------------------------------------------
 * Purpose:
 * Resume playing after a pause, sets state back to playing.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
EAS_RESULT SMF_Resume (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_SMF_DATA *pSMFData;

    /* can't resume a stopped stream */
    pSMFData = (S_SMF_DATA*) pInstData;
    if (pSMFData->state == EAS_STATE_STOPPED)
        return EAS_ERROR_ALREADY_STOPPED;

    /* nothing to do but resume playback */
    pSMFData->state = EAS_STATE_PLAY;
    return EAS_SUCCESS;
}
#endif

/*----------------------------------------------------------------------------
 * SMF_SetData()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets parser parameters
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
EAS_RESULT SMF_SetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value)
{
    S_SMF_DATA *pSMFData;

    pSMFData = (S_SMF_DATA*) pInstData;
    switch (param)
    {

        /* set metadata callback */
        case PARSER_DATA_METADATA_CB:
            EAS_HWMemCpy(&pSMFData->metadata, (void*) value, sizeof(S_METADATA_CB));
            break;

#ifdef JET_INTERFACE
        /* set jet segment and track ID of all tracks for callback function */
        case PARSER_DATA_JET_CB:
            {
                EAS_U32 i;
                EAS_U32 bit = (EAS_U32) value;
                bit = (bit << JET_EVENT_SEG_SHIFT) & JET_EVENT_SEG_MASK;
                for (i = 0; i < pSMFData->numStreams; i++)
                    pSMFData->streams[i].midiStream.jetData =
                        (pSMFData->streams[i].midiStream.jetData &
                        ~(JET_EVENT_TRACK_MASK | JET_EVENT_SEG_MASK)) |
                        i << JET_EVENT_TRACK_SHIFT | bit | MIDI_FLAGS_JET_CB;
                pSMFData->flags |= SMF_FLAGS_JET_STREAM;
            }
            break;

        /* set state of all mute flags at once */
        case PARSER_DATA_MUTE_FLAGS:
            {
                EAS_INT i;
                EAS_U32 bit = (EAS_U32) value;
                for (i = 0; i < pSMFData->numStreams; i++)
                {
                    if (bit & 1)
                        pSMFData->streams[i].midiStream.jetData |= MIDI_FLAGS_JET_MUTE;
                    else
                        pSMFData->streams[i].midiStream.jetData &= ~MIDI_FLAGS_JET_MUTE;
                    bit >>= 1;
                }
            }
            break;

        /* set track mute */
        case PARSER_DATA_SET_MUTE:
            if (value < pSMFData->numStreams)
                pSMFData->streams[value].midiStream.jetData |= MIDI_FLAGS_JET_MUTE;
            else
                return EAS_ERROR_PARAMETER_RANGE;
            break;

        /* clear track mute */
        case PARSER_DATA_CLEAR_MUTE:
            if (value < pSMFData->numStreams)
                pSMFData->streams[value].midiStream.jetData &= ~MIDI_FLAGS_JET_MUTE;
            else
                return EAS_ERROR_PARAMETER_RANGE;
            break;
#endif

        default:
            return EAS_ERROR_INVALID_PARAMETER;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * SMF_GetData()
 *----------------------------------------------------------------------------
 * Purpose:
 * Retrieves parser parameters
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
EAS_RESULT SMF_GetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue)
{
    S_SMF_DATA *pSMFData;

    pSMFData = (S_SMF_DATA*) pInstData;
    switch (param)
    {
        /* return file type */
        case PARSER_DATA_FILE_TYPE:
            if (pSMFData->numStreams == 1)
                *pValue = EAS_FILE_SMF0;
            else
                *pValue = EAS_FILE_SMF1;
            break;

/* now handled in eas_public.c */
#if 0
        case PARSER_DATA_POLYPHONY:
            if (pSMFData->pSynth)
                VMGetPolyphony(pEASData->pVoiceMgr, pSMFData->pSynth, pValue);
            else
                return EAS_ERROR_NOT_VALID_IN_THIS_STATE;
            break;

        case PARSER_DATA_PRIORITY:
            if (pSMFData->pSynth)
                VMGetPriority(pEASData->pVoiceMgr, pSMFData->pSynth, pValue);
            break;

        /* set transposition */
        case PARSER_DATA_TRANSPOSITION:
            *pValue = pSMFData->transposition;
            break;
#endif

        case PARSER_DATA_SYNTH_HANDLE:
            *pValue = (EAS_I32) pSMFData->pSynth;
            break;

        default:
            return EAS_ERROR_INVALID_PARAMETER;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * SMF_GetVarLenData()
 *----------------------------------------------------------------------------
 * Purpose:
 * Reads a varible length quantity from an SMF file
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
static EAS_RESULT SMF_GetVarLenData (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE fileHandle, EAS_U32 *pData)
{
    EAS_RESULT result;
    EAS_U32 data;
    EAS_U8 c;

    /* read until bit 7 is zero */
    data = 0;
    do
    {
        if ((result = EAS_HWGetByte(hwInstData, fileHandle,&c)) != EAS_SUCCESS)
            return result;
        data = (data << 7) | (c & 0x7f);
    } while (c & 0x80);
    *pData = data;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * SMF_GetDeltaTime()
 *----------------------------------------------------------------------------
 * Purpose:
 * Reads a varible length quantity from an SMF file
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
static EAS_RESULT SMF_GetDeltaTime (EAS_HW_DATA_HANDLE hwInstData, S_SMF_STREAM *pSMFStream)
{
    EAS_RESULT result;
    EAS_U32 ticks;

    if ((result = SMF_GetVarLenData(hwInstData, pSMFStream->fileHandle, &ticks)) != EAS_SUCCESS)
        return result;

    /* number of ticks must not exceed 32-bits */
    if (ticks > (UINT32_MAX - pSMFStream->ticks))
        return EAS_ERROR_FILE_FORMAT;

    pSMFStream->ticks += ticks;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * SMF_ParseMetaEvent()
 *----------------------------------------------------------------------------
 * Purpose:
 * Reads a varible length quantity from an SMF file
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
static EAS_RESULT SMF_ParseMetaEvent (S_EAS_DATA *pEASData, S_SMF_DATA *pSMFData, S_SMF_STREAM *pSMFStream)
{
    EAS_RESULT result;
    EAS_U32 len;
    EAS_I32 pos;
    EAS_U32 temp;
    EAS_U8 c;

    /* get the meta-event type */
    if ((result = EAS_HWGetByte(pEASData->hwInstData, pSMFStream->fileHandle, &c)) != EAS_SUCCESS)
        return result;

    /* get the length */
    if ((result = SMF_GetVarLenData(pEASData->hwInstData, pSMFStream->fileHandle, &len)) != EAS_SUCCESS)
        return result;

    /* get the current file position so we can skip the event */
    if ((result = EAS_HWFilePos(pEASData->hwInstData, pSMFStream->fileHandle, &pos)) != EAS_SUCCESS)
        return result;

    /* prevent a large unsigned length from being treated as a negative length */
    if ((EAS_I32) len < 0) {
        /* note that EAS_I32 is a long, which can be 64-bits on some computers */
        ALOGE("%s() negative len = %ld", __func__, (long) len);
        android_errorWriteLog(0x534e4554, "68953854");
        return EAS_ERROR_FILE_FORMAT;
    }
    /* prevent numeric overflow caused by a very large len, assume pos > 0 */
    const EAS_I32 EAS_I32_MAX = 0x7FFFFFFF;
    if ((EAS_I32) len > (EAS_I32_MAX - pos)) {
        ALOGE("%s() too large len = %ld", __func__, (long) len);
        android_errorWriteLog(0x534e4554, "68953854");
        return EAS_ERROR_FILE_FORMAT;
    }

    pos += (EAS_I32) len;

    /* end of track? */
    if (c == SMF_META_END_OF_TRACK)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Meta-event: end of track\n", c, len); */ }
        pSMFStream->ticks = SMF_END_OF_TRACK;
    }

    /* tempo event? */
    else if (c == SMF_META_TEMPO)
    {
        /* read the 3-byte timebase value */
        temp = 0;
        while (len)
        {
            len--;
            if ((result = EAS_HWGetByte(pEASData->hwInstData, pSMFStream->fileHandle, &c)) != EAS_SUCCESS)
                return result;
            temp = (temp << 8) | c;
        }
        {
            // pSMFData->tickConv = (EAS_U16) (((temp * 1024) / pSMFData->ppqn + 500) / 1000);
            uint64_t temp64;
            if (__builtin_mul_overflow(temp, 1024u, &temp64) ||
                    pSMFData->ppqn == 0 ||
                    (temp64 /= pSMFData->ppqn, false) ||
                    __builtin_add_overflow(temp64, 500, &temp64) ||
                    (temp64 /= 1000, false) ||
                    temp64 > 65535) {
                pSMFData->tickConv = 65535;
            } else {
                pSMFData->tickConv = (EAS_U16) temp64;
            }
        }
        pSMFData->flags |= SMF_FLAGS_HAS_TEMPO;
    }

    /* check for time signature - see iMelody spec V1.4 section 4.1.2.2.3.6 */
    else if (c == SMF_META_TIME_SIGNATURE)
    {
        pSMFData->flags |= SMF_FLAGS_HAS_TIME_SIG;
    }

    /* if the host has registered a metadata callback return the metadata */
    else if (pSMFData->metadata.callback)
    {
        EAS_I32 readLen;
        E_EAS_METADATA_TYPE metaType;

        metaType = EAS_METADATA_UNKNOWN;

        /* only process title on the first track */
        if (c == SMF_META_SEQTRK_NAME)
            metaType = EAS_METADATA_TITLE;
        else if (c == SMF_META_TEXT)
            metaType = EAS_METADATA_TEXT;
        else if (c == SMF_META_COPYRIGHT)
            metaType = EAS_METADATA_COPYRIGHT;
        else if (c == SMF_META_LYRIC)
            metaType = EAS_METADATA_LYRIC;

        if (metaType != EAS_METADATA_UNKNOWN)
        {
            readLen = pSMFData->metadata.bufferSize - 1;
            if ((EAS_I32) len < readLen)
                readLen = (EAS_I32) len;
            if ((result = EAS_HWReadFile(pEASData->hwInstData, pSMFStream->fileHandle, pSMFData->metadata.buffer, readLen, &readLen)) != EAS_SUCCESS)
                return result;
            pSMFData->metadata.buffer[readLen] = 0;
            pSMFData->metadata.callback(metaType, pSMFData->metadata.buffer, pSMFData->metadata.pUserData);
        }
    }

    /* position file to next event - in case we ignored all or part of the meta-event */
    if ((result = EAS_HWFileSeek(pEASData->hwInstData, pSMFStream->fileHandle, pos)) != EAS_SUCCESS)
        return result;

    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Meta-event: type=%02x, len=%d\n", c, len); */ }
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * SMF_ParseSysEx()
 *----------------------------------------------------------------------------
 * Purpose:
 * Reads a varible length quantity from an SMF file
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
static EAS_RESULT SMF_ParseSysEx (S_EAS_DATA *pEASData, S_SMF_DATA *pSMFData, S_SMF_STREAM *pSMFStream, EAS_U8 f0, EAS_INT parserMode)
{
    EAS_RESULT result;
    EAS_U32 len;
    EAS_U8 c;

    /* get the length */
    if ((result = SMF_GetVarLenData(pEASData->hwInstData, pSMFStream->fileHandle, &len)) != EAS_SUCCESS)
        return result;

    /* start of SysEx message? */
    if (f0 == 0xf0)
    {
        if ((result = EAS_ParseMIDIStream(pEASData, pSMFData->pSynth, &pSMFStream->midiStream, f0, parserMode)) != EAS_SUCCESS)
            return result;
    }

    /* feed the SysEx to the stream parser */
    while (len)
    {
        len--;
        if ((result = EAS_HWGetByte(pEASData->hwInstData, pSMFStream->fileHandle, &c)) != EAS_SUCCESS)
            return result;
        if ((result = EAS_ParseMIDIStream(pEASData, pSMFData->pSynth, &pSMFStream->midiStream, c, parserMode)) != EAS_SUCCESS)
            return result;

        /* check for GM system ON */
        if (pSMFStream->midiStream.flags & MIDI_FLAG_GM_ON)
            pSMFData->flags |= SMF_FLAGS_HAS_GM_ON;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * SMF_ParseEvent()
 *----------------------------------------------------------------------------
 * Purpose:
 * Reads a varible length quantity from an SMF file
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
static EAS_RESULT SMF_ParseEvent (S_EAS_DATA *pEASData, S_SMF_DATA *pSMFData, S_SMF_STREAM *pSMFStream, EAS_INT parserMode)
{
    EAS_RESULT result;
    EAS_U8 c;

    /* get the event type */
    if ((result = EAS_HWGetByte(pEASData->hwInstData, pSMFStream->fileHandle, &c)) != EAS_SUCCESS)
        return result;

    /* parse meta-event */
    if (c == 0xff)
    {
        if ((result = SMF_ParseMetaEvent(pEASData, pSMFData, pSMFStream)) != EAS_SUCCESS)
            return result;
    }

    /* parse SysEx */
    else if ((c == 0xf0) || (c == 0xf7))
    {
        if ((result = SMF_ParseSysEx(pEASData, pSMFData, pSMFStream, c, parserMode)) != EAS_SUCCESS)
            return result;
    }

    /* parse MIDI message */
    else
    {
        if ((result = EAS_ParseMIDIStream(pEASData, pSMFData->pSynth, &pSMFStream->midiStream, c, parserMode)) != EAS_SUCCESS)
            return result;

        /* keep streaming data to the MIDI parser until the message is complete */
        while (pSMFStream->midiStream.pending)
        {
            if ((result = EAS_HWGetByte(pEASData->hwInstData, pSMFStream->fileHandle, &c)) != EAS_SUCCESS)
                return result;
            if ((result = EAS_ParseMIDIStream(pEASData, pSMFData->pSynth, &pSMFStream->midiStream, c, parserMode)) != EAS_SUCCESS)
                return result;
        }

    }

    /* chase mode logic */
    if (pSMFData->time == 0)
    {
        if (pSMFData->flags & SMF_FLAGS_CHASE_MODE)
        {
            if (pSMFStream->midiStream.flags & MIDI_FLAG_FIRST_NOTE)
                pSMFData->flags &= ~SMF_FLAGS_CHASE_MODE;
        }
        else if ((pSMFData->flags & SMF_FLAGS_SETUP_BAR) == SMF_FLAGS_SETUP_BAR)
            pSMFData->flags = (pSMFData->flags & ~SMF_FLAGS_SETUP_BAR) | SMF_FLAGS_CHASE_MODE;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * SMF_ParseHeader()
 *----------------------------------------------------------------------------
 * Purpose:
 * Parses the header of an SMF file, allocates memory the stream parsers and initializes the
 * stream parsers.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * pSMFData         - pointer to parser instance data
 * fileHandle       - file handle
 * fileOffset       - offset in the file where the header data starts, usually 0
 *
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{801} we know that 'goto' is deprecated - but it's cleaner in this case */
EAS_RESULT SMF_ParseHeader (EAS_HW_DATA_HANDLE hwInstData, S_SMF_DATA *pSMFData)
{
    EAS_RESULT result;
    EAS_I32 i;
    EAS_U16 division;
    EAS_U16 numStreams;
    EAS_U32 chunkSize;
    EAS_U32 chunkStart;
    EAS_U32 temp;
    EAS_U32 ticks;

    /* explicitly set numStreams to 0. It will later be used by SMF_Close to
     * determine whether we have valid streams or not. */
    pSMFData->numStreams = 0;

    /* rewind the file and find the end of the header chunk */
    if ((result = EAS_HWFileSeek(hwInstData, pSMFData->fileHandle, pSMFData->fileOffset + SMF_OFS_HEADER_SIZE)) != EAS_SUCCESS)
        goto ReadError;
    if ((result = EAS_HWGetDWord(hwInstData, pSMFData->fileHandle, &chunkSize, EAS_TRUE)) != EAS_SUCCESS)
        goto ReadError;

    /* determine the number of tracks */
    if ((result = EAS_HWFileSeek(hwInstData, pSMFData->fileHandle, pSMFData->fileOffset + SMF_OFS_NUM_TRACKS)) != EAS_SUCCESS)
        goto ReadError;
    if ((result = EAS_HWGetWord(hwInstData, pSMFData->fileHandle, &numStreams, EAS_TRUE)) != EAS_SUCCESS)
        goto ReadError;

    /* limit the number of tracks */
    if (numStreams > MAX_SMF_STREAMS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "SMF file contains %u tracks, playing %d tracks\n", numStreams, MAX_SMF_STREAMS); */ }
        numStreams = MAX_SMF_STREAMS;
    } else if (numStreams == 0)
    {
        /* avoid 0 sized allocation */
        return EAS_ERROR_PARAMETER_RANGE;
    }

    /* get the time division */
    if ((result = EAS_HWGetWord(hwInstData, pSMFData->fileHandle, &division, EAS_TRUE)) != EAS_SUCCESS)
        goto ReadError;

    /* setup default timebase for 120 bpm */
    pSMFData->ppqn = 192;
    if (!division || division & 0x8000)
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING,"No support for SMPTE code timebase\n"); */ }
    else
        pSMFData->ppqn = (division & 0x7fff);
    pSMFData->tickConv = (EAS_U16) (((SMF_DEFAULT_TIMEBASE * 1024) / pSMFData->ppqn + 500) / 1000);

    /* dynamic memory allocation, allocate memory for streams */
    if (pSMFData->streams == NULL)
    {
        pSMFData->streams = EAS_HWMalloc(hwInstData,sizeof(S_SMF_STREAM) * numStreams);
        if (pSMFData->streams == NULL)
            return EAS_ERROR_MALLOC_FAILED;

        /* zero the memory to insure complete initialization */
        EAS_HWMemSet((void *)(pSMFData->streams), 0, sizeof(S_SMF_STREAM) * numStreams);
    }
    pSMFData->numStreams = numStreams;

    /* find the start of each track */
    chunkStart = (EAS_U32) pSMFData->fileOffset;
    ticks = 0x7fffffffL;
    pSMFData->nextStream = NULL;
    for (i = 0; i < pSMFData->numStreams; i++)
    {

        for (;;)
        {

            /* calculate start of next chunk - checking for errors */
            temp = chunkStart + SMF_CHUNK_INFO_SIZE + chunkSize;
            if (temp <= chunkStart)
            {
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING,"Error in chunk size at offset %d\n", chunkStart); */ }
                return EAS_ERROR_FILE_FORMAT;
            }
            chunkStart = temp;

            /* seek to the start of the next chunk */
            if ((result = EAS_HWFileSeek(hwInstData, pSMFData->fileHandle, (EAS_I32) chunkStart)) != EAS_SUCCESS)
                goto ReadError;

            /* read the chunk identifier */
            if ((result = EAS_HWGetDWord(hwInstData, pSMFData->fileHandle, &temp, EAS_TRUE)) != EAS_SUCCESS)
                goto ReadError;

            /* read the chunk size */
            if ((result = EAS_HWGetDWord(hwInstData, pSMFData->fileHandle, &chunkSize, EAS_TRUE)) != EAS_SUCCESS)
                goto ReadError;

            /* make sure this is an 'MTrk' chunk */
            if (temp == SMF_CHUNK_TYPE_TRACK)
                break;

            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING,"Unexpected chunk type: 0x%08x\n", temp); */ }
        }

        /* initalize some data */
        pSMFData->streams[i].ticks = 0;
        pSMFData->streams[i].fileHandle = pSMFData->fileHandle;

        /* NULL the file handle so we don't try to close it twice */
        pSMFData->fileHandle = NULL;

        /* save this file position as the start of the track */
        pSMFData->streams[i].startFilePos = (EAS_I32) chunkStart + SMF_CHUNK_INFO_SIZE;

        /* initalize the MIDI parser data */
        EAS_InitMIDIStream(&pSMFData->streams[i].midiStream);

        /* parse the first delta time in each stream */
        if ((result = SMF_GetDeltaTime(hwInstData, &pSMFData->streams[i])) != EAS_SUCCESS)
                goto ReadError;

        if (pSMFData->streams[i].ticks < ticks)
        {
            ticks = pSMFData->streams[i].ticks;
            pSMFData->nextStream = &pSMFData->streams[i];
        }

        /* more tracks to do, create a duplicate file handle */
        if (i < (pSMFData->numStreams - 1))
        {
            if ((result = EAS_HWDupHandle(hwInstData, pSMFData->streams[i].fileHandle, &pSMFData->fileHandle)) != EAS_SUCCESS)
                goto ReadError;
        }
    }

    /* update the time of the next event */
    if (pSMFData->nextStream)
        SMF_UpdateTime(pSMFData, pSMFData->nextStream->ticks);

    return EAS_SUCCESS;

    /* ugly goto: but simpler than structured */
    ReadError:
        if (result == EAS_EOF)
            return EAS_ERROR_FILE_FORMAT;
        return result;
}

/*----------------------------------------------------------------------------
 * SMF_UpdateTime()
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the millisecond time base by converting the ticks into millieconds
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
static void SMF_UpdateTime (S_SMF_DATA *pSMFData, EAS_U32 ticks)
{
    EAS_U32 temp1, temp2;

    if (pSMFData->flags & SMF_FLAGS_CHASE_MODE)
        return;

    temp1 = (ticks >> 10) * pSMFData->tickConv;
    temp2 = (ticks & 0x3ff) * pSMFData->tickConv;
    pSMFData->time += (EAS_I32)((temp1 << 8) + (temp2 >> 2));
}

