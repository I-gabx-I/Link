package com.example.link.ui.screens.connection

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.example.link.ui.components.LinkTopBar
import com.example.link.ui.components.PrimaryButton
import com.example.link.ui.theme.LinkMuted

// TEMPORAL (P6): se elimina cuando la URL de Cloud Run vaya fija en la app.
@Composable
fun ConnectionScreen(viewModel: ConnectionViewModel, onBack: () -> Unit) {
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
            Text("Conexión con el asistente", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Dirección del backend. Temporal hasta tener una URL fija (P6).",
                color = LinkMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = viewModel::onBaseUrlChange,
                label = { Text("Dirección del backend") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = if (state.checking) "Comprobando…" else "Guardar y comprobar",
                onClick = {
                    focusManager.clearFocus()
                    viewModel.saveAndCheck()
                },
                enabled = !state.checking
            )
            state.status?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}