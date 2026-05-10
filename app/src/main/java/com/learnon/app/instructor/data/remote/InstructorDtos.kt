package com.learnon.app.instructor.data.remote

import com.google.gson.annotations.SerializedName

data class InstructorLoginRequestDto(
    val email: String,
    val password: String,
)

data class InstructorLoginResponseDto(
    val token: String?,
    val accessToken: String?,
    val refreshToken: String?,
    val name: String?,
    val instructor: InstructorUserDto?,
)

data class InstructorUserDto(
    val id: Int?,
    val name: String?,
    val email: String?,
    val role: String?,
)

data class MatchDto(
    val id: Int?,
    @SerializedName("request_id") val requestId: Int?,
    @SerializedName("expires_at") val expiresAt: String?,
    @SerializedName("match_attempt") val matchAttempt: Int?,
    val title: String?,
    val description: String?,
    @SerializedName("topic_tag") val topicTag: String?,
    @SerializedName("format_preference") val formatPreference: String?,
    val urgency: String?,
    @SerializedName("student_name") val studentName: String?,
    @SerializedName("priority_score") val priorityScore: Double?,
    @SerializedName("total_score") val totalScore: Double?,
    val status: String?,
)

data class QueueRequestDto(
    val id: Int?,
    val title: String?,
    val description: String?,
    val category: String?,
    @SerializedName("topic_tag") val topicTag: String?,
    @SerializedName("format_preference") val formatPreference: String?,
    val urgency: String?,
    val status: String?,
    @SerializedName("priority_score") val priorityScore: Double?,
    @SerializedName("created_at") val createdAt: String?,
    val student: QueueStudentDto?,
)

data class QueueStudentDto(val name: String?)

data class MatchResponseRequestDto(
    val accepted: Boolean,
    @SerializedName("format_preference") val formatPreference: String? = null,
    @SerializedName("complexity_level") val complexityLevel: String? = null,
)

data class CreateCourseRequestDto(
    @SerializedName("request_id") val requestId: Int?,
    val title: String,
    val description: String,
    @SerializedName("video_url") val videoUrl: String?,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    @SerializedName("duration_minutes") val durationMinutes: Int?,
    val price: Double?,
)

data class CreateCourseResponseDto(
    @SerializedName("course_id") val courseId: Int?,
)

data class ScheduleDto(
    val id: Int?,
    @SerializedName("course_title") val courseTitle: String?,
    @SerializedName("scheduled_at") val scheduledAt: String?,
    @SerializedName("duration_min") val durationMin: Int?,
    @SerializedName("meeting_url") val meetingUrl: String?,
    val status: String?,
)

data class CreateScheduleRequestDto(
    @SerializedName("course_id") val courseId: Int,
    @SerializedName("scheduled_at") val scheduledAt: String,
    @SerializedName("duration_min") val durationMin: Int,
    @SerializedName("meeting_url") val meetingUrl: String?,
)

data class QuestionDto(
    val id: Int?,
    val question: String?,
    @SerializedName("is_resolved") val isResolved: Boolean?,
    @SerializedName("student_name") val studentName: String?,
    @SerializedName("course_title") val courseTitle: String?,
)

data class AnswerRequestDto(val answer: String)
