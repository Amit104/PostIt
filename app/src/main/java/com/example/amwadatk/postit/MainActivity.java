package com.example.amwadatk.postit;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.contract.*;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    Button ChoosePhoto,Process;
    GridView display;
    ArrayList<Uri> imageUri = new ArrayList<Uri>();

    private static final int PICK_FROM_GALLERY = 1;
    private final String apiEndpoint = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0";
    private final String subscriptionKey = "d7502ded756743e5bd3c20227b188a44";

    private final FaceServiceClient faceServiceClient =
            new FaceServiceRestClient(apiEndpoint, subscriptionKey);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ChoosePhoto = findViewById(R.id.chooseButton);
        Process = findViewById(R.id.process);
        display = findViewById(R.id.display);

        ChoosePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PICK_FROM_GALLERY);
                    } else {

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                imageUri.clear();
                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
                Log.d("Date", today);
                Cursor cursor = getContentResolver().query(uri, new String[]{MediaStore.Images.Media.DATA}, null, null, MediaStore.Images.Media.DATE_ADDED + " ASC");
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        if (Uri.parse(cursor.getString(0)).toString().contains(today)) {
                            imageUri.add(Uri.parse(cursor.getString(0)));
                        }
                    }
                    cursor.close();
                }
                for (Uri i : imageUri)
                    Log.d("Photo", i.toString());
                display.setAdapter(new ImageAdapter(MainActivity.this));
            }
        });

        Process.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(imageUri.size()==0)
                {
                    Toast.makeText(MainActivity.this,"No Images available!!", Toast.LENGTH_LONG).show();
                }
                else
                {
                    for(Uri i : imageUri)
                    {
                        Bitmap image = BitmapFactory.decodeFile(i.toString());
                        detectAndFrame(image);
                        Log.d("API","Worked for " + i.toString());
                    }

                }
            }
        });
    }

    // Detect faces by uploading a face image.
    // Frame faces after detection.
    private void detectAndFrame(final Bitmap imageBitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 25, outputStream);
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());
        /*FileOutputStream out = null;
        try {
            out = new FileOutputStream("/storage/emulated/0/DCIM/output.jpg");
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 30, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
        Log.d("API","[START]");
        AsyncTask<InputStream, String, Face[]> detectTask =
                new AsyncTask<InputStream, String, Face[]>() {
                    String exceptionMessage = "";

                    @Override
                    protected Face[] doInBackground(InputStream... params) {
                        try {
                            publishProgress("Detecting...");
                            Face[] result = faceServiceClient.detect(
                                    params[0],
                                    true,         // returnFaceId
                                    false,        // returnFaceLandmarks
                                    null          // returnFaceAttributes:
                                /* new FaceServiceClient.FaceAttributeType[] {
                                    FaceServiceClient.FaceAttributeType.Age,
                                    FaceServiceClient.FaceAttributeType.Gender }
                                */
                            );
                            if (result == null){
                                publishProgress(
                                        "Detection Finished. Nothing detected");
                                return null;
                            }
                            publishProgress(String.format(
                                    "Detection Finished. %d face(s) detected",
                                    result.length));
                            Log.d("API","Worked!");
                            return result;
                        } catch (Exception e) {
                            exceptionMessage = String.format(
                                    "Detection failed: %s", e.getMessage());
                            return null;
                        }
                    }

                    @Override
                    protected void onPreExecute() {
                        //TODO: show progress dialog
                    }
                    @Override
                    protected void onProgressUpdate(String... progress) {
                        //TODO: update progress
                    }
                    @Override
                    protected void onPostExecute(Face[] result) {
                        //TODO: update face frames
                        Log.d("Results",result.toString());
                    }
                };

        detectTask.execute(inputStream);
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }})
                .create().show();
    }

    private class ImageAdapter extends BaseAdapter {

        private Activity context;
        public ImageAdapter(Activity localContext) {
            context = localContext;
        }

        public int getCount() {
            return imageUri.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(final int position, View convertView,
                            ViewGroup parent) {
            ImageView picturesView;
            if (convertView == null) {
                picturesView = new ImageView(context);
                picturesView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                picturesView
                        .setLayoutParams(new GridView.LayoutParams(270, 270));

            } else {
                picturesView = (ImageView) convertView;
            }

            Glide.with(context).load(imageUri.get(position).toString())
                    .into(picturesView);

            return picturesView;
        }
    }
}