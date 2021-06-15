/*----------------------------------------------------------------------------
 *
 * File:
 * eas_imelody.c
 *
 * Contents and purpose:
 * iMelody parser
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
 *   $Revision: 797 $
 *   $Date: 2007-08-01 00:15:56 -0700 (Wed, 01 Aug 2007) $
 *----------------------------------------------------------------------------
*/

/* lint doesn't like the way some string.h files look */
#ifdef _lint
#include "lint_stdlib.h"
#else
#include <string.h>
#endif

#include "eas_data.h"
#include "eas_miditypes.h"
#include "eas_parser.h"
#include "eas_report.h"
#include "eas_host.h"
#include "eas_midi.h"
#include "eas_config.h"
#include "eas_vm_protos.h"
#include "eas_imelodydata.h"
#include "eas_ctype.h"

// #define _DEBUG_IMELODY

/* increase gain for mono ringtones */
#define IMELODY_GAIN_OFFSET     8

/* length of 32nd note in 1/256ths of a msec for 120 BPM tempo */
#define DEFAULT_TICK_CONV       16000
#define TICK_CONVERT            1920000

/* default channel and program for iMelody playback */
#define IMELODY_CHANNEL         0
#define IMELODY_PROGRAM         80
#define IMELODY_VEL_MUL         4
#define IMELODY_VEL_OFS         67

/* multiplier for fixed point triplet conversion */
#define TRIPLET_MULTIPLIER      683
#define TRIPLET_SHIFT           10

static const char* const tokens[] =
{
    "BEGIN:IMELODY",
    "VERSION:",
    "FORMAT:CLASS",
    "NAME:",
    "COMPOSER:",
    "BEAT:",
    "STYLE:",
    "VOLUME:",
    "MELODY:",
    "END:IMELODY"
};

/* ledon or ledoff */
static const char ledStr[] = "edo";

/* vibeon or vibeoff */
static const char vibeStr[] = "ibeo";

/* backon or backoff */
static const char backStr[] = "cko";

typedef enum
{
    TOKEN_BEGIN,
    TOKEN_VERSION,
    TOKEN_FORMAT,
    TOKEN_NAME,
    TOKEN_COMPOSER,
    TOKEN_BEAT,
    TOKEN_STYLE,
    TOKEN_VOLUME,
    TOKEN_MELODY,
    TOKEN_END,
    TOKEN_INVALID
} ENUM_IMELODY_TOKENS;

/* lookup table for note values */
static const EAS_I8 noteTable[] = { 9, 11, 0, 2, 4, 5, 7 };

/* inline functions */
#ifdef _DEBUG_IMELODY
static void PutBackChar (S_IMELODY_DATA *pData)
{
    if (pData->index)
        pData->index--;
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "PutBackChar '%c'\n", pData->buffer[pData->index]); */ }
}
#else
EAS_INLINE void PutBackChar (S_IMELODY_DATA *pData) { if (pData->index) pData->index--; }
#endif


