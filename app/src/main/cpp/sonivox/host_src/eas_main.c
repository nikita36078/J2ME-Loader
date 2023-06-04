/*----------------------------------------------------------------------------
 *
 * File:
 * eas_main.c
 *
 * Contents and purpose:
 * The entry point and high-level functions for the EAS Synthesizer test
 * harness.
 *
 * Copyright Sonic Network Inc. 2004

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
 *   $Revision: 775 $
 *   $Date: 2007-07-20 10:11:11 -0700 (Fri, 20 Jul 2007) $
 *----------------------------------------------------------------------------
*/

#ifdef _lint
#include "lint_stdlib.h"
#else
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>
#endif

#include "eas.h"
#include "eas_wave.h"
#include "eas_report.h"

/* determines how many EAS buffers to fill a host buffer */
#define NUM_BUFFERS         8

/* default file to play if no filename is specified on the command line */
static const char defaultTestFile[] = "test.mid";

EAS_I32 polyphony;

/* prototypes for helper functions */
static void StrCopy(char *dest, const char *src, EAS_I32 size);
static EAS_BOOL ChangeFileExt(char *str, const char *ext, EAS_I32 size);
static EAS_RESULT PlayFile (EAS_DATA_HANDLE easData, const char* filename, const char* outputFile, const S_EAS_LIB_CONFIG *pLibConfig, void *buffer, EAS_I32 bufferSize);
static EAS_BOOL EASLibraryCheck (const S_EAS_LIB_CONFIG *pLibConfig);

/* main is defined after playfile to avoid the need for two passes through lint */

/*----------------------------------------------------------------------------
 * PlayFile()
 *----------------------------------------------------------------------------
 * Purpose:
 * This function plays the file requested by filename
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/

static EAS_RESULT PlayFile (EAS_DATA_HANDLE easData, const char* filename, const char* outputFile, const S_EAS_LIB_CONFIG *pLibConfig, void *buffer, EAS_I32 bufferSize)
{
    EAS_HANDLE handle;
    EAS_RESULT result, reportResult;
    EAS_I32 count;
    EAS_STATE state;
    EAS_I32 playTime;
    char waveFilename[256];
    WAVE_FILE *wFile;
    EAS_INT i;
    EAS_PCM *p;
    EAS_FILE file;

    /* determine the name of the output file */
    wFile = NULL;
    if (outputFile == NULL)
    {
        StrCopy(waveFilename, filename, sizeof(waveFilename));
        if (!ChangeFileExt(waveFilename, "wav", sizeof(waveFilename)))
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Error in output filename %s\n", waveFilename); */ }
            return EAS_FAILURE;
        }
        outputFile = waveFilename;
    }

    /* call EAS library to open file */
    file.path = filename;
    file.fd = 0;
    if ((reportResult = EAS_OpenFile(easData, &file, &handle)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "EAS_OpenFile returned %ld\n", reportResult); */ }
        return reportResult;
    }

    /* prepare to play the file */
    if ((result = EAS_Prepare(easData, handle)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "EAS_Prepare returned %ld\n", result); */ }
        reportResult = result;
    }

    /* get play length */
    if ((result = EAS_ParseMetaData(easData, handle, &playTime)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "EAS_ParseMetaData returned %ld\n", result); */ }
        return result;
    }
    EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0xe624f4d9, 0x00000005 , playTime / 1000, playTime % 1000);

    if (reportResult == EAS_SUCCESS)
    {
        /* create the output file */
        wFile = WaveFileCreate(outputFile, pLibConfig->numChannels, pLibConfig->sampleRate, sizeof(EAS_PCM) * 8);
        if (!wFile)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Unable to create output file %s\n", waveFilename); */ }
            reportResult = EAS_FAILURE;
        }
    }

    /* rendering loop */
    while (reportResult == EAS_SUCCESS)
    {

        /* we may render several buffers here to fill one host buffer */
        for (i = 0, p = buffer; i < NUM_BUFFERS; i++, p+= pLibConfig->mixBufferSize * pLibConfig->numChannels)
        {

            /* get the current time */
            if ((result = EAS_GetLocation(easData, handle, &playTime)) != EAS_SUCCESS)
            {
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "EAS_GetLocation returned %d\n",result); */ }
                if (reportResult == EAS_SUCCESS)
                    reportResult = result;
                break;
            }
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_DETAIL, "Parser time: %d.%03d\n", playTime / 1000, playTime % 1000); */ }

            /* render a buffer of audio */
            if ((result = EAS_Render(easData, p, pLibConfig->mixBufferSize, &count)) != EAS_SUCCESS)
            {
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "EAS_Render returned %d\n",result); */ }
                if (reportResult == EAS_SUCCESS)
                    reportResult = result;
            }
        }

        if (result == EAS_SUCCESS)
        {
            /* write it to the wave file */
            if (WaveFileWrite(wFile, buffer, bufferSize) != bufferSize)
            {
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "WaveFileWrite failed\n"); */ }
                reportResult = EAS_FAILURE;
            }
        }

        if (reportResult == EAS_SUCCESS)
        {
            /* check stream state */
            if ((result = EAS_State(easData, handle, &state)) != EAS_SUCCESS)
            {
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "EAS_State returned %d\n", result); */ }
                reportResult = result;
            }

            /* is playback complete */
            if ((state == EAS_STATE_STOPPED) || (state == EAS_STATE_ERROR))
                break;
        }
    }

    /* close the output file */
    if (wFile)
    {
        if (!WaveFileClose(wFile))
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Error closing wave file %s\n", waveFilename); */ }
            if (reportResult == EAS_SUCCESS)
                result = EAS_FAILURE;
        }
    }

    /* close the input file */
    if ((result = EAS_CloseFile(easData,handle)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "EAS_Close returned %ld\n", result); */ }
        if (reportResult == EAS_SUCCESS)
            result = EAS_FAILURE;
    }

    return reportResult;
} /* end PlayFile */

