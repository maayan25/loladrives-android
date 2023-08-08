package org.rdeapp.pcdftester.Sinks

import org.junit.Assert.*

import org.junit.Before
import org.junit.Test
import kotlin.math.exp

class TrajectoryAnalyserTest {
    private val velocityProfile: VelocityProfile = VelocityProfile()
    private lateinit var trajectoryAnalyser: TrajectoryAnalyser;
    private var expectedDistance: Double = 83.0

    private var initialState: List<Double> = listOf<Double>(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    private var validState: List<Double> = listOf<Double>(0.34 * expectedDistance, 0.33 * expectedDistance, 0.33 * expectedDistance, 60.0, 30.0, 25.0)
//    private var invalidAverageSpeedState: List<Double> = listOf<Double>(25.0, 22.0, 22.0, 115.0, 30.0, 90.0)

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
            initialState[3], initialState[4], initialState[5]
        )

        // The desired driving mode should be URBAN at the start of the test
        assertTrue(trajectoryAnalyser.currentDrivingMode() == DrivingMode.URBAN)

        // No constraints should alert the driver at the beginning of the test.
        val constraints = trajectoryAnalyser.getConstraints()

        // High speed and Average Urban Speed are set to 0 for the initial 10 minutes, as
        // their values are not reliable and therefore irrelevant to the driver.
        assertEquals(constraints[0], 0.0) // High speed

