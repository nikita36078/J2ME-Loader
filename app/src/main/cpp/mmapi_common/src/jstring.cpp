//
// Created by woesss on 09.07.2023.
//

#include "jstring.h"

namespace util {

    JStringPtr::JStringPtr(JNIEnv *env, jstring ref) : env(env), ref(ref) {
        ptr = ref ? env->GetStringUTFChars(ref, nullptr) : nullptr;
    }

    JStringPtr::~JStringPtr() {
        if (env != nullptr && ref != nullptr && ptr != nullptr) {
            env->ReleaseStringUTFChars(ref, ptr);
        }
    }

    const char *JStringPtr::operator*() const {
        return ptr;
    }
} // namespace util
