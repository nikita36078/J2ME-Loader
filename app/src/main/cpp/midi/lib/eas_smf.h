/*----------------------------------------------------------------------------
 *
 * File:
 * eas_smf.h
 *
 * Contents and purpose:
 * SMF Type 0 and 1 File Parser
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
 *   $Revision: 82 $
 *   $Date: 2006-07-10 11:45:19 -0700 (Mon, 10 Jul 2006) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_SMF_H
#define _EAS_SMF_H

/* prototypes for private interface to SMF parser */
EAS_RESULT SMF_CheckFileType (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, EAS_VOID_PTR *ppHandle, EAS_I32 offset);
EAS_RESULT SMF_Prepare (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
EAS_RESULT SMF_Time (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_U32 *pTime);
EAS_RESULT SMF_Event (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_INT parserMode);
EAS_RESULT SMF_State (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_STATE *pState);
EAS_RESULT SMF_Close (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
EAS_RESULT SMF_Reset (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
EAS_RESULT SMF_Pause (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
EAS_RESULT SMF_Resume (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
EAS_RESULT SMF_SetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value);
EAS_RESULT SMF_GetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue);
EAS_RESULT SMF_ParseHeader (EAS_HW_DATA_HANDLE hwInstData, S_SMF_DATA *pSMFData);

#endif /* end _EAS_SMF_H */


