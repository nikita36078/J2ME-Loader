//
// Created by woesss on 08.07.2023.
//

#include <jni.h>
#include <cstddef>
#include "libsonivox/eas.h"
#include "util/jstring.h"
#include "mmapi_file.h"
#include "mmapi_util.h"
#include "mmapi_player.h"
#include "util/log.h"

#define LOG_TAG "MMAPI_JNI"

/* for C++ linkage */
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    mmapi::JNIEnvPtr::vm = vm;
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_init
(JNIEnv *env, jclass /*clazz*/, jstring sound_bank) {
    if (sound_bank == nullptr) {
        return;
    }

    util::JStringPtr sb(env, sound_bank);
    EAS_RESULT result = mmapi::Player::initSoundBank(*sb);
    if (result != EAS_SUCCESS) {
        auto &&message = MMAPI_GetErrorString(result);
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), message);
    }
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerInit
(JNIEnv *env, jclass /*clazz*/, jstring locator) {
    mmapi::Player *player;
    util::JStringPtr path(env, locator);
    EAS_RESULT result = mmapi::Player::createPlayer(*path, &player);
    if (result != EAS_SUCCESS) {
        auto &&message = MMAPI_GetErrorString(result);
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), message);
        return 0;
    }
    return reinterpret_cast<jlong>(player);
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerFinalize
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    delete player;
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerRealize
(JNIEnv *env, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    if (!player->realize()) {
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), "Dummy message");
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerPrefetch
(JNIEnv *env, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    oboe::Result result = player->prefetch();
    if (result != oboe::Result::OK) {
        auto &&message = oboe::convertToText(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerStart
(JNIEnv *env, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    oboe::Result result = player->start();
    if (result != oboe::Result::OK) {
        auto message = oboe::convertToText(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerPause
(JNIEnv *env, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    oboe::Result result = player->pause();
    if (result != oboe::Result::OK) {
        auto message = oboe::convertToText(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerDeallocate
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    player->deallocate();
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerClose
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    player->close();
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_setMediaTime
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle, jlong now) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    return player->setMediaTime(now);
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_getMediaTime
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    return player->getMediaTime();
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_setRepeat
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle, jint count) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    player->setRepeat(count);
}

JNIEXPORT jint JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_setPan
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle, jint pan) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    return player->setPan(pan);
}

JNIEXPORT jint JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_getPan
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
        auto *player = reinterpret_cast<mmapi::Player *>(handle);
        return player->getPan();
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_setMute
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle, jboolean mute) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    player->setMute(mute);
}

JNIEXPORT jboolean JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_isMuted
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    return player->isMuted();
}

JNIEXPORT jint JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_setVolume
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle, jint level) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    return player->setVolume(level);
}

JNIEXPORT jint JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_getVolume
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    return player->getVolume();
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerGetDuration
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    return player->duration;
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerListener
        (JNIEnv *env, jclass /*clazz*/, jlong handle, jobject listener) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    player->setListener(new mmapi::PlayerListener(env, listener));
}

#ifdef __cplusplus
} /* end extern "C" */
#endif
