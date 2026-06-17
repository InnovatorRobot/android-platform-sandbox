package com.mediaplatform.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Draws a compact spectrum module from FFT magnitude data.
 * Call [updateBands] from the main thread to refresh.
 */
class AudioVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF03DAC5.toInt() // teal
    }
    private val bgPaint = Paint().apply {
        color = 0x331E2A3A
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x22FFFFFF
        strokeWidth = 1.0f
    }
    private val peakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 2.0f
    }
    private val rect    = RectF()
    private var bands   = FloatArray(16)

    /** Update displayed bands. Must be called on the main thread. */
    fun updateBands(newBands: FloatArray) {
        bands = newBands.copyOf()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val mid = h * 0.5f
        canvas.drawLine(0f, mid, w, mid, gridPaint)
        canvas.drawLine(0f, h * 0.25f, w, h * 0.25f, gridPaint)
        canvas.drawLine(0f, h * 0.75f, w, h * 0.75f, gridPaint)

        val count     = bands.size
        val barSlot   = w / count
        val barWidth  = barSlot * 0.62f
        val gap       = (barSlot - barWidth) / 2f
        val maxHeight = h * 0.78f
        var peakX = 0f
        var peakY = h

        for (i in bands.indices) {
            val level = bands[i].coerceIn(0f, 1f)
            val barH  = (0.08f + level * 0.92f) * maxHeight
            val left  = i * barSlot + gap
            val top   = h - barH
            rect.set(left, top, left + barWidth, h)
            barPaint.alpha = (125 + (level * 130f).toInt()).coerceIn(125, 255)
            canvas.drawRoundRect(rect, 7f, 7f, barPaint)

            if (top < peakY) {
                peakY = top
                peakX = left + barWidth / 2f
            }
        }

        canvas.drawCircle(peakX, peakY.coerceAtLeast(8f), 4.5f, peakPaint)
    }
}
