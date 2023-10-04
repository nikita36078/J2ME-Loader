LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := mmapi_common

LOCAL_SRC_FILES = \
	src/PlayerListener.cpp \
	src/BasePlayer.cpp \
	src/jstring.cpp \
	src/jbytearray.cpp \

LOCAL_CFLAGS += -O2 \

LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/include/mmapi \
	$(LOCAL_PATH)/include/util \

LOCAL_LDLIBS := -llog

LOCAL_SHARED_LIBRARIES := oboe

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_EXPORT_LDLIBS := $(LOCAL_LDLIBS)
LOCAL_EXPORT_SHARED_LIBRARIES := $(LOCAL_SHARED_LIBRARIES)

# Don't strip debug builds
ifeq ($(NDK_DEBUG),1)
    cmd-strip :=
endif

include $(BUILD_SHARED_LIBRARY)

$(call import-module,prefab/oboe)