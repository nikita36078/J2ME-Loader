/*----------------------------------------------------------------------------
 *
 * File: 
 * eas_waveout_oboe.cpp
 *
 * Contents and purpose:
 * This module outputs a PCM stream to a Android output device using Oboe sound library
*/

#include <malloc.h>
#include "eas_waveout.h"
#include <oboe/Oboe.h>

#define LOG_TAG "EAS_WAVEOUT"

#include "../sonivox/log/log.h"

/* for C++ linkage */
#ifdef __cplusplus
extern "C" {
#endif

static std::shared_ptr<oboe::AudioStream> createAudioStream(EAS_I32 channels, EAS_I32 samplesPerSec) {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output);
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
    oboe::Result &&startResult = stream->start();
    if (startResult != oboe::Result::OK) {
        ALOGE("%s: can't start audio stream. %s", __func__, convertToText(startResult));
        stream->close();
        stream.reset();
    }
    return stream;
}

/*----------------------------------------------------------------------------
 * OpenWaveDevice()
 *----------------------------------------------------------------------------
 * Purpose:
 * Opens the Wave output device for playback
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_HANDLE OpenWaveDevice(EAS_U32 devNum, EAS_I32 channels, EAS_I32 samplesPerSec, EAS_I32 bitsPerSample) {
    if (bitsPerSample != 16) {
        ALOGE("%s: illegal argument 'bitsPerSample' = %ld", __func__, bitsPerSample);
        return nullptr;
    }
    auto &&stream = createAudioStream(channels, samplesPerSec);
    if (stream) {
        return reinterpret_cast<EAS_HANDLE>(new std::shared_ptr<oboe::AudioStream>(stream));
    }
    return nullptr;
} /* end OpenWaveDevice */

/*----------------------------------------------------------------------------
 * OutputWaveDevice()
 *----------------------------------------------------------------------------
 * Purpose:
 * Outputs a buffer of audio to the Wave device
 *
 * Inputs:
 *
 * Outputs:
 *
 *----------------------------------------------------------------------------
*/
EAS_INT OutputWaveDevice(EAS_HANDLE waveOutDev, void *buffer, EAS_I32 n) {
    auto handle = reinterpret_cast<std::shared_ptr<oboe::AudioStream> *>(waveOutDev);
    std::shared_ptr<oboe::AudioStream> stream(*handle);

    auto &&result = stream->write(buffer, n, n * oboe::kNanosPerSecond / stream->getSampleRate());
    if (result.error() == oboe::Result::OK) {
        return result.value();
    } else if (result.error() == oboe::Result::ErrorDisconnected) {
        auto &&newStream = createAudioStream(stream->getChannelCount(), stream->getSampleRate());
        if (newStream) {
            stream->close();
            handle->swap(newStream);
        }
    }
    ALOGW("OutputWaveDevice error: %s", convertToText(result.error()));
    return 0;
} /* end OutputWaveDevice */

/*----------------------------------------------------------------------------
 * CloseWaveDevice()
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
EAS_BOOL CloseWaveDevice(EAS_HANDLE waveOutDev) {
    auto handle = reinterpret_cast<std::shared_ptr<oboe::AudioStream> *>(waveOutDev);
    std::shared_ptr<oboe::AudioStream> stream(*handle);
    EAS_BOOL result = stream->stop() == oboe::Result::OK && stream->close() == oboe::Result::OK;
    handle->reset();
    return result;
} /* end CloseWaveDevice */

#ifdef __cplusplus
} /* end extern "C" */
#endif

