//
// Created by woesss on 08.07.2023.
//

#include "mmapi_player.h"
#include "mmapi_file.h"
#include "libsonivox/eas.h"
#include "util/log.h"
#include "mmapi_util.h"

#define LOG_TAG "MMAPI"

#define NUM_COMBINE_BUFFERS 4

EAS_DLSLIB_HANDLE mmapi::Player::easDlsHandle{nullptr};

mmapi::Player::Player(EAS_DATA_HANDLE easHandle) : easHandle(easHandle) {
    EAS_SetGlobalDLSLib(easHandle, mmapi::Player::easDlsHandle);
}

mmapi::Player::~Player() {
    close();
}

EAS_RESULT mmapi::Player::createAudioStream() {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Shared);
    builder.setFormat(oboe::AudioFormat::I16);
    builder.setChannelCount(static_cast<int>(easConfig->numChannels));
    builder.setSampleRate(static_cast<int>(easConfig->sampleRate));
    builder.setCallback(this);
    builder.setFramesPerDataCallback(easConfig->mixBufferSize * NUM_COMBINE_BUFFERS);

    oboe::Result &&result = builder.openStream(oboeStream);
    if (result != oboe::Result::OK) {
        oboeStream.reset();
        ALOGE("%s: can't open audio stream. %s", __func__, convertToText(result));
        return EAS_FAILURE;
    }
    return EAS_SUCCESS;
}

long mmapi::Player::init(const char *path) {
    if (path == nullptr) {
        return EAS_ERROR_INVALID_PARAMETER;
    }
    file = new File(path, "rb");
    EAS_RESULT result = EAS_OpenFile(easHandle, &file->easFile, &media);
    if (result != EAS_SUCCESS) {
        return result;
    }
    result = EAS_Prepare(easHandle, media);
    if (result != EAS_SUCCESS) {
        return result;
    }
    EAS_I32 type = EAS_FILE_UNKNOWN;
    result = EAS_GetFileType(easHandle, media, &type);
    if (result != EAS_SUCCESS) {
        return result;
    }
    ALOGV("EAS_checkFileType(): %s file recognized", MMAPI_GetFileTypeString(type));
    if (type == EAS_FILE_WAVE_PCM) {
        result = EAS_ERROR_INVALID_PCM_TYPE;
    }
    return EAS_ParseMetaData(easHandle, media, &duration);
}

EAS_RESULT mmapi::Player::prefetch() {
    EAS_RESULT result = createAudioStream();
    if (result != EAS_SUCCESS) {
        return result;
    }
    state = PREFETCHED;
    return result;
}

EAS_RESULT mmapi::Player::start() {
    oboe::Result &&result = oboeStream->start();
    if (result == oboe::Result::OK) {
        state = STARTED;
        return EAS_SUCCESS;
    }
    ALOGE("%s: can't start audio stream. %s", __func__, convertToText(result));
    return EAS_FAILURE;
}

EAS_RESULT mmapi::Player::pause() {
    if (oboeStream->getState() < oboe::StreamState::Starting ||
        oboeStream->getState() > oboe::StreamState::Started) {
        return EAS_SUCCESS;
    }
    oboe::Result &&result = oboeStream->pause();
    if (result == oboe::Result::OK) {
        state = PREFETCHED;
        return EAS_SUCCESS;
    }
    return EAS_FAILURE;
}

void mmapi::Player::deallocate() {
    oboeStream->stop();
    oboeStream->close();
    oboeStream.reset();
    EAS_Locate(easHandle, media, 0, EAS_FALSE);
    if (looping != 0) {
        EAS_SetRepeat(easHandle, media, looping);
    }
    state = REALIZED;
}

void mmapi::Player::close() {
    if (state == CLOSED) {
        return;
    } else if (state == STARTED) {
        oboeStream->stop();
    }
    if (state >= PREFETCHED) {
        oboeStream->close();
        oboeStream.reset();
    }
    EAS_CloseFile(easHandle, media);
    delete file;
    EAS_Shutdown(easHandle);
    state = CLOSED;
}

