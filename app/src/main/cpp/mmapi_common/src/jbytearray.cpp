//
// Created by woesss on 11.09.2023.
//

#include "jbytearray.h"

namespace util {

    JByteArrayPtr::JByteArrayPtr(JNIEnv *env, jbyteArray array, jint offset, jint count)
            : length(count) {
        jbyte *buf = new jbyte[count];
        env->GetByteArrayRegion(array, offset, count, buf);
        buffer = buf;
    }

    JByteArrayPtr::JByteArrayPtr(JNIEnv *env, jbyteArray array)
            : length(env->GetArrayLength(array)) {
        jbyte *buf = new jbyte[length];
        env->GetByteArrayRegion(array, 0, length, buf);
        buffer = buf;
    }

    JByteArrayPtr::~JByteArrayPtr() {
        delete[] buffer;
    }
}