/*----------------------------------------------------------------------------
 *
 * File:
 * eas_chorus.c
 *
 * Contents and purpose:
 * Contains the implementation of the Chorus effect.
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
 *   $Revision: 499 $
 *   $Date: 2006-12-11 16:07:20 -0800 (Mon, 11 Dec 2006) $
 *----------------------------------------------------------------------------
*/

#include "eas_data.h"
#include "eas_effects.h"
#include "eas_math.h"
#include "eas_chorusdata.h"
#include "eas_chorus.h"
#include "eas_config.h"
#include "eas_host.h"
#include "eas_report.h"

/* prototypes for effects interface */
static EAS_RESULT ChorusInit (EAS_DATA_HANDLE pEASData, EAS_VOID_PTR *pInstData);
static void ChorusProcess (EAS_VOID_PTR pInstData, EAS_PCM *pSrc, EAS_PCM *pDst, EAS_I32 numSamples);
static EAS_RESULT ChorusShutdown (EAS_DATA_HANDLE pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT ChorusGetParam (EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue);
static EAS_RESULT ChorusSetParam (EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value);

/* common effects interface for configuration module */
const S_EFFECTS_INTERFACE EAS_Chorus =
{
    ChorusInit,
    ChorusProcess,
    ChorusShutdown,
    ChorusGetParam,
    ChorusSetParam
};



//LFO shape table used by the chorus, larger table would sound better
//this is a sine wave, where 32767 = 1.0
static const EAS_I16 EAS_chorusShape[CHORUS_SHAPE_SIZE] = {
    0, 1608, 3212, 4808, 6393, 7962, 9512, 11309, 12539, 14010, 15446, 16846, 18204, 19519, 20787, 22005, 23170,
    24279, 25329, 26319, 27245, 28105, 28898, 29621, 30273, 30852, 31356, 31785, 32137, 32412, 32609, 32728,
    32767, 32728, 32609, 32412, 32137, 31785, 31356, 30852, 30273, 29621, 28898, 28105, 27245, 26319, 25329,
    24279, 23170, 22005, 20787, 19519, 18204, 16846, 15446, 14010, 12539, 11039, 9512, 7962, 6393, 4808, 3212,
    1608, 0, -1608, -3212, -4808, -6393, -7962, -9512, -11309, -12539, -14010, -15446, -16846, -18204, -19519,
    -20787, -22005, -23170, -24279, -25329, -26319, -27245, -28105, -28898, -29621, -30273, -30852, -31356, -31785,
    -32137, -32412, -32609, -32728, -32767, -32728, -32609, -32412, -32137, -31785, -31356, -30852, -30273, -29621,
    -28898, -28105, -27245, -26319, -25329, -24279, -23170, -22005, -20787, -19519, -18204, -16846, -15446, -14010,
    -12539, -11039, -9512, -7962, -6393, -4808, -3212, -1608
};

/*----------------------------------------------------------------------------
 * InitializeChorus()
 *----------------------------------------------------------------------------
 * Purpose: Initializes chorus parameters
 *
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ChorusInit (EAS_DATA_HANDLE pEASData, EAS_VOID_PTR *pInstData)
{
    S_CHORUS_OBJECT *pChorusData;
    S_CHORUS_PRESET *pPreset;
    EAS_I32 index;

    /* check Configuration Module for data allocation */
    if (pEASData->staticMemoryModel)
        pChorusData = EAS_CMEnumFXData(EAS_MODULE_CHORUS);

    /* allocate dynamic memory */
    else
        pChorusData = EAS_HWMalloc(pEASData->hwInstData, sizeof(S_CHORUS_OBJECT));

    if (pChorusData == NULL)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_FATAL, "Failed to allocate Chorus memory\n"); */ }
        return EAS_ERROR_MALLOC_FAILED;
    }

    /* clear the structure */
    EAS_HWMemSet(pChorusData, 0, sizeof(S_CHORUS_OBJECT));

    ChorusReadInPresets(pChorusData);

    /* set some default values */
    pChorusData->bypass =       EAS_CHORUS_BYPASS_DEFAULT;
    pChorusData->preset =       EAS_CHORUS_PRESET_DEFAULT;
    pChorusData->m_nLevel =     EAS_CHORUS_LEVEL_DEFAULT;
    pChorusData->m_nRate =      EAS_CHORUS_RATE_DEFAULT;
    pChorusData->m_nDepth =     EAS_CHORUS_DEPTH_DEFAULT;

    //chorus rate and depth need some massaging from preset value (which is sample rate independent)

    //convert rate from steps of .05 Hz to value which can be used as phase increment,
    //with current CHORUS_SHAPE_SIZE and rate limits, this fits into 16 bits
    //want to compute ((shapeSize * 65536) * (storedRate/20))/sampleRate;
    //computing it as below allows rate steps to be evenly spaced
    //uses 32 bit divide, but only once when new value is selected
    pChorusData->m_nRate = (EAS_I16)
        ((((EAS_I32)CHORUS_SHAPE_SIZE<<16)/(20*(EAS_I32)_OUTPUT_SAMPLE_RATE)) * pChorusData->m_nRate);

    //convert depth from steps of .05 ms, to samples, with 16 bit whole part, discard fraction
    //want to compute ((depth * sampleRate)/20000)
    //use the following approximation since 105/32 is roughly 65536/20000
    /*lint -e{704} use shift for performance */
    pChorusData->m_nDepth = (EAS_I16)
        (((((EAS_I32)pChorusData->m_nDepth * _OUTPUT_SAMPLE_RATE)>>5) * 105) >> 16);

    pChorusData->m_nLevel = pChorusData->m_nLevel;

    //zero delay memory for chorus
    for (index = CHORUS_L_SIZE - 1; index >= 0; index--)
    {
        pChorusData->chorusDelayL[index] = 0;
    }
    for (index = CHORUS_R_SIZE - 1; index >= 0; index--)
    {
        pChorusData->chorusDelayR[index] = 0;
    }

    //init delay line index, these are used to implement circular delay buffer
    pChorusData->chorusIndexL = 0;
    pChorusData->chorusIndexR = 0;

    //init LFO phase
    //16 bit whole part, 16 bit fraction
    pChorusData->lfoLPhase = 0;
    pChorusData->lfoRPhase = (CHORUS_SHAPE_SIZE << 16) >> 2; // 1/4 of total, i.e. 90 degrees out of phase;

    //init chorus delay position
    //right now chorus delay is a compile-time value, as is sample rate
    pChorusData->chorusTapPosition = (EAS_I16)((CHORUS_DELAY_MS * _OUTPUT_SAMPLE_RATE)/1000);

    //now copy from the new preset into Chorus
    pPreset = &pChorusData->m_sPreset.m_sPreset[pChorusData->m_nNextChorus];

    pChorusData->m_nLevel = pPreset->m_nLevel;
    pChorusData->m_nRate =  pPreset->m_nRate;
    pChorusData->m_nDepth = pPreset->m_nDepth;

    pChorusData->m_nRate = (EAS_I16)
        ((((EAS_I32)CHORUS_SHAPE_SIZE<<16)/(20*(EAS_I32)_OUTPUT_SAMPLE_RATE)) * pChorusData->m_nRate);

    /*lint -e{704} use shift for performance */
    pChorusData->m_nDepth = (EAS_I16)
        (((((EAS_I32)pChorusData->m_nDepth * _OUTPUT_SAMPLE_RATE)>>5) * 105) >> 16);

    *pInstData = pChorusData;

    return EAS_SUCCESS;
} /* end ChorusInit */

