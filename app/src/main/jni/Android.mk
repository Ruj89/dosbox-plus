LOCAL_PATH := $(call my-dir)
HOST_PATH := $(LOCAL_PATH)

include $(LOCAL_PATH)/dosbox-libretro/jni/Android.mk

LOCAL_PATH := $(HOST_PATH)
include $(CLEAR_VARS)
LOCAL_MODULE := dosboxplus
LOCAL_SRC_FILES := ../cpp/libretro_host.cpp
LOCAL_C_INCLUDES := $(LOCAL_PATH)/dosbox-libretro/libretro
# Both shared libraries are built, but the host opens/closes retro per session.
# A DT_NEEDED dependency here would prevent the core globals from being reset.
LOCAL_LDLIBS := -llog -ldl
LOCAL_CPPFLAGS := -std=c++17 -fexceptions
include $(BUILD_SHARED_LIBRARY)
