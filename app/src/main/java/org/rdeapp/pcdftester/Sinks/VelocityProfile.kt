package org.rdeapp.pcdftester.Sinks

import java.util.Calendar

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
     */
    fun updateVelocityProfile(speed: Double) {
        // update the timestamp of the last update
        setLastUpdated()

        currentSpeed = speed
        currentTime = Calendar.getInstance().timeInMillis

        // check if the current speed is in a certain range
        setStop()
        setHighSpeed()
        setVeryHighSpeed()
    }

    /**
     * Set the time spent stopping (at 0 km/h).
     */
    private fun setStop() {
        velStop = if (currentSpeed == 0.0 && previousSpeed == 0.0) {
            velStop + (getTimeDifference() / 60000.0)
        } else {
            velStop
        }
    }

    /**
     * Set the time spent at 145 km/h or more.
     */
    private fun setVeryHighSpeed() {
        vel145plus = if (currentSpeed > 145.0 && previousSpeed > 145.0) {
            vel145plus + (getTimeDifference() / 60000.0)
        } else {
            vel145plus
        }
    }

    /**
     * Set the time spent at 100 km/h or more.
     */
    private fun setHighSpeed() {
        vel100plus = if (currentSpeed > 100.0 && previousSpeed > 100.0) {
            vel100plus + (getTimeDifference() / 60000.0)
        } else {
            vel100plus
        }
    }

    /**
     * Set the timestamp of the last update.
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