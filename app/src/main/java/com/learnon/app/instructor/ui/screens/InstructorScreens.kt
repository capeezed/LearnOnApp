package com.learnon.app.instructor.ui.screens

import androidx.compose.animation.AnimatedVisibility
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.learnon.app.instructor.domain.model.InstructorRequest
import com.learnon.app.instructor.navigation.InstructorRoute
import com.learnon.app.instructor.ui.components.LoadingSkeleton
import com.learnon.app.instructor.ui.components.MetricCard
import com.learnon.app.instructor.ui.components.RequestCard
import com.learnon.app.instructor.ui.components.SectionTitle
import com.learnon.app.instructor.viewmodel.InstructorUiState

@Composable
fun HomeScreen(state: InstructorUiState, onRefresh: () -> Unit, navigate: (InstructorRoute) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            InstructorHero()
        }
        if (state.isLoading && state.dashboard == null) {
            item { LoadingSkeleton() }
        }
        state.dashboard?.let { dashboard ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    dashboard.metrics.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { MetricCard(it, Modifier.weight(1f)) }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            item { SectionTitle("Pedidos que merecem resposta", "ver todos") }
            items(dashboard.pendingRequests) { request ->
                RequestCard(request)
            }
            item { SectionTitle("Proximas aulas") }
            items(dashboard.upcomingSchedules) {
                InfoCard(it.courseTitle, "${it.scheduledAt} - ${it.durationMin} min")
            }
            item { SectionTitle("Notificacoes recentes") }
            items(dashboard.notifications) {
                InfoCard(it.title, it.body)
            }
        }
        item {
            Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Atualizar painel") }
        }
    }
}

@Composable
private fun InstructorHero() {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF4937A6), Color(0xFF17213A)),
                ),
                RoundedCornerShape(8.dp),
            )
            .padding(22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("Painel do instrutor") })
            Text(
                "Operacao de microcursos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Matches, prazos, aulas e entregas rapidas no mesmo painel.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RequestsScreen(
    state: InstructorUiState,
    onAccept: (InstructorRequest) -> Unit,
    onReject: (String) -> Unit,
    onOpenDetail: (InstructorRequest) -> Unit,
) {
    var urgency by remember { mutableStateOf("todos") }
    var format by remember { mutableStateOf("todos") }
    val all = (state.pendingRequests + state.queueRequests).distinctBy { it.id }
    val filtered = all.filter {
        (urgency == "todos" || it.urgency == urgency) && (format == "todos" || it.formatPreference == format)
    }.sortedByDescending { it.priorityScore }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ScreenHero(
                title = "Pedidos recebidos",
                subtitle = "${filtered.size} pedidos filtrados para avaliar e transformar em microcurso.",
                badge = "Fila de oportunidades",
            )
        }
        item {
            PanelCard {
                Text("Filtros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("todos", "normal", "fast_track").forEach {
                        FilterChip(selected = urgency == it, onClick = { urgency = it }, label = { Text(it) })
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("todos", "live", "recorded", "no_preference").forEach {
                        FilterChip(selected = format == it, onClick = { format = it }, label = { Text(it) })
                    }
                }
            }
        }
        if (filtered.isEmpty()) {
            item { EmptyState("Nada nesta combinacao", "Troque os filtros ou atualize o painel para buscar novos pedidos.") }
        }
        items(filtered) { request ->
            RequestCard(
                request = request,
                actions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onOpenDetail(request) }) { Text("Detalhes") }
                        if (!request.id.startsWith("request:")) {
                            OutlinedButton(onClick = { onReject(request.id) }) { Text("Rejeitar") }
                        }
                        Button(onClick = { onAccept(request) }) { Text("Aceitar") }
                    }
                },
            )
        }
    }
}

