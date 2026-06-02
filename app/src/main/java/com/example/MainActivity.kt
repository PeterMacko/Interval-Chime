package com.example

import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val totalDuration by viewModel.totalDurationSeconds.collectAsStateWithLifecycle()
    val remaining by viewModel.remainingSeconds.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val selectedSound by viewModel.selectedSound.collectAsStateWithLifecycle()
    val isChimeRippling by viewModel.isChimeRippling.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Period progress fraction
    val progress = if (totalDuration > 0) remaining.toFloat() / totalDuration.toFloat() else 1f

    // Smooth progress representation
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "Progress"
    )

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isLandscape) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HeaderSection(
                    onMenuClick = { showAboutDialog = true },
                    onSettingsClick = { showSettingsDialog = true }
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT SIDE: Timer and presets vertically organized and centered in its half
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            val dialSize = 180.dp
                            ChimeVisualRipple(isActive = isChimeRippling, dialSize = dialSize)
                            CentralTimerDial(
                                progress = animatedProgress,
                                remaining = remaining,
                                onAdjustInterval = { delta -> viewModel.adjustInterval(delta) },
                                dialSize = dialSize
                            )
                        }

                        PresetPeriodRow(
                            currentDuration = totalDuration,
                            presets = presets,
                            onPresetClick = { seconds -> viewModel.setIntervalSeconds(seconds) }
                        )
                    }

                    // RIGHT SIDE: Sound config and Play buttons/controller centered in its half
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        SoundConfigDisplay(
                            selectedSound = selectedSound,
                            totalDuration = totalDuration,
                            onSelectNext = {
                                val types = SoundType.values()
                                val nextIndex = (selectedSound.ordinal + 1) % types.size
                                viewModel.selectSound(types[nextIndex])
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LandscapeControls(
                            isPlaying = isPlaying,
                            remaining = remaining,
                            totalDuration = totalDuration,
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onReset = { viewModel.resetTimer() }
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Elegant top menu header bar matching design HTML
                HeaderSection(
                    onMenuClick = { showAboutDialog = true },
                    onSettingsClick = { showSettingsDialog = true }
                )

                // Centered content column which stretches over intermediate space
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(bottom = 20.dp)
                    ) {
                        // Radiant periodic sound feedback animation
                        ChimeVisualRipple(isActive = isChimeRippling)

                        // CENTRAL TIMER DIAL
                        CentralTimerDial(
                            progress = animatedProgress,
                            remaining = remaining,
                            onAdjustInterval = { delta -> viewModel.adjustInterval(delta) }
                        )
                    }

                    PresetPeriodRow(
                        currentDuration = totalDuration,
                        presets = presets,
                        onPresetClick = { seconds -> viewModel.setIntervalSeconds(seconds) },
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Interactive sound selection subtext controller matching HTML
                    SoundConfigDisplay(
                        selectedSound = selectedSound,
                        totalDuration = totalDuration,
                        onSelectNext = {
                            val types = SoundType.values()
                            val nextIndex = (selectedSound.ordinal + 1) % types.size
                            viewModel.selectSound(types[nextIndex])
                        }
                    )
                }

                // Elegant high-fidelity footer sections for play controls and background feedback
                FooterSection(
                    isPlaying = isPlaying,
                    remaining = remaining,
                    totalDuration = totalDuration,
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onReset = { viewModel.resetTimer() }
                )
            }
        }
    }

    // Modal explaining how the app works
    if (showAboutDialog) {
        AboutThemeDialog(onDismiss = { showAboutDialog = false })
    }

    if (showSettingsDialog) {
        SettingsPresetsDialog(
            presets = presets,
            onUpdatePresets = { newPresets -> viewModel.updatePresets(newPresets) },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun HeaderSection(
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu details",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = "ZenPulse",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 0.5.sp
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { onSettingsClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings details",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun CentralTimerDial(
    progress: Float,
    remaining: Int,
    onAdjustInterval: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dialSize: androidx.compose.ui.unit.Dp = 282.dp
) {
    val isSmall = dialSize < 240.dp
    val textFontSize = if (isSmall) 34.sp else 54.sp
    val labelFontSize = if (isSmall) 9.sp else 12.sp
    val spacerHeight = if (isSmall) 2.dp else 4.dp
    val adjustmentSpacerHeight = if (isSmall) 4.dp else 14.dp
    val paddingSize = if (isSmall) 6.dp else 14.dp
    val buttonSize = if (isSmall) 32.dp else 40.dp

    Box(
        modifier = modifier
            .size(dialSize)
            .testTag("central_timer_dial")
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                spotColor = MaterialTheme.colorScheme.primary
            )
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)) // Outer border circle ring in HTML
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Double custom overlaying circles & progress track arcs in canvas
        val trackBgColor = MaterialTheme.colorScheme.secondary
        val activeLineColor = MaterialTheme.colorScheme.primary

        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = if (isSmall) 2.5.dp.toPx() else 4.dp.toPx()
            val sizeToDraw = size.minDimension - strokeWidth
            val topLeftOffset = (size.minDimension - sizeToDraw) / 2f

            // Clean background circular path representation
            drawCircle(
                color = trackBgColor.copy(alpha = 0.4f),
                radius = sizeToDraw / 2f,
                style = Stroke(width = strokeWidth)
            )

            // Dynamic progress arc representation
            drawArc(
                color = activeLineColor,
                startAngle = -90f, // Starting perfectly at the top center
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth + (if (isSmall) 1.5.dp.toPx() else 2.dp.toPx()), cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(topLeftOffset, topLeftOffset),
                size = androidx.compose.ui.geometry.Size(sizeToDraw, sizeToDraw)
            )
        }

        // Inner solid surface card of the dial
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "DURATION",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = labelFontSize),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(spacerHeight))

                // Beautiful crisp numerical digits with customizable font attributes
                Text(
                    text = formatTime(remaining),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = textFontSize,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.SansSerif
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(adjustmentSpacerHeight))

                // Fine-tuning duration adjustment keys inside the circle center
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdjustmentButton(
                        isPlus = false,
                        onClick = { onAdjustInterval(-15) },
                        buttonSize = buttonSize
                    )
                    AdjustmentButton(
                        isPlus = true,
                        onClick = { onAdjustInterval(15) },
                        buttonSize = buttonSize
                    )
                }
            }
        }
    }
}

