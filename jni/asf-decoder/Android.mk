LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

needs_mks = $(LOCAL_PATH)/asf.mk
needs_cpufeatures =

# Decoder: FFMPEG/WMA
#	needs_cpufeatures 		= cpufeatures
needs_mks 				+= $(LOCAL_PATH)/ffmpeg.mk

# Build components:
include $(needs_mks)


# Build cpufeatures if needed
ifneq ($(needs_cpufeatures),)
	include $(NDK_ROOT)/sources/cpufeatures/Android.mk
endif

