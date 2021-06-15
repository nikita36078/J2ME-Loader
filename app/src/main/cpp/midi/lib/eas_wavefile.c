/*----------------------------------------------------------------------------
 *
 * File:
 * eas_wavefile.c
 *
 * Contents and purpose:
 * This file implements the wave file parser.
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
 *   $Revision: 852 $
 *   $Date: 2007-09-04 11:43:49 -0700 (Tue, 04 Sep 2007) $
 *----------------------------------------------------------------------------
*/

#include "eas_data.h"
#include "eas_report.h"
#include "eas_host.h"
#include "eas_config.h"
#include "eas_parser.h"
#include "eas_pcm.h"
#include "eas_wavefile.h"

/* lint is choking on the ARM math.h file, so we declare the log10 function here */
extern double log10(double x);

/* increase gain to compensate for loss in mixer */
#define WAVE_GAIN_OFFSET            6

/* constant for 1200 / log10(2.0) */
#define PITCH_CENTS_CONVERSION      3986.313714

/*----------------------------------------------------------------------------
 * WAVE file defines
 *----------------------------------------------------------------------------
*/
/* RIFF chunks */
#define CHUNK_TYPE(a,b,c,d) (   \
        ( ((EAS_U32)(a) & 0xFF) << 24 ) \
    +   ( ((EAS_U32)(b) & 0xFF) << 16 ) \
    +   ( ((EAS_U32)(c) & 0xFF) <<  8 ) \
    +   ( ((EAS_U32)(d) & 0xFF)       ) )

#define CHUNK_RIFF                  CHUNK_TYPE('R','I','F','F')
#define CHUNK_WAVE                  CHUNK_TYPE('W','A','V','E')
#define CHUNK_FMT                   CHUNK_TYPE('f','m','t',' ')
#define CHUNK_DATA                  CHUNK_TYPE('d','a','t','a')
#define CHUNK_LIST                  CHUNK_TYPE('L','I','S','T')
#define CHUNK_INFO                  CHUNK_TYPE('I','N','F','O')
#define CHUNK_INAM                  CHUNK_TYPE('I','N','A','M')
#define CHUNK_ICOP                  CHUNK_TYPE('I','C','O','P')
#define CHUNK_IART                  CHUNK_TYPE('I','A','R','T')

/* wave file format identifiers */
#define WAVE_FORMAT_PCM             0x0001
#define WAVE_FORMAT_IMA_ADPCM       0x0011

/* file size for streamed file */
#define FILE_SIZE_STREAMING         0x80000000

