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
#include "javax_microedition_m3g_Platform.h"

/*
 * Must be excuted in UI thread
 */
JNIEXPORT void JNICALL Java_javax_microedition_m3g_Platform_finalizeInterface
(JNIEnv* aEnv, jclass, jlong aHObj)
{
    M3G_DO_LOCK
    m3gDeleteInterface((M3GInterface)aHObj);
    M3G_DO_UNLOCK(aEnv)
}

/*
 * Must be excuted in UI thread
 */
JNIEXPORT void JNICALL Java_javax_microedition_m3g_Platform__1finalizeObject
(JNIEnv* aEnv, jclass, jlong aHObj)
{
    M3G_DO_LOCK
    m3gDeleteObject((M3GObject)aHObj);
    M3G_DO_UNLOCK(aEnv)
}
