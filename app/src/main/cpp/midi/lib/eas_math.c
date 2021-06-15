/*----------------------------------------------------------------------------
 *
 * File:
 * eas_math.c
 *
 * Contents and purpose:
 * Contains common math routines for the various audio engines.
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
 *   $Revision: 586 $
 *   $Date: 2007-03-08 20:33:04 -0800 (Thu, 08 Mar 2007) $
 *----------------------------------------------------------------------------
*/

#include "eas.h"
#include "eas_math.h"

/* anything less than this converts to a fraction too small to represent in 32-bits */
#define MIN_CENTS   -18000

/*----------------------------------------------------------------------------
 * EAS_Calculate2toX()
 *----------------------------------------------------------------------------
 * Purpose:
 * Calculate 2^x
 *
 * Inputs:
 * nCents -     measured in cents
 * psEASData - pointer to overall EAS data structure
 *
 * Outputs:
 * nResult - int.frac result (where frac has NUM_DENTS_FRAC_BITS)
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_I32 EAS_Calculate2toX (EAS_I32 nCents)
{
    EAS_I32 nDents;
    EAS_I32 nExponentInt, nExponentFrac;
    EAS_I32 nTemp1, nTemp2;
    EAS_I32 nResult;

    /* check for minimum value */
    if (nCents < MIN_CENTS)
        return 0;

    /* for the time being, convert cents to dents */
    nDents = FMUL_15x15(nCents, CENTS_TO_DENTS);

    nExponentInt = GET_DENTS_INT_PART(nDents);
    nExponentFrac = GET_DENTS_FRAC_PART(nDents);

    /*
    implement 2^(fracPart) as a power series
    */
    nTemp1 = GN2_TO_X2 + MULT_DENTS_COEF(nExponentFrac, GN2_TO_X3);
    nTemp2 = GN2_TO_X1 + MULT_DENTS_COEF(nExponentFrac, nTemp1);
    nTemp1 = GN2_TO_X0 + MULT_DENTS_COEF(nExponentFrac, nTemp2);

    /*
    implement 2^(intPart) as
    a left shift for intPart >= 0 or
    a left shift for intPart <  0
    */
    if (nExponentInt >= 0)
    {
        /* left shift for positive exponents */
        /*lint -e{703} <avoid multiply for performance>*/
        nResult = nTemp1 << nExponentInt;
    }
    else
    {
        /* right shift for negative exponents */
        nExponentInt = -nExponentInt;
        nResult = nTemp1 >> nExponentInt;
    }

    return nResult;
}

/*----------------------------------------------------------------------------
 * EAS_LogToLinear16()
 *----------------------------------------------------------------------------
 * Purpose:
 * Transform log value to linear gain multiplier using piece-wise linear
 * approximation
 *
 * Inputs:
 * nGain - log scale value in 20.10 format. Even though gain is normally
 * stored in 6.10 (16-bit) format we use 32-bit numbers here to eliminate
 * the need for saturation checking when combining gain values.
 *
 * Outputs:
 * Returns a 16-bit linear value approximately equal to 2^(nGain/1024)
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
EAS_U16 EAS_LogToLinear16 (EAS_I32 nGain)
{
    EAS_INT nExp;
    EAS_U16 nTemp;

    /* bias to positive */
    nGain += 32767;

    /* check for infinite attenuation */
    if (nGain < 0)
        return 0;

    /* extract the exponent */
    nExp = 31 - (nGain >> 10);

    /* check for maximum output */
    if (nExp < 0)
        return 0x7fff;

    /* extract mantissa and restore implied 1 bit */
    nTemp = (EAS_U16)((((nGain & 0x3ff) << 4) | 0x4000) >> nExp);

    /* use shift to approximate power-of-2 operation */
    return nTemp;
}

/*----------------------------------------------------------------------------
 * EAS_VolumeToGain()
 *----------------------------------------------------------------------------
 * Purpose:
 * Transform volume control in 1dB increments to gain multiplier
 *
 * Inputs:
 * volume - 100 = 0dB, 99 = -1dB, 0 = -inf
 *
 * Outputs:
 * Returns a 16-bit linear value
 *----------------------------------------------------------------------------
*/
EAS_I16 EAS_VolumeToGain (EAS_INT volume)
{
    /* check for limits */
    if (volume <= 0)
        return 0;
    if (volume >= 100)
        return 0x7fff;

    /*lint -e{702} use shift instead of division */
    return (EAS_I16) EAS_Calculate2toX((((volume - EAS_MAX_VOLUME) * 204099) >> 10) - 1);
}

