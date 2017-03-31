package com.musicretrieval.beatsbear.Activities;

import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.musicretrieval.beatsbear.Models.Song;
import com.musicretrieval.beatsbear.R;

import com.musicretrieval.beatsbear.Utils.TimeUtil;

import java.util.ArrayList;
import java.util.LinkedList;
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


public class Play extends AppCompatActivity {

    private final String TAG = "PLAY";
    private LinkedList<Song> songQ;

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

        songQ = new LinkedList<>();
        songQ.addAll((ArrayList<Song>)getIntent().getSerializableExtra("PLAY_PLAYLIST"));

        currentSong = songQ.remove();
        currentBpm = currentSong.getFeatures().bpm;
        songQ.add(currentSong);

        currentSongTimeTv.setText(TimeUtil.secondsToMMSS(currentTime));
        currentSongTotalTimeTv.setText(TimeUtil.secondsToMMSS(currentSong.getDuration()/1000));
        songSeekBar.setMax(1000);
        songSeekBar.setProgress(0);

        play(0);
        playing = true;

        songPlayBtn.setVisibility(View.GONE);
        songPauseBtn.setVisibility(View.VISIBLE);

        setIncreaseTempoOnTouchListener();
        setDecreaseTempoOnTouchListener();
        setSeekbarListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void play(long seconds) {
        new AndroidFFMPEGLocator(this);

        try {
            tempo = currentBpm / currentSong.getFeatures().bpm;
            wsola = new WaveformSimilarityBasedOverlapAdd(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(tempo, SAMPLE_RATE));

            dispatcher = AudioDispatcherFactory.fromPipe(currentSong.getData(), SAMPLE_RATE , wsola.getInputBufferSize(), wsola.getOverlap());
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

        updateSongInfoUI();
    }

    public void updateTimes() {
        currentTime = (long) dispatcher.secondsProcessed();
        updateSeekBarUI();
    }

    public void playNextSong() {
        currentSong = songQ.pop();
        songQ.add(currentSong);
        play(0);
    }

    public void playPreviousSong() {
        currentSong = songQ.removeLast();
        songQ.add(currentSong);
        play(0);
    }

    void updateSeekBarUI() {
        // Things that need to be updated on UI thread, otherwise it will crash
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                long totalTime = currentSong.getDuration()/1000;
                double percent = currentTime / (currentSong.getDuration()/1000.0);
                int progress =  (int) (percent * songSeekBar.getMax());
                currentSongTimeTv.setText(TimeUtil.secondsToMMSS(currentTime));
                currentSongTotalTimeTv.setText(TimeUtil.secondsToMMSS(totalTime));
                songSeekBar.setProgress(progress);
            }
        });
    }

    void updateSongInfoUI() {
        // Things that need to be updated on UI thread, otherwise it will crash
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                songTitle.setText(currentSong.getTitle());
                String tempoString = String.format(Locale.getDefault(),"%.2fx", tempo);
                songTempo.setText(tempoString);
            }
        });
    }

    private void stopDispatcher() {
        if (dispatcher != null) {
            dispatcher.stop();
        }
    }

    private void setIncreaseTempoOnTouchListener() {
        increaseTempoBtn.setOnTouchListener(new View.OnTouchListener() {
            private Handler handler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (handler != null) return true;
                        handler = new Handler();
                        handler.postDelayed(increaseTempoAction, 300);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (handler == null) return true;
                        handler.removeCallbacks(increaseTempoAction);
                        handler = null;
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        if (handler == null) return true;
                        handler.removeCallbacks(increaseTempoAction);
                        handler = null;
                        break;
                }
                return false;
            }

            Runnable increaseTempoAction = new Runnable() {
                @Override public void run() {
                    increaseTempoLong();
                    handler.postDelayed(this, 300);
                }
            };
        });
    }

    private void setDecreaseTempoOnTouchListener() {
        decreaseTempoBtn.setOnTouchListener(new View.OnTouchListener() {
            private Handler handler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        if (handler != null) return true;
                        handler = new Handler();
                        handler.postDelayed(decreaseTempoAction, 300);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (handler == null) return true;
                        handler.removeCallbacks(decreaseTempoAction);
                        handler = null;
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        if (handler == null) return true;
                        handler.removeCallbacks(decreaseTempoAction);
                        handler = null;
                        break;
                }
                return false;
            }

            Runnable decreaseTempoAction = new Runnable() {
                @Override public void run() {
                    decreaseTempoLong();
                    handler.postDelayed(this, 300);
                }
            };
        });
    }

    private void setSeekbarListener() {
        songSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                currentTime = (long) (seekBar.getProgress()/1000.0 * (currentSong.getDuration()/1000.0));
                playing = false;
                stopDispatcher();
                play(currentTime);
                playing = true;
                updateSeekBarUI();
            }
        });
    }

    private void increaseTempoLong() {
        tempo += 0.05;
        currentBpm = (int) (currentSong.getFeatures().bpm * tempo);
        String tempoString = String.format(Locale.getDefault(),"%.2fx", tempo);
        wsola.setParameters(new WaveformSimilarityBasedOverlapAdd.Parameters(tempo, SAMPLE_RATE, SEQUENCE_MODEL, WINDOW_MODEL, OVERLAP_MODEL));
        songTempo.setText(tempoString);
    }

    private void decreaseTempoLong() {
        tempo -= 0.05;
        currentBpm = (int) (currentSong.getFeatures().bpm * tempo);
        String tempoString = String.format(Locale.getDefault(),"%.2fx", tempo);
        wsola.setParameters(new WaveformSimilarityBasedOverlapAdd.Parameters(tempo, SAMPLE_RATE, SEQUENCE_MODEL, WINDOW_MODEL, OVERLAP_MODEL));
        songTempo.setText(tempoString);
    }

    @OnClick(R.id.play_increase_tempo_btn)
    public void increaseTempo() {
        tempo += 0.01;
        currentBpm = (int) (currentSong.getFeatures().bpm * tempo);
        String tempoString = String.format(Locale.getDefault(),"%.2fx", tempo);
        wsola.setParameters(new WaveformSimilarityBasedOverlapAdd.Parameters(tempo, SAMPLE_RATE, SEQUENCE_MODEL, WINDOW_MODEL, OVERLAP_MODEL));
        songTempo.setText(tempoString);
    }

    @OnClick(R.id.play_decrease_tempo_btn)
    public void decreaseTempo() {
        tempo -= 0.01;
        currentBpm = (int) (currentSong.getFeatures().bpm * tempo);
        String tempoString = String.format(Locale.getDefault(),"%.2fx", tempo);
        wsola.setParameters(new WaveformSimilarityBasedOverlapAdd.Parameters(tempo, SAMPLE_RATE, SEQUENCE_MODEL, WINDOW_MODEL, OVERLAP_MODEL));
        songTempo.setText(tempoString);
    }

    @OnClick(R.id.play_play_btn)
    public void togglePlay() {
        songPlayBtn.setVisibility(View.GONE);
        songPauseBtn.setVisibility(View.VISIBLE);
        if (!playing && dispatcher != null) {
            play(currentTime);
            playing = true;
        }
    }

    @OnClick(R.id.play_pause_btn)
    public void togglePause() {
        songPlayBtn.setVisibility(View.VISIBLE);
        songPauseBtn.setVisibility(View.GONE);
        if (playing && dispatcher != null) {
            currentTime = (long) dispatcher.secondsProcessed();
            playing = false;
            stopDispatcher();
        }
    }

    @OnClick(R.id.play_forward_btn)
    public void nextSong() {
        if (playing && dispatcher != null) {
            playing = false;
            stopDispatcher();
            playNextSong();
            playing = true;
        } else {
            currentSong = songQ.pop();
            songQ.add(currentSong);
            currentTime = 0;
            updateSongInfoUI();
            updateSeekBarUI();
        }
    }

    @OnClick(R.id.play_rewind_btn)
    public void previousSong() {
        if (playing && dispatcher != null) {
            playing = false;
            stopDispatcher();
            playPreviousSong();
            playing = true;
        } else {
            currentSong = songQ.removeLast();
            songQ.add(currentSong);
            currentTime = 0;
            updateSongInfoUI();
            updateSeekBarUI();
        }
    }
}
