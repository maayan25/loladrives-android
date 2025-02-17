package de.unisaarland.loladrives.CustomObjects
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi

class DynamicBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


    // Default values for properties
    var high: Float = 0.0f // Default value for high
    var low: Float = 0.0f  // Default value for low
    var max: Float = 0.0f
    var min: Float = 0.0f

    private val outlinePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

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

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val barHeightStart = height.toFloat() * 0.25f
        val barHeightEnd = height.toFloat() * 0.75f

        // Calculate the positions of the min and max values
        val minPosition = width.toFloat() * 0.15f
        val maxPosition = width.toFloat() * 0.85f
        val midPosition = width.toFloat() / 2


        val normalizedLow = (low - min) / (1.2f - min)
        val stepsLow = minPosition + (midPosition - minPosition) * normalizedLow
        val stepsHigh = ((maxPosition - midPosition) / max) * high

        // Calculate the positions of the low and high values
        val lowPosition = if (low > 1.2) midPosition else stepsLow
        var highPosition = midPosition + stepsHigh

        if (highPosition > maxPosition) {
            highPosition = maxPosition
        }

        // Determine the color for the rectangle
        val rectLeft = lowPosition
        val rectRight = highPosition
        canvas.drawRoundRect(0f, barHeightStart, width.toFloat(), barHeightEnd, 20f, 20f, outlinePaint)


        // Determine if the rectangle exceeds min/max
        if (min == 0f) {
            canvas.drawRect(rectLeft, barHeightStart, rectRight, barHeightEnd, normalPaint)
        }
        else if (max == 0f) {
            canvas.drawRect(rectLeft, barHeightStart, rectRight, barHeightEnd, normalPaint)
        } else {
            if (high > max && low < min) {
                canvas.drawRoundRect(rectLeft, barHeightStart, midPosition, barHeightEnd, 20f, 0f, belowMinPaint)
                canvas.drawRoundRect(midPosition, barHeightStart, rectRight, barHeightEnd, 0f, 20f, aboveMaxPaint)
            } else if (high > max && low > min) {
                // max is exceeded and min is not
                canvas.drawRoundRect(rectLeft, barHeightStart, midPosition, barHeightEnd, 20f, 0f, normalPaint)
                canvas.drawRoundRect(midPosition, barHeightStart, rectRight, barHeightEnd, 0f, 20f, aboveMaxPaint)
            } else if (high < max && low < min) {
                // min is exceeded and max is not
                canvas.drawRoundRect(rectLeft, barHeightStart, midPosition, barHeightEnd, 20f, 0f, belowMinPaint)
                canvas.drawRoundRect(midPosition, barHeightStart, rectRight, barHeightEnd, 0f, 20f, normalPaint)
            } else {
                // neither min nor max is exceeded
                canvas.drawRoundRect(rectLeft, barHeightStart, midPosition, barHeightEnd, 20f, 20f, normalPaint)
            }
        }
        Log.d("low", low.toString())
        Log.d("high", high.toString())
        Log.d("min", min.toString())
        Log.d("max", max.toString())

        // Calculate the line positions
        val lineHeightStart = height.toFloat() * 0.15f
        val lineHeightEnd = height.toFloat() * 0.85f

        // Draw min and max lines
        canvas.drawLine(minPosition, lineHeightStart, minPosition, lineHeightEnd, minMaxPaint)
        canvas.drawLine(maxPosition, lineHeightStart, maxPosition, lineHeightEnd, minMaxPaint)

        // Add labels for min and max
        val minLabel = String.format("%.2fm²/s³", min)
        val maxLabel = String.format("%.2fm²/s³", max)
        canvas.drawText(minLabel, minPosition, height.toFloat(), labelPaint)
        canvas.drawText(maxLabel, maxPosition, height.toFloat(), labelPaint)

//        // Add labels for low and high values above the bar
//        val lowLabel = String.format("%.2fm²/s³", low)
//        val highLabel = String.format("%.2fm²/s³", high)
//        val labelOffset = width.toFloat() / 8
//        canvas.drawText(lowLabel, midPosition - labelOffset, barHeightStart - 10, labelPaint)
//        canvas.drawText(highLabel, midPosition + labelOffset, barHeightStart - 10, labelPaint)
    }


}
