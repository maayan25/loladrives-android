package org.rdeapp.pcdftester.Sinks

import android.view.View
import androidx.core.content.ContextCompat
import de.unisaarland.loladrives.Fragments.HomeFragment
import de.unisaarland.loladrives.Fragments.RDE.RDEFragment
import de.unisaarland.loladrives.R
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_r_d_e.*
import kotlinx.android.synthetic.main.fragment_r_d_e_modify.viewFlipper
import kotlinx.android.synthetic.main.motorway.dynamicBarMotorway
import kotlinx.android.synthetic.main.motorway.motorwayAverageSpeedValue
import kotlinx.android.synthetic.main.motorway.motorwayDistanceValue
import kotlinx.android.synthetic.main.motorway.motorwayTimeValue
import kotlinx.android.synthetic.main.motorway.progressProportionMotorway
import kotlinx.android.synthetic.main.rural.dynamicBarRural
import kotlinx.android.synthetic.main.rural.progressProportionRural
import kotlinx.android.synthetic.main.rural.ruralAverageSpeedValue
import kotlinx.android.synthetic.main.rural.ruralDistanceValue
import kotlinx.android.synthetic.main.rural.ruralTimeValue
import kotlinx.android.synthetic.main.urban.dynamicBarUrban
import kotlinx.android.synthetic.main.urban.progressProportionUrban
import kotlinx.android.synthetic.main.urban.urbanAverageSpeedValue
import kotlinx.android.synthetic.main.urban.urbanDistanceValue
import kotlinx.android.synthetic.main.urban.urbanTimeValue
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * UI class for the [RDEFragment].
 * @property inputChannel Channel over which we receive the RTLola results from the [RDEValidator] as DoubleArrays.
 * @property fragment The current [RDEFragment] we are updating.
 */
