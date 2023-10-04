//
// Created by woesss on 08.07.2023.
//

#include <jni.h>
#include "eas_player.h"
#include "util/jstring.h"
#include "eas_util.h"

/* for C++ linkage */
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_init
(JNIEnv *env, jclass /*clazz*/, jstring sound_bank) {
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

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerInit
(JNIEnv *env, jclass /*clazz*/, jstring pLocator) {
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

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerFinalize
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    delete player;
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerRealize
(JNIEnv *env, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    if (!player->realize()) {
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), "Dummy message");
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerPrefetch
(JNIEnv *env, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    oboe::Result result = player->prefetch();
    if (result != oboe::Result::OK) {
        auto &&message = oboe::convertToText(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerStart
(JNIEnv *env, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    oboe::Result result = player->start();
    if (result != oboe::Result::OK) {
        auto message = oboe::convertToText(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerPause
(JNIEnv *env, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    oboe::Result result = player->pause();
    if (result != oboe::Result::OK) {
        auto message = oboe::convertToText(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerDeallocate
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    player->deallocate();
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerClose
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    player->close();
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_setMediaTime
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle, jlong now) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    return player->setMediaTime(now);
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_getMediaTime
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    return player->getMediaTime();
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_setRepeat
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle, jint count) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    player->setRepeat(count);
}

JNIEXPORT jint JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_setPan
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle, jint pan) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    return player->setPan(pan);
}

JNIEXPORT jint JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_getPan
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    return player->getPan();
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_setMute
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle, jboolean mute) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    player->setMute(mute);
}

JNIEXPORT jboolean JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_isMuted
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    return player->isMuted();
}

JNIEXPORT jint JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_setVolume
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle, jint level) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    return player->setVolume(level);
}

JNIEXPORT jint JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_getVolume
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    return player->getVolume();
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerGetDuration
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    return player->duration;
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerListener
(JNIEnv *env, jclass /*clazz*/, jlong handle, jobject listener) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    player->setListener(new mmapi::PlayerListener(env, listener));
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerSetDataSource
(JNIEnv *env, jclass /*clazz*/, jlong handle, jbyteArray data) {
    auto *player = reinterpret_cast<mmapi::eas::Player *>(handle);
    auto *file = new mmapi::eas::MemFile(env, data);
    int32_t result = player->setDataSource(file);
    if (result != 0) {
        delete file;
        const char *message = mmapi::eas::EAS_GetErrorString(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

#ifdef __cplusplus
} /* end extern "C" */
#endif
