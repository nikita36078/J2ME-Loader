/*----------------------------------------------------------------------------
 *
 * File:
 * eas_pcmdata.h
 *
 * Contents and purpose:
 * Data declarations for the PCM engine
 *
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
 *   $Revision: 847 $
 *   $Date: 2007-08-27 21:30:08 -0700 (Mon, 27 Aug 2007) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_PCMDATA_H
#define _EAS_PCMDATA_H

/* sets the maximum number of simultaneous PCM streams */
#ifndef MAX_PCM_STREAMS
#define MAX_PCM_STREAMS             16
#define PCM_STREAM_THRESHOLD        (MAX_PCM_STREAMS - 4)
#endif

/* coefficents for high-pass filter in ADPCM */
#define INTEGRATOR_COEFFICIENT      100     /* coefficient for leaky integrator */

/* additional flags in S_PCM_STATE.flags used internal to module */
#define PCM_FLAGS_EMPTY             0x01000000  /* unsigned format */

/*----------------------------------------------------------------------------
 * S_PCM_STATE
 *
 * Retains state information for PCM streams.
 *----------------------------------------------------------------------------
*/
typedef struct s_decoder_state_tag
{
    EAS_I32             output;             /* last output for DC offset filter */
    EAS_I32             acc;                /* accumulator for DC offset filter */
    EAS_I32             step;               /* current ADPCM step size */
    EAS_PCM             x1;                 /* current generated sample */
    EAS_PCM             x0;                 /* previous generated sample */
} S_DECODER_STATE;

typedef enum
{
    PCM_ENV_START = 0,
    PCM_ENV_ATTACK,
    PCM_ENV_DECAY,
    PCM_ENV_SUSTAIN,
    PCM_ENV_RELEASE,
    PCM_ENV_END
} E_PCM_ENV_STATE;

typedef struct s_pcm_state_tag
{
#ifdef _CHECKED_BUILD
    EAS_U32             handleCheck;        /* signature check for checked build */
#endif
    EAS_FILE_HANDLE     fileHandle;         /* pointer to input file */
    EAS_PCM_CALLBACK    pCallback;          /* pointer to callback function */
    EAS_VOID_PTR        cbInstData;         /* instance data for callback function */
    struct s_decoder_interface_tag EAS_CONST * pDecoder;    /* pointer to decoder interface */
    EAS_STATE           state;              /* stream state */
    EAS_I32             time;               /* media time */
    EAS_I32             startPos;           /* start of PCM stream */
    EAS_I32             loopLocation;       /* file location where loop starts */
    EAS_I32             byteCount;          /* size of file */
    EAS_U32             loopStart;          /* loop start, offset in samples from startPos */
                                            /* NOTE: For CMF, we use this to store total sample size */
    EAS_U32             loopSamples;        /* total loop length, in samples, 0 means no loop */
                                            /* NOTE: For CMF, non-zero means looped */
    EAS_U32             samplesInLoop;      /* samples left in the loop to play back */
    EAS_I32             samplesTilLoop;     /* samples left to play until top of loop */
    EAS_I32             bytesLeft;          /* count of bytes left in stream */
    EAS_I32             bytesLeftLoop;      /* count of bytes left in stream, value at start of loop */
    EAS_U32             phase;              /* current phase for interpolator */
    EAS_U32             basefreq;           /* frequency multiplier */
    EAS_U32             flags;              /* stream flags */
    EAS_U32             envData;            /* envelope data (and LFO data) */
    EAS_U32             envValue;           /* current envelope value */
    EAS_U32             envScale;           /* current envelope scale */
    EAS_U32             startOrder;         /* start order index, first is 0, next is 1, etc. */
    S_DECODER_STATE     decoderL;           /* left (mono) ADPCM state */
    S_DECODER_STATE     decoderR;           /* right ADPCM state */
    S_DECODER_STATE     decoderLLoop;       /* left (mono) ADPCM state, value at start of loop */
    S_DECODER_STATE     decoderRLoop;       /* right ADPCM state, value at start of loop */
    E_PCM_ENV_STATE     envState;           /* current envelope state */
    EAS_I16             volume;             /* volume for stream */
    EAS_I16             pitch;              /* relative pitch in cents - zero is unity playback */
    EAS_I16             gainLeft;           /* requested gain */
    EAS_I16             gainRight;          /* requested gain */
    EAS_I16             currentGainLeft;    /* current gain for anti-zipper filter */
    EAS_I16             currentGainRight;   /* current gain for anti-zipper filter */
    EAS_U16             blockSize;          /* block size for ADPCM decoder */
    EAS_U16             blockCount;         /* block counter for ADPCM decoder */
    EAS_U16             sampleRate;         /* input sample rate */
    EAS_U8              srcByte;            /* source byte */
    EAS_U8              msBitCount;         /* count keeps track of MS bits */
    EAS_U8              msBitMask;          /* mask keeps track of MS bits */
    EAS_U8              msBitValue;         /* value keeps track of MS bits */
    EAS_U8              msBitCountLoop;     /* count keeps track of MS bits, value at loop start */
    EAS_U8              msBitMaskLoop;      /* mask keeps track of MS bits, value at loop start */
    EAS_U8              msBitValueLoop;     /* value keeps track of MS bits, value at loop start */
    EAS_BOOL8           hiNibble;           /* indicates high/low nibble is next */
    EAS_BOOL8           hiNibbleLoop;       /* indicates high/low nibble is next, value loop start */
    EAS_U8              rateShift;          /* for playback rate greater than 1.0 */
} S_PCM_STATE;

/*----------------------------------------------------------------------------
 * S_DECODER_INTERFACE
 *
 * Generic interface for audio decoders
 *----------------------------------------------------------------------------
*/
typedef struct s_decoder_interface_tag
{
    EAS_RESULT (* EAS_CONST pfInit)(EAS_DATA_HANDLE pEASData, S_PCM_STATE *pState);
    EAS_RESULT (* EAS_CONST pfDecodeSample)(EAS_DATA_HANDLE pEASData, S_PCM_STATE *pState);
    EAS_RESULT (* EAS_CONST pfLocate)(EAS_DATA_HANDLE pEASData, S_PCM_STATE *pState, EAS_I32 time);
} S_DECODER_INTERFACE;


/* header chunk for SMAF ADPCM */
#define TAG_YAMAHA_ADPCM    0x4d776100
#define TAG_MASK            0xffffff00
#define TAG_RIFF_FILE       0x52494646
#define TAG_WAVE_CHUNK      0x57415645
#define TAG_FMT_CHUNK       0x666d7420

/*----------------------------------------------------------------------------
 * EAS_PESeek
 *----------------------------------------------------------------------------
 * Purpose:
 * Locate to a particular byte in a PCM stream
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_PESeek (EAS_DATA_HANDLE pEASData, S_PCM_STATE *pState, EAS_I32 *pLocation);

#endif /* _EAS_PCMDATA_H */

