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
#include "javax_microedition_m3g_AnimationController.h"

JNIEXPORT jfloat JNICALL Java_javax_microedition_m3g_AnimationController__1getPosition
(JNIEnv* aEnv, jclass, jint aHController, jint aWorldTime)
{
    M3G_DO_LOCK
    jfloat position = (jfloat)m3gGetPosition((M3GAnimationController)aHController, aWorldTime);
    M3G_DO_UNLOCK(aEnv)
    return position;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_AnimationController__1setWeight
(JNIEnv* aEnv, jclass, jint aHController, jfloat aWeight)
{
    M3G_DO_LOCK
    m3gSetWeight((M3GAnimationController)aHController, aWeight);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_AnimationController__1setActiveInterval
(JNIEnv* aEnv, jclass, jint aHController, jint aWorldTimeMin, jint aWorldTimeMax)
{
    M3G_DO_LOCK
    m3gSetActiveInterval((M3GAnimationController)aHController, aWorldTimeMin, aWorldTimeMax);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_AnimationController__1getActiveIntervalStart
(JNIEnv* aEnv, jclass, jint aHController)
{
    M3G_DO_LOCK
    jint start = (jint)m3gGetActiveIntervalStart((M3GAnimationController)aHController);
    M3G_DO_UNLOCK(aEnv)
    return start;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_AnimationController__1ctor
(JNIEnv* aEnv, jclass, jint aM3g)
{
    M3G_DO_LOCK
    jint handle = (jint)m3gCreateAnimationController((M3GInterface)aM3g);
    M3G_DO_UNLOCK(aEnv)
    return handle;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_AnimationController__1setPosition
(JNIEnv* aEnv, jclass, jint aHController, jfloat aTime, jint aWorldTime)
{
    M3G_DO_LOCK
    m3gSetPosition((M3GAnimationController)aHController, aTime, aWorldTime);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_AnimationController__1setSpeed
(JNIEnv* aEnv, jclass, jint aHController, jfloat aFactor, jint aWorldTime)
{
    M3G_DO_LOCK
    m3gSetSpeed((M3GAnimationController)aHController, aFactor, aWorldTime);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jfloat JNICALL Java_javax_microedition_m3g_AnimationController__1getSpeed
(JNIEnv* aEnv, jclass, jint aHController)
{
    M3G_DO_LOCK
    jfloat speed = (jfloat)m3gGetSpeed((M3GAnimationController)aHController);
    M3G_DO_UNLOCK(aEnv)
    return speed;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_AnimationController__1getActiveIntervalEnd
(JNIEnv* aEnv, jclass, jint aHController)
{
    M3G_DO_LOCK
    jint end = (jint)m3gGetActiveIntervalEnd((M3GAnimationController)aHController);
    M3G_DO_UNLOCK(aEnv)
    return end;
}

JNIEXPORT jfloat JNICALL Java_javax_microedition_m3g_AnimationController__1getWeight
(JNIEnv* aEnv, jclass, jint aHController)
{
    M3G_DO_LOCK
    jfloat weight = (jfloat)m3gGetWeight((M3GAnimationController)aHController);
    M3G_DO_UNLOCK(aEnv)
    return weight;
}

/* M3G 1.1 JNI Calls*/

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_AnimationController__1getRefWorldTime
(JNIEnv* aEnv, jclass, jint aHController)
{
    M3G_DO_LOCK
    jint time = (jint)m3gGetRefWorldTime((M3GAnimationController)aHController);
    M3G_DO_UNLOCK(aEnv)
    return time;
}
