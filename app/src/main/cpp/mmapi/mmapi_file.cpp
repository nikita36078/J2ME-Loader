//
// Created by woesss on 09.07.2023.
//

#include "mmapi_file.h"

mmapi::File::File(const char *path, const char *const mode) {
    file = fopen(path, mode);
    fseek(file, 0, SEEK_END);
    length = ftell(file);
    fseek(file, 0, SEEK_SET);
}

mmapi::File::~File() {
    fclose(file);
}

int mmapi::File::readAt(void *handle, void *buf, int offset, int size) {
    auto *fh = static_cast<File *>(handle);
    if (fseek(fh->file, offset, SEEK_SET) != 0) {
        return -1;
    }
    return fread(buf, 1, size, fh->file);
}

int mmapi::File::size(void *handle) {
    auto *fh = static_cast<File *>(handle);
    return fh->length;
}