EAS_I32 mmapi::Player::setMediaTime(EAS_I32 now) {
    EAS_RESULT result = EAS_Locate(easHandle, media, now, EAS_FALSE);
    if (result != EAS_SUCCESS) {
        ALOGE("%s: EAS_Locate return %s", __func__, MMAPI_GetErrorString(result));
    }
    EAS_I32 pTime = -1;
    result = EAS_GetLocation(easHandle, media, &pTime);
    if (result != EAS_SUCCESS) {
        ALOGE("%s: EAS_GetLocation return %s", __func__, MMAPI_GetErrorString(result));
    }
    return pTime;
}

EAS_I32 mmapi::Player::getMediaTime() {
    EAS_I32 pTime = -1;
    EAS_RESULT result = EAS_GetLocation(easHandle, media, &pTime);
    if (result != EAS_SUCCESS) {
        ALOGE("%s: EAS_GetLocation return %s", __func__, MMAPI_GetErrorString(result));
    }
    return pTime;
}

void mmapi::Player::setRepeat(EAS_I32 count) {
    if (count > 0) {
        count--;
    }
    looping = count;
    if (state < REALIZED) {
        return;
    }
    EAS_RESULT result = EAS_SetRepeat(easHandle, media, count);
    if (result != EAS_SUCCESS) {
        ALOGE("%s: EAS_SetRepeat return %s", __func__, MMAPI_GetErrorString(result));
    }
}

EAS_I32 mmapi::Player::setPan(EAS_I32 pan) {
    return 0;
}

EAS_I32 mmapi::Player::getPan() {
    return 0;
}

void mmapi::Player::setMute(EAS_BOOL mute) {
    if (mute && !muted) {
        if (state >= REALIZED) {
            volume = EAS_GetVolume(easHandle, media);
            EAS_SetVolume(easHandle, media, 0);
        }
        muted = EAS_TRUE;
    } else if (!mute && muted) {
        if (state >= REALIZED) {
            EAS_SetVolume(easHandle, media, volume);
        }
        muted = EAS_FALSE;
    }
}

int mmapi::Player::setLevel(EAS_I32 level) {
    volume = level;
    if (state >= REALIZED) {
        EAS_SetVolume(easHandle, media, level);
        volume = EAS_GetVolume(easHandle, media);
    }
    return volume;
}

EAS_BOOL mmapi::Player::isMuted() const {
    return muted;
}

EAS_I32 mmapi::Player::getLevel() {
    if (state >= REALIZED) {
        volume = EAS_GetVolume(easHandle, media);
    }
    return volume;
}

EAS_RESULT mmapi::Player::realize() {
    state = REALIZED;
    return EAS_SUCCESS;
}

oboe::DataCallbackResult
mmapi::Player::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    EAS_STATE easState;
    EAS_State(easHandle, media, &easState);
    if ((easState == EAS_STATE_STOPPED) || (easState == EAS_STATE_ERROR)) {
        return oboe::DataCallbackResult::Stop;
    }

    auto *p = static_cast<EAS_PCM *>(audioData);
    int numFramesOutput = 0;
    EAS_RESULT result;
    for (int i = 0; i < NUM_COMBINE_BUFFERS; i++) {
        EAS_I32 numRendered;
        result = EAS_Render(easHandle, p, easConfig->mixBufferSize, &numRendered);
        if (result != EAS_SUCCESS) {
            ALOGE("EAS_Render() returned %ld, numBytesOutput = %d", result, numFramesOutput);
            return oboe::DataCallbackResult::Stop; // Stop processing to prevent infinite loops.
        }
        p += numRendered * easConfig->numChannels;
        numFramesOutput += numRendered;
    }
    EAS_I32 mediaTime(0);
    result = EAS_GetLocation(easHandle, media, &mediaTime);
    if (result == EAS_SUCCESS) {
        if (mediaTime >= duration) {
            ALOGD("%s: END_OF_MEDIA", __func__);
        }
    }
    return oboe::DataCallbackResult::Continue;
}

void mmapi::Player::onErrorAfterClose(oboe::AudioStream *stream, oboe::Result result) {
    if (result == oboe::Result::ErrorDisconnected) {
        EAS_RESULT res = createAudioStream();
        if (res == EAS_SUCCESS && state == STARTED) {
            oboeStream->requestStart();
        }
    }
}
