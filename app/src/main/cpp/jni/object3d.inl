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

#include "javax_microedition_m3g_Object3D.h"

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Object3D__1animate
(JNIEnv* aEnv, jclass, jint aHObject, jint aTime)
{
    M3G_DO_LOCK
    jint anim = (jint)m3gAnimate((M3GObject)aHObject, aTime);
    M3G_DO_UNLOCK(aEnv)
    return anim;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Object3D__1getAnimationTrack
(JNIEnv* aEnv, jclass, jint aHObject, jint aIndex)
{
    M3G_DO_LOCK
    jint handle = (M3Guint)m3gGetAnimationTrack((M3GObject)aHObject, aIndex);
    M3G_DO_UNLOCK(aEnv)
    return handle;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Object3D__1find
(JNIEnv* aEnv, jclass, jint aHObject, jint aUserID)
{
    M3G_DO_LOCK
    jint target = (M3Guint)m3gFind((M3GObject)aHObject, aUserID);
    M3G_DO_UNLOCK(aEnv)
    return target;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Object3D__1getUserID
(JNIEnv* aEnv, jclass, jint aHObject)
{
    M3G_DO_LOCK
    jint id = (jint)m3gGetUserID((M3GObject)aHObject);
    M3G_DO_UNLOCK(aEnv)
    return id;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Object3D__1addAnimationTrack
(JNIEnv* aEnv, jclass, jint aHObject, jint aHTrack)
{
    M3G_DO_LOCK
    jint ret = (jint)m3gAddAnimationTrack((M3GObject)aHObject, (M3GAnimationTrack)aHTrack);
    M3G_DO_UNLOCK(aEnv)
    return ret;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Object3D__1getAnimationTrackCount
(JNIEnv* aEnv, jclass, jint aHObject)
{
    M3G_DO_LOCK
    jint count = (jint)m3gGetAnimationTrackCount((M3GObject)aHObject);
    M3G_DO_UNLOCK(aEnv)
    return count;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Object3D__1removeAnimationTrack
(JNIEnv* aEnv, jclass, jint aHObject, jint aHTrack)
{
    M3G_DO_LOCK
    m3gRemoveAnimationTrack((M3GObject)aHObject, (M3GAnimationTrack)aHTrack);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Object3D__1setUserID
(JNIEnv* aEnv, jclass, jint aHObject, jint aUserID)
{
    M3G_DO_LOCK
    m3gSetUserID((M3GObject)aHObject, aUserID);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Object3D__1addRef
(JNIEnv* aEnv, jclass, jint aObject)
{
    M3G_DO_LOCK
    m3gAddRef((M3GObject) aObject);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Object3D__1duplicate
(JNIEnv* aEnv, jclass, jint aHObject, jintArray aHReferences)
{
    jint* references = NULL;
    if (aHReferences)
    {
        references = aEnv->GetIntArrayElements(aHReferences, NULL);
        if (references == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }

    M3G_DO_LOCK
    M3Guint ret = (M3Guint)m3gDuplicate((M3GObject)aHObject, (M3GObject *)references);
    M3G_DO_UNLOCK(aEnv)

    if (references)
    {
        aEnv->ReleaseIntArrayElements(aHReferences, references, 0);
    }

    return ret;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Object3D__1getReferences
(JNIEnv* aEnv, jclass, jint aHObject, jintArray aHReferences)
{
    jint* references = NULL;
    if (aHReferences)
    {
        references = aEnv->GetIntArrayElements(aHReferences, NULL);
        if (references == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }
    jint numReferences = aHReferences ? aEnv->GetArrayLength(aHReferences) : 0;

    M3G_DO_LOCK
    jint ret = m3gGetReferences((M3GObject)aHObject, (M3GObject *)references, numReferences);
    M3G_DO_UNLOCK(aEnv)

    if (references)
    {
        aEnv->ReleaseIntArrayElements(aHReferences, references, 0);
    }

    return ret;
}