/*----------------------------------------------------------------------------
 * WeightedTap()
 *----------------------------------------------------------------------------
 * Purpose: Does fractional array look-up using linear interpolation
 *
 * first convert indexDesired to actual desired index by taking into account indexReference
 * then do linear interpolation between two actual samples using fractional part
 *
 * Inputs:
 * array: pointer to array of signed 16 bit values, typically either PCM data or control data
 * indexReference: the circular buffer relative offset
 * indexDesired: the fractional index we are looking up (16 bits index + 16 bits fraction)
 * indexLimit: the total size of the array, used to compute buffer wrap
 *
 * Outputs:
 * Value from the input array, linearly interpolated between two actual data values
 *
 *----------------------------------------------------------------------------
*/
static EAS_I16 WeightedTap(const EAS_I16 *array, EAS_I16 indexReference, EAS_I32 indexDesired, EAS_I16 indexLimit)
{
    EAS_I16 index;
    EAS_I16 fraction;
    EAS_I16 val1;
    EAS_I16 val2;

    //separate indexDesired into whole and fractional parts
    /*lint -e{704} use shift for performance */
    index = (EAS_I16)(indexDesired >> 16);
    /*lint -e{704} use shift for performance */
    fraction = (EAS_I16)((indexDesired>>1) & 0x07FFF); //just use 15 bits of fractional part

    //adjust whole part by indexReference
    index = indexReference - index;
    //make sure we stay within array bounds, this implements circular buffer
    while (index < 0)
    {
        index += indexLimit;
    }

    //get two adjacent values from the array
    val1 = array[index];

    //handle special case when index == 0, else typical case
    if (index == 0)
    {
        val2 = array[indexLimit-1]; //get last value from array
    }
    else
    {
        val2 = array[index-1]; //get previous value from array
    }

    //compute linear interpolation as (val1 + ((val2-val1)*fraction))
    return(val1 + (EAS_I16)MULT_EG1_EG1(val2-val1,fraction));
}

