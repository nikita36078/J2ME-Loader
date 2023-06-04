#
# Auto-generated sample makefile
#
# This makefile is intended for use with GNU make.
# Set the paths to the tools (CC, AR, LD, etc.)
#

vpath %.c lib_src

CC = C:\Program Files\GNUARM\bin\arm-elf-gcc.exe
AS = C:\Program Files\GNUARM\bin\arm-elf-as.exe
LD = C:\Program Files\GNUARM\bin\arm-elf-gcc.exe
AR = C:\Program Files\GNUARM\bin\arm-elf-ar.exe

%.o: %.c
	$(CC) -c -O2 -o $@ -I lib_src -I host_src -D NUM_OUTPUT_CHANNELS=2 -D _SAMPLE_RATE_22050 -D MAX_SYNTH_VOICES=64 -D EAS_WT_SYNTH -D _8_BIT_SAMPLES -D _FILTER_ENABLED -D _IMELODY_PARSER -D _RTTTL_PARSER -D _OTA_PARSER -D _XMF_PARSER -D _WAVE_PARSER -D _REVERB_ENABLED -D _CHORUS_ENABLED -D DLS_SYNTHESIZER -D _IMA_DECODER -D MMAPI_SUPPORT -D NATIVE_EAS_KERNEL -D JET_INTERFACE $<

%.o: %.s
	$(AS) -o $@ -EL -mcpu=arm946e-s -mfpu=softfpa -I lib_src --defsym CHECK_STACK=0 --defsym REVERB=0 --defsym CHORUS=0 --defsym STEREO_OUTPUT=1 --defsym SAMPLE_RATE_22050=1 --defsym SAMPLES_8_BIT=1 --defsym FILTER_ENABLED=1 $<

OBJS = eas_mididata.o eas_pan.o eas_wavefiledata.o eas_imelody.o eas_xmfdata.o ARM-E_interpolate_noloop_gnu.o eas_chorusdata.o ARM-E_voice_gain_gnu.o eas_ota.o eas_reverbdata.o eas_rtttl.o eas_reverb.o jet.o eas_mdls.o eas_mixbuf.o eas_smf.o eas_tcdata.o eas_chorus.o eas_pcmdata.o eas_xmf.o eas_smfdata.o eas_math.o eas_tonecontrol.o eas_rtttldata.o eas_voicemgt.o eas_public.o eas_dlssynth.o ARM-E_interpolate_loop_gnu.o ARM-E_filter_gnu.o eas_midi.o eas_otadata.o eas_flog.o eas_wtengine.o eas_imaadpcm.o eas_wtsynth.o wt_22khz.o eas_pcm.o eas_mixer.o eas_wavefile.o eas_ima_tables.o eas_data.o ARM-E_mastergain_gnu.o eas_imelodydata.o

arm-wt-22k.a: $(OBJS)
	$(AR) rc lib$@ $(OBJS)

