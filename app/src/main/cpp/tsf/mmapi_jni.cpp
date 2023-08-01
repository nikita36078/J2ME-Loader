//
// Created by woesss on 29.07.2023.
//

#include <jni.h>
#include "Player.h"
#include "util/jstring.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_init
(JNIEnv *env, jclass /*clazz*/, jstring sound_bank) {
    if (sound_bank == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Unsupported sound bank file is null");
        return;
    }

    util::JStringHolder sb(env, sound_bank);
    if (!tsf_mmapi::Player::initSoundBank(sb.ptr)) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Unsupported sound bank file");
    }
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_playerInit
(JNIEnv *env, jclass /*clazz*/, jstring locator) {
    auto *player = new tsf_mmapi::Player();
    util::JStringHolder path(env, locator);
    if (player->init(path.ptr)) {
        return reinterpret_cast<jlong>(player);
    }
    delete player;
    return 0;
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_playerFinalize
        (JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    delete player;
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_playerRealize
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle, jstring /*locator*/) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    player->realize();
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_playerPrefetch
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    if (!player->prefetch()) {
        return -1LL;
    }
    return player->duration;
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_playerStart
(JNIEnv *env, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    if (!player->start()) {
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), "Native start failed");
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_playerPause
(JNIEnv *env, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    if (!player->pause()) {
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), "Native start failed");
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
    return player->setLevel(level);
}

JNIEXPORT jint JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_getVolume
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    return player->getLevel();
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_tsf_TinySoundFont_playerGetDuration
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<tsf_mmapi::Player *>(handle);
    return player->duration;
}

#ifdef __cplusplus
} /* end extern "C" */
#endif
