package de.unisaarland.loladrives.CustomObjects

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class ProportionBar  @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Default values for properties
    var percent: Float = 0f
    var min: Float = 23f
    var max: Float = 44f

    private val proportionPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val labelPaint = Paint().apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = 35f
        color = Color.BLACK
    }

    private val normalPaint = Paint().apply {
        color = Color.GREEN
    }

    private val aboveMaxPaint = Paint().apply {
        color = Color.RED
    }

    private val belowMinPaint = Paint().apply {
        color = Color.RED
    }

    @SuppressLint("DefaultLocale")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val barWidth = width.toFloat() * 0.8f

        // Draw the proportion bar here
        if (percent < min) {
            canvas.drawRect(0f, 0f, barWidth * percent , height.toFloat(), belowMinPaint)
        } else if (percent > max) {
            canvas.drawRect(0f, 0f, barWidth * percent , height.toFloat(), aboveMaxPaint)
        } else {
            canvas.drawRect(0f, 0f, barWidth * percent , height.toFloat(), normalPaint)
        }

        // Draw the rounded corners on the proportion bar
        canvas.drawCircle(barWidth * percent, height.toFloat() / 2, 10f, proportionPaint)
        canvas.drawCircle(barWidth * percent, height.toFloat() / 2, 10f, proportionPaint)


        // Draw the min and max limits on top of the bar
        canvas.drawLine(barWidth * min, 0f, barWidth * min , height.toFloat(), proportionPaint)
        canvas.drawLine(barWidth * max, 0f, barWidth * max , height.toFloat(), proportionPaint)


        // Add labels for min and max
        val minLabel = String.format("%.1f", min)
        val maxLabel = String.format("%.1f", max)
        canvas.drawText(minLabel, barWidth * min, height.toFloat(), labelPaint)
        canvas.drawText(maxLabel, barWidth * max, height.toFloat(), labelPaint)
        //Add label for the current value
        val percentLabel = String.format("%.1f", percent)
        canvas.drawText(percentLabel, barWidth * percent, height.toFloat(), labelPaint)
    }

}