/*----------------------------------------------------------------------------
 * ChorusProcess()
 *----------------------------------------------------------------------------
 * Purpose: compute the chorus on the input buffer, and mix into output buffer
 *
 *
 * Inputs:
 * src: pointer to input buffer of PCM values to be processed
 * dst: pointer to output buffer of PCM values we are to sume the result with
 * bufSize: the number of sample frames (i.e. stereo samples) in the buffer
 *
 * Outputs:
 * None
 *
 *----------------------------------------------------------------------------
*/
//compute the chorus, and mix into output buffer
static void ChorusProcess (EAS_VOID_PTR pInstData, EAS_PCM *pSrc, EAS_PCM *pDst, EAS_I32 numSamples)
{
    EAS_I32 ix;
    EAS_I32 nChannelNumber;
    EAS_I16 lfoValueLeft;
    EAS_I16 lfoValueRight;
    EAS_I32 positionOffsetL;
    EAS_I32 positionOffsetR;
    EAS_PCM tapL;
    EAS_PCM tapR;
    EAS_I32 tempValue;
    EAS_PCM nInputSample;
    EAS_I32 nOutputSample;
    EAS_PCM *pIn;
    EAS_PCM *pOut;

    S_CHORUS_OBJECT *pChorusData;

    pChorusData = (S_CHORUS_OBJECT*) pInstData;

    //if the chorus is disabled or turned all the way down
    if (pChorusData->bypass == EAS_TRUE || pChorusData->m_nLevel == 0)
    {
        if (pSrc != pDst)
            EAS_HWMemCpy(pSrc, pDst, numSamples * NUM_OUTPUT_CHANNELS * (EAS_I32) sizeof(EAS_PCM));
        return;
    }

    if (pChorusData->m_nNextChorus != pChorusData->m_nCurrentChorus)
    {
        ChorusUpdate(pChorusData);
    }

    for (nChannelNumber = 0; nChannelNumber < NUM_OUTPUT_CHANNELS; nChannelNumber++)
    {

        pIn = pSrc + nChannelNumber;
        pOut = pDst + nChannelNumber;

        if(nChannelNumber==0)
        {
            for (ix = 0; ix < numSamples; ix++)
            {
                nInputSample = *pIn;
                pIn += NUM_OUTPUT_CHANNELS;

                //feed input into chorus delay line
                pChorusData->chorusDelayL[pChorusData->chorusIndexL] = nInputSample;

                //compute chorus lfo value using phase as fractional index into chorus shape table
                //resulting value is between -1.0 and 1.0, expressed as signed 16 bit number
                lfoValueLeft = WeightedTap(EAS_chorusShape, 0, pChorusData->lfoLPhase, CHORUS_SHAPE_SIZE);

                //scale chorus depth by lfo value to get relative fractional sample index
                //index is expressed as 32 bit number with 16 bit fractional part
                /*lint -e{703} use shift for performance */
                positionOffsetL = pChorusData->m_nDepth * (((EAS_I32)lfoValueLeft) << 1);

                //add fixed chorus delay to get actual fractional sample index
                positionOffsetL += ((EAS_I32)pChorusData->chorusTapPosition) << 16;

                //get tap value from chorus delay using fractional sample index
                tapL = WeightedTap(pChorusData->chorusDelayL, pChorusData->chorusIndexL, positionOffsetL, CHORUS_L_SIZE);

                //scale by chorus level, then sum with input buffer contents and saturate
                tempValue = MULT_EG1_EG1(tapL, pChorusData->m_nLevel);
                nOutputSample = SATURATE(tempValue + nInputSample);

                *pOut = (EAS_I16)SATURATE(nOutputSample);
                pOut += NUM_OUTPUT_CHANNELS;


                //increment chorus delay index and make it wrap as needed
                //this implements circular buffer
                if ((pChorusData->chorusIndexL+=1) >= CHORUS_L_SIZE)
                    pChorusData->chorusIndexL = 0;

                //increment fractional lfo phase, and make it wrap as needed
                pChorusData->lfoLPhase += pChorusData->m_nRate;
                while (pChorusData->lfoLPhase >= (CHORUS_SHAPE_SIZE<<16))
                {
                    pChorusData->lfoLPhase -= (CHORUS_SHAPE_SIZE<<16);
                }
            }
        }
        else
        {
            for (ix = 0; ix < numSamples; ix++)
            {
                nInputSample = *pIn;
                pIn += NUM_OUTPUT_CHANNELS;

                //feed input into chorus delay line
                pChorusData->chorusDelayR[pChorusData->chorusIndexR] = nInputSample;

                //compute chorus lfo value using phase as fractional index into chorus shape table
                //resulting value is between -1.0 and 1.0, expressed as signed 16 bit number
                lfoValueRight = WeightedTap(EAS_chorusShape, 0, pChorusData->lfoRPhase, CHORUS_SHAPE_SIZE);

                //scale chorus depth by lfo value to get relative fractional sample index
                //index is expressed as 32 bit number with 16 bit fractional part
                /*lint -e{703} use shift for performance */
                positionOffsetR = pChorusData->m_nDepth * (((EAS_I32)lfoValueRight) << 1);

                //add fixed chorus delay to get actual fractional sample index
                positionOffsetR += ((EAS_I32)pChorusData->chorusTapPosition) << 16;

                //get tap value from chorus delay using fractional sample index
                tapR = WeightedTap(pChorusData->chorusDelayR, pChorusData->chorusIndexR, positionOffsetR, CHORUS_R_SIZE);

                //scale by chorus level, then sum with output buffer contents and saturate
                tempValue = MULT_EG1_EG1(tapR, pChorusData->m_nLevel);
                nOutputSample = SATURATE(tempValue + nInputSample);

                *pOut = (EAS_I16)SATURATE(nOutputSample);
                pOut += NUM_OUTPUT_CHANNELS;

                //increment chorus delay index and make it wrap as needed
                //this implements circular buffer
                if ((pChorusData->chorusIndexR+=1) >= CHORUS_R_SIZE)
                    pChorusData->chorusIndexR = 0;

                //increment fractional lfo phase, and make it wrap as needed
                pChorusData->lfoRPhase += pChorusData->m_nRate;
                while (pChorusData->lfoRPhase >= (CHORUS_SHAPE_SIZE<<16))
                {
                    pChorusData->lfoRPhase -= (CHORUS_SHAPE_SIZE<<16);
                }
            }
        }

    }
}  /* end ChorusProcess */



