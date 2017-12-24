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

#include "javax_microedition_m3g_Background.h"

JNIEXPORT jboolean JNICALL Java_javax_microedition_m3g_Background__1isEnabled
(JNIEnv* aEnv,   jclass, jint aHandle, jint aWhich)
{
    M3G_DO_LOCK
    jboolean enabled = (jboolean)m3gIsBgEnabled((M3GBackground)aHandle, aWhich);
    M3G_DO_UNLOCK(aEnv)
    return enabled;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Background__1setCrop
(JNIEnv* aEnv,   jclass, jint aHandle, jint aCropX, jint aCropY, jint aWidth, jint aHeight)
{
    M3G_DO_LOCK
    m3gSetBgCrop((M3GBackground)aHandle, (M3Gint)aCropX, (M3Gint)aCropY, (M3Gint)aWidth, (M3Gint)aHeight);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Background__1setImageMode
(JNIEnv* aEnv,   jclass, jint aHandle, jint aModeX, jint aModeY)
{
    M3G_DO_LOCK
    m3gSetBgMode((M3GBackground)aHandle, aModeX, aModeY);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Background__1ctor
(JNIEnv* aEnv,   jclass, jint aM3g)
{
    M3G_DO_LOCK
    jint handle = (jint)m3gCreateBackground((M3GInterface)aM3g);
    M3G_DO_UNLOCK(aEnv)
    return handle;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Background__1getCrop
(JNIEnv* aEnv,   jclass, jint aHandle, jint aWhich)
{
    M3G_DO_LOCK
    jint crop = (jint)m3gGetBgCrop((M3GBackground)aHandle, aWhich);
    M3G_DO_UNLOCK(aEnv)
    return crop;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Background__1getImageMode
(JNIEnv* aEnv,   jclass, jint aHandle, jint aWhich)
{
    M3G_DO_LOCK
    jint mode = (jint)m3gGetBgMode((M3GBackground)aHandle, aWhich);
    M3G_DO_UNLOCK(aEnv)
    return mode;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Background__1setImage
(JNIEnv* aEnv,   jclass, jint aHandle, jint aHImage)
{
    M3G_DO_LOCK
    m3gSetBgImage((M3GBackground)aHandle, (M3GImage)aHImage);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Background__1setColor
(JNIEnv* aEnv,   jclass, jint aHandle, jint aARGB)
{
    M3G_DO_LOCK
    m3gSetBgColor((M3GBackground)aHandle, aARGB);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Background__1enable
(JNIEnv* aEnv,   jclass, jint aHandle, jint aWhich, jboolean aEnable)
{
    M3G_DO_LOCK
    m3gSetBgEnable((M3GBackground)aHandle, aWhich, aEnable);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Background__1getImage
(JNIEnv* aEnv,   jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint image = (jint)m3gGetBgImage((M3GBackground)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return image;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Background__1getColor
(JNIEnv* aEnv,   jclass, jint aHandle)
{
    M3G_DO_LOCK
    jint color = (jint)m3gGetBgColor((M3GBackground)aHandle);
    M3G_DO_UNLOCK(aEnv)
    return color;
}
