package com.example.link.ui.screens.caregiver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.link.ui.components.ActivityRow
import com.example.link.ui.components.CaregiverBottomBar
import com.example.link.ui.components.LinkCard
import com.example.link.ui.components.LinkIcon
import com.example.link.ui.components.LinkIconType
import com.example.link.ui.components.LinkLogo
import com.example.link.ui.components.PrimaryButton
import com.example.link.ui.components.SecondaryButton
import com.example.link.ui.components.SectionDivider
import com.example.link.ui.components.SettingRow
import com.example.link.ui.theme.LinkBackground
import com.example.link.ui.theme.LinkBlue
import com.example.link.ui.theme.LinkBlueSoft
import com.example.link.ui.theme.LinkBorder
import com.example.link.ui.theme.LinkInk
import com.example.link.ui.theme.LinkMuted
import com.example.link.ui.theme.LinkSuccess
import com.example.link.ui.theme.LinkSuccessSoft

@Composable
fun DashboardScreen(
    onSettings: () -> Unit,
    onPause: () -> Unit,
    onSeniorMode: () -> Unit
) {
    CaregiverPage(
        current = "dashboard",
        onDashboard = {},
        onSettings = onSettings,
        onPause = onPause
    ) {
        Text("PANEL DEL CUIDADOR", color = LinkBlue, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(7.dp))
        Text("Todo está en calma", style = MaterialTheme.typography.headlineMedium)
        Text("Resumen de la actividad de Don Carlos", color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))

        LinkCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(LinkSuccessSoft, CircleShape), contentAlignment = Alignment.Center) {
                    LinkIcon(LinkIconType.Check, "Activo", Modifier.size(23.dp), LinkSuccess)
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Don Carlos", style = MaterialTheme.typography.titleLarge)
                    Text("Asistente activo", color = LinkSuccess, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("LINK está listo para ayudar cuando Don Carlos lo necesite.", color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("3", "consultas", "realizadas hoy", Modifier.weight(1f))
            MetricCard("0", "alertas", "por revisar", Modifier.weight(1f), success = true)
        }
        Spacer(Modifier.height(22.dp))
        Text("Actividad reciente", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        LinkCard {
            ActivityRow(LinkIconType.Phone, "Llamó a María", "Hoy · 10:42")
            SectionDivider()
            ActivityRow(LinkIconType.Calendar, "Consultó su calendario", "Hoy · 8:15")
            SectionDivider()
            ActivityRow(LinkIconType.Message, "Preparó mensaje para María", "Ayer · 17:30")
        }
        Spacer(Modifier.height(22.dp))
        Text("Accesos rápidos", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        SettingRow(LinkIconType.Person, "Contactos", "María, Dr. López y 2 más", onSettings)
        Spacer(Modifier.height(9.dp))
        SettingRow(LinkIconType.Apps, "Aplicaciones", "WhatsApp, llamadas y calendario", onSettings)
        Spacer(Modifier.height(9.dp))
        SettingRow(LinkIconType.Lock, "Permisos", "Confirmar mensajes y llamadas", onSettings)
        Spacer(Modifier.height(18.dp))
        PrimaryButton("Abrir modo adulto mayor", onSeniorMode, leadingIcon = LinkIconType.Microphone)
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Pausar asistente", onPause)
    }
}

@Composable
private fun MetricCard(
    number: String,
    label: String,
    detail: String,
    modifier: Modifier,
    success: Boolean = false
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(17.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LinkBorder)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(number, color = if (success) LinkSuccess else LinkBlue, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(label, fontWeight = FontWeight.Bold)
            Text(detail, color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SettingsScreen(
    onDashboard: () -> Unit,
    onPause: () -> Unit,
    onContacts: () -> Unit,
    onApps: () -> Unit,
    onPermissions: () -> Unit,
    onSeniorMode: () -> Unit
) {
    CaregiverPage(
        current = "settings",
        onDashboard = onDashboard,
        onSettings = {},
        onPause = onPause
    ) {
        Text("PANEL DEL CUIDADOR", color = LinkBlue, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(7.dp))
        Text("Configuración", style = MaterialTheme.typography.headlineMedium)
        Text("Ajustes para acompañar a Don Carlos", color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        Text("USO DIARIO", color = LinkMuted, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(10.dp))
        SettingRow(LinkIconType.Person, "Contactos importantes", "Personas de confianza", onContacts)
        Spacer(Modifier.height(10.dp))
        SettingRow(LinkIconType.Apps, "Aplicaciones", "Herramientas disponibles", onApps)
        Spacer(Modifier.height(10.dp))
        SettingRow(LinkIconType.Lock, "Permisos", "Control antes de actuar", onPermissions)
        Spacer(Modifier.height(24.dp))
        Text("EXPERIENCIA", color = LinkMuted, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(10.dp))
        LinkCard {
            Text("Modo adulto mayor", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Vista sencilla que Don Carlos usará a diario.", color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(14.dp))
            PrimaryButton("Abrir modo adulto mayor", onSeniorMode)
        }
        Spacer(Modifier.height(18.dp))
        Text("Los cambios se pueden hacer en cualquier momento.", color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun PauseAssistantScreen(
    onDashboard: () -> Unit,
    onSettings: () -> Unit
) {
    var paused by rememberSaveable { mutableStateOf(false) }
    var duration by rememberSaveable { mutableStateOf("30 minutos") }
    CaregiverPage(
        current = "pause",
        onDashboard = onDashboard,
        onSettings = onSettings,
        onPause = {}
    ) {
        Text("PANEL DEL CUIDADOR", color = LinkBlue, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(7.dp))
        Text(if (paused) "Asistente pausado" else "¿Pausar el asistente?", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(18.dp))
        LinkCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(92.dp).background(LinkBlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                    LinkIcon(
                        if (paused) LinkIconType.Check else LinkIconType.Pause,
                        if (paused) "Pausado" else "Pausa",
                        Modifier.size(44.dp),
                        if (paused) LinkSuccess else LinkBlue,
                        4.dp
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(if (paused) "LINK está en pausa" else "LINK está activo", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (paused) "Don Carlos no podrá usarlo hasta que lo reactives."
                    else "Mientras esté pausado, Don Carlos no podrá usarlo.",
                    color = LinkMuted,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(6.dp))
                Text("La configuración no se perderá.", color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(20.dp))
        if (!paused) {
            Text("¿Por cuánto tiempo?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            LinkCard {
                listOf("30 minutos", "1 hora", "Hasta que lo active").forEachIndexed { index, item ->
                    DurationRow(item, selected = duration == item) { duration = item }
                    if (index < 2) SectionDivider()
                }
            }
            Spacer(Modifier.height(18.dp))
            PrimaryButton("Sí, pausar", { paused = true }, leadingIcon = LinkIconType.Pause)
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Cancelar", onDashboard)
        } else {
            LinkCard {
                Text("Duración seleccionada", color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
                Text(duration, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(18.dp))
            PrimaryButton("Reactivar asistente", { paused = false })
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Volver al inicio", onDashboard)
        }
    }
}

@Composable
private fun DurationRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.RadioButton, onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(22.dp).border(2.dp, if (selected) LinkBlue else LinkBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Box(Modifier.size(11.dp).background(LinkBlue, CircleShape))
        }
        Spacer(Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun CaregiverPage(
    current: String,
    onDashboard: () -> Unit,
    onSettings: () -> Unit,
    onPause: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(LinkBackground).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        LinkLogo(Modifier.fillMaxWidth(), "Cuidador")
        Spacer(Modifier.height(19.dp))
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            content = content
        )
        Spacer(Modifier.height(12.dp))
        CaregiverBottomBar(current, onDashboard, onSettings, onPause)
    }
}
