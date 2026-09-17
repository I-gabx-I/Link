package com.example.link.ui.screens.senior

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay

@Composable
fun SeniorHomeScreen(
    onListen: () -> Unit,
    onCaregiverMode: () -> Unit
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
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(30.dp))
            Text("Buenos días,", color = LinkInk, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 35.sp)
            Text("Don Carlos", color = LinkInk, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 42.sp)
            Spacer(Modifier.height(10.dp))
            Text("¿En qué puedo ayudarte?", color = LinkMuted, fontSize = 20.sp, lineHeight = 27.sp)
            Spacer(Modifier.height(34.dp))

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                LargeMicrophoneButton(onClick = onListen)
                Spacer(Modifier.height(12.dp))
                Text("Toca para hablar", color = LinkMuted, fontSize = 17.sp)
            }
            Spacer(Modifier.height(28.dp))
            PrimaryButton("HABLAR", onListen, Modifier.height(66.dp), leadingIcon = LinkIconType.Microphone)
            Spacer(Modifier.height(26.dp))
            Text("Puedes decir:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            ExamplePhrase(LinkIconType.Phone, "Llama a María")
            Spacer(Modifier.height(9.dp))
            ExamplePhrase(LinkIconType.Calendar, "¿Qué tengo hoy?")
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun LargeMicrophoneButton(onClick: () -> Unit) {
    Box(
        Modifier.size(176.dp).background(LinkBlueSoft.copy(alpha = .72f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(148.dp).background(Color(0xFFD8E5FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(124.dp).clickable(role = Role.Button, onClick = onClick),
                shape = CircleShape,
                color = LinkBlue,
                shadowElevation = 7.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    LinkIcon(LinkIconType.Microphone, "Hablar con LINK", Modifier.size(62.dp), Color.White, 4.dp)
                }
            }
        }
    }
}

@Composable
private fun ExamplePhrase(icon: LinkIconType, phrase: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(15.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LinkBorder)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(LinkBlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                LinkIcon(icon, phrase, Modifier.size(17.dp))
            }
            Spacer(Modifier.width(11.dp))
            Text(phrase, color = LinkInk, fontSize = 17.sp)
        }
    }
}

private enum class ListeningPhase { Listening, Processing, Understood }

@Composable
fun ListeningScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    var phase by remember { mutableStateOf(ListeningPhase.Listening) }
    LaunchedEffect(Unit) {
        delay(1600)
        phase = ListeningPhase.Processing
        delay(1000)
        phase = ListeningPhase.Understood
    }

    SeniorPage(onBack = onBack) {
        Spacer(Modifier.height(26.dp))
        Text(
            when (phase) {
                ListeningPhase.Listening -> "Te estoy escuchando..."
                ListeningPhase.Processing -> "Un momento..."
                ListeningPhase.Understood -> "Entendido"
            },
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(Modifier.height(7.dp))
        Text(
            when (phase) {
                ListeningPhase.Listening -> "Puedes seguir hablando"
                ListeningPhase.Processing -> "Estoy preparando tu solicitud"
                ListeningPhase.Understood -> "Esto es lo que quieres hacer"
            },
            color = LinkMuted,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(30.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(154.dp).background(LinkBlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(122.dp).background(if (phase == ListeningPhase.Understood) LinkSuccess else LinkBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    LinkIcon(
                        if (phase == ListeningPhase.Understood) LinkIconType.Check else LinkIconType.Microphone,
                        if (phase == ListeningPhase.Understood) "Entendido" else "Escuchando",
                        Modifier.size(58.dp),
                        Color.White,
                        4.dp
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Waveform(active = phase == ListeningPhase.Listening)
        }
        Spacer(Modifier.height(28.dp))
        LinkCard {
            Text(
                if (phase == ListeningPhase.Understood) "ENTENDÍ" else "LO QUE ESCUCHÉ",
                color = if (phase == ListeningPhase.Understood) LinkSuccess else LinkMuted,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(8.dp))
            Text("Llama a mi hija María", fontSize = 22.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold)
            if (phase == ListeningPhase.Processing) {
                Spacer(Modifier.height(8.dp))
                Text("Procesando…", color = LinkBlue, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(20.dp))
        if (phase == ListeningPhase.Understood) {
            Text("Quieres llamar a María.", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Continuar", onContinue)
            Spacer(Modifier.height(10.dp))
        }
        SecondaryButton("Cancelar", onBack)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Waveform(active: Boolean) {
    val bars = if (active) listOf(.30f, .56f, .82f, .48f, 1f, .65f, .88f, .52f, .76f, .42f, .24f)
    else listOf(.18f, .18f, .18f, .18f, .18f, .18f, .18f, .18f, .18f, .18f, .18f)
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
                LinkIcon(LinkIconType.Check, "Confirmación requerida", Modifier.size(17.dp), LinkSuccess)
                Spacer(Modifier.width(7.dp))
                Text("Antes de llamar", color = LinkSuccess, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(22.dp))
            Text("¿Quieres llamar\na María?", fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text("Confirma para preparar la llamada.", color = LinkMuted, fontSize = 18.sp)
            Spacer(Modifier.height(24.dp))
            LinkCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(68.dp).background(LinkBlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                        Text("M", color = LinkBlue, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("María", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text("Tu hija", color = LinkMuted, fontSize = 17.sp)
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Surface(color = LinkBlueSoft, shape = RoundedCornerShape(15.dp)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    LinkIcon(LinkIconType.Lock, "Protección", Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("LINK siempre te pedirá permiso antes de llamar o enviar mensajes.", color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Sí, llamar", { confirmed = true }, Modifier.height(66.dp), leadingIcon = LinkIconType.Phone)
            Spacer(Modifier.height(11.dp))
            SecondaryButton("No, cancelar", onCancel, Modifier.height(62.dp))
        } else {
            Spacer(Modifier.height(40.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(132.dp).background(LinkSuccessSoft, CircleShape), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(88.dp).background(LinkSuccess, CircleShape), contentAlignment = Alignment.Center) {
                        LinkIcon(LinkIconType.Check, "Acción preparada", Modifier.size(48.dp), Color.White, 4.dp)
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text("Acción preparada", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text("La llamada a María está lista.", color = LinkMuted, fontSize = 20.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Esta es una simulación para la demostración.", color = LinkMuted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
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
