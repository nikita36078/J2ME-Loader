/*----------------------------------------------------------------------------
 *
 * File:
 * eas_ota.c
 *
 * Contents and purpose:
 * OTA parser
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
 *   $Revision: 795 $
 *   $Date: 2007-08-01 00:14:45 -0700 (Wed, 01 Aug 2007) $
 *----------------------------------------------------------------------------
*/

#define LOG_TAG "Sonivox"
#include <log/log.h>

#include "eas_data.h"
#include "eas_miditypes.h"
#include "eas_parser.h"
#include "eas_report.h"
#include "eas_host.h"
#include "eas_midi.h"
#include "eas_config.h"
#include "eas_vm_protos.h"
#include "eas_otadata.h"

/* increase gain for mono ringtones */
#define OTA_GAIN_OFFSET             8

/* file definitions */
#define OTA_RINGTONE                0x25
#define OTA_SOUND                   0x1d
#define OTA_UNICODE                 0x22

/* song type definitions */
#define OTA_BASIC_SONG_TYPE         0x01
#define OTA_TEMPORARY_SONG_TYPE     0x02

/* instruction ID coding */
#define OTA_PATTERN_HEADER_ID       0x00
#define OTA_NOTE_INST_ID            0x01
#define OTA_SCALE_INST_ID           0x02
#define OTA_STYLE_INST_ID           0x03
#define OTA_TEMPO_INST_ID           0x04
#define OTA_VOLUME_INST_ID          0x05

/* note durations */
#define OTA_NORMAL_DURATION         0x00
#define OTA_DOTTED_NOTE             0x01
#define OTA_DOUBLE_DOTTED_NOTE      0x02
#define OTA_TRIPLET_NOTE            0x03

/* loop count value for infinite loop */
#define OTA_INFINITE_LOOP           0x0f

/* length of 32nd note in 1/256ths of a msec for 63 BPM tempo */
#define DEFAULT_TICK_CONV           30476

/* default channel and program for OTA playback */
#define OTA_CHANNEL                 0
#define OTA_PROGRAM                 80
#define OTA_VEL_MUL                 4
#define OTA_VEL_OFS                 67
#define OTA_VEL_DEFAULT             95

/* multiplier for fixed point triplet conversion */
#define TRIPLET_MULTIPLIER          683
#define TRIPLET_SHIFT               10

