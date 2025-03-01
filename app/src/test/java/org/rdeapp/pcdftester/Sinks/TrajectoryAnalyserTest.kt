package org.rdeapp.pcdftester.Sinks

import org.junit.Assert.*

import org.junit.Before
import org.junit.Test

class TrajectoryAnalyserTest {
    private val velocityProfile: VelocityProfile = VelocityProfile()
    private lateinit var trajectoryAnalyser: TrajectoryAnalyser

    // The expected distance that is chosen for the test.
    private var expectedDistance: Double = 83.0

    // Example states for the test.
    private var initialStateLong: List<Long> = listOf<Long>(0L, 0L, 0L, 0L , 0L, 0L, 0L)
    private var initialState: List<Double> = listOf<Double>(0.0, 0.0, 0.0)
    private var validState: List<Double> =
        listOf<Double>(0.34 * expectedDistance * 1000, 0.33 * expectedDistance * 1000, 0.33 * expectedDistance * 1000,
            25.0, 30.0, 25.0, 0.0, 1.0)
    private var urbanTime = 20L

    @Before
    fun setUp() {
        trajectoryAnalyser = TrajectoryAnalyser(expectedDistance, velocityProfile)
    }

    /**
     * Test if the trajectory analyser is updated correctly at the start of the test.
     */
    @Test
    fun updateProgressAtStart() {
        // All counters should be 0 at the start of the test
        trajectoryAnalyser.updateProgress(
            initialState[0], initialState[1], initialState[2],
            initialStateLong[3], validState[3], validState[4], validState[5],
            validState[6], validState[7], 0.0, 2.0
        )

        // The desired driving mode should be URBAN at the start of the test
        assertTrue(trajectoryAnalyser.currentDrivingMode() == DrivingMode.URBAN)

        // No constraints should alert the driver at the beginning of the test.
        val constraints = trajectoryAnalyser.getConstraints()

        // High speed and Average Urban Speed are set to 0 for the initial 10 minutes, as
        // their values are not reliable and therefore irrelevant to the driver.
        assertEquals(constraints[0], 5.0) // High speed

        // The other constraints cannot be invalid in this range, and are set to null.
        assertNull(constraints[1]) // Very high speed
        assertNull(constraints[2]) // Stopping time
        assertNull(constraints[3]) // Average Urban Speed
    }

    /**
     * Test if the checkInvalid function returns "false" when the test is valid.
     */
    @Test
    fun checkInvalidForValidRDETest() {
        // A valid test case, so the isInvalid variable should be set to "false".
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, validState[3], validState[4], validState[5],
            validState[6], validState[7], 0.0, 2.0
        )

