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
* Description: Symbian offscreen GL context implementation
*
*/


/*!
 * \file
 * \brief Symbian offscreen GL context implementation
 */

#include "m3g_gl.h"
#include "m3g_defs.h"

#include <w32std.h>
#include <fbs.h>

//----------------------------------------------------------------------
// Private functions
//----------------------------------------------------------------------

static M3GPixelFormat m3gSymbianPixelFormat(TDisplayMode displayMode)
{
    switch (displayMode) {
    case EColor4K:
        return M3G_RGB4;
    case EColor64K:
        return M3G_RGB565;
    case EColor16M:
        return M3G_RGB8;
    case EColor16MU:
        return M3G_BGR8_32;
    case EColor16MA:    
    case EColor16MAP:    
        return M3G_BGRA8;
    case ERgb:
        return M3G_RGB8_32;
    default:
        return M3G_NO_FORMAT;
    }
}

//----------------------------------------------------------------------
// Internal functions
//----------------------------------------------------------------------

/*!
 * \brief
 */
M3Gbool m3gglLockNativeBitmap(M3GNativeBitmap bitmap,
                              M3Gubyte **ptr,
                              M3Gsizei *stride) 
{
    CFbsBitmap *fbm = (CFbsBitmap*) bitmap;
    fbm->LockHeap();
    *ptr = (M3Gubyte *) fbm->DataAddress();
    *stride = (M3Gsizei)
        CFbsBitmap::ScanLineLength(fbm->SizeInPixels().iWidth,
                                   fbm->DisplayMode());
    return M3G_TRUE;
}

/*!
 * \brief
 */
void m3gglReleaseNativeBitmap(M3GNativeBitmap bitmap) 
{    
    ((CFbsBitmap*)bitmap)->UnlockHeap();
}

/*!
 * \brief Queries properties of a native window
 */
extern "C" M3Gbool m3gglGetNativeBitmapParams(M3GNativeBitmap bitmap,
                                              M3GPixelFormat *format,
                                              M3Gint *width, M3Gint *height, M3Gint *pixels)
{
    CFbsBitmap *pBitmap = (CFbsBitmap *) bitmap;

    *format = m3gSymbianPixelFormat(pBitmap->DisplayMode());
    
    TSize size = pBitmap->SizeInPixels();
    *width = size.iWidth;
    *height = size.iHeight;
    
    pBitmap->LockHeap();
    *pixels = (M3Gint) pBitmap->DataAddress();
    pBitmap->UnlockHeap();
    
    return M3G_TRUE;
}

/*!
 * \brief Queries properties of a native window
 */
extern "C" M3Gbool m3gglGetNativeWindowParams(M3GNativeWindow wnd,
                                              M3GPixelFormat *format,
                                              M3Gint *width, M3Gint *height)
{
    RWindow *pWnd = (RWindow *) wnd;

    *format = m3gSymbianPixelFormat(pWnd->DisplayMode());
    
    TSize size = pWnd->Size();
    *width = size.iWidth;
    *height = size.iHeight;
    
    return M3G_TRUE;
}

