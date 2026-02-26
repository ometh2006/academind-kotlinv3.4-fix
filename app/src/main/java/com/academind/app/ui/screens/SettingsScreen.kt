package com.academind.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.academind.app.ui.components.*
import com.academind.app.ui.theme.*
import com.academind.app.viewmodel.AppState
import com.academind.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, appState: AppState) {
    val prefs = appState.userPrefs
    var editName       by remember { mutableStateOf(prefs.userName) }
    var editExamDate   by remember { mutableStateOf(prefs.examDate) }
    var expandedLevel  by remember { mutableStateOf(false) }
    var showReset      by remember { mutableStateOf(false) }
    var showAvatarPick by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // Profile card
        AppCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(16.dp), Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(Primary.copy(.15f)).clickable { showAvatarPick = true },
                    Alignment.Center) {
                    Text(prefs.avatarEmoji, fontSize = 32.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(prefs.userName.ifBlank { "Student" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(MainViewModel.STUDY_LEVELS.find { it.first == prefs.studyLevel }?.second ?: prefs.studyLevel,
                        style = MaterialTheme.typography.bodySmall, color = Primary)
                    if (prefs.examDate.isNotBlank()) Text("Exam: ${prefs.examDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton({ showAvatarPick = true }) { Text("Edit avatar", fontSize = 11.sp) }
            }
        }

        // Avatar picker
        if (showAvatarPick) {
            AppCard(Modifier.fillMaxWidth()) {
                Text("Choose Avatar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                val rows = MainViewModel.AVATAR_EMOJIS.chunked(8)
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                        row.forEach { emoji ->
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (emoji == prefs.avatarEmoji) Primary.copy(.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(if (emoji == prefs.avatarEmoji) 2.dp else 0.dp, Primary, RoundedCornerShape(12.dp))
                                .clickable { viewModel.updateAvatar(emoji); showAvatarPick = false },
                                Alignment.Center) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // Name
        SettingsCard("👤 Profile Name") {
            OutlinedTextField(editName, { editName = it }, Modifier.fillMaxWidth(),
                label = { Text("Your name") }, shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { viewModel.updateUserName(editName) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text("Save Name") }
        }

        // Exam date
        SettingsCard("📅 Exam Date") {
            OutlinedTextField(editExamDate, { editExamDate = it }, Modifier.fillMaxWidth(),
                label = { Text("YYYY-MM-DD") }, shape = RoundedCornerShape(12.dp), singleLine = true,
                placeholder = { Text("e.g. 2025-06-15") })
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { viewModel.updateExamDate(editExamDate) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) { Text("Save Date", color = Color.White) }
        }

        // Study level
        SettingsCard("🎓 Study Level") {
            ExposedDropdownMenuBox(expandedLevel, { expandedLevel = it }) {
                OutlinedTextField(
                    value = MainViewModel.STUDY_LEVELS.find { it.first == prefs.studyLevel }?.second ?: prefs.studyLevel,
                    onValueChange = {}, readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    label = { Text("Level") }, shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedLevel) }
                )
                ExposedDropdownMenu(expandedLevel, { expandedLevel = false }) {
                    MainViewModel.STUDY_LEVELS.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { viewModel.updateStudyLevel(key); expandedLevel = false },
                            leadingIcon = if (prefs.studyLevel == key) {{ Icon(Icons.Default.Check, null, tint = Primary) }} else null
                        )
                    }
                }
            }
        }

        // Theme
        SettingsCard("🎨 Appearance") {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("Dark Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(if (prefs.isDarkTheme) "Dark mode active" else "Light mode active",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(prefs.isDarkTheme, { viewModel.toggleTheme(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary))
            }
        }

        // Stats summary
        SettingsCard("📊 Data Summary") {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                DataStat("${appState.subjects.size}", "Subjects")
                DataStat("${appState.allTests.size}", "Tests")
                DataStat(if (appState.allTests.isEmpty()) "—" else "${"%.1f".format(appState.allTests.map { it.percentage }.average().toFloat())}%", "Average")
            }
        }

        // Reset
        SettingsCard("⚠️ Danger Zone") {
            Text("This will permanently delete all your subjects, test results, and preferences. This action cannot be undone.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { showReset = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Danger)
            ) {
                Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reset All Data")
            }
        }

        // Social Links
        val context = LocalContext.current
        SettingsCard("🔗 Connect with Developer") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/OMETH_PM_BOT")))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC))
                ) {
                    Text("✈ Telegram", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Button(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ometh2006")))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24292E))
                ) {
                    Text("🐱 GitHub", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }

        // Developer Credit
        AppCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🎓", fontSize = 32.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Developed By Ometh Virusara",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Text(
                    "© 2026 • AcadeMind v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Built with Jetpack Compose • Fully Offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(80.dp))
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 22.sp)
                    Text("Reset All Data?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("This will permanently delete:")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "📚 All subjects",
                            "📝 All test results",
                            "⚙️ All settings & preferences"
                        ).forEach { item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(item, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Text(
                        "This action cannot be undone.",
                        fontWeight = FontWeight.Bold,
                        color = Danger,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetAllData(); showReset = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Yes, Delete Everything")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showReset = false },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun DataStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
