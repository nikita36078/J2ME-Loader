/*----------------------------------------------------------------------------
 *
 * File:
 * eas_public.c
 *
 * Contents and purpose:
 * Contains EAS library public interface
 *
 * Copyright Sonic Network Inc. 2004

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
 *   $Revision: 842 $
 *   $Date: 2007-08-23 14:32:31 -0700 (Thu, 23 Aug 2007) $
 *----------------------------------------------------------------------------
*/

#define LOG_TAG "Sonivox"
#include "util/log.h"

#include "eas_synthcfg.h"
#include "eas.h"
#include "eas_config.h"
#include "eas_host.h"
#include "eas_report.h"
#include "eas_data.h"
#include "eas_parser.h"
#include "eas_pcm.h"
#include "eas_midi.h"
#include "eas_mixer.h"
#include "eas_build.h"
#include "eas_vm_protos.h"
#include "eas_math.h"

#ifdef JET_INTERFACE
#include "jet_data.h"
#endif

#ifdef DLS_SYNTHESIZER
#include "eas_mdls.h"
#endif

/* number of events to parse before calling EAS_HWYield function */
#define YIELD_EVENT_COUNT       10

/*----------------------------------------------------------------------------
 * easLibConfig
 *
 * This structure is available through the EAS public interface to allow
 * the user to check the configuration of the library.
 *----------------------------------------------------------------------------
*/
static const S_EAS_LIB_CONFIG easLibConfig =
{
    LIB_VERSION,
#ifdef _CHECKED_BUILD
    EAS_TRUE,
#else
    EAS_FALSE,
#endif
    MAX_SYNTH_VOICES,
    NUM_OUTPUT_CHANNELS,
    _OUTPUT_SAMPLE_RATE,
    BUFFER_SIZE_IN_MONO_SAMPLES,
#ifdef _FILTER_ENABLED
    EAS_TRUE,
#else
    EAS_FALSE,
#endif
    _BUILD_TIME_,
    _BUILD_VERSION_
};

/* local prototypes */
static EAS_RESULT EAS_ParseEvents (S_EAS_DATA *pEASData, S_EAS_STREAM *pStream, EAS_U32 endTime, EAS_INT parseMode);

/*----------------------------------------------------------------------------
 * EAS_SetStreamParameter
 *----------------------------------------------------------------------------
 * Sets the specified parameter in the stream. Allows access to
 * customizable settings within the individual file parsers.
 *----------------------------------------------------------------------------
 * pEASData         - pointer to EAS persistent data object
 * pStream          - stream handle
 * param            - enumerated parameter (see eas_parser.h)
 * value            - new value
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_SetStreamParameter (S_EAS_DATA *pEASData, EAS_HANDLE pStream, EAS_I32 param, EAS_I32 value)
{
    S_FILE_PARSER_INTERFACE *pParserModule;

    pParserModule = (S_FILE_PARSER_INTERFACE*) pStream->pParserModule;
    if (pParserModule->pfSetData)
        return (*pParserModule->pfSetData)(pEASData, pStream->handle, param, value);
    return EAS_ERROR_FEATURE_NOT_AVAILABLE;
}

/*----------------------------------------------------------------------------
 * EAS_GetStreamParameter
 *----------------------------------------------------------------------------
 * Sets the specified parameter in the stream. Allows access to
 * customizable settings within the individual file parsers.
 *----------------------------------------------------------------------------
 * pEASData         - pointer to EAS persistent data object
 * pStream          - stream handle
 * param            - enumerated parameter (see eas_parser.h)
 * pValue           - pointer to variable to receive current setting
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_GetStreamParameter (S_EAS_DATA *pEASData, EAS_HANDLE pStream, EAS_I32 param, EAS_I32 *pValue)
{
    S_FILE_PARSER_INTERFACE *pParserModule;

    pParserModule = (S_FILE_PARSER_INTERFACE*) pStream->pParserModule;
    if (pParserModule->pfGetData)
        return (*pParserModule->pfGetData)(pEASData, pStream->handle, param, pValue);
    return EAS_ERROR_FEATURE_NOT_AVAILABLE;
}

/*----------------------------------------------------------------------------
 * EAS_StreamReady()
 *----------------------------------------------------------------------------
 * This routine sets common parameters like transpose, volume, etc.
 * First, it attempts to use the parser EAS_SetStreamParameter interface. If that
 * fails, it attempts to get the synth handle from the parser and
 * set the parameter directly on the synth. This eliminates duplicate
 * code in the parser.
 *----------------------------------------------------------------------------
*/
EAS_BOOL EAS_StreamReady (S_EAS_DATA *pEASData, EAS_HANDLE pStream)
{
    S_FILE_PARSER_INTERFACE *pParserModule;
    EAS_STATE state;

    pParserModule = (S_FILE_PARSER_INTERFACE*) pStream->pParserModule;
    if (pParserModule->pfState(pEASData, pStream->handle, &state) != EAS_SUCCESS)
        return EAS_FALSE;
    return (state < EAS_STATE_OPEN);
}

/*----------------------------------------------------------------------------
 * EAS_IntSetStrmParam()
 *----------------------------------------------------------------------------
 * This routine sets common parameters like transpose, volume, etc.
 * First, it attempts to use the parser EAS_SetStreamParameter interface. If that
 * fails, it attempts to get the synth handle from the parser and
 * set the parameter directly on the synth. This eliminates duplicate
 * code in the parser.
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_IntSetStrmParam (S_EAS_DATA *pEASData, EAS_HANDLE pStream, EAS_INT param, EAS_I32 value)
{
    S_SYNTH *pSynth;

    /* try to set the parameter using stream interface */
    if (EAS_SetStreamParameter(pEASData, pStream, param, value) == EAS_SUCCESS)
        return EAS_SUCCESS;

    /* get a pointer to the synth object and set it directly */
    /*lint -e{740} we are cheating by passing a pointer through this interface */
    if (EAS_GetStreamParameter(pEASData, pStream, PARSER_DATA_SYNTH_HANDLE, (EAS_I32*) &pSynth) != EAS_SUCCESS)
        return EAS_ERROR_INVALID_PARAMETER;

    if (pSynth == NULL)
        return EAS_ERROR_INVALID_PARAMETER;

    switch (param)
    {

#ifdef DLS_SYNTHESIZER
        case PARSER_DATA_DLS_COLLECTION:
            {
                EAS_RESULT result = VMSetDLSLib(pSynth, (EAS_DLSLIB_HANDLE) value);
                if (result == EAS_SUCCESS)
                {
                    DLSAddRef((S_DLS*) value);
                    VMInitializeAllChannels(pEASData->pVoiceMgr, pSynth);
                }
                return result;
            }
#endif

        case PARSER_DATA_EAS_LIBRARY:
            return VMSetEASLib(pSynth, (EAS_SNDLIB_HANDLE) value);

        case PARSER_DATA_POLYPHONY:
            return VMSetPolyphony(pEASData->pVoiceMgr, pSynth, value);

        case PARSER_DATA_PRIORITY:
            return VMSetPriority(pEASData->pVoiceMgr, pSynth, value);

        case PARSER_DATA_TRANSPOSITION:
            VMSetTranposition(pSynth, value);
            break;

        case PARSER_DATA_VOLUME:
            VMSetVolume(pSynth, (EAS_U16) value);
            break;

        default:
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Invalid paramter %d in call to EAS_IntSetStrmParam", param); */ }
            return EAS_ERROR_INVALID_PARAMETER;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_IntGetStrmParam()
 *----------------------------------------------------------------------------
 * This routine gets common parameters like transpose, volume, etc.
 * First, it attempts to use the parser EAS_GetStreamParameter interface. If that
 * fails, it attempts to get the synth handle from the parser and
 * get the parameter directly on the synth.
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_IntGetStrmParam (S_EAS_DATA *pEASData, EAS_HANDLE pStream, EAS_INT param, EAS_I32 *pValue)
{
    S_SYNTH *pSynth;

    /* try to set the parameter */
    if (EAS_GetStreamParameter(pEASData, pStream, param, pValue) == EAS_SUCCESS)
        return EAS_SUCCESS;

    /* get a pointer to the synth object and retrieve data directly */
    /*lint -e{740} we are cheating by passing a pointer through this interface */
    if (EAS_GetStreamParameter(pEASData, pStream, PARSER_DATA_SYNTH_HANDLE, (EAS_I32*) &pSynth) != EAS_SUCCESS)
        return EAS_ERROR_INVALID_PARAMETER;

    if (pSynth == NULL)
        return EAS_ERROR_INVALID_PARAMETER;

    switch (param)
    {
        case PARSER_DATA_POLYPHONY:
            return VMGetPolyphony(pEASData->pVoiceMgr, pSynth, pValue);

        case PARSER_DATA_PRIORITY:
            return VMGetPriority(pEASData->pVoiceMgr, pSynth, pValue);

        case PARSER_DATA_TRANSPOSITION:
            VMGetTranposition(pSynth, pValue);
            break;

        case PARSER_DATA_NOTE_COUNT:
            *pValue = VMGetNoteCount(pSynth);
            break;

        default:
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Invalid paramter %d in call to EAS_IntSetStrmParam", param); */ }
            return EAS_ERROR_INVALID_PARAMETER;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_AllocateStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Allocates a stream handle
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
static EAS_INT EAS_AllocateStream (EAS_DATA_HANDLE pEASData)
{
    EAS_INT streamNum;

    /* check for static allocation, only one stream allowed */
    if (pEASData->staticMemoryModel)
    {
        if (pEASData->streams[0].handle != NULL)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Attempt to open multiple streams in static model\n"); */ }
            return -1;
        }
        return 0;
    }

    /* dynamic model */
    for (streamNum = 0; streamNum < MAX_NUMBER_STREAMS; streamNum++)
        if (pEASData->streams[streamNum].handle == NULL)
            break;
    if (streamNum == MAX_NUMBER_STREAMS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Exceeded maximum number of open streams\n"); */ }
        return -1;
    }
    return streamNum;
}

/*----------------------------------------------------------------------------
 * EAS_InitStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Initialize a stream
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
static void EAS_InitStream (S_EAS_STREAM *pStream, EAS_VOID_PTR pParserModule, EAS_VOID_PTR streamHandle)
{
    pStream->pParserModule = pParserModule;
    pStream->handle = streamHandle;
    pStream->time = 0;
    pStream->frameLength = AUDIO_FRAME_LENGTH;
    pStream->repeatCount = 0;
    pStream->volume = DEFAULT_STREAM_VOLUME;
    pStream->streamFlags = 0;
}

/*----------------------------------------------------------------------------
 * EAS_Config()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns a pointer to a structure containing the configuration options
 * in this library build.
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC const S_EAS_LIB_CONFIG *EAS_Config (void)
{
    return &easLibConfig;
}

/*----------------------------------------------------------------------------
 * EAS_Init()
 *----------------------------------------------------------------------------
 * Purpose:
 * Initialize the synthesizer library
 *
 * Inputs:
 *  ppEASData       - pointer to data handle variable for this instance
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_Init (EAS_DATA_HANDLE *ppEASData)
{
    EAS_HW_DATA_HANDLE pHWInstData;
    EAS_RESULT result;
    S_EAS_DATA *pEASData;
    EAS_INT module;
    EAS_BOOL staticMemoryModel;

    /* get the memory model */
    staticMemoryModel = EAS_CMStaticMemoryModel();

    /* initialize the host wrapper interface */
    *ppEASData = NULL;
    if ((result = EAS_HWInit(&pHWInstData)) != EAS_SUCCESS)
        return result;

    /* check Configuration Module for S_EAS_DATA allocation */
    if (staticMemoryModel)
        pEASData = EAS_CMEnumData(EAS_CM_EAS_DATA);
    else
        pEASData = EAS_HWMalloc(pHWInstData, sizeof(S_EAS_DATA));
    if (!pEASData)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_FATAL, "Failed to allocate EAS library memory\n"); */ }
        return EAS_ERROR_MALLOC_FAILED;
    }

    /* initialize some data */
    EAS_HWMemSet(pEASData, 0, sizeof(S_EAS_DATA));
    pEASData->staticMemoryModel = (EAS_BOOL8) staticMemoryModel;
    pEASData->hwInstData = pHWInstData;
    pEASData->renderTime = 0;

    /* set header search flag */
