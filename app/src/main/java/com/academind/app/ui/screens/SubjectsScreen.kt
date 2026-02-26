package com.academind.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun SubjectsScreen(viewModel: MainViewModel, appState: AppState) {
    var showAddSubject    by remember { mutableStateOf(false) }
    var showAddTest       by remember { mutableStateOf(false) }
    var selectedSubjectId by remember { mutableStateOf<Long?>(null) }
    var editingSubject    by remember { mutableStateOf<Subject?>(null) }
    var editingTest       by remember { mutableStateOf<TestResult?>(null) }
    var expandedCard      by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 100.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { showAddSubject = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Subject", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        if (appState.subjects.isEmpty()) return@Button
                        selectedSubjectId = appState.subjects.first().id
                        showAddTest = true
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Test", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            Text(
                text = "${appState.subjects.size} subject${if (appState.subjects.size != 1) "s" else ""} enrolled",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (appState.subjects.isEmpty()) {
            item { EmptyState(icon = "📚", title = "No subjects yet", subtitle = "Tap 'Add Subject' to get started") }
        } else {
            items(items = appState.subjectsWithTests, key = { it.subject.id }) { swt ->
                SubjectCard(
                    swt = swt,
                    isExpanded = expandedCard == swt.subject.id,
                    onToggle = { expandedCard = if (expandedCard == swt.subject.id) null else swt.subject.id },
                    onAddTest = { selectedSubjectId = swt.subject.id; showAddTest = true },
                    onEdit = { editingSubject = swt.subject },
                    onDelete = { viewModel.deleteSubject(swt.subject) },
                    onEditTest = { editingTest = it },
                    onDeleteTest = { viewModel.deleteTest(it) }
                )
            }
        }
    }

    if (showAddSubject) {
        AddSubjectDialog(
            onDismiss = { showAddSubject = false },
            onConfirm = { n, c, col -> viewModel.addSubject(n, c, col); showAddSubject = false }
        )
    }
    if (showAddTest) {
        AddTestDialog(
            subjects = appState.subjects,
            initSubjectId = selectedSubjectId,
            onDismiss = { showAddTest = false },
            onConfirm = { sid, n, m, t, d -> viewModel.addTest(sid, n, m, t, d); showAddTest = false }
        )
    }
    editingSubject?.let { s ->
        EditSubjectDialog(
            subject = s,
            onDismiss = { editingSubject = null },
            onConfirm = { upd -> viewModel.updateSubject(upd); editingSubject = null }
        )
    }
    editingTest?.let { t ->
        SubjectEditTestDialog(
            test = t,
            subjects = appState.subjects,
            onDismiss = { editingTest = null },
            onConfirm = { upd -> viewModel.updateTest(upd); editingTest = null }
        )
    }
}

@Composable
private fun SubjectCard(
    swt: SubjectWithTests,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAddTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEditTest: (TestResult) -> Unit,
    onDeleteTest: (TestResult) -> Unit
) {
    val sc  = try { Color(android.graphics.Color.parseColor(swt.subject.colorHex)) } catch (e: Exception) { Primary }
    val gc  = Color(MainViewModel.getGradeColor(swt.average))
    var del by remember { mutableStateOf(false) }

    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onToggle) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(sc.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Text(swt.subject.name.firstOrNull()?.toString() ?: "?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = sc)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(swt.subject.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    if (swt.subject.code.isNotEmpty()) {
                        Text(swt.subject.code, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (swt.testCount > 0) GradeBadge(grade = MainViewModel.getGradeFromPct(swt.average), pct = swt.average)
                Text("${swt.testCount} tests", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (swt.testCount > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Average", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${"%.1f".format(swt.average)}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = gc)
            }
            Spacer(modifier = Modifier.height(4.dp))
            PercentageBar(percentage = swt.average, color = sc, modifier = Modifier.fillMaxWidth(), showLabel = false)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onAddTest,
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                border = BorderStroke(1.dp, sc)
            ) {
                Text("+ Test", fontSize = 12.sp, color = sc)
            }
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { del = true },
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                border = BorderStroke(1.dp, Danger.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Danger)
            }
        }

        if (isExpanded && swt.tests.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Test Results", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            swt.tests.sortedByDescending { it.createdAt }.forEach { test ->
                TestRowItem(test = test, onEdit = onEditTest, onDelete = onDeleteTest)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (del) {
        AlertDialog(
            onDismissRequest = { del = false },
            title = { Text("Delete Subject?") },
            text  = { Text("'${swt.subject.name}' and all ${swt.testCount} test(s) will be permanently deleted.") },
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
private fun TestRowItem(test: TestResult, onEdit: (TestResult) -> Unit, onDelete: (TestResult) -> Unit) {
    val ms = if (test.marks == test.marks.toLong().toFloat()) test.marks.toLong().toString() else "%.1f".format(test.marks)
    val ts = if (test.total == test.total.toLong().toFloat()) test.total.toLong().toString() else "%.1f".format(test.total)
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(test.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text("$ms/$ts • ${test.date}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            GradeBadge(grade = test.grade, pct = test.percentage)
            IconButton(onClick = { onEdit(test) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onDelete(test) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Danger.copy(alpha = 0.7f))
            }
        }
    }
}

// ── Add Subject Dialog ───────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSubjectDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name  by remember { mutableStateOf("") }
    var code  by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(MainViewModel.SUBJECT_COLORS.first().first) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("➕ Add Subject", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Subject Name*") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Code (optional)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Color", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MainViewModel.SUBJECT_COLORS.forEach { (hex, _) ->
                        val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Primary }
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(c)
                                .border(if (color == hex) 3.dp else 0.dp, Color.White, CircleShape)
                                .clickable { color = hex }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                    Button(
                        onClick = { onConfirm(name, code, color) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("Add") }
                }
            }
        }
    }
}

// ── Add Test Dialog ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTestDialog(
    subjects: List<Subject>,
    initSubjectId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, String, String) -> Unit
) {
    var name      by remember { mutableStateOf("") }
    var marks     by remember { mutableStateOf("") }
    var total     by remember { mutableStateOf("") }
    var date      by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var subjectId by remember { mutableStateOf(initSubjectId ?: subjects.firstOrNull()?.id ?: 0L) }
    var expanded  by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("📝 Log Test Result", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = subjects.find { it.id == subjectId }?.name ?: "Select",
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
                    label = { Text("Test Name*") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = marks,
                        onValueChange = { marks = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Marks*") },
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
                        label = { Text("Out of*") },
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
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                    Button(
                        onClick = { onConfirm(subjectId, name, marks, total, date) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) { Text("Save", color = Color.White) }
                }
            }
        }
    }
}

// ── Edit Subject Dialog ──────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSubjectDialog(subject: Subject, onDismiss: () -> Unit, onConfirm: (Subject) -> Unit) {
    var name  by remember { mutableStateOf(subject.name) }
    var code  by remember { mutableStateOf(subject.code) }
    var color by remember { mutableStateOf(subject.colorHex) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("✏️ Edit Subject", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Subject Name") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Subject Code") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MainViewModel.SUBJECT_COLORS.forEach { (hex, _) ->
                        val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Primary }
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(c)
                                .border(if (color == hex) 3.dp else 0.dp, Color.White, CircleShape)
                                .clickable { color = hex }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                    Button(
                        onClick = { onConfirm(subject.copy(name = name, code = code, colorHex = color)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Save") }
                }
            }
        }
    }
}

// ── Edit Test Dialog (from Subjects) ────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectEditTestDialog(
    test: TestResult,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onConfirm: (TestResult) -> Unit
) {
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
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
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
