//
// Created by woesss on 08.07.2023.
//

#ifndef JL_MOD_MMAPI_PLAYER_H
#define JL_MOD_MMAPI_PLAYER_H

#include <oboe/Oboe.h>
#include "libsonivox/eas.h"
#include "mmapi_file.h"
#include "PlayerListener.h"

namespace mmapi {
    enum State {
        CLOSED = 0,
        UNREALIZED = 100,
        REALIZED = 200,
        PREFETCHED = 300,
        STARTED = 400,
    };

    class Player : public oboe::AudioStreamCallback {
        static EAS_DLSLIB_HANDLE soundBank;

        const S_EAS_LIB_CONFIG *easConfig = EAS_Config();
        EAS_DATA_HANDLE easHandle;
        EAS_HANDLE media;
        std::shared_ptr<oboe::AudioStream> oboeStream;
        State state = UNREALIZED;
        bool muted = false;
        int32_t volume = 100;
        int32_t looping = 0;
        int64_t timeToSet = -1;
        int32_t loopCount = 1;
        PlayerListener *playerListener = nullptr;
        File *file;

    public:
        int64_t duration;

        Player(EAS_DATA_HANDLE easHandle, File *file, EAS_HANDLE stream, int64_t duration);
        ~Player() override;

        oboe::Result prefetch();
        oboe::Result start();
        oboe::Result pause();
        void deallocate();
        void close();
        int64_t setMediaTime(int64_t now);
        int64_t getMediaTime();
        void setRepeat(int32_t count);
        int32_t setPan(int32_t pan);
        int32_t getPan();
        void setMute(bool mute);
        int32_t setVolume(int32_t level);
        bool isMuted() const;
        int32_t getVolume();
        bool realize();
        void setListener(PlayerListener *listener);

        oboe::DataCallbackResult
        onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;
        void onErrorAfterClose(oboe::AudioStream *stream, oboe::Result result) override;

        static EAS_RESULT initSoundBank(const char *sound_bank);
        static EAS_RESULT createPlayer(const char *path, Player **pPlayer);

    private:
        oboe::Result createAudioStream();
    };
}

#endif //JL_MOD_MMAPI_PLAYER_H
