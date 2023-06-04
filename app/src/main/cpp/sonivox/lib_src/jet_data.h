/*----------------------------------------------------------------------------
 *
 * File:
 * jet_data.h
 *
 * Contents and purpose:
 * Internal data structures and interfaces for JET
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

#ifndef _JET_DATA_H
#define _JET_DATA_H

#include "eas.h"
#include "jet.h"

/* maximum number of segments allowed in a JET file */
#ifndef JET_MAX_SEGMENTS
#define JET_MAX_SEGMENTS            32
#endif

/* maximum number of DLS collections allowed in a JET file */
#ifndef JET_MAX_DLS_COLLECTIONS
#define JET_MAX_DLS_COLLECTIONS     4
#endif

/* maximum number of JET events in internal queue */
#ifndef JET_EVENT_QUEUE_SIZE
#define JET_EVENT_QUEUE_SIZE        32
#endif

/* maximum number of JET events in application queue */
#ifndef APP_EVENT_QUEUE_SIZE
#define APP_EVENT_QUEUE_SIZE        32
#endif

/* maximum number of active mute events */
#ifndef JET_MUTE_QUEUE_SIZE
#define JET_MUTE_QUEUE_SIZE         8
#endif

/*----------------------------------------------------------------------------
 * JET event definitions
 *----------------------------------------------------------------------------
*/
#define JET_EVENT_APP_LOW           80
#define JET_EVENT_APP_HIGH          83
#define JET_EVENT_LOW               102
#define JET_EVENT_HIGH              119
#define JET_EVENT_MARKER            102
#define JET_EVENT_TRIGGER_CLIP      103

#define JET_MARKER_LOOP_END         0

#define JET_CLIP_ACTIVE_FLAG        0x80
#define JET_CLIP_TRIGGER_FLAG       0x40
#define JET_CLIP_ID_MASK            0x3f

/*----------------------------------------------------------------------------
 * JET file definitions
 *----------------------------------------------------------------------------
*/
#define JET_TAG(a,b,c,d) (\
    ( ((EAS_U32)(a) & 0xFF) << 24 ) \
    + ( ((EAS_U32)(b) & 0xFF) << 16 ) \
    + ( ((EAS_U32)(c) & 0xFF) <<  8 ) \
    + ( ((EAS_U32)(d) & 0xFF)))

#define JET_VERSION 0x01000000
#define JET_HEADER_TAG JET_TAG('J','E','T',' ')
#define JET_INFO_CHUNK JET_TAG('J','I','N','F')
#define JET_SMF_CHUNK JET_TAG('J','S','M','F')
#define JET_DLS_CHUNK JET_TAG('J','D','L','S')
#define INFO_JET_COPYRIGHT JET_TAG('J','C','O','P')
#define JET_APP_DATA_CHUNK JET_TAG('J','A','P','P')

#define INFO_NUM_SMF_CHUNKS JET_TAG('S','M','F','#')
#define INFO_NUM_DLS_CHUNKS JET_TAG('D','L','S','#')
#define INFO_JET_VERSION JET_TAG('J','V','E','R')

/*----------------------------------------------------------------------------
 * S_JET_SEGMENT
 *
 * JET segment data
 *----------------------------------------------------------------------------
*/
typedef struct s_jet_segment_tag
{
    EAS_HANDLE          streamHandle;
    EAS_U32             muteFlags;
    EAS_I16             repeatCount;
    EAS_U8              userID;
    EAS_I8              transpose;
    EAS_I8              libNum;
    EAS_U8              state;
    EAS_U8              flags;
} S_JET_SEGMENT;

/* S_JET_SEGMENT.state */
typedef enum
{
    JET_STATE_CLOSED,
    JET_STATE_OPEN,
    JET_STATE_READY,
    JET_STATE_PLAYING,
    JET_STATE_PAUSED,
    JET_STATE_STOPPING
} E_JET_SEGMENT_STATE;

/* S_JEG_SEGMENT.flags */
#define JET_SEG_FLAG_MUTE_UPDATE        0x01

/*----------------------------------------------------------------------------
 * S_JET_DATA
 *
 * Main JET data structure
 *----------------------------------------------------------------------------
*/
#define SEG_QUEUE_DEPTH 3
typedef struct s_jet_data_tag
{
    EAS_FILE_HANDLE     jetFileHandle;
    S_JET_SEGMENT       segQueue[SEG_QUEUE_DEPTH];
    EAS_I32             segmentOffsets[JET_MAX_SEGMENTS];
    EAS_I32             appDataOffset;
    EAS_I32             appDataSize;
    EAS_DLSLIB_HANDLE   libHandles[JET_MAX_DLS_COLLECTIONS];
    EAS_U32             jetEventQueue[JET_EVENT_QUEUE_SIZE];
    EAS_U32             appEventQueue[APP_EVENT_QUEUE_SIZE];
    S_JET_CONFIG        config;
    EAS_U32             segmentTime;
    EAS_U8              muteQueue[JET_MUTE_QUEUE_SIZE];
    EAS_U8              numSegments;
    EAS_U8              numLibraries;
    EAS_U8              flags;
    EAS_U8              playSegment;
    EAS_U8              queueSegment;
    EAS_U8              numQueuedSegments;
    EAS_U8              jetEventQueueRead;
    EAS_U8              jetEventQueueWrite;
    EAS_U8              appEventQueueRead;
    EAS_U8              appEventQueueWrite;
} S_JET_DATA;

/* flags for S_JET_DATA.flags */
#define JET_FLAGS_PLAYING       1

#define JET_EVENT_VAL_MASK      0x0000007f  /* mask for value */
#define JET_EVENT_CTRL_MASK     0x00003f80  /* mask for controller */
#define JET_EVENT_CHAN_MASK     0x0003c000  /* mask for channel */
#define JET_EVENT_TRACK_MASK    0x00fc0000  /* mask for track number */
#define JET_EVENT_SEG_MASK      0xff000000  /* mask for segment ID */
#define JET_EVENT_CTRL_SHIFT    7           /* shift for controller number */
#define JET_EVENT_CHAN_SHIFT    14          /* shift to for MIDI channel */
#define JET_EVENT_TRACK_SHIFT   18          /* shift to get track ID to bit 0 */
#define JET_EVENT_SEG_SHIFT     24          /* shift to get segment ID to bit 0 */

/* prototype for callback function */
extern void JET_Event (EAS_DATA_HANDLE easHandle, EAS_U32 segTrack, EAS_U8 channel, EAS_U8 controller, EAS_U8 value);

/* prototype for JET render function */
extern EAS_PUBLIC EAS_RESULT JET_Process (EAS_DATA_HANDLE easHandle);

#endif

