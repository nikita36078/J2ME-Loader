//
// Created by woesss on 08.07.2023.
//

#include "eas_player.h"
#include "eas_file.h"
#include "libsonivox/eas.h"
#include "util/log.h"
#include "eas_strings.h"

#define LOG_TAG "MMAPI"
#define NUM_COMBINE_BUFFERS 4

namespace mmapi {
    namespace eas {
        EAS_DLSLIB_HANDLE Player::soundBank{nullptr};

        Player::Player(EAS_DATA_HANDLE easHandle, FileImpl *file, EAS_HANDLE stream, const int64_t duration)
                : BasePlayer(duration), easHandle(easHandle), file(file), media(stream) {
            EAS_SetGlobalDLSLib(easHandle, Player::soundBank);
        }

        Player::~Player() {
            close();
        }

        int32_t Player::createPlayer(const char *path, Player **pPlayer) {
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
            FileImpl *file = new FileImpl(path, "rb");
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
            ALOGV("EAS_checkFileType(): %s file recognized", EAS_GetFileTypeString(type));
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

        void Player::deallocate() {
            BasePlayer::deallocate();
            EAS_Locate(easHandle, media, 0, EAS_FALSE);
        }

        void Player::close() {
            BasePlayer::close();
            EAS_CloseFile(easHandle, media);
            EAS_Shutdown(easHandle);
            delete file;
        }

        int64_t Player::getMediaTime() {
            long pTime = -1;
            long result1 = EAS_GetLocation(easHandle, media, &pTime);
            if (result1 != EAS_SUCCESS) {
                ALOGE("%s: EAS_GetLocation return %s", __func__, EAS_GetErrorString(result1));
            }
            return pTime * 1000LL;
        }

        int32_t Player::setVolume(int32_t level) {
            EAS_SetVolume(easHandle, media, level);
            return BasePlayer::setVolume(level);
        }

        int32_t Player::initSoundBank(const char *sound_bank) {
            EAS_DATA_HANDLE easHandle;
            EAS_RESULT result = EAS_Init(&easHandle);
            if (result != EAS_SUCCESS) {
                return result;
            }

            FileImpl file(sound_bank, "rb");
            result = EAS_LoadDLSCollection(easHandle, nullptr, &file.easFile);
            if (result == EAS_SUCCESS) {
                EAS_GetGlobalDLSLib(easHandle, &Player::soundBank);
            }
            EAS_Shutdown(easHandle);
            return result;
        }

        oboe::DataCallbackResult
        Player::onAudioReady(oboe::AudioStream *audioStream, void *audioData,
                             int32_t numFrames) {
            memset(audioData, 0, sizeof(EAS_PCM) * easConfig->numChannels);
            EAS_STATE easState;
            EAS_State(easHandle, media, &easState);
            if (easState == EAS_STATE_STOPPED || easState == EAS_STATE_ERROR) {
                playerListener->postEvent(END_OF_MEDIA, getMediaTime());
                EAS_RESULT result = EAS_Locate(easHandle, media, 0, EAS_FALSE);
                if (result != EAS_SUCCESS) {
                    ALOGE("%s: EAS_Locate() return %s", __func__, EAS_GetErrorString(result));
                }
                if (looping == -1 || (--loopCount) > 0) {
                    playerListener->postEvent(START, 0);
                } else {
                    return oboe::DataCallbackResult::Stop;
                }
            }

            if (timeToSet != -1) {
                EAS_RESULT result = EAS_Locate(easHandle, media,
                                               static_cast<EAS_I32>(timeToSet / 1000LL), EAS_FALSE);
                if (result != EAS_SUCCESS) {
                    ALOGE("%s: EAS_Locate() return %s", __func__, EAS_GetErrorString(result));
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
                          EAS_GetErrorString(result),
                          numFramesOutput);
                    return oboe::DataCallbackResult::Stop; // Stop processing to prevent infinite loops.
                }
                p += numRendered * easConfig->numChannels;
                numFramesOutput += numRendered;
            }
            return oboe::DataCallbackResult::Continue;
        }
    } // namespace eas
} // namespace mmapi
