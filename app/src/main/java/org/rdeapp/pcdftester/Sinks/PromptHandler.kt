package org.rdeapp.pcdftester.Sinks

import android.graphics.Color
import android.os.Build
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.fragment.app.FragmentTransaction
import de.unisaarland.loladrives.Fragments.RDE.RDEFragment
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.R
import kotlinx.android.synthetic.main.fragment_r_d_e.textViewAnalysis
import kotlinx.android.synthetic.main.fragment_r_d_e.textViewRDEPrompt
import java.util.Locale

/**
 * Class for handling the prompt for improving the driving style.
 * @property fragment The RDEFragment in which the prompt is displayed.
 */
class PromptHandler (
    private val fragment: RDEFragment
) : TextToSpeech.OnInitListener {

    // Text to speech object
    private var tts: TextToSpeech? = TextToSpeech(fragment.requireActivity(), this)

    // The distance that should be travelled in the RDE test
    private var expectedDistance = fragment.distance

    // Used to analyse the trajectory to choose the next instructions for the driver
    private var trajectoryAnalyser = fragment.trajectoryAnalyser

    // Used to determine the current prompt that should be displayed
    private var promptGenerator = PromptGenerator(expectedDistance)

    // Variables to store the current prompt
    private var currentPromptText: String = ""
    private var currentPromptType: PromptType? = null
    private var newPromptType: PromptType? = null

    /**
     * Update the prompt for improving the driving style according to the received RTLola results.
     * @param totalDistance The total distance travelled so far.
     */
    suspend fun handlePrompt(totalDistance: Double) {
        // Check if the RDE test is still valid
        handleInvalidRDE()

        // Determine the next instructions according to analysis of the trajectory driven.
        generatePrompt(totalDistance)
    }

    /**
     * Handles invalid RDE tests by updating the TextView for the RDE prompt and creates
     * an error message It also stops the current RDE test and navigates to the RDE setting.
     */
    private suspend fun handleInvalidRDE() {
        if (trajectoryAnalyser.checkInvalid()) {
            fragment.textViewRDEPrompt.text = "This RDE test is invalid, and will be stopped now."
            fragment.textViewRDEPrompt.setTextColor(Color.RED)

            // Only speak if the text has changed
            if (currentPromptText != promptGenerator.getPromptText()) {
                speak()
            }

            Toast.makeText(fragment.requireActivity(),"Exiting...", Toast.LENGTH_LONG).show()

            // Stop tracking the RDE test
            (fragment.requireActivity() as MainActivity).stopTracking()

            // Move to the RDE settings fragment
            fragment.requireActivity().supportFragmentManager.beginTransaction().replace(
                R.id.frame_layout,
                (fragment.requireActivity() as MainActivity).rdeSettingsFragment
            ).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
        }
    }

    /**
     * Generate the prompt according to the PromptType set from the analysis done on the trajectory.
     * Sets the TextViews for the RDE prompt and analysis depending on the PromptType
     */
    private fun generatePrompt(totalDistance: Double) {
        // Update the prompt and analysis texts and colours
        updatePrompt(totalDistance)
        newPromptType = promptGenerator.getPromptType()

        // If the prompt type is DRIVINGSTYLE, then only speak if the text has changed
        if (newPromptType == PromptType.DRIVINGSTYLE) {
            // Only speak if the text has changed
            if (currentPromptText != fragment.textViewRDEPrompt.text.toString() && currentPromptType != newPromptType){
                speak()
            }
        }
        // For all other prompt types, only speak if the prompt type has changed
        else if (newPromptType != currentPromptType) {
            speak()
        }

        // Keep track of the last prompt type and text that was displayed
        currentPromptType = newPromptType
        currentPromptText = fragment.textViewRDEPrompt.text.toString()
    }

    /**
     * Update the prompt and analysis texts and colours according to the determined values
     * in the prompt generator.
     */
    private fun updatePrompt(totalDistance: Double) {
        // Set the new instructions in the prompt generator
        promptGenerator.determinePrompt(totalDistance, trajectoryAnalyser)

        // Update the prompt text
        fragment.textViewRDEPrompt.text = promptGenerator.getPromptText()
        fragment.textViewRDEPrompt.setTextColor(promptGenerator.getPromptColour())

        // Update the analysis text
        fragment.textViewAnalysis.text = promptGenerator.getAnalysisText()
        fragment.textViewAnalysis.setTextColor(promptGenerator.getAnalysisColour())
    }

    /**
     * Initialise the Text To Speech engine.
     * @param status The status of the Text To Speech engine.
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(fragment.requireActivity(),"The Language not supported!", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Speak the text in the RDE prompt TextView.
     * If the SDK version is below LOLLIPOP, then a toast is shown that Text To Speech is not supported.
     */
    private fun speak() {
        val text = fragment.textViewRDEPrompt.text.toString()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ID")
        } else {
            Toast.makeText(fragment.requireActivity(), "This SDK version does not support Text To Speech.", Toast.LENGTH_LONG).show()
        }
    }

}