package com.musicretrieval.trackmix.Activities;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;


import com.musicretrieval.trackmix.Adapters.PlaylistAdapter;
import com.musicretrieval.trackmix.Models.Song;
import com.musicretrieval.trackmix.R;

import java.util.ArrayList;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.StopAudioProcessor;
import be.tarsos.dsp.beatroot.Agent;
import be.tarsos.dsp.beatroot.AgentList;
import be.tarsos.dsp.beatroot.Event;
import be.tarsos.dsp.beatroot.EventList;
import be.tarsos.dsp.beatroot.Induction;
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.BeatRootSpectralFluxOnsetDetector;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
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

    private AudioDispatcher dispatcher;

    private final int SAMPLE_RATE = 44100;

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
        Intent intent = new Intent(this, Play.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        intent.putExtra("PLAY_PLAYLIST", songs);
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
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        ContentResolver musicResolver = this.getContentResolver();

        Cursor musicCursor = musicResolver.query(uri, null, selection, null, sortOrder);

        if ((musicCursor != null && musicCursor.moveToFirst())) {
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int albumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int songDurationColumn  = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int songDataColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA);

            do {
                String data = musicCursor.getString(songDataColumn);
                long thisId = musicCursor.getLong(idColumn);
                String thisAlbum = musicCursor.getString(albumColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                int thisDuration = musicCursor.getInt(songDurationColumn);
                int thisBpm = getBpm(data, thisDuration);

                if (thisBpm > lowBpm && thisBpm < highBpm) {
                    songs.add(new Song(thisId, data, thisTitle, thisArtist, thisAlbum, thisDuration, thisBpm));
                }
            } while (musicCursor.moveToNext());
        }
    }

    /**
     * Estimates the songs BPM
     * @param path the path of the song
     * @return the beats per minute of the song
     */
    public int getBpm(String path, int duration) {
        new AndroidFFMPEGLocator(this);

        final EventList onsetList = new EventList();

        dispatcher = AudioDispatcherFactory.fromPipe(path, SAMPLE_RATE, 512, 256);

        ComplexOnsetDetector b = new ComplexOnsetDetector(512, 0.3, 256.0/44100.0*4.0, -70);

        dispatcher.addAudioProcessor(b);
        b.setHandler(new OnsetHandler() {
            @Override
            public void handleOnset(double v, double v1) {
                double roundedTime = Math.round(v * 100)/100.0;
                Event e = newEvent(roundedTime,0);
                e.salience = v1;
                onsetList.add(e);
            }
        });

        //TODO: Fix this hack, only checking last 10% of song, want to check in the middle of
        // the song for  2.2 seconds
        dispatcher.skip(duration/1000.0/1.1);
        dispatcher.run();

        AgentList agents = Induction.beatInduction(onsetList);
        agents.beatTrack(onsetList, -1);
        Agent best = agents.bestAgent();
        if (best != null) {
            best.fillBeats(-1.0);
            Log.d(TAG, "beatcount = " + String.valueOf(best.beatCount));
            Log.d(TAG, "beatinterval = " + String.valueOf(best.beatInterval));
            Log.d(TAG, "beattime = " + String.valueOf(best.beatTime));
            Log.d(TAG, "temposcore = " + String.valueOf(best.tempoScore));
            Log.d(TAG, "topscoretime = " + String.valueOf(best.topScoreTime));
            return (int) ((1.0/best.beatInterval)*60.0);
        } else {
            return 0;
        }
    }

    /** Creates a new Event object representing an onset or beat.
     *  @param time The time of the beat in seconds
     *  @param beatNum The index of the beat or onset.
     *  @return The Event object representing the beat or onset.
     */
    public static Event newEvent(double time, int beatNum) {
        return new Event(time,time, time, 56, 64, beatNum, 0, 1);
    } // newBeat()
}
