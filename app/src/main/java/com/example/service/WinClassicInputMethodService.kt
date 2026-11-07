package com.example.service

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import com.example.keyboard.ClassicWindowsKeyboardView
import com.example.keyboard.SoundAndHapticHelper
import com.example.keyboard.WinKeyboardActionListener
import com.example.model.KeyType
import com.example.model.KeyboardStateBus

/**
 * Full Android System InputMethodService (IME) for the Classic Windows HE/EN Keyboard.
 * Also intercepts hardware KEYCODE_0 long-presses when active so physical keypad phones
 * (including Android 4.4+ keypad devices) immediately pop up the full on-screen Windows keyboard.
 */
class WinClassicInputMethodService : InputMethodService(), WinKeyboardActionListener {

    private var keyboardView: ClassicWindowsKeyboardView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isZeroKeyDown = false
    private var zeroLongPressConsumed = false

    private val zeroKeyLongPressRunnable = Runnable {
        if (isZeroKeyDown) {
            zeroLongPressConsumed = true
            SoundAndHapticHelper.performZeroKeyPopUpFeedback(this)
            KeyboardStateBus.notifyZeroKeyLongPressTriggered(
                this,
                "מקש 0 פיזי (שירות מקלדת מערכת IME)"
            )
            @Suppress("DEPRECATION")
            requestShowSelf(0)
            keyboardView?.visibility = View.VISIBLE
            keyboardView?.invalidate()
        }
    }

    override fun onCreate() {
        super.onCreate()
        KeyboardStateBus.loadFromPrefs(this)
    }

    override fun onCreateInputView(): View {
        val view = ClassicWindowsKeyboardView(this).apply {
            showQuickScratchpadBar = false
            targetContextLabel = "מקלדת מערכת פעילה (IME)"
            actionListener = this@WinClassicInputMethodService
        }
        keyboardView = view
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        KeyboardStateBus.loadFromPrefs(this)
        keyboardView?.requestLayout()
        keyboardView?.invalidate()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val config = KeyboardStateBus.state.value
        if (config.interceptHardwareZeroKey &&
            (keyCode == KeyEvent.KEYCODE_0 || keyCode == KeyEvent.KEYCODE_NUMPAD_0)
        ) {
            if (event?.repeatCount == 0) {
                isZeroKeyDown = true
                zeroLongPressConsumed = false
                handler.removeCallbacks(zeroKeyLongPressRunnable)
                handler.postDelayed(
                    zeroKeyLongPressRunnable,
                    config.longPressZeroThresholdMs.toLong()
                )
            }
            if ((event?.repeatCount ?: 0) > 0 && zeroLongPressConsumed) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_0 || keyCode == KeyEvent.KEYCODE_NUMPAD_0) {
            isZeroKeyDown = false
            handler.removeCallbacks(zeroKeyLongPressRunnable)
            if (zeroLongPressConsumed) {
                zeroLongPressConsumed = false
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onCommitText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun onBackspace() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }

    override fun onEnter() {
        val ic = currentInputConnection ?: return
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    override fun onArrowKey(keyType: KeyType) {
        val ic = currentInputConnection ?: return
        val code = when (keyType) {
            KeyType.ARROW_LEFT -> KeyEvent.KEYCODE_DPAD_LEFT
            KeyType.ARROW_RIGHT -> KeyEvent.KEYCODE_DPAD_RIGHT
            KeyType.ARROW_UP -> KeyEvent.KEYCODE_DPAD_UP
            KeyType.ARROW_DOWN -> KeyEvent.KEYCODE_DPAD_DOWN
            else -> return
        }
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    override fun onCopyRequested() {
        val ic = currentInputConnection ?: return
        val extracted = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)?.text
        if (!extracted.isNullOrEmpty()) {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(android.content.ClipData.newPlainText("IME", extracted))
        }
    }

    override fun onPasteRequested() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val pasteText = cm?.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
        if (!pasteText.isNullOrEmpty()) {
            currentInputConnection?.commitText(pasteText, 1)
        }
    }

    override fun onClearRequested() {
        val ic = currentInputConnection ?: return
        ic.performContextMenuAction(android.R.id.selectAll)
        ic.commitText("", 1)
    }

    override fun onCloseKeyboardRequested() {
        requestHideSelf(0)
    }

    override fun onMinimizeToZeroBadgeRequested() {
        requestHideSelf(0)
    }

    override fun onDragWindowDelta(dx: Float, dy: Float) {
        // Docked IME view does not float freely unless in overlay mode
    }
}
