package com.pengcit.simplecamera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * Created by Satria on 10/5/2016.
 */
public class FaceRecog extends AppCompatActivity {
    public static Bitmap FaceRecogBitmap;
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
        ImageView imgFace = (ImageView)findViewById(R.id.imgFace);
        if (imgFace != null){
            imgFace.setImageBitmap(FaceRecogBitmap);
        }

        Button detectbut = (Button)findViewById(R.id.detectbutton);
        //Set spinner
        Spinner spinner = (Spinner) findViewById(R.id.spinnerFilter);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.filterChoice_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);



        detectbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FilterImage();
            }
        });
    }

    public void FilterImage(){
        Toast.makeText(getApplicationContext(), "Detect Face", Toast.LENGTH_SHORT).show();
    }
}
