/*----------------------------------------------------------------------------
 *
 * File:
 * eas_xmf.c
 *  5
 * Contents and purpose:
 * XMF File Parser
 *
 * Copyright Sonic Network Inc. 2005

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *----------------------------------------------------------------------------
 * Revision Control:
 *   $Revision: 501 $
 *   $Date: 2006-12-11 17:53:36 -0800 (Mon, 11 Dec 2006) $
 *----------------------------------------------------------------------------
*/

#include <log/log.h>

#include "eas_data.h"
#include "eas_miditypes.h"
#include "eas_parser.h"
#include "eas_report.h"
#include "eas_host.h"
#include "eas_midi.h"
#include "eas_xmf.h"
#include "eas_xmfdata.h"
#include "eas_config.h"
#include "eas_vm_protos.h"
#include "eas_mdls.h"
#include "eas_smf.h"


/* XMF header file type */
#define XMF_IDENTIFIER          0x584d465f
#define XMF_VERSION_1_00        0x312e3030
#define XMF_VERSION_1_01        0x312e3031
#define XMF_VERSION_2_00        0x322e3030
#define XMF_FILE_TYPE           0x00000002
#define XMF_SPEC_LEVEL          0x00000001
#define XMF_RIFF_CHUNK          0x52494646
#define XMF_RIFF_DLS            0x444c5320
#define XMF_SMF_CHUNK           0x4d546864

