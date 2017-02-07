package com.musicretrieval.trackmix.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.musicretrieval.trackmix.Models.Song;
import com.musicretrieval.trackmix.R;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PlaylistAdapter extends ArrayAdapter<Song> {
    public PlaylistAdapter(Context context, int resource, ArrayList<Song> songs) {
        super(context, resource, songs);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Song song = getItem(position);

        PlaylistViewHolder vh;
        if (convertView == null) {
            vh = new PlaylistViewHolder(convertView);
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.playlistitem, parent, false);

            vh.songName.setText(song.GetName());
        }

        return convertView;
    }

    private static class PlaylistViewHolder {
        @BindView(R.id.playlistitem_songname)
        TextView songName;
        @BindView(R.id.playlistitem_bpm)
        ImageView songBpm;

        public PlaylistViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
