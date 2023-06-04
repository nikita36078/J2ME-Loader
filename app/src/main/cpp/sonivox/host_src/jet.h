/*----------------------------------------------------------------------------
 *
 * File:
 * jet.h
 *
 * Contents and purpose:
 * Public interface for JET sound engine
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
 *   $Revision: 554 $
 *   $Date: 2007-02-02 11:06:10 -0800 (Fri, 02 Feb 2007) $
 *----------------------------------------------------------------------------
*/

#ifndef _JET_H
#define _JET_H

#include "eas_types.h"
#include "eas.h"

/* for C++ linkage */
#ifdef __cplusplus
extern "C" {
#endif

/* opaque handle types for JET interface */
typedef struct s_jet_data_tag *JET_DATA_HANDLE;

typedef struct s_jet_config_tag
{
    EAS_U8      appEventRangeLow;
    EAS_U8      appEventRangeHigh;
} S_JET_CONFIG;

typedef struct s_jet_status_tag
{
    EAS_INT     currentUserID;
    EAS_INT     segmentRepeatCount;
    EAS_INT     numQueuedSegments;
    EAS_BOOL    paused;
    EAS_I32     location;
    EAS_U8      currentPlayingSegment;
    EAS_U8      currentQueuedSegment;
} S_JET_STATUS;

typedef struct s_jet_event_tag
{
    EAS_U8      segment;
    EAS_U8      channel;
    EAS_U8      track;
    EAS_U8      controller;
    EAS_U8      value;
} S_JET_EVENT;

/*----------------------------------------------------------------------------
 * JET_Init()
 *----------------------------------------------------------------------------
 * Initializes the JET library, allocates memory, etc. Call
 * JET_Shutdown to de-allocate memory. Pass NULL for pConfig
 * to use defaults. If passing config data, configSize should be
 * sizeof(S_JET_CONFIG). This allows for future expansion of the
 * config structure while maintaining compatibility.
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_Init (EAS_DATA_HANDLE easHandle, const S_JET_CONFIG *pConfig, EAS_INT configSize);

/*----------------------------------------------------------------------------
 * JET_Shutdown()
 *----------------------------------------------------------------------------
 * Frees any memory used by the JET library
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_Shutdown (EAS_DATA_HANDLE easHandle);

/*----------------------------------------------------------------------------
 * JET_OpenFile()
 *----------------------------------------------------------------------------
 * Opens a JET content file for playback
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_OpenFile (EAS_DATA_HANDLE easHandle, EAS_FILE_LOCATOR locator);

/*----------------------------------------------------------------------------
 * JET_GetAppData()
 *----------------------------------------------------------------------------
 * Returns location and size of application data in the JET file
 *----------------------------------------------------------------------------
*/
EAS_RESULT JET_GetAppData (EAS_DATA_HANDLE easHandle, EAS_I32 *pAppDataOffset, EAS_I32 *pAppDataSize);

/*----------------------------------------------------------------------------
 * JET_CloseFile()
 *----------------------------------------------------------------------------
 * Closes a JET content file and releases associated resources
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_CloseFile (EAS_DATA_HANDLE easHandle);

/*----------------------------------------------------------------------------
 * JET_Status()
 *----------------------------------------------------------------------------
 * Returns current status
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_Status (EAS_DATA_HANDLE easHandle, S_JET_STATUS *pStatus);

/*----------------------------------------------------------------------------
 * JET_GetEvent()
 *----------------------------------------------------------------------------
 * Checks for application events
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_BOOL JET_GetEvent (EAS_DATA_HANDLE easHandle, EAS_U32 *pEventRaw, S_JET_EVENT *pEvent);

/*----------------------------------------------------------------------------
 * JET_ParseEvent()
 *----------------------------------------------------------------------------
 * Returns current status
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC void JET_ParseEvent (EAS_U32 event, S_JET_EVENT *pEvent);

/*----------------------------------------------------------------------------
 * JET_QueueSegment()
 *----------------------------------------------------------------------------
 * Queue a segment for playback
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_QueueSegment (EAS_DATA_HANDLE easHandle, EAS_INT segmentNum, EAS_INT libNum, EAS_INT repeatCount, EAS_INT transpose, EAS_U32 muteFlags, EAS_U8 userID);

/*----------------------------------------------------------------------------
 * JET_Play()
 *----------------------------------------------------------------------------
 * Starts playback of the file
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_Play (EAS_DATA_HANDLE easHandle);

/*----------------------------------------------------------------------------
 * JET_Pause()
 *----------------------------------------------------------------------------
 * Pauses playback of the file
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_Pause (EAS_DATA_HANDLE easHandle);

/*----------------------------------------------------------------------------
 * JET_SetMuteFlags()
 *----------------------------------------------------------------------------
 * Change the state of the mute flags
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_SetMuteFlags (EAS_DATA_HANDLE easHandle, EAS_U32 muteFlags, EAS_BOOL sync);

/*----------------------------------------------------------------------------
 * JET_SetMuteFlag()
 *----------------------------------------------------------------------------
 * Change the state of a single mute flag
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_SetMuteFlag (EAS_DATA_HANDLE easHandle, EAS_INT trackNum, EAS_BOOL muteFlag, EAS_BOOL sync);

/*----------------------------------------------------------------------------
 * JET_TriggerClip()
 *----------------------------------------------------------------------------
 * Unmute a track and then mute it when it is complete
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_TriggerClip (EAS_DATA_HANDLE easHandle, EAS_INT clipID);

/*----------------------------------------------------------------------------
 * JET_Clear_Queue()
 *----------------------------------------------------------------------------
 * Clears all segments in the queue
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT JET_Clear_Queue (EAS_DATA_HANDLE easHandle);

#ifdef __cplusplus
} /* end extern "C" */
#endif


#endif

