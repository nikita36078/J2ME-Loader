/*----------------------------------------------------------------------------
 *
 * File:
 * eas_pcm.c
 *
 * Contents and purpose:
 * Implements the PCM engine including ADPCM decode for SMAF and CMX audio playback.
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
 *   $Revision: 849 $
 *   $Date: 2007-08-28 08:59:11 -0700 (Tue, 28 Aug 2007) $
 *----------------------------------------------------------------------------
*/

#include "eas_data.h"
#include "eas_report.h"
#include "eas_host.h"
#include "eas_config.h"
#include "eas_parser.h"
#include "eas_pcm.h"
#include "eas_math.h"
#include "eas_mixer.h"

#define PCM_MIXER_GUARD_BITS (NUM_MIXER_GUARD_BITS + 1)

/*----------------------------------------------------------------------------
 * Decoder interfaces
 *----------------------------------------------------------------------------
*/

static EAS_RESULT LinearPCMDecode (EAS_DATA_HANDLE pEASData, S_PCM_STATE *pState);
static EAS_RESULT LinearPCMLocate (EAS_DATA_HANDLE pEASData, S_PCM_STATE *pState, EAS_I32 time);

static const S_DECODER_INTERFACE PCMDecoder =
{
    NULL,
    LinearPCMDecode,
    LinearPCMLocate,
};

/* SMAF ADPCM decoder */
#ifdef _SMAF_PARSER
extern S_DECODER_INTERFACE SmafDecoder;
#define SMAF_DECODER &SmafDecoder
extern S_DECODER_INTERFACE Smaf7BitDecoder;
#define SMAF_7BIT_DECODER &Smaf7BitDecoder
#else
#define SMAF_DECODER NULL
#define SMAF_7BIT_DECODER NULL
#endif

/* IMA ADPCM decoder */
#ifdef _IMA_DECODER
extern S_DECODER_INTERFACE IMADecoder;
#define IMA_DECODER &IMADecoder
#else
#define IMA_DECODER NULL
#endif

static const S_DECODER_INTERFACE * const decoders[] =
{
    &PCMDecoder,
    SMAF_DECODER,
    IMA_DECODER,
    SMAF_7BIT_DECODER
};

/*----------------------------------------------------------------------------
 * Sample rate conversion
 *----------------------------------------------------------------------------
*/

#define SRC_RATE_MULTIPLER (0x40000000 / _OUTPUT_SAMPLE_RATE)

#ifdef _LOOKUP_SAMPLE_RATE
static const EAS_U32 srcConvRate[][2] =
{
    4000L, (4000L << 15) / _OUTPUT_SAMPLE_RATE,
    8000L, (8000L << 15) / _OUTPUT_SAMPLE_RATE,
    11025L, (11025L << 15) / _OUTPUT_SAMPLE_RATE,
    12000L, (12000L << 15) / _OUTPUT_SAMPLE_RATE,
    16000L, (16000L << 15) / _OUTPUT_SAMPLE_RATE,
    22050L, (22050L << 15) / _OUTPUT_SAMPLE_RATE,
    24000L, (24000L << 15) / _OUTPUT_SAMPLE_RATE,
    32000L, (32000L << 15) / _OUTPUT_SAMPLE_RATE
};
static EAS_U32 CalcBaseFreq (EAS_U32 sampleRate);
#define SRC_CONV_RATE_ENTRIES (sizeof(srcConvRate)/sizeof(EAS_U32)/2)
#endif


/* interface prototypes */
static EAS_RESULT RenderPCMStream (S_EAS_DATA *pEASData, S_PCM_STATE *pState, EAS_I32 numSamples);


/* local prototypes */
static S_PCM_STATE *FindSlot (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, EAS_PCM_CALLBACK pCallbackFunc, EAS_VOID_PTR cbInstData);
static EAS_RESULT InitPCMStream (S_EAS_DATA *pEASData, S_PCM_STATE *pState);

