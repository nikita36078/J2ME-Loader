/*----------------------------------------------------------------------------
 *
 * File:
 * jet.c
 *
 * Contents and purpose:
 * Implementation for JET sound engine
 *
 * Copyright (c) 2006 Sonic Network Inc.

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
 *----------------------------------------------------------------------------
 * Revision Control:
 *   $Revision: 563 $
 *   $Date: 2007-02-13 20:26:23 -0800 (Tue, 13 Feb 2007) $
 *----------------------------------------------------------------------------
*/

//#define LOG_NDEBUG 0
#define LOG_TAG "JET_C"

//#define DEBUG_JET

#include "eas_data.h"
#include "eas_smf.h"
#include "jet_data.h"
#include "eas_host.h"
#include "eas_report.h"


/* default configuration */
static const S_JET_CONFIG jetDefaultConfig =
{
    JET_EVENT_APP_LOW,
    JET_EVENT_APP_HIGH
};

/* function prototypes */
extern EAS_RESULT EAS_IntSetStrmParam (S_EAS_DATA *pEASData, EAS_HANDLE pStream, EAS_INT param, EAS_I32 value);
extern EAS_RESULT EAS_OpenJETStream (EAS_DATA_HANDLE pEASData, EAS_FILE_HANDLE fileHandle, EAS_I32 offset, EAS_HANDLE *ppStream);
extern EAS_RESULT DLSParser (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE fileHandle, EAS_I32 offset, EAS_DLSLIB_HANDLE *ppDLS);

/*----------------------------------------------------------------------------
 * JET_ParseEvent()
 *----------------------------------------------------------------------------
 * Returns current status
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC void JET_ParseEvent (EAS_U32 event, S_JET_EVENT *pEvent)
{
    pEvent->segment = (EAS_U8) ((event & JET_EVENT_SEG_MASK) >> JET_EVENT_SEG_SHIFT);
    pEvent->track = (EAS_U8) ((event & JET_EVENT_TRACK_MASK) >> JET_EVENT_TRACK_SHIFT);
    pEvent->channel = (EAS_U8) ((event & JET_EVENT_CHAN_MASK) >> JET_EVENT_CHAN_SHIFT);
    pEvent->controller = (EAS_U8) ((event & JET_EVENT_CTRL_MASK) >> JET_EVENT_CTRL_SHIFT);
    pEvent->value = (EAS_U8) (event & JET_EVENT_VAL_MASK);
}

#ifdef DEBUG_JET
/*----------------------------------------------------------------------------
 * JET_DumpEvent
 *----------------------------------------------------------------------------
 * Advances queue read/write index
 *----------------------------------------------------------------------------
*/
static void JET_DumpEvent (const char *procName, EAS_U32 event)
{
    S_JET_EVENT sEvent;
    JET_ParseEvent(event, &sEvent);
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "%s: SegID=%d, TrkID=%d, channel=%d, ctrl=%d, val=%d\n",
        procName, sEvent.segment, sEvent.track, sEvent.channel, sEvent.controller, sEvent.value); */ }
}
#endif

/*----------------------------------------------------------------------------
 * JET_IncQueueIndex
 *----------------------------------------------------------------------------
 * Advances queue read/write index
 *----------------------------------------------------------------------------
*/
EAS_INLINE EAS_U8 JET_IncQueueIndex (EAS_U8 index, EAS_U8 queueSize)
{
    if (++index == queueSize)
        index = 0;
    return index;
}

/*----------------------------------------------------------------------------
 * JET_WriteQueue
 *----------------------------------------------------------------------------
 * Save event to queue
 *----------------------------------------------------------------------------
*/
EAS_INLINE void JET_WriteQueue (EAS_U32 *pEventQueue, EAS_U8 *pWriteIndex, EAS_U8 readIndex, EAS_U8 queueSize, EAS_U32 event)
{
    EAS_U8 temp;

    /* check for queue overflow */
    temp = JET_IncQueueIndex(*pWriteIndex, queueSize);
    if (temp == readIndex)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "JET_Event: Event queue overflow --- event ignored!\n"); */ }
        return;
    }

    /* save in queue and advance write index */
    pEventQueue[*pWriteIndex] = event;
    *pWriteIndex = temp;
}

/*----------------------------------------------------------------------------
 * JET_ReadQueue
 *----------------------------------------------------------------------------
 * Read event to queue
 *----------------------------------------------------------------------------
*/
EAS_INLINE EAS_BOOL JET_ReadQueue (EAS_U32 *pEventQueue, EAS_U8 *pReadIndex, EAS_U8 writeIndex, EAS_U8 queueSize, EAS_U32 *pEvent)
{

    /* check for empty queue */
    if (*pReadIndex == writeIndex)
        return EAS_FALSE;

    /* save in queue and advance write index */
    *pEvent = pEventQueue[*pReadIndex];
    *pReadIndex = JET_IncQueueIndex(*pReadIndex, queueSize);
    return EAS_TRUE;
}

