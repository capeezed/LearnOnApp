package com.learnon.app.instructor.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learnon.app.instructor.data.remote.InstructorApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeacherRegistrationViewModel(
    private val api: InstructorApi,
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherRegistrationUiState())
    val state: StateFlow<TeacherRegistrationUiState> = _state

    fun updateForm(transform: (TeacherRegistrationForm) -> TeacherRegistrationForm) {
        _state.update { it.copy(form = transform(it.form), error = null, fieldErrors = emptyMap()) }
    }

    fun submit() {
        val form = _state.value.form
        val errors = validate(form)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors, error = "Revise os campos destacados.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, fieldErrors = emptyMap()) }
            val request = form.toRequest()
            runCatching {
                val response = api.createTeacherApplication(request)
                if (!response.isSuccessful) error("Nao foi possivel enviar candidatura (${response.code()}).")
            }
                .onSuccess { _state.update { it.copy(isLoading = false, isSuccess = true) } }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Falha ao enviar candidatura.",
                        )
                    }
                }
        }
    }

    private fun validate(form: TeacherRegistrationForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (form.fullName.trim().length < 3) errors["fullName"] = "Informe seu nome completo."
        if (!form.email.contains("@") || !form.email.contains(".")) errors["email"] = "Informe um e-mail valido."
        if (form.phone.filter(Char::isDigit).length < 10) errors["phone"] = "Informe um telefone com DDD."
        if (form.bio.trim().length < 40) errors["bio"] = "Escreva uma bio com pelo menos 40 caracteres."
        if (form.location.trim().length < 4) errors["location"] = "Informe cidade e estado."
        if (form.knowledgeAreas.isEmpty()) errors["knowledgeAreas"] = "Selecione pelo menos uma area."
        if (form.subjects.trim().length < 3) errors["subjects"] = "Liste tecnologias ou materias dominadas."
        if (form.experienceLevel.isBlank()) errors["experienceLevel"] = "Selecione o nivel de experiencia."
        if ((form.yearsExperience.toIntOrNull() ?: -1) < 0) errors["yearsExperience"] = "Informe anos de experiencia."
        if (!form.linkedInUrl.startsWith("http")) errors["linkedInUrl"] = "Informe um link completo do LinkedIn."
        if (!form.githubUrl.startsWith("http")) errors["githubUrl"] = "Informe um link completo do GitHub."
        if (form.classFormat.isBlank()) errors["classFormat"] = "Escolha o formato das aulas."
        if (form.weeklyAvailability.isEmpty()) errors["weeklyAvailability"] = "Selecione sua disponibilidade."
        if (form.suggestedPriceRange.isBlank()) errors["suggestedPriceRange"] = "Escolha uma faixa de preco."
        if (form.averageResponseTime.isBlank()) errors["averageResponseTime"] = "Escolha o tempo medio de resposta."
        if (form.documentUri == null) errors["documentUri"] = "Envie um documento para verificacao."
        if (!form.acceptedTerms) errors["acceptedTerms"] = "Aceite os termos para continuar."
        return errors
    }
}

class TeacherRegistrationViewModelFactory(
    private val api: InstructorApi,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TeacherRegistrationViewModel(api) as T
    }
}
