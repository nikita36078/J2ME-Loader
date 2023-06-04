/*----------------------------------------------------------------------------
 *
 * File:
 * eas_mixer.c
 *
 * Contents and purpose:
 * This file contains the critical components of the mix engine that
 * must be optimized for best performance.
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
 *   $Revision: 706 $
 *   $Date: 2007-05-31 17:22:51 -0700 (Thu, 31 May 2007) $
 *----------------------------------------------------------------------------
*/

//3 dls: This module is in the midst of being converted from a synth
//3 specific module to a general purpose mix engine

/*------------------------------------
 * includes
 *------------------------------------
*/
#include "eas_data.h"
#include "eas_host.h"
#include "eas_math.h"
#include "eas_mixer.h"
#include "eas_config.h"
#include "eas_report.h"

#ifdef _MAXIMIZER_ENABLED
EAS_I32 MaximizerProcess (EAS_VOID_PTR pInstData, EAS_I32 *pSrc, EAS_I32 *pDst, EAS_I32 numSamples);
#endif

/*------------------------------------
 * defines
 *------------------------------------
*/

/* need to boost stereo by ~3dB to compensate for the panner */
#define STEREO_3DB_GAIN_BOOST       512

/*----------------------------------------------------------------------------
 * EAS_MixEngineInit()
 *----------------------------------------------------------------------------
 * Purpose:
 * Prepares the mix engine for work, allocates buffers, locates effects modules, etc.
 *
 * Inputs:
 * pEASData         - instance data
 * pInstData        - pointer to variable to receive instance data handle
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_MixEngineInit (S_EAS_DATA *pEASData)
{

    /* check Configuration Module for mix buffer allocation */
    if (pEASData->staticMemoryModel)
        pEASData->pMixBuffer = EAS_CMEnumData(EAS_CM_MIX_BUFFER);
    else
        pEASData->pMixBuffer = EAS_HWMalloc(pEASData->hwInstData, BUFFER_SIZE_IN_MONO_SAMPLES * NUM_OUTPUT_CHANNELS * sizeof(EAS_I32));
    if (pEASData->pMixBuffer == NULL)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_FATAL, "Failed to allocate mix buffer memory\n"); */ }
        return EAS_ERROR_MALLOC_FAILED;
    }
    EAS_HWMemSet((void *)(pEASData->pMixBuffer), 0, BUFFER_SIZE_IN_MONO_SAMPLES * NUM_OUTPUT_CHANNELS * sizeof(EAS_I32));

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_MixEnginePrep()
 *----------------------------------------------------------------------------
 * Purpose:
 * Performs prep before synthesize a buffer of audio, such as clearing
 * audio buffers, etc.
 *
 * Inputs:
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
void EAS_MixEnginePrep (S_EAS_DATA *pEASData, EAS_I32 numSamples)
{

    /* clear the mix buffer */
#if (NUM_OUTPUT_CHANNELS == 2)
    EAS_HWMemSet(pEASData->pMixBuffer, 0, numSamples * (EAS_I32) sizeof(long) * 2);
#else
    EAS_HWMemSet(pEASData->pMixBuffer, 0, (EAS_I32) numSamples * (EAS_I32) sizeof(long));
#endif

    /* need to clear other side-chain effect buffers (chorus & reverb) */
}