/*----------------------------------------------------------------------------
 * JET_NextSegment
 *----------------------------------------------------------------------------
 * Advances segment number
 *----------------------------------------------------------------------------
*/
EAS_INLINE EAS_INT JET_NextSegment (EAS_INT seg_num)
{
    if (++seg_num == SEG_QUEUE_DEPTH)
        seg_num = 0;
    return seg_num;
}

/*----------------------------------------------------------------------------
 * JET_PrepareSegment()
 *----------------------------------------------------------------------------
 * Prepare a segment for playback
 *----------------------------------------------------------------------------
*/
static EAS_RESULT JET_PrepareSegment (EAS_DATA_HANDLE easHandle, EAS_I32 queueNum)
{
    EAS_RESULT result;
    S_JET_SEGMENT *p;

    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "JET_PrepareSegment: %d\n", queueNum); */ }

    p = &easHandle->jetHandle->segQueue[queueNum];
    result = EAS_Prepare(easHandle, p->streamHandle);
    if (result != EAS_SUCCESS)
        return result;

    /* pause segment - must be triggered by play or end of previous segment */
    result = EAS_Pause(easHandle, p->streamHandle);
    if (result != EAS_SUCCESS)
        return result;
    p->state = JET_STATE_READY;

    /* set calback data */
    result = EAS_IntSetStrmParam(easHandle, p->streamHandle, PARSER_DATA_JET_CB, queueNum);
    if (result != EAS_SUCCESS)
        return result;

    /* set DLS collection */
    if (p->libNum >= 0)
    {
        result = EAS_IntSetStrmParam(easHandle, p->streamHandle,
            PARSER_DATA_DLS_COLLECTION, (EAS_I32) easHandle->jetHandle->libHandles[p->libNum]);
        if (result != EAS_SUCCESS)
            return result;
    }

    /* set transposition */
    if (p->transpose)
    {
        result = EAS_SetTransposition(easHandle, p->streamHandle, p->transpose);
        if (result != EAS_SUCCESS)
            return result;
    }

    return result;
}

/*----------------------------------------------------------------------------
 * JET_StartPlayback()
 *----------------------------------------------------------------------------
 * Start segment playback
 *----------------------------------------------------------------------------
*/
static EAS_RESULT JET_StartPlayback (EAS_DATA_HANDLE easHandle, EAS_I32 queueNum)
{
    EAS_RESULT result = EAS_SUCCESS;
    S_JET_SEGMENT *pSeg;

    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "JET_StartPlayback %d\n", queueNum); */ }

    /* if next segment is queued, start playback */
    pSeg = &easHandle->jetHandle->segQueue[queueNum];
    if (pSeg->streamHandle != NULL)
    {
        result = EAS_Resume(easHandle, pSeg->streamHandle);
        easHandle->jetHandle->segQueue[queueNum].state = JET_STATE_PLAYING;

        /* set mute flags */
        if ((result == EAS_SUCCESS) && (pSeg->muteFlags != 0))
            result = EAS_IntSetStrmParam(easHandle, pSeg->streamHandle, PARSER_DATA_MUTE_FLAGS, (EAS_I32) pSeg->muteFlags);
    }
    return result;
}

/*----------------------------------------------------------------------------
 * JET_CloseSegment
 *----------------------------------------------------------------------------
 * Closes stream associated with a segment
 *----------------------------------------------------------------------------
*/
EAS_INLINE EAS_INT JET_CloseSegment (EAS_DATA_HANDLE easHandle, EAS_INT queueNum)
{
    EAS_RESULT result;

    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "JET_CloseSegment %d\n", queueNum); */ }

    /* close the segment */
    result = EAS_CloseFile(easHandle, easHandle->jetHandle->segQueue[queueNum].streamHandle);
    if (result != EAS_SUCCESS)
        return result;

    easHandle->jetHandle->segQueue[queueNum].streamHandle = NULL;
    easHandle->jetHandle->segQueue[queueNum].state = JET_STATE_CLOSED;
    easHandle->jetHandle->numQueuedSegments--;
    return result;
}

