package com.musicretrieval.trackmix.Models;

public class Song {
    private String mName;
    private int mBpm;

    public Song(String name, int bpm) {
        mName = name;
        mBpm = bpm;
    }

    public String GetName() {
        return mName;
    }

    public void SetName(String name) {
        mName = name;
    }
}
