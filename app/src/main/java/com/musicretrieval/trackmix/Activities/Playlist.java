package com.musicretrieval.trackmix.Activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.musicretrieval.trackmix.Models.Song;
import com.musicretrieval.trackmix.R;

import java.util.ArrayList;

public class Playlist extends AppCompatActivity {

    private int mLowBpm;
    private int mHighBpm;
    private ArrayList<Song> mSongs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        int bpms[] = getIntent().getIntArrayExtra("SONG_LIST_BPM");
        mLowBpm = bpms[0];
        mHighBpm = bpms[1];

        mSongs = RecommendSongs(mLowBpm, mHighBpm);

        Toast.makeText(this, String.valueOf(mLowBpm) + " " + String.valueOf(mHighBpm), Toast.LENGTH_SHORT).show();
    }

    private ArrayList<Song> RecommendSongs(int lowBpm, int highBpm) {
        ArrayList<Song> songs = new ArrayList<>();
        songs.add(new Song("Happy", 125));
        return songs;
    }

}
