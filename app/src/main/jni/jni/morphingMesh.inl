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

#include "javax_microedition_m3g_MorphingMesh.h"

JNIEXPORT void JNICALL Java_javax_microedition_m3g_MorphingMesh__1setWeights
(JNIEnv* aEnv, jclass, jint aHandle, jfloatArray aWeightArray)
{
    if (aWeightArray != NULL)
    {
        jfloat* weightArray = aEnv->GetFloatArrayElements(aWeightArray, NULL);
        if (weightArray == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }

        M3G_DO_LOCK
        m3gSetWeights((M3GMorphingMesh)aHandle, (M3Gfloat *)weightArray, aEnv->GetArrayLength(aWeightArray));
        M3G_DO_UNLOCK(aEnv)

        if (weightArray)
        {
            aEnv->ReleaseFloatArrayElements(aWeightArray, weightArray, JNI_ABORT);
        }
    }
    else
    {
        M3G_RAISE_EXCEPTION(aEnv, "java/lang/NullPointerException");
    }
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_MorphingMesh__1getMorphTargetCount
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint count = (M3Guint)m3gGetMorphTargetCount((M3GMorphingMesh)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return count;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_MorphingMesh__1getWeights
(JNIEnv* aEnv, jclass, jint aHandle, jfloatArray aWeightArray)
{
    if (aWeightArray != NULL)
    {
        jfloat* weightArray = aEnv->GetFloatArrayElements(aWeightArray, NULL);
        if (weightArray == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }

        M3G_DO_LOCK
        m3gGetWeights((M3GMorphingMesh)aHandle, (M3Gfloat *)weightArray, aEnv->GetArrayLength(aWeightArray));
        M3G_DO_UNLOCK(aEnv)

        if (weightArray)
        {
            aEnv->ReleaseFloatArrayElements(aWeightArray, weightArray, 0);
        }
    }
    else
    {
        M3G_RAISE_EXCEPTION(aEnv, "java/lang/NullPointerException");
    }
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_MorphingMesh__1ctor
(JNIEnv* aEnv, jclass, jint aM3g, jint aHVertices, jintArray aHTargets, jintArray aHTriangles, jintArray aHAppearances)
{
    if (aHVertices == 0 || aHTargets == NULL || aHTriangles == NULL)
    {
        M3G_RAISE_EXCEPTION(aEnv, "java/lang/NullPointerException");
        return 0;
    }

    int trianglesLen = aEnv->GetArrayLength(aHTriangles);
    int targetsLen = aEnv->GetArrayLength(aHTargets);

    if (trianglesLen == 0 || targetsLen == 0)
    {
        M3G_RAISE_EXCEPTION(aEnv, "java/lang/IllegalArgumentException");
        return 0;
    }

    if (aHAppearances != NULL)
    {
        int appearancesLen = aEnv->GetArrayLength(aHAppearances);
        if (appearancesLen < trianglesLen)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/IllegalArgumentException");
            return 0;
        }
    }

    jint* targets = aEnv->GetIntArrayElements(aHTargets, NULL);
    if (targets == NULL)
    {
        M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
        return 0;
    }

    jint* triangles = aEnv->GetIntArrayElements(aHTriangles, NULL);
    if (triangles == NULL)
    {
        aEnv->ReleaseIntArrayElements(aHTargets, targets, JNI_ABORT);

        M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
        return 0;
    }

    jint* appearances = NULL;
    if (aHAppearances)
    {
        appearances = aEnv->GetIntArrayElements(aHAppearances, NULL);
        if (appearances == NULL)
        {
            aEnv->ReleaseIntArrayElements(aHTargets, targets, JNI_ABORT);
            aEnv->ReleaseIntArrayElements(aHTriangles, triangles, JNI_ABORT);

            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }

    M3G_DO_LOCK
    M3Guint ret = (M3Guint)m3gCreateMorphingMesh((M3GInterface)aM3g,
                  (M3GVertexBuffer)aHVertices,
                  (M3GVertexBuffer*)targets,
                  (M3GIndexBuffer*)triangles,
                  (M3GAppearance*)appearances,
                  trianglesLen,
                  targetsLen);
    M3G_DO_UNLOCK(aEnv)

    aEnv->ReleaseIntArrayElements(aHTargets, targets, JNI_ABORT);
    aEnv->ReleaseIntArrayElements(aHTriangles, triangles, JNI_ABORT);
    if (appearances)
    {
        aEnv->ReleaseIntArrayElements(aHAppearances, appearances, JNI_ABORT);
    }

    return ret;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_MorphingMesh__1getMorphTarget
(JNIEnv* aEnv, jclass, jint aHandle, jint aIndex)
{
    M3G_DO_LOCK
    jint target = (M3Guint)m3gGetMorphTarget((M3GMorphingMesh)aHandle, aIndex);
    M3G_DO_UNLOCK(aEnv)
    return target;
}
