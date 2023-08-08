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
) : TextToSpeech.OnInitListener {

    // Text to speech object
    private var tts: TextToSpeech? = TextToSpeech(fragment.requireActivity(), this)

    // The distance that should be travelled in the RDE test
    private var expectedDistance = fragment.distance

    // Used to analyse the trajectory to choose the next instructions for the driver
    private var trajectoryAnalyser = fragment.trajectoryAnalyser

    // Variables to store information needed to determine the prompt
    private var speedChange: Double = 0.0
    private var drivingStyleText: String = ""
    private var desiredDrivingMode: DrivingMode = DrivingMode.URBAN
    private var sufficientDrivingMode: DrivingMode? = null

    // Variables to store the current prompt
    private var currentText: String = ""
    private var currentPromptType: PromptType? = null

    private var promptType: PromptType? = null
    private var constraints: Array<Double?> = arrayOf(null)

    /**
     * Update the prompt for improving the driving style according to the received RTLola results.
     * @param totalDistance The total distance travelled so far.
     */
    suspend fun handlePrompt(totalDistance: Double) {
        // Check if the RDE test is still valid
        handleInvalidRDE()

        // Determine the next instruction according to analysis of the trajectory driven.
        analyseTrajectory(totalDistance)

        // Update the prompt according to the determined prompt type, and activate text to speech.
        generatePrompt()
        currentText = fragment.textViewRDEPrompt.text.toString()
    }

    /**
     * Handles invalid RDE tests by updating the TextView for the RDE prompt and creates
     * an error message It also stops the current RDE test and navigates to the RDE setting.
     */
    private suspend fun handleInvalidRDE() {
        if (trajectoryAnalyser.checkInvalid()) {
            fragment.textViewRDEPrompt.text = "This RDE test is invalid, and will be stopped now."
            fragment.textViewRDEPrompt.setTextColor(Color.RED)

            // Only speak if the text has changed
            if (currentText != fragment.textViewRDEPrompt.text.toString()) {
                speak()
            }

            Toast.makeText(fragment.requireActivity(),"Exiting...", Toast.LENGTH_LONG).show()

            // Stop tracking the RDE test
            (fragment.requireActivity() as MainActivity).stopTracking()

            // Move to the RDE settings fragment
            fragment.requireActivity().supportFragmentManager.beginTransaction().replace(
                R.id.frame_layout,
                (fragment.requireActivity() as MainActivity).rdeSettingsFragment
            ).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
        }
    }

    /**
     * Analyse the trajectory driven so far by using functions from the TrajectoryAnalyser class.
     *
     * Check the constraints on the current driving style, and set the prompt type according to the
     * analysis.
     *
     * For the 1st part of the test, only check if the driving style is sufficient.
     * For the 2nd part of the test, also set the desired driving mode and speed change.
     *
     * @param totalDistance The total distance travelled so far.
     */
    private fun analyseTrajectory(totalDistance: Double) {
        if (totalDistance >= 1/3 * expectedDistance) {
            // set the desired driving mode accrued to the sufficient driving modes so far
            desiredDrivingMode = trajectoryAnalyser.setDesiredDrivingMode()
            // get the speed change needed to improve the driving style
            speedChange = trajectoryAnalyser.computeSpeedChange()
        }

        // set the prompt type according to the driving mode and constraints.
        constraints = trajectoryAnalyser.getConstraints()
        setPromptType(constraints, totalDistance)
    }

    /**
     * Set the prompt type according to the constraints.
     * @param constraints The constraints on the driving style.
     * @param totalDistance The total distance travelled so far.
     */
    private fun setPromptType(constraints: Array<Double?>, totalDistance: Double) {
        val highSpeed = constraints[0]
        val veryHighSpeed = constraints[1]
        val stoppingTime = constraints[2]
        val averageUrbanSpeed = constraints[3]

        when (trajectoryAnalyser.currentDrivingMode()) {
            DrivingMode.MOTORWAY -> {
                if (highSpeed != null && highSpeed != 0.0 && promptType != PromptType.VERYHIGHSPEEDPERCENTAGE) {
                    promptType = PromptType.HIGHSPEEDPERCENTAGE
                } else if (veryHighSpeed != null) {
                    promptType = PromptType.VERYHIGHSPEEDPERCENTAGE
                }
            }
            DrivingMode.URBAN -> {
                if (averageUrbanSpeed != null && averageUrbanSpeed != 0.0 && promptType != PromptType.STOPPINGPERCENTAGE) {
                    promptType = PromptType.AVERAGEURBANSPEED
                } else if (stoppingTime != null) {
                    promptType = PromptType.STOPPINGPERCENTAGE
                }
            }
            else -> {
                if (totalDistance < expectedDistance / 3) {
                    // Less than 1/3 of the expected distance is travelled, check if any driving style has become sufficient.
                    val sufficientDrivingMode = trajectoryAnalyser.checkSufficient()
                    if (sufficientDrivingMode != null) {
                        promptType = PromptType.SUFFICIENCY
                    }
                } else {
                    // More than 1/3 of the expected distance is travelled, check the driving style.
                    promptType = PromptType.DRIVINGSTYLE
                }
            }
        }

        // Store the most recent prompt type
        currentPromptType = promptType
    }

    /**
     * Generate the prompt according to the PromptType set from the analysis done on the trajectory.
     * Sets the TextViews for the RDE prompt and analysis depending on the PromptType
     */
    private fun generatePrompt() {
        when (promptType) {
            PromptType.SUFFICIENCY -> {
                setSufficientPrompt(sufficientDrivingMode!!)
            }
            PromptType.DRIVINGSTYLE -> {
                setDrivingStyleText()
                setDrivingStylePrompt(drivingStyleText)
                setDrivingStyleAnalysis(trajectoryAnalyser.computeDuration())
            }
            // Set prompt and analysis in case of constraint in Urban driving mode
            PromptType.AVERAGEURBANSPEED -> {
                val averageUrbanSpeed = trajectoryAnalyser.getAverageUrbanSpeed()
                setAverageUrbanSpeedPrompt(averageUrbanSpeed, constraints[3]!!)
            }
            PromptType.STOPPINGPERCENTAGE -> {
                setStoppingPercentagePrompt(constraints[2]!!)
            }
            // Set analysis in case of constraint in Motorway driving mode
            PromptType.HIGHSPEEDPERCENTAGE -> {
                setDrivingStyleText()
                setDrivingStylePrompt(drivingStyleText)
                setHighSpeedPrompt(constraints[0]!!)
            }
            PromptType.VERYHIGHSPEEDPERCENTAGE -> {
                setDrivingStyleText()
                setDrivingStylePrompt(drivingStyleText)
                setVeryHighSpeedPrompt(constraints[1]!!)
            }
        }

        if (currentPromptType == PromptType.DRIVINGSTYLE) {
            // Only speak if the text has changed
            if (currentText != fragment.textViewRDEPrompt.text.toString()) {
                speak()
            }
        }
        // For all other prompt types, only speak if the prompt type has changed
        else if (currentPromptType != promptType) {
            speak()
        }
        currentText = fragment.textViewRDEPrompt.text.toString()
    }

    /**
     * Set the analysis text for the constraint of driving at 100km/h or more for at least 5 minutes.
     * @param highSpeedDuration The duration of driving at > 100km/h.
     */
    private fun setHighSpeedPrompt(highSpeedDuration: Double){
        fragment.textViewAnalysis.text =
            "You need to drive at 100km/h or more for at least $highSpeedDuration more minutes."
        fragment.textViewAnalysis.setTextColor(Color.BLACK)
    }

    /**
     * Set analysis text for the constraint of driving of 145km/h or more for only 3% of the
     * motorway driving.
     * @param veryHighSpeedPercentage The very high speed percentage.
     */
    private fun setVeryHighSpeedPrompt(veryHighSpeedPercentage: Double){
        when (veryHighSpeedPercentage) {
            0.025 -> {
                fragment.textViewAnalysis.text =
                    "You have driven at 145km/h or more for 2.5% of the motorway driving distance."
                fragment.textViewAnalysis.setTextColor(Color.BLACK)
            }
            0.015 -> {
                fragment.textViewAnalysis.text =
                    "You have driven at 145km/h or more for 1.5% of the motorway driving distance."
                fragment.textViewAnalysis.setTextColor(Color.BLACK)
            }
        }
    }

    /**
     * Set the prompt and analysis text for the constraint to make the average urban speed
     * between 15km/h to 40km/h.
     * @param averageUrbanSpeed The average urban speed.
     * @param changeSpeed The change in speed needed to improve the driving style so
     *                    how far from nearest bound.
     */
    private fun setAverageUrbanSpeedPrompt(averageUrbanSpeed: Double, changeSpeed: Double){
        when {
            averageUrbanSpeed > 35 && averageUrbanSpeed < 40 -> {
            fragment.textViewRDEPrompt.text = "Your average urban speed (${averageUrbanSpeed}km/h) is close to being invalid."
            fragment.textViewAnalysis.text = "You are ${changeSpeed}km/h away from exceeding the upper bound."
            fragment.textViewRDEPrompt.setTextColor(Color.RED)
            }
            averageUrbanSpeed > 15 && averageUrbanSpeed < 20 -> {
            fragment.textViewRDEPrompt.text = "Your average urban speed (${averageUrbanSpeed}km/h) is close to being invalid."
            fragment.textViewAnalysis.text = "You are ${changeSpeed}km/h above the lower bound."
            fragment.textViewRDEPrompt.setTextColor(Color.GREEN)
            }
            changeSpeed < 0 -> {
            fragment.textViewRDEPrompt.text = "Your average urban speed (${averageUrbanSpeed}km/h) is too high."
            fragment.textViewAnalysis.text = "You are ${changeSpeed}km/h more than the upper bound."
            fragment.textViewRDEPrompt.setTextColor(Color.RED)
            }
            changeSpeed > 0 -> {
            fragment.textViewRDEPrompt.text = "Your average urban speed (${averageUrbanSpeed}km/h) is too low."
            fragment.textViewAnalysis.text = "You are ${changeSpeed}km/h less than the lower bound."
            fragment.textViewRDEPrompt.setTextColor(Color.GREEN)
            }
        }
    }

    /**
     * Set prompt text for the stopping percentage.
     * @param stoppingPercentage The difference in stopping percentage from the upper or lower bounds
     */
    private fun setStoppingPercentagePrompt(stoppingPercentage: Double) {
        if (stoppingPercentage > 0) {
            fragment.textViewRDEPrompt.text = "You are stopping too little. Try to stop more."
            fragment.textViewAnalysis.text = "You need to stop for at least ${stoppingPercentage * 100}% more of the urban time."
            fragment.textViewRDEPrompt.setTextColor(Color.RED)
        } else{
            fragment.textViewRDEPrompt.text = "You are close to exceeding the stopping percentage. Try to stop less."
            fragment.textViewAnalysis.text = "You are stopping ${-(stoppingPercentage) * 100}% less than the upper bound."
            fragment.textViewRDEPrompt.setTextColor(Color.GREEN)
        }
    }

    /**
     * Set the prompt for a newly sufficient driving style.
     * @param sufficientDrivingMode The driving mode for which the driving style is sufficient.
     */
    private fun setSufficientPrompt(sufficientDrivingMode: DrivingMode) {
        when (sufficientDrivingMode) {
            DrivingMode.URBAN -> {
                fragment.textViewRDEPrompt.text = "Your urban driving is sufficient."
                fragment.textViewRDEPrompt.setTextColor(Color.BLACK)
            }
            DrivingMode.RURAL -> {
                fragment.textViewRDEPrompt.text = "Your rural driving is sufficient."
                fragment.textViewRDEPrompt.setTextColor(Color.BLACK)
            }
            DrivingMode.MOTORWAY -> {
                fragment.textViewRDEPrompt.text = "Your motorway driving is sufficient."
                fragment.textViewRDEPrompt.setTextColor(Color.BLACK)
            }
        }
    }

    /**
     * Set the prompt text according to the driving mode and speed change.
     * @param drivingStyleText The text for the desired driving mode.
     */
    private fun setDrivingStylePrompt(drivingStyleText: String) {
        // Calculate the speed change needed to improve the driving style
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
    }

    /**
     * Set the text for the prompt TextView according to the driving mode.
     */
    private fun setDrivingStyleText() {
        drivingStyleText = when (desiredDrivingMode) {
            DrivingMode.URBAN -> "for more urban driving"
            DrivingMode.RURAL -> "for more rural driving"
            DrivingMode.MOTORWAY -> "for more motorway driving"
        }
    }

    /**
     * Set analysis textview for the driving style prompt.
     * @param duration The duration for which the driver should drive at the desired driving mode.
     */
    private fun setDrivingStyleAnalysis(duration: Double) {
        when (desiredDrivingMode) {
            DrivingMode.URBAN -> {
                fragment.textViewAnalysis.text = "Drive at an average speed of 30 km/h for at most $duration minutes."
            }
            DrivingMode.RURAL -> {
                fragment.textViewAnalysis.text = "Drive at an average speed of 75 km/h for at most $duration minutes"
            }
            DrivingMode.MOTORWAY -> {
                fragment.textViewAnalysis.text = "Drive at an average speed of 115 km/h for at most $duration minutes"
            }
        }
    }

    /**
     * Initialise the Text To Speech engine.
     * @param status The status of the Text To Speech engine.
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
     * If the SDK version is below LOLLIPOP, then a toast is shown that Text To Speech is not supported.
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