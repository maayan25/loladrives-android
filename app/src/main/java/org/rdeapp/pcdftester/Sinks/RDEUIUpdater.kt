package org.rdeapp.pcdftester.Sinks

import android.graphics.Color
import android.os.Build
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import de.unisaarland.loladrives.Fragments.HomeFragment
import de.unisaarland.loladrives.Fragments.RDE.RDEFragment
import de.unisaarland.loladrives.R
import kotlinx.android.synthetic.main.fragment_home.homeTotalRDETime
import kotlinx.android.synthetic.main.fragment_r_d_e.guidelineCircleMotorwayHigh
import kotlinx.android.synthetic.main.fragment_r_d_e.guidelineCircleMotorwayLow
import kotlinx.android.synthetic.main.fragment_r_d_e.guidelineCircleRuralHigh
import kotlinx.android.synthetic.main.fragment_r_d_e.guidelineCircleRuralLow
import kotlinx.android.synthetic.main.fragment_r_d_e.guidelineCircleUrbanHigh
import kotlinx.android.synthetic.main.fragment_r_d_e.guidelineCircleUrbanLow
import kotlinx.android.synthetic.main.fragment_r_d_e.guidelineDynamicMarkerHighMotorway
import kotlinx.android.synthetic.main.fragment_r_d_e.guidelineDynamicMarkerHighRural
import kotlinx.android.synthetic.main.fragment_r_d_e.guidelineDynamicMarkerHighUrban
import kotlinx.android.synthetic.main.fragment_r_d_e.guidelineDynamicMarkerLowMotorway
import kotlinx.android.synthetic.main.fragment_r_d_e.guidelineDynamicMarkerLowRural
import kotlinx.android.synthetic.main.fragment_r_d_e.guidelineDynamicMarkerLowUrban
import kotlinx.android.synthetic.main.fragment_r_d_e.progressBarDistance
import kotlinx.android.synthetic.main.fragment_r_d_e.progressBarTime
import kotlinx.android.synthetic.main.fragment_r_d_e.roundCornerProgressBarMotorway
import kotlinx.android.synthetic.main.fragment_r_d_e.roundCornerProgressBarNOX
import kotlinx.android.synthetic.main.fragment_r_d_e.roundCornerProgressBarRural
import kotlinx.android.synthetic.main.fragment_r_d_e.roundCornerProgressBarUrban
import kotlinx.android.synthetic.main.fragment_r_d_e.textViewMotorwayDistance
import kotlinx.android.synthetic.main.fragment_r_d_e.textViewMotorwayTime
import kotlinx.android.synthetic.main.fragment_r_d_e.textViewNOxCurrentValue
import kotlinx.android.synthetic.main.fragment_r_d_e.textViewRDEPrompt
import kotlinx.android.synthetic.main.fragment_r_d_e.textViewRuralDistance
import kotlinx.android.synthetic.main.fragment_r_d_e.textViewRuralTime
import kotlinx.android.synthetic.main.fragment_r_d_e.textViewTotalDistance
import kotlinx.android.synthetic.main.fragment_r_d_e.textViewTotalTime
import kotlinx.android.synthetic.main.fragment_r_d_e.textViewUrbanDistance
import kotlinx.android.synthetic.main.fragment_r_d_e.textViewUrbanTime
import kotlinx.android.synthetic.main.fragment_r_d_e.validityImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


/**
 * UI class for the [RDEFragment].
 * @property inputChannel Channel over which we receive the RTLola results from the [RDEValidator] as DoubleArrays.
 * @property fragment The current [RDEFragment] we are updating.
 */
