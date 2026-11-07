package com.example.keyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.example.model.KeyType
import com.example.model.KeyboardLanguage
import com.example.model.KeyboardLayoutMode
import com.example.model.KeyboardStateBus
import com.example.model.WinClassicSkin
import com.example.model.WinKeySpec
import com.example.model.WinKeyboardLayouts
import kotlin.math.abs

interface WinKeyboardActionListener {
    fun onCommitText(text: String)
    fun onBackspace()
    fun onEnter()
    fun onArrowKey(keyType: KeyType)
    fun onCopyRequested()
    fun onPasteRequested()
    fun onClearRequested()
    fun onCloseKeyboardRequested()
    fun onMinimizeToZeroBadgeRequested()
    fun onDragWindowDelta(dx: Float, dy: Float)
}

/**
 * Custom Canvas-rendered Classic Windows 95/98/XP Keyboard View in Hebrew (SI-1452) and US English.
 * Compatible with Android 4.4 (API 19) through Android 16 (API 36), usable inside InputMethodService,
 * AccessibilityService global overlays, Service window overlays, and Jetpack Compose via AndroidView.
 */
class ClassicWindowsKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var actionListener: WinKeyboardActionListener? = null
    var showQuickScratchpadBar: Boolean = true
    var scratchpadPreviewText: String = ""
        set(value) {
            field = value
            invalidate()
        }
    var targetContextLabel: String = "יעד הקלדה: בכל מקום במכשיר"
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private var isShiftActive = false
    private var isCapsLockActive = false
    private var isCtrlActive = false
    private var isAltActive = false
    private var isWinMenuOpen = false

    private val hitRects = mutableListOf<Pair<RectF, WinKeySpec>>()
    private val phraseRects = mutableListOf<Pair<RectF, String>>()
    private val titleLangRect = RectF()
    private val titleLayoutRect = RectF()
    private val titleSkinRect = RectF()
    private val titleMinRect = RectF()
    private val titleCloseRect = RectF()
    private val scratchCopyRect = RectF()
    private val scratchClearRect = RectF()
    private val titleBarDragRect = RectF()

    private var pressedKeyId: String? = null
    private var isDraggingTitleBar = false
    private var lastRawX = 0f
    private var lastRawY = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val backspaceRepeatRunnable = object : Runnable {
        override fun run() {
            if (pressedKeyId == "bksp" || pressedKeyId == "bksp_n") {
                SoundAndHapticHelper.performKeyTapFeedback(context, false)
                handleBackspaceInternal()
                handler.postDelayed(this, 65L)
            }
        }
    }

    private val chassisPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titleGradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPrimaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val textSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }
    private val titleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        color = 0xFFFFFFFF.toInt()
    }
    private val ledPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast((320 * density).toInt())
        val config = KeyboardStateBus.state.value
        val scale = config.keyboardScalePercent / 100f
        val baseHeightDp = when (config.layoutMode) {
            KeyboardLayoutMode.FULL_PC -> if (showQuickScratchpadBar) 296f else 260f
            KeyboardLayoutMode.COMPACT_PC -> if (showQuickScratchpadBar) 254f else 220f
            KeyboardLayoutMode.NUMPAD_NIKUD -> if (showQuickScratchpadBar) 264f else 228f
        }
        val desiredHeight = (baseHeightDp * density * scale).toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val config = KeyboardStateBus.state.value
        val skin = config.skin
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // 1. Outer Classic Windows 3D Raised Frame & Chassis
        draw3DBox(canvas, 0f, 0f, w, h, skin.chassisColor, false, skin)

        val pad = 4f * density
        val titleH = 30f * density
        titleBarDragRect.set(pad, pad, w - pad, pad + titleH)

        // 2. Classic Windows Navy/Teal Gradient Title Bar
        titleGradientPaint.shader = LinearGradient(
            pad, pad, w - pad, pad,
            skin.titleBarStartColor, skin.titleBarEndColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(titleBarDragRect, titleGradientPaint)

        // Title Bar Buttons on Right: [X] [_] [Skin] [123] [HE/EN]
        val btnH = titleH - 6f * density
        val btnY = pad + 3f * density
        var curRight = w - pad - 4f * density

        // Close [X]
        val closeW = 26f * density
        titleCloseRect.set(curRight - closeW, btnY, curRight, btnY + btnH)
        draw3DBox(canvas, titleCloseRect.left, titleCloseRect.top, titleCloseRect.right, titleCloseRect.bottom, skin.specialKeyColor, false, skin)
        drawCenteredText(canvas, "✕", titleCloseRect, 12f * scaledDensity, skin.keyTextColor)
        curRight = titleCloseRect.left - 3f * density

        // Minimize [_] (to 0 badge)
        val minW = 26f * density
        titleMinRect.set(curRight - minW, btnY, curRight, btnY + btnH)
        draw3DBox(canvas, titleMinRect.left, titleMinRect.top, titleMinRect.right, titleMinRect.bottom, skin.specialKeyColor, false, skin)
        drawCenteredText(canvas, "—", titleMinRect, 12f * scaledDensity, skin.keyTextColor)
        curRight = titleMinRect.left - 4f * density

        // Skin cycle button
        val skinW = 38f * density
        titleSkinRect.set(curRight - skinW, btnY, curRight, btnY + btnH)
        draw3DBox(canvas, titleSkinRect.left, titleSkinRect.top, titleSkinRect.right, titleSkinRect.bottom, skin.specialKeyColor, false, skin)
        drawCenteredText(canvas, "צבע", titleSkinRect, 10.5f * scaledDensity, skin.keyTextColor)
        curRight = titleSkinRect.left - 3f * density

        // Layout Mode toggle button
        val layoutW = 46f * density
        titleLayoutRect.set(curRight - layoutW, btnY, curRight, btnY + btnH)
        draw3DBox(canvas, titleLayoutRect.left, titleLayoutRect.top, titleLayoutRect.right, titleLayoutRect.bottom, skin.specialKeyColor, false, skin)
        val layoutBtnLabel = when (config.layoutMode) {
            KeyboardLayoutMode.FULL_PC -> "מלאה"
            KeyboardLayoutMode.COMPACT_PC -> "גדולה"
            KeyboardLayoutMode.NUMPAD_NIKUD -> "ניקוד"
        }
        drawCenteredText(canvas, layoutBtnLabel, titleLayoutRect, 10.5f * scaledDensity, skin.keyTextColor)
        curRight = titleLayoutRect.left - 4f * density

        // Iconic Windows Blue Language Indicator [HE עב] / [EN אנג]
        val langW = 52f * density
        titleLangRect.set(curRight - langW, btnY, curRight, btnY + btnH)
        bevelPaint.color = if (config.language == KeyboardLanguage.HEBREW) 0xFF0044AA.toInt() else 0xFF006666.toInt()
        canvas.drawRect(titleLangRect, bevelPaint)
        drawCenteredText(canvas, config.language.badgeText, titleLangRect, 11f * scaledDensity, 0xFFFFFFFF.toInt())
        curRight = titleLangRect.left - 6f * density

        // Classic LED Indicators (CAPS / ALT / SHIFT)
        drawLedIndicator(canvas, curRight - 42f * density, btnY + 3f * density, "CAPS", isCapsLockActive)

        // Title Text on Left
        titleTextPaint.textSize = 12f * scaledDensity
        titleTextPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            "⊞ WinClassic 98",
            pad + 6f * density,
            pad + titleH * 0.68f,
            titleTextPaint
        )

        var contentTop = pad + titleH + 3f * density

        // 3. Optional Quick-Type / Scratchpad Strip (works on Home Screen even without focused EditText!)
        if (showQuickScratchpadBar) {
            val barH = 30f * density
            val copyW = 48f * density
            val clearW = 42f * density

            scratchClearRect.set(w - pad - clearW, contentTop, w - pad, contentTop + barH)
            draw3DBox(canvas, scratchClearRect.left, scratchClearRect.top, scratchClearRect.right, scratchClearRect.bottom, skin.specialKeyColor, false, skin)
            drawCenteredText(canvas, "נקה", scratchClearRect, 10.5f * scaledDensity, skin.keyTextColor)

            scratchCopyRect.set(scratchClearRect.left - 3f * density - copyW, contentTop, scratchClearRect.left - 3f * density, contentTop + barH)
            draw3DBox(canvas, scratchCopyRect.left, scratchCopyRect.top, scratchCopyRect.right, scratchCopyRect.bottom, skin.specialKeyColor, false, skin)
            drawCenteredText(canvas, "העתק", scratchCopyRect, 10.5f * scaledDensity, skin.keyTextColor)

            val boxRight = scratchCopyRect.left - 4f * density
            val textBoxRect = RectF(pad, contentTop, boxRight, contentTop + barH)
            draw3DBox(canvas, textBoxRect.left, textBoxRect.top, textBoxRect.right, textBoxRect.bottom, 0xFFFFFFFF.toInt(), true, skin)

            val displayBuffer = if (scratchpadPreviewText.isEmpty()) {
                "$targetContextLabel | הקלד כאן..."
            } else {
                "${scratchpadPreviewText.takeLast(38)}|"
            }
            textPrimaryPaint.color = if (scratchpadPreviewText.isEmpty()) 0xFF666666.toInt() else 0xFF000000.toInt()
            textPrimaryPaint.textSize = 12f * scaledDensity
            textPrimaryPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                displayBuffer,
                textBoxRect.right - 8f * density,
                textBoxRect.centerY() + 4f * density,
                textPrimaryPaint
            )
            textPrimaryPaint.textAlign = Paint.Align.CENTER

            contentTop += barH + 3f * density
        }

        // 4. If ❖ Win Start Popup Strip is open, render quick phrases bar
        phraseRects.clear()
        if (isWinMenuOpen) {
            val winBarH = 28f * density
            val phrases = if (config.language == KeyboardLanguage.HEBREW) {
                WinKeyboardLayouts.quickPhrasesHe
            } else {
                WinKeyboardLayouts.quickPhrasesEn
            }
            val itemW = (w - pad * 2) / phrases.size
            phrases.forEachIndexed { idx, phrase ->
                val r = RectF(
                    pad + idx * itemW + 1.5f * density,
                    contentTop,
                    pad + (idx + 1) * itemW - 1.5f * density,
                    contentTop + winBarH
                )
                draw3DBox(canvas, r.left, r.top, r.right, r.bottom, 0xFFFFF8DC.toInt(), false, skin)
                drawCenteredText(canvas, phrase.trim(), r, 10f * scaledDensity, 0xFF000080.toInt())
                phraseRects.add(r to phrase)
            }
            contentTop += winBarH + 2f * density
        }

        // 5. Render Keyboard Rows
        hitRects.clear()
        val rows: List<List<WinKeySpec>> = when (config.layoutMode) {
            KeyboardLayoutMode.FULL_PC -> listOf(
                WinKeyboardLayouts.utilityRow,
                WinKeyboardLayouts.numberRow,
                WinKeyboardLayouts.topLetterRow,
                WinKeyboardLayouts.homeLetterRow,
                WinKeyboardLayouts.bottomLetterRow,
                WinKeyboardLayouts.modifierRow
            )
            KeyboardLayoutMode.COMPACT_PC -> listOf(
                WinKeyboardLayouts.utilityRow,
                WinKeyboardLayouts.compactTopRow,
                WinKeyboardLayouts.compactHomeRow,
                WinKeyboardLayouts.compactBottomRow,
                WinKeyboardLayouts.modifierRow
            )
            KeyboardLayoutMode.NUMPAD_NIKUD -> WinKeyboardLayouts.numpadRows
        }

        val availableH = (h - contentTop - pad).coerceAtLeast(80f * density)
        val rowGap = 2.5f * density
        val rowCount = rows.size
        val rowH = (availableH - rowGap * (rowCount - 1)) / rowCount
        val keyGap = 2f * density

        rows.forEachIndexed { rowIndex, rowKeys ->
            val totalWeight = rowKeys.sumOf { it.weight.toDouble() }.toFloat()
            val rowTop = contentTop + rowIndex * (rowH + rowGap)
            val rowBottom = rowTop + rowH
            var curX = pad
            val usableRowW = w - pad * 2

            rowKeys.forEach { key ->
                val keyW = (usableRowW * (key.weight / totalWeight))
                val keyRect = RectF(
                    curX + keyGap * 0.5f,
                    rowTop,
                    curX + keyW - keyGap * 0.5f,
                    rowBottom
                )
                val isLatched = (key.keyType == KeyType.SHIFT && isShiftActive) ||
                    (key.keyType == KeyType.CAPS && isCapsLockActive) ||
                    (key.keyType == KeyType.CTRL && isCtrlActive) ||
                    (key.keyType == KeyType.ALT && isAltActive) ||
                    (key.keyType == KeyType.WIN && isWinMenuOpen)
                val isPressed = pressedKeyId == key.id || isLatched

                val faceColor = when {
                    isLatched -> 0xFFFFECB3.toInt()
                    key.keyType != KeyType.CHAR -> skin.specialKeyColor
                    else -> skin.keyFaceColor
                }

                draw3DBox(
                    canvas,
                    keyRect.left,
                    keyRect.top,
                    keyRect.right,
                    keyRect.bottom,
                    faceColor,
                    isPressed,
                    skin
                )

                drawKeyLegends(canvas, keyRect, key, config.language, skin, isPressed)
                hitRects.add(keyRect to key)
                curX += keyW
            }
        }
    }

    private fun drawKeyLegends(
        canvas: Canvas,
        rect: RectF,
        key: WinKeySpec,
        language: KeyboardLanguage,
        skin: WinClassicSkin,
        isPressed: Boolean
    ) {
        val pressOffset = if (isPressed) 1.2f * density else 0f
        val effectiveShift = isShiftActive xor isCapsLockActive

        if (key.keyType == KeyType.CHAR) {
            val mainLabel = if (language == KeyboardLanguage.HEBREW) {
                if (isShiftActive) key.heShift else key.hePrimary
            } else {
                if (effectiveShift) key.enShift else key.enPrimary
            }

            val secondaryLabel = if (language == KeyboardLanguage.HEBREW) {
                key.enPrimary.uppercase()
            } else {
                key.hePrimary
            }

            // Draw secondary dual-language legend in top-left corner of keycap
            if (secondaryLabel != mainLabel && rect.width() > 20f * density) {
                textSubPaint.color = if (isPressed && skin == WinClassicSkin.WIN98_DARK) {
                    0xFF004D40.toInt()
                } else {
                    skin.subLegendColor
                }
                textSubPaint.textSize = (9.5f * scaledDensity).coerceAtMost(rect.height() * 0.28f)
                canvas.drawText(
                    secondaryLabel,
                    rect.left + 3.5f * density + pressOffset,
                    rect.top + 10.5f * density + pressOffset,
                    textSubPaint
                )
            }

            // Draw bold primary legend in center-right / center of keycap
            textPrimaryPaint.color = if (isPressed && skin == WinClassicSkin.WIN98_DARK) {
                0xFF000000.toInt()
            } else {
                skin.keyTextColor
            }
            val mainTextSize = if (mainLabel.length > 2) {
                10.5f * scaledDensity
            } else {
                (14.5f * scaledDensity).coerceAtMost(rect.height() * 0.46f)
            }
            textPrimaryPaint.textSize = mainTextSize
            val fm = textPrimaryPaint.fontMetrics
            val cy = rect.centerY() - (fm.ascent + fm.descent) / 2f + pressOffset + 2f * density
            canvas.drawText(mainLabel, rect.centerX() + pressOffset, cy, textPrimaryPaint)
        } else {
            val label = if (language == KeyboardLanguage.HEBREW) key.hePrimary else key.enPrimary
            textPrimaryPaint.color = if (isPressed && skin == WinClassicSkin.WIN98_DARK) {
                0xFF000000.toInt()
            } else {
                skin.keyTextColor
            }
            val textSize = when {
                label.length > 8 -> 10.5f * scaledDensity
                label.length > 4 -> 11f * scaledDensity
                else -> 12f * scaledDensity
            }
            textPrimaryPaint.textSize = textSize
            val fm = textPrimaryPaint.fontMetrics
            val cy = rect.centerY() - (fm.ascent + fm.descent) / 2f + pressOffset
            canvas.drawText(label, rect.centerX() + pressOffset, cy, textPrimaryPaint)
        }
    }

    private fun drawLedIndicator(canvas: Canvas, x: Float, y: Float, label: String, active: Boolean) {
        val boxW = 38f * density
        val boxH = 16f * density
        bevelPaint.color = 0xFF1A1A1A.toInt()
        canvas.drawRect(x, y, x + boxW, y + boxH, bevelPaint)

        ledPaint.color = if (active) 0xFF00FF44.toInt() else 0xFF224422.toInt()
        canvas.drawCircle(x + 7f * density, y + boxH / 2f, 3.2f * density, ledPaint)

        titleTextPaint.textSize = 8.5f * scaledDensity
        titleTextPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(label, x + 13f * density, y + boxH * 0.72f, titleTextPaint)
    }

    private fun drawCenteredText(canvas: Canvas, text: String, rect: RectF, sizePx: Float, color: Int) {
        textPrimaryPaint.color = color
        textPrimaryPaint.textSize = sizePx
        textPrimaryPaint.textAlign = Paint.Align.CENTER
        val fm = textPrimaryPaint.fontMetrics
        val cy = rect.centerY() - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, rect.centerX(), cy, textPrimaryPaint)
    }

    /**
     * Authentic Windows 95/98 4-edge 3D bevel renderer.
     */
    private fun draw3DBox(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        faceColor: Int,
        sunken: Boolean,
        skin: WinClassicSkin
    ) {
        val b = (1.5f * density).coerceAtLeast(1f)
        val topLeftOuter = if (sunken) 0xFF000000.toInt() else skin.highlightLightColor
        val bottomRightOuter = if (sunken) skin.highlightLightColor else 0xFF000000.toInt()
        val topLeftInner = if (sunken) skin.shadowDarkColor else 0xFFDFDFDF.toInt()
        val bottomRightInner = if (sunken) 0xFFDFDFDF.toInt() else skin.shadowDarkColor

        // Outer shadow (bottom-right)
        bevelPaint.color = bottomRightOuter
        canvas.drawRect(left, top, right, bottom, bevelPaint)

        // Outer highlight (top-left)
        bevelPaint.color = topLeftOuter
        canvas.drawRect(left, top, right - b, bottom - b, bevelPaint)

        // Inner shadow (bottom-right)
        bevelPaint.color = bottomRightInner
        canvas.drawRect(left + b, top + b, right - b, bottom - b, bevelPaint)

        // Inner highlight (top-left)
        bevelPaint.color = topLeftInner
        canvas.drawRect(left + b, top + b, right - b * 2, bottom - b * 2, bevelPaint)

        // Keycap / Window Face
        bevelPaint.color = faceColor
        canvas.drawRect(left + b * 2, top + b * 2, right - b * 2, bottom - b * 2, bevelPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Check Title Bar buttons first
                if (titleCloseRect.contains(x, y)) {
                    SoundAndHapticHelper.performKeyTapFeedback(context, true)
                    actionListener?.onCloseKeyboardRequested()
                    return true
                }
                if (titleMinRect.contains(x, y)) {
                    SoundAndHapticHelper.performKeyTapFeedback(context, true)
                    actionListener?.onMinimizeToZeroBadgeRequested()
                    return true
                }
                if (titleLangRect.contains(x, y)) {
                    SoundAndHapticHelper.performKeyTapFeedback(context, true)
                    KeyboardStateBus.toggleLanguage(context)
                    invalidate()
                    return true
                }
                if (titleLayoutRect.contains(x, y)) {
                    SoundAndHapticHelper.performKeyTapFeedback(context, true)
                    val cur = KeyboardStateBus.state.value.layoutMode
                    val next = when (cur) {
                        KeyboardLayoutMode.FULL_PC -> KeyboardLayoutMode.COMPACT_PC
                        KeyboardLayoutMode.COMPACT_PC -> KeyboardLayoutMode.NUMPAD_NIKUD
                        KeyboardLayoutMode.NUMPAD_NIKUD -> KeyboardLayoutMode.FULL_PC
                    }
                    KeyboardStateBus.setLayoutMode(context, next)
                    requestLayout()
                    invalidate()
                    return true
                }
                if (titleSkinRect.contains(x, y)) {
                    SoundAndHapticHelper.performKeyTapFeedback(context, true)
                    val cur = KeyboardStateBus.state.value.skin
                    val next = when (cur) {
                        WinClassicSkin.WIN95_GRAY -> WinClassicSkin.WINXP_SILVER
                        WinClassicSkin.WINXP_SILVER -> WinClassicSkin.WIN98_DARK
                        WinClassicSkin.WIN98_DARK -> WinClassicSkin.WIN95_GRAY
                    }
                    KeyboardStateBus.setSkin(context, next)
                    invalidate()
                    return true
                }
                if (titleBarDragRect.contains(x, y)) {
                    isDraggingTitleBar = true
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                    return true
                }

                // Check Scratchpad strip buttons
                if (showQuickScratchpadBar) {
                    if (scratchCopyRect.contains(x, y)) {
                        SoundAndHapticHelper.performKeyTapFeedback(context, true)
                        copyScratchpadToClipboard()
                        actionListener?.onCopyRequested()
                        return true
                    }
                    if (scratchClearRect.contains(x, y)) {
                        SoundAndHapticHelper.performKeyTapFeedback(context, true)
                        scratchpadPreviewText = ""
                        KeyboardStateBus.updateSharedScratchpad("")
                        actionListener?.onClearRequested()
                        return true
                    }
                }

                // Check Win Quick Phrase Menu
                if (isWinMenuOpen) {
                    phraseRects.find { it.first.contains(x, y) }?.let { (_, phrase) ->
                        SoundAndHapticHelper.performKeyTapFeedback(context, false)
                        commitCharacterString(phrase)
                        isWinMenuOpen = false
                        requestLayout()
                        invalidate()
                        return true
                    }
                }

                // Check Keys
                val hit = hitRects.find { it.first.contains(x, y) }?.second
                if (hit != null) {
                    pressedKeyId = hit.id
                    SoundAndHapticHelper.performKeyTapFeedback(context, hit.keyType != KeyType.CHAR)
                    if (hit.keyType == KeyType.BACKSPACE) {
                        handleBackspaceInternal()
                        handler.removeCallbacks(backspaceRepeatRunnable)
                        handler.postDelayed(backspaceRepeatRunnable, 380L)
                    }
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDraggingTitleBar) {
                    val dx = event.rawX - lastRawX
                    val dy = event.rawY - lastRawY
                    if (abs(dx) > 1f || abs(dy) > 1f) {
                        actionListener?.onDragWindowDelta(dx, dy)
                        lastRawX = event.rawX
                        lastRawY = event.rawY
                    }
                    return true
                }
                val hit = hitRects.find { it.first.contains(x, y) }?.second
                if (hit?.id != pressedKeyId) {
                    handler.removeCallbacks(backspaceRepeatRunnable)
                    pressedKeyId = hit?.id
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(backspaceRepeatRunnable)
                if (isDraggingTitleBar) {
                    isDraggingTitleBar = false
                    return true
                }
                val currentPressedId = pressedKeyId
                pressedKeyId = null
                if (event.actionMasked == MotionEvent.ACTION_UP && currentPressedId != null) {
                    val key = hitRects.find { it.second.id == currentPressedId }?.second
                    if (key != null && key.keyType != KeyType.BACKSPACE) {
                        executeKeyAction(key)
                    }
                }
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleBackspaceInternal() {
        if (scratchpadPreviewText.isNotEmpty()) {
            scratchpadPreviewText = scratchpadPreviewText.dropLast(1)
            KeyboardStateBus.updateSharedScratchpad(scratchpadPreviewText)
        }
        actionListener?.onBackspace()
    }

    private fun commitCharacterString(text: String) {
        scratchpadPreviewText += text
        KeyboardStateBus.updateSharedScratchpad(scratchpadPreviewText)
        actionListener?.onCommitText(text)
    }

    private fun copyScratchpadToClipboard() {
        if (scratchpadPreviewText.isNotEmpty()) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("WinClassicKeyboard", scratchpadPreviewText))
            Toast.makeText(context, "הועתק ללוח: $scratchpadPreviewText", Toast.LENGTH_SHORT).show()
        }
    }

    private fun executeKeyAction(key: WinKeySpec) {
        val config = KeyboardStateBus.state.value
        val language = config.language
        val effectiveShift = isShiftActive xor isCapsLockActive

        when (key.keyType) {
            KeyType.CHAR -> {
                // Support Ctrl+C / Ctrl+V shortcuts like real Windows!
                if (isCtrlActive) {
                    when (key.id) {
                        "c" -> {
                            copyScratchpadToClipboard()
                            actionListener?.onCopyRequested()
                        }
                        "v" -> actionListener?.onPasteRequested()
                    }
                    isCtrlActive = false
                    invalidate()
                    return
                }
                val out = if (language == KeyboardLanguage.HEBREW) {
                    if (isShiftActive) key.heShift else key.hePrimary
                } else {
                    if (effectiveShift) key.enShift else key.enPrimary
                }
                commitCharacterString(out)
                if (isShiftActive) {
                    isShiftActive = false
                }
                isAltActive = false
            }

            KeyType.SPACE -> {
                if (isCtrlActive) {
                    // Ctrl + Space toggles HE/EN like Windows!
                    KeyboardStateBus.toggleLanguage(context)
                    isCtrlActive = false
                } else {
                    commitCharacterString(" ")
                }
            }

            KeyType.ENTER -> {
                scratchpadPreviewText += "\n"
                KeyboardStateBus.updateSharedScratchpad(scratchpadPreviewText)
                actionListener?.onEnter()
            }

            KeyType.TAB -> commitCharacterString("    ")

            KeyType.SHIFT -> {
                if (isAltActive) {
                    // Authentic Windows Alt + Shift switches Hebrew <-> English!
                    isAltActive = false
                    isShiftActive = false
                    KeyboardStateBus.toggleLanguage(context)
                } else {
                    isShiftActive = !isShiftActive
                }
            }

            KeyType.CAPS -> isCapsLockActive = !isCapsLockActive

            KeyType.ALT -> {
                if (isShiftActive) {
                    // Shift + Alt also toggles Hebrew <-> English!
                    isShiftActive = false
                    isAltActive = false
                    KeyboardStateBus.toggleLanguage(context)
                } else {
                    isAltActive = !isAltActive
                }
            }

            KeyType.CTRL -> isCtrlActive = !isCtrlActive

            KeyType.WIN -> {
                isWinMenuOpen = !isWinMenuOpen
                requestLayout()
            }

            KeyType.LANG_TOGGLE -> KeyboardStateBus.toggleLanguage(context)

            KeyType.NUMPAD_TOGGLE -> {
                val next = if (config.layoutMode == KeyboardLayoutMode.NUMPAD_NIKUD) {
                    KeyboardLayoutMode.FULL_PC
                } else {
                    KeyboardLayoutMode.NUMPAD_NIKUD
                }
                KeyboardStateBus.setLayoutMode(context, next)
                requestLayout()
            }

            KeyType.ARROW_LEFT, KeyType.ARROW_RIGHT, KeyType.ARROW_UP, KeyType.ARROW_DOWN -> {
                actionListener?.onArrowKey(key.keyType)
            }

            KeyType.ESC -> actionListener?.onCloseKeyboardRequested()

            KeyType.COPY -> {
                copyScratchpadToClipboard()
                actionListener?.onCopyRequested()
            }

            KeyType.PASTE -> actionListener?.onPasteRequested()

            KeyType.CLEAR -> {
                scratchpadPreviewText = ""
                KeyboardStateBus.updateSharedScratchpad("")
                actionListener?.onClearRequested()
            }

            KeyType.BACKSPACE -> handleBackspaceInternal()
        }
    }
}