class RDEUIUpdater(
    private val inputChannel: ReceiveChannel<DoubleArray>,
    val fragment: RDEFragment
) {
    // The current expected distance (may change during track).
    private var expectedDistance = fragment.distance
    private var started = false
    // Constants concerning the NOx values, we take 200[mg/km] to be the largest amount we may display in the NOx-Bar.
    private val noxMaximum = 0.2 // [g/km]
    private val noxThr1 = 0.12 // [g/km]
    private val noxThr2 = 0.168 // [g/km]

    private var metricSystem = true
    private var swipedToRightTime : Long = 0L

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
                fragment.viewFlipper.visibility = View.VISIBLE
                fragment.progressProportionMotorway.visibility = View.VISIBLE
                fragment.progressProportionMotorway.min = 23f
                fragment.progressProportionMotorway.max = 43f
                fragment.progressProportionRural.visibility = View.VISIBLE
                fragment.progressProportionRural.min = 23f
                fragment.progressProportionRural.max = 43f
                fragment.progressProportionUrban.visibility = View.VISIBLE
                fragment.progressProportionUrban.min = 29f
                fragment.progressProportionUrban.max = 44f
                started = true
            }

            // add listener for viewFlipper to swipe
            fragment.viewFlipper.isClickable = true



            try {
                // Update metric toggle button
                metricSystem = fragment.metricToggleButton.isChecked
                // Update all the simple TextViews.
                fragment.textViewTotalDistance.text = convertMeters(outputs[0].toLong())
                fragment.urbanDistanceValue.text = convertMeters(outputs[1].toLong())
                fragment.ruralDistanceValue.text = convertMeters(outputs[2].toLong())
                fragment.motorwayDistanceValue.text = convertMeters(outputs[3].toLong())

                fragment.urbanTimeValue.text = convertSeconds(outputs[4].toLong())
                fragment.ruralTimeValue.text = convertSeconds(outputs[5].toLong())
                fragment.motorwayTimeValue.text = convertSeconds(outputs[6].toLong())

                fragment.urbanAverageSpeedValue.text = convertSpeed(outputs[7])
                fragment.ruralAverageSpeedValue.text = convertSpeed(outputs[8])
                fragment.motorwayAverageSpeedValue.text = convertSpeed(outputs[9])

                fragment.textViewTotalTime.text = convertSeconds(outputs[4].toLong() + outputs[5].toLong() + outputs[6].toLong())
                val flipperView = fragment.viewFlipper
                // Change view based on current speed if change is detected
                if (checkSwipeRight()) {
                    if(fragment.rdeValidator.currentSpeed < 60 && flipperView.displayedChild != 0) {
                        flipperView.displayedChild = 0
                    } else if (fragment.rdeValidator.currentSpeed >= 60 && fragment.rdeValidator.currentSpeed < 90 && flipperView.displayedChild != 1) {
                        flipperView.displayedChild = 1
                    } else if (fragment.rdeValidator.currentSpeed >= 90 && flipperView.displayedChild != 2) {
                        flipperView.displayedChild = 2
                    }
                }

                flipperView.setOnClickListener {
                    if (flipperView.displayedChild == 0) {
                        flipperView.displayedChild = 1
                        swipedToRightTime = System.currentTimeMillis()
                    } else if (flipperView.displayedChild == 1) {
                        flipperView.displayedChild = 2
                        swipedToRightTime = System.currentTimeMillis()
                    } else {
                        flipperView.displayedChild = 0
                        swipedToRightTime = System.currentTimeMillis()
                    }
                }

                // Update the distance ProgressBars (total[0], urban[1], rural[2], motorway[3])
                handleDistance(outputs[0], outputs[1], outputs[2], outputs[3])

                val totalTime = (outputs[4] + outputs[5] + outputs[6]) / 60  // Compute total test time so far in minutes

                // Check progress (urban[1], rural[2], motorway[3])
                fragment.trajectoryAnalyser.updateProgress(
                    outputs[1],
                    outputs[2],
                    outputs[3],
                    outputs[4].toLong(),
                    totalTime,
                    fragment.rdeValidator.currentSpeed,
                    outputs[7],
                    outputs[8],
                    outputs[9],
                    outputs[17],
                    outputs[18]
                )

                // Update the prompt ProgressBars (total[0])
                fragment.promptHandler.handlePrompt(outputs[0], outputs[18] == 1.0, outputs[17] == 1.0, metricSystem)

                fragment.trajectoryAnalyser.updateDynamicThresholds(
                    outputs[7], // Average Speed
                    outputs[8],
                    outputs[9],
                    outputs[13], // Low values
                    outputs[14],
                    outputs[15],
                    outputs[10], // High values
                    outputs[11],
                    outputs[12]
                )


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

        // Calculate Horizontal Marker Positions
        val uRpaThreshold = getMinimumDynamics(u_avg_v)
        val rRpaThreshold = getMinimumDynamics(r_avg_v)
        val mRpaThreshold = getMinimumDynamics(m_avg_v)


        // Calculate maximum Dynamics using the average speed boundary
        val uPctThreshold = getMaximumDynamics(u_avg_v)
        val rPctThreshold = getMaximumDynamics(r_avg_v)
        val mPctThreshold = getMaximumDynamics(m_avg_v)


        val dynamicBarUrban = fragment.dynamicBarUrban
        val dynamicBarRural = fragment.dynamicBarRural
        val dynamicBarMotorway = fragment.dynamicBarMotorway

        dynamicBarMotorway.low = m_rpa.toFloat()
        dynamicBarMotorway.high = m_va_pct.toFloat()
        dynamicBarMotorway.max = mPctThreshold.toFloat()
        dynamicBarMotorway.min = mRpaThreshold.toFloat()
        dynamicBarMotorway.invalidate()

        dynamicBarUrban.low = u_rpa.toFloat()
        dynamicBarUrban.high = u_va_pct.toFloat()
        dynamicBarUrban.max = uPctThreshold.toFloat()
        dynamicBarUrban.min = uRpaThreshold.toFloat()
        dynamicBarUrban.invalidate()

        dynamicBarRural.low = r_rpa.toFloat()
        dynamicBarRural.high = r_va_pct.toFloat()
        dynamicBarRural.max = rPctThreshold.toFloat()
        dynamicBarRural.min = rRpaThreshold.toFloat()
        dynamicBarRural.invalidate()

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
        val urbanProgress = fragment.progressProportionUrban
        val ruralProgress = fragment.progressProportionRural
        val motorwayProgress = fragment.progressProportionMotorway

        val urbanPercent = urbanDistance.toFloat() / 1000 / expectedDistance.toFloat() * 100
        val ruralPercent = ruralDistance.toFloat() / 1000 / expectedDistance.toFloat() * 100
        val motorwayPercent = motorwayDistance.toFloat() / 1000 / expectedDistance.toFloat() * 100

        urbanProgress.percent = urbanPercent
        ruralProgress.percent = ruralPercent
        motorwayProgress.percent = motorwayPercent
        urbanProgress.invalidate()
        ruralProgress.invalidate()
        motorwayProgress.invalidate()

        fragment.distance = expectedDistance
    }

    /**
     * Get maximum Dynamics using the average speed.
     */
    private fun getMaximumDynamics(averageSpeed: Double): Double {
        return if (averageSpeed < 74.6) {
            if (averageSpeed == 0.0) {
                0.0
            } else {
                0.136 * averageSpeed + 14.44
            }
        } else {
            0.0742 * averageSpeed + 18.966
        }
    }

    /**
     * Get minimum Dynamics using the average speed.
     */
    private fun getMinimumDynamics(averageSpeed: Double): Double {
        return if (averageSpeed < 94.05) {
            if (averageSpeed == 0.0) {
                0.0
            } else {
                -0.0016 * averageSpeed + 0.1755
            }
        } else {
            0.025
        }
    }

    /**
     * Check if it has been swiped to the right for more than 5 seconds.
     */
    fun checkSwipeRight(): Boolean {
        return  if (System.currentTimeMillis() - swipedToRightTime < 5000) {
            false
        } else if (System.currentTimeMillis() - swipedToRightTime > 5000) {
            swipedToRightTime = 0L
            true
        } else {
            true
        }
    }

    fun convertMeters(meters: Long): String {
        if (metricSystem){
            return "%.2f".format(meters / 1000.0).replace(",", ".") + " km"
        } else {
            return "%.2f".format(meters / 1609.344).replace(",", ".") + " mi"
        }
    }

    fun convertSpeed(speed: Double): String {
        if (metricSystem){
            return "%.2f".format(speed).replace(",", ".") + " km/h"
        } else {
            return "%.2f".format(speed * 0.621371).replace(",", ".") + " mph"
        }
    }

    companion object {
        fun convertSeconds(seconds: Long): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, secs)
        }

        fun convert(value: Double, unit: String): String {
            return "%.2f".format(value).replace(",", ".") + " $unit"
        }

        fun convertMeters(meters: Long): String {
            return "%.2f".format(meters / 1000.0).replace(",", ".") + " km"
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
