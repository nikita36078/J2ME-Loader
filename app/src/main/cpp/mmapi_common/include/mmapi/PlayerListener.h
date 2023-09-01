//
// Created by woesss on 13.08.2023.
//

#ifndef MMAPI_PLAYER_LISTENER_H
#define MMAPI_PLAYER_LISTENER_H

#include <jni.h>

namespace mmapi {

    enum PlayerState {
        CLOSED = 0,
        UNREALIZED = 100,
        REALIZED = 200,
        PREFETCHED = 300,
        STARTED = 400,
    };

    enum PlayerListenerEvent {
        END_OF_MEDIA = 1,
        START = 2,
        ERROR = 3
    };

    class JNIEnvPtr {
        bool isJavaThread;
        JNIEnv *env;
    public:
        JNIEnvPtr();
        virtual ~JNIEnvPtr();
        JNIEnv *operator->() const;

        static JavaVM *vm;
    };

    class PlayerListener {
        jobject listener;
        jmethodID method;
    public:
        PlayerListener(JNIEnv *env, jobject pListener);
        virtual ~PlayerListener();

        void sendEvent(PlayerListenerEvent eventType, const int64_t time);
        void postEvent(PlayerListenerEvent type, int64_t time);

    };
} // mmapi

#endif //MMAPI_PLAYER_LISTENER_H
