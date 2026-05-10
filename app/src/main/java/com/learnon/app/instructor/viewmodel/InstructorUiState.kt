package com.learnon.app.instructor.viewmodel

import com.learnon.app.instructor.domain.model.InstructorAnalytics
import com.learnon.app.instructor.domain.model.InstructorCourse
import com.learnon.app.instructor.domain.model.InstructorDashboard
import com.learnon.app.instructor.domain.model.InstructorFinance
import com.learnon.app.instructor.domain.model.InstructorNotification
import com.learnon.app.instructor.domain.model.InstructorProfile
import com.learnon.app.instructor.domain.model.InstructorQuestion
import com.learnon.app.instructor.domain.model.InstructorRequest
import com.learnon.app.instructor.domain.model.InstructorReview
import com.learnon.app.instructor.domain.model.InstructorSchedule

data class InstructorUiState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null,
    val dashboard: InstructorDashboard? = null,
    val pendingRequests: List<InstructorRequest> = emptyList(),
    val queueRequests: List<InstructorRequest> = emptyList(),
    val courses: List<InstructorCourse> = emptyList(),
    val schedules: List<InstructorSchedule> = emptyList(),
    val questions: List<InstructorQuestion> = emptyList(),
    val reviews: List<InstructorReview> = emptyList(),
    val finance: InstructorFinance? = null,
    val profile: InstructorProfile? = null,
    val notifications: List<InstructorNotification> = emptyList(),
    val analytics: InstructorAnalytics? = null,
)
