/*----------------------------------------------------------------------------
 *
 * File:
 * eas_config.c
 *
 * Contents and purpose:
 * This file contains the Configuration Module interface (CM). The CM
 * is a module compiled external to the library that sets the configuration
 * for this build. It allows the library to find optional components and
 * links to static memory allocations (when used in a static configuration).
 *
 * DO NOT MODIFY THIS FILE!
 *
 * NOTE: This module is not intended to be modified by the customer. It
 * needs to be included in the build process with the correct configuration
 * defines (see the library documentation for information on how to configure
 * the library).
 *
 * Copyright Sonic Network Inc. 2004-2006

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
 *   $Revision: 796 $
 *   $Date: 2007-08-01 00:15:25 -0700 (Wed, 01 Aug 2007) $
 *----------------------------------------------------------------------------
*/

#include "eas.h"
#include "eas_config.h"


#ifdef _MFI_PARSER
/*----------------------------------------------------------------------------
 * Vendor/Device ID for MFi Extensions
 *
 * Define the preprocessor symbols to establish the vendor ID and
 * device ID for the MFi PCM/ADPCM extensions.
 *----------------------------------------------------------------------------
*/
const EAS_U8 eas_MFIVendorIDMSB = (MFI_VENDOR_ID >> 8) & 0xff;
const EAS_U8 eas_MFIVendorIDLSB = MFI_VENDOR_ID & 0xff;
const EAS_U8 eas_MFIDeviceID = MFI_DEVICE_ID;
#endif

/*----------------------------------------------------------------------------
 *
 * parserModules
 *
 * This structure is used by the EAS library to locate file parsing
 * modules.
 *----------------------------------------------------------------------------
*/

/* define the external file parsers */
extern EAS_VOID_PTR EAS_SMF_Parser;

#ifdef _XMF_PARSER
extern EAS_VOID_PTR EAS_XMF_Parser;
#endif

#ifdef _SMAF_PARSER
extern EAS_VOID_PTR EAS_SMAF_Parser;
#endif

#ifdef _WAVE_PARSER
extern EAS_VOID_PTR EAS_Wave_Parser;
#endif

#ifdef _OTA_PARSER
extern EAS_VOID_PTR EAS_OTA_Parser;
#endif

#ifdef _IMELODY_PARSER
extern EAS_VOID_PTR EAS_iMelody_Parser;
#endif

#ifdef _RTTTL_PARSER
extern EAS_VOID_PTR EAS_RTTTL_Parser;
#endif

#if defined (_CMX_PARSER) || defined(_MFI_PARSER)
extern EAS_VOID_PTR EAS_CMF_Parser;
#endif

/* initalize pointers to parser interfaces */
/*lint -e{605} not pretty, but it works */
EAS_VOID_PTR const parserModules[] =
{
    &EAS_SMF_Parser,

#ifdef _XMF_PARSER
    &EAS_XMF_Parser,
#endif

#ifdef _WAVE_PARSER
    &EAS_Wave_Parser,
#endif

#ifdef _SMAF_PARSER
    &EAS_SMAF_Parser,
#endif

#ifdef _OTA_PARSER
    &EAS_OTA_Parser,
#endif

#ifdef _IMELODY_PARSER
    &EAS_iMelody_Parser,
#endif

#ifdef _RTTTL_PARSER
    &EAS_RTTTL_Parser,
#endif

#if defined (_CMX_PARSER) || defined(_MFI_PARSER)
    &EAS_CMF_Parser
#endif
};
#define NUM_PARSER_MODULES (sizeof(parserModules) / sizeof(EAS_VOID_PTR))

/*----------------------------------------------------------------------------
 * Data Modules
 *----------------------------------------------------------------------------
*/

#ifdef _STATIC_MEMORY
extern EAS_VOID_PTR eas_SMFData;
extern EAS_VOID_PTR eas_Data;
extern EAS_VOID_PTR eas_MixBuffer;
extern EAS_VOID_PTR eas_Synth;
extern EAS_VOID_PTR eas_MIDI;
extern EAS_VOID_PTR eas_PCMData;
extern EAS_VOID_PTR eas_MIDIData;