/* local prototypes */
static EAS_RESULT IMY_CheckFileType (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, EAS_VOID_PTR *ppHandle, EAS_I32 offset);
static EAS_RESULT IMY_Prepare (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT IMY_Time (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_U32 *pTime);
static EAS_RESULT IMY_Event (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_INT parserMode);
static EAS_RESULT IMY_State (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_STATE *pState);
static EAS_RESULT IMY_Close (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT IMY_Reset (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT IMY_Pause (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT IMY_Resume (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT IMY_SetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value);
static EAS_RESULT IMY_GetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue);
static EAS_BOOL IMY_PlayNote (S_EAS_DATA *pEASData, S_IMELODY_DATA *pData, EAS_I8 note, EAS_INT parserMode);
static EAS_BOOL IMY_PlayRest (S_EAS_DATA *pEASData, S_IMELODY_DATA *pData);
static EAS_BOOL IMY_GetDuration (EAS_HW_DATA_HANDLE hwInstData, S_IMELODY_DATA *pData, EAS_I32 *pDuration);
static EAS_BOOL IMY_GetLEDState (S_EAS_DATA *pEASData, S_IMELODY_DATA *pData);
static EAS_BOOL IMY_GetVibeState (S_EAS_DATA *pEASData, S_IMELODY_DATA *pData);
static EAS_BOOL IMY_GetBackState (S_EAS_DATA *pEASData, S_IMELODY_DATA *pData);
static EAS_BOOL IMY_GetVolume (EAS_HW_DATA_HANDLE hwInstData, S_IMELODY_DATA *pData, EAS_BOOL inHeader);
static EAS_BOOL IMY_GetNumber (EAS_HW_DATA_HANDLE hwInstData, S_IMELODY_DATA *pData, EAS_INT *temp, EAS_BOOL inHeader);
static EAS_RESULT IMY_ParseHeader (S_EAS_DATA *pEASData, S_IMELODY_DATA* pData);
static EAS_I8 IMY_GetNextChar (EAS_HW_DATA_HANDLE hwInstData, S_IMELODY_DATA *pData, EAS_BOOL inHeader);
static EAS_RESULT IMY_ReadLine (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE fileHandle, EAS_I8 *buffer, EAS_I32 *pStartLine);
static EAS_INT IMY_ParseLine (EAS_I8 *buffer, EAS_U8 *pIndex);


/*----------------------------------------------------------------------------
 *
 * EAS_iMelody_Parser
 *
 * This structure contains the functional interface for the iMelody parser
 *----------------------------------------------------------------------------
*/
const S_FILE_PARSER_INTERFACE EAS_iMelody_Parser =
{
    IMY_CheckFileType,
    IMY_Prepare,
    IMY_Time,
    IMY_Event,
    IMY_State,
    IMY_Close,
    IMY_Reset,
    IMY_Pause,
    IMY_Resume,
    NULL,
    IMY_SetData,
    IMY_GetData,
    NULL
};

/*----------------------------------------------------------------------------
 * IMY_CheckFileType()
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
static EAS_RESULT IMY_CheckFileType (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, EAS_VOID_PTR *ppHandle, EAS_I32 offset)
{
    S_IMELODY_DATA* pData;
    EAS_I8 buffer[MAX_LINE_SIZE+1];
    EAS_U8 index;

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Enter IMY_CheckFileType\n"); */ }
#endif

    /* read the first line of the file */
    *ppHandle = NULL;
    if (IMY_ReadLine(pEASData->hwInstData, fileHandle, buffer, NULL) != EAS_SUCCESS)
        return EAS_SUCCESS;

    /* check for header string */
    if (IMY_ParseLine(buffer, &index) == TOKEN_BEGIN)
    {

        /* check for static memory allocation */
        if (pEASData->staticMemoryModel)
            pData = EAS_CMEnumData(EAS_CM_IMELODY_DATA);
        else
            pData = EAS_HWMalloc(pEASData->hwInstData, sizeof(S_IMELODY_DATA));
        if (!pData)
            return EAS_ERROR_MALLOC_FAILED;
        EAS_HWMemSet(pData, 0, sizeof(S_IMELODY_DATA));

        /* initialize */
        pData->fileHandle = fileHandle;
        pData->fileOffset = offset;
        pData->state = EAS_STATE_ERROR;
        pData->state = EAS_STATE_OPEN;

        /* return a pointer to the instance data */
        *ppHandle = pData;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * IMY_Prepare()
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
static EAS_RESULT IMY_Prepare (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_IMELODY_DATA* pData;
    EAS_RESULT result;

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Enter IMY_Prepare\n"); */ }
#endif

    /* check for valid state */
    pData = (S_IMELODY_DATA*) pInstData;
    if (pData->state != EAS_STATE_OPEN)
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;

    /* instantiate a synthesizer */
    if ((result = VMInitMIDI(pEASData, &pData->pSynth)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "VMInitMIDI returned %d\n", result); */ }
        return result;
    }

    /* parse the header */
    if ((result = IMY_ParseHeader(pEASData,  pData)) != EAS_SUCCESS)
        return result;

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_Prepare: state set to EAS_STATE_READY\n"); */ }
#endif

    pData ->state = EAS_STATE_READY;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * IMY_Time()
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
static EAS_RESULT IMY_Time (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_U32 *pTime)
{
    S_IMELODY_DATA *pData;

    pData = (S_IMELODY_DATA*) pInstData;

    /* return time in milliseconds */
    /*lint -e{704} use shift instead of division */
    *pTime = pData->time >> 8;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * IMY_Event()
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
static EAS_RESULT IMY_Event (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_INT parserMode)
{
    S_IMELODY_DATA* pData;
    EAS_RESULT result;
    EAS_I8 c;
    EAS_BOOL eof;
    EAS_INT temp;

    pData = (S_IMELODY_DATA*) pInstData;
    if (pData->state >= EAS_STATE_OPEN)
        return EAS_SUCCESS;

    if (pData->state == EAS_STATE_READY) {
        pData->state = EAS_STATE_PLAY;
    }

    /* initialize MIDI channel when the track starts playing */
    if (pData->time == 0)
    {

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_Event: Reset\n"); */ }
#endif
        /* set program to square lead */
        VMProgramChange(pEASData->pVoiceMgr, pData->pSynth, IMELODY_CHANNEL, IMELODY_PROGRAM);

        /* set channel volume to max */
        VMControlChange(pEASData->pVoiceMgr, pData->pSynth, IMELODY_CHANNEL, 7, 127);
    }

    /* check for end of note */
    if (pData->note)
    {

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Stopping note %d\n", pData->note); */ }
#endif
        /* stop the note */
        VMStopNote(pEASData->pVoiceMgr, pData->pSynth, IMELODY_CHANNEL, pData->note, 0);
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
    eof = EAS_FALSE;
    while (!eof)
    {

        /* get next character */
        c = IMY_GetNextChar(pEASData->hwInstData, pData, EAS_FALSE);

        switch (c)
        {
            /* start repeat */
            case '(':

#ifdef _DEBUG_IMELODY
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Enter repeat section\n", c); */ }
#endif

                if (pData->repeatOffset < 0)
                {
                    pData->repeatOffset = pData->startLine + (EAS_I32) pData->index;

                    /* save current time and check it later to make sure the loop isn't zero length */
                    pData->repeatTime = pData->time;

#ifdef _DEBUG_IMELODY
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Repeat offset = %d\n", pData->repeatOffset); */ }
#endif
                }
                else
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Ignoring nested repeat section\n"); */ }
                break;

            /* end repeat */
            case ')':

#ifdef _DEBUG_IMELODY
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "End repeat section, repeat offset = %d\n", pData->repeatOffset); */ }
#endif
                /* ignore zero-length loops */
                if (pData->repeatTime == pData->time) {
                    pData->repeatCount = -1;
                    pData->repeatOffset = -1;
                } else if (pData->repeatCount >= 0) {

                    /* decrement repeat count (repeatCount == 0 means infinite loop) */
                    if (pData->repeatCount > 0)
                    {
                        if (--pData->repeatCount == 0)
                        {
                            pData->repeatCount = -1;
#ifdef _DEBUG_IMELODY
                            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Repeat loop complete\n"); */ }
#endif
                        }
                    }

//2 TEMPORARY FIX: If locating, don't do infinite loops.
//3 We need a different mode for metadata parsing where we don't loop at all
                    if ((parserMode == eParserModePlay) || (pData->repeatCount != 0))
                    {

#ifdef _DEBUG_IMELODY
                        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Rewinding file for repeat\n"); */ }
#endif
                        /* rewind to start of loop */
                        if ((result = EAS_HWFileSeek(pEASData->hwInstData, pData->fileHandle, pData->repeatOffset)) != EAS_SUCCESS)
                            return result;
                        IMY_ReadLine(pEASData->hwInstData, pData->fileHandle, pData->buffer, &pData->startLine);
                        pData->index = 0;

                        /* if last loop, prevent future loops */
                        if (pData->repeatCount == -1)
                            pData->repeatOffset = -1;
                    }
                }
                break;

            /* repeat count */
            case '@':
                if (!IMY_GetNumber(pEASData->hwInstData, pData, &temp, EAS_FALSE))
                    eof = EAS_TRUE;
                else if (pData->repeatOffset > 0)
                {

#ifdef _DEBUG_IMELODY
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Repeat count = %dt", pData->repeatCount); */ }
#endif
                    if (pData->repeatCount < 0)
                        pData->repeatCount = (EAS_I16) temp;
                }
                break;

            /* volume */
            case 'V':
                if (!IMY_GetVolume(pEASData->hwInstData, pData, EAS_FALSE))
                    eof = EAS_TRUE;
                break;

            /* flat */
            case '&':
                pData->noteModifier = -1;
                break;

            /* sharp */
            case '#':
                pData->noteModifier = +1;
                break;

            /* octave */
            case '*':
                c = IMY_GetNextChar(pEASData->hwInstData, pData, EAS_FALSE);
                if (IsDigit(c))
                    pData->octave = (EAS_U8) ((c - '0' + 1) * 12);
                else if (!c)
                    eof = EAS_TRUE;
                break;

            /* ledon or ledoff */
            case 'l':
                if (!IMY_GetLEDState(pEASData, pData))
                    eof = EAS_TRUE;
                break;

            /* vibeon or vibeoff */
            case 'v':
                if (!IMY_GetVibeState(pEASData, pData))
                    eof = EAS_TRUE;
                break;

            /* either a B note or backon or backoff */
            case 'b':
                if (IMY_GetNextChar(pEASData->hwInstData, pData, EAS_FALSE) == 'a')
                {
                    if (!IMY_GetBackState(pEASData, pData))
                        eof = EAS_TRUE;
                }
                else
                {
                    PutBackChar(pData);
                    if (IMY_PlayNote(pEASData, pData, c, parserMode))
                        return EAS_SUCCESS;
                    eof = EAS_TRUE;
                }
                break;

            /* rest */
            case 'r':
            case 'R':
                if (IMY_PlayRest(pEASData, pData))
                    return EAS_SUCCESS;
                eof = EAS_TRUE;
                break;

            /* EOF */
            case 0:
#ifdef _DEBUG_IMELODY
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_Event: end of iMelody file detected\n"); */ }
#endif
                eof = EAS_TRUE;
            break;

            /* must be a note */
            default:
                c = ToLower(c);
                if ((c >= 'a') && (c <= 'g'))
                {
                    if (IMY_PlayNote(pEASData, pData, c, parserMode))
                        return EAS_SUCCESS;
                    eof = EAS_TRUE;
                }
                else
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Ignoring unexpected character '%c' [0x%02x]\n", c, c); */ }
                break;
        }
    }

    /* handle EOF */
#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_Event: state set to EAS_STATE_STOPPING\n"); */ }
#endif
    pData->state = EAS_STATE_STOPPING;
    VMReleaseAllVoices(pEASData->pVoiceMgr, pData->pSynth);
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * IMY_State()
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
static EAS_RESULT IMY_State (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 *pState)
{
    S_IMELODY_DATA* pData;

    /* establish pointer to instance data */
    pData = (S_IMELODY_DATA*) pInstData;

    /* if stopping, check to see if synth voices are active */
    if (pData->state == EAS_STATE_STOPPING)
    {
        if (VMActiveVoices(pData->pSynth) == 0)
        {
            pData->state = EAS_STATE_STOPPED;
#ifdef _DEBUG_IMELODY
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_State: state set to EAS_STATE_STOPPED\n"); */ }
#endif
        }
    }

    if (pData->state == EAS_STATE_PAUSING)
    {
        if (VMActiveVoices(pData->pSynth) == 0)
        {
#ifdef _DEBUG_IMELODY
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_State: state set to EAS_STATE_PAUSED\n"); */ }
#endif
            pData->state = EAS_STATE_PAUSED;
        }
    }

    /* return current state */
    *pState = pData->state;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * IMY_Close()
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
static EAS_RESULT IMY_Close (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_IMELODY_DATA* pData;
    EAS_RESULT result;

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_Close: close file\n"); */ }
#endif

    pData = (S_IMELODY_DATA*) pInstData;

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
 * IMY_Reset()
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
static EAS_RESULT IMY_Reset (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_IMELODY_DATA* pData;
    EAS_RESULT result;

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_Reset: reset file\n"); */ }
#endif
    pData = (S_IMELODY_DATA*) pInstData;

    /* reset the synth */
    VMReset(pEASData->pVoiceMgr, pData->pSynth, EAS_TRUE);

    /* reset time to zero */
    pData->time = 0;
    pData->note = 0;

    /* reset file position and re-parse header */
    pData->state = EAS_STATE_ERROR;
    if ((result = EAS_HWFileSeek(pEASData->hwInstData, pData->fileHandle, pData->fileOffset)) != EAS_SUCCESS)
        return result;
    if ((result = IMY_ParseHeader (pEASData,  pData)) != EAS_SUCCESS)
        return result;

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_Reset: state set to EAS_STATE_ERROR\n"); */ }
#endif

    pData->state = EAS_STATE_READY;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * IMY_Pause()
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
static EAS_RESULT IMY_Pause (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_IMELODY_DATA *pData;

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_Pause: pause file\n"); */ }
#endif

    /* can't pause a stopped stream */
    pData = (S_IMELODY_DATA*) pInstData;
    if (pData->state == EAS_STATE_STOPPED)
        return EAS_ERROR_ALREADY_STOPPED;

    /* mute the synthesizer */
    VMMuteAllVoices(pEASData->pVoiceMgr, pData->pSynth);
    pData->state = EAS_STATE_PAUSING;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * IMY_Resume()
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
static EAS_RESULT IMY_Resume (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_IMELODY_DATA *pData;

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_Resume: resume file\n"); */ }
#endif

    /* can't resume a stopped stream */
    pData = (S_IMELODY_DATA*) pInstData;
    if (pData->state == EAS_STATE_STOPPED)
        return EAS_ERROR_ALREADY_STOPPED;

    /* nothing to do but resume playback */
    pData->state = EAS_STATE_PLAY;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * IMY_SetData()
 *----------------------------------------------------------------------------
 * Purpose:
 * Adjust tempo relative to song tempo
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * pInstData        - pointer to iMelody instance data
 * rate             - rate (28-bit fractional amount)
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) common decoder interface - pEASData not used */
static EAS_RESULT IMY_SetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value)
{
    S_IMELODY_DATA *pData;

    pData = (S_IMELODY_DATA*) pInstData;
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
 * IMY_GetData()
 *----------------------------------------------------------------------------
 * Purpose:
 * Return the file type
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * pInstData        - pointer to iMelody instance data
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) common decoder interface - pEASData not used */
static EAS_RESULT IMY_GetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue)
{
    S_IMELODY_DATA *pData;

    pData = (S_IMELODY_DATA*) pInstData;

    switch (param)
    {
        /* return file type as iMelody */
        case PARSER_DATA_FILE_TYPE:
            *pValue = EAS_FILE_IMELODY;
            break;

        case PARSER_DATA_SYNTH_HANDLE:
            *pValue = (EAS_I32) pData->pSynth;
            break;

        case PARSER_DATA_GAIN_OFFSET:
            *pValue = IMELODY_GAIN_OFFSET;
            break;

        default:
            return EAS_ERROR_INVALID_PARAMETER;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * IMY_PlayNote()
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
static EAS_BOOL IMY_PlayNote (S_EAS_DATA *pEASData, S_IMELODY_DATA *pData, EAS_I8 note, EAS_INT parserMode)
{
    EAS_I32 duration;
    EAS_U8 velocity;


#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_PlayNote: start note %d\n", note); */ }
#endif

    /* get the duration */
    if (!IMY_GetDuration(pEASData->hwInstData, pData, &duration))
        return EAS_FALSE;

    /* save note value */
    pData->note = (EAS_U8) (pData->octave + noteTable[note - 'a'] + pData->noteModifier);
    velocity = (EAS_U8) (pData->volume ? pData->volume * IMELODY_VEL_MUL + IMELODY_VEL_OFS : 0);

    /* start note only if in play mode */
    if (parserMode == eParserModePlay)
        VMStartNote(pEASData->pVoiceMgr, pData->pSynth, IMELODY_CHANNEL, pData->note, velocity);

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_PlayNote: Start note %d, duration %d\n", pData->note, duration); */ }
#endif

    /* determine note length */
    switch (pData->style)
    {
        case 0:
            /*lint -e{704} shift for performance */
            pData->restTicks = duration >> 4;
            break;
        case 1:
            pData->restTicks = 0;
            break;
        case 2:
            /*lint -e{704} shift for performance */
            pData->restTicks = duration >> 1;
            break;
        default:
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "IMY_PlayNote: Note style out of range: %d\n", pData->style); */ }
            /*lint -e{704} shift for performance */
            pData->restTicks = duration >> 4;
            break;
    }

    /* next event is at end of this note */
    pData->time += duration - pData->restTicks;

    /* reset the flat/sharp modifier */
    pData->noteModifier = 0;

    return EAS_TRUE;
}

/*----------------------------------------------------------------------------
 * IMY_PlayRest()
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
static EAS_BOOL IMY_PlayRest (S_EAS_DATA *pEASData, S_IMELODY_DATA *pData)
{
    EAS_I32 duration;

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Enter IMY_PlayRest]n"); */ }
#endif

    /* get the duration */
    if (!IMY_GetDuration(pEASData->hwInstData, pData, &duration))
        return EAS_FALSE;

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_PlayRest: note duration %d\n", duration); */ }
#endif

    /* next event is at end of this note */
    pData->time += duration;
    return EAS_TRUE;
}

/*----------------------------------------------------------------------------
 * IMY_GetDuration()
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

static EAS_BOOL IMY_GetDuration (EAS_HW_DATA_HANDLE hwInstData, S_IMELODY_DATA *pData, EAS_I32 *pDuration)
{
    EAS_I32 duration;
    EAS_I8 c;

    /* get the duration */
    *pDuration = 0;
    c = IMY_GetNextChar(hwInstData, pData, EAS_FALSE);
    if (!c)
        return EAS_FALSE;
    if ((c < '0') || (c > '5'))
    {
#ifdef _DEBUG_IMELODY
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_GetDuration: error in duration '%c'\n", c); */ }
#endif
        return EAS_FALSE;
    }

    /* calculate total length of note */
    duration = pData->tick * (1 << ('5' - c));

    /* check for duration modifier */
    c = IMY_GetNextChar(hwInstData, pData, EAS_FALSE);
    if (c)
    {
        if (c == '.')
            /*lint -e{704} shift for performance */
            duration += duration >> 1;
        else if (c == ':')
            /*lint -e{704} shift for performance */
            duration += (duration >> 1) + (duration >> 2);
        else if (c == ';')
            /*lint -e{704} shift for performance */
            duration = (duration * TRIPLET_MULTIPLIER) >> TRIPLET_SHIFT;
        else
            PutBackChar(pData);
    }

    *pDuration = duration;
    return EAS_TRUE;
}

/*----------------------------------------------------------------------------
 * IMY_GetLEDState()
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
static EAS_BOOL IMY_GetLEDState (S_EAS_DATA *pEASData, S_IMELODY_DATA *pData)
{
    EAS_I8 c;
    EAS_INT i;

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Enter IMY_GetLEDState\n"); */ }
#endif

    for (i = 0; i < 5; i++)
    {
        c = IMY_GetNextChar(pEASData->hwInstData, pData, EAS_FALSE);
        if (!c)
            return EAS_FALSE;
        switch (i)
        {
            case 3:
                if (c == 'n')
                {
#ifdef _DEBUG_IMELODY
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_GetLEDState: LED on\n"); */ }
#endif
                    EAS_HWLED(pEASData->hwInstData, EAS_TRUE);
                    return EAS_TRUE;
                }
                else if (c != 'f')
                    return EAS_FALSE;
                break;

            case 4:
                if (c == 'f')
                {
#ifdef _DEBUG_IMELODY
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_GetLEDState: LED off\n"); */ }
#endif
                    EAS_HWLED(pEASData->hwInstData, EAS_FALSE);
                    return EAS_TRUE;
                }
                return EAS_FALSE;

            default:
                if (c != ledStr[i])
                    return EAS_FALSE;
                break;
        }
    }
    return EAS_FALSE;
}

/*----------------------------------------------------------------------------
 * IMY_GetVibeState()
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
static EAS_BOOL IMY_GetVibeState (S_EAS_DATA *pEASData, S_IMELODY_DATA *pData)
{
    EAS_I8 c;
    EAS_INT i;

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Enter IMY_GetVibeState\n"); */ }
#endif

    for (i = 0; i < 6; i++)
    {
        c = IMY_GetNextChar(pEASData->hwInstData, pData, EAS_FALSE);
        if (!c)
            return EAS_FALSE;
        switch (i)
        {
            case 4:
                if (c == 'n')
                {
#ifdef _DEBUG_IMELODY
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_GetVibeState: vibrate on\n"); */ }
#endif
                    EAS_HWVibrate(pEASData->hwInstData, EAS_TRUE);
                    return EAS_TRUE;
                }
                else if (c != 'f')
                    return EAS_FALSE;
                break;

            case 5:
                if (c == 'f')
                {
#ifdef _DEBUG_IMELODY
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_GetVibeState: vibrate off\n"); */ }
#endif
                    EAS_HWVibrate(pEASData->hwInstData, EAS_FALSE);
                    return EAS_TRUE;
                }
                return EAS_FALSE;

            default:
                if (c != vibeStr[i])
                    return EAS_FALSE;
                break;
        }
    }
    return EAS_FALSE;
}

