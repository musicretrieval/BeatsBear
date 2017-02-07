package com.musicretrieval.trackmix.Activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.musicretrieval.trackmix.Adapters.PlaylistAdapter;
import com.musicretrieval.trackmix.Models.Song;
import com.musicretrieval.trackmix.R;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class Playlist extends AppCompatActivity {

    private int lowBpm;
    private int highBpm;
    private ArrayList<Song> songs;
    private PlaylistAdapter adapter;
    private LinearLayoutManager layoutManager;

    @BindView(R.id.playlist)
    RecyclerView playListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);
        ButterKnife.bind(this);

        int bpms[] = getIntent().getIntArrayExtra("SONG_LIST_BPM");
        lowBpm = bpms[0];
        highBpm = bpms[1];

        songs = recommendSongs(lowBpm, highBpm);

        adapter = new PlaylistAdapter(this, songs);
        layoutManager = new LinearLayoutManager(this);
        playListView.setAdapter(adapter);
        playListView.setLayoutManager(layoutManager);

        Toast.makeText(this, String.valueOf(lowBpm) + " " + String.valueOf(highBpm), Toast.LENGTH_SHORT).show();
    }

    /**
     * Recommends a play list dependent on the range of requested beats per minute
     * @param lowBpm low end range of beats per minute
     * @param highBpm high end range of beats per minute
     * @return a list of songs which fit the bpm criteria
     */
    private ArrayList<Song> recommendSongs(int lowBpm, int highBpm) {
        ArrayList<Song> songs = new ArrayList<>();
        songs.add(new Song("wow", "a", 300));
        songs.add(new Song("Sup", "b", 200));
        songs.add(new Song("Happy", "c", 125));
        songs.add(new Song("Birthday", "d", 70));
        songs.add(new Song("Du hast mich gefragt und ich hab nichts gesagt", "e", 12));
        return songs;
    }

}
