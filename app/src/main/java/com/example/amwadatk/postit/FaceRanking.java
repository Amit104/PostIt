package com.example.amwadatk.postit;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceAttribute;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.rest.ClientException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class FaceRanking {
    ImageView tagImage;
    EditText tags;
    private Context context;
    Button shareimage,refresh;
    DatabaseHandler db;
    public static Face[] faces, faceidall;
    private final String apiEndpoint = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0";
    private final String subscriptionKey = "34ab0e4557724ec8a90ef592bf76a3e5";
    private final FaceServiceClient faceServiceClient =
            new FaceServiceRestClient(apiEndpoint, subscriptionKey);
    IdentifyResult[] resultsPersons;
    int totalSize = 0;
    double score = 0.0;
    double finalScore = 0.0;

    public FaceRanking(Context context) {
        this.context = context;
        db = new DatabaseHandler(context);
    }
    public double rank(Face[] faces) {
        if(faces!= null){

            if(faces.length==0) {
                // this is fine
            } else {

                List<Integer> newfaces = new ArrayList<Integer>();
                int baseArea = Getarea(faces[0].faceRectangle.width, faces[0].faceRectangle.height);
                int fc = 0,facenum=0;
                for (Face face : faces) {
                    FaceRectangle faceRectangle = face.faceRectangle;
                    FaceAttribute faceAttribute = face.faceAttributes;
                    int size = Getarea(faceRectangle.width, faceRectangle.height);
                    Log.d("FACES", String.valueOf(fc));
                    if (size > 0.1 * baseArea) {
                        fc++;
                        newfaces.add(facenum);
                    }
                    facenum++;
                }
                if(newfaces.size()>0)
                {
                    faceidall = new Face[newfaces.size()];
                    Log.d("FACESSIZE", String.valueOf(newfaces.size()) + " " + faceidall.length + " " + newfaces.get(0));
                    for(int i=0;i<newfaces.size();i++)
                    {
                        faceidall[i]=faces[newfaces.get(i)];
                    }
                }

                for (Face face:faceidall){

                    FaceRectangle faceRectangle = face.faceRectangle;
                    FaceAttribute faceAttribute = face.faceAttributes;
                    int size = Getarea(faceRectangle.width,faceRectangle.height);
                    totalSize = totalSize + size;
                    score += GetScore(face.faceId, size, faceAttribute.smile,faceAttribute.emotion.happiness,faceAttribute.exposure.value
                            ,faceAttribute.blur.value,faceAttribute.occlusion.foreheadOccluded,
                            faceAttribute.occlusion.eyeOccluded,faceAttribute.occlusion.mouthOccluded)*size;

                }
                finalScore = score/totalSize;
                UUID[] faceIds = new  UUID[faceidall.length];
                for(int i = 0; i < faceidall.length ; ++i)
                {
                    faceIds[i] = faceidall[i].faceId;
                }
                return getKnownPersons(faceIds);
            }
        }
        return -50.0;
    }

    public int Getarea(int width, int height){
        return width*height;
    }

    public double GetScore(UUID faceId, int size, double smile, double happiness, double exposure, double blur,
                           boolean forheadOcclusion, boolean eyeOcclusion, boolean mouthOcclusion){
        double score = 0;
        score -= (forheadOcclusion)?5.0:0.0;
        score -= (eyeOcclusion)?3.0:0.0;
        score -= (mouthOcclusion)?2.0:0.0;
        score += smile*3.0;
        score += happiness*5.0;
        score += exposure*4.0;
        return score;

    }
    private double getKnownPersons(final UUID[] fIds) {

        AsyncTask<UUID[], Void, IdentifyResult[]> getKnown =
                new AsyncTask<UUID[], Void, IdentifyResult[]>() {

                    @Override
                    protected IdentifyResult[] doInBackground(UUID[]... params) {
                        IdentifyResult[] p = null;
                        try {
                            Log.d("typeoffaceid", String.valueOf(params[0].getClass().getName()));
                            p = faceServiceClient.identityInPersonGroup(MainActivity.getDefaults("personGroupId",context),params[0],20);

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
                        List<Integer> personCount = new ArrayList<>();
                        if(null!=resultsPersons)
                        {
                            for(int i=0; i<resultsPersons.length; ++i)
                            {
                                if(resultsPersons[i].candidates.size() == 0)
                                {
                                    Log.d("GROUP","NO ONE FOUND");
                                }
                                else
                                {
                                    Log.d("GROUP","FOUND");
                                    UUID person = resultsPersons[i].candidates.get(0).personId;
                                    //db.incrementPersonCount(String.valueOf(person));
                                    int count = db.getPersonCount(String.valueOf(person));
                                    Log.d("MAXCOUNT", String.valueOf(db.getMaxCount()));
                                    finalScore += count/db.getMaxCount() * 2.0;
                                    Log.d("LISTSIZE", String.valueOf(db.getAllPersons().size()));
                                    for(int j=0;j<db.getAllPersons().size();j++)
                                        Log.d("LIST", String.valueOf(db.getAllPersons().get(j).name) + " " + String.valueOf(db.getAllPersons().get(j).counter));
                                }
                            }
                            Log.d("SCORESNEW", String.valueOf(finalScore));

                        }
                    }

                    @Override
                    protected void onPreExecute() {

                    }

                    @Override
                    protected void onProgressUpdate(Void... text) {

                    }
                };
        try {
            getKnown.execute(fIds).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return finalScore;
    }

}
