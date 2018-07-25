package com.example.amwadatk.postit;

import android.content.SharedPreferences;
import android.util.Log;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.*;
import com.microsoft.projectoxford.face.rest.ClientException;

import java.io.IOException;
import java.util.UUID;

public class FaceRanking {
    private final String apiEndpoint = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0";
    private final String subscriptionKey = "34ab0e4557724ec8a90ef592bf76a3e5";
    private final FaceServiceClient faceServiceClient =
            new FaceServiceRestClient(apiEndpoint, subscriptionKey);
    public double rank(Face[] faces) {
        if(faces!= null){
            int totalSize = 0;
            double score = 0.0;
            for (Face face:faces){

                FaceRectangle faceRectangle = face.faceRectangle;
                FaceAttribute faceAttribute = face.faceAttributes;
                int size = Getarea(faceRectangle.width,faceRectangle.height);
                totalSize = totalSize + size;
                score += GetScore(face.faceId, size, faceAttribute.smile,faceAttribute.emotion.happiness,faceAttribute.exposure.value
                                    ,faceAttribute.blur.value,faceAttribute.occlusion.foreheadOccluded,
                                    faceAttribute.occlusion.eyeOccluded,faceAttribute.occlusion.mouthOccluded)*size;

            }
            return score/totalSize;
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
}
