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

#include "javax_microedition_m3g_Light.h"

JNIEXPORT jfloat JNICALL Java_javax_microedition_m3g_Light__1getSpotAngle
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jfloat angle = (jfloat)m3gGetSpotAngle((M3GLight)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return angle;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Light__1setSpotExponent
(JNIEnv* aEnv, jclass, jint aHandle, jfloat aExponent)
{
    M3G_DO_LOCK
    m3gSetSpotExponent((M3GLight)aHandle, aExponent);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Light__1setMode
(JNIEnv* aEnv, jclass, jint aHandle, jint aMode)
{
    M3G_DO_LOCK
    m3gSetLightMode((M3GLight)aHandle, (int)aMode);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Light__1setAttenuation
(JNIEnv* aEnv, jclass, jint aHandle, jfloat aConstant, jfloat aLinear, jfloat aQuadratic)
{
    M3G_DO_LOCK
    m3gSetAttenuation((M3GLight)aHandle, aConstant, aLinear, aQuadratic);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Light__1setIntensity
(JNIEnv* aEnv, jclass, jint aHandle, jfloat aIntensity)
{
    M3G_DO_LOCK
    m3gSetIntensity((M3GLight)aHandle, aIntensity);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Light__1ctor
(JNIEnv* aEnv, jclass, jint aM3g)
{
    M3G_DO_LOCK
    jint handle = (jint)m3gCreateLight((M3GInterface)aM3g);
    M3G_DO_UNLOCK(aEnv)
    return handle;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Light__1getMode
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint mode = (jint)m3gGetLightMode((M3GLight)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return mode;
}

JNIEXPORT jfloat JNICALL Java_javax_microedition_m3g_Light__1getAttenuation
(JNIEnv* aEnv, jclass, jint aHandle, jint aType)
{
    M3G_DO_LOCK
    jfloat att = (jfloat)m3gGetAttenuation((M3GLight)aHandle, aType);
    M3G_DO_UNLOCK(aEnv)
    return att;
}

JNIEXPORT jfloat JNICALL Java_javax_microedition_m3g_Light__1getSpotExponent
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jfloat spotExp = (jfloat)m3gGetSpotExponent((M3GLight)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return spotExp;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Light__1setSpotAngle
(JNIEnv* aEnv, jclass, jint aHandle, jfloat aAngle)
{
    M3G_DO_LOCK
    m3gSetSpotAngle((M3GLight)aHandle, aAngle);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jfloat JNICALL Java_javax_microedition_m3g_Light__1getIntensity
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jfloat intensity = (jfloat)m3gGetIntensity((M3GLight)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return intensity;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Light__1setColor
(JNIEnv* aEnv, jclass, jint aHandle, jint aRGB)
{
    M3G_DO_LOCK
    m3gSetLightColor((M3GLight)aHandle, aRGB);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Light__1getColor
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint color = (jint)m3gGetLightColor((M3GLight)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return color;
}
