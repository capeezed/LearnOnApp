package com.learnon.app.instructor.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.PlayCircle
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
            Text("Operacao de microcursos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Matches, prazos e entregas rapidas no mesmo painel.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("Atualizar painel") }
        }
    }
}

@Composable
fun RequestsScreen(
    state: InstructorUiState,
    onAccept: (String) -> Unit,
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
            SectionTitle("Pedidos recebidos")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("todos", "normal", "fast_track").forEach {
                    FilterChip(selected = urgency == it, onClick = { urgency = it }, label = { Text(it) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("todos", "live", "recorded", "no_preference").forEach {
                    FilterChip(selected = format == it, onClick = { format = it }, label = { Text(it) })
                }
            }
        }
        items(filtered) { request ->
            RequestCard(
                request = request,
                actions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onOpenDetail(request) }) { Text("Detalhes") }
                        OutlinedButton(onClick = { onReject(request.id) }) { Text("Rejeitar") }
                        Button(onClick = { onAccept(request.id) }) { Text("Aceitar") }
                    }
                },
            )
        }
    }
}

@Composable
fun RequestDetailScreen(request: InstructorRequest?) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionTitle("Detalhe do pedido") }
        if (request == null) {
            item { InfoCard("Nenhum pedido selecionado", "Abra um pedido na lista para ver o detalhe completo.") }
        } else {
            item { RequestCard(request) }
            item { DetailRow("Descricao da duvida", request.description) }
            item { DetailRow("Formato solicitado", request.formatPreference) }
            item { DetailRow("Urgencia", request.urgency) }
            item { DetailRow("Prazo", request.deadline ?: "dinamico") }
            item { DetailRow("Alunos interessados", request.interestedStudents.toString()) }
            item { DetailRow("Dificuldade estimada", request.difficulty) }
            item { DetailRow("Status do match", request.status) }
        }
    }
}

