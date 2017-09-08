package com.example.joy_l.face_app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IdRes;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class EnrollmentActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    final Context context = this;
    private JavaCameraView cameraView;
    private int counter = 0;
    private Mat frame;
    Mat normed;
    int touch_x, touch_y;
    boolean isTouched = false, toggle = false;
    boolean yes = false;
    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status){
                case BaseLoaderCallback.SUCCESS:
                    cameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    Log.e("camera error", "couldn't connect to the camera!");
                    break;
            }
        }
    };

    static {
        System.loadLibrary("MyLibs");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTouched = true;
                touch_x = (int)(event.getX() * 0.75);
                touch_y = (int)(event.getY() * 0.75);
                if(OpencvNativeClass.locationIsValid(touch_x, touch_y)) {
                    toggle = true;
                } else {
                    toggle = false;
                }
                Log.i("counter", String.valueOf(counter));
                /*if(counter % 2 == 1) {
                    toggle = true;
                } else toggle = false;*/

                break;
            default:
                //isTouched = false;
                break;
        }
        return true;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enrollment);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        cameraView = (JavaCameraView) findViewById(R.id.camera);
        cameraView.setCameraIndex(1);
        cameraView.setVisibility(View.VISIBLE);
        cameraView.setCvCameraViewListener(this);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void onResume(){
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.i("MESSAGE", "opencv loaded succesfully!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.e("MESSAGE", "opencv unsuccesfully loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, baseLoaderCallback);
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(cameraView != null){
            cameraView.disableView();
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(cameraView != null){
            cameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        frame = new Mat(width, height, CvType.CV_8UC3);
        normed = new Mat(200, 200, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        frame.release();
    }

    public void showToast(final String toast)
    {
        runOnUiThread(new Runnable() {
            public void run()
            {
                Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void alert(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
                alertBuilder.setMessage("Are you sure you want " +
                        "to select this face for enrollment?").setPositiveButton("Yes", dialogListener).setNegativeButton("No", dialogListener).show();
            }
        });
    }
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        frame = inputFrame.rgba();
        boolean isValidLoc = true;

        OpencvNativeClass.faceDetection(frame.getNativeObjAddr(), toggle, touch_x, touch_y);
        if(isTouched) {
            Log.i("toggle is: ", String.valueOf(toggle));
            if (!toggle) {
                showToast("Please select a detected face!");
                isTouched = false;
            } else {
                //alert();
            }
                /*
                if(OpencvNativeClass.getNumEyes(frame.getNativeObjAddr()) == 2) {
                    Log.i("NUM_EYES", "number of eyes approved");
                    alert();
                } else {
                    showToast("invalid number of eyes detected");
                    isTouched = false;
                    toggle = false;
                }*/
                /*if (OpencvNativeClass.getNumEyes() != 2) {
                    showToast("Number of eyes detected not valid");
                    isTouched = false;
                } else {
                    alert();
                    if (yes) {

                        OpencvNativeClass.normalize(frame.getNativeObjAddr(), normed.getNativeObjAddr());
                        showToast("norming");
                    } else {
                        isTouched = false;
                    }
                }*/
            }


        //OpencvNativeClass.faceDetection(frame.getNativeObjAddr(), isTouched, touch_x, touch_y);

        return frame;
    }

    DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener(){


        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch(which){
                case DialogInterface.BUTTON_POSITIVE:
                    yes = true;
                    Log.i("normed", String.valueOf(normed.cols()) + ", " + String.valueOf(normed.rows()));

                    //OpencvNativeClass.normalize(frame.getNativeObjAddr(), normed.getNativeObjAddr());
                    Log.i("NORMALIZING", "normalizing");
                    showToast("normalizing");
                    isTouched = false;
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    showToast("ok then");
                    yes = false;
                    dialog.dismiss();
                    break;

            }
        }
    };


}
