package com.example.amwadatk.postit;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.AnalysisResult;
import com.microsoft.projectoxford.vision.contract.Caption;
import com.microsoft.projectoxford.vision.contract.Description;
import com.microsoft.projectoxford.vision.contract.Tag;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;


public class TagGenerator extends Activity {

    ImageView tagImage;
    EditText tags;
    String path,tagstext;
    LinearLayout linearLayout;
    private Bitmap mBitmap;
    private VisionServiceClient client;
    Button shareimage;
    RadioButton[] rb;
    int currentquote ;
        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_generator);
        StrictMode.VmPolicy.Builder newbuilder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(newbuilder.build());
        path = getIntent().getStringExtra("path");
        Log.d("MSG",getIntent().getStringExtra("path"));
        tagImage = findViewById(R.id.tagImage);
        tags = findViewById(R.id.tags);
        currentquote =0;
        tagstext="";
        tags.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s)
            {
                    String changed = tags.getText().toString();
                    if(changed.contains(tagstext)==false)
                    {
                        RadioButton  rb = findViewById(currentquote);
                        tagstext = tagstext.substring(0,changed.indexOf(rb.getText().toString()));
                    }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {

            }
        });
        linearLayout = findViewById(R.id.tags_quotes);
        LinearLayout layout = new LinearLayout(this);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        rb = new RadioButton[5];
        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.VERTICAL);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton rb=(RadioButton)findViewById(checkedId);
                currentquote = checkedId;
                tags.setText(tagstext + " " +rb.getText().toString());
            }
        });
        shareimage = findViewById(R.id.shareimage);
        View.OnClickListener shareimg = new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                final Intent intent = new Intent(     android.content.Intent.ACTION_SEND);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
                intent.setType("image/jpeg");
                ClipboardManager myClipboard;
                myClipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                ClipData myClip;
                String text = tags.getText().toString();
                myClip = ClipData.newPlainText("text", text);
                myClipboard.setPrimaryClip(myClip);
                startActivity(intent);
            }
        };
        shareimage.setOnClickListener(shareimg);
        for(int i=0; i<5; i++)
        {
            rb[i]  = new RadioButton(this);
            rb[i].setText("Quote" + i);
            rb[i].setId(i);
            rg.addView(rb[i]);
        }
        layout.addView(rg);//you add the whole
        linearLayout.addView(layout,1);
        Glide.with(this).load(path)
                .into(tagImage);


        if (client==null){
            client = new VisionServiceRestClient("84b235ebbcbe455f9173f10630cc15aa", "https://westcentralus.api.cognitive.microsoft.com/vision/v1.0");
        }
        try {
            new doRequest().execute();
        } catch (Exception e) {

        }
    }


    private String process() throws VisionServiceException, IOException {
        Gson gson = new Gson();
        String[] features = {"Tags","Description"};
        String[] details = {};

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap = BitmapFactory.decodeFile(path);
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 25, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        AnalysisResult v = this.client.analyzeImage(inputStream, features, details);

        String result = gson.toJson(v);
        Log.d("result", result);

        try {
            final ExifInterface exifInterface = new ExifInterface(path);
            float[] latLong = new float[2];
            if (exifInterface.getLatLong(latLong)) {
                Log.d("LAT",String.valueOf(latLong[0]));
                Log.d("LONG",String.valueOf(latLong[1]));
            }
        } catch (IOException e) {
            Log.d("ERROR","Couldn't read exif info: " + e.getLocalizedMessage());
        }

        return result;
    }

    private class doRequest extends AsyncTask<String, String, String> {
        // Store error message
        private Exception e = null;
        private ProgressDialog dialog;

        public doRequest() {
            dialog = new ProgressDialog(TagGenerator.this);
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Generating Tags, please wait.");
            dialog.show();
        }

        @Override
        protected String doInBackground(String... args) {
            try {
                return process();
            } catch (Exception e) {
                this.e = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String data) {
            super.onPostExecute(data);

            if (e != null) {

                tags.setText("Error: " + e.getMessage());
                this.e = null;
            } else {
                Gson gson = new Gson();
                AnalysisResult result = gson.fromJson(data, AnalysisResult.class);


                for (Tag tag: result.tags) {
                    tags.append("#" + tag.name + " " );
                    //write logic for captions
                }
                tags.append("\n");
                for (Caption d : result.description.captions){
                    tags.append(" " + d.text + " ");
                }

                tags.append("\n");
                int faceCount = 0;
                tagstext =tags.getText().toString();
            }
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

        }
    }

}
