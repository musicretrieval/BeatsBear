package com.musicretrieval.beatsbear.Activities;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.musicretrieval.beatsbear.Adapters.ItemClickSupport;
import com.musicretrieval.beatsbear.Adapters.PlaylistAdapter;
import com.musicretrieval.beatsbear.Models.Song;
import com.musicretrieval.beatsbear.R;
import com.musicretrieval.beatsbear.Utils.PlaylistType;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PlaylistFragment extends Fragment {
    private static final String SONGS_PARAM = "SONGS_PARAM";

    @BindView(R.id.main_all_button)         Button allButton;
    @BindView(R.id.main_relaxing_button)    Button relaxingButton;
    @BindView(R.id.main_activating_button)  Button activatingButton;

    @BindView(R.id.playlist)                RecyclerView playlistView;
    @BindView(R.id.playlist_add)            FloatingActionButton playlistAdd;
    @BindView(R.id.playlist_play)           FloatingActionButton playlistPlay;

    private PlaylistAdapter adapter;

    private ArrayList<Song> songs;

    private OnPlaySongsListener playListener;
    private OnPlaylistSwitchListener playlistSwitchListener;

    public PlaylistFragment() {
        // Required empty public constructor
    }

    public static PlaylistFragment newInstance(ArrayList<Song> songs) {
        PlaylistFragment fragment = new PlaylistFragment();
        Bundle args = new Bundle();
        args.putSerializable(SONGS_PARAM, songs);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            songs = (ArrayList<Song>) getArguments().getSerializable(SONGS_PARAM);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        ButterKnife.bind(this, view);

        adapter = new PlaylistAdapter(getActivity(), songs);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        playlistView.setAdapter(adapter);
        playlistView.setLayoutManager(layoutManager);
        ItemClickSupport.addTo(playlistView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                playListener.onPlaySongsPressed(position);
            }
        });
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnPlaySongsListener) {
            playListener = (OnPlaySongsListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnPlaySongsListener");
        }

        if (context instanceof OnPlaylistSwitchListener) {
            playlistSwitchListener = (OnPlaylistSwitchListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnPlaylistSwitchListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        playListener = null;
        playlistSwitchListener = null;
    }

    public void toggleButtonState(Button button) {
        switch (button.getId()) {
            case R.id.main_all_button:
                button.setBackgroundResource(R.drawable.button_bg_selected);
                button.setTextColor(Color.WHITE);
                relaxingButton.setBackgroundResource(R.drawable.button_bg_not_selected);
                relaxingButton.setTextColor(ContextCompat.getColor(getActivity(), R.color.colorAccent));
                activatingButton.setBackgroundResource(R.drawable.button_bg_not_selected);
                activatingButton.setTextColor(ContextCompat.getColor(getActivity(), R.color.colorAccent));
                break;
            case R.id.main_relaxing_button:
                button.setBackgroundResource(R.drawable.button_bg_selected);
                button.setTextColor(Color.WHITE);
                allButton.setBackgroundResource(R.drawable.button_bg_not_selected);
                allButton.setTextColor(ContextCompat.getColor(getActivity(), R.color.colorAccent));
                activatingButton.setBackgroundResource(R.drawable.button_bg_not_selected);
                activatingButton.setTextColor(ContextCompat.getColor(getActivity(), R.color.colorAccent));
                break;
            case R.id.main_activating_button:
                button.setBackgroundResource(R.drawable.button_bg_selected);
                button.setTextColor(Color.WHITE);
                allButton.setBackgroundResource(R.drawable.button_bg_not_selected);
                allButton.setTextColor(ContextCompat.getColor(getActivity(), R.color.colorAccent));
                relaxingButton.setBackgroundResource(R.drawable.button_bg_not_selected);
                relaxingButton.setTextColor(ContextCompat.getColor(getActivity(), R.color.colorAccent));
                break;
        }
    }

    @OnClick(R.id.main_all_button)
    public void startAll() {
        toggleButtonState(allButton);
        if (playlistSwitchListener != null) {
            playlistSwitchListener.onPlaylistTypeChanged(PlaylistType.PLAYALL);
        }
    }

    @OnClick(R.id.main_relaxing_button)
    public void startRelaxing() {
        toggleButtonState(relaxingButton);
        if (playlistSwitchListener != null) {
            playlistSwitchListener.onPlaylistTypeChanged(PlaylistType.PLAYRELAXING);
        }
    }

    @OnClick(R.id.main_activating_button)
    public void startActivating() {
        toggleButtonState(activatingButton);
        if (playlistSwitchListener != null) {
            playlistSwitchListener.onPlaylistTypeChanged(PlaylistType.PLAYACTIVATING);
        }
    }

    @OnClick(R.id.playlist_play)
    public void startPlaylist() {
        playListener.onPlaySongsPressed(0);
    }

    public void switchPlaylist(ArrayList<Song> s) {
        songs.clear();
        songs.addAll(s);
        adapter.notifyDataSetChanged();
    }

    public interface OnPlaySongsListener {
        void onPlaySongsPressed(int position);
    }

    public interface OnPlaylistSwitchListener {
        void onPlaylistTypeChanged(PlaylistType type);
    }
}
