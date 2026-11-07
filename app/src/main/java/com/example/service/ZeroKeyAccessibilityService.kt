package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.keyboard.ClassicWindowsKeyboardView
import com.example.keyboard.FloatingZeroKeyBadgeView
import com.example.keyboard.SoundAndHapticHelper
import com.example.keyboard.WinKeyboardActionListener
import com.example.model.KeyType
import com.example.model.KeyboardStateBus

/**
 * System-wide AccessibilityService that:
 * 1. Intercepts physical/hardware long-press on the '0' key (KEYCODE_0 / KEYCODE_NUMPAD_0)
 *    everywhere on the device — whether on the Home Screen or inside any app.
 * 2. Displays the Classic Windows HE/EN Keyboard overlay on top of any screen using
 *    TYPE_ACCESSIBILITY_OVERLAY (API 22+) or TYPE_PHONE (API 19-21).
 * 3. Injects typed Hebrew/English text directly into any focused editable field across apps,
 *    while also keeping the built-in Windows Scratchpad strip active when on the Home Screen.
 */
class ZeroKeyAccessibilityService : AccessibilityService(), WinKeyboardActionListener {

    companion object {
        @Volatile
        var instance: ZeroKeyAccessibilityService? = null
            private set
    }

    private var windowManager: WindowManager? = null
    private var keyboardOverlayView: ClassicWindowsKeyboardView? = null
    private var keyboardLayoutParams: WindowManager.LayoutParams? = null

    private var zeroBadgeView: FloatingZeroKeyBadgeView? = null
    private var zeroBadgeParams: WindowManager.LayoutParams? = null

    private val handler = Handler(Looper.getMainLooper())
    private var isZeroKeyDown = false
    private var zeroKeyLongPressFired = false
    private var lastActivePackageName: String = "מסך הבית / מערכת"

