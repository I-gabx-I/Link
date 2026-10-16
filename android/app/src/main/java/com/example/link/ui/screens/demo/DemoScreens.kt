package com.example.link.ui.screens.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.link.data.LinkContact
import com.example.link.data.LinkPreferences
import com.example.link.data.LinkResponse
import com.example.link.ui.components.*
import com.example.link.ui.theme.LinkMuted

@Composable
private fun DemoPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(20.dp)) {
            LinkTopBar(onBack = onBack)
            Spacer(Modifier.height(20.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
        }
    }
}

@Composable
fun DemoDashboard(contactCount: Int, onBack: () -> Unit, onContacts: () -> Unit, onConnection: () -> Unit, onWrite: () -> Unit) {
    DemoPage("Panel del cuidador", onBack) {
        LinkCard {
            Text("Mensajes por Telegram", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("Guarda a una persona de confianza y pide un mensaje con tu voz.")
            Text("Contactos guardados: $contactCount", color = LinkMuted)
        }
        PrimaryButton("Contactos", onContacts, leadingIcon = LinkIconType.Person)
        SecondaryButton("Conexión del backend", onConnection)
        SecondaryButton("Escribir una solicitud", onWrite)
        SecondaryButton("Volver al micrófono", onBack)
    }
}

@Composable
fun ConnectionScreen(preferences: LinkPreferences, busy: Boolean, status: String, onBack: () -> Unit, onCheck: () -> Unit) {
    var url by rememberSaveable { mutableStateOf(preferences.gatewayUrl) }
    var secret by remember { mutableStateOf(preferences.appSecret) }
    var error by remember { mutableStateOf("") }
    DemoPage("Conectar con el asistente", onBack) {
        Text("Usa la dirección y la clave del backend de esta demostración.", color = LinkMuted)
        Text("Demo 1.1 · USB y Wi-Fi", color = LinkMuted)
        TextButton(onClick = { url = "http://127.0.0.1:8000"; error = "" }, enabled = !busy) { Text("Usar conexión USB") }
        OutlinedTextField(url, { url = it }, label = { Text("Dirección del backend") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(secret, { secret = it }, label = { Text("Clave de conexión") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
        Text("USB: usa http://127.0.0.1:8000 después de preparar el enlace en la computadora y conserva el cable conectado. Wi-Fi: usa la IP actual de la computadora. Emulador: http://10.0.2.2:8000.", color = LinkMuted)
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        if (status.isNotEmpty()) Text(status)
        if (busy) CircularProgressIndicator()
        PrimaryButton("Guardar y comprobar", {
            try {
                preferences.saveConnection(url, secret)
                error = ""
                onCheck()
            } catch (exception: Exception) {
                error = exception.message ?: "Revisa la dirección y la clave."
            }
        }, enabled = !busy)
    }
}

@Composable
fun ContactsEditorScreen(
    contacts: List<LinkContact>, busy: Boolean, status: String,
    onBack: () -> Unit, onConnection: () -> Unit, onLoad: () -> Unit, onSave: (LinkContact) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var aliases by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { onLoad() }
    DemoPage("Contactos de Telegram", onBack) {
        if (contacts.isEmpty()) Text("Agrega tu primer contacto para preparar un mensaje.", color = LinkMuted)
        contacts.forEach { contact ->
            LinkCard {
                Text(contact.name, style = MaterialTheme.typography.titleMedium)
                Text("@${contact.username}")
                if (contact.aliases.isNotEmpty()) Text(contact.aliases.joinToString(", "), color = LinkMuted)
                TextButton(onClick = { name = contact.name; username = contact.username; aliases = contact.aliases.joinToString(", ") }) { Text("Editar") }
            }
        }
        Text(if (name.isBlank()) "Agregar contacto" else "Datos del contacto", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(username, { username = it }, label = { Text("Usuario de Telegram") }, placeholder = { Text("@usuario") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(aliases, { aliases = it }, label = { Text("También lo llamo… (opcional)") }, placeholder = { Text("mamá, mi mamá") }, modifier = Modifier.fillMaxWidth())
        if (status.isNotBlank()) Text(status)
        if (busy) CircularProgressIndicator()
        PrimaryButton("Guardar contacto", {
            onSave(LinkContact(name, username, aliases.split(',').map { it.trim() }.filter { it.isNotEmpty() }))
        }, enabled = !busy && name.isNotBlank() && username.isNotBlank())
        TextButton(onClick = { name = ""; username = ""; aliases = "" }) { Text("Nuevo contacto") }
        TextButton(onClick = onLoad, enabled = !busy) { Text("Actualizar contactos") }
        SecondaryButton("Revisar conexión", onConnection)
    }
}

@Composable
fun AssistantScreen(
    text: String, response: LinkResponse?, busy: Boolean,
    onText: (String) -> Unit, onSend: () -> Unit, onVoice: () -> Unit,
    onConfirm: () -> Unit, onCancel: () -> Unit, onTelegram: () -> Unit,
    onBack: () -> Unit, onConnection: () -> Unit,
) {
    DemoPage("Tu asistente", onBack) {
        if (response != null) {
            LinkCard {
                Text(if (response.type == "confirmation") "Antes de continuar" else "Respuesta", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                Text(response.reply, color = if (response.failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                if (response.summary.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(response.summary, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (busy) {
            CircularProgressIndicator()
            Text("Estoy preparando tu solicitud…", color = LinkMuted)
        }
        if (response?.type == "confirmation") {
            PrimaryButton("Sí, preparar mensaje", onConfirm, enabled = !busy)
            if (!busy) SecondaryButton("No, cancelar", onCancel)
        } else {
            if (response?.actionUrl != null && !busy) {
                PrimaryButton("Abrir Telegram", onTelegram, leadingIcon = LinkIconType.Message)
                Text("Revisa el mensaje y toca Enviar dentro de Telegram.", color = LinkMuted)
            }
            OutlinedTextField(text, onText, label = { Text(if (response?.type == "clarification") "Tu respuesta" else "¿Qué necesitas?") }, modifier = Modifier.fillMaxWidth(), minLines = 2, enabled = !busy)
            PrimaryButton("Enviar solicitud", onSend, enabled = !busy && text.isNotBlank())
        }
        if (!busy) {
            SecondaryButton("Dictar con mi voz", onVoice)
            TextButton(onClick = onConnection) { Text("Revisar conexión") }
        }
    }
}