/* local prototypes */
static EAS_RESULT XMF_CheckFileType (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, EAS_VOID_PTR *ppHandle, EAS_I32 offset);
static EAS_RESULT XMF_Prepare (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT XMF_Time (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_U32 *pTime);
static EAS_RESULT XMF_Event (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_INT parserMode);
static EAS_RESULT XMF_State (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_STATE *pState);
static EAS_RESULT XMF_Close (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT XMF_Reset (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT XMF_Pause (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT XMF_Resume (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData);
static EAS_RESULT XMF_SetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value);
static EAS_RESULT XMF_GetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue);
static EAS_RESULT XMF_FindFileContents (EAS_HW_DATA_HANDLE hwInstData, S_XMF_DATA *pXMFData);
static EAS_RESULT XMF_ReadNode (EAS_HW_DATA_HANDLE hwInstData, S_XMF_DATA *pXMFData, EAS_I32 nodeOffset, EAS_I32 endOffset, EAS_I32 *pLength, EAS_I32 depth);
static EAS_RESULT XMF_ReadVLQ (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE fileHandle, EAS_U32 *remainingBytes, EAS_I32 *value);


/*----------------------------------------------------------------------------
 *
 * XMF_Parser
 *
 * This structure contains the functional interface for the XMF parser
 *----------------------------------------------------------------------------
*/
const S_FILE_PARSER_INTERFACE EAS_XMF_Parser =
{
    XMF_CheckFileType,
    XMF_Prepare,
    XMF_Time,
    XMF_Event,
    XMF_State,
    XMF_Close,
    XMF_Reset,
#ifdef JET_INTERFACE
    XMF_Pause,
    XMF_Resume,
#else
    NULL,
    NULL,
#endif
    NULL,
    XMF_SetData,
    XMF_GetData,
    NULL
};

/*----------------------------------------------------------------------------
 * XMF_CheckFileType()
 *----------------------------------------------------------------------------
 * Purpose:
 * Check the file type to see if we can parse it
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT XMF_CheckFileType (S_EAS_DATA *pEASData, EAS_FILE_HANDLE fileHandle, EAS_VOID_PTR *ppHandle, EAS_I32 offset)
{
    S_XMF_DATA *pXMFData;
    EAS_RESULT result;
    EAS_U32 temp;

    /* assume we don't recognize it initially */
    *ppHandle = NULL;

    /* read the file identifier */
    if ((result = EAS_HWGetDWord(pEASData->hwInstData, fileHandle, &temp, EAS_TRUE))  != EAS_SUCCESS)
        return result;
    if (temp != XMF_IDENTIFIER)
        return EAS_SUCCESS;

    /* read the version */
    if ((result = EAS_HWGetDWord(pEASData->hwInstData, fileHandle, &temp, EAS_TRUE))  != EAS_SUCCESS)
        return result;

    if (temp == XMF_VERSION_2_00)
    {
        /* read the file type */
        result = EAS_HWGetDWord(pEASData->hwInstData, fileHandle, &temp, EAS_TRUE);
        if (result != EAS_SUCCESS)
            return result;

        if (temp != XMF_FILE_TYPE)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR,
                          "XMF file type was 0x%08x, expected 0x%08x\n", temp, XMF_FILE_TYPE); */ }
            return EAS_SUCCESS;
        }

        /* read the spec level */
        result = EAS_HWGetDWord(pEASData->hwInstData, fileHandle, &temp, EAS_TRUE);
        if (result != EAS_SUCCESS)
            return result;

        if (temp != XMF_SPEC_LEVEL)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR,
                          "XMF file spec was 0x%08x, expected 0x%08x\n", temp, XMF_SPEC_LEVEL); */ }
            return EAS_SUCCESS;
        }
    }
    else if (temp != XMF_VERSION_1_00 && temp != XMF_VERSION_1_01)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "XMF file version was 0x%08x\n", temp); */ }
        return EAS_SUCCESS;
    }

    /* check for static memory allocation */
    if (pEASData->staticMemoryModel)
        pXMFData = EAS_CMEnumData(EAS_CM_XMF_DATA);
    else
        pXMFData = EAS_HWMalloc(pEASData->hwInstData, sizeof(S_XMF_DATA));
    if (!pXMFData)
        return EAS_ERROR_MALLOC_FAILED;

    /* zero the memory to insure complete initialization */
    EAS_HWMemSet((void *)pXMFData,0, sizeof(S_XMF_DATA));

    pXMFData->fileHandle = fileHandle;
    pXMFData->fileOffset = offset;

    /* locate the SMF and DLS contents */
    if ((result = XMF_FindFileContents(pEASData->hwInstData, pXMFData)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "No SMF data found in XMF file\n"); */ }
        EAS_HWFree(pEASData->hwInstData, pXMFData);
        return result;
    }

    /* let the SMF parser take over */
    if ((result = EAS_HWFileSeek(pEASData->hwInstData, fileHandle, pXMFData->midiOffset)) != EAS_SUCCESS) {
        EAS_HWFree(pEASData->hwInstData, pXMFData);
        return result;
    }
    if ((result = SMF_CheckFileType(pEASData, fileHandle, &pXMFData->pSMFData, pXMFData->midiOffset)) != EAS_SUCCESS) {
        EAS_HWFree(pEASData->hwInstData, pXMFData);
        return result;
    }
    *ppHandle = pXMFData;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * XMF_Prepare()
 *----------------------------------------------------------------------------
 * Purpose:
 * Prepare to parse the file. Allocates instance data (or uses static allocation for
 * static memory model).
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT XMF_Prepare (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_XMF_DATA* pXMFData;
    EAS_RESULT result;

    /* parse DLS collection */
    pXMFData = (S_XMF_DATA*) pInstData;
    if (pXMFData->dlsOffset != 0)
    {
        if ((result = DLSParser(pEASData->hwInstData, pXMFData->fileHandle, pXMFData->dlsOffset, &pXMFData->pDLS)) != EAS_SUCCESS)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Error converting XMF DLS data\n"); */ }
            return result;
        }
    }

    /* Prepare the SMF parser */
    if ((result = SMF_Prepare(pEASData, pXMFData->pSMFData)) != EAS_SUCCESS)
        return result;

    /* if no DLS file, skip this step */
    if (pXMFData->pDLS == NULL)
        return EAS_SUCCESS;

    /* tell the synth to use the DLS collection */
    result = VMSetDLSLib(((S_SMF_DATA*) pXMFData->pSMFData)->pSynth, pXMFData->pDLS);
    if (result == EAS_SUCCESS)
    {
        DLSAddRef(pXMFData->pDLS);
        VMInitializeAllChannels(pEASData->pVoiceMgr, ((S_SMF_DATA*) pXMFData->pSMFData)->pSynth);
    }
    return result;
}

