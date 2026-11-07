package com.example.service

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import com.example.keyboard.ClassicWindowsKeyboardView
import com.example.keyboard.FloatingZeroKeyBadgeView
import com.example.keyboard.WinKeyboardActionListener
import com.example.model.KeyType
import com.example.model.KeyboardStateBus

/**
 * Standalone Global Window Overlay Service that draws the floating '0' trigger badge and
 * Classic Windows HE/EN Keyboard over the Home Screen and other apps whenever
 * SYSTEM_ALERT_WINDOW permission is granted (works down to Android 4.4 API 19).
 */
class GlobalKeyboardOverlayService : Service(), WinKeyboardActionListener {

    companion object {
        const val ACTION_SHOW_BADGE = "com.example.action.SHOW_ZERO_BADGE"
        const val ACTION_POPUP_KEYBOARD = "com.example.action.POPUP_WIN_KEYBOARD"
        const val ACTION_STOP_OVERLAY = "com.example.action.STOP_OVERLAY"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun canDrawOverlayCompat(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                // On Android 4.4 (API 19) to 5.1 (API 22), SYSTEM_ALERT_WINDOW is granted at install time
                true
            }
        }
    }

    private var windowManager: WindowManager? = null
    private var badgeView: FloatingZeroKeyBadgeView? = null
    private var badgeParams: WindowManager.LayoutParams? = null
    private var keyboardView: ClassicWindowsKeyboardView? = null
    private var keyboardParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        KeyboardStateBus.loadFromPrefs(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!canDrawOverlayCompat(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_STOP_OVERLAY -> {
                removeAllViews()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_POPUP_KEYBOARD -> {
                ensureBadgeShown()
                showKeyboardOverlay()
            }
            else -> {
                ensureBadgeShown()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeAllViews()
        isRunning = false
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun resolveOverlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            // Android 4.4 (API 19 KitKat) through Android 7.1 (API 25)
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun ensureBadgeShown() {
        val wm = windowManager ?: return
        if (badgeView != null) return

        val badge = FloatingZeroKeyBadgeView(this).apply {
            onLongPressZeroTriggered = {
                KeyboardStateBus.notifyZeroKeyLongPressTriggered(
                    this@GlobalKeyboardOverlayService,
                    "לחיצה ארוכה על מקש 0 הצף (מעל כל המסך)"
                )
                showKeyboardOverlay()
            }
            onDragBadgeDelta = { dx, dy ->
                badgeParams?.let { p ->
                    p.x = (p.x + dx.toInt()).coerceAtLeast(0)
                    p.y = (p.y + dy.toInt()).coerceAtLeast(0)
                    try {
                        wm.updateViewLayout(this, p)
                    } catch (_: Throwable) {
                    }
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            resolveOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (resources.displayMetrics.widthPixels - (76 * resources.displayMetrics.density).toInt()).coerceAtLeast(12)
            y = (resources.displayMetrics.heightPixels * 0.35f).toInt()
        }

        try {
            wm.addView(badge, params)
            badgeView = badge
            badgeParams = params
        } catch (_: Throwable) {
        }
    }

    private fun showKeyboardOverlay() {
        val wm = windowManager ?: return
        if (keyboardView != null) {
            keyboardView?.invalidate()
            return
        }

        val view = ClassicWindowsKeyboardView(this).apply {
            showQuickScratchpadBar = true
            targetContextLabel = "שכבה גלובלית (מסך הבית / אפליקציות)"
            scratchpadPreviewText = KeyboardStateBus.sharedScratchpadText.value
            actionListener = this@GlobalKeyboardOverlayService
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            resolveOverlayType(),
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
            keyboardView = view
            keyboardParams = params
            KeyboardStateBus.setOverlayKeyboardVisible(true, "שכבת מקלדת ווינדוס גלובלית פעילה")
        } catch (_: Throwable) {
        }
    }

    private fun hideKeyboardOverlay() {
        val wm = windowManager ?: return
        keyboardView?.let {
            try {
                wm.removeView(it)
            } catch (_: Throwable) {
            }
        }
        keyboardView = null
        keyboardParams = null
        KeyboardStateBus.setOverlayKeyboardVisible(false)
    }

    private fun removeAllViews() {
        hideKeyboardOverlay()
        val wm = windowManager ?: return
        badgeView?.let {
            try {
                wm.removeView(it)
            } catch (_: Throwable) {
            }
        }
        badgeView = null
        badgeParams = null
    }

    override fun onCommitText(text: String) {
        // Also forward to ZeroKeyAccessibilityService if active so it types directly into the focused app
        ZeroKeyAccessibilityService.instance?.onCommitText(text)
    }

    override fun onBackspace() {
        ZeroKeyAccessibilityService.instance?.onBackspace()
    }

    override fun onEnter() {
        ZeroKeyAccessibilityService.instance?.onEnter()
    }

    override fun onArrowKey(keyType: KeyType) {
        ZeroKeyAccessibilityService.instance?.onArrowKey(keyType)
    }

    override fun onCopyRequested() {
        val text = keyboardView?.scratchpadPreviewText ?: ""
        if (text.isNotEmpty()) {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("WinKeyOverlay", text))
        }
    }

    override fun onPasteRequested() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = cm?.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
        if (!clip.isNullOrEmpty()) {
            val updated = (keyboardView?.scratchpadPreviewText ?: "") + clip
            keyboardView?.scratchpadPreviewText = updated
            KeyboardStateBus.updateSharedScratchpad(updated)
            ZeroKeyAccessibilityService.instance?.onCommitText(clip)
        }
    }

    override fun onClearRequested() {
        ZeroKeyAccessibilityService.instance?.onClearRequested()
    }

    override fun onCloseKeyboardRequested() {
        hideKeyboardOverlay()
    }

    override fun onMinimizeToZeroBadgeRequested() {
        hideKeyboardOverlay()
        ensureBadgeShown()
    }

    override fun onDragWindowDelta(dx: Float, dy: Float) {
        val wm = windowManager ?: return
        val view = keyboardView ?: return
        val params = keyboardParams ?: return
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
