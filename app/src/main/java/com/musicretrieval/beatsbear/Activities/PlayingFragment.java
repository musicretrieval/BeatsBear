package com.musicretrieval.beatsbear.Activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.musicretrieval.beatsbear.Models.Song;
import com.musicretrieval.beatsbear.R;
import com.musicretrieval.beatsbear.Services.MediaPlayerService;
import com.musicretrieval.beatsbear.Utils.TimeUtil;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PlayingFragment extends Fragment {
    private static final String SONG_PARAM = "SONG_PARAM";

    private Song song;

    private OnPlayListener playListener;
    private OnNextListener nextListener;
    private OnPrevListener prevListener;
    private OnPauseListener pauseListener;
    private OnTempoListener tempoListener;
    private OnSeekListener seekListener;

    @BindView(R.id.play_increase_tempo_btn) Button increaseTempoBtn;
    @BindView(R.id.play_decrease_tempo_btn) Button decreaseTempoBtn;
    @BindView(R.id.play_tempo)              TextView songTempo;
    @BindView(R.id.play_song_title)         TextView songTitle;
    @BindView(R.id.play_song_image)         ImageView songImage;

    @BindView(R.id.play_current_song_time)  TextView currentSongTimeTv;
    @BindView(R.id.play_current_song_total_time) TextView currentSongTotalTimeTv;
    @BindView(R.id.play_seek_bar)           SeekBar songSeekBar;

    @BindView(R.id.play_play_btn)           Button songPlayBtn;
    @BindView(R.id.play_pause_btn)          Button songPauseBtn;
    @BindView(R.id.play_rewind_btn)         Button songRewindBtn;
    @BindView(R.id.play_forward_btn)        Button songForwardBtn;

    private double tempo = 1.0;
    private int currentBpm = 0;
    private long currentTime;

    private BroadcastReceiver currentTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentTime = intent.getLongExtra("CURRENT_TIME", 0);
            updateSeekBarUI();
        }
    };

    private BroadcastReceiver currentSongReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            song = (Song) intent.getSerializableExtra("CURRENT_SONG");
            updateSongInfoUI();
        }
    };

    public PlayingFragment() {
        // Required empty public constructor
    }

    public static PlayingFragment newInstance(Song song) {
        PlayingFragment fragment = new PlayingFragment();
        Bundle args = new Bundle();
        args.putSerializable(SONG_PARAM, song);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            song = (Song) getArguments().getSerializable(SONG_PARAM);
            currentBpm = song.getFeatures().bpm;
        }
        getActivity().registerReceiver(currentTimeReceiver, new IntentFilter(MediaPlayerService.CURRENT_TIME));
        getActivity().registerReceiver(currentSongReceiver, new IntentFilter(MediaPlayerService.CURRENT_SONG));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_playing, container, false);
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        ButterKnife.bind(this, view);

        songPlayBtn.setVisibility(View.GONE);
        songPauseBtn.setVisibility(View.VISIBLE);

        currentSongTimeTv.setText(TimeUtil.secondsToMMSS(currentTime));
        currentSongTotalTimeTv.setText(TimeUtil.secondsToMMSS(song.getDuration()/1000));
        songSeekBar.setMax(1000);
        songSeekBar.setProgress(0);

        updateSongInfoUI();

        setIncreaseTempoOnTouchListener();
        setDecreaseTempoOnTouchListener();
        setSeekbarListener();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(currentTimeReceiver);
        getActivity().unregisterReceiver(currentSongReceiver);
    }

    @OnClick(R.id.play_play_btn)
    public void play() {
        songPlayBtn.setVisibility(View.GONE);
        songPauseBtn.setVisibility(View.VISIBLE);
        if (playListener != null) {
            playListener.onPlayPressed();
        }
    }

    @OnClick(R.id.play_forward_btn)
    public void next() {
        if (nextListener != null) {
            nextListener.onNextPressed();
        }
    }

    @OnClick(R.id.play_rewind_btn)
    public void previous() {
        if (prevListener != null) {
            prevListener.onPrevPressed();
        }
    }

    @OnClick(R.id.play_pause_btn)
    public void pause() {
        songPlayBtn.setVisibility(View.VISIBLE);
        songPauseBtn.setVisibility(View.GONE);
        if (pauseListener != null) {
            pauseListener.onPausePressed();
        }
    }

    @OnClick(R.id.play_increase_tempo_btn)
    public void increaseTempo() {
        if (tempoListener != null) {
            tempo += 0.01;
            tempoListener.onTempoPressed(tempo);
            currentBpm = (int) (song.getFeatures().bpm * tempo);
            String tempoString = String.format(Locale.getDefault(),"%.2fx", tempo);
            songTempo.setText(tempoString);
        }
    }

    @OnClick(R.id.play_decrease_tempo_btn)
    public void decreaseTempo() {
        if (tempoListener != null) {
            tempo -= 0.01;
            tempoListener.onTempoPressed(tempo);
            currentBpm = (int) (song.getFeatures().bpm * tempo);
            String tempoString = String.format(Locale.getDefault(),"%.2fx", tempo);
            songTempo.setText(tempoString);
        }
    }

    public void increaseTempoLong() {
        if (tempoListener != null) {
            tempo += 0.05;
            tempoListener.onTempoPressed(tempo);
            currentBpm = (int) (song.getFeatures().bpm * tempo);
            String tempoString = String.format(Locale.getDefault(),"%.2fx", tempo);
            songTempo.setText(tempoString);
        }
    }

    public void decreaseTempoLong() {
        if (tempoListener != null) {
            tempo -= 0.05;
            tempoListener.onTempoPressed(tempo);
            currentBpm = (int) (song.getFeatures().bpm * tempo);
            String tempoString = String.format(Locale.getDefault(),"%.2fx", tempo);
            songTempo.setText(tempoString);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnPlayListener) {
            playListener = (OnPlayListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnPlayListener");
        }

        if (context instanceof OnNextListener) {
            nextListener = (OnNextListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnNextListener");
        }

        if (context instanceof OnPrevListener) {
            prevListener = (OnPrevListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnPrevListener");
        }

        if (context instanceof OnPauseListener) {
            pauseListener = (OnPauseListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnPauseListener");
        }

        if (context instanceof OnTempoListener) {
            tempoListener = (OnTempoListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnTempoListener");
        }

        if (context instanceof OnSeekListener) {
            seekListener = (OnSeekListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnSeekListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        playListener = null;
        nextListener = null;
        prevListener = null;
        pauseListener = null;
        tempoListener = null;
        seekListener = null;
    }

    public void updateSongInfoUI() {
        // Things that need to be updated on UI thread, otherwise it will crash
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                songTitle.setText(song.getTitle());
                String tempoString = String.format(Locale.getDefault(),"%.2fx", tempo);
                songTempo.setText(tempoString);
            }
        });
    }

    void updateSeekBarUI() {
        // Things that need to be updated on UI thread, otherwise it will crash
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                long totalTime = song.getDuration()/1000;
                double percent = currentTime / (song.getDuration()/1000.0);
                int progress =  (int) (percent * songSeekBar.getMax());
                currentSongTimeTv.setText(TimeUtil.secondsToMMSS(currentTime));
                currentSongTotalTimeTv.setText(TimeUtil.secondsToMMSS(totalTime));
                songSeekBar.setProgress(progress);
            }
        });
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
                currentTime = (long) (seekBar.getProgress()/1000.0 * (song.getDuration()/1000.0));
                seekListener.onSeekPressed(currentTime);
                updateSeekBarUI();
            }
        });
    }

    public interface OnPlayListener {
        void onPlayPressed();
    }

    public interface OnNextListener {
        void onNextPressed();
    }

    public interface OnPrevListener {
        void onPrevPressed();
    }

    public interface OnPauseListener {
        void onPausePressed();
    }

    public interface OnTempoListener {
        void onTempoPressed(double amount);
    }

    public interface OnSeekListener {
        void onSeekPressed(long seconds);
    }
}
