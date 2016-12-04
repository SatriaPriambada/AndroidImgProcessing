package com.pengcit.simplecamera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by Satria on 10/5/2016.
 */
public class FaceRecog extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    public static Bitmap FaceRecogBitmap;
    public static Bitmap nextBitmap;
    private int matrixSize = 3;
    private  float[][] matrix = new float[matrixSize][matrixSize];
    private final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 80;

    ImageView imgFace;
    TextView textGratioKepala, textGratioMata, textStringcode;
    int maxString = 99999;
    int stringCode[] = new int[maxString];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_recog);
        //set toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Set image
        Intent intent = getIntent();
        FaceRecogBitmap = (Bitmap) intent.getParcelableExtra("bitmap");
        imgFace = (ImageView)findViewById(R.id.imgFace);
        textGratioKepala = (TextView)findViewById(R.id.GratioKepala);
        textGratioMata = (TextView)findViewById(R.id.GratioMata);
        textStringcode = (TextView)findViewById(R.id.StringCodeFace);
        if (imgFace != null){
            imgFace.setImageBitmap(FaceRecogBitmap);
        }

        Button detectbut = (Button)findViewById(R.id.detectbutton);
        Button applybut = (Button)findViewById(R.id.buttonApply);
        //Set spinner
        Spinner spinner = (Spinner) findViewById(R.id.spinnerFilter);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.filterChoice_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        detectbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FilterFaceImage();
            }
        });

        applybut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CharRecog();
            }
        });
    }

    public void CharRecog(){
        Intent i = new Intent(this, CharRecognitionActivity.class);
        this.startActivity(i);
    }

    public void FilterFaceImage(){
        Toast.makeText(getApplicationContext(), "Detect Face", Toast.LENGTH_SHORT).show();
        Bitmap bMap = BitmapFactory.decodeFile("/sdcard/MaskMuka.png");
        Bitmap tempBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resultBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        Robinson3();
        Otsu(true);
        int[] listMarker = new int[10];
        int couterStringCode = 0;
        imgFace.setImageBitmap(nextBitmap);
        for (int i = 0; i < ImgWidth ; i++) {
            for (int j = 0; j < ImgHeight ; j++) {
                int pixel = nextBitmap.getPixel(i,j);
                int redPixel = Color.red(pixel);

                int bMappixel = bMap.getPixel(i, j);
                int alpha = Color.alpha(bMappixel);
                Color NewColour = new Color();
                int newpixel;
                //invert otsu
                if(redPixel == 0){
                    newpixel = NewColour.rgb(255, 255, 255);
                } else {
                    newpixel = NewColour.rgb(0, 0, 0);
                }

                tempBitmap.setPixel(i, j, newpixel);

                if (alpha == 255){
                    newpixel = NewColour.rgb(0, 0, 0);
                    tempBitmap.setPixel(i,j,newpixel);

                    //Check for string code
                    stringCode[couterStringCode] = ((i+j) % 9) + 1;
                    ++couterStringCode;
                }
            }
        }
        int borderMiddle = (int) (ImgWidth/2);
        int Iter = 0;
        for (int searchBorder = 0; searchBorder < ImgHeight; searchBorder++) {

            int bMappixel = bMap.getPixel(borderMiddle,searchBorder );
            int alpha = Color.alpha(bMappixel);
            if (alpha == 255){

                //Check for marker
                listMarker[Iter] = searchBorder;
                ++Iter;
            }
        }
        imgFace.setImageBitmap(tempBitmap);
        for (int i = 0; i < listMarker.length; i++) {
            System.out.println("ListMarker [" + i + "] " + listMarker[i]);
        }
        double upPart = (Math.abs(listMarker[0] - listMarker[5])) ;
        double downPart = (Math.abs(listMarker[8] - listMarker[5]));
        double GRatio = (double) (upPart/downPart) ;
        textGratioKepala.setText("Kepala-Hidung-Dagu : " + String.valueOf(GRatio));
        upPart = (Math.abs(listMarker[3] - listMarker[5])) ;
        downPart = (Math.abs(listMarker[6] - listMarker[5]));
        GRatio = (double) (upPart/downPart) ;
        textGratioMata.setText("Mata-Hidung-Mulut : " + String.valueOf(GRatio));

        String sc = "";
        for (int i = 0; i < 100; i++) {
            sc = sc + stringCode[i];
        }
        System.out.println("sc :" + sc);
        textStringcode.setText("String Code : " + sc);

    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        String selected = (String) parent.getItemAtPosition(pos);
        int posNow = (int) parent.getLastVisiblePosition();
        /*Toast.makeText(getApplicationContext(), selected, Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), String.valueOf(pos), Toast.LENGTH_SHORT).show();*/
        switch (posNow){
            case 0:
                break;
            case 1:
                Prewitt();
                break;
            case 2:
                Sobel();
                break;
            case 3:
                FreiChen();
                break;
            case 4:
                Robert();
                break;
            case 5:
                Prewitt8();
                break;
            case 6:
                Kirch();
                break;
            case 7:
                Robinson3();
                break;
            case 8:
                Robinson5();
                break;
            case 9:
                Otsu(false);
                break;
            default:
                break;
        }
    }



    public void Robert(){
        Toast.makeText(getApplicationContext(), "Robert Convolution", Toast.LENGTH_SHORT).show();
        readFile("OpRobert.txt");
        int totalBlur = 1;
        Bitmap tempBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resultBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        int cellSize = 3;
        int counter, totalRed = 0, totalGreen = 0, totalBlue = 0;

        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                //Do for vertical Matrix
                float TotalTempR = 0f;
                float TotalTempG = 0f;
                float TotalTempB = 0f;
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

                        //Start to convolve image with matrix
                        int pixelNeighbour = tempBitmap.getPixel(coordX,coordY);
                        TotalTempR = TotalTempR + Color.red(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTempG = TotalTempG + Color.green(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTempB = TotalTempB + Color.blue(pixelNeighbour) * matrix[neighbourX][neighbourY];
                    }
                }

                //rotate 2 times to get the horizontal matrix
                rotateMatrix();
                rotateMatrix();
                //printMatrix();
                float TotalTemp2R = 0f;
                float TotalTemp2G = 0f;
                float TotalTemp2B = 0f;
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

                        //Start to convolve image with matrix
                        int pixelNeighbour = tempBitmap.getPixel(coordX,coordY);
                        TotalTemp2R = TotalTemp2R + Color.red(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTemp2G = TotalTemp2G + Color.green(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTemp2B = TotalTemp2B + Color.blue(pixelNeighbour) * matrix[neighbourX][neighbourY];
                    }
                }
                //Log.d("TotalColor", "R:" + String.valueOf(totalRed) +",G:" + String.valueOf(totalGreen) +",B:" + String.valueOf(totalBlue) );

                //Log.d("NewColor", "R:" + String.valueOf(avgRed) +",G:" + String.valueOf(avgGreen) +",B:" + String.valueOf(avgBlue) );
                Color NewColour = new Color();
                int pixel;
                double totalR = Math.pow(TotalTempR,2) + Math.pow(TotalTemp2R,2);
                double magnitudeR = Math.sqrt(totalR);
                double totalG = Math.pow(TotalTempG,2) + Math.pow(TotalTemp2G,2);
                double magnitudeG = Math.sqrt(totalG);
                double totalB = Math.pow(TotalTempB,2) + Math.pow(TotalTemp2B,2);
                double magnitudeB = Math.sqrt(totalB);
                pixel = NewColour.rgb((int)magnitudeR, (int)magnitudeG, (int)magnitudeB);

                resultBitmap.setPixel(i, j, pixel);
                //rotate matrix back to normal
                rotateBackMatrix();
                rotateBackMatrix();
            }
        }
        Log.d("Finish", "Robert finished");
        imgFace.setImageBitmap(resultBitmap);
    }

    public void Prewitt(){
        Toast.makeText(getApplicationContext(), "Prewitt Convolution", Toast.LENGTH_SHORT).show();
        readFile("OpPrewitt.txt");
        int totalBlur = 1;
        Bitmap tempBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resultBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        int cellSize = 3;
        int counter, totalRed = 0, totalGreen = 0, totalBlue = 0;

        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                //Do for vertical Matrix
                float TotalTempR = 0f;
                float TotalTempG = 0f;
                float TotalTempB = 0f;
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

                        //Start to convolve image with matrix
                        int pixelNeighbour = tempBitmap.getPixel(coordX,coordY);
                        TotalTempR = TotalTempR + Color.red(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTempG = TotalTempG + Color.green(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTempB = TotalTempB + Color.blue(pixelNeighbour) * matrix[neighbourX][neighbourY];
                    }
                }

                //rotate 2 times to get the horizontal matrix
                rotateMatrix();
                rotateMatrix();
                //printMatrix();
                float TotalTemp2R = 0f;
                float TotalTemp2G = 0f;
                float TotalTemp2B = 0f;
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

                        //Start to convolve image with matrix
                        int pixelNeighbour = tempBitmap.getPixel(coordX,coordY);
                        TotalTemp2R = TotalTemp2R + Color.red(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTemp2G = TotalTemp2G + Color.green(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTemp2B = TotalTemp2B + Color.blue(pixelNeighbour) * matrix[neighbourX][neighbourY];
                    }
                }
                //Log.d("TotalColor", "R:" + String.valueOf(totalRed) +",G:" + String.valueOf(totalGreen) +",B:" + String.valueOf(totalBlue) );

                //Log.d("NewColor", "R:" + String.valueOf(avgRed) +",G:" + String.valueOf(avgGreen) +",B:" + String.valueOf(avgBlue) );
                Color NewColour = new Color();
                int pixel;
                double totalR = Math.pow(TotalTempR,2) + Math.pow(TotalTemp2R,2);
                double magnitudeR = Math.sqrt(totalR);
                double totalG = Math.pow(TotalTempG,2) + Math.pow(TotalTemp2G,2);
                double magnitudeG = Math.sqrt(totalG);
                double totalB = Math.pow(TotalTempB,2) + Math.pow(TotalTemp2B,2);
                double magnitudeB = Math.sqrt(totalB);
                pixel = NewColour.rgb((int)magnitudeR, (int)magnitudeG, (int)magnitudeB);

                resultBitmap.setPixel(i, j, pixel);
                //rotate matrix back to normal
                rotateBackMatrix();
                rotateBackMatrix();
            }
        }
        Log.d("Finish", "Prewitt finished");
        imgFace.setImageBitmap(resultBitmap);
    }

    public void Sobel(){
        Toast.makeText(getApplicationContext(), "Sobel Convolution", Toast.LENGTH_SHORT).show();
        readFile("OpSobel.txt");
        int totalBlur = 1;
        Bitmap tempBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resultBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        int cellSize = 3;
        int counter, totalRed = 0, totalGreen = 0, totalBlue = 0;

        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                //Do for vertical Matrix
                float TotalTempR = 0f;
                float TotalTempG = 0f;
                float TotalTempB = 0f;
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

                        //Start to convolve image with matrix
                        int pixelNeighbour = tempBitmap.getPixel(coordX,coordY);
                        TotalTempR = TotalTempR + Color.red(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTempG = TotalTempG + Color.green(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTempB = TotalTempB + Color.blue(pixelNeighbour) * matrix[neighbourX][neighbourY];
                    }
                }

                //rotate 2 times to get the horizontal matrix
                rotateMatrix();
                rotateMatrix();
                //printMatrix();
                float TotalTemp2R = 0f;
                float TotalTemp2G = 0f;
                float TotalTemp2B = 0f;
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

                        //Start to convolve image with matrix
                        int pixelNeighbour = tempBitmap.getPixel(coordX,coordY);
                        TotalTemp2R = TotalTemp2R + Color.red(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTemp2G = TotalTemp2G + Color.green(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTemp2B = TotalTemp2B + Color.blue(pixelNeighbour) * matrix[neighbourX][neighbourY];
                    }
                }
                //Log.d("TotalColor", "R:" + String.valueOf(totalRed) +",G:" + String.valueOf(totalGreen) +",B:" + String.valueOf(totalBlue) );

                //Log.d("NewColor", "R:" + String.valueOf(avgRed) +",G:" + String.valueOf(avgGreen) +",B:" + String.valueOf(avgBlue) );
                Color NewColour = new Color();
                int pixel;
                double totalR = Math.pow(TotalTempR,2) + Math.pow(TotalTemp2R,2);
                double magnitudeR = Math.sqrt(totalR);
                double totalG = Math.pow(TotalTempG,2) + Math.pow(TotalTemp2G,2);
                double magnitudeG = Math.sqrt(totalG);
                double totalB = Math.pow(TotalTempB,2) + Math.pow(TotalTemp2B,2);
                double magnitudeB = Math.sqrt(totalB);
                pixel = NewColour.rgb((int)magnitudeR, (int)magnitudeG, (int)magnitudeB);

                resultBitmap.setPixel(i, j, pixel);
                //rotate matrix back to normal
                rotateBackMatrix();
                rotateBackMatrix();
            }
        }
        Log.d("Finish", "Sobel finished");
        imgFace.setImageBitmap(resultBitmap);
    }

    public void FreiChen(){
        Toast.makeText(getApplicationContext(), "Frei-Chen Convolution", Toast.LENGTH_SHORT).show();
        readFile("OpFreichen.txt");
        int totalBlur = 1;
        Bitmap tempBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resultBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        int cellSize = 3;
        int counter, totalRed = 0, totalGreen = 0, totalBlue = 0;

        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                //Do for vertical Matrix
                float TotalTempR = 0f;
                float TotalTempG = 0f;
                float TotalTempB = 0f;
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

                        //Start to convolve image with matrix
                        int pixelNeighbour = tempBitmap.getPixel(coordX,coordY);
                        TotalTempR = TotalTempR + Color.red(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTempG = TotalTempG + Color.green(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTempB = TotalTempB + Color.blue(pixelNeighbour) * matrix[neighbourX][neighbourY];
                    }
                }

                //rotate 2 times to get the horizontal matrix
                rotateMatrix();
                rotateMatrix();
                //printMatrix();
                float TotalTemp2R = 0f;
                float TotalTemp2G = 0f;
                float TotalTemp2B = 0f;
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

                        //Start to convolve image with matrix
                        int pixelNeighbour = tempBitmap.getPixel(coordX,coordY);
                        TotalTemp2R = TotalTemp2R + Color.red(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTemp2G = TotalTemp2G + Color.green(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTemp2B = TotalTemp2B + Color.blue(pixelNeighbour) * matrix[neighbourX][neighbourY];
                    }
                }
                //Log.d("TotalColor", "R:" + String.valueOf(totalRed) +",G:" + String.valueOf(totalGreen) +",B:" + String.valueOf(totalBlue) );

                //Log.d("NewColor", "R:" + String.valueOf(avgRed) +",G:" + String.valueOf(avgGreen) +",B:" + String.valueOf(avgBlue) );
                Color NewColour = new Color();
                int pixel;
                double totalR = Math.pow(TotalTempR,2) + Math.pow(TotalTemp2R,2);
                double magnitudeR = Math.sqrt(totalR);
                double totalG = Math.pow(TotalTempG,2) + Math.pow(TotalTemp2G,2);
                double magnitudeG = Math.sqrt(totalG);
                double totalB = Math.pow(TotalTempB,2) + Math.pow(TotalTemp2B,2);
                double magnitudeB = Math.sqrt(totalB);
                pixel = NewColour.rgb((int)magnitudeR, (int)magnitudeG, (int)magnitudeB);

                resultBitmap.setPixel(i, j, pixel);
                //rotate matrix back to normal
                rotateBackMatrix();
                rotateBackMatrix();
            }
        }
        Log.d("Finish", "FreiChen finished");
        imgFace.setImageBitmap(resultBitmap);
    }

    public void Prewitt8(){
        Toast.makeText(getApplicationContext(), "Prewitt 8 Convolution", Toast.LENGTH_SHORT).show();
        readFile("OpPrewitt8.txt");
        int totalBlur = 1;
        Bitmap tempBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resultBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        int cellSize = 3;
        int counter, totalRed = 0, totalGreen = 0, totalBlue = 0;

        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                //Do for 1st Matrix
                float TotalTempR = 0f;
                float TotalTempG = 0f;
                float TotalTempB = 0f;
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

                        //Start to convolve image with matrix
                        int pixelNeighbour = tempBitmap.getPixel(coordX,coordY);
                        TotalTempR = TotalTempR + Color.red(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTempG = TotalTempG + Color.green(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTempB = TotalTempB + Color.blue(pixelNeighbour) * matrix[neighbourX][neighbourY];
                    }
                }
                int pixel = 0;
                for (int l = 0; l < 7; l++) {
                    //rotate 1 times to get the next matrix
                    rotateMatrix();
                    //System.out.println("Rotation number : " + l + 1);
                    //printMatrix();

                    float TotalTemp2R = 0f;
                    float TotalTemp2G = 0f;
                    float TotalTemp2B = 0f;
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

                            //Start to convolve image with matrix
                            int pixelNeighbour = tempBitmap.getPixel(coordX,coordY);

                            TotalTemp2R = TotalTemp2R + Color.red(pixelNeighbour) * matrix[neighbourX][neighbourY];
                            TotalTemp2G = TotalTemp2G + Color.green(pixelNeighbour) * matrix[neighbourX][neighbourY];
                            TotalTemp2B = TotalTemp2B + Color.blue(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        }
                    }
                    //Log.d("TotalColor", "R:" + String.valueOf(totalRed) +",G:" + String.valueOf(totalGreen) +",B:" + String.valueOf(totalBlue) );

                    //Log.d("NewColor", "R:" + String.valueOf(avgRed) +",G:" + String.valueOf(avgGreen) +",B:" + String.valueOf(avgBlue) );

                    if (TotalTempR < TotalTemp2R){
                        //the current totalTemp is bigger assign the intermediate bigger with current
                        TotalTempR = TotalTemp2R;
                    }
                    if (TotalTempG < TotalTemp2G){
                        //the current totalTemp is bigger assign the intermediate bigger with current
                        TotalTempG = TotalTemp2G;
                    }
                    if (TotalTempB < TotalTemp2B){
                        //the current totalTemp is bigger assign the intermediate bigger with current
                        TotalTempB = TotalTemp2B;
                    }
                    //System.out.println("R: " + TotalTempR +"G: " + TotalTempG + "B: " + TotalTempB);

                }
                Color NewColour = new Color();
                pixel = NewColour.rgb((int)TotalTempR, (int)TotalTempG, (int)TotalTempB);
                resultBitmap.setPixel(i, j, pixel);
            }
        }
        Log.d("Finish", "Prewitt8 finished");
        imgFace.setImageBitmap(resultBitmap);
    }

    public void Kirch(){
        Toast.makeText(getApplicationContext(), "Kirch Convolution", Toast.LENGTH_SHORT).show();
        readFile("OpKirch.txt");
        int totalBlur = 1;
        Bitmap tempBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resultBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        int cellSize = 3;
        int counter, totalRed = 0, totalGreen = 0, totalBlue = 0;

        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                //Do for 1st Matrix
                float TotalTempR = 0f;
                float TotalTempG = 0f;
                float TotalTempB = 0f;
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

                        //Start to convolve image with matrix
                        int pixelNeighbour = tempBitmap.getPixel(coordX,coordY);
                        TotalTempR = TotalTempR + Color.red(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTempG = TotalTempG + Color.green(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTempB = TotalTempB + Color.blue(pixelNeighbour) * matrix[neighbourX][neighbourY];
                    }
                }
                int pixel = 0;
                for (int l = 0; l < 7; l++) {
                    //rotate 1 times to get the next matrix
                    rotateMatrix();
                    //System.out.println("Rotation number : " + l + 1);
                    //printMatrix();

                    float TotalTemp2R = 0f;
                    float TotalTemp2G = 0f;
                    float TotalTemp2B = 0f;
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

                            //Start to convolve image with matrix
                            int pixelNeighbour = tempBitmap.getPixel(coordX,coordY);

                            TotalTemp2R = TotalTemp2R + Color.red(pixelNeighbour) * matrix[neighbourX][neighbourY];
                            TotalTemp2G = TotalTemp2G + Color.green(pixelNeighbour) * matrix[neighbourX][neighbourY];
                            TotalTemp2B = TotalTemp2B + Color.blue(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        }
                    }
                    //Log.d("TotalColor", "R:" + String.valueOf(totalRed) +",G:" + String.valueOf(totalGreen) +",B:" + String.valueOf(totalBlue) );

                    //Log.d("NewColor", "R:" + String.valueOf(avgRed) +",G:" + String.valueOf(avgGreen) +",B:" + String.valueOf(avgBlue) );

                    if (TotalTempR < TotalTemp2R){
                        //the current totalTemp is bigger assign the intermediate bigger with current
                        TotalTempR = TotalTemp2R;
                    }
                    if (TotalTempG < TotalTemp2G){
                        //the current totalTemp is bigger assign the intermediate bigger with current
                        TotalTempG = TotalTemp2G;
                    }
                    if (TotalTempB < TotalTemp2B){
                        //the current totalTemp is bigger assign the intermediate bigger with current
                        TotalTempB = TotalTemp2B;
                    }
                    //System.out.println("R: " + TotalTempR +"G: " + TotalTempG + "B: " + TotalTempB);

                }
                Color NewColour = new Color();
                pixel = NewColour.rgb((int)TotalTempR, (int)TotalTempG, (int)TotalTempB);
                resultBitmap.setPixel(i, j, pixel);
            }
        }
        Log.d("Finish", "Kirch finished");
        imgFace.setImageBitmap(resultBitmap);
    }
    public void Robinson3(){
        Toast.makeText(getApplicationContext(), "Robinson 3 Convolution", Toast.LENGTH_SHORT).show();
        readFile("OpRobinson3.txt");
        int totalBlur = 1;
        Bitmap tempBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resultBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        int cellSize = 3;
        int counter, totalRed = 0, totalGreen = 0, totalBlue = 0;

        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                //Do for 1st Matrix
                float TotalTempR = 0f;
                float TotalTempG = 0f;
                float TotalTempB = 0f;
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

                        //Start to convolve image with matrix
                        int pixelNeighbour = tempBitmap.getPixel(coordX,coordY);
                        TotalTempR = TotalTempR + Color.red(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTempG = TotalTempG + Color.green(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTempB = TotalTempB + Color.blue(pixelNeighbour) * matrix[neighbourX][neighbourY];
                    }
                }
                int pixel = 0;
                for (int l = 0; l < 7; l++) {
                    //rotate 1 times to get the next matrix
                    rotateMatrix();
                    //System.out.println("Rotation number : " + l + 1);
                    //printMatrix();

                    float TotalTemp2R = 0f;
                    float TotalTemp2G = 0f;
                    float TotalTemp2B = 0f;
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

                            //Start to convolve image with matrix
                            int pixelNeighbour = tempBitmap.getPixel(coordX,coordY);

                            TotalTemp2R = TotalTemp2R + Color.red(pixelNeighbour) * matrix[neighbourX][neighbourY];
                            TotalTemp2G = TotalTemp2G + Color.green(pixelNeighbour) * matrix[neighbourX][neighbourY];
                            TotalTemp2B = TotalTemp2B + Color.blue(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        }
                    }
                    //Log.d("TotalColor", "R:" + String.valueOf(totalRed) +",G:" + String.valueOf(totalGreen) +",B:" + String.valueOf(totalBlue) );

                    //Log.d("NewColor", "R:" + String.valueOf(avgRed) +",G:" + String.valueOf(avgGreen) +",B:" + String.valueOf(avgBlue) );

                    if (TotalTempR < TotalTemp2R){
                        //the current totalTemp is bigger assign the intermediate bigger with current
                        TotalTempR = TotalTemp2R;
                    }
                    if (TotalTempG < TotalTemp2G){
                        //the current totalTemp is bigger assign the intermediate bigger with current
                        TotalTempG = TotalTemp2G;
                    }
                    if (TotalTempB < TotalTemp2B){
                        //the current totalTemp is bigger assign the intermediate bigger with current
                        TotalTempB = TotalTemp2B;
                    }
                    //System.out.println("R: " + TotalTempR +"G: " + TotalTempG + "B: " + TotalTempB);

                }
                Color NewColour = new Color();
                pixel = NewColour.rgb((int)TotalTempR, (int)TotalTempG, (int)TotalTempB);
                resultBitmap.setPixel(i, j, pixel);
            }
        }
        Log.d("Finish", "Robinson3 finished");
        imgFace.setImageBitmap(resultBitmap);
        nextBitmap = resultBitmap;
    }

    public void Robinson5(){
        Toast.makeText(getApplicationContext(), "Robinson 5 Convolution", Toast.LENGTH_SHORT).show();
        readFile("OpRobinson5.txt");
        int totalBlur = 1;
        Bitmap tempBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resultBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        int cellSize = 3;
        int counter, totalRed = 0, totalGreen = 0, totalBlue = 0;

        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                //Do for 1st Matrix
                float TotalTempR = 0f;
                float TotalTempG = 0f;
                float TotalTempB = 0f;
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

                        //Start to convolve image with matrix
                        int pixelNeighbour = tempBitmap.getPixel(coordX,coordY);
                        TotalTempR = TotalTempR + Color.red(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTempG = TotalTempG + Color.green(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        TotalTempB = TotalTempB + Color.blue(pixelNeighbour) * matrix[neighbourX][neighbourY];
                    }
                }
                int pixel = 0;
                for (int l = 0; l < 7; l++) {
                    //rotate 1 times to get the next matrix
                    rotateMatrix();
                    //System.out.println("Rotation number : " + l + 1);
                    //printMatrix();

                    float TotalTemp2R = 0f;
                    float TotalTemp2G = 0f;
                    float TotalTemp2B = 0f;
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

                            //Start to convolve image with matrix
                            int pixelNeighbour = tempBitmap.getPixel(coordX,coordY);

                            TotalTemp2R = TotalTemp2R + Color.red(pixelNeighbour) * matrix[neighbourX][neighbourY];
                            TotalTemp2G = TotalTemp2G + Color.green(pixelNeighbour) * matrix[neighbourX][neighbourY];
                            TotalTemp2B = TotalTemp2B + Color.blue(pixelNeighbour) * matrix[neighbourX][neighbourY];
                        }
                    }
                    //Log.d("TotalColor", "R:" + String.valueOf(totalRed) +",G:" + String.valueOf(totalGreen) +",B:" + String.valueOf(totalBlue) );

                    //Log.d("NewColor", "R:" + String.valueOf(avgRed) +",G:" + String.valueOf(avgGreen) +",B:" + String.valueOf(avgBlue) );

                    if (TotalTempR < TotalTemp2R){
                        //the current totalTemp is bigger assign the intermediate bigger with current
                        TotalTempR = TotalTemp2R;
                    }
                    if (TotalTempG < TotalTemp2G){
                        //the current totalTemp is bigger assign the intermediate bigger with current
                        TotalTempG = TotalTemp2G;
                    }
                    if (TotalTempB < TotalTemp2B){
                        //the current totalTemp is bigger assign the intermediate bigger with current
                        TotalTempB = TotalTemp2B;
                    }
                    //System.out.println("R: " + TotalTempR +"G: " + TotalTempG + "B: " + TotalTempB);

                }
                Color NewColour = new Color();
                pixel = NewColour.rgb((int)TotalTempR, (int)TotalTempG, (int)TotalTempB);
                resultBitmap.setPixel(i, j, pixel);
            }
        }
        Log.d("Finish", "Robinson5 finished");
        imgFace.setImageBitmap(resultBitmap);
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }


    private void readFile(String filename){
        //Find the directory for the SD Card using the API
        //*Don't* hardcode "/sdcard"
        File sdcard = Environment.getExternalStorageDirectory();
        //Get the text file
        File file = new File(sdcard,filename);
        Log.d("tio", file.toString());
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
        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize ; j++) {
                matrix[i][j] = Float.valueOf(listText[i*matrixSize + j]);
            }
        }

    }

    //code taken from http://www.labbookpages.co.uk/software/imgProc/otsuThreshold.html with modification for android
    public void Otsu(boolean next){
        int histData[] = new int[256];
        // Calculate histogram
        int ptr = 0;
        Bitmap tempBitmap;
        if (next) {

            tempBitmap = nextBitmap.copy(Bitmap.Config.ARGB_8888, true);
        } else {

            tempBitmap = FaceRecogBitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
        int red,green,blue,alpha, totalRed = 0, totalGreen = 0, totalBlue = 0;
        int ImgWidth = FaceRecogBitmap.getWidth();
        int ImgHeight = FaceRecogBitmap.getHeight();
        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                int colour = tempBitmap.getPixel(i,j);
                red = Color.red(colour);
                green = Color.green(colour);
                blue = Color.blue(colour);
                //String msg = "R: "+ red + " G: " + green + " B: " + blue;
                //Log.d("Color",msg);
                int avgColor = (int) (0.299f * red + 0.587f * green + 0.114f * blue);
                histData[avgColor] ++;
            }
        }

        // Total number of pixels
        int total = ImgHeight * ImgWidth;

        float sum = 0;
        for (int t=0 ; t<256 ; t++) sum += t * histData[t];

        float sumB = 0;
        int wB = 0;
        int wF = 0;

        float varMax = 0;
        int threshold = 0;

        for (int t=0 ; t<256 ; t++) {
            wB += histData[t];               // Weight Background
            if (wB == 0) continue;

            wF = total - wB;                 // Weight Foreground
            if (wF == 0) break;

            sumB += (float) (t * histData[t]);

            float mB = sumB / wB;            // Mean Background
            float mF = (sum - sumB) / wF;    // Mean Foreground

            // Calculate Between Class Variance
            float varBetween = (float)wB * (float)wF * (mB - mF) * (mB - mF);

            // Check if new maximum found
            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = t;
            }
        }

        System.out.println("Threshold : " + threshold);
        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                int colour = tempBitmap.getPixel(i,j);
                red = Color.red(colour);
                green = Color.green(colour);
                blue = Color.blue(colour);
                //String msg = "R: "+ red + " G: " + green + " B: " + blue;
                //Log.d("Color",msg);
                int avgColor = (int) (0.299f * red + 0.587f * green + 0.114f * blue);
                Color NewColour = new Color();
                int pixel = 0;

                if (avgColor < threshold){
                    pixel = NewColour.rgb(0, 0, 0);
                } else {
                    pixel = NewColour.rgb(255, 255, 255);
                }

                tempBitmap.setPixel(i, j, pixel);
            }
        }
        imgFace.setImageBitmap(tempBitmap);
        nextBitmap = tempBitmap;
    }

    public void rotateMatrix(){
        float temp = matrix[0][0];
        //Change 0,1 to 0,0
        matrix[0][0] = matrix[0][1];
        //Change 0,2 to 0,1
        matrix[0][1] = matrix[0][2];
        //Change 1,2 to 0,2
        matrix[0][2] = matrix[1][2];
        //Change 2,2 to 1,2
        matrix[1][2] = matrix[2][2];
        //Change 2,1 to 2,2
        matrix[2][2] = matrix[2][1];
        //Change 2,0 to 2,1
        matrix[2][1] = matrix[2][0];
        //Change 1,0 to 2,0
        matrix[2][0] = matrix[1][0];
        //Change 0,0 to 1,0
        matrix[1][0] = temp;
    }

    public void rotateBackMatrix(){
        float temp = matrix[0][0];
        //Change 1,0 to 0,0
        matrix[0][0] = matrix[1][0];
        //Change 2,0 to 1,0
        matrix[1][0] = matrix[2][0];
        //Change 2,1 to 2,0
        matrix[2][0] = matrix[2][1];
        //Change 2,2 to 2,1
        matrix[2][1] = matrix[2][2];
        //Change 1,2 to 2,2
        matrix[2][2] = matrix[1][2];
        //Change 0,2 to 1,2
        matrix[1][2] = matrix[0][2];
        //Change 0,1 to 0,2
        matrix[0][2] = matrix[0][1];
        //Change 0,0 to 1,0
        matrix[0][1] = temp;
    }


}


