package org.rdeapp.pcdftester.Sinks

import org.junit.Assert.*

import org.junit.Before
import org.junit.Test

class PromptGeneratorTest {
    private val velocityProfile: VelocityProfile = VelocityProfile()
    private lateinit var trajectoryAnalyser: TrajectoryAnalyser
    private lateinit var promptGenerator: PromptGenerator

    // The expected distance that is chosen for the test.
    private var expectedDistance: Double = 83.0

    // Example states for the test.
    private var initialState: List<Double> = listOf<Double>(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    private var sufficientState: List<Double> = // Urban is sufficient and constraints are not violated
        listOf<Double>(0.34 * expectedDistance, 0.1 * expectedDistance, 0.1 * expectedDistance,
            20.0, 30.0, 25.0)
    private var validState: List<Double> =
        listOf<Double>(0.34 * expectedDistance, 0.33 * expectedDistance, 0.20 * expectedDistance,
            60.0, 30.0, 25.0)

    @Before
    fun setUp() {
        trajectoryAnalyser = TrajectoryAnalyser(expectedDistance, velocityProfile)
        promptGenerator = PromptGenerator(expectedDistance)
    }

    /**
     * Test that in the initial state of the RDE test, the prompt generator does not generate a prompt.
     */
    @Test
    fun determinePromptInitialState() {
        trajectoryAnalyser.updateProgress(initialState[0], initialState[1], initialState[2],
            initialState[3], initialState[4], initialState[5])
        promptGenerator.determinePrompt(0.0, trajectoryAnalyser)
        assertEquals(promptGenerator.getPromptType(), null)
        assertEquals(promptGenerator.getPromptText(), "")
        assertEquals(promptGenerator.getAnalysisText(), "")
    }

    /**
     * Test that in a 1st third of the RDE test, the prompt generator
     * generates a sufficiency prompt.
     */
    @Test
    fun determinePromptSufficiency() {
        // A valid state is a state where the constraints are not violated.
        trajectoryAnalyser.updateProgress(sufficientState[0], sufficientState[1], sufficientState[2],
            sufficientState[3], sufficientState[4], sufficientState[5])
        promptGenerator.determinePrompt(20.0, trajectoryAnalyser)

        // The prompt generator should generate a sufficiency prompt.
        assertEquals(promptGenerator.getPromptType(), PromptType.SUFFICIENCY)
        assertEquals(promptGenerator.getPromptText(), "Your urban driving is sufficient.")
    }

    /**
     * Test that after a 1st third of the RDE test, the prompt generator
     * generates a sufficiency prompt.
     */
    @Test
    fun determinePromptDrivingStyle() {
        // A valid state is a state where the constraints are not violated and
        trajectoryAnalyser.updateProgress(validState[0], validState[1], validState[2],
            validState[3], validState[4], validState[5])

        // Update the prompt generator with the current state.
        promptGenerator.determinePrompt(60.0, trajectoryAnalyser)

        // The prompt generator should generate a driving style prompt.
        assertEquals(promptGenerator.getPromptType(), PromptType.DRIVINGSTYLE)
        assertEquals(promptGenerator.getPromptText(), "Your driving style is good")
    }

    /**
     * Test that a warning for average urban speed is generated when the
     * average urban speed is too high and driving style URBAN is recommended.
     */
    @Test
    fun determinePromptAverageUrbanSpeedTooHigh() {
        trajectoryAnalyser.updateProgress(
            0.1 * expectedDistance, validState[1], validState[2],
            validState[3], validState[4], 45.0)

        // Update the prompt generator with the current state.
        promptGenerator.determinePrompt(60.0, trajectoryAnalyser)

        // The prompt generator should generate a driving style prompt.
        assertEquals(promptGenerator.getPromptType(), PromptType.AVERAGEURBANSPEED)
        assertEquals(promptGenerator.getPromptText(), "Your average urban speed, 45.0km/h, is too high.")
        assertEquals(promptGenerator.getAnalysisText(), "You are 5.0km/h more than the upper limit.")
    }

    /**
     * Test that a warning for average urban speed is generated when the
     * average urban speed is too low and driving style URBAN is recommended.
     */
    @Test
    fun determinePromptAverageUrbanSpeedTooLow() {
        trajectoryAnalyser.updateProgress(
            0.1 * expectedDistance, validState[1], validState[2],
            validState[3], validState[4], 6.0)

        // Update the prompt generator with the current state.
        promptGenerator.determinePrompt(60.0, trajectoryAnalyser)

        // The prompt generator should generate a driving style prompt.
        assertEquals(promptGenerator.getPromptType(), PromptType.AVERAGEURBANSPEED)
        assertEquals(promptGenerator.getPromptText(), "Your average urban speed, 6.0km/h, is too low.")
        assertEquals(promptGenerator.getAnalysisText(), "You are 9.0km/h less than the lower limit.")
    }

    /**
     * Test that a warning for average urban speed is generated when the
     * average urban speed is close to being too low and driving style URBAN is recommended.
     */
    @Test
    fun determinePromptAverageUrbanSpeedLow() {
        trajectoryAnalyser.updateProgress(
            0.1 * expectedDistance, validState[1], validState[2],
            validState[3], validState[4], 18.0)

        // Update the prompt generator with the current state.
        promptGenerator.determinePrompt(60.0, trajectoryAnalyser)

        // The prompt generator should generate a driving style prompt.
        assertEquals(promptGenerator.getPromptType(), PromptType.AVERAGEURBANSPEED)
        assertEquals(promptGenerator.getPromptText(), "Your average urban speed, 18.0km/h, is close to being invalid.")
        assertEquals(promptGenerator.getAnalysisText(), "You are 3.0km/h above the lower limit.")
    }

    /**
     * Test that a warning for average urbam speed is generated when the
     *  average urban speed is close to being too high and driving style URBAN is recommended.
     */
    @Test
    fun determinePromptAverageUrbanSpeedHigh() {
        trajectoryAnalyser.updateProgress(
            0.1 * expectedDistance, validState[1], validState[2],
            validState[3], validState[4], 37.0)

        // Update the prompt generator with the current state.
        promptGenerator.determinePrompt(60.0, trajectoryAnalyser)

        // The prompt generator should generate a driving style prompt.
        assertEquals(promptGenerator.getPromptType(), PromptType.AVERAGEURBANSPEED)
        assertEquals(promptGenerator.getPromptText(), "Your average urban speed, 37.0km/h, is close to being invalid.")
        assertEquals(promptGenerator.getAnalysisText(), "You are 3.0km/h away from exceeding the upper limit.")
    }



}