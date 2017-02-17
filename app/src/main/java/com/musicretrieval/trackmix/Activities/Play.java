package com.musicretrieval.trackmix.Activities;

import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.musicretrieval.trackmix.Models.Song;
import com.musicretrieval.trackmix.R;

import com.musicretrieval.trackmix.Utils.TimeUtil;

import java.util.ArrayList;
import java.util.Locale;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;


public class Play extends AppCompatActivity {

    private final String TAG = "PLAY";
    private ArrayList<Song> songs;

    private AudioDispatcher dispatcher;
    private WaveformSimilarityBasedOverlapAdd wsola;
    private AndroidAudioPlayer audioPlayer;
    private AudioProcessor processor;

    private double tempo = 1.0;
    private final int SAMPLE_RATE = 44100;

    private final int SEQUENCE_MODEL = 82;
    private final int WINDOW_MODEL   = 28;
    private final int OVERLAP_MODEL  = 12;

    private boolean playing;
    private Song currentSong;
    private long currentTime;
    private double currentBpm;

    @BindView(R.id.play_increase_tempo_btn)
    Button increaseTempoBtn;
    @BindView(R.id.play_decrease_tempo_btn)
    Button decreaseTempoBtn;
    @BindView(R.id.play_tempo)
    TextView songTempo;
    @BindView(R.id.play_song_title)
    TextView songTitle;
    @BindView(R.id.play_song_image)
    ImageView songImage;

    @BindView(R.id.play_current_song_time)
    TextView currentSongTimeTv;
    @BindView(R.id.play_current_song_total_time)
    TextView currentSongTotalTimeTv;
    @BindView(R.id.play_seek_bar)
    SeekBar songSeekBar;

    @BindView(R.id.play_play_btn)
    Button songPlayBtn;
    @BindView(R.id.play_pause_btn)
    Button songPauseBtn;
    @BindView(R.id.play_rewind_btn)
    Button songRewindBtn;
    @BindView(R.id.play_forward_btn)
    Button songForwardBtn;
    @BindView(R.id.play_repeat_btn)
    Button songRepeatBtn;
    @BindView(R.id.play_shuffle_btn)
    Button songShuffleBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        ButterKnife.bind(this);

        songs = (ArrayList<Song>)getIntent().getSerializableExtra("PLAY_PLAYLIST");
        currentSong = songs.get(0);
        currentBpm = currentSong.getBpm();

        currentSongTimeTv.setText(TimeUtil.secondsToMMSS(currentTime));
        currentSongTotalTimeTv.setText(TimeUtil.secondsToMMSS(currentSong.getDuration()/1000));
        songSeekBar.setMax(1000);
        songSeekBar.setProgress(0);

        play(currentSong, 0);
        playing = true;

