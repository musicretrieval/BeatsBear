package com.musicretrieval.trackmix.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.musicretrieval.trackmix.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.main_walk_button)   Button walkButton;
    @BindView(R.id.main_jog_button)    Button jogButton;
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

    @OnClick(R.id.main_walk_button)
    public void startWalk() {
        Toast.makeText(this, "start walking", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, Playlist.class);
        sharedPref.edit()
                .putInt("SONG_LIST_BPM_LOW", 90)
                .putInt("SONG_LIST_BPM_HIGH", 137)
                .apply();
        startActivity(intent);
    }

    @OnClick(R.id.main_jog_button)
    public void startJog() {
        Toast.makeText(this, "start jogging", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, Playlist.class);
        sharedPref.edit()
                .putInt("SONG_LIST_BPM_LOW", 137)
                .putInt("SONG_LIST_BPM_HIGH", 147)
                .apply();
        startActivity(intent);
    }

    @OnClick(R.id.main_run_button)
    public void startRun() {
        Toast.makeText(this, "start run", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, Playlist.class);
        sharedPref.edit()
                .putInt("SONG_LIST_BPM_LOW", 147)
                .putInt("SONG_LIST_BPM_HIGH", 160)
                .apply();
        startActivity(intent);
    }

    @OnClick(R.id.main_sprint_button)
    public void startSprint() {
        Toast.makeText(this, "start sprint", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, Playlist.class);
        sharedPref.edit()
                .putInt("SONG_LIST_BPM_LOW", 160)
                .putInt("SONG_LIST_BPM_HIGH", 300)
                .apply();
        startActivity(intent);
    }
}
