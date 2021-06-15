/*----------------------------------------------------------------------------
 *
 * File:
 * eas_imaadpcm.c
 *
 * Contents and purpose:
 * Implements the IMA ADPCM decoder
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
 *   $Revision: 847 $
 *   $Date: 2007-08-27 21:30:08 -0700 (Mon, 27 Aug 2007) $
 *----------------------------------------------------------------------------
*/

#include "eas_data.h"
#include "eas_host.h"
#include "eas_pcm.h"
#include "eas_math.h"
#include "eas_report.h"

// #define _DEBUG_IMA_ADPCM_LOCATE

/*----------------------------------------------------------------------------
 * externs
 *----------------------------------------------------------------------------
*/
extern const EAS_I16 imaIndexTable[];
extern const EAS_I16 imaStepSizeTable[];

/*----------------------------------------------------------------------------
 * prototypes
 *----------------------------------------------------------------------------
*/
static EAS_RESULT IMADecoderInit (EAS_DATA_HANDLE pEASData, S_PCM_STATE *pState);
static EAS_RESULT IMADecoderSample (EAS_DATA_HANDLE pEASData, S_PCM_STATE *pState);
static void IMADecoderADPCM (S_DECODER_STATE *pState, EAS_U8 nibble);
static EAS_RESULT IMADecoderLocate (EAS_DATA_HANDLE pEASData, S_PCM_STATE *pState, EAS_I32 time);

/*----------------------------------------------------------------------------
 * IMA ADPCM Decoder interface
 *----------------------------------------------------------------------------
*/
const S_DECODER_INTERFACE IMADecoder =
{
    IMADecoderInit,
    IMADecoderSample,
    IMADecoderLocate
};

/*----------------------------------------------------------------------------
 * IMADecoderInit()
 *----------------------------------------------------------------------------
 * Purpose:
 * Initializes the IMA ADPCM decoder
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
/*lint -esym(715, pEASData) common decoder interface - pEASData not used */
static EAS_RESULT IMADecoderInit (EAS_DATA_HANDLE pEASData, S_PCM_STATE *pState)
{
    pState->decoderL.step = 0;
    pState->decoderR.step = 0;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * IMADecoderSample()
 *----------------------------------------------------------------------------
 * Purpose:
 * Decodes an IMA ADPCM sample
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
static EAS_RESULT IMADecoderSample (EAS_DATA_HANDLE pEASData, S_PCM_STATE *pState)
{
    EAS_RESULT result;
    EAS_I16 sTemp;

    /* if high nibble, decode */
    if (pState->hiNibble)
    {
        IMADecoderADPCM(&pState->decoderL, (EAS_U8)(pState->srcByte >> 4));
        pState->hiNibble = EAS_FALSE;
    }

    /* low nibble, need to fetch another byte */
    else
    {
        /* check for loop */
        if ((pState->bytesLeft == 0) && (pState->loopSamples != 0))
        {
            /* seek to start of loop */
            if ((result = EAS_HWFileSeek(pEASData->hwInstData, pState->fileHandle, (EAS_I32) (pState->startPos + pState->loopLocation))) != EAS_SUCCESS)
                return result;
            pState->bytesLeft = pState->byteCount = (EAS_I32) pState->bytesLeftLoop;
            pState->blockCount = 0;
            pState->flags &= ~PCM_FLAGS_EMPTY;
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "IMADecoderSample: Rewind file to %d, bytesLeft = %d\n", pState->startPos, pState->bytesLeft); */ }
        }

        /* if start of block, fetch new predictor and step index */
        if ((pState->blockSize != 0) && (pState->blockCount == 0) && (pState->bytesLeft != 0))
        {

            /* get predicted sample for left channel */
            if ((result = EAS_HWGetWord(pEASData->hwInstData, pState->fileHandle, &sTemp, EAS_FALSE)) != EAS_SUCCESS)
                return result;
#ifdef _DEBUG_IMA_ADPCM
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Predictor: Was %d, now %d\n", pState->decoderL.acc, sTemp); */ }
#endif
            pState->decoderL.acc = pState->decoderL.x1 = sTemp;

            /* get step index for left channel - upper 8 bits are reserved */
            if ((result = EAS_HWGetWord(pEASData->hwInstData, pState->fileHandle, &sTemp, EAS_FALSE)) != EAS_SUCCESS)
                return result;
#ifdef _DEBUG_IMA_ADPCM
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Step: Was %d, now %d\n", pState->decoderL.step, sTemp); */ }
#endif
            pState->decoderL.step = sTemp & 0xff;

            if (pState->flags & PCM_FLAGS_STEREO)
            {
                /* get predicted sample for right channel */
                if ((result = EAS_HWGetWord(pEASData->hwInstData, pState->fileHandle, &sTemp, EAS_FALSE)) != EAS_SUCCESS)
                    return result;
                pState->decoderR.acc = pState->decoderR.x1 = sTemp;

                /* get step index for right channel - upper 8 bits are reserved */
                if ((result = EAS_HWGetWord(pEASData->hwInstData, pState->fileHandle, &sTemp, EAS_FALSE)) != EAS_SUCCESS)
                    return result;
#ifdef _DEBUG_IMA_ADPCM
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Step: Was %d, now %d\n", pState->decoderR.step, sTemp); */ }
#endif
                pState->decoderR.step = sTemp & 0xff;

                pState->blockCount = pState->blockSize - 8;
                pState->bytesLeft -= 8;
            }
            else
            {
                pState->blockCount = pState->blockSize - 4;
                pState->bytesLeft -= 4;
            }
        }
        else
        {

            /* get another ADPCM data pair */
            if (pState->bytesLeft)
            {

                if ((result = EAS_HWGetByte(pEASData->hwInstData, pState->fileHandle, &pState->srcByte)) != EAS_SUCCESS)
                    return result;

                /* decode the low nibble */
                pState->bytesLeft--;
                pState->blockCount--;
                IMADecoderADPCM(&pState->decoderL, (EAS_U8)(pState->srcByte & 0x0f));

                if (pState->flags & PCM_FLAGS_STEREO)
                    IMADecoderADPCM(&pState->decoderR, (EAS_U8)(pState->srcByte >> 4));
                else
                    pState->hiNibble = EAS_TRUE;
            }

            /* out of ADPCM data, generate enough samples to fill buffer */
            else
            {
                pState->decoderL.x1 = pState->decoderL.x0;
                pState->decoderR.x1 = pState->decoderR.x0;
            }
        }
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * IMADecoderADPCM()
 *----------------------------------------------------------------------------
 * Purpose:
 * Decodes an IMA ADPCM sample
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
static void IMADecoderADPCM (S_DECODER_STATE *pState, EAS_U8 nibble)
{
    EAS_INT delta;
    EAS_INT stepSize;

    /* get stepsize from table */
    stepSize = imaStepSizeTable[pState->step];

    /* delta = (abs(delta) + 0.5) * step / 4 */
    delta = 0;
    if (nibble & 4)
        delta += stepSize;

    if (nibble & 2)
        /*lint -e{702} use shift for performance */
        delta += stepSize >> 1;

    if (nibble & 1)
        /*lint -e{702} use shift for performance */
        delta += stepSize >> 2;

    /*lint -e{702} use shift for performance */
    delta += stepSize >> 3;

    /* integrate the delta */
    if (nibble & 8)
      pState->acc -= delta;
    else
      pState->acc += delta;

    /* saturate */
    if (pState->acc > 32767)
        pState->acc = 32767;
    if (pState->acc < -32768)
        pState->acc = -32768;
    pState->x1 = (EAS_PCM) pState->acc;

    /* compute new step size */
    pState->step += imaIndexTable[nibble];
    if (pState->step < 0)
        pState->step = 0;
    if (pState->step > 88)
        pState->step = 88;

#ifdef _DEBUG_IMA_ADPCM
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "In=%u, Pred=%d, Step=%d\n", nibble, pState->acc,  imaStepSizeTable[pState->step]); */ }
#endif
}

