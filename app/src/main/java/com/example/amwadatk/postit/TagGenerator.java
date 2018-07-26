package com.example.amwadatk.postit;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.AddPersistedFaceResult;
import com.microsoft.projectoxford.face.contract.Candidate;
import com.microsoft.projectoxford.face.contract.CreatePersonResult;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceAttribute;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.PersonGroup;
import com.microsoft.projectoxford.face.rest.ClientException;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.AnalysisResult;
import com.microsoft.projectoxford.vision.contract.Category;
import com.microsoft.projectoxford.vision.contract.Tag;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


public class TagGenerator extends Activity implements AdapterView.OnItemSelectedListener {

    ImageView tagImage;
    EditText tags;
    String path,tagstext;
    LinearLayout linearLayout;
    private Bitmap mBitmap;
    private VisionServiceClient client;
    String cityName = "",category="Life";
    Button shareimage,refresh;
    RadioButton[] rb;
    RadioGroup rg;
    DatabaseHandler db;
    int currentquote,facecounter=0;
    Spinner dropdown;
    HashMap<String,List<String>> quotesList = new HashMap<>();
    List<String> quoteCat;
    SharedPreferences sharedpreferences;
    public static Face[] faces;
    private final String apiEndpoint = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0";
    private final String subscriptionKey = "34ab0e4557724ec8a90ef592bf76a3e5";
    private final FaceServiceClient faceServiceClient =
            new FaceServiceRestClient(apiEndpoint, subscriptionKey);
    IdentifyResult[] resultsPersons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_generator);
        StrictMode.VmPolicy.Builder newbuilder = new StrictMode.VmPolicy.Builder();
        dropdown = findViewById(R.id.quoteCategories);
        StrictMode.setVmPolicy(newbuilder.build());
        path = getIntent().getStringExtra("path");
        Log.d("MSG",getIntent().getStringExtra("path"));
        tagImage = findViewById(R.id.tagImage);
        tags = findViewById(R.id.tags);
        currentquote =0;
        tagstext="";
        db = new DatabaseHandler(this);
        sharedpreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        try{
            InputStreamReader is = new InputStreamReader(getAssets()
                    .open("quotes.csv"));

            BufferedReader reader = new BufferedReader(is);
            reader.readLine();
            String line;
            quoteCat = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                Log.d("LINES",line.split("\t")[0]);
                String key = line.split("\t")[0];
                String val = line.split("\t")[1];
                List<String> tempList;
                if(quotesList.containsKey(key))
                {
                    tempList = quotesList.get(key);
                }
                else
                {
                    tempList = new ArrayList<>();
                    quoteCat.add(key);
                }
                tempList.add(val);
                quotesList.put(key,tempList);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, quoteCat);
            dropdown.setAdapter(adapter);
            dropdown.setOnItemSelectedListener(this);
        }catch(Exception e){
            e.printStackTrace();
            Toast.makeText(this, "The specified file was not found", Toast.LENGTH_SHORT).show();
        }

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
        rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.VERTICAL);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton rb=(RadioButton)findViewById(checkedId);
                currentquote = checkedId;
                tags.setText(rb.getText().toString()+" "+tagstext);
            }
        });
        shareimage = findViewById(R.id.shareimage);
        refresh = findViewById(R.id.refresh);
        View.OnClickListener shareimg = new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(null == faces) {
                    detectFaces();
                }
                else
                {
                    UUID[] faceIds = new  UUID[faces.length];
                    for(int i = 0; i < faces.length ; ++i)
                    {
                        faceIds[i] = faces[i].faceId;
                    }
                    getKnownPersons(faceIds);
                }
            }
        };
        shareimage.setOnClickListener(shareimg);
        View.OnClickListener refreshquotes = new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(quotesList.containsKey(category))
                {
                    List<String> quotes = quotesList.get(category);
                    Random rand = new Random();
                    rg.removeAllViews();
                    tags.setText(tagstext);
                    for(int i=0;i<3;i++)
                    {
                        int randnum = rand.nextInt(quotes.size());
                        rb[i] = new RadioButton(TagGenerator.this);
                        rb[i].setText(quotes.get(randnum));
                        rb[i].setId(i);
                        rg.addView(rb[i]);
                    }
                }
            }
        };

        refresh.setOnClickListener(refreshquotes);
        layout.addView(rg);//you add the whole
        linearLayout.addView(layout,1);
        Glide.with(this).load(path)
                .into(tagImage);


        if (client==null){
            client = new VisionServiceRestClient("c9eeece173424d5fb5419051d98a5808", "https://westcentralus.api.cognitive.microsoft.com/vision/v1.0");
        }
        try {
            new doRequest().execute();
        } catch (Exception e) {

        }
    }


    private IdentifyResult[] getKnownPersons(final UUID[] fIds) {

        AsyncTask<UUID[], Void, IdentifyResult[]> getKnown =
                new AsyncTask<UUID[], Void, IdentifyResult[]>() {

                    @Override
                    protected IdentifyResult[] doInBackground(UUID[]... params) {
                        IdentifyResult[] p = null;
                        try {
                            Log.d("typeoffaceid", String.valueOf(params[0].getClass().getName()));
                            p = faceServiceClient.identityInPersonGroup(getDefaults("personGroupId",getApplicationContext()),params[0],20);
;
                        } catch (ClientException e) {
                            e.printStackTrace();
                            Log.d("Persongroupapiexc","adsf");
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.d("Persongroupapiexce","adsf");
                        }
                        if(p!=null)
                        Log.d("Persongroupapi",String.valueOf(p.length));
                        return p;
                    }

                    @Override
                    protected void onPostExecute(IdentifyResult[] results) {
                        resultsPersons = results;
                        Log.d("KNOWNPEOPLE", String.valueOf(results));
                        if(results==null)
                        {
                            for(int i=0;i<faces.length;i++)
                            {
                                createPersonsInGroup(i);
                            }
                        }
                        if(null!=resultsPersons)
                        {
                            for(int i=0; i<resultsPersons.length; ++i)
                            {
                                if(resultsPersons[i].candidates.size() == 0)
                                {
                                    Log.d("GROUP","NO ONE FOUND");
                                    createPersonsInGroup(i);
                                }
                                else
                                {
                                    Log.d("GROUP","FOUND");
                                    UUID person = resultsPersons[i].candidates.get(0).personId;
                                    db.incrementPersonCount(String.valueOf(person));
                                    Log.d("LISTSIZE", String.valueOf(db.getAllPersons().size()));
                                    for(int j=0;j<db.getAllPersons().size();j++)
                                    Log.d("LIST", String.valueOf(db.getAllPersons().get(j).name) + " " + String.valueOf(db.getAllPersons().get(j).counter));
                                }
                            }
                        }
                        final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(Intent.EXTRA_TEXT, tags.getText().toString());
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

                    @Override
                    protected void onPreExecute() {

                    }

                    @Override
                    protected void onProgressUpdate(Void... text) {

                    }
                };
        try {
            return getKnown.execute(fIds).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String process() throws VisionServiceException, IOException {
        Gson gson = new Gson();
        String[] features = {"Tags","Description","Categories"};
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

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
        category = (String) adapterView.getItemAtPosition(pos);
        if(quotesList.containsKey(category))
        {
            List<String> quotes = quotesList.get(category);
            Random rand = new Random();
            rg.removeAllViews();
            tags.setText(tagstext);
            for(int i=0;i<3;i++)
            {
                int randnum = rand.nextInt(quotes.size());
                rb[i] = new RadioButton(TagGenerator.this);
                rb[i].setText(quotes.get(randnum));
                rb[i].setId(i);
                rg.addView(rb[i]);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

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
                double maxScore = 0.0;
                String temp = "";
                for (Category category: result.categories) {
                    if(category.score > maxScore) {
                        maxScore = category.score;
                        temp = category.name;
                    }
                }
                //tags.append("Category: " + temp + ", score: " + maxScore + "\n");
                ArrayList<String> food = new ArrayList<String>();
                food.add("Food is Lovveeeeeeeeeeeeee!!!!!");
                food.add("The only thing I like better than talking about Food is eating");
                food.add("The closest I’ve been to a diet this year is erasing food searches from my browser history ");
                food.add("Good food is good mood ");
                food.add("Count the memories not the calories ");
                food.add("If it doesn't involve food, I'm not going ");
                food.add("You can’t live a full life on an empty stomach ");

                ArrayList<String> people = new ArrayList<String>();
                people.add("I would rather walk with a friend in the dark, than alone in the light");
                people.add("I’m not perfect. I’ll annoy you, make fun of you, say stupid things, but you’ll never find someone who loves you as much as I do. ");
                people.add("This is to the Echos of our laughter. The looks That we Share. The never ending gossips. and the Sudden amazing get aways. This is to our Past And This is to Our Future. This is to our Friendship that will Never Fade");
                people.add("The most important thing in the world is family and love.\n");
                people.add("Smile a little more, regret a little less ");
                people.add("Beauty is power; a smile is it’s sword ");
                people.add("Lighten up, just enjoy life, smile more, laugh more, and don't get so worked up about things.\n");

                ArrayList<String> outdoor = new ArrayList<String>();
                outdoor.add("Look deep into nature and then you will understand everything better ");
                outdoor.add("To walk in nature is to witness a thousand miracles ");
                outdoor.add("Difficult roads often lead to beautiful destinations ");
                outdoor.add("Nature is not a place to visit. It’s home ");
                outdoor.add("In every walk with nature, one receives far more than he seeks ");


                ArrayList<String> others = new ArrayList<String>();
                others.add("Fill your life with adventures, not things. Have stories to tell, not stuff to show");
                others.add("Life is short there is no time to leave important words unsaid");
                others.add("Sometimes you have to go up really high to understand how small you really are");
                others.add("Do not take life too seriously. You will never get out of it alive ");
                others.add("When you actually matter to a person, they’ll make time for you. No lies, No excuses ");

                int faceCount = 0;
                Collections.shuffle(food);
                Collections.shuffle(others);
                Collections.shuffle(outdoor);
                Collections.shuffle(people);

                tagstext =tags.getText().toString();
            }
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

        }
    }

    private void addPersonsToGroup(Integer facenum) {


        AsyncTask<String, Void, AddPersistedFaceResult> addPersonToGroup =
                new AsyncTask<String, Void, AddPersistedFaceResult>() {
                    @Override
                    protected AddPersistedFaceResult doInBackground(String... params) {

                        try {
                            int facenum = Integer.parseInt(params[0]);
                            Bitmap imageBitmap = BitmapFactory.decodeFile(path);
                            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
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
                            AddPersistedFaceResult addedPersonFace = faceServiceClient.addPersonFace(getDefaults("personGroupId", getApplicationContext()), faces[facenum].faceId, inputStream, "Adding User", faces[facenum].faceRectangle);
                           return addedPersonFace;
                        } catch (ClientException e) {
                            e.printStackTrace();
                            Log.d("ExceptionAdding","Null");

                        } catch (IOException e) {

                        }
                        return null;
                    }


                    @Override
                    protected void onPostExecute(AddPersistedFaceResult p) {

                        if(p!=null)
                        {
                            Log.d("ADDEDPERSONTOGROUP","Added "+ p.persistedFaceId);
                        }
                        else
                            Log.d("Error", "PesonGroupID not found");

                    }


                    @Override
                    protected void onPreExecute() {

                    }

                    @Override
                    protected void onProgressUpdate(Void... text) {

                    }
                };
        addPersonToGroup.execute(facenum.toString());
    }
    private void trainPersonGroup() {

        AsyncTask<String, Void,Void> trainGroup =
                new AsyncTask<String, Void,Void>() {
                    String exceptionMessage = "";

                    @Override
                    protected Void doInBackground(String... params) {
                        try {

                            faceServiceClient.trainPersonGroup(getDefaults("personGroupId",getApplicationContext()));
                        } catch (ClientException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                            return null;
                    }

                    @Override
                    protected void onPreExecute() {
                        //TODO: show progress dialog

                    }
                    @Override
                    protected void onProgressUpdate(Void... progress) {
                        //TODO: update progress
                    }
                    @Override
                    protected void onPostExecute(Void r) {
                        Log.d("TrainingPersonGroup","Done");
                        }
                };

        trainGroup.execute();
    }

    private void createPersonsInGroup(final Integer facenum) {

        AsyncTask<String, Void,CreatePersonResult> createPersons =
                new AsyncTask<String, Void,CreatePersonResult>() {
                    String exceptionMessage = "";

                    @Override
                    protected CreatePersonResult doInBackground(String... params) {
                        try {

                           CreatePersonResult personResult = faceServiceClient.createPerson(getDefaults("personGroupId",getApplicationContext()),faces[Integer.parseInt(params[0])].faceId.toString(),"UserCreation");
                           return personResult;
                        } catch (ClientException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPreExecute() {
                        //TODO: show progress dialog

                    }
                    @Override
                    protected void onProgressUpdate(Void... progress) {
                        //TODO: update progress
                    }
                    @Override
                    protected void onPostExecute(CreatePersonResult p)
                    {
                        faces[facenum].faceId = p.personId;
                        Groupdata gd = new Groupdata(p.personId.toString());
                        db.addPerson(gd);
                        addPersonsToGroup(facenum);
                        facecounter++;
                        if(facecounter==faces.length)
                        {
                            trainPersonGroup();
                        }

                    }
                };

        createPersons.execute(facenum.toString());
    }

    public void detectFaces()
    {
        Bitmap image = BitmapFactory.decodeFile(path);
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 30, outputStream);
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());
        FileOutputStream out = null;
        try {
            out = new FileOutputStream("/storage/emulated/0/DCIM/output.jpg");
            image.compress(Bitmap.CompressFormat.JPEG, 30, out); // bmp is your Bitmap instance
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

        Log.d("API", "[START]");
        AsyncTask<InputStream, String, Face[]> detectTask =
                new AsyncTask<InputStream, String, Face[]>() {
                    String exceptionMessage = "";

                    @Override
                    protected Face[] doInBackground(InputStream... params) {
                        try {
                            Log.d("Inside API","Hello API");
                            Face[] result = faceServiceClient.detect(
                                    params[0],
                                    true,         // returnFaceId
                                    false,        // returnFaceLandmarks
                                    new FaceServiceClient.FaceAttributeType[]{
                                            FaceServiceClient.FaceAttributeType.Age,
                                            FaceServiceClient.FaceAttributeType.Gender,
                                            FaceServiceClient.FaceAttributeType.Smile,
                                            FaceServiceClient.FaceAttributeType.Emotion,
                                            FaceServiceClient.FaceAttributeType.Exposure,
                                            FaceServiceClient.FaceAttributeType.Blur,
                                            FaceServiceClient.FaceAttributeType.Occlusion}

                            );
                            if (result == null) {
                                publishProgress(
                                        "Detection Finished. Nothing detected");
                                return null;
                            }
                            publishProgress(String.format(
                                    "Detection Finished. %d face(s) detected",
                                    result.length));
                            Log.d("API", "Worked!");
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
                        Log.d("Outside API","API exit");
                        faces = result;
                        UUID[] faceIds = new  UUID[faces.length];
                        for(int i = 0; i < faces.length ; ++i)
                        {
                            faceIds[i] = faces[i].faceId;
                        }
                        Log.d("NUMFACES",String.valueOf(faceIds.length));
                        getKnownPersons(faceIds);
                    }
                };
        try {
            detectTask.execute(inputStream).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}