/*----------------------------------------------------------------------------
 * EAS_PEInit()
 *----------------------------------------------------------------------------
 * Purpose:
 * Initializes the PCM engine
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_PEInit (S_EAS_DATA *pEASData)
{
    S_PCM_STATE *pState;
    EAS_INT i;

    /* check for static memory allocation */
    if (pEASData->staticMemoryModel)
        pEASData->pPCMStreams = EAS_CMEnumData(EAS_CM_PCM_DATA);
    /* allocate dynamic memory */
    else
        pEASData->pPCMStreams = EAS_HWMalloc(pEASData->hwInstData, sizeof(S_PCM_STATE) * MAX_PCM_STREAMS);

    if (!pEASData->pPCMStreams)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_FATAL, "Failed to allocate memory for PCM streams\n"); */ }
        return EAS_ERROR_MALLOC_FAILED;
    }

    //zero the memory to insure complete initialization
    EAS_HWMemSet((void *)(pEASData->pPCMStreams),0, sizeof(S_PCM_STATE) * MAX_PCM_STREAMS);

    /* initialize the state data */
    for (i = 0, pState = pEASData->pPCMStreams; i < MAX_PCM_STREAMS; i++, pState++)
        pState->fileHandle = NULL;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_PEShutdown()
 *----------------------------------------------------------------------------
 * Purpose:
 * Shuts down the PCM engine
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_PEShutdown (S_EAS_DATA *pEASData)
{

    /* free any dynamic memory */
    if (!pEASData->staticMemoryModel)
    {
        if (pEASData->pPCMStreams)
        {
            EAS_HWFree(pEASData->hwInstData, pEASData->pPCMStreams);
            pEASData->pPCMStreams = NULL;
        }
    }
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_PERender()
 *----------------------------------------------------------------------------
 * Purpose:
 * Render a buffer of PCM audio
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_PERender (S_EAS_DATA* pEASData, EAS_I32 numSamples)
{
    S_PCM_STATE *pState;
    EAS_RESULT result;
    EAS_INT i;

    /* render all the active streams */
    for (i = 0, pState = pEASData->pPCMStreams; i < MAX_PCM_STREAMS; i++, pState++)
    {
        if ((pState->fileHandle) && (pState->state != EAS_STATE_STOPPED) && (pState->state != EAS_STATE_PAUSED))
            if ((result = RenderPCMStream(pEASData, pState, numSamples)) != EAS_SUCCESS)
                return result;
    }
    return EAS_SUCCESS;
}


/*----------------------------------------------------------------------------
 * EAS_PEState()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the current state of the stream
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 * pState           - pointer to variable to store state
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 * Notes:
 * This interface is also exposed in the internal library for use by the other modules.
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
EAS_RESULT EAS_PEState (S_EAS_DATA *pEASData, EAS_PCM_HANDLE pInstData, EAS_STATE *pState)
{
    /* return current state */
    *pState = pInstData->state;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_PEClose()
 *----------------------------------------------------------------------------
 * Purpose:
 * Close the file and clean up
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_PEClose (S_EAS_DATA *pEASData, EAS_PCM_HANDLE pState)
{
    EAS_RESULT result;

    if ((result = EAS_HWCloseFile(pEASData->hwInstData, pState->fileHandle)) != EAS_SUCCESS)
        return result;

    pState->fileHandle = NULL;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * PCM_Reset()
 *----------------------------------------------------------------------------
 * Purpose:
 * Reset the sequencer. Used for locating backwards in the file.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_PEReset (S_EAS_DATA *pEASData, EAS_PCM_HANDLE pState)
{
    EAS_RESULT result;

    /* reset file position to first byte of data in the stream */
    if ((result = EAS_HWFileSeek(pEASData->hwInstData, pState->fileHandle, pState->startPos)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Error %d seeking to start of PCM file\n", result); */ }
        return result;
    }

    /* re-initialize stream */
    return InitPCMStream(pEASData, pState);
}

/*----------------------------------------------------------------------------
 * EAS_PEOpenStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Starts up a PCM playback
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_PEOpenStream (S_EAS_DATA *pEASData, S_PCM_OPEN_PARAMS *pParams, EAS_PCM_HANDLE *pHandle)
{
    EAS_RESULT result;
    S_PCM_STATE *pState;
    EAS_I32 filePos;

    /* make sure we support this decoder */
    if (pParams->decoder >= NUM_DECODER_MODULES)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Decoder selector out of range\n"); */ }
        return EAS_ERROR_PARAMETER_RANGE;
    }
    if (decoders[pParams->decoder] == NULL)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Decoder module not available\n"); */ }
        return EAS_ERROR_FEATURE_NOT_AVAILABLE;
    }

    /* find a slot for the new stream */
    if ((pState = FindSlot(pEASData, pParams->fileHandle, pParams->pCallbackFunc, pParams->cbInstData)) == NULL)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Unable to open ADPCM stream, too many streams open\n"); */ }
        return EAS_ERROR_MAX_PCM_STREAMS;
    }

    /* get the current file position */
    if ((result = EAS_HWFilePos(pEASData->hwInstData, pState->fileHandle, &filePos)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "EAS_HWFilePos returned %ld\n",result); */ }
        pState->fileHandle = NULL;
        return result;
    }

    pState->pDecoder = decoders[pParams->decoder];
    pState->startPos = filePos;
    pState->bytesLeftLoop = pState->byteCount = pParams->size;
    pState->loopStart = pParams->loopStart;
    pState->samplesTilLoop = (EAS_I32) pState->loopStart;
    pState->loopSamples = pParams->loopSamples;
    pState->samplesInLoop = 0;
    pState->blockSize = (EAS_U16) pParams->blockSize;
    pState->flags = pParams->flags;
    pState->envData = pParams->envData;
    pState->volume = pParams->volume;
    pState->sampleRate = (EAS_U16) pParams->sampleRate;

    /* set the base frequency */
    pState->basefreq = (SRC_RATE_MULTIPLER * (EAS_U32) pParams->sampleRate) >> 15;

    /* calculate shift for frequencies > 1.0 */
    pState->rateShift = 0;
    while (pState->basefreq > 32767)
    {
        pState->basefreq = pState->basefreq >> 1;
        pState->rateShift++;
    }

    /* initialize */
    if ((result = InitPCMStream(pEASData, pState)) != EAS_SUCCESS)
        return result;

    *pHandle = pState;

    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "EAS_PEOpenStream: StartPos=%d, byteCount = %d, loopSamples=%d\n",
        pState->startPos, pState->byteCount, pState->loopSamples); */ }
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_PEContinueStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Continues a PCM stream
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -e{715} reserved for future use */
EAS_RESULT EAS_PEContinueStream (S_EAS_DATA *pEASData, EAS_PCM_HANDLE pState, EAS_I32 size)
{

    /* add new samples to count */
    pState->bytesLeft += size;
    if (pState->bytesLeft > 0)
        pState->flags &= ~PCM_FLAGS_EMPTY;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_PEGetFileHandle()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the file handle of a stream
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
EAS_RESULT EAS_PEGetFileHandle (S_EAS_DATA *pEASData, EAS_PCM_HANDLE pState, EAS_FILE_HANDLE *pFileHandle)
{
    *pFileHandle = pState->fileHandle;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_PEUpdateParams()
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the pitch and volume parameters for a PCM stream
 *
 * Inputs:
 * pEASData         - pointer to EAS library instance data
 * handle           - pointer to S_PCM_STATE for this stream
 * gainLeft         - linear gain multipler in 1.15 fraction format
 * gainRight        - linear gain multipler in 1.15 fraction format
 * pitch            - pitch shift in cents
 * initial          - initial settings, set current gain
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 * Notes
 * In mono mode, leftGain controls the output gain and rightGain is ignored
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
/*lint -esym(715, gainRight) used only in 2-channel version */
EAS_RESULT EAS_PEUpdateParams (S_EAS_DATA* pEASData, EAS_PCM_HANDLE pState, EAS_I16 pitch, EAS_I16 gainLeft, EAS_I16 gainRight)
{

    pState->gainLeft = gainLeft;

#if (NUM_OUTPUT_CHANNELS == 2)
    pState->gainRight = gainRight;
#endif

    pState->pitch = pitch;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_PELocate()
 *----------------------------------------------------------------------------
 * Purpose:
 * This function seeks to the requested place in the file. Accuracy
 * is dependent on the sample rate and block size.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * pState           - stream handle
 * time             - media time in milliseconds
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_PELocate (S_EAS_DATA *pEASData, EAS_PCM_HANDLE pState, EAS_I32 time)
{
    if (pState->pDecoder->pfLocate == NULL)
        return EAS_ERROR_FEATURE_NOT_AVAILABLE;

    return pState->pDecoder->pfLocate(pEASData, pState, time);
}

/*----------------------------------------------------------------------------
 * EAS_PEUpdateVolume()
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the volume parameters for a PCM stream
 *
 * Inputs:
 * pEASData         - pointer to EAS library instance data
 * handle           - pointer to S_PCM_STATE for this stream
 * gainLeft         - linear gain multipler in 1.15 fraction format
 * gainRight        - linear gain multipler in 1.15 fraction format
 * initial          - initial settings, set current gain
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 * Notes
 * In mono mode, leftGain controls the output gain and rightGain is ignored
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
EAS_RESULT EAS_PEUpdateVolume (S_EAS_DATA* pEASData, EAS_PCM_HANDLE pState, EAS_I16 volume)
{
    pState->volume = volume;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_PEUpdatePitch()
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the pitch parameter for a PCM stream
 *
 * Inputs:
 * pEASData         - pointer to EAS library instance data
 * pState           - pointer to S_PCM_STATE for this stream
 * pitch            - new pitch value in pitch cents
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
EAS_RESULT EAS_PEUpdatePitch (S_EAS_DATA* pEASData, EAS_PCM_HANDLE pState, EAS_I16 pitch)
{
    pState->pitch = pitch;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_PEPause()
 *----------------------------------------------------------------------------
 * Purpose:
 * Mute and stop rendering a PCM stream. Sets the gain target to zero and stops the playback
 * at the end of the next audio frame.
 *
 * Inputs:
 * pEASData         - pointer to EAS library instance data
 * handle           - pointer to S_PCM_STATE for this stream
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
EAS_RESULT EAS_PEPause (S_EAS_DATA *pEASData, EAS_PCM_HANDLE pState)
{
    /* set state to stopping */
    pState->state = EAS_STATE_PAUSING;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_PEResume()
 *----------------------------------------------------------------------------
 * Purpose:
 * Resume rendering a PCM stream. Sets the gain target back to its
 * previous setting and restarts playback at the end of the next audio
 * frame.
 *
 * Inputs:
 * pEASData         - pointer to EAS library instance data
 * handle           - pointer to S_PCM_STATE for this stream
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
EAS_RESULT EAS_PEResume (S_EAS_DATA *pEASData, EAS_PCM_HANDLE pState)
{
    /* set state to stopping */
    pState->state = EAS_STATE_PLAY;
    return EAS_SUCCESS;
}

EAS_U32 getDecayScale(EAS_U32 index)
{
    EAS_U32 utemp;

    //envelope decay segment
    switch (index)
    {
    case 0: //no decay
        utemp = 512;//32768;
        break;
    case 1: //.0156 dB per update
        utemp = 511;//32709;
        break;
    case 2: //.03125
        utemp = 510;//32649;
        break;
    case 3: //.0625
        utemp = 508;//32532;
        break;
    case 4: //.125
        utemp = 505;//32298;
        break;
    case 5: //.25
        utemp = 497;//31835;
        break;
    case 6: //.5
        utemp = 483;//30929;
        break;
    case 7: //1.0
        utemp = 456;//29193;
        break;
    case 8: //2.0
        utemp = 406;//26008;
        break;
    case 9: //4.0
        utemp = 323;//20642;
        break;
    case 10: //8.0
        utemp = 203;//13004;
        break;
    case 11: //16.0
        utemp = 81;//5160;
        break;
    case 12: //32.0
        utemp = 13;//813;
        break;
    case 13: //64.0
        utemp = 0;//20;
        break;
    case 14: //128.0
        utemp = 0;
        break;
    case 15: //256.0
    default:
        utemp = 0;
        break;
    }
    //printf("getdecayscale returned %d\n",utemp);
    return utemp;
}

EAS_U32 getAttackIncrement(EAS_U32 index)
{
    EAS_U32 utemp;

    //envelope decay segment
    switch (index)
    {
    case 0:
        utemp = 32;
        break;
    case 1:
        utemp = 64;
        break;
    case 2:
        utemp = 128;
        break;
    case 3:
        utemp = 256;
        break;
    case 4:
        utemp = 512;
        break;
    case 5:
        utemp = 1024;
        break;
    case 6:
        utemp = 2048;
        break;
    case 7:
        utemp = 4096;
        break;
    case 8:
        utemp = 8192;
        break;
    case 9:
        utemp = 16384;
        break;
    case 10:
        utemp = 32768;
        break;
    case 11:
        utemp = 65536;
        break;
    case 12:
        utemp = 65536;
        break;
    case 13:
        utemp = 65536;
        break;
    case 14:
        utemp = 65535;
        break;
    case 15:
    default:
        utemp = 0;
        break;
    }
    //printf("getattackincrement returned %d\n",utemp);
    return utemp;
}

/*----------------------------------------------------------------------------
 * EAS_PERelease()
 *----------------------------------------------------------------------------
 * Purpose:
 * Put the PCM stream envelope into release.
 *
 * Inputs:
 * pEASData         - pointer to EAS library instance data
 * handle           - pointer to S_PCM_STATE for this stream
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
EAS_RESULT EAS_PERelease (S_EAS_DATA *pEASData, EAS_PCM_HANDLE pState)
{
    EAS_U32 utemp;

    //printf("handling note-off part of envelope\n");
    /*if the note is not ignore release or sustained*/
    if (((pState->envData >> 24) & 0x0F)==0)
    {
        /* set envelope state to release */
        pState->envState = PCM_ENV_RELEASE;
        utemp = ((pState->envData >> 20) & 0x0F);
        pState->envScale = getDecayScale(utemp); //getReleaseScale(utemp);
    }
    else
    {
        /*else change envelope state to sustain */
        pState->envState = PCM_ENV_SUSTAIN;
        utemp = ((pState->envData >> 28) & 0x0F);
        pState->envScale = getDecayScale(utemp); //getSustainScale(utemp);
    }
    //since we are in release, don't let anything hang around too long
    //printf("checking env scale, val = %d\n",((S_PCM_STATE*) handle)->envScale);
    if (pState->envScale > 505)
        pState->envScale = 505;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * FindSlot()
 *----------------------------------------------------------------------------
 * Purpose:
 * Locates an empty stream slot and assigns the file handle
 *
 * Inputs:
 * pEASData         - pointer to EAS library instance data
 * fileHandle       - file handle
 * pCallbackFunc    - function to be called back upon EAS_STATE_STOPPED
 *
 * Outputs:
 * returns handle to slot or NULL if all slots are used
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static S_PCM_STATE *FindSlot (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, EAS_PCM_CALLBACK pCallbackFunc, EAS_VOID_PTR cbInstData)
{
    EAS_INT i;
    S_PCM_STATE *pState;

#ifndef NO_PCM_STEAL
    S_PCM_STATE *foundState = NULL;
    EAS_INT count = 0;
    EAS_U32 startOrder = 0xFFFFFFFF;
    S_PCM_STATE *stealState = NULL;
    EAS_U32 youngest = 0;

    /* find an empty slot, count total in use, and find oldest in use (lowest start order) */
    for (i = 0, pState = pEASData->pPCMStreams; i < MAX_PCM_STREAMS; i++, pState++)
    {
        /* if this one is available */
        if (pState->fileHandle == NULL)
        {
            foundState = pState;
        }
        /* else this one is in use, so see if it is the oldest, and count total in use */
        /* also find youngest */
        else
        {
            /*one more voice in use*/
            count++;
            /* is this the oldest? (lowest start order) */
            if ((pState->state != EAS_STATE_STOPPING) && (pState->startOrder < startOrder))
            {
                /* remember this one */
                stealState = pState;
                /* remember the oldest so far */
                startOrder = pState->startOrder;
            }
            /* is this the youngest? (highest start order) */
            if (pState->startOrder >= youngest)
            {
                youngest = pState->startOrder;
            }
        }
    }

    /* if there are too many voices active, stop the oldest one */
    if (count > PCM_STREAM_THRESHOLD)
    {
        //printf("stealing!!!\n");
        /* make sure we got one, although we should always have one at this point */
        if (stealState != NULL)
        {
            //flag this as stopping, so it will get shut off
            stealState->state = EAS_STATE_STOPPING;
        }
    }

    /* if there are no available open streams (we won't likely see this, due to stealing) */
    if (foundState == NULL)
        return NULL;

    /* save info */
    foundState->startOrder = youngest + 1;
    foundState->fileHandle = fileHandle;
    foundState->pCallback = pCallbackFunc;
    foundState->cbInstData = cbInstData;
    return foundState;
#else
    /* find an empty slot*/
    for (i = 0; i < MAX_PCM_STREAMS; i++)
    {
        pState = &pEASData->pPCMStreams[i];
        if (pState->fileHandle != NULL)
            continue;

        pState->fileHandle = fileHandle;
        pState->pCallback = pCallbackFunc;
        pState->cbInstData = cbInstData;
        return pState;
    }
    return NULL;
#endif
}

#ifdef _LOOKUP_SAMPLE_RATE
/*----------------------------------------------------------------------------
 * CalcBaseFreq()
 *----------------------------------------------------------------------------
 * Purpose:
 * Calculates the fractional phase increment for the sample rate converter
 *
 * Inputs:
 * sampleRate       - sample rate in samples/sec
 *
 * Outputs:
 * Returns fractional sample rate with a 15-bit fraction
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_U32 CalcBaseFreq (EAS_U32 sampleRate)
{
    EAS_INT i;

    /* look up the conversion rate */
    for (i = 0; i < (EAS_INT)(SRC_CONV_RATE_ENTRIES); i ++)
    {
        if (srcConvRate[i][0] == sampleRate)
            return srcConvRate[i][1];
    }

    /* if not found in table, do it the long way */
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Sample rate %u not in table, calculating by division\n", sampleRate); */ }

    return (SRC_RATE_MULTIPLER * (EAS_U32) sampleRate) >> 15;
}
#endif

/*----------------------------------------------------------------------------
 * InitPCMStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Start an ADPCM stream playback. Decodes the header, preps the engine.
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT InitPCMStream (S_EAS_DATA *pEASData, S_PCM_STATE *pState)
{

    /* initialize the data structure */
    pState->bytesLeft = pState->byteCount;
    pState->phase = 0;
    pState->srcByte = 0;
    pState->decoderL.acc = 0;
    pState->decoderL.output = 0;
    pState->decoderL.x0 = pState->decoderL.x1 = 0;
    pState->decoderL.step = 0;
    pState->decoderR.acc = 0;
    pState->decoderR.output = 0;
    pState->decoderR.x0 = pState->decoderR.x1 = 0;
    pState->decoderR.step = 0;
    pState->hiNibble = EAS_FALSE;
    pState->pitch = 0;
    pState->blockCount = 0;
    pState->gainLeft = PCM_DEFAULT_GAIN_SETTING;
//  pState->currentGainLeft = PCM_DEFAULT_GAIN_SETTING;
    pState->envValue = 0;
    pState->envState = PCM_ENV_START;

#if (NUM_OUTPUT_CHANNELS == 2)
    pState->gainRight = PCM_DEFAULT_GAIN_SETTING;
//  pState->currentGainRight = PCM_DEFAULT_GAIN_SETTING;
#endif
    pState->state = EAS_STATE_READY;

    /* initialize the decoder */
    if (pState->pDecoder->pfInit)
        return (*pState->pDecoder->pfInit)(pEASData, pState);
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * RenderPCMStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Decodes a buffer of ADPCM data.
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT RenderPCMStream (S_EAS_DATA *pEASData, S_PCM_STATE *pState, EAS_I32 numSamples)
{
    EAS_RESULT result;
    EAS_U32 phaseInc;
    EAS_I32 gainLeft, gainIncLeft;
    EAS_I32 *pOut;
    EAS_I32 temp;
    EAS_U32 utemp;

#if (NUM_OUTPUT_CHANNELS == 2)
    EAS_I32 gainRight, gainIncRight;
#endif

#if 0
    printf("env data: AR = %d, DR = %d, SL = %d, SR = %d, RR = %d\n",
        ((pState->envData >> 12) & 0x0F),
        ((pState->envData >> 16) & 0x0F),
        ((pState->envData >> 8) & 0x0F),
        ((pState->envData >> 28) & 0x0F),
        ((pState->envData >> 20) & 0x0F));
#endif

    if (pState->envState == PCM_ENV_START)
    {
        //printf("env start\n");
        utemp = ((pState->envData >> 12) & 0x0F);
        //if fastest rate, attack is already completed
        //do the same for slowest rate, since that allows zero to be passed for default envelope
        if (utemp == 0x0F || utemp == 0x00)
        {
            //start envelope at full
            pState->envValue = (32768<<7);
            //jump right into decay
            utemp = ((pState->envData >> 16) & 0x0F);
            pState->envScale = getDecayScale(utemp);
            pState->envState = PCM_ENV_DECAY;
            pState->currentGainLeft = (EAS_I16) FMUL_15x15(pState->gainLeft, pState->volume);
            pState->currentGainRight = (EAS_I16) FMUL_15x15(pState->gainRight, pState->volume);
        }
        //else attack has a ramp
        else
        {
            //start the envelope very low
            pState->envValue = (2<<7);
            pState->currentGainLeft = 0;
            pState->currentGainRight = 0;
            //get envelope attack scaling value
            pState->envScale = getAttackIncrement(utemp);
            //go to attack state
            pState->envState = PCM_ENV_ATTACK;
        }
    }
    if (pState->envState == PCM_ENV_ATTACK)
    {
        //printf("env attack, env value = %d, env scale = %d\n",pState->envValue>>7,pState->envScale);
        //update envelope value
        pState->envValue = pState->envValue + (pState->envScale << 7);
        //check envelope level and update state if needed
        if (pState->envValue >= (32768<<7))
        {
            pState->envValue = (32768<<7);
            utemp = ((pState->envData >> 16) & 0x0F);
            pState->envScale = getDecayScale(utemp);
            pState->envState = PCM_ENV_DECAY;
        }
    }
    else if (pState->envState == PCM_ENV_DECAY)
    {
        //printf("env decay, env value = %d, env scale = %d\n",pState->envValue>>7,pState->envScale);
        //update envelope value
        pState->envValue = (pState->envValue * pState->envScale)>>9;
        //check envelope level against sustain level and update state if needed
        utemp = ((pState->envData >> 8) & 0x0F);
        if (utemp == (EAS_U32)0x0F)
            utemp = (2<<7);
        else
        {
            utemp = ((32769<<7) >> (utemp>>1));
        }
        if (pState->envValue <= utemp)
        {
            utemp = ((pState->envData >> 28) & 0x0F);
            pState->envScale = getDecayScale(utemp); //getSustainScale(utemp);
            pState->envState = PCM_ENV_SUSTAIN;
        }
    }
    else if (pState->envState == PCM_ENV_SUSTAIN)
    {
        //printf("env sustain, env value = %d, env scale = %d\n",pState->envValue>>7,pState->envScale);
        //update envelope value
        pState->envValue = (pState->envValue * pState->envScale)>>9;
        //check envelope level against bottom level and update state if needed
        if (pState->envValue <= (2<<7))
        {
            //no more decay
            pState->envScale = 512;
            pState->envState = PCM_ENV_END;
        }
    }
    else if (pState->envState == PCM_ENV_RELEASE)
    {
        //printf("env release, env value = %d, env scale = %d\n",pState->envValue>>7,pState->envScale);
        //update envelope value
        pState->envValue = (pState->envValue * pState->envScale)>>9;
        //check envelope level against bottom level and update state if needed
        if (pState->envValue <= (2<<7))
        {
            //no more decay
            pState->envScale = 512;
            pState->envState = PCM_ENV_END;
        }
    }
    else if (pState->envState == PCM_ENV_END)
    {
        //printf("env end\n");
        /* set state to stopping, already ramped down */
        pState->state = EAS_STATE_STOPPING;
    }

    //pState->gainLeft = (EAS_U16)((pState->gainLeft * (pState->envValue>>7))>>15);
    //pState->gainRight = (EAS_U16)((pState->gainRight * (pState->envValue>>7))>>15);

    /* gain to 32-bits to increase resolution on anti-zipper filter */
    /*lint -e{703} use shift for performance */
    gainLeft = (EAS_I32) pState->currentGainLeft << SYNTH_UPDATE_PERIOD_IN_BITS;
#if (NUM_OUTPUT_CHANNELS == 2)
    /*lint -e{703} use shift for performance */
    gainRight = (EAS_I32) pState->currentGainRight << SYNTH_UPDATE_PERIOD_IN_BITS;
#endif

    /* calculate a new gain increment, gain target is zero if pausing */
    if ((pState->state == EAS_STATE_PAUSING) || (pState->state == EAS_STATE_PAUSED))
    {
        gainIncLeft = -pState->currentGainLeft;
#if (NUM_OUTPUT_CHANNELS == 2)
        gainIncRight= -pState->currentGainRight;
#endif
    }
    else
    {
        EAS_I32 gain = FMUL_15x15(pState->envValue >> 7, pState->volume);
        gainIncLeft = FMUL_15x15(pState->gainLeft, gain) - pState->currentGainLeft;
#if (NUM_OUTPUT_CHANNELS == 2)
        gainIncRight = FMUL_15x15(pState->gainRight, gain) - pState->currentGainRight;
#endif
    }

    /* calculate phase increment */
    phaseInc = pState->basefreq;

    /* convert pitch cents to linear multiplier */
    if (pState->pitch)
    {
        temp = EAS_Calculate2toX(pState->pitch);
        phaseInc = FMUL_15x15(phaseInc, temp);
    }
    phaseInc = phaseInc << pState->rateShift;

    /* pointer to mix buffer */
    pOut = pEASData->pMixBuffer;

    /* render a buffer of samples */
    while (numSamples--)
    {

        /* interpolate an output sample */
        pState->decoderL.output = pState->decoderL.x0 + FMUL_15x15((pState->decoderL.x1 - pState->decoderL.x0), pState->phase & PHASE_FRAC_MASK);

        /* stereo output */
#if (NUM_OUTPUT_CHANNELS == 2)

        /* stereo stream? */
        if (pState->flags & PCM_FLAGS_STEREO)
            pState->decoderR.output = pState->decoderR.x0 + FMUL_15x15((pState->decoderR.x1 - pState->decoderR.x0), pState->phase & PHASE_FRAC_MASK);

        /* gain scale and mix */
        /*lint -e{704} use shift instead of division */
        *pOut++ += (pState->decoderL.output * (gainLeft >> SYNTH_UPDATE_PERIOD_IN_BITS)) >> PCM_MIXER_GUARD_BITS;
        gainLeft += gainIncLeft;

        /*lint -e{704} use shift instead of division */
        if (pState->flags & PCM_FLAGS_STEREO)
            *pOut++ += (pState->decoderR.output * (gainRight >> SYNTH_UPDATE_PERIOD_IN_BITS)) >> PCM_MIXER_GUARD_BITS;
        else
            *pOut++ += (pState->decoderL.output * (gainRight >> SYNTH_UPDATE_PERIOD_IN_BITS)) >> PCM_MIXER_GUARD_BITS;

        gainRight += gainIncRight;

        /* mono output */
#else
        /* if stereo stream, decode right channel and mix to mono */
        if (pState->flags & PCM_FLAGS_STEREO)
        {
            pState->decoderR.output= pState->decoderR.x0 + FMUL_15x15((pState->decoderR.x1 - pState->decoderR.x0), pState->phase & PHASE_FRAC_MASK);

            /* for mono, sum stereo ADPCM to mono */
            /*lint -e{704} use shift instead of division */
            *pOut++ += ((pState->decoderL.output + pState->decoderR.output) * (gainLeft >> SYNTH_UPDATE_PERIOD_IN_BITS)) >> PCM_MIXER_GUARD_BITS;
        }
        else
            /*lint -e{704} use shift instead of division */
            *pOut++ += (pState->decoderL.output * (gainLeft >> SYNTH_UPDATE_PERIOD_IN_BITS)) >> PCM_MIXER_GUARD_BITS;

        gainLeft += gainIncLeft;
#endif

        /* advance phase accumulator */
        pState->phase += phaseInc;

        /* if integer part of phase accumulator is non-zero, advance to next sample */
        while (pState->phase & ~PHASE_FRAC_MASK)
        {
            pState->decoderL.x0 = pState->decoderL.x1;
            pState->decoderR.x0 = pState->decoderR.x1;

            /* give the source a chance to continue the stream */
            if (!pState->bytesLeft && pState->pCallback && ((pState->flags & PCM_FLAGS_EMPTY) == 0))
            {
                pState->flags |= PCM_FLAGS_EMPTY;
                (*pState->pCallback)(pEASData, pState->cbInstData, pState, EAS_STATE_EMPTY);
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "RenderPCMStream: After empty callback, bytesLeft = %d\n", pState->bytesLeft); */ }
            }

            /* decode the next sample */
            if ((result = (*pState->pDecoder->pfDecodeSample)(pEASData, pState)) != EAS_SUCCESS)
                return result;

            /* adjust phase by one sample */
            pState->phase -= (1L << NUM_PHASE_FRAC_BITS);
        }

    }

    /* save new gain */
    /*lint -e{704} use shift instead of division */
    pState->currentGainLeft = (EAS_I16) (gainLeft >> SYNTH_UPDATE_PERIOD_IN_BITS);

#if (NUM_OUTPUT_CHANNELS == 2)
    /*lint -e{704} use shift instead of division */
    pState->currentGainRight = (EAS_I16) (gainRight >> SYNTH_UPDATE_PERIOD_IN_BITS);
#endif

    /* if pausing, set new state and notify */
    if (pState->state == EAS_STATE_PAUSING)
    {
        pState->state = EAS_STATE_PAUSED;
        if (pState->pCallback)
            (*pState->pCallback)(pEASData, pState->cbInstData, pState, pState->state);
    }

    /* if out of data, set stopped state and notify */
    if (pState->bytesLeft == 0 || pState->state == EAS_STATE_STOPPING)
    {
        pState->state = EAS_STATE_STOPPED;

        /* do callback unless the file has already been closed */
        if (pState->pCallback && pState->fileHandle)
            (*pState->pCallback)(pEASData, pState->cbInstData, pState, pState->state);
    }

    if (pState->state == EAS_STATE_READY)
        pState->state = EAS_STATE_PLAY;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * LinearPCMDecode()
 *----------------------------------------------------------------------------
 * Purpose:
 * Decodes a PCM sample
 *
 * Inputs:
 *
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT LinearPCMDecode (EAS_DATA_HANDLE pEASData, S_PCM_STATE *pState)
{
    EAS_RESULT result;
    EAS_HW_DATA_HANDLE hwInstData;

    hwInstData = ((S_EAS_DATA*) pEASData)->hwInstData;

    /* if out of data, check for loop */
    if ((pState->bytesLeft == 0) && (pState->loopSamples != 0))
    {
        if ((result = EAS_HWFileSeek(pEASData->hwInstData, pState->fileHandle, (EAS_I32) (pState->startPos + pState->loopLocation))) != EAS_SUCCESS)
            return result;
        pState->bytesLeft = pState->byteCount = (EAS_I32) pState->bytesLeftLoop;
        pState->flags &= ~PCM_FLAGS_EMPTY;
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "LinearPCMDecode: Rewind file to %d, bytesLeft = %d\n", pState->startPos, pState->bytesLeft); */ }
    }

    if (pState->bytesLeft)
    {

        /* check format byte for 8-bit samples */
        if (pState->flags & PCM_FLAGS_8_BIT)
        {
            /* fetch left or mono sample */
            if ((result = EAS_HWGetByte(hwInstData, pState->fileHandle, &pState->srcByte)) != EAS_SUCCESS)
                return result;

            /* if unsigned */
            if (pState->flags & PCM_FLAGS_UNSIGNED)
            {
                /*lint -e{734} converting unsigned 8-bit to signed 16-bit */
                pState->decoderL.x1 = (EAS_PCM)(((EAS_PCM) pState->srcByte << 8) ^ 0x8000);
            }
            else
            {
                /*lint -e{734} converting signed 8-bit to signed 16-bit */
                pState->decoderL.x1 = (EAS_PCM)((EAS_PCM) pState->srcByte << 8);
            }
            pState->bytesLeft--;

            /* fetch right sample */
            if(pState->flags & PCM_FLAGS_STEREO)
            {
                if ((result = EAS_HWGetByte(hwInstData, pState->fileHandle, &pState->srcByte)) != EAS_SUCCESS)
                    return result;

                /* if unsigned */
                if (pState->flags & PCM_FLAGS_UNSIGNED)
                {
                    /*lint -e{734} converting unsigned 8-bit to signed 16-bit */
                    pState->decoderR.x1 = (EAS_PCM)(((EAS_PCM) pState->srcByte << 8) ^ 0x8000);
                }
                else
                {
                    /*lint -e{734} converting signed 8-bit to signed 16-bit */
                    pState->decoderR.x1 = (EAS_PCM)((EAS_PCM) pState->srcByte << 8);
                }
                pState->bytesLeft--;
            }
        }

        /* must be 16-bit samples */
        else
        {
            //unsigned 16 bit currently not supported
            if (pState->flags & PCM_FLAGS_UNSIGNED)
            {
                return EAS_ERROR_INVALID_PCM_TYPE;
            }

            /* fetch left or mono sample */
            if ((result = EAS_HWGetWord(hwInstData, pState->fileHandle, &pState->decoderL.x1, EAS_FALSE)) != EAS_SUCCESS)
                return result;
            pState->bytesLeft -= 2;

            /* fetch right sample */
            if(pState->flags & PCM_FLAGS_STEREO)
            {
                if ((result = EAS_HWGetWord(hwInstData, pState->fileHandle, &pState->decoderR.x1, EAS_FALSE)) != EAS_SUCCESS)
                    return result;
                pState->bytesLeft -= 2;
            }
        }
    }

    /* no more data, force zero samples */
    else
        pState->decoderL.x1 = pState->decoderR.x1 = 0;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * LinearPCMLocate()
 *----------------------------------------------------------------------------
 * Purpose:
 * Locate in a linear PCM stream
 *----------------------------------------------------------------------------
