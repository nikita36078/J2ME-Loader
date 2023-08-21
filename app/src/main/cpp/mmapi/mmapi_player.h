//
// Created by woesss on 08.07.2023.
//

#ifndef MMAPI_EAS_PLAYER_H
#define MMAPI_EAS_PLAYER_H

#include <oboe/Oboe.h>
#include "libsonivox/eas.h"
#include "mmapi_file.h"
#include "mmapi/PlayerListener.h"
#include "mmapi/BasePlayer.h"

namespace mmapi {
    namespace eas {
        class Player : public BasePlayer {
            static EAS_DLSLIB_HANDLE soundBank;

            const S_EAS_LIB_CONFIG *easConfig = EAS_Config();
            EAS_DATA_HANDLE easHandle;
            EAS_HANDLE media;
            File *file;

        public:
            Player(EAS_DATA_HANDLE easHandle, File *file, EAS_HANDLE stream, const int64_t duration);
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
        }; // class Player
    } // namespace eas
} // namespace mmapi

#endif //MMAPI_EAS_PLAYER_H
