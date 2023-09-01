//
// Created by woesss on 24.08.2023.
//

#ifndef MMAPI_BASE_PLAYER_H
#define MMAPI_BASE_PLAYER_H

#include "PlayerListener.h"
#include <oboe/Oboe.h>

namespace mmapi {

    class BasePlayer : public oboe::AudioStreamCallback {
        bool muted = false;
        int32_t volume = 100;
    protected:
        int32_t loopCount = 1;
        int32_t looping = 0;
        std::shared_ptr<oboe::AudioStream> oboeStream;
        int64_t timeToSet = -1;
        PlayerState state = UNREALIZED;
        PlayerListener *playerListener = nullptr;
    public:
        const int64_t duration;

        BasePlayer(const int64_t duration);
        ~BasePlayer() override;

        virtual oboe::Result prefetch();
        virtual oboe::Result start();
        virtual oboe::Result pause();
        virtual void deallocate();
        virtual void close();
        virtual int64_t setMediaTime(int64_t now);
        virtual int64_t getMediaTime() = 0;
        virtual void setRepeat(int32_t count);
        virtual int32_t setPan(int32_t pan);
        virtual int32_t getPan();
        virtual void setMute(bool mute);
        virtual int32_t setVolume(int32_t level);
        virtual bool isMuted() const;
        virtual int32_t getVolume();
        virtual bool realize();
        void setListener(PlayerListener *listener);
        void onErrorAfterClose(oboe::AudioStream *stream, oboe::Result result) override;

    protected:
        virtual oboe::Result createAudioStream() = 0;
    }; // class BasePlayer
} // namespace mmapi

#endif //MMAPI_BASE_PLAYER_H