/*----------------------------------------------------------------------------
 * XMF_Time()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the time of the next event in msecs
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 * pTime            - pointer to variable to hold time of next event (in msecs)
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT XMF_Time (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_U32 *pTime)
{
    return SMF_Time(pEASData, ((S_XMF_DATA*) pInstData)->pSMFData, pTime);
}

/*----------------------------------------------------------------------------
 * XMF_Event()
 *----------------------------------------------------------------------------
 * Purpose:
 * Parse the next event in the file
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT XMF_Event (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_INT parserMode)
{
    return SMF_Event(pEASData, ((S_XMF_DATA*) pInstData)->pSMFData, parserMode);
}

/*----------------------------------------------------------------------------
 * XMF_State()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the current state of the stream
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 * pState           - pointer to variable to store state
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT XMF_State (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 *pState)
{
    return SMF_State(pEASData, ((S_XMF_DATA*) pInstData)->pSMFData, pState);
}

/*----------------------------------------------------------------------------
 * XMF_Close()
 *----------------------------------------------------------------------------
 * Purpose:
 * Close the file and clean up
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT XMF_Close (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    S_XMF_DATA* pXMFData;
    EAS_RESULT result;

    pXMFData = (S_XMF_DATA *)pInstData;

    /* close the SMF stream, it will close the file handle */
    if ((result = SMF_Close(pEASData, pXMFData->pSMFData)) != EAS_SUCCESS)
        return result;

    if (pXMFData->pDLS)
        DLSCleanup(pEASData->hwInstData, pXMFData->pDLS);

    /* if using dynamic memory, free it */
    if (!pEASData->staticMemoryModel)
    {
        /* free the instance data */
        EAS_HWFree(pEASData->hwInstData, pXMFData);
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * XMF_Reset()
 *----------------------------------------------------------------------------
 * Purpose:
 * Reset the sequencer. Used for locating backwards in the file.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT XMF_Reset (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    return SMF_Reset(pEASData, ((S_XMF_DATA*) pInstData)->pSMFData);
}

#ifdef JET_INTERFACE
/*----------------------------------------------------------------------------
 * XMF_Pause()
 *----------------------------------------------------------------------------
 * Purpose:
 * Pauses the sequencer. Mutes all voices and sets state to pause.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT XMF_Pause (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    return SMF_Pause(pEASData, ((S_XMF_DATA*) pInstData)->pSMFData);
}

/*----------------------------------------------------------------------------
 * XMF_Resume()
 *----------------------------------------------------------------------------
 * Purpose:
 * Resume playing after a pause, sets state back to playing.
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT XMF_Resume (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData)
{
    return SMF_Resume(pEASData, ((S_XMF_DATA*) pInstData)->pSMFData);
}
#endif

/*----------------------------------------------------------------------------
 * XMF_SetData()
 *----------------------------------------------------------------------------
 * Purpose:
 * Sets the playback rate of the underlying SMF file
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 * rate             - rate (28-bit fraction)
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT XMF_SetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 value)
{
    return SMF_SetData(pEASData, ((S_XMF_DATA*) pInstData)->pSMFData, param, value);
}

/*----------------------------------------------------------------------------
 * XMF_GetData()
 *----------------------------------------------------------------------------
 * Purpose:
 * Gets the file type
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * handle           - pointer to file handle
 * rate             - rate (28-bit fraction)
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT XMF_GetData (S_EAS_DATA *pEASData, EAS_VOID_PTR pInstData, EAS_I32 param, EAS_I32 *pValue)
{
    EAS_RESULT result;

    /* call SMF parser to get value */
    if ((result = SMF_GetData(pEASData, ((S_XMF_DATA*) pInstData)->pSMFData, param, pValue)) != EAS_SUCCESS)
        return result;

    /* special case for file type */
    if (param == PARSER_DATA_FILE_TYPE)
    {
        if (*pValue == EAS_FILE_SMF0)
            *pValue = EAS_FILE_XMF0;
        else if (*pValue == EAS_FILE_SMF1)
            *pValue = EAS_FILE_XMF1;
    }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * XMF_FindFileContents()
 *----------------------------------------------------------------------------
 * Purpose:
 * Finds SMF data and DLS data in XMF file, and remembers offset for each.
 * If more than one is found, uses the first one found of each.
 * Makes assumptions about the format of a mobile XMF file
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * pXMFData         - pointer to XMF parser instance data
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT XMF_FindFileContents (EAS_HW_DATA_HANDLE hwInstData, S_XMF_DATA *pXMFData)
{
    EAS_RESULT result;
    EAS_I32 value;
    EAS_I32 treeStart;
    EAS_I32 treeEnd;
    EAS_I32 length;
    EAS_I32 node_depth = 0 ;
    EAS_I32 fileLength;
    EAS_U32 remainingBytes;

    /* initialize offsets */
    pXMFData->dlsOffset = pXMFData->midiOffset = 0;

    /* read file length. We're arbitrarily limiting the size of this field to
     * 16 bytes, since we don't yet have an actual limit. Once the field is
     * read, we correct remainingBytes using the actual value that we had read.
     * Since upon return from XMF_ReadVLQ(), remainingBytes represents how much
     * is left unread, it will be set to (16 - size_of_field). Therefore, size
     * of field is (16 - remainingBytes), which is what we need to subtract from
     * the total size. */
    const EAS_U32 kInitialRemainingBytes = 16;

    remainingBytes = kInitialRemainingBytes;
    if ((result = XMF_ReadVLQ(hwInstData,
                              pXMFData->fileHandle,
                              &remainingBytes,
                              &fileLength)) != EAS_SUCCESS)
        return result;
    if (fileLength < 0)
        return EAS_ERROR_FILE_FORMAT;
    remainingBytes = fileLength + remainingBytes - kInitialRemainingBytes;

    /* read MetaDataTypesTable length and skip over it */
    if ((result = XMF_ReadVLQ(hwInstData, pXMFData->fileHandle, &remainingBytes, &value)) != EAS_SUCCESS)
        return result;
    if (value > remainingBytes)
        return EAS_ERROR_FILE_FORMAT;
    if ((result = EAS_HWFileSeekOfs(hwInstData, pXMFData->fileHandle, value)) != EAS_SUCCESS)
        return result;
    remainingBytes -= value;

    /* get TreeStart and TreeEnd offsets */
    if ((result = XMF_ReadVLQ(hwInstData, pXMFData->fileHandle, &remainingBytes, &treeStart)) != EAS_SUCCESS)
        return result;
    if (treeStart < 0)
        return EAS_ERROR_FILE_FORMAT;

    if ((result = XMF_ReadVLQ(hwInstData, pXMFData->fileHandle, &remainingBytes, &treeEnd)) != EAS_SUCCESS)
        return result;
    if (treeEnd < treeStart || treeEnd >= fileLength)
        return EAS_ERROR_FILE_FORMAT;

    if ((result = XMF_ReadNode(hwInstData, pXMFData, treeStart, treeEnd + 1, &length, node_depth)) != EAS_SUCCESS)
        return result;

    /* check for SMF data */
    if (pXMFData->midiOffset == 0)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "No SMF data found in XMF file\n"); */ }
        return EAS_ERROR_FILE_FORMAT;
    }

    /* check for SFM in wrong order */
    if ((pXMFData->dlsOffset > 0) && (pXMFData->midiOffset < pXMFData->dlsOffset))
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "DLS data must precede SMF data in Mobile XMF file\n"); */ }

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * XMF_ReadNode()
 *----------------------------------------------------------------------------
 * Purpose:
 *
 * Inputs:
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT XMF_ReadNode (EAS_HW_DATA_HANDLE hwInstData, S_XMF_DATA *pXMFData, EAS_I32 nodeOffset, EAS_I32 endOffset, EAS_I32 *pLength, EAS_I32 depth)
{
    EAS_RESULT result;
    EAS_I32 refType;
    EAS_I32 numItems;
    EAS_I32 offset;
    EAS_I32 length;
    EAS_I32 headerLength;
    EAS_U32 chunkType;
    EAS_U32 remainingInlineBytes;
    EAS_U32 deltaBytes;

    /* check the depth of current node*/
    if ( depth > 100 )
        return EAS_ERROR_FILE_FORMAT;

    /* this will be used to check we don't read past the node limits */
    remainingInlineBytes = endOffset - nodeOffset;

    /* seek to start of node */
    if ((result = EAS_HWFileSeek(hwInstData, pXMFData->fileHandle, nodeOffset)) != EAS_SUCCESS)
        return result;

    /* get node length */
    if ((result = XMF_ReadVLQ(hwInstData, pXMFData->fileHandle, &remainingInlineBytes, pLength)) != EAS_SUCCESS)
        return result;
    if ((*pLength < 0) || (*pLength > endOffset - nodeOffset))
        return EAS_ERROR_FILE_FORMAT;

    /* recalculate our end offset and remaining bytes. */
    deltaBytes = endOffset - nodeOffset - *pLength;
    if (remainingInlineBytes < deltaBytes)
        return EAS_ERROR_FILE_FORMAT;

    remainingInlineBytes -= deltaBytes;
    endOffset = nodeOffset + *pLength;

    /* get number of contained items */
    if ((result = XMF_ReadVLQ(hwInstData, pXMFData->fileHandle, &remainingInlineBytes, &numItems)) != EAS_SUCCESS)
        return result;
    if (numItems < 0)
        return EAS_ERROR_FILE_FORMAT;

    /* get node header length */
    if ((result = XMF_ReadVLQ(hwInstData, pXMFData->fileHandle, &remainingInlineBytes, &headerLength)) != EAS_SUCCESS)
        return result;
    if (headerLength < 0 || headerLength > *pLength)
        return EAS_ERROR_FILE_FORMAT;

    /* get metadata length */
    if ((result = XMF_ReadVLQ(hwInstData, pXMFData->fileHandle, &remainingInlineBytes, &length)) != EAS_SUCCESS)
        return result;
    if (length < 0)
        return EAS_ERROR_FILE_FORMAT;

    /* get the current location */
    if ((result = EAS_HWFilePos(hwInstData, pXMFData->fileHandle, &offset)) != EAS_SUCCESS)
        return result;

    /* check that we didn't go past the header. */
    if (offset - nodeOffset > headerLength)
        return EAS_FAILURE;

    /* skip to node contents */
    if ((result = EAS_HWFileSeek(hwInstData, pXMFData->fileHandle, nodeOffset + headerLength)) != EAS_SUCCESS)
        return result;
    remainingInlineBytes = endOffset - (nodeOffset + headerLength);

    /* get reference type */
    if ((result = XMF_ReadVLQ(hwInstData, pXMFData->fileHandle, &remainingInlineBytes, &refType)) != EAS_SUCCESS)
        return result;

    /* get the current location */
    if ((result = EAS_HWFilePos(hwInstData, pXMFData->fileHandle, &offset)) != EAS_SUCCESS)
        return result;

    /* process file node */
    if (numItems == 0)

    {
        /* if in-file resource, find out where it is and jump to it */
        if (refType == 2)
        {
            if ((result = XMF_ReadVLQ(hwInstData, pXMFData->fileHandle, &remainingInlineBytes, &offset)) != EAS_SUCCESS)
                return result;
            offset += pXMFData->fileOffset;
            if ((result = EAS_HWFileSeek(hwInstData, pXMFData->fileHandle, offset)) != EAS_SUCCESS)
                return result;
        }

        /* or else it must be an inline resource */
        else if (refType != 1)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Unexpected reference type %d\n", refType); */ }
            return EAS_ERROR_FILE_FORMAT;
        }

        /* at this point we stop enforcing reading past the node. */

        /* get the chunk type */
        if ((result = EAS_HWGetDWord(hwInstData, pXMFData->fileHandle, &chunkType, EAS_TRUE)) != EAS_SUCCESS)
            return result;

        /* found a RIFF chunk, check for DLS type */
        if (chunkType == XMF_RIFF_CHUNK)
        {
            /* skip length */
            if ((result = EAS_HWFileSeekOfs(hwInstData, pXMFData->fileHandle, 4)) != EAS_SUCCESS)
                return result;

            /* get RIFF file type */
            if ((result = EAS_HWGetDWord(hwInstData, pXMFData->fileHandle, &chunkType, EAS_TRUE)) != EAS_SUCCESS)
                return result;
            if (chunkType == XMF_RIFF_DLS)
                pXMFData->dlsOffset = offset;
        }

        /* found an SMF chunk */
        else if (chunkType == XMF_SMF_CHUNK)
            pXMFData->midiOffset = offset;
    }

    /* folder node, process the items in the list */
    else
    {
        for ( ; numItems > 0; numItems--)
        {
            /* process this item */
            if ((result = XMF_ReadNode(hwInstData, pXMFData, offset, endOffset, &length, depth+1)) != EAS_SUCCESS)
                return result;

            /* seek to start of next item */
            offset += length;
            if ((result = EAS_HWFileSeek(hwInstData, pXMFData->fileHandle, offset)) != EAS_SUCCESS)
                return result;
        }
    }

    return EAS_SUCCESS;
}

