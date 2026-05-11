package com.learnon.app.instructor.data.remote

import retrofit2.Response
import retrofit2.http.Body
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface InstructorApi {
    @POST("auth/instructors/login")
    suspend fun login(@Body body: InstructorLoginRequestDto): Response<InstructorLoginResponseDto>

    @POST("teacher-applications")
    suspend fun createTeacherApplication(@Body body: TeacherRegisterRequest): Response<TeacherApplicationResponseDto>

    @GET("matches/pending")
    suspend fun pendingMatches(): Response<List<MatchDto>>

    @PUT("matches/{id}/respond")
    suspend fun respondToMatch(
        @Path("id") id: String,
        @Body body: MatchResponseRequestDto,
    ): Response<Unit>

    @POST("requests/{id}/claim")
    suspend fun claimRequest(@Path("id") id: String): Response<Unit>

    @GET("requests/queue")
    suspend fun requestQueue(
        @Query("queue") queue: String = "normal",
        @Query("limit") limit: Int = 50,
    ): Response<List<QueueRequestDto>>

    @POST("courses")
    suspend fun createCourse(@Body body: CreateCourseRequestDto): Response<CreateCourseResponseDto>

    @POST("courses/{id}/publish")
    suspend fun publishCourse(@Path("id") id: String): Response<CreateCourseResponseDto>

    @GET("instructors/me/dashboard")
    suspend fun dashboard(): Response<InstructorDashboardDto>

    @GET("instructors/me/courses")
    suspend fun createdCourses(): Response<List<InstructorCourseDto>>

    @GET("courses/{id}/videos")
    suspend fun courseVideos(@Path("id") id: String): Response<List<CourseVideoDto>>

    @Multipart
    @POST("courses/{id}/videos")
    suspend fun uploadCourseVideo(
        @Path("id") id: String,
        @Part video: MultipartBody.Part,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("order_index") orderIndex: RequestBody,
    ): Response<CourseVideoDto>

    @PUT("videos/{id}")
    suspend fun updateCourseVideo(
        @Path("id") id: String,
        @Body body: UpdateCourseVideoRequestDto,
    ): Response<CourseVideoDto>

    @DELETE("videos/{id}")
    suspend fun deleteCourseVideo(@Path("id") id: String): Response<Unit>

    @GET("instructors/me/questions")
    suspend fun instructorQuestions(): Response<List<QuestionDto>>

    @GET("instructors/me/reviews")
    suspend fun reviews(): Response<List<InstructorReviewDto>>

    @GET("instructors/me/finance")
    suspend fun finance(): Response<InstructorFinanceDto>

    @GET("instructors/me/profile")
    suspend fun profile(): Response<InstructorProfileDto>

    @GET("instructors/me/notifications")
    suspend fun notifications(): Response<List<InstructorNotificationDto>>

    @GET("instructors/me/analytics")
    suspend fun analytics(): Response<InstructorAnalyticsDto>

    @GET("schedules")
    suspend fun schedules(): Response<List<ScheduleDto>>

    @POST("schedules")
    suspend fun createSchedule(@Body body: CreateScheduleRequestDto): Response<Unit>

    @GET("courses/{courseId}/questions")
    suspend fun questions(@Path("courseId") courseId: String): Response<List<QuestionDto>>

    @POST("questions/{questionId}/answers")
    suspend fun answerQuestion(
        @Path("questionId") questionId: String,
        @Body body: AnswerRequestDto,
    ): Response<Unit>
}
