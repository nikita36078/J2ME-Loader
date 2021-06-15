/*----------------------------------------------------------------------------
 *
 * File:
 * eas_tonecontrol.c
 *
 * Contents and purpose:
 * MMAPI ToneControl parser
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
 *   $Revision: 795 $
 *   $Date: 2007-08-01 00:14:45 -0700 (Wed, 01 Aug 2007) $
 *----------------------------------------------------------------------------
*/

#include "eas_data.h"
#include "eas_miditypes.h"
#include "eas_parser.h"
#include "eas_report.h"
#include "eas_host.h"
#include "eas_midi.h"
#include "eas_config.h"
#include "eas_vm_protos.h"
#include "eas_tcdata.h"


/* default channel and program for TC playback */
#define TC_CHANNEL              0
#define TC_PROGRAM              80
#define TC_VELOCITY             127

#define TC_FIELD_SILENCE        -1
#define TC_FIELD_VERSION        -2
#define TC_FIELD_TEMPO          -3
#define TC_FIELD_RESOLUTION     -4
#define TC_FIELD_BLOCK_START    -5
#define TC_FIELD_BLOCK_END      -6
#define TC_FIELD_PLAY_BLOCK     -7
#define TC_FIELD_SET_VOLUME     -8
#define TC_FIELD_REPEAT         -9
#define TC_FIELD_INVALID        -10

/* convert 0-100 volume to 0-127 velocity using fixed point */
#define TC_VOLUME_CONV          21307064
#define TC_VOLUME_SHIFT         24


