package com.learnon.app.instructor.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface InstructorApi {
    @POST("auth/instructors/login")
    suspend fun login(@Body body: InstructorLoginRequestDto): Response<InstructorLoginResponseDto>

    @GET("matches/pending")
    suspend fun pendingMatches(): Response<List<MatchDto>>

    @PUT("matches/{id}/respond")
    suspend fun respondToMatch(
        @Path("id") id: String,
        @Body body: MatchResponseRequestDto,
    ): Response<Unit>

    @GET("requests/queue")
    suspend fun requestQueue(
        @Query("queue") queue: String = "normal",
        @Query("limit") limit: Int = 50,
    ): Response<List<QueueRequestDto>>

    @POST("courses")
    suspend fun createCourse(@Body body: CreateCourseRequestDto): Response<CreateCourseResponseDto>

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
