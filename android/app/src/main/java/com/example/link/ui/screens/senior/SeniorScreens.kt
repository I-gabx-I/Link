package com.example.link.ui.screens.senior

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.link.speech.SpeechRecognitionError
import com.example.link.speech.SpeechRecognitionState
import com.example.link.speech.SpeechRecognizerManager
import com.example.link.ui.components.LinkCard
import com.example.link.ui.components.LinkIcon
import com.example.link.ui.components.LinkIconType
import com.example.link.ui.components.LinkLogo
import com.example.link.ui.components.LinkTopBar
import com.example.link.ui.components.PrimaryButton
import com.example.link.ui.components.SecondaryButton
import com.example.link.ui.theme.LinkBackground
import com.example.link.ui.theme.LinkBlue
import com.example.link.ui.theme.LinkBlueSoft
import com.example.link.ui.theme.LinkBorder
import com.example.link.ui.theme.LinkInk
import com.example.link.ui.theme.LinkMuted
import com.example.link.ui.theme.LinkSuccess
import com.example.link.ui.theme.LinkSuccessSoft
import com.example.link.ui.theme.LinkWave

@Composable
fun SeniorHomeScreen(
    onListen: () -> Unit,
    onCaregiverMode: () -> Unit,
    onWrite: () -> Unit = {},
    onContacts: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxSize().background(LinkBackground).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LinkLogo(Modifier.weight(1f))
            TextButton(onClick = onCaregiverMode) {
                Text("Cuidador", color = LinkMuted, fontWeight = FontWeight.SemiBold)
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))
            Text(
                "Buenas tardes,",
                modifier = Modifier.fillMaxWidth(),
                color = LinkInk,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp
            )
            Text(
                "Don Carlos",
                modifier = Modifier.fillMaxWidth(),
                color = LinkInk,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 43.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "¿En qué puedo ayudarte?",
                modifier = Modifier.fillMaxWidth(),
                color = LinkMuted,
                fontSize = 20.sp,
                lineHeight = 28.sp
            )
            Spacer(Modifier.height(34.dp))
            LargeMicrophoneButton(onClick = onListen)
            Spacer(Modifier.height(14.dp))
            Text("HABLAR", color = LinkBlue, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text("Toca el micrófono", color = LinkMuted, fontSize = 17.sp)
            Spacer(Modifier.height(32.dp))
            Text(
                "Puedes decir:",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(10.dp))
            ExamplePhrase(LinkIconType.Message, "Avísale a María que voy saliendo")
            Spacer(Modifier.height(9.dp))
            ExamplePhrase(LinkIconType.Message, "Quiero escribirle a mi mamá")
            Spacer(Modifier.height(16.dp))
            SecondaryButton("Escribir una solicitud", onWrite)
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Mis contactos", onContacts)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun LargeMicrophoneButton(onClick: () -> Unit) {
    Box(
        Modifier.size(184.dp).background(LinkBlueSoft.copy(alpha = .72f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(146.dp)
                .semantics { contentDescription = "Hablar con Guardian" }
                .clickable(role = Role.Button, onClick = onClick),
            shape = CircleShape,
            color = LinkBlue,
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                LinkIcon(
                    LinkIconType.Microphone,
                    contentDescription = "Micrófono",
                    modifier = Modifier.size(70.dp),
                    color = Color.White,
                    strokeWidth = 4.dp
                )
            }
        }
    }
}

@Composable
private fun ExamplePhrase(icon: LinkIconType, phrase: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LinkBorder)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(LinkBlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                LinkIcon(icon, phrase, Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(phrase, color = LinkInk, fontSize = 18.sp)
        }
    }
}

