package com.musicretrieval.beatsbear.Classifier;

import android.content.Context;
import android.util.Log;

import java.io.InputStream;

import weka.classifiers.Classifier;
import weka.core.Instance;

public class GenreClassifier {
    Classifier classifier;

    public GenreClassifier(Context context) {
        try {
            InputStream trainedModel = context.getAssets().open("trained.model");
            classifier = (Classifier) weka.core.SerializationHelper.read(trainedModel);
        } catch(Exception e) {
            Log.d("CLASSIFIER CONSTRUCTOR", e.getLocalizedMessage());
        }
    }

    public String classify(Instance i) {
        double label = -1.0;
        try {
            label = classifier.classifyInstance(i);
        } catch(Exception e) {
            Log.d("CLASSIFIER CLASSIFY", e.getLocalizedMessage());
        }
        if (label != -1)
            return AudioFeatures.ClassFeature.value((int) label);
        else
            return "NO LABEL";
    }
}