#ifdef FILE_HEADER_SEARCH
    pEASData->searchHeaderFlag = EAS_TRUE;
#endif

    /* initalize parameters */
    EAS_SetVolume(pEASData, NULL, DEFAULT_VOLUME);

#ifdef _METRICS_ENABLED
    /* initalize the metrics module */
    pEASData->pMetricsModule = EAS_CMEnumOptModules(EAS_MODULE_METRICS);
    if (pEASData->pMetricsModule != NULL)
    {
        if ((result = (*pEASData->pMetricsModule->pfInit)(pEASData, &pEASData->pMetricsData)) != EAS_SUCCESS)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Error %ld initializing metrics module\n", result); */ }
            return result;
        }
    }
#endif

    /* initailize the voice manager & synthesizer */
    if ((result = VMInitialize(pEASData)) != EAS_SUCCESS)
        return result;

    /* initialize mix engine */
    if ((result = EAS_MixEngineInit(pEASData)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Error %ld starting up mix engine\n", result); */ }
        return result;
    }

    /* initialize effects modules */
    for (module = 0; module < NUM_EFFECTS_MODULES; module++)
    {
        pEASData->effectsModules[module].effect = EAS_CMEnumFXModules(module);
        if (pEASData->effectsModules[module].effect != NULL)
        {
            if ((result = (*pEASData->effectsModules[module].effect->pfInit)(pEASData, &pEASData->effectsModules[module].effectData)) != EAS_SUCCESS)
            {
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_FATAL, "Initialization of effects module %d returned %d\n", module, result); */ }
                return result;
            }
        }
    }

    /* initialize PCM engine */
    if ((result = EAS_PEInit(pEASData)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_FATAL, "EAS_PEInit failed with error code %ld\n", result); */ }
        return result;
    }

    /* return instance data pointer to host */
    *ppEASData = pEASData;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_Shutdown()
 *----------------------------------------------------------------------------
 * Purpose:
 * Shuts down the library. Deallocates any memory associated with the
 * synthesizer (dynamic memory model only)
 *
 * Inputs:
 *  pEASData        - handle to data for this instance
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_Shutdown (EAS_DATA_HANDLE pEASData)
{
    /* check for NULL handle */
    if (!pEASData)
        return EAS_ERROR_HANDLE_INTEGRITY;

    /* establish pointers */
    EAS_HW_DATA_HANDLE hwInstData = pEASData->hwInstData;

    /* if there are streams open, close them */
    EAS_RESULT reportResult = EAS_SUCCESS;

    EAS_RESULT result;
    EAS_INT i;
    for (i = 0; i < MAX_NUMBER_STREAMS; i++)
    {
        if (pEASData->streams[i].pParserModule && pEASData->streams[i].handle)
        {
            if ((result = (*((S_FILE_PARSER_INTERFACE*)(pEASData->streams[i].pParserModule))->pfClose)(pEASData, pEASData->streams[i].handle)) != EAS_SUCCESS)
            {
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Error %ld shutting down parser module\n", result); */ }
                reportResult = result;
            }
        }
    }

    /* shutdown PCM engine */
    if ((result = EAS_PEShutdown(pEASData)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Error %ld shutting down PCM engine\n", result); */ }
        if (reportResult == EAS_SUCCESS)
            reportResult = result;
    }

    /* shutdown mix engine */
    if ((result = EAS_MixEngineShutdown(pEASData)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Error %ld shutting down mix engine\n", result); */ }
        if (reportResult == EAS_SUCCESS)
            reportResult = result;
    }

    /* shutdown effects modules */
    for (i = 0; i < NUM_EFFECTS_MODULES; i++)
    {
        if (pEASData->effectsModules[i].effect)
        {
            if ((result = (*pEASData->effectsModules[i].effect->pfShutdown)(pEASData, pEASData->effectsModules[i].effectData)) != EAS_SUCCESS)
            {
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_FATAL, "Shutdown of effects module %d returned %d\n", i, result); */ }
                if (reportResult == EAS_SUCCESS)
                    reportResult = result;
            }
        }
    }

    /* shutdown the voice manager & synthesizer */
    VMShutdown(pEASData);

#ifdef _METRICS_ENABLED
    /* shutdown the metrics module */
    if (pEASData->pMetricsModule != NULL)
    {
        if ((result = (*pEASData->pMetricsModule->pfShutdown)(pEASData, pEASData->pMetricsData)) != EAS_SUCCESS)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Error %ld shutting down metrics module\n", result); */ }
            if (reportResult == EAS_SUCCESS)
                reportResult = result;
        }
    }
#endif

    /* release allocated memory */
    if (!pEASData->staticMemoryModel)
        EAS_HWFree(hwInstData, pEASData);

    /* shutdown host wrappers */
    if (hwInstData)
    {
        if ((result = EAS_HWShutdown(hwInstData)) != EAS_SUCCESS)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Error %ld shutting down host wrappers\n", result); */ }
            if (reportResult == EAS_SUCCESS)
                reportResult = result;
        }
    }

    return reportResult;
}

#ifdef JET_INTERFACE
/*----------------------------------------------------------------------------
 * EAS_OpenJETStream()
 *----------------------------------------------------------------------------
 * Private interface for JET to open an SMF stream with an offset
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_OpenJETStream (EAS_DATA_HANDLE pEASData, EAS_FILE_HANDLE fileHandle, EAS_I32 offset, EAS_HANDLE *ppStream)
{
    EAS_RESULT result;
    EAS_VOID_PTR streamHandle;
    S_FILE_PARSER_INTERFACE *pParserModule;
    EAS_INT streamNum;

    /* allocate a stream */
    if ((streamNum = EAS_AllocateStream(pEASData)) < 0)
        return EAS_ERROR_MAX_STREAMS_OPEN;

    /* check Configuration Module for SMF parser */
    *ppStream = NULL;
    streamHandle = NULL;
    pParserModule = (S_FILE_PARSER_INTERFACE *) EAS_CMEnumModules(0);
    if (pParserModule == NULL)
        return EAS_ERROR_UNRECOGNIZED_FORMAT;

    /* see if SMF parser recognizes the file */
    if ((result = (*pParserModule->pfCheckFileType)(pEASData, fileHandle, &streamHandle, offset)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "CheckFileType returned error %ld\n", result); */ }
        return result;
    }

    /* parser recognized the file, return the handle */
    if (streamHandle)
    {
        EAS_InitStream(&pEASData->streams[streamNum], pParserModule, streamHandle);
        *ppStream = &pEASData->streams[streamNum];
        return EAS_SUCCESS;
    }

    return EAS_ERROR_UNRECOGNIZED_FORMAT;
}
#endif

