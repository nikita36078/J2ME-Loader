/*----------------------------------------------------------------------------
 *
 * File:
 * eas_xmfdata.h
 *
 * Contents and purpose:
 * Contains declarations for the XMF file parser.
 *
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

#ifndef _EAS_XMFDATA_H
#define _EAS_XMFDATA_H

#include "eas_data.h"

/*----------------------------------------------------------------------------
 *
 * S_XMF_DATA
 *
 * This structure contains the instance data required to parse an XMF file.
 *
 *----------------------------------------------------------------------------
*/

typedef struct
{
    EAS_FILE_HANDLE     fileHandle;
    EAS_I32             fileOffset;
    EAS_VOID_PTR        pSMFData;
    EAS_I32             midiOffset;
    EAS_I32             dlsOffset;
    S_DLS               *pDLS;
} S_XMF_DATA;

#endif
