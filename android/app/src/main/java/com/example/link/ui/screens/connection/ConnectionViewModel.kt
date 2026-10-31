package com.example.link.ui.screens.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.link.data.ConnectionStore
import com.example.link.data.GuardianRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConnectionUiState(
    val baseUrl: String = "",
    val checking: Boolean = false,
    val status: String? = null,
)

class ConnectionViewModel(
    private val store: ConnectionStore,
    private val repository: GuardianRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectionUiState(baseUrl = store.baseUrl))
    val state: StateFlow<ConnectionUiState> = _state.asStateFlow()

    fun onBaseUrlChange(value: String) = _state.update { it.copy(baseUrl = value, status = null) }

    fun saveAndCheck() {
        val current = _state.value
        _state.update { it.copy(checking = true, status = null) }
        viewModelScope.launch {
            val reachable = repository.checkHealth(current.baseUrl)
            if (reachable) store.baseUrl = current.baseUrl
            _state.update {
                it.copy(
                    checking = false,
                    status = if (reachable) "Conexión lista. El backend responde."
                    else "No pude llegar al backend. Revisa la dirección y que esté encendido."
                )
            }
        }
    }
}