/*----------------------------------------------------------------------------
 * EAS_OpenFile()
 *----------------------------------------------------------------------------
 * Purpose:
 * Opens a file for audio playback.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * pHandle          - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_OpenFile (EAS_DATA_HANDLE pEASData, EAS_FILE_LOCATOR locator, EAS_HANDLE *ppStream)
{
    EAS_RESULT result;
    EAS_FILE_HANDLE fileHandle;
    EAS_VOID_PTR streamHandle;
    S_FILE_PARSER_INTERFACE *pParserModule;
    EAS_INT streamNum;
    EAS_INT moduleNum;

    /* open the file */
    if ((result = EAS_HWOpenFile(pEASData->hwInstData, locator, &fileHandle, EAS_FILE_READ)) != EAS_SUCCESS)
        return result;

    /* allocate a stream */
    if ((streamNum = EAS_AllocateStream(pEASData)) < 0)
    {
        /* Closing the opened file as stream allocation failed */
        EAS_HWCloseFile(pEASData->hwInstData, fileHandle);
        return EAS_ERROR_MAX_STREAMS_OPEN;
    }
    /* check Configuration Module for file parsers */
    pParserModule = NULL;
    *ppStream = NULL;
    streamHandle = NULL;
    for (moduleNum = 0; ; moduleNum++)
    {
        pParserModule = (S_FILE_PARSER_INTERFACE *) EAS_CMEnumModules(moduleNum);
        if (pParserModule == NULL)
            break;

        /* see if this parser recognizes it */
        if ((result = (*pParserModule->pfCheckFileType)(pEASData, fileHandle, &streamHandle, 0L)) != EAS_SUCCESS)
        {
            /* Closing the opened file as file type check failed */
            EAS_HWCloseFile(pEASData->hwInstData, fileHandle);

            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "CheckFileType returned error %ld\n", result); */ }
            return result;
        }

        /* parser recognized the file, return the handle */
        if (streamHandle)
        {

            /* save the parser pointer and file handle */
            EAS_InitStream(&pEASData->streams[streamNum], pParserModule, streamHandle);
            *ppStream = &pEASData->streams[streamNum];
            return EAS_SUCCESS;
        }

        /* rewind the file for the next parser */
        if ((result = EAS_HWFileSeek(pEASData->hwInstData, fileHandle, 0L)) != EAS_SUCCESS)
        {
            /* Closing the opened file as file seek failed */
            EAS_HWCloseFile(pEASData->hwInstData, fileHandle);

            return result;
         }
    }

    /* no parser was able to recognize the file, close it and return an error */
    EAS_HWCloseFile(pEASData->hwInstData, fileHandle);
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "No parser recognized the requested file\n"); */ }
    return EAS_ERROR_UNRECOGNIZED_FORMAT;
}

#ifdef MMAPI_SUPPORT
/*----------------------------------------------------------------------------
 * EAS_MMAPIToneControl()
 *----------------------------------------------------------------------------
 * Purpose:
 * Opens a ToneControl file for audio playback.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * pHandle          - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_MMAPIToneControl (EAS_DATA_HANDLE pEASData, EAS_FILE_LOCATOR locator, EAS_HANDLE *ppStream)
{
    EAS_RESULT result;
    EAS_FILE_HANDLE fileHandle;
    EAS_VOID_PTR streamHandle;
    S_FILE_PARSER_INTERFACE *pParserModule;
    EAS_INT streamNum;

    /* check if the tone control parser is available */
    *ppStream = NULL;
    streamHandle = NULL;
    pParserModule = EAS_CMEnumOptModules(EAS_MODULE_MMAPI_TONE_CONTROL);
    if (pParserModule == NULL)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "EAS_MMAPIToneControl: ToneControl parser not available\n"); */ }
        return EAS_ERROR_FEATURE_NOT_AVAILABLE;
    }

    /* open the file */
    if ((result = EAS_HWOpenFile(pEASData->hwInstData, locator, &fileHandle, EAS_FILE_READ)) != EAS_SUCCESS)
        return result;

    /* allocate a stream */
    if ((streamNum = EAS_AllocateStream(pEASData)) < 0)
        return EAS_ERROR_MAX_STREAMS_OPEN;

    /* see if ToneControl parser recognizes it */
    if ((result = (*pParserModule->pfCheckFileType)(pEASData, fileHandle, &streamHandle, 0L)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "CheckFileType returned error %ld\n", result); */ }
        return result;
    }

    /* parser accepted the file, return the handle */
    if (streamHandle)
    {

        /* save the parser pointer and file handle */
        EAS_InitStream(&pEASData->streams[streamNum], pParserModule, streamHandle);
        *ppStream = &pEASData->streams[streamNum];
        return EAS_SUCCESS;
    }

    /* parser did not recognize the file, close it and return an error */
    EAS_HWCloseFile(pEASData->hwInstData, fileHandle);
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "No parser recognized the requested file\n"); */ }
    return EAS_ERROR_UNRECOGNIZED_FORMAT;
}

/*----------------------------------------------------------------------------
 * EAS_GetWaveFmtChunk
 *----------------------------------------------------------------------------
 * Helper function to retrieve WAVE file fmt chunk for MMAPI
 *----------------------------------------------------------------------------
 * pEASData         - pointer to EAS persistent data object
 * pStream          - stream handle
 * pFmtChunk        - pointer to variable to receive current setting
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetWaveFmtChunk (S_EAS_DATA *pEASData, EAS_HANDLE pStream, EAS_VOID_PTR *ppFmtChunk)
{
    EAS_RESULT result;
    EAS_I32 value;

    if ((result = EAS_GetStreamParameter(pEASData, pStream, PARSER_DATA_FORMAT, &value)) != EAS_SUCCESS)
        return result;
    *ppFmtChunk = (EAS_VOID_PTR) value;
    return EAS_SUCCESS;
}
#endif

/*----------------------------------------------------------------------------
 * EAS_GetFileType
 *----------------------------------------------------------------------------
 * Returns the file type (see eas_types.h for enumerations)
 *----------------------------------------------------------------------------
 * pEASData         - pointer to EAS persistent data object
 * pStream          - stream handle
 * pFileType        - pointer to variable to receive file type
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetFileType (S_EAS_DATA *pEASData, EAS_HANDLE pStream, EAS_I32 *pFileType)
{
    return EAS_GetStreamParameter(pEASData, pStream, PARSER_DATA_FILE_TYPE, pFileType);
}

/*----------------------------------------------------------------------------
 * EAS_Prepare()
 *----------------------------------------------------------------------------
 * Purpose:
 * Prepares the synthesizer to play the file or stream. Parses the first
 * frame of data from the file and arms the synthesizer.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - file or stream handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_Prepare (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream)
{
    S_FILE_PARSER_INTERFACE *pParserModule;
    EAS_STATE state;
    EAS_RESULT result;

    pParserModule = (S_FILE_PARSER_INTERFACE*) pStream->pParserModule;
    if (pParserModule == NULL)
        return EAS_ERROR_FEATURE_NOT_AVAILABLE;

    /* check for valid state */
    result = pParserModule->pfState(pEASData, pStream->handle, &state);
    if (result == EAS_SUCCESS)
    {
        /* prepare the stream */
        if (state == EAS_STATE_OPEN)
        {
            pParserModule = (S_FILE_PARSER_INTERFACE*) pStream->pParserModule;
            result = (*pParserModule->pfPrepare)(pEASData, pStream->handle);

            /* set volume */
            if (result == EAS_SUCCESS)
                result = EAS_SetVolume(pEASData, pStream, pStream->volume);
        }
        else
            result = EAS_ERROR_NOT_VALID_IN_THIS_STATE;

    }

    return result;
}

/*----------------------------------------------------------------------------
 * EAS_Render()
 *----------------------------------------------------------------------------
 * Purpose:
 * Parse the Midi data and render PCM audio data.
 *
 * Inputs:
 *  pEASData        - buffer for internal EAS data
 *  pOut            - output buffer pointer
 *  nNumRequested   - requested num samples to generate
 *  pnNumGenerated  - actual number of samples generated
 *
 * Outputs:
 *  EAS_SUCCESS if PCM data was successfully rendered
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_Render (EAS_DATA_HANDLE pEASData, EAS_PCM *pOut, EAS_I32 numRequested, EAS_I32 *pNumGenerated)
{
    S_FILE_PARSER_INTERFACE *pParserModule;
    EAS_RESULT result;
    EAS_I32 voicesRendered;
    EAS_STATE parserState;
    EAS_INT streamNum;

    /* assume no samples generated and reset workload */
    *pNumGenerated = 0;
    VMInitWorkload(pEASData->pVoiceMgr);

    /* no support for other buffer sizes yet */
    if (numRequested != BUFFER_SIZE_IN_MONO_SAMPLES)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "This library supports only %ld samples in buffer, host requested %ld samples\n",
            (EAS_I32) BUFFER_SIZE_IN_MONO_SAMPLES, numRequested); */ }
        return EAS_BUFFER_SIZE_MISMATCH;
    }

#ifdef _METRICS_ENABLED
    /* start performance counter */
    if (pEASData->pMetricsData)
        (*pEASData->pMetricsModule->pfStartTimer)(pEASData->pMetricsData, EAS_PM_TOTAL_TIME);
#endif

    /* prep the frame buffer, do mix engine prep only if TRUE */
#ifdef _SPLIT_ARCHITECTURE
    if (VMStartFrame(pEASData))
        EAS_MixEnginePrep(pEASData, numRequested);
#else
    /* prep the mix engine */
    EAS_MixEnginePrep(pEASData, numRequested);
#endif

    /* save the output buffer pointer */
    pEASData->pOutputAudioBuffer = pOut;


#ifdef _METRICS_ENABLED
        /* start performance counter */
        if (pEASData->pMetricsData)
            (*pEASData->pMetricsModule->pfStartTimer)(pEASData->pMetricsData, EAS_PM_PARSE_TIME);
