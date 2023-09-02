//
// Created by woesss on 30.06.2023.
//

#ifndef JL_MOD_MMAPI_FILE_H
#define JL_MOD_MMAPI_FILE_H

#include <stdio.h>
#include "libsonivox/eas_types.h"

namespace mmapi {
    class File {

    public:
        File(const char *path, const char *const mode);
        virtual ~File();
        static int readAt(void *handle, void *buf, int offset, int size);
        static int size(void *handle);

        EAS_FILE easFile{this, readAt, size};

    private:
        FILE *file;
        size_t length = 0;
    };
}

#endif //JL_MOD_MMAPI_FILE_H