#ifdef _XMF_PARSER
extern EAS_VOID_PTR eas_XMFData;
#endif

#ifdef _SMAF_PARSER
extern EAS_VOID_PTR eas_SMAFData;
#endif

#ifdef _OTA_PARSER
extern EAS_VOID_PTR eas_OTAData;
#endif

#ifdef _IMELODY_PARSER
extern EAS_VOID_PTR eas_iMelodyData;
#endif

#ifdef _RTTTL_PARSER
extern EAS_VOID_PTR eas_RTTTLData;
#endif

#ifdef _WAVE_PARSER
extern EAS_VOID_PTR eas_WaveData;
#endif

#if defined (_CMX_PARSER) || defined(_MFI_PARSER)
extern EAS_VOID_PTR eas_CMFData;
#endif
#endif

/*----------------------------------------------------------------------------
 *
 * Effects Modules
 *
 * These declarations are used by the EAS library to locate
 * effects modules.
 *----------------------------------------------------------------------------
*/

#ifdef _ENHANCER_ENABLED
extern EAS_VOID_PTR                 EAS_Enhancer;
#define EAS_ENHANCER_INTERFACE      &EAS_Enhancer
#ifdef _STATIC_MEMORY
extern EAS_VOID_PTR                 eas_EnhancerData;
#define EAS_ENHANCER_DATA           &eas_EnhancerData
#else
#define EAS_ENHANCER_DATA           NULL
#endif
#else
#define EAS_ENHANCER_INTERFACE      NULL
#define EAS_ENHANCER_DATA           NULL
#endif

#ifdef _COMPRESSOR_ENABLED
extern EAS_VOID_PTR                 EAS_Compressor;
#define EAS_COMPRESSOR_INTERFACE    &EAS_Compressor
#ifdef _STATIC_MEMORY
extern EAS_VOID_PTR                 eas_CompressorData;
#define EAS_COMPRESSOR_DATA         &eas_CompressorData
#else
#define EAS_COMPRESSOR_DATA         NULL
#endif
#else
#define EAS_COMPRESSOR_INTERFACE    NULL
#define EAS_COMPRESSOR_DATA         NULL
#endif

#ifdef _MAXIMIZER_ENABLED
extern EAS_VOID_PTR                 EAS_Maximizer;
#define EAS_MAXIMIZER_INTERFACE     &EAS_Maximizer
#ifdef _STATIC_MEMORY
extern EAS_VOID_PTR                 eas_MaximizerData;
#define EAS_MAXIMIZER_DATA          &eas_MaximizerData
#else
#define EAS_MAXIMIZER_DATA          NULL
#endif
#else
#define EAS_MAXIMIZER_INTERFACE NULL
#define EAS_MAXIMIZER_DATA          NULL
#endif


#ifdef _REVERB_ENABLED
extern EAS_VOID_PTR                 EAS_Reverb;
#define EAS_REVERB_INTERFACE        &EAS_Reverb
#ifdef _STATIC_MEMORY
extern EAS_VOID_PTR                 eas_ReverbData;
#define EAS_REVERB_DATA             &eas_ReverbData
#else
#define EAS_REVERB_DATA             NULL
#endif
#else
#define EAS_REVERB_INTERFACE        NULL
#define EAS_REVERB_DATA             NULL
#endif

#ifdef _CHORUS_ENABLED
extern EAS_VOID_PTR                 EAS_Chorus;
#define EAS_CHORUS_INTERFACE        &EAS_Chorus
#ifdef _STATIC_MEMORY
extern EAS_VOID_PTR                 eas_ChorusData;
#define EAS_CHORUS_DATA             &eas_ChorusData
#else
#define EAS_CHORUS_DATA             NULL
#endif
#else
#define EAS_CHORUS_INTERFACE        NULL
#define EAS_CHORUS_DATA             NULL
#endif

#ifdef _WIDENER_ENABLED
extern EAS_VOID_PTR                 EAS_Widener;
#define EAS_WIDENER_INTERFACE       &EAS_Widener
#ifdef _STATIC_MEMORY
extern EAS_VOID_PTR                 eas_WidenerData;
#define EAS_WIDENER_DATA            &eas_WidenerData
#else
#define EAS_WIDENER_DATA            NULL
#endif
#else
#define EAS_WIDENER_INTERFACE       NULL
#define EAS_WIDENER_DATA            NULL
#endif

