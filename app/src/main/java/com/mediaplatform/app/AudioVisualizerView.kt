package com.mediaplatform.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Draws a semi-transparent 16-band equaliser bar chart from FFT magnitude data.
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
        color = 0x99000000.toInt() // semi-transparent black
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

        val count     = bands.size
        val barSlot   = w / count
        val barWidth  = barSlot * 0.7f
        val gap       = (barSlot - barWidth) / 2f
        val maxHeight = h * 0.85f

        for (i in bands.indices) {
            val barH  = bands[i].coerceIn(0f, 1f) * maxHeight
            val left  = i * barSlot + gap
            val top   = h - barH
            rect.set(left, top, left + barWidth, h)
            // Alpha driven by magnitude: dim bars when quiet
            barPaint.alpha = (160 + (bands[i] * 95f).toInt()).coerceIn(160, 255)
            canvas.drawRoundRect(rect, 4f, 4f, barPaint)
        }
    }
}
