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
* Description: AnimationTrack interface
*
*/

#ifndef __M3G_ANIMATIONTRACK_H__
#define __M3G_ANIMATIONTRACK_H__

/*!
 * \internal
 * \file
 * \brief AnimationTrack interface
 */

#include "m3g_gl.h"
#include "m3g_keyframesequence.h"
#include "m3g_animationcontroller.h"
#include "m3g_object.h"

struct M3GAnimationTrackImpl
{
    Object object;
    KeyframeSequence *sequence;
    AnimationController *controller;
    M3Gint property;
};

typedef struct
{
    M3Gfloat weight;
    M3Gint validity;
} SampleInfo;

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static void m3gGetContribution(const AnimationTrack *track, M3Gint time,
                               M3Gfloat *accumSamples, SampleInfo *sampleInfo);

#endif /*__M3G_ANIMATIONTRACK_H__*/

