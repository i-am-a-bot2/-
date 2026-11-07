package com.example

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.keyboard.ClassicWindowsKeyboardView
import com.example.keyboard.FloatingZeroKeyBadgeView
import com.example.keyboard.WinKeyboardActionListener
import com.example.model.KeyType
import com.example.model.KeyboardConfigState
import com.example.model.KeyboardStateBus
import com.example.ui.ApkAndSdk19GuideTab
import com.example.ui.AppMainTab
import com.example.ui.LiveZeroSimulatorTab
import com.example.ui.SettingsAndSkinsTab
import com.example.ui.SystemWideSetupTab
import com.example.ui.WinKeyUiState
import com.example.ui.WinKeyViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val viewModel: WinKeyViewModel by viewModels()
    private val hwKeyHandler = Handler(Looper.getMainLooper())
    private var isHardwareZeroDown = false
    private var hwZeroDownStartMs = 0L
    private var hwZeroTriggered = false

    private val hwZeroProgressRunnable = object : Runnable {
        override fun run() {
            if (!isHardwareZeroDown) return
            val threshold = KeyboardStateBus.state.value.longPressZeroThresholdMs.coerceAtLeast(250)
            val elapsed = (SystemClock.uptimeMillis() - hwZeroDownStartMs).toInt()
            val progress = (elapsed.toFloat() / threshold.toFloat()).coerceIn(0f, 1f)
            viewModel.updateZeroHoldState(true, progress, elapsed)
            if (elapsed >= threshold) {
                hwZeroTriggered = true
                isHardwareZeroDown = false
                viewModel.onZeroKeyLongPressTriggered(
                    this@MainActivity,
                    "לחיצה ארוכה על מקש 0 פיזי של המכשיר (${threshold}ms)"
                )
            } else {
                hwKeyHandler.postDelayed(this, 16L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KeyboardStateBus.loadFromPrefs(this)
        viewModel.refreshSystemStatuses(this)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val configState by KeyboardStateBus.state.collectAsStateWithLifecycle()

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    WinKeyMainScreen(
                        uiState = uiState,
                        configState = configState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSystemStatuses(this)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_0 || keyCode == KeyEvent.KEYCODE_NUMPAD_0) {
            if (event?.repeatCount == 0 && !isHardwareZeroDown) {
                isHardwareZeroDown = true
                hwZeroTriggered = false
                hwZeroDownStartMs = SystemClock.uptimeMillis()
                hwKeyHandler.removeCallbacks(hwZeroProgressRunnable)
                hwKeyHandler.post(hwZeroProgressRunnable)
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_0 || keyCode == KeyEvent.KEYCODE_NUMPAD_0) {
            isHardwareZeroDown = false
            hwKeyHandler.removeCallbacks(hwZeroProgressRunnable)
            if (!hwZeroTriggered) {
                viewModel.updateZeroHoldState(false, 0f, 0)
                viewModel.onShortKeypadTap(this, "0")
            }
            hwZeroTriggered = false
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WinKeyMainScreen(
    uiState: WinKeyUiState,
    configState: KeyboardConfigState,
    viewModel: WinKeyViewModel
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    if (uiState.selectedTab != AppMainTab.SIMULATOR) {
        BackHandler {
            viewModel.selectTab(AppMainTab.SIMULATOR)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Column {
                            Text(
                                text = "WinKey Classic HE/EN",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "מקלדת ווינדוס קלאסית • לחיצה ארוכה על 0 בכל מקום",
                                color = Color(0xFFFFE082),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        color = if (uiState.isInAppKeyboardVisible) Color(0xFF00C853) else Color(0xFFFFB300),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .testTag("top_bar_zero_status_chip")
                    ) {
                        Text(
                            text = if (uiState.isInAppKeyboardVisible) "מקלדת פעילה" else "החזק 0 להקפצה",
                            color = Color(0xFF001B47),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                AppMainTab.entries.forEach { tab ->
                    val icon = when (tab) {
                        AppMainTab.SIMULATOR -> Icons.Default.PhoneAndroid
                        AppMainTab.SYSTEM_SETUP -> Icons.Default.Layers
                        AppMainTab.SETTINGS -> Icons.Default.Settings
                        AppMainTab.APK_GUIDE -> Icons.Default.Download
                    }
                    NavigationBarItem(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        icon = { Icon(icon, contentDescription = tab.titleHe) },
                        label = {
                            Text(
                                text = tab.titleHe,
                                fontSize = 10.5.sp,
                                fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                AppMainTab.SIMULATOR -> LiveZeroSimulatorTab(
                    uiState = uiState,
                    configState = configState,
                    viewModel = viewModel,
                    onRequestSystemSetupTab = { viewModel.selectTab(AppMainTab.SYSTEM_SETUP) }
                )
                AppMainTab.SYSTEM_SETUP -> SystemWideSetupTab(
                    uiState = uiState,
                    viewModel = viewModel
                )
                AppMainTab.SETTINGS -> SettingsAndSkinsTab(
                    configState = configState
                )
                AppMainTab.APK_GUIDE -> ApkAndSdk19GuideTab()
            }

            // Floating '0' Key Badge when the keyboard is minimized (and showFloatingZeroBadge is enabled)
            if (!uiState.isInAppKeyboardVisible && configState.showFloatingZeroBadge) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    AndroidView(
                        factory = { ctx ->
                            FloatingZeroKeyBadgeView(ctx).apply {
                                onLongPressZeroTriggered = {
                                    viewModel.onZeroKeyLongPressTriggered(
                                        ctx,
                                        "לחיצה ארוכה על מקש 0 הצף בקצה המסך"
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                            .size(62.dp)
                            .testTag("floating_zero_badge_view")
                    )
                }
            }

            // Classic Windows HE/EN Keyboard Docked / Draggable Overlay
            AnimatedVisibility(
                visible = uiState.isInAppKeyboardVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = with(density) { -uiState.floatingWindowOffsetYDp.dp.toPx().roundToInt() }
                        )
                    }
            ) {
                // Keep keyboard physical layout LTR so QWERTY / קראטוןםפ matches real PC keyboards
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    val activeText = viewModel.getActiveSceneText()
                    val sceneLabel = uiState.simulatorScene.titleHe

                    AndroidView(
                        factory = { ctx ->
                            ClassicWindowsKeyboardView(ctx).apply {
                                showQuickScratchpadBar = true
                                actionListener = object : WinKeyboardActionListener {
                                    override fun onCommitText(text: String) {
                                        viewModel.handleKeyboardCommitText(text)
                                    }

                                    override fun onBackspace() {
                                        viewModel.handleKeyboardBackspace()
                                    }

                                    override fun onEnter() {
                                        viewModel.handleKeyboardEnter()
                                    }

                                    override fun onArrowKey(keyType: KeyType) {
                                        viewModel.handleArrowKey(keyType)
                                    }

                                    override fun onCopyRequested() {
                                        viewModel.handleKeyboardCopy(context)
                                    }

                                    override fun onPasteRequested() {
                                        viewModel.handleKeyboardPaste(context)
                                    }

                                    override fun onClearRequested() {
                                        viewModel.handleKeyboardClear()
                                    }

                                    override fun onCloseKeyboardRequested() {
                                        viewModel.setInAppKeyboardVisible(false)
                                    }

                                    override fun onMinimizeToZeroBadgeRequested() {
                                        viewModel.setInAppKeyboardVisible(false)
                                    }

                                    override fun onDragWindowDelta(dx: Float, dy: Float) {
                                        val dyDp = dy / ctx.resources.displayMetrics.density
                                        viewModel.adjustFloatingKeyboardOffset(dyDp)
                                    }
                                }
                            }
                        },
                        update = { view ->
                            view.targetContextLabel = "מקליד אל: $sceneLabel"
                            view.scratchpadPreviewText = activeText
                            view.requestLayout()
                            view.invalidate()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(configState.skin.chassisColor))
                            .testTag("classic_windows_keyboard_view")
                    )
                }
            }
        }
    }
}