/* local prototypes */
static EAS_RESULT OTA_CheckFileType (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, EAS_VOID_PTR *ppHandle, EAS_I32 offset);
static EAS_RESULT OTA_Prepare (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT OTA_Time (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_U32 *pTime);
static EAS_RESULT OTA_Event (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_INT parserMode);
static EAS_RESULT OTA_State (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_STATE *pState);
static EAS_RESULT OTA_Close (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT OTA_Reset (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT OTA_Pause (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT OTA_Resume (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT OTA_SetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value);
static EAS_RESULT OTA_GetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue);
static EAS_RESULT OTA_ParseHeader (S_EAS_DATA *pEASData, S_OTA_DATA* pData);
static EAS_RESULT OTA_FetchBitField (EAS_HW_DATA_HANDLE hwInstData, S_OTA_DATA *pData, EAS_I32 numBits, EAS_U8 *pValue);
static EAS_RESULT OTA_SavePosition (EAS_HW_DATA_HANDLE hwInstData, S_OTA_DATA *pData, S_OTA_LOC *pLoc);
static EAS_RESULT OTA_RestorePosition (EAS_HW_DATA_HANDLE hwInstData, S_OTA_DATA *pData, S_OTA_LOC *pLoc);


/*----------------------------------------------------------------------------
 *
 * EAS_OTA_Parser
 *
 * This structure contains the functional interface for the OTA parser
 *----------------------------------------------------------------------------
*/
const S_FILE_PARSER_INTERFACE EAS_OTA_Parser =
{
    OTA_CheckFileType,
    OTA_Prepare,
    OTA_Time,
    OTA_Event,
    OTA_State,
    OTA_Close,
    OTA_Reset,
    OTA_Pause,
    OTA_Resume,
    NULL,
    OTA_SetData,
    OTA_GetData,
    NULL
};

/*----------------------------------------------------------------------------
 *
 * bpmTable
 *
 * BPM conversion table. Converts bpm values to 256ths of a millisecond for a 32nd note
 *----------------------------------------------------------------------------
*/
static const EAS_U32 bpmTable[32] =
{
    76800, 68571, 61935, 54857,
    48000, 42667, 38400, 34286,
    30476, 27429, 24000, 21333,
    19200, 17143, 15360, 13714,
    12000, 10667, 9600, 8533,
    7680, 6737, 6000, 5408,
    4800, 4267, 3840, 3398,
    3024, 2685, 2400, 2133
};

/*----------------------------------------------------------------------------
 * OTA_CheckFileType()
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
static EAS_RESULT OTA_CheckFileType (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, EAS_VOID_PTR *ppHandle, EAS_I32 offset)
{
    S_OTA_DATA* pData;
    EAS_RESULT result;
    EAS_INT cmdLen;
    EAS_INT state;
    EAS_U8 temp;

    /* read the first byte, should be command length */
    *ppHandle = NULL;
    if ((result = EAS_HWGetByte(pEASData->hwInstData, fileHandle, &temp)) != EAS_SUCCESS)
        return result;

    /* read all the commands */
    cmdLen = temp;
    state = 0;
    while (cmdLen--)
    {

        /* read the command, upper 7 bits */
        if ((result = EAS_HWGetByte(pEASData->hwInstData, fileHandle, &temp)) != EAS_SUCCESS)
            return result;
        temp = temp >> 1;

        if (state == 0)
        {
            if (temp != OTA_RINGTONE)
                break;
            state++;
        }
        else
        {

            if (temp == OTA_SOUND)
            {

                /* check for static memory allocation */
                if (pEASData->staticMemoryModel)
                    pData = EAS_CMEnumData(EAS_CM_OTA_DATA);
                else
                    pData = EAS_HWMalloc(pEASData->hwInstData, sizeof(S_OTA_DATA));
                if (!pData)
                {
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Malloc failed in OTA_Prepare\n"); */ }
                    return EAS_ERROR_MALLOC_FAILED;
                }
                EAS_HWMemSet(pData, 0, sizeof(S_OTA_DATA));

                /* return a pointer to the instance data */
                pData->fileHandle = fileHandle;
                pData->fileOffset = offset;
                pData->state = EAS_STATE_OPEN;
                *ppHandle = pData;
                ALOGD("%s() OTA file recognized", __func__);
                break;
            }

            if (temp != OTA_UNICODE)
                break;
        }
    }

    /* not recognized */
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * OTA_Prepare()
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
static EAS_RESULT OTA_Prepare (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_OTA_DATA* pData;
    EAS_RESULT result;

    /* check for valid state */
    pData = (S_OTA_DATA*) pInstData;
    if (pData->state != EAS_STATE_OPEN)
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;

    /* instantiate a synthesizer */
    if ((result = VMInitMIDI(pEASData, &pData->pSynth)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "VMInitMIDI returned %d\n", result); */ }
        return result;
    }

    pData->state = EAS_STATE_ERROR;
    if ((result = OTA_ParseHeader(pEASData, pData)) != EAS_SUCCESS)
        return result;

    pData->state = EAS_STATE_READY;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * OTA_Time()
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
/*lint -esym(715, pEASData) common decoder interface - pEASData not used */
static EAS_RESULT OTA_Time (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_U32 *pTime)
{
    S_OTA_DATA *pData;

    pData = (S_OTA_DATA*) pInstData;

    /* return time in milliseconds */
    /*lint -e{704} use shift instead of division */
    *pTime = pData->time >> 8;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * OTA_Event()
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
static EAS_RESULT OTA_Event (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_INT parserMode)
{
    S_OTA_DATA* pData;
    EAS_RESULT result;
    EAS_U32 duration;
    EAS_U8 temp;

    pData = (S_OTA_DATA*) pInstData;
    if (pData->state >= EAS_STATE_OPEN)
        return EAS_SUCCESS;

    /* initialize MIDI channel when the track starts playing */
    if (pData->time == 0)
    {
        /* set program to square lead */
        if (parserMode != eParserModeMetaData)
            VMProgramChange(pEASData->pVoiceMgr, pData->pSynth, OTA_CHANNEL, OTA_PROGRAM);

        /* set channel volume to max */
        if (parserMode != eParserModeMetaData)
            VMControlChange(pEASData->pVoiceMgr, pData->pSynth, OTA_CHANNEL, 7, 127);
    }

    /* check for end of note */
    if (pData->note)
    {
        /* stop the note */
        VMStopNote(pEASData->pVoiceMgr, pData->pSynth, OTA_CHANNEL, pData->note, 0);
        pData->note = 0;

        /* check for rest between notes */
        if (pData->restTicks)
        {
            pData->time += (EAS_I32) pData->restTicks;
            pData->restTicks = 0;
            return EAS_SUCCESS;
        }
    }

    /* if not in a pattern, read the pattern header */
    while (pData->current.patternLen == 0)
    {

        /* check for loop - don't do infinite loops when locating */
        if (pData->loopCount && ((parserMode == eParserModePlay) || (pData->loopCount != OTA_INFINITE_LOOP)))
        {
            ALOGV("%s() loop backwards, loopCount = %d", __func__, pData->loopCount);
            /* if not infinite loop, decrement loop count */
            if (pData->loopCount != OTA_INFINITE_LOOP)
                pData->loopCount--;

            /* back to start of pattern*/
            if ((result = OTA_RestorePosition(pEASData->hwInstData, pData, &pData->patterns[pData->currentPattern])) != EAS_SUCCESS)
                return result;
        }

        /* if no previous position to restore, continue forward */
        else if (pData->restore.fileOffset < 0)
        {

            /* check for end of song */
            if (pData->numPatterns == 0)
            {
                pData->state = EAS_STATE_STOPPING;
                VMReleaseAllVoices(pEASData->pVoiceMgr, pData->pSynth);
                return EAS_SUCCESS;
            }

            /* read the next pattern header */
            if ((result = OTA_FetchBitField(pEASData->hwInstData, pData, 3, &temp)) != EAS_SUCCESS)
                return result;
            if (temp != OTA_PATTERN_HEADER_ID)
            {
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Expected OTA pattern header\n"); */ }
                return EAS_ERROR_FILE_FORMAT;
            }

            /* get the pattern ID */
            if ((result = OTA_FetchBitField(pEASData->hwInstData, pData, 2, &pData->currentPattern)) != EAS_SUCCESS)
                return result;

            /* get the loop count */
            if ((result = OTA_FetchBitField(pEASData->hwInstData, pData, 4, &pData->loopCount)) != EAS_SUCCESS)
                return result;

            /* get the pattern length */
            if ((result = OTA_FetchBitField(pEASData->hwInstData, pData, 8, &pData->current.patternLen)) != EAS_SUCCESS)
                return result;

            /* if pattern definition, save the current position */
            if (pData->current.patternLen)
            {
                if ((result = OTA_SavePosition(pEASData->hwInstData, pData, &pData->patterns[pData->currentPattern])) != EAS_SUCCESS)
                    return result;
            }

            /* if pattern length is zero, repeat a previous pattern */
            else
            {
                /* make sure it's a valid pattern */
                if (pData->patterns[pData->currentPattern].fileOffset < 0)
                {
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "OTA pattern error, invalid pattern specified\n"); */ }
                    return EAS_ERROR_FILE_FORMAT;
                }

                /* save current position and data */
                if ((result = OTA_SavePosition(pEASData->hwInstData, pData, &pData->restore)) != EAS_SUCCESS)
                    return result;

                /* seek to the pattern in the file */
                if ((result = OTA_RestorePosition(pEASData->hwInstData, pData, &pData->patterns[pData->currentPattern])) != EAS_SUCCESS)
                    return result;
            }

            /* decrement pattern count */
            pData->numPatterns--;
        }

        /* restore previous position */
        else
        {
            if ((result = OTA_RestorePosition(pEASData->hwInstData, pData, &pData->restore)) != EAS_SUCCESS)
                return result;
        }
    }

    /* get the next event */
    if ((result = OTA_FetchBitField(pEASData->hwInstData, pData, 3, &temp)) != EAS_SUCCESS)
        return result;

    switch (temp)
    {
        case OTA_NOTE_INST_ID:
            /* fetch note value */
            if ((result = OTA_FetchBitField(pEASData->hwInstData, pData, 4, &pData->note)) != EAS_SUCCESS)
                return result;

            /* fetch note duration */
            if ((result = OTA_FetchBitField(pEASData->hwInstData, pData, 3, &temp)) != EAS_SUCCESS)
                return result;
            duration = pData->tick * (0x20 >> temp);

            /* fetch note duration modifier */
            if ((result = OTA_FetchBitField(pEASData->hwInstData, pData, 2, &temp)) != EAS_SUCCESS)
                return result;
            switch (temp)
            {
                case OTA_NORMAL_DURATION:
                    break;

                case OTA_DOTTED_NOTE:
                    duration += duration >> 1;
                    break;

                case OTA_DOUBLE_DOTTED_NOTE:
                    duration += (duration >> 1) + (duration >> 2);
                    break;

                case OTA_TRIPLET_NOTE:
                    duration = (duration * TRIPLET_MULTIPLIER) >> TRIPLET_SHIFT;
                    break;

                default:
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Unrecognized note duration ignored\n"); */ }
                    break;
            }

            /* check for note */
            if (pData->note)
            {

                /* determine note length based on style */
                switch (pData->style)
                {
                    case 0:
                        pData->restTicks = duration >> 4;
                        break;
                    case 1:
                        pData->restTicks = 0;
                        break;
                    case 2:
                        pData->restTicks = duration >> 1;
                        break;
                    default:
                        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Unrecognized note style ignored\n"); */ }
                }

                /* add octave */
                pData->note += pData->octave;
                if (parserMode == eParserModePlay)
                    VMStartNote(pEASData->pVoiceMgr, pData->pSynth, OTA_CHANNEL, pData->note, pData->velocity);
                pData->time += (EAS_I32) duration - (EAS_I32) pData->restTicks;
            }

            /* this is a rest */
            else
                pData->time += (EAS_I32) duration;
            break;

        case OTA_SCALE_INST_ID:
            /* fetch octave */
            if ((result = OTA_FetchBitField(pEASData->hwInstData, pData, 2, &temp)) != EAS_SUCCESS)
                return result;
            pData->octave = (EAS_U8) (temp * 12 + 59);
            break;

        case OTA_STYLE_INST_ID:
            /* fetch note style */
            if ((result = OTA_FetchBitField(pEASData->hwInstData, pData, 2, &pData->style)) != EAS_SUCCESS)
                return result;
            break;

        case OTA_TEMPO_INST_ID:
            /* fetch tempo */
            if ((result = OTA_FetchBitField(pEASData->hwInstData, pData, 5, &temp)) != EAS_SUCCESS)
                return result;
            pData->tick = bpmTable[temp];
            break;

        case OTA_VOLUME_INST_ID:
            /* fetch volume */
            if ((result = OTA_FetchBitField(pEASData->hwInstData, pData, 4, &temp)) != EAS_SUCCESS)
                return result;
            pData->velocity = temp ? (EAS_U8) (temp * OTA_VEL_MUL + OTA_VEL_OFS) : 0;
            break;

        default:
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Unexpected instruction ID in OTA stream\n"); */ }
            return EAS_ERROR_FILE_FORMAT;
    }

    /* decrement pattern length */
    pData->current.patternLen--;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * OTA_State()
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
/*lint -esym(715, pEASData) common decoder interface - pEASData not used */
static EAS_RESULT OTA_State (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 *pState)
{
    S_OTA_DATA* pData;

    /* establish pointer to instance data */
    pData = (S_OTA_DATA*) pInstData;

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
 * OTA_Close()
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
static EAS_RESULT OTA_Close (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_OTA_DATA* pData;
    EAS_RESULT result;

    pData = (S_OTA_DATA*) pInstData;

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
 * OTA_Reset()
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
static EAS_RESULT OTA_Reset (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_OTA_DATA* pData;
    EAS_RESULT result;

    pData = (S_OTA_DATA*) pInstData;

    /* reset the synth */
    VMReset(pEASData->pVoiceMgr, pData->pSynth, EAS_TRUE);
    pData->note = 0;

    /* reset file position and re-parse header */
    pData->state = EAS_STATE_ERROR;
    if ((result = OTA_ParseHeader (pEASData,  pData)) != EAS_SUCCESS)
        return result;

    pData->state = EAS_STATE_READY;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * OTA_Pause()
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
static EAS_RESULT OTA_Pause (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_OTA_DATA *pData;

    /* can't pause a stopped stream */
    pData = (S_OTA_DATA*) pInstData;
    if (pData->state == EAS_STATE_STOPPED)
        return EAS_ERROR_ALREADY_STOPPED;

    /* mute the synthesizer */
    VMMuteAllVoices(pEASData->pVoiceMgr, pData->pSynth);
    pData->state = EAS_STATE_PAUSING;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * OTA_Resume()
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
/*lint -esym(715, pEASData) common decoder interface - pEASData not used */
static EAS_RESULT OTA_Resume (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_OTA_DATA *pData;

    /* can't resume a stopped stream */
    pData = (S_OTA_DATA*) pInstData;
    if (pData->state == EAS_STATE_STOPPED)
        return EAS_ERROR_ALREADY_STOPPED;

    /* nothing to do but resume playback */
    pData->state = EAS_STATE_PLAY;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * OTA_SetData()
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
/*lint -esym(715, pEASData) common decoder interface - pEASData not used */
static EAS_RESULT OTA_SetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value)
{
    S_OTA_DATA *pData;

    pData = (S_OTA_DATA *) pInstData;
    switch (param)
    {

        /* set metadata callback */
        case PARSER_DATA_METADATA_CB:
            EAS_HWMemCpy(&pData->metadata, (void*) value, sizeof(S_METADATA_CB));
            break;

        default:
            return EAS_ERROR_INVALID_PARAMETER;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * OTA_GetData()
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
/*lint -esym(715, pEASData) common decoder interface - pEASData not used */
static EAS_RESULT OTA_GetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue)
{
    S_OTA_DATA *pData;

    pData = (S_OTA_DATA*) pInstData;
    switch (param)
    {
        /* return file type as OTA */
        case PARSER_DATA_FILE_TYPE:
            *pValue = EAS_FILE_OTA;
            break;

#if 0
        /* set transposition */
        case PARSER_DATA_TRANSPOSITION:
            *pValue = pData->transposition;
            break;
#endif

        case PARSER_DATA_SYNTH_HANDLE:
            *pValue = (EAS_I32) pData->pSynth;
            break;

        case PARSER_DATA_GAIN_OFFSET:
            *pValue = OTA_GAIN_OFFSET;
            break;

        default:
            return EAS_ERROR_INVALID_PARAMETER;
    }
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * OTA_ParseHeader()
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
static EAS_RESULT OTA_ParseHeader (S_EAS_DATA *pEASData, S_OTA_DATA* pData)
{
    EAS_RESULT result;
    EAS_INT i;
    EAS_INT state;
    EAS_U8 temp;
    EAS_U8 titleLen;

    /* initialize some data */
    pData->flags = 0;
    pData->time = 0;
    pData->tick = DEFAULT_TICK_CONV;
    pData->patterns[0].fileOffset = pData->patterns[1].fileOffset =
        pData->patterns[2].fileOffset = pData->patterns[3].fileOffset = -1;
    pData->current.bitCount = 0;
    pData->current.patternLen = 0;
    pData->loopCount = 0;
    pData->restore.fileOffset = -1;
    pData->note = 0;
    pData->restTicks = 0;
    pData->velocity = OTA_VEL_DEFAULT;
    pData->style = 0;
    pData->octave = 59;

    /* seek to start of data */
    if ((result = EAS_HWFileSeek(pEASData->hwInstData, pData->fileHandle, pData->fileOffset)) != EAS_SUCCESS)
        return result;

    /* read the first byte, should be command length */
    if ((result = EAS_HWGetByte(pEASData->hwInstData, pData->fileHandle, &temp)) != EAS_SUCCESS)
        return result;

    /* read all the commands */
    i = temp;
    state = 0;
    while (i--)
    {

        /* fetch command, always starts on byte boundary */
        pData->current.bitCount = 0;
        if ((result = OTA_FetchBitField(pEASData->hwInstData, pData, 7, &temp)) != EAS_SUCCESS)
            return result;

        if (state == 0)
        {
            if (temp != OTA_RINGTONE)
            {
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Expected OTA Ring Tone Programming type\n"); */ }
                return EAS_ERROR_FILE_FORMAT;
            }
            state++;
        }
        else
        {

            if (temp == OTA_SOUND)
                break;

            if (temp == OTA_UNICODE)
                pData->flags |= OTA_FLAGS_UNICODE;
            else
            {
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Expected OTA Sound or Unicode type\n"); */ }
                return EAS_ERROR_FILE_FORMAT;
            }
        }
    }

    /* get song type */
    if ((result = OTA_FetchBitField(pEASData->hwInstData, pData, 3, &temp)) != EAS_SUCCESS)
        return result;

    /* check for basic song type */
    if (temp == OTA_BASIC_SONG_TYPE)
    {
        /* fetch title length */
        if ((result = OTA_FetchBitField(pEASData->hwInstData, pData, 4, &titleLen)) != EAS_SUCCESS)
            return result;

        /* if unicode, double the length */
        if (pData->flags & OTA_FLAGS_UNICODE)
            titleLen = (EAS_U8) (titleLen << 1);

        /* zero the metadata buffer */
        if (pData->metadata.buffer)
            EAS_HWMemSet(pData->metadata.buffer, 0, pData->metadata.bufferSize);

        /* read the song title */
        for (i = 0; i < titleLen; i++)
        {
            /* fetch character */
            if ((result = OTA_FetchBitField(pEASData->hwInstData, pData, 8, &temp)) != EAS_SUCCESS)
                return result;

            /* check for metadata callback */
            if (pData->metadata.callback)
            {
                if (i < (pData->metadata.bufferSize - 1))
                    pData->metadata.buffer[i] = (char) temp;
            }
        }

        /* if host has registered callback, call it now */
        if (pData->metadata.callback)
            (*pData->metadata.callback)(EAS_METADATA_TITLE, pData->metadata.buffer, pData->metadata.pUserData);
    }

    /* must be temporary song */
    else if (temp != OTA_TEMPORARY_SONG_TYPE)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Expected OTA basic or temporary song type\n"); */ }
        return EAS_ERROR_FILE_FORMAT;
    }

    /* get the song length */
    if ((result = OTA_FetchBitField(pEASData->hwInstData, pData, 8, &pData->numPatterns)) != EAS_SUCCESS)
        return result;

    /* sanity check */
    if (pData->numPatterns == 0)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "OTA number of patterns is zero\n"); */ }
        return EAS_ERROR_FILE_FORMAT;
    }

    /* at start of first pattern */
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * OTA_FetchBitField()
 *----------------------------------------------------------------------------
 * Purpose:
 * Fetch a specified number of bits from the input stream
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
static EAS_RESULT OTA_FetchBitField (EAS_HW_DATA_HANDLE hwInstData, S_OTA_DATA *pData, EAS_I32 numBits, EAS_U8 *pValue)
{
    EAS_RESULT result;
    EAS_I32 bitsLeft;
    EAS_U8 value;

    value = 0;

    /* do we have enough bits? */
    bitsLeft = pData->current.bitCount - numBits;

    /* not enough bits, assemble them from 2 characters */
    if (bitsLeft < 0)
    {
        /* grab the remaining bits from the previous byte */
        if (pData->current.bitCount)
            /*lint -e{504,734} this is a legitimate shift operation */
            value = pData->current.dataByte << -bitsLeft;

        /* read the next byte */
        if ((result = EAS_HWGetByte(hwInstData, pData->fileHandle, &pData->current.dataByte)) != EAS_SUCCESS)
            return result;
        bitsLeft += 8;
    }

    /* more bits than needed? */
    if (bitsLeft > 0)
    {
        value |= pData->current.dataByte >> bitsLeft;
        pData->current.bitCount = (EAS_U8) bitsLeft;
        pData->current.dataByte = pData->current.dataByte & (0xff >> (8 - bitsLeft));
    }

    /* exactly the right number of bits */
    else
    {
        value |= pData->current.dataByte;
        pData->current.bitCount = 0;
    }

    *pValue = value;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * OTA_SavePosition()
 *----------------------------------------------------------------------------
 * Purpose:
 *
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
static EAS_RESULT OTA_SavePosition (EAS_HW_DATA_HANDLE hwInstData, S_OTA_DATA *pData, S_OTA_LOC *pLoc)
{
    EAS_HWMemCpy(pLoc, &pData->current, sizeof(S_OTA_LOC));
    return EAS_HWFilePos(hwInstData, pData->fileHandle, &pLoc->fileOffset);
}

/*----------------------------------------------------------------------------
 * OTA_RestorePosition()
 *----------------------------------------------------------------------------
 * Purpose:
 *
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
static EAS_RESULT OTA_RestorePosition (EAS_HW_DATA_HANDLE hwInstData, S_OTA_DATA *pData, S_OTA_LOC *pLoc)
{
    EAS_HWMemCpy(&pData->current, pLoc, sizeof(S_OTA_LOC));
    pData->restore.fileOffset = -1;
    return EAS_HWFileSeek(hwInstData, pData->fileHandle, pLoc->fileOffset);
}