@Composable
fun SoundConfigDisplay(
    selectedSound: SoundType,
    totalDuration: Int,
    onSelectNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Interval Sound",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Tactile Sound Select Chip mapping the notifications icon & sound name
        Surface(
            onClick = onSelectNext,
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            modifier = Modifier.testTag("sound_selector_chip")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Customized decorative chime soundwave icon
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Box(modifier = Modifier.size(2.5.dp, 8.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.size(2.5.dp, 14.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.size(2.5.dp, 10.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp)))
                }
                Text(
                    text = selectedSound.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Every $totalDuration seconds",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun FooterSection(
    isPlaying: Boolean,
    remaining: Int,
    totalDuration: Int,
    onTogglePlay: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High-contrast central Start pill action button from the HTML
        val actionText = if (isPlaying) "PAUSE TIMER" else "START TIMER"
        
        Surface(
            onClick = onTogglePlay,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(200.dp)
                .height(52.dp)
                .testTag("play_pause_button")
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    spotColor = MaterialTheme.colorScheme.primary
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isPlaying) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(4.dp, 14.dp).background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(1.dp)))
                        Box(modifier = Modifier.size(4.dp, 14.dp).background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(1.dp)))
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start control",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Contextual fine-grain Reset label
        if (remaining != totalDuration || isPlaying) {
            Text(
                text = "Reset",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .testTag("reset_button")
                    .clickable { onReset() }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(28.dp))
        }


    }
}

@Composable
fun LandscapeControls(
    isPlaying: Boolean,
    remaining: Int,
    totalDuration: Int,
    onTogglePlay: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // High-contrast Start/Pause action button
        val actionText = if (isPlaying) "PAUSE" else "START"
        Surface(
            onClick = onTogglePlay,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .width(130.dp)
                .height(44.dp)
                .testTag("play_pause_button")
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isPlaying) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(3.dp, 12.dp).background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(1.dp)))
                        Box(modifier = Modifier.size(3.dp, 12.dp).background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(1.dp)))
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start control",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Reset option
        if (remaining != totalDuration || isPlaying) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Reset",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .testTag("reset_button")
                    .clickable { onReset() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}



