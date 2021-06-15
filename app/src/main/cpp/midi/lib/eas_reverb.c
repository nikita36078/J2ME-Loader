/*----------------------------------------------------------------------------
 *
 * File:
 * eas_reverb.c
 *
 * Contents and purpose:
 * Contains the implementation of the Reverb effect.
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
 *   $Revision: 510 $
 *   $Date: 2006-12-19 01:47:33 -0800 (Tue, 19 Dec 2006) $
 *----------------------------------------------------------------------------
*/

/*------------------------------------
 * includes
 *------------------------------------
*/

#include "eas_data.h"
#include "eas_effects.h"
#include "eas_math.h"
#include "eas_reverbdata.h"
#include "eas_reverb.h"
#include "eas_config.h"
#include "eas_host.h"
#include "eas_report.h"

/* prototypes for effects interface */
static EAS_RESULT ReverbInit (EAS_DATA_HANDLE pEASData, EAS_VOID_PTR *pInstData);
static void ReverbProcess (EAS_VOID_PTR pInstData, EAS_PCM *pSrc, EAS_PCM *pDst, EAS_I32 numSamples);
static EAS_RESULT ReverbShutdown (EAS_DATA_HANDLE pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT ReverbGetParam (EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue);
static EAS_RESULT ReverbSetParam (EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value);

/* common effects interface for configuration module */
const S_EFFECTS_INTERFACE EAS_Reverb =
{
    ReverbInit,
    ReverbProcess,
    ReverbShutdown,
    ReverbGetParam,
    ReverbSetParam
};



/*----------------------------------------------------------------------------
 * InitializeReverb()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ReverbInit(EAS_DATA_HANDLE pEASData, EAS_VOID_PTR *pInstData)
{
    EAS_I32 i;
    EAS_U16 nOffset;
    EAS_INT temp;

    S_REVERB_OBJECT *pReverbData;
    S_REVERB_PRESET *pPreset;

    /* check Configuration Module for data allocation */
    if (pEASData->staticMemoryModel)
        pReverbData = EAS_CMEnumFXData(EAS_MODULE_REVERB);

    /* allocate dynamic memory */
    else
        pReverbData = EAS_HWMalloc(pEASData->hwInstData, sizeof(S_REVERB_OBJECT));

    if (pReverbData == NULL)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_FATAL, "Failed to allocate Reverb memory\n"); */ }
        return EAS_ERROR_MALLOC_FAILED;
    }

    /* clear the structure */
    EAS_HWMemSet(pReverbData, 0, sizeof(S_REVERB_OBJECT));

    ReverbReadInPresets(pReverbData);

    pReverbData->m_nMinSamplesToAdd = REVERB_UPDATE_PERIOD_IN_SAMPLES;

    pReverbData->m_nRevOutFbkR = 0;
    pReverbData->m_nRevOutFbkL = 0;

    pReverbData->m_sAp0.m_zApIn  = AP0_IN;
    pReverbData->m_sAp0.m_zApOut = AP0_IN + DEFAULT_AP0_LENGTH;
    pReverbData->m_sAp0.m_nApGain = DEFAULT_AP0_GAIN;

    pReverbData->m_zD0In = DELAY0_IN;

    pReverbData->m_sAp1.m_zApIn  = AP1_IN;
    pReverbData->m_sAp1.m_zApOut = AP1_IN + DEFAULT_AP1_LENGTH;
    pReverbData->m_sAp1.m_nApGain = DEFAULT_AP1_GAIN;

    pReverbData->m_zD1In = DELAY1_IN;

    pReverbData->m_zLpf0    = 0;
    pReverbData->m_zLpf1    = 0;
    pReverbData->m_nLpfFwd  = 8837;
    pReverbData->m_nLpfFbk  = 6494;

    pReverbData->m_nSin     = 0;
    pReverbData->m_nCos     = 0;
    pReverbData->m_nSinIncrement    = 0;
    pReverbData->m_nCosIncrement    = 0;

    // set xfade parameters
    pReverbData->m_nXfadeInterval = (EAS_U16)REVERB_XFADE_PERIOD_IN_SAMPLES;
    pReverbData->m_nXfadeCounter = pReverbData->m_nXfadeInterval + 1;   // force update on first iteration
    pReverbData->m_nPhase = -32768;
    pReverbData->m_nPhaseIncrement = REVERB_XFADE_PHASE_INCREMENT;

    pReverbData->m_nNoise = (EAS_I16)0xABCD;

    pReverbData->m_nMaxExcursion = 0x007F;

    // set delay tap lengths
    nOffset = ReverbCalculateNoise( pReverbData->m_nMaxExcursion,
                                    &pReverbData->m_nNoise );

    pReverbData->m_zD1Cross =
        DELAY1_OUT - pReverbData->m_nMaxExcursion + nOffset;

    nOffset = ReverbCalculateNoise( pReverbData->m_nMaxExcursion,
                                    &pReverbData->m_nNoise );

    pReverbData->m_zD0Cross =
        DELAY1_OUT - pReverbData->m_nMaxExcursion - nOffset;

    nOffset = ReverbCalculateNoise( pReverbData->m_nMaxExcursion,
                                    &pReverbData->m_nNoise );

    pReverbData->m_zD0Self  =
        DELAY0_OUT - pReverbData->m_nMaxExcursion - nOffset;

    nOffset = ReverbCalculateNoise( pReverbData->m_nMaxExcursion,
                                    &pReverbData->m_nNoise );

    pReverbData->m_zD1Self  =
        DELAY1_OUT - pReverbData->m_nMaxExcursion + nOffset;

    // for debugging purposes, allow noise generator
    pReverbData->m_bUseNoise = EAS_FALSE;

    // for debugging purposes, allow bypass
    pReverbData->m_bBypass = EAS_TRUE;  //EAS_FALSE;

    pReverbData->m_nNextRoom = 1;

    pReverbData->m_nCurrentRoom = pReverbData->m_nNextRoom + 1; // force update on first iteration

    pReverbData->m_nWet = REVERB_DEFAULT_WET;

    pReverbData->m_nDry = REVERB_DEFAULT_DRY;

    // set base index into circular buffer
    pReverbData->m_nBaseIndex = 0;

    // set the early reflections, L
    pReverbData->m_sEarlyL.m_nLpfFbk = 4915;
    pReverbData->m_sEarlyL.m_nLpfFwd = 27852;
    pReverbData->m_sEarlyL.m_zLpf = 0;

    for (i=0; i < REVERB_MAX_NUM_REFLECTIONS; i++)
    {
        pReverbData->m_sEarlyL.m_nGain[i] = 0;
        pReverbData->m_sEarlyL.m_zDelay[i] = 0;
    }

    // set the early reflections, R
    pReverbData->m_sEarlyR.m_nLpfFbk = 4915;
    pReverbData->m_sEarlyR.m_nLpfFwd = 27852;
    pReverbData->m_sEarlyR.m_zLpf = 0;

    for (i=0; i < REVERB_MAX_NUM_REFLECTIONS; i++)
    {
        pReverbData->m_sEarlyR.m_nGain[i] = 0;
        pReverbData->m_sEarlyR.m_zDelay[i] = 0;
    }

    // clear the reverb delay line
    for (i=0; i < REVERB_BUFFER_SIZE_IN_SAMPLES; i++)
    {
        pReverbData->m_nDelayLine[i] = 0;
    }

    ////////////////////////////////
    ///code from the EAS DEMO Reverb
    //now copy from the new preset into the reverb
    pPreset = &pReverbData->m_sPreset.m_sPreset[pReverbData->m_nNextRoom];

    pReverbData->m_nLpfFbk = pPreset->m_nLpfFbk;
    pReverbData->m_nLpfFwd = pPreset->m_nLpfFwd;

    pReverbData->m_nEarly = pPreset->m_nEarly;
    pReverbData->m_nWet = pPreset->m_nWet;
    pReverbData->m_nDry = pPreset->m_nDry;

    pReverbData->m_nMaxExcursion = pPreset->m_nMaxExcursion;
    //stored as time based, convert to sample based
    temp = pPreset->m_nXfadeInterval;
    /*lint -e{702} shift for performance */
    temp = (temp * _OUTPUT_SAMPLE_RATE) >> 16;
    pReverbData->m_nXfadeInterval = (EAS_U16) temp;
    //gsReverbObject.m_nXfadeInterval = pPreset->m_nXfadeInterval;

    pReverbData->m_sAp0.m_nApGain = pPreset->m_nAp0_ApGain;
    //stored as time based, convert to absolute sample value
    temp = pPreset->m_nAp0_ApOut;
    /*lint -e{702} shift for performance */
    temp = (temp * _OUTPUT_SAMPLE_RATE) >> 16;
    pReverbData->m_sAp0.m_zApOut = (EAS_U16) (pReverbData->m_sAp0.m_zApIn + temp);
    //gsReverbObject.m_sAp0.m_zApOut = pPreset->m_nAp0_ApOut;

    pReverbData->m_sAp1.m_nApGain = pPreset->m_nAp1_ApGain;
    //stored as time based, convert to absolute sample value
    temp = pPreset->m_nAp1_ApOut;
    /*lint -e{702} shift for performance */
    temp = (temp * _OUTPUT_SAMPLE_RATE) >> 16;
    pReverbData->m_sAp1.m_zApOut = (EAS_U16) (pReverbData->m_sAp1.m_zApIn + temp);
    //gsReverbObject.m_sAp1.m_zApOut = pPreset->m_nAp1_ApOut;
    ///code from the EAS DEMO Reverb
    ////////////////////////////////

    *pInstData = pReverbData;

    return EAS_SUCCESS;

}   /* end InitializeReverb */



