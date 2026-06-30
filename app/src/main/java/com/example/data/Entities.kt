package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapters")
data class Chapter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val rangeFrom: Int,
    val rangeTo: Int,
    val daysToStudy: Int,
    val addedTime: Long = System.currentTimeMillis(),
    val isFinished: Boolean = false,
    val currentDayOfStudy: Int = 1
)

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chapterId: Int? = null,
    val question: String,
    val answer: String,
    val difficulty: String = "Normal", // Easy, Normal, Hard
    val nextReviewTime: Long = System.currentTimeMillis(),
    val intervalDays: Int = 1,
    val repetitions: Int = 0,
    val easeFactor: Float = 2.5f
)

@Entity(tableName = "quiz_history")
data class QuizHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chapterName: String,
    val examType: String, // Board, NEET, MDCAT, IELTS, CSS, Custom
    val level: String, // Easy, Normal, Hard
    val score: Int,
    val totalQuestions: Int,
    val award: String, // Gold Medal, Silver Medal, Bronze Medal, Participant Badge
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val id: Int = 1,
    val streakCount: Int = 0,
    val lastStudyDate: Long = 0,
    val totalXp: Int = 0,
    val dailyRewardCollectedDate: Long = 0
)
