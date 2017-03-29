package com.musicretrieval.beatsbear.Activities;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.musicretrieval.beatsbear.Adapters.PlaylistAdapter;
import com.musicretrieval.beatsbear.DialogFragments.AddSongDialogFragment;
import com.musicretrieval.beatsbear.Models.Song;
import com.musicretrieval.beatsbear.Models.SongFeatures;
import com.musicretrieval.beatsbear.R;

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
import be.tarsos.dsp.mfcc.MFCC;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class Playlist extends AppCompatActivity {

    private static final String TAG = "PLAYLIST_ACTIVITY";

    private int lowBpm;
    private int highBpm;
    private ArrayList<Song> songs;
    private ArrayList<Song> recommendedSongs;
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
    @BindView(R.id.playlist_progress)
    ProgressBar playlistProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);
        ButterKnife.bind(this);
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        songs = new ArrayList<>();
        recommendedSongs = new ArrayList<>();
        adapter = new PlaylistAdapter(this, recommendedSongs);
        layoutManager = new LinearLayoutManager(this);
        playlistView.setAdapter(adapter);
        playlistView.setLayoutManager(layoutManager);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!getIntent().getBooleanExtra("SONG_RECOMMEND", false)) {
            lowBpm = sharedPref.getInt("SONG_LIST_BPM_LOW", Song.MINIMUM_BPM);
            highBpm = sharedPref.getInt("SONG_LIST_BPM_HIGH", Song.MAXIMUM_BPM);
        } else {
            lowBpm = getIntent().getIntExtra("SONG_LIST_BPM_LOW", Song.MINIMUM_BPM);
            highBpm = getIntent().getIntExtra("SONG_LIST_BPM_HIGH", Song.MAXIMUM_BPM);
        }

        if (recommendedSongs.isEmpty()) {
            new RecommendSongsTask().execute();
        }
    }

    @OnClick(R.id.playlist_play)
    public void play() {
        Intent intent = new Intent(this, Play.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        intent.putExtra("PLAY_PLAYLIST", recommendedSongs);
        startActivity(intent);
    }

    @OnClick(R.id.playlist_add)
    public void addSong() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("ADDSONG_DIALOG_FRAGMENT");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment addSong = AddSongDialogFragment.newInstance(songs);
        addSong.show(ft, "ADDSONG_DIALOG_FRAGMENT");
    }

    /**
     * Gets all songs
     * @return a list of songs
     */
    private void getSongs() {
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
                SongFeatures features = getFeatures(data, thisDuration);

                while (!dispatcher.isStopped());

                songs.add(new Song(thisId, data, thisTitle, thisArtist, thisAlbum, thisDuration, features));

            } while (musicCursor.moveToNext());
        }
    }

    /**
     * Recommends a list of songs based on BPM
     * @param lowBpm the minimum bpm
     * @param highBpm the maximum bpm
     */
    private void recommendSongs(int lowBpm, int highBpm) {
        for (Song song : songs) {
            int thisBpm = song.getBpm();
            if (thisBpm > lowBpm && thisBpm < highBpm) {
                recommendedSongs.add(song);
                //songs.remove(song);
            }
        }
    }

    /**
     * Estimates the songs features
     * @param path the path of the song
     * @param duration the duration of the song in milliseconds
     * @return the features of the song
     */
    public SongFeatures getFeatures(String path, int duration) {
        SongFeatures features = new SongFeatures();

        new AndroidFFMPEGLocator(this);

        final EventList onsetList = new EventList();
        final int buffSize = 512;
        final int overlap = buffSize/2;

        dispatcher = AudioDispatcherFactory.fromPipe(path, SAMPLE_RATE, buffSize, overlap);

        // Detect the beat intervals
        ComplexOnsetDetector c = new ComplexOnsetDetector(buffSize, 0.3, 256.0/SAMPLE_RATE*4.0, -70);
        c.setHandler(new OnsetHandler() {
            @Override
            public void handleOnset(double v, double v1) {
                double roundedTime = Math.round(v * 100)/100.0;
                Event e = newEvent(roundedTime, 0);
                e.salience = v1;
                onsetList.add(e);
            }
        });

        // Marsyas: hopsize 512, winsize 512, 13 coefficients
        MFCC m = new MFCC(buffSize, SAMPLE_RATE, 13, 13, 133.3334f, (float)SAMPLE_RATE/2f);

        // Sample 2.2 seconds of music
        double start = duration/1000.0/2.0;
        double stop = start + 2.2;
        dispatcher.skip(start);
        dispatcher.addAudioProcessor(new StopAudioProcessor(stop));
        dispatcher.addAudioProcessor(c);
        dispatcher.addAudioProcessor(m);

        dispatcher.run();

        // Find the best agent that fits
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
            features.bpm = (int) ((1.0/best.beatInterval)*60.0);
        } else {
            features.bpm = 0;
        }

        features.mfcc = m.getMFCC();

        return features;
    }

    /** Creates a new Event object representing an onset or beat.
     *  @param time The time of the beat in seconds
     *  @param beatNum The index of the beat or onset.
     *  @return The Event object representing the beat or onset.
     */
    public static Event newEvent(double time, int beatNum) {
        return new Event(time, time, time, 56, 64, beatNum, 0, 1);
    }

    private class RecommendSongsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            // Start getting all songs
            getSongs();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // Hide progress bar and update adapter when done
            recommendSongs(lowBpm, highBpm);
            adapter.notifyDataSetChanged();
            playlistAdd.setVisibility(View.VISIBLE);
            playlistPlay.setVisibility(View.VISIBLE);
            playlistProgress.setVisibility(View.GONE);
        }

        @Override
        protected void onPreExecute() {
            // Show progress bar while recommending songs
            playlistAdd.setVisibility(View.GONE);
            playlistPlay.setVisibility(View.GONE);
            playlistProgress.setVisibility(View.VISIBLE);
        }
    }

}
