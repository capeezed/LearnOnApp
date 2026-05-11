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
    val format: String?,
)

data class CreateCourseResponseDto(
    val id: Int?,
    @SerializedName("course_id") val courseId: Int?,
    val status: String?,
    @SerializedName("interested_students_count") val interestedStudentsCount: Int?,
    @SerializedName("enrolled_students_count") val enrolledStudentsCount: Int?,
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

data class InstructorDashboardDto(
    val metrics: List<InstructorMetricDto>?,
)

data class InstructorMetricDto(
    val label: String?,
    val value: String?,
    val delta: String?,
)

data class InstructorCourseDto(
    val id: Int?,
    val title: String?,
    val description: String?,
    val format: String?,
    @SerializedName("duration_minutes") val durationMinutes: Int?,
    val price: Double?,
    val status: String?,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("topic_tag") val topicTag: String?,
    @SerializedName("interested_students_count") val interestedStudentsCount: Int?,
)

data class CourseVideoDto(
    val id: Int?,
    @SerializedName("course_id") val courseId: Int?,
    val title: String?,
    val description: String?,
    @SerializedName("video_url") val videoUrl: String?,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    val duration: Int?,
    @SerializedName("order_index") val orderIndex: Int?,
    @SerializedName("created_at") val createdAt: String?,
)

data class UpdateCourseVideoRequestDto(
    val title: String? = null,
    val description: String? = null,
    @SerializedName("order_index") val orderIndex: Int? = null,
)

data class InstructorReviewDto(
    val id: Int?,
    val rating: Double?,
    val comment: String?,
    @SerializedName("student_name") val studentName: String?,
    @SerializedName("course_title") val courseTitle: String?,
)

data class InstructorFinanceDto(
    @SerializedName("total_revenue") val totalRevenue: String?,
    @SerializedName("pending_revenue") val pendingRevenue: String?,
    val payments: List<String>?,
    @SerializedName("top_courses") val topCourses: List<String>?,
)

data class InstructorProfileDto(
    val id: Int?,
    val name: String?,
    val email: String?,
    val bio: String?,
    val specialties: List<String>?,
    @SerializedName("social_links") val socialLinks: List<String>?,
    val availability: String?,
    @SerializedName("accepted_formats") val acceptedFormats: List<String>?,
)

data class InstructorNotificationDto(
    val id: String?,
    val title: String?,
    val body: String?,
    @SerializedName("created_at") val createdAt: String?,
)

data class InstructorAnalyticsDto(
    @SerializedName("top_viewed_courses") val topViewedCourses: List<String>?,
    @SerializedName("completion_rate") val completionRate: String?,
    @SerializedName("average_response_time") val averageResponseTime: String?,
    @SerializedName("top_categories") val topCategories: List<String>?,
)