/*----------------------------------------------------------------------------
 * prototypes
 *----------------------------------------------------------------------------
*/
static EAS_RESULT WaveCheckFileType (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, EAS_VOID_PTR *pHandle, EAS_I32 offset);
static EAS_RESULT WavePrepare (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT WaveState (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_STATE *pState);
static EAS_RESULT WaveClose (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT WaveReset (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT WaveLocate (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 time, EAS_BOOL *pParserLocate);
static EAS_RESULT WavePause (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT WaveResume (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT WaveSetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value);
static EAS_RESULT WaveGetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue);
static EAS_RESULT WaveParseHeader (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, S_WAVE_STATE *pWaveData);
static EAS_RESULT WaveGetMetaData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 *pMediaLength);

#ifdef MMAPI_SUPPORT
static EAS_RESULT SaveFmtChunk (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, S_WAVE_STATE *pWaveData, EAS_I32 size);
#endif

/*----------------------------------------------------------------------------
 *
 * EAS_Wave_Parser
 *
 * This structure contains the functional interface for the Wave file parser
 *----------------------------------------------------------------------------
*/
const S_FILE_PARSER_INTERFACE EAS_Wave_Parser =
{
    WaveCheckFileType,
    WavePrepare,
    NULL,
    NULL,
    WaveState,
    WaveClose,
    WaveReset,
    WavePause,
    WaveResume,
    WaveLocate,
    WaveSetData,
    WaveGetData,
    WaveGetMetaData
};

/*----------------------------------------------------------------------------
 * WaveCheckFileType()
 *----------------------------------------------------------------------------
 * Purpose:
 * Check the file type to see if we can parse it
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
static EAS_RESULT WaveCheckFileType (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, EAS_VOID_PTR *pHandle, EAS_I32 offset)
{
    S_WAVE_STATE *pWaveData;

    /* zero the memory to insure complete initialization */
    *pHandle = NULL;

    /* read the file header */
    if (WaveParseHeader(pEASData, fileHandle, NULL) == EAS_SUCCESS)
    {

        /* check for static memory allocation */
        if (pEASData->staticMemoryModel)
            pWaveData = EAS_CMEnumData(EAS_CM_WAVE_DATA);
        else
            pWaveData = EAS_HWMalloc(pEASData->hwInstData, sizeof(S_WAVE_STATE));
        if (!pWaveData)
            return EAS_ERROR_MALLOC_FAILED;
        EAS_HWMemSet(pWaveData, 0, sizeof(S_WAVE_STATE));

        /* return a pointer to the instance data */
        pWaveData->fileHandle = fileHandle;
        pWaveData->fileOffset = offset;
        *pHandle = pWaveData;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * WavePrepare()
 *----------------------------------------------------------------------------
 * Purpose:
 * Prepare to parse the file.
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
static EAS_RESULT WavePrepare (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_WAVE_STATE *pWaveData;
    EAS_RESULT result;

    /* validate parser state */
    pWaveData = (S_WAVE_STATE*) pInstData;
    if (pWaveData->streamHandle != NULL)
        return EAS_ERROR_NOT_VALID_IN_THIS_STATE;

    /* back to start of file */
    pWaveData->time = 0;
    if ((result = EAS_HWFileSeek(pEASData->hwInstData, pWaveData->fileHandle, pWaveData->fileOffset)) != EAS_SUCCESS)
        return result;

    /* parse the file header */
    if ((result = WaveParseHeader(pEASData, pWaveData->fileHandle, pWaveData)) != EAS_SUCCESS)
        return result;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * WaveState()
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
static EAS_RESULT WaveState (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_STATE *pState)
{
    S_WAVE_STATE *pWaveData;

    /* return current state */
    pWaveData = (S_WAVE_STATE*) pInstData;
    if (pWaveData->streamHandle)
        return EAS_PEState(pEASData, pWaveData->streamHandle, pState);

    /* if no stream handle, and time is not zero, we are done */
    if (pWaveData->time > 0)
        *pState = EAS_STATE_STOPPED;
    else
        *pState = EAS_STATE_OPEN;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * WaveClose()
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
static EAS_RESULT WaveClose (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_WAVE_STATE *pWaveData;
    EAS_RESULT result;

    pWaveData = (S_WAVE_STATE*) pInstData;

    /* close the stream */
    if (pWaveData->streamHandle)
    {
        if ((result = EAS_PEClose(pEASData, pWaveData->streamHandle)) != EAS_SUCCESS)
            return result;
        pWaveData->streamHandle = NULL;
    }

    /* if using dynamic memory, free it */
    if (!pEASData->staticMemoryModel)
    {

#ifdef MMAPI_SUPPORT
        /* need to free the fmt chunk */
        if (pWaveData->fmtChunk != NULL)
            EAS_HWFree(pEASData->hwInstData, pWaveData->fmtChunk);
#endif

        /* free the instance data */
        EAS_HWFree(pEASData->hwInstData, pWaveData);

    }
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * WaveReset()
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
static EAS_RESULT WaveReset (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    EAS_PCM_HANDLE streamHandle;

    /* reset to first byte of data in the stream */
    streamHandle = ((S_WAVE_STATE*)pInstData)->streamHandle;
    if (streamHandle)
        return EAS_PEReset(pEASData, streamHandle);
    return EAS_ERROR_NOT_VALID_IN_THIS_STATE;
}

/*----------------------------------------------------------------------------
 * WaveLocate()
 *----------------------------------------------------------------------------
 * Purpose:
 * Rewind/fast-forward in file.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 * time             - time (in msecs)
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pParserLocate) reserved for future use */
static EAS_RESULT WaveLocate (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 time, EAS_BOOL *pParserLocate)
{
    EAS_PCM_HANDLE streamHandle;

    /* reset to first byte of data in the stream */
    streamHandle = ((S_WAVE_STATE*)pInstData)->streamHandle;
    if (streamHandle)
        return EAS_PELocate(pEASData, streamHandle, time);
    return EAS_ERROR_NOT_VALID_IN_THIS_STATE;
}

/*----------------------------------------------------------------------------
 * WavePause()
 *----------------------------------------------------------------------------
 * Purpose:
 * Mute and stop rendering a PCM stream. Sets the gain target to zero and stops the playback
 * at the end of the next audio frame.
 *
 * Inputs:
 * pEASData         - pointer to EAS library instance data
 * handle           - pointer to S_WAVE_STATE for this stream
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
static EAS_RESULT WavePause (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    EAS_PCM_HANDLE streamHandle;

    /* pause the stream */
    streamHandle = ((S_WAVE_STATE*)pInstData)->streamHandle;
    if (streamHandle)
        return EAS_PEPause(pEASData, streamHandle);
    return EAS_ERROR_NOT_VALID_IN_THIS_STATE;
}

/*----------------------------------------------------------------------------
 * WaveResume()
 *----------------------------------------------------------------------------
 * Purpose:
 * Resume rendering a PCM stream. Sets the gain target back to its
 * previous setting and restarts playback at the end of the next audio
 * frame.
 *
 * Inputs:
 * pEASData         - pointer to EAS library instance data
 * handle           - pointer to S_WAVE_STATE for this stream
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
static EAS_RESULT WaveResume (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    EAS_PCM_HANDLE streamHandle;

    /* resume the stream */
    streamHandle = ((S_WAVE_STATE*)pInstData)->streamHandle;
    if (streamHandle)
        return EAS_PEResume(pEASData, streamHandle);
    return EAS_ERROR_NOT_VALID_IN_THIS_STATE;
}

/*----------------------------------------------------------------------------
 * WaveSetData()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 * pEASData         - pointer to EAS library instance data
 * handle           - pointer to S_WAVE_STATE for this stream
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT WaveSetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value)
{
    S_WAVE_STATE *pWaveData = (S_WAVE_STATE*) pInstData;

    switch (param)
    {
        /* set metadata callback */
        case PARSER_DATA_METADATA_CB:
            EAS_HWMemCpy(&pWaveData->metadata, (void*) value, sizeof(S_METADATA_CB));
            return EAS_SUCCESS;

        case PARSER_DATA_PLAYBACK_RATE:
            value = (EAS_I32) (PITCH_CENTS_CONVERSION * log10((double) value / (double) (1 << 28)));
            return EAS_PEUpdatePitch(pEASData, pWaveData->streamHandle, (EAS_I16) value);

        case PARSER_DATA_VOLUME:
            return EAS_PEUpdateVolume(pEASData, pWaveData->streamHandle, (EAS_I16) value);

        default:
            return EAS_ERROR_INVALID_PARAMETER;
    }
}

/*----------------------------------------------------------------------------
 * WaveGetData()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 * pEASData         - pointer to EAS library instance data
 * handle           - pointer to S_WAVE_STATE for this stream
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, pEASData) reserved for future use */
static EAS_RESULT WaveGetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue)
{
    S_WAVE_STATE *pWaveData;

    pWaveData = (S_WAVE_STATE*) pInstData;
    switch (param)
    {
        /* return file type as WAVE */
        case PARSER_DATA_FILE_TYPE:
            *pValue = pWaveData->fileType;
            break;

#ifdef MMAPI_SUPPORT
        /* return pointer to 'fmt' chunk */
        case PARSER_DATA_FORMAT:
            *pValue = (EAS_I32) pWaveData->fmtChunk;
            break;
#endif

        case PARSER_DATA_GAIN_OFFSET:
            *pValue = WAVE_GAIN_OFFSET;
            break;

        default:
            return EAS_ERROR_INVALID_PARAMETER;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * WaveParseHeader()
 *----------------------------------------------------------------------------
 * Purpose:
 * Parse the WAVE file header.
 *
 * Inputs:
 * pEASData         - pointer to EAS library instance data
 * handle           - pointer to S_WAVE_STATE for this stream
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT WaveParseHeader (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, S_WAVE_STATE *pWaveData)
{
    S_PCM_OPEN_PARAMS params;
    EAS_RESULT result;
    EAS_U32 tag;
    EAS_U32 fileSize;
    EAS_U32 size;
    EAS_I32 pos;
    EAS_I32 audioOffset;
    EAS_U16 usTemp;
    EAS_BOOL parseDone;
    EAS_U32 avgBytesPerSec;

    /* init some data (and keep lint happy) */
    params.sampleRate = 0;
    params.size = 0;
    audioOffset = 0;
    params.decoder = 0;
    params.blockSize = 0;
    params.pCallbackFunc = NULL;
    params.cbInstData = NULL;
    params.loopSamples = 0;
    params.fileHandle = fileHandle;
    params.volume = 0x7fff;
    params.envData = 0;
    avgBytesPerSec = 8000;

    /* check for 'RIFF' tag */
    if ((result = EAS_HWGetDWord(pEASData->hwInstData, fileHandle, &tag, EAS_TRUE)) != EAS_FALSE)
        return result;
    if (tag != CHUNK_RIFF)
        return EAS_ERROR_UNRECOGNIZED_FORMAT;

    /* get size */
    if ((result = EAS_HWGetDWord(pEASData->hwInstData, fileHandle, &fileSize, EAS_FALSE)) != EAS_FALSE)
        return result;

    /* check for 'WAVE' tag */
    if ((result = EAS_HWGetDWord(pEASData->hwInstData, fileHandle, &tag, EAS_TRUE)) != EAS_FALSE)
        return result;
    if (tag != CHUNK_WAVE)
        return EAS_ERROR_UNRECOGNIZED_FORMAT;

    /* this is enough to say we recognize the file */
    if (pWaveData == NULL)
        return EAS_SUCCESS;

    /* check for streaming mode */
    pWaveData->flags = 0;
    pWaveData->mediaLength = -1;
    pWaveData->infoChunkPos = -1;
    pWaveData->infoChunkSize = -1;
    if (fileSize== FILE_SIZE_STREAMING)
    {
        pWaveData->flags |= PCM_FLAGS_STREAMING;
        fileSize = 0x7fffffff;
    }

    /* find out where we're at */
    if ((result = EAS_HWFilePos(pEASData->hwInstData, fileHandle, &pos)) != EAS_SUCCESS)
        return result;
    fileSize -= 4;

    parseDone = EAS_FALSE;
    for (;;)
    {
        /* get tag and size for next chunk */
        if ((result = EAS_HWGetDWord(pEASData->hwInstData, fileHandle, &tag, EAS_TRUE)) != EAS_FALSE)
            return result;
        if ((result = EAS_HWGetDWord(pEASData->hwInstData, fileHandle, &size, EAS_FALSE)) != EAS_FALSE)
            return result;

        /* process chunk */
        pos += 8;
        switch (tag)
        {
            case CHUNK_FMT:

#ifdef MMAPI_SUPPORT
                if ((result = SaveFmtChunk(pEASData, fileHandle, pWaveData, (EAS_I32) size)) != EAS_SUCCESS)
                    return result;
#endif

                /* get audio format */
                if ((result = EAS_HWGetWord(pEASData->hwInstData, fileHandle, &usTemp, EAS_FALSE)) != EAS_FALSE)
                    return result;
                if (usTemp == WAVE_FORMAT_PCM)
                {
                    params.decoder = EAS_DECODER_PCM;
                    pWaveData->fileType = EAS_FILE_WAVE_PCM;
                }
                else if (usTemp == WAVE_FORMAT_IMA_ADPCM)
                {
                    params.decoder = EAS_DECODER_IMA_ADPCM;
                    pWaveData->fileType = EAS_FILE_WAVE_IMA_ADPCM;
                }
                else
                    return EAS_ERROR_UNRECOGNIZED_FORMAT;

                /* get number of channels */
                if ((result = EAS_HWGetWord(pEASData->hwInstData, fileHandle, &usTemp, EAS_FALSE)) != EAS_FALSE)
                    return result;
                if (usTemp == 2)
                    pWaveData->flags |= PCM_FLAGS_STEREO;
                else if (usTemp != 1)
                    return EAS_ERROR_UNRECOGNIZED_FORMAT;

                /* get sample rate */
                if ((result = EAS_HWGetDWord(pEASData->hwInstData, fileHandle, &params.sampleRate, EAS_FALSE)) != EAS_FALSE)
                    return result;

                /* get stream rate */
                if ((result = EAS_HWGetDWord(pEASData->hwInstData, fileHandle, &avgBytesPerSec, EAS_FALSE)) != EAS_FALSE)
                    return result;

                /* get block alignment */
                if ((result = EAS_HWGetWord(pEASData->hwInstData, fileHandle, &usTemp, EAS_FALSE)) != EAS_FALSE)
                    return result;
                params.blockSize = usTemp;

                /* get bits per sample */
                if ((result = EAS_HWGetWord(pEASData->hwInstData, fileHandle, &usTemp, EAS_FALSE)) != EAS_FALSE)
                    return result;

                /* PCM, must be 8 or 16 bit samples */
                if (params.decoder == EAS_DECODER_PCM)
                {
                    if (usTemp == 8)
                        pWaveData->flags |= PCM_FLAGS_8_BIT | PCM_FLAGS_UNSIGNED;
                    else if (usTemp != 16)
                        return EAS_ERROR_UNRECOGNIZED_FORMAT;
                }

                /* for IMA ADPCM, we only support mono 4-bit ADPCM */
                else
                {
                    if ((usTemp != 4) || (pWaveData->flags & PCM_FLAGS_STEREO))
                        return EAS_ERROR_UNRECOGNIZED_FORMAT;
                }

                break;

            case CHUNK_DATA:
                audioOffset = pos;
                if (pWaveData->flags & PCM_FLAGS_STREAMING)
                {
                    params.size = 0x7fffffff;
                    parseDone = EAS_TRUE;
                }
                else
                {
                    params.size = (EAS_I32) size;
                    params.loopStart = size;
                    /* use more accurate method if possible */
                    if (size <= (0x7fffffff / 1000))
                        pWaveData->mediaLength = (EAS_I32) ((size * 1000) / avgBytesPerSec);
                    else
                        pWaveData->mediaLength = (EAS_I32) (size / (avgBytesPerSec / 1000));
                }
                break;

            case CHUNK_LIST:
                /* get the list type */
                if ((result = EAS_HWGetDWord(pEASData->hwInstData, fileHandle, &tag, EAS_TRUE)) != EAS_FALSE)
                    return result;
                if (tag == CHUNK_INFO)
                {
                    pWaveData->infoChunkPos = pos + 4;
                    pWaveData->infoChunkSize = (EAS_I32) size - 4;
                }
                break;

            default:
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "WaveParseHeader: %c%c%c%c chunk - %d byte(s) ignored\n",
                    (char) (tag >> 24), (char) (tag >> 16), (char) (tag >> 8), (char) tag, size); */ }
                break;
        }

        if (parseDone)
            break;

        /* subtract header size */
        fileSize -= 8;

        /* account for zero-padding on odd length chunks */
        if (size & 1)
            size++;

        /* this check works for files with odd length last chunk and no zero-pad */
        if (size >= fileSize)
        {
            if (size > fileSize)
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "WaveParseHeader: '%c%c%c%c' chunk size exceeds length of file or is not zero-padded\n",
                    (char) (tag >> 24), (char) (tag >> 16), (char) (tag >> 8), (char) tag, size); */ }
            break;
        }

        /* subtract size of data chunk (including any zero-pad) */
        fileSize -= size;

        /* seek to next chunk */
        pos += (EAS_I32) size;
        if ((result = EAS_HWFileSeek(pEASData->hwInstData, fileHandle, pos)) != EAS_SUCCESS)
            return result;
    }

    /* check for valid header */
    if ((params.sampleRate == 0) || (params.size == 0))
        return EAS_ERROR_UNRECOGNIZED_FORMAT;

    /* save the pertinent information */
    pWaveData->audioOffset = audioOffset;
    params.flags = pWaveData->flags;

    /* seek to data */
    if ((result = EAS_HWFileSeek(pEASData->hwInstData, fileHandle, audioOffset)) != EAS_SUCCESS)
        return result;

    /* open a stream in the PCM engine */
    return EAS_PEOpenStream(pEASData, &params, &pWaveData->streamHandle);
}

