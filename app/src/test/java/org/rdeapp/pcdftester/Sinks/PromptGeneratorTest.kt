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

    // Distances in metres for the velocity profile.
    private var earlyDistance: Double = 20000.0
    private var progressDistance: Double = 60000.0

    // Example states for the test.
    private var initialState: List<Double> = listOf<Double>(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    private var sufficientState: List<Double> = // Urban is sufficient and constraints are not violated
        listOf<Double>(0.34 * expectedDistance * 1000, 0.1 * expectedDistance * 1000, 0.1 * expectedDistance * 1000,
            20.0, 30.0, 25.0)
    private var validState: List<Double> =
        listOf<Double>(0.34 * expectedDistance * 1000, 0.33 * expectedDistance * 1000, 0.20 * expectedDistance * 1000,
            60.0, 30.0, 25.0)

    @Before
    fun setUp() {
        trajectoryAnalyser = TrajectoryAnalyser(expectedDistance , velocityProfile)
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
        assertEquals(promptGenerator.getPromptType(), PromptType.NONE)
        assertEquals(promptGenerator.getPromptText(), "Analysis will be available after 1/3 of the test is completed.")
        assertEquals(promptGenerator.getAnalysisText(), "")
    }

    /**
     * Test that in a 1st third of the RDE test, the prompt generator
     * generates a prompt saying the analysis will be available after 1/3 of the test is completed.
     */
    @Test
    fun determinePromptNone() {
        // A valid state is a state where the constraints are not violated.
        trajectoryAnalyser.updateProgress(initialState[0], initialState[1], initialState[2],
            initialState[3], initialState[4], initialState[5])
        promptGenerator.determinePrompt(0.0, trajectoryAnalyser)

        Thread.sleep(1000) // Wait for 1 second

        // Update current state to be in the 1st third of the test still.
        trajectoryAnalyser.updateProgress(0.01, initialState[1], initialState[2],
            initialState[3], 20.0, initialState[5])
        promptGenerator.determinePrompt(0.0, trajectoryAnalyser)

        // The prompt generator should generate a NONE prompt.
        assertEquals(promptGenerator.getPromptType(), PromptType.NONE)
        assertEquals(promptGenerator.getPromptText(), "Analysis will be available after 1/3 of the test is completed.")
        assertEquals(promptGenerator.getAnalysisText(), "")
    }

    /**
     * Test that in a 1st third of the RDE test, the prompt generator
     * generates a sufficiency prompt.
     */
    @Test
    fun determinePromptSufficiency() {
        // A valid state is a state where the constraints are not violated.
        trajectoryAnalyser.updateProgress(earlyDistance, initialState[1], initialState[2],
            sufficientState[3], sufficientState[4], sufficientState[5])
        promptGenerator.determinePrompt(earlyDistance, trajectoryAnalyser)

        // The prompt generator should generate a sufficiency prompt.
        assertEquals(promptGenerator.getPromptType(), PromptType.SUFFICIENCY)
        assertEquals(promptGenerator.getPromptText(), "Your urban driving is sufficient.")
    }

    /**
     * Test that the prompt generator generates a prompt for the driving style URBAN and
     * not driving at the urban driving mode.
     */
    @Test
    fun determinePromptDrivingStyleUrban(){
        trajectoryAnalyser.updateProgress(
            0.1 * expectedDistance * 1000, validState[1], validState[2],
            validState[3], 70.5, validState[5]
        ) // Urban distance isn't sufficient and Motorway and Rural are sufficient

        // Update the prompt generator with the current state.
        promptGenerator.determinePrompt(progressDistance, trajectoryAnalyser)

        // The prompt generator should generate a driving style and appropriate analysis
        assertEquals(promptGenerator.getPromptType(), PromptType.DRIVINGSTYLE)
        assertEquals(promptGenerator.getPromptText(), "Aim for a lower driving speed, if it is safe to do so, for more urban driving")
        assertEquals(promptGenerator.getAnalysisText(), "Drive at an average speed of 30 km/h for at most 56.44 minutes.")
    }

    /**
     * Test that the prompt generator generates a prompt for the driving style RURAL
     */
    @Test
    fun determinePromptDrivingStyleRural(){
        trajectoryAnalyser.updateProgress(
            validState[0], 0.13 * expectedDistance * 1000, validState[2],
            validState[3], 10.5, validState[5]
        ) // Rural distance isn't sufficient and Motorway and Urban are sufficient

        promptGenerator.determinePrompt(progressDistance, trajectoryAnalyser)
        Thread.sleep(300000)  // Wait for stopping time to be valid

        // Update the prompt generator with the current state.
        promptGenerator.determinePrompt(progressDistance, trajectoryAnalyser)

        // The prompt generator should generate a driving style and appropriate analysis
        assertEquals(promptGenerator.getPromptType(), PromptType.DRIVINGSTYLE)
        assertEquals(promptGenerator.getPromptText(), "Aim for a higher driving speed, if it is safe to do so, for more rural driving")
        assertEquals(promptGenerator.getAnalysisText(), "Drive at an average speed of 75 km/h for at most 19.92 minutes")
    }


    /**
     * Test that the prompt generator generates a prompt for the driving style MOTORWAY
     */
    @Test
    fun determinePromptDrivingStyleMotorway(){
        trajectoryAnalyser.updateProgress(
            validState[0],
            validState[1],
            0.05 * expectedDistance * 1000,
            validState[3], 70.0, validState[5]
        ) // Motorway distance isn't sufficient and Rural and Urban are sufficient

        promptGenerator.determinePrompt(progressDistance + 1000.0, trajectoryAnalyser)
        // The prompt generator should generate a driving style and appropriate analysis
        assertEquals(promptGenerator.getPromptType(), PromptType.DRIVINGSTYLE)
        assertEquals(promptGenerator.getPromptText(), "Aim for a higher driving speed, if it is safe to do so, for more motorway driving")
        assertEquals(promptGenerator.getAnalysisText(), "Drive at an average speed of 115 km/h for at most 16.46 minutes")
    }

    /**
     * Test that a warning for average urban speed is generated when the
     * average urban speed is too high and driving style URBAN is recommended.
     */
    @Test
    fun determinePromptAverageUrbanSpeedTooHigh() {
        trajectoryAnalyser.updateProgress(
            0.1 * expectedDistance * 1000, validState[1], validState[2],
            validState[3], validState[4], 45.0)

        // Update the prompt generator with the current state.
        promptGenerator.determinePrompt(progressDistance, trajectoryAnalyser)

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
            0.1 * expectedDistance * 1000, validState[1], validState[2],
            validState[3], validState[4], 6.0)

        // Update the prompt generator with the current state.
        promptGenerator.determinePrompt(progressDistance, trajectoryAnalyser)

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
            0.1 * expectedDistance * 1000, validState[1], validState[2],
            validState[3], validState[4], 18.0)

        // Update the prompt generator with the current state.
        promptGenerator.determinePrompt(progressDistance, trajectoryAnalyser)

        // The prompt generator should generate a driving style prompt.
        assertEquals(promptGenerator.getPromptType(), PromptType.AVERAGEURBANSPEED)
        assertEquals(promptGenerator.getPromptText(), "Your average urban speed, 18.0km/h, is close to being invalid.")
        assertEquals(promptGenerator.getAnalysisText(), "You are 3.0km/h above the lower limit.")
    }

    /**
     * Test that a warning for average urban speed is generated when the
     *  average urban speed is close to being too high and driving style URBAN is recommended.
     */
    @Test
    fun determinePromptAverageUrbanSpeedHigh() {
        trajectoryAnalyser.updateProgress(
            0.1 * expectedDistance * 1000, validState[1], validState[2],
            validState[3], validState[4], 37.4)

        // Update the prompt generator with the current state.
        promptGenerator.determinePrompt(progressDistance, trajectoryAnalyser)

        // The prompt generator should generate a driving style prompt.
        assertEquals(promptGenerator.getPromptType(), PromptType.AVERAGEURBANSPEED)
        assertEquals(promptGenerator.getPromptText(), "Your average urban speed, 37.4km/h, is close to being invalid.")
        assertEquals(promptGenerator.getAnalysisText(), "You are 2.6km/h away from exceeding the upper limit.")
    }

    /**
     * Test that a warning for stopping percentage is generated when stopping time 0.0
     * and driving style URBAN is recommended
     */
    @Test
    fun determinePromptNoStoppingTime(){
        trajectoryAnalyser.updateProgress(
            0.13 * expectedDistance * 1000, validState[1], validState[2],
            90.0, 1.0, validState[5]
        )

        promptGenerator.determinePrompt(progressDistance, trajectoryAnalyser)
        // The prompt generator should generate a driving style prompt.
        assertEquals(promptGenerator.getPromptType(), PromptType.DRIVINGSTYLE)
    }

    /**
     * Test that a warning for stopping percentage is generated when stopping time is
     * very low and some time has passed and driving style URBAN is recommended
     */
    @Test
    fun determinePromptVeryLowStoppingTime(){
        trajectoryAnalyser.updateProgress(
            0.13 * expectedDistance * 1000, validState[1], validState[2],
            90.0, 0.0, validState[5]
        )

        promptGenerator.determinePrompt(progressDistance, trajectoryAnalyser)
        Thread.sleep(500)  // Wait for stopping time to be valid

        // Update the prompt generator with the current state.
        promptGenerator.determinePrompt(progressDistance, trajectoryAnalyser)

        promptGenerator.determinePrompt(progressDistance, trajectoryAnalyser)
        // The prompt generator should generate a stopping percentage prompt.
        assertEquals(promptGenerator.getPromptType(), PromptType.STOPPINGPERCENTAGE)
        assertEquals(promptGenerator.getPromptText(), "You are stopping too little. Try to stop more.")
        assertEquals(promptGenerator.getAnalysisText(), "You need to stop for at least 6.0% more of the urban time.")
    }

    /**
     * Test that a warning for stopping percentage is generated when the stopping percentage is
     * below the lower boundary
     */
    @Test
    fun determinePromptLowStoppingTime(){
        trajectoryAnalyser.updateProgress(
            0.13 * expectedDistance * 1000, validState[1], validState[2],
            20.0, 0.0, validState[5]
        )
        promptGenerator.determinePrompt(progressDistance, trajectoryAnalyser)
        Thread.sleep(192300)  // Wait for 3.2 minutes
        trajectoryAnalyser.updateProgress(
            0.13 * expectedDistance * 1000, validState[1], validState[2],
            20.0 + velocityProfile.getTimeDifference(), 0.0, validState[5]
        )
        promptGenerator.determinePrompt(progressDistance + 5000.0, trajectoryAnalyser)
        // The prompt generator should generate a driving style prompt.
        assertEquals(promptGenerator.getPromptType(), PromptType.STOPPINGPERCENTAGE)
        assertEquals(promptGenerator.getPromptText(), "You are stopping too little. Try to stop more.")
        assertEquals(promptGenerator.getAnalysisText(), "You need to stop for at least 2.0% more of the urban time.")
    }

    /**
     * Test that a warning for stopping percentage is generated when the stopping time is close to
     * the upper bound.
     */
    @Test
    fun determinePromptHighStoppingTime(){
        trajectoryAnalyser.updateProgress(
            0.13 * expectedDistance * 1000, validState[1], validState[2],
            20.0, 0.0, validState[5]
        )
        promptGenerator.determinePrompt(progressDistance, trajectoryAnalyser)

        Thread.sleep(1520000)  // Wait for 22.5 minutes
        trajectoryAnalyser.updateProgress(
            0.13 * expectedDistance * 1000, validState[1], validState[2],
            20.0 + velocityProfile.getTimeDifference(), 0.0, validState[5]
        )
        promptGenerator.determinePrompt(progressDistance + 5000.0, trajectoryAnalyser)
        // The prompt generator should generate a driving style prompt.
        assertEquals(promptGenerator.getPromptType(), PromptType.STOPPINGPERCENTAGE)
        assertEquals(promptGenerator.getPromptText(), "You are close to exceeding the stopping percentage. Try to stop less.")
        assertEquals(promptGenerator.getAnalysisText(), "You are stopping 9.0% less than the upper bound.")
    }


    /**
     * Test that a prompt for high speed is generated advising the user to drive a
     * certain duration to reach at least 5 minutes
     */
    @Test
    fun determinePromptHighSpeed(){
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], 0.11 * expectedDistance * 1000,
            validState[3], 130.1, validState[5]
        )
        promptGenerator.determinePrompt(progressDistance, trajectoryAnalyser)
        assertEquals(promptGenerator.getPromptType(), PromptType.HIGHSPEEDPERCENTAGE)
        assertEquals(promptGenerator.getPromptText(), "Your driving style is good")
        assertEquals(promptGenerator.getAnalysisText(), "You need to drive at 100km/h or more for at least 5.0 more minutes.")
    }

    /**
     * Test that a warning for the very high speed is generated when 1.5% of test has
     * been driven at 145km/h or more and check that the Prompt Type changes.
     */
    @Test
    fun determinePromptAndSetPromptMotorway(){
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], 0.1 * expectedDistance * 1000,
            validState[3], 145.1, validState[5]
        )
        // Generates a high speed prompt first
        promptGenerator.determinePrompt(progressDistance, trajectoryAnalyser)
        assertEquals(promptGenerator.getPromptType(), PromptType.HIGHSPEEDPERCENTAGE)

        Thread.sleep(23490) // Wait for 23.5 seconds
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], 0.1 * expectedDistance * 1000,
            20 + velocityProfile.getTimeDifference(), 145.1, validState[5]
        )
        promptGenerator.determinePrompt(progressDistance + 1000.0, trajectoryAnalyser)
        assertEquals(promptGenerator.getPromptType(), PromptType.VERYHIGHSPEEDPERCENTAGE)
        assertEquals(promptGenerator.getPromptText(), "Aim for a lower driving speed, if it is safe to do so, for more motorway driving")
        assertEquals(promptGenerator.getAnalysisText(), "You have driven at 145km/h or more for 1.5% of the motorway driving distance.")
    }

    /**
     * Test that a warning for the very high speed is generated when 2.5% of test has
     * been driven at 145km/h or more.
     */
    @Test
    fun determinePromptVeryHighSpeed(){
        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], 0.1 * expectedDistance * 1000,
            validState[3], 145.1, validState[5]
        )
        promptGenerator.determinePrompt(progressDistance, trajectoryAnalyser)

        Thread.sleep(40000) // Wait for 40 seconds

        trajectoryAnalyser.updateProgress(
            validState[0], validState[1], 0.1 * expectedDistance * 1000,
            20 + velocityProfile.getTimeDifference(), 145.1, validState[5]
        )
        promptGenerator.determinePrompt(progressDistance + 1000.0, trajectoryAnalyser)

        assertEquals(promptGenerator.getPromptType(), PromptType.VERYHIGHSPEEDPERCENTAGE)
        assertEquals(promptGenerator.getPromptText(), "Aim for a lower driving speed, if it is safe to do so, for more motorway driving")
        assertEquals(promptGenerator.getAnalysisText(), "You have driven at 145km/h or more for 2.5% of the motorway driving distance.")
    }

    /**
     * Test that when both the stopping percentage and average urban speed constraints
     * require prompts then correct prompt is set.
     */
    @Test
    fun setPromptTypeUrban(){
        trajectoryAnalyser.updateProgress(
            0.13 * expectedDistance * 1000, validState[1], validState[2],
            90.0, 0.0, 18.0
        ) // Stopping time is too low and average urban speed is close to being low
        promptGenerator.determinePrompt(progressDistance, trajectoryAnalyser)

        assertEquals(promptGenerator.getPromptType(), PromptType.AVERAGEURBANSPEED)

        trajectoryAnalyser.updateProgress(
            0.13 * expectedDistance * 1000, validState[1], validState[2],
            90.0 + velocityProfile.getTimeDifference(), 0.0, 27.0
        ) // Average Urban speed is valid and stopping time is still low
        promptGenerator.determinePrompt(progressDistance, trajectoryAnalyser)

        assertEquals(promptGenerator.getPromptType(), PromptType.STOPPINGPERCENTAGE)
    }


}