//
// Created by woesss on 11.09.2023.
//

#include <jni.h>

#ifndef MMAPI_JSTRING_H
#define MMAPI_JSTRING_H

namespace util {
    class JStringPtr {
        JNIEnv *env;
        jstring ref;
        const char *ptr;

    public:
        JStringPtr(JNIEnv *env, jstring ref);
        ~JStringPtr();

        const char *operator*() const;
    };
}

#endif //MMAPI_JSTRING_H
