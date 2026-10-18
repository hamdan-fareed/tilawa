package com.tilawa.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tilawa.app.data.QuranRepository
import com.tilawa.app.domain.model.CorrectionState
import com.tilawa.app.domain.model.QuranVerse
import com.tilawa.app.domain.model.RecitationMode
import com.tilawa.app.domain.model.VerseCandidate
import com.tilawa.app.domain.model.WordStatus
import com.tilawa.app.domain.model.WordVerdict
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TilawaViewModel(
    private val repository: QuranRepository = QuranRepository()
) : ViewModel() {

    private val _mode = MutableStateFlow(RecitationMode.TRACKING)
    val mode: StateFlow<RecitationMode> = _mode.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _currentVerse = MutableStateFlow(repository.getVerse(1, 1)!!)
    val currentVerse: StateFlow<QuranVerse> = _currentVerse.asStateFlow()

    private val _currentWordIndex = MutableStateFlow(0)
    val currentWordIndex: StateFlow<Int> = _currentWordIndex.asStateFlow()

    private val _matchedIndices = MutableStateFlow<List<Int>>(emptyList())
    val matchedIndices: StateFlow<List<Int>> = _matchedIndices.asStateFlow()

    private val _candidates = MutableStateFlow<List<VerseCandidate>>(emptyList())
    val candidates: StateFlow<List<VerseCandidate>> = _candidates.asStateFlow()

    private val _correctionState = MutableStateFlow<CorrectionState?>(null)
    val correctionState: StateFlow<CorrectionState?> = _correctionState.asStateFlow()

    private var simulationJob: Job? = null

    init {
        setupCorrectionForVerse(_currentVerse.value)
    }

    fun setMode(newMode: RecitationMode) {
        _mode.value = newMode
        if (newMode == RecitationMode.CORRECTION) {
            setupCorrectionForVerse(_currentVerse.value)
        }
    }

    fun selectVerse(surah: Int, ayah: Int) {
        val v = repository.getVerse(surah, ayah) ?: return
        _currentVerse.value = v
        _currentWordIndex.value = 0
        _matchedIndices.value = emptyList()
        setupCorrectionForVerse(v)
    }

    private fun setupCorrectionForVerse(verse: QuranVerse) {
        val words = verse.textUthmani.split(" ").filter { it.isNotBlank() }
        val verdicts = words.mapIndexed { index, w ->
            WordVerdict(index = index, expected = w, status = WordStatus.PENDING)
        }
        _correctionState.value = CorrectionState(
            targetSurah = verse.surah,
            targetAyah = verse.ayah,
            verse = verse,
            verdicts = verdicts,
            currentWordIndex = 0,
            accuracy = 100,
            slipsCount = 0,
            isComplete = false
        )
    }

    fun startListening() {
        _isListening.value = true
        // Start live word progression / simulation
        simulateRecitation(_currentVerse.value.surah, _currentVerse.value.ayah)
    }

    fun stopListening() {
        _isListening.value = false
        simulationJob?.cancel()
    }

    fun reset() {
        stopListening()
        _currentWordIndex.value = 0
        _matchedIndices.value = emptyList()
        _candidates.value = emptyList()
        setupCorrectionForVerse(_currentVerse.value)
    }

    fun simulateRecitation(surah: Int, ayah: Int) {
        val v = repository.getVerse(surah, ayah) ?: return
        _currentVerse.value = v
        _isListening.value = true
        setupCorrectionForVerse(v)

        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            val words = v.textUthmani.split(" ").filter { it.isNotBlank() }
            _candidates.value = listOf(
                VerseCandidate(v.surah, v.ayah, 0.98f, v.surahName, v.surahNameEn)
            )

            for (i in words.indices) {
                delay(650)
                if (!_isListening.value) break
                _currentWordIndex.value = i
                _matchedIndices.value = (0..i).toList()

                // Update correction verdicts
                val currentVerdicts = _correctionState.value?.verdicts?.toMutableList() ?: mutableListOf()
                if (i < currentVerdicts.size) {
                    currentVerdicts[i] = currentVerdicts[i].copy(status = WordStatus.CORRECT, confidence = 0.99f)
                    val correct = currentVerdicts.count { it.status == WordStatus.CORRECT }
                    _correctionState.value = _correctionState.value?.copy(
                        verdicts = currentVerdicts,
                        currentWordIndex = i + 1,
                        accuracy = (correct * 100) / words.size,
                        isComplete = (i == words.size - 1)
                    )
                }
            }
            _isListening.value = false
        }
    }

    fun resolveWord(index: Int, action: String) {
        val current = _correctionState.value ?: return
        val updated = current.verdicts.toMutableList()
        if (index < updated.size) {
            val status = when (action) {
                "accept" -> WordStatus.CORRECT
                "skip" -> WordStatus.SKIPPED
                else -> WordStatus.PENDING
            }
            updated[index] = updated[index].copy(status = status)
            val slips = updated.count { it.status == WordStatus.SLIP }
            val correct = updated.count { it.status == WordStatus.CORRECT }
            _correctionState.value = current.copy(
                verdicts = updated,
                slipsCount = slips,
                accuracy = (correct * 100) / updated.size
            )
        }
    }
}
