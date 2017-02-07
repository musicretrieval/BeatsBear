package com.musicretrieval.trackmix.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.musicretrieval.trackmix.Models.Song;
import com.musicretrieval.trackmix.R;
import com.musicretrieval.trackmix.Utils.CircularTextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    // List of songs to display
    private ArrayList<Song> songs;
    private Context context;

    public PlaylistAdapter(Context context, ArrayList<Song> songs) {
        this.context = context;
        this.songs = songs;
    }

    @Override
    public PlaylistViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View playlistItemView = inflater.inflate(R.layout.playlistitem, parent, false);
        return new PlaylistViewHolder(playlistItemView);
    }

    @Override
    public void onBindViewHolder(PlaylistViewHolder holder, int position) {
        Song song = songs.get(position);

        holder.bindMetaData(song);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    private Context getContext() {
        return context;
    }

    public static class PlaylistViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.playlistitem_songname)
        TextView songName;
        @BindView(R.id.playlistitem_artist)
        TextView songArtist;
        @BindView(R.id.playlistitem_bpm)
        CircularTextView songBpm;

        public PlaylistViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            Toast.makeText(view.getContext(), "Clicked", Toast.LENGTH_SHORT).show();
        }

        public void bindMetaData(Song song) {
            songName.setText(song.getName());
            songArtist.setText(song.getArtist());

            songBpm.setText(String.valueOf(song.getBpm()));
            songBpm.setSolidColor(getBpmColor(song.getBpm()));
            songBpm.setTextColor(Color.WHITE);
        }

        /**
         * Returns a color in hex format (#RRGGBB) based on the beats per minute of a song.
         * The range is between 0 (blue) and 255 (red)
         * @param bpm the input bpm of the song
         * @return a color between blue and red
         */
        private String getBpmColor(int bpm){
            if (bpm > 255) bpm = 255;
            int rgb[] = new int[3];
            int r[] = {255, 0, 0};
            int b[] = {0, 0, 255};
            for (int i = 0; i < 3; i++) {
                rgb[i] = Math.round(rgb[i] + bpm * (b[i] - r[i]));
            }
            return String.format("#%06X", (0xFFFFFF & ((rgb[0]&0x0ff)<<16)|((rgb[1]&0x0ff)<<8)|(rgb[2]&0x0ff)));
        }
    }
}
