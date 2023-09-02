//
// Created by woesss on 08.07.2023.
//

#ifndef JL_MOD_MMAPI_PLAYER_H
#define JL_MOD_MMAPI_PLAYER_H

#include <oboe/Oboe.h>
#include "libsonivox/eas.h"
#include "mmapi_file.h"

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

        long setDataSource(const char *path);
        EAS_RESULT prefetch();
        EAS_RESULT start();
        EAS_RESULT pause();
        void deallocate();
        void close();
        EAS_I32 setMediaTime(EAS_I32 now);

        oboe::DataCallbackResult
        onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;

        void onErrorAfterClose(oboe::AudioStream *stream, oboe::Result result) override;

    private:
        EAS_RESULT createAudioStream();

        const S_EAS_LIB_CONFIG *easConfig = EAS_Config();
        EAS_DATA_HANDLE easHandle;
        EAS_HANDLE media = nullptr;
        std::shared_ptr<oboe::AudioStream> oboeStream;
        State state = UNREALIZED;
        EAS_BOOL muted = EAS_FALSE;
        EAS_I32 volume = 100;
        EAS_I32 looping = 0;
        File *file = nullptr;

    public:
        EAS_I32 duration = -1;

        EAS_I32 getMediaTime();
        void setRepeat(EAS_I32 count);
        EAS_I32 setPan(EAS_I32 pan);
        EAS_I32 getPan();
        void setMute(EAS_BOOL mute);
        int setLevel(EAS_I32 level);
        EAS_BOOL isMuted() const;
        EAS_I32 getLevel();
    };
}

#endif //JL_MOD_MMAPI_PLAYER_H
