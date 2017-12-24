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
#include "javax_microedition_m3g_KeyframeSequence.h"

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_KeyframeSequence__1getRepeatMode
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint mode = (jint)m3gGetRepeatMode((M3GKeyframeSequence)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return mode;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_KeyframeSequence__1setKeyframe
(JNIEnv* aEnv, jclass, jint aHandle, jint aIndex, jint aTime, jfloatArray aValue)
{
    jfloat* elems = NULL;
    if (aValue)
    {
        elems = aEnv->GetFloatArrayElements(aValue, NULL);
        if (elems == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }
    }

    jsize length = aValue ? aEnv->GetArrayLength(aValue) : 0;

    M3G_DO_LOCK
    m3gSetKeyframe((M3GKeyframeSequence)aHandle,
                   aIndex,
                   aTime,
                   length,
                   (const M3Gfloat *)elems);
    M3G_DO_UNLOCK(aEnv)

    if (elems)
        aEnv->ReleaseFloatArrayElements(aValue, elems, 0);
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_KeyframeSequence__1setRepeatMode
(JNIEnv* aEnv, jclass, jint aHandle, jint aMode)
{
    M3G_DO_LOCK
    m3gSetRepeatMode((M3GKeyframeSequence)aHandle, (M3Genum)aMode);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_KeyframeSequence__1setDuration
(JNIEnv* aEnv, jclass, jint aHandle, jint aDuration)
{
    M3G_DO_LOCK
    m3gSetDuration((M3GKeyframeSequence)aHandle, (int)aDuration);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_KeyframeSequence__1ctor
(JNIEnv* aEnv, jclass, jint aM3g, jint aNumKeyframes, jint aNumComponents, jint aInterpolation)
{
    M3G_DO_LOCK
    jint handle = (jint)m3gCreateKeyframeSequence((M3GInterface)aM3g, aNumKeyframes,
                  aNumComponents, aInterpolation);
    M3G_DO_UNLOCK(aEnv)
    return handle;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_KeyframeSequence__1setValidRange
(JNIEnv* aEnv, jclass, jint aHandle, jint aFirst, jint aLast)
{
    M3G_DO_LOCK
    m3gSetValidRange((M3GKeyframeSequence)aHandle, aFirst, aLast);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_KeyframeSequence__1getDuration
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint duration = (jint)m3gGetDuration((M3GKeyframeSequence)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return duration;
}

/* M3G 1.1 JNI Calls */

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_KeyframeSequence__1getComponentCount
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint count = (jint)m3gGetComponentCount((M3GKeyframeSequence)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return count;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_KeyframeSequence__1getInterpolationType
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint type = (jint)m3gGetInterpolationType((M3GKeyframeSequence)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return type;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_KeyframeSequence__1getKeyframe
(JNIEnv* aEnv, jclass, jint aHandle, jint aIndex, jfloatArray aValue)
{
    jfloat* elems = NULL;
    if (aValue)
    {
        elems = aEnv->GetFloatArrayElements(aValue, NULL);
        if (elems == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }

    jsize length = aValue ? aEnv->GetArrayLength(aValue) : 0;

    if ((length < m3gGetComponentCount((M3GKeyframeSequence)aHandle)) &&
            (aValue != NULL))
    {
        if (elems)
        {
            aEnv->ReleaseFloatArrayElements(aValue, elems, JNI_ABORT);
        }
        M3G_RAISE_EXCEPTION(aEnv, "java/lang/IllegalArgumentException");
        return 0;
    }

    M3G_DO_LOCK
    jint keyFrame = m3gGetKeyframe((M3GKeyframeSequence)aHandle, aIndex, elems);
    M3G_DO_UNLOCK(aEnv)

    if (elems)
    {
        /* copy array to java side and release arrays */
        aEnv->ReleaseFloatArrayElements(aValue, elems, 0);
    }
    return keyFrame;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_KeyframeSequence__1getKeyframeCount
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint count = (jint)m3gGetKeyframeCount((M3GKeyframeSequence)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return count;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_KeyframeSequence__1getValidRangeFirst
(JNIEnv* aEnv, jclass, jint aHandle)
{
    int first = 0;
    int last = 0;
    M3G_DO_LOCK
    m3gGetValidRange((M3GKeyframeSequence)aHandle, &first, &last);
    M3G_DO_UNLOCK(aEnv)
    return first;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_KeyframeSequence__1getValidRangeLast
(JNIEnv* aEnv, jclass, jint aHandle)
{
    int first = 0;
    int last = 0;
    M3G_DO_LOCK
    m3gGetValidRange((M3GKeyframeSequence)aHandle, &first, &last);
    M3G_DO_UNLOCK(aEnv)
    return last;
}
