package com.example.ui

import android.content.Intent
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.KeyboardConfigState
import com.example.model.KeyboardStateBus
import com.example.service.GlobalKeyboardOverlayService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun LiveZeroSimulatorTab(
    uiState: WinKeyUiState,
    configState: KeyboardConfigState,
    viewModel: WinKeyViewModel,
    onRequestSystemSetupTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Hero Banner Card with Generated Illustration & Instant Controls
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_win_keyboard_1791539543695),
                    contentDescription = "איור מקלדת ווינדוס קלאסית מעל מקש 0 בטלפון",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x99001438),
                                    Color(0xEE001845)
                                )
                            )
                        )
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFF008080),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "SDK 19+ (Android 4.4+) תואם",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                color = Color(0xFF0044AA),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .testTag("hero_language_badge")
                                    .clickable { KeyboardStateBus.toggleLanguage(context) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "החלף שפה",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = configState.language.badgeText,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = "מקלדת ווינדוס קלאסית (עברית / אנגלית)",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "לחיצה ארוכה על מקש 0 (${configState.longPressZeroThresholdMs}ms) מקפיצה את המקלדת בכל מקום — במסך הבית ובתוך אפליקציות!",
                                color = Color(0xFFFFE082),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Bottom quick action strip inside Hero Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = configState.lastTriggerSource,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (uiState.isInAppKeyboardVisible) {
                            viewModel.setInAppKeyboardVisible(false)
                        } else {
                            viewModel.onZeroKeyLongPressTriggered(context, "הפעלה ישירה מהכפתור הראשי")
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("toggle_keyboard_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isInAppKeyboardVisible) "הסתר מקלדת" else "הקפץ מקלדת (0)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Interactive Phone Keypad with Long-Press '0' Trigger
        PhoneKeypadZeroSimulatorCard(
            uiState = uiState,
            thresholdMs = configState.longPressZeroThresholdMs,
            viewModel = viewModel,
            onRequestSystemSetupTab = onRequestSystemSetupTab
        )

        // 3. "Works Everywhere" Scene Selector (Home Screen / In-App / Windows 95 Notepad)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "בדיקת פעולה בכל מקום במכשיר:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "הופעל ${configState.totalZeroLongPressCount} פעמים ממקש 0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SimulatorScene.entries.forEach { scene ->
                        val selected = uiState.simulatorScene == scene
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.selectSimulatorScene(scene) },
                            label = {
                                Text(
                                    text = scene.titleHe,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                val icon = when (scene) {
                                    SimulatorScene.HOME_SCREEN -> Icons.Default.Home
                                    SimulatorScene.EXTERNAL_APP -> Icons.Default.Apps
                                    SimulatorScene.WIN_NOTEPAD -> Icons.Default.Description
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scene_chip_${scene.name.lowercase()}")
                        )
                    }
                }

                // Render the active workspace scene
                when (uiState.simulatorScene) {
                    SimulatorScene.HOME_SCREEN -> HomeScreenSimulatorWorkspace(
                        searchText = uiState.homeScreenSearchText,
                        onSearchTextChange = { viewModel.updateActiveSceneTextDirect(it) },
                        onPopKeyboard = {
                            viewModel.onZeroKeyLongPressTriggered(context, "לחיצה ארוכה על 0 במסך הבית")
                        }
                    )
                    SimulatorScene.EXTERNAL_APP -> ExternalAppSimulatorWorkspace(
                        inputText = uiState.externalAppInputText,
                        messages = uiState.externalAppMessages,
                        onInputTextChange = { viewModel.updateActiveSceneTextDirect(it) },
                        onSendMessage = { viewModel.sendSimulatedChatMessage() }
                    )
                    SimulatorScene.WIN_NOTEPAD -> Win95NotepadWorkspace(
                        notepadText = uiState.notepadText,
                        onNotepadTextChange = { viewModel.updateActiveSceneTextDirect(it) },
                        onCopy = { viewModel.handleKeyboardCopy(context) },
                        onClear = { viewModel.handleKeyboardClear() },
                        onShare = {
                            if (uiState.notepadText.isNotBlank()) {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, uiState.notepadText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "שתף טקסט ממקלדת ווינדוס"))
                            }
                        }
                    )
                }
            }
        }

        // Extra bottom space when in-app keyboard is docked so user can scroll everything cleanly
        Spacer(modifier = Modifier.height(if (uiState.isInAppKeyboardVisible) 310.dp else 24.dp))
    }
}

