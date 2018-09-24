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
#include "javax_microedition_m3g_Node.h"

JNIEXPORT jboolean JNICALL Java_javax_microedition_m3g_Node__1isEnabled
(JNIEnv* aEnv, jclass, jlong aHandle, jint aWhich)
{
    M3G_DO_LOCK
    jboolean enabled = (jboolean)m3gIsEnabled((M3GNode)aHandle, aWhich);
    M3G_DO_UNLOCK(aEnv)
    return enabled;
}

JNIEXPORT jfloat JNICALL Java_javax_microedition_m3g_Node__1getAlphaFactor
(JNIEnv* aEnv, jclass, jlong aHandle)
{
    M3G_DO_LOCK
    jfloat alpha = (jfloat)m3gGetAlphaFactor((M3GNode)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return alpha;
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_Node__1getParent
(JNIEnv* aEnv, jclass, jlong aHandle)
{
    M3G_DO_LOCK
    jlong parent = (jlong)m3gGetParent((M3GNode)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return parent;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Node__1setScope
(JNIEnv* aEnv, jclass, jlong aHandle, jint aId)
{
    M3G_DO_LOCK
    m3gSetScope((M3GNode)aHandle, aId);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Node__1getScope
(JNIEnv* aEnv, jclass, jlong aHandle)
{
    M3G_DO_LOCK
    jint scope = (jint)m3gGetScope((M3GNode)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return scope;
}

JNIEXPORT jboolean JNICALL Java_javax_microedition_m3g_Node__1getTransformTo
(JNIEnv* aEnv, jclass, jlong aHandle, jlong aHTarget, jbyteArray aDstArray)
{
    jboolean ret = 0;
    if (aDstArray != NULL && aHTarget != 0)
    {
        jbyte* dstArray = aEnv->GetByteArrayElements(aDstArray, NULL);
        if (dstArray == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }

        M3G_DO_LOCK
        ret = m3gGetTransformTo((M3GNode)aHandle, (M3GNode)aHTarget, (M3GMatrix *)dstArray);
        M3G_DO_UNLOCK(aEnv)

        aEnv->ReleaseByteArrayElements(aDstArray, dstArray, 0);
    }
    else
    {
        M3G_RAISE_EXCEPTION(aEnv, "java/lang/NullPointerException");
    }

    return ret;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Node__1align
(JNIEnv* aEnv, jclass, jlong aHNode, jlong aHRef)
{
    M3G_BEGIN_PROFILE(M3G_PROFILE_ALIGN);
    M3G_DO_LOCK
    m3gAlignNode((M3GNode)aHNode, (M3GNode)aHRef);
    M3G_DO_UNLOCK(aEnv)
    M3G_END_PROFILE(M3G_PROFILE_ALIGN);
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Node__1setAlphaFactor
(JNIEnv* aEnv, jclass, jlong aHandle, jfloat aAlphaFactor)
{
    M3G_DO_LOCK
    m3gSetAlphaFactor((M3GNode)aHandle, aAlphaFactor);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Node__1enable
(JNIEnv* aEnv, jclass, jlong aHandle, jint aWhich, jboolean aEnabled)
{
    M3G_DO_LOCK
    m3gEnable((M3GNode)aHandle, aWhich, (jint)aEnabled);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Node__1setAlignment
(JNIEnv* aEnv, jclass, jlong aHandle, jlong aHZReference, jint aZTarget, jlong aHYReference, jint aYTarget)
{
    M3G_DO_LOCK
    m3gSetAlignment((M3GNode)aHandle, (M3GNode)aHZReference, aZTarget, (M3GNode)aHYReference, aYTarget);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_Node__1getZRef
(JNIEnv* aEnv, jclass, jlong aHandle)
{
    M3G_DO_LOCK
    jlong zRef = (jlong)m3gGetZRef((M3GNode)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return zRef;
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_Node__1getYRef
(JNIEnv* aEnv, jclass, jlong aHandle)
{
    M3G_DO_LOCK
    jlong yRef = (jlong)m3gGetYRef((M3GNode)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return yRef;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Node__1getSubtreeSize
(JNIEnv* aEnv, jclass, jlong aHandle)
{
    M3G_DO_LOCK
    jint size = (M3Guint)m3gGetSubtreeSize((M3GNode)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return size;
}

/* M3G 1.1 JNI Calls */

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Node__1getAlignmentTarget
(JNIEnv* aEnv, jclass, jlong aHandle, jint aAxis)
{
    M3G_DO_LOCK
    jint target = (jint)m3gGetAlignmentTarget((M3GNode)aHandle, aAxis);
    M3G_DO_UNLOCK(aEnv)
    return target;
}