#if 0
/*----------------------------------------------------------------------------
 * XMF_FindFileContents()
 *----------------------------------------------------------------------------
 * Purpose:
 * Finds SMF data and DLS data in XMF file, and remembers offset for each.
 * If more than one is found, uses the first one found of each.
 * Makes assumptions about the format of a mobile XMF file
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * pXMFData         - pointer to XMF parser instance data
 * handle           - pointer to file handle
 *
 * Outputs:
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT XMF_FindFileContents(S_EAS_DATA *pEASData, S_XMF_DATA *pXMFData, EAS_FILE_HANDLE fileHandle)
{
    EAS_RESULT result;
    EAS_I32 offset;
    EAS_I32 value;
    EAS_I32 numItems;
    EAS_I32 length;
    EAS_CHAR id[4];
    EAS_I32 location;

    /* init dls offset, so that we know we haven't found a dls chunk yet */
    pXMFData->dlsOffset = 0;

    /* read file length, ignore it for now */
    if ((result = XMF_ReadVLQ(pEASData, fileHandle, &value)) != EAS_SUCCESS)
        return result;

    /* read MetaDataTypesTable length and skip over it */
    if ((result = XMF_ReadVLQ(pEASData, fileHandle, &value)) != EAS_SUCCESS)
        return result;
    if ((result = EAS_HWFileSeekOfs(pEASData, fileHandle, value)) != EAS_SUCCESS)
        return result;

    /* get TreeStart offset and jump to it */
    if ((result = XMF_ReadVLQ(pEASData, fileHandle, &offset)) != EAS_SUCCESS)
        return result;
    if ((result = EAS_HWFileSeek(pEASData->hwInstData, fileHandle, offset)) != EAS_SUCCESS)
        return result;

    /* read node length, ignore it for now */
    if ((result = XMF_ReadVLQ(pEASData, fileHandle, &value)) != EAS_SUCCESS)
        return result;

    /* read number of contained items */
    if ((result = XMF_ReadVLQ(pEASData, fileHandle, &numItems)) != EAS_SUCCESS)
        return result;

    /*read node header length */
    if ((result = XMF_ReadVLQ(pEASData, fileHandle, &value)) != EAS_SUCCESS)
        return result;

    /*go to the node offset */
    if ((result = EAS_HWFileSeek(pEASData->hwInstData, fileHandle, offset + value)) != EAS_SUCCESS)
        return result;

    /* read Reference Type */
    if ((result = XMF_ReadVLQ(pEASData, fileHandle, &value)) != EAS_SUCCESS)
        return result;

    /* make sure it is an in-line resource, for now */
    if (value != 1)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Problem parsing XMF file tree\n"); */ }
        return EAS_FAILURE;
    }

    /* parse through the list of items */
    while (numItems > 0)
    {
        /*get current offset */
        if ((result = EAS_HWFilePos(pEASData->hwInstData, fileHandle, &offset)) != EAS_SUCCESS)
            return result;

        /*read node length */
        if ((result = XMF_ReadVLQ(pEASData, fileHandle, &length)) != EAS_SUCCESS)
            return result;

        /* read number of items */
        if ((result = XMF_ReadVLQ(pEASData, fileHandle, &value)) != EAS_SUCCESS)
            return result;

        /* make sure not a folder */
        if (value != 0)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Problem parsing XMF file node\n"); */ }
            return EAS_FAILURE;
        }

        /* read offset to resource and jump to it */
        if ((result = XMF_ReadVLQ(pEASData, fileHandle, &value)) != EAS_SUCCESS)
            return result;
        if ((result = EAS_HWFileSeek(pEASData->hwInstData, fileHandle, offset + value)) != EAS_SUCCESS)
            return result;

        /* read Reference Type */
        if ((result = XMF_ReadVLQ(pEASData, fileHandle, &value)) != EAS_SUCCESS)
            return result;

        /* make sure it is an in-line resource */
        if (value != 1)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Problem parsing XMF file node\n"); */ }
            return EAS_FAILURE;
        }

        /* get current offset as a possible location for SMF file or DLS file */
        if ((result = EAS_HWFilePos(pEASData->hwInstData, fileHandle, &location)) != EAS_SUCCESS)
            return result;

        /* read four bytes */
        if ((result = EAS_HWReadFile(pEASData->hwInstData, fileHandle, id, sizeof(id), &value)) != EAS_SUCCESS)
            return result;

        /* check if DLS */
        if (pXMFData->dlsOffset == 0 && id[0] == 'R' && id[1] == 'I' && id[2] == 'F' && id[3] == 'F')
        {
            //remember offset
            pXMFData->dlsOffset = location;
        }

        /* else check if SMF */
        else if (id[0] == 'M' && id[1] == 'T' && id[2] == 'h' && id[3] == 'd')
        {
            //remember offset
            pXMFData->midiOffset = location;

            //we are done
            return EAS_SUCCESS;
        }

        //one less item
        numItems--;

        //if more data, go to the next item
        if (numItems >0)
        {
            if ((result = EAS_HWFileSeek(pEASData->hwInstData, fileHandle, offset + length)) != EAS_SUCCESS)
                return result;
        }
    }

    return EAS_FAILURE;

}
#endif

