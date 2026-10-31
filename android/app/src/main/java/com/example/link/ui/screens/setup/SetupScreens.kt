package com.example.link.ui.screens.setup

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.link.ui.components.AppToggleRow
import com.example.link.ui.components.ContactCard
import com.example.link.ui.components.InfoPanel
import com.example.link.ui.components.LinkCard
import com.example.link.ui.components.LinkIcon
import com.example.link.ui.components.LinkIconType
import com.example.link.ui.components.LinkLogo
import com.example.link.ui.components.LinkTopBar
import com.example.link.ui.components.PrimaryButton
import com.example.link.ui.components.SecondaryButton
import com.example.link.ui.components.SetupScaffold
import com.example.link.ui.theme.LinkBackground
import com.example.link.ui.theme.LinkBlue
import com.example.link.ui.theme.LinkBlueSoft
import com.example.link.ui.theme.LinkBorder
import com.example.link.ui.theme.LinkInk
import com.example.link.ui.theme.LinkMuted
import com.example.link.ui.theme.LinkSuccess
import com.example.link.ui.theme.LinkSuccessSoft

@Composable
fun AccountTypeScreen(onContinue: () -> Unit) {
    var selected by remember { mutableStateOf("familiar") }
    SetupScaffold(
        step = 1,
        title = "¿Para quién es esta cuenta?",
        subtitle = "Elige cómo quieres configurar LINK.",
        onContinue = onContinue
    ) {
        ChoiceCard(
            title = "Para un familiar",
            description = "Configuraré LINK para acompañar a un ser querido.",
            icon = LinkIconType.Person,
            selected = selected == "familiar",
            onClick = { selected = "familiar" }
        )
        Spacer(Modifier.height(12.dp))
        ChoiceCard(
            title = "Para mí",
            description = "Usaré LINK en este teléfono.",
            icon = LinkIconType.Logo,
            selected = selected == "propia",
            onClick = { selected = "propia" }
        )
        Spacer(Modifier.height(22.dp))
        InfoPanel("Pensado para acompañar", "El cuidador prepara el asistente; Don Carlos lo usa cada día con una interfaz sencilla.")
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    description: String,
    icon: LinkIconType,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.RadioButton, onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) LinkBlue else LinkBorder)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(LinkBlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                LinkIcon(icon, title, Modifier.size(24.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(3.dp))
                Text(description, color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Box(
                Modifier.size(22.dp).border(2.dp, if (selected) LinkBlue else LinkBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Box(Modifier.size(11.dp).background(LinkBlue, CircleShape))
            }
        }
    }
}

@Composable
fun SeniorDataScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    var name by remember { mutableStateOf("Carlos Hernández") }
    var preferredName by remember { mutableStateOf("Don Carlos") }
    SetupScaffold(
        step = 2,
        title = "Datos del adulto mayor",
        subtitle = "Estos datos ayudan a que LINK se dirija a él de forma cercana.",
        continueText = "Guardar y continuar",
        onContinue = onContinue,
        onBack = onBack
    ) {
        FormField("Nombre completo", name) { name = it }
        Spacer(Modifier.height(14.dp))
        FormField("¿Cómo quieres que lo llame el asistente?", preferredName) { preferredName = it }
        Spacer(Modifier.height(22.dp))
        InfoPanel("Un trato a su manera", "Podrás editar estos datos cuando quieras.")
    }
}

@Composable
private fun FormField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, color = LinkInk, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(15.dp)
        )
    }
}

@Composable
fun ContactsScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    var extraContact by remember { mutableStateOf(false) }
    SetupScaffold(
        step = 3,
        title = "Contactos importantes",
        subtitle = "LINK podrá ayudar a Don Carlos a llamar o escribir a estas personas.",
        onContinue = onContinue,
        onBack = onBack
    ) {
        ContactsContent(extraContact = extraContact, onAdd = { extraContact = true })
        Spacer(Modifier.height(22.dp))
        InfoPanel("Tú decides quién aparece aquí", "Los contactos se pueden editar después.")
    }
}

