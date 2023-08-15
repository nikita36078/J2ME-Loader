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

    public:
        Player(EAS_DATA_HANDLE easHandle);

        ~Player() override;

        EAS_RESULT init(const char *path);
        bool prefetch();
        bool start();
        bool pause();
        void deallocate();
        void close();
        int64_t setMediaTime(int64_t now);
        int64_t getMediaTime();
        void setRepeat(int32_t count);
        int32_t setPan(int32_t pan);
        int32_t getPan();
        void setMute(bool mute);
        int setVolume(int32_t level);
        bool isMuted() const;
        int32_t getVolume();
        bool realize();

        oboe::DataCallbackResult
        onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;

        void onErrorAfterClose(oboe::AudioStream *stream, oboe::Result result) override;

    private:
        bool createAudioStream();

        static EAS_DLSLIB_HANDLE easDlsHandle;
        const S_EAS_LIB_CONFIG *easConfig = EAS_Config();
        EAS_DATA_HANDLE easHandle;
        EAS_HANDLE media = nullptr;
        std::shared_ptr<oboe::AudioStream> oboeStream;
        State state = UNREALIZED;
        EAS_BOOL muted = EAS_FALSE;
        EAS_I32 volume = 100;
        EAS_I32 looping = 0;
        File *file = nullptr;
        EAS_I32 timeSet = -1;
        EAS_I32 loopCount = 1;
        PlayerListener *playerListener = nullptr;

    public:
        EAS_I32 duration = -1;

        void setListener(PlayerListener *pListener);

        static EAS_RESULT initSoundBank(const char *string);
    };
}

#endif //JL_MOD_MMAPI_PLAYER_H
