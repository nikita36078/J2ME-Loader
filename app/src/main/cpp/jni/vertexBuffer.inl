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

#include "javax_microedition_m3g_VertexBuffer.h"

JNIEXPORT void JNICALL Java_javax_microedition_m3g_VertexBuffer__1setTexCoords
(JNIEnv* aEnv, jclass, jint aHBuffer, jint aTexUnit, jint aHArray, jfloat aScale, jfloatArray aSrcArray)
{
    int biasLength = 0;
    float *bias = NULL;
    if (aSrcArray)
    {
        bias = (float *)aEnv->GetFloatArrayElements(aSrcArray, NULL);
        if (bias == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }
    }

    biasLength = aSrcArray ? aEnv->GetArrayLength(aSrcArray) : 0;

    M3G_DO_LOCK
    m3gSetTexCoordArray((M3GVertexBuffer)aHBuffer, aTexUnit, (M3GVertexArray)aHArray, aScale, bias, biasLength);
    M3G_DO_UNLOCK(aEnv)

    if (aSrcArray)
    {
        aEnv->ReleaseFloatArrayElements(aSrcArray, bias, JNI_ABORT);
    }
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_VertexBuffer__1getArray
(JNIEnv* aEnv, jclass, jint aHBuffer, jint aWhich, jfloatArray aDstArray)
{
    int dstLength = 0;
    float *dstData = NULL;
    if (aDstArray)
    {
        dstData = (float *)aEnv->GetFloatArrayElements(aDstArray, NULL);
        if (dstData == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }

    dstLength = aDstArray ? aEnv->GetArrayLength(aDstArray) : 0;

    M3G_DO_LOCK
    M3Guint ret = (M3Guint)m3gGetVertexArray((M3GVertexBuffer)aHBuffer, aWhich, dstData, dstLength);
    M3G_DO_UNLOCK(aEnv)

    if (aDstArray)
    {
        aEnv->ReleaseFloatArrayElements(aDstArray, dstData, 0);
    }

    return ret;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_VertexBuffer__1getDefaultColor
(JNIEnv* aEnv, jclass, jint aHBuffer)
{
    M3G_DO_LOCK
    jint color = (M3Guint)m3gGetVertexDefaultColor((M3GVertexBuffer)aHBuffer);
    M3G_DO_UNLOCK(aEnv)
    return color;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_VertexBuffer__1ctor
(JNIEnv* aEnv, jclass, jint aM3g)
{
    M3G_DO_LOCK
    M3GVertexBuffer vb = m3gCreateVertexBuffer((M3GInterface)aM3g);
    M3G_DO_UNLOCK(aEnv)
    return (M3Guint) vb;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_VertexBuffer__1setNormals
(JNIEnv* aEnv, jclass, jint aHBuffer, jint aHArray)
{
    M3G_DO_LOCK
    m3gSetNormalArray((M3GVertexBuffer) aHBuffer, (M3GVertexArray) aHArray);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_VertexBuffer__1setDefaultColor
(JNIEnv* aEnv, jclass, jint aHBuffer, jint aARGB)
{
    M3G_DO_LOCK
    m3gSetVertexDefaultColor((M3GVertexBuffer)aHBuffer, aARGB);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_VertexBuffer__1setColors
(JNIEnv* aEnv, jclass, jint aHBuffer, jint aHArray)
{
    M3G_DO_LOCK
    m3gSetColorArray((M3GVertexBuffer) aHBuffer, (M3GVertexArray) aHArray);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_VertexBuffer__1setVertices
(JNIEnv* aEnv, jclass, jint aHBuffer, jint aHArray, jfloat aScale, jfloatArray aSrcArray)
{
    int biasLength = 0;
    float *bias = NULL;
    if (aSrcArray)
    {
        bias = (float *)aEnv->GetFloatArrayElements(aSrcArray, NULL);
        if (bias == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }
    }

    biasLength = aSrcArray ? aEnv->GetArrayLength(aSrcArray) : 0;

    M3G_DO_LOCK
    m3gSetVertexArray((M3GVertexBuffer)aHBuffer, (M3GVertexArray)aHArray, aScale, bias, biasLength);
    M3G_DO_UNLOCK(aEnv)

    if (aSrcArray)
    {
        aEnv->ReleaseFloatArrayElements(aSrcArray, bias, JNI_ABORT);
    }
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_VertexBuffer__1getVertexCount
(JNIEnv* aEnv, jclass, jint aHBuffer)
{
    M3G_DO_LOCK
    jint count = (M3Guint)m3gGetVertexCount((M3GVertexBuffer)aHBuffer);
    M3G_DO_UNLOCK(aEnv)
    return count;
}