#ifdef _GRAPHIC_EQ_ENABLED
extern EAS_VOID_PTR                 EAS_GraphicEQ;
#define EAS_GRAPHIC_EQ_INTERFACE    &EAS_GraphicEQ
#ifdef _STATIC_MEMORY
extern EAS_VOID_PTR                 eas_GraphicEQData;
#define EAS_GRAPHIC_EQ_DATA         &eas_GraphicEQData
#else
#define EAS_GRAPHIC_EQ_DATA         NULL
#endif
#else
#define EAS_GRAPHIC_EQ_INTERFACE    NULL
#define EAS_GRAPHIC_EQ_DATA         NULL
#endif

#ifdef _WOW_ENABLED
extern EAS_VOID_PTR                 EAS_Wow;
#define EAS_WOW_INTERFACE           &EAS_Wow
#ifdef _STATIC_MEMORY
#error "WOW module requires dynamic memory model"
#else
#define EAS_WOW_DATA                NULL
#endif
#else
#define EAS_WOW_INTERFACE           NULL
#define EAS_WOW_DATA                NULL
#endif

#ifdef _TONECONTROLEQ_ENABLED
extern EAS_VOID_PTR                 EAS_ToneControlEQ;
#define EAS_TONECONTROLEQ_INTERFACE &EAS_ToneControlEQ
#ifdef _STATIC_MEMORY
extern EAS_VOID_PTR                 eas_ToneControlEQData;
#define EAS_TONECONTROLEQ_DATA      &eas_ToneControlEQData
#else
#define EAS_TONECONTROLEQ_DATA      NULL
#endif
#else
#define EAS_TONECONTROLEQ_INTERFACE NULL
#define EAS_TONECONTROLEQ_DATA      NULL
#endif

/*lint -e{605} not pretty, but it works */
EAS_VOID_PTR const effectsModules[] =
{
    EAS_ENHANCER_INTERFACE,
    EAS_COMPRESSOR_INTERFACE,
    EAS_REVERB_INTERFACE,
    EAS_CHORUS_INTERFACE,
    EAS_WIDENER_INTERFACE,
    EAS_GRAPHIC_EQ_INTERFACE,
    EAS_WOW_INTERFACE,
    EAS_MAXIMIZER_INTERFACE,
    EAS_TONECONTROLEQ_INTERFACE
};

EAS_VOID_PTR const effectsData[] =
{
    EAS_ENHANCER_DATA,
    EAS_COMPRESSOR_DATA,
    EAS_REVERB_DATA,
    EAS_CHORUS_DATA,
    EAS_WIDENER_DATA,
    EAS_GRAPHIC_EQ_DATA,
    EAS_WOW_DATA,
    EAS_MAXIMIZER_DATA,
    EAS_TONECONTROLEQ_DATA
};

/*----------------------------------------------------------------------------
 *
 * Optional Modules
 *
 * These declarations are used by the EAS library to locate
 * effects modules.
 *----------------------------------------------------------------------------
*/

#ifdef _METRICS_ENABLED
extern EAS_VOID_PTR                 EAS_Metrics;
#define EAS_METRICS_INTERFACE       &EAS_Metrics
#ifdef _STATIC_MEMORY
extern EAS_VOID_PTR                 eas_MetricsData;
#define EAS_METRICS_DATA            &eas_MetricsData
#else
#define EAS_METRICS_DATA            NULL
#endif
#else
#define EAS_METRICS_INTERFACE       NULL
#define EAS_METRICS_DATA            NULL
#endif

#ifdef MMAPI_SUPPORT
extern EAS_VOID_PTR                 EAS_TC_Parser;
#define EAS_TONE_CONTROL_PARSER     &EAS_TC_Parser
#ifdef _STATIC_MEMORY
extern EAS_VOID_PTR                 eas_TCData;
#define EAS_TONE_CONTROL_DATA       &eas_TCData
#else
#define EAS_TONE_CONTROL_DATA       NULL
#endif
#else
#define EAS_TONE_CONTROL_PARSER     NULL
#define EAS_TONE_CONTROL_DATA       NULL
#endif

