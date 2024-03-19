package org.rdeapp.pcdftester.Sinks

import java.util.Calendar

/**
 * Class for handling the velocity profile of the driver during a specific RDE test.
 */
class VelocityProfile {
    private var currentSpeed: Double = 0.0
    private var previousSpeed: Double = 0.0
    private var velStop: Double = 0.0
    private var vel100plus: Double = 0.0
    private var vel145plus: Double = 0.0
    private var lastUpdated: Long = 0
    private var currentTime: Long = Calendar.getInstance().timeInMillis

    /**
     * Update the velocity profile with a new speed.
     * @param speed The current speed of the vehicle in km/h.
     */
    fun updateVelocityProfile(speed: Double) {
        // update the timestamp of the last update
        setLastUpdated()

        currentSpeed = speed
        currentTime = Calendar.getInstance().timeInMillis

        // check if the current speed is in a certain range, and update the velocity profile accordingly.
        setStop()
        setHighSpeed()
        setVeryHighSpeed()
    }

    /**
     * Update the time spent stopping (at 0.0 km/h).
     */
    private fun setStop() {
        velStop = if (currentSpeed == 0.0 && previousSpeed == 0.0) {
            velStop + (getTimeDifference() / 60000.0)
        } else {
            velStop
        }
    }

    /**
     * Update the time spent at 145 km/h or more.
     */
    private fun setVeryHighSpeed() {
        vel145plus = if (currentSpeed > 145.0 && (previousSpeed == 0.0 || previousSpeed > 145.0)) {
            vel145plus + (getTimeDifference() / 60000.0)
        } else {
            vel145plus
        }
    }

    /**
     * Update the time spent at 100 km/h or more.
     */
    private fun setHighSpeed() {
        vel100plus = if (currentSpeed > 100.0 && (previousSpeed == 0.0 || previousSpeed > 100.0)) {
            vel100plus + (getTimeDifference() / 60000.0)
        } else {
            vel100plus
        }
    }

    /**
     * Update the timestamp of the last update.
     */
    private fun setLastUpdated() {
        lastUpdated = currentTime
    }

    /**
     * Get the time spent stopping (at 0 km/h) in minutes.
     */
    fun getStoppingTime(): Double {
        return velStop
    }

    /**
     * Get the time spent at 100 km/h or more in minutes.
     */
    fun getHighSpeed(): Double {
        return vel100plus
    }

    /**
     * Get the time spent at 145 km/h or more in minutes.
     */
    fun getVeryHighSpeed(): Double {
        return vel145plus
    }

    /**
     * Get the time passed since the last update in milliseconds
     */
    fun getTimeDifference(): Double {
        return (currentTime - lastUpdated).toDouble()
    }
}