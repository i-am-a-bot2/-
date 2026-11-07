package com.example.model

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class KeyboardLanguage(val code: String, val badgeText: String, val displayNameHe: String) {
    HEBREW("HE", "HE עב", "עברית (SI-1452)"),
    ENGLISH("EN", "EN אנג", "English (US)")
}

enum class WinClassicSkin(
    val id: String,
    val titleHe: String,
    val subtitleHe: String,
    val chassisColor: Int,
    val keyFaceColor: Int,
    val specialKeyColor: Int,
    val keyTextColor: Int,
    val subLegendColor: Int,
    val titleBarStartColor: Int,
    val titleBarEndColor: Int,
    val highlightLightColor: Int,
    val shadowDarkColor: Int
) {
    WIN95_GRAY(
        id = "win95",
        titleHe = "Windows 95 / 98 Classic Gray",
        subtitleHe = "אפור קלאסי תלת-ממדי עם פס כותרת כחול רויאל",
        chassisColor = 0xFFD4D0C8.toInt(),
        keyFaceColor = 0xFFECE9D8.toInt(),
        specialKeyColor = 0xFFD4D0C8.toInt(),
        keyTextColor = 0xFF000000.toInt(),
        subLegendColor = 0xFF003399.toInt(),
        titleBarStartColor = 0xFF000080.toInt(),
        titleBarEndColor = 0xFF1084D0.toInt(),
        highlightLightColor = 0xFFFFFFFF.toInt(),
        shadowDarkColor = 0xFF808080.toInt()
    ),
    WINXP_SILVER(
        id = "winxp",
        titleHe = "Windows XP / 2000 Silver",
        subtitleHe = "כסוף מתכתי בהיר עם מקשים מודגשים",
        chassisColor = 0xFFDCE0E8.toInt(),
        keyFaceColor = 0xFFF7F8FC.toInt(),
        specialKeyColor = 0xFFC5CCD9.toInt(),
        keyTextColor = 0xFF101828.toInt(),
        subLegendColor = 0xFF0055B3.toInt(),
        titleBarStartColor = 0xFF0A246A.toInt(),
        titleBarEndColor = 0xFFA6CAF0.toInt(),
        highlightLightColor = 0xFFFFFFFF.toInt(),
        shadowDarkColor = 0xFF7B869C.toInt()
    ),
    WIN98_DARK(
        id = "win98_dark",
        titleHe = "Windows Retro Dark Teal",
        subtitleHe = "ניגודיות גבוהה כהה עם כיתוב צהוב/תכלת בולט",
        chassisColor = 0xFF1E262B.toInt(),
        keyFaceColor = 0xFF2B363E.toInt(),
        specialKeyColor = 0xFF182026.toInt(),
        keyTextColor = 0xFFFFE082.toInt(),
        subLegendColor = 0xFF80DEEA.toInt(),
        titleBarStartColor = 0xFF004D4D.toInt(),
        titleBarEndColor = 0xFF008080.toInt(),
        highlightLightColor = 0xFF4F6370.toInt(),
        shadowDarkColor = 0xFF0D1216.toInt()
    );

    companion object {
        fun fromId(id: String): WinClassicSkin = entries.find { it.id == id } ?: WIN95_GRAY
    }
}

enum class KeyboardLayoutMode(val id: String, val titleHe: String) {
    FULL_PC("full_pc", "מקלדת PC מלאה (5 שורות + חצים)"),
    COMPACT_PC("compact_pc", "מקלדת קומפקטית (מקשים גדולים)"),
    NUMPAD_NIKUD("numpad_nikud", "לוח מספרים (Numpad) + ניקוד עברי");

    companion object {
        fun fromId(id: String): KeyboardLayoutMode = entries.find { it.id == id } ?: FULL_PC
    }
}

enum class KeyType {
    CHAR,
    BACKSPACE,
    ENTER,
    SHIFT,
    CAPS,
    TAB,
    SPACE,
    CTRL,
    ALT,
    WIN,
    LANG_TOGGLE,
    NUMPAD_TOGGLE,
    ARROW_LEFT,
    ARROW_RIGHT,
    ARROW_UP,
    ARROW_DOWN,
    ESC,
    COPY,
    PASTE,
    CLEAR
}

