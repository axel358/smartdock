package cu.axel.smartdock.fragments

import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import cu.axel.smartdock.R

class DockElementsLandscapePreferences : PreferenceFragmentCompat() {
    override fun onCreatePreferences(arg0: Bundle?, arg1: String?) {
        setPreferencesFromResource(R.xml.preferences_dock_elements_landscape, arg1)

        val maxRunningApps: EditTextPreference = findPreference("max_running_apps_landscape")!!
        maxRunningApps.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.imeOptions = EditorInfo.IME_ACTION_GO
        }
        maxRunningApps.setOnPreferenceChangeListener { _, newValue ->
            val value = newValue as String
            value.isNotEmpty() && value.toInt() < 50
        }
    }
}
