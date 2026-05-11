package com.learnon.app.data.model;

import com.google.gson.annotations.SerializedName;

public class VideoProgress {
    @SerializedName("watched_seconds")
    private int watchedSeconds;

    @SerializedName("total_seconds")
    private int totalSeconds;

    @SerializedName("percent_complete")
    private double percentComplete;

    @SerializedName("last_position_sec")
    private int lastPositionSec;

    public int getWatchedSeconds() { return watchedSeconds; }
    public int getTotalSeconds() { return totalSeconds; }
    public double getPercentComplete() { return percentComplete; }
    public int getLastPositionSec() { return lastPositionSec; }
}
