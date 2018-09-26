/*
* Copyright (c) 2003 Nokia Corporation and/or its subsidiary(-ies).
* All rights reserved.
* This component and the accompanying materials are made available
* under the terms of the License "Eclipse Public License v1.0"
* which accompanies this distribution, and is available
* at the URL "http://www.eclipse.org/legal/epl-v10.html".
*
* Initial Contributors:
* Nokia Corporation - initial contribution.
*
* Contributors:
*
* Description: Native implementation of the Loader class
*
*/


/*!
 * \internal
 * \file
 * \brief Native implementation of the Loader class
 *
*/

#include "m3g_object.h"
#include "m3g_array.h"

/*----------------------------------------------------------------------
 * Internal data types
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Possible global states for the loader
 */
typedef enum {
    /*! \internal \brief Loading not supported yet */
    LOADSTATE_NOT_SUPPORTED = -2,
    /*! \internal \brief Loading has terminated with an error */
    LOADSTATE_ERROR         = -1,
    /*! \internal \brief Loading has not started yet */
    LOADSTATE_INITIAL       =  0,
    /*! \internal \brief The identifier of the file is being read */
    LOADSTATE_IDENTIFIER,
    /*! \internal \brief The header of the section is being read */
    LOADSTATE_SECTION,
    /*! \internal \brief The header field of an object is being read */
    LOADSTATE_OBJECT,
    /*! \internal \brief Loading is finished */
    LOADSTATE_DONE
} LoaderState;

/*!
 * \internal
 * \brief Possible local states for the loader
 */
typedef enum {
    /*! \internal \brief Local state is entered */
    LOADSTATE_ENTER,
    /*! \internal \brief Local state is exited */
    LOADSTATE_EXIT,
    /*! \internal \brief Local state is section checksum */
    LOADSTATE_CHECKSUM
} LoaderLocalState;

/*!
 * \internal
 * \brief Buffered byte stream class
 */
typedef struct
{
    M3Gubyte *allocatedData;
    M3Gubyte *data;
    M3Gsizei capacity, bytesAvailable, totalBytes;
} BufferedStream;

/*!
 * \internal
 * \brief User data for a loaded object
 */
typedef struct
{
    M3GObject object;
    M3Gint numParams;
    M3Gbyte **params;
    M3Gsizei *paramLengths;
    M3Gint *paramId;
} UserData;

/*!
 * \internal
 * \brief Loader instance data
 */
typedef struct M3GLoaderImpl
{
    Object object;

    BufferedStream stream;
    M3Gsizei bytesRequired;
    M3Gsizei sectionBytesRequired;

    PointerArray refArray;
    PointerArray userDataArray;

    /*!
     * \internal
     * \brief The global state the loader is in
     *
     * This is a rather ordinary state machine thing; basically the
     * type of object being loaded, or one of the possible error
     * conditions. In here, it also amounts to a particular coroutine
     * execution context.
     */
    LoaderState state;

    /*!
     * \internal
     * \brief The local state of the loader
     *
     * This is basically the line number within a particular coroutine
     * function.
     */
    M3Gint localState;

    /*!
     * \internal
     * \brief Object being loaded
     */
    M3Gint objectType;

    /*!
     * \internal
     * \brief Loaded object
     */
    M3GObject loadedObject;

    /*!
     * \internal
     * \brief Pointer to the beginning of an object
     */
    M3Gubyte *objectData;

    /*!
     * \internal
     * \brief Pointer to the end of an object
     */
    M3Gubyte *objectDataEnd;

    /*!
     * \internal
     * \brief Pointer to the context data for the current coroutine
     * context
     */
    M3Gubyte *localData;

    /*!
     * \internal
     * \brief Size of the current coroutine data
     *
     * This is grown dynamically as necessary, rather than trying to
     * maintain a single size that fits all coroutines.
     */
    size_t localDataSize;

    /* File information */
    M3Gbool hasReferences;
    M3Gsizei fileSize;
    M3Gsizei contentSize;
    M3Gint triCount;
    M3Gint triConstraint;

    /* Section information */
    M3Gbool compressed;
    M3Gint sectionLength;
    M3Gint sectionNum;
    M3Gint inflatedLength;
    M3Gubyte *sectionData;
    M3Gubyte *allocatedSectionData;
    
    /* Adler data */
    M3Gint S12[2];
} Loader;

typedef struct {
    const unsigned char *data;
    int read;
    int length;
} compressedData;

/* Type ID used for classifying objects derived from Node */
#define ANY_NODE_CLASS ((M3GClass)(-1))

#include <string.h>
#define m3gCmp(s1, s2, len)  memcmp(s1, s2, len)

/*----------------------------------------------------------------------
 * Private constants
 *--------------------------------------------------------------------*/

#define M3G_MIN_OBJECT_SIZE     (1 + 4)
#define M3G_MIN_SECTION_SIZE    (1 + 4 + 4)
#define M3G_CHECKSUM_SIZE       4

#define M3G_ADLER_CONST         65521;

static const M3Gubyte M3G_FILE_IDENTIFIER[] = {
    0xAB, 0x4A, 0x53, 0x52, 0x31, 0x38, 0x34, 0xBB, 0x0D, 0x0A, 0x1A, 0x0A
};

static const M3Gubyte PNG_FILE_IDENTIFIER[] = {
    0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
};

static void m3gCleanupLoader(M3GLoader loader);

/*----------------------------------------------------------------------
 * Platform-specific "inflate" decompression code
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Decompresses a block of data into an output buffer
 *
 * \param srcLength number of bytes in the input (compressed) buffer
 * \param src       pointer to the input buffer
 * \param dstLength number of bytes allocated in the output buffer
 * \param dst       pointer to the output buffer
 * \return the number of bytes written to \c dst
 */
static M3Gsizei m3gInflateBlock(M3Gsizei srcLength, const M3Gubyte *src,
                                M3Gsizei dstLength, M3Gubyte *dst);
                           
/* Include the platform-dependent implementation */
#include "m3g_loader_inflate.inl"

/*----------------------------------------------------------------------
 * Private functions
 *--------------------------------------------------------------------*/

/*!
 * \internal
 * \brief Destructor
 */
static void m3gDestroyLoader(Object *obj)
{
    Loader* loader = (Loader *) obj;
    M3G_VALIDATE_OBJECT(loader);
    {
        Interface *m3g = M3G_INTERFACE(loader);
        M3Gint n, i;

        m3gCleanupLoader(loader);
        m3gDestroyArray(&loader->refArray, m3g);
        n = m3gArraySize(&loader->userDataArray);
        for (i = 0; i < n; ++i)
        {
            UserData *data = (UserData *)m3gGetArrayElement(&loader->userDataArray, i);
            m3gFree(m3g, data->params);
            m3gFree(m3g, data->paramLengths);
            m3gFree(m3g, data->paramId);
            m3gFree(m3g, data);
        }
        m3gDestroyArray(&loader->userDataArray, m3g);
        m3gFree(m3g, loader->stream.allocatedData);
        m3gFree(m3g, loader->allocatedSectionData);
    }
    m3gDestroyObject(obj);
}

/*!
 * \internal
 * \brief Stores new data in the stream buffer of this loader
 */