/*----------------------------------------------------------------------------
 * ChorusShutdown()
 *----------------------------------------------------------------------------
 * Purpose:
 * Initializes the Chorus effect.
 *
 * Inputs:
 * pInstData        - handle to instance data
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ChorusShutdown (EAS_DATA_HANDLE pEASData, EAS_VOID_PTR pInstData)
{
    /* check Configuration Module for static memory allocation */
    if (!pEASData->staticMemoryModel)
        EAS_HWFree(pEASData->hwInstData, pInstData);
    return EAS_SUCCESS;
} /* end ChorusShutdown */

/*----------------------------------------------------------------------------
 * ChorusGetParam()
 *----------------------------------------------------------------------------
 * Purpose:
 * Get a Chorus parameter
 *
 * Inputs:
 * pInstData        - handle to instance data
 * param            - parameter index
 * *pValue          - pointer to variable to hold retrieved value
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ChorusGetParam (EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue)
{
    S_CHORUS_OBJECT *p;

    p = (S_CHORUS_OBJECT*) pInstData;

    switch (param)
    {
        case EAS_PARAM_CHORUS_BYPASS:
            *pValue = (EAS_I32) p->bypass;
            break;
        case EAS_PARAM_CHORUS_PRESET:
            *pValue = (EAS_I8) p->m_nCurrentChorus;
            break;
        case EAS_PARAM_CHORUS_RATE:
            *pValue = (EAS_I32) p->m_nRate;
            break;
        case EAS_PARAM_CHORUS_DEPTH:
            *pValue = (EAS_I32) p->m_nDepth;
            break;
        case EAS_PARAM_CHORUS_LEVEL:
            *pValue = (EAS_I32) p->m_nLevel;
            break;
        default:
            return EAS_ERROR_INVALID_PARAMETER;
    }
    return EAS_SUCCESS;
} /* end ChorusGetParam */