@Composable
private fun PhoneKeypadZeroSimulatorCard(
    uiState: WinKeyUiState,
    thresholdMs: Int,
    viewModel: WinKeyViewModel,
    onRequestSystemSetupTab: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "לוח מקשי הפלאפון — החזק את מקש ה-0!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "החזק את מקש 0 לחיצה ארוכה (${thresholdMs}ms) או לחץ על 0 במקלדת המכשיר",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (uiState.dialerDigits.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { viewModel.clearDialerDigits() }
                    ) {
                        Text(
                            text = "${uiState.dialerDigits} ✕",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Progress bar when holding 0
            AnimatedVisibility(visible = uiState.isZeroKeyHolding) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF002255), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "מזהה לחיצה ארוכה על מקש 0...",
                            color = Color(0xFFFFE082),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.zeroHoldElapsedMs}ms / ${thresholdMs}ms",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { uiState.zeroHoldProgress },
                        color = Color(0xFFFFB300),
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            // Compact 4x3 Phone Keypad Grid with prominent '0' key
            val keypadRows = listOf(
                listOf("1" to "תא קולי", "2" to "אבג ABC", "3" to "דהו DEF"),
                listOf("4" to "זחט GHI", "5" to "יכל JKL", "6" to "מנס MNO"),
                listOf("7" to "עפצ PQRS", "8" to "קרת TUV", "9" to "ש WXYZ")
            )

            keypadRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (digit, sub) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable { viewModel.onShortKeypadTap(context, digit) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = digit,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = sub,
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Row: [*]  [ 0 (LONG PRESS TRIGGER) ]  [#]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(0.8f)
                        .height(56.dp)
                        .clickable { viewModel.onShortKeypadTap(context, "*") }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "✱", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // THE '0' KEY WITH REAL LONG-PRESS GESTURE DETECTOR
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (uiState.isZeroKeyHolding) Color(0xFFFFB300) else Color(0xFF003380),
                    border = BorderStroke(
                        2.dp,
                        if (uiState.isZeroKeyHolding) Color.White else Color(0xFFFFB300)
                    ),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .weight(1.9f)
                        .height(56.dp)
                        .testTag("phone_keypad_zero_button")
                        .pointerInput(thresholdMs) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                val startMs = SystemClock.uptimeMillis()
                                var triggered = false
                                val job: Job = scope.launch {
                                    while (isActive && !triggered) {
                                        val elapsed = (SystemClock.uptimeMillis() - startMs).toInt()
                                        val progress = elapsed.toFloat() / thresholdMs.toFloat()
                                        viewModel.updateZeroHoldState(true, progress, elapsed)
                                        if (elapsed >= thresholdMs) {
                                            triggered = true
                                            viewModel.onZeroKeyLongPressTriggered(
                                                context,
                                                "לחיצה ארוכה על מקש ה-0 (${thresholdMs}ms)"
                                            )
                                            break
                                        }
                                        delay(16L)
                                    }
                                }
                                waitForUpOrCancellation()
                                job.cancel()
                                if (!triggered) {
                                    viewModel.updateZeroHoldState(false, 0f, 0)
                                    viewModel.onShortKeypadTap(context, "0")
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.isZeroKeyHolding) {
                            CircularProgressIndicator(
                                progress = { uiState.zeroHoldProgress },
                                strokeWidth = 3.dp,
                                color = Color(0xFF00205B),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "0  —  החזק להקפצת מקלדת",
                                color = if (uiState.isZeroKeyHolding) Color(0xFF001B47) else Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp
                            )
                            Text(
                                text = "לחיצה ארוכה מעלה מקלדת ווינדוס בכל מקום",
                                color = if (uiState.isZeroKeyHolding) Color(0xFF003380) else Color(0xFFFFE082),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(0.8f)
                        .height(56.dp)
                        .clickable { viewModel.onShortKeypadTap(context, "#") }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "#", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // System-wide Overlay quick launcher strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        if (GlobalKeyboardOverlayService.canDrawOverlayCompat(context)) {
                            viewModel.startOrPopupGlobalOverlayService(context)
                        } else {
                            onRequestSystemSetupTab()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launch_global_overlay_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isOverlayPermissionGranted) {
                            "הפעל כעת מעל מסך הבית האמיתי (שכבה צפה)"
                        } else {
                            "הגדר הפעלה מעל מסך הבית ואפליקציות אחרות"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreenSimulatorWorkspace(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onPopKeyboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF004D4D), Color(0xFF008080), Color(0xFF003366))
                )
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "12:45",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "מסך הבית של המכשיר • החזק 0 בכל רגע להקפצת המקלדת",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp
                )
            }
            Surface(
                color = Color(0xFFFFB300),
                shape = CircleShape,
                modifier = Modifier.clickable { onPopKeyboard() }
            ) {
                Text(
                    text = "מקש 0 פעיל",
                    color = Color(0xFF001B47),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // Home screen search / quick input bar where the Windows Keyboard types directly
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            label = { Text("חיפוש / פתק מהיר במסך הבית (מוקלד ממקלדת ווינדוס)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { onSearchTextChange("") }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "נקה")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.94f), RoundedCornerShape(10.dp))
                .testTag("home_screen_search_input")
        )

        // Simulated Home Screen App Icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val apps = listOf("חייגן (0)" to "📞", "הודעות" to "💬", "דפדפן" to "🌐", "פתקים" to "📝")
            apps.forEach { (label, emoji) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPopKeyboard() }
                        .padding(6.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = emoji, fontSize = 22.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ExternalAppSimulatorWorkspace(
    inputText: String,
    messages: List<ChatMessageItem>,
    onInputTextChange: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE5DDD5))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Fake WhatsApp / Messaging Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF075E54), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "צ'אט חיצוני — מקלדת ווינדוס עובדת בתוך כל אפליקציה",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp
            )
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF80CBC4),
                modifier = Modifier.size(16.dp)
            )
        }

        // Chat bubbles
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            messages.takeLast(4).forEach { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isFromUser) Arrangement.Start else Arrangement.End
                ) {
                    Surface(
                        color = if (msg.isFromUser) Color(0xFFDCF8C6) else Color.White,
                        shape = RoundedCornerShape(10.dp),
                        shadowElevation = 1.dp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = msg.text,
                                color = Color(0xFF111B21),
                                fontSize = 13.sp
                            )
                            Text(
                                text = msg.time,
                                color = Color(0xFF667781),
                                fontSize = 9.5.sp,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }

        // Message Compose Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = { Text("הקלד הודעה בעברית/אנגלית ממקלדת ווינדוס...") },
                modifier = Modifier
                    .weight(1f)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .testTag("external_app_chat_input")
            )
            Button(
                onClick = onSendMessage,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF075E54)),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("send_chat_message_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "שלח הודעה"
                )
            }
        }
    }
}

@Composable
private fun Win95NotepadWorkspace(
    notepadText: String,
    onNotepadTextChange: (String) -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color(0xFF808080), RoundedCornerShape(4.dp))
            .background(Color(0xFFD4D0C8))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Classic Windows 95 Notepad Title Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF000080), Color(0xFF1084D0))
                    )
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📝 Notepad.exe - פנקס רשימות ווינדוס קלאסי",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = "${notepadText.length} תווים",
                color = Color(0xFFFFE082),
                fontSize = 11.sp
            )
        }

        // Classic Menu Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("קובץ (F)", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                Text("עריכה (E)", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                Text("חיפוש (S)", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                Text("עזרה (H)", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Medium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onCopy,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("העתק", fontSize = 10.5.sp)
                }
                OutlinedButton(
                    onClick = onShare,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("שתף", fontSize = 10.5.sp)
                }
                OutlinedButton(
                    onClick = onClear,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("נקה", fontSize = 10.5.sp)
                }
            }
        }

        // Sunken White Text Editor Area
        OutlinedTextField(
            value = notepadText,
            onValueChange = onNotepadTextChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.5.sp,
                color = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color.White)
                .testTag("win95_notepad_input")
        )
    }
}
