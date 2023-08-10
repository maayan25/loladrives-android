package org.rdeapp.pcdftester.Sinks

import android.graphics.Color

class PromptGenerator (
    private var expectedDistance: Double,
) {
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
    private var promptType: PromptType? = null

    private var constraints: Array<Double?> = arrayOf(null)

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
                } else {
                    setModePromptType(totalDistance)
                }
            }
            DrivingMode.URBAN -> {
                if (averageUrbanSpeed != null && averageUrbanSpeed != 0.0 && promptType != PromptType.STOPPINGPERCENTAGE) {
                    promptType = PromptType.AVERAGEURBANSPEED
                } else if (stoppingTime != null && stoppingTime != -0.06) { // TODO change this to a better value
                    promptType = PromptType.STOPPINGPERCENTAGE
                } else {
                    setModePromptType(totalDistance)
                }
            }
            else -> {
                setModePromptType(totalDistance)
            }
        }
    }

    /**
     * Set the prompt type if no constraints are violated.
     */
    private fun setModePromptType(totalDistance: Double) {
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

    /**
     * Generate the prompt according to the PromptType set from the analysis done on the trajectory.
     * Sets the TextViews for the RDE prompt and analysis depending on the PromptType
     */
    fun determinePrompt(totalDistance: Double, trajectoryAnalyser: TrajectoryAnalyser) {
        this.trajectoryAnalyser = trajectoryAnalyser
        // Analyse the trajectory driven so far
        analyseTrajectory(totalDistance)

        // Set the prompt and analysis according to the prompt type
        when (promptType) {
            PromptType.SUFFICIENCY -> {
                if (sufficientDrivingMode != null) {
                    setSufficientPrompt(sufficientDrivingMode!!)
                } else {
                    setSufficientPrompt(DrivingMode.URBAN) // TODO change this to a better value
                }
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
     * Set the analysis text for the constraint of driving at 100km/h or more for at least 5 minutes.
     * @param highSpeedDuration The duration of driving at > 100km/h.
     */
    private fun setHighSpeedPrompt(highSpeedDuration: Double){
        analysisText =
            "You need to drive at 100km/h or more for at least $highSpeedDuration more minutes."
        analysisColour = Color.BLACK
    }

    /**
     * Set analysis text for the constraint of driving of 145km/h or more for only 3% of the
     * motorway driving.
     * @param veryHighSpeedPercentage The very high speed percentage.
     */
    private fun setVeryHighSpeedPrompt(veryHighSpeedPercentage: Double){
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

    // TODO: Round values to 2 decimal places
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
                promptText = "Your average urban speed (${averageUrbanSpeed}km/h) is close to being invalid."
                analysisText = "You are ${changeSpeed}km/h away from exceeding the upper bound."
                promptColour = Color.RED
            }
            averageUrbanSpeed > 15 && averageUrbanSpeed < 20 -> {
                promptText = "Your average urban speed (${averageUrbanSpeed}km/h) is close to being invalid."
                analysisText = "You are ${-changeSpeed}km/h above the lower bound."
                promptColour = Color.GREEN
            }
            changeSpeed < 0 -> {
                promptText = "Your average urban speed (${averageUrbanSpeed}km/h) is too high."
                analysisText = "You are ${-changeSpeed}km/h more than the upper bound."
                promptColour = Color.RED
            }
            changeSpeed > 0 -> {
                promptText = "Your average urban speed (${averageUrbanSpeed}km/h) is too low."
                analysisText = "You are ${changeSpeed}km/h less than the lower bound."
                promptColour = Color.GREEN
            }
        }
    }

    /**
     * Set prompt text for the stopping percentage.
     * @param stoppingPercentage The difference in stopping percentage from the upper or lower bounds
     */
    private fun setStoppingPercentagePrompt(stoppingPercentage: Double) {
        if (stoppingPercentage > 0) {
            promptText = "You are stopping too little. Try to stop more."
            analysisText = "You need to stop for at least ${stoppingPercentage * 100}% more of the urban time."
            promptColour = Color.RED
        } else{
            promptText = "You are close to exceeding the stopping percentage. Try to stop less."
            analysisText = "You are stopping ${-(stoppingPercentage) * 100}% less than the upper bound."
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
     * Set analysis textview for the driving style prompt.
     * @param duration The duration for which the driver should drive at the desired driving mode.
     */
    private fun setDrivingStyleAnalysis(duration: Double) {
        analysisText = when (desiredDrivingMode) {
            DrivingMode.URBAN -> {
                "Drive at an average speed of 30 km/h for at most $duration minutes."
            }
            DrivingMode.RURAL -> {
                "Drive at an average speed of 75 km/h for at most $duration minutes"
            }
            DrivingMode.MOTORWAY -> {
                "Drive at an average speed of 115 km/h for at most $duration minutes"
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