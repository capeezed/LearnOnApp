package com.learnon.app.instructor.data.repository

import android.content.Context
import com.learnon.app.instructor.data.remote.AnswerRequestDto
import com.learnon.app.instructor.data.remote.CreateCourseRequestDto
import com.learnon.app.instructor.data.remote.CreateScheduleRequestDto
import com.learnon.app.instructor.data.remote.InstructorApi
import com.learnon.app.instructor.data.remote.InstructorLoginRequestDto
import com.learnon.app.instructor.data.remote.InstructorTokenStore
import com.learnon.app.instructor.data.remote.MatchResponseRequestDto
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
import com.learnon.app.instructor.domain.repository.InstructorRepository

class InstructorRepositoryImpl(
    context: Context,
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
        val requests = pendingRequests().getOrElse { mockRequests() }
        val schedules = schedules().getOrElse { mockSchedules() }
        val notifications = notifications().getOrElse { mockNotifications() }
        val delivered = createdCourses().getOrElse { mockCourses() }
        val reviews = reviews().getOrElse { mockReviews() }
        val finance = finance().getOrElse { mockFinance() }

        InstructorDashboard(
            metrics = listOf(
                InstructorMetric("Pedidos pendentes", requests.size.toString(), "matchmaking ativo"),
                InstructorMetric("Cursos entregues", delivered.size.toString(), "microcursos publicados"),
                InstructorMetric("Aceitacao", "82%", "ultimos 30 dias"),
                InstructorMetric("Avaliacao media", reviews.map { it.rating }.average().takeIf { !it.isNaN() }?.let { "%.1f".format(it) } ?: "4.8", "sinal de confianca"),
                InstructorMetric("Faturamento", finance.totalRevenue, "sob demanda"),
            ),
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
                difficulty = "A estimar",
            )
        }
    }

    override suspend fun queueRequests(): Result<List<InstructorRequest>> = runCatching {
        val response = api.requestQueue()
        if (!response.isSuccessful) error("Fila indisponivel (${response.code()}).")
        response.body().orEmpty().map {
            InstructorRequest(
                id = (it.id ?: 0).toString(),
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
        }.ifEmpty { mockRequests() }
    }

    override suspend fun acceptRequest(matchId: String): Result<Unit> = respond(matchId, true)
    override suspend fun rejectRequest(matchId: String): Result<Unit> = respond(matchId, false)

    private suspend fun respond(matchId: String, accepted: Boolean): Result<Unit> = runCatching {
        val response = api.respondToMatch(matchId, MatchResponseRequestDto(accepted))
        if (!response.isSuccessful) error("Nao foi possivel responder ao match (${response.code()}).")
    }

    override suspend fun createCourse(requestId: String?, title: String, description: String): Result<Unit> = runCatching {
        val response = api.createCourse(
            CreateCourseRequestDto(
                requestId = requestId?.toIntOrNull(),
                title = title,
                description = description,
                videoUrl = null,
                thumbnailUrl = null,
                durationMinutes = 12,
                price = 0.0,
            )
        )
        if (!response.isSuccessful) error("Nao foi possivel criar curso (${response.code()}).")
    }

    override suspend fun createdCourses(): Result<List<InstructorCourse>> = Result.success(mockCourses())

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
        }.ifEmpty { mockSchedules() }
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
        // TODO backend: idealmente expor perguntas do instrutor sem exigir courseId.
        val id = courseId ?: return@runCatching mockQuestions()
        val response = api.questions(id)
        if (!response.isSuccessful) return@runCatching mockQuestions()
        response.body().orEmpty().map {
            InstructorQuestion(
                id = (it.id ?: 0).toString(),
                courseTitle = it.courseTitle ?: "Microcurso",
                studentName = it.studentName ?: "Aluno",
                question = it.question.orEmpty(),
                isResolved = it.isResolved ?: false,
            )
        }.ifEmpty { mockQuestions() }
    }

    override suspend fun answerQuestion(questionId: String, answer: String): Result<Unit> = runCatching {
        val response = api.answerQuestion(questionId, AnswerRequestDto(answer))
        if (!response.isSuccessful) error("Nao foi possivel responder pergunta (${response.code()}).")
    }

    override suspend fun reviews(): Result<List<InstructorReview>> = Result.success(mockReviews())
    override suspend fun finance(): Result<InstructorFinance> = Result.success(mockFinance())
    override suspend fun profile(): Result<InstructorProfile> = Result.success(mockProfile())
    override suspend fun notifications(): Result<List<InstructorNotification>> = Result.success(mockNotifications())
    override suspend fun analytics(): Result<InstructorAnalytics> = Result.success(mockAnalytics())

    private fun mockRequests() = listOf(
        InstructorRequest("1", "JWT com refresh token seguro", "Aluno quer entender rotacao de refresh token em Express.", "node.js", "live", "fast_track", "aguardando_match", 92.5, "Hoje, 18:00", "Marina", 7, "Intermediaria"),
        InstructorRequest("2", "Compose Navigation em app real", "Duvida sobre grafo de navegacao e estados compartilhados.", "android", "recorded", "normal", "aguardando_match", 71.2, "Amanha", "Rafael", 4, "Intermediaria"),
        InstructorRequest("3", "SQL para ranking de pedidos", "Precisa montar score por urgencia e tempo em fila.", "sql", "no_preference", "normal", "aguardando_instrutor", 66.8, "2 dias", "Lia", 5, "Avancada"),
    )

    private fun mockCourses() = listOf(
        InstructorCourse("18", "Refresh token sem susto", "node.js", "recorded", "published", "Entregue em 14 min", "R$ 420,00"),
        InstructorCourse("21", "Compose StateFlow na pratica", "android", "recorded", "draft", "Rascunho salvo", "R$ 180,00"),
        InstructorCourse("22", "Matchmaking com SQL", "sql", "live", "scheduled", "Aula hoje", "R$ 260,00"),
    )

    private fun mockSchedules() = listOf(
        InstructorSchedule("1", "Matchmaking com SQL", "Hoje 19:30", 45, null),
        InstructorSchedule("2", "Compose StateFlow na pratica", "Amanha 10:00", 60, null),
    )

    private fun mockQuestions() = listOf(
        InstructorQuestion("1", "Refresh token sem susto", "Marina", "Onde eu salvo o refresh token no mobile?", false),
        InstructorQuestion("2", "Compose StateFlow", "Rafael", "Quando usar collectAsStateWithLifecycle?", true),
    )

    private fun mockReviews() = listOf(
        InstructorReview("1", "Refresh token sem susto", "Marina", 5.0, "Direto ao ponto e aplicavel."),
        InstructorReview("2", "Compose StateFlow", "Rafael", 4.7, "Gostei dos exemplos reais."),
    )

    private fun mockFinance() = InstructorFinance(
        totalRevenue = "R$ 3.840,00",
        pendingRevenue = "R$ 620,00",
        payments = listOf("Pix previsto em 12/05", "Repasse concluido R$ 840,00"),
        topCourses = listOf("Refresh token sem susto", "Compose StateFlow", "Matchmaking com SQL"),
    )

    private fun mockProfile() = InstructorProfile(
        name = tokenStore.name(),
        bio = "Instrutor focado em microcursos objetivos para duvidas reais.",
        specialties = listOf("Node.js", "Android", "SQL", "Arquitetura"),
        socialLinks = listOf("github.com/learnon-instructor", "linkedin.com/in/learnon"),
        availability = "Seg a Sex, 18h-22h",
        acceptedFormats = listOf("Ao vivo", "Gravado"),
    )

    private fun mockNotifications() = listOf(
        InstructorNotification("1", "Novo match fast-track", "Pedido de Node.js subiu na fila.", "agora"),
        InstructorNotification("2", "Pergunta aguardando resposta", "Aluno pediu complemento em um microcurso.", "12 min"),
        InstructorNotification("3", "Aula ao vivo proxima", "Comeca hoje as 19:30.", "1 h"),
    )

    private fun mockAnalytics() = InstructorAnalytics(
        topViewedCourses = listOf("Refresh token sem susto", "Compose StateFlow", "Matchmaking com SQL"),
        completionRate = "74%",
        averageResponseTime = "38 min",
        topCategories = listOf("Node.js", "Android", "SQL"),
    )
}
