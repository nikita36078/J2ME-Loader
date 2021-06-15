/*----------------------------------------------------------------------------
 *
 * File:
 * eas_pan.h
 *
 * Contents and purpose:
 * Calculates left and right gain multipliers based on a pan value from -63 to +63
 *
 * NOTES:
 * The _CMX_PARSER and _MFI_PARSER preprocessor symbols determine
 * whether the parser works for those particular file formats.
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
 *   $Revision: 82 $
 *   $Date: 2006-07-10 11:45:19 -0700 (Mon, 10 Jul 2006) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_PAN_H
#define _EAS_PAN_H

#include "eas_types.h"

/*----------------------------------------------------------------------------
 * EAS_CalcPanControl()
 *----------------------------------------------------------------------------
 * Purpose:
 * Assign the left and right gain values corresponding to the given pan value.
 *
 * This routine uses sin/cos approximations for an equal power curve:
 *
 * sin(x) = (2-4*c)*x^2 + c + x
 * cos(x) = (2-4*c)*x^2 + c - x
 *
 * where  c = 1/sqrt(2)
 * using the a0 + x*(a1 + x*a2) approach
 *
 * Inputs:
 * pan          - pan value (-63 to + 63)
 *
 * Outputs:
 * pGainLeft    linear gain multiplier for left channel (15-bit fraction)
 * pGainRight   linear gain multiplier for left channel (15-bit fraction)
 *
 * Side Effects:
 *----------------------------------------------------------------------------
*/
void EAS_CalcPanControl (EAS_INT pan, EAS_I16 *pGainLeft, EAS_I16 *pGainRight);

#endif /* #ifndef _EAS_PAN_H */