class RDEUIUpdater(
    private val inputChannel: ReceiveChannel<DoubleArray>,
    val fragment: RDEFragment
): TextToSpeech.OnInitListener {
    // The current expected distance (may change during track).
    private var expectedDistance = fragment.distance
    private var started = false
    private var tts: TextToSpeech? = TextToSpeech(fragment.requireActivity(), this)
    // Constants concerning the NOx values, we take 200[mg/km] to be the largest amount we may display in the NOx-Bar.
    private val noxMaximum = 0.2 // [g/km]
    private val noxThr1 = 0.12 // [g/km]
    private val noxThr2 = 0.168 // [g/km]

    /**
     * Suspending function which receives (blocking) RTLola results over the [inputChannel] and updates the UI accordingly.
     */
    suspend fun start() {
        for (outputs in inputChannel) {
            // If this is the first received RTLola result, hide the Progress-Indicators and show total time and total
            // duration.
            if (!started) {
                fragment.progressBarTime.visibility = View.INVISIBLE
                fragment.progressBarDistance.visibility = View.INVISIBLE
                fragment.textViewTotalTime.visibility = View.VISIBLE
                fragment.textViewTotalDistance.visibility = View.VISIBLE
                fragment.textViewRDEPrompt.visibility = View.VISIBLE
                started = true
            }

            try {
                // Update all the simple TextViews.
                fragment.textViewTotalDistance.text = convertMeters(outputs[0].toLong())
                fragment.textViewUrbanDistance.text = convertMeters(outputs[1].toLong())
                fragment.textViewRuralDistance.text = convertMeters(outputs[2].toLong())
                fragment.textViewMotorwayDistance.text = convertMeters(outputs[3].toLong())
                fragment.textViewUrbanTime.text = convertSeconds(outputs[4].toLong())
                fragment.textViewRuralTime.text = convertSeconds(outputs[5].toLong())
                fragment.textViewMotorwayTime.text = convertSeconds(outputs[6].toLong())
                fragment.textViewTotalTime.text = convertSeconds(outputs[4].toLong() + outputs[5].toLong() + outputs[6].toLong())

                // Update the distance ProgressBars (total[0], urban[1], rural[2], motorway[3])
                handleDistance(outputs[0], outputs[1], outputs[2], outputs[3])

                // Update the Dynamics-Markers (grey balls)
                handleDynamics(
                    outputs[7],
                    outputs[8],
                    outputs[9],
                    outputs[13],
                    outputs[14],
                    outputs[15],
                    outputs[10],
                    outputs[11],
                    outputs[12]
                )

                // Update the NOx ProgessBar and TextView
                fragment.roundCornerProgressBarNOX.progress = (outputs[16] / noxMaximum * 100.0).toFloat()
                fragment.textViewNOxCurrentValue.text = convert(outputs[16] * 1000.0, "mg/km")

                // Change the color of the NOx ProgressBar, depending on exceedance of the thresholds.
                when {
                    outputs[16] > noxThr2 -> {
                        fragment.roundCornerProgressBarNOX.progressColor = ContextCompat.getColor(fragment.requireContext(), R.color.redColor)
                    }
                    outputs[16] > noxThr1 -> {
                        fragment.roundCornerProgressBarNOX.progressColor = ContextCompat.getColor(fragment.requireContext(), R.color.questionYellow)
                    }
                    else -> {
                        fragment.roundCornerProgressBarNOX.progressColor = ContextCompat.getColor(fragment.requireContext(), R.color.greenColor)
                    }
                }

                // Update the Validity Icon (green checkmark, yellow questionmark or red cross).
                when {
                    outputs[17] == 1.0 -> {
                        fragment.validityImageView.setImageResource(R.drawable.bt_connected)
                    }
                    outputs[18] == 1.0 -> {
                        fragment.validityImageView.setImageResource(R.drawable.bt_not_connected)
                    }
                    else -> {
                        fragment.validityImageView.setImageResource(R.drawable.yellow_question)
                    }
                }

                //
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Updates the Dynamic-Markers according to received RTLola results.
     */
    private fun handleDynamics(
        u_avg_v: Double,
        r_avg_v: Double,
        m_avg_v: Double,
        u_rpa: Double,
        r_rpa: Double,
        m_rpa: Double,
        u_va_pct: Double,
        r_va_pct: Double,
        m_va_pct: Double
    ) {
        // RPA Threshold-Markers
        val offsetRpa = 0.35 // GuidelineDynamicsBarLow Percentage
        val boundaryRpa = 0.605
        val lengthRpa = boundaryRpa - offsetRpa

        val maxRpa = 0.3 // Realistic maximum RPA

        // Calculate Horizontal Marker Positions
        val uRpaThreshold = -0.0016 * u_avg_v + 0.1755
        val rRpaThreshold = -0.0016 * r_avg_v + 0.1755
        val mRpaThreshold = if (m_avg_v <= 94.05) { -0.0016 * m_avg_v + 0.1755 } else { 0.025 }

        val uRpaMarkerPercentage = uRpaThreshold / maxRpa
        val rRpaMarkerPercentage = rRpaThreshold / maxRpa
        val mRpaMarkerPercentage = mRpaThreshold / maxRpa

        fragment.guidelineDynamicMarkerLowUrban.setGuidelinePercent(((lengthRpa * uRpaMarkerPercentage) + offsetRpa).toFloat())
        fragment.guidelineDynamicMarkerLowRural.setGuidelinePercent(((lengthRpa * rRpaMarkerPercentage) + offsetRpa).toFloat())
        fragment.guidelineDynamicMarkerLowMotorway.setGuidelinePercent(((lengthRpa * mRpaMarkerPercentage) + offsetRpa).toFloat())

        // PCT95 Threshold-Markers
        val offsetPct = 0.62
        val boundaryPct = 0.88
        val lengthPct = boundaryPct - offsetPct

        val maxPct = 35

        // Calculate Horizontal Marker Positions
        val uPctThreshold = 0.136 * u_avg_v + 14.44
        val rPctThreshold = if (r_avg_v <= 74.6) { 0.136 * r_avg_v + 14.44 } else { 0.0742 * r_avg_v + 18.966 }
        val mPctThreshold = 0.0742 * m_avg_v + 18.966

        val uPctMarkerPercentage = uPctThreshold / maxPct
        val rPctMarkerPercentage = rPctThreshold / maxPct
        val mPctMarkerPercentage = mPctThreshold / maxPct

        fragment.guidelineDynamicMarkerHighUrban.setGuidelinePercent(((lengthPct * uPctMarkerPercentage) + offsetPct).toFloat())
        fragment.guidelineDynamicMarkerHighRural.setGuidelinePercent(((lengthPct * rPctMarkerPercentage) + offsetPct).toFloat())
        fragment.guidelineDynamicMarkerHighMotorway.setGuidelinePercent(((lengthPct * mPctMarkerPercentage) + offsetPct).toFloat())

        // Calculate RPA Ball Positions
        val uRpaBallPercentage = u_rpa / maxRpa
        val rRpaBallPercentage = r_rpa / maxRpa
        val mRpaBallPercentage = m_rpa / maxRpa

        fragment.guidelineCircleUrbanLow.setGuidelinePercent(
            (lengthRpa * uRpaBallPercentage + offsetRpa).toFloat().coerceAtMost(boundaryRpa.toFloat())
        )
        fragment.guidelineCircleRuralLow.setGuidelinePercent(
            (lengthRpa * rRpaBallPercentage + offsetRpa).toFloat().coerceAtMost(boundaryRpa.toFloat())
        )
        fragment.guidelineCircleMotorwayLow.setGuidelinePercent(
            (lengthRpa * mRpaBallPercentage + offsetRpa).toFloat().coerceAtMost(boundaryRpa.toFloat())
        )

        // Calculate PCT Ball Positions
        val uPctBallPercentage = u_va_pct / maxPct
        val rPctBallPercentage = r_va_pct / maxPct
        val mPctBallPercentage = m_va_pct / maxPct

        fragment.guidelineCircleUrbanHigh.setGuidelinePercent(
            (lengthPct * uPctBallPercentage + offsetPct).toFloat().coerceAtMost(boundaryPct.toFloat())
        )
        fragment.guidelineCircleRuralHigh.setGuidelinePercent(
            (lengthPct * rPctBallPercentage + offsetPct).toFloat().coerceAtMost(boundaryPct.toFloat())
        )
        fragment.guidelineCircleMotorwayHigh.setGuidelinePercent(
            (lengthPct * mPctBallPercentage + offsetPct).toFloat().coerceAtMost(boundaryPct.toFloat())
        )
    }

    /**
     * Update the distance ProgressBars according to the received RTLola results.
     */
    private fun handleDistance(
        totalDistance: Double,
        urbanDistance: Double,
        ruralDistance: Double,
        motorwayDistance: Double
    ) {
        // TODO: change expected distance from km to m, makes things easier
        expectedDistance = fragment.distance
        /*
        Update Interval-Markers if their position depending on the current expected distance and make sure this is not
        below 16km.
         */
        fragment.initIntervalMarkers(16.0 / expectedDistance)

        /*
        Check whether the current expected distance is exceeded, either by the total distance (all segment-distances still
        in boundaries) or by a segment exceeding the upper relative limit.
         */
        expectedDistance = maxOf(expectedDistance, totalDistance / 1000.0)

        if (urbanDistance / 1000.0 / expectedDistance > 0.44) {
            expectedDistance = urbanDistance / 1000.0 / 0.44
        }
        if (ruralDistance / 1000.0 / expectedDistance > 0.43) {
            expectedDistance = ruralDistance / 1000.0 / 0.43
        }
        if (motorwayDistance / 1000.0 / expectedDistance > 0.43) {
            expectedDistance = motorwayDistance / 1000.0 / 0.43
        }

        // Distance Progress Bars
        val urbanProgress = fragment.roundCornerProgressBarUrban
        val ruralProgress = fragment.roundCornerProgressBarRural
        val motorwayProgress = fragment.roundCornerProgressBarMotorway

        urbanProgress.progress = urbanDistance.toFloat() / 1000 / expectedDistance.toFloat() * 2 * 100
        ruralProgress.progress = ruralDistance.toFloat() / 1000 / expectedDistance.toFloat() * 2 * 100
        motorwayProgress.progress = motorwayDistance.toFloat() / 1000 / expectedDistance.toFloat() * 2 * 100

        fragment.distance = expectedDistance
    }

    /**
     * Update the prompt for improving the driving style according to the received RTLola results.
     */
    private fun handlePrompt(
        totalDistance: Double,
        urbanDistance: Double,
        ruralDistance: Double,
        motorwayDistance: Double
    ) {
        expectedDistance = fragment.distance
        val urbanProportion = urbanDistance / expectedDistance
        val ruralProportion = ruralDistance / expectedDistance
        val motorwayProportion = motorwayDistance / expectedDistance

        if (urbanProportion > 0.44 || ruralProportion> 0.43 || motorwayProportion > 0.43) {
            fragment.textViewRDEPrompt.text = "This RDE test will be invalid...."
            fragment.textViewRDEPrompt.setTextColor(Color.RED)
        }
        if (totalDistance > expectedDistance/2){
            if (urbanProportion > 0.29){
                if (ruralProportion < 0.23) {
                    if (motorwayProportion > 0.23) {
                        fragment.textViewRDEPrompt.text = "Aim for more rural driving"
                        fragment.textViewRDEPrompt.setTextColor(Color.BLACK)
                    } else {
                        // Rural and Motorway have not passed yet
                        fragment.textViewRDEPrompt.text = "Aim for a higher driving speed"
                        fragment.textViewRDEPrompt.setTextColor(Color.GREEN)
                    }
                } else if (motorwayProportion < 0.23) {
                    fragment.textViewRDEPrompt.text = "Aim for more motorway driving"
                    fragment.textViewRDEPrompt.setTextColor(Color.BLACK)
                }
            } else {
                if (motorwayProportion < 0.23) {
                    if (ruralProportion > 0.23) {
                        fragment.textViewRDEPrompt.text = "Aim for more motorway driving"
                        fragment.textViewRDEPrompt.setTextColor(Color.BLACK)
                    } else {
                        // Rural and Motorway have not passed yet
                        fragment.textViewRDEPrompt.text = "Aim for a lower driving speed"
                        fragment.textViewRDEPrompt.setTextColor(Color.GREEN)
                    }
                } else if (ruralProportion < 0.23) {
                    fragment.textViewRDEPrompt.text = "Aim for more rural driving"
                    fragment.textViewRDEPrompt.setTextColor(Color.BLACK)
                }
            }
        }
    }

    companion object {
        fun convertSeconds(seconds: Long): String {
            val millis: Long = seconds * 1000
            val tz = TimeZone.getTimeZone("UTC")
            val df = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)
            df.timeZone = tz
            return df.format(Date(millis))
        }

        fun convertMeters(meters: Long): String {
            val kilometers = meters / 1000.0
            return "%.2f".format(kilometers) + " km".replace(",", ".")
        }

        fun convert(value: Double, unit: String): String {
            return "%.2f".format(value).replace(",", ".") + " $unit"
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(fragment.requireActivity(),"The Language not supported!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun speak() {
        val text = fragment.textViewRDEPrompt.text.toString()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ID")
        } else {
            Toast.makeText(fragment.requireActivity(), "This SDK version does not support Text To Speech.", Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * UI class for the [HomeFragment].
 * Updates the little total duration TextView on the HomeFragment during an ongoing RDE-Track (with blinking red dot).
 */
class RDEHomeUpdater(private val inputChannel: ReceiveChannel<DoubleArray>, val fragment: HomeFragment) {
    private var uiJob: Job? = null
    fun start() {
        uiJob = GlobalScope.launch(Dispatchers.Main) {
            for (inputs in inputChannel) {
                try {
                    fragment.homeTotalRDETime.text = RDEUIUpdater.convertSeconds(
                        inputs[4].toLong() + inputs[5].toLong() +
                                inputs[6]
                                    .toLong()
                    )
                } catch (e: Exception) {
                    cancel()
                }
            }
        }
    }

    fun stop() {
        uiJob?.cancel()
    }
}