/*----------------------------------------------------------------------------
 * JetParseInfoChunk()
 *----------------------------------------------------------------------------
 * Parses the JET info chunk
 *----------------------------------------------------------------------------
*/
static EAS_RESULT JetParseInfoChunk (EAS_DATA_HANDLE easHandle, EAS_I32 pos, EAS_I32 chunkSize)
{
    EAS_RESULT result;
    EAS_U32 infoType;
    EAS_U32 temp;

    /* offset to data */
    result = EAS_HWFileSeek(easHandle->hwInstData, easHandle->jetHandle->jetFileHandle, pos);
    if (result != EAS_SUCCESS)
        return result;

    /* read the entire chunk */
    result = EAS_SUCCESS;
    while ((result == EAS_SUCCESS) && (chunkSize > 0))
    {

        /* get info infoType */
        result = EAS_HWGetDWord(easHandle->hwInstData, easHandle->jetHandle->jetFileHandle, &infoType, EAS_TRUE);
        if (result != EAS_SUCCESS)
            break;

        /* get info field */
        result = EAS_HWGetDWord(easHandle->hwInstData, easHandle->jetHandle->jetFileHandle, &temp, EAS_FALSE);
        if (result == EAS_SUCCESS)

        switch (infoType)
        {
            case INFO_NUM_SMF_CHUNKS:
                if (temp >= JET_MAX_SEGMENTS) {
                    return EAS_ERROR_INCOMPATIBLE_VERSION;
                }
                easHandle->jetHandle->numSegments = (EAS_U8) temp;
                break;

            case INFO_NUM_DLS_CHUNKS:
                if (temp >= JET_MAX_DLS_COLLECTIONS) {
                    return EAS_ERROR_INCOMPATIBLE_VERSION;
                }
                easHandle->jetHandle->numLibraries = (EAS_U8) temp;
                break;

            case INFO_JET_VERSION:
                /* check major version number */
                if ((temp & 0xff000000) != (JET_VERSION & 0xff000000))
                    return EAS_ERROR_INCOMPATIBLE_VERSION;
                break;

            default:
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Ignoring unrecognized JET info type 0x%08x", infoType); */ }
                break;
        }

        chunkSize -= 8;
    }

    /* allocate pointers for chunks to follow */

    return result;
}

/*----------------------------------------------------------------------------
 * JET_OpenFile()
 *----------------------------------------------------------------------------
 * Opens a JET content file for playback
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_OpenFile (EAS_DATA_HANDLE easHandle, EAS_FILE_LOCATOR locator)
{
    EAS_RESULT result;
    EAS_U32 chunkType;
    EAS_I32 pos;
    EAS_I32 chunkSize;
    EAS_INT smfChunkNum;
    EAS_INT dlsChunkNum;
    EAS_I32 dataSize = 0; /* make lint happy */

    /* make sure that we don't have an open file */
    if (easHandle->jetHandle->jetFileHandle != NULL)
        return EAS_ERROR_FILE_ALREADY_OPEN;

    /* open the media file */
    result = EAS_HWOpenFile(easHandle->hwInstData, locator, &easHandle->jetHandle->jetFileHandle, EAS_FILE_READ);
    if (result != EAS_SUCCESS)
        return result;

    /* check header */
    result = EAS_HWGetDWord(easHandle->hwInstData, easHandle->jetHandle->jetFileHandle, &chunkType, EAS_TRUE);
    if (result == EAS_SUCCESS)
    {
        if (chunkType != JET_HEADER_TAG)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "File is not JET format\n"); */ }
            result = EAS_ERROR_UNRECOGNIZED_FORMAT;
        }
    }
    /* get the file data size */
    if (result == EAS_SUCCESS)
        result = EAS_HWGetDWord(easHandle->hwInstData, easHandle->jetHandle->jetFileHandle, &dataSize, EAS_FALSE);

    /* parse through the file to find contents */
    smfChunkNum = dlsChunkNum = 0;
    pos = chunkSize = 8;
    while ((result == EAS_SUCCESS) && (pos < dataSize))
    {

        /* offset to chunk data */
        result = EAS_HWFileSeek(easHandle->hwInstData, easHandle->jetHandle->jetFileHandle, pos);
        if (result != EAS_SUCCESS)
            break;

        /* get chunk size and type */
        result = EAS_HWGetDWord(easHandle->hwInstData, easHandle->jetHandle->jetFileHandle, &chunkType, EAS_TRUE);
        if (result != EAS_SUCCESS)
            break;

        result = EAS_HWGetDWord(easHandle->hwInstData, easHandle->jetHandle->jetFileHandle, &chunkSize, EAS_FALSE);
        if (result != EAS_SUCCESS)
            break;
        pos += 8;

        switch (chunkType)
        {
            case JET_INFO_CHUNK:
                result = JetParseInfoChunk(easHandle, pos, chunkSize);
                break;

            case JET_SMF_CHUNK:
                if (smfChunkNum < easHandle->jetHandle->numSegments)
                    easHandle->jetHandle->segmentOffsets[smfChunkNum++] = pos;
                else
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Ignoring extraneous SMF chunk"); */ }
                break;

            case JET_DLS_CHUNK:
                if (dlsChunkNum < easHandle->jetHandle->numLibraries)
                    result = DLSParser(easHandle->hwInstData, easHandle->jetHandle->jetFileHandle, pos, &easHandle->jetHandle->libHandles[dlsChunkNum++]);
                else
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Ignoring extraneous DLS chunk"); */ }
                break;

            case JET_APP_DATA_CHUNK:
                easHandle->jetHandle->appDataOffset = pos;
                easHandle->jetHandle->appDataSize = chunkSize;
                break;

            case INFO_JET_COPYRIGHT:
                break;

            default:
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Ignoring unrecognized JET chunk type 0x%08x", chunkType); */ }
                break;
        }

        /* offset to next chunk */
        pos += chunkSize;
    }

    /* close file if something went wrong */
    if (result != EAS_SUCCESS)
        EAS_HWCloseFile(easHandle->hwInstData, easHandle->jetHandle->jetFileHandle);

    return result;
}

