LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := mmapi_eas

LOCAL_SRC_FILES = \
	eas_file.cpp \
	eas_player.cpp \
	eas_player_jni.cpp \
	eas_util.cpp \


LOCAL_CFLAGS += -O2 \

LOCAL_C_INCLUDES := $(LOCAL_PATH) \

LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384

LOCAL_ARM_MODE := arm

LOCAL_SHARED_LIBRARIES := mmapi_common

LOCAL_STATIC_LIBRARIES := sonivox

# Don't strip debug builds
ifeq ($(NDK_DEBUG),1)
    cmd-strip :=
endif

include $(BUILD_SHARED_LIBRARY)

$(call import-module,prefab/oboe)