/*----------------------------------------------------------------------------
 * ReverbProcess()
 *----------------------------------------------------------------------------
 * Purpose:
 * Reverberate the requested number of samples (block based processing)
 *
 * Inputs:
 * pInputBuffer - src buffer
 * pOutputBuffer - dst buffer
 * nNumSamplesToAdd - number of samples to write to buffer
 *
 * Outputs:
 * number of samples actually written to buffer
 *
 * Side Effects:
 * - samples are added to the presently free buffer
 *
 *----------------------------------------------------------------------------
*/
static void ReverbProcess(EAS_VOID_PTR pInstData, EAS_PCM *pSrc, EAS_PCM *pDst, EAS_I32 numSamples)
{
    S_REVERB_OBJECT *pReverbData;

    pReverbData = (S_REVERB_OBJECT*) pInstData;

    //if bypassed or the preset forces the signal to be completely dry
    if (pReverbData->m_bBypass ||
        (pReverbData->m_nWet == 0 && pReverbData->m_nDry == 32767))
    {
        if (pSrc != pDst)
            EAS_HWMemCpy(pSrc, pDst, numSamples * NUM_OUTPUT_CHANNELS * (EAS_I32) sizeof(EAS_PCM));
        return;
    }

    if (pReverbData->m_nNextRoom != pReverbData->m_nCurrentRoom)
    {
        ReverbUpdateRoom(pReverbData);
    }

    ReverbUpdateXfade(pReverbData, numSamples);

    Reverb(pReverbData, numSamples, pDst, pSrc);

    /* check if update counter needs to be reset */
    if (pReverbData->m_nUpdateCounter >= REVERB_MODULO_UPDATE_PERIOD_IN_SAMPLES)
    {
        /* update interval has elapsed, so reset counter */
        pReverbData->m_nUpdateCounter = 0;
    }   /* end if m_nUpdateCounter >= update interval */

    /* increment update counter */
    pReverbData->m_nUpdateCounter += (EAS_I16)numSamples;

}   /* end ComputeReverb */

