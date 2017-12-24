/*
* Copyright (c) 2003 Nokia Corporation and/or its subsidiary(-ies).
* All rights reserved.
* This component and the accompanying materials are made available
* under the terms of the License "Eclipse Public License v1.0"
* which accompanies this distribution, and is available
* at the URL "http://www.eclipse.org/legal/epl-v10.html".
*
* Initial Contributors:
* Nokia Corporation - initial contribution.
*
* Contributors:
*
* Description: Native Loader support functions for Symbian
*
*/


/*!
 * \file
 * \brief Native Loader support functions for Symbian
 */

M3Gsizei m3gSymbianInflateBlock(M3Gsizei srcLength, const M3Gubyte *src,
                                M3Gsizei dstLength, M3Gubyte *dst);
    
/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static M3G_INLINE M3Gsizei m3gInflateBlock(M3Gsizei srcLength,
                                           const M3Gubyte *src,
                                           M3Gsizei dstLength,
                                           M3Gubyte *dst)
{
    return m3gSymbianInflateBlock(srcLength, src, dstLength, dst);
}

