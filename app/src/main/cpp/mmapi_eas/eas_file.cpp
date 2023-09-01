//
// Created by woesss on 09.07.2023.
//

#include "eas_file.h"

namespace mmapi {
    namespace eas {

        FileImpl::FileImpl(const char *path, const char *const mode) {
            file = fopen(path, mode);
            fseek(file, 0, SEEK_END);
            length = ftell(file);
            fseek(file, 0, SEEK_SET);
        }

        FileImpl::~FileImpl() {
            fclose(file);
        }

        int FileImpl::readAt(void *handle, void *buf, int offset, int size) {
            auto *fh = static_cast<FileImpl *>(handle);
            if (fseek(fh->file, offset, SEEK_SET) != 0) {
                return -1;
            }
            return fread(buf, 1, size, fh->file);
        }

        int FileImpl::size(void *handle) {
            auto *fh = static_cast<FileImpl *>(handle);
            return fh->length;
        }
    } // namespace eas
} // namespace mmapi
