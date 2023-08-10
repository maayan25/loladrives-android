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
            expectedDistance / 4, 30.0, 25.0)
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

    @Test
    fun determinePromptSufficiency() {
        trajectoryAnalyser.updateProgress(sufficientState[0], sufficientState[1], sufficientState[2],
            sufficientState[3], sufficientState[4], sufficientState[5])
        promptGenerator.determinePrompt(20.0, trajectoryAnalyser)
        assertEquals(promptGenerator.getPromptType(), PromptType.SUFFICIENCY)
    }
}