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
#include "javax_microedition_m3g_Loader.h"

//#include <ezlib.h>
#include <zlib.h>

#ifndef TRUE
#define TRUE 1
#endif

#ifndef FALSE
#define FALSE 0
#endif

/*!
 * \brief Symbian implementation of the block inflation function for
 * the Loader class
 */
static M3Gsizei m3gSymbianInflateBlock1(M3Gsizei srcLength,
                                        const M3Gubyte *src,
                                        M3Gsizei dstLength,
                                        M3Gubyte *dst)
{
    unsigned long len = (unsigned long) dstLength;
    if (uncompress((Bytef *) dst, &len,
                   (const Bytef *) src, (uLong) srcLength) != Z_OK)
    {
        return 0;
    }
    return (M3Gsizei) len;
}

JNIEXPORT jboolean JNICALL Java_javax_microedition_m3g_Loader__1inflate
(JNIEnv* aEnv, jclass, jbyteArray aCompressed, jbyteArray aInflated)
{
    M3Guint result;

    M3Gubyte *compressedData = (M3Gubyte *)aEnv->GetByteArrayElements(aCompressed, NULL);
    if (compressedData == NULL)
    {
        M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
        return 0;
    }
    M3Gint compressedLength = aEnv->GetArrayLength(aCompressed);
    M3Gubyte *inflatedData = (M3Gubyte *)aEnv->GetByteArrayElements(aInflated, NULL);
    if (inflatedData == NULL)
    {
        if (compressedData)
            aEnv->ReleaseByteArrayElements(aCompressed, (jbyte*)compressedData, JNI_ABORT);
        M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
        return 0;
    }
    M3Gint inflatedLength = aEnv->GetArrayLength(aInflated);

    M3G_DO_LOCK
    if (m3gSymbianInflateBlock1(compressedLength, compressedData,
                                inflatedLength, inflatedData))
    {
        result = TRUE;
    }
    else
    {
        result = FALSE;
    }
    M3G_DO_UNLOCK(aEnv)

    if (aCompressed)
    {
        aEnv->ReleaseByteArrayElements(aCompressed, (jbyte*)compressedData, JNI_ABORT);
    }

    if (aInflated)
    {
        aEnv->ReleaseByteArrayElements(aInflated, (jbyte*)inflatedData, 0);
    }

    return result;
}


JNIEXPORT jlong JNICALL Java_javax_microedition_m3g_Loader__1ctor
(JNIEnv* aEnv, jclass, jlong aM3g)
{
    M3G_DO_LOCK
    M3GLoader loader = (M3GLoader)m3gCreateLoader((M3GInterface)aM3g);
    M3G_DO_UNLOCK(aEnv)
    return (jlong)loader;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Loader__1decodeData
(JNIEnv* aEnv, jclass, jlong aLoader, jint aOffset, jbyteArray aDataArray)
{
    /* null array is never passed */
    M3Gubyte *data = (M3Gubyte *)aEnv->GetByteArrayElements(aDataArray, NULL);
    if (data == NULL)
    {
        M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
        return 0;
    }

    M3Gint bytes = aEnv->GetArrayLength(aDataArray);
    M3GLoader loader = (M3GLoader)aLoader;

    M3G_DO_LOCK
    jint retVal = m3gDecodeData(loader, bytes, data + aOffset);
    M3G_DO_UNLOCK(aEnv)

    if (aDataArray)
    {
        aEnv->ReleaseByteArrayElements(aDataArray, (jbyte*)data, JNI_ABORT);
    }

    return retVal;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Loader__1getLoadedObjects
(JNIEnv* aEnv, jclass, jlong aLoader, jlongArray aObjectArray)
{
    M3Gulong *objects = NULL;
    if (aObjectArray)
    {
        objects = (M3Gulong *)aEnv->GetLongArrayElements(aObjectArray, NULL);
        if (objects == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }

    M3GLoader loader = (M3GLoader)aLoader;

    M3G_DO_LOCK
    jint retVal = m3gGetLoadedObjects(loader, objects);
    M3G_DO_UNLOCK(aEnv)

    if (aObjectArray)
    {
        aEnv->ReleaseLongArrayElements(aObjectArray, (jlong*)objects, 0);
    }

    return retVal;
}

JNIEXPORT void JNICALL Java_javax_microedition_m3g_Loader__1setExternalReferences
(JNIEnv* aEnv, jclass, jlong aLoader, jlongArray aObjectArray)
{
    /* null array is never passed */
    M3Gulong *xRefs = (M3Gulong *)aEnv->GetLongArrayElements(aObjectArray, NULL);
    if (xRefs == NULL)
    {
        M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
        return;
    }

    M3Gint numXRefs = aEnv->GetArrayLength(aObjectArray);
    M3GLoader loader = (M3GLoader)aLoader;

    M3G_DO_LOCK
    m3gImportObjects(loader, numXRefs, xRefs);
    M3G_DO_UNLOCK(aEnv)

    aEnv->ReleaseLongArrayElements(aObjectArray, (jlong*)xRefs, JNI_ABORT);
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Loader__1getObjectsWithUserParameters
(JNIEnv* aEnv, jclass, jlong aLoader, jlongArray aObjectArray)
{
    M3Gulong *objects = NULL;
    if (aObjectArray)
    {
        objects = (M3Gulong *)aEnv->GetLongArrayElements(aObjectArray, NULL);
        if (objects == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }

    M3GLoader loader = (M3GLoader)aLoader;

    M3G_DO_LOCK
    jint retVal = m3gGetObjectsWithUserParameters(loader, objects);
    M3G_DO_UNLOCK(aEnv)

    if (objects)
    {
        aEnv->ReleaseLongArrayElements(aObjectArray, (jlong*)objects, 0);
    }

    return retVal;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Loader__1getNumUserParameters
(JNIEnv* aEnv, jclass, jlong aLoader, jint aObj)
{
    M3GLoader loader = (M3GLoader)aLoader;
    M3G_DO_LOCK
    jint numParams = (jint)m3gGetNumUserParameters(loader, aObj);
    M3G_DO_UNLOCK(aEnv)
    return numParams;
}

JNIEXPORT jint JNICALL Java_javax_microedition_m3g_Loader__1getUserParameter
(JNIEnv* aEnv, jclass, jlong aLoader, jint aObj, jint aIndex, jbyteArray aDataArray)
{
    M3Gbyte *data = NULL;
    if (aDataArray)
    {
        data = aEnv->GetByteArrayElements(aDataArray, NULL);
        if (data == NULL)
        {
            M3G_RAISE_EXCEPTION(aEnv, "java/lang/OutOfMemoryError");
            return 0;
        }
    }

    M3GLoader loader = (M3GLoader)aLoader;
    M3G_DO_LOCK
    jint retVal = m3gGetUserParameter(loader, aObj, aIndex, data);
    M3G_DO_UNLOCK(aEnv)

    if (data)
    {
        aEnv->ReleaseByteArrayElements(aDataArray, data, 0);
    }

    return retVal;
}
