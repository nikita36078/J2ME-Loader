//
// Created by woesss on 30.06.2023.
//

#ifndef MMAPI_EAS_FILE_H
#define MMAPI_EAS_FILE_H

#include <stdio.h>
#include <jni.h>
#include "libsonivox/eas_types.h"

namespace mmapi {
    namespace eas {
        class BaseFile {

            static int readAt(void *handle, void *buf, int offset, int size);
            static int size(void *handle);

        public:
            virtual ~BaseFile();

            EAS_FILE easFile{this, readAt, size};

        protected:
            virtual int readAt(void *buf, int offset, int size) = 0;
            size_t length = 0;
        };

        class IOFile : public BaseFile {
            FILE *file;

        public:
            IOFile(const char *path, const char *const mode);
            ~IOFile() override;

            int readAt(void *buf, int offset, int size) override;
        }; // class FileImpl

        class MemFile : public BaseFile {
            char *data;

        public:
            MemFile(JNIEnv *env, jbyteArray array);
            ~MemFile() override;

            int readAt(void *buf, int offset, int size) override;
        }; // class MemFileImpl
    } // namespace eas
} // namespace mmapi

#endif //MMAPI_EAS_FILE_H
