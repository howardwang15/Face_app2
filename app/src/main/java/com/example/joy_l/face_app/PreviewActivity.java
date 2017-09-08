package com.example.joy_l.face_app;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PreviewActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION_CODE = 0;

    private static final int STATE_PREVIEW = 0, STATE_CAPTURE_IMAGE = 1;
    private int currentCaptureState = STATE_PREVIEW;
    private String cameraID;
    private Size previewSize, imageSize;
    private ImageButton captureImageButton;

    private ImageView view;

    private static byte[] bytes = null;
    private ByteBuffer buffer = null;

    private TextureView textureView;
    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Toast.makeText(getApplicationContext(), "Preview is now available", Toast.LENGTH_SHORT).show();
            initCam(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            rotateImage(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };


    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            initCam(textureView.getWidth(), textureView.getHeight());
            connectCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    protected void onPause() {
        super.onPause();
        if(cameraDevice != null) {
            cameraDevice.close();
        }
        stopBackgroundThread();
    }
    private CameraDevice cameraDevice;
    private CameraDevice.StateCallback deviceCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.i("OPENED", "cameradevice has been opened");
            cameraDevice = camera;
            //Toast.makeText(getApplicationContext(), "Camera successfully opened!", Toast.LENGTH_SHORT).show();
            createPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.i("DISCONECTED", "cameradevice has been disconnected");
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            if(cameraDevice!=null) {
                cameraDevice.close();
                cameraDevice = null;
            }
        }
    };
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private ImageReader imageReader;
    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            buffer = image.getPlanes()[0].getBuffer();
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            Log.i("BSIZE", String.valueOf(bytes.length));
            Log.i("BSAVE", "image saved as bytes");
            Intent intent = new Intent(PreviewActivity.this, EnrollActivity.class);
            startActivity(intent);

            //Log.i("IMAGE_name", imageFile.toString());
            //backgroundHandler.post(new ImageSaver(reader.acquireLatestImage()));
        }
    };

    private CameraCaptureSession captureSession;
    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback(){
        public void process(CaptureResult result){
            if(currentCaptureState == STATE_PREVIEW){
                Log.i("CURSTATE", "preview state");
            } else if(currentCaptureState == STATE_CAPTURE_IMAGE){
                Log.i("CURSTATE", "capture state");
                Integer autoFocusState = result.get(CaptureResult.CONTROL_AF_STATE);
                if(autoFocusState == null){
                    Log.i("AUTO FOCUS", "AUTO FOCUS IS NULL");
                    startStillCaptureRequest();
                } else if(autoFocusState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || autoFocusState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                    Toast.makeText(getApplicationContext(), "Focus is Locked!", Toast.LENGTH_SHORT).show();
                    startStillCaptureRequest();
                } else {
                    Toast.makeText(getApplicationContext(), "Focus overriden!", Toast.LENGTH_SHORT).show();
                    startStillCaptureRequest();
                }
                currentCaptureState = STATE_PREVIEW;
            }
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            process(result);

        }
    };

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private static int deviceRotation(CameraCharacteristics characteristics, int deviceOrientation){
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private static class CompareSizesByArea implements Comparator<Size>{
        @Override
        public int compare(Size o1, Size o2) {
            return Long.signum((long) o1.getWidth() * o1.getHeight() / (long) o2.getWidth() * o2.getHeight());
        }
    }

    private Size getOptimalSize(Size[] options, int texturewidth, int textureheight){
        ArrayList<Size> biggerThanPreview = new ArrayList<>();
        for(Size choice: options){
            if(choice.getHeight() == choice.getWidth() * textureheight/texturewidth &&
                    choice.getWidth() >= texturewidth && choice.getHeight() >= textureheight) {
                biggerThanPreview.add(choice);
            }
        }
        if(biggerThanPreview.size() > 0){
            return Collections.min(biggerThanPreview, new CompareSizesByArea());
        } else {
            Log.e("PREVIEW_SIZE", "no suitable size found");
            return options[0];
        }
    }

    private void rotateImage(int width, int height){
        Matrix matrix = new Matrix();
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        RectF textureRect = new RectF(0, 0, width, height);
        RectF previewRect = new RectF(0, 0, previewSize.getWidth(), previewSize.getHeight());
        double centerX = textureRect.centerX();
        double centerY = textureRect.centerY();
        if(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270){
            previewRect.offset((float)centerX - previewRect.centerX(), (float)centerY - previewRect.centerY());
            matrix.setRectToRect(textureRect, previewRect, Matrix.ScaleToFit.CENTER);
            double scale = Math.max(height/previewSize.getHeight(), width/previewSize.getWidth());
            matrix.postScale((float)scale, (float)scale, (float)centerX, (float)centerY);
            matrix.postRotate(90 * (rotation - 2), (float) centerX, (float) centerY);
        } else if(rotation == Surface.ROTATION_180){
            matrix.postRotate(180, (float)centerX, (float)centerY);
        }
        textureView.setTransform(matrix);
    }

    private void initCam(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraID : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraID);
                int directionFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (directionFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                    this.cameraID = cameraID;
                }
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                int totalRotation = deviceRotation(characteristics, deviceOrientation);
                int rotatedWidth = width;
                int rotatedHeight = height;
                boolean swapRotation = totalRotation == 90 || totalRotation == 270;
                Log.i("Rotation", String.valueOf(swapRotation));
                if (swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }
                previewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                Log.i("PREVIEW_SIZE_Width", String.valueOf(previewSize.getWidth()));
                Log.i("PREVIEW_SIZE_height", String.valueOf(previewSize.getHeight()));
                rotateImage(width, height);
                imageSize = getOptimalSize(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);
                imageReader = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(), ImageFormat.JPEG, 1);
                imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private HandlerThread backgroundHandlerThread;
    private Handler backgroundHandler;
    private void startBackgroundThread() {
        backgroundHandlerThread = new HandlerThread("CaptureImage");
        backgroundHandlerThread.start();
        backgroundHandler = new Handler(backgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundHandlerThread.quitSafely();
        try {
            backgroundHandlerThread.join();
            backgroundHandlerThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static final String CAM_TAG = "FAIL";
    private static final String CAM_ID = "camera id";

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(code, permissions, grantResults);
        if(code == REQUEST_CAMERA_PERMISSION_CODE){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(), "Application won't run with camera", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void connectCamera() {
        Log.e(CAM_ID, cameraID);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(cameraID, deviceCallback, backgroundHandler);
            } else {
                if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                    Toast.makeText(this, "Video requires access to camera!", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_CODE);
                Log.e(CAM_TAG, "No permission to open camera");
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CaptureRequest.Builder previewRequestBuilder;
    private Surface previewSurface;
    private void createPreview(){
        SurfaceTexture texture = textureView.getSurfaceTexture();
        assert texture != null;
        texture.setDefaultBufferSize(1280, 720);
        previewSurface = new Surface(texture);
        try {
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(previewSurface);
            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if(cameraDevice == null) {
                        Log.e("DEVICE", "Camera device is null");
                        return;
                    }
                    captureSession = session;
                    try {
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        session.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(getApplicationContext(), "Session unable to setup preview", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CaptureRequest.Builder captureBuilder;
    private void startStillCaptureRequest() {
        try {
            captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            Log.i("Capturing", "capturing image");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        CameraCaptureSession.CaptureCallback stillCaptureCallback = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                super.onCaptureStarted(session, request, timestamp, frameNumber);
                unlockFocus();
            }

            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                Toast.makeText(getApplicationContext(), "Saving time", Toast.LENGTH_SHORT).show();
                unlockFocus();
            }
        };
        try {
            captureSession.stopRepeating();
            captureSession.capture(captureBuilder.build(), stillCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void lockFocus(){
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus(){
        if(cameraDevice == null) {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
            try {
                captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
                captureSession.setRepeatingRequest(previewRequestBuilder.build(), captureCallback, backgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void finish(){
        super.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        TextView textView = (TextView) findViewById(R.id.enroll);
        textureView = (TextureView) findViewById(R.id.preview);
        ImageButton back = (ImageButton) findViewById(R.id.back_button);
        captureImageButton = (ImageButton) findViewById(R.id.imageButton);
        captureImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //lockFocus();
                if(cameraDevice != null) {
                    Log.i("BUTTON", "button clicked!");
                    startStillCaptureRequest();
                }
            }
        });


    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode == 0 && resultCode == RESULT_OK) {
            Log.i("DISPLAY", "display image");
            startActivity(intent);
        }
    }

    public void backHome(View view){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public static byte[] getBytes(){
        return bytes;
    }
}