/*----------------------------------------------------------------------------
 * EAS_MixEnginePost
 *----------------------------------------------------------------------------
 * Purpose:
 * This routine does the post-processing after all voices have been
 * synthesized. It calls any sweeteners and does the final mixdown to
 * the output buffer.
 *
 * Inputs:
 *
 * Outputs:
 *
 * Notes:
 *----------------------------------------------------------------------------
*/
void EAS_MixEnginePost (S_EAS_DATA *pEASData, EAS_I32 numSamples)
{
    EAS_U16 gain;

//3 dls: Need to restore the mix engine metrics

    /* calculate the gain multiplier */
#ifdef _MAXIMIZER_ENABLED
    if (pEASData->effectsModules[EAS_MODULE_MAXIMIZER].effect)
    {
        EAS_I32 temp;
        temp = MaximizerProcess(pEASData->effectsModules[EAS_MODULE_MAXIMIZER].effectData, pEASData->pMixBuffer, pEASData->pMixBuffer, numSamples);
        temp = (temp * pEASData->masterGain) >> 15;
        if (temp > 32767)
            gain = 32767;
        else
            gain = (EAS_U16) temp;
    }
    else
        gain = (EAS_U16) pEASData->masterGain;
#else
    gain = (EAS_U16) pEASData->masterGain;
#endif

    /* Not using all the gain bits for now
     * Reduce the input to the compressor by 6dB to prevent saturation
     */
#ifdef _COMPRESSOR_ENABLED
    if (pEASData->effectsModules[EAS_MODULE_COMPRESSOR].effectData)
        gain = gain >> 5;
    else
        gain = gain >> 4;
#else
    gain = gain >> 4;
#endif

    /* convert 32-bit mix buffer to 16-bit output format */
#if (NUM_OUTPUT_CHANNELS == 2)
    SynthMasterGain(pEASData->pMixBuffer, pEASData->pOutputAudioBuffer, gain, (EAS_U16) ((EAS_U16) numSamples * 2));
#else
    SynthMasterGain(pEASData->pMixBuffer, pEASData->pOutputAudioBuffer, gain, (EAS_U16) numSamples);
#endif

#ifdef _ENHANCER_ENABLED
    /* enhancer effect */
    if (pEASData->effectsModules[EAS_MODULE_ENHANCER].effectData)
        (*pEASData->effectsModules[EAS_MODULE_ENHANCER].effect->pfProcess)
            (pEASData->effectsModules[EAS_MODULE_ENHANCER].effectData,
            pEASData->pOutputAudioBuffer,
            pEASData->pOutputAudioBuffer,
            numSamples);
#endif

#ifdef _GRAPHIC_EQ_ENABLED
    /* graphic EQ effect */
    if (pEASData->effectsModules[EAS_MODULE_GRAPHIC_EQ].effectData)
        (*pEASData->effectsModules[EAS_MODULE_GRAPHIC_EQ].effect->pfProcess)
            (pEASData->effectsModules[EAS_MODULE_GRAPHIC_EQ].effectData,
            pEASData->pOutputAudioBuffer,
            pEASData->pOutputAudioBuffer,
            numSamples);
#endif

#ifdef _COMPRESSOR_ENABLED
    /* compressor effect */
    if (pEASData->effectsModules[EAS_MODULE_COMPRESSOR].effectData)
        (*pEASData->effectsModules[EAS_MODULE_COMPRESSOR].effect->pfProcess)
            (pEASData->effectsModules[EAS_MODULE_COMPRESSOR].effectData,
            pEASData->pOutputAudioBuffer,
            pEASData->pOutputAudioBuffer,
            numSamples);
#endif

#ifdef _WOW_ENABLED
    /* WOW requires a 32-bit buffer, borrow the mix buffer and
     * pass it as the destination buffer
     */
    /*lint -e{740} temporarily passing a parameter through an existing I/F */
    if (pEASData->effectsModules[EAS_MODULE_WOW].effectData)
        (*pEASData->effectsModules[EAS_MODULE_WOW].effect->pfProcess)
            (pEASData->effectsModules[EAS_MODULE_WOW].effectData,
            pEASData->pOutputAudioBuffer,
            (EAS_PCM*) pEASData->pMixBuffer,
            numSamples);
#endif

#ifdef _TONECONTROLEQ_ENABLED
    /* ToneControlEQ effect */
    if (pEASData->effectsModules[EAS_MODULE_TONECONTROLEQ].effectData)
        (*pEASData->effectsModules[EAS_MODULE_TONECONTROLEQ].effect->pfProcess)
            (pEASData->effectsModules[EAS_MODULE_TONECONTROLEQ].effectData,
            pEASData->pOutputAudioBuffer,
            pEASData->pOutputAudioBuffer,
            numSamples);
#endif

#ifdef _REVERB_ENABLED
    /* Reverb effect */
    if (pEASData->effectsModules[EAS_MODULE_REVERB].effectData)
        (*pEASData->effectsModules[EAS_MODULE_REVERB].effect->pfProcess)
            (pEASData->effectsModules[EAS_MODULE_REVERB].effectData,
            pEASData->pOutputAudioBuffer,
            pEASData->pOutputAudioBuffer,
            numSamples);
#endif

#ifdef _CHORUS_ENABLED
    /* Chorus effect */
    if (pEASData->effectsModules[EAS_MODULE_CHORUS].effectData)
        (*pEASData->effectsModules[EAS_MODULE_CHORUS].effect->pfProcess)
            (pEASData->effectsModules[EAS_MODULE_CHORUS].effectData,
            pEASData->pOutputAudioBuffer,
            pEASData->pOutputAudioBuffer,
            numSamples);
#endif

}

