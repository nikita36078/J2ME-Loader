/*----------------------------------------------------------------------------
 *
 * File:
 * eas_reverb.h
 *
 * Contents and purpose:
 * Contains parameter enumerations for the Reverb effect
 *
 *
 * Copyright Sonic Network Inc. 2006

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
 *   $Revision: 300 $
 *   $Date: 2006-09-11 17:37:20 -0700 (Mon, 11 Sep 2006) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_REVERB_H
#define _EAS_REVERB_H


/* enumerated parameter settings for Reverb effect */
typedef enum
{
    EAS_PARAM_REVERB_BYPASS,
    EAS_PARAM_REVERB_PRESET,
    EAS_PARAM_REVERB_WET,
    EAS_PARAM_REVERB_DRY
} E_REVERB_PARAMS;


typedef enum
{
    EAS_PARAM_REVERB_LARGE_HALL,
    EAS_PARAM_REVERB_HALL,
    EAS_PARAM_REVERB_CHAMBER,
    EAS_PARAM_REVERB_ROOM,
} E_REVERB_PRESETS;


#endif /* _REVERB_H */