/*----------------------------------------------------------------------------
 * JET_GetAppData()
 *----------------------------------------------------------------------------
 * Returns location and size of application data in the JET file
 *----------------------------------------------------------------------------
*/
EAS_RESULT JET_GetAppData (EAS_DATA_HANDLE easHandle, EAS_I32 *pAppDataOffset, EAS_I32 *pAppDataSize)
{

    /* check for app chunk */
    if (easHandle->jetHandle->appDataSize == 0)
    {
        *pAppDataOffset = *pAppDataSize = 0;
        return EAS_FAILURE;
    }

    /* return app data */
    *pAppDataOffset = easHandle->jetHandle->appDataOffset;
    *pAppDataSize = easHandle->jetHandle->appDataSize;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * JET_CloseFile()
 *----------------------------------------------------------------------------
 * Closes a JET content file and releases associated resources
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_CloseFile (EAS_DATA_HANDLE easHandle)
{
    EAS_INT index;
    EAS_RESULT result = EAS_SUCCESS;

    /* close open streams */
    for (index = 0; index < SEG_QUEUE_DEPTH; index++)
    {
        if (easHandle->jetHandle->segQueue[index].streamHandle != NULL)
        {
            result = JET_CloseSegment(easHandle, index);
            if (result != EAS_SUCCESS)
                break;
        }
    }

    /* close the main file handle */
    if ((result == EAS_SUCCESS) && (easHandle->jetHandle->jetFileHandle != NULL))
    {
        result = EAS_HWCloseFile(easHandle->hwInstData, easHandle->jetHandle->jetFileHandle);
        if (result == EAS_SUCCESS)
            easHandle->jetHandle->jetFileHandle = NULL;
    }
    return result;
}

/*----------------------------------------------------------------------------
 * JET_Init()
 *----------------------------------------------------------------------------
 * Initializes the JET library, allocates memory, etc. Call
 * JET_Shutdown to de-allocate memory.
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_Init (EAS_DATA_HANDLE easHandle, const S_JET_CONFIG *pConfig, EAS_INT configSize)
{
    S_JET_DATA *pJet;
    EAS_U8 flags = 0;

    /* sanity check */
    if (easHandle == NULL)
        return EAS_ERROR_HANDLE_INTEGRITY;
    if (easHandle->jetHandle != NULL)
        return EAS_ERROR_FEATURE_ALREADY_ACTIVE;
    if (pConfig == NULL)
        pConfig = &jetDefaultConfig;

    /* allocate the JET data object */
    pJet = EAS_HWMalloc(easHandle->hwInstData, sizeof(S_JET_DATA));
    if (pJet == NULL)
        return EAS_ERROR_MALLOC_FAILED;

    /* initialize JET data structure */
    EAS_HWMemSet(pJet, 0, sizeof(S_JET_DATA));
    easHandle->jetHandle = pJet;
    pJet->flags = flags;

    /* copy config data */
    if (configSize > (EAS_INT) sizeof(S_JET_CONFIG))
        configSize = sizeof(S_JET_CONFIG);
    EAS_HWMemCpy(&pJet->config, pConfig, configSize);
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * JET_Shutdown()
 *----------------------------------------------------------------------------
 * Frees any memory used by the JET library
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_Shutdown (EAS_DATA_HANDLE easHandle)
{
    EAS_RESULT result;
    int i;

    /* close any open files */
    result = JET_CloseFile(easHandle);

    /* free allocated data */
    for(i = 0 ; i < easHandle->jetHandle->numLibraries ; i++) {
        if(easHandle->jetHandle->libHandles[i] != NULL) {
            EAS_HWFree(easHandle->hwInstData, easHandle->jetHandle->libHandles[i]);
            easHandle->jetHandle->libHandles[i] = NULL;
        }
    }

    EAS_HWFree(easHandle->hwInstData, easHandle->jetHandle);
    easHandle->jetHandle = NULL;
    return result;
}

/*----------------------------------------------------------------------------
 * JET_Status()
 *----------------------------------------------------------------------------
 * Returns current status
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_Status (EAS_DATA_HANDLE easHandle, S_JET_STATUS *pStatus)
{
    S_JET_SEGMENT *pSeg;

    pSeg = &easHandle->jetHandle->segQueue[easHandle->jetHandle->playSegment];
    if (pSeg->streamHandle != NULL)
    {
        pStatus->currentUserID = pSeg->userID;
        pStatus->segmentRepeatCount = pSeg->repeatCount;
    }
    else
    {
        pStatus->currentUserID = -1;
        pStatus->segmentRepeatCount = 0;
    }

    pStatus->paused = !(easHandle->jetHandle->flags & JET_FLAGS_PLAYING);
    pStatus->numQueuedSegments = easHandle->jetHandle->numQueuedSegments;
    pStatus->currentPlayingSegment = easHandle->jetHandle->playSegment;
    pStatus->currentQueuedSegment = easHandle->jetHandle->queueSegment;
    if (pSeg->streamHandle != NULL)
    {
        EAS_RESULT result;
        EAS_I32 location ;
        if ((result = EAS_GetLocation(easHandle, pSeg->streamHandle, &location)) == EAS_SUCCESS)
            if(location != 0)
            {
                pStatus->location = location;
            }
    }
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * JET_GetEvent()
 *----------------------------------------------------------------------------
 * Checks for application events
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_BOOL JET_GetEvent (EAS_DATA_HANDLE easHandle, EAS_U32 *pEventRaw, S_JET_EVENT *pEvent)
{
    EAS_U32 jetEvent;
    EAS_BOOL gotEvent;

    /* process event queue */
    gotEvent = JET_ReadQueue(easHandle->jetHandle->appEventQueue,
        &easHandle->jetHandle->appEventQueueRead,
        easHandle->jetHandle->appEventQueueWrite,
        APP_EVENT_QUEUE_SIZE, &jetEvent);

    if (gotEvent)
    {
        if (pEventRaw != NULL)
            *pEventRaw = jetEvent;

        if (pEvent != NULL)
            JET_ParseEvent(jetEvent, pEvent);
    }

    return gotEvent;
}

