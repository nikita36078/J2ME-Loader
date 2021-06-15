/*----------------------------------------------------------------------------
 *
 * File:
 * eas_mdls.h
 *
 * Contents and purpose:
 * Declarations, interfaces, and prototypes for eas_mdls.c
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
*/

#ifndef _EAS_MDLS_H
#define _EAS_MDLS_H

/*------------------------------------
 * includes
 *------------------------------------
*/
#include "eas_data.h"


/*------------------------------------
 * Some defines for dls.h
 *------------------------------------
*/
#ifndef DWORD
#define DWORD EAS_I32
#define FAR
#define SHORT EAS_I16
#define USHORT EAS_U16
#define LONG EAS_I32
#define ULONG EAS_U32
#endif


/* GUID struct (call it DLSID in case GUID is defined elsewhere) */
typedef struct
{
    EAS_U32 Data1;
    EAS_U16 Data2;
    EAS_U16 Data3;
    EAS_U8  Data4[8];
} DLSID;

#define DEFINE_DLSID(name, l, w1, w2, b1, b2, b3, b4, b5, b6, b7, b8) const DLSID name = { l, w1, w2, { b1, b2, b3, b4, b5, b6, b7, b8 } }

/*------------------------------------
 * defines
 *------------------------------------
*/

/* maximum sample memory for DLS query support */
#ifndef MAX_DLS_MEMORY
#define MAX_DLS_MEMORY 65536
#endif

/* size of conditional chunk stack */
#ifndef CDL_STACK_SIZE
#define CDL_STACK_SIZE 8
#endif

/* size of read buffer for sample conversion */
#ifndef SAMPLE_CONVERT_CHUNK_SIZE
#define SAMPLE_CONVERT_CHUNK_SIZE 32
#endif


#define ZERO_TIME_IN_CENTS              -32768

/* Pan calculation macros */
#define PAN_CONVERSION_FACTOR       4129
#define MAX_PAN_VALUE               63
#define MIN_PAN_VALUE               -63

/* multiplier to convert time cents to 10-bit fraction log for EAS_LogToLinear16 */
#define TIME_CENTS_TO_LOG2          27962

/* conversion factor sustain level from percent to exponent for LogToLinear16 */
#define SUSTAIN_LOG_CONVERSION_FACTOR   536871
#define SUSTAIN_LOG_CONVERSION_SHIFT    15

/* conversion factor sustain level from percent to EG full scale */
#define SUSTAIN_LINEAR_CONVERSION_FACTOR    1073709

/* conversion factor to convert frame period to decay rate */
#define DECAY_CONVERSION_FACTOR     -16

/*----------------------------------------------------------------------------
 * These macros define the various characteristics of the defined sample rates
 *----------------------------------------------------------------------------
 * DLS_ATTACK_TIME_CONVERT      log offset for conversion from time cents to attack rate
 * DLS_LFO_FREQUENCY_CONVERT    pitch-cents offset for LFO frequency conversion
 *----------------------------------------------------------------------------
*/

#if defined (_SAMPLE_RATE_8000)
#define DLS_RATE_CONVERT                -9559
#define DLS_LFO_FREQUENCY_CONVERT       5921

#elif defined (_SAMPLE_RATE_16000)
#define DLS_RATE_CONVERT                -9559
#define DLS_LFO_FREQUENCY_CONVERT       5921

#elif defined (_SAMPLE_RATE_20000)
#define DLS_RATE_CONVERT                -8745
#define DLS_LFO_FREQUENCY_CONVERT       5108

#elif defined (_SAMPLE_RATE_22050)
#define DLS_RATE_CONVERT                -8914
#define DLS_LFO_FREQUENCY_CONVERT       5277

#elif defined (_SAMPLE_RATE_24000)
#define DLS_RATE_CONVERT                -9061
#define DLS_LFO_FREQUENCY_CONVERT       5423

#elif defined (_SAMPLE_RATE_32000)
#define DLS_RATE_CONVERT                -9559
#define DLS_LFO_FREQUENCY_CONVERT       5921

#elif defined (_SAMPLE_RATE_44100)
#define DLS_RATE_CONVERT                -8914
#define DLS_LFO_FREQUENCY_CONVERT       5277

#elif defined (_SAMPLE_RATE_48000)
#define DLS_RATE_CONVERT                -9061
#define DLS_LFO_FREQUENCY_CONVERT       5423

#else
#error "_SAMPLE_RATE_XXXXX must be defined to valid rate"
#endif

/*
 * FILTER_Q_CONVERSION_FACTOR convers the 0.1dB steps in the DLS
 * file to our internal 0.75 dB steps. The value is calculated
 * as follows:
 *
 * 32768 / (10 * <step-size in dB>)
 *
 * FILTER_RESONANCE_NUM_ENTRIES is the number of entries in the table
*/
#define FILTER_Q_CONVERSION_FACTOR          4369
#define FILTER_RESONANCE_NUM_ENTRIES        31

/*
 * Multiplier to convert DLS gain units (10ths of a dB) to a
 * power-of-two exponent for conversion to linear gain using our
 * piece-wise linear approximator. Note that we ignore the lower
 * 16-bits of the DLS gain value. The result is a 10-bit fraction
 * that works with the EAS_LogToLinear16 function.
 *
 * DLS_GAIN_FACTOR = (2^18) / (200 * log10(2))
 */
#define DLS_GAIN_FACTOR         4354
#define DLS_GAIN_SHIFT          8

/*
 * Reciprocal of 10 for quick divide by 10's
 *
 * DLS_GAIN_FACTOR = (2^18) / (200 * log10(2))
 */
#define DLS_DIV_10_FACTOR       3277
#define DLS_DIV_10_SHIFT        16

