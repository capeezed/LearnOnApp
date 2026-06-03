package com.learnon.app.data.model;

import com.google.gson.annotations.SerializedName;

public class CoursePayment {

    @SerializedName("id")
    private long id;

    @SerializedName("course_id")
    private int courseId;

    @SerializedName("status")
    private String status;

    @SerializedName("provider")
    private String provider;

    @SerializedName("provider_preference_id")
    private String providerPreferenceId;

    @SerializedName("provider_payment_id")
    private String providerPaymentId;

    @SerializedName("amount")
    private double amount;

    @SerializedName("currency")
    private String currency;

    @SerializedName("checkout_url")
    private String checkoutUrl;

    @SerializedName("paid_at")
    private String paidAt;

    public long getId() { return id; }
    public int getCourseId() { return courseId; }
    public String getStatus() { return status; }
    public String getProvider() { return provider; }
    public String getProviderPreferenceId() { return providerPreferenceId; }
    public String getProviderPaymentId() { return providerPaymentId; }
    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public String getPaidAt() { return paidAt; }
}