/*----------------------------------------------------------------------------
 * IMY_GetBackState()
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
static EAS_BOOL IMY_GetBackState (S_EAS_DATA *pEASData, S_IMELODY_DATA *pData)
{
    EAS_I8 c;
    EAS_INT i;

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Enter IMY_GetBackState\n"); */ }
#endif

    for (i = 0; i < 5; i++)
    {
        c = IMY_GetNextChar(pEASData->hwInstData, pData, EAS_FALSE);
        if (!c)
            return EAS_FALSE;
        switch (i)
        {
            case 3:
                if (c == 'n')
                {
#ifdef _DEBUG_IMELODY
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_GetBackState: backlight on\n"); */ }
#endif
                    EAS_HWBackLight(pEASData->hwInstData, EAS_TRUE);
                    return EAS_TRUE;
                }
                else if (c != 'f')
                    return EAS_FALSE;
                break;

            case 4:
                if (c == 'f')
                {
#ifdef _DEBUG_IMELODY
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_GetBackState: backlight off\n"); */ }
#endif
                    EAS_HWBackLight(pEASData->hwInstData, EAS_FALSE);
                    return EAS_TRUE;
                }
                return EAS_FALSE;

            default:
                if (c != backStr[i])
                    return EAS_FALSE;
                break;
        }
    }
    return EAS_FALSE;
}

