package com.example.amwadatk.postit;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
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
import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class TagGenerator extends Activity {

    ImageView tagImage;
    EditText tags;
    String path;
    LinearLayout linearLayout;
    private Bitmap mBitmap;
    private VisionServiceClient client;
    String cityName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_generator);
        path = getIntent().getStringExtra("path");
        Log.d("MSG",getIntent().getStringExtra("path"));
        tagImage = findViewById(R.id.tagImage);
        linearLayout = findViewById(R.id.tags_quotes);
        LinearLayout layout = new LinearLayout(this);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tags = findViewById(R.id.tags);
        final RadioButton[] rb = new RadioButton[5];
        RadioGroup rg = new RadioGroup(this); //create the RadioGroup
        rg.setOrientation(RadioGroup.VERTICAL);//or RadioGroup.VERTICAL
        for(int i=0; i<5; i++){
            rb[i]  = new RadioButton(this);
            rb[i].setText("Quote" + i);
            rb[i].setId(i + 100);
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
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latLong[0], latLong[1], 1);
                cityName = "#" + addresses.get(0).getLocality();
                Log.d("CITY",cityName);
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

                tags.append(cityName+" ");
                for (Tag tag: result.tags) {
                    tags.append("#" + tag.name + " " );
                    //write logic for captions
                }
                tags.append("\n");
                int faceCount = 0;
            }
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

        }
    }

}
