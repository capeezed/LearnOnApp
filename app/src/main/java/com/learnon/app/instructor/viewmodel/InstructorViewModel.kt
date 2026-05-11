package com.learnon.app.instructor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.learnon.app.instructor.data.remote.InstructorTokenStore
import com.learnon.app.instructor.domain.repository.InstructorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InstructorViewModel(
    private val repository: InstructorRepository,
    private val tokenStore: InstructorTokenStore,
) : ViewModel() {
    private val _state = MutableStateFlow(InstructorUiState(isAuthenticated = tokenStore.hasToken()))
    val state: StateFlow<InstructorUiState> = _state

    init {
        if (_state.value.isAuthenticated) refreshAll()
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            repository.login(email, password)
                .onSuccess {
                    _state.update { it.copy(isAuthenticated = true, isLoading = false, message = "Bem-vindo ao painel do instrutor.") }
                    refreshAll()
                }
                .onFailure { err ->
                    _state.update { it.copy(isLoading = false, message = err.message ?: "Falha no login.") }
                }
        }
    }

    fun logout() {
        tokenStore.clear()
        _state.value = InstructorUiState(isAuthenticated = false, message = "Sessao encerrada.")
    }

    fun refreshAll() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }

            val dashboard = repository.dashboard()
            val pending = repository.pendingRequests()
            val queue = repository.queueRequests()
            val courses = repository.createdCourses()
            val schedules = repository.schedules()
            val questions = repository.questions()
            val reviews = repository.reviews()
            val finance = repository.finance()
            val profile = repository.profile()
            val notifications = repository.notifications()
            val analytics = repository.analytics()

            _state.update {
                it.copy(
                    isLoading = false,
                    dashboard = dashboard.getOrNull(),
                    pendingRequests = pending.getOrElse { emptyList() },
                    queueRequests = queue.getOrElse { emptyList() },
                    courses = courses.getOrElse { emptyList() },
                    schedules = schedules.getOrElse { emptyList() },
                    questions = questions.getOrElse { emptyList() },
                    reviews = reviews.getOrElse { emptyList() },
                    finance = finance.getOrNull(),
                    profile = profile.getOrNull(),
                    notifications = notifications.getOrElse { emptyList() },
                    analytics = analytics.getOrNull(),
                    message = dashboard.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun acceptRequest(id: String) = respond(id, accepted = true)
    fun rejectRequest(id: String) = respond(id, accepted = false)
    fun selectRequest(request: com.learnon.app.instructor.domain.model.InstructorRequest?) {
        _state.update { it.copy(selectedRequest = request) }
    }

    private fun respond(id: String, accepted: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            val result = if (accepted) repository.acceptRequest(id) else repository.rejectRequest(id)
            result
                .onSuccess {
                    _state.update { it.copy(message = if (accepted) "Pedido aceito." else "Pedido rejeitado.") }
                    refreshAll()
                }
                .onFailure { err -> _state.update { it.copy(isLoading = false, message = err.message) } }
        }
    }

    fun createCourse(requestId: String?, title: String, description: String, format: String, durationMinutes: Int, price: Double) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            repository.createCourse(requestId, title, description, format, durationMinutes, price)
                .onSuccess {
                    _state.update { it.copy(message = "Curso publicado e alunos matriculados.", selectedRequest = null) }
                    refreshAll()
                }
                .onFailure { err -> _state.update { it.copy(isLoading = false, message = err.message) } }
        }
    }

    fun loadVideos(courseId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, selectedCourseId = courseId, message = null) }
            repository.courseVideos(courseId)
                .onSuccess { videos -> _state.update { it.copy(isLoading = false, videos = videos) } }
                .onFailure { err -> _state.update { it.copy(isLoading = false, message = err.message) } }
        }
    }

    fun uploadVideo(courseId: String, title: String, description: String, orderIndex: Int, uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = "Enviando video...") }
            repository.uploadCourseVideo(courseId, title, description, orderIndex, uri)
                .onSuccess {
                    _state.update { it.copy(message = "Video enviado.") }
                    loadVideos(courseId)
                }
                .onFailure { err -> _state.update { it.copy(isLoading = false, message = err.message) } }
        }
    }

    fun updateVideo(videoId: String, courseId: String, title: String, description: String, orderIndex: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            repository.updateCourseVideo(videoId, title, description, orderIndex)
                .onSuccess {
                    _state.update { it.copy(message = "Video atualizado.") }
                    loadVideos(courseId)
                }
                .onFailure { err -> _state.update { it.copy(isLoading = false, message = err.message) } }
        }
    }

    fun deleteVideo(videoId: String, courseId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            repository.deleteCourseVideo(videoId)
                .onSuccess {
                    _state.update { it.copy(message = "Video removido.") }
                    loadVideos(courseId)
                }
                .onFailure { err -> _state.update { it.copy(isLoading = false, message = err.message) } }
        }
    }

    fun createSchedule(courseId: String, scheduledAt: String, durationMin: Int, meetingUrl: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            repository.createSchedule(courseId, scheduledAt, durationMin, meetingUrl)
                .onSuccess {
                    _state.update { it.copy(message = "Aula ao vivo agendada.") }
                    refreshAll()
                }
                .onFailure { err -> _state.update { it.copy(isLoading = false, message = err.message) } }
        }
    }

    fun answerQuestion(questionId: String, answer: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            repository.answerQuestion(questionId, answer)
                .onSuccess {
                    _state.update { it.copy(message = "Resposta enviada.") }
                    refreshAll()
                }
                .onFailure { err -> _state.update { it.copy(isLoading = false, message = err.message) } }
        }
    }
}

class InstructorViewModelFactory(
    private val repository: InstructorRepository,
    private val tokenStore: InstructorTokenStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return InstructorViewModel(repository, tokenStore) as T
    }
}
