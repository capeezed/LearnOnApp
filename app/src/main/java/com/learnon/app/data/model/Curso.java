package com.learnon.app.data.model;

import com.google.gson.annotations.SerializedName;

public class Curso {

    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("format")
    private String format;

    @SerializedName("topic_tag")
    private String topicTag;

    @SerializedName("duration_minutes")
    private int durationMinutes;

    @SerializedName("progress")
    private int progress;

    @SerializedName("enrolled_at")
    private String enrolledAt;

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getFormat() { return format; }
    public String getTopicTag() { return topicTag; }
    public int getDurationMinutes() { return durationMinutes; }
    public int getProgress() { return progress; }
    public String getEnrolledAt() { return enrolledAt; }
}