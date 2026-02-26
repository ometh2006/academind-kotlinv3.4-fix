package com.academind.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY createdAt ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)
}

@Dao
interface TestResultDao {
    @Query("SELECT * FROM test_results ORDER BY createdAt DESC")
    fun getAllTests(): Flow<List<TestResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTest(test: TestResult): Long

    @Update
    suspend fun updateTest(test: TestResult)

    @Delete
    suspend fun deleteTest(test: TestResult)
}
