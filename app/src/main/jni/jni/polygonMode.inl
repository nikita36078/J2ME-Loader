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
#include "javax_microedition_m3g_PolygonMode.h"

JNIEXPORT void JNICALL Java_javax_microedition_m3g_PolygonMode__1setCulling
(JNIEnv* aEnv, jclass, jint aHPolygonMode, jint aModeBits)
{
    M3G_DO_LOCK
    m3gSetCulling((M3GPolygonMode)aHPolygonMode, aModeBits);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_PolygonMode__1getCulling
(JNIEnv* aEnv, jclass, jint aHPolygonMode)
{
    M3G_DO_LOCK
    jint culling = (jint)m3gGetCulling((M3GPolygonMode)aHPolygonMode);
    M3G_DO_UNLOCK(aEnv)
    return culling;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_PolygonMode__1setWinding
(JNIEnv* aEnv, jclass, jint aHPolygonMode, jint aModeBits)
{
    M3G_DO_LOCK
    m3gSetWinding((M3GPolygonMode)aHPolygonMode, aModeBits);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_PolygonMode__1getWinding
(JNIEnv* aEnv, jclass, jint aHPolygonMode)
{
    M3G_DO_LOCK
    jint winding = (jint)m3gGetWinding((M3GPolygonMode)aHPolygonMode);
    M3G_DO_UNLOCK(aEnv)
    return winding;
}

JNIEXPORT jboolean JNICALL Java_javax_microedition_m3g_PolygonMode__1isTwoSidedLightingEnabled
(JNIEnv* aEnv, jclass, jint aHPolygonMode)
{
    M3G_DO_LOCK
    jboolean enabled = (jboolean)m3gIsTwoSidedLightingEnabled((M3GPolygonMode)aHPolygonMode);
    M3G_DO_UNLOCK(aEnv)
    return enabled;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_PolygonMode__1setTwoSidedLightingEnable
(JNIEnv* aEnv, jclass, jint aHPolygonMode, jboolean aEnable)
{
    M3G_DO_LOCK
    m3gSetTwoSidedLightingEnable((M3GPolygonMode)aHPolygonMode, (M3Gbool)aEnable);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_PolygonMode__1setPerspectiveCorrectionEnable
(JNIEnv* aEnv, jclass, jint aHPolygonMode, jboolean aEnable)
{
    M3G_DO_LOCK
    m3gSetPerspectiveCorrectionEnable((M3GPolygonMode)aHPolygonMode, (M3Gbool)aEnable);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_PolygonMode__1ctor
(JNIEnv* aEnv, jclass, jint aM3g)
{
    M3G_DO_LOCK
    M3GPolygonMode gm = m3gCreatePolygonMode((M3GInterface)aM3g);
    M3G_DO_UNLOCK(aEnv)
    return (M3Guint)gm;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_PolygonMode__1setLocalCameraLightingEnable
(JNIEnv* aEnv, jclass, jint aHPolygonMode, jboolean aEnable)
{
    M3G_DO_LOCK
    m3gSetLocalCameraLightingEnable((M3GPolygonMode)aHPolygonMode, (M3Gbool)aEnable);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_PolygonMode__1setShading
(JNIEnv* aEnv, jclass, jint aHPolygonMode, jint aModeBits)
{
    M3G_DO_LOCK
    m3gSetShading((M3GPolygonMode)aHPolygonMode, aModeBits);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_PolygonMode__1getShading
(JNIEnv* aEnv, jclass, jint aHPolygonMode)
{
    M3G_DO_LOCK
    jint shading = (jint)m3gGetShading((M3GPolygonMode)aHPolygonMode);
    M3G_DO_UNLOCK(aEnv)
    return shading;
}

/* M3G 1.1 JNI Calls */

JNIEXPORT jboolean JNICALL Java_javax_microedition_m3g_PolygonMode__1isLocalCameraLightingEnabled
(JNIEnv* aEnv, jclass, jint aHPolygonMode)
{
    M3G_DO_LOCK
    jboolean enabled = (jboolean)m3gIsLocalCameraLightingEnabled((M3GPolygonMode)aHPolygonMode);
    M3G_DO_UNLOCK(aEnv)
    return enabled;
}

JNIEXPORT jboolean JNICALL Java_javax_microedition_m3g_PolygonMode__1isPerspectiveCorrectionEnabled
(JNIEnv* aEnv, jclass, jint aHPolygonMode)
{
    M3G_DO_LOCK
    jboolean enabled = (jboolean)m3gIsPerspectiveCorrectionEnabled((M3GPolygonMode)aHPolygonMode);
    M3G_DO_UNLOCK(aEnv)
    return enabled;
}
