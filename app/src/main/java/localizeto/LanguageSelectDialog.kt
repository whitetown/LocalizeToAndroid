package localizeto.androidexample

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import java.lang.IllegalStateException

class LanguageSelectDialog(
    private val languages: Array<String>,
    private val defaultLanguageIndex: Int
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            var selectedIdx = defaultLanguageIndex
            builder.setTitle("Select Language")
                .setSingleChoiceItems(languages, defaultLanguageIndex) { _, idx ->
                    selectedIdx = idx
                }
                .setPositiveButton("OK") { _, _ ->
                    (activity as MainActivity).doChangeLanguage(selectedIdx)
                }
                .setNegativeButton("Cancel") {_, _ -> }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}