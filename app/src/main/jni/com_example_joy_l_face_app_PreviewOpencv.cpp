#include <iostream>
#include "com_example_joy_1l_face_1app_PreviewOpencv.h"

using namespace std;

JNIEXPORT void JNICALL Java_com_example_joy_1l_face_1app_PreviewOpencv_convert(JNIEnv *env, jclass, jbyteArray array){
    jbyte* buffer = (*env)->GetByteArrayElements(env, array, 0);

}

