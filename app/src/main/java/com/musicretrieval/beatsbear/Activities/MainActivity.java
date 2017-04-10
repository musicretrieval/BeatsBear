package com.musicretrieval.beatsbear.Activities;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.musicretrieval.beatsbear.Classifier.AudioFeatures;
import com.musicretrieval.beatsbear.Classifier.AudioFeaturizer;
import com.musicretrieval.beatsbear.Classifier.GenreClassifier;
import com.musicretrieval.beatsbear.Models.Song;
import com.musicretrieval.beatsbear.Models.SongFeatures;
import com.musicretrieval.beatsbear.Peripheral.StepDetector;
import com.musicretrieval.beatsbear.Peripheral.StepListener;
import com.musicretrieval.beatsbear.R;
import com.musicretrieval.beatsbear.Services.MediaPlayerService;
import com.musicretrieval.beatsbear.Utils.PlaylistType;
import com.musicretrieval.beatsbear.Utils.StorageUtil;
import com.musicretrieval.beatsbear.Utils.TempoAdjustmentType;

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
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class MainActivity extends AppCompatActivity implements  PlaylistFragment.OnPlaySongsListener,
                                                                PlaylistFragment.OnPlaylistSwitchListener,
                                                                PlayingFragment.OnPlayListener,
                                                                PlayingFragment.OnNextListener,
                                                                PlayingFragment.OnPrevListener,
                                                                PlayingFragment.OnPauseListener,
                                                                PlayingFragment.OnTempoListener,
                                                                PlayingFragment.OnSeekListener,
                                                                PlayingFragment.OnTempoAdjustmentTypeListener,
                                                                SensorEventListener,
                                                                StepListener {

    @BindView(R.id.playlist_progress)       ProgressBar playlistProgress;

    private static final String TAG = "MAIN_ACTIVITY";

    private ArrayList<Song> songs;
    private ArrayList<Song> relaxingSongs;
    private ArrayList<Song> activatingSongs;
    private ArrayList<Song> currentSongs;

    private int songIndex = 0;

    private static boolean doneFeaturizing = false;

    private MediaPlayerService player;
    boolean serviceBound = false;
    public static final String Broadcast_PLAY_NEW_AUDIO = "musicretrieval.beatsbear.PlayNewAudio";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private StepDetector stepDetector;
    private long steps;
    private long lastStepTime = 0;
    private long[] lastStepDeltas = {-1, -1, -1, -1};
    private int lastStepDeltasIndex = 0;
    private int pace = 0;
    private TempoAdjustmentType tempoAdjustmentType;

    private PlaylistFragment playlistFragment = null;
    private PlayingFragment playingFragment = null;

    private Handler paceHandler = new Handler();

    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;
            Toast.makeText(MainActivity.this, "Playing", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        songs = new ArrayList<>();
        relaxingSongs = new ArrayList<>();
        activatingSongs = new ArrayList<>();
        currentSongs = songs;

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        stepDetector = new StepDetector();
        stepDetector.registerListener(this);

        tempoAdjustmentType = TempoAdjustmentType.MANUAL;
     }

    @Override
    protected void onResume() {
        super.onResume();
        if (songs.isEmpty())
            new RecommendSongsTask().execute();
        showPlaylist();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            player.stopSelf();
        }

        paceHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            stepDetector.updateAccel(event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void step(long timeNs) {
        long thisStepTime = System.currentTimeMillis();
        steps++;

        if (lastStepTime > 0) {
            long delta = thisStepTime - lastStepTime;
            lastStepDeltas[lastStepDeltasIndex] = delta;
            lastStepDeltasIndex = (lastStepDeltasIndex + 1) % lastStepDeltas.length;

            long sum = 0;
            boolean isMeaningful = true;
            for (long lastStepDelta : lastStepDeltas) {
                if (lastStepDelta < 0) {
                    isMeaningful = false;
                    break;
                }
                sum += lastStepDelta;
            }
            if (isMeaningful && sum > 0) {
                long avg = sum / lastStepDeltas.length;
                pace = (int) (60*1000 / avg);
            }
            else {
                pace = -1;
            }
        }

        lastStepTime = thisStepTime;

        if (tempoAdjustmentType == TempoAdjustmentType.PACE && pace > 60) {
            player.updateBPM(pace);
        }
    }

    @Override
    public void onPlaySongsPressed(int position) {
        songIndex = position;
        playAudio(currentSongs);
    }

    @Override
    public void onPlaylistTypeChanged(PlaylistType type) {
        switch (type) {
            case PLAYALL:
                currentSongs = songs;
                break;
            case PLAYRELAXING:
                currentSongs = relaxingSongs;
                break;
            case PLAYACTIVATING:
                currentSongs = activatingSongs;
                break;
            default:
                break;
        }

        playlistFragment.switchPlaylist(currentSongs);
    }

    @Override
    public void onTempoAdjustmentTypeChanged(TempoAdjustmentType type) {
        switch (type) {
            case MANUAL:
                tempoAdjustmentType = TempoAdjustmentType.MANUAL;
                paceHandler.removeCallbacksAndMessages(null);
                break;
            case PACE:
                tempoAdjustmentType = TempoAdjustmentType.PACE;
                paceHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        boolean decrease = false;
                        if (System.currentTimeMillis() - lastStepTime > 5000) {
                            decrease = true;
                        }

                        if (pace > 60 && decrease) {
                            pace--;
                            player.updateBPM(pace);
                        }

                        paceHandler.postDelayed(this, 1000);
                    }
                }, 1000);
                break;
        }
    }

    @Override
    public void onPlayPressed() {
        player.resume();
    }

    @Override
    public void onPausePressed() {
        player.pause();
    }

    @Override
    public void onNextPressed() {
        player.next();
    }

    @Override
    public void onPrevPressed() {
        player.previous();
    }

    @Override
    public void onTempoChanged(double amount) {
        player.updateTempo(amount);
    }

    @Override
    public void onSeekPressed(long seconds) {
        player.seek(seconds);
    }

    private void playAudio(ArrayList<Song> songs) {
        pace = songs.get(songIndex).getFeatures().bpm;
        //Check is service is active
        if (!serviceBound) {
            //Store Serializable audioList to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(songs);
            storage.storeAudioIndex(songIndex);

            Intent playerIntent = new Intent(this, MediaPlayerService.class);

            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudioIndex(songIndex);
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }

        showController();
    }

    private void showController() {
        FragmentManager fm = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction ft = fm.beginTransaction();
        Song currentSong = currentSongs.get(songIndex);

        playingFragment = PlayingFragment.newInstance(currentSong);
        if (!playingFragment.isVisible()) {
            ft.add(R.id.fragment_container, playingFragment);
            ft.addToBackStack("CURRENTLY_PLAYING");
            ft.commit();
        }
    }

    private void showPlaylist() {
        FragmentManager fm = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction ft = fm.beginTransaction();

        playlistFragment = PlaylistFragment.newInstance((ArrayList<Song>) currentSongs.clone());
        if (!playlistFragment.isVisible()) {
            ft.add(R.id.fragment_container, playlistFragment);
            ft.commit();
        }
    }

    /**
     * Gets all songs
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

            boolean relaxing = (features.bpm < Song.RELAXING_THRESHOLD) &&
                                (features.genre.equals("Blues") ||
                                 features.genre.equals("Classical") ||
                                 features.genre.equals("Country"));

            if (relaxing) {
                relaxingSongs.add(song);
            } else {
                activatingSongs.add(song);
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

        AudioDispatcher dispatcher = null;

        final EventList onsetList = new EventList();

        final int SAMPLE_RATE = 22050;
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
            ComplexOnsetDetector c = new ComplexOnsetDetector(buffSize, 0.4);
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
            e.printStackTrace();
        }

        // Wait for processing to finish
        while (!doneFeaturizing) { }

        if (dispatcher != null) {
            dispatcher.stop();
        }

        // Find the best agent that fits
        AgentList agents = Induction.beatInduction(onsetList);
        agents.beatTrack(onsetList, -1);
        Agent best = agents.bestAgent();
        features.bpm = (best != null) ? (int) ((1.0/best.beatInterval)*60.0) : 60;

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
            playlistProgress.setVisibility(View.GONE);
            playlistFragment.switchPlaylist(songs);
        }

        @Override
        protected void onPreExecute() {
            // Show progress bar while recommending songs
            playlistProgress.setVisibility(View.VISIBLE);
        }
    }
}
