LOCAL_PATH := $(call my-dir)
HOST_PATH := $(LOCAL_PATH)

include $(LOCAL_PATH)/dosbox-libretro/jni/Android.mk

LOCAL_PATH := $(HOST_PATH)
include $(CLEAR_VARS)
LOCAL_MODULE := dosboxplus
LOCAL_SRC_FILES := ../cpp/libretro_host.cpp
LOCAL_C_INCLUDES := $(LOCAL_PATH)/dosbox-libretro/libretro
LOCAL_SHARED_LIBRARIES := retro
LOCAL_LDLIBS := -llog
LOCAL_CPPFLAGS := -std=c++17 -fexceptions
include $(BUILD_SHARED_LIBRARY)