#ifndef NATIVE_EAS_KERNEL
/*----------------------------------------------------------------------------
 * SynthMasterGain
 *----------------------------------------------------------------------------
 * Purpose:
 * Mixes down audio from 32-bit to 16-bit target buffer
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
void SynthMasterGain (long *pInputBuffer, EAS_PCM *pOutputBuffer, EAS_U16 nGain, EAS_U16 numSamples) {

    /* loop through the buffer */
    while (numSamples) {
        long s;

        numSamples--;
        /* read a sample from the input buffer and add some guard bits */
        s = *pInputBuffer++;

        /* add some guard bits */
        /*lint -e{704} <avoid divide for performance>*/
        s = s >> 7;

        /* apply master gain */
        s *= (long) nGain;

        /* shift to lower 16-bits */
        /*lint -e{704} <avoid divide for performance>*/
        s = s >> 9;

        /* saturate */
        s = SATURATE(s);

        *pOutputBuffer++ = (EAS_PCM)s;
    }
}
#endif

/*----------------------------------------------------------------------------
 * EAS_MixEngineShutdown()
 *----------------------------------------------------------------------------
 * Purpose:
 * Shuts down effects modules and deallocates memory
 *
 * Inputs:
 * pEASData         - instance data
 * pInstData        - instance data handle
 *
 * Outputs:
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_MixEngineShutdown (S_EAS_DATA *pEASData)
{

    /* check Configuration Module for static memory allocation */
    if (!pEASData->staticMemoryModel && (pEASData->pMixBuffer != NULL))
        EAS_HWFree(pEASData->hwInstData, pEASData->pMixBuffer);

    return EAS_SUCCESS;
}

