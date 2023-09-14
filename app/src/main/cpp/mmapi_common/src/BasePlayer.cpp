//
// Created by woesss on 24.08.2023.
//

#include "BasePlayer.h"
#include "log.h"

#define LOG_TAG "mmapi"

namespace mmapi {
    BasePlayer::BasePlayer(const int64_t duration) : duration(duration) {}

    BasePlayer::~BasePlayer() {
        delete playerListener;
    }

    oboe::Result BasePlayer::prefetch() {
        oboe::Result result = createAudioStream();
        if (result == oboe::Result::OK) {
            state = PREFETCHED;
        }
        return result;
    }

    oboe::Result BasePlayer::start() {
        oboe::Result result = oboeStream->start();
        if (result != oboe::Result::OK) {
            ALOGE("%s: can't start audio stream. %s", __func__, oboe::convertToText(result));
            return result;
        }
        state = STARTED;
        return result;
    }

    oboe::Result BasePlayer::pause() {
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

    void BasePlayer::deallocate() {
        oboeStream->stop();
        oboeStream->close();
        oboeStream.reset();
        loopCount = looping;
        state = REALIZED;
    }

    void BasePlayer::close() {
        if (state == CLOSED) {
            return;
        } else if (state == STARTED) {
            oboeStream->stop();
        }
        if (state >= PREFETCHED) {
            oboeStream->close();
            oboeStream.reset();
        }
        state = CLOSED;
    }

    int64_t BasePlayer::setMediaTime(int64_t now) {
        if (now < 0) {
            now = 0;
        } else if (now > duration) {
            now = duration;
        }
        timeToSet = now;
        return now;
    }

    void BasePlayer::setRepeat(int32_t count) {
        looping = count;
        loopCount = count;
    }

    int32_t BasePlayer::setPan(int32_t pan) {
        return 0;
    }

    int32_t BasePlayer::getPan() {
        return 0;
    }

    void BasePlayer::setMute(bool mute) {
        if (mute && !muted) {
            volume = getVolume();
            setVolume(0);
            muted = true;
        } else if (!mute && muted) {
            setVolume(volume);
            muted = false;
        }
    }

    int32_t BasePlayer::setVolume(int32_t level) {
        volume = level;
        return level;
    }

    bool BasePlayer::isMuted() const {
        return muted;
    }

    int32_t BasePlayer::getVolume() {
        return volume;
    }

    bool BasePlayer::realize() {
        state = REALIZED;
        return true;
    }

    void BasePlayer::setListener(PlayerListener *listener) {
        this->playerListener = listener;
    }

    void BasePlayer::onErrorAfterClose(oboe::AudioStream *stream, oboe::Result result) {
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
} // namespace mmapi