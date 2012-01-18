LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE 			:= asf
LOCAL_SRC_FILES 		:= asf-common.c asf-array-common.c asf-decoder.c
LOCAL_CFLAGS 			:= $(cflags_array_features) $(cflags_loglevels)
LOCAL_LDLIBS 			:= -llog
LOCAL_STATIC_LIBRARIES 	:= asfffmpeg ffmpeg
include $(BUILD_SHARED_LIBRARY)