/*----------------------------------------------------------------------------
 * IMY_GetVolume()
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
static EAS_BOOL IMY_GetVolume (EAS_HW_DATA_HANDLE hwInstData, S_IMELODY_DATA *pData, EAS_BOOL inHeader)
{
    EAS_INT temp;
    EAS_I8 c;

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Enter IMY_GetVolume\n"); */ }
#endif

    c = IMY_GetNextChar(hwInstData, pData, inHeader);
    if (c == '+')
    {
        if (pData->volume < 15)
            pData->volume++;
        return EAS_TRUE;
    }
    else if (c == '-')
    {
        if (pData->volume > 0)
            pData->volume--;
        return EAS_TRUE;
    }
    else if (IsDigit(c))
        temp = c - '0';
    else
        return EAS_FALSE;

    c = IMY_GetNextChar(hwInstData, pData, inHeader);
    if (IsDigit(c))
        temp = temp * 10 + c - '0';
    else if (c)
        PutBackChar(pData);
    if ((temp >= 0) && (temp <= 15))
    {
        if (inHeader && (temp == 0))
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Ignoring V0 encountered in header\n"); */ }
        else
            pData->volume = (EAS_U8) temp;
    }
    return EAS_TRUE;
}

/*----------------------------------------------------------------------------
 * IMY_GetNumber()
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
static EAS_BOOL IMY_GetNumber (EAS_HW_DATA_HANDLE hwInstData, S_IMELODY_DATA *pData, EAS_INT *temp, EAS_BOOL inHeader)
{
    EAS_BOOL ok;
    EAS_I8 c;

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Enter IMY_GetNumber\n"); */ }
#endif

    *temp = 0;
    ok = EAS_FALSE;
    for (;;)
    {
        c = IMY_GetNextChar(hwInstData, pData, inHeader);
        if (IsDigit(c))
        {
            *temp = *temp * 10 + c - '0';
            ok = EAS_TRUE;
        }
        else
        {
            if (c)
                PutBackChar(pData);

#ifdef _DEBUG_IMELODY
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_GetNumber: value %d\n", *temp); */ }
#endif

            return ok;
        }
    }
}