/*----------------------------------------------------------------------------
 * ReverbUpdateXfade
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the xfade parameters as required
 *
 * Inputs:
 * nNumSamplesToAdd - number of samples to write to buffer
 *
 * Outputs:
 *
 *
 * Side Effects:
 * - xfade parameters will be changed
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ReverbUpdateXfade(S_REVERB_OBJECT *pReverbData, EAS_INT nNumSamplesToAdd)
{
    EAS_U16 nOffset;
    EAS_I16 tempCos;
    EAS_I16 tempSin;

    if (pReverbData->m_nXfadeCounter >= pReverbData->m_nXfadeInterval)
    {
        /* update interval has elapsed, so reset counter */
        pReverbData->m_nXfadeCounter = 0;

        // Pin the sin,cos values to min / max values to ensure that the
        // modulated taps' coefs are zero (thus no clicks)
        if (pReverbData->m_nPhaseIncrement > 0)
        {
            // if phase increment > 0, then sin -> 1, cos -> 0
            pReverbData->m_nSin = 32767;
            pReverbData->m_nCos = 0;

            // reset the phase to match the sin, cos values
            pReverbData->m_nPhase = 32767;

            // modulate the cross taps because their tap coefs are zero
            nOffset = ReverbCalculateNoise( pReverbData->m_nMaxExcursion, &pReverbData->m_nNoise );

            pReverbData->m_zD1Cross =
                DELAY1_OUT - pReverbData->m_nMaxExcursion + nOffset;

            nOffset = ReverbCalculateNoise( pReverbData->m_nMaxExcursion, &pReverbData->m_nNoise );

            pReverbData->m_zD0Cross =
                DELAY0_OUT - pReverbData->m_nMaxExcursion - nOffset;
        }
        else
        {
            // if phase increment < 0, then sin -> 0, cos -> 1
            pReverbData->m_nSin = 0;
            pReverbData->m_nCos = 32767;

            // reset the phase to match the sin, cos values
            pReverbData->m_nPhase = -32768;

            // modulate the self taps because their tap coefs are zero
            nOffset = ReverbCalculateNoise( pReverbData->m_nMaxExcursion, &pReverbData->m_nNoise );

            pReverbData->m_zD0Self  =
                DELAY0_OUT - pReverbData->m_nMaxExcursion - nOffset;

            nOffset = ReverbCalculateNoise( pReverbData->m_nMaxExcursion, &pReverbData->m_nNoise );

            pReverbData->m_zD1Self  =
                DELAY1_OUT - pReverbData->m_nMaxExcursion + nOffset;

        }   // end if-else (pReverbData->m_nPhaseIncrement > 0)

        // Reverse the direction of the sin,cos so that the
        // tap whose coef was previously increasing now decreases
        // and vice versa
        pReverbData->m_nPhaseIncrement = -pReverbData->m_nPhaseIncrement;

    }   // end if counter >= update interval

    //compute what phase will be next time
    pReverbData->m_nPhase += pReverbData->m_nPhaseIncrement;

    //calculate what the new sin and cos need to reach by the next update
    ReverbCalculateSinCos(pReverbData->m_nPhase, &tempSin, &tempCos);

    //calculate the per-sample increment required to get there by the next update
    /*lint -e{702} shift for performance */
    pReverbData->m_nSinIncrement =
            (tempSin - pReverbData->m_nSin) >> REVERB_UPDATE_PERIOD_IN_BITS;

    /*lint -e{702} shift for performance */
    pReverbData->m_nCosIncrement =
            (tempCos - pReverbData->m_nCos) >> REVERB_UPDATE_PERIOD_IN_BITS;


    /* increment update counter */
    pReverbData->m_nXfadeCounter += (EAS_U16) nNumSamplesToAdd;

    return EAS_SUCCESS;

}   /* end ReverbUpdateXfade */


/*----------------------------------------------------------------------------
 * ReverbCalculateNoise
 *----------------------------------------------------------------------------
 * Purpose:
 * Calculate a noise sample and limit its value
 *
 * Inputs:
 * nMaxExcursion - noise value is limited to this value
 * pnNoise - return new noise sample in this (not limited)
 *
 * Outputs:
 * new limited noise value
 *
 * Side Effects:
 * - *pnNoise noise value is updated
 *
 *----------------------------------------------------------------------------
*/
static EAS_U16 ReverbCalculateNoise(EAS_U16 nMaxExcursion, EAS_I16 *pnNoise)
{
    // calculate new noise value
    *pnNoise = (EAS_I16) (*pnNoise * 5 + 1);

#if 0   // 1xxx, test
    *pnNoise = 0;
#endif  // 1xxx, test

    // return the limited noise value
    return (nMaxExcursion & (*pnNoise));

}   /* end ReverbCalculateNoise */

/*----------------------------------------------------------------------------
 * ReverbCalculateSinCos
 *----------------------------------------------------------------------------
 * Purpose:
 * Calculate a new sin and cosine value based on the given phase
 *
 * Inputs:
 * nPhase   - phase angle
 * pnSin    - input old value, output new value
 * pnCos    - input old value, output new value
 *
 * Outputs:
 *
 * Side Effects:
 * - *pnSin, *pnCos are updated
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ReverbCalculateSinCos(EAS_I16 nPhase, EAS_I16 *pnSin, EAS_I16 *pnCos)
{
    EAS_I32 nTemp;
    EAS_I32 nNetAngle;

    //  -1 <=  nPhase  < 1
    // However, for the calculation, we need a value
    // that ranges from -1/2 to +1/2, so divide the phase by 2
    /*lint -e{702} shift for performance */
    nNetAngle = nPhase >> 1;

    /*
    Implement the following
    sin(x) = (2-4*c)*x^2 + c + x
    cos(x) = (2-4*c)*x^2 + c - x

      where  c = 1/sqrt(2)
    using the a0 + x*(a1 + x*a2) approach
    */

    /* limit the input "angle" to be between -0.5 and +0.5 */
    if (nNetAngle > EG1_HALF)
    {
        nNetAngle = EG1_HALF;
    }
    else if (nNetAngle < EG1_MINUS_HALF)
    {
        nNetAngle = EG1_MINUS_HALF;
    }

    /* calculate sin */
    nTemp = EG1_ONE + MULT_EG1_EG1(REVERB_PAN_G2, nNetAngle);
    nTemp = REVERB_PAN_G0 + MULT_EG1_EG1(nTemp, nNetAngle);
    *pnSin = (EAS_I16) SATURATE_EG1(nTemp);

    /* calculate cos */
    nTemp = -EG1_ONE + MULT_EG1_EG1(REVERB_PAN_G2, nNetAngle);
    nTemp = REVERB_PAN_G0 + MULT_EG1_EG1(nTemp, nNetAngle);
    *pnCos = (EAS_I16) SATURATE_EG1(nTemp);

    return EAS_SUCCESS;
}   /* end ReverbCalculateSinCos */

