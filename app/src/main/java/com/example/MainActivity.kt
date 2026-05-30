package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    val totalDuration by viewModel.totalDurationSeconds.collectAsStateWithLifecycle()
    val remaining by viewModel.remainingSeconds.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val selectedSound by viewModel.selectedSound.collectAsStateWithLifecycle()
    val isChimeRippling by viewModel.isChimeRippling.collectAsStateWithLifecycle()

    var showAboutDialog by remember { mutableStateOf(false) }

    // Period progress fraction
    val progress = if (totalDuration > 0) remaining.toFloat() / totalDuration.toFloat() else 1f

    // Smooth progress representation
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "Progress"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
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
                onSettingsClick = { showAboutDialog = true }
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
                    modifier = Modifier.padding(bottom = 28.dp)
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

    // Modal explaining how the app works
    if (showAboutDialog) {
        AboutThemeDialog(onDismiss = { showAboutDialog = false })
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
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(282.dp)
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
            val strokeWidth = 4.dp.toPx()
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
                startAngle = -135f, // Custom rotated aesthetic for Geometric Balance
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth + 2.dp.toPx(), cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(topLeftOffset, topLeftOffset),
                size = androidx.compose.ui.geometry.Size(sizeToDraw, sizeToDraw)
            )
        }

        // Inner solid surface card of the dial
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
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
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Beautiful crisp numerical digits with customizable font attributes
                Text(
                    text = formatTime(remaining),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.SansSerif
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Fine-tuning duration adjustment keys inside the circle center
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdjustmentButton(
                        isPlus = false,
                        onClick = { onAdjustInterval(-15) }
                    )
                    AdjustmentButton(
                        isPlus = true,
                        onClick = { onAdjustInterval(15) }
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

        // "Playing in background" helper info block matching the HTML design perfectly
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Integrated musical details
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val activeColorVal = MaterialTheme.colorScheme.primary
                        // Sound note vector lines
                        Canvas(modifier = Modifier.size(16.dp)) {
                            drawCircle(color = activeColorVal, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(4.dp.toPx(), 12.dp.toPx()))
                            drawLine(
                                color = activeColorVal,
                                start = androidx.compose.ui.geometry.Offset(7.dp.toPx(), 12.dp.toPx()),
                                end = androidx.compose.ui.geometry.Offset(7.dp.toPx(), 2.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                            drawLine(
                                color = activeColorVal,
                                start = androidx.compose.ui.geometry.Offset(7.dp.toPx(), 2.dp.toPx()),
                                end = androidx.compose.ui.geometry.Offset(14.dp.toPx(), 4.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Overlay Mode Active",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        Text(
                            text = "Music players will keep playing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                // Decorative equalizer visualizer representing harmonious sound playing
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(2.dp, 10.dp).background(MaterialTheme.colorScheme.primary))
                    Box(modifier = Modifier.size(2.dp, 16.dp).background(MaterialTheme.colorScheme.primary))
                    Box(modifier = Modifier.size(2.dp, 6.dp).background(MaterialTheme.colorScheme.primary))
                    Box(modifier = Modifier.size(2.dp, 12.dp).background(MaterialTheme.colorScheme.primary))
                }
            }
        }
    }
}



@Composable
fun AdjustmentButton(
    isPlus: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tag = if (isPlus) "increment_button" else "decrement_button"
    val drawColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(40.dp)
            .testTag(tag)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
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
fun ChimeVisualRipple(isActive: Boolean) {
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
                .size(282.dp)
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
