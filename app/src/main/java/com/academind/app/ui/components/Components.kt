package com.academind.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.academind.app.ui.theme.*
import com.academind.app.viewmodel.MainViewModel

// ── Card ─────────────────────────────────────────────────────────
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    val cardShape  = RoundedCornerShape(20.dp)
    val cardElev   = CardDefaults.cardElevation(defaultElevation = 0.dp)
    val inner: @Composable () -> Unit = { Column(modifier = Modifier.padding(20.dp), content = content) }
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = cardShape,
            colors = cardColors,
            elevation = cardElev,
            border = cardBorder
        ) { inner() }
    } else {
        Card(
            modifier = modifier,
            shape = cardShape,
            colors = cardColors,
            elevation = cardElev,
            border = cardBorder
        ) { inner() }
    }
}

// ── KPI Card ─────────────────────────────────────────────────────
@Composable
fun KpiCard(label: String, value: String, subtitle: String, accentColor: Color, modifier: Modifier = Modifier, icon: String = "") {
    AppCard(modifier = modifier) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(8.dp))
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = accentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (icon.isNotEmpty()) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(accentColor.copy(.15f)), Alignment.Center) {
                    Text(icon, fontSize = 20.sp)
                }
            }
        }
    }
}

// ── Grade Badge ──────────────────────────────────────────────────
@Composable
fun GradeBadge(grade: String, pct: Float, modifier: Modifier = Modifier) {
    val color = Color(MainViewModel.getGradeColor(pct))
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(.15f)).padding(horizontal = 10.dp, vertical = 4.dp), Alignment.Center) {
        Text(grade, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// ── Progress Bar ─────────────────────────────────────────────────
@Composable
fun PercentageBar(percentage: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp, showLabel: Boolean = true) {
    val anim by animateFloatAsState(
        targetValue = (percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(600, easing = FastOutSlowInEasing), label = "bar"
    )
    Box(modifier.height(height).clip(RoundedCornerShape(height / 2)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(anim).clip(RoundedCornerShape(height / 2))
            .background(Brush.horizontalGradient(listOf(color.copy(.8f), color))))
    }
}

// ── Bar Chart (FIXED with BoxWithConstraints) ─────────────────────
@Composable
fun BarChart(data: List<Pair<String, Float>>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) {
        Box(modifier, Alignment.Center) { Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }
    BoxWithConstraints(modifier = modifier) {
        val totalHeight = maxHeight
        Row(Modifier.fillMaxSize(), Arrangement.SpaceEvenly, Alignment.Bottom) {
            data.forEach { (label, value) ->
                val barColor  = Color(MainViewModel.getGradeColor(value))
                val animFrac by animateFloatAsState(
                    targetValue = (value / 100f).coerceIn(0f, 1f),
                    animationSpec = tween(800, easing = FastOutSlowInEasing), label = "bar_$label"
                )
                Column(Modifier.weight(1f).fillMaxHeight(), Arrangement.Bottom, Alignment.CenterHorizontally) {
                    Text("${value.toInt()}%", fontSize = 9.sp, color = barColor, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Box(Modifier.width(18.dp).height(totalHeight * animFrac * 0.72f)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(Brush.verticalGradient(listOf(barColor, barColor.copy(.6f)))))
                    Spacer(Modifier.height(4.dp))
                    Text(label, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ── Line Chart ───────────────────────────────────────────────────
@Composable
fun LineChart(dataPoints: List<Float>, labels: List<String>, color: Color, modifier: Modifier = Modifier) {
    if (dataPoints.isEmpty()) {
        Box(modifier, Alignment.Center) { Text("No data yet", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }
    val gridColor = MaterialTheme.colorScheme.outline.copy(.3f)
    Canvas(modifier) {
        if (dataPoints.size < 2) {
            drawCircle(color, 6.dp.toPx(), Offset(size.width / 2, size.height - dataPoints[0] / 100f * size.height))
            return@Canvas
        }
        val stepX  = size.width / (dataPoints.size - 1f)
        val points = dataPoints.mapIndexed { i, v -> Offset(i * stepX, (size.height - v / 100f * size.height).coerceIn(0f, size.height)) }
        repeat(5) { i -> val y = size.height * (1f - i * 0.25f); drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()) }
        val fillPath = Path().apply {
            moveTo(0f, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(size.width, size.height); close()
        }
        drawPath(fillPath, Brush.verticalGradient(listOf(color.copy(.3f), Color.Transparent)))
        val linePath = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                val cx = (points[i-1].x + points[i].x) / 2
                cubicTo(cx, points[i-1].y, cx, points[i].y, points[i].x, points[i].y)
            }
        }
        drawPath(linePath, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        points.forEach { p -> drawCircle(Color.White, 4.dp.toPx(), p); drawCircle(color, 3.dp.toPx(), p) }
    }
}

// ── Toast ────────────────────────────────────────────────────────
@Composable
fun ToastMessage(message: String, type: String, onDismiss: () -> Unit) {
    val bg   = when(type) { "success" -> Success; "error" -> Danger; "warning" -> Warning; else -> Primary }
    val icon = when(type) { "success" -> "✅"; "error" -> "❌"; "warning" -> "⚠️"; else -> "ℹ️" }
    LaunchedEffect(message) { kotlinx.coroutines.delay(3000); onDismiss() }
    Snackbar(Modifier.padding(16.dp), containerColor = bg, contentColor = Color.White,
        action = { TextButton(onDismiss) { Text("✕", color = Color.White, fontWeight = FontWeight.Bold) } }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(icon); Text(message, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Empty State ──────────────────────────────────────────────────
@Composable
fun EmptyState(icon: String = "📭", title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(40.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

// ── Multi Line Chart (Dashboard - all subjects normalized to /100) ─
@Composable
fun MultiLineChart(
    series: List<Triple<String, Color, List<Float>>>,   // (subjectName, color, chronological scores)
    modifier: Modifier = Modifier
) {
    if (series.isEmpty() || series.all { it.third.isEmpty() }) {
        Box(modifier, Alignment.Center) { Text("No data yet", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }
    val maxTests = series.maxOf { it.third.size }
    if (maxTests == 0) return
    val gridColor  = MaterialTheme.colorScheme.outline.copy(.2f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier) {
        // Legend
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            series.forEach { (name, color, _) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(22.dp, 3.dp).background(color, RoundedCornerShape(2.dp)))
                    Text(name.uppercase(), fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(180.dp)) {
            // Y-axis labels
            Column(
                modifier = Modifier.width(38.dp).fillMaxHeight().padding(end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                listOf("100%","80%","60%","40%","20%","0%").forEach {
                    Text(it, fontSize = 8.sp, color = labelColor)
                }
            }
            // Canvas
            Canvas(Modifier.weight(1f).fillMaxHeight()) {
                // Grid lines at 0/20/40/60/80/100
                listOf(0f,20f,40f,60f,80f,100f).forEach { pct ->
                    val y = size.height * (1f - pct / 100f)
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                series.forEach { (_, color, scores) ->
                    if (scores.isEmpty()) return@forEach
                    val stepX = if (maxTests > 1) size.width / (maxTests - 1).toFloat() else size.width / 2f
                    val points = scores.mapIndexed { i, v ->
                        Offset(i * stepX, (size.height * (1f - v / 100f)).coerceIn(0f, size.height))
                    }
                    if (points.size >= 2) {
                        val path = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                val cx = (points[i-1].x + points[i].x) / 2f
                                cubicTo(cx, points[i-1].y, cx, points[i].y, points[i].x, points[i].y)
                            }
                        }
                        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    }
                    points.forEach { p ->
                        drawCircle(Color.White, 4.5.dp.toPx(), p)
                        drawCircle(color, 3.5.dp.toPx(), p)
                    }
                }
            }
        }
        // X-axis labels
        Row(Modifier.fillMaxWidth().padding(start = 38.dp, top = 4.dp)) {
            (1..maxTests).forEach { i ->
                Text("Test $i", fontSize = 8.sp, color = labelColor,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
    }
}

// ── Analytics Trend Chart (line + dashed avg) ─────────────────────
@Composable
fun AnalyticsTrendChart(
    scores: List<Float>,
    avgLine: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (scores.isEmpty()) return
    val gridColor = MaterialTheme.colorScheme.outline.copy(.2f)
    val avgColor  = Color(0xFFFFB347).copy(.8f)
    Canvas(modifier) {
        // Grid lines
        listOf(0f,25f,50f,75f,100f).forEach { pct ->
            val y = size.height * (1f - pct / 100f)
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 0.8.dp.toPx())
        }
        // Avg dashed line
        val avgY = (size.height * (1f - avgLine / 100f)).coerceIn(0f, size.height)
        val dash = 12.dp.toPx(); val gap = 4.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(avgColor, Offset(x, avgY), Offset((x + dash).coerceAtMost(size.width), avgY), 1.5.dp.toPx())
            x += dash + gap
        }
        val stepX = if (scores.size > 1) size.width / (scores.size - 1).toFloat() else size.width / 2f
        val points = scores.mapIndexed { i, v ->
            Offset(i * stepX, (size.height * (1f - v / 100f)).coerceIn(0f, size.height))
        }
        if (points.size >= 2) {
            val fill = Path().apply {
                moveTo(points[0].x, size.height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, size.height); close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(color.copy(.3f), Color.Transparent)))
            val line = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    val cx = (points[i-1].x + points[i].x) / 2f
                    cubicTo(cx, points[i-1].y, cx, points[i].y, points[i].x, points[i].y)
                }
            }
            drawPath(line, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        }
        points.forEachIndexed { i, p ->
            val gc = Color(MainViewModel.getGradeColor(scores[i]))
            drawCircle(Color.White, 5.dp.toPx(), p)
            drawCircle(gc, 4.dp.toPx(), p)
        }
    }
}

// ── Analytics Bar Chart (per-test, grade-colored) ─────────────────
@Composable
fun AnalyticsBarChart(scores: List<Float>, modifier: Modifier = Modifier) {
    if (scores.isEmpty()) return
    BoxWithConstraints(modifier) {
        val h = maxHeight
        Row(Modifier.fillMaxSize(), Arrangement.SpaceEvenly, Alignment.Bottom) {
            scores.forEachIndexed { i, score ->
                val barColor = Color(MainViewModel.getGradeColor(score))
                val animFrac by animateFloatAsState(
                    targetValue = (score / 100f).coerceIn(0f, 1f),
                    animationSpec = tween(800, easing = FastOutSlowInEasing), label = "ab_$i"
                )
                Column(Modifier.weight(1f).fillMaxHeight(), Arrangement.Bottom, Alignment.CenterHorizontally) {
                    Text("${score.toInt()}%", fontSize = 8.sp, color = barColor, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Box(Modifier.width(14.dp).height(h * animFrac * 0.82f)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(Brush.verticalGradient(listOf(barColor, barColor.copy(.55f)))))
                    Spacer(Modifier.height(4.dp))
                    Text("T${i+1}", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Donut / Pie Chart ────────────────────────────────────────────
@Composable
fun DonutChart(
    segments: List<Pair<String, Float>>,   // (label, value) — values are counts, not pct
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    if (segments.isEmpty() || segments.all { it.second == 0f }) {
        Box(modifier, Alignment.Center) {
            Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        return
    }
    val total = segments.sumOf { it.second.toDouble() }.toFloat()
    val sweeps = segments.map { it.second / total * 360f }
    val animatedSweeps = sweeps.mapIndexed { i, target ->
        animateFloatAsState(
            targetValue = target,
            animationSpec = tween(900, delayMillis = i * 80, easing = FastOutSlowInEasing),
            label = "donut_$i"
        ).value
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(160.dp), Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                var startAngle = -90f
                val stroke = size.minDimension * 0.22f
                val inset  = stroke / 2f
                animatedSweeps.forEachIndexed { i, sweep ->
                    if (sweep > 0f) {
                        drawArc(
                            color = colors[i % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sweep - 2f,           // 2° gap between segments
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Butt),
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - stroke, size.height - stroke)
                        )
                        startAngle += sweep
                    }
                }
            }
            // Centre total label
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${total.toInt()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Tests",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        // Legend — two rows
        val nonEmpty = segments.mapIndexedNotNull { i, (label, count) ->
            if (count > 0f) Triple(label, count, colors[i % colors.size]) else null
        }
        nonEmpty.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (label, count, color) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
                        Text(
                            "$label (${count.toInt()})",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
        }
    }
}

// ── Countdown Box ────────────────────────────────────────────────
@Composable
fun CountdownBox(days: Long, hours: Long, minutes: Long, seconds: Long, isSet: Boolean, isPast: Boolean) {
    AppCard {
        Text("📅 Exam Countdown", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 12.dp))
        when {
            !isSet -> Text("No exam date set — configure in Settings", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            isPast -> Text("🎉 Exam day has passed!", style = MaterialTheme.typography.bodyMedium, color = Success, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            else   -> Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                listOf(days to "DAYS", hours to "HRS", minutes to "MIN", seconds to "SEC").forEach { (v, l) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 8.dp), Alignment.Center) {
                            Text(v.toString().padStart(2, '0'), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryLight)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(l, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
                    }
                }
            }
        }
    }
}
