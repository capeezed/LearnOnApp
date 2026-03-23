package com.learnon.app.data.model;

import com.google.gson.annotations.SerializedName;

public class Pedido {

    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("topic_tag")
    private String topicTag;

    @SerializedName("format_preference")
    private String formatPreference;

    @SerializedName("urgency")
    private String urgency;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getTopicTag() { return topicTag; }
    public String getFormatPreference() { return formatPreference; }
    public String getUrgency() { return urgency; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}