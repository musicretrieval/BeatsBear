package com.musicretrieval.beatsbear.Models;

import java.io.Serializable;

import weka.core.Instance;

public class SongFeatures implements Serializable {
    public int bpm;
    public String genre;

    public SongFeatures() { }
}
