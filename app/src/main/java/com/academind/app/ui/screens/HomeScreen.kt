package com.academind.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.academind.app.data.TestResult
import com.academind.app.data.SubjectWithTests
import com.academind.app.ui.components.*
import com.academind.app.ui.theme.*
import com.academind.app.viewmodel.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(appState: AppState, stats: DashboardStats, countdown: ExamCountdown) {
    val hour      = remember { java.time.LocalTime.now().hour }
    val greeting  = when { hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" }
    val firstName = appState.userPrefs.userName.split(" ").firstOrNull() ?: "Student"
    val today     = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))
    val quote     = remember { MainViewModel.QUOTES.random() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Welcome
        AppCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("$greeting, $firstName 👋", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("\"$quote\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
                    Spacer(Modifier.height(8.dp))
                    Text(today, style = MaterialTheme.typography.labelSmall, color = Primary)
                }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(52.dp).background(Primary.copy(.14f), RoundedCornerShape(16.dp)), Alignment.Center) {
                    Text(appState.userPrefs.avatarEmoji, fontSize = 28.sp)
                }
            }
        }

        // Countdown
        CountdownBox(countdown.days, countdown.hours, countdown.minutes, countdown.seconds, countdown.isSet, countdown.isPast)

        // KPIs
        Text("📊 Overview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCard("Subjects", "${stats.totalSubjects}", "enrolled",  Primary, Modifier.weight(1f), "📚")
            KpiCard("Tests",    "${stats.totalTests}",    "recorded",  Accent,  Modifier.weight(1f), "📝")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val avgColor = Color(MainViewModel.getGradeColor(stats.overallAverage))
            KpiCard("Average",   if (stats.totalTests > 0) "${"%.1f".format(stats.overallAverage)}%" else "—", "Grade ${stats.overallGrade}", avgColor, Modifier.weight(1f), "🎯")
            KpiCard("Pass Rate", if (stats.totalTests > 0) "${"%.0f".format(stats.passRate)}%" else "—",
                "${appState.allTests.count { it.isPassing }}/${stats.totalTests} passed", Success, Modifier.weight(1f), "✅")
        }

        // All Subjects Performance — normalized line chart (defined after activeSwt)
        // Subject performance
        val activeSwt = appState.subjectsWithTests.filter { it.testCount > 0 }.sortedByDescending { it.average }
        if (activeSwt.isNotEmpty()) {

            // Multi-subject line chart
            val series = activeSwt.map { swt ->
                val c = try { Color(android.graphics.Color.parseColor(swt.subject.colorHex)) } catch (e: Exception) { Primary }
                Triple(swt.subject.name, c, swt.tests.sortedBy { it.createdAt }.map { it.percentage })
            }
            AppCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("📈 All Subjects Performance\n(Normalized to /100)",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Primary.copy(.1f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("Line Chart", fontSize = 10.sp, color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(14.dp))
                MultiLineChart(series, Modifier.fillMaxWidth().height(220.dp))
            }

            // Grade Distribution donut chart
            val allTests = appState.allTests
            if (allTests.isNotEmpty()) {
                val gradeA = allTests.count { it.percentage >= 75f }.toFloat()
                val gradeB = allTests.count { it.percentage >= 65f && it.percentage < 75f }.toFloat()
                val gradeC = allTests.count { it.percentage >= 55f && it.percentage < 65f }.toFloat()
                val gradeS = allTests.count { it.percentage >= 35f && it.percentage < 55f }.toFloat()
                val gradeF = allTests.count { it.percentage <  35f }.toFloat()
                val segments = listOf(
                    "A (75-100)" to gradeA,
                    "B (65-74)"  to gradeB,
                    "C (55-64)"  to gradeC,
                    "S (35-54)"  to gradeS,
                    "F (0-34)"   to gradeF
                )
                val donutColors = listOf(
                    Color(0xFF4ADE80), // Green  — A
                    Color(0xFF6DB8FF), // Blue   — B
                    Color(0xFF4EC9B0), // Teal   — C
                    Color(0xFFFFB347), // Orange — S
                    Color(0xFFF87171)  // Red    — F
                )
                AppCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("🎓 Grade Distribution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Primary.copy(.1f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("Overall", fontSize = 10.sp, color = Primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    DonutChart(segments, donutColors, Modifier.fillMaxWidth())
                }
            }

            AppCard(Modifier.fillMaxWidth()) {
                Text("📈 Subject Performance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                activeSwt.forEach { swt -> SubjectPerfRow(swt); Spacer(Modifier.height(12.dp)) }
            }

            // Recent tests
            val recent = appState.allTests.sortedByDescending { it.createdAt }.take(5)
            if (recent.isNotEmpty()) {
                AppCard(Modifier.fillMaxWidth()) {
                    Text("🕐 Recent Tests", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    recent.forEachIndexed { i, test ->
                        val subject     = appState.subjects.find { it.id == test.subjectId }
                        val subjectColor = try { Color(android.graphics.Color.parseColor(subject?.colorHex ?: "#6e56cf")) } catch (e: Exception) { Primary }
                        RecentTestRow(test, subject?.name ?: "Unknown", subjectColor)
                        if (i < recent.lastIndex) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(.4f))
                    }
                }
            }

            // Smart insights
            SmartInsights(appState.subjectsWithTests)
        } else {
            EmptyState("🎓", "Welcome to AcadeMind!", "Start by adding your subjects, then log your test results to see analytics here.")
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun SubjectPerfRow(swt: SubjectWithTests) {
    val c = try { Color(android.graphics.Color.parseColor(swt.subject.colorHex)) } catch (e: Exception) { Primary }
    val gc = Color(MainViewModel.getGradeColor(swt.average))
    Column {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(c, androidx.compose.foundation.shape.CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(swt.subject.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
            Text("${"%.1f".format(swt.average)}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = gc)
        }
        Spacer(Modifier.height(6.dp))
        PercentageBar(swt.average, c, Modifier.fillMaxWidth(), showLabel = false)
    }
}

@Composable
private fun RecentTestRow(test: TestResult, subjectName: String, subjectColor: Color) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(test.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(subjectName, style = MaterialTheme.typography.labelSmall, color = subjectColor)
        }
        Column(horizontalAlignment = Alignment.End) {
            GradeBadge(test.grade, test.percentage)
            Text(test.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SmartInsights(subjectsWithTests: List<SubjectWithTests>) {
    val insights = mutableListOf<Triple<String, String, String>>()
    val active = subjectsWithTests.filter { it.testCount > 0 }.sortedBy { it.average }
    active.firstOrNull()?.let { w -> insights.add(Triple("⚠️", "Needs attention: ${w.subject.name}", "Average ${"%.1f".format(w.average)}% – consider extra study time")) }
    active.lastOrNull()?.let  { b -> insights.add(Triple("🏆", "Best subject: ${b.subject.name}",     "Average ${"%.1f".format(b.average)}% – keep it up!")) }
    val allTests = subjectsWithTests.flatMap { it.tests }
    val recent   = allTests.sortedByDescending { it.createdAt }.take(5)
    val older    = allTests.sortedByDescending { it.createdAt }.drop(5).take(5)
    if (recent.size >= 2 && older.isNotEmpty()) {
        val rAvg = recent.map { it.percentage }.average().toFloat()
        val oAvg = older.map  { it.percentage }.average().toFloat()
        if      (rAvg > oAvg + 3) insights.add(Triple("📈", "Improving trend", "Recent avg up ${"%.1f".format(rAvg - oAvg)}% vs earlier tests"))
        else if (rAvg < oAvg - 3) insights.add(Triple("📉", "Declining trend", "Recent scores down – review weak areas"))
    }
    if (insights.isEmpty()) return
    AppCard(Modifier.fillMaxWidth()) {
        Text("💡 Smart Insights", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        insights.forEach { (icon, title, sub) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).background(Primary.copy(.07f), RoundedCornerShape(12.dp)).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Text(icon, fontSize = 18.sp)
                Column {
                    Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text(sub,   style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
