package com.musicretrieval.trackmix.DialogFragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.musicretrieval.trackmix.Adapters.PlaylistAdapter;
import com.musicretrieval.trackmix.Models.Song;
import com.musicretrieval.trackmix.R;

import java.util.ArrayList;


public class AddSongDialogFragment extends DialogFragment {

    private ArrayList<Song> songs;

    private RecyclerView songList;
    private PlaylistAdapter adapter;
    private LinearLayoutManager layoutManager;

    /**
     * Create a new instance of AddSongDialogFragment, providing "songs"
     * as an argument.
     */
    public static AddSongDialogFragment newInstance(ArrayList<Song> songs) {
        AddSongDialogFragment f = new AddSongDialogFragment();

        Bundle args = new Bundle();
        args.putSerializable("ADDSONG_SONGS", songs);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        songs = (ArrayList<Song>) getArguments().getSerializable("ADDSONG_SONGS");
        adapter = new PlaylistAdapter(getActivity(), songs);
        layoutManager = new LinearLayoutManager(getActivity());
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_fragment_songlist, container, false);
        getDialog().setTitle("Add Song");
        songList = (RecyclerView) v.findViewById(R.id.songlist_add);
        songList.setAdapter(adapter);
        songList.setLayoutManager(layoutManager);
        adapter.notifyDataSetChanged();
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog d = getDialog();
        if (d != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            d.getWindow().setLayout(width, height);
        }
    }
}
