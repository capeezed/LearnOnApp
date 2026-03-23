package com.learnon.app.data.model;

import com.google.gson.annotations.SerializedName;

public class Aula {

    @SerializedName("id")
    private int id;

    @SerializedName("course_title")
    private String courseTitle;

    @SerializedName("scheduled_at")
    private String scheduledAt;

    @SerializedName("duration_min")
    private int durationMin;

    @SerializedName("meeting_url")
    private String meetingUrl;

    @SerializedName("instructor_name")
    private String instructorName;

    public int getId() { return id; }
    public String getCourseTitle() { return courseTitle; }
    public String getScheduledAt() { return scheduledAt; }
    public int getDurationMin() { return durationMin; }
    public String getMeetingUrl() { return meetingUrl; }
    public String getInstructorName() { return instructorName; }
}