/*lint -e{605} not pretty, but it works */
EAS_VOID_PTR const optionalModules[] =
{
    EAS_TONE_CONTROL_PARSER,
    EAS_METRICS_INTERFACE
};

EAS_VOID_PTR const optionalData[] =
{
    EAS_TONE_CONTROL_DATA,
    EAS_METRICS_DATA
};

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
EAS_BOOL EAS_CMStaticMemoryModel (void)
{
#ifdef _STATIC_MEMORY
    return EAS_TRUE;
#else
    return EAS_FALSE;
#endif
}

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
EAS_VOID_PTR EAS_CMEnumModules (EAS_INT module)
{

    if (module >= (EAS_INT) NUM_PARSER_MODULES)
        return NULL;
    return parserModules[module];
}

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
/*lint -esym(715, dataModule) used only when _STATIC_MEMORY is defined */
EAS_VOID_PTR EAS_CMEnumData (EAS_INT dataModule)
{

#ifdef _STATIC_MEMORY
    switch (dataModule)
    {

    /* main instance data for synthesizer */
    case EAS_CM_EAS_DATA:
        return &eas_Data;

    /* mix buffer for mix engine */
    case EAS_CM_MIX_BUFFER:
        /*lint -e{545} lint doesn't like this because it sees the underlying type */
        return &eas_MixBuffer;

    /* instance data for synth */
    case EAS_CM_SYNTH_DATA:
        return &eas_Synth;

    /* instance data for MIDI parser */
    case EAS_CM_MIDI_DATA:
        return &eas_MIDI;

    /* instance data for SMF parser */
    case EAS_CM_SMF_DATA:
        return &eas_SMFData;

#ifdef _XMF_PARSER
    /* instance data for XMF parser */
    case EAS_CM_XMF_DATA:
        return &eas_XMFData;
#endif

#ifdef _SMAF_PARSER
    /* instance data for SMAF parser */
    case EAS_CM_SMAF_DATA:
        return &eas_SMAFData;
#endif

    /* instance data for the PCM engine */
    case EAS_CM_PCM_DATA:
        /*lint -e{545} lint doesn't like this because it sees the underlying type */
        return &eas_PCMData;

    case EAS_CM_MIDI_STREAM_DATA:
        return &eas_MIDIData;

#ifdef _OTA_PARSER
    /* instance data for OTA parser */
    case EAS_CM_OTA_DATA:
        return &eas_OTAData;
#endif

#ifdef _IMELODY_PARSER
    /* instance data for iMelody parser */
    case EAS_CM_IMELODY_DATA:
        return &eas_iMelodyData;
#endif

#ifdef _RTTTL_PARSER
    /* instance data for RTTTL parser */
    case EAS_CM_RTTTL_DATA:
        return &eas_RTTTLData;
#endif

#ifdef _WAVE_PARSER
    /* instance data for WAVE parser */
    case EAS_CM_WAVE_DATA:
        return &eas_WaveData;
#endif

#if defined (_CMX_PARSER) || defined(_MFI_PARSER)
    /* instance data for CMF parser */
    case EAS_CM_CMF_DATA:
        return &eas_CMFData;
#endif

    default:
        return NULL;
    }

#else
    return NULL;
#endif
}

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
EAS_VOID_PTR EAS_CMEnumFXModules (EAS_INT module)
{

    if (module >= NUM_EFFECTS_MODULES)
        return NULL;
    return effectsModules[module];
}

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
EAS_VOID_PTR EAS_CMEnumFXData (EAS_INT dataModule)
{

    if (dataModule >= NUM_EFFECTS_MODULES)
        return NULL;
    return effectsData[dataModule];
}

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
EAS_VOID_PTR EAS_CMEnumOptModules (EAS_INT module)
{

    /* sanity check */
    if (module >= NUM_OPTIONAL_MODULES)
        return EAS_FALSE;
    return optionalModules[module];
}

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
EAS_VOID_PTR EAS_CMEnumOptData (EAS_INT dataModule)
{

    if (dataModule >= NUM_OPTIONAL_MODULES)
        return NULL;
    return optionalData[dataModule];
}