/*----------------------------------------------------------------------------
 * Reverb
 *----------------------------------------------------------------------------
 * Purpose:
 * apply reverb to the given signal
 *
 * Inputs:
 * nNu
 * pnSin    - input old value, output new value
 * pnCos    - input old value, output new value
 *
 * Outputs:
 * number of samples actually reverberated
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT Reverb(S_REVERB_OBJECT *pReverbData, EAS_INT nNumSamplesToAdd, EAS_PCM *pOutputBuffer, EAS_PCM *pInputBuffer)
{
    EAS_I32 i;
    EAS_I32 nDelayOut;
    EAS_U16 nBase;

    EAS_U32 nAddr;
    EAS_I32 nTemp1;
    EAS_I32 nTemp2;
    EAS_I32 nApIn;
    EAS_I32 nApOut;

    EAS_I32 j;
    EAS_I32 nEarlyOut;

    EAS_I32 tempValue;


    // get the base address
    nBase = pReverbData->m_nBaseIndex;

    for (i=0; i < nNumSamplesToAdd; i++)
    {
        // ********** Left Allpass - start
        // left input = (left dry/4) + right feedback from previous period
        /*lint -e{702} use shift for performance */
        nApIn = ((*pInputBuffer++)>>2) + pReverbData->m_nRevOutFbkR;
//      nApIn = *pInputBuffer++;    // 1xxx test and debug ap

        // fetch allpass delay line out
        //nAddr = CIRCULAR(nBase, psAp0->m_zApOut, REVERB_BUFFER_MASK);
        nAddr = CIRCULAR(nBase, pReverbData->m_sAp0.m_zApOut, REVERB_BUFFER_MASK);
        nDelayOut = pReverbData->m_nDelayLine[nAddr];

        // calculate allpass feedforward; subtract the feedforward result
        nTemp1 = MULT_EG1_EG1(nApIn, pReverbData->m_sAp0.m_nApGain);
        nApOut = SATURATE(nDelayOut - nTemp1);          // allpass output

        // calculate allpass feedback; add the feedback result
        nTemp1 = MULT_EG1_EG1(nApOut, pReverbData->m_sAp0.m_nApGain);
        nTemp1 = SATURATE(nApIn + nTemp1);

        // inject into allpass delay
        nAddr = CIRCULAR(nBase, pReverbData->m_sAp0.m_zApIn, REVERB_BUFFER_MASK);
        pReverbData->m_nDelayLine[nAddr] = (EAS_PCM) nTemp1;

        // inject allpass output into delay line
        nAddr = CIRCULAR(nBase, pReverbData->m_zD0In, REVERB_BUFFER_MASK);
        pReverbData->m_nDelayLine[nAddr] = (EAS_PCM) nApOut;

        // ********** Left Allpass - end

        // ********** Right Allpass - start
        // right input = (right dry/4) + left feedback from previous period
        /*lint -e{702} use shift for performance */
        nApIn = ((*pInputBuffer++)>>2) + pReverbData->m_nRevOutFbkL;
