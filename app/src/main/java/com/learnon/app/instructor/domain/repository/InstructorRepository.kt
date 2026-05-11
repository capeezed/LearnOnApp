package com.learnon.app.instructor.domain.repository

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
import com.learnon.app.instructor.domain.model.InstructorVideo

interface InstructorRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun dashboard(): Result<InstructorDashboard>
    suspend fun pendingRequests(): Result<List<InstructorRequest>>
    suspend fun queueRequests(): Result<List<InstructorRequest>>
    suspend fun acceptRequest(matchId: String): Result<Unit>
    suspend fun rejectRequest(matchId: String): Result<Unit>
    suspend fun createCourse(requestId: String?, title: String, description: String, format: String, durationMinutes: Int, price: Double): Result<Unit>
    suspend fun createdCourses(): Result<List<InstructorCourse>>
    suspend fun courseVideos(courseId: String): Result<List<InstructorVideo>>
    suspend fun uploadCourseVideo(courseId: String, title: String, description: String, orderIndex: Int, videoUri: android.net.Uri): Result<Unit>
    suspend fun updateCourseVideo(videoId: String, title: String, description: String, orderIndex: Int): Result<Unit>
    suspend fun deleteCourseVideo(videoId: String): Result<Unit>
    suspend fun schedules(): Result<List<InstructorSchedule>>
    suspend fun createSchedule(courseId: String, scheduledAt: String, durationMin: Int, meetingUrl: String?): Result<Unit>
    suspend fun questions(courseId: String? = null): Result<List<InstructorQuestion>>
    suspend fun answerQuestion(questionId: String, answer: String): Result<Unit>
    suspend fun reviews(): Result<List<InstructorReview>>
    suspend fun finance(): Result<InstructorFinance>
    suspend fun profile(): Result<InstructorProfile>
    suspend fun notifications(): Result<List<InstructorNotification>>
    suspend fun analytics(): Result<InstructorAnalytics>
}
