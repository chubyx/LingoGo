package com.example.lingogo.games

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

data class StorySentence(
    val part1: String, // "The cat "
    val part2: String, // " on the mat."
    val correctOption: String, // "sat"
    val options: List<String> // ["sat", "run", "blue", "eat"]
)

class StoryBuilderViewModel(application: Application) : AndroidViewModel(application) {

    // Datos de ejemplo (Inglés)
    private val sentences = listOf(
        StorySentence("The cat ", " on the mat.", "sat", listOf("sat", "run", "blue", "sky")),
        StorySentence("I like to ", " apples.", "eat", listOf("eat", "flying", "car", "under")),
        StorySentence("She is ", " a book.", "reading", listOf("reading", "jump", "lamp", "fast")),
        StorySentence("The sun is ", " today.", "hot", listOf("hot", "water", "green", "run")),
        StorySentence("We are ", " to the park.", "going", listOf("going", "go", "gone", "goes"))
    )

    private var currentIndex = 0

    // --- LiveData ---
    private val _currentSentence = MutableLiveData<StorySentence>()
    val currentSentence: LiveData<StorySentence> = _currentSentence

    private val _userSelection = MutableLiveData<String?>() // Lo que el usuario eligió
    val userSelection: LiveData<String?> = _userSelection

    private val _feedback = MutableLiveData<String>()
    val feedback: LiveData<String> = _feedback

    private val _isCorrect = MutableLiveData<Boolean>()
    val isCorrect: LiveData<Boolean> = _isCorrect

    init {
        loadSentence()
    }

    private fun loadSentence() {
        _currentSentence.value = sentences[currentIndex]
        _userSelection.value = null
        _feedback.value = ""
        _isCorrect.value = false
    }

    fun selectOption(option: String) {
        _userSelection.value = option
    }

    fun checkAnswer() {
        val current = _currentSentence.value ?: return
        val selection = _userSelection.value ?: return

        if (selection == current.correctOption) {
            _isCorrect.value = true
            _feedback.value = "CORRECT" // Usaremos un ID de string en la Activity
        } else {
            _isCorrect.value = false
            _feedback.value = "INCORRECT"
        }
    }

    fun nextSentence() {
        currentIndex = (currentIndex + 1) % sentences.size
        loadSentence()
    }
}