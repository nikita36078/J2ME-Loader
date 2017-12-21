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
* Description: World interface
*
*/

#ifndef __M3G_WORLD_H__
#define __M3G_WORLD_H__

/*!
 * \internal
 * \file
 * \brief World interface
 */

#include "m3g_gl.h"
#include "m3g_group.h"
#include "m3g_camera.h"
#include "m3g_background.h"

struct M3GWorldImpl
{
	Group group;
	Background *background;
	Camera *activeCamera;
};

#endif /*__M3G_WORLD_H__*/