        songPlayBtn.setVisibility(View.GONE);
        songPauseBtn.setVisibility(View.VISIBLE);
    }

    public void play(final Song song, long seconds) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Things that need to be updated on UI thread, otherwise it will crash
                songTitle.setText(song.getTitle());
                String tempoString = String.format(Locale.getDefault(),"%.2fx", tempo);
                songTempo.setText(tempoString);
            }
        });

        new AndroidFFMPEGLocator(this);
        try {
            tempo = currentBpm / song.getBpm();
            wsola = new WaveformSimilarityBasedOverlapAdd(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(tempo, SAMPLE_RATE));

            dispatcher = AudioDispatcherFactory.fromPipe(song.getData(), SAMPLE_RATE , wsola.getInputBufferSize(), wsola.getOverlap());
            audioPlayer = new AndroidAudioPlayer(dispatcher.getFormat(), wsola.getInputBufferSize(), AudioManager.STREAM_MUSIC);
            processor = new AudioProcessor() {
                @Override
                public boolean process(AudioEvent audioEvent) {
                    if (playing) {
                        updateTimes();
                    }
                    return false;
                }

                @Override
                public void processingFinished() {
                    if (playing) {
                        playNextSong();
                    }
                }
            };

            wsola.setDispatcher(dispatcher);
            dispatcher.skip(seconds);
            dispatcher.addAudioProcessor(wsola);
            dispatcher.addAudioProcessor(audioPlayer);
            dispatcher.addAudioProcessor(processor);
            new Thread(dispatcher, "Audio Dispatcher").start();
        } catch (Exception ex) {
            Log.d(TAG, ex.getMessage());
        }
    }

    public void updateTimes() {
        currentTime = (long) dispatcher.secondsProcessed();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                double percent = currentTime / (currentSong.getDuration()/1000.0);
                int progress =  (int) (percent * songSeekBar.getMax());
                // Things that need to be updated on UI thread, otherwise it will crash
                currentSongTimeTv.setText(TimeUtil.secondsToMMSS(currentTime));
                songSeekBar.setProgress(progress);
            }
        });
    }

    public void playNextSong() {
        int currentSongIndex = songs.indexOf(currentSong);
        if (currentSongIndex + 1 == songs.size()) {
            // play from beginning
            currentSongIndex = -1;
        }
        Song nextSong = songs.get(currentSongIndex + 1);
        play(nextSong, 0);
    }

    @OnLongClick(R.id.play_increase_tempo_btn)
    public boolean longIncreaseTempo() {
        tempo += 0.05;
        currentBpm = (int) (currentSong.getBpm() * tempo);
        String tempoString = String.format(Locale.getDefault(),"%.2fx", tempo);
        wsola.setParameters(new WaveformSimilarityBasedOverlapAdd.Parameters(tempo, SAMPLE_RATE, SEQUENCE_MODEL, WINDOW_MODEL, OVERLAP_MODEL));
        songTempo.setText(tempoString);
        return true;
    }

    @OnClick(R.id.play_increase_tempo_btn)
    public void increaseTempo() {
        tempo += 0.01;
        currentBpm = (int) (currentSong.getBpm() * tempo);
        String tempoString = String.format(Locale.getDefault(),"%.2fx", tempo);
        wsola.setParameters(new WaveformSimilarityBasedOverlapAdd.Parameters(tempo, SAMPLE_RATE, SEQUENCE_MODEL, WINDOW_MODEL, OVERLAP_MODEL));
        songTempo.setText(tempoString);
    }

    @OnLongClick(R.id.play_decrease_tempo_btn)
    public boolean longDecreaseTempo() {
        tempo -= 0.05;
        currentBpm = (int) (currentSong.getBpm() * tempo);
        String tempoString = String.format(Locale.getDefault(),"%.2fx", tempo);
        wsola.setParameters(new WaveformSimilarityBasedOverlapAdd.Parameters(tempo, SAMPLE_RATE, SEQUENCE_MODEL, WINDOW_MODEL, OVERLAP_MODEL));
        songTempo.setText(tempoString);
        return true;
    }

    @OnClick(R.id.play_decrease_tempo_btn)
    public void decreaseTempo() {
        tempo -= 0.01;
        currentBpm = (int) (currentSong.getBpm() * tempo);
        String tempoString = String.format(Locale.getDefault(),"%.2fx", tempo);
        wsola.setParameters(new WaveformSimilarityBasedOverlapAdd.Parameters(tempo, SAMPLE_RATE, SEQUENCE_MODEL, WINDOW_MODEL, OVERLAP_MODEL));
        songTempo.setText(tempoString);
    }

    @OnClick(R.id.play_play_btn)
    public void togglePlay() {
        songPlayBtn.setVisibility(View.GONE);
        songPauseBtn.setVisibility(View.VISIBLE);
        if (!playing) {
            play(currentSong, currentTime);
            playing = true;
        }
    }

    @OnClick(R.id.play_pause_btn)
    public void togglePause() {
        songPlayBtn.setVisibility(View.VISIBLE);
        songPauseBtn.setVisibility(View.GONE);
        if (playing) {
            currentTime = (long) dispatcher.secondsProcessed();
            playing = false;
            dispatcher.stop();
        }
    }
}