data class WinKeySpec(
    val id: String,
    val enPrimary: String,
    val enShift: String = enPrimary,
    val hePrimary: String = enPrimary,
    val heShift: String = enShift,
    val weight: Float = 1f,
    val keyType: KeyType = KeyType.CHAR
)

object WinKeyboardLayouts {

    // Function / Quick Action top utility row (optional strip inside Full PC mode)
    val utilityRow = listOf(
        WinKeySpec("esc", "Esc", keyType = KeyType.ESC, weight = 1.1f),
        WinKeySpec("copy", "Copy", hePrimary = "העתק", keyType = KeyType.COPY, weight = 1.3f),
        WinKeySpec("paste", "Paste", hePrimary = "הדבק", keyType = KeyType.PASTE, weight = 1.3f),
        WinKeySpec("clear", "Clr", hePrimary = "נקה", keyType = KeyType.CLEAR, weight = 1.1f),
        WinKeySpec("shekel", "₪", "₪", "₪", "₪", weight = 0.9f),
        WinKeySpec("geresh", "'", "\"", "׳", "״", weight = 0.9f),
        WinKeySpec("up", "▲", keyType = KeyType.ARROW_UP, weight = 0.95f),
        WinKeySpec("down", "▼", keyType = KeyType.ARROW_DOWN, weight = 0.95f),
        WinKeySpec("left", "◀", keyType = KeyType.ARROW_LEFT, weight = 0.95f),
        WinKeySpec("right", "▶", keyType = KeyType.ARROW_RIGHT, weight = 0.95f)
    )

    // Row 1: Classic Windows Number Row
    val numberRow = listOf(
        WinKeySpec("grave", "`", "~", ";", "~", weight = 0.9f),
        WinKeySpec("1", "1", "!", "1", "!", weight = 1f),
        WinKeySpec("2", "2", "@", "2", "@", weight = 1f),
        WinKeySpec("3", "3", "#", "3", "#", weight = 1f),
        WinKeySpec("4", "4", "$", "4", "₪", weight = 1f),
        WinKeySpec("5", "5", "%", "5", "%", weight = 1f),
        WinKeySpec("6", "6", "^", "6", "^", weight = 1f),
        WinKeySpec("7", "7", "&", "7", "&", weight = 1f),
        WinKeySpec("8", "8", "*", "8", "*", weight = 1f),
        WinKeySpec("9", "9", "(", "9", ")", weight = 1f),
        WinKeySpec("0", "0", ")", "0", "(", weight = 1f),
        WinKeySpec("minus", "-", "_", "-", "_", weight = 0.95f),
        WinKeySpec("equals", "=", "+", "=", "+", weight = 0.95f),
        WinKeySpec("bksp", "← Bksp", hePrimary = "← מחק", keyType = KeyType.BACKSPACE, weight = 1.7f)
    )

    // Row 2: Top QWERTY / קראטוןםפ Row (Exact Israeli Standard SI-1452)
    val topLetterRow = listOf(
        WinKeySpec("tab", "Tab ⇥", keyType = KeyType.TAB, weight = 1.35f),
        WinKeySpec("q", "q", "Q", "/", "Q", weight = 1f),
        WinKeySpec("w", "w", "W", "'", "W", weight = 1f),
        WinKeySpec("e", "e", "E", "ק", "E", weight = 1f),
        WinKeySpec("r", "r", "R", "ר", "R", weight = 1f),
        WinKeySpec("t", "t", "T", "א", "T", weight = 1f),
        WinKeySpec("y", "y", "Y", "ט", "Y", weight = 1f),
        WinKeySpec("u", "u", "U", "ו", "U", weight = 1f),
        WinKeySpec("i", "i", "I", "ן", "I", weight = 1f),
        WinKeySpec("o", "o", "O", "ם", "O", weight = 1f),
        WinKeySpec("p", "p", "P", "פ", "P", weight = 1f),
        WinKeySpec("lbracket", "[", "{", "]", "}", weight = 0.95f),
        WinKeySpec("rbracket", "]", "}", "[", "{", weight = 0.95f),
        WinKeySpec("backslash", "\\", "|", "\\", "|", weight = 1.05f)
    )