static M3Gbool m3gBufferData( M3GInterface m3g,
                           BufferedStream *stream,
                           M3Gsizei bytes,
                           const M3Gubyte *data)
{
    M3Gsizei used;

    /* Allocate initial buffer */
    if (stream->allocatedData == NULL) {
        stream->capacity = bytes + 512;
        stream->allocatedData = m3gAllocZ(m3g, stream->capacity);
        if (!stream->allocatedData) {
            return M3G_FALSE;
        }
        stream->data = stream->allocatedData;
        stream->bytesAvailable = 0;
        stream->totalBytes = 0;
    }

    /* First skip used bytes */
    used = stream->data - stream->allocatedData;
    if (used > 0) {
        m3gMove(stream->allocatedData, stream->data, stream->bytesAvailable);
        stream->data = stream->allocatedData;
    }

    /* Check if new data fits in current buffer */
    if ((stream->capacity - stream->bytesAvailable) < bytes) {
        M3Gubyte *newData;
        stream->capacity = stream->capacity + bytes + 512;
        newData = m3gAllocZ(m3g, stream->capacity);
        if (!newData) {
            m3gFree(m3g, stream->allocatedData);
            stream->allocatedData = NULL;
            return M3G_FALSE;
        }
        m3gCopy(newData, stream->data, stream->bytesAvailable);
        m3gFree(m3g, stream->allocatedData);
        stream->allocatedData = newData;
        stream->data = stream->allocatedData;
    }

    m3gCopy(stream->data + stream->bytesAvailable, data, bytes);
    stream->bytesAvailable += bytes;

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Resets buffered data
 */
static void m3gResetBufferedData(BufferedStream *stream)
{
    stream->bytesAvailable = 0;
    stream->data = stream->allocatedData;
    stream->totalBytes = 0;
}

/*!
 * \internal
 * \brief Gets buffered data pointer
 */
static M3Gubyte *m3gGetBufferedDataPtr(BufferedStream *stream, M3Gint length)
{
    if (stream->bytesAvailable >= length) {
        return stream->data;
    }
    else {
        return NULL;
    }
}

/*!
 * \internal
 * \brief Advances buffered data pointer
 */
static void m3gAdvanceBufferedData(BufferedStream *stream, M3Gint length)
{
    stream->data += length;
    stream->bytesAvailable -= length;
    stream->totalBytes += length;
}

/*!
 * \internal
 * \brief Verify a boolean
 */
static M3Gbool m3gVerifyBool(M3Gubyte *data)
{
    return (*data == 0 || *data == 1);
}

/*!
 * \internal
 * \brief Loads ARGB color from data array
 */
static M3Guint m3gLoadARGB(M3Gubyte *data)
{
    M3Guint v = data[3];
    v <<= 8;
    v |=  data[0];
    v <<= 8;
    v |=  data[1];
    v <<= 8;
    v |=  data[2];

    return v;
}

/*!
 * \internal
 * \brief Loads RGB color from data array
 */
static M3Guint m3gLoadRGB(M3Gubyte *data)
{
    M3Guint v = data[0];
    v <<= 8;
    v |=  data[1];
    v <<= 8;
    v |=  data[2];

    return v;
}

/*!
 * \internal
 * \brief Loads short from data array
 */
static M3Gshort m3gLoadShort(M3Gubyte *data)
{
    M3Gshort v = data[1];
    v <<= 8;
    v |=  data[0];

    return v;
}

/*!
 * \internal
 * \brief Loads integer from data array
 */
static M3Gint m3gLoadInt(M3Gubyte *data)
{
    M3Gint v = data[3];
    v <<= 8;
    v |=  data[2];
    v <<= 8;
    v |=  data[1];
    v <<= 8;
    v |=  data[0];

    return v;
}

/*!
 * \internal
 * \brief Loads integer from data array
 */
static M3Gbool m3gLoadFloat(M3Gubyte *data, M3Gfloat *res)
{
    M3Guint v = data[3];
    v <<= 8;
    v |=  data[2];
    v <<= 8;
    v |=  data[1];
    v <<= 8;
    v |=  data[0];

    *res = (*(M3Gfloat*)&(v));
    if ((v & 0x7f800000) ==  0x7f800000 ||
        v == 0x80000000 || // negative zero
        ((v & 0x007FFFFF ) != 0 && ( v & 0x7F800000 ) == 0))
        return M3G_FALSE;
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads 4 * 4 matrix from data array
 */
static M3Gbool m3gLoadMatrix(Matrix *m, M3Gubyte *data)
{
    M3Gint i;
    M3Gfloat array[16];

    for (i = 0; i < 16; i++) {
        if (!m3gLoadFloat(data + 4 * i, &array[i]))
            return M3G_FALSE;
    }

    m3gSetMatrixRows(m, array);
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Inflates a section
 */
static M3Gubyte *m3gInflateSection(Loader *loader,
                                   M3Gubyte *compressed,
                                   M3Gint cLength, M3Gint iLength)
{
    M3Gubyte *inflated = m3gAllocZ(M3G_INTERFACE(loader), iLength);
    if (inflated && !m3gInflateBlock(cLength, compressed, iLength, inflated)) {
        m3gFree(M3G_INTERFACE(loader), inflated);
        return NULL;
    }

    return inflated;
}

/*!
 * \internal
 * \brief Loads file identifier
 */
static LoaderState m3gLoadIdentifier(Loader *loader)
{
    M3Gubyte *data = m3gGetBufferedDataPtr(&loader->stream, loader->bytesRequired);

    if (loader->localState == LOADSTATE_ENTER) {
        if (m3gCmp(data, PNG_FILE_IDENTIFIER, sizeof(PNG_FILE_IDENTIFIER)) == 0) {
            m3gAdvanceBufferedData(&loader->stream, loader->bytesRequired);
            return LOADSTATE_NOT_SUPPORTED;
        }
        else {
            loader->localState = LOADSTATE_EXIT;
            loader->bytesRequired = sizeof(M3G_FILE_IDENTIFIER);
            return LOADSTATE_IDENTIFIER;
        }
    }
    else {
        if (m3gCmp(data, M3G_FILE_IDENTIFIER, sizeof(M3G_FILE_IDENTIFIER)) == 0) {
            m3gAdvanceBufferedData(&loader->stream, loader->bytesRequired);
            loader->localState = LOADSTATE_ENTER;
            loader->bytesRequired = M3G_MIN_SECTION_SIZE;
            return LOADSTATE_SECTION;
        }

        loader->bytesRequired = 0;
        return LOADSTATE_ERROR;
    }
}

/*!
 * \internal
 * \brief Adler helper functions
 */

static void m3gInitAdler(M3Gint *S12)
{
    S12[0] = 1; S12[1] = 0;
}

static void m3gUpdateAdler(M3Gint *S12, M3Gubyte *data, M3Gint length)
{
    int i;
    for (i = 0; i < length; i++) {
        S12[0] = (S12[0] + data[i]) % M3G_ADLER_CONST;
        S12[1] = (S12[1] + S12[0])  % M3G_ADLER_CONST;
    }
}

static M3Gint m3gGetAdler(M3Gint *S12)
{
    return (S12[1] << 16) | S12[0];
}

/*!
 * \internal
 * \brief Loads a section
 */
static LoaderState m3gLoadSection(Loader *loader)
{
    M3Gubyte *data = m3gGetBufferedDataPtr(&loader->stream, loader->bytesRequired);

    if (data == NULL) {
        m3gRaiseError(M3G_INTERFACE(loader), M3G_IO_ERROR);
        return LOADSTATE_ERROR;
    }

    switch(loader->localState) {
    case LOADSTATE_ENTER:
        m3gAdvanceBufferedData(&loader->stream, loader->bytesRequired);
        m3gInitAdler(loader->S12);
        m3gUpdateAdler(loader->S12, data, loader->bytesRequired);

        if (*data > 1)
        {
            m3gRaiseError(M3G_INTERFACE(loader), M3G_IO_ERROR);
            return LOADSTATE_ERROR;
        }
        loader->compressed = data[0];
        loader->sectionLength = m3gLoadInt(data + 1);
        loader->inflatedLength = m3gLoadInt(data + 5);

        loader->localState = LOADSTATE_EXIT;
        loader->bytesRequired = loader->sectionLength - loader->bytesRequired;
        if (!loader->compressed && loader->inflatedLength != (loader->bytesRequired - M3G_CHECKSUM_SIZE))
        {
            m3gRaiseError(M3G_INTERFACE(loader), M3G_IO_ERROR);
            return LOADSTATE_ERROR;
        }

        loader->sectionNum++;

        /* Special case for empty sections */
        if (loader->bytesRequired == M3G_CHECKSUM_SIZE) {
            loader->localData = data + loader->sectionLength - M3G_CHECKSUM_SIZE;
            loader->sectionData = loader->localData;
            loader->compressed = M3G_FALSE;
            loader->localState = LOADSTATE_CHECKSUM;
        }
        return LOADSTATE_SECTION;

    case LOADSTATE_EXIT:
    default:
        m3gUpdateAdler(loader->S12, data, loader->bytesRequired - M3G_CHECKSUM_SIZE);

        if (loader->compressed) {
            if (loader->inflatedLength > 0) {
                m3gFree(M3G_INTERFACE(loader), loader->allocatedSectionData);
                loader->sectionData = m3gInflateSection(loader, data, loader->bytesRequired, loader->inflatedLength);
                loader->allocatedSectionData = loader->sectionData;

                if (!loader->sectionData) {
                    if (m3gErrorRaised(M3G_INTERFACE(loader)) == M3G_NO_ERROR)
                        m3gRaiseError(M3G_INTERFACE(loader), M3G_IO_ERROR);
                    return LOADSTATE_ERROR;
                }
            }
            else {
                loader->sectionData = NULL;
            }
        }
        else {
            loader->sectionData = data;
        }

        loader->localData = loader->sectionData;
        loader->sectionBytesRequired = M3G_MIN_OBJECT_SIZE;
        loader->localState = LOADSTATE_ENTER;
        return LOADSTATE_OBJECT;

    case LOADSTATE_CHECKSUM:
        if (loader->localData != loader->sectionData + loader->inflatedLength || /* Length */
            m3gLoadInt(data + loader->bytesRequired - M3G_CHECKSUM_SIZE) != m3gGetAdler(loader->S12))
        {
            m3gRaiseError(M3G_INTERFACE(loader), M3G_IO_ERROR);
            m3gFree(M3G_INTERFACE(loader), loader->allocatedSectionData);
            loader->allocatedSectionData = NULL;
            return LOADSTATE_ERROR;
        }
        m3gAdvanceBufferedData(&loader->stream, loader->bytesRequired);

        m3gFree(M3G_INTERFACE(loader), loader->allocatedSectionData);
        loader->allocatedSectionData = NULL;

        loader->localState = LOADSTATE_ENTER;
        loader->bytesRequired = M3G_MIN_SECTION_SIZE;
        return LOADSTATE_SECTION;
    }
}

/*!
 * \internal
 * \brief Resets section data pointer to the beginning of an object
 */
static void m3gRewindObject(Loader *loader)
{
    loader->localData = loader->objectData;
}

/*!
 * \internal
 * \brief Resets section data pointer to the end of an object
 */
static void m3gSkipObject(Loader *loader)
{
    loader->localData = loader->objectDataEnd;
}

/*!
 * \internal
 * \brief Marks object to begin
 */
static void m3gBeginObject(Loader *loader)
{
    loader->objectData = loader->localData;
}

/*!
 * \internal
 * \brief Marks object to end
 */
static void m3gEndObject(Loader *loader)
{
    loader->objectDataEnd = loader->localData;
}

/*!
 * \internal
 * \brief Gets section data pointer
 */
static M3Gubyte *m3gGetSectionDataPtr(Loader *loader, M3Gint length)
{
    if ((loader->localData + length) <= (loader->sectionData + loader->inflatedLength)) {
        return loader->localData;
    }
    else {
        return NULL;
    }
}

/*!
 * \internal
 * \brief Advances section data pointer
 */
static void m3gAdvanceSectionData(Loader *loader, M3Gint length)
{
    loader->localData += length;
}

/*!
 * \internal
 * \brief Check length of the available section data
 */
static M3Gbool m3gCheckSectionDataLength(Loader *loader, const M3Gubyte *data, M3Gsizei length)
{
    if (data + length < data) return M3G_FALSE; /* Check for overflow */
    return ((data + length) <= (loader->sectionData + loader->inflatedLength));
}

/*!
 * \internal
 * \brief References an object in the object array
 *
 * \note Uses lowest bit of the pointer to mark a reference
 */
static void m3gReferenceLoaded(PointerArray *array, M3Gint idx)
{
    M3Gpointer ptr = (M3Gpointer)m3gGetArrayElement(array, idx);
    ptr |= 1;
    m3gSetArrayElement(array, idx, (void *)ptr);
}

/*!
 * \internal
 * \brief Gets an object in the object array and
 * returns reference status
 */
static M3GObject m3gGetLoadedPtr(PointerArray *array, M3Gint idx, M3Gbool *referenced)
{
    M3Gpointer ptr = (M3Gpointer)m3gGetArrayElement(array, idx);
    if (referenced != NULL) {
        *referenced = ptr & 1;
    }
    return (M3GObject)(ptr & (~1));
}

/*!
 * \internal
 * \brief Gets a loaded object and marks it referenced
 */
static M3GObject m3gGetLoaded(Loader *loader, M3Gint idx, M3GClass classID)
{
    M3GObject obj;
    M3GClass objClassID;
    M3Gbool isCompatible;

    if (idx == 0) return NULL;
    idx -= 2;

    if (idx < 0 || idx >= m3gArraySize(&loader->refArray)) {
        /* Error, not loaded */
        m3gRaiseError(M3G_INTERFACE(loader), M3G_IO_ERROR);
        return NULL;
    }

    obj = m3gGetLoadedPtr(&loader->refArray, idx, NULL);
    objClassID = M3G_CLASS(obj);

    /* Class type check; handle nodes as a special case */
    
    if (classID == ANY_NODE_CLASS) {
        switch (objClassID) {
        case M3G_CLASS_CAMERA:
        case M3G_CLASS_GROUP:
        case M3G_CLASS_LIGHT:
        case M3G_CLASS_MESH:
        case M3G_CLASS_MORPHING_MESH:
        case M3G_CLASS_SKINNED_MESH:
        case M3G_CLASS_SPRITE:
        case M3G_CLASS_WORLD:
            isCompatible = M3G_TRUE;
            break;
        default:
            isCompatible = M3G_FALSE;
        }
    }
    else {
        switch (classID) {
        case M3G_ABSTRACT_CLASS:
            M3G_ASSERT(M3G_FALSE);
            isCompatible = M3G_FALSE;
            break;
        case M3G_CLASS_MESH:
            isCompatible = (objClassID == M3G_CLASS_MESH)
                || (objClassID == M3G_CLASS_MORPHING_MESH)
                || (objClassID == M3G_CLASS_SKINNED_MESH);
            break;
        default:
            isCompatible = (classID == objClassID);
        }
    }

    if (!isCompatible) {
        /* Error, class mismatch */
        m3gRaiseError(M3G_INTERFACE(loader), M3G_IO_ERROR);
        return NULL;
    }

    /* Mark object as referenced */
    m3gReferenceLoaded(&loader->refArray, idx);
    return obj;
}

/*!
 * \internal
 * \brief Loads Object3D data
 */
static M3Gbool m3gLoadObject3DData(Loader *loader, M3GObject obj)
{
    M3Guint animTracks, i, userParams, paramId, paramLength;
    UserData *userData = NULL;
    M3Gubyte *data = m3gGetSectionDataPtr(loader, 8);
    if (data == NULL) return M3G_FALSE;

    m3gSetUserID(obj, m3gLoadInt(data));
    data += 4;

    animTracks = m3gLoadInt(data);
    data += 4;

    /* Overflow? */
    if (animTracks >= 0x1fffffff) {
        return M3G_FALSE;
    }

    if (m3gCheckSectionDataLength(loader, data, animTracks * 4 + 4) == M3G_FALSE) return M3G_FALSE;
    for (i = 0; i < animTracks; i++) {
        M3GAnimationTrack at = (M3GAnimationTrack)m3gGetLoaded(loader, m3gLoadInt(data),M3G_CLASS_ANIMATION_TRACK);
        if (at == NULL || m3gAddAnimationTrack(obj, at) == -1) {
            return M3G_FALSE;
        }
        data += 4;
    }

    userParams = m3gLoadInt(data);
    data += 4;

    if (userParams != 0) {
        /* Overflow? */
        if (userParams >= 0x10000000) {
            return M3G_FALSE;
        }

        if (m3gCheckSectionDataLength(loader, data, userParams * 8) == M3G_FALSE)
            return M3G_FALSE; /* Check the minimum size to avoid useless allocation */
        userData = (UserData *)m3gAllocZ(M3G_INTERFACE(loader), sizeof(UserData));
        if (userData == NULL)
            return M3G_FALSE;
        userData->object = obj;
        userData->numParams = userParams;
        userData->params = (M3Gbyte **)m3gAllocZ(M3G_INTERFACE(loader), userParams*sizeof(M3Gbyte *));
        userData->paramLengths = (M3Gsizei *)m3gAlloc(M3G_INTERFACE(loader), userParams*sizeof(M3Gsizei));
        userData->paramId = (M3Gint *)m3gAlloc(M3G_INTERFACE(loader), userParams*sizeof(M3Gint));
        if (userData->params == NULL ||
            userData->paramLengths == NULL ||
            userData->paramId == NULL ||
            m3gArrayAppend(&loader->userDataArray, userData, M3G_INTERFACE(loader)) == -1) {
            m3gFree(M3G_INTERFACE(loader), userData->params);
            m3gFree(M3G_INTERFACE(loader), userData->paramLengths);
            m3gFree(M3G_INTERFACE(loader), userData->paramId);
            m3gFree(M3G_INTERFACE(loader), userData);
            return M3G_FALSE;
        }

        for (i = 0; i < userParams; i++) {
            if (m3gCheckSectionDataLength(loader, data, 8) == M3G_FALSE) return M3G_FALSE;
            paramId = m3gLoadInt(data);
            data += 4;
            paramLength = m3gLoadInt(data);
            data += 4;
            userData->paramId[i] = paramId;
            userData->paramLengths[i] = paramLength;
            if (m3gCheckSectionDataLength(loader, data, paramLength) == M3G_FALSE) return M3G_FALSE;
            userData->params[i] = (M3Gbyte *)m3gAlloc(M3G_INTERFACE(loader), paramLength*sizeof(M3Gbyte));
            if (userData->params[i] == NULL)
                return M3G_FALSE;
            m3gCopy(userData->params[i], data, paramLength);
            data += paramLength;
        }
    }

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Skips Object3D data
 */
static M3Gbool m3gSkipObject3DData(Loader *loader)
{
    M3Guint animTracks, i, userParams, paramLength;
    M3Gubyte *data = m3gGetSectionDataPtr(loader, 8);
    if (data == NULL) return M3G_FALSE;

    data += 4;
    animTracks = m3gLoadInt(data);
    data += 4;

    /* Overflow? */
    if (animTracks >= 0x1fffffff) {
        return M3G_FALSE;
    }

    if (m3gCheckSectionDataLength(loader, data, animTracks * 4 + 4) == M3G_FALSE) return M3G_FALSE;
    for (i = 0; i < animTracks; i++) {
        data += 4;
    }

    userParams = m3gLoadInt(data);
    data += 4;

    for (i = 0; i < userParams; i++) {
        if (m3gCheckSectionDataLength(loader, data, 8) == M3G_FALSE) return M3G_FALSE;
        data += 4;
        paramLength = m3gLoadInt(data);
        if (m3gCheckSectionDataLength(loader, data, paramLength) == M3G_FALSE) return M3G_FALSE;
        data += 4 + paramLength;
    }

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads transformable data
 */
static M3Gbool m3gLoadTransformableData(Loader *loader, M3GTransformable obj)
{
    M3Gfloat f1, f2, f3, f4;
    M3Gubyte *data;
    if (!m3gLoadObject3DData(loader, (M3GObject)obj)) {
        return M3G_FALSE;
    }

    data = m3gGetSectionDataPtr(loader, 1);
    if (data == NULL) return M3G_FALSE;

    if (!m3gVerifyBool(data)) return M3G_FALSE;
    /* Component transform ? */
    if (*data++) {
        if (m3gCheckSectionDataLength(loader, data, 40) == M3G_FALSE) return M3G_FALSE;
        if (!m3gLoadFloat(data + 0, &f1) ||
            !m3gLoadFloat(data + 4, &f2) ||
            !m3gLoadFloat(data + 8, &f3))
            return M3G_FALSE;
        m3gSetTranslation(obj, f1, f2, f3);
        if (!m3gLoadFloat(data + 12, &f1) ||
            !m3gLoadFloat(data + 16, &f2) ||
            !m3gLoadFloat(data + 20, &f3))
            return M3G_FALSE;
        m3gSetScale(obj, f1, f2, f3);
        if (!m3gLoadFloat(data + 24, &f1) ||
            !m3gLoadFloat(data + 28, &f2) ||
            !m3gLoadFloat(data + 32, &f3) ||
            !m3gLoadFloat(data + 36, &f4))
            return M3G_FALSE;
        m3gSetOrientation(obj, f1, f2, f3, f4);
        data += 40;
    }

    if (m3gCheckSectionDataLength(loader, data, 1) == M3G_FALSE) return M3G_FALSE;
    if (!m3gVerifyBool(data)) return M3G_FALSE;
    /* Generic transform */
    if (*data++) {
        Matrix m;
        if (m3gCheckSectionDataLength(loader, data, 64) == M3G_FALSE) return M3G_FALSE;
        if (!m3gLoadMatrix(&m, data)) return M3G_FALSE;
        m3gSetTransform(obj, &m);
        data += 64;
    }

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Skips transformable data
 */
static M3Gbool m3gSkipTransformableData(Loader *loader)
{
    M3Gubyte *data;
    if (!m3gSkipObject3DData(loader)) return M3G_FALSE;

    data = m3gGetSectionDataPtr(loader, 1);
    if (data == NULL) return M3G_FALSE;

    /* Component transform ? */
    if (*data++) {
        if (m3gCheckSectionDataLength(loader, data, 40) == M3G_FALSE) return M3G_FALSE;
        data += 40;
    }

    if (m3gCheckSectionDataLength(loader, data, 1) == M3G_FALSE) return M3G_FALSE;
    /* Generic transform */
    if (*data++) {
        if (m3gCheckSectionDataLength(loader, data, 64) == M3G_FALSE) return M3G_FALSE;
        data += 64;
    }

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads node data
 */
static M3Gbool m3gLoadNodeData(Loader *loader, M3GNode obj)
{
    M3Gubyte *data;
    if (!m3gLoadTransformableData(loader, (M3GTransformable)obj)) {
        return M3G_FALSE;
    }

    data = m3gGetSectionDataPtr(loader, 8);
    if (data == NULL) return M3G_FALSE;

    if (!m3gVerifyBool(data)) return M3G_FALSE;
    m3gEnable(obj, 0, *data++);
    if (!m3gVerifyBool(data)) return M3G_FALSE;
    m3gEnable(obj, 1, *data++);
    {
        unsigned a = *data++; 
        m3gSetAlphaFactor(obj, m3gDivif(a, 255));
    }
    m3gSetScope(obj, m3gLoadInt(data));
    data += 4;

    if (!m3gVerifyBool(data)) return M3G_FALSE;
    if (*data++) {
        M3Gubyte zt, yt;
        M3Gint zr, yr;
        if (m3gCheckSectionDataLength(loader, data, 10) == M3G_FALSE) return M3G_FALSE;
        zt = *data++;
        yt = *data++;
        zr = m3gLoadInt(data);
        yr = m3gLoadInt(data + 4);
        m3gSetAlignment(obj,    (M3GNode)m3gGetLoaded(loader, zr, ANY_NODE_CLASS),
                                zt,
                                (M3GNode)m3gGetLoaded(loader, yr, ANY_NODE_CLASS),
                                yt);
        data += 8;
    }

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Skips node data
 */
static M3Gbool m3gSkipNodeData(Loader *loader)
{
    M3Gubyte *data;
    if (!m3gSkipTransformableData(loader)) return M3G_FALSE;

    data = m3gGetSectionDataPtr(loader, 8);
    if (data == NULL) return M3G_FALSE;
    data += 7;

    /* Alignment? */
    if (*data++) {
        if (m3gCheckSectionDataLength(loader, data, 10) == M3G_FALSE) return M3G_FALSE;
        data += 10;
    }

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads a camera
 */
static M3Gbool m3gLoadCamera(Loader *loader)
{
    M3Gfloat f1, f2, f3, f4;
    M3Gubyte *data;
    M3GCamera obj = m3gCreateCamera(M3G_INTERFACE(loader));
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    if (!m3gLoadNodeData(loader, (M3GNode)obj)) {
        return M3G_FALSE;
    }

    data = m3gGetSectionDataPtr(loader, 1);
    if (data == NULL) return M3G_FALSE;

    switch(*data++) {
    case M3G_GENERIC:
        {
            Matrix m;
            if (m3gCheckSectionDataLength(loader, data, 64) == M3G_FALSE) return M3G_FALSE;
            if (!m3gLoadMatrix(&m, data)) return M3G_FALSE;
            m3gSetProjectionMatrix(obj, &m);
            data += 64;
        }
        break;
    case M3G_PERSPECTIVE:
        if (m3gCheckSectionDataLength(loader, data, 16) == M3G_FALSE) return M3G_FALSE;
        if (!m3gLoadFloat(data + 0, &f1) ||
            !m3gLoadFloat(data + 4, &f2) ||
            !m3gLoadFloat(data + 8, &f3) ||
            !m3gLoadFloat(data + 12, &f4))
            return M3G_FALSE;
        m3gSetPerspective(obj, f1, f2, f3, f4);
        data += 16;
        break;
    case M3G_PARALLEL:
        if (m3gCheckSectionDataLength(loader, data, 16) == M3G_FALSE) return M3G_FALSE;
        if (!m3gLoadFloat(data + 0, &f1) ||
            !m3gLoadFloat(data + 4, &f2) ||
            !m3gLoadFloat(data + 8, &f3) ||
            !m3gLoadFloat(data + 12, &f4))
            return M3G_FALSE;
        m3gSetParallel(obj, f1, f2, f3, f4);
        data += 16;
        break;
    default:
        /* Error */
        break;
    }

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads a background
 */
static M3Gbool m3gLoadBackground(Loader *loader)
{
    M3Gubyte *data;
    M3GBackground obj = m3gCreateBackground(M3G_INTERFACE(loader));
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    if (!m3gLoadObject3DData(loader, (M3GObject)obj)) {
        return M3G_FALSE;
    }

    data = m3gGetSectionDataPtr(loader, 28);
    if (data == NULL) return M3G_FALSE;

    m3gSetBgColor(obj, m3gLoadARGB(data));
    data += 4;
    m3gSetBgImage(obj, (M3GImage)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_IMAGE));
    data += 4;
    m3gSetBgMode(obj, data[0], data[1]);
    data += 2;
    m3gSetBgCrop(obj,   m3gLoadInt(data),
                        m3gLoadInt(data + 4),
                        m3gLoadInt(data + 8),
                        m3gLoadInt(data + 12) );
    data += 16;

    if (!m3gVerifyBool(data)) return M3G_FALSE;
    m3gSetBgEnable(obj, 0, *data++);
    if (!m3gVerifyBool(data)) return M3G_FALSE;
    m3gSetBgEnable(obj, 1, *data++);

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads a vertex array
 */
static M3Gbool m3gLoadVertexArray(Loader *loader)
{
    M3Gint i, j;
    M3Guint size;
    M3Gushort vertices;
    M3Gubyte *data, components, encoding;
    M3Gdatatype componentSize;
    M3GVertexArray obj;

    m3gBeginObject(loader);
    if (!m3gSkipObject3DData(loader)) return M3G_FALSE;

    data = m3gGetSectionDataPtr(loader, 5);
    if (data == NULL) return M3G_FALSE;

    if (*data != 1 && *data != 2) return M3G_FALSE;
    componentSize = (*data++ == 1) ? M3G_BYTE : M3G_SHORT;
    components    = *data++;
    encoding      = *data++;
    vertices      = m3gLoadShort(data);
    data += 2;

    size = vertices * components * (componentSize == M3G_BYTE ? 1 : 2);

    /* Overflow? */
    if (size < vertices) {
        return M3G_FALSE;
    }

    if (m3gCheckSectionDataLength(loader, data, size) == M3G_FALSE) return M3G_FALSE;
    obj = m3gCreateVertexArray(M3G_INTERFACE(loader), vertices, components, componentSize);
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    if (componentSize == M3G_BYTE) {
        M3Gbyte previousValues[4];
        m3gZero(previousValues, sizeof(previousValues));

        for (i = 0; i < vertices; i++) {
            for (j = 0; j < components; j++) {
                if (encoding == 0) {
                    previousValues[j] = *data++;
                }
                else {
                    previousValues[j] = (M3Gbyte) (previousValues[j] + *data++);
                }
            }
            m3gSetVertexArrayElements(obj, i, 1, sizeof(previousValues), componentSize, previousValues);
        }
    }
    else {
        M3Gshort previousValues[4];
        m3gZero(previousValues, sizeof(previousValues));

        for (i = 0; i < vertices; i++) {
            for (j = 0; j < components; j++) {
                if (encoding == 0) {
                    previousValues[j] = m3gLoadShort(data);
                }
                else {
                    previousValues[j] = (M3Gshort) (previousValues[j] + m3gLoadShort(data));
                }
                data += 2;
            }
            m3gSetVertexArrayElements(obj, i, 1, sizeof(previousValues), componentSize, previousValues);
        }
    }

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    m3gEndObject(loader);
    m3gRewindObject(loader);
    if (!m3gLoadObject3DData(loader, (M3GObject)obj)) {
        return M3G_FALSE;
    }
    m3gSkipObject(loader);

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads a vertex buffer
 */
static M3Gbool m3gLoadVertexBuffer(Loader *loader)
{
    M3Gubyte *data;
    M3GVertexArray va;
    M3Gfloat bias[3], scale;
    M3Guint i, taCount;
    M3GVertexBuffer obj = m3gCreateVertexBuffer(M3G_INTERFACE(loader));
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    if (!m3gLoadObject3DData(loader, (M3GObject)obj)) {
        return M3G_FALSE;
    }

    data = m3gGetSectionDataPtr(loader, 36);
    if (data == NULL) return M3G_FALSE;

    m3gSetVertexDefaultColor(obj, m3gLoadARGB(data));
    data += 4;

    /* Positions */
    va = (M3GVertexArray)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_VERTEX_ARRAY);
    data += 4;
    if (!m3gLoadFloat(data + 0, &bias[0])) return M3G_FALSE;
    if (!m3gLoadFloat(data + 4, &bias[1])) return M3G_FALSE;
    if (!m3gLoadFloat(data + 8, &bias[2])) return M3G_FALSE;
    if (!m3gLoadFloat(data + 12, &scale)) return M3G_FALSE;
    if (va != NULL) {
        m3gSetVertexArray(obj, va, scale, bias, 3);
    }
    data += 16;

    /* Normals */
    va = (M3GVertexArray)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_VERTEX_ARRAY);
    data += 4;
    if (va != NULL) {
        m3gSetNormalArray(obj, va);
    }

    /* Colors */
    va = (M3GVertexArray)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_VERTEX_ARRAY);
    data += 4;
    if (va != NULL) {
        m3gSetColorArray(obj, va);
    }

    /* Texture coordinates */
    taCount = m3gLoadInt(data);
    data += 4;

    /* Overflow? */
    if (taCount >= 0x0ccccccc) {
        return M3G_FALSE;
    }

    if (m3gCheckSectionDataLength(loader, data, taCount * 20) == M3G_FALSE) return M3G_FALSE;
    for (i = 0; i < taCount; i++) {
        va = (M3GVertexArray)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_VERTEX_ARRAY);
        data += 4;
        if (!m3gLoadFloat(data + 0, &bias[0])) return M3G_FALSE;
        if (!m3gLoadFloat(data + 4, &bias[1])) return M3G_FALSE;
        if (!m3gLoadFloat(data + 8, &bias[2])) return M3G_FALSE;
        if (!m3gLoadFloat(data + 12, &scale)) return M3G_FALSE;
        if (va != NULL) {
            m3gSetTexCoordArray(obj, i, va, scale, bias, 3);
        }
        else {
            return M3G_FALSE;
        }
        data += 16;
    }

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    return M3G_TRUE;
}


/*!
 * \internal
 * \brief Loads a triangle strip array
 */
static M3Gbool m3gLoadTsa(Loader *loader)
{
    M3Gubyte *data;
    M3GIndexBuffer obj = 0;
    M3Gubyte encoding;
    M3Guint startIndex = 0, indicesCount = 0, *indices = NULL;
    M3Guint lengthCount;
    M3Gint *lengths = NULL;
    M3Guint i;

    m3gBeginObject(loader);
    if (!m3gSkipObject3DData(loader)) return M3G_FALSE;

    data = m3gGetSectionDataPtr(loader, 1);
    if (data == NULL) return M3G_FALSE;

    encoding = *data++;

    switch(encoding) {
    case 0:
        if (m3gCheckSectionDataLength(loader, data, 4) == M3G_FALSE) return M3G_FALSE;
        startIndex = m3gLoadInt(data);
        data += 4;
        break;

    case 1:
        if (m3gCheckSectionDataLength(loader, data, 1) == M3G_FALSE) return M3G_FALSE;
        startIndex = *data++;
        break;

    case 2:
        if (m3gCheckSectionDataLength(loader, data, 2) == M3G_FALSE) return M3G_FALSE;
        startIndex = (M3Gushort) m3gLoadShort(data);
        data += 2;
        break;

    case 128:
        if (m3gCheckSectionDataLength(loader, data, 4) == M3G_FALSE) return M3G_FALSE;
        indicesCount = m3gLoadInt(data);
        data += 4;

        /* Overflow? */
        if (indicesCount >= 0x20000000) {
            return M3G_FALSE;
        }

        if (m3gCheckSectionDataLength(loader, data, indicesCount * 4) == M3G_FALSE) return M3G_FALSE;
        indices = m3gAllocZ(M3G_INTERFACE(loader), sizeof(M3Gint) * indicesCount);
        if (!indices) return M3G_FALSE;
        for (i = 0; i < indicesCount; i++ ) {
            indices[i] = m3gLoadInt(data);
            data += 4;
        }
        break;

    case 129:
        if (m3gCheckSectionDataLength(loader, data, 4) == M3G_FALSE) return M3G_FALSE;
        indicesCount = m3gLoadInt(data);
        data += 4;
        if (m3gCheckSectionDataLength(loader, data, indicesCount) == M3G_FALSE) return M3G_FALSE;
        indices = m3gAllocZ(M3G_INTERFACE(loader), sizeof(M3Gint) * indicesCount);
        if (!indices) return M3G_FALSE;
        for (i = 0; i < indicesCount; i++ ) {
            indices[i] = *data++;
        }
        break;

    case 130:
        if (m3gCheckSectionDataLength(loader, data, 4) == M3G_FALSE) return M3G_FALSE;
        indicesCount = m3gLoadInt(data);
        data += 4;

        /* Overflow? */
        if (indicesCount >= 0x40000000) {
            return M3G_FALSE;
        }

        if (m3gCheckSectionDataLength(loader, data, indicesCount * 2) == M3G_FALSE) return M3G_FALSE;
        indices = m3gAllocZ(M3G_INTERFACE(loader), sizeof(M3Gint) * indicesCount);
        if (!indices) return M3G_FALSE;
        for (i = 0; i < indicesCount; i++) {
            indices[i] = (M3Gushort)m3gLoadShort(data);
            data += 2;
        }
        break;

    default:
        return M3G_FALSE;
    }

    if (m3gCheckSectionDataLength(loader, data, 4) == M3G_FALSE) goto cleanup;
    lengthCount = m3gLoadInt(data);
    data += 4;

    /* Overflow? */
    if (lengthCount >= 0x20000000) {
        goto cleanup;
    }

    if (m3gCheckSectionDataLength(loader, data, lengthCount * 4) == M3G_FALSE) goto cleanup;
    lengths = m3gAllocZ(M3G_INTERFACE(loader), sizeof(M3Gint) * lengthCount);
    if (!lengths) goto cleanup;

    for (i = 0; i < lengthCount; i++) {
        lengths[i] = m3gLoadInt(data);
        data += 4;
    }

    if (encoding == 0 || encoding == 1 || encoding == 2) {
        obj = m3gCreateImplicitStripBuffer( M3G_INTERFACE(loader),
                                            lengthCount,
                                            lengths,
                                            startIndex);
    }
    else {
        obj = m3gCreateStripBuffer( M3G_INTERFACE(loader),
                                    M3G_TRIANGLE_STRIPS,
                                    lengthCount,
                                    lengths,
                                    M3G_INT,
                                    indicesCount,
                                    indices);
    }

cleanup:
    m3gFree(M3G_INTERFACE(loader), indices);
    m3gFree(M3G_INTERFACE(loader), lengths);

    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    m3gEndObject(loader);
    m3gRewindObject(loader);
    if (!m3gLoadObject3DData(loader, (M3GObject)obj)) {
        return M3G_FALSE;
    }
    m3gSkipObject(loader);

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads a compositing mode
 */
static M3Gbool m3gLoadCompositingMode(Loader *loader)
{
    M3Gfloat f1, f2;
    M3Gubyte *data;
    M3GCompositingMode obj = m3gCreateCompositingMode(M3G_INTERFACE(loader));
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    if (!m3gLoadObject3DData(loader, (M3GObject)obj)) {
        return M3G_FALSE;
    }

    data = m3gGetSectionDataPtr(loader, 14);
    if (data == NULL) return M3G_FALSE;

    if (!m3gVerifyBool(data)) return M3G_FALSE;
    m3gEnableDepthTest      (obj, *data++);
    if (!m3gVerifyBool(data)) return M3G_FALSE;
    m3gEnableDepthWrite     (obj, *data++);
    if (!m3gVerifyBool(data)) return M3G_FALSE;
    m3gEnableColorWrite     (obj, *data++);
    if (!m3gVerifyBool(data)) return M3G_FALSE;
    m3gSetAlphaWriteEnable  (obj, *data++);
    m3gSetBlending          (obj, *data++);
    {
        unsigned a = *data++; 
        m3gSetAlphaThreshold(obj, m3gDivif(a, 255));
    }
    if (!m3gLoadFloat(data, &f1) || !m3gLoadFloat(data + 4, &f2)) return M3G_FALSE;
    m3gSetDepthOffset       (obj, f1, f2);
    data += 8;

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads a polygon mode
 */
static M3Gbool m3gLoadPolygonMode(Loader *loader)
{
    M3Gubyte *data;
    M3GPolygonMode obj = m3gCreatePolygonMode(M3G_INTERFACE(loader));
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    if (!m3gLoadObject3DData(loader, (M3GObject)obj)) {
        return M3G_FALSE;
    }

    data = m3gGetSectionDataPtr(loader, 6);
    if (data == NULL) return M3G_FALSE;

    m3gSetCulling                       (obj, *data++);
    m3gSetShading                       (obj, *data++);
    m3gSetWinding                       (obj, *data++);
    if (!m3gVerifyBool(data)) return M3G_FALSE;
    m3gSetTwoSidedLightingEnable        (obj, *data++);
    if (!m3gVerifyBool(data)) return M3G_FALSE;
    m3gSetLocalCameraLightingEnable     (obj, *data++);
    if (!m3gVerifyBool(data)) return M3G_FALSE;
    m3gSetPerspectiveCorrectionEnable   (obj, *data++);

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads an appearance
 */
static M3Gbool m3gLoadAppearance(Loader *loader)
{
    M3Guint textures, i;
    M3Gubyte *data;
    M3GAppearance obj = m3gCreateAppearance(M3G_INTERFACE(loader));
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    if (!m3gLoadObject3DData(loader, (M3GObject)obj)) {
        return M3G_FALSE;
    }

    data = m3gGetSectionDataPtr(loader, 21);
    if (data == NULL) return M3G_FALSE;

    m3gSetLayer(obj, (M3Gbyte)*data++);
    m3gSetCompositingMode(obj, (M3GCompositingMode)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_COMPOSITING_MODE));
    data += 4;
    m3gSetFog(obj, (M3GFog)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_FOG));
    data += 4;
    m3gSetPolygonMode(obj, (M3GPolygonMode)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_POLYGON_MODE));
    data += 4;
    m3gSetMaterial(obj, (M3GMaterial)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_MATERIAL));
    data += 4;

    textures = m3gLoadInt(data);
    data += 4;

    /* Overflow? */
    if (textures >= 0x20000000) {
        return M3G_FALSE;
    }

    if (m3gCheckSectionDataLength(loader, data, textures * 4) == M3G_FALSE) return M3G_FALSE;
    for (i = 0; i < textures; i++) {
        M3GTexture tex = (M3GTexture)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_TEXTURE);
        if (!tex) {
            return M3G_FALSE;
        }
        m3gSetTexture(obj, i, tex);
        data += 4;
    }

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads a mesh
 */
static M3Gbool m3gLoadMesh(Loader *loader)
{
    M3Guint subMeshCount, i;
    M3GVertexBuffer vb;
    M3Gulong *ib = NULL;
    M3Gulong *ap = NULL;
    M3Gubyte *data;
    M3GMesh obj = NULL;

    m3gBeginObject(loader);
    if (!m3gSkipNodeData(loader)) return M3G_FALSE;

    data = m3gGetSectionDataPtr(loader, 8);
    if (data == NULL) return M3G_FALSE;

    vb = (M3GVertexBuffer)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_VERTEX_BUFFER);
    if (vb == NULL) return M3G_FALSE;
    data += 4;
    subMeshCount = m3gLoadInt(data);
    data += 4;

    /* Overflow? */
    if (subMeshCount >= 0x10000000) {
        return M3G_FALSE;
    }

    if (m3gCheckSectionDataLength(loader, data, subMeshCount * 8) == M3G_FALSE) return M3G_FALSE;
    ib = m3gAllocZ(M3G_INTERFACE(loader), sizeof(M3Gulong) * subMeshCount);
    ap = m3gAllocZ(M3G_INTERFACE(loader), sizeof(M3Gulong) * subMeshCount);
    if (!ib || !ap) goto cleanup;

    for (i = 0; i < subMeshCount; i++) {
        M3GIndexBuffer indexBuffer;
        indexBuffer = (M3GIndexBuffer)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_INDEX_BUFFER);
        if (indexBuffer != NULL && loader->triConstraint != 0) {
            loader->triCount += indexBuffer->indexCount;
            if (loader->triCount > loader->triConstraint) goto cleanup;
        }
        ib[i] = (M3Gulong)indexBuffer;
        data += 4;
        ap[i] = (M3Gulong)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_APPEARANCE);
        data += 4;
    }

    obj = m3gCreateMesh(    M3G_INTERFACE(loader),
                            vb,
                            ib,
                            ap,
                            subMeshCount);

cleanup:
    m3gFree(M3G_INTERFACE(loader), ib);
    m3gFree(M3G_INTERFACE(loader), ap);
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    m3gEndObject(loader);
    m3gRewindObject(loader);
    if (!m3gLoadNodeData(loader, (M3GNode)obj)) {
        return M3G_FALSE;
    }
    m3gSkipObject(loader);

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads group data
 */
static M3Gbool m3gLoadGroupData(Loader *loader, M3GGroup obj)
{
    M3Guint childCount, i;
    M3Gubyte *data;

    if (!m3gLoadNodeData(loader, (M3GNode)obj)) {
        return M3G_FALSE;
    }

    data = m3gGetSectionDataPtr(loader, 4);
    if (data == NULL) return M3G_FALSE;

    childCount = m3gLoadInt(data);
    data += 4;

    /* Overflow? */
    if (childCount >= 0x20000000) {
        return M3G_FALSE;
    }

    if (m3gCheckSectionDataLength(loader, data, childCount * 4) == M3G_FALSE) return M3G_FALSE;
    for (i = 0; i < childCount; i++) {
        m3gAddChild(obj, (M3GNode)m3gGetLoaded(loader, m3gLoadInt(data), ANY_NODE_CLASS));
        data += 4;
    }

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads a group
 */
static M3Gbool m3gLoadGroup(Loader *loader)
{
    M3GGroup obj = m3gCreateGroup(M3G_INTERFACE(loader));
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    if (!m3gLoadGroupData(loader, obj)) {
        return M3G_FALSE;
    }

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads a world
 */
static M3Gbool m3gLoadWorld(Loader *loader)
{
    M3GCamera cam;
    M3Gubyte *data;
    M3GWorld obj = m3gCreateWorld(M3G_INTERFACE(loader));
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    if (!m3gLoadGroupData(loader, (M3GGroup)obj)) {
        return M3G_FALSE;
    }

    data = m3gGetSectionDataPtr(loader, 8);
    if (data == NULL) return M3G_FALSE;

    cam = (M3GCamera)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_CAMERA);
    data += 4;
    if (cam != NULL) {
        m3gSetActiveCamera(obj, cam);
    }
    m3gSetBackground(obj, (M3GBackground)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_BACKGROUND));
    data += 4;

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads a light
 */
static M3Gbool m3gLoadLight(Loader *loader)
{
    M3Gfloat f1, f2, f3;
    M3Gubyte *data;
    M3GLight obj = m3gCreateLight(M3G_INTERFACE(loader));
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    if (!m3gLoadNodeData(loader, (M3GNode)obj)) {
        return M3G_FALSE;
    }

    data = m3gGetSectionDataPtr(loader, 28);
    if (data == NULL) return M3G_FALSE;

    if (!m3gLoadFloat(data, &f1) ||
        !m3gLoadFloat(data + 4, &f2) ||
        !m3gLoadFloat(data + 8, &f3)) return M3G_FALSE;
    m3gSetAttenuation   (obj, f1, f2, f3);
    data += 12;
    m3gSetLightColor    (obj, m3gLoadRGB(data));
    data += 3;
    m3gSetLightMode     (obj, *data++);
    if (!m3gLoadFloat(data, &f1)) return M3G_FALSE;
    m3gSetIntensity     (obj, f1);
    data += 4;
    if (!m3gLoadFloat(data, &f1)) return M3G_FALSE;
    m3gSetSpotAngle     (obj, f1);
    data += 4;
    if (!m3gLoadFloat(data, &f1)) return M3G_FALSE;
    m3gSetSpotExponent  (obj, f1);
    data += 4;

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads a keyframe sequence
 */
static M3Gbool m3gLoadKeyframeSequence(Loader *loader)
{
    M3Guint i, j, interpolation, repeatMode, encoding, duration,
            rangeFirst, rangeLast, components, keyFrames, size;
    M3Gfloat *values;
    M3Gubyte *data;
    M3GKeyframeSequence obj;

    m3gBeginObject(loader);
    if (!m3gSkipObject3DData(loader)) return M3G_FALSE;

    data = m3gGetSectionDataPtr(loader, 23);
    if (data == NULL) return M3G_FALSE;

    interpolation = *data++;
    repeatMode    = *data++;
    encoding      = *data++;
    duration      = m3gLoadInt(data);
    data += 4;
    rangeFirst    = m3gLoadInt(data);
    data += 4;
    rangeLast     = m3gLoadInt(data);
    data += 4;
    components    = m3gLoadInt(data);
    data += 4;
    keyFrames     = m3gLoadInt(data);
    data += 4;

    if (encoding == 0) {
        size = keyFrames * (4 + components * 4);
    }
    else {
        size = components * 8 + keyFrames * (4 + components * (encoding == 1 ? 1 : 2));
    }

    /* Overflow? */
    if (size < keyFrames) {
        return M3G_FALSE;
    }

    if (m3gCheckSectionDataLength(loader, data, size) == M3G_FALSE) return M3G_FALSE;

    obj = m3gCreateKeyframeSequence(M3G_INTERFACE(loader), keyFrames, components, interpolation);
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    m3gSetRepeatMode(obj, repeatMode);
    m3gSetDuration(obj, duration);
    m3gSetValidRange(obj, rangeFirst, rangeLast);

    values = m3gAllocZ(M3G_INTERFACE(loader), sizeof(M3Gfloat) * components);
    if (!values) return M3G_FALSE;

    if (encoding == 0) {
        for (i = 0; i < keyFrames; i++ ) {
            M3Gint time = m3gLoadInt(data);
            data += 4;

            for (j = 0; j < components; j++ ) {
                if (!m3gLoadFloat(data, &values[j])) {
                    m3gFree(M3G_INTERFACE(loader), values);
                    return M3G_FALSE;
                }
                data += 4;
            }

            m3gSetKeyframe(obj, i, time, components, values);
        }
    }
    else {
        M3Gfloat *vectorBiasScale = m3gAllocZ(M3G_INTERFACE(loader), sizeof(M3Gfloat) * components * 2);
        if (!vectorBiasScale) {
            m3gFree(M3G_INTERFACE(loader), values);
            return M3G_FALSE;
        }

        for (i = 0; i < components; i++ ) {
            if (!m3gLoadFloat(data, &vectorBiasScale[i])) {
                m3gFree(M3G_INTERFACE(loader), vectorBiasScale);
                m3gFree(M3G_INTERFACE(loader), values);
                return M3G_FALSE;
            }
            data += 4;
        }
        for (i = 0; i < components; i++ ) {
            if (!m3gLoadFloat(data, &vectorBiasScale[i + components])) {
                m3gFree(M3G_INTERFACE(loader), vectorBiasScale);
                m3gFree(M3G_INTERFACE(loader), values);
                return M3G_FALSE;
            }
            data += 4;
        }

        for (i = 0; i < keyFrames; i++ ) {
            M3Gint time;
            time = m3gLoadInt(data);
            data += 4;

            if (encoding == 1) {
                for (j = 0; j < components; j++ ) {
                    M3Gubyte v = *data++;
                    values[j] = vectorBiasScale[j] + ((vectorBiasScale[j + components] * v ) / 255.0f);
                }
            }
            else {
                for (j = 0; j < components; j++ ) {
                    M3Gushort v = m3gLoadShort(data);
                    data += 2;
                    values[j] = vectorBiasScale[j] + ((vectorBiasScale[j + components] * v) / 65535.0f);
                }
            }

            m3gSetKeyframe(obj, i, time, components, values);
        }

        m3gFree(M3G_INTERFACE(loader), vectorBiasScale);
    }

    m3gFree(M3G_INTERFACE(loader), values);

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    m3gEndObject(loader);
    m3gRewindObject(loader);
    if (!m3gLoadObject3DData(loader, (M3GObject)obj)) {
        return M3G_FALSE;
    }
    m3gSkipObject(loader);

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads an animation controller
 */
static M3Gbool m3gLoadAnimationController(Loader *loader)
{
    M3Gfloat speed, weight, referenceSeqTime;
    M3Gint referenceWorldTime;
    M3Gubyte *data;
    M3GAnimationController obj = m3gCreateAnimationController(M3G_INTERFACE(loader));
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    if (!m3gLoadObject3DData(loader, (M3GObject)obj)) {
        return M3G_FALSE;
    }

    data = m3gGetSectionDataPtr(loader, 24);
    if (data == NULL) return M3G_FALSE;

    if (!m3gLoadFloat(data, &speed)) return M3G_FALSE;
    data += 4;
    if (!m3gLoadFloat(data, &weight)) return M3G_FALSE;
    data += 4;
    m3gSetActiveInterval(obj, m3gLoadInt(data), m3gLoadInt(data + 4));
    data += 8;
    if (!m3gLoadFloat(data, &referenceSeqTime)) return M3G_FALSE;
    data += 4;
    referenceWorldTime = m3gLoadInt(data);
    data += 4;

    m3gSetPosition(obj, referenceSeqTime, referenceWorldTime);
    m3gSetSpeed(obj, speed, referenceWorldTime);
    m3gSetWeight(obj, weight);

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads an animation track
 */
static M3Gbool m3gLoadAnimationTrack(Loader *loader)
{
    M3Gint property;
    M3GKeyframeSequence ks;
    M3GAnimationController ac;
    M3Gubyte *data;
    M3GAnimationTrack obj;

    m3gBeginObject(loader);
    if (!m3gSkipObject3DData(loader)) return M3G_FALSE;

    data = m3gGetSectionDataPtr(loader, 12);
    if (data == NULL) return M3G_FALSE;

    ks = (M3GKeyframeSequence)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_KEYFRAME_SEQUENCE);
    data += 4;
    ac = (M3GAnimationController)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_ANIMATION_CONTROLLER);
    data += 4;
    property = m3gLoadInt(data);
    data += 4;

    obj = m3gCreateAnimationTrack(M3G_INTERFACE(loader), ks, property);
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    m3gSetController(obj, ac);

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    m3gEndObject(loader);
    m3gRewindObject(loader);
    if (!m3gLoadObject3DData(loader, (M3GObject)obj)) {
        return M3G_FALSE;
    }
    m3gSkipObject(loader);

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads a material
 */
static M3Gbool m3gLoadMaterial(Loader *loader)
{
    M3Gfloat f1;
    M3Gubyte *data;
    M3GMaterial obj = m3gCreateMaterial(M3G_INTERFACE(loader));
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    if (!m3gLoadObject3DData(loader, (M3GObject)obj)) {
        return M3G_FALSE;
    }

    data = m3gGetSectionDataPtr(loader, 18);
    if (data == NULL) return M3G_FALSE;

    m3gSetColor(obj, M3G_AMBIENT_BIT, m3gLoadRGB(data));
    data += 3;
    m3gSetColor(obj, M3G_DIFFUSE_BIT, m3gLoadARGB(data));
    data += 4;
    m3gSetColor(obj, M3G_EMISSIVE_BIT, m3gLoadRGB(data));
    data += 3;
    m3gSetColor(obj, M3G_SPECULAR_BIT, m3gLoadRGB(data));
    data += 3;
    if (!m3gLoadFloat(data, &f1)) return M3G_FALSE;
    m3gSetShininess(obj, f1);
    data += 4;
    if (!m3gVerifyBool(data)) return M3G_FALSE;
    m3gSetVertexColorTrackingEnable(obj, *data++);

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads a fog
 */
static M3Gbool m3gLoadFog(Loader *loader)
{
    M3Gfloat f1, f2;
    M3Gubyte *data;
    M3GFog obj = m3gCreateFog(M3G_INTERFACE(loader));
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    if (!m3gLoadObject3DData(loader, (M3GObject)obj)) {
        return M3G_FALSE;
    }

    data = m3gGetSectionDataPtr(loader, 4);
    if (data == NULL) return M3G_FALSE;

    m3gSetFogColor(obj, m3gLoadRGB(data));
    data += 3;
    m3gSetFogMode(obj, *data);

    if (*data++ == M3G_EXPONENTIAL_FOG) {
        if (m3gCheckSectionDataLength(loader, data, 4) == M3G_FALSE) return M3G_FALSE;
        if (!m3gLoadFloat(data, &f1)) return M3G_FALSE;
        m3gSetFogDensity(obj, f1);
        data += 4;
    }
    else {
        if (m3gCheckSectionDataLength(loader, data, 8) == M3G_FALSE) return M3G_FALSE;
        if (!m3gLoadFloat(data, &f1) || !m3gLoadFloat(data + 4, &f2)) return M3G_FALSE;
        m3gSetFogLinear(obj, f1, f2);
        data += 8;
    }

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads an image
 */
static M3Gbool m3gLoadImage(Loader *loader)
{
    M3GImageFormat format;
    M3Guint width, height;
    M3Gbyte isMutable;
    M3Gubyte *data;
    M3GImage obj;

    m3gBeginObject(loader);
    if (!m3gSkipObject3DData(loader)) return M3G_FALSE;

    data = m3gGetSectionDataPtr(loader, 10);
    if (data == NULL) return M3G_FALSE;

    format = (M3GImageFormat)*data++;
    if (!m3gVerifyBool(data)) return M3G_FALSE;
    isMutable = *data++;
    width = m3gLoadInt(data);
    data += 4;
    height = m3gLoadInt(data);
    data += 4;

    if (isMutable) {
        obj = m3gCreateImage(M3G_INTERFACE(loader), format,
                                                    width, height,
                                                    M3G_RENDERING_TARGET);
    }
    else {
        M3Gubyte *palette = NULL, *pixels = NULL;
        M3Gint paletteLength, pixelsLength, bpp;

        switch(format) {
        case M3G_ALPHA:             bpp = 1; break;
        case M3G_LUMINANCE:         bpp = 1; break;
        case M3G_LUMINANCE_ALPHA:   bpp = 2; break;
        case M3G_RGB:               bpp = 3; break;
        case M3G_RGBA:              bpp = 4; break;
        default:                    return M3G_FALSE;
        }

        if (m3gCheckSectionDataLength(loader, data, 4) == M3G_FALSE) return M3G_FALSE;
        paletteLength = m3gLoadInt(data);
        data += 4;

        if (paletteLength > 0) {
            if (m3gCheckSectionDataLength(loader, data, paletteLength) == M3G_FALSE) return M3G_FALSE;
            palette = data;
            data += paletteLength;
        }

        if (m3gCheckSectionDataLength(loader, data, 4) == M3G_FALSE) return M3G_FALSE;
        pixelsLength = m3gLoadInt(data);
        data += 4;
        if (m3gCheckSectionDataLength(loader, data, pixelsLength) == M3G_FALSE) return M3G_FALSE;
        pixels = data;
        data += pixelsLength;

        if (palette != NULL) {
            obj = m3gCreateImage(M3G_INTERFACE(loader), format,
                                                        width, height,
                                                        M3G_PALETTED);
            if (obj != NULL) {
                M3Gint numEntries = paletteLength / bpp;
                if (numEntries > 256) {
                    numEntries = 256;
                }
                m3gSetImage(obj, pixels);
                m3gSetImagePalette(obj, numEntries, palette);
                m3gCommitImage(obj);
            }
        }
        else {
            obj = m3gCreateImage(M3G_INTERFACE(loader), format,
                                                        width, height,
                                                        0);
            if (obj != NULL) {
                m3gSetImage(obj, pixels);
                m3gCommitImage(obj);
            }
        }
    }

    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    m3gEndObject(loader);
    m3gRewindObject(loader);
    if (!m3gLoadObject3DData(loader, (M3GObject)obj)) {
        return M3G_FALSE;
    }
    m3gSkipObject(loader);

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads a texture
 */
static M3Gbool m3gLoadTexture(Loader *loader)
{
    M3GImage image;
    M3Gubyte *data;
    M3GTexture obj;

    m3gBeginObject(loader);
    if (!m3gSkipTransformableData(loader)) return M3G_FALSE;

    data = m3gGetSectionDataPtr(loader, 12);
    if (data == NULL) return M3G_FALSE;

    image = (M3GImage)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_IMAGE);
    data += 4;

    obj = m3gCreateTexture(M3G_INTERFACE(loader), image);
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    m3gSetBlendColor(obj, m3gLoadRGB(data));
    data += 3;
    m3gTextureSetBlending(obj, *data++);
    m3gSetWrapping(obj, data[0], data[1]);
    data += 2;
    m3gSetFiltering(obj, data[0], data[1]);
    data += 2;

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    m3gEndObject(loader);
    m3gRewindObject(loader);
    if (!m3gLoadTransformableData(loader, (M3GTransformable)obj)) {
        return M3G_FALSE;
    }
    m3gSkipObject(loader);

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads a skinned mesh
 */
static M3Gbool m3gLoadSkinnedMesh(Loader *loader)
{
    M3GVertexBuffer vb;
    M3Guint i, subMeshCount, transformReferenceCount, firstVertex, vertexCount;
    M3Gint weight;
    M3Gulong *ib = NULL;
    M3Gulong *ap = NULL;
    M3GGroup skeleton;
    M3GNode bone;
    M3Gubyte *data;
    M3GSkinnedMesh obj = NULL;

    m3gBeginObject(loader);
    if (!m3gSkipNodeData(loader)) return M3G_FALSE;

    data = m3gGetSectionDataPtr(loader, 8);
    if (data == NULL) return M3G_FALSE;

    vb = (M3GVertexBuffer)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_VERTEX_BUFFER);
    if (vb == NULL) return M3G_FALSE;
    data += 4;
    subMeshCount = m3gLoadInt(data);
    data += 4;

    /* Overflow? */
    if (subMeshCount >= 0x10000000) {
        return M3G_FALSE;
    }

    if (m3gCheckSectionDataLength(loader, data, subMeshCount * 8) == M3G_FALSE) return M3G_FALSE;
    ib = m3gAllocZ(M3G_INTERFACE(loader), sizeof(M3Gulong) * subMeshCount);
    ap = m3gAllocZ(M3G_INTERFACE(loader), sizeof(M3Gulong) * subMeshCount);
    if (!ib || !ap) goto cleanup;

    for (i = 0; i < subMeshCount; i++) {
        ib[i] = (M3Gulong)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_INDEX_BUFFER);
        data += 4;
        ap[i] = (M3Gulong)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_APPEARANCE);
        data += 4;
    }

    if (m3gCheckSectionDataLength(loader, data, 8) == M3G_FALSE) goto cleanup;
    skeleton = (M3GGroup)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_GROUP);
    data += 4;

    obj = m3gCreateSkinnedMesh( M3G_INTERFACE(loader),
                                vb,
                                ib,
                                ap,
                                subMeshCount,
                                skeleton);

cleanup:
    m3gFree(M3G_INTERFACE(loader), ib);
    m3gFree(M3G_INTERFACE(loader), ap);
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    transformReferenceCount = m3gLoadInt(data);
    data += 4;

    /* Overflow? */
    if (transformReferenceCount >= 0x08000000) {
        return M3G_FALSE;
    }

    if (m3gCheckSectionDataLength(loader, data, transformReferenceCount * 16) == M3G_FALSE) return M3G_FALSE;
    for (i = 0; i < transformReferenceCount; i++) {
        bone        = (M3GNode)m3gGetLoaded(loader, m3gLoadInt(data), ANY_NODE_CLASS);
        data += 4;
        firstVertex = m3gLoadInt(data);
        data += 4;
        vertexCount = m3gLoadInt(data);
        data += 4;
        weight      = m3gLoadInt(data);
        data += 4;
        m3gAddTransform(obj, bone, weight, firstVertex, vertexCount);
    }

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    m3gEndObject(loader);
    m3gRewindObject(loader);
    if (!m3gLoadNodeData(loader, (M3GNode)obj)) {
        return M3G_FALSE;
    }
    m3gSkipObject(loader);

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads a morphing mesh
 */
static M3Gbool m3gLoadMorphingMesh(Loader *loader)
{
    M3GVertexBuffer vb;
    M3Gulong *targets = NULL;
    M3Guint i, subMeshCount, targetCount;
    M3Gfloat *weights = NULL;
    M3Gulong *ib = NULL;
    M3Gulong *ap = NULL;
    M3Gubyte *data;
    M3GMorphingMesh obj = NULL;

    m3gBeginObject(loader);
    if (!m3gSkipNodeData(loader)) return M3G_FALSE;

    data = m3gGetSectionDataPtr(loader, 8);
    if (data == NULL) return M3G_FALSE;

    vb = (M3GVertexBuffer)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_VERTEX_BUFFER);
    if (vb == NULL) return M3G_FALSE;
    data += 4;
    subMeshCount = m3gLoadInt(data);
    data += 4;

    /* Overflow? */
    if (subMeshCount >= 0x10000000) {
        return M3G_FALSE;
    }

    if (m3gCheckSectionDataLength(loader, data, subMeshCount * 8) == M3G_FALSE) return M3G_FALSE;
    ib = m3gAllocZ(M3G_INTERFACE(loader), sizeof(M3Gulong) * subMeshCount);
    ap = m3gAllocZ(M3G_INTERFACE(loader), sizeof(M3Gulong) * subMeshCount);
    if (!ib || !ap) goto cleanup;

    for (i = 0; i < subMeshCount; i++) {
        ib[i] = (M3Gulong)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_INDEX_BUFFER);
        data += 4;
        ap[i] = (M3Gulong)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_APPEARANCE);
        data += 4;
    }

    if (m3gCheckSectionDataLength(loader, data, 4) == M3G_FALSE) goto cleanup;
    targetCount = m3gLoadInt(data);
    data += 4;

    /* Overflow? */
    if (targetCount >= 0x10000000) {
        goto cleanup;
    }

    if (m3gCheckSectionDataLength(loader, data, targetCount * 8) == M3G_FALSE) goto cleanup;
    weights = m3gAllocZ(M3G_INTERFACE(loader), sizeof(M3Gfloat) * targetCount);
    targets = m3gAllocZ(M3G_INTERFACE(loader), sizeof(*targets) * targetCount);
    if (!weights || !targets) goto cleanup;

    for (i = 0; i < targetCount; i++) {
        targets[i] = (M3Gulong)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_VERTEX_BUFFER);
        data += 4;
        if (!m3gLoadFloat(data, &weights[i])) goto cleanup;
        data += 4;
    }

    obj = m3gCreateMorphingMesh(    M3G_INTERFACE(loader),
                                    vb,
                                    targets,
                                    ib,
                                    ap,
                                    subMeshCount,
                                    targetCount);

cleanup:
    m3gFree(M3G_INTERFACE(loader), ib);
    m3gFree(M3G_INTERFACE(loader), ap);
    m3gFree(M3G_INTERFACE(loader), targets);
    m3gFree(M3G_INTERFACE(loader), weights);
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    m3gEndObject(loader);
    m3gRewindObject(loader);
    if (!m3gLoadNodeData(loader, (M3GNode)obj)) {
        return M3G_FALSE;
    }
    m3gSkipObject(loader);

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads a sprite
 */
static M3Gbool m3gLoadSprite(Loader *loader)
{
    M3GImage image;
    M3GAppearance appearance;
    M3Gubyte *data;
    M3GSprite obj;

    m3gBeginObject(loader);
    if (!m3gSkipNodeData(loader)) return M3G_FALSE;

    data = m3gGetSectionDataPtr(loader, 25);
    if (data == NULL) return M3G_FALSE;

    image = (M3GImage)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_IMAGE);
    data += 4;
    appearance = (M3GAppearance)m3gGetLoaded(loader, m3gLoadInt(data), M3G_CLASS_APPEARANCE);
    data += 4;

    if (!m3gVerifyBool(data)) return M3G_FALSE;
    obj = m3gCreateSprite(  M3G_INTERFACE(loader),
                            *data++,
                            image,
                            appearance);
    loader->loadedObject = (M3GObject)obj;
    if (!obj) return M3G_FALSE;

    m3gSetCrop(obj, m3gLoadInt(data),
                    m3gLoadInt(data + 4),
                    m3gLoadInt(data + 8),
                    m3gLoadInt(data + 12));
    data += 16;

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    m3gEndObject(loader);
    m3gRewindObject(loader);
    if (!m3gLoadNodeData(loader, (M3GNode)obj)) {
        return M3G_FALSE;
    }
    m3gSkipObject(loader);

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads a M3G file header
 */
static M3Gbool m3gLoadHeader(Loader *loader)
{
    M3Gubyte *data;
    data = m3gGetSectionDataPtr(loader, 12);
    if (data == NULL) return M3G_FALSE;

    /* Check version */
    if (data[0] != 1 || data[1] != 0 || loader->sectionNum != 0) {
        return M3G_FALSE;
    }
    data += 2;

    if (!m3gVerifyBool(data)) return M3G_FALSE;
    loader->hasReferences = *data++;
    loader->fileSize = m3gLoadInt(data);
    data += 4;
    loader->contentSize = m3gLoadInt(data);
    data += 4;

    /* Skip authoring field */
    while(*data++)
        if (m3gCheckSectionDataLength(loader, data, 1) == M3G_FALSE) return M3G_FALSE;

    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));

    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Skips external reference
 */
static M3Gbool m3gLoadExternalReference(Loader *loader)
{
    M3Gubyte *data;
    if (loader->sectionNum != 1 || !loader->hasReferences)
        return M3G_FALSE;
    data = m3gGetSectionDataPtr(loader, 1);
    while(*data++) { /* Skip string */
        if (m3gCheckSectionDataLength(loader, data, 1) == M3G_FALSE)
            return M3G_FALSE;
    }
    m3gAdvanceSectionData(loader, data - m3gGetSectionDataPtr(loader, 0));
    return M3G_TRUE;
}

/*!
 * \internal
 * \brief Loads an object
 */
static LoaderState m3gLoadObject(Loader *loader)
{
    M3Gubyte *data = m3gGetSectionDataPtr(loader, loader->sectionBytesRequired);

    if (data == NULL) {
        loader->localState = LOADSTATE_CHECKSUM;
        return LOADSTATE_SECTION;
    }

    m3gAdvanceSectionData(loader, loader->sectionBytesRequired);

    if (loader->localState == LOADSTATE_ENTER) {
        M3Gbool status = M3G_TRUE;

        loader->objectType = *data++;
        loader->loadedObject = NULL;
        loader->localState = LOADSTATE_EXIT;
        loader->sectionBytesRequired = m3gLoadInt(data);
        data += 4;
        if (loader->sectionNum == 0 && loader->objectType != 0) {
            m3gRaiseError(M3G_INTERFACE(loader), M3G_IO_ERROR);
            return LOADSTATE_ERROR;
        }

        switch(loader->objectType) {
        case 0: /* Header Object */
            status = m3gLoadHeader(loader);
            break;
        case 1: /* AnimationController */
            status = m3gLoadAnimationController(loader);
            break;
        case 2: /* AnimationTrack */
            status = m3gLoadAnimationTrack(loader);
            break;
        case 3: /* Appearance */
            status = m3gLoadAppearance(loader);
            break;
        case 4: /* Background */
            status = m3gLoadBackground(loader);
            break;
        case 5: /* Camera */
            status = m3gLoadCamera(loader);
            break;
        case 6: /* CompositingMode */
            status = m3gLoadCompositingMode(loader);
            break;
        case 7: /* Fog */
            status = m3gLoadFog(loader);
            break;
        case 8: /* PolygonMode */
            status = m3gLoadPolygonMode(loader);
            break;
        case 9: /* Group */
            status = m3gLoadGroup(loader);
            break;
        case 10: /* Image2D */
            status = m3gLoadImage(loader);
            break;
        case 11: /* TriangleStripArray */
            status = m3gLoadTsa(loader);
            break;
        case 12: /* Light */
            status = m3gLoadLight(loader);
            break;
        case 13: /* Material */
            status = m3gLoadMaterial(loader);
            break;
        case 14: /* Mesh */
            status = m3gLoadMesh(loader);
            break;
        case 15: /* MorphingMesh */
            status = m3gLoadMorphingMesh(loader);
            break;
        case 16: /* SkinnedMesh */
            status = m3gLoadSkinnedMesh(loader);
            break;
        case 17: /* Texture2D */
            status = m3gLoadTexture(loader);
            break;
        case 18: /* Sprite */
            status = m3gLoadSprite(loader);
            break;
        case 19: /* KeyframeSequence */
            status = m3gLoadKeyframeSequence(loader);
            break;
        case 20: /* VertexArray */
            status = m3gLoadVertexArray(loader);
            break;
        case 21: /* VertexBuffer */
            status = m3gLoadVertexBuffer(loader);
            break;
        case 22: /* World */
            status = m3gLoadWorld(loader);
            break;
        case 255: /* External Reference */
            status = m3gLoadExternalReference(loader);
            break;
        default:  /* 23 ... 254 Reserved for use in future versions of the file format */
            status = M3G_FALSE;
            break;
        }

        /* Check if object loading caused an error */
        if (m3gErrorRaised(M3G_INTERFACE(loader))) {
            m3gDeleteObject(loader->loadedObject);
            return LOADSTATE_ERROR;
        }
        if (!status || 
            m3gGetSectionDataPtr(loader, 0) != data + loader->sectionBytesRequired) {
            m3gDeleteObject(loader->loadedObject);
            m3gRaiseError(M3G_INTERFACE(loader), M3G_IO_ERROR);
            return LOADSTATE_ERROR;
        }
        loader->sectionBytesRequired = 0;
        
        /* Add object to loaded objects array */
        if (loader->loadedObject != NULL) {
            if (m3gArrayAppend(&loader->refArray, loader->loadedObject, M3G_INTERFACE(loader)) == -1) {
                /* OOM */
                m3gDeleteObject(loader->loadedObject);
                return LOADSTATE_ERROR;
            }
            m3gAddRef(loader->loadedObject);
        }
    }
    else {
        loader->sectionBytesRequired = M3G_MIN_OBJECT_SIZE;
        loader->localState = LOADSTATE_ENTER;
    }

    return LOADSTATE_OBJECT;
}

/*!
 * \internal
 * \brief Handles branching to different subroutines upon re-entry
 *
 * When sufficient data is available in internal buffers, this
 * function gets called and directs execution into the coroutine
 * matching the current global state.
 */
static LoaderState m3gLoaderMain(Loader *loader)
{
    M3G_VALIDATE_OBJECT(loader);
    M3G_ASSERT(loader->bytesRequired > 0);
    M3G_ASSERT(loader->bytesRequired <= loader->stream.bytesAvailable);

    switch (loader->state) {
    case LOADSTATE_INITIAL:
        loader->stream.totalBytes = 0;
        loader->localState = LOADSTATE_ENTER;
        loader->fileSize = 0x00ffffff;
        loader->bytesRequired = sizeof(PNG_FILE_IDENTIFIER);
        return LOADSTATE_IDENTIFIER;
    case LOADSTATE_IDENTIFIER:
        return m3gLoadIdentifier(loader);
    case LOADSTATE_SECTION:
        return m3gLoadSection(loader);
    case LOADSTATE_OBJECT:
        return m3gLoadObject(loader);
    default:
        loader->bytesRequired = 0;
        return LOADSTATE_ERROR;
    }
}

/*!
 * \brief Deletes all unreferenced objects
 */
static void m3gCleanupLoader(M3GLoader loader)
{
    M3Gint i, j, n;
    PointerArray *refs;
    M3Gbool referenced;
    M3GObject obj;    

    refs = &loader->refArray;
    n = m3gArraySize(refs);

    /* All unreferenced objects will be deleted, as their ref count becomes 0 */
    for (i = 0; i < n; ++i) {
        obj = m3gGetLoadedPtr(refs, i, &referenced);
        m3gDeleteRef(obj);
    }
    m3gClearArray(&loader->refArray);

    n = m3gArraySize(&loader->userDataArray);
    for (i = 0; i < n; ++i)
    {
        UserData *data = (UserData *)m3gGetArrayElement(&loader->userDataArray, i);
        for (j = 0; j < data->numParams; ++j)
            m3gFree(M3G_INTERFACE(loader), data->params[j]);
        m3gFree(M3G_INTERFACE(loader), data->params);
        m3gFree(M3G_INTERFACE(loader), data->paramLengths);
        m3gFree(M3G_INTERFACE(loader), data->paramId);
        m3gFree(M3G_INTERFACE(loader), data);
    }
    m3gClearArray(&loader->userDataArray);

    m3gFree(M3G_INTERFACE(loader), loader->allocatedSectionData);
    loader->allocatedSectionData = NULL;
}

/*!
 * \internal
 * \brief Resets the loader
 */
static void m3gResetLoader(Loader *loader)
{
    /* Reset loader state */
    loader->state = LOADSTATE_INITIAL;
    loader->bytesRequired = sizeof(PNG_FILE_IDENTIFIER);

    m3gCleanupLoader(loader);
    m3gResetBufferedData(&loader->stream);
}

/*----------------------------------------------------------------------
 * Virtual function table
 *--------------------------------------------------------------------*/

static const ObjectVFTable m3gvf_Loader = {
    NULL, /* ApplyAnimation */
    NULL, /* IsCompatible */
    NULL, /* UpdateProperty */
    NULL, /* DoGetReferences */
    NULL, /* FindID */
    NULL, /* Duplicate */
    m3gDestroyLoader
};
    

/*----------------------------------------------------------------------
 * Public interface
 *--------------------------------------------------------------------*/

/*!
 * \brief Creates a new loader instance
 */
M3G_API M3GLoader m3gCreateLoader(M3GInterface m3g)
{
    Loader *loader;
    M3G_VALIDATE_INTERFACE(m3g);

    loader = m3gAllocZ(m3g, sizeof(*loader));
    if (loader != NULL) {
        m3gInitObject(&loader->object, m3g, M3G_CLASS_LOADER);
        m3gInitArray(&loader->refArray);
        m3gInitArray(&loader->userDataArray);
        loader->bytesRequired = sizeof(PNG_FILE_IDENTIFIER);
        loader->state = LOADSTATE_INITIAL;
        loader->sectionNum = -1;
    }
    return loader;
}

/*!
 * \brief Import a set of objects into the reference table of a loader
 *
 * This is intended for passing in external references, decoded in
 * Java
 */
M3G_API void m3gImportObjects(M3GLoader loader, M3Gint n, M3Gulong *refs)
{
    int i;
    M3G_VALIDATE_OBJECT(loader);

    if (loader->state == LOADSTATE_DONE)
        m3gResetLoader(loader);
    for (i = 0; i < n; ++i) {
        /* For loop is interrupted in case of OOM */
        if (m3gArrayAppend(&loader->refArray,
                           (void *) refs[i],
                           M3G_INTERFACE(loader)) == -1) {
            break;
        }
        m3gAddRef((M3GObject) refs[i]);
    }
}

/*!
 * \brief Return the complete reference table for this loader instance
 *
 * The reference table will contain the handle of each object that has
 * been loaded so far.
 *
 * \param loader loader instance
 * \param buffer destination buffer, or NULL to just get
 *               the number of unreferenced objects
 * \return number of unreferenced objects
 */
M3G_API M3Gint m3gGetLoadedObjects(M3GLoader loader, M3Gulong *buffer)
{
    PointerArray *refs;
    int i, n, unref = 0;
    M3Gbool referenced;
    M3Gulong obj, *dst = buffer;
    M3G_VALIDATE_OBJECT(loader);

    /* If error in decoding, reset and return 0 objects */
    if (loader->state < LOADSTATE_INITIAL) {
        return 0;
    }

    refs = &loader->refArray;
    n = m3gArraySize(refs);

    /* Scan unreferenced objects */
    for (i = 0; i < n; ++i) {
        obj = (M3Gulong) m3gGetLoadedPtr(refs, i, &referenced);
        if (!referenced) {
            unref++;
            if (dst != NULL) {
                *dst++ = obj;
            }
        }
    }

    return unref;
}

/*!
 * \brief Submits data to the loader for processing
 *
 * Upon data input, the loader will either read more objects from the
 * stream or buffer the data for processing later on. The return value
 * will indicate how many bytes of data are required before the next
 * data element can be loaded, but that data can still be submitted in
 * smaller blocks.
 *
 * \param loader the loader instance
 * \param bytes  the number of bytes in the data
 * \param data   pointer to the data
 *
 * \return the number of bytes required to load the next data element,
 * or zero to indicate that loading has finished
 */

#ifdef M3G_ENABLE_PROFILING
static M3Gsizei m3gDecodeDataInternal(M3GLoader loader,
                               M3Gsizei bytes,
                               const M3Gubyte *data);

M3G_API M3Gsizei m3gDecodeData(M3GLoader loader,
                               M3Gsizei bytes,
                               const M3Gubyte *data)
{
    M3Gsizei bytesReq;
    M3G_BEGIN_PROFILE(M3G_INTERFACE(loader), M3G_PROFILE_LOADER_DECODE);
    bytesReq = m3gDecodeDataInternal(loader, bytes, data);
    M3G_END_PROFILE(M3G_INTERFACE(loader), M3G_PROFILE_LOADER_DECODE);
    return bytesReq;
}

static M3Gsizei m3gDecodeDataInternal(M3GLoader loader,
                               M3Gsizei bytes,
                               const M3Gubyte *data)

#else
M3G_API M3Gsizei m3gDecodeData(M3GLoader loader,
                               M3Gsizei bytes,
                               const M3Gubyte *data)

#endif
{
    m3gErrorHandler *errorHandler;
    Interface *m3g = M3G_INTERFACE(loader);

    M3G_VALIDATE_OBJECT(loader);
    M3G_VALIDATE_INTERFACE(m3g);

    /* Check for errors */
    if (bytes <= 0 || data == NULL) {
        m3gRaiseError(m3g, M3G_INVALID_VALUE);
        return 0;
    }

    if (loader->state == LOADSTATE_DONE)
        m3gResetLoader(loader);

    /* Submit data, then load until we run out of data again or are
     * finished */

    if (!m3gBufferData(m3g, &loader->stream, bytes, data)) {
        return 0;
    }

    /* Disable error handler */
    errorHandler = m3gSetErrorHandler(m3g, NULL);

    /* Continue loading if sufficient data has arrived */
    while (loader->bytesRequired > 0
           && loader->bytesRequired <= loader->stream.bytesAvailable) {
        loader->state = m3gLoaderMain(loader);
    }

    /* Restore error handler */
    m3gSetErrorHandler(m3g, errorHandler); 

    /* Check if error was raised */
    if (m3gErrorRaised(m3g) != M3G_NO_ERROR) {
        /* Need to free all loaded objects */
        m3gResetLoader(loader);

        /* Raise again with original error handler in place */
        if (m3gErrorRaised(m3g) == M3G_OUT_OF_MEMORY)
            m3gRaiseError(m3g, M3G_OUT_OF_MEMORY);
        else
            m3gRaiseError(m3g, M3G_IO_ERROR);
        return 0;
    }

    /* Return the number of bytes we need for loading to proceed
     * further; clamp to zero in case we're exiting due to an error,
     * or just have been fed excess data */
    {
        M3Gsizei bytesReq =
            loader->bytesRequired - loader->stream.bytesAvailable;

        /* Check if whole file is done */
        if (loader->stream.totalBytes >= loader->fileSize) {
            loader->state = LOADSTATE_DONE;
            bytesReq = 0;
        }

        return (bytesReq >= 0) ? bytesReq : 0;
    }
}

/*!
 * \brief Return all loaded objects with user parameters
 *
 * \param loader  the loader instance
 * \param objects an array for objects with user parameters,
 *                or null to return the number of objects
 *
 * \return Number of objects with user parameters
 */
M3G_API M3Gint m3gGetObjectsWithUserParameters(M3GLoader loader, M3Gulong *objects) {
    const Loader *ldr = (const Loader *) loader;
    M3G_VALIDATE_OBJECT(ldr);
    {
        M3Gint i, n;
        n = m3gArraySize(&ldr->userDataArray);
    
        if (objects != NULL)
            for (i = 0; i < n; ++i)
            {
                const UserData *data = (const UserData *)m3gGetArrayElement(&ldr->userDataArray, i);
                objects[i] = (M3Gulong) data->object;
            }
        return n;
    }
}

/*!
 * \brief Return the number of user parameters loaded for an object
 */
M3G_API M3Gint m3gGetNumUserParameters(M3GLoader loader, M3Gint object)
{
    const Loader *ldr = (const Loader *) loader;
    M3G_VALIDATE_OBJECT(ldr);
    {
        const UserData *data = (const UserData *)m3gGetArrayElement(&ldr->userDataArray, object);
        return (data != NULL) ? data->numParams : 0;
    }
}

/*!
 * \brief Set constraints for loading.
 * \param triConstraint maximum triangle count
 */
M3G_API void m3gSetConstraints(M3GLoader loader, M3Gint triConstraint)
{
    M3G_VALIDATE_OBJECT(loader);
    loader->triConstraint = triConstraint;
}

/*!
 * \brief Return the given user parameter for an object
 *
 * \param loader the loader instance
 * \param object the object to query
 * \param index  index of the string to query
 * \param buffer buffer to copy the data into,
 *               or NULL to just query the length
 *
 * \return id of the parameter, or the length of the string if data was NULL
 */
M3G_API M3Gsizei m3gGetUserParameter(M3GLoader loader,
                                     M3Gint object,
                                     M3Gint index,
                                     M3Gbyte *buffer)
{
    const Loader *ldr = (const Loader *) loader;
    M3G_VALIDATE_OBJECT(ldr);
    {
        const UserData *data = (const UserData *)m3gGetArrayElement(&ldr->userDataArray, object);
        if (data != NULL && m3gInRange(index, 0, data->numParams - 1)) {
            const char *src = (const char *)data->params[index];
            M3Gsizei len = data->paramLengths[index];
            if (buffer != NULL) {
                m3gCopy(buffer, src, len);
                return data->paramId[index];
            }
            else
                return len;
        }
        else
            return 0;
    }
}

#undef ANY_NODE_CLASS

