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

namespace mmapi {
    EAS_DLSLIB_HANDLE Player::easDlsHandle{nullptr};

    Player::Player(EAS_DATA_HANDLE easHandle) : easHandle(easHandle) {
        EAS_SetGlobalDLSLib(easHandle, Player::easDlsHandle);
    }

    Player::~Player() {
        close();
        delete playerListener;
    }

    bool Player::createAudioStream() {
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
            return false;
        }
        return true;
    }

    EAS_RESULT Player::init(const char *path) {
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

    bool Player::prefetch() {
        bool result = createAudioStream();
        if (result) {
            state = PREFETCHED;
        }
        return result;
    }

    bool Player::start() {
        oboe::Result &&result = oboeStream->start();
        if (result == oboe::Result::OK) {
            state = STARTED;
            return true;
        }
        ALOGE("%s: can't start audio stream. %s", __func__, convertToText(result));
        return false;
    }

    bool Player::pause() {
        if (oboeStream->getState() < oboe::StreamState::Starting ||
            oboeStream->getState() > oboe::StreamState::Started) {
            return true;
        }
        oboe::Result &&result = oboeStream->pause();
        if (result == oboe::Result::OK) {
            state = PREFETCHED;
            return true;
        }
        return false;
    }

    void Player::deallocate() {
        oboeStream->stop();
        oboeStream->close();
        oboeStream.reset();
        EAS_Locate(easHandle, media, 0, EAS_FALSE);
        loopCount = looping;
        state = REALIZED;
    }

    void Player::close() {
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

    int64_t Player::setMediaTime(int64_t now) {
        if (now < 0) {
            now = 0;
        } else if (now > duration) {
            now = duration;
        }
        timeSet = now;
        return now;
    }

    int64_t Player::getMediaTime() {
        EAS_I32 pTime = -1;
        EAS_RESULT result = EAS_GetLocation(easHandle, media, &pTime);
        if (result != EAS_SUCCESS) {
            ALOGE("%s: EAS_GetLocation return %s", __func__, MMAPI_GetErrorString(result));
        }
        return pTime;
    }

    void Player::setRepeat(int32_t count) {
        if (count > 0) {
            count--;
        }
        looping = count;
        loopCount = count;
    }

    int32_t Player::setPan(int32_t pan) {
        return 0;
    }

    int32_t Player::getPan() {
        return 0;
    }

    void Player::setMute(bool mute) {
        if (mute && !muted) {
            if (state >= REALIZED) {
                volume = EAS_GetVolume(easHandle, media);
                EAS_SetVolume(easHandle, media, 0);
            }
            muted = true;
        } else if (!mute && muted) {
            if (state >= REALIZED) {
                EAS_SetVolume(easHandle, media, volume);
            }
            muted = false;
        }
    }

    int32_t Player::setVolume(int32_t level) {
        volume = level;
        if (state >= REALIZED) {
            EAS_SetVolume(easHandle, media, level);
            volume = EAS_GetVolume(easHandle, media);
        }
        return volume;
    }

    bool Player::isMuted() const {
        return muted;
    }

    int32_t Player::getVolume() {
        if (state >= REALIZED) {
            volume = EAS_GetVolume(easHandle, media);
        }
        return volume;
    }

    bool Player::realize() {
        state = REALIZED;
        return true;
    }

    void Player::setListener(PlayerListener *listener) {
        this->playerListener = listener;
    }

    EAS_RESULT Player::initSoundBank(const char *string) {
        EAS_DATA_HANDLE easHandle;
        EAS_RESULT result = EAS_Init(&easHandle);
        if (result != EAS_SUCCESS) {
            return result;
        }

        mmapi::File file(string, "rb");
        result = EAS_LoadDLSCollection(easHandle, nullptr, &file.easFile);
        if (result == EAS_SUCCESS) {
            EAS_GetGlobalDLSLib(easHandle, &mmapi::Player::easDlsHandle);
        }
        EAS_Shutdown(easHandle);
        return result;
    }

    oboe::DataCallbackResult
    Player::onAudioReady(oboe::AudioStream *audioStream, void *audioData,
                                int32_t numFrames) {
        EAS_STATE easState;
        EAS_State(easHandle, media, &easState);
        if (easState == EAS_STATE_STOPPED || easState == EAS_STATE_ERROR) {
            playerListener->postEvent(END_OF_MEDIA, getMediaTime());
            if (looping == -1 || (--loopCount) > 0) {
                setMediaTime(0);
                playerListener->postEvent(START, 0);
            } else {
                return oboe::DataCallbackResult::Stop;
            }
        }

        if (timeSet != -1) {
            EAS_RESULT result = EAS_Locate(easHandle, media, timeSet, EAS_FALSE);
            if (result != EAS_SUCCESS) {
                ALOGE("%s: EAS_Locate() return %s", __func__, MMAPI_GetErrorString(result));
            }
            timeSet = -1;
        }

        auto *p = static_cast<EAS_PCM *>(audioData);
        int numFramesOutput = 0;
        EAS_RESULT result;
        for (int i = 0; i < NUM_COMBINE_BUFFERS; i++) {
            EAS_I32 numRendered;
            result = EAS_Render(easHandle, p, easConfig->mixBufferSize, &numRendered);
            if (result != EAS_SUCCESS) {
                playerListener->postEvent(ERROR, result);
                ALOGE("EAS_Render() returned %s, numBytesOutput = %d", MMAPI_GetErrorString(result),
                      numFramesOutput);
                return oboe::DataCallbackResult::Stop; // Stop processing to prevent infinite loops.
            }
            p += numRendered * easConfig->numChannels;
            numFramesOutput += numRendered;
        }
        return oboe::DataCallbackResult::Continue;
    }

    void Player::onErrorAfterClose(oboe::AudioStream *stream, oboe::Result result) {
        if (result == oboe::Result::ErrorDisconnected) {
            bool res = createAudioStream();
            if (res && state == STARTED) {
                oboeStream->requestStart();
            }
        }
    }
} // mmapi