/*----------------------------------------------------------------------------
 * IMY_GetVersion()
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
static EAS_BOOL IMY_GetVersion (S_IMELODY_DATA *pData, EAS_INT *pVersion)
{
    EAS_I8 c;
    EAS_INT temp;
    EAS_INT version;

    version = temp = 0;
    for (;;)
    {
        c = pData->buffer[pData->index++];
        if ((c == 0) || (c == '.'))
        {
            /*lint -e{701} use shift for performance */
            version = (version << 8) + temp;
            if (c == 0)
            {

#ifdef _DEBUG_IMELODY
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_GetVersion: version 0x%04x\n", version); */ }
#endif

                *pVersion = version;
                return EAS_TRUE;
            }
            temp = 0;
        }
        else if (IsDigit(c))
            temp = (temp * 10) + c - '0';
    }
}

/*----------------------------------------------------------------------------
 * IMY_MetaData()
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
static void IMY_MetaData (S_IMELODY_DATA *pData, E_EAS_METADATA_TYPE metaType, EAS_I8 *buffer)
{
    EAS_I32 len;

    /* check for callback */
    if (!pData->metadata.callback)
        return;

    /* copy data to host buffer */
    len = (EAS_I32) strlen((char*) buffer);
    if (len >pData->metadata.bufferSize)
        len = pData->metadata.bufferSize;
    strncpy((char*) pData->metadata.buffer, (char*) buffer, (size_t) len);
    pData->metadata.buffer[len] = 0;

    /* callback to host */
    pData->metadata.callback(metaType, pData->metadata.buffer, pData->metadata.pUserData);
}

