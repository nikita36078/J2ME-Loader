//
// Created by woesss on 11.09.2023.
//
#include <jni.h>

#ifndef MMAPI_JBYTE_ARRAY_H
#define MMAPI_JBYTE_ARRAY_H

namespace util {
    class JByteArrayPtr {
    public:
        JByteArrayPtr(JNIEnv *env, jbyteArray array, jint offset, jint count);
        JByteArrayPtr(JNIEnv *env, jbyteArray array);
        JByteArrayPtr(const JByteArrayPtr &) = delete;

        virtual ~JByteArrayPtr();

        const jbyte *buffer;
        const jsize length;
    };
}

#endif //MMAPI_JBYTE_ARRAY_H
