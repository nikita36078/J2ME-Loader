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

#include "javax_microedition_m3g_Appearance.h"

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Appearance__1setLayer
(JNIEnv* aEnv,   jclass, jlong aHApp, jint aLayer)
{
    M3G_DO_LOCK
    m3gSetLayer((M3GAppearance)aHApp, (M3Gint)aLayer);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_Appearance__1getCompositingMode
(JNIEnv* aEnv,   jclass, jlong aHApp)
{
    M3G_DO_LOCK
    jlong compMode = (jlong)m3gGetCompositingMode((M3GAppearance)aHApp);
    M3G_DO_UNLOCK(aEnv)
    return compMode;
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_Appearance__1getFog
(JNIEnv* aEnv,   jclass, jlong aHApp)
{
    M3G_DO_LOCK
    jlong fog = (jlong)m3gGetFog((M3GAppearance)aHApp);
    M3G_DO_UNLOCK(aEnv)
    return fog;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Appearance__1getLayer
(JNIEnv* aEnv,   jclass, jlong aHApp)
{
    M3G_DO_LOCK
    jint layer = (jint)m3gGetLayer((M3GAppearance)aHApp);
    M3G_DO_UNLOCK(aEnv)
    return layer;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Appearance__1setCompositingMode
(JNIEnv* aEnv,   jclass, jlong aHApp, jlong aHMode)
{
    M3G_DO_LOCK
    m3gSetCompositingMode((M3GAppearance)aHApp, (M3GCompositingMode)aHMode);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Appearance__1setPolygonMode
(JNIEnv* aEnv,   jclass, jlong aHApp, jlong aHMode)
{
    M3G_DO_LOCK
    m3gSetPolygonMode((M3GAppearance)aHApp, (M3GPolygonMode)aHMode);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_Appearance__1getPolygonMode
(JNIEnv* aEnv,   jclass, jlong aHApp)
{
    M3G_DO_LOCK
    jlong polyMode = (jlong)m3gGetPolygonMode((M3GAppearance)aHApp);
    M3G_DO_UNLOCK(aEnv)
    return polyMode;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Appearance__1setTexture
(JNIEnv* aEnv,   jclass, jlong aHApp, jint aUnit, jlong aHTex)
{
    M3G_DO_LOCK
    m3gSetTexture((M3GAppearance)aHApp, (M3Gint)aUnit, (M3GTexture)aHTex);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_Appearance__1ctor
(JNIEnv* aEnv,   jclass, jlong aM3g)
{
    M3G_DO_LOCK
    jlong handle = (jlong)m3gCreateAppearance((M3GInterface)aM3g);
    M3G_DO_UNLOCK(aEnv)
    return handle;
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_Appearance__1getTexture
(JNIEnv* aEnv,   jclass, jlong aHApp, jint aUnit)
{
    M3G_DO_LOCK
    jlong texture = (jlong)m3gGetTexture((M3GAppearance)aHApp, (M3Gint)aUnit);
    M3G_DO_UNLOCK(aEnv)
    return texture;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Appearance__1setFog
(JNIEnv* aEnv,   jclass, jlong aHApp, jlong aHFog)
{
    M3G_DO_LOCK
    m3gSetFog((M3GAppearance)aHApp, (M3GFog)aHFog);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Appearance__1setMaterial
(JNIEnv* aEnv,   jclass, jlong aHApp, jlong aHMaterial)
{
    M3G_DO_LOCK
    m3gSetMaterial((M3GAppearance)aHApp, (M3GMaterial)aHMaterial);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_Appearance__1getMaterial
(JNIEnv* aEnv,   jclass, jlong aHApp)
{
    M3G_DO_LOCK
    jlong material = (jlong)m3gGetMaterial((M3GAppearance)aHApp);
    M3G_DO_UNLOCK(aEnv)
    return material;
}
