//
// Created by woesss on 09.07.2023.
//

#include <cstring>
#include "eas_file.h"

namespace mmapi {
    namespace eas {

        BaseFile::~BaseFile() {}

        int BaseFile::size(void *handle) {
            return static_cast<BaseFile *>(handle)->length;
        }

        int BaseFile::readAt(void *handle, void *buf, int offset, int size) {
            return static_cast<BaseFile *>(handle)->readAt(buf, offset, size);
        }

        IOFile::IOFile(const char *path, const char *const mode) {
            file = fopen(path, mode);
            fseek(file, 0, SEEK_END);
            length = ftell(file);
            fseek(file, 0, SEEK_SET);
        }

        IOFile::~IOFile() {
            fclose(file);
        }

        int IOFile::readAt(void *buf, int offset, int size) {
            if (fseek(file, offset, SEEK_SET) != 0) {
                return -1;
            }
            return fread(buf, 1, size, file);
        }

        MemFile::MemFile(JNIEnv *env, jbyteArray array) {
            length = env->GetArrayLength(array);
            char *buf = new char[length];
            env->GetByteArrayRegion(array, 0, length, reinterpret_cast<jbyte *>(buf));
            data = buf;
        }

        MemFile::~MemFile() {
            delete[] data;
        }

        int MemFile::readAt(void *buf, int offset, int size) {
            if (offset < 0 || offset >= length || size < 0) {
                return -1;
            }
            if (size > length - offset) {
                size = length - offset;
            }
            memcpy(buf, data + offset, size);
            return size;
        }
    } // namespace eas
} // namespace mmapi