@Composable
fun RequestDetailScreen(request: InstructorRequest?) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ScreenHero(
                title = "Detalhe do pedido",
                subtitle = "Veja contexto, prioridade e formato antes de aceitar a entrega.",
                badge = "Analise",
            )
        }
        if (request == null) {
            item { EmptyState("Nenhum pedido selecionado", "Abra um pedido na lista para ver o detalhe completo.") }
        } else {
            item { RequestCard(request) }
            item {
                PanelCard {
                    DetailRow("Descricao da duvida", request.description)
                    DetailRow("Formato solicitado", request.formatPreference)
                    DetailRow("Urgencia", request.urgency)
                    DetailRow("Prazo", request.deadline ?: "dinamico")
                    DetailRow("Alunos interessados", request.interestedStudents.toString())
                    DetailRow("Dificuldade estimada", request.difficulty)
                    DetailRow("Status do match", request.status)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateCourseScreen(state: InstructorUiState, onCreate: (String?, String, String, String, Int, Double) -> Unit) {
    val selected = state.selectedRequest
    var requestId by remember(selected?.id) { mutableStateOf(selected?.id ?: state.pendingRequests.firstOrNull()?.id.orEmpty()) }
    var title by remember(selected?.title) { mutableStateOf(selected?.title?.let { "Microcurso: $it" } ?: "") }
    var description by remember(selected?.description) { mutableStateOf(selected?.description.orEmpty()) }
    var format by remember(selected?.formatPreference) { mutableStateOf(if (selected?.formatPreference == "live") "live" else "recorded") }
    var duration by remember { mutableStateOf("20") }
    var price by remember { mutableStateOf("0.00") }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ScreenHero(
                title = "Criar microcurso",
                subtitle = "Monte a entrega, revise o formato e publique para matricular os alunos interessados.",
                badge = "Publicacao",
            )
        }
        selected?.let { request ->
            item {
                RequestCard(request)
            }
        }
        item {
            PanelCard {
                StepTitle("1", "Vincule ao pedido")
                OutlinedTextField(requestId, { requestId = it }, label = { Text("ID do pedido") }, modifier = Modifier.fillMaxWidth())
                if (state.pendingRequests.isNotEmpty()) {
                    Text("Pedidos recentes", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.pendingRequests.take(6).forEach { request ->
                            AssistChip(
                                onClick = {
                                    requestId = request.id
                                    title = "Microcurso: ${request.title}"
                                    description = request.description
                                    format = if (request.formatPreference == "live") "live" else "recorded"
                                },
                                label = { Text(request.title, maxLines = 1) },
                            )
                        }
                    }
                }
            }
        }
        item {
            PanelCard {
                StepTitle("2", "Conteudo do curso")
                OutlinedTextField(title, { title = it }, label = { Text("Titulo") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    description,
                    { description = it },
                    label = { Text("Descricao") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = format == "recorded", onClick = { format = "recorded" }, label = { Text("Gravado") })
                    FilterChip(selected = format == "live", onClick = { format = "live" }, label = { Text("Ao vivo") })
                }
            }
        }
        item {
            PanelCard {
                StepTitle("3", "Preco e publicacao")
                OutlinedTextField(duration, { duration = it.filter(Char::isDigit) }, label = { Text("Duracao em minutos") }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("15", "20", "30", "60").forEach {
                        AssistChip(onClick = { duration = it }, label = { Text("${it} min") })
                    }
                }
                OutlinedTextField(price, { price = it.replace(",", ".") }, label = { Text("Preco") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        onCreate(
                            requestId.ifBlank { null },
                            title,
                            description,
                            format,
                            duration.toIntOrNull() ?: 20,
                            price.toDoubleOrNull() ?: 0.0,
                        )
                    },
                    enabled = title.isNotBlank() && description.isNotBlank() && requestId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(if (state.isLoading) "Publicando..." else "Publicar curso")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadScreen(
    state: InstructorUiState,
    onLoadVideos: (String) -> Unit,
    onUpload: (String, String, String, Int, Uri) -> Unit,
    onUpdate: (String, String, String, String, Int) -> Unit,
    onDelete: (String, String) -> Unit,
) {
    var courseId by remember(state.courses) { mutableStateOf(state.selectedCourseId ?: state.courses.firstOrNull()?.id.orEmpty()) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var orderIndex by remember { mutableStateOf("0") }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedVideoUri = uri
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            ScreenHero(
                title = "Upload de video",
                subtitle = "Escolha o curso, envie uma nova aula e organize a ordem dos videos.",
                badge = "Biblioteca",
            )
        }
        item {
            PanelCard {
                StepTitle("1", "Selecione o curso")
                OutlinedTextField(courseId, { courseId = it }, label = { Text("ID do curso") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { if (courseId.isNotBlank()) onLoadVideos(courseId) }, enabled = courseId.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text("Carregar videos")
                }
                if (state.courses.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.courses.take(8).forEach { course ->
                        AssistChip(
                            onClick = {
                                courseId = course.id
                                onLoadVideos(course.id)
                            },
                            label = { Text(course.title) },
                        )
                        }
                    }
                }
            }
        }
        item {
            PanelCard {
                StepTitle("2", "Nova aula em video")
                OutlinedTextField(title, { title = it }, label = { Text("Titulo da aula") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Descricao") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OutlinedTextField(orderIndex, { orderIndex = it.filter(Char::isDigit) }, label = { Text("Ordem na playlist") }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { picker.launch("video/*") }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(if (selectedVideoUri == null) Icons.Outlined.CloudUpload else Icons.Outlined.CloudDone, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text(if (selectedVideoUri == null) "Selecionar video" else "Video selecionado")
                }
                Button(
                    onClick = {
                        selectedVideoUri?.let { uri ->
                            onUpload(courseId, title, description, orderIndex.toIntOrNull() ?: 0, uri)
                        }
                    },
                    enabled = !state.isLoading && courseId.isNotBlank() && title.isNotBlank() && selectedVideoUri != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(if (state.isLoading) "Enviando..." else "Enviar video")
                }
            }
        }
        item { SectionTitle("Videos do curso") }
        if (state.videos.isEmpty()) {
            item { EmptyState("Nenhum video carregado", "Selecione um curso ou envie a primeira aula desta entrega.") }
        }
        items(state.videos) { video ->
            VideoEditCard(
                courseId = courseId,
                video = video,
                onUpdate = onUpdate,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun VideoEditCard(
    courseId: String,
    video: com.learnon.app.instructor.domain.model.InstructorVideo,
    onUpdate: (String, String, String, String, Int) -> Unit,
    onDelete: (String, String) -> Unit,
) {
    var title by remember(video.id) { mutableStateOf(video.title) }
    var description by remember(video.id) { mutableStateOf(video.description) }
    var orderIndex by remember(video.id) { mutableStateOf(video.orderIndex.toString()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text("Aula ${video.orderIndex}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                AssistChip(onClick = {}, label = { Text("${video.duration}s") })
            }
            OutlinedTextField(title, { title = it }, label = { Text("Titulo") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text("Descricao") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            OutlinedTextField(orderIndex, { orderIndex = it.filter(Char::isDigit) }, label = { Text("Ordem") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onUpdate(video.id, courseId, title, description, orderIndex.toIntOrNull() ?: video.orderIndex) }) {
                    Icon(Icons.Outlined.Save, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Salvar")
                }
                OutlinedButton(onClick = { onDelete(video.id, courseId) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Excluir")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveScheduleScreen(state: InstructorUiState, onCreate: (String, String, Int, String?) -> Unit) {
    var courseId by remember { mutableStateOf(state.courses.firstOrNull()?.id.orEmpty()) }
    var date by remember { mutableStateOf("2026-05-10T19:30:00.000Z") }
    var duration by remember { mutableStateOf("60") }
    var url by remember { mutableStateOf("") }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ScreenHero(
                title = "Agendar aula ao vivo",
                subtitle = "Defina curso, horario, duracao e link da sala para os alunos.",
                badge = "Ao vivo",
            )
        }
        item {
            PanelCard {
                StepTitle("1", "Curso e horario")
                OutlinedTextField(courseId, { courseId = it }, label = { Text("ID do curso") }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.courses.take(8).forEach { course ->
                        AssistChip(onClick = { courseId = course.id }, label = { Text(course.title) })
                    }
                }
                OutlinedTextField(date, { date = it }, label = { Text("Data/hora ISO") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(duration, { duration = it.filter(Char::isDigit) }, label = { Text("Duracao em minutos") }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("30", "45", "60", "90").forEach {
                        AssistChip(onClick = { duration = it }, label = { Text("${it} min") })
                    }
                }
                OutlinedTextField(url, { url = it }, label = { Text("Link da sala") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = { onCreate(courseId, date, duration.toIntOrNull() ?: 60, url.ifBlank { null }) },
                    enabled = courseId.isNotBlank() && date.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Agendar aula") }
            }
        }
        item { SectionTitle("Proximos agendamentos") }
        if (state.schedules.isEmpty()) {
            item { EmptyState("Agenda livre", "As aulas ao vivo marcadas aparecem aqui.") }
        }
        items(state.schedules) { ScheduleCard(it.courseTitle, it.scheduledAt, it.durationMin, it.meetingUrl) }
    }
}

@Composable
fun CoursesScreen(state: InstructorUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ScreenHero(
                title = "Cursos criados",
                subtitle = "${state.courses.size} entregas publicadas ou em preparacao.",
                badge = "Catalogo",
            )
        }
        if (state.courses.isEmpty()) {
            item { EmptyState("Nenhum curso criado", "Aceite um pedido e publique sua primeira entrega.") }
        }
        items(state.courses) {
            CourseCard(it.title, it.category, it.format, it.status, it.progressLabel, it.revenue)
        }
    }
}

@Composable
fun QaScreen(state: InstructorUiState, onAnswer: (String, String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ScreenHero(
                title = "Q&A",
                subtitle = "Responda duvidas dos alunos e acompanhe o que ja foi resolvido.",
                badge = "Suporte",
            )
        }
        if (state.questions.isEmpty()) {
            item { EmptyState("Sem perguntas abertas", "Quando alunos enviarem duvidas, elas entram nesta fila.") }
        }
        items(state.questions) { question ->
            QuestionCard(
                courseTitle = question.courseTitle,
                studentName = question.studentName,
                question = question.question,
                isResolved = question.isResolved,
                onAnswer = { answer -> onAnswer(question.id, answer) },
            )
        }
    }
}

@Composable
fun ReviewsScreen(state: InstructorUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ScreenHero(
                title = "Avaliacoes",
                subtitle = "Feedback dos alunos para ajustar proximas entregas.",
                badge = "Qualidade",
            )
        }
        if (state.reviews.isEmpty()) {
            item { EmptyState("Ainda sem avaliacoes", "As notas aparecem quando alunos concluirem cursos.") }
        }
        items(state.reviews) {
            ReviewCard(it.rating, it.courseTitle, it.studentName, it.comment)
        }
    }
}

@Composable
fun FinanceScreen(state: InstructorUiState) {
    val finance = state.finance
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ScreenHero(
                title = "Financeiro",
                subtitle = "Acompanhe receita, pendencias e cursos com melhor retorno.",
                badge = "Receita",
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStatCard("Ganhos totais", finance?.totalRevenue ?: "R$ 0,00", Modifier.weight(1f))
                MiniStatCard("Pendentes", finance?.pendingRevenue ?: "R$ 0,00", Modifier.weight(1f))
            }
        }
        item { SectionTitle("Historico de pagamentos") }
        if (finance?.payments.orEmpty().isEmpty()) {
            item { EmptyState("Sem pagamentos ainda", "Os repasses aparecem aqui quando houver vendas.") }
        }
        items(finance?.payments.orEmpty()) { InfoCard(it, "Repasse LearnOn") }
        item { SectionTitle("Cursos mais lucrativos") }
        items(finance?.topCourses.orEmpty()) { InfoCard(it, "Receita recorrente de microcursos") }
    }
}

@Composable
fun ProfileScreen(state: InstructorUiState) {
    val profile = state.profile
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ScreenHero(
                title = profile?.name ?: "Perfil profissional",
                subtitle = profile?.bio ?: "Bio ainda nao configurada.",
                badge = "Instrutor",
            )
        }
        item {
            PanelCard {
                DetailRow("Especialidades", profile?.specialties.orEmpty().joinToString().ifBlank { "Nao definido" })
                DetailRow("Links sociais", profile?.socialLinks.orEmpty().joinToString().ifBlank { "Nao definido" })
                DetailRow("Disponibilidade", profile?.availability ?: "Nao definida")
                DetailRow("Formatos aceitos", profile?.acceptedFormats.orEmpty().joinToString().ifBlank { "Nao definido" })
            }
        }
    }
}

@Composable
fun NotificationsScreen(state: InstructorUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ScreenHero(
                title = "Notificacoes",
                subtitle = "Atualizacoes importantes sobre pedidos, alunos e publicacoes.",
                badge = "Alertas",
            )
        }
        if (state.notifications.isEmpty()) {
            item { EmptyState("Tudo em dia", "Voce nao tem notificacoes no momento.") }
        }
        items(state.notifications) { NotificationCard(it.title, it.body, it.createdAt) }
    }
}

@Composable
fun AnalyticsScreen(state: InstructorUiState) {
    val analytics = state.analytics
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ScreenHero(
                title = "Analytics",
                subtitle = "Leia sinais de desempenho para decidir o proximo curso.",
                badge = "Desempenho",
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStatCard("Conclusao", analytics?.completionRate ?: "0%", Modifier.weight(1f))
                MiniStatCard("Resposta", analytics?.averageResponseTime ?: "-", Modifier.weight(1f))
            }
        }
        item { SectionTitle("Cursos mais vistos") }
        if (analytics?.topViewedCourses.orEmpty().isEmpty()) {
            item { EmptyState("Sem dados suficientes", "Os rankings aparecem depois que seus cursos receberem visitas.") }
        }
        items(analytics?.topViewedCourses.orEmpty()) { InfoCard(it, "Visualizacoes qualificadas") }
        item { SectionTitle("Categorias mais solicitadas") }
        items(analytics?.topCategories.orEmpty()) { InfoCard(it, "Demanda dos alunos") }
    }
}

@Composable
private fun ScreenHero(title: String, subtitle: String, badge: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(Color(0xFF4937A6), Color(0xFF17213A))),
                RoundedCornerShape(8.dp),
            )
            .padding(22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text(badge) })
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PanelCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun StepTitle(step: String, title: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(step, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    PanelCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CourseCard(title: String, category: String, format: String, status: String, progress: String, revenue: String) {
    PanelCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.AutoMirrored.Outlined.LibraryBooks, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(category, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text(format) })
            AssistChip(onClick = {}, label = { Text(status) })
        }
        DetailRow("Progresso", progress)
        DetailRow("Receita", revenue)
    }
}

@Composable
private fun ScheduleCard(title: String, scheduledAt: String, durationMin: Int, meetingUrl: String?) {
    PanelCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(scheduledAt, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AssistChip(onClick = {}, label = { Text("${durationMin}min") })
        }
        meetingUrl?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun QuestionCard(
    courseTitle: String,
    studentName: String,
    question: String,
    isResolved: Boolean,
    onAnswer: (String) -> Unit,
) {
    var answer by remember { mutableStateOf("") }
    PanelCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.QuestionAnswer, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(courseTitle, fontWeight = FontWeight.SemiBold)
                Text(studentName, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AssistChip(onClick = {}, label = { Text(if (isResolved) "Resolvida" else "Aberta") })
        }
        Text(question, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(answer, { answer = it }, label = { Text("Resposta") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Button(onClick = { onAnswer(answer.ifBlank { "Resposta enviada pelo painel mobile." }) }, modifier = Modifier.fillMaxWidth()) {
            Text("Enviar resposta")
        }
    }
}

@Composable
private fun ReviewCard(rating: Double, courseTitle: String, studentName: String, comment: String) {
    PanelCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(courseTitle, fontWeight = FontWeight.SemiBold)
                Text(studentName, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("%.1f".format(rating), color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
        }
        Text(comment, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NotificationCard(title: String, body: String, createdAt: String) {
    PanelCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(createdAt, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MiniStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    PanelCard {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
