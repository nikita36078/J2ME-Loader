//
// Created by woesss on 08.07.2023.
//
#include <jni.h>

#include <cstddef>
#include "libsonivox/eas.h"
#include "mmapi_jstring.h"
#include "mmapi_file.h"
#include "mmapi_error.h"
#include "mmapi_player.h"

/* for C++ linkage */
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_initEAS(JNIEnv *env, jclass /*clazz*/) {
    EAS_DATA_HANDLE easHandle;
    EAS_RESULT result = EAS_Init(&easHandle);
    if (result != EAS_SUCCESS) {
        auto &&message = MMAPI_GetErrorString(result);
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), message);
        return result;
    }
    jclass mlc = env->FindClass("javax/microedition/shell/MicroLoader");
    jmethodID method = env->GetStaticMethodID(mlc, "getSoundfont", "()Ljava/lang/String;");
    jobject sb = env->CallStaticObjectMethod(mlc, method);
    if (sb != nullptr) {
        mmapi::JStringHolder soundbank(env, reinterpret_cast<jstring>(sb));
        mmapi::File file(soundbank.ptr, "rb");
        EAS_LoadDLSCollection(easHandle, nullptr, &file.easFile);
        EAS_GetGlobalDLSLib(easHandle, &mmapi::Player::easDlsHandle);
    }
    return reinterpret_cast<uintptr_t>(easHandle);
}

JNIEXPORT jboolean JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_checkFileType
(JNIEnv *env, jclass /*clazz*/, jlong eas_handle, jstring path) {
    auto easHandle = reinterpret_cast<EAS_DATA_HANDLE>(eas_handle);
    const mmapi::JStringHolder locator(env, path);
    mmapi::File file(locator.ptr, "rb");
    EAS_HANDLE stream = nullptr;
    jboolean result = EAS_OpenFile(easHandle, &file.easFile, &stream) == EAS_SUCCESS;
    if (result) {
        EAS_CloseFile(easHandle, stream);
    }
    return result;
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerInit
(JNIEnv *env, jclass /*clazz*/) {
    EAS_DATA_HANDLE easHandle;
    EAS_RESULT result = EAS_Init(&easHandle);
    if (result != EAS_SUCCESS) {
        auto &&message = MMAPI_GetErrorString(result);
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), message);
        return result;
    }
    auto *player = new mmapi::Player(easHandle);
    return reinterpret_cast<jlong>(player);
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerFinalize
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    delete player;
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerRealize
(JNIEnv *env, jclass /*clazz*/, jlong handle, jstring locator) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    mmapi::JStringHolder stringHolder(env, locator);
    EAS_RESULT result = player->setDataSource(stringHolder.ptr);
    if (result != EAS_SUCCESS) {
        auto &&message = MMAPI_GetErrorString(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerPrefetch
(JNIEnv *env, jclass /*clazz*/, jlong handle) {
    auto player = reinterpret_cast<mmapi::Player *>(handle);
    EAS_RESULT result = player->prefetch();
    if (result != EAS_SUCCESS) {
        auto &&message = MMAPI_GetErrorString(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
    return player->duration * 1000LL;
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerStart
(JNIEnv *env, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    EAS_RESULT result = player->start();
    if (result != EAS_SUCCESS) {
        auto &&message = MMAPI_GetErrorString(result);
        env->ThrowNew(env->FindClass("javax/microedition/media/MediaException"), message);
    }
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_playerPause
(JNIEnv *env, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    EAS_RESULT result = player->pause();
    if (result != EAS_SUCCESS) {
        auto &&message = MMAPI_GetErrorString(result);
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
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle, jlong new_time) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    jlong now = player->setMediaTime(new_time / 1000LL);
    if (now >= 0) {
        now *= 1000;
    }
    return now;
}

JNIEXPORT jlong JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_getMediaTime
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    jlong now = player->getMediaTime();
    if (now >= 0) {
        now *= 1000;
    }
    return now;
}

JNIEXPORT void JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_setRepeat
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle, jint count) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    player->setRepeat(count);
}

JNIEXPORT jint JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_setPan
(JNIEnv *env, jclass /*clazz*/, jlong handle, jint pan) {
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
    return player->setLevel(level);
}

JNIEXPORT jint JNICALL Java_ru_woesss_j2me_mmapi_sonivox_EAS_getVolume
(JNIEnv */*env*/, jclass /*clazz*/, jlong handle) {
    auto *player = reinterpret_cast<mmapi::Player *>(handle);
    return player->getLevel();
}

#ifdef __cplusplus
} /* end extern "C" */
#endif
