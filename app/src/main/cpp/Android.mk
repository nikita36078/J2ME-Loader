LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CFLAGS    := -O3 -DM3G_TARGET_ANDROID -DM3G_GL_ES_1_1 #-DM3G_DEBUG
LOCAL_CXXFLAGS  := $(LOCAL_CFLAGS)
LOCAL_LDLIBS    := -llog -lEGL -lGLESv1_CM -lz -ljnigraphics
LOCAL_MODULE    := javam3g
LOCAL_C_INCLUDES := $(LOCAL_PATH)/inc/
LOCAL_SRC_FILES := \
	CSynchronization.cpp \
	m3g_android_java_api.cpp \
	src/m3g_core.c \
	src/m3g_android.cpp \
	src/m3g_android_gl.cpp

# Don't strip debug builds
ifeq ($(NDK_DEBUG),1)
    cmd-strip :=
endif

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_ARM_NEON := false
endif

include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)

LOCAL_CFLAGS    := -D_NO_DEBUG_PREPROCESSOR -DMMAPI_SUPPORT -DJET_INTERFACE -DEAS_WT_SYNTH \
    -D_SAMPLE_RATE_22050 \
    -DNUM_OUTPUT_CHANNELS=2 \
    -DMAX_SYNTH_VOICES=64 \
    -D_16_BIT_SAMPLES \
    -D_FILTER_ENABLED \
    -DDLS_SYNTHESIZER \
    -D_REVERB_ENABLED
LOCAL_CXXFLAGS  := $(LOCAL_CFLAGS) -Wconstant-conversion
LOCAL_LDLIBS    := -llog -lOpenSLES
LOCAL_MODULE    := mmapi
LOCAL_C_INCLUDES := $(LOCAL_PATH)/midi/mmapi/ \
    $(LOCAL_PATH)/midi/host/ \
    $(LOCAL_PATH)/midi/lib/
LOCAL_SRC_FILES := \
	midi/mmapi/eas_mmapi_jvm.cpp \
	midi/mmapi/eas_mmapi.c \
	midi/mmapi/eas_mmapi_host.c \
	midi/mmapi/eas_mmapi_wave.c \
	midi/mmapi/eas_mmapi_android.c \
	midi/host/eas_config.c \
	midi/lib/eas_dlssynth.c \
	midi/lib/eas_flog.c \
	midi/lib/eas_math.c \
	midi/lib/eas_mdls.c \
	midi/lib/eas_midi.c \
	midi/lib/eas_mixer.c \
	midi/lib/eas_pan.c \
	midi/lib/eas_pcm.c \
	midi/lib/eas_public.c \
	midi/lib/eas_reverb.c \
	midi/lib/eas_smf.c \
	midi/lib/eas_tonecontrol.c \
	midi/lib/eas_voicemgt.c \
	midi/lib/eas_wtengine.c \
	midi/lib/eas_wtsynth.c \
	midi/lib/jet.c \
	midi/lib/wt_22khz.c

# Don't strip debug builds
ifeq ($(NDK_DEBUG),1)
    cmd-strip :=
endif

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_ARM_NEON := false
endif

include $(BUILD_SHARED_LIBRARY)