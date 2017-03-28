package com.musicretrieval.beatsbear.Models;

public class SongFeature {
    float meanCentroid;
    float meanRolloff;
    float meanFlux;
    float meanZeroCrossings;
    float stdCentroid;
    float stdRolloff;
    float stdFlux;
    float stdZeroCrossings;
    float lowEnergy;

    public SongFeature() {
        meanCentroid = 0.0f;
        meanRolloff = 0.0f;
        meanFlux = 0.0f;
        meanZeroCrossings = 0.0f;
        stdCentroid = 0.0f;
        stdRolloff = 0.0f;
        stdFlux = 0.0f;
        stdZeroCrossings = 0.0f;
        lowEnergy = 0.0f;
    }

    public SongFeature getFeatures(String path, int duration) {
        return null;
    }
}
