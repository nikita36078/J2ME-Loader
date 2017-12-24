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

#include "javax_microedition_m3g_Material.h"

JNIEXPORT jboolean JNICALL Java_javax_microedition_m3g_Material__1isVertexColorTrackingEnabled
(JNIEnv* aEnv, jclass, jint aHMaterial)
{
    M3G_DO_LOCK
    jboolean enabled = (jboolean)m3gIsVertexColorTrackingEnabled((M3GMaterial)aHMaterial);
    M3G_DO_UNLOCK(aEnv)
    return enabled;
}

JNIEXPORT jfloat JNICALL Java_javax_microedition_m3g_Material__1getShininess
(JNIEnv* aEnv, jclass, jint aHMaterial)
{
    M3G_DO_LOCK
    jfloat shininess = (jfloat)m3gGetShininess((M3GMaterial)aHMaterial);
    M3G_DO_UNLOCK(aEnv)
    return shininess;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Material__1setVertexColorTrackingEnable
(JNIEnv* aEnv, jclass, jint aHMaterial, jboolean aEnable)
{
    M3G_DO_LOCK
    m3gSetVertexColorTrackingEnable((M3GMaterial)aHMaterial, (M3Gbool)aEnable);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Material__1ctor
(JNIEnv* aEnv, jclass, jint aM3g)
{
    M3G_DO_LOCK
    M3GMaterial material = (M3GMaterial)m3gCreateMaterial((M3GInterface)aM3g);
    M3G_DO_UNLOCK(aEnv)
    return (M3Guint)material;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Material__1setColor
(JNIEnv* aEnv, jclass, jint aHMaterial, jint aTarget, jint aARGB)
{
    M3G_DO_LOCK
    m3gSetColor((M3GMaterial)aHMaterial, aTarget, aARGB);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Material__1setShininess
(JNIEnv* aEnv, jclass, jint aHMaterial, jfloat aShininess)
{
    M3G_DO_LOCK
    m3gSetShininess((M3GMaterial)aHMaterial, (M3Gfloat)aShininess);
    M3G_DO_UNLOCK(aEnv)
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Material__1getColor
(JNIEnv* aEnv, jclass, jint aHMaterial, jint aTarget)
{
    M3G_DO_LOCK
    jint color = (jint)m3gGetColor((M3GMaterial)aHMaterial, aTarget);
    M3G_DO_UNLOCK(aEnv)
    return color;
}
