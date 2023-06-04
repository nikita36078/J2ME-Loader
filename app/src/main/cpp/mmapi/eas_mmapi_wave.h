/*----------------------------------------------------------------------------
 *
 * File:
 * eas_mmapi_wave.h
 *
 * Contents and purpose:
 * Declarations for MMAPI WAVE writing support (RecordControl).
 * Loosely based on eas_wave.h
 *
 * Copyright Sonic Network Inc. 2006
 *----------------------------------------------------------------------------
 * Revision Control:
 *   $Revision: 560 $
 *   $Date: 2007-02-02 14:34:18 -0800 (Fri, 02 Feb 2007) $
 *----------------------------------------------------------------------------
*/

#include "eas_mmapi_config.h"
#include "eas_mmapi_types.h"
#include "../sonivox/host_src/eas_types.h"
#include "../sonivox/host_src/eas_host.h"

#ifndef _EAS_MMAPI_WAVE_INCLUDED
#define _EAS_MMAPI_WAVE_INCLUDED

/* Align fields in WAVE_FMT_CHUNK to 2-byte boundaries.
 * May require a different pragma, depending on compilers
 */
#pragma pack (2)

/* .WAV file format chunk */
typedef struct {
	EAS_U16 wFormatTag;
	EAS_U16 nChannels;
	EAS_U32 nSamplesPerSec;
	EAS_U32 nAvgBytesPerSec;
	EAS_U16 nBlockAlign;
	EAS_U16 wBitsPerSample;
} WAVE_FMT_CHUNK;

/* the Windows WAVE tag for PCM encoding */
#define WAVE_FORMAT_TAG_PCM  (1)


#pragma pack ()

/*----------------------------------------------------------------------------
 * Write a wave file header to the file stream.
 * It uses MMAPI_HWWriteFile() to write the header to the stream.
 *
 * @param hwInstData the host synth handle
 * @param file the handle to the open file stream (opened for writing with EAS_HWOpenFile).
 * @param fmt the format of the file to write
 * @param dataSize the total number of bytes in the data chunk, or -1 if not known
 * @param headerSize on successful completion, receives the total size of the written header
 * @return an EAS error code
 *----------------------------------------------------------------------------
*/
EAS_RESULT WAVE_WriteHeader(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file,
							WAVE_FMT_CHUNK* fmt, EAS_I32 dataSize, EAS_I32* headerSize);


/*----------------------------------------------------------------------------
 * Write a wave file header to the streaming media buffer.
 * It uses MMAPI_HWWriteFileImpl() to write the header to the stream.
 *
 * @param mb the media buffer pointer
 * @param fmt the format of the file to write
 * @param dataSize the total number of bytes in the data chunk, or -1 if not known
 * @param headerSize on successful completion, receives the total size of the written header
 * @return an EAS error code
 *----------------------------------------------------------------------------
*/
EAS_RESULT WAVE_WriteHeaderToBuffer(MMAPI_MediaBuffer* mb,
						    WAVE_FMT_CHUNK* fmt, EAS_I32 dataSize, EAS_I32* headerSize);

/*----------------------------------------------------------------------------
 * WAVE_FillFormat()
 *----------------------------------------------------------------------------
 * Purpose: Construct the fmt_ chunk for WAVE files. The fields will be 
 *          initialized in native endianness.
 * 
 * Inputs: 
 *			
 * Outputs:
 *          returns false if the fmt chunk cannot be constructed
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT WAVE_FillFormat(WAVE_FMT_CHUNK* fmt, 
						 MMAPI_CAPTURE_ENCODING encoding, EAS_I32 rate, EAS_I32 bits, 
						 EAS_I32 channels, EAS_BOOL isBigEndian, EAS_BOOL isSigned);

#endif /* end #ifndef _EAS_MMAPI_WAVE_INCLUDED */
