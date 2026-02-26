package com.academind.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.academind.app.data.*
import com.academind.app.ui.components.*
import com.academind.app.ui.theme.*
import com.academind.app.viewmodel.AppState
import com.academind.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: MainViewModel, appState: AppState) {
    var search        by remember { mutableStateOf("") }
    var subjectFilter by remember { mutableStateOf<Long?>(null) }
    var sortOrder     by remember { mutableStateOf("date_desc") }
    var expandedSort  by remember { mutableStateOf(false) }
    var editingTest   by remember { mutableStateOf<TestResult?>(null) }

    val withSubject = appState.allTests.mapNotNull { test ->
        val sub = appState.subjects.find { it.id == test.subjectId } ?: return@mapNotNull null
        Triple(test, sub, sub.name)
    }

    val filtered = withSubject
        .filter { (test, _, sName) ->
            (search.isBlank() || test.name.contains(search, ignoreCase = true) ||
                    sName.contains(search, ignoreCase = true)) &&
                    (subjectFilter == null || test.subjectId == subjectFilter)
        }
        .sortedWith { a, b ->
            when (sortOrder) {
                "date_desc"  -> b.first.createdAt.compareTo(a.first.createdAt)
                "date_asc"   -> a.first.createdAt.compareTo(b.first.createdAt)
                "score_desc" -> b.first.percentage.compareTo(a.first.percentage)
                "score_asc"  -> a.first.percentage.compareTo(b.first.percentage)
                "name_az"    -> a.first.name.compareTo(b.first.name, ignoreCase = true)
                else -> 0
            }
        }

    val sortOpts = listOf(
        "date_desc"  to "Newest First",
        "date_asc"   to "Oldest First",
        "score_desc" to "Highest Score",
        "score_asc"  to "Lowest Score",
        "name_az"    to "Name A-Z"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search tests or subjects…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { search = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = subjectFilter == null,
                    onClick = { subjectFilter = null },
                    label = { Text("All") }
                )
                appState.subjects.forEach { s ->
                    val c = try { Color(android.graphics.Color.parseColor(s.colorHex)) } catch (e: Exception) { Primary }
                    FilterChip(
                        selected = subjectFilter == s.id,
                        onClick = { subjectFilter = if (subjectFilter == s.id) null else s.id },
                        label = { Text(s.name, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = c.copy(alpha = 0.15f),
                            selectedLabelColor = c
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filtered.size} test${if (filtered.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExposedDropdownMenuBox(
                    expanded = expandedSort,
                    onExpandedChange = { expandedSort = it }
                ) {
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.menuAnchor().height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = sortOpts.find { it.first == sortOrder }?.second ?: "Sort", fontSize = 12.sp)
                    }
                    ExposedDropdownMenu(
                        expanded = expandedSort,
                        onDismissRequest = { expandedSort = false }
                    ) {
                        sortOpts.forEach { (k, l) ->
                            DropdownMenuItem(
                                text = { Text(l) },
                                onClick = { sortOrder = k; expandedSort = false },
                                leadingIcon = if (sortOrder == k) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = Primary) }
                                } else null
                            )
                        }
                    }
                }
            }

            if (filtered.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val avg  = filtered.map { it.first.percentage }.average().toFloat()
                val best = filtered.maxOf { it.first.percentage }
                val pass = filtered.count { it.first.isPassing }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("Avg",  "${"%.1f".format(avg)}%",  Modifier.weight(1f))
                    MiniStat("Best", "${"%.1f".format(best)}%", Modifier.weight(1f))
                    MiniStat("Pass", "$pass/${filtered.size}",  Modifier.weight(1f))
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = "📋",
                    title = if (search.isNotEmpty()) "No results found" else "No tests recorded",
                    subtitle = if (search.isNotEmpty()) "Try a different search term" else "Add test results in the Subjects tab"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = filtered, key = { it.first.id }) { (test, subject, _) ->
                    HistTestCard(
                        test = test,
                        subject = subject,
                        onEdit = { editingTest = test },
                        onDelete = { viewModel.deleteTest(test) }
                    )
                }
            }
        }
    }

    editingTest?.let { t ->
        HistEditDialog(
            test = t,
            subjects = appState.subjects,
            onDismiss = { editingTest = null },
            onConfirm = { upd -> viewModel.updateTest(upd); editingTest = null }
        )
    }
}

@Composable
private fun HistTestCard(test: TestResult, subject: Subject, onEdit: () -> Unit, onDelete: () -> Unit) {
    val sc  = try { Color(android.graphics.Color.parseColor(subject.colorHex)) } catch (e: Exception) { Primary }
    val gc  = Color(MainViewModel.getGradeColor(test.percentage))
    var del by remember { mutableStateOf(false) }
    val ms  = if (test.marks == test.marks.toLong().toFloat()) test.marks.toLong().toString() else "%.1f".format(test.marks)
    val ts  = if (test.total == test.total.toLong().toFloat()) test.total.toLong().toString() else "%.1f".format(test.total)

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(test.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subject.name, style = MaterialTheme.typography.labelSmall, color = sc, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(test.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                GradeBadge(grade = test.grade, pct = test.percentage)
                Spacer(modifier = Modifier.height(4.dp))
                Text("${"%.1f".format(test.percentage)}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = gc)
                Text("$ms/$ts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        PercentageBar(percentage = test.percentage, color = gc, modifier = Modifier.fillMaxWidth(), height = 5.dp, showLabel = false)
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { del = true },
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                border = BorderStroke(1.dp, Danger.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Danger)
            }
        }
    }

    if (del) {
        AlertDialog(
            onDismissRequest = { del = false },
            title = { Text("Delete Test?") },
            text  = { Text("'${test.name}' will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); del = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) { Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { del = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)).padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistEditDialog(test: TestResult, subjects: List<Subject>, onDismiss: () -> Unit, onConfirm: (TestResult) -> Unit) {
    val mi = if (test.marks == test.marks.toLong().toFloat()) test.marks.toLong().toString() else "%.1f".format(test.marks)
    val ti = if (test.total == test.total.toLong().toFloat()) test.total.toLong().toString() else "%.1f".format(test.total)
    var name      by remember { mutableStateOf(test.name) }
    var marks     by remember { mutableStateOf(mi) }
    var total     by remember { mutableStateOf(ti) }
    var date      by remember { mutableStateOf(test.date) }
    var subjectId by remember { mutableStateOf(test.subjectId) }
    var expanded  by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("✏️ Edit Test", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    // FIX: all named args — no positional mix-up
                    OutlinedTextField(
                        value = subjects.find { it.id == subjectId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text("Subject") },
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        subjects.forEach { s ->
                            DropdownMenuItem(text = { Text(s.name) }, onClick = { subjectId = s.id; expanded = false })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Test Name") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = marks,
                        onValueChange = { marks = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Marks") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        )
                    )
                    OutlinedTextField(
                        value = total,
                        onValueChange = { total = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Out of") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Date (YYYY-MM-DD)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Cancel") }
                    Button(
                        onClick = {
                            val m = marks.toFloatOrNull() ?: return@Button
                            val t = total.toFloatOrNull() ?: return@Button
                            onConfirm(test.copy(subjectId = subjectId, name = name, marks = m, total = t, date = date))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Save") }
                }
            }
        }
    }
}