/*----------------------------------------------------------------------------
 * XMF_ReadVLQ()
 *----------------------------------------------------------------------------
 * Purpose:
 * Reads a VLQ encoded value from the file referenced by fileHandle
 *
 * Inputs:
 * pEASData         - pointer to overall EAS data structure
 * fileHandle       - pointer to file handle
 *
 * Outputs:
 * value            - pointer to the value decoded from the VLQ data
 *
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_RESULT XMF_ReadVLQ (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE fileHandle, EAS_U32 *remainingBytes, EAS_I32 *value)
{
    EAS_RESULT result;
    EAS_U8 c;
    *value = 0;

    if ((*remainingBytes)-- == 0)
        return EAS_ERROR_FILE_FORMAT;

    if ((result = EAS_HWGetByte(hwInstData, fileHandle, &c)) != EAS_SUCCESS)
        return result;

    while (c > 0x7F)
    {
        /*lint -e{703} shift for performance */
        *value = (*value << 7) | (c & 0x7F);

        if ((*remainingBytes)-- == 0)
            return EAS_ERROR_FILE_FORMAT;

        if ((result = EAS_HWGetByte(hwInstData, fileHandle, &c)) != EAS_SUCCESS)
            return result;
    }

    /*lint -e{703} shift for performance */
    *value = (*value << 7) | c;

    return EAS_SUCCESS;
}

