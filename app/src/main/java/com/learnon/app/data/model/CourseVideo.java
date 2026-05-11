package com.learnon.app.data.model;

import com.google.gson.annotations.SerializedName;

public class CourseVideo {
    @SerializedName("id")
    private int id;

    @SerializedName("course_id")
    private int courseId;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("video_url")
    private String videoUrl;

    @SerializedName("thumbnail_url")
    private String thumbnailUrl;

    @SerializedName("duration")
    private int duration;

    @SerializedName("order_index")
    private int orderIndex;

    @SerializedName("progress")
    private VideoProgress progress;

    public int getId() { return id; }
    public int getCourseId() { return courseId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getVideoUrl() { return videoUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public int getDuration() { return duration; }
    public int getOrderIndex() { return orderIndex; }
    public VideoProgress getProgress() { return progress; }

    public int getLastPositionSec() {
        return progress == null ? 0 : progress.getLastPositionSec();
    }

    public int getPercentComplete() {
        return progress == null ? 0 : Math.round((float) progress.getPercentComplete());
    }
}
