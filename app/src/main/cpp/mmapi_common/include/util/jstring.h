#include <jni.h>

#ifndef MMAPI_JSTRING_H
#define MMAPI_JSTRING_H

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

#endif //MMAPI_JSTRING_H