@Composable
fun ListeningScreen(
    onBack: () -> Unit,
    onRecognizedText: (String) -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember(context) { SpeechRecognizerManager(context.applicationContext) }
    val speechState = manager.state

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) manager.startListening() else manager.showPermissionDenied()
    }

    DisposableEffect(manager) {
        onDispose { manager.destroy() }
    }

    LaunchedEffect(manager) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            manager.startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(speechState) {
        if (speechState is SpeechRecognitionState.Result) {
            onRecognizedText(speechState.text)
        }
    }

    val isListening = speechState is SpeechRecognitionState.Listening
    val title = when (speechState) {
        SpeechRecognitionState.Idle -> "Listo para escucharte"
        is SpeechRecognitionState.Listening -> "Te estoy escuchando..."
        is SpeechRecognitionState.Processing -> "Un momento..."
        is SpeechRecognitionState.Result -> "Te escuché"
        is SpeechRecognitionState.Error -> "Intentémoslo de nuevo"
    }
    val subtitle = when (speechState) {
        SpeechRecognitionState.Idle -> "Toca el botón para comenzar"
        is SpeechRecognitionState.Listening -> "Habla con calma y claridad"
        is SpeechRecognitionState.Processing -> "Estoy preparando lo que dijiste"
        is SpeechRecognitionState.Result -> "Esto es lo que escuché"
        is SpeechRecognitionState.Error -> speechState.message
    }
    val capturedText = when (speechState) {
        is SpeechRecognitionState.Listening -> speechState.partialText
        is SpeechRecognitionState.Processing -> speechState.partialText
        is SpeechRecognitionState.Result -> speechState.text
        else -> ""
    }

    val retry = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            manager.startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    SeniorPage(onBack = {
        manager.cancelListening()
        onBack()
    }) {
        Spacer(Modifier.height(26.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = LinkMuted, fontSize = 18.sp, lineHeight = 26.sp)
        Spacer(Modifier.height(30.dp))
        ListeningIndicator(speechState)
        Spacer(Modifier.height(28.dp))

        if (speechState !is SpeechRecognitionState.Error) {
            LinkCard {
                Text(
                    when (speechState) {
                        is SpeechRecognitionState.Result -> "LO QUE ESCUCHÉ"
                        is SpeechRecognitionState.Processing -> "PROCESANDO"
                        else -> "LO QUE ESCUCHO"
                    },
                    color = if (speechState is SpeechRecognitionState.Result) LinkSuccess else LinkMuted,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    capturedText.ifBlank { "Tus palabras aparecerán aquí." },
                    color = if (capturedText.isBlank()) LinkMuted else LinkInk,
                    fontSize = 21.sp,
                    lineHeight = 29.sp,
                    fontWeight = if (capturedText.isBlank()) FontWeight.Normal else FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        when (speechState) {
            SpeechRecognitionState.Idle -> PrimaryButton("Empezar a escuchar", retry, Modifier.height(64.dp))
            is SpeechRecognitionState.Result -> {
                PrimaryButton("Continuar", onContinue, Modifier.height(64.dp))
                Spacer(Modifier.height(10.dp))
            }
            is SpeechRecognitionState.Error -> {
                if (speechState.reason != SpeechRecognitionError.Unavailable) {
                    PrimaryButton("Intentar nuevamente", retry, Modifier.height(64.dp))
                    Spacer(Modifier.height(10.dp))
                }
            }
            else -> Unit
        }
        SecondaryButton(
            text = if (isListening) "Cancelar escucha" else "Volver",
            onClick = {
                manager.cancelListening()
                onBack()
            },
            modifier = Modifier.height(60.dp)
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ListeningIndicator(state: SpeechRecognitionState) {
    val transition = rememberInfiniteTransition(label = "listening pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "microphone scale"
    )
    val isListening = state is SpeechRecognitionState.Listening
    val isResult = state is SpeechRecognitionState.Result
    val isError = state is SpeechRecognitionState.Error

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(160.dp)
                .graphicsLayer {
                    scaleX = if (isListening) pulse else 1f
                    scaleY = if (isListening) pulse else 1f
                }
                .background(
                    when {
                        isResult -> LinkSuccessSoft
                        isError -> MaterialTheme.colorScheme.errorContainer
                        else -> LinkBlueSoft
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(122.dp).background(
                    when {
                        isResult -> LinkSuccess
                        isError -> MaterialTheme.colorScheme.error
                        else -> LinkBlue
                    },
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                LinkIcon(
                    type = if (isResult) LinkIconType.Check else LinkIconType.Microphone,
                    contentDescription = when {
                        isResult -> "Resultado entendido"
                        isError -> "Error de reconocimiento"
                        else -> "Escuchando"
                    },
                    modifier = Modifier.size(58.dp),
                    color = Color.White,
                    strokeWidth = 4.dp
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Waveform(active = isListening)
    }
}

@Composable
private fun Waveform(active: Boolean) {
    val bars = if (active) listOf(.30f, .56f, .82f, .48f, 1f, .65f, .88f, .52f, .76f, .42f, .24f)
    else List(11) { .18f }
    Canvas(Modifier.width(132.dp).height(50.dp)) {
        val gap = size.width / (bars.size + 1)
        bars.forEachIndexed { index, value ->
            val barHeight = size.height * value
            drawLine(
                color = if (active) LinkWave else LinkBorder,
                start = Offset(gap * (index + 1), (size.height - barHeight) / 2),
                end = Offset(gap * (index + 1), (size.height + barHeight) / 2),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun ConfirmationScreen(
    recognizedText: String,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit
) {
    var confirmed by remember { mutableStateOf(false) }
    SeniorPage(onBack = onBack) {
        Spacer(Modifier.height(26.dp))
        if (!confirmed) {
            Row(
                Modifier.background(LinkSuccessSoft, CircleShape).padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinkIcon(LinkIconType.Check, "Solicitud entendida", Modifier.size(17.dp), LinkSuccess)
                Spacer(Modifier.width(7.dp))
                Text("Solicitud entendida", color = LinkSuccess, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(22.dp))
            Text("¿Quieres continuar?", fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text("Revisa lo que escuché antes de continuar.", color = LinkMuted, fontSize = 18.sp)
            Spacer(Modifier.height(24.dp))
            LinkCard {
                Text("LO QUE ESCUCHÉ", color = LinkMuted, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(9.dp))
                Text(
                    recognizedText.ifBlank { "No hay texto reconocido." },
                    color = LinkInk,
                    fontSize = 23.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(22.dp))
            Surface(color = LinkBlueSoft, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    LinkIcon(LinkIconType.Lock, "Protección", Modifier.size(25.dp))
                    Spacer(Modifier.width(11.dp))
                    Text(
                        "Guardian siempre pedirá confirmación antes de realizar una acción importante.",
                        color = LinkMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Continuar", { confirmed = true }, Modifier.height(66.dp))
            Spacer(Modifier.height(11.dp))
            SecondaryButton("Cancelar", onCancel, Modifier.height(62.dp))
        } else {
            Spacer(Modifier.height(40.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(132.dp).background(LinkSuccessSoft, CircleShape), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(88.dp).background(LinkSuccess, CircleShape), contentAlignment = Alignment.Center) {
                        LinkIcon(LinkIconType.Check, "Solicitud guardada", Modifier.size(48.dp), Color.White, 4.dp)
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text("Solicitud guardada", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Esta demostración conserva el texto, pero todavía no ejecuta acciones.",
                    color = LinkMuted,
                    fontSize = 19.sp,
                    lineHeight = 27.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(44.dp))
            PrimaryButton("Volver al inicio", onDone, Modifier.height(66.dp))
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SeniorPage(
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(LinkBackground).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        LinkTopBar(onBack = onBack)
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            content = content
        )
    }
}
