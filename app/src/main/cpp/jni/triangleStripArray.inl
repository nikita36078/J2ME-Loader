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
#include "javax_microedition_m3g_TriangleStripArray.h"

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_TriangleStripArray__1createImplicit
(JNIEnv* aEnv, jclass, jint aM3g, jint first, jintArray aLengthArray)
{
    M3GIndexBuffer buffer;

    int *lengths = NULL;
    if (aLengthArray)
    {
        lengths = (int *)(aEnv->GetIntArrayElements(aLengthArray, NULL));
        if (lengths == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }
    int count = aLengthArray != NULL ? aEnv->GetArrayLength(aLengthArray) : 0;

    M3G_DO_LOCK
    buffer = m3gCreateImplicitStripBuffer((M3GInterface)aM3g,
                                          count,
                                          lengths,
                                          first);
    M3G_DO_UNLOCK(aEnv)

    if (lengths)
    {
        aEnv->ReleaseIntArrayElements(aLengthArray, lengths, JNI_ABORT);
    }

    return (M3Guint) buffer;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_TriangleStripArray__1createExplicit
(JNIEnv* aEnv, jclass, jint aM3g, jintArray aIndices, jintArray aLengths)
{
    M3GIndexBuffer buffer;


    jint* lengths = NULL;
    jint* indices = NULL;
    if (aLengths)
    {
        lengths = aEnv->GetIntArrayElements(aLengths, NULL);
        if (lengths == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }
    if (aIndices)
    {
        indices = aEnv->GetIntArrayElements(aIndices, NULL);
        if (indices == NULL)
        {
            if (lengths)
            {
                aEnv->ReleaseIntArrayElements(aLengths, lengths, JNI_ABORT);
            }

            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }
    M3G_DO_LOCK
    buffer = m3gCreateStripBuffer((M3GInterface)aM3g,
                                  M3G_TRIANGLE_STRIPS,
                                  aLengths != NULL ? aEnv->GetArrayLength(aLengths) : 0,
                                  (M3Gsizei *)lengths,
                                  M3G_INT,
                                  aIndices != NULL ? aEnv->GetArrayLength(aIndices) : 0,
                                  (void *)indices);
    M3G_DO_UNLOCK(aEnv)

    if (indices)
    {
        aEnv->ReleaseIntArrayElements(aIndices, indices, JNI_ABORT);
    }
    if (lengths)
    {
        aEnv->ReleaseIntArrayElements(aLengths, lengths, JNI_ABORT);
    }

    return (M3Guint) buffer;
}

/* M3G 1.1 JNI Calls */

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_TriangleStripArray__1getIndexCount
(JNIEnv* aEnv, jclass, jint aHTsa)
{
    M3G_DO_LOCK
    jint size = (jint)m3gGetBatchSize((M3GIndexBuffer)aHTsa, 0);
    M3G_DO_UNLOCK(aEnv)
    return size;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_TriangleStripArray__1getIndices
(JNIEnv* aEnv, jclass, jint aHTsa, jintArray aIndices)
{
    jint* indices = NULL;

    if (aIndices)
    {
        indices = aEnv->GetIntArrayElements(aIndices, NULL);
        if (indices == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }
    }
    M3G_DO_LOCK
    m3gGetBatchIndices((M3GIndexBuffer)aHTsa, 0, indices);
    M3G_DO_UNLOCK(aEnv)

    if (indices)
    {
        aEnv->ReleaseIntArrayElements(aIndices, indices, 0);
    }
}
