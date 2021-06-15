/*----------------------------------------------------------------------------
 *
 * File:
 * eas_flog2.c
 *
 * Contents and purpose:
 * Fixed point square root
 *
 *
 * Copyright (c) 2006 Sonic Network Inc.

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
 *   $Revision$
 *   $Date$
 *----------------------------------------------------------------------------
*/

#include "eas_types.h"
#include "eas_math.h"

#define MANTISSA_SHIFT          27
#define MANTISSA_MASK           0x0000000f
#define MANTISSA_LSB_SHIFT      7
#define MANTISSA_LSB_MASK       0x000fffff
#define LOG_EXPONENT_SHIFT      10
#define INTERPOLATION_SHIFT     20
#define MAX_NEGATIVE            (-2147483647-1)

/* log lookup table */
static const EAS_U16 eas_log2_table[] =
{
    0, 90, 174, 254, 330, 402, 470, 536,
    599, 659, 717, 773, 827, 879, 929, 977,
    1024
};

/*----------------------------------------------------------------------------
 * EAS_flog2()
 *----------------------------------------------------------------------------
 * Purpose:
 * Calculates the log2 of a 32-bit fixed point value
 *
 * Inputs:
 * n = value of interest
 *
 * Outputs:
 * returns the log2 of n
 *
 *----------------------------------------------------------------------------
*/
EAS_I32 EAS_flog2 (EAS_U32 n)
{
    EAS_U32 exp;
    EAS_U32 interp;

    /* check for error condition */
    if (n == 0)
        return MAX_NEGATIVE;

    /* find exponent */
    for (exp = 31; exp > 0; exp--)
    {
        /* shift until we get a 1 bit in bit 31 */
        if ((n & (EAS_U32) MAX_NEGATIVE) != 0)
            break;
        n <<= 1;
    }
    /*lint -e{701} use shift for performance */
    exp <<= LOG_EXPONENT_SHIFT;

    /* get the least significant bits for interpolation */
    interp = (n >> MANTISSA_LSB_SHIFT) & MANTISSA_LSB_MASK;

    /* get the most significant bits for mantissa lookup */
    n = (n >> MANTISSA_SHIFT) & MANTISSA_MASK;

    /* interpolate mantissa */
    interp = ((eas_log2_table[n+1] - eas_log2_table[n]) * interp) >> INTERPOLATION_SHIFT;
    exp += eas_log2_table[n] + interp;

    return (EAS_I32) exp;
}

