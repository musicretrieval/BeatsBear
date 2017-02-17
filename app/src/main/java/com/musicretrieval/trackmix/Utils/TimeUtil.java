package com.musicretrieval.trackmix.Utils;

import java.util.Locale;

public class TimeUtil {
    public static String secondsToMMSS(long seconds) {
        int min = (int) (seconds/60.0);
        int sec = (int) (seconds%60.0);
        String minString = String.format(Locale.getDefault(), "%d", min);
        String secString = String.format(Locale.getDefault(), "%d", sec);
        if (sec < 10) {
            secString = String.format(Locale.getDefault(), "0%d", sec);
        }
        return String.format(Locale.getDefault(), "%s:%s", minString, secString);
    }
}