/*----------------------------------------------------------------------------
 * WaveGetMetaData()
 *----------------------------------------------------------------------------
 * Purpose:
 * Process the INFO chunk and return metadata to host
 *----------------------------------------------------------------------------
*/
static EAS_RESULT WaveGetMetaData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 *pMediaLength)
{
    S_WAVE_STATE *pWaveData;
    EAS_RESULT result;
    EAS_I32 pos;
    EAS_U32 size;
    EAS_I32 infoSize;
    EAS_U32 tag;
    EAS_I32 restorePos;
    E_EAS_METADATA_TYPE metaType;
    EAS_I32 metaLen;

    /* get current position so we can restore it */
    pWaveData = (S_WAVE_STATE*) pInstData;

    /* return media length */
    *pMediaLength = pWaveData->mediaLength;

    /* did we encounter an INFO chunk? */
    if (pWaveData->infoChunkPos < 0)
        return EAS_SUCCESS;

    if ((result = EAS_HWFilePos(pEASData->hwInstData, pWaveData->fileHandle, &restorePos)) != EAS_SUCCESS)
        return result;

    /* offset to start of first chunk in INFO chunk */
    pos = pWaveData->infoChunkPos;
    infoSize = pWaveData->infoChunkSize;

    /* read all the chunks in the INFO chunk */
    for (;;)
    {

        /* seek to next chunk */
        if ((result = EAS_HWFileSeek(pEASData->hwInstData, pWaveData->fileHandle, pos)) != EAS_SUCCESS)
            return result;

        /* get tag and size for next chunk */
        if ((result = EAS_HWGetDWord(pEASData->hwInstData, pWaveData->fileHandle, &tag, EAS_TRUE)) != EAS_FALSE)
            return result;
        if ((result = EAS_HWGetDWord(pEASData->hwInstData, pWaveData->fileHandle, &size, EAS_FALSE)) != EAS_FALSE)
            return result;

        /* process chunk */
        pos += 8;
        metaType = EAS_METADATA_UNKNOWN;
        switch (tag)
        {
            case CHUNK_INAM:
                metaType = EAS_METADATA_TITLE;
                break;

            case CHUNK_IART:
                metaType = EAS_METADATA_AUTHOR;
                break;

            case CHUNK_ICOP:
                metaType = EAS_METADATA_COPYRIGHT;
                break;

            default:
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "WaveParseHeader: %c%c%c%c chunk - %d byte(s) ignored\n",
                    (char) (tag >> 24), (char) (tag >> 16), (char) (tag >> 8), (char) tag, size); */ }
                break;
        }

        /* process known metadata */
        if (metaType != EAS_METADATA_UNKNOWN)
        {
            metaLen = pWaveData->metadata.bufferSize - 1;
            if (metaLen > (EAS_I32) size)
                metaLen = (EAS_I32) size;
            if ((result = EAS_HWReadFile(pEASData->hwInstData, pWaveData->fileHandle, pWaveData->metadata.buffer, metaLen, &metaLen)) != EAS_SUCCESS)
                return result;
            pWaveData->metadata.buffer[metaLen] = 0;
            pWaveData->metadata.callback(metaType, pWaveData->metadata.buffer, pWaveData->metadata.pUserData);
        }

        /* subtract this block */
        if (size & 1)
            size++;
        infoSize -= (EAS_I32) size + 8;
        if (infoSize == 0)
            break;
        pos += (EAS_I32) size;
    }


    /* restore original position */
    return EAS_HWFileSeek(pEASData->hwInstData, pWaveData->fileHandle, restorePos);
}

