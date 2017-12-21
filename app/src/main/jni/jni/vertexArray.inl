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
#include "javax_microedition_m3g_VertexArray.h"

JNIEXPORT void JNICALL Java_javax_microedition_m3g_VertexArray__1setShort
(JNIEnv* aEnv, jclass, jint aHandle, jint aFirst, jint aCount, jshortArray aSrcArray)
{
    int srcLength = 0;
    unsigned short *srcData = NULL;
    if (aSrcArray)
    {
        srcData = (unsigned short *)aEnv->GetShortArrayElements(aSrcArray, NULL);
        if (srcData == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }
    }

    srcLength = aSrcArray ? aEnv->GetArrayLength(aSrcArray) : 0;

    M3G_DO_LOCK
    m3gSetVertexArrayElements(
        (M3GVertexArray) aHandle, aFirst, aCount, srcLength, M3G_SHORT, srcData);
    M3G_DO_UNLOCK(aEnv)

    if (aSrcArray)
    {
        aEnv->ReleaseShortArrayElements(aSrcArray, (jshort*)srcData, JNI_ABORT);
    }
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_VertexArray__1ctor
(JNIEnv* aEnv, jclass, jint aM3g, jint aNumVertices, jint aNumComponents, jint aComponentSize)
{
    M3G_DO_LOCK
    M3GVertexArray va = m3gCreateVertexArray((M3GInterface)aM3g,
                        aNumVertices,
                        aNumComponents,
                        (aComponentSize == 1) ? M3G_BYTE :
                        (aComponentSize == 2) ? M3G_SHORT :
                        M3G_INT);
    M3G_DO_UNLOCK(aEnv)
    return (M3Guint) va;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_VertexArray__1setByte
(JNIEnv* aEnv, jclass, jint aHandle, jint aFirst, jint aCount, jbyteArray aSrcArray)
{
    int srcLength = 0;
    unsigned char *srcData = NULL;
    if (aSrcArray)
    {
        srcData = (unsigned char *)aEnv->GetByteArrayElements(aSrcArray, NULL);
        if (srcData == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }
    }

    srcLength = aSrcArray ? aEnv->GetArrayLength(aSrcArray) : 0;

    if (validateArray(aEnv, aSrcArray, aCount))
    {
        M3G_DO_LOCK
        m3gSetVertexArrayElements(
            (M3GVertexArray) aHandle, aFirst, aCount, srcLength, M3G_BYTE, srcData);
        M3G_DO_UNLOCK(aEnv)
    }

    if (aSrcArray)
    {
        aEnv->ReleaseByteArrayElements(aSrcArray, (jbyte*)srcData, JNI_ABORT);
    }
}

/* M3G 1.1 JNI Calls */

JNIEXPORT void JNICALL Java_javax_microedition_m3g_VertexArray__1getByte
(JNIEnv* aEnv, jclass, jint aHandle, jint aFirstVertex, jint aNumVertices, jbyteArray aSrcArray)
{
    int dstLength = 0;
    unsigned char *dstData = NULL;
    if (aSrcArray)
    {
        dstData = (unsigned char *)aEnv->GetByteArrayElements(aSrcArray, NULL);
        if (dstData == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }
    }

    dstLength = aSrcArray ? aEnv->GetArrayLength(aSrcArray) : 0;

    /*
    * Parameter checking and exception throwing is handled in engine side, thus no
    * checking / validation is done here.
    */
    M3G_DO_LOCK
    m3gGetVertexArrayElements((M3GVertexArray) aHandle, aFirstVertex, aNumVertices, dstLength, M3G_BYTE, dstData);
    M3G_DO_UNLOCK(aEnv)

    if (aSrcArray)
    {
        /* copy dstData array to java side and release both arrays */
        aEnv->ReleaseByteArrayElements(aSrcArray, (jbyte*)dstData, 0);
    }
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_VertexArray__1getShort
(JNIEnv* aEnv, jclass, jint aHandle, jint aFirstVertex, jint aNumVertices, jshortArray aSrcArray)
{
    int dstLength = 0;
    unsigned short *dstData = NULL;
    if (aSrcArray)
    {
        dstData = (unsigned short *)aEnv->GetShortArrayElements(aSrcArray, NULL);
        if (dstData == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }
    }

    dstLength = aSrcArray ? aEnv->GetArrayLength(aSrcArray) : 0;

    /*
    * Parameter checking and exception throwing is handled in engine side, thus no
    * checking / validation is done here.
    */
    M3G_DO_LOCK
    m3gGetVertexArrayElements((M3GVertexArray) aHandle, aFirstVertex, aNumVertices, dstLength, M3G_SHORT, dstData);
    M3G_DO_UNLOCK(aEnv)

    if (aSrcArray)
    {
        /* copy dstData array to java side and release both arrays */
        aEnv->ReleaseShortArrayElements(aSrcArray, (jshort*)dstData, 0);
    }
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_VertexArray__1getComponentCount
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3Gsizei size;
    M3G_DO_LOCK
    m3gGetVertexArrayParams((M3GVertexArray)aHandle, NULL, &size, NULL, NULL);
    M3G_DO_UNLOCK(aEnv)
    return (jint)size;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_VertexArray__1getComponentType
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3Gdatatype type;
    M3G_DO_LOCK
    m3gGetVertexArrayParams((M3GVertexArray)aHandle, NULL, NULL, &type, NULL);
    M3G_DO_UNLOCK(aEnv)
    type = (type == M3G_BYTE) ? (M3Gdatatype) 1 : (M3Gdatatype) 2;
    return (jint)type;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_VertexArray__1getVertexCount
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3Gsizei count;
    M3G_DO_LOCK
    m3gGetVertexArrayParams((M3GVertexArray)aHandle, &count, NULL, NULL, NULL);
    M3G_DO_UNLOCK(aEnv)
    return (jint)count;
}
