/*----------------------------------------------------------------------------
 *
 * File:
 * eas_parser.h
 *
 * Contents and purpose:
 * Interface declarations for the generic parser interface
 *
 * This header only contains declarations that are specific
 * to this implementation.
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
 *   $Revision: 767 $
 *   $Date: 2007-07-19 13:47:31 -0700 (Thu, 19 Jul 2007) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_PARSER_H
#define _EAS_PARSER_H

#include "eas_types.h"


/* metadata callback */
typedef struct s_metadata_cb_tag
{
    EAS_METADATA_CBFUNC     callback;
    char                    *buffer;
    EAS_VOID_PTR            pUserData;
    EAS_I32                 bufferSize;
} S_METADATA_CB;

/* generic parser interface */
typedef struct
{
    EAS_RESULT (* EAS_CONST pfCheckFileType)(struct s_eas_data_tag *pEASData, EAS_FILE_HANDLE fileHandle, EAS_VOID_PTR *ppHandle, EAS_I32 offset);
    EAS_RESULT (* EAS_CONST pfPrepare)(struct s_eas_data_tag *pEASData, EAS_VOID_PTR pInstData);
    EAS_RESULT (* EAS_CONST pfTime)(struct s_eas_data_tag *pEASData, EAS_VOID_PTR pInstData, EAS_U32 *pTime);
    EAS_RESULT (* EAS_CONST pfEvent)(struct s_eas_data_tag *pEASData, EAS_VOID_PTR pInstData, EAS_INT parseMode);
    EAS_RESULT (* EAS_CONST pfState)(struct s_eas_data_tag *pEASData, EAS_VOID_PTR pInstData, EAS_STATE *pState);
    EAS_RESULT (* EAS_CONST pfClose)(struct s_eas_data_tag *pEASData, EAS_VOID_PTR pInstData);
    EAS_RESULT (* EAS_CONST pfReset)(struct s_eas_data_tag *pEASData, EAS_VOID_PTR pInstData);
    EAS_RESULT (* EAS_CONST pfPause)(struct s_eas_data_tag *pEASData, EAS_VOID_PTR pInstData);
    EAS_RESULT (* EAS_CONST pfResume)(struct s_eas_data_tag *pEASData, EAS_VOID_PTR pInstData);
    EAS_RESULT (* EAS_CONST pfLocate)(struct s_eas_data_tag *pEASData, EAS_VOID_PTR pInstData, EAS_I32 time, EAS_BOOL *pParserLocate);
    EAS_RESULT (* EAS_CONST pfSetData)(struct s_eas_data_tag *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value);
    EAS_RESULT (* EAS_CONST pfGetData)(struct s_eas_data_tag *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue);
    EAS_RESULT (* EAS_CONST pfGetMetaData)(struct s_eas_data_tag *pEASData, EAS_VOID_PTR pInstData, EAS_I32 *pMediaLength);
} S_FILE_PARSER_INTERFACE;

typedef enum
{
    eParserModePlay,
    eParserModeLocate,
    eParserModeMute,
    eParserModeMetaData
} E_PARSE_MODE;

typedef enum
{
    PARSER_DATA_FILE_TYPE,
    PARSER_DATA_PLAYBACK_RATE,
    PARSER_DATA_TRANSPOSITION,
    PARSER_DATA_VOLUME,
    PARSER_DATA_SYNTH_HANDLE,
    PARSER_DATA_METADATA_CB,
    PARSER_DATA_DLS_COLLECTION,
    PARSER_DATA_EAS_LIBRARY,
    PARSER_DATA_POLYPHONY,
    PARSER_DATA_PRIORITY,
    PARSER_DATA_FORMAT,
    PARSER_DATA_MEDIA_LENGTH,
    PARSER_DATA_JET_CB,
    PARSER_DATA_MUTE_FLAGS,
    PARSER_DATA_SET_MUTE,
    PARSER_DATA_CLEAR_MUTE,
    PARSER_DATA_NOTE_COUNT,
    PARSER_DATA_MAX_PCM_STREAMS,
    PARSER_DATA_GAIN_OFFSET,
    PARSER_DATA_PLAY_MODE
} E_PARSER_DATA;

#endif /* #ifndef _EAS_PARSER_H */
