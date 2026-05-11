package com.learnon.app.instructor.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.learnon.app.instructor.data.remote.AnswerRequestDto
import com.learnon.app.instructor.data.remote.CreateCourseRequestDto
import com.learnon.app.instructor.data.remote.CreateScheduleRequestDto
import com.learnon.app.instructor.data.remote.InstructorApi
import com.learnon.app.instructor.data.remote.InstructorLoginRequestDto
import com.learnon.app.instructor.data.remote.InstructorTokenStore
import com.learnon.app.instructor.data.remote.MatchResponseRequestDto
import com.learnon.app.instructor.data.remote.UpdateCourseVideoRequestDto
import com.learnon.app.instructor.domain.model.InstructorAnalytics
import com.learnon.app.instructor.domain.model.InstructorCourse
import com.learnon.app.instructor.domain.model.InstructorDashboard
import com.learnon.app.instructor.domain.model.InstructorFinance
import com.learnon.app.instructor.domain.model.InstructorMetric
import com.learnon.app.instructor.domain.model.InstructorNotification
import com.learnon.app.instructor.domain.model.InstructorProfile
import com.learnon.app.instructor.domain.model.InstructorQuestion
import com.learnon.app.instructor.domain.model.InstructorRequest
import com.learnon.app.instructor.domain.model.InstructorReview
import com.learnon.app.instructor.domain.model.InstructorSchedule
import com.learnon.app.instructor.domain.model.InstructorVideo
import com.learnon.app.instructor.domain.repository.InstructorRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class InstructorRepositoryImpl(
    private val context: Context,
    private val api: InstructorApi,
) : InstructorRepository {
    private val tokenStore = InstructorTokenStore(context.applicationContext)

    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val response = api.login(InstructorLoginRequestDto(email, password))
        if (!response.isSuccessful) error("Login de instrutor falhou (${response.code()}).")
        val body = response.body() ?: error("Resposta vazia.")
        val token = body.accessToken ?: body.token ?: error("Token nao retornado.")
        tokenStore.save(token, body.refreshToken, body.name ?: body.instructor?.name)
    }

    override suspend fun dashboard(): Result<InstructorDashboard> = runCatching {
        val response = api.dashboard()
        if (!response.isSuccessful) error("Dashboard indisponivel (${response.code()}).")
        val metrics = response.body()?.metrics.orEmpty().map {
            InstructorMetric(
                label = it.label.orEmpty(),
                value = it.value.orEmpty(),
                delta = it.delta.orEmpty(),
            )
        }
        val requests = pendingRequests().getOrElse { emptyList() }
        val schedules = schedules().getOrElse { emptyList() }
        val notifications = notifications().getOrElse { emptyList() }
        val delivered = createdCourses().getOrElse { emptyList() }

        InstructorDashboard(
            metrics = metrics,
            pendingRequests = requests.take(4),
            deliveredCourses = delivered.take(3),
            upcomingSchedules = schedules.take(3),
            notifications = notifications.take(4),
        )
    }

    override suspend fun pendingRequests(): Result<List<InstructorRequest>> = runCatching {
        val response = api.pendingMatches()
        if (!response.isSuccessful) error("Nao foi possivel carregar matches (${response.code()}).")
        response.body().orEmpty().map {
            InstructorRequest(
                id = (it.id ?: it.requestId ?: 0).toString(),
                title = it.title.orEmpty(),
                description = it.description.orEmpty(),
                category = it.topicTag ?: "geral",
                formatPreference = it.formatPreference ?: "no_preference",
                urgency = it.urgency ?: "normal",
                status = it.status ?: "pending_acceptance",
                priorityScore = it.priorityScore ?: it.totalScore ?: 0.0,
                deadline = it.expiresAt,
                studentName = it.studentName,
                interestedStudents = 1,
                difficulty = "Calculada pelo match",
            )
        }
    }

    override suspend fun queueRequests(): Result<List<InstructorRequest>> = runCatching {
        val response = api.requestQueue()
        if (!response.isSuccessful) error("Fila indisponivel (${response.code()}).")
        response.body().orEmpty().map {
            InstructorRequest(
                id = "request:${it.id ?: 0}",
                title = it.title.orEmpty(),
                description = it.description.orEmpty(),
                category = it.category ?: it.topicTag ?: "geral",
                formatPreference = it.formatPreference ?: "no_preference",
                urgency = it.urgency ?: "normal",
                status = it.status ?: "aguardando_match",
                priorityScore = it.priorityScore ?: 0.0,
                deadline = it.createdAt,
                studentName = it.student?.name,
                interestedStudents = 1,
                difficulty = "A estimar",
            )
        }
    }

    override suspend fun acceptRequest(matchId: String): Result<Unit> = runCatching {
        if (matchId.startsWith("request:")) {
            val requestId = matchId.removePrefix("request:")
            val response = api.claimRequest(requestId)
            if (!response.isSuccessful) error("Nao foi possivel assumir pedido (${response.code()}).")
        } else {
            respond(matchId, true).getOrThrow()
        }
    }
    override suspend fun rejectRequest(matchId: String): Result<Unit> = respond(matchId, false)

    private suspend fun respond(matchId: String, accepted: Boolean): Result<Unit> = runCatching {
        val response = api.respondToMatch(matchId, MatchResponseRequestDto(accepted))
        if (!response.isSuccessful) error("Nao foi possivel responder ao match (${response.code()}).")
    }

    override suspend fun createCourse(requestId: String?, title: String, description: String, format: String, durationMinutes: Int, price: Double): Result<Unit> = runCatching {
        val cleanRequestId = requestId?.removePrefix("request:")?.toIntOrNull()
        val response = api.createCourse(
            CreateCourseRequestDto(
                requestId = cleanRequestId,
                title = title,
                description = description,
                videoUrl = null,
                thumbnailUrl = null,
                durationMinutes = durationMinutes,
                price = price,
                format = format,
            )
        )
        if (!response.isSuccessful) error("Nao foi possivel criar curso (${response.code()}).")
        val courseId = response.body()?.courseId ?: response.body()?.id
        if (courseId != null) {
            val publish = api.publishCourse(courseId.toString())
            if (!publish.isSuccessful) error("Curso criado, mas nao foi possivel publicar (${publish.code()}).")
        }
    }

    override suspend fun createdCourses(): Result<List<InstructorCourse>> = runCatching {
        val response = api.createdCourses()
        if (!response.isSuccessful) error("Cursos indisponiveis (${response.code()}).")
        response.body().orEmpty().map {
            InstructorCourse(
                id = (it.id ?: 0).toString(),
                title = it.title.orEmpty(),
                category = it.topicTag ?: "geral",
                format = it.format ?: "recorded",
                status = it.status ?: "draft",
                progressLabel = "${it.interestedStudentsCount ?: 0} alunos interessados",
                revenue = "R$ %.2f".format(it.price ?: 0.0),
            )
        }
    }

    override suspend fun courseVideos(courseId: String): Result<List<InstructorVideo>> = runCatching {
        val response = api.courseVideos(courseId)
        if (!response.isSuccessful) error("Videos indisponiveis (${response.code()}).")
        response.body().orEmpty().map {
            InstructorVideo(
                id = (it.id ?: 0).toString(),
                courseId = (it.courseId ?: 0).toString(),
                title = it.title.orEmpty(),
                description = it.description.orEmpty(),
                videoUrl = it.videoUrl.orEmpty(),
                thumbnailUrl = it.thumbnailUrl,
                duration = it.duration ?: 0,
                orderIndex = it.orderIndex ?: 0,
            )
        }
    }

    override suspend fun uploadCourseVideo(courseId: String, title: String, description: String, orderIndex: Int, videoUri: Uri): Result<Unit> = runCatching {
        val resolver = context.contentResolver
        val bytes = resolver.openInputStream(videoUri)?.use { it.readBytes() } ?: error("Nao foi possivel ler o video selecionado.")
        val mimeType = resolver.getType(videoUri) ?: "video/mp4"
        val fileName = fileName(videoUri)
        val videoBody = bytes.toRequestBody(mimeType.toMediaType())
        val videoPart = MultipartBody.Part.createFormData("video", fileName, videoBody)
        val textType = "text/plain".toMediaType()

        val response = api.uploadCourseVideo(
            id = courseId,
            video = videoPart,
            title = title.toRequestBody(textType),
            description = description.toRequestBody(textType),
            orderIndex = orderIndex.toString().toRequestBody(textType),
        )
        if (!response.isSuccessful) error("Upload de video falhou (${response.code()}).")
    }

    override suspend fun updateCourseVideo(videoId: String, title: String, description: String, orderIndex: Int): Result<Unit> = runCatching {
        val response = api.updateCourseVideo(videoId, UpdateCourseVideoRequestDto(title, description, orderIndex))
        if (!response.isSuccessful) error("Nao foi possivel editar video (${response.code()}).")
    }

    override suspend fun deleteCourseVideo(videoId: String): Result<Unit> = runCatching {
        val response = api.deleteCourseVideo(videoId)
        if (!response.isSuccessful) error("Nao foi possivel excluir video (${response.code()}).")
    }

    private fun fileName(uri: Uri): String {
        val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return it.getString(index)
            }
        }
        return "learnon-video.mp4"
    }

    override suspend fun schedules(): Result<List<InstructorSchedule>> = runCatching {
        val response = api.schedules()
        if (!response.isSuccessful) error("Agenda indisponivel (${response.code()}).")
        response.body().orEmpty().map {
            InstructorSchedule(
                id = (it.id ?: 0).toString(),
                courseTitle = it.courseTitle ?: "Aula ao vivo",
                scheduledAt = it.scheduledAt ?: "",
                durationMin = it.durationMin ?: 60,
                meetingUrl = it.meetingUrl,
            )
        }
    }

    override suspend fun createSchedule(courseId: String, scheduledAt: String, durationMin: Int, meetingUrl: String?): Result<Unit> = runCatching {
        val response = api.createSchedule(
            CreateScheduleRequestDto(
                courseId = courseId.toIntOrNull() ?: error("ID do curso invalido."),
                scheduledAt = scheduledAt,
                durationMin = durationMin,
                meetingUrl = meetingUrl,
            )
        )
        if (!response.isSuccessful) error("Nao foi possivel agendar aula (${response.code()}).")
    }

    override suspend fun questions(courseId: String?): Result<List<InstructorQuestion>> = runCatching {
        val response = if (courseId == null) api.instructorQuestions() else api.questions(courseId)
        if (!response.isSuccessful) error("Perguntas indisponiveis (${response.code()}).")
        response.body().orEmpty().map {
            InstructorQuestion(
                id = (it.id ?: 0).toString(),
                courseTitle = it.courseTitle ?: "Microcurso",
                studentName = it.studentName ?: "Aluno",
                question = it.question.orEmpty(),
                isResolved = it.isResolved ?: false,
            )
        }
    }

    override suspend fun answerQuestion(questionId: String, answer: String): Result<Unit> = runCatching {
        val response = api.answerQuestion(questionId, AnswerRequestDto(answer))
        if (!response.isSuccessful) error("Nao foi possivel responder pergunta (${response.code()}).")
    }

    override suspend fun reviews(): Result<List<InstructorReview>> = runCatching {
        val response = api.reviews()
        if (!response.isSuccessful) error("Avaliacoes indisponiveis (${response.code()}).")
        response.body().orEmpty().map {
            InstructorReview(
                id = (it.id ?: 0).toString(),
                courseTitle = it.courseTitle ?: "Microcurso",
                studentName = it.studentName ?: "Aluno",
                rating = it.rating ?: 0.0,
                comment = it.comment.orEmpty(),
            )
        }
    }

    override suspend fun finance(): Result<InstructorFinance> = runCatching {
        val response = api.finance()
        if (!response.isSuccessful) error("Financeiro indisponivel (${response.code()}).")
        val body = response.body()
        InstructorFinance(
            totalRevenue = body?.totalRevenue ?: "R$ 0,00",
            pendingRevenue = body?.pendingRevenue ?: "R$ 0,00",
            payments = body?.payments.orEmpty(),
            topCourses = body?.topCourses.orEmpty(),
        )
    }

    override suspend fun profile(): Result<InstructorProfile> = runCatching {
        val response = api.profile()
        if (!response.isSuccessful) error("Perfil indisponivel (${response.code()}).")
        val body = response.body()
        InstructorProfile(
            name = body?.name ?: tokenStore.name(),
            bio = body?.bio.orEmpty(),
            specialties = body?.specialties.orEmpty(),
            socialLinks = body?.socialLinks.orEmpty(),
            availability = body?.availability.orEmpty(),
            acceptedFormats = body?.acceptedFormats.orEmpty(),
        )
    }

    override suspend fun notifications(): Result<List<InstructorNotification>> = runCatching {
        val response = api.notifications()
        if (!response.isSuccessful) error("Notificacoes indisponiveis (${response.code()}).")
        response.body().orEmpty().map {
            InstructorNotification(
                id = it.id.orEmpty(),
                title = it.title.orEmpty(),
                body = it.body.orEmpty(),
                createdAt = it.createdAt.orEmpty(),
            )
        }
    }

    override suspend fun analytics(): Result<InstructorAnalytics> = runCatching {
        val response = api.analytics()
        if (!response.isSuccessful) error("Analytics indisponivel (${response.code()}).")
        val body = response.body()
        InstructorAnalytics(
            topViewedCourses = body?.topViewedCourses.orEmpty(),
            completionRate = body?.completionRate ?: "0 cursos",
            averageResponseTime = body?.averageResponseTime ?: "Sem avaliacoes",
            topCategories = body?.topCategories.orEmpty(),
        )
    }
}