/*----------------------------------------------------------------------------
 * IMY_ParseHeader()
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
static EAS_RESULT IMY_ParseHeader (S_EAS_DATA *pEASData, S_IMELODY_DATA* pData)
{
    EAS_RESULT result;
    EAS_INT token;
    EAS_INT temp;
    EAS_I8 c;

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Enter IMY_ParseHeader\n"); */ }
#endif

    /* initialize some defaults */
    pData->time = 0;
    pData->tick = DEFAULT_TICK_CONV;
    pData->note = 0;
    pData->noteModifier = 0;
    pData ->restTicks = 0;
    pData->volume = 7;
    pData->octave = 60;
    pData->repeatOffset = -1;
    pData->repeatCount = -1;
    pData->style = 0;

    /* force the read of the first line */
    pData->index = 1;

    /* read data until we get to melody */
    for (;;)
    {
        /* read a line from the file and parse the token */
        if (pData->index != 0)
        {
            if ((result = IMY_ReadLine(pEASData->hwInstData, pData->fileHandle, pData->buffer, &pData->startLine)) != EAS_SUCCESS)
            {
#ifdef _DEBUG_IMELODY
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_ParseHeader: IMY_ReadLine returned %d\n", result); */ }
#endif
                return result;
            }
        }
        token = IMY_ParseLine(pData->buffer, &pData->index);

        switch (token)
        {
            /* ignore these valid tokens */
            case TOKEN_BEGIN:
                break;

            case TOKEN_FORMAT:
                if (!IMY_GetVersion(pData, &temp))
                {
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Invalid FORMAT field '%s'\n", pData->buffer); */ }
                    return EAS_ERROR_FILE_FORMAT;
                }
                if ((temp != 0x0100) && (temp != 0x0200))
                {
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Unsupported FORMAT %02x\n", temp); */ }
                    return EAS_ERROR_UNRECOGNIZED_FORMAT;
                }
                break;

            case TOKEN_VERSION:
                if (!IMY_GetVersion(pData, &temp))
                {
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Invalid VERSION field '%s'\n", pData->buffer); */ }
                    return EAS_ERROR_FILE_FORMAT;
                }
                if ((temp != 0x0100) && (temp != 0x0102))
                {
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Unsupported VERSION %02x\n", temp); */ }
                    return EAS_ERROR_UNRECOGNIZED_FORMAT;
                }
                break;

            case TOKEN_NAME:
                IMY_MetaData(pData, EAS_METADATA_TITLE, pData->buffer + pData->index);
                break;

            case TOKEN_COMPOSER:
                IMY_MetaData(pData, EAS_METADATA_AUTHOR, pData->buffer + pData->index);
                break;

            /* handle beat */
            case TOKEN_BEAT:
                IMY_GetNumber(pEASData->hwInstData, pData, &temp, EAS_TRUE);
                if ((temp >= 25) && (temp <= 900))
                    pData->tick = TICK_CONVERT / temp;
                break;

            /* handle style */
            case TOKEN_STYLE:
                c = IMY_GetNextChar(pEASData->hwInstData, pData, EAS_TRUE);
                if (c == 'S')
                    c = IMY_GetNextChar(pEASData->hwInstData, pData, EAS_TRUE);
                if ((c >= '0') && (c <= '2'))
                    pData->style = (EAS_U8) (c - '0');
                else
                {
                    PutBackChar(pData);
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Error in style command: %s\n", pData->buffer); */ }
                }
                break;

            /* handle volume */
            case TOKEN_VOLUME:
                c = IMY_GetNextChar(pEASData->hwInstData, pData, EAS_TRUE);
                if (c != 'V')
                {
                    PutBackChar(pData);
                    if (!IsDigit(c))
                    {
                        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Error in volume command: %s\n", pData->buffer); */ }
                        break;
                    }
                }
                IMY_GetVolume(pEASData->hwInstData, pData, EAS_TRUE);
                break;

            case TOKEN_MELODY:
#ifdef _DEBUG_IMELODY
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Header successfully parsed\n"); */ }
#endif
                return EAS_SUCCESS;

            case TOKEN_END:
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Unexpected END:IMELODY encountered\n"); */ }
                return EAS_ERROR_FILE_FORMAT;

            default:
                /* force a read of the next line */
                pData->index = 1;
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Ignoring unrecognized token in iMelody file: %s\n", pData->buffer); */ }
                break;
        }
    }
}

