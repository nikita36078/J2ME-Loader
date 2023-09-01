//
// Created by woesss on 01.08.2023.
//

#ifndef MMAPI_TSF_PLAYER_H
#define MMAPI_TSF_PLAYER_H

#include "tsf.h"
#include "tml.h"
#include "mmapi/PlayerListener.h"
#include "mmapi/BasePlayer.h"

namespace mmapi {
    namespace tiny {
        class Player : public BasePlayer {
            static tsf *soundBank;

            tsf *synth;
            tml_message *media;
            int64_t playTime = 0;
            tml_message *currentMsg;

        public:
            Player(tsf *synth, tml_message *midi, const int64_t duration);
            ~Player() override;

            void deallocate() override;
            void close() override;
            int64_t getMediaTime() override;
            int32_t setVolume(int32_t level) override;

            oboe::DataCallbackResult
            onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;

            static int32_t initSoundBank(const char *sound_bank);
            static int32_t createPlayer(const char *path, Player **pPlayer);

        private:
            oboe::Result createAudioStream() override;
            void processEvents(bool playMode);

            static float computeGain(int32_t level);
        }; // class Player
    } // namespace tiny
} // namespace mmapi

#endif //MMAPI_TSF_PLAYER_H
