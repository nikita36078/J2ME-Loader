# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := sonivox

LOCAL_SRC_FILES = \
	lib_src/eas_chorus.c \
	lib_src/eas_chorusdata.c \
	lib_src/eas_data.c \
	lib_src/eas_dlssynth.c \
	lib_src/eas_flog.c \
	lib_src/eas_ima_tables.c \
	lib_src/eas_imaadpcm.c \
	lib_src/eas_imelody.c \
	lib_src/eas_imelodydata.c \
	lib_src/eas_math.c \
	lib_src/eas_mdls.c \
	lib_src/eas_midi.c \
	lib_src/eas_mididata.c \
	lib_src/eas_mixbuf.c \
	lib_src/eas_mixer.c \
	lib_src/eas_ota.c \
	lib_src/eas_otadata.c \
	lib_src/eas_pan.c \
	lib_src/eas_pcm.c \
	lib_src/eas_pcmdata.c \
	lib_src/eas_public.c \
	lib_src/eas_reverb.c \
	lib_src/eas_reverbdata.c \
	lib_src/eas_rtttl.c \
	lib_src/eas_rtttldata.c \
	lib_src/eas_smf.c \
	lib_src/eas_smfdata.c \
	lib_src/eas_tcdata.c \
	lib_src/eas_tonecontrol.c \
	lib_src/eas_voicemgt.c \
	lib_src/eas_wavefile.c \
	lib_src/eas_wavefiledata.c \
	lib_src/eas_wtengine.c \
	lib_src/eas_wtsynth.c \
	lib_src/eas_xmf.c \
	lib_src/eas_xmfdata.c \
	lib_src/jet.c \
	lib_src/wt_22khz.c \
	host_src/eas_config.c \
	host_src/eas_report.c \
	host_src/eas_wave.c \
	host_src/eas_hostmm.c

LOCAL_CFLAGS += \
	-O2 \
	-DUNIFIED_DEBUG_MESSAGES \
	-DEAS_WT_SYNTH \
	-D_IMELODY_PARSER \
	-D_RTTTL_PARSER \
	-D_OTA_PARSER \
	-D_XMF_PARSER \
	-DNUM_OUTPUT_CHANNELS=2 \
	-D_SAMPLE_RATE_22050 \
	-DMAX_SYNTH_VOICES=64 \
	-D_16_BIT_SAMPLES \
	-D_FILTER_ENABLED \
	-DDLS_SYNTHESIZER \
	-D_REVERB_ENABLED \
	-DFILE_HEADER_SEARCH \
	-D_CRT_SECURE_NO_DEPRECATE \
	-D_CRT_NONSTDC_NO_DEPRECATE \
	-DMMAPI_SUPPORT \
	-DJET_INTERFACE \
	-Dfalse=0 \
	-Wno-unused-parameter \
	-Werror \
	-D_CHORUS_ENABLED \
#	-D_WAVE_PARSER \
	-D_IMA_DECODER # (needed for IMA-ADPCM wave files)
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/host_src \
	$(LOCAL_PATH)/lib_src

LOCAL_ARM_MODE := arm

LOCAL_SHARED_LIBRARIES := mmapi_common

ifeq ($(TARGET_ARCH),arm)
LOCAL_SRC_FILES += \
	lib_src/ARM-E_filter_gnu.s \
	lib_src/ARM-E_mastergain_gnu.s \
# not used with -D_16_BIT_SAMPLES
#	lib_src/ARM-E_interpolate_loop_gnu.s \
	lib_src/ARM-E_interpolate_noloop_gnu.s \
	lib_src/ARM-E_voice_gain_gnu.s

LOCAL_ASFLAGS += \
	-xassembler-with-cpp \
	-DSAMPLE_RATE_22050=1 \
	-DSTEREO_OUTPUT=1 \
	-DFILTER_ENABLED=1 \
	-D SAMPLES_16_BIT=1

LOCAL_CFLAGS += -D NATIVE_EAS_KERNEL

LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/lib_src/ARM_synth_constants_gnu.inc
endif

# Don't strip debug builds
ifeq ($(NDK_DEBUG),1)
    cmd-strip :=
endif

LOCAL_EXPORT_C_INCLUDES  :=	$(LOCAL_PATH)/include
LOCAL_EXPORT_CFLAGS := $(LOCAL_CFLAGS)

include $(BUILD_STATIC_LIBRARY)
