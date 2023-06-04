/*----------------------------------------------------------------------------
 *
 * File:
 * eas_smfdata.h
 *
 * Contents and purpose:
 * SMF File Parser
 *
 * This file contains data definitions for the SMF parser.
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
 *   $Revision: 686 $
 *   $Date: 2007-05-03 14:10:54 -0700 (Thu, 03 May 2007) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_SMF_DATA_H
#define _EAS_SMF_DATA_H

#ifndef MAX_SMF_STREAMS
#define MAX_SMF_STREAMS             128
#endif

/* offsets in to the SMF file */
#define SMF_OFS_HEADER_SIZE         4
#define SMF_OFS_FILE_TYPE           8
#define SMF_OFS_NUM_TRACKS          10

/* size of chunk info (chunk ID + chunk size) */
#define SMF_CHUNK_INFO_SIZE         8

/* 'MTrk' track chunk ID */
#define SMF_CHUNK_TYPE_TRACK        0x4d54726bL

/* some useful meta-events */
#define SMF_META_TEXT               0x01
#define SMF_META_COPYRIGHT          0x02
#define SMF_META_SEQTRK_NAME        0x03
#define SMF_META_LYRIC              0x05
#define SMF_META_END_OF_TRACK       0x2f
#define SMF_META_TEMPO              0x51
#define SMF_META_TIME_SIGNATURE     0x58

/* default timebase (120BPM) */
#define SMF_DEFAULT_TIMEBASE        500000L

/* value for pSMFStream->ticks to signify end of track */
#define SMF_END_OF_TRACK            0xffffffff

#endif

