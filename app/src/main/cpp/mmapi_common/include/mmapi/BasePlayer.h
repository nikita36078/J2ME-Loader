//
// Created by woesss on 24.08.2023.
//

#ifndef MMAPI_BASE_PLAYER_H
#define MMAPI_BASE_PLAYER_H

#include "PlayerListener.h"
#include <oboe/Oboe.h>

namespace mmapi {

    class BasePlayer : public oboe::AudioStreamCallback {
    protected:
        int32_t loopCount = 0;
        int32_t looping = 0;
        std::shared_ptr<oboe::AudioStream> oboeStream;
        int64_t playTime = 0;
        int64_t seekTime = -1;
        PlayerState state = UNREALIZED;
        PlayerListener *playerListener = nullptr;
    public:
        int64_t duration;

        BasePlayer(const int64_t duration);
        BasePlayer(const BasePlayer &) = delete;
        ~BasePlayer() override;

        virtual oboe::Result prefetch();
        virtual oboe::Result pause();
        virtual void deallocate();
        virtual void close();
        virtual int64_t setMediaTime(int64_t now);
        virtual int64_t getMediaTime();
        virtual void setVolume(int32_t level) = 0;

        oboe::Result start();
        bool realize();
        void setRepeat(int32_t count);
        void setPan(int32_t pan);
        void setListener(PlayerListener *listener);
        void onErrorAfterClose(oboe::AudioStream *stream, oboe::Result result) override;

    protected:
        virtual oboe::Result createAudioStream() = 0;
    }; // class BasePlayer
} // namespace mmapi

#endif //MMAPI_BASE_PLAYER_H