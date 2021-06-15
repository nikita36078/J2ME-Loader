/*----------------------------------------------------------------------------
 *
 * File: 
 * eas_mmapi_wave.h
 *
 * Contents and purpose:
 * MMAPI WAVE writing support (RecordControl).
 * Based on eas_wave.c. Added the size of optional
 * data field to the format chunk, since it's
 * required by some software.
 *			
 * Copyright Sonic Network Inc. 2006
 *----------------------------------------------------------------------------
 * Revision Control:
 *   $Revision: 560 $
 *   $Date: 2007-02-02 14:34:18 -0800 (Fri, 02 Feb 2007) $
 *----------------------------------------------------------------------------
*/

#include "eas_mmapi_wave.h"
#include "eas_mmapi.h"

/* .WAV file format tags */
const EAS_U32 WAVE_TAG_RIFF = 0x46464952;
const EAS_U32 WAVE_TAG_WAVE = 0x45564157;
const EAS_U32 WAVE_TAG_FMT = 0x20746d66;
const EAS_U32 WAVE_TAG_DATA = 0x61746164;

#define WAVE_SIZE_NOT_KNOWN		0xFFFFFFFF

/* Align fields in WAVE_FMT_CHUNK to 2-byte boundaries. 
 * May require a different pragma, depending on compilers
 */
#pragma pack (2)

/* .WAV file header */
typedef struct {
	EAS_U32 nRiffTag;
	EAS_U32 nRiffSize;
	EAS_U32 nWaveTag;
	EAS_U32 nFmtTag;
	EAS_U32 nFmtSize;
	WAVE_FMT_CHUNK fc;
	EAS_U16 wSizeOfOptionalData;
	EAS_U16 wPadding;
	EAS_U32 nDataTag;
	EAS_U32 nDataSize;
} WAVE_HEADER;

#pragma pack ()

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
	FlipWord(&p->wSizeOfOptionalData);
	FlipWord(&p->fc.wFormatTag);
	FlipWord(&p->fc.nChannels);
	FlipDWord(&p->fc.nSamplesPerSec);
	FlipDWord(&p->fc.nAvgBytesPerSec);
	FlipWord(&p->fc.nBlockAlign);
	FlipWord(&p->fc.wBitsPerSample);
}
#endif

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
						 EAS_I32 channels, EAS_BOOL isBigEndian, EAS_BOOL isSigned) {
	if (encoding == MMAPI_CAPTURE_ENCODING_PCM) {
		/* sanity check */
		if (rate < 1000 || rate > 96000 || bits < 8 || bits > 24 
			|| channels < 1 || channels > 2 || isBigEndian 
			|| (bits == 8 && isSigned)) {
				return EAS_FAILURE;
		}
	}
	switch (encoding) {
	case MMAPI_CAPTURE_ENCODING_PCM:
		fmt->wFormatTag = WAVE_FORMAT_TAG_PCM;
		break;
	default:
		/* encoding not supported */
		return EAS_FAILURE;
	}
	fmt->nChannels = (EAS_U16) channels;
	fmt->nSamplesPerSec = rate;
	fmt->wBitsPerSample = (EAS_U16) bits;
	fmt->nBlockAlign = (fmt->wBitsPerSample + 7) / 8;
	fmt->nAvgBytesPerSec = fmt->nBlockAlign * fmt->nSamplesPerSec;
	return EAS_SUCCESS;
}


