package org.rdeapp.pcdftester.Sinks

import android.graphics.Color

/**
 * A class to generate prompts based on the trajectory driven so far.
 * It uses the TrajectoryAnalyser class to analyse the trajectory and determine the next prompt.
 */
class PromptGenerator (
    private var expectedDistance: Double,
) {
    // Used to analyse the trajectory to choose the next instructions for the driver
    private lateinit var trajectoryAnalyser: TrajectoryAnalyser

    // Variables to store information needed to determine the prompt
    private var speedChange: Double = 0.0
    private var drivingStyleText: String = ""
    private var desiredDrivingMode: DrivingMode = DrivingMode.URBAN
    private var sufficientDrivingMode: DrivingMode? = null

    // Variables to store the new prompt
    private var promptText: String = ""
    private var analysisText: String = ""
    private var promptColour: Int = Color.BLACK
    private var analysisColour: Int = Color.BLACK
    private var promptType: PromptType? = PromptType.NONE

    // Array to store the current state of the RDE test constraints
    private var constraints: Array<Double?> = arrayOf(null)

    /**
     * Analyse the trajectory driven so far by using functions from the TrajectoryAnalyser class. If
     * the distance travelled is less than 1/3 of the expected distance, do not analyse.
     * Otherwise, set the desired driving mode, the required speed change, and the prompt type
     * according to the constraints on the driving style.
     *
     * @param totalDistance The total distance travelled so far.
     */
    private fun analyseTrajectory(totalDistance: Double) {
        if ((totalDistance >= 1/4 * expectedDistance || sufficientDrivingMode != null) && trajectoryAnalyser.getTotalTime() > 15) {
            // set the desired driving mode accrued to the sufficient driving modes so far
            desiredDrivingMode = trajectoryAnalyser.setDesiredDrivingMode()

            // get the speed change needed to improve the driving style
            speedChange = trajectoryAnalyser.computeSpeedChange()

            // get the constraints on the current driving style
            constraints = trajectoryAnalyser.getConstraints()

            // set the prompt type according to the constraints
            setPromptType(constraints, totalDistance)
        }
    }

    /**
     * Set the prompt type according to the current driving mode and the test constraints.
     * @param constraints The constraints on the driving style.
     * @param totalDistance The total distance travelled so far.
     */
    private fun setPromptType(constraints: Array<Double?>, totalDistance: Double) {
        val highSpeed = constraints[0]
        val veryHighSpeed = constraints[1]
        val stoppingTime = constraints[2]
        val averageUrbanSpeed = constraints[3]

        when (trajectoryAnalyser.currentDrivingMode()) {
            // in motorway driving mode, check if the high speed or very high speed constraints
            // are violated. If not, set to a driving mode prompt type.
            DrivingMode.MOTORWAY -> {
                if (highSpeed != null && highSpeed != 0.0 && promptType != PromptType.HIGHSPEEDPERCENTAGE) {
                    promptType = PromptType.HIGHSPEEDPERCENTAGE
                } else if (veryHighSpeed != null) {
                    promptType = PromptType.VERYHIGHSPEEDPERCENTAGE
                } else {
                    setModePromptType(totalDistance)
                }
            }

            // in urban driving mode, check if the average urban speed or stopping time constraints
            // are violated. If not, set to a driving mode prompt type.
            DrivingMode.URBAN -> {
                if (stoppingTime != null && stoppingTime != -0.06  && promptType != PromptType.AVERAGEURBANSPEED) {
                    promptType = PromptType.STOPPINGPERCENTAGE
                } else if (averageUrbanSpeed != null && averageUrbanSpeed != 0.0) {
                    promptType = PromptType.AVERAGEURBANSPEED
                } else {
                    setModePromptType(totalDistance)
                }
            }

            // in rural driving mode, set to a driving mode prompt type.
            else -> {
                setModePromptType(totalDistance)
            }
        }
    }

    /**
     * Set the prompt type if no constraints are violated.
     *
     * If the test is in its early stages, update the driver on mode sufficiency. If no prompt needed,
     * set to no prompt type. If the test is in its later stages, set to a driving style prompt type.
     *
     * @param totalDistance The total distance travelled so far.
     */
    private fun setModePromptType(totalDistance: Double) {
        promptType = if (totalDistance < expectedDistance / 3) {
            // Less than 1/3 of the expected distance is travelled, check if any driving style has become sufficient.
            sufficientDrivingMode = trajectoryAnalyser.checkSufficient()
            if (sufficientDrivingMode != null) {
                PromptType.SUFFICIENCY // Some driving style has just become sufficient
            } else {
                PromptType.NONE // No prompt needed right now
            }
        } else {
            // More than 1/3 of the expected distance is travelled, check the driving style.
            PromptType.DRIVINGSTYLE
        }
    }

    /**
     * Generate the prompt according to the PromptType set from the analysis done on the trajectory.
     * Sets the text views for the RDE prompt and analysis depending on the PromptType.
     *
     * @param totalDistance The total distance travelled so far.
     * @param trajectoryAnalyser The TrajectoryAnalyser object used to analyse the trajectory.
     */
    fun determinePrompt(totalDistance: Double, trajectoryAnalyser: TrajectoryAnalyser) {
        this.trajectoryAnalyser = trajectoryAnalyser
        // Analyse the trajectory driven so far
        analyseTrajectory(totalDistance / 1000) // convert to kilometres

        // Set the prompt and analysis according to the prompt type
        when (promptType) {
            // Set prompt and analysis in case of no constraints violated
            PromptType.NONE -> {
                setNonePrompt()
            }
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
    }

    /**
     * Set the prompt text and analysis text for the case where no prompt is needed yet.
     * (Less than 1/3 of the expected distance is travelled)
     */
    private fun setNonePrompt() {
        promptText = "Analysis will be available after 1/3 of the test is completed."
        analysisText = ""
        promptColour = Color.BLACK
        analysisColour = Color.BLACK
    }

    /**
     * Set the analysis text for the constraint of driving at 100km/h or more for at least 5 minutes.
     * @param highSpeedDuration The duration of driving at > 100km/h.
     */
    private fun setHighSpeedPrompt(highSpeedDuration: Double) {
        // Round the duration to 2 decimal
        val highSpeedDurationRounded = String.format("%.1f", highSpeedDuration).toDouble()

        analysisText =
            "You need to drive at 100km/h or more for at least $highSpeedDurationRounded more minutes."
        analysisColour = Color.BLACK
    }

    /**
     * Set analysis text for the constraint of driving of 145km/h or more for only 3% of the
     * motorway driving.
     * @param veryHighSpeedPercentage The very high speed percentage.
     */
    private fun setVeryHighSpeedPrompt(veryHighSpeedPercentage: Double) {
        when (veryHighSpeedPercentage) {
            0.025 -> {
                analysisText =
                    "You have driven at 145km/h or more for 2.5% of the motorway driving distance."
                analysisColour = Color.BLACK
            }
            0.015 -> {
                analysisText =
                    "You have driven at 145km/h or more for 1.5% of the motorway driving distance."
                analysisColour = Color.BLACK
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
    private fun setAverageUrbanSpeedPrompt(averageUrbanSpeed: Double, changeSpeed: Double) {
        // Round values to 2 decimal places
        val averageUrbanSpeedRounded = String.format("%.2f", averageUrbanSpeed).toDouble()
        val changeSpeedRounded = String.format("%.2f", changeSpeed).toDouble()

        when {
            averageUrbanSpeedRounded > 35 && averageUrbanSpeedRounded < 40 -> {
                promptText = "Your average urban speed, ${averageUrbanSpeedRounded}km/h, is close to being invalid."
                analysisText = "You are ${changeSpeedRounded}km/h away from exceeding the upper limit."
                promptColour = Color.RED
            }
            averageUrbanSpeedRounded > 15 && averageUrbanSpeedRounded < 20 -> {
                promptText = "Your average urban speed, ${averageUrbanSpeedRounded}km/h, is close to being invalid."
                analysisText = "You are ${-changeSpeedRounded}km/h above the lower limit."
                promptColour = Color.GREEN
            }
            changeSpeedRounded < 0 -> {
                promptText = "Your average urban speed, ${averageUrbanSpeedRounded}km/h, is too high."
                analysisText = "You are ${-changeSpeedRounded}km/h more than the upper limit."
                promptColour = Color.RED
            }
            changeSpeedRounded > 0 -> {
                promptText = "Your average urban speed, ${averageUrbanSpeedRounded}km/h, is too low."
                analysisText = "You are ${changeSpeedRounded}km/h less than the lower limit."
                promptColour = Color.GREEN
            }
        }
    }

    /**
     * Set prompt text for the stopping percentage.
     * @param stoppingPercentage The difference in stopping percentage from the upper or lower bounds
     */
    private fun setStoppingPercentagePrompt(stoppingPercentage: Double) {
        // Round values to 2 decimal places
        val stoppingPercentageRounded = String.format("%.2f", stoppingPercentage).toDouble()

        if (stoppingPercentageRounded > 0) {
            promptText = "You are stopping too little. Try to stop more."
            analysisText = "You need to stop for at least ${stoppingPercentageRounded * 100}% more of the urban time."
            promptColour = Color.RED
        } else{
            promptText = "You are close to exceeding the stopping percentage. Try to stop less."
            analysisText = "You are stopping ${-(stoppingPercentageRounded) * 100}% more than the upper bound."
            promptColour = Color.GREEN
        }
    }

    /**
     * Set the prompt for a newly sufficient driving style.
     * @param sufficientDrivingMode The driving mode for which the driving style is sufficient.
     */
    private fun setSufficientPrompt(sufficientDrivingMode: DrivingMode) {
        when (sufficientDrivingMode) {
            DrivingMode.URBAN -> {
                promptText = "Your urban driving is sufficient."
                promptColour = Color.BLACK
            }
            DrivingMode.RURAL -> {
                promptText = "Your rural driving is sufficient."
                promptColour = Color.BLACK
            }
            DrivingMode.MOTORWAY -> {
                promptText = "Your motorway driving is sufficient."
                promptColour = Color.BLACK
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
            promptText = "Aim for a higher driving speed, if it is safe to do so, $drivingStyleText"
            promptColour = Color.GREEN
        } else if (speedChange < 0) {
            promptText = "Aim for a lower driving speed, if it is safe to do so, $drivingStyleText"
            promptColour = Color.RED
        } else {
            promptText = "Your driving style is good"
            promptColour = Color.BLACK
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
     * Set the analysis textview for the driving style prompt.
     * @param duration The duration for which the driver should drive at the desired driving mode.
     */
    private fun setDrivingStyleAnalysis(duration: Double) {
        // Round value to 2 decimal places
        val durationRounded = String.format("%.2f", duration).toDouble()

        analysisText = when (desiredDrivingMode) {
            DrivingMode.URBAN -> {
                "Drive at an average speed of 30 km/h for at most $durationRounded minutes."
            }
            DrivingMode.RURAL -> {
                "Drive at an average speed of 75 km/h for at most $durationRounded minutes"
            }
            DrivingMode.MOTORWAY -> {
                "Drive at an average speed of 115 km/h for at most $durationRounded minutes"
            }
        }
    }

    /**
     * @return the current prompt type
     */
    fun getPromptType(): PromptType? {
        return promptType
    }


    /**
     * @return the current prompt text
     */
    fun getPromptText(): String {
        return promptText
    }

    /**
     * @return the new prompt colour
     */
    fun getPromptColour(): Int {
        return promptColour
    }

    /**
     * @return the new analysis text
     */
    fun getAnalysisText(): String {
        return analysisText
    }

    /**
     * @return the new analysis colour
     */
    fun getAnalysisColour(): Int {
        return analysisColour
    }
    
}