#ifdef MMAPI_SUPPORT
/*----------------------------------------------------------------------------
 * SaveFmtChunk()
 *----------------------------------------------------------------------------
 * Purpose:
 * Save the fmt chunk for the MMAPI library
 *----------------------------------------------------------------------------
*/
static EAS_RESULT SaveFmtChunk (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, S_WAVE_STATE *pWaveData, EAS_I32 fmtSize)
{
    EAS_RESULT result;
    EAS_I32 pos;
    EAS_I32 count;

    /* save current file position */
    if ((result = EAS_HWFilePos(pEASData->hwInstData, fileHandle, &pos)) != EAS_SUCCESS)
        return result;

    /* allocate a chunk of memory */
    pWaveData->fmtChunk = EAS_HWMalloc(pEASData->hwInstData, fmtSize);
    if (!pWaveData->fmtChunk)
        return EAS_ERROR_MALLOC_FAILED;

    /* read the fmt chunk into memory */
    if ((result = EAS_HWReadFile(pEASData->hwInstData, fileHandle, pWaveData->fmtChunk, fmtSize, &count)) != EAS_SUCCESS)
        return result;
    if (count != fmtSize)
        return EAS_ERROR_FILE_READ_FAILED;

    /* restore file position */
    return EAS_HWFileSeek(pEASData->hwInstData, fileHandle, pos);
}
#endif

