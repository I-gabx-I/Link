package com.example.link.navigation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.link.data.*
import com.example.link.speech.SystemSpeaker
import com.example.link.ui.screens.demo.*
import com.example.link.ui.screens.senior.ListeningScreen
import com.example.link.ui.screens.senior.SeniorHomeScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun LinkApp(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { LinkPreferences(context) }
    val repository = remember(preferences) { LinkRepository(preferences) }
    val speaker = remember(context) { SystemSpeaker(context) }
    val scope = rememberCoroutineScope()
    var contacts by remember { mutableStateOf(preferences.cachedContacts()) }
    var input by rememberSaveable { mutableStateOf("") }
    var recognizedText by rememberSaveable { mutableStateOf("") }
    var response by remember { mutableStateOf<LinkResponse?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    DisposableEffect(speaker) { onDispose { speaker.destroy() } }

    fun home() {
        speaker.stop()
        navController.popBackStack("home", false)
    }

    fun rememberContacts(reply: LinkResponse) {
        reply.contacts?.let { contacts = it; preferences.cacheContacts(it) }
    }

    fun loadContacts(checkGateway: Boolean = false) {
        if (busy) return
        busy = true
        status = "Comprobando conexión…"
        scope.launch {
            try {
                if (checkGateway) {
                    repository.checkGateway()
                    status = "El backend responde. Verificando la clave y cargando contactos…"
                }
                val reply = repository.contacts()
                rememberContacts(reply)
                status = if (reply.failed) reply.reply else "Conexión lista. Contactos actualizados."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                status = error.message ?: "No pude actualizar los contactos."
            } finally { busy = false }
        }
    }

    fun saveContact(contact: LinkContact) {
        if (busy) return
        busy = true
        status = "Guardando contacto…"
        scope.launch {
            try {
                val reply = repository.saveContact(contact.name, contact.username, contact.aliases)
                rememberContacts(reply)
                status = reply.reply
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                status = error.message ?: "No pude guardar el contacto."
            } finally { busy = false }
        }
    }

    fun send(text: String) {
        if (busy || text.isBlank()) return
        busy = true
        response = null
        input = ""
        navController.navigate("assistant") { launchSingleTop = true }
        scope.launch {
            try {
                val reply = repository.message(text)
                response = reply
                rememberContacts(reply)
                speaker.speak(listOf(reply.reply, reply.summary).filter { it.isNotBlank() }.joinToString(" "))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                response = LinkResponse("error", error.message ?: "No pude comunicarme con el asistente.")
            } finally { busy = false }
        }
    }

    fun voice() {
        speaker.stop()
        recognizedText = ""
        navController.navigate("listen") { launchSingleTop = true }
    }

    fun connection() {
        status = ""
        navController.navigate("connection") { launchSingleTop = true }
    }

    fun openTelegram() {
        val url = response?.actionUrl ?: return
        val uri = Uri.parse(url)
        if (uri.scheme != "https" || uri.host != "t.me" || uri.pathSegments.size != 1) {
            response = LinkResponse("error", "La dirección de Telegram recibida no es válida.")
            return
        }
        val telegram = Uri.Builder().scheme("tg").authority("resolve")
            .appendQueryParameter("domain", uri.pathSegments.first())
            .appendQueryParameter("text", uri.getQueryParameter("text").orEmpty()).build()
        speaker.stop()
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, telegram).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: ActivityNotFoundException) {
                response = response?.copy(type = "error", reply = "Instala Telegram para abrir el mensaje preparado.")
            }
        }
    }

    NavHost(navController, startDestination = "home") {
        composable("home") {
            SeniorHomeScreen(
                onListen = { voice() },
                onCaregiverMode = { navController.navigate("caregiver") { launchSingleTop = true } },
                onWrite = { response = null; navController.navigate("assistant") { launchSingleTop = true } },
                onContacts = { status = ""; navController.navigate("contacts") { launchSingleTop = true } },
            )
        }
        composable("caregiver") {
            DemoDashboard(
                contactCount = contacts.size, onBack = { home() },
                onContacts = { status = ""; navController.navigate("contacts") { launchSingleTop = true } },
                onConnection = { connection() },
                onWrite = { response = null; navController.navigate("assistant") { launchSingleTop = true } },
            )
        }
        composable("connection") {
            ConnectionScreen(preferences, busy, status, onBack = { navController.popBackStack() }, onCheck = { loadContacts(checkGateway = true) })
        }
        composable("contacts") {
            ContactsEditorScreen(contacts, busy, status, onBack = { navController.popBackStack() },
                onConnection = { connection() }, onLoad = { loadContacts() }, onSave = { saveContact(it) })
        }
        composable("listen") {
            ListeningScreen(
                onBack = { navController.popBackStack() },
                onRecognizedText = { recognizedText = it },
                onContinue = { navController.popBackStack(); send(recognizedText) },
            )
        }
        composable("assistant") {
            AssistantScreen(input, response, busy, onText = { input = it }, onSend = { send(input) },
                onVoice = { voice() }, onConfirm = { send("sí") }, onCancel = { send("no") },
                onTelegram = { openTelegram() }, onBack = { home() }, onConnection = { connection() })
        }
    }
}
