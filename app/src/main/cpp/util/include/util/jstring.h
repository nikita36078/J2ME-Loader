#include <jni.h>

#ifndef JL_MOD_UTIL_H
#define JL_MOD_UTIL_H

namespace util {
    class JStringHolder {
    public:
        JStringHolder(JNIEnv *env, jstring ref);
        ~JStringHolder();

        const char *ptr;

    private:
        JNIEnv *env;
        jstring ref;
    };
}

#endif //JL_MOD_UTIL_H
