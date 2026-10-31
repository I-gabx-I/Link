package com.example.link.ui.screens.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.link.data.AssistantReply
import com.example.link.data.GuardianRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssistantUiState(
    val input: String = "",
    val sending: Boolean = false,
    val lastRequest: String? = null,
    val reply: AssistantReply? = null,
)

class AssistantViewModel(private val repository: GuardianRepository) : ViewModel() {

    private val _state = MutableStateFlow(AssistantUiState())
    val state: StateFlow<AssistantUiState> = _state.asStateFlow()

    fun onInputChange(value: String) = _state.update { it.copy(input = value) }

    fun send() {
        val text = _state.value.input.trim()
        if (text.isEmpty()) return
        submit(text, clearInput = true)
    }

    fun confirm() = submit("sí")

    fun cancel() = submit("no")

    private fun submit(message: String, clearInput: Boolean = false) {
        if (_state.value.sending) return
        _state.update {
            it.copy(
                sending = true,
                lastRequest = message,
                input = if (clearInput) "" else it.input,
            )
        }
        viewModelScope.launch {
            val reply = repository.sendMessage(message)
            _state.update { it.copy(sending = false, reply = reply) }
        }
    }
}