/*----------------------------------------------------------------------------
 *
 * File:
 * eas_wave.c
 *
 * Contents and purpose:
 * This module contains .WAV file functions for the EAS synthesizer
 * test harness.
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
 *----------------------------------------------------------------------------
 * Revision Control:
 *   $Revision: 658 $
 *   $Date: 2007-04-24 13:35:49 -0700 (Tue, 24 Apr 2007) $
 *----------------------------------------------------------------------------
*/

/* lint complaints about most C library headers, so we use our own during lint step */
#ifdef _lint
#include "lint_stdlib.h"
#else
#include <stdio.h>
#include <stdlib.h>
#endif

#include "eas_wave.h"

/* .WAV file format tags */
const EAS_U32 riffTag = 0x46464952;
const EAS_U32 waveTag = 0x45564157;
const EAS_U32 fmtTag = 0x20746d66;
const EAS_U32 dataTag = 0x61746164;

#ifdef _BIG_ENDIAN
/*----------------------------------------------------------------------------
 * FlipDWord()
 *----------------------------------------------------------------------------
 * Purpose: Endian flip a DWORD for big-endian processors
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
static void FlipDWord (EAS_U32 *pValue)
{
    EAS_U8 *p;
    EAS_U32 temp;

    p = (EAS_U8*) pValue;
    temp = (((((p[3] << 8) | p[2]) << 8) | p[1]) << 8) | p[0];
    *pValue = temp;
}

/*----------------------------------------------------------------------------
 * FlipWord()
 *----------------------------------------------------------------------------
 * Purpose: Endian flip a WORD for big-endian processors
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
static void FlipWord (EAS_U16 *pValue)
{
    EAS_U8 *p;
    EAS_U16 temp;

    p = (EAS_U8*) pValue;
    temp = (p[1] << 8) | p[0];
    *pValue = temp;
}

/*----------------------------------------------------------------------------
 * FlipWaveHeader()
 *----------------------------------------------------------------------------
 * Purpose: Endian flip the wave header for big-endian processors
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
static void FlipWaveHeader (WAVE_HEADER *p)
{

    FlipDWord(&p->nRiffTag);
    FlipDWord(&p->nRiffSize);
    FlipDWord(&p->nWaveTag);
    FlipDWord(&p->nFmtTag);
    FlipDWord(&p->nFmtSize);
    FlipDWord(&p->nDataTag);
    FlipDWord(&p->nDataSize);
    FlipWord(&p->fc.wFormatTag);
    FlipWord(&p->fc.nChannels);
    FlipDWord(&p->fc.nSamplesPerSec);
    FlipDWord(&p->fc.nAvgBytesPerSec);
    FlipWord(&p->fc.nBlockAlign);
    FlipWord(&p->fc.wBitsPerSample);

}
#endif

/*----------------------------------------------------------------------------
 * WaveFileCreate()
 *----------------------------------------------------------------------------
 * Purpose: Opens a wave file for writing and writes the header
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/

WAVE_FILE *WaveFileCreate (const char *filename, EAS_I32 nChannels, EAS_I32 nSamplesPerSec, EAS_I32 wBitsPerSample)
{
    WAVE_FILE *wFile;

    /* allocate memory */
    wFile = malloc(sizeof(WAVE_FILE));
    if (!wFile)
        return NULL;
    wFile->write = EAS_TRUE;

    /* create the file */
    wFile->file = fopen(filename,"wb");
    if (!wFile->file)
    {
        free(wFile);
        return NULL;
    }

    /* initialize PCM format .WAV file header */
    wFile->wh.nRiffTag = riffTag;
    wFile->wh.nRiffSize = sizeof(WAVE_HEADER) - 8;
    wFile->wh.nWaveTag = waveTag;
    wFile->wh.nFmtTag = fmtTag;
    wFile->wh.nFmtSize = sizeof(FMT_CHUNK);

    /* initalize 'fmt' chunk */
    wFile->wh.fc.wFormatTag = 1;
    wFile->wh.fc.nChannels = (EAS_U16) nChannels;
    wFile->wh.fc.nSamplesPerSec = (EAS_U32) nSamplesPerSec;
    wFile->wh.fc.wBitsPerSample = (EAS_U16) wBitsPerSample;
    wFile->wh.fc.nBlockAlign = (EAS_U16) (nChannels * (EAS_U16) (wBitsPerSample / 8));
    wFile->wh.fc.nAvgBytesPerSec = wFile->wh.fc.nBlockAlign * (EAS_U32) nSamplesPerSec;

    /* initialize 'data' chunk */
    wFile->wh.nDataTag = dataTag;
    wFile->wh.nDataSize = 0;

#ifdef _BIG_ENDIAN
    FlipWaveHeader(&wFile->wh);
#endif

    /* write the header */
    if (fwrite(&wFile->wh, sizeof(WAVE_HEADER), 1, wFile->file) != 1)
    {
        fclose(wFile->file);
        free(wFile);
        return NULL;
    }

#ifdef _BIG_ENDIAN
    FlipWaveHeader(&wFile->wh);
#endif

    /* return the file handle */
    return wFile;
} /* end WaveFileCreate */

