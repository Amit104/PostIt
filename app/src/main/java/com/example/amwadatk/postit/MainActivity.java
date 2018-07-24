package com.example.amwadatk.postit;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.StringSearch;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceAttribute;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.microsoft.projectoxford.face.rest.ClientException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    Button ChoosePhoto,Process;
    ExpandableHeightGridView singlePics,groupPics,viewPics,allPics;
    int counter;
    String userfaceid="";
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
    HashMap<String,Pair<Double,List<String>>> faceidall = new HashMap<>();
    private static final int PICK_FROM_GALLERY = 1;
    private final String apiEndpoint = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0";
    private final String subscriptionKey = "34ab0e4557724ec8a90ef592bf76a3e5";
    private final FaceServiceClient faceServiceClient =
            new FaceServiceRestClient(apiEndpoint, subscriptionKey);
    SharedPreferences sharedpreferences ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        sharedpreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(sharedpreferences.contains("face"))
        {
            //Toast.makeText(getApplicationContext(),getDefaults("faceid",getApplicationContext()),Toast.LENGTH_LONG).show();
            String lastupdated= sharedpreferences.getString("facedate","");
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd hhmmss");
            try {
                Date updated=formatter.parse(lastupdated);
                Date currenttime = new Date();
                long diff = currenttime.getTime() - updated.getTime();
                long diffhours = diff  /(60*60*1000) ;
                if(diffhours>23)
                {
                    Log.d("API from home","API home");
                    new TagGenerator().detectFaces();
                }
                else
                {
                    userfaceid = getDefaults("faceid",getApplicationContext());
                    Log.d("FACEID",userfaceid);
                }

            } catch (ParseException e) {
                e.printStackTrace();

            }
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
    public static String getDefaults(String key, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(key, null);
    }

    // Detect faces by uploading a face image.
    // Frame faces after detection.
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
                        Pair<Double,List<String>> facescore;
                        List<String> facelist = new ArrayList<String>();
                        Log.d("FaceResult",result.length+"");
                        if(result!= null) {
                            int baseArea = Getarea(result[0].faceRectangle.width, result[0].faceRectangle.height);
                            int fc = 0;
                            for (Face face : result) {
                                FaceRectangle faceRectangle = face.faceRectangle;
                                FaceAttribute faceAttribute = face.faceAttributes;
                                int size = Getarea(faceRectangle.width, faceRectangle.height);
                                if (size > 0.1 * baseArea) {
                                    fc++;
                                    facelist.add(face.faceId.toString());
                                }
                            }
                            facescore = new Pair<>(score, facelist);
                            faceidall.put(path, facescore);
                        }
                        Log.d("Scores : ", String.valueOf(score));
                        addPair(0,new Pair<String, Double>(path,score));
                        imageUriRanked.add(Uri.parse(path));

                        // add conditions here
                        if(result==null || result.length==0) {
                            addPair(3,new Pair<String, Double>(path,score));
                            imageUriView.add(Uri.parse(path));
                        } else {
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
                            if(!userfaceid.equals("") ) { //If the face is already set check for all photos to improve score
                                scoreList.clear();
                                scoreListSingle.clear();
                                scoreListGroup.clear();
                                for (Map.Entry<String, Pair<Double,List<String>>> entry : faceidall.entrySet()) {
                                    String urlpath = entry.getKey();
                                    facescore = entry.getValue();
                                    facelist = facescore.second;
                                    Double newscore = facescore.first;
                                    for (String faceid : facelist) {

                                        AsyncTask<String, String, String> verifyTask =
                                                new AsyncTask<String, String, String>() {
                                                    String exceptionMessage = "";

                                                    @Override
                                                    protected String doInBackground(String... params) {
                                                        boolean samePerson = false;
                                                        try {
                                                            samePerson = faceServiceClient.verify(UUID.fromString(params[0]), UUID.fromString(params[1])).isIdentical;
                                                        } catch (ClientException e) {
                                                            e.printStackTrace();
                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                        }
                                                        Log.d("API VERIFY", "Worked!");
                                                        if(samePerson)
                                                            return "true";
                                                        else
                                                            return "false";
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
                                                    protected void onPostExecute(String result) {
                                                        //TODO: update face frames

                                                    }
                                                };
                                        boolean samePerson = false;
                                        try {
                                            samePerson = verifyTask.execute(faceid, userfaceid).get().equals("true")?true:false;
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        } catch (ExecutionException e) {
                                            e.printStackTrace();
                                        }

                                        Log.d("SAME", String.valueOf(samePerson));
                                        if (samePerson)
                                        {
                                            newscore += 2.0 / facelist.size();
                                            Log.d("NEW", String.valueOf(newscore));
                                        }
                                    }
                                    scoreList.add(new Pair<String, Double>(urlpath,newscore));
                                    if(facelist.size()==1)
                                        scoreListSingle.add(new Pair<String, Double>(urlpath,newscore));
                                    if(facelist.size()>1)
                                        scoreListGroup.add(new Pair<String, Double>(path,newscore));
                                }
                                //Sorting Logic
                                scoreList.sort(new Comparator<Pair<String, Double>>() {

                                    @Override
                                    public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                                        if (o1.second > o2.second) {
                                            return -1;
                                        } else if (o1.second.equals(o2.second)) {
                                            return 0; // You can change this to make it then look at the
                                            //words alphabetical order
                                        } else {
                                            return 1;
                                        }
                                    }
                                });
                                scoreListSingle.sort(new Comparator<Pair<String, Double>>() {

                                    @Override
                                    public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                                        if (o1.second > o2.second) {
                                            return -1;
                                        } else if (o1.second.equals(o2.second)) {
                                            return 0; // You can change this to make it then look at the
                                            //words alphabetical order
                                        } else {
                                            return 1;
                                        }
                                    }
                                });
                                scoreListGroup.sort(new Comparator<Pair<String, Double>>() {

                                    @Override
                                    public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                                        if (o1.second > o2.second) {
                                            return -1;
                                        } else if (o1.second.equals(o2.second)) {
                                            return 0; // You can change this to make it then look at the
                                            //words alphabetical order
                                        } else {
                                            return 1;
                                        }
                                    }
                                });

                                imageUriRanked.clear();
                                imageUriSingle.clear();
                                imageUriGroup.clear();
                                for(Pair<String, Double> res : scoreList)
                                {
                                    Log.d("SCORE", String.valueOf(res.second) + " for " + res.first);
                                    imageUriRanked.add(Uri.parse(res.first));
                                }

                                for(Pair<String, Double> res : scoreListSingle)
                                {
                                    Log.d("SCORE", String.valueOf(res.second) + " for " + res.first);
                                    imageUriSingle.add(Uri.parse(res.first));
                                }

                                for(Pair<String, Double> res : scoreListGroup)
                                {
                                    Log.d("SCORE", String.valueOf(res.second) + " for " + res.first);
                                    imageUriGroup.add(Uri.parse(res.first));
                                }
                            }
                            progressBar.setVisibility(View.GONE);

                        }


                            // logic for ranking and captioning
                         /*   scoreList.sort(new Comparator<Pair<String, Double>>() {

                                @Override
                                public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                                    if (o1.second > o2.second) {
                                        return -1;
                                    } else if (o1.second.equals(o2.second)) {
                                        return 0; // You can change this to make it then look at the
                                        //words alphabetical order
                                    } else {
                                        return 1;
                                    }
                                }
                            });
                            scoreListSingle.sort(new Comparator<Pair<String, Double>>() {

                                @Override
                                public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                                    if (o1.second > o2.second) {
                                        return -1;
                                    } else if (o1.second.equals(o2.second)) {
                                        return 0; // You can change this to make it then look at the
                                        //words alphabetical order
                                    } else {
                                        return 1;
                                    }
                                }
                            });
                            scoreListGroup.sort(new Comparator<Pair<String, Double>>() {

                                @Override
                                public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                                    if (o1.second > o2.second) {
                                        return -1;
                                    } else if (o1.second.equals(o2.second)) {
                                        return 0; // You can change this to make it then look at the
                                        //words alphabetical order
                                    } else {
                                        return 1;
                                    }
                                }
                            });
                            scoreListView.sort(new Comparator<Pair<String, Double>>() {

                                @Override
                                public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                                    if (o1.second > o2.second) {
                                        return -1;
                                    } else if (o1.second.equals(o2.second)) {
                                        return 0; // You can change this to make it then look at the
                                        //words alphabetical order
                                    } else {
                                        return 1;
                                    }
                                }
                            });
                            imageUri.clear();
                            imageUriSingle.clear();
                            imageUriGroup.clear();
                            imageUriView.clear();

                            for(Pair<String, Double> res : scoreList)
                            {
                                Log.d("SCORE", String.valueOf(res.second) + " for " + res.first);
                                imageUri.add(Uri.parse(res.first));
                            }

                            for(Pair<String, Double> res : scoreListSingle)
                            {
                                Log.d("SCORE", String.valueOf(res.second) + " for " + res.first);
                                imageUriSingle.add(Uri.parse(res.first));
                            }

                            for(Pair<String, Double> res : scoreListGroup)
                            {
                                Log.d("SCORE", String.valueOf(res.second) + " for " + res.first);
                                imageUriGroup.add(Uri.parse(res.first));
                            }

                            for(Pair<String, Double> res : scoreListView)
                            {
                                Log.d("SCORE", String.valueOf(res.second) + " for " + res.first);
                                imageUriView.add(Uri.parse(res.first));
                            }*/

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

    class ImageAdapter extends BaseAdapter {

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
                    String urlpath = imageUrit.get(position).toString();
                    i.putExtra("path",urlpath);
                    if(faceidall!= null && faceidall.size()!=0) { //if faces are ranked
                        i.putExtra("imagetype", faceidall.get(urlpath).second.size() + "");
                        if (faceidall.get(urlpath).second.size() == 1)
                            i.putExtra("faceid", faceidall.get(urlpath).second.get(0));
                        else
                            i.putExtra("faceid", "0");
                    }
                    startActivity(i);
                }
            });
            return picturesView;
        }
    }
}