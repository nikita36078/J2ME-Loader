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

#include "javax_microedition_m3g_Camera.h"

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Camera__1setPerspective
(JNIEnv* aEnv, jclass, jint aHandle, jfloat aFovy, jfloat aAspectRatio, jfloat aNear, jfloat aFar)
{
    M3G_DO_LOCK
    m3gSetPerspective((M3GCamera)aHandle, aFovy, aAspectRatio, aNear, aFar);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Camera__1setGeneric
(JNIEnv* aEnv, jclass, jint aHandle, jbyteArray aTransform)
{
    jbyte* elems = NULL;
    if (aTransform)
    {
        elems = aEnv->GetByteArrayElements(aTransform, NULL);
        if (elems == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }
    }
    M3G_DO_LOCK
    m3gSetProjectionMatrix((M3GCamera)aHandle,
                           (const M3GMatrix *)elems);
    M3G_DO_UNLOCK(aEnv)
    if (elems)
        aEnv->ReleaseByteArrayElements(aTransform, elems, 0);
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Camera__1getProjectionAsParams
(JNIEnv* aEnv, jclass, jint aHandle, jfloatArray aParams)
{
    jfloat* elems = NULL;

    if (aParams && aEnv->GetArrayLength(aParams) < 4)
    {
        M3G_RAISE_EXCEPTION(aEnv, "java/lang/IllegalArgumentException");
        return 0;
    }
    elems = NULL;
    if (aParams)
    {
        elems = aEnv->GetFloatArrayElements(aParams, NULL);
        if (elems == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }

    M3G_DO_LOCK
    jint ret = m3gGetProjectionAsParams((M3GCamera)aHandle, (jfloat*)elems);
    M3G_DO_UNLOCK(aEnv)

    if (elems)
    {
        aEnv->ReleaseFloatArrayElements(aParams, elems, 0);
    }

    return ret;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Camera__1setParallel
(JNIEnv* aEnv, jclass, jint aHandle, jfloat aHeight, jfloat aAspectRatio, jfloat aNear, jfloat aFar)
{
    M3G_DO_LOCK
    m3gSetParallel((M3GCamera)aHandle, aHeight, aAspectRatio, aNear, aFar);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Camera__1ctor
(JNIEnv* aEnv, jclass, jint aM3g)
{
    M3G_DO_LOCK
    jint handle = (jint)m3gCreateCamera((M3GInterface)aM3g);
    M3G_DO_UNLOCK(aEnv)
    return handle;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Camera__1getProjectionAsTransform
(JNIEnv* aEnv, jclass, jint aHandle, jbyteArray aTransform)
{
    jbyte* elems = NULL;
    if (aTransform)
    {
        elems = aEnv->GetByteArrayElements(aTransform, NULL);
        if (elems == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }

    M3G_DO_LOCK
    jint ret = m3gGetProjectionAsMatrix((M3GCamera)aHandle,
                                        (M3GMatrix *)elems);
    M3G_DO_UNLOCK(aEnv)

    if (elems)
        aEnv->ReleaseByteArrayElements(aTransform, elems, 0);
    return ret;
}