    // Row 3: Home ASDFGHJKL / שדגכעיחלךף Row
    val homeLetterRow = listOf(
        WinKeySpec("caps", "Caps", keyType = KeyType.CAPS, weight = 1.55f),
        WinKeySpec("a", "a", "A", "ש", "A", weight = 1f),
        WinKeySpec("s", "s", "S", "ד", "S", weight = 1f),
        WinKeySpec("d", "d", "D", "ג", "D", weight = 1f),
        WinKeySpec("f", "f", "F", "כ", "F", weight = 1f),
        WinKeySpec("g", "g", "G", "ע", "G", weight = 1f),
        WinKeySpec("h", "h", "H", "י", "H", weight = 1f),
        WinKeySpec("j", "j", "J", "ח", "J", weight = 1f),
        WinKeySpec("k", "k", "K", "ל", "K", weight = 1f),
        WinKeySpec("l", "l", "L", "ך", "L", weight = 1f),
        WinKeySpec("semicolon", ";", ":", "ף", ":", weight = 1f),
        WinKeySpec("quote", "'", "\"", ",", "\"", weight = 1f),
        WinKeySpec("enter", "↵ Enter", hePrimary = "↵ אנטר", keyType = KeyType.ENTER, weight = 1.85f)
    )

    // Row 4: Bottom ZXCVBNM / זסבהנמצתץ Row
    val bottomLetterRow = listOf(
        WinKeySpec("lshift", "⇧ Shift", keyType = KeyType.SHIFT, weight = 1.85f),
        WinKeySpec("z", "z", "Z", "ז", "Z", weight = 1f),
        WinKeySpec("x", "x", "X", "ס", "X", weight = 1f),
        WinKeySpec("c", "c", "C", "ב", "C", weight = 1f),
        WinKeySpec("v", "v", "V", "ה", "V", weight = 1f),
        WinKeySpec("b", "b", "B", "נ", "B", weight = 1f),
        WinKeySpec("n", "n", "N", "מ", "N", weight = 1f),
        WinKeySpec("m", "m", "M", "צ", "M", weight = 1f),
        WinKeySpec("comma", ",", "<", "ת", ">", weight = 1f),
        WinKeySpec("period", ".", ">", "ץ", "<", weight = 1f),
        WinKeySpec("slash", "/", "?", ".", "?", weight = 1f),
        WinKeySpec("rshift", "⇧ Shift", keyType = KeyType.SHIFT, weight = 1.55f)
    )

    // Row 5: Classic Windows Bottom Modifier & Space Row
    val modifierRow = listOf(
        WinKeySpec("ctrl", "Ctrl", keyType = KeyType.CTRL, weight = 1.15f),
        WinKeySpec("win", "❖ Win", keyType = KeyType.WIN, weight = 1.25f),
        WinKeySpec("alt", "Alt", keyType = KeyType.ALT, weight = 1.15f),
        WinKeySpec("lang", "HE/EN", hePrimary = "עב/EN", keyType = KeyType.LANG_TOGGLE, weight = 1.35f),
        WinKeySpec("space", "Space", hePrimary = "רווח — Windows Classic", keyType = KeyType.SPACE, weight = 4.2f),
        WinKeySpec("numpad", "123/ניקוד", keyType = KeyType.NUMPAD_TOGGLE, weight = 1.45f),
        WinKeySpec("left_b", "◀", keyType = KeyType.ARROW_LEFT, weight = 0.9f),
        WinKeySpec("right_b", "▶", keyType = KeyType.ARROW_RIGHT, weight = 0.9f)
    )

    // Compact 4-Row Layout (larger buttons for smaller screens)
    val compactTopRow = listOf(
        WinKeySpec("q", "q", "Q", "/", "1"),
        WinKeySpec("w", "w", "W", "'", "2"),
        WinKeySpec("e", "e", "E", "ק", "3"),
        WinKeySpec("r", "r", "R", "ר", "4"),
        WinKeySpec("t", "t", "T", "א", "5"),
        WinKeySpec("y", "y", "Y", "ט", "6"),
        WinKeySpec("u", "u", "U", "ו", "7"),
        WinKeySpec("i", "i", "I", "ן", "8"),
        WinKeySpec("o", "o", "O", "ם", "9"),
        WinKeySpec("p", "p", "P", "פ", "0")
    )

