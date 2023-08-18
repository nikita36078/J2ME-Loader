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
    EAS_DLSLIB_HANDLE Player::soundBank{nullptr};

    Player::Player(EAS_DATA_HANDLE easHandle, File *file, EAS_HANDLE stream, int64_t duration)
            : easHandle(easHandle),
            file(file),
            media(stream),
            duration(duration) {
        EAS_SetGlobalDLSLib(easHandle, Player::soundBank);
    }

    Player::~Player() {
        close();
        delete playerListener;
    }

    EAS_RESULT Player::createPlayer(const char *path, Player **pPlayer) {
        EAS_DATA_HANDLE easHandle;
        EAS_RESULT result = EAS_Init(&easHandle);
        if (result != EAS_SUCCESS) {
            return result;
        }
        if (path == nullptr) {
            result = EAS_ERROR_INVALID_PARAMETER;
            EAS_Shutdown(easHandle);
            return result;
        }
        File *file = new File(path, "rb");
        EAS_HANDLE stream;
        result = EAS_OpenFile(easHandle, &file->easFile, &stream);
        if (result != EAS_SUCCESS) {
            delete file;
            EAS_Shutdown(easHandle);
            return result;
        }
        result = EAS_Prepare(easHandle, stream);
        if (result != EAS_SUCCESS) {
            EAS_CloseFile(easHandle, stream);
            EAS_Shutdown(easHandle);
            delete file;
            return result;
        }
        EAS_I32 type = EAS_FILE_UNKNOWN;
        result = EAS_GetFileType(easHandle, stream, &type);
        if (result != EAS_SUCCESS) {
            EAS_CloseFile(easHandle, stream);
            EAS_Shutdown(easHandle);
            delete file;
            return result;
        }
        ALOGV("EAS_checkFileType(): %s file recognized", MMAPI_GetFileTypeString(type));
        if (type == EAS_FILE_WAVE_PCM) {
            EAS_CloseFile(easHandle, stream);
            EAS_Shutdown(easHandle);
            delete file;
            result = EAS_ERROR_INVALID_PCM_TYPE;
            return result;
        }
        EAS_I32 duration;
        result = EAS_ParseMetaData(easHandle, stream, &duration);
        if (result != EAS_SUCCESS) {
            EAS_CloseFile(easHandle, stream);
            EAS_Shutdown(easHandle);
            delete file;
            return result;
        }
        *pPlayer = new Player(easHandle, file, stream, duration > 0 ? duration * 1000LL : -1);
        return result;
    }

    oboe::Result Player::createAudioStream() {
        oboe::AudioStreamBuilder builder;
        builder.setDirection(oboe::Direction::Output);
        builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
        builder.setSharingMode(oboe::SharingMode::Shared);
        builder.setFormat(oboe::AudioFormat::I16);
        builder.setChannelCount(static_cast<int>(easConfig->numChannels));
        builder.setSampleRate(static_cast<int>(easConfig->sampleRate));
        builder.setCallback(this);
        builder.setFramesPerDataCallback(easConfig->mixBufferSize * NUM_COMBINE_BUFFERS);

        oboe::Result result = builder.openStream(oboeStream);
        if (result != oboe::Result::OK) {
            oboeStream.reset();
            ALOGE("%s: can't open audio stream. %s", __func__, oboe::convertToText(result));
        }
        return result;
    }

    oboe::Result Player::prefetch() {
        oboe::Result result = createAudioStream();
        if (result == oboe::Result::OK) {
            state = PREFETCHED;
        }
        return result;
    }

    oboe::Result Player::start() {
        oboe::Result result = oboeStream->start();
        if (result != oboe::Result::OK) {
            ALOGE("%s: can't start audio stream. %s", __func__, oboe::convertToText(result));
            return result;
        }
        state = STARTED;
        return result;
    }

    oboe::Result Player::pause() {
        if (oboeStream->getState() < oboe::StreamState::Starting ||
            oboeStream->getState() > oboe::StreamState::Started) {
            return oboe::Result::OK;
        }
        oboe::Result result = oboeStream->pause();
        if (result != oboe::Result::OK) {
            return result;
        }
        state = PREFETCHED;
        return result;
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
        EAS_Shutdown(easHandle);
        delete file;
        state = CLOSED;
    }

    int64_t Player::setMediaTime(int64_t now) {
        if (now < 0) {
            now = 0;
        } else if (now > duration) {
            now = duration;
        }
        timeToSet = now;
        return now;
    }

    int64_t Player::getMediaTime() {
        EAS_I32 pTime = -1;
        EAS_RESULT result = EAS_GetLocation(easHandle, media, &pTime);
        if (result != EAS_SUCCESS) {
            ALOGE("%s: EAS_GetLocation return %s", __func__, MMAPI_GetErrorString(result));
        }
        return pTime * 1000LL;
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

    EAS_RESULT Player::initSoundBank(const char *sound_bank) {
        EAS_DATA_HANDLE easHandle;
        EAS_RESULT result = EAS_Init(&easHandle);
        if (result != EAS_SUCCESS) {
            return result;
        }

        mmapi::File file(sound_bank, "rb");
        result = EAS_LoadDLSCollection(easHandle, nullptr, &file.easFile);
        if (result == EAS_SUCCESS) {
            EAS_GetGlobalDLSLib(easHandle, &mmapi::Player::soundBank);
        }
        EAS_Shutdown(easHandle);
        return result;
    }

    oboe::DataCallbackResult
    Player::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
        memset(audioData, 0, sizeof(EAS_PCM) * easConfig->numChannels);
        EAS_STATE easState;
        EAS_State(easHandle, media, &easState);
        if (easState == EAS_STATE_STOPPED || easState == EAS_STATE_ERROR) {
            playerListener->postEvent(END_OF_MEDIA, getMediaTime());
            EAS_RESULT result = EAS_Locate(easHandle, media, 0, EAS_FALSE);
            if (result != EAS_SUCCESS) {
                ALOGE("%s: EAS_Locate() return %s", __func__, MMAPI_GetErrorString(result));
            }
            if (looping == -1 || (--loopCount) > 0) {
                playerListener->postEvent(START, 0);
            } else {
                return oboe::DataCallbackResult::Stop;
            }
        }

        if (timeToSet != -1) {
            EAS_RESULT result = EAS_Locate(easHandle, media, static_cast<EAS_I32>(timeToSet / 1000LL), EAS_FALSE);
            if (result != EAS_SUCCESS) {
                ALOGE("%s: EAS_Locate() return %s", __func__, MMAPI_GetErrorString(result));
            }
            timeToSet = -1;
        }

        auto *p = static_cast<EAS_PCM *>(audioData);
        int numFramesOutput = 0;
        EAS_RESULT result;
        for (int i = 0; i < NUM_COMBINE_BUFFERS; i++) {
            EAS_I32 numRendered;
            result = EAS_Render(easHandle, p, easConfig->mixBufferSize, &numRendered);
            if (result != EAS_SUCCESS) {
                playerListener->postEvent(ERROR, result);
                ALOGE("%s: EAS_Render() returned %s, numFramesOutput = %d",
                      __func__,
                      MMAPI_GetErrorString(result),
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
            oboe::Result res = createAudioStream();
            if (res != oboe::Result::OK) {
                ALOGE("%s: reconnect error=%s", __func__, oboe::convertToText(res));
                playerListener->postEvent(ERROR, 0);
            } else if (state == STARTED) {
                oboeStream->requestStart();
            }
        } else {
            ALOGE("%s: %s", __func__, oboe::convertToText(result));
            playerListener->postEvent(ERROR, 0);
        }
    }
} // mmapi