/*----------------------------------------------------------------------------
 * main()
 *----------------------------------------------------------------------------
 * Purpose: The entry point for the EAS sample application
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
int main( int argc, char **argv )
{
    EAS_DATA_HANDLE easData;
    const S_EAS_LIB_CONFIG *pLibConfig;
    void *buffer;
    EAS_RESULT result, playResult;
    EAS_I32 bufferSize;
    int i;
    int temp;
    FILE *debugFile;
    char *outputFile = NULL;

    /* set the error reporting level */
    EAS_SetDebugLevel(_EAS_SEVERITY_INFO);
    debugFile = NULL;

    /* process command-line arguments */
    for (i = 1; i < argc; i++)
    {
        /* check for switch */
        if (argv[i][0] == '-')
        {
            switch (argv[i][1])
            {
            case 'd':
                temp = argv[i][2];
                if ((temp >= '0') || (temp <= '9'))
                    EAS_SetDebugLevel(temp);
                else
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Invalid debug level %d\n", temp); */ }
                break;
            case 'f':
                if ((debugFile = fopen(&argv[i][2],"w")) == NULL)
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_WARNING, "Unable to create debug file %s\n", &argv[i][2]); */ }
                else
                    EAS_SetDebugFile(debugFile, EAS_TRUE);
                break;
            case 'o':
                outputFile = &argv[i][2];
                break;
            case 'p':
                polyphony = atoi(&argv[i][2]);
                if (polyphony < 1)
                    polyphony = 1;
                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "Polyphony set to %d\n", polyphony); */ }
                break;
            default:
                break;
            }
            continue;
        }
    }

    /* assume success */
    playResult = EAS_SUCCESS;

    /* get the library configuration */
    pLibConfig = EAS_Config();
    if (!EASLibraryCheck(pLibConfig))
        return -1;
    if (polyphony > pLibConfig->maxVoices)
        polyphony = pLibConfig->maxVoices;

    /* calculate buffer size */
    bufferSize = pLibConfig->mixBufferSize * pLibConfig->numChannels * (EAS_I32)sizeof(EAS_PCM) * NUM_BUFFERS;

    /* allocate output buffer memory */
    buffer = malloc((EAS_U32)bufferSize);
    if (!buffer)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_FATAL, "Error allocating memory for audio buffer\n"); */ }
        return EAS_FAILURE;
    }

    /* initialize the EAS library */
    polyphony = pLibConfig->maxVoices;
    if ((result = EAS_Init(&easData)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_FATAL, "EAS_Init returned %ld - aborting!\n", result); */ }
        free(buffer);
        return result;
    }

    /*
     * Some debugging environments don't allow for passed parameters.
     * In this case, just play the default MIDI file "test.mid"
     */
    if (argc < 2)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "Playing '%s'\n", defaultTestFile); */ }
        if ((playResult = PlayFile(easData, defaultTestFile, NULL, pLibConfig, buffer, bufferSize)) != EAS_SUCCESS)
        {
            { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Error %d playing file %s\n", playResult, defaultTestFile); */ }
        }
    }
    /* iterate through the list of files to be played */
    else
    {
        for (i = 1; i < argc; i++)
        {
            /* check for switch */
            if (argv[i][0] != '-')
            {

                { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "Playing '%s'\n", argv[i]); */ }
                if ((playResult = PlayFile(easData, argv[i], outputFile, pLibConfig, buffer, bufferSize)) != EAS_SUCCESS)
                {
                    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_ERROR, "Error %d playing file %s\n", playResult, argv[i]); */ }
                    break;
                }
            }
        }
    }

    /* shutdown the EAS library */
    if ((result = EAS_Shutdown(easData)) != EAS_SUCCESS)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_FATAL, "EAS_Shutdown returned %ld\n", result); */ }
    }

    /* free the output buffer */
    free(buffer);

    /* close the debug file */
    if (debugFile)
        fclose(debugFile);

    /* play errors take precedence over shutdown errors */
    if (playResult != EAS_SUCCESS)
        return playResult;
    return result;
} /* end main */

