package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class MainViewModel(application: Application) : AndroidViewModel(application) {

    init {
        TimerStateManager.initialize(application)
    }

    val totalDurationSeconds = TimerStateManager.totalDurationSeconds
    val remainingSeconds = TimerStateManager.remainingSeconds
    val isPlaying = TimerStateManager.isPlaying
    val selectedSound = TimerStateManager.selectedSound
    val isChimeRippling = TimerStateManager.isChimeRippling
    val presets = TimerStateManager.presets

    fun adjustInterval(deltaSeconds: Int) {
        TimerStateManager.adjustInterval(getApplication(), deltaSeconds)
    }

    fun setIntervalSeconds(seconds: Int) {
        TimerStateManager.setIntervalSeconds(getApplication(), seconds)
    }

    fun updatePresets(newPresets: List<Int>) {
        TimerStateManager.updatePresets(getApplication(), newPresets)
    }

    fun selectSound(type: SoundType) {
        TimerStateManager.selectSound(getApplication(), type)
    }

    fun togglePlayPause() {
        TimerStateManager.togglePlayPause(getApplication())
    }

    fun resetTimer() {
        TimerStateManager.resetTimer(getApplication())
    }
}
