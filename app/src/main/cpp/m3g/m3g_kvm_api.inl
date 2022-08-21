/*
* Copyright (c) 2003 Nokia Corporation and/or its subsidiary(-ies).
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


/*!
 * \file \brief Java bindings for the K Virtual Machine native API
 *
 */



#ifndef M3G_JAVA_INCLUDE
#   error included by m3g_<platform>_java_api.c; do not compile separately.
#endif

/*!
 * \brief Macros for dispatching exceptions to vm.
 *
 */
#include <jni.h>
#define M3G_RAISE_EXCEPTION(aEnv, aException){\
                 if (aEnv != NULL){\
                jclass jException = aEnv->FindClass(aException);\
                if (jException != NULL){\
                    aEnv->ThrowNew(jException, NULL);\
         }}}\
 

/*!
 * \brief Macros for serializing m3gcore function calls 
 * in native threading environment.
 */
#include "CSynchronization.hpp"
#define M3G_DO_LOCK CSynchronization::InstanceL()->Lock();
#define M3G_DO_UNLOCK(aEnv) {\
                    int errorCode = CSynchronization::InstanceL()->GetErrorCode();\
                    if ( errorCode != 0){\
                            M3G_RAISE_EXCEPTION(aEnv, jsr184Exception(errorCode));\
                    }\
                    CSynchronization::InstanceL()->Unlock();\
                    }\
 

/*----------------------------------------------------------------------
 * Internal data types
 *--------------------------------------------------------------------*/



/*----------------------------------------------------------------------
 * Internal utility functions
 *--------------------------------------------------------------------*/

/*!
 * \brief Checks that a Java array is non-null and meets a minimum
 * length requirement
 *
 * Throws NullPointerException or IllegalArgumentException if
 * constraints violated.
 */
static M3Gbool validateArray(JNIEnv* aEnv, const jbyteArray aArray, M3Gsizei aMinLength)
{
    if (aArray == NULL)
    {
        M3G_RAISE_EXCEPTION(aEnv, "java/lang/NullPointerException");
        return M3G_FALSE;
    }

    if (aEnv->GetArrayLength(aArray) < aMinLength)
    {
        M3G_RAISE_EXCEPTION(aEnv, "java/lang/IllegalArgumentException");
        return M3G_FALSE;
    }

    return M3G_TRUE;
}

#if __SIZEOF_POINTER__ == 8
typedef jlong java_ptr;
#else
typedef jint java_ptr;
#endif

#include "m3g_jsr184.inl"
#include "jni/triangleStripArray.inl"
#include "jni/vertexBuffer.inl"
#include "jni/fog.inl"
#include "jni/polygonMode.inl"
#include "jni/object3d.inl"
#include "jni/node.inl"
#include "jni/group.inl"
#include "jni/skinnedMesh.inl"
#include "jni/camera.inl"
#include "jni/vertexArray.inl"
#include "jni/transform.inl"
#include "jni/graphics3d.inl"
#include "jni/platform.inl"
#include "jni/compositingMode.inl"
#include "jni/world.inl"
#include "jni/material.inl"
#include "jni/keyframeSequence.inl"
#include "jni/sprite3d.inl"
#include "jni/mesh.inl"
#include "jni/animationTrack.inl"
#include "jni/texture2d.inl"
#include "jni/morphingMesh.inl"
#include "jni/transformable.inl"
#include "jni/background.inl"
#include "jni/image2d.inl"
#include "jni/appearance.inl"
#include "jni/light.inl"
#include "jni/animationController.inl"
#include "jni/interface.inl"
#include "jni/loader.inl"

