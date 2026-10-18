package com.tilawa.app.domain.model

data class QuranVerse(
    val surah: Int,
    val ayah: Int,
    val textUthmani: String,
    val textClean: String,
    val surahName: String,
    val surahNameEn: String
)

data class VerseCandidate(
    val surah: Int,
    val ayah: Int,
    val confidence: Float,
    val surahName: String,
    val surahNameEn: String
)

enum class WordStatus {
    CORRECT,
    SLIP,
    SKIPPED,
    PENDING
}

data class WordVerdict(
    val index: Int,
    val expected: String,
    val heard: String? = null,
    val status: WordStatus = WordStatus.PENDING,
    val confidence: Float = 0f
)

enum class RecitationMode {
    TRACKING,
    CORRECTION
}

data class CorrectionState(
    val targetSurah: Int,
    val targetAyah: Int,
    val verse: QuranVerse,
    val verdicts: List<WordVerdict>,
    val currentWordIndex: Int,
    val accuracy: Int,
    val slipsCount: Int,
    val isComplete: Boolean
)