/*----------------------------------------------------------------------------
 * WAVE_WriteHeaderImpl()
 *----------------------------------------------------------------------------
 * Purpose: Internal implementation of WriteHeader -- will write to the file, if 
 *          hwInstData and file are spcified, or to the media buffer, if mb is non-NULL.
 * 
 * Inputs: 
 *         fmt: the format chunk, in native endianness. It will be written
                correctly in little endian to the stream.
 *         dataSize: set to -1 if not known
 *			
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT WAVE_WriteHeaderImpl(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, 
								MMAPI_MediaBuffer* mb,
							    WAVE_FMT_CHUNK* fmt, EAS_I32 dataSize, EAS_I32* headerSize) {
	WAVE_HEADER header;
	EAS_RESULT res = EAS_FAILURE;
	
	/* initialize .WAV file header */
	header.nRiffTag = WAVE_TAG_RIFF;
	if (dataSize >= 0) {
		header.nRiffSize = sizeof(WAVE_HEADER) - 8 + ((EAS_U32) dataSize);
	} else {
#ifdef MMAPI_CAPTURE_STREAMING_WORKAROUND
		if (dataSize == -2) 
			/* workaround value for EAS to mean unlimited size */
			header.nRiffSize = MMAPI_WAVE_SIZE_UNKNOWN;
		else
#endif
		/* if data size is not known, use 0xFFFFFFFF as data size 
		 * (per specification of WAVE file format) */
		header.nRiffSize = WAVE_SIZE_NOT_KNOWN;
	}
	header.nWaveTag = WAVE_TAG_WAVE;
	header.nFmtTag = WAVE_TAG_FMT;
	header.nFmtSize = sizeof(WAVE_FMT_CHUNK) + sizeof(header.wSizeOfOptionalData) + sizeof(header.wPadding);

	/* copy 'fmt' chunk */
	header.fc = (*fmt);
	header.wSizeOfOptionalData = 0;
	header.wPadding = 0;

	/* initialize 'data' chunk */
	header.nDataTag = WAVE_TAG_DATA;
	if (dataSize >= 0) {
		header.nDataSize = (EAS_U32) dataSize;
	} else {
#ifdef MMAPI_CAPTURE_STREAMING_WORKAROUND
		if (dataSize == -2) 
			/* workaround value for EAS to mean unlimited size */
			header.nDataSize = MMAPI_WAVE_SIZE_UNKNOWN;
		else
#endif
		/* if total length is (not yet) known, set the data size to -1, 
		 * per the WAVE file spec */
		header.nDataSize = WAVE_SIZE_NOT_KNOWN;
	}

#ifdef _BIG_ENDIAN
	FlipWaveHeader(&header);
#endif

	/* write the header */
	if (hwInstData != NULL && file != NULL) {
		res = MMAPI_HWWriteFile(hwInstData, file, &header, sizeof(WAVE_HEADER), headerSize);
	} else
	if (mb != NULL) {
		res = MMAPI_HWWriteFileImpl(MMAPI_OPEN_MODE_STREAM, mb, (EAS_U8*) &header, sizeof(WAVE_HEADER), headerSize);
	}
	if (res == EAS_SUCCESS && (*headerSize) != sizeof(WAVE_HEADER)) {
		res = EAS_FAILURE;
	}
	return res;
}


/*----------------------------------------------------------------------------
 * WAVE_WriteHeaderToBuffer()
 *----------------------------------------------------------------------------
 * Purpose: Write a full WAVE header, including RIFF container chunk
 *          and format chunk, and data chunk header, to the specified media buffer.
 * 
 * Inputs: 
 *         fmt: the format chunk, in native endianness. It will be written
                correctly in little endian to the stream.
 *         dataSize: set to -1 if not known
 *			
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT WAVE_WriteHeaderToBuffer(MMAPI_MediaBuffer* mb,
						    WAVE_FMT_CHUNK* fmt, EAS_I32 dataSize, EAS_I32* headerSize) {
	return WAVE_WriteHeaderImpl(NULL, NULL, mb, fmt, dataSize, headerSize);
}

/*----------------------------------------------------------------------------
 * WAVE_WriteHeader()
 *----------------------------------------------------------------------------
 * Purpose: Write a full WAVE header, including RIFF container chunk
 *          and format chunk, and data chunk header, to the stream identified
 *          by "file".
 * 
 * Inputs: 
 *         fmt: the format chunk, in native endianness. It will be written
                correctly in little endian to the stream.
 *         dataSize: set to -1 if not known
 *			
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT WAVE_WriteHeader(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, 
						    WAVE_FMT_CHUNK* fmt, EAS_I32 dataSize, EAS_I32* headerSize) {
	return WAVE_WriteHeaderImpl(hwInstData, file, NULL, fmt, dataSize, headerSize);
}


