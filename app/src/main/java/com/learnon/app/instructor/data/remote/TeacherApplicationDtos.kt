package com.learnon.app.instructor.data.remote

import com.google.gson.annotations.SerializedName

data class TeacherRegisterRequest(
    @SerializedName("full_name") val fullName: String,
    val email: String,
    val phone: String,
    @SerializedName("profile_photo_uri") val profilePhotoUri: String?,
    val bio: String,
    val location: String,
    @SerializedName("knowledge_areas") val knowledgeAreas: List<String>,
    val subjects: String,
    @SerializedName("experience_level") val experienceLevel: String,
    @SerializedName("years_experience") val yearsExperience: Int,
    @SerializedName("linkedin_url") val linkedInUrl: String,
    @SerializedName("github_url") val githubUrl: String,
    @SerializedName("portfolio_url") val portfolioUrl: String?,
    @SerializedName("class_format") val classFormat: String,
    @SerializedName("weekly_availability") val weeklyAvailability: List<String>,
    @SerializedName("suggested_price_range") val suggestedPriceRange: String,
    @SerializedName("average_response_time") val averageResponseTime: String,
    @SerializedName("document_uri") val documentUri: String?,
    @SerializedName("certificate_uri") val certificateUri: String?,
    @SerializedName("accepted_terms") val acceptedTerms: Boolean,
)

data class TeacherApplicationResponseDto(
    val id: Long?,
    val status: String?,
    val message: String?,
)
