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
* Description: Camera interface
*
*/

#ifndef __M3G_CAMERA_H__
#define __M3G_CAMERA_H__

/*!
 * \internal
 * \file 
 * \brief Camera interface
 */

#include "m3g_gl.h"
#include "m3g_node.h"

/*!
 * \internal
 * \brief Camera node instance data
 */
struct M3GCameraImpl
{
	Node node;
    
    M3Gint	 projType;   
    M3Gfloat heightFov;
    M3Gfloat aspect;
    M3Gfloat clipNear;
    M3Gfloat clipFar;

    Matrix	projMatrix;
    Vec4 frustumPlanes[6];
    
    M3Gbool frustumPlanesValid;
    M3Gbool zeroViewVolume;
};

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static void m3gInitCamera(Interface *m3g, Camera *camera);
static void m3gApplyProjection(const Camera *camera);

static const Vec4 *m3gFrustumPlanes(const Camera *camera);
static const Matrix *m3gProjectionMatrix(const Camera *camera);

M3G_INLINE static M3Gbool m3gValidProjection(const Camera *camera)
{
    if (camera->zeroViewVolume) {
        M3G_LOG1(M3G_LOG_WARNINGS,
                 "Warning: Invalid projection for camera 0x%08X\n",
                 (unsigned) camera);
        return M3G_FALSE;
    }
    return M3G_TRUE;
}


#endif /*__M3G_CAMERA_H__*/