/*----------------------------------------------------------------------------
 * ChorusSetParam()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set a Chorus parameter
 *
 * Inputs:
 * pInstData        - handle to instance data
 * param            - parameter index
 * *pValue          - new paramter value
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ChorusSetParam (EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value)
{
    S_CHORUS_OBJECT *p;

    p = (S_CHORUS_OBJECT*) pInstData;

    switch (param)
    {
        case EAS_PARAM_CHORUS_BYPASS:
            p->bypass = (EAS_BOOL) value;
            break;
        case EAS_PARAM_CHORUS_PRESET:
            if(value!=EAS_PARAM_CHORUS_PRESET1 && value!=EAS_PARAM_CHORUS_PRESET2 &&
                value!=EAS_PARAM_CHORUS_PRESET3 && value!=EAS_PARAM_CHORUS_PRESET4)
                return EAS_ERROR_INVALID_PARAMETER;
            p->m_nNextChorus = (EAS_I8)value;
            break;
        case EAS_PARAM_CHORUS_RATE:
            if(value<EAS_CHORUS_RATE_MIN || value>EAS_CHORUS_RATE_MAX)
                return EAS_ERROR_INVALID_PARAMETER;
            p->m_nRate = (EAS_I16) value;
            break;
        case EAS_PARAM_CHORUS_DEPTH:
            if(value<EAS_CHORUS_DEPTH_MIN || value>EAS_CHORUS_DEPTH_MAX)
                return EAS_ERROR_INVALID_PARAMETER;
            p->m_nDepth = (EAS_I16) value;
            break;
        case EAS_PARAM_CHORUS_LEVEL:
            if(value<EAS_CHORUS_LEVEL_MIN || value>EAS_CHORUS_LEVEL_MAX)
                return EAS_ERROR_INVALID_PARAMETER;
            p->m_nLevel = (EAS_I16) value;
            break;

        default:
            return EAS_ERROR_INVALID_PARAMETER;
    }
    return EAS_SUCCESS;
} /* end ChorusSetParam */


/*----------------------------------------------------------------------------
 * ChorusReadInPresets()
 *----------------------------------------------------------------------------
 * Purpose: sets global Chorus preset bank to defaults
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ChorusReadInPresets(S_CHORUS_OBJECT *pChorusData)
{

    int preset = 0;
    int defaultPreset = 0;

    //now init any remaining presets to defaults
    for (defaultPreset = preset; defaultPreset < CHORUS_MAX_TYPE; defaultPreset++)
    {
        S_CHORUS_PRESET *pPreset = &pChorusData->m_sPreset.m_sPreset[defaultPreset];
        if (defaultPreset == 0 || defaultPreset > CHORUS_MAX_TYPE-1)
        {
            pPreset->m_nDepth = 39;
            pPreset->m_nRate = 30;
            pPreset->m_nLevel = 32767;
        }
        else if (defaultPreset == 1)
        {
            pPreset->m_nDepth = 21;
            pPreset->m_nRate = 45;
            pPreset->m_nLevel = 25000;
        }
        else if (defaultPreset == 2)
        {
            pPreset->m_nDepth = 53;
            pPreset->m_nRate = 25;
            pPreset->m_nLevel = 32000;
        }
        else if (defaultPreset == 3)
        {
            pPreset->m_nDepth = 32;
            pPreset->m_nRate = 37;
            pPreset->m_nLevel = 29000;
        }
    }

    return EAS_SUCCESS;
}


/*----------------------------------------------------------------------------
 * ChorusUpdate
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the Chorus preset parameters as required
 *
 * Inputs:
 *
 * Outputs:
 *
 *
 * Side Effects:
 * - chorus paramters will be changed
 * - m_nCurrentRoom := m_nNextRoom
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ChorusUpdate(S_CHORUS_OBJECT *pChorusData)
{
    S_CHORUS_PRESET *pPreset = &pChorusData->m_sPreset.m_sPreset[pChorusData->m_nNextChorus];

    pChorusData->m_nLevel = pPreset->m_nLevel;
    pChorusData->m_nRate =  pPreset->m_nRate;
    pChorusData->m_nDepth = pPreset->m_nDepth;

    pChorusData->m_nRate = (EAS_I16)
        ((((EAS_I32)CHORUS_SHAPE_SIZE<<16)/(20*(EAS_I32)_OUTPUT_SAMPLE_RATE)) * pChorusData->m_nRate);

    /*lint -e{704} use shift for performance */
    pChorusData->m_nDepth = (EAS_I16)
        (((((EAS_I32)pChorusData->m_nDepth * _OUTPUT_SAMPLE_RATE)>>5) * 105) >> 16);

    pChorusData->m_nCurrentChorus = pChorusData->m_nNextChorus;

    return EAS_SUCCESS;

}   /* end ChorusUpdate */