#endif

    /* if we haven't finished parsing from last time, do it now */
    /* need to parse another frame of events before we render again */
    for (streamNum = 0; streamNum < MAX_NUMBER_STREAMS; streamNum++)
    {
        /* clear the locate flag */
        pEASData->streams[streamNum].streamFlags &= ~STREAM_FLAGS_LOCATE;

        if (pEASData->streams[streamNum].pParserModule)
        {

            /* establish pointer to parser module */
            pParserModule = pEASData->streams[streamNum].pParserModule;

#ifdef JET_INTERFACE
            /* handle pause */
            if (pEASData->streams[streamNum].streamFlags & STREAM_FLAGS_PAUSE)
            {
                if (pParserModule->pfPause)
                    result = pParserModule->pfPause(pEASData, pEASData->streams[streamNum].handle);
                pEASData->streams[streamNum].streamFlags &= ~STREAM_FLAGS_PAUSE;
            }
#endif

            /* get current state */
            if ((result = (*pParserModule->pfState)(pEASData, pEASData->streams[streamNum].handle, &parserState)) != EAS_SUCCESS)
                return result;

#ifdef JET_INTERFACE
            /* handle resume */
            if (parserState == EAS_STATE_PAUSED)
            {
                if (pEASData->streams[streamNum].streamFlags & STREAM_FLAGS_RESUME)
                {
                    if (pParserModule->pfResume)
                        result = pParserModule->pfResume(pEASData, pEASData->streams[streamNum].handle);
                    pEASData->streams[streamNum].streamFlags &= ~STREAM_FLAGS_RESUME;
                }
            }
#endif

            /* if necessary, parse stream */
            if ((pEASData->streams[streamNum].streamFlags & STREAM_FLAGS_PARSED) == 0)
                if ((result = EAS_ParseEvents(pEASData, &pEASData->streams[streamNum], pEASData->streams[streamNum].time + pEASData->streams[streamNum].frameLength, eParserModePlay)) != EAS_SUCCESS)
                    return result;

            /* check for an early abort */
            if ((pEASData->streams[streamNum].streamFlags) == 0)
            {

#ifdef _METRICS_ENABLED
                /* stop performance counter */
                if (pEASData->pMetricsData)
                    (*pEASData->pMetricsModule->pfStartTimer)(pEASData->pMetricsData, EAS_PM_TOTAL_TIME);
#endif

                return EAS_SUCCESS;
            }

            /* check for repeat */
            if (pEASData->streams[streamNum].repeatCount)
            {

                /* check for stopped state */
                if ((result = (*pParserModule->pfState)(pEASData, pEASData->streams[streamNum].handle, &parserState)) != EAS_SUCCESS)
                    return result;
                if (parserState == EAS_STATE_STOPPED)
                {

                    /* decrement repeat count, unless it is negative */
                    if (pEASData->streams[streamNum].repeatCount > 0)
                        pEASData->streams[streamNum].repeatCount--;

                    /* reset the parser */
                    if ((result = (*pParserModule->pfReset)(pEASData, pEASData->streams[streamNum].handle)) != EAS_SUCCESS)
                        return result;
                    pEASData->streams[streamNum].time = 0;
                }
            }
        }
    }

#ifdef _METRICS_ENABLED
    /* stop performance counter */
    if (pEASData->pMetricsData)
        (void)(*pEASData->pMetricsModule->pfStopTimer)(pEASData->pMetricsData, EAS_PM_PARSE_TIME);
#endif

#ifdef _METRICS_ENABLED
    /* start the render timer */
    if (pEASData->pMetricsData)
        (*pEASData->pMetricsModule->pfStartTimer)(pEASData->pMetricsData, EAS_PM_RENDER_TIME);
#endif

    /* render audio */
    if ((result = VMRender(pEASData->pVoiceMgr, BUFFER_SIZE_IN_MONO_SAMPLES, pEASData->pMixBuffer, &voicesRendered)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "pfRender function returned error %ld\n", result); */ }
        return result;
    }

#ifdef _METRICS_ENABLED
    /* stop the render timer */
    if (pEASData->pMetricsData) {
        (*pEASData->pMetricsModule->pfIncrementCounter)(pEASData->pMetricsData, EAS_PM_FRAME_COUNT, 1);
        (void)(*pEASData->pMetricsModule->pfStopTimer)(pEASData->pMetricsData, EAS_PM_RENDER_TIME);
        (*pEASData->pMetricsModule->pfIncrementCounter)(pEASData->pMetricsData, EAS_PM_TOTAL_VOICE_COUNT, (EAS_U32) voicesRendered);
        (void)(*pEASData->pMetricsModule->pfRecordMaxValue)(pEASData->pMetricsData, EAS_PM_MAX_VOICES, (EAS_U32) voicesRendered);
    }
#endif

    //2 Do we really need frameParsed?
    /* need to parse another frame of events before we render again */
    for (streamNum = 0; streamNum < MAX_NUMBER_STREAMS; streamNum++)
        if (pEASData->streams[streamNum].pParserModule != NULL)
            pEASData->streams[streamNum].streamFlags &= ~STREAM_FLAGS_PARSED;

#ifdef _METRICS_ENABLED
    /* start performance counter */
    if (pEASData->pMetricsData)
        (*pEASData->pMetricsModule->pfStartTimer)(pEASData->pMetricsData, EAS_PM_STREAM_TIME);
#endif

    /* render PCM audio */
    if ((result = EAS_PERender(pEASData, numRequested)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "EAS_PERender returned error %ld\n", result); */ }
        return result;
    }

#ifdef _METRICS_ENABLED
    /* stop the stream timer */
    if (pEASData->pMetricsData)
        (void)(*pEASData->pMetricsModule->pfStopTimer)(pEASData->pMetricsData, EAS_PM_STREAM_TIME);
#endif

#ifdef _METRICS_ENABLED
    /* start the post timer */
    if (pEASData->pMetricsData)
        (*pEASData->pMetricsModule->pfStartTimer)(pEASData->pMetricsData, EAS_PM_POST_TIME);
#endif

    /* for split architecture, send DSP vectors.  Do post only if return is TRUE */
#ifdef _SPLIT_ARCHITECTURE
    if (VMEndFrame(pEASData))
    {
        /* now do post-processing */
        EAS_MixEnginePost(pEASData, numRequested);
        *pNumGenerated = numRequested;
    }
#else
    /* now do post-processing */
    EAS_MixEnginePost(pEASData, numRequested);
    *pNumGenerated = numRequested;
#endif

#ifdef _METRICS_ENABLED
    /* stop the post timer */
    if (pEASData->pMetricsData)
        (void)(*pEASData->pMetricsModule->pfStopTimer)(pEASData->pMetricsData, EAS_PM_POST_TIME);
#endif

    /* advance render time */
    pEASData->renderTime += AUDIO_FRAME_LENGTH;

#if 0
    /* dump workload for debug */
    if (pEASData->pVoiceMgr->workload)
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Workload = %d\n", pEASData->pVoiceMgr->workload); */ }
#endif

#ifdef _METRICS_ENABLED
    /* stop performance counter */
    if (pEASData->pMetricsData)
    {
        PERF_TIMER temp;
        temp = (*pEASData->pMetricsModule->pfStopTimer)(pEASData->pMetricsData, EAS_PM_TOTAL_TIME);

        /* if max render time, record the number of voices and time */
        if ((*pEASData->pMetricsModule->pfRecordMaxValue)
            (pEASData->pMetricsData, EAS_PM_MAX_CYCLES, (EAS_U32) temp))
        {
            (*pEASData->pMetricsModule->pfRecordValue)(pEASData->pMetricsData, EAS_PM_MAX_CYCLES_VOICES, (EAS_U32) voicesRendered);
            (*pEASData->pMetricsModule->pfRecordValue)(pEASData->pMetricsData, EAS_PM_MAX_CYCLES_TIME, (EAS_I32) (pEASData->renderTime >> 8));
        }
    }
#endif

#ifdef JET_INTERFACE
    /* let JET to do its thing */
    if (pEASData->jetHandle != NULL)
    {
        result = JET_Process(pEASData);
        if (result != EAS_SUCCESS)
            return result;
    }
