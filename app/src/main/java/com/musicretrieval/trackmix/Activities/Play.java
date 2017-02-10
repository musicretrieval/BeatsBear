package com.musicretrieval.trackmix.Activities;

import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.musicretrieval.trackmix.Models.Song;
import com.musicretrieval.trackmix.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.GainProcessor;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.tarsos.dsp.beatroot.Agent;
import be.tarsos.dsp.beatroot.AgentList;
import be.tarsos.dsp.beatroot.Event;
import be.tarsos.dsp.beatroot.EventList;
import be.tarsos.dsp.beatroot.Induction;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.BeatRootSpectralFluxOnsetDetector;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class Play extends AppCompatActivity {

    private final String TAG = "PLAY";
    private ArrayList<Song> songs;

    private AudioDispatcher dispatcher;
    private WaveformSimilarityBasedOverlapAdd wsola;
    private AndroidAudioPlayer audioPlayer;

    private double tempo = 1.0;
    private final int SAMPLE_RATE = 44100;

    private final int SEQUENCE_MODEL = 82;
    private final int WINDOW_MODEL   = 28;
    private final int OVERLAP_MODEL  = 12;

    @BindView(R.id.play_increase_tempo_btn)
    Button increaseTempo;
    @BindView(R.id.play_decrease_tempo_btn)
    Button decreaseTempo;
    @BindView(R.id.play_tempo)
    TextView songTempo;
    @BindView(R.id.play_song_title)
    TextView songTitle;
    @BindView(R.id.play_song_image)
    ImageView songImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        ButterKnife.bind(this);

        songs = (ArrayList<Song>)getIntent().getSerializableExtra("PLAY_PLAYLIST");

        play(songs.get(0));
    }

    public void play(Song song) {
        songTitle.setText(song.getTitle());
        String tempoString = String.format(Locale.getDefault(),"%.2fx", tempo);
        songTempo.setText(tempoString);

        new AndroidFFMPEGLocator(this);
        try {
            wsola = new WaveformSimilarityBasedOverlapAdd(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(tempo, SAMPLE_RATE));

            dispatcher = AudioDispatcherFactory.fromPipe(song.getData(), SAMPLE_RATE , wsola.getInputBufferSize(), wsola.getOverlap());
            audioPlayer = new AndroidAudioPlayer(dispatcher.getFormat(), wsola.getInputBufferSize(), AudioManager.STREAM_MUSIC);

            wsola.setDispatcher(dispatcher);

            dispatcher.addAudioProcessor(wsola);
            dispatcher.addAudioProcessor(audioPlayer);

            new Thread(dispatcher, "Audio Dispatcher").start();
        } catch (Exception ex) {
            Log.d(TAG, ex.getMessage());
        }
    }

    @OnClick(R.id.play_increase_tempo_btn)
    public void increaseTempo() {
        tempo += 0.05;
        String tempoString = String.format(Locale.getDefault(),"%.2fx", tempo);
        wsola.setParameters(new WaveformSimilarityBasedOverlapAdd.Parameters(tempo, SAMPLE_RATE, SEQUENCE_MODEL, WINDOW_MODEL, OVERLAP_MODEL));
        songTempo.setText(tempoString);
    }

    @OnClick(R.id.play_decrease_tempo_btn)
    public void decreaseTempo() {
        tempo -= 0.05;
        String tempoString = String.format(Locale.getDefault(),"%.2fx", tempo);
        wsola.setParameters(new WaveformSimilarityBasedOverlapAdd.Parameters(tempo, SAMPLE_RATE, SEQUENCE_MODEL, WINDOW_MODEL, OVERLAP_MODEL));
        songTempo.setText(tempoString);
    }

}