@Composable
private fun ColumnScope.ContactsContent(extraContact: Boolean, onAdd: () -> Unit) {
    ContactCard("María", "Hija", "WhatsApp / Llamadas", primary = true)
    Spacer(Modifier.height(11.dp))
    ContactCard("Dr. López", "Doctor", "Llamadas")
    if (extraContact) {
        Spacer(Modifier.height(11.dp))
        ContactCard("Ana", "Vecina", "Llamadas")
    }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onAdd, enabled = !extraContact, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        LinkIcon(LinkIconType.Plus, "Agregar", Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(if (extraContact) "Contacto agregado" else "+ Agregar contacto", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AppsScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    SetupScaffold(
        step = 4,
        title = "Aplicaciones",
        subtitle = "Elige qué herramientas podrá usar LINK para ayudar a Don Carlos.",
        onContinue = onContinue,
        onBack = onBack
    ) {
        AppsContent()
        Spacer(Modifier.height(22.dp))
        InfoPanel("Solo lo necesario", "Las opciones activas se pueden cambiar en cualquier momento.")
    }
}

@Composable
private fun ColumnScope.AppsContent() {
    var whatsapp by remember { mutableStateOf(true) }
    var calls by remember { mutableStateOf(true) }
    var calendar by remember { mutableStateOf(true) }
    var location by remember { mutableStateOf(false) }
    AppToggleRow(LinkIconType.Message, "WhatsApp", "Mensajes a sus contactos", whatsapp) { whatsapp = it }
    Spacer(Modifier.height(10.dp))
    AppToggleRow(LinkIconType.Phone, "Llamadas", "Llamar con una frase", calls) { calls = it }
    Spacer(Modifier.height(10.dp))
    AppToggleRow(LinkIconType.Calendar, "Calendario", "Consultar citas y recordatorios", calendar) { calendar = it }
    Spacer(Modifier.height(10.dp))
    AppToggleRow(LinkIconType.Location, "Ubicación", "Compartir solo con permiso", location) { location = it }
}

@Composable
fun PermissionsScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    SetupScaffold(
        step = 5,
        title = "Permisos del asistente",
        subtitle = "Reglas simples para que LINK ayude con claridad y siempre bajo control.",
        onContinue = onContinue,
        onBack = onBack
    ) {
        PermissionsContent()
        Spacer(Modifier.height(22.dp))
        InfoPanel("Control y privacidad", "Estas reglas se pueden revisar después desde Configuración.")
    }
}

@Composable
private fun ColumnScope.PermissionsContent() {
    PermissionCard(LinkIconType.Check, "Acciones seguras", "Automáticas", LinkSuccess, LinkSuccessSoft)
    Spacer(Modifier.height(10.dp))
    PermissionCard(LinkIconType.Message, "Mensajes y llamadas", "Pedir confirmación", LinkBlue, LinkBlueSoft)
    Spacer(Modifier.height(10.dp))
    PermissionCard(LinkIconType.Lock, "Acciones sensibles", "Bloqueadas", MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer)
}

@Composable
private fun PermissionCard(
    icon: LinkIconType,
    title: String,
    status: String,
    color: Color,
    tint: Color
) {
    LinkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(tint, CircleShape), contentAlignment = Alignment.Center) {
                LinkIcon(icon, title, Modifier.size(23.dp), color)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(status, color = color, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun CompleteScreen(onFinish: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(LinkBackground).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 14.dp)
    ) {
        LinkLogo(Modifier.fillMaxWidth(), "Cuidador")
        Spacer(Modifier.height(20.dp))
        Text("CONFIGURACIÓN · PASO 6 DE 6", color = LinkBlue, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinkCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(104.dp).background(LinkBlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(64.dp).background(LinkBlue, CircleShape), contentAlignment = Alignment.Center) {
                            LinkIcon(LinkIconType.Check, "Configuración completada", Modifier.size(35.dp), Color.White, 3.dp)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("¡Todo listo!", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(7.dp))
                    Text("Don Carlos ya puede usar el asistente.", color = LinkMuted, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(22.dp))
                    SummaryLine("4 contactos")
                    SummaryLine("WhatsApp y llamadas")
                    SummaryLine("Calendario preparado")
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Puedes ajustar todo después desde el panel del cuidador.", color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Atrás", onBack, Modifier.weight(.38f))
            PrimaryButton("Finalizar", onFinish, Modifier.weight(.62f))
        }
    }
}

@Composable
private fun SummaryLine(text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp).background(LinkSuccessSoft, CircleShape), contentAlignment = Alignment.Center) {
            LinkIcon(LinkIconType.Check, "Listo", Modifier.size(13.dp), LinkSuccess, 2.dp)
        }
        Spacer(Modifier.size(10.dp))
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ContactsSettingsScreen(onBack: () -> Unit) {
    var extraContact by remember { mutableStateOf(false) }
    SettingsDetailScaffold("Contactos importantes", "Personas con quienes Don Carlos puede comunicarse.", onBack) {
        ContactsContent(extraContact, onAdd = { extraContact = true })
    }
}

@Composable
fun AppsSettingsScreen(onBack: () -> Unit) {
    SettingsDetailScaffold("Aplicaciones", "Herramientas disponibles para el uso diario.", onBack) { AppsContent() }
}

@Composable
fun PermissionsSettingsScreen(onBack: () -> Unit) {
    SettingsDetailScaffold("Permisos", "Reglas de confirmación y seguridad.", onBack) { PermissionsContent() }
}

@Composable
private fun SettingsDetailScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(LinkBackground).statusBarsPadding().navigationBarsPadding().padding(22.dp)
    ) {
        LinkTopBar(onBack = onBack)
        Spacer(Modifier.height(22.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(18.dp))
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), content = content)
        Spacer(Modifier.height(14.dp))
        PrimaryButton("Guardar cambios", onBack)
    }
}
