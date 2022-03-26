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
* Description: AnimationController interface
*
*/

#ifndef __M3G_ANIMATIONCONTROLLER_H__
#define __M3G_ANIMATIONCONTROLLER_H__

/*!
 * \internal
 * \file
 * \brief AnimationController interface
 */

#include "m3g_gl.h"
#include "m3g_object.h"

/*!
 * \internal
 * \brief \c AnimationController object instance
 */
struct M3GAnimationControllerImpl
{
    Object object;

    M3Gint      activationTime;
    M3Gint      deactivationTime;
    M3Gfloat    weight;
    M3Gfloat    speed;
    M3Gint      refWorldTime;
    M3Gfloat    refSequenceTime;
};

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static M3Gbool  m3gIsActive(const AnimationController *controller,
                            M3Gint worldTime);

static M3Gint m3gTimeToActivation(const AnimationController *controller,
                                  M3Gint worldTime);

static M3Gint m3gTimeToDeactivation(const AnimationController *controller,
                                    M3Gint worldTime);

#endif /*__M3G_ANIMATIONCONTROLLER_H__*/