/*----------------------------------------------------------------------------
 * JET_QueueSegment()
 *----------------------------------------------------------------------------
 * Queue a segment for playback
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_QueueSegment (EAS_DATA_HANDLE easHandle, EAS_INT segmentNum, EAS_INT libNum, EAS_INT repeatCount, EAS_INT transpose, EAS_U32 muteFlags, EAS_U8 userID)
{
    EAS_FILE_HANDLE fileHandle;
    EAS_RESULT result;
    S_JET_SEGMENT *p;

    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "JET_QueueSegment segNum=%d, queue=%d\n", segmentNum, easHandle->jetHandle->queueSegment); */ }

    /* make sure it's a valid segment */
    if (segmentNum >= easHandle->jetHandle->numSegments)
        return EAS_ERROR_PARAMETER_RANGE;

    /* make sure it's a valid DLS */
    if (libNum >= easHandle->jetHandle->numLibraries)
        return EAS_ERROR_PARAMETER_RANGE;

    /* check to see if queue is full */
    p = &easHandle->jetHandle->segQueue[easHandle->jetHandle->queueSegment];
    if (p->streamHandle != NULL)
        return EAS_ERROR_QUEUE_IS_FULL;

    /* initialize data */
    p->userID = userID;
    p->repeatCount = (EAS_I16) repeatCount;
    p->transpose = (EAS_I8) transpose;
    p->libNum = (EAS_I8) libNum;
    p->muteFlags = muteFlags;
    p->state = JET_STATE_CLOSED;

    /* open the file */
    result = EAS_OpenJETStream(easHandle, easHandle->jetHandle->jetFileHandle, easHandle->jetHandle->segmentOffsets[segmentNum], &p->streamHandle);
    if (result != EAS_SUCCESS)
        return result;
    p->state = JET_STATE_OPEN;

    /* if less than SEG_QUEUE_DEPTH segments queued up, prepare file for playback */
    if (++easHandle->jetHandle->numQueuedSegments < SEG_QUEUE_DEPTH)
    {
        result = JET_PrepareSegment(easHandle, easHandle->jetHandle->queueSegment);
        if (result != EAS_SUCCESS)
            return result;
    }

    /* create duplicate file handle */
    result = EAS_HWDupHandle(easHandle->hwInstData, easHandle->jetHandle->jetFileHandle, &fileHandle);
    if (result != EAS_SUCCESS)
        return result;

    easHandle->jetHandle->jetFileHandle = fileHandle;
    easHandle->jetHandle->queueSegment = (EAS_U8) JET_NextSegment(easHandle->jetHandle->queueSegment);
    return result;
}