#endif

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_SetRepeat()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the selected stream to repeat.
 *
 * Inputs:
 *  pEASData        - handle to data for this instance
 *  handle          - handle to stream
 *  repeatCount     - repeat count
 *
 * Outputs:
 *
 * Side Effects:
 *
 * Notes:
 *  0 = no repeat
 *  1 = repeat once, i.e. play through twice
 *  -1 = repeat forever
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
EAS_PUBLIC EAS_RESULT EAS_SetRepeat (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_I32 repeatCount)
{
    pStream->repeatCount = repeatCount;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_GetRepeat()
 *----------------------------------------------------------------------------
 * Purpose:
 * Gets the current repeat count for the selected stream.
 *
 * Inputs:
 *  pEASData        - handle to data for this instance
 *  handle          - handle to stream
 *  pRrepeatCount   - pointer to variable to hold repeat count
 *
 * Outputs:
 *
 * Side Effects:
 *
 * Notes:
 *  0 = no repeat
 *  1 = repeat once, i.e. play through twice
 *  -1 = repeat forever
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
EAS_PUBLIC EAS_RESULT EAS_GetRepeat (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_I32 *pRepeatCount)
{
    *pRepeatCount = pStream->repeatCount;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_SetPlaybackRate()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the playback rate.
 *
 * Inputs:
 *  pEASData        - handle to data for this instance
 *  handle          - handle to stream
 *  rate            - rate (28-bit fractional amount)
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
EAS_PUBLIC EAS_RESULT EAS_SetPlaybackRate (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_U32 rate)
{

    /* check range */
    if ((rate < (1 << 27)) || (rate > (1 << 29)))
        return EAS_ERROR_INVALID_PARAMETER;

    /* calculate new frame length
     *
     * NOTE: The maximum frame length we can accomodate based on a
     * maximum rate of 2.0 (2^28) is 2047 (2^13-1). To accomodate a
     * longer frame length or a higher maximum rate, the fixed point
     * divide below will need to be adjusted
     */
    pStream->frameLength = (AUDIO_FRAME_LENGTH * (rate >> 8)) >> 20;

    /* notify stream of new playback rate */
    EAS_SetStreamParameter(pEASData, pStream, PARSER_DATA_PLAYBACK_RATE, (EAS_I32) rate);
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_SetTransposition)
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the key tranposition for the synthesizer. Transposes all
 * melodic instruments by the specified amount. Range is limited
 * to +/-12 semitones.
 *
 * Inputs:
 *  pEASData        - handle to data for this instance
 *  handle          - handle to stream
 *  transposition   - +/-12 semitones
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetTransposition (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_I32 transposition)
{

    /* check range */
    if ((transposition < -12) || (transposition > 12))
        return EAS_ERROR_INVALID_PARAMETER;

    if (!EAS_StreamReady(pEASData, pStream))
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;
    return EAS_IntSetStrmParam(pEASData, pStream, PARSER_DATA_TRANSPOSITION, transposition);
}

/*----------------------------------------------------------------------------
 * EAS_ParseEvents()
 *----------------------------------------------------------------------------
 * Purpose:
 * Parse events in the current streams until the desired time is reached.
 *
 * Inputs:
 *  pEASData        - buffer for internal EAS data
 *  endTime         - stop parsing if this time is reached
 *  parseMode       - play, locate, or metadata
 *
 * Outputs:
 *  EAS_SUCCESS if PCM data was successfully rendered
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT EAS_ParseEvents (S_EAS_DATA *pEASData, EAS_HANDLE pStream, EAS_U32 endTime, EAS_INT parseMode)
{
    S_FILE_PARSER_INTERFACE *pParserModule;
    EAS_RESULT result;
    EAS_I32 parserState;
    EAS_BOOL done;
    EAS_INT yieldCount = YIELD_EVENT_COUNT;
    EAS_U32 time = 0;

    // This constant is the maximum number of events that can be processed in a single time slice.
    // A typical ringtone will contain a few events per time slice.
    // Extremely dense ringtones might go up to 50 events.
    // If we see this many events then the file is probably stuck in an infinite loop
    // and should be aborted.
    static const EAS_INT MAX_EVENT_COUNT = 100000;
    EAS_INT eventCount = 0;

    /* does this parser have a time function? */
    pParserModule = pStream->pParserModule;
    if (pParserModule->pfTime == NULL)
    {
        /* check state */
        if ((result = (*pParserModule->pfState)(pEASData, pStream->handle, &parserState)) != EAS_SUCCESS)
            return result;
        /* if play state, advance time */
        if ((parserState >= EAS_STATE_READY) && (parserState <= EAS_STATE_PAUSING))
            pStream->time += pStream->frameLength;
        done = EAS_TRUE;
    }

    /* assume we're not done, in case we abort out */
    else
    {
        pStream->streamFlags &= ~STREAM_FLAGS_PARSED;
        done = EAS_FALSE;
    }

    while (!done)
    {

        /* check for stopped state */
        if ((result = (*pParserModule->pfState)(pEASData, pStream->handle, &parserState)) != EAS_SUCCESS)
            return result;
        if (parserState > EAS_STATE_PLAY)
        {
            /* save current time if we're not in play mode */
            if (parseMode != eParserModePlay)
                pStream->time = time << 8;
            done = EAS_TRUE;
            break;
        }

        /* get the next event time */
        if (pParserModule->pfTime)
        {
            if ((result = (*pParserModule->pfTime)(pEASData, pStream->handle, &time)) != EAS_SUCCESS)
                return result;

            /* if next event is within this frame, parse it */
            if (time < (endTime >> 8))
            {

                /* parse the next event */
                if (pParserModule->pfEvent) {
                    if ((result = (*pParserModule->pfEvent)(pEASData, pStream->handle, parseMode))
                            != EAS_SUCCESS) {
                        ALOGE("%s() pfEvent returned %ld", __func__, result);
                        return result;
                    }
                }

                // An infinite loop within a ringtone file can cause this function
                // to loop forever.  Try to detect that and return an error.
                // Only check when playing. Otherwise a very large file could be rejected
                // when scanning the entire file in a single call to this function.
                // OTA files will only do infinite loops when in eParserModePlay.
                if (++eventCount >= MAX_EVENT_COUNT && parseMode == eParserModePlay) {
                    ALOGE("%s() aborting, %d events. Infinite loop in song file?!",
                            __func__, eventCount);
                    android_errorWriteLog(0x534e4554, "68664359");
                    return EAS_ERROR_FILE_POS;
                }
            }

            /* no more events in this frame, advance time */
            else
            {
                pStream->time = endTime;
                done = EAS_TRUE;
            }
        }

        /* check for max workload exceeded */
        if (VMCheckWorkload(pEASData->pVoiceMgr))
        {
            /* stop even though we may not have parsed
             * all the events in this frame. The parser will try to
             * catch up on the next frame.
             */
            break;
        }

        /* give host a chance for an early abort */
        if (--yieldCount == 0)
        {
            if (EAS_HWYield(pEASData->hwInstData))
                break;
            yieldCount = YIELD_EVENT_COUNT;
        }
    }

    /* if no early abort, parsing is complete for this frame */
    if (done)
        pStream->streamFlags |= STREAM_FLAGS_PARSED;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_ParseMetaData()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - file or stream handle
 * playLength       - pointer to variable to store the play length (in msecs)
 *
 * Outputs:
 *
 *
 * Side Effects:
 *                  - resets the parser to the start of the file
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_ParseMetaData (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_I32 *playLength)
{
    S_FILE_PARSER_INTERFACE *pParserModule;
    EAS_RESULT result;
    EAS_STATE state;

    pParserModule = (S_FILE_PARSER_INTERFACE*) pStream->pParserModule;
    if (pParserModule == NULL)
        return EAS_ERROR_FEATURE_NOT_AVAILABLE;

    /* check parser state */
    if ((result = (*pParserModule->pfState)(pEASData, pStream->handle, &state)) != EAS_SUCCESS)
        return result;
    if (state >= EAS_STATE_OPEN)
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;

    /* if parser has metadata function, use that */
    if (pParserModule->pfGetMetaData != NULL)
        return pParserModule->pfGetMetaData(pEASData, pStream->handle, playLength);

    /* reset the parser to the beginning */
    if ((result = (*pParserModule->pfReset)(pEASData, pStream->handle)) != EAS_SUCCESS)
        return result;

    /* parse the file to end */
    pStream->time = 0;
    VMInitWorkload(pEASData->pVoiceMgr);
    if ((result = EAS_ParseEvents(pEASData, pStream, 0x7fffffff, eParserModeMetaData)) != EAS_SUCCESS)
        return result;

    /* get the parser time */
    if ((result = EAS_GetLocation(pEASData, pStream, playLength)) != EAS_SUCCESS)
        return result;

    /* reset the parser to the beginning */
    pStream->time = 0;
    return (*pParserModule->pfReset)(pEASData, pStream->handle);
}

/*----------------------------------------------------------------------------
 * EAS_RegisterMetaDataCallback()
 *----------------------------------------------------------------------------
 * Purpose:
 * Registers a metadata callback function for parsed metadata.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - file or stream handle
 * cbFunc           - pointer to host callback function
 * metaDataBuffer   - pointer to metadata buffer
 * metaDataBufSize  - maximum size of the metadata buffer
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_RegisterMetaDataCallback (
    EAS_DATA_HANDLE pEASData,
    EAS_HANDLE pStream,
    EAS_METADATA_CBFUNC cbFunc,
    char *metaDataBuffer,
    EAS_I32 metaDataBufSize,
    EAS_VOID_PTR pUserData)
{
    S_METADATA_CB metadata;

    if (!EAS_StreamReady(pEASData, pStream))
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;

    /* register callback function */
    metadata.callback = cbFunc;
    metadata.buffer = metaDataBuffer;
    metadata.bufferSize = metaDataBufSize;
    metadata.pUserData = pUserData;
    return EAS_SetStreamParameter(pEASData, pStream, PARSER_DATA_METADATA_CB, (EAS_I32) &metadata);
}

/*----------------------------------------------------------------------------
 * EAS_GetNoteCount ()
 *----------------------------------------------------------------------------
 * Returns the total number of notes played in this stream
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetNoteCount (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_I32 *pNoteCount)
{
    if (!EAS_StreamReady(pEASData, pStream))
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;
    return EAS_IntGetStrmParam(pEASData, pStream, PARSER_DATA_NOTE_COUNT, pNoteCount);
}

/*----------------------------------------------------------------------------
 * EAS_CloseFile()
 *----------------------------------------------------------------------------
 * Purpose:
 * Closes an audio file or stream. Playback should have either paused or
 * completed (EAS_State returns EAS_PAUSED or EAS_STOPPED).
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - file or stream handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_CloseFile (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream)
{
    S_FILE_PARSER_INTERFACE *pParserModule;
    EAS_RESULT result;

    /* call the close function */
    pParserModule = (S_FILE_PARSER_INTERFACE*) pStream->pParserModule;
    if (pParserModule == NULL)
        return EAS_ERROR_FEATURE_NOT_AVAILABLE;

    result = (*pParserModule->pfClose)(pEASData, pStream->handle);

    /* clear the handle and parser interface pointer */
    pStream->handle = NULL;
    pStream->pParserModule = NULL;
    return result;
}