    val compactHomeRow = listOf(
        WinKeySpec("a", "a", "A", "ש", "!"),
        WinKeySpec("s", "s", "S", "ד", "@"),
        WinKeySpec("d", "d", "D", "ג", "#"),
        WinKeySpec("f", "f", "F", "כ", "₪"),
        WinKeySpec("g", "g", "G", "ע", "%"),
        WinKeySpec("h", "h", "H", "י", "&"),
        WinKeySpec("j", "j", "J", "ח", "*"),
        WinKeySpec("k", "k", "K", "ל", "("),
        WinKeySpec("l", "l", "L", "ך", ")"),
        WinKeySpec("semicolon", ";", ":", "ף", "-")
    )

    val compactBottomRow = listOf(
        WinKeySpec("lshift", "⇧", keyType = KeyType.SHIFT, weight = 1.3f),
        WinKeySpec("z", "z", "Z", "ז", "\""),
        WinKeySpec("x", "x", "X", "ס", "'"),
        WinKeySpec("c", "c", "C", "ב", ":"),
        WinKeySpec("v", "v", "V", "ה", ";"),
        WinKeySpec("b", "b", "B", "נ", "/"),
        WinKeySpec("n", "n", "N", "מ", "?"),
        WinKeySpec("m", "m", "M", "צ", "="),
        WinKeySpec("comma", ",", "<", "ת", "+"),
        WinKeySpec("period", ".", ">", "ץ", "."),
        WinKeySpec("bksp", "⌫", keyType = KeyType.BACKSPACE, weight = 1.4f)
    )

    // Classic Windows Numpad + Hebrew Nikud (Vowels) Mode
    val numpadRows = listOf(
        listOf(
            WinKeySpec("kamatz", "ָ קמץ", "ָ", "ָ", "ָ", weight = 1.2f),
            WinKeySpec("patach", "ַ פתח", "ַ", "ַ", "ַ", weight = 1.2f),
            WinKeySpec("tzere", "ֵ צירה", "ֵ", "ֵ", "ֵ", weight = 1.2f),
            WinKeySpec("num7", "7", "Home", "7", "7", weight = 1f),
            WinKeySpec("num8", "8", "▲", "8", "8", weight = 1f),
            WinKeySpec("num9", "9", "PgUp", "9", "9", weight = 1f),
            WinKeySpec("num_div", "/", "/", "/", "/", weight = 0.9f),
            WinKeySpec("bksp_n", "← מחק", keyType = KeyType.BACKSPACE, weight = 1.3f)
        ),
        listOf(
            WinKeySpec("segol", "ֶ סגול", "ֶ", "ֶ", "ֶ", weight = 1.2f),
            WinKeySpec("chirik", "ִ חיריק", "ִ", "ִ", "ִ", weight = 1.2f),
            WinKeySpec("cholam", "ֹ חולם", "ֹ", "ֹ", "ֹ", weight = 1.2f),
            WinKeySpec("num4", "4", "◀", "4", "4", weight = 1f),
            WinKeySpec("num5", "5", "5", "5", "5", weight = 1f),
            WinKeySpec("num6", "6", "▶", "6", "6", weight = 1f),
            WinKeySpec("num_mul", "*", "*", "*", "*", weight = 0.9f),
            WinKeySpec("num_sub", "-", "-", "-", "-", weight = 1.3f)
        ),
        listOf(
            WinKeySpec("kubutz", "ֻ קובוץ", "ֻ", "ֻ", "ֻ", weight = 1.2f),
            WinKeySpec("shva", "ְ שווא", "ְ", "ְ", "ְ", weight = 1.2f),
            WinKeySpec("dagesh", "ּ דגש", "ּ", "ּ", "ּ", weight = 1.2f),
            WinKeySpec("num1", "1", "End", "1", "1", weight = 1f),
            WinKeySpec("num2", "2", "▼", "2", "2", weight = 1f),
            WinKeySpec("num3", "3", "PgDn", "3", "3", weight = 1f),
            WinKeySpec("num_add", "+", "+", "+", "+", weight = 0.9f),
            WinKeySpec("enter_n", "↵ Enter", keyType = KeyType.ENTER, weight = 1.3f)
        ),
        listOf(
            WinKeySpec("back_abc", "א-ת / ABC", keyType = KeyType.NUMPAD_TOGGLE, weight = 1.8f),
            WinKeySpec("lang_n", "HE/EN", keyType = KeyType.LANG_TOGGLE, weight = 1.2f),
            WinKeySpec("shekel_n", "₪", "₪", "₪", "₪", weight = 0.9f),
            WinKeySpec("num0", "0 Ins", "0", "0", "0", weight = 1.8f),
            WinKeySpec("num_dot", ".", "Del", ".", ".", weight = 0.9f),
            WinKeySpec("num_eq", "=", "=", "=", "=", weight = 0.9f),
            WinKeySpec("space_n", "רווח", keyType = KeyType.SPACE, weight = 1.5f)
        )
    )

