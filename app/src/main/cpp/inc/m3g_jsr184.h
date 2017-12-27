/*
* Copyright (c) 2003 Nokia Corporation and/or its subsidiary(-ies).
* All rights reserved.
* This component and the accompanying materials are made available
* under the terms of "Eclipse Public License v1.0"
* which accompanies this distribution, and is available
* at the URL "http://www.eclipse.org/legal/epl-v10.html".
*
* Initial Contributors:
* Nokia Corporation - initial contribution.
*
* Contributors:
*
* Description:
*
*/

#ifndef M3G_JSR184_H
#define M3G_JSR184_H

/*!
 * \file \brief Global enumerations for JSR-184
 *
 */

#if defined(M3G_CORE_INCLUDE)
#   error includes Java dependencies; do not include into the core module.
#endif

#include <m3g_core.h>

#if defined(__cplusplus)
extern "C"
{
#endif

    /*----------------------------------------------------------------------
     * Enumerations
     *--------------------------------------------------------------------*/

    /*----------------------------------------------------------------------
     * JSR-184 API internal functions
     *--------------------------------------------------------------------*/

    static M3Guint jsr184BytesPerPixels(int format);
    static const char *jsr184Exception(M3Genum errorCode);

#if defined(__cplusplus)
} /* extern "C" */
#endif

#endif // M3G_JSR184_H

