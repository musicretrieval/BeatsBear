package com.musicretrieval.trackmix.Activities;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
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

public class Playlist extends AppCompatActivity {

    private static final String TAG = "PLAYLIST_ACTIVITY";

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
        ArrayList<Song> userSongs = new ArrayList<>();

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
                String path = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                long thisId = musicCursor.getLong(idColumn);
                String thisAlbum = musicCursor.getString(albumColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                int thisDuration = musicCursor.getInt(songDurationColumn);
                String thisData = musicCursor.getString(songDataColumn);
                int thisBpm = getBpm(path);

                //if (thisBpm > lowBpm && thisBpm < highBpm) {
                    userSongs.add(new Song(thisId, thisTitle, thisArtist, thisAlbum, thisDuration, thisBpm));
                //}
            } while (musicCursor.moveToNext());

        }
        return userSongs;
    }

    public int getBpm(String path) {
        return new Random().nextInt(300 - 60);
    }
}
