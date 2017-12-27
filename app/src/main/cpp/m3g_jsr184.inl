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


/*!
 * \file \brief Global methods for JSR-184
 *
 * This file is <em>included</em>, not linked, by specific VM bindings.
 *
 */

#ifndef M3G_JAVA_INCLUDE
#   error included by m3g_<platform>_java_api.c; do not compile separately.
#endif

#include <m3g_core.h>
#include "m3g_jsr184.h"

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Returns the number of bytes per pixel for a JSR-184 pixel
 * format
 *
 * \note Dependent on constants in Image2D.java
 */
static M3Guint jsr184BytesPerPixel(int jsrFormat)
{
    switch (jsrFormat)
    {
    case M3G_ALPHA:
        return 1;
    case M3G_LUMINANCE:
        return 1;
    case M3G_LUMINANCE_ALPHA:
        return 2;
    case M3G_RGB:
        return 3;
    case M3G_RGBA:
        return 4;
    default:
        return 0;
    }
}

/*!
 * \brief Returns m3g core pixel format for qiven Qt based pixel format
 *
 */
#if 0
static M3GPixelFormat mapQtPixelformat(int qtFormat)
{
    switch (qtFormat)
    {
    case QImage::Format_RGB32:
    case QImage::Format_ARGB32:
        return M3G_ARGB8;
    case QImage::Format_RGB888:
        return M3G_RGB8;
    case QImage::Format_RGB444:
        return M3G_RGB4;
    case QImage::Format_RGB16:
        return M3G_RGB565;
    default:
        return M3G_NO_FORMAT;
    }
}
#endif


/*!
 * \brief Return a MIDP exception string corresponding to an M3G error
 */
static const char *jsr184Exception(M3Genum errorCode)
{
    switch (errorCode)
    {
    case M3G_NO_ERROR:
        return NULL;
    case M3G_OUT_OF_MEMORY:
        return "java/lang/OutOfMemoryError";
    case M3G_INVALID_OPERATION:
        return "java/lang/IllegalStateException";
    case M3G_INVALID_INDEX:
        return "java/lang/IndexOutOfBoundsException";
    case M3G_NULL_POINTER:
        return "java/lang/NullPointerException";
    case M3G_ARITHMETIC_ERROR:
        return "java/lang/ArithmeticException";
    case M3G_IO_ERROR:
        return "java/io/IOException";
    default:
        return "java/lang/IllegalArgumentException";
    }
}