/*----------------------------------------------------------------------------
 * JET_Play()
 *----------------------------------------------------------------------------
 * Starts playback of the file
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_Play (EAS_DATA_HANDLE easHandle)
{
    EAS_RESULT result;
    EAS_INT index;
    EAS_INT count = 0;

    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "JET_Play\n"); */ }

    /* sanity check */
    if (easHandle->jetHandle->flags & JET_FLAGS_PLAYING)
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;

    /* resume all paused streams */
    for (index = 0; index < SEG_QUEUE_DEPTH; index++)
    {
        if (((index == easHandle->jetHandle->playSegment) && (easHandle->jetHandle->segQueue[index].state == JET_STATE_READY)) ||
            (easHandle->jetHandle->segQueue[index].state == JET_STATE_PAUSED))
        {
            result = JET_StartPlayback(easHandle, index);
            if (result != EAS_SUCCESS)
                return result;
            count++;
        }
    }

    /* if no streams are playing, return error */
    if (!count)
        return EAS_ERROR_QUEUE_IS_EMPTY;

    easHandle->jetHandle->flags |= JET_FLAGS_PLAYING;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * JET_Pause()
 *----------------------------------------------------------------------------
 * Pauses playback of the file
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_Pause (EAS_DATA_HANDLE easHandle)
{
    EAS_RESULT result;
    EAS_INT index;
    EAS_INT count = 0;

    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "JET_Pause\n"); */ }

    /* sanity check */
    if ((easHandle->jetHandle->flags & JET_FLAGS_PLAYING) == 0)
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;

    /* pause all playing streams */
    for (index = 0; index < SEG_QUEUE_DEPTH; index++)
    {
        if (easHandle->jetHandle->segQueue[index].state == JET_STATE_PLAYING)
        {
            result = EAS_Pause(easHandle, easHandle->jetHandle->segQueue[easHandle->jetHandle->playSegment].streamHandle);
            if (result != EAS_SUCCESS)
                return result;
            easHandle->jetHandle->segQueue[easHandle->jetHandle->playSegment].state = JET_STATE_PAUSED;
            count++;
        }
    }

    /* if no streams are paused, return error */
    if (!count)
        return EAS_ERROR_QUEUE_IS_EMPTY;

    easHandle->jetHandle->flags &= ~JET_FLAGS_PLAYING;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * JET_SetMuteFlags()
 *----------------------------------------------------------------------------
 * Change the state of the mute flags
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_SetMuteFlags (EAS_DATA_HANDLE easHandle, EAS_U32 muteFlags, EAS_BOOL sync)
{
    S_JET_SEGMENT *pSeg;

    /* get pointer to current segment */
    pSeg = &easHandle->jetHandle->segQueue[easHandle->jetHandle->playSegment];

    /* unsynchronized mute, set flags and return */
    if (!sync)
    {
        if (pSeg->streamHandle == NULL)
            return EAS_ERROR_QUEUE_IS_EMPTY;
        pSeg->muteFlags = muteFlags;
        return EAS_IntSetStrmParam(easHandle, pSeg->streamHandle, PARSER_DATA_MUTE_FLAGS, (EAS_I32) muteFlags);
    }


    /* check for valid stream state */
    if (pSeg->state == JET_STATE_CLOSED)
        return EAS_ERROR_QUEUE_IS_EMPTY;

    /* save mute flags */
    pSeg->muteFlags = muteFlags;

    /* if repeating segment, set mute update flag */
    if (sync)
        pSeg->flags |= JET_SEG_FLAG_MUTE_UPDATE;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * JET_SetMuteFlag()
 *----------------------------------------------------------------------------
 * Change the state of a single mute flag
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_SetMuteFlag (EAS_DATA_HANDLE easHandle, EAS_INT trackNum, EAS_BOOL muteFlag, EAS_BOOL sync)
{
    S_JET_SEGMENT *pSeg;
    EAS_U32 trackMuteFlag;


    /* setup flag */
    if ((trackNum < 0) || (trackNum > 31))
        return EAS_ERROR_PARAMETER_RANGE;
    trackMuteFlag = (1 << trackNum);

    /* get pointer to current segment */
    pSeg = &easHandle->jetHandle->segQueue[easHandle->jetHandle->playSegment];

    /* unsynchronized mute, set flags and return */
    if (!sync)
    {
        if (pSeg->streamHandle == NULL)
            return EAS_ERROR_QUEUE_IS_EMPTY;
        if (muteFlag)
            pSeg->muteFlags |= trackMuteFlag;
        else
            pSeg->muteFlags &= ~trackMuteFlag;
        return EAS_IntSetStrmParam(easHandle, pSeg->streamHandle, PARSER_DATA_MUTE_FLAGS, (EAS_I32) pSeg->muteFlags);
    }


    /* check for valid stream state */
    if (pSeg->state == JET_STATE_CLOSED)
        return EAS_ERROR_QUEUE_IS_EMPTY;

    /* save mute flags and set mute update flag */
    if (muteFlag)
        pSeg->muteFlags |= trackMuteFlag;
    else
        pSeg->muteFlags &= ~trackMuteFlag;
    pSeg->flags |= JET_SEG_FLAG_MUTE_UPDATE;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * JET_TriggerClip()
 *----------------------------------------------------------------------------
 * Unmute a track and then mute it when it is complete. If a clip
 * is already playing, change mute event to a trigger event. The
 * JET_Event function will not mute the clip, but will allow it
 * to continue playing through the next clip.
 *
 * NOTE: We use bit 7 to indicate an entry in the queue. For a
 * small queue, it is cheaper in both memory and CPU cycles to
 * scan the entire queue for non-zero events than keep enqueue
 * and dequeue indices.
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_TriggerClip (EAS_DATA_HANDLE easHandle, EAS_INT clipID)
{
    EAS_INT i;
    EAS_INT index = -1;

    /* check for valid clipID */
    if ((clipID < 0) || (clipID > 63))
        return EAS_ERROR_PARAMETER_RANGE;

    /* set active flag */
    clipID |= JET_CLIP_ACTIVE_FLAG;

    /* Reverse the search so that we get the first empty element */
    for (i = JET_MUTE_QUEUE_SIZE-1; i >= 0 ; i--)
    {
        if (easHandle->jetHandle->muteQueue[i] == clipID)
        {
            index = i;
            break;
        }
        if (easHandle->jetHandle->muteQueue[i] == 0)
            index = i;
    }
    if (index < 0)
        return EAS_ERROR_QUEUE_IS_FULL;

    easHandle->jetHandle->muteQueue[index] = (EAS_U8) clipID | JET_CLIP_TRIGGER_FLAG;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * JET_Process()
 *----------------------------------------------------------------------------
 * Called during EAS_Render to process stream states
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_Process (EAS_DATA_HANDLE easHandle)
{
    S_JET_SEGMENT *pSeg;
    EAS_STATE state;
    EAS_INT index;
    EAS_INT playIndex;
    EAS_RESULT result = EAS_SUCCESS;
    EAS_BOOL endOfLoop = EAS_FALSE;
    EAS_BOOL startNextSegment = EAS_FALSE;
    EAS_BOOL prepareNextSegment = EAS_FALSE;
    EAS_U32 jetEvent;

    /* process event queue */
    while (JET_ReadQueue(easHandle->jetHandle->jetEventQueue,
        &easHandle->jetHandle->jetEventQueueRead,
        easHandle->jetHandle->jetEventQueueWrite,
        JET_EVENT_QUEUE_SIZE, &jetEvent))
    {
        S_JET_EVENT event;
#ifdef DEBUG_JET
        JET_DumpEvent("JET_Process", jetEvent);
#endif
        JET_ParseEvent(jetEvent, &event);

        /* check for end of loop */
        if ((event.controller == JET_EVENT_MARKER) &&
                (event.value == JET_MARKER_LOOP_END) &&
                (easHandle->jetHandle->segQueue[easHandle->jetHandle->playSegment].streamHandle != NULL))
            endOfLoop = EAS_TRUE;
    }

    /* check state of all streams */
    index = playIndex = easHandle->jetHandle->playSegment;
    for (;;)
    {
        pSeg = &easHandle->jetHandle->segQueue[index];
        if (pSeg->state != JET_STATE_CLOSED)
        {

            /* get playback state */
            result = EAS_State(easHandle, pSeg->streamHandle, &state);
            if (result != EAS_SUCCESS)
                return result;

            /* process state */
            switch (pSeg->state)
            {
                /* take action if this segment is stopping */
                case JET_STATE_PLAYING:
                    if (endOfLoop || (state == EAS_STATE_STOPPING) || (state == EAS_STATE_STOPPED))
                    {
                        /* handle repeats */
                        if (pSeg->repeatCount != 0)
                        {
                            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "JET_Render repeating segment %d\n", index); */ }
                            result = EAS_Locate(easHandle, pSeg->streamHandle, 0, EAS_FALSE);
                            if (result != EAS_SUCCESS)
                                return result;
                            if (pSeg->repeatCount > 0)
                                pSeg->repeatCount--;

                            /* update mute flags if necessary */
                            if (pSeg->flags & JET_SEG_FLAG_MUTE_UPDATE)
                            {
                                result = EAS_IntSetStrmParam(easHandle, pSeg->streamHandle, PARSER_DATA_MUTE_FLAGS, (EAS_I32) pSeg->muteFlags);
                                if (result != EAS_SUCCESS)
                                    return result;
                                pSeg->flags &= ~JET_SEG_FLAG_MUTE_UPDATE;
                            }

                        }
                        /* no repeat, start next segment */
                        else
                        {
                            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "JET_Render stopping queue %d\n", index); */ }
                            startNextSegment = EAS_TRUE;
                            pSeg->state = JET_STATE_STOPPING;
                            easHandle->jetHandle->playSegment = (EAS_U8) JET_NextSegment(index);
                        }
                    }
                    break;

                /* if playback has stopped, close the segment */
                case JET_STATE_STOPPING:
                    if (state == EAS_STATE_STOPPED)
                    {
                        result = JET_CloseSegment(easHandle, index);
                        if (result != EAS_SUCCESS)
                            return result;
                    }
                    break;

                case JET_STATE_READY:
                    if (startNextSegment)
                    {
                        result = JET_StartPlayback(easHandle, index);
                        if (result != EAS_SUCCESS)
                            return result;
                        startNextSegment = EAS_FALSE;
                        prepareNextSegment = EAS_TRUE;
                    }
                    break;

                case JET_STATE_OPEN:
                    if (prepareNextSegment)
                    {
                        result = JET_PrepareSegment(easHandle, index);
                        if (result != EAS_SUCCESS)
                            return result;
                        prepareNextSegment = EAS_FALSE;
                    }
                    break;

                case JET_STATE_PAUSED:
                    break;

                default:
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "JET_Render: Unexpected segment state %d\n", pSeg->state); */ }
                    break;
            }
        }

        /* increment index */
        index = JET_NextSegment(index);
        if (index == playIndex)
            break;
    }

    /* if out of segments, clear playing flag */
    if (easHandle->jetHandle->numQueuedSegments == 0)
        easHandle->jetHandle->flags &= ~JET_FLAGS_PLAYING;

    return result;
}

