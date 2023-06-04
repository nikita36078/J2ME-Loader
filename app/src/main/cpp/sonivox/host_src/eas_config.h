/*----------------------------------------------------------------------------
 *
 * File:
 * eas_config.h
 *
 * Contents and purpose:
 * This header declares the Configuration Module interface (CM). The CM
 * is a module compiled external to the library that sets the configuration
 * for this build. It allows the library to find optional components and
 * links to static memory allocations (when used in a static configuration).
 *
 * NOTE: This module is not intended to be modified by the customer. It
 * needs to be included in the build process with the correct configuration
 * defines (see the library documentation for information on how to configure
 * the library).
 *
 * DO NOT MODIFY THIS FILE!
 *
 * Copyright 2005 Sonic Network Inc.

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

// sentinel
#ifndef _EAS_CONFIG_H
#define _EAS_CONFIG_H

#include "eas_types.h"

/* list of enumerators for optional modules */
typedef enum {
    EAS_CM_FILE_PARSERS = 1
} E_CM_ENUM_MODULES;

/* list of enumerators for module and memory pointers */
typedef enum {
    EAS_CM_EAS_DATA = 1,
    EAS_CM_MIX_BUFFER,
    EAS_CM_SYNTH_DATA,
    EAS_CM_MIDI_DATA,
    EAS_CM_SMF_DATA,
    EAS_CM_XMF_DATA,
    EAS_CM_SMAF_DATA,
    EAS_CM_PCM_DATA,
    EAS_CM_MIDI_STREAM_DATA,
    EAS_CM_METRICS_DATA,
    EAS_CM_OTA_DATA,
    EAS_CM_IMELODY_DATA,
    EAS_CM_RTTTL_DATA,
    EAS_CM_WAVE_DATA,
    EAS_CM_CMF_DATA
} E_CM_DATA_MODULES;

typedef struct
{
    int maxSMFStreams;
    void *pSMFData;
    void *pSMFStream;
} S_EAS_SMF_PTRS;

typedef struct
{
    int maxSMAFStreams;
    void *pSMAFData;
    void *pSMAFStream;
} S_EAS_SMAF_PTRS;

/*----------------------------------------------------------------------------
 * EAS_CMStaticMemoryModel()
 *----------------------------------------------------------------------------
 * Purpose:
 * This function returns true if EAS has been configured for
 * a static memory model. There are some limitations in the
 * static memory model, see the documentation for more
 * information.
 *
 * Outputs:
 *  returns EAS_TRUE if a module is found
 *----------------------------------------------------------------------------
*/
EAS_BOOL EAS_CMStaticMemoryModel (void);

/*----------------------------------------------------------------------------
 * EAS_CMEnumModules()
 *----------------------------------------------------------------------------
 * Purpose:
 * This function is used to find pointers to optional modules.
 *
 * Inputs:
 *  module          - module number
 *
 * Outputs:
 *  returns a pointer to the module function table or NULL if no module
 *----------------------------------------------------------------------------
*/
EAS_VOID_PTR EAS_CMEnumModules (EAS_INT module);

/*----------------------------------------------------------------------------
 * EAS_CMEnumData()
 *----------------------------------------------------------------------------
 * Purpose:
 * This function is used to find pointers to static memory allocations.
 *
 * Inputs:
 *  dataModule          - enumerated module number
 *
 * Outputs:
 * Returns handle to data or NULL if not found
 *----------------------------------------------------------------------------
*/
EAS_VOID_PTR EAS_CMEnumData (EAS_INT dataModule);

/*----------------------------------------------------------------------------
 * EAS_CMEnumFXModules()
 *----------------------------------------------------------------------------
 * Purpose:
 * This function is used to find pointers to optional effects modules.
 *
 * Inputs:
 *  module          - enumerated module number
 *  pModule         - pointer to module interface
 *
 * Outputs:
 *  Returns pointer to function table or NULL if not found
 *----------------------------------------------------------------------------
*/
EAS_VOID_PTR EAS_CMEnumFXModules (EAS_INT module);

/*----------------------------------------------------------------------------
 * EAS_CMEnumFXData()
 *----------------------------------------------------------------------------
 * Purpose:
 * This function is used to find pointers to static memory allocations.
 *
 * Inputs:
 *  dataModule          - enumerated module number
 *  pData               - pointer to handle variable
 *
 * Outputs:
 * Returns handle to data or NULL if not found
 *----------------------------------------------------------------------------
*/
EAS_VOID_PTR EAS_CMEnumFXData (EAS_INT dataModule);

/*----------------------------------------------------------------------------
 * EAS_CMEnumOptModules()
 *----------------------------------------------------------------------------
 * Purpose:
 * This function is used to find pointers to optional modules.
 *
 * Inputs:
 *  module          - enumerated module number
 *
 * Outputs:
 *  returns pointer to function table or NULL if no module
 *----------------------------------------------------------------------------
*/
EAS_VOID_PTR EAS_CMEnumOptModules (EAS_INT module);

/*----------------------------------------------------------------------------
 * EAS_CMEnumOptData()
 *----------------------------------------------------------------------------
 * Purpose:
 * This function is used to find pointers to static memory allocations.
 *
 * Inputs:
 *  dataModule          - enumerated module number
 *
 * Outputs:
 * Returns handle to data or NULL if not found
 *----------------------------------------------------------------------------
*/
EAS_VOID_PTR EAS_CMEnumOptData (EAS_INT dataModule);

#endif /* end _EAS_CONFIG_H */
