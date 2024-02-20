//
// Created by woesss on 01.08.2023.
//

#ifndef MMAPI_TSF_PLAYER_H
#define MMAPI_TSF_PLAYER_H

#include "tsf.h"
#include "tml.h"
#include "mmapi/PlayerListener.h"
#include "mmapi/BasePlayer.h"
#include "util/jbytearray.h"

namespace mmapi {
    namespace tiny {
        class Player : public BasePlayer {
            static tsf *soundBank;

            tsf *synth;
            tml_message *media;
            tml_message *currentMsg;

        public:
            Player(tsf *synth, tml_message *midi, const int64_t duration);
            ~Player() override;

            void deallocate() override;
            void close() override;
            oboe::Result prefetch() override;
            void setVolume(int32_t level) override;
            int32_t setDataSource(util::JByteArrayPtr *data);

            oboe::DataCallbackResult
            onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;

            static int32_t initSoundBank(const char *sound_bank);
            static int32_t createPlayer(const char *locator, Player **pPlayer);

        protected:
            oboe::Result createAudioStream() override;

        private:
            void processEvents(bool playMode);

            static float computeGain(int32_t level);
        }; // class Player
    } // namespace tiny
} // namespace mmapi

#endif //MMAPI_TSF_PLAYER_H
