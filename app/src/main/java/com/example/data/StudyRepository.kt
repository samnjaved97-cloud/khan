package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.*

class StudyRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val chapterDao = db.chapterDao()
    private val flashcardDao = db.flashcardDao()
    private val quizHistoryDao = db.quizHistoryDao()
    private val userProgressDao = db.userProgressDao()

    // Expose local database flows
    val allChapters: Flow<List<Chapter>> = chapterDao.getAllChapters()
    val allFlashcards: Flow<List<Flashcard>> = flashcardDao.getAllFlashcards()
    val allQuizHistory: Flow<List<QuizHistory>> = quizHistoryDao.getAllQuizHistory()
    val userProgress: Flow<UserProgress?> = userProgressDao.getUserProgress()

    // Initialize Progress
    suspend fun checkAndInitProgress() = withContext(Dispatchers.IO) {
        val existing = userProgressDao.getUserProgressSync()
        if (existing == null) {
            userProgressDao.insertUserProgress(
                UserProgress(id = 1, streakCount = 0, lastStudyDate = 0, totalXp = 0)
            )
        }
    }

    // Chapters
    suspend fun insertChapter(chapter: Chapter) = withContext(Dispatchers.IO) {
        chapterDao.insertChapter(chapter)
        addXp(100) // Reward 100 XP for adding a new course study plan
        syncToFirebase()
    }

    suspend fun updateChapter(chapter: Chapter) = withContext(Dispatchers.IO) {
        chapterDao.updateChapter(chapter)
        syncToFirebase()
    }

    suspend fun deleteChapter(chapter: Chapter) = withContext(Dispatchers.IO) {
        chapterDao.deleteChapter(chapter)
        syncToFirebase()
    }

    suspend fun finishChapter(chapterId: Int) = withContext(Dispatchers.IO) {
        val chapter = chapterDao.getChapterById(chapterId)
        if (chapter != null && !chapter.isFinished) {
            chapterDao.updateChapter(chapter.copy(isFinished = true))
            addXp(300) // Huge 300 XP for finishing a whole chapter!
            updateStreakOnActivity()
            syncToFirebase()
        }
    }

    // Flashcards with SuperMemo-2 Spaced Repetition Algorithm
    suspend fun insertFlashcard(flashcard: Flashcard) = withContext(Dispatchers.IO) {
        flashcardDao.insertFlashcard(flashcard)
        addXp(10) // 10 XP for creating a flashcard
        syncToFirebase()
    }

    suspend fun deleteFlashcard(flashcard: Flashcard) = withContext(Dispatchers.IO) {
        flashcardDao.deleteFlashcard(flashcard)
    }

    // Review flashcard and update its spaced repetition values
    suspend fun reviewFlashcard(flashcard: Flashcard, responseQuality: Int) = withContext(Dispatchers.IO) {
        // responseQuality: 0-5. 0=complete blackout, 5=perfect response
        val now = System.currentTimeMillis()
        var reps = flashcard.repetitions
        var interval = flashcard.intervalDays
        var ef = flashcard.easeFactor

        if (responseQuality >= 3) {
            if (reps == 0) {
                interval = 1
            } else if (reps == 1) {
                interval = 6
            } else {
                interval = (interval * ef).toInt()
            }
            reps++
        } else {
            reps = 0
            interval = 1
        }

        // Adjust Ease Factor
        ef = ef + (0.1f - (5 - responseQuality) * (0.08f + (5 - responseQuality) * 0.02f))
        if (ef < 1.3f) ef = 1.3f

        val nextReview = now + (interval * 24 * 60 * 60 * 1000L) // in days

        val updated = flashcard.copy(
            repetitions = reps,
            intervalDays = interval,
            easeFactor = ef,
            nextReviewTime = nextReview,
            difficulty = when {
                responseQuality >= 4 -> "Easy"
                responseQuality >= 2 -> "Normal"
                else -> "Hard"
            }
        )

        flashcardDao.updateFlashcard(updated)
        addXp(20) // 20 XP for reviewing a card
        updateStreakOnActivity()
        syncToFirebase()
    }

    // Quiz History & Awards distribution
    suspend fun saveQuizResult(
        chapterName: String,
        examType: String,
        level: String,
        score: Int,
        totalQuestions: Int
    ) = withContext(Dispatchers.IO) {
        val percentage = (score.toFloat() / totalQuestions * 100).toInt()
        val award = when {
            percentage == 100 && level == "Hard" -> "🏆 Elite Platinum Trophy"
            percentage >= 90 && level == "Hard" -> "🥇 Gold Scholar Medal"
            percentage >= 90 -> "🥇 Gold Star Medal"
            percentage >= 75 && level == "Normal" -> "🥈 Silver Scholar Medal"
            percentage >= 75 -> "🥈 Silver Star Medal"
            percentage >= 50 -> "🥉 Bronze Milestone Medal"
            else -> "🎖️ Chapter Participant Ribbon"
        }

        val history = QuizHistory(
            chapterName = chapterName,
            examType = examType,
            level = level,
            score = score,
            totalQuestions = totalQuestions,
            award = award
        )
        quizHistoryDao.insertQuizHistory(history)

        // Give XP according to level and accuracy
        val baseMultiplier = when (level) {
            "Hard" -> 30
            "Normal" -> 20
            else -> 10
        }
        val xpEarned = (score * baseMultiplier) + (if (percentage == 100) 100 else 0)
        addXp(xpEarned)
        updateStreakOnActivity()
        syncToFirebase()
    }

    // Gamification Mechanics
    suspend fun addXp(amount: Int) = withContext(Dispatchers.IO) {
        val current = userProgressDao.getUserProgressSync() ?: UserProgress(id = 1)
        val updated = current.copy(totalXp = current.totalXp + amount)
        userProgressDao.updateUserProgress(updated)
    }

    suspend fun claimDailyReward(): Boolean = withContext(Dispatchers.IO) {
        val current = userProgressDao.getUserProgressSync() ?: UserProgress(id = 1)
        val now = System.currentTimeMillis()
        if (isSameDay(current.dailyRewardCollectedDate, now)) {
            false // Already collected today
        } else {
            val updated = current.copy(
                totalXp = current.totalXp + 150, // 150 XP Reward!
                dailyRewardCollectedDate = now
            )
            userProgressDao.updateUserProgress(updated)
            updateStreakOnActivity()
            true
        }
    }

    private suspend fun updateStreakOnActivity() {
        val current = userProgressDao.getUserProgressSync() ?: UserProgress(id = 1)
        val now = System.currentTimeMillis()
        val last = current.lastStudyDate

        if (last == 0L) {
            userProgressDao.updateUserProgress(current.copy(streakCount = 1, lastStudyDate = now))
        } else {
            if (isSameDay(last, now)) {
                // Streak already counted for today
            } else if (isYesterday(last, now)) {
                // Increment streak!
                userProgressDao.updateUserProgress(current.copy(streakCount = current.streakCount + 1, lastStudyDate = now))
            } else {
                // Reset streak since a day was missed
                userProgressDao.updateUserProgress(current.copy(streakCount = 1, lastStudyDate = now))
            }
        }
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        if (t1 == 0L || t2 == 0L) return false
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        cal1.add(Calendar.DAY_OF_YEAR, 1)
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    // --- Firebase Sync Service ---
    suspend fun syncToFirebase() = withContext(Dispatchers.IO) {
        try {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            if (user != null) {
                val dbFs = FirebaseFirestore.getInstance()
                val uid = user.uid

                val localProgress = userProgressDao.getUserProgressSync()
                val localChapters = chapterDao.getAllChapters().firstOrNull() ?: emptyList()
                val localQuizHistory = quizHistoryDao.getAllQuizHistory().firstOrNull() ?: emptyList()

                val data = hashMapOf(
                    "progress" to localProgress,
                    "chaptersCount" to localChapters.size,
                    "quizHistoryCount" to localQuizHistory.size,
                    "lastSync" to System.currentTimeMillis()
                )

                dbFs.collection("users").document(uid).set(data)
                    .addOnSuccessListener {
                        Log.d("FirebaseSync", "Successfully synced student progress to Firestore!")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseSync", "Error syncing to Firestore: ${e.message}")
                    }
            }
        } catch (e: Exception) {
            Log.d("FirebaseSync", "Firebase is not initialized or missing config. Running in offline-first mode.")
        }
    }
}
