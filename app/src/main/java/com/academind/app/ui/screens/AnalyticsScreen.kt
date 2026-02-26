package com.academind.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.academind.app.data.SubjectWithTests
import com.academind.app.ui.components.*
import com.academind.app.ui.theme.*
import com.academind.app.viewmodel.AppState
import com.academind.app.viewmodel.MainViewModel
import kotlin.math.abs
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(appState: AppState) {
    val subjectsWithData = remember(appState.subjectsWithTests) {
        appState.subjectsWithTests.filter { it.testCount > 0 }
    }
    var selectedSubjectId by remember(subjectsWithData) {
        mutableStateOf(subjectsWithData.firstOrNull()?.subject?.id)
    }
    var expanded by remember { mutableStateOf(false) }
    val selectedSwt = subjectsWithData.find { it.subject.id == selectedSubjectId }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // Subject Selector Card
        AppCard(Modifier.fillMaxWidth()) {
            Text("SELECT SUBJECT",
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (subjectsWithData.isNotEmpty()) expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedSwt?.subject?.name ?: "Select a subject",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    subjectsWithData.forEach { swt ->
                        val c = try { Color(android.graphics.Color.parseColor(swt.subject.colorHex)) } catch (e: Exception) { Primary }
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(Modifier.size(10.dp).background(c, CircleShape))
                                    Text(swt.subject.name, Modifier.weight(1f))
                                    Text("${swt.testCount} tests", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = { selectedSubjectId = swt.subject.id; expanded = false }
                        )
                    }
                    if (subjectsWithData.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No subjects with tests yet", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { expanded = false }
                        )
                    }
                }
            }
        }

        if (selectedSwt != null) {
            SubjectDetailedAnalytics(selectedSwt)
        } else {
            Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), Alignment.Center) {
                EmptyState("📊", "Select a subject with test data to view analytics",
                    "Add subjects and test results in the Subjects tab first")
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun SubjectDetailedAnalytics(swt: SubjectWithTests) {
    val tests    = remember(swt) { swt.tests.sortedBy { it.createdAt } }
    val scores   = remember(tests) { tests.map { it.percentage } }
    val avg      = swt.average
    val best     = swt.bestScore
    val worst    = swt.worstScore
    val subColor = try { Color(android.graphics.Color.parseColor(swt.subject.colorHex)) } catch (e: Exception) { Primary }

    if (scores.isEmpty()) {
        EmptyState("📝", "No tests yet", "Add test results for ${swt.subject.name}")
        return
    }

    // Calculations
    val variance    = scores.map { s -> (s - avg) * (s - avg) }.average().toFloat()
    val stdDev      = sqrt(variance.toDouble()).toFloat()
    val consistency = (100f - stdDev * 2f).coerceAtLeast(0f)
    val trend = when {
        scores.size >= 2 && scores.last() > scores.first() + 5f -> "up"
        scores.size >= 2 && scores.last() < scores.first() - 5f -> "down"
        else -> "stable"
    }
    var wSum = 0f; var wTotal = 0f
    scores.forEachIndexed { i, s -> val w = (i + 1).toFloat(); wSum += s * w; wTotal += w }
    val projected   = if (wTotal > 0f) wSum / wTotal else avg
    val half        = scores.size / 2
    val firstAvg    = if (half > 0) scores.take(half).average().toFloat() else avg
    val secondAvg   = if (scores.size - half > 0) scores.drop(half).average().toFloat() else avg
    val delta       = secondAvg - firstAvg
    val deltaText   = when { delta > 2f -> "+${"%.1f".format(delta)}%"; delta < -2f -> "${"%.1f".format(delta)}%"; else -> "~${"%.1f".format(abs(delta))}%" }
    val gradeMap    = mutableMapOf<String, Int>()
    tests.forEach { t -> gradeMap[t.grade] = (gradeMap[t.grade] ?: 0) + 1 }
    val passCount   = tests.count { it.isPassing }
    val passRate    = passCount.toFloat() / tests.size * 100f
    val trendColor  = when (trend) { "up" -> Success; "down" -> Danger; else -> Warning }
    val trendArrow  = when (trend) { "up" -> "↑"; "down" -> "↓"; else -> "→" }
    val trendLabel  = when (trend) { "up" -> "Improving"; "down" -> "Declining"; else -> "Stable" }
    val projColor   = Color(MainViewModel.getGradeColor(projected))
    val consColor   = when { consistency > 70f -> Success; consistency > 40f -> Warning; else -> Danger }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // 8 KPI cards
        data class Kpi(val label: String, val value: String, val sub: String, val color: Color)
        val kpis = listOf(
            Kpi("📊 Average Score", "${"%.1f".format(avg)}%", "Grade ${MainViewModel.getGradeFromPct(avg)}", Color(MainViewModel.getGradeColor(avg))),
            Kpi("🏆 Personal Best", "${"%.1f".format(best)}%", "${MainViewModel.getGradeFromPct(best)} Grade", Success),
            Kpi("📉 Lowest Score", "${"%.1f".format(worst)}%", "Room to improve", Danger),
            Kpi("📈 Trend", trendArrow, trendLabel, trendColor),
            Kpi("🎯 Consistency", "${"%.0f".format(consistency)}%", "σ = ${"%.1f".format(stdDev)}", consColor),
            Kpi("🔮 Projected", "${"%.1f".format(projected)}%", "Weighted recent avg", projColor),
            Kpi("✅ Pass Rate", "${"%.0f".format(passRate)}%", "$passCount/${tests.size} passed", Accent),
            Kpi("📋 Tests Taken", "${tests.size}", "Δ $deltaText", Primary),
        )
        kpis.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { kpi ->
                    AppCard(Modifier.weight(1f)) {
                        Text(kpi.label, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Text(kpi.value,
                            style = if (kpi.value.length <= 3) MaterialTheme.typography.headlineMedium
                                    else MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = kpi.color, maxLines = 1)
                        Text(kpi.sub, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        // Score Trend Chart
        AppCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Score Trend", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                AnalChip("Over Time")
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(16.dp, 3.dp).background(subColor, RoundedCornerShape(2.dp)))
                    Text("Score", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(16.dp, 2.dp).background(Color(0xFFFFB347).copy(.8f), RoundedCornerShape(1.dp)))
                    Text("Average", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(10.dp))
            AnalyticsTrendChart(scores, avg, subColor, Modifier.fillMaxWidth().height(160.dp))
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                scores.forEachIndexed { i, _ ->
                    Text("T${i+1}", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
        }

        // Per-test Bar Chart
        AppCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Test Scores", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                AnalChip("Per Test")
            }
            Spacer(Modifier.height(12.dp))
            AnalyticsBarChart(scores, Modifier.fillMaxWidth().height(150.dp))
        }

        // Grade Distribution
        AppCard(Modifier.fillMaxWidth()) {
            Text("Grade Distribution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            val gColors = mapOf(
                "A" to Color(0xFF4ADE80),   // Green
                "B" to Color(0xFF6DB8FF),   // Blue
                "C" to Color(0xFF4EC9B0),   // Teal
                "S" to Color(0xFFFFB347),   // Orange
                "F" to Danger
            )
            listOf("A","B","C","S","F").forEach { grade ->
                val count = gradeMap[grade] ?: 0
                if (count > 0) {
                    val pct   = count.toFloat() / tests.size * 100f
                    val color = gColors[grade] ?: Primary
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(36.dp).height(22.dp)
                            .background(color.copy(.15f), RoundedCornerShape(6.dp)), Alignment.Center) {
                            Text(grade, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
                        }
                        Box(Modifier.weight(1f)) {
                            PercentageBar(pct, color, Modifier.fillMaxWidth(), 10.dp, false)
                        }
                        Text("$count", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color,
                            modifier = Modifier.width(20.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(.3f))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                GradeCount("A",   gradeMap["A"] ?: 0, Color(0xFF4ADE80))
                GradeCount("B",   gradeMap["B"] ?: 0, Color(0xFF6DB8FF))
                GradeCount("C",   gradeMap["C"] ?: 0, Color(0xFF4EC9B0))
                GradeCount("S",   gradeMap["S"] ?: 0, Color(0xFFFFB347))
                GradeCount("F",   gradeMap["F"] ?: 0, Danger)
            }
        }

        // Test Breakdown
        AppCard(Modifier.fillMaxWidth()) {
            Text("Test Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("#",      fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(18.dp))
                Text("Test",   fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("Score",  fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(52.dp))
                Text("Grade",  fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(38.dp))
                Text("vs Avg", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(44.dp))
            }
            Spacer(Modifier.height(6.dp))
            tests.forEachIndexed { i, test ->
                val pct  = test.percentage
                val diff = pct - avg
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("${i+1}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(18.dp))
                    Text(test.name, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${test.marks.toInt()}/${test.total.toInt()}", fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace, modifier = Modifier.width(52.dp))
                    GradeBadge(test.grade, pct, Modifier.width(38.dp))
                    Text("${if (diff >= 0f) "+" else ""}${"%.1f".format(diff)}%",
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = if (diff >= 0f) Success else Danger,
                        modifier = Modifier.width(44.dp))
                }
                if (i < tests.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(.2f))
            }
        }

        // Performance Insights
        val insights = buildInsights(swt.subject.name, avg, stdDev, trend, projected, tests.size)
        AppCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Performance Insights", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                AnalChip(swt.subject.name)
            }
            Spacer(Modifier.height(12.dp))
            insights.forEach { (icon, title, body) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)
                    .background(Primary.copy(.06f), RoundedCornerShape(12.dp)).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Text(icon, fontSize = 20.sp)
                    Column {
                        Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(3.dp))
                        Text(body, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun AnalChip(text: String) {
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Primary.copy(.1f))
        .padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(text, fontSize = 10.sp, color = Primary, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun GradeCount(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun buildInsights(
    name: String, avg: Float, stdDev: Float, trend: String, projected: Float, count: Int
): List<Triple<String, String, String>> {
    val list = mutableListOf<Triple<String, String, String>>()
    when {
        stdDev < 8f  -> list.add(Triple("🎯","Highly Consistent",
            "Scores stay within a tight range (σ = ${"%.1f".format(stdDev)}%). Disciplined preparation is paying off."))
        stdDev > 20f -> list.add(Triple("⚡","High Variability",
            "Scores vary significantly (σ = ${"%.1f".format(stdDev)}%). Standardise your revision approach for more reliable results."))
    }
    when (trend) {
        "up"   -> list.add(Triple("🚀","Positive Momentum",
            "Scores show a clear upward trend — hard work is paying off. Keep this rhythm going."))
        "down" -> list.add(Triple("⚠️","Declining Trend",
            "Recent scores are lower than earlier ones. Review your study strategy and revision frequency."))
    }
    when {
        avg >= 80f -> list.add(Triple("🏆","Top Performer",
            "Averaging ${"%.1f".format(avg)}% in $name puts you in the highest grade territory. Maintain this standard."))
        avg < 35f  -> list.add(Triple("📖","Foundation Focus",
            "At ${"%.1f".format(avg)}% average, prioritise core concepts. Build your foundation before tackling advanced material."))
        else        -> list.add(Triple("💪","Keep Pushing",
            "You're averaging ${"%.1f".format(avg)}%. Consistent practice and targeting weak areas will push you higher."))
    }
    if (count >= 3 && projected > avg + 3f)
        list.add(Triple("📈","Upward Projection",
            "Projected score (${"%.1f".format(projected)}%) is tracking above your average — momentum is on your side."))
    if (list.isEmpty())
        list.add(Triple("💡","Keep Going","Add more tests to unlock deeper insights about your performance."))
    return list
}
