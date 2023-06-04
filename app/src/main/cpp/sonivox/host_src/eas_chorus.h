/*----------------------------------------------------------------------------
 *
 * File:
 * eas_chorus.h
 *
 * Contents and purpose:
 * Contains parameter enumerations for the Chorus effect
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
 *   $Revision: 309 $
 *   $Date: 2006-09-12 18:52:45 -0700 (Tue, 12 Sep 2006) $
 *----------------------------------------------------------------------------
*/

#ifndef EAS_CHORUS_H
#define EAS_CHORUS_H

/* enumerated parameter settings for Chorus effect */
typedef enum
{
    EAS_PARAM_CHORUS_BYPASS,
    EAS_PARAM_CHORUS_PRESET,
    EAS_PARAM_CHORUS_RATE,
    EAS_PARAM_CHORUS_DEPTH,
    EAS_PARAM_CHORUS_LEVEL
} E_CHORUS_PARAMS;

typedef enum
{
    EAS_PARAM_CHORUS_PRESET1,
    EAS_PARAM_CHORUS_PRESET2,
    EAS_PARAM_CHORUS_PRESET3,
    EAS_PARAM_CHORUS_PRESET4
} E_CHORUS_PRESETS;


#endif