/*----------------------------------------------------------------------------
 * JET_Event()
 *----------------------------------------------------------------------------
 * Called from MIDI parser when data of interest is received
 *----------------------------------------------------------------------------
*/
void JET_Event (EAS_DATA_HANDLE easHandle, EAS_U32 segTrack, EAS_U8 channel, EAS_U8 controller, EAS_U8 value)
{
    EAS_U32 event;

    if (easHandle->jetHandle == NULL)
        return;

    /* handle triggers */
    if (controller == JET_EVENT_TRIGGER_CLIP)
    {
        S_JET_SEGMENT *pSeg;
        EAS_INT i;
        EAS_U32 muteFlag;

        for (i = 0; i < JET_MUTE_QUEUE_SIZE; i++)
        {
            /* search for event in queue */
            if ((easHandle->jetHandle->muteQueue[i] & JET_CLIP_ID_MASK) == (value & JET_CLIP_ID_MASK))
            {
                /* get segment pointer and mute flag */
                pSeg = &easHandle->jetHandle->segQueue[segTrack >> JET_EVENT_SEG_SHIFT];
                muteFlag = 1 << ((segTrack & JET_EVENT_TRACK_MASK) >> JET_EVENT_TRACK_SHIFT);

                /* un-mute the track */
                if ((easHandle->jetHandle->muteQueue[i] & JET_CLIP_TRIGGER_FLAG) && ((value & 0x40) > 0))
                {
                    pSeg->muteFlags &= ~muteFlag;
                    easHandle->jetHandle->muteQueue[i] &= ~JET_CLIP_TRIGGER_FLAG;
                }

                /* mute the track */
                else
                {
                    EAS_U32 beforeMute ;
                    beforeMute = pSeg->muteFlags ;
                    pSeg->muteFlags |= muteFlag;
                    if (beforeMute != pSeg->muteFlags)
                        easHandle->jetHandle->muteQueue[i] = 0;
                }
                EAS_IntSetStrmParam(easHandle, pSeg->streamHandle, PARSER_DATA_MUTE_FLAGS, (EAS_I32) pSeg->muteFlags);
                return;
            }
        }
        return;
    }

    /* generic event stuff */
    event = (channel << JET_EVENT_CHAN_SHIFT) | (controller << JET_EVENT_CTRL_SHIFT) | value;

    /* write to app queue, translate queue index to segment number */
    if ((controller >= easHandle->jetHandle->config.appEventRangeLow) && (controller <= easHandle->jetHandle->config.appEventRangeHigh))
    {

        event |= easHandle->jetHandle->segQueue[(segTrack & JET_EVENT_SEG_MASK) >> JET_EVENT_SEG_SHIFT].userID << JET_EVENT_SEG_SHIFT;
#ifdef DEBUG_JET
        JET_DumpEvent("JET_Event[app]", event);
#endif
        JET_WriteQueue(easHandle->jetHandle->appEventQueue,
            &easHandle->jetHandle->appEventQueueWrite,
            easHandle->jetHandle->appEventQueueRead,
            APP_EVENT_QUEUE_SIZE,
            event);
    }

    /* write to JET queue */
    else if ((controller >= JET_EVENT_LOW) && (controller <= JET_EVENT_HIGH))
    {
        event |= segTrack;
#ifdef DEBUG_JET
        JET_DumpEvent("JET_Event[jet]", event);
#endif
        JET_WriteQueue(easHandle->jetHandle->jetEventQueue,
            &easHandle->jetHandle->jetEventQueueWrite,
            easHandle->jetHandle->jetEventQueueRead,
            JET_EVENT_QUEUE_SIZE,
            event);
    }
}

