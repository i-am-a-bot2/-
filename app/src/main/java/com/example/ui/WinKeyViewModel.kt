package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.example.keyboard.SoundAndHapticHelper
import com.example.model.KeyType
import com.example.model.KeyboardStateBus
import com.example.service.GlobalKeyboardOverlayService
import com.example.service.ZeroKeyAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppMainTab(val titleHe: String) {
    SIMULATOR("הדמיה ומקש 0"),
    SYSTEM_SETUP("הפעלה בכל המכשיר"),
    SETTINGS("עיצוב והגדרות"),
    APK_GUIDE("הורדת APK ו-SDK 19")
}

enum class SimulatorScene(val titleHe: String, val subtitleHe: String) {
    HOME_SCREEN("מסך הבית", "הדמיית לחיצה ארוכה על 0 במסך הבית של הפלאפון"),
    EXTERNAL_APP("באמצע אפליקציות", "הדמיית הקלדה בוואטסאפ / דפדפן / הודעות"),
    WIN_NOTEPAD("פנקס רשימות Win95", "עורך טקסט מלא של ווינדוס בעברית ובאנגלית")
}

data class ChatMessageItem(
    val id: Long,
    val text: String,
    val time: String,
    val isFromUser: Boolean = true
)

data class WinKeyUiState(
    val selectedTab: AppMainTab = AppMainTab.SIMULATOR,
    val simulatorScene: SimulatorScene = SimulatorScene.HOME_SCREEN,
    val isInAppKeyboardVisible: Boolean = true,
    val isZeroKeyHolding: Boolean = false,
    val zeroHoldProgress: Float = 0f,
    val zeroHoldElapsedMs: Int = 0,
    val dialerDigits: String = "",
    val homeScreenSearchText: String = "שלום מעל מסך הבית! ",
    val externalAppInputText: String = "",
    val externalAppMessages: List<ChatMessageItem> = listOf(
        ChatMessageItem(
            1L,
            "היי! לחץ לחיצה ארוכה על מקש ה-0 כדי להקפיץ את המקלדת הקלאסית של ווינדוס ולהשיב לי.",
            "10:14",
            isFromUser = false
        ),
        ChatMessageItem(
            2L,
            "מעולה, המקלדת עולה בכל מקום — גם במסך הבית וגם בתוך אפליקציות!",
            "10:15",
            isFromUser = true
        )
    ),
    val notepadText: String = "בס\"ד\nמקלדת ווינדוס קלאסית (עברית / English)\n-------------------------------------\nלחץ לחיצה ארוכה על מקש 0 בכל מקום במכשיר כדי להעלות או להסתיר את המקלדת.\nלמעבר שפה: לחץ על [HE/EN] או על Alt + Shift כמו במחשב!\n",
    val floatingWindowOffsetYDp: Float = 0f,
    val isAccessibilityEnabled: Boolean = false,
    val isOverlayPermissionGranted: Boolean = false,
    val isOverlayServiceRunning: Boolean = false,
    val isImeEnabled: Boolean = false
)

class WinKeyViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WinKeyUiState())
    val uiState: StateFlow<WinKeyUiState> = _uiState.asStateFlow()

    fun refreshSystemStatuses(context: Context) {
        val overlayGranted = GlobalKeyboardOverlayService.canDrawOverlayCompat(context)
        val accessibilityEnabled = isAccessibilityServiceEnabled(context)
        val imeEnabled = isInputMethodEnabled(context)
        _uiState.value = _uiState.value.copy(
            isAccessibilityEnabled = accessibilityEnabled,
            isOverlayPermissionGranted = overlayGranted,
            isOverlayServiceRunning = GlobalKeyboardOverlayService.isRunning ||
                ZeroKeyAccessibilityService.instance != null,
            isImeEnabled = imeEnabled
        )
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabledServices.contains(context.packageName) &&
                enabledServices.contains("ZeroKeyAccessibilityService")
        } catch (_: Throwable) {
            false
        }
    }

    private fun isInputMethodEnabled(context: Context): Boolean {
        return try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.enabledInputMethodList?.any { it.packageName == context.packageName } == true
        } catch (_: Throwable) {
            false
        }
    }

    fun selectTab(tab: AppMainTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun selectSimulatorScene(scene: SimulatorScene) {
        _uiState.value = _uiState.value.copy(simulatorScene = scene)
    }

    fun updateZeroHoldState(holding: Boolean, progress: Float, elapsedMs: Int) {
        _uiState.value = _uiState.value.copy(
            isZeroKeyHolding = holding,
            zeroHoldProgress = progress.coerceIn(0f, 1f),
            zeroHoldElapsedMs = elapsedMs
        )
    }

    fun onZeroKeyLongPressTriggered(context: Context, sourceLabel: String) {
        SoundAndHapticHelper.performZeroKeyPopUpFeedback(context)
        KeyboardStateBus.notifyZeroKeyLongPressTriggered(context, sourceLabel)
        _uiState.value = _uiState.value.copy(
            isInAppKeyboardVisible = true,
            isZeroKeyHolding = false,
            zeroHoldProgress = 0f,
            zeroHoldElapsedMs = 0
        )
    }

    fun setInAppKeyboardVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isInAppKeyboardVisible = visible)
        KeyboardStateBus.setOverlayKeyboardVisible(visible)
    }

    fun onShortKeypadTap(context: Context, keyLabel: String) {
        SoundAndHapticHelper.performKeyTapFeedback(context, false)
        _uiState.value = _uiState.value.copy(
            dialerDigits = (_uiState.value.dialerDigits + keyLabel).takeLast(18)
        )
    }

    fun clearDialerDigits() {
        _uiState.value = _uiState.value.copy(dialerDigits = "")
    }

    fun adjustFloatingKeyboardOffset(deltaYDp: Float) {
        val next = (_uiState.value.floatingWindowOffsetYDp - deltaYDp).coerceIn(0f, 240f)
        _uiState.value = _uiState.value.copy(floatingWindowOffsetYDp = next)
    }

    fun handleKeyboardCommitText(text: String) {
        val s = _uiState.value
        when (s.simulatorScene) {
            SimulatorScene.HOME_SCREEN -> {
                _uiState.value = s.copy(homeScreenSearchText = s.homeScreenSearchText + text)
            }
            SimulatorScene.EXTERNAL_APP -> {
                _uiState.value = s.copy(externalAppInputText = s.externalAppInputText + text)
            }
            SimulatorScene.WIN_NOTEPAD -> {
                _uiState.value = s.copy(notepadText = s.notepadText + text)
            }
        }
    }

    fun handleKeyboardBackspace() {
        val s = _uiState.value
        when (s.simulatorScene) {
            SimulatorScene.HOME_SCREEN -> {
                if (s.homeScreenSearchText.isNotEmpty()) {
                    _uiState.value = s.copy(homeScreenSearchText = s.homeScreenSearchText.dropLast(1))
                }
            }
            SimulatorScene.EXTERNAL_APP -> {
                if (s.externalAppInputText.isNotEmpty()) {
                    _uiState.value = s.copy(externalAppInputText = s.externalAppInputText.dropLast(1))
                }
            }
            SimulatorScene.WIN_NOTEPAD -> {
                if (s.notepadText.isNotEmpty()) {
                    _uiState.value = s.copy(notepadText = s.notepadText.dropLast(1))
                }
            }
        }
    }

    fun handleKeyboardEnter() {
        val s = _uiState.value
        when (s.simulatorScene) {
            SimulatorScene.HOME_SCREEN -> {
                // Keep text in Home Screen quick search/note bar
            }
            SimulatorScene.EXTERNAL_APP -> {
                sendSimulatedChatMessage()
            }
            SimulatorScene.WIN_NOTEPAD -> {
                _uiState.value = s.copy(notepadText = s.notepadText + "\n")
            }
        }
    }

    fun handleKeyboardClear() {
        val s = _uiState.value
        when (s.simulatorScene) {
            SimulatorScene.HOME_SCREEN -> _uiState.value = s.copy(homeScreenSearchText = "")
            SimulatorScene.EXTERNAL_APP -> _uiState.value = s.copy(externalAppInputText = "")
            SimulatorScene.WIN_NOTEPAD -> _uiState.value = s.copy(notepadText = "")
        }
    }

    fun handleKeyboardCopy(context: Context) {
        val activeText = getActiveSceneText()
        if (activeText.isNotEmpty()) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("WinKeyClassic", activeText))
            Toast.makeText(context, "הטקסט הועתק ללוח!", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleKeyboardPaste(context: Context) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = cm?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
        if (!clip.isNullOrEmpty()) {
            handleKeyboardCommitText(clip)
        }
    }

    fun handleArrowKey(keyType: KeyType) {
        // Cursor navigation feedback
        if (keyType == KeyType.ARROW_DOWN && _uiState.value.simulatorScene == SimulatorScene.EXTERNAL_APP) {
            sendSimulatedChatMessage()
        }
    }

    fun sendSimulatedChatMessage() {
        val s = _uiState.value
        val msg = s.externalAppInputText.trim()
        if (msg.isEmpty()) return
        val newList = s.externalAppMessages + ChatMessageItem(
            id = System.currentTimeMillis(),
            text = msg,
            time = "עכשיו",
            isFromUser = true
        )
        _uiState.value = s.copy(
            externalAppInputText = "",
            externalAppMessages = newList
        )
    }

    fun getActiveSceneText(): String {
        val s = _uiState.value
        return when (s.simulatorScene) {
            SimulatorScene.HOME_SCREEN -> s.homeScreenSearchText
            SimulatorScene.EXTERNAL_APP -> s.externalAppInputText
            SimulatorScene.WIN_NOTEPAD -> s.notepadText
        }
    }

    fun updateActiveSceneTextDirect(newText: String) {
        val s = _uiState.value
        when (s.simulatorScene) {
            SimulatorScene.HOME_SCREEN -> _uiState.value = s.copy(homeScreenSearchText = newText)
            SimulatorScene.EXTERNAL_APP -> _uiState.value = s.copy(externalAppInputText = newText)
            SimulatorScene.WIN_NOTEPAD -> _uiState.value = s.copy(notepadText = newText)
        }
    }

    fun startOrPopupGlobalOverlayService(context: Context) {
        if (!GlobalKeyboardOverlayService.canDrawOverlayCompat(context)) {
            Toast.makeText(
                context,
                "נא לאשר הרשאת תצוגה מעל אפליקציות אחרות כדי להציג מעל מסך הבית",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        try {
            val intent = Intent(context, GlobalKeyboardOverlayService::class.java).apply {
                action = GlobalKeyboardOverlayService.ACTION_POPUP_KEYBOARD
            }
            context.startService(intent)
            refreshSystemStatuses(context)
            Toast.makeText(
                context,
                "שכבת מקלדת ווינדוס ומקש 0 הצף הופעלו מעל כל המכשיר!",
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Throwable) {
        }
    }

    fun stopGlobalOverlayService(context: Context) {
        try {
            val intent = Intent(context, GlobalKeyboardOverlayService::class.java).apply {
                action = GlobalKeyboardOverlayService.ACTION_STOP_OVERLAY
            }
            context.startService(intent)
            ZeroKeyAccessibilityService.instance?.hideGlobalKeyboardOverlay()
            refreshSystemStatuses(context)
        } catch (_: Throwable) {
        }
    }
}
