package com.musicretrieval.trackmix.Models;

/**
 * Song is the base class for all song contexts. A Song object encapsulates the state information
 * for a particular musical piece. The state information includes:
 * <ul>
 *     <li> Name
 *     <li> Artist
 *     <li> Beats per minute (BPM)
 * </ul>
 */
public class Song {
    public static final int MINIMUM_BPM = 60;
    public static final int MAXIMUM_BPM = 400;

    private String name;
    private String artist;
    private int bpm;

    /**
     * Song constructor
     * @param name name of the song
     * @param artist artist of the song
     * @param bpm beats per minute of the song
     */
    public Song(String name, String artist, int bpm) {
        this.name = name;
        this.artist = artist;
        this.bpm = bpm;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public int getBpm() { return bpm; }
    public void setBpm(int bpm) { this.bpm = bpm; }
}
