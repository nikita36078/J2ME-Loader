//
// Created by woesss on 09.07.2023.
//

#include "mmapi_jstring.h"

mmapi::JStringHolder::JStringHolder(JNIEnv *env, jstring ref) : env(env), ref(ref) {
    ptr = ref ? env->GetStringUTFChars(ref, nullptr) : nullptr;
}

mmapi::JStringHolder::~JStringHolder() {
    if (env != nullptr && ref != nullptr && ptr != nullptr) {
        env->ReleaseStringUTFChars(ref, ptr);
    }
}
