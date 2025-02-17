package de.unisaarland.loladrives.CustomObjects

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi

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

    private val outlinePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val normalPaint = Paint().apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL

    }

    private val aboveMaxPaint = Paint().apply {
        color = Color.parseColor("#c55a57")
        style = Paint.Style.FILL

    }

    private val belowMinPaint = Paint().apply {
        color = Color.parseColor("#c55a57")
        style = Paint.Style.FILL

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("DefaultLocale")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val barHeightStart = height.toFloat() * 0.25f
        val barHeightEnd = height.toFloat() * 0.75f

        val offset = 0f
        val barWidthStart = width.toFloat() * offset
        val barWidthEnd = width.toFloat() * (1 - offset)
        val barWidth = barWidthEnd - barWidthStart

        // Draw outline of the bar
        canvas.drawRoundRect(barWidthStart, barHeightStart, barWidthEnd, barHeightEnd, 20f, 20f, outlinePaint)


        // Draw the proportion bar here
        val widthPercent = barWidthStart + (barWidth * (percent/100))
        if (percent < min) {
            canvas.drawRoundRect(barWidthStart, barHeightStart, widthPercent , barHeightEnd, 20f, 20f, belowMinPaint)
        } else if (percent > max) {
            canvas.drawRoundRect(barWidthStart,barHeightStart,widthPercent , barHeightEnd, 20f, 20f, aboveMaxPaint)
        } else {
            canvas.drawRoundRect(barWidthStart, barHeightStart, widthPercent , barHeightEnd, 20f, 20f, normalPaint)
        }

        val minBarWidth = barWidthStart + (barWidth * (min/100))
        val maxBarWidth = barWidthStart + (barWidth * (max/100))

        // Draw the min and max limits on top of the bar
        canvas.drawLine(minBarWidth, height.toFloat()*0.15f, minBarWidth , height.toFloat()*0.85f, proportionPaint)
        canvas.drawLine(maxBarWidth, height.toFloat()*0.15f, maxBarWidth , height.toFloat()*0.85f, proportionPaint)

        // Add labels for min and max
        val minLabel = String.format("%.1f", min)
        val maxLabel = String.format("%.1f", max)
        canvas.drawText(minLabel + "%", minBarWidth, height.toFloat()*0.95f, labelPaint)
        canvas.drawText(maxLabel + "%", maxBarWidth, height.toFloat()*0.95f, labelPaint)
//       //Add label for the current value
//        val percentLabel = String.format("%.1f", percent)
//        val percentWidthBar = barWidthStart + (barWidth * (percent/100))
//        canvas.drawText(percentLabel, percentWidthBar, height.toFloat()*0.15f, labelPaint)
    }

}