/*----------------------------------------------------------------------------
 * IMY_GetNextChar()
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
static EAS_I8 IMY_GetNextChar (EAS_HW_DATA_HANDLE hwInstData, S_IMELODY_DATA *pData, EAS_BOOL inHeader)
{
    EAS_I8 c;
    EAS_U8 index;

    for (;;)
    {
        /* get next character */
        c = pData->buffer[pData->index++];

        /* buffer empty, read more */
        if (!c)
        {
            /* don't read the next line in the header */
            if (inHeader)
                return 0;

            pData->index = 0;
            pData->buffer[0] = 0;
            if (IMY_ReadLine(hwInstData, pData->fileHandle, pData->buffer, &pData->startLine) != EAS_SUCCESS)
            {
#ifdef _DEBUG_IMELODY
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_GetNextChar: EOF\n"); */ }
#endif
                return 0;
            }

            /* check for END:IMELODY token */
            if (IMY_ParseLine(pData->buffer, &index) == TOKEN_END)
            {
#ifdef _DEBUG_IMELODY
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_GetNextChar: found END:IMELODY\n"); */ }
#endif
                pData->buffer[0] = 0;
                return 0;
            }
            continue;
        }

        /* ignore white space */
        if (!IsSpace(c))
        {

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_GetNextChar returned '%c'\n", c); */ }
#endif
            return c;
        }
    }
}