    val quickPhrasesHe = listOf(
        "שלום וברכה! ",
        "בס\"ד ",
        "אני חוזר אליך בהקדם. ",
        "תודה רבה! ",
        "נשלח ממקלדת ווינדוס הקלאסית "
    )

    val quickPhrasesEn = listOf(
        "Hello! ",
        "Thank you very much! ",
        "I will get back to you soon. ",
        "@gmail.com",
        "https://www."
    )
}

data class KeyboardConfigState(
    val language: KeyboardLanguage = KeyboardLanguage.HEBREW,
    val skin: WinClassicSkin = WinClassicSkin.WIN95_GRAY,
    val layoutMode: KeyboardLayoutMode = KeyboardLayoutMode.FULL_PC,
    val longPressZeroThresholdMs: Int = 450,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val showFloatingZeroBadge: Boolean = true,
    val keyboardScalePercent: Int = 100,
    val interceptHardwareZeroKey: Boolean = true,
    val api19CompatMode: Boolean = true,
    val isOverlayKeyboardVisible: Boolean = false,
    val lastTriggerSource: String = "מוכן — החזק מקש 0 להקפצת המקלדת",
    val totalZeroLongPressCount: Int = 0
)

/**
 * Shared singleton reactive bus so MainActivity, ZeroKeyAccessibilityService,
 * WinClassicInputMethodService, and GlobalKeyboardOverlayService stay synchronized.
 */
object KeyboardStateBus {
    private const val PREFS_NAME = "winkey_classic_prefs"

    private val _state = MutableStateFlow(KeyboardConfigState())
    val state: StateFlow<KeyboardConfigState> = _state.asStateFlow()

    private val _sharedScratchpadText = MutableStateFlow("")
    val sharedScratchpadText: StateFlow<String> = _sharedScratchpadText.asStateFlow()

