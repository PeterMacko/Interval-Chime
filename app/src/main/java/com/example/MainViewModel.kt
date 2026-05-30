package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val soundPlayer = AmbientSoundPlayer()

    // Configuration
    private val _totalDurationSeconds = MutableStateFlow(60) // default 1 minute
    val totalDurationSeconds = _totalDurationSeconds.asStateFlow()

    // Countdown state
    private val _remainingSeconds = MutableStateFlow(60)
    val remainingSeconds = _remainingSeconds.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    // Sound customization
    private val _selectedSound = MutableStateFlow(SoundType.ZEN_BOWL)
    val selectedSound = _selectedSound.asStateFlow()

    // Visual ripple effect state
    private val _isChimeRippling = MutableStateFlow(false)
    val isChimeRippling = _isChimeRippling.asStateFlow()

    private var timerJob: Job? = null

    fun startTimer() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        
        timerJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (_isPlaying.value) {
                delay(50)
                val now = System.currentTimeMillis()
                val dt = now - lastTime
                if (dt >= 1000) {
                    val secondsElapsed = (dt / 1000).toInt()
                    lastTime += secondsElapsed * 1000
                    
                    val nextRemaining = _remainingSeconds.value - secondsElapsed
                    if (nextRemaining <= 0) {
                        // Play sound & notify visual ripple
                        triggerChimeFeedback()
                        
                        // Restart cycle cleanly
                        _remainingSeconds.value = _totalDurationSeconds.value
                    } else {
                        _remainingSeconds.value = nextRemaining
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        _isPlaying.value = false
        timerJob?.cancel()
        timerJob = null
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    fun adjustInterval(deltaSeconds: Int) {
        val current = _totalDurationSeconds.value
        // Limit period from 5 seconds to 60 minutes (3600 seconds)
        val next = (current + deltaSeconds).coerceIn(5, 3600)
        _totalDurationSeconds.value = next
        
        // If stopped, update remaining time directly to showcase changes
        if (!_isPlaying.value) {
            _remainingSeconds.value = next
        }
    }

    fun selectSound(type: SoundType) {
        _selectedSound.value = type
        // Play brief preview so user immediately knows which sound they selected
        soundPlayer.playSound(type)
    }

    fun triggerChimeFeedback() {
        soundPlayer.playSound(_selectedSound.value)
        
        // Launch dynamic ripple animation
        viewModelScope.launch {
            _isChimeRippling.value = true
            delay(1500) // Duration matching the ripple visual wave fading Out
            _isChimeRippling.value = false
        }
    }

    fun resetTimer() {
        pauseTimer()
        _remainingSeconds.value = _totalDurationSeconds.value
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