/*----------------------------------------------------------------------------
 * WaveFileWrite()
 *----------------------------------------------------------------------------
 * Purpose: Writes data to the wave file
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_I32 WaveFileWrite (WAVE_FILE *wFile, void *buffer, EAS_I32 n)
{
    EAS_I32 count;

    /* make sure we have an open file */
    if (wFile == NULL)
    {
        return 0;
    }

#ifdef _BIG_ENDIAN
    {
        EAS_I32 i;
        EAS_U16 *p;
        p = buffer;
        i = n >> 1;
        while (i--)
            FlipWord(p++);
    }
#endif

    /* write the data */
    count = (EAS_I32) fwrite(buffer, 1, (size_t) n, wFile->file);

    /* add the number of bytes written */
    wFile->wh.nRiffSize += (EAS_U32) count;
    wFile->wh.nDataSize += (EAS_U32) count;

    /* return the count of bytes written */
    return count;
} /* end WriteWaveHeader */

/*----------------------------------------------------------------------------
 * WaveFileClose()
 *----------------------------------------------------------------------------
 * Purpose: Opens a wave file for writing and writes the header
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/

EAS_BOOL WaveFileClose (WAVE_FILE *wFile)
{
    EAS_I32 count = 1;

    /* return to beginning of file and write the header */
    if (wFile->write)
    {
        if (fseek(wFile->file, 0L, SEEK_SET) == 0)
        {

#ifdef _BIG_ENDIAN
            FlipWaveHeader(&wFile->wh);
#endif
            count = (EAS_I32) fwrite(&wFile->wh, sizeof(WAVE_HEADER), 1, wFile->file);
#ifdef _BIG_ENDIAN
            FlipWaveHeader(&wFile->wh);
#endif
        }
    }

    /* close the file */
    if (fclose(wFile->file) != 0)
        count = 0;

    /* free the memory */
    free(wFile);

    /* return the file handle */
    return (count == 1 ? EAS_TRUE : EAS_FALSE);
} /* end WaveFileClose */

#ifdef _WAVE_FILE_READ
#ifdef _BIG_ENDIAN
#error "WaveFileOpen not currently supported on big-endian processors"
#endif
/*----------------------------------------------------------------------------
 * WaveFileOpen()
 *----------------------------------------------------------------------------
 * Purpose: Opens a wave file for reading and reads the header
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/

WAVE_FILE *WaveFileOpen (const char *filename)
{
    WAVE_FILE *wFile;
    struct
    {
        EAS_U32 tag;
        EAS_U32 size;
    } chunk;
    EAS_U32 tag;
    EAS_I32 startChunkPos;
    EAS_INT state;
    EAS_BOOL done;

    /* allocate memory */
    wFile = malloc(sizeof(WAVE_FILE));
    if (!wFile)
        return NULL;

    /* open the file */
    wFile->write = EAS_FALSE;
    wFile->file = fopen(filename,"rb");
    if (!wFile->file)
    {
        free(wFile);
        return NULL;
    }

    /* make lint happy */
    chunk.tag = chunk.size = 0;
    startChunkPos = 0;

    /* read the RIFF tag and file size */
    state = 0;
    done = EAS_FALSE;
    while (!done)
    {

        switch(state)
        {
            /* read the RIFF tag */
            case 0:
                if (fread(&chunk, sizeof(chunk), 1, wFile->file) != 1)
                    done = EAS_TRUE;
                else
                {
                    if (chunk.tag != riffTag)
                        done = EAS_TRUE;
                    else
                        state++;
                }
                break;

            /* read the WAVE tag */
            case 1:
                if (fread(&tag, sizeof(tag), 1, wFile->file) != 1)
                    done = EAS_TRUE;
                else
                {
                    if (tag != waveTag)
                        done = EAS_TRUE;
                    else
                        state++;
                }
                break;

            /* looking for fmt chunk */
            case 2:
                if (fread(&chunk, sizeof(chunk), 1, wFile->file) != 1)
                    done = EAS_TRUE;
                else
                {
                    startChunkPos = ftell(wFile->file);

                    /* not fmt tag, skip it */
                    if (chunk.tag != fmtTag)
                        fseek(wFile->file, startChunkPos + (EAS_I32) chunk.size, SEEK_SET);
                    else
                        state++;
                }
                break;

            /* read fmt chunk */
            case 3:
                if (fread(&wFile->wh.fc, sizeof(FMT_CHUNK), 1, wFile->file) != 1)
                    done = EAS_TRUE;
                else
                {
                    fseek(wFile->file, startChunkPos + (EAS_I32) chunk.size, SEEK_SET);
                    state++;
                }
                break;

            /* looking for data chunk */
            case 4:
                if (fread(&chunk, sizeof(chunk), 1, wFile->file) != 1)
                    done = EAS_TRUE;
                else
                {
                    startChunkPos = ftell(wFile->file);

                    /* not data tag, skip it */
                    if (chunk.tag != dataTag)
                        fseek(wFile->file, startChunkPos + (EAS_I32) chunk.size, SEEK_SET);
                    else
                    {
                        wFile->dataSize = chunk.size;
                        state++;
                        done = EAS_TRUE;
                    }
                }
                break;

            default:
                done = EAS_TRUE;
                break;
        }
    }

    /* if not final state, an error occurred */
    if (state != 5)
    {
        fclose(wFile->file);
        free(wFile);
        return NULL;
    }

    /* return the file handle */
    return wFile;
} /* end WaveFileOpen */
#endif



