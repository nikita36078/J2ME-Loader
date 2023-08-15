//
// Created by woesss on 13.08.2023.
//

#ifndef JL_MOD_PLAYERLISTENER_H
#define JL_MOD_PLAYERLISTENER_H

#include <jni.h>

namespace tsf_mmapi {

    enum EventType {
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

        void sendEvent(EventType eventType, const int64_t time);
        void postEvent(EventType type, int64_t time);

    };

} // tsf_mmapi

#endif //JL_MOD_PLAYERLISTENER_H
