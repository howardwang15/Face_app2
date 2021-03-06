/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
#include <opencv2/opencv.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>


using namespace std;
using namespace cv;

#ifndef _Included_com_example_joy_l_face_app_OpencvNativeClass
#define _Included_com_example_joy_l_face_app_OpencvNativeClass
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_example_joy_1l_face_1app_OpencvNativeClass_faceDetection(JNIEnv *, jclass, jlong input, jboolean touched, jint x, jint y);

JNIEXPORT void JNICALL Java_com_example_joy_1l_face_1app_OpencvNativeClass_faceDetectionMain(JNIEnv *, jclass, jlong input);

JNIEXPORT int JNICALL Java_com_example_joy_1l_face_1app_OpencvNativeClass_getNumEyes(JNIEnv *, jclass, jlong frame);


JNIEXPORT jboolean JNICALL Java_com_example_joy_1l_face_1app_OpencvNativeClass_locationIsValid(JNIEnv *, jclass, jint x, jint y);

JNIEXPORT void JNICALL Java_com_example_joy_1l_face_1app_OpencvNativeClass_normalize(JNIEnv *, jclass, jlong input, jlong normalized);


#ifdef __cplusplus
}
#endif
#endif