/*
 * Multiplier to convert DLS time cents units to a power-of-two
 * exponent for conversion to absolute time units using our
 * piece-wise linear approximator.
 *
 * DLS_TIME_FACTOR = (2^22) / 1200
 */
#define DLS_TIME_FACTOR         3495
#define DLS_TIME_SHIFT          22


/* LFO limits */
#define MAX_LFO_FREQUENCY_IN_HERTZ          20
#define MIN_LFO_FREQUENCY_IN_HERTZ          0.1
#define MAX_LFO_FREQUENCY_IN_PITCHCENTS     1549
#define MIN_LFO_FREQUENCY_IN_PITCHCENTS     -7624
#define MAX_LFO_AMPLITUDE_DEPTH             12  /* in dB, DLS2.1 p 31*/
#define MIN_LFO_AMPLITUDE_DEPTH             -12 /* in dB, DLS2.1 p 31*/

/* add to pitch cents before pow(2.0, n) to convert to frequency */
#define ABSOLUTE_PITCH_BIAS                 238395828

#define A5_PITCH_OFFSET                     6900

/*
CHUNK_TYPE is a macro that converts the 4 input args into a 32-bit int
where
argument a is placed at the MSB location and
argument d is placed at the LSB location.
This is useful for determining the DLS chunk types
*/
#define CHUNK_TYPE(a,b,c,d) (   \
        ( ((EAS_U32)(a) & 0xFF) << 24 ) \
    +   ( ((EAS_U32)(b) & 0xFF) << 16 ) \
    +   ( ((EAS_U32)(c) & 0xFF) <<  8 ) \
    +   ( ((EAS_U32)(d) & 0xFF)       ) )

#define CHUNK_RIFF                  CHUNK_TYPE('R','I','F','F')
#define CHUNK_DLS                   CHUNK_TYPE('D','L','S',' ')
#define CHUNK_CDL                   CHUNK_TYPE('c','d','l',' ')
#define CHUNK_VERS                  CHUNK_TYPE('v','e','r','s')
#define CHUNK_DLID                  CHUNK_TYPE('d','l','i','d')
#define CHUNK_LIST                  CHUNK_TYPE('L','I','S','T')
#define CHUNK_COLH                  CHUNK_TYPE('c','o','l','h')
#define CHUNK_LINS                  CHUNK_TYPE('l','i','n','s')
#define CHUNK_PTBL                  CHUNK_TYPE('p','t','b','l')
#define CHUNK_WVPL                  CHUNK_TYPE('w','v','p','l')
#define CHUNK_INFO                  CHUNK_TYPE('I','N','F','O')
#define CHUNK_INAM                  CHUNK_TYPE('I','N','A','M')
#define CHUNK_INS                   CHUNK_TYPE('i','n','s',' ')
#define CHUNK_INSH                  CHUNK_TYPE('i','n','s','h')
#define CHUNK_LRGN                  CHUNK_TYPE('l','r','g','n')
#define CHUNK_RGN                   CHUNK_TYPE('r','g','n',' ')
#define CHUNK_RGN2                  CHUNK_TYPE('r','g','n','2')
#define CHUNK_RGNH                  CHUNK_TYPE('r','g','n','h')
#define CHUNK_WSMP                  CHUNK_TYPE('w','s','m','p')
#define CHUNK_WLNK                  CHUNK_TYPE('w','l','n','k')
#define CHUNK_LART                  CHUNK_TYPE('l','a','r','t')
#define CHUNK_LAR2                  CHUNK_TYPE('l','a','r','2')
#define CHUNK_ART1                  CHUNK_TYPE('a','r','t','1')
#define CHUNK_ART2                  CHUNK_TYPE('a','r','t','2')
#define CHUNK_WAVE                  CHUNK_TYPE('w','a','v','e')
#define CHUNK_FMT                   CHUNK_TYPE('f','m','t',' ')
#define CHUNK_DATA                  CHUNK_TYPE('d','a','t','a')
#define CHUNK_DMPR                  CHUNK_TYPE('d','m','p','r')


#define WAVE_FORMAT_PCM             0x0001 /* Microsoft PCM format, see DLS2.1 p60 */
#define WAVE_FORMAT_EXTENSIBLE      0xffff

/* defines for wave table structures */

/* initialize each articulation structure to a harmless state */
/* change art values after we've determined EAS internals */
#define DEFAULT_DLS_FILTER_CUTOFF_FREQUENCY     0x7FFF      /* DLS2.1, p 31 means leave filter off */

/**********/

/* define the waves that we expect to generate instead of store */
/* NOTE: our comparison routine converts the input string
to lowercase, so the following comparison values should all
be in lowercase.
*/
#define STRING_NOISE            "noise"


/*------------------------------------
 * type definitions
 *------------------------------------
*/
#ifdef _STANDALONE_CONVERTER
typedef struct s_dls_params
{
    EAS_INT     sampleRate;
    EAS_INT     samplesPerFrame;
    EAS_INT     bitDepth;
    double      ditherLevel;
    double      ditherFilterCoeff;
    EAS_BOOL    compatibility;
    EAS_BOOL    encodeADPCM;
} S_DLS_PARAMS;
#endif


/* function prototypes */
EAS_RESULT DLSParser (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE fileHandle, EAS_I32 offset, S_DLS **pDLS);
EAS_RESULT DLSCleanup (EAS_HW_DATA_HANDLE hwInstData, S_DLS *pDLS);
void DLSAddRef (S_DLS *pDLS);
EAS_I16 ConvertDelay (EAS_I32 timeCents);
EAS_I16 ConvertRate (EAS_I32 timeCents);


#ifdef _STANDALONE_CONVERTER
void DLSConvParams (S_DLS_PARAMS *pParams, EAS_BOOL set);
#endif

#endif

