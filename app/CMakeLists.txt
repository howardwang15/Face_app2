cmake_minimum_required(VERSION 3.4.1)
include_directories(C:/howard/OpenCV-android-sdk/sdk/native/jni/include)
add_library( lib_opencv SHARED IMPORTED )
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libopencv_java3.so)

add_library(
    MyLibs SHARED src/main/jni/hello.cpp)

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )
target_link_libraries( # Specifies the target library.
         native-lib

         # OpenCV lib
         lib_opencv

         # Links the target library to the log library
         # included in the NDK.
         ${log-lib} )