//      nApIn = *pInputBuffer++;    // 1xxx test and debug ap

        // fetch allpass delay line out
        nAddr = CIRCULAR(nBase, pReverbData->m_sAp1.m_zApOut, REVERB_BUFFER_MASK);
        nDelayOut = pReverbData->m_nDelayLine[nAddr];

        // calculate allpass feedforward; subtract the feedforward result
        nTemp1 = MULT_EG1_EG1(nApIn, pReverbData->m_sAp1.m_nApGain);
        nApOut = SATURATE(nDelayOut - nTemp1);          // allpass output

        // calculate allpass feedback; add the feedback result
        nTemp1 = MULT_EG1_EG1(nApOut, pReverbData->m_sAp1.m_nApGain);
        nTemp1 = SATURATE(nApIn + nTemp1);

        // inject into allpass delay
        nAddr = CIRCULAR(nBase, pReverbData->m_sAp1.m_zApIn, REVERB_BUFFER_MASK);
        pReverbData->m_nDelayLine[nAddr] = (EAS_PCM) nTemp1;

        // inject allpass output into delay line
        nAddr = CIRCULAR(nBase, pReverbData->m_zD1In, REVERB_BUFFER_MASK);
        pReverbData->m_nDelayLine[nAddr] = (EAS_PCM) nApOut;

        // ********** Right Allpass - end

        // ********** D0 output - start
        // fetch delay line self out
        nAddr = CIRCULAR(nBase, pReverbData->m_zD0Self, REVERB_BUFFER_MASK);
        nDelayOut = pReverbData->m_nDelayLine[nAddr];

        // calculate delay line self out
        nTemp1 = MULT_EG1_EG1(nDelayOut, pReverbData->m_nSin);

        // fetch delay line cross out
        nAddr = CIRCULAR(nBase, pReverbData->m_zD1Cross, REVERB_BUFFER_MASK);
        nDelayOut = pReverbData->m_nDelayLine[nAddr];

        // calculate delay line self out
        nTemp2 = MULT_EG1_EG1(nDelayOut, pReverbData->m_nCos);

        // calculate unfiltered delay out
        nDelayOut = SATURATE(nTemp1 + nTemp2);

        // calculate lowpass filter (mixer scale factor included in LPF feedforward)
        nTemp1 = MULT_EG1_EG1(nDelayOut, pReverbData->m_nLpfFwd);

        nTemp2 = MULT_EG1_EG1(pReverbData->m_zLpf0, pReverbData->m_nLpfFbk);

        // calculate filtered delay out and simultaneously update LPF state variable
        // filtered delay output is stored in m_zLpf0
        pReverbData->m_zLpf0 = (EAS_PCM) SATURATE(nTemp1 + nTemp2);

        // ********** D0 output - end

        // ********** D1 output - start
        // fetch delay line self out
        nAddr = CIRCULAR(nBase, pReverbData->m_zD1Self, REVERB_BUFFER_MASK);
        nDelayOut = pReverbData->m_nDelayLine[nAddr];

        // calculate delay line self out
        nTemp1 = MULT_EG1_EG1(nDelayOut, pReverbData->m_nSin);

        // fetch delay line cross out
        nAddr = CIRCULAR(nBase, pReverbData->m_zD0Cross, REVERB_BUFFER_MASK);
        nDelayOut = pReverbData->m_nDelayLine[nAddr];

        // calculate delay line self out
        nTemp2 = MULT_EG1_EG1(nDelayOut, pReverbData->m_nCos);

        // calculate unfiltered delay out
        nDelayOut = SATURATE(nTemp1 + nTemp2);

        // calculate lowpass filter (mixer scale factor included in LPF feedforward)
        nTemp1 = MULT_EG1_EG1(nDelayOut, pReverbData->m_nLpfFwd);

        nTemp2 = MULT_EG1_EG1(pReverbData->m_zLpf1, pReverbData->m_nLpfFbk);

        // calculate filtered delay out and simultaneously update LPF state variable
        // filtered delay output is stored in m_zLpf1
        pReverbData->m_zLpf1 = (EAS_PCM)SATURATE(nTemp1 + nTemp2);

        // ********** D1 output - end

        // ********** mixer and feedback - start
        // sum is fedback to right input (R + L)
        pReverbData->m_nRevOutFbkL =
            (EAS_PCM)SATURATE((EAS_I32)pReverbData->m_zLpf1 + (EAS_I32)pReverbData->m_zLpf0);

        // difference is feedback to left input (R - L)
        /*lint -e{685} lint complains that it can't saturate negative */
        pReverbData->m_nRevOutFbkR =
            (EAS_PCM)SATURATE((EAS_I32)pReverbData->m_zLpf1 - (EAS_I32)pReverbData->m_zLpf0);

        // ********** mixer and feedback - end

        // ********** start early reflection generator, left
        //psEarly = &(pReverbData->m_sEarlyL);

        nEarlyOut = 0;

        for (j=0; j < REVERB_MAX_NUM_REFLECTIONS; j++)
        {
            // fetch delay line out
            //nAddr = CIRCULAR(nBase, psEarly->m_zDelay[j], REVERB_BUFFER_MASK);
            nAddr = CIRCULAR(nBase, pReverbData->m_sEarlyL.m_zDelay[j], REVERB_BUFFER_MASK);

            nDelayOut = pReverbData->m_nDelayLine[nAddr];

            // calculate reflection
            //nTemp1 = MULT_EG1_EG1(nDelayOut, psEarly->m_nGain[j]);
            nTemp1 = MULT_EG1_EG1(nDelayOut, pReverbData->m_sEarlyL.m_nGain[j]);

            nEarlyOut = SATURATE(nEarlyOut + nTemp1);

        }   // end for (j=0; j < REVERB_MAX_NUM_REFLECTIONS; j++)

        // apply lowpass to early reflections
        //nTemp1 = MULT_EG1_EG1(nEarlyOut, psEarly->m_nLpfFwd);
        nTemp1 = MULT_EG1_EG1(nEarlyOut, pReverbData->m_sEarlyL.m_nLpfFwd);

        //nTemp2 = MULT_EG1_EG1(psEarly->m_zLpf, psEarly->m_nLpfFbk);
        nTemp2 = MULT_EG1_EG1(pReverbData->m_sEarlyL.m_zLpf, pReverbData->m_sEarlyL.m_nLpfFbk);


        // calculate filtered out and simultaneously update LPF state variable
        // filtered output is stored in m_zLpf1
        //psEarly->m_zLpf = SATURATE(nTemp1 + nTemp2);
        pReverbData->m_sEarlyL.m_zLpf = (EAS_PCM) SATURATE(nTemp1 + nTemp2);

        // combine filtered early and late reflections for output
        //*pOutputBuffer++ = inL;
        //tempValue = SATURATE(psEarly->m_zLpf + pReverbData->m_nRevOutFbkL);
        tempValue = SATURATE((EAS_I32)pReverbData->m_sEarlyL.m_zLpf + (EAS_I32)pReverbData->m_nRevOutFbkL);
        //scale reverb output by wet level
        /*lint -e{701} use shift for performance */
        tempValue = MULT_EG1_EG1(tempValue, (pReverbData->m_nWet<<1));
        //sum with output buffer
        tempValue += *pOutputBuffer;
        *pOutputBuffer++ = (EAS_PCM)SATURATE(tempValue);

        // ********** end early reflection generator, left

        // ********** start early reflection generator, right
        //psEarly = &(pReverbData->m_sEarlyR);

        nEarlyOut = 0;

        for (j=0; j < REVERB_MAX_NUM_REFLECTIONS; j++)
        {
            // fetch delay line out
            nAddr = CIRCULAR(nBase, pReverbData->m_sEarlyR.m_zDelay[j], REVERB_BUFFER_MASK);
            nDelayOut = pReverbData->m_nDelayLine[nAddr];

            // calculate reflection
            nTemp1 = MULT_EG1_EG1(nDelayOut, pReverbData->m_sEarlyR.m_nGain[j]);

            nEarlyOut = SATURATE(nEarlyOut + nTemp1);

        }   // end for (j=0; j < REVERB_MAX_NUM_REFLECTIONS; j++)

        // apply lowpass to early reflections
        nTemp1 = MULT_EG1_EG1(nEarlyOut, pReverbData->m_sEarlyR.m_nLpfFwd);

        nTemp2 = MULT_EG1_EG1(pReverbData->m_sEarlyR.m_zLpf, pReverbData->m_sEarlyR.m_nLpfFbk);

        // calculate filtered out and simultaneously update LPF state variable
        // filtered output is stored in m_zLpf1
        pReverbData->m_sEarlyR.m_zLpf = (EAS_PCM)SATURATE(nTemp1 + nTemp2);

        // combine filtered early and late reflections for output
        //*pOutputBuffer++ = inR;
        tempValue = SATURATE((EAS_I32)pReverbData->m_sEarlyR.m_zLpf + (EAS_I32)pReverbData->m_nRevOutFbkR);
        //scale reverb output by wet level
        /*lint -e{701} use shift for performance */
        tempValue = MULT_EG1_EG1(tempValue, (pReverbData->m_nWet << 1));
        //sum with output buffer
        tempValue = tempValue + *pOutputBuffer;
        *pOutputBuffer++ = (EAS_PCM)SATURATE(tempValue);

        // ********** end early reflection generator, right

        // decrement base addr for next sample period
        nBase--;

        pReverbData->m_nSin += pReverbData->m_nSinIncrement;
        pReverbData->m_nCos += pReverbData->m_nCosIncrement;

    }   // end for (i=0; i < nNumSamplesToAdd; i++)

    // store the most up to date version
    pReverbData->m_nBaseIndex = nBase;

    return EAS_SUCCESS;
}   /* end Reverb */



