package com.example.amwadatk.postit;

import com.microsoft.projectoxford.face.contract.*;

public class FaceRanking {
    public double rank(Face[] faces) {
        if(faces!= null){
            int totalSize = 0;
            double score = 0.0;
            for (Face face:faces){

                FaceRectangle faceRectangle = face.faceRectangle;
                FaceAttribute faceAttribute = face.faceAttributes;
                int size = Getarea(faceRectangle.width,faceRectangle.height);
                totalSize = totalSize + size;
                score += GetScore(faceAttribute.smile,faceAttribute.emotion.happiness,faceAttribute.exposure.value
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

    public double GetScore(double smile, double happiness, double exposure, double blur,
                          boolean forheadOcclusion, boolean eyeOcclusion, boolean mouthOcclusion){
        double score = 0;
        score -= (forheadOcclusion)?5.0:0.0;
        score -= (eyeOcclusion)?3.0:0.0;
        score -= (mouthOcclusion)?2.0:0.0;
        score += smile*5.0;
        score += happiness*3.0;
        score += exposure*4.0;
        score -= blur*2.0;
        return score;
    }
}
