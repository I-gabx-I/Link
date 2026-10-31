package com.example.link.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.link.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false,
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun signIn() = submit { email, password -> auth.signIn(email, password) }

    fun register() = submit { email, password -> auth.register(email, password) }

    private fun submit(action: suspend (String, String) -> String?) {
        val current = _state.value
        if (current.loading) return
        if (current.email.isBlank() || current.password.isBlank()) {
            _state.update { it.copy(error = "Escribe tu correo y tu contraseña.") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val error = action(current.email, current.password)
            _state.update { it.copy(loading = false, error = error, loggedIn = error == null) }
        }
    }
}