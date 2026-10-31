package com.example.link.ui.screens.assistant

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.link.data.AssistantReply
import com.example.link.data.ReplyType
import com.example.link.ui.components.LinkCard
import com.example.link.ui.components.LinkTopBar
import com.example.link.ui.components.PrimaryButton
import com.example.link.ui.components.SecondaryButton
import com.example.link.ui.theme.LinkBlue
import com.example.link.ui.theme.LinkMuted
import com.example.link.ui.theme.LinkSuccess

@Composable
fun AssistantScreen(viewModel: AssistantViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .safeDrawingPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            LinkTopBar(onBack = onBack)
            Spacer(Modifier.height(20.dp))
            Text("¿En qué puedo ayudarte?", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::onInputChange,
                label = { Text("Escribe tu solicitud") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            PrimaryButton(
                text = if (state.sending) "Un momento…" else "Enviar",
                onClick = {
                    focusManager.clearFocus()
                    viewModel.send()
                },
                enabled = !state.sending && state.input.isNotBlank()
            )

            state.lastRequest?.let {
                Spacer(Modifier.height(20.dp))
                Text("Pediste: $it", color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
            }

            state.reply?.let { reply ->
                Spacer(Modifier.height(12.dp))
                ReplyCard(
                    reply = reply,
                    sending = state.sending,
                    onConfirm = viewModel::confirm,
                    onCancel = viewModel::cancel
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReplyCard(
    reply: AssistantReply,
    sending: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val (label, color) = when (reply.type) {
        ReplyType.CONFIRMATION -> "ANTES DE CONTINUAR" to LinkBlue
        ReplyType.RESULT -> "LISTO" to LinkSuccess
        ReplyType.DENIED -> "NO PERMITIDO" to MaterialTheme.colorScheme.error
        ReplyType.INFO -> "ASISTENTE" to LinkMuted
        ReplyType.ERROR -> "HUBO UN PROBLEMA" to MaterialTheme.colorScheme.error
    }

    LinkCard {
        Text(label, color = color, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))

        if (reply.type == ReplyType.CONFIRMATION) {
            Text("¿Confirmas esta acción?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(reply.summary ?: reply.reply, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Sí, confirmar", onConfirm, enabled = !sending)
            Spacer(Modifier.height(10.dp))
            SecondaryButton("No, cancelar", onCancel)
        } else {
            Text(reply.reply, style = MaterialTheme.typography.bodyLarge)
            reply.actionUrl?.let { url ->
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Abrir", onClick = { runCatching { uriHandler.openUri(url) } })
            }
        }
    }
}

private infix fun String.to(color: Color) = Pair(this, color)