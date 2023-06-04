/*----------------------------------------------------------------------------
 *
 * File:
 * eas_audioconst.h
 *
 * Contents and purpose:
 * Defines audio constants related to the sample rate, bit size, etc.
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
 *   $Revision: 82 $
 *   $Date: 2006-07-10 11:45:19 -0700 (Mon, 10 Jul 2006) $
 *----------------------------------------------------------------------------
*/

#ifndef _EAS_AUDIOCONST_H
#define _EAS_AUDIOCONST_H

/*----------------------------------------------------------------------------
 * These macros define the various characteristics of the defined sample rates
 *----------------------------------------------------------------------------
 * BUFFER_SIZE_IN_MONO_SAMPLES  size of buffer in samples
 * _OUTPUT_SAMPLE_RATE          compiled output sample rate
 * AUDIO_FRAME_LENGTH           length of an audio frame in 256ths of a millisecond
 * SYNTH_UPDATE_PERIOD_IN_BITS  length of an audio frame (2^x samples)
 *----------------------------------------------------------------------------
*/

#if defined (_SAMPLE_RATE_8000)
#define BUFFER_SIZE_IN_MONO_SAMPLES     32
#define _OUTPUT_SAMPLE_RATE             8000
#define AUDIO_FRAME_LENGTH              1024
#define SYNTH_UPDATE_PERIOD_IN_BITS     5

#elif defined (_SAMPLE_RATE_16000)
#define BUFFER_SIZE_IN_MONO_SAMPLES     64
#define _OUTPUT_SAMPLE_RATE             16000
#define AUDIO_FRAME_LENGTH              1024
#define SYNTH_UPDATE_PERIOD_IN_BITS     6

#elif defined (_SAMPLE_RATE_20000)
#define BUFFER_SIZE_IN_MONO_SAMPLES     128
#define _OUTPUT_SAMPLE_RATE             20000
#define AUDIO_FRAME_LENGTH              1638
#define SYNTH_UPDATE_PERIOD_IN_BITS     7

#elif defined (_SAMPLE_RATE_22050)
#define BUFFER_SIZE_IN_MONO_SAMPLES     128
#define _OUTPUT_SAMPLE_RATE             22050
#define AUDIO_FRAME_LENGTH              1486
#define SYNTH_UPDATE_PERIOD_IN_BITS     7

#elif defined (_SAMPLE_RATE_24000)
#define BUFFER_SIZE_IN_MONO_SAMPLES     128
#define _OUTPUT_SAMPLE_RATE             24000
#define AUDIO_FRAME_LENGTH              1365
#define SYNTH_UPDATE_PERIOD_IN_BITS     7

#elif defined (_SAMPLE_RATE_32000)
#define BUFFER_SIZE_IN_MONO_SAMPLES     128
#define _OUTPUT_SAMPLE_RATE             32000
#define AUDIO_FRAME_LENGTH              1024
#define SYNTH_UPDATE_PERIOD_IN_BITS     7

#elif defined (_SAMPLE_RATE_44100)
#define BUFFER_SIZE_IN_MONO_SAMPLES     256
#define _OUTPUT_SAMPLE_RATE             44100
#define AUDIO_FRAME_LENGTH              1486
#define SYNTH_UPDATE_PERIOD_IN_BITS     8

#elif defined (_SAMPLE_RATE_48000)
#define BUFFER_SIZE_IN_MONO_SAMPLES     256
#define _OUTPUT_SAMPLE_RATE             48000
#define AUDIO_FRAME_LENGTH              1365
#define SYNTH_UPDATE_PERIOD_IN_BITS     8

#else
#error "_SAMPLE_RATE_XXXXX must be defined to valid rate"
#endif

#endif /* #ifndef _EAS_AUDIOCONST_H */

