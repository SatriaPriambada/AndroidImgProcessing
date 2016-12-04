package com.pengcit.simplecamera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.openalpr.OpenALPR;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CharRecognitionActivity extends AppCompatActivity {

    public static int MEDIA_TYPE_IMAGE = 1;
    private final int REQUEST_CAMERA = 100;
    private final int SELECT_FILE = 101;
    private final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 80;
    public static Bitmap _bitmap;
    TextView stringCodeText;
    EditText lowPass, highPass;
    Uri _selectedImageUri;
    ImageView imgIdentitas, imgFreqFourier;
    int maxString = 99999;
    int stringCode[] = new int[maxString];
    String filename = new String();

    final static int[][] nbrs = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1},
            {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}};

    final static int[][][] nbrGroups = {{{0, 2, 4}, {2, 4, 6}}, {{0, 2, 6},
            {0, 4, 6}}};

    static List<Point> toWhite = new ArrayList<>();
    static char[][] grid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_char_recognition);

        Button photobut = (Button)findViewById(R.id.photobutton);
        Button selectbut = (Button)findViewById(R.id.selectbutton);
        Button recogbut = (Button)findViewById(R.id.charRecogbutton);
        Button skeletonbut = (Button)findViewById(R.id.Skeletonbutton);
        Button findfacebut = (Button)findViewById(R.id.findface_button);
        Button fourierbut = (Button)findViewById(R.id.fourier_button);
        Button platebut = (Button)findViewById(R.id.recog_button);
        imgIdentitas = (ImageView)findViewById(R.id.imgFace);
        imgFreqFourier = (ImageView)findViewById(R.id.fourierFreq);
        stringCodeText = (TextView) findViewById(R.id.stringCodeText);
        lowPass = (EditText) findViewById(R.id.lowPass);
        highPass = (EditText) findViewById(R.id.highPass);

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

        recogbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CharRecog();
            }
        });
        findfacebut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FindFace();
            }
        });

        platebut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RecogPlate();
            }
        });

        fourierbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fourier();
            }
        });

        skeletonbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                skeletonizeCode();
                String[] numberList = filename.split("\\/");
                String numberExtension = numberList[numberList.length - 1 ];
                String[] numExtList = numberExtension.split("\\.");
                String number = numExtList[0];
                stringCodeText.setText("Text : " + number);
            }
        });
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

    public void CharRecog(){
        Toast.makeText(getApplicationContext(), "Detect Character", Toast.LENGTH_SHORT).show();
        Bitmap tempBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resultBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        int totalBlur = 1;
        int cellSize = 3;
        boolean start = false;
        int startX = 0, startY = 0;
        //find the first pixel to traverse
        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                int colour = tempBitmap.getPixel(i,j);
                int red = Color.red(colour);
                int green = Color.green(colour);
                int blue = Color.blue(colour);

                if (red == 0 || green == 0 || blue == 0 ){
                    start = true;
                    startX = i;
                    startY = j;
                    break;
                }
            }
            if(start){
                break;
            }
        }
        int counterString = 0;

        int i = startX;
        int j = startY;
        boolean foundNeighbour = false;
        boolean skipFirst = true;
        System.out.println("Start Border x:"+ startX +" y: " + startY);
        int neighbourX = 0;
        int neighbourY = 0;
        int stringValue = 0;
        while (start && counterString < maxString){
            // check all neighbour if there is white

            while (neighbourX < cellSize){
                while( neighbourY < cellSize){
                    //System.out.println("Before loop Y neighX " + neighbourX + " neighY " + neighbourY);
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
                    //read neighbourY, neighbourX or baris,kolom
                    //change 0,0 to 1,2
                    if (neighbourX ==0 && neighbourY == 0){
                        coordX = coordX + 2;
                        coordY = coordY + 1;
                        stringValue = 6;
                    }

                    //change 1,0 to 2,2
                    if (neighbourX ==0 && neighbourY == 1){
                        coordX = coordX + 2;
                        coordY = coordY + 1;
                        stringValue = 9;
                    }

                    //change 2,0 to 2,1
                    if (neighbourX ==0 && neighbourY == 2){
                        coordX = coordX + 1;
                        stringValue = 8;
                    }

                    //change 0,1 to 2,0
                    if (neighbourX ==1 && neighbourY == 0){
                        coordX = coordX - 1;
                        coordY = coordY + 2;
                        stringValue = 7;
                    }

                    //change 1,1 to 1,0
                    if (neighbourX ==1 && neighbourY == 1){
                        coordX = coordX - 1;
                        stringValue = 4;
                    }

                    //change 2,1 to 0,0
                    if (neighbourX ==1 && neighbourY == 2){
                        coordX = coordX - 1;
                        coordY = coordY - 2;
                        stringValue = 1;
                    }

                    //change 0,2 to 0,1
                    if (neighbourX ==2 && neighbourY == 0){
                        coordX = coordX - 1;
                        stringValue = 2;
                    }

                    //change 1,2 to 0,2
                    if (neighbourX ==1 && neighbourY == 2){
                        coordY = coordY - 1;
                        stringValue = 3;
                    }

                    //change 2,2 to Finish
                    if (neighbourX ==2 && neighbourY == 2){
                        foundNeighbour = true;
                        start = false;
                        System.out.println("Next neighbour");
                        break;
                    }

                    //Start to convolve image with matrix
                    int pixelNeighbour = tempBitmap.getPixel(coordX,coordY);
                    int TotalTempR = Color.red(pixelNeighbour);
                    int TotalTempG = Color.green(pixelNeighbour);
                    int TotalTempB = Color.blue(pixelNeighbour);

                    System.out.println("Iterator coordX:"+ coordX +" coordY: " + coordY +" neighX " + neighbourX + " neighY " + neighbourY);
                    neighbourY++;
                    //search for the next black dot
                    if (TotalTempR == 0 && TotalTempG == 0 && TotalTempB == 0 ){
                        //skip the previous border
                        //check wether they have white neighbour else it is not border

                        //check the new black should be taken or not
                        boolean quitChecking = false;
                        int neighbourXCheck = 0;
                        int neighbourYCheck = 0;
                        while (neighbourXCheck < cellSize){
                            while( neighbourYCheck < cellSize){
                                //System.out.println("Before loop Y neighX " + neighbourX + " neighY " + neighbourY);
                                //check for validity else fill with 0 and ignore
                                //check for validity else fill with the extended from the other side for X side
                                int coordXCheck, coordYCheck;
                                if (((coordX - totalBlur + neighbourXCheck ) < 0 )) {
                                    coordXCheck = ImgWidth - 1;
                                } else if ((coordX + neighbourXCheck ) >= ImgWidth ){
                                    coordXCheck = 0;
                                } else {
                                    coordXCheck =coordX - totalBlur + neighbourXCheck;
                                }
                                //check for validity else fill with the extended from the other side for Y side
                                if((coordY-totalBlur + neighbourYCheck) < 0 ){
                                    coordYCheck = ImgHeight - 1;
                                } else if ((coordY+ neighbourYCheck ) >= ImgHeight ){
                                    coordYCheck = 0;
                                } else {
                                    coordYCheck = coordY-totalBlur + neighbourYCheck;
                                }
                                //change 2,2 to Finish
                                if (neighbourX == 2 && neighbourY == 2){
                                    foundNeighbour = true;
                                    System.out.println("Do not found white");
                                    break;
                                }

                                //Start to convolve image with matrix
                                int pixelNeighbourCheck = tempBitmap.getPixel(coordXCheck,coordYCheck);
                                int TotalTempRCheck = Color.red(pixelNeighbourCheck);
                                int TotalTempGCheck = Color.green(pixelNeighbourCheck);
                                int TotalTempBCheck = Color.blue(pixelNeighbourCheck);
                                neighbourYCheck++;
                                //search for the next black dot
                                if (TotalTempRCheck == 255 && TotalTempGCheck == 255 && TotalTempBCheck == 255 ){
                                    //skip the previous border
                                    //check wether they have white neighbour else it is not border
                                    quitChecking = true;
                                    System.out.println("Find White i:"+ coordX +" j: " + coordY +" neighX " + neighbourX + " neighY " + neighbourY);

                                    foundNeighbour = true;
                                    break;
                                }

                            }
                            neighbourXCheck++;
                            neighbourYCheck = 0;
                            if(foundNeighbour){
                                foundNeighbour = false;
                                break;
                            }
                        }

                        if(quitChecking){
                            //found the white so it is border take and color
                            i = coordX;
                            j = coordY;
                            counterString++;
                            Color NewColour = new Color();
                            int pixel = NewColour.rgb(255, 0, 0);
                            System.out.println("Border i:"+i +" j: " +j +" neighX " + neighbourX + " neighY " + neighbourY);
                            tempBitmap.setPixel(i, j, pixel);
                            stringCode[counterString] = stringValue;
                            foundNeighbour = true;
                        }

                        skipFirst = false;
                        break;
                    }
                    //stop if end in start
                    if (i == startX && j == startY && !skipFirst){
                        start = false;
                        System.out.println("Stopped found start");
                    }

                }
                neighbourX++;
                neighbourY = 0;
                if(foundNeighbour){
                    foundNeighbour = false;
                    break;
                }
            }
            System.out.println("counter string: " + counterString);
            neighbourX = 0;
            neighbourY = 0;
        }
        Log.d("Finish", "String Code finished");
        String textAllCode = new String();
        for (int l = 0; l < counterString; l++) {
            textAllCode = textAllCode + String.valueOf(stringCode[l]);

        }
        stringCodeText.setText("String code : " + textAllCode);
        imgIdentitas.setImageBitmap(tempBitmap);

    }

    public void RecogPlate(){

        final String ANDROID_DATA_DIR = this.getApplicationInfo().dataDir;
        final String openAlprConfFile = ANDROID_DATA_DIR + File.separatorChar + "runtime_data" + File.separatorChar + "openalpr.conf";

        String result = OpenALPR.Factory.create(this, ANDROID_DATA_DIR).recognizeWithCountryRegionNConfig("eu", "", _selectedImageUri.getPath(), openAlprConfFile, 10);
        Gson g = new Gson();
        Map<String, Object>  currJSON = g.fromJson(result, Map.class);
        //Log.d("result", result);
        String plateNum = ((Map) ((List) currJSON.get("results")).get(0)).get("plate").toString();
        stringCodeText.setText(plateNum);
        //stringCodeText.setText(result);
    }
    public void skeletonizeCode() {
        Bitmap tempBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resultBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();

        grid = new char[ImgWidth][ImgHeight];
        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                int pixelNeighbour = tempBitmap.getPixel(i, j);
                int TotalTempR = Color.red(pixelNeighbour);
                if (TotalTempR != 0) {
                    grid[i][j] = ' ';
                } else {
                    grid[i][j] = '1';
                }
                System.out.print(grid[i][j] + " ");
            }
            System.out.println();
        }

        thinImage();

        for (int i = 0; i < ImgWidth; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                if (grid[i][j] == '1'){
                    Color NewColour = new Color();
                    int pixel = NewColour.rgb(0, 0, 0);
                    resultBitmap.setPixel(i,j, pixel);
                } else{
                    Color NewColour = new Color();
                    int pixel = NewColour.rgb(255, 255, 255);
                    resultBitmap.setPixel(i,j, pixel);
                }

            }
        }


        Log.d("Finish", "Skeletonize finished");
        imgIdentitas.setImageBitmap(resultBitmap);
    }

    static void thinImage() {
        boolean firstStep = false;
        boolean hasChanged;

        do {
            hasChanged = false;
            firstStep = !firstStep;

            for (int r = 1; r < grid.length - 1; r++) {
                for (int c = 1; c < grid[0].length - 1; c++) {

                    if (grid[r][c] != '1')
                        continue;

                    int nn = numNeighbors(r, c);
                    if (nn < 2 || nn > 6)
                        continue;

                    if (numTransitions(r, c) != 1)
                        continue;

                    if (!atLeastOneIsWhite(r, c, firstStep ? 0 : 1))
                        continue;

                    toWhite.add(new Point(c, r));
                    hasChanged = true;
                }
            }

            for (Point p : toWhite)
                grid[p.y][p.x] = ' ';
            toWhite.clear();

        } while (firstStep || hasChanged);

    }

    static int numNeighbors(int r, int c) {
        int count = 0;
        for (int i = 0; i < nbrs.length - 1; i++)
            if (grid[r + nbrs[i][1]][c + nbrs[i][0]] == '1')
                count++;
        return count;
    }

    static int numTransitions(int r, int c) {
        int count = 0;
        for (int i = 0; i < nbrs.length - 1; i++)
            if (grid[r + nbrs[i][1]][c + nbrs[i][0]] == ' ') {
                if (grid[r + nbrs[i + 1][1]][c + nbrs[i + 1][0]] == '1')
                    count++;
            }
        return count;
    }

    static boolean atLeastOneIsWhite(int r, int c, int step) {
        int count = 0;
        int[][] group = nbrGroups[step];
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < group[i].length; j++) {
                int[] nbr = nbrs[group[i][j]];
                if (grid[r + nbr[1]][c + nbr[0]] == ' ') {
                    count++;
                    break;
                }
            }
        return count > 1;
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

    public void FindFace(){
        Bitmap tempBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resultBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        //paint.setStyle(Paint.Style.STROKE);
        int borderWidth = 0, borderHeight = 0, counterFace = 0;
        Canvas myCanvas = new Canvas(resultBitmap);
        boolean first = true;
        for (int i = 0; i < ImgWidth ; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                int pixel = tempBitmap.getPixel(i, j);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                int a = 0;
                if (isSkinYCbCr(r,g,b)) {
                    a+=1;
                }
                if (isSkinRGB(r, g, b)) {
                    a+=1;
                }
                if (isSkinHSI(r, g, b)) {
                    a+=1;
                }
                if (a==3) { // if all the methods respond "true", mark the pixel
                    Color NewColour = new Color();
                    pixel = NewColour.rgb(0, 255, 0);

                    resultBitmap.setPixel(i, j, pixel);
                    if (i > borderWidth + 70 && j < (ImgHeight/2 - 20)){
                        first = true;
                    }
                    if (first) {
                        first = false;
                        borderWidth = i + 70;
                        if (borderWidth > ImgWidth){
                            borderWidth = ImgWidth - 1;
                        }
                        borderHeight = j + 80;
                        if (borderHeight > ImgHeight){
                            borderHeight = ImgHeight - 1;
                        }
                        myCanvas.drawRect(i, j, borderWidth, borderHeight, paint);
                        counterFace++;
                    }
                }
            }
        }
        Log.d("Finish", "FindFace finished");
        imgIdentitas.setImageBitmap(resultBitmap);
        stringCodeText.setText("Jumlah Wajah : " + counterFace);
    }

    public void Fourier(){
        Bitmap tempBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resultBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int ImgWidth = tempBitmap.getWidth();
        int ImgHeight = tempBitmap.getHeight();
        Paint paint = new Paint();
        Complex[] pixels = new Complex[ImgHeight * ImgWidth];
        paint.setColor(Color.RED);
        FFT transformer = new FFT();
        //paint.setStyle(Paint.Style.STROKE);
        int borderWidth = 0, borderHeight = 0, counterFace = 0;
        Canvas myCanvas = new Canvas(resultBitmap);
        boolean first = true;
        for (int i = 0; i < ImgWidth ; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                int pixel = tempBitmap.getPixel(i, j);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                pixels[i * ImgHeight + j] = new Complex((int) (0.299 * r + 0.587 * g + 0.114 * b), 0);
            }
        }
        Complex[] frequency_pixels = transformer.fft(pixels);
        Complex[] convolved_pixels = transformer.cconvolve(frequency_pixels, frequency_pixels);
        Complex[] z_picture = transformer.ifft(frequency_pixels);
        int max_magnitude = -9999999;
        int max_magnitude_z = -9999999;
        //find the max magnitude of power
        for (int i = 0; i < ImgWidth ; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                //count the freq image
                double real = convolved_pixels[i * ImgWidth + j].re();
                double imaginer = convolved_pixels[i * ImgWidth + j].im();
                int magnitude = (int) (Math.sqrt(real * real + imaginer * imaginer) );
                if (magnitude > max_magnitude){
                    max_magnitude = magnitude;
                }
            }
        }
        boolean isUsingFilter = false;
        int limitLow = 0;
        int limitHigh = 99999;
        if (lowPass.getText().toString().equals("low:") || highPass.getText().toString().equals("high:")){
            stringCodeText.setText("No filter");
        } else {
            isUsingFilter = true;
            limitLow = Integer.valueOf(lowPass.getText().toString());
            limitHigh = Integer.valueOf(highPass.getText().toString());
        }
        for (int i = 0; i < ImgWidth ; i++) {
            for (int j = 0; j < ImgHeight; j++) {
                Color NewColour = new Color();
                //draw the freq image
                double real = convolved_pixels[i * ImgWidth + j].re();
                double imaginer = convolved_pixels[i * ImgWidth + j].im();
                double magnitude =  (Math.sqrt(real * real + imaginer * imaginer) );
                int color = (int) magnitude/max_magnitude * 255;
                int pixel = NewColour.rgb(color , color, color);
                resultBitmap.setPixel(i, j, pixel);

                //draw the z image


                real = z_picture[i * ImgWidth + j].re();
                imaginer = z_picture[i * ImgWidth + j].im();
                magnitude =  (Math.sqrt(real * real + imaginer * imaginer) );
                if(isUsingFilter) {
                    if (real > limitLow && real < limitHigh) {
                        color = (int) magnitude;
                    } else {
                        color = 0;
                    }
                } else {
                    color = (int) magnitude;
                }
                pixel = NewColour.rgb(color , color, color);
                tempBitmap.setPixel(i, j, pixel);
            }
        }

        Log.d("Finish", "FindFace finished");
        imgIdentitas.setImageBitmap(tempBitmap);
        imgFreqFourier.setImageBitmap(resultBitmap);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == this.RESULT_OK && data != null) {
            Log.d("Status", "result ok");
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

            } else if (requestCode == SELECT_FILE) {
                _selectedImageUri = data.getData();
                InputStream image_stream = null;
                try {
                    image_stream = getContentResolver().openInputStream(_selectedImageUri);
                    filename = _selectedImageUri.toString();
                    _bitmap = BitmapFactory.decodeStream(image_stream);

                    Bitmap _processed = _bitmap;

                    int nh = (int) (_bitmap.getHeight() * (1024.0 / _processed.getWidth()));
                    Bitmap scaled = Bitmap.createScaledBitmap(_processed, 1024, nh, true);

//                    imgIdentitas.setImageBitmap(Utils.decodeSampledBitmapFromResource(outputURI.getPath(), 300, 300));
                    imgIdentitas.setImageBitmap(scaled);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

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

    // We are assuming here that most pixels in the random image
    // are non-skin pixels. So let's try to get out of the function
    // as fast as possible to avoid slowest operations, min and max.
    // code from http://popscan.blogspot.co.id/2012/08/skin-detection-in-digital-images.html

    public boolean isSkinRGB(int r, int g, int b) {
        // first easiest comparisons
        if ( (r<95) | (g<40) | (b<20) | (r<g) | (r<b) ) {
            return false; // no match, stop function here
        }
        int d = r-g;
        if ( -15<d && d<15) {
            return false; // no match, stop function here
        }
        // we have left most time consuming operation last
        // hopefully most of the time we are not reaching this point
        int max = Math.max(Math.max(r, g), b);
        int min = Math.min(Math.min(r, g), b);
        if ((max-min)<15) {
            // this is the worst case
            return false; // no match, stop function
        }
        // all comparisons passed
        return true;
    }

    public boolean isSkinHSI(int r, int g, int b) {
        HSI hsi = new HSI();
        hsi = hsi.RGB2HSI(r,g,b);
        if (hsi.h<25||hsi.h>230) {
            return true;
        } else
            return false;
    }

    public static boolean isSkinYCbCr(int r, int g, int b) {
        YCrCb result = YCrCb.RGB2YCbCr(r, g, b);
        double cr = result.Cr;
        double cb = result.Cb;
        if (cr >= ((1.5862*cb) + 20)) {
            return false;
        }
        if (cr <= ((0.3448*cb) + 76.2069)) {
            return false;
        }
        if (cr <= ((-4.5652*cb) + 234.5652)) {
            return false;
        }
        if (cr >= ((-1.15*cb) + 301.75)) {
            return false;
        }
        if (cr >= ((-2.2857*cb) + 432.85)) {
            return false;
        }
        // all comparisons passed
        return true;
    }
}

class HSI {
    double h = 0.0;
    double s = 0.0;
    double i = 0.0;

    public HSI RGB2HSI(int R, int G, int B) {
        HSI result = new HSI();
        result.i = (R+G+B)/3.0;    // we have calculated I!
        if (R==G&&G==B) {
            return result;    // return result with h=0.0 and s=0.0
        }
        double r = R/i;            // normalize R
        double g = G/i;            // normalize G
        double b = B/i;            // normalize B
        double w = 0.5*(R-G+R-B) / Math.sqrt((R-G)*(R-G)+(R-B)*(G-B));
        if (w>1) w = 1.0;       // clip input for acos to -1 <= w <= 1
        if (w<-1) w = -1.0;     // clip input for acos to -1 <= w <= 1
        result.h = Math.acos(w);   // the value is 0 <= h <= Math.PI
        if (B>G) {
            result.h = 2*Math.PI - result.h;
        }
        // finally the last component s
        result.s = 1 - (3 * (Math.min(Math.min(r, g), b) )  );
        return result;

    }

}

class YCrCb {

    double Y = 0.0;
    double Cr = 0.0;
    double Cb = 0.0;

    public static YCrCb RGB2YCbCr(double R, double G, double B) {
        YCrCb result = new YCrCb();
        result.Y  = (0.257*R) + (0.504*G) + (0.098*B) + 16;
        result.Cr = (0.439*R) - (0.368*G) - (0.071*B) + 128;
        result.Cb = -(0.148*R)- (0.291*G) + (0.439*B) + 128;
        return result;
    }
}