    private val zeroKeyHoldRunnable = Runnable {
        if (isZeroKeyDown) {
            zeroKeyLongPressFired = true
            SoundAndHapticHelper.performZeroKeyPopUpFeedback(this)
            KeyboardStateBus.notifyZeroKeyLongPressTriggered(
                this,
                "לחיצה ארוכה על מקש 0 פיזי ($lastActivePackageName)"
            )
            showGlobalKeyboardOverlay()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        KeyboardStateBus.loadFromPrefs(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (KeyboardStateBus.state.value.showFloatingZeroBadge) {
            showFloatingZeroBadgeOverlay()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString()
        if (!pkg.isNullOrBlank() && pkg != packageName) {
            lastActivePackageName = pkg.substringAfterLast('.')
            keyboardOverlayView?.targetContextLabel = "אפליקציה פעילה: $lastActivePackageName"
        }
    }

    override fun onInterrupt() {
        hideGlobalKeyboardOverlay()
    }

    override fun onDestroy() {
        removeAllOverlays()
        if (instance == this) {
            instance = null
        }
        super.onDestroy()
    }

    /**
     * Intercepts physical/hardware '0' key press anywhere on the phone (Home Screen, Dialer, Browser, Chat).
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        val config = KeyboardStateBus.state.value
        if (!config.interceptHardwareZeroKey) {
            return super.onKeyEvent(event)
        }

        val keyCode = event.keyCode
        if (keyCode == KeyEvent.KEYCODE_0 || keyCode == KeyEvent.KEYCODE_NUMPAD_0) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount == 0) {
                        isZeroKeyDown = true
                        zeroKeyLongPressFired = false
                        handler.removeCallbacks(zeroKeyHoldRunnable)
                        handler.postDelayed(
                            zeroKeyHoldRunnable,
                            config.longPressZeroThresholdMs.toLong()
                        )
                    }
                    // Consume repeats if long-press already triggered
                    if (zeroKeyLongPressFired) {
                        return true
                    }
                }
                KeyEvent.ACTION_UP -> {
                    isZeroKeyDown = false
                    handler.removeCallbacks(zeroKeyHoldRunnable)
                    if (zeroKeyLongPressFired) {
                        zeroKeyLongPressFired = false
                        return true
                    }
                }
            }
        }
        return super.onKeyEvent(event)
    }

    @Suppress("DEPRECATION")
    private fun resolveOverlayWindowType(): Int {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ->
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Settings.canDrawOverlays(this) ->
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else ->
                // Android 4.4 (API 19 KitKat) compatible system window type
                WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    fun showFloatingZeroBadgeOverlay() {
        val wm = windowManager ?: return
        if (zeroBadgeView != null) return

        val badge = FloatingZeroKeyBadgeView(this).apply {
            onLongPressZeroTriggered = {
                KeyboardStateBus.notifyZeroKeyLongPressTriggered(
                    this@ZeroKeyAccessibilityService,
                    "לחיצה ארוכה על מקש 0 צף ($lastActivePackageName)"
                )
                showGlobalKeyboardOverlay()
            }
            onDragBadgeDelta = { dx, dy ->
                zeroBadgeParams?.let { params ->
                    params.x = (params.x + dx.toInt()).coerceAtLeast(0)
                    params.y = (params.y + dy.toInt()).coerceAtLeast(0)
                    try {
                        wm.updateViewLayout(this, params)
                    } catch (_: Throwable) {
                    }
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            resolveOverlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (resources.displayMetrics.widthPixels - (76 * resources.displayMetrics.density).toInt()).coerceAtLeast(16)
            y = (resources.displayMetrics.heightPixels * 0.42f).toInt()
        }

        try {
            wm.addView(badge, params)
            zeroBadgeView = badge
            zeroBadgeParams = params
        } catch (_: Throwable) {
        }
    }

    fun hideFloatingZeroBadgeOverlay() {
        val wm = windowManager ?: return
        zeroBadgeView?.let {
            try {
                wm.removeView(it)
            } catch (_: Throwable) {
            }
        }
        zeroBadgeView = null
        zeroBadgeParams = null
    }

    fun showGlobalKeyboardOverlay() {
        val wm = windowManager ?: return
        if (keyboardOverlayView != null) {
            keyboardOverlayView?.invalidate()
            return
        }

        val view = ClassicWindowsKeyboardView(this).apply {
            showQuickScratchpadBar = true
            targetContextLabel = "פעיל מעל: $lastActivePackageName"
            scratchpadPreviewText = KeyboardStateBus.sharedScratchpadText.value
            actionListener = this@ZeroKeyAccessibilityService
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            resolveOverlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 0
        }

        try {
            wm.addView(view, params)
            keyboardOverlayView = view
            keyboardLayoutParams = params
            KeyboardStateBus.setOverlayKeyboardVisible(true, "מקלדת ווינדוס פעילה על המסך")
        } catch (_: Throwable) {
        }
    }

    fun hideGlobalKeyboardOverlay() {
        val wm = windowManager ?: return
        keyboardOverlayView?.let {
            try {
                wm.removeView(it)
            } catch (_: Throwable) {
            }
        }
        keyboardOverlayView = null
        keyboardLayoutParams = null
        KeyboardStateBus.setOverlayKeyboardVisible(false)
    }

    private fun removeAllOverlays() {
        hideGlobalKeyboardOverlay()
        hideFloatingZeroBadgeOverlay()
    }

    /**
     * Finds the focused editable AccessibilityNodeInfo in whichever app is currently on screen
     * and injects text directly into it (compatible with API 21+ ACTION_SET_TEXT and clipboard fallback).
     */
    private fun findFocusedEditableNode(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && focused.isEditable) {
            return focused
        }
        return findFirstEditableRecursive(root)
    }

    private fun findFirstEditableRecursive(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable && node.isFocused) return node
        for (i in 0 until node.childCount) {
            val found = findFirstEditableRecursive(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    override fun onCommitText(text: String) {
        val node = findFocusedEditableNode()
        if (node != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val existing = node.text?.toString() ?: ""
            val updated = existing + text
            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    updated
                )
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
    }

    override fun onBackspace() {
        val node = findFocusedEditableNode()
        if (node != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val existing = node.text?.toString() ?: ""
            if (existing.isNotEmpty()) {
                val updated = existing.dropLast(1)
                val args = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        updated
                    )
                }
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
        }
    }

    override fun onEnter() {
        onCommitText("\n")
    }

    override fun onArrowKey(keyType: KeyType) {
        val node = findFocusedEditableNode() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val textLen = node.text?.length ?: 0
            val curEnd = node.textSelectionEnd.takeIf { it >= 0 } ?: textLen
            val nextPos = when (keyType) {
                KeyType.ARROW_LEFT -> (curEnd - 1).coerceAtLeast(0)
                KeyType.ARROW_RIGHT -> (curEnd + 1).coerceAtMost(textLen)
                KeyType.ARROW_UP -> 0
                KeyType.ARROW_DOWN -> textLen
                else -> curEnd
            }
            val args = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, nextPos)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, nextPos)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
        }
    }

    override fun onCopyRequested() {
        val node = findFocusedEditableNode()
        node?.performAction(AccessibilityNodeInfo.ACTION_COPY)
    }

    override fun onPasteRequested() {
        val node = findFocusedEditableNode()
        if (node != null) {
            node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        } else {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = cm?.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
            if (!clip.isNullOrEmpty()) {
                keyboardOverlayView?.scratchpadPreviewText =
                    (keyboardOverlayView?.scratchpadPreviewText ?: "") + clip
                KeyboardStateBus.updateSharedScratchpad(keyboardOverlayView?.scratchpadPreviewText ?: "")
            }
        }
    }

    override fun onClearRequested() {
        val node = findFocusedEditableNode()
        if (node != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
    }

    override fun onCloseKeyboardRequested() {
        hideGlobalKeyboardOverlay()
    }

    override fun onMinimizeToZeroBadgeRequested() {
        hideGlobalKeyboardOverlay()
        showFloatingZeroBadgeOverlay()
    }

    override fun onDragWindowDelta(dx: Float, dy: Float) {
        val wm = windowManager ?: return
        val view = keyboardOverlayView ?: return
        val params = keyboardLayoutParams ?: return
        params.y = (params.y - dy.toInt()).coerceIn(
            0,
            (resources.displayMetrics.heightPixels * 0.55f).toInt()
        )
        try {
            wm.updateViewLayout(view, params)
        } catch (_: Throwable) {
        }
    }
}