*/
static EAS_RESULT LinearPCMLocate (EAS_DATA_HANDLE pEASData, S_PCM_STATE *pState, EAS_I32 time)
{
    EAS_RESULT result;
    EAS_I32 temp;
    EAS_I32 secs, msecs;
    EAS_INT shift;

    /* calculate size of sample frame */
    if (pState->flags & PCM_FLAGS_8_BIT)
        shift = 0;
    else
        shift = 1;
    if (pState->flags & PCM_FLAGS_STEREO)
        shift++;

    /* break down into secs and msecs */
    secs = time / 1000;
    msecs = time - (secs * 1000);

    /* calculate sample number fraction from msecs */
    temp = (msecs * pState->sampleRate);
    temp = (temp >> 10) + ((temp * 49) >> 21);

    /* add integer sample count */
    temp += secs * pState->sampleRate;

    /* calculate the position based on sample frame size */
    /*lint -e{703} use shift for performance */
    temp <<= shift;

    /* past end of sample? */
    if (temp > (EAS_I32) pState->loopStart)
    {
        /* if not looped, flag error */
        if (pState->loopSamples == 0)
        {
            pState->bytesLeft = 0;
            pState->flags |= PCM_FLAGS_EMPTY;
            return EAS_ERROR_LOCATE_BEYOND_END;
        }

        /* looped sample - calculate position in loop */
        while (temp > (EAS_I32) pState->loopStart)
            temp -= (EAS_I32) pState->loopStart;
    }

    /* seek to new position */
    if ((result = EAS_PESeek(pEASData, pState, &temp)) != EAS_SUCCESS)
        return result;

    /* reset state */
    if ((pState->state != EAS_STATE_PAUSING) && (pState->state != EAS_STATE_PAUSED))
        pState->state = EAS_STATE_READY;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_PESeek
 *----------------------------------------------------------------------------
 * Purpose:
 * Locate to a particular byte in a PCM stream
 *----------------------------------------------------------------------------
 * This bit is tricky because the chunks may not be contiguous,
 * so we have to rely on the parser to position in the file. We
 * do this by seeking to the end of each chunk and simulating an
 * empty buffer condition until we get to where we want to go.
 *
 * A better solution would be a parser API for re-positioning,
 * but there isn't time at the moment to re-factor all the
 * parsers to support a new API.
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_PESeek (S_EAS_DATA *pEASData, S_PCM_STATE *pState, EAS_I32 *pLocation)
{
    EAS_RESULT result;

    /* seek to start of audio */
    if ((result = EAS_HWFileSeek(pEASData->hwInstData, pState->fileHandle, pState->startPos)) != EAS_SUCCESS)
    {
        pState->state = EAS_STATE_ERROR;
        return result;
    }
    pState->bytesLeft = pState->bytesLeftLoop;

    /* skip through chunks until we find the right chunk */
    while (*pLocation > (EAS_I32) pState->bytesLeft)
    {
        /* seek to end of audio chunk */
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "EAS_PESeek: Seek to offset = %d\n", pState->bytesLeft); */ }
        if ((result = EAS_HWFileSeekOfs(pEASData->hwInstData, pState->fileHandle, pState->bytesLeft)) != EAS_SUCCESS)
        {
            pState->state = EAS_STATE_ERROR;
            return result;
        }
        *pLocation -= pState->bytesLeft;
        pState->bytesLeft = 0;
        pState->flags |= PCM_FLAGS_EMPTY;

        /* retrieve more data */
        if (pState->pCallback)
            (*pState->pCallback)(pEASData, pState->cbInstData, pState, EAS_STATE_EMPTY);

        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "EAS_PESeek: bytesLeft=%d, byte location = %d\n", pState->bytesLeft, *pLocation); */ }

        /* no more samples */
        if (pState->bytesLeft == 0)
            return EAS_ERROR_LOCATE_BEYOND_END;
    }

    /* seek to new offset in current chunk */
    if (*pLocation > 0)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "EAS_PESeek: Seek to offset = %d\n", *pLocation); */ }
        if ((result = EAS_HWFileSeekOfs(pEASData->hwInstData, pState->fileHandle, *pLocation)) != EAS_SUCCESS)
        {
            pState->state = EAS_STATE_ERROR;
            return result;
        }

        /* if not streamed, calculate number of bytes left */
        if (pState->flags & PCM_FLAGS_STREAMING)
            pState->bytesLeft = 0x7fffffff;
        else
            pState->bytesLeft -= *pLocation;
    }
    return EAS_SUCCESS;
}

