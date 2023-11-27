package org.rdeapp.pcdftester.Sinks

/**
 * Class to analyse the progress of the test and to check the constraints on the driving modes.
 */
class TrajectoryAnalyser(
    private var expectedDistance: Double,
    private val velocityProfile: VelocityProfile
) {
    // Boolean variables to check the progress of the driving modes
    private var motorwayComplete: Boolean = false
    private var ruralComplete: Boolean = false
    private var urbanComplete: Boolean = false
    private var motorwaySufficient: Boolean = false
    private var ruralSufficient: Boolean = false
    private var urbanSufficient: Boolean = false
    private var sufficientModes = mutableListOf<DrivingMode>()

    // Proportions of the driving modes out of the expected total distance
    private var urbanProportion: Double = 0.0
    private var ruralProportion: Double = 0.0
    private var motorwayProportion: Double = 0.0

    // Variables to keep track of the current state of the test
    private var desiredDrivingMode: DrivingMode = DrivingMode.URBAN
    private var totalTime: Double = 0.0
    private var currentSpeed: Double = 0.0
    private var averageUrbanSpeed: Double = 0.0

    // Variables to keep track of whether the test is invalid
    private var isInvalid: PromptType = PromptType.NONE

    /**
     * Update the analyser with data on the progress of the test according to the received RTLola results.
     * @param urbanDistance The distance travelled in the urban driving mode.
     * @param ruralDistance The distance travelled in the rural driving mode.
     * @param motorwayDistance The distance travelled in the motorway driving mode.
     * @param totalTime The total time travelled so far.
     * @param currentSpeed The current speed of the vehicle.
     * @param averageUrbanSpeed The current average speed of the vehicle in the urban driving mode.
     */
    fun updateProgress(
        urbanDistance: Double,
        ruralDistance: Double,
        motorwayDistance: Double,
        totalTime: Double,
        currentSpeed: Double,
        averageUrbanSpeed: Double
    ) {
        this.totalTime = totalTime
        this.currentSpeed = currentSpeed
        this.averageUrbanSpeed = averageUrbanSpeed

        // total distance travelled so far
        val totalDistance = urbanDistance + ruralDistance + motorwayDistance

        // update the expected distance if the total distance travelled so far is greater than the expected distance
        expectedDistance = maxOf(expectedDistance, totalDistance / 1000.0)

        // update the velocity profile according to the current speed
        velocityProfile.updateVelocityProfile(currentSpeed)

        // check the progress of the driving modes
        urbanProportion = urbanDistance / 1000 / expectedDistance
        ruralProportion = ruralDistance / 1000 / expectedDistance
        motorwayProportion = motorwayDistance / 1000 / expectedDistance

        motorwayComplete = motorwayProportion > 0.43
        ruralComplete = ruralProportion > 0.43
        urbanComplete = urbanProportion > 0.44

        motorwaySufficient = motorwayProportion >= 0.18
        ruralSufficient = ruralProportion >= 0.18
        urbanSufficient = urbanProportion >= 0.23
    }

    /**
     * @return whether the test has exceeded the time limit.
     */
    fun checkTimeLimit(): Boolean {
        return totalTime > 120
    }

    /**
     * @return whether the test is invalid.
     */
    fun checkInvalid(): PromptType {
        return isInvalid
    }

    /**
     * @return the Array of the constraints on motorway and urban driving modes. All return values
     * are null if the constraint is satisfied or is invalid, and a Double otherwise.
     * [0] = isHighSpeedValid()
     * [1] = isVeryHighSpeedValid()
     * [2] = isStoppingTimeValid()
     * [3] = isAverageSpeedValid()
     */
    fun getConstraints(): Array<Double?> {
        return arrayOf(
            isHighSpeedValid(),
            isVeryHighSpeedValid(),
            isStoppingTimeValid(),
            isAverageSpeedValid()
        )
    }

    /**
     * @return the average speed of driving in the urban driving mode.
     */
    fun getAverageUrbanSpeed(): Double {
        return averageUrbanSpeed
    }

    /**
     * Set the desired driving mode according to:
     * the proportions of all driving modes,
     * the current driving mode
     * and the previously desired driving mode.
     */
    fun setDesiredDrivingMode(): DrivingMode {
        when {
            urbanSufficient && !ruralSufficient && !motorwaySufficient -> {
                desiredDrivingMode = chooseNextDrivingMode(DrivingMode.RURAL, DrivingMode.MOTORWAY)
            }
            !urbanSufficient && ruralSufficient && !motorwaySufficient -> {
                desiredDrivingMode = chooseNextDrivingMode(DrivingMode.URBAN, DrivingMode.MOTORWAY)
            }
            !urbanSufficient && !ruralSufficient && motorwaySufficient -> {
                desiredDrivingMode = chooseNextDrivingMode(DrivingMode.URBAN, DrivingMode.RURAL)
            }
            !motorwaySufficient -> {
                desiredDrivingMode = DrivingMode.MOTORWAY
            }
            !ruralSufficient -> {
                desiredDrivingMode = DrivingMode.RURAL
            }
            !urbanSufficient -> {
                desiredDrivingMode = DrivingMode.URBAN
            }
        }

        return desiredDrivingMode
    }

    /**
     * Choose which should be the next driving mode between 2 required driving modes.
     * If the current driving mode or the recently desired driving modes are the 1st option, choose it
     * to be the newly desired mode. Otherwise, choose the alternative.
     * @param firstDrivingMode The 1st driving mode to choose from.
     * @param secondDrivingMode The 2nd driving mode to choose from.
     * @return the chosen driving mode.
     */
    private fun chooseNextDrivingMode(
        firstDrivingMode: DrivingMode,
        secondDrivingMode: DrivingMode
    ): DrivingMode {
        return if (desiredDrivingMode == firstDrivingMode || currentDrivingMode() == firstDrivingMode) {
            firstDrivingMode
        } else {
            secondDrivingMode
        }
    }

    /**
     * Check that a speed of 145km/h is driven for less than 3% of the maximum test time
     * of the motorway driving mode.
     * If this is exceeded, set isInvalid to the relevant prompt.
     * @return the duration of the driven speed if it is valid and requires warning, null otherwise
     */
    private fun isVeryHighSpeedValid(): Double? {
        val veryHighSpeedDuration = velocityProfile.getVeryHighSpeed() // in minutes
        if (veryHighSpeedDuration > 0.03 * 120 * 0.43) {
            // driven in > 145 km/h for more than 3% of the max test time
            isInvalid = PromptType.VERYHIGHSPEEDPERCENTAGE
            return null
        } else if ((0.026 * 90 * 0.29) >= veryHighSpeedDuration && veryHighSpeedDuration >= (0.025 * 90 * 0.29)) {
            return 0.025 // driven in > 145 km/h for more 1.5% of the min test time
        } else if ((0.016 * 90 * 0.29) >= veryHighSpeedDuration && veryHighSpeedDuration >= (0.015 * 90 * 0.29)) {
            return 0.015 // driven in > 145 km/h for more 1.5% of the min test time
        }
        return null
    }

    /**
     * Check that the motorway driving style is valid and does not violate the constraints.
     * If not and they cannot be validated, set isInvalid to the relevant prompt.
     * @return the remaining time to be driven if it is valid and requires instruction, null otherwise
     */
    private fun isHighSpeedValid(): Double? {
        return when (val highSpeed = canHighSpeedPass()) {
            null -> {
                isInvalid = PromptType.HIGHSPEEDPERCENTAGE
                null
            }

            else -> highSpeed // the remaining time to be driven at 100km/h, or 0 if it is exceeded.
        }
    }

    /**
     * Consider time and distance left to compute whether high speed can pass.
     * @return the remaining time to be driven at 100km/h, 0,0 if it is exceeded, and null
     * if it cannot be validated.
     */
    private fun canHighSpeedPass(): Double? {
        val highSpeedDuration = velocityProfile.getHighSpeed() // in minutes
        return if (highSpeedDuration > 5) {
            0.0
        } else {
            if (totalTime + (5 - highSpeedDuration) <= 120) {
                5 - highSpeedDuration // There is enough time to drive at 100km/h for 5 minutes
            } else {
                null
            }
        }
    }

    /**
     * Check if the stopping percentage can be increased or decreased to pass this condition.
     * If it cannot be validated, set isInvalid to the relevant prompt.
     * @return the stopping percentage that can be increased or decreased to pass this condition
     *         or null if it cannot be validated.
     */
    private fun isStoppingTimeValid(): Double? {
        val currentStoppingTime: Double = velocityProfile.getStoppingTime() // in minutes
        val remainingTime = 120 - totalTime

        if (totalTime < 15 || currentStoppingTime == 0.0) {
            // Don't check for stopping time in the first 15 minutes of the test
            // because the stopping percentage is not reliable.
            return null
        }

        when {
            currentStoppingTime > 0.3 * 120.0 && remainingTime < (0.3 * 120.0 - currentStoppingTime) -> {
                // Stopping percentage is invalid and can't be decreased to pass
                isInvalid = PromptType.STOPPINGPERCENTAGE
                return null
            }
            currentStoppingTime < 0.06 * 90.0 && remainingTime < (0.06 * 90.0 - currentStoppingTime) -> {
                // Stopping percentage is invalid and can't be increased to pass
                isInvalid = PromptType.STOPPINGPERCENTAGE
                return null
            }
            totalTime > 30 && currentStoppingTime < 0.02 * totalTime -> {
                // Stopping percentage is very low and some of the test time has passed
                return  0.0
            }
            currentStoppingTime >= 0.03 * 90.0 && currentStoppingTime < 0.06 * 120.0 -> {
                // Stopping percentage (Between 2.7 and 7.2 minutes) is close to being valid but can be increased to pass
                return  0.06 - (currentStoppingTime / 90.0)
            }
            currentStoppingTime > 0.25 * 90 && currentStoppingTime < 0.3 * 120 -> {
                // Stopping percentage (between 22.5 and 36 minutes) is close to being invalid but can be decreased to pass
                return (currentStoppingTime / 120) - 0.3
            }
            0.06 * totalTime <= currentStoppingTime && currentStoppingTime <= 0.3 * totalTime -> {
                return null
            }
            else -> {
                return null
            }
        }
    }

    /**
     * Check if the average urban speed is between 15km/h and 40km/h, or can be increased or
     * decreased to pass this condition.
     * If it cannot be validated, set isInvalid to the relevant prompt.
     * @return the change in average urban speed to pass this condition
     *         eg. -5 or 10, or null if it is either valid or cannot be validated.
     */
    private fun isAverageSpeedValid(): Double? {
        val urbanDistanceLeft = (0.44 - urbanProportion) * expectedDistance
        val remainingTime = 120.0 - totalTime
        // Estimate an average speed with remaining time and distance left in urban driving
        val requiredSpeed = urbanDistanceLeft / remainingTime

        when {
            totalTime < 15.0 -> {
                // Don't check for average speed in the first 15 minutes of the test
                // because the average speed is not reliable.
                return null
            }
            averageUrbanSpeed < 15.0 && (15.0 > requiredSpeed || 40.0 < requiredSpeed) && remainingTime < 20.0 -> {
                // Need to drive faster to make the average urban speed higher to pass but can't
                isInvalid = PromptType.AVERAGEURBANSPEED
                return null
            }
            averageUrbanSpeed > 40.0 && (urbanDistanceLeft == 0.0 || (15.0 > requiredSpeed || 40.0 < requiredSpeed)) && remainingTime < 20.0 -> {
                // Need to drive slower to make the average urban speed lower to pass but can't
                isInvalid = PromptType.AVERAGEURBANSPEED
                return null
            }
            averageUrbanSpeed > 35.0 && averageUrbanSpeed < 40.0 || averageUrbanSpeed > 40.0 -> {
                // average speed is high and close to being invalid or exceeded the limit but can be decreased to pass
                return 40.0 - averageUrbanSpeed
            }
            averageUrbanSpeed > 15.0 && averageUrbanSpeed < 20.0 || averageUrbanSpeed < 15.0 -> {
                // average speed is low and close to being invalid or exceeded the limit but can be increased to pass
                return 15.0 - averageUrbanSpeed
            }
            averageUrbanSpeed > 15.0 && averageUrbanSpeed < 40.0 -> {
                // average speed is valid
                return null
            }
            else -> {
                return null
            }
        }
    }

    /**
     * Check whether the distance in a driving mode is sufficient. If it is, add the driving mode
     * to the sufficientModes list.
     * @return a driving style has recently become sufficient, or null if no sufficiency has changed.
     */
    fun checkSufficient(): DrivingMode? {
        if (motorwaySufficient && !sufficientModes.contains(DrivingMode.MOTORWAY)) {
            sufficientModes.add(DrivingMode.MOTORWAY)
            return DrivingMode.MOTORWAY
        } else if (ruralSufficient && !sufficientModes.contains(DrivingMode.RURAL)) {
            sufficientModes.add(DrivingMode.RURAL)
            return DrivingMode.RURAL
        } else if (urbanSufficient && !sufficientModes.contains(DrivingMode.URBAN)) {
            sufficientModes.add(DrivingMode.URBAN)
            return DrivingMode.URBAN
        }
        return null
    }

    /**
     * Determine the current driving mode according to the current speed, and the thresholds
     * determined by the RDE test regulations.
     * @return the current driving mode.
     */
    fun currentDrivingMode(): DrivingMode {
        return when {
            currentSpeed < 60 -> DrivingMode.URBAN
            currentSpeed < 90 -> DrivingMode.RURAL
            else -> DrivingMode.MOTORWAY
        }
    }

    /**
     * Calculate the speed change required to enter the speed range for the desired driving mode.
     * @return the difference in speed required to improve the driving style if the current speed
     * is not in range. Otherwise, return 0.0.
     */
    fun computeSpeedChange(): Double {
        val lowerThreshold: Double
        val upperThreshold: Double

        when (desiredDrivingMode) {
            DrivingMode.URBAN -> {
                lowerThreshold = 0.0; upperThreshold = 60.0
            }

            DrivingMode.RURAL -> {
                lowerThreshold = 60.0; upperThreshold = 90.0
            }

            DrivingMode.MOTORWAY -> {
                lowerThreshold = 90.0; upperThreshold = 145.0
            }
        }

        return if (currentSpeed < lowerThreshold) {
            lowerThreshold - currentSpeed
        } else if (currentSpeed > upperThreshold) {
            upperThreshold - currentSpeed
        } else {
            0.0
        }
    }

    /**
     * Calculate how long the user to should drive in the certain driving mode to improve their
     * driving style.
     * The duration is calculated by the distance left to drive in the certain driving mode divided
     * by the average speed of that driving mode.
     * @return the duration in minutes.
     */
    fun computeDuration(): Double {
        when (desiredDrivingMode) {
            DrivingMode.URBAN -> {
                // Calculate the distance left to drive in urban mode with an average speed of 30 km/h
                val urbanDistanceLeft = (0.44 - urbanProportion) * expectedDistance
                return urbanDistanceLeft * 2
            }

            DrivingMode.RURAL -> {
                // Calculate the distance left to drive in rural mode with an average speed of 75 km/h
                val ruralDistanceLeft = (0.43 - ruralProportion) * expectedDistance
                return ruralDistanceLeft * 0.8
            }

            DrivingMode.MOTORWAY -> {
                // Calculate the distance left to drive in motorway mode with an average speed of 115 km/h
                val motorwayDistanceLeft = (0.43 - motorwayProportion) * expectedDistance
                return motorwayDistanceLeft * 60 / 115
            }
        }
    }

    /**
     * Return total time the test has been running for
     */
    fun getTotalTime(): Double{
        return totalTime
    }
}