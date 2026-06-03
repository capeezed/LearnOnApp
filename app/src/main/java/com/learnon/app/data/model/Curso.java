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

    @SerializedName("price")
    private double price;

    @SerializedName("access_status")
    private String accessStatus;

    @SerializedName("payment_status")
    private String paymentStatus;

    @SerializedName("payment_id")
    private Long paymentId;

    @SerializedName("checkout_url")
    private String checkoutUrl;

    @SerializedName("enrolled_at")
    private String enrolledAt;

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getFormat() { return format; }
    public String getTopicTag() { return topicTag; }
    public int getDurationMinutes() { return durationMinutes; }
    public int getProgress() { return progress; }
    public double getPrice() { return price; }
    public String getAccessStatus() { return accessStatus; }
    public String getPaymentStatus() { return paymentStatus; }
    public Long getPaymentId() { return paymentId; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public String getEnrolledAt() { return enrolledAt; }

    public boolean isPaymentRequired() {
        return "payment_required".equals(accessStatus) || "processing".equals(accessStatus);
    }
}
