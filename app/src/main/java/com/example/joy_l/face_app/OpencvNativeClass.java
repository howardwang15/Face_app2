package com.example.joy_l.face_app;

/**
 * Created by joy_l on 8/28/2017.
 */

public class OpencvNativeClass {

    public native static void faceDetectionMain(long input);
    public native static void faceDetection(long input, boolean touched, int x, int y);
    public native static int getNumEyes(long frame);
    public native static boolean locationIsValid(int x, int y);
    public native static void normalize(long frame, long normalized);
}
