//
// Created by woesss on 01.08.2023.
//

#ifndef JL_MOD_PLAYER_H
#define JL_MOD_PLAYER_H

#include <oboe/Oboe.h>
#include "tsf.h"
#include "tml.h"
#include "PlayerListener.h"

namespace tsf_mmapi {
    enum State {
        CLOSED = 0,
        UNREALIZED = 100,
        REALIZED = 200,
        PREFETCHED = 300,
        STARTED = 400,
    };

    class Player : public oboe::AudioStreamCallback {
        static tsf *soundBank;
        tsf *synth;
        tml_message *media = nullptr;
        std::shared_ptr<oboe::AudioStream> oboeStream;
        State state = UNREALIZED;
        bool muted = false;
        int32_t volume = 100;
        int32_t looping = 0;
        int64_t playTime = 0;
        int64_t timeSet = -1;
        int32_t loopCount = 1;
        tml_message *currentMsg = nullptr;
        PlayerListener *playerListener = nullptr;

    public:
        int64_t duration = -1;

        Player();
        ~Player() override;

        bool init(const char *path);
        bool prefetch();
        bool start();
        bool pause();
        void deallocate();
        void close();
        int64_t setMediaTime(int64_t now);
        int64_t getMediaTime() const;
        void setRepeat(int32_t count);
        int32_t setPan(int32_t pan);
        int32_t getPan();
        void setMute(bool mute);
        int32_t setVolume(int32_t level);
        bool isMuted() const;
        int32_t getLevel() const;
        bool realize();
        void setListener(PlayerListener *listener);

        oboe::DataCallbackResult
        onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;
        void onErrorAfterClose(oboe::AudioStream *stream, oboe::Result result) override;

        static bool initSoundBank(const char *sound_bank);

    private:
        bool createAudioStream();
        static float computeGain(int32_t level);
    };

} // tsf_mmapi

#endif //JL_MOD_PLAYER_H
