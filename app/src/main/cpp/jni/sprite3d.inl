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

#include "javax_microedition_m3g_Sprite3D.h"

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Sprite3D__1setImage
(JNIEnv* aEnv, jclass, jlong aHandle, jlong aHImage)
{
    M3G_DO_LOCK
    m3gSetSpriteImage((M3GSprite)aHandle, (M3GImage)aHImage);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jboolean JNICALL Java_javax_microedition_m3g_Sprite3D__1isScaled
(JNIEnv* aEnv, jclass, jlong aHandle)
{
    M3G_DO_LOCK
    jboolean scaled = (jboolean)m3gIsScaledSprite((M3GSprite)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return scaled;
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_Sprite3D__1getImage
(JNIEnv* aEnv, jclass, jlong aHandle)
{
    M3G_DO_LOCK
    jlong image = (jlong)m3gGetSpriteImage((M3GSprite)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return image;
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_Sprite3D__1ctor
(JNIEnv* aEnv, jclass, jlong aM3g, jboolean aScaled, jlong aHImage, jlong aHAppearance)
{
    M3G_DO_LOCK
    jlong handle = (jlong)m3gCreateSprite((M3GInterface)aM3g, aScaled, (M3GImage)aHImage, (M3GAppearance)aHAppearance);
    M3G_DO_UNLOCK(aEnv)
    return handle;
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_Sprite3D__1getAppearance
(JNIEnv* aEnv, jclass, jlong aHandle)
{
    M3G_DO_LOCK
    jlong appearence = (jlong)m3gGetSpriteAppearance((M3GSprite)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return appearence;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Sprite3D__1getCrop
(JNIEnv* aEnv, jclass, jlong aHandle, jint aWhich)
{
    M3G_DO_LOCK
    jint crop = (jint)m3gGetCrop((M3GSprite)aHandle, aWhich);
    M3G_DO_UNLOCK(aEnv)
    return crop;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Sprite3D__1setCrop
(JNIEnv* aEnv, jclass, jlong aHandle, jint aCropX, jint aCropY, jint aWidth, jint aHeight)
{
    M3G_DO_LOCK
    m3gSetCrop((M3GSprite)aHandle, aCropX, aCropY, aWidth, aHeight);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Sprite3D__1setAppearance
(JNIEnv* aEnv, jclass, jlong aHandle, jlong aHAppearance)
{
    M3G_DO_LOCK
    m3gSetSpriteAppearance((M3GSprite)aHandle, (M3GAppearance)aHAppearance);
    M3G_DO_UNLOCK(aEnv)
}
