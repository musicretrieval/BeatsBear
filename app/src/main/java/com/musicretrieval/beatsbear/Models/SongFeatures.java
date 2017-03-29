package com.musicretrieval.beatsbear.Models;

import java.io.Serializable;

public class SongFeatures implements Serializable {
    public int bpm;
    public float[] mfcc;

    public SongFeatures() { }
}
