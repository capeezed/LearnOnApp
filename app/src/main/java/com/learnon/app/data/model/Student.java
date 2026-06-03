package com.learnon.app.data.model;

import com.google.gson.annotations.SerializedName;

public class Student {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("token")
    private String token;

    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("refreshToken")
    private String refreshToken;

    @SerializedName("student")
    private StudentData student;

    public int getId() { return id; }
    public String getEmail() { return email; }
    public String getToken() {
        if (accessToken != null) return accessToken;
        return token;
    }

    public String getRefreshToken() { return refreshToken; }

    public String getName() {
        if (name != null) return name;
        if (student != null) return student.name;
        return "";
    }

    public static class StudentData {
        @SerializedName("id")
        public int id;

        @SerializedName("name")
        public String name;

        @SerializedName("email")
        public String email;
    }
}