/*----------------------------------------------------------------------------
 * IMY_ReadLine()
 *----------------------------------------------------------------------------
 * Purpose:
 * Reads a line of input from the file, discarding the CR/LF
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
static EAS_RESULT IMY_ReadLine (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE fileHandle, EAS_I8 *buffer, EAS_I32 *pStartLine)
{
    EAS_RESULT result;
    EAS_INT i;
    EAS_I8 c;

    /* fetch current file position and save it */
    if (pStartLine != NULL)
    {
        if ((result = EAS_HWFilePos(hwInstData, fileHandle, pStartLine)) != EAS_SUCCESS)
        {
#ifdef _DEBUG_IMELODY
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_ParseHeader: EAS_HWFilePos returned %d\n", result); */ }
#endif
            return result;
        }
    }

    buffer[0] = 0;
    for (i = 0; i < MAX_LINE_SIZE; )
    {
        if ((result = EAS_HWGetByte(hwInstData, fileHandle, &c)) != EAS_SUCCESS)
        {
            if ((result == EAS_EOF) && (i > 0))
                break;
            return result;
        }

        /* return on LF or end of data */
        if (c == '\n')
            break;

        /* store characters in buffer */
        if (c != '\r')
            buffer[i++] = c;
    }
    buffer[i] = 0;

#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_ReadLine read %s\n", buffer); */ }
#endif

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * IMY_ParseLine()
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
static EAS_INT IMY_ParseLine (EAS_I8 *buffer, EAS_U8 *pIndex)
{
    EAS_INT i;
    EAS_INT j;

    /* there's no strnicmp() in stdlib, so we have to roll our own */
    for (i = 0; i < TOKEN_INVALID; i++)
    {
        for (j = 0; ; j++)
        {
            /* end of token, must be a match */
            if (tokens[i][j] == 0)
            {
#ifdef _DEBUG_IMELODY
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_ParseLine found token %d\n", i); */ }
#endif
                *pIndex = (EAS_U8) j;
                return i;
            }
            if (tokens[i][j] != ToUpper(buffer[j]))
                break;
        }
    }
#ifdef _DEBUG_IMELODY
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMY_ParseLine: no token found\n"); */ }
#endif
    return TOKEN_INVALID;
}

