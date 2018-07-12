package com.example.amwadatk.postit;

import android.media.Image;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class TagGenerator extends Activity {

    ImageView tagImage;
    EditText tags;
    String path;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_generator);
        path = getIntent().getStringExtra("path");
        Log.d("MSG",getIntent().getStringExtra("path"));
        tagImage = findViewById(R.id.tagImage);
        tags = findViewById(R.id.tags);
        Glide.with(this).load(path)
                .into(tagImage);
        
    }

}
