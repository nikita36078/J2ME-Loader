//
// Created by woesss on 29.07.2023.
//

#include <jni.h>
#include "tsf_player.h"
#include "util/jstring.h"
#include "util/jbytearray.h"

/* for C++ linkage */
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_tsf_LibTSF_loadSoundBank
(JNIEnv *env, jobject /*thiz*/, jstring sound_bank) {
    if (sound_bank == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Sound bank file is null");
        return;
    }

    util::JStringPtr sb(env, sound_bank);
    int32_t result = mmapi::tiny::Player::initSoundBank(*sb);
    if (result != 0) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Unsupported sound bank file");
    }
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_synth_tsf_LibTSF_createPlayer
(JNIEnv *env, jobject /*thiz*/, jstring pLocator) {
    mmapi::tiny::Player *player;
    util::JStringPtr locator(env, pLocator);
    int32_t result = mmapi::tiny::Player::createPlayer(*locator, &player);
    if (result != 0) {
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), "Unsupported file type");
        return 0;
    }
    return reinterpret_cast<jlong>(player);
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_tsf_LibTSF_finalize
(JNIEnv */*env*/, jobject /*thiz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::tiny::Player *>(handle);
    delete player;
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_tsf_LibTSF_realize
(JNIEnv *env, jobject /*thiz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::tiny::Player *>(handle);
    if (!player->realize()) {
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), "Dummy message");
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_tsf_LibTSF_prefetch
(JNIEnv *env, jobject /*thiz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::tiny::Player *>(handle);
    oboe::Result result = player->prefetch();
    if (result != oboe::Result::OK) {
        auto &&message = oboe::convertToText(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_tsf_LibTSF_start
(JNIEnv *env, jobject /*thiz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::tiny::Player *>(handle);
    oboe::Result result = player->start();
    if (result != oboe::Result::OK) {
        auto message = oboe::convertToText(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_tsf_LibTSF_pause
(JNIEnv *env, jobject /*thiz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::tiny::Player *>(handle);
    oboe::Result result = player->pause();
    if (result != oboe::Result::OK) {
        auto message = oboe::convertToText(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_tsf_LibTSF_deallocate
(JNIEnv */*env*/, jobject /*thiz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::tiny::Player *>(handle);
    player->deallocate();
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_tsf_LibTSF_close
(JNIEnv */*env*/, jobject /*thiz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::tiny::Player *>(handle);
    player->close();
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_synth_tsf_LibTSF_setMediaTime
(JNIEnv */*env*/, jobject /*thiz*/, jlong handle, jlong now) {
    auto *player = reinterpret_cast<mmapi::tiny::Player *>(handle);
    return player->setMediaTime(now);
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_synth_tsf_LibTSF_getMediaTime
(JNIEnv */*env*/, jobject /*thiz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::tiny::Player *>(handle);
    return player->getMediaTime();
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_tsf_LibTSF_setRepeat
(JNIEnv */*env*/, jobject /*thiz*/, jlong handle, jint count) {
    auto *player = reinterpret_cast<mmapi::tiny::Player *>(handle);
    player->setRepeat(count);
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_tsf_LibTSF_setPan
(JNIEnv */*env*/, jobject /*thiz*/, jlong handle, jint pan) {
    auto *player = reinterpret_cast<mmapi::tiny::Player *>(handle);
    player->setPan(pan);
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_tsf_LibTSF_setVolume
(JNIEnv */*env*/, jobject /*thiz*/, jlong handle, jint level) {
    auto *player = reinterpret_cast<mmapi::tiny::Player *>(handle);
    player->setVolume(level);
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_synth_tsf_LibTSF_getDuration
(JNIEnv */*env*/, jobject /*thiz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::tiny::Player *>(handle);
    return player->duration;
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_tsf_LibTSF_setListener
(JNIEnv *env, jobject /*thiz*/, jlong handle, jobject listener) {
    auto *player = reinterpret_cast<mmapi::tiny::Player *>(handle);
    player->setListener(new mmapi::PlayerListener(env, listener));
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_tsf_LibTSF_setDataSource
(JNIEnv *env, jobject /*thiz*/, jlong handle, jbyteArray data) {
    auto *player = reinterpret_cast<mmapi::tiny::Player *>(handle);
    util::JByteArrayPtr ptr(env, data);
    int32_t result = player->setDataSource(&ptr);
    if (result != 0) {
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), "Unsupported file type");
    }
}

#ifdef __cplusplus
} /* end extern "C" */
#endif
