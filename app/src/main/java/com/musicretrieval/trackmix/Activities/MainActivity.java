package com.musicretrieval.trackmix.Activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.musicretrieval.trackmix.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.main_walk_button)   Button mWalkButton;
    @BindView(R.id.main_jog_button)    Button mJogButton;
    @BindView(R.id.main_run_button)    Button mRunButton;
    @BindView(R.id.main_sprint_button) Button mSprintButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.main_walk_button)
    public void StartWalk() {
        Toast.makeText(this, "start walking", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, Playlist.class);
        intent.putExtra("SONG_LIST_BPM", new int[] {90, 137});
        startActivity(intent);
    }

    @OnClick(R.id.main_jog_button)
    public void StartJog() {
        Toast.makeText(this, "start jogging", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, Playlist.class);
        intent.putExtra("SONG_LIST_BPM", new int[] {137, 147});
        startActivity(intent);
    }

    @OnClick(R.id.main_run_button)
    public void StartRun() {
        Toast.makeText(this, "start run", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, Playlist.class);
        intent.putExtra("SONG_LIST_BPM", new int[] {147, 160});
        startActivity(intent);
    }

    @OnClick(R.id.main_sprint_button)
    public void StartSprint() {
        Toast.makeText(this, "start sprint", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, Playlist.class);
        intent.putExtra("SONG_LIST_BPM", new int[] {160, 250});
        startActivity(intent);
    }
}