/*----------------------------------------------------------------------------
 * ReverbShutdown()
 *----------------------------------------------------------------------------
 * Purpose:
 * Initializes the Reverb effect.
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
static EAS_RESULT ReverbShutdown (EAS_DATA_HANDLE pEASData, EAS_VOID_PTR pInstData)
{
    /* check Configuration Module for static memory allocation */
    if (!pEASData->staticMemoryModel)
        EAS_HWFree(pEASData->hwInstData, pInstData);
    return EAS_SUCCESS;
} /* end ReverbShutdown */

/*----------------------------------------------------------------------------
 * ReverbGetParam()
 *----------------------------------------------------------------------------
 * Purpose:
 * Get a Reverb parameter
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
static EAS_RESULT ReverbGetParam (EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue)
{
    S_REVERB_OBJECT *p;

    p = (S_REVERB_OBJECT*) pInstData;

    switch (param)
    {
        case EAS_PARAM_REVERB_BYPASS:
            *pValue = (EAS_I32) p->m_bBypass;
            break;
        case EAS_PARAM_REVERB_PRESET:
            *pValue = (EAS_I8) p->m_nCurrentRoom;
            break;
        case EAS_PARAM_REVERB_WET:
            *pValue = p->m_nWet;
            break;
        case EAS_PARAM_REVERB_DRY:
            *pValue = p->m_nDry;
            break;
        default:
            return EAS_ERROR_INVALID_PARAMETER;
    }
    return EAS_SUCCESS;
} /* end ReverbGetParam */


/*----------------------------------------------------------------------------
 * ReverbSetParam()
 *----------------------------------------------------------------------------
 * Purpose:
 * Set a Reverb parameter
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
static EAS_RESULT ReverbSetParam (EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value)
{
    S_REVERB_OBJECT *p;

    p = (S_REVERB_OBJECT*) pInstData;

    switch (param)
    {
        case EAS_PARAM_REVERB_BYPASS:
            p->m_bBypass = (EAS_BOOL) value;
            break;
        case EAS_PARAM_REVERB_PRESET:
            if(value!=EAS_PARAM_REVERB_LARGE_HALL && value!=EAS_PARAM_REVERB_HALL &&
                value!=EAS_PARAM_REVERB_CHAMBER && value!=EAS_PARAM_REVERB_ROOM)
                return EAS_ERROR_INVALID_PARAMETER;
            p->m_nNextRoom = (EAS_I16)value;
            break;
        case EAS_PARAM_REVERB_WET:
            if(value>EAS_REVERB_WET_MAX || value<EAS_REVERB_WET_MIN)
                return EAS_ERROR_INVALID_PARAMETER;
            p->m_nWet = (EAS_I16)value;
            break;
        case EAS_PARAM_REVERB_DRY:
            if(value>EAS_REVERB_DRY_MAX || value<EAS_REVERB_DRY_MIN)
                return EAS_ERROR_INVALID_PARAMETER;
            p->m_nDry = (EAS_I16)value;
            break;
        default:
            return EAS_ERROR_INVALID_PARAMETER;
    }
    return EAS_SUCCESS;
} /* end ReverbSetParam */


