package com.learnon.app.instructor.registration

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.learnon.app.instructor.data.remote.InstructorNetwork

private val knowledgeAreas = listOf("Android", "Backend", "Frontend", "Dados", "IA", "Design", "Produto", "Cloud")
private val experienceLevels = listOf("Junior", "Pleno", "Senior", "Especialista")
private val classFormats = listOf("Ao vivo", "Gravadas", "Ambos")
private val availabilityOptions = listOf("Manhas", "Tardes", "Noites", "Seg-Sex", "Fins de semana")
private val priceRanges = listOf("R$ 50-100", "R$ 100-200", "R$ 200-400", "R$ 400+")
private val responseTimes = listOf("Ate 2h", "Mesmo dia", "24h", "48h")

@Composable
fun TeacherRegistrationRoute(
    onBack: () -> Unit,
    onSuccessDone: () -> Unit,
) {
    val context = LocalContext.current
    val api = InstructorNetwork.createApi(context)
    val viewModel: TeacherRegistrationViewModel = viewModel(
        factory = TeacherRegistrationViewModelFactory(api),
    )
    val state by viewModel.state.collectAsState()
    if (state.isSuccess) {
        TeacherRegistrationSuccessScreen(onDone = onSuccessDone)
    } else {
        TeacherRegistrationScreen(
            state = state,
            onBack = onBack,
            onUpdate = viewModel::updateForm,
            onSubmit = viewModel::submit,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TeacherRegistrationScreen(
    state: TeacherRegistrationUiState,
    onBack: () -> Unit,
    onUpdate: ((TeacherRegistrationForm) -> TeacherRegistrationForm) -> Unit,
    onSubmit: () -> Unit,
) {
    val form = state.form
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onUpdate { it.copy(profilePhotoUri = uri?.toString()) }
    }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onUpdate { it.copy(documentUri = uri?.toString()) }
    }
    val certificateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onUpdate { it.copy(certificateUri = uri?.toString()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Candidatura de instrutor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                RegistrationHero()
            }

            item {
                FormSection("Dados pessoais", Icons.Outlined.Person) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProfilePreview(form.profilePhotoUri, form.fullName)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Foto de perfil", fontWeight = FontWeight.SemiBold)
                            Text("Use uma imagem profissional para aumentar a confianca no match.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(onClick = { photoLauncher.launch("image/*") }) {
                                Icon(Icons.Outlined.CloudUpload, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Selecionar foto")
                            }
                        }
                    }
                    FormTextField(form.fullName, { value -> onUpdate { it.copy(fullName = value) } }, "Nome completo", state.fieldErrors["fullName"])
                    FormTextField(form.email, { value -> onUpdate { it.copy(email = value) } }, "Email", state.fieldErrors["email"])
                    FormTextField(form.phone, { value -> onUpdate { it.copy(phone = value) } }, "Telefone", state.fieldErrors["phone"])
                    FormTextField(form.location, { value -> onUpdate { it.copy(location = value) } }, "Cidade/Estado", state.fieldErrors["location"])
                    FormTextField(form.bio, { value -> onUpdate { it.copy(bio = value) } }, "Mini bio profissional", state.fieldErrors["bio"], minLines = 4)
                }
            }

            item {
                FormSection("Informacoes profissionais", Icons.Outlined.Badge) {
                    FieldLabel("Areas de conhecimento", state.fieldErrors["knowledgeAreas"])
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        knowledgeAreas.forEach { option ->
                            FilterChip(
                                selected = option in form.knowledgeAreas,
                                onClick = {
                                    onUpdate {
                                        it.copy(knowledgeAreas = it.knowledgeAreas.toggle(option))
                                    }
                                },
                                label = { Text(option) },
                            )
                        }
                    }
                    FormTextField(form.subjects, { value -> onUpdate { it.copy(subjects = value) } }, "Tecnologias/materias dominadas", state.fieldErrors["subjects"])
                    FieldLabel("Nivel de experiencia", state.fieldErrors["experienceLevel"])
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        experienceLevels.forEach { option ->
                            InputChip(
                                selected = form.experienceLevel == option,
                                onClick = { onUpdate { it.copy(experienceLevel = option) } },
                                label = { Text(option) },
                            )
                        }
                    }
                    FormTextField(form.yearsExperience, { value -> onUpdate { it.copy(yearsExperience = value.filter(Char::isDigit)) } }, "Anos de experiencia", state.fieldErrors["yearsExperience"])
                    FormTextField(form.linkedInUrl, { value -> onUpdate { it.copy(linkedInUrl = value) } }, "Link LinkedIn", state.fieldErrors["linkedInUrl"], leadingIcon = Icons.Outlined.Link)
                    FormTextField(form.githubUrl, { value -> onUpdate { it.copy(githubUrl = value) } }, "Link GitHub", state.fieldErrors["githubUrl"])
                    FormTextField(form.portfolioUrl, { value -> onUpdate { it.copy(portfolioUrl = value) } }, "Portfolio opcional", null)
                }
            }

            item {
                FormSection("Configuracao de aulas", Icons.Outlined.Description) {
                    FieldLabel("Aceita aulas", state.fieldErrors["classFormat"])
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        classFormats.forEach { option ->
                            FilterChip(
                                selected = form.classFormat == option,
                                onClick = { onUpdate { it.copy(classFormat = option) } },
                                label = { Text(option) },
                            )
                        }
                    }
                    FieldLabel("Disponibilidade semanal", state.fieldErrors["weeklyAvailability"])
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availabilityOptions.forEach { option ->
                            FilterChip(
                                selected = option in form.weeklyAvailability,
                                onClick = { onUpdate { it.copy(weeklyAvailability = it.weeklyAvailability.toggle(option)) } },
                                label = { Text(option) },
                            )
                        }
                    }
                    FieldLabel("Faixa de preco sugerida", state.fieldErrors["suggestedPriceRange"])
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        priceRanges.forEach { option ->
                            InputChip(
                                selected = form.suggestedPriceRange == option,
                                onClick = { onUpdate { it.copy(suggestedPriceRange = option) } },
                                label = { Text(option) },
                            )
                        }
                    }
                    FieldLabel("Tempo medio de resposta", state.fieldErrors["averageResponseTime"])
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        responseTimes.forEach { option ->
                            InputChip(
                                selected = form.averageResponseTime == option,
                                onClick = { onUpdate { it.copy(averageResponseTime = option) } },
                                label = { Text(option) },
                            )
                        }
                    }
                }
            }

            item {
                FormSection("Verificacao", Icons.Outlined.Verified) {
                    UploadRow(
                        title = "Documento",
                        uri = form.documentUri,
                        error = state.fieldErrors["documentUri"],
                        onClick = { documentLauncher.launch("*/*") },
                    )
                    UploadRow(
                        title = "Certificado opcional",
                        uri = form.certificateUri,
                        error = null,
                        onClick = { certificateLauncher.launch("*/*") },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = form.acceptedTerms,
                            onCheckedChange = { checked -> onUpdate { it.copy(acceptedTerms = checked) } },
                        )
                        Text("Aceito os termos da plataforma e a verificacao do meu perfil.", modifier = Modifier.weight(1f))
                    }
                    state.fieldErrors["acceptedTerms"]?.let { ErrorText(it) }
                }
            }

            item {
                state.error?.let { ErrorBanner(it) }
                Button(
                    onClick = onSubmit,
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Enviar candidatura", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TeacherRegistrationSuccessScreen(onDone: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF16A34A).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF6B5CFF), modifier = Modifier.size(54.dp))
            }
            Spacer(Modifier.height(22.dp))
            Text("Candidatura enviada", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                "Seu perfil entrou na fila de verificacao. Quando aprovado, o LearnOn libera matches com alunos e criacao de microcursos.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
            Spacer(Modifier.height(28.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Voltar para acesso do instrutor")
            }
        }
    }
}

@Composable
private fun RegistrationHero() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF172033),
                            Color(0xFF080C16),
                            Color(0xFF0F1222),
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ElevatedAssistChip(onClick = {}, label = { Text("Professor LearnOn") })
            Text("Transforme duvidas reais em microcursos sob demanda", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Complete sua candidatura para entrar no sistema de match automatico com alunos.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FormSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    minLines: Int = 1,
    leadingIcon: ImageVector? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FieldLabel(label: String, error: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        error?.let { ErrorText(it) }
    }
}

@Composable
private fun UploadRow(title: String, uri: String?, error: String?, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(uri?.let { Uri.parse(it).lastPathSegment ?: "Arquivo selecionado" } ?: "Nenhum arquivo selecionado", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onClick) { Text(if (uri == null) "Enviar" else "Trocar") }
        }
        error?.let { ErrorText(it) }
    }
}

@Composable
private fun ProfilePreview(uri: String?, fullName: String) {
    val initials = fullName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "LO" }

    Box(
        Modifier
            .size(82.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (uri == null) initials else "OK", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        color = Color(0xFF7F1D1D).copy(alpha = 0.24f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Text(message, color = Color(0xFFFCA5A5), modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(message, color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodySmall)
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (value in this) this - value else this + value
