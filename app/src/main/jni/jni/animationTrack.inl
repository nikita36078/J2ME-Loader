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

#include "javax_microedition_m3g_AnimationTrack.h"

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_AnimationTrack__1getTargetProperty
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint props = (jint)m3gGetTargetProperty((M3GAnimationTrack)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return props;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_AnimationTrack__1getSequence
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint sequence = (jint)m3gGetSequence((M3GAnimationTrack)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return sequence;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_AnimationTrack__1ctor
(JNIEnv* aEnv, jclass, jint aM3g, jint aHSequence, jint aProperty)
{
    M3G_DO_LOCK
    jint handle = (jint)m3gCreateAnimationTrack((M3GInterface)aM3g, (M3GKeyframeSequence)aHSequence, (M3Gint)aProperty);
    M3G_DO_UNLOCK(aEnv)
    return handle;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_AnimationTrack__1getController
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint controller = (jint)m3gGetController((M3GAnimationTrack)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return controller;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_AnimationTrack__1setController
(JNIEnv* aEnv, jclass, jint aHandle, jint aHController)
{
    M3G_DO_LOCK
    m3gSetController((M3GAnimationTrack)aHandle, (M3GAnimationController)aHController);
    M3G_DO_UNLOCK(aEnv)
}
