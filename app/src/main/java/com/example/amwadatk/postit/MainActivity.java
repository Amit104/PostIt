package com.example.amwadatk.postit;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.contract.*;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.microsoft.projectoxford.face.rest.ClientException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    Button ChoosePhoto,Process;
    ExpandableHeightGridView singlePics,groupPics,viewPics,allPics;
    int counter;
    ProgressBar progressBar;
    ArrayList<Uri> imageUri = new ArrayList<Uri>();
    ArrayList<Uri> imageUriRanked = new ArrayList<Uri>();
    ArrayList< Pair<String, Double> > scoreList = new ArrayList<>();
    ArrayList<Uri> imageUriSingle = new ArrayList<Uri>();
    ArrayList< Pair<String, Double> > scoreListSingle = new ArrayList<>();
    ArrayList<Uri> imageUriGroup = new ArrayList<Uri>();
    ArrayList< Pair<String, Double> > scoreListGroup = new ArrayList<>();
    ArrayList<Uri> imageUriView = new ArrayList<Uri>();
    ArrayList< Pair<String, Double> > scoreListView = new ArrayList<>();
    private static final int PICK_FROM_GALLERY = 1;
    private final String apiEndpoint = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0";
    private final String subscriptionKey = "34ab0e4557724ec8a90ef592bf76a3e5";
    private final FaceServiceClient faceServiceClient =
            new FaceServiceRestClient(apiEndpoint, subscriptionKey);
    DatabaseHandler db;
    SharedPreferences sharedpreferences;
    HashMap<String,Face[]> faceidall = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ChoosePhoto = findViewById(R.id.chooseButton);
        Process = findViewById(R.id.process);
        ChoosePhoto.setEnabled(true);
        progressBar = findViewById(R.id.progressbar);
        progressBar.setVisibility(View.GONE);
        singlePics = findViewById(R.id.displaySingle);
        groupPics = findViewById(R.id.displayGroup);
        viewPics = findViewById(R.id.displayView);
        allPics = findViewById(R.id.displayAll);
        singlePics.setExpanded(true);
        groupPics.setExpanded(true);
        viewPics.setExpanded(true);
        allPics.setExpanded(true);

        db = new DatabaseHandler(this);
        sharedpreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(!sharedpreferences.contains("personGroupId"))
        {
            String uniqueID = UUID.randomUUID().toString();
            Log.d("unique Id",uniqueID);
            CreateGroup(uniqueID);
        }


        ChoosePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PICK_FROM_GALLERY);
                    } else {
                        imageUri.clear();
                        imageUriSingle.clear();
                        imageUriGroup.clear();
                        imageUriView.clear();
                        imageUriRanked.clear();

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
                        allPics.setAdapter(new ImageAdapter(MainActivity.this,imageUri));
                        singlePics.setAdapter(new ImageAdapter(MainActivity.this, imageUriSingle));
                        groupPics.setAdapter(new ImageAdapter(MainActivity.this, imageUriGroup));
                        viewPics.setAdapter(new ImageAdapter(MainActivity.this, imageUriView));
                        Process.setEnabled(true);

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        Process.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                progressBar.setVisibility(View.VISIBLE);
                Process.setEnabled(false);
                ChoosePhoto.setEnabled(false);

                if(imageUri.size()==0)
                {
                    progressBar.setVisibility(View.GONE);
                    Process.setEnabled(true);
                    ChoosePhoto.setEnabled(true);
                    Toast.makeText(MainActivity.this,"No Images available!!", Toast.LENGTH_LONG).show();
                }
                else
                {
                    imageUriRanked.clear();
                    imageUriView.clear();
                    imageUriGroup.clear();
                    imageUriSingle.clear();
                    counter=0;
                    for(Uri i : imageUri)
                    {
                        Bitmap image = BitmapFactory.decodeFile(i.toString());
                        detectAndFrame(image, i.toString());
                    }

                }
            }
        });
    }


    private void detectAndFrame(final Bitmap imageBitmap, final String path) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 30, outputStream);
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());
        FileOutputStream out = null;
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
        }

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
                                    new FaceServiceClient.FaceAttributeType[] {
                                        FaceServiceClient.FaceAttributeType.Age,
                                        FaceServiceClient.FaceAttributeType.Gender,
                                        FaceServiceClient.FaceAttributeType.Smile,
                                        FaceServiceClient.FaceAttributeType.Emotion,
                                        FaceServiceClient.FaceAttributeType.Exposure,
                                        FaceServiceClient.FaceAttributeType.Blur,
                                        FaceServiceClient.FaceAttributeType.Occlusion }

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
                        progressBar.setVisibility(View.VISIBLE);
                    }
                    @Override
                    protected void onProgressUpdate(String... progress) {
                        //TODO: update progress
                    }
                    @Override
                    protected void onPostExecute(Face[] result) {
                        //TODO: update face frames

                        double score = new FaceRanking().rank(result);
                        Log.d("Scores : ", String.valueOf(score));
                        addPair(0,new Pair<String, Double>(path,score));
                        imageUriRanked.add(Uri.parse(path));

                        // add conditions here
                        if(result==null || result.length==0) {
                            addPair(3,new Pair<String, Double>(path,score));
                            imageUriView.add(Uri.parse(path));
                        } else {
                            faceidall.put(path, result);
                            int baseArea = Getarea(result[0].faceRectangle.width, result[0].faceRectangle.height);
                            int fc = 0;
                            for (Face face : result) {

                                FaceRectangle faceRectangle = face.faceRectangle;
                                FaceAttribute faceAttribute = face.faceAttributes;
                                int size = Getarea(faceRectangle.width, faceRectangle.height);
                                if (size > 0.1 * baseArea) {
                                    fc++;
                                }
                            }
                            if(fc==1) {
                                addPair(1,new Pair<String, Double>(path,score));
                                imageUriSingle.add(Uri.parse(path));
                            } else {
                                addPair(2,new Pair<String, Double>(path,score));
                                imageUriGroup.add(Uri.parse(path));
                            }
                        }

                        counter++;
                        if(counter==imageUri.size()) {
                            Process.setEnabled(true);
                            ChoosePhoto.setEnabled(true);
                            for (Pair<String, Double> res : scoreList) {
                                Log.d("SCORE", String.valueOf(res.second) + " for " + res.first);
                            }
                            progressBar.setVisibility(View.GONE);
                        }

                            allPics.setAdapter(new ImageAdapter(MainActivity.this, imageUriRanked));
                            singlePics.setAdapter(new ImageAdapter(MainActivity.this, imageUriSingle));
                            groupPics.setAdapter(new ImageAdapter(MainActivity.this, imageUriGroup));
                            viewPics.setAdapter(new ImageAdapter(MainActivity.this, imageUriView));
                    }
                };

        detectTask.execute(inputStream);
    }

    public void addPair(int group, Pair<String, Double> newitem)
    {
        if(group==0)
        {
            int position= BinaryFit(scoreList,newitem);
            scoreList.add(position,newitem);
        }
        if(group==1)
        {
            int position= BinaryFit(scoreListSingle,newitem);
            scoreListSingle.add(position,newitem);
        }
        if(group==2)
        {
            int position= BinaryFit(scoreListGroup,newitem);
            scoreListGroup.add(position,newitem);
        }
        if(group==3)
        {
            int position= BinaryFit(scoreListView,newitem);
            scoreListView.add(position,newitem);
        }
    }

    public int BinaryFit(ArrayList< Pair<String, Double> > list, Pair<String,Double> newitem)
    {
        if(list.size()==0)
            return 0;
        int left=0,right=list.size()-1,mid=0;
        while(left<right)
        {
            mid=left+(right-left)/2;
            if(list.get(mid).second>=newitem.second)
            {
                right=mid-1;
            }
            else
            {
                left=mid+1;
            }
        }
        return left;
    }
    public int Getarea(int width, int height){
        return width*height;
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

    public static String getDefaults(String key, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(key, null);
    }

    public static void setDefaults(String key, String value, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    private void CreateGroup(final String uniqueId) {

        AsyncTask<String, Void, String> createPersonGroup =
                new AsyncTask<String, Void, String>() {

                    @Override
                    protected String doInBackground(String... params) {
                        try {
                            faceServiceClient.createPersonGroup(params[0], "Persons", "Faces");
                        } catch (ClientException e) {

                        } catch (IOException e) {

                        }
                        return params[0];
                    }


                    @Override
                    protected void onPostExecute(String uid) {
                        progressBar.setVisibility(View.GONE);
                        getGroup(uid);
                    }


                    @Override
                    protected void onPreExecute() {
                        progressBar.setVisibility(View.VISIBLE);
                    }


                    @Override
                    protected void onProgressUpdate(Void... text) {

                    }
                };
        try {
            createPersonGroup.execute(uniqueId).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void getGroup(final String uniqueId) {

        AsyncTask<String, Void, PersonGroup> getPersonGroup =
                new AsyncTask<String, Void, PersonGroup>() {

                    @Override
                    protected PersonGroup doInBackground(String... params) {
                        PersonGroup p = null;
                        try {
                            p = faceServiceClient.getPersonGroup(params[0]);
                        } catch (ClientException e) {

                        } catch (IOException e) {

                        }
                        return p;
                    }


                    @Override
                    protected void onPostExecute(PersonGroup p) {
                        progressBar.setVisibility(View.GONE);
                        if(p!=null && p.personGroupId.equals(uniqueId))
                        {
                            setDefaults("personGroupId",uniqueId,getApplicationContext());
                            Log.d("SUCCESS","Added "+ p.personGroupId);
                        }
                        else
                            Log.d("Error", "PesonGroupID not found");
                    }


                    @Override
                    protected void onPreExecute() {
                        progressBar.setVisibility(View.VISIBLE);
                    }

                    @Override
                    protected void onProgressUpdate(Void... text) {

                    }
                };
        getPersonGroup.execute(uniqueId);
    }

    private class ImageAdapter extends BaseAdapter {

        private Activity context;
        private ArrayList<Uri> imageUrit;

        public ImageAdapter(Activity localContext, ArrayList<Uri> imageUri) {
            context = localContext;
            this.imageUrit = imageUri;
        }

        public int getCount() {
            return imageUrit.size();
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
                        .setLayoutParams(new GridView.LayoutParams(GridView.LayoutParams.WRAP_CONTENT, 500));

            } else {
                picturesView = (ImageView) convertView;
            }

            Glide.with(context).load(imageUrit.get(position).toString())
                    .into(picturesView);
            picturesView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(MainActivity.this,TagGenerator.class);
                    i.putExtra("path",imageUrit.get(position).toString());
                    if(faceidall.containsKey(imageUrit.get(position).toString()))
                    {
                        TagGenerator.faces = faceidall.get(imageUrit.get(position).toString());
                    }
                    else
                    {
                        TagGenerator.faces = null;
                    }
                    startActivity(i);
                }
            });
            return picturesView;
        }
    }
}