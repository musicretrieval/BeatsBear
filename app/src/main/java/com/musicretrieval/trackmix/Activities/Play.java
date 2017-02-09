package com.musicretrieval.trackmix.Activities;

import android.media.AudioManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.musicretrieval.trackmix.R;

import java.util.ArrayList;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import butterknife.ButterKnife;

public class Play extends AppCompatActivity {

    private final String TAG = "PLAY";
    private ArrayList<String> songs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        ButterKnife.bind(this);

        //TODO: Serialize song data, so that we can have a list of songs, not just paths
        songs = getIntent().getStringArrayListExtra("PLAY_PLAYLIST");

        play(songs.get(0));
    }

    public void play(String song) {
        final String s = song;
        final int sampleRate = 44100;
        final int bufferSize = 4096;
        new AndroidFFMPEGLocator(this);
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(song, sampleRate, bufferSize, 0);

        PitchDetectionHandler pdh = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
                final float pitchInHz = pitchDetectionResult.getPitch();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView tv = (TextView) findViewById(R.id.play_tempo);
                        tv.setText("" + pitchInHz);
                    }
                });
            }
        };
        
        AudioProcessor p = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, sampleRate, bufferSize, pdh);
        dispatcher.addAudioProcessor(p);
        dispatcher.addAudioProcessor(new AndroidAudioPlayer(dispatcher.getFormat(),5000, AudioManager.STREAM_MUSIC));

        new Thread(dispatcher, "Audio Dispatcher").start();
    }

}