@Composable
fun AdjustmentButton(
    isPlus: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: androidx.compose.ui.unit.Dp = 40.dp
) {
    val tag = if (isPlus) "increment_button" else "decrement_button"
    val drawColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(buttonSize)
            .testTag(tag)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(buttonSize / 4)) {
            val strokeThickness = 2.dp.toPx()

            drawLine(
                color = drawColor,
                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                strokeWidth = strokeThickness,
                cap = StrokeCap.Round
            )

            if (isPlus) {
                drawLine(
                    color = drawColor,
                    start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                    strokeWidth = strokeThickness,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun ChimeVisualRipple(isActive: Boolean, dialSize: androidx.compose.ui.unit.Dp = 282.dp) {
    if (isActive) {
        val animationProgress = remember { Animatable(0f) }
        
        LaunchedEffect(isActive) {
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1300, easing = LinearOutSlowInEasing)
            )
        }

        val progress = animationProgress.value
        val alpha = (1f - progress).coerceIn(0f, 1f)
        val scale = 1f + progress * 1.5f

        Box(
            modifier = Modifier
                .size(dialSize)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    alpha = alpha
                )
                .border(
                    width = 4.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.7f),
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun AboutThemeDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ZenPulse Info",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Designed with Geometrical Balance, ZenPulse generates periodic background sounds of singing bowls, bells, and chimes without disrupting other media players.\n\nYou can stream music in any application (Spotify, YouTube Music, Apple Music) and hear clean ambient tones layered on top of your background audio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun activeLineColor(): Color = MaterialTheme.colorScheme.primary

@Composable
private fun inactiveLineColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

@Composable
fun PresetPeriodRow(
    currentDuration: Int,
    presets: List<Int>,
    onPresetClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        presets.forEach { seconds ->
            val isSelected = currentDuration == seconds
            val containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            }
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
            val borderStroke = if (isSelected) {
                null
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            Surface(
                onClick = { onPresetClick(seconds) },
                shape = RoundedCornerShape(16.dp),
                color = containerColor,
                contentColor = contentColor,
                border = borderStroke,
                modifier = Modifier
                    .height(38.dp)
                    .testTag("preset_${seconds}s_button")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Text(
                        text = formatPresetLabel(seconds),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

fun formatPresetLabel(seconds: Int): String {
    return if (seconds % 60 == 0) {
        "${seconds / 60} Min"
    } else if (seconds < 60) {
        "$seconds Sec"
    } else {
        val mins = seconds / 60
        val secs = seconds % 60
        "${mins}m ${secs}s"
    }
}

@Composable
fun SettingsPresetsDialog(
    presets: List<Int>,
    onUpdatePresets: (List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var minutesInput by remember { mutableStateOf("1") }
    var secondsInput by remember { mutableStateOf("0") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp).testTag("settings_presets_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Preset Buttons Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "Configure 1 to 5 interval presets:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Exiting presets list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEachIndexed { index, preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Preset ${index + 1}: ${formatPresetLabel(preset)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            if (presets.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f))
                                        .clickable {
                                            val newList = presets.filterIndexed { idx, _ -> idx != index }
                                            onUpdatePresets(newList)
                                        }
                                        .testTag("delete_preset_$index"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Delete preset",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Add Preset section
                if (presets.size < 5) {
                    Text(
                        text = "Add Custom Preset Duration",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.OutlinedTextField(
                            value = minutesInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 3) {
                                    minutesInput = input
                                    errorMessage = null
                                }
                            },
                            label = { Text("Mins", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("preset_mins_input"),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Next
                            ),
                            singleLine = true
                        )
                        
                        androidx.compose.material3.OutlinedTextField(
                            value = secondsInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 3) {
                                    secondsInput = input
                                    errorMessage = null
                                }
                            },
                            label = { Text("Secs", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("preset_secs_input"),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Done
                            ),
                            singleLine = true
                        )
                        
                        Surface(
                            onClick = {
                                val mins = minutesInput.toIntOrNull() ?: 0
                                val secs = secondsInput.toIntOrNull() ?: 0
                                val totalSecs = (mins * 60) + secs
                                if (totalSecs < 5 || totalSecs > 3600) {
                                    errorMessage = "Must be between 5s and 1h"
                                } else if (presets.contains(totalSecs)) {
                                    errorMessage = "Preset already exists"
                                } else {
                                    val newList = presets + totalSecs
                                    onUpdatePresets(newList.sorted())
                                    minutesInput = "1"
                                    secondsInput = "0"
                                    errorMessage = null
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .height(56.dp)
                                .padding(top = 4.dp)
                                .testTag("add_preset_button")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add preset duration",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    
                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }
                } else {
                    Text(
                        text = "Maximum of 5 presets reached.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Reset standard defaults options
                androidx.compose.material3.TextButton(
                    onClick = {
                        onUpdatePresets(listOf(60, 120, 300))
                        errorMessage = null
                    },
                    modifier = Modifier.testTag("reset_presets_defaults")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset presets to defaults",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset to Defaults (1, 2, 5 Min)")
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("close_presets_dialog")
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Save & Close",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
