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

#include "javax_microedition_m3g_Fog.h"

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Fog__1setMode
(JNIEnv* aEnv, jclass, jint aHandle, jint aMode)
{
    M3G_DO_LOCK
    m3gSetFogMode((M3GFog)aHandle, aMode);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Fog__1setLinear
(JNIEnv* aEnv, jclass, jint aHandle, jfloat aNear, jfloat aFar)
{
    M3G_DO_LOCK
    m3gSetFogLinear((M3GFog)aHandle, aNear, aFar);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jfloat JNICALL Java_javax_microedition_m3g_Fog__1getDistance
(JNIEnv* aEnv, jclass, jint aHandle, jint aWhich)
{
    M3G_DO_LOCK
    jfloat distance = (jfloat)m3gGetFogDistance((M3GFog)aHandle, aWhich);
    M3G_DO_UNLOCK(aEnv)
    return distance;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Fog__1ctor
(JNIEnv* aEnv, jclass, jint aM3g)
{
    M3G_DO_LOCK
    jint handle = (jint)m3gCreateFog((M3GInterface)aM3g);
    M3G_DO_UNLOCK(aEnv)
    return handle;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Fog__1getMode
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint mode = (jint)m3gGetFogMode((M3GFog)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return mode;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Fog__1setDensity
(JNIEnv* aEnv, jclass, jint aHandle, jfloat aDensity)
{
    M3G_DO_LOCK
    m3gSetFogDensity((M3GFog)aHandle, aDensity);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jfloat JNICALL Java_javax_microedition_m3g_Fog__1getDensity
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jfloat density = (jfloat)m3gGetFogDensity((M3GFog)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return density;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Fog__1setColor
(JNIEnv* aEnv, jclass, jint aHandle, jint aRGB)
{
    M3G_DO_LOCK
    m3gSetFogColor((M3GFog)aHandle, aRGB);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Fog__1getColor
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint color = (jint)m3gGetFogColor((M3GFog)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return color;
}