/* local prototypes */
static EAS_RESULT TC_CheckFileType (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, EAS_VOID_PTR *ppHandle, EAS_I32 offset);
static EAS_RESULT TC_Prepare (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT TC_Time (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_U32 *pTime);
static EAS_RESULT TC_Event (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_INT parserMode);
static EAS_RESULT TC_State (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_STATE *pState);
static EAS_RESULT TC_Close (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT TC_Reset (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT TC_Pause (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT TC_Resume (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT TC_SetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value);
static EAS_RESULT TC_GetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue);
static EAS_RESULT TC_ParseHeader (S_EAS_DATA *pEASData, S_TC_DATA* pData);
static EAS_RESULT TC_StartNote (S_EAS_DATA *pEASData, S_TC_DATA* pData, EAS_INT parserMode, EAS_I8 note);
static EAS_RESULT TC_GetRepeat (S_EAS_DATA *pEASData, S_TC_DATA* pData, EAS_INT parserMode);
static EAS_RESULT TC_PlayBlock (S_EAS_DATA *pEASData, S_TC_DATA* pData);
static EAS_RESULT TC_BlockEnd (S_EAS_DATA *pEASData, S_TC_DATA* pData);
static EAS_RESULT TC_GetVolume (S_EAS_DATA *pEASData, S_TC_DATA* pData);
static EAS_RESULT TC_GetTempo (S_EAS_DATA *pEASData, S_TC_DATA* pData);
static EAS_RESULT TC_GetResolution (S_EAS_DATA *pEASData, S_TC_DATA* pData);
static EAS_RESULT TC_GetNextChar (EAS_HW_DATA_HANDLE hwInstData, S_TC_DATA *pData, EAS_I8 *pValue);
static void TC_PutBackChar (S_TC_DATA *pData, EAS_I8 value);

/* calculate a new tick time based on resolution & tempo */
EAS_INLINE void TC_CalcTimeBase (S_TC_DATA *pData)
{

    /* ticks in 256ths of a millisecond */
    pData->tick = ((60 * 1000) << 8) / (pData->tempo * pData->resolution);
}

/*----------------------------------------------------------------------------
 *
 * EAS_TC_Parser
 *
 * This structure contains the functional interface for the iMelody parser
 *----------------------------------------------------------------------------
*/
const S_FILE_PARSER_INTERFACE EAS_TC_Parser =
{
    TC_CheckFileType,
    TC_Prepare,
    TC_Time,
    TC_Event,
    TC_State,
    TC_Close,
    TC_Reset,
    TC_Pause,
    TC_Resume,
    NULL,
    TC_SetData,
    TC_GetData,
    NULL
};

/*----------------------------------------------------------------------------
 * TC_CheckFileType()
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
static EAS_RESULT TC_CheckFileType (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, EAS_VOID_PTR *ppHandle, EAS_I32 offset)
{
    S_TC_DATA data;
    S_TC_DATA *pData;

    /* init data */
    EAS_HWMemSet(&data, 0, sizeof(S_TC_DATA));
    data.fileHandle = fileHandle;
    data.fileOffset = offset;
    *ppHandle= NULL;

    /* see if we can parse the header */
    if (TC_ParseHeader(pEASData, &data) == EAS_SUCCESS)
    {

        /* check for static memory allocation */
        if (pEASData->staticMemoryModel)
            pData = EAS_CMEnumOptData(EAS_MODULE_MMAPI_TONE_CONTROL);
        else
            pData = EAS_HWMalloc(pEASData->hwInstData, sizeof(S_TC_DATA));
        if (!pData)
            return EAS_ERROR_MALLOC_FAILED;

        /* copy data to persistent storage */
        EAS_HWMemCpy(pData, &data, sizeof(S_TC_DATA));

        /* return a pointer to the instance data */
        pData->state = EAS_STATE_OPEN;
        *ppHandle = pData;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * TC_Prepare()
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
static EAS_RESULT TC_Prepare (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_TC_DATA* pData;
    EAS_RESULT result;

    /* check for valid state */
    pData = (S_TC_DATA*) pInstData;
    if (pData->state != EAS_STATE_OPEN)
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;

    /* instantiate a synthesizer */
    if ((result = VMInitMIDI(pEASData, &pData->pSynth)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "VMInitMIDI returned %d\n", result); */ }
        return result;
    }

    /* set to ready state */
    pData->state = EAS_STATE_READY;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * TC_Time()
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
static EAS_RESULT TC_Time (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_U32 *pTime)
{
    S_TC_DATA *pData;

    pData = (S_TC_DATA*) pInstData;

    /* return time in milliseconds */
    /*lint -e{704} use shift instead of division */
    *pTime = pData->time >> 8;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * TC_Event()
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
static EAS_RESULT TC_Event (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_INT parserMode)
{
    S_TC_DATA* pData;
    EAS_RESULT result;
    EAS_I8 temp;

    pData = (S_TC_DATA*) pInstData;
    if (pData->state >= EAS_STATE_OPEN)
        return EAS_SUCCESS;

    /* initialize MIDI channel when the track starts playing */
    if (pData->time == 0)
    {
        /* set program to square lead */
        VMProgramChange(pEASData->pVoiceMgr, pData->pSynth, TC_CHANNEL, TC_PROGRAM);

        /* set channel volume to max */
        VMControlChange(pEASData->pVoiceMgr, pData->pSynth, TC_CHANNEL, 7, 127);
    }

    /* check for end of note */
    if (pData->note >= 0)
    {
        /* stop the note */
        VMStopNote(pEASData->pVoiceMgr, pData->pSynth, TC_CHANNEL, (EAS_U8) pData->note, 0);

        /* check for repeat note */
        if (pData->repeatCount)
        {
            pData->repeatCount--;
            pData->time += pData->length;
            if ((pData->note >= 0) && (parserMode == eParserModePlay))
                VMStartNote(pEASData->pVoiceMgr, pData->pSynth, TC_CHANNEL, (EAS_U8) pData->note, pData->volume);
            return EAS_SUCCESS;
        }

        pData->note = TC_FIELD_SILENCE;
    }

    /* parse stream until we get a note or rest */
    for (;;)
    {

        /* get next byte from stream */
        if ((result = TC_GetNextChar(pEASData->hwInstData, pData, &temp)) != EAS_SUCCESS)
        {
            if (result == EAS_EOF)
            {
                pData->state = EAS_STATE_STOPPING;
                return EAS_SUCCESS;
            }
            break;
        }

        /* check for musical events */
        if (temp >= TC_FIELD_SILENCE)
        {
            result = TC_StartNote(pEASData, pData, parserMode, temp);
            break;
        }

        /* must be a control field */
        switch (temp)
        {
            case TC_FIELD_TEMPO:
                result = TC_GetTempo(pEASData, pData);
                break;

            case TC_FIELD_RESOLUTION:
                result = TC_GetResolution(pEASData, pData);
                break;

            case TC_FIELD_SET_VOLUME:
                result = TC_GetVolume(pEASData, pData);
                break;

            case TC_FIELD_REPEAT:
                result = TC_GetRepeat(pEASData, pData, parserMode);
                break;

            case TC_FIELD_PLAY_BLOCK:
                result = TC_PlayBlock(pEASData, pData);
                break;

            case TC_FIELD_BLOCK_START:
                result = TC_GetNextChar(pEASData->hwInstData, pData, &temp);
                break;

            case TC_FIELD_BLOCK_END:
                result = TC_BlockEnd(pEASData, pData);
                break;

            default:
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Unexpected byte 0x%02x in ToneControl stream\n", temp); */ }
                result = EAS_ERROR_FILE_FORMAT;
        }

        /* check for error */
        if (result != EAS_SUCCESS)
            break;
    }

    /* check for error */
    if (result != EAS_SUCCESS)
    {
        if (result == EAS_EOF)
            result = EAS_ERROR_FILE_FORMAT;
        pData->state = EAS_STATE_ERROR;
    }
    else
        pData->state = EAS_STATE_PLAY;
    return result;
}

/*----------------------------------------------------------------------------
 * TC_State()
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
static EAS_RESULT TC_State (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 *pState)
{
    S_TC_DATA* pData;

    /* establish pointer to instance data */
    pData = (S_TC_DATA*) pInstData;

    /* if stopping, check to see if synth voices are active */
    if (pData->state == EAS_STATE_STOPPING)
    {
        if (VMActiveVoices(pData->pSynth) == 0)
            pData->state = EAS_STATE_STOPPED;
    }

    if (pData->state == EAS_STATE_PAUSING)
    {
        if (VMActiveVoices(pData->pSynth) == 0)
            pData->state = EAS_STATE_PAUSED;
    }

    /* return current state */
    *pState = pData->state;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * TC_Close()
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
static EAS_RESULT TC_Close (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_TC_DATA* pData;
    EAS_RESULT result;

    pData = (S_TC_DATA*) pInstData;

    /* close the file */
    if ((result = EAS_HWCloseFile(pEASData->hwInstData, pData->fileHandle)) != EAS_SUCCESS)
            return result;

    /* free the synth */
    if (pData->pSynth != NULL)
        VMMIDIShutdown(pEASData, pData->pSynth);

    /* if using dynamic memory, free it */
    if (!pEASData->staticMemoryModel)
        EAS_HWFree(pEASData->hwInstData, pData);

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * TC_Reset()
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
static EAS_RESULT TC_Reset (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_TC_DATA* pData;
    EAS_RESULT result;

    pData = (S_TC_DATA*) pInstData;

    /* reset the synth */
    VMReset(pEASData->pVoiceMgr, pData->pSynth, EAS_TRUE);

    /* reset time to zero */
    pData->time = 0;

    /* reset file position and re-parse header */
    pData->state = EAS_STATE_ERROR;
    if ((result = EAS_HWFileSeek(pEASData->hwInstData, pData->fileHandle, pData->fileOffset)) != EAS_SUCCESS)
        return result;
    if ((result = TC_ParseHeader (pEASData,  pData)) != EAS_SUCCESS)
        return result;

    pData->state = EAS_STATE_READY;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * TC_Pause()
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
static EAS_RESULT TC_Pause (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_TC_DATA *pData;

    /* can't pause a stopped stream */
    pData = (S_TC_DATA*) pInstData;
    if (pData->state == EAS_STATE_STOPPED)
        return EAS_ERROR_ALREADY_STOPPED;

    /* mute the synthesizer */
    VMMuteAllVoices(pEASData->pVoiceMgr, pData->pSynth);
    pData->state = EAS_STATE_PAUSING;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * TC_Resume()
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
static EAS_RESULT TC_Resume (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_TC_DATA *pData;

    /* can't resume a stopped stream */
    pData = (S_TC_DATA*) pInstData;
    if (pData->state == EAS_STATE_STOPPED)
        return EAS_ERROR_ALREADY_STOPPED;

    /* nothing to do but resume playback */
    pData->state = EAS_STATE_PLAY;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * TC_SetData()
 *----------------------------------------------------------------------------
 * Purpose:
 * Return file type
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
/*lint -esym(715, pEASData, pInstData, value) reserved for future use */
static EAS_RESULT TC_SetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value)
{
    /* we don't parse any metadata, but we need to return success here */
    if (param == PARSER_DATA_METADATA_CB)
        return EAS_SUCCESS;

    return EAS_ERROR_INVALID_PARAMETER;
}

/*----------------------------------------------------------------------------
 * TC_GetData()
 *----------------------------------------------------------------------------
 * Purpose:
 * Return file type
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
/*lint -e{715} common with other parsers */
static EAS_RESULT TC_GetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue)
{
    S_TC_DATA *pData;

    pData = (S_TC_DATA *) pInstData;
    switch (param)
    {
        /* return file type as TC */
        case PARSER_DATA_FILE_TYPE:
            *pValue = EAS_FILE_MMAPI_TONE_CONTROL;
            break;

        case PARSER_DATA_SYNTH_HANDLE:
            *pValue = (EAS_I32) pData->pSynth;
            break;

    default:
            return EAS_ERROR_INVALID_PARAMETER;
    }
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * TC_ParseHeader()
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
static EAS_RESULT TC_ParseHeader (S_EAS_DATA *pEASData, S_TC_DATA* pData)
{
    EAS_RESULT result;
    EAS_I8 temp;

    /* initialize some defaults */
    pData->time = 0;
    pData->tempo = 120;
    pData->resolution = 64;
    pData->volume = 127;
    pData->repeatCount = 0;
    pData->note = TC_FIELD_SILENCE;
    pData->byteAvail = EAS_FALSE;

    /* set default timebase */
    TC_CalcTimeBase(pData);

    /* seek to start of data */
    if ((result = EAS_HWFileSeek(pEASData->hwInstData, pData->fileHandle, pData->fileOffset)) != EAS_SUCCESS)
        return result;

    /* get version */
    if ((result = TC_GetNextChar(pEASData->hwInstData, pData, &temp)) != EAS_SUCCESS)
        return result;

    /* check for version number */
    if (temp == TC_FIELD_VERSION)
    {
        TC_GetNextChar(pEASData->hwInstData, pData, &temp);
//      { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "ToneControl sequence version %d\n", temp); */ }
    }
    else
        return EAS_ERROR_FILE_FORMAT;

    /* parse the header data until we find the first note or block */
    for (;;)
    {

        /* get next byte from stream */
        if ((result = TC_GetNextChar(pEASData->hwInstData, pData, &temp)) != EAS_SUCCESS)
            return result;

        /* check for tempo */
        if (temp == TC_FIELD_TEMPO)
        {
            if ((result = TC_GetTempo(pEASData, pData)) != EAS_SUCCESS)
                return result;
        }

        /* or resolution */
        else if (temp == TC_FIELD_TEMPO)
        {
            if ((result = TC_GetResolution(pEASData, pData)) != EAS_SUCCESS)
                return result;
        }

        /* must be music data */
        else if (temp > TC_FIELD_INVALID)
        {
            TC_PutBackChar(pData, temp);
            return EAS_SUCCESS;
        }

        /* unknown codes */
        else
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Unexpected byte 0x%02x in ToneControl stream\n", temp); */ }
            return EAS_ERROR_FILE_FORMAT;
        }
    }
}

/*----------------------------------------------------------------------------
 * TC_StartNote()
 *----------------------------------------------------------------------------
 * Process a note or silence event
 *----------------------------------------------------------------------------
*/
static EAS_RESULT TC_StartNote (S_EAS_DATA *pEASData, S_TC_DATA* pData, EAS_INT parserMode, EAS_I8 note)
{
    EAS_I8 duration;

    /* get the duration */
    if (TC_GetNextChar(pEASData->hwInstData, pData, &duration) != EAS_SUCCESS)
        return EAS_ERROR_FILE_FORMAT;

    /* calculate time of next event */
    pData->length = (EAS_I32) duration * pData->tick;
    pData->time += pData->length;

    /* start the note */
    if ((note >= 0) && (parserMode == eParserModePlay))
    {
        VMStartNote(pEASData->pVoiceMgr, pData->pSynth, TC_CHANNEL, (EAS_U8) note, pData->volume);
        pData->note = note;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * TC_GetRepeat()
 *----------------------------------------------------------------------------
 * Process a repeat code
 *----------------------------------------------------------------------------
*/
static EAS_RESULT TC_GetRepeat (S_EAS_DATA *pEASData, S_TC_DATA* pData, EAS_INT parserMode)
{
    EAS_I8 count;

    /* get the repeat count */
    if (TC_GetNextChar(pEASData->hwInstData, pData, &count) != EAS_SUCCESS)
        return EAS_ERROR_FILE_FORMAT;

    /* validiate it */
    if (count < 2)
        return EAS_ERROR_FILE_FORMAT;

    /* calculate time of next event */
    pData->time += pData->length;
    pData->repeatCount = count - 2;

    /* start the note */
    if ((pData->note >= 0) && (parserMode == eParserModePlay))
        VMStartNote(pEASData->pVoiceMgr, pData->pSynth, TC_CHANNEL, (EAS_U8) pData->note, pData->volume);

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * TC_PlayBlock()
 *----------------------------------------------------------------------------
 * Play a block of notes
 *----------------------------------------------------------------------------
*/
static EAS_RESULT TC_PlayBlock (S_EAS_DATA *pEASData, S_TC_DATA* pData)
{
    EAS_RESULT result;
    EAS_I8 blockNum;
    EAS_I8 temp;
    EAS_I8 temp2;

    /* get the block number */
    if (TC_GetNextChar(pEASData->hwInstData, pData, &blockNum) != EAS_SUCCESS)
        return EAS_ERROR_FILE_FORMAT;

    /* validiate it */
    if (blockNum < 0)
        return EAS_ERROR_FILE_FORMAT;

    /* save the current position */
    if ((result = EAS_HWFilePos(pEASData->hwInstData, pData->fileHandle, &pData->restorePos)) != EAS_SUCCESS)
        return result;

    /* return to start of file */
    pData->byteAvail = EAS_FALSE;
    if ((result = EAS_HWFileSeek(pEASData->hwInstData, pData->fileHandle, pData->fileOffset)) != EAS_SUCCESS)
        return result;

    /* find the block */
    for (;;)
    {
        if (TC_GetNextChar(pEASData->hwInstData, pData, &temp) != EAS_SUCCESS)
            return EAS_ERROR_FILE_FORMAT;

        if (TC_GetNextChar(pEASData->hwInstData, pData, &temp2) != EAS_SUCCESS)
            return EAS_ERROR_FILE_FORMAT;

        if ((temp == TC_FIELD_BLOCK_START) && (temp2 == blockNum))
            return EAS_SUCCESS;
    }
}

/*----------------------------------------------------------------------------
 * TC_BlockEnd()
 *----------------------------------------------------------------------------
 * Handle end of block
 *----------------------------------------------------------------------------
*/
static EAS_RESULT TC_BlockEnd (S_EAS_DATA *pEASData, S_TC_DATA* pData)
{
    EAS_I8 blockNum;

    /* get the block number */
    if (TC_GetNextChar(pEASData->hwInstData, pData, &blockNum) != EAS_SUCCESS)
        return EAS_ERROR_FILE_FORMAT;

    /* validiate it */
    if (blockNum < 0)
        return EAS_ERROR_FILE_FORMAT;

    /* if we were playing this block, restore to previous position */
    pData->byteAvail = EAS_FALSE;
    return EAS_HWFileSeek(pEASData->hwInstData, pData->fileHandle, pData->restorePos);
}

/*----------------------------------------------------------------------------
 * TC_GetVolume()
 *----------------------------------------------------------------------------
 * Get the volume field and process it
 *----------------------------------------------------------------------------
*/
static EAS_RESULT TC_GetVolume (S_EAS_DATA *pEASData, S_TC_DATA* pData)
{
    EAS_I8 volume;

    /* get volume */
    if (TC_GetNextChar(pEASData->hwInstData, pData, &volume) != EAS_SUCCESS)
        return EAS_ERROR_FILE_FORMAT;
    if ((volume < 0) || (volume > 100))
        return EAS_ERROR_FILE_FORMAT;

    /* save volume */
    pData->volume = (EAS_U8) ((EAS_I32) (volume * TC_VOLUME_CONV + 1) >> TC_VOLUME_SHIFT);
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * TC_GetTempo()
 *----------------------------------------------------------------------------
 * Get the tempo field and process it
 *----------------------------------------------------------------------------
*/
static EAS_RESULT TC_GetTempo (S_EAS_DATA *pEASData, S_TC_DATA* pData)
{
    EAS_I8 tempo;

    /* get tempo */
    if (TC_GetNextChar(pEASData->hwInstData, pData, &tempo) != EAS_SUCCESS)
        return EAS_ERROR_FILE_FORMAT;
    if (tempo < 5)
        return EAS_ERROR_FILE_FORMAT;

    /* save tempo */
    pData->tempo = tempo;

    /* calculate new timebase */
    TC_CalcTimeBase(pData);
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * TC_GetResolution()
 *----------------------------------------------------------------------------
 * Get the resolution field and process it
 *----------------------------------------------------------------------------
*/
static EAS_RESULT TC_GetResolution (S_EAS_DATA *pEASData, S_TC_DATA* pData)
{
    EAS_I8 resolution;

    /* get resolution */
    if (TC_GetNextChar(pEASData->hwInstData, pData, &resolution) != EAS_SUCCESS)
        return EAS_ERROR_FILE_FORMAT;
    if (resolution < 0)
        return EAS_ERROR_FILE_FORMAT;

    /* save tempo */
    pData->resolution = resolution;

    /* calculate new timebase */
    TC_CalcTimeBase(pData);
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * TC_GetNextChar()
 *----------------------------------------------------------------------------
 * Fetch the next character from the stream
 *----------------------------------------------------------------------------
*/
static EAS_RESULT TC_GetNextChar (EAS_HW_DATA_HANDLE hwInstData, S_TC_DATA *pData, EAS_I8 *pValue)
{

    /* get character from "put back" buffer */
    if (pData->byteAvail)
    {
        pData->byteAvail = EAS_FALSE;
        *pValue = pData->dataByte;
        return EAS_SUCCESS;
    }

    /* get character from file */
    return EAS_HWGetByte(hwInstData, pData->fileHandle, pValue);
}

/*----------------------------------------------------------------------------
 * TC_PutBackChar()
 *----------------------------------------------------------------------------
 * Put back the character
 *----------------------------------------------------------------------------
*/
static void TC_PutBackChar (S_TC_DATA *pData, EAS_I8 value)
{

    pData->dataByte = value;
    pData->byteAvail = EAS_TRUE;
}