        // The other constraints cannot be invalid in this range, and are set to null.
        assertNull(constraints[1]) // Very high speed
        assertNull(constraints[2]) // Stopping time
        assertNull(constraints[3]) // Average Urban Speed
    }

    @Test
    fun updateProgress() {
    }

    /**
     * Test if the checkInvalid function returns "false" when the test is valid.
     */
    @Test
    fun checkInvalidForValidRDETest() {
        // A valid test case, so the isInvalid variable should be set to "false".
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            validState[3], validState[4], validState[5]
        )

        // Update the constraints to make sure the isInvalid variable is updated
        trajectoryAnalyser.getConstraints()
        assertFalse(trajectoryAnalyser.checkInvalid())
    }

    /**
     * Test if the checkInvalid function returns "true" when the test is invalid.
     */
    @Test
    fun checkInvalidForIsInvalid() {
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            115.0, // 5 minutes remaining
            140.0, // speed is not close to the Urban speed range
            90.0 // very high average speed
        )
        // Update the constraints to make sure the isInvalid variable is updated
        trajectoryAnalyser.getConstraints()

        assertTrue(trajectoryAnalyser.checkInvalid())
    }

    /**
     * Test if the checkInvalid function returns "true" when the duration of test is too long.
     */
    @Test
    fun checkInvalidForTimeExceeded() {
        // The initial state is a valid test case, so the isInvalid variable should be set to "false".
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            121.0, // 1 minute too long
            validState[4], validState[5]
        )
        // Update the constraints to make sure the isInvalid variable is updated
        trajectoryAnalyser.getConstraints()

        assertTrue(trajectoryAnalyser.checkInvalid())
    }

    /**
     * Test that the getConstraints function returns all the correct constraints.
     */
    @Test
    fun getConstraintsWhenValid() {
        // Some valid test case
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            validState[3], validState[4], validState[5]
        )

        val constraints = trajectoryAnalyser.getConstraints()
        println("constraints: ${constraints[0]}, ${constraints[1]}, ${constraints[2]}, ${constraints[3]}")
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
    }

    /**
     * Test that the value returned by the getConstraints function is correct
     * when the Very-High speed is too high.
     */
    @Test
    fun getConstraintsInvalidVeryHighSpeed() {
    }

    /**
     * Test that the value returned by the getConstraints function is correct
     * when the High speed is too low but can be increased.
     */
    @Test
    fun getConstraintsWarningHighSpeed() {
    }

    /**
     * Test that the value returned by the getConstraints function is correct
     * when the High speed is invalid.
     */
    @Test
    fun getConstraintsInvalidHighSpeed() {
    }

    /**
     * Test that the value returned by the getConstraints function is correct
     * when the Stopping time is too low but can be increased.
     */
    @Test
    fun getConstraintsWarningStoppingTimeLow() {
    }

    /**
     * Test that the value returned by the getConstraints function is correct
     * when the Stopping time is too high but can be decreased.
     */
    @Test
    fun getConstraintsWarningStoppingTimeHigh() {
    }

    /**
     * Test that the value returned by the getConstraints function is correct
     * when the Stopping time is invalid.
     */
    @Test
    fun getConstraintsInvalidStoppingTime() {
    }

    /**
     * Test that the value returned by the getConstraints function is correct
     * when the Average Urban Speed is too low but can be increased.
     */
    @Test
    fun getConstraintsWarningAverageUrbanSpeedLow() {
    }

    /**
     * Test that the value returned by the getConstraints function is correct
     * when the Average Urban Speed is too high but can be decreased.
     */
    @Test
    fun getConstraintsWarningAverageUrbanSpeedHigh() {
    }

    /**
     * Test that the value returned by the getConstraints function is correct
     * when the Average Urban Speed is invalid.
     */
    @Test
    fun getConstraintsInvalidAverageUrbanSpeed() {
    }

    /**
     * Test that the getAverageUrbanSpeed function returns the correct average urban speed.
     */
    @Test
    fun getAverageUrbanSpeed() {
        // Initial state
        trajectoryAnalyser.updateProgress(
            initialState[0], initialState[1], initialState[2],
            initialState[3], initialState[4], initialState[5]
        )
        assertTrue(trajectoryAnalyser.getAverageUrbanSpeed() == initialState[5])

        // Some valid test case
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            validState[3], validState[4], validState[5]
        )
        assertTrue(trajectoryAnalyser.getAverageUrbanSpeed() == validState[5])

        // Some invalid test case (average urban speed is too high)
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], validState[2],
            115.0, 140.0, 90.0
        )
        assertTrue(trajectoryAnalyser.getAverageUrbanSpeed() == 90.0)
    }

    /**
     * Test that the setDesiredDrivingMode function sets the desired driving mode correctly
     * in the case where more than one driving mode is insufficient.
     */
    @Test
    fun setDesiredDrivingModeMultipleInsufficient() {
        trajectoryAnalyser.updateProgress(
            0.15 * expectedDistance,
            validState[1],
            0.15 * expectedDistance,
            validState[3], validState[4], validState[5]
        )
        var desiredDrivingMode = trajectoryAnalyser.setDesiredDrivingMode()
        assertTrue(desiredDrivingMode == DrivingMode.MOTORWAY)

        trajectoryAnalyser.updateProgress(
            0.15 * expectedDistance,
            validState[1],
            0.15 * expectedDistance,
            validState[3], validState[4], validState[5]
        )
        desiredDrivingMode = trajectoryAnalyser.setDesiredDrivingMode()
        assertTrue(desiredDrivingMode == DrivingMode.MOTORWAY)  // TODO figure out why this is not URBAN
    }

    /**
     * Test that the setDesiredDrivingMode function sets the desired driving mode correctly
     * in the case where only one driving mode is insufficient.
     */
    @Test
    fun setDesiredDrivingModeSingleInsufficient() {
        trajectoryAnalyser.updateProgress(
            validState[0],
            0.15 * expectedDistance,
            validState[2],
            validState[3], validState[4], validState[5]
        )
        val desiredDrivingMode = trajectoryAnalyser.setDesiredDrivingMode()
        assertTrue(desiredDrivingMode == DrivingMode.MOTORWAY) // TODO figure out why this is not RURAL
    }

    @Test
    fun checkSufficient() {
    }

    @Test
    fun currentDrivingMode() {
    }

    @Test
    fun computeSpeedChange() {
    }

    @Test
    fun computeDuration() {
    }
}