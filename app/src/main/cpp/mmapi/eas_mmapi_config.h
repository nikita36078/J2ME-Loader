/*----------------------------------------------------------------------------
 *
 * File:
 * eas_mmapi_config.h
 *
 * Contents and purpose:
 * Flags to configure the MMAPI implementation. Note that
 * some Java flags need to be set to the same values
 * in class com.sonivox.mmapi.Config.
 *
 * Copyright 2006 Sonic Network Inc.
 *
 *----------------------------------------------------------------------------
 * Revision Control:
 *   $Revision: 560 $
 *   $Date: 2007-02-02 14:34:18 -0800 (Fri, 02 Feb 2007) $
 *----------------------------------------------------------------------------
*/

/* define the EAS debug level, only used if SONIVOX_DEBUG is defined */
#define MMAPI_DEBUG_EAS_DEBUG_LEVEL 2

/* define this to not allow 0 duration -- seems to be broken in EAS */
#define MMAPI_DURATION_WORKAROUND

/* for a single-thread KVM, define this symbol */
#define MMAPI_SINGLE_THREAD_VM

/* define this if rendering is triggered from native.
 * Synchronize with class com.sonivox.mmapi.Config
 */
/* #define MMAPI_NATIVE_RENDERING_THREAD */

/* define this symbol if audio device playback is wanted */
#define MMAPI_DEBUG_HAS_AUDIO_DEV


/* for debugging: write all output to a file */
/* #define MMAPI_DEBUG_WAVEFILE */


/* for debugging: if writing to file, use this filename */
#define MMAPI_DEBUG_WAVEFILE_NAME "C:\\mmapi_out.wav"


/* define this symbol if EAS implements SetTempo().
 * Synchronize with class com.sonivox.mmapi.Config */
/* #define MMAPI_HAS_TEMPO_CONTROL */


/* define this symbol if EAS implements ToneControl()
 * Synchronize with class com.sonivox.mmapi.Config */
#define MMAPI_HAS_TONE_CONTROL

/* define this symbol if audio capture is available
 * Synchronize with class com.sonivox.mmapi.Config */
#define MMAPI_HAS_CAPTURE


/* number of mixBuffers to read from EAS_Render() before writing
 * all together to the wave device */
#define MMAPI_AUDIODEVICE_BUFFERS	4

/*
 * the size of the circular buffer for streaming buffer mode.
 * This size must be at least Java's
 *    (Config.STREAM_BUFFER_SIZE * Config.STREAM_BUFFER_PREFETCH_COUNT)
 * The larger the buffer, the smaller the risk of buffer underruns
 * (especially for high sample rates, and fast playback rate).
 * Furthermore, if a streamed file fits completely into the
 * circular buffer, the mode is changed to MEMORY, with the
 * advantages of the MEMORY mode: winding possible, looping,
 * etc.
 */
#define MMAPI_STREAM_CIRCULAR_BUFFER_SIZE  (1024 * 50)


/*
 * The size of the circular buffer for recording.
 * This size should be sufficient to hold enough audio data
 * as is necessary in between calls to MMAPI_ReadBuffer()
 * from method EAS.nreadRecordedBytes().
 * The absolute minimum is the amount of input wave data
 * that is necessary for filling one render buffer.
 * E.g. rendering is in 128 samples buffers, at 11025Hz
 * mono. Now an input file be in CD quality, it requires
 * 4 times more data for the higher sampling rate, and
 * twice as much data for 2 channels to fill 128 samples.
 * Now with a faster playback rate of maximum 200%, the
 * recording circular buffer needs to be at least
 * 4*2*2*128 = 2048 samples -> 4096 bytes (to allow 16-bit
 * samples).
 * The current design reads the recorded data asynchronous
 * to the rendering thread, so even more data should be
 * reserved for the recording circular buffer to prevent
 * buffer overflows.
 */
#define MMAPI_RECORD_CIRCULAR_BUFFER_SIZE  (1024 * 8)


/*
 * The size of the circular buffer for capture.
 * The same calculation as for recording circular buffer,
 * except that rate cannot be changed for capture.
 */
#define MMAPI_CAPTURE_CIRCULAR_BUFFER_SIZE (1024 * 4)

/*
 * Number of Windows capture buffers for the capture device.
 * The larger, the higher latency for capture.
 */
#define MMAPI_CAPTURE_DEVICE_BUFFERS	8

/* if EAS cannot handle WAVE file playback with unknown size, set this.
 * It will cause the capture device to be time limited */
#define MMAPI_CAPTURE_STREAMING_WORKAROUND

/* For streaming mode: define this symbol if the host implementation
 * should return silence rather than aborting the read call
 * with an error code. This is a work-around only! */
#define MMAPI_PROVIDE_SILENCE_ON_UNDERRUN

/* for debugging, define if EAS_GetWaveFmtChunk() is not available */
/* #define MMAPI_DEBUG_USE_FORMAT_QUERY_STUB */

/* the size of the meta data buffer for file streams */
#define MMAPI_METADATA_BUFFER_SIZE 200


/* define this to write all played tone sequence files to C:\tonesequence.jts */
/* #define MMAPI_DEBUG_WRITE_TONE_SEQUENCE */

/* define this to write everything that is read from a stream to C:\streamdump.pcm */
/* #define MMAPI_DEBUG_WRITE_STREAM_DUMP */


/* define if using Microsoft Visual C++ 2003 Standard */
/* #define USING_MS_VC_2003_NET_STANDARD */

/* debugging: define to use Sonivox's host implementation */
/* #define MMAPI_USE_ORIGINAL_HOST */
