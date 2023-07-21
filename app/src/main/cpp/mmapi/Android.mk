LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES = \
	mmapi_jni.cpp \
	mmapi_player.cpp \
	mmapi_util.cpp \
	mmapi_file.cpp \
	mmapi_jstring.cpp
#	eas_mmapi.c \
	eas_mmapi_host.c \
	eas_mmapi_jni.c \
	eas_mmapi_wave.c \
	eas_mmapi_android.c \
    eas_wavein_oboe.cpp \
    eas_waveout_oboe.cpp \


LOCAL_CFLAGS += -O2 \

LOCAL_C_INCLUDES := $(LOCAL_PATH) \

LOCAL_LDLIBS := -llog
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384

LOCAL_ARM_MODE := arm

LOCAL_MODULE := mmapi

LOCAL_SHARED_LIBRARIES := oboe sonivox

# Don't strip debug builds
ifeq ($(NDK_DEBUG),1)
    cmd-strip :=
endif

include $(BUILD_SHARED_LIBRARY)

$(call import-module,prefab/oboe)