@Composable
fun CreateCourseScreen(state: InstructorUiState, onCreate: (String?, String, String) -> Unit) {
    var requestId by remember { mutableStateOf(state.pendingRequests.firstOrNull()?.id.orEmpty()) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            SectionTitle("Criar microcurso")
            Text("Fluxo preparado para rascunho, publicacao e entrega vinculada ao pedido.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { OutlinedTextField(requestId, { requestId = it }, label = { Text("ID do pedido") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(title, { title = it }, label = { Text("Titulo") }, modifier = Modifier.fillMaxWidth()) }
        item {
            OutlinedTextField(
                description,
                { description = it },
                label = { Text("Descricao") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {}) { Text("Salvar rascunho") }
                Button(onClick = { onCreate(requestId.ifBlank { null }, title, description) }, enabled = title.isNotBlank() && description.isNotBlank()) {
                    Text("Publicar/entregar")
                }
            }
        }
    }
}

@Composable
fun UploadScreen() {
    var progress by remember { mutableFloatStateOf(0.34f) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SectionTitle("Upload de video")
            Text("Preparado para multipart. TODO backend: confirmar endpoint de upload/processamento.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.PlayCircle, contentDescription = null)
                    Text("Preview do microcurso", fontWeight = FontWeight.SemiBold)
                    Text("video-refresh-token.mp4 - 128 MB", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text("Processamento: ${(progress * 100).toInt()}%")
                    Button(onClick = { progress = (progress + 0.18f).coerceAtMost(1f) }) { Text("Simular progresso") }
                    AnimatedVisibility(progress >= 1f) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text("Upload pronto para publicacao")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveScheduleScreen(state: InstructorUiState, onCreate: (String, String, Int, String?) -> Unit) {
    var courseId by remember { mutableStateOf(state.courses.firstOrNull()?.id.orEmpty()) }
    var date by remember { mutableStateOf("2026-05-10T19:30:00.000Z") }
    var duration by remember { mutableStateOf("60") }
    var url by remember { mutableStateOf("") }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionTitle("Agendar aula ao vivo") }
        item { OutlinedTextField(courseId, { courseId = it }, label = { Text("ID do curso") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(date, { date = it }, label = { Text("Data/hora ISO") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(duration, { duration = it }, label = { Text("Duracao em minutos") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(url, { url = it }, label = { Text("Link da sala") }, modifier = Modifier.fillMaxWidth()) }
        item { Button(onClick = { onCreate(courseId, date, duration.toIntOrNull() ?: 60, url.ifBlank { null }) }) { Text("Agendar") } }
        item { SectionTitle("Proximos agendamentos") }
        items(state.schedules) { InfoCard(it.courseTitle, "${it.scheduledAt} - ${it.durationMin} min") }
    }
}

@Composable
fun CoursesScreen(state: InstructorUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionTitle("Cursos criados") }
        items(state.courses) {
            InfoCard(it.title, "${it.category} - ${it.format} - ${it.status} - ${it.revenue}")
        }
    }
}

@Composable
fun QaScreen(state: InstructorUiState, onAnswer: (String, String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionTitle("Q&A") }
        items(state.questions) { question ->
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(question.courseTitle, fontWeight = FontWeight.SemiBold)
                    Text("${question.studentName}: ${question.question}")
                    AssistChip(onClick = {}, label = { Text(if (question.isResolved) "Resolvida" else "Aberta") })
                    Button(onClick = { onAnswer(question.id, "Resposta enviada pelo painel mobile.") }) { Text("Responder") }
                }
            }
        }
    }
}

@Composable
fun ReviewsScreen(state: InstructorUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionTitle("Avaliacoes") }
        items(state.reviews) {
            InfoCard("${it.rating} - ${it.courseTitle}", "${it.studentName}: ${it.comment}")
        }
    }
}

@Composable
fun FinanceScreen(state: InstructorUiState) {
    val finance = state.finance
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionTitle("Financeiro") }
        item { InfoCard("Ganhos totais", finance?.totalRevenue ?: "R$ 0,00") }
        item { InfoCard("Ganhos pendentes", finance?.pendingRevenue ?: "R$ 0,00") }
        item { SectionTitle("Historico de pagamentos") }
        items(finance?.payments.orEmpty()) { InfoCard(it, "Repasse LearnOn") }
        item { SectionTitle("Cursos mais lucrativos") }
        items(finance?.topCourses.orEmpty()) { InfoCard(it, "Receita recorrente de microcursos") }
    }
}

@Composable
fun ProfileScreen(state: InstructorUiState) {
    val profile = state.profile
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionTitle("Perfil profissional") }
        item { InfoCard(profile?.name ?: "Instrutor", profile?.bio ?: "Bio ainda nao configurada.") }
        item { DetailRow("Especialidades", profile?.specialties.orEmpty().joinToString()) }
        item { DetailRow("Links sociais", profile?.socialLinks.orEmpty().joinToString()) }
        item { DetailRow("Disponibilidade", profile?.availability ?: "Nao definida") }
        item { DetailRow("Formatos aceitos", profile?.acceptedFormats.orEmpty().joinToString()) }
    }
}

@Composable
fun NotificationsScreen(state: InstructorUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionTitle("Notificacoes") }
        items(state.notifications) { InfoCard(it.title, "${it.body} - ${it.createdAt}") }
    }
}

@Composable
fun AnalyticsScreen(state: InstructorUiState) {
    val analytics = state.analytics
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionTitle("Analytics do instrutor") }
        item { InfoCard("Taxa de conclusao", analytics?.completionRate ?: "0%") }
        item { InfoCard("Tempo medio de resposta", analytics?.averageResponseTime ?: "-") }
        item { SectionTitle("Cursos mais vistos") }
        items(analytics?.topViewedCourses.orEmpty()) { InfoCard(it, "Visualizacoes qualificadas") }
        item { SectionTitle("Categorias mais solicitadas") }
        items(analytics?.topCategories.orEmpty()) { InfoCard(it, "Demanda dos alunos") }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
