package com.learnon.app.instructor.registration

import com.learnon.app.instructor.data.remote.TeacherRegisterRequest

data class TeacherRegistrationForm(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val profilePhotoUri: String? = null,
    val bio: String = "",
    val location: String = "",
    val knowledgeAreas: Set<String> = emptySet(),
    val subjects: String = "",
    val experienceLevel: String = "",
    val yearsExperience: String = "",
    val linkedInUrl: String = "",
    val githubUrl: String = "",
    val portfolioUrl: String = "",
    val classFormat: String = "",
    val weeklyAvailability: Set<String> = emptySet(),
    val suggestedPriceRange: String = "",
    val averageResponseTime: String = "",
    val documentUri: String? = null,
    val certificateUri: String? = null,
    val acceptedTerms: Boolean = false,
) {
    fun toRequest(): TeacherRegisterRequest = TeacherRegisterRequest(
        fullName = fullName.trim(),
        email = email.trim(),
        phone = phone.trim(),
        profilePhotoUri = profilePhotoUri,
        bio = bio.trim(),
        location = location.trim(),
        knowledgeAreas = knowledgeAreas.toList(),
        subjects = subjects.trim(),
        experienceLevel = experienceLevel,
        yearsExperience = yearsExperience.toIntOrNull() ?: 0,
        linkedInUrl = linkedInUrl.trim(),
        githubUrl = githubUrl.trim(),
        portfolioUrl = portfolioUrl.trim().ifBlank { null },
        classFormat = classFormat,
        weeklyAvailability = weeklyAvailability.toList(),
        suggestedPriceRange = suggestedPriceRange,
        averageResponseTime = averageResponseTime,
        documentUri = documentUri,
        certificateUri = certificateUri,
        acceptedTerms = acceptedTerms,
    )
}

data class TeacherRegistrationUiState(
    val form: TeacherRegistrationForm = TeacherRegistrationForm(),
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
)
