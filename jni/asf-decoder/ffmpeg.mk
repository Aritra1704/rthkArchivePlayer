LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE 			:= asfffmpeg
LOCAL_SRC_FILES 		:= asf-ffmpeg-wma-decoder.c
LOCAL_C_INCLUDES 		:= $(LOCAL_PATH)/../ffmpeg $(NDK_ROOT)/sources/cpufeatures
LOCAL_CFLAGS 			:= $(cflags_loglevels)
include $(BUILD_STATIC_LIBRARY)

include $(LOCAL_PATH)/../ffmpeg/Android.mk

