package de.unisaarland.loladrives.CustomObjects
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View

class DynamicBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


    // Default values for properties
    var high: Float = 0f
    var low: Float = 0f
    var max: Float = 0f
    var min: Float = 0f

    private val minMaxPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 4f
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val barHeightStart = height.toFloat() * 0.25f
        val barHeightEnd = height.toFloat() * 0.75f

        // Calculate the positions of the min and max values
        val minPosition = width.toFloat() * 0.15f
        val maxPosition = width.toFloat() * 0.85f
        val midPosition = width.toFloat() / 2

        // Calculate the steps for low and high values
        val stepsLow = low * (midPosition - minPosition) / (min-0.8f)
//        val stepsLow = low * ((min-0.8f)/ (midPosition - minPosition))
        val stepsHigh = (maxPosition - midPosition) * (high - midPosition) / (maxPosition - midPosition)
        Log.d("DynamicBar", "mid P: $midPosition")
        Log.d("DynamicBar", "min P: $minPosition")
        Log.d("DynamicBar", "width: $width")
        Log.d("DynamicBar", "d: ${(midPosition - minPosition)}")
        Log.d("DynamicBar", "v: ${(min-0.8f)}")
        Log.d("DynamicBar", "scale: ${(midPosition - minPosition) / (min-0.8f)}")
        Log.d("DynamicBar", "Low: $stepsLow")
        Log.d("DynamicBar", "High: $stepsHigh")

        // Calculate the positions of the low and high values
        val lowPosition = if (low >0.8) midPosition else minPosition - stepsLow
        val highPosition = midPosition + stepsHigh

        // Determine the color for the rectangle
        val rectLeft = lowPosition
        val rectRight = highPosition

        // Determine if the rectangle exceeds min/max
        if (high > max && low < min) {
            canvas.drawRect(rectLeft, barHeightStart, midPosition, barHeightEnd, belowMinPaint)
            canvas.drawRect(midPosition, barHeightStart, rectRight, barHeightEnd, aboveMaxPaint)
        } else if (high > max) {
            Log.d("DynamicBar", "high > max")
            Log.d("DynamicBar", "Left: $rectLeft")
            Log.d("DynamicBar", "Right: $rectRight")

            canvas.drawRect(rectLeft, barHeightStart, midPosition, barHeightEnd, normalPaint)
            canvas.drawRect(midPosition, barHeightStart, rectRight, barHeightEnd, aboveMaxPaint)
        } else if (low < min) {
            canvas.drawRect(rectLeft, barHeightStart, midPosition, barHeightEnd, belowMinPaint)
            canvas.drawRect(midPosition, barHeightStart, rectRight, barHeightEnd, normalPaint)
        } else {
            canvas.drawRect(rectLeft, barHeightStart, rectRight, barHeightEnd, normalPaint)
        }

        // Calculate the line positions
        val lineHeightStart = height.toFloat() * 0.15f
        val lineHeightEnd = height.toFloat() * 0.85f

        // Draw min and max lines
        canvas.drawLine(minPosition, lineHeightStart, minPosition, lineHeightEnd, minMaxPaint)
        canvas.drawLine(maxPosition, lineHeightStart, maxPosition, lineHeightEnd, minMaxPaint)

        // Add labels for min and max
        canvas.drawText(min.toString(), minPosition, height.toFloat(), labelPaint)
        canvas.drawText(max.toString(), maxPosition, height.toFloat(), labelPaint)

        // Add labels for low and high values above the bar
        val lowLabel = String.format("%.2fm²/s³", low)
        val highLabel = String.format("%.2fm²/s³", high)
        val labelOffset = width.toFloat() / 8
        canvas.drawText(lowLabel, midPosition - labelOffset, barHeightStart - 10, labelPaint)
        canvas.drawText(highLabel, midPosition + labelOffset, barHeightStart - 10, labelPaint)
    }


}
