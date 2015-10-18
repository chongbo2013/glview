LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := GLView-freetype
LOCAL_SRC_FILES := GLView-freetype.cpp

include $(BUILD_SHARED_LIBRARY)
