package org.rdeapp.pcdftester.Sinks

import android.graphics.Color
import android.os.Build
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.fragment.app.FragmentTransaction
import de.unisaarland.loladrives.Fragments.RDE.RDEFragment
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.R
import kotlinx.android.synthetic.main.fragment_r_d_e.textViewAnalysis
import kotlinx.android.synthetic.main.fragment_r_d_e.textViewRDEPrompt
import java.util.Locale

/**
 * Class for handling the prompt for improving the driving style.
 * @property fragment The RDEFragment in which the prompt is displayed.
 */
class PromptHandler (
    private val fragment: RDEFragment
    ) : TextToSpeech.OnInitListener{

    private var tts: TextToSpeech? = TextToSpeech(fragment.requireActivity(), this)
    private var expectedDistance = fragment.distance

    private var motorwayComplete: Boolean = false
    private var ruralComplete: Boolean = false
    private var urbanComplete: Boolean = false
    private var motorwaySufficient: Boolean = false
    private var ruralSufficient: Boolean = false
    private var urbanSufficient: Boolean = false
    private var motorwayInsufficient: Boolean = false
    private var ruralInsufficient: Boolean = false
    private var urbanInsufficient: Boolean = false
    private var currentText: String = ""

    private var urbanProportion: Double = 0.0
    private var ruralProportion: Double = 0.0
    private var motorwayProportion: Double = 0.0


    /**
     * Update the prompt for improving the driving style according to the received RTLola results.
     */
    fun handlePrompt(
        totalDistance: Double,
        totalTime: Double,
        ) {
        // Check if the RDE test is still valid
        checkInvalidRDE(totalTime)

        val currentSpeed = fragment.rdeValidator.currentSpeed
        var speedChange: Double = 0.0
        var drivingStyleText: String = ""

        // Cases where the RDE test is still valid, but the driver should improve
        if (totalDistance > expectedDistance/2) {
            when {
                urbanSufficient && ruralSufficient && motorwayInsufficient -> {
                    drivingStyleText = "for more motorway driving"
                    speedChange = computeSpeedChange(currentSpeed, 90, 145)
                    computeDuration(DrivingMode.MOTORWAY)
                }
                urbanSufficient && ruralInsufficient && motorwaySufficient -> {
                    drivingStyleText = "for more rural driving"
                    speedChange = computeSpeedChange(currentSpeed, 60, 90)
                    computeDuration(DrivingMode.RURAL)
                }
                urbanInsufficient && ruralSufficient && motorwaySufficient -> {
                    drivingStyleText = "for more urban driving"
                    speedChange = computeSpeedChange(currentSpeed, 0, 60)
                    computeDuration(DrivingMode.URBAN)
                }
                urbanSufficient && ruralInsufficient && motorwayInsufficient -> {
                    drivingStyleText = "for more rural and motorway driving"
                    if (ruralProportion < motorwayProportion) {
                        speedChange = computeSpeedChange(currentSpeed, 60, 90)
                        computeDuration(DrivingMode.RURAL)
                    } else {
                        speedChange = computeSpeedChange(currentSpeed, 90, 145)
                        computeDuration(DrivingMode.MOTORWAY)
                    }
                }
                urbanInsufficient && ruralSufficient && motorwayInsufficient -> {
                    drivingStyleText = "for less rural driving"
                    if (urbanProportion < motorwayProportion) {
                        speedChange = computeSpeedChange(currentSpeed, 0, 60)
                        computeDuration(DrivingMode.URBAN)
                    } else {
                        speedChange = computeSpeedChange(currentSpeed, 90, 145)
                        computeDuration(DrivingMode.MOTORWAY)
                    }
                }
                urbanInsufficient && ruralInsufficient && motorwaySufficient -> {
                    drivingStyleText = "for more urban and rural driving"
                    if (ruralProportion < urbanProportion) {
                        speedChange = computeSpeedChange(currentSpeed, 60, 90)
                        computeDuration(DrivingMode.RURAL)
                    } else {
                        speedChange = computeSpeedChange(currentSpeed, 0, 60)
                        computeDuration(DrivingMode.URBAN)
                    }
                }
            }

            if (speedChange > 0) {
                fragment.textViewRDEPrompt.text = "Aim for a higher driving speed, if it is safe to do so, $drivingStyleText"
                fragment.textViewRDEPrompt.setTextColor(Color.GREEN)
            } else if (speedChange < 0) {
                fragment.textViewRDEPrompt.text = "Aim for a lower driving speed, if it is safe to do so, $drivingStyleText"
                fragment.textViewRDEPrompt.setTextColor(Color.RED)
            } else {
                fragment.textViewRDEPrompt.text = "Your driving style is good"
                fragment.textViewRDEPrompt.setTextColor(Color.BLACK)
            }
            // Only speak if the text has changed
            if (currentText != fragment.textViewRDEPrompt.text.toString()) {
                speak()
            }
        } else {
            // Only 1 driving style can be sufficient in the first half of the test.
            fragment.textViewRDEPrompt.text = checkSufficient()
            fragment.textViewRDEPrompt.setTextColor(Color.BLACK)
        }
        currentText = fragment.textViewRDEPrompt.text.toString()
    }

    /**
     * Check the progress of Urban, Rural and Motorway driving and update corresponding booleans.
     */
    fun checkProgress(
        urbanDistance: Double,
        ruralDistance: Double,
        motorwayDistance: Double
    ){
        urbanProportion = urbanDistance / expectedDistance
        ruralProportion = ruralDistance / expectedDistance
        motorwayProportion = motorwayDistance / expectedDistance

        motorwayComplete = motorwayProportion > 0.43
        ruralComplete = ruralProportion > 0.43
        urbanComplete = urbanProportion > 0.44

        motorwaySufficient = motorwayProportion > 0.23
        ruralSufficient = ruralProportion > 0.23
        urbanSufficient = urbanProportion > 0.29

        motorwayInsufficient = motorwayProportion < 0.18
        ruralInsufficient = ruralProportion < 0.18
        urbanInsufficient = urbanProportion < 0.23
    }

    /**
     * Check if the RDE test is invalid.
     * If so, announce it to the driver, and move to the RDE settings fragment.
     */
    private fun checkInvalidRDE(totalTime: Double) {
        if (urbanComplete || ruralComplete || motorwayComplete || totalTime > 120) {
            fragment.textViewRDEPrompt.text = "This RDE test is invalid, and will be stopped now."
            fragment.textViewRDEPrompt.setTextColor(Color.RED)

            // Only speak if the text has changed
            if (currentText != fragment.textViewRDEPrompt.text.toString()) {
                speak()
            }

            Toast.makeText(fragment.requireActivity(),"Exiting...", Toast.LENGTH_LONG).show()

            fragment.requireActivity().supportFragmentManager.beginTransaction().replace(
                R.id.frame_layout,
                (fragment.requireActivity() as MainActivity).rdeSettingsFragment
            ).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
        }
    }

    /**
     * Check whether a driving style is sufficient.
     */
    private fun checkSufficient(): String {
        return if (motorwaySufficient) {
            "Motorway driving is sufficient"
        } else if (ruralSufficient) {
            "Rural driving is sufficient"
        } else if (urbanSufficient) {
            "Urban driving is sufficient"
        } else {
            "Your driving style is good"
        }
    }

    /**
     * Calculate the speed change required to improve the driving style.
     */
    private fun computeSpeedChange (
        currentSpeed: Double,
        lowerThreshold: Int,
        upperThreshold: Int,
    ): Double {
        val speedChange: Double
        if (currentSpeed < lowerThreshold) {
            speedChange = lowerThreshold - currentSpeed
        } else if (currentSpeed > upperThreshold) {
            speedChange = currentSpeed - upperThreshold
        } else {
            speedChange = 0.0
        }
        return speedChange
    }

    /**
     * Calculate how long the user to should drive in the certain driving mode to improve their driving style.
     */
    private fun computeDuration(mode:DrivingMode) {
        when(mode) {
            DrivingMode.URBAN -> {
                // Calculate the distance left to drive in urban mode with an average speed of 30 km/h
                val urbanDistanceLeft = (0.29 - urbanProportion) * expectedDistance
                fragment.textViewAnalysis.text = "Drive at an average speed of 30 km/h for ${urbanDistanceLeft * 2} minutes"
            }
            DrivingMode.RURAL -> {
                // Calculate the distance left to drive in rural mode with an average speed of 75 km/h
                val ruralDistanceLeft = (0.23 - ruralProportion) * expectedDistance
                fragment.textViewAnalysis.text = "Drive at an average speed of 75 km/h for ${ruralDistanceLeft * 0.8} minutes"
            }
            DrivingMode.MOTORWAY -> {
                // Calculate the distance left to drive in motorway mode with an average speed of 115 km/h
                val motorwayDistanceLeft = (0.23 - motorwayProportion) * expectedDistance
                fragment.textViewAnalysis.text = "Drive at an average speed of 30 km/h for ${motorwayDistanceLeft * 60 / 115} minutes"
            }
        }
    }

    /**
     * Initialise the Text To Speech engine.
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(fragment.requireActivity(),"The Language not supported!", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Speak the text in the RDE prompt TextView.
     */
    private fun speak() {
        val text = fragment.textViewRDEPrompt.text.toString()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ID")
        } else {
            Toast.makeText(fragment.requireActivity(), "This SDK version does not support Text To Speech.", Toast.LENGTH_LONG).show()
        }
    }

}