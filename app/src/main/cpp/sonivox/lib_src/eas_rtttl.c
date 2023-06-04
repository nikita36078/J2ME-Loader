/*----------------------------------------------------------------------------
 *
 * File:
 * eas_rtttl.c
 *
 * Contents and purpose:
 * RTTTL parser
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

#include "eas_data.h"
#include "eas_miditypes.h"
#include "eas_parser.h"
#include "eas_report.h"
#include "eas_host.h"
#include "eas_midi.h"
#include "eas_config.h"
#include "eas_vm_protos.h"
#include "eas_rtttldata.h"
#include "eas_ctype.h"

/* increase gain for mono ringtones */
#define RTTTL_GAIN_OFFSET       8

/* maximum title length including colon separator */
#define RTTTL_MAX_TITLE_LEN     32
#define RTTTL_INFINITE_LOOP     15

/* length of 32nd note in 1/256ths of a msec for 63 BPM tempo */
#define DEFAULT_TICK_CONV       30476
#define TICK_CONVERT            1920000

/* default channel and program for RTTTL playback */
#define RTTTL_CHANNEL           0
#define RTTTL_PROGRAM           80
#define RTTTL_VELOCITY          127

/* note used for rest */
#define RTTTL_REST              1

/* multiplier for fixed point triplet conversion */
#define TRIPLET_MULTIPLIER      683
#define TRIPLET_SHIFT           10

/* local prototypes */
static EAS_RESULT RTTTL_CheckFileType (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, EAS_VOID_PTR *ppHandle, EAS_I32 offset);
static EAS_RESULT RTTTL_Prepare (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT RTTTL_Time (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_U32 *pTime);
static EAS_RESULT RTTTL_Event (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_INT parserMode);
static EAS_RESULT RTTTL_State (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_STATE *pState);
static EAS_RESULT RTTTL_Close (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT RTTTL_Reset (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT RTTTL_Pause (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT RTTTL_Resume (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT RTTTL_SetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value);
static EAS_RESULT RTTTL_GetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue);
static EAS_RESULT RTTTL_GetStyle (EAS_HW_DATA_HANDLE hwInstData, S_RTTTL_DATA *pData);
static EAS_RESULT RTTTL_GetDuration (EAS_HW_DATA_HANDLE hwInstData, S_RTTTL_DATA *pData, EAS_I8 *pDuration);
static EAS_RESULT RTTTL_GetOctave (EAS_HW_DATA_HANDLE hwInstData, S_RTTTL_DATA *pData, EAS_U8 *pOctave);
static EAS_RESULT RTTTL_GetTempo (EAS_HW_DATA_HANDLE hwInstData, S_RTTTL_DATA *pData);
static EAS_RESULT RTTTL_GetNumber (EAS_HW_DATA_HANDLE hwInstData, S_RTTTL_DATA *pData, EAS_I32 *pValue);
static EAS_RESULT RTTTL_ParseHeader (S_EAS_DATA *pEASData, S_RTTTL_DATA* pData, EAS_BOOL metaData);
static EAS_RESULT RTTTL_GetNextChar (EAS_HW_DATA_HANDLE hwInstData, S_RTTTL_DATA *pData, EAS_I8 *pValue);
static EAS_RESULT RTTTL_PeekNextChar (EAS_HW_DATA_HANDLE hwInstData, S_RTTTL_DATA *pData, EAS_I8 *pValue);

/* inline functions */
EAS_INLINE void RTTTL_PutBackChar (S_RTTTL_DATA *pData, EAS_I8 value) { pData->dataByte = value; }


/* lookup table for note values */
static const EAS_U8 noteTable[] = { 21, 23, 12, 14, 16, 17, 19, 23 };

/*----------------------------------------------------------------------------
 *
 * EAS_RTTTL_Parser
 *
 * This structure contains the functional interface for the iMelody parser
 *----------------------------------------------------------------------------
*/
const S_FILE_PARSER_INTERFACE EAS_RTTTL_Parser =
{
    RTTTL_CheckFileType,
    RTTTL_Prepare,
    RTTTL_Time,
    RTTTL_Event,
    RTTTL_State,
    RTTTL_Close,
    RTTTL_Reset,
#ifdef JET_INTERFACE
    RTTTL_Pause,
    RTTTL_Resume,
#else
    NULL,
    NULL,
#endif
    NULL,
    RTTTL_SetData,
    RTTTL_GetData,
    NULL
};

/*----------------------------------------------------------------------------
 * RTTTL_CheckFileType()
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
static EAS_RESULT RTTTL_CheckFileType (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, EAS_VOID_PTR *ppHandle, EAS_I32 offset)
{
    S_RTTTL_DATA data;
    S_RTTTL_DATA *pData;

    /* see if we can parse the header */
    data.fileHandle = fileHandle;
    data.fileOffset = offset;
    *ppHandle= NULL;
    if (RTTTL_ParseHeader (pEASData, &data, EAS_FALSE) == EAS_SUCCESS)
    {

        /* check for static memory allocation */
        if (pEASData->staticMemoryModel)
            pData = EAS_CMEnumData(EAS_CM_RTTTL_DATA);
        else
            pData = EAS_HWMalloc(pEASData->hwInstData, sizeof(S_RTTTL_DATA));
        if (!pData)
            return EAS_ERROR_MALLOC_FAILED;
        EAS_HWMemSet(pData, 0, sizeof(S_RTTTL_DATA));

        /* return a pointer to the instance data */
        pData->fileHandle = fileHandle;
        pData->fileOffset = offset;
        pData->state = EAS_STATE_OPEN;
        *ppHandle = pData;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * RTTTL_Prepare()
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
static EAS_RESULT RTTTL_Prepare (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_RTTTL_DATA* pData;
    EAS_RESULT result;

    /* check for valid state */
    pData = (S_RTTTL_DATA*) pInstData;
    if (pData->state != EAS_STATE_OPEN)
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;

    /* instantiate a synthesizer */
    if ((result = VMInitMIDI(pEASData, &pData->pSynth)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "VMInitMIDI returned %d\n", result); */ }
        return result;
    }

    pData->state = EAS_STATE_ERROR;
    if ((result = RTTTL_ParseHeader (pEASData,  pData, (EAS_BOOL) (pData->metadata.callback != NULL))) != EAS_SUCCESS)
    {
        /* if using dynamic memory, free it */
        if (!pEASData->staticMemoryModel)
            EAS_HWFree(pEASData->hwInstData, pData);
        return result;
    }

    pData->state = EAS_STATE_READY;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * RTTTL_Time()
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
static EAS_RESULT RTTTL_Time (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_U32 *pTime)
{
    S_RTTTL_DATA *pData;

    pData = (S_RTTTL_DATA*) pInstData;

    /* return time in milliseconds */
    /*lint -e{704} use shift instead of division */
    *pTime = pData->time >> 8;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * RTTTL_Event()
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
static EAS_RESULT RTTTL_Event (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_INT parserMode)
{
    S_RTTTL_DATA* pData;
    EAS_RESULT result;
    EAS_I32 ticks;
    EAS_I32 temp;
    EAS_I8 c;
    EAS_U8 note;
    EAS_U8 octave;

    pData = (S_RTTTL_DATA*) pInstData;
    if (pData->state >= EAS_STATE_OPEN)
        return EAS_SUCCESS;

    /* initialize MIDI channel when the track starts playing */
    if (pData->time == 0)
    {
        /* set program to square lead */
        VMProgramChange(pEASData->pVoiceMgr, pData->pSynth, RTTTL_CHANNEL, RTTTL_PROGRAM);

        /* set channel volume to max */
        VMControlChange(pEASData->pVoiceMgr, pData->pSynth, RTTTL_CHANNEL, 7, 127);
    }

    /* check for end of note */
    if (pData->note)
    {
        /* stop the note */
        VMStopNote(pEASData->pVoiceMgr, pData->pSynth, RTTTL_CHANNEL, pData->note, 0);
        pData->note = 0;

        /* check for rest between notes */
        if (pData->restTicks)
        {
            pData->time += pData->restTicks;
            pData->restTicks = 0;
            return EAS_SUCCESS;
        }
    }

    /* parse the next event */
    octave = pData->octave;
    note = 0;
    ticks = pData->duration * pData->tick;
    for (;;)
    {

        /* get next character */
        if ((result = RTTTL_GetNextChar(pEASData->hwInstData, pData, &c)) != EAS_SUCCESS)
        {
            if (result != EAS_EOF)
                return result;

            /* end of file, if no notes to process, check for looping */
            if (!note)
            {
                /* if no loop set state to stopping */
                if (pData->repeatCount == 0)
                {
                    pData->state = EAS_STATE_STOPPING;
                    VMReleaseAllVoices(pEASData->pVoiceMgr, pData->pSynth);
                    return EAS_SUCCESS;
                }

                /* decrement loop count */
                if (pData->repeatCount != RTTTL_INFINITE_LOOP)
                    pData->repeatCount--;

                /* if locating, ignore infinite loops */
                else if (parserMode != eParserModePlay)
                {
                    pData->state = EAS_STATE_STOPPING;
                    VMReleaseAllVoices(pEASData->pVoiceMgr, pData->pSynth);
                    return EAS_SUCCESS;
                }

                /* loop back to start of notes */
                if (pData->notePlayedSinceRepeat == 0) {
                    return EAS_ERROR_FILE_FORMAT;
                }
                if ((result = EAS_HWFileSeek(pEASData->hwInstData, pData->fileHandle, pData->repeatOffset)) != EAS_SUCCESS)
                    return result;
                pData->notePlayedSinceRepeat = 0;
                continue;
            }

            /* still have a note to process */
            else
                c = ',';
        }

        /* bpm */
        if (c == 'b')
        {
            /* peek at next character */
            if ((result = RTTTL_PeekNextChar(pEASData->hwInstData, pData, &c)) != EAS_SUCCESS)
                return result;

            /* if a number, must be octave or tempo */
            if (IsDigit(c))
            {
                if ((result = RTTTL_GetNumber(pEASData->hwInstData, pData, &temp)) != EAS_SUCCESS)
                    return result;

                /* check for octave first */
                if ((temp >= 4) && (temp <= 7))
                {
                    octave = (EAS_U8) temp;
                }

                /* check for tempo */
                else if ((temp >= 25) && (temp <= 900))
                {
                    pData->tick = TICK_CONVERT / (EAS_U32) temp;
                }

                /* don't know what it was */
                else
                    return EAS_ERROR_FILE_FORMAT;
            }

            /* must be a note */
            else
            {
                note = noteTable[1];
            }
        }

        /* octave */
        else if (c == 'o')
        {
            if ((result = RTTTL_GetOctave(pEASData->hwInstData, pData, &pData->octave)) != EAS_SUCCESS)
                return result;
        }

        /* style */
        else if (c == 's')
        {
            if ((result = RTTTL_GetStyle(pEASData->hwInstData, pData)) != EAS_SUCCESS)
                return result;
        }

        /* duration or octave */
        else if (IsDigit(c))
        {
            RTTTL_PutBackChar(pData, c);

            /* duration comes before note */
            if (!note)
            {
                if ((result = RTTTL_GetDuration(pEASData->hwInstData, pData, &c)) != EAS_SUCCESS)
                    return result;
                ticks = c * pData->tick;
            }

            /* octave comes after note */
            else
            {
                if ((result = RTTTL_GetOctave(pEASData->hwInstData, pData, &octave)) != EAS_SUCCESS)
                    return result;
            }
        }

        /* note or rest */
        else if ((c >= 'a') && (c <= 'h'))
        {
            note = noteTable[c - 'a'];
        }

        else if (c == 'p')
        {
            note = RTTTL_REST;
        }

        /* dotted note */
        else if (c == '.')
        {
            /* Number of ticks must not be greater than 32-bits */
            if ((ticks >> 1) > (INT32_MAX - ticks))
            {
                return EAS_ERROR_FILE_FORMAT;
            }

            /*lint -e{704} shift for performance */
            ticks += ticks >> 1;
        }

        /* accidental */
        else if (c == '#')
        {
            if (note)
                note++;
        }

        /* end of event */
        else if ((c == ',') && note)
        {
            pData->notePlayedSinceRepeat = 1;

            /* handle note events */
            if (note != RTTTL_REST)
            {

                /* save note and start it */
                pData->note = note + octave;
                if (parserMode == eParserModePlay)
                    VMStartNote(pEASData->pVoiceMgr, pData->pSynth, RTTTL_CHANNEL, pData->note, RTTTL_VELOCITY);

                /* determine note length */
                switch (pData->style)
                {
                    /* natural */
                    case 'n':
                        /*lint -e{704} shift for performance */
                        pData->restTicks = ticks >> 4;
                        break;
                    /* continuous */

                    case 'c':
                        pData->restTicks = 0;
                        break;

                    /* staccato */
                    case 's':
                        /*lint -e{704} shift for performance */
                        pData->restTicks = ticks >> 1;
                        break;

                    default:
                        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "RTTTL_Event: Unexpected style type %c\n", pData->style); */ }
                        break;
                }

                /* next event is at end of this note */
                if ((ticks - pData->restTicks) > (INT32_MAX - pData->time))
                {
                    return EAS_ERROR_FILE_FORMAT;
                }
                pData->time += ticks - pData->restTicks;
            }

            /* rest */
            else
            {
                if (ticks > (INT32_MAX - pData->time))
                {
                    return EAS_ERROR_FILE_FORMAT;
                }
                pData->time += ticks;
            }

            /* event found, return to caller */
            break;
        }
    }

    pData->state = EAS_STATE_PLAY;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * RTTTL_State()
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
static EAS_RESULT RTTTL_State (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 *pState)
{
    S_RTTTL_DATA* pData;

    /* establish pointer to instance data */
    pData = (S_RTTTL_DATA*) pInstData;

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
 * RTTTL_Close()
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
static EAS_RESULT RTTTL_Close (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_RTTTL_DATA* pData;
    EAS_RESULT result;

    pData = (S_RTTTL_DATA*) pInstData;

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
 * RTTTL_Reset()
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
static EAS_RESULT RTTTL_Reset (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_RTTTL_DATA* pData;
    EAS_RESULT result;

    pData = (S_RTTTL_DATA*) pInstData;

    /* reset the synth */
    VMReset(pEASData->pVoiceMgr, pData->pSynth, EAS_TRUE);

    /* reset time to zero */
    pData->time = 0;
    pData->note = 0;

    /* reset file position and re-parse header */
    pData->state = EAS_STATE_ERROR;
    if ((result = EAS_HWFileSeek(pEASData->hwInstData, pData->fileHandle, pData->fileOffset)) != EAS_SUCCESS)
        return result;
    if ((result = RTTTL_ParseHeader (pEASData,  pData, EAS_TRUE)) != EAS_SUCCESS)
        return result;

    pData->state = EAS_STATE_READY;
    return EAS_SUCCESS;
}

#ifdef JET_INTERFACE
/*----------------------------------------------------------------------------
 * RTTTL_Pause()
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
static EAS_RESULT RTTTL_Pause (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_RTTTL_DATA *pData;

    /* can't pause a stopped stream */
    pData = (S_RTTTL_DATA*) pInstData;
    if (pData->state == EAS_STATE_STOPPED)
        return EAS_ERROR_ALREADY_STOPPED;

    /* mute the synthesizer */
    VMMuteAllVoices(pEASData->pVoiceMgr, pData->pSynth);
    pData->state = EAS_STATE_PAUSING;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * RTTTL_Resume()
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
static EAS_RESULT RTTTL_Resume (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_RTTTL_DATA *pData;

    /* can't resume a stopped stream */
    pData = (S_RTTTL_DATA*) pInstData;
    if (pData->state == EAS_STATE_STOPPED)
        return EAS_ERROR_ALREADY_STOPPED;

    /* nothing to do but resume playback */
    pData->state = EAS_STATE_PLAY;
    return EAS_SUCCESS;
}
#endif

/*----------------------------------------------------------------------------
 * RTTTL_SetData()
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
/*lint -esym(715, pEASData) reserved for future use */
static EAS_RESULT RTTTL_SetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value)
{
    S_RTTTL_DATA *pData;

    pData = (S_RTTTL_DATA *) pInstData;
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
 * RTTTL_GetData()
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
/*lint -esym(715, pEASData) reserved for future use */
static EAS_RESULT RTTTL_GetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue)
{
    S_RTTTL_DATA *pData;

    pData = (S_RTTTL_DATA *) pInstData;
    switch (param)
    {
        /* return file type as RTTTL */
        case PARSER_DATA_FILE_TYPE:
            *pValue = EAS_FILE_RTTTL;
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
            *pValue = RTTTL_GAIN_OFFSET;
            break;

    default:
            return EAS_ERROR_INVALID_PARAMETER;
    }
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * RTTTL_GetStyle()
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
static EAS_RESULT RTTTL_GetStyle (EAS_HW_DATA_HANDLE hwInstData, S_RTTTL_DATA *pData)
{
    EAS_RESULT result;
    EAS_I8 style;

    /* get style */
    if ((result = RTTTL_GetNextChar(hwInstData, pData, &style)) != EAS_SUCCESS)
        return result;

    if ((style != 's')  && (style != 'n') && (style != 'c'))
        return EAS_ERROR_FILE_FORMAT;

    pData->style = style;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * RTTTL_GetDuration()
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
static EAS_RESULT RTTTL_GetDuration (EAS_HW_DATA_HANDLE hwInstData, S_RTTTL_DATA *pData, EAS_I8 *pDuration)
{
    EAS_RESULT result;
    EAS_I32 duration;
    EAS_I8 temp;

    /* get the duration */
    if ((result = RTTTL_GetNumber(hwInstData, pData, &duration)) != EAS_SUCCESS)
        return result;

    if ((duration != 1) && (duration != 2) && (duration != 4) && (duration != 8) && (duration != 16) && (duration != 32))
        return EAS_ERROR_FILE_FORMAT;

    temp = 64;
    while (duration)
    {
        /*lint -e{704} shift for performance */
        duration = duration >> 1;
        /*lint -e{702} use shift for performance */
        temp = temp >> 1;
    }

    *pDuration = temp;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * RTTTL_GetOctave()
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
static EAS_RESULT RTTTL_GetOctave (EAS_HW_DATA_HANDLE hwInstData, S_RTTTL_DATA *pData, EAS_U8 *pOctave)
{
    EAS_RESULT result;
    EAS_I32 octave;

    /* get the tempo */
    if ((result = RTTTL_GetNumber(hwInstData, pData, &octave)) != EAS_SUCCESS)
        return result;

    if ((octave < 4) || (octave > 7))
        return EAS_ERROR_FILE_FORMAT;

    *pOctave = (EAS_U8) (octave * 12);
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * RTTTL_GetTempo()
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
static EAS_RESULT RTTTL_GetTempo (EAS_HW_DATA_HANDLE hwInstData, S_RTTTL_DATA *pData)
{
    EAS_RESULT result;
    EAS_I32 tempo;

    /* get the tempo */
    if ((result = RTTTL_GetNumber(hwInstData, pData, &tempo)) != EAS_SUCCESS)
        return result;

    if ((tempo < 25) || (tempo > 900))
        return EAS_ERROR_FILE_FORMAT;

    pData->tick = TICK_CONVERT / (EAS_U32) tempo;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * RTTTL_GetNumber()
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
static EAS_RESULT RTTTL_GetNumber (EAS_HW_DATA_HANDLE hwInstData, S_RTTTL_DATA *pData, EAS_I32 *pValue)
{
    EAS_RESULT result;
    EAS_INT temp;
    EAS_I8 c;

    *pValue = -1;
    temp = 0;
    for (;;)
    {
        if ((result = RTTTL_PeekNextChar(hwInstData, pData, &c)) != EAS_SUCCESS)
        {
            if ((result == EAS_EOF) && (*pValue != -1))
                return EAS_SUCCESS;
            return result;
        }

        if (IsDigit(c))
        {
            pData->dataByte = 0;
            if (temp > 100) {
                // This is just to prevent overflows in case of a really large number
                // in the file, but rather than allowing the number to grow up to INT_MAX,
                // we limit it to a much smaller number since the numbers in an RTTTL file
                // are supposed to be at most in the hundreds, not millions or billions.
                // There are more specific checks in the callers of this function to enforce
                // the various limits for notes, octaves, tempo, etc.
                return EAS_FAILURE;
            }
            temp = temp * 10 + c - '0';
            *pValue = temp;
        }
        else
            return EAS_SUCCESS;
    }
}

/*----------------------------------------------------------------------------
 * RTTTL_ParseHeader()
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
static EAS_RESULT RTTTL_ParseHeader (S_EAS_DATA *pEASData, S_RTTTL_DATA* pData, EAS_BOOL metaData)
{
    EAS_RESULT result;
    EAS_I32 i;
    EAS_I8 temp;
    EAS_I8 control;

    /* initialize some defaults */
    pData->time = 0;
    pData->tick = DEFAULT_TICK_CONV;
    pData->note = 0;
    pData->duration = 4;
    pData ->restTicks = 0;
    pData->octave = 60;
    pData->repeatOffset = -1;
    pData->repeatCount = 0;
    pData->style = 'n';
    pData->dataByte = 0;

    metaData = metaData && (pData->metadata.buffer != NULL) && (pData->metadata.callback != NULL);

    /* seek to start of data */
    if ((result = EAS_HWFileSeek(pEASData->hwInstData, pData->fileHandle, pData->fileOffset)) != EAS_SUCCESS)
        return result;

    /* zero the metadata buffer */
    if (metaData)
        EAS_HWMemSet(pData->metadata.buffer, 0, pData->metadata.bufferSize);

    /* read the title */
    for (i = 0; i < RTTTL_MAX_TITLE_LEN; i++)
    {
        if ((result = EAS_HWGetByte(pEASData->hwInstData, pData->fileHandle, &temp)) != EAS_SUCCESS)
            return result;

        if (temp == ':')
            break;

        /* pass along metadata */
        if (metaData)
        {
            if (i < (pData->metadata.bufferSize- 1))
                pData->metadata.buffer[i] = (char) temp;
        }
    }

    /* check for error in title */
    if (i == RTTTL_MAX_TITLE_LEN)
        return EAS_ERROR_FILE_FORMAT;

    /* pass along metadata */
    if (metaData)
        (*pData->metadata.callback)(EAS_METADATA_TITLE, pData->metadata.buffer, pData->metadata.pUserData);

    /* control fields */
    for (;;)
    {

        /* get control type */
        if ((result = RTTTL_GetNextChar(pEASData->hwInstData, pData, &control)) != EAS_SUCCESS)
            return result;

        /* next char should be equal sign */
        if ((result = RTTTL_GetNextChar(pEASData->hwInstData, pData, &temp)) != EAS_SUCCESS)
            return result;
        if (temp != '=')
            return EAS_ERROR_FILE_FORMAT;

        /* get the control value */
        switch (control)
        {

            /* bpm */
            case 'b':
                if ((result = RTTTL_GetTempo(pEASData->hwInstData, pData)) != EAS_SUCCESS)
                    return result;
                break;

            /* duration */
            case 'd':
                if ((result = RTTTL_GetDuration(pEASData->hwInstData, pData, &temp)) != EAS_SUCCESS)
                    return result;
                pData->duration = temp;
                break;

            /* loop */
            case 'l':
                if ((result = RTTTL_GetNumber(pEASData->hwInstData, pData, &i)) != EAS_SUCCESS)
                    return result;
                if ((i < 0) || (i > 15))
                    return EAS_ERROR_FILE_FORMAT;
                pData->repeatCount = (EAS_U8) i;
                break;

            /* octave */
            case 'o':
                if ((result = RTTTL_GetOctave(pEASData->hwInstData, pData, &pData->octave)) != EAS_SUCCESS)
                    return result;
                break;

            /* get style */
            case 's':
                if ((result = RTTTL_GetStyle(pEASData->hwInstData, pData)) != EAS_SUCCESS)
                    return result;
                break;

            /* unrecognized control */
            default:
                return EAS_ERROR_FILE_FORMAT;
        }

        /* next character should be comma or colon */
        if ((result = RTTTL_GetNextChar(pEASData->hwInstData, pData, &temp)) != EAS_SUCCESS)
            return result;

        /* check for end of control field */
        if (temp == ':')
            break;

        /* must be a comma */
        if (temp != ',')
            return EAS_ERROR_FILE_FORMAT;
    }

    /* should be at the start of the music block */
    if ((result = EAS_HWFilePos(pEASData->hwInstData, pData->fileHandle, &pData->repeatOffset)) != EAS_SUCCESS)
        return result;

    pData->notePlayedSinceRepeat = 0;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * RTTTL_GetNextChar()
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
static EAS_RESULT RTTTL_GetNextChar (EAS_HW_DATA_HANDLE hwInstData, S_RTTTL_DATA *pData, EAS_I8 *pValue)
{
    EAS_RESULT result;
    EAS_I8 temp;

    *pValue = 0;
    for(;;)
    {

        /* check for character that has been put back */
        if (pData->dataByte)
        {
            temp = pData->dataByte;
            pData->dataByte = 0;
        }
        else
        {
            if ((result = EAS_HWGetByte(hwInstData, pData->fileHandle, &temp)) != EAS_SUCCESS)
                return result;
        }

        /* ignore white space */
        if (!IsSpace(temp))
        {
            *pValue = ToLower(temp);
            return EAS_SUCCESS;
        }
    }
}

/*----------------------------------------------------------------------------
 * RTTTL_PeekNextChar()
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
static EAS_RESULT RTTTL_PeekNextChar (EAS_HW_DATA_HANDLE hwInstData, S_RTTTL_DATA *pData, EAS_I8 *pValue)
{
    EAS_RESULT result;
    EAS_I8 temp;

    *pValue = 0;
    for(;;)
    {

        /* read a character from the file, if necessary */
        if (!pData->dataByte)
        {
            if ((result = EAS_HWGetByte(hwInstData, pData->fileHandle, &pData->dataByte)) != EAS_SUCCESS)
                return result;

        }
        temp = pData->dataByte;

        /* ignore white space */
        if (!IsSpace(temp))
        {
            *pValue = ToLower(temp);
            return EAS_SUCCESS;
        }
        pData->dataByte = 0;
    }
}

