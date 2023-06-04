/*----------------------------------------------------------------------------
 *
 * File:
 * eas_wavein_oboe.cpp
 *
 * Contents and purpose:
 * This module captures a PCM stream from a Android input device via Oboe library
 *
*/

#include <oboe/Oboe.h>
#include <malloc.h>
#include "eas_wavein.h"
#include "../sonivox/host_src/eas_report.h"
#include "../sonivox/log/log.h"

#define LOG_TAG "EAS_WAVEIN"

#ifndef MMAPI_CAPTURE_DEVICE_BUFFERS
#define MMAPI_CAPTURE_DEVICE_BUFFERS		8
#endif

#ifndef CAPTURE_BUFFER_SIZE
#define CAPTURE_BUFFER_SIZE	256
#endif

#define QUEUE_TIMEOUT	5000

#ifdef __cplusplus
extern "C"
{
#endif

/* wave header blocks for buffers */
typedef struct
{
	EAS_INT		readCount;
	EAS_INT 	readBuffer;
	EAS_INT 	readOffset;
	EAS_INT 	recBuffer;
	EAS_BOOL	closed;
	EAS_BOOL 	queued[MMAPI_CAPTURE_DEVICE_BUFFERS];
} S_WAVE_IN_STATE;

S_WAVE_IN_STATE waveInState{};

static std::shared_ptr<oboe::AudioStream> createAudioStream(EAS_I32 channels, EAS_I32 samplesPerSec) {
	oboe::AudioStreamBuilder builder;
	builder.setDirection(oboe::Direction::Input);
	builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
	builder.setSharingMode(oboe::SharingMode::Shared);
	builder.setFormat(oboe::AudioFormat::I16);
	builder.setChannelCount(static_cast<int>(channels));
	builder.setSampleRate(static_cast<int>(samplesPerSec));

	std::shared_ptr<oboe::AudioStream> stream;
	oboe::Result &&openResult = builder.openStream(stream);
	if (openResult != oboe::Result::OK) {
		ALOGE("%s: can't open audio stream. %s", __func__, convertToText(openResult));
		return stream;
	}
	return stream;
}

/*----------------------------------------------------------------------------
 * NumWaveInDevices()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the number of WaveIn devices
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_INT NumWaveInDevices (void)
{
	return (EAS_INT) 0;
}

/*----------------------------------------------------------------------------
 * GetWaveInReadSize()
 *----------------------------------------------------------------------------
 * Purpose:
 * Returns the number bytes in the capture buffers
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_INT GetWaveInReadSize (void)
{
	return waveInState.readCount;
}

/*----------------------------------------------------------------------------
 * OpenWaveInDevice()
 *----------------------------------------------------------------------------
 * Purpose:
 * Opens the Wave input device for capture
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_VOID_PTR OpenWaveInDevice (EAS_U32 devNum, EAS_I32 channels, EAS_I32 samplesPerSec, EAS_I32 bitsPerSample)
{
	if (bitsPerSample != 16) {
		ALOGE("%s: illegal argument 'bitsPerSample' = %ld", __func__, bitsPerSample);
		return nullptr;
	}
	auto &&stream = createAudioStream(channels, samplesPerSec);
	if (!stream) {
		return nullptr;
	}
	waveInState.queued[waveInState.readBuffer] = EAS_TRUE;
	return reinterpret_cast<EAS_HANDLE>(new std::shared_ptr<oboe::AudioStream>(stream));
}

/*----------------------------------------------------------------------------
 * StartWaveInCapture()
 *----------------------------------------------------------------------------
 * Purpose:
 * Start recording
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_BOOL StartWaveInCapture (EAS_VOID_PTR waveInDev)
{
	auto handle = reinterpret_cast<std::shared_ptr<oboe::AudioStream> *>(waveInDev);
	std::shared_ptr<oboe::AudioStream> stream(*handle);
	oboe::Result &&startResult = stream->start();
	if (startResult != oboe::Result::OK) {
		ALOGE("%s: can't start audio stream. %s", __func__, convertToText(startResult));
		stream->close();
		stream.reset();
		return EAS_FALSE;
	}
	return EAS_TRUE;
}

/*----------------------------------------------------------------------------
 * StopWaveInCapture()
 *----------------------------------------------------------------------------
 * Purpose:
 * Stop recording
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_BOOL StopWaveInCapture (EAS_VOID_PTR waveInDev)
{
	auto handle = reinterpret_cast<std::shared_ptr<oboe::AudioStream> *>(waveInDev);
	std::shared_ptr<oboe::AudioStream> stream(*handle);
	oboe::Result &&startResult = stream->stop();
	if (startResult != oboe::Result::OK) {
		ALOGE("%s: can't start audio stream. %s", __func__, convertToText(startResult));
		stream->close();
		stream.reset();
		return EAS_FALSE;
	}
	return EAS_TRUE;
}

/*----------------------------------------------------------------------------
 * ReadWaveInData()
 *----------------------------------------------------------------------------
 * Purpose:
 * Reads data from the WaveIn device capture buffers
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_INT ReadWaveInData (EAS_VOID_PTR waveInDev, void *buffer, EAS_I32 n)
{
	return 0;
}


/*----------------------------------------------------------------------------
 * WriteWaveInDataToStream()
 *----------------------------------------------------------------------------
 * Purpose:
 * Reads data from the WaveIn device capture buffers and writes it to the
 * specified stream.
 *
 * Inputs:
 *    mHandle: the synth instance
 *    streamHandle: MMAPI handle to the stream.
 * TODO: signature should be modified to be
 *       EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file
 *       once the host write function is available.
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_INT WriteWaveInDataToStream (EAS_VOID_PTR waveInDev, EAS_HW_DATA_HANDLE hwInstData, MMAPI_FILE_STRUCT* streamHandle)
{
	return 0;
}

/*----------------------------------------------------------------------------
 * CloseWaveInDevice()
 *----------------------------------------------------------------------------
 * Purpose:
 * Closes the Wave device
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_BOOL CloseWaveInDevice (EAS_VOID_PTR waveInDev)
{
	auto handle = reinterpret_cast<std::shared_ptr<oboe::AudioStream> *>(waveInDev);
	std::shared_ptr<oboe::AudioStream> stream(*handle);
	EAS_BOOL result = stream->stop() == oboe::Result::OK && stream->close() == oboe::Result::OK;
	handle->reset();
	return result;
}

#ifdef __cplusplus
}
#endif
