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
        int64_t playTime = -1;
        int64_t seekTime = -1;
        PlayerState state = UNREALIZED;
        PlayerListener *playerListener = nullptr;
        float gainLeft = 1;
        float gainRight = 1;
    public:
        int64_t duration;

        BasePlayer(const int64_t duration);
        BasePlayer(const BasePlayer &) = delete;
        ~BasePlayer() override;

        virtual oboe::Result prefetch();
        virtual oboe::Result pause();
        virtual void deallocate();
        virtual void close();

        oboe::Result start();
        bool realize();
        int64_t getMediaTime();
        int64_t setMediaTime(int64_t now);
        void setRepeat(int32_t count);
        void setVolume(float_t left, float_t right);
        void setListener(PlayerListener *listener);
        void onErrorAfterClose(oboe::AudioStream *stream, oboe::Result result) override;

    protected:
        virtual oboe::Result createAudioStream() = 0;
    }; // class BasePlayer
} // namespace mmapi

#endif //MMAPI_BASE_PLAYER_H