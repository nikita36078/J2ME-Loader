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
 * \file \brief Symbian Java bindings
 *
 */

#include <m3g_core.h> // Please check if this header is needed

#include <jni.h>

#include <math.h>
#include <stdlib.h>
#include <string.h>

#include <GLES/egl.h>

#define M3G_ASSERT(a)           ((void)(a))
#define M3G_BEGIN_PROFILE(a)    ((void)(a))
#define M3G_END_PROFILE(a)      ((void)(a))

#define Matrix  M3GMatrix
#define Quat    M3GQuat
#define Vec4    M3GVec4

#define M3G_JAVA_INCLUDE
extern "C"
{
#include "m3g_kvm_api.inl"
}
#undef M3G_JAVA_INCLUDE

