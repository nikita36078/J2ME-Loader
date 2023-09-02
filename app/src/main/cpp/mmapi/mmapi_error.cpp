//
// Created by woesss on 09.07.2023.
//

#include <cstdio>
#include "mmapi_error.h"
#include "libsonivox/eas_types.h"

const char *MMAPI_GetErrorString(int errorCode) {
    int idx = -errorCode;
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