/*----------------------------------------------------------------------------
 * EAS_OpenMIDIStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Opens a raw MIDI stream allowing the host to route MIDI cable data directly to the synthesizer
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * pHandle          - pointer to variable to hold file or stream handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_OpenMIDIStream (EAS_DATA_HANDLE pEASData, EAS_HANDLE *ppStream, EAS_HANDLE streamHandle)
{
    EAS_RESULT result;
    S_INTERACTIVE_MIDI *pMIDIStream;
    EAS_INT streamNum;

    /* initialize some pointers */
    *ppStream = NULL;

    /* allocate a stream */
    if ((streamNum = EAS_AllocateStream(pEASData)) < 0)
        return EAS_ERROR_MAX_STREAMS_OPEN;

    /* check Configuration Module for S_EAS_DATA allocation */
    if (pEASData->staticMemoryModel)
        pMIDIStream = EAS_CMEnumData(EAS_CM_MIDI_STREAM_DATA);
    else
        pMIDIStream = EAS_HWMalloc(pEASData->hwInstData, sizeof(S_INTERACTIVE_MIDI));

    /* allocate dynamic memory */
    if (!pMIDIStream)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_FATAL, "Failed to allocate MIDI stream data\n"); */ }
        return EAS_ERROR_MALLOC_FAILED;
    }

    /* zero the memory to insure complete initialization */
    EAS_HWMemSet(pMIDIStream, 0, sizeof(S_INTERACTIVE_MIDI));
    EAS_InitStream(&pEASData->streams[streamNum], NULL, pMIDIStream);

    /* instantiate a new synthesizer */
    if (streamHandle == NULL)
    {
        result = VMInitMIDI(pEASData, &pMIDIStream->pSynth);
    }

    /* use an existing synthesizer */
    else
    {
        EAS_I32 value;
        result = EAS_GetStreamParameter(pEASData, streamHandle, PARSER_DATA_SYNTH_HANDLE, &value);
        pMIDIStream->pSynth = (S_SYNTH*) value;
        VMIncRefCount(pMIDIStream->pSynth);
    }
    if (result != EAS_SUCCESS)
    {
        EAS_CloseMIDIStream(pEASData, &pEASData->streams[streamNum]);
        return result;
    }

    /* initialize the MIDI stream data */
    EAS_InitMIDIStream(&pMIDIStream->stream);

    *ppStream = (EAS_HANDLE) &pEASData->streams[streamNum];
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_WriteMIDIStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Send data to the MIDI stream device
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - stream handle
 * pBuffer          - pointer to buffer
 * count            - number of bytes to write
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_WriteMIDIStream (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_U8 *pBuffer, EAS_I32 count)
{
    S_INTERACTIVE_MIDI *pMIDIStream;
    EAS_RESULT result;

    pMIDIStream = (S_INTERACTIVE_MIDI*) pStream->handle;

    if (count <= 0)
        return EAS_ERROR_PARAMETER_RANGE;

    /* send the entire buffer */
    while (count--)
    {
        if ((result = EAS_ParseMIDIStream(pEASData, pMIDIStream->pSynth, &pMIDIStream->stream, *pBuffer++, eParserModePlay)) != EAS_SUCCESS)
            return result;
    }
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_CloseMIDIStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Closes a raw MIDI stream
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - stream handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_CloseMIDIStream (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream)
{
    S_INTERACTIVE_MIDI *pMIDIStream;

    pMIDIStream = (S_INTERACTIVE_MIDI*) pStream->handle;

    /* close synth */
    if (pMIDIStream->pSynth != NULL)
    {
        VMMIDIShutdown(pEASData, pMIDIStream->pSynth);
        pMIDIStream->pSynth = NULL;
    }

    /* release allocated memory */
    if (!pEASData->staticMemoryModel)
        EAS_HWFree(((S_EAS_DATA*) pEASData)->hwInstData, pMIDIStream);

    pStream->handle = NULL;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_State()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the state of an audio file or stream.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - file or stream handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_State (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_STATE *pState)
{
    S_FILE_PARSER_INTERFACE *pParserModule;
    EAS_RESULT result;

    /* call the parser to return state */
    pParserModule = (S_FILE_PARSER_INTERFACE*) pStream->pParserModule;
    if (pParserModule == NULL)
        return EAS_ERROR_FEATURE_NOT_AVAILABLE;

    if ((result = (*pParserModule->pfState)(pEASData, pStream->handle, pState)) != EAS_SUCCESS)
        return result;

    /* if repeat count is set for this parser, mask the stopped state from the application */
    if (pStream->repeatCount && (*pState == EAS_STATE_STOPPED))
        *pState = EAS_STATE_PLAY;

    /* if we're not paused or pausing, we don't need to hide state from host */
    if (*pState != EAS_STATE_PAUSED && *pState != EAS_STATE_PAUSING)
        return EAS_SUCCESS;

    /* if stream is about to be paused, report it as paused */
    if (pStream->streamFlags & STREAM_FLAGS_PAUSE)
    {
        if (pStream->streamFlags & STREAM_FLAGS_LOCATE)
            *pState = EAS_STATE_PAUSED;
        else
            *pState = EAS_STATE_PAUSING;
    }

    /* if stream is about to resume, report it as playing */
    if (pStream->streamFlags & STREAM_FLAGS_RESUME)
        *pState = EAS_STATE_PLAY;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_SetPolyphony()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the polyphony of the stream. A value of 0 allows the stream
 * to use all voices (set by EAS_SetSynthPolyphony).
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - handle returned by EAS_OpenFile
 * polyphonyCount   - the desired polyphony count
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetPolyphony (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_I32 polyphonyCount)
{
    if (!EAS_StreamReady(pEASData, pStream))
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;
    return EAS_IntSetStrmParam(pEASData, pStream, PARSER_DATA_POLYPHONY, polyphonyCount);
}

/*----------------------------------------------------------------------------
 * EAS_GetPolyphony()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the current polyphony setting of the stream
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - handle returned by EAS_OpenFile
 * pPolyphonyCount  - pointer to variable to receive polyphony count
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetPolyphony (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_I32 *pPolyphonyCount)
{
    if (!EAS_StreamReady(pEASData, pStream))
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;
    return EAS_IntGetStrmParam(pEASData, pStream, PARSER_DATA_POLYPHONY, pPolyphonyCount);
}

/*----------------------------------------------------------------------------
 * EAS_SetSynthPolyphony()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the polyphony of the synth . Value must be >= 1 and <= the
 * maximum number of voices. This function will pin the polyphony
 * at those limits
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * synthNum         - synthesizer number (0 = onboard, 1 = DSP)
 * polyphonyCount   - the desired polyphony count
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetSynthPolyphony (EAS_DATA_HANDLE pEASData, EAS_I32 synthNum, EAS_I32 polyphonyCount)
{
    return VMSetSynthPolyphony(pEASData->pVoiceMgr, synthNum, polyphonyCount);
}

/*----------------------------------------------------------------------------
 * EAS_GetSynthPolyphony()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the current polyphony setting of the synth
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * synthNum         - synthesizer number (0 = onboard, 1 = DSP)
 * pPolyphonyCount  - pointer to variable to receive polyphony count
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetSynthPolyphony (EAS_DATA_HANDLE pEASData, EAS_I32 synthNum, EAS_I32 *pPolyphonyCount)
{
    return VMGetSynthPolyphony(pEASData->pVoiceMgr, synthNum, pPolyphonyCount);
}

/*----------------------------------------------------------------------------
 * EAS_SetPriority()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the priority of the stream. Determines which stream's voices
 * are stolen when there are insufficient voices for all notes.
 * Value must be in the range of 1-15, lower values are higher
 * priority.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - handle returned by EAS_OpenFile
 * polyphonyCount   - the desired polyphony count
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetPriority (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_I32 priority)
{
    if (!EAS_StreamReady(pEASData, pStream))
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;
    return EAS_IntSetStrmParam(pEASData, pStream, PARSER_DATA_PRIORITY, priority);
}

/*----------------------------------------------------------------------------
 * EAS_GetPriority()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the current priority setting of the stream
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - handle returned by EAS_OpenFile
 * pPriority        - pointer to variable to receive priority
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetPriority (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_I32 *pPriority)
{
    if (!EAS_StreamReady(pEASData, pStream))
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;
    return EAS_IntGetStrmParam(pEASData, pStream, PARSER_DATA_PRIORITY, pPriority);
}

/*----------------------------------------------------------------------------
 * EAS_SetVolume()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the master gain for the mix engine in 1dB increments
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * volume           - the desired master gain (100 is max)
 * handle           - file or stream handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 * overrides any previously set master volume from sysex
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetVolume (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_I32 volume)
{
    EAS_I16 gain;

    /* check range */
    if ((volume < 0) || (volume > EAS_MAX_VOLUME))
        return EAS_ERROR_PARAMETER_RANGE;

    /* stream volume */
    if (pStream != NULL)
    {
        EAS_I32 gainOffset;
        EAS_RESULT result;

        if (!EAS_StreamReady(pEASData, pStream))
            return EAS_ERROR_NOT_VALID_IN_THIS_STATE;

        /* get gain offset */
        pStream->volume = (EAS_U8) volume;
        result = EAS_GetStreamParameter(pEASData, pStream, PARSER_DATA_GAIN_OFFSET, &gainOffset);
        if (result == EAS_SUCCESS)
            volume += gainOffset;

        /* set stream volume */
        gain = EAS_VolumeToGain(volume - STREAM_VOLUME_HEADROOM);

        /* convert to linear scalar */
        return EAS_IntSetStrmParam(pEASData, pStream, PARSER_DATA_VOLUME, gain);
    }

    /* master volume */
    pEASData->masterVolume = (EAS_U8) volume;
#if (NUM_OUTPUT_CHANNELS == 1)
    /* leave 3dB headroom for mono output */
    volume -= 3;
#endif

    gain = EAS_VolumeToGain(volume - STREAM_VOLUME_HEADROOM);
    pEASData->masterGain = gain;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_GetVolume()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the master volume for the synthesizer. The default volume setting is
 * 50. The volume range is 0 to 100;
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * volume           - the desired master volume
 * handle           - file or stream handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 * overrides any previously set master volume from sysex
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_I32 EAS_GetVolume (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream)
{
    if (pStream == NULL)
        return pEASData->masterVolume;

    if (!EAS_StreamReady(pEASData, pStream))
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;
    return pStream->volume;
}

/*----------------------------------------------------------------------------
 * EAS_SetMaxLoad()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the maximum workload the parsers will do in a single call to
 * EAS_Render. The units are currently arbitrary, but should correlate
 * well to the actual CPU cycles consumed. The primary effect is to
 * reduce the occasional peaks in CPU cycles consumed when parsing
 * dense parts of a MIDI score.
 *
 * Inputs:
 *  pEASData        - handle to data for this instance
 *  maxLoad         - the desired maximum workload
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetMaxLoad (EAS_DATA_HANDLE pEASData, EAS_I32 maxLoad)
{
    VMSetWorkload(pEASData->pVoiceMgr, maxLoad);
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_SetMaxPCMStreams()
 *----------------------------------------------------------------------------
 * Sets the maximum number of PCM streams allowed in parsers that
 * use PCM streaming.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * streamHandle     - handle returned by EAS_OpenFile
 * maxNumStreams    - maximum number of PCM streams
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetMaxPCMStreams (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_I32 maxNumStreams)
{
    if (!EAS_StreamReady(pEASData, pStream))
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;
    return EAS_IntSetStrmParam(pEASData, pStream, PARSER_DATA_MAX_PCM_STREAMS, maxNumStreams);
}

/*----------------------------------------------------------------------------
 * EAS_Locate()
 *----------------------------------------------------------------------------
 * Purpose:
 * Locate into the file associated with the handle.
 *
 * Inputs:
 * pEASData - pointer to overall EAS data structure
 * handle           - file handle
 * milliseconds     - playback offset from start of file in milliseconds
 *
 * Outputs:
 *
 *
 * Side Effects:
 * the actual offset will be quantized to the closest update period, typically
 * a resolution of 5.9ms. Notes that are started prior to this time will not
 * sound. Any notes currently playing will be shut off.
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_Locate (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_I32 milliseconds, EAS_BOOL offset)
{
    S_FILE_PARSER_INTERFACE *pParserModule;
    EAS_RESULT result;
    EAS_U32 requestedTime;
    EAS_STATE state;

    /* get pointer to parser function table */
    pParserModule = (S_FILE_PARSER_INTERFACE*) pStream->pParserModule;
    if (pParserModule == NULL)
        return EAS_ERROR_FEATURE_NOT_AVAILABLE;

    if ((result = (*pParserModule->pfState)(pEASData, pStream->handle, &state)) != EAS_SUCCESS)
        return result;
    if (state >= EAS_STATE_OPEN)
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;

    /* handle offset and limit to start of file */
    /*lint -e{704} use shift for performance*/
    if (offset)
        milliseconds += (EAS_I32) pStream->time >> 8;
    if (milliseconds < 0)
        milliseconds = 0;

    /* check to see if the request is different from the current time */
    requestedTime = (EAS_U32) milliseconds;
    if (requestedTime == (pStream->time >> 8))
        return EAS_SUCCESS;

    /* set the locate flag */
    pStream->streamFlags |= STREAM_FLAGS_LOCATE;

    /* use the parser locate function, if available */
    if (pParserModule->pfLocate != NULL)
    {
        EAS_BOOL parserLocate = EAS_FALSE;
        result = pParserModule->pfLocate(pEASData, pStream->handle, (EAS_I32) requestedTime, &parserLocate);
        if (!parserLocate)
        {
            if (result == EAS_SUCCESS)
                pStream->time = requestedTime << 8;
            return result;
        }
    }

    /* if we were paused and not going to resume, set pause request flag */
    if (((state == EAS_STATE_PAUSING) || (state == EAS_STATE_PAUSED)) && ((pStream->streamFlags & STREAM_FLAGS_RESUME) == 0))
        pStream->streamFlags |= STREAM_FLAGS_PAUSE;

    /* reset the synth and parser */
    if ((result = (*pParserModule->pfReset)(pEASData, pStream->handle)) != EAS_SUCCESS)
        return result;
    pStream->time = 0;

    /* locating forward, clear parsed flag and parse data until we get to the requested location */
    if ((result = EAS_ParseEvents(pEASData, pStream, requestedTime << 8, eParserModeLocate)) != EAS_SUCCESS)
        return result;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_GetLocation()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the current playback offset
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - file handle
 *
 * Outputs:
 * The offset in milliseconds from the start of the current sequence, quantized
 * to the nearest update period. Actual resolution is typically 5.9 ms.
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
EAS_PUBLIC EAS_RESULT EAS_GetLocation (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_I32 *pTime)
{
    if (!EAS_StreamReady(pEASData, pStream))
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;

    *pTime = pStream->time >> 8;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_GetRenderTime()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the current playback offset
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 *
 * Outputs:
 * Gets the render time clock in msecs.
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetRenderTime (EAS_DATA_HANDLE pEASData, EAS_I32 *pTime)
{
    *pTime = pEASData->renderTime >> 8;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_Pause()
 *----------------------------------------------------------------------------
 * Purpose:
 * Pauses the playback of the data associated with this handle. The audio
 * is gracefully ramped down to prevent clicks and pops. It may take several
 * buffers of audio before the audio is muted.
 *
 * Inputs:
 * psEASData        - pointer to overall EAS data structure
 * handle           - file or stream handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_Pause (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream)
{
    S_FILE_PARSER_INTERFACE *pParserModule;
    EAS_STATE state;
    EAS_RESULT result;

    pParserModule = (S_FILE_PARSER_INTERFACE*) pStream->pParserModule;
    if (pParserModule == NULL)
        return EAS_ERROR_FEATURE_NOT_AVAILABLE;

    /* check for valid state */
    result = pParserModule->pfState(pEASData, pStream->handle, &state);
    if (result == EAS_SUCCESS)
    {
        if ((state != EAS_STATE_PLAY) && (state != EAS_STATE_READY) && ((pStream->streamFlags & STREAM_FLAGS_RESUME) == 0))
            return EAS_ERROR_NOT_VALID_IN_THIS_STATE;

        /* make sure parser implements pause */
        if (pParserModule->pfPause == NULL)
            result = EAS_ERROR_NOT_IMPLEMENTED;

        /* clear resume flag */
        pStream->streamFlags &= ~STREAM_FLAGS_RESUME;

        /* set pause flag */
        pStream->streamFlags |= STREAM_FLAGS_PAUSE;

#if 0
        /* pause the stream */
        if (pParserModule->pfPause)
            result = pParserModule->pfPause(pEASData, pStream->handle);
        else
            result = EAS_ERROR_NOT_IMPLEMENTED;
#endif
    }

    return result;
}

/*----------------------------------------------------------------------------
 * EAS_Resume()
 *----------------------------------------------------------------------------
 * Purpose:
 * Resumes the playback of the data associated with this handle. The audio
 * is gracefully ramped up to prevent clicks and pops.
 *
 * Inputs:
 * psEASData        - pointer to overall EAS data structure
 * handle           - file or stream handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_Resume (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream)
{
    S_FILE_PARSER_INTERFACE *pParserModule;
    EAS_STATE state;
    EAS_RESULT result;

    pParserModule = (S_FILE_PARSER_INTERFACE*) pStream->pParserModule;
    if (pParserModule == NULL)
        return EAS_ERROR_FEATURE_NOT_AVAILABLE;

    /* check for valid state */
    result = pParserModule->pfState(pEASData, pStream->handle, &state);
    if (result == EAS_SUCCESS)
    {
        if ((state != EAS_STATE_PAUSED) && (state != EAS_STATE_PAUSING) && ((pStream->streamFlags & STREAM_FLAGS_PAUSE) == 0))
            return EAS_ERROR_NOT_VALID_IN_THIS_STATE;

        /* make sure parser implements this function */
        if (pParserModule->pfResume == NULL)
            result = EAS_ERROR_NOT_IMPLEMENTED;

        /* clear pause flag */
        pStream->streamFlags &= ~STREAM_FLAGS_PAUSE;

        /* set resume flag */
        pStream->streamFlags |= STREAM_FLAGS_RESUME;

#if 0
        /* resume the stream */
        if (pParserModule->pfResume)
            result = pParserModule->pfResume(pEASData, pStream->handle);
        else
            result = EAS_ERROR_NOT_IMPLEMENTED;
#endif
    }

    return result;
}

/*----------------------------------------------------------------------------
 * EAS_GetParameter()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the parameter of a module. See E_MODULES for a list of modules
 * and the header files of the modules for a list of parameters.
 *
 * Inputs:
 * psEASData        - pointer to overall EAS data structure
 * handle           - file or stream handle
 * module           - enumerated module number
 * param            - enumerated parameter number
 * pValue           - pointer to variable to receive parameter value
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetParameter (EAS_DATA_HANDLE pEASData, EAS_I32 module, EAS_I32 param, EAS_I32 *pValue)
{

    if (module >= NUM_EFFECTS_MODULES)
        return EAS_ERROR_INVALID_MODULE;

    if (pEASData->effectsModules[module].effectData == NULL)
        return EAS_ERROR_INVALID_MODULE;

    return (*pEASData->effectsModules[module].effect->pFGetParam)
        (pEASData->effectsModules[module].effectData, param, pValue);
}

/*----------------------------------------------------------------------------
 * EAS_SetParameter()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set the parameter of a module. See E_MODULES for a list of modules
 * and the header files of the modules for a list of parameters.
 *
 * Inputs:
 * psEASData        - pointer to overall EAS data structure
 * handle           - file or stream handle
 * module           - enumerated module number
 * param            - enumerated parameter number
 * value            - new parameter value
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetParameter (EAS_DATA_HANDLE pEASData, EAS_I32 module, EAS_I32 param, EAS_I32 value)
{

    if (module >= NUM_EFFECTS_MODULES)
        return EAS_ERROR_INVALID_MODULE;

    if (pEASData->effectsModules[module].effectData == NULL)
        return EAS_ERROR_INVALID_MODULE;

    return (*pEASData->effectsModules[module].effect->pFSetParam)
        (pEASData->effectsModules[module].effectData, param, value);
}

#ifdef _METRICS_ENABLED
/*----------------------------------------------------------------------------
 * EAS_MetricsReport()
 *----------------------------------------------------------------------------
 * Purpose:
 * Displays the current metrics through the metrics interface.
 *
 * Inputs:
 * p                - instance data handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_MetricsReport (EAS_DATA_HANDLE pEASData)
{
    if (!pEASData->pMetricsModule)
        return EAS_ERROR_INVALID_MODULE;

    return (*pEASData->pMetricsModule->pfReport)(pEASData->pMetricsData);
}

/*----------------------------------------------------------------------------
 * EAS_MetricsReset()
 *----------------------------------------------------------------------------
 * Purpose:
 * Resets the metrics.
 *
 * Inputs:
 * p                - instance data handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_MetricsReset (EAS_DATA_HANDLE pEASData)
{

    if (!pEASData->pMetricsModule)
        return EAS_ERROR_INVALID_MODULE;

    return (*pEASData->pMetricsModule->pfReset)(pEASData->pMetricsData);
}
#endif

/*----------------------------------------------------------------------------
 * EAS_SetSoundLibrary()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the location of the sound library.
 *
 * Inputs:
 * pEASData             - instance data handle
 * pSoundLib            - pointer to sound library
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetSoundLibrary (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_SNDLIB_HANDLE pSndLib)
{
    if (pStream)
    {
        if (!EAS_StreamReady(pEASData, pStream))
            return EAS_ERROR_NOT_VALID_IN_THIS_STATE;
        return EAS_IntSetStrmParam(pEASData, pStream, PARSER_DATA_EAS_LIBRARY, (EAS_I32) pSndLib);
    }

    return VMSetGlobalEASLib(pEASData->pVoiceMgr, pSndLib);
}

/*----------------------------------------------------------------------------
 * EAS_SetHeaderSearchFlag()
 *----------------------------------------------------------------------------
 * By default, when EAS_OpenFile is called, the parsers check the
 * first few bytes of the file looking for a specific header. Some
 * mobile devices may add a header to the start of a file, which
 * will prevent the parser from recognizing the file. If the
 * searchFlag is set to EAS_TRUE, the parser will search the entire
 * file looking for the header. This may enable EAS to recognize
 * some files that it would ordinarily reject. The negative is that
 * it make take slightly longer to process the EAS_OpenFile request.
 *
 * Inputs:
 * pEASData             - instance data handle
 * searchFlag           - search flag (EAS_TRUE or EAS_FALSE)
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetHeaderSearchFlag (EAS_DATA_HANDLE pEASData, EAS_BOOL searchFlag)
{
    pEASData->searchHeaderFlag = (EAS_BOOL8) searchFlag;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_SetPlayMode()
 *----------------------------------------------------------------------------
 * Some file formats support special play modes, such as iMode partial
 * play mode. This call can be used to change the play mode. The
 * default play mode (usually straight playback) is always zero.
 *
 * Inputs:
 * pEASData             - instance data handle
 * handle               - file or stream handle
 * playMode             - play mode (see file parser for specifics)
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetPlayMode (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_I32 playMode)
{
    return EAS_IntSetStrmParam(pEASData, pStream, PARSER_DATA_PLAY_MODE, playMode);
}

#ifdef DLS_SYNTHESIZER
/*----------------------------------------------------------------------------
 * EAS_LoadDLSCollection()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the location of the sound library.
 *
 * Inputs:
 * pEASData             - instance data handle
 * pSoundLib            - pointer to sound library
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_LoadDLSCollection (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_FILE_LOCATOR locator)
{
    EAS_FILE_HANDLE fileHandle;
    EAS_RESULT result;
    EAS_DLSLIB_HANDLE pDLS;

    if (pStream != NULL)
    {
        if (!EAS_StreamReady(pEASData, pStream))
            return EAS_ERROR_NOT_VALID_IN_THIS_STATE;
    }

    /* open the file */
    if ((result = EAS_HWOpenFile(pEASData->hwInstData, locator, &fileHandle, EAS_FILE_READ)) != EAS_SUCCESS)
        return result;

    /* parse the file */
    result = DLSParser(pEASData->hwInstData, fileHandle, 0, &pDLS);
    EAS_HWCloseFile(pEASData->hwInstData, fileHandle);

    if (result == EAS_SUCCESS)
    {

        /* if a stream pStream is specified, point it to the DLS collection */
        if (pStream)
            result = EAS_IntSetStrmParam(pEASData, pStream, PARSER_DATA_DLS_COLLECTION, (EAS_I32) pDLS);

        /* global DLS load */
        else
            result = VMSetGlobalDLSLib(pEASData, pDLS);
    }

    return result;
}

EAS_PUBLIC void EAS_GetGlobalDLSLib (EAS_DATA_HANDLE pEASData, EAS_DLSLIB_HANDLE *ppDls) {
    *ppDls = pEASData->pVoiceMgr->pGlobalDLS;
    DLSAddRef(*ppDls);
}

EAS_PUBLIC void EAS_SetGlobalDLSLib (EAS_DATA_HANDLE pEASData, EAS_DLSLIB_HANDLE pDls) {
    if (pDls != NULL) {
        VMSetGlobalDLSLib(pEASData, pDls);
        DLSAddRef(pDls);
    }
}
#endif

#ifdef EXTERNAL_AUDIO
/*----------------------------------------------------------------------------
 * EAS_RegExtAudioCallback()
 *----------------------------------------------------------------------------
 * Purpose:
 * Registers callback functions for audio events.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - file or stream handle
 * cbProgChgFunc    - pointer to host callback function for program change
 * cbEventFunc      - pointer to host callback functio for note events
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_RegExtAudioCallback (EAS_DATA_HANDLE pEASData,
    EAS_HANDLE pStream,
    EAS_VOID_PTR pInstData,
    EAS_EXT_PRG_CHG_FUNC cbProgChgFunc,
    EAS_EXT_EVENT_FUNC cbEventFunc)
{
    S_SYNTH *pSynth;

    if (!EAS_StreamReady(pEASData, pStream))
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;

    if (EAS_GetStreamParameter(pEASData, pStream, PARSER_DATA_SYNTH_HANDLE, (EAS_I32*) &pSynth) != EAS_SUCCESS)
        return EAS_ERROR_INVALID_PARAMETER;

    if (pSynth == NULL)
        return EAS_ERROR_INVALID_PARAMETER;

    VMRegExtAudioCallback(pSynth, pInstData, cbProgChgFunc, cbEventFunc);
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_GetMIDIControllers()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the current state of MIDI controllers on the requested channel.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - file or stream handle
 * pControl         - pointer to structure to receive data
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_GetMIDIControllers (EAS_DATA_HANDLE pEASData, EAS_HANDLE pStream, EAS_U8 channel, S_MIDI_CONTROLLERS *pControl)
{
    S_SYNTH *pSynth;

    if (!EAS_StreamReady(pEASData, pStream))
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;

    if (EAS_GetStreamParameter(pEASData, pStream, PARSER_DATA_SYNTH_HANDLE, (EAS_I32*) &pSynth) != EAS_SUCCESS)
        return EAS_ERROR_INVALID_PARAMETER;

    if (pSynth == NULL)
        return EAS_ERROR_INVALID_PARAMETER;

    VMGetMIDIControllers(pSynth, channel, pControl);
    return EAS_SUCCESS;
}
#endif

#ifdef _SPLIT_ARCHITECTURE
/*----------------------------------------------------------------------------
 * EAS_SetFrameBuffer()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the frame buffer pointer passed to the IPC communications functions
 *
 * Inputs:
 * pEASData             - instance data handle
 * locator              - file locator
 *
 * Outputs:
 *
 *
 * Side Effects:
 * May overlay instruments in the GM sound set
 *
 *----------------------------------------------------------------------------
*/
EAS_PUBLIC EAS_RESULT EAS_SetFrameBuffer (EAS_DATA_HANDLE pEASData, EAS_FRAME_BUFFER_HANDLE pFrameBuffer)
{
    if (pEASData->pVoiceMgr)
        pEASData->pVoiceMgr->pFrameBuffer = pFrameBuffer;
    return EAS_SUCCESS;
}
#endif

#ifdef FILE_HEADER_SEARCH
/*----------------------------------------------------------------------------
 * EAS_SearchFile
 *----------------------------------------------------------------------------
 * Search file for specific sequence starting at current file
 * position. Returns offset to start of sequence.
 *
 * Inputs:
 * pEASData         - pointer to EAS persistent data object
 * fileHandle       - file handle
 * searchString     - pointer to search sequence
 * len              - length of search sequence
 * pOffset          - pointer to variable to store offset to sequence
 *
 * Returns EAS_EOF if end-of-file is reached
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_SearchFile (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, const EAS_U8 *searchString, EAS_I32 len, EAS_I32 *pOffset)
{
    EAS_RESULT result;
    EAS_INT index;
    EAS_U8 c;

    *pOffset = -1;
    index = 0;
    for (;;)
    {
        result = EAS_HWGetByte(pEASData->hwInstData, fileHandle, &c);
        if (result != EAS_SUCCESS)
            return result;
        if (c == searchString[index])
        {
            index++;
            if (index == 4)
            {
                result = EAS_HWFilePos(pEASData->hwInstData, fileHandle, pOffset);
                if (result != EAS_SUCCESS)
                    return result;
                *pOffset -= len;
                break;
            }
        }
        else
            index = 0;
    }
    return EAS_SUCCESS;
}
#endif


