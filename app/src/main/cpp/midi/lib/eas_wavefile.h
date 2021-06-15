/*----------------------------------------------------------------------------
 *
 * File:
 * eas_wavefile.h
 *
 * Contents and purpose:
 * Static data block for wave file parser.
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
 *   $Revision: 439 $
 *   $Date: 2006-10-26 11:53:18 -0700 (Thu, 26 Oct 2006) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_WAVEFILE_H
#define _EAS_WAVEFILE_H

#include "eas_data.h"
#include "eas_pcm.h"

/*----------------------------------------------------------------------------
 *
 * S_WAVE_STATE
 *
 * This structure contains the WAVE file parser state information
 *----------------------------------------------------------------------------
*/
typedef struct s_wave_state_tag
{
    EAS_FILE_HANDLE fileHandle;
    EAS_PCM_HANDLE  streamHandle;
    S_METADATA_CB   metadata;
    EAS_U32         time;
    EAS_I32         fileOffset;
    EAS_I32         audioOffset;
    EAS_I32         mediaLength;
    EAS_U32         audioSize;
    EAS_U32         flags;
    EAS_I16         fileType;
#ifdef MMAPI_SUPPORT
    EAS_VOID_PTR    fmtChunk;
#endif
    EAS_I32         infoChunkPos;
    EAS_I32         infoChunkSize;
} S_WAVE_STATE;

#endif

