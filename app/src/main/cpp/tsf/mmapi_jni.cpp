//
// Created by woesss on 29.07.2023.
//

#include <jni.h>
#include "Player.h"
#include "PlayerListener.h"
#include "util/jstring.h"

/* for C++ linkage */
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    tsf_mmapi::JNIEnvPtr::vm = vm;
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_init
(JNIEnv *env, jclass /*clazz*/, jstring sound_bank) {
    if (sound_bank == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Sound bank file is null");
        return;
    }

    util::JStringPtr sb(env, sound_bank);
    if (!tsf_mmapi::Player::initSoundBank(*sb)) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Unsupported sound bank file");
    }
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_playerInit
(JNIEnv *env, jclass /*clazz*/, jstring locator) {
    tsf_mmapi::Player *player;
    util::JStringPtr path(env, locator);
    bool result = tsf_mmapi::Player::createPlayer(*path, &player);
    if (!result) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "Unsupported file type");
        return 0;
    }
    return reinterpret_cast<jlong>(player);
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_playerFinalize
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    delete player;
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_playerRealize
(JNIEnv *env, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    if (!player->realize()) {
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), "Dummy message");
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_playerPrefetch
(JNIEnv *env, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    oboe::Result result = player->prefetch();
    if (result != oboe::Result::OK) {
        auto &&message = oboe::convertToText(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_playerStart
(JNIEnv *env, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    oboe::Result result = player->start();
    if (result != oboe::Result::OK) {
        auto message = oboe::convertToText(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_playerPause
(JNIEnv *env, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    oboe::Result result = player->pause();
    if (result != oboe::Result::OK) {
        auto message = oboe::convertToText(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_playerDeallocate
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    player->deallocate();
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_playerClose
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    player->close();
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_setMediaTime
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle, jlong now) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    return player->setMediaTime(now);
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_getMediaTime
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    return player->getMediaTime();
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_setRepeat
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle, jint count) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    player->setRepeat(count);
}

JNIEXPORT jint JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_setPan
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle, jint pan) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    return player->setPan(pan);
}

JNIEXPORT jint JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_getPan
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    return player->getPan();
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_setMute
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle, jboolean mute) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    player->setMute(mute);
}

JNIEXPORT jboolean JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_isMuted
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    return player->isMuted();
}

JNIEXPORT jint JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_setVolume
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle, jint level) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    return player->setVolume(level);
}

JNIEXPORT jint JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_getVolume
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    return player->getVolume();
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_playerGetDuration
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    return player->duration;
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_playerListener
(JNIEnv *env, jclass /*clazz*/, jlong handle, jobject listener) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    player->setListener(new tsf_mmapi::PlayerListener(env, listener));
}

#ifdef __cplusplus
} /* end extern "C" */
#endif
