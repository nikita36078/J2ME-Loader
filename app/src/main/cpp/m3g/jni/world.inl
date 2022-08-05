/*
* Copyright (c) 2009 Nokia Corporation and/or its subsidiary(-ies).
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

#include "javax_microedition_m3g_World.h"

JNIEXPORT void JNICALL Java_javax_microedition_m3g_World__1setBackground
(JNIEnv* aEnv, jclass, jlong aHandle, jlong aHBackground)
{
    M3G_DO_LOCK
    m3gSetBackground((M3GWorld)aHandle, (M3GBackground)aHBackground);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_World__1ctor
(JNIEnv* aEnv, jclass, jlong aM3g)
{
    M3G_DO_LOCK
    jlong handle = (jlong)m3gCreateWorld((M3GInterface)aM3g);
    M3G_DO_UNLOCK(aEnv)
    return handle;
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_World__1getBackground
(JNIEnv* aEnv, jclass, jlong aHandle)
{
    M3G_DO_LOCK
    jlong bg = (jlong)m3gGetBackground((M3GWorld)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return bg;
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_World__1getActiveCamera
(JNIEnv* aEnv, jclass, jlong aHandle)
{
    M3G_DO_LOCK
    jlong camera = (jlong)m3gGetActiveCamera((M3GWorld)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return camera;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_World__1setActiveCamera
(JNIEnv* aEnv, jclass, jlong aHandle, jlong aHCamera)
{
    M3G_DO_LOCK
    m3gSetActiveCamera((M3GWorld)aHandle, (M3GCamera)aHCamera);
    M3G_DO_UNLOCK(aEnv)
}
