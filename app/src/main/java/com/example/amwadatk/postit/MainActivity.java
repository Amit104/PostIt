package com.example.amwadatk.postit;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    Button ChoosePhoto;
    ImageView display;
    ArrayList<Uri> imageUri = new ArrayList<Uri>();

    private static final int PICK_FROM_GALLERY = 1;

    private void displayImage(String path)
    {
        display.setImageBitmap(BitmapFactory.decodeFile(path));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ChoosePhoto = findViewById(R.id.chooseButton);
        display = findViewById(R.id.display);
        ChoosePhoto.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick (View v){
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PICK_FROM_GALLERY);
                    } else {

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
                Log.d("Date",today);
                Cursor cursor = getContentResolver().query(uri, new String[] {MediaStore.Images.Media.DATA}, null, null, MediaStore.Images.Media.DATE_ADDED + " ASC");
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        if(Uri.parse(cursor.getString(0)).toString().contains(today))
                        {
                            imageUri.add(Uri.parse(cursor.getString(0)));
                        }
                    }
                    cursor.close();
                }
                for(Uri i : imageUri)
                    Log.d("Photo",i.toString());
                displayImage(imageUri.get(0).toString());
            }
        });
    }
}
