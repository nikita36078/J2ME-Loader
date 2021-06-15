/*----------------------------------------------------------------------------
 *
 * File:
 * eas_effects.h
 *
 * Contents and purpose:
 * Defines a generic effects interface.
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

#ifndef _EAS_EFFECTS_H
#define _EAS_EFFECTS_H

#include "eas_types.h"

typedef struct
{
    EAS_RESULT  (*pfInit)(EAS_DATA_HANDLE pEASData, EAS_VOID_PTR *pInstData);
    void        (*pfProcess)(EAS_VOID_PTR pInstData, EAS_PCM *in, EAS_PCM *out, EAS_I32 numSamples);
    EAS_RESULT  (*pfShutdown)(EAS_DATA_HANDLE pEASData, EAS_VOID_PTR pInstData);
    EAS_RESULT  (*pFGetParam)(EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue);
    EAS_RESULT  (*pFSetParam)(EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value);
} S_EFFECTS_INTERFACE;

typedef struct
{
    EAS_RESULT  (*pfInit)(EAS_DATA_HANDLE pEASData, EAS_VOID_PTR *pInstData);
    void        (*pfProcess)(EAS_VOID_PTR pInstData, EAS_I32 *in, EAS_I32 *out, EAS_I32 numSamples);
    EAS_RESULT  (*pfShutdown)(EAS_DATA_HANDLE pEASData, EAS_VOID_PTR pInstData);
    EAS_RESULT  (*pFGetParam)(EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue);
    EAS_RESULT  (*pFSetParam)(EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value);
} S_EFFECTS32_INTERFACE;

/* mixer instance data */
typedef struct
{
    S_EFFECTS_INTERFACE *effect;
    EAS_VOID_PTR        effectData;
} S_EFFECTS_MODULE;

#endif /* end _EAS_EFFECTS_H */