/*----------------------------------------------------------------------------
 * ReverbUpdateRoom
 *----------------------------------------------------------------------------
 * Purpose:
 * Update the room's preset parameters as required
 *
 * Inputs:
 *
 * Outputs:
 *
 *
 * Side Effects:
 * - reverb paramters (fbk, fwd, etc) will be changed
 * - m_nCurrentRoom := m_nNextRoom
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ReverbUpdateRoom(S_REVERB_OBJECT *pReverbData)
{
    EAS_INT temp;

    S_REVERB_PRESET *pPreset = &pReverbData->m_sPreset.m_sPreset[pReverbData->m_nNextRoom];

    pReverbData->m_nLpfFwd = pPreset->m_nLpfFwd;
    pReverbData->m_nLpfFbk = pPreset->m_nLpfFbk;

    pReverbData->m_nEarly = pPreset->m_nEarly;
    pReverbData->m_nWet = pPreset->m_nWet;
    pReverbData->m_nDry = pPreset->m_nDry;


    pReverbData->m_nMaxExcursion = pPreset->m_nMaxExcursion;
    //stored as time based, convert to sample based
    temp = pPreset->m_nXfadeInterval;
    /*lint -e{702} shift for performance */
    temp = (temp * _OUTPUT_SAMPLE_RATE) >> 16;
    pReverbData->m_nXfadeInterval = (EAS_U16) temp;
    //gpsReverbObject->m_nXfadeInterval = pPreset->m_nXfadeInterval;
    pReverbData->m_sAp0.m_nApGain = pPreset->m_nAp0_ApGain;
    //stored as time based, convert to absolute sample value
    temp = pPreset->m_nAp0_ApOut;
    /*lint -e{702} shift for performance */
    temp = (temp * _OUTPUT_SAMPLE_RATE) >> 16;
    pReverbData->m_sAp0.m_zApOut = (EAS_U16) (pReverbData->m_sAp0.m_zApIn + temp);
    //gpsReverbObject->m_sAp0.m_zApOut = pPreset->m_nAp0_ApOut;
    pReverbData->m_sAp1.m_nApGain = pPreset->m_nAp1_ApGain;
    //stored as time based, convert to absolute sample value
    temp = pPreset->m_nAp1_ApOut;
    /*lint -e{702} shift for performance */
    temp = (temp * _OUTPUT_SAMPLE_RATE) >> 16;
    pReverbData->m_sAp1.m_zApOut = (EAS_U16) (pReverbData->m_sAp1.m_zApIn + temp);
    //gpsReverbObject->m_sAp1.m_zApOut = pPreset->m_nAp1_ApOut;

    pReverbData->m_nCurrentRoom = pReverbData->m_nNextRoom;

    return EAS_SUCCESS;

}   /* end ReverbUpdateRoom */


