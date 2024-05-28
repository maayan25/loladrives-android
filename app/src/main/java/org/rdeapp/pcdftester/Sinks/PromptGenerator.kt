package org.rdeapp.pcdftester.Sinks

import android.graphics.Color
import android.util.Log
import java.util.Calendar

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
    private var lastUpdated: Long = 0

    // Array to store the current state of the RDE test constraints
    private var constraints: Array<Double?> = arrayOf(null)

    // Last update to the driving mode
    private var lastUpdateDrivingMode: Long = 0

    private var lastUpdatePrompt: Double = 0.0


    /**
     * Analyse the trajectory driven so far by using functions from the TrajectoryAnalyser class. If
     * the distance travelled is less than 1/3 of the expected distance, do not analyse.
     * Otherwise, set the desired driving mode, the required speed change, and the prompt type
     * according to the constraints on the driving style.
     *
     * @param totalDistance The total distance travelled so far.
     */
    private fun analyseTrajectory(totalDistance: Double) {
        val currentDrivingMode = trajectoryAnalyser.currentDrivingMode()
        if (sufficientDrivingMode != null || trajectoryAnalyser.getTotalTime() > 5 || trajectoryAnalyser.getSufficient(currentDrivingMode)) {
            val previousDrivingMode = desiredDrivingMode
            // set the desired driving mode accrued to the sufficient driving modes so far
            desiredDrivingMode = trajectoryAnalyser.setDesiredDrivingMode()

            // Fix the driving mode if it has not changed for 15 seconds
            if (desiredDrivingMode == previousDrivingMode && currentDrivingMode != desiredDrivingMode && getTimeDifferenceDrivingMode() > 100000 || lastUpdatePrompt > 0.0) {
                desiredDrivingMode = currentDrivingMode
                trajectoryAnalyser.updateDesiredDrivingMode(desiredDrivingMode)
                if (lastUpdatePrompt > 0.0) {
                    desiredDrivingMode = previousDrivingMode
                } else {
                    lastUpdatePrompt = 1.0
                }
                if (getTimeDifferenceDrivingMode() > 150000) {
                    println("Fixing the driving mode to $desiredDrivingMode")
                    lastUpdateDrivingMode = System.currentTimeMillis()
                    lastUpdatePrompt = 0.0
                }
            }

            Log.d("desired driving t",getTimeDifferenceDrivingMode().toString())

            // get the speed change needed to improve the driving style
            speedChange = trajectoryAnalyser.computeSpeedChange()

            // get the constraints on the current driving style
            constraints = trajectoryAnalyser.getConstraints()

            // set the prompt type according to the constraints, if at least 30 seconds have passed
            if (getTimeDifference() > 30) {
                setPromptType(constraints, totalDistance)
                setPromptUpdated()
            }
        } else {
            setModePromptType(totalDistance)
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
                if (veryHighSpeed != null && veryHighSpeed != 0.0 && promptType != PromptType.HIGHSPEEDPERCENTAGE) {
                    promptType = PromptType.VERYHIGHSPEEDPERCENTAGE
                } else if (highSpeed != null && highSpeed != 0.0) {
                    promptType = PromptType.HIGHSPEEDPERCENTAGE
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

        if (trajectoryAnalyser.getTotalTime() > 90 && trajectoryAnalyser.getTotalTime() < 120) {
            promptType = PromptType.INVALIDRDEREASON
        }
    }

    /**
     * Set the timestamp of the last update to the prompt type.
     * This is used to determine if the prompt type has changed recently.
     */
    private fun setPromptUpdated() {
        lastUpdated = Calendar.getInstance().timeInMillis
    }

    /**
     * Set the timestamp of the last update to the driving mode.
     * This is used to determine if the driving mode has changed recently.
     */
    private fun setDrivingModeUpdated() {
        lastUpdateDrivingMode = Calendar.getInstance().timeInMillis
    }

    /**
     * @return the time passed since the last update to the prompt type.
     */
    private fun getTimeDifference(): Double {
        return (Calendar.getInstance().timeInMillis - lastUpdated) / 1000.0
    }

    /**
     * @return the time passed since the last update to the driving mode.
     */
    private fun getTimeDifferenceDrivingMode(): Long {
        return (Calendar.getInstance().timeInMillis - lastUpdateDrivingMode)
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
        promptType = if (totalDistance < expectedDistance / 5) {
            // Less than 1/3 of the expected distance is travelled, check if any driving style has become sufficient.
            sufficientDrivingMode = trajectoryAnalyser.checkSufficient()
            if (sufficientDrivingMode != null) {
                PromptType.SUFFICIENCY // Some driving style has just become sufficient
            } else {
                PromptType.NONE
            }
        } else {
            if (trajectoryAnalyser.getTotalTime() > 90.0){
                PromptType.NONE
            } else {
                // More than 1/3 of the expected distance is travelled, check the driving style.
                PromptType.DRIVINGSTYLE
            }
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
                val stoppingPercentage = trajectoryAnalyser.getStoppingPercentage()
                setStoppingPercentagePrompt(stoppingPercentage, constraints[2]!!)
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
            PromptType.INVALIDRDEREASON -> {
                setInvalidRDEPrompt()
            }
        }
    }

    /**
     *
     */
    private fun setInvalidRDEPrompt() {
        val isValid = trajectoryAnalyser.getIsValid()
        val notRDEtest = trajectoryAnalyser.getNotRDETest()
        val reason = generateInvalidRDEReason(isValid, notRDEtest)
        val time = String.format("%.3f", trajectoryAnalyser.getTotalTime()).toDouble()
        if (isValid == 1.0) {
            promptText = "This RDE test is valid."
            analysisText = "The RDE test is valid because all constraints are met."
            promptColour = Color.GREEN
            analysisColour = Color.BLACK
        } else {
            promptText = "A valid RDE test time of $time has passed."
            analysisText = "This RDE test is invalid because ${reason.toLowerCase()}."
            promptColour = Color.RED
            analysisColour = Color.BLACK
        }
    }

    /**
     * Set the prompt text and analysis text for the case where no prompt is needed yet.
     * Gives the driver information on the current speed and the percentage of the required
     * driving completed so far.
     * (Less than 1/3 of the expected distance is travelled)
     */
    private fun setNonePrompt() {
        analysisText = "You have completed "
        val urbanPercentage =
            String.format("%.3f", trajectoryAnalyser.getUrbanPercentage()).toDouble().toInt()
        val ruralPercentage =
            String.format("%.3f", trajectoryAnalyser.getRuralPercentage()).toDouble().toInt()
        val motorwayPercentage =
            String.format("%.3f", trajectoryAnalyser.getMotorwayPercentage()).toDouble().toInt()

        promptText = "You are driving at the speed of ${trajectoryAnalyser.getCurrentSpeed().toInt()}km/h."
        if (urbanPercentage != 0) {
            analysisText += "$urbanPercentage% of the required urban driving, "
        }
        if (ruralPercentage != 0) {
            analysisText += "$ruralPercentage% of the required rural driving, "
        }
        if (motorwayPercentage != 0) {
            analysisText += "$motorwayPercentage% of the required motorway driving."
        }
        if (analysisText == "You have completed ") {
            analysisText = "You have completed 0% of the required driving."
        }

        promptColour = Color.BLACK
        analysisColour = Color.BLACK
    }

    /**
     * Set the analysis text for the constraint of driving at 100km/h or more for at least 5 minutes.
     * @param highSpeedDuration The duration of driving at > 100km/h.
     */
    private fun setHighSpeedPrompt(highSpeedDuration: Double) {
        // Round the duration to 2 decimal
        val highSpeedDurationRounded = String.format("%.3f", highSpeedDuration).toDouble()

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
        val averageUrbanSpeedRounded = String.format("%.3f", averageUrbanSpeed).toDouble()
        val changeSpeedRounded = String.format("%.3f", changeSpeed).toDouble()

        when {
            averageUrbanSpeedRounded > 38 && averageUrbanSpeedRounded < 40 -> {
                promptText = "Your average urban speed, ${averageUrbanSpeedRounded}km/h, is close to being invalid."
                analysisText = "You are ${changeSpeedRounded}km/h away from exceeding the upper limit."
                promptColour = Color.RED
            }
            averageUrbanSpeedRounded > 15 && averageUrbanSpeedRounded < 18 -> {
                promptText = "Your average urban speed, ${averageUrbanSpeedRounded}km/h, is close to being invalid."
                analysisText = "You are ${-changeSpeedRounded}km/h above the lower limit."
                promptColour = Color.GREEN
            }
            changeSpeedRounded < 0 -> {
                promptText =
                    "Your average urban speed, ${averageUrbanSpeedRounded}km/h, is too high."
                analysisText =
                    "You are ${-changeSpeedRounded}km/h more than the upper limit."
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
    private fun setStoppingPercentagePrompt(stoppingPercentage: Double, changeStopping: Double) {
        // Round values to 2 decimal places
        val stoppingPercentageRounded = String.format("%.3f", stoppingPercentage * 100).toDouble()
        val changeStoppingRounded = String.format("%.3f", changeStopping * 100).toDouble()

        when {
            stoppingPercentage == 0.0 -> {
                promptText = "You have not stopped at all."
                analysisText = "You need to stop for at least 6% of the urban driving time."
                promptColour = Color.RED
            }
            stoppingPercentage > 0.26 && stoppingPercentage < 0.3 -> {
                promptText = "Your stopping percentage, ${stoppingPercentageRounded}%, is close to being invalid"
                analysisText = "You need to spend ${changeStoppingRounded}% of the urban driving time driving instead of stopping."
                promptColour = Color.RED
            }
            stoppingPercentage > 0.06 && stoppingPercentage < 0.1 -> {
                promptText = "Your stopping percentage, ${stoppingPercentageRounded}%, is close to being invalid"
                analysisText = "You need to stop for at least ${-changeStoppingRounded}% less of the urban driving time."
                promptColour = Color.GREEN
            }
            changeStoppingRounded < 0 -> {
                promptText = "Your stopping percentage, ${stoppingPercentageRounded}%, is too high."
                analysisText = "You need to spend ${-changeStoppingRounded}% more time driving rather than stopping."
                promptColour = Color.RED
            }
            changeStoppingRounded > 0 -> {
                promptText = "Your stopping percentage, ${stoppingPercentageRounded}%, is too low."
                analysisText = "You need to stop for at least ${changeStoppingRounded}% more of the urban driving time."
                promptColour = Color.GREEN
            }
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
            if (trajectoryAnalyser.getSufficient(desiredDrivingMode)){
                promptText = "Driven the required distance for the ${desiredDrivingMode.toString().toLowerCase()} driving style."
                promptColour = Color.BLACK
            } else {
                promptText = "Your current speed, ${trajectoryAnalyser.getCurrentSpeed()}km/h, is an appropriate speed to complete the ${
                    desiredDrivingMode.toString().toLowerCase()
                } driving style."
                promptColour = Color.BLACK
            }
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
        val durationRounded = String.format("%.3f", duration).toDouble()
        analysisText = when (desiredDrivingMode) {
            DrivingMode.URBAN -> {
                if (duration < 0.0) {
                    "Do not drive for more than ${-durationRounded.toInt()} minutes at an average speed of 30 km/h"
                } else {
                    "Drive at an average speed of 30 km/h for at least ${durationRounded.toInt()} minutes."
                }
            }

            DrivingMode.RURAL -> {
                if (duration < 0.0) {
                    "Do not drive for more than ${-durationRounded.toInt()} minutes at an average speed of 75 km/h"
                } else {
                    "Drive at an average speed of 75 km/h for at least ${durationRounded.toInt()} minutes"
                }
            }

            DrivingMode.MOTORWAY -> {
                if (duration < 0.0) {
                    "Do not drive for more than ${-durationRounded.toInt()} at an average speed of 115 km/h"
                } else {
                    "Drive at an average speed of 115 km/h for at least ${durationRounded.toInt()} minutes"
                }
            }
        }

    }

    /**
     * Generate the prompt for an invalid RDE test.
     * Using the outputs from the RTLola analysis, the prompt is generated and displayed to the user.
     * Parameters:
     * @param isValidTest The value of the isValidTest output from the RTLola analysis.
     * @param notRDEtest The value of the notRDEtest output from the RTLola analysis.
     */
    private fun generateInvalidRDEReason(isValidTest: Double, notRDEtest: Double): String {
        // matching isValidTest(1.0...8.0) with appropriate violation prompt
        when (isValidTest) {
            0.0 -> return "Unknown reason for invalid RDE test"
            1.0 -> return "Trip is valid"
            2.0 -> return "Trip duration was too short or too long"
            3.0 -> return "Exceeded the maximum speed"
            4.0 -> return "Invalid stopping percentage"
            5.0 -> return "Exceeded the ambient temperature"
            6.0 -> return "Invalid trip dynamics"
            7.0 -> return "More than 5 long stops"
            8.0 -> return "Invalid average urban speed"
            9.0 -> return "Invalid urban proportion of the trip"
            10.0 -> return "Invalid rural proportion of the trip"
            11.0 -> return "Invalid motorway proportion of the trip"
            12.0 -> return "Failed to satisfy trip requirements"
            else -> return "Unknown reason for invalid RDE test"
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