#ifdef UNIFIED_MIXER
#ifndef NATIVE_MIX_STREAM
/*----------------------------------------------------------------------------
 * EAS_MixStream
 *----------------------------------------------------------------------------
 * Mix a 16-bit stream into a 32-bit buffer
 *
 * pInputBuffer 16-bit input buffer
 * pMixBuffer   32-bit mix buffer
 * numSamples   number of samples to mix
 * gainLeft     initial gain left or mono
 * gainRight    initial gain right
 * gainLeft     left gain increment per sample
 * gainRight    right gain increment per sample
 * flags        bit 0 = stereo source
 *              bit 1 = stereo output
 *----------------------------------------------------------------------------
*/
void EAS_MixStream (EAS_PCM *pInputBuffer, EAS_I32 *pMixBuffer, EAS_I32 numSamples, EAS_I32 gainLeft, EAS_I32 gainRight, EAS_I32 gainIncLeft, EAS_I32 gainIncRight, EAS_I32 flags)
{
    EAS_I32 temp;
    EAS_INT src, dest;

    /* NOTE: There are a lot of optimizations that can be done
     * in the native implementations based on register
     * availability, etc. For example, it may make sense to
     * break this down into 8 separate routines:
     *
     * 1. Mono source to mono output
     * 2. Mono source to stereo output
     * 3. Stereo source to mono output
     * 4. Stereo source to stereo output
     * 5. Mono source to mono output - no gain change
     * 6. Mono source to stereo output - no gain change
     * 7. Stereo source to mono output - no gain change
     * 8. Stereo source to stereo output - no gain change
     *
     * Other possibilities include loop unrolling, skipping
     * a gain calculation every 2 or 4 samples, etc.
     */

    /* no gain change, use fast loops */
    if ((gainIncLeft == 0) && (gainIncRight == 0))
    {
        switch (flags & (MIX_FLAGS_STEREO_SOURCE | MIX_FLAGS_STEREO_OUTPUT))
        {
            /* mono to mono */
            case 0:
                gainLeft >>= 15;
                for (src = dest = 0; src < numSamples; src++, dest++)
                {

                    pMixBuffer[dest] += (pInputBuffer[src] * gainLeft) >> NUM_MIXER_GUARD_BITS;
                }
                break;

            /* mono to stereo */
            case MIX_FLAGS_STEREO_OUTPUT:
                gainLeft >>= 15;
                gainRight >>= 15;
                for (src = dest = 0; src < numSamples; src++, dest+=2)
                {
                    pMixBuffer[dest] += (pInputBuffer[src] * gainLeft) >> NUM_MIXER_GUARD_BITS;
                    pMixBuffer[dest+1] += (pInputBuffer[src] * gainRight) >> NUM_MIXER_GUARD_BITS;
                }
                break;

            /* stereo to mono */
            case MIX_FLAGS_STEREO_SOURCE:
                gainLeft >>= 15;
                gainRight >>= 15;
                for (src = dest = 0; src < numSamples; src+=2, dest++)
                {
                    temp = (pInputBuffer[src] * gainLeft) >> NUM_MIXER_GUARD_BITS;
                    temp += ((pInputBuffer[src+1] * gainRight) >> NUM_MIXER_GUARD_BITS);
                    pMixBuffer[dest] += temp;
                }
                break;

            /* stereo to stereo */
            case MIX_FLAGS_STEREO_SOURCE | MIX_FLAGS_STEREO_OUTPUT:
                gainLeft >>= 15;
                gainRight >>= 15;
                for (src = dest = 0; src < numSamples; src+=2, dest+=2)
                {
                    pMixBuffer[dest] += (pInputBuffer[src] * gainLeft) >> NUM_MIXER_GUARD_BITS;
                    pMixBuffer[dest+1] += (pInputBuffer[src+1] * gainRight) >> NUM_MIXER_GUARD_BITS;
                }
                break;
        }
    }

    /* gain change - do gain increment */
    else
    {
        switch (flags & (MIX_FLAGS_STEREO_SOURCE | MIX_FLAGS_STEREO_OUTPUT))
        {
            /* mono to mono */
            case 0:
                for (src = dest = 0; src < numSamples; src++, dest++)
                {
                    gainLeft += gainIncLeft;
                    pMixBuffer[dest] += (pInputBuffer[src] * (gainLeft >> 15)) >> NUM_MIXER_GUARD_BITS;
                }
                break;

            /* mono to stereo */
            case MIX_FLAGS_STEREO_OUTPUT:
                for (src = dest = 0; src < numSamples; src++, dest+=2)
                {
                    gainLeft += gainIncLeft;
                    gainRight += gainIncRight;
                    pMixBuffer[dest] += (pInputBuffer[src] * (gainLeft >> 15)) >> NUM_MIXER_GUARD_BITS;
                    pMixBuffer[dest+1] += (pInputBuffer[src] * (gainRight >> 15)) >> NUM_MIXER_GUARD_BITS;
                }
                break;

            /* stereo to mono */
            case MIX_FLAGS_STEREO_SOURCE:
                for (src = dest = 0; src < numSamples; src+=2, dest++)
                {
                    gainLeft += gainIncLeft;
                    gainRight += gainIncRight;
                    temp = (pInputBuffer[src] * (gainLeft >> 15)) >> NUM_MIXER_GUARD_BITS;
                    temp += ((pInputBuffer[src+1] * (gainRight >> 15)) >> NUM_MIXER_GUARD_BITS);
                    pMixBuffer[dest] += temp;
                }
                break;

            /* stereo to stereo */
            case MIX_FLAGS_STEREO_SOURCE | MIX_FLAGS_STEREO_OUTPUT:
                for (src = dest = 0; src < numSamples; src+=2, dest+=2)
                {
                    gainLeft += gainIncLeft;
                    gainRight += gainIncRight;
                    pMixBuffer[dest] += (pInputBuffer[src] * (gainLeft >> 15)) >> NUM_MIXER_GUARD_BITS;
                    pMixBuffer[dest+1] += (pInputBuffer[src+1] * (gainRight >> 15)) >> NUM_MIXER_GUARD_BITS;
                }
                break;
        }
    }
}
#endif
#endif

