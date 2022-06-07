package de.westnordost.streetcomplete.quests

import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import de.westnordost.streetcomplete.Prefs
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.elementfilter.toElementFilterExpression
import java.text.ParseException

fun singleTypeElementSelectionDialog(context: Context, prefs: SharedPreferences, pref: String, defaultValue: String, messageId: Int): AlertDialog {
    var dialog: AlertDialog? = null
    val textInput = EditText(context)
    textInput.addTextChangedListener {
        val button = dialog?.getButton(AlertDialog.BUTTON_POSITIVE)
        button?.isEnabled = textInput.text.toString().let {
            it.lowercase().matches("[a-z0-9_?,\\s]+".toRegex())
                && !it.trim().endsWith(',')
                && !it.contains(",,")
                && it.isNotEmpty() }
    }
    dialog = dialog(context, messageId, prefs.getString(pref, defaultValue)?.replace("|",", ") ?: "", textInput)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            prefs.edit().putString(pref, textInput.text.toString().split(",").joinToString("|") { it.trim() }).apply()
            showRestartToast(context)
        }
        .setNeutralButton(R.string.quest_settings_reset) { _, _ ->
            prefs.edit().remove(pref).apply()
            showRestartToast(context)
        }
        .create()
    return dialog
}

fun numberSelectionDialog(context: Context, prefs: SharedPreferences, pref: String, defaultValue: Int, messageId: Int): AlertDialog {
    var dialog: AlertDialog? = null
    val numberInput = EditText(context)
    numberInput.inputType = InputType.TYPE_CLASS_NUMBER
    numberInput.addTextChangedListener {
        val button = dialog?.getButton(AlertDialog.BUTTON_POSITIVE)
        button?.isEnabled = numberInput.text.toString().let { it.toIntOrNull() != null }
    }
    numberInput.setPaddingRelative(30,10,30,10)
    numberInput.setText(prefs.getInt(pref, defaultValue).toString())
    dialog = AlertDialog.Builder(context)
        .setMessage(messageId)
        .setView(numberInput)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok) { _,_ ->
            numberInput.text.toString().toIntOrNull()?.let {
                prefs.edit().putInt(pref, it).apply()
                showRestartToast(context)
            }
        }
        .setNeutralButton(R.string.quest_settings_reset) { _, _ ->
            prefs.edit().remove(pref).apply()
            showRestartToast(context)
        }
        .create()
    return dialog
}

fun fullElementSelectionDialog(context: Context, prefs: SharedPreferences, pref: String, messageId: Int, defaultValue: String? = null): AlertDialog {
    var dialog: AlertDialog? = null
    val textInput = EditText(context)
    textInput.addTextChangedListener {
        val button = dialog?.getButton(AlertDialog.BUTTON_POSITIVE)
        button?.isEnabled = textInput.text.toString().let {
            it.lowercase().matches("[a-z0-9_=!~()|<>\\s+-]+".toRegex())
                && it.count { c -> c == '('} == it.count { c -> c == ')'}
                && (it.contains('=') || it.contains('~'))
                && try {"nodes with $it".toElementFilterExpression()
                true }
            catch(e: ParseException) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                false
            }
        }
    }

    dialog = dialog(context, messageId, prefs.getString(pref, defaultValue ?: "") ?: "", textInput)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            prefs.edit().putString(pref, textInput.text.toString()).apply()
            showRestartToast(context)
        }
        .setNeutralButton(R.string.quest_settings_reset) { _, _ ->
            prefs.edit().remove(pref).apply()
            showRestartToast(context)
        }
        .create()
    return dialog
}

fun showRestartToast(context: Context) {
    Toast.makeText(context, R.string.quest_settings_restart, Toast.LENGTH_LONG).show()
}

private fun dialog(context: Context, messageId: Int, initialValue: String, input: EditText): AlertDialog.Builder {
    input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
    input.setPaddingRelative(30,10,30,10)
    input.setText(initialValue)
    input.maxLines = 12
    return AlertDialog.Builder(context)
        .setMessage(messageId)
        .setView(input)
        .setNegativeButton(android.R.string.cancel, null)
}

fun getStringFor(prefs: SharedPreferences, pref: String) = prefs.getString(pref, "")?.let { if (it.isEmpty()) "" else "or $it"}

fun questPrefix(prefs: SharedPreferences) = if (prefs.getBoolean(Prefs.QUEST_SETTINGS_PER_PROFILE, false))
    prefs.getLong(Prefs.SELECTED_QUESTS_PRESET, 0).toString() + "_"
else
    ""
