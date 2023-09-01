//
// Created by woesss on 09.07.2023.
//

#include <cstdio>
#include "eas_strings.h"
#include "libsonivox/eas_types.h"

const char *EAS_GetErrorString(int32_t errorCode) {
    int32_t idx = -errorCode;
    if (idx < sizeof(EAS_ERRORS) / sizeof(EAS_ERRORS[0])) {
        return EAS_ERRORS[idx];
    } else if (errorCode == EAS_EOF) {
        return "EAS_EOF";
    } else {
        static char str[4];
        sprintf(str, "%d", errorCode);
        return str;
    }
}

const char *EAS_GetFileTypeString(int32_t type) {
    if (type >= 0 && type < sizeof(EAS_ERRORS) / sizeof(EAS_ERRORS[0])) {
        return EAS_FILE_TYPES[type];
    }
    return EAS_FILE_TYPES[0];
}
