package com.musicretrieval.beatsbear.Activities;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.musicretrieval.beatsbear.Adapters.PlaylistAdapter;
import com.musicretrieval.beatsbear.Classifier.AudioFeatures;
import com.musicretrieval.beatsbear.Classifier.AudioFeaturizer;
import com.musicretrieval.beatsbear.Classifier.GenreClassifier;
import com.musicretrieval.beatsbear.DialogFragments.AddSongDialogFragment;
import com.musicretrieval.beatsbear.Models.Song;
import com.musicretrieval.beatsbear.Models.SongFeatures;
import com.musicretrieval.beatsbear.R;

import java.util.ArrayList;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.StopAudioProcessor;
import be.tarsos.dsp.beatroot.Agent;
import be.tarsos.dsp.beatroot.AgentList;
import be.tarsos.dsp.beatroot.Event;
import be.tarsos.dsp.beatroot.EventList;
import be.tarsos.dsp.beatroot.Induction;
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MAIN_ACTIVITY";

    @BindView(R.id.main_all_button)         Button allButton;
    @BindView(R.id.main_relaxing_button)    Button relaxingButton;
    @BindView(R.id.main_activating_button)  Button activatingButton;

    @BindView(R.id.playlist)
    RecyclerView playlistView;
    @BindView(R.id.playlist_add)
    FloatingActionButton playlistAdd;
    @BindView(R.id.playlist_play)
    FloatingActionButton playlistPlay;
    @BindView(R.id.playlist_progress)
    ProgressBar playlistProgress;

    private ArrayList<Song> songs;
    private ArrayList<Song> recommendedRelaxingSongs;
    private ArrayList<Song> recommendedActivatingSongs;
    private PlaylistAdapter adapter;

    private enum PLAYLISTTYPE { PLAYALL, PLAYRELAXING, PLAYACTIVATING }

    private PLAYLISTTYPE playlistType;

    private static boolean doneFeaturizing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        playlistType = PLAYLISTTYPE.PLAYALL;

        songs = new ArrayList<>();
        recommendedRelaxingSongs = new ArrayList<>();
        recommendedActivatingSongs = new ArrayList<>();
        adapter = new PlaylistAdapter(this, songs);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        playlistView.setAdapter(adapter);
        playlistView.setLayoutManager(layoutManager);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (songs.isEmpty() || recommendedRelaxingSongs.isEmpty() || recommendedActivatingSongs.isEmpty()) {
            new RecommendSongsTask().execute();
        }
    }

    public void toggleButtonState(Button button) {
        switch (button.getId()) {
            case R.id.main_all_button:
                button.setBackgroundResource(R.drawable.button_bg_selected);
                button.setTextColor(Color.WHITE);
                relaxingButton.setBackgroundResource(R.drawable.button_bg_not_selected);
                relaxingButton.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
                activatingButton.setBackgroundResource(R.drawable.button_bg_not_selected);
                activatingButton.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
                playlistType = PLAYLISTTYPE.PLAYALL;
                break;
            case R.id.main_relaxing_button:
                button.setBackgroundResource(R.drawable.button_bg_selected);
                button.setTextColor(Color.WHITE);
                allButton.setBackgroundResource(R.drawable.button_bg_not_selected);
                allButton.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
                activatingButton.setBackgroundResource(R.drawable.button_bg_not_selected);
                activatingButton.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
                playlistType = PLAYLISTTYPE.PLAYRELAXING;
                break;
            case R.id.main_activating_button:
                button.setBackgroundResource(R.drawable.button_bg_selected);
                button.setTextColor(Color.WHITE);
                allButton.setBackgroundResource(R.drawable.button_bg_not_selected);
                allButton.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
                relaxingButton.setBackgroundResource(R.drawable.button_bg_not_selected);
                relaxingButton.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
                playlistType = PLAYLISTTYPE.PLAYACTIVATING;
                break;
        }
    }

    @OnClick(R.id.main_all_button)
    public void startAll() {
        toggleButtonState(allButton);
        adapter = new PlaylistAdapter(this, songs);
        playlistView.setAdapter(adapter);
        playlistView.getAdapter().notifyDataSetChanged();
    }

    @OnClick(R.id.main_relaxing_button)
    public void startRelaxing() {
        toggleButtonState(relaxingButton);
        adapter = new PlaylistAdapter(this, recommendedRelaxingSongs);
        playlistView.setAdapter(adapter);
        playlistView.getAdapter().notifyDataSetChanged();
    }

    @OnClick(R.id.main_activating_button)
    public void startActivating() {
        toggleButtonState(activatingButton);
        adapter = new PlaylistAdapter(this, recommendedActivatingSongs);
        playlistView.setAdapter(adapter);
        playlistView.getAdapter().notifyDataSetChanged();
    }

    @OnClick(R.id.playlist_play)
    public void play() {
        Intent intent = new Intent(this, Play.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        switch(playlistType) {
            case PLAYALL:
                intent.putExtra("PLAY_PLAYLIST", songs);
                startActivity(intent);
                break;
            case PLAYRELAXING:
                intent.putExtra("PLAY_PLAYLIST", recommendedRelaxingSongs);
                startActivity(intent);
                break;
            case PLAYACTIVATING:
                intent.putExtra("PLAY_PLAYLIST", recommendedActivatingSongs);
                startActivity(intent);
                break;
        }
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

                songs.add(new Song(thisId, data, thisTitle, thisArtist, thisAlbum, thisDuration, features));

            } while (musicCursor.moveToNext());

            musicCursor.close();
        }
    }

    /**
     * Recommends a list of songs based on BPM
     */
    private void recommendSongs() {
        for (Song song : songs) {
            SongFeatures features = song.getFeatures();


            System.out.println("Song " + song.getTitle() + " is " + features.genre);
            boolean relaxing = (features.bpm < Song.RELAXING_THRESHOLD) &&
                                (features.genre.equals("Blues") ||
                                features.genre.equals("Classical") ||
                                features.genre.equals("Country"));

            if (relaxing) {
                recommendedRelaxingSongs.add(song);
            } else {
                recommendedActivatingSongs.add(song);
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

        AudioDispatcher dispatcher;

        final EventList onsetList = new EventList();

        final int SAMPLE_RATE = 44100;
        final int buffSize = 1024;
        final int overlap = buffSize/2;

        final AudioFeaturizer audioFeaturizer = new AudioFeaturizer();
        final ArrayList<Instance> songSampleData = new ArrayList<>();

        doneFeaturizing = false;

        try {
            dispatcher = AudioDispatcherFactory.fromPipe(path, SAMPLE_RATE, buffSize, overlap);

            // Sample 2.2 seconds of music at the center
            double start = duration/1000.0/2.0;
            double stop = start + 2.2;
            dispatcher.skip(start);
            dispatcher.addAudioProcessor(new StopAudioProcessor(stop));
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
            dispatcher.addAudioProcessor(c);
            dispatcher.addAudioProcessor(new AudioProcessor() {
                @Override
                public boolean process(AudioEvent audioEvent) {
                    songSampleData.add(audioFeaturizer.featurize(audioEvent, null));
                    return true;
                }

                @Override
                public void processingFinished() {
                    doneFeaturizing = true;
                }
            });

            dispatcher.run();
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
        }

        // Wait for processing to finish
        while (!doneFeaturizing) { }

        // Find the best agent that fits
        AgentList agents = Induction.beatInduction(onsetList);
        agents.beatTrack(onsetList, -1);
        Agent best = agents.bestAgent();
        if (best != null) {
            best.fillBeats(-1.0);
            features.bpm = (int) ((1.0/best.beatInterval)*60.0);
        } else {
            // Arbitrary BPM that isn't 0
            features.bpm = 60;
        }

        Instance featureVals = new DenseInstance(AudioFeatures.Features.length+1);
        Instances data = new Instances(audioFeaturizer.getDataset());
        featureVals.setDataset(data);

        // take the average
        for (int i = 0; i < AudioFeatures.Features.length; i++) {
            double avgC = 0.0;
            for (Instance ins : songSampleData) {
                avgC += ins.value(i)/songSampleData.size();
            }
            featureVals.setValue(i, avgC);
        }

        GenreClassifier classifier = new GenreClassifier(this);

        features.genre = classifier.classify(featureVals);

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
            recommendSongs();
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
