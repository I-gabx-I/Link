package com.example.link.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.link.ui.theme.LinkBackground
import com.example.link.ui.theme.LinkBlue
import com.example.link.ui.theme.LinkBlueSoft
import com.example.link.ui.theme.LinkBorder
import com.example.link.ui.theme.LinkInk
import com.example.link.ui.theme.LinkMuted
import com.example.link.ui.theme.LinkSuccess
import com.example.link.ui.theme.LinkSuccessSoft

enum class LinkIconType {
    Logo, Microphone, Check, Pause, Phone, Calendar, Message, Person,
    Back, Chevron, Home, Settings, Apps, Lock, Location, Plus
}

@Composable
fun LinkIcon(
    type: LinkIconType,
    contentDescription: String,
    modifier: Modifier = Modifier,
    color: Color = LinkBlue,
    strokeWidth: Dp = 2.4.dp
) {
    Canvas(
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        }
    ) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        when (type) {
            LinkIconType.Logo -> {
                drawArc(color, 28f, 230f, false, Offset(w * .20f, h * .20f), Size(w * .60f, h * .60f), style = stroke)
                drawCircle(color, w * .07f, Offset(w * .68f, h * .25f))
                drawCircle(color, w * .07f, Offset(w * .31f, h * .75f))
            }
            LinkIconType.Microphone -> {
                drawRoundRect(color, Offset(w * .34f, h * .12f), Size(w * .32f, h * .50f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .17f), style = stroke)
                drawArc(color, 0f, 180f, false, Offset(w * .22f, h * .29f), Size(w * .56f, h * .50f), style = stroke)
                drawLine(color, Offset(w * .5f, h * .79f), Offset(w * .5f, h * .91f), sw, StrokeCap.Round)
                drawLine(color, Offset(w * .34f, h * .91f), Offset(w * .66f, h * .91f), sw, StrokeCap.Round)
            }
            LinkIconType.Check -> {
                drawLine(color, Offset(w * .16f, h * .52f), Offset(w * .42f, h * .76f), sw, StrokeCap.Round)
                drawLine(color, Offset(w * .42f, h * .76f), Offset(w * .86f, h * .25f), sw, StrokeCap.Round)
            }
            LinkIconType.Pause -> {
                drawLine(color, Offset(w * .36f, h * .20f), Offset(w * .36f, h * .80f), sw * 1.5f, StrokeCap.Round)
                drawLine(color, Offset(w * .64f, h * .20f), Offset(w * .64f, h * .80f), sw * 1.5f, StrokeCap.Round)
            }
            LinkIconType.Phone -> {
                val p = Path().apply {
                    moveTo(w * .26f, h * .16f)
                    cubicTo(w * .14f, h * .25f, w * .26f, h * .58f, w * .45f, h * .76f)
                    cubicTo(w * .63f, h * .92f, w * .82f, h * .84f, w * .85f, h * .72f)
                    lineTo(w * .66f, h * .57f)
                    lineTo(w * .54f, h * .68f)
                    cubicTo(w * .45f, h * .63f, w * .36f, h * .53f, w * .31f, h * .43f)
                    lineTo(w * .42f, h * .32f)
                    close()
                }
                drawPath(p, color, style = stroke)
            }
            LinkIconType.Calendar -> {
                drawRoundRect(color, Offset(w * .16f, h * .22f), Size(w * .68f, h * .62f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .08f), style = stroke)
                drawLine(color, Offset(w * .17f, h * .42f), Offset(w * .83f, h * .42f), sw)
                drawLine(color, Offset(w * .34f, h * .13f), Offset(w * .34f, h * .30f), sw, StrokeCap.Round)
                drawLine(color, Offset(w * .66f, h * .13f), Offset(w * .66f, h * .30f), sw, StrokeCap.Round)
            }
            LinkIconType.Message -> {
                drawRoundRect(color, Offset(w * .13f, h * .18f), Size(w * .74f, h * .55f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .14f), style = stroke)
                drawLine(color, Offset(w * .30f, h * .72f), Offset(w * .23f, h * .88f), sw, StrokeCap.Round)
                drawLine(color, Offset(w * .23f, h * .88f), Offset(w * .48f, h * .73f), sw, StrokeCap.Round)
            }
            LinkIconType.Person -> {
                drawCircle(color, w * .17f, Offset(w * .5f, h * .31f), style = stroke)
                drawArc(color, 198f, 144f, false, Offset(w * .18f, h * .50f), Size(w * .64f, h * .54f), style = stroke)
            }
            LinkIconType.Back -> {
                drawLine(color, Offset(w * .72f, h * .18f), Offset(w * .28f, h * .50f), sw, StrokeCap.Round)
                drawLine(color, Offset(w * .28f, h * .50f), Offset(w * .72f, h * .82f), sw, StrokeCap.Round)
            }
            LinkIconType.Chevron -> {
                drawLine(color, Offset(w * .34f, h * .18f), Offset(w * .68f, h * .50f), sw, StrokeCap.Round)
                drawLine(color, Offset(w * .68f, h * .50f), Offset(w * .34f, h * .82f), sw, StrokeCap.Round)
            }
            LinkIconType.Home -> {
                val p = Path().apply {
                    moveTo(w * .13f, h * .48f); lineTo(w * .5f, h * .16f); lineTo(w * .87f, h * .48f)
                    moveTo(w * .22f, h * .42f); lineTo(w * .22f, h * .86f); lineTo(w * .78f, h * .86f); lineTo(w * .78f, h * .42f)
                }
                drawPath(p, color, style = stroke)
            }
            LinkIconType.Settings -> {
                drawCircle(color, w * .20f, Offset(w * .5f, h * .5f), style = stroke)
                drawLine(color, Offset(w * .5f, h * .08f), Offset(w * .5f, h * .25f), sw, StrokeCap.Round)
                drawLine(color, Offset(w * .5f, h * .75f), Offset(w * .5f, h * .92f), sw, StrokeCap.Round)
                drawLine(color, Offset(w * .08f, h * .5f), Offset(w * .25f, h * .5f), sw, StrokeCap.Round)
                drawLine(color, Offset(w * .75f, h * .5f), Offset(w * .92f, h * .5f), sw, StrokeCap.Round)
            }
            LinkIconType.Apps -> {
                listOf(.18f to .18f, .58f to .18f, .18f to .58f, .58f to .58f).forEach { (x, y) ->
                    drawRoundRect(color, Offset(w * x, h * y), Size(w * .24f, h * .24f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .04f), style = stroke)
                }
            }
            LinkIconType.Lock -> {
                drawRoundRect(color, Offset(w * .20f, h * .42f), Size(w * .60f, h * .43f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .08f), style = stroke)
                drawArc(color, 180f, -180f, false, Offset(w * .32f, h * .12f), Size(w * .36f, h * .55f), style = stroke)
            }
            LinkIconType.Location -> {
                drawCircle(color, w * .09f, Offset(w * .5f, h * .38f), style = stroke)
                drawArc(color, 198f, 144f, false, Offset(w * .20f, h * .09f), Size(w * .60f, h * .76f), style = stroke)
            }
            LinkIconType.Plus -> {
                drawLine(color, Offset(w * .5f, h * .20f), Offset(w * .5f, h * .80f), sw, StrokeCap.Round)
                drawLine(color, Offset(w * .20f, h * .5f), Offset(w * .80f, h * .5f), sw, StrokeCap.Round)
            }
        }
    }
}

