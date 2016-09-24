package com.pengcit.simplecamera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends Activity {
    private Camera mCamera = null;
    private CameraView mCameraView = null;
    Camera.PictureCallback rawCallback;
    Camera.ShutterCallback shutterCallback;
    Camera.PictureCallback jpegCallback;
    File rootsd;
    private final int REQUEST_CAMERA = 88;
    private String IMG_URI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootsd = Environment.getExternalStorageDirectory();

        try{
            mCamera = Camera.open();//you can use open(int) to use different cameras
        } catch (Exception e){
            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
        }

        if(mCamera != null) {
            mCameraView = new CameraView(this, mCamera);//create a SurfaceView to show camera data
            FrameLayout camera_view = (FrameLayout)findViewById(R.id.camera_view);
            camera_view.addView(mCameraView);//add the SurfaceView to the layout
        }

        //btn to close the application
        ImageButton imgClose = (ImageButton)findViewById(R.id.imgClose);
        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.exit(0);
            }
        });
        //green box target
        //Box box = new Box(this);
        //addContentView(box, new RadioGroup.LayoutParams(RadioGroup.LayoutParams.FILL_PARENT, RadioGroup.LayoutParams.FILL_PARENT));

        //code to capture image
        ImageButton capture = (ImageButton) findViewById(R.id.btnCapture);
        capture.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                captureImage();

            }
        });

        rawCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.d("Log", "onPictureTaken - raw");
            }
        };

        /** Handles data for jpeg picture */
        shutterCallback = new Camera.ShutterCallback() {
            public void onShutter() {
                Log.i("Log", "onShutter'd");
            }
        };
        jpegCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                FileOutputStream outStream = null;
                try {
                    IMG_URI = String.format(rootsd.getAbsolutePath() + "/DCIM/Camera/%d.jpg", System.currentTimeMillis());
                    Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                    if (bmp.getWidth() > bmp.getHeight()) {
                        bmp = rotateImage(bmp, 90);
                        Log.d("rotate", "Rotate 90");
                    }
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] flippedImageByteArray = stream.toByteArray();
                    outStream = new FileOutputStream(IMG_URI);
                    outStream.write(flippedImageByteArray);
                    outStream.close();
                    Log.d("Filename", IMG_URI);
                    Log.d("Log", "onPictureTaken - wrote bytes: " + data.length);
                    ExifInterface ei = new ExifInterface(IMG_URI);
                    int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                    Uri ImageUri = Uri.fromFile(new File(IMG_URI));
                    InputStream image_stream = getContentResolver().openInputStream(ImageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(image_stream);
                    switch(orientation) {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            rotateImage(bitmap, 90);
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            rotateImage(bitmap, 180);
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            rotateImage(bitmap, 270);
                            break;
                        default:
                            break;
                    }
                    changeActivity();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {

                    Log.d("Log", "onPictureTaken - jpeg");
                }

            }
        };
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Bitmap retVal;

        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        retVal = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);

        return retVal;
    }

    private void captureImage() {
        // TODO Auto-generated method stub
        mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);

    }

    private void changeActivity(){
        Intent intent = new Intent();
        intent.putExtra("imgURI", IMG_URI);
        setResult(RESULT_OK, intent);
        finish();
    }
}
