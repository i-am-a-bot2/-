package com.example.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.model.KeyboardStateBus
import kotlin.math.abs

/**
 * A draggable Classic Windows 3D beveled "[ 0 ]" hardware-key trigger badge that floats over
 * the Home Screen and any app. Long-pressing this "0" key for the configured threshold (e.g. 450ms)
 * pops up the full Classic Windows HE/EN Keyboard anywhere on the device.
 */
class FloatingZeroKeyBadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onLongPressZeroTriggered: (() -> Unit)? = null
    var onDragBadgeDelta: ((dx: Float, dy: Float) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private var isHolding = false
    private var holdStartMs = 0L
    private var holdProgress = 0f
    private var isDragging = false
    private var downRawX = 0f
    private var downRawY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            if (!isHolding) return
            val threshold = KeyboardStateBus.state.value.longPressZeroThresholdMs.coerceAtLeast(250)
            val elapsed = SystemClock.uptimeMillis() - holdStartMs
            holdProgress = (elapsed.toFloat() / threshold.toFloat()).coerceIn(0f, 1f)
            invalidate()
            if (elapsed >= threshold) {
                isHolding = false
                holdProgress = 0f
                SoundAndHapticHelper.performZeroKeyPopUpFeedback(context)
                onLongPressZeroTriggered?.invoke()
                invalidate()
            } else {
                handler.postDelayed(this, 16L)
            }
        }
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f * density
        strokeCap = Paint.Cap.ROUND
        color = 0xFFFFB300.toInt()
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val arcRect = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = (62 * density).toInt()
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val pad = 4f * density
        val b = 2f * density

        // Classic Windows 3D Beveled Keycap Frame
        val sunken = isHolding
        paint.style = Paint.Style.FILL
        paint.color = if (sunken) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        canvas.drawRoundRect(pad, pad, w - pad, h - pad, 8f * density, 8f * density, paint)

        paint.color = if (sunken) 0xFF002266.toInt() else 0xFF003380.toInt()
        canvas.drawRoundRect(pad + b, pad + b, w - pad - b, h - pad - b, 6f * density, 6f * density, paint)

        // Inner Beige Keycap Center
        val innerPad = 9f * density
        paint.color = if (sunken) 0xFFE5DFC5.toInt() else 0xFFECE9D8.toInt()
        canvas.drawRoundRect(
            innerPad,
            innerPad,
            w - innerPad,
            h - innerPad,
            5f * density,
            5f * density,
            paint
        )

        // "0" Main Legend
        textPaint.color = 0xFF00205B.toInt()
        textPaint.textSize = 20f * scaledDensity
        canvas.drawText("0", w / 2f, h * 0.54f, textPaint)

        // Sub-caption "החזק"
        textPaint.color = 0xFF004D40.toInt()
        textPaint.textSize = 8.5f * scaledDensity
        canvas.drawText("החזק", w / 2f, h * 0.76f, textPaint)

        // Hold progress ring
        if (isHolding && holdProgress > 0f) {
            arcRect.set(2.5f * density, 2.5f * density, w - 2.5f * density, h - 2.5f * density)
            canvas.drawArc(arcRect, -90f, 360f * holdProgress, false, arcPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isHolding = true
                isDragging = false
                holdStartMs = SystemClock.uptimeMillis()
                holdProgress = 0f
                downRawX = event.rawX
                downRawY = event.rawY
                lastRawX = event.rawX
                lastRawY = event.rawY
                SoundAndHapticHelper.performKeyTapFeedback(context, false)
                handler.removeCallbacks(progressRunnable)
                handler.post(progressRunnable)
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val totalDx = abs(event.rawX - downRawX)
                val totalDy = abs(event.rawY - downRawY)
                if (totalDx > 12f * density || totalDy > 12f * density) {
                    isDragging = true
                    isHolding = false
                    holdProgress = 0f
                    handler.removeCallbacks(progressRunnable)
                    invalidate()
                }
                if (isDragging) {
                    val dx = event.rawX - lastRawX
                    val dy = event.rawY - lastRawY
                    onDragBadgeDelta?.invoke(dx, dy)
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isHolding = false
                isDragging = false
                holdProgress = 0f
                handler.removeCallbacks(progressRunnable)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
