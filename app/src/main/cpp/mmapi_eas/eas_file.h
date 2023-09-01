//
// Created by woesss on 30.06.2023.
//

#ifndef MMAPI_EAS_FILE_H
#define MMAPI_EAS_FILE_H

#include <stdio.h>
#include "libsonivox/eas_types.h"

namespace mmapi {
    namespace eas {

        class FileImpl {

        public:
            FileImpl(const char *path, const char *const mode);
            virtual ~FileImpl();
            static int readAt(void *handle, void *buf, int offset, int size);
            static int size(void *handle);

            EAS_FILE easFile{this, readAt, size};

        private:
            FILE *file;
            size_t length = 0;
        }; // class FileImpl
    } // namespace eas
} // namespace mmapi

#endif //MMAPI_EAS_FILE_H
