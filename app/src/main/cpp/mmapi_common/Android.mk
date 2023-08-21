LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES = \
	src/PlayerListener.cpp \
	src/BasePlayer.cpp \
#	Player.cpp \
	mmapi_jni.cpp \

LOCAL_CFLAGS += -O2 \

LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384

LOCAL_C_INCLUDES := $(LOCAL_PATH)/include/mmapi \
	$(LOCAL_PATH)/include/mmapi/midi \

LOCAL_MODULE := mmapi_common

LOCAL_SHARED_LIBRARIES := oboe util

LOCAL_EXPORT_C_INCLUDES  :=	$(LOCAL_PATH)/include
LOCAL_EXPORT_CFLAGS := $(LOCAL_CFLAGS)

# Don't strip debug builds
ifeq ($(NDK_DEBUG),1)
    cmd-strip :=
endif

include $(BUILD_SHARED_LIBRARY)

$(call import-module,prefab/oboe)