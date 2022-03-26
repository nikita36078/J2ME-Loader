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
* Description: KeyframeSequence interface
*
*/

#ifndef __M3G_KEYFRAMESEQUENCE_H__
#define __M3G_KEYFRAMESEQUENCE_H__

/*!
 * \internal
 * \file
 * \brief KeyframeSequence interface
 */

#include "m3g_object.h"

/*!
 * \internal
 * \brief KeyframeSequence class definition
 */

struct M3GKeyframeSequenceImpl
{
    Object object;

    M3Gfloat *keyframes;
    M3Gint   *keyframeTimes;
    M3Gint   numKeyframes;
    M3Gint   numComponents;
    M3Gint   duration;
    M3Gint   interpolation;
    M3Gbool  closed;
    M3Gint   firstValid;
    M3Gint   lastValid;
    M3Gint   probablyNext;
    M3Gbool  dirty;
    M3Gfloat *inTangents;
    M3Gfloat *outTangents;
    M3GQuat  *a;
    M3GQuat  *b;
};

/*----------------------------------------------------------------------
 * Internal functions
 *--------------------------------------------------------------------*/

static M3Gint m3gGetNumComponents(const KeyframeSequence *sequence);
static M3Gint m3gGetSample(KeyframeSequence *sequence,
                           M3Gint time,
                           M3Gfloat *sample);

#endif /*__M3G_KEYFRAMESEQUENCE_H__*/
