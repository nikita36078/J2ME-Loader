#
# Auto-generated sample makefile
#
# This makefile is intended for use with GNU make.
# Set the paths to the tools (CC, AR, LD, etc.)
#

vpath %.c host_src

CC = C:\Program Files\GNUARM\bin\arm-elf-gcc.exe
AS = C:\Program Files\GNUARM\bin\arm-elf-as.exe
LD = C:\Program Files\GNUARM\bin\arm-elf-gcc.exe
AR = C:\Program Files\GNUARM\bin\arm-elf-ar.exe

%.o: %.c
	$(CC) -c -O2 -o $@ -I host_src -D UNIFIED_DEBUG_MESSAGES -D EAS_WT_SYNTH -D _IMELODY_PARSER -D _RTTTL_PARSER -D _OTA_PARSER -D _XMF_PARSER -D _WAVE_PARSER -D _REVERB_ENABLED -D _CHORUS_ENABLED -D MMAPI_SUPPORT -D JET_INTERFACE $<

%.o: %.s
	$(AS) -o $@ -EL -mcpu=arm946e-s -mfpu=softfpa -I lib_src --defsym CHECK_STACK=0 --defsym REVERB=0 --defsym CHORUS=0 --defsym STEREO_OUTPUT=1 --defsym SAMPLE_RATE_22050=1 --defsym SAMPLES_8_BIT=1 --defsym FILTER_ENABLED=1 $<

OBJS = eas_main.o eas_report.o eas_wave.o eas_hostmm.o eas_config.o

arm-wt-22k: $(OBJS)
	$(LD) -o $@ $(OBJS) libarm-wt-22k.a -lm

