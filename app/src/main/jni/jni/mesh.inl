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

#include "javax_microedition_m3g_Mesh.h"

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Mesh__1getIndexBuffer
(JNIEnv* aEnv, jclass, jint aHandle, jint aIndex)
{
    M3G_DO_LOCK
    jint buffer = (M3Guint)m3gGetIndexBuffer((M3GMesh)aHandle, aIndex);
    M3G_DO_UNLOCK(aEnv)
    return buffer;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Mesh__1getSubmeshCount
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint count = (M3Guint)m3gGetSubmeshCount((M3GMesh)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return count;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Mesh__1ctor
(JNIEnv* aEnv, jclass, jint aM3g, jint aHVertices, jintArray aHTriangles, jintArray aHAppearances)
{
    if (aHVertices == 0 || aHTriangles == NULL)
    {
        M3G_RAISE_EXCEPTION(aEnv, "java/lang/NullPointerException");
        return 0;
    }

    int trianglesLen = aEnv->GetArrayLength(aHTriangles);
    int appearancesLen = aHAppearances ? aEnv->GetArrayLength(aHAppearances) : 0;
    if (trianglesLen == 0 || (aHAppearances != NULL && appearancesLen < trianglesLen))
    {
        M3G_RAISE_EXCEPTION(aEnv, "java/lang/IllegalArgumentException");
        return 0;
    }

    jint* triangle = aEnv->GetIntArrayElements(aHTriangles, NULL);
    if (triangle == NULL)
    {
        M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
        return 0;
    }

    jint* appearance = NULL;
    if (aHAppearances)
    {
        appearance = aEnv->GetIntArrayElements(aHAppearances, NULL);
        if (appearance == NULL)
        {
            aEnv->ReleaseIntArrayElements(aHTriangles, triangle, JNI_ABORT);

            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }
    M3G_DO_LOCK
    M3Guint ret = (M3Guint)m3gCreateMesh((M3GInterface)aM3g,
                                         (M3GVertexBuffer)aHVertices,
                                         (M3GIndexBuffer*)triangle,
                                         (M3GAppearance*)appearance,
                                         trianglesLen);
    M3G_DO_UNLOCK(aEnv)

    if (triangle)
    {
        aEnv->ReleaseIntArrayElements(aHTriangles, triangle, JNI_ABORT);
    }
    if (appearance)
    {
        aEnv->ReleaseIntArrayElements(aHAppearances, appearance, JNI_ABORT);
    }

    return ret;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Mesh__1getVertexBuffer
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint vBuffer = (M3Guint)m3gGetVertexBuffer((M3GMesh)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return vBuffer;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Mesh__1getAppearance
(JNIEnv* aEnv, jclass, jint aHandle, jint aIndex)
{
    M3G_DO_LOCK
    jint appearence = (M3Guint)m3gGetAppearance((M3GMesh)aHandle, aIndex);
    M3G_DO_UNLOCK(aEnv)
    return appearence;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Mesh__1setAppearance
(JNIEnv* aEnv, jclass, jint aHandle, jint aIndex, jint aHAppearance)
{
    M3G_DO_LOCK
    m3gSetAppearance((M3GMesh)aHandle, aIndex, (M3GAppearance)aHAppearance);
    M3G_DO_UNLOCK(aEnv)
}
