package com.academind.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

data class UserPrefs(
    val userName: String = "",
    val examDate: String = "",
    val studyLevel: String = "secondary",
    val isSetupComplete: Boolean = false,
    val isDarkTheme: Boolean = true,
    val avatarEmoji: String = "🎓"
)

class UserPreferences(private val context: Context) {
    companion object {
        val USER_NAME         = stringPreferencesKey("user_name")
        val EXAM_DATE         = stringPreferencesKey("exam_date")
        val STUDY_LEVEL       = stringPreferencesKey("study_level")
        val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
        val IS_DARK_THEME     = booleanPreferencesKey("is_dark_theme")
        val AVATAR_EMOJI      = stringPreferencesKey("avatar_emoji")
    }

    val userPrefs: Flow<UserPrefs> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            UserPrefs(
                userName        = p[USER_NAME] ?: "",
                examDate        = p[EXAM_DATE] ?: "",
                studyLevel      = p[STUDY_LEVEL] ?: "secondary",
                isSetupComplete = p[IS_SETUP_COMPLETE] ?: false,
                isDarkTheme     = p[IS_DARK_THEME] ?: true,
                avatarEmoji     = p[AVATAR_EMOJI] ?: "🎓"
            )
        }

    suspend fun saveUserSetup(name: String, examDate: String, level: String) {
        context.dataStore.edit { p ->
            p[USER_NAME] = name; p[EXAM_DATE] = examDate
            p[STUDY_LEVEL] = level; p[IS_SETUP_COMPLETE] = true
        }
    }
    suspend fun updateUserName(name: String)   { context.dataStore.edit { it[USER_NAME] = name } }
    suspend fun updateExamDate(date: String)   { context.dataStore.edit { it[EXAM_DATE] = date } }
    suspend fun updateStudyLevel(level: String){ context.dataStore.edit { it[STUDY_LEVEL] = level } }
    suspend fun toggleTheme(isDark: Boolean)   { context.dataStore.edit { it[IS_DARK_THEME] = isDark } }
    suspend fun updateAvatar(emoji: String)    { context.dataStore.edit { it[AVATAR_EMOJI] = emoji } }
    suspend fun resetAllData()                 { context.dataStore.edit { it.clear() } }
}
