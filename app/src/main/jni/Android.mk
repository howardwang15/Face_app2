LOCAL_PATH := $(call my-dir)

CVROOT := C:/howard/OpenCV-android-sdk/sdk/native/jni
include $(CLEAR_VARS)
OPENCV_INSTALL_MODULES:=on
OPENCV_CAMERA_MODULES:=on
OPENCV_LIB_TYPE:=SHARED
include $(CVROOT)/OpenCV.mk
LOCAL_MODULE := MyLibs
LOCAL_C_INCLUDES := ./ jni/ C:/howard/OpenCV-android-sdk/sdk/native/jni/include
                   LOCAL_SRC_FILES := com_example_joy_l_face_app_OpencvNativeClass.cpp
LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)