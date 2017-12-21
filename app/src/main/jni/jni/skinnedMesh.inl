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
#include "javax_microedition_m3g_SkinnedMesh.h"

JNIEXPORT void JNICALL Java_javax_microedition_m3g_SkinnedMesh__1addTransform
(JNIEnv* aEnv, jclass, jint aHandle, jint aHBone, jint aWeight, jint aFirstVertex, jint aNumVertices)
{
    M3G_DO_LOCK
    m3gAddTransform((M3GSkinnedMesh)aHandle, (M3GNode)aHBone, aWeight, aFirstVertex, aNumVertices);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_SkinnedMesh__1ctor
(JNIEnv* aEnv, jclass, jint aM3g, jint aHVertices, jintArray aHTriangles, jintArray aHAppearances, jint aHSkeleton)
{
    jint* appearances = NULL;
    if (aHAppearances)
    {
        appearances = aEnv->GetIntArrayElements(aHAppearances, NULL);
        if (appearances == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }

    jint* triangles = NULL;
    if (aHTriangles)
    {
        triangles = aEnv->GetIntArrayElements(aHTriangles, NULL);
        if (triangles == NULL)
        {
            if (appearances)
            {
                aEnv->ReleaseIntArrayElements(aHAppearances, appearances, JNI_ABORT);
            }
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }
    jint numTriangles = aEnv->GetArrayLength(aHTriangles);
    M3GInterface m3g = (M3GInterface) aM3g;

    M3G_DO_LOCK
    M3Guint ret = (M3Guint)m3gCreateSkinnedMesh(m3g,
                  (M3GVertexBuffer)aHVertices,
                  (M3GIndexBuffer*)triangles,
                  (M3GAppearance *)appearances,
                  numTriangles,
                  (M3GGroup)aHSkeleton);
    M3G_DO_UNLOCK(aEnv)

    if (appearances)
    {
        aEnv->ReleaseIntArrayElements(aHAppearances, appearances, JNI_ABORT);
    }
    if (triangles)
    {
        aEnv->ReleaseIntArrayElements(aHTriangles, triangles, JNI_ABORT);
    }

    return ret;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_SkinnedMesh__1getSkeleton
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint skeleton = (M3Guint)m3gGetSkeleton((M3GSkinnedMesh)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return skeleton;
}

/* M3G 1.1 JNI Calls */

JNIEXPORT void JNICALL Java_javax_microedition_m3g_SkinnedMesh__1getBoneTransform
(JNIEnv* aEnv, jclass, jint aHandle, jint aBone, jbyteArray aTransform)
{
    jbyte *transform = NULL;
    if (aTransform)
    {
        transform = aEnv->GetByteArrayElements(aTransform, NULL);
        if (transform == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }
    }

    M3G_DO_LOCK
    m3gGetBoneTransform((M3GSkinnedMesh)aHandle, (M3GNode)aBone, (M3GMatrix *)transform);
    M3G_DO_UNLOCK(aEnv)

    if (transform)
    {
        /* Update array to java side and release arrays */
        aEnv->ReleaseByteArrayElements(aTransform, transform, 0);
    }
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_SkinnedMesh__1getBoneVertices
(JNIEnv* aEnv, jclass, jint aHandle, jint aBone, jintArray aIndices, jfloatArray aWeights)
{
    int *indices = NULL;
    float *weights = NULL;
    jint vertices = 0;

    /* get indices int array */
    if (aIndices != NULL)
    {
        indices = aEnv->GetIntArrayElements(aIndices, NULL);
        if (indices == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }

    /* get weights float array */
    if (aWeights != NULL)
    {
        weights = aEnv->GetFloatArrayElements(aWeights, NULL);
        if (weights == NULL)
        {
            if (indices)
            {
                /* Release indices int array*/
                aEnv->ReleaseIntArrayElements(aIndices, indices, JNI_ABORT);
            }
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }

    M3G_DO_LOCK
    /* Get number of vertices */
    int requiredLength = m3gGetBoneVertices((M3GSkinnedMesh)aHandle, (M3GNode)aBone, NULL, NULL);
    M3G_DO_UNLOCK(aEnv)

    /* If return value is 0, an error has occured so we leave here */
    if (requiredLength == 0)
    {

        /* release arrays before exiting */
        if (indices)
        {
            /* Release indices int array */
            aEnv->ReleaseIntArrayElements(aIndices, indices, JNI_ABORT);
        }
        if (weights)
        {
            /* Release indices int array */
            aEnv->ReleaseFloatArrayElements(aWeights, weights, JNI_ABORT);
        }
        return 0;
    }
    /*
    *  If either of arrays is null, lengths are not checked.
    *  If length validation fails, exception is automatically raised.
    */
    if (indices != NULL && weights != NULL &&
            (!validateArray(aEnv, (jbyteArray)aIndices, requiredLength) ||
             !validateArray(aEnv, (jbyteArray)aWeights, requiredLength)))
    {

        /* release arrays here */
        if (indices)
        {
            /* Release indices int array */
            aEnv->ReleaseIntArrayElements(aIndices, indices, JNI_ABORT);
        }
        if (weights)
        {
            /* Release indices int array */
            aEnv->ReleaseFloatArrayElements(aWeights, weights, JNI_ABORT);
        }

        return 0;
    }
    else
    {
        M3G_DO_LOCK
        vertices = m3gGetBoneVertices((M3GSkinnedMesh)aHandle, (M3GNode)aBone, indices, weights);
        M3G_DO_UNLOCK(aEnv)
    }

    if (indices)
    {
        /* Update array to java side and release arrays */
        aEnv->ReleaseIntArrayElements(aIndices, indices, 0);
    }
    if (weights)
    {
        /* Update array to java side and release arrays */
        aEnv->ReleaseFloatArrayElements(aWeights, weights, 0);
    }
    return vertices;
}