/*----------------------------------------------------------------------------
 * JET_Clear_Queue()
 *----------------------------------------------------------------------------
 * Clears the queue and stops play without a complete shutdown
 *----------------------------------------------------------------------------
*/
EAS_RESULT JET_Clear_Queue(EAS_DATA_HANDLE easHandle)
{
    EAS_INT index;
    EAS_RESULT result = EAS_SUCCESS;

    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "JET_Clear_Queue\n"); */ }

    /* pause all playing streams */
    for (index = 0; index < SEG_QUEUE_DEPTH; index++)
    {
        if (easHandle->jetHandle->segQueue[index].state == JET_STATE_PLAYING)
        {
            result = EAS_Pause(easHandle, easHandle->jetHandle->segQueue[index].streamHandle);
            if (result != EAS_SUCCESS)
                return result;

            easHandle->jetHandle->segQueue[index].state = JET_STATE_PAUSED;
        }
    }

    /* close all streams */
    for (index = 0; index < SEG_QUEUE_DEPTH; index++)
    {
        if (easHandle->jetHandle->segQueue[index].streamHandle != NULL)
        {
            result = JET_CloseSegment(easHandle, index);
            if (result != EAS_SUCCESS)
                return result;
        }
    }

    /* clear all clips */
    for (index = 0; index < JET_MUTE_QUEUE_SIZE ; index++)
    {
        easHandle->jetHandle->muteQueue[index] = 0;
    }

    easHandle->jetHandle->flags &= ~JET_FLAGS_PLAYING;
    easHandle->jetHandle->playSegment = easHandle->jetHandle->queueSegment = 0;
    return result;
}

