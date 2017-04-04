package com.musicretrieval.beatsbear.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.musicretrieval.beatsbear.Activities.Play;
import com.musicretrieval.beatsbear.Models.Song;
import com.musicretrieval.beatsbear.R;
import com.musicretrieval.beatsbear.Utils.CircularTextView;

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

    public class PlaylistViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.playlistitem_songname)   TextView songName;
        @BindView(R.id.playlistitem_artist)     TextView songArtist;
        @BindView(R.id.playlistitem_bpm)        CircularTextView songBpm;
        @BindView(R.id.playlistitem_genre)      ImageView songGenre;

        public PlaylistViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        /**
         * Sets the title, artist, and bpm of the song to the UI
         * @param song the input song
         */
        public void bindMetaData(Song song) {
            songName.setText(song.getTitle());
            songArtist.setText(song.getArtist());

            songBpm.setTextSize(12f);
            songBpm.setText(String.valueOf(song.getFeatures().bpm));
            songBpm.setSolidColor(getBpmColor(song.getFeatures().bpm));
            songBpm.setTextColor(Color.WHITE);

            switch(song.getFeatures().genre) {
                case "Blues":
                    songGenre.setBackgroundResource(R.drawable.blues);
                    break;
                case "Classical":
                    songGenre.setBackgroundResource(R.drawable.classical);
                    break;
                case "Country":
                    songGenre.setBackgroundResource(R.drawable.country);
                    break;
                case "Disco":
                    songGenre.setBackgroundResource(R.drawable.disco);
                    break;
                case "HipHop":
                    songGenre.setBackgroundResource(R.drawable.hiphop);
                    break;
                case "Jazz":
                    songGenre.setBackgroundResource(R.drawable.jazz);
                    break;
            }

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
