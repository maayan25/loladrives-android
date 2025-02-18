package org.rdeapp.pcdftester.Sinks

import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Before

import org.junit.Test
import kotlin.concurrent.thread

class VelocityProfileTest {
    lateinit var velocityProfile: VelocityProfile;

    @Before
    fun setUp() {
        velocityProfile = VelocityProfile()
    }

    /**
     * Test if the velocity profile is updated correctly when the speed is 0 km/h.
     * The stopping time should be updated and the other counters should be 0.
     */
    @Test
    fun updateVelocityProfileWithStop() {
        val speed = 0.0

        velocityProfile.updateVelocityProfile(speed)
        val firstIteration = velocityProfile.getTimeDifference()

        assertTrue(firstIteration == velocityProfile.getStoppingTime())
        assertTrue(0.0 == velocityProfile.getHighSpeed())
        assertTrue(0.0 == velocityProfile.getVeryHighSpeed())
    }

    /**
     * Test if the velocity profile is updated correctly when the speed is 10 km/h.
     * None of the counters should be updated.
     */
    @Test
    fun updateVelocityProfileWithSlowSpeed() {
        val speed = 10.0

        velocityProfile.updateVelocityProfile(speed)

        assertTrue(0.0 == velocityProfile.getStoppingTime())
        assertTrue(0.0 == velocityProfile.getHighSpeed())
        assertTrue(0.0 == velocityProfile.getVeryHighSpeed())
    }

    /**
     * Test that the stopping time is calculated correctly when the speed is 0 km/h.
     */
    @Test
    fun getStoppingTime() {
        val speed = 0.0

        // Update the velocity profile with a speed of 0 km/h
        velocityProfile.updateVelocityProfile(speed)
        val firstIteration = velocityProfile.getTimeDifference()
        assertTrue(firstIteration / 60000 == velocityProfile.getStoppingTime())

        // The velocity is still 0 km/h, so the stopping time should increase
        velocityProfile.updateVelocityProfile(speed)
        val secondIteration = velocityProfile.getTimeDifference()
        assertTrue((firstIteration + secondIteration) / 60000 == velocityProfile.getStoppingTime())
    }

    /**
     * Test that the high-speed time is calculated correctly when the speed is 101 km/h.
     */
    @Test
    fun getHighSpeed() {
        val speed = 101.0

        // update the velocity profile with a speed of 101 km/h
        velocityProfile.updateVelocityProfile(speed)
        val firstIteration = velocityProfile.getTimeDifference()

        // The velocity is still 101 km/h, so the high-speed time should increase
        velocityProfile.updateVelocityProfile(speed)
        val secondIteration = velocityProfile.getTimeDifference()
        assertTrue(secondIteration / 60000 == velocityProfile.getHighSpeed())
    }

    /**
     * Test that the very high-speed time is calculated correctly when the speed is 151 km/h.
     * The high-speed time should also be increased.
     */
    @Test
    fun getVeryHighSpeed() {
        val speed = 151.0

        // Update the velocity profile with a speed of 151 km/h
        velocityProfile.updateVelocityProfile(speed)
        val firstIteration = velocityProfile.getTimeDifference()

        // The velocity is still 151 km/h, so the high-speed time should increase
        velocityProfile.updateVelocityProfile(speed)
        val secondIteration = velocityProfile.getTimeDifference()
        assertTrue(secondIteration / 60000 == velocityProfile.getVeryHighSpeed())

        // The high-speed time should also increase
        assertTrue(secondIteration / 60000 == velocityProfile.getHighSpeed())
    }
}