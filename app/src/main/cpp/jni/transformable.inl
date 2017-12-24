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

#include "javax_microedition_m3g_Transformable.h"

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Transformable__1preRotate
(JNIEnv* aEnv, jclass, jint aHandle, jfloat aAngle, jfloat aAx, jfloat aAy, jfloat aAz)
{
    M3G_DO_LOCK
    m3gPreRotate((M3GTransformable)aHandle, aAngle, aAx, aAy, aAz);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Transformable__1getComposite
(JNIEnv* aEnv, jclass, jint aHandle, jbyteArray aDstArray)
{
    if (validateArray(aEnv, aDstArray, sizeof(M3GMatrix)))
    {
        jbyte* dstArray = aEnv->GetByteArrayElements(aDstArray, NULL);
        if (dstArray == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }

        M3G_DO_LOCK
        m3gGetCompositeTransform((M3GTransformable)aHandle, (M3GMatrix *)dstArray);
        M3G_DO_UNLOCK(aEnv)
        aEnv->ReleaseByteArrayElements(aDstArray, dstArray, 0);
    }
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Transformable__1setTransform
(JNIEnv* aEnv, jclass, jint aHandle, jbyteArray aSrcArray)
{
    if (aSrcArray != NULL)
    {
        jbyte* srcArray = aEnv->GetByteArrayElements(aSrcArray, NULL);
        if (srcArray == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }
        M3G_DO_LOCK
        m3gSetTransform((M3GTransformable)aHandle, (const M3GMatrix *)srcArray);
        M3G_DO_UNLOCK(aEnv)
        aEnv->ReleaseByteArrayElements(aSrcArray, srcArray, JNI_ABORT);
    }
    else
    {
        M3G_DO_LOCK
        m3gSetTransform((M3GTransformable)aHandle, NULL);
        M3G_DO_UNLOCK(aEnv)
    }
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Transformable__1setTranslation
(JNIEnv* aEnv, jclass, jint aHandle, jfloat aTx, jfloat aTy, jfloat aTz, jboolean aAbsolute)
{

    if (aAbsolute)
    {
        M3G_DO_LOCK
        m3gSetTranslation((M3GTransformable)aHandle, aTx, aTy, aTz);
        M3G_DO_UNLOCK(aEnv)
    }
    else
    {
        M3G_DO_LOCK
        m3gTranslate((M3GTransformable)aHandle, aTx, aTy, aTz);
        M3G_DO_UNLOCK(aEnv)
    }
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Transformable__1setOrientation
(JNIEnv* aEnv, jclass, jint aHandle, jfloat aAngle, jfloat aAx, jfloat aAy, jfloat aAz, jboolean aAbsolute)
{

    if (aAbsolute)
    {
        M3G_DO_LOCK
        m3gSetOrientation((M3GTransformable)aHandle, aAngle, aAx, aAy, aAz);
        M3G_DO_UNLOCK(aEnv)
    }
    else
    {
        M3G_DO_LOCK
        m3gPostRotate((M3GTransformable)aHandle, aAngle, aAx, aAy, aAz);
        M3G_DO_UNLOCK(aEnv)
    }
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Transformable__1getTransform
(JNIEnv* aEnv, jclass, jint aHandle, jbyteArray aDstArray)
{
    if (validateArray(aEnv, aDstArray, sizeof(M3GMatrix)))
    {
        jbyte* dstArray = aEnv->GetByteArrayElements(aDstArray, NULL);
        if (dstArray == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }
        M3G_DO_LOCK
        m3gGetTransform((M3GTransformable)aHandle, (M3GMatrix *)dstArray);
        M3G_DO_UNLOCK(aEnv)
        aEnv->ReleaseByteArrayElements(aDstArray, dstArray, 0);
    }
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Transformable__1setScale
(JNIEnv* aEnv, jclass, jint aHandle, jfloat aSx, jfloat aSy, jfloat aSz, jboolean aAbsolute)
{

    if (aAbsolute)
    {
        M3G_DO_LOCK
        m3gSetScale((M3GTransformable)aHandle, aSx, aSy, aSz);
        M3G_DO_UNLOCK(aEnv)
    }
    else
    {
        M3G_DO_LOCK
        m3gScale((M3GTransformable)aHandle, aSx, aSy, aSz);
        M3G_DO_UNLOCK(aEnv)
    }
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Transformable__1getTranslation
(JNIEnv* aEnv, jclass, jint aHandle, jfloatArray aDstArray)
{
    if (validateArray(aEnv, (jbyteArray)aDstArray, 3))
    {
        jfloat* dstArray = aEnv->GetFloatArrayElements(aDstArray, NULL);
        if (dstArray == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }
        M3G_DO_LOCK
        m3gGetTranslation((M3GTransformable)aHandle, (M3Gfloat*)dstArray);
        M3G_DO_UNLOCK(aEnv)
        aEnv->ReleaseFloatArrayElements(aDstArray, dstArray, 0);
    }
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Transformable__1getScale
(JNIEnv* aEnv, jclass, jint aHandle, jfloatArray aDstArray)
{
    if (validateArray(aEnv, (jbyteArray)aDstArray, 3))
    {
        jfloat* dstArray = aEnv->GetFloatArrayElements(aDstArray, NULL);
        if (dstArray == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }
        M3G_DO_LOCK
        m3gGetScale((M3GTransformable)aHandle, (M3Gfloat*)dstArray);
        M3G_DO_UNLOCK(aEnv)
        aEnv->ReleaseFloatArrayElements(aDstArray, dstArray, 0);
    }
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Transformable__1getOrientation
(JNIEnv* aEnv, jclass, jint aHandle, jfloatArray aDstArray)
{
    if (validateArray(aEnv, (jbyteArray)aDstArray, 4))
    {
        jfloat* dstArray = aEnv->GetFloatArrayElements(aDstArray, NULL);
        if (dstArray == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return;
        }
        M3G_DO_LOCK
        m3gGetOrientation((M3GTransformable)aHandle, (M3Gfloat*)dstArray);
        M3G_DO_UNLOCK(aEnv)
        aEnv->ReleaseFloatArrayElements(aDstArray, dstArray, 0);
    }
}