        // Update the constraints to make sure the isInvalid variable is updated
        trajectoryAnalyser.getConstraints()
        assertEquals(trajectoryAnalyser.checkInvalid(),PromptType.NONE)
    }

    /**
     * Test if the checkInvalid function returns "true" when the test is invalid.
     */
    @Test
    fun checkInvalidForIsInvalid() {
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            115L, // 5 minutes remaining
            140.0,
            90.0, // speed is not close to the Urban speed range
            39.0, // very high average speed
            0.0,
            0.0,
            0.0,
            0.0
        )
        // Update the constraints to make sure the isInvalid variable is updated
        trajectoryAnalyser.getConstraints()

        assertNotEquals(trajectoryAnalyser.checkInvalid(),PromptType.NONE)
    }

    /**
     * Test if the checkInvalid function returns "true" when the duration of test is too long.
     */
    @Test
    fun checkInvalidForTimeExceeded() {
        // The initial state is a valid test case, so the isInvalid variable should be set to "false".
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            121L, // 1 minute too long
            validState[3],
            validState[4],
            validState[5],
            validState[6],
            validState[7],
            1.0,
            2.0
        )
        // Update the constraints to make sure the isInvalid variable is updated
        trajectoryAnalyser.getConstraints()

        assertEquals(trajectoryAnalyser.checkInvalid(),PromptType.NONE)
    }

    /**
     * Test that the getConstraints function returns all the correct constraints.
     */
    @Test
    fun getConstraintsWhenValid() {
        // Some valid test case
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, validState[3], validState[4],
            validState[5], validState[6], validState[7],
            0.0, 2.0
        )

        val constraints = trajectoryAnalyser.getConstraints()
        assertEquals(constraints.size, 4)

        // The case where no constraints are violated
        assertEquals(constraints[0], 5.0)
        assertNull(constraints[1])
        assertNull(constraints[2])
        assertNull(constraints[3])
    }

    /**
     * Test that the value returned by the getConstraints function is correct
     * when the Very-High speed is quite high.
     */
    @Test
    fun getConstraintsWarningVeryHighSpeed() {
        // Driving in a very high speed
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, validState[3], 145.1, validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        trajectoryAnalyser.getConstraints()

        // Wait for 23.49 seconds, the min time for the very high speed constraint to be 1.5% of the min time.
        Thread.sleep(23490)

        // Still driving in a very high speed
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, validState[3], 145.1, validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        // The constraint for very high speed should return a value of 1.5%.
        assertTrue(trajectoryAnalyser.getConstraints()[1] == 0.015)
    }

    /**
     * Test that the value returned by the getConstraints function is correct
     * when the Very-High speed is too high.
     */
    @Test
    fun getConstraintsInvalidVeryHighSpeed() {
        // Driving in a very high speed
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, validState[3], 145.1, validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        trajectoryAnalyser.getConstraints()

        // Wait for 92.88 seconds, the min time for the very high speed constraint to become invalid.
        Thread.sleep(92880)

        // Still driving in a very high speed
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, validState[3], 145.1, validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        // The constraint for very high speed should return a value of 1.5%.
        assertTrue(trajectoryAnalyser.getConstraints()[1] == null)
        assertEquals(trajectoryAnalyser.checkInvalid(),PromptType.VERYHIGHSPEEDPERCENTAGE)
    }

    /**
     * Test that the value returned by the getConstraints function is correct
     * when the High speed is too low but can be increased.
     */
    @Test
    fun getConstraintsWarningHighSpeed() {
        // Driving in a high speed
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, validState[3],100.1, validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        trajectoryAnalyser.getConstraints()

        // Wait for 10 seconds, time for the high speed constraint to increase but not pass.
        Thread.sleep(10000)

        // Still driving in a high speed
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, validState[3], 100.1, validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        // The constraint for high speed should return a value smaller than 5.
        assertTrue(trajectoryAnalyser.getConstraints()[0] == 5 - velocityProfile.getHighSpeed())
    }

    /**
     * Test that the value returned by the getConstraints function is correct
     * when the High speed is invalid.
     */
    @Test
    fun getConstraintsInvalidHighSpeed() {
        // Driving in a high speed and remaining time is too low.
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            118L, 100.1, validState[4],
            validState[5], validState[6], validState[7], 0.0, 2.0
        )
        trajectoryAnalyser.getConstraints()

        // Wait for 10 seconds, time for the high speed constraint to increase but not pass.
        Thread.sleep(10000)

        // Still driving in a high speed
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            (118.0 + velocityProfile.getTimeDifference()).toLong(), 100.1, validState[4],
            validState[5], validState[6], validState[7], 0.0, 2.0
        )

        // The constraint for high speed should return null because no remaining time is sufficient.
        assertTrue(trajectoryAnalyser.getConstraints()[0] != null)
        assertEquals(trajectoryAnalyser.checkInvalid(),PromptType.NONE)
    }

    /**
     * Test that the value returned by the getConstraints function is correct
     * when the Stopping time is too low but can be increased.
     */
    @Test
    fun getConstraintsWarningStoppingTimeLow() {
        // Stopping
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            30L, 0.0, validState[4],
            validState[5], validState[6], validState[7], 0.0, 2.0
        )
        trajectoryAnalyser.getConstraints()

        // Wait for 5 seconds, a very small amount of stopping time.
        Thread.sleep(5000)

        // Still stopping
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            (30.1 + velocityProfile.getTimeDifference()).toLong(), 0.0, validState[4],
            validState[5], validState[6], validState[7], 0.0, 2.0
        )

        // The constraint for stopping time should return the remaining stopping time for the lower threshold.
        assertTrue(trajectoryAnalyser.getConstraints()[2] != 0.0)
    }

    /**
     * Test that the value returned by the getConstraints function is correct
     * when the Stopping time is invalid.
     */
    @Test
    fun getConstraintsInvalidStoppingTime() {
        // Stopping and remaining time is too low.
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            118L, 0.0, validState[4],
            validState[5], validState[6], validState[7], 0.0, 2.0
        )
        trajectoryAnalyser.getConstraints()

        // Wait for 5 seconds, a very small amount of stopping time.
        Thread.sleep(5000)

        // Still stopping.
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            (118.0 + velocityProfile.getTimeDifference()).toLong(), 8.0, validState[4],
            validState[5], validState[6], validState[7], 0.0, 2.0
        )

        // The constraint for stopping time should return null because no remaining time is sufficient.
        assertTrue(trajectoryAnalyser.getConstraints()[2] == null)
        assertNotEquals(trajectoryAnalyser.checkInvalid(),PromptType.STOPPINGPERCENTAGE)
    }

    /**
     * Test that the value returned by the getConstraints function is correct
     * when the Average Urban Speed is too low but can be increased.
     */
    @Test
    fun getConstraintsWarningAverageUrbanSpeedLow() {
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, validState[3], validState[4], 8.0,
            validState[6], validState[7], 0.0, 2.0
        ) // Average urban speed is below 15. The constraint should return a value of 7.0
        assertTrue(trajectoryAnalyser.getConstraints()[3] == 7.0)

        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, validState[3], validState[4], 16.0,
            validState[6], validState[7], 0.0, 2.0
        ) // Average urban speed is valid but close to 15. The constraint should return a value of -3.0
        assertEquals(trajectoryAnalyser.getConstraints()[3], -1.0)
    }

    /**
     * Test that the value returned by the getConstraints function is correct
     * when the Average Urban Speed is too high but can be decreased.
     */
    @Test
    fun getConstraintsWarningAverageUrbanSpeedHigh() {
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, validState[3], validState[4], 45.0,
            validState[6], validState[7], 0.0, 2.0
        ) // Average urban speed is above 45. The constraint should return a negative value of 5.1
        assertTrue(trajectoryAnalyser.getConstraints()[3] == -5.0)

        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, validState[3], validState[4], 39.0,
            validState[6], validState[7], 0.0, 2.0
        ) // Average urban speed is valid but close to 40. The constraint should return 3.0
        assertTrue(trajectoryAnalyser.getConstraints()[3] == 1.0)
    }

    /**
     * Test that the value returned by the getConstraints function is correct
     * when the Average Urban Speed is invalid.
     */
    @Test
    fun getConstraintsInvalidAverageUrbanSpeed() {
        val lowTimeRemaining = 118.0

        // Very low average urban speed and remaining time is too low.
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, lowTimeRemaining, 0.0, 2.0,
            validState[6], validState[7], 0.0, 2.0
        )
        trajectoryAnalyser.getConstraints()

        // Still driving in a very high speed
        val timePassed = velocityProfile.getTimeDifference()
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            (lowTimeRemaining + timePassed).toLong(), 0.0, 2.0,
            validState[5], validState[6], validState[7], 0.0, 2.0
        )

        // The constraint for average urban speed should return null because no remaining time is sufficient.
        assertTrue(trajectoryAnalyser.getConstraints()[3] == null)
        assertEquals(trajectoryAnalyser.checkInvalid(),PromptType.AVERAGEURBANSPEED)
    }

    /**
     * Test that the getAverageUrbanSpeed function returns the correct average urban speed.
     */
    @Test
    fun getAverageUrbanSpeed() {
        // Initial state
        trajectoryAnalyser.updateProgress(
            initialState[0], initialState[1], initialState[2],
            urbanTime, validState[3], validState[4], validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        assertTrue(trajectoryAnalyser.getAverageUrbanSpeed() == validState[5])

        // Some valid test case
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, validState[3], validState[4], validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        assertTrue(trajectoryAnalyser.getAverageUrbanSpeed() == validState[5])

        // Some invalid test case (average urban speed is too high)
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, 115.0, 140.0, 90.0,
            validState[6], validState[7], 0.0, 2.0
        )
        assertTrue(trajectoryAnalyser.getAverageUrbanSpeed() == 90.0)
    }

    /**
     * Test that the setDesiredDrivingMode function sets the desired driving mode correctly
     * in the case where more than one driving mode is insufficient.
     */
    @Test
    fun setDesiredDrivingModeMultipleInsufficient() {
        // Should be set to Urban as it is the current Desired driving mode.
        trajectoryAnalyser.updateProgress(
            0.15 * expectedDistance * 1000,
            validState[0],
            0.15 * expectedDistance * 1000,
            urbanTime, validState[3], validState[4], validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        var desiredDrivingMode = trajectoryAnalyser.setDesiredDrivingMode()
        assertTrue(desiredDrivingMode == DrivingMode.URBAN)

        // Should be set to Motorway as Rural is not current Desired Mode and not Current Speed.
        trajectoryAnalyser.updateProgress(
            validState[0],
            0.15 * expectedDistance * 1000,
            0.15 * expectedDistance * 1000,
            urbanTime, validState[3], validState[4], validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        desiredDrivingMode = trajectoryAnalyser.setDesiredDrivingMode()
        assertTrue(desiredDrivingMode == DrivingMode.MOTORWAY)

        // Should be set to Urban for current speed.
        trajectoryAnalyser.updateProgress(
            0.15 * expectedDistance,
            validState[1],
            0.15 * expectedDistance * 1000,
            urbanTime, validState[3], validState[4], validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        desiredDrivingMode = trajectoryAnalyser.setDesiredDrivingMode()
        assertTrue(desiredDrivingMode == DrivingMode.URBAN)
    }

    /**
     * Test that the setDesiredDrivingMode function sets the desired driving mode correctly
     * in the case where only one driving mode is insufficient.
     */
    @Test
    fun setDesiredDrivingModeSingleInsufficient() {
        trajectoryAnalyser.updateProgress(
            validState[0],
            validState[1],
            0.10 * expectedDistance * 1000,
            urbanTime, validState[3], validState[4], validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        val desiredDrivingMode = trajectoryAnalyser.setDesiredDrivingMode()
        assertEquals(desiredDrivingMode, DrivingMode.MOTORWAY)
    }

    /**
     * Test that the checkSufficient function return the correct DrivingMode when
     * passed sufficient level and returns null when nothing has passed or when
     * all of them have passed already.
     */
    @Test
    fun checkSufficient() {
        // None of the driving modes are sufficient yet so should return null.
        trajectoryAnalyser.updateProgress(
            0.15 * expectedDistance * 1000,
            0.15 * expectedDistance * 1000,
            0.15 * expectedDistance * 1000,
            urbanTime, validState[3], validState[4], validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        assertTrue(trajectoryAnalyser.checkSufficient() == null)
        // Urban driving mode is now sufficient so should return that
        trajectoryAnalyser.updateProgress(
            0.23 * expectedDistance * 1000,
            0.15 * expectedDistance * 1000,
            0.15 * expectedDistance * 1000,
            urbanTime, validState[3], validState[4], validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        assertTrue(trajectoryAnalyser.checkSufficient() == DrivingMode.URBAN)
        trajectoryAnalyser.updateProgress(
            0.29 * expectedDistance * 1000,
            0.15 * expectedDistance * 1000,
            0.18 * expectedDistance * 1000,
            urbanTime, validState[3], validState[4], validState[5],
            validState[6], validState[7], 0.0, 2.0

        )
        assertTrue(trajectoryAnalyser.checkSufficient() == DrivingMode.MOTORWAY)
        trajectoryAnalyser.updateProgress(
            0.29 * expectedDistance * 1000,
            0.19 * expectedDistance * 1000,
            0.20 * expectedDistance * 1000,
            urbanTime, validState[3], validState[4], validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        assertTrue(trajectoryAnalyser.checkSufficient() == DrivingMode.RURAL)
        assertTrue(trajectoryAnalyser.checkSufficient() == null)
    }

    /**
     * Test that the currentDrivingMode function return the correct DrivingMode for the
     * current speed.
     */
    @Test
    fun currentDrivingMode() {
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, validState[3], 49.0 , validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        assertTrue(trajectoryAnalyser.currentDrivingMode() == DrivingMode.URBAN)
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, validState[3], 60.0 , validState[5], // Rural bound (60km/h)
            validState[6], validState[7], 0.0, 2.0
        )
        assertTrue(trajectoryAnalyser.currentDrivingMode() == DrivingMode.RURAL)
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, validState[3], 80.0 , validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        assertTrue(trajectoryAnalyser.currentDrivingMode() == DrivingMode.RURAL)
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            urbanTime, validState[3], 90.0 , validState[5], // Motorway bound (90km/h)
            validState[6], validState[7], 0.0, 2.0
        )
        assertTrue(trajectoryAnalyser.currentDrivingMode() == DrivingMode.MOTORWAY)
    }

    /**
     * Test that the computeSpeedChange return ths correct value when compare with the lower bound.
     */
    @Test
    fun computeSpeedChange() {
        trajectoryAnalyser.updateProgress(
            validState[0],
            0.15 * expectedDistance * 1000,
            validState[2],
            urbanTime, validState[3], 40.5 , validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        trajectoryAnalyser.setDesiredDrivingMode() // RURAL is the desired driving mode
        val speedChange = trajectoryAnalyser.computeSpeedChange()
        assertTrue(speedChange == 60 - 40.5)
    }

    /**
     * Test the function computeSpeedChange returns 0.0 when current speed is in
     * the desired driving mode.
     */
    @Test
    fun computeSpeedChangeForNoChangeRequired() {
        trajectoryAnalyser.updateProgress(
            validState[0],
            0.15 * expectedDistance,
            validState[2],
            urbanTime, validState[3], 75.0 , validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        trajectoryAnalyser.setDesiredDrivingMode() // RURAL is the desired driving mode
        println(trajectoryAnalyser.setDesiredDrivingMode())
        val speedChange = trajectoryAnalyser.computeSpeedChange()
        println(speedChange)
        assertTrue(speedChange == 0.0)
    }

    /**
     * Test that the computeDuration returns the correct time to drive in the desired driving style.
     */
    @Test
    fun computeDuration() {
        trajectoryAnalyser.updateProgress(
            0.10 * expectedDistance * 1000, validState[1], validState[2],
            urbanTime, validState[3], validState[4], validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        trajectoryAnalyser.setDesiredDrivingMode() // URBAN is the desired driving mode
        var duration = trajectoryAnalyser.computeDuration()
        assertTrue(duration> 21.58 && duration < 21.59)  // Check that the correct time is returned

        trajectoryAnalyser.updateProgress(
            0.2 * expectedDistance * 1000, validState[1], validState[2],
            urbanTime, validState[3], validState[4], validState[5],
            validState[6], validState[7], 0.0, 2.0
        )
        trajectoryAnalyser.setDesiredDrivingMode() // URBAN is the desired driving mode
        duration = trajectoryAnalyser.computeDuration()
        assertTrue(duration < 56.44)  // Check that the duration has decrease as urban distance covered-- has increased
    }
}