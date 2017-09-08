package com.example.joy_l.face_app;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class EnrollActivity extends AppCompatActivity {

    private int width, height;
    private ImageView image;
    private Bitmap bitmap;
    byte[] bytes = PreviewActivity.getBytes();
    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        if(bytes != null){
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            bitmap = rotate(270, bitmap);
            Log.i("Bitmap", "image saved in bitmap");
        } else {
            Log.e("Null bytes", "image is null");
            return;
        }
        Log.i("width", String.valueOf(image.getWidth()));
        Log.i("height", String.valueOf(image.getHeight()));
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            bitmap = rotate(90, bitmap);
        }
        image.setImageBitmap(Bitmap.createScaledBitmap(bitmap, image.getWidth(), image.getHeight(), false));
    }


    private Bitmap rotate(int angle, Bitmap source){
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("ENROLL_BYTES", String.valueOf(bytes.length));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll);
        image = (ImageView) findViewById(R.id.captureDisplay);
    }

}