/*----------------------------------------------------------------------------
 * ReverbReadInPresets()
 *----------------------------------------------------------------------------
 * Purpose: sets global reverb preset bank to defaults
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT ReverbReadInPresets(S_REVERB_OBJECT *pReverbData)
{

    int preset = 0;
    int defaultPreset = 0;

    //now init any remaining presets to defaults
    for (defaultPreset = preset; defaultPreset < REVERB_MAX_ROOM_TYPE; defaultPreset++)
    {
        S_REVERB_PRESET *pPreset = &pReverbData->m_sPreset.m_sPreset[defaultPreset];
        if (defaultPreset == 0 || defaultPreset > REVERB_MAX_ROOM_TYPE-1)
        {
            pPreset->m_nLpfFbk = 8307;
            pPreset->m_nLpfFwd = 14768;
            pPreset->m_nEarly = 0;
            pPreset->m_nWet = 27690;
            pPreset->m_nDry = 32767;
            pPreset->m_nEarlyL_LpfFbk = 3692;
            pPreset->m_nEarlyL_LpfFwd = 29075;
            pPreset->m_nEarlyL_Delay0 = 922;
            pPreset->m_nEarlyL_Gain0 = 22152;
            pPreset->m_nEarlyL_Delay1 = 1462;
            pPreset->m_nEarlyL_Gain1 = 17537;
            pPreset->m_nEarlyL_Delay2 = 0;
            pPreset->m_nEarlyL_Gain2 = 14768;
            pPreset->m_nEarlyL_Delay3 = 1221;
            pPreset->m_nEarlyL_Gain3 = 14307;
            pPreset->m_nEarlyL_Delay4 = 0;
            pPreset->m_nEarlyL_Gain4 = 13384;
            pPreset->m_nEarlyR_Delay0 = 502;
            pPreset->m_nEarlyR_Gain0 = 20306;
            pPreset->m_nEarlyR_Delay1 = 1762;
            pPreset->m_nEarlyR_Gain1 = 17537;
            pPreset->m_nEarlyR_Delay2 = 0;
            pPreset->m_nEarlyR_Gain2 = 14768;
            pPreset->m_nEarlyR_Delay3 = 0;
            pPreset->m_nEarlyR_Gain3 = 16153;
            pPreset->m_nEarlyR_Delay4 = 0;
            pPreset->m_nEarlyR_Gain4 = 13384;
            pPreset->m_nMaxExcursion = 127;
            pPreset->m_nXfadeInterval = 6388;
            pPreset->m_nAp0_ApGain = 15691;
            pPreset->m_nAp0_ApOut = 711;
            pPreset->m_nAp1_ApGain = 17999;
            pPreset->m_nAp1_ApOut = 1113;
            pPreset->m_rfu4 = 0;
            pPreset->m_rfu5 = 0;
            pPreset->m_rfu6 = 0;
            pPreset->m_rfu7 = 0;
            pPreset->m_rfu8 = 0;
            pPreset->m_rfu9 = 0;
            pPreset->m_rfu10 = 0;
        }
        else if (defaultPreset == 1)
        {
            pPreset->m_nLpfFbk = 6461;
            pPreset->m_nLpfFwd = 14307;
            pPreset->m_nEarly = 0;
            pPreset->m_nWet = 27690;
            pPreset->m_nDry = 32767;
            pPreset->m_nEarlyL_LpfFbk = 3692;
            pPreset->m_nEarlyL_LpfFwd = 29075;
            pPreset->m_nEarlyL_Delay0 = 922;
            pPreset->m_nEarlyL_Gain0 = 22152;
            pPreset->m_nEarlyL_Delay1 = 1462;
            pPreset->m_nEarlyL_Gain1 = 17537;
            pPreset->m_nEarlyL_Delay2 = 0;
            pPreset->m_nEarlyL_Gain2 = 14768;
            pPreset->m_nEarlyL_Delay3 = 1221;
            pPreset->m_nEarlyL_Gain3 = 14307;
            pPreset->m_nEarlyL_Delay4 = 0;
            pPreset->m_nEarlyL_Gain4 = 13384;
            pPreset->m_nEarlyR_Delay0 = 502;
            pPreset->m_nEarlyR_Gain0 = 20306;
            pPreset->m_nEarlyR_Delay1 = 1762;
            pPreset->m_nEarlyR_Gain1 = 17537;
            pPreset->m_nEarlyR_Delay2 = 0;
            pPreset->m_nEarlyR_Gain2 = 14768;
            pPreset->m_nEarlyR_Delay3 = 0;
            pPreset->m_nEarlyR_Gain3 = 16153;
            pPreset->m_nEarlyR_Delay4 = 0;
            pPreset->m_nEarlyR_Gain4 = 13384;
            pPreset->m_nMaxExcursion = 127;
            pPreset->m_nXfadeInterval = 6391;
            pPreset->m_nAp0_ApGain = 15230;
            pPreset->m_nAp0_ApOut = 708;
            pPreset->m_nAp1_ApGain = 9692;
            pPreset->m_nAp1_ApOut = 1113;
            pPreset->m_rfu4 = 0;
            pPreset->m_rfu5 = 0;
            pPreset->m_rfu6 = 0;
            pPreset->m_rfu7 = 0;
            pPreset->m_rfu8 = 0;
            pPreset->m_rfu9 = 0;
            pPreset->m_rfu10 = 0;
        }
        else if (defaultPreset == 2)
        {
            pPreset->m_nLpfFbk = 5077;
            pPreset->m_nLpfFwd = 12922;
            pPreset->m_nEarly = 0;
            pPreset->m_nWet = 24460;
            pPreset->m_nDry = 32767;
            pPreset->m_nEarlyL_LpfFbk = 3692;
            pPreset->m_nEarlyL_LpfFwd = 29075;
            pPreset->m_nEarlyL_Delay0 = 922;
            pPreset->m_nEarlyL_Gain0 = 22152;
            pPreset->m_nEarlyL_Delay1 = 1462;
            pPreset->m_nEarlyL_Gain1 = 17537;
            pPreset->m_nEarlyL_Delay2 = 0;
            pPreset->m_nEarlyL_Gain2 = 14768;
            pPreset->m_nEarlyL_Delay3 = 1221;
            pPreset->m_nEarlyL_Gain3 = 14307;
            pPreset->m_nEarlyL_Delay4 = 0;
            pPreset->m_nEarlyL_Gain4 = 13384;
            pPreset->m_nEarlyR_Delay0 = 502;
            pPreset->m_nEarlyR_Gain0 = 20306;
            pPreset->m_nEarlyR_Delay1 = 1762;
            pPreset->m_nEarlyR_Gain1 = 17537;
            pPreset->m_nEarlyR_Delay2 = 0;
            pPreset->m_nEarlyR_Gain2 = 14768;
            pPreset->m_nEarlyR_Delay3 = 0;
            pPreset->m_nEarlyR_Gain3 = 16153;
            pPreset->m_nEarlyR_Delay4 = 0;
            pPreset->m_nEarlyR_Gain4 = 13384;
            pPreset->m_nMaxExcursion = 127;
            pPreset->m_nXfadeInterval = 6449;
            pPreset->m_nAp0_ApGain = 15691;
            pPreset->m_nAp0_ApOut = 774;
            pPreset->m_nAp1_ApGain = 15691;
            pPreset->m_nAp1_ApOut = 1113;
            pPreset->m_rfu4 = 0;
            pPreset->m_rfu5 = 0;
            pPreset->m_rfu6 = 0;
            pPreset->m_rfu7 = 0;
            pPreset->m_rfu8 = 0;
            pPreset->m_rfu9 = 0;
            pPreset->m_rfu10 = 0;
        }
        else if (defaultPreset == 3)
        {
            pPreset->m_nLpfFbk = 5077;
            pPreset->m_nLpfFwd = 11076;
            pPreset->m_nEarly = 0;
            pPreset->m_nWet = 23075;
            pPreset->m_nDry = 32767;
            pPreset->m_nEarlyL_LpfFbk = 3692;
            pPreset->m_nEarlyL_LpfFwd = 29075;
            pPreset->m_nEarlyL_Delay0 = 922;
            pPreset->m_nEarlyL_Gain0 = 22152;
            pPreset->m_nEarlyL_Delay1 = 1462;
            pPreset->m_nEarlyL_Gain1 = 17537;
            pPreset->m_nEarlyL_Delay2 = 0;
            pPreset->m_nEarlyL_Gain2 = 14768;
            pPreset->m_nEarlyL_Delay3 = 1221;
            pPreset->m_nEarlyL_Gain3 = 14307;
            pPreset->m_nEarlyL_Delay4 = 0;
            pPreset->m_nEarlyL_Gain4 = 13384;
            pPreset->m_nEarlyR_Delay0 = 502;
            pPreset->m_nEarlyR_Gain0 = 20306;
            pPreset->m_nEarlyR_Delay1 = 1762;
            pPreset->m_nEarlyR_Gain1 = 17537;
            pPreset->m_nEarlyR_Delay2 = 0;
            pPreset->m_nEarlyR_Gain2 = 14768;
            pPreset->m_nEarlyR_Delay3 = 0;
            pPreset->m_nEarlyR_Gain3 = 16153;
            pPreset->m_nEarlyR_Delay4 = 0;
            pPreset->m_nEarlyR_Gain4 = 13384;
            pPreset->m_nMaxExcursion = 127;
            pPreset->m_nXfadeInterval = 6470;   //6483;
            pPreset->m_nAp0_ApGain = 14768;
            pPreset->m_nAp0_ApOut = 792;
            pPreset->m_nAp1_ApGain = 15783;
            pPreset->m_nAp1_ApOut = 1113;
            pPreset->m_rfu4 = 0;
            pPreset->m_rfu5 = 0;
            pPreset->m_rfu6 = 0;
            pPreset->m_rfu7 = 0;
            pPreset->m_rfu8 = 0;
            pPreset->m_rfu9 = 0;
            pPreset->m_rfu10 = 0;

        }
    }

    return EAS_SUCCESS;
}
