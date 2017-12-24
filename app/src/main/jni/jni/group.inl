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

#include "javax_microedition_m3g_Group.h"

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Group__1addChild
(JNIEnv* aEnv, jclass, jint aHandle, jint aHNode)
{
    M3G_DO_LOCK
    m3gAddChild((M3GGroup)aHandle, (M3GNode)aHNode);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Group__1ctor
(JNIEnv* aEnv, jclass, jint aM3g)
{
    M3G_DO_LOCK
    jint handle = (jint)m3gCreateGroup((M3GInterface)aM3g);
    M3G_DO_UNLOCK(aEnv)
    return handle;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Group__1pick2D
(JNIEnv* aEnv, jclass, jint aHandle, jint aMask, jfloat aX, jfloat aY, jint aHCamera, jfloatArray aResult)
{
    jfloat* elems = NULL;
    if (aResult)
    {
        elems = aEnv->GetFloatArrayElements(aResult, NULL);
        if (elems == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }

    M3G_BEGIN_PROFILE(M3G_PROFILE_PICK);
    M3G_DO_LOCK
    jint ret = (jint)m3gPick2D((M3GGroup)aHandle, aMask, aX, aY, (M3GCamera)aHCamera, (jfloat*)elems);
    M3G_DO_UNLOCK(aEnv)
    M3G_END_PROFILE(M3G_PROFILE_PICK);

    if (aResult)
        aEnv->ReleaseFloatArrayElements(aResult, elems, 0);
    return ret;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Group__1getChild
(JNIEnv* aEnv, jclass, jint aHandle, jint aIndex)
{
    M3G_DO_LOCK
    jint child = (jint)m3gGetChild((M3GGroup)aHandle, aIndex);
    M3G_DO_UNLOCK(aEnv)
    return child;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Group__1pick3D
(JNIEnv* aEnv, jclass, jint aHandle, jint aMask, jfloatArray aRay, jfloatArray aResult)
{
    jfloat* rayElems = NULL;
    if (aRay)
    {
        rayElems = aEnv->GetFloatArrayElements(aRay, NULL);
        if (rayElems == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }

    jfloat* resultElems = NULL;
    if (aResult)
    {
        resultElems = aEnv->GetFloatArrayElements(aResult, NULL);
        if (resultElems == NULL)
        {
            if (rayElems)
                aEnv->ReleaseFloatArrayElements(aRay, rayElems, JNI_ABORT);
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }

    M3G_BEGIN_PROFILE(M3G_PROFILE_PICK);
    M3G_DO_LOCK
    jint ret = (jint)m3gPick3D((M3GGroup)aHandle, aMask, (jfloat*)rayElems, (jfloat*)resultElems);
    M3G_DO_UNLOCK(aEnv)
    M3G_END_PROFILE(M3G_PROFILE_PICK);

    if (resultElems)
        aEnv->ReleaseFloatArrayElements(aResult, resultElems, 0);
    if (rayElems)
        aEnv->ReleaseFloatArrayElements(aRay, rayElems, 0);
    return ret;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Group__1getChildCount
(JNIEnv* aEnv, jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint count =(jint)m3gGetChildCount((M3GGroup)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return count;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Group__1removeChild
(JNIEnv* aEnv, jclass, jint aHandle, jint aHNode)
{
    M3G_DO_LOCK
    m3gRemoveChild((M3GGroup)aHandle, (M3GNode)aHNode);
    M3G_DO_UNLOCK(aEnv)
}
