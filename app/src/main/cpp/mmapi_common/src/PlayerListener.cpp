//
// Created by woesss on 13.08.2023.
//

#include <thread>
#include "PlayerListener.h"
#include "util/log.h"

#define LOG_TAG "MMAPI"

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    mmapi::JNIEnvPtr::vm = vm;
    return JNI_VERSION_1_6;
}

namespace mmapi {
    PlayerListener::PlayerListener(JNIEnv *env, jobject pListener)
            : listener(env->NewGlobalRef(pListener)),
              method(env->GetMethodID(env->GetObjectClass(listener), "postEvent", "(IJ)V")) {}

    PlayerListener::~PlayerListener() {
        JNIEnvPtr env;
        env->DeleteGlobalRef(listener);
    }

    void PlayerListener::sendEvent(EventType eventType, const int64_t time) {
        if (listener == nullptr || method == nullptr) {
            ALOGE("%s: obj=%p, mID=%p", __func__, listener, method);
            return;
        }
        JNIEnvPtr env;
        env->CallVoidMethod(listener, method, eventType, time);
    }

    void PlayerListener::postEvent(EventType type, int64_t time) {
        std::thread thread(&PlayerListener::sendEvent, this, type, time);
        thread.detach();
    }

    JavaVM *JNIEnvPtr::vm = nullptr;

    JNIEnvPtr::JNIEnvPtr() : env(nullptr) {
        jint res = vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
        if (res == JNI_OK) {
            isJavaThread = true;
            return;
        } else if (res != JNI_EDETACHED) {
            ALOGE("%s: JavaVM::GetEnv() returned %d", __func__, res);
            return;
        }
        res = vm->AttachCurrentThread(&env, nullptr);
        if (res == JNI_OK) {
            isJavaThread = false;
        } else {
            ALOGE("%s: JavaVM::AttachCurrentThread() returned %d", __func__, res);
        }
    }

    JNIEnvPtr::~JNIEnvPtr() {
        if (isJavaThread) {
            return;
        }
        jint res = vm->DetachCurrentThread();
        if (res != JNI_OK) {
            ALOGE("%s: JavaVM::DetachCurrentThread() returned %d", __func__, res);
        }
    }

    JNIEnv *JNIEnvPtr::operator->() const {
        return env;
    }
} // tsf_mmapi