/*----------------------------------------------------------------------------
 * IMADecoderLocate()
 *----------------------------------------------------------------------------
 * Locate in an IMA ADPCM stream
 *----------------------------------------------------------------------------
*/
static EAS_RESULT IMADecoderLocate (EAS_DATA_HANDLE pEASData, S_PCM_STATE *pState, EAS_I32 time)
{
    EAS_RESULT result;
    EAS_I32 temp;
    EAS_I32 samplesPerBlock;
    EAS_I32 secs, msecs;

    /* no need to calculate if time is zero */
    if (time == 0)
        temp = 0;

    /* not zero */
    else
    {

        /* can't seek if not a blocked file */
        if (pState->blockSize == 0)
            return EAS_ERROR_FEATURE_NOT_AVAILABLE;

        /* calculate number of samples per block */
        if (pState->flags & PCM_FLAGS_STEREO)
            samplesPerBlock = pState->blockSize - 7;
        else
            samplesPerBlock = (pState->blockSize << 1) - 7;

        /* break down into secs and msecs */
        secs = time / 1000;
        msecs = time - (secs * 1000);

        /* calculate sample number fraction from msecs */
        temp = (msecs * pState->sampleRate);
        temp = (temp >> 10) + ((temp * 49) >> 21);

        /* add integer sample count */
        temp += secs * pState->sampleRate;

#ifdef _DEBUG_IMA_ADPCM_LOCATE
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x2380b977, 0x00000006 , time, temp);
#endif

        /* for looped samples, calculate position in the loop */
        if ((temp > pState->byteCount) && (pState->loopSamples != 0))
        {
            EAS_I32 numBlocks;
            EAS_I32 samplesPerLoop;
            EAS_I32 samplesInLastBlock;

            numBlocks = (EAS_I32) (pState->loopStart / pState->blockSize);
            samplesInLastBlock = (EAS_I32) pState->loopStart - (numBlocks * pState->blockSize);
            if (samplesInLastBlock)
            {
                if (pState->flags & PCM_FLAGS_STEREO)
                    samplesInLastBlock = samplesInLastBlock - 7;
                else
                    /*lint -e{703} use shift for performance */
                    samplesInLastBlock = (samplesInLastBlock << 1) - 7;
            }
            samplesPerLoop = numBlocks * samplesPerBlock + samplesInLastBlock;
            temp = temp % samplesPerLoop;
#ifdef _DEBUG_IMA_ADPCM_LOCATE
            EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x2380b977, 0x00000007 , numBlocks, samplesPerLoop, samplesInLastBlock, temp);
#endif
        }

        /* find start of block for requested sample */
        temp = (temp / samplesPerBlock) * pState->blockSize;
#ifdef _DEBUG_IMA_ADPCM_LOCATE
        EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x2380b977, 0x00000008 , temp);
#endif

    }

    /* seek to new location */
    if ((result = EAS_PESeek(pEASData, pState, &temp)) != EAS_SUCCESS)
        return result;

#ifdef _DEBUG_IMA_ADPCM_LOCATE
    EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x2380b977, 0x00000009 , pState->bytesLeft);
#endif

    /* reset state */
    pState->blockCount = 0;
    pState->hiNibble = EAS_FALSE;
    if ((pState->state != EAS_STATE_PAUSING) && (pState->state != EAS_STATE_PAUSED))
        pState->state = EAS_STATE_READY;

    return EAS_SUCCESS;
}