/*----------------------------------------------------------------------------
 * StrCopy()
 *----------------------------------------------------------------------------
 * Purpose:
 * Safe string copy
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
static void StrCopy(char *dest, const char *src, EAS_I32 size)
{
    int len;

    strncpy(dest, src, (size_t) size-1);
    len = (int) strlen(src);
    if (len < size)
        dest[len] = 0;
} /* end StrCopy */

/*----------------------------------------------------------------------------
 * ChangeFileExt()
 *----------------------------------------------------------------------------
 * Purpose:
 * Changes the file extension of a filename
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
static EAS_BOOL ChangeFileExt(char *str, const char *ext, EAS_I32 size)
{
    char *p;

    /* find the extension, if any */
    p = strrchr(str,'.');
    if (!p)
    {
        if ((EAS_I32)(strlen(str) + 5) > size)
            return EAS_FALSE;
        strcat(str,".");
        strcat(str,ext);
        return EAS_TRUE;
    }

    /* make sure there's room for the extension */
    p++;
    *p = 0;
    if ((EAS_I32)(strlen(str) + 4) > size)
        return EAS_FALSE;
    strcat(str,ext);
    return EAS_TRUE;
} /* end ChangeFileExt */

/*----------------------------------------------------------------------------
 * EASLibraryCheck()
 *----------------------------------------------------------------------------
 * Purpose:
 * Displays the library version and checks it against the header
 * file used to build this code.
 *
 * Inputs:
 * pLibConfig       - library configuration retrieved from the library
 *
 * Outputs:
 * returns EAS_TRUE if matched
 *
 * Side Effects:
 *
 *----------------------------------------------------------------------------
*/
static EAS_BOOL EASLibraryCheck (const S_EAS_LIB_CONFIG *pLibConfig)
{

    /* display the library version */
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "EAS Library Version %d.%d.%d.%d\n",
        pLibConfig->libVersion >> 24,
        (pLibConfig->libVersion >> 16) & 0x0f,
        (pLibConfig->libVersion >> 8) & 0x0f,
        pLibConfig->libVersion & 0x0f); */ }

    /* display some info about the library build */
    if (pLibConfig->checkedVersion)
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "\tChecked library\n"); */ }
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "\tMaximum polyphony: %d\n", pLibConfig->maxVoices); */ }
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "\tNumber of channels: %d\n", pLibConfig->numChannels); */ }
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "\tSample rate: %d\n", pLibConfig->sampleRate); */ }
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "\tMix buffer size: %d\n", pLibConfig->mixBufferSize); */ }
    if (pLibConfig->filterEnabled)
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "\tFilter enabled\n"); */ }
#ifndef _WIN32_WCE
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "\tLibrary Build Timestamp: %s", ctime((time_t*)&pLibConfig->buildTimeStamp)); */ }
#endif
    { /* dpp: EAS_ReportEx(_EAS_SEVERITY_INFO, "\tLibrary Build ID: %s\n", pLibConfig->buildGUID); */ }

    /* check it against the header file used to build this code */
    /*lint -e{778} constant expression used for display purposes may evaluate to zero */
    if (LIB_VERSION != pLibConfig->libVersion)
    {
        { /* dpp: EAS_ReportEx(_EAS_SEVERITY_FATAL, "Library version does not match header files. EAS Header Version %d.%d.%d.%d\n",
            LIB_VERSION >> 24,
            (LIB_VERSION >> 16) & 0x0f,
            (LIB_VERSION >> 8) & 0x0f,
            LIB_VERSION & 0x0f); */ }
        return EAS_FALSE;
    }
    return EAS_TRUE;
} /* end EASLibraryCheck */

