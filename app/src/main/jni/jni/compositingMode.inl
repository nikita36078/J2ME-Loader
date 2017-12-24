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

#include "javax_microedition_m3g_CompositingMode.h"

JNIEXPORT jboolean JNICALL Java_javax_microedition_m3g_CompositingMode__1isDepthTestEnabled
(JNIEnv* aEnv, jclass, jint aHCompositingMode)
{
    M3G_DO_LOCK
    jboolean enabled = (jboolean)m3gIsDepthTestEnabled((M3GCompositingMode)aHCompositingMode);
    M3G_DO_UNLOCK(aEnv)
    return enabled;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_CompositingMode__1getBlending
(JNIEnv* aEnv, jclass, jint aHCompositingMode)
{
    M3G_DO_LOCK
    jint blending = (jint)m3gGetBlending((M3GCompositingMode)aHCompositingMode);
    M3G_DO_UNLOCK(aEnv)
    return blending;
}

JNIEXPORT jboolean JNICALL Java_javax_microedition_m3g_CompositingMode__1isAlphaWriteEnabled
(JNIEnv* aEnv, jclass, jint aHCompositingMode)
{
    M3G_DO_LOCK
    jboolean enabled = (jboolean)m3gIsAlphaWriteEnabled((M3GCompositingMode)aHCompositingMode);
    M3G_DO_UNLOCK(aEnv)
    return enabled;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_CompositingMode__1enableDepthWrite
(JNIEnv* aEnv, jclass, jint aHCompositingMode, jboolean aEnable)
{
    M3G_DO_LOCK
    m3gEnableDepthWrite((M3GCompositingMode)aHCompositingMode, (M3Gbool)aEnable);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_CompositingMode__1setAlphaThreshold
(JNIEnv* aEnv, jclass, jint aHCompositingMode, jfloat aThreshold)
{
    M3G_DO_LOCK
    m3gSetAlphaThreshold((M3GCompositingMode)aHCompositingMode, (M3Gfloat)aThreshold);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_CompositingMode__1enableColorWrite
(JNIEnv* aEnv, jclass, jint aHCompositingMode, jboolean aEnable)
{
    M3G_DO_LOCK
    m3gEnableColorWrite((M3GCompositingMode)aHCompositingMode, (M3Gbool)aEnable);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_CompositingMode__1setDepthOffset
(JNIEnv* aEnv, jclass, jint aHCompositingMode, jfloat aFactor, jfloat aUnits)
{
    M3G_DO_LOCK
    m3gSetDepthOffset((M3GCompositingMode)aHCompositingMode, (M3Gfloat)aFactor, (M3Gfloat)aUnits);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_CompositingMode__1enableDepthTest
(JNIEnv* aEnv, jclass, jint aHCompositingMode, jboolean aEnable)
{
    M3G_DO_LOCK
    m3gEnableDepthTest((M3GCompositingMode)aHCompositingMode, (M3Gbool)aEnable);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_CompositingMode__1ctor
(JNIEnv* aEnv, jclass, jint aM3g)
{
    M3G_DO_LOCK
    jint handle = (jint)m3gCreateCompositingMode((M3GInterface)aM3g);
    M3G_DO_UNLOCK(aEnv)
    return handle;
}

JNIEXPORT jboolean JNICALL Java_javax_microedition_m3g_CompositingMode__1isColorWriteEnabled
(JNIEnv* aEnv, jclass, jint aHCompositingMode)
{
    M3G_DO_LOCK
    jboolean enabled = (jboolean)m3gIsColorWriteEnabled((M3GCompositingMode)aHCompositingMode);
    M3G_DO_UNLOCK(aEnv)
    return enabled;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_CompositingMode__1setAlphaWriteEnable
(JNIEnv* aEnv, jclass, jint aHCompositingMode, jboolean aEnable)
{
    M3G_DO_LOCK
    m3gSetAlphaWriteEnable((M3GCompositingMode)aHCompositingMode, (M3Gbool)aEnable);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jboolean JNICALL Java_javax_microedition_m3g_CompositingMode__1isDepthWriteEnabled
(JNIEnv* aEnv, jclass, jint aHCompositingMode)
{
    M3G_DO_LOCK
    jboolean enabled = (jboolean)m3gIsDepthWriteEnabled((M3GCompositingMode)aHCompositingMode);
    M3G_DO_UNLOCK(aEnv)
    return enabled;
}

JNIEXPORT jfloat JNICALL Java_javax_microedition_m3g_CompositingMode__1getDepthOffsetFactor
(JNIEnv* aEnv, jclass, jint aHCompositingMode)
{
    M3G_DO_LOCK
    jfloat factor = (jfloat)m3gGetDepthOffsetFactor((M3GCompositingMode)aHCompositingMode);
    M3G_DO_UNLOCK(aEnv)
    return factor;
}

JNIEXPORT jfloat JNICALL Java_javax_microedition_m3g_CompositingMode__1getDepthOffsetUnits
(JNIEnv* aEnv, jclass, jint aHCompositingMode)
{
    M3G_DO_LOCK
    jfloat offset = (jfloat)m3gGetDepthOffsetUnits((M3GCompositingMode)aHCompositingMode);
    M3G_DO_UNLOCK(aEnv)
    return offset;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_CompositingMode__1setBlending
(JNIEnv* aEnv, jclass, jint aHCompositingMode, jint aMode)
{
    M3G_DO_LOCK
    m3gSetBlending((M3GCompositingMode)aHCompositingMode, (int)aMode);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jfloat JNICALL Java_javax_microedition_m3g_CompositingMode__1getAlphaThreshold
(JNIEnv* aEnv, jclass, jint aHCompositingMode)
{
    M3G_DO_LOCK
    jfloat treshold = (jfloat)m3gGetAlphaThreshold((M3GCompositingMode)aHCompositingMode);
    M3G_DO_UNLOCK(aEnv)
    return treshold;
}
