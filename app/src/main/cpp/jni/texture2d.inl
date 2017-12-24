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
#include "javax_microedition_m3g_Texture2D.h"

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Texture2D__1getBlending
(JNIEnv* aEnv, jclass, jint aHTexture2D)
{
    M3G_DO_LOCK
    jint blending = (jint)m3gTextureGetBlending((M3GTexture)aHTexture2D);
    M3G_DO_UNLOCK(aEnv)
    return blending;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Texture2D__1getWrappingT
(JNIEnv* aEnv, jclass, jint aHTexture2D)
{
    M3G_DO_LOCK
    jint wrapping = (jint)m3gGetWrappingT((M3GTexture)aHTexture2D);
    M3G_DO_UNLOCK(aEnv)
    return wrapping;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Texture2D__1getWrappingS
(JNIEnv* aEnv, jclass, jint aHTexture2D)
{
    M3G_DO_LOCK
    jint wrapping = (jint)m3gGetWrappingS((M3GTexture)aHTexture2D);
    M3G_DO_UNLOCK(aEnv)
    return wrapping;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Texture2D__1setFiltering
(JNIEnv* aEnv, jclass, jint aHTexture2D, jint aLevelFilter, jint aImageFilter)
{
    M3G_DO_LOCK
    m3gSetFiltering((M3GTexture)aHTexture2D, aLevelFilter, aImageFilter);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Texture2D__1getBlendColor
(JNIEnv* aEnv, jclass, jint aHTexture2D)
{
    M3G_DO_LOCK
    jint color = (jint)m3gGetBlendColor((M3GTexture)aHTexture2D);
    M3G_DO_UNLOCK(aEnv)
    return color;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Texture2D__1setBlendColor
(JNIEnv* aEnv, jclass, jint aHTexture2D, jint aRGB)
{
    M3G_DO_LOCK
    m3gSetBlendColor((M3GTexture)aHTexture2D, aRGB);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Texture2D__1ctor
(JNIEnv* aEnv, jclass, jint aM3g, jint aHImage)
{
    M3G_DO_LOCK
    jint handle = (M3Guint)m3gCreateTexture((M3GInterface)aM3g, (M3GImage)aHImage);
    M3G_DO_UNLOCK(aEnv)
    return handle;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Texture2D__1setWrapping
(JNIEnv* aEnv, jclass, jint aHTexture2D, jint aWrapS, jint aWrapT)
{
    M3G_DO_LOCK
    m3gSetWrapping((M3GTexture)aHTexture2D, aWrapS, aWrapT);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Texture2D__1setImage
(JNIEnv* aEnv, jclass, jint aHTex, jint aHImg)
{
    M3G_DO_LOCK
    m3gSetTextureImage((M3GTexture)aHTex, (M3GImage)aHImg);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Texture2D__1setBlending
(JNIEnv* aEnv, jclass, jint aHTexture2D, jint aFunc)
{
    M3G_DO_LOCK
    m3gTextureSetBlending((M3GTexture)aHTexture2D, aFunc);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Texture2D__1getImage
(JNIEnv* aEnv, jclass, jint aHTex)
{
    M3G_DO_LOCK
    jint image = (M3Guint) m3gGetTextureImage((M3GTexture)aHTex);
    M3G_DO_UNLOCK(aEnv)
    return image;
}

/* M3G 1.1 JNI Calls */

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Texture2D__1getImageFilter
(JNIEnv* aEnv, jclass, jint aHTex)
{
    M3Gint levelFilter = 0;
    M3Gint imageFilter = 0;
    M3G_DO_LOCK
    m3gGetFiltering((M3GTexture)aHTex, &levelFilter, &imageFilter);
    M3G_DO_UNLOCK(aEnv)
    return (jint)imageFilter;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Texture2D__1getLevelFilter
(JNIEnv* aEnv, jclass, jint aHTex)
{
    M3Gint levelFilter = 0;
    M3Gint imageFilter = 0;
    M3G_DO_LOCK
    m3gGetFiltering((M3GTexture)aHTex, &levelFilter, &imageFilter);
    M3G_DO_UNLOCK(aEnv)
    return (jint)levelFilter;
}
