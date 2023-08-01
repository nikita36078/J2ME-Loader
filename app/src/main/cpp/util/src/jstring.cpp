//
// Created by woesss on 09.07.2023.
//

#include "jstring.h"

util::JStringHolder::JStringHolder(JNIEnv *env, jstring ref) : env(env), ref(ref) {
    ptr = ref ? env->GetStringUTFChars(ref, nullptr) : nullptr;
}

util::JStringHolder::~JStringHolder() {
    if (env != nullptr && ref != nullptr && ptr != nullptr) {
        env->ReleaseStringUTFChars(ref, ptr);
    }
}
