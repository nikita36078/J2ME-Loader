//
// Created by woesss on 08.07.2023.
//

#include <jni.h>
#include "eas_player.h"
#include "util/jstring.h"
#include "eas_util.h"
#include "util/jbytearray.h"

/* for C++ linkage */
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_eas_LibEAS_loadSoundBank
(JNIEnv *env, jobject /*thiz*/, jstring sound_bank) {
    if (sound_bank == nullptr) {
        return;
    }

    util::JStringPtr sb(env, sound_bank);
    int32_t result = mmapi::eas::Player::initSoundBank(*sb);
    if (result != 0) {
        const char *message = mmapi::eas::EAS_GetErrorString(result);
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), message);
    }
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_synth_eas_LibEAS_createPlayer
(JNIEnv *env, jobject /*thiz*/, jstring pLocator) {
    mmapi::eas::Player *player;
    util::JStringPtr locator(env, pLocator);
    int32_t result = mmapi::eas::Player::createPlayer(*locator, &player);
    if (result != 0) {
        const char *message = mmapi::eas::EAS_GetErrorString(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
        return 0;
    }
    return reinterpret_cast<jlong>(player);
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_eas_LibEAS_finalize
(JNIEnv */*env*/, jobject /*thiz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    delete player;
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_eas_LibEAS_realize
(JNIEnv *env, jobject /*thiz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    if (!player->realize()) {
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), "Dummy message");
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_eas_LibEAS_prefetch
(JNIEnv *env, jobject /*thiz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    oboe::Result result = player->prefetch();
    if (result != oboe::Result::OK) {
        auto &&message = oboe::convertToText(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_eas_LibEAS_start
(JNIEnv *env, jobject /*thiz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    oboe::Result result = player->start();
    if (result != oboe::Result::OK) {
        auto message = oboe::convertToText(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_eas_LibEAS_pause
(JNIEnv *env, jobject /*thiz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    oboe::Result result = player->pause();
    if (result != oboe::Result::OK) {
        auto message = oboe::convertToText(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_eas_LibEAS_deallocate
(JNIEnv */*env*/, jobject /*thiz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    player->deallocate();
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_eas_LibEAS_close
(JNIEnv */*env*/, jobject /*thiz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    player->close();
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_synth_eas_LibEAS_setMediaTime
(JNIEnv */*env*/, jobject /*thiz*/, jlong handle, jlong now) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    return player->setMediaTime(now);
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_synth_eas_LibEAS_getMediaTime
(JNIEnv */*env*/, jobject /*thiz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    return player->getMediaTime();
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_eas_LibEAS_setRepeat
(JNIEnv */*env*/, jobject /*thiz*/, jlong handle, jint count) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    player->setRepeat(count);
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_eas_LibEAS_setPan
(JNIEnv */*env*/, jobject /*thiz*/, jlong handle, jint pan) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    player->setPan(pan);
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_eas_LibEAS_setVolume
(JNIEnv */*env*/, jobject /*thiz*/, jlong handle, jint level) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    player->setVolume(level);
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_synth_eas_LibEAS_getDuration
(JNIEnv */*env*/, jobject /*thiz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    return player->duration;
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_eas_LibEAS_setListener
(JNIEnv *env, jobject /*thiz*/, jlong handle, jobject listener) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    player->setListener(new mmapi::PlayerListener(env, listener));
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_synth_eas_LibEAS_setDataSource
(JNIEnv *env, jobject /*thiz*/, jlong handle, jbyteArray data) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    auto *file = new mmapi::eas::MemFile(env, data);
    int32_t result = player->setDataSource(file);
    if (result != 0) {
        delete file;
        const char *message = mmapi::eas::EAS_GetErrorString(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

JNIEXPORT jint JNICALL Java_ru_woesss_j2me_mmapi_synth_eas_LibEAS_writeMIDI
(JNIEnv *env, jobject /*thiz*/, jlong handle, jbyteArray data, jint offset, jint length) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    util::JByteArrayPtr ptr(env, data, offset, length);
    return player->writeMIDI(ptr);
}

#ifdef __cplusplus
} /* end extern "C" */
#endif