    fun loadFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val langCode = prefs.getString("language", KeyboardLanguage.HEBREW.code) ?: "HE"
        val skinId = prefs.getString("skin", WinClassicSkin.WIN95_GRAY.id) ?: "win95"
        val layoutId = prefs.getString("layout_mode", KeyboardLayoutMode.FULL_PC.id) ?: "full_pc"
        _state.value = _state.value.copy(
            language = if (langCode == "EN") KeyboardLanguage.ENGLISH else KeyboardLanguage.HEBREW,
            skin = WinClassicSkin.fromId(skinId),
            layoutMode = KeyboardLayoutMode.fromId(layoutId),
            longPressZeroThresholdMs = prefs.getInt("long_press_ms", 450).coerceIn(250, 1000),
            soundEnabled = prefs.getBoolean("sound_enabled", true),
            vibrationEnabled = prefs.getBoolean("vibration_enabled", true),
            showFloatingZeroBadge = prefs.getBoolean("floating_zero_badge", true),
            keyboardScalePercent = prefs.getInt("scale_percent", 100).coerceIn(80, 130),
            interceptHardwareZeroKey = prefs.getBoolean("intercept_hw_zero", true),
            api19CompatMode = prefs.getBoolean("api19_compat", true),
            totalZeroLongPressCount = prefs.getInt("zero_press_count", 0)
        )
    }

    private fun saveToPrefs(context: Context, updater: SharedPreferences.Editor.() -> Unit) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.updater()
        editor.apply()
    }

    fun toggleLanguage(context: Context? = null) {
        val next = if (_state.value.language == KeyboardLanguage.HEBREW) {
            KeyboardLanguage.ENGLISH
        } else {
            KeyboardLanguage.HEBREW
        }
        _state.value = _state.value.copy(language = next)
        context?.let { saveToPrefs(it) { putString("language", next.code) } }
    }

    fun setLanguage(context: Context?, language: KeyboardLanguage) {
        _state.value = _state.value.copy(language = language)
        context?.let { saveToPrefs(it) { putString("language", language.code) } }
    }

    fun setSkin(context: Context?, skin: WinClassicSkin) {
        _state.value = _state.value.copy(skin = skin)
        context?.let { saveToPrefs(it) { putString("skin", skin.id) } }
    }

    fun setLayoutMode(context: Context?, mode: KeyboardLayoutMode) {
        _state.value = _state.value.copy(layoutMode = mode)
        context?.let { saveToPrefs(it) { putString("layout_mode", mode.id) } }
    }

    fun setLongPressThreshold(context: Context?, ms: Int) {
        val clamped = ms.coerceIn(250, 1000)
        _state.value = _state.value.copy(longPressZeroThresholdMs = clamped)
        context?.let { saveToPrefs(it) { putInt("long_press_ms", clamped) } }
    }

    fun setSoundEnabled(context: Context?, enabled: Boolean) {
        _state.value = _state.value.copy(soundEnabled = enabled)
        context?.let { saveToPrefs(it) { putBoolean("sound_enabled", enabled) } }
    }

    fun setVibrationEnabled(context: Context?, enabled: Boolean) {
        _state.value = _state.value.copy(vibrationEnabled = enabled)
        context?.let { saveToPrefs(it) { putBoolean("vibration_enabled", enabled) } }
    }

    fun setShowFloatingZeroBadge(context: Context?, enabled: Boolean) {
        _state.value = _state.value.copy(showFloatingZeroBadge = enabled)
        context?.let { saveToPrefs(it) { putBoolean("floating_zero_badge", enabled) } }
    }

    fun setKeyboardScalePercent(context: Context?, percent: Int) {
        val clamped = percent.coerceIn(80, 130)
        _state.value = _state.value.copy(keyboardScalePercent = clamped)
        context?.let { saveToPrefs(it) { putInt("scale_percent", clamped) } }
    }

    fun setInterceptHardwareZeroKey(context: Context?, enabled: Boolean) {
        _state.value = _state.value.copy(interceptHardwareZeroKey = enabled)
        context?.let { saveToPrefs(it) { putBoolean("intercept_hw_zero", enabled) } }
    }

    fun setApi19CompatMode(context: Context?, enabled: Boolean) {
        _state.value = _state.value.copy(api19CompatMode = enabled)
        context?.let { saveToPrefs(it) { putBoolean("api19_compat", enabled) } }
    }

    fun notifyZeroKeyLongPressTriggered(context: Context?, sourceDescription: String) {
        val nextCount = _state.value.totalZeroLongPressCount + 1
        _state.value = _state.value.copy(
            isOverlayKeyboardVisible = true,
            lastTriggerSource = sourceDescription,
            totalZeroLongPressCount = nextCount
        )
        context?.let { saveToPrefs(it) { putInt("zero_press_count", nextCount) } }
    }

    fun setOverlayKeyboardVisible(visible: Boolean, reason: String? = null) {
        _state.value = _state.value.copy(
            isOverlayKeyboardVisible = visible,
            lastTriggerSource = reason ?: _state.value.lastTriggerSource
        )
    }

    fun updateSharedScratchpad(text: String) {
        _sharedScratchpadText.value = text
    }
}
