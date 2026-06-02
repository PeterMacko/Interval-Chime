package com.example

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object TimerStateManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var rippleJob: Job? = null
    private val soundPlayer = AmbientSoundPlayer()

    private lateinit var context: Context
    private val _totalDurationSeconds = MutableStateFlow(60)
    val totalDurationSeconds = _totalDurationSeconds.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(60)
    val remainingSeconds = _remainingSeconds.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _selectedSound = MutableStateFlow(SoundType.ZEN_BOWL)
    val selectedSound = _selectedSound.asStateFlow()

    private val _isChimeRippling = MutableStateFlow(false)
    val isChimeRippling = _isChimeRippling.asStateFlow()

    private val _presets = MutableStateFlow<List<Int>>(listOf(60, 120, 300))
    val presets = _presets.asStateFlow()

    private var isInitialized = false

    fun initialize(appContext: Context) {
        if (isInitialized) return
        this.context = appContext.applicationContext
        val sharedPrefs = context.getSharedPreferences("interval_chime_prefs", Context.MODE_PRIVATE)
        val savedDuration = sharedPrefs.getInt("total_duration_seconds", 60)
        _totalDurationSeconds.value = savedDuration
        _remainingSeconds.value = savedDuration
        
        val savedSoundStr = sharedPrefs.getString("selected_sound", SoundType.ZEN_BOWL.name) ?: SoundType.ZEN_BOWL.name
        _selectedSound.value = try {
            SoundType.valueOf(savedSoundStr)
        } catch (e: Exception) {
            SoundType.ZEN_BOWL
        }

        val savedPresetsStr = sharedPrefs.getString("preset_periods", "60,120,300") ?: "60,120,300"
        val loadedPresets = savedPresetsStr.split(",").mapNotNull { it.toIntOrNull() }
        if (loadedPresets.isNotEmpty()) {
            _presets.value = loadedPresets
        } else {
            _presets.value = listOf(60, 120, 300)
        }

        isInitialized = true
    }

    fun updatePresets(ctx: Context, newPresets: List<Int>) {
        val appContext = ctx.applicationContext
        initialize(appContext)
        val filtered = newPresets.map { it.coerceIn(5, 3600) }.take(5)
        _presets.value = filtered
        
        val sharedPrefs = appContext.getSharedPreferences("interval_chime_prefs", Context.MODE_PRIVATE)
        val prStr = filtered.joinToString(",")
        sharedPrefs.edit().putString("preset_periods", prStr).apply()
    }

    fun startTimer(ctx: Context) {
        val appContext = ctx.applicationContext
        initialize(appContext)
        _isPlaying.value = true
        
        val serviceIntent = Intent(appContext, IntervalTimerService::class.java).apply {
            action = IntervalTimerService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(serviceIntent)
        } else {
            appContext.startService(serviceIntent)
        }
    }

    fun pauseTimer(ctx: Context) {
        _isPlaying.value = false
        val serviceIntent = Intent(ctx.applicationContext, IntervalTimerService::class.java).apply {
            action = IntervalTimerService.ACTION_PAUSE
        }
        ctx.applicationContext.startService(serviceIntent)
    }

    fun setPlayingState(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun togglePlayPause(ctx: Context) {
        if (_isPlaying.value) {
            pauseTimer(ctx)
        } else {
            startTimer(ctx)
        }
    }

    fun adjustInterval(ctx: Context, deltaSeconds: Int) {
        val appContext = ctx.applicationContext
        initialize(appContext)
        val current = _totalDurationSeconds.value
        val next = (current + deltaSeconds).coerceIn(5, 3600)
        _totalDurationSeconds.value = next
        
        val sharedPrefs = appContext.getSharedPreferences("interval_chime_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("total_duration_seconds", next).apply()

        if (!_isPlaying.value) {
            _remainingSeconds.value = next
        }
    }

    fun setIntervalSeconds(ctx: Context, seconds: Int) {
        val appContext = ctx.applicationContext
        initialize(appContext)
        val value = seconds.coerceIn(5, 3600)
        _totalDurationSeconds.value = value
        
        val sharedPrefs = appContext.getSharedPreferences("interval_chime_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("total_duration_seconds", value).apply()

        if (!_isPlaying.value || _remainingSeconds.value > value) {
            _remainingSeconds.value = value
        }
    }

    fun selectSound(ctx: Context, type: SoundType) {
        val appContext = ctx.applicationContext
        initialize(appContext)
        _selectedSound.value = type
        
        val sharedPrefs = appContext.getSharedPreferences("interval_chime_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("selected_sound", type.name).apply()
        
        // Play audio preview on demand
        soundPlayer.playSound(type)
    }

    fun triggerChimeFeedback() {
        soundPlayer.playSound(_selectedSound.value)
        rippleJob?.cancel()
        rippleJob = scope.launch {
            _isChimeRippling.value = true
            delay(1500)
            _isChimeRippling.value = false
        }
    }

    fun resetTimer(ctx: Context) {
        pauseTimer(ctx)
        _remainingSeconds.value = _totalDurationSeconds.value
    }

    fun updateRemainingSeconds(seconds: Int) {
        _remainingSeconds.value = seconds
    }
}
