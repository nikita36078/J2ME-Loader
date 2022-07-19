LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := micro3d
LOCAL_SRC_FILES := \
	src/utils.cpp  \

LOCAL_CPPFLAGS +=
LOCAL_LDLIBS := -llog -lGLESv2 -ljnigraphics
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384
LOCAL_STATIC_LIBRARIES :=
LOCAL_C_INCLUDES := $(LOCAL_PATH)/inc

# Don't strip debug builds
ifeq ($(NDK_DEBUG),1)
    cmd-strip :=
endif

include $(BUILD_SHARED_LIBRARY)
