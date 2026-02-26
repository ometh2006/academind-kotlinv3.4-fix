package com.academind.app.data

import androidx.room.*

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String,
    val colorHex: String = "#6e56cf",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "test_results",
    foreignKeys = [ForeignKey(
        entity = Subject::class,
        parentColumns = ["id"],
        childColumns = ["subjectId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("subjectId")]
)
data class TestResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val name: String,
    val marks: Float,
    val total: Float,
    val date: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    val percentage: Float get() = if (total > 0f) (marks / total) * 100f else 0f
    val grade: String get() = when {
        percentage >= 75 -> "A"
        percentage >= 65 -> "B"
        percentage >= 55 -> "C"
        percentage >= 35 -> "S"
        else             -> "F"
    }
    val isPassing: Boolean get() = percentage >= 35f
}

data class SubjectWithTests(
    val subject: Subject,
    val tests: List<TestResult>
) {
    val testCount: Int   get() = tests.size
    val average: Float   get() = if (tests.isEmpty()) 0f else tests.map { it.percentage }.average().toFloat()
    val bestScore: Float get() = tests.maxOfOrNull { it.percentage } ?: 0f
    val worstScore: Float get() = tests.minOfOrNull { it.percentage } ?: 0f
    val passRate: Float  get() = if (tests.isEmpty()) 0f else tests.count { it.isPassing }.toFloat() / tests.size * 100f
}
