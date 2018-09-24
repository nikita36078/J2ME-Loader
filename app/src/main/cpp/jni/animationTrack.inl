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
(JNIEnv* aEnv, jclass, jlong aHandle)
{
    M3G_DO_LOCK
    jint props = (jint)m3gGetTargetProperty((M3GAnimationTrack)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return props;
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_AnimationTrack__1getSequence
(JNIEnv* aEnv, jclass, jlong aHandle)
{
    M3G_DO_LOCK
    jlong sequence = (jlong)m3gGetSequence((M3GAnimationTrack)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return sequence;
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_AnimationTrack__1ctor
(JNIEnv* aEnv, jclass, jlong aM3g, jlong aHSequence, jint aProperty)
{
    M3G_DO_LOCK
    jlong handle = (jlong)m3gCreateAnimationTrack((M3GInterface)aM3g, (M3GKeyframeSequence)aHSequence, (M3Gint)aProperty);
    M3G_DO_UNLOCK(aEnv)
    return handle;
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_AnimationTrack__1getController
(JNIEnv* aEnv, jclass, jlong aHandle)
{
    M3G_DO_LOCK
    jlong controller = (jlong)m3gGetController((M3GAnimationTrack)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return controller;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_AnimationTrack__1setController
(JNIEnv* aEnv, jclass, jlong aHandle, jlong aHController)
{
    M3G_DO_LOCK
    m3gSetController((M3GAnimationTrack)aHandle, (M3GAnimationController)aHController);
    M3G_DO_UNLOCK(aEnv)
}
