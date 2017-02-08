package com.musicretrieval.trackmix.Activities;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.musicretrieval.trackmix.Adapters.PlaylistAdapter;
import com.musicretrieval.trackmix.Models.Song;
import com.musicretrieval.trackmix.R;

import java.util.ArrayList;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class Playlist extends AppCompatActivity {

    private static final String TAG = "PLAYLIST_ACTIVITY";

    private int lowBpm;
    private int highBpm;
    private int bpms[];
    private ArrayList<Song> songs;
    private PlaylistAdapter adapter;
    private LinearLayoutManager layoutManager;

    private SharedPreferences sharedPref;

    @BindView(R.id.playlist)
    RecyclerView playlistView;
    @BindView(R.id.playlist_add)
    FloatingActionButton playlistAdd;
    @BindView(R.id.playlist_play)
    FloatingActionButton playlistPlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);
        ButterKnife.bind(this);

        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        songs = new ArrayList<>();
        adapter = new PlaylistAdapter(this, songs);
        layoutManager = new LinearLayoutManager(this);
        playlistView.setAdapter(adapter);
        playlistView.setLayoutManager(layoutManager);
    }

    @Override
    protected void onResume() {
        super.onResume();

        lowBpm = sharedPref.getInt("SONG_LIST_BPM_LOW", Song.MINIMUM_BPM);
        highBpm = sharedPref.getInt("SONG_LIST_BPM_HIGH", Song.MAXIMUM_BPM);
        recommendSongs(lowBpm, highBpm);
        adapter.notifyDataSetChanged();
    }

    @OnClick(R.id.playlist_play)
    public void Play() {
        ArrayList<String> paths = new ArrayList<>();
        for (int i = 0; i < songs.size(); i++) paths.add(songs.get(i).getPath());
        Intent intent = new Intent(this, Play.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        intent.putStringArrayListExtra("PLAY_PLAYLIST", paths);
        startActivity(intent);
    }

    @OnClick(R.id.playlist_add)
    public void AddSong() {
        
    }

    /**
     * Recommends a play list dependent on the range of requested beats per minute
     * @param lowBpm low end range of beats per minute
     * @param highBpm high end range of beats per minute
     * @return a list of songs which fit the bpm criteria
     */
    private void recommendSongs(int lowBpm, int highBpm) {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DURATION
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        ContentResolver musicResolver = this.getContentResolver();

        Cursor musicCursor = musicResolver.query(uri, projection, selection, null, sortOrder);

        if ((musicCursor != null && musicCursor.moveToFirst())) {
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int albumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int songDurationColumn  = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int songDataColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA);

            do {
                String path = musicCursor.getString(songDataColumn);
                long thisId = musicCursor.getLong(idColumn);
                String thisAlbum = musicCursor.getString(albumColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                int thisDuration = musicCursor.getInt(songDurationColumn);
                int thisBpm = getBpm(path);

                //if (thisBpm > lowBpm && thisBpm < highBpm) {
                    songs.add(new Song(thisId, path, thisTitle, thisArtist, thisAlbum, thisDuration, thisBpm));
                //}
            } while (musicCursor.moveToNext());

        }
    }

    /**
     * Estimates the songs BPM
     * @param path the path of the song
     * @return the beats per minute of the song
     */
    public int getBpm(String path) {
        return new Random().nextInt(300 - 60);
    }
}
