LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES = \
	eas_mmapi.c \
	eas_mmapi_host.c \
	eas_mmapi_jni.c \
	eas_mmapi_wave.c \
	eas_mmapi_android.c \
    eas_wavein_oboe.cpp \
    eas_waveout_oboe.cpp \


LOCAL_CFLAGS += -O2 \
	 -D_XMF_PARSER \
	 -D_ENHANCER_ENABLED \
	 -D_WAVE_PARSER \
	 -DMMAPI_SUPPORT \
	 -D_CRT_SECURE_NO_DEPRECATE \
	 -D_CRT_NONSTDC_NO_DEPRECATE \
	 -D_NO_DEBUG_PREPROCESSOR \
	 -DDLS_SYNTHESIZER \
#	 -DSONIVOX_DEBUG \

LOCAL_C_INCLUDES := $(LOCAL_PATH) \

LOCAL_LDLIBS := -llog
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384

LOCAL_ARM_MODE := arm

LOCAL_MODULE := mmapi

LOCAL_STATIC_LIBRARIES := sonivox

LOCAL_SHARED_LIBRARIES := oboe

# Don't strip debug builds
ifeq ($(NDK_DEBUG),1)
    cmd-strip :=
endif

include $(BUILD_SHARED_LIBRARY)

$(call import-module,prefab/oboe)