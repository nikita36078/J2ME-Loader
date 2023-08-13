#include <jni.h>

#ifndef JL_MOD_UTIL_H
#define JL_MOD_UTIL_H

namespace util {
    class JStringPtr {
        const char *ptr;
    public:
        JStringPtr(JNIEnv *env, jstring ref);
        ~JStringPtr();

        const char *operator*() const;

    private:
        JNIEnv *env;
        jstring ref;
    };
}

#endif //JL_MOD_UTIL_H