@Composable
fun LinkLogo(modifier: Modifier = Modifier, roleLabel: String? = null) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(34.dp).background(LinkBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            LinkIcon(LinkIconType.Logo, "Logo de LINK", Modifier.size(22.dp), Color.White, 2.dp)
        }
        Spacer(Modifier.width(9.dp))
        Text("LINK", color = LinkInk, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        if (roleLabel != null) {
            Spacer(Modifier.weight(1f))
            Text(roleLabel, color = LinkMuted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun LinkTopBar(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    roleLabel: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (onBack == null) {
            LinkLogo(Modifier.fillMaxWidth(), roleLabel)
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onBack),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, LinkBorder)
            ) {
                Row(
                    Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinkIcon(LinkIconType.Back, "Volver", Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Volver", color = LinkBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: LinkIconType? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(58.dp),
        enabled = enabled,
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LinkBlue, disabledContainerColor = LinkBorder)
    ) {
        if (leadingIcon != null) {
            LinkIcon(leadingIcon, "", Modifier.size(22.dp), Color.White)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(15.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LinkBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (danger) MaterialTheme.colorScheme.error else LinkBlue)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SetupScaffold(
    step: Int,
    title: String,
    subtitle: String,
    continueText: String = "Continuar",
    onContinue: () -> Unit,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(LinkBackground).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 14.dp)
    ) {
        LinkTopBar(roleLabel = "Cuidador")
        Spacer(Modifier.height(20.dp))
        Text("CONFIGURACIÓN · PASO $step DE 6", color = LinkBlue, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(14.dp))
        SetupProgress(step)
        Spacer(Modifier.height(18.dp))
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            content = content
        )
        Spacer(Modifier.height(14.dp))
        if (onBack == null) {
            PrimaryButton(continueText, onContinue)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Atrás", onBack, Modifier.weight(.38f))
                PrimaryButton(continueText, onContinue, Modifier.weight(.62f))
            }
        }
    }
}

@Composable
fun SetupProgress(step: Int) {
    Box(Modifier.fillMaxWidth().height(4.dp).background(LinkBorder, CircleShape)) {
        Box(Modifier.fillMaxWidth(step / 6f).height(4.dp).background(LinkBlue, CircleShape))
    }
}

@Composable
fun LinkCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, LinkBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun InfoPanel(title: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().background(LinkBlueSoft, RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Text(title, color = LinkBlue, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Text(text, color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ContactCard(name: String, relation: String, detail: String, primary: Boolean = false) {
    LinkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(LinkBlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                Text(name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""), color = LinkBlue, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text("$relation · $detail", color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
            }
            if (primary) {
                Text(
                    "Principal",
                    Modifier.background(LinkSuccessSoft, CircleShape).padding(horizontal = 10.dp, vertical = 5.dp),
                    color = LinkSuccess,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun AppToggleRow(
    icon: LinkIconType,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    LinkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(LinkBlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                LinkIcon(icon, title, Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = LinkBlue,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = LinkBorder,
                    uncheckedBorderColor = LinkBorder
                )
            )
        }
    }
}

@Composable
fun SettingRow(
    icon: LinkIconType,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(17.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LinkBorder)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(LinkBlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                LinkIcon(icon, title, Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) Text(subtitle, color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
            }
            LinkIcon(LinkIconType.Chevron, "Abrir", Modifier.size(20.dp), LinkMuted)
        }
    }
}

@Composable
fun ActivityRow(icon: LinkIconType, title: String, time: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).background(LinkBlueSoft, CircleShape), contentAlignment = Alignment.Center) {
            LinkIcon(icon, title, Modifier.size(20.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, color = LinkInk)
            Text(time, color = LinkMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun CaregiverBottomBar(
    current: String,
    onDashboard: () -> Unit,
    onSettings: () -> Unit,
    onPause: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LinkBorder)
    ) {
        Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.SpaceAround) {
            BottomDestination("Inicio", LinkIconType.Home, current == "dashboard", Modifier.weight(1f), onDashboard)
            BottomDestination("Ajustes", LinkIconType.Settings, current == "settings", Modifier.weight(1f), onSettings)
            BottomDestination("Pausar", LinkIconType.Pause, current == "pause", Modifier.weight(1f), onPause)
        }
    }
}

@Composable
private fun BottomDestination(
    label: String,
    icon: LinkIconType,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(role = Role.Tab, onClick = onClick)
            .background(if (selected) LinkBlueSoft else Color.Transparent, RoundedCornerShape(13.dp)).padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinkIcon(icon, label, Modifier.size(23.dp), if (selected) LinkBlue else LinkMuted)
        Text(label, color = if (selected) LinkBlue else LinkMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
fun SectionDivider() {
    HorizontalDivider(color = LinkBorder)
}
