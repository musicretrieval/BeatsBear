package com.musicretrieval.beatsbear.Classifier;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.mfcc.MFCC;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class AudioFeaturizer {
    private Instances dataset;

    public AudioFeaturizer() {
        // set up the data headers/attributes
        ArrayList<Attribute> attInfo = new ArrayList<Attribute>(AudioFeatures.Features.length);
        for (AudioFeatures.Feature feature : AudioFeatures.Features) {
            attInfo.add(new Attribute(AudioFeatures.FeatureNames.get(feature)));
        }
        attInfo.add(AudioFeatures.ClassFeature);
        dataset = new Instances("TestInstances", attInfo, 0);
        dataset.setClassIndex(AudioFeatures.ClassFeature.index());
    }

    public Instances getDataset(){
        return dataset;
    }

    public Instance featurize(AudioEvent e, String sampleClass) {
        Instance featureVals = new DenseInstance(AudioFeatures.Features.length+1);
        Instances data = new Instances(dataset);
        featureVals.setDataset(data);
        double mfcc[] = computeMFCC(e);
        for (int i = 0; i < AudioFeatures.Features.length; i++) {
            double featureVal = Math.random();
            boolean setFeatureVal = true;
            switch (AudioFeatures.Features[i]) {
                case MFCC0:
                    featureVal = mfcc[0];
                    break;
                case MFCC1:
                    featureVal = mfcc[1];
                    break;
                case MFCC2:
                    featureVal = mfcc[2];
                    break;
                case MFCC3:
                    featureVal = mfcc[3];
                    break;
                case MFCC4:
                    featureVal = mfcc[4];
                    break;
                case MFCC5:
                    featureVal = mfcc[5];
                    break;
                case MFCC6:
                    featureVal = mfcc[6];
                    break;
                case MFCC7:
                    featureVal = mfcc[7];
                    break;
                case MFCC8:
                    featureVal = mfcc[8];
                    break;
                case MFCC9:
                    featureVal = mfcc[9];
                    break;
                case MFCC10:
                    featureVal = mfcc[10];
                    break;
                case MFCC11:
                    featureVal = mfcc[11];
                    break;
                case MFCC12:
                    featureVal = mfcc[12];
                    break;
                case MFCC13:
                    featureVal = mfcc[13];
                    break;
                case MFCC14:
                    featureVal = mfcc[14];
                    break;
                case MFCC15:
                    featureVal = mfcc[15];
                    break;
                case MFCC16:
                    featureVal = mfcc[16];
                    break;
                case MFCC17:
                    featureVal = mfcc[17];
                    break;
                case MFCC18:
                    featureVal = mfcc[18];
                    break;
                case MFCC19:
                    featureVal = mfcc[19];
                    break;
                case MFCC20:
                    featureVal = mfcc[20];
                    break;
                case MFCC21:
                    featureVal = mfcc[21];
                    break;
                case MFCC22:
                    featureVal = mfcc[22];
                    break;
                case MFCC23:
                    featureVal = mfcc[23];
                    break;
                case MFCC24:
                    featureVal = mfcc[24];
                    break;
                case MFCC25:
                    featureVal = mfcc[25];
                    break;
                case MFCC26:
                    featureVal = mfcc[26];
                    break;
                case MFCC27:
                    featureVal = mfcc[27];
                    break;
                case MFCC28:
                    featureVal = mfcc[28];
                    break;
                case MFCC29:
                    featureVal = mfcc[29];
                    break;
                default:
                    featureVal = Math.random();
                    break;
            }
            if (setFeatureVal) {
                featureVals.setValue(i, featureVal);
            }
        }
        if (sampleClass != null)
            featureVals.setValue(AudioFeatures.Features.length, sampleClass);
        return featureVals;
    }

    private double[] computeMFCC(AudioEvent e) {
        PrintStream out = System.out;
        System.setOut(new PrintStream(new NullOutputStream()));
        final MFCC mfcc = new MFCC(1024, (int)e.getSampleRate());
        mfcc.process(e);
        float mfccs[] = mfcc.getMFCC();
        double mfccDoubles[] = new double[mfccs.length];
        for (int i = 0; i < mfccs.length; i++) {
            mfccDoubles[i] = (double) mfccs[i];
        }
        System.setOut(out);
        return mfccDoubles;
    }

    /**Writes to nowhere*/
    public class NullOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {

        }
    }
}
