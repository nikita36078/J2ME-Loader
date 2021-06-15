/*----------------------------------------------------------------------------
 *
 * File:
 * eas_ctype.h
 *
 * Contents and purpose:
 * This is a replacement for the CRT ctype.h functions. These
 * functions are currently ASCII only, but eventually, we will want
 * to support wide-characters for localization.
 *
 * Copyright (c) 2005 Sonic Network Inc.

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
 *   $Revision: 429 $
 *   $Date: 2006-10-19 23:50:15 -0700 (Thu, 19 Oct 2006) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_CTYPE_H
#define _EAS_CTYPE_H

EAS_INLINE EAS_I8 IsDigit (EAS_I8 c) { return ((c >= '0') && (c <= '9')); }
EAS_INLINE EAS_I8 IsSpace (EAS_I8 c) { return (((c >= 9) && (c <= 13)) || (c == ' ')); }
EAS_INLINE EAS_I8 ToUpper (EAS_I8 c) { if ((c >= 'a') && (c <= 'z')) return c & ~0x20; else return c; }
EAS_INLINE EAS_I8 ToLower (EAS_I8 c) { if ((c >= 'A') && (c <= 'Z')) return c |= 0x20; else return c; }

#endif

