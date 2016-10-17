package com.pengcit.simplecamera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TestActivity extends AppCompatActivity{

    public static int MEDIA_TYPE_IMAGE = 1;
    private final int REQUEST_CAMERA = 100;
    private final int SELECT_FILE = 101;
    private final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 80;
    private SeekBar seekBarTop,seekBarLeft, seekBarRight;
    private float ArrayInt[][] = new float[3][3];
    Uri _selectedImageUri;
    public static Bitmap _bitmap, grayscaleBitmap;
    float maxElmt;
    int newValueLeft=100, newValueTop=100, newValueRight=100;
    int directNeighbour, currentPixelDiv, diagonalNeighbour;
    private Count<Object, Integer> c;
    ImageView imgIdentitas, imgHistogram;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_bar);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Button photobut = (Button)findViewById(R.id.photobutton);
        Button selectbut = (Button)findViewById(R.id.selectbutton);
        Button filterbut = (Button)findViewById(R.id.filterbutton);
        Button smoothbut = (Button)findViewById(R.id.smoothbutton);
        Button FaceRecogbut = (Button)findViewById(R.id.buttonFaceRecog);
        imgIdentitas = (ImageView)findViewById(R.id.imgFace);
        imgHistogram = (ImageView)findViewById(R.id.imgHistogram);
        seekBarTop = (SeekBar)findViewById(R.id.seekBarTop);
        seekBarLeft = (SeekBar)findViewById(R.id.seekBarLeft);
        seekBarRight = (SeekBar)findViewById(R.id.seekBarRight);

        photobut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCamera();
            }
        });
        selectbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectPhoto();
            }
        });

        filterbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FilterImage();
                imgIdentitas.setImageBitmap(grayscaleBitmap);
                drawHistogram(grayscaleBitmap);
            }
        });

        smoothbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SmoothImage();
            }
        });
        FaceRecogbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FaceRecog();
            }
        });
        seekBarTop.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Toast.makeText(getApplicationContext(),"SS",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(getApplicationContext(), String.valueOf(seekBar.getProgress()), Toast.LENGTH_SHORT).show();
                newValueTop = seekBar.getProgress();
                rerenderImage();
            }
        });

        seekBarLeft.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Toast.makeText(getApplicationContext(),"SS",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(getApplicationContext(), String.valueOf(seekBar.getProgress()), Toast.LENGTH_SHORT).show();
                newValueLeft  = seekBar.getProgress();
                rerenderImage();
            }
        });

        seekBarRight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Toast.makeText(getApplicationContext(),"SS",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(getApplicationContext(), String.valueOf(seekBar.getProgress()), Toast.LENGTH_SHORT).show();
                newValueRight = seekBar.getProgress();
                rerenderImage();
            }
        });
    }

    public void FaceRecog(){
        Intent intent = new Intent(this, FaceRecog.class);
        intent.putExtra("bitmap",grayscaleBitmap);
        TestActivity.this.startActivity(intent);
    }

    public void startCamera(){

        Intent intent = new Intent(this, MainActivity.class);
        //_selectedImageUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
        //intent.putExtra(MediaStore.EXTRA_OUTPUT, _selectedImageUri);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    public void selectPhoto(){
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(
                Intent.createChooser(intent, "Choose a Picture"), SELECT_FILE);
        RefreshGallery();
    }

    public void FilterImage(){
        Bitmap tempBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        int red,green,blue,alpha, totalRed = 0, totalGreen = 0, totalBlue = 0;

        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                int colour = tempBitmap.getPixel(i,j);
                red = Color.red(colour);
                green = Color.green(colour);
                blue = Color.blue(colour);
                //String msg = "R: "+ red + " G: " + green + " B: " + blue;
                //Log.d("Color",msg);
                int avgColor = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
                Color NewColour = new Color();
                int pixel = NewColour.rgb(avgColor, avgColor, avgColor);
                tempBitmap.setPixel(i, j, pixel);
            }
        }
        Log.d("Finish", "Process finished");
        grayscaleBitmap = tempBitmap;
    }

    public void drawHistogram(Bitmap histBitmap){
        Bitmap tempBitmap = histBitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        c = new Count<Object, Integer>();
        int red,green,blue,alpha, totalRed = 0, totalGreen = 0, totalBlue = 0;

        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                int colour = tempBitmap.getPixel(i,j);
                red = Color.red(colour);
                green = Color.green(colour);
                blue = Color.blue(colour);
                //String msg = "R: "+ red + " G: " + green + " B: " + blue;
                //Log.d("Color",msg);
                float avgColor = (0.299f * red + 0.587f * green + 0.114f * blue);
                c.add(avgColor);
            }
        }

        Bitmap hist = Bitmap.createBitmap(imgHistogram.getWidth(),imgHistogram.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(hist);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        maxElmt = 0;
        for (Map.Entry<Object, Integer> entry : c.entrySet()) {
            System.out.println(entry.getKey() + " - " + entry.getValue());
            if (entry.getValue() > maxElmt){
                maxElmt = entry.getValue();
            }
        }

        for(Map.Entry<Object, Integer> entry: c.entrySet()){
            float heightBin = (entry.getValue()/maxElmt * 200);
            float colorBin;
            colorBin = (float)entry.getKey();
            canvas.drawLine( colorBin, imgHistogram.getHeight(), colorBin, imgHistogram.getHeight() - heightBin, paint);//(0.0f, 0.0f, 100.0f,100.0f,paint); //

        }
        Log.d("Finish", "Histogram finished");
        imgHistogram.setImageBitmap(hist);
    }

    public void rerenderImage(){
        Bitmap tempBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        int red,green,blue,alpha, totalRed = 0, totalGreen = 0, totalBlue = 0;

        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                int colour = tempBitmap.getPixel(i,j);
                red = Color.red(colour);
                green = Color.green(colour);
                blue = Color.blue(colour);
                //String msg = "R: "+ red + " G: " + green + " B: " + blue;
                //Log.d("Color",msg);
                float avgColor = (float) (0.299f * red + 0.587f * green + 0.114f * blue);
                if (c.containsKey(avgColor)){
                    Color NewColour = new Color();
                    int newRed = (red * newValueTop/100);
                    int newGreen = (green * newValueTop/100);
                    int newBlue =  (blue * newValueTop/100);
                    int pixel = 0;
                    if (avgColor > 255/100 * newValueTop){
                        pixel = NewColour.rgb((int) (newRed * newValueRight/100),(int) (newGreen * newValueRight/100),(int) (newBlue * newValueRight/100));

                    } else {
                        pixel = NewColour.rgb((int) (newRed * newValueLeft/100),(int) (newGreen * newValueLeft/100),(int) (newBlue * newValueLeft/100));
                    }
                    tempBitmap.setPixel(i, j, pixel);
                }

            }
        }
        Log.d("Finish", "rerender finished");
        imgIdentitas.setImageBitmap(tempBitmap);
        drawHistogram(tempBitmap);
    }

    public void SmoothImage(){
        int totalBlur = 1;
        Bitmap tempBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        int[] pix = new int[ImgWidth * ImgHeight];
        int cellSize = (2*totalBlur) + 1;
        int counter, totalRed = 0, totalGreen = 0, totalBlue = 0;

        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                //Check for neighbour
                counter = 0;
                totalRed = 0; totalGreen = 0; totalBlue = 0;

                //Log.d("Pixel",String.valueOf(i) + "," + String.valueOf(j));
                for(int neighbourX = 0; neighbourX < cellSize; neighbourX++){
                    for(int neighbourY = 0; neighbourY < cellSize; neighbourY++){
                        //check for validity else fill with 0 and ignore
                        if (((i-totalBlur + neighbourX ) < 0 ) || ((j-totalBlur + neighbourY) < 0 ) || ((i+ neighbourX ) >= ImgWidth ) || ((j+ neighbourY ) >= ImgHeight ) ){
                            //not valid neighbour
                            //Log.d("Test","0");
                        } else {
                            //Log.d("test counter", String.valueOf(counter));
                            int pixelNeighbour = tempBitmap.getPixel(i - totalBlur + neighbourX,j - totalBlur + neighbourY);
                            totalRed = totalRed + Color.red(pixelNeighbour);
                            //Log.d("tempRed", String.valueOf( Color.red(pixelNeighbour)));
                            totalGreen = totalGreen + Color.green(pixelNeighbour);
                            //Log.d("tempG", String.valueOf( Color.green(pixelNeighbour)));
                            totalBlue = totalBlue + Color.blue(pixelNeighbour);
                            //Log.d("tempB", String.valueOf( Color.blue(pixelNeighbour)));
                            counter = counter + 1;
                        }
                    }
                }
                //Log.d("TotalColor", "R:" + String.valueOf(totalRed) +",G:" + String.valueOf(totalGreen) +",B:" + String.valueOf(totalBlue) );

                int avgRed = (int) (totalRed / counter );
                int avgGreen = (int) (totalGreen / counter);
                int avgBlue = (int) (totalBlue / counter);
                //Log.d("NewColor", "R:" + String.valueOf(avgRed) +",G:" + String.valueOf(avgGreen) +",B:" + String.valueOf(avgBlue) );
                Color NewColour = new Color();
                int pixel = NewColour.rgb(avgRed, avgGreen, avgBlue);
                tempBitmap.setPixel(i, j, pixel);
            }
        }
        Log.d("Finish", "Smoothing finished");
        imgIdentitas.setImageBitmap(tempBitmap);
        drawHistogram(tempBitmap);
    }

    private void readFile(String filename){
        //Find the directory for the SD Card using the API
        //*Don't* hardcode "/sdcard"
        File sdcard = Environment.getExternalStorageDirectory();
        //Get the text file
        File file = new File(sdcard,filename);
        Log.d("tio",file.toString());
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED){
            // Should we show an explanation?
            Log.d("tio","Check if permission not yet given");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Log.d("tio","Permission need any explanation given");
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.
                Log.d("tio","Permission not need any explanation given");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

        } else {

            Log.d("tio","Permission has been given");
        }

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
            }
            br.close();
        }

        catch (IOException e) {
            //You'll need to add proper error handling here
            Log.d("ERROR",e.toString());
        }

        Log.d("text", text.toString());
        String[] listText = text.toString().split(",");
        diagonalNeighbour = Integer.valueOf(listText[0]);
        directNeighbour = Integer.valueOf(listText[1]);
        currentPixelDiv = Integer.valueOf(listText[2]);
    }

    private void readMatrix(String filename){
        //Find the directory for the SD Card using the API
        //*Don't* hardcode "/sdcard"
        File sdcard = Environment.getExternalStorageDirectory();
        //Get the text file
        File file = new File(sdcard,filename);
        Log.d("tio",file.toString());
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED){
            // Should we show an explanation?
            Log.d("tio","Check if permission not yet given");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Log.d("tio","Permission need any explanation given");
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.
                Log.d("tio","Permission not need any explanation given");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

        } else {

            Log.d("tio","Permission has been given");
        }

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
            }
            br.close();
        }

        catch (IOException e) {
            //You'll need to add proper error handling here
            Log.d("ERROR",e.toString());
        }

        Log.d("text", text.toString());
        String[] listText = text.toString().split(",");
        fillMatrix(listText);
    }

    public void fillMatrix(String[] list){
        for (int i = 0; i < list.length; i++){
            ArrayInt[i / 3 ][i % 3] = Float.valueOf(list[i]);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Toast.makeText(getApplicationContext(), "Permission read granted", Toast.LENGTH_SHORT).show();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getApplicationContext(), "Permission read not granted", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void RefreshGallery(){
        //Try to refresh gallery
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File("file://"+ Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            sendBroadcast(mediaScanIntent);
        }
        else
        {
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        }
    }

    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    private static File getOutputMediaFile(int type) {

        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Pengcit");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MediaStorage","Oops! Failed create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == this.RESULT_OK && data != null) {
            Log.d("Status","result ok" );
            if (requestCode == REQUEST_CAMERA) {
                Log.d("Status","request camera" );
                previewCapturedImage();
                InputStream image_stream = null;
                try {
                    _selectedImageUri = Uri.fromFile(new File(data.getStringExtra("imgURI")));
                    Log.d("URI",_selectedImageUri.toString() );
                    image_stream = getContentResolver().openInputStream(_selectedImageUri);
                    _bitmap = BitmapFactory.decodeStream(image_stream);

                    Bitmap _processed = _bitmap;
                    FilterImage();

                    int nh = (int) (_bitmap.getHeight() * (1024.0 / _processed.getWidth()));
//                    Bitmap scaled = Bitmap.createScaledBitmap(_bitmap, 1024, nh, true);
                    Bitmap scaled = Bitmap.createScaledBitmap(_processed, 1024, nh, true);

//                    imgIdentitas.setImageBitmap(Utils.decodeSampledBitmapFromResource(outputURI.getPath(), 300, 300));
                    imgIdentitas.setImageBitmap(scaled);


                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                drawHistogram(_bitmap);

            } else if (requestCode == SELECT_FILE) {
                _selectedImageUri = data.getData();
                InputStream image_stream = null;
                try {
                    image_stream = getContentResolver().openInputStream(_selectedImageUri);
                    _bitmap = BitmapFactory.decodeStream(image_stream);

                    Bitmap _processed = _bitmap;
                    FilterImage();

                    int nh = (int) (_bitmap.getHeight() * (1024.0 / _processed.getWidth()));
                    Bitmap scaled = Bitmap.createScaledBitmap(_processed, 1024, nh, true);

//                    imgIdentitas.setImageBitmap(Utils.decodeSampledBitmapFromResource(outputURI.getPath(), 300, 300));
                    imgIdentitas.setImageBitmap(scaled);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                drawHistogram(_bitmap);
            }
        } else {
            Log.d("Data Error", "data is null");
        }
    }

    private void previewCapturedImage() {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8;
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_refresh:
                Toast.makeText(this, "Refresh selected", Toast.LENGTH_SHORT)
                        .show();
                imgIdentitas.setImageBitmap(_bitmap);
                rerenderImage();
                drawHistogram(_bitmap);
                seekBarLeft.setProgress(100);
                seekBarTop.setProgress(100);
                seekBarLeft.setProgress(100);
                break;
            // action with ID action_settings was selected
            case R.id.action_settings:
                Toast.makeText(this, "Settings selected", Toast.LENGTH_SHORT)
                        .show();
                break;

            // action with ID action_settings was selected
            case R.id.filter:
                Toast.makeText(this, "Filter selected", Toast.LENGTH_SHORT)
                        .show();
                break;
            // action with ID action_settings was selected
            case R.id.borderNeighbour:
                Toast.makeText(this, "Border Neighbour selected", Toast.LENGTH_SHORT)
                        .show();
                borderNeighbour();
                break;
            case R.id.borderCenter:
                Toast.makeText(this, "Border Center selected", Toast.LENGTH_SHORT)
                        .show();
                borderCenter();
                break;
            // action with ID action_settings was selected
            case R.id.sharpen:
                Toast.makeText(this, "Sharpen selected", Toast.LENGTH_SHORT)
                        .show();
                readFile("sharpen.txt");
                sharpenImage();
                break;
            // action with ID action_settings was selected
            case R.id.blur:
                Toast.makeText(this, "Blur selected", Toast.LENGTH_SHORT)
                        .show();
                readFile("blur.txt");
                blurImage();
                break;
            // action with ID action_settings was selected
            case R.id.level1:
                Toast.makeText(this, "Level 1 selected", Toast.LENGTH_SHORT)
                        .show();
                break;
            // action with ID action_settings was selected
            case R.id.other:
                Toast.makeText(this, "Menu not yet created", Toast.LENGTH_SHORT)
                        .show();
                break;
            default:
                break;
        }

        return true;
    }

    public void borderNeighbour(){
        int totalBlur = 1;
        Bitmap tempBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        int cellSize = (2*totalBlur) + 1;

        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                //Check for neighbour
                int Red , Green , Blue , Grayscale,MaxDiffGrayscale = 0;


                //Do it for the first row minus third row of the 3x3 matrix
                for(int neighbourX = 0; neighbourX < cellSize; neighbourX++){
                        //check for validity else fill with the extended from the other side for X side
                        int coordX, coordY, coord2Y;
                        if (((i-totalBlur + neighbourX ) < 0 )) {
                            coordX = ImgWidth - 1;
                        } else if ((i+ neighbourX ) >= ImgWidth ){
                            coordX = 0;
                        } else {
                            coordX = i - totalBlur + neighbourX;
                        }
                        //check for validity else fill with the extended from the other side for Y side
                        if((j-totalBlur ) < 0 ){
                            coordY = ImgHeight - 1;
                        } else if ((j) >= ImgHeight ){
                            coordY = 0;
                        } else {
                            coordY = j-totalBlur;
                        }
                        coord2Y = coordY + 1;
                        if(coord2Y >= ImgHeight){
                            coord2Y = 0;
                        }
                        //count the diff
                        int CurrPix = tempBitmap.getPixel(coordX,coord2Y);
                        Red = Color.red(CurrPix);
                        Green = Color.green(CurrPix);
                        Blue = Color.blue(CurrPix);
                        Grayscale = (int) (0.299 * Red + 0.587 * Green + 0.114 * Blue);
                        //Log.d("test counter", String.valueOf(counter));
                        int pixelNeighbour = tempBitmap.getPixel(coordX, coordY);
                        int diffRed = Color.red(pixelNeighbour);
                        //Log.d("tempRed", String.valueOf( Color.red(pixelNeighbour)));
                        int diffGreen = Color.green(pixelNeighbour);
                        //Log.d("tempG", String.valueOf( Color.green(pixelNeighbour)));
                        int diffBlue = Color.blue(pixelNeighbour);
                        int tempGrayscale = (int) (0.299 * diffRed + 0.587 * diffGreen + 0.114 * diffBlue);
                        int currDiff = Math.abs(tempGrayscale - Grayscale);
                        if (currDiff >= MaxDiffGrayscale) {
                            MaxDiffGrayscale = currDiff;
                        }

                }
                Color NewColour = new Color();
                int pixel = NewColour.rgb(MaxDiffGrayscale, MaxDiffGrayscale, MaxDiffGrayscale);
                resBitmap.setPixel(i, j, pixel);
            }
        }
        Log.d("Finish", "Border Neighbour finished");
        imgIdentitas.setImageBitmap(resBitmap);
        drawHistogram(resBitmap);
    }

    public void borderCenter(){
        int totalBlur = 1;
        Bitmap tempBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        int cellSize = (2*totalBlur) + 1;

        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                //Check for neighbour
                int CurrPix = tempBitmap.getPixel(i,j);
                int Red , Green , Blue , Grayscale,MaxDiffGrayscale = 0;
                Red = Color.red(CurrPix);
                Green = Color.green(CurrPix);
                Blue = Color.blue(CurrPix);
                Grayscale = (int) (0.299 * Red + 0.587 * Green + 0.114 * Blue);

                //Log.d("Pixel",String.valueOf(i) + "," + String.valueOf(j));
                for(int neighbourX = 0; neighbourX < cellSize; neighbourX++){
                    for(int neighbourY = 0; neighbourY < cellSize; neighbourY++){
                        //check for validity else fill with the extended from the other side for X side
                        int coordX, coordY;
                        if (((i-totalBlur + neighbourX ) < 0 )) {
                            coordX = ImgWidth - 1;
                        } else if ((i+ neighbourX ) >= ImgWidth ){
                            coordX = 0;
                        } else {
                            coordX = i - totalBlur + neighbourX;
                        }
                        //check for validity else fill with the extended from the other side for Y side
                        if((j-totalBlur + neighbourY) < 0 ){
                            coordY = ImgHeight - 1;
                        } else if ((j+ neighbourY ) >= ImgHeight ){
                            coordY = 0;
                        } else {
                            coordY = j-totalBlur + neighbourY;
                        }

                            //count the diff
                            if (neighbourX != 1 && neighbourY != 1) {
                                //Log.d("test counter", String.valueOf(counter));
                                int pixelNeighbour = tempBitmap.getPixel(coordX, coordY);
                                int diffRed = Color.red(pixelNeighbour);
                                //Log.d("tempRed", String.valueOf( Color.red(pixelNeighbour)));
                                int diffGreen = Color.green(pixelNeighbour);
                                //Log.d("tempG", String.valueOf( Color.green(pixelNeighbour)));
                                int diffBlue = Color.blue(pixelNeighbour);
                                int tempGrayscale = (int) (0.299 * diffRed + 0.587 * diffGreen + 0.114 * diffBlue);
                                int currDiff = Math.abs(tempGrayscale - Grayscale);
                                if (currDiff >= MaxDiffGrayscale){
                                    MaxDiffGrayscale = currDiff;
                                }
                            }

                    }
                }
                Color NewColour = new Color();
                int pixel = NewColour.rgb(MaxDiffGrayscale, MaxDiffGrayscale, MaxDiffGrayscale);
                resBitmap.setPixel(i, j, pixel);
            }
        }
        Log.d("Finish", "Border Center finished");
        imgIdentitas.setImageBitmap(resBitmap);
        drawHistogram(resBitmap);
    }

    public void sharpenImage(){
        int totalBlur = 1;
        Bitmap tempBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        int[] pix = new int[ImgWidth * ImgHeight];
        int cellSize = (2*totalBlur) + 1;
        int counter, totalRed = 0, totalGreen = 0, totalBlue = 0;

        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                //Check for neighbour
                counter = 0;
                totalRed = 0; totalGreen = 0; totalBlue = 0;

                //Log.d("Pixel",String.valueOf(i) + "," + String.valueOf(j));
                for(int neighbourX = 0; neighbourX < cellSize; neighbourX++){
                    for(int neighbourY = 0; neighbourY < cellSize; neighbourY++){
                        //check for validity else fill with 0 and ignore
                        if (((i-totalBlur + neighbourX ) < 0 ) || ((j-totalBlur + neighbourY) < 0 ) || ((i+ neighbourX ) >= ImgWidth ) || ((j+ neighbourY ) >= ImgHeight ) ){
                            //not valid neighbour
                            //Log.d("Test","0");
                        } else {
                            //Log.d("test counter", String.valueOf(counter));
                            int pixelNeighbour = tempBitmap.getPixel(i - totalBlur + neighbourX,j - totalBlur + neighbourY);
                            counter = counter + 1;
                            if (neighbourX + neighbourY == 1 || neighbourX + neighbourY == 3){
                                //direct neighbour

                                totalRed = totalRed + (int) (Color.red(pixelNeighbour)*directNeighbour);
                                //Log.d("tempRed", String.valueOf( Color.red(pixelNeighbour)));
                                totalGreen = totalGreen + (int) (Color.green(pixelNeighbour)*directNeighbour);
                                //Log.d("tempG", String.valueOf( Color.green(pixelNeighbour)));
                                totalBlue = totalBlue + (int) (Color.blue(pixelNeighbour)*directNeighbour);
                                //Log.d("tempB", String.valueOf( Color.blue(pixelNeighbour)));

                            } else if (neighbourX == 1 && neighbourY == 1){
                                //current pixel

                                totalRed = totalRed + (int) (Color.red(pixelNeighbour)*currentPixelDiv);
                                //Log.d("tempRed", String.valueOf( Color.red(pixelNeighbour)));
                                totalGreen = totalGreen + (int) (Color.green(pixelNeighbour)*currentPixelDiv);
                                //Log.d("tempG", String.valueOf( Color.green(pixelNeighbour)));
                                totalBlue = totalBlue + (int) (Color.blue(pixelNeighbour)*currentPixelDiv);
                                //Log.d("tempB", String.valueOf( Color.blue(pixelNeighbour)));
                            } else {
                                //diagonal neighbour

                                totalRed = totalRed + (int) (Color.red(pixelNeighbour)*diagonalNeighbour);
                                //Log.d("tempRed", String.valueOf( Color.red(pixelNeighbour)));
                                totalGreen = totalGreen + (int) (Color.green(pixelNeighbour) * diagonalNeighbour);
                                //Log.d("tempG", String.valueOf( Color.green(pixelNeighbour)));
                                totalBlue = totalBlue + (int) (Color.blue(pixelNeighbour)* diagonalNeighbour);
                                //Log.d("tempB", String.valueOf( Color.blue(pixelNeighbour)));
                            }

                        }
                    }
                }
                Log.d("TotalColor", "R:" + String.valueOf(totalRed) +",G:" + String.valueOf(totalGreen) +",B:" + String.valueOf(totalBlue) );

                //Log.d("NewColor", "R:" + String.valueOf(avgRed) +",G:" + String.valueOf(avgGreen) +",B:" + String.valueOf(avgBlue) );
                //normalize color
                if (totalBlue < 0){
                    totalBlue = 0;
                }
                if (totalGreen < 0){
                    totalGreen = 0;
                }
                if (totalRed < 0) {
                    totalRed = 0;
                }

                if (totalBlue >255){
                    totalBlue = 255;
                }
                if (totalGreen >255){
                    totalGreen = 255;
                }
                if (totalRed >255) {
                    totalRed = 255;
                }
                Color NewColour = new Color();
                int pixel = NewColour.rgb(totalRed, totalGreen, totalBlue);
                resBitmap.setPixel(i, j, pixel);
            }
        }
        Log.d("Finish", "Sharpen finished");
        imgIdentitas.setImageBitmap(resBitmap);
        drawHistogram(resBitmap);
    }

    public void OperatorLevel1(){
        int totalBlur = 1;
        Bitmap tempBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        int cellSize = (2*totalBlur) + 1;
        int counter, totalRed = 0, totalGreen = 0, totalBlue = 0;

        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                //Check for neighbour
                counter = 0;
                totalRed = 0; totalGreen = 0; totalBlue = 0;

                //Log.d("Pixel",String.valueOf(i) + "," + String.valueOf(j));
                for(int neighbourX = 0; neighbourX < cellSize; neighbourX++){
                    for(int neighbourY = 0; neighbourY < cellSize; neighbourY++){
                        //check for validity else fill with 0 and ignore
                        if (((i-totalBlur + neighbourX ) < 0 ) || ((j-totalBlur + neighbourY) < 0 ) || ((i+ neighbourX ) >= ImgWidth ) || ((j+ neighbourY ) >= ImgHeight ) ){
                            //not valid neighbour
                            //Log.d("Test","0");
                        } else {
                            //Log.d("test counter", String.valueOf(counter));
                            int pixelNeighbour = tempBitmap.getPixel(i - totalBlur + neighbourX,j - totalBlur + neighbourY);
                            counter = counter + 1;
                            totalRed = totalRed + (int) (Color.red(pixelNeighbour)*ArrayInt[neighbourX][neighbourY]);
                            //Log.d("tempRed", String.valueOf( Color.red(pixelNeighbour)));
                            totalGreen = totalGreen + (int) (Color.green(pixelNeighbour)*directNeighbour);
                            //Log.d("tempG", String.valueOf( Color.green(pixelNeighbour)));
                            totalBlue = totalBlue + (int) (Color.blue(pixelNeighbour)*directNeighbour);
                            //Log.d("tempB", String.valueOf( Color.blue(pixelNeighbour)));
                        }
                    }
                }
                Log.d("TotalColor", "R:" + String.valueOf(totalRed) +",G:" + String.valueOf(totalGreen) +",B:" + String.valueOf(totalBlue) );

                //Log.d("NewColor", "R:" + String.valueOf(avgRed) +",G:" + String.valueOf(avgGreen) +",B:" + String.valueOf(avgBlue) );
                //normalize color
                if (totalBlue < 0){
                    totalBlue = 0;
                }
                if (totalGreen < 0){
                    totalGreen = 0;
                }
                if (totalRed < 0) {
                    totalRed = 0;
                }

                if (totalBlue >255){
                    totalBlue = 255;
                }
                if (totalGreen >255){
                    totalGreen = 255;
                }
                if (totalRed >255) {
                    totalRed = 255;
                }
                Color NewColour = new Color();
                int pixel = NewColour.rgb(totalRed, totalGreen, totalBlue);
                resBitmap.setPixel(i, j, pixel);
            }
        }
        Log.d("Finish", "Sharpen finished");
        imgIdentitas.setImageBitmap(resBitmap);
        drawHistogram(resBitmap);
    }

    public void blurImage(){
        int totalBlur = 1;
        Bitmap tempBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        int cellSize = (2*totalBlur) + 1;
        int counter, totalRed = 0, totalGreen = 0, totalBlue = 0;

        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                //Check for neighbour
                counter = 0;
                totalRed = 0; totalGreen = 0; totalBlue = 0;

                //Log.d("Pixel",String.valueOf(i) + "," + String.valueOf(j));
                for(int neighbourX = 0; neighbourX < cellSize; neighbourX++){
                    for(int neighbourY = 0; neighbourY < cellSize; neighbourY++){
                        //check for validity else fill with 0 and ignore
                        //check for validity else fill with the extended from the other side for X side
                        int coordX, coordY;
                        if (((i-totalBlur + neighbourX ) < 0 )) {
                            coordX = ImgWidth - 1;
                        } else if ((i+ neighbourX ) >= ImgWidth ){
                            coordX = 0;
                        } else {
                            coordX = i - totalBlur + neighbourX;
                        }
                        //check for validity else fill with the extended from the other side for Y side
                        if((j-totalBlur + neighbourY) < 0 ){
                            coordY = ImgHeight - 1;
                        } else if ((j+ neighbourY ) >= ImgHeight ){
                            coordY = 0;
                        } else {
                            coordY = j-totalBlur + neighbourY;
                        }

                        //Start to blur
                        int pixelNeighbour = tempBitmap.getPixel(coordX,coordY);
                        counter = counter + 1;
                        if (neighbourX + neighbourY == 1 || neighbourX + neighbourY == 3){
                            //direct neighbour

                            totalRed = totalRed + (int) (Color.red(pixelNeighbour)/directNeighbour);
                            //Log.d("tempRed", String.valueOf( Color.red(pixelNeighbour)));
                            totalGreen = totalGreen + (int) (Color.green(pixelNeighbour)/directNeighbour);
                            //Log.d("tempG", String.valueOf( Color.green(pixelNeighbour)));
                            totalBlue = totalBlue + (int) (Color.blue(pixelNeighbour)/directNeighbour);
                            //Log.d("tempB", String.valueOf( Color.blue(pixelNeighbour)));

                        } else if (neighbourX == 1 && neighbourY == 1){
                            //current pixel

                            totalRed = totalRed + (int) (Color.red(pixelNeighbour)/currentPixelDiv);
                            //Log.d("tempRed", String.valueOf( Color.red(pixelNeighbour)));
                            totalGreen = totalGreen + (int) (Color.green(pixelNeighbour)/currentPixelDiv);
                            //Log.d("tempG", String.valueOf( Color.green(pixelNeighbour)));
                            totalBlue = totalBlue + (int) (Color.blue(pixelNeighbour)/currentPixelDiv);
                            //Log.d("tempB", String.valueOf( Color.blue(pixelNeighbour)));
                        } else {
                            //diagonal neighbour

                            totalRed = totalRed + (int) (Color.red(pixelNeighbour)/diagonalNeighbour);
                            //Log.d("tempRed", String.valueOf( Color.red(pixelNeighbour)));
                            totalGreen = totalGreen + (int) (Color.green(pixelNeighbour)/diagonalNeighbour);
                            //Log.d("tempG", String.valueOf( Color.green(pixelNeighbour)));
                            totalBlue = totalBlue + (int) (Color.blue(pixelNeighbour)/diagonalNeighbour);
                            //Log.d("tempB", String.valueOf( Color.blue(pixelNeighbour)));
                        }
                    }
                }
                //Log.d("TotalColor", "R:" + String.valueOf(totalRed) +",G:" + String.valueOf(totalGreen) +",B:" + String.valueOf(totalBlue) );

                //Log.d("NewColor", "R:" + String.valueOf(avgRed) +",G:" + String.valueOf(avgGreen) +",B:" + String.valueOf(avgBlue) );
                Color NewColour = new Color();
                int pixel = NewColour.rgb(totalRed, totalGreen, totalBlue);
                tempBitmap.setPixel(i, j, pixel);
            }
        }
        Log.d("Finish", "Blur finished");
        imgIdentitas.setImageBitmap(tempBitmap);
        drawHistogram(tempBitmap);
    }

}

class Count<K, V> extends HashMap<K, V> {

    // Counts unique objects
    public void add(K o) {
        int count = this.containsKey(o) ? ((Integer) this.get(o)).intValue() + 1 : 1;
        super.put(o, (V) new Integer(count));
    }
}
