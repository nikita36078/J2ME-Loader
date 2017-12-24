#!/bin/sh
ndk-build APP_BUILD_SCRIPT=./Android.mk NDK_PROJECT_PATH=. TARGET_ARCH_ABI=armeabi APP_PLATFORM=android-14 $@
