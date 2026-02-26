package com.academind.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.academind.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class AppState(
    val userPrefs: UserPrefs = UserPrefs(),
    val subjects: List<Subject> = emptyList(),
    val allTests: List<TestResult> = emptyList(),
    val subjectsWithTests: List<SubjectWithTests> = emptyList()
)

data class DashboardStats(
    val totalSubjects: Int = 0,
    val totalTests: Int = 0,
    val overallAverage: Float = 0f,
    val overallGrade: String = "—",
    val bestSubject: String = "—",
    val passRate: Float = 0f
)

data class ExamCountdown(
    val days: Long = 0,
    val hours: Long = 0,
    val minutes: Long = 0,
    val seconds: Long = 0,
    val isSet: Boolean = false,
    val isPast: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db            = AppDatabase.getInstance(application)
    private val subjectDao    = db.subjectDao()
    private val testDao       = db.testResultDao()
    private val userPrefs_    = UserPreferences(application)

    private val _subjects = subjectDao.getAllSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _allTests = testDao.getAllTests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPrefs: StateFlow<UserPrefs> = userPrefs_.userPrefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPrefs())

    val appState: StateFlow<AppState> = combine(_subjects, _allTests, userPrefs_. userPrefs) { subjects, tests, prefs ->
        AppState(
            userPrefs          = prefs,
            subjects           = subjects,
            allTests           = tests,
            subjectsWithTests  = subjects.map { s -> SubjectWithTests(s, tests.filter { it.subjectId == s.id }) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppState())

    val dashboardStats: StateFlow<DashboardStats> = appState.map { state ->
        val tests = state.allTests
        val avg   = if (tests.isEmpty()) 0f else tests.map { it.percentage }.average().toFloat()
        DashboardStats(
            totalSubjects  = state.subjects.size,
            totalTests     = tests.size,
            overallAverage = avg,
            overallGrade   = if (tests.isEmpty()) "—" else getGradeFromPct(avg),
            bestSubject    = state.subjectsWithTests.maxByOrNull { it.average }?.subject?.name ?: "—",
            passRate       = if (tests.isEmpty()) 0f else tests.count { it.isPassing }.toFloat() / tests.size * 100f
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // Toast
    private val _toast = MutableSharedFlow<Pair<String, String>>()
    val toastMessage: SharedFlow<Pair<String, String>> = _toast

    // ── Setup ──────────────────────────────────────────
    fun completeSetup(name: String, examDate: String, level: String) {
        if (name.isBlank()) { toast("Please enter your name", "error"); return }
        viewModelScope.launch { userPrefs_.saveUserSetup(name.trim(), examDate, level) }
    }

    // ── Subjects ───────────────────────────────────────
    fun addSubject(name: String, code: String, colorHex: String) {
        if (name.isBlank()) { toast("Subject name cannot be empty", "error"); return }
        viewModelScope.launch {
            subjectDao.insertSubject(Subject(name = name.trim(), code = code.trim(), colorHex = colorHex))
            toast("Subject added!", "success")
        }
    }
    fun updateSubject(subject: Subject) = viewModelScope.launch { subjectDao.updateSubject(subject); toast("Subject updated", "success") }
    fun deleteSubject(subject: Subject) = viewModelScope.launch { subjectDao.deleteSubject(subject); toast("Subject deleted", "info") }

    // ── Tests ──────────────────────────────────────────
    fun addTest(subjectId: Long, name: String, marks: String, total: String, date: String) {
        val m = marks.toFloatOrNull()
        val t = total.toFloatOrNull()
        when {
            name.isBlank()      -> { toast("Test name is required", "error"); return }
            m == null           -> { toast("Invalid marks value", "error"); return }
            t == null || t <= 0 -> { toast("Invalid total marks", "error"); return }
            m < 0               -> { toast("Marks cannot be negative", "error"); return }
            m > t               -> { toast("Marks cannot exceed total", "error"); return }
        }
        viewModelScope.launch {
            testDao.insertTest(
                TestResult(
                    subjectId = subjectId, name = name.trim(), marks = m!!, total = t!!,
                    date = date.ifBlank { LocalDate.now().format(DateTimeFormatter.ISO_DATE) }
                )
            )
            toast("Test saved!", "success")
        }
    }
    fun updateTest(test: TestResult) = viewModelScope.launch { testDao.updateTest(test); toast("Test updated", "success") }
    fun deleteTest(test: TestResult) = viewModelScope.launch { testDao.deleteTest(test); toast("Test deleted", "info") }

    // ── Settings ───────────────────────────────────────
    fun updateUserName(name: String) {
        if (name.isBlank()) { toast("Name cannot be empty", "error"); return }
        viewModelScope.launch { userPrefs_.updateUserName(name.trim()); toast("Name updated", "success") }
    }
    fun updateExamDate(date: String)   = viewModelScope.launch { userPrefs_.updateExamDate(date);     toast("Exam date updated", "success") }
    fun updateStudyLevel(level: String)= viewModelScope.launch { userPrefs_.updateStudyLevel(level) }
    fun toggleTheme(isDark: Boolean)   = viewModelScope.launch { userPrefs_.toggleTheme(isDark) }
    fun updateAvatar(emoji: String)    = viewModelScope.launch { userPrefs_.updateAvatar(emoji) }
    fun resetAllData() = viewModelScope.launch {
        appState.value.subjects.forEach { subjectDao.deleteSubject(it) }
        userPrefs_.resetAllData()
        toast("All data cleared", "info")
    }

    // ── Countdown (FIXED — no Period.toTotalMonths) ────
    fun computeCountdown(examDateStr: String): ExamCountdown {
        if (examDateStr.isBlank()) return ExamCountdown(isSet = false)
        return try {
            val examDate = LocalDate.parse(examDateStr)
            val examDT   = examDate.atTime(23, 59, 59)
            val now      = LocalDateTime.now()
            val totalSec = ChronoUnit.SECONDS.between(now, examDT)
            if (totalSec <= 0) return ExamCountdown(isSet = true, isPast = true)
            ExamCountdown(
                days    = totalSec / 86400L,
                hours   = (totalSec % 86400L) / 3600L,
                minutes = (totalSec % 3600L) / 60L,
                seconds = totalSec % 60L,
                isSet   = true,
                isPast  = false
            )
        } catch (e: Exception) { ExamCountdown(isSet = false) }
    }

    private fun toast(msg: String, type: String) = viewModelScope.launch { _toast.emit(msg to type) }

    companion object {
        fun getGradeFromPct(pct: Float): String = when {
            pct >= 75 -> "A"
            pct >= 65 -> "B"
            pct >= 55 -> "C"
            pct >= 35 -> "S"
            else      -> "F"
        }

        fun getGradeColor(pct: Float): Long = when {
            pct >= 75 -> 0xFF4ADE80  // Green  — A
            pct >= 65 -> 0xFF6DB8FF  // Blue   — B
            pct >= 55 -> 0xFF4EC9B0  // Teal   — C
            pct >= 35 -> 0xFFFFB347  // Orange — S
            else      -> 0xFFF87171  // Red    — F
        }

        val SUBJECT_COLORS = listOf(
            "#6e56cf" to "Purple", "#4ec9b0" to "Teal",     "#ffb347" to "Orange",
            "#4ade80" to "Green",  "#6db8ff" to "Blue",     "#f87171" to "Red",
            "#f472b6" to "Pink",   "#a78bfa" to "Lavender"
        )

        val STUDY_LEVELS = listOf(
            "secondary"     to "Secondary School",
            "alevel"        to "A-Level / Pre-U",
            "undergraduate" to "Undergraduate",
            "postgraduate"  to "Postgraduate",
            "professional"  to "Professional"
        )

        val AVATAR_EMOJIS = listOf(
            "🎓","📚","🧑‍💻","👨‍🎨","👩‍🔬","🦁","🐺","🦊",
            "🐉","⭐","🌟","🔥","💎","🚀","🎯","🏆"
        )

        val QUOTES = listOf(
            "Success is the sum of small efforts, repeated day in and day out.",
            "The expert in anything was once a beginner.",
            "Hard work beats talent when talent doesn't work hard.",
            "Learning is the only thing the mind never exhausts.",
            "Don't watch the clock; do what it does — keep going.",
            "The beautiful thing about learning is that nobody can take it away from you.",
            "Your only limit is your mind.",
            "Study while others are sleeping; work while others are loafing.",
            "Believe you can and you're halfway there.",
            "It always seems impossible until it is done."
        )
    }
}
