package com.learnon.app.instructor.domain.model

data class InstructorMetric(
    val label: String,
    val value: String,
    val delta: String,
)

data class InstructorRequest(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val formatPreference: String,
    val urgency: String,
    val status: String,
    val priorityScore: Double,
    val deadline: String?,
    val studentName: String?,
    val interestedStudents: Int,
    val difficulty: String,
)

data class InstructorCourse(
    val id: String,
    val title: String,
    val category: String,
    val format: String,
    val status: String,
    val progressLabel: String,
    val revenue: String,
)

data class InstructorVideo(
    val id: String,
    val courseId: String,
    val title: String,
    val description: String,
    val videoUrl: String,
    val thumbnailUrl: String?,
    val duration: Int,
    val orderIndex: Int,
)

data class InstructorSchedule(
    val id: String,
    val courseTitle: String,
    val scheduledAt: String,
    val durationMin: Int,
    val meetingUrl: String?,
)

data class InstructorQuestion(
    val id: String,
    val courseTitle: String,
    val studentName: String,
    val question: String,
    val isResolved: Boolean,
)

data class InstructorReview(
    val id: String,
    val courseTitle: String,
    val studentName: String,
    val rating: Double,
    val comment: String,
)

data class InstructorFinance(
    val totalRevenue: String,
    val pendingRevenue: String,
    val payments: List<String>,
    val topCourses: List<String>,
)

data class InstructorProfile(
    val name: String,
    val bio: String,
    val specialties: List<String>,
    val socialLinks: List<String>,
    val availability: String,
    val acceptedFormats: List<String>,
)

data class InstructorNotification(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: String,
)

data class InstructorAnalytics(
    val topViewedCourses: List<String>,
    val completionRate: String,
    val averageResponseTime: String,
    val topCategories: List<String>,
)

data class InstructorDashboard(
    val metrics: List<InstructorMetric>,
    val pendingRequests: List<InstructorRequest>,
    val deliveredCourses: List<InstructorCourse>,
    val upcomingSchedules: List<InstructorSchedule>,
    val notifications: List<InstructorNotification>,
)
