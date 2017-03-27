package com.musicretrieval.tempobear.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

import com.musicretrieval.tempobear.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.main_all_button)    Button allButton;
    @BindView(R.id.main_walk_button)   Button walkButton;
    @BindView(R.id.main_run_button)    Button runButton;
    @BindView(R.id.main_sprint_button) Button sprintButton;

    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
    }

    @OnClick(R.id.main_all_button)
    public void startAll() {
        Intent intent = new Intent(this, Playlist.class);
        intent.putExtra("SONG_LIST_BPM_LOW", 0);
        intent.putExtra("SONG_LIST_BPM_HIGH", 999);
        intent.putExtra("SONG_RECOMMEND", true);
        sharedPref.edit()
                .remove("SONG_LIST_BPM_LOW")
                .remove("SONG_LIST_BPM_HIGH")
                .putInt("SONG_LIST_BPM_LOW", 0)
                .putInt("SONG_LIST_BPM_HIGH", 999)
                .apply();
        startActivity(intent);
    }

    @OnClick(R.id.main_walk_button)
    public void startWalk() {
        Intent intent = new Intent(this, Playlist.class);
        intent.putExtra("SONG_LIST_BPM_LOW", 60);
        intent.putExtra("SONG_LIST_BPM_HIGH", 137);
        intent.putExtra("SONG_RECOMMEND", true);
        sharedPref.edit()
                .remove("SONG_LIST_BPM_LOW")
                .remove("SONG_LIST_BPM_HIGH")
                .putInt("SONG_LIST_BPM_LOW", 60)
                .putInt("SONG_LIST_BPM_HIGH", 137)
                .apply();
        startActivity(intent);
    }

    @OnClick(R.id.main_run_button)
    public void startRun() {
        Intent intent = new Intent(this, Playlist.class);
        intent.putExtra("SONG_LIST_BPM_LOW", 137);
        intent.putExtra("SONG_LIST_BPM_HIGH", 160);
        intent.putExtra("SONG_RECOMMEND", true);
        sharedPref.edit()
                .remove("SONG_LIST_BPM_LOW")
                .remove("SONG_LIST_BPM_HIGH")
                .putInt("SONG_LIST_BPM_LOW", 137)
                .putInt("SONG_LIST_BPM_HIGH", 160)
                .apply();
        startActivity(intent);
    }

    @OnClick(R.id.main_sprint_button)
    public void startSprint() {
        Intent intent = new Intent(this, Playlist.class);
        intent.putExtra("SONG_LIST_BPM_LOW", 160);
        intent.putExtra("SONG_LIST_BPM_HIGH", 300);
        intent.putExtra("SONG_RECOMMEND", true);
        sharedPref.edit()
                .remove("SONG_LIST_BPM_LOW")
                .remove("SONG_LIST_BPM_HIGH")
                .putInt("SONG_LIST_BPM_LOW", 160)
                .putInt("SONG_LIST_BPM_HIGH", 300)
                .apply();
        startActivity